package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.agent.service.alarm.Alarm;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 *
 * @author gvidaver@bbn.com
 *         Date: Dec 23, 2005
 *         Time: 2:48:14 PM
 *         To change this template use File | Settings | File Templates.
 */
public interface ReconcileSupplyExpanderInventoryManager {
  BlackboardService getBBService();
  Collection getCommStatusSubscription();

  /**
   * @see ReconcileSupplyExpander#findLastSupplyTaskTime(String)
   * @return all supply tasks
   */
  Collection getSupplyTasks();
  /**
   * @see ReconcileSupplyExpander#determineCommStatus
   * @param timeOut
   * @return Alarm
   */
  Alarm addAlarm(long timeOut);
}
