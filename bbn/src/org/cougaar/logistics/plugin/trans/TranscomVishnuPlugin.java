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

import java.util.*;

import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;
import org.cougaar.lib.callback.UTILWorkflowCallback;

import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.lib.vishnu.client.custom.CustomVishnuAllocatorPlugin;

import org.cougaar.logistics.ldm.Constants;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipType;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import org.w3c.dom.Document;

public class TranscomVishnuPlugin extends CustomVishnuAllocatorPlugin {
  protected Set reportedIDs = new HashSet ();
  protected Set expectedIDs = new HashSet();
  protected TranscomDataXMLize transcomDataXMLizer;
  protected List delayedTasks = new ArrayList ();
  protected long waitTime = 10000; // millis

  public void localSetup() {     
    super.localSetup();

    addExpectedProviders();

    createXMLizer (getRunDirectly()); // we need one of these for getOrganizationRole
  }

  protected void addExpectedProviders() {
    try {
      boolean needsAir = !getMyParams().hasParam ("doesNotNeedAirTransportProvider");
      String GLOBAL_AIR_ID = (getMyParams().hasParam ("GlobalAirRole")) ?
	getMyParams().getStringParam("GlobalAirRole") :
	"AirTransportationProvider";

      String GLOBAL_SEA_ID = (getMyParams().hasParam ("GlobalSeaRole")) ?
	getMyParams().getStringParam("GlobalSeaRole") :
	"SeaTransportationProvider";

      String NULL_ASSET_ID = (getMyParams().hasParam ("NullAssetRole")) ?
	getMyParams().getStringParam("NullAssetRole") :
	"NullNomen";
      
      if (needsAir) 
	expectedIDs.add (GLOBAL_AIR_ID);

      expectedIDs.add (GLOBAL_SEA_ID);
      expectedIDs.add (NULL_ASSET_ID);
    } catch(Exception e) {
      error ("got really unexpected exception " + e);
    } 
  }

  /**
   * Is the task interesting to the plugin?  This is the inner-most part of 
   * the predicate.                                                         <br>
   * By default, it ignores tasks produced from this plugin                 <br>                    
   * If you redefine this, it's good to call this using super.
   *
   * @param t - the task begin checked
   * @see org.cougaar.lib.callback.UTILGenericListener#interestingTask
   */
  public boolean interestingTask (Task t) { 
    if (!super.interestingTask (t))
      return false;
    boolean hasTransport = t.getVerb().equals (Constants.Verb.TRANSPORT);

    if (logger.isDebugEnabled())
      logger.debug ("found " + t.getUID() + (hasTransport ? " interesting" : " uninteresting"));

    return hasTransport;
  }

  /**
   * Sort through assets to make sure we have the proper subordinates.
   *
   * @param newAssets new assets found in the container
   */
  public void handleChangedAssets(Enumeration newAssets) {
    handleNewAssets (newAssets);
  }

  public void handleNewAssets(Enumeration newAssets) {
    super.handleNewAssets (newAssets);
    if (isInfoEnabled()) {
      info ("handleNewAssets - got called with " + myNewAssets.size() + " assets.");
    }

    for (Iterator iter = myNewAssets.iterator (); iter.hasNext (); ) {
      String name = "";

      Asset asset = (Asset) iter.next();
      boolean isOrg = false;
      try {
	if (asset instanceof Organization) {
	  name = getOrganizationRole(asset);
	  if (isInfoEnabled()) {
	    info ("handleNewAssets - received subordinate org : " + asset + "'s name is " + name);
	  }
	}
	else {
	  name = asset.getTypeIdentificationPG().getNomenclature();
	  if (isDebugEnabled()) {
	    debug ("handleNewAssets - " + asset + " is NOT an org.");
	  }
	}
      } catch (Exception e) {
	if (isWarnEnabled()) {
	  logger.warn ("handleNewAssets - " + asset + " was strange - ", e);
	}
      }

      if (name != null) {
	if (expectedIDs.contains (name)) {
	  if (!reportedIDs.contains(name)) {
	    if (logger.isInfoEnabled())
	      logger.info (getName() + " - expected "+  name + " has reported.");
	    reportedIDs.add (name);
	  }
	}
	else {
	  if (logger.isInfoEnabled())
	    logger.info ("ignoring id "+  name);
	}
      }
    }
  }

