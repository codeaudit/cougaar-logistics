/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
import org.cougaar.planning.ldm.asset.AssetGroup;

import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;

import org.cougaar.lib.filter.UTILSingleTaskAllocatorPlugin;

import java.util.Date;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;

import org.cougaar.glm.ldm.plan.GeolocLocation;  

import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;

import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPrepPhrase;

/**
 * <pre>
 * Plugin that looks for TRANSIT or TRANSPORT tasks.
 *
 * In general, like it's super class, TransportAllocatorPlugin, 
 * BUT if the asset is self-propelled, short-circuits the process
 * by assigning the task to its direct object.  This follows the simple
 * idea that to deploy a truck, you have it move itself.
 * 
 * </pre>
 */
public class GroundTransportAllocatorPlugin extends TransportAllocatorPlugin {

  /** creates GLMMeasure and GLMPrepPhrase helper classes */
  public void localSetup () {
    super.localSetup ();
    measureHelper = new GLMMeasure (logger);
    glmPrepHelper = new GLMPrepPhrase (logger);
  }

  /** 
   * <pre>
   * This plugin is interested in tasks with verb TRANSIT or TRANSPORT 
   * The task must also have the VISHNU prep attached, indicating that the Vishnu
   * plugin upstream has created the task.
   *
   * BUT if the task is to move a self-transportable item, just go ahead, be
   * interested in it, and allocate it. These tasks will never go to the Vishnu 
   * Aggregator.
   * </pre>
   * @param t task to check to see if it is an input task to this plugin
   */
  public boolean interestingTask(Task t) {
    boolean hasTransitVerb = t.getVerb().equals (Constants.Verb.TRANSIT);
    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);

    if (!(hasTransitVerb || hasTransportVerb)) return false;

    boolean hasVishnu = prepHelper.hasPrepNamed (t, "VISHNU");

    if (hasTransitVerb && hasVishnu) {
      if (isDebugEnabled())
	debug (getName ()
	      + ".interestingTask - interested in TRANSIT task "
	      + t.getUID ());
      return true;
    }

    if (hasTransportVerb && hasVishnu) {
      if (isDebugEnabled())
	debug (getName ()
	      + ".interestingTask - interested in TRANSPORT task "
	      + t.getUID ());
      return true;
    }

    boolean isSelfPropelled = isSelf(t);

    if (hasTransportVerb && !hasVishnu && isSelfPropelled) {
      if (isDebugEnabled())
	debug (getName ()
	      + ".interestingTask - interested in self-propelled task "
	      + t.getUID ());
      return true;
    }

    if (isDebugEnabled() && !isSelfPropelled)
      debug (getName ()
	    + ".interestingTask - NOT interested in "
	    + t.getUID ());

