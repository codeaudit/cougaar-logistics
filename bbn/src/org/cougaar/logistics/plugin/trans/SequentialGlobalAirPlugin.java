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
package org.cougaar.logistics.plugin.trans;

import org.cougaar.glm.callback.GLMOrganizationCallback;
import org.cougaar.glm.callback.GLMOrganizationListener;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;

import org.cougaar.glm.ldm.asset.AirportPG;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MilitaryOrgPG;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.TransportationNode;

import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.NewGeolocLocation;
import org.cougaar.glm.ldm.plan.Position;

import org.cougaar.glm.util.AssetUtil;
import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPreference;
import org.cougaar.glm.util.GLMPrepPhrase;


import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;

import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;

import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AuxiliaryQueryType;
import org.cougaar.planning.ldm.plan.ItineraryElement;
import org.cougaar.planning.ldm.plan.Location;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.NewItineraryElement;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Priority;
import org.cougaar.planning.ldm.plan.Relationship;  
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;


import org.cougaar.lib.callback.UTILAssetCallback;
import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;
import org.cougaar.lib.callback.UTILWorkflowCallback;


import org.cougaar.util.TimeSpan;
import org.cougaar.util.log.Logger;
import org.cougaar.lib.util.UTILItinerary;
import org.cougaar.lib.util.UTILPluginException;

import org.cougaar.logistics.plugin.trans.GLMTransConst;
import org.cougaar.logistics.plugin.trans.base.SequentialPlannerPlugin;
import org.cougaar.logistics.plugin.trans.base.SequentialScheduleElement;

import org.cougaar.logistics.plugin.trans.tools.Locator;
import org.cougaar.logistics.plugin.trans.tools.LocatorImpl;
import org.cougaar.core.service.BlackboardService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Random;

/**
 * <pre>
 * This class orchestrates the backwards planning that goes on in GLMTrans.
 *
 * It makes decisions about how to expand incoming tasks into their appropriate
 * forms, e.g CONUSGround-Air-TheaterGround.  It monitors the state of each subtask
 * and then notices when a task that is dependent on another has all its dependencies
 * met and is ready to be planned.  This task is then planned.
 *
 * </pre>
 * <!--
 * (When printed, any longer line will wrap...)
 *2345678901234567890123456789012345678901234567890123456789012345678901234567890
 *        1         2         3         4         5         6         7         8
 * -->
 */
