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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPreference;
import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.util.UTILAsset;

import org.cougaar.lib.vishnu.client.custom.CustomDataXMLize;

import org.cougaar.logistics.ldm.Constants;
import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;

import org.cougaar.planning.ldm.measure.Distance;

import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
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
  public static final String TRUE  = "true";
  public static final String FALSE = "false";

  protected Set assetIncludeSet;
  protected UTILAsset assetHelper;
  protected Organization self;

  public TranscomDataXMLize (boolean direct, Logger logger, Set includeSet) {
    super (direct, logger);
    glmPrepHelper = new GLMPrepPhrase (logger);
    glmPrefHelper = new GLMPreference (logger);
    measureHelper = new GLMMeasure    (logger);

    this.assetIncludeSet = includeSet;
    this.assetHelper = new UTILAsset (logger);
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
    return (assetIncludeSet.contains (type));
  }

  /** 
   * Get the relationship role of the org, where we're only really interested 
   * in GlobalSea (SeaTransportationProvider) and GlobalAir (AirTransportationProvider)
   */
  protected String getOrganizationRole (Asset asset) {
    Organization org = (Organization) asset;
    if (org.isSelf())
      self = org;

    Collection providers = 
      org.getRelationshipSchedule().getMatchingRelationships(Constants.RelationshipType.PROVIDER_SUFFIX,
							     TimeSpan.MIN_VALUE,
							     TimeSpan.MAX_VALUE);

    if (providers == null || providers.isEmpty()) {
      if (logger.isInfoEnabled()) {
	logger.info ("TranscomDataXMLize.getOrganizationRole - no providers for " + org + " schedule is " +
		     org.getRelationshipSchedule() +
		     " so getting superiors (which should be a TRANSCOM agent).");
      }

      /*
      if (self != null) {
	RelationshipSchedule schedule = self.getRelationshipSchedule();

	Collection selfProviders = 
	  schedule.getMatchingRelationships(Constants.RelationshipType.PROVIDER_SUFFIX,
					    TimeSpan.MIN_VALUE,
					    TimeSpan.MAX_VALUE);
	if (selfProviders.isEmpty()) {
	  if (logger.isInfoEnabled()) {
	    logger.info ("hmmm, no providers on self schedule, it's " + schedule);
	  }
	}

	for (Iterator iter = selfProviders.iterator (); iter.hasNext(); ) {
	  Relationship relation = (Relationship) iter.next();
	  if (schedule.getOther (relation).equals (org)) {
	    Role subRoleA = relation.getRoleA();
	    Role subRoleB = relation.getRoleB();
	    if (logger.isWarnEnabled()) {
	      logger.warn ("TranscomDataXMLize.getOrganizationRole - returning role for " + org);
	    }
	    return (subRoleA.getName().startsWith ("Converse") ? subRoleB.getName () : subRoleA.getName());
	  }
	  else {
	    if (logger.isInfoEnabled()) {
	      logger.info ("TranscomDataXMLize.getOrganizationRole - skipping provider " + schedule.getOther(relation) +
			   " that is not new org " + org);
	    }
	  }
	}
      }
      else {
	if (logger.isInfoEnabled()) {
	  logger.info ("TranscomDataXMLize.getOrganizationRole - no self org.");
	}
      }
      
      providers = org.getSuperiors(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
      */
    }

    if (providers == null)
      return "NO_PROVIDERS";

    if (providers.size () > 1) {
      if (logger.isInfoEnabled()) {
	logger.info ("TranscomDataXMLize.getOrganizationRole - NOTE - org " + org + 
		     " has multiple providers :");
	if (logger.isInfoEnabled()) {
	  for (Iterator iter = providers.iterator(); iter.hasNext(); ) {
	    logger.info ("\tprovider " + iter.next());
	  }
	}
      }
      return "MANY_PROVIDERS";
    }
	  
    if (providers.isEmpty ()) {
      if (logger.isInfoEnabled()) {
	logger.info ("TranscomDataXMLize.getOrganizationRole - Note - no providers for " + org + 
		     " schedule is " + org.getRelationshipSchedule());
      }

      return "NO_PROVIDERS";
    }

    if (providers.size() == 1)
      if (logger.isInfoEnabled()) 
	logger.info ("found provider for " + org + " schedule was " + org.getRelationshipSchedule());

    Relationship relationToSuperior = (Relationship) providers.iterator().next();
    Role subRoleA = relationToSuperior.getRoleA();
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
	if (task.getDirectObject () instanceof AssetGroup) {
	  if (logger.isInfoEnabled())
	    logger.info("processTasks - got asset group for task " + task.getUID());
	  baseAsset = getBaseAsset ((AssetGroup) task.getDirectObject ());
	}
	else {
	  baseAsset = (GLMAsset)directObject;
	}
      } catch (ClassCastException cce) {
	logger.error ("TranscomDataXMLize.processTask - ERROR for task " + task +
		      "\nDirectObject was not a GLMAsset, as expected.");
      }
    }

    addTaskPersonField (object, baseAsset);
    if (object == null)
      logger.error ("huh? object is null for task " + task.getUID() + " verb " + task.getVerb() + 
		    " is there a field missing for the task in the format file?");
    addIsAmmoField     (object, baseAsset);

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

  /** 
   * try to determine base asset of asset group 
   * Basically, we're trying to determine whether this is ammo or not, or people or not
   * and we need to go down the nested sets of things until we find atomic assets.
   */
  protected GLMAsset getBaseAsset (AssetGroup directObject) {
    Collection assets = assetHelper.expandAssetGroup (directObject);
    GLMAsset base = null;
    for (Iterator iter = assets.iterator (); iter.hasNext () && base == null;) {
      Asset subasset = (Asset) iter.next();
      if (subasset instanceof AssetGroup) {
	base = getBaseAsset ((AssetGroup) subasset); // recurse
      }
      else if (subasset instanceof AggregateAsset) {
	base = (GLMAsset) ((AggregateAsset) subasset).getAsset();
      }
      else if (subasset instanceof GLMAsset) {
	base = (GLMAsset) subasset;
      }
      else
	logger.warn ("huh? unknown asset class " + subasset.getClass () + " for asset " + subasset);
    }

    if (!(base instanceof GLMAsset))
      logger.error ("huh? " + base + " is not a GLMAsset");

    return base;
  }

  /** subclass if people aren't relevant */
  protected void addTaskPersonField (Object object, GLMAsset baseAsset) {
    dataHelper.createBooleanField(object, "isPerson", isPerson (baseAsset));
  }
  
  protected boolean isPerson (GLMAsset asset) {	return asset.hasPersonPG ();  }

  protected void addIsAmmoField (Object object, GLMAsset baseAsset) {
    if (object == null)
      logger.error ("huh? object is null testing " + baseAsset);

    boolean isContainer = isContainer (baseAsset);
    dataHelper.createField(object, "Transport", "isAmmo", 
			   (isContainer ? (isAmmo (baseAsset) ? TRUE : FALSE) : FALSE));
  }

  protected boolean isContainer (GLMAsset asset) {
    LowFidelityAssetPG currentLowFiAssetPG = (LowFidelityAssetPG)
      asset.resolvePG (LowFidelityAssetPG.class);
    
    if (currentLowFiAssetPG != null) {
      return currentLowFiAssetPG.getCCCDim().getIsContainer();
    }
    else {
      return asset instanceof Container;
    }
  }

  /** 
   * An asset is an ammo container if it has a contents pg, since
   * only the Ammo Packer put a contents pg on a container.
   *
   * NOTE : should call isContainer first!
   */
  protected boolean isAmmo (GLMAsset asset) {
    return asset.hasContentsPG ();
  }

  protected GLMPrepPhrase glmPrepHelper;
  protected GLMPreference glmPrefHelper;
  protected GLMMeasure measureHelper;
}
