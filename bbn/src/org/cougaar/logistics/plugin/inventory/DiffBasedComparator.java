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
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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

public class DiffBasedComparator extends InventoryModule implements ComparatorModule {

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public DiffBasedComparator(InventoryPlugin imPlugin) {
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

    // Check for an empty schedule
    if ((newRefillProjs == null) || newRefillProjs.isEmpty()) {
      // Rescind any tasks that were not accounted for
      logger.debug("publishChangeProjection(), New Task List empty: "+newRefillProjs);
      Iterator list  = oldRefillProjs.iterator();
      while (list.hasNext()) {
        Task oldRefill = (Task)list.next();
        logger.debug("DIFF ********** Removing task --> \n"+getTaskUtils().taskDesc(oldRefill));
        if (oldRefill != null) {
	  // clean out the reference in the maintain inventory workflow
	  ((NewWorkflow)oldRefill.getWorkflow()).removeTask(oldRefill);
          inventoryPlugin.publishRemove(oldRefill);
        }
      }
      return;
    }

    Schedule publishedSchedule = getTaskUtils().newObjectSchedule(oldRefillProjs);
    Schedule newTaskSchedule = getTaskUtils().newObjectSchedule(newRefillProjs);

    // Compare new tasks to previously scheduled tasks, if a published task is found that
    // spans the new task's start time then adjust the published task (if needed) and publish
    // the change.  If no task is found than add new_task to list of tasks to be published.
    long start;
    Task new_task=null, published_task=null;
    ObjectScheduleElement ose=null;
    Collection c=null;
    while (!newTaskSchedule.isEmpty()) {
      start = newTaskSchedule.getStartTime();
      ose = (ObjectScheduleElement)ScheduleUtils.getElementWithTime(newTaskSchedule, start);
      if (ose != null) {
        new_task = (Task)ose.getObject();
        ((NewSchedule)newTaskSchedule).removeScheduleElement(ose);
      }
      else {
        logger.error("publishChangeProjection(), Bad Schedule: "+newTaskSchedule);
        return;
      }
      // Get overlapping schedule elements from start to end of new task
      c = publishedSchedule.getScheduleElementsWithTime(start);
      if (!c.isEmpty()) {
        // change the task to look like new task
        ose = (ObjectScheduleElement)c.iterator().next();
        published_task = (Task)ose.getObject();
        ((NewSchedule)publishedSchedule).removeScheduleElement(ose);

        logger.debug(" Comparing plublished task  "+getTaskUtils().taskDesc(published_task)+
                     " with \n"+getTaskUtils().taskDesc(new_task));
        published_task = getTaskUtils().changeTask(published_task, new_task);
        if (published_task != null) {
          logger.debug("DIFF ********** Replaced task with ---> \n"+ getTaskUtils().taskDesc(published_task));
          inventoryPlugin.publishChange(published_task);
        }
      }
      else {
        // no task exists that covers this timespan, publish it
        // apply the Task to the LogisticsInventoryBG
        logger.debug("No task exists that covers this timespan, publish task "+
                     getTaskUtils().taskDesc(new_task));
        LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
          searchForPropertyGroup(LogisticsInventoryPG.class);
        thePG.addRefillProjection(new_task);
        // hook the task in with the MaintainInventory workflow and publish
        inventoryPlugin.publishRefillTask(new_task, inv);
      }
    }
    // Rescind any tasks that were not accounted for
    Enumeration e = publishedSchedule.getAllScheduleElements();
    while (e.hasMoreElements()) {
      Task task = (Task) ((ObjectScheduleElement) e.nextElement()).getObject();
      logger.debug("DIFF ********** Removing task --> \n"+getTaskUtils().taskDesc(task));
      ((NewWorkflow)task.getWorkflow()).removeTask(task);
      inventoryPlugin.publishRemove(task);
    }
  }
}
    
  
  






