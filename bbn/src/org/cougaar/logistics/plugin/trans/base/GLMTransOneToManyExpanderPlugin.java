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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;

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
import org.cougaar.lib.callback.*;

import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
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
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;

/**
 * getSubtasks is filled in.  It justs blows up composite
 * tasks into smaller one unit tasks.  Examines each Task.
 *
 * @see UTILExpanderPluginAdapter
 */
public class GLMTransOneToManyExpanderPlugin extends UTILExpanderPluginAdapter implements BlackboardPlugin {
  /** VTH operating modes */
  protected transient OperatingMode level2Horizon, level6Horizon;

  public final Integer LEVEL_2_MIN = new Integer(2); // later, these should be parameters to plugin...
  public final Integer LEVEL_2_MAX = new Integer(365);
  public final Integer LEVEL_6_MIN = new Integer(1);
  public final Integer LEVEL_6_MAX = new Integer(365);
  public final int LEVEL_6_MODE = 0;
  public final int LEVEL_2_MODE = 1;
  /** currently not supported **/
  public final int DONT_PROCESS_MODE = 2;
  public final long MILLIS_PER_DAY = 24*60*60*1000; 

  public final String  LEVEL_2_TIME_HORIZON = "Level2TimeHorizon";
  public final Integer LEVEL_2_TIME_HORIZON_DEFAULT = LEVEL_2_MAX;
  public final String  LEVEL_6_TIME_HORIZON = "Level6TimeHorizon";
  public final Integer LEVEL_6_TIME_HORIZON_DEFAULT = LEVEL_6_MAX;

  public final int NUM_TRANSPORT_CLASSES = 9;
  public final int ASSET_CLASS_UNKNOWN = 0;
  public final int ASSET_CLASS_1 = 1;
  public final int ASSET_CLASS_2 = 2;
  public final int ASSET_CLASS_3 = 3;
  public final int ASSET_CLASS_4 = 4;
  public final int ASSET_CLASS_5 = 5;
  public final int ASSET_CLASS_6 = 6;
  public final int ASSET_CLASS_7 = 7;
  public final int ASSET_CLASS_8 = 8;
  public final int ASSET_CLASS_9 = 9;
  public final int ASSET_CLASS_10 = 10;
  public final int ASSET_CLASS_CONTAINER = 11;
  public final int ASSET_CLASS_PERSON = 12;
  public final long DAY_IN_MILLIS = MILLIS_PER_DAY; //30*1000l;

  public void localSetup() {     
    super.localSetup();

    try {
      myExpandAggregates = (getMyParams ().hasParam ("ExpandAggregates")) ?
	getMyParams().getBooleanParam("ExpandAggregates") :
	true;
    } catch (Exception e) { warn ("got really unexpected exception " + e); }

    glmPrepHelper = new GLMPrepPhrase (logger);
    glmPrefHelper = new GLMPreference (logger);
    glmAssetHelper = new AssetUtil (logger);

    setupOperatingModes ();
    portLocator.setFactory (ldmf); // tell route finder the ldm factory to use
  }

  /** create the port locator */
  public void setupFilters () {
    super.setupFilters ();
    portLocator = new PortLocatorImpl (this, logger);
    
    addFilter (modeCallback = new OperatingModeCallback (this, logger));
  }

    UTILFilterCallback modeCallback;

    class OperatingModeCallback extends UTILFilterCallbackAdapter {
	public OperatingModeCallback (UTILFilterCallbackListener listener, Logger logger) {
	    super (listener, logger);
	}
	protected UnaryPredicate getPredicate () {
	    return new UnaryPredicate() {
		    public boolean execute(Object o) { 
			boolean val = (o instanceof OperatingMode);
			if (val && logger.isInfoEnabled())
			    logger.info ("GLMTransOneToManyExpanderPlugin.OperatingModeCallback.getPredicate - interested in " + o);
			return val;
		    }
		};
	}
	public void reactToChangedFilter () {
	    if (isInfoEnabled())
		info (getName () + " operating modes sub changed " +
		      modeCallback.getSubscription ().getAddedCollection().size () + " added, " + 
		      modeCallback.getSubscription ().getChangedCollection().size () + " changed, " + 
		      modeCallback.getSubscription ().getRemovedCollection().size () + " removed");
	    if (!modeCallback.getSubscription().getChangedCollection().isEmpty ()) {
		if (isInfoEnabled())
		    info (getName () + " operating modes changed, so reviewing level 2 tasks.");
		reviewLevel2 ();
		if (isInfoEnabled())
		    info (getName () + " operating modes changed, so reviewing deferred tasks.");
		processTasks (new ArrayList(myInputTaskCallback.getSubscription().getCollection()));
	    }
	}

    }

