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
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.plugins.ScheduleUtils;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.glm.plugins.TimeUtils;

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
  private long MSEC_PER_BUCKET = TimeUtils.MSEC_PER_DAY * 3;
  private Duration durationArray[];
  protected LogisticsInventoryPG myPG;
  protected long startTime;
  protected int timeZero;
  private LoggingService logger;
  private LogisticsInventoryLogger csvLogger=null;
  private LogisticsInventoryFormatter csvWriter=null;
  // customerHash holds the time(long) of the last actual is seen
  // for each customer
  private HashMap customerHash;
  private TaskUtils taskUtils;

  protected ArrayList refillProjections;
  protected ArrayList refillRequisitions;
  protected ArrayList dueOutList;
//    protected Schedule inventoryLevelsSchedule;
  protected Schedule criticalLevelsSchedule;
  // projectedDemandList mirrors dueOutList, each element
  // contains the sum of all Projected Demand for corresponding
  // bucket in dueOutList
  protected double projectedDemandArray[];
  // Policy inputs
  protected int criticalLevel = 3;
  // Lists for csv logging & UI support
  protected ArrayList projWithdrawList;
  protected ArrayList withdrawList;
  protected ArrayList projSupplyList;
  protected ArrayList supplyList;
  protected Schedule  bufferedCritLevels;
  // protected <unknown> bufferedInvLevels;
  
  public LogisticsInventoryBG(LogisticsInventoryPG pg) {
    myPG = pg;
    customerHash = new HashMap();
    dueOutList = new ArrayList();
    refillProjections = new ArrayList();
    refillRequisitions = new ArrayList();
    projectedDemandArray = new double[180];
    durationArray = new Duration[15];
    for (int i=0; i <= 14; i++) {
      durationArray[i] = Duration.newDays(0.0+i);
    }
    projWithdrawList = new ArrayList();
    withdrawList = new ArrayList();
    projSupplyList = new ArrayList();
    supplyList = new ArrayList();
  }

  public void initialize(long today, int criticalLevel, InventoryPlugin parentPlugin) {
    startTime = today;
    timeZero = (int)(startTime/MSEC_PER_BUCKET);
    logger = parentPlugin.getLoggingService(this);
    if(false) {
	csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(),parentPlugin.getMyOrganization(),parentPlugin);
	csvWriter = new LogisticsInventoryFormatter(csvLogger, parentPlugin);
    }
    taskUtils = parentPlugin.getTaskUtils();
    logger.debug("Start day: "+TimeUtils.dateString(today)+", time zero "+TimeUtils.dateString(timeZero));
    System.out.println("Start day: "+TimeUtils.dateString(today)+", time zero "+TimeUtils.dateString(timeZero));
    this.criticalLevel = criticalLevel;
  }

  public void addWithdrawProjection(Task task) {
    long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
    int bucket_start = convertTimeToBucket(start);
    int bucket_end = convertTimeToBucket(end);
    if (bucket_end >= projectedDemandArray.length) {
      projectedDemandArray = expandArray(projectedDemandArray);
    }
//      logger.error("\n"+taskUtils.taskDesc(task));
//      logger.error("start: "+TimeUtils.dateString(start));
//      logger.error("end  : "+TimeUtils.dateString(end));
//      logger.error("bucket start "+bucket_start+", bucket end "+bucket_end);
    while (bucket_end >= dueOutList.size()) {
      dueOutList.add(new ArrayList());
    }
//  System.out.println("Task start: "+TimeUtils.dateString(start)+" end  : "+TimeUtils.dateString(end));
//  System.out.println("bucket start "+bucket_start+", bucket end "+bucket_end);
    for (int i=bucket_start; i < bucket_end; i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      list.add(task);
      updateProjectedDemandList(task, i, start, end);
    }
  }

  public void addWithdrawRequisition(Task task) {
    long endTime = PluginHelper.getEndTime(task);
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

  // Updates the running demand sum per bucket
  private void updateProjectedDemandList(Task task, int bucket,
					 long start, long end) {
    long bucket_start = convertBucketToTime(bucket);
    long bucket_end = bucket_start + MSEC_PER_BUCKET;
    long interval_start = Math.max(start, bucket_start);
    long interval_end = Math.min(end, bucket_end);
    int days_spanned = (int)((interval_end - interval_start)/TimeUtils.MSEC_PER_DAY);
    Rate rate = taskUtils.getRate(task);
    Scalar scalar = (Scalar)rate.computeNumerator(durationArray[days_spanned]);
    double demand = taskUtils.getDouble(scalar);
    projectedDemandArray[bucket] += demand;
//      logger.error("Bucket "+bucket+": new demand "+demand+", total is "+
//  		 projectedDemandArray[bucket]);
  }

  // If have alloc results then use to remove from buckets
  // else use add method for removal
  // Make this a general method (finding the days)
  // findFirstBucket(projection)
  // findLastBucket(projection)  may be able to use same
  // methods for refill requisitions as well
  public void removeWithdrawTask(Task task) {
    int index;
    for (int i=0; i < dueOutList.size(); i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      list.remove(task);
    }
  }

  public void addRefillRequisition(Task task) {
    long endTime = PluginHelper.getEndTime(task);
    int bucket = convertTimeToBucket(endTime);
    while (bucket >= refillRequisitions.size()) {
      refillRequisitions.add(null);
    }
    refillRequisitions.set(bucket, task);
  }

  public void addRefillProjection(Task task) {
    long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
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
    // remove uncommitted refill tasks and refill
    // projections from the list.  Add all removed
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
    for (int i=0; i < refillProjections.size(); i++) {
      task = (Task)refillProjections.get(i);
      if ((task != null) && task.beforeCommitment(now)) {
	refillProjections.set(i, null);
	if (!taskList.contains(task)) {
	  taskList.add(task);
	}
      }
    }	
    return taskList;
  }

  public Schedule computeCriticalLevel() {
    // Need a generic way of thinking about buckets and
    // reconciling the buckets with the criticalLevel,
    // a number representing days
    return null;
  }

  public void updateRefillAllocation(Task task) {
    // need to update DueIn list
    // different updates obviously needed for Supply
    // and ProjectSupply because projections can span
    // serveral buckets.
    // This needs to be a well implemented method as it involves
    // shifting Refills and Projections in the dueIn list
  }

  public Schedule getProjectedDemand() {
    Schedule demand_schedule;
    Vector new_elements = new Vector();
    QuantityScheduleElement qse;
    long bucket_zero_time = convertBucketToTime(0);
    for (int i=0; i < projectedDemandArray.length; i++) {
      long start = bucket_zero_time+(MSEC_PER_BUCKET*i);
      long end = start + MSEC_PER_BUCKET;
      try {
	qse = ScheduleUtils.buildQuantityScheduleElement(projectedDemandArray[i], start, end);
      } catch (IllegalArgumentException iae) {
	iae.printStackTrace();
	continue;
      }
      new_elements.add(qse);
    }
    demand_schedule = GLMFactory.newQuantitySchedule(new_elements.elements(), "DemandSchedule");
    printQuantityScheduleTimes(demand_schedule);
    return demand_schedule;
  }

  private void logAllToCSVFile(long aCycleStamp) {
      if(csvLogger != null) {
	  csvWriter.logDemandToExcelOutput(withdrawList,projWithdrawList,aCycleStamp);
	  csvWriter.logResupplyToExcelOutput(supplyList,projSupplyList,aCycleStamp);
      }
  }

  public long getStartTime() {
    return startTime;
  }

  /**
   * Convert a time (long) into a bucket of this inventory that can be
   * used to index duein/out vectors, levels, etc.
   **/
  private int convertTimeToBucket(long time) {
    int thisDay = (int) (time / MSEC_PER_BUCKET);
    return thisDay - timeZero;
  }

  /**
   * Convert a bucket (int) into the start time of the bucket.
   * The end time of the bucket must be inferred from the 
   * next sequential bucket's start time (non-inclusive).
   **/
  private long convertBucketToTime(int bucket) {
    return (bucket + timeZero) * MSEC_PER_BUCKET;
  }

  private double[] expandArray(double[] doubleArray) {
    double biggerArray[] = new double[(int)(doubleArray.length*1.5)];
    for (int i=0; i < doubleArray.length; i++) {
      biggerArray[i] = doubleArray[i];
    }
    return biggerArray;
  }

  public void takeSnapshot() {
    // do double buffer
  }

  // If have alloc results then use to remove from buckets
  // else use add method for removal
  // Make this a general method (finding the days)
  // findFirstBucket(projection)
  // findLastBucket(projection)  may be able to use same
  // methods for refill requisitions as well
  private int findFirstBucket(Task task) {
    return 0;
  }

  private int findLastBucket(Task task) {
    return 0;
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


}
