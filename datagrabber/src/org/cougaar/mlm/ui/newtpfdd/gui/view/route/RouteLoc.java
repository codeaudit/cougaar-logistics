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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A location on the route
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 4/19/01
 **/
public class RouteLoc{
  //Constants:
  ////////////

  //Variables:
  ////////////
  private String dbUID;
  private String geoloc;
  private String prettyName;
  private float lat;
  private float lon;
  
  //Constructors:
  ///////////////
  public RouteLoc(String dbUID, String geoloc, String prettyName, float lat, float lon){
    this.dbUID=dbUID;
    this.geoloc=geoloc;
    this.prettyName=prettyName;
    this.lat=lat;
    this.lon=lon;
  }
  
  //Members:
  //////////
  public String getDBUID(){return dbUID;}
  public String getGeoLoc(){return geoloc;}
  public String getPrettyName(){return prettyName;}
  public float getLat(){return lat;}
  public float getLon(){return lon;}
  public int hashCode(){
    if (geoloc != null) {
      return geoloc.hashCode();
    } else {
      return super.hashCode ();
    }
  }
  public boolean equals(Object o){
    if(this==o)
      return true;
    if(o instanceof RouteLoc){
      if(dbUID!=null&&((RouteLoc)o).dbUID!=null)
	return dbUID.equals(((RouteLoc)o).dbUID);
    }
    return false;
  }
}
