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

package org.cougaar.logistics.plugin.trans;

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
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.util.UnaryPredicate;

import java.lang.reflect.Constructor;
import java.util.*;

/** The Level2TranslatorPlugin generates demand during execution.
 **/

public class Level2TranslatorPlugin extends ComponentPlugin
    implements UtilsProvider {
  private DomainService domainService;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils AssetUtils;
  private ScheduleUtils scheduleUtils;
  private HashMap pluginParams;

  private Level2Disposer disposer;


  private String supplyType;


  public final String SUPPLY_TYPE = "SUPPLY_TYPE";


  private Organization myOrganization;
  private String myOrgName;


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

    disposer = getNewLevel2DisposerModule();

  }

  public void unload() {
    super.unload();
    if (domainService != null) {
      getServiceBroker().releaseService(this, DomainService.class, domainService);
    }
  }

  /** Subscription for the Organization(s) in which this plugin resides **/
  private IncrementalSubscription selfOrganizations;

  private IncrementalSubscription supplyTaskSubscription;
  private IncrementalSubscription projectionTaskSubscription;
  private IncrementalSubscription level2TaskSubscription;

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


  private static class SupplyTaskPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;

    public SupplyTaskPredicate(String type, TaskUtils aTaskUtils) {
      supplyType = type;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.SUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            return true;
          }
        }
      }
      return false;
    }
  }


  private static class ProjectionTaskPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;

    public ProjectionTaskPredicate(String type, TaskUtils aTaskUtils) {
      supplyType = type;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            return !(taskUtils.isLevel2(task));
          }
        }
      }
      return false;
    }
  }


  private static class Level2TaskPredicate implements UnaryPredicate {
    String supplyType;
    TaskUtils taskUtils;

    public Level2TaskPredicate(String type, TaskUtils aTaskUtils) {
      supplyType = type;
      taskUtils = aTaskUtils;
    }

    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        if (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
          if (taskUtils.isDirectObjectOfType(task, supplyType)) {
            if(taskUtils.isLevel2(task)) {
              return (!(taskUtils.isReadyForTransport(task)));
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
        level2TaskSubscription = (IncrementalSubscription) blackboard.
            subscribe(new Level2TaskPredicate(supplyType, taskUtils));
	if(logger.isDebugEnabled()) {
	    logger.debug("Level2TranslatorPlugin just loaded level 2 subscription");
	}
              } else {
        if (logger.isInfoEnabled()) {
          logger.info("\n Level2TranslatorPlugin " + supplyType +
                      " not ready to process tasks yet." +
                      " my org is: " + myOrganization);
        }
        return;
      }
    }
    if (!level2TaskSubscription.getAddedCollection().isEmpty()) {
      supplyTaskSubscription = (IncrementalSubscription) blackboard.
          subscribe(new SupplyTaskPredicate(supplyType, taskUtils));

      projectionTaskSubscription = (IncrementalSubscription) blackboard.
          subscribe(new ProjectionTaskPredicate(supplyType, taskUtils));

      logger.shout("Level2TranslatorPlugin got Level 2 task - now disposing");

      //for now just dispose of them
      disposer.disposeAndRemoveExpansion(level2TaskSubscription.getAddedCollection());
    }


  }




  private Level2Disposer getNewLevel2DisposerModule() {
    return new Level2Disposer(this);
  }


  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    final String errorString = "Level2TranslatorPlugin requires 1 parameter, SUPPLY_TYPE.   As in Level2TranslatorPlugin(SUPPLY_TYPE=BulkPOL)";
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

    //MWD Is this right
    /*
    if(supplyType == null) {
      supplyType = "Ammunition";
    }
    */

    if ((supplyType == null) &&
        logger.isErrorEnabled()) {
      logger.error(errorString);
    }
    return map;
  }
}




