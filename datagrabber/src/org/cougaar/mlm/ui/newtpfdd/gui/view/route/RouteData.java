/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Holds the data for the RouteLayer
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 4/19/01
 **/
public class RouteData{

  //Constants:
  ////////////

  //Variables:
  ////////////
  private String name;
  private Set locations;
  private Map segments;

  //Constructors:
  ///////////////
  public RouteData(String name){
    locations=new HashSet(89);
    segments=new HashMap(89);
    this.name=name;
  }
  
  //Members:
  //////////

  public String getName(){
    return name;
  }

  public void setName(String name){
    this.name=name;
  }

  public void addSegment(RouteLoc startLoc, 
			 RouteLoc endLoc, 
			 RouteSegment seg){
    locations.add(startLoc);
    locations.add(endLoc);
    segments.put(seg.getKey(),seg);
  }
  
  public Iterator getLocationsIterator(){
    return locations.iterator();
  }
  public Iterator getSegementsIterator(){
    return segments.values().iterator();
  }

  public RouteSegment getSegment(Object key){
    return (RouteSegment)segments.get(key);
  }

  //InnerClasses:
  ///////////////
}
