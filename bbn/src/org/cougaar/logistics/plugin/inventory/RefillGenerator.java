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

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
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
 *  Generates Refill tasks which are passed to the InventoryPlugin
 *  to be added to the MaintainInventory workflow and published.
 **/

public class RefillGenerator extends InventoryModule {

  private transient Organization myOrg = null;
  private transient String myOrgName = null;
  private transient GeolocLocation homeGeoloc = null;

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillGenerator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  /** Called by the Inventory Plugin to re-calculate refills when an demand has changed
   *  for an inventory.  This method creates Refill Tasks and publishes them through the
   *  Inventory Plugin.
   *  @param touchedInventories  The collection of changed Inventories.
   *  @param policy The InventoryPolicy
   **/
  public void calculateRefills(ArrayList touchedInventories, InventoryPolicy policy) {
    int orderShipTime = policy.getOrderShipTime();
    int maxLeadTime = policy.getSupplierAdvanceNoticeTime() + orderShipTime;
    
    //Should we push now to the end of today? For now we WILL NOT.
    //long today = getTimeUtils().
    //  pushToEndOfDay(inventoryPlugin.getCurrentTimeMillis());
    long today = inventoryPlugin.getCurrentTimeMillis();
    //start time (k) is today plus OST.
    long start = getTimeUtils().addNDays(today, orderShipTime);
    
    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      Inventory anInventory = (Inventory) tiIter.next();
      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
        searchForPropertyGroup(LogisticsInventoryPG.class);
      //clear the refills
      thePG.clearRefillTasks(new Date(start));

      int inventoryBucket = thePG.convertTimeToBucket(today);
      int startBucket = thePG.convertTimeToBucket(start);
      // refill time is start + 1 bucket (k+1)
      int refillBucket = startBucket + 1; 
      // max lead day is today + maxLeadTime
      int maxLeadBucket = thePG.convertTimeToBucket(getTimeUtils().
                                             addNDays(today, maxLeadTime));

      //calculate inventory levels for today through start (today + OST)
      while (inventoryBucket <= startBucket) {
	  double level = thePG.getLevel(inventoryBucket) -
	      thePG.getActualDemand(inventoryBucket + 1);
	  double committedRefill = findCommittedRefill(inventoryBucket, thePG);
	  thePG.setLevel(inventoryBucket, (level + committedRefill) );
	  inventoryBucket = inventoryBucket + 1;
      }


      //  create the refills
      while (refillBucket <= maxLeadBucket) {
         double invLevel = thePG.getLevel(startBucket) - 
          thePG.getActualDemand(refillBucket);
        if (thePG.getCriticalLevel(refillBucket) < invLevel) {
          thePG.setLevel(refillBucket, invLevel);
        } else {
          int reorderPeriodEndBucket = refillBucket + (int)thePG.getReorderPeriod();
          double refillQty = generateRefill(invLevel, refillBucket, 
                                            reorderPeriodEndBucket, thePG);
          // make a task for this refill and publish it to glue plugin
          // and apply it to the LogisticsInventoryBG
          createRefillTask(refillQty, thePG.convertBucketToTime(refillBucket), 
                           anInventory, thePG, today, orderShipTime);
          thePG.setLevel(refillBucket, (invLevel + refillQty));
        }
        //reset the buckets
        startBucket = refillBucket;
        refillBucket = startBucket + 1;
      }
    
    } // done going through inventories
  }

  /** Utility method to generate the Refill Amount
   *  This method starts the following calculation
   *  RF(k+1)=(C(k+RP+1)-IL(k+1)) + (D(K+2)+...+(k+RP+1))
   *  @param invLevel  The current inventory level for the refill bucket
   *  @param refillBucket  The bucket we are generating a refill for.  This is
   *   the bucket at k+1
   *  @param reorderPeriodEndBucket  The end point for which we want demand for.
   *   This is the bucket at k+RP+1.
   *  @param thePG  The LogisticsInventoryPG of the Inventory we are Refilling.
   *  @return double The Refill Amount. This is RF(k+1)
   **/
  private double generateRefill(double invLevel, int refillBucket, 
                                int reorderPeriodEndBucket, 
                                LogisticsInventoryPG thePG) {
    double refillQty = 0;
    double criticalAtEndOfPeriod = thePG.getCriticalLevel(reorderPeriodEndBucket);
    double demandForPeriod = calculateDemandForPeriod(thePG, 
                                                      refillBucket, 
                                                      reorderPeriodEndBucket);
    refillQty = (criticalAtEndOfPeriod - invLevel) + demandForPeriod;
    return refillQty;
  }

  /** Utility method to calculate the demand for the Reorder Period
   *  This method does the calculation for:
   *  (D(K+2)+...+(k+RP+1))
   *  @param thePG The LogisticsInventoryPG of the Inventory we are Refilling.
   *  @param refillBucket  The bucket we are generating a refill for.  This is
   *   the bucket at k+1
   *  @param reorderPeriodEndBucket  The end point for which we want demand for.
   *   This is the bucket at k+RP+1.
   *  @return double  The sum of Demand for the Reorder Period.
   **/
  private double calculateDemandForPeriod(LogisticsInventoryPG thePG, 
                                          int refillBucket, int endOfPeriodBucket) {
    double totalDemand = 0.0;
    int currentBucket = refillBucket;
    while (currentBucket <= endOfPeriodBucket) {
      double demand = thePG.getActualDemand(currentBucket);
      totalDemand = totalDemand + demand;
      currentBucket = currentBucket + 1;
    }
    return totalDemand;
  }

  /** Make a Refill Task and publish it to the InventoryPlugin 
   *  The InventoryPlugin will hook it up with the MaintainInventory task
   *  and it's workflow as well as publishing it to the Blackboard.
   *  @param quantity The quantity of the Refill Task
   *  @param endDay  The desired delivery date of the Refill Task
   *  @param inv  The Inventory this Refill Task is resupplying
   *  @param thePG  The LogisticsInventoryPG for the Inventory.
   *  @param today  Time representing now to use as the earliest possible delivery
   *  @param ost The OrderShipTime used to calculate the CommitmentDate
   **/
  private void createRefillTask(double quantity, long endDay, 
                                Asset inv, LogisticsInventoryPG thePG, 
                                long today, int ost) {
    // make a new task
    NewTask newRefill = inventoryPlugin.getRootFactory().newTask();
    newRefill.setVerb(Constants.Verb.Supply);
    newRefill.setDirectObject(inv);
    //set the commitment date to endDay - ost.
    newRefill.setCommitmentDate(new Date(getTimeUtils().subtractNDays(endDay, ost)));
    // create preferences
    Vector prefs = new Vector();
    Preference p_end,p_qty;
    p_end = createRefillTimePreference(endDay, today);
    p_qty = createRefillQuantityPreference(quantity);
    prefs.add(p_end);
    prefs.add(p_qty);
    newRefill.setPreferences(prefs.elements());

    //create Prepositional Phrases
    Vector pp_vector = new Vector();
    pp_vector.add(createPrepPhrase(Constants.Preposition.FOR, getOrgName()));
    pp_vector.add(createPrepPhrase(Constants.Preposition.OFTYPE, 
					 inventoryPlugin.getSupplyType()));

    Object io;
    Enumeration geolocs = getAssetUtils().
      getGeolocLocationAtTime(getMyOrganization(), endDay);
    if (geolocs.hasMoreElements()) {
      io = (GeolocLocation)geolocs.nextElement();
    } else {
	io = getHomeLocation();
    }
    pp_vector.add(createPrepPhrase(Constants.Preposition.TO, io));
    Asset resource = thePG.getResource();
    TypeIdentificationPG tip = ((Asset)resource).getTypeIdentificationPG();
    MaintainedItem itemID = MaintainedItem.
      findOrMakeMaintainedItem("Inventory", tip.getTypeIdentification(),
                               null, tip.getNomenclature(), inventoryPlugin);
    pp_vector.add(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.add(createPrepPhrase(Constants.Preposition.REFILL, null));

    newRefill.setPrepositionalPhrases(pp_vector.elements());
    // hook this in with MaintainInventory and publish
    inventoryPlugin.publishRefillTask(newRefill, (Inventory)inv);
    //apply this to the LogisticsInventoryBG
    thePG.addRefillRequisition(newRefill);
  }

  /** Utility method to create the Refill tasks time preference
   *  Use a V scoring function for now in place of our new scoring function
   *  design.
   *  @param bestDay The time representation of the desired result
   *  @param today The time representation of today or now
   *  @return Preference  The new time preference
   **/
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

  /** Utility method to create a Refill Quantity  preference
   *  We use a V scoring function for this preference.
   *  @param refill_qty  The quantity we want for this Refill Task
   *  @return Preference  The new quantity preference for the Refill Task
   **/
  private Preference createRefillQuantityPreference(double refill_qty) {
    AspectValue lowAV = new AspectValue(AspectType.QUANTITY, 0.01);
    AspectValue bestAV = new AspectValue(AspectType.QUANTITY, refill_qty);
    AspectValue highAV = new AspectValue(AspectType.QUANTITY, refill_qty+1.0);
    ScoringFunction qtySF = ScoringFunction.
      createVScoringFunction(lowAV, bestAV, highAV);
    return  inventoryPlugin.getRootFactory().
      newPreference(AspectType.QUANTITY, qtySF);
  }

 /** Utility method to create a Refill Task Prepositional Phrase
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
        logger.error("RefillGenerator can not get MyOrganization from " +
                     "the InventoryPlugin");
      }
    } 
    return myOrg;
  }

  /** Utility method to get the Org Name from my organization and keep it around. **/
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
        GeolocLocation geoloc = (GeolocLocation)org.
          getMilitaryOrgPG().getHomeLocation();
        if (geoloc != null) {
          homeGeoloc = geoloc;
        } else {
          //if we can't find the home loc either print an error
          logger.error("RefillGenerator can not generate a " +
                       "Home Geoloc for org: " + org);
        }  
      }
    }
    return homeGeoloc;
  }
  
  /**Utility method to help find commited refills 
   *  NOTE this only finds a quantity IF there is a reported AllocationResult
   *  for the Task!
   **/
  private double findCommittedRefill(int bucket, LogisticsInventoryPG thePG) {
    double refillQty = 0;
    ArrayList reqs = thePG.getRefillRequisitions();
    Iterator reqsIter = reqs.iterator();
    while (reqsIter.hasNext()) {
      Task refill = (Task) reqsIter.next();
      PlanElement pe = refill.getPlanElement();
	if (pe !=null ) {
	  AllocationResult ar = pe.getReportedResult();
	  if (ar != null) {
	    double endTime = ar.getValue(AspectType.END_TIME);
	    if (bucket == thePG.convertTimeToBucket((long)endTime)) {
	      // we found a refill for this bucket
	      refillQty = ar.getValue(AspectType.QUANTITY);
	      return refillQty;
	     }
	  }
	}
    }
    // if we did not find a match return 0.0
    return refillQty;
  }


}
    
