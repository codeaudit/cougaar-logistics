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

  // can we only call this if the 'touched' part was a projection touch!!
  public void calculateRefillProjections(ArrayList touchedInventories, int daysOnHand,
                                         long endOfLevelSix, long endOfLevelTwo) {
    // get the demand projections for each customer from bg
    // time shift the demand for each customer
    // sum the each customer's time shifted demand

    //TODO :need to figure out what day to start on
    long today = inventoryPlugin.getCurrentTimeMillis();
    // TODO need whole list of inventories
    ArrayList inventories = new ArrayList();
    if (today < endOfLevelSix) {
      calculateLevelSixProjections(touchedInventories, today, daysOnHand, endOfLevelSix);
      calculateLevelTwoProjections(inventories, daysOnHand, 
                                   getTimeUtils().addNDays(endOfLevelSix, 1),
                                   endOfLevelTwo);
    }

    if ((today > endOfLevelSix) && (today < endOfLevelTwo)) {
      calculateLevelTwoProjections(inventories, daysOnHand, 
                                   getTimeUtils().addNDays(endOfLevelSix, 1),
                                   endOfLevelTwo);
    } 
  }

  private void calculateLevelSixProjections(ArrayList touchedInventories, long today,
                                            int daysOnHand, long endOfLevelSix) {
    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      Inventory anInventory = (Inventory) tiIter.next();
      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
        searchForPropertyGroup(LogisticsInventoryPG.class);
      //do we need to clear the projected refills from the bg?
      //thePG.clearRefillProjections(new Date(today));
      long startDay = today;
      long currentDay = today;
      long customerDemandDay = getTimeUtils().addNDays(currentDay, daysOnHand);
      double projDemand = 0;
      double nextProjDemand = 0;
      
      //get the initial demand for today - Is there a better way to do this
      // to avoid the duplication?
 //      Schedule projectedDemandSched = thePG.getProjectedDemand();
//       Collection projDemandElements = projectedDemandSched.
//         getScheduleElementsWithTime(customerDemandDay);
//       if (! projDemandElements.isEmpty()) {
//         //assume only one match for now
//         projDemand = ((QuantityScheduleElement)projDemandElements.iterator().
//                              next()).getQuantity();
//       // what do we do if this returns null or 0???
//       }
//       //move forward a day
//       currentDay = getTimeUtils().addNDays(currentDay, 1);
//       customerDemandDay = getTimeUtils().addNDays(customerDemandDay, 1);

//       //Begin looping through currentDay forward until you hit the end of
//       // the level six boundary
//       //possible boundary issue... is it '<' or '<=' ??
//       while (currentDay < endOfLevelSix) {
//         projDemandElements = projectedDemandSched.
//           getScheduleElementsWithTime(customerDemandDay);
//         if (! projDemandElements.isEmpty()) {
//           //assume only one match for now
//           nextProjDemand = ((QuantityScheduleElement)projDemandElements.
//                                    iterator().next()).getQuantity();
//           // what do we do if this returns null or 0???
//         }

//         if (projDemand != nextProjDemand) {
//           //if there's a change in the demand, create a refill projection
//           createProjectionRefill(startDay, 
//                                  getTimeUtils().subtractNDays(currentDay, 1),
//                                  today, projDemand, anInventory, thePG);
//           //then reset the startday and the new demand 
//           startDay = currentDay;
//           projDemand = nextProjDemand;
//         }
//         //in either case bump forward a day.
//         currentDay = getTimeUtils().addNDays(currentDay, 1);
//         customerDemandDay = getTimeUtils().addNDays(customerDemandDay, 1);
//        }
//       // when we get to the end of the level six window create the last
//       // projection task (if there is one)
//       if (startDay != currentDay) {
//         createProjectionRefill(startDay,
//                                getTimeUtils().subtractNDays(currentDay, 1),
//                                today, projDemand, anInventory, thePG);
//       }
     }
  }

  private void calculateLevelTwoProjections(ArrayList myInventories, int daysOnHand, 
                                             long start, long endOfLevelTwo) {
    long startDay = start;
    long currentDay = startDay;
    double projDemand = -1;
    while (currentDay < endOfLevelTwo) {
      double combinedDailyDemand = 0;
      for (int i=0; i < myInventories.size(); i++) {
        Inventory inv = (Inventory) myInventories.get(i);
        // should we make a PG collection and keep it around???
        LogisticsInventoryPG thePG = (LogisticsInventoryPG) inv.
          searchForPropertyGroup(LogisticsInventoryPG.class);
        // how do we make aggregates??
       //  combinedDailyDemand = combinedDailyDemand + 
//           thePG.getProjectedDemand(getTimeUtils().addNDays(currentDay, daysOnHand));
        if (i == 0) {
          projDemand = combinedDailyDemand;
        }
      }

      //check the results for the Day.
      if (projDemand != combinedDailyDemand) {
        //if there's a change in the demand, create a refill projection
        createAggregateProjectionRefill(startDay, 
                                        getTimeUtils().subtractNDays(currentDay, 1),
                                        projDemand);
        //then reset the startday and the new demand 
        startDay = currentDay;
        projDemand = combinedDailyDemand;
      }
      //in either case bump forward a day.
      currentDay = getTimeUtils().addNDays(currentDay, 1);
    }
    // when we get to the end of the level two window create the last
    // projection task (if there is one)
    if (startDay != currentDay) {
      createAggregateProjectionRefill(startDay,
                                      getTimeUtils().subtractNDays(currentDay, 1),
                                      projDemand);
    }
  }      
 

  /** Make a Projection Refill Task and publish it to the InventoryPlugin.
   *  The InventoryPlugin will hook the task in to the proper workflow
   *  and publish it to the blackboard.
   **/
  private void createProjectionRefill(long start, long end, long today,  
				      double demand, Inventory inv, 
                                      LogisticsInventoryPG thePG) {
    //create a projection refill task
    NewTask newRefill = inventoryPlugin.getRootFactory().newTask();
    newRefill.setVerb(Constants.Verb.ProjectSupply);
    newRefill.setDirectObject(inv);
    // create preferences
    Vector prefs = new Vector();
    Preference p_start,p_end,p_qty;
    p_start = createRefillTimePreference(start, today);
    p_end = createRefillTimePreference(end, today);
    p_qty = createRefillRatePreference(demand);
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
    Asset resource = thePG.getResource();
    TypeIdentificationPG tip = ((Asset)resource).getTypeIdentificationPG();
    MaintainedItem itemID = MaintainedItem.
      findOrMakeMaintainedItem("Inventory", tip.getTypeIdentification(), 
                               null, tip.getNomenclature(), inventoryPlugin);
    pp_vector.add(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.add(createPrepPhrase(Constants.Preposition.REFILL, null));
    
    newRefill.setPrepositionalPhrases(pp_vector.elements());

    // can this be the same for projection refills?
    inventoryPlugin.publishRefillTask(newRefill, (Inventory)inv);
    // apply this to the bg
    thePG.addRefillProjection(newRefill);
  }


  // DO we really need two separate methods???
  private void createAggregateProjectionRefill(long start, long end, double demand) {
    //create a level two projection refill task
  }

  //  UTILITY METHODS

  //USE V Scoring Function for now. Note that this doesn't exactly represent
  //the scoring function we developed in our IM SDD.
  private Preference createRefillTimePreference(long bestDay, long today) {
    AspectValue beforeAV = new TimeAspectValue(AspectType.END_TIME, today);
    AspectValue bestAV = new TimeAspectValue(AspectType.END_TIME, bestDay);
    //TODO - really need end of deployment from an OrgActivity -
    // As a hack for now just add 180 days from today - note that this
    // will push the possible end date out too far...
    //AspectValue afterAV = new TimeAspectValue(AspectType.END_TIME, 
    //				    inventoryPlugin.getEndOfDeplyment()); 
    AspectValue afterAV = new TimeAspectValue(AspectType.END_TIME,
					      getTimeUtils().addNDays(today, 180));
    ScoringFunction endTimeSF = ScoringFunction.
	createVScoringFunction(beforeAV, bestAV, afterAV);
    return inventoryPlugin.getRootFactory().
	  newPreference(AspectType.END_TIME, endTimeSF);
  }

  // utility method to create Refill Projection Rate preference
  private Preference createRefillRatePreference(double refill_qty) {
    AspectValue lowAV = new AspectValue(AlpineAspectType.DEMANDRATE, 0.01);
    AspectValue bestAV = new AspectValue(AlpineAspectType.DEMANDRATE, refill_qty);
    AspectValue highAV = new AspectValue(AlpineAspectType.DEMANDRATE, refill_qty+1.0);
    ScoringFunction qtySF = ScoringFunction.
	createVScoringFunction(lowAV, bestAV, highAV);
    return  inventoryPlugin.getRootFactory().
	newPreference(AlpineAspectType.DEMANDRATE, qtySF);
  }

  private PrepositionalPhrase createPrepPhrase(String prep, Object io) {
    NewPrepositionalPhrase newpp = inventoryPlugin.getRootFactory().
	newPrepositionalPhrase();
    newpp.setPreposition(prep);
    newpp.setIndirectObject(io);
    return newpp;
  }

  //Get and Keep organization info from the InventoryPlugin.
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

  //Get the Org Name from my organization and keep it around.
  private String getOrgName() {
    if (myOrgName == null) {
      myOrgName =getMyOrganization().getItemIdentificationPG().getItemIdentification();
    } 
    return myOrgName;
  }

  // Get the default (home) location of the Org
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
    
  
  
