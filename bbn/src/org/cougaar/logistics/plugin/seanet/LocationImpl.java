/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.seanet;

import org.cougaar.logistics.plugin.seanet.Location;

/** The obvious Location implemntation. **/
public class LocationImpl implements Location {
  public LocationImpl(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }
  
  protected double latitude;
  public double getLatitude() { return latitude; }

  protected double longitude;
  public double getLongitude() { return longitude; }
  
  public double distance(double lat, double lon) {
    return (new GreatCircle(this.getLatitude(), this.getLongitude()))
      .distanceNM(lat, lon);
  }

  public double distance(Location L) {
    return distance(L.getLatitude(), L.getLongitude());
  }

  public String toString() { return "{" + latitude + " " + longitude + "}"; }

}
