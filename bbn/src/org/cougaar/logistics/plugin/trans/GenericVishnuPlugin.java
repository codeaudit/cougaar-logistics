/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.logistics.plugin.trans;

import java.text.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.lib.util.UTILAllocate;
import org.cougaar.lib.util.UTILPreference;

import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.lib.vishnu.client.custom.CustomVishnuAggregatorPlugin;

import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.logistics.ldm.Constants;

import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.plan.GeolocLocation;  
import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPrepPhrase;

public class GenericVishnuPlugin extends CustomVishnuAggregatorPlugin {

  private static final double NM_TO_MILE = 1.15078;
  private static final double MILE_TO_NM = 1.0d/1.15078d;
  private static final NumberFormat format = new DecimalFormat ("##.#");

  public void localSetup() {     
    super.localSetup();

    glmPrepHelper = new GLMPrepPhrase (logger);
    measureHelper = new GLMMeasure    (logger);
  }

  protected XMLizer createXMLizer (boolean direct) {
    GenericDataXMLize xmlizer = new GenericDataXMLize (direct, logger);
    setDataXMLizer(xmlizer);
    return xmlizer;
  }
  
  protected void setDataXMLizer (GenericDataXMLize xmlizer) { dataXMLizer = xmlizer; }

    //  java.util.Set seen = new java.util.HashSet ();
    
  public void processTasks (java.util.List tasks) {
      java.util.List prunedTasks = new java.util.ArrayList(tasks.size());

      Collection removed = myWorkflowCallback.getSubscription().getRemovedCollection();

      for (Iterator iter = tasks.iterator(); iter.hasNext();){
	  Task task = (Task) iter.next();
	  if (removed.contains(task)) {
	      if (isInfoEnabled()) {
		  info ("ignoring task on removed list " + task.getUID());
	      }
	  }
	  else {
	      /*	      if (seen.contains(task.getUID())) {
		  if (isInfoEnabled()) {
		      info ("ignoring already processed " + task.getUID());
		  }
	      }
	      else {
		  seen.add (task.getUID());
	      */
		  prunedTasks.add (task);
		  //	      }
	  }
      }
      super.processTasks (prunedTasks);
  }