  /** create and publish level-2 and 6 VTH Operating Modes */
  protected void setupOperatingModes () {
    Collection modes = 
      blackboard.query (new UnaryPredicate() { 
	  public boolean execute (Object obj) { return (obj instanceof OperatingMode); }
	});

    if (modes.isEmpty()) {
      OMCRange level2Range = new IntRange (LEVEL_2_MIN.intValue(), LEVEL_2_MAX.intValue());
      OMCRangeList rangeList = new OMCRangeList (level2Range);
      publishAdd (level2Horizon = new OperatingModeImpl (LEVEL_2_TIME_HORIZON, rangeList, 
							 LEVEL_2_TIME_HORIZON_DEFAULT));

      OMCRange level6Range = new IntRange (LEVEL_6_MIN.intValue(), LEVEL_6_MAX.intValue());
      rangeList = new OMCRangeList (level6Range);
      publishAdd (level6Horizon = new OperatingModeImpl (LEVEL_6_TIME_HORIZON, rangeList,
							 LEVEL_6_TIME_HORIZON_DEFAULT));

      if (isInfoEnabled())
	info (getBindingSite().getAgentIdentifier() + " created operating modes - " + 
	      "level 2 time horizon is " + level2Horizon + 
	      " and level 6 is " + level6Horizon);

    } else {
      if (isInfoEnabled()) {
	info (getName() + " skipping creating of operating modes since got them from blackboard.");
      }

      if (modes.size () != 2) {
	error (getName() + " expecting two operating modes on rehydation - got " + modes.size () + 
	       " instead : " + modes);
      }

      for (Iterator iter = modes.iterator(); iter.hasNext(); ) {
	OperatingMode mode = (OperatingMode) iter.next();
	if (mode.getName().equals (LEVEL_2_TIME_HORIZON))
	  level2Horizon = mode;
	else
	  level6Horizon = mode;
      }

    }
  }

  protected static class IntRange extends OMCRange {
    public IntRange (int a, int b) { super (a, b); }
  }