public class SequentialGlobalAirPlugin extends SequentialPlannerPlugin
  implements GLMOrganizationListener {
  
  private static final long ONE_HOUR = 1000l*60l*60l;
  private static final double BILLION = 1000000000.0d;
  private static final String SAND = "SAND";
  public static int CONUS_THEATER_DIVIDING_LONGITUDE = 
    Integer.getInteger("SequentialGlobalAirPlugin.CONUS_THEATER_DIVIDING_LONGITUDE",25).intValue();

  public void localSetup() {     
    super.localSetup();

    try {bestDateBackoff = getMyParams().getLongParam("bestDateBackoff");}
    catch(Exception e) {bestDateBackoff = ONE_HOUR;} 

    outerGLMPrepHelper = new GLMPrepPhrase (logger);
    glmAssetHelper = new AssetUtil (logger);
  }

  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) { 
    UTILFilterCallback myCallback = new UTILExpandableTaskCallback (bufferingThread, logger);
    return myCallback;
  } 

  /** 
   * Implemented for UTILGenericListener interface
   *
   * Look for tasks that
   * 1) Have TRANSPORT as their verb
   *
   * @param t Task to check for interest
   * @return boolean true if task is interesting
   * @see org.cougaar.lib.callback.UTILGenericListener
   */

  public boolean interestingTask(Task t) {
    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);
    if (isDebugEnabled() &&
	hasTransportVerb) {
      debug (" : interested in expandable task " + 
	     t + " with direct obj " + 
	     t.getDirectObject ());
    }
    if (isDebugEnabled() && !hasTransportVerb) 
      debug(" : Ignoring task with verb " + t.getVerb());

    return hasTransportVerb;
  }

  public boolean interestingExpandedTask(Task t) {
    return outerGLMPrepHelper.hasPrepNamed(t, GLMTransConst.SequentialSchedule);
  }

  /*** AssetListener ***/
  /** only interested in airports/seaports */
  public boolean interestingAsset (Asset asset) {
    try {
      boolean retval = asset instanceof TransportationNode;
      if (isDebugEnabled())
	debug (".interestingAsset : " + ((retval) ? "interested in " : "ignoring ") +
	       asset);
	  
      return retval;
    } catch (Exception e) {}
    return false;
  }
  
  /*** Organization Listener ***/
    
  public void setupFilters () {
    super.setupFilters ();
        
    if (isInfoEnabled())
      info (" : Filtering for Organizations...");
        
    addFilter (myOrgCallback   = createOrganizationCallback ());

    // Instantiate the Locator, which adds a LocationCallback
    aPOELocator = new LocatorImpl(this, logger);

    if (blackboard.didRehydrate ()) {
      if (isInfoEnabled())
	info (".localSetup - didRehydrate!");
	  
      if (myAssetCallback == null)
	info (".localSetup - asset callback is null?");

      handleNewAssets (myAssetCallback.getSubscription().elements());
    }
  }
      
  public BlackboardService getBlackboard () {
    return blackboard;
  }
  
  protected GLMOrganizationCallback getOrganizationCallback    () { return myOrgCallback; }
  protected GLMOrganizationCallback createOrganizationCallback () { 
    return new GLMOrganizationCallback (this, logger); 
  }
    
  public Enumeration getOrganizationAssets () {
    return myOrgCallback.getSubscription ().elements ();
  }
    
  public Organization getSelf () {
    return glmAssetHelper.getSelf (getOrganizationAssets ());
  }
    
  public void handleNewOrganization(Enumeration org_assets) {
    shouldRefreshOrgList = true;
  }

  public void handleChangedOrganization (Enumeration e) {}

  public void handleNewAssets (Enumeration org_assets) {
    if (isDebugEnabled()) debug (".handleNewAsset called.");

    for (; org_assets.hasMoreElements ();) {
      GLMAsset airport = (GLMAsset) org_assets.nextElement ();
      Position loc = airport.getPositionPG ().getPosition ();
      String geoloc = ((GeolocLocation)loc).getGeolocCode();
      geolocToAirport.put (geoloc, airport);
      airportToLocation.put (airport, loc);
      if (isDebugEnabled())
	debug (".handleNewAsset mapping <" + 
			    geoloc + "> to <" + airport + ">");
    }
  }

  public Schedule createEmptyPlan(Task parent) {
    TheaterPortion theater = new TheaterPortion(parent, logger);
    // IsbPortion isb = new IsbPortion(parent);
    AirPortion air = new AirPortion(parent, logger);
    ConusPortion conus = new ConusPortion(parent, logger);

    boolean needsTheater  = needsTheater (parent);
    // boolean needsISB     = needsISB (parent);
    boolean needsCONUS    = needsCONUS (parent);
    boolean needsAirOrSea = needsAirOrSea   (parent);

    // create the dependencies ------------------

    if (isDebugEnabled())
      debug (".createEmptyPlan - " + parent.getDirectObject().getUID().getOwner() + 
	     "'s task " + parent.getUID() + 
	     " - " + parent.getDirectObject () + " Legs : " +
	     ((needsCONUS) ? "G" : "") +
	     ((needsAirOrSea) ? "A/S" : "") +
	     ((needsTheater) ? "T" : ""));
		
    //        Vector isbdep = new Vector();
    //		if (needsTheater)
    //		  isbdep.addElement(theater);
    //        isb.setDependencies(isbdep);

    //		if (needsISB)
    //		  airdep.addElement(isb);
    if (needsTheater) {
      Vector airdep = new Vector();
      airdep.addElement(theater);
      air.setDependencies(airdep);
    }

    if (needsAirOrSea) {
      Vector conusdep = new Vector();
      conusdep.addElement(air);
      conus.setDependencies(conusdep);
    }

    // create the Schedule ------------------

    NewSchedule retSchedule = new ScheduleImpl();
    if (needsTheater)
      retSchedule.addScheduleElement(theater);
    //		if (needsISB)
    //		  retSchedule.addScheduleElement(isb);
    if (needsAirOrSea)
      retSchedule.addScheduleElement (air);
    if (needsAirOrSea && needsCONUS)
      retSchedule.addScheduleElement (conus);

    if (isDebugEnabled())
      debug (".createEmptyPlan - " + retSchedule);
    if (retSchedule.isEmpty ()) {
      Object origin = outerGLMPrepHelper.getFromLocation(parent);
      Object destination = outerGLMPrepHelper.getToLocation(parent);
      error (".createEmptyPlan - created an empty portion schedule. "+
			  "For task " + parent.getUID () + " going from " + origin + " to " + destination);
      /*
      error ("Reasoning is as follows : ");
      needsTheater (parent);
      needsAirOrSea (parent);
      needsCONUS (parent);
      */
    }

    return retSchedule;
  }

  /** 
   * needs a theater leg if TO location is not an airbase OR if FROM loc is in theater (i.e. starts in theater).
   */
  boolean needsTheater (Task task) {
    if (glmAssetHelper.isPassenger(task.getDirectObject()))
      return false;
    boolean inTheater = startsInTheater(task);
    GeolocLocation destinationGeoloc = (GeolocLocation) outerGLMPrepHelper.getIndirectObject(task, Constants.Preposition.TO); 
    Object airport = geolocToAirport.get (destinationGeoloc.getGeolocCode());

    if (isDebugEnabled()) {
      if (airport == null)
	debug(".needsTheater - Theater move needed, since TO location <" + destinationGeoloc + 
			   "> is not an airport/seaport among these airports/seaports : " + geolocToAirport.keySet ());
      else
	debug(".needsTheater - no Theater move needed, since TO location <" + destinationGeoloc + 
			   "> is an airport/seaport (" + airport + ").");
    }
  
    // if the destination is not an airbase, add a theater ground move leg
    return (inTheater || (airport == null));
  }

  /** 
   * sort of cheesy, but anything starting east of 25 degrees Longitude gets a final theater leg.
   * (The 25 degrees can be set with a system property.)
   */
  boolean startsInTheater (Task task) {
    GeolocLocation sourceGeoloc = 
      (GeolocLocation) outerGLMPrepHelper.getIndirectObject(task, Constants.Preposition.FROM); 
    return (sourceGeoloc.getLongitude().getDegrees() > CONUS_THEATER_DIVIDING_LONGITUDE);
  }
	
  /** 
   * sort of cheesy, but anything starting west of 25 degrees Longitude gets an initial ground leg.
   * (The 25 degrees can be set with a system property.)
   */
  boolean startsInCONUS (Task task) {
    GeolocLocation sourceGeoloc = 
      (GeolocLocation) outerGLMPrepHelper.getIndirectObject(task, Constants.Preposition.FROM);
    return (sourceGeoloc.getLongitude().getDegrees() < CONUS_THEATER_DIVIDING_LONGITUDE);
  }
	
  /** 
   ** if the task is coming from CONUS, needs an air leg to get it to theater 
   ** Also, if the task is coming from an airbase, e.g. Rhein Mein in Germany
   **/
  boolean needsAirOrSea (Task task) {
    String origin = outerGLMPrepHelper.getFromLocation(task).getGeolocCode();
    Object airport = geolocToAirport.get (origin);
    boolean startsAtPOE   = startsAtPOE (task);
    boolean startsInCONUS = startsInCONUS (task);
	
    if (isDebugEnabled() && !startsInCONUS && !startsAtPOE) 
      debug (".needsAirOrSea - could not find FROM " + origin + 
			  " on list of possible POEs : " + geolocToAirport.keySet () + 
			  " and doesn't start in CONUS.");
	
    return startsInCONUS || startsAtPOE;
  }

  boolean startsAtPOE (Task task) {
    String origin = outerGLMPrepHelper.getFromLocation(task).getGeolocCode();
    Object airport = geolocToAirport.get (origin);
    return (airport != null);
  }

  boolean needsISB (Task task) {
    GeolocLocation PODGeoloc = 
      (GeolocLocation) outerGLMPrepHelper.getIndirectObject(task, Constants.Preposition.VIA); 
    GLMAsset airport = (GLMAsset) geolocToAirport.get (PODGeoloc.getGeolocCode());

    try {
      AirportPG airportPG = airport.getAirportPG ();
      if (isDebugEnabled())
	debug(".needsISB - for " + PODGeoloc + 
			   " airport is " + airport + " with runway " + airportPG.getRunwayType());
		
      return (airportPG.getRunwayType().equals (SAND)); // if the airbase has sand runways, need an isb (= Cairo)
    }
    catch (Exception e) {	  
      error(".needsISB - ERROR - no airport for  " + PODGeoloc);
    }

    return false;
  }

  boolean needsCONUS (Task task) {
    if (glmAssetHelper.isPassenger(task.getDirectObject()))
      return false;
    boolean inCONUS = startsInCONUS (task);

    if (!inCONUS) {
      if (isDebugEnabled()) {
	GeolocLocation sourceGeoloc = 
	  (GeolocLocation) outerGLMPrepHelper.getIndirectObject(task, Constants.Preposition.FROM); 
	debug(".needsCONUS - no CONUS move needed, since from " + sourceGeoloc + 
	     " is not in CONUS.");
      }
      return false;
    }

    GeolocLocation sourceGeoloc = 
      (GeolocLocation) outerGLMPrepHelper.getIndirectObject(task, Constants.Preposition.FROM); 
    String geoloc = sourceGeoloc.getGeolocCode();
    Object airport = geolocToAirport.get (geoloc);
	
    if (isDebugEnabled()) {
      if (airport == null)
	debug(".needsCONUS - CONUS move needed, since from <" + geoloc + 
			   "> is not an airport/seaport among these airports/seaports : " + geolocToAirport.keySet ());
      else
	debug(".needsCONUS - no CONUS move needed, since from " + sourceGeoloc + 
			   " is an airport/seaport (" + airport + ").");
    }

    // if the source is not an airbase, add a conus ground move leg to the airbase
    return (airport == null);
  }

  /** 
   * Uses locator
   **/
  Location getPOD (Task task) {
    return (Location) aPOELocator.getNearestLocation(outerGLMPrepHelper.getToLocation(task));
  }

  public Organization findOrgForMiddleStep () {
    Organization organicAir = findOrgWithRole(GLMTransConst.ORGANIC_AIR_ROLE);
    if (organicAir == null) {
      error(".findOrgForMiddleStep - ERROR - No subordinate with role " + 
	    GLMTransConst.ORGANIC_AIR_ROLE);
      return null;
    }
    return organicAir;
  }
  
  public class TheaterPortion extends SequentialScheduleElement {
    public TheaterPortion(Task parent, Logger logger) {
      super(parent, outerGLMPrepHelper, logger);
    }

    public String toString () { return "TheaterPortion"; }
	  
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      if (this.logger.isDebugEnabled())
	this.logger.debug (".TheaterPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						parentTask,
						parentTask.getDirectObject(),
						myPlugin.publicGetClusterIdentifier());
      // find the theater org
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
			
      Organization theater = plugin.findOrgWithRole(GLMTransConst.THEATER_MCC_ROLE);
      if (theater == null) {
	this.logger.error(".TheaterPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.THEATER_MCC_ROLE);
	return null;
      }

      // if we've got an air move, find the nearest airbase, otherwise, just do ground move from from loc
      Location PODLocation = 
	(plugin.startsInTheater(parentTask)) ? outerGLMPrepHelper.getFromLocation(parentTask) : plugin.getPOD (parentTask);

      outerGLMPrepHelper.replacePrepOnTask(new_task, 
				      outerGLMPrepHelper.makePrepositionalPhrase(plugin.publicGetFactory(),
									    Constants.Preposition.FROM,
									    PODLocation));
      outerGLMPrepHelper.removePrepNamed(new_task, GLMTransConst.MODE);
            
      PlanElement pe = allocHelper.makeAllocation(myPlugin,
						   myPlugin.publicGetFactory(), 
						   myPlugin.publicGetRealityPlan(),
						   new_task,
						   theater,   
						   prefHelper.getReadyAt(new_task),
						   prefHelper.getBestDate(new_task),
						   allocHelper.MEDIUM_CONFIDENCE,
						   Constants.Role.TRANSPORTER);
      myPlugin.publicPublishAdd(pe);
      return new_task;
    }
  }
    
  public class IsbPortion extends SequentialScheduleElement {
    protected double myMaxCost = BILLION;
        
    public IsbPortion(Task parent, Logger logger) {
      super(parent, outerGLMPrepHelper, logger);
    }

    public String toString () { return "IsbPortion"; }
        
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      if (this.logger.isDebugEnabled())
	this.logger.debug (".IsbPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						parentTask,
						parentTask.getDirectObject(),
						myPlugin.publicGetClusterIdentifier());
            
      // find the ISB org
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Organization isb = plugin.findOrgWithRole(GLMTransConst.C130_ROLE);
      if (isb == null) {
	this.logger.error(".IsbPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.C130_ROLE);
	return null;
      }

      Organization theater = plugin.findOrgWithRole(GLMTransConst.THEATER_MCC_ROLE);
      if (theater == null) {
	this.logger.error(".TheaterPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.THEATER_MCC_ROLE);
	return null;
      }

      Date end = null;

      if (getDependencies().isEmpty()) { // if there was no theater leg
	end = prefHelper.getLateDate(parentTask);
	if (this.logger.isDebugEnabled())
	  this.logger.debug (".IsbPortion.planMe - no dependencies, end preference is " +
		       end);
      } else {
	end = ((SequentialScheduleElement)getDependencies().elementAt(0)).getStartDate();
	prefHelper.replacePreference(new_task, 
					 prefHelper.makeEndDatePreference(myPlugin.publicGetFactory(), end));
	if (this.logger.isDebugEnabled())
	  this.logger.debug (".IsbPortion.planMe - dependent on theater, end preference is " +
		       end);
      }

      Location isbLocation = plugin.getISBLocation(end); // doesn't matter when
      Location PODLocation = plugin.getPOD (parentTask);
            
      outerGLMPrepHelper.replacePrepOnTask(new_task, 
				      outerGLMPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.FROM,
									    isbLocation));
      outerGLMPrepHelper.replacePrepOnTask(new_task, 
				      outerGLMPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.TO,
									    PODLocation));

      outerGLMPrepHelper.removePrepNamed(new_task, GLMTransConst.MODE);

      new_task.addPreference (prefHelper.makeCostPreference (myPlugin.publicGetFactory(), myMaxCost));

      List aspects = new ArrayList ();
      aspects.add (new AspectValue (AspectType.START_TIME, 
				    (double) prefHelper.getReadyAt(new_task).getTime ()));
      aspects.add (new AspectValue (AspectType.END_TIME,   
				    (double) end.getTime ()));
      aspects.add (new AspectValue (AspectType.COST, 0));

      PlanElement c130_alloc = allocHelper.makeAllocation (myPlugin,
							    myPlugin.publicGetFactory(),
							    myPlugin.publicGetRealityPlan(),
							    new_task,
							    isb,
							    (AspectValue []) aspects.toArray (new AspectValue[0]), 
							    allocHelper.MEDIUM_CONFIDENCE,
							    Constants.Role.TRANSPORTER);
      myPlugin.publicPublishAdd(c130_alloc);
      return new_task;
    }
  }

  public class AirPortion extends SequentialScheduleElement {
    public AirPortion(Task parent, Logger logger) {
      super(parent, outerGLMPrepHelper, logger);
    }
        
    public String toString () { return "AirPortion"; }
        
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      if (this.logger.isDebugEnabled())
	this.logger.debug (".AirPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						parentTask,
						parentTask.getDirectObject(),
						myPlugin.publicGetClusterIdentifier());

      Vector getDependencies = getDependencies();

      /*
	boolean dependsOnISB = false;
			
	for (int i = 0; i < getDependencies.size(); i++) {
	if (getDependencies.get (i) instanceof IsbPortion) {
	dependsOnISB = true;
	break;
	}
	}
      */

      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Location [] locations = plugin.getPOEandPOD (parentTask, new_task);
      Location fromLocation = locations[0];
      Location toLocation   = locations[1];

      Date end = null;
      Date early, best;

      if (getDependencies().isEmpty()) { // if there was neither a theater nor a isb leg
	end = prefHelper.getLateDate(parentTask);
	best = end;
      } else {
	end = ((SequentialScheduleElement)getDependencies().elementAt(0)).getStartDate();
	early = prefHelper.getReadyAt(new_task);
	best  = new Date(end.getTime() - plugin.bestDateBackoff);
	if (best.getTime () < early.getTime())
	  best = early;
			  
	prefHelper.replacePreference(new_task, 
					 prefHelper.makeEndDatePreference(myPlugin.publicGetFactory(), 
									      early,best,end));
	// if (dependsOnISB)
	//	toLocation = plugin.getISBLocation(end); // doesn't matter when
      }

      outerGLMPrepHelper.replacePrepOnTask(new_task, 
				      outerGLMPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.FROM,
									    fromLocation));

      outerGLMPrepHelper.replacePrepOnTask(new_task, 
				      outerGLMPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.TO,
									    toLocation));
            
      Organization middleOrg = plugin.findOrgForMiddleStep ();
			
      List aspects = new ArrayList ();
      aspects.add (new AspectValue (AspectType.START_TIME, 
				    (double) prefHelper.getReadyAt(new_task).getTime ()));
      aspects.add (new AspectValue (AspectType.END_TIME,   
				    (double) best.getTime ()));
      outerGLMPrepHelper.removePrepNamed(new_task, GLMTransConst.MODE);

      PlanElement pe = allocHelper.makeAllocation(myPlugin,
						   myPlugin.publicGetFactory(), 
						   myPlugin.publicGetRealityPlan(),
						   new_task, 
						   middleOrg,
						   (AspectValue []) aspects.toArray (new AspectValue[0]), 
						   allocHelper.MEDIUM_CONFIDENCE,
						   Constants.Role.TRANSPORTER);

      myPlugin.publicPublishAdd(pe);
      return new_task;
    }
        
  }

  public class ConusPortion extends SequentialScheduleElement {
    public ConusPortion(Task parent, Logger logger) {
      super(parent, outerGLMPrepHelper, logger);
    }

    public String toString () { return "ConusPortion"; }
        
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      if (this.logger.isDebugEnabled())
	this.logger.debug (".ConusPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						parentTask,
						parentTask.getDirectObject(),
						myPlugin.publicGetClusterIdentifier());

      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Location toLocation;
						
      Date end = null;
      if (!getDependencies().isEmpty()) {
	SequentialScheduleElement sse = (SequentialScheduleElement)getDependencies().elementAt(0);
	Task dependingTask = sse.getTask ();
	end = sse.getStartDate();
	toLocation = outerGLMPrepHelper.getFromLocation (dependingTask);
      }
      else {
	end = prefHelper.getLateDate(new_task);
	toLocation = plugin.getPOENearestToFromLoc (parentTask);
      }
			
      Date early = prefHelper.getReadyAt(new_task);
      Date best  = new Date(end.getTime() - 1000);
			  
      prefHelper.replacePreference(new_task, 
				       prefHelper.makeEndDatePreference(myPlugin.publicGetFactory(), 
									    early,best,end));

      outerGLMPrepHelper.replacePrepOnTask(new_task, 
				      outerGLMPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.TO,
									    toLocation));

      Organization conusGround = plugin.findOrgWithRole(GLMTransConst.CONUS_GROUND_ROLE);
      if (conusGround == null) {
	this.logger.error(".ConusPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.CONUS_GROUND_ROLE);
	return null;
      }

      outerGLMPrepHelper.removePrepNamed(new_task, GLMTransConst.MODE);

      PlanElement pe = allocHelper.makeAllocation(myPlugin,
						   myPlugin.publicGetFactory(), 
						   myPlugin.publicGetRealityPlan(),
						   new_task,
						   conusGround,    // allocate to conus ground
						   prefHelper.getReadyAt(new_task),
						   end,
						   allocHelper.MEDIUM_CONFIDENCE,
						   Constants.Role.TRANSPORTER);
      myPlugin.publicPublishAdd(pe);
      return new_task;
    }

    public void finishPlan (Allocation alloc, SequentialPlannerPlugin plugin) {
      if (this.logger.isDebugEnabled()) 
	this.logger.debug(".ConusPortion.finishPlan - Planning complete.");
      super.finishPlan (alloc, plugin);
    }
  }

  Location getISBLocation (Date when) {
    Organization isb = findOrgWithRole (GLMTransConst.C130_ROLE);
    Location loc = (Location) airportToLocation.get (isb);
    if (loc == null) {
      loc = findLocationAtTime (isb, when.getTime());
      airportToLocation.put (isb, loc);
    }
    return loc;
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
    
    locs[0] = getPOENearestToFromLoc(parentTask);
    locs[1] = getPOD(parentTask);
    
    return locs;
  }

  /** 
   * First check from to see if it's an airport, otherwise, must be a fort 
   * If so, lookup airbase (APOE) nearest fort.
   *
   * @return NULL probably only when no locations have been read in -- an ERROR
   *  otherwise, the nearest location
   **/
  Location getPOENearestToFromLoc (Task parentTask) {
    String origin = outerGLMPrepHelper.getFromLocation(parentTask).getGeolocCode();
    Object airport = geolocToAirport.get (origin);
    if (airport != null)
      return (Location) airportToLocation.get (airport);
    return getPOENearestToFromLoc (parentTask, Collections.EMPTY_SET);
  }

  Location getPOENearestToFromLoc (Task parentTask, Collection exceptions) {
    String origin = outerGLMPrepHelper.getFromLocation(parentTask).getGeolocCode();
    Object airport = geolocToAirport.get (origin);
    if (airport != null)
      return (Location) airportToLocation.get (airport);

    Location poe = aPOELocator.getNearestLocationExcept(outerGLMPrepHelper.getFromLocation(parentTask), exceptions);

    if (poe == null)
      error(".getPOENearestToFromLoc - could not find POD for task " + 
	    parentTask.getUID () +
	    " going to " + outerGLMPrepHelper.getFromLocation(parentTask) + ", though I can choose " +
	    "from " + aPOELocator.getNumKnownLocations () + " known locations.");
    return poe;
  }
  
  Location getPOENearestToFromLocMiddleStep (Task parentTask) {
    return getPOENearestToFromLoc (parentTask);
  }
  
  /** 
   * cache role to org mapping 
   *
   * Supports spawning -- choosing among several possible subordinates with role 
   * <tt>role</tt>.
   *
   * Calls chooseAmongOrgs.
   *
   * @see #chooseAmongOrgs
   */
  public Organization findOrgWithRole(Role role) {
    List orgs = (List) roleToOrg.get (role);

    if (orgs == null || shouldRefreshOrgList) {
      orgs = new ArrayList ();
      roleToOrg.put (role, orgs);
      RelationshipSchedule sched = getSelf().getRelationshipSchedule();
      Collection matchingRels =  sched.getMatchingRelationships(role,
								TimeSpan.MIN_VALUE,
								TimeSpan.MAX_VALUE);

      for (Iterator it = matchingRels.iterator(); it.hasNext();) {
	Relationship rel = (Relationship)it.next();
	orgs.add (sched.getOther(rel));
      }

      shouldRefreshOrgList = false;
    }
	
    if (orgs.isEmpty ()) {
      error (".findOrgWithRole - could not find any organizations with role " + 
			  role + " among subordinates -- relationship schedule is:\n" + 
			  getSelf().getRelationshipSchedule());
      return null;
    }

    return chooseAmongOrgs (orgs);
  }

  /** 
   * this could be more sophisticated in the future 
   * 
   * Right now, just round-robin.
   */
  protected Organization chooseAmongOrgs (List orgs) {
    int i = r.nextInt(orgs.size());

    if (isDebugEnabled())
      debug (".chooseAmongOrgs - choosing org #" + i + " (" +
	     orgs.get(i) + ") from " + orgs.size () + " orgs :" + orgs);

    return (Organization) orgs.get(i);
  }

  private Location findLocationAtTime(Organization org, long time) {
    LocationSchedulePG locPG = org.getLocationSchedulePG();
    if (locPG == null) {
      MilitaryOrgPG moPG = org.getMilitaryOrgPG();
      return moPG.getHomeLocation();
    }
	  
    Schedule sched = locPG.getSchedule();
    Collection schedElements = sched.getScheduleElementsWithTime(time);
    LocationScheduleElement lse = (LocationScheduleElement) schedElements.iterator().next();
    return lse.getLocation();
  }


  protected GLMOrganizationCallback myOrgCallback;
    
  protected Map roleToOrg = new HashMap ();
  protected Map geolocToAirport = new HashMap ();
  protected Map airportToLocation = new HashMap ();
  protected int myMaxOrgs;
  protected Locator aPOELocator;

  protected long bestDateBackoff;
  boolean shouldRefreshOrgList = false;
  private Random r = new Random();

  protected GLMPrepPhrase outerGLMPrepHelper;
  protected AssetUtil     glmAssetHelper;
}
