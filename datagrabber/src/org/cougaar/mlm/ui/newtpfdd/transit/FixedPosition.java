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
package org.cougaar.mlm.ui.newtpfdd.transit;
import java.io.Serializable;

/** Position of an object at a specific geoloc/lat/lon
 * @author Benjamin Lubin; last modified by $Author: mthome $
 *
 * @since 11/14/00
 */
public class FixedPosition implements Position{
  protected String name;
  protected float lat;
  protected float lon;
  
  public FixedPosition(String name, double lat, double lon){
    if(name != null) {
      this.name=name.intern();
    } else {
      this.name=null;
    }
    this.lat=(float)lat;
    this.lon=(float)lon;
  }
  public FixedPosition(String name, float lat, float lon){
    if(name != null) {
      this.name=name.intern();
    } else {
      this.name=null;
    }
    this.lat=lat;
    this.lon=lon;
  }
  
  public String getName(){return name;}
  public float getLat(){return lat;}
  public float getLon(){return lon;}
  
  public int hashCode(){
    if(name==null)
      return super.hashCode();
    return name.hashCode();
  }
  
  public boolean equals(Object o){
    return (o instanceof FixedPosition &&
	    name.equals( ((FixedPosition)o).name));
  }
  public String toString(){
    return name + "("+lat+","+lon+")";
  }
}
