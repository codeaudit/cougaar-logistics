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
import org.cougaar.core.blackboard.Transaction;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
//import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AllocationResultAggregator;
import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewPlanElement;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskScoreTable;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.plugin.util.AllocationResultHelper;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;

public class SupplyExpander extends InventoryModule implements ExpanderModule {

  /**
   * Define an ARA that can deal with the expansion of a
   * ProjectSupply task. Mostly, we just clone the result of the
   * ProjectWithdraw task.
   **/
  private static class ProjectionARA implements AllocationResultAggregator {
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      if (tst.size() != 1)
        throw new IllegalArgumentException("projectionARA: multiple subtasks");
      AllocationResult ar = (AllocationResult) tst.getAllocationResult(0);
      if (ar == null) return null;
      if (ar.isEqual(currentar)) return currentar;
      return (AllocationResult) ar.clone();
    }
  }

  private static class SupplyARA implements AllocationResultAggregator {
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      AspectValue[] merged = new AspectValue[AlpineAspectType.LAST_ALPINE_ASPECT + 1];
      long startTime = Long.MAX_VALUE;
      long endTime = Long.MIN_VALUE;
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
        switch (type) {
          case START_TIME:
            startTime = (long) av.getValue();
            break;
          case END_TIME:
            endTime = (long) av.getValue();
            break;
        }
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
    private AspectValue[] merge(AspectValue[] rollup, AspectValue[] phased) {
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

  private static class SupplyTaskKey {
    private long endTime;
    private Object customer;
    private int hc;

    public SupplyTaskKey(Task aTask) {
      endTime = TaskUtils.getEndTime(aTask);
      customer = TaskUtils.getCustomer(aTask);
      hc = ((int) endTime) ^ ((int) (endTime >> 32)) ^ customer.hashCode();
    }
    public int hashCode() {
      return hc;
    }
    public boolean equals(Object o) {
      if (o instanceof SupplyTaskKey) {
        SupplyTaskKey that = (SupplyTaskKey) o;
        return this.endTime == that.endTime && this.customer.equals(that.customer);
      }
      return false;
    }
  }

  protected static final long MSEC_PER_MIN =  60 * 1000;
  protected static final long MSEC_PER_HOUR = MSEC_PER_MIN *60;
  public static final long  DEFAULT_ORDER_AND_SHIPTIME = 24 * MSEC_PER_HOUR; // second day

  public static final Verb                 WITHDRAWVERB = Verb.get(Constants.Verb.WITHDRAW);
  public static final Verb                 PROJECTWITHDRAWVERB = Constants.Verb.ProjectWithdraw;
  public static final Verb                 TRANSPORTVERB = Verb.get(Constants.Verb.TRANSPORT);

  //private Organization myOrg;
  protected boolean addTransport; // Add load tasks when expanding supply tasks
  private long ost;
  private static AllocationResultAggregator projectionARA = new ProjectionARA();
  private static AllocationResultAggregator supplyARA = new SupplyARA();

  private MessageAddress clusterId;

  private Map supplyTaskOfTaskKey;
  private Map supplyTaskKeyOfTaskUID = new HashMap();

  public SupplyExpander(InventoryPlugin imPlugin) {
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
      SupplyTaskKey key = getSupplyTaskKey(aTask);
      Task existingTask = getExistingTask(key);
      if (isPrediction(aTask)) {
        if (existingTask != null) {
          if (logger.isWarnEnabled()) {
            logger.warn("SupplyExpander rescinding redundant prediction: " + aTask);
          } else if (logger.isDebugEnabled()) {
            logger.debug("SupplyExpander rescinding unnecessary predicton: " + aTask);
          }
          inventoryPlugin.publishRemove(aTask); // Probably a bug
          continue;
        }
        if (logger.isDebugEnabled()) {
          logger.debug("SupplyExpander adding prediction: " + aTask);
        }
        putExistingTask(key, aTask);
      } else {
        putExistingTask(key, aTask); // Regardless of what follows, this is now the prevailing task for this slot
        if (existingTask != null && isPrediction(existingTask)) {
          // Maybe we can use the withdraw of the prediction
          NewPlanElement pe = (NewPlanElement) existingTask.getPlanElement();
          inventoryPlugin.publishRemove(existingTask);
          if (logger.isDebugEnabled()) {
            logger.debug("SupplyExpander replacing prediction with: " + aTask);
          }
          if (pe != null) {
            pe.resetTask(aTask);
            // Cause the existing estimated AllocationResult of the expansion to be sent for the new task.
            Transaction.noteChangeReport(pe, new PlanElement.EstimatedResultChangeReport());
            inventoryPlugin.publishChange(pe);
            continue;     // No need to process the task itself
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Adding supply task " + aTask);
          }
        }
      }
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
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
                     "processing removal of requisition: " + aTask.getUID());
      }
      LogisticsInventoryPG thePG = getLogisticsInventoryPG(aTask);
      if (thePG != null) {
        thePG.removeWithdrawRequisition(aTask);
      }
    }
  }

  public void handleRemovedRealRequisitions(Collection tasks) {
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      Task aTask = (Task)taskIter.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
                     "processing removal of real requisition: " + aTask.getUID());
      }
      SupplyTaskKey key = getSupplyTaskKey(aTask);
      Task existingTask = getExistingTask(key);
      if (existingTask != null) {
        if (logger.isDebugEnabled()) {
          if (isPrediction(existingTask)) {
            logger.debug("SupplyExpander removing prediction: " + existingTask);
          } else {
            logger.debug("SupplyExpander removing requisition: " + existingTask);
          }
        }
        removeExistingTask(key);
      }
    }
  }

  public boolean handleRemovedProjections(Collection tasks) {
    boolean removedProjections = false;
    LogisticsInventoryPG thePG;
    Task aTask;
    Iterator taskIter = tasks.iterator();
    while (taskIter.hasNext()) {
      aTask = (Task)taskIter.next();
      if (logger.isDebugEnabled()) {
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "SupplyExpander[" + inventoryPlugin.getSupplyType()+"]" +
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
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
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
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
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
    // we might be looking at pass-thru's from the change list
    Inventory inventory = inventoryPlugin.findOrMakeInventory(asset);
    if (inventory != null) {
      if (!wdrawTask.isDeleted()) {
        inventoryPlugin.touchInventory(inventory);
      } else {
        inventoryPlugin.touchInventoryWithDeletions(inventory);
      }
      logInvPG = (LogisticsInventoryPG)
          inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
    }
    return logInvPG;
  }

  public void updateExpandedTasks(Collection changedTasks) {
    Task aTask;
    Iterator tasksIter = changedTasks.iterator();
    while (tasksIter.hasNext()) {
      aTask = (Task)tasksIter.next();
    }
  }


  private static boolean isPrediction(Task aTask) {
    PrepositionalPhrase for_pp = aTask.getPrepositionalPhrase(Constants.Preposition.FOR);
    String forOrgName = (String) for_pp.getIndirectObject();
    String fromOrgName = aTask.getSource().toString();
    return !forOrgName.equals(fromOrgName);
  }

  /**
   * Create the supplyTaskMap if necessary. We populate the map with
   * the current prevailing task for every particular customer/time.
   * The prevailing task is always the one task having a PlanElement
   * (Expansion). No further checking is required
   **/
  private synchronized Map getSupplyTaskOfTaskKey() {
    if (supplyTaskOfTaskKey == null) {
      supplyTaskOfTaskKey = new HashMap();
      Iterator i = inventoryPlugin.getSupplyTaskScheduler().iterator();
      while (i.hasNext()) {
        Task aTask = (Task) i.next();
        // Ignore tasks without plan elements for now; they will be processed later.
        if (aTask.getPlanElement() == null) continue;
        SupplyTaskKey key = getSupplyTaskKey(aTask);
        supplyTaskOfTaskKey.put(key, aTask);
        supplyTaskKeyOfTaskUID.put(aTask.getUID(), key);
      }
    }
    return supplyTaskOfTaskKey;
  }

  private Task getExistingTask(SupplyTaskKey key) {
    return (Task) getSupplyTaskOfTaskKey().get(key);
  }

  private void putExistingTask(SupplyTaskKey key, Task aTask) {
    getSupplyTaskOfTaskKey().put(key, aTask);
  }

  private void removeExistingTask(SupplyTaskKey key) {
    getSupplyTaskOfTaskKey().remove(key);
  }

  private SupplyTaskKey getSupplyTaskKey(Task aTask) {
    UID uid = aTask.getUID();
    SupplyTaskKey key = (SupplyTaskKey) supplyTaskKeyOfTaskUID.get(uid);
    if (key == null) {
      key = new SupplyTaskKey(aTask);
      supplyTaskKeyOfTaskUID.put(uid, key);
    }
    return key;
  }

  private Task expandDemandTask(Task parentTask, Task withdrawTask) {
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
      if (PluginHelper.updatePlanElement(pe)) {
        boolean didPubChg = inventoryPlugin.publishChange(pe);
        if (logger.isDebugEnabled()) {
          logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "SupplyExp[" + inventoryPlugin.getSupplyType()+"]" +
                       "publish Changing expansion due to AR change: " + pe.getUID()+
                       " parent task is: " + pe.getTask().getUID() + " publishChange returned: " + didPubChg);
        }
      }
    }
  }
}



