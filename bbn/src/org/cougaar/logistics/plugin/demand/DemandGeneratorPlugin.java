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
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.reflect.Constructor;

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
  private DGClass9Scheduler      class9Scheduler;

  private String supplyType;
  private long frequency;

  public final String SUPPLY_TYPE = "SUPPLY_TYPE";
  public final String GENERATE_FREQUENCY = "GENERATE_FREQUENCY";

  public final String DEMAND_GENERATOR = "DEMAND_GENERATOR";

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
    if (domainService != null) {
      getServiceBroker().releaseService(this, DomainService.class, domainService);
    }
  }

  public void setupSubscriptions() {
    //nothing for now
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
    //nothing for now
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

  /**
   Read the Plugin parameters(Accepts key/value pairs)
   Initializes supplyType and inventoryFile
   **/
  private HashMap readParameters() {
    final String errorString = "DemandGeneratorPlugin requires 2 parameters, Supply Type and Gemerate Frequency.";
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

    if (((supplyType == null) ||
         (frequency <= 0) &&
         logger.isErrorEnabled()))
    {
      logger.error(errorString);
    }
    return map;
  }


}


