/***********************************************************************************
 *                                                                                 *
 * This is open-source software developed by the TOPS team of BBN Technologies     *
 * under DARPA's ALPINE program. You can modify the software and redistribute it,  *
 * but you CANNOT DELETE OR MODIFY THIS COMMENT.                                   *
 *                                                                                 *
 * This software is distributed in the hope that it will be useful,                *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                            *
 *                                                                                 *
 * For details on the ALPINE open-source license please write:                     *
 * alpine-support@bbn.com                                                          *
 *                                                                                 *
 * Copyright (C) 1998-2002 BBN Technologies                                        *
 *                                                                                 *
 ***********************************************************************************/

package org.cougaar.logistics.plugin.trans.tools;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.NewGeolocLocation;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationNode;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.asset.TransportationSeaLink;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.domain.RootFactory;

import org.cougaar.glm.callback.GLMLocationListener;
import org.cougaar.glm.callback.GLMLocationCallback;
import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.logistics.plugin.trans.SequentialGlobalAirPlugin;

import org.cougaar.lib.filter.UTILPluginAdapter;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Enumeration; 
import java.util.Collection;
import java.util.Collections;


/**
 * An object for finding the correct POE or POD for a given geoloc
 */
public class LocatorImpl implements Locator {
  private Set myLocations;
  private UTILPluginAdapter myPlugin;
  protected GLMLocationCallback locationCallback = null;
  protected RouteFinder routeFinder;
  protected Logger logger;
  protected GLMMeasure measureHelper;
  protected double MAX_GROUND_DISTANCE;
  private static double EARTH_RADIUS = Distance.newNauticalMiles (3437.75d).getMiles(); // originally nmi

  public void setFactory (RootFactory ldmf) {
    routeFinder = new RouteFinder (logger);
    routeFinder.setFactory (ldmf);

    try {MAX_GROUND_DISTANCE = myPlugin.getMyParams().getDoubleParam("MAX_GROUND_DISTANCE");}
    catch(Exception e) {MAX_GROUND_DISTANCE = 400.0;} 
  }
  
  /*
   * Locator must be instantiated within the setupFilters() method on its
   * parent plugIn.  This is because the LocationCallback has to be 
   * instantiated within the context of an open transaction.  
   */
  public LocatorImpl(SequentialGlobalAirPlugin pi, Logger logger) {
    this.logger = logger;
    myPlugin = pi;
    pi.addFilter(locationCallback = new GLMLocationCallback(this, logger));
    myLocations = new HashSet();

    if (pi.getBlackboard().didRehydrate ())
      handleNewLocations (locationCallback.getSubscription ().elements ());
    measureHelper = new GLMMeasure (logger);
  }

  /*********** Implemented for GLMLocationListener ************************/
  
  // By default is interested in all TransportationNode assets
  // Override to filter for specific kinds of transportation nodes.  
  public boolean interestingLocation(TransportationNode location) {
    return true;
  }

  // Adds new locations to the set of geolocs the locator know about
  public void handleNewLocations(Enumeration newLocations) {
    while (newLocations.hasMoreElements()) {
      TransportationNode locationAsset = (TransportationNode) newLocations.nextElement();
      GeolocLocation geoloc = (GeolocLocation) locationAsset.getPositionPG().getPosition();

      myLocations.add(geoloc);

      // logger.debug("Locator: adding geoloc " + geoloc);
    }
  }

  // Don't worry about changed locations
  public void handleChangedLocations(Enumeration changedLocations) {}


  /*********** Implemented for UTILFilterCallbackListener ************/

  // All listeners must be able to create a subscription
  public IncrementalSubscription subscribeFromCallback(UnaryPredicate pred) {
    return myPlugin.subscribeFromCallback(pred);
  }

  // All listeners must be able to create a subscription with a special container
  public IncrementalSubscription subscribeFromCallback(UnaryPredicate pred,
						       Collection specialContainer) {
    return myPlugin.subscribeFromCallback(pred, specialContainer);
  }


  /*********************** Implemented for Locator ************************/

  /**
   * Returns geoloc with the closest cartesian distance as calculated
   * in an ad hoc way from lat and long.
   */
  public GeolocLocation getNearestLocation(GeolocLocation geoloc) {
    return getNearestLocationExcept (geoloc, Collections.EMPTY_SET);
  }

