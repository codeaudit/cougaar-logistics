/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
