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

import java.util.Collection;
import java.util.Date;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPreference;
import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.util.UTILPreference;

import org.cougaar.lib.vishnu.client.custom.CustomDataXMLize;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.measure.Distance;

import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipType;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.log.Logger;

import org.w3c.dom.Element;

/**
 * Create either an XML document in the Vishnu Data format or Vishnu objects from ALP objects. <p>
 * <p>
 * Overrides processAsset to exclude extraneous assets and processTask to add fields.
 * <p>
 */
public class TranscomDataXMLize extends CustomDataXMLize {
  public TranscomDataXMLize (boolean direct, Logger logger, String GLOBAL_AIR_ID, String GLOBAL_SEA_ID, String NULL_ASSET_ID) {
    super (direct, logger);
    glmPrepHelper = new GLMPrepPhrase (logger);
    glmPrefHelper = new GLMPreference (logger);
    measureHelper = new GLMMeasure    (logger);

    this.GLOBAL_AIR_ID = GLOBAL_AIR_ID;
    this.GLOBAL_SEA_ID = GLOBAL_SEA_ID;
    this.NULL_ASSET_ID = NULL_ASSET_ID;
  }
  
  public boolean interestingTask(Task t) {
    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);

    return hasTransportVerb;
  }
  
  /** 
   * Create XML for asset, subclass to add fields <p>
   * 
   * Ignore everything but GlobalAir, GlobalSea, and the null asset.
   *
   * Uses statics for asset names from TranscomVishnuPlugin.
   * These are settable by properties.
   *
   * NOTE : field names should match those in .dff file
   *
   * @param object node representing asset
   * @param taskOrAsset asset being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processAsset (Object object, Object taskOrAsset) {
    setType (object, "Asset");
    Asset asset = (Asset) taskOrAsset;
    String type;

    if (asset instanceof Organization) {
      type = getOrganizationRole (asset);
      String name = getAssetName(asset);
      String parentType = "Asset";
	  
      dataHelper.createField(object, parentType, "id", asset.getUID().toString());
      dataHelper.createField(object, parentType, "type", type);
      dataHelper.createField(object, parentType, "name", name);

      if (logger.isDebugEnabled())
	logger.debug ("TranscomDataXMLize.processAsset - asset " + asset.getUID() + 
		     " type " + type + " name " + name);
    }
    else
      type = setName ("Asset", object, asset);
	
    if (direct && logger.isDebugEnabled())
      logger.debug ("TranscomDataXMLize.processAsset - created resource " + object);
	
    // ignore all but global air, sea, and nomenclature
    return (type.startsWith (GLOBAL_AIR_ID) ||
	    type.startsWith (GLOBAL_SEA_ID) ||
	    type.startsWith (NULL_ASSET_ID));
  }

  /** 
   * Get the relationship role of the org, where we're only really interested 
   * in GlobalSea (SeaTransportationProvider) and GlobalAir (AirTransportationProvider)
   */
  protected String getOrganizationRole (Asset asset) {
    Organization org = (Organization) asset;

    Collection providers = 
      org.getRelationshipSchedule().getMatchingRelationships(Constants.RelationshipType.PROVIDER_SUFFIX,
							     TimeSpan.MIN_VALUE,
							     TimeSpan.MAX_VALUE);

    if (providers == null)
      return "NO_PROVIDERS";

    if (providers.size () > 1) {
      if (logger.isDebugEnabled()) 
	logger.debug ("TranscomDataXMLize.getOrganizationRole - NOTE - org " + org + " has multiple providers.\n");
      return "MANY_PROVIDERS";
    }
	  
    if (providers.isEmpty ()) {
      if (logger.isDebugEnabled()) 
	logger.debug ("TranscomDataXMLize.getOrganizationRole - Note - no providers for " + org + ", relationships are " +
		     providers);
      return "NO_PROVIDERS";
    }

    Relationship relationToSuperior = (Relationship) providers.iterator().next();
    Role subRoleA  = relationToSuperior.getRoleA();
    Role subRoleB = relationToSuperior.getRoleB();
    if (logger.isDebugEnabled())
      logger.debug ("TranscomDataXMLize.getOrganizationRole - org " + org + " - roleA " + subRoleA + " roleB " + subRoleB);
    // don't want the converse one - it seems random which org gets to be A and which B
    return (subRoleA.getName().startsWith ("Converse") ? subRoleB.getName () : subRoleA.getName());
  }
  
  /** 
   * Create XML for task, subclass to add fields <p>
   *
   * Adds departure, arrival, from, to, name, type, and isPerson fields.
   * 
   * NOTE : field names should match those in .dff file
   *
   * @param object node representing task
   * @param taskOrAsset task being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processTask (Object object, Object taskOrAsset) {
    super.processTask (object, taskOrAsset);
    Task task = (Task) taskOrAsset;
    String taskName = getTaskName (); 
	
    //	if (logger.isDebugEnabled())
    //	  logger.debug ("TranscomDataXMLize.processTask - uid " + task.getUID());

    dataHelper.createDateField(object, "departure", glmPrefHelper.getReadyAt(task));

    Date arrival = new Date(glmPrefHelper.getBestDate(task).getTime());
    dataHelper.createDateField(object, "arrival", arrival);
    GeolocLocation from = glmPrepHelper.getFromLocation (task);
    GeolocLocation to   = glmPrepHelper.getToLocation (task);

    try {
      if (direct) {
	dataHelper.createGeoloc (object, "from", from);
	dataHelper.createGeoloc (object, "to",   to);
      }
      else {
	Object field = dataHelper.createField (object, taskName, "from");
	dataHelper.createGeoloc (field, "from", from);
	field = dataHelper.createField (object, taskName, "to");
	dataHelper.createGeoloc (field, "to",   to);
      }
    } catch (Exception e) {
      logger.error ("TranscomDataXMLize.createDoc - ERROR - " + 
		    " no from or to geoloc on " + task);
    }

    try {
      dataHelper.createField(object, taskName, "name", getAssetName(task.getDirectObject()));
      dataHelper.createField(object, taskName, "type", getAssetType(task.getDirectObject()));
    } catch (Exception e) {
      logger.error ("TranscomDataXMLize.createDoc - ERROR - " + 
	     " no type id pg on direct object of " + task);
    }

    GLMAsset baseAsset = null;
    Asset directObject = task.getDirectObject ();
	
    if (directObject instanceof AggregateAsset) {
      baseAsset = (GLMAsset) ((AggregateAsset)directObject).getAsset ();
    } 
    else {
      try {
	baseAsset = (GLMAsset)directObject;
      } catch (ClassCastException cce) {
	logger.error ("TranscomDataXMLize.processTask - ERROR for task " + task +
		      "\nDirectObject was not a GLMAsset, as expected.");
      }
    }

    addTaskPersonField (object, baseAsset);

    float distance = (float) 
      ((glmPrepHelper.hasPrepNamed (task, GLMTransConst.SEAROUTE_DISTANCE)) ?
       ((Distance) glmPrepHelper.getIndirectObject (task, 
						    GLMTransConst.SEAROUTE_DISTANCE)).getNauticalMiles() :
       measureHelper.distanceBetween (from, to).getNauticalMiles());

    dataHelper.createFloatField(object, "distance", distance);

    if (logger.isDebugEnabled())
      logger.debug ("TranscomDataXMLize.processTask - did task " + task.getUID());

    if (direct && logger.isDebugEnabled())
      logger.debug ("TranscomDataXMLize.processTask - created task " + object);

    return true;
  }

  /** subclass if people aren't relevant */
  protected void addTaskPersonField (Object object, GLMAsset baseAsset) {
    dataHelper.createBooleanField(object, "isPerson", isPerson (baseAsset));
  }
  
  protected boolean isPerson (GLMAsset asset) {	return asset.hasPersonPG ();  }

  protected String GLOBAL_AIR_ID;
  protected String GLOBAL_SEA_ID;
  protected String NULL_ASSET_ID;

  protected GLMPrepPhrase glmPrepHelper;
  protected GLMPreference glmPrefHelper;
  protected GLMMeasure measureHelper;
}
