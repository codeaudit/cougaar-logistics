package org.cougaar.logistics.plugin.inventory;

/**
 * Created by IntelliJ IDEA.
 *
 * @author gvidaver@bbn.com
 *         Date: Dec 22, 2005
 *         Time: 7:46:15 PM
 *         To change this template use File | Settings | File Templates.
 */
public interface LevelOfDetailInventoryManager {
  /**
   * @see AllocationAssessor#createPhasedAllocationResult
   * @see AllocationAssessor#reconcileInventoryLevels(java.util.Collection)
   * @return time of end of level 2
   */
  long getEndOfLevelTwo();

  /**
   * @see RefillProjectionGenerator#calculateLevelSixProjections
   * @return refill start
   */
  long getRefillStartTime();

  /**
   * @see RefillProjectionGenerator#calculateLevelSixProjections
   * @return days (hours?) of ost
   */
  int getOrderShipTime();
}
