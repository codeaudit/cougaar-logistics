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
package org.cougaar.logistics.plugin.trans.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.adaptivity.*;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.ldm.asset.PhysicalPG;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.glm.ldm.asset.ForUnitPG;
import org.cougaar.glm.ldm.asset.NewForUnitPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.asset.GLMAsset;

import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.AssetUtil;
import org.cougaar.glm.util.GLMPrepPhrase;
import org.cougaar.glm.util.GLMPreference;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.*;
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
import org.cougaar.logistics.plugin.trans.NewLowFidelityAssetPG;
import org.cougaar.logistics.plugin.trans.LowFidelityAssetPG;

import org.cougaar.glm.util.AssetUtil;
import org.cougaar.glm.util.GLMPrepPhrase;
import org.cougaar.glm.util.GLMPreference;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.logistics.plugin.trans.tools.BlackboardPlugin;
import org.cougaar.logistics.plugin.trans.tools.PortLocatorImpl;

import org.cougaar.glm.ldm.asset.TransportationRoute;

/**
 * getSubtasks is filled in.  It justs blows up composite
 * tasks into smaller one unit tasks.  Examines each Task.
 *
 * @see UTILExpanderPluginAdapter
 */
public class GLMTransOneToManyExpanderPlugin extends UTILExpanderPluginAdapter implements BlackboardPlugin {
  public static final Integer LOW_FIDELITY  = new Integer(0);
  public static final Integer HIGH_FIDELITY = new Integer(1);
  public static final String  FIDELITY_KNOB = "FidelityKnob";

  public void localSetup() {     
    super.localSetup();

    try {
      if (getMyParams ().hasParam ("ExpandAggregates"))
	myExpandAggregates = getMyParams().getBooleanParam("ExpandAggregates");
      else
	myExpandAggregates = true;
    } catch (Exception e) {}

    glmPrepHelper = new GLMPrepPhrase (logger);
    glmPrefHelper = new GLMPreference (logger);
    glmAssetHelper = new AssetUtil (logger);

    setupOperatingModes ();
    portLocator.setFactory (ldmf); // tell route finder the ldm factory to use
  }

   public void setupFilters () {
     super.setupFilters ();
     portLocator = new PortLocatorImpl (this, logger);
   }

  protected OperatingMode mode;

  /** create and publish Fidelity Knob Operating Mode */
  protected void setupOperatingModes () {
    OMCRange range = new IntRange (LOW_FIDELITY.intValue(), HIGH_FIDELITY.intValue());
    OMCRangeList rangeList = new OMCRangeList (range);
    publishAdd (mode = new OperatingModeImpl (FIDELITY_KNOB, rangeList, HIGH_FIDELITY));
  }
  
  protected static class IntRange extends OMCRange {
    public IntRange (int a, int b) { super (a, b); }
  }

  /** 
   * Implemented for UTILGenericListener interface
   *
   * Look for tasks that
   * 1) Have TRANSPORT as their verb
   *
   * @param t Task to check for interest
   * @return boolean true if task is interesting
   * @see org.cougaar.lib.callback.UTILGenericListener
   */

