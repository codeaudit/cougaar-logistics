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

/**
 * Takes PROJECT_SUPPLY tasks marked with the LOW_FIDELITY prep
 * and converts them into TRANSPORT tasks with a RESERVATION prep. <p>
 *
 * The only real work that goes on is adding a FROM prep of Blue Grass, KY,
 * which is prototypical ammo depot.
 */
public class AmmoProjectionExpanderPlugin extends AmmoLowFidelityExpanderPlugin {
    public static long CHUNK_DAYS = 30;
    public static long MILLIS_PER_DAY = 1000*60*60*24;

  /**
   * State that we are interested in all transport tasks
   * @param task the task to test.
   * @return true if the tasks verb is SUPPLY, false otherwise
   */
  public boolean interestingTask(Task task){
    boolean hasSupply = task.getVerb().equals (Constants.Verb.PROJECTSUPPLY);
    if (!hasSupply)
      return false;

    /*
    if (!prepHelper.hasPrepNamed (task, GLMTransConst.LOW_FIDELITY)) {
      if (isDebugEnabled())
	debug (".interestingTask - ignoring SUPPLY task " + task.getUID () + " that doesn't have a LOW_FIDELITY prep.");
      return false;
    }
    */

    if (isDebugEnabled())
      debug (".interestingTask - processing SUPPLY task " + task.getUID () + " that has a LOW_FIDELITY prep.");

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
    double rate = prefHelper.getPreferenceBestValue (pref);

    Date readyAt = prefHelper.getReadyAt   (parentTask);
    Date early   = prefHelper.getEarlyDate (parentTask);
    Date best    = prefHelper.getBestDate  (parentTask);
    Date late    = prefHelper.getLateDate  (parentTask);

    long window = best.getTime () - readyAt.getTime();
    long numSubtasks = window/(CHUNK_DAYS*MILLIS_PER_DAY);
    if (window - (numSubtasks * CHUNK_DAYS*MILLIS_PER_DAY) != 0)
	numSubtasks++;

    if (isWarnEnabled ())
	warn (getName () + ".getSubtasks - task " + parentTask.getUID () + 
	      " from " + readyAt + 
	      " to " + best + 
	      " will produce " + numSubtasks + " subtasks.");

    // create one subtask for every chunk set of days, with an asset that is the total
    // delivered over the period = days*rate

    for (int i = 0; i < (int) numSubtasks; i++) {
	Asset deliveredAsset = createDeliveredAsset (supplyAsset, rate, CHUNK_DAYS);

	// set item id pg to show it's a reservation, and not a normal task's asset

	ItemIdentificationPG itemIDPG = (ItemIdentificationPG) supplyAsset.getItemIdentificationPG();
	NewItemIdentificationPG newItemIDPG = (NewItemIdentificationPG) PropertyGroupFactory.newItemIdentificationPG();
	newItemIDPG.setItemIdentification (itemIDPG.getItemIdentification() + "_Reservation_" + total);
	newItemIDPG.setNomenclature       (itemIDPG.getNomenclature ()      + "_Reservation_" + (total++));
	deliveredAsset.setItemIdentificationPG (newItemIDPG);
    
	Task subTask = makeTask (parentTask, deliveredAsset);
	long bestTime = early.getTime() + (i*CHUNK_DAYS*MILLIS_PER_DAY);
	if (bestTime > best.getTime())
	    bestTime = best.getTime();

	prefHelper.replacePreference((NewTask)subTask, 
				     prefHelper.makeEndDatePreference (ldmf,
								       early, 
								       new Date (bestTime),
								       late));
	childTasks.addElement (subTask);
    }

    return childTasks;
  }

    /** create aggregate asset aggregating the direct object's prototype **/
    protected Asset createDeliveredAsset (Asset originalAsset, double rate, long chunkDays) {
	
	AggregateAsset deliveredAsset = (AggregateAsset)
	 ldmf.createAggregate(originalAsset.getPrototype (), (int) ((double) chunkDays * rate));

      return deliveredAsset;
    }
}
