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

import java.util.Date;
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

  public Organization findOrgForMiddleStep (Task task) {
    Organization org = null;
    if (isAmmo((GLMAsset) task.getDirectObject ())) {
      org = findOrgWithRole(GLMTransConst.AMMO_SHIP_PACKER_ROLE);
      if (org == null) {
	org = findOrgWithRole(GLMTransConst.SHIP_PACKER_ROLE);
      }
    }
    else
      org = findOrgWithRole(GLMTransConst.SHIP_PACKER_ROLE);

    if (org == null) {
      error(getName () + "findOrgForMiddleStep - No subordinate with role " + 
	    GLMTransConst.SHIP_PACKER_ROLE);
      return null;
    }
    return org;
  }

  /** 
   * overridden from sequentialglobalairplugin 
   */
  protected boolean allNecessaryAssetsReportedMiddleStep () {
    Object org = findOrgWithRole(GLMTransConst.SHIP_PACKER_ROLE);
    if (org == null) {
      org = findOrgWithRole(GLMTransConst.AMMO_SHIP_PACKER_ROLE);
    }

    return (org != null);
  }

  /** 
   * An asset is an ammo container if it has a contents pg, since
   * only the Ammo Packer put a contents pg on a container.
   *
   * NOTE : should call isContainer first!
   */
  protected boolean isAmmo (GLMAsset asset) {
    return asset.hasContentsPG ();
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
   * @param ignoredTask is ignored in this method
   */
  protected Location [] getPOEandPOD (Task parentTask, Task ignoredTask) {
    Location [] locs = new Location[2];

    if (useSeaRoutes) {
      TransportationRoute route = (TransportationRoute)
	glmPrepHelper.getIndirectObject(parentTask, GLMTransConst.SEAROUTE);

      locs[0] = route.getSource().getGeolocLocation();
      locs[1] = route.getDestination().getGeolocLocation();

      if (isInfoEnabled()) {
	info (getName () + ".getPOEandPOD - getting route from " + parentTask.getUID() + 
	      " it starts at " + locs[0] + 
	      " and ends at " + locs[1] +
	      " isAmmo " + isAmmo((GLMAsset) parentTask.getDirectObject ()));
      }
    } else { // just use great circle calcs
      warn ("not using searoutes?");
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

     private static final double MAX_SHIP_SPEED_KNOTS = 20.0;
     private static final long MILLIS_PER_HOUR = 60*60*1000;
     private static final long CONUS_PLANNING_FACTOR = 12*MILLIS_PER_HOUR;

  protected Date getEarlyArrivalMiddleStep (Task task, Date best) {
    Date mostEarly = prefHelper.getReadyAt(task);
    Distance distance = (Distance) glmPrepHelper.getIndirectObject(task, GLMTransConst.SEAROUTE_DISTANCE);
    long approxSeaDur = (long) (distance.getNauticalMiles()/MAX_SHIP_SPEED_KNOTS); // in hours
    long possibleEarly = best.getTime()- approxSeaDur*MILLIS_PER_HOUR;
    
    // if the travel time is already probably too little to do the job,
    // just try for the original early date
    if (possibleEarly < mostEarly.getTime())
      possibleEarly = mostEarly.getTime();
    else // otherwise, let's give CONUS a couple of days to do it
      possibleEarly = mostEarly.getTime() + CONUS_PLANNING_FACTOR;
    
    return new Date(possibleEarly);
  }
}
