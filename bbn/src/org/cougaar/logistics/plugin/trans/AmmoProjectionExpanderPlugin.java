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
package org.cougaar.logistics.plugin.trans;

import java.util.Date;
import java.util.Vector;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.TimeAspectValue;

import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.packer.Geolocs;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.planning.ldm.measure.*;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.lib.util.UTILPreference;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.asset.PhysicalAsset;

/**
 * Takes PROJECT_SUPPLY tasks 
 * and converts them into TRANSPORT tasks with a RESERVATION prep. <p>
 *
 */
public class AmmoProjectionExpanderPlugin extends AmmoLowFidelityExpanderPlugin {
    public static long CHUNK_DAYS = 30;
    public static long MILLIS_PER_DAY = 1000*60*60*24;
    public static long SECS_PER_DAY = 60*60*24;

  /**
   * State that we are interested in all transport tasks
   * @param task the task to test.
   * @return true if the tasks verb is SUPPLY, false otherwise
   */
  public boolean interestingTask(Task task){
    boolean hasSupply = task.getVerb().equals (Constants.Verb.PROJECTSUPPLY);
    if (!hasSupply)
      return false;

    if (isDebugEnabled())
      debug (".interestingTask - processing PROJECT_SUPPLY task " + task.getUID ());

    return true;
  }
  public void handleTask(Task t) {
    super.handleTask(t);
    //    if (t.getWorkflow() != null)
    //      warn ((t.getWorkflow ().isPropagatingToSubtasks ()) ? " workflow is propagating " : "not prop");
  }

  int total = 0;

  /**
   * Implemented for UTILExpanderPlugin interface
   *
   * Break up tasks into constituent parts.
   */
  public Vector getSubtasks(Task parentTask) {
    
    Vector childTasks = new Vector ();

    // create the d.o.

    Asset supplyAsset = parentTask.getDirectObject();

    Preference pref = prefHelper.getPrefWithAspectType (parentTask, AlpineAspectType.DEMANDRATE);
    double rate = prefHelper.getPreferenceBestValue (pref)*SECS_PER_DAY; // base number is in quantity per second!

    Date readyAt = prefHelper.getReadyAt   (parentTask);
    Date early   = getEarlyDate (parentTask);
    Date best    = prefHelper.getBestDate  (parentTask);
    Date late    = getLateDate  (parentTask);

    long window = best.getTime () - readyAt.getTime();
    long numSubtasks = window/(CHUNK_DAYS*MILLIS_PER_DAY);
    if (window - (numSubtasks * CHUNK_DAYS*MILLIS_PER_DAY) != 0)
      numSubtasks++;

    if (isDebugEnabled ())
      debug (getName () + ".getSubtasks - task " + parentTask.getUID () + 
	     " from " + readyAt + 
	     " to " + best + 
	     " will produce " + numSubtasks + " subtasks.");

    // create one subtask for every chunk set of days, with an asset that is the total
    // delivered over the period = days*rate

    for (int i = 0; i < (int) numSubtasks; i++) {
      Asset deliveredAsset = createDeliveredAsset (parentTask, supplyAsset, rate, CHUNK_DAYS);

      // set item id pg to show it's a reservation, and not a normal task's asset

      ItemIdentificationPG itemIDPG = (ItemIdentificationPG) supplyAsset.getItemIdentificationPG();
      NewItemIdentificationPG newItemIDPG = (NewItemIdentificationPG) PropertyGroupFactory.newItemIdentificationPG();
      String itemID = itemIDPG.getItemIdentification();
      String itemNomen = itemIDPG.getNomenclature ();

      if (itemID == null) {
	TypeIdentificationPG typeID = supplyAsset.getTypeIdentificationPG ();
	itemID = typeID.getTypeIdentification();
      }

      if (itemNomen == null) {
	TypeIdentificationPG typeID = supplyAsset.getTypeIdentificationPG ();
	itemID = typeID.getNomenclature();
      }

      newItemIDPG.setItemIdentification (itemID    + "_Reservation_" + total);
      newItemIDPG.setNomenclature       (itemNomen + "_Reservation_" + (total++));
      deliveredAsset.setItemIdentificationPG (newItemIDPG);
    
      Task subTask = makeTask (parentTask, deliveredAsset);
      long bestTime = early.getTime() + ((i+1)*CHUNK_DAYS*MILLIS_PER_DAY);
      if (bestTime > best.getTime())
	bestTime = best.getTime();

      if (isDebugEnabled ())
	debug (getName () + ".getSubtasks - making task " + subTask.getUID() + 
	       " with best arrival " + new Date(bestTime));

      prefHelper.replacePreference((NewTask)subTask, 
				   prefHelper.makeEndDatePreference (ldmf,
								     early, 
								     new Date (bestTime),
								     late));
      prefHelper.removePrefWithAspectType (subTask, AlpineAspectType.DEMANDRATE); // we've included it in the d.o.

      prepHelper.removePrepNamed (subTask, Constants.Preposition.MAINTAINING);
      prepHelper.removePrepNamed (subTask, Constants.Preposition.REFILL);

      childTasks.addElement (subTask);
    }

    return childTasks;
  }

