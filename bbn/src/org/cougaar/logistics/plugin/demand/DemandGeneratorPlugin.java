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

package org.cougaar.logistics.plugin.demand;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.logistics.plugin.inventory.*;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.logistics.plugin.utils.OrgActivityPred;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.util.UnaryPredicate;

import java.lang.reflect.Constructor;
import java.util.*;

/** The DemandGeneratorPlugin generates demand during execution.
 **/

public class DemandGeneratorPlugin extends ComponentPlugin
    implements UtilsProvider {
  private DomainService domainService;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils AssetUtils;
  private ScheduleUtils scheduleUtils;
  private HashMap pluginParams;

  private DemandTaskGeneratorIfc demandGenerator;
  private DGClass9Scheduler class9Scheduler;
  private DemandGeneratorOutputModule demandOutputModule;

  private String supplyType;
  private long period;
  private long stepPeriod;

  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String GENERATE_PERIOD = "GENERATE_PERIOD";
  public final String STEP_PERIOD = "STEP_PERIOD";
  public final String RANDOM_DEVIATION_ON = "RANDOM_DEVIATION_ON";

  public final String DEMAND_GENERATOR = "DEMAND_GENERATOR";
  public final String DG_TO_FILE = "DG_TO_FILE";

  private boolean poissonOn = true;

  private Organization myOrganization;
  private String myOrgName;

  //TODO: MWD Remove
  //private long orgStartTime=-1;
  //private long orgEndTime=-1;

  LogisticsOPlan logOPlan = null;


  /** A timer for recurrent events.  All access should be synchronized on timerLock **/
  private Alarm timer = null;

  /** Lock for accessing timer **/
  private final Object timerLock = new Object();

  //TODO: remove calendar variable that old version of getStartOfPeriod() depended upon.
  //private Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

  public void load() {
    super.load();
    logger = getLoggingService(this);
    timeUtils = new TimeUtils(this);
    AssetUtils = new AssetUtils(this);
    taskUtils = new TaskUtils(this);
    scheduleUtils = new ScheduleUtils(this);
    pluginParams = readParameters();

    domainService = (DomainService)
        getServiceBroker().getService(this,
                                      DomainService.class,
                                      new ServiceRevokedListener() {
                                        public void serviceRevoked(ServiceRevokedEvent re) {
                                          if (DomainService.class.equals(re.getService()))
                                            domainService = null;
                                        }
                                      });

    logger = getLoggingService(this);

    String dg_to_file = (String)pluginParams.get(DG_TO_FILE);
    if (dg_to_file != null && dg_to_file.equalsIgnoreCase("true")) {
      demandOutputModule = getDemandOutputModule();

      String demandGen = (String)pluginParams.get(DEMAND_GENERATOR);
      if (demandGen != null && demandGen.endsWith("DemandGeneratorInputModule")) {
        logger.warn("Invalid Combination of Plugin Params: Cannot output AND " +
                    "input demand info in same run.  Changing Demand " +
                    "Generator Module to default!");
        pluginParams.remove(DEMAND_GENERATOR);
      }
    }

    demandGenerator = getDemandTaskGeneratorModule();
    class9Scheduler = getClass9SchedulerModule();

  }

  private DemandGeneratorOutputModule getDemandOutputModule() {
    return new DemandGeneratorOutputModule(this);
  }

  public void unload() {
    super.unload();
    cancelTimer();
    if (domainService != null) {
      getServiceBroker().releaseService(this, DomainService.class, domainService);
    }
  }

  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;

  private IncrementalSubscription projectionTaskSubscription;

  /*** TODO: MWD Remove
   private IncrementalSubscription orgActivities;
   private IncrementalSubscription oplanSubscription;
   **/
  private IncrementalSubscription logisticsOPlanSubscription;


  public void setupSubscriptions() {

    selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);

    //TODO: MWD Remove
    //UnaryPredicate orgActivityPred = new OrgActivityPred();
    //orgActivities = (IncrementalSubscription) blackboard.subscribe(orgActivityPred);
    //oplanSubscription = (IncrementalSubscription) blackboard.subscribe(oplanPredicate);
    logisticsOPlanSubscription = (IncrementalSubscription) blackboard.subscribe(new LogisticsOPlanPredicate());

    resetTimer();
  }

  /** TODO: MWD Remove
   private static UnaryPredicate oplanPredicate = new UnaryPredicate() {
   public boolean execute(Object o) {
   return (o instanceof Oplan);
   }
   };

   **/

  /** Selects the LogisticsOPlan objects **/
  private static class LogisticsOPlanPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return o instanceof LogisticsOPlan;
    }
  }

  private static class ProjectionTaskPredicate implements UnaryPredicate {
    String supplyType;
    String orgName;
    TaskUtils taskUtils;

    public ProjectionTaskPredicate(String type, String orgname, TaskUtils aTaskUtils) {
      supplyType = type;
      orgName = orgname;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (!taskUtils.isLevel2(task)) {
            if (taskUtils.isDirectObjectOfType(task, supplyType)) {
              //if (!taskUtils.isMyInventoryProjection(task, orgName)) {
              if (taskUtils.isMyDemandForecastProjection(task, orgName)) {
                return true;
              }
            }
          }
        }
      }
      return false;
    }

  }

  private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        return ((Organization) o).isSelf();
      }
      return false;
    }
  };


  /** TODO: MWD Remove
   *  Find the earliest and latest times of all the org activites.
   *
   private void computeOrgTimes(Enumeration orgActs) {
   long latestEnd = 0;
   long earliestStart = 0;
   while(orgActs.hasMoreElements()) {
   OrgActivity oa = (OrgActivity) orgActs.nextElement();
   long endTime = oa.getEndTime();
   if (endTime > latestEnd) {
   latestEnd = endTime;
   }
   long startTime = oa.getStartTime();
   if (startTime < earliestStart) {
   earliestStart = startTime;
   }
   }
   orgEndTime = latestEnd;
   orgStartTime = earliestStart;
   }

   public long getOrgStartTime() {
   return orgStartTime;
   }

   public long getOrgEndTime() {
   return orgEndTime;
   }

   ***/

  // get the first day in theater
  public long getLogOPlanStartTime() {
    return logOPlan.getStartTime();
  }

  // get the last day in theater
  public long getLogOPlanEndTime() {
    return logOPlan.getEndTime();
  }

  public TaskUtils getTaskUtils() {
    return taskUtils;
  }

  public TimeUtils getTimeUtils() {
    return timeUtils;
  }

  public AssetUtils getAssetUtils() {
    return AssetUtils;
  }

  public ScheduleUtils getScheduleUtils() {
    return scheduleUtils;
  }

  public String getSupplyType() {
    return supplyType;
  }

  private Organization getMyOrganization(Enumeration orgs) {
    Organization myOrg = null;
    // look for this organization
    if (orgs.hasMoreElements()) {
      myOrg = (Organization) orgs.nextElement();
    }
    return myOrg;
  }

  public Organization getMyOrganization() {
    return myOrganization;
  }

  public String getOrgName() {
    if ((myOrgName == null) &&
        (getMyOrganization() != null)) {
      myOrgName = getMyOrganization().getItemIdentificationPG().getItemIdentification();
    }
    return myOrgName;
  }

  public long getCurrentTimeMillis() {
    return currentTimeMillis();
  }

  public boolean publishAdd(Object o) {
    getBlackboardService().publishAdd(o);
    return true;
  }

  public boolean publishChange(Object o) {
    getBlackboardService().publishChange(o);
    return true;
  }

  public boolean publishRemove(Object o) {
    getBlackboardService().publishRemove(o);
    return true;
  }

  public PlanningFactory getPlanningFactory() {
    PlanningFactory rootFactory = null;
    if (domainService != null) {
      rootFactory = (PlanningFactory) domainService.getFactory("planning");
    }
    return rootFactory;
  }

  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService)
        getServiceBroker().getService(requestor,
                                      LoggingService.class,
                                      null);
  }

  public BlackboardService getBlackboardService() {
    return blackboard;
  }

  protected void execute() {
    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if (myOrganization != null) {
        projectionTaskSubscription = (IncrementalSubscription) blackboard.
            subscribe(new ProjectionTaskPredicate(supplyType, getOrgName(), taskUtils));
      }
    }

    /** TODO: MWD Remove
     if(orgActivities.getCollection().isEmpty()) {
     return;
     }
     else if((orgStartTime == -1) || (orgEndTime == -1)) {
     computeOrgTimes(orgActivities.elements());
     }

     if (oplanSubscription.getCollection().isEmpty()) {
     return;
     }
     **/

    // get the Logistics OPlan (our homegrown version with specific dates).
    if ((logOPlan == null) || logisticsOPlanSubscription.hasChanged()) {
      Iterator opIt = logisticsOPlanSubscription.iterator();
      if (opIt.hasNext()) {
        //we only expect to have one
        logOPlan = (LogisticsOPlan) opIt.next();
      }
    }

    if (myOrganization == null) {
      if (logger.isInfoEnabled()) {
        logger.info("\n DemandGeneratorPlugin " + supplyType +
                    " not ready to process tasks yet." +
                    " my org is: " + myOrganization);
      }
      return;
    }

    //nothing for now
    if (timerExpired()) {
      long planTime = getStartOfPeriod();
      if (logger.isInfoEnabled()) {
        logger.info("Timer has gone off.  Planning for the next " + ((int) (period / getTimeUtils().MSEC_PER_HOUR)) + "hours from " + new Date(planTime));
      }
      for (long time = planTime; time<planTime+period; time += stepPeriod) {

	  //filter and create task from time to time plus step period inclusive
	  //(stepPeriod -1)
        Collection relevantProjs = filterProjectionsOnTime(projectionTaskSubscription,
                                                         time, time + (stepPeriod - 1));
        //TODO: MWD Remove debug statements:
        if ((getOrgName() != null) &&
            (getOrgName().trim().equals("1-35-ARBN")) &&
            (logger.isDebugEnabled())) {
          logger.debug("I'm waking up - new period starts " + new Date(planTime) +
                             " and step (start,dur) is (" + new Date(time) +
                             "," + stepPeriod + ")" +
                             " and Num of projections for " + getOrgName() +
                             " is: " + relevantProjs.size());
        }

        relevantProjs = class9Scheduler.filterProjectionsToMaxSpareParts(relevantProjs);
	//filter and create task from time to time plus step period inclusive
	  //(stepPeriod -1)
        List demandTasks = demandGenerator.generateDemandTasks(time, (stepPeriod - 1), relevantProjs);

        if (demandOutputModule != null) {
          demandOutputModule.writeDemandOutputToFile(demandTasks);
        }
      }

      resetTimer();
    }
  }


  /***
   * Filter the passed in collection of projection tasks to those overlapping the period
   * between the start and end time.
   *
   * @param projections - whole collection of projection tasks
   * @param startGen - start time for generating supply tasks
   * @param endGen -  end time for generating supply tasks
   * @return Collection of projection tasks filtered by start and end time.
   */
  protected Collection filterProjectionsOnTime(Collection projections,
                                               long startGen,
                                               long endGen) {
    ArrayList filteredProjs = new ArrayList();
    Iterator projectionIt = projections.iterator();
    while (projectionIt.hasNext()) {
      Task proj = (Task) projectionIt.next();
      if ((TaskUtils.getStartTime(proj) < endGen) &&
          (TaskUtils.getEndTime(proj) > startGen)) {
        filteredProjs.add(proj);
      }
    }
    return filteredProjs;
  }


  /**
   * Creates an instance of an DemandTaskGeneratorIfc by
   * searching plugin parameters for DEMAND_GENERATOR argument.
   * In the absence of an REQ_EXPANDER argument, a default is used:
   * org.cougaar.logistics.plugin.demand.DemandTaskGenerator
   * @return {@link DemandTaskGeneratorIfc}
   **/
  protected DemandTaskGeneratorIfc getDemandTaskGeneratorModule() {
    String demGenClass = (String) pluginParams.get(DEMAND_GENERATOR);
    if (demGenClass != null) {
      try {
        Class[] paramTypes = {this.getClass()};
        Object[] initArgs = {this};
        Class cls = Class.forName(demGenClass);
        Constructor constructor = cls.getConstructor(paramTypes);
        DemandTaskGeneratorIfc demandGen = (DemandTaskGeneratorIfc) constructor.newInstance(initArgs);
        logger.info("Using RequirementsExpander " + demGenClass);
        return demandGen;
      } catch (Exception e) {
        logger.error(e + " Unable to create demandTaskGeneratorModule instance of " + demGenClass + ". " +
                     "Loading default org.cougaar.logistics.plugin.demand.DemandTaskGenerator");
      }
    }

    return new DemandTaskGenerator(this);
  }


  private DGClass9Scheduler getClass9SchedulerModule() {
    return new DGClass9Scheduler(this);
  }


  public boolean getPoissonOn() {
    return poissonOn;
  }

  public long getPeriod() {
    return period;
  }

  private boolean isLegalPeriod(long periodInMSEC) {
    long remainder = -1;
    if (periodInMSEC >= getTimeUtils().MSEC_PER_DAY) {
      remainder = periodInMSEC % getTimeUtils().MSEC_PER_DAY;
    } else if (periodInMSEC >= getTimeUtils().MSEC_PER_HOUR) {
      remainder = periodInMSEC % getTimeUtils().MSEC_PER_HOUR;
    }
    return ((periodInMSEC > 0) && (remainder == 0));
  }


  public boolean periodInDays() {
    return (getPeriodUnit() == getTimeUtils().MSEC_PER_DAY);
  }

  public long getPeriodUnit() {
    if (period >= getTimeUtils().MSEC_PER_DAY) {
      return getTimeUtils().MSEC_PER_DAY;
    } else {
      return getTimeUtils().MSEC_PER_HOUR;
    }
  }

  private int getPeriodMultiplier() {
    return (int) ((int) period / getPeriodUnit());
  }

  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    final String errorString = "DemandGeneratorPlugin requires 2 parameters, Supply Type and Gemerate Period (secs).  Generate Period must be in whole days or whole hours (< 24).  There is also a optional parameter RANDOM_DEVIATION_ON=<true,false> which enables whole number POISSON random deviation around the supply task quanity.";
    Collection p = getParameters();

    if (p.isEmpty()) {
      if (logger.isErrorEnabled()) {
        logger.error(errorString);
      }
      return null;
    }
    HashMap map = new HashMap();
    int idx;

    for (Iterator i = p.iterator(); i.hasNext();) {
      String s = (String) i.next();
      if ((idx = s.indexOf('=')) != -1) {
        String key = new String(s.substring(0, idx));
        String value = new String(s.substring(idx + 1, s.length()));
        map.put(key.trim(), value.trim());
      }
    }
    supplyType = (String) map.get(SUPPLY_TYPE);

    String periodString = (String) map.get(GENERATE_PERIOD);

    if ((periodString != null) &&
        (!(periodString.trim().equals("")))) {
      try {
        period = (new Long(periodString)).longValue();
      } catch (Exception e) {
        period = -1;
      }
    } else {
      //Default is 24 hours
      period = 24 * 60 * 60;
      period = period * 1000;
    }

    if (!isLegalPeriod(period)) {
      logger.error("Illegal Period - not in days or hours");
      period = -1;
    }

    String stepPeriodString = (String)map.get(STEP_PERIOD);
    if ((stepPeriodString != null) &&
        (!(stepPeriodString.trim().equals("")))) {
      try {
        stepPeriod = (new Long(stepPeriodString)).longValue();
      } catch (Exception e) {
        stepPeriod = -1;
      }
    } else {
      //Default is 24 hours
      stepPeriod = 24 * 60 * 60;
      stepPeriod = stepPeriod * 1000;
    }

    if (!isLegalPeriod(stepPeriod)) {
      logger.error("Illegal Step Period - not in days or hours");
      stepPeriod = -1;
    }


    String poissonOnString = (String) map.get(RANDOM_DEVIATION_ON);

    if (poissonOnString != null) {
      poissonOnString = poissonOnString.trim().toLowerCase();
      poissonOn = (poissonOnString.equals("true"));
      //logger.shout("RANDOM_DEVIATION_ON=" + poissonOn);
    }

    if (((supplyType == null) ||
        (period <= 0) &&
        logger.isErrorEnabled())) {
      logger.error(errorString);
    }
    return map;
  }


  /**
   * Get the time in milliseconds that would be midnight of the day
   * before or first thing in the morning today.
   *
   * @return - the time in milliseconds that represents first thing in the
   *morning today
   */
  protected long getStartOfPeriod() {
    long timeIn = getCurrentTimeMillis();
    //truncate to the whole number that represents the period num since the start of time.
    long periods = (long) (timeIn / period);
    //Multiply it back to which gives the start of the period.
    long timeOut = periods * period;
    if (timeIn == timeOut) {
      logger.debug("GetStartOfToday - unexpected timeIn==timeOut==" + new Date(timeOut));

    }
    return timeOut;
  }

  /** TODO: MWD take out this older verion of getStartOfPeriod()

   protected long getStartOfPeriod() {
   long timeIn = getCurrentTimeMillis();
   long timeOut = 0;
   calendar.setTimeInMillis(timeIn);
   if (periodInDays()) {
   calendar.set(calendar.HOUR_OF_DAY, 0);
   }
   calendar.set(calendar.MINUTE, 0);
   calendar.set(calendar.SECOND, 0);
   calendar.set(calendar.MILLISECOND, 0);
   timeOut = calendar.getTimeInMillis();
   if (timeIn == timeOut) {
   logger.error("GetStartOfToday - unexpected timeIn==timeOut==" + new Date(timeOut));

   }
   return timeOut;
   }

   **/
  /**
   * Schedule a timer for midnight tonight.
   *
   */

  protected void resetTimer() {
    long expiration = getStartOfPeriod() + period;
    resetTimer(expiration);
  }


  /**
   * Schedule a update wakeup after some interval of time
   * @param delay how long to delay before the timer expires.
   **/
  protected void resetTimerWDelay(long delay) {
    resetTimer(getCurrentTimeMillis() + delay);
  }


  /**
   * Schedule a update wakeup after some interval of time
   * @param expiration The date-time you wish this alarm to expire at
   **/
  protected void resetTimer(long expiration) {
    synchronized (timerLock) {
      Alarm old = timer;        // keep any old one around
      if (old != null) {
        old.cancel();           // cancel the old one
      }
      if (getBlackboardService() == null &&
          logger != null &&
          logger.isWarnEnabled()) {
        logger.warn(
            "Started service alarm before the blackboard service" +
            " is available");
      }

      /**
       * TODO: MWD Remove
       *
       if ((getOrgName() != null) &&
       (getOrgName().trim().equals("1-35-ARBN"))) {
       System.out.println("Setting new timer to go at: " + new Date(expiration));
       }
       */

      timer = new CougTimeAlarm(expiration);
      getAlarmService().addAlarm(timer);
    }
  }

  /**
   * Cancel the timer.
   **/
  protected void cancelTimer() {
    synchronized (timerLock) {
      if (timer == null) return;
      //     if (logger.isDebugEnabled()) logger.debug("Cancelling timer");
      timer.cancel();
      timer = null;
    }
  }

  /** access the timer itself (if any) **/
  protected Alarm getTimer() {
    synchronized (timerLock) {
      return timer;
    }
  }

  /** When will (has) the timer expire **/
  protected long getTimerExpirationTime() {
    synchronized (timerLock) {
      if (timer != null) {
        return timer.getExpirationTime();
      } else {
        return 0;
      }
    }
  }

  /** Returns true IFF there is an unexpired timer.
   **/
  protected boolean hasUnexpiredTimer() {
    synchronized (timerLock) {
      if (timer != null) {
        return !timer.hasExpired();
      } else {
        return false;
      }
    }
  }

  /**
   * Test if the timer has expired.
   * @return false if the timer is not running or has not yet expired
   * else return true.
   **/
  protected boolean timerExpired() {
    synchronized (timerLock) {
      return timer != null && timer.hasExpired();
    }
  }

  private final class CougTimeAlarm implements Alarm {
    private long expirationTime;
    private boolean expired = false;

    public CougTimeAlarm(long expiration) {
      this.expirationTime = expiration;
    }

    public long getExpirationTime() {
      return expirationTime;
    }

    public synchronized void expire() {
      if (!expired) {
        expired = true;
        BlackboardService bb = getBlackboardService();
        if (bb != null) {
          bb.signalClientActivity();
        } else {
          if (logger != null && logger.isWarnEnabled()) {
            logger.warn(
                "Alarm to trigger at " + (new Date(expirationTime)) + " has expired," +
                " but the blackboard service is null.  Plugin " +
                " model state is " + getModelState());
          }
        }
      }
    }

    public synchronized boolean hasExpired() {
      return expired;
    }

    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
  }
}




