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
import org.cougaar.planning.ldm.plan.AspectScorePoint;
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
import java.util.Collection;
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

public class RefillGenerator extends InventoryLevelGenerator {

  private transient Organization myOrg = null;
  private transient String myOrgName = null;
  private transient GeolocLocation homeGeoloc = null;
  
//   private transient String debugItem;

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
  public void calculateRefills(Collection touchedInventories, InventoryPolicy policy, 
			       RefillComparator myComparator) {
    if (policy == null) {
      logger.error("\n Inventory RefillGenerator got a null InventoryPolicy in: " +
		   myOrgName);
    }
    ArrayList newRefills = new ArrayList();
    ArrayList oldRefills = new ArrayList();
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
      // clear the refill lists from the last inventory
      oldRefills.clear();
      newRefills.clear();
      Inventory anInventory = (Inventory) tiIter.next();

//       debugItem = anInventory.getItemIdentificationPG().getItemIdentification();

      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
        searchForPropertyGroup(LogisticsInventoryPG.class);
      // only process Level 6 inventories
      if (! thePG.getIsLevel2()) {
	//clear the refills
	oldRefills.addAll(thePG.clearRefillTasks(new Date(start)));

        int inventoryBucket = thePG.convertTimeToBucket(today);
        int startBucket = thePG.convertTimeToBucket(start);
        // refill time is start + 1 bucket (k+1)
        int refillBucket = startBucket + 1; 
        // max lead day is today + maxLeadTime
        int maxLeadBucket = thePG.convertTimeToBucket(getTimeUtils().
						      addNDays(today, maxLeadTime));


	double prevTarget = 0;

        //calculate inventory levels for today through start (today + OST)
	calculateInventoryLevels(inventoryBucket, startBucket, thePG);

// 	System.out.println("##############  ITEM "+debugItem);

        //  create the refills
        while (refillBucket <= maxLeadBucket) {
          double invLevel = thePG.getLevel(startBucket) - 
            thePG.getActualDemand(refillBucket);
	  if (logger.isDebugEnabled()) { 
	      logger.debug("\n Item "+thePG.getResource()+" Inv Level for startBucket:"+startBucket+" is: "+
			   thePG.getLevel(startBucket) + " - the actual demand for refillbucket:"+
			   refillBucket + " is: " + thePG.getActualDemand(refillBucket) + 
			   ".... The Critical Level for the refill bucket is: " +
			   thePG.getCriticalLevel(refillBucket));
	  }
          if (thePG.getCriticalLevel(refillBucket) < invLevel) {
            thePG.setLevel(refillBucket, invLevel);
	    if (logger.isDebugEnabled()) { 
		logger.debug("\nSetting the Inv Level for refillbucket: " + refillBucket +
			     " to level: " + invLevel);
	    }
	  } else {
//             int reorderPeriodEndBucket = refillBucket + (int)thePG.getReorderPeriod();
//             double refillQty = generateRefill(invLevel, refillBucket, 
// 					      reorderPeriodEndBucket, thePG);
            double refillQty = calculateRefillQty(thePG, refillBucket, invLevel);
	    if(refillQty > 0.0) {

		if (logger.isDebugEnabled()) { 
		    logger.debug("\n Creating Refill for bucket: " + refillBucket +
				 " of quantity: " + refillQty);
		}
		// make a task for this refill and publish it to glue plugin
		// and apply it to the LogisticsInventoryBG
		Task theRefill = createRefillTask(refillQty, 
						  thePG.convertBucketToTime(refillBucket), 
						  anInventory, thePG, 
						  today, orderShipTime);
		newRefills.add(theRefill);
	    }
	    else if (logger.isDebugEnabled()) {
		logger.debug("Not placing a refill order of qty: " + refillQty) ;
	    }

	    thePG.setLevel(refillBucket, (invLevel + refillQty));
            //set the target level to invlevel + refillQty (if we get the refill we
            //ask for we hit the target - otherwise we don't)
            thePG.setTarget(refillBucket, (invLevel + refillQty));

// 	    if ((debugItem.indexOf("9140002865294") > 0) && (getOrgName().indexOf("FSB") > 0)) {
// 	      System.out.println("     bucket: "+refillBucket+", endbucket: "+reorderPeriodEndBucket+
// 				 ", inv level : "+invLevel+", refillQty: "+refillQty+
// 				 ", Target: "+(invLevel+refillQty));
// 	    }
	    if (logger.isDebugEnabled()) { 
		double printsum = invLevel + refillQty;
  		logger.debug("\nSetting the InventoryLevel and the TargetLevel to: " + printsum);
	    }
	    prevTarget = (invLevel + refillQty);
	  }
	  //reset the buckets
	  startBucket = refillBucket;
	  refillBucket = startBucket + 1;
	}
	// Set the target levels for projection period here since very similar 
	// calculations are done for both projection and refill period.
	setTargetForProjectionPeriod(thePG, maxLeadBucket+1, prevTarget);
	// call the Comparator for this Inventory which will compare the old and
	// new Refills and then publish the new Refills and Rescind the old Refills.
	myComparator.compareRefills(newRefills, oldRefills, anInventory);
      } // end of if not level 2 inventory
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
    int currentBucket = refillBucket + 1;
    while (currentBucket <= endOfPeriodBucket) {
      double demand = thePG.getActualDemand(currentBucket);
      totalDemand = totalDemand + demand;
      currentBucket = currentBucket + 1;
    }
    return totalDemand;
  }