  /** 
   * Implemented for UTILGenericListener interface
   *
   * Look for tasks that have TRANSPORT as their verb
   *
   * @param t Task to check for interest
   * @return boolean true if task is interesting
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

    if (glmPrepHelper == null)
	error (getName() + ".isTaskWellFormed - huh? glmPrepHelper is null??");

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

  public void processTasks (java.util.List tasks) {
      super.processTasks (getPrunedTaskList(tasks));
      
      if (isWarnEnabled()) {
	  warn ("Finished processing " + tasks.size() + " tasks at " + new Date(alarmService.currentTimeMillis()) + 
		" Cougaar Time " + new Date() + " clock time.");
      }
  }

    protected List getPrunedTaskList (List tasks) {
	java.util.List prunedTasks = new java.util.ArrayList(tasks.size());

	Collection removed = myInputTaskCallback.getSubscription().getRemovedCollection();

	for (Iterator iter = tasks.iterator(); iter.hasNext();){
	    Task task = (Task) iter.next();
	    if (removed.contains(task)) {
		if (isInfoEnabled()) {
		    info ("ignoring task on removed list " + task.getUID());
		}
	    }
	    else
		prunedTasks.add (task);
	}
	return prunedTasks;
    }

  public void handleTask(Task parentTask) {
    int mode = getMode (parentTask);
    if (isDebugEnabled()) {
      debug (".getSubtasks - mode for task " + parentTask.getUID () + " is " + 
	    ((mode==LEVEL_6_MODE) ? "LEVEL_6" : ((mode==LEVEL_2_MODE) ? "LEVEL_2" : "DONT_PROCESS")));
    }

    if (parentTask.getPlanElement () != null) { // shouldn't it have been removed from my collection???
      if (isInfoEnabled ()) {
	info (getName() + ".getSubtasks - skipping previously planned task " + parentTask.getUID() + ".");
      }
      return;
    }
      
    if (mode != LEVEL_6_MODE) {
      synchronized (alarmMutex) { // alarm.expire will try to clear currentAlarm so must synchronize on it
	if (currentAlarm == null) {
	  startAgainIn (DAY_IN_MILLIS);
	}
	else {
	  if (isDebugEnabled ()) {
	    debug (getName() + ".getSubtasks - not starting new alarm.");
	  }
	}
      }

      if (mode == DONT_PROCESS_MODE) {
	if (isInfoEnabled ()) {
	  info (getName() + ".getSubtasks - not processing " + parentTask.getUID() + " for now (= " +
		new Date(alarmService.currentTimeMillis()) + "), will revisit at " +
		new Date(currentAlarm.getExpirationTime ()));
	}
      }
      else {
	if (isInfoEnabled ()) {
	  info (getName() + ".getSubtasks - processing " + parentTask.getUID() + " at level 2 for now (= " +
		new Date(alarmService.currentTimeMillis()) + "), will revisit at " +
		new Date(currentAlarm.getExpirationTime ()));
	}
	super.handleTask (parentTask);
      }

      return;
    }
    else 
      super.handleTask(parentTask);
  }

  int i = 0;

  /**
   * <pre>
   * Implemented for UTILExpanderPlugin interface
   *
   * Break up tasks into constituent parts.
   *
   * There are three possible paths a task can take:
   * 
   * 1) If the task is already a LEVEL 2 task, attaches a lowFiAssetPG to the asset and 
   * passes it through.
   * 2) If the task's dates and the operating mode VTH determine it should be handled
   * in level-2 mode, create a level-2 task from the original
   * 3) If the task should be handled in level-6, expand it's d.o. as usual
   * 4) TBD - perhaps, in the future, we'll ignore tasks that are far into the future
   * until the current time advances far enough.  This would require the buffering thread
   * to wake up periodically, however, using the alarm service.  Hmmm...
   *
   * There is also the possibility of rescinding level 2 and replanning as level 6.  
   * That would be real work though.
   * </pre>
   * @return Vector of subtasks of parentTask
   */
  public Vector getSubtasks(Task parentTask) {
    Vector childTasks = new Vector ();

    if (isInfoEnabled())
      info (".getSubtasks - received task " + (i++) + " total.");

    int mode = getMode (parentTask);
    if (isDebugEnabled()) {
      debug (".getSubtasks - mode for task " + parentTask.getUID () + " is " + 
	     ((mode==LEVEL_6_MODE) ? "LEVEL_6" : ((mode==LEVEL_2_MODE) ? "LEVEL_2" : "DONT_PROCESS")));
    }

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
    else if ((getMode(parentTask) == LEVEL_2_MODE) && 
	     ((parentTask.getDirectObject() instanceof AssetGroup) ||      // it doesn't make sense to aggregate an individual item asset
	      (parentTask.getDirectObject() instanceof AggregateAsset)) && 
	     !isPersonTask(parentTask)) {
      if (isDebugEnabled()) 
	debug (".getSubtasks - processing task " + parentTask.getUID () + 
	       " in LOW fidelity mode. d.o. is " + parentTask.getDirectObject());

      Set [] categories = sortAssetsByCategory(expandAsset(parentTask, parentTask.getDirectObject()));

      for (int i = 0; i < NUM_TRANSPORT_CLASSES; i++) {
	if (!categories[i].isEmpty())
	  childTasks.add(getLowFidelityTask (parentTask, categories[i]));
      }
    }
    else {
      if (isDebugEnabled()) 
	debug (".getSubtasks - processing task " + parentTask.getUID () + 
	       " in HIGH fidelity mode. d.o. is " + parentTask.getDirectObject());

      Vector itemsToMove = expandAsset(parentTask, (Asset)parentTask.getDirectObject());

      for (int i = 0; i < itemsToMove.size (); i++) {
	Task subTask = makeTask (parentTask, (Asset) itemsToMove.elementAt(i), null);
	childTasks.addElement (subTask);
      }
    }

    return childTasks;
  }

