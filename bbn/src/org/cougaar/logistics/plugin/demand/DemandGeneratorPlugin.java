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
import org.cougaar.glm.ldm.Constants;
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

  private String supplyType;
  private long frequency;

  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String GENERATE_FREQUENCY = "GENERATE_FREQUENCY";

  public final String DEMAND_GENERATOR = "DEMAND_GENERATOR";

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

  private Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

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

    demandGenerator = getDemandTaskGeneratorModule();
    class9Scheduler = getClass9SchedulerModule();

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
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            //if (!taskUtils.isMyInventoryProjection(task, orgName)) {
            if (taskUtils.isMyDemandForecastProjection(task,orgName)) {
              return true;
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
        logger.info("Timer has gone off.  Planning for the next " + ((int) (frequency / getTimeUtils().MSEC_PER_HOUR)) + "hours from " + new Date(planTime));
      }
      Collection relevantProjs = filterProjectionsOnTime(projectionTaskSubscription,
                                                         planTime,
                                                         planTime + frequency);
      //TODO: MWD Remove debug statements:
      if ((getOrgName() != null) &&
          (getOrgName().trim().equals("1-35-ARBN"))) {
        System.out.println("I'm waking up - new period starts " + new Date(planTime) + " and Num of projections for " + getOrgName() + " is: " + relevantProjs.size());
      }

      relevantProjs = class9Scheduler.filterProjectionsToMaxSpareParts(relevantProjs);
      demandGenerator.generateDemandTasks(planTime, frequency, relevantProjs);
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
  private DemandTaskGeneratorIfc getDemandTaskGeneratorModule() {
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


  public long getFrequency() {
    return frequency;
  }

  private boolean isLegalFrequency(long frequencyInMSEC) {
    long remainder = -1;
    if (frequency >= getTimeUtils().MSEC_PER_DAY) {
      remainder = frequency % getTimeUtils().MSEC_PER_DAY;
    } else if (frequency >= getTimeUtils().MSEC_PER_HOUR) {
      remainder = frequency % getTimeUtils().MSEC_PER_HOUR;
    }
    return ((frequencyInMSEC > 0) && (remainder == 0));
  }


  public boolean frequencyInDays() {
    return (getFrequencyUnit() == getTimeUtils().MSEC_PER_DAY);
  }

  public long getFrequencyUnit() {
    if (frequency >= getTimeUtils().MSEC_PER_DAY) {
      return getTimeUtils().MSEC_PER_DAY;
    } else {
      return getTimeUtils().MSEC_PER_HOUR;
    }
  }

  private int getFrequencyMultiplier() {
    return (int) ((int) frequency / getFrequencyUnit());
  }

  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    final String errorString = "DemandGeneratorPlugin requires 2 parameters, Supply Type and Gemerate Frequency (secs).  Generate Frequency must be in whole days or whole hours (< 24)";
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

    //frequency = (new Long((String)map.get(GENERATE_FREQUENCY))).longValue();
    frequency = 24 * 60 * 60;

    frequency = frequency * 1000;

    if (!isLegalFrequency(frequency)) {
      logger.error("Illegal Frequency - not in days or hours");
      frequency = -1;
    }

    if (((supplyType == null) ||
        (frequency <= 0) &&
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
    long timeOut = 0;
    calendar.setTimeInMillis(timeIn);
    if (frequencyInDays()) {
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


  /**
   * Schedule a timer for midnight tonight.
   *
   */

  protected void resetTimer() {
    long expiration = getStartOfPeriod() + frequency;
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
      getAlarmService().addRealTimeAlarm(timer);
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
      return (System.currentTimeMillis() +
          (expirationTime - getCurrentTimeMillis()));
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




