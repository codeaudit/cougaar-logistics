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
      double level = thePG.getLevel(startBucket) -
	thePG.getActualDemand(startBucket + 1);
      double committedRefill = findCommittedRefill(startBucket, thePG);
      thePG.setLevel(startBucket, (level + committedRefill) );
      startBucket = startBucket + 1;
    }

  }

  /** Utility method to help find commited refills 
   *  NOTE this only finds a quantity IF there is a reported or
   *  Estimated AllocationResult for the Task!
   *  @param bucket The time bucket to match the Task with
   *  @param thePG The PG for the Inventory the Tasks are against
   *  @return double The quantity of the committed Refill Task for the time period.
   **/
  protected double findCommittedRefill(int bucket, LogisticsInventoryPG thePG) {
    double refillQty = 0;
    ArrayList reqs = thePG.getRefillRequisitions();
    Iterator reqsIter = reqs.iterator();
    while (reqsIter.hasNext()) {
      Task refill = (Task) reqsIter.next();
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
