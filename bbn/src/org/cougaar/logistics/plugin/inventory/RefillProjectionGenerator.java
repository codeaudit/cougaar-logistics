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

import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.plan.*;

import java.util.*;


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


public class RefillProjectionGenerator extends InventoryLevelGenerator implements RefillProjectionGeneratorModule {

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
   *  @param endOfLevelTwo  The day representing the end of the Level 2 window
   *   from the VariableTimeHorizon OperatingMode (knob)
   **/
  public void calculateRefillProjections(Collection touchedInventories, int daysOnHand,
                                         long endOfLevelSix, long endOfLevelTwo, 
					 ComparatorModule theComparator) {

    // NOTE!!!!
    // For now since we aren't doing anything special for the level 2 time window like
    // translating level 6 to level 2 or vice versa - just use the level 6 method to
    // calculate everything.  This means the end time used by that method will
    // be the end of the level 2 window as we don't want to process anything after that.

    if (! touchedInventories.isEmpty()) {
      calculateLevelSixProjections(touchedInventories, daysOnHand, endOfLevelTwo, theComparator);
    }



    // get the demand projections for each customer from bg
    // time shift the demand for each customer
    // sum the each customer's time shifted demand

  //   ArrayList level6Inventories = new ArrayList();
//     Inventory level2Inv = null;

//     Iterator touchedIter = touchedInventories.iterator();
//     while (touchedIter.hasNext()) {
//       Inventory inv = (Inventory) touchedIter.next();
//       LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
//         searchForPropertyGroup(LogisticsInventoryPG.class);
//       if (thePG.getIsLevel2()) {
//         level2Inv = inv;
//       } else {
//         level6Inventories.add(inv);
//       }
//     }

//     if (! level6Inventories.isEmpty()) {
//       calculateLevelSixProjections(level6Inventories, daysOnHand,
//                                    endOfLevelSix, theComparator);
//     }
//     // if the level 2 and level 6 boundaries are = don't process
//     // and just for safety make sure the end of level 2 doesn't fall before
//     // the end of the level 6 window
//     if ((level2Inv != null) && ( endOfLevelTwo > endOfLevelSix)) {
//       if (inventoryPlugin.getClusterId().toString().equals("1-35-ARBN")) {
//         System.out.println("RPG calling calculate level 2 projections");
//       }
//       calculateLevelTwoProjections(level2Inv, daysOnHand, 
//                                    getTimeUtils().addNDays(endOfLevelSix, 1),
//                                    endOfLevelTwo, theComparator);
//     }
//   }
  }

   
  /** Calculate Projection Refills in level Six detail until the end of the
   *  Level 6 VTH window.
   *  @param touchedInventories  The Inventories that have changed wrt projections.
   *   We will only recalculate refill projections for these.
   *  @param daysOnHand DaysOnHand policy.
   *  @param endOfLevelSix The date representing the end of the Level 6 VTH window.
   **/
  protected void calculateLevelSixProjections(Collection touchedInventories, 
                                            int daysOnHand, long endOfLevelSix, 
					    ComparatorModule myComparator) {

    ArrayList refillProjections = new ArrayList();
    ArrayList oldProjections = new ArrayList();

    //temp mwd
    inventoryPlugin.updateStartAndEndTimes();

    Iterator tiIter = touchedInventories.iterator();
    while (tiIter.hasNext()) {
      // clear out the old and new projections for the last Inventory
      refillProjections.clear();
      oldProjections.clear();
      Inventory anInventory = (Inventory) tiIter.next();
      LogisticsInventoryPG thePG = (LogisticsInventoryPG)anInventory.
        searchForPropertyGroup(LogisticsInventoryPG.class);

      //start time is the start time of the inventorybg
      long startDay = thePG.getStartTime();
      if(startDay < inventoryPlugin.getRefillStartTime()) {
        startDay = inventoryPlugin.getRefillStartTime();
      }

      long now = inventoryPlugin.currentTimeMillis();

      //Bug fix for 13464
      long cutoffTime = now + ((inventoryPlugin.getOrderShipTime()) * thePG.getBucketMillis());
      //round up if need be (not exactly on the zero mark)
      cutoffTime = thePG.convertBucketToTime(thePG.convertTimeToBucket(cutoffTime,true));

      if (cutoffTime > startDay) {
          startDay = cutoffTime;
      }

      int startBucket = thePG.convertTimeToBucket(startDay, true);     


      // clear all of the projections
      oldProjections.addAll(thePG.clearRefillProjectionTasks(cutoffTime));
      //grab the overlapping tasks and change their end time
      ArrayList projectionsToCut = (ArrayList) thePG.getOverlappingRefillProjections();
      Iterator toCut = projectionsToCut.iterator();
      while(toCut.hasNext()) {
	  Task taskToCut = (Task) toCut.next();
	  if (taskToCut != null) {
	      Preference new_end = getTaskUtils().createTimePreference(cutoffTime,
							      inventoryPlugin.getOPlanArrivalInTheaterTime(),
							      inventoryPlugin.getOPlanEndTime(),
							      AspectType.END_TIME, inventoryPlugin.getClusterId(),
							      inventoryPlugin.getPlanningFactory(), thePG);
	      ((NewTask)taskToCut).setPreference(new_end);
	      inventoryPlugin.publishChange(taskToCut);
	      thePG.updateRefillProjection(taskToCut);
	  }
      }
	      
      int currentBucket = startBucket;
//       int customerDemandBucket = thePG.convertTimeToBucket(getTimeUtils().
//                                                            addNDays(startDay, daysOnHand), true);
      //int customerDemandBucket = thePG.convertTimeToBucket(startDay, false) + daysOnHand;

      //Bug fix for #13464 
      int customerDemandBucket = startBucket + daysOnHand;

      double projDemand = 0;
      double nextProjDemand = 0;
      int endOfLevelSixBucket = thePG.convertTimeToBucket(endOfLevelSix, false);
      thePG.setEndOfLevelSixBucket(endOfLevelSixBucket);
      
      //get the initial demand for the customer for startBucket + daysOnHand
      //Is there a better way to do this to avoid the duplication?
      projDemand = thePG.getProjectedDemand(customerDemandBucket);
      
      //move forward a bucket
      currentBucket = currentBucket + 1;
      customerDemandBucket = customerDemandBucket + 1;

      int refillCtr = 0;
      
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
						 projDemand, thePG);
            if (startBucket >= currentBucket) {
              logger.error("Invalid task : "+getTaskUtils().taskDesc(refill)+", startDay: "+
                           getTimeUtils().dateString(startDay)+", startBucket: "+startBucket+
                           ", currentBucket: "+currentBucket);
            } else {
              refillProjections.add(refill);
            }
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
	// HERE LIES A BUG.  We should never generate zero demand projection tasks
	// and never generate projection tasks that go beyond the end of the war
	if (projDemand > 0.0) {
	  Task lastRefill = createProjectionRefill(thePG.convertBucketToTime(startBucket),
						   thePG.convertBucketToTime(currentBucket),
						   projDemand, thePG);
          if (startBucket >= currentBucket) {
            logger.error("Invalid task : "+getTaskUtils().taskDesc(lastRefill)+", startDay: "+
                         getTimeUtils().dateString(startDay)+", startBucket: "+startBucket+
                         ", currentBucket: "+currentBucket);
          } else {
            refillProjections.add(lastRefill);
          }
        }
      }
      // send the new projections and the old projections to the Comparator
      // the comparator will rescind the old and publish the new projections
      myComparator.compareRefillProjections(refillProjections, oldProjections, 
                                            anInventory);
     
