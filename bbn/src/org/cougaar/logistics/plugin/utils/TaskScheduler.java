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

package org.cougaar.logistics.plugin.utils;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.log.Logger;
import java.util.*;

/**
 *  A TaskScheduler instance takes as arguments a predicate
 *  and a TaskSchedulingPolicy instance.
 *  It sets up n incremental subscriptions, one corresponding to
 *  each priority level in the TaskSchedulingPolicy.
 *  The predicate for the i<sup>th</sup> incremental subscription
 *  is a logical and of the predicate given as an argument and a
 *  test whether the task has priority i.
 *  The TaskScheduler keeps track of tasks across execute cycles
 *  so that tasks are not cleared away as they normally are from
 *  a subscription (assuming the plugin properly calls initForExecuteCycle
 *  each time through).
 *  Once the plugin has finished processing a particular priority,
 *  it should clear this list explicitly.
 **/

public class TaskScheduler {

  private TaskSchedulingPolicy.Predicate outerFilter;
  private TaskSchedulingPolicy policy;
  private IncrementalSubscription[] subscriptions;
  private Logger logger;
  private BlackboardService blackboard;

  // lists of added, changed, and removed tasks that lives across
  // execute cycles; one for each priority/subscription
  private ArrayList[] addedLists;
  private ArrayList[] changedLists;
  private ArrayList[] removedLists;
  
  /**
   * Call this in the plugin's setupSubscriptions method
   * @param outerFilter selects all tasks of interest
   * @param policy prioritizes tasks
   * @param blackboard where to register for subscriptions
   */
  public TaskScheduler (TaskSchedulingPolicy.Predicate outerFilter,
                        TaskSchedulingPolicy policy,
                        BlackboardService blackboard,
                        Logger logger) {
    this.outerFilter = outerFilter;
    this.policy = policy;
    this.logger = logger;
    this.blackboard = blackboard;
    addedLists = new ArrayList [policy.numPriorities()];
    changedLists = new ArrayList [policy.numPriorities()];
    removedLists = new ArrayList [policy.numPriorities()];
    subscriptions = new IncrementalSubscription [policy.numPriorities()];
    for (int i = 0; i < subscriptions.length; i++) {
      subscriptions[i] = (IncrementalSubscription)
        blackboard.subscribe (ithPredicate (i));
      addedLists[i] = new ArrayList();
      changedLists[i] = new ArrayList();
      removedLists[i] = new ArrayList();
    }
  }

  private UnaryPredicate ithPredicate (final int priority) {
    return new UnaryPredicate() {
      public boolean execute (Object o) {
        return ((o instanceof Task) &&
                outerFilter.execute ((Task) o) &&
                policy.isPriority (priority, (Task) o));
      }
    };
  }

  /**
   * Plugins must call this each execute cycle before accessing any tasks,
   * whether or not they access any tasks that cycle
   */
  public void initForExecuteCycle() {
    for (int i = 0; i < addedLists.length; i++) {
      addedLists[i].addAll (subscriptions[i].getAddedCollection());
      changedLists[i].addAll (subscriptions[i].getChangedCollection());
      removedLists[i].addAll (subscriptions[i].getRemovedCollection());
    }
  }

  /**
   * Plugins call this at end of execute cycle if they want to be
   * requeued for execution and not have to wait for new data to
   * trigger execution.
   */
  public void requeueForExecute() {
    blackboard.signalClientActivity();
  }

  /**
   * Plugins must explicitly clear the added, changed and removed task lists
   * when they are done using them with this method
   * @param priority Lists for which priority level to clear
   */
  public void clearCollections (int priority) {
    if (priority >= addedLists.length) {
      logger.error ("Bad priority level " + priority);
      return;
    }
    addedLists[priority].clear();
    changedLists[priority].clear();
    removedLists[priority].clear();
  }

  /** All added tasks at given priority since last cleared */
  public Collection getAddedCollection (int priority) {
    return addedLists[priority];
  }

  /** All changed tasks at given priority since last cleared */
  public Collection getChangedCollection (int priority) {
    return changedLists[priority];
  }

  /** All removed tasks at given priority since last cleared */
  public Collection getRemovedCollection (int priority) {
    return removedLists[priority];
  }

  /** Tells the highest priority for which there are entries in
   *  the subscription */
  public int highestPriorityWithEntries() {
    for (int i = 0; i < addedLists.length; i++) {
      if ((addedLists[i].size() > 0) ||
          (changedLists[i].size() > 0) ||
          (removedLists[i].size() > 0))
        return i;
    }
    return TaskSchedulingPolicy.UNKNOWN_PRIORITY;
  }

}
