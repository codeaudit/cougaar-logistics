/*
 * <copyright>
 *  Copyright 2001-2 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.trans.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.ForUnitPG;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.glm.ldm.asset.NewMovabilityPG;
import org.cougaar.glm.ldm.asset.NewForUnitPG;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.glm.ldm.asset.PhysicalPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;

import org.cougaar.glm.ldm.asset.ClassISubsistence;
import org.cougaar.glm.ldm.asset.ClassIIClothingAndEquipment;
import org.cougaar.glm.ldm.asset.ClassIIIPOL;
import org.cougaar.glm.ldm.asset.ClassIVConstructionMaterial;
import org.cougaar.glm.ldm.asset.ClassVAmmunition;
import org.cougaar.glm.ldm.asset.ClassVIPersonalDemandItem;
import org.cougaar.glm.ldm.asset.ClassVIIMajorEndItem;
import org.cougaar.glm.ldm.asset.ClassVIIIMedical;
import org.cougaar.glm.ldm.asset.ClassIXRepairPart;
import org.cougaar.glm.ldm.asset.ClassXNonMilitaryItem;
import org.cougaar.glm.ldm.asset.Person;

import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.AssetUtil;
import org.cougaar.glm.util.GLMPrepPhrase;
import org.cougaar.glm.util.GLMPreference;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Preposition;
import org.cougaar.planning.ldm.plan.Priority;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Verb;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;

import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;
import org.cougaar.planning.ldm.measure.Area;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.measure.Mass;
import org.cougaar.planning.ldm.measure.Volume;
import org.cougaar.logistics.plugin.trans.NewLowFidelityAssetPG;
import org.cougaar.logistics.plugin.trans.LowFidelityAssetPG;

import org.cougaar.logistics.plugin.trans.CargoCatCodeDimensionPG;
import org.cougaar.logistics.plugin.trans.NewCargoCatCodeDimensionPG;

import org.cougaar.logistics.plugin.trans.tools.BlackboardPlugin;
import org.cougaar.logistics.plugin.trans.tools.PortLocatorImpl;

import org.cougaar.glm.ldm.asset.TransportationRoute;

/**
 * getSubtasks is filled in.  It justs blows up composite
 * tasks into smaller one unit tasks.  Examines each Task.
 *
 * @see UTILExpanderPluginAdapter
 */
public class TranscomProjectionExpanderPlugin extends UTILExpanderPluginAdapter implements BlackboardPlugin {
  public final long MILLIS_PER_DAY = 24*60*60*1000; 

  public void localSetup() {     
    super.localSetup();

    glmPrepHelper = new GLMPrepPhrase (logger);
    glmPrefHelper = new GLMPreference (logger);
    glmAssetHelper = new AssetUtil (logger);
  }

  /** 
   * Implemented for UTILGenericListener interface
   *
   * Look for tasks that have TRANSPORT or PREPAREFORTRANSPORT as their verb
   *
   * @param t Task to check for interest
   * @return boolean true if task is interesting
   */

  public boolean interestingTask(Task t) {
    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);
    boolean hasProjectTransportVerb = t.getVerb().equals (Constants.Verb.PREPAREFORTRANSPORT);
    if (isDebugEnabled() &&
	hasTransportVerb || hasProjectTransportVerb) {
      debug (getName () + 
	     " : interested in expandable task " + 
	     t + " with direct obj " + 
	     t.getDirectObject ());
    }
    if (isDebugEnabled() && !hasTransportVerb && !hasProjectTransportVerb) 
      debug(getName () + 
	    " : Ignoring task " + t.getUID () + 
	    " with verb " + t.getVerb());