      if (thePG.getIsLevel2()) {
        setTargetForProjectionPeriod(thePG, 0, thePG.getInitialLevel());
      }
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
//   private void calculateLevelTwoProjections(Inventory level2Inv, int daysOnHand, 
//                                             long start, long endOfLevelTwo, 
//                                             RefillComparator theComparator) {

//     ArrayList newProjections = new ArrayList();
//     ArrayList oldProjections = new ArrayList();

//     LogisticsInventoryPG level2PG = (LogisticsInventoryPG) level2Inv.
//       searchForPropertyGroup(LogisticsInventoryPG.class);

//     // for now clear all the level 2 projections and only create
//     // new ones for the current level2 window
//     // this will have to change once we have mismatched level 2 windows
//     oldProjections.addAll(level2PG.clearRefillProjectionTasks());

//     int startBucket = level2PG.convertTimeToBucket(start, true);
//     // int currentBucket = startBucket;
// //     int customerDemandBucket = level2PG.convertTimeToBucket(getTimeUtils().
// //                                                            addNDays(start, daysOnHand));
//     // We need to time shift refills by days on hand.  So if level 2 demand starts the 
//     // day the level 2 window starts then we actually need to order outside of the level 2
//     // window.  Perhaps this is wrong - and we should order the first 3 days in level 6
//     // but right now we don't have the data to do this.
//     int currentBucket = startBucket - daysOnHand;
//     //reset startBucket
//     startBucket = currentBucket;
//     int customerDemandBucket = startBucket;

//     int endOfLevelTwoBucket = level2PG.convertTimeToBucket(endOfLevelTwo, true);
//     // get intital demand level
//     double projDemand = level2PG.getProjectedDemand(customerDemandBucket);
//     double nextProjDemand = 0;
//     // move ahead one day
   //  currentBucket = currentBucket + 1;
//     customerDemandBucket = customerDemandBucket + 1;

//     //In the future with varying customer level 2 windows we will have to 
//     //loop through all of our Inventories and sum the tonnage of demand
//     // for a level 2 combined demand per bucket.

//     //loop through the buckets until we reach the end of level two
//     while (currentBucket < endOfLevelTwoBucket) {
//       nextProjDemand = level2PG.getProjectedDemand(customerDemandBucket);
//       //check the results for the Day.
//       if (projDemand != nextProjDemand) {
//         //if there's a change in the demand, create a refill projection 
//         //BUT only if demand is non-zero 
//         if (projDemand > 0.0) {
    //       old
//           Task newLevel2Refill = 
//             createAggregateProjectionRefill(level2PG.convertBucketToTime(startBucket), 
//                                             level2PG.convertBucketToTime(currentBucket),
//                                             level2PG.getStartTime(), projDemand, level2PG,
//                                             level2Inv);
    //       new (can't have earliest arrive before we get to theatre
//           Task newLevel2Refill = 
//             createAggregateProjectionRefill(level2PG.convertBucketToTime(startBucket), 
//                                             level2PG.convertBucketToTime(currentBucket),
//                                             inventoryPlugin.getOPlanArrivalInTheaterTime(), projDemand,
//                                             level2PG,
//                                             level2Inv);
//           newProjections.add(newLevel2Refill);
//         }
//         //then reset the start bucket  and the new demand 
//         startBucket = currentBucket;
//         projDemand = nextProjDemand;
//       }
//       // bump forward a bucket.
//       currentBucket = currentBucket + 1;
//       customerDemandBucket = customerDemandBucket + 1;
//     }
//     // when we get to the end of the level two window create the last
//     // projection task (if there is one)
//     if (startBucket != currentBucket) {
//       if (projDemand > 0.0) {
    //     old
//         Task lastLevel2Refill = 
//           createAggregateProjectionRefill(level2PG.convertBucketToTime(startBucket), 
//                                           level2PG.convertBucketToTime(currentBucket),
//                                           level2PG.getStartTime(), projDemand, level2PG,
//                                           level2Inv);
    //       new (can't have earliest arrive before we get to theatre
//         Task lastLevel2Refill = 
//           createAggregateProjectionRefill(level2PG.convertBucketToTime(startBucket), 
//                                           level2PG.convertBucketToTime(currentBucket),
//                                           inventoryPlugin.getOPlanArrivalInTheaterTime(),
//                                           projDemand, level2PG,
//                                           level2Inv);
//         newProjections.add(lastLevel2Refill);
//       }
//     }
//     // send the new projections and the old projections to the Comparator
//     // the comparator will rescind the old and publish the new projections
//     theComparator.compareRefillProjections(newProjections, oldProjections, 
//                                           level2Inv);
//     setTargetForProjectionPeriod(level2PG, 1, level2PG.getInitialLevel());
    
//   }      
 

