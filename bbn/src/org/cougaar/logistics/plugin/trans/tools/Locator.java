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
import java.util.Set;

import org.cougaar.planning.ldm.PlanningFactory;

import org.cougaar.glm.callback.GLMLocationListener;

import org.cougaar.glm.ldm.asset.TransportationRoute;

import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.planning.ldm.plan.Location;
import org.cougaar.planning.ldm.plan.Task;

/**
 * An object for finding the correct POE or POD for a given geoloc
 */
public interface Locator extends GLMLocationListener {

  /**
   * For a given Geoloc, finds the corresponding locations in 
   * the locator that is closest.  
   */
  GeolocLocation getNearestLocation(GeolocLocation geoloc);
  /** get nearest, but not from among set of exceptions */
  GeolocLocation getNearestLocationExcept(GeolocLocation geoloc, Collection exceptions);
  Location getPOENearestToFromLoc (Task parentTask);
  Location getPOENearestToFromLoc (Task parentTask, Collection exceptions);

  int getNumKnownLocations ();

  void setFactory (PlanningFactory ldmf);

  TransportationRoute getRoute (GeolocLocation fromGeoloc, GeolocLocation toGeoloc, 
				//				boolean fromIsPOE, boolean toIsPOD,
				Collection exceptions);

  boolean isKnownGeolocCode (String geoloc);
  Set knownGeolocCodes ();
  Object getAssetAtGeolocCode (String geoloc);

  Location getLocationForGeolocCode (String geoloc);
  Location getLocationOfAsset (Object asset);
  void addLocationOfAsset (Object asset, Location loc);
}









