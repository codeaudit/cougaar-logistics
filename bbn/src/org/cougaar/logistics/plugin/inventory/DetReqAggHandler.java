/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.Enumerator;

public class DetReqAggHandler extends InventoryModule{

  /** The aggMIL task found/created during the current transaction **/
  private Task aggMILTask = null;
  private HashMap MILTaskHash = new HashMap();

  public DetReqAggHandler(InventoryPlugin imPlugin) {
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
      Inventory inventory = (Inventory) task.getDirectObject();
      MILTaskHash.put(inventory, task);
    }
  }

  private void removeMILTasks(Enumeration milTasks) {    
    while (milTasks.hasMoreElements()) {
      Task task = (Task) milTasks.nextElement();
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
                                                   0.25, true);
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
    subtask.setDirectObject(inventory);
    subtask.setParentTask(parent);
    subtask.setVerb(new Verb(Constants.Verb.MAINTAININVENTORY));
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
  **/
  public NewMPTask createAggTask(Collection parents) {
    NewMPTask mpTask = inventoryPlugin.getPlanningFactory().newMPTask();
    NewComposition composition = inventoryPlugin.getPlanningFactory().newComposition();
    composition.setIsPropagating(false);
    mpTask.setComposition(composition);
    composition.setCombinedTask(mpTask);
    mpTask.setVerb(new Verb(Constants.Verb.MAINTAININVENTORY));
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
      if (parent == null) {
	/**
	   This might happen just after the last determine
	   requirements task is removed. Because of inertia,
	   the inventory manager might still be trying to
	   refill the inventory although, in fact the demand
	   for that inventory is in the process of
	   disappearing. The caller, getting a null return will
	   simply abandon the attempt to do the refill.
	**/
	logger.error("CANNOT CREATE MILTASK, no parent, inventory: "
		     + getAssetUtils().assetDesc(inventory.getScheduledContentPG().getAsset()));	
	return null; // Can't make one
      }
      milTask = createMILTask(parent, inventory);
      PlanElement pe = parent.getPlanElement();
      if (pe instanceof Expansion) {
        Expansion expansion =(Expansion)pe;
        NewWorkflow wf = (NewWorkflow)expansion.getWorkflow();
        wf.addTask(milTask);
        ((NewTask) milTask).setWorkflow(wf);
        inventoryPlugin.publishChange(expansion);
      } else {
        logger.error("publish Change to MIL Top Expansion: problem pe not Expansion? "+pe);
      }
      // Publish new task
      if (!inventoryPlugin.publishAdd(milTask)) {
        logger.error("publishAdd(milTask) fail to publish task "+ getTaskUtils().taskDesc(milTask));
      }
      MILTaskHash.put(inventory, milTask);
    }
    return milTask;
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
  
}
