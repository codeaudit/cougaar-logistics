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

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;

import org.cougaar.lib.filter.UTILSingleTaskAllocatorPlugin;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.GLMAsset;

import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;

/**
 * <pre>
 * Plugin that looks for TRANSIT or TRANSPORT tasks.
 *
 * Will just allocate the task to whatever asset
 * is hanging off the WITH prep, since this plugin is meant to paired 
 * with and fire after a VishnuAggregatorPlugin.  That plugin 
 * indicates the assignment of task to asset by attaching the WITH prep.
 * 
 * </pre>
 */
public class TransportAllocatorPlugin extends UTILSingleTaskAllocatorPlugin {
  /** 
   * <pre>
   * This plugin is interested in tasks with verb TRANSIT or TRANSPORT 
   * The task must also have the VISHNU prep attached, indicating that the Vishnu
   * plugin upstream has created the task.
   * </pre>
   * @param t task to check to see if it is an input task to this plugin
   */
  public boolean interestingTask(Task t) {
    boolean hasTransitVerb = t.getVerb().equals (Constants.Verb.TRANSIT);
    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);
    boolean hasVishnu = prepHelper.hasPrepNamed (t, "VISHNU");

    if (hasTransitVerb && hasVishnu) {
      if (isDebugEnabled())
	debug (getName () + ".interestingTask - interested in TRANSIT task " + t.getUID ());
      return true;
    }

    if (hasTransportVerb && hasVishnu) {
      if (isDebugEnabled())
	debug (getName () + ".interestingTask - interested in TRANSPORT task " + t.getUID ());
      return true;
    }

    if (isDebugEnabled())
      debug (getName () + ".interestingTask - NOT interested in " + t.getUID ());

    return false;
  }

  /** determines the asset that is allocated to */
  public Asset findAsset(Task t){
    Asset found = getAssetFromTask (t);
    return found;
  }

  /** looks at the task's WITH prep to get the asset -- was attached by the aggregator */
  protected Asset getAssetFromTask (Task combinedTask) {
    Object returnedObj = 
      prepHelper.getIndirectObject(combinedTask, Constants.Preposition.WITH);
	
    return (Asset) returnedObj;
  }
  
  /**
   * Deal with the tasks that we have accumulated.
   * Find the asset that is attached to the task 
   * (in agents with this plugin the vishnu aggregator makes 
   * the task->asset assignment, encoded as a WITH preposition),
   * make an allocation, and publish the allocation.
   *
   * @param List of tasks to handle
   * @see #getAssetFromTask
   */
  public void processTasks (List tasks) {
    for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
      Task t = (Task) iter.next();
      if (t.getPlanElement () == null) {
	Asset a = findAsset(t);
	PlanElement alloc = createAllocation(t, a);
	publishAddingOfAllocation(alloc);
      }
      else {
	Object uid = ((Allocation)t.getPlanElement()).getAsset().getUID();

	// this will happen when we become aware of changed tasks
	if (isInfoEnabled()) {
	  info (getName () + " task " + t.getUID () + " was already allocated to " + uid);
	}

	if (!uid.equals(findAsset(t).getUID ())) {
	  if (isWarnEnabled()) {
	    warn (getName () + " task " + t.getUID () + " was already allocated to " + uid + 
		  " but trying to allocate to different asset " + findAsset(t).getUID());
	  }
	}
      }
    } 

    tasks.clear();
  }

  /** 
   * <pre>
   * Do the actual allocation here
   *
   * If the asset is an organization, allocate with a MEDIUM confidence, otherwise,
   * use a HIGH confidence.
   *
   * Uses two aspects : START and END time.
   *
   * </pre>
   * @param t the task to allocate 
   * @param a the asset to allocate to
   * @return the allocation
   */
  public PlanElement createAllocation(Task t, Asset a){
    Date from = prefHelper.getReadyAt (t);
    Date to   = prefHelper.getBestDate (t);

    double confidence = 
      (((GLMAsset) a).hasOrganizationPG ()) ? allocHelper.MEDIUM_CONFIDENCE : allocHelper.HIGHEST_CONFIDENCE;

    if (isDebugEnabled())
      debug (getName () + ".createAllocation - ready at " + from + 
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
}
