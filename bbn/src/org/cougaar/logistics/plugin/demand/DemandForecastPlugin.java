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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.inventory.LogisticsOPlan;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.utils.LogisticsOPlanPredicate;
import org.cougaar.logistics.plugin.utils.OrgActivityPred;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.logistics.plugin.utils.TaskScheduler;
import org.cougaar.logistics.plugin.utils.TaskSchedulingPolicy;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.Filters;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.DynamicUnaryPredicate;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.DynamicUnaryPredicate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/** The DemandForecastPlugin is the Glue of demand generation.
 *  It handles all blackboard services for its modules,
 *  facilitates inter-module communication and manages the
 *  subscriptions.
 *  All modules are called from the DemandForecastPlugin.
 **/

public class DemandForecastPlugin extends ComponentPlugin
    implements UtilsProvider {

  private DomainService domainService;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils AssetUtils;
  private ScheduleUtils scheduleUtils;
  private HashMap pluginParams;
  private HashMap pgToPredsHash;
  private HashMap pgToGPTaskHash;
  private HashMap subToPGsHash;
  private HashMap predToSubHash;
  private boolean rehydrate = false;

  private String supplyType;
  private Class supplyClassPG;

  private Organization myOrganization;
  private String myOrgName;
  private DetReqExpanderIfc determineRequirementsExpander;
  private GenProjExpanderIfc generateProjectionsExpander;
  //  private SchedulerModule planningScheduler;

  private boolean processedDetReq = false;

  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String SUPPLY_PG_CLASS = "SUPPLY_PG_CLASS";
  public final String REQ_EXPANDER = "REQ_EXPANDER";
  public final String PROJ_EXPANDER = "PROJ_EXPANDER";
  public final String TASK_SCHEDULER_OFF = "TASK_SCHEDULER_OFF";

  LogisticsOPlan logOPlan = null;

  public void load() {
    super.load();
    logger = getLoggingService(this);
    timeUtils = new TimeUtils(this);
    AssetUtils = new AssetUtils(this);
    taskUtils = new TaskUtils(this);
    scheduleUtils = new ScheduleUtils(this);

    //detReqHandler = new DetReqAggHandler(this);
    // readParameters() initializes supplyType and inventoryFile
    pluginParams = readParameters();
    determineRequirementsExpander = getDetermineRequirementsExpanderModule();
    generateProjectionsExpander = getGenerateProjectionsExpanderModule();


    pgToPredsHash = new HashMap();
    pgToGPTaskHash = new HashMap();
    subToPGsHash = new HashMap();
    predToSubHash = new HashMap();

    //startTime = currentTimeMillis();


    domainService = (DomainService)
        getServiceBroker().getService(this,
                                      DomainService.class,
                                      new ServiceRevokedListener() {
                                        public void serviceRevoked(ServiceRevokedEvent re) {
                                          if (DomainService.class.equals(re.getService()))
                                            domainService = null;
                                        }
                                      });
    //   System.out.println("\n LOADING DemandForecastPlugin of type: " + supplyType +
//  		       "in org: " + getAgentIdentifier().toString() +
//    		       " this plugin is: " + this);
  }

  public void unload() {
    super.unload();
    if (domainService != null) {
      getServiceBroker().releaseService(this, DomainService.class, domainService);
    }
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

  public Organization getMyOrganization() {
    return myOrganization;
  }

  public long getCurrentTimeMillis() {
    return currentTimeMillis();
  }

  public boolean publishAdd(Object o) {
    getBlackboardService().publishAdd(o);
    return true;
  }

  public void publishAddExpansion(Expansion expansion) {
    PluginHelper.publishAddExpansion(getBlackboardService(), expansion);
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
    if ((supplyClassPG == null) ||
        (oplanSubscription.isEmpty()) ||
        (orgActivities.isEmpty())) {
      processedDetReq = false;
      return;
    }

    genProjTaskScheduler.initForExecuteCycle();

    if (!detReqSubscription.isEmpty()) {
      Iterator detReqIt = detReqSubscription.iterator();
      Task detReqTask = (Task) detReqIt.next();
      processedDetReq = (!(detReqTask.getPlanElement() == null));




      //There should be both a determineRequirements task
      //and an oplan before kicking off the expander for the first time.
      //from then on out we should be catching additional assets added, or removed.
      //It is also possible that this agent has no assets and the expander has to dispose of the detReqTask.

      //if there is a new determine requirements task or new oplan do this
      if (((!orgActivities.getAddedCollection().isEmpty()) &&
          (!processedDetReq)) ||
          (!detReqSubscription.getAddedCollection().isEmpty())) {
        processDetReq(detReqSubscription,
                      assetsWithPGSubscription);
      }
      //otherwise just issue a new
      else if (!assetsWithPGSubscription.getAddedCollection().isEmpty()) {
        processDetReq(detReqSubscription,
                      assetsWithPGSubscription.getAddedCollection());
      } else if (!assetsWithPGSubscription.getRemovedCollection().isEmpty()) {
        removeFromDetReq(detReqSubscription,
                         assetsWithPGSubscription.getRemovedCollection());
      }
    }

    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if (myOrganization != null) {
        projectSupplySubscription = (IncrementalSubscription)
            blackboard.subscribe(new ProjectSupplyPredicate(supplyType, getOrgName(), taskUtils));
      }
    }

    if (myOrganization == null) {
      if (logger.isInfoEnabled()) {
        logger.info("\n DemandForecastPlugin " + supplyType +
                    " not ready to process tasks yet." +
                    " my org is: " + myOrganization);
      }
      return;
    }

    // get the Logistics OPlan (our homegrown version with specific dates).
    if ((logOPlan == null) || logisticsOPlanSubscription.hasChanged()) {
      Iterator opIt = logisticsOPlanSubscription.iterator();
      if (opIt.hasNext()) {
        //we only expect to have one
        logOPlan = (LogisticsOPlan) opIt.next();
      }
      //Only after we have all the constituent parts to start going - oplan, orgActitivities, logOplan do we
      //lay down
      if (logOPlan != null) {
        if ((supplyClassPG != null) &&
            (genProjTaskScheduler == null)) {
          setupTaskScheduler();
//          genProjSubscription = (IncrementalSubscription) blackboard.subscribe(new GenProjPredicate(supplyType, taskUtils));
        }
      } else {// wait for logOPlan
        logger.warn("OrgActivities received but no LogOPlan object. "+getOrgName()+" waiting...");
        return;
      }
    }

//    if (genProjSubscription != null) {

    HashSet justExpandedPGs = new HashSet();

    if (! genProjTaskScheduler.isEmpty()) {
      Collection removed = genProjTaskScheduler.getRemovedCollection();
      Collection added = genProjTaskScheduler.getAddedCollection();
      TimeSpan timeSpan = genProjTaskScheduler.getCurrentTimeSpan();
      if (! removed.isEmpty())
        processRemovedGenProjs (removed, timeSpan);
      if (! added.isEmpty())
        justExpandedPGs = processNewGenProjs (added, timeSpan);

      genProjTaskScheduler.finishedExecuteCycle();
    }
    //if the determine requirements task has already fired we're this far down
    //in the execute we should check the hash table subscriptions and see if
    //we have to regenerate some of the expansions due to subscription changes.
    if (processedDetReq) {
      checkAndProcessHashSubscriptions(justExpandedPGs);
    }

    //Update the Allocation results on new or changed GP PlanElements
    if (genProjPESubscription.hasChanged()) {
      if (!genProjPESubscription.getAddedCollection().isEmpty()) {
        generateProjectionsExpander.updateAllocationResults(genProjPESubscription.getAddedCollection());
      }
      if (!genProjPESubscription.getChangedCollection().isEmpty()) {
        generateProjectionsExpander.updateAllocationResults(genProjPESubscription.getChangedCollection());
      }
    }

    //Update the Allocation results on new or changed DR PlanElements
    if (detReqPESubscription.hasChanged()) {
      if (!detReqPESubscription.getAddedCollection().isEmpty()) {
        determineRequirementsExpander.updateAllocationResults(detReqPESubscription.getAddedCollection());
      }
      if (!detReqPESubscription.getChangedCollection().isEmpty()) {
        determineRequirementsExpander.updateAllocationResults(detReqPESubscription.getChangedCollection());
      }
    }

    //deal with rehydration
    if (rehydrate) {
      rehydrateHashMaps();
      rehydrate = false;
    }

  }


  private IncrementalSubscription orgActivities;
  private IncrementalSubscription oplanSubscription;
  private IncrementalSubscription detReqSubscription;
  private IncrementalSubscription detReqPESubscription;
  private TaskScheduler genProjTaskScheduler;
  private IncrementalSubscription genProjPESubscription;
  private IncrementalSubscription projectSupplySubscription;
  private IncrementalSubscription logisticsOPlanSubscription;

  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;

  /** Subscription for all assets with plugin parameters PG class attached to it **/
  private IncrementalSubscription assetsWithPGSubscription;

  protected void setupSubscriptions() {
    if (blackboard.didRehydrate()) {
      rehydrate = true;
    }
    selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);

    UnaryPredicate orgActivityPred = new OrgActivityPred();
    orgActivities = (IncrementalSubscription) blackboard.subscribe(orgActivityPred);
    predToSubHash.put(orgActivityPred, orgActivities);

    oplanSubscription = (IncrementalSubscription) blackboard.subscribe(oplanPredicate);

    detReqSubscription = (IncrementalSubscription) blackboard.subscribe(new DetReqPredicate(supplyType, taskUtils));
    detReqPESubscription = (IncrementalSubscription) blackboard.subscribe(new DetReqPEPredicate(supplyType, taskUtils));

    genProjTaskScheduler = null;
    assetsWithPGSubscription = null;

    if (supplyClassPG != null) {
      setupTaskScheduler();
      //genProjSubscription = (IncrementalSubscription) blackboard.subscribe(new GenProjPredicate(supplyType, taskUtils));

      assetsWithPGSubscription = (IncrementalSubscription)
          getBlackboardService().subscribe(new AssetOfTypePredicate(supplyClassPG));
    }

    genProjPESubscription = (IncrementalSubscription)
        blackboard.subscribe(new GenProjPEPredicate(supplyType, taskUtils));

    logisticsOPlanSubscription = (IncrementalSubscription) blackboard.subscribe(new LogisticsOPlanPredicate());
  }

  private void setupTaskScheduler() {
    String taskScheduler = (String) pluginParams.get(TASK_SCHEDULER_OFF);
    boolean turnOffTaskSched = new Boolean(taskScheduler).booleanValue();
    QuiescenceReportService q = (QuiescenceReportService)
      getServiceBroker().getService(this, QuiescenceReportService.class, null);
    if (!turnOffTaskSched) {
      java.io.InputStream is = null;
      try {
        is = getConfigFinder().open ("demandSchedPolicy.xml");
      } catch (Exception e) {
        logger.error ("Could not find file demandSchedPolicy.xml");
      }
      genProjTaskScheduler = new TaskScheduler
        (new GenProjPredicate (supplyType, taskUtils),
         TaskSchedulingPolicy.fromXML (is, this, getAlarmService()),
         blackboard, q, logger,"GenProjs for " + getBlackboardClientName());
    } else {
      logger.debug("TASK SCHEDULER OFF - TASK SCHEDULER OFF - TASK SCHEDULER OFF - TASK SCHEDULER OFF");
     genProjTaskScheduler = new TaskScheduler
      (new GenProjPredicate (supplyType, taskUtils),
       new TaskSchedulingPolicy (new TaskSchedulingPolicy.Predicate[]
                                     {TaskSchedulingPolicy.PASSALL}),
       blackboard, q, logger,"GenProjs for " + getBlackboardClientName());
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


  private static UnaryPredicate oplanPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Oplan);
    }
  };

  /** Predicate defining expandable Determine Reqs. **/
  private static class DetReqPredicate implements UnaryPredicate {
    private String supplyType;
    private TaskUtils taskUtils;

    public DetReqPredicate(String type, TaskUtils utils) {
      this.supplyType = type;
      this.taskUtils = utils;
    } // constructor

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.DETERMINEREQUIREMENTS)) {
          return taskUtils.isTaskOfType(t, supplyType);
        } // if
      } // if
      return false;
    } // execute
  } // DetReqPredicate

  /** Predicate defining Determine Reqs PlanElements created in this plugin.  **/
  private static class DetReqPEPredicate implements UnaryPredicate {
    private String supplyType;
    private TaskUtils taskUtils;

    public DetReqPEPredicate(String type, TaskUtils utils) {
      this.supplyType = type;
      this.taskUtils = utils;
    } // constructor

    public boolean execute(Object o) {
      if (o instanceof PlanElement) {
        Task t = ((PlanElement) o).getTask();
        if (t.getVerb().equals(Constants.Verb.DETERMINEREQUIREMENTS)) {
          return taskUtils.isTaskOfType(t, supplyType);
        } // if
      } // if
      return false;
    } // execute
  } // DetReqPEPredicate

  /** Predicate defining expandable Determine Reqs. **/
  private static class GenProjPredicate
      implements TaskSchedulingPolicy.Predicate {
    private String supplyType;
    private TaskUtils taskUtils;

    public GenProjPredicate(String type, TaskUtils utils) {
      this.supplyType = type;
      this.taskUtils = utils;
    } // constructor

    public boolean execute (Task t) {
      return t.getVerb().equals (Constants.Verb.GENERATEPROJECTIONS) &&
             taskUtils.isTaskOfTypeString (t, supplyType);
    } // execute
  } // GenProjPredicate

  /** Predicate defining GenerateProjection PEs that this plugin created. **/
  private static class GenProjPEPredicate implements UnaryPredicate {
    private String supplyType;
    private TaskUtils taskUtils;

    public GenProjPEPredicate(String type, TaskUtils utils) {
      this.supplyType = type;
      this.taskUtils = utils;
    }

    public boolean execute(Object o) {
      if (o instanceof PlanElement) {
        Task t = ((PlanElement) o).getTask();
        if (t.getVerb().equals(Constants.Verb.GENERATEPROJECTIONS)) {
          return taskUtils.isTaskOfTypeString(t, supplyType);
        }
      }
      return false;
    }
  } // end GenProjPEPredicate

  /** Predicate defining ProjectSupply tasks that this plugin created. **/
  private static class ProjectSupplyPredicate implements UnaryPredicate {
    private String supplyType;
    private String orgName;
    private TaskUtils taskUtils;

    public ProjectSupplyPredicate(String type, String myOrgName, TaskUtils utils) {
      this.supplyType = type;
      this.orgName = myOrgName;
      this.taskUtils = utils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isTaskOfTypeString(t, supplyType)) {
            return (taskUtils.isMyDemandForecastProjection(t, orgName));
          }
        }
      }
      return false;
    }
  } // end SupplyTaskPredicate


  private class AssetOfTypePredicate implements DynamicUnaryPredicate {
    private Class supplyPGClass;

    public AssetOfTypePredicate(Class pgClass) {
      this.supplyPGClass = pgClass;
    } // constructor

    /**
     *  Predicate defining expandable Determine Reqs.
     **/
    public boolean execute(Object o) {
      if (o instanceof Asset) {
        Asset a = (Asset) o;
        if (a instanceof AggregateAsset) {
          a = ((AggregateAsset) a).getAsset();
        }
        return (a.searchForPropertyGroup(supplyPGClass) != null);
      } // if
      return false;
    } // execute
  } // DetReqPredicate

  /**
   * Filters out tasks that already have PEs -- fix for bug #1695
   * @param tasks - possibly from added list
   * @return Collection - tasks that have no PEs
   */
  protected Collection getTasksWithoutPEs(Collection tasks) {

    // I'm curious as to why we are using a hash set here?   -- llg
    Set tasksWithoutPEs = new HashSet();
    for (Iterator iter = tasks.iterator(); iter.hasNext();) {
      Task task = (Task) iter.next();

      if (task.getPlanElement() != null) {
        if (logger.isDebugEnabled()) {
          logger.debug(getMyOrganization() + " - found task that already had a p.e. attached? : " +
                       task.getUID() + " - so skipping it.");
        }
      } else {
        tasksWithoutPEs.add(task);
      }
    }

    return tasksWithoutPEs;
  }


  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    final String errorString = "DemandForecastPlugin requires 2 parameters, Supply Type and associated SupplyPGClass.  Additional parameter to change expander module.  e.g. org.cougaar.logistics.plugin.inventory.DemandForecastPlugin(" +
        SUPPLY_TYPE + "=BulkPOL, " + SUPPLY_PG_CLASS + "=FuelConsumerPG); Default package for SUPPLY_PG_CLASS is org.cougaar.logistics.ldm.asset.   If PG is not in this package use fully qualified name.";
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
    String supplyClassPGStr = (String) map.get(SUPPLY_PG_CLASS);
    if (((supplyType == null) ||
        (supplyClassPGStr == null) ||
        (supplyClassPGStr.trim().equals(""))
        && logger.isErrorEnabled())) {
      logger.error(errorString);
    } else {
      if (supplyClassPGStr.indexOf(".") == -1) {
        supplyClassPGStr = "org.cougaar.logistics.ldm.asset." + supplyClassPGStr;
      }
      try {
        supplyClassPG = Class.forName(supplyClassPGStr);
      } catch (Exception e) {
        logger.error("Problem loading SUPPLY_PG_CLASS-" + supplyClassPGStr +
                     "- exeception: " + e);
        logger.error(errorString);
        supplyClassPG = null;
      }
    }
    return map;
  }


  private void processDetReq(Collection addedDRs, Collection assets) {
    // with one oplan we should only have one DR task.
    Iterator drIt = addedDRs.iterator();
    if (drIt.hasNext()) {
      Task detReq = (Task) drIt.next();
      //synch on the detReq task so only one instance of this plugin
      // checks and creates a single agg task and then creates an
      // empty expansion (wf) for the maintain inventory for each item tasks
      synchronized (detReq) {
        determineRequirementsExpander.expandDetermineRequirements(detReq, assets);
        processedDetReq = true;
      }
    }
  }

  /**
   * This method processes the new GenerateProjection tasks.   Basically it
   * just adds this tasks unique PG and GP and updates the hash maps
   * with the new information.   We don't typically do the expansion here
   * because the newly added subscriptions (due to new PG) will be triggered
   * and caught by the checkAndProcessHashSubscriptions() method called later
   * in the same execute cycle, and expanded there.
   *
   * @param addedGPs - The collection of new GenerateProjections tasks
   */

  private HashSet processNewGenProjs(Collection addedGPs, TimeSpan timeSpan) {
    Iterator gpIt = addedGPs.iterator();
    HashSet justExpandedPGs = new HashSet();
    while (gpIt.hasNext()) {
      Task genProj = (Task) gpIt.next();
      Asset asset = genProj.getDirectObject();
      if (asset instanceof AggregateAsset) {
        asset = ((AggregateAsset) asset).getAsset();
      }
      PropertyGroup pg = asset.searchForPropertyGroup(supplyClassPG);

      pgToGPTaskHash.put(pg, genProj);
      justExpandedPGs.add(pg);

      /*
      * If there is a new pg (every new GP task has a new distinct MEI,
      * that has a distinct PG that has a distinct ConsumerPredicate),
      * add it to the hash tables, which will have the side effect
      * of subscribing the new ConsumerPredicate on the blackboard.
      * Later in the same execute cycle checkAndProcessHashSubscriptions()
      * will be called and fire for each new GP - because of the new ConsumerPredicate
      * firing.
      */
      if (!pgToPredsHash.containsKey(pg)) {
        addNewPG(pg);
        //We invoke GenProjections now because we do not expect any new subscriptions in the Hash table
        //to fire immediately.   They should be just the orgActivities subscription at this point.
        invokeGenProjectionsExp(pg, genProj,timeSpan);
      }
      // For each new GP task it actually has a new unique PGImpl, so
      // this code should never be called.   Especially if all the
      // hash tables are kept in line with whats on the blackboard.
      else {
	  //with new TaskScheduler this is no longer a surprise, but is expected behavior.
	  //logger.error("Surprise!!!! - unexpected expansion code firing in processNewGenProjs");
        invokeGenProjectionsExp(pg, genProj,timeSpan);
      }

//TODO: MWD Remove
      // Collection pgInputs = getSubscriptions(pg);
//       Oplan oplan = getOplan();
//       //TimeSpan projectSpan = opPlanningScheduler.getProjectDemandTimeSpan();
//       TimeSpan projectSpan = new ScheduleElementImpl(oplan.getCday(),
//                                                      oplan.getEndDay());
//       Schedule paramSchedule = getParameterSchedule(pg, pgInputs, projectSpan);
//       generateProjectionsExpander.expandGenerateProjections(genProj, paramSchedule, genProj.getDirectObject());

    }
    return justExpandedPGs;
  }

  /**
   *     This method keeps all the state hashTables in line with the GPTasks
   *  processed.   When GenerateProjection tasks are removed off the blackboard
   *  This method is called to take all related PG hash and subscription hashes
   *  up to date.
   *
   * @param removedGPs - The collection of GenerateProjection tasks just removed
   * from the blackboard.
   */

  private void processRemovedGenProjs(Collection removedGPs, TimeSpan timeSpan) {
    Iterator gpIt = removedGPs.iterator();
    while (gpIt.hasNext()) {
      Task genProj = (Task) gpIt.next();
      Asset asset = genProj.getDirectObject();
      if (asset instanceof AggregateAsset) {
        asset = ((AggregateAsset) asset).getAsset();
      }
      PropertyGroup pg = asset.searchForPropertyGroup(supplyClassPG);

      Collection preds = (Collection) pgToPredsHash.get(pg);
      pgToPredsHash.remove(pg);
      pgToGPTaskHash.remove(pg);

      if (preds != null) {
        Iterator predsIt = preds.iterator();
        while (predsIt.hasNext()) {
          UnaryPredicate pred = (UnaryPredicate) predsIt.next();
          IncrementalSubscription sub = (IncrementalSubscription) predToSubHash.get(pred);
          Collection subsPGs = (Collection) subToPGsHash.get(sub);
          subsPGs.remove(pg);
          if (subsPGs.isEmpty()) {
            blackboard.unsubscribe(sub);
            subToPGsHash.remove(sub);
            predToSubHash.remove(pred);
          }
        }
      }

    }
  }


  /**
   * This method goes through the subscriptions hash table and sees if any
   * of the subscriptions have changed.   For each subscription thats changed
   * its PGs are collected in a set (so it doesn't exist more than once).   The
   * resultant PG collection are set off to be processed (ie get the MEI and
   * GP task and re expand ).
   */

  protected void checkAndProcessHashSubscriptions(HashSet justExpandedPGs) {
    HashSet PGs = new HashSet();
    Iterator subIt = predToSubHash.entrySet().iterator();
    while (subIt.hasNext()) {
      Map.Entry entry = (Map.Entry) subIt.next();
      UnaryPredicate pred = (UnaryPredicate) entry.getKey();
      IncrementalSubscription sub = (IncrementalSubscription) entry.getValue();

      if ((!sub.getChangedCollection().isEmpty()) ||
          (!sub.getRemovedCollection().isEmpty()) ||
          (!sub.getAddedCollection().isEmpty())) {


        if (logger.isDebugEnabled()) {
          logger.debug("At " + getOrgName() + "-" + getSupplyType() +
                       "-Subscription w/predicate: " + pred + " has changed: Added: "
                       + sub.getAddedCollection().size() + " Removed: " +
                       +sub.getRemovedCollection().size() + " Changed: " +
                       +sub.getChangedCollection().size());
        }
        Collection subPGs = (Collection) subToPGsHash.get(sub);
        if (subPGs == null) {
          if ((sub != orgActivities) && (logger.isErrorEnabled())){
            String errString = "Subscription fired in the hash table at " + getOrgName() + ", but there are no PGs in the other hash tables that correspond. The Predicate is " + pred.getClass().getName() + ".";
            logger.error(errString);
          }
        } else {
          PGs.addAll(subPGs);
        }
      }
    }
    if (PGs.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("No subscription change,no PGs to notfiy! subToPGsHash is: " + subToPGsHash);
      }
    } else {
      if (logger.isDebugEnabled()) {
        //logger.debug("!!!Subscriptions changed got PGs to notfiy! Collection of PGs are " + PGs);
      }

      /*
      * Whats going on here is we're filtering out an PGs that have just been expanded in the same
      * execute cycle.   We were anticipating that this should never happen - that just expanded
      * GenerateProjection tasks pgs, their subscriptions should never fire.  In which case this PGs
      * variable will be empty or the filteredPGs is empty.   If it does ever happen, like a new
      * Consumer PG is introduced with new subscriptions that do fire immediately we will be covered. MWD.
      *
      */

      HashSet filteredPGs;
      if (justExpandedPGs.isEmpty()) {
        filteredPGs = PGs;
      } else {
        filteredPGs = new HashSet();
        Iterator pgIt = PGs.iterator();
        while (pgIt.hasNext()) {
          PropertyGroup pg = (PropertyGroup) pgIt.next();
          if (!(justExpandedPGs.contains(pg))) {
            filteredPGs.add(pg);
          }
        }
      }

      if(!filteredPGs.isEmpty()) {
       Oplan oplan = getOplan();
       TimeSpan projectSpan = new ScheduleElementImpl(oplan.getCday(),
                                                     oplan.getEndDay());
       processSubscriptionChangedPG(filteredPGs,projectSpan);
       genProjTaskScheduler.clearState();
      }
    }
  }


  /**
   * This method goes through the HashTables and gets all the current PGs
   * registered in the Hash Table and has all of them reprocess.   This occurs
   * the if we get the log Oplan for the first time, after possibly some of the
   * subscriptions in the hash tables have fired.
   *
   * TODO: Do we need this method anymore

   protected void processAllHashSubscriptions() {
   Set PGs = pgToPredsHash.keySet();
   if (PGs.isEmpty()) {
   if (logger.isDebugEnabled()) {
   logger.debug("No PGs in the hash tables: " + pgToPredsHash);
   }
   } else {
   if (logger.isDebugEnabled()) {
   //logger.debug("!!!Subscriptions changed got PGs to notfiy! Collection of PGs are " + PGs);
   logger.debug("DemandForecastPlugin::ProcessAllHashSubscriptions at " + myOrganization +
   "with Num PGs: " + PGs.size());
   }
   processSubscriptionChangedPG(PGs);
   }
   }

   **/

  //Invoke the BG and the genProjExpander if there are changes
  //in the OrgActivities or Removals of OrgActivities.
  private void processSubscriptionChangedPG(Collection PGs,TimeSpan projectSpan) {
    Iterator pgIt = PGs.iterator();
    while (pgIt.hasNext()) {
      //ConsumerPG pg = (ConsumerPG) pgIt.next();
      //Asset asset = pg.getMei();
      PropertyGroup pg = (PropertyGroup) pgIt.next();
      Task gp = (Task) pgToGPTaskHash.get(pg);
      if (gp != null) {
        PlanElement pe = gp.getPlanElement();
        if ((pe == null) ||
            (!(pe instanceof Disposition))) {
          logger.debug("******* invoking BG and GPE with changed Subscriptions **********");
          invokeGenProjectionsExp(pg, gp, projectSpan);
        }
      } else {
        if (logger.isErrorEnabled()) {
          logger.error("Property group :" + pg + " does not have an associated GenerateProjections task in the HashMap.");
        }
      }
    }
  }

  private void invokeGenProjectionsExp(PropertyGroup pg, Task genProj, TimeSpan projectSpan) {
    Collection pgInputs = getSubscriptions(pg);
    Schedule paramSchedule = getParameterSchedule(pg, pgInputs, projectSpan);
    generateProjectionsExpander.expandGenerateProjections(genProj, paramSchedule, genProj.getDirectObject(), projectSpan);
  }

  private void removeFromDetReq(Collection addedDRs, Collection removedAssets) {
    // with one oplan we should only have one DR for MI.
    Iterator drIt = addedDRs.iterator();
    if (drIt.hasNext()) {
      Task detReq = (Task) drIt.next();
      //synch on the detReq task so only one instance of this plugin
      // checks and creates a single agg task and then creates an
      // empty expansion (wf) for the maintain inventory for each item tasks
      synchronized (detReq) {
        determineRequirementsExpander.removeSubtasksFromDetermineRequirements(detReq, removedAssets);
        processedDetReq = true;
      }
    }
  }

  protected Collection getSubscriptions(PropertyGroup pg) {
    if (!pgToPredsHash.containsKey(pg)) {
      addNewPG(pg);
    }
    ArrayList pgInputs = new ArrayList();
    Collection preds = (Collection) pgToPredsHash.get(pg);


    Iterator predsIt = preds.iterator();
    while (predsIt.hasNext()) {
      UnaryPredicate pred = (UnaryPredicate) predsIt.next();
      ArrayList inputPair = new ArrayList();
      IncrementalSubscription sub = (IncrementalSubscription) predToSubHash.get(pred);
      inputPair.add(pred);
      inputPair.add(sub.getCollection());
      pgInputs.add(inputPair);
    }
    return pgInputs;
  }

  protected void addNewPG(PropertyGroup pg) {
    Collection preds = getPredicates(pg);
    Iterator predIt = preds.iterator();
    while (predIt.hasNext()) {
      UnaryPredicate pred = (UnaryPredicate) predIt.next();
      IncrementalSubscription sub = (IncrementalSubscription) predToSubHash.get(pred);
      if (sub == null) {
        sub = (IncrementalSubscription) blackboard.subscribe(pred);

        //MWD Defuse the subscriptions Additions collection.  A new PG with a new subscription
        //comes in only when a new GenerateProjections task comes in with a new MEI - this
        //is handled by the PG and GenerateProjectionExpander in the same section of code in
        //the execute() method when new GP tasks come in.   We don't want to expand the
        //new task a second time when the checkAndProcessHashSubscriptions() kicks off later
        //in the execute run.  So we disarm the added collection here when it is first added
        //to the black board.
        //sub.getAddedCollection().clear();

        predToSubHash.put(pred, sub);
      }
      Collection PGs = (Collection) subToPGsHash.get(sub);
      if (PGs == null) {
        PGs = new ArrayList();
        subToPGsHash.put(sub, PGs);
      }
      if (!PGs.contains(pg)) {
        PGs.add(pg);
      }
    }
    Collection hashPreds = (Collection) pgToPredsHash.get(pg);
    if (hashPreds == null) {
      pgToPredsHash.put(pg, preds);
    }
  }

  private void rehydrateHashMaps() {
    Iterator gpIt = genProjTaskScheduler.getAllTasks();
    while (gpIt.hasNext()) {
      Task gpTask = (Task) gpIt.next();
      Asset mei = gpTask.getDirectObject();
      if (mei instanceof AggregateAsset) {
        mei = ((AggregateAsset) mei).getAsset();
      }
      PropertyGroup pg = mei.searchForPropertyGroup(supplyClassPG);
      addNewPG(pg);
      pgToGPTaskHash.put(pg, gpTask);
    }
  }


  private String getClusterSuffix(String clusterId) {
    String result = null;
    int i = clusterId.lastIndexOf("-");
    if (i == -1) {
      result = clusterId;
    } else {
      result = clusterId.substring(i + 1);
    }
    return result;
  }

  /**
   * Creates an instance of an DetReqExpanderIfc by
   * searching plugin parameters for REQ_EXPANDER argument.
   * In the absence of an REQ_EXPANDER argument, a default is used:
   * org.cougaar.logistics.plugin.projection.DetermineRequirementsExpander
   * @return {@link DetReqExpanderIfc}
   **/
  private DetReqExpanderIfc getDetermineRequirementsExpanderModule() {
    String expanderClass = (String) pluginParams.get(REQ_EXPANDER);
    if (expanderClass != null) {
      try {
        Class[] paramTypes = {this.getClass()};
        Object[] initArgs = {this};
        Class cls = Class.forName(expanderClass);
        Constructor constructor = cls.getConstructor(paramTypes);
        DetReqExpanderIfc expander = (DetReqExpanderIfc) constructor.newInstance(initArgs);
        logger.info("Using RequirementsExpander " + expanderClass);
        return expander;
      } catch (Exception e) {
        logger.error(e + " Unable to create RequirementsExpander instance of " + expanderClass + ". " +
                     "Loading default org.cougaar.logistics.plugin.projection.DetermineRequirementsExpander");
      }
    }
    return new DetermineRequirementsExpander(this);
  }


  /**
   * Creates an instance of an GenProjExpanderIfc by
   * searching plugin parameters for PROJ_EXPANDER argument.
   * In the absence of an PROJ_EXPANDER argument, a default is used:
   * org.cougaar.logistics.plugin.projection.DetermineRequirementsExpander
   * @return {@link GenProjExpanderIfc}
   **/
  private GenProjExpanderIfc getGenerateProjectionsExpanderModule() {
    String expanderClass = (String) pluginParams.get(PROJ_EXPANDER);
    if (expanderClass != null) {
      try {
        Class[] paramTypes = {this.getClass()};
        Object[] initArgs = {this};
        Class cls = Class.forName(expanderClass);
        Constructor constructor = cls.getConstructor(paramTypes);
        GenProjExpanderIfc expander = (GenProjExpanderIfc) constructor.newInstance(initArgs);
        logger.info("Using ProjectionsExpander " + expanderClass);
        return expander;
      } catch (Exception e) {
        logger.error(e + " Unable to create ProjectionsExpander instance of " + expanderClass + ". " +
                     "Loading default org.cougaar.logistics.plugin.projections.GenerateProjectionsExpander");
      }
    }
    return new GenerateProjectionsExpander(this);
  }

  public void publishAddToExpansion(Task parent, Task subtask) {
    //attach the subtask to its parent and the parent's workflow
    PlanElement pe = parent.getPlanElement();
    Expansion expansion;
    NewWorkflow wf;
    ((NewTask) subtask).setParentTask(parent);
    ((NewTask) subtask).setPlan(parent.getPlan());
    // Task has not been expanded, create an expansion
    if (pe == null) {
      PlanningFactory factory = getPlanningFactory();
      // Create workflow
      wf = factory.newWorkflow();
      wf.setParentTask(parent);
      wf.setIsPropagatingToSubtasks(true);
      wf.addTask(subtask);
      ((NewTask) subtask).setWorkflow(wf);
      // Build Expansion
      expansion = factory.createExpansion(parent.getPlan(), parent, wf, null);
      // Publish Expansion
      publishAdd(expansion);
    }
    // Task already has expansion, add task to the workflow and publish the change
    else if (pe instanceof Expansion) {
      expansion = (Expansion) pe;
      wf = (NewWorkflow) expansion.getWorkflow();
      wf.addTask(subtask);
      ((NewTask) subtask).setWorkflow(wf);
      publishChange(expansion);
    } else {
      if (logger.isErrorEnabled()) {
        logger.error("publishAddToExpansion: problem pe not Expansion? " + pe);
      }
    }

    // Publish new task
    publishAdd(subtask);
  }


  private Organization getMyOrganization(Enumeration orgs) {
    Organization myOrg = null;
    // look for this organization
    if (orgs.hasMoreElements()) {
      myOrg = (Organization) orgs.nextElement();
    }
    return myOrg;
  }

  public MessageAddress getClusterId() {
    return getAgentIdentifier();
  }

  public Oplan getOplan() {
    Iterator oplanIt = oplanSubscription.iterator();
    if (oplanIt.hasNext()) {
      return (Oplan) oplanIt.next();
    }
    return null;
  }


  public String getOrgName() {
    if (myOrgName == null) {
      myOrgName = getMyOrganization().getItemIdentificationPG().getItemIdentification();
    }
    return myOrgName;
  }

  public Class getSupplyClassPG() {
    return supplyClassPG;
  }

  // get the first day in theater
  public long getLogOPlanStartTime() {
    return logOPlan.getStartTime();
  }

  // get the last day in theater
  public long getLogOPlanEndTime() {
    return logOPlan.getEndTime();
  }

  public Collection getPredicates(PropertyGroup pg) {
    Collection preds = null;
    Class parameters[] = {};
    Object arguments[] = {};
    Method m = null;
    try {
      m = supplyClassPG.getMethod("getPredicates", parameters);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    try {
      preds = (Collection) m.invoke(pg, arguments);
      return preds;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return new ArrayList();
  }


  public Schedule getParameterSchedule(PropertyGroup pg,
                                       Collection pgInputs,
                                       TimeSpan projectSpan) {
    Schedule paramSchedule = null;

    if(projectSpan.getEndTime() <= projectSpan.getStartTime()) {
	logger.error("Was going to call getParameterSchedule, but the projectSpan spans a zero time span!");
    }
    else {
	Class parameters[] = {Collection.class, TimeSpan.class};
	Object arguments[] = {pgInputs, projectSpan};
	Method m = null;
	try {
	    if (!supplyClassPG.isInstance(pg)) {
		throw new IllegalArgumentException("PG is not an instanceof of "+supplyClassPG+": "+pg);
	    }
	    m = supplyClassPG.getMethod("getParameterSchedule", parameters);
	    paramSchedule = (Schedule) m.invoke(pg, arguments);
	    return paramSchedule;
	} catch(Exception e) {
	    e.printStackTrace();
	}
	/*** TODO: MWD take out extra exceptions
	} catch (NoSuchMethodException e) {
	    e.printStackTrace();
	} catch (SecurityException e) {
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	}
	**/
    }
    return new ScheduleImpl();
  }

  //temp method for getting the MEI
  public Asset getMEI(PropertyGroup pg) {
    Asset mei = null;
    Class parameters[] = {};
    Object arguments[] = {};
    Method m = null;
    try {
      m = supplyClassPG.getMethod("getMei", parameters);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    try {
      mei = (Asset) m.invoke(pg, arguments);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return mei;
  }


  public Collection filter(UnaryPredicate predicate) {
    return Filters.filter(projectSupplySubscription, predicate);
  }

  /**
   * Returns a subset of project supply tasks for a given asset, for a given parent generate
   * projection task's UID.
   * @param parentTask the generate projects tasks that was expanded
   * @return all project supply tasks of the parent generate projections task
   */
  public Collection projectSupplySet(final Task parentTask, final Asset consumedItem) {
    return filter(new UnaryPredicate() {
      public boolean execute(Object o) {
        Task t = (Task) o;
        if (t.getParentTaskUID().equals(parentTask.getUID())) {
          Asset a = t.getDirectObject();
          return a.equals(consumedItem);
        }
        return false;
      }
    });
  }


  /**
   Self-Test
   **/
  public void automatedSelfTest() {
    if (logger.isErrorEnabled()) {
      if (supplyType == null) logger.error("No SupplyType Plugin parameter.");
      if (myOrganization == null)
        logger.error("Missing myorganization");
    }
  }
}


