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
 * </pre>
 */
public class CPUSettingSupport extends BlackboardServletSupport {
  public CPUSettingSupport(
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
      ConditionService condition) {
    super (path, agentId, blackboardQuery, ns, logger, blackboard, configFinder, ldmf, ldm, scheduler);
    this.conditionService = condition;
    
    if (getLog().isInfoEnabled())
      getLog().info (getAgentIdentifier() + " - Publishing CPU condition : " + 
		     CPUSettingServlet.CPU_CONDITION_NAME);

    CPUSettingServlet.CPUCondition cpu = new CPUSettingServlet.CPUCondition();

    try {
      getBlackboardService().openTransaction();
      getBlackboardService().publishAdd(cpu);
    } 
    catch (Exception exc) {
      getLog().error ("Could not publish cpu condition???", exc);
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

  protected ConditionService conditionService;
}
