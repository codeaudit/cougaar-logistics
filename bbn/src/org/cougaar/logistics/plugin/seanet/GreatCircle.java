package org.cougaar.logistics.plugin.seanet;

/**
 * A stateful great circle calculator.
 <p>The theory is presented in
 <a href="http://stout.bbn.com/~kanderso/java/course/doc/episode-10/index.html">Partial Evaluation - Freeing the essence of a computation</a>.
 <p>Two lat,lon points are kept in a 3D unit vector representation.
 <p> Once the two points are set, various distance*() and azimuth() function
 may be called.
 <p>The method position() computes the position of a fractional distance along the great circle between the points.
 */
public class GreatCircle {

  private double a, b, c, d, e, f;
  private double posLatitude, posLongitude;
	
  public GreatCircle() { }
  public GreatCircle(double lat, double lon) {
    this.setPoint1(lat, lon);
  }

  /** Set the first point.**/
  public void setPoint1(double lat, double lon) {
    // Planet2.earth.toV(u("lat"), u("lon"))
    double ph0 = (0.017453292519943295 * lon);
    double ph1 = (0.017453292519943295 * lat);
    double ph2 = Math.tan(ph1);
    double ph3 = (0.9933056200098587 * ph2);
    double ph4 = Math.atan(ph3);
    double ph5 = Math.cos(ph4);
    double ph6 = Math.cos(ph0);
    double ph7 = (ph5 * ph6);
    double ph8 = Math.sin(ph0);
    double ph9 = (ph5 * ph8);
    double ph10 = Math.sin(ph4);
    // return new V(ph7, ph9, ph10);
    a = ph7;
    b = ph9; 
    c = ph10; 
  }
	
  /** Set the second point.**/
  public void setPoint2(double lat, double lon) {
    // Planet2.earth.toV(u("lat"), u("lon"))
    double ph0 = (0.017453292519943295 * lon);
    double ph1 = (0.017453292519943295 * lat);
    double ph2 = Math.tan(ph1);
    double ph3 = (0.9933056200098587 * ph2);
    double ph4 = Math.atan(ph3);
    double ph5 = Math.cos(ph4);
    double ph6 = Math.cos(ph0);
    double ph7 = (ph5 * ph6);
    double ph8 = Math.sin(ph0);
    double ph9 = (ph5 * ph8);
    double ph10 = Math.sin(ph4);
    // return new V(ph7, ph9, ph10);
    d = ph7;
    e = ph9;
    f = ph10; }
  
  /** Return the latitude of the current position. **/
  public double getPositionLatitude() { return this.posLatitude; }
  /** Return the longiute of the current position. **/
  public double getPositionLongitude() { return this.posLongitude; }

  /** Compute distance in nautical miles. **/
  public double distanceNM(double lat1, double lon1,
			   double lat2, double lon2) {
    this.setPoint1(lat1, lon1);
    return distanceNM(lat2, lon2);
  }

  /** Distance in nautical miles from the first point and this location. **/
  public double distanceNM(double lat, double lon) {
    this.setPoint2(lat, lon);
    return this.distanceNM();
  }

  /** Distance in nautical miles between the two points. **/
  public double distanceNM() {
    // http://forum.swarthmore.edu/pow/solutio69.html
    return distanceKM()/1.852;
  }

  /** Compute distance in kilometers. **/
  public double distanceKM(double lat, double lon) {
    this.setPoint2(lat, lon);
    return this.distanceKM();
  }

  /** Distance in kilometers between the two points. **/
  public double distanceKM() {
    // times(Planet2.earth.radius(), v1.distance(v2))
    double ph0 = (e * c);
    double ph1 = (f * b);
    double ph2 = (ph0 - ph1);
    double ph3 = (f * a);
    double ph4 = (d * c);
    double ph5 = (ph3 - ph4);
    double ph6 = (d * b);
    double ph7 = (e * a);
    double ph8 = (ph6 - ph7);
    double ph9 = (ph2 * ph2);
    double ph10 = (ph5 * ph5);
    double ph11 = (ph8 * ph8);
    double ph12 = (ph10 + ph11);
    double ph13 = (ph9 + ph12);
    double ph14 = Math.sqrt(ph13);
    double ph15 = (d * a);
    double ph16 = (e * b);
    double ph17 = (f * c);
    double ph18 = (ph16 + ph17);
    double ph19 = (ph15 + ph18);
    double ph20 = Math.atan2(ph14, ph19);
    double ph21 = (6378.137 * ph20);
    return ph21; }

  /** Returns the azimuth from point1 to point2 in degees. **/
  public double azimuth() {
    //Planet2.degrees(v1.azimuth(v2))

    double ph0 = (- b);
    double ph1 = (ph0 * ph0);
    double ph2 = (a * a);
    double ph3 = (ph2 + ph1);
    double ph4 = Math.sqrt(ph3);
    boolean ph5 = ph4 < 1.1920929E-7;
    double ph6;
    if(ph5) {
      ph6 = 3.141592653589793;
    } else {
      double ph7 = (e * c);
      double ph8 = (f * b);
      double ph9 = (ph7 - ph8);
      double ph10 = (f * a);
      double ph11 = (d * c);
      double ph12 = (ph10 - ph11);
      double ph13 = (d * b);
      double ph14 = (e * a);
      double ph15 = (ph13 - ph14);
      double ph16 = (- ph15);
      double ph17 = (ph0 * ph9);
      double ph18 = (a * ph12);
      double ph19 = (ph18 + ph17);
      double ph20 = (- ph15);
      double ph21 = (ph0 * ph9);
      double ph22 = (a * ph12);
      double ph23 = (ph22 + ph21);
      double ph24 = Math.atan2(ph20, ph23);
      boolean ph25 = ph24 > 0.0;
      double ph26;
      if(ph25) {
	ph26 = ph24;
      } else {
	double ph27 = (6.283185307179586 + ph24);
	ph26 = ph27;
      }
      ph6 = ph26;
    }
    double ph28 = (57.29577951308232 * ph6);
    return ph28; }
}
