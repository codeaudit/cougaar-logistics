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

import org.cougaar.core.service.LoggingService;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Duration;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.Scalar;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;

import java.util.*;

/** The LogisticsInventoryBG is responsible for maintaining all
 *  information concerning an Inventory Asset.  The Logistics
 *  InventoryBG collects Withdraws, ProjectWithdraws, Supply and
 *  ProjectSupply tasks as well as calculated information such as
 *  inventory levels, refill levels and demand distribution over
 *  time.
 **/
public class LogisticsInventoryBG implements PGDelegate {


    // Bucket will be a knob, this implementation temporary
    private long MSEC_PER_BUCKET = TimeUtils.MSEC_PER_DAY * 1;
    protected LogisticsInventoryPG myPG;
    protected transient long startCDay;
    protected long arrivalTime;
    protected long startTime;
    protected int timeZero;
    private transient LoggingService logger;
    private transient LogisticsInventoryLogger csvLogger = null;
    private transient LogisticsInventoryFormatter csvWriter = null;
    // customerHash holds the time(long) of the last actual is seen
    // for each customer
    private HashMap customerHash;
    private TaskUtils taskUtils;

    protected int endOfLevelSixBucket;
    protected ArrayList refillProjections;
    protected ArrayList overlappingRefillProjections;
    protected ArrayList refillRequisitions;
    protected ArrayList dueOutList;
    // projectedDemandList mirrors dueOutList, each element
    // contains the sum of all Projected Demand for corresponding
    // bucket in dueOutList
    protected double projectedDemandArray[];
    protected double criticalLevelsArray[];
    protected double inventoryLevelsArray[];
    // Policy inputs
    protected long bucketSize = MSEC_PER_BUCKET;
    protected int criticalLevel = 3; // num buckets
    protected int reorderPeriod = 3; // num buckets
    protected int ost = 1;

    // Lists for csv logging & UI support
    protected ArrayList projWithdrawList;
    protected ArrayList withdrawList;
    protected ArrayList projSupplyList;
    protected ArrayList supplyList;
    protected ArrayList targetLevelsList;
    protected Schedule bufferedTargetLevels;
    protected ArrayList actualDemandTasksList;
    protected Schedule bufferedCriticalLevels;
    protected Schedule bufferedInventoryLevels;
    // booleans used for recalculations
    private boolean failures = false;
    private boolean compute_critical_levels = true;
    private boolean regenerate_projected_demand = false;
    private boolean recalculate_initial_level = true;


    public LogisticsInventoryBG(LogisticsInventoryPG pg) {
        myPG = pg;
        customerHash = new HashMap();
        dueOutList = new ArrayList();
        refillProjections = new ArrayList();
        refillRequisitions = new ArrayList();
	overlappingRefillProjections = new ArrayList();
        projectedDemandArray = new double[180];
        criticalLevelsArray = new double[180];
        inventoryLevelsArray = new double[180];
        Arrays.fill(criticalLevelsArray, Double.NEGATIVE_INFINITY);
        projWithdrawList = new ArrayList();
        withdrawList = new ArrayList();
        projSupplyList = new ArrayList();
        supplyList = new ArrayList();
        targetLevelsList = new ArrayList();
        actualDemandTasksList = new ArrayList();
        endOfLevelSixBucket = 1;
    }