  /** Make a Projection Refill Task and publish it to the InventoryPlugin.
   *  The InventoryPlugin will hook the task in to the proper workflow
   *  and publish it to the blackboard.
   *  @param start The start time for the Task
   *  @param end The end time for the Task
   *  @param demand The demandrate value of the task
   *  @param thePG  The Property Group of the Inventory Asset
   *  @return Task The new Projection Refill
   **/
  protected Task createProjectionRefill(long start, long end,
                                      double demand,
                                      LogisticsInventoryPG thePG) {
    //create a projection refill task
    NewTask newRefill = inventoryPlugin.getPlanningFactory().newTask();
    newRefill.setVerb(Constants.Verb.ProjectSupply);
    newRefill.setDirectObject(thePG.getResource());
    //    newRefill = fillInTask(newRefill, start, end, thePG.getStartTime(), 
    //                       demand, thePG);
    newRefill = fillInTask(newRefill, start, end, inventoryPlugin.getOPlanArrivalInTheaterTime(), 
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
    NewTask newAggRefill = inventoryPlugin.getPlanningFactory().newTask();
    newAggRefill.setVerb(Constants.Verb.ProjectSupply);
    //TODO - for now physical pg can stay the same as the demand task
    //physical pg needs to represent demand
    // level2Asset.setPhysicalPG(demand);
    newAggRefill.setDirectObject(level2Inv);
    newAggRefill = fillInTask(newAggRefill, start, end, earliest, demand, level2PG);
    return newAggRefill;
  }

  /** Utility method to fill in task details
   *  @param newRefill The task to fill in
   *  @param start Start time for Task
   *  @param end End Time for Task
   *  @param qty Quantity Pref for Task
   *  @param thePG The property group attached to the Inventory 
   *  @return NewTask the filled in Task
   **/
  protected NewTask fillInTask(NewTask newRefill, long start, long end, long earliest, 
                             double qty, LogisticsInventoryPG thePG) {
    // create preferences
    Vector prefs = new Vector();
    Preference p_start,p_end,p_qty;
    p_start = getTaskUtils().createTimePreference(start, earliest, inventoryPlugin.getOPlanEndTime(),
                                                  AspectType.START_TIME, inventoryPlugin.getClusterId(),
                                                  inventoryPlugin.getPlanningFactory(), thePG);
    p_end = getTaskUtils().createTimePreference(end, earliest, inventoryPlugin.getOPlanEndTime(),
                                                AspectType.END_TIME, inventoryPlugin.getClusterId(),
                                                inventoryPlugin.getPlanningFactory(), thePG);
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
								  (end-1000));
    if (geolocs.hasMoreElements()) {
      io = (GeolocLocation)geolocs.nextElement();
    } else {
      io = getHomeLocation();
    }
    pp_vector.addElement(createPrepPhrase(Constants.Preposition.TO, io));
    TypeIdentificationPG tip = thePG.getResource().getTypeIdentificationPG();
    MaintainedItem itemID = MaintainedItem.
      findOrMakeMaintainedItem("Inventory", tip.getTypeIdentification(), 
                               null, tip.getNomenclature(), (UtilsProvider)inventoryPlugin);
    pp_vector.add(createPrepPhrase(Constants.Preposition.MAINTAINING, itemID));
    pp_vector.add(createPrepPhrase(Constants.Preposition.REFILL, null));
    
    newRefill.setPrepositionalPhrases(pp_vector.elements());
    return newRefill;
  } 

