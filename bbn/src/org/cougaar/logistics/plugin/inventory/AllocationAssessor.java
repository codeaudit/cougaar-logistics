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


public class AllocationAssessor extends InventoryLevelGenerator {


  public AllocationAssessor(InventoryPlugin imPlugin) {
	super(imPlugin);
  }

  public void reconcileInventoryLevels(Collection inventories) {
    Iterator inv_list = inventories.iterator();
    long today = inventoryPlugin.getCurrentTimeMillis();
    int today_bucket;
    Inventory inventory;
    LogisticsInventoryPG thePG;
    while (inv_list.hasNext()) {
     inventory = (Inventory)inv_list.next();
     thePG = (LogisticsInventoryPG)
       inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
     today_bucket = thePG.convertTimeToBucket(today);
     reconcileThePast(today_bucket, thePG);
     //TODO figure out what the end is
     int end_bucket = 180;
     createWithdrawAllocations(today_bucket, end_bucket, thePG);
    }
  }

  private void reconcileThePast(int today_bucket, LogisticsInventoryPG thePG) {
    calculateInventoryLevels(0, today_bucket, thePG);
  }

  /** Create and update Withdraw Task Allocations for a particular Inventory
   *  @param todayBucket This is the starting bucket to process
   *  @param endBucket Whats the last valid bucket for the inventory
   *  @param the PG This is the PG for the Inventory we are processing
   **/
  private void createWithdrawAllocations(int todayBucket, int endBucket, 
                                        LogisticsInventoryPG thePG) {
    int currentBucket = todayBucket;
    double qty = 0;
    double runningQty = 0;
    double todayLevel, yesterdayLevel, todayRefill, testLevel, testQty;
    ArrayList trailingPointers = new ArrayList();

    // loop through the buckets in the inventory
    while (currentBucket <= endBucket) {
      yesterdayLevel = thePG.getLevel(currentBucket - 1);
      todayRefill = findCommittedRefill(currentBucket, thePG);
      runningQty = 0;

       //TODO...
      //should we really use getActualDemandTasks so we know when
      // withdraw tasks are being counted?
      // NOTE:  right now ProjectWithdraws are being ignored
      Collection wdTasks = thePG.getWithdrawTasks(currentBucket);

      if (! trailingPointers.isEmpty()) {
        //process any withdraws from previous days that we haven't been able
        // to allocate - if we process some return their qty to take them into 
        // account for today's levels.
        runningQty = processLateWithdraws(trailingPointers, currentBucket, 
                                          yesterdayLevel+todayRefill, thePG);
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
            createBestAllocation(withdraw);
          } else {
            checkPlanElement(withdraw);
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

  /** Utility method to create an Allocation that matches the
   *  best preferences for the withdraw task
   *  @param withdraw The withdraw task we are allocating
   **/
  private void createBestAllocation(Task withdraw) {
    AllocationResult estimatedResult = PluginHelper.
      createEstimatedAllocationResult(withdraw, inventoryPlugin.getRootFactory(), 
                                      0.9, true);
    //TODO need mySupplier and a Role...
   //  Allocation alloc = inventoryPlugin.getRootFactory().
//       createAllocation(withdraw.getPlan(), withdraw,
//                        mySupplier, estimatedResult,
//                        aRole);
//     inventoryPlugin.publishAdd(alloc);
  }

  private double processLateWithdraws(ArrayList lateWithdraws, int currentBucket,
                                      double level, LogisticsInventoryPG thePG) {
    double filled = 0;
    ArrayList processed = new ArrayList();
    // need to go through and calculate scores for the late withdraws and 
    //allocate the high scores first.
    return filled;
  }

  private void checkPlanElement(Task withdraw) {
    //if this task already has a pe - make sure the results are consistent with 
    //today.
  }
      

}