  /** 
   * Decide the mode the task should be processed in, depending on when it asks to be done,
   * and comparing that time against the current time and the level 2 and 6 time horizons
   * 
   * @return either LEVEL_6_MODE, LEVEL_2_MODE, or DONT_PROCESS_MODE (currently not supported)
   */
  protected int getMode (Task parentTask) {
    long bestTime = prefHelper.getBestDate(parentTask).getTime();
    long currentTime = getAlarmService().currentTimeMillis();
    long level6Day = ((Integer)level6Horizon.getValue()).longValue();
    long level2Day = ((Integer)level2Horizon.getValue()).longValue();
    if (bestTime < currentTime + level6Day*MILLIS_PER_DAY) {
      return LEVEL_6_MODE;
    }
    else if (bestTime < currentTime + level2Day*MILLIS_PER_DAY) {
      if (isInfoEnabled())
	info (getName() + ".getMode - got level 2 task (" + parentTask.getUID () + 
	      ")\nsince best " + new java.util.Date(bestTime) +
	      " is after current " + new java.util.Date (currentTime) + 
	      " + level6 days " + level6Day);
      return LEVEL_2_MODE;
    }
    return DONT_PROCESS_MODE;
  }

  protected Set [] sortAssetsByCategory (Collection assets) {
    Set [] categories = new Set[NUM_TRANSPORT_CLASSES];

    categories[0] = new HashSet();
    categories[1] = new HashSet();
    categories[2] = new HashSet();
    categories[3] = new HashSet();
    categories[4] = new HashSet();
    categories[5] = new HashSet();
    categories[6] = new HashSet();
    categories[7] = new HashSet();
    categories[8] = new HashSet();

    for (Iterator iter = assets.iterator(); iter.hasNext();) { 
      GLMAsset asset = (GLMAsset) iter.next();
      String ccc = getCategory(asset);

      switch (ccc.charAt(1)) {
      case '0': // non-air
	if (ccc.charAt(0) == 'R') {
	  categories[4].add(asset);
	} else {
	  categories[0].add(asset); 
	}
	break;
      case '1': // outsized
	if (ccc.charAt(0) == 'R') {
	  categories[5].add(asset);
	} else {
	  categories[1].add(asset); 
	} 
	break;
      case '2': // oversized
	if (ccc.charAt(0) == 'R') {
	  categories[6].add(asset);
	} else {
	  categories[2].add(asset); 
	}
	break;
      default: // bulk or unknown
	if (asset instanceof Container) {
	  categories[8].add (asset);
	} else if (ccc.charAt(0) == 'R') {
	  categories[7].add(asset);
	} else {
	  categories[3].add(asset);
	}
	break;
      }
    }

    return categories;
  }

  /** create level 2 task by aggregating the contents of the direct object **/
  protected Task getLowFidelityTask (Task parentTask, Set uniformAssets) {
    Task newTask = null;

    GLMAsset lowFiAsset = null;
    NewLowFidelityAssetPG lowFiPG = 
      (NewLowFidelityAssetPG)ldmf.createPropertyGroup(LowFidelityAssetPG.class);
    NewMovabilityPG movabilityPG = 
      (NewMovabilityPG)ldmf.createPropertyGroup(MovabilityPG.class);

    try {
      NewPhysicalPG newPhysicalPG = PropertyGroupFactory.newPhysicalPG ();
      CargoCatCodeDimensionPG cccd = setDimensions (parentTask, newPhysicalPG, uniformAssets);
      lowFiPG.setCCCDim(cccd);

      lowFiAsset = 
	(GLMAsset) assetHelper.createInstance (getLDMService().getLDM(), "Level2Prototype", 
					       "Level2_" + getTransportType(cccd) + "_" + getNextID());
      ((NewItemIdentificationPG) lowFiAsset.getItemIdentificationPG()).setNomenclature ("Level2Aggregate");

      lowFiPG.setOriginalAsset (lowFiAsset); // now subobject can have a pointer back to parent

      lowFiAsset.setPhysicalPG (newPhysicalPG);
      lowFiAsset.addOtherPropertyGroup(lowFiPG);
      lowFiAsset.setMovabilityPG(movabilityPG);

      movabilityPG.setCargoCategoryCode(cccd.getCargoCatCode());

      if (!movabilityPG.getCargoCategoryCode().equals(cccd.getCargoCatCode()))
	logger.error ("huh? for asset " + lowFiAsset + " on task " + parentTask.getUID() +  
		      " movabilityPG ccc " + lowFiAsset.getMovabilityPG ().getCargoCategoryCode() + 
		      " != " + cccd.getCargoCatCode());
 
    } catch (Exception e) { 
      Asset asset = parentTask.getDirectObject();
      logger.error ("problem processing task " + parentTask.getUID() + "'s d.o. asset" + asset, e);
      return null;
    }

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

    return newTask;
  }