  /**
   * Returns geoloc with the closest cartesian distance as calculated
   * in an ad hoc way from lat and long.
   */
  public GeolocLocation getNearestLocationExcept (GeolocLocation geoloc, Collection exceptions) {
    GeolocLocation nearestGeoloc = null;
    double shortestDistance = 1000000; // larger than any actual cartesian distance
    Set restrictedLocations = new HashSet(myLocations);
    restrictedLocations.removeAll (exceptions);

    for (Iterator i = restrictedLocations.iterator(); i.hasNext();) {
      GeolocLocation currentGeoloc = (GeolocLocation) i.next();
      double distance = findCartesianDistance(currentGeoloc, geoloc);

      // logger.debug("Trying geoloc " + currentGeoloc + " with distance " + distance);
      if (distance < shortestDistance) {
	// logger.debug("Previous geoloc was the closest match so far");
	shortestDistance = distance;
	nearestGeoloc = currentGeoloc;
      }
    }

    // logger.debug("Location.getNearestLocation() - found location " + nearestGeoloc + " for " + geoloc);
    return nearestGeoloc;
  }

  /**
   * <pre>
   * Returns geoloc with the closest distance as calculated
   * using ship routes and great circle from origin to POE and POD to final destination
   * Looks in an ever-widening circle out from FROM location to find nearest port.
   *
   * For example, looks at all ports within 400 miles, and if can't find any, looks at ones
   * within 800 miles, then 1600 miles, etc.
   * </pre>
   */
  public TransportationRoute getRoute (GeolocLocation fromGeoloc, GeolocLocation toGeoloc, boolean toIsPOD,
				       Collection exceptions) {
    TransportationRoute bestRoute = null;
    double shortestDistance = 1000000; // larger than any actual cartesian distance
    Set restrictedLocations = new HashSet(myLocations);
    restrictedLocations.removeAll (exceptions);
    double maxGround = MAX_GROUND_DISTANCE; 
    if (logger.isDebugEnabled())
      logger.debug ("LocatorImpl.getRoute - max ground dist " + maxGround + 
		    " vs earth radius " + EARTH_RADIUS);

    while (bestRoute == null && (maxGround < EARTH_RADIUS)) {
      for (Iterator i = restrictedLocations.iterator(); i.hasNext();) {
	GeolocLocation POE = (GeolocLocation) i.next();
	double fromToPOE = measureHelper.distanceBetween (fromGeoloc, POE).getMiles();

	if (fromToPOE > maxGround) {
	  if (logger.isDebugEnabled())
	    logger.debug ("LocatorImpl.getRoute - skipping " + POE + " dist > max " + 
				fromToPOE + " > " + maxGround);
	  continue;
	}

	// controls whether to include leg from last node on 
	// sea network to destination node
	TransportationRoute route = routeFinder.getRoute(POE, toGeoloc, toIsPOD);
	GeolocLocation POD = route.getDestination().getGeolocLocation();
	double POEtoPOD  = route.getLength ().getMiles(); 
	double distance  = fromToPOE + POEtoPOD;

	if (logger.isDebugEnabled())
	  logger.debug("LocatorImpl.getRoute - POE " + POE + 
			     " POD " + POD + 
			     " dist " + distance + 
			     " from->POE " + fromToPOE + 
			     " POE->POD " + POEtoPOD);

	if (distance < shortestDistance) {
	  if (logger.isDebugEnabled())
	    logger.debug("LocatorImpl.getRoute - found shorter route from " + POE + " to " + POD);
	  shortestDistance = distance;
	  bestRoute = route;
	}
      }

      if (bestRoute == null) {
	maxGround *= 2; // look in a wider circle every time
	if (logger.isDebugEnabled())
	  logger.debug ("LocatorImpl.getRoute - max ground dist now " + maxGround);
      }
    }

    return bestRoute;
  }

  public int getNumKnownLocations () {	return myLocations.size ();  }

  /**
   * Note that this deals with the lat. and long. coordinates as if they
   * existed in a cartesian coordinate plane.  It doesn't deal with the
   * non-Euclidean aspect of it.  
   */
  private double findCartesianDistance(GeolocLocation geoloc1, GeolocLocation geoloc2) {
    double dlat = geoloc1.getLatitude().getValue(Latitude.DEGREES) - 
      geoloc2.getLatitude().getValue(Latitude.DEGREES);
    double dlong = geoloc1.getLongitude().getValue(Longitude.DEGREES) - 
      geoloc2.getLongitude().getValue(Longitude.DEGREES);

    if (dlong > 180)
      dlong = 360 - dlong;

    return Math.sqrt(dlat * dlat + dlong * dlong);
  }
}
