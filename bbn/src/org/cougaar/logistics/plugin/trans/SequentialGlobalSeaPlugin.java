/*
 * <copyright>
 *  Copyright 2001-2 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.trans;

import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Location;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.logistics.plugin.trans.GLMTransConst;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.cougaar.logistics.plugin.trans.tools.LocatorImpl;
import org.cougaar.logistics.plugin.trans.tools.PortLocatorImpl;

/**
 * <pre>
 * Specific logic for dealing with backwards sea planning.
 *
 * Mainly involves worrying about POE and POD choice.
 * </pre>
 */
public class SequentialGlobalSeaPlugin extends SequentialGlobalAirPlugin {
  protected transient Set exceptions = new HashSet();
  boolean useSeaRoutes = false;
  protected transient Map routeCache = new HashMap ();

  public void localSetup() {     
    super.localSetup();

    try {
      useSeaRoutes = (getMyParams().hasParam ("useSeaRoutes")) ? 
	getMyParams().getBooleanParam("useSeaRoutes") : 
	true;
    } catch (Exception e) { warn ("got really unexpected exception " + e); }

    if (useSeaRoutes)
      locator.setFactory (ldmf); // tell route finder the ldm factory to use

    if (isDebugEnabled())
      debug ("localSetup - this " + this + " prep helper " + glmPrepHelper);
  }

  protected String type () { return "seaport"; }

  public Organization findOrgForMiddleStep () {
    Organization org = findOrgWithRole(GLMTransConst.SHIP_PACKER_ROLE);
    if (org == null) {
      error(".MiddlePortion.planMe - ERROR - No subordinate with role " + 
	    GLMTransConst.SHIP_PACKER_ROLE);
      return null;
    }
    return org;
  }

  /** don't include destination ports as POEs */
  boolean startsAtPOE (Task task) {
    String origin  = glmPrepHelper.getFromLocation(task).getGeolocCode();
    Object airport = locator.getAssetAtGeolocCode (origin);
    
    return (airport != null) && !((PortLocatorImpl)locator).isKnownException ((Asset) airport);
  }

  /** 
   * <pre>
   * Given a task, find the POE and POD for the task 
   * This will be a search among possible POEs and PODs for those that
   * are closest to the FROM-TO pair on the parent task.
   *
   * The choice will be affected by the how long it takes
   * to get from POE to POD.
   * </pre>
   */
  protected Location [] getPOEandPOD (Task parentTask, Task subtask) {
    Location [] locs = new Location[2];
    
    if (useSeaRoutes) {
      TransportationRoute route = (TransportationRoute)
	glmPrepHelper.getIndirectObject(parentTask, GLMTransConst.SEAROUTE);

      locs[0] = route.getSource().getGeolocLocation();
      locs[1] = route.getDestination().getGeolocLocation();
    } else { // just use great circle calcs
      locs[0] = getPOENearestToFromLocMiddleStep (parentTask);
      locs[1] = getPOD(parentTask);
    }

    return locs;
  }

  Location getPOENearestToFromLocMiddleStep (Task parentTask) {
    return ((PortLocatorImpl)locator).getPortNearestToFromLoc (parentTask);
  }

  /** don't remove SEAROUTE prep -- need it for datagrabber output! */
  protected void removePrepsFromMiddleStep (Task new_task) {
    glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE_DISTANCE);
  } 

  /** Instantiate the Locator, which adds a LocationCallback */
  protected void makeLocator () {
    locator = new PortLocatorImpl(this, logger);
  }
}
