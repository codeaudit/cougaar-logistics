/*
 * <copyright>
 *  Copyright 1999-2003 Honeywell Inc.
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

package org.cougaar.logistics.plugin.packer;

import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.NewGeolocLocation;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;

/**
 * An abstract class with some static methods that make it easier
 * for us to work with Geolocs.
 * This code mostly based on code written by LCG.
 */
public abstract class Geolocs {
  public static GeolocLocation abuDhabi() {
    NewGeolocLocation geoloc = GLMFactory.newGeolocLocation();
    geoloc.setName("Abu Dahbi, UARE");
    geoloc.setCountryStateCode("Abu Dahbi, UARE");
    geoloc.setCountryStateName("Abu Dahbi, UARE");
    geoloc.setGeolocCode("AAVW");
    geoloc.setIcaoCode("AAVW");
    geoloc.setLatitude(Latitude.newLatitude("24.4331"));
    geoloc.setLongitude(Longitude.newLongitude("54.6489"));
    geoloc.setInstallationTypeCode("MAP");
    return geoloc;
  }

  public static GeolocLocation blueGrass() {
    NewGeolocLocation geoloc = GLMFactory.newGeolocLocation();
    geoloc.setName("Blue Grass Depot, KY");
    geoloc.setCountryStateCode("Blue Grass Depot, KY");
    geoloc.setCountryStateName("Blue Grass Depot, KY");
    geoloc.setGeolocCode("BVJS");
    geoloc.setIcaoCode("BVJS");
    geoloc.setLatitude(Latitude.newLatitude("37.7"));
    geoloc.setLongitude(Longitude.newLongitude("-84.2167"));
    geoloc.setInstallationTypeCode("AMO");
    return geoloc;
  }

  public static GeolocLocation asmara() {
    NewGeolocLocation geoloc = GLMFactory.newGeolocLocation();
    geoloc.setName("ASMARA");
    geoloc.setCountryStateCode("ER");
    geoloc.setCountryStateName("ERITREA");
    geoloc.setGeolocCode("ZNKY");
    geoloc.setIcaoCode("ZNKY");
    geoloc.setLatitude(Latitude.newLatitude("15.2906"));
    geoloc.setLongitude(Longitude.newLongitude("38.9102"));
    geoloc.setInstallationTypeCode("JAP");
    return geoloc;
  }

  public static GeolocLocation sanliurfa() {
    NewGeolocLocation geoloc = GLMFactory.newGeolocLocation();
    geoloc.setName("SANLIURFA");
    geoloc.setCountryStateCode("TU");
    geoloc.setCountryStateName("TURKEY");
    geoloc.setGeolocCode("UWCP");
    geoloc.setIcaoCode("UWCP");
    geoloc.setLatitude(Latitude.newLatitude("37.0994"));
    geoloc.setLongitude(Longitude.newLongitude("38.8550"));
    geoloc.setInstallationTypeCode("AFD");
    return geoloc;
  }
}





