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

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.plan.AlpineAspectType;

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
  public class TaskDeficit {
    public int deficitStartBucket;
    public ArrayList allocated = new ArrayList();
    public ArrayList deficit = new ArrayList();

    public TaskDeficit(int bucket, double availAmt, double defAmt) {
      deficitStartBucket = bucket;
      allocated.add(new Double(availAmt));
      deficit.add(new Double(defAmt));
    }

    public void addBucket(double availAmt, double defAmt) {
      allocated.add(new Double(availAmt));
      deficit.add(new Double(defAmt));
    }

    public int getDeficitStartBucket() { return deficitStartBucket;}
    
    public double getAllocated(int index) { 
      if (index < allocated.size()) {
	return ((Double)allocated.get(index)).doubleValue();
      }
      return 0.0;
    }

    public double getDeficit(int index) {
      if (index < deficit.size()) {
	return ((Double)deficit.get(index)).doubleValue();
      }
      return 0.0;
    }

    public double getAccumulatedDeficit() {
      return getDeficit(deficit.size()-1);
    }
   
    public double getAccumulatedAllocation() {
      double sum = 0.0;
      for (int i=0; i < allocated.size(); i++) {
	sum += getAllocated(i);
      }
      return sum;
    }

    public int getAllocatedArraySize() { return allocated.size(); }
  }

  private transient HashMap trailingPointersHash = new HashMap();
  private transient ArrayList allocatedProjections = new ArrayList();
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
      trailingPointersHash.clear();
      allocatedProjections.clear();
      inventory = (Inventory)inv_list.next();
      thePG = (LogisticsInventoryPG)
        inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
      today_bucket = thePG.convertTimeToBucket(today);
      reconcileThePast(today_bucket, thePG);
      //TODO figure out what the end is
      // AF - we should get this from the BG (PG)
      int end_bucket = thePG.getLastDemandBucket();
      int lastWithdrawBucket = thePG.getLastWithdrawBucket();
      int firstProjectBucket = thePG.getFirstProjectWithdrawBucket();
      // bump this one more day - otherwise we get withdraws at the boundary
      if (today_bucket < lastWithdrawBucket) {
        createWithdrawAllocations(today_bucket, lastWithdrawBucket, inventory, thePG);
      }
      determineProjectionAllocations(firstProjectBucket, end_bucket, inventory, thePG);
      createBestProjectionAllocations(allocatedProjections, inventory, thePG);
      allocateLateDeliveries(trailingPointersHash, inventory);
      allocateEarlyProjections(firstProjectBucket, inventory, thePG);
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

      if (! trailingPointersHash.isEmpty()) {
        //process any withdraws from previous days that we haven't been able
        // to allocate - if we process some return their qty to take them into 
        // account for today's levels.
        runningQty = processLateWithdraws(currentBucket, 
                                          yesterdayLevel+todayRefill, inv, thePG);
      }
      
      Iterator wdIter = wdTasks.iterator();
      while(wdIter.hasNext()) {
        Task withdraw = (Task)wdIter.next();
	if (withdraw.getVerb().equals(Constants.Verb.WITHDRAW)) {
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
	    trailingPointersHash.put(withdraw, new TaskDeficit(currentBucket, 0, qty));
	    // if it already has a pe - should we rescind it?
	    // if (pe != null) publishRemove(pe);	  
	  }
	}
      }
      //when we are done going through all the tasks for the day set the level
      todayLevel = yesterdayLevel - runningQty + todayRefill;
      thePG.setLevel(currentBucket, todayLevel);
      currentBucket = currentBucket + 1;
    }
  }

  /** Create best allocations for Projections that are not being counted.
   *  These are likely early projections that are not counted because
   *  Supply tasks (actuals) are being counted in their place.
   *  Allocate these with yes or best.
   *  Note that projections that span the not counted and counted projection
   *  windows are not allocated here.
   *  @param countedBucket Process projections up to this bucket
   *  @param inv The Inventory we are processing
   *  @param the PG This is the PG for the Inventory we are processing
   **/
  private void allocateEarlyProjections(int countedBucket, Inventory inventory, 
                                        LogisticsInventoryPG thePG) {
    int currentBucket = 0;
    // loop through the buckets in the inventory
    while (currentBucket < countedBucket) {
      Collection wdprojs = thePG.getProjectedDemandTasks(currentBucket);
      Iterator wdpIter = wdprojs.iterator();
      while(wdpIter.hasNext()) {
        Task withdrawProj = (Task)wdpIter.next();
        double endTimePref = getTaskUtils().getPreference(withdrawProj, AspectType.END_TIME);
        //make sure there is an end time pref AND that the
        //bucket of the end time pref is not equal to or past the countedBucket
        // countedBucket is the firstCountedProjection - if the projection spans both
        //the uncounted and counted windows - don't blindly allocate it here ... it should
        // be picked up by the counted projections allocation method.
        if (endTimePref != Double.NaN) {
          if (thePG.convertTimeToBucket((long)endTimePref) < countedBucket) {
            if (withdrawProj.getPlanElement() == null) {
              createBestAllocation(withdrawProj, inventory, thePG);
            }
            // if it already has a pe we could check it - but for now we won't
          }
        }
      }
      //bump the bucket
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
    // need to go through and calculate scores for the late withdraws and 
    //allocate the high scores first.
    // should be based on scoring functions or TimeLate * Quantity.
    // for now just do first come, first serve
    Iterator tpIter = trailingPointersHash.keySet().iterator();
    while (tpIter.hasNext()) {
      Task task = (Task) tpIter.next();
      if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
	qty = getTaskUtils().getPreference(task, AspectType.QUANTITY);
      } else {
	long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
	long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
	qty = thePG.getProjectionTaskDemand(task, currentBucket, start, end);
	TaskDeficit deficit = (TaskDeficit)trailingPointersHash.get(task);
	qty += deficit.getAccumulatedDeficit();
      }
      checkQty = filled + qty;
      if ((level - checkQty) >= 0) {
        //The Alloc start time is the start time of the currentBucket
        // and the end time is the start time of currentBucket + 1
        // If the bucket is more than one day we only want to promise that
        // it will be delivered sometime during the bucket!
	if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
	  createLateAllocation(task, thePG.convertBucketToTime(currentBucket), 
                               thePG.convertBucketToTime(currentBucket+1), 
                               inv, thePG);
	  trailingPointersHash.remove(task);
	} else {
	  // Tasks remain in the trailingPointersHash (just projections for now)
	  // in order to create the complete allocation result (will handle split-
	  // deliveries for actuals later.)
	  TaskDeficit taskdeficit = (TaskDeficit)trailingPointersHash.get(task);
	  taskdeficit.addBucket(qty, 0);
	}
        filled = filled + qty;
      } else {
	// For now, only handle projections
	TaskDeficit taskdeficit = (TaskDeficit)trailingPointersHash.get(task);
	taskdeficit.addBucket(0, qty);	
      }
    }
    return filled;
  }

  /** Determine the Projection Allocations and create Allocations for
   *  late delivery Withdraw tasks.
   *  @param startBucket This is the starting bucket to process
   *  @param endBucket Whats the last valid bucket for the inventory
   *  @param inv The Inventory we are processing
   *  @param the PG This is the PG for the Inventory we are processing
   **/
  private void determineProjectionAllocations(int startBucket, int endBucket, 
					      Inventory inv, 
					      LogisticsInventoryPG thePG) {
    //calculate levels by adding in all refill projection results
    // go through each projectwithdraw - allocate to the inventory
    int currentBucket = startBucket;
    double qty = 0;
    double runningQty = 0;
    double todayLevel, yesterdayLevel, todayRefill, testLevel, testQty;
    long start, end;
    Task failedProjection = null;
    int endOfLevelSixBucket = thePG.getEndOfLevelSixBucket();
    endBucket = Math.min(endOfLevelSixBucket-1, endBucket);
    // loop through the bucket in the inventory
    while (currentBucket <= endBucket) {
      yesterdayLevel = thePG.getLevel(currentBucket - 1);
      Task refill = thePG.getRefillProjection(currentBucket);
      //!!!NOTE getRefillProjection can return a null for a bucket check to make sure
      // you have a real task and not a null.
      if (refill != null) {
	start = (long)PluginHelper.getPreferenceBestValue(refill, AspectType.START_TIME);
	end = (long)PluginHelper.getPreferenceBestValue(refill, AspectType.END_TIME);
	todayRefill = thePG.getProjectionTaskDemand(refill, currentBucket, start, end);
      } else {
	todayRefill = 0;
      }
      runningQty = 0;
      if (! trailingPointersHash.isEmpty()) {
  	runningQty = processLateWithdraws(currentBucket, yesterdayLevel+todayRefill,
 					  inv, thePG);
      }

      Collection wdTasks = thePG.getActualDemandTasks(currentBucket);
      Iterator wdIter = wdTasks.iterator();
      while (wdIter.hasNext()) {
	Task projWdraw = (Task)wdIter.next();
	if (projWdraw.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
	  start = (long)PluginHelper.getPreferenceBestValue(projWdraw, AspectType.START_TIME);
	  end = (long)PluginHelper.getPreferenceBestValue(projWdraw, AspectType.END_TIME);
	  qty = thePG.getProjectionTaskDemand(projWdraw, currentBucket, start, end);
	  testQty = runningQty + qty;
	  testLevel = yesterdayLevel - testQty + todayRefill;
	  
	  if (testLevel >= 0.0) {
	    // if its ok give it a pe and update the quantity  
	    runningQty = runningQty + qty;
	    if (projWdraw.getPlanElement() == null) {
	      TaskDeficit taskdeficit = (TaskDeficit)trailingPointersHash.get(projWdraw);
	      if (taskdeficit == null) {
		allocatedProjections.add(projWdraw);
//  		if(logger.isWarnEnabled()) {
//  		  logger.warn("AA.determineProjectionAllocations Adding to allocated projections "+
//  			      getTaskUtils().taskDesc(projWdraw));
//  		}
	      } else {
		taskdeficit.addBucket(qty, 0);
	      }
	    }
	  } else {
//  	    System.out.println("&&&&&&&&&&Failed projectWithdraw, todayRefill ="+
//  			       todayRefill+" "+			       
//    			       getTaskUtils().taskDesc(projWdraw));
//  	    int cb=1;
//  	    while (cb <= currentBucket+2) {
//  	      System.out.println("bucket: "+cb+" "+
//  				 TimeUtils.dateString(thePG.convertBucketToTime(cb))+
//  				 " level="+
//  				 thePG.getLevel(cb));
//  	      cb=cb+1;
//  	    }

	    failedProjection=projWdraw;
	    allocatedProjections.remove(projWdraw);
	    TaskDeficit taskdeficit = (TaskDeficit)trailingPointersHash.get(projWdraw);
	    if (taskdeficit == null) {
	      trailingPointersHash.put(projWdraw, new TaskDeficit(currentBucket, 0, qty));
	    } else {
	      taskdeficit.addBucket(0, qty);
	    }
	  }
	} else {
	  logger.debug("AA.determineProjectionAllocations " +
		       "Got SUPPLY task while processing ProjecWithdraws : " +
		       getTaskUtils().taskDesc(projWdraw));
	}
      }
      todayLevel = yesterdayLevel - runningQty + todayRefill;
      thePG.setLevel(currentBucket, todayLevel);
      currentBucket = currentBucket + 1;
    }
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
   *  @param start The start time of the window that it will be filled
   *  @param end The end time of the window that it will be filled
   *  @param inv  The Inventory we are allocating against
   *  @param thePG  The PG for the Inventory we are allocating against
   *  Note that we are using a start and end preference because the allocation
   *  is based on a bucket that may span more than one day. So we want to say it
   *  will be filled sometime within the bucket start and end time.
   **/
  private void createLateAllocation(Task withdraw, long start, long end,
                                    Inventory inv, LogisticsInventoryPG thePG) {
    int aspectTypes[] = {AspectType.END_TIME, AspectType.END_TIME, AspectType.QUANTITY};
    double results[] = new double[3];
    results[0] = (double) start;
    results[1] = (double) end;
    results[2] = getTaskUtils().getPreference(withdraw, AspectType.QUANTITY);
    AllocationResult estimatedResult = inventoryPlugin.getRootFactory().
      newAllocationResult(0.9, true, aspectTypes, results);
    Allocation lateAlloc = inventoryPlugin.getRootFactory().
      createAllocation(withdraw.getPlan(), withdraw, inv, 
                        estimatedResult, myRole);
    inventoryPlugin.publishAdd(lateAlloc);
    thePG.updateWithdrawRequisition(withdraw);
  }

  private void createBestProjectionAllocations(Collection list, Inventory inv, 
					       LogisticsInventoryPG thePG) {
    LogisticsAllocationResultHelper helper;
    Task task;
    AllocationResult ar;
    Allocation alloc;
    Iterator taskIter = list.iterator();
    while (taskIter.hasNext()) {
      task = (Task)taskIter.next();
      if (task.getPlanElement() == null) {
	long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
	long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
	int endOfLevelSixBucket = thePG.getEndOfLevelSixBucket();
	if (thePG.convertTimeToBucket(end) <= endOfLevelSixBucket) {
	  helper = new LogisticsAllocationResultHelper(task, null);
	  helper.setBest(AlpineAspectType.DEMANDRATE, start, end);
	  ar = helper.getAllocationResult(0.9);
	  alloc = inventoryPlugin.getRootFactory().
	    createAllocation(task.getPlan(), task, inv, ar, myRole);
	  inventoryPlugin.publishAdd(alloc);
	} else {
//  	  System.out.println(" Not generating AR for PW :"+getTaskUtils().taskDesc(task));
	}
      } else {
	// need to check the Allocation Result
      }
    }
    // Only need to update BG for late deliveries
  }

  private void allocateLateDeliveries(HashMap trailingPointersHash, Inventory inventory) {
    Iterator tpIter = trailingPointersHash.keySet().iterator();
    while (tpIter.hasNext()) {
      Task task = (Task) tpIter.next();
      if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
	createFailedAllocation(task, inventory);
      } else {
	TaskDeficit deficit = (TaskDeficit)trailingPointersHash.get(task);
	if (deficit.getAccumulatedAllocation() > 0.0) {
	  createLateProjectionAllocation(task, inventory);
	} else {
	  createFailedAllocation(task, inventory);
	}
	// update BG with late deliveries
	LogisticsInventoryPG thePG = (LogisticsInventoryPG)
	  inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
	thePG.updateWithdrawProjection(task);
      }
    }
  }

  private void createFailedAllocation(Task task, Inventory inventory) {
    LogisticsAllocationResultHelper helper = new LogisticsAllocationResultHelper(task, null);
    long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
    if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
      helper.setFailed(AspectType.QUANTITY, start, end);
    } else {
      helper.setFailed(AlpineAspectType.DEMANDRATE, start, end);
    }
    AllocationResult ar = helper.getAllocationResult(0.9);
    Allocation alloc = inventoryPlugin.getRootFactory().
      createAllocation(task.getPlan(), task, inventory, ar, myRole);
    inventoryPlugin.publishAdd(alloc);
  }

  private void createLateProjectionAllocation(Task task, Inventory inventory) {
    TaskDeficit deficit = (TaskDeficit)trailingPointersHash.get(task);
    LogisticsInventoryPG thePG = (LogisticsInventoryPG)
      inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
    LogisticsAllocationResultHelper helper = new LogisticsAllocationResultHelper(task, null);
    long msecperbucket = thePG.getBucketMillis();
    long start = thePG.convertBucketToTime(deficit.getDeficitStartBucket());
    int size = deficit.getAllocatedArraySize();
    for (int i=0; i < size; i++)  {
      helper.setPartial(AlpineAspectType.DEMANDRATE, start, start+msecperbucket, 
			deficit.getAllocated(i));
    }
    AllocationResult ar = helper.getAllocationResult(0.9);
    Allocation alloc = inventoryPlugin.getRootFactory().
      createAllocation(task.getPlan(), task, inventory, ar, myRole);
    inventoryPlugin.publishAdd(alloc); 
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
      double resultTime;
      // make sure END_TIME is specified - otherwise use START_TIME
      // UniversalAllocator plugin only gives start times
      if (ar.isDefined(AspectType.END_TIME)) {
        resultTime = ar.getValue(AspectType.END_TIME);
      } else {
        resultTime = ar.getValue(AspectType.START_TIME);
      }
      //arEnd = ar.getValue(AspectType.END_TIME);
      arQty = ar.getValue(AspectType.QUANTITY);
      taskEnd = getTaskUtils().getPreference(withdraw, AspectType.END_TIME);
      taskQty = getTaskUtils().getPreference(withdraw, AspectType.QUANTITY);
      if ( (resultTime == taskEnd) && (arQty == taskQty) ) {
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
