/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.trans;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;

import org.cougaar.lib.vishnu.client.VishnuAggregatorPlugin;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.util.log.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.ScoringFunction;

import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;

/**
 * Note that although this is an aggregator, an expansion is made and subtasks created. <p>
 *
 * This is because after the aggregation, the setup and wrapup times must be represented. <br>
 * They are represented as separate tasks with verb "Transit".  They represent the time   <br>
 * taken to travel from the supply point and to return.  They should not be figured into  <br>
 * the time that the task takes to perform, since they are only indirectly related to it. <p>
 * 
 * If the specs were altered, and no setup and wrapup duration were included, only aggregations <br>
 * would be made, and therefore only MPTask would be produced.  The allocator downstream        <br>
 * would have to be sensitive to this change.
 */
public class TransportAggregatorPlugin extends VishnuAggregatorPlugin {
  boolean debugPrefs = false;

  /** 
   * Only tasks with transport verbs are given to Vishnu   <p>
   * If a task has no FROM prep, it is not handled.         <p>
   * If a task has a FROM prep that is in CONUS, it is not handled. <p>
   * If a task is an MPTask (and possibly an output of the plugin), it is not handled.
   */
  public boolean interestingTask(Task t) {
    if (debugPrefs) {
      for (Enumeration prefs = t.getPreferences(); prefs.hasMoreElements (); ) {
	Preference pref = (Preference) prefs.nextElement ();
	int aspectType = pref.getAspectType ();

	AspectValue lower = new AspectValue (aspectType, 0.0d);

	Calendar cal = java.util.Calendar.getInstance();
	cal.set(2200, 0, 0, 0, 0, 0);
	cal.set(Calendar.MILLISECOND, 0);
	double endOfRange = (double) ((Date) cal.getTime()).getTime();
	AspectValue upper = new AspectValue (aspectType, endOfRange);

	print (pref, pref.getScoringFunction().getDefinedRange (),
	       pref.getScoringFunction().getValidRanges (lower, upper), logger);
      }
    }

    boolean hasTransport = t.getVerb().equals (Constants.Verb.TRANSPORT) ||
      t.getVerb().equals(Constants.Verb.TRANSIT);

    if (!hasTransport)
      return false;
    if (!prepHelper.hasPrepNamed (t, Constants.Preposition.FROM)) {
      if (isInfoEnabled())
	info (getName () + ".interestingTask - ignoring TRANSPORT task " + t.getUID () + " that doesn't have a FROM prep.");
      return false;
    }
    /*
      GeolocLocation geoloc = 
      (GeolocLocation) UTILPrepPhrase.getIndirectObject (t, Constants.Preposition.FROM);
      if (geoloc.getLongitude().getDegrees () < 0) {
      if (isInfoEnabled())
      info (getName () + ".interestingTask - ignoring task " + t.getUID() + " with FROM of " + geoloc + " - it's not in theater.");
      return false;
      }
      geoloc = 
      (GeolocLocation) UTILPrepPhrase.getIndirectObject (t, Constants.Preposition.TO);
      if (geoloc.getLongitude().getDegrees () < 0) {
      if (isInfoEnabled())
      info (getName () + ".interestingTask - ignoring task " + t.getUID() + " with TO of " + geoloc + " - it's not in theater.");
      return false;
      }
    */

    /*
      boolean hasPrepo = UTILPrepPhrase.hasPrepNamed (t, "PREPO");
      boolean hasStratTrans = false;

      if (UTILPrepPhrase.hasPrepNamed (t, "OFTYPE")) {
      Asset oftype = (Asset) UTILPrepPhrase.getIndirectObject (t, "OFTYPE");
      String typeid = oftype.getTypeIdentificationPG().getTypeIdentification ();
      hasStratTrans = typeid.equals ("StrategicTransportation");
      }
    */

    boolean val = 
      !(t instanceof MPTask) &&
      super.interestingTask(t);
    //  !hasPrepo && !hasStratTrans; 

    if (isDebugEnabled() && val)
      debug (getName () + ".interestingTask - interested in " + t.getUID());
	
    return val;
  }
  
  protected void print (Preference pref, 
			       AspectScoreRange definedRange, Enumeration validRanges, Logger logger) {
    double prefval = pref.getScoringFunction().getBest ().getValue();
    String prefstr = "" + prefval;
    String type = "" + pref.getAspectType ();
    String value = "" + prefval;
    boolean isDate = false;
    switch (pref.getAspectType ()) {
    case AspectType.START_TIME: 
      type = "START_TIME";
      prefstr = "" + new Date ((long) prefval);
      isDate = true;
      break;
    case AspectType.END_TIME: 
      type = "END_TIME";
      prefstr = "" + new Date ((long) prefval);
      isDate = true;
      break;
    case AspectType.COST: 
      type = "COST";
      prefstr = "$" + (long) prefval;
      break;
    }

    logger.info ("pref type " + type + " value " + prefstr + " sf " + pref.getScoringFunction());

    for (; validRanges.hasMoreElements (); ) {
      AspectScoreRange range = (AspectScoreRange) validRanges.nextElement();
      AspectScorePoint start = range.getRangeStartPoint ();
      AspectScorePoint end   = range.getRangeEndPoint ();
      double startValue = start.getValue ();
      double endValue   = end.getValue ();

      if (isDate)
	logger.info ("<" + new Date ((long) (startValue)) + "-" + new Date ((long) (endValue)) + "> "); 
      else
	logger.info ("<" + startValue + "-" + endValue + "> "); 
    }
    logger.info ("");
  }

  /** only trucks, which have contain pgs, are given to Vishnu */
  public boolean interestingAsset(Asset a) {
    if (!(a instanceof GLMAsset)) {
      if (isInfoEnabled())
	info (getName () + ".interestingAsset - ignoring asset " + a + " because it's not an GLMAsset.");
      return false;
    }
	
    boolean val = ((GLMAsset)a).hasContainPG ();
    if (isInfoEnabled() && !val)
      info (getName () + ".interestingAsset - ignoring GLMAsset " + a + " because it's missing a Contain PG.");
    return val;
  }
}



