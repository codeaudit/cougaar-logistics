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

//utils

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is the PreferenceAggregator used by the packer created by
 * HTC.  The set of preferences it creates is set up to meet the
 * needs of the TOPS MCCGlobalMode cluster that will be receiving
 * the tasks the packer creates.
 */
public class DefaultPreferenceAggregator implements PreferenceAggregator {
  // Start time is set to 40 days prior to the specified end time
  private static long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
  private static long START_DECREMENT = 40 * MILLIS_PER_DAY;

  /* Increments added to specified end time to come up with an earliest,
   * best, and latest for TOPS. Distribution specified by
   * tops team
   */
  private static long EARLIEST_INCREMENT = -(21 * MILLIS_PER_DAY);
  private static long BEST_INCREMENT = 0 * MILLIS_PER_DAY;
  private static long LATEST_INCREMENT = 0 * MILLIS_PER_DAY;

  /**
   * Will create a preference as follows:
   * START_TIME should be at or greater than 0.0
   * END_TIME should be bracketed around the earliest END_TIME of
   * the input tasks and
   * QUANTITY should be set at the sum of the quantities of the
   * input tasks.
   */
  public ArrayList aggregatePreferences(Iterator tasks, PlanningFactory rootFactory) {

    ArrayList prefs = new ArrayList();
    double endTime = java.lang.Double.POSITIVE_INFINITY;
    double startTime = 0.0;
    double quantity = 0.0;

    // find values for endTime and quantity
    while (tasks.hasNext()) {
      Task t = (Task) tasks.next();

      // replaced min with this CWG
      if (t.getPreferredValue(AspectType.END_TIME) < endTime) {
        endTime = t.getPreferredValue(AspectType.END_TIME);
      }

      // replaced min with this CWG
      if (t.getPreferredValue(AspectType.START_TIME) > startTime) {
        startTime = t.getPreferredValue(AspectType.START_TIME);
      }
      quantity += t.getPreferredValue(AspectType.QUANTITY);
    }

    // make the START_TIME preference
    // this is a placeholder for more faithful logic later...
    // [1999/11/15:goldman]
    //
    // MSB 1-25-2000 : Make Start time 40 days before end_time
    startTime = endTime - START_DECREMENT;

    prefs.add(makeStartPreference(startTime, rootFactory));

    // make the endTime preference...
    prefs.add(makeEndPreference(endTime, rootFactory));

    prefs.add(makeQuantityPreference(quantity, rootFactory));
    return prefs;
  }

  // Added the rootFactory argument.  Seemed to need it to make the pref. CGW
  private Preference makeQuantityPreference(double amount, PlanningFactory rootFactory) {
    AspectValue av = AspectValue.newAspectValue(AspectType.QUANTITY, amount);
    ScoringFunction sf = ScoringFunction.createNearOrBelow(av, 0.1);
    Preference pref = rootFactory.newPreference(AspectType.QUANTITY, sf);
    return pref;
  }

  private Preference makeStartPreference(double startDate, PlanningFactory rootFactory) {
    AspectValue startTime = AspectValue.newAspectValue(AspectType.START_TIME, startDate);
    ScoringFunction sf = ScoringFunction.createNearOrAbove(startTime, 0.0);
    Preference pref = rootFactory.newPreference(AspectType.START_TIME, sf);
    return pref;
  }

  /**
   * makeEndPreference -
   * separate earliest, best, and latest for TOPS. Picked 1 day out
   * of the blue (with help from Gordon
   */
  private Preference makeEndPreference(double endDate, PlanningFactory rootFactory) {

    AspectValue earliest =
        AspectValue.newAspectValue(AspectType.END_TIME, endDate + EARLIEST_INCREMENT);

    AspectValue best =
        AspectValue.newAspectValue(AspectType.END_TIME, endDate + BEST_INCREMENT);

    AspectValue latest =
        AspectValue.newAspectValue(AspectType.END_TIME, endDate + LATEST_INCREMENT);

    ScoringFunction sf =
        ScoringFunction.createVScoringFunction(earliest, best, latest);
    Preference pref = rootFactory.newPreference(AspectType.END_TIME, sf);
    return pref;
  }
}







