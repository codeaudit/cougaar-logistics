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

import org.cougaar.core.service.AlarmService;
import org.cougaar.lib.util.UTILPreference;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.ArrayList;
import java.util.Date;
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
  private AlarmService alarmService;

  //TODO:  find out which logger class to use
  //private Logger logger;
  private UTILPreference prefHelper = new UTILPreference(logger);
  private static Logger logger = Logging.getLogger(DefaultPreferenceAggregator.class);

  public DefaultPreferenceAggregator(AlarmService alarmService) {
    this.alarmService = alarmService;
    // this.logger = logger;
  }

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
    double best = java.lang.Double.POSITIVE_INFINITY;
    double earliest = 0.0;
    double latest = java.lang.Double.POSITIVE_INFINITY;
    double startTime = 0.0;
    double quantity = 0.0;

    // find values for endTime and quantity
    while (tasks.hasNext()) {
      Task t = (Task) tasks.next();
      Date taskBest = prefHelper.getBestDate(t);
      if (taskBest.getTime() < best)
        best = taskBest.getTime();

      Preference endDatePref = t.getPreference(AspectType.END_TIME);
      ScoringFunction sf = endDatePref.getScoringFunction();
      AspectScorePoint aspStart = sf.getDefinedRange().getRangeStartPoint();
      Date taskEarlyDate = new Date((long) aspStart.getValue());
      if (taskEarlyDate.getTime() > earliest) {
        earliest = taskEarlyDate.getTime();
      }
      AspectScorePoint aspEnd = sf.getDefinedRange().getRangeEndPoint();
      Date taskLateDate = new Date((long) aspEnd.getValue());
      if (taskLateDate.getTime() < latest) {
        latest = taskLateDate.getTime();
      }
      quantity += t.getPreferredValue(AspectType.QUANTITY);
    }

    //TODO: change this to be latestStart = min END_TIME - OST, earliestStart Tomorrow
    startTime = alarmService.currentTimeMillis() + MILLIS_PER_DAY;
    // System.out.println(" PREFERENCEAGG--> agg quant is " + quantity);
    //startTime = getStartOfPeriod();
    prefs.add(makeStartPreference(startTime, rootFactory));
    // make the endTime preference...
    prefs.add(makeEndPreference(earliest, best, latest, rootFactory));
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
  private Preference makeEndPreference(double earliest, double best, double latest, PlanningFactory rootFactory) {

    AspectValue earliestAV =
        AspectValue.newAspectValue(AspectType.END_TIME, earliest);

    AspectValue bestAV =
        AspectValue.newAspectValue(AspectType.END_TIME, best);

    AspectValue latestAV =
        AspectValue.newAspectValue(AspectType.END_TIME, latest);

    ScoringFunction sf =
        ScoringFunction.createVScoringFunction(earliestAV, bestAV, latestAV);
    Preference pref = rootFactory.newPreference(AspectType.END_TIME, sf);
    return pref;
  }

  private long getStartOfPeriod() {
    long timeIn = alarmService.currentTimeMillis();
    //truncate to the whole number that represents the period num since the start of time.
    long periods = (long) (timeIn / MILLIS_PER_DAY);
    //Multiply it back to which gives the start of the period.
    long timeOut = timeIn * MILLIS_PER_DAY;
    return timeOut;
  }
}







