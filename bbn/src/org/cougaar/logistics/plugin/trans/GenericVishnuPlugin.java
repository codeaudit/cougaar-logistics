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

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.lib.vishnu.client.custom.CustomVishnuAggregatorPlugin;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.GLMAsset;

import org.cougaar.lib.util.UTILAllocate;
import org.cougaar.lib.util.UTILPreference;

public class GenericVishnuPlugin extends CustomVishnuAggregatorPlugin {
  protected XMLizer createXMLizer (boolean direct) {
    return new GenericDataXMLize (direct, logger);
  }

  /** prints out date info on failure */
  protected void handleImpossibleTasks (Collection impossibleTasks) {
    if (!impossibleTasks.isEmpty ())
      info (getName () + 
			  ".handleImpossibleTasks - failing " + 
			  impossibleTasks.size () + 
			  " tasks.");

    for (Iterator iter = impossibleTasks.iterator (); iter.hasNext ();) {
      Task task = (Task) iter.next ();

      publishAdd (allocHelper.makeFailedDisposition (this, ldmf, task));
      error (getName() + ".handleImpossibleTasks - impossible task : " + task.getUID() +
			  " readyAt " + prefHelper.getReadyAt (task) + 
			  " early " + prefHelper.getEarlyDate (task) + 
			  " best " + prefHelper.getBestDate (task) + 
			  " late " + prefHelper.getLateDate (task));
    }

    if (stopOnFailure && !impossibleTasks.isEmpty()) {
      info (getName() + ".handleImpossibleTasks - stopping on failure!");
      System.exit (-1);
    }
  }

  /**
   * Implemented for UTILAssetListener
   * <p>
   * OVERRIDE to see which assets you think are interesting.
   * <p>
   * For instance, if you are scheduling trucks/ships/planes, 
   * you'd want to check like this : 
   * <code>
   * return (GLMAsset).hasContainPG ();
   * </code>
   * @param a asset to check 
   * @return boolean true if asset is interesting
   */
  public boolean interestingAsset(Asset a) {
    if (a instanceof GLMAsset) {
      return ((GLMAsset) a).hasContainPG();
    }
    return false;
  }

  public boolean interestingTask(Task t) {
    if (t.getVerb().equals(Constants.Verb.TRANSPORT)) {
      if (t.getDirectObject() instanceof AbstractAsset) {
        error("Whoops, apparently interesting AbstractAsset: " + t);
        return false;
      }
      return super.interestingTask(t);
    }
    return false;
  }
}
