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

import java.util.Date;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.GLMAsset;

import org.cougaar.lib.util.UTILPreference;
import org.cougaar.lib.util.UTILPrepPhrase;

import org.cougaar.logistics.plugin.trans.GLMTransConst;
import org.cougaar.util.log.Logger;

/**
 * Create XML document in the Vishnu Data format, directly from ALP objects.
 * <p>
 * Create and return xml for log plan objects.
 * <p>
 */

public class SeaDataXMLize extends GenericDataXMLize {
  public static final String TRUE  = "true";
  public static final String FALSE = "false";

  public SeaDataXMLize (boolean direct, Logger logger) {
    super (direct, logger);
    prefHelper = new UTILPreference (logger);
    prepHelper = new UTILPrepPhrase (logger);
  }

  /** 
   * Create XML for asset, subclass to add fields
   * 
   * @param object node representing asset
   * @param taskOrAsset asset being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processAsset (Object object, Object taskOrAsset) {
    if (!super.processAsset(object, taskOrAsset))
      return false;

    GLMAsset asset = (GLMAsset) taskOrAsset;
	
    dataHelper.createField(object, "Asset", "containerCapacity", "" + getContainerCapacity   (asset));
    dataHelper.createField(object, "Asset", "isAmmoShip", isAmmoShip (asset) ? TRUE : FALSE);

    return true;
  }

  /** 
   * Create XML for task, subclass to add fields
   * 
   * @param object node representing task
   * @param taskOrAsset task being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processTask (Object object, Object taskOrAsset) {
    super.processTask (object, taskOrAsset);
    Task task = (Task) taskOrAsset;
    Asset directObject = task.getDirectObject();
    GLMAsset baseAsset;

    if (directObject instanceof AggregateAsset) {
      baseAsset = (GLMAsset) ((AggregateAsset)directObject).getAsset ();
    } 
    else {
      baseAsset = (GLMAsset)directObject;
    }
	
    dataHelper.createField(object, "Transport", "isContainer", isContainer (baseAsset) ? TRUE : FALSE);
    dataHelper.createField(object, "Transport", "isAmmo", isAmmo (baseAsset) ? TRUE : FALSE);

    Date earliestArrival = new Date(prefHelper.getEarlyDate(task).getTime());
    dataHelper.createDateField(object, "earliestArrival", earliestArrival);
    Date latestArrival = new Date(prefHelper.getLateDate(task).getTime());
    dataHelper.createDateField(object, "latestArrival", latestArrival);

    float distance = 
      (float) ((Distance) prepHelper.getIndirectObject (task, 
							GLMTransConst.SEAROUTE_DISTANCE)).getNauticalMiles();
    dataHelper.createFloatField(object, "distance", distance);

    return true;
  }

  /** don't send the isPerson field */
  protected void addTaskPersonField (Object object, GLMAsset baseAsset) {}
  /** don't send the passengerCapacity field */
  protected void addPassengerCapacity (Object object, GLMAsset asset) {}

  protected boolean isContainer (GLMAsset asset) {
    return asset instanceof Container;
  }

  /** 
   * this is a hack -- we tell if a container is an ammo container if it comes 
   * from the ammo packer.  There should be a better way to tell that it's a 
   * container full of ammo.
   */
  protected boolean isAmmo (GLMAsset asset) {
    boolean isContainer = isContainer(asset);
    if (!isContainer)
      return false;

    String unit = "";
    try{
      unit = asset.getForUnitPG ().getUnit ();
    } catch (Exception e) {
      return false;
    }
	
    return unit.equals ("IOC") || unit.equals ("OSC") || getAssetType(asset).equals ("20FT_AMMO_CONTAINER");
  }

  protected double getContainerCapacity (GLMAsset asset) {
    double weight = 0;
	
    try {
      weight = asset.getContainPG().getMaximumContainers();
    } catch (Exception e) {}
	
    return weight;
  }

  protected boolean isAmmoShip (GLMAsset asset) {
    String typeid = asset.getTypeIdentificationPG().getNomenclature ();
    return typeid.equals ("Ammo");
  }

  protected UTILPreference prefHelper;
  protected UTILPrepPhrase prepHelper;
}
