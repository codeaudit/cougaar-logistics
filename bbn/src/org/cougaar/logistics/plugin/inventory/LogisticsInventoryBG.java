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

import org.cougaar.core.service.LoggingService;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.servlet.LogisticsInventoryServlet;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Duration;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.Scalar;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.glm.ldm.asset.SupplyClassPG;

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
    protected int deletionBucket = -1;


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
            if (logger.isErrorEnabled()) {
              logger.error(".addWithdrawProjection - adding non-PROJECTWITHDRAW task " + task +
                           " to inventory BG.", exception);
            }
        }

        long start = taskUtils.getStartTime(task);
        long end = taskUtils.getEndTime(task);
        // THIS ONE
        int bucket_start = convertTimeToBucket(start, false);
        int bucket_end = convertTimeToBucket(end, true);
        // Adding projections mean changed critical levels and
        // target levels.  Set boolean to recompute critical
        // levels and clear targetLevelsList for CSV logging
        compute_critical_levels = true;

        // StartBucket for the inventory is the first bucket which has not seen any deletions
        // It is possible to have a case where a projection is added to buckets in the past
        // because the allocation results on a projection can change until the end of the
        // task which may still be in the future.  Allow projections to be added to buckets
        // in the past but not to buckets before the start bucket of the inventory.
        if (bucket_start < getStartBucket()) {
          if (bucket_end < getStartBucket()) {
            if (logger.isErrorEnabled()) {
              logger.error("addWithdrawProjection not adding old projection. startBucket is "+
                          TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                          ", task "+taskUtils.taskDesc(task));
              
            }
            return;
          } else {
            if (logger.isInfoEnabled()) {
              logger.info("addWithdrawProjection not adding projection to deleted buckets. "+
                          "startBucket is "+TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                          ", task "+taskUtils.taskDesc(task));
            }
            bucket_start = getStartBucket();
          }
        }

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
        int bucket = convertTimeToBucket(endTime, false);
        if (bucket < getStartBucket()) {
          if (logger.isErrorEnabled()) {
            logger.error("addWithdrawRequisition not adding old requisition. startBucket is "+
                         TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                         ", task "+taskUtils.taskDesc(task));
          }
          return;
        }
        Object org = TaskUtils.getCustomer(task);
        if (org != null) {
            Long lastActualSeen = (Long) customerHash.get(org);
            if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
                customerHash.put(org, new Long(endTime));
            }
        }
        while (bucket >= dueOutList.size()) {
            dueOutList.add(new ArrayList());
        }
        ArrayList list = (ArrayList) dueOutList.get(bucket);
        list.add(task);
    }

    private void regenerateProjectedDemandList() {
        // clear out old demand
//         Arrays.fill(projectedDemandArray, 0.0);
        Arrays.fill(projectedDemandArray,getStartBucket(),projectedDemandArray.length-1, 0.0);
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
      if (bucket < getStartBucket()) {
        if (logger.isInfoEnabled()) {
          logger.info("updateProjectedDemandList not adding demand for old projection. startBucket is "+
                      TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                      ", task "+taskUtils.taskDesc(task));
        }
        return;
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
      if (task.isDeleted()) {
        int end_bucket = convertTimeToBucket(getEndTime(task), false);
        if (logger.isDebugEnabled()) {
          logger.debug("Removing task "+taskUtils.taskDesc(task));
        }
        setDeletionBucket(end_bucket);
      }
      for (int i = 0; i < dueOutList.size(); i++) {
        ArrayList list = (ArrayList) dueOutList.get(i);
        if (list != null) {
          list.remove(task);
        }
      }
    }

    /** Called by SupplyExpander to remove a Withdraw task that has either
     *  been rescinded or changed.
     *  @param task  The ProjectWithdraw task being removed
     **/
  // TODO possible gotcha - only removing the deleted task from reported end time back.
  //      could it be hiding somewhere else in the list - TEST
    public void removeWithdrawProjection(Task task) {
      if (task.isDeleted()) {
        int end_bucket = convertTimeToBucket(getEndTime(task), true);
        if (logger.isDebugEnabled()) {
          logger.debug("Removing task "+taskUtils.taskDesc(task));
        }
        setDeletionBucket(end_bucket-1);
      }else {
        compute_critical_levels = true;
        recalculate_initial_level = true;
        regenerate_projected_demand = true;
      }
      ArrayList list;
      for (int i = 0; i < dueOutList.size(); i++) {
        list = (ArrayList) dueOutList.get(i);
        if (list != null) {
          list.remove(task);
        }
      }
    }

  /** Called by all methods which remove tasks from lists.
   *  When a task is deleted, the last affected bucket back to the beginning of
   *  list are emptied IFF the buckets have not already been emptied.
   *  @param bucket Last effected bucket by task deletion
   **/ 
  protected void setDeletionBucket(int bucket) {
    if (bucket > deletionBucket) {
      deletionBucket = bucket;
    }
  }

  public void addRefillRequisition(Task task) {
    boolean taskAdded = false;

    PlanElement pe = task.getPlanElement();
    if (pe != null) {
      AllocationResult ar = pe.getReportedResult();
      if (ar == null) {
        ar = pe.getEstimatedResult();
      }
      if (ar != null) {
        if (!ar.isPhased()) {
          long endTime = getEndTime(task);
          addTaskToBucket(endTime, task);
          taskAdded = true;
        }
        else {
          int[] ats = ar.getAspectTypes();
          int endInd =  csvWriter.getIndexForType(ats, AspectType.END_TIME);
          Enumeration phasedResults = ar.getPhasedResults();
          while (phasedResults.hasMoreElements()) {
            double[] results = (double[]) phasedResults.nextElement();
            long phasedEndTime;
            phasedEndTime = (long) results[endInd];
            addTaskToBucket(phasedEndTime, task);
            taskAdded = true;
          }
        }
      }
    }
    if (!taskAdded) {
      long endTime = getEndTime(task);
      addTaskToBucket(endTime, task);
    }
  }

  private void addTaskToBucket(long endTime, Task task) {
    // probably can factor this out into a separate method.
    int bucket = convertTimeToBucket(endTime, false);
    if (bucket < getStartBucket()) {
      if (logger.isErrorEnabled()) {
        logger.error("addRefillRequisition not adding old requisition. startBucket is "+
                     TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                     ", task "+taskUtils.taskDesc(task));
      }
      return;
    }
    while (bucket >= refillRequisitions.size()) {
      refillRequisitions.add(null);
    }

    //MWD Debugging
    ArrayList refills = (ArrayList) refillRequisitions.get(bucket);
    if(refills == null) {
      refills = new ArrayList();
      refillRequisitions.set(bucket, refills);
    }
    /**
     * Debug
     else {
     logger.warn("Uh-Oh. At " + getOrgName() + "-item:" + getItemName() +
     " on " + TimeUtils.dateString(endTime)+" we would be overwritting task "
     + ((Task) refills.get(0)).getUID() + " with new task " + task.getUID()); 
     }
    */
    refills.add(task);
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
        return convertTimeToBucket(lastSeen, false);
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

	//Test case where first bucket of demand is beyone the supplierAvailableBucket then we should make sure we cover the 1st days of demand with the ost amount of demand.
	int firstDemandBucket = getFirstProjectWithdrawBucket();
	if(firstDemandBucket > supplierAvailableBucket) {
	    supplierAvailableBucket = firstDemandBucket;
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
        return (convertTimeToBucket(firstSeen, false) + 1);
    }

    public int getLastRefillRequisition() {
        int lastRefill = -1;
        for (int i = 0; i < refillRequisitions.size(); i++) {
            ArrayList refills = (ArrayList) refillRequisitions.get(i);
            if ((refills != null) &&
		(refills.size() > 0)) {
		//The last refill is actually the target date for it's arrival, 
		//because thats what the refill generator is planning on.
		for(int j=0; j < refills.size() ; j++) {
		    Task task = (Task) refills.get(j);
		    //taskUtils.getEndTime() returns the preference (the best time)
		    int endBucket = convertTimeToBucket(taskUtils.getEndTime(task),false);
		    lastRefill = Math.max(endBucket,lastRefill);
		}
		//Put above for loop and code in to replace line below this 
		//because we were counting all the projections from an early arrival 
		//to the request date of a last refill.
		//lastRefill = i;
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
      int bucket_start = convertTimeToBucket(start, false);
      int bucket_end = convertTimeToBucket(end, true);
      // StartBucket for the inventory is the first bucket which has not seen any deletions
      // It is possible to have a case where a projection is added to buckets in the past
      // because the allocation results on a projection can change until the end of the
      // task which may still be in the future.  Allow projections to be added to buckets
      // in the past but not to buckets before the start bucket of the inventory.
      if (bucket_start < getStartBucket()) {
        if (bucket_end < getStartBucket()) {
          if (logger.isErrorEnabled()) {
            logger.error("addRefillProjection not adding old projection. startBucket is "+
                         TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                         ", task "+taskUtils.taskDesc(task));
            
          }
          return;
        } else {
          if (logger.isInfoEnabled()) {
            logger.info("addRefillProjection not adding projection to deleted buckets. "+
                        "startBucket is "+TimeUtils.dateString(convertBucketToTime(getStartBucket()))+
                        ", task "+taskUtils.taskDesc(task));
          }
          bucket_start = getStartBucket();
        }
      }
      
      while (bucket_end >= refillProjections.size()) {
        refillProjections.add(null);
      }
      for (int i = bucket_start; i < bucket_end; i++) {
        ArrayList refills = (ArrayList) refillProjections.get(i);
        if(refills == null) {
          refills = new ArrayList();
          refillProjections.set(i, refills);
        }
        if (!refills.contains(task)) {
          refills.add(task);
        }
      }
    }

    public void removeRefillProjection(Task task) {
      if (task.isDeleted()) {
        int end_bucket = convertTimeToBucket(getEndTime(task), true);
        if (logger.isDebugEnabled()) {
          logger.debug("Removing task "+taskUtils.taskDesc(task));
        }
        setDeletionBucket(end_bucket-1);
      }
      ArrayList refills;
      Task refillProj = null;
      for (int i=0; i < refillProjections.size(); i++) {
        refills = (ArrayList)refillProjections.get(i);
        if(refills != null) {
          //	    for(j=0; j < refills.size() ;j++) {
          //		refillProj = (Task) refills.get(j);
          //	if ((refillProj != null) &&
          //	    (refillProj.getUID().equals(task.getUID()))) {
          //	    refills.remove(j);
          //}
          //}
          if(refills.remove(task)) {
            if(refills.size() < 1) {
              refillProjections.set(i,null);
            }
          }
	}
 //         int index;
//         while ((index = refillProjections.indexOf(task)) != -1) {
//             refillProjections.set(index, null);
//         }
      }
    }

    public void removeRefillRequisition(Task task) {
      if (task.isDeleted()) {
        int end_bucket = convertTimeToBucket(getEndTime(task), false);
        if (logger.isDebugEnabled()) {
          logger.debug("Removing task "+taskUtils.taskDesc(task));
        }
        setDeletionBucket(end_bucket);
      }
      for (int i = 0; i < refillRequisitions.size(); i++) {
        ArrayList refills = (ArrayList) refillRequisitions.get(i);
        if(refills != null) {
          if(refills.remove(task)){
            if(refills.size() < 1) {
              refillRequisitions.set(i, null);
            }
          }
        }
      }
    }

    public List clearRefillTasks(Date now) {
        // remove uncommitted refill tasks. Add all removed
        // tasks to a second list that will be returned
        // for comparison
        Task task;
	ArrayList refills;
        ArrayList taskList = new ArrayList();
        for (int i = 0; i < refillRequisitions.size(); i++) {
            refills = (ArrayList) refillRequisitions.get(i);
	    if(refills != null) {
		ArrayList refillsToRemove = new ArrayList(refills.size());
		for(int j=0; j < refills.size() ; j++) {
		    task = (Task) refills.get(j);
		    if ((task != null) && task.beforeCommitment(now)) {
			if(!taskList.contains(task)) {
			    taskList.add(task);
			}
			refillsToRemove.add(task);
		    }
		}
		refills.removeAll(refillsToRemove);
		if(refills.size() < 1) {
		    refillRequisitions.set(i, null);
		}
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
      ArrayList refills;
      for (int i = 0; i < refillProjections.size(); i++) {
	  refills = (ArrayList) refillProjections.get(i);
	  if(refills != null) {
	      ArrayList refillsToRemove = new ArrayList(refills.size());
	      for(int j=0; j < refills.size(); j++) {
		  t = (Task) refills.get(j);
		  if ((t != null) && (!removedTaskList.contains(t))) {
		      long start = taskUtils.getStartTime(t);
		      if (start >= now) { 
			  refillsToRemove.add(t);
			  if(!removedTaskList.contains(t)) {
			      removedTaskList.add(t);
			  }
		      } else if ((start < now) && (taskUtils.getEndTime(t) > now)) {
			  //this task spans now - shorten it	
			  if (!overlappingRefillProjections.contains(t)) {
			      overlappingRefillProjections.add(t);
			  }
		      }
		  }
	      }
	      refills.removeAll(refillsToRemove);
	      if(refills.size() == 0) {
		  refillProjections.set(i, null);
	      }	      
	  }
      }
      return removedTaskList;
    }

    public List getOverlappingRefillProjections() {
	return (ArrayList) overlappingRefillProjections.clone();
    }

    private ArrayList getFlattenedList(ArrayList listOLists) {
	ArrayList uniqueTasks = new ArrayList();
        for (int i = 0; i < listOLists.size(); i++) {
            ArrayList list = (ArrayList) listOLists.get(i);
	    if(list != null) {
		for(int j=0; j<list.size(); j++) {
		    Task task = (Task) list.get(j);
		    if(!uniqueTasks.contains(task)) {
			uniqueTasks.add(task);
		    }
		}
	    }
	}
        return uniqueTasks;
    }

    private ArrayList deepClone(ArrayList listOLists) {
	
	ArrayList clone = new ArrayList(listOLists.size());
        for (int i = 0; i < listOLists.size(); i++) {
            ArrayList list = (ArrayList) listOLists.get(i);
	    if(list != null) {
		list = (ArrayList) list.clone();
	    }
	    clone.add(i,list);
	}
	return clone;
    }

    public ArrayList getRefillRequisitions() {	
	return ((ArrayList) refillRequisitions.clone());
    }


    public ArrayList getRefillProjection(int bucket) {
        // make sure the bucket doesn't cause an array out of bounds
        if (bucket < refillProjections.size()) {
            return (ArrayList) refillProjections.get(bucket);
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
      if((recalculate_initial_level) && (getStartBucket() == 0)) {
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
// 	    for (int i=getStartBucket(); i < projectedDemandArray.length; i++) {
	    for (int i=0; i < projectedDemandArray.length; i++) {
	        Ci =  getProjectedDemand(i);
		criticalLevelsArray[i] = Ci;
	    }
	} else { // criticalLevel spans multiple buckets
//           int start = (getStartBucket() > 1) ? getStartBucket() : 1;
          int start = 1;
	  int buckets = criticalLevel;
	  Ci = 0.0;
	  for (int i=start; i <= buckets; i++) {
	      Ci += getProjectedDemand(i);
	  }
	  criticalLevelsArray[0] = Ci;
	  for (int i=start; i < projectedDemandArray.length-buckets-1; i++) {
	      Ci =  Ci - getProjectedDemand(i) + getProjectedDemand(i+buckets);
	      if (Ci < 0.0) {
		  Ci = 0.0;
	      }
	      criticalLevelsArray[i] = Ci;
	  }
          // TODO may want to test start bucket here too but I'm on the fence
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
	int arrivalTimeBucket = convertTimeToBucket(arrivalTime,false);
	for (int i=getStartBucket(); i<arrivalTimeBucket; i++) {
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
      if (bucket < getStartBucket()) {
        if (bucket < 0) {
          if (logger.isErrorEnabled()) {
            logger.error("setLevel called with bucket "+bucket+" when startBucket is "+
                         getStartBucket(), new Throwable());
          }
        }
        if (logger.isDebugEnabled()) {
          logger.debug("setLevel called with bucket "+bucket+" when startBucket is "+getStartBucket());
        }
        return;
      }
        if (bucket >= inventoryLevelsArray.length) {
            inventoryLevelsArray = expandArray(inventoryLevelsArray, bucket);
        }
	//Potentially if you reset the first inventory level bucket, you
	//will want to reset it on the next refillGenerator call to recalculate
	if(bucket == 0) {
	    recalculate_initial_level=true;
	    //	    if(value <= 0) {
	    //logger.warn("At " + getOrgName() + "AAARRGH. Setting the initial level of item " + getItemName() + "-" + getSupplyType() + " to " + value);
	    // }
	}
        inventoryLevelsArray[bucket] = value;
    }

    public void setTarget(int bucket, double value) {
      if (bucket < getStartBucket()) {
        if (bucket < 0) {
          if (logger.isErrorEnabled()) {
            logger.error("setTarget called with bucket " + bucket + " and value " + value, new Throwable());
          }
        }
        if (logger.isDebugEnabled()) {
          logger.debug("setTarget called with bucket "+bucket+" when startBucket is "+getStartBucket());
        }
        return;
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
        if (logger.isErrorEnabled()) {
          logger.error("clearTargetLevels called with startBucket " + startBucket, new Throwable());
        }
	startBucket = 0;
      }
      if (logger.isDebugEnabled()) {
        logger.debug(getOrgName()+" clearTargetLevels called with bucket "+
                     TimeUtils.dateString(convertBucketToTime(startBucket))+" when start bucket is "+
                     TimeUtils.dateString(convertBucketToTime(getStartBucket())));
      }
      // Below will be replaced by the following once debugging is complete
      // startBucket = (startBucket < getStartBucket()) ? getStartBucket() : startBucket;
      if (startBucket < getStartBucket()) {
        startBucket = getStartBucket();
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

  public int getStartBucket() {
    return deletionBucket+1;
  }

    public ArrayList getProjWithdrawList() {
        return projWithdrawList;
    }

    public ArrayList getWithdrawList() {
        return withdrawList;
    }

    public ArrayList getProjSupplyList() {
        return getFlattenedList(projSupplyList);
    }

    public ArrayList getSupplyList() {
        return getFlattenedList(supplyList);
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

    public ShortfallInventory checkForShortfall(String invID,
						String unitOfIssue) {

      ShortfallInventory shortfallInv=new ShortfallInventory(invID,unitOfIssue);

      int numDemandProj = countProjFailures(getProjWithdrawList(),true,true);
      int numPermDemandProj = countProjFailures(getProjWithdrawList(),true,false);
      shortfallInv.setNumDemandProj(numDemandProj);
      shortfallInv.setNumTempDemandProj(numDemandProj - numPermDemandProj);

      int numResupplyProj = countProjFailures(getProjSupplyList(),false,true);
      int numPermResupplyProj = countProjFailures(getProjSupplyList(),false,false);
      shortfallInv.setNumResupplyProj(numResupplyProj);
      shortfallInv.setNumTempResupplyProj(numResupplyProj - numPermResupplyProj);

      int numDemandSupply = countActualShortfall(getWithdrawList(), true);
      int numPermDemandSupply = countActualShortfall(getWithdrawList(),false);
      shortfallInv.setNumDemandSupply(numDemandSupply);
      shortfallInv.setNumTempDemandSupply(numDemandSupply - numPermDemandSupply);
      int numResupplySupply = countActualShortfall(getSupplyList(),true);
      int numPermResupplySupply = countActualShortfall(getSupplyList(),false);
      shortfallInv.setNumResupplySupply(numResupplySupply);
      shortfallInv.setNumTempResupplySupply(numResupplySupply - numPermResupplySupply);

      if(shortfallInv.getNumTotalShortfall() > 0) {
	  addShortfallPeriods(shortfallInv);
	  return shortfallInv;
      }
      else {
	  return null;
      }
    }

    
    protected void addShortfallPeriods(ShortfallInventory shortfallInv) {
      int startBucket = getStartBucket();
      int endBucket = getLastDemandBucket();
	
      long startOfPeriod = -1;
      long endOfPeriod = -1;
      boolean inShortfallPeriod =false;
      
      for(int i=startBucket; i <= endBucket; i++) {
	double level = getLevel(i);
	if(level ==  0.0) {
	  if(!inShortfallPeriod) {
	    startOfPeriod = convertBucketToTime(i);
	    inShortfallPeriod = true;
	  }
	}
	else {
	  if(inShortfallPeriod) {
            //The bucket before the current one was the last shortfall period bucket.  Therefore i-1
	    endOfPeriod = convertBucketToTime(i) - 1;
	    ShortfallPeriod newPeriod = new ShortfallPeriod(startOfPeriod,
							    endOfPeriod);
	    calculateShortfallPeriod(newPeriod,getProjWithdrawList());
	    calculateShortfallPeriod(newPeriod,getWithdrawList());
	    shortfallInv.addShortfallPeriod(newPeriod);  
	    inShortfallPeriod=false;
	  }
	}
      }

      if(inShortfallPeriod) {
	  //The bucket before the current one was the last shortfall period bucket.  Therefore i-1
	  endOfPeriod = convertBucketToTime(endBucket) - 1;
	  ShortfallPeriod newPeriod = new ShortfallPeriod(startOfPeriod,
							    endOfPeriod);
	  calculateShortfallPeriod(newPeriod,getProjWithdrawList());
	  calculateShortfallPeriod(newPeriod,getWithdrawList());
	  shortfallInv.addShortfallPeriod(newPeriod);  
      }
    }

    protected void calculateShortfallPeriod(ShortfallPeriod shortPeriod,ArrayList tasks) {
      double totalDemand = 0.0d;
      double totalFilled = 0.0d;
      Iterator taskIt = tasks.iterator();
      while(taskIt.hasNext()) {
	Task t = (Task) taskIt.next();
	PlanElement pe = t.getPlanElement();
	AllocationResult ar=null;
	if (pe != null) {
	  ar = pe.getReportedResult();
	  if (ar == null) {
	    ar = pe.getEstimatedResult();
	  }
	}
	double taskQty = 0.0d;
	if(taskUtils.isProjection(t)) {
	  long start = Math.max(shortPeriod.getStartTime(),getEffectiveProjectionStart(t,getStartTime(t)));
	  long end = Math.min(shortPeriod.getEndTime(),getEndTime(t));
	  taskQty = taskUtils.getTotalQuantity(t,start,end);
	  totalDemand += taskQty;
	  if(ar == null) {
	    totalFilled += taskQty;
	  } else if (ar.isSuccess()) {
	    if(!ar.isPhased()) {
	      totalFilled += taskQty;
	    }
	    else {
	      int[] ats = ar.getAspectTypes();
	      int rateInd = -1;
	      int startInd = -1;
	      int endInd = -1;
	      double totalQty=0;
	      rateInd = LogisticsInventoryFormatter.getIndexForType(ats, AlpineAspectType.DEMANDRATE);
	      startInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.START_TIME);
	      endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);
	      
	      Enumeration phasedResults = ar.getPhasedResults();
	      while (phasedResults.hasMoreElements()) {
		double[] results = (double[]) phasedResults.nextElement();
		double phaseRate = results[rateInd];
		double phaseStart = results[startInd];
		double phaseEnd = results[endInd];
		start = Math.max(shortPeriod.getStartTime(),getEffectiveProjectionStart(t,(long) phaseStart));
		end = Math.min(shortPeriod.getEndTime(),(long) phaseEnd);
		totalQty += taskUtils.getTotalQuantity(t,phaseRate,start,end);
	      }	
	      totalFilled += totalQty;
	    }
	  }
	}
	else {
	  long taskEnd = taskUtils.getEndTime(t);
	  if(!((taskEnd >= shortPeriod.getStartTime()) &&
	       (taskEnd <= shortPeriod.getEndTime()))) {
	    continue;
	  }

	  taskQty = taskUtils.getQuantity(t);
	  totalDemand += taskQty;
	  if(ar == null) {
	    totalFilled += taskQty;
	  }
	  else if(!ar.isPhased()) {
	    if(ar.isSuccess()) {
	      double arEnd = taskUtils.getEndTime(ar);
	      if((arEnd >= shortPeriod.getStartTime()) &&
		 (arEnd <= shortPeriod.getEndTime())) {
		totalFilled += taskQty;	      
	      }
	    }
	  }
	  else {
	    int[] ats = ar.getAspectTypes();
	    int qtyInd = -1;
	    int endInd = -1;
	    double totalQty=0;
	    qtyInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.QUANTITY);
	    endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);
	    Enumeration phasedResults = ar.getPhasedResults();
	    while (phasedResults.hasMoreElements()) {
	      double[] results = (double[]) phasedResults.nextElement();
	      double phaseQty = results[qtyInd];
	      double phaseEnd = results[endInd];
	      
	      if(phaseEnd <= shortPeriod.getEndTime()) {
		  totalFilled += phaseQty;
	      }
	    }
	  }
	}
      }
	
      shortPeriod.setTotalDemand(shortPeriod.getTotalDemand() + totalDemand);
      shortPeriod.setTotalFilled(shortPeriod.getTotalFilled() + totalFilled);
    }


    protected int countProjFailures(ArrayList taskList, boolean isDemand, boolean includeTemps) {
	Iterator it = taskList.iterator();
	int ctr=0;
	while(it.hasNext()) {
	  Task t = (Task) it.next();
	  PlanElement pe = t.getPlanElement();
	  if (pe != null) {
	    AllocationResult ar = pe.getReportedResult();
	    if (ar == null) {
	      ar = pe.getEstimatedResult();
	    }
	    long start = 0;
	    int lastReqBucket = getLastRefillRequisition();
	    long projRefillStart = convertBucketToTime(lastReqBucket + 1);
	    if(isDemand) {
		start = getEffectiveProjectionStart(t,getStartTime(t));
	    }
	    else {
		start = Math.max(getStartTime(t), projRefillStart);
	    }
	    long end = getEndTime(t);
	    double taskTotal = taskUtils.getTotalQuantity(t,start,end);
	    if(ar != null) {
	      if ((!ar.isSuccess())  && (taskTotal > 0)) {
		ctr++;  		    
	      }
	      else if(includeTemps && ar.isPhased()) {
		int[] ats = ar.getAspectTypes();
		int rateInd = -1;
		int startInd = -1;
		int endInd = -1;
		double totalQty=0;
		rateInd = LogisticsInventoryFormatter.getIndexForType(ats, AlpineAspectType.DEMANDRATE);
		startInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.START_TIME);
		endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);
		
		Enumeration phasedResults = ar.getPhasedResults();
		long maxPhaseEnd = 0;
		while (phasedResults.hasMoreElements()) {
		  double[] results = (double[]) phasedResults.nextElement();
		  double phaseRate = results[rateInd];
		  double phaseStart = results[startInd];
		  double phaseEnd = results[endInd];
		  long arStart = 0;
		  if(isDemand) {
		      arStart = getEffectiveProjectionStart(t,(long) phaseStart);
		  }
		  else {
		      arStart = Math.max(start,(long)phaseStart);
		  }
		  //long arEnd =(long) Math.min(end,(long) phaseEnd;
		  totalQty += taskUtils.getTotalQuantity(t,phaseRate,arStart,(long)phaseEnd);
		  maxPhaseEnd = Math.max((long)phaseEnd,maxPhaseEnd);
		} 
		//When doing all this addition of double rates some precision is lost so we need a small offset to avoid "false shortfalls".
		double offset = 0.001d;
		if(taskTotal > (totalQty + offset)) {
		    /****
		    if(getOrgName().startsWith("1-35-ARBN")) {
			logger.error("MWD: at 1-35-ARBN - item: " + getItemName() + " taskTotal is: " + taskTotal + " and totalQuantity is : " + totalQty);
		    } 
		    **/
		    ctr++;
		}
		else if (maxPhaseEnd > taskUtils.getEndTime(t)) {
		    ctr++;
		}
	      }
	    }
	  }
	}
    	return ctr;
    }

    protected int countActualShortfall(ArrayList taskList, 
				       boolean includeTemps) {
	Iterator it = taskList.iterator();
	int ctr=0;
	while(it.hasNext()) {
	    Task t = (Task) it.next();
	    PlanElement pe = t.getPlanElement();
	    if (pe != null) {
		AllocationResult ar = pe.getReportedResult();
		if (ar == null) {
		    ar = pe.getEstimatedResult();
		}
		if ((ar != null) && (!ar.isSuccess())) {
		    ctr++;  		    
		}
		else if((ar != null) && (!taskUtils.isProjection(t))) {
		    double unfilled=0;
		    double arEndTime=0;
		    double taskEndTime = taskUtils.getEndTime(t);

		    if(!ar.isPhased()) {
			unfilled = taskUtils.getQuantity(t) - taskUtils.getQuantity(ar);
			arEndTime = taskUtils.getEndTime(ar);
		    }
		    else {
			int[] ats = ar.getAspectTypes();
			int qtyInd = -1;
			int endInd = -1;
			double totalQty=0;
			qtyInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.QUANTITY);
			endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);
			Enumeration phasedResults = ar.getPhasedResults();
			while (phasedResults.hasMoreElements()) {
			    double[] results = (double[]) phasedResults.nextElement();
			    if(qtyInd != -1) {
				totalQty += results[qtyInd];
			    }
			    else {
				totalQty = taskUtils.getQuantity(t);
			    }
			    double endTime=0;
			    if(endInd == -1) {
				endTime = taskEndTime;
			    }
			    else {
				endTime = results[endInd];
			    }
			    arEndTime = Math.max(arEndTime,endTime);
			}
			unfilled = taskUtils.getQuantity(t) - totalQty;
		    }
		    double offset = 0.001d;
		    if((unfilled - offset) > 0) {
		      ctr++;
		    }
		    else if(includeTemps && (arEndTime > taskEndTime)) {
		      ctr++;
		    }
		}
	    }
	}
	return ctr;
    }



    protected double totalShortfall(ArrayList taskList) {
	Iterator it = taskList.iterator();
	double requested=0;
	double granted=0;
	while(it.hasNext()) {
	    Task t = (Task) it.next();
	    double taskQty = taskUtils.getTotalQuantity(t);
	    requested += taskQty;
	    PlanElement pe = t.getPlanElement();
	    if (pe != null) {
		AllocationResult ar = pe.getReportedResult();
		if (ar == null) {
		    ar = pe.getEstimatedResult();
		}
		if (ar == null) {
		    granted += taskQty;	    		    
		}
		else if(taskUtils.isProjection(t)) {
		    if(ar.isSuccess()) {
			granted += taskQty;
		    }
		}
		else {
		    granted += taskUtils.getQuantity(ar);
		}
	    }
	}
	double returnQty = granted - requested;

	if((returnQty <= 0) && (returnQty > -0.00005)) {
	    return 0.0;
	}
	else {
	    return returnQty;
	}
    }





    /**
     *  Take the incomming time and convert it to the beginning of the bucket
     *  in which it falls
     */
    public long truncateToBucketStart(long aTime) {
        int bucket = convertTimeToBucket(aTime, false);
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
	  logger.error("convertTimeToBucket: For Org: " + getOrgName() + "-" 
		       + getSupplyType() + " item: " + getItemName() 
		       + " - thisBucket(" + thisBucket + ") - timeZero(" + 
                       timeZero +") is < 0! (" + (thisBucket - timeZero) + 
		       ") when started with time " + 
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
        AllocationResult ar = pe.getReportedResult();
        if (ar == null) {
          ar = pe.getEstimatedResult();
        } else if (!ar.isSuccess()) {
          return PluginHelper.getStartTime(task);
        }
        return (long) PluginHelper.getStartTime(ar);
    }

    protected long getEndTime(Task task) {
        PlanElement pe = task.getPlanElement();
        // If the task has no plan element then return the EndTime Pref
        if (pe == null) {
            return PluginHelper.getEndTime(task);
        }
        AllocationResult ar = pe.getReportedResult();
        if (ar == null) {
          ar = pe.getEstimatedResult();
        }
        // make sure that we got atleast a valid reported OR estimated allocation result
        if (ar != null) {
          if (!ar.isSuccess()) {
            PluginHelper.getEndTime(task);
          }
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

	/**
	Iterator projResupplyIt = refillProjections.iterator();
	while(projResupplyIt.hasNext()) {
	    ArrayList refills = (ArrayList)projResupplyIt.next();
	    if(refills != null) {
		for(int i=0; i < refills.size(); i++) {
		    Task task = (Task) refills.get(i);
		    if(!tmpProjResupply.contains(task)) {
			tmpProjResupply.add(task);
		    }
		}
	    }
	}
	**/

	projSupplyList = deepClone(refillProjections);
	supplyList = deepClone(refillRequisitions);

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
	    ArrayList flatProjList = getFlattenedList(projSupplyList);
            for (int i = 0; i < flatProjList.size(); i++) {
                logger.error(taskUtils.taskDesc((Task) flatProjList.get(i)));
            }
            logger.error("********* SupplyList ********");
	    ArrayList flatSupplyList = getFlattenedList(supplyList);
            for (int i = 0; i < flatSupplyList.size(); i++) {
                logger.error(taskUtils.taskDesc((Task) flatSupplyList.get(i)));
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

    public String getSupplyType() {
	SupplyClassPG pg = (SupplyClassPG)
myPG.getResource().searchForPropertyGroup(SupplyClassPG.class);
        return pg.getSupplyType();
    }
}
