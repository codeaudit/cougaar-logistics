package org.cougaar.logistics.plugin.seanet;

/** A location on the earth.**/
public interface Location {
  public double getLatitude();
  public double getLongitude();

  /** Distance in nautical miles from this location to lat, lon. **/
  public double distance(double lat, double lon);

  /** Distance in nautical miles from this location to that location. **/
  public double distance(Location that);
}
