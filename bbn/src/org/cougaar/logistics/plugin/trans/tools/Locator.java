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

import org.cougaar.core.domain.RootFactory;
import org.cougaar.glm.callback.GLMLocationListener;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import java.util.Collection;

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

  int getNumKnownLocations ();

  void setFactory (RootFactory ldmf);

  TransportationRoute getRoute (GeolocLocation fromGeoloc, GeolocLocation toGeoloc, boolean toIsPOD,
				Collection exceptions);
}









