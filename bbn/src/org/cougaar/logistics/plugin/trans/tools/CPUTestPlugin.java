/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.logistics.plugin.trans.tools;

import java.util.Iterator;

import org.cougaar.core.adaptivity.*;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import org.cougaar.core.persist.NotPersistable;

import org.cougaar.core.plugin.ComponentPlugin;

import org.cougaar.core.service.ConditionService;

import org.cougaar.util.GenericStateModelAdapter;

public class CPUTestPlugin extends ServiceUserPluginBase {
  public static final String CPU_CONDITION_NAME = "CPUTestPlugin.CPU";

  private static final OMCRange[] CPU_RANGES = {
    new CPURange(0.0, 1.0)
  };

  protected static class CPURange extends OMCRange {
    public CPURange (double a, double b) { super (a,b); }
  }

  private static final OMCRangeList CPU_VALUES = new OMCRangeList(CPU_RANGES);

  private ConditionService conditionService;

  private static final Double[] cpuValues = {
    new Double(0.4), new Double (0.7), new Double (0.99)
  };

  private int cpuStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class CPUTestCondition extends SensorCondition implements NotPersistable {
    public CPUTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public CPUTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    if (logger.isDebugEnabled()) logger.debug("setupSubscriptions called.");
    CPUTestCondition cpu =
      new CPUTestCondition(CPU_CONDITION_NAME, CPU_VALUES, cpuValues[0]);
    getBlackboardService().publishAdd(cpu);

    if (logger.isInfoEnabled() && getParameters().isEmpty()) logger.info ("plugin got NO parametes.");

    for (Iterator iter = getParameters().iterator (); iter.hasNext(); ) {
      String param = (String) iter.next();
      if (param.equals ("alwaysHigh")) {
	alwaysHigh = true;
	break;
      }
    }

    if (haveServices()) setCPUCondition();
  }

  private boolean haveServices() {
    if (conditionService != null) return true;
    if (acquireServices()) {
      if (logger.isDebugEnabled()) logger.debug(".haveServices - acquiredServices.");
      ServiceBroker sb = getServiceBroker();
      conditionService = (ConditionService)
        sb.getService(this, ConditionService.class, null);
      return true;
    }
    else if (logger.isDebugEnabled()) logger.debug(".haveServices - did NOT acquire services.");
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setCPUCondition();
      }
      else if (logger.isDebugEnabled()) 
	logger.debug(".execute - not all services ready yet.");
    }
  }

  private void setCPUCondition() {
    CPUTestCondition cpu = (CPUTestCondition)
      conditionService.getConditionByName(CPU_CONDITION_NAME);
    if (cpu != null) {

      if (alwaysHigh) {
	cpu.setValue(cpuValues[cpuValues.length-1]);
      } else {
	cpu.setValue(cpuValues[cpuStep]);
      }

      if (logger.isInfoEnabled()) 
	logger.info("Setting cpu = " + cpu.getValue());

      getBlackboardService().publishChange(cpu);
      cpuStep++;
      if (cpuStep == cpuValues.length) cpuStep = 0;
    }
    startTimer(10000);
  }

  protected boolean alwaysHigh = false;
}