  /** Utility method to create Refill Projection Rate preference
   *  We use a V scoring function for this preference.
   *  @param refill_qty  The quantity we want for this Refill Task
   *  @return Preference  The new demand rate preference for the Refill Task
   **/
  protected Preference createRefillRatePreference(double refill_qty, long bucketMillis) {
    AspectValue bestAV;
    Duration dur = new Duration(bucketMillis, Duration.MILLISECONDS);
    //highAV could be bumped to more than refill_qty + 1 if needed
    if (inventoryPlugin.getSupplyType().equals("BulkPOL")) {
      Volume vol = new Volume(refill_qty, Volume.GALLONS);
      bestAV = AspectValue.newAspectValue(AlpineAspectType.DEMANDRATE, 
					  new FlowRate(vol, dur));
    } else {
      Count cnt = new Count(refill_qty, Count.EACHES);
      bestAV = AspectValue.newAspectValue(AlpineAspectType.DEMANDRATE,
					  new CountRate(cnt, dur));
    }
    ScoringFunction qtySF = ScoringFunction.
	createStrictlyAtValue(bestAV);
    return  inventoryPlugin.getPlanningFactory().
	newPreference(AlpineAspectType.DEMANDRATE, qtySF);
  }

  /** Utility method to create a Refill Projection Prepositional Phrase
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
       if (myOrg == null) {
	 logger.error("RefillProjectionGenerator can not get MyOrganization " + 
		      "from the InventoryPlugin");
       }
    } 
    return myOrg;
  }

  /** Utility accessor to get the Org Name from my organization and keep it around **/
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
    
  
  
