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

import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewWorkflow;

import java.util.ArrayList;
import java.util.Iterator;

/** The Refill Comparator Module is responsible for deciding whether to
 *  rescind all previous refills and publish all new refills generated
 *  by the total replan refill generator  module or whether to 
 *  compare and merge the 'old' and 'new' refill tasks.  The first
 *  version will simply rescind all old refills and publish all new
 *  refills.
 *  Called by the Refill Generator with the new refills and old refills.
 *  Publishes new Refill tasks and rescinds the old through the InventoryPlugin.
 *  Also applies the new Refill tasks to the Inventory's BG.
 **/

public class RefillComparator extends InventoryModule implements ComparatorModule {

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public RefillComparator(InventoryPlugin imPlugin) {
    super(imPlugin);
  }

  /** Compares the old and new Refill tasks.
   *  Publishes any new Refills and Rescinds any old Refill Tasks.
   *  For now this implementation rescinds ALL old Refills and publishes ALL
   *  new Refill tasks.  In the future a smart comparison will be done.
   *  @param newRefills The collection of newly generated Refills from the
   *    RefillGeneratorModule
   *  @param oldRefills The previously generated Refill Tasks
   *  @param inv The Inventory the Refills are refilling.
   **/
  public void compareRefills(ArrayList newRefills, ArrayList oldRefills, Inventory inv) {
    //Rescind all old Refill Tasks
    Iterator oldIter = oldRefills.iterator();
    while (oldIter.hasNext()) {
      Task oldRefill = (Task) oldIter.next();
      // check for a null in the refills list!
      if (oldRefill != null) {
	  // clean out the reference in the maintain inventory workflow
	  ((NewWorkflow)oldRefill.getWorkflow()).removeTask(oldRefill);
	inventoryPlugin.publishRemove(oldRefill);
      }
    }

    //Process all new Refill Tasks
    LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
	searchForPropertyGroup(LogisticsInventoryPG.class);
    Iterator newIter = newRefills.iterator();
    while (newIter.hasNext()) {
      Task newRefill = (Task) newIter.next();
      // apply the Task to the LogisticsInventoryBG
      thePG.addRefillRequisition(newRefill);
      // hook the task in with the MaintainInventory workflow and publish
      inventoryPlugin.publishRefillTask(newRefill, inv);
    }
  }
                       
 /** Compares the old and new Refill Projection tasks.
   *  Publishes any new Refill Projections and Rescinds any old Refill Projection Tasks.
   *  For now this implementation rescinds ALL old Refill Projections
   *  and publishes ALL new Refill Projection tasks.  
   *  In the future a smart comparison will be done.
   *  Right now this method is almost identical to compareRefills.
   *  @param newRefillProjs The collection of newly generated Refill Projections
   *    from the RefillProjectionsGenerator Module
   *  @param oldRefillProjs The previously generated Refill Projection Tasks
   *  @param inv The Inventory the Refills Projections are refilling.
   **/
  public void compareRefillProjections(ArrayList newRefillProjs, 
					ArrayList oldRefillProjs, 
					Inventory inv) {
    //Rescind all old Refill Projection Tasks
    Iterator oldIter = oldRefillProjs.iterator();
    while (oldIter.hasNext()) {
      Task oldRefillProj = (Task) oldIter.next();
      // add in a check for a null - nulls and tasks are put in the
      // refill lists so make sure we don't publish null!
      if (oldRefillProj != null) {
	  // remove this from the workflow's sub task list first
	  ((NewWorkflow)oldRefillProj.getWorkflow()).removeTask(oldRefillProj);
	inventoryPlugin.publishRemove(oldRefillProj);
      }
    }

    //Process all new Refill Projection Tasks
    LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
	searchForPropertyGroup(LogisticsInventoryPG.class);
    Iterator newIter = newRefillProjs.iterator();
    while (newIter.hasNext()) {
      Task newRefillProj = (Task) newIter.next();
      // apply the Task to the LogisticsInventoryBG
      thePG.addRefillProjection(newRefillProj);
      // hook the task in with the MaintainInventory workflow and publish
      inventoryPlugin.publishRefillTask(newRefillProj, inv);
    }
  }

}
    
  
  
