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
      POEExceptionsNotSunnyPoint = new HashSet(); 
      routeCache = new HashMap ();
    }

    if (POEExceptions.isEmpty ()) {
      Object port = getAssetAtGeolocCode ("WMPT");
      if (port != null)
	POEExceptions.add(getLocationOfAsset(port));
    }

    for (Iterator iter = newPorts.iterator (); iter.hasNext(); ) {
      Asset asset = (Asset) iter.next();
      if (asset.getTypeIdentificationPG().getTypeIdentification ().equals ("TheaterSeaport")) {
	POEExceptions.add (getLocationOfAsset(asset));
	POEExceptionsNotSunnyPoint.add (getLocationOfAsset(asset));
      }
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
	
      //      logger.info ("PortLocatorImpl - got great circle route from " + origin + " to " + destination);
    } else {
      isAmmo = isAmmo ((GLMAsset) asset);
    }

    if (route == null) {
      if (isAmmo) {
	if (logger.isInfoEnabled()) {
	  logger.info ("PortLocatorImpl.getRoute - task " + parentTask.getUID() + 
		       " found ammo so getting route starting at SunnyPoint.");
	}

	Object seaport = getAssetAtGeolocCode ("WMPT");
	if (seaport != null) {
	  origin = (GeolocLocation) getLocationOfAsset (seaport);
	  if (logger.isInfoEnabled()) {
	    logger.info ("PortLocatorImpl.getRoute - task " + parentTask.getUID() +  
			 " getting route starting at " + origin);
	  }
	} else {
	  if (logger.isWarnEnabled()) {
	    logger.warn ("PortLocatorImpl.getRoute - could not find SunnyPoint port among known ports?");
	  }
	}
      }
    
      // determine POE that has the shortest ship route to the POD
      // this may differ a great deal from the simple great-circle distance
      route = getRoute (origin, destination, isAmmo);

      if (!route.getSource ().getGeolocLocation().equals (origin)) {
	if (logger.isInfoEnabled()) {
	  logger.info ("asked for a route from " + origin + " but got one from " + route.getSource() + 
		       " geoloc " + route.getSource().getGeolocLocation());
	}
      }
    }
    
    if (logger.isDebugEnabled()) {
      boolean fromIsPOE = isKnownGeolocCode (origin.getGeolocCode());
      boolean toIsPOD   = isKnownGeolocCode (destination.getGeolocCode());

      logger.debug ("PortLocatorImpl - getting route from " + origin + ((fromIsPOE) ? " POE " : "")+ 
		   " to " + destination + ((toIsPOD) ? " POD " : ""));
      logger.debug ("PortLocatorImpl - route obtained was from " + route.getSource ().getGeolocLocation () +
		   " to " + route.getDestination ().getGeolocLocation ());
    }

    return route;
  }

  /** caches routes found for FROM-TO pairs */
  protected TransportationRoute getRoute (GeolocLocation origin, GeolocLocation destination, 
					  boolean isAmmo) {
    TransportationRoute route = getRouteFromCache (origin, destination);

    if (route == null) {
      route = getRoute (origin, destination, isAmmo ? POEExceptionsNotSunnyPoint : POEExceptions);
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

    if (!route.getSource ().getGeolocLocation().equals (origin)) {
      if (logger.isInfoEnabled()) {
	logger.info ("cacheing route from " + origin + 
		     " that starts from " + route.getSource() + 
		     " geoloc " + route.getSource().getGeolocLocation());
      }
    }

    if (destinationMap == null)
      routeCache.put (origin.getGeolocCode(), (destinationMap = new HashMap()));

    if (logger.isInfoEnabled()) {
      logger.info ("cacheing route from " + origin + 
		   " to " + destination + 
		   " with route from " + route.getSource() + 
		   " to " + route.getDestination ());
    }

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

  /** 
   * An asset is an ammo container if it has a contents pg, since
   * only the Ammo Packer put a contents pg on a container.
   *
   * @return true if asset has a contents PG - i.e. is a milvan
   */
  protected boolean isAmmo (GLMAsset asset) {
    return asset.hasContentsPG ();
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

  protected transient Set POEExceptions; // Locations not to leave from including Sunny Point
  protected transient Set POEExceptionsNotSunnyPoint; // Locations not to leave from, except Sunny Point
  protected transient Map routeCache;

  UTILAllocate allocHelper;
}
