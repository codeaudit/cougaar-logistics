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
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.util.TimeSpan;
import java.util.*;

/**
 *  A TaskSchedulingPolicy instance prioritizes tasks as to which
 *  should be scheduled first.
 *  There are n priority levels, each with an accompanying predicate/test
 *  that tells whether a task is at that priority level.
 *  If a task matches the predicate for more than one level, it is
 *  assigned the highest level.
 *  Note that the highest level corresponds to the lowest number (e.g.,
 *  priority 0 is the highest priority and should be scheduled first).
 *  <p> A TaskSchedulingPolicy also can contain a sequence of time phases.
 *  This specified how to break a task into different time periods without
 *  actually expanding the task, hence allowing a plugin to handle
 *  particular time periods before others.
 **/

public class TaskSchedulingPolicy {

  public final static int UNKNOWN_PRIORITY = -1;

  // list of Predicates with 0th element corresponding to priority 0, etc.
  private Predicate[] priorityTests;

  // specifies phases of processing of tasks, including prioritization
  private PriorityPhaseMix[] ordering;

  /** Like UnaryPredicate except that object must always be a Task */
  public static interface Predicate {
    public boolean execute (Task t);
  }

  /** class that allows specification of a priority and phase jointly */
  public static class PriorityPhaseMix {
    private int priority;
    private TimeSpan timeSpan;
    public PriorityPhaseMix (int priority, TimeSpan timeSpan) {
      this.priority = priority;
      this.timeSpan = timeSpan;
    }
    public int getPriority()  { return priority; }
    public TimeSpan getTimeSpan()  { return timeSpan; }
  }

  /**
   * Constructor when using just priorities and not phases
   * @param priorityTests A set of tests that determines what priority
   * a given task is. If a task passes the test in the i<sup>th</sup>
   * entry of the array but not previous test, then the task is
   * of priority i.
   */
  public TaskSchedulingPolicy (Predicate[] priorityTests) {
    this.priorityTests = priorityTests;
    ordering = new PriorityPhaseMix [priorityTests.length];
    for (int i = 0; i < ordering.length; i++)
      ordering[i] = new PriorityPhaseMix (i, null);
  }

  /**
   * Constructor when using just phases and not priorities
   * @param phases A set of time intervals in order of how to handle
   */
  public TaskSchedulingPolicy (TimeSpan[] phases) {
    priorityTests = new Predicate[] { PASSALL };
    ordering = new PriorityPhaseMix [phases.length];
    for (int i = 0; i < ordering.length; i++)
      ordering[i] = new PriorityPhaseMix (0, phases[i]);
  }

  /**
   * Constructor specifying both priorities and phases
   */
  public TaskSchedulingPolicy (Predicate[] priorityTests,
                               PriorityPhaseMix[] ordering) {
    this.priorityTests = priorityTests;
    this.ordering = ordering;
  }

  /* Specifies a time period over which to process tasks */
  private static class TimePeriod implements TimeSpan {
    private long start;
    private long end;
    public TimePeriod (long start, long end) {
      this.start = start;
      this.end = end;
    }
    public long getStartTime()  { return start; }
    public long getEndTime()  { return end; }
  }

  /** Get a time span object */
  public static TimeSpan makeTimeSpan (long start, long end) {
    return new TimePeriod (start, end);
  }

  /**
   * Predicate that lets everything pass
   */
  public static Predicate PASSALL = new Predicate() {
    public boolean execute (Task task)  { return true; }
  };

  /** 
   * It is anticipated that the most common usage will involve
   * tests on the start time and/or detail level of a task.
   * Therefore, we provide a generic predicate that implements
   * these tests.
   */
  public static class StandardPredicate implements Predicate {
    long earliestStart;
    long latestStart;
    int maxDetailLevel;

    public StandardPredicate (long earliestStart, long latestStart,
                              int maxDetailLevel) {
      this.earliestStart = earliestStart;
      this.latestStart = latestStart;
      this.maxDetailLevel = maxDetailLevel;
    }

    public boolean execute (Task task) {
      Preference pref = task.getPreference (AspectType.START_TIME);
      if (pref == null)
        pref = task.getPreference (AspectType.END_TIME);
      if (pref == null)
        return false;
      long time =
        pref.getScoringFunction().getBest().getAspectValue().longValue();
      if ((time < earliestStart) || (time >= latestStart))
        return false;
      // ????? still need to check that detail level of task (e.g., level 2,
      // level 6, etc.) is not greater than maxDetailLevel ?????
      return true;
    }
  }

  public PriorityPhaseMix[] getOrdering() {
    return ordering;
  }

  /** number of different phases for a particular priority */
  public int numPhases (int priority) {
    int count = 0;
    for (int i = 0; i < ordering.length; i++)
      if (ordering[i].getPriority() == priority)
        count++;
    return count;
  }

  /** number of different priorities */
  public int numPriorities() {
    return priorityTests.length;
  }

  public int taskPriority (Task task) {
    return findPriority (task, 0, priorityTests.length - 1);
  }

  public boolean isPriority (int priority, Task task) {
    return findPriority (task, 0, priority) == priority;
  }

  private int findPriority (Task task, int lowest, int highest) {
    for (int i = lowest; i <= highest; i++) {
      if (priorityTests[i].execute (task))
        return i;
    }
    return UNKNOWN_PRIORITY;
  }


  /** unit test */
  public static void main (String[] args) {
  }

}
