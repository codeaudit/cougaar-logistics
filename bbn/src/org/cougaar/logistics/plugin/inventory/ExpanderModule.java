/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;


/**
 * The ExpanderModule dictates the functionality needed by the
 * {@link InventoryPlugin}.  The Expander should extend the
 * InventoryModule for convenience and call super(InventoryPlugin).
 * The constructor of the Expander will take a single argument, a
 * reference to the plugin.
 **/
public interface ExpanderModule {

  /** 
   * Expand Supply tasks into Withdraw tasks.  The Withdraw tasks are
   * added to the inventory asset's behavior group.
   **/
  void expandAndDistributeRequisitions(Collection tasks);
  
  /** 
   * Expand ProjectSupply tasks into ProjectWithdraw tasks.  The
   * ProjectWithdraw tasks are added to the inventory asset's 
   * behavior group.
   * @return true if new projections were processed
   **/
  boolean expandAndDistributeProjections(Collection tasks);
  
  /**
   * Given removed ProjectSupply tasks, remove ProjectWithdraw
   * child task from the inventory's behavior group.
   * @return true if removed projections were processed.
   **/
  boolean handleRemovedProjections(Collection tasks);
  
  /**
   * Given removed Supply tasks, remove Withdraw child task from
   * the inventory's behavior group.
   **/
  void handleRemovedRequisitions(Collection tasks);
  
  /** 
   * Given changed Supply tasks, Withdraw tasks are updated and
   * changed in the inventory's behavior group.
   **/
  void updateChangedRequisitions(Collection tasks);
  
  /** 
   * Given changed ProjectSupply tasks, ProjectWithdraw tasks are updated
   * and changed in the inventory's behavior group.
   **/
  void updateChangedProjections(Collection tasks);
  
  /** 
   * Given a subscription to the expansions, update the 
   * PlanElements>
   **/
  void updateAllocationResult(IncrementalSubscription sub);
}