    return hasTransportVerb;
  }

  /** 
   * <pre>
   * Examines task to see if task looks like what the plugin 
   * expects it to look like.
   *
   * Checks FROM and TO prepositions to see they're all there.
   *
   * </pre>
   * @param t Task to check for consistency
   * @return true if task is OK
   */
  public boolean isTaskWellFormed(Task taskToCheck) {
    boolean taskTiming = verifyHelper.isTaskTimingCorrect (taskToCheck);
	
    if (!taskTiming)
      return false;
	
    GeolocLocation start = 
      (GeolocLocation) glmPrepHelper.getIndirectObject (taskToCheck, Constants.Preposition.FROM);
    GeolocLocation end   = 
      (GeolocLocation) glmPrepHelper.getIndirectObject (taskToCheck, Constants.Preposition.TO);

    if (start == null) {
      reportError (".isTaskWellFormed : Hey! For task " + taskToCheck + 
		   " FROM geoloc is null!");
      return false;
    }
    else if (end == null) {
      reportError (".isTaskWellFormed : Hey! For task " + taskToCheck + 
		   " TO geoloc is null!");
      return false;
    }
    else {
      Longitude lon = start.getLongitude ();
      Latitude  lat = start.getLatitude ();
      if (lon == null) {
	reportError (".isTaskWellFormed : For task " + taskToCheck + 
		     "\n FROM longitude is null!");
	return false;
      }
      else if (isDebugEnabled()) 
	debug (".isTaskWellFormed\n\tTask : " + 
	       taskToCheck + "\nstart (long, lat) is (" + 
	       lon.getDegrees () + "," + lat.getDegrees () + ")");

      lon = end.getLongitude ();
      lat = end.getLatitude ();
      if (lon == null) {
	reportError (".isTaskWellFormed : TO longitude is null!");
	return false;
      }
      else if (isDebugEnabled()) 
	debug ("end (long, lat) is (" + 
	       lon.getDegrees () + "," + lat.getDegrees () + ")");

      //      if (isInfoEnabled()) 
      //	info ("Distance between is : " + 
      //			    UTILUtil.distanceBetween (start, end).getMiles () + " miles.");
    }
    return true;
  }

  int i = 0;

  /**
   * <pre>
   * Implemented for UTILExpanderPlugin interface
   *
   *
   * </pre>
   * @return Vector of subtasks of parentTask
   */
  public Vector getSubtasks(Task parentTask) {
    Vector childTasks = new Vector ();

    if (isInfoEnabled())
      info (".getSubtasks - received task " + (i++) + " total.");

    if (parentTask.getVerb ().equals (Constants.Verb.PREPAREFORTRANSPORT)) {
    }
    else {
    }

    return childTasks;
  }

  /** 
   * Makes subtask of parent task, with given direct object.
   *
   * removes OFTYPE prep, since it's not needed by scheduler 
   **/
  protected Task makeTask (Task parentTask, Asset directObject, String originalOwner) {
    if (isDebugEnabled()) 
      debug (".makeTask - making subtask of " + parentTask.getUID () + 
	     " d.o. " + directObject);
    
    Task newtask = expandHelper.makeSubTask (ldmf,
					     parentTask,
					     directObject,
					     getBindingSite().getAgentIdentifier());
    glmPrepHelper.removePrepNamed(newtask, Constants.Preposition.OFTYPE);

    // Next four lines create a Property Group for unit and attach it to all assets attached to task
    ForUnitPG unitPG = (ForUnitPG)ldmf.createPropertyGroup(ForUnitPG.class);
    if (!glmPrepHelper.hasPrepNamed (newtask,Constants.Preposition.FOR)) {
      String owner = (originalOwner != null) ? originalOwner : directObject.getUID().getOwner();

      if (isInfoEnabled()) {
	info (".getSubtasks - WARNING : got task " + parentTask.getUID() + 
	      " which has no FOR unit prep, using owner - " + owner + ".");
      }
	
      ((NewForUnitPG)unitPG).setUnit(owner);
    } else {
      ((NewForUnitPG)unitPG).setUnit((String)glmPrepHelper.getIndirectObject(newtask,Constants.Preposition.FOR));
      glmPrepHelper.removePrepNamed(newtask, Constants.Preposition.FOR);
    }
	
    //    attachPG(directObject, unitPG);

    // glmtrans doesn't deal with quantity -- only get success or failure
    if (prefHelper.hasPrefWithAspectType(newtask, AspectType.QUANTITY))
      prefHelper.removePrefWithAspectType(newtask, AspectType.QUANTITY);

    //    attachRoute (parentTask, newtask);

    return newtask;
  }

  /** 
   * <pre>
   * NOTE : This is called magically by reflection from BindingUtility.setServices 
   * setServices looks for any method in a component that starts with "set" and 
   * tries to find a service X.  
   *
   * More specifically, it's looking for a method signature like:
   *   setXService (XService s)
   *
   * </pre>
   * @see org.cougaar.core.component.BindingUtility#setServices
   */
  public void setDomainService(DomainService ds) {
    theDomainService = ds;
  }
  public final DomainService getDomainService() {
    return theDomainService;
  }
  //Domain service (factory service piece of old LDM)
  private DomainService theDomainService = null;

  /** implemented for BlackboardPlugin interface -- need public access! */
  public BlackboardService getBlackboard () {
    return blackboard;
  }

  // Utility functions ----------------------------------------------------

  private void reportError (String err) { 
    error (getName () + " : " + err); 
  }

  protected GLMPrepPhrase glmPrepHelper;
  protected GLMPreference glmPrefHelper;
  protected AssetUtil glmAssetHelper;
}
