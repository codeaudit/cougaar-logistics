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

import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.logistics.ldm.Constants;
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
    int start = (thePG.getStartBucket() > startBucket) ? thePG.getStartBucket() : startBucket;
    while (start <= endBucket) {
      double level;
      if (start == 0) {
        level = thePG.getLevel(0);
      } else {
        level = thePG.getLevel(start - 1) -
          thePG.getActualDemand(start);
      }
      double committedRefill = findCommittedRefill(start, thePG, true);
      thePG.setLevel(start, (level + committedRefill) );
      start = start + 1;
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
    ArrayList refills=null;
    Task refill = null;
    if (bucket < reqs.size()) {
      refills = (ArrayList) reqs.get(bucket);
    }
    
    // long today = inventoryPlugin.getCurrentTimeMillis();
//     // max lead day is today + maxLeadTime
//     int maxLeadBucket = thePG.convertTimeToBucket(getTimeUtils().
// 						  addNDays(today, inventoryPlugin.getMaxLeadTime()), false);
//     if ((refill == null) && (bucket > maxLeadBucket)) {
//       refill = thePG.getRefillProjection(bucket);
//     }
    // check that the bucket we're looking at is in the projection period and its
    // not just an off day during the Requisition period
    int lastReqBucket = thePG.getLastRefillRequisition();
    if (((refills == null) || (refills.size() == 0)) 
	&& (bucket > lastReqBucket) && (countProjections)) {
      refills = thePG.getRefillProjection(bucket);
    }


    //!!!NOTE that the inside slots of thePG.getRefillRequisitions are sometimes
    // filled with null instead of a task - so make sure you really have a task!
    if (refills != null) {
	for(int i=0; i < refills.size(); i++) {
	    refill = (Task) refills.get(i);
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
			refillQty += getTaskUtils().getQuantity(refill, ar, thePG.getBucketMillis());
		    } else {
			try {
			    refillQty += ar.getValue(AspectType.QUANTITY);
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
		}
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
   **/
  protected void setTargetForProjectionPeriod(LogisticsInventoryPG thePG, 
					      int startBucket, double prevTarget){
    if (logger.isDebugEnabled()) { 
      logger.debug("For item: "+thePG.getResource() + 
                   " set Projection inventory and target levels starting with bucket: "+
                   startBucket);
    }

    // Bug #13358  clearTargetLevels needs to be called for both refill period and projection
    //             period.  TargetLevels in the refill period are cleared from firstLegalRefillBucket
    //             forward but when no refills are generated it may be the case that firstLegalRefillBucket
    //             is greater than startBucket for projection period, thereby not properly 
    //             clearing target levels which can result in a 'jagged' target level.
    thePG.clearTargetLevels(startBucket);

    int reorderPeriod = (int)thePG.getReorderPeriod();
    int lastDemandBucket = thePG.getLastDemandBucket();
    double lastTarget;
    int inventoryBucket,inventoryBucketStart;
    
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
      inventoryBucketStart = targetLevelBucket - reorderPeriod;

      for (int i=0; i < reorderPeriod; i++) {
	inventoryBucket = inventoryBucketStart + i;
        double diff = target - lastTarget; 
        double calcTarget = lastTarget + ((diff * i)/reorderPeriod);
        //inv level in projection land is the average of the critcial and target levels
        double level = (thePG.getCriticalLevel(inventoryBucket) + calcTarget) / 2;

	//Only for Level2Projections do we call this method with a startBucket == 0
	//For Level2Projections we were resetting the zero th bucket of invnentory
	//which is really what holds the initial inventory level - 
	//Don't reset the initial inventory level!
	if(inventoryBucket > 0) {
	    thePG.setLevel(inventoryBucket, level);
	}
      }
      lastTarget = target;
    }
  }

  /** Utility method to generate the Refill Amount
   *  This method starts the following calculation
   *  RF(k+1)=(C(k+RP+1)-IL(k+1)) + (D(K+2)+...+(k+RP+1))
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
    double demandForPeriod = calculateDemandForPeriod(thePG, 
						      refillBucket, 
						      reorderPeriodEndBucket);
    //     targetLevel = criticalAtEndOfPeriod + demandForPeriod;
    // used once to fix a problem with fresh fruit - but seems wrong since the rest of the
    // refill generator algorithm expects us to calculate for the whole reorder period
    // switched back to original (above) for 10.4
     //    double demandForPeriod = calculateDemandForPeriod(thePG, 
     //                                               refillBucket, 
     //                                               reorderPeriodEndBucket-1);

    targetLevel = criticalAtEndOfPeriod + demandForPeriod + .0005;
    if (logger.isDebugEnabled()) {
      if ((inventoryPlugin.getOrgName().indexOf("ARBN") > -1) &&
          (thePG.getResource().getTypeIdentificationPG().getTypeIdentification().indexOf("C380") > -1)) {
        logger.debug("##ILG## "+getTimeUtils().dateString(thePG.convertBucketToTime(refillBucket))+
                     " Target "+targetLevel+" = critical "+criticalAtEndOfPeriod+" + demand "+
                     demandForPeriod);
      }
    }
    return targetLevel;
  }

  /** Utility method to calculate the demand for the Reorder Period
   *  This method does the calculation for:
   *  (D(K+2)+...+(k+RP+1))
   *  @param thePG The LogisticsInventoryPG of the Inventory we are Refilling.
   *  @param refillBucket  The bucket we are generating a refill for.  This is
   *   the bucket at k+1
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

