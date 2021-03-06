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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

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
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
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

  protected TranscomDataXMLize dataXMLizer; // we need it for getOrganizationRole
  
  private static final long ONE_HOUR = 1000l*60l*60l;
  //private static final double BILLION = 1000000000.0d;
  //private static final String SAND = "SAND";
  public static int CONUS_THEATER_DIVIDING_LONGITUDE = 
    Integer.getInteger("SequentialGlobalAirPlugin.CONUS_THEATER_DIVIDING_LONGITUDE",25).intValue();
  public int numTheaterGroundAgents = 2;

  public void localSetup() {     
    super.localSetup();

    try {
      if (getMyParams().hasParam ("bestDateBackoff")) {
	bestDateBackoff = getMyParams().getLongParam("bestDateBackoff");
      }
      else {
	bestDateBackoff = ONE_HOUR; 
      }
      if (getMyParams().hasParam ("numTheaterGroundAgents")) {
	numTheaterGroundAgents = getMyParams().getIntParam("numTheaterGroundAgents");
      }
      else {
	numTheaterGroundAgents = 2;  // by default we run with the TRANSCOM-20 society
      }
    } catch (Exception e) { warn ("got really unexpected exception " + e); }

    if (isDebugEnabled())
      debug ("localSetup - Creating prep helper and asset helper.");

    if (isDebugEnabled())
      debug ("localSetup - this " + this + " prep helper " + glmPrepHelper);
    
    dataXMLizer = new TranscomDataXMLize(true, logger, new HashSet());
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
    for (;org_assets.hasMoreElements(); ) {
      info (getName() + " - got new org " + org_assets.nextElement());
    }
  }

  public void handleChangedOrganization (Enumeration org_assets) {}

  public void handleTask(Task t) {
    NewTask subtask = expandHelper.makeSubTask (ldmf, t, t.getDirectObject(), getAgentIdentifier());
    subtask.setContext (t.getContext());

    NewWorkflow wf  = ldmf.newWorkflow();
    wf.setParentTask(t);
    ((NewTask)subtask).setWorkflow(wf);
    ((NewTask)subtask).setParentTask(t);
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

  public Organization findOrgForMiddleStep (Task ignored) {
    Organization organicAir = findOrgWithRole(GLMTransConst.ORGANIC_AIR_ROLE);
    if (organicAir == null) {
      error(".findOrgForMiddleStep - ERROR - No subordinate with role " + 
	    GLMTransConst.ORGANIC_AIR_ROLE);
      return null;
    }
    return organicAir;
  }

  boolean warnedBefore = false;
  protected boolean allNecessaryAssetsReported () {
    Organization conusGround = findOrgWithRole(GLMTransConst.CONUS_GROUND_ROLE);
    if (conusGround == null) {
      if (isInfoEnabled())
	info ("conus ground subord is missing.");

      return false;
    }

    if (!allNecessaryAssetsReportedMiddleStep()) {
      if (isInfoEnabled())
	info ("middle (air/sea) subord is missing.");

      return false;
    }

    Organization theater = findOrgWithRole(GLMTransConst.THEATER_MCC_ROLE);
    if (theater == null) {
      if (isInfoEnabled())
	info ("theater subord is missing.");

      return false;
    }

    List orgs = (List) roleToOrg.get (GLMTransConst.THEATER_MCC_ROLE); // sets roleToOrg
    if (orgs.size () < getNumTheaterGroundAgents()) {
      if (isWarnEnabled() && !warnedBefore) {
	warn ("only " + orgs.size() + 
	      " theater subords have reported, expecting " + numTheaterGroundAgents +
	      ". " +
	      "Set numTheaterGroundAgents to 1 in GLMT.Transcom.env.xml in configs/glmtrans " +
	      "if running in TRANSCOM-7 configuration. " +
	      "2 is correct for the TRANSCOM-20 configuration.");
	warnedBefore = true;
      }

      shouldRefreshOrgList = true; // let's try again later -- see findOrgWithRole

      return false;
    }
      
    return true;
  }

  public int getNumTheaterGroundAgents () {
    Object ammoShipPacker = findOrgWithRole(GLMTransConst.AMMO_SHIP_PACKER_ROLE);
    if (getAgentIdentifier ().toString().toLowerCase().startsWith("ammo")) {
      return 1; // ammo sea only needs one theater ground agent - ammo theater ground
    }
    else {
      return numTheaterGroundAgents;
    }
  }

  /** overridden in sequentialglobalseaplugin */
  protected boolean allNecessaryAssetsReportedMiddleStep () {
    Organization middleOrg = findOrgWithRole(GLMTransConst.ORGANIC_AIR_ROLE);
    return (middleOrg != null);
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
            
      Organization middleOrg = plugin.findOrgForMiddleStep (new_task);
			
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
      Location toLocation = null;
      Date end = null;

      NewTask new_task = expandHelper.makeSubTask(myPlugin.publicGetFactory(),
						  parentTask,
						  parentTask.getDirectObject(),
						  myPlugin.publicGetMessageAddress());
      new_task.setContext(parentTask.getContext());

      if (!getDependencies().isEmpty()) {
	SequentialScheduleElement sse = (SequentialScheduleElement)getDependencies().elementAt(0);
	Task dependingTask = sse.getTask ();
	if (dependingTask == null) {
	  logger.error (" - problem with replanning earlier tasks - no task on schedule element " + sse);
	}
	else {
	  end = sse.getStartDate();
	  toLocation = glmPrepHelper.getFromLocation (dependingTask);
	}
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
      if (isInfoEnabled()) {
	info (getName () + " found " + matchingRels.size() + " matches for role " + role);
      }
      for (Iterator it = matchingRels.iterator(); it.hasNext();) {
	Relationship rel = (Relationship)it.next();
	Object other = sched.getOther(rel);
	if (isInfoEnabled()) {
	  info (getName () + " adding other " + other + " to known orgs for " + role);
	}
	orgs.add (other);

	if (isInfoEnabled()) {
	  info (getName () + " has " + orgs.size() + " orgs for role " + role);
	}
      }

      if (matchingRels.size () == 1) {
	Collection selfProviders = 
	  sched.getMatchingRelationships(Constants.RelationshipType.PROVIDER_SUFFIX,
					 TimeSpan.MIN_VALUE,
					 TimeSpan.MAX_VALUE);

	info (getName () + " self providers schedule was " + selfProviders);
	/*
	List otherWay = getOrgsWithRole (role);
	if (otherWay.size () > 1) {
	  info (getName () + " found more orgs by direct interrogation, so using them.");
	  orgs = otherWay;
	}
	*/
      }

      shouldRefreshOrgList = false;
    }
	
    if (orgs.isEmpty ()) {
      if (isInfoEnabled()) {
	info (getName() + ".findOrgWithRole - could not find any organizations with role " + 
	      role + " among subordinates -- relationship schedule is:\n" + 
	      getSelf().getRelationshipSchedule());
      }

      return null;
    }

    return chooseAmongOrgs (orgs);
  }

  /** 
   * thought this might help, but didn't - self org and individual org role schedules 
   * do in fact agree 
   */
  protected List getOrgsWithRole (Role role) {
    // send subscription contents through again...
    Collection assets = getOrganizationCallback().getSubscription ().getCollection();
    List orgs = new ArrayList ();

    for (Iterator iter = assets.iterator(); iter.hasNext(); ) {
      Asset asset = (Asset) iter.next();
      if (asset instanceof Organization) {
	String name = dataXMLizer.getOrganizationRole(asset);

	if (isInfoEnabled()) {
	  info (getName () + " examining " + asset + " for role " + role + " says it's " + name);
	}
	if (role.getName().equals(name)) {
	  if (isInfoEnabled()) {
	    info (getName () + " " + asset + " indeed has role " + role);
	  }

	  orgs.add (asset);
	}
      }
    }

    return orgs;
  }

  /**
   * re-examine the workflow to see if the any tasks overlap the time
   * of the recently changed allocation, if they do, replan tasks
   * that depend on it.
   */
  protected void replanDependingTasks (Task parentTask, long beforeTime) {
    Expansion exp = (Expansion) parentTask.getPlanElement();
    if (exp == null) {
      if (isInfoEnabled()) {
	info ("no expansion for " + parentTask.getUID() + " must be in middle of rescinds.");
      }

      return;
    }
      
    PrepositionalPhrase prep = prepHelper.getPrepNamed(parentTask, GLMTransConst.SequentialSchedule);
    Schedule sched = (Schedule) prep.getIndirectObject();
    Enumeration en = sched.getAllScheduleElements();
    boolean overlap = false;
    Set toReplan = new HashSet();

    while (en.hasMoreElements()) {
      SequentialScheduleElement spe = (SequentialScheduleElement)en.nextElement();
      String uid = "<NO TASK>";
      if (spe.getTask () != null)
	uid = spe.getTask ().getUID ().toString();

      if (isInfoEnabled ()) {
	info ("for task " + parentTask.getUID() + 
	      " spe task " + uid +
	      " spe planned " + spe.isPlanned () +
	      " spe end date " + spe.getEndDate () + 
	      " before time " + new Date(beforeTime));
      }

      if (spe.isPlanned ()) {
	for (Iterator iter = spe.getDependencies ().iterator (); iter.hasNext(); ) {
	  SequentialScheduleElement dependency = (SequentialScheduleElement)iter.next();

	  // |--- spe ---|               (overlap) 
	  //    |--- dep ---|
	  //   OR 
	  // |--- dep ---| |--- spe ---| (dependency should always be after!)

	  if (spe.overlapSchedule (dependency) || dependency.getEndTime () < spe.getStartTime()) {
	    if (isInfoEnabled ()) {
	      info ("for task " + parentTask.getUID() + " replanning spe at " + spe.getEndDate ());
	    }
	    toReplan.add (spe);
	  }
	}
      }
    }

    for (Iterator iter = toReplan.iterator(); iter.hasNext(); ) {
      SequentialScheduleElement replanSSE = (SequentialScheduleElement) iter.next();
      replanPortion (exp, replanSSE);
    }

    // let's replan!
    if (!toReplan.isEmpty()) {
      if (isInfoEnabled ()) {
	info ("got overlap of " + parentTask.getUID());
      }
      turnCrank (parentTask);
    }
  }

  protected void replanPortion (Expansion exp, SequentialScheduleElement spe) {
    handleRemovedAlloc ((Allocation) spe.getTask().getPlanElement());
    if (exp != null) { // fix for bug #13417
      try {
	NewWorkflow nw = (NewWorkflow)exp.getWorkflow ();
	if (nw != null)
	  nw.removeTask (spe.getTask());
	publishRemove (spe.getTask());
      } catch (IllegalArgumentException iae) {
	error (getName () + " - task " + spe.getTask().getUID () + 
	       " is not in workflow for task " + exp.getTask().getUID() + 
	       " - likely rescinds happening concurrently. Exception was " + iae);
      }
      publishChange (exp);
    }

    spe.unplan ();
    spe.setTask (null);
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
