/*
 * <copyright>
 *  Copyright 2001-2 BBNT Solutions, LLC
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

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration; 
import java.util.Collection;
import java.util.Collections;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.domain.RootFactory;

import org.cougaar.glm.callback.GLMLocationListener;
import org.cougaar.glm.callback.GLMLocationCallback;

import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationNode;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.asset.TransportationSeaLink;

import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.NewGeolocLocation;

import org.cougaar.logistics.plugin.trans.SequentialGlobalAirPlugin;

import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILPluginAdapter;

import org.cougaar.planning.ldm.asset.Asset;
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
public class LocatorImpl implements Locator {
  public Set myLocations;

  protected Map geolocToAsset = new HashMap ();
  protected Map assetToLocation = new HashMap ();

  private BlackboardPlugin myPlugin;
  protected GLMLocationCallback locationCallback = null;
  protected RouteFinder routeFinder;
  protected Logger logger;
  protected GLMMeasure measureHelper;
  protected double MAX_GROUND_DISTANCE;
  private static double EARTH_RADIUS = Distance.newNauticalMiles (3437.75d).getMiles(); // originally nmi

  public void setFactory (RootFactory ldmf) {
    routeFinder = new RouteFinder (logger);
    routeFinder.setFactory (ldmf);

    try {
      if (myPlugin.getMyParams().hasParam ("MAX_GROUND_DISTANCE"))
	MAX_GROUND_DISTANCE = myPlugin.getMyParams().getDoubleParam("MAX_GROUND_DISTANCE");
      else
	MAX_GROUND_DISTANCE = 400.0; 
    }
    catch(Exception e) {}
  }
  
  /*
   * Locator must be instantiated within the setupFilters() method on its
   * parent plugIn.  This is because the LocationCallback has to be 
   * instantiated within the context of an open transaction.  
   */
  public LocatorImpl(BlackboardPlugin pi, Logger logger) {
    this.logger = logger;
    myPlugin = pi;
    pi.addFilter(locationCallback = new GLMLocationCallback(this, logger));
    myLocations = new HashSet();

    if (pi.getBlackboard().didRehydrate ())
      handleNewLocations (locationCallback.getSubscription ().elements ());
    measureHelper = new GLMMeasure (logger);
    glmPrepHelper = new GLMPrepPhrase (logger);
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
      String geolocCode = geoloc.getGeolocCode();
      geolocToAsset.put (geolocCode, locationAsset);
      assetToLocation.put (locationAsset, geoloc);

      if (logger.isDebugEnabled())
	logger.debug (".handleNewLocations mapping <" + geoloc + "> to <" + locationAsset + ">");

      myLocations.add(geoloc);
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

  public boolean isKnownGeolocCode (String geoloc) {
    return (geolocToAsset.get(geoloc) != null);
  }

  public Set knownGeolocCodes () { return geolocToAsset.keySet(); }
  public Object getAssetAtGeolocCode (String geoloc) { return geolocToAsset.get (geoloc); }

  public Location getLocationForGeolocCode (String geoloc) { 
    Object asset = geolocToAsset.get (geoloc);
    if (asset != null)
      return (Location) assetToLocation.get (asset);
    else
      return null;
  }
  
  public Location getLocationOfAsset (Object asset) { return (Location) assetToLocation.get(asset); }
  public void addLocationOfAsset (Object asset, Location loc) { assetToLocation.put(asset, loc); }

  /** 
   * First check from to see if it's an airport, otherwise, must be a fort 
   * If so, lookup airbase (APOE) nearest fort.
   *
   * @return NULL probably only when no locations have been read in -- an ERROR
   *  otherwise, the nearest location
   **/
  public Location getPOENearestToFromLoc (Task parentTask) {
    String origin = glmPrepHelper.getFromLocation(parentTask).getGeolocCode();
    Object airport = getAssetAtGeolocCode (origin);
    if (airport != null)
      return getLocationOfAsset (airport);

    return getPOENearestToFromLoc (parentTask, Collections.EMPTY_SET);
  }

  public Location getPOENearestToFromLoc (Task parentTask, Collection exceptions) {
    String origin = glmPrepHelper.getFromLocation(parentTask).getGeolocCode();
    //    Object airport = geolocToAirport.get (origin);
    Object airport = getAssetAtGeolocCode (origin);
    if (airport != null)
      //      return (Location) airportToLocation.get (airport);
      return getLocationOfAsset (airport);

    Location poe = getNearestLocationExcept(glmPrepHelper.getFromLocation(parentTask), exceptions);

    if (poe == null)
      logger.error(".getPOENearestToFromLoc - could not find POD for task " + 
		   parentTask.getUID () +
		   " going to " + glmPrepHelper.getFromLocation(parentTask) + ", though I can choose " +
		   "from " + getNumKnownLocations () + " known locations.");
    return poe;
  }
  
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
   * using ship routes 
   *
   * </pre>
   */
  public TransportationRoute getRoute (GeolocLocation fromGeoloc, GeolocLocation toGeoloc, 
				       Collection exceptions) {
    GeolocLocation POE = getNearestLocationExcept (fromGeoloc,  exceptions);
    if (POE == null)
      System.out.println ("Locator chose POE " + POE + " nearest to " + fromGeoloc + " from among " +
			  myLocations + "\nexceptions " + exceptions);
			
    GeolocLocation POD = getNearestLocation (toGeoloc);
    if (POD == null)
      System.out.println ("Locator chose POD " + POD + " nearest to " + toGeoloc + " from among " +
			  myLocations + "\nexceptions " + exceptions);

    TransportationRoute route = routeFinder.getRoute(POE, POD);

    return routeFinder.makeRouteWithPOEandPOD (route, POE, POD);
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

  protected GLMPrepPhrase glmPrepHelper;
}
