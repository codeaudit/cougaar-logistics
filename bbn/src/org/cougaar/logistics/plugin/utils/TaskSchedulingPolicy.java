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
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.util.TimeSpan;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import java.util.*;
import java.io.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AlarmService;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

  /** Predicate that lets everything pass */
  public static Predicate PASSALL = new Predicate() {
    public boolean execute (Task task)  { return true; }
  };

  /** Predicate that tests if task is for level 2 detail */
  public static class Level2Predicate implements Predicate {
    private boolean isLevel2;
    private TaskUtils taskUtils;
    public Level2Predicate (boolean isLevel2, TaskUtils taskUtils) {
      this.isLevel2 = isLevel2;
      this.taskUtils = taskUtils;
    }
    public boolean execute (Task task) {
      return (! isLevel2) ^ taskUtils.isLevel2 (task);
    }
  }

  /** Predicate that tests if task is within given time span */
  public static class TimeSpanPredicate implements Predicate {
    private long earliestStart;
    private long latestStart;
    public TimeSpanPredicate (long earliestStart, long latestStart) {
      this.earliestStart = earliestStart;
      this.latestStart = latestStart;
    }
    public boolean execute (Task task) {
      Preference pref = task.getPreference (AspectType.START_TIME);
      if (pref == null)
        pref = task.getPreference (AspectType.END_TIME);
      if (pref == null)
        return false;
      long time =
        pref.getScoringFunction().getBest().getAspectValue().longValue();
      return (time >= earliestStart) && (time < latestStart);
    }
  }

  public static TaskSchedulingPolicy fromXML
      (String filename, UtilsProvider plugin, AlarmService alarm) {
    try {
      return fromXML (new FileInputStream (filename), plugin, alarm);
    } catch (Exception e) {
      plugin.getLoggingService(plugin).error
          ("Could not open file " + filename);
      return null;
    }
  }

  public static TaskSchedulingPolicy fromXML
      (InputStream stream, UtilsProvider plugin, AlarmService alarm) {
    try {
      SAXParser parser = new SAXParser();
      ParmsHandler handler = new ParmsHandler (plugin, alarm);
      parser.setContentHandler (handler);
      parser.parse (new InputSource (stream));
      return handler.getPolicy();
    } catch (Exception e) {
      plugin.getLoggingService(plugin).error (e.getMessage());
      return null;
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

  private static class ParmsHandler extends DefaultHandler {
    private ArrayList criteria = null;
    private ArrayList phases = null;
    private ArrayList ordering = null;
    private TaskUtils taskUtils;
    private TimeUtils timeUtils;
    private long now;
    private TaskSchedulingPolicy policy = null;

    public ParmsHandler (UtilsProvider plugin, AlarmService alarm) {
      taskUtils = plugin.getTaskUtils();
      timeUtils = plugin.getTimeUtils();
      now = alarm.currentTimeMillis();
    }

    public TaskSchedulingPolicy getPolicy()  { return policy; }

    public void startElement (String uri, String local,
                     String name, Attributes atts) throws SAXException {
      if (name.equals ("PRIORITIES")) {
        criteria = new ArrayList();
      }
      else if (name.equals ("PHASES")) {
        phases = new ArrayList();
      }
      else if (name.equals ("ORDERING")) {
        if ((criteria == null) || (phases == null))
          throw new SAXException ("In scheduling policy, should " +
                "have criteria and phases defined before ordering");
        ordering = new ArrayList();
      }
      else if (name.equals ("CRITERION")) {
        String passAll = atts.getValue ("passall");
        String level = atts.getValue ("level");
        String earliest = atts.getValue ("earliest");
        String latest = atts.getValue ("latest");
        if (passAll != null)
          criteria.add (PASSALL);
        else if (level != null)
          criteria.add (new Level2Predicate ("2".equals (level.trim()),
                                             taskUtils));
        else if ((earliest != null) || (latest != null)) {
          try {
            int t1 = (earliest == null) ? 0 : Integer.parseInt (earliest);
            int t2 = (latest == null) ? 0 : Integer.parseInt (latest);
            criteria.add (new TimeSpanPredicate
              (timeUtils.addNDays (now, t1), timeUtils.addNDays (now, t2)));
          } catch (NumberFormatException e) {
            throw new SAXException ("In scheduling policy criterion, the " +
             "values of earliest and latest of a criterion must be integers");
          }
        }
        else
          throw new SAXException ("In scheduling policy criterion, " +
                "need to define at least one expected attribute");
      }
      else if (name.equals ("PHASE")) {
        String start = atts.getValue ("start");
        String end = atts.getValue ("end");
        if ((start == null) || (end == null))
          throw new SAXException ("In scheduling policy criterion, " +
                "must define start and end attributes for a phase");
        try {
          int istart = Integer.parseInt (start);
          int iend = Integer.parseInt (end);
          phases.add (makeTimeSpan (timeUtils.addNDays (now, istart),
                                    timeUtils.addNDays (now, iend)));
        } catch (NumberFormatException e) {
          throw new SAXException ("In scheduling policy criterion, " +
             "the values of start and end of a phase must be integers");
        }
      }
      else if (name.equals ("PRIORITYPHASEMIX")) {
        String priority = atts.getValue ("priority");
        String phase = atts.getValue ("phase");
        if ((priority == null) || (phase == null))
          throw new SAXException ("In scheduling policy criterion, " +
            "must define priority and phase attributes for a priphasemix");
        try {
          int ipri = Integer.parseInt (priority);
          int iphase = Integer.parseInt (phase);
          ordering.add (new PriorityPhaseMix
                             (ipri, (TimeSpan) phases.get (iphase)));
        } catch (NumberFormatException e) {
          throw new SAXException ("In scheduling policy criterion, the " +
            "values of priority and phase of a priphasemix must be integers");
        }
      }
    }

    public void endElement (String uri, String local,
                            String name) {
      if (name.equals ("POLICY")) {
        if (ordering != null)
          policy = new TaskSchedulingPolicy
            ((Predicate[]) criteria.toArray (new Predicate [criteria.size()]),
             (PriorityPhaseMix[]) ordering.toArray
               (new PriorityPhaseMix [ordering.size()]));
        else if (phases != null)
          policy = new TaskSchedulingPolicy
            ((TimeSpan[]) phases.toArray (new TimeSpan [phases.size()]));
        else if (criteria != null)
          policy = new TaskSchedulingPolicy
            ((Predicate[]) criteria.toArray
                           (new Predicate [criteria.size()]));
      }
    }
  }

}
