/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.agent.service.alarm.Alarm;


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
   * Remove Withdraw task from
   * the inventory's behavior group.
   **/
  void handleRemovedRequisitions(Collection tasks);
  /**
   * Respond to removed dispostions, e.g, remove related predictions.
   * @param dispositions
   */
  void handleRemovedDispositions(Collection dispositions);

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
  
  /**
   * Check the alarm status during communications loss.
   */
  void checkCommStatusAlarms();

  /**
   * Determine the state of comms, e.g., is it up/down, did it come back up?
   * @param commStatusSub
   * @param tasks
   */
  void determineCommStatus(IncrementalSubscription commStatusSub, Collection tasks);
}
