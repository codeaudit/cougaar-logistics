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
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.AspectType;
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
import org.cougaar.core.plugin.util.AllocationResultHelper;
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
  protected Schedule bufferedCriticalLevels;
  protected Schedule bufferedInventoryLevels;
  private boolean failures = false;
  private boolean compute_critical_levels = true;
  
  public LogisticsInventoryBG(LogisticsInventoryPG pg) {
    myPG = pg;
    customerHash = new HashMap();
    dueOutList = new ArrayList();
    refillProjections = new ArrayList();
    refillRequisitions = new ArrayList();
    projectedDemandArray = new double[180];
    criticalLevelsArray  = new double[180];
    inventoryLevelsArray = new double[180];
    durationArray = new Duration[15];
    for (int i=0; i <= 14; i++) {
      durationArray[i] = Duration.newDays(0.0+i);
    }
    projWithdrawList = new ArrayList();
    withdrawList = new ArrayList();
    projSupplyList = new ArrayList();
    supplyList = new ArrayList();
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
    Arrays.fill(inventoryLevelsArray, 0, now_bucket, myPG.getInitialLevel());
    logger = parentPlugin.getLoggingService(this);
    if(logToCSV) {
	csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(),myPG.getOrg(),parentPlugin);
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
    bufferedInventoryLevels =
      ScheduleUtils.buildSimpleQuantitySchedule(myPG.getInitialLevel(), 
						startTime, startTime+(TimeUtils.MSEC_PER_DAY*10));
  }

  public void addWithdrawProjectionAllocation(Task task) {
    LogisticsAllocationResultHelper helper = 
      new LogisticsAllocationResultHelper(task, task.getPlanElement());
    for (int i=0, n=helper.getPhaseCount(); i < n; i++) {
      LogisticsAllocationResultHelper.Phase phase =
	(LogisticsAllocationResultHelper.Phase) helper.getPhase(i);
      double qty = phase.getAspectValue(AlpineAspectType.DEMANDRATE).getValue();
      int start = convertTimeToBucket(phase.getStartTime());
      int end = convertTimeToBucket(phase.getEndTime());
      while (end >= dueOutList.size()) {
	dueOutList.add(new ArrayList());
      }
      for (; start < end; start++) {
	ArrayList list = (ArrayList)dueOutList.get(i);
	list.add(task);
      }
    }
  }

  public void addWithdrawProjection(Task task) {
    compute_critical_levels = true;
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

  // ******* HERE LIES A BUG
  // Boundary condition bug - daily projected demands do not
  // jive when comparing daily buckets to 3-day buckets
  // Updates the running demand sum per bucket
  private void updateProjectedDemandList(Task task, int bucket, long start, long end, boolean add) {

    double demand = getProjectionTaskDemand(task, bucket, start, end);
    if (add)
      projectedDemandArray[bucket] += demand;
    else
      projectedDemandArray[bucket] -= demand;
//      logger.error("Bucket "+bucket+": new demand "+demand+", total is "+
//  		 projectedDemandArray[bucket]);
  }

  public double getProjectionTaskDemand(Task task, int bucket, long start, long end) {
    int days_spanned = getDaysSpanned(bucket, start, end);
    Rate rate = taskUtils.getRate(task);
    Scalar scalar = (Scalar)rate.computeNumerator(durationArray[days_spanned]);
    return taskUtils.getDouble(scalar);
  }
  // Beth,  Because allocation results on Withdraw tasks can change,
  // from one transaction to the next, I do not know which bucket to
  // look in for this task.  Is there a better way of doing this?
  // Do you want to treat Withdraws the way we treat ProjectWithdraws?
  // (remove, readd)
  public void removeWithdrawRequisition(Task task) {
    int index;
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
    long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
    int bucket_start = convertTimeToBucket(start);
    int bucket_end = convertTimeToBucket(end);
    for (int i=bucket_start; i < bucket_end; i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      list.remove(task);
      updateProjectedDemandList(task, i, start, end, false);
    }
  }

  public void removeWithdrawProjectionAllocation(Task task) {
    int size = dueOutList.size();
    ArrayList list;
    for (int i=0; i < size; i++) {
      list = (ArrayList)dueOutList.get(i);
      list.remove(task);
    }
  }

  public void addRefillRequisition(Task task) {
    long endTime = getEndTime(task);
    int bucket = convertTimeToBucket(endTime);
    while (bucket >= refillRequisitions.size()) {
      refillRequisitions.add(null);
    }
    refillRequisitions.set(bucket, task);
  }

  public int getLastRefillRequisitionBucket() {
    int last = (refillRequisitions.size() - 1);
    if (refillRequisitions.get(last) != null) {
      return last;
    } else {
      Object validLast = null;
      while (validLast == null) {
        last = last -1;
        validLast = refillRequisitions.get(last);
      }
      return last;
    }
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
    long days_per_bucket = MSEC_PER_BUCKET/TimeUtils.MSEC_PER_DAY;
    double cl_per_bucket = (double)criticalLevel/(double)days_per_bucket;
//      System.out.println("criticalLevel: "+criticalLevel+", days_per_bucket: "+
//  		       days_per_bucket+", cl_per_bucket: "+cl_per_bucket);
    int mode = (int)Math.floor(cl_per_bucket);
    long start = convertBucketToTime(0);
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

    if (bucket > projectedDemandArray.length) {
      return 0.0;
    }
    return projectedDemandArray[bucket];
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
	start = getProjectedDemandStart(task, start);
	long end_of_bucket = (convertBucketToTime(bucket + 1) - 1);
	if ((start < end) &&
	    (start < end_of_bucket)) {
	  int days_spanned = getDaysSpanned(bucket, start, end);
	  rate = taskUtils.getRate(task);
	  scalar = (Scalar)rate.computeNumerator(durationArray[days_spanned]);
	  actualDemand += taskUtils.getDouble(scalar);	
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
      if (taskUtils.isProjection(task)) {
	long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
	long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
	start = getProjectedDemandStart(task, start);
	if (start < end) {
          demand.add(task);
        }
      } else {
        demand.add(task);
      }
    }
    return demand;
  }


  private int getDaysSpanned(int bucket, long start, long end) {
    long bucket_start = convertBucketToTime(bucket);
    long bucket_end = bucket_start + MSEC_PER_BUCKET;
    long interval_start = Math.max(start, bucket_start);
    long interval_end = Math.min(end, bucket_end);
    return (int)((interval_end - interval_start)/TimeUtils.MSEC_PER_DAY);
  }

  public double getReorderPeriod() {
    return Math.ceil((double)reorderPeriod/(double)(MSEC_PER_BUCKET/TimeUtils.MSEC_PER_DAY));
  }

  private long getProjectedDemandStart(Task task, long start) {
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

  public void logAllToCSVFile(long aCycleStamp) {
      if(csvLogger != null) {
	  csvWriter.logToExcelOutput(withdrawList,
				     projWithdrawList,
				     supplyList,
				     projSupplyList,
				     bufferedCriticalLevels,
				     bufferedInventoryLevels,
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
    AllocationResultHelper helper = new AllocationResultHelper(task, pe);
    return (long)PluginHelper.getStartTime(helper.getAllocationResult());
  }

  private long getEndTime(Task task) {
    PlanElement pe = task.getPlanElement();
    // If the task has no plan element then return the EndTime Pref
    if (pe == null) {
      return PluginHelper.getEndTime(task);
    }
    AllocationResultHelper helper = new AllocationResultHelper(task, pe);
    return (long)PluginHelper.getEndTime(helper.getAllocationResult());
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
    projSupplyList = (ArrayList)refillProjections.clone();
    supplyList = (ArrayList)refillRequisitions.clone();
    QuantityScheduleElement qse;
    Vector list = new Vector();
    long start = convertBucketToTime(0);
    for (int i=0; i < criticalLevelsArray.length; i++) {
      list.add(ScheduleUtils.buildQuantityScheduleElement(criticalLevelsArray[i],
							  start, start+MSEC_PER_BUCKET));
      start += MSEC_PER_BUCKET;
    }
    bufferedCriticalLevels = GLMFactory.newQuantitySchedule(list.elements(), 
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
      System.out.println("qty: "+qse.getQuantity()+
			 " "+qse.getStartDate()+" to "+ qse.getEndDate());
    }
    return 0;
  }

  public void Test() {
//      System.out.println("Bucket size is "+MSEC_PER_BUCKET/TimeUtils.MSEC_PER_DAY);
//      System.out.println("ReorderPeriod is "+reorderPeriod+", getReorderPeriod() "+getReorderPeriod()+
//  		       ", critical level "+criticalLevel);
//      computeCriticalLevels();
//      setLevel(0, myPG.getInitialLevel());
//      for (int i=1; i < projectedDemandArray.length; i++) {
//        double new_level = getLevel(i-1) - projectedDemandArray[i];
//        setLevel(i, new_level);
//      }
//      System.out.println("Date for Bucket Zero is "+TimeUtils.dateString(convertBucketToTime(0)));
//      for (int i=0; i < projectedDemandArray.length; i++) {
//        System.out.println("Bucket "+i+", Demand "+getActualDemand(i)+", criticalLevel "+
//  			 criticalLevelsArray[i]+" Level "+inventoryLevelsArray[i]);
//      }
    System.out.println("********* ProjectWithdrawList ********");
    for (int i=0; i < projWithdrawList.size(); i++) {
      System.out.println(taskUtils.taskDesc((Task)projWithdrawList.get(i)));
    }
    System.out.println("********* WithdrawList ********");
    for (int i=0; i < withdrawList.size(); i++) {
      System.out.println(taskUtils.taskDesc((Task)withdrawList.get(i)));
    }
    System.out.println("********* ProjectSupplyList ********");
    for (int i=0; i < projSupplyList.size(); i++) {
      System.out.println(taskUtils.taskDesc((Task)projSupplyList.get(i)));
    }
    System.out.println("********* SupplyList ********");
    for (int i=0; i < supplyList.size(); i++) {
      System.out.println(taskUtils.taskDesc((Task)supplyList.get(i)));
    }
    System.out.println("********* Buffered Critical Levels ********");
    printQuantityScheduleTimes(bufferedCriticalLevels);
    System.out.println("********* Buffered Inventory Levels ********");
    printQuantityScheduleTimes(bufferedInventoryLevels);
  }

}