  /** return a human-readable string to indicate transport type */
  protected String getTransportType (CargoCatCodeDimensionPG cccd) {
    String ccc = cccd.getCargoCatCode ();
    String suffix = (ccc.charAt(0) == 'R') ? "_Roadable" : "";

    switch (ccc.charAt(1)) {
    case '0': // non-air
      return "Non-air Transport" + suffix;
    case '1': // outsized
      return "Outsized" + suffix;
    case '2': // oversized
      return "Oversized" + suffix;
    default: // bulk or unknown
      if (cccd.getIsContainer()) {
	return "Container" + suffix;
      }
      else {
	return "Bulk" + suffix;
      }
    }
  }

  /** 
   * Recovers owner of task's d.o. from either a FOR prep on the task
   * or the owner part of UID of the d.o.
   * @param parentTask task to examine
   * @return owner of the task = which unit sent the task, owns the asset
   */
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
   *
   * @param asset to attach PG to
   * @param thisPG pg to attach to asset
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

  /** 
   * <pre>
   * Tries to determine if direct object is a person aggregate.
   * 
   * Can be tricky since sometimes the d.o. is a group of aggregates.
   * </pre>
   */
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
      Vector assetList = group.getAssets();
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

  /**
   * <pre>
   * Takes a collection of assets and sets the dimensions of the physical pg
   * to be their sum, in area, volume, and weight.
   *
   * It doesn't make sense to aggregate length, width, and height, since they
   * wouldn't correspond to area and volume.
   * </pre>
   * @param realAssets to sum
   * @param physicalPG to set with their aggregate dimensions
   **/
  protected CargoCatCodeDimensionPG setDimensions (Task parentTask, NewPhysicalPG physicalPG, Collection realAssets) {
    double area   = 0.0;
    double volume = 0.0;
    double mass   = 0.0;

    Object firstItem = realAssets.iterator().next();

    PhysicalPG cccdPhysicalPG         = (PhysicalPG)ldmf.createPropertyGroup(PhysicalPG.class);
    NewCargoCatCodeDimensionPG cccdPG = (NewCargoCatCodeDimensionPG)ldmf.createPropertyGroup(CargoCatCodeDimensionPG.class);
    cccdPG.setDimensions(cccdPhysicalPG);

    if (firstItem instanceof AggregateAsset) { // there is only one item...
      if (realAssets.size () > 1)
	logger.error (getBindingSite().getAgentIdentifier() + " found aggregate, but skipping some expanded items???");

      AggregateAsset aggAsset = (AggregateAsset) firstItem;
      GLMAsset baseAsset = (GLMAsset)aggAsset.getAsset();
      PhysicalPG itemPhysicalPG = baseAsset.getPhysicalPG();
      String ccc = getCategory(baseAsset);
	
      if (itemPhysicalPG == null) {
	if (!baseAsset.hasPersonPG()) {
	  warn (".setDimensions - for task " + parentTask.getUID() + " asset " + firstItem + 
		"'s base asset " + (GLMAsset)aggAsset.getAsset() + " has no physical PG.");
	}
	else if (isDebugEnabled ()) {
	  debug (".setDimensions - NOTE : asset " + firstItem + 
		 "'s base asset " + (GLMAsset)aggAsset.getAsset() + " has no physical PG.");
	}
      }
      else {
	long quantity = aggAsset.getQuantity();
	double q = (double)quantity;

	// it doesn't make sense to display aggregate length, width, height,
	// since they won't correspond to area and volume
	area   = itemPhysicalPG.getFootprintArea().getSquareMeters() * q;
	volume = itemPhysicalPG.getVolume().getCubicMeters() * q;
	mass   = itemPhysicalPG.getMass().getKilograms() * q;

	if (area == 0.0d) {
	  area = itemPhysicalPG.getLength().getMeters() * 
	    itemPhysicalPG.getWidth().getMeters()* q;
	  if (isInfoEnabled ()) {
	    info (".setDimensions - fixing footprint area, was zero for asset " + aggAsset + 
		  " on task " + parentTask.getUID());
	  }
	}
	if (area == 0.0d || volume == 0.0d || mass == 0.0d) {
	  if (isWarnEnabled()) {
	    warn (".setDimensions - asset " + firstItem + 
		  " for task " + parentTask.getUID() + " has a zero dimension.");
	  }
	}
		
	addToDimension (ccc, q, itemPhysicalPG, cccdPG);

	if (baseAsset instanceof Container)
	  cccdPG.setIsContainer(true);
	cccdPG.setAssetClass(getAssetClass(baseAsset));
      }
    }
    else {
      Object last = null;
      for (Iterator iter = realAssets.iterator(); iter.hasNext();) {
	GLMAsset asset = (GLMAsset)iter.next();
	last = asset;
	PhysicalPG itemPhysicalPG = asset.getPhysicalPG();
	String ccc = getCategory(asset);

	if (itemPhysicalPG == null)
	  error ("Asset " + asset + " doesn't have a physical PG.");
	else {
	  // it doesn't make sense to display aggregate length, width, height,
	  // since they won't correspond to area and volume
	  double itemArea   = itemPhysicalPG.getFootprintArea().getSquareMeters();
	  double itemVolume = itemPhysicalPG.getVolume().getCubicMeters();
	  double itemMass   = itemPhysicalPG.getMass().getKilograms();

	  if (itemArea < 0.01d) {
	    itemArea = itemPhysicalPG.getLength().getMeters() * 
	      itemPhysicalPG.getWidth().getMeters();
	    if (isInfoEnabled ()) {
	      info (".setDimensions - fixing footprint area, was zero for asset " + asset + " on task " + parentTask.getUID());
	    }
	  }

	  area   += itemArea;
	  volume += itemVolume;
	  mass   += itemMass;

	  if (itemArea == 0.0d || itemVolume == 0.0d || itemMass == 0.0d) {
	    if (isWarnEnabled ()) {
	      warn (".setDimensions - asset " + asset + 
		    " for task " + parentTask.getUID() + " has a zero dimension.");
	    }
	  }

	  addToDimension (ccc, 1.0, itemPhysicalPG, cccdPG);
	}
      }
      if (last instanceof Container)
	cccdPG.setIsContainer(true);
      cccdPG.setAssetClass(getAssetClass((Asset)last));
    }

    physicalPG.setMass  (new Mass     (mass,   Mass.KILOGRAMS ));
    physicalPG.setFootprintArea(new Area (area,   Area.SQUARE_METERS));
    physicalPG.setVolume(new Volume   (volume,   Volume.CUBIC_METERS));

    if (isDebugEnabled()) {
      debug (".setDimensions got " + realAssets.size () + " items.");
      debug (".setDimensions low fi dimensions : " + 
	     " m " + physicalPG.getMass () + 
	     " a " + physicalPG.getFootprintArea () + 
	     " v " + physicalPG.getVolume ());
    }

    return cccdPG;
  }

