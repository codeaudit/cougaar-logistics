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

import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.AllocationResultHelper;
import org.cougaar.planning.plugin.util.PluginHelper;

import java.util.*;

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

  private boolean allocateTask(Task task) {
    Organization provider = findBestSource(task);
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

  /** Figure out which organization supplying item is best for us. */
  private Organization findBestSource(Task task) {
    Enumeration support_orgs;
        if (getTaskUtils().isProjection(task)) {
            /* For a projection, should be time-phased as support
      changes over time. We try for one provider for the whole task, if that doesn't work
      we try to split it.*/
      support_orgs = getAssetUtils().getSupportingOrgs(inventoryPlugin.getMyOrganization(),
                                                providerRole,
                                                       getTaskUtils().getStartTime(task),
                                                       TaskUtils.getEndTime(task));
      // if we have no providers that cover the whole projection, split it and try again
      if (!support_orgs.hasMoreElements()) {
        HashMap provSchedMap = inventoryPlugin.relationshipScheduleMap();
        List howToSplit = inventoryPlugin.getNewTaskSplitTimes(task, provSchedMap);
        // if we can split it, do it, if not warn that we have no provider
        if (!howToSplit.isEmpty()) {
          Collection newPartialTasks = getTaskUtils().splitProjection(task, howToSplit, inventoryPlugin);
          allocateRefillTasks(newPartialTasks);
        } else {
          logger.warn("No "+providerRole+" for task " + task.getUID() + ", during ["+
                    getTimeUtils().dateString(getTaskUtils().getStartTime(task))+
                    "-" +
                    getTimeUtils().dateString(TaskUtils.getEndTime(task)) +"]"+". Will retry.");
              return null;
            }
		}
		    } else {
      support_orgs = getAssetUtils().getSupportingOrgs(inventoryPlugin.getMyOrganization(),
                                                       providerRole,
                                                       getTaskUtils().getEndTime(task));
      if (!support_orgs.hasMoreElements()) {
        logger.warn("No "+providerRole+" for task " + task.getUID() + ", during "+
                    getTimeUtils().dateString(getTaskUtils().getEndTime(task))+". Will retry.");
        return null;
			}
		    }
    // For now, returning the first supporting org during the time span
    return (Organization)support_orgs.nextElement();
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
	logger.warn(inventoryPlugin.getClusterId().toString() + message);
      else
	logger.warn(inventoryPlugin.getClusterId().toString() + message, e);
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
      asset = refill.getDirectObject();
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

    protected void findSourcesWarning(Task task) {
    //String itemId = task.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
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
