
/*
 * <copyright>
 * LEGEND
 * Use, duplication, or disclosure by the Government is as set forth in the
 * Rights in technical data noncommercial items clause DFAR 252.227-7013 and
 * Rights in noncommercial computer software and noncommercial computer
 * software documentation clause DFAR 252.227-7014.
 *
 * Copyright 2003 BBNT Solutions LLC
 * </copyright>
 */

package org.cougaar.logistics.plugin.strattrans;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;


import org.cougaar.glm.ldm.Constants;

import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.Person;
import org.cougaar.glm.ldm.asset.ClassVIIMajorEndItem;
import org.cougaar.glm.ldm.asset.MovabilityPG;

import org.cougaar.planning.plugin.util.ExpanderHelper;

import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;

import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.oplan.TimeSpan;

import org.cougaar.util.ShortDateFormat;

import java.util.Date;

/**
 * Class <code>StrategicTransportExpanderPlugin</code> is a replacement
 * for <code>StrategicTranportProjectorPlugin</code>.
 * <p>
 * This class subscribes to the single "Deploy" "DetermineRequirements" 
 * Task and expands it to "Transport" Tasks for all applicable assets.
 * <p>
 * Currently expects only one oplan, one "self" org activity, and one
 * "Deploy" "DetermineRequirements" task.
 * <p>
 * Debug information is now off by default.  See method <code>setDebug()</code>
 */

