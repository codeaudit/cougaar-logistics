package org.cougaar.logistics.plugin.inventory;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.core.mts.MessageAddress;

import java.util.List;

/**
 * What the inventory plugin exports as methods to the modules
 */
public interface InventoryManager extends UtilsProvider {
  String getSupplyType();

  /**
   * @see InventoryLevelGenerator#getTargetLevel
   * @return org name
   */
  String getOrgName();
  Organization getMyOrganization();
  MessageAddress getClusterId();

  /**
   * @see SupplyExpander#updateChangedProjections
   * @see SupplyExpander#updateChangedRequisitions
   * @return oplan arrival
   */
  long getOPlanArrivalInTheaterTime();

  /**
   * @see TaskUtils#changeDatePrefs
   * @return end of the oplan
   */
  long getOPlanEndTime();
  long getCurrentTimeMillis();

  PlanningFactory getPlanningFactory();

  void publishAdd(Object object);
  void publishAddExpansion(Expansion expansion);
  void publishChange(Object object);

  /**
   * @see ExternalAllocator#rescindTaskAllocations
   * @see ReconcileSupplyExpander#expandAndDistributeRequisitions
   * @see RefillComparator#compareRefills
   * @param object
   */
  void publishRemove(Object object);

  /**
   * Called by the RefillGenerator to hook up the refill task to the maintain
   * inventory parent task and workflow.
   * @see DiffBasedComparator#compareRefills
   * @see DiffBasedComparator#compareRefillProjections
   */
  boolean publishRefillTask(Task task, Inventory inventory);

  /**
   * Called by DiffBasedComparator
   * @see DiffBasedComparator#compareRefills
   * @param taskToRemove
   */
  void removeSubTask(Task taskToRemove);

  /**
   * @see ExternalAllocator#findBestSource(org.cougaar.planning.ldm.plan.Task)
   * @param task
   * @return List of 0 or 2 TimeSpans
   */
  List getNewTaskSplitTimes(Task task);

  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#calculateRefills(java.util.Collection, org.cougaar.logistics.plugin.inventory.ComparatorModule)
   */
  void disposeOfUnusedMILTask(Inventory inventory, boolean noRefills);

  /**
   * @see SupplyExpander#getLogisticsInventoryPG
   * @param taskWithInventory
   * @param inventory
   */
  void touchInventoryForTask (Task taskWithInventory, Inventory inventory);

  /**
   * Not called in AL.
   * @param t
   * @return (new) inventory
   */
  Inventory findOrMakeInventory(Task t);

  /**
   * @see ExternalAllocator#updateAllocationResult(java.util.Collection)
   * @see SupplyExpander#getLogisticsInventoryPG(org.cougaar.planning.ldm.plan.Task)
   * @see TaskUtils#splitProjection(org.cougaar.planning.ldm.plan.Task, java.util.List, InventoryManager)
   * @param a
   * @return (new) inventory
   */
  Inventory findOrMakeInventory(Asset a);
}
