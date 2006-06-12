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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleElementType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.plan.QuantityScheduleElementImpl;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.logistics.plugin.packer.GenericPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.MoreMath;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.log.Logger;

import java.io.Serializable;
import java.util.*;


/** Provides convenience methods. */
public class TaskUtils extends PluginHelper implements Serializable { // revisit making Serializable later...

  private transient Logger logger;
  private transient UtilsProvider utilProvider;
  private transient AssetUtils assetUtils;

  public TaskUtils(UtilsProvider provider) {
    super();
    utilProvider = provider;
    logger = (Logger)utilProvider.getLoggingService(this);
    assetUtils = utilProvider.getAssetUtils();
  }



  public TaskUtils(Logger aLogger) {
    super();
    utilProvider = null;
    logger = aLogger;
    assetUtils = new AssetUtils(aLogger);
  }
  /** @param task the task
   *  @return true if the task has an packer INTERNAL prep phrase */
  public static boolean isInternal(Task task) {
    PrepositionalPhrase pp = task.getPrepositionalPhrase(GenericPlugin.INTERNAL);
    if (pp == null) {
      return false;
    }
    return true;
  }


  /** @param t the task
   *  @param type type identification string
   *  @return true if the task's OFTYPE preposition's indirect object is
   *  an asset with nomeclature equal to 'type'.*/
  public static boolean isTaskOfType(Task t, String type) {
    PrepositionalPhrase pp =t.getPrepositionalPhrase(Constants.Preposition.OFTYPE) ;
    if (pp != null) {
      Object obj = pp.getIndirectObject();
      if (obj instanceof Asset) {
        Asset a = (Asset)obj;
        return a.getTypeIdentificationPG().getTypeIdentification().equals(type);
      }
    }
    return false;
  }

  /** @param t the task
   *  @param type type identification string
   *  @return true if the task's OFTYPE preposition's indirect object is
   *  an string with nomeclature equal to 'type'.*/
  public static boolean isTaskOfTypeString(Task t, String type) {
    PrepositionalPhrase pp =t.getPrepositionalPhrase(Constants.Preposition.OFTYPE) ;
    if (pp != null) {
      Object obj = pp.getIndirectObject();
      if (obj instanceof String) {
        return ((String)obj).equals(type);
      }
    }
    return false;
  }

  public boolean isDirectObjectOfType(Task t, String type) {
    boolean result = false;
    Asset asset = t.getDirectObject();
    // Check for aggregate assets and grab the prototype
    if (asset instanceof AggregateAsset) {
      asset = ((AggregateAsset)asset).getAsset();
    }
    try {
      SupplyClassPG pg = (SupplyClassPG)asset.searchForPropertyGroup(SupplyClassPG.class);
      if (pg != null) {
        result = type.equals(pg.getSupplyType());
        if((result == true) && (type.equals("PackagedPOL")) &&
            asset.getTypeIdentificationPG().getTypeIdentification().endsWith("Aggregate")) {
          logger.debug("\n direct object type... type for plugin is: " +
              type + "]" + " type for DO is: [" + pg.getSupplyType() + "]");
        }

      }
      else {
        logger.debug("No SupplyClassPG found on asset "+ this.taskDesc(t));
      }
    } catch (Exception e) {
      logger.error("Tasks DO is null "+ this.taskDesc(t)+"\n"+e);
    }
    return result;
  }

  // utility functions
  public String taskDesc(Task task) {
    if (isProjection(task)) {
      return task.getUID() + ": "
        + task.getVerb()+"("+
        getDailyQuantity(task, getStartTime(task))+" "+
        getTaskItemName(task)+") "+
        getTimeUtils().
        dateString(new Date(getStartTime(task)))+
        "  -  " +
        getTimeUtils().
        dateString(new Date(getEndTime(task)));
    } else {
      return task.getUID() + ": "
        + task.getVerb()+"("+
        getQuantity(task)+" "+
        getTaskItemName(task)+") "+
        getTimeUtils().
        dateString(new Date(getEndTime(task)));
    }
  }


  public String getTaskItemName(Task task){
    Asset prototype = (Asset)task.getDirectObject();
    if (prototype == null) return "null";
    return assetUtils.assetDesc(prototype);
  }