public class StrategicTransportExpanderPlugin extends UTILExpanderPluginAdapter 
    implements UTILOrgActivityListener, UTILOrganizationListener, UTILParameterizedAssetListener {


  /**
   * Provide the callback that is paired with the buffering thread, which is a
   * listener.  The buffering thread is the listener to the callback
   *
   *  overridden to use UTILExpandableChildTaskCallback to allow
   *    tasks that are child of another task (specifically required for 
   *    Strat-Trans Deter-Req tasks, which are child of GLS)
   *
   * @return an UTILExpandableChildTaskCallback with the buffering thread as its listener
   * @see org.cougaar.lib.callback.UTILWorkflowCallback
   */
  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) { 
    if (isInfoEnabled())
      logger.info(getAgentIdentifier() + " : Filtering for Expandable Tasks...");
    myInputTaskCallback = new UTILExpandableChildTaskCallback (bufferingThread, logger);
    return myInputTaskCallback;
  } 

  public void redoTasks() {

    SubscriptionResults subscriptionResults = checkSubscriptions();
    if (subscriptionResults != null) {

      Enumeration enum = myInputTaskCallback.getSubscription().elements();
      boolean alreadyFound = false;

      while (enum.hasMoreElements()) {
          if (alreadyFound)
              logger.warn(getAgentIdentifier() + " - more than one determine requirements task!");
          Task t = (Task) enum.nextElement();
          alreadyFound = true;
          if (t.getPlanElement() == null) {
              logger.info(getAgentIdentifier() + " - redoTasks: Null Plan Element to be filled");
              handleTask(t, subscriptionResults);
          }
          else {
              logger.info(getAgentIdentifier() + " - redoTasks: Plan Element to be removed and filled.");
              publishRemove(t.getPlanElement());
              handleTask(t, subscriptionResults);
          }
      }
    }
  }

  public void handleNewOrganizations (Enumeration e) {
      redoTasks();
  }

  public void handleNewParameterizedAssets (Enumeration e, String key) {
      redoTasks();
  }

  public void handleNewOrgActivities (Enumeration e) {
      redoTasks();
  }

  public void handleChangedOrganizations (Enumeration e) {
      redoTasks();
  }

  public void handleChangedParameterizedAssets (Enumeration e, String key) {
      redoTasks();
  }

  public void handleChangedOrgActivities (Enumeration e) {
      redoTasks();
  }


  // extending UTILExpanderPluginAdapter gets me a callback that listens to the types of tasks specified in "interestingTask" (see the method UTILExpanderPluginAdapter.createThreadCallback, which is called by setupFilters of its parent class UTILBufferingPluginAdapter)
  // extending UTILExpanderPluginAdapter gets me a callback that listens to the types of tasks specified in "interestingExpandedTask" (see the method UTILExpanderPluginAdapter.setupFilters)
  // by default, from UTILExpanderPluginAdapter.interestingExpandedTask, the interestingExpandedTask is also the interestingTask

  protected UTILOrgActivityCallback myDeploymentOrgActivityCallback;
  protected UTILOrganizationCallback mySelfOrganizationCallback;
  protected UTILParameterizedAggregateAssetCallback myPersonAssetCallback;
  protected UTILParameterizedAggregateAssetCallback myMEIAssetCallback;

  /*
   * <pre>
   * The idea is to add subscriptions (via the filterCallback), and when 
   * they change, to have the callback react to the change, and tell 
   * the listener (many times the plugin) what to do.
   *
   * </pre>
   * @see #createOrgActivityCallback
   * @see #createOrganizationCallback
   * @see #createParameterizedAggregateAssetCallback
   */
  public void setupFilters () {
    super.setupFilters ();

    addFilter (myDeploymentOrgActivityCallback = createOrgActivityCallback());
    addFilter (mySelfOrganizationCallback = createOrganizationCallback());
    addFilter (myPersonAssetCallback = createParameterizedAggregateAssetCallback("Person", false));
    addFilter (myMEIAssetCallback = createParameterizedAggregateAssetCallback("ClassVIIMajorEndItem", true));
  }


    /*-----------------------------------------------------------------
     * 
     *   Task methods
     *
     *-----------------------------------------------------------------
     */

  /**
   * State that we are interested in all DETERMINEREQUIREMENT tasks of type Asset
   * where the asset type is StrategicTransportation.
   * @param task the task to test.
   * @return true if the tasks verb is DetermineRequirements with the preposition "StrategicTransportation", false otherwise
   */
  public boolean interestingTask(Task task){
    if (task.getVerb().equals (Constants.Verb.DetermineRequirements)) {
        return (ExpanderHelper.isOfType(task, Constants.Preposition.OFTYPE,
                     "StrategicTransportation"));
    }
    return false;
  }


    /*-----------------------------------------------------------------
     * 
     *   Expanded task methods
     *
     *-----------------------------------------------------------------
     */

	// by default the same DETERMINEREQUIREMENT tasks of type StrategicTransportation

    /*-----------------------------------------------------------------
     * 
     *   Organization methods
     *
     *-----------------------------------------------------------------
     */

  /**
   * create the organization callback
   */
  protected UTILOrganizationCallback createOrganizationCallback () { 
    if (isInfoEnabled())
      warn (getName () + " : Filtering for Organizations...");
        
    UTILOrganizationCallback cb = new UTILOrganizationCallback (this, logger); 
    return cb;
  }

  /** Interested in listening to self-organization (from interface UTILOrganizationListener,
   * and linked to UTILOrganizationCallback)
   */
  public boolean interestingOrganization(Organization org) {
    return (org.isSelf());
  }


    /*-----------------------------------------------------------------
     * 
     *   Org Activity methods
     *
     *-----------------------------------------------------------------
     */

  /**
   * create the org activity callback
   */
  protected UTILOrgActivityCallback createOrgActivityCallback () { 
    if (isInfoEnabled())
      warn (getName () + " : Filtering for Org Activities...");
        
    UTILOrgActivityCallback cb = new UTILOrgActivityCallback (this, logger); 
    return cb;
  }

  /** Interested in listening to deployment org-activities (from interface
   *   UTILOrgActivityListener, and linked to UTILOrgActivityCallback)
   */
  public boolean interestingOrgActivity(OrgActivity orgAct) {
    return (orgAct.getActivityType().equals("Deployment"));
  }

    /*-----------------------------------------------------------------
     * 
     *   Asset (Person/ClassVIIMajorEndItem) methods
     *
     *-----------------------------------------------------------------
     */

  /**
   * create the parameterized aggregate asset callback
   */
  protected UTILParameterizedAggregateAssetCallback createParameterizedAggregateAssetCallback (String key, boolean isDynamic) { 
    if (isInfoEnabled()) {
      if (isDynamic)
          System.out.println (getName () + " : Filtering for Asset/Aggregate Assets (that are dynamic) based on key: " + key + "...");
      else
          System.out.println (getName () + " : Filtering for Asset/Aggregate Assets (that are static) based on key: " + key + "...");
    }
        
    UTILParameterizedAggregateAssetCallback cb = new UTILParameterizedAggregateAssetCallback (this, logger, key, isDynamic); 
    return cb;
  }

  /** Interested in listening to Person/ClassVIIMajorEndItem assets
   *
   * See bug 2998 in bugzilla.
   *
   * In order to not strat trans move Level2MEIs we filter out equipment with the 
   * cargo cat code of "000" which means phantom equipment.  The
   * phantom equipment corresponds to Level2MEIs.   The cargo cat code is put
   * onto the Level2MEIs retroactively after they have been made that is the
   * reason for the DynamicUnaryPredicate (i.e., callback created with isDynamic true)
   */
  public boolean interestingParameterizedAsset(Asset o, String key) {
    if (key.equals("Person")) {
        return (o instanceof Person);
    }
    else if (key.equals("ClassVIIMajorEndItem")) {
        if(o instanceof ClassVIIMajorEndItem) {
            ClassVIIMajorEndItem asset = (ClassVIIMajorEndItem) o;
            MovabilityPG moveProp = asset.getMovabilityPG();
            if((moveProp != null) &&
               (moveProp.getCargoCategoryCode() != null) &&
               (moveProp.getCargoCategoryCode().equals("000"))) {
                return false;
            }
            return true;
        }
    }
    return false;
  }

  /** helper function
   */
  protected String getOrgID(Organization org) {
    try {
      return org.getClusterPG().getMessageAddress().toString();
    } catch (Exception e) {
      return null;
    }
  }

  /** return "self" organization in mySelfOrganizationCallback bucket
   */
  protected Organization getSelfOrg() {
      Enumeration selfOrgsElements = mySelfOrganizationCallback.getSubscription().elements();
      Organization selfOrg = null;

      while (selfOrgsElements.hasMoreElements()) {
          Organization org = (Organization) selfOrgsElements.nextElement();
          if (selfOrg != null) {
              logger.error (getAgentIdentifier() + " - Expecting only one \"SELF\" Organization! Already have " 
              + selfOrg + " with self-id: " + getOrgID(selfOrg) + " >> ignoring org with id: " + getOrgID(org));
              continue;
          }
          selfOrg = org;
      }
      if (selfOrg == null) 
         logger.info (getAgentIdentifier() + " - Expecting a \"SELF\" Organization!   Expansion will fail");

      return selfOrg;
  }

  /** return orgActivity with matching id (in myDeploymentOrgActivityCallback bucket) 
   */
  protected OrgActivity getOrgActivity(String id) {
      Enumeration orgActivitiesElements = myDeploymentOrgActivityCallback.getSubscription().elements();
      OrgActivity matchingOrgActivity = null;

      while (orgActivitiesElements.hasMoreElements()) {
          OrgActivity orgAct = (OrgActivity) orgActivitiesElements.nextElement();
          if (id.equals(orgAct.getOrgID())) {
              matchingOrgActivity = orgAct; // found matching org activity 
              break;
          }
      }
      if (matchingOrgActivity == null) 
         logger.info (getAgentIdentifier() + " - Expecting an org Activity with id " + id + "!   Expansion will fail");

      return matchingOrgActivity;
  }
      
  /** return Vector of Person assets from (in myPersonAssetCallback bucket)
   */
  protected Vector getPersonAssets() {
      return new Vector(myPersonAssetCallback.getSubscription().getCollection());
  }

  /** return Vector of ClassVIIMajorEndItem assets from (in myMEIAssetCallback bucket)
   */
  protected Vector getMEIAssets() {
      return new Vector(myMEIAssetCallback.getSubscription().getCollection());
  }

  class SubscriptionResults {
      Organization selfOrg = null;
      String selfOrgID = null;
      OrgActivity selfDeployOrgActivity = null;
      Vector personAssets = null;
      Vector MEIAssets =  null;
  }

  /** Returns a structure containing all the information extracted from the blackboard.
   *    Returns null if any key information is missing.
   */
  public SubscriptionResults checkSubscriptions() {
      SubscriptionResults results = new SubscriptionResults();
      results.selfOrg = getSelfOrg();
      if (results.selfOrg == null)
          return null; // if no self org, not valid

      results.selfOrgID = getOrgID(results.selfOrg);
      if (logger.isInfoEnabled()) 
          logger.info (getAgentIdentifier() + " - Found Self: " + results.selfOrgID);

      results.selfDeployOrgActivity = getOrgActivity(results.selfOrgID);
      if (results.selfDeployOrgActivity == null)
          return null;  // if no deployment activity, not valid

      results.personAssets = getPersonAssets();
      results.MEIAssets = getMEIAssets();
      if ( (results.personAssets == null || results.personAssets.size() == 0) && 
           (results.MEIAssets == null || results.MEIAssets.size() == 0) )
          return null;  // if nothing to transport, not valid

      return results;
  }  

  /** 
   * <pre>
   * Implemented for UTILGenericListener interface
   *
   * This method Expands the given Task and publishes the PlanElement,
   *   but only if subtasks have been created.
   *
   * The method expandTask should be implemented by child classes.
   * </pre>
   * @param t the task to be expanded.
   */
  public void handleTask(Task t) {

    if (isDebugEnabled())
      debug (getName () + 
	     ".handleTask : called on - " + t.getUID());

    // Need special handling here in case the getSubtasks returns no subtasks
    //   This is possible if not all subscriptions have been "filled"
    Vector subtasks = getSubtasks(t);

    if (subtasks.size() > 0)
        expand.handleTask(ldmf, 
			 getBlackboardService(), 
			 getName(),
			 wantConfidence, 
			 t, 
			 subtasks);
  }

  /** overloaded for efficient checking of subscriptions */
  public void handleTask(Task t, SubscriptionResults subscriptionResults) {

    if (isDebugEnabled())
      debug (getName () + 
	     ".handleTask : called on - " + t.getUID());

    // Need special handling here in case the getSubtasks returns no subtasks
    //   This is possible if not all subscriptions have been "filled"
    Vector subtasks = getSubtasks(t, subscriptionResults);

    if (subtasks.size() > 0)
        expand.handleTask(ldmf, 
			 getBlackboardService(), 
			 getName(),
			 wantConfidence, 
			 t, 
			 subtasks);
  }

    
  /**
   * Expands the task using information from several different subscriptions
   *   If the key subscriptions have not been filled, as determined by checkSubscriptions(),
   *   then returns an empty Vector, which must be handled appropriately by higher levels.
   *
   * @param task the task to expand
   * @return a vector with the expanded subtask
   */
  public Vector getSubtasks(Task task){
      Vector subtasks = new Vector();

      // ==== check buckets to make sure have information required from subcriptions to blackboard

      SubscriptionResults subscriptionResults = checkSubscriptions();

      if (subscriptionResults == null) {
          return subtasks;
      }

      return getSubtasks(task, subscriptionResults);
  }

  /** overloaded for efficient checking of subscriptions */
  public Vector getSubtasks(Task task, SubscriptionResults subscriptionResults){
      Vector subtasks = new Vector();

      // PREPOSITIONS ====== extract information from buckets and create prepositions

      Vector prepositions = new Vector();

      // 1.  --------- get FROM geographic location from selfOrg

      //   this is taken from the MilitaryOrgPG
      GeolocLocation fromLoc = null;
      org.cougaar.glm.ldm.asset.MilitaryOrgPG milPG = subscriptionResults.selfOrg.getMilitaryOrgPG();
      if (milPG != null)
	      fromLoc = (GeolocLocation) milPG.getHomeLocation();

      prepositions.add(prepHelper.makePrepositionalPhrase(ldmf,
                                                        Constants.Preposition.FROM,
                                                        fromLoc));

      // 2.  --------- get TO geographic location from selfDeployOrgActivity
      GeolocLocation toLoc = subscriptionResults.selfDeployOrgActivity.getGeoLoc();

      prepositions.add(prepHelper.makePrepositionalPhrase(ldmf,
                                                        Constants.Preposition.TO,
                                                        toLoc));

      // 3. ---------- FOR (me) preposition based on selfOrg
      prepositions.add(prepHelper.makePrepositionalPhrase(ldmf,
                                                        Constants.Preposition.FOR,
                                                        subscriptionResults.selfOrg.getItemIdentificationPG().getItemIdentification()));


      // 4. ---------- OFTYPE  (StrategicTransport)
      AbstractAsset strans = null;
      /* May be inefficient due to repeated use of Class.forName() */
      try {
        Asset strans_proto = ldmf.createPrototype(
           Class.forName( "org.cougaar.planning.ldm.asset.AbstractAsset" ),
           "StrategicTransportation" );
        strans = (AbstractAsset) ldmf.createInstance( strans_proto );
      } catch (Exception exc) {
        logger.error (getAgentIdentifier() + " - Unable to create abstract strategictransport\n"+exc);
      }

      prepositions.add(prepHelper.makePrepositionalPhrase(ldmf,
                                                        Constants.Preposition.OFTYPE,
                                                        strans));



     // PREFERENCES ====== extract information from buckets and create preferences

      Vector preferences = new Vector();

      // 1. ---------- get start and end time

      // get start/end time
      TimeSpan actTS = subscriptionResults.selfDeployOrgActivity.getTimeSpan();
      Date thruTime = null, startTime = null;
      if (actTS != null) {
        // do we want to fix dates to be after System time?
    	// startDate will be null if OffsetDays days wasn't an command line parameter
        thruTime = actTS.getEndDate();
    	startTime = actTS.getStartDate();
      }
      
      preferences.add(prefHelper.makeStartDatePreference(ldmf, startTime));
      //preferences.add(prefHelper.makeEndDatePreference(ldmf, thruTime));


      //  Slight hack based upon logic approved by Jeff Berliner
      //     - best time to arrive is 1 day earlier than end time, 
      //        and no earlier than 5 days earlier than end time (or actual starttime, whichever is greater)
      //
      //    This should be changed to use parameter for greater flexibility
      //   END DATE    (RDD - 5) <= RDD - 1 <= RDD

      Date lateEndDate = thruTime;
      Date bestEndDate = ShortDateFormat.adjustDate(lateEndDate, 0, -1);
      if (bestEndDate.getTime() < startTime.getTime())  // boundary correction
          bestEndDate = startTime;
      Date earlyEndDate = ShortDateFormat.adjustDate(lateEndDate, 0, -5);
      if (earlyEndDate.getTime() < startTime.getTime()) // boundary correction
          earlyEndDate = startTime;

      preferences.add(prefHelper.makeEndDatePreference(ldmf, earlyEndDate, bestEndDate, lateEndDate));


     // CREATE SUBTASKS
     //   To reach here, must be at least one person or one MEI.
     //
     //  if only one in a given collection, then that is the directobject
     //  if more than one in given collection, create an assetgroup and add all to that group.

      Asset personDirectObject = null;

      // if size 0, then don't create subtask
      if (subscriptionResults.personAssets.size() > 0) {

        if (subscriptionResults.personAssets.size() > 1)
          personDirectObject = assetHelper.makeAssetGroup(ldmf,subscriptionResults.personAssets); // put everything into a single asset group
        else
          personDirectObject = (Asset) subscriptionResults.personAssets.get(0);


        NewTask newPersonSubtask = expandHelper.makeSubTask (ldmf,
						  task,
						  personDirectObject,
						  getAgentIdentifier());

        newPersonSubtask.setVerb(Constants.Verb.Transport);

        // ATTACH PREPS and PREFS to SUBTASK

        newPersonSubtask.setPrepositionalPhrases (prepositions.elements());
        newPersonSubtask.setPreferences(preferences.elements());

        // add new subtask to vector
        subtasks.add(newPersonSubtask);
      }

      Asset MEIDirectObject = null;

      // if size 0, then don't create subtask
      if (subscriptionResults.MEIAssets.size() > 0) {

        if (subscriptionResults.MEIAssets.size() > 1)
          MEIDirectObject = assetHelper.makeAssetGroup(ldmf,subscriptionResults.MEIAssets);
        else
          MEIDirectObject = (Asset) subscriptionResults.MEIAssets.get(0);


        NewTask newMEISubtask = expandHelper.makeSubTask (ldmf,
						  task,
						  MEIDirectObject,
						  getAgentIdentifier());

        // ATTACH PREPS and PREFS to SUBTASK

        newMEISubtask.setVerb(Constants.Verb.Transport);

        newMEISubtask.setPrepositionalPhrases (prepositions.elements());
        newMEISubtask.setPreferences(preferences.elements());

        // add new subtask to vector
        subtasks.add(newMEISubtask);
      }

    return subtasks;
  }

}

