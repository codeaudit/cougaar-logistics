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
package org.cougaar.logistics.plugin.trans.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration; 
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.PlanningFactory;

import org.cougaar.glm.callback.GLMLocationListener;
import org.cougaar.glm.callback.GLMLocationCallback;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationNode;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.asset.TransportationSeaLink;

import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.NewGeolocLocation;

import org.cougaar.logistics.plugin.trans.SequentialGlobalAirPlugin;

import org.cougaar.glm.util.GLMMeasure;

import org.cougaar.lib.filter.UTILPluginAdapter;
import org.cougaar.lib.util.UTILAllocate;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;

import org.cougaar.planning.ldm.plan.Location;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;

/**
 * An object for finding the correct POE or POD for a given geoloc
 */
public class PortLocatorImpl extends LocatorImpl {
  /*
   * Locator must be instantiated within the setupFilters() method on its
   * parent plugIn.  This is because the LocationCallback has to be 
   * instantiated within the context of an open transaction.  
   */
  public PortLocatorImpl(BlackboardPlugin pi, Logger logger) {
    super (pi, logger);

  }

  /** 
   * A new asset has appeared on the blackboard - if it's the ammo port, add it to list of POEExceptions 
   * Also, if it's a theater port, mark it as an exception (i.e. don't use it to look for POEs).
   */
  public void handleNewLocations(Enumeration newLocations) {
    Vector newPorts = allocHelper.enumToVector (newLocations);
    super.handleNewLocations (newPorts.elements());

    if (POEExceptions == null) {
      allocHelper = new UTILAllocate (logger);
      POEExceptions = new HashSet(); 
      routeCache = new HashMap ();
    }

    if (POEExceptions.isEmpty ()) {
      Object port = getAssetAtGeolocCode ("WMPT");
      if (port != null)
	POEExceptions.add(getLocationOfAsset(port));
    }

    for (Iterator iter = newPorts.iterator (); iter.hasNext(); ) {
      Asset asset = (Asset) iter.next();
      if (asset.getTypeIdentificationPG().getTypeIdentification ().equals ("TheaterSeaport"))
	POEExceptions.add (getLocationOfAsset(asset));
    }
  }

  public boolean isKnownException (Asset seaport) {
    return POEExceptions.contains(getLocationOfAsset (seaport));
  }

  public TransportationRoute getRoute (Task parentTask) {
    GeolocLocation origin      = glmPrepHelper.getFromLocation(parentTask);
    GeolocLocation destination = glmPrepHelper.getToLocation  (parentTask);

    Asset asset = parentTask.getDirectObject();
    boolean isAmmo = false;

    boolean fromIsPOE = isKnownGeolocCode (origin.getGeolocCode());
    boolean toIsPOD   = isKnownGeolocCode (destination.getGeolocCode());

    TransportationRoute route = null;

    if (asset instanceof AssetGroup) {
      Vector assetList = ((AssetGroup)asset).getAssets();
      for (int i = 0; i < assetList.size(); i++) {
	Asset subAsset = (Asset) assetList.elementAt(i);

	if (subAsset instanceof AggregateAsset)
	  subAsset = ((AggregateAsset) subAsset).getAsset ();
	
	if ((subAsset instanceof GLMAsset) && isAmmo ((GLMAsset) subAsset))
	  isAmmo = true;

	break;
      }
    } else if (asset instanceof AggregateAsset) {
      //      route = getGreatCircleRoute(origin, destination); 
      Asset subAsset = ((AggregateAsset) asset).getAsset ();
      
      // if not ammo, don't know what it is -- it must not be ammo
      isAmmo = (subAsset instanceof GLMAsset) ? isAmmo ((GLMAsset) subAsset) : false; 
	
      //      logger.warn ("PortLocatorImpl - got great circle route from " + origin + " to " + destination);
    } else {
      isAmmo = isAmmo ((GLMAsset) asset);
    }

    if (route == null) {
      if (isAmmo) {
	Object seaport = getAssetAtGeolocCode ("WMPT");
	if (seaport != null)
	  origin = (GeolocLocation) getLocationOfAsset (seaport);
      }
    
      // determine POE that has the shortest ship route to the POD
      // this may differ a great deal from the simple great-circle distance
      route = getRoute (origin, destination);//, fromIsPOE, toIsPOD);
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug ("PortLocatorImpl - getting route from " + origin + ((fromIsPOE) ? " POE " : "")+ 
		   " to " + destination + ((toIsPOD) ? " POD " : ""));
      logger.debug ("PortLocatorImpl - route obtained was from " + route.getSource ().getGeolocLocation () +
		   " to " + route.getDestination ().getGeolocLocation ());
    }

    return route;
  }

  /** caches routes found for FROM-TO pairs */
  protected TransportationRoute getRoute (GeolocLocation origin, GeolocLocation destination) {//, 
    //					  boolean fromIsPOE,
    //					  boolean toIsPOD) {
    TransportationRoute route = getRouteFromCache (origin, destination);

    if (route == null) {
      route = getRoute (origin, destination, /*fromIsPOE, toIsPOD,*/ POEExceptions);
      cacheRoute (origin, destination, route);
    }

    return route;
  }

  /*
  protected TransportationRoute getGreatCircleRoute (GeolocLocation origin, GeolocLocation destination) {
    TransportationRoute route = getRouteFromCache (origin, destination);

    if (route == null) {
      route = routeFinder.makeSimpleRoute (origin, destination);
      cacheRoute (origin, destination, route);
    }

    return route;
  }
  */

  protected TransportationRoute getRouteFromCache (GeolocLocation origin, GeolocLocation destination) {
    TransportationRoute route = null;
    Map destinationMap = (Map) routeCache.get (origin.getGeolocCode());

    if (destinationMap != null)
      route = (TransportationRoute) destinationMap.get (destination.getGeolocCode());
    
    return route;
  }

  protected void cacheRoute (GeolocLocation origin, GeolocLocation destination, TransportationRoute route) {
    Map destinationMap = (Map) routeCache.get (origin.getGeolocCode());
    if (destinationMap == null)
      routeCache.put (origin.getGeolocCode(), (destinationMap = new HashMap()));
    destinationMap.put (destination.getGeolocCode(), route);
  }

  public Location getPortNearestToFromLoc (Task parentTask) {
    if (!isAmmo((GLMAsset) parentTask.getDirectObject ())) {
      return getPOENearestToFromLoc (parentTask, POEExceptions);
    }

    //    Object seaport = geolocToAirport.get ("WMPT");
    Object seaport = getAssetAtGeolocCode ("WMPT");
    if (seaport != null) {
      return getLocationOfAsset (seaport);
    }
    else {
      logger.error (".getPOENearestToFromLoc - " + 
		    " could not find sunny point port, using nearest port instead.");
      return getPOENearestToFromLoc (parentTask);
    }
  }

  protected boolean isContainer (GLMAsset asset) { return asset instanceof Container;  }

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

  protected String getAssetType (Asset asset) {
    String name = "";

    try {
      name = asset.getTypeIdentificationPG().getNomenclature();
    } catch (Exception e) {
      logger.error (".getAssetType - ERROR - no type id pg on " + asset);
    }

    if (name == null) {
      try {
	name = asset.getTypeIdentificationPG().getTypeIdentification();
      } catch (Exception e) {
	logger.error (".getAssetType - ERROR - no type id pg on " + asset);
      }
    }
    if (name == null) name = "";
    return name;
  }

  protected transient Set POEExceptions; // Locations
  protected transient Map routeCache;

  UTILAllocate allocHelper;
}
