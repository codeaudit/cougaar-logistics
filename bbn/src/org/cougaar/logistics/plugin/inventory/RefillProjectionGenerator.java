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

import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.FlowRate;

import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
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
 *  NOTE:  Right now this module assumes that all customers have the 
 *  same VTH boundaries as we do.  This means that Level 2 demand is
 *  calculated soley from Level 2 incoming demand.  In the future we will
 *  need to account for differing level2 boundaries and calculate level 2 
 *  projections by summing the tonnage across level 6 projections for all
 *  inventories.
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
  public void calculateRefillProjections(Collection touchedInventories, int daysOnHand,
                                         long endOfLevelSix, long endOfLevelTwo, 
					 RefillComparator theComparator) {
    // get the demand projections for each customer from bg
    // time shift the demand for each customer
    // sum the each customer's time shifted demand

    ArrayList level6Inventories = new ArrayList();
    Inventory level2Inv = null;

    Iterator touchedIter = touchedInventories.iterator();
    while (touchedIter.hasNext()) {
      Inventory inv = (Inventory) touchedIter.next();
      LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
        searchForPropertyGroup(LogisticsInventoryPG.class);
      if (thePG.getIsLevel2()) {
        level2Inv = inv;
      } else {
        level6Inventories.add(inv);
      }
    }

    if (! level6Inventories.isEmpty()) {
      calculateLevelSixProjections(level6Inventories, daysOnHand,
                                   endOfLevelSix, theComparator);
    }
    // if the level 2 and level 6 boundaries are = don't process
    // and just for safety make sure the end of level 2 doesn't fall before
    // the end of the level 6 window
    if ((level2Inv != null) && ( endOfLevelTwo > endOfLevelSix)) {
      calculateLevelTwoProjections(level2Inv, daysOnHand, 
                                   getTimeUtils().addNDays(endOfLevelSix, 1),
                                   endOfLevelTwo, theComparator);
    }
  }

   
  /** Calculate Projection Refills in level Six detail until the end of the
   *  Level 6 VTH window.
   *  @param touchedInventories  The Inventories that have changed wrt projections.
   *   We will only recalculate refill projections for these.
   *  @param daysOnHand DaysOnHand policy.
   *  @param endOfLevelSix The date representing the end of the Level 6 VTH window.
   **/
  private void calculateLevelSixProjections(Collection touchedInventories, 
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
      thePG.setEndOfLevelSixBucket(endOfLevelSixBucket);
      
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
	  //BUT only if demand is non-zero 
	  if (projDemand > 0.0) {
	    Task refill = createProjectionRefill(thePG.convertBucketToTime(startBucket), 
						 thePG.convertBucketToTime(currentBucket),
						 projDemand, anInventory, thePG);
	    refillProjections.add(refill);
	  }
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
                                                 thePG.convertBucketToTime(currentBucket),
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
   *  Right now we assume that all of our customers have the same
   *  VTH boundaries as we do - so all of our level refills are based
   *  on customer level 2 demand.
   *  In the future we will want to enhance this code to deal with 
   *  customers having different VTH boundaries then us, meaning that we
   *  need to take into account customer level 2 demand and calculate level 2
   *  demand from customer level 6 demand if our level 2 window is earlier than the
   *  customer's level2 window.
   *  @param level2Inv The level 2 inventory for this supply type.
   *  @param daysOnHand  The DaysOnHand value from the InventoryPolicy
   *  @param start The start time of the Level 2 VTH window
   *  @param endOfLevelTwo The end time of the Level 2 VTH window
   *  @param theComparator The Comparator instance to compare old and new level 2 projections
   **/
  private void calculateLevelTwoProjections(Inventory level2Inv, int daysOnHand, 
                                            long start, long endOfLevelTwo, 
                                            RefillComparator theComparator) {

    ArrayList newProjections = new ArrayList();
    ArrayList oldProjections = new ArrayList();

    LogisticsInventoryPG level2PG = (LogisticsInventoryPG) level2Inv.
      searchForPropertyGroup(LogisticsInventoryPG.class);

    // for now clear all the level 2 projections and only create
    // new ones for the current level2 window
    // this will have to change once we have mismatched level 2 windows
    oldProjections.addAll(level2PG.clearRefillProjectionTasks());

    int startBucket = level2PG.convertTimeToBucket(start);
    int currentBucket = startBucket;
    int customerDemandBucket = level2PG.convertTimeToBucket(getTimeUtils().
                                                           addNDays(start, daysOnHand));
    int endOfLevelTwoBucket = level2PG.convertTimeToBucket(endOfLevelTwo);
    // get intital demand level
    double projDemand = level2PG.getProjectedDemand(customerDemandBucket);
    double nextProjDemand = 0;
    // move ahead one day
    currentBucket = currentBucket + 1;
    customerDemandBucket = customerDemandBucket + 1;

    //In the future with varying customer level 2 windows we will have to 
    //loop through all of our Inventories and sum the tonnage of demand
    // for a level 2 combined demand per bucket.

    //loop through the buckets until we reach the end of level two
    while (currentBucket < endOfLevelTwoBucket) {
      nextProjDemand = level2PG.getProjectedDemand(customerDemandBucket);
      //check the results for the Day.
      if (projDemand != nextProjDemand) {
        //if there's a change in the demand, create a refill projection
        Task newLevel2Refill = 
          createAggregateProjectionRefill(level2PG.convertBucketToTime(startBucket), 
                                          level2PG.convertBucketToTime(currentBucket - 1),
                                          level2PG.getStartTime(), projDemand, level2PG,
                                          level2Inv);
        newProjections.add(newLevel2Refill);
        //then reset the start bucket  and the new demand 
        startBucket = currentBucket;
        projDemand = nextProjDemand;
      }
      // bump forward a bucket.
      currentBucket = currentBucket + 1;
      customerDemandBucket = customerDemandBucket + 1;
    }
    // when we get to the end of the level two window create the last
    // projection task (if there is one)
    if (startBucket != currentBucket) {
      Task lastLevel2Refill = 
        createAggregateProjectionRefill(level2PG.convertBucketToTime(startBucket), 
                                        level2PG.convertBucketToTime(currentBucket - 1),
                                        level2PG.getStartTime(), projDemand, level2PG,
                                        level2Inv);
      newProjections.add(lastLevel2Refill);
    }
    // send the new projections and the old projections to the Comparator
    // the comparator will rescind the old and publish the new projections
    theComparator.compareRefillProjections(newProjections, oldProjections, 
                                          level2Inv);
  }      
 

  /** Make a Projection Refill Task and publish it to the InventoryPlugin.
   *  The InventoryPlugin will hook the task in to the proper workflow
   *  and publish it to the blackboard.
   *  @param start The start time for the Task
   *  @param end The end time for the Task
   *  @param demand The demandrate value of the task
   *  @param inv  The inventory Asset this Task is refilling.
   *  @param thePG  The Property Group of the Inventory Asset
   *  @return Task The new Projection Refill
   **/
  private Task createProjectionRefill(long start, long end,
                                      double demand, Inventory inv, 
                                      LogisticsInventoryPG thePG) {
    //create a projection refill task
    NewTask newRefill = inventoryPlugin.getRootFactory().newTask();
    newRefill.setVerb(Constants.Verb.ProjectSupply);
    //newRefill.setDirectObject(inv);
    newRefill.setDirectObject(thePG.getResource());
    newRefill = fillInTask(newRefill, start, end, thePG.getStartTime(), 
                           demand, thePG);
    return newRefill;
  }

  /** Create a Level 2 Projection Refill
   *  @param start The start time of the Task
   *  @param end The end time of the Task
   *  @param earliest  The earliest delivery time.
   *  @param demand The total demand in terms of tons or volume
   *  @param level2PG  The PropertyGroup of the Level 2 Inventory
   *  @param level2Inv  The Level 2 Inventory
   *  @return Task The new Level 2 Projection Task
   **/
  private Task createAggregateProjectionRefill(long start, long end, 
                                               long earliest, double demand, 
                                               LogisticsInventoryPG level2PG,
                                               Inventory level2Inv) {
    //create a level two projection refill task
    NewTask newAggRefill = inventoryPlugin.getRootFactory().newTask();
    newAggRefill.setVerb(Constants.Verb.ProjectSupply);
    //TODO - for now physical pg can stay the same as the demand task
    //physical pg needs to represent demand
    // level2Asset.setPhysicalPG(demand);
    newAggRefill.setDirectObject(level2Inv);
    newAggRefill = fillInTask(newAggRefill, start, end, earliest, 1, level2PG);
    return newAggRefill;
  }

  /** Utility method to fill in task details
   *  @param newRefill The task to fill in
   *  @param start Start time for Task
   *  @param end End Time for Task
   *  @param qty Quantity Pref for Task
   *  @param thePG The property group attached to the Inventory 
   *  @param NewTask Return the filled in Task
   **/
  private NewTask fillInTask(NewTask newRefill, long start, long end, long earliest, 
                             double qty, LogisticsInventoryPG thePG) {
    // create preferences
    Vector prefs = new Vector();
    Preference p_start,p_end,p_qty;
    p_start = createRefillTimePreference(start, earliest, AspectType.START_TIME);
    p_end = createRefillTimePreference(end, earliest, AspectType.END_TIME);
    p_qty = createRefillRatePreference(qty, thePG.getBucketMillis());
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
    TypeIdentificationPG tip = thePG.getResource().getTypeIdentificationPG();
    MaintainedItem itemID = MaintainedItem.
      findOrMakeMaintainedItem("Inventory", tip.getTypeIdentification(), 
                               null, tip.getNomenclature(), inventoryPlugin);
    pp_vector.add(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.add(createPrepPhrase(Constants.Preposition.REFILL, null));
    
    newRefill.setPrepositionalPhrases(pp_vector.elements());
    return newRefill;
  } 

  /** Create a Time Preference for the Refill Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param start The earliest time this preference can have
   *  @param aspectType The AspectType of the preference- should be start_time or end_time
   *  @return Preference The new Time Preference
   **/
  private Preference createRefillTimePreference(long bestDay, long start, 
                                                int aspectType) {
    //TODO - really need last day in theatre from an OrgActivity -
    long end = inventoryPlugin.getOPlanEndTime();
    double daysBetween = ((end - bestDay)  / getTimeUtils().MSEC_PER_DAY) - 1;
    //Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
    // define alpha .25
    double alpha = .25;
    Vector points = new Vector();

    AspectScorePoint earliest = new AspectScorePoint(start, alpha, aspectType);
    AspectScorePoint best = new AspectScorePoint(bestDay, 0.0, aspectType);
    AspectScorePoint first_late = new AspectScorePoint(getTimeUtils().addNDays(bestDay, 1), 
                                                       alpha, aspectType);
    AspectScorePoint latest = new AspectScorePoint(end, (alpha + late_score), aspectType);

    points.addElement(earliest);
    points.addElement(best);
    points.addElement(first_late);
    points.addElement(latest);
    ScoringFunction timeSF = ScoringFunction.
      createPiecewiseLinearScoringFunction(points.elements());
    return inventoryPlugin.getRootFactory().
      newPreference(aspectType, timeSF);
  }

  /** Utility method to create Refill Projection Rate preference
   *  We use a V scoring function for this preference.
   *  @param refill_qty  The quantity we want for this Refill Task
   *  @return Preference  The new demand rate preference for the Refill Task
   **/
  private Preference createRefillRatePreference(double refill_qty, long bucketMillis) {
    double ratevalue = refill_qty / (bucketMillis / getTimeUtils().MSEC_PER_DAY);
    AspectRate bestAV;
    //highAV could be bumped to more than refill_qty + 1 if needed
    if (inventoryPlugin.getSupplyType().equals("BulkPOL")) {
      bestAV = new AspectRate(AlpineAspectType.DEMANDRATE, 
                              FlowRate.newGallonsPerDay(ratevalue));
    } else {
      bestAV = new AspectRate(AlpineAspectType.DEMANDRATE, 
                              CountRate.newEachesPerDay(ratevalue));
    }
    ScoringFunction qtySF = ScoringFunction.
	createStrictlyAtValue(bestAV);
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
    
  
  
