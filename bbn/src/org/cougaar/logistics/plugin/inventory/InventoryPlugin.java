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

package org.cougaar.logistics.plugin.inventory;

import java.lang.reflect.Constructor;
import java.util.*;

import org.cougaar.core.mts.*;
import org.cougaar.glm.plugins.FileUtils;

import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.ScheduledContentPG;
import org.cougaar.glm.ldm.asset.NewScheduledContentPG;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.service.DomainService;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Enumerator;
import org.cougaar.util.TimeSpan;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.logistics.plugin.utils.TaskScheduler;
import org.cougaar.logistics.plugin.utils.TaskSchedulingPolicy;
import org.cougaar.logistics.plugin.utils.QuiescenceAccumulator;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.planning.service.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OperatingMode;
import org.cougaar.core.adaptivity.OperatingModeImpl;

import org.cougaar.logistics.plugin.utils.ScheduleUtils;

/** The InventoryPlugin is the Glue of inventory management.
 *  It handles all blackboard services for its modules,
 *  facilitates inter-module communication and manages the
 *  subscriptions.  The InventoryPlugin also creates inventories.
 *  All modules are called from the InventoryPlugin.
 **/

public class InventoryPlugin extends ComponentPlugin
    implements UtilsProvider {

  private boolean initialized = false;
//  private boolean firstTimeThrough = true;
  private DomainService domainService;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils AssetUtils;
  protected ScheduleUtils scheduleUtils;
  private HashMap pluginParams;
  protected HashMap inventoryHash;
  protected HashMap inventoryInitHash;
  protected HashSet touchedInventories;
  // private HashSet backwardFlowInventories;  // ### Captures Inventories with unchanged demand
  private boolean touchedProjections;
  private boolean touchedChangedProjections = false;
  protected String supplyType;
  private String inventoryFile;
//   private boolean fillToCapacity; Will be added bug #1482
//   private boolean maintainAtCapacity; Will be added bug #1482
  protected DetReqAggHandler detReqHandler;
  protected Organization myOrganization;
  private String myOrgName;
  private ExpanderModule supplyExpander;
  protected AllocatorModule externalAllocator;
  private RefillGeneratorModule refillGenerator;
  private RefillProjectionGeneratorModule refillProjGenerator;
  private ComparatorModule refillComparator;
  private AllocationAssessor allocationAssessor;
  protected long startTime;
  private long cycleStamp;
  protected boolean logToCSV = false;
  protected transient ArrayList newRefills = new ArrayList();
  private boolean rehydrateInvs = false;
  private boolean OMChange = false;
  private long prevLevel6;
  private boolean turnOnTaskSched=false;
  private int prepoArrivalOffset=3;

  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String INVENTORY_FILE = "INVENTORY_FILE";
  public final String ENABLE_CSV_LOGGING = "ENABLE_CSV_LOGGING";
  public final String PREPO_ARRIVAL_OFFSET = "PREPO_ARRIVAL_OFFSET";
  public final String TASK_SCHEDULER_ON = "TASK_SCHEDULER_ON";

  // as a default make the max the end of the oplan (225)
  public final Integer LEVEL_2_MIN = new Integer(40); // later, these should be parameters to plugin...
  public final Integer LEVEL_2_MAX = new Integer(225);
  public final Integer LEVEL_6_MIN = new Integer(20);
  public final Integer LEVEL_6_MAX = new Integer(225);
  // then default to the end of the oplan (max)
  public final String LEVEL_2_TIME_HORIZON = "Level2TimeHorizon";
  public final Integer LEVEL_2_TIME_HORIZON_DEFAULT = LEVEL_2_MAX;
  public final String LEVEL_6_TIME_HORIZON = "Level6TimeHorizon";
  public final Integer LEVEL_6_TIME_HORIZON_DEFAULT = LEVEL_6_MAX;

  // OPlan variable
  protected LogisticsOPlan logOPlan = null;

  // Policy variables
  private InventoryPolicy inventoryPolicy = null;
  protected int criticalLevel = 3;
  protected int reorderPeriod = 3;
  protected long bucketSize = TimeUtils.MSEC_PER_DAY;
  protected boolean fillToCapacity = false;

  public void load() {
    super.load();
    logger = getLoggingService(this);
    timeUtils = new TimeUtils(this);
    AssetUtils = new AssetUtils(this);
    taskUtils = new TaskUtils(this);
    scheduleUtils = new ScheduleUtils(this);
    detReqHandler = new DetReqAggHandler(this);
    // readParameters() initializes supplyType and inventoryFile
    pluginParams = readParameters();
    supplyExpander = getExpanderModule();
    externalAllocator = getAllocatorModule();
    refillGenerator = getRefillGeneratorModule();
    refillProjGenerator = getRefillProjectionGeneratorModule();
    refillComparator = getComparatorModule();
    allocationAssessor = new AllocationAssessor(this, getRole(supplyType));
    inventoryHash = new HashMap();
    inventoryInitHash = new HashMap();
    touchedInventories = new HashSet();
    //backwardFlowInventories = new HashSet();
    touchedProjections = false;
    startTime = currentTimeMillis();


    domainService = (DomainService)
        getServiceBroker().getService(this,
                                      DomainService.class,
                                      new ServiceRevokedListener() {
                                        public void serviceRevoked(ServiceRevokedEvent re) {
                                          if (DomainService.class.equals(re.getService()))
                                            domainService = null;
                                        }
                                      });
    //   System.out.println("\n LOADING InventoryPlugin of type: " + supplyType +
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

  private String getInventoryFileName() {
    return inventoryFile;
  }

  public Organization getMyOrganization() {
    return myOrganization;
  }

  public String getOrgName() {
      return myOrgName;
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
    LoggingService ls = (LoggingService) 
      getServiceBroker().getService(requestor,
				    LoggingService.class,
				    null);
    return LoggingServiceWithPrefix.add(ls, getAgentIdentifier() + ": ");
  }


  protected void execute() {
    //clear our new refill list
    newRefills.clear();

    // need to call these at beginning of execute cycle
    supplyTaskScheduler.initForExecuteCycle();
    projectionTaskScheduler.initForExecuteCycle();

    // if the OM changed and the window is further out then before
    // then mark the flag true so we process previously ignored tasks
    // and allocation results.  If it went down - don't undo work
    if (!Level6OMSubscription.getChangedCollection().isEmpty()) {
      long currentLevel6 = getEndOfLevelSix();
      if (logger.isInfoEnabled()) {
        logger.info("Inv Mgr got changed OM ... new end of level 6 window is: " +
                    currentLevel6 + " in agent: " + getAgentIdentifier() +
                    " supply type: " + getSupplyType());
      }
      if (currentLevel6 > prevLevel6) {
        OMChange = true;
      }
      //reset the previous level 6 to the current
      prevLevel6 = currentLevel6;
    }

    if (inventoryPolicy == null) {
      updateInventoryPolicy(inventoryPolicySubscription);
    }
    updateInventoryPolicy(inventoryPolicySubscription.getChangedCollection());
    processDetReq(detReqSubscription.getAddedCollection());
    cycleStamp = (new Date()).getTime();

    if (inventoryPolicy == null) {
      if (logger.isInfoEnabled()) {
        logger.info("\n InventoryPlugin " + supplyType +
                    " not ready to process tasks yet." +
                    " my inv policy is: " + inventoryPolicy + " in " + getMyOrganization());
      }
      return;
    }

    if (myOrganization == null) {
      myOrganization = getMyOrganization(selfOrganizations.elements());
    }

    if (myOrganization == null) {
      if (logger.isInfoEnabled()) {
        logger.info("\n InventoryPlugin " + supplyType +
                    " not ready to process tasks yet." +
                    " my org is: " + myOrganization);
      }
      return;
    }

    if (!initialized) {
      myOrgName = myOrganization.getItemIdentificationPG().getItemIdentification();
      inventoryFile = getInventoryFile(supplyType);
      getInventoryData();
      initialized = true;
    }

    if ((logOPlan == null) || logisticsOPlanSubscription.hasChanged()) {
      Collection c = logisticsOPlanSubscription.getCollection();
      for (Iterator i = c.iterator(); i.hasNext();) {
	  logOPlan = (LogisticsOPlan) i.next();
          //        System.out.println("logOplan in :" + getAgentIdentifier().toString() + 
          //                   " is: " + logOPlan);
	  resetLogOPlanForInventories();
	  break;
      }
    }

    if ((detReqHandler.getDetermineRequirementsTask(aggMILSubscription) != null) &&
        (logOPlan != null)) {
      if (rehydrateInvs) {
        addRehydratedInventories(blackboard.query(new InventoryPredicate(supplyType)));
        rehydrateInvs = false;
      }
      boolean touchedRemovedProjections =
          supplyExpander.handleRemovedProjections(projectWithdrawTaskSubscription.getRemovedCollection());
      supplyExpander.handleRemovedRequisitions(withdrawTaskSubscription.getRemovedCollection());
      // The following is here because the above lies about what it does
      supplyExpander.handleRemovedRealRequisitions(supplyTaskScheduler.getRemovedCollection());
      handleRemovedRefills(refillSubscription.getRemovedCollection());

      // If its the first time we've gotten this far we now have our policy, org and others.
      // However, we may have missed some tasks on the added list in previous executes.
      // If its the first time through use the underlying subscription collection
      // instead of the added list.
//      if (firstTimeThrough) {
//         Enumeration newReqs = supplyTaskSubscription.elements();
//         ArrayList newReqsCollection = new ArrayList();
//         while (newReqs.hasMoreElements()) {
//           newReqsCollection.add(newReqs.nextElement());
//         }
//         expandIncomingRequisitions(getTasksWithoutPEs(newReqsCollection));
//         Enumeration newProjReqs = projectionTaskSubscription.elements();
//         ArrayList newProjReqsCollection = new ArrayList();
//         while (newProjReqs.hasMoreElements()) {
//           newProjReqsCollection.add(newProjReqs.nextElement());
//         }
//         touchedProjections = expandIncomingProjections(getTasksWithoutPEs(newProjReqsCollection));
//        expandIncomingRequisitions(getTasksWithoutPEs(supplyTaskSubscription));
//        touchedProjections = expandIncomingProjections(getTasksWithoutPEs(projectionTaskSubscription));
//        firstTimeThrough = false;
//      } else {
        Collection addedSupply = supplyTaskScheduler.getAddedCollection();
        TimeSpan timeSpan = null;
        if (turnOnTaskSched) {
          timeSpan = supplyTaskScheduler.getCurrentTimeSpan();
        } else {
          timeSpan = new ScheduleElementImpl(getLogOPlanStartTime(),
                                             getLogOPlanEndTime());
        }
        if (!addedSupply.isEmpty()) {
          expandIncomingRequisitions(getTasksWithoutPEs(addedSupply)); // fix for bug #1695
        }
        Collection changedSupply = supplyTaskScheduler.getChangedCollection();
        if (!changedSupply.isEmpty()) {
          supplyExpander.updateChangedRequisitions(changedSupply);
        }
        supplyTaskScheduler.finishedExecuteCycle();

        Collection addedProjections = projectionTaskScheduler.getAddedCollection();
        TimeSpan timeSpan2 = null;
        if (turnOnTaskSched) {
          timeSpan2 = projectionTaskScheduler.getCurrentTimeSpan();
        } else {
          timeSpan2 = new ScheduleElementImpl(getLogOPlanStartTime(),
                                              getLogOPlanEndTime());
        }
        if (!addedProjections.isEmpty()) {
          // getTasksWithoutPEs is fix for bug #1695
          touchedProjections = expandIncomingProjections(getTasksWithoutPEs(addedProjections));
        }
        Collection changedProjections = projectionTaskScheduler.getChangedCollection();
        if (!changedProjections.isEmpty()) {
          supplyExpander.updateChangedProjections(changedProjections);
          touchedChangedProjections = true;
// System.out.println("Touched changed projections in " + getAgentIdentifier() +
//                                " type is" + getSupplyType());
        }
        projectionTaskScheduler.finishedExecuteCycle();

	// Rescind tasks that no longer have a provider
	// and
        // Allocate any refill tasks from previous executions that were not allocated to providers
        // but only if we are not about to rip out previous work we have done
        if (didOrgRelationshipsChange()) {
//        logger.warn("ORG RELATIONSHIPS CHANGED");
//	  System.out.println("SDSD myorg: " + myOrganization + " supply type:" + 
//			       supplyType + " role: " + getRole(supplyType) + "\n");
	  HashMap providers = getProvidersAndEndDates();
	  Collection unprovidedTasks = getUnprovidedTasks(refillSubscription, 
							    Constants.Verb.Supply,
							    providers);
	  if (!unprovidedTasks.isEmpty()){
	      logger.warn("Trying to rescind unprovided supply refill tasks...");
	      rescindTaskAllocations(unprovidedTasks);
	      externalAllocator.allocateRefillTasks(unprovidedTasks);
	  }
	  unprovidedTasks = getUnprovidedTasks(refillSubscription, 
						 Constants.Verb.ProjectSupply,
						 providers);
	  if (!unprovidedTasks.isEmpty()){
	      logger.warn("Trying to rescind unprovided projection refill tasks...");
	      rescindTaskAllocations(unprovidedTasks);
	      externalAllocator.allocateRefillTasks(unprovidedTasks);
	  }

          Collection unallocRefill = null;
          if (addedSupply.isEmpty() && changedSupply.isEmpty()) {
            unallocRefill = sortIncomingSupplyTasks(getTaskUtils().getUnallocatedTasks(refillSubscription, 
										       Constants.Verb.Supply));
            if (!unallocRefill.isEmpty()){
              logger.warn("TRYING TO ALLOCATE SUPPLY REFILL TASKS...");
              externalAllocator.allocateRefillTasks(unallocRefill);
            }
          }
          if (addedProjections.isEmpty() && changedProjections.isEmpty()) {
            unallocRefill = sortIncomingSupplyTasks(getTaskUtils().getUnallocatedTasks(refillSubscription,
										       Constants.Verb.ProjectSupply));
            if (!unallocRefill.isEmpty()) {
              logger.warn("TRYING TO ALLOCATE PROJECTION REFILL TASKS...");
              externalAllocator.allocateRefillTasks(unallocRefill);
            }
          }
        }
//      }

      // call the Refill Generators if we have new demand
      if (!getTouchedInventories().isEmpty()) {
        // update the inventory customer hash tables when we have new demand
        rebuildPGCustomerHash();
        //check to see if we have new projections
        if (touchedProjections || touchedRemovedProjections || touchedChangedProjections) {
          //check to see if the OM changed.  If it did process all inventories
          //since we probably ignored some demand tasks before the change
          if (OMChange) {
            refillProjGenerator.calculateRefillProjections(getInventories(),
                                                           criticalLevel,
                                                           getEndOfLevelSix(),
                                                           getEndOfLevelTwo(),
                                                           refillComparator);
          } else {
            refillProjGenerator.calculateRefillProjections(getTouchedInventories(),
                                                           criticalLevel,
                                                           getEndOfLevelSix(),
                                                           getEndOfLevelTwo(),
                                                           refillComparator);
          }
        }
        refillGenerator.calculateRefills(getTouchedInventories(), refillComparator);
        externalAllocator.allocateRefillTasks(newRefills);

        //we might get new demand where we don't need to generate any new refills
        // such as small demand from the stimulator servlet - when this happens we
        // need to kick the allocation assessor to allocate the withdraws
        allocationAssessor.reconcileInventoryLevels(getActionableInventories());

      }
      //        externalAllocator.updateAllocationResult(getActionableRefillAllocations());
      //        allocationAssessor.reconcileInventoryLevels(backwardFlowInventories);

      // if we are in downward flow ONLY check the withdraw expansion results
      // note we may go through the whole list multiple times - but this seems like the
      // simplest fix to get rid of places where we miss change reports because the AA
      // compares previous results to new ones and leaves the old ones if they are equal.
      // note that the updates only occur if the reported result is not equal to the estimated
      // so we will not be waking up the whole chain by checking these more than once.
      HashSet backwardFlowTouched = null;
      if (getTouchedInventories().isEmpty()) {
        supplyExpander.updateAllocationResult(expansionSubscription);
        backwardFlowTouched =
            externalAllocator.updateAllocationResult(refillAllocationSubscription);
        // if the OM changed, process ALL inventories for demand projections and
        // allocation results since some results were likely ignored before
        // the OM level 6 window changed.
        if (OMChange) {
          refillProjGenerator.calculateRefillProjections(getInventories(),
                                                         criticalLevel,
                                                         getEndOfLevelSix(),
                                                         getEndOfLevelTwo(),
                                                         refillComparator);
          externalAllocator.allocateRefillTasks(newRefills);
          allocationAssessor.reconcileInventoryLevels(getInventories());
        } else {
          allocationAssessor.reconcileInventoryLevels(backwardFlowTouched);
        }
      } else {
        // if the we are not in backwards flow but the OM changed
        // process ARs anyway because we may not get woken up again to
        // process them if they have all come in already
        if (OMChange) {
          allocationAssessor.reconcileInventoryLevels(getInventories());
        }
      }

      // update the Maintain Inventory Expansion results
      PluginHelper.updateAllocationResult(MIExpansionSubscription);
      PluginHelper.updateAllocationResult(MITopExpansionSubscription);
      PluginHelper.updateAllocationResult(DetReqInvExpansionSubscription);

      if (backwardFlowTouched != null) {
        takeInventorySnapshot(backwardFlowTouched);
      }
      takeInventorySnapshot(getTouchedInventories());

      // touchedInventories should not be cleared until the end of transaction
      touchedInventories.clear();
      //backwardFlowInventories.clear(); //###
      touchedProjections = false;
      touchedChangedProjections = false;
      OMChange = false;
      //testBG();
    }
  }

  protected HashMap getProvidersAndEndDates() {
      HashMap providers = new HashMap();
      RelationshipSchedule relSched = myOrganization.getRelationshipSchedule();
      Collection relationships = relSched.getMatchingRelationships(TimeSpan.MIN_VALUE,
								   TimeSpan.MAX_VALUE);
      Iterator rit = relationships.iterator();
      Role myRole = getRole(supplyType);
// 	  System.out.println("SDSD myorg: " + myOrganization + " supply type:" + 
// 			       supplyType + " role: " + getRole(supplyType) + "\n");
      while (rit.hasNext()) {
	  Relationship r = (Relationship) rit.next();
	  HasRelationships hr = relSched.getOther(r);
	  if (hr instanceof Organization) {
	      Role role = relSched.getOtherRole(r);
	      if (role == myRole) {
		  Date endDate = r.getEndDate();
		  providers.put(hr, endDate);
		  //	  System.out.println("SDSD   org: " + hr + " end:" + endDate + "\n");
	      }
	  }
      }
      return providers;
  }

  private Collection getUnprovidedTasks(Collection tasks, Verb verb, HashMap providers) {
      Iterator taskIt = tasks.iterator();
      ArrayList unprovidedTasks = new ArrayList();
      Task task;
      Organization provider;
      Allocation alloc;
      long taskEnd;
      Date providerEndDate;
      while (taskIt.hasNext()) {
	  task = (Task)taskIt.next();
	  alloc = (Allocation)task.getPlanElement();
	  if ((alloc != null) && (task.getVerb().equals(verb))){
	      taskEnd = TaskUtils.getEndTime(task);
	      provider = (Organization)alloc.getAsset();
	      if (alloc.getRole() != getRole(supplyType)) {
		  logger.warn("SDSD MISMATCH: " + alloc.getRole() + " " + task + "\n");	
	      }
	      providerEndDate = (Date) providers.get(provider);
	      if (providerEndDate != null && providerEndDate.getTime() < taskEnd) {
		  unprovidedTasks.add(task);
	      }
	  }
      }
//       if (! unprovidedTasks.isEmpty()) {
// 	  System.out.println("SDSD unprovided: " + unprovidedTasks + "\n");	
//       }
      return unprovidedTasks;
  }

  private void rescindTaskAllocations(Collection tasks) {
      Task task;
      Allocation alloc;
      Iterator taskIt = tasks.iterator(); 
      while (taskIt.hasNext()) {
	  task = (Task) taskIt.next();
	  //	  System.out.println("SDSD rescind: " + task + "\n");
	  alloc = (Allocation)task.getPlanElement();
	  publishRemove(alloc);
      }
  }

  /** Subscription for aggregatable support requests. **/
  private IncrementalSubscription detReqSubscription;

  /** Subscription for the aggregated support request **/
  protected CollectionSubscription aggMILSubscription;

  /** Subscription for the MIL tasks **/
  private IncrementalSubscription milSubscription;

  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;

  /** Subscription for incoming Supply tasks **/
  private TaskScheduler supplyTaskScheduler;
//  private IncrementalSubscription supplyTaskSubscription;

  /** Subscription for incoming Projection tasks **/
  private TaskScheduler projectionTaskScheduler;
//  private IncrementalSubscription projectionTaskSubscription;

  /** Subscription for Allocations on outgoing Refill (Supply & ProjectSupply) tasks **/
  private IncrementalSubscription refillAllocationSubscription;

  /** Subscription for my Refill (Supply & ProjectSupply) tasks **/
  private IncrementalSubscription refillSubscription;

  /** Subscription for Supply/ProjectSupply Expansions **/
  private IncrementalSubscription expansionSubscription;

  /** Subscription for InventoryPolicy **/
  private IncrementalSubscription inventoryPolicySubscription;

  /** Subscription for LogisticsOPlan object **/
  private IncrementalSubscription logisticsOPlanSubscription;

  /** Subscription for Withdraw tasks created by this plugin **/
  private IncrementalSubscription withdrawTaskSubscription;

  /** Subscription for ProjectWithdraw tasks created by this plugin **/
  private IncrementalSubscription projectWithdrawTaskSubscription;

  /** Subscription for MaintainInventory Expansion PlanElements created by this plugin**/
  private IncrementalSubscription MIExpansionSubscription;

  /** Subscription for MaintainInventory Expansion for Top level MI task (Aggregate task) **/
  private IncrementalSubscription MITopExpansionSubscription;

  /** Subscription for DetermineRequirements of type MaintainInventory Expansion **/
  private IncrementalSubscription DetReqInvExpansionSubscription;

  /** special subscription to oms only used in subsistence to deal with the level2 -> level6
   *  issue that occurs because subsistence does not generate level 2 tasks
   **/
  private IncrementalSubscription Level6OMSubscription;


  protected void setupSubscriptions() {
    if (!getBlackboardService().didRehydrate()) {
      setupOperatingModes();
      prevLevel6 = getEndOfLevelSix();
    } else {
      // if we did rehydrate set a flag to rehydrate the inventories
      //when we are ready in the execute block
      rehydrateInvs = true;
      Collection level2OMs = getBlackboardService().
          query(new OperatingModePredicate(supplyType, LEVEL_2_TIME_HORIZON));
      //there should only be one.
      Iterator level2it = level2OMs.iterator();
      if (level2it.hasNext()) {
        level2Horizon = (OperatingMode) level2it.next();
      }
      Collection level6OMs = getBlackboardService().
          query(new OperatingModePredicate(supplyType, LEVEL_6_TIME_HORIZON));
      //there should only be one.
      Iterator level6it = level6OMs.iterator();
      if (level6it.hasNext()) {
        level6Horizon = (OperatingMode) level6it.next();
      }
      prevLevel6 = getEndOfLevelSix();
      if (level2Horizon == null || level6Horizon == null) {
        if (logger.isErrorEnabled()) {
          logger.error("InventoryPlugin in agent: " + getAgentIdentifier() +
                       " of supply type: " + supplyType +
                       " is missing operating modes upon rehydration... level2 OM is: " +
                       level2Horizon + " level 6 OM is: " + level6Horizon);
        }
      }
    }

    Level6OMSubscription = (IncrementalSubscription) blackboard.subscribe(new OperatingModePredicate(supplyType, LEVEL_6_TIME_HORIZON));
    detReqSubscription = (IncrementalSubscription) blackboard.subscribe(new DetInvReqPredicate(taskUtils));
    aggMILSubscription = (CollectionSubscription) blackboard.subscribe(new AggMILPredicate(), false);
    milSubscription = (IncrementalSubscription) blackboard.subscribe(new MILPredicate());
    detReqHandler.addMILTasks(milSubscription.elements());
    selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);
    inventoryPolicySubscription = (IncrementalSubscription) blackboard.subscribe(new InventoryPolicyPredicate(supplyType));
    logisticsOPlanSubscription = (IncrementalSubscription) blackboard.subscribe(new LogisticsOPlanPredicate());
    withdrawTaskSubscription = (IncrementalSubscription) blackboard.subscribe(new WithdrawPredicate(supplyType));
    projectWithdrawTaskSubscription = (IncrementalSubscription) blackboard.subscribe(new ProjectWithdrawPredicate(supplyType));
    MIExpansionSubscription = (IncrementalSubscription) blackboard.subscribe(new MIExpansionPredicate(supplyType, taskUtils));
    MITopExpansionSubscription = (IncrementalSubscription) blackboard.subscribe(new MITopExpansionPredicate());
    DetReqInvExpansionSubscription = (IncrementalSubscription) blackboard.subscribe(new DetReqInvExpansionPredicate(taskUtils));

    if (getAgentIdentifier() == null && logger.isErrorEnabled()) {
      logger.error("No agentIdentifier ... subscriptions need this info!!  In plugin: " + this);
    }
    refillAllocationSubscription = (IncrementalSubscription) blackboard.
        subscribe(new RefillAllocPredicate(supplyType, getAgentIdentifier().toString(), taskUtils));
    expansionSubscription = (IncrementalSubscription) blackboard.
        subscribe(new ExpansionPredicate(supplyType, getAgentIdentifier().toString(), taskUtils));
    refillSubscription = (IncrementalSubscription) blackboard.
        subscribe(new RefillPredicate(supplyType, getAgentIdentifier().toString(), taskUtils));

    // Setup TaskSchedulers
    String taskScheduler = (String)pluginParams.get(TASK_SCHEDULER_ON);
    boolean turnOnTaskSched = new Boolean(taskScheduler).booleanValue();
    QuiescenceReportService qrs = (QuiescenceReportService)
      getServiceBroker().getService(this, QuiescenceReportService.class, null);
    AgentIdentificationService ais = (AgentIdentificationService) 
      getServiceBroker().getService(this, AgentIdentificationService.class, null);
    qrs.setAgentIdentificationService(ais);
    QuiescenceAccumulator q = new QuiescenceAccumulator (qrs);
    String id = getAgentIdentifier().toString();
    if (turnOnTaskSched) {
      logger.debug("TASK SCHEDULER ON for "+id);
      java.io.InputStream is = null;
      try {
        is = getConfigFinder().open ("supplyTaskPolicy.xml");
      } catch (Exception e) {
        logger.error ("Could not find file supplyTaskPolicy.xml");
      }
      supplyTaskScheduler = new TaskScheduler
        (new SupplyTaskPredicate(supplyType, id, taskUtils),
         TaskSchedulingPolicy.fromXML (is, this, getAlarmService()),
         blackboard, q, logger, "supplyTasks for " + getBlackboardClientName());
      try {
        is = getConfigFinder().open ("projectionTaskPolicy.xml");
      } catch (Exception e) {
        logger.error ("Could not find file projectionTaskPolicy.xml");
      }
      projectionTaskScheduler = new TaskScheduler
        (new ProjectionTaskPredicate(supplyType, id, taskUtils),
         TaskSchedulingPolicy.fromXML (is, this, getAlarmService()),
         blackboard, q, logger, "projTasks for " + getBlackboardClientName());
    } else { // TaskScheduler OFF
      supplyTaskScheduler = new TaskScheduler
        (new SupplyTaskPredicate(supplyType, id, taskUtils),
         new TaskSchedulingPolicy (new TaskSchedulingPolicy.Predicate[]
                                   {TaskSchedulingPolicy.PASSALL}),
         blackboard, q, logger, "supplyTasks for " + getBlackboardClientName());

      projectionTaskScheduler = new TaskScheduler
        (new ProjectionTaskPredicate(supplyType, id, taskUtils),
         new TaskSchedulingPolicy (new TaskSchedulingPolicy.Predicate[]
                                   {TaskSchedulingPolicy.PASSALL}),
         blackboard, q, logger, "projTasks for " + getBlackboardClientName());
    }
//    supplyTaskSubscription = (IncrementalSubscription) blackboard.
//        subscribe(new SupplyTaskPredicate(supplyType, id, taskUtils));
//    projectionTaskSubscription = (IncrementalSubscription) blackboard.
//        subscribe(new ProjectionTaskPredicate(supplyType, id, taskUtils));
  }

  private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        return ((Organization) o).isSelf();
      }
      return false;
    }
  };

  private static class SupplyTaskPredicate
          implements TaskSchedulingPolicy.Predicate {
    String supplyType;
    String orgName;
    TaskUtils taskUtils;

    public SupplyTaskPredicate(String type, String myOrg, TaskUtils aTaskUtils) {
      supplyType = type;
      orgName = myOrg;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Task task) {
      return task.getVerb().equals(Constants.Verb.SUPPLY) &&
             taskUtils.isDirectObjectOfType(task, supplyType) &&
             (!taskUtils.isMyRefillTask(task, orgName)) &&
             (taskUtils.getQuantity(task) > 0);
    }
  }


  private static class ProjectionTaskPredicate
          implements TaskSchedulingPolicy.Predicate {
    String supplyType;
    String orgName;
    TaskUtils taskUtils;

    public ProjectionTaskPredicate(String type, String orgname, TaskUtils aTaskUtils) {
      supplyType = type;
      orgName = orgname;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Task task) {
      return task.getVerb().equals(Constants.Verb.PROJECTSUPPLY) &&
             taskUtils.isDirectObjectOfType(task, supplyType) &&
             (!taskUtils.isMyInventoryProjection(task, orgName));
    }
  }

  /**
   Passes DetermineRequirements tasks of type MaintainInventory.
   **/
  private static class DetInvReqPredicate implements UnaryPredicate {

    private TaskUtils taskUtils;

    public DetInvReqPredicate(TaskUtils aTaskUtils) {
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.DETERMINEREQUIREMENTS)) {
          return taskUtils.isTaskOfType(t, "MaintainInventory");
        }
      }
      return false;
    }
  }

  /** Grab the Expansion of DetReq MaintainInventory and update ARs **/
  private static class DetReqInvExpansionPredicate implements UnaryPredicate {
    private TaskUtils taskUtils;

    public DetReqInvExpansionPredicate(TaskUtils aTaskUtils) {
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task parent = ((Expansion) o).getTask();
        if (parent.getVerb().equals(Constants.Verb.DETERMINEREQUIREMENTS)) {
          return taskUtils.isTaskOfType(parent, "MaintainInventory");
        }
      }
      return false;
    }
  }

  /**
   Selects the per-inventory MaintainInventory tasks.
   **/
  private static class MILPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.MAINTAININVENTORY)) {
          return t.getDirectObject() != null; // true if this is the agg task
        }
      }
      return false;
    }
  }

  /** get the Expansion for the TOP MI task
   * note that this means each instance for each class of supply will
   * be looking for the same task - but since the results are checked and only
   * changed if there's a difference it shouldn't be too bad.
   **/
  private static class MITopExpansionPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task parent = ((Expansion) o).getTask();
        if (parent.getVerb().equals(Constants.Verb.MAINTAININVENTORY)) {
          if (parent.getDirectObject() == null) {
            return true;
          }
        }
      }
      return false;
    }
  }

  /**
   Selects the aggregate MaintainInventory task
   **/
  private static class AggMILPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        if (t.getVerb().equals(Constants.Verb.MAINTAININVENTORY)) {
          return t.getDirectObject() == null; // true if this is not the agg task
        }
      }
      return false;
    }
  }

  /** Selects the MaintainInventory Expansions we create **/
  private static class MIExpansionPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;

    public MIExpansionPredicate(String type, TaskUtils utils) {
      supplyType = type;
      taskUtils = utils;
    }

    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task parent = ((Expansion) o).getTask();
        if (parent.getVerb().equals(Constants.Verb.MAINTAININVENTORY)) {
          Asset directObject = parent.getDirectObject();
          if (directObject != null && directObject instanceof Inventory) {
            LogisticsInventoryPG thePG = (LogisticsInventoryPG) ((Inventory) directObject).
                searchForPropertyGroup(LogisticsInventoryPG.class);
            Asset resource = thePG.getResource();
            SupplyClassPG pg = (SupplyClassPG) resource.searchForPropertyGroup(SupplyClassPG.class);
            if (pg != null) {
              if (supplyType.equals(pg.getSupplyType())) {
                //            if (taskUtils.isDirectObjectOfType(parent, supplyType)) {
                return true;
              }
            }
          }
        }
      }
      return false;
    }
  }

  /**
   Selects the LogisticsOPlan objects
   **/
  private static class LogisticsOPlanPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return o instanceof LogisticsOPlan;
    }
  }

  /**
   Passes Inventory assets that have a valid LogisticsInventoryPG
   **/

  private static class InventoryPredicate implements UnaryPredicate {
    String supplyType;

    public InventoryPredicate(String type) {
      supplyType = type;
    }

    public boolean execute(Object o) {
      if (o instanceof Inventory) {
        Inventory inv = (Inventory) o;
        LogisticsInventoryPG logInvpg =
            (LogisticsInventoryPG)
            inv.searchForPropertyGroup(LogisticsInventoryPG.class);
        if (logInvpg != null) {
          String type = getAssetType(logInvpg);
          if (supplyType.equals(type)) {
            return true;
          }
        }
      }
      return false;
    }

    private String getAssetType(LogisticsInventoryPG invpg) {
      Asset a = invpg.getResource();
      if (a == null) return null;
      SupplyClassPG pg = (SupplyClassPG)
          a.searchForPropertyGroup(SupplyClassPG.class);
      return pg.getSupplyType();
    }
  }


  //Allocation of refill tasks
  static class RefillAllocPredicate implements UnaryPredicate {
    String type_;
    String orgName_;
    TaskUtils taskUtils;

    public RefillAllocPredicate(String type, String orgName, TaskUtils aTaskUtils) {
      type_ = type;
      orgName_ = orgName;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Task task = ((Allocation) o).getTask();
        if (task.getVerb().equals(Constants.Verb.SUPPLY) ||
            task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, type_)) {
            // need to check if externally allocated
            if (((Allocation) o).getAsset() instanceof Organization) {
              //if (taskUtils.isMyRefillTask(task, orgName_)){
              return true;
              //}
            }
          }
        }
      }
      return false;
    }
  }

  //Refill tasks
  static class RefillPredicate implements UnaryPredicate {
    String type_;
    String orgName_;
    TaskUtils taskUtils;

    public RefillPredicate(String type, String orgName, TaskUtils aTaskUtils) {
      type_ = type;
      orgName_ = orgName;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.SUPPLY) ||
            task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, type_)) {
            if (taskUtils.isMyRefillTask(task, orgName_)) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  private class ExpansionPredicate implements UnaryPredicate {
    String supplyType;
    String orgName;
    TaskUtils taskUtils;

    public ExpansionPredicate(String type, String orgname, TaskUtils taskUtils) {
      supplyType = type;
      orgName = orgname;
      this.taskUtils = taskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Expansion) {
        Task task = ((Expansion) o).getTask();
        if (task.getVerb().equals(Constants.Verb.SUPPLY) ||
            task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            if (!taskUtils.isMyRefillTask(task, orgName)) {
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  private class InventoryPolicyPredicate implements UnaryPredicate {
    String type;

    public InventoryPolicyPredicate(String type) {
      this.type = type;
    }

    public boolean execute(Object o) {
      if (o instanceof org.cougaar.logistics.plugin.inventory.InventoryPolicy) {
        String type = ((InventoryPolicy) o).getResourceType();
        if (type.equals(this.type)) {
          if (logger.isInfoEnabled()) {
            logger.info("Found an inventory policy for " + this.type + "agent is: " +
                        getMyOrganization());
          }
          return true;
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Ignoring type of: " + type + " in " +
                         getMyOrganization() + " this type is: " +
                         this.type);
          }
        }
      }
      return false;
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

  private class WithdrawPredicate implements UnaryPredicate {
    String supplyType;

    public WithdrawPredicate(String type) {
      supplyType = type;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.WITHDRAW)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            return true;
          }
        }
      }
      return false;
    }
  }

  private class ProjectWithdrawPredicate implements UnaryPredicate {
    String supplyType;

    public ProjectWithdrawPredicate(String type) {
      supplyType = type;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            return true;
          }
        }
      }
      return false;
    }
  }

  private class OperatingModePredicate implements UnaryPredicate {
    String supplyType;
    String level;

    public OperatingModePredicate(String type, String level) {
      supplyType = type;
      this.level = level;
    }

    public boolean execute(Object o) {
      if (o instanceof OperatingMode) {
        OperatingMode om = (OperatingMode) o;
        if (om.getName().equals(level + "_" + supplyType)) {
          return true;
        }
      }
      return false;
    }
  }

  // Determines which tasks should be expanded and which should be
  // re-allocated to a supplier
  protected Collection sortIncomingSupplyTasks(Collection tasks) {
    ArrayList expandList = new ArrayList();
    ArrayList passThruList = new ArrayList();
    Task t;
    Inventory inventory;
    Asset asset;
    for (Iterator i = tasks.iterator(); i.hasNext();) {
      t = (Task) i.next();
      asset = (Asset) t.getDirectObject();
      inventory = findOrMakeInventory(asset);
      if (inventory != null) {
        expandList.add(t);
      } else {  // allocate tasks to supplier?
        passThruList.add(t);
      }
    }
    externalAllocator.forwardSupplyTasks(passThruList);
    return expandList;
  }

  private void expandIncomingRequisitions(Collection tasks) {
    Collection tasksToExpand = sortIncomingSupplyTasks(tasks);
    supplyExpander.expandAndDistributeRequisitions(tasksToExpand);
  }

  private boolean expandIncomingProjections(Collection tasks) {
    Collection tasksToExpand = sortIncomingSupplyTasks(tasks);
    return supplyExpander.expandAndDistributeProjections(tasksToExpand);
  }

  /**
   Add some inventories to the inventoryHash.
   Method called during rehydration to populate inventory hash
   **/
  private void addRehydratedInventories(Collection inventories) {
    for (Iterator i = inventories.iterator(); i.hasNext();) {
      Inventory inv = (Inventory) i.next();
      LogisticsInventoryPG logInvPG = (LogisticsInventoryPG) inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      logInvPG.reinitialize(logToCSV, this);
      addInventory(inv);
    }
  }


    //AHF
  protected void resetLogOPlanForInventories() {
    Iterator inventories = getInventories().iterator();
    while(inventories.hasNext()) {
      Inventory inv = (Inventory) inventories.next();
      LogisticsInventoryPG logInvPG = (LogisticsInventoryPG) 
        inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      if(logInvPG.getArrivalTime() != getOPlanArrivalInTheaterTime()) {
        long newSupplierArrivalTime = logInvPG.getSupplierArrivalTime() + 
          (logInvPG.getArrivalTime() - getOPlanArrivalInTheaterTime());
        logInvPG.setArrivalTime(getOPlanArrivalInTheaterTime());
        ((NewLogisticsInventoryPG)logInvPG).setSupplierArrivalTime(newSupplierArrivalTime);
        logInvPG.setStartCDay(logOPlan.getOplanCday());
        publishChange(inv);
	touchInventory(inv);
      }
    }
  }


  protected void addInventory(Inventory inventory) {
    String item = getInventoryType(inventory);
    inventoryHash.put(item, inventory);
  }

  private void removeInventories(Enumeration inventories) {
    while (inventories.hasMoreElements()) {
      removeInventory((Inventory) inventories.nextElement());
    }
  }

  private void removeInventory(Inventory inventory) {
    String item = getInventoryType(inventory);
    inventoryHash.remove(item);
  }

  public String getInventoryType(Inventory inventory) {
    ScheduledContentPG scp = inventory.getScheduledContentPG();
    Asset proto = scp.getAsset();
    if (proto == null) {
      if (logger.isErrorEnabled()) {
        logger.error("getInventoryType failed to get asset for " +
                     inventory.getScheduledContentPG().getAsset().getTypeIdentificationPG());
      }
      return "";
    }
    return proto.getTypeIdentificationPG().getTypeIdentification();
  }

  public Inventory findOrMakeInventory(Asset resource) {
    Inventory inventory = null;
    String item = resource.getTypeIdentificationPG().getTypeIdentification();
    inventory = (Inventory) inventoryHash.get(item);
    if (inventory == null) {
      inventory = createInventory(resource, item);
      if (inventory != null) {
        addInventory(inventory);
        publishAdd(inventory);
        detReqHandler.findOrMakeMILTask(inventory, aggMILSubscription);
      }
    }
    if (inventory == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Inventory is null for " + item);
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("findOrMakeInventory(), CREATED inventory bin for: " +
                     AssetUtils.assetDesc(inventory.getScheduledContentPG().getAsset()));
      }
    }
    return inventory;
  }

  protected Inventory createInventory(Asset resource, String item) {
    double levels[] = null;
    Inventory inventory = null;
    levels = (double[]) inventoryInitHash.get(item);
    if (levels != null) {
      inventory = (Inventory) getPlanningFactory().createAsset("Inventory");
      NewLogisticsInventoryPG logInvPG =
          (NewLogisticsInventoryPG) PropertyGroupFactory.newLogisticsInventoryPG();
      inventory.addOtherPropertyGroup(logInvPG);
      if (getAssetUtils().isLevel2Asset(resource)) {
        logInvPG.setIsLevel2(true); // will need to key off asset to identify level2 item
        // Need to distinguish Level2Package aggregates of different supply types otherwise
        // they are determined to be the same asset and are removed from the blackboard
        ((NewItemIdentificationPG) inventory.getItemIdentificationPG()).setItemIdentification("Inventory:" + item + ":" + supplyType);
      } else {
        logInvPG.setIsLevel2(false);
        ((NewItemIdentificationPG) inventory.getItemIdentificationPG()).setItemIdentification("Inventory:" + item);
      }
      logInvPG.setCapacity(levels[0]);
      logInvPG.setFillToCapacity(fillToCapacity);
      logInvPG.setInitialLevel(levels[1]);
      logInvPG.setResource(resource);
      logInvPG.setOrg(getMyOrganization());
      logInvPG.setSupplierArrivalTime(getSupplierArrivalTime());
      logInvPG.setLogInvBG(new LogisticsInventoryBG(logInvPG));
      logInvPG.initialize(startTime, criticalLevel, reorderPeriod, getOrderShipTime(), bucketSize, getCurrentTimeMillis(), logToCSV, this);
      logInvPG.setArrivalTime(getOPlanArrivalInTheaterTime());
      logInvPG.setStartCDay(logOPlan.getOplanCday());

      NewTypeIdentificationPG ti =
          (NewTypeIdentificationPG) inventory.getTypeIdentificationPG();
      ti.setTypeIdentification("InventoryAsset");
      ti.setNomenclature("Inventory Asset");

      NewScheduledContentPG scp;
      scp = (NewScheduledContentPG) inventory.getScheduledContentPG();
      scp.setAsset(resource);
      scp.setSchedule(scheduleUtils.buildSimpleQuantitySchedule(levels[1], startTime,
                                                                startTime + (TimeUtils.MSEC_PER_DAY * 10)));
    }
    return inventory;
  }

  public void touchInventory(Inventory inventory) {
    if (!touchedInventories.contains(inventory)) {
      touchedInventories.add(inventory);
    }
  }

  public Collection getTouchedInventories() {
    return touchedInventories;
  }

  public Collection getInventories() {
    return inventoryHash.values();
  }

  public void takeInventorySnapshot(Collection inventories) {
    Inventory inv;
    Iterator inv_it = inventories.iterator();
    LogisticsInventoryPG logInvPG = null;
    while (inv_it.hasNext()) {
      inv = (Inventory) inv_it.next();
      logInvPG = (LogisticsInventoryPG) inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      logInvPG.takeSnapshot(inv);
      if (logToCSV) {
        logInvPG.logAllToCSVFile(cycleStamp);
      }
    }
  }

  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    Collection p = getParameters();

    if (p.isEmpty()) {
      if (logger.isErrorEnabled()) {
        logger.error("No parameters: InventoryPlugin requires 1 parameter, Supply Type.  Additional parameter for csv logging, default is disabled.   e.g. org.cougaar.logistics.plugin.inventory.InventoryPlugin("
                     +SUPPLY_TYPE
                     +"=BulkPOL, ENABLE_CSV_LOGGING=true)");
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
//      inventoryFile = (String)map.get(INVENTORY_FILE);
    if (supplyType == null && logger.isErrorEnabled()) {
      logger.error("No SUPPLY_TYPE parameter: InventoryPlugin requires 1 parameter, Supply Type.  Additional parameter for csv logging, default is disabled.   e.g. org.cougaar.logistics.plugin.inventory.InventoryPlugin("
                   +SUPPLY_TYPE
                   +"=BulkPOL, ENABLE_CSV_LOGGING=true)");
    }
    String loggingEnabled = (String) map.get(ENABLE_CSV_LOGGING);
    if ((loggingEnabled != null) &&
        (loggingEnabled.trim().equals("true"))) {
      logToCSV = true;
    }

    String prepoOffsetStr = (String) map.get(PREPO_ARRIVAL_OFFSET);
    if((prepoOffsetStr != null) &&
       !(prepoOffsetStr.trim().equals(""))) {
      try {
        int prepoOffset = Integer.parseInt(prepoOffsetStr);
        prepoArrivalOffset = prepoOffset;
      }
      catch(NumberFormatException ex) {
        logger.error("InventoryPlugin(" + PREPO_ARRIVAL_OFFSET + "=" + prepoOffsetStr +
                     ") value is not a parseable integer.  Defaulting to " + prepoArrivalOffset);
      }

    }
    return map;
  }

  private String getInventoryFile(String type) {
    String result = null;
    // if defined in plugin argument list
    String inv_file = null;
    if ((inv_file = (String) pluginParams.get(INVENTORY_FILE)) != null) {
      result = inv_file;
      //   }
      //    else {
//       result = getClusterSuffix(myOrganization.getClusterPG().getMessageAddress().toString()) +
// 	"_"+type.toLowerCase()+".inv";
    } else if (type.equals("Ammunition")) {
      result = getAgentIdentifier().toString() +
          "_" + type.toLowerCase() + ".inv";
    } else {
      result = getClusterSuffix(getAgentIdentifier().toString()) +
          "_" + type.toLowerCase() + ".inv";
    }
    return result;
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

  public void publishAddToExpansion(Task parent, Task subtask) {
    //attach the subtask to its parent and the parent's workflow
    PlanElement pe = parent.getPlanElement();
    Expansion expansion;
    NewWorkflow wf;
    ((NewTask) subtask).setParentTask(parent);
    ((NewTask) subtask).setContext(parent.getContext());
    ((NewTask) subtask).setPlan(parent.getPlan());
    // Task has not been expanded, create an expansion
    if (pe == null) {
      PlanningFactory factory = getPlanningFactory();
      // Create workflow
      wf = (NewWorkflow) factory.newWorkflow();
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

    if ((subtask.getVerb().equals(Constants.Verb.SUPPLY)) ||
        (subtask.getVerb().equals(Constants.Verb.PROJECTSUPPLY))) {
      newRefills.add(subtask);
    }
  }

  // called by the RefillGenerator to hook up the refill task to the maintain
  // inventory parent task and workflow.
  public void publishRefillTask(Task task, Inventory inventory) {
    Task milTask = detReqHandler.findOrMakeMILTask(inventory,
                                                   aggMILSubscription);
    publishAddToExpansion(milTask, task);
  }

  private Organization getMyOrganization(Enumeration orgs) {
    Organization myOrg = null;
    // look for this organization
    if (orgs.hasMoreElements()) {
      myOrg = (Organization) orgs.nextElement();
    }
    return myOrg;
  }

  public long getOPlanStartTime() {
    return logOPlan.getStartTime();
  }

  public long getOPlanEndTime() {
    return logOPlan.getEndTime();
  }

  public long getOPlanArrivalInTheaterTime() {
    return logOPlan.getArrivalTime();
  }

  public long getPrepoArrivalTime() {
    return getOPlanArrivalInTheaterTime() - (bucketSize * prepoArrivalOffset);
  }

  public void getInventoryData() {
    String invFile = getInventoryFileName();
    if (invFile != null) {
      Enumeration initialInv = FileUtils.readConfigFile(invFile, getConfigFinder());
      if (initialInv != null) {
        stashInventoryInformation(initialInv);
      }
    }
  }

  private void stashInventoryInformation(Enumeration initInv) {
    String line;
    String item = null;
    double capacity, level;

    while (initInv.hasMoreElements()) {
      line = (String) initInv.nextElement();
      // Find the fields in the line, values seperated by ','
      Vector fields = FileUtils.findFields(line, ',');
      if (fields.size() < 3)
        continue;
      item = (String) fields.elementAt(0);
      capacity = Double.valueOf((String) fields.elementAt(1)).doubleValue();
      level = Double.valueOf((String) fields.elementAt(2)).doubleValue();
      double[] levels = {capacity, level};
      inventoryInitHash.put(item, levels);
    }
  }

  protected Role getRole(String supply_type) {
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

  protected boolean updateInventoryPolicy(Collection policies) {
    InventoryPolicy pol;
    boolean changed = false;
    Iterator policy_iterator = policies.iterator();
    while (policy_iterator.hasNext()) {
      pol = (InventoryPolicy) policy_iterator.next();
      inventoryPolicy = pol;
      int cl = pol.getCriticalLevel();
      if ((cl >= 0) && (cl != criticalLevel)) {
        criticalLevel = cl;
        changed = true;
      }
      int rp = pol.getReorderPeriod();
      if ((rp >= 0) && (rp != reorderPeriod)) {
        reorderPeriod = rp;
        changed = true;
      }
      long bucket = pol.getBucketSize();
      if (bucket >= TimeUtils.MSEC_PER_HOUR) {
        bucketSize = bucket;
        changed = true;
      }
      boolean ftc = pol.getFillToCapacity();
      if (ftc != fillToCapacity) {
        fillToCapacity = ftc;
        changed = true;
      }
    }
    return changed;
  }

  public int getOrderShipTime() {
    return inventoryPolicy.getOrderShipTime();
  }

  public long getSupplierArrivalTime() {
    return inventoryPolicy.getSupplierArrivalTime();
  }

  public long getRefillStartTime() {
	return Math.max(getOPlanArrivalInTheaterTime(),getSupplierArrivalTime());
  }

  public int getMaxLeadTime() {
    return inventoryPolicy.getSupplierAdvanceNoticeTime() + getOrderShipTime();
  }

  /** VTH operating modes */
  protected OperatingMode level2Horizon, level6Horizon;

  /** create and publish VTH Operating Modes */
  protected void setupOperatingModes() {
    try {
      //getBlackboardService().openTransaction();
      OMCRange level2Range = new IntRange(LEVEL_2_MIN.intValue(), LEVEL_2_MAX.intValue());
      OMCRangeList rangeList = new OMCRangeList(level2Range);
      publishAdd(level2Horizon = new OperatingModeImpl(LEVEL_2_TIME_HORIZON + "_" + supplyType, rangeList,
                                                       LEVEL_2_TIME_HORIZON_DEFAULT));

      OMCRange level6Range = new IntRange(LEVEL_6_MIN.intValue(), LEVEL_6_MAX.intValue());
      rangeList = new OMCRangeList(level6Range);
      publishAdd(level6Horizon = new OperatingModeImpl(LEVEL_6_TIME_HORIZON + "_" + supplyType, rangeList,
                                                       LEVEL_6_TIME_HORIZON_DEFAULT));
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("" + getMyOrganization() + " got exception creating operating modes.", e);
      }
      //} finally {
      //getBlackboardService().closeTransaction();
    }

    if (logger.isInfoEnabled()) {
      logger.info("" + getMyOrganization() + " created operating modes - " +
                  "level 2 time horizon is " + level2Horizon +
                  " and level 6 is " + level6Horizon);
    }
  }

  /** tiny helper class for VTH Operating Modes */
  protected static class IntRange extends OMCRange {
    public IntRange(int a, int b) {
      super(a, b);
    }
  }

  /** relative to now -- this is correct, isn't it? */
  protected long getEndOfLevelSix() {
    long now = currentTimeMillis();
    int days = ((Integer) level6Horizon.getValue()).intValue();

    return timeUtils.addNDays(now, days);
  }

  /** relative to now -- this is correct, isn't it? */
  protected long getEndOfLevelTwo() {
    long now = currentTimeMillis();
    int days = ((Integer) level2Horizon.getValue()).intValue();

    return timeUtils.addNDays(now, days);
  }

  /** When one of our Refill tasks gets removed (Supply or ProjectSupply),
   *  remove it from the BG list.
   *  @param removedTasks The collection of removed refill tasks.
   **/
  public void handleRemovedRefills(Collection removedTasks) {
    Iterator removedIter = removedTasks.iterator();
    while (removedIter.hasNext()) {
      Task removed = (Task) removedIter.next();
      String item = removed.getDirectObject().getTypeIdentificationPG().getTypeIdentification();
      Inventory inventory = (Inventory) inventoryHash.get(item);
      LogisticsInventoryPG invPG = (LogisticsInventoryPG)
          inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
      if (removed.getVerb().equals(Constants.Verb.SUPPLY)) {
        invPG.removeRefillRequisition(removed);
      } else {
        invPG.removeRefillProjection(removed);
      }
    }
  }

  public MessageAddress getClusterId() {
    return getAgentIdentifier();
  }

  public TaskScheduler getSupplyTaskScheduler() {
    return supplyTaskScheduler;
  }

  //
  private void processDetReq(Collection addedDRs) {
    // with one oplan we should only have one DR for MI.
    Iterator drIt = addedDRs.iterator();
    if (drIt.hasNext()) {
      Task detReq = (Task) drIt.next();
      //synch on the detReq task so only one instance of this plugin
      // checks and creates a single agg task and then creates an
      // empty expansion (wf) for the maintain inventory for each item tasks
      synchronized (detReq) {
        if (detReq.getPlanElement() == null) {
          detReqHandler.createAggTask(addedDRs);
        }
      }
    }
  }

  // We only want to process inventories that we have no new refills for.
  protected Collection getActionableInventories() {
    ArrayList actionableInvs = new ArrayList(touchedInventories);
    Task refill;
    Asset asset;
    Inventory inventory;
    Iterator refillIt = newRefills.iterator();
    while (refillIt.hasNext()) {
      refill = (Task) refillIt.next();
      asset = (Asset) refill.getDirectObject();
      inventory = findOrMakeInventory(asset);
      actionableInvs.remove(inventory);
    }
    return actionableInvs;
  }

  private void rebuildPGCustomerHash() {
    Collection changedInventories = getTouchedInventories();
    Iterator invIter = changedInventories.iterator();
    Inventory inventory;
    LogisticsInventoryPG thePG;
    while (invIter.hasNext()) {
      inventory = (Inventory) invIter.next();
      thePG = (LogisticsInventoryPG)
          inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
      thePG.rebuildCustomerHash();
    }
  }

  private boolean didOrgRelationshipsChange() {
    boolean relSchedChange=false;
    if (selfOrganizations.hasChanged())  {
      Set changeReports = selfOrganizations.getChangeReports(getMyOrganization());
      
      Iterator crits = changeReports.iterator();
      while(crits.hasNext()) {
        if(crits.next() instanceof RelationshipSchedule.RelationshipScheduleChangeReport){
          relSchedChange=true;
          break;
        }
      }
    }
    return relSchedChange;
  }

  // get the first day in theater
  public long getLogOPlanStartTime() {
    return logOPlan.getStartTime();
  }

  // get the last day in theater
  public long getLogOPlanEndTime() {
    return logOPlan.getEndTime();
  }

  protected ExpanderModule getExpanderModule() {
    return new SupplyExpander(this);
  }

  protected AllocatorModule getAllocatorModule() {
    return new ExternalAllocator(this, getRole(supplyType));
  }

  protected RefillGeneratorModule getRefillGeneratorModule() {
    return new RefillGenerator(this);
  }

  protected RefillProjectionGeneratorModule getRefillProjectionGeneratorModule() {
    return new RefillProjectionGenerator(this);
  }

  protected ComparatorModule getComparatorModule() {
    String comparatorClass = (String) pluginParams.get("COMPARATOR");
    if (comparatorClass == null) {
      return new RefillComparator(this);
    } else {
      if (comparatorClass.indexOf('.') == -1) {
        comparatorClass = "org.cougaar.logistics.plugin.inventory." + comparatorClass;
      }
      try {
        Class[] paramTypes = {this.getClass()};
        Object[] initArgs = {this};
        Class cls = Class.forName(comparatorClass);
        Constructor constructor = cls.getConstructor(paramTypes);
        ComparatorModule comparator = (ComparatorModule) constructor.newInstance(initArgs);
        logger.info("Using comparator " + comparatorClass);
        return comparator;
      } catch (Exception e) {
        logger.error(e + " Unable to create Expander instance of " + comparatorClass + ". " +
                     "Loading default org.cougaar.logistics.plugin.inventory.RefillComparator");
      }
    }
    return new RefillComparator(this);
  }

  /**
   Self-Test
   **/
  public void automatedSelfTest() {
    if (logger.isErrorEnabled()) {
      if (supplyType == null) logger.error("No SupplyType Plugin parameter.");
      if (inventoryFile == null) logger.error("No Inventory File Plugin parameter.");
      if (inventoryInitHash.isEmpty()) {
        logger.error("No initial inventory information.  Inventory File is empty or non-existant.");
        logger.error("Could not find Inventory file : " + inventoryFile);
      }
      if (detReqHandler.getDetermineRequirementsTask(aggMILSubscription) == null)
        logger.error("Missing DetermineRequirements for MaintainInventory task.");
      if (logOPlan == null)
        logger.error("Missing LogisticsOPlan object. Is the LogisticsOPlanPlugin loaded?");
      if (myOrganization == null)
        logger.error("Missing myorganization");
      logger.error("Critical Level is " + criticalLevel);
      logger.error("Reorder Period is " + reorderPeriod);
      logger.error("Days per bucket is " + bucketSize);
    }
  }

  private void testBG() {
    Iterator inv_it = inventoryHash.values().iterator();
    Inventory inv;
    LogisticsInventoryPG logInvPG = null;
    cycleStamp = (new Date()).getTime();
    while (inv_it.hasNext()) {
      inv = (Inventory) inv_it.next();
      if (logger.isErrorEnabled()) {
        logger.error("***" + inv.getItemIdentificationPG().getItemIdentification());
      }
      logInvPG = (LogisticsInventoryPG) inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      logInvPG.takeSnapshot(inv);
      if (logToCSV) {
        logInvPG.logAllToCSVFile(cycleStamp);
      }
      logInvPG.Test();
    }
  }
}

