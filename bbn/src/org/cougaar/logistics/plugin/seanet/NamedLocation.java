package org.cougaar.logistics.plugin.seanet;

public class NamedLocation extends LocationImpl {

  String name;
  public String getName() { return this.name; }

  public NamedLocation(double latitude, double longitude, String name) {
    super(latitude, longitude);
    this.name = name;
  }

  public String toString() {
    return "{" + name + "," + latitude + "," + longitude + "}";
  }
}
