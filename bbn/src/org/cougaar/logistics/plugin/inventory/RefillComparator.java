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

import java.util.ArrayList;

/** The Refill Comparator Module is responsible for deciding whether to
 *  rescind all previous refills and publish all new refills generated
 *  by the total replan refill generator  module or whether to 
 *  compare and merge the 'old' and 'new' refill tasks.  The first
 *  version will simply rescind all old refills and publish all new
 *  refills.
 *  Called by the Refill Generator(?) with the new refills.
 *  Uses the InventoryBG module to get the old refills.
 *  Publishes new Refill tasks through the InventoryPlugin.
 **/

public class RefillComparator extends InventoryModule {
  private ArrayList newRefills;
  private ArrayList oldRefills;

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillComparator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  public void compareRefills(ArrayList newRefills) {
    this.newRefills = newRefills;
    // get the old Refills for the affected inventory bin(s)
    //imPlugin.publishRemove(oldRefills);
    //imPlugin.publishAdd(newRefills);
  }
                       


}
    
  
  
