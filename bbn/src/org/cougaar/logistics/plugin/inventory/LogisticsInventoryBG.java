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
  private long MSEC_PER_BUCKET = TimeUtils.MSEC_PER_DAY;
  private Duration durationArray[];
  protected LogisticsInventoryPG myPG;
  protected long startTime;
  protected int timeZero;
  private boolean initialized;
  private LoggingService logger;
  private LogisticsInventoryLogger csvLogger=null;
  private LogisticsInventoryFormatter csvWriter=null;
  // customerHash holds the time(long) of the last actual is seen
  // for each customer
  private HashMap customerHash;
  private long earliestDemand;
  private TaskUtils taskUtils;

  protected ArrayList dueInList;
  protected ArrayList dueOutList;
  // projectedDemandList mirrors dueOutList, each element
  // contains the sum of all Projected Demand for corresponding
  // bucket in dueOutList
  protected ArrayList projectedDemandList;
  // Lists for csv logging
  protected ArrayList projWithdrawList;
  protected ArrayList withdrawList;
  protected ArrayList projSupplyList;
  protected ArrayList supplyList;

  public LogisticsInventoryBG(LogisticsInventoryPG pg) {
    myPG = pg;
    initialized = false;
    customerHash = new HashMap();
    dueOutList = new ArrayList();
    dueInList = new ArrayList();
    projectedDemandList = new ArrayList();
    durationArray = new Duration[15];
    for (int i=0; i <= 14; i++) {
      durationArray[i] = Duration.newDays(0.0+i);
    }
    projWithdrawList = new ArrayList();
    withdrawList = new ArrayList();
    projSupplyList = new ArrayList();
    supplyList = new ArrayList();
  }

  public void initialize(long today, InventoryPlugin parentPlugin) {
    // It would be nice to make this part of the constructor but I don't
    // know of a way.  If the BG is not initialized then we can't even
    // log an error so I added the initialized boolean but I don't care
    // for it.
    startTime = today;
    timeZero = (int)(startTime/MSEC_PER_BUCKET);
    logger = parentPlugin.getLoggingService(this);
    if(false) {
	csvLogger = LogisticsInventoryLogger.createInventoryLogger(myPG.getResource(),parentPlugin.getMyOrganization(),parentPlugin);
	csvWriter = new LogisticsInventoryFormatter(csvLogger, parentPlugin);
    }
    taskUtils = parentPlugin.getTaskUtils();
    logger.debug("Start day: "+TimeUtils.dateString(today));
    initialized = true;
  }

  public void addWithdrawTask(Task task) {
    if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
      addDueOut(task);
      withdrawList.add(task);
      earliestDemand = Math.min(earliestDemand, PluginHelper.getEndTime(task));
    } else if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
      addDueOutProjection(task);
      projWithdrawList.add(task);
      earliestDemand = Math.min(earliestDemand, PluginHelper.getStartTime(task));
    }
  }

  private void addDueOut(Task task) {
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

  private void addDueOutProjection(Task task) {
    long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
    int bucket_start = convertTimeToBucket(start);
    int bucket_end = convertTimeToBucket(end);
    logger.debug("\n"+taskUtils.taskDesc(task));
    logger.debug("start: "+TimeUtils.dateString(start));
    logger.debug("end  : "+TimeUtils.dateString(end));
    while (bucket_end >= dueOutList.size()) {
      dueOutList.add(new ArrayList());
      projectedDemandList.add(new Double(0.0));
    }
    for (int i=bucket_start; i < bucket_end; i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      list.add(task);
      updateProjectedDemandList(task, i, start, end);
    }
  }

  private void updateProjectedDemandList(Task task, int bucket,
				      long start, long end) {
    long bucket_start = convertBucketToTime(bucket);
    long bucket_end = convertBucketToTime(bucket+1);
    long interval_start = Math.max(start, bucket_start);
    long interval_end = Math.min(end, bucket_end);
    int days_spanned = (int)((interval_end - interval_start)/TimeUtils.MSEC_PER_DAY);
    Rate rate = taskUtils.getRate(task);
    Scalar scalar = (Scalar)rate.computeNumerator(durationArray[days_spanned]);
    double demand = taskUtils.getDouble(scalar);
    Double prev_demand = (Double)projectedDemandList.get(bucket);
    projectedDemandList.add(bucket, new Double(prev_demand.doubleValue()+demand));
    logger.debug("Bucket "+bucket+": previous demand is "+prev_demand
		 +", new demand "+demand+", total is "+
		 (Double)projectedDemandList.get(bucket));
  }

  public void removeWithdrawTask(Task task) {
    int index;
    for (int i=0; i < dueOutList.size(); i++) {
      ArrayList list = (ArrayList)dueOutList.get(i);
      while ((index = list.indexOf(task)) != -1) {
	list.remove(index);
      }
    }
    if (task.getVerb().equals(Constants.Verb.SUPPLY)) {
      if ((index = withdrawList.indexOf(task)) != -1) {
	withdrawList.remove(index);
      }
    } else if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
      while ((index = projWithdrawList.indexOf(task)) != -1) {
	projWithdrawList.remove(index);
      }
    }
  }

  public void addRefillTask(Task task) {
    if (task.getVerb().equals(Constants.Verb.SUPPLY)) {
      addDueIn(task);
      supplyList.add(task);
    } else if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
      addDueInProjection(task);
      projSupplyList.add(task);
    }
  }

  private void addDueIn(Task task) {
    long endTime = PluginHelper.getEndTime(task);
    int bucket = convertTimeToBucket(endTime);
    while (bucket >= dueInList.size()) {
      dueInList.add(new ArrayList());
    }
    ArrayList list = (ArrayList)dueInList.get(bucket);
    list.add(task);
  }

  private void addDueInProjection(Task task) {
    long start = (long)PluginHelper.getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long)PluginHelper.getPreferenceBestValue(task, AspectType.END_TIME);
    int bucket_start = convertTimeToBucket(start);
    int bucket_end = convertTimeToBucket(end);
    while (bucket_end >= dueInList.size()) {
      dueInList.add(new ArrayList());
    }
    for (int i=bucket_start; i < bucket_end; i++) {
      ArrayList list = (ArrayList)dueInList.get(i);
      list.add(task);
    }
  }

  // Might want to refactor and combine with removeWithdrawTask()
  public void removeRefillTask(Task task) {
    int index;
    for (int i=0; i < dueInList.size(); i++) {
      ArrayList list = (ArrayList)dueInList.get(i);
      while ((index = list.indexOf(task)) != -1) {
	list.remove(index);
      }
    }
    if (task.getVerb().equals(Constants.Verb.SUPPLY)) {
      if ((index = supplyList.indexOf(task)) != -1) {
	supplyList.remove(index);
      }
    } else if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
      while ((index = projSupplyList.indexOf(task)) != -1) {
	projSupplyList.remove(index);
      }
    }
  }

  public List clearRefillTasks(Date now) {
    // remove uncommitted refill tasks and refill
    // projections from the list.  Add all removed
    // tasks to a second list that will be returned
    // for comparison
    Task task;
    ArrayList taskList = new ArrayList();
    for (int i=0; i < dueInList.size(); i++) {
      ArrayList list = (ArrayList)dueInList.get(i);
      for (int j=0; j < list.size(); j++) {
	task = (Task)list.get(j);
	if (task.beforeCommitment(now)) {
	  taskList.add(task);
	  list.remove(j);
	  int index;
	  if ((index = supplyList.indexOf(task)) != -1) {
	    supplyList.remove(index);
	  }
	  if ((index = projSupplyList.indexOf(task)) != -1) {
	    projSupplyList.remove(index);
	  }
	}
      }
    }
    return taskList;
  }

  public Schedule getCriticalLevel() {
    
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

  public void logAllToCSVFile(long aCycleStamp) {
      if(csvLogger != null) {
	  logDueOutsToCSVFile(aCycleStamp);
      }
  }

  private void logDueOutsToCSVFile(long aCycleStamp) {
      if(csvLogger != null) {
	  csvWriter.logToExcelOutput(dueOutList,aCycleStamp);
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

  // Ask Beth about persistance.  Would like to make sure structures
  // are persisted when they are added to the code
  public PGDelegate copy(PropertyGroup pg) {
    return new LogisticsInventoryBG((LogisticsInventoryPG)pg);
  }

}


