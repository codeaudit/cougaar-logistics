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









