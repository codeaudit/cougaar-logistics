/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

import org.cougaar.glm.ldm.asset.Inventory;

import org.cougaar.core.plugin.util.PluginHelper;

import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Role;

/** AllocationAssessor module is a module of the InventoryPlugin looks at
 *  Refill results and Inventory levels to allocate Withdraws 
 *  against the Inventories.
 *  Right now this is implemented with first come first serve, but it
 *  should be changed to allocate withdraws that have the highest score
 *  first where the score is something like quantity * time late or
 *  scoring function scores.
 *  Note that this allocator does NOT allocate split shipments.
 **/

public class AllocationAssessor extends InventoryLevelGenerator {
  private transient ArrayList trailingPointers = new ArrayList();
  private Role myRole;

  /** Constructor for this module
   *  @param imPlugin The Plugin calling this module.
   *  @param role  The role the Plugin is playing.
   **/
  public AllocationAssessor(InventoryPlugin imPlugin, Role role) {
    super(imPlugin);
    myRole = role;    
  }

  /** Called by the InventoryPlugin when we are processing in Backwards Flow
   *  (which is allocation result notifications) to try and allocated
   *  withdraw tasks.  It also updates the BG's Inventory Levels.
   *  @param inventories  The collection of inventories to be processed
   **/
  public void reconcileInventoryLevels(Collection inventories) {
    Iterator inv_list = inventories.iterator();
    long today = inventoryPlugin.getCurrentTimeMillis();
    int today_bucket;
    Inventory inventory;
    LogisticsInventoryPG thePG;
    while (inv_list.hasNext()) {
      // clear out the trailing pointers every time we get another inventory
      trailingPointers.clear();
      inventory = (Inventory)inv_list.next();
      thePG = (LogisticsInventoryPG)
        inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
      today_bucket = thePG.convertTimeToBucket(today);
      reconcileThePast(today_bucket, thePG);
      //TODO figure out what the end is
      // AF - we should get this from the BG (PG)
      int end_bucket = 180;
      int lastRefillBucket = thePG.getLastRefillRequisitionBucket();
      if (today_bucket < lastRefillBucket) {
        createWithdrawAllocations(today_bucket, lastRefillBucket, inventory, thePG);
      }
      createWithdrawProjectionAllocations(lastRefillBucket+1, end_bucket, inventory, thePG);
    }
  }

  /** Update the inventory levels from time zero to today.
   *  @param today_bucket  Representation of today.
   *  @param thePG The PG for the Inventory Asset we are working with.
   **/
  private void reconcileThePast(int today_bucket, LogisticsInventoryPG thePG) {
    calculateInventoryLevels(0, today_bucket, thePG);
  }

  /** Create and update Withdraw Task Allocations for a particular Inventory
   *  @param todayBucket This is the starting bucket to process
   *  @param endBucket Whats the last valid bucket for the inventory
   *  @param inv The Inventory we are processing
   *  @param the PG This is the PG for the Inventory we are processing
   **/
  private void createWithdrawAllocations(int todayBucket, int endBucket, 
                                        Inventory inv, LogisticsInventoryPG thePG) {
    int currentBucket = todayBucket;
    double qty = 0;
    double runningQty = 0;
    double todayLevel, yesterdayLevel, todayRefill, testLevel, testQty;

    // loop through the buckets in the inventory
    while (currentBucket <= endBucket) {

      yesterdayLevel = thePG.getLevel(currentBucket - 1);
      todayRefill = findCommittedRefill(currentBucket, thePG);
      runningQty = 0;

      Collection wdTasks = thePG.getActualDemandTasks(currentBucket);

      if (! trailingPointers.isEmpty()) {
        //process any withdraws from previous days that we haven't been able
        // to allocate - if we process some return their qty to take them into 
        // account for today's levels.
        runningQty = processLateWithdraws(currentBucket, 
                                          yesterdayLevel+todayRefill, inv, thePG);
      }
      
      Iterator wdIter = wdTasks.iterator();
      while(wdIter.hasNext()) {
        Task withdraw = (Task)wdIter.next();
        qty = getTaskUtils().getPreference(withdraw, AspectType.QUANTITY);
        // check the level
        testQty = runningQty + qty;
        testLevel = yesterdayLevel - testQty + todayRefill;
        if (testLevel >= 0) {
          // if its ok give it a pe and update the quantity
          runningQty = runningQty + qty;
          if (withdraw.getPlanElement() == null) {
            createBestAllocation(withdraw, inv, thePG);
          } else {
            checkPlanElement(withdraw, thePG);
          }
        } else {
          //if we can't fulfill this withdraw add it for later
          trailingPointers.add(withdraw);
          // if it already has a pe - should we rescind it?
          // if (pe != null) publishRemove(pe);
        }
      }
      //when we are done going through all the tasks for the day set the level
      todayLevel = yesterdayLevel - runningQty + todayRefill;
      thePG.setLevel(currentBucket, todayLevel);
      currentBucket = currentBucket + 1;
    }
  }