  /** prints out date info on failure */
  protected void handleImpossibleTasks (Collection impossibleTasks) {
    if (!foundMaxAssetValues) {
      findMaxAssetValues ();
      foundMaxAssetValues = true;
    }

    if (!impossibleTasks.isEmpty () && isInfoEnabled()) {
      info (getName () + 
	    ".handleImpossibleTasks - failing " + 
	    impossibleTasks.size () + 
	    " tasks.");
    }

    for (Iterator iter = impossibleTasks.iterator (); iter.hasNext ();) {
      Task task = (Task) iter.next ();

      publishAdd (allocHelper.makeFailedDisposition (this, ldmf, task));
      error (getName() + ".handleImpossibleTasks - impossible task : " + task.getUID() +
			  "\nreadyAt " + prefHelper.getReadyAt (task) + 
			  "\nearly   " + prefHelper.getEarlyDate (task) + 
			  "\nbest    " + prefHelper.getBestDate (task) + 
			  "\nlate    " + prefHelper.getLateDate (task));
      
      GeolocLocation from = glmPrepHelper.getFromLocation (task);
      GeolocLocation to   = glmPrepHelper.getToLocation (task);
      float great = (float) measureHelper.distanceBetween (from, to).getNauticalMiles();
      float timeDiff = 
	(float) (prefHelper.getBestDate(task).getTime() - prefHelper.getReadyAt(task).getTime());
      float time = timeDiff/3600000.0f;
      float speed = great/time;

      if (speed > maxSpeed) {
	error (getName () + ".handleImpossibleTasks - impossible task : " + task.getUID() + 
	       "\ndistance from " + from +
	       " to " + to +
	       " is great-circle " + format.format(great) +
	       " nm, (" + format.format(great*NM_TO_MILE) + 
	       " miles) \narrival-departure is " + format.format(time) + " hrs so" +
	       " speed would have to be at least " + format.format(speed) + 
	       " knots (" + format.format(speed*NM_TO_MILE) + " mph).\n" +
	       "This is greater than the speed of the fastest asset : " + format.format(maxSpeed) + 
	       " knots (" + format.format(maxSpeed*NM_TO_MILE) + " mph).\n");
      }
      else {
	if (isWarnEnabled ()) {
	  warn (getName () + ".handleImpossibleTasks - impossible task : " + task.getUID() + 
		"\ndistance from " + from +
		" to " + to +
		" is great-circle " + format.format(great) +
		" nm, (" + format.format(great*NM_TO_MILE) + 
		" miles) \narrival-departure is " + format.format(time) + " hrs so" +
		" speed would have to be at least " + format.format(speed) + 
		" knots (" + format.format(speed*NM_TO_MILE) + " mph).\n" +
		"But this is not greater than the speed of the fastest asset : " + format.format(maxSpeed) + 
		" knots (" + format.format(maxSpeed*NM_TO_MILE) + " mph).  So should be OK.\n");
	}
      }

      Asset directObject = task.getDirectObject();
      GLMAsset baseAsset = null;

      if (directObject instanceof AggregateAsset) {
	baseAsset = (GLMAsset) ((AggregateAsset)directObject).getAsset ();
      } 
      else if (directObject instanceof AssetGroup) {
	error (getName () + ".handleImpossibleTasks - something is really wrong - " + 
	       " input task had a d.o. that was an asset group, task was " + task.getUID());
	return;
      } 
      else {
	baseAsset = (GLMAsset)directObject;
      }

      if (dataXMLizer.getArea (baseAsset) > maxAreaCap) {
	error (getName () + ".handleImpossibleTasks - impossible task : " + task.getUID() + 
	       "\narea of " + format.format(dataXMLizer.getArea (baseAsset)) + " sq ft is > max carrier area " + 
	       format.format(maxAreaCap));
      }

      if (dataXMLizer.getWeight (baseAsset) > maxWeightCap) {
	error (getName () + ".handleImpossibleTasks - impossible task : " + task.getUID() + 
	       "\nweight of " + format.format(dataXMLizer.getWeight (baseAsset)) + " tons is > max carrier weight " + 
	       format.format(maxWeightCap));
      }
    }

    if (stopOnFailure && !impossibleTasks.isEmpty()) {
      if (isWarnEnabled ()) {
	warn (getName() + ".handleImpossibleTasks - stopping on failure!");
      }
      System.exit (-1);
    }
  }

  /** get max dimensions and speed on error so can aid reporting */
  protected void findMaxAssetValues () {
    for (Iterator iter = getAssetCallback().getSubscription ().getCollection().iterator (); 
	 iter.hasNext (); ) {
      Asset asset = (Asset) iter.next();
      if (asset instanceof GLMAsset) {
	GLMAsset glmAsset = (GLMAsset) asset;
	if (glmAsset.hasContainPG ()) {
	  if (dataXMLizer == null)
	    error ("huh? dataxmlizer is null???");
	  double speed = dataXMLizer.getSpeed (glmAsset); // mph

	  double knots = speed * MILE_TO_NM;
	  if (maxSpeed < knots)
	    maxSpeed = knots;

	  double weightCap = dataXMLizer.getWeightCapacity (glmAsset); // tons
	  if (maxWeightCap < weightCap)
	    maxWeightCap = weightCap;

	  double areaCap = dataXMLizer.getAreaCapacity (glmAsset); // square feet
	  if (maxAreaCap < areaCap)
	    maxAreaCap = areaCap;
	}
      }
    }

    if (isDebugEnabled()) {
      debug (getName () + " - max speed " + maxSpeed + " area " + maxAreaCap + " weight " + maxWeightCap);
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

  protected GLMPrepPhrase glmPrepHelper;
  protected GLMMeasure measureHelper;
  protected GenericDataXMLize dataXMLizer;

  protected boolean foundMaxAssetValues = false;

  protected double maxSpeed = 0;
  protected double maxWeightCap = 0;
  protected double maxAreaCap = 0;
}
