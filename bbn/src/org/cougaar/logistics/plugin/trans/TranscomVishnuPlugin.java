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

import org.cougaar.lib.vishnu.client.custom.CustomVishnuAllocatorPlugin;
import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.glm.ldm.asset.Organization;

import org.w3c.dom.Document;

import java.util.Collection;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipType;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.TimeSpan;

public class TranscomVishnuPlugin extends CustomVishnuAllocatorPlugin {
  public void localSetup() {     
    super.localSetup();

    try {
      GLOBAL_AIR_ID = (getMyParams().hasParam ("GlobalAirRole")) ?
	getMyParams().getStringParam("GlobalAirRole") :
	"AirTransportationProvider";

      GLOBAL_SEA_ID = (getMyParams().hasParam ("GlobalSeaRole")) ?
	getMyParams().getStringParam("GlobalSeaRole") :
	"SeaTransportationProvider";

      NULL_ASSET_ID = (getMyParams().hasParam ("NullAssetRole")) ?
	getMyParams().getStringParam("NullAssetRole") :
	"NullNomen";

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
    return hasTransport;
  }

  /**
   * Sort through assets to make sure we have the proper subordinates.
   *
   * @param newAssets new assets found in the container
   */
  public void handleNewAssets(Enumeration newAssets) {
    super.handleNewAssets (newAssets);
    if (isInfoEnabled()) {
      info (".handleNewAssets - got called with " + myNewAssets.size() + " assets.");
    }

    for (Iterator iter = myNewAssets.iterator (); iter.hasNext (); ) {
      String name = "";

      Asset asset = (Asset) iter.next();
      try {
	if (asset instanceof Organization) {
	  name = getOrganizationRole(asset);
	  if (isInfoEnabled()) {
	      info (".handleNewAssets - received subordinate org : " + asset + "'s name is " + name);
	  }
	}
	else {
	  name = asset.getTypeIdentificationPG().getNomenclature();
	  if (isDebugEnabled()) {
	    debug (".handleNewAssets - " + asset + " is NOT an org.");
	  }
	}
      } catch (Exception e) {
	if (isDebugEnabled()) {
	  logger.debug (".handleNewAssets - " + asset + " was strange - ", e);
	}
      }

      if (name != null) {
	if (name.startsWith (GLOBAL_AIR_ID))
	  globalAirReport = true;
	if (name.startsWith (GLOBAL_SEA_ID))
	  globalSeaReport = true;
	if (name.startsWith (NULL_ASSET_ID))
	  nullAssetReport = true;
      }
    }
  }

  /** replicated from TranscomDataXMLize - evil, but just for the moment */
  protected String getOrganizationRole (Asset asset) {
    Organization org = (Organization) asset;

    Collection providers = 
      org.getRelationshipSchedule().getMatchingRelationships(Constants.RelationshipType.PROVIDER_SUFFIX,
							     TimeSpan.MIN_VALUE,
							     TimeSpan.MAX_VALUE);

    if (providers == null)
      return "NO_PROVIDERS";

    if (providers.size () > 1) {
      if (isDebugEnabled()) {
	debug ("TranscomDataXMLize.getOrganizationRole - NOTE - org " + org + " has multiple providers.\n");
      }
      return "MANY_PROVIDERS";
    }
	  
    if (providers.isEmpty ()) {
      if (isDebugEnabled()) {
	debug ("TranscomDataXMLize.getOrganizationRole - Note - no providers for " + org + ", relationships are " +
	      providers);
      }	
      return "NO_PROVIDERS";
    }

    Relationship relationToSuperior = (Relationship) providers.iterator().next();
    Role subRoleA  = relationToSuperior.getRoleA();
    Role subRoleB = relationToSuperior.getRoleB();
    if (isDebugEnabled()) {
      debug ("TranscomDataXMLize.getOrganizationRole - org " + org + " - roleA " + subRoleA + " roleB " + subRoleB);
    }
    // don't want the converse one - it seems random which org gets to be A and which B
    return (subRoleA.getName().startsWith ("Converse") ? subRoleB.getName () : subRoleA.getName());
  }

  /** use the TranscomDataXMLize XMLizer */
  protected XMLizer createXMLizer (boolean direct) {
    return new TranscomDataXMLize (direct, logger, GLOBAL_AIR_ID, GLOBAL_SEA_ID, NULL_ASSET_ID);
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
		  if (!globalAirReport && name.startsWith (GLOBAL_AIR_ID))
		      orgs.add (asset);
		  if (!globalSeaReport && name.startsWith (GLOBAL_SEA_ID))
		      orgs.add (asset);
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
				Date start, Date end, Date setupStart, Date wrapupEnd) {
    Date best = prefHelper.getBestDate(task);
    if (end.getTime() > best.getTime() &&
	end.getTime() < (best.getTime() + 1000l)) {
      end = best;
    }
    super.handleAssignment (task, asset, start, end, setupStart, wrapupEnd);
  }

  protected boolean allNecessaryAssetsReported () {
    return (globalSeaReport && globalAirReport && nullAssetReport);
  }

  protected void reportMissingAssets () {
    if (!globalAirReport)
      error (" - ERROR - missing expected subordinate with type nomen " + GLOBAL_AIR_ID);
    if (!globalSeaReport)
      error (" - ERROR - missing expected subordinate with type nomen " + GLOBAL_SEA_ID);
    if (!nullAssetReport)
      error (" - ERROR - missing expected asset " + NULL_ASSET_ID);
  }

  protected String GLOBAL_AIR_ID;
  protected String GLOBAL_SEA_ID;
  protected String NULL_ASSET_ID;
  
  boolean globalAirReport = false;
  boolean globalSeaReport = false;
  boolean nullAssetReport = false;
}
