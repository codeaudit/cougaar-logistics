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

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.lib.vishnu.server.Reusable;
import org.cougaar.lib.vishnu.server.SchObject;
import org.cougaar.util.log.Logger;

import org.w3c.dom.Element;

/**
 * Create either an XML document in the Vishnu Data format or Vishnu objects from ALP objects. <p>
 *
 * Adds fields for physical dimensions to tasks and assets.
 * <p>
 */
public class GenericDataXMLize extends TranscomDataXMLize {
  boolean warnAboutMissingSpeed = 
    System.getProperty ("GenericDataXMLize.warnAboutMissingSpeed", "false").equals("true");

  public GenericDataXMLize (boolean direct, Logger logger) {
    super (direct, logger);
  }

  /** 
   * Create XML for asset, subclass to add fields
   * 
   * @param object node representing asset
   * @param taskOrAsset asset being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processAsset (Object object, Object taskOrAsset) {
    GLMAsset asset = (GLMAsset) taskOrAsset;
    if (!asset.hasContainPG())
      return false;
	
    setType (object, "Asset");
    String type = setName ("Asset", object, asset);

    dataHelper.createRoleScheduleListField(object, "roleSchedule", asset);
    dataHelper.createAvailableScheduleListField(object, "availableSchedule", asset);

    dataHelper.createFloatField(object, "speed",             (float) getSpeed (asset));
    dataHelper.createFloatField(object, "weightCapacity",    (float) getWeightCapacity (asset));
    dataHelper.createFloatField(object, "areaCapacity",      (float) getAreaCapacity   (asset));
    addPassengerCapacity (object, asset);

    if (logger.isDebugEnabled())
      logger.debug ("GenericDataXMLize.processAsset - type " + type);
    if (direct && logger.isDebugEnabled())
      logger.debug ("GenericDataXMLize.processAsset - created resource : " + object);
	
    return true;
  }

  protected void addPassengerCapacity (Object object, GLMAsset asset) {
    dataHelper.createFloatField(object, "passengerCapacity", (float) getPassengerCapacity   (asset));
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
	
    dataHelper.createFloatField(object, "weight",   (float) getWeight   (baseAsset));
    dataHelper.createFloatField(object, "area",     (float) getArea     (baseAsset));
    dataHelper.createFloatField(object, "quantity", (float) getQuantity (directObject));
    dataHelper.createBooleanField(object, "isVehicle", isVehicle (baseAsset));

    if (direct && logger.isDebugEnabled()) {
      logger.debug ("GenericDataXMLize.processTask - created task : " + object);
      Reusable.RInteger departureTime = 
	(Reusable.RInteger) ((SchObject)object).getField ("departure");
      Reusable.RInteger arrivalTime   = 
	(Reusable.RInteger) ((SchObject)object).getField ("arrival");
	  
      logger.debug ("\tdeparture " + timeOps.timeToString (departureTime.intValue()) + 
		    " arrival "    + timeOps.timeToString (arrivalTime.intValue()));
    }

    return true;
  }

  protected double getSpeed (GLMAsset asset) {
    double speed = 55;
	
    try {
      speed = asset.getGroundSelfPropulsionPG().getCruiseSpeed().getMilesPerHour();
    } catch (Exception e) {
      try {
	speed = asset.getAirSelfPropulsionPG().getCruiseSpeed().getMilesPerHour();
      } catch (Exception ee) {
	try {
	  speed = asset.getWaterSelfPropulsionPG().getCruiseSpeed().getMilesPerHour();
	} catch (Exception eee) {
	  if (warnAboutMissingSpeed) {
	    logger.warn ("GenericDataXMLize.getSpeed - WARNING - Could not determine"+ 
			 " resource speed for " + asset.getUID());
	  }
	}
      }
    }
	
    return speed;
  }
  
  protected double getWeightCapacity (GLMAsset asset) {
    double weight = 0;
	
    try {
      weight = asset.getContainPG().getMaximumWeight().getShortTons();
    } catch (Exception e) {}
	
    return weight;
  }
  
  protected double getWeight (GLMAsset asset) {
    double weight = 0;
	
    try {
      weight = asset.getPhysicalPG().getMass().getShortTons();
    } catch (Exception e) {}
	
    return weight;
  }

  protected double getAreaCapacity (GLMAsset asset) {
    double area = 0;
	
    try {
      area = asset.getContainPG().getMaximumFootprintArea().getSquareFeet();
    } catch (Exception e) {}
	
    return area;
  }
  
  protected double getArea (GLMAsset asset) {
    double area = 0;
	
    try {
      area = asset.getPhysicalPG().getFootprintArea().getSquareFeet();
    } catch (Exception e) {}
	
    return area;
  }

  protected double getQuantity (Asset asset) {
    double quantity = 1;
	
    try {
      quantity = ((AggregateAsset)asset).getQuantity();
    } catch (Exception e) {}
	
    return quantity;
  }

  protected double getPassengerCapacity (GLMAsset asset) {
    double pax = 0; 
	
    try {
      pax = asset.getContainPG().getMaximumPassengers();
    } catch (Exception e) {}
	
    return pax;
  }

  /** 
   * <pre>
   * Something is a vehicle if 
   * a) it has a ground vehicle PG OR
   * b) it has a movability PG with a cargo category code whose
   *    first character is either R or A
   *
   * </pre>
   * @return true if asset is a vehicle
   */
  protected boolean isVehicle (GLMAsset asset) {
    if (asset.hasGroundVehiclePG()) return true;

    if (!asset.hasMovabilityPG())
      return false;

    try {
      MovabilityPG move_prop = asset.getMovabilityPG();
      String cargocatcode = move_prop.getCargoCategoryCode();
      char first = cargocatcode.charAt(0);
      if (first == 'R' || first == 'A')
	return true;
    } catch (Exception e) {
      return false;
    } 

    return false;
  }
}

