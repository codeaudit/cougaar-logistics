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
 
package org.cougaar.logistics.servlet;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.LDMServesPlugin;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.SchedulerService;

import org.cougaar.planning.servlet.BlackboardServletSupport;

import org.cougaar.util.ConfigFinder;

/** 
 * <pre>
 * This support class offers additional services on top of the
 * SimpleServletSupport class, including access to the blackboard,
 * config finder, root factory, ldm serves plugin, and scheduler service.
 *
 * Publishes a condition that must be in the range 0.0-1.0.
 *
 * The name is a parameter to the component.  If none is provided, the name
 * is "DoubleCondition".
 * </pre>
 */
public class ConditionSupport extends BlackboardServletSupport {
  public ConditionSupport(
      String path,
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery,
      NamingService ns,
      LoggingService logger,
      BlackboardService blackboard,
      ConfigFinder configFinder,
      PlanningFactory ldmf,
      LDMServesPlugin ldm,
      SchedulerService scheduler,
      String conditionName) {
    super (path, agentId, blackboardQuery, ns, logger, blackboard, configFinder, ldmf, ldm, scheduler);
    this.conditionName = conditionName;

    publishCondition ();
  }

  /** publishes the condition to blackboard, if the condition service is available. */
  public void publishCondition () {
    ConditionServlet.DoubleCondition doubleCondition = 
      new ConditionServlet.DoubleCondition(conditionName);

    try {
      getBlackboardService().openTransaction();
      getBlackboardService().publishAdd(doubleCondition);
      setCondition (doubleCondition);
      if (getLog().isInfoEnabled())
	getLog().info (getAgentIdentifier() + " - published condition " + doubleCondition);
    } 
    catch (Exception exc) {
      getLog().error ("Could not publish double condition???", exc);
    }
    finally{
     getBlackboardService().closeTransactionDontReset();
    }  
  }

  protected void setCondition (ConditionServlet.DoubleCondition condition) { this.condition = condition; }
  /** not usually needed */
  public ConditionServlet.DoubleCondition getCondition () { return condition; }

  protected String getConditionName () { return conditionName; }

  protected ConditionServlet.DoubleCondition condition;
  protected String conditionName;
}
