package org.cougaar.logistics.servlet;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.domain.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.SchedulerService;

import org.cougaar.core.servlet.SimpleServletSupportImpl;
import org.cougaar.core.servlet.BlackboardServletSupport;

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
public class ConditionSupport extends BlackboardServletSupport implements ServiceRevokedListener {
  public ConditionSupport(
      String path,
      ClusterIdentifier agentId,
      BlackboardQueryService blackboardQuery,
      NamingService ns,
      LoggingService logger,
      BlackboardService blackboard,
      ConfigFinder configFinder,
      RootFactory ldmf,
      LDMServesPlugin ldm,
      SchedulerService scheduler,
      ConditionService condition,
      ServiceBroker broker,
      String conditionName) {
    super (path, agentId, blackboardQuery, ns, logger, blackboard, configFinder, ldmf, ldm, scheduler);
    this.conditionService = condition;
    this.conditionName = conditionName;
    this.broker = broker;

    publishCondition ();
  }

  /** publishes the condition to blackboard, if the condition service is available. */
  public void publishCondition () {
    if (didPublish)
      return;

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
    
    didPublish = true;
  }

  /**
   * Get the condition service. <p>
   *
   * @return null if there is no condition service provider in this agent
   */
  protected ConditionService getConditionService() {
    if (conditionService == null)
      publishCondition (); // may fail if configuration is bad

    return conditionService;
  }

  /** condition service went away? */
  public void serviceRevoked(ServiceRevokedEvent re) {
    conditionService = null;
  }

  protected void setCondition (ConditionServlet.DoubleCondition condition) { this.condition = condition; }
  /** not usually needed */
  public ConditionServlet.DoubleCondition getCondition () { return condition; }

  protected String getConditionName () { return conditionName; }

  protected ConditionService conditionService;
  protected ConditionServlet.DoubleCondition condition;
  protected String conditionName;
  protected ServiceBroker broker = null;

  protected boolean didPublish = false;
}
