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


/** The Refill Projection Generator Module is responsible for generating 
 *  projection refill tasks.  These projections will be calculated by
 *  time shifting the projections from each customer and summing the
 *  results.
 *  Called by the Inventory Plugin when there is new projection demand.
 *  Uses the InventoryBG module to gather projected demand.
 *  Generates Refill Projection tasks 
 **/

public class RefillProjectionGenerator extends InventoryModule {

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

      // what do we do if this returns null or 0???
     //  double projDemand = theBG.getProjectedDemand(getTimeUtils().
//                                                    addNDays(currentDay, daysOnHand));
//       currentDay = getTimeUtils().addNDays(currentDay, 1);
//       //possible boundary issue... is it '<' or '<=' ??
//       while (currentDay < endOfLevelSix) {
//         double nextProjDemand = thePG.getProjectedDemand(getTimeUtils().
//                                                   addNDays(currentDay, daysOnHand));
//         if (projDemand != nextProjDemand) {
//           //if there's a change in the demand, create a refill projection
//           createProjectionRefill(startDay, 
//                                  getTimeUtils().subtractNDays(currentDay, 1),
//                                  projDemand);
//           //then reset the startday and the new demand 
//           startDay = currentDay;
//           projDemand = nextProjDemand;
//         }
//         //in either case bump forward a day.
//         currentDay = getTimeUtils().addNDays(currentDay, 1);
//       }
//       // when we get to the end of the level six window create the last
//       // projection task (if there is one)
//       if (startDay != currentDay) {
//         createProjectionRefill(startDay,
//                                getTimeUtils().subtractNDays(currentDay, 1),
//                                projDemand);
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
        
  private void createProjectionRefill(long start, long end, double demand) {
    //create a projection refill task
  }

  // DO we really need two separate methods???
  private void createAggregateProjectionRefill(long start, long end, double demand) {
    //create a level two projection refill task
  }

}
    
  
  
