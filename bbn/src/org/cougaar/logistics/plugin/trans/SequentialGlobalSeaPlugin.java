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

import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Location;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.logistics.plugin.trans.GLMTransConst;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.cougaar.logistics.plugin.trans.tools.LocatorImpl;

/**
 * <pre>
 * Specific logic for dealing with backwards sea planning.
 *
 * Mainly involves worrying about POE and POD choice.
 * </pre>
 */
public class SequentialGlobalSeaPlugin extends SequentialGlobalAirPlugin {
  protected transient Set exceptions = new HashSet();
  boolean useSeaRoutes = false;
  protected transient Map routeCache = new HashMap ();

  public void localSetup() {     
    super.localSetup();

    try {useSeaRoutes = getMyParams().getBooleanParam("useSeaRoutes");}
    catch(Exception e) {useSeaRoutes = true;} // this is really expensive computationally

    if (useSeaRoutes)
      aPOELocator.setFactory (ldmf); // tell route finder the ldm factory to use
  }

  public Organization findOrgForMiddleStep () {
    Organization org = findOrgWithRole(GLMTransConst.SHIP_PACKER_ROLE);
    if (org == null) {
      error(".MiddlePortion.planMe - ERROR - No subordinate with role " + 
	    GLMTransConst.SHIP_PACKER_ROLE);
      return null;
    }
    return org;
  }

  /** 
   * A new asset has appeared on the blackboard - if it's the ammo port, add it to list of exceptions 
   * Also, if it's a theater port, mark it as an exception (i.e. don't use it to look for POEs).
   */
  public void handleNewAssets (Enumeration org_assets) {
    Vector newPorts = allocHelper.enumToVector (org_assets);
    super.handleNewAssets (newPorts.elements());
    if (exceptions.isEmpty ()) {
      Object port = geolocToAirport.get ("WMPT");
      if (port != null)
	exceptions.add(airportToLocation.get(port));
    }

    for (Iterator iter = newPorts.iterator (); iter.hasNext(); ) {
      Asset asset = (Asset) iter.next();
      if (asset.getTypeIdentificationPG().getTypeIdentification ().equals ("TheaterSeaport"))
	exceptions.add (airportToLocation.get(asset));
    }
  }

  /** don't include destination ports as POEs */
  boolean startsAtPOE (Task task) {
    String origin = outerGLMPrepHelper.getFromLocation(task).getGeolocCode();
    Object airport = geolocToAirport.get (origin);
    
    return (airport != null) && !exceptions.contains (airportToLocation.get(airport));
  }

  /** 
   * <pre>
   * Given a task, find the POE and POD for the task 
   * This will be a search among possible POEs and PODs for those that
   * are closest to the FROM-TO pair on the parent task.
   *
   * The choice will be affected by the how long it takes
   * to get from POE to POD.
   * </pre>
   */
  protected Location [] getPOEandPOD (Task parentTask, Task subtask) {
    Location [] locs = new Location[2];
    
    if (useSeaRoutes) {
      GeolocLocation origin      = outerGLMPrepHelper.getFromLocation(parentTask);
      GeolocLocation destination = outerGLMPrepHelper.getToLocation  (parentTask);

      // if ammo, we have only one possible POE port, for now
      if (isAmmo((GLMAsset) parentTask.getDirectObject ())) {
	Object seaport = geolocToAirport.get ("WMPT");
	if (seaport != null)
	  origin = (GeolocLocation) airportToLocation.get (seaport);
      }
	
      // determine POE that has the shortest ship route to the POD
      // this may differ a great deal from the simple great-circle distance
      boolean toIsPOD = (geolocToAirport.get (destination.getGeolocCode()) != null);
      TransportationRoute route = getRoute (origin, destination, toIsPOD);

      locs[0] = route.getSource().getGeolocLocation();
      locs[1] = aPOELocator.getNearestLocation(route.getDestination().getGeolocLocation());
      Distance distance =  route.getLength();
      outerGLMPrepHelper.addPrepToTask (subtask, 
					outerGLMPrepHelper.makePrepositionalPhrase (ldmf,
										    GLMTransConst.SEAROUTE_DISTANCE,
										    distance));
    } else { // just use great circle calcs
      locs[0] = getPOENearestToFromLocMiddleStep (parentTask);
      locs[1] = getPOD(parentTask);
    }

    return locs;
  }

  /** caches routes found for FROM-TO pairs */
  protected TransportationRoute getRoute (GeolocLocation origin, GeolocLocation destination, boolean toIsPOD) {
    TransportationRoute route = null;
    Map destinationMap = (Map) routeCache.get (origin.getGeolocCode());

    if (destinationMap != null)
      route = (TransportationRoute) destinationMap.get (destination.getGeolocCode());
    else 
      routeCache.put (origin.getGeolocCode(), (destinationMap = new HashMap()));

    if (route == null) {
      route = aPOELocator.getRoute (origin, destination, toIsPOD, exceptions);
      destinationMap.put (destination.getGeolocCode(), route);
    }

    return route;
  }

  Location getPOENearestToFromLocMiddleStep (Task parentTask) {
    if (!isAmmo((GLMAsset) parentTask.getDirectObject ())) {
      return getPOENearestToFromLoc (parentTask, exceptions);
    }

    Object seaport = geolocToAirport.get ("WMPT");
    if (seaport != null) {
      return (Location) airportToLocation.get (seaport);
    }
    else {
      error (".getPOENearestToFromLocMiddleStep - " + 
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
      error ("CustomDataXMLize.createDoc - ERROR - no type id pg on " + asset);
    }

    if (name == null) {
      try {
	name = asset.getTypeIdentificationPG().getTypeIdentification();
      } catch (Exception e) {
	error ("CustomDataXMLize.createDoc - ERROR - no type id pg on " + asset);
      }
    }
    if (name == null) name = "";
    return name;
  }
}






