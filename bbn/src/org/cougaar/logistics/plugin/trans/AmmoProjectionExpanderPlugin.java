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
    double ratePerSec = prefHelper.getPreferenceBestValue (pref); // base number is in quantity per second!
    double ratePerDay = ratePerSec*SECS_PER_DAY; 

    Date readyAt = prefHelper.getReadyAt   (parentTask);
    Date early   = getEarlyDate (parentTask);
    Date best    = prefHelper.getBestDate  (parentTask);
    Date late    = getLateDate  (parentTask);

    if (early.getTime () < readyAt.getTime ())
      early = readyAt;
    if (best.getTime () < readyAt.getTime ())
      best = readyAt;

    long window = best.getTime () - readyAt.getTime();
    long originalWindowInDays = window/MILLIS_PER_DAY;
    long numSubtasks = window/(CHUNK_DAYS*MILLIS_PER_DAY);
    if (window - (numSubtasks * CHUNK_DAYS*MILLIS_PER_DAY) != 0)
      numSubtasks++;

    if (isInfoEnabled ())
      info (getName () + ".getSubtasks - task " + parentTask.getUID () + 
	     " from " + readyAt + 
	     " to " + best + 
	     " will produce " + numSubtasks + " subtasks.");

    // create one subtask for every chunk set of days, with an asset that is the total
    // delivered over the period = days*ratePerDay
    double daysSoFar = 0;
    int totalQuantity = 0;
    int targetQuantity = (int) (((double) (window/1000l))*ratePerSec);
    Date lastBestDate = readyAt;

    if (isInfoEnabled ())
      info (getName () + ".getSubtasks - task " + parentTask.getUID () + " target quantity " + targetQuantity + 
	    " windowInSec " + window/1000l + " rate/sec " + ratePerSec);

    for (int i = 0; i < (int) numSubtasks; i++) {
      boolean onLastTask = (window/MILLIS_PER_DAY) < CHUNK_DAYS;
      double daysToChunk = (onLastTask) ? window/MILLIS_PER_DAY : CHUNK_DAYS;

      if (isInfoEnabled () && onLastTask)
	info ("on last task - days " + daysToChunk + " since " + window/MILLIS_PER_DAY + " < " + CHUNK_DAYS);

      daysSoFar += daysToChunk;
      window    -= ((long)daysToChunk)*MILLIS_PER_DAY;
      int quantity = (int) (daysToChunk * ratePerDay);
      if (onLastTask && ((totalQuantity + quantity) != targetQuantity)) {
	if (isInfoEnabled ())
	  info (" task " + parentTask.getUID () + 
		" adjusting quantity from " +quantity + 
		" to " + (targetQuantity - totalQuantity) + 
		" total is " + totalQuantity);
	quantity = targetQuantity - totalQuantity;
      }
      else if (isInfoEnabled ())
	info (".getSubtasks - task " + parentTask.getUID () + " quantity is " + quantity + 
	      " chunk days " + daysToChunk+ " rate " + ratePerDay);
      if (quantity < 1) {
	error (".getSubtasks - task " + parentTask.getUID () + 
	       " gets a quantity of zero, ratePerDay was " + ratePerDay + 
	       " chunk days " +daysToChunk);
      }

      AggregateAsset deliveredAsset = createDeliveredAsset (parentTask, supplyAsset, quantity);
      totalQuantity += deliveredAsset.getQuantity ();

      if (deliveredAsset.getQuantity () != quantity) {
	error (".getSubtasks - task " + parentTask.getUID () + " quantities don't match");
      }

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
      long bestTime = early.getTime() + ((long)daysSoFar)*MILLIS_PER_DAY;
      if (bestTime > best.getTime()) {
	if (isInfoEnabled())
	  info (getName () + 
		".getSubtasks - had to correct bestTime, was " + new Date (bestTime) + 
		" now " + best);
	bestTime = best.getTime();
      }

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
      prepHelper.addPrepToTask (subTask, prepHelper.makePrepositionalPhrase (ldmf, "Start", lastBestDate));
      lastBestDate = new Date (bestTime);
      childTasks.addElement (subTask);
    }

    // post condition
    if (totalQuantity != targetQuantity) {
      if (isWarnEnabled ())
	warn (getName () + " total quantity " + totalQuantity + 
	      " != original total " + targetQuantity +
	      " = window " + originalWindowInDays + 
	      " * ratePerDay " + ratePerDay);
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
  protected AggregateAsset createDeliveredAsset (Task originalTask, Asset originalAsset, int quantity) {
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

    AggregateAsset deliveredAsset = (AggregateAsset) ldmf.createAggregate(prototype, quantity);

    return deliveredAsset;
  }
}
