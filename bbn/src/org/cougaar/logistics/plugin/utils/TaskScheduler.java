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

package org.cougaar.logistics.plugin.utils;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.TimeSpan;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.log.Logger;
import org.cougaar.core.blackboard.Publishable;
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
 *  The TaskScheduler handles the bookkeeping associated with
 *  stepping through the various phases of processing, based on
 *  both priorities and time intervals, specified in the policy.
 *  The plugin must call initForExecuteCycle before processing the
 *  tasks during each execute cycle and finishedExecuteCycle after
 *  processing the tasks.
 **/

public class TaskScheduler {

  private TaskSchedulingPolicy.Predicate outerFilter;
  private TaskSchedulingPolicy policy;
  private IncrementalSubscription[] subscriptions;
  private Logger logger;
  private QuiescenceAccumulator quiescence;
  private BlackboardService blackboard;
  private String id;
  private Storage storage;

  // lists of added, changed, and removed tasks that lives across
  // execute cycles; one for each priority/subscription
  private ArrayList[][] addedLists;
  private ArrayList[][] changedLists;
  private ArrayList[][] removedLists;

  // current phase of processing
  private int currentPhase;
  
  /**
   * Call this in the plugin's setupSubscriptions method
   * @param outerFilter selects all tasks of interest
   * @param policy prioritizes tasks
   * @param blackboard where to register for subscriptions
   * @param id an identifier for this scheduler that is unique to this agent
   */
  public TaskScheduler (TaskSchedulingPolicy.Predicate outerFilter,
                        TaskSchedulingPolicy policy,
                        BlackboardService blackboard,
                        QuiescenceAccumulator quiescence,
                        Logger logger,
                        String id) {
    this.outerFilter = outerFilter;
    this.policy = policy;
    this.logger = logger;
    this.blackboard = blackboard;
    this.quiescence = quiescence;
    this.id = id;
    subscriptions = new IncrementalSubscription [policy.numPriorities()];
    for (int i = 0; i < subscriptions.length; i++) 
      subscriptions[i] = (IncrementalSubscription)
        blackboard.subscribe (ithPredicate (i));
    Collection prev = blackboard.query (new UnaryPredicate() {
      public boolean execute (Object o) {
        if (! (o instanceof Storage))
          return false;
        return TaskScheduler.this.id.equals (((Storage) o).id);
      }});
    if (prev.size() == 0) {
      addedLists = setupLists();
      changedLists = setupLists();
      removedLists = setupLists();
      resetCurrentPhase();
      storage = new Storage();
      updateStorage();
      blackboard.publishAdd (storage);
    } else {
      storage = (Storage) prev.iterator().next();
      addedLists = storage.addedLists;
      changedLists = storage.changedLists;
      removedLists = storage.removedLists;
      currentPhase = storage.currentPhase;
    }
  }

  private ArrayList[][] setupLists() {
    ArrayList[][] lists = new ArrayList [policy.numPriorities()][];
    for (int i = 0; i < policy.numPriorities(); i++) {
      int numPhases = policy.numPhases (i);
      if (numPhases == 0)
	if(logger.isErrorEnabled()) {
          logger.error ("There are no phases for priority " + i +
                        " in a task scheduler.");
        }
      lists[i] = new ArrayList [numPhases];
      for (int j = 0; j < numPhases; j++)
        lists[i][j] = new ArrayList();
    }
    return lists;
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
    if(logger.isDebugEnabled()) {
      logger.debug("in initForExecuteCycle");
    }
    for (int i = 0; i < addedLists.length; i++) {
      addItems (addedLists[i], subscriptions[i].getAddedCollection());
      addItems (changedLists[i], subscriptions[i].getChangedCollection());
      addItems (removedLists[i], subscriptions[i].getRemovedCollection());
    }
  }

  private void addItems (ArrayList[] lists, Collection items) {
    if (lists.length == 0)
      return;
    lists[0].addAll (items);
    if (! items.isEmpty()) {
      currentPhase = 0;
      if(logger.isDebugEnabled()) {
        logger.debug("in TaskScheduler addItems, calling clearQuiescentState");
      }
      quiescence.clearQuiescentState (this);
    }
  }

  // Default case - assumes that plugin processed tasks off of the task scheduler lists.
  public void finishedExecuteCycle() {
    finishedExecuteCycle(true);
  }


