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
import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;


/** The Refill Generator Module is responsible for generating new
 *  refill tasks as needed when new demand is received.  This Refill 
 *  Generator uses a total replan algorithm which ignores all previously
 *  generated refill tasks (that are not yet committed) and replans by
 *  creating new refills based on the new due out totals and the 
 *  inventory levels.  The Comparator module will decide whether to
 *  rescind all previous refills and publish all new refills generated
 *  by this module or whether to compare and merge the 'old' and
 *  'new' refill tasks.
 *  Called by the Inventory Plugin when there is new demand.
 *  Uses the InventoryBG module for inventory bin calculations
 *  Generates Refill tasks which are passed to the Comparator module
 *  (or does the Inventory plugin do that for us?)
 **/

public class RefillGenerator extends InventoryModule {

  private Organization myOrg = null;
  private String myOrgName = null;
  private GeolocLocation homeGeoloc = null;

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillGenerator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  public void calculateRefills(ArrayList touchedInventories, int advanceOrderTime,
                                    int orderFrequency, int maxLeadTime) {
    //Should we push now to the end of today? For now we WILL NOT.
    //long today = getTimeUtils().pushToEndOfDay(inventoryPlugin.getCurrentTimeMillis());
    long today = inventoryPlugin.getCurrentTimeMillis();
    long maxLeadDay = getTimeUtils().addNDays(today, maxLeadTime);
    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      Inventory anInventory = (Inventory) tiIter.next();
      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
        searchForPropertyGroup(LogisticsInventoryPG.class);
      //clear the refills
      thePG.clearRefillTasks(new Date(today));

      //ask the bg (or another module) what the reorder level is
      //int reorderLevel = theBG.getReorderLevel();

      //pick a starting day - today + advanceOrderTime + 1
      long refillDay = getTimeUtils().addNDays(today, advanceOrderTime + 1);
      
      //ask the bg for the inventory level for that day or get a schedule for it
      //int invLevel = theBG.getInventoryLevel(refillDay);
      // or...
      //Schedule invLevelSched = thePG.getInventoryLevelsSchedule();
      //Collection invLevels = invLevelSched.getScheduleElementsWithTime(refillDay);
      //if (! invLevels.isEmpty()) {
      //  //assume there is only one match for now
      //  double invLevel = ((QuantityScheduleElement)invLevel.iterator().
      //                       next()).getQuantity();
      
      //if the inv level is below the reorder level create a refill.
      //if (invLevel < reorderLevel) {
      //  create the refills
      while (refillDay < maxLeadDay) {
        double aRefill = generateRefill(refillDay, orderFrequency);
        //  TODO...add refill to local inventory count
        // make a task for this refill and publish it to glue plugin
        createRefillTask(aRefill, refillDay, anInventory, today);
	// TODO: Apply Refill to LogisticsInventoryBG
        refillDay = getTimeUtils().addNDays(refillDay, orderFrequency);
      }
      
      //}
    
    } // done going through inventories
  }

  private double generateRefill(long day, int orderFrequency) {
    double refillQty = 0;
    long endOfPeriod = getTimeUtils().addNDays(day, orderFrequency);
    //double criticalAtEndOfPeriod = getCriticalLevel(endOfPeriod);
    double demandForPeriod = calculateDemandForPeriod(day, endOfPeriod);
    //refillQty = (criticalAtEndOfPeriod - invLevel) + demandForPeriod;
    return refillQty;
  }

  private double calculateDemandForPeriod(long day, long endOfPeriod) {
    double totalDemand = 0.0;
    long currentDay = day;
    while (currentDay <= endOfPeriod) {
      // double demand = getDemand(currentDay);
//       totalDemand = totalDemand + demand;
      currentDay = getTimeUtils().addNDays(currentDay, 1);
    }
    return totalDemand;
  }

  /** Make a Refill Task and publish it to the InventoryPlugin 
   *  The InventoryPlugin will hook it up with the MaintainInventory task
   *  and it's workflow as well as publishing it to the Blackboard.
   **/
  private void createRefillTask(double quantity, long endDay, Asset inv, long today) {
    // make a new task
    NewTask newRefill = inventoryPlugin.getRootFactory().newTask();
    newRefill.setVerb(Constants.Verb.Supply);
    newRefill.setDirectObject(inv);
    // create preferences
    Vector prefs = new Vector();
    Preference p_end,p_qty;
    p_end = createRefillTimePreference(endDay, today);
    p_qty = createRefillQuantityPreference(quantity);
    prefs.addElement(p_end);
    prefs.addElement(p_qty);
    newRefill.setPreferences(prefs.elements());

    //create Prepositional Phrases
    Vector pp_vector = new Vector();
    pp_vector.addElement(createPrepPhrase(Constants.Preposition.FOR, getOrgName()));
    pp_vector.add(createPrepPhrase(Constants.Preposition.OFTYPE, 
					 inventoryPlugin.getSupplyType()));

    Object io;
    Enumeration geolocs = getAssetUtils().getGeolocLocationAtTime(getMyOrganization(), endDay);
    if (geolocs.hasMoreElements()) {
      io = (GeolocLocation)geolocs.nextElement();
    } else {
	io = getHomeLocation();
    }
    pp_vector.addElement(createPrepPhrase(Constants.Preposition.TO, io));
    //TODO - get the LogInvPG accessor settled
    // Asset resource = inv.getLogisticsInventoryPG().getResource();
//     TypeIdentificationPG tip = ((Asset)resource).getTypeIdentificationPG();
//     MaintainedItem itemID = MaintainedItem.findOrMakeMaintainedItem(
// 								    "Inventory", 
// 								    tip.getTypeIdentification(),
// 								    null,
// 								    tip.getNomenclature());
//     pp_vector.addElement(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.addElement(createPrepPhrase(Constants.Preposition.REFILL, null));

    newRefill.setPrepositionalPhrases(pp_vector.elements());

    inventoryPlugin.publishRefillTask(newRefill, (Inventory)inv);
  }

    //USE V Scoring Function for now - check with Rusty about a better one.
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
      ScoringFunction endTimeSF = ScoringFunction.createVScoringFunction(beforeAV, 
									bestAV, afterAV);
      return inventoryPlugin.getRootFactory().newPreference(AspectType.END_TIME, endTimeSF);
    }

  private Preference createRefillQuantityPreference(double refill_qty) {
    AspectValue lowAV = new AspectValue(AspectType.QUANTITY, 0.01);
    AspectValue bestAV = new AspectValue(AspectType.QUANTITY, refill_qty);
    AspectValue highAV = new AspectValue(AspectType.QUANTITY, refill_qty+1.0);
    ScoringFunction qtySF = ScoringFunction.createVScoringFunction(lowAV, bestAV, highAV);
    return  inventoryPlugin.getRootFactory().newPreference(AspectType.QUANTITY, qtySF);
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
	 logger.error("RefillGenerator can not get MyOrganization from " +
		      "the InventoryPlugin");
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
	    logger.error("RefillGenerator can not generate a Home Geoloc for org: " + org);
	  }  
	}
      }
      return homeGeoloc;
    }


}
    