  /** calls TranscomDataXMLize */
  protected String getOrganizationRole (Asset asset) {
    return transcomDataXMLizer.getOrganizationRole (asset);
  }

  /** use the TranscomDataXMLize XMLizer */
  protected XMLizer createXMLizer (boolean direct) {
    return (transcomDataXMLizer = new TranscomDataXMLize (direct, logger, expectedIDs));
  }

  protected Collection getAllAssets() {
    if (!allNecessaryAssetsReported()) {
      if (isWarnEnabled()) {
	warn ("Trying to find subordinates, despite not seeing the following in added lists...");
	      
	reportMissingAssets();
      }
	  
      // send subscription contents through again...
      Collection assets = getAssetCallback().getSubscription ().getCollection();
      List orgs = new ArrayList ();

      for (Iterator iter = assets.iterator(); iter.hasNext(); ) {
	Asset asset = (Asset) iter.next();
	if (asset instanceof Organization) {
	  String name = getOrganizationRole(asset);
	  if (expectedIDs.contains(name) && !reportedIDs.contains(name)) {
	    orgs.add (asset);
	  }
	}
      }
	      
      if (!orgs.isEmpty()) {
	// send them through again as though they appeared on the added list
	if (isWarnEnabled()) {
	  warn ("Found the missing subord(s) : " + orgs);
	}

	handleNewAssets(Collections.enumeration(orgs));
      }
    }

    return super.getAllAssets();
  }

  /**
   * Overridden to provide check for missing assets.  Calls super first.
   *
   * @param stuffToSend - initially the list of tasks to send to scheduler
   * @param objectFormatDoc - optional object format used by data xmlizers
   *  to determine types for fields when running directly
   */
  protected void prepareData (List stuffToSend, Document objectFormatDoc) {
    super.prepareData (stuffToSend, objectFormatDoc);

    // localDidRehydrate - handleNewAssets is not called if we were rehydrated, 
    // so we would otherwise report spurious error.
    if (!localDidRehydrate && !allNecessaryAssetsReported())
      reportMissingAssets ();
  }
  
  public void handleAssignment (org.cougaar.planning.ldm.plan.Task task, Asset asset, 
				Date start, Date end, Date setupStart, Date wrapupEnd, String contribs, String taskText) {
    Date best = prefHelper.getBestDate(task);
    if (end.getTime() > best.getTime() &&
	end.getTime() < (best.getTime() + 1000l)) {
      end = best;
    }
    super.handleAssignment (task, asset, start, end, setupStart, wrapupEnd, contribs, taskText);
  }

  protected boolean allNecessaryAssetsReported () {
    return (reportedIDs.size() == expectedIDs.size());
  }

  protected void reportMissingAssets () {
    for (Iterator iter = expectedIDs.iterator(); iter.hasNext(); ) {
      Object key = iter.next();
      if (!reportedIDs.contains (key)) {
	error (" - ERROR - missing expected asset with role " + key);
      }
    }
  }

  /** so we can deal with race condition between tasks showing up and subordinates showing up */
  public void processTasks (List tasks) {
    if (!allNecessaryAssetsReported()) { // if need subordinates aren't there yet, way 10 seconds
      if (logger.isInfoEnabled())
	logger.info (getName() + " - necessary subords have not reported, so waiting " + waitTime);

      delayedTasks.addAll (tasks);
      examineBufferAgainIn (waitTime); // wait 10 seconds and check again
    }
    else { // ok, all subords are here, lets go!
      if (logger.isInfoEnabled())
	logger.info (getName() + " - all necessary subords have reported, so processing " + tasks.size() + " tasks.");

      tasks.addAll (delayedTasks);
      delayedTasks.clear();
      super.processTasks (tasks);
    }
  }
}