  protected String getCategory (GLMAsset asset) {
    MovabilityPG movabilityPG = asset.getMovabilityPG();

    String ccc;

    if (movabilityPG == null) {
      ccc = "XXX";
      logger.warn (getBindingSite().getAgentIdentifier() + " " + asset +
		   " was missing a movability PG, so could not determine cargo cat code.");
    }
    else {
      ccc = movabilityPG.getCargoCategoryCode();
      if (logger.isDebugEnabled ())
	logger.debug (asset.toString () + " ccc was " + ccc);
    }

    return ccc;
  }

  protected void addToDimension (String ccc, 
				 double quantity,
				 PhysicalPG itemPhysicalPG,
				 CargoCatCodeDimensionPG cccdPG) {
    NewPhysicalPG whichPG = (NewPhysicalPG) cccdPG.getDimensions();
    NewCargoCatCodeDimensionPG whichCCCDimPG = (NewCargoCatCodeDimensionPG) cccdPG;

    if (whichPG == null) {
      logger.error ("Could not set physical PG?");
    }

    String pgCCCString = whichCCCDimPG.getCargoCatCode();

    if (pgCCCString == null) { // we've never set the cargo cat code
      whichCCCDimPG.setCargoCatCode(ccc);
    }
    else if (!pgCCCString.equals(ccc)) {
      char [] pgCCC = pgCCCString.toCharArray();
      char [] newCCC = new char [3];
      char [] cccArray = ccc.toCharArray ();

      if (isDebugEnabled())
	debug (".addToDimension old " + new String(pgCCC) + " additional ccc " + ccc);

      if (cccArray[0] != pgCCC[0]) {
	newCCC[0]='X';
      } else {
	newCCC[0]=pgCCC[0];
      }

      if (cccArray[1] != pgCCC[1]) {
	newCCC[1]='X';
      } else {
	newCCC[1]=pgCCC[1];
      }

      if (cccArray[2] != pgCCC[2]) {
	newCCC[2]='X';
      } else {
	newCCC[2]=pgCCC[2];
      }

      whichCCCDimPG.setCargoCatCode(new String(newCCC));
    }

    double area = 0.0d;
    double volume = 0.0d;
    double mass = 0.0d;

    if (whichPG.getFootprintArea () != null) {
      area   = whichPG.getFootprintArea().getSquareMeters ();
      /*
      if (area < 0.01d) {
	area = whichPG.getLength().getMeters() * whichPG.getWidth().getMeters();
	if (isInfoEnabled ()) {
	  info (".addToDimensions - fixing footprint area, was zero for asset");
	}
      }
      */
      volume = whichPG.getVolume().getCubicMeters ();
      mass   = whichPG.getMass().getKilograms ();
    } 

    whichPG.setFootprintArea (new Area (area +
					itemPhysicalPG.getFootprintArea().getSquareMeters() * quantity, Area.SQUARE_METERS));
    whichPG.setVolume        (new Volume (volume +
					  itemPhysicalPG.getVolume().getCubicMeters() * quantity, Volume.CUBIC_METERS));
    whichPG.setMass          (new Mass (mass +
					itemPhysicalPG.getMass().getKilograms() * quantity, Mass.KILOGRAMS));

    if (isDebugEnabled())
      debug (".addToDimension final dim for " + ccc + " - " + 
	     whichPG.getFootprintArea().getSquareMeters() + " m^2" + 
	     whichPG.getVolume().getCubicMeters() + " m^3 " + 
	     whichPG.getMass().getShortTons() + " short tons.");
  }