  public boolean interestingTask(Task t) {
    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);
    if (isDebugEnabled() &&
	hasTransportVerb) {
      debug (getName () + 
	    " : interested in expandable task " + 
	    t + " with direct obj " + 
	    t.getDirectObject ());
    }
    if (isDebugEnabled() && !hasTransportVerb) 
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
	info (".isTaskWellFormed\n\tTask : " + 
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

  /**
   * Implemented for UTILExpanderPlugin interface
   *
   * Break up tasks into constituent parts.
   */
  public Vector getSubtasks(Task parentTask) {
    Vector childTasks = new Vector ();

    if (isDebugEnabled()) 
      debug (".getSubtasks - mode is " + mode);

    // if the incoming task is already a low fidelity task, we don't need to do anything to it
    if (prepHelper.hasPrepNamed(parentTask, GLMTransConst.LOW_FIDELITY)) {
      // pass through
      Asset lowFiAsset = parentTask.getDirectObject();
      Task subTask = makeTask (parentTask, lowFiAsset, null);
      NewLowFidelityAssetPG lowFiPG = 
	(NewLowFidelityAssetPG)ldmf.createPropertyGroup(LowFidelityAssetPG.class);
      lowFiPG.setOriginalAsset (lowFiAsset);
      attachPG(lowFiAsset, lowFiPG); // now subobject has pointer back to parent
      if (isDebugEnabled()) 
	debug (".getSubtasks - processing task " + parentTask.getUID () + 
	       " by attaching low fi pg to it's d.o. - " + lowFiAsset);
      childTasks.addElement (subTask);
    }
    else if (mode.getValue ().equals(LOW_FIDELITY) && 
	     !isPersonTask(parentTask) &&
	     (parentTask.getDirectObject() instanceof AssetGroup)) {
      if (isDebugEnabled()) 
	debug (".getSubtasks - processing task " + parentTask.getUID () + 
	       " in LOW fidelity mode. d.o. is " + parentTask.getDirectObject());

      childTasks = getLowFidelityTask (parentTask);
    }
    else {
      if (isDebugEnabled()) 
	debug (".getSubtasks - processing task " + parentTask.getUID () + 
	       " in HIGH fidelity mode. d.o. is " + parentTask.getDirectObject());

      Vector itemsToMove = expandAsset((Asset)parentTask.getDirectObject());

      for (int i = 0; i < itemsToMove.size (); i++) {
	Task subTask = makeTask (parentTask, (Asset) itemsToMove.elementAt(i), null);
	childTasks.addElement (subTask);
      }
    }

    return childTasks;
  }

  protected Vector getLowFidelityTask (Task parentTask) {
    Vector retval = new Vector ();
    Task newTask = null;

    GLMAsset lowFiAsset = 
      (GLMAsset) assetHelper.createInstance (getLDMService().getLDM(), "LowFidelityPrototype", "LowFi_" + getNextID());
    NewLowFidelityAssetPG lowFiPG = 
      (NewLowFidelityAssetPG)ldmf.createPropertyGroup(LowFidelityAssetPG.class);
    lowFiPG.setOriginalAsset (lowFiAsset);
    attachPG(lowFiAsset, lowFiPG); // now subobject has pointer back to parent

    Asset asset = parentTask.getDirectObject();

    try {
      NewPhysicalPG newPhysicalPG = PropertyGroupFactory.newPhysicalPG ();
      setDimensions (newPhysicalPG, expandAsset (asset));
      lowFiAsset.setPhysicalPG (newPhysicalPG);
    } catch (Exception e) { 
      logger.error ("problem processing " + asset, e);
    }

    ((NewItemIdentificationPG) lowFiAsset.getItemIdentificationPG()).setNomenclature ("LowFidelityAggregate");

    if (isDebugEnabled())
      debug ("getLowFidelityTask - created low fi asset " + 
	     lowFiAsset.getUID() + " : " +
	     lowFiAsset.getItemIdentificationPG().getNomenclature() +  " - " + 
	     lowFiAsset.getItemIdentificationPG().getItemIdentification());

    newTask = makeTask (parentTask, lowFiAsset, getOriginalOwner(parentTask));

      // mark the new task as an aggregate low fi task
    prepHelper.addPrepToTask (newTask, 
			      prepHelper.makePrepositionalPhrase(ldmf,
								 GLMTransConst.LOW_FIDELITY,
								 GLMTransConst.LOW_FIDELITY));

    retval.add(newTask);

    return retval;
  }

  protected String getOriginalOwner (Task parentTask) {
    if (!glmPrepHelper.hasPrepNamed (parentTask,Constants.Preposition.FOR)) {
      Asset directObject = parentTask.getDirectObject();
      String owner = directObject.getUID().getOwner();

      if (isInfoEnabled()) {
	info (".getOriginalOwner - WARNING : got task " + parentTask.getUID() + 
	      " which has no FOR unit prep, using owner - " + owner + ".");
      }
      
      return owner;
    } 
    else
      return (String)glmPrepHelper.getIndirectObject(parentTask,Constants.Preposition.FOR);
  }

  /** 
   * Since FOR preps are lost a custom property is added to determine unit
   */
  public void attachPG(Asset asset, PropertyGroup thisPG) {
    if (asset instanceof AssetGroup) {
      Vector assetList = ((AssetGroup)asset).getAssets();
      for (int i = 0; i < assetList.size(); i++) {
	attachPG((Asset)assetList.elementAt(i), thisPG);
      }
    } else if (asset instanceof AggregateAsset) {
      // Put in both because unsure of behavior
      asset.addOtherPropertyGroup(thisPG);
      // Don't want to do this, since every aggregate of X
      //XX asset will then have this pg
      //	    attachUnitPG(((AggregateAsset)asset).getAsset(),unitPG);
    } else {
      asset.addOtherPropertyGroup(thisPG);
    }
  }

  protected boolean isPersonTask (Task parentTask) {
    Asset asset = parentTask.getDirectObject();
    if (asset instanceof AggregateAsset) {
      GLMAsset itemProto = (GLMAsset) ((AggregateAsset)asset).getAsset();
      return itemProto.hasPersonPG();
    }
    // if aggregate assets of people are inside of an asset group
    // it's a person...
    else if (asset instanceof AssetGroup) {
      AssetGroup group = (AssetGroup) asset;
      Vector assetList = ((AssetGroup)asset).getAssets();
      for (int i = 0; i < assetList.size(); i++) {
	Asset subasset = (Asset)assetList.elementAt(i);
	if (subasset instanceof AggregateAsset) {
	  GLMAsset itemProto = (GLMAsset) ((AggregateAsset)subasset).getAsset();
	  return itemProto.hasPersonPG();
	}
	else 
	  return false;
      }
    }
    return false;
  }

  protected int id = 0;
  protected int getNextID () {
    return id++;
  }

  /** assumes all will be people or not */
  protected void setDimensions (NewPhysicalPG physicalPG, Collection realAssets) {
    double length = 0.0;
    double width  = 0.0;
    double area   = 0.0;
    double volume = 0.0;
    double height = 0.0;
    double mass   = 0.0;

    Object firstItem = realAssets.iterator().next();
    
    if (firstItem instanceof AggregateAsset) {
      AggregateAsset aggAsset = (AggregateAsset) firstItem;
      PhysicalPG itemPhysicalPG = ((GLMAsset)aggAsset.getAsset()).getPhysicalPG();
      if (itemPhysicalPG == null) {
	if (!((GLMAsset)aggAsset.getAsset()).hasPersonPG())
	  warn (".setDimensions - asset " + firstItem + 
		"'s base asset " + (GLMAsset)aggAsset.getAsset() + " has no physical PG.");
	else if (isDebugEnabled ())
	  debug (".setDimensions - NOTE : asset " + firstItem + 
		"'s base asset " + (GLMAsset)aggAsset.getAsset() + " has no physical PG.");
      }
      else {
	long quantity = aggAsset.getQuantity();
	double q = (double)quantity;

	length = itemPhysicalPG.getLength().getMeters() * q;
	width  = itemPhysicalPG.getWidth().getMeters() * q;
	height = itemPhysicalPG.getHeight().getMeters() * q;
	area   = itemPhysicalPG.getFootprintArea().getSquareMeters() * q;
	volume = itemPhysicalPG.getVolume().getCubicMeters() * q;
	mass   = itemPhysicalPG.getMass().getKilograms() * q;
      }
    }
    else {
      for (Iterator iter = realAssets.iterator(); iter.hasNext();) {
	GLMAsset asset = (GLMAsset)iter.next();
	PhysicalPG itemPhysicalPG = asset.getPhysicalPG();
	if (itemPhysicalPG == null)
	  error ("Asset " + asset + " doesn't have a physical PG.");
	else {
	  length += itemPhysicalPG.getLength().getMeters();
	  width  += itemPhysicalPG.getWidth().getMeters();
	  height += itemPhysicalPG.getHeight().getMeters();
	  area   += itemPhysicalPG.getFootprintArea().getSquareMeters();
	  volume += itemPhysicalPG.getVolume().getCubicMeters();
	  mass   += itemPhysicalPG.getMass().getKilograms();
	}
      }
    }

    physicalPG.setLength(new Distance (length, Distance.METERS));
    physicalPG.setWidth (new Distance (width,  Distance.METERS));
    physicalPG.setHeight(new Distance (height, Distance.METERS));
    physicalPG.setMass  (new Mass     (mass,   Mass.KILOGRAMS ));

    physicalPG.setFootprintArea(new Area (area,   Area.SQUARE_METERS));
    physicalPG.setVolume(new Volume   (volume,   Volume.CUBIC_METERS));

    if (isDebugEnabled()) {
      debug (".setDimensions got " + realAssets.size () + " items.");
      debug (".setDimensions low fi dimensions : l " + physicalPG.getLength () + 
	     " w " + physicalPG.getWidth () + 
	     " h " + physicalPG.getHeight () + 
	     " m " + physicalPG.getMass () + 
	     " a " + physicalPG.getFootprintArea () + 
	     " v " + physicalPG.getVolume ());
    }
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
	
    attachPG(directObject, unitPG);

    // glmtrans doesn't deal with quantity -- only get success or failure
    if (prefHelper.hasPrefWithAspectType(newtask, AspectType.QUANTITY))
      prefHelper.removePrefWithAspectType(newtask, AspectType.QUANTITY);

    attachRoute (parentTask, newtask);

    return newtask;
  }

  protected void attachRoute (Task parentTask, Task subtask) {
    TransportationRoute route = portLocator.getRoute (parentTask);

    glmPrepHelper.addPrepToTask (subtask, 
				 glmPrepHelper.makePrepositionalPhrase (ldmf,
									GLMTransConst.SEAROUTE,
									route));
    Distance distance = route.getLength();
    glmPrepHelper.addPrepToTask (subtask, 
				 glmPrepHelper.makePrepositionalPhrase (ldmf,
									GLMTransConst.SEAROUTE_DISTANCE,
									distance));
  }

  /**
   * <pre>
   * Function that breaks up a AssetGroup or AggregateAsset into smaller pieces.
   * Return values should only by Assets and small AggregateAssets (derived classes
   * must determine how big is OK.  By default all Aggregate are broken up).
   *
   * If myExpandAggregates is true, expands both aggregate assets and asset groups.
   * If it's false, only expands asset groups.
   *
   * </pre>
   * @param asset
   * @return A vector of assets that this asset has been broken into.
   **/
  public Vector expandAsset(Asset asset) {
    if (myExpandAggregates) {
      if (isDebugEnabled())
	debug (getName() + ".expandAsset - expanding aggregate asset " + asset);

      return glmAssetHelper.ExpandAsset(getDomainService().getFactory (), asset);
    }
    else if (asset instanceof AssetGroup) {
      if (isDebugEnabled())
	debug (getName() + ".expandAsset - expanding asset group " + asset);
      return glmAssetHelper.expandAssetGroup((AssetGroup) asset);
    }
    else {
      if (isDebugEnabled())
	debug (getName() + ".expandAsset - not expanding " + asset);
    }

    Vector vector = new Vector ();
    vector.add (asset);
	  
    return vector;
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

  protected boolean myExpandAggregates;
  protected GLMPrepPhrase glmPrepHelper;
  protected GLMPreference glmPrefHelper;
  protected AssetUtil glmAssetHelper;
  protected PortLocatorImpl portLocator;
}
