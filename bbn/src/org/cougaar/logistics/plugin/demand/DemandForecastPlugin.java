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
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.oplan.Oplan;
//import org.cougaar.logistics.ldm.asset.ConsumerPG;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.inventory.LogisticsOPlan;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.utils.OrgActivityPred;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
  private HashMap subToPGsHash;
  private HashMap predToSubHash;

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
        (orgActivities.isEmpty()) ||
        (detReqSubscription.isEmpty())) {
      processedDetReq = false;
      return;
    }

    if (!detReqSubscription.isEmpty()) {
      Iterator detReqIt = detReqSubscription.iterator();
      Task detReqTask = (Task) detReqIt.next();
      processedDetReq = (!(detReqTask.getPlanElement() == null));
    }

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

    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
      if(myOrganization != null) {
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

    if (genProjSubscription.hasChanged()) {
      if (!genProjSubscription.getAddedCollection().isEmpty()) {
        processNewGenProjs(genProjSubscription.getAddedCollection());
      }
    }

    //if an orgActivity changes, is removed, or is added,  replan by calling the BGs
    //and the generateprojections expander.  Note the added case is only used when
    // the detreqs task has already been expanded. If the detreqs has not been expanded
    //and we get an added orgAct we process the detreq above.
    if ((!orgActivities.getChangedCollection().isEmpty()) ||
        (!orgActivities.getRemovedCollection().isEmpty()) ||
        ((!orgActivities.getAddedCollection().isEmpty()) && processedDetReq)) {
      //processOrgActChanges((Collection)subToPGsHash.get(orgActivities));
      Collection PGs = (Collection)subToPGsHash.get(orgActivities);
      if (PGs == null || PGs.isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug("ORG ACT change with no PGs to notfiy! subToPGsHash is: " +
                             subToPGsHash);
        }
      } else {
        System.out.println("ORG ACT got PGs to notfiy! subToPGsHash is: " +
                           subToPGsHash);
        processOrgActChanges(PGs);
      }
    } 

    // get the Logistics OPlan (our homegrown version with specific dates).
    if ((logOPlan == null) || logisticsOPlanSubscription.hasChanged()) {
      Iterator opIt = logisticsOPlanSubscription.iterator();
      if (opIt.hasNext()) {
        //we only expect to have one
        logOPlan = (LogisticsOPlan) opIt.next();
      }
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

  }


  private IncrementalSubscription orgActivities;
  private IncrementalSubscription oplanSubscription;
  private IncrementalSubscription detReqSubscription;
  private IncrementalSubscription detReqPESubscription;
  private IncrementalSubscription genProjSubscription;
  private IncrementalSubscription genProjPESubscription;
  private IncrementalSubscription projectSupplySubscription;
  private IncrementalSubscription logisticsOPlanSubscription;

  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;

  /** Subscription for all assets with plugin parameters PG class attached to it **/
  private IncrementalSubscription assetsWithPGSubscription;

  protected void setupSubscriptions() {

    selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);

    UnaryPredicate orgActivityPred = new OrgActivityPred();
    orgActivities = (IncrementalSubscription) blackboard.subscribe(orgActivityPred);
    predToSubHash.put(orgActivityPred, orgActivities);

    oplanSubscription = (IncrementalSubscription) blackboard.subscribe(oplanPredicate);

    detReqSubscription = (IncrementalSubscription) blackboard.subscribe(new DetReqPredicate(supplyType, taskUtils));
    detReqPESubscription = (IncrementalSubscription) blackboard.subscribe(new DetReqPEPredicate(supplyType, taskUtils));

    genProjSubscription = null;
    assetsWithPGSubscription = null;

    if (supplyClassPG != null) {
      genProjSubscription = (IncrementalSubscription) blackboard.subscribe(new GenProjPredicate(supplyType, taskUtils));
      assetsWithPGSubscription = (IncrementalSubscription) 
        getBlackboardService().subscribe(new AssetOfTypePredicate(supplyClassPG));
    }

    genProjPESubscription = (IncrementalSubscription) 
      blackboard.subscribe(new GenProjPEPredicate(supplyType, taskUtils));

    logisticsOPlanSubscription = (IncrementalSubscription) blackboard.subscribe(new LogisticsOPlanPredicate());
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
        Task t = ((PlanElement)o).getTask();
        if (t.getVerb().equals(Constants.Verb.DETERMINEREQUIREMENTS)) {
          return taskUtils.isTaskOfType(t, supplyType);
        } // if
      } // if
      return false;
    } // execute
  } // DetReqPEPredicate

  /** Predicate defining expandable Determine Reqs. **/
  private static class GenProjPredicate implements UnaryPredicate {
    private String supplyType;
    private TaskUtils taskUtils;
    public GenProjPredicate(String type, TaskUtils utils) {
      this.supplyType = type;
      this.taskUtils = utils;
    } // constructor

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.GENERATEPROJECTIONS)) {
          return taskUtils.isTaskOfTypeString(t, supplyType);
        }
      }
      return false;
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
        Task t = ((PlanElement)o).getTask();
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
    public ProjectSupplyPredicate(String type, String myOrgName,TaskUtils utils) {
      this.supplyType = type;
      this.orgName = myOrgName;
      this.taskUtils = utils;
    } 
    
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if(taskUtils.isTaskOfTypeString(t, supplyType)) {
            return (taskUtils.isMyDemandForecastProjection(t,orgName));
          }
        }
      }
      return false;
    } 
  } // end SupplyTaskPredicate


  private static class AssetOfTypePredicate implements UnaryPredicate {
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

  /** Selects the LogisticsOPlan objects **/
  private static class LogisticsOPlanPredicate implements UnaryPredicate{
    public boolean execute(Object o) {
      return o instanceof LogisticsOPlan;
    }
  }

  /**
   * Filters out tasks that already have PEs -- fix for bug #1695
   * @param tasks - possibly from added list
   * @return Collection - tasks that have no PEs
   */
  protected Collection getTasksWithoutPEs(Collection tasks) {
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
      if(supplyClassPGStr.indexOf(".") == -1){
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


  private void processNewGenProjs(Collection addedGPs) {
    Iterator gpIt = addedGPs.iterator();
    while (gpIt.hasNext()) {
      Task genProj = (Task) gpIt.next();
      Asset asset = genProj.getDirectObject();
      if (asset instanceof AggregateAsset) {
        asset = ((AggregateAsset) asset).getAsset();
      }
      PropertyGroup pg = asset.searchForPropertyGroup(supplyClassPG);
      invokeGenProjectionsExp(pg, genProj);
      // Collection pgInputs = getSubscriptions(pg);
//       Oplan oplan = getOplan();
//       //TimeSpan projectSpan = opPlanningScheduler.getProjectDemandTimeSpan();
//       TimeSpan projectSpan = new ScheduleElementImpl(oplan.getCday(),
//                                                      oplan.getEndDay());
//       Schedule paramSchedule = getParameterSchedule(pg, pgInputs, projectSpan);
//       generateProjectionsExpander.expandGenerateProjections(genProj, paramSchedule, genProj.getDirectObject());

    }
  }

  //Invoke the BG and the genProjExpander if there are changes
  //in the OrgActivities or Removals of OrgActivities.
  private void processOrgActChanges(Collection PGs) {
    Iterator pgIt = PGs.iterator();
    while (pgIt.hasNext()) {
      //ConsumerPG pg = (ConsumerPG) pgIt.next();
      //Asset asset = pg.getMei();
      PropertyGroup pg = (PropertyGroup) pgIt.next();
      Asset asset = getMEI(pg);
      if (asset instanceof AggregateAsset) {
        asset = ((AggregateAsset) asset).getAsset();
      }
      Iterator gpIt = genProjSubscription.iterator();
      while (gpIt.hasNext()) {
        Task gp = (Task) gpIt.next();
        Asset directObj = gp.getDirectObject();
        if (directObj instanceof AggregateAsset) {
          directObj = ((AggregateAsset)directObj).getAsset();
        }
        if (directObj.equals(asset)) {
          System.out.println("******* invoking BG and GPE with changed OrgACT **********");
          invokeGenProjectionsExp(pg, gp);
          break;
        } 
      }
    }
  }

  private void invokeGenProjectionsExp(PropertyGroup pg, Task genProj) {
    Collection pgInputs = getSubscriptions(pg);
    Oplan oplan = getOplan();
    // placeholder for task scheduler
    //TimeSpan projectSpan = opPlanningScheduler.getProjectDemandTimeSpan();
    TimeSpan projectSpan = new ScheduleElementImpl(oplan.getCday(),
                                                   oplan.getEndDay());
    Schedule paramSchedule = getParameterSchedule(pg, pgInputs, projectSpan);
    generateProjectionsExpander.expandGenerateProjections(genProj, paramSchedule, genProj.getDirectObject());
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
      inputPair.add(sub);
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

  private Role getRole(String supply_type) {
    if (supply_type.equals("Ammunition"))
      return Constants.Role.AMMUNITIONPROVIDER;
    if (supply_type.equals("BulkPOL"))
      return Constants.Role.FUELSUPPLYPROVIDER;
    if (supply_type.equals("Consumable"))
      return Constants.Role.SPAREPARTSPROVIDER;
    if (supply_type.equals("PackagedPOL"))
      return Constants.Role.PACKAGEDPOLSUPPLYPROVIDER;
    if (supply_type.equals("Subsistence"))
      return Constants.Role.SUBSISTENCESUPPLYPROVIDER;
    if (logger.isErrorEnabled()) {
      logger.error("Unsupported Supply Type");
    }
    return null;
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
    Class parameters[] = {Collection.class, TimeSpan.class};
    Object arguments[] = {pgInputs, projectSpan};
    Method m = null;
    try {
      m = supplyClassPG.getMethod("getParameterSchedule", parameters);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    try {
      paramSchedule = (Schedule) m.invoke(pg, arguments);
      return paramSchedule;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
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