    return isSelfPropelled;
  }

  /** checks to see if the the task is to move a self-propelled item */
  protected boolean isSelf (Task t) {
    boolean isSelfPropelled = false;

    Asset directObject = t.getDirectObject ();
    if (!(directObject instanceof AssetGroup))
      isSelfPropelled = isSelfPropelled (t, directObject);

    return isSelfPropelled;
  }

  /** 
   * <pre>
   * Checks to see if the the task is to move a self-propelled item, given the direct object 
   *
   * Determines the nature of the direct object by examining its Cargo Category Code on its
   * movability property group.
   *
   * If an asset doesn't have a movability PG, returns false.
   * </pre>
   * @param t just passed in so we can report which task has a missing movability PG.
   * @param directObject asset to examine 
   */
  protected boolean isSelfPropelled (Task t, Asset directObject) {
    GLMAsset baseAsset = 
      (directObject instanceof AggregateAsset) ? 
      (GLMAsset) ((AggregateAsset)directObject).getAsset() : 
      (GLMAsset) directObject;
	
    MovabilityPG move_prop = baseAsset.getMovabilityPG();

    if (move_prop != null) {
      String cargocatcode = move_prop.getCargoCategoryCode();
      if (cargocatcode == null) {
	if (isWarnEnabled())
	  warn (getName () + ".isSelfPropelled - found task " + t.getUID () + 
		"\nwith d.o. " + directObject + "\nthat had a movabilityPG but no cargo cat code?");
      }
      else if (cargocatcode.charAt(0) == 'R') {
	if (isDebugEnabled())
	  debug (getName() + ".isSelfPropelled - found self-propelled vehicle on task " + t.getUID());
	return true;
      }
    }
    else {
      if (isInfoEnabled())
	info (getName() + ".isSelfPropelled - asset " + baseAsset + " for task " + t + 
	      " is missing its movability PG.");
    }

    return false;
  }

  /** 
   * <pre>
   * determines the asset that is allocated to 
   *
   * Allocate to self (=direct object) if self propelled.
   *
   * Otherwise grab asset from WITH prep.
   * </pre>
   **/
  public Asset findAsset(Task t){
    if (isSelf (t)) {
      return t.getDirectObject();
    }
    else {
      return super.findAsset (t);
    }
  }

  /** 
   * <pre>
   * Do the actual allocation here
   *
   * If self propelled, do simple great circle calculations based on the speed
   * of the asset to determine START and END aspect values.  
   *
   * Calls <tt>getSpeed</tt> to determine asset (vehicle) speed.
   *
   * Otherwise, use super's createAllocation.
   * <pre>
   * @see #isSelf
   * @see org.cougaar.glm.util.GLMMeasure#distanceBetween
   * @see #getSpeed
   * @param t the task to allocate 
   * @param a the asset to allocate to
   * @return the allocation
   */
  public PlanElement createAllocation(Task t, Asset a){
    try {
      if (isSelf (t)) {
	Date to   = prefHelper.getBestDate (t);
	Date from = to;
	GeolocLocation poe = glmPrepHelper.getFromLocation (t);
	GeolocLocation pod = glmPrepHelper.getToLocation (t);

	double distance = measureHelper.distanceBetween (poe,pod).getMiles();
	double speed = getSpeed ((GLMAsset) a);
	long time = (long) ((distance/speed)*60.0d*60.0d*1000.0d); // millis

	from = new Date (to.getTime () - time);
	double confidence = allocHelper.HIGHEST_CONFIDENCE;

	if (isDebugEnabled())
	  debug (".createAllocation - for self propelled " + t.getUID() + 
		 " , ready at " + from + 
		 " - best " + to + " confidence " + confidence);

	AspectValue [] values = new AspectValue [2];
	values[0] = AspectValue.newAspectValue (AspectType.START_TIME, from.getTime());
	values[1] = AspectValue.newAspectValue (AspectType.END_TIME,   to.getTime());

	PlanElement pe = allocHelper.makeAllocation(this,
						    ldmf, realityPlan, t, a, 
						    values,
						    confidence,
						    Constants.Role.TRANSPORTER);
	return pe;
      }
      else 
	return super.createAllocation (t, a);
    } catch (Exception e) {
      error (getName () + ".createAllocation - for task " + t.getUID () + 
	     " the asset allocated was " + 
	     a + " which is NOT a GLMAsset.  How strange.");
      return null;
    }
  }

  /** 
   * Examines GroundSelfPropulsionPG to determine speed of asset 
   * 
   * @see org.cougaar.glm.ldm.asset.GroundSelfPropulsionPG#getCruiseSpeed
   **/
  protected double getSpeed (GLMAsset asset) {
    double speed = 55;
	
    try {
      speed = asset.getGroundSelfPropulsionPG().getCruiseSpeed().getMilesPerHour();
    } catch (Exception e) {
      try {
	speed = asset.getAirSelfPropulsionPG().getCruiseSpeed().getMilesPerHour();
      } catch (Exception ee) {
	try {
	  speed = asset.getWaterSelfPropulsionPG().getCruiseSpeed().getMilesPerHour();
	} catch (Exception eee) {
	  if (isDebugEnabled())
	    debug (getName() + ".getSpeed - WARNING - Could not determine"+ 
		   " resource speed for " + asset.getUID());
	}
      }
    }
	
    return speed;
  }

  protected GLMMeasure measureHelper;
  protected GLMPrepPhrase glmPrepHelper;
}
