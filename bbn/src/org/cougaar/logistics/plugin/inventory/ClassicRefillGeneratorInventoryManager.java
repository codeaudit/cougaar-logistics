package org.cougaar.logistics.plugin.inventory;

/**
 * Created by IntelliJ IDEA.
 *
 * @author gvidaver@bbn.com
 *         Date: Dec 21, 2005
 *         Time: 6:14:05 PM
 *         To change this template use File | Settings | File Templates.
 */
public interface ClassicRefillGeneratorInventoryManager {
  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#calculateRefills(java.util.Collection, org.cougaar.logistics.plugin.inventory.ComparatorModule)
   * @return days (hours?) of ost
   */
  int getOrderShipTime();

  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#calculateRefills(java.util.Collection, org.cougaar.logistics.plugin.inventory.ComparatorModule)
   * @return days (hours?) of max lead time
   */
  int getMaxLeadTime();

  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#calculateRefills(java.util.Collection, org.cougaar.logistics.plugin.inventory.ComparatorModule)
   * @return moment of prepo arrival
   */
  long getPrepoArrivalTime();

  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#calculateRefills(java.util.Collection, org.cougaar.logistics.plugin.inventory.ComparatorModule)
   * @return refill start
   */
  long getRefillStartTime();

  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#calculateRefills(java.util.Collection, org.cougaar.logistics.plugin.inventory.ComparatorModule)
   * @return supplier arrival
   */
  long getSupplierArrivalTime();

  /**
   * @see org.cougaar.logistics.plugin.inventory.RefillGenerator#nextLegalRefillBucket
   * @param today
   * @return moment of next legal refill
   */
  long getNextLegalRefillTime(long today);
}
