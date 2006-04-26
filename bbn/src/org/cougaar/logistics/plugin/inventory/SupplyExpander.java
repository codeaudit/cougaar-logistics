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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.AllocationResultHelper;
import org.cougaar.planning.plugin.util.PluginHelper;

import java.util.*;

public class SupplyExpander extends InventoryModule implements ExpanderModule {

  /**
   * Define an ARA that can deal with the expansion of a
   * ProjectSupply task. Mostly, we just clone the result of the
   * ProjectWithdraw task.
   **/
  protected static class ProjectionARA implements AllocationResultAggregator {
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      if (tst.size() != 1)
        throw new IllegalArgumentException("projectionARA: multiple subtasks");
      AllocationResult ar = (AllocationResult) tst.getAllocationResult(0);
      if (ar == null) return null;
      if (ar.isEqual(currentar)) return currentar;
      return (AllocationResult) ar.clone();
    }
  }

  protected static class SupplyARA implements AllocationResultAggregator {
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      AspectValue[] merged = new AspectValue[AlpineAspectType.LAST_ALPINE_ASPECT + 1];
      boolean success = true;
      float rating = 0.0f;
      int tstSize = tst.size();
      AllocationResult withdrawAR = null; // Remember this when we see it
      Task parentTask = wf.getParentTask();
      AllocationResultHelper helper = new AllocationResultHelper(parentTask, null);
      AllocationResult bestAR = helper.getAllocationResult();
      AspectValue[] curr = bestAR.getAspectValueResults();

      for (int i = 0; i < curr.length; i++) {
        AspectValue av = curr[i];
        int type = av.getAspectType();
        merged[type] = av;
      }
      for (int i = 0; i < tstSize; i++) {
        AllocationResult ar = tst.getAllocationResult(i);
        if (ar == null) return null; // bail if undefined
        Task t = tst.getTask(i);
        Verb verb = t.getVerb();
        boolean isWithdraw =
            verb.equals(Constants.Verb.Withdraw)
            || verb.equals(Constants.Verb.ProjectWithdraw);
        if (isWithdraw) {
          if (ar == null) return null;
          withdrawAR = ar;
        }
        AspectValue[] avs = ar.getAspectValueResults();
        success = success && ar.isSuccess();
        rating += ar.getConfidenceRating();
        for (int j = 0; j < avs.length; j++) {
          int type = avs[j].getAspectType();
          switch (type) {
            case AspectType.START_TIME:
              break;
            case AspectType.END_TIME:
              break;
            case AspectType.QUANTITY:
              if (isWithdraw) merged[AspectType.QUANTITY] = avs[j];
              break;
            default:
              if (!isWithdraw) merged[type] = avs[j];
          }
        }
      }
      List mergedPhasedResults = new ArrayList();
      // this seems strange why do we assume everything is phased?
      //for now check if this is null (as all the isphased seem to be true
      // and if it is null get the nonphased results.
      List withdrawPhasedResults = withdrawAR.getPhasedAspectValueResults();
      if (withdrawPhasedResults != null) {
        for (int i = 0, n = withdrawPhasedResults.size(); i < n; i++) {
          AspectValue[] oneResult = (AspectValue[]) withdrawPhasedResults.get(i);
          mergedPhasedResults.add(merge(merged, oneResult));
        }
      } else {
        AspectValue[] npresult = (AspectValue[]) withdrawAR.getAspectValueResults();
        mergedPhasedResults.add(merge(merged, npresult));
      }
      return new AllocationResult(rating / tstSize, success,
                                  merge(merged, null), mergedPhasedResults);
    }

    /**
     * Merges an array of AspectValue indexed by AspectType and an
     * unindexed array of AspectValues into an unindexed array of
     * AspectValues.
     **/
    protected AspectValue[] merge(AspectValue[] rollup, AspectValue[] phased) {
      if (phased != null) {
        rollup = (AspectValue[]) rollup.clone(); // Don't clobber the original
        for (int i = 0; i < phased.length; i++) {
          AspectValue av = phased[i];
          if (av != null) rollup[av.getAspectType()] = av;
        }
      }
      int nAspects = 0;
      for (int i = 0; i < rollup.length; i++) {
        if (rollup[i] != null) nAspects++;
      }
      AspectValue[] result = new AspectValue[nAspects];
      int aspect = 0;
      for (int i = 0; i < rollup.length; i++) {
        if (rollup[i] != null) result[aspect++] = rollup[i];
      }
      return result;
    }
  }

  protected static final long MSEC_PER_MIN =  60 * 1000;
  protected static final long MSEC_PER_HOUR = MSEC_PER_MIN *60;
  public static final long  DEFAULT_ORDER_AND_SHIPTIME = 24 * MSEC_PER_HOUR; // second day

  public static final Verb WITHDRAWVERB =Constants.Verb.Withdraw;
  public static final Verb PROJECTWITHDRAWVERB = Constants.Verb.ProjectWithdraw;
  public static final Verb TRANSPORTVERB = Constants.Verb.Transport;

  //private Organization myOrg;
  protected boolean addTransport; // Add load tasks when expanding supply tasks
  protected long ost;
  protected static AllocationResultAggregator projectionARA = new ProjectionARA();
  protected static AllocationResultAggregator supplyARA = new SupplyARA();

  protected MessageAddress clusterId;

  public SupplyExpander(InventoryManager imPlugin) {
    super(imPlugin);
    ost = DEFAULT_ORDER_AND_SHIPTIME;  //In the future plugin should supply from suppliers predictor the OST - MWD
    addTransport = false;
    clusterId = imPlugin.getClusterId();
  }

  public boolean expandAndDistributeProjections(Collection tasks) {
    boolean newProjections = false;
    LogisticsInventoryPG logInvPG;
    Task aTask, wdrawTask;
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      aTask = (Task)taskIter.next();
      wdrawTask = expandDemandTask(aTask, createProjectWithdrawTask(aTask));
      logInvPG = getLogisticsInventoryPG(wdrawTask);
      if (logInvPG != null) {
        logInvPG.addWithdrawProjection(wdrawTask);
        // if we have atleast one new projection - set this to true.
        newProjections = true;
      }
      ((NewWorkflow)wdrawTask.getWorkflow()).setAllocationResultAggregator(projectionARA);
    }
    return newProjections;
  }

  public void expandAndDistributeRequisitions(Collection tasks) {
    LogisticsInventoryPG logInvPG;
    Task aTask, wdrawTask;
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      aTask = (Task) taskIter.next();
      wdrawTask = expandDemandTask(aTask, createWithdrawTask(aTask));
      logInvPG = getLogisticsInventoryPG(wdrawTask);
      if (logInvPG != null) {
        logInvPG.addWithdrawRequisition(wdrawTask);
      }
      ((NewWorkflow)wdrawTask.getWorkflow()).setAllocationResultAggregator(supplyARA);
    }
  }

  public void handleRemovedRequisitions(Collection tasks) {
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      Task aTask = (Task)taskIter.next();
      if (logger.isDebugEnabled()) {
        logger.debug("SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
                     "processing removal of requisition: " + aTask.getUID());
      }
      LogisticsInventoryPG thePG = getLogisticsInventoryPG(aTask);
      if (thePG != null) {
        thePG.removeWithdrawRequisition(aTask);
      }
    }
  }

  public void handleRemovedDispositions(Collection dispositions) {
    // This SupplyExpander does nothing for removed dispositions
  }

  public boolean handleRemovedProjections(Collection tasks) {
    boolean removedProjections = false;
    LogisticsInventoryPG thePG;
    Task aTask;
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      aTask = (Task)taskIter.next();
      if (logger.isDebugEnabled()) {
        logger.debug("SupplyExpander[" + inventoryPlugin.getSupplyType()+"]" +
                     "processing removal of projection: " + aTask.getUID());
      }
      thePG = getLogisticsInventoryPG(aTask);
      if (thePG != null) {
        thePG.removeWithdrawProjection(aTask);
        if (!aTask.isDeleted()) {
          removedProjections = true;
        } 
//         else {
//           Inventory inventory = inventoryPlugin.findOrMakeInventory(thePG.getResource());
//           inventoryPlugin.touchInventoryWithDeletions(inventory);
//         }
      }
    }
    return removedProjections;
  }

  public void updateChangedRequisitions(Collection tasks) {
    LogisticsInventoryPG thePG;
    Expansion exp;
    Task task, supply;
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      supply = (Task)taskIter.next();
      if (logger.isDebugEnabled()) {
        logger.debug("SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
                     "processing changed reqs task: " + supply.getUID());
      }
      if (supply.getPlanElement() instanceof Expansion) {
        exp = (Expansion)supply.getPlanElement();
        Workflow wf = exp.getWorkflow();
        Enumeration subTasks = wf.getTasks();
        while (subTasks.hasMoreElements()) {
          task = (Task)subTasks.nextElement();
          thePG = getLogisticsInventoryPG(supply);
          if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
            if((getTaskUtils().getEndTime(task) < inventoryPlugin.getOPlanArrivalInTheaterTime()) && (logger.isErrorEnabled())) {
              logger.error("At " + inventoryPlugin.getOrgName() + "- Requisition Task:" +
                           task.getUID() + " item: " + getTaskUtils().getTaskItemName(task) +
                           " has an endTime of " +
                           getTimeUtils().dateString(getTaskUtils().getEndTime(task)) +
                           " which is before this orgs arrival time of " +
                           getTimeUtils().dateString(inventoryPlugin.getOPlanArrivalInTheaterTime()));
            }
            if (thePG != null) {
              thePG.updateWithdrawRequisition(task);
              synchronized (supply) {
                ((NewTask)task).setPreferences(supply.getPreferences());
              }
              inventoryPlugin.publishChange(task);
            }
          } else if (task.getVerb().equals(Constants.Verb.TRANSPORT)) {
            ((NewTask)task).setPrepositionalPhrases(supply.getPrepositionalPhrases());
            inventoryPlugin.publishChange(task);
          }
        }
      }
    }
  }

  public void updateChangedProjections(Collection tasks) {
    LogisticsInventoryPG thePG;
    Expansion exp;
    Task projSupply, task;
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      projSupply = (Task)taskIter.next();
      if (logger.isDebugEnabled()) {
        logger.debug("SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
                     "processing changed projections task: " + projSupply.getUID());
      }
      if (projSupply.getPlanElement() instanceof Expansion) {
        exp = (Expansion)projSupply.getPlanElement();
        Workflow wf = exp.getWorkflow();
        Enumeration subTasks = wf.getTasks();
        while (subTasks.hasMoreElements()) {
          task = (Task)subTasks.nextElement();
          if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
            thePG = getLogisticsInventoryPG(projSupply);
            if((getTaskUtils().getStartTime(projSupply) < inventoryPlugin.getOPlanArrivalInTheaterTime()) && (logger.isErrorEnabled())) {
              logger.error("At " + inventoryPlugin.getOrgName() +
                           "- Projection Task:" + task.getUID() + " item: " +
                           getTaskUtils().getTaskItemName(projSupply) + " has an start time of " +
                           getTimeUtils().dateString(getTaskUtils().getStartTime(projSupply)) +
                           " which is before this orgs arrival time of " +
                           getTimeUtils().dateString(inventoryPlugin.getOPlanArrivalInTheaterTime()));
            }
            if (thePG != null) {
              thePG.removeWithdrawProjection(task);
              if (task.getPlanElement() != null) {
                inventoryPlugin.publishRemove(task.getPlanElement());
              }
              synchronized (projSupply) {
                ((NewTask)task).setPreferences(projSupply.getPreferences());
              }
              inventoryPlugin.publishChange(task);
              //BD Why is this here?  We never removed the task from the wf???
              // commenting this code out
              //((NewWorkflow)wf).addTask(task);
              thePG.addWithdrawProjection(task);
              //inventoryPlugin.publishChange(wf);
            }
          } else if (task.getVerb().equals(Constants.Verb.TRANSPORT)) {
            ((NewTask)task).setPrepositionalPhrases(projSupply.getPrepositionalPhrases());
            inventoryPlugin.publishChange(task);
          }
        }
      }
    }
  }

  public LogisticsInventoryPG getLogisticsInventoryPG(Task wdrawTask) {
	LogisticsInventoryPG logInvPG = null;
	Asset asset = (Asset)wdrawTask.getDirectObject();
	Inventory inventory = inventoryPlugin.findOrMakeInventory(asset);
    inventoryPlugin.touchInventoryForTask(wdrawTask, inventory);
	logInvPG = (LogisticsInventoryPG)
	    inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
	return logInvPG;
  }

  public void updateExpandedTasks(Collection changedTasks) {
    Task aTask;
    Iterator tasksIter = changedTasks.iterator();
    while (tasksIter.hasNext()) {
      aTask = (Task)tasksIter.next();
    }
  }

  protected Task expandDemandTask(Task parentTask, Task withdrawTask) {
    Vector expand_tasks = new Vector();
    expand_tasks.addElement(withdrawTask);
    NewTask transportTask = null;
    if (addTransport) {
      transportTask = createTransportTask(parentTask, withdrawTask);
      expand_tasks.addElement(transportTask);
    }
    Expansion expansion = PluginHelper.wireExpansion(parentTask, expand_tasks, inventoryPlugin.getPlanningFactory());
    inventoryPlugin.publishAddExpansion(expansion);
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    wf.setAllocationResultAggregator(new AllocationResultAggregator.DefaultARA());
    return withdrawTask;
  }

  protected NewTask createWithdrawTask(Task parentTask) {

    NewTask subtask = createVanillaWithdrawTask(parentTask);

    //This method does the Supply specific stuff

    long anticipation = 0L;
    if (addTransport) anticipation += ost;

    subtask.setVerb(WITHDRAWVERB);
    //TODO Figure out what to offset the task by for transport
    //addEndTimePref(subtask, getTaskUtils().getEndTime(parentTask) - anticipation);

    return subtask;
  }

  protected NewTask createProjectWithdrawTask(Task parentTask) {

    NewTask subtask = createVanillaWithdrawTask(parentTask);

    //This method does the ProjectSupply specific stuff

    long anticipation = 0L;
    if (addTransport) anticipation += ost;

    subtask.setVerb(PROJECTWITHDRAWVERB);
    //design issue:
    //MWD do we build in ost anticipation to end time pref
    //like above if there is a
    //PROJECTTRANSPORT in theatre transportation.

    return subtask;
  }

  /** creates a Withdraw task from a Supply task **/
  protected NewTask createVanillaWithdrawTask(Task parentTask) {

    // Create new task
    Asset prototype = parentTask.getDirectObject();
    NewTask subtask = inventoryPlugin.getPlanningFactory().newTask();
    // attach withdraw task to parent and fill it in
    subtask.setDirectObject( prototype);
    subtask.setParentTask( parentTask );
    subtask.setPlan( parentTask.getPlan() );
    subtask.setPrepositionalPhrases( parentTask.getPrepositionalPhrases() );
    subtask.setPriority(parentTask.getPriority());
    subtask.setSource( clusterId );

    // Copy all preferences
    synchronized (parentTask) {
      subtask.setPreferences(parentTask.getPreferences());
    }
    return subtask;
  }

  /** creates a Transport or ProjectTransport task from a Supply and Withdraw
   ** or ProjectSupply and ProjectWithdraw task.
   ** Must fill in.
   **/
  protected NewTask createTransportTask(Task parentTask, Task wdraw_task) {
    return null;
  }

  public void updateAllocationResult(IncrementalSubscription sub) {
    Iterator subIt = sub.iterator();
    while (subIt.hasNext()) {
      PlanElement pe = (PlanElement) subIt.next();
      if (logger.isDebugEnabled()) {
        logger.debug("SupplyExp[" +
                     inventoryPlugin.getSupplyType()+"]" +
                     "checking AR change: " + pe.getUID()+
                     " parent task is: " + pe.getTask().getUID());
      }
      if (PluginHelper.updatePlanElement(pe)) {
        inventoryPlugin.publishChange(pe);
        if (logger.isDebugEnabled()) {
          logger.debug("SupplyExp[" +
                       inventoryPlugin.getSupplyType()+"]" +
                       "publish Changing expansion due to AR change: " + pe.getUID()+
                       " parent task is: " + pe.getTask().getUID());
        }
      }
    }
  }

  public void checkCommStatusAlarms() {
    // This SupplyExpander doesn't check for communications loss
  }

  public void determineCommStatus(IncrementalSubscription commStatusSub, Collection tasks) {
    //This SupplyExpander doesn't check for communications loss
  }
}



