package org.cougaar.logistics.servlet;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.domain.*;

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
public class ConditionSupport extends BlackboardServletSupport {
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
      String conditionName) {
    super (path, agentId, blackboardQuery, ns, logger, blackboard, configFinder, ldmf, ldm, scheduler);
    this.conditionService = condition;
    this.conditionName = conditionName;

    if (getLog().isInfoEnabled())
      getLog().info (getAgentIdentifier() + " - Publishing condition : " + conditionName);

    if (conditionService == null)
      getLog().warn (getAgentIdentifier() + " - No condition service available - will not be able to set a condition in this agent.\n"+
		     "Consider loading the ConditionServiceProvider as an agent component OR before the servlet.");

    ConditionServlet.DoubleCondition doubleCondition = 
      new ConditionServlet.DoubleCondition(conditionName);

    try {
      getBlackboardService().openTransaction();
      getBlackboardService().publishAdd(doubleCondition);
    } 
    catch (Exception exc) {
      getLog().error ("Could not publish double condition???", exc);
    }
    finally{
     getBlackboardService().closeTransactionDontReset();
    }  
  }

  /**
   * Get the condition service
   */
  protected ConditionService getConditionService() {
    return conditionService;
  }

  protected String getConditionName () { return conditionName; }

  protected ConditionService conditionService;
  protected String conditionName;
}
