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
