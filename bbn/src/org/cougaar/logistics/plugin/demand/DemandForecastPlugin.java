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

import java.lang.reflect.Constructor;
import java.util.*;
import org.cougaar.core.mts.*;

import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.oplan.Oplan;
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
import org.cougaar.planning.plugin.util.PluginHelper;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.planning.service.LDMService;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;

import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OperatingMode;
import org.cougaar.core.adaptivity.OperatingModeImpl;
import org.cougaar.logistics.plugin.inventory.*;

/** The DemandForecastPlugin is the Glue of demand generation.
 *  It handles all blackboard services for its modules,
 *  facilitates inter-module communication and manages the
 *  subscriptions.
 *  All modules are called from the DemandForecastPlugin.
 **/

public class DemandForecastPlugin extends ComponentPlugin
    implements UtilsProvider {

    private boolean initialized = false;
    private DomainService domainService;
    private LoggingService logger;
    private TaskUtils taskUtils;
    private TimeUtils timeUtils;
    private AssetUtils AssetUtils;
    private HashMap pluginParams;
    private HashMap subscriptionToBGHash;
    private HashMap predToSubscriptionHash;

    private String supplyType;
    private Class supplyClassPG;

    private Organization myOrganization;
    private String myOrgName;
    private DetReqExpanderIfc determineRequirementsExpander;
    private GenProjExpanderIfc generateProjectionsExpander;
    //  private SchedulerModule planningScheduler;

    private boolean processedDetReq=false;


    public final String SUPPLY_TYPE = "SUPPLY_TYPE";
    public final String SUPPLY_PG_CLASS = "SUPPLY_PG_CLASS";
    public final String REQ_EXPANDER = "REQ_EXPANDER";
    public final String PROJ_EXPANDER = "PROJ_EXPANDER";

    private transient ArrayList newRefills = new ArrayList();


    public void load() {
        super.load();
        logger = getLoggingService(this);
        timeUtils = new TimeUtils(this);
        AssetUtils = new AssetUtils(this);
        taskUtils = new TaskUtils(this);
        //detReqHandler = new DetReqAggHandler(this);
        // readParameters() initializes supplyType and inventoryFile
        pluginParams = readParameters();
        determineRequirementsExpander = getDetermineRequirementsExpanderModule();
        generateProjectionsExpander = getGenerateProjectionsExpanderModule();

        subscriptionToBGHash = new HashMap();
        predToSubscriptionHash = new HashMap();

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

	if((orgActivities.getCollection().isEmpty()) || (detReqSubscription.getCollection().isEmpty())) {
	    processDetReq = false;
	    return;
	}

	if(!detReqSubscription.getCollection.isEmpty()) {
	    Iterator detReqIt = detReqSubscription.getCollection.iterator();
	    Task detReqTask = (Task) detReqIt.next();
	    processDetReq = (!(detReqTask.getPlanElement() == null));
	}
	
	//There should be both a determineRequirements task
	//and an oplan before kicking off the expander for the first time.
	//from then on out we should be catching additional assets added, or removed.
	//It is also possible that this agent has no assets and the expander has to dispose of the detReqTask.
	
	//if there is a new determine requirements task or new oplan do this
	if(((!orgActivities.getAddedCollection().isEmpty()) &&
	    (!processedDetReq)) ||
	   (!detReqSubscription.getAddedCollection().isEmpty())) {
	    processDetReq(detReqSubscription.getCollection(),
			  assetsWithPGSubscription.getCollection());
	}
	//otherwise just issue a new 
	else if(!assetsWithPGSubscription.getAddedCollection().isEmpty()) {
	    processDetReq(detReqSubscription.getCollection(),
			  assetsWithPGSubscription.getAddedCollection());
	}
	else if(!assetsWithPGSubscription.getRemovedCollection().isEmpty()) {
	    removeFromDetReq(detReqSubscription.getCollection(),
			     assetsWithPGSubscription.getRemovedCollection());
	    
	}
	
        if (myOrganization == null) {
            myOrganization = getMyOrganization(selfOrganizations.elements());
        }
	
        if (myOrganization == null) {
            if (logger.isInfoEnabled()) {
                logger.info("\n DemandForecastPlugin " + supplyType +
                            " not ready to process tasks yet." +
                            " my org is: " + myOrganization);
            }
            return;
        }
	
    }


    /** Subscription for aggregatable support requests. **/
    private IncrementalSubscription orgActivities;

    /** Subscription for aggregatable support requests. **/
    private IncrementalSubscription detReqSubscription;

     /** Subscription for the Organization(s) in which this plugin resides **/
    private IncrementalSubscription selfOrganizations;

    /** Subscription for all assets with plugin parameters PG class attached to it **/
    private IncrementalSubscription assetsWithPGSubscription;

    protected void setupSubscriptions() {

        selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);

	orgActivities = (IncrementalSubscription) blackboard.subscribe(orgActivityPredicate);

        detReqSubscription = (IncrementalSubscription) blackboard.subscribe(new DetReqPredicate(supplyType, taskUtils));

	assetsWithPGSubscription = (IncrementalSubscription) getBlackboardService().subscribe(new AssetOfTypePredicate(supplyClassPG));
    }

    private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            if (o instanceof Organization) {
                return ((Organization) o).isSelf();
            }
            return false;
        }
    };

    private static UnaryPredicate orgActivityPredicate = new UnaryPredicate() {
	public boolean execute(Object o) {
	    return (o instanceof OrgActivity);
	}
    };


    private static class DetReqPredicate implements UnaryPredicate {
        private String supplyType;
        private TaskUtils taskUtils;

        public DetReqPredicate(String type, TaskUtils utils) {
            this.supplyType = type;
            this.taskUtils = utils;
        } // constructor

        /**
         *  Predicate defining expandable Determine Reqs.
         **/
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
        final String errorString = "DemandForecastPlugin requires 2 parameters, Supply Type and associated SupplyPGClass.  Additional parameter for csv logging, default is disabled.   e.g. org.cougaar.logistics.plugin.inventory.DemandForecastPlugin(" + SUPPLY_TYPE + "=BulkPOL, " + SUPPLY_PG_CLASS + "=org.cougaar.logistics.ldm.asset.BulkPOLPG);";
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
                && logger.isErrorEnabled()))
        {
            logger.error(errorString);
        } else {
            try {
                supplyClassPG = Class.forName(supplyClassPGStr);
            } catch (Exception e) {
                logger.error("Problem loading SUPPLY_PG_CLASS: " + e);
                logger.error(errorString);
            }
        }
        return map;
    }


   private void processDetReq(Collection addedDRs, Collection assets) {
    // with one oplan we should only have one DR for MI.
    Iterator drIt = addedDRs.iterator();
    if (drIt.hasNext()) {
      Task detReq = (Task) drIt.next();
      //synch on the detReq task so only one instance of this plugin
      // checks and creates a single agg task and then creates an
      // empty expansion (wf) for the maintain inventory for each item tasks
      synchronized(detReq) {
          determineRequirementsExpander.expandDetermineRequirements(detReq,assets);
          processedDetReq = true;
      }
    }
  }

    private void removeFromDetReq(Collection addedDRs, Collection removedAssets) {
    // with one oplan we should only have one DR for MI.
    Iterator drIt = addedDRs.iterator();
    if (drIt.hasNext()) {
      Task detReq = (Task) drIt.next();
      //synch on the detReq task so only one instance of this plugin
      // checks and creates a single agg task and then creates an
      // empty expansion (wf) for the maintain inventory for each item tasks
      synchronized(detReq) {
          determineRequirementsExpander.removeSubtasksFromDetermineRequirements(detReq,removedAssets);
          processedDetReq = true;
      }
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