  /**
   * Plugins call this at end of execute cycle
   *
   * @param didProcessing true if the plugin processed the tasks and it's now safe to remove them
   *  from the task scheduler's task queue.
   */
  public void finishedExecuteCycle(boolean didProcessing) {
    if(logger.isDebugEnabled()) {
      logger.debug("in finishedExecuteCycle");
    }
    if (isEmpty()){
      return;
    }
    //only do these steps if we actually processed stuff (note the default is that we did)
    if (didProcessing) {
      shiftCollections(); //lists might be cleared here
      currentPhase++;
    }
    // only requeue for execution if more to do
    if (currentPhase < policy.getOrdering().length) {
      blackboard.signalClientActivity();
      if(logger.isDebugEnabled()) {
        logger.debug("in finishedExecuteCycle, currentPhase: " + currentPhase +
		     " policy.getOrdering().length: " + policy.getOrdering().length);
      }
    } else {
      resetCurrentPhase();
      if(logger.isDebugEnabled()) {
        logger.debug("in TaskScheduler addItems, calling setQuiescentState");
      }
      quiescence.setQuiescentState (this);
    }
    updateStorage();
    blackboard.publishChange (storage);
  }

  private void updateStorage() {
    storage.addedLists = addedLists;
    storage.changedLists = changedLists;
    storage.removedLists = removedLists;
    storage.currentPhase = currentPhase;
    storage.id = id;
  }

  private static class Storage implements java.io.Serializable, Publishable {
    public String id;
    public int currentPhase;
    public ArrayList[][] addedLists;
    public ArrayList[][] changedLists;
    public ArrayList[][] removedLists;
    public boolean isPersistable()  { return true; }
  }

  private void resetCurrentPhase() {
    currentPhase = policy.getOrdering().length;
  }

  /**
   * Clear out all the state information from the task scheduler
   */
  public void clearState() {
    resetCurrentPhase();
    for (int i = 0; i < addedLists.length; i++) {
      for (int j = 0; j < addedLists[i].length; j++) {
        addedLists[i][j].clear();
        changedLists[i][j].clear();
        removedLists[i][j].clear();
      }
    }
    quiescence.setQuiescentState(this);
  }

  /** check if anything to do in this scheduler */
  public boolean isEmpty() {
    return currentPhase >= policy.getOrdering().length;
  }

  /** Tell the priority associated with the current processing phase */
  public int getCurrentPriority() {
    if (isEmpty())
      return TaskSchedulingPolicy.UNKNOWN_PRIORITY;
    return policy.getOrdering()[currentPhase].getPriority();
  }

  /**
   * Tell the time interval associate with the current phase
   */
  public TimeSpan getCurrentTimeSpan() {
    if (isEmpty())
      return null;
    return policy.getOrdering()[currentPhase].getTimeSpan();
  }

  private int getCurrentPhase() {
    int pri = getCurrentPriority();
    int phase = 0;
    for (int i = 0; i < currentPhase; i++)
      if (policy.getOrdering()[i].getPriority() == pri)
        phase++;
    return phase;
  }

  private void shiftCollections() {
    int pri = getCurrentPriority();
    int phase = getCurrentPhase();
    if (phase != (addedLists[pri].length - 1)) {
      addedLists[pri][phase+1].addAll (addedLists[pri][phase]);
      changedLists[pri][phase+1].addAll (changedLists[pri][phase]);
      removedLists[pri][phase+1].addAll (removedLists[pri][phase]);
    }
    addedLists[pri][phase].clear();
    changedLists[pri][phase].clear();
    removedLists[pri][phase].clear();
  }

  /** iterates over all tasks */
  public Iterator iterator() {
    if (isEmpty())
      return (new ArrayList(0)).iterator();
    ArrayList al = new ArrayList (getAddedCollection());
    al.addAll (getChangedCollection());
    al.addAll (getRemovedCollection());
    return al.iterator();
  }

  /** All added tasks at given priority since last cleared */
  public Collection getAddedCollection() {
    if (isEmpty())
      return new ArrayList(0);
    return addedLists[getCurrentPriority()][getCurrentPhase()];
  }

  /** All changed tasks at given priority since last cleared */
  public Collection getChangedCollection() {
    if (isEmpty())
      return new ArrayList(0);
    return changedLists[getCurrentPriority()][getCurrentPhase()];
  }

  /** All removed tasks at given priority since last cleared */
  public Collection getRemovedCollection() {
    if (isEmpty())
      return new ArrayList(0);
    return removedLists[getCurrentPriority()][getCurrentPhase()];
  }

  /** All tasks in the lists */
  public Iterator getAllTasks() {
    ArrayList al = new ArrayList();
    for (int i = 0; i < addedLists.length; i++) {
      for (int j = 0; j < addedLists[i].length; j++) {
        al.addAll (addedLists[i][j]);
        al.addAll (changedLists[i][j]);
        al.addAll (removedLists[i][j]);
      }
    }
    return al.iterator();
  }

  public Collection getAllTasksCollection() {
    List allTasks = new ArrayList();
    for (int i = 0; i < subscriptions.length; i ++) {
      allTasks.addAll(subscriptions[i]);
    }
    return allTasks;
  }
}
