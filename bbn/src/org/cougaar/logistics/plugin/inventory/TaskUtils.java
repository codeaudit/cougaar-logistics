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
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleElementType;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.logistics.plugin.packer.GenericPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.MoreMath;
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
          getDailyQuantity(task)+" "+
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
    return t.getPreference(AlpineAspectType.DEMANDRATE) != null;
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

  public Rate getRate(Task task) {
    AspectValue best = getPreferenceBest(task, AlpineAspectType.DEMANDRATE);
    if (best == null)
      logger.error("TaskUtils.getRate(), Task is not Projection :"+taskDesc(task));
    return ((AspectRate) best).getRateValue();
  }

  public double getDailyQuantity(Task task) {
    if (isProjection(task)) {
      return getDailyQuantity(getRate(task));
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
    if(isProjection(task)) {
      Rate r = getRate(task);
      if(!(r instanceof FlowRate)) {
        return demandRate * TimeUtils.SEC_PER_DAY;
      }
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

  /** Create a Time Preference for the Refill Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param start The earliest time this preference can have
   *  @param end The last time this preference can have (such as Oplan end time)
   *  @param aspectType The AspectType of the preference- should be start_time or end_time
   *  @param planningFactory The planning factory from the plugin
   *  @return Preference The new Time Preference
   **/
    public Preference createRefillTimePreference(long bestDay, long start, long end,
                                                int aspectType, LogisticsInventoryPG thePG,
						PlanningFactory planningFactory) {
    //TODO - really need last day in theatre from an OrgActivity -
    double daysBetween = ((end - bestDay)  / thePG.getBucketMillis()) - 1;
    //Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
    // define alpha .25
    double alpha = .25;
    Vector points = new Vector();

    AspectScorePoint earliest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, start), alpha);
    AspectScorePoint best = new AspectScorePoint(AspectValue.newAspectValue(aspectType, bestDay), 0.0);
//     AspectScorePoint first_late = new AspectScorePoint(getTimeUtils().addNDays(bestDay, 1),
//                                                        alpha, aspectType);
    AspectScorePoint first_late = new AspectScorePoint(AspectValue.newAspectValue(aspectType,
                                                                                  bestDay + thePG.getBucketMillis()),
                                                       alpha);
    AspectScorePoint latest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, end),
                                                   (alpha + late_score));

    points.addElement(earliest);
    points.addElement(best);
    points.addElement(first_late);
    points.addElement(latest);
    ScoringFunction timeSF = ScoringFunction.
      createPiecewiseLinearScoringFunction(points.elements());
    return planningFactory.newPreference(aspectType, timeSF);
  }



  /** Create a Time Preference for the Refill Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param early The earliest time this preference can have
   *  @param end The last time this preference can have (such as Oplan end time)
   *  @param aspectType The AspectType of the preference- should be start_time or end_time
   *  @param clusterId Agent cluster ID
   *  @param planningFactory Planning factory from plugin
   *  @return Preference The new Time Preference
   **/
  public Preference createTimePreference(long bestDay, long early, long end, int aspectType, MessageAddress clusterId, PlanningFactory planningFactory) {
    long late = getTimeUtils().addNDays(bestDay, 1);
    double daysBetween = ((end - bestDay) / 86400000);

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

    // prefs.addElement(TaskUtils.createDemandRatePreference(planFactory, rate));
    //return prefs;
  }

}






