/*
* <copyright>
*  
*  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import java.text.DecimalFormat;


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

public class RefillGenerator extends InventoryLevelGenerator implements RefillGeneratorModule {

  private transient Organization myOrg = null;
  private transient String myOrgName = null;
  private transient GeolocLocation homeGeoloc = null;
  private transient DecimalFormat myDecimalFormatter = null;

  //private transient String debugItem;

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
   *  @param myComparator The RefillComparator to use.
   **/
  public void calculateRefills(Collection touchedInventories, ComparatorModule myComparator) {
    ArrayList newRefills = new ArrayList();
    ArrayList oldRefills = new ArrayList();
    int orderShipTime = inventoryPlugin.getOrderShipTime();
    int maxLeadTime = inventoryPlugin.getMaxLeadTime();

    //Should we push now to the end of today? For now we WILL NOT.
    //long today = getTimeUtils().
    //  pushToEndOfDay(inventoryPlugin.getCurrentTimeMillis());
    long today = inventoryPlugin.getCurrentTimeMillis();
    //start time (k) is today plus OST.
    //long start = getTimeUtils().addNDays(today, orderShipTime);
    long start;



    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      // clear the refill lists from the last inventory
      oldRefills.clear();
      newRefills.clear();
      Inventory anInventory = (Inventory) tiIter.next();

      //debugItem = anInventory.getItemIdentificationPG().getItemIdentification();

      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
              searchForPropertyGroup(LogisticsInventoryPG.class);

      start = today + (orderShipTime * thePG.getBucketMillis());

      // DEBUG
      //   String myOrgName = inventoryPlugin.getOrgName();
      String myItemId = anInventory.getItemIdentificationPG().getItemIdentification();

      //Only magically recalculate the initial inventory level
      //(the prepo) up to when the prepo arrives.
      if(today <= inventoryPlugin.getPrepoArrivalTime()) {
	  thePG.recalculateInitialLevel();
      }

      // only process Level 6 inventories
      if (! thePG.getIsLevel2()) {
        //clear the refills
        oldRefills.addAll(thePG.clearRefillTasks(new Date(today)));

        int startBucket = thePG.convertTimeToBucket(start, false);
        // refill time is start + 1 bucket (k+1)
        int refillBucket = startBucket + 1;
        // max lead day is today + maxLeadTime
        //int maxLeadBucket = thePG.convertTimeToBucket(getTimeUtils().
        //addNDays(today, maxLeadTime), false);
        // Do not count partial buckets hear because when planning we want the start of the
        // bucket.  This prevents us from removing refills that because of OST, cannot be
        // replaced leaving holes in supply.
        int maxLeadBucket = thePG.convertTimeToBucket(today, false) + maxLeadTime;
	int firstLegalRefillBucket =
	    thePG.convertTimeToBucket(inventoryPlugin.getRefillStartTime(),false) + orderShipTime;
        boolean clearTargetLevels = true;
        int clearTargetBucket = refillBucket;
        double prevTarget = 0;

        //calculate inventory levels for time ZERO through start (today + OST)
        //Time period which refillGenerator should not touch
        calculateInventoryLevelsForRG(0, startBucket, thePG);

        //System.out.println("##############  ITEM "+debugItem);

        //  create the refills
        // RJB
        int lastRefillBucket=refillBucket;

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

          // Note we had the following to fix some weirdness in subsistence charts
          // but the critical == 0.0 prevents refills from being created during
          // times when we get demand that causes us to go below 0 and the critical level
          // is 0.  For now just check that we are below and if we see weird behavior with
          // subsistence charts we'll put in another check.
          //  if ((thePG.getCriticalLevel(refillBucket)< invLevel)  ||
          //     (thePG.getCriticalLevel(refillBucket) == 0.0)) {

          // create an offset to account for the ammo rounding - otherwise we refill
          // more then we need to because the refill might cause us to go below the
          // critical level by .05 or so - pad with .06 to make sure we cover the bases.
          double criticalLevelOffset = 0.00;
          if (inventoryPlugin.getSupplyType().equals("Ammunition")) {
            criticalLevelOffset = 0.06;
          }
          if (((thePG.getCriticalLevel(refillBucket) - criticalLevelOffset) < invLevel) ||
                  ((thePG.getCriticalLevel(refillBucket) == 0.0) && (invLevel >= 0.0))){
            thePG.setLevel(refillBucket, invLevel);
            if (logger.isDebugEnabled()) {
              logger.debug("\nSetting the Inv Level for refillbucket: " + refillBucket +
                           " to level: " + invLevel);
            }
          } else {
//             int reorderPeriodEndBucket = refillBucket + (int)thePG.getReorderPeriod();
//             double refillQty = generateRefill(invLevel, refillBucket,
// 					      reorderPeriodEndBucket, thePG);
            int reorderPeriodEndBucket = refillBucket + (int)thePG.getReorderPeriod();
            double targetLevel = getTargetLevel(refillBucket,reorderPeriodEndBucket, thePG);
            if (clearTargetLevels) {
              clearTargetLevels = false;
              thePG.clearTargetLevels(clearTargetBucket);
            }
            thePG.setTarget(refillBucket, targetLevel);
            if (thePG.getFillToCapacity()) {
              double capacity = thePG.getCapacity();
              if (targetLevel > capacity) {
                logger.warn("Target Level of "+targetLevel+" for inventory: "+
                            thePG.getResource().getTypeIdentificationPG().getTypeIdentification()+
                            " at "+getMyOrganization()+
                            " was clipped to meet capacity of "+capacity);
              }
              targetLevel = capacity;
            }
            double refillQty = targetLevel-invLevel;
            // do some rounding/formatting
//            System.out.println("\nrefillqty is:" + refillQty + " for agent:" +
//                               inventoryPlugin.getClusterId() + " supplytype:" +
//                               inventoryPlugin.getSupplyType() + " for item:"+
//                               debugItem);
            if (inventoryPlugin.getSupplyType().equals("Ammunition")) {
              String formatted = getDecimalFormatter().format(refillQty);
              refillQty = (new Double(formatted)).doubleValue();
            } else {
              // make the refill a integer
              refillQty = Math.ceil(refillQty);
              //refillQty = Math.rint(refillQty);
            }
//            System.out.println("\nrefillqty after formatting is:" + refillQty + " for agent:" +
//                               inventoryPlugin.getClusterId() + " supplytype:" +
//                               inventoryPlugin.getSupplyType() + " for item:"+
//                               debugItem);
	    if(refillBucket < firstLegalRefillBucket) {
		double criticalLevel = thePG.getCriticalLevel(refillBucket);
		
		String refillBucketTime = getTimeUtils().dateString(thePG.convertBucketToTime(refillBucket));

		if((criticalLevel <= 0) || (invLevel <= 0)) {
		    //		    if((criticalLevel < -1E-8) && logger.isWarnEnabled()) {
		    if((criticalLevel < 0) && logger.isWarnEnabled()) {
			// Accounts for rounding errors
			logger.warn("At " + inventoryPlugin.getOrgName() +
				    "-" + inventoryPlugin.getSupplyType() +
				    "-Item:" + myItemId + ":" +
				    "Critical Level is " + criticalLevel + 
				    " and inventory level is " + invLevel +
				    " at refill bucket time " + refillBucketTime + "!" );
		    }
		    else if (logger.isDebugEnabled()) {
			logger.debug("At " + inventoryPlugin.getOrgName() +
				     "-" + inventoryPlugin.getSupplyType() +
				     "-Item:" + myItemId + ":" +
				     "Critical Level is " + criticalLevel + 
				     " and inventory level is " + invLevel + 
				     " at refill bucket time " + refillBucketTime + "!" );
		    }
		}
		//if the drop below critical level is greater than 5% then log a warning otherwise
		//a debug message
		else if((criticalLevel - invLevel) > (0.20 * criticalLevel)) {
		    if(logger.isDebugEnabled()) {
			logger.debug("At " + inventoryPlugin.getOrgName() +
				    "-" + inventoryPlugin.getSupplyType() +
				    "-Item:" + myItemId + ":" +
				    " not ordering a refill on day " + refillBucketTime +
				    " when we've fallen below critical level by " + (((criticalLevel - invLevel)/criticalLevel)) + "percent, because it is before supplier arrival time + ost.   Supplier arrival time is " + TimeUtils.dateString(inventoryPlugin.getSupplierArrivalTime()) + " and add ost which is " + orderShipTime);
		    }
		}
		else if (logger.isDebugEnabled()) {
		    logger.debug("At " + inventoryPlugin.getOrgName() +
				 "-" + inventoryPlugin.getSupplyType() +
				 "-Item:" + myItemId + ":" +
				 " not ordering a refill on day " + refillBucketTime +
				 " when we've fallen below critical level by " + (((criticalLevel - invLevel)/criticalLevel)) + "percent, because it is before supplier arrival time + ost.   Supplier arrival time is " + TimeUtils.dateString(inventoryPlugin.getSupplierArrivalTime()) + " and add ost which is " + orderShipTime);
		}
	    }
            else if(refillQty > 0.00) {
              lastRefillBucket=refillBucket;
              if (logger.isDebugEnabled()) {
                logger.debug("\n Creating Refill for bucket: " + refillBucket +
                             " of quantity: " + refillQty);
              }
              // make a task for this refill and publish it to glue plugin
              // and apply it to the LogisticsInventoryBG
              Task theRefill = createRefillTask(refillQty, thePG.convertBucketToTime(refillBucket),
                                                thePG, today, orderShipTime);
              newRefills.add(theRefill);
              invLevel = invLevel + refillQty;

            }
            else if (logger.isDebugEnabled()) {
              logger.debug("Not placing a refill order of qty: " + refillQty) ;
            }

            thePG.setLevel(refillBucket, invLevel);
            if (logger.isDebugEnabled()) {
              double printsum = targetLevel;
              logger.debug("\nSetting the InventoryLevel and the TargetLevel to: " + printsum);
            }
            prevTarget = targetLevel;
          }
          //reset the buckets
          startBucket = refillBucket;
          refillBucket = startBucket + 1;
        }
        // Set the target levels for projection period here since very similar
        // calculations are done for both projection and refill period.
        //setTargetForProjectionPeriod(thePG, maxLeadBucket+1, prevTarget);
        // RJB
        setTargetForProjectionPeriod(thePG, lastRefillBucket+1, prevTarget);
        // call the Comparator for this Inventory which will compare the old and
        // new Refills and then publish the new Refills and Rescind the old Refills.
        myComparator.compareRefills(newRefills, oldRefills, anInventory);
        inventoryPlugin.disposeOfUnusedMILTask(anInventory, newRefills.isEmpty());
      } // end of if not level 2 inventory
    } // done going through inventories
  }

  /** Make a Refill Task
   *  @param quantity The quantity of the Refill Task
   *  @param endDay  The desired delivery date of the Refill Task
   *  @param thePG  The LogisticsInventoryPG for the Inventory.
   *  @param today  Time representing now to use as the earliest possible delivery
   *  @param ost The OrderShipTime used to calculate the CommitmentDate
   *  @return Task The new Refill Task
   **/
  private Task createRefillTask(double quantity, long endDay,
                                LogisticsInventoryPG thePG,
                                long today, int ost) {
    // make a new task
    NewTask newRefill = inventoryPlugin.getPlanningFactory().newTask();
    newRefill.setVerb(Constants.Verb.Supply);
    newRefill.setDirectObject(thePG.getResource());
    //set the commitment date to endDay - ost.
    newRefill.setCommitmentDate(new Date(endDay-(thePG.getBucketMillis()*ost)));
//     newRefill.setCommitmentDate(new Date(getTimeUtils().subtractNDays(endDay, ost)));
    // create preferences
    Vector prefs = new Vector();
    Preference p_end,p_qty;
    p_end = createRefillTimePreference(endDay, today, thePG);
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
      io = geolocs.nextElement();
    } else {
      io = getHomeLocation();
    }
    pp_vector.add(createPrepPhrase(Constants.Preposition.TO, io));
    Asset resource = thePG.getResource();
    TypeIdentificationPG tip = resource.getTypeIdentificationPG();
    MaintainedItem itemID = MaintainedItem.
            findOrMakeMaintainedItem("Inventory", tip.getTypeIdentification(),
                                     null, tip.getNomenclature(), inventoryPlugin);
    pp_vector.add(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.add(createPrepPhrase(Constants.Preposition.REFILL, null));

    newRefill.setPrepositionalPhrases(pp_vector.elements());

    if((endDay < inventoryPlugin.getRefillStartTime()) &&
        (logger.isWarnEnabled())){
      logger.warn("Creating new refill task for day " + getTimeUtils().dateString(endDay) +
                  " before arrival in theatre time or arrival of supplier of "
                  + getTimeUtils().dateString(inventoryPlugin.getRefillStartTime()) +
                  ".  The task that is about to be published is " + newRefill);
    }

    return newRefill;
  }

  /** Utility method to create the Refill tasks time preference
   *  Use a Piecewise Linear scoring function - see the IM desing doc
   *  for details.
   *  @param bestDay The time representation of the desired result
   *  @param today The time representation of today or now
   *  @return Preference  The new time preference
   **/
  protected Preference createRefillTimePreference(long bestDay, long today, LogisticsInventoryPG thePG) {
    //TODO - really need last day in theatre from an OrgActivity -
    long end = inventoryPlugin.getOPlanEndTime();
    double bucketsBetween = ((end - bestDay)  / thePG.getBucketMillis()) - 1;
    //Use .0033 as a slope for now
    double late_score = .0033 * bucketsBetween;
    // define alpha .25
    double alpha = .25;
    Vector points = new Vector();

    AspectScorePoint earliest = new AspectScorePoint(AspectValue.newAspectValue(AspectType.END_TIME, today), alpha);
    AspectScorePoint best = new AspectScorePoint(AspectValue.newAspectValue(AspectType.END_TIME, bestDay), 0.0);
//     AspectScorePoint first_late = new AspectScorePoint(getTimeUtils().addNDays(bestDay, 1),
//                                                        alpha, AspectType.END_TIME);
    AspectScorePoint first_late = new AspectScorePoint(AspectValue.newAspectValue(AspectType.END_TIME,
                                                                                  bestDay+thePG.getBucketMillis()),
                                                       alpha);
    end = getTimeUtils().addNDays(bestDay, 20);
    AspectScorePoint latest = new AspectScorePoint(AspectValue.newAspectValue(AspectType.END_TIME, end),
                                                   (alpha + late_score));

    points.addElement(earliest);
    points.addElement(best);
    points.addElement(first_late);
    points.addElement(latest);
    ScoringFunction endTimeSF = ScoringFunction.
            createPiecewiseLinearScoringFunction(points.elements());
    return inventoryPlugin.getPlanningFactory().
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
  protected Preference createRefillQuantityPreference(double refill_qty) {
    ScoringFunction qtySF = ScoringFunction.
            createStrictlyAtValue(AspectValue.newAspectValue(AspectType.QUANTITY, refill_qty));
    return  inventoryPlugin.getPlanningFactory().
            newPreference(AspectType.QUANTITY, qtySF);
  }

  /** Utility method to create a Refill Task Prepositional Phrase
   *  @param prep  The preposition
   *  @param io  The indirect object
   *  @return PrepositionalPhrase  A new prep phrase for the task
   **/
  protected PrepositionalPhrase createPrepPhrase(String prep, Object io) {
    NewPrepositionalPhrase newpp = inventoryPlugin.getPlanningFactory().
            newPrepositionalPhrase();
    newpp.setPreposition(prep);
    newpp.setIndirectObject(io);
    return newpp;
  }

  /** Utility method to get and keep organization info from the InventoryPlugin. **/
  protected Organization getMyOrganization() {
    if (myOrg == null) {
      myOrg = inventoryPlugin.getMyOrganization();
      // if we still don't have it after we ask the inventory plugin, throw an error!
      if (myOrg == null && logger.isErrorEnabled()) {
        logger.error("RefillGenerator can not get MyOrganization from " +
                     "the InventoryPlugin");
      }
    }
    return myOrg;
  }

  /** Utility method to get the Org Name from my organization and keep it around. **/
  protected String getOrgName() {
    if (myOrgName == null) {
      myOrgName =getMyOrganization().getItemIdentificationPG().getItemIdentification();
    }
    return myOrgName;
  }

  /** Utility method to get the default (home) location of the Org **/
  protected GeolocLocation getHomeLocation() {
    if (homeGeoloc == null ) {
      Organization org = getMyOrganization();
      if (org.getMilitaryOrgPG() != null) {
        GeolocLocation geoloc = (GeolocLocation)org.
                getMilitaryOrgPG().getHomeLocation();
        if (geoloc != null) {
          homeGeoloc = geoloc;
        } else {
          //if we can't find the home loc either print an error
          if (logger.isErrorEnabled()) {
            logger.error("RefillGenerator can not generate a " +
                         "Home Geoloc for org: " + org);
          }
        }
      }
    }
    return homeGeoloc;
  }

  protected void calculateInventoryLevelsForRG(int startBucket, int endBucket, LogisticsInventoryPG thePG) {
    //calculate inventory levels for today through start (today + OST)
    startBucket = (startBucket < thePG.getStartBucket()) ? thePG.getStartBucket() : startBucket;
    while (startBucket <= endBucket) {
      double level;
      if (startBucket == 0) {
        level = thePG.getLevel(0);
      } else {
        level = thePG.getLevel(startBucket - 1) -
                thePG.getActualDemand(startBucket);
      }
      double committedRefill = findCommittedRefill(startBucket, thePG, false);
      thePG.setLevel(startBucket, (level + committedRefill) );
      startBucket = startBucket + 1;
    }

  }

  protected DecimalFormat getDecimalFormatter() {
    if (myDecimalFormatter == null) {
      myDecimalFormatter = new DecimalFormat("##########.##");
    }
    return myDecimalFormatter;
  }
}


