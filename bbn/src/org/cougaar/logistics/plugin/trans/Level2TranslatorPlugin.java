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
  private Level2Expander expander;


  private String supplyType;
  private boolean doTranslate = true;


  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String TRANSLATOR_ON = "TRANSLATOR_ON";


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
    expander = getNewLevel2ExpanderModule();

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


  public void setupSubscriptions() {

    selfOrganizations = (IncrementalSubscription) blackboard.subscribe(orgsPredicate);

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
            if (taskUtils.isLevel2(task)) {
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
        supplyTaskSubscription = (IncrementalSubscription) blackboard.
            subscribe(new SupplyTaskPredicate(supplyType, taskUtils));
        projectionTaskSubscription = (IncrementalSubscription) blackboard.
            subscribe(new ProjectionTaskPredicate(supplyType, taskUtils));
        if (logger.isDebugEnabled()) {
          logger.debug("Level2TranslatorPlugin just loaded level 2 subscription");
        }
      } else {
        if (logger.isInfoEnabled()) {
          logger.info("\n Level2TranslatorPlugin " + supplyType +
                      " not ready to process tasks yet." +
                      " my org is: " + myOrganization +
                      " supply type is " + supplyType);
        }
        return;
      }
    }


    if ((level2TaskSubscription != null) &&
        (!level2TaskSubscription.getAddedCollection().isEmpty())) {

      if (doTranslate) {
        translateTasks(level2TaskSubscription.getAddedCollection(),
                       projectionTaskSubscription.getCollection(),
                       supplyTaskSubscription.getCollection());
      } else {
        //logger.shout("Level2TranslatorPlugin got Level 2 task - now disposing");
        disposer.disposeAndRemoveExpansion(level2TaskSubscription.getAddedCollection());
      }
    } else if ((level2TaskSubscription != null) &&
        ((level2TaskSubscription.hasChanged()) ||
        (projectionTaskSubscription.hasChanged()) ||
        (supplyTaskSubscription.hasChanged()))) {
      if (doTranslate) {
        translateTasks(level2TaskSubscription.getCollection(),
                       projectionTaskSubscription.getCollection(),
                       supplyTaskSubscription.getCollection());
      }
    }


  }

  private void translateTasks(Collection level2Tasks,
                              Collection level6Tasks,
                              Collection supplyTasks) {
    Collection doneL2Tasks = expander.translateLevel2Tasks(level2Tasks,
                                                           level6Tasks,
                                                           supplyTasks);
    if (!doneL2Tasks.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Level2TranslatorPlugin:There are done tasks! disposing.");
      }
      disposer.disposeAndRemoveExpansion(doneL2Tasks);
    }

  }


  private Level2Disposer getNewLevel2DisposerModule() {
    return new Level2Disposer(this);
  }

  private Level2Expander getNewLevel2ExpanderModule() {
    return new Level2Expander(this);
  }


  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    final String errorString = "Level2TranslatorPlugin requires 1 parameter, SUPPLY_TYPE and there is an optional parameter TRANSLATOR_ON with a boolean value (default is true).   As in Level2TranslatorPlugin(SUPPLY_TYPE=Ammunition,TRANSLATOR_ON=false)";
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
    String translateOn = (String) map.get(TRANSLATOR_ON);

    if (translateOn != null) {
      translateOn = translateOn.trim().toLowerCase();
      doTranslate = (translateOn.equals("true"));
    }

    if ((supplyType == null) &&
        logger.isErrorEnabled()) {
      logger.error(errorString);
    }
    return map;
  }
}




