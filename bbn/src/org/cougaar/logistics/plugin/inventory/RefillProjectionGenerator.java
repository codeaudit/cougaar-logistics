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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;


/** The Refill Projection Generator Module is responsible for generating 
 *  projection refill tasks.  These projections will be calculated by
 *  time shifting the projections from each customer and summing the
 *  results.
 *  Called by the Inventory Plugin when there is new projection demand.
 *  Uses the InventoryBG module to gather projected demand.
 *  Generates Refill Projection tasks 
 **/

public class RefillProjectionGenerator extends InventoryModule {

  private transient Organization myOrg = null;
  private transient String myOrgName = null;
  private transient GeolocLocation homeGeoloc = null;

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillProjectionGenerator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  /** Called by the InventoryPlugin to calculate new Refills.
   *  We only want to calculate new Refills for inventories that have changed
   *  because of projection changes.
   *  @param touchedInventories Inventories that have changed
   *  @param daysOnHand Number of DaysOnHand from the InventoryPolicy
   *  @param endOfLevelSix The day representing the end of the Level 6 window
   *   from the VariableTimeHorizon OperatingMode (knob)
   *  @param endOfLelelTwo  The day representing the end of the Level 2 window
   *   from the VariableTimeHorizon OperatingMode (knob)
   **/
  public void calculateRefillProjections(ArrayList touchedInventories, int daysOnHand,
                                         long endOfLevelSix, long endOfLevelTwo, 
					 RefillComparator theComparator) {
    // get the demand projections for each customer from bg
    // time shift the demand for each customer
    // sum the each customer's time shifted demand

    //get today to see where we are with respect to the VTH windows
    long today = inventoryPlugin.getCurrentTimeMillis();
    
    if (today < endOfLevelSix) {
      calculateLevelSixProjections(touchedInventories, daysOnHand, 
				   endOfLevelSix, theComparator);
      calculateLevelTwoProjections(inventoryPlugin.getInventories(), daysOnHand, 
                                   getTimeUtils().addNDays(endOfLevelSix, 1),
                                   endOfLevelTwo);
    }

    if ((today > endOfLevelSix) && (today < endOfLevelTwo)) {
      calculateLevelTwoProjections(inventoryPlugin.getInventories(), daysOnHand, 
                                   getTimeUtils().addNDays(endOfLevelSix, 1),
                                   endOfLevelTwo);
    } 
  }

