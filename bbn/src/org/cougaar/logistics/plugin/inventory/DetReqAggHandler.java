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
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.Enumerator;

public class DetReqAggHandler extends InventoryModule{

  /** The aggMIL task found/created during the current transaction **/
  private Task aggMILTask = null;
  private HashMap MILTaskHash = new HashMap();

  public DetReqAggHandler(InventoryManager imPlugin) {
    super(imPlugin);
  }

  public void aggregateDetermineRequirementsTasks(IncrementalSubscription detReqSubscription,
						  IncrementalSubscription aggMILSubscription) {
    aggMILTask = null;
    if (detReqSubscription.hasChanged()) {
      aggregateDetermineRequirementsTasks((NewMPTask)getDetermineRequirementsTask(aggMILSubscription),
					  detReqSubscription.getAddedCollection());
    }
  }

  public void handleMILTasks(IncrementalSubscription milSubscription) {
    if (milSubscription.hasChanged()) {
      //Added tasks are handled at the time of creation
      removeMILTasks(milSubscription.getRemovedList());
    }
  }

  public void addMILTasks(Enumeration milTasks) {
    while (milTasks.hasMoreElements()) {
      Task task = (Task) milTasks.nextElement();
      if (logger.isDebugEnabled()) {
        logger.debug("Maintain Inv task being added... inserting into hashmap " + task.toString());
      }
      Inventory inventory = (Inventory) task.getDirectObject();
      MILTaskHash.put(inventory, task);
    }
  }

  private void removeMILTasks(Enumeration milTasks) {
    while (milTasks.hasMoreElements()) {
      Task task = (Task) milTasks.nextElement();
      if (logger.isDebugEnabled()) {
        logger.debug("Agent: " + inventoryPlugin.getClusterId().toString() + "DetReqHandler[" +
                     inventoryPlugin.getSupplyType() + "]" +
                     "Maintain Inv task being removed... cleaning out hashmap " + task.toString());
      }
      Inventory inventory = (Inventory) task.getDirectObject();
      MILTaskHash.remove(inventory);
    }
  }
  
  /**
     Get _the_ aggregate MIL task. This is complicated because we
     want to detect when the task has been deleted, but we only want
     to create one of them. The lag between publishing a new task
     and its appearance in the subscription poses a problem because,
     typically, this method is called repeatedly in one transaction.
     We store the task temporarily in a variable (aggMILTask) to
     prevent multiple creation, but clear the variable at the
     beginning of each new transaction. If the task has not yet been
     created, we try to create it by aggregating all the existing
     per-oplan DetermineRequirements tasks into it. Subsequent
     per-oplan tasks will be aggregated in as they arrive. There
     will be no task if there are no DetermineRequirements tasks to
     be aggregated.
  **/
  public Task getDetermineRequirementsTask(CollectionSubscription aggMILSubscription) {
    if (aggMILTask == null) {
      if (!aggMILSubscription.isEmpty()) {
        aggMILTask = (Task) aggMILSubscription.elements().nextElement();
      } 
    }
    return aggMILTask;
  }

