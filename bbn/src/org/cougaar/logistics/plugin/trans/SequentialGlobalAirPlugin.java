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
package org.cougaar.logistics.plugin.trans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Random;

import org.cougaar.core.service.BlackboardService;

import org.cougaar.glm.callback.GLMOrganizationCallback;
import org.cougaar.glm.callback.GLMOrganizationListener;

import org.cougaar.logistics.ldm.Constants;

import org.cougaar.glm.ldm.asset.AirportPG;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MilitaryOrgPG;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.TransportationNode;

import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.logistics.plugin.trans.GLMTransConst;
import org.cougaar.logistics.plugin.trans.base.SequentialPlannerPlugin;
import org.cougaar.logistics.plugin.trans.base.SequentialScheduleElement;

import org.cougaar.logistics.plugin.trans.tools.BlackboardPlugin;
import org.cougaar.logistics.plugin.trans.tools.Locator;
import org.cougaar.logistics.plugin.trans.tools.LocatorImpl;

import org.cougaar.glm.util.AssetUtil;
import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;

import org.cougaar.lib.util.UTILAllocate;
import org.cougaar.lib.util.UTILExpand;
import org.cougaar.lib.util.UTILPreference;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;

import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Location;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Relationship;  
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewWorkflow;

import org.cougaar.util.TimeSpan;
import org.cougaar.util.log.Logger;

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
  implements GLMOrganizationListener, BlackboardPlugin {
  
  private static final long ONE_HOUR = 1000l*60l*60l;
  //private static final double BILLION = 1000000000.0d;
  //private static final String SAND = "SAND";
  public static int CONUS_THEATER_DIVIDING_LONGITUDE = 
    Integer.getInteger("SequentialGlobalAirPlugin.CONUS_THEATER_DIVIDING_LONGITUDE",25).intValue();

  public void localSetup() {     
    super.localSetup();

    try {
      if (getMyParams().hasParam ("bestDateBackoff")) {
	bestDateBackoff = getMyParams().getLongParam("bestDateBackoff");
      }
      else {
	bestDateBackoff = ONE_HOUR; 
      }
    } catch (Exception e) { warn ("got really unexpected exception " + e); }

    if (isDebugEnabled())
      debug ("localSetup - Creating prep helper and asset helper.");

    if (isDebugEnabled())
      debug ("localSetup - this " + this + " prep helper " + glmPrepHelper);
  }

  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) { 
    myInputTaskCallback = new UTILExpandableTaskCallback (bufferingThread, logger);
    return myInputTaskCallback;
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
    if (!t.getSource ().equals (getAgentIdentifier ()))
      return true;
    return glmPrepHelper.hasPrepNamed(t, GLMTransConst.SequentialSchedule);
  }

  /** 
   * Implemented for AssetListener interface<br>
   * only interested in airports/seaports 
   */
  public boolean interestingAsset (Asset asset) {
    boolean retval = asset instanceof TransportationNode;
    if (isDebugEnabled())
      debug (".interestingAsset : " + ((retval) ? "interested in " : "ignoring ") +
	     asset);
    
    return retval;
  }
  
  /*** Organization Listener ***/
    
  public void setupFilters () {
    glmPrepHelper  = new GLMPrepPhrase (logger);
    glmAssetHelper = new AssetUtil (logger);

    super.setupFilters ();
        
    if (isInfoEnabled())
      info (" : Filtering for Organizations...");
        
    addFilter (myOrgCallback   = createOrganizationCallback ());

    makeLocator ();
  }

  /** Instantiate the Locator, which adds a LocationCallback */
  protected void makeLocator () {
    locator = new LocatorImpl(this, logger);
  }
      
  /** implemented for BlackboardPlugin interface -- need public access! */
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

  public void handleTask(Task t) {
    NewTask subtask = expandHelper.makeSubTask (ldmf, t, t.getDirectObject(), getAgentIdentifier());
    subtask.setContext (t.getContext());

    NewWorkflow wf  = ldmf.newWorkflow();
    wf.setParentTask(t);
    ((NewTask)subtask).setWorkflow(wf);
    wf.addTask (subtask);

    Expansion   exp = ldmf.createExpansion(t.getPlan(), t, wf, null);

    publishAdd(subtask);
    publishAdd(exp);

    super.handleTask (subtask);
  }

  public Schedule createEmptyPlan(Task parent) {
    TheaterPortion theater = new TheaterPortion(parent);
    // IsbPortion isb = new IsbPortion(parent);
    AirPortion     air     = new AirPortion(parent);
    ConusPortion   conus   = new ConusPortion(parent);

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
      Object origin = glmPrepHelper.getFromLocation(parent);
      Object destination = glmPrepHelper.getToLocation(parent);
      error (".createEmptyPlan - created an empty portion schedule. "+
	     "For task " + parent.getUID () + " going from " + origin + " to " + destination);
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
    GeolocLocation destinationGeoloc = (GeolocLocation) glmPrepHelper.getIndirectObject(task, Constants.Preposition.TO); 
    Object asset = locator.getAssetAtGeolocCode (destinationGeoloc.getGeolocCode());

    if (isDebugEnabled()) {
      if (asset == null) {
	debug(".needsTheater - Theater move needed, since TO location <" + destinationGeoloc + 
	      "> is not a " + type () + " among these " + type () +"s : " + locator.knownGeolocCodes ());
      }
      else {
	debug(".needsTheater - no Theater move needed, since TO location <" + destinationGeoloc + 
	      "> is a " + type () + " (" + asset + ").");
      }
    }
  
    // if the destination is not an airbase/seaport, add a theater ground move leg
    return (inTheater || (asset == null));
  }

  protected String type () { return "airport"; }

  /** 
   * sort of cheesy, but anything starting east of 25 degrees Longitude gets a final theater leg.
   * (The 25 degrees can be set with a system property.)
   */
  boolean startsInTheater (Task task) {
    GeolocLocation sourceGeoloc = 
      (GeolocLocation) glmPrepHelper.getIndirectObject(task, Constants.Preposition.FROM); 
    return (sourceGeoloc.getLongitude().getDegrees() > CONUS_THEATER_DIVIDING_LONGITUDE);
  }
	
  /** 
   * sort of cheesy, but anything starting west of 25 degrees Longitude gets an initial ground leg.
   * (The 25 degrees can be set with a system property.)
   */
  boolean startsInCONUS (Task task) {
    GeolocLocation sourceGeoloc = 
      (GeolocLocation) glmPrepHelper.getIndirectObject(task, Constants.Preposition.FROM);
    return (sourceGeoloc.getLongitude().getDegrees() < CONUS_THEATER_DIVIDING_LONGITUDE);
  }
	
  /** 
   ** if the task is coming from CONUS, needs an air leg to get it to theater 
   ** Also, if the task is coming from an airbase, e.g. Rhein Mein in Germany
   **/
  boolean needsAirOrSea (Task task) {
    String origin = glmPrepHelper.getFromLocation(task).getGeolocCode();
    boolean startsAtPOE   = startsAtPOE   (task);
    boolean startsInCONUS = startsInCONUS (task);
	
    if (isDebugEnabled() && !startsInCONUS && !startsAtPOE) 
      debug (".needsAirOrSea - could not find FROM " + origin + 
	     " on list of possible POEs : " + locator.knownGeolocCodes() + 
	     " and doesn't start in CONUS.");
	
    return startsInCONUS || startsAtPOE;
  }

  boolean startsAtPOE (Task task) {
    String origin = glmPrepHelper.getFromLocation(task).getGeolocCode();
    Object airport = locator.getAssetAtGeolocCode (origin);
    return (airport != null);
  }

  /*
  boolean needsISB (Task task) {
    GeolocLocation PODGeoloc = 
      (GeolocLocation) glmPrepHelper.getIndirectObject(task, Constants.Preposition.VIA); 
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
  */

  boolean needsCONUS (Task task) {
    if (glmAssetHelper.isPassenger(task.getDirectObject()))
      return false;
    boolean inCONUS = startsInCONUS (task);

    if (!inCONUS) {
      if (isDebugEnabled()) {
	GeolocLocation sourceGeoloc = 
	  (GeolocLocation) glmPrepHelper.getIndirectObject(task, Constants.Preposition.FROM); 
	debug(".needsCONUS - no CONUS move needed, since from " + sourceGeoloc + 
	      " is not in CONUS.");
      }
      return false;
    }

    GeolocLocation sourceGeoloc = 
      (GeolocLocation) glmPrepHelper.getIndirectObject(task, Constants.Preposition.FROM); 
    String geoloc = sourceGeoloc.getGeolocCode();
    //    Object airport = geolocToAirport.get (geoloc);
    Object airport = locator.getAssetAtGeolocCode (geoloc);
	
    if (isDebugEnabled()) {
      if (airport == null) {
	debug(".needsCONUS - CONUS move needed, since from <" + geoloc + 
	      "> is not a " + type () + " among these " + type () + "s : " + locator.knownGeolocCodes ());
      }
      else {
	debug(".needsCONUS - no CONUS move needed, since from " + sourceGeoloc + 
	      " is a " + type () + " (" + airport + ").");
      }
    }

    // if the source is not an airbase, add a conus ground move leg to the airbase
    return (airport == null);
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

  protected abstract static class ElementBase extends SequentialScheduleElement {
    protected ElementBase (Task parent) {
      super (parent);
    }

    /** check to see if was an error */
    protected void reportZeroDuration (Allocation alloc, SequentialPlannerPlugin plugin) {
      GLMPrepPhrase glmPrepHelper = ((SequentialGlobalAirPlugin) plugin).getPrepHelper();
      Logger logger = ((SequentialGlobalAirPlugin) plugin).getLogger();

      Task task = alloc.getTask();
      Asset directObject = task.getDirectObject();

      if (!glmPrepHelper.getFromLocation (task).getGeolocCode ().equals (glmPrepHelper.getToLocation (task).getGeolocCode ()))
	logger.info ("SequentialGlobalAirPlugin.finishPlan - WARNING - start = end time for task " +
		     task.getUID() + " asset " + directObject.getUID() + 
		     " from " + glmPrepHelper.getFromLocation (task) + 
		     " != to "   + glmPrepHelper.getToLocation (task));
    }
  }

  protected static class TheaterPortion extends ElementBase {
    public TheaterPortion(Task parent) {
      super(parent);
    }

    public String toString () { return "TheaterPortion"; }
	  
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Logger logger = plugin.getLogger();
      UTILExpand expandHelper = plugin.getExpandHelper();
      GLMPrepPhrase glmPrepHelper = plugin.getPrepHelper();

      if (logger.isDebugEnabled())
	logger.debug (".TheaterPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						  parentTask,
						  parentTask.getDirectObject(),
						  myPlugin.publicGetMessageAddress());
      new_task.setContext(parentTask.getContext());

      // find the theater org
			
      Organization theater = plugin.findOrgWithRole(GLMTransConst.THEATER_MCC_ROLE);
      if (theater == null) {
	logger.error(".TheaterPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.THEATER_MCC_ROLE);
	return null;
      }

      // if we've got an air move, find the nearest airbase, otherwise, just do ground move from from loc
      Location PODLocation = 
	(plugin.startsInTheater(parentTask)) ? glmPrepHelper.getFromLocation(parentTask) : plugin.getPOD (parentTask);

      glmPrepHelper.replacePrepOnTask(new_task, 
				      glmPrepHelper.makePrepositionalPhrase(plugin.publicGetFactory(),
									    Constants.Preposition.FROM,
									    PODLocation));
      if (logger.isDebugEnabled())
	logger.debug (".TheaterPortion.planMe - created leg FROM " + PODLocation + 
		      " TO " + glmPrepHelper.getToLocation(new_task));

      glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE);
      glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE_DISTANCE);
            
      PlanElement pe = plugin.getAllocHelper().makeAllocation(myPlugin,
							      myPlugin.publicGetFactory(), 
							      myPlugin.publicGetRealityPlan(),
							      new_task,
							      theater,   
							      plugin.getPrefHelper().getReadyAt(new_task),
							      plugin.getPrefHelper().getBestDate(new_task),
							      plugin.getAllocHelper().MEDIUM_CONFIDENCE,
							      Constants.Role.TRANSPORTER);
      myPlugin.publicPublishAdd(pe);
      return new_task;
    }
  }

  /** comment this back in whenever we want to revivify the ISB planning */
  /*
  protected class IsbPortion extends ElementBase {
    protected double myMaxCost = BILLION;
        
    public IsbPortion(Task parent) {
      super(parent);
    }

    public String toString () { return "IsbPortion"; }
        
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Logger logger = plugin.getLogger();
      UTILExpand expandHelper = plugin.getExpandHelper();
      GLMPrepPhrase glmPrepHelper = plugin.getPrepHelper();
      if (logger.isDebugEnabled())
	logger.debug (".IsbPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						  parentTask,
						  parentTask.getDirectObject(),
						  myPlugin.publicGetMessageAddress());
            
      // find the ISB org
      Organization isb = plugin.findOrgWithRole(GLMTransConst.C130_ROLE);
      if (isb == null) {
	logger.error(".IsbPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.C130_ROLE);
	return null;
      }

      Organization theater = plugin.findOrgWithRole(GLMTransConst.THEATER_MCC_ROLE);
      if (theater == null) {
	logger.error(".TheaterPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.THEATER_MCC_ROLE);
	return null;
      }

      Date end = null;

      if (getDependencies().isEmpty()) { // if there was no theater leg
	end = prefHelper.getLateDate(parentTask);
	if (logger.isDebugEnabled())
	  logger.debug (".IsbPortion.planMe - no dependencies, end preference is " +
			end);
      } else {
	end = ((SequentialScheduleElement)getDependencies().elementAt(0)).getStartDate();
	prefHelper.replacePreference(new_task, 
				     prefHelper.makeEndDatePreference(myPlugin.publicGetFactory(), end));
	if (logger.isDebugEnabled())
	  logger.debug (".IsbPortion.planMe - dependent on theater, end preference is " +
			end);
      }

      Location isbLocation = plugin.getISBLocation(end); // doesn't matter when
      Location PODLocation = plugin.getPOD (parentTask);
            
      glmPrepHelper.replacePrepOnTask(new_task, 
				      glmPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.FROM,
									    isbLocation));
      glmPrepHelper.replacePrepOnTask(new_task, 
				      glmPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.TO,
									    PODLocation));

      glmPrepHelper.removePrepNamed(new_task, GLMTransConst.MODE);

      new_task.addPreference (prefHelper.makeCostPreference (myPlugin.publicGetFactory(), myMaxCost));

      List aspects = new ArrayList ();
      aspects.add (AspectValue.newAspectValue (AspectType.START_TIME, 
				    (double) prefHelper.getReadyAt(new_task).getTime ()));
      aspects.add (AspectValue.newAspectValue (AspectType.END_TIME,   
				    (double) end.getTime ()));
      aspects.add (AspectValue.newAspectValue (AspectType.COST, 0));

      PlanElement c130_alloc = plugin.getAllocHelper().makeAllocation (myPlugin,
								       myPlugin.publicGetFactory(),
								       myPlugin.publicGetRealityPlan(),
								       new_task,
								       isb,
								       (AspectValue []) aspects.toArray (new AspectValue[0]), 
								       plugin.getAllocHelper().MEDIUM_CONFIDENCE,
								       Constants.Role.TRANSPORTER);
      myPlugin.publicPublishAdd(c130_alloc);
      return new_task;
    }
  }
  */

  protected static class AirPortion extends ElementBase {
    public AirPortion(Task parent) {
      super(parent);
    }
        
    public String toString () { return "AirPortion"; }
        
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Logger logger = plugin.getLogger();
      UTILExpand expandHelper = plugin.getExpandHelper();
      GLMPrepPhrase glmPrepHelper = plugin.getPrepHelper();

      if (logger.isDebugEnabled())
	logger.debug (".AirPortion.planMe - starting planning.");

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						  parentTask,
						  parentTask.getDirectObject(),
						  myPlugin.publicGetMessageAddress());
      new_task.setContext(parentTask.getContext());

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

      Location [] locations = plugin.getPOEandPOD (parentTask, new_task);
      Location fromLocation = locations[0];
      Location toLocation   = locations[1];

      Date end = null;
      Date early, best;

      if (getDependencies().isEmpty()) { // if there was neither a theater nor a isb leg
	end = plugin.getPrefHelper().getLateDate(parentTask);
	best = end;
      } else {
	SequentialScheduleElement sse = (SequentialScheduleElement)getDependencies().elementAt(0);
	end = sse.getStartDate();
	//	early = plugin.getPrefHelper().getReadyAt(new_task);
	best  = new Date(end.getTime() - plugin.bestDateBackoff);
	early = plugin.getEarlyArrivalMiddleStep (new_task, best);
	if (best.getTime () < early.getTime())
	  best = early;
			  
	plugin.getPrefHelper().replacePreference(new_task, 
						 plugin.getPrefHelper().makeEndDatePreference(myPlugin.publicGetFactory(), 
											      early,best,end));
	GeolocLocation theaterLegFrom = plugin.getPrepHelper ().getFromLocation (sse.getTask());

	if (!theaterLegFrom.getGeolocCode ().equals (((GeolocLocation)toLocation).getGeolocCode()))
	  plugin.getLogger().error (" - Theater leg task " + sse.getTask().getUID () + 
				    " FROM " + theaterLegFrom + " is not equal to air/sea TO " + toLocation);

	// if (dependsOnISB)
	//	toLocation = plugin.getISBLocation(end); // doesn't matter when
      }

      if (logger.isDebugEnabled())
	logger.debug (".AirPortion.planMe - created leg FROM " + fromLocation + 
		     " TO " + toLocation);

      glmPrepHelper.replacePrepOnTask(new_task, 
				      glmPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.FROM,
									    fromLocation));

      glmPrepHelper.replacePrepOnTask(new_task, 
				      glmPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.TO,
									    toLocation));
            
      Organization middleOrg = plugin.findOrgForMiddleStep ();
			
      List aspects = new ArrayList ();
      aspects.add (AspectValue.newAspectValue (AspectType.START_TIME, 
				    (double) plugin.getPrefHelper().getReadyAt(new_task).getTime ()));
      aspects.add (AspectValue.newAspectValue (AspectType.END_TIME,   
				    (double) best.getTime ()));

      plugin.removePrepsFromMiddleStep (new_task);

      PlanElement pe = plugin.getAllocHelper().makeAllocation(myPlugin,
							      myPlugin.publicGetFactory(), 
							      myPlugin.publicGetRealityPlan(),
							      new_task, 
							      middleOrg,
							      (AspectValue []) aspects.toArray (new AspectValue[0]), 
							      plugin.getAllocHelper().MEDIUM_CONFIDENCE,
							      Constants.Role.TRANSPORTER);

      myPlugin.publicPublishAdd(pe);
      return new_task;
    }
        
  }

  protected Date getEarlyArrivalMiddleStep (Task task, Date best) {
    return prefHelper.getReadyAt(task);
  }

  protected void removePrepsFromMiddleStep (Task new_task) {
    glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE);
    glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE_DISTANCE);
  } 

  protected static class ConusPortion extends ElementBase {
    public ConusPortion(Task parent) {
      super(parent);
    }

    public String toString () { return "ConusPortion"; }
        
    public Task planMe(SequentialPlannerPlugin myPlugin) {
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Logger logger = plugin.getLogger();
      UTILExpand expandHelper = plugin.getExpandHelper();
      GLMPrepPhrase glmPrepHelper = plugin.getPrepHelper();
      Location toLocation;
      Date end = null;

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						  parentTask,
						  parentTask.getDirectObject(),
						  myPlugin.publicGetMessageAddress());
      new_task.setContext(parentTask.getContext());

      if (!getDependencies().isEmpty()) {
	SequentialScheduleElement sse = (SequentialScheduleElement)getDependencies().elementAt(0);
	Task dependingTask = sse.getTask ();
	end = sse.getStartDate();
	toLocation = glmPrepHelper.getFromLocation (dependingTask);
      }
      else {
	end = plugin.getPrefHelper().getLateDate(new_task);
	toLocation = plugin.getLocator().getPOENearestToFromLoc (parentTask);
      }
			
      Date early = plugin.getPrefHelper().getReadyAt(new_task);
      Date best  = new Date(end.getTime() - 1000);
			  
      plugin.getPrefHelper().replacePreference(new_task, 
					       plugin.getPrefHelper().makeEndDatePreference(myPlugin.publicGetFactory(), 
											    early,best,end));

      glmPrepHelper.replacePrepOnTask(new_task, 
				      glmPrepHelper.makePrepositionalPhrase(myPlugin.publicGetFactory(),
									    Constants.Preposition.TO,
									    toLocation));
      if (logger.isDebugEnabled())
	logger.debug (".ConusPortion.planMe - created leg FROM " + glmPrepHelper.getFromLocation (new_task) + 
		      " TO " + toLocation);

      Organization conusGround = plugin.findOrgWithRole(GLMTransConst.CONUS_GROUND_ROLE);
      if (conusGround == null) {
	logger.error(".ConusPortion.planMe - ERROR - No subordinate with role " + 
		     GLMTransConst.CONUS_GROUND_ROLE);
	return null;
      }

      glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE);
      glmPrepHelper.removePrepNamed(new_task, GLMTransConst.SEAROUTE_DISTANCE);

      PlanElement pe = plugin.getAllocHelper().makeAllocation(myPlugin,
							      myPlugin.publicGetFactory(), 
							      myPlugin.publicGetRealityPlan(),
							      new_task,
							      conusGround,    // allocate to conus ground
							      plugin.getPrefHelper().getReadyAt(new_task),
							      end,
							      plugin.getAllocHelper().MEDIUM_CONFIDENCE,
							      Constants.Role.TRANSPORTER);
      myPlugin.publicPublishAdd(pe);
      return new_task;
    }

    public void finishPlan (Allocation alloc, SequentialPlannerPlugin myPlugin) {
      SequentialGlobalAirPlugin plugin = (SequentialGlobalAirPlugin) myPlugin;
      Logger logger = plugin.getLogger();

      if (logger.isDebugEnabled()) 
	logger.debug(".ConusPortion.finishPlan - Planning complete.");
      super.finishPlan (alloc, plugin);
    }
  }

  /** supports finding an ISB at a certain time -- assumes it can move! */
  /*
  Location getISBLocation (Date when) {
    Organization isb = findOrgWithRole (GLMTransConst.C130_ROLE);
    Location loc = (Location) airportToLocation.get (isb);
    if (loc == null) {
      loc = findLocationAtTime (isb, when.getTime());
      airportToLocation.put (isb, loc);
    }
    return loc;
  }
  */

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
    
    locs[0] = locator.getPOENearestToFromLoc(parentTask);
    locs[1] = getPOD(parentTask);
    
    return locs;
  }

  /** 
   * Uses locator to get POD
   **/
  Location getPOD (Task task) {
    /*
    Location POD =locator.getNearestLocation(glmPrepHelper.getToLocation(task));
    System.out.println ("SeqGlobalAir.getPOD - chose POD " + POD + 
			" nearest to " + glmPrepHelper.getToLocation(task) + " from among " +
			((LocatorImpl)locator).myLocations);
    */
    return (Location) locator.getNearestLocation(glmPrepHelper.getToLocation(task));
  }

  Location getPOENearestToFromLocMiddleStep (Task parentTask) {
    return locator.getPOENearestToFromLoc (parentTask);
  }
  
  /** 
   * cache role to org mapping <p>
   *
   * Supports spawning -- choosing among several possible subordinates with role 
   * <tt>role</tt>. <p>
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

  public GLMPrepPhrase  getPrepHelper   () { return glmPrepHelper;  }
  public AssetUtil      getAssetHelper  () { return glmAssetHelper; }
  public UTILExpand     getExpandHelper () { return expandHelper; }
  public UTILPreference getPrefHelper   () { return prefHelper; }
  public UTILAllocate   getAllocHelper  () { return allocHelper; }
  public Logger         getLogger       () { return logger; }
  public Locator        getLocator      () { return locator; }

  protected GLMOrganizationCallback myOrgCallback;
    
  protected Map roleToOrg = new HashMap ();
  protected Locator locator;

  protected long bestDateBackoff;
  boolean shouldRefreshOrgList = false;
  private Random r = new Random();

  protected transient GLMPrepPhrase glmPrepHelper;
  protected transient AssetUtil     glmAssetHelper;
}