  /** Make a Refill Task 
   *  @param quantity The quantity of the Refill Task
   *  @param endDay  The desired delivery date of the Refill Task
   *  @param inv  The Inventory this Refill Task is resupplying
   *  @param thePG  The LogisticsInventoryPG for the Inventory.
   *  @param today  Time representing now to use as the earliest possible delivery
   *  @param ost The OrderShipTime used to calculate the CommitmentDate
   *  @return Task The new Refill Task
   **/
  private Task createRefillTask(double quantity, long endDay, 
                                Inventory inv, LogisticsInventoryPG thePG, 
                                long today, int ost) {
    // make a new task
    NewTask newRefill = inventoryPlugin.getRootFactory().newTask();
    newRefill.setVerb(Constants.Verb.Supply);
    //newRefill.setDirectObject(inv);
    newRefill.setDirectObject(thePG.getResource());
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
    return newRefill;
  }

  /** Utility method to create the Refill tasks time preference
   *  Use a Piecewise Linear scoring function - see the IM desing doc
   *  for details.
   *  @param bestDay The time representation of the desired result
   *  @param today The time representation of today or now
   *  @return Preference  The new time preference
   **/
  private Preference createRefillTimePreference(long bestDay, long today) {
    //TODO - really need last day in theatre from an OrgActivity -
    long end = inventoryPlugin.getOPlanEndTime();
    double daysBetween = ((end - bestDay)  / getTimeUtils().MSEC_PER_DAY) - 1;
    //Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
    // define alpha .25
    double alpha = .25;
    Vector points = new Vector();

    AspectScorePoint earliest = new AspectScorePoint(today, alpha, AspectType.END_TIME);
    AspectScorePoint best = new AspectScorePoint(bestDay, 0.0, AspectType.END_TIME);
    AspectScorePoint first_late = new AspectScorePoint(getTimeUtils().addNDays(bestDay, 1), 
                                                       alpha, AspectType.END_TIME);
    AspectScorePoint latest = new AspectScorePoint(end, (alpha + late_score), 
                                                   AspectType.END_TIME);

    points.addElement(earliest);
    points.addElement(best);
    points.addElement(first_late);
    points.addElement(latest);
    ScoringFunction endTimeSF = ScoringFunction.
      createPiecewiseLinearScoringFunction(points.elements());
    return inventoryPlugin.getRootFactory().
      newPreference(AspectType.END_TIME, endTimeSF);
    
  }

