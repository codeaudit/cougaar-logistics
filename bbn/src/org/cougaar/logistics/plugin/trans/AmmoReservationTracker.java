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

import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.cougaar.util.UnaryPredicate;

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

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.lib.util.UTILPreference;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.planning.ldm.plan.Preposition;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;

public class AmmoReservationTracker extends UTILExpanderPluginAdapter {
  public static long MILLIS_PER_DAY = 1000*60*60*24;

  /** 
   * Implemented for UTILBufferingPlugin interface
   *
   * filter for tasks you find interesting. 
   * @param t Task to check for interest
   * @return boolean true if task is interesting
   * @see UTILBufferingPlugin
   */
  public boolean interestingTask(Task t) { 
    boolean hasTransport = t.getVerb().equals (Constants.Verb.TRANSPORT);
    return hasTransport;
  }

  /**
   * find matching reservation transport task
   * see if date overlaps
   * if it does, publish remove it and replace it with one with altered date span and quantity
   */
  public void handleTask(Task task1) {
    // find matching reservation transport task
    final Task task = task1;
    Collection matchingTaskCollection = blackboard.query (new UnaryPredicate () {
      public boolean execute (Object obj) {
	if (!(obj instanceof Task)) return false;
	Task examinedTask = (Task) obj;
	if (task == examinedTask) return false; // don't match yourself

	// better be a transport task
	if (!(examinedTask.getVerb ().equals (Constants.Verb.TRANSPORT)))
	  return false;

	// is it a reservation task?
	if (!prepHelper.hasPrepNamed (examinedTask, "Start"))
	  return false;

	// is it for the same org? redundant, but...
	if (prepHelper.hasPrepNamed (examinedTask, Constants.Preposition.FOR)) {
	  String unit = (prepHelper.hasPrepNamed (task, Constants.Preposition.FOR)) ?
	    (String) prepHelper.getIndirectObject (task, Constants.Preposition.FOR) : "null";
	  String reservedUnit = (String) prepHelper.getIndirectObject (examinedTask, Constants.Preposition.FOR);
	  if (!unit.equals (reservedUnit))
	    return false;
	}

	// are they for the same type of supply?
	Asset taskDO = ((AggregateAsset)task.getDirectObject ()).getAsset();
	Asset examinedDO = ((AggregateAsset)examinedTask.getDirectObject()).getAsset();

	if (!(taskDO.getTypeIdentificationPG().getTypeIdentification().equals (examinedDO.getTypeIdentificationPG().getTypeIdentification()))) 
	  return false;

	// do the dates overlap?
	Date readyAt = prefHelper.getReadyAt (task);
	Date examinedReady = (Date) prepHelper.getIndirectObject (examinedTask, "Start");

	if (examinedReady.getTime () > readyAt.getTime())
	  return false;

	Date best         = prefHelper.getBestDate  (task);
	Date examinedBest = prefHelper.getBestDate  (examinedTask);

	return (best.getTime() <= examinedBest.getTime());
      }
      } 
							  );

    if (matchingTaskCollection.size () > 1)
      error (".handleTask - expecting only one matching task");

    if (matchingTaskCollection.isEmpty () && isWarnEnabled ()) {
      warn (".handleTask - could not find matching task for " + task.getUID());
      return;
    }

    Task reservedTask = (Task) matchingTaskCollection.iterator().next ();

    Date reservedReady = (Date) prepHelper.getIndirectObject (reservedTask, "Start");
    Date readyAt = prefHelper.getReadyAt (task);
    Date best    = prefHelper.getBestDate (task);
    Date reservedBest    = prefHelper.getBestDate (reservedTask);
    long daysLeft    = (reservedBest.getTime()-best.getTime()/MILLIS_PER_DAY);
    long currentDays = (reservedBest.getTime()-reservedReady.getTime()/MILLIS_PER_DAY);
    if (daysLeft < 0) error ("best dates broken");

    //    if (examinedReady.getTime () > readyAt.getTime())
    //      error (".handleTask  - huh? thought examined start " + examinedReady + " was before transport start " + readyAt);

    // if it does, publish remove it and replace it with one with altered date span, quantity, and START prep
    
    AggregateAsset reservedDO = (AggregateAsset)reservedTask.getDirectObject();
    int quantity = (int) (((double)reservedDO.getQuantity ())*((double)daysLeft/(double)currentDays));
    AggregateAsset deliveredAsset = (AggregateAsset) ldmf.createAggregate(reservedDO.getAsset(), quantity);

    NewTask replacement = 
      (NewTask) expandHelper.makeSubTask (ldmf,
					  reservedTask.getPlan(),
					  reservedTask.getParentTaskUID(),
					  reservedTask.getVerb(),
					  reservedTask.getPrepositionalPhrases(),
					  deliveredAsset,
					  reservedTask.getPreferences(),
					  reservedTask.getPriority(),
					  reservedTask.getSource());

    if (isWarnEnabled ())
      warn ("on task " + replacement.getUID() + " replacing asset quantity " +reservedDO.getQuantity () + 
	    " with " + quantity);

    if (isWarnEnabled ())
      warn ("on task " + replacement.getUID() + " replacing start prep date " + reservedReady);

    prepHelper.replacePrepOnTask (replacement, 
				  prepHelper.makePrepositionalPhrase(ldmf,"START",best));
    if (isWarnEnabled ())
      warn ("\twith " + best + " - also becomes early date for task");

    prefHelper.replacePreference (replacement, 
				  prefHelper.makeEndDatePreference(ldmf, 
								   best, 
								   reservedBest, 
								   prefHelper.getLateDate(reservedTask)));
    replaceTaskInWorkflow (reservedTask, replacement);
  }
}
