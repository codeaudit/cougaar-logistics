/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
