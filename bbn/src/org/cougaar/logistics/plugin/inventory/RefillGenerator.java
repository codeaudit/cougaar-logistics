/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.glm.ldm.asset.Inventory;

import java.util.ArrayList;
import java.util.Iterator;

/** The Refill Generator Module is responsible for generating new
 *  refill tasks as needed when new demand is received.  This Refill 
 *  Generator uses a total replan algorithm which ignores all previously
 *  generated refill tasks (that are not yet committed) and replans by
 *  creating new refills based on the new due out totals and the 
 *  inventory levels.  The Comparator module will decide whether to
 *  rescind all previous refills and publish all new refills generated
 *  by this module or whether to compare and merge the 'old' and
 *  'new' refill tasks.
 *  Called by the Inventory Plugin when there is new demand.
 *  Uses the InventoryBG module for inventory bin calculations
 *  Generates Refill tasks which are passed to the Comparator module
 *  (or does the Inventory plugin do that for us?)
 **/

public class RefillGenerator extends InventoryModule {

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillGenerator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  public ArrayList calculateRefills(ArrayList touchedInventories, int advanceOrderTime,
                                    int orderFrequency) {
    ArrayList newRefills = new ArrayList();
    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      Inventory anInventory = (Inventory) tiIter.next();
      //LogisticsInventoryBG theBG = (LogisticsInventoryBG) anInventory.getLogisticsInventoryPG().
      //  getLogisticsInventoryBG();
      //clear the refills
      //theBG.clearRefills();

      //ask the bg (or another module) what the reorder level is
      //int reorderLevel = theBG.getReorderLevel();

      //pick a starting day - probably today
      //ask the bg for the inventory level for that day.
      //int invLevel = theBG.getInventoryLevel(today);

      //if the inv level is below the reorder level create a refill.
      //if (invLevel < reorderLevel) {
      //  create the refills
      //  add refills to newRefills
      //}
    
    } // done going through inventories
    return newRefills;
  }

}
    
  
  
