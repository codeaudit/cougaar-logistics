// Copyright (10/99) Honeywell Inc.
// Unpublished - All rights reserved. This software was developed with funding 
// under U.S. government contract MDA972-97-C-0800

package org.cougaar.logistics.plugin.packer; 

import java.util.*;

import org.cougaar.planning.ldm.plan.AllocationResultDistributor;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;

import org.cougaar.util.Sortings;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Ammunition;
import org.cougaar.glm.packer.Packer;
import org.cougaar.glm.packer.AggregationClosure;
import org.cougaar.glm.packer.AmmoTransport;

/**
 * LowFidelityAmmoPacker - handles packing ammo supply requests
 * 
 */
public class LowFidelityAmmoPacker extends Packer {

  /**
   * LowFidelityAmmoPacker - constructor 
   */
  public LowFidelityAmmoPacker() {
    super();
  }

  /**
   * getTaskPredicate - returns predicate which screens for ammo supply tasks
   * 
   * @return UnaryPredicate screens for incoming tasks which the packer should
   * handle
   */
  public UnaryPredicate getTaskPredicate() {
    return LowFidelityAmmoPackerPredicate.getInputTaskPredicate();
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
    return LowFidelityAmmoPackerPredicate.getPlanElementPredicate();
  }

  /*
   * getAggregationClosure - returns AggregationClosure for transporting ammo
   */
  public AggregationClosure getAggregationClosure(ArrayList tasks) {
    // BOZO - source and destination should be taken from the tasks not
    // hardcoded.
    AmmoTransport ac = new AmmoTransport(tasks);

    ac.setGenericPlugin(this);
    
    return ac;
  }

  protected Collection groupByAggregationClosure(Collection tasks) {
    return AmmoTransport.getTransportGroups(tasks);
  }
}