  public static boolean isMyRefillTask(Task task, String myOrgName) {
    PrepositionalPhrase pp =task.getPrepositionalPhrase(Constants.Preposition.REFILL);
    if (pp == null) {
      return false;
    }
    pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
      String orgName = (String)io;
      if ( orgName.equals(myOrgName)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isMyNonRefillTask(Task task, String myOrgName) {
    PrepositionalPhrase pp =task.getPrepositionalPhrase(Constants.Preposition.REFILL);
    if (pp != null) {
      return false;
    }
    pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
      String orgName = (String)io;
      if ( orgName.equals(myOrgName)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isMyInventoryProjection(Task task, String myOrgName) {
    PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
      String orgName = (String)io;
      if ( orgName.equals(myOrgName)) {
        pp = task.getPrepositionalPhrase(Constants.Preposition.MAINTAINING);
        if (pp != null) {
          try {
            if (((MaintainedItem)pp.getIndirectObject()).getMaintainedItemType().equals("Inventory")) {
              return true;
            }
          } catch (ClassCastException exc) {
            return false;
          }
        }
      }
    }
    return false;
  }


  public boolean isReadyForTransport(Task task){
    PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.READYFORTRANSPORT);
    return (pp != null);
  }

  public boolean isMyDemandForecastProjection(Task task,String orgName) {
    PrepositionalPhrase pp = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    if (pp == null) {
      return false;
    }
    Object io = pp.getIndirectObject();
    if (io instanceof String) {
      String taskOrg = (String) io;
      if(taskOrg.equals(orgName)) {
        pp = task.getPrepositionalPhrase(Constants.Preposition.REFILL);
        return (pp == null);
      }
    }
    return false;
  }



  /** return the preference of the given aspect type.  Returns null if
   *  the task does not have the given aspect. */
  public static double getPreference(Task t, int aspect_type) {
    Preference p = t.getPreference(aspect_type);
    if (p == null) return Double.NaN;

    AspectScorePoint asp = p.getScoringFunction().getBest();
    return asp.getValue();
  }


  public static boolean isProjection(Task t) {
    return 
      (t.getPrepositionalPhrase(Constants.Preposition.DEMANDRATE) != null ||
       t.getPreference(AlpineAspectType.DEMANDRATE) != null);
  }

  public static boolean isSupply(Task t) {
    return !isProjection(t);
  }

  public boolean isLevel2(Task t) {
    return assetUtils.isLevel2Asset(t.getDirectObject());
  }


  // TASK PREFERENCE UTILS

  public static double getQuantity(Task task) {
    return getPreferenceBestValue(task, AspectType.QUANTITY);
  }

  public static Preference createDemandRatePreference(PlanningFactory rf, Rate rate) {
    ScoringFunction sf = ScoringFunction
      .createStrictlyAtValue(AspectValue.newAspectValue(AlpineAspectType.DEMANDRATE,
            rate));
    return rf.newPreference(AlpineAspectType.DEMANDRATE, sf);
  }

  /**
   * Compare the preferences of two tasks return true if the tasks
   * have preferences for the same aspect types and if all
   * corresponding AspectValues are nearly equal.
   * This needs to be fixed to be more efficient.
   **/
  private boolean comparePreferences(Task a, Task b) {
    return comparePreferencesInner(a, b) && comparePreferencesInner(b, a);
  }

  private boolean comparePreferencesInner(Task a, Task b) {
    Enumeration ae = a.getPreferences();
    while (ae.hasMoreElements()) {
      Preference p = (Preference) ae.nextElement();
      int at = p.getAspectType();
      double av = p.getScoringFunction().getBest().getValue();
      double bv = getPreferenceBestValue(b, at);
      if (at == AspectType.START_TIME || at == AspectType.END_TIME) {
        //for times say they are nearly equal if they are within the same hour
        // it could be longer if we only had daily buckets - but this should
        // work for both daily and hourly buckets
        long aHourLong = (long) av/3600000;
        long bHourLong = (long) bv/3600000;
        if (aHourLong != bHourLong) return false;
      }  else {
        if (!MoreMath.nearlyEquals(av, bv, 0.0001)) return false;
      }
    }
    return true;
  }

  /** @param task
   *  @return Value of the FOR Preposition if available, else null
   */
  public static Object getCustomer(Task task) {
    PrepositionalPhrase pp_for = task.getPrepositionalPhrase(Constants.Preposition.FOR);
    Object org;
    if (pp_for != null) {
      org = pp_for.getIndirectObject();
      return org;
    }
    return null;
  }

  public boolean isFlowRate(Task task) {
    // select any rate in the rate_schedule, e.g. the first:
    Rate r = getRate(task);
    return (r instanceof FlowRate);
  }

  public Rate getRate(Task task) {
    return getRate(task, getStartTime(task));
  }

  public Rate getRate(Task task, long time) {
    return getRate(task, time, time);
  }

  public Rate getRate(Task task, long start, long end) {
    // look for time-phased rate schedule
    PrepositionalPhrase pp_rate = task.getPrepositionalPhrase(Constants.Preposition.DEMANDRATE);
    if (pp_rate != null) {
      Object indObj = pp_rate.getIndirectObject();
      if (indObj instanceof Schedule) {
        Schedule sched = (Schedule) indObj;
        Collection rate_elems = 
          (start == end ?
           sched.getScheduleElementsWithTime(start) :
           sched.getOverlappingScheduleElements(start, end));
        int n = (rate_elems == null ? 0 : rate_elems.size());
        if (n > 0) {
          if (n == 1 || (start >= end)) {
            // return the single matching rate
            ObjectScheduleElement ose = (ObjectScheduleElement) 
              rate_elems.iterator().next();
            return (Rate) ose.getObject();
          }
          // compute the average rate for this timespan
          Rate avgRate = null;
          long prevStart = start;
          Rate firstRate = null;
          double totalQty = 0.0;
          for (Iterator iter = rate_elems.iterator(); iter.hasNext(); ) {
            ObjectScheduleElement ose = (ObjectScheduleElement) iter.next();
            long st = Math.max(prevStart, ose.getStartTime());
            long et = Math.min(ose.getEndTime(), end);
            long time_spanned = (et - st);
            if (time_spanned <= 0) {
              continue;
            }
            prevStart = et;
            Rate r = (Rate) ose.getObject();
            if (firstRate == null) {
              firstRate = r;
            }
            double dailyRate = getDailyQuantity(r);
            if (Double.isNaN(dailyRate)) {
              continue;
            }
            double qty = (dailyRate * ((double) time_spanned/TimeUtils.MSEC_PER_DAY));
            totalQty += qty;
          }
          Duration dur = new Duration((end - start), Duration.MILLISECONDS);
          if (firstRate instanceof FlowRate) {
            Volume vol = new Volume(totalQty, Volume.GALLONS);
            avgRate = new FlowRate(vol, dur);
          } else if (firstRate instanceof CountRate) {
            Count cnt = new Count(totalQty, Count.EACHES);
            avgRate = new CountRate(cnt, dur);
          } else if (firstRate instanceof MassTransferRate) {
            Mass mass = new Mass(totalQty, Mass.SHORT_TONS);
            avgRate = new MassTransferRate(mass, dur);
          } else {
            avgRate = null;
          }
          return avgRate;
        }
      }
    }
    // look for preference
    AspectValue best = getPreferenceBest(task, AlpineAspectType.DEMANDRATE);
    if (best != null) {
      return ((AspectRate) best).getRateValue();
    }
    // not a projection?
    return null;
  }

  public double getDailyQuantity(Task task) {
    return getDailyQuantity(task, getEndTime(task));
  }

  public double getDailyQuantity(Task task, long time) {
    return getDailyQuantity(task, time, time);
  }

  public double getDailyQuantity(Task task, long start, long end) {
    if (isProjection(task)) {
      return getDailyQuantity(getRate(task, start, end));
    } else {
      return getQuantity(task);
    }
  }

  public static double getDailyQuantity(Rate r) {
    if (r instanceof FlowRate) {
      return ((FlowRate) r).getGallonsPerDay();
    } else if (r instanceof CountRate) {
      return ((CountRate) r).getEachesPerDay();
    } else if (r instanceof MassTransferRate) {
      return ((MassTransferRate) r).getShortTonsPerDay();
    } else {
      return Double.NaN;
    }
  }

  /**
   * Given a Scalar, return a double value representing
   * Gallons for Volume,
   * Eaches for Count and
   * Short Tons for Mass.
   **/
  public double getDouble(Scalar measure) {
    double result = Double.NaN;
    if (measure instanceof Volume) {
      result = ((Volume)measure).getGallons();
    } else if (measure instanceof Count) {
      result = ((Count)measure).getEaches();
    } else if (measure instanceof Mass) {
      result = ((Mass)measure).getShortTons();
    } else {
      logger.error("InventoryBG.getDouble(), Inventory cannot determine type of measure");
    }
    return result;
  }

  public static double getQuantity(AllocationResult ar) {
    return getARAspectValue(ar, AspectType.QUANTITY);
  }

  // Hand in the demandRate from a phase of particular allocation result
  // and its parent task.  This function basically handles the
  // contained demand rate result and returns the corresponding
  // daily rate.    If its fuel (FlowRate) that's already gallons
  // per day, otherwise its eaches per millisecond and should be
  // multiplied correspondingly.
  public double convertResultsToDailyRate(Task task, double demandRate) {
    if (isProjection(task) && !isFlowRate(task)) {
      return demandRate * TimeUtils.SEC_PER_DAY;
    }
    return demandRate;
  }

  public double getQuantity(Task task, AllocationResult ar) {
    if(isProjection(task)) {
      // 	  logger.warn("TaskUtils::getting qty from projection!");
      return convertResultsToDailyRate(task,
          getARAspectValue(ar, AlpineAspectType.DEMANDRATE));
    }
    else {
      return getQuantity(ar);
    }
  }

  public double getQuantity(Task task, AllocationResult ar, long time_spanned) {
    if (isProjection(task)) {
      Rate rate = getARAspectRate(ar);
      Duration d = Duration.newMilliseconds((double)time_spanned);
      Scalar scalar = (Scalar)rate.computeNumerator(d);
      return getDouble(scalar);
    } else {
      return getQuantity(ar);
    }
  }

  public double getTotalQuantity(Task task) {
    return getTotalQuantity(task, getStartTime(task), getEndTime(task));   
  }

  public double getTotalQuantity(Task task, long startTime, long endTime) {
    if (isProjection(task)) {
      double time_spanned = endTime - startTime;
      if (time_spanned > 0) {
        Rate rate = getRate(task, startTime, endTime);
        Duration d = Duration.newMilliseconds((double) time_spanned);
        Scalar scalar = (Scalar) rate.computeNumerator(d);
        return getDouble(scalar);
      }
      else {
        return 0.0d;
      }
    } else {
      return getQuantity(task);
    }
  }

  public double getTotalQuantity(Task task, double demandRate, long startTime, long endTime) {
    if (isProjection(task)) {
      double time_spanned = endTime - startTime;
      if (time_spanned > 0) {
        double dailyRate = convertResultsToDailyRate(task, demandRate);
        return (dailyRate * (time_spanned/TimeUtils.MSEC_PER_DAY));
      } else {
        return 0.0d;
      }
    } else {
      return getQuantity(task);
    }
  }

  public static Rate getARAspectRate(AllocationResult ar) {
    if (ar == null) return null;
    AspectValue[] avs = ar.getAspectValueResults();
    for (int ii = 0; ii < avs.length; ii++) {
      if (avs[ii].getAspectType() == AlpineAspectType.DEMANDRATE) {
        return ((AspectRate)avs[ii]).getRateValue();
      }
    }
    return null;
  }

  public TimeUtils getTimeUtils() {return utilProvider.getTimeUtils();}

  public static Collection getUnallocatedTasks(Collection tasks, Verb verb) {
    Iterator taskIt = tasks.iterator();
    ArrayList list = new ArrayList();
    Task task;
    while (taskIt.hasNext()) {
      task = (Task)taskIt.next();
      if ((task.getPlanElement() == null) && (task.getVerb().equals(verb))){
        list.add(task);
      }
    }
    return list;
  }

  public Schedule newObjectSchedule(Collection tasks) {
    Vector os_elements = new Vector();
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(PlanScheduleElementType.OBJECT);
    s.setScheduleType(ScheduleType.OTHER);

    for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
      Task task = (Task)iterator.next();
      try {
        os_elements.add(new ObjectScheduleElement(getStartTime(task),
              getEndTime(task), task));
      } catch (IllegalArgumentException iae) {
        if (logger.isErrorEnabled()) {
          logger.error("newObjectSchedule failed, start and end time is " + new Date(getStartTime(task)) +
              " for task " + task + "\n" + iae.getMessage());
        }
      }
    }
    s.setScheduleElements(os_elements.elements());
    return s;
  }



  /** Change the prev task's preferences to the new tasks preferences if they are different.
   * @param prev_task previously published task.
   * @param new_task already defined to have the same taskKey as task a.
   * @return null if the two tasks are the same,
   *         or returns task a modified for a publishChange.
   */
  public Task changeTask(Task prev_task, Task new_task) {
    // Checks for changed preferences.
    if(prev_task==new_task) {
      //return new_task;
      return null;
    }
    if (!comparePreferences(new_task, prev_task)) {
      synchronized ( new_task ) {
        Enumeration ntPrefs = new_task.getPreferences();
        ((NewTask)prev_task).setPreferences(ntPrefs);
      } // synch
      return prev_task;
    }
    return null;
  }

  // Time Preference Utils

  /** Create a Time Preference for a Refill Task or a Demand Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param early The earliest time this preference can have
   *  @param end The last time this preference can have (such as Oplan end time)
   *  @param aspectType The AspectType of the preference- should be start_time or end_time
   *  @param clusterId Agent cluster ID
   *  @param planningFactory Planning factory from plugin
   *  @param thePG InventoryPG for the item maintained by a refill task
   *  @return Preference The new Time Preference
   **/
  public Preference createTimePreference(long bestDay, long early, long end, int aspectType,
      MessageAddress clusterId, PlanningFactory planningFactory,
      LogisticsInventoryPG thePG) {
    double daysBetween;
    long late;
    if (thePG !=null) {
      daysBetween = ((end - bestDay)  / thePG.getBucketMillis()) - 1;
      late = bestDay +  thePG.getBucketMillis();
    } else {
      late = getTimeUtils().addNDays(bestDay, 1);
      daysBetween = ((end - bestDay) / 86400000);
    }

    // Negative value here is bad. Note that end==bestDay is OK. This case
    // is handled below, where we skip adding the end AspectScorePoint
    if (daysBetween < 0.0) {
      if (logger.isWarnEnabled())
        logger.warn(clusterId + ".createTimePref had OplanEnd < bestDay! OplanEnd: " + new Date(end) + ". Best: " + new Date(bestDay));
    }

    //Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
    // define alpha .25
    double alpha = .25;

    Vector points = new Vector();
    AspectScorePoint earliest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, early), alpha);
    AspectScorePoint best = new AspectScorePoint(AspectValue.newAspectValue(aspectType, bestDay), 0.0);
    AspectScorePoint first_late = new AspectScorePoint(AspectValue.newAspectValue(aspectType, late), alpha);
    AspectScorePoint latest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, end), (alpha + late_score));

    // Don't add the early point if best is same time or earlier
    if (bestDay > early) {
      points.addElement(earliest);
    } else if (bestDay == early) {
      if (logger.isInfoEnabled()) {
        logger.info(clusterId + ".createTimePref skipping early point: best == early (OplanStart)! bestDay: " + new Date(bestDay) + ". AspectType: " + aspectType);
      }
    } else {
      if (logger.isWarnEnabled()) {
        logger.warn(clusterId + ".createTimePref skipping early point: best < early (OplanStart)! bestDay: " + new Date(bestDay) + ", early: " + new Date(early) + ". AspectType: " + aspectType);
      }
    }

    points.addElement(best);
    points.addElement(first_late);

    // Only add the "late" point if it's value is later than first_late
    if (end > late) {
      points.addElement(latest);
    } else if (logger.isInfoEnabled()) {
      // Note that this case is equivalent to any daysBetween value <= 1.0,
      // including the case above where daysBetween < 0.0

      // If bestDay == end, this is almost certainly an end Preference, where the preference
      // is OplanEnd. So best+1 is necessarily > end
      // check aspectType == AspectType.END_TIME
      logger.info(clusterId + ".createTimePref skipping end point: end <= late! end: " + new Date(end) + ", late: " + new Date(late) + ((bestDay==end && aspectType == AspectType.END_TIME) ? ". A Task EndPref where best==OplanEnd." : ". AspectType: " + aspectType));
    }

    ScoringFunction timeSF = ScoringFunction.createPiecewiseLinearScoringFunction(points.elements());
    return planningFactory.newPreference(aspectType, timeSF);
  }

  /** copies a Supply task from another to split it**/
  public NewTask copySupplyTask(Task origTask, long start, long end,
      LogisticsInventoryPG invPG,
      InventoryManager inventoryPlugin) {

    NewTask task = inventoryPlugin.getPlanningFactory().newTask();
    task.setVerb( origTask.getVerb());
    task.setDirectObject( origTask.getDirectObject());
    task.setParentTaskUID( origTask.getParentTaskUID() );
    task.setContext( origTask.getContext() );
    task.setPlan( origTask.getPlan() );
    Enumeration pp = origTask.getPrepositionalPhrases();
    Vector ppv = new Vector();
    while(pp.hasMoreElements()) {
      ppv.addElement(pp.nextElement());
    }
    NewPrepositionalPhrase newPP = inventoryPlugin.getPlanningFactory().newPrepositionalPhrase();
    newPP.setPreposition("SplitTask");
    ppv.add(newPP);
    task.setPrepositionalPhrases(ppv.elements());
    task.setPriority(origTask.getPriority());
    task.setSource(inventoryPlugin.getClusterId() );
    changeDatePrefs(task, start, end, inventoryPlugin, invPG);
    // TODO: is this ok to to just add these prefs to the a vector?
    // TODO: the changeDatePrefs clones the scoring function
    Enumeration origPrefs = task.getPreferences();
    Vector newPrefs = new Vector();
    Preference currentPref;
    while (origPrefs.hasMoreElements()) {
      currentPref = (Preference) origPrefs.nextElement();
      newPrefs.add(currentPref);
    }
    // old pref-style rate w/o rate_schedule
    AspectValue rate_best = getPreferenceBest(task, AlpineAspectType.DEMANDRATE);
    if (rate_best != null) {
      Rate rate = ((AspectRate) rate_best).getRateValue();
      if (rate != null) {
        newPrefs.addElement(createDemandRatePreference(inventoryPlugin.getPlanningFactory(), rate));
      }
    }
    // start and end from schedule element

    task.setPreferences(newPrefs.elements());
    return task;
  }

  public void changeDatePrefs(NewTask task, long start, long end,
      InventoryManager inventoryPlugin, LogisticsInventoryPG invPG) {
    Preference startPref = createTimePreference(start, inventoryPlugin.getOPlanArrivalInTheaterTime(),
        inventoryPlugin.getOPlanEndTime(),
        AspectType.START_TIME, inventoryPlugin.getClusterId(),
        inventoryPlugin.getPlanningFactory(), invPG);
    Preference endPref = createTimePreference(end, inventoryPlugin.getOPlanArrivalInTheaterTime(),
        inventoryPlugin.getOPlanEndTime(),
        AspectType.END_TIME, inventoryPlugin.getClusterId(),
        inventoryPlugin.getPlanningFactory(), invPG);

    Enumeration origPrefs = task.getPreferences();
    Preference currentPref, copiedPref;
    Vector newPrefs = new Vector();
    while (origPrefs.hasMoreElements()) {
      currentPref = (Preference) origPrefs.nextElement();
      if (! (currentPref.getAspectType() == AspectType.START_TIME ||
            currentPref.getAspectType() == AspectType.END_TIME)) {
        copiedPref = inventoryPlugin.getPlanningFactory().
          newPreference(currentPref.getAspectType(),
              (ScoringFunction)currentPref.getScoringFunction().clone());
        newPrefs.add(copiedPref);
      }
    }
    newPrefs.add(startPref);
    newPrefs.add(endPref);
    synchronized (task) {
      task.setPreferences(newPrefs.elements());
    }
  }

  public Collection splitProjection(Task task, List howToSplit,
      InventoryManager invPlugin) {
    ArrayList newSplitTasks = new ArrayList();
    Asset asset = task.getDirectObject();
    Inventory inventory = invPlugin.findOrMakeInventory(asset);
    LogisticsInventoryPG invPG = (LogisticsInventoryPG)
      inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
    //remove the orig task from the BG - we'll re-add after the split
    invPG.removeRefillProjection(task);
    Iterator tsIt = howToSplit.iterator();
    TimeSpan first = (TimeSpan) tsIt.next();
    //first element from howToSplit is the one to reuse the allocation
    long startOfSplit = first.getStartTime();
    long endOfSplit = first.getEndTime();
    TimeSpan second = (TimeSpan) tsIt.next();
    long startOfSecondSplit = second.getStartTime();
    long endOfSecondSplit = second.getEndTime();
    if (task.getPlanElement() != null) {
      changeDatePrefs((NewTask)task, startOfSplit, endOfSplit, invPlugin, invPG);
      invPG.addRefillProjection(task);
      invPlugin.publishChange(task);
      //add the changed list to newSplitTasks to be sent to the ExtAlloc so it can update its estimated result
      newSplitTasks.add(task);
      newSplitTasks.add(makeNewSplit(task, startOfSecondSplit, endOfSecondSplit, invPG, invPlugin, inventory));
    } else {
      newSplitTasks.add(makeNewSplit(task, startOfSplit, endOfSplit, invPG, invPlugin, inventory));
      newSplitTasks.add(makeNewSplit(task, startOfSecondSplit, endOfSecondSplit, invPG, invPlugin, inventory));
    }
    return newSplitTasks;
  }

  private Task makeNewSplit(Task task, long startOfSplit, long endOfSplit, LogisticsInventoryPG invPG,
      InventoryManager invPlugin, Inventory inventory) {
    Task newSplitTask = copySupplyTask(task, startOfSplit, endOfSplit, invPG, invPlugin);
    if (logger.isDebugEnabled()) {
      logger.debug("Made a new split task with dates of: "+ new Date(startOfSplit) + "..." + new Date(endOfSplit));
    }
    invPG.addRefillProjection(newSplitTask);
    //hookup newSplitTask
    invPlugin.publishRefillTask(newSplitTask, inventory);
    return newSplitTask;
  }

  public long getReportedStartTime(Task task) {
    PlanElement pe = task.getPlanElement();
    // If the task has no plan element then return the StartTime Pref
    if (pe == null) {
      return getStartTime(task);
    }
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    } else if (!ar.isSuccess()) {
      return getStartTime(task);
    }
    return (long) getStartTime(ar);
  }

  public long getReportedEndTime(Task task) {
    PlanElement pe = task.getPlanElement();
    // If the task has no plan element then return the EndTime Pref
    if (pe == null) {
      return getEndTime(task);
    }
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    }
    // make sure that we got atleast a valid reported OR estimated allocation result
    if (ar != null) {
      if (!ar.isSuccess()) {
        getEndTime(task); // bug?
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
      return getEndTime(task);
    }
  }

  public double calculateDemand(Task t, long minStart, long maxEnd) {
    if (isProjection(t)) {
      long start = Math.max(getReportedStartTime(t), minStart);
      long end = Math.min(getReportedEndTime(t), maxEnd);
      return getTotalQuantity(t, start, end);
    }
    long taskEnd = getEndTime(t);
    if ((taskEnd < minStart) || (taskEnd > maxEnd)) {
      return 0.0d;
    }
    return getQuantity(t);
  }

  public double calculateFilled(Task t, long minStart, long maxEnd) {
    PlanElement pe = t.getPlanElement();
    AllocationResult ar = null;
    if (pe != null) {
      ar = pe.getReportedResult();
      if (ar == null) {
        ar = pe.getEstimatedResult();
      }
    }
    if (isProjection(t)) {
      long start = Math.max(getReportedStartTime(t), minStart);
      long end = Math.min(getReportedEndTime(t), maxEnd);
      if (ar != null && !ar.isSuccess()) {
        return 0.0d;
      }
      double taskQty = getTotalQuantity(t, start, end);
      if (ar == null || !ar.isPhased()) {
        return taskQty;
      }

      int[] ats = ar.getAspectTypes();
      int rateInd = LogisticsInventoryFormatter.getIndexForType(ats, AlpineAspectType.DEMANDRATE);
      int startInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.START_TIME);
      int endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);

      double totalQty=0;
      for (Enumeration phasedResults = ar.getPhasedResults();
          phasedResults.hasMoreElements();
          ) {
        double[] results = (double[]) phasedResults.nextElement();
        double phaseRate = results[rateInd];
        double phaseStart = results[startInd];
        double phaseEnd = results[endInd];
        start = Math.max((long) phaseStart, minStart);
        end = Math.min((long) phaseEnd, maxEnd);
        totalQty += getTotalQuantity(t, phaseRate, start, end);
      }
      return totalQty;
    }
    long taskEnd = getEndTime(t);
    if ((taskEnd < minStart) || (taskEnd > maxEnd)) {
      return 0.0d;
    }

    if (ar == null) {
      return getQuantity(t);
    }
    if (!ar.isPhased()) {
      if (ar.isSuccess()) {
        double arEnd = getEndTime(ar);
        double taskQty = getQuantity(t, ar);
        if ((arEnd >= minStart) && (arEnd <= maxEnd)) {
          return taskQty;
        }
      }
      return 0.0d;
    }

    int[] ats = ar.getAspectTypes();
    int qtyInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.QUANTITY);
    int endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);

    double totalQty=0;
    for (Enumeration phasedResults = ar.getPhasedResults(); phasedResults.hasMoreElements();) {
      double[] results = (double[]) phasedResults.nextElement();
      double phaseQty = results[qtyInd];
      double phaseEnd = results[endInd];
      if (phaseEnd <= maxEnd) {
        totalQty += phaseQty;
      }
    }
    return totalQty;
  }

  public boolean isProjFailure(Task t, long minStart, boolean includeTemps) {
    PlanElement pe = t.getPlanElement();
    if (pe == null) {
      return false;
    }
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    }
    if (ar == null) {
      return false;
    }
    if (ar.isSuccess() && (!includeTemps || !ar.isPhased())) {
      return false;
    }
    int[] ats = ar.getAspectTypes();
    int rateInd = LogisticsInventoryFormatter.getIndexForType(ats, AlpineAspectType.DEMANDRATE);
    if (rateInd < 0) {
      // no rate response, assume success?
      return false;
    }

    long start = 
      Math.max(
          getReportedStartTime(t),
          minStart);
    long end = getReportedEndTime(t);
    double taskTotal = getTotalQuantity(t, start, end);
    if ((!ar.isSuccess()) && (taskTotal > 0)) {
      return true;
    }
    if (!includeTemps || !ar.isPhased()) {
      return false;
    }

    int startInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.START_TIME);
    int endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);

    double totalQty = 0;
    long maxPhaseEnd = 0;

    for (Enumeration phasedResults = ar.getPhasedResults();
        phasedResults.hasMoreElements();
        ) {
      double[] results = (double[]) phasedResults.nextElement();
      double phaseRate = results[rateInd];
      double phaseStart = results[startInd];
      double phaseEnd = results[endInd];
      long arStart = Math.max((long) phaseStart, start);
      //long arEnd = (long) Math.min(end, (long) phaseEnd);
      totalQty += getTotalQuantity(t, phaseRate, arStart, (long) phaseEnd);
      maxPhaseEnd = Math.max((long) phaseEnd, maxPhaseEnd);
    }
    // When doing all this addition of double rates some precision is lost so we
    // need a small offset to avoid "false shortfalls".
    double offset = 0.001d;
    if (taskTotal > (totalQty + offset)) {
      return true;
    } else if (maxPhaseEnd > getEndTime(t)) {
      return true;
    }
    return false;
  }

  public boolean isActualShortfall(Task t, boolean includeTemps) {
    PlanElement pe = t.getPlanElement();
    if (pe == null) {
      return false;
    }
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    }
    if ((ar == null) || isProjection(t)) {
      return false;
    }
    if (!ar.isSuccess()) {
      return true;
    }
    double unfilled=0;
    double arEndTime=0;
    double taskEndTime = getEndTime(t);

    if (!ar.isPhased()) {
      unfilled = getQuantity(t) - getQuantity(ar);
      arEndTime = getEndTime(ar);
    } else {
      int[] ats = ar.getAspectTypes();
      int qtyInd = -1;
      int endInd = -1;
      double totalQty=0;
      qtyInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.QUANTITY);
      endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);
      Enumeration phasedResults = ar.getPhasedResults();
      while (phasedResults.hasMoreElements()) {
        double[] results = (double[]) phasedResults.nextElement();
        if (qtyInd != -1) {
          totalQty += results[qtyInd];
        } else {
          totalQty = getQuantity(t);
        }
        double endTime = 
          (endInd == -1 ? taskEndTime : results[endInd]);
        arEndTime = Math.max(arEndTime,endTime);
      }
      unfilled = getQuantity(t) - totalQty;
    }
    double offset = 0.001d;
    if ((unfilled - offset) > 0) {
      return true;
    } else if (includeTemps && (arEndTime > taskEndTime)) {
      return true;
    }
    return false;
  }

  public double getGrantedQuantity(Task t) {
    PlanElement pe = t.getPlanElement();
    if (pe == null) {
      return 0;
    }
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    }
    double taskQty;
    if (ar == null) {
      taskQty = getTotalQuantity(t);
    } else if (isProjection(t)) {
      if (ar.isSuccess()) {
        taskQty = 0;
      } else {
        taskQty = getTotalQuantity(t);
      }
    } else {
      taskQty = getQuantity(ar);
    }
    return taskQty;
  }

  public double getActualDemand(Task task, long startTime, long endTime, long minStart) {
    if (!isProjection(task)) {
      return getQuantity(task);
    }
    long start = (long) getPreferenceBestValue(task, AspectType.START_TIME);
    long end = (long) getPreferenceBestValue(task, AspectType.END_TIME);
    start = Math.max(start, minStart);
    if ((start >= end) || (start >= endTime - 1)) {
      // task is not in this interval
      return 0.0;
    }
    // get the time spanned (if any) for this task within the specified bucket
    long interval_start = Math.max(start, startTime);
    long interval_end = Math.min(end, endTime);
    long time_spanned = interval_end - interval_start;
    // add quantity for overlapping timespan
    Rate rate = getRate(task, interval_start, interval_end);
    try {
      Scalar scalar = (Scalar)
        rate.computeNumerator(Duration.newMilliseconds((double) time_spanned));
      return getDouble(scalar);
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(taskDesc(task)+
            " Start: "+(new Date(start))+
            " time_spanned: "+time_spanned);
      }
      return 0.0;
    }
  }

  public Collection getDailyQuantities(Task task) {
    if (task == null) {
      return null;
    }

    long startTime = -1;
    if (getPreferenceBest(task, AspectType.START_TIME) != null) {
      startTime = getStartTime(task);
    }

    long endTime = getEndTime(task);
    if (startTime == -1) {
      startTime = endTime - 1;
    }

    double dailyRate = 0.0d;

    if (!isProjection(task)) {
      dailyRate = getQuantity(task);
    } else {
      Rate rate = null;

      PrepositionalPhrase pp_rate = task.getPrepositionalPhrase(Constants.Preposition.DEMANDRATE);
      if (pp_rate != null) {
        Object indObj = pp_rate.getIndirectObject();
        if (indObj instanceof Schedule) {
          Schedule sched = (Schedule) indObj;
          Collection rate_elems = 
            sched.getOverlappingScheduleElements(startTime, endTime);
          int n = (rate_elems == null ? 0 : rate_elems.size());
          if (n == 1) {
            rate = (Rate) rate_elems.iterator().next();
          } else if (n > 1) {
            // return a schedule of daily rates
            List ret = new ArrayList(n);
            for (Iterator iter = rate_elems.iterator(); iter.hasNext(); ) {
              ObjectScheduleElement ose = (ObjectScheduleElement) iter.next();
              Rate r = (Rate) ose.getObject();
              double d = getDailyQuantity(r);
              ScheduleElement se = 
                new QuantityScheduleElementImpl(
                    ose.getStartTime(),
                    ose.getEndTime(),
                    d);
              ret.add(se);
            }
            return ret;
          }
        }
      }

      if (rate != null) {
        AspectValue best = getPreferenceBest(task, AlpineAspectType.DEMANDRATE);
        if (best != null) {
          rate = ((AspectRate) best).getRateValue();
        }
      }

      if (rate != null) {
        dailyRate = getDailyQuantity(rate);
      }
    }

    ScheduleElement se = new QuantityScheduleElementImpl(startTime, endTime, dailyRate);
    return Collections.singleton(se);
  }


  public Collection getReportedDailyQuantities(Task task) {
    if (task == null) {
      return null;
    }
    PlanElement pe = task.getPlanElement();
    if (pe == null) {
      return null;
    }
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    }
    if (ar == null) {
      return null;
    }
    return getReportedDailyQuantities(task, ar);
  }

  // This function is based heavily off of logAllocationResult in the
  // LogisticsInventoryFormatter
  public Collection getReportedDailyQuantities(Task task, AllocationResult ar) {
    if (! ar.isSuccess()) {
      // logger.warn("Allocation Result was failure. Not returning.");
      return null;
    }

    if (!ar.isPhased()) {
      long endTime = (long) getEndTime(ar);
      long startTime = 0;

      if (isProjection(task)) {
        startTime = (long) getStartTime(ar);
        if (startTime == endTime) {
          startTime = endTime - 1;
        }
      } else {
        // if supply or withdraw task then we don't have a start time
        startTime = endTime - 1;
      }
      if (startTime == endTime) {
        startTime = endTime - 1;
      }

      double quantity = 0;
      try {
        quantity = getQuantity(task, ar);
      } catch (RuntimeException re) {
        throw re;
      }

      return Collections.singleton(new QuantityScheduleElementImpl(startTime, endTime, quantity));
    }

    Collection returnQSEs = new HashSet();

    int[] ats = ar.getAspectTypes();
    int qtyInd = -1;
    if (isProjection(task)) {
      qtyInd = LogisticsInventoryFormatter.getIndexForType(ats, AlpineAspectType.DEMANDRATE);
    } else {
      qtyInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.QUANTITY);
    }
    int startInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.START_TIME);
    int endInd = LogisticsInventoryFormatter.getIndexForType(ats, AspectType.END_TIME);
    Enumeration phasedResults = ar.getPhasedResults();
    while (phasedResults.hasMoreElements()) {
      double[] results = (double[]) phasedResults.nextElement();
      long startTime = 0;
      if (startInd != -1) {
        startTime = (long) results[startInd];
      }
      long endTime = 0;
      if (endInd == -1) {
        /** MWD The following line of code which replaces the
         *  allocation result end time with the task end time
         *  is only due to a current error in the
         *  UniversalAllocator.   The UA is only setting
         *  the start time in the allocation result. There
         *  is a bug in and when the UA is fixed this line
         *  of code should be removed. If there is no end time
         *  in the allocation result, none should be appended.
         *  GUI needs the end
         *  times for the rates.
         */
        endTime = getEndTime(task);
      } else {
        endTime = (long) results[endInd];
      }
      if ((qtyInd < 0) || (qtyInd >= results.length)) {
        // logger.error("qtyInd is " + qtyInd + " - No Qty in this phase of allocation results:");
        continue;
      }
      double dailyRate = convertResultsToDailyRate(task, results[qtyInd]);
      double quantity = dailyRate;

      if (startTime == 0) {
        startTime = endTime - 1;
      }
      if (startTime > endTime) {
        // logger.error("Error - start time can't be later than end time");
        continue;
      }
      // logger.warn("2nd Method adding (" + startTime + ", " + endTime + ", " + quantity + ")");
      returnQSEs.add(new QuantityScheduleElementImpl(startTime, endTime, quantity));
    }
    // logger.warn("AR Was Null. Result of " + returnQSEs.size() + " is alternate method");
    return returnQSEs;
  }
}