  /**
     Aggregate some DetermineRequirements tasks
  **/
  private void aggregateDetermineRequirementsTasks(NewMPTask mpTask, Collection parents) {
    if (mpTask == null) return;
    NewComposition composition = (NewComposition) mpTask.getComposition();
    long minStartTime = 0;
    long maxEndTime = 0;
    Iterator parentIt = parents.iterator();
    while (parentIt.hasNext()) {
       try {
        maxEndTime = getTaskUtils().getEndTime(mpTask);
      } catch (IllegalArgumentException iae) {
        maxEndTime = Long.MIN_VALUE;
      }
      try {
        minStartTime = getTaskUtils().getStartTime(mpTask);
      } catch (IllegalArgumentException iae) {
        minStartTime = Long.MAX_VALUE;
      }
      Task parent = (Task) parentIt.next();
      if (parent.getPlanElement() != null) {
        logger.error("Det Req for MaintainInventory already disposed: " + parent);
      }
      minStartTime = Math.min(minStartTime, getTaskUtils().getStartTime(parent));
      maxEndTime = Math.max(maxEndTime, getTaskUtils().getEndTime(parent));
      AllocationResult estAR =
	PluginHelper.createEstimatedAllocationResult(parent, 
                                                     inventoryPlugin.getPlanningFactory(), 
                                                     1.0, true);
      Aggregation agg = inventoryPlugin.getPlanningFactory().
        createAggregation(parent.getPlan(), parent,
                          composition, estAR);
      composition.addAggregation(agg);      
      inventoryPlugin.publishAdd(agg);
    }
    setStartTimePreference(mpTask, minStartTime);
    setEndTimePreference(mpTask, maxEndTime);
    mpTask.setParentTasks(new Enumerator (composition.getParentTasks()));
    inventoryPlugin.publishAdd(mpTask);
    // create an expty expansion for the mp maintain inv task to be filled
    // in later with a MaintainInventory task for each maintained item
    createTopMIExpansion(mpTask);
    aggMILTask = mpTask;
  }

  /** Create an empty expansion pe for the top level maintain inventory task
   *  that will be filled in with Maintain inventory tasks for each
   *  Maintained item.
   *  @param parent The Top maintain inventory task
   **/
  private void createTopMIExpansion(Task parent) {
    PlanningFactory factory = inventoryPlugin.getPlanningFactory();
    // Create workflow
    NewWorkflow wf = (NewWorkflow)factory.newWorkflow();
    wf.setParentTask(parent);
    wf.setIsPropagatingToSubtasks(true);
    // Build Expansion
    AllocationResult estAR =
      PluginHelper.createEstimatedAllocationResult(parent, 
                                                   inventoryPlugin.getPlanningFactory(), 
                                                   1.0, true);
    Expansion expansion = factory.createExpansion(parent.getPlan(), parent, wf, estAR);
    // Publish Expansion
    inventoryPlugin.publishAdd(expansion);
  }
  
  /**
     Create a MaintainInventory task for an inventory. This task is
     the parent of all refill tasks for the inventory and is itself
     a subtask of the aggregated determine requirements tasks.
  **/
  private NewTask createMILTask(Task parent, Inventory inventory) {
    NewTask subtask = inventoryPlugin.getPlanningFactory().newTask();
    subtask.setContext(parent.getContext());
    subtask.setDirectObject(inventory);
    subtask.setParentTask(parent);
    subtask.setVerb(Verb.get(Constants.Verb.MAINTAININVENTORY));
    setStartTimePreference(subtask, getTaskUtils().getStartTime(parent));
    setEndTimePreference(subtask, getTaskUtils().getEndTime(parent));
    return subtask;
  }
  
  /**
     Create the aggregated maintain inventory task. This task is
     the parent of all the per-inventory MaintainInventory tasks.
     It, too, uses the MaintainInventory verb but with no direct
     object. It is an MPTask that combines all the
     DetermineRequirements tasks of type MaintainInventory. The
     Composition of this MPTask is non-propagating so it is
     rescinded only if all the parent tasks are rescinded.
   @see InventoryPlugin#processDetReq
  **/
  public NewMPTask createAggTask(Collection parents) {
    Task parentTask = null;
    Iterator tasks = parents.iterator();
    HashSet set = new HashSet();
    while (tasks.hasNext()) {
      parentTask = (Task)tasks.next();
      if (parentTask.getContext() != null) {
        set.addAll((ContextOfOplanIds)parentTask.getContext());
      }
    }
    NewMPTask mpTask = inventoryPlugin.getPlanningFactory().newMPTask();
    mpTask.setContext(new ContextOfOplanIds(set));
    NewComposition composition = inventoryPlugin.getPlanningFactory().newComposition();
    composition.setIsPropagating(false);
    mpTask.setComposition(composition);
    composition.setCombinedTask(mpTask);
    mpTask.setVerb(Verb.get(Constants.Verb.MAINTAININVENTORY));
    aggregateDetermineRequirementsTasks(mpTask, parents);
    return mpTask;
  }

