/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.plugin.util.AllocationResultHelper;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.NewTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.plugin.util.AllocationResultHelper;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.servicediscovery.description.Lineage;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.core.util.UID;

public class ExternalAllocator extends InventoryModule implements AllocatorModule {

  private static int WARNING_SUPPRESSION_INTERVAL = 2;
  private long warningCutoffTime = 0;

  Role providerRole;
  /** list of nsn's with no resupply point */
  private Vector noResupply;

  public ExternalAllocator(InventoryPlugin imPlugin, Role provider) {
    super(imPlugin);
    providerRole = provider;
    noResupply = new Vector();
  }

  // allocate our new refill tasks
  public void allocateRefillTasks(Collection newRefills) {
    Task refill;
    Iterator refillIT = newRefills.iterator();
    while(refillIT.hasNext()) {
      refill = (Task) refillIT.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "ExtAlloc" + inventoryPlugin.getSupplyType()+"]" +
                     "trying to allocate refill task: " + refill.getUID());
      }
      allocateTask(refill);
    }
  }

    private void allocateTask(Task task) {
	try {
	    if (task.getPlanElement() != null) {
		logger.error("Found Task with PlanElement. Ignoring task.");
//    		return true;
                return;
	    }

	    Collection tasksAndProviders = findBestSourceOrSplit(task);
	    if (tasksAndProviders != null) {
		Iterator tasksAndProvidersIT = tasksAndProviders.iterator();
		Task currentTask;
		Organization currentProvider;
		while(tasksAndProvidersIT.hasNext()) {
		    currentTask = (Task) tasksAndProvidersIT.next();
		    currentProvider = (Organization) tasksAndProvidersIT.next();
		    makeAllocationOrReset(currentTask, currentProvider);
		}
	    }
	} catch (Exception e) {
	    logger.error("Allocate Task Exception: " + e);	    
	    e.printStackTrace();
	}
//  	return true;
    }

  /** build Allocation with an estimated alloc result */
  private Allocation buildAllocation(Task t, Asset a, Role r)
  {
    return inventoryPlugin.getPlanningFactory().createAllocation(t.getPlan(), t, a, null, r);
  }

  private AllocationResult createPredictedAllocationResult(Task task) {
    //MWD in the future this will have to generate the expected result
    //from the predictor.
    return new AllocationResultHelper(task, null).getAllocationResult(0.25, true);
  }

    protected Collection findBestSourceOrSplit(Task task) {
        if (getTaskUtils().isProjection(task)) {
            /* For a projection, should be time-phased as support
               changes over time.*/
	    long startTime = getTaskUtils().getStartTime(task);
	    long endTime = getTaskUtils().getEndTime(task);
            Vector supportOrgs = null;
            try {
              supportOrgs = buildSupportOrgList(inventoryPlugin.getMyOrganization(), 
                                                providerRole, 
                                                startTime, 
                                                endTime);
            } catch(IllegalArgumentException exc) {
              System.out.println("Illegal task "+getTaskUtils().taskDesc(task)+", "+exc.getMessage()+
                                 ", given Start "+new java.util.Date(startTime)+" given End "+
                                 new java.util.Date(endTime));
              return null;
            }
	    long numOfSupportOrgs = supportOrgs.size(), currentStart, currentEnd, nextStart;
	    if (numOfSupportOrgs > 0) {
		LogisticsInventoryPG invPG = getLogisticsInventoryPG(task);

		
		if (invPG != null) {
		    invPG.removeRefillProjection(task);
		}
		currentStart = startTime;
		boolean makeSupplyTask = false;
		Organization provider;
		Vector tasksAndProviders = new Vector();
		SupportOrg supportOrg;
		Task currentTask;
		for (int i = 0; i < numOfSupportOrgs; i++) {
		    supportOrg = (SupportOrg)supportOrgs.elementAt(i);
		    provider = supportOrg.getOrg();
		    if (i + 1 == numOfSupportOrgs) {
			// last one
			currentEnd = endTime;
		    } else {
			currentEnd = supportOrg.getEndTime();
			nextStart =  ((SupportOrg)supportOrgs.elementAt(i + 1)).getStartTime();
			// if gap between this end and next start, set end to next start
			if (nextStart > currentEnd) {
			    currentEnd = nextStart;
			}
		    }
		    currentTask = editOrCopySupplyTask(makeSupplyTask, task, currentStart, 
						       currentEnd, invPG, startTime, endTime);

		    if (invPG != null) {
			if (makeSupplyTask) {
			    Inventory inventory = inventoryPlugin.findOrMakeInventory(task.getDirectObject());
			    inventoryPlugin.publishRefillTask(currentTask, inventory);
			} else {
			    inventoryPlugin.publishChange(currentTask);
			}
			invPG.addRefillProjection(currentTask);
		    } else {
			if (makeSupplyTask) {
			    inventoryPlugin.publishAdd(currentTask);
			    NewWorkflow wf = (NewWorkflow) task.getWorkflow();
			    if (wf == null) {
				logger.error("Split: NULL Workflow: " + task.getUID() + " " + task);
			    } else {
				wf.addTask(currentTask);
				((NewTask)currentTask).setWorkflow(wf);
				PlanElement pe = wf.getParentTask().getPlanElement();
				if (pe instanceof Expansion) {
				    inventoryPlugin.publishChange((Expansion) pe);
				} else {
				    if (logger.isErrorEnabled()) {
					logger.error("Split: PE not Expansion? " + pe);
				    }
				}
			    }
			} else {
			    inventoryPlugin.publishChange(currentTask);
			}
		    }

		    //		    logger.error("Task and Provider: " + currentTask + " " + provider);
		    tasksAndProviders.add(currentTask);
		    tasksAndProviders.add(provider);
		    long checkStart = -2;
		    checkStart = getTaskUtils().getStartTime(currentTask);
		    makeSupplyTask = true;
		    currentStart = currentEnd;
		}
		    
		return(tasksAndProviders);
	    } else {
		findSourcesWarning(task);
	    }

        } else {
	    return(findSingleSource(task, getTaskUtils().getEndTime(task)));
        }
	return null;
    }


    protected Collection findSingleSource(Task task, long endTime) {
	Vector tasksAndProviders = new Vector();
	Enumeration supportOrgs = getAssetUtils().getSupportingOrgs(inventoryPlugin.getMyOrganization(), 
								    providerRole, 
								    endTime);
	if (supportOrgs.hasMoreElements()) {
	    // For now, returning the first supporting org during the time span
	    tasksAndProviders.add(task);
	    tasksAndProviders.add((Organization)supportOrgs.nextElement());
	    return(tasksAndProviders);
	} else {
	    findSourcesWarning(task);
	    return(null);
	}
    }

    protected class SupportOrg {
	Organization org;
	long startTime;
	long endTime;
	public SupportOrg(Organization org, long startTime, long endTime) {
	    this.org = org;
	    this.startTime = startTime;
	    this.endTime = endTime;
	}
	public Organization getOrg() { return org; }
	public long getEndTime() { return endTime; }
	public long getStartTime() { return startTime; }
    };

    public Vector buildSupportOrgList(Organization myOrg, Role role, long start, long end) {

	// We want to build a list of orgs where
	//   - the first one: Has a start time before or equal to "start", and has the latest end time;
	//     if none have a start time before "start", then the first one is the one
	//     with the earliest start time.
	//   - subsequent orgs: Has a start time before or equal to the start of the 
        //     previous org in the list, and has the latest end time;
	//     if none have a start time before that of the previous org, then the first one is the one
	//     with the earliest start time.
        //   - the list ends with the first org with an end time greater to or equal to "end";
	//     otherwise, with the one with the latest end time

	TimeSpan duration = new MutableTimeSpan();
        ((NewTimeSpan)duration).setTimeSpan(start, end);
	RelationshipSchedule rel_sched = myOrg.getRelationshipSchedule();
	Collection relationships = rel_sched.getMatchingRelationships(role, duration);
	Vector supportOrgs = new Vector();
	Iterator relationshipsIt;
	Relationship relationship;
	boolean hasGoodStart;
	Organization best = null, org;
	boolean stop = false, findingFirstEntry = true;
	long localStart = start, orgStart, orgEnd, bestStart = 0, bestEnd = 0, prevEnd = 0;

	while (! stop) {
	    relationshipsIt = relationships.iterator();
	    hasGoodStart = false;

	    // Find next entry in list
	    while (relationshipsIt.hasNext() && ! stop) {
		relationship = (Relationship)relationshipsIt.next();
		org = (Organization) rel_sched.getOther(relationship);
		orgStart = relationship.getStartDate().getTime();
		orgEnd = relationship.getEndDate().getTime();

		if (orgStart < end) {
		    if (best == null && findingFirstEntry) {
			best = org;
			bestStart = orgStart;
			bestEnd = orgEnd;
			if (bestStart <= localStart) {
			    hasGoodStart = true;
			}
		    } else {
			if (hasGoodStart) {
			    if (orgStart <= localStart &&
				orgEnd >= bestEnd) {
				best = org;
				bestStart = orgStart;
				bestEnd = orgEnd;
			    }
			} else if ((findingFirstEntry && orgStart < bestStart) ||
				   ((best == null || orgStart < bestStart) &&
				    orgEnd > prevEnd)) {

			    best = org;
			    bestStart = orgStart;
			    bestEnd = orgEnd;
			    if (bestStart <= localStart) {
				hasGoodStart = true;
			    }
			}
		    }
		}
	    }
	    if (best != null || (best == null && findingFirstEntry)) {
		supportOrgs.add(new SupportOrg(best, bestStart, bestEnd));
	    }
	    prevEnd = bestEnd;
	    findingFirstEntry = false;
	    if (best == null || bestEnd >= end) {
		stop = true;
	    } else {
		localStart = bestEnd;
		best = null;
	    }
	}
	return supportOrgs;
    }

  private long getWarningCutOffTime() {
    if (warningCutoffTime == 0) {
//       WARNING_SUPPRESSION_INTERVAL = Integer.getInteger(QUERY_GRACE_PERIOD_PROPERTY,
// 							WARNING_SUPPRESSION_INTERVAL).intValue();
      warningCutoffTime = System.currentTimeMillis() + WARNING_SUPPRESSION_INTERVAL*60000;
    }

    return warningCutoffTime;
  }

  private void stagedErrorLog(String message) {
    stagedErrorLog(message, null);
  }

  private void stagedErrorLog(String message, Throwable e) {

    if(System.currentTimeMillis() > getWarningCutOffTime()) {
      if (e == null)
	logger.error(inventoryPlugin.getClusterId().toString() + message);
      else
	logger.error(inventoryPlugin.getClusterId().toString() + message, e);
    } else if (logger.isInfoEnabled()) {
      if (e == null)
	logger.info(inventoryPlugin.getClusterId().toString() + message);
      else
	logger.info(inventoryPlugin.getClusterId().toString() + message, e);
    }
  }

  private boolean verifyBeforeAllocation(Task task, Organization org) {
    // Do not allocate tasks after they have taken place-AF does this make sense?
    if (!(task.beforeCommitment(new Date(inventoryPlugin.currentTimeMillis())))) {
      logger.warn("verifyBeforeAllocation: return ... after commitment "+task.getCommitmentDate()+" task:"+task+" to Asset "+org);
      // too late to change
      return false;
    }
    return true;
  }

  public HashSet updateAllocationResult(Collection sub) {
    //    System.out.println("ex alloc update ARs being called in" + inventoryPlugin.getClusterId());
    HashSet backwardFlowInventories = new HashSet();
    // Set up the affected inventories for the AllocationAssessor
    Task refill;
    Asset asset;
    Allocation alloc;
    Inventory inventory;
    LogisticsInventoryPG logInvPG = null;
    Iterator refill_list = sub.iterator(); // ###
    while (refill_list.hasNext()) {
      alloc = (Allocation) refill_list.next();
      refill = alloc.getTask();
      asset = (Asset)refill.getDirectObject();
      inventory = inventoryPlugin.findOrMakeInventory(asset);

      if (PluginHelper.updatePlanElement(alloc)) {
        boolean didPubChg = inventoryPlugin.publishChange(alloc);
        if (logger.isDebugEnabled()) {
          logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "ExtAlloc[" + inventoryPlugin.getSupplyType()+"]" +
                       "publist Changing allocation due to AR change: " + alloc.getUID()+ 
                       " parent task is: " + alloc.getTask().getUID() + " publishChange returned: " + didPubChg);
        }
	//System.out.println("ex all publish changing and added to AA's inventory list " + logInvPG.getResource());
	backwardFlowInventories.add(inventory);
	
	logInvPG = (LogisticsInventoryPG)
            inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
	if (refill.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
            logInvPG.updateRefillProjection(refill);
	} else {
            logInvPG.updateRefillRequisition(refill);
	}
      }
    }
    return backwardFlowInventories;
  }

  public LogisticsInventoryPG getLogisticsInventoryPG(Task wdrawTask) {
	LogisticsInventoryPG logInvPG = null;
	Asset asset = (Asset)wdrawTask.getDirectObject();
	Inventory inventory = inventoryPlugin.findOrMakeInventory(asset);
	logInvPG = (LogisticsInventoryPG)
	    inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
	return logInvPG;
  }

    /** copies a Supply task from another to split it**/
    protected NewTask editOrCopySupplyTask(boolean makeNewTask, Task origTask, 
					 long start, long end, LogisticsInventoryPG invPG, 
					 long origStart, long origEnd) {

	NewTask task;
	boolean updateDates = true;
	if (makeNewTask) {
	    task = inventoryPlugin.getPlanningFactory().newTask();
	    task.setVerb( origTask.getVerb());
	    task.setDirectObject( origTask.getDirectObject());
	    task.setParentTaskUID( origTask.getParentTaskUID() );
	    task.setContext( origTask.getContext() );
	    task.setPlan( origTask.getPlan() );
	    Enumeration pp = origTask.getPrepositionalPhrases();
	    Vector ppv = new Vector();
	    while(pp.hasMoreElements()) {
		ppv.addElement(pp.nextElement());
	    }
	    ppv.add(newPrepositionalPhrase("SplitTask"));
	    task.setPrepositionalPhrases(ppv.elements());
	    task.setPriority(origTask.getPriority());
	    task.setSource( inventoryPlugin.getClusterId() );
	} else {
	    task = (NewTask) origTask;
	    if (origStart == start && origEnd == end) {
		updateDates = false;
	    }
	}
	if (updateDates) {
	    Preference startPref, endPref;
	    if (invPG != null) {
		startPref = getTaskUtils().createRefillTimePreference(start, inventoryPlugin.getOPlanArrivalInTheaterTime(), 
						       inventoryPlugin.getOPlanEndTime(),
						       AspectType.START_TIME, invPG, inventoryPlugin.getPlanningFactory());
		endPref = getTaskUtils().createRefillTimePreference(end, inventoryPlugin.getOPlanArrivalInTheaterTime(), 
						     inventoryPlugin.getOPlanEndTime(),
						     AspectType.END_TIME, invPG, inventoryPlugin.getPlanningFactory());
	    } else {
		startPref = getTaskUtils().createTimePreference(start, inventoryPlugin.getLogOPlanStartTime(), inventoryPlugin.getLogOPlanEndTime(), AspectType.START_TIME, inventoryPlugin.getClusterId(), inventoryPlugin.getPlanningFactory());
		endPref = getTaskUtils().createTimePreference(end, inventoryPlugin.getLogOPlanStartTime(), inventoryPlugin.getLogOPlanEndTime(), AspectType.END_TIME, inventoryPlugin.getClusterId(), inventoryPlugin.getPlanningFactory());
	    }

	    Enumeration origPrefs = origTask.getPreferences();
	    Preference currentPref, copiedPref;
	    Vector newPrefs = new Vector();
	    while (origPrefs.hasMoreElements()) {
		currentPref = (Preference) origPrefs.nextElement();
		if (! (currentPref.getAspectType() == AspectType.START_TIME || 
		       currentPref.getAspectType() == AspectType.END_TIME)) {
		    copiedPref = inventoryPlugin.getPlanningFactory().
			newPreference(currentPref.getAspectType(),
				      (ScoringFunction)currentPref.getScoringFunction().clone());
		    newPrefs.add(copiedPref);
		} 
	    }
	    newPrefs.add(startPref);
	    newPrefs.add(endPref);
	    synchronized (origTask) {
		task.setPreferences(newPrefs.elements());
	    }
	}

	return task;
    }

  private PrepositionalPhrase newPrepositionalPhrase(String preposition) {
    NewPrepositionalPhrase pp = inventoryPlugin.getPlanningFactory().newPrepositionalPhrase();
    pp.setPreposition(preposition);
    return pp;
  }


    protected void findSourcesWarning(Task task) {
	    String itemId = task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
	    if (getTaskUtils().isProjection(task)) {
		logger.error("No "+providerRole+" for task " + task.getUID() + ", during ["+
                            getTimeUtils().dateString(getTaskUtils().getStartTime(task))+
			     "-" +
			   getTimeUtils().dateString(getTaskUtils().getEndTime(task)) +"]"+". Will retry.");
	    } else {
		logger.error("No "+providerRole+" for task " + task.getUID() + ", during "+
                            getTimeUtils().dateString(getTaskUtils().getEndTime(task))+". Will retry.");
	    }
    }

    private boolean makeAllocationOrReset(Task task, Organization provider) {
	if (provider != null) {
	  if(verifyBeforeAllocation(task,provider)){
	    AllocationResult estAR =  createPredictedAllocationResult(task);
            Allocation alloc;
            //either make an allocation or reset the estimated AR on the
            // already existing pe - mostly likely to happen if a task's prefs
            // changed.
            if (task.getPlanElement() == null) {
              alloc = buildAllocation(task, provider, providerRole);
              if (estAR != null){
                alloc.setEstimatedResult(estAR);
                if(inventoryPlugin.publishAdd(alloc)) {
                  if (logger.isDebugEnabled()) {
                    logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "ExtAlloc[" + inventoryPlugin.getSupplyType()+"]" +
                                 "publish Adding allocation: " + alloc.getUID()+
                                 " parent task is: " + task.getUID() + " publishAdd returned: true");
                  }
                  return true;
                }
                else {
                  logger.error("Unable to publish the allocation " + alloc);
                }
              }
            } else {
              alloc = (Allocation)task.getPlanElement();
              if (estAR != null){
                alloc.setEstimatedResult(estAR);
                if(inventoryPlugin.publishChange(alloc)) {
                  return true;
                }
                else {
                  logger.error("Unable to publish Change the allocation " + alloc);
                }
              }
            }
          } 
        }
        return false;
    }

  public void rescindTaskAllocations(Collection tasks) {
    Task task;
    Allocation alloc;
    Iterator taskIt = tasks.iterator();
    while (taskIt.hasNext()) {
      task = (Task) taskIt.next();
      alloc = (Allocation)task.getPlanElement();
      inventoryPlugin.publishRemove(alloc);
    }
  }
}