  /** Calculate Projection Refills in level Six detail until the end of the
   *  Level 6 VTH window.
   *  @param touchedInventories  The Inventories that have changed wrt projections.
   *   We will only recalculate refill projections for these.
   *  @param daysOnHand DaysOnHand policy.
   *  @param endOfLevelSix The date representing the end of the Level 6 VTH window.
   **/
  private void calculateLevelSixProjections(ArrayList touchedInventories, 
                                            int daysOnHand, long endOfLevelSix, 
					    RefillComparator myComparator) {

    ArrayList refillProjections = new ArrayList();
    ArrayList oldProjections = new ArrayList();
    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      // clear out the old and new projections for the last Inventory
      refillProjections.clear();
      oldProjections.clear();
      Inventory anInventory = (Inventory) tiIter.next();
      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
        searchForPropertyGroup(LogisticsInventoryPG.class);
      // clear all of the projections
      oldProjections.addAll(thePG.clearRefillProjectionTasks());

      //start time is the start time of the inventorybg
      long startDay = thePG.getStartTime();
      int startBucket = thePG.convertTimeToBucket(startDay);
      int currentBucket = startBucket;
      int customerDemandBucket = thePG.convertTimeToBucket(getTimeUtils().
                                                           addNDays(startDay, daysOnHand));
      double projDemand = 0;
      double nextProjDemand = 0;
      int endOfLevelSixBucket = thePG.convertTimeToBucket(endOfLevelSix);
      
      //get the initial demand for the customer for startBucket + daysOnHand
      //Is there a better way to do this to avoid the duplication?
      projDemand = thePG.getProjectedDemand(customerDemandBucket);

      //move forward a bucket
      currentBucket = currentBucket + 1;
      customerDemandBucket = customerDemandBucket + 1;

      //Begin looping through currentBucket forward until you hit the end of
      // the level six boundary
      //possible boundary issue... is it '<' or '<=' ??
      while (currentBucket < endOfLevelSixBucket) {
        nextProjDemand = thePG.getProjectedDemand(customerDemandBucket);
        if (projDemand != nextProjDemand) {
          //if there's a change in the demand, create a refill projection
          Task refill = createProjectionRefill(thePG.convertBucketToTime(startBucket), 
					       thePG.convertBucketToTime(currentBucket -1),
					       projDemand, anInventory, thePG);
	  refillProjections.add(refill);
          //then reset the start bucket and the new demand 
          startBucket = currentBucket;
          projDemand = nextProjDemand;
        }
        //in either case bump current and customer forward a bucket.
        currentBucket = currentBucket + 1;
        customerDemandBucket = customerDemandBucket + 1;
       }
      // when we get to the end of the level six window create the last
      // projection task (if there is one)
      if (startBucket != currentBucket) {
        Task lastRefill = createProjectionRefill(thePG.convertBucketToTime(startBucket),
                               thePG.convertBucketToTime(currentBucket - 1),
                               projDemand, anInventory, thePG);
	refillProjections.add(lastRefill);
      }
      // send the new projections and the old projections to the Comparator
      // the comparator will rescind the old and publish the new projections
      myComparator.compareRefillProjections(refillProjections, oldProjections, 
					    anInventory);
    }
  }

  /** Calculate the Projection Refills in Level 2
   *  @param myInventories All of the Inventories for this supply Type.
   *  @param daysOnHand  The DaysOnHand value from the InventoryPolicy
   *  @param start The start time of the Level 2 VTH window
   *  @param endOfLevelTwo The end time of the Level 2 VTH window
   **/
  private void calculateLevelTwoProjections(Collection inventories, int daysOnHand, 
                                            long start, long endOfLevelTwo) {
      ArrayList myInventories = new ArrayList(inventories);
    //since all Inventories in the agent will use the same bucket size
    // get a token inventory to convert the start and end times to buckets
    // so that we don't have to deal with times and buckets for each inventory.
    LogisticsInventoryPG tokenPG = (LogisticsInventoryPG) ((Inventory)myInventories.get(0)).
      searchForPropertyGroup(LogisticsInventoryPG.class);

    int startBucket = tokenPG.convertTimeToBucket(start);
    int currentBucket = startBucket;
    int customerDemandBucket = tokenPG.convertTimeToBucket(getTimeUtils().
                                                           addNDays(start, daysOnHand));
    int endOfLevelTwoBucket = tokenPG.convertTimeToBucket(endOfLevelTwo);
    double projDemand = -1;

    //loop through the buckets until we reach the end of level two
    while (currentBucket < endOfLevelTwoBucket) {
      double combinedDailyDemand = 0;
      for (int i=0; i < myInventories.size(); i++) {
        Inventory inv = (Inventory) myInventories.get(i);
        // should we make a PG collection and keep it around???
        LogisticsInventoryPG thePG = (LogisticsInventoryPG) ((Inventory)myInventories.get(i)).
          searchForPropertyGroup(LogisticsInventoryPG.class);
        // how do we make aggregates??
        double rawdemand = thePG.getProjectedDemand(customerDemandBucket);
        // need physical pg so demand = rawdemand * tons or whatever
        //  combinedDailyDemand = combinedDailyDemand + demand;
      }
      //if its the first bucket, seed proj demand
      if (projDemand == -1) {
        projDemand = combinedDailyDemand;
      }
      //check the results for the Day.
      if (projDemand != combinedDailyDemand) {
        //if there's a change in the demand, create a refill projection
        createAggregateProjectionRefill(tokenPG.convertBucketToTime(startBucket), 
                                        tokenPG.convertBucketToTime(currentBucket - 1),
                                        tokenPG.getStartTime(), projDemand);
        //then reset the start bucket  and the new demand 
        startBucket = currentBucket;
        projDemand = combinedDailyDemand;
      }
      //if we create a refill or don't bump forward a bucket.
      currentBucket = currentBucket + 1;
    }
    // when we get to the end of the level two window create the last
    // projection task (if there is one)
    if (startBucket != currentBucket) {
      createAggregateProjectionRefill(tokenPG.convertBucketToTime(startBucket), 
                                      tokenPG.convertBucketToTime(currentBucket - 1),
                                      tokenPG.getStartTime(), projDemand);      
    }
  }      
 

  /** Make a Projection Refill Task and publish it to the InventoryPlugin.
   *  The InventoryPlugin will hook the task in to the proper workflow
   *  and publish it to the blackboard.
   *  @param start The start time for the Task
   *  @param end The end time for the Task
   *  @param demand The demandrate value of the task
   *  @param inv  The inventory Asset this Task is refilling.
   *  @param thePG  The Property Group of the Inventory Asset
   **/
  private Task createProjectionRefill(long start, long end,
                                      double demand, Inventory inv, 
                                      LogisticsInventoryPG thePG) {
    //create a projection refill task
    NewTask newRefill = inventoryPlugin.getRootFactory().newTask();
    newRefill.setVerb(Constants.Verb.ProjectSupply);
    newRefill.setDirectObject(inv);
    fillInTask(newRefill, start, end, thePG.getStartTime(), demand, thePG.getResource());
    return newRefill;
  }

  /** Create a Level 2 Projection Refill
   *  @param start The start time of the Task
   *  @param end The end time of the Task
   *  @param demand The total demand in terms of tons or volume
   **/
  private void createAggregateProjectionRefill(long start, long end, 
                                               long earliest, double demand) {
    //create a level two projection refill task
    NewTask newAggRefill = inventoryPlugin.getRootFactory().newTask();
    newAggRefill.setVerb(Constants.Verb.ProjectSupply);
    //need to create the asset representing this class of supply
    //physical pg needs to represent demand
    Asset asset = null;
    newAggRefill.setDirectObject(asset);
    fillInTask(newAggRefill, start, end, earliest, 0, asset);
    //TODO: publish the refill
    //inventoryPlugin.publishAggRefillTask(newAggRefill);
    //do we even apply this to a bg - to what inventory is this attached to??
  }

  /** Utility method to fill in task details
   *  @param newRefill The task to fill in
   *  @param start Start time for Task
   *  @param end End Time for Task
   *  @param qty Quantity Pref for Task
   *  @param asset Direct Object for Task
   **/
  private void fillInTask(NewTask newRefill, long start, long end, long earliest, 
                          double qty, Asset asset) {
    // create preferences
    Vector prefs = new Vector();
    Preference p_start,p_end,p_qty;
    p_start = createRefillTimePreference(start, earliest);
    p_end = createRefillTimePreference(end, earliest);
    p_qty = createRefillRatePreference(qty);
    prefs.add(p_start);
    prefs.add(p_end);
    prefs.add(p_qty);
    newRefill.setPreferences(prefs.elements());

    //create Prepositional Phrases
    Vector pp_vector = new Vector();
    pp_vector.add(createPrepPhrase(Constants.Preposition.FOR, getOrgName()));
    pp_vector.add(createPrepPhrase(Constants.Preposition.OFTYPE, 
                                   inventoryPlugin.getSupplyType()));
    
    Object io;
    Enumeration geolocs = getAssetUtils().getGeolocLocationAtTime(
								  getMyOrganization(),
								  end);
    if (geolocs.hasMoreElements()) {
      io = (GeolocLocation)geolocs.nextElement();
    } else {
      io = getHomeLocation();
    }
    pp_vector.addElement(createPrepPhrase(Constants.Preposition.TO, io));
    TypeIdentificationPG tip = asset.getTypeIdentificationPG();
    MaintainedItem itemID = MaintainedItem.
      findOrMakeMaintainedItem("Inventory", tip.getTypeIdentification(), 
                               null, tip.getNomenclature(), inventoryPlugin);
    pp_vector.add(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.add(createPrepPhrase(Constants.Preposition.REFILL, null));
    
    newRefill.setPrepositionalPhrases(pp_vector.elements());
  } 

  /** Create a Time Preference for the Refill Task
   *  USE V Scoring Function for now. Note that this doesn't exactly represent
   *  the scoring function we developed in our IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param start The earliest time this preference can have
   *  @return Preference The new Time Preference
   **/
  private Preference createRefillTimePreference(long bestDay, long start) {
    AspectValue beforeAV = new TimeAspectValue(AspectType.END_TIME, start);
    AspectValue bestAV = new TimeAspectValue(AspectType.END_TIME, bestDay);
    //TODO - really need end of deployment from an OrgActivity -
    // As a hack for now just add 180 days from today - note that this
    // will push the possible end date out too far...
    //AspectValue afterAV = new TimeAspectValue(AspectType.END_TIME, 
    //				    inventoryPlugin.getEndOfDeplyment()); 
    AspectValue afterAV = new TimeAspectValue(AspectType.END_TIME,
					      getTimeUtils().addNDays(start, 180));
    ScoringFunction endTimeSF = ScoringFunction.
	createVScoringFunction(beforeAV, bestAV, afterAV);
    return inventoryPlugin.getRootFactory().
	  newPreference(AspectType.END_TIME, endTimeSF);
  }

  /** Utility method to create Refill Projection Rate preference
   *  We use a V scoring function for this preference.
   *  @param refill_qty  The quantity we want for this Refill Task
   *  @return Preference  The new demand rate preference for the Refill Task
   **/
  private Preference createRefillRatePreference(double refill_qty) {
    AspectValue lowAV = new AspectValue(AlpineAspectType.DEMANDRATE, 0.01);
    AspectValue bestAV = new AspectValue(AlpineAspectType.DEMANDRATE, refill_qty);
    AspectValue highAV = new AspectValue(AlpineAspectType.DEMANDRATE, refill_qty+1.0);
    ScoringFunction qtySF = ScoringFunction.
	createVScoringFunction(lowAV, bestAV, highAV);
    return  inventoryPlugin.getRootFactory().
	newPreference(AlpineAspectType.DEMANDRATE, qtySF);
  }

  /** Utility method to create a Refill Projection Prepositional Phrase
   *  @param prep  The preposition
   *  @param io  The indirect object
   *  @return PrepositionalPhrase  A new prep phrase for the task
   **/
  private PrepositionalPhrase createPrepPhrase(String prep, Object io) {
    NewPrepositionalPhrase newpp = inventoryPlugin.getRootFactory().
	newPrepositionalPhrase();
    newpp.setPreposition(prep);
    newpp.setIndirectObject(io);
    return newpp;
  }

  /** Utility method to get and keep organization info from the InventoryPlugin. **/
  private Organization getMyOrganization() {
    if (myOrg == null) {
       myOrg = inventoryPlugin.getMyOrganization();
       // if we still don't have it after we ask the inventory plugin, throw an error!
       if (myOrg == null) {
	 logger.error("RefillProjectionGenerator can not get MyOrganization " + 
		      "from the InventoryPlugin");
       }
    } 
    return myOrg;
  }

  /** Utility accessor to get the Org Name from my organization and keep it around **/
  private String getOrgName() {
    if (myOrgName == null) {
      myOrgName =getMyOrganization().getItemIdentificationPG().getItemIdentification();
    } 
    return myOrgName;
  }

  /** Utility method to get the default (home) location of the Org **/
  private GeolocLocation getHomeLocation() {
    if (homeGeoloc == null ) {
      Organization org = getMyOrganization();
      if (org.getMilitaryOrgPG() != null) {
	GeolocLocation geoloc = (GeolocLocation)org.getMilitaryOrgPG().getHomeLocation();
	if (geoloc != null) {
	  homeGeoloc = geoloc;
	} else {
	  //if we can't find the home loc either print an error
	  logger.error("RefillProjectionGenerator can not generate a " +
		       "Home Geoloc for org: " + org);
	}  
      }
    }
    return homeGeoloc;
  }


}
    
  
  