  /**
     Find or make the aggregated MIL task for an inventory. If the
     MILTaskHash_ does not contain an existing task, create a new
     one and link it to the determine requirements tasks.
  **/
  public Task findOrMakeMILTask(Inventory inventory, CollectionSubscription aggMILSubscription) {
    // Creates the MaintainInventoryLevels Task for this item 
    // if one does not already exist
    NewTask milTask = (NewTask) MILTaskHash.get(inventory);
    if (milTask == null) {
      Task parent = getDetermineRequirementsTask(aggMILSubscription);
      if (parent == null || parent.getPlanElement() == null) {
        /**
         This might happen just after the last determine
         requirements task is removed. Because of inertia,
         the inventory manager might still be trying to
         refill the inventory although, in fact the demand
         for that inventory is in the process of
         disappearing. The caller, getting a null return will
         simply abandon the attempt to do the refill.
         **/
        reportWhenCantMakeMaintainInventoryTask(inventory, parent);
        return null; // Can't make one
      }
      milTask = createMILTask(parent, inventory);
      PlanElement pe = parent.getPlanElement();
      if (pe instanceof Expansion) {
        Expansion expansion =(Expansion)pe;
        NewWorkflow wf = (NewWorkflow)expansion.getWorkflow();
        wf.addTask(milTask);
        ((NewTask) milTask).setWorkflow(wf);
        // Publish new task
        inventoryPlugin.publishAdd(milTask);
        MILTaskHash.put(inventory, milTask);
        inventoryPlugin.publishChange(expansion);
        if (logger.isDebugEnabled()) {
          logger.debug("Created new Maintain Inv task: " + milTask.getUID() +
                       " Parent UID is: " + parent.getUID());
        }
      } else {
        logger.error("publish Change to MIL Top Expansion: problem pe not Expansion? "+pe +
                     " parent task is: " + parent.getUID());
      }
    }
    return milTask;
  }

  protected void reportWhenCantMakeMaintainInventoryTask(Inventory inventory, Task parent) {
    if (logger.isWarnEnabled()) {
      AssetUtils assetUtils = getAssetUtils();
      if (assetUtils == null) {
        logger.warn ("asset utils is null");
      }
      if (inventory == null) {
        logger.warn ("inventory is null");
      }
      ScheduledContentPG scheduledContentPG = inventory.getScheduledContentPG();
      if (scheduledContentPG == null) {
        logger.warn ("scheduled content for " + inventory + " is null");
      }
      PlanElement planElement = null;
      if (parent == null) {
        logger.warn ("parent is null");
      }
      else {
        planElement = parent.getPlanElement();
      }
      logger.warn("CANNOT CREATE MILTASK, no parent, parent pe or inventory: "
        + assetUtils.assetDesc(scheduledContentPG.getAsset()) +
        " PARENT TASK IS: " + parent + " PARENT PE IS: " + planElement);
    }
  }

  private void setStartTimePreference(NewTask mpTask, long newStartTime) {
    ScoringFunction sf;
    Preference pref;
    sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.START_TIME,
                                                                          newStartTime));
    pref = inventoryPlugin.getPlanningFactory().newPreference(AspectType.START_TIME, sf);
    mpTask.setPreference(pref);
    //    mpTask.setCommitmentDate(new Date(newStartTime));
  }

  private void setEndTimePreference(NewTask mpTask, long newEndTime) {
    ScoringFunction sf;
    Preference pref;
    sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.END_TIME,
                                                                          newEndTime));
    pref = inventoryPlugin.getPlanningFactory().newPreference(AspectType.END_TIME, sf);
    mpTask.setPreference(pref);
  }

  //Reset the task reference to AggMilTask if something was removed from top level MI subscription
  public void resetAggMITask() {
    aggMILTask = null;
  }
}
