/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;

import java.util.*;

public class InventoryLevelGenerator extends InventoryModule {

  public InventoryLevelGenerator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  protected void calculateInventoryLevels(int startBucket, int endBucket, LogisticsInventoryPG thePG) {
    //calculate inventory levels for today through start (today + OST)
    while (startBucket <= endBucket) {
      double level;
      if (startBucket == 0) {
        level = thePG.getLevel(0);
      } else {
        level = thePG.getLevel(startBucket - 1) -
          thePG.getActualDemand(startBucket);
      }
      double committedRefill = findCommittedRefill(startBucket, thePG, true);
      thePG.setLevel(startBucket, (level + committedRefill) );
      startBucket = startBucket + 1;
    }

  }

  /** Utility method to help find commited refills 
   *  NOTE this only finds a quantity IF there is a reported or
   *  Estimated AllocationResult for the Task!
   *  @param bucket The time bucket to match the Task with
   *  @param thePG The PG for the Inventory the Tasks are against
   *  @param countProjections Count projection refill results
   *  @return double The quantity of the committed Refill Task for the time period.
   **/
  protected double findCommittedRefill(int bucket, LogisticsInventoryPG thePG, boolean countProjections) {
    double refillQty = 0;
    ArrayList reqs = thePG.getRefillRequisitions();
    Task refill = null;
    if (bucket < reqs.size()) {
      refill = (Task) reqs.get(bucket);
    }
    
    // long today = inventoryPlugin.getCurrentTimeMillis();
//     // max lead day is today + maxLeadTime
//     int maxLeadBucket = thePG.convertTimeToBucket(getTimeUtils().
// 						  addNDays(today, inventoryPlugin.getMaxLeadTime()));
//     if ((refill == null) && (bucket > maxLeadBucket)) {
//       refill = thePG.getRefillProjection(bucket);
//     }
    // check that the bucket we're looking at is in the projection period and its
    // not just an off day during the Requisition period
    int lastReqBucket = thePG.getLastRefillRequisition();
    if ((refill == null) && (bucket > lastReqBucket) && (countProjections)) {
      refill = thePG.getRefillProjection(bucket);
    }


    //!!!NOTE that the inside slots of thePG.getRefillRequisitions are sometimes
    // filled with null instead of a task - so make sure you really have a task!
    if (refill != null) {
      PlanElement pe = refill.getPlanElement();
      AllocationResult ar = null;
      if (pe !=null ) {
        //try to use the reported result - but if its null - use the 
        // estimated result
        if (pe.getReportedResult() != null) {
          ar = pe.getReportedResult();
        } else {
          ar = pe.getEstimatedResult();
        }
        // make sure that we got atleast a valid reported OR estimated allocation result
        if (ar != null) {
          if (refill.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) {
            //demandrate
	    //             if (inventoryPlugin.getSupplyType().equals("BulkPOL")) {
	    //               //default rate for volume is days
	    //               refillQty = ar.getValue(AlpineAspectType.DEMANDRATE);
	    //             } else {
	    //               //default rate for counts is millis
	    //               refillQty = ar.getValue(AlpineAspectType.DEMANDRATE) * thePG.getBucketMillis();
	    // 	  }
	    refillQty = getTaskUtils().getQuantity(refill, ar, thePG.getBucketMillis());
          } else {
            try {
              refillQty = ar.getValue(AspectType.QUANTITY);
            } catch (IllegalArgumentException iae) {
		if (logger.isErrorEnabled()) { 
		    logger.error("findCommittedRefill - The refill task " + 
				 refill.getUID() + 
				 "'s plan element has an allocation result without a quantity : " + ar);
		}
	    } catch (Exception e) {
	      if (ar == null && logger.isErrorEnabled()) {
		logger.error("This would blow our minds.");
	      } else {
		  if (logger.isErrorEnabled()) {
		      logger.error(" Task is " + getTaskUtils().taskDesc(refill) );
		      e.printStackTrace();
		  }
	      }
	    }
	  }
	  return refillQty;
	}
      }
    }
    // if we did not find a match return 0.0
    return refillQty;
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
  protected void setTargetForProjectionPeriod(LogisticsInventoryPG thePG, 
					      int startBucket, double prevTarget){
    if (logger.isDebugEnabled()) { 
      logger.debug("For item: "+thePG.getResource() + 
                   " set Projection inventory and target levels starting with bucket: "+
                   startBucket);
    }

    thePG.clearTargetLevels(startBucket);
    int reorderPeriod = (int)thePG.getReorderPeriod();
    int lastDemandBucket = thePG.getLastDemandBucket();
    double lastTarget;
    int inventoryBucket;
    
    // get the first target before we loop so we have 2 points for the
    // inventory level calculations.
    int reorderPeriodEndBucket = startBucket + reorderPeriod;
    double target = getTargetLevel(startBucket,reorderPeriodEndBucket, thePG);
    thePG.setTarget(startBucket, target);
    lastTarget = target;
    
//     System.out.println("Last demand bucket for "+inventoryPlugin.getResourceName(thePG.getResource())+
// 		       "-"+getAssetUtils().getPartNomenclature(thePG.getResource())+" is "+
// 		       lastDemandBucket+" at "+getOrgName());
    // Start the loop after the first reorderperiod
    for (int targetLevelBucket = startBucket + reorderPeriod; 
         targetLevelBucket <= lastDemandBucket; 
         targetLevelBucket = targetLevelBucket + reorderPeriod ) {
      reorderPeriodEndBucket = targetLevelBucket + reorderPeriod;
      target = getTargetLevel(targetLevelBucket,reorderPeriodEndBucket, thePG);
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
      lastTarget = target;
    }
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
  protected double getTargetLevel(int refillBucket, 
                                int reorderPeriodEndBucket, 
                                LogisticsInventoryPG thePG) {
    double targetLevel = 0;
    //    double criticalAtEndOfPeriod = Math.max(.9, thePG.getCriticalLevel(reorderPeriodEndBucket));
    double criticalAtEndOfPeriod = thePG.getCriticalLevel(reorderPeriodEndBucket);
    // OLD METHOD -- generates bad refill for Fresh Fruit, which shows up the Inventory Plotting bug!
//     double demandForPeriod = calculateDemandForPeriod(thePG, 
//                                                       refillBucket, 
//                                                       reorderPeriodEndBucket);
//     targetLevel = criticalAtEndOfPeriod + demandForPeriod;
    double demandForPeriod = calculateDemandForPeriod(thePG, 
                                                      refillBucket, 
                                                      reorderPeriodEndBucket-1);

    targetLevel = criticalAtEndOfPeriod + demandForPeriod + .0005;

    return targetLevel;
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
  protected double calculateDemandForPeriod(LogisticsInventoryPG thePG, 
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
}

