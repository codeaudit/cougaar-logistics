/*--------------------------------------------------------------------------
 *                         RESTRICTED RIGHTS LEGEND
 *
 *   Use, duplication, or disclosure by the Government is subject to
 *   restrictions as set forth in the Rights in Technical Data and Computer
 *   Software Clause at DFARS 52.227-7013.
 *
 *                             BBNT Solutions LLC,
 *                             10 Moulton Street
 *                            Cambridge, MA 02138
 *                              (617) 873-3000
 *
 *   Copyright 2000 by
 *             BBNT Solutions LLC,
 *             all rights reserved.
 *
 * --------------------------------------------------------------------------*/
package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Duration;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.Scalar;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.plugins.ScheduleUtils;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.glm.plugins.TimeUtils;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.logistics.plugin.inventory.InventoryPlugin;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryLogger;
import org.cougaar.logistics.plugin.inventory.TaskUtils;

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
  private Duration durationArray[];
  protected LogisticsInventoryPG myPG;
  protected long startTime;
  protected int timeZero;
  private transient LoggingService logger;
  private transient LogisticsInventoryLogger csvLogger=null;
  private transient LogisticsInventoryFormatter csvWriter=null;
  // customerHash holds the time(long) of the last actual is seen
  // for each customer
  private HashMap customerHash;
  private TaskUtils taskUtils;

  protected int       endOfLevelSixBucket;
  protected ArrayList refillProjections;
  protected ArrayList refillRequisitions;
  protected ArrayList dueOutList;
  // projectedDemandList mirrors dueOutList, each element
  // contains the sum of all Projected Demand for corresponding
  // bucket in dueOutList
  protected double projectedDemandArray[];
  protected double criticalLevelsArray[];
  protected double inventoryLevelsArray[];
  // Policy inputs
  protected int criticalLevel = 3;
  protected int reorderPeriod = 3;
  protected int bucketSize = 1;
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
  
  public LogisticsInventoryBG(LogisticsInventoryPG pg) {
    myPG = pg;
    customerHash = new HashMap();
    dueOutList = new ArrayList();
    refillProjections = new ArrayList();
    refillRequisitions = new ArrayList();
    projectedDemandArray = new double[180];
    criticalLevelsArray  = new double[180];
    inventoryLevelsArray = new double[180];
    Arrays.fill(criticalLevelsArray,Double.NEGATIVE_INFINITY);
    durationArray = new Duration[15];
    for (int i=0; i <= 14; i++) {
      durationArray[i] = Duration.newDays(0.0+i);
    }
    projWithdrawList = new ArrayList();
    withdrawList = new ArrayList();
    projSupplyList = new ArrayList();
    supplyList = new ArrayList();
    targetLevelsList = new ArrayList();
    actualDemandTasksList = new ArrayList();
    endOfLevelSixBucket = 1;
  }


  public void initialize(long startTime, int criticalLevel, int reorderPeriod, int bucketSize, long now, boolean logToCSV, InventoryPlugin parentPlugin) {
    this.startTime = startTime;
    // Set initial level of inventory from time zero to today.  This assumes that the inventory
    // is created because the existence of demand and the RefillGenerator will be run
    // on this inventory before time has advanced, thus the RefillGenerator will set 
    // the inventory level for today and in general we always assume that levels prior
    // to today have been set before the RefillGenerator is run
    // Contract: the inventory for yesterday is always valid because initially it is
    // set by the behavior group of the inventory.
    timeZero = (int)((startTime/MSEC_PER_BUCKET) - 1);
    int now_bucket = convertTimeToBucket(now);
    

    //Initialize with initial level since the refill generator won't start setting inv levels
    //until the first day it processes which is today + OST - so depending on OST it could be a while.
    inventoryLevelsArray[0] = myPG.getInitialLevel();

    logger = parentPlugin.getLoggingService(this);
    if(logToCSV) {
	csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(),myPG.getOrg(),false,parentPlugin);
	csvWriter = new LogisticsInventoryFormatter(csvLogger, parentPlugin);
    }
    taskUtils = parentPlugin.getTaskUtils();

    logger.debug("Start day: "+TimeUtils.dateString(startTime)+", time zero "+TimeUtils.dateString(timeZero));
    this.criticalLevel = criticalLevel;
    this.reorderPeriod = reorderPeriod;
    this.bucketSize = bucketSize;
    MSEC_PER_BUCKET = bucketSize * TimeUtils.MSEC_PER_DAY;
    bufferedCriticalLevels = 
      ScheduleUtils.buildSimpleQuantitySchedule(0, startTime, startTime+(TimeUtils.MSEC_PER_DAY*10));
    bufferedTargetLevels = 
      ScheduleUtils.buildSimpleQuantitySchedule(0, startTime, startTime+(TimeUtils.MSEC_PER_DAY*10));
    bufferedInventoryLevels =
      ScheduleUtils.buildSimpleQuantitySchedule(myPG.getInitialLevel(), 
						startTime, startTime+(TimeUtils.MSEC_PER_DAY*10));
  }

    //Call reinitialize which does the default that has to be done
    //on set up after rehydration.   It does the minimum initialization
    //that has to be done, even upon rehydration.  
    //which does the initial set up when the inventory is created.
  public void reinitialize(boolean logToCSV, InventoryPlugin parentPlugin) {
    logger = parentPlugin.getLoggingService(this);
    if(logToCSV) {
	csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(),myPG.getOrg(),true,parentPlugin);
	csvWriter = new LogisticsInventoryFormatter(csvLogger, parentPlugin);
    }
    taskUtils = parentPlugin.getTaskUtils();
  }

  public void addWithdrawProjectionAllocation(Task task) {
    AllocationResult result = task.getPlanElement().getEstimatedResult();
    Iterator it = null;
    if (result.isPhased()) {
      it = result.getPhasedAspectValueResults().iterator();
    } else {
      ArrayList tmp = new ArrayList();
      tmp.add(result.getAspectValueResults());
      it = tmp.iterator();
    }
    while (it.hasNext()) {
      AspectValue phase[] = (AspectValue[]) it.next();
      int start = 0;
      int end = 0;
      for (int i=0; i <phase.length;i++) {
        AspectValue aValue = phase[i];
        if (aValue.getAspectType() == AspectType.END_TIME) {
          end = convertTimeToBucket((long) aValue.getValue());
        } else if (aValue.getAspectType() == AspectType.START_TIME) {
          start = convertTimeToBucket((long) aValue.getValue());
        }
      }
      while (end >= dueOutList.size()) {
        dueOutList.add(new ArrayList());
      }
      for (; start < end; start++) {
        ArrayList list = (ArrayList)dueOutList.get(start);
        list.add(task);
      }
    }
  }

  public void addWithdrawProjection(Task task) {
    // Adding projections mean changed critical levels and
    // target levels.  Set boolean to recompute critical
    // levels and clear targetLevelsList for CSV logging
    compute_critical_levels = true;
    targetLevelsList.clear();
    long start = getStartTime(task);
    long end = getEndTime(task);
    int bucket_start = convertTimeToBucket(start);
    int bucket_end = convertTimeToBucket(end);
    if (bucket_end >= projectedDemandArray.length) {
      projectedDemandArray = expandArray(projectedDemandArray);
    }
    while (bucket_end >= dueOutList.size()) {
      dueOutList.add(new ArrayList());
    }
//      System.out.println("Task start: "+TimeUtils.dateString(start)+" end  : "+TimeUtils.dateString(end));
//      System.out.println("bucket start "+bucket_start+", bucket end "+bucket_end);
    for (int i=bucket_start; i < bucket_end; i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      list.add(task);
      updateProjectedDemandList(task, i, start, end, true);
    }
  }

  public void addWithdrawRequisition(Task task) {
    long endTime = getEndTime(task);
    Object org = TaskUtils.getCustomer(task);
    if (org != null) {
      Long lastActualSeen = (Long)customerHash.get(org);
      if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
	customerHash.put(org, new Long(endTime));
      }
    }
    int bucket = convertTimeToBucket(endTime);
    while (bucket >= dueOutList.size()) {
      dueOutList.add(new ArrayList());
    }
    ArrayList list = (ArrayList)dueOutList.get(bucket);
    list.add(task);
  }

  private void regenerateProjectedDemandList() {
    // clear out old demand
    Arrays.fill(projectedDemandArray, 0.0);
    int size = dueOutList.size();
    Collection list;
    Iterator list_itr;
    Task task;
    for (int i=0; i < size; i++) {
      list = getProjectedDemandTasks(i);
      list_itr = list.iterator();
      while (list_itr.hasNext()) {
	task = (Task)list_itr.next();
	long start = getStartTime(task);
	long end = getEndTime(task);
	updateProjectedDemandList(task, i, start, end, true);
      }
    }
  }

  // ******* HERE LIES A BUG
  // Boundary condition bug - daily projected demands do not
  // jive when comparing daily buckets to 3-day buckets
  // Updates the running demand sum per bucket
  private void updateProjectedDemandList(Task task, int bucket, long start, long end, boolean add) {

    double demand = getProjectionTaskDemand(task, bucket, start, end);
    if (add){
      if (bucket >= projectedDemandArray.length) {
      projectedDemandArray = expandArray(projectedDemandArray);
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
    int days_spanned = getDaysSpanned(bucket, start, end);
    if ((days_spanned > 0) && !(days_spanned > bucketSize)) {
      Rate rate = taskUtils.getRate(task);
      Duration d = durationArray[days_spanned];
      Scalar scalar = (Scalar)rate.computeNumerator(d);
      return taskUtils.getDouble(scalar);
    }
    return 0.0;
  }
  // Beth,  Because allocation results on Withdraw tasks can change,
  // from one transaction to the next, I do not know which bucket to
  // look in for this task.  Is there a better way of doing this?
  // Do you want to treat Withdraws the way we treat ProjectWithdraws?
  // (remove, readd)
  public void removeWithdrawRequisition(Task task) {
    for (int i=0; i < dueOutList.size(); i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      list.remove(task);
    }
  }

  /** Called by SupplyExpander to remove a Withdraw task that has either
   *  been rescinded or changed.
   *  @param task  The ProjectWithdraw task being removed
   **/
  public void removeWithdrawProjection(Task task) {
    compute_critical_levels = true;
    regenerate_projected_demand = true;
    ArrayList list;
    for (int i=0; i < dueOutList.size(); i++) {
      list = (ArrayList)dueOutList.get(i);
      list.remove(task);
    }
  }

  public void removeWithdrawProjectionAllocation(Task task) {
    removeWithdrawProjection(task);
  }

  public void addRefillRequisition(Task task) {
    long endTime = getEndTime(task);
    int bucket = convertTimeToBucket(endTime);
    while (bucket >= refillRequisitions.size()) {
      refillRequisitions.add(null);
    }
    refillRequisitions.set(bucket, task);
  }

  public int getLastWithdrawBucket() {
    Iterator list = customerHash.values().iterator();
    long lastSeen = convertBucketToTime(1);
    while (list.hasNext()) {
      long time = ((Long)list.next()).longValue();
      if (lastSeen < time) {
	lastSeen = time;
      }
    }
    return convertTimeToBucket(lastSeen);
  }

  public int getFirstProjectWithdrawBucket() {
    Iterator list = customerHash.values().iterator();
    // if we don't have any actual demand signal with -1
    if (!list.hasNext()) {
	return -1;
    }
    long firstSeen = convertBucketToTime(dueOutList.size()-1);
    while (list.hasNext()) {
      long time = ((Long)list.next()).longValue();
      if (firstSeen > time) {
	firstSeen = time;
      }
    }
    // return the bucket after the first seen actual's end time bucket
    // consistent with getEffectiveProjectionStart
    return (convertTimeToBucket(firstSeen) + 1);
  }

    public int getLastRefillRequisition() {
	int lastRefill = -1;
	for (int i=0; i < refillRequisitions.size(); i++) {
	   Task task = (Task)refillRequisitions.get(i);
	   if (task != null) {
	       lastRefill = i;
	   }
	}
	return lastRefill;
    }
	

  public int getLastDemandBucket() {
    return dueOutList.size()-1;
  }

  public void setEndOfLevelSixBucket(int bucket) {
    endOfLevelSixBucket = bucket;
  }

  public int getEndOfLevelSixBucket() {
    return endOfLevelSixBucket;
  }

  public void addRefillProjection(Task task) {
    long start = getStartTime(task);
    long end = getEndTime(task);
    int bucket_start = convertTimeToBucket(start);
    int bucket_end = convertTimeToBucket(end);
    while (bucket_end >= refillProjections.size()) {
      refillProjections.add(null);
    }
    for (int i=bucket_start; i < bucket_end; i++) {
      refillProjections.set(i, task);
    }
  }

  public void removeRefillProjection(Task task) {
    int index;
    while ((index = refillProjections.indexOf(task)) != -1) {
	refillProjections.set(index, null);
    }
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
    for (int i=0; i < refillRequisitions.size(); i++) {
      task = (Task)refillRequisitions.get(i);
      if ((task != null) && task.beforeCommitment(now)) {
	refillRequisitions.set(i, null);
	taskList.add(task);
      }
    }
     return taskList;
  }

  // clear the projections (all of them)
  // and return the removed tasks for comparison
  public List clearRefillProjectionTasks() {
    ArrayList removedTaskList = new ArrayList(refillProjections);
    refillProjections.clear();
    return removedTaskList;
  }
	

  public ArrayList getRefillRequisitions() {
    return (ArrayList)refillRequisitions.clone();
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

  private double[] computeCriticalLevels() {
    if (regenerate_projected_demand) {
      regenerateProjectedDemandList();
      regenerate_projected_demand = false;
    }
    long days_per_bucket = MSEC_PER_BUCKET/TimeUtils.MSEC_PER_DAY;
    double cl_per_bucket = (double)criticalLevel/(double)days_per_bucket;
    int mode = (int)Math.floor(cl_per_bucket);
    double Ci;
    criticalLevelsArray = new double[projectedDemandArray.length];
    // Number of days in criticalLevel falls within a single bucket
    if (mode == 0) { 
      for (int i=0; i < projectedDemandArray.length; i++) {
	Ci =  projectedDemandArray[i] * cl_per_bucket;
	criticalLevelsArray[i] = Ci;
      }
    } else { // Number of days in criticalLevel spans multiple buckets
      int buckets = (int)Math.floor(cl_per_bucket);
      double fe = cl_per_bucket - buckets;
      Ci = 0.0;
      for (int i=1; i <= buckets; i++) {
	Ci += projectedDemandArray[i];
      }
      Ci += projectedDemandArray[buckets+1]*fe;
      criticalLevelsArray[0] = Ci;
      for (int i=1; i < projectedDemandArray.length-buckets-1; i++) {
	Ci =  Ci - projectedDemandArray[i] + projectedDemandArray[i+buckets]*(1-fe)+
	  projectedDemandArray[i+buckets+1]*fe;
	criticalLevelsArray[i] = Ci;
      }
      for (int i=projectedDemandArray.length-buckets-1; i < projectedDemandArray.length; i++) {
	Ci =  Ci - projectedDemandArray[i];
	criticalLevelsArray[i] = Ci;
      }
    }
    return criticalLevelsArray;
  }

  public double getLevel(int bucket) {
    if (bucket >= inventoryLevelsArray.length) {
      return inventoryLevelsArray[inventoryLevelsArray.length-1];
    }
    return inventoryLevelsArray[bucket];
  }

  public void setLevel(int bucket, double value) {
    if (bucket >= inventoryLevelsArray.length) {
      inventoryLevelsArray = expandArray(inventoryLevelsArray);
    }
    inventoryLevelsArray[bucket] = value;
  }

  public void setTarget(int bucket, double value) {
    // The intention of the List is to hold values for the buckets
    // that have target levels and hold nulls for those buckets that
    // do not have a target level
    int len = targetLevelsList.size();
    if (bucket >= len) {
      for (int i=len; i < bucket+20; i++) {
	targetLevelsList.add(null);
      }
    }
    targetLevelsList.set(bucket, new Double(value));
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
    removeWithdrawProjectionAllocation(task);
    addWithdrawProjectionAllocation(task);
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
    Iterator dueOutIter = ((ArrayList)dueOutList.get(bucket)).iterator();
    while (dueOutIter.hasNext()) {
      Task task = (Task)dueOutIter.next();
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
	  int days_spanned = getDaysSpanned(bucket, start, end);
	  rate = taskUtils.getRate(task);
	  try {
	    scalar = (Scalar)rate.computeNumerator(durationArray[days_spanned]);
	    actualDemand += taskUtils.getDouble(scalar);	
	  } catch(Exception e) {
	    logger.error(taskUtils.taskDesc(task)+
			 " Start: "+TimeUtils.dateString(start)+
			 " days_spanned: "+days_spanned);
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
    Iterator dueOutIter = ((ArrayList)dueOutList.get(bucket)).iterator();
    while (dueOutIter.hasNext()) {
      Task task = (Task)dueOutIter.next();
      if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
	long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
	long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
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
      }
    else {
      return true;
    }
  }   

  private int getDaysSpanned(int bucket, long start, long end) {
    long bucket_start = convertBucketToTime(bucket);
    long bucket_end = bucket_start + MSEC_PER_BUCKET;
    long interval_start = Math.max(start, bucket_start);
    long interval_end = Math.min(end, bucket_end);
    int value = (int)((interval_end - interval_start)/TimeUtils.MSEC_PER_DAY);
//     if (value < 0) {
//       logger.error("Bucket start: "+bucket_start+", Bucket end: "+bucket_end+
// 		   ", Task start: "+TimeUtils.dateString(start)+
// 		   ", Task end: "+TimeUtils.dateString(end)+
// 		   "interval start: "+TimeUtils.dateString(interval_start)+
// 		   "interval end: "+TimeUtils.dateString(interval_end));
//     }
    if (value > bucketSize) {
      logger.error("bucket "+bucket+", Bucket start "+TimeUtils.dateString(bucket_start)+
		   ", end "+TimeUtils.dateString(bucket_end));
      logger.error("Task start "+TimeUtils.dateString(start)+", end "+TimeUtils.dateString(end));
      logger.error("Interval start "+TimeUtils.dateString(interval_start)+", end "+
		   TimeUtils.dateString(interval_end));
      int b_end = convertTimeToBucket(end);
      logger.error("Calculated bucket end "+b_end+", task end "+TimeUtils.dateString(end));
    }
    return value;
  }

  public double getReorderPeriod() {
    return Math.ceil((double)reorderPeriod/(double)(MSEC_PER_BUCKET/TimeUtils.MSEC_PER_DAY));
  }

  /** 
      Ignore projected demand that occurs before customer switchover day.
   **/
  public long getEffectiveProjectionStart(Task task, long start) {
    long firstProjectionDay;
    Object org = taskUtils.getCustomer(task);
    if (org != null) {
      Long lastActualSeen = (Long)customerHash.get(org);
      if (lastActualSeen != null){
	firstProjectionDay = lastActualSeen.longValue() + TimeUtils.MSEC_PER_DAY;
	if (firstProjectionDay > start) {
	  return firstProjectionDay;
	}
      }
    }
    return start;
  }

  private Collection getTasks(int bucket, String verb) {
    ArrayList task_list = new ArrayList();
    if (bucket < dueOutList.size()) {
      Iterator list = ((ArrayList)dueOutList.get(bucket)).iterator();
      Task task;
      while (list.hasNext()) {
	task = (Task)list.next();
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
    for (int i=dueOutList.size()-1; i >= 0; i--) {
      list = (ArrayList)dueOutList.get(i);
      if (!list.isEmpty()) {
	listIter = list.iterator();
	while (listIter.hasNext()) {
	  task = (Task)listIter.next();
	  if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
	    org = TaskUtils.getCustomer(task);
	    endTime = getEndTime(task);
	    lastActualSeen = (Long)customerHash.get(org);
	    if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
	      customerHash.put(org, new Long(endTime));
	    }
	  }
	}
      }
    }  
  }

  public void logAllToCSVFile(long aCycleStamp) {
      if(csvLogger != null) {
	  csvWriter.logToExcelOutput(myPG,
				     aCycleStamp);
      }
  }

  public long getStartTime() {
    return startTime;
  }

    public ArrayList getProjWithdrawList() {return projWithdrawList;}
    public ArrayList getWithdrawList() {return withdrawList;}
    public ArrayList getProjSupplyList() {return projSupplyList;}
    public ArrayList getSupplyList() { return supplyList;}
    public Schedule  getBufferedCritLevels() { return bufferedCriticalLevels;}
    public Schedule  getBufferedInvLevels() { return bufferedInventoryLevels;}
    public Schedule getBufferedTargetLevels() { return bufferedTargetLevels;}
  public ArrayList getActualDemandTasksList() { return actualDemandTasksList;}

  /**
   * Convert a time (long) into a bucket of this inventory that can be
   * used to index duein/out vectors, levels, etc.
   **/
  public int convertTimeToBucket(long time) {
    int thisDay = (int) (time / MSEC_PER_BUCKET);
    return thisDay - timeZero;
  }

  /**
   * Convert a bucket (int) into the start time of the bucket.
   * The end time of the bucket must be inferred from the 
   * next sequential bucket's start time (non-inclusive).
   **/
  public long convertBucketToTime(int bucket) {
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
    return (long)PluginHelper.getStartTime(pe.getEstimatedResult());
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
    
  private double[] expandArray(double[] doubleArray) {
    double biggerArray[] = new double[(int)(doubleArray.length*1.5)];
    for (int i=0; i < doubleArray.length; i++) {
      biggerArray[i] = doubleArray[i];
    }
    return biggerArray;
  }

  public int getCriticalLevel() { return criticalLevel; }

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
    for (int i=0; i < criticalLevelsArray.length; i++) {
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
    for (int i=0; i < inventoryLevelsArray.length; i++) {
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
    return new LogisticsInventoryBG((LogisticsInventoryPG)pg);
  }

  private int printQuantityScheduleTimes(Schedule sched) {
    Enumeration elements = sched.getAllScheduleElements();
    QuantityScheduleElement qse;
    while (elements.hasMoreElements()) {
      qse = (QuantityScheduleElement)elements.nextElement();
      logger.info("qty: "+qse.getQuantity()+
		  " "+qse.getStartDate()+" to "+ qse.getEndDate());
    }
    return 0;
  }

  public void Test() {
//      logger.error("Bucket size is "+MSEC_PER_BUCKET/TimeUtils.MSEC_PER_DAY);
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
    logger.error("********* ProjectWithdrawList ********");
    for (int i=0; i < projWithdrawList.size(); i++) {
      logger.error(taskUtils.taskDesc((Task)projWithdrawList.get(i)));
    }
    logger.error("********* WithdrawList ********");
    for (int i=0; i < withdrawList.size(); i++) {
      logger.error(taskUtils.taskDesc((Task)withdrawList.get(i)));
    }
    logger.error("********* ProjectSupplyList ********");
    for (int i=0; i < projSupplyList.size(); i++) {
      logger.error(taskUtils.taskDesc((Task)projSupplyList.get(i)));
    }
    logger.error("********* SupplyList ********");
    for (int i=0; i < supplyList.size(); i++) {
      logger.error(taskUtils.taskDesc((Task)supplyList.get(i)));
    }
    logger.error("********* Buffered Critical Levels ********");
    printQuantityScheduleTimes(bufferedCriticalLevels);
    logger.error("********* Buffered Inventory Levels ********");
    printQuantityScheduleTimes(bufferedInventoryLevels);
  }

}
