package org.cougaar.logistics.plugin.seanet;

/** A location on the earth.**/
public interface Location {
  double getLatitude();
  double getLongitude();

  /** Distance in nautical miles from this location to lat, lon. **/
  double distance(double lat, double lon);

  /** Distance in nautical miles from this location to that location. **/
  double distance(Location that);
}