  private int getAssetClass(Asset a) {
    int value = (a instanceof ClassISubsistence) ? 
      ASSET_CLASS_1 :
      (a instanceof ClassIIClothingAndEquipment) ?
      ASSET_CLASS_2 :
      (a instanceof ClassIIIPOL) ?
      ASSET_CLASS_3 :
      (a instanceof ClassIVConstructionMaterial) ?
      ASSET_CLASS_4 :
      (a instanceof ClassVAmmunition) ?
      ASSET_CLASS_5 :
      (a instanceof ClassVIPersonalDemandItem) ?
      ASSET_CLASS_6 :
      (a instanceof ClassVIIMajorEndItem) ?
      ASSET_CLASS_7 :
      (a instanceof ClassVIIIMedical) ?
      ASSET_CLASS_8 :
      (a instanceof ClassIXRepairPart) ?
      ASSET_CLASS_9 :
      (a instanceof ClassXNonMilitaryItem) ?
      ASSET_CLASS_10 :
      ((a instanceof Container) ||
       (a instanceof org.cougaar.glm.ldm.asset.Package)// ||
       //       (glmAssetHelper.isPallet(a))) ?
       ) ?
      ASSET_CLASS_CONTAINER :
      ((a instanceof Person) ||
       ((a instanceof GLMAsset) &&
        (((GLMAsset)a).hasPersonPG()))) ?
      ASSET_CLASS_PERSON :
      ASSET_CLASS_UNKNOWN;

    return value;
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
	info (".getSubtasks - NOTE : got task " + parentTask.getUID() + 
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

  /** 
   * <pre>
   * Attaches two preps to task : SEAROUTE and SEAROUTE_DISTANCE 
   *
   * SEAROUTE will appear in the TPFDD Viewer as the path of the ship, if the 
   * item goes by ship.  SEAROUTE_DISTANCE is used by the TRANSCOM vishnu scheduler
   * to make the sea-vs-air decision.  (Specifically, it decides if at a reasonable
   * ship speed (~15 knots), the task could be completed in the time allowed by ship,
   * and if so, decides to send the item by ship.)
   * </pre>
   * @param parentTask - used to calculate the route (using the from-to pair)
   * @param subtask - task to attach the preps to
   */
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
  public Vector expandAsset(Task task, Asset asset) {
    if (myExpandAggregates) {
      if (isDebugEnabled())
	debug (getName() + ".expandAsset - expanding aggregate asset " + asset);

      Vector items = glmAssetHelper.ExpandAsset(getDomainService().getFactory (), asset);
      if (isDebugEnabled())
	debug (getName() + ".expandAsset - aggregate asset had " + items.size () + " items.");
      return items;
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

  /** Buffering runnable wants to restart later */
  public void startAgainIn (long millis) {
    if (isInfoEnabled())
	info (getName () + " asking to be restarted in " + millis);

    if (currentAlarm != null)
      currentAlarm.cancel ();

    alarmService.addAlarm (currentAlarm = new BufferingAlarm (millis));
  }

  /** Alarm for when buffering runnable wants to restart later */
  class BufferingAlarm implements Alarm {
    long clockExpireTime;
    boolean expired = false;

    public BufferingAlarm (long ticksToWait) {
      clockExpireTime = alarmService.currentTimeMillis() + ticksToWait; // based on wall clock time
    }

    /** @return absolute time (in milliseconds) that the Alarm should
     * go off.  
     * This value must be implemented as a fixed value.
     **/
    public long getExpirationTime() {
      return clockExpireTime;
    }
  
    /** 
     * Called by the cluster clock when clock-time >= getExpirationTime().
     * The system will attempt to Expire the Alarm as soon as possible on 
     * or after the ExpirationTime, but cannot guarantee any specific
     * maximum latency.
     * NOTE: this will be called in the thread of the cluster clock.  
     * Implementations should make certain that this code does not block
     * for a significant length of time.
     * If the alarm has been canceled, this should be a no-op.
     **/
    public synchronized void expire() {
      if (!expired) {
	synchronized (alarmMutex) { // getSubtasks will check to see if one exists, so must synchronize
	  currentAlarm = null;
	}

	expired = true;

	String name = getName()+"_restartThread";

	if (isInfoEnabled())
	  info (getName () + " re-examining unprocessed tasks.");

	schedulable = 
	  threadService.getThread (this, 
				   new Runnable () {
				       public void run() {
					 // review the known task list
					 try {
					   blackboard.openTransaction ();
					   reviewLevel2 ();
					   processTasks (new ArrayList(myInputTaskCallback.getSubscription().getCollection()));
					 } catch (Throwable t) {
					   System.err.println("Error: Uncaught exception in "+this+": "+t);
					   t.printStackTrace();
					 } finally {
					   blackboard.closeTransaction();
					 }
				       }
				     }, 
				   name);
	schedulable.start();
      }
    }

    /** @return true IFF the alarm has rung (expired) or was canceled. **/
    public boolean hasExpired() { 
      return expired;
    }

    /** 
     * Can be called by a client to cancel the alarm.  May or may not remove
     * the alarm from the queue, but should prevent expire from doing anything.
     * @return false IF the the alarm has already expired or was already canceled.
     **/
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }

    public String toString() {
      return "<BufferingAlarm "+clockExpireTime+
        (expired?"(Expired) ":" ")+
        "for "+GLMTransOneToManyExpanderPlugin.this.toString()+">";
    }
  }

  public void reviewLevel2 () {
    // review the known task list
    replanLevel2 (blackboard.query (new UnaryPredicate() {
	public boolean execute (Object obj) {
	  if (obj instanceof Task) {
	    Task task = (Task) obj;
	    if (interestingTask (task)) {
	      // if just by looking at the dates it ought to be level 6
	      if (getMode (task) == LEVEL_6_MODE) {
		if (task.getPlanElement () != null && (task.getPlanElement () instanceof Expansion)) {
		  Task subtask = 
		    (Task) ((Expansion)task.getPlanElement ()).getWorkflow().getTasks().nextElement ();
		  // but it's got low fi children
		  return (prepHelper.hasPrepNamed (subtask, GLMTransConst.LOW_FIDELITY));
		} else return false;
	      } else return false;
	    } else return false;
	  } else return false;
	}
      }));
  }

  protected void replanLevel2 (Collection level2Tasks) {
    for (Iterator iter = level2Tasks.iterator(); iter.hasNext();) {
      NewTask level2 = (NewTask) iter.next();
      
      PlanElement pe = level2.getPlanElement ();
      publishRemove (pe);
      publishChange (level2);

      // replan the task
      
      if (isInfoEnabled())
	info (getName () + ".reviewLevel2 - replanning task " + level2.getUID() + 
	      " as a Level 6 task.");
    }
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
  protected Alarm currentAlarm;
  protected Schedulable schedulable; 
  protected Object alarmMutex = new Object();
}
