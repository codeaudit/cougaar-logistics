/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.mlm.ui.psp.transit;

import org.cougaar.mlm.ui.psp.transit.data.legs.LegsData;
import org.cougaar.mlm.ui.psp.transit.data.locations.LocationsData;
import org.cougaar.mlm.ui.psp.transit.data.population.PopulationData;
import org.cougaar.mlm.ui.psp.transit.data.instances.InstancesData;
import org.cougaar.mlm.ui.psp.transit.data.prototypes.PrototypesData;
import org.cougaar.mlm.ui.psp.transit.data.convoys.ConvoysData;
import org.cougaar.mlm.ui.psp.transit.data.routes.RoutesData;

/**
 * Holds all our "registered" data.
 */
public class Registry {

  // timestamps
  public long beginComputeTime;
  public long endComputeTime;

  // gathered data
  public LegsData legs;
  public LocationsData locs;
  public PopulationData carriers;
  public InstancesData cargoInstances;
  public PrototypesData cargoPrototypes;
  public ConvoysData convoys;
  public RoutesData routes;

  // "myState" flag
  public boolean interpolate;
  public boolean includeTransitLegs;

  public void setIncludeTransitLegs (boolean value) { includeTransitLegs = value; }

  public Registry() {
    legs = new LegsData();
    locs = new LocationsData();
    carriers = new PopulationData();
    cargoInstances = new InstancesData();
    cargoPrototypes = new PrototypesData();
    convoys = new ConvoysData();
    routes = new RoutesData();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Registry {");
    buf.append("\n  beginComputeTime: ").append(beginComputeTime);
    buf.append("\n  endComputeTime: ");
    if (endComputeTime < Long.MAX_VALUE) {
      buf.append(endComputeTime);
    } else {
      buf.append("<computing>");
    }
    // other fields?
    buf.append("\n}");
    return buf.toString();
  }
}
