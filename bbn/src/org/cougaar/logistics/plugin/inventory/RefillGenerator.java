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

  //need our suppliers OST from somewhere
  //need the reorder period policy
  //tell the inventorybg to clear the refills
  //ask the bg (or another module) what the reorder level is
  //pick a starting day
  //ask the bg for the inventory level for that day.
  //if the inv level is below the reorder level create a refill.


}
    
  
  