    public void initialize(long startTime, int criticalLevel, int reorderPeriod, int ost, long bucketSize, long now, boolean logToCSV, InventoryPlugin parentPlugin) {
        this.startTime = startTime;
        // Set initial level of inventory from time zero to today.  This assumes that the inventory
        // is created because the existence of demand and the RefillGenerator will be run
        // on this inventory before time has advanced, thus the RefillGenerator will set
        // the inventory level for today and in general we always assume that levels prior
        // to today have been set before the RefillGenerator is run
        // Contract: the inventory for yesterday is always valid because initially it is
        // set by the behavior group of the inventory.
	// FSC - HOURLY:
	this.bucketSize = bucketSize;
	MSEC_PER_BUCKET = this.bucketSize;
        timeZero = (int) ((startTime / MSEC_PER_BUCKET) - 1);
        
        //Initialize with initial level since the refill generator won't start setting inv levels
        //until the first day it processes which is today + OST - so depending on OST it could be a while.
        inventoryLevelsArray[0] = myPG.getInitialLevel();

        logger = parentPlugin.getLoggingService(this);
        if (logToCSV) {
            csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(), myPG.getOrg(), false, parentPlugin);
            csvWriter = new LogisticsInventoryFormatter(csvLogger, new Date(startCDay), parentPlugin);
        }
        taskUtils = parentPlugin.getTaskUtils();
        if (logger.isDebugEnabled()) {
            logger.debug("Start day: " + TimeUtils.dateString(startTime) + ", time zero " +
                         TimeUtils.dateString(timeZero));
        }
        this.criticalLevel = criticalLevel;
        this.reorderPeriod = reorderPeriod;
        this.ost = ost;
        bufferedCriticalLevels =
                ScheduleUtils.buildSimpleQuantitySchedule(0, startTime, startTime + (TimeUtils.MSEC_PER_DAY * 10));
        bufferedTargetLevels =
                ScheduleUtils.buildSimpleQuantitySchedule(0, startTime, startTime + (TimeUtils.MSEC_PER_DAY * 10));
        bufferedInventoryLevels =
                ScheduleUtils.buildSimpleQuantitySchedule(myPG.getInitialLevel(),
                                                          startTime, startTime + (TimeUtils.MSEC_PER_DAY * 10));
    }

    //Call reinitialize which does the default that has to be done
    //on set up after rehydration.   It does the minimum initialization
    //that has to be done, even upon rehydration.
    //which does the initial set up when the inventory is created.
    public void reinitialize(boolean logToCSV, InventoryPlugin parentPlugin) {
        logger = parentPlugin.getLoggingService(this);
        if (logToCSV) {
            csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(), myPG.getOrg(), true, parentPlugin);
            csvWriter = new LogisticsInventoryFormatter(csvLogger, new Date(startCDay), parentPlugin);
        }
        taskUtils = parentPlugin.getTaskUtils();
    }

    public void addWithdrawProjection(Task task) {
        if (!task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) { // assertion/pre-condition
            Exception exception = new Exception("Adding non-PROJECTWITHDRAW task to inventory BG");
            logger.error(".addWithdrawProjection - adding non-PROJECTWITHDRAW task " + task +
                         " to inventory BG.", exception);
        }

        // Adding projections mean changed critical levels and
        // target levels.  Set boolean to recompute critical
        // levels and clear targetLevelsList for CSV logging
        compute_critical_levels = true;
        targetLevelsList.clear();
        long start = taskUtils.getStartTime(task);
        long end = taskUtils.getEndTime(task);
        // THIS ONE
        int bucket_start = convertTimeToBucket(start, false);
        int bucket_end = convertTimeToBucket(end, true);

        //If this new projection effects the initial level demand then recalc.
        if((bucket_start <= getInitialLevelDemandEndBucket()) &&
	   (bucket_end >= convertTimeToBucket(arrivalTime,false))) {
             recalculate_initial_level = true;
        }

        if (bucket_end >= projectedDemandArray.length) {
            projectedDemandArray = expandArray(projectedDemandArray, bucket_end);
        }
        while (bucket_end >= dueOutList.size()) {
            dueOutList.add(new ArrayList());
        }
//      System.out.println("Task start: "+TimeUtils.dateString(start)+" end  : "+TimeUtils.dateString(end));
//      System.out.println("bucket start "+bucket_start+", bucket end "+bucket_end);
        for (int i = bucket_start; i < bucket_end; i++) {
            ArrayList list = (ArrayList) dueOutList.get(i);
            list.add(task);
            updateProjectedDemandList(task, i, start, end, true);
        }
    }

    public void addWithdrawRequisition(Task task) {
        if (!task.getVerb().equals(Constants.Verb.WITHDRAW)) { // assertion/pre-condition
            Exception exception = new Exception("Adding non-WITHDRAW task to inventory BG");
            logger.error(".addWithdrawRequisition - adding non-WITHDRAW task " + task +
                         " to inventory BG.", exception);
        }

        long endTime = taskUtils.getEndTime(task);
        Object org = TaskUtils.getCustomer(task);
        if (org != null) {
            Long lastActualSeen = (Long) customerHash.get(org);
            if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
                customerHash.put(org, new Long(endTime));
            }
        }
        int bucket = convertTimeToBucket(endTime, true);
        while (bucket >= dueOutList.size()) {
            dueOutList.add(new ArrayList());
        }
        ArrayList list = (ArrayList) dueOutList.get(bucket);
        list.add(task);
    }

    private void regenerateProjectedDemandList() {
        // clear out old demand
        Arrays.fill(projectedDemandArray, 0.0);
        int size = dueOutList.size();
        Collection list;
        Iterator list_itr;
        Task task;
        for (int i = 0; i < size; i++) {
            list = getProjectedDemandTasks(i);
            list_itr = list.iterator();
            while (list_itr.hasNext()) {
                task = (Task) list_itr.next();
                long start = taskUtils.getStartTime(task);
                long end = taskUtils.getEndTime(task);
                updateProjectedDemandList(task, i, start, end, true);
            }
        }
    }

    // ******* HERE LIES A BUG
    // Boundary condition bug - daily projected demands do not
    // jive when comparing daily buckets to 3-day buckets
    // Updates the running demand sum per bucket
    // AHF 7/31/02 - The bug actually is in the adding of the projection for multiple day buckets
    //  see bug #1823
    private void updateProjectedDemandList(Task task, int bucket, long start, long end, boolean add) {
      if (bucket < 0) {
	logger.error("updateProjectedDemandList got bucket " + bucket + " for task " + task, new Throwable());
	bucket = 0;
      }

        double demand = getProjectionTaskDemand(task, bucket, start, end);
        if (add) {
            if (bucket >= projectedDemandArray.length) {
                projectedDemandArray = expandArray(projectedDemandArray, bucket);
            }
            projectedDemandArray[bucket] += demand;
        } else {
            projectedDemandArray[bucket] -= demand;
        }
//      logger.error("Bucket "+bucket+": new demand "+demand+", total is "+
//  		 projectedDemandArray[bucket]);
    }

    public double getProjectionTaskDemand(Task task, int bucket, long start, long end) {
        // If days_spanned is less than zero or days_spanned is greater than the bucket size
        // then the task has no demand for this bucket
        // FCS - HOURLY : getProjectionTaskDemand calls getDaysSpanned()
        long time_spanned = getTimeSpanned(bucket, start, end);
        if ((time_spanned > 0) && !(time_spanned > bucketSize)) {
            Rate rate = taskUtils.getRate(task);
	    Duration d = Duration.newMilliseconds((double)time_spanned);
	    Scalar scalar = (Scalar)rate.computeNumerator(d);
	    return taskUtils.getDouble(scalar);
	}
	return 0.0;
    }

    public void removeWithdrawRequisition(Task task) {
        for (int i = 0; i < dueOutList.size(); i++) {
            ArrayList list = (ArrayList) dueOutList.get(i);
            list.remove(task);
        }
    }

    /** Called by SupplyExpander to remove a Withdraw task that has either
     *  been rescinded or changed.
     *  @param task  The ProjectWithdraw task being removed
     **/
    public void removeWithdrawProjection(Task task) {
        compute_critical_levels = true;
        recalculate_initial_level = true;
        regenerate_projected_demand = true;
        ArrayList list;
        for (int i = 0; i < dueOutList.size(); i++) {
            list = (ArrayList) dueOutList.get(i);
            list.remove(task);
        }
    }

    public void addRefillRequisition(Task task) {
        long endTime = getEndTime(task);
        int bucket = convertTimeToBucket(endTime, true);
        while (bucket >= refillRequisitions.size()) {
            refillRequisitions.add(null);
        }
        refillRequisitions.set(bucket, task);
    }

    public int getLastWithdrawBucket() {
        Iterator list = customerHash.values().iterator();
        long lastSeen = convertBucketToTime(1);
        while (list.hasNext()) {
            long time = ((Long) list.next()).longValue();
            if (lastSeen < time) {
                lastSeen = time;
            }
        }
        return convertTimeToBucket(lastSeen, true);
    }

    private int getInitialLevelDemandEndBucket() {
      if(myPG.getSupplierArrivalTime() == -1) {
        return -1;
      }
      else {
        int supplierAvailableBucket = convertTimeToBucket(myPG.getSupplierArrivalTime(),false);

	//At the top of the supply chain 191-ORDBN has a
	//supplier available bucket that is 8/15 which is before its
	//arrival time.  The supplier is only truly available to you
	//once you arrive.
	int arrivalBucket = convertTimeToBucket(arrivalTime,false);
	if(supplierAvailableBucket < arrivalBucket) {
	    supplierAvailableBucket = arrivalBucket;
	}

	//The end of the horizon lookahead to compute the initial level	
	int initialLevelDemandEndBucket = supplierAvailableBucket + ost + criticalLevel;

        return initialLevelDemandEndBucket;
      }
    }


    private double getDemandDerivedInitialLevel() {
	//Not sure if this should be false or true.
	int startBucket = convertTimeToBucket(arrivalTime,false);
	//This includes the critical level and order ship time.
	int endBucket = getInitialLevelDemandEndBucket();
	if(endBucket == -1) {
	    return -1.0;
	}
	else {
	    double totalDemand=0.0;
	    for(int i=startBucket; i <= endBucket; i++) {
		totalDemand+=getProjectedDemand(i);
	    }
	    //Round up,no matter, to the next larger whole integer
	    //This is done mainly for class IX components, whose demand 
	    //is fractional - but we reorder actuals in whole numbers.
	    totalDemand = Math.ceil(totalDemand);
	    return totalDemand;
	}
    }
    
    public int getFirstProjectWithdrawBucket() {
        Iterator list = customerHash.values().iterator();
        // if we don't have any actual demand signal with -1
        if (!list.hasNext()) {
            return -1;
        }
        long firstSeen = convertBucketToTime(dueOutList.size() - 1);
        while (list.hasNext()) {
            long time = ((Long) list.next()).longValue();
            if (firstSeen > time) {
                firstSeen = time;
            }
        }
        // return the bucket after the first seen actual's end time bucket
        // consistent with getEffectiveProjectionStart
        return (convertTimeToBucket(firstSeen, true) + 1);
    }

    public int getLastRefillRequisition() {
        int lastRefill = -1;
        for (int i = 0; i < refillRequisitions.size(); i++) {
            Task task = (Task) refillRequisitions.get(i);
            if (task != null) {
                lastRefill = i;
            }
        }
        return lastRefill;
    }


    public int getLastDemandBucket() {
        return dueOutList.size() - 1;
    }

    public void setEndOfLevelSixBucket(int bucket) {
        endOfLevelSixBucket = bucket;
    }

    public void setArrivalTime(long anArrivalTime) {
	this.arrivalTime = anArrivalTime;
    }

    public long getArrivalTime() {
	return arrivalTime;
    }

    public void setStartCDay(long startCTime) {
	this.startCDay = startCTime;
    }

    public int getEndOfLevelSixBucket() {
        return endOfLevelSixBucket;
    }

    public void addRefillProjection(Task task) {
        long start = getStartTime(task);
        long end = getEndTime(task);
        // Test for Failed Dispositions Bug #2033
        if (start == 0L) {
            return;
        }
        int bucket_start = convertTimeToBucket(start, true);
        int bucket_end = convertTimeToBucket(end, true);

        while (bucket_end >= refillProjections.size()) {
            refillProjections.add(null);
        }
        for (int i = bucket_start; i < bucket_end; i++) {
            refillProjections.set(i, task);
        }
    }

    public void removeRefillProjection(Task task) {
      Task refillProj = null;
      for (int i=0; i < refillProjections.size(); i++) {
        refillProj = (Task)refillProjections.get(i);
        if ((refillProj != null) &&
            (refillProj.getUID().equals(task.getUID()))) {
          refillProjections.set(i, null);
        }
      }
//         int index;
//         while ((index = refillProjections.indexOf(task)) != -1) {
//             refillProjections.set(index, null);
//         }
    }

    public void removeRefillRequisition(Task task) {
        int index;
        if ((index = refillRequisitions.indexOf(task)) != -1) {
            refillRequisitions.set(index, null);
        }
    }

    public List clearRefillTasks(Date now) {
        // remove uncommitted refill tasks. Add all removed
        // tasks to a second list that will be returned
        // for comparison
        Task task;
        ArrayList taskList = new ArrayList();
        for (int i = 0; i < refillRequisitions.size(); i++) {
            task = (Task) refillRequisitions.get(i);
            if ((task != null) && task.beforeCommitment(now)) {
                refillRequisitions.set(i, null);
                taskList.add(task);
            }
        }
        return taskList;
    }

    // clear the projections 
    // and return the removed tasks for comparison
    // also keep a list of overlapping refill projection tasks
    public List clearRefillProjectionTasks(long now) {
      ArrayList removedTaskList = new ArrayList();
      overlappingRefillProjections.clear();
      Task t;
      for (int i = 0; i < refillProjections.size(); i++) {
	  t = (Task) refillProjections.get(i);
	  if ((t != null) && (!removedTaskList.contains(t))) {
	      long start = taskUtils.getStartTime(t);
	      if (start > now) { 
		  refillProjections.set(i, null);
		  removedTaskList.add(t);
	      } else if ((start < now) && (taskUtils.getEndTime(t) > now)) {
		  //this task spans now - shorten it	
                if (!overlappingRefillProjections.contains(t)) {
		  overlappingRefillProjections.add(t);
                }
	      }
	  }
      }
      return removedTaskList;
    }

    public List getOverlappingRefillProjections() {
	return (ArrayList) overlappingRefillProjections.clone();
    }

    public ArrayList getRefillRequisitions() {
        return (ArrayList) refillRequisitions.clone();
    }

    public Task getRefillProjection(int bucket) {
        // make sure the bucket doesn't cause an array out of bounds
        if (bucket < refillProjections.size()) {
            return (Task) refillProjections.get(bucket);
        } else {
            return null;
        }
    }

    public double getCriticalLevel(int bucket) {
        if (compute_critical_levels) {
            computeCriticalLevels();
            compute_critical_levels = false;
        }
        if ((bucket >= 0) && (bucket < criticalLevelsArray.length)) {
            return criticalLevelsArray[bucket];
        }
        return 0.0;
    }

    public void recalculateInitialLevel() {
      double newInitialLevel = getDemandDerivedInitialLevel();
      if(recalculate_initial_level) {
	      if(newInitialLevel != -1.0) {
	         inventoryLevelsArray[0] = newInitialLevel;
        }
      }
      recalculate_initial_level = false;
    }


    private double[] computeCriticalLevels() {
        if (regenerate_projected_demand) {
	    regenerateProjectedDemandList();
	    regenerate_projected_demand = false;
	}
	double Ci;
	criticalLevelsArray = new double[projectedDemandArray.length];
	// criticalLevel falls within a single bucket
	if (criticalLevel == 1) {
	    for (int i=0; i < projectedDemandArray.length; i++) {
	        Ci =  getProjectedDemand(i);
		criticalLevelsArray[i] = Ci;
	    }
	} else { // criticalLevel spans multiple buckets
	  int buckets = criticalLevel;
	  Ci = 0.0;
	  for (int i=1; i <= buckets; i++) {
	      Ci += getProjectedDemand(i);
	  }
	  criticalLevelsArray[0] = Ci;
	  for (int i=1; i < projectedDemandArray.length-buckets-1; i++) {
	      Ci =  Ci - getProjectedDemand(i) + getProjectedDemand(i+buckets);
	      if (Ci < 0.0) {
		  Ci = 0.0;
	      }
	      criticalLevelsArray[i] = Ci;
	  }
	  for (int i=projectedDemandArray.length-buckets-1; i < projectedDemandArray.length; i++) {
	      Ci =  Ci - getProjectedDemand(i);
	      if (Ci < 0.0) {
		  Ci = 0.0;
	      }
	      criticalLevelsArray[i] = Ci;
	  }
	}

	//No critical level before we arrive in theatre.
	//We're not supposed to order refills before we arrive in theatre.
	int arrivalTimeBucket = convertTimeToBucket(arrivalTime,true);
	for (int i=0; i<arrivalTimeBucket; i++) {
	    criticalLevelsArray[i] = 0.0;
	}

	return criticalLevelsArray;
    }

    public double getLevel(int bucket) {
      if (bucket < 0) {
	logger.error("getLevel asked for level at bucket " + bucket + " when inventoryLevelsArray has length " + inventoryLevelsArray.length, new Throwable());
	return inventoryLevelsArray[0];
      }
        if (bucket >= inventoryLevelsArray.length) {
            return inventoryLevelsArray[inventoryLevelsArray.length - 1];
        }
        return inventoryLevelsArray[bucket];
    }

    public void setLevel(int bucket, double value) {
        if (bucket >= inventoryLevelsArray.length) {
            inventoryLevelsArray = expandArray(inventoryLevelsArray, bucket);
        }
	//Potentially if you reset the first inventory level bucket, you
	//will want to reset it on the next refillGenerator call to recalculate
	if(bucket == 0) {
	    recalculate_initial_level=true;
	}
        inventoryLevelsArray[bucket] = value;
    }

    public void setTarget(int bucket, double value) {
      if (bucket < 0) {
	logger.error("setTarget called with bucket " + bucket + " and value " + value, new Throwable());
	bucket = 0;
      }
        // The intention of the List is to hold values for the buckets
        // that have target levels and hold nulls for those buckets that
        // do not have a target level
        int len = targetLevelsList.size();
        if (bucket >= len) {
            for (int i = len; i < bucket + 20; i++) {
                targetLevelsList.add(null);
            }
        }
        targetLevelsList.set(bucket, new Double(value));
    }


    public void clearTargetLevels(int startBucket) {
      if (startBucket < 0) {
	logger.error("clearTargetLevels called with startBucket " + startBucket, new Throwable());
	startBucket = 0;
      }
        // Clear target levels from the given bucket to end of array
        int len = targetLevelsList.size();
        for (int i = startBucket; i < len; i++) {
            targetLevelsList.set(i, null);
        }
    }

    public void updateRefillRequisition(Task task) {
        removeRefillRequisition(task);
        addRefillRequisition(task);
    }

    public void updateRefillProjection(Task task) {
        removeRefillProjection(task);
        addRefillProjection(task);
    }

    public void updateWithdrawRequisition(Task task) {
        removeWithdrawRequisition(task);
        addWithdrawRequisition(task);
    }

    public void updateWithdrawProjection(Task task) {
        removeWithdrawProjection(task);
        addWithdrawProjection(task);
    }

    public double getProjectedDemand(int bucket) {
        if (regenerate_projected_demand) {
            regenerateProjectedDemandList();
            regenerate_projected_demand = false;
        }
        if ((bucket >= projectedDemandArray.length) ||
                (bucket < 0)) {
            return 0.0;
        }

        return projectedDemandArray[bucket];
    }

    public Collection getProjectedDemandTasks(int bucket) {
        ArrayList projDemand = new ArrayList();
        if (bucket >= dueOutList.size()) {
            return projDemand;
        }
        Iterator dueOutIter = ((ArrayList) dueOutList.get(bucket)).iterator();
        while (dueOutIter.hasNext()) {
            Task task = (Task) dueOutIter.next();
            if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
                projDemand.add(task);
            }
        }
        return projDemand;
    }

    public double getActualDemand(int bucket) {
        if (bucket >= dueOutList.size()) {
	    return 0.0;
	}
	double actualDemand = 0.0;
	Rate rate;
	Scalar scalar;
	Iterator list = ((ArrayList)dueOutList.get(bucket)).iterator();
	while (list.hasNext()) {
	  Task task = (Task)list.next();
	  if (taskUtils.isProjection(task)) {
	      long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
	      long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
	      start = getEffectiveProjectionStart(task, start);
	      if (isValidProjectionTaskInBucket(start, end, bucket)) {
		  long time_spanned = getTimeSpanned(bucket, start, end);
		  rate = taskUtils.getRate(task);
		  try {
		      scalar = (Scalar)rate.computeNumerator(Duration.newMilliseconds((double)time_spanned));
		      actualDemand += taskUtils.getDouble(scalar);
		  } catch(Exception e) {
		      if (logger.isErrorEnabled()) {
			logger.error(taskUtils.taskDesc(task)+
				     " Start: "+TimeUtils.dateString(start)+
				     " time_spanned: "+time_spanned);
		      }
		  }
	      }
	  } else {
	      actualDemand += taskUtils.getQuantity(task);
	  }
	}
	return actualDemand;
    }

    public Collection getActualDemandTasks(int bucket) {
        ArrayList demand = new ArrayList();
        if (bucket >= dueOutList.size()) {
            return demand;
        }
        Iterator dueOutIter = ((ArrayList) dueOutList.get(bucket)).iterator();
        while (dueOutIter.hasNext()) {
            Task task = (Task) dueOutIter.next();
            if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
                long start = (long) PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
                long end = (long) PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
                start = getEffectiveProjectionStart(task, start);
                if (isValidProjectionTaskInBucket(start, end, bucket)) {
                    demand.add(task);
                }
            } else {
                demand.add(task);
            }
        }
        return demand;
    }

    private boolean isValidProjectionTaskInBucket(long start, long end, int bucket) {
        long end_of_bucket = (convertBucketToTime(bucket + 1) - 1);
        if ((start >= end) // task is entirely before swithover day
                || (end_of_bucket <= start))// task effectively starts outside current bucket
        {
            // ignore task
            return false;
        } else {
            return true;
        }
    }

    private long getTimeSpanned(int bucket, long start, long end) {
        long bucket_start = convertBucketToTime(bucket);
	long bucket_end = bucket_start + MSEC_PER_BUCKET;
	long interval_start = Math.max(start, bucket_start);
	long interval_end = Math.min(end, bucket_end);
	long value = interval_end - interval_start;
//     if (value < 0) {
//       logger.error("Bucket start: "+bucket_start+", Bucket end: "+bucket_end+
// 		   ", Task start: "+TimeUtils.dateString(start)+
// 		   ", Task end: "+TimeUtils.dateString(end)+
// 		   "interval start: "+TimeUtils.dateString(interval_start)+
// 		   "interval end: "+TimeUtils.dateString(interval_end));
//     }
        if (value > bucketSize) {
            if (logger.isErrorEnabled()) {
                logger.error("bucket " + bucket + ", Bucket start " + TimeUtils.dateString(bucket_start) +
                             ", end " + TimeUtils.dateString(bucket_end));
                logger.error("Task start " + TimeUtils.dateString(start) + ", end " + TimeUtils.dateString(end));
                logger.error("Interval start " + TimeUtils.dateString(interval_start) + ", end " +
                             TimeUtils.dateString(interval_end));
            }
            int b_end = convertTimeToBucket(end, true);
            if (logger.isErrorEnabled()) {
                logger.error("Calculated bucket end " + b_end + ", task end " + TimeUtils.dateString(end));
            }
        }
        return value;
    }

    public double getReorderPeriod() {
        return reorderPeriod;
    }

    /**
     Ignore projected demand that occurs before customer switchover day.
     **/
    public synchronized long getEffectiveProjectionStart(Task task, long start) {
        long firstProjectionTime;
        Object org = taskUtils.getCustomer(task);
        if (org != null) {
            Long lastActualSeen = (Long) customerHash.get(org);
            if (lastActualSeen != null) {
                firstProjectionTime = lastActualSeen.longValue() + MSEC_PER_BUCKET;
                firstProjectionTime = truncateToBucketStart(firstProjectionTime);
                if (firstProjectionTime > start) {
                    return firstProjectionTime;
                }
            }
        }
        return start;
    }


    private Collection getTasks(int bucket, String verb) {
        ArrayList task_list = new ArrayList();
        if (bucket < dueOutList.size()) {
            Iterator list = ((ArrayList) dueOutList.get(bucket)).iterator();
            Task task;
            while (list.hasNext()) {
                task = (Task) list.next();
                if (task.getVerb().equals(verb)) {
                    task_list.add(task);
                }
            }
        }
        return task_list;
    }

    public Collection getWithdrawTasks(int bucket) {
        return getTasks(bucket, Constants.Verb.WITHDRAW);
    }

    public Collection getProjectWithdrawTasks(int bucket) {
        return getTasks(bucket, Constants.Verb.PROJECTWITHDRAW);
    }

    public void rebuildCustomerHash() {
        customerHash.clear();
        ArrayList list;
        Iterator listIter;
        Task task;
        Object org;
        Long lastActualSeen;
        long endTime;
        long startTime;
        // first run through the projectwithdraws and make the lastActualSeen the
        // day before the start of the earliest projectwithdraw.  We won't really have
        // gotten an actual then, but its the only way to make sure that a customer who
        // ONLY sends projectwithdraws (instead of atleast one withdraw) actually
        // gets an entry put in the customer has for itself.
        for (int i = dueOutList.size() - 1; i >= 0; i--) {
          list = (ArrayList) dueOutList.get(i);
          if (!list.isEmpty()) {
            listIter = list.iterator();
            while (listIter.hasNext()) {
              task = (Task) listIter.next();
              if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
                org = TaskUtils.getCustomer(task);
                startTime = taskUtils.getStartTime(task);
                // now make end time the bucket before the start time of the first projectwithdraw
                endTime = startTime - (MSEC_PER_BUCKET * 2);
                lastActualSeen = (Long) customerHash.get(org);
                if ((lastActualSeen == null) || (endTime < lastActualSeen.longValue())) {
                  customerHash.put(org, new Long(endTime));
                }
              }
            }
          }
        }

        // now reset the customer has values for those customers that really did
        // send atleast one withdraw task
        for (int i = dueOutList.size() - 1; i >= 0; i--) {
            list = (ArrayList) dueOutList.get(i);
            if (!list.isEmpty()) {
                listIter = list.iterator();
                while (listIter.hasNext()) {
                    task = (Task) listIter.next();
                    if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
                        org = TaskUtils.getCustomer(task);
                        endTime = taskUtils.getEndTime(task);
                        lastActualSeen = (Long) customerHash.get(org);
                        if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
                            customerHash.put(org, new Long(endTime));
                        }
                    }
                }
            }
        }
    }

  public HashMap getCustomerHash() {
    return customerHash;
  }


  public void logAllToCSVFile(long aCycleStamp) {
    if (csvLogger != null) {
          csvWriter.logToExcelOutput(myPG,
                                     aCycleStamp);
    }
  }

    public long getStartTime() {
        return startTime;
    }

    public ArrayList getProjWithdrawList() {
        return projWithdrawList;
    }

    public ArrayList getWithdrawList() {
        return withdrawList;
    }

    public ArrayList getProjSupplyList() {
        return projSupplyList;
    }

    public ArrayList getSupplyList() {
        return supplyList;
    }

    public Schedule getBufferedCritLevels() {
        return bufferedCriticalLevels;
    }

    public Schedule getBufferedInvLevels() {
        return bufferedInventoryLevels;
    }

    public Schedule getBufferedTargetLevels() {
        return bufferedTargetLevels;
    }

    public ArrayList getActualDemandTasksList() {
        return actualDemandTasksList;
    }


    /**
     *  Take the incomming time and convert it to the beginning of the bucket
     *  in which it falls
     */
    public long truncateToBucketStart(long aTime) {
        int bucket = convertTimeToBucket(aTime, true);
        return convertBucketToTime(bucket);
    }

    /**
     * Convert a time (long) into a bucket of this inventory that can be
     * used to index duein/out vectors, levels, etc.
     **/
    public int convertTimeToBucket(long time, boolean partialBuckets) {
      if (time < 0) {
	logger.error("convertTimeToBucket: Got negative time " + TimeUtils.dateString(time), new Throwable());
	return 0;
      }
        int thisBucket = (int) (time / MSEC_PER_BUCKET);
 	if (partialBuckets) {
	  // FCS - HOURLY : Added this code from Bug #2413
	  if ((time % MSEC_PER_BUCKET) > 0.0) {
 	    thisBucket += 1;
	  }
	  // FCS - HOURLY : End added code
	}
	if (thisBucket < timeZero) {
	  logger.error("convertTimeToBucket: thisBucket(" + thisBucket + ") - timeZero(" + 
                       timeZero +") is < 0! (" + (thisBucket - timeZero) + ") when started with time " + 
                       TimeUtils.dateString(time)  + "!", new Throwable());
	  return 0;
	}
	return thisBucket - timeZero;
    }

    /**
     * Convert a bucket (int) into the start time of the bucket.
     * The end time of the bucket must be inferred from the
     * next sequential bucket's start time (non-inclusive).
     **/
    public long convertBucketToTime(int bucket) {
      if (bucket < 0) {
        logger.error("convertBucketToTime: Got negative bucket "+bucket, new Throwable());
        return 0;
      }
      return (bucket + timeZero) * MSEC_PER_BUCKET;
    }

    /** @return long The amount of ms the bucket spans **/
    public long getBucketMillis() {
        return MSEC_PER_BUCKET;
    }

    private long getStartTime(Task task) {
        PlanElement pe = task.getPlanElement();
        // If the task has no plan element then return the StartTime Pref
        if (pe == null) {
            return PluginHelper.getStartTime(task);
        } 
        return (long) PluginHelper.getStartTime(pe.getEstimatedResult());
    }

    private long getEndTime(Task task) {
        PlanElement pe = task.getPlanElement();
        // If the task has no plan element then return the EndTime Pref
        if (pe == null) {
            return PluginHelper.getEndTime(task);
        }
        AllocationResult ar = pe.getEstimatedResult();

        // make sure that we got atleast a valid reported OR estimated allocation result
        if (ar != null) {
            double resultTime;
            // make sure END_TIME is specified - otherwise use START_TIME
            // UniversalAllocator plugin only gives start times
            if (ar.isDefined(AspectType.END_TIME)) {
                resultTime = ar.getValue(AspectType.END_TIME);
            } else {
                resultTime = ar.getValue(AspectType.START_TIME);
            }
            return (long) resultTime;
        } else {
            // if for some reason we have a pe but no ar return the pref
            return PluginHelper.getEndTime(task);
        }
    }


  // Grow the array by 50% at a time, but at least enough to cover
  // the incoming request
    private double[] expandArray(double[] doubleArray, int newMinLength) {
      if (newMinLength < doubleArray.length)
	return doubleArray;

        double biggerArray[] = new double[Math.max(newMinLength, (int) (doubleArray.length * 1.5))];
        for (int i = 0; i < doubleArray.length; i++) {
            biggerArray[i] = doubleArray[i];
        }
        return biggerArray;
    }

    public int getCriticalLevel() {
        return criticalLevel;
    }


    public void takeSnapshot(Inventory inventory) {
        ArrayList tmpProjWdraw = new ArrayList();
	ArrayList tmpWdraw = new ArrayList();
	ArrayList tmpProjResupply = new ArrayList();

	Iterator due_outs;
	for (int i=0; i < dueOutList.size(); i++) {
	    due_outs = ((ArrayList)dueOutList.get(i)).iterator();
	    while (due_outs.hasNext()) {
	        Task task = (Task)due_outs.next();
		if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
		    tmpWdraw.add(task);
		} else { // PROJECTWITHDRAW
		    if (!tmpProjWdraw.contains(task)) {
		      tmpProjWdraw.add(task);
		    }
		}
	    }
	}
	projWithdrawList =tmpProjWdraw;
	withdrawList = tmpWdraw;

	Iterator projResupplyIt = refillProjections.iterator();
	while(projResupplyIt.hasNext()) {
	    Task task = (Task) projResupplyIt.next();
	    if(!tmpProjResupply.contains(task)) {
	        tmpProjResupply.add(task);
	    }
	}

	projSupplyList = tmpProjResupply;
	supplyList = (ArrayList)refillRequisitions.clone();

	// MWD took this out when converted list to a schedule.
	//    bufferedTargetLevels = (ArrayList)targetLevelsList.clone();

	ArrayList tmpActualDemandTasksList = new ArrayList();
	for (int i=0; i < dueOutList.size(); i++) {
	    tmpActualDemandTasksList.add(getActualDemandTasks(i));
	}
	actualDemandTasksList = tmpActualDemandTasksList;

	Vector list = new Vector();
	long start = convertBucketToTime(0);
	//the length of the critical level and inventory level 
	//arrays should match up
        int length = Math.max(getMeaningfulLength(criticalLevelsArray),
			      getMeaningfulLength(inventoryLevelsArray));

	int maxArrayLength = Math.min(criticalLevelsArray.length,
				   inventoryLevelsArray.length);

	if((length > maxArrayLength)  && (logger.isWarnEnabled())) {
	    logger.warn(getOrgName() + "-" + getItemName() + " Meaningful length is " + length + "but, criticalLevelsArray length is " + criticalLevelsArray.length + " and the inventoryLevelsArray length is " + inventoryLevelsArray.length + ". Will truncate to eliminate array index out of bounds error.");
	}

	//We want to make sure the meaningful length doesn't exceed
	//the length of either array.
	length = Math.min(length,maxArrayLength);

	for (int i=0; i < length; i++) {
	    if(criticalLevelsArray[i] >= 0.0) {
	        list.add(ScheduleUtils.buildQuantityScheduleElement(criticalLevelsArray[i],
								    start, start+MSEC_PER_BUCKET));
	    }
	    start += MSEC_PER_BUCKET;
	}
	bufferedCriticalLevels = GLMFactory.newQuantitySchedule(list.elements(),
								PlanScheduleType.OTHER);
	start = convertBucketToTime(0);
	list.clear();
	for (int i=0; i < targetLevelsList.size(); i++) {
	    Double targetLevel = (Double)targetLevelsList.get(i);
	    if(targetLevel != null) {
	        list.add(ScheduleUtils.buildQuantityScheduleElement(targetLevel.doubleValue(),start,start+MSEC_PER_BUCKET));
	    }
	    start += MSEC_PER_BUCKET;
	}
	bufferedTargetLevels = GLMFactory.newQuantitySchedule(list.elements(),
							      PlanScheduleType.OTHER);


	start = convertBucketToTime(0);
	list.clear();
        //length = 
	for (int i=0; i < length; i++) {
	    list.add(ScheduleUtils.buildQuantityScheduleElement(inventoryLevelsArray[i],
								start, start+MSEC_PER_BUCKET));
	    start += MSEC_PER_BUCKET;
	}
	bufferedInventoryLevels = GLMFactory.newQuantitySchedule(list.elements(),
							    PlanScheduleType.OTHER);
	NewScheduledContentPG scp = (NewScheduledContentPG)inventory.getScheduledContentPG();
	scp.setSchedule(bufferedInventoryLevels);

    }

    public boolean getFailuresFlag() {
        return failures;
    }

    // failures boolean used to determine when due outs
    // need to be re-bucketed
    public void setFailuresFlag(boolean value) {
        failures = value;
    }

    // Ask Beth about persistance.  Would like to make sure structures
    // are persisted when they are added to the code
    public PGDelegate copy(PropertyGroup pg) {
        return new LogisticsInventoryBG((LogisticsInventoryPG) pg);
    }

    private int printQuantityScheduleTimes(Schedule sched) {
        Enumeration elements = sched.getAllScheduleElements();
        QuantityScheduleElement qse;
        while (elements.hasMoreElements()) {
            qse = (QuantityScheduleElement) elements.nextElement();
            if (logger.isInfoEnabled()) {
                logger.info("qty: " + qse.getQuantity() +
                            " " + qse.getStartDate() + " to " + qse.getEndDate());
            }
        }
        return 0;
    }

    public void Test() {
        logger.error("Bucket size is "+MSEC_PER_BUCKET/TimeUtils.MSEC_PER_HOUR+" hrs.");
//      logger.error("ReorderPeriod is "+reorderPeriod+", getReorderPeriod() "+getReorderPeriod()+
//  		       ", critical level "+criticalLevel);
//      computeCriticalLevels();
//      setLevel(0, myPG.getInitialLevel());
//      for (int i=1; i < projectedDemandArray.length; i++) {
//        double new_level = getLevel(i-1) - projectedDemandArray[i];
//        setLevel(i, new_level);
//      }
//      logger.error("Date for Bucket Zero is "+TimeUtils.dateString(convertBucketToTime(0)));
//      for (int i=0; i < projectedDemandArray.length; i++) {
//        logger.error("Bucket "+i+", Demand "+getActualDemand(i)+", criticalLevel "+
//  			 criticalLevelsArray[i]+" Level "+inventoryLevelsArray[i]);
//      }
        if (logger.isErrorEnabled()) {
            logger.error("********* ProjectWithdrawList ********");
            for (int i = 0; i < projWithdrawList.size(); i++) {
                logger.error(taskUtils.taskDesc((Task) projWithdrawList.get(i)));
            }
            logger.error("********* WithdrawList ********");
            for (int i = 0; i < withdrawList.size(); i++) {
                logger.error(taskUtils.taskDesc((Task) withdrawList.get(i)));
            }
            logger.error("********* ProjectSupplyList ********");
            for (int i = 0; i < projSupplyList.size(); i++) {
                logger.error(taskUtils.taskDesc((Task) projSupplyList.get(i)));
            }
            logger.error("********* SupplyList ********");
            for (int i = 0; i < supplyList.size(); i++) {
                logger.error(taskUtils.taskDesc((Task) supplyList.get(i)));
            }
            logger.error("********* Buffered Critical Levels ********");
            printQuantityScheduleTimes(bufferedCriticalLevels);
            logger.error("********* Buffered Inventory Levels ********");
            printQuantityScheduleTimes(bufferedInventoryLevels);
        }
    }

  private int getMeaningfulLength(double[] list) {
    if (list.length < 5) {
      return list.length;
    }
    int lastMeaningfulPosition = list.length-1;
    for(int i = list.length-1; i > 0; i--) {
      if (list[i] == list[i-1]) {
        lastMeaningfulPosition = i;
      } else {
        break;
      }
    }
    return lastMeaningfulPosition+1;
  }

    public String getOrgName() { return myPG.getOrg().getItemIdentificationPG().getItemIdentification(); }

    public String getItemName() { return myPG.getResource().getTypeIdentificationPG().getTypeIdentification();}

}
