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
 
package org.cougaar.logistics.servlet;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.LDMServesPlugin;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.wp.WhitePagesService;

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
      LoggingService logger,
      BlackboardService blackboard,
      ConfigFinder configFinder,
      PlanningFactory ldmf,
      LDMServesPlugin ldm,
      SchedulerService scheduler,
      WhitePagesService wp,
      String conditionName) {
    super (path, agentId, blackboardQuery, logger, blackboard, configFinder, ldmf, ldm, scheduler);
    this.wp = wp;
    this.conditionName = conditionName;

    publishCondition ();
  }

  public WhitePagesService getWhitePagesService() { return wp; }

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
  protected WhitePagesService wp;
  protected String conditionName;
}
