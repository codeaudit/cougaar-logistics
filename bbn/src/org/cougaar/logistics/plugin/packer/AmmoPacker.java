/*
 * <copyright>
 *  Copyright 1999-2003 Honeywell Inc.
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

package org.cougaar.logistics.plugin.packer;

import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;

/**
 * AmmoPacker - handles packing ammo supply requests
 *
 */
public class AmmoPacker extends Packer {

  /**
   * AmmoPacker - constructor
   */
  public AmmoPacker() {
    super();
  }

  /**
   * getTaskPredicate - returns predicate which screens for ammo supply tasks
   *
   * @return UnaryPredicate screens for incoming tasks which the packer should
   * handle
   */
  public UnaryPredicate getTaskPredicate() {
    return AmmoPackerPredicate.getInputTaskPredicate();
  }

  /**
   * getPlanElementPredicate - returns predicate which screens for plan
   * elements which will need to have allocation results set. In this case,
   * plan elements associated with Ammunition Supply tasks
   *
   * @return UnaryPredicate screens for plan elements which the packer is
   * reponsible
   */
  public UnaryPredicate getPlanElementPredicate() {
    return AmmoPackerPredicate.getPlanElementPredicate();
  }

  /*
   * getAggregationClosure - returns AggregationClosure for transporting ammo
   */
  public AggregationClosure getAggregationClosure(ArrayList tasks) {
    // BOZO - source and destination should be taken from the tasks not
    // hardcoded.
    AmmoTransport ac = new AmmoTransport();

    ac.setGenericPlugin(this);
    ac.setDestinations(tasks);

    return ac;
  }

  protected Collection groupByAggregationClosure(Collection tasks) {
    return AmmoTransport.getTransportGroups(tasks);
  }
}









