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

/** The Refill Projection Generator Module is responsible for generating 
 *  projection refill tasks.  These projections will be calculated by
 *  time shifting the projections from each customer and summing the
 *  results.
 *  (?) Not sure if this should be a total replan module like the
 *  refill generator...
 *  Called by the Inventory Plugin when there is new projection demand.
 *  Uses the InventoryBG module to gather projected demand.
 *  Generates Refill Projection tasks 
 **/

public class RefillProjectionGenerator extends InventoryModule {

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillProjectionGenerator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  public void calculateRefillProjections() {
    // get the demand projections for each customer from bg
    // time shift the demand for each customer
    // sum the each customer's time shifted demand
    // ??? take into account previous projections
    // ??? do we need to send this to a comparator
    // publish the new projections to the Inventory Plugin
  }

}
    
  
  