  /** Utility method to create a Refill Quantity  preference
   *  We use a Strictly At scoring function for this preference.
   *  Note that out use of strictly at allows for multiple shipments as 
   *  long as the total amount delivered meets the best quantity for this
   *  preference inside the feasable delivery time (defined by the end_time
   *  scoring function)
   *  @param refill_qty  The quantity we want for this Refill Task
   *  @return Preference  The new quantity preference for the Refill Task
   **/
  private Preference createRefillQuantityPreference(double refill_qty) {
    ScoringFunction qtySF = ScoringFunction.
      createStrictlyAtValue(new AspectValue(AspectType.QUANTITY, refill_qty));
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
  
  /** Determines and sets the Target and Inventory Levels for projection period.
   *  Target and Inventory Levels are used for display purposes only.
   *  The calculation for Target Level during the projection period is:
   *  target = (criticalLevelEnd - criticalLevelBegin) + demand
   *  The calculation for the Inventory level for each bucket during the projection period is
   *  the average of the critical level and the target level for that day.
   *  Note that since target levels are only set for each reorder period which usually spans
   *  more than one bucket, we must interpolate what the target level would be for buckets in
   *  the middle of the reorder period.
   *  @param thePG The LogisticsInventoryPG for current inventory
   *  @param startBucket The bucket that starts the Projection period.
   *  @return void  The target level is set in thePG with this method
   **/
  private void setTargetForProjectionPeriod(LogisticsInventoryPG thePG, 
                                            int startBucket, double prevTarget){
    if (logger.isDebugEnabled()) { 
      logger.debug("For item: "+thePG.getResource() + 
                   " set Projection inventory and target levels starting with bucket: "+
                   startBucket);
    }

    int reorderPeriod = (int)thePG.getReorderPeriod();
    int lastDemandBucket = thePG.getLastDemandBucket();
    double lastTarget;
    int inventoryBucket;
    
    // get the first target before we loop so we have 2 points for the
    // inventory level calculations.
    double invLevel = thePG.getLevel(startBucket - 1) - 
      thePG.getActualDemand(startBucket);
    double refillQty = calculateRefillQty(thePG, startBucket, invLevel);
    double target = invLevel + refillQty;
    thePG.setTarget(startBucket, target);
    lastTarget = target;

    // Start the loop after the first reorderperiod
    for (int targetLevelBucket = startBucket + reorderPeriod; 
         targetLevelBucket <= lastDemandBucket; 
         targetLevelBucket = targetLevelBucket + reorderPeriod ) {
      invLevel = thePG.getLevel(targetLevelBucket - 1) - 
        thePG.getActualDemand(targetLevelBucket);
      refillQty = calculateRefillQty(thePG, targetLevelBucket, invLevel);
      target = invLevel + refillQty;
      thePG.setTarget(targetLevelBucket, target);

      // set the start point for the inventory calcualtions to the 
      // beginning of the last target window 
      //ie whatever bucket lastTarget was set for.
      inventoryBucket = targetLevelBucket - reorderPeriod;

      for (int i=0; i < reorderPeriod; i++) {
        double diff = target - lastTarget; 
        double calcTarget = lastTarget + ((diff * i)/reorderPeriod);
        //inv level in projection land is the average of the critcial and target levels
        double level = (thePG.getCriticalLevel(inventoryBucket + i) + calcTarget) / 2;
        thePG.setLevel(inventoryBucket + i, level);
      }
    }
  }


  private double calculateRefillQty(LogisticsInventoryPG thePG, int refillBucket, double invLevel) {
    int reorderPeriodEndBucket = refillBucket + (int)thePG.getReorderPeriod();
    return generateRefill(invLevel, refillBucket, 
                          reorderPeriodEndBucket, thePG);
  }

}
    