  public Date getEarlyDate(Task t) {
    Preference endDatePref = prefHelper.getPrefWithAspectType(t, AspectType.END_TIME);
    AspectScoreRange range = endDatePref.getScoringFunction().getDefinedRange();
    return new Date (((AspectScorePoint) range.getRangeStartPoint ()).getAspectValue ().longValue ());
  }
  public Date getLateDate(Task t) {
    Preference endDatePref = prefHelper.getPrefWithAspectType(t, AspectType.END_TIME);
    AspectScoreRange range = endDatePref.getScoringFunction().getDefinedRange();
    return new Date (((AspectScorePoint) range.getRangeEndPoint ()).getAspectValue ().longValue ());
  }

  protected Enumeration getValidEndDateRanges (Preference endDatePref) {
    Calendar cal = java.util.Calendar.getInstance();
    cal.set(2200, 0, 0, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date endOfRange = (Date) cal.getTime();

    Enumeration validRanges = 
      endDatePref.getScoringFunction().getValidRanges (new TimeAspectValue (AspectType.END_TIME,
									    0l),
						       new TimeAspectValue (AspectType.END_TIME,
									    endOfRange));
    return validRanges;
  }

  /** create aggregate asset aggregating the direct object's prototype **/
  protected Asset createDeliveredAsset (Task originalTask, Asset originalAsset, double rate, long chunkDays) {
    Asset prototype = originalAsset.getPrototype ();

    if (prototype == null) {
      prototype = originalAsset;
      GLMAsset glmProto = (GLMAsset)prototype;
      if (!glmProto.hasPhysicalPG()) {
	warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + " - " +
	      prototype + " doesn't have a physical PG - " + glmProto.getPhysicalPG());
	if (!(prototype instanceof PhysicalAsset))
	  error ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + 
		 prototype + " is not a physical asset?.");
	else if (isInfoEnabled()){
	  info ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + 
		" is a physical asset.");
	}
      }
      else { 
	if (glmProto.getPhysicalPG().getFootprintArea() == null) {
	  ((NewPhysicalPG)glmProto.getPhysicalPG()).setFootprintArea (new Area (Area.SQUARE_FEET, 1)); 
	  if (isWarnEnabled()) {
	    warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + 
		  " doesn't have an area slot on its physical pg.");
	  }
	}
	if (glmProto.getPhysicalPG().getVolume() == null) {
	  ((NewPhysicalPG)glmProto.getPhysicalPG()).setVolume (new Volume (Volume.CUBIC_FEET, 1)); 
	  if (isWarnEnabled()) {
	    warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + 
		  " doesn't have a volume slot on its physical pg.");
	  }
	}
      }
      if (!glmProto.hasPackagePG()) {
	warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + " - " + 
	      prototype + " doesn't have a package PG.");
      }
    }

    int quantity = (int) (((double) chunkDays) * rate);

    if (quantity < 1) {
      error ("createDeliveredAsset - task " + originalTask.getUID () + 
	     " gets a quantity of zero, rate was " + rate + 
	     " chunk days " +chunkDays);
    }

    AggregateAsset deliveredAsset = (AggregateAsset)
      ldmf.createAggregate(prototype, (int) ((double) chunkDays * rate));

    return deliveredAsset;
  }
}