  /** Process Withdraws that we are filling Late because the inventory
   *  level went to 0 at some point.
   *  @param currentBucket Representation of the day we are trying to fill the withdraw on
   *  @param level  Representation of Today's inventory level (level+refill)
   *  @param inv  The Inventory we are allocating against.
   *  @param thePG  The PG of the Inventory
   **/
  private double processLateWithdraws(int currentBucket, double level, 
                                      Inventory inv, LogisticsInventoryPG thePG) {
    double filled = 0;
    double qty, checkQty;
    ArrayList processed = new ArrayList();
    // need to go through and calculate scores for the late withdraws and 
    //allocate the high scores first.
    // should be based on scoring functions or TimeLate * Quantity.
    // for now just do first come, first serve
    Iterator tpIter = trailingPointers.iterator();
    while (tpIter.hasNext()) {
      Task task = (Task) tpIter.next();
      qty = getTaskUtils().getPreference(task, AspectType.QUANTITY);
      checkQty = filled + qty;
      if ((level - checkQty) >= 0) {
        //TODO - should the Alloc date be the start time of the currentBucket
        // or should it be the end time??
        createLateAllocation(task, thePG.convertBucketToTime(currentBucket), 
                             inv, thePG);
        filled = filled + qty;
        processed.add(task);
      } else {
        // we could break here, but its conceivable that the next late
        // withdraw could be filled if its quantity is less
        // when we put in spit deliveries, this will be taken care of.
      }
    }
    // remove the processed tasks from the trailingPointers collection
    if (!processed.isEmpty()) {
      trailingPointers.removeAll(processed);
    }
    return filled;
  }

  /** Create and update Project Withdraw Task Allocations for a particular Inventory
   *  @param startBucket This is the starting bucket to process
   *  @param endBucket Whats the last valid bucket for the inventory
   *  @param inv The Inventory we are processing
   *  @param the PG This is the PG for the Inventory we are processing
   **/
  private void createWithdrawProjectionAllocations(int startBucket, int endBucket, 
                                                   Inventory inv, 
                                                   LogisticsInventoryPG thePG) {
    //calculate levels by adding in all refill projection results
    // go through each projectwithdraw - allocate and reset the inventory?

  }

  /** Utility method to create an Allocation that matches the
   *  best preferences for the withdraw task
   *  @param withdraw The withdraw task we are allocating
   *  @param inv The Inventory we are allocating against
   *  @param thePG The PG of the Inventory we are allocating against
   **/
  private void createBestAllocation(Task withdraw, Inventory inv, 
                                    LogisticsInventoryPG thePG) {
    AllocationResult estimatedResult = PluginHelper.
      createEstimatedAllocationResult(withdraw, inventoryPlugin.getRootFactory(), 
                                      0.9, true);
    Allocation alloc = inventoryPlugin.getRootFactory().
      createAllocation(withdraw.getPlan(), withdraw,
                       inv, estimatedResult, myRole);
    inventoryPlugin.publishAdd(alloc);
    thePG.updateWithdrawRequisition(withdraw);
  }

  /** Utility method to create a late Allocation
   *  @param withdraw The withdraw task to allocate
   *  @param time The time that it will be filled
   *  @param inv  The Inventory we are allocating against
   *  @param thePG  The PG for the Inventory we are allocating against
   **/
  private void createLateAllocation(Task withdraw, long time, Inventory inv, 
                                    LogisticsInventoryPG thePG) {
    int aspectTypes[] = {AspectType.END_TIME, AspectType.QUANTITY};
    double results[] = new double[2];
    results[0] = (double) time;
    results[1] = getTaskUtils().getPreference(withdraw, AspectType.QUANTITY);
    AllocationResult estimatedResult = inventoryPlugin.getRootFactory().
      newAllocationResult(0.9, true, aspectTypes, results);
    Allocation lateAlloc = inventoryPlugin.getRootFactory().
      createAllocation(withdraw.getPlan(), withdraw, inv, 
                        estimatedResult, myRole);
    inventoryPlugin.publishAdd(lateAlloc);
    thePG.updateWithdrawRequisition(withdraw);
  }

  /** Method which checks a previously created planelement for the withdraw task
   *  to make sure its consistent with the result we just calculated.
   *  This is called if we want to give a best result - so if the previous
   *  result was not best we will change it.
   *  @param withdraw The Withdraw Task we are allocating against the Inventory
   *  @param thePG  The PG for the Inventory this allocation is against.
   **/
  private void checkPlanElement(Task withdraw, LogisticsInventoryPG thePG) {
    //if this task already has a pe - make sure the results are consistent
    // with best.
    PlanElement pe = withdraw.getPlanElement();
    AllocationResult ar = pe.getEstimatedResult();
    double arEnd, taskEnd, arQty, taskQty;
    boolean correct = false;
    if (ar != null) {
      arEnd = ar.getValue(AspectType.END_TIME);
      arQty = ar.getValue(AspectType.QUANTITY);
      taskEnd = getTaskUtils().getPreference(withdraw, AspectType.END_TIME);
      taskQty = getTaskUtils().getPreference(withdraw, AspectType.QUANTITY);
      if ( (arEnd == taskEnd) && (arQty == taskQty) ) {
        correct = true;
      }
    } 
    //if the existing allocation result was not best or the 
    //Allocation result was null, make a new allocation result
    if (!correct) {
      AllocationResult estimatedResult = PluginHelper.
        createEstimatedAllocationResult(withdraw, inventoryPlugin.getRootFactory(), 
                                        0.9, true);
      pe.setEstimatedResult(estimatedResult);
      inventoryPlugin.publishChange(pe);
      thePG.updateWithdrawRequisition(withdraw);
    }
  }
      

}
