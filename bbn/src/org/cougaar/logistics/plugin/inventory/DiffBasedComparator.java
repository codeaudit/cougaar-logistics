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
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;

/** The DiffBasedComparator compares both the set of old refills and new
 *  refills.  The goal of this module is to reduce the number of changes
 *  replans make on the blackboard.  It accomplishes this by finding published
 *  tasks that are identical to just created tasks.  The code will also try 
 *  and modify an existing task that is similar to the the newly created task 
 *  as opposed to to rescinding the published task and publishing the new task.
 *  Of course, if no published task can be found during the appropriate time span
 *  of the new task, the new task is published.  Any unaccounted for published
 *  tasks will be rescinded.
 *
 *  Called by the Refill Generator with the new refills and old refills.
 * 
 **/

public class DiffBasedComparator extends InventoryModule implements ComparatorModule {

  /** Need to pass in the IM Plugin for now to get services
   * and util classes.
   **/
  public DiffBasedComparator(InventoryPlugin imPlugin) {
    super(imPlugin);
    if (logger.isDebugEnabled()) {
      logger.debug("DiffBasedComparator LOADED!!!!!");
    }
  }

  /** Compares the old and new Refill tasks.
   *  The previously published refills are bucketized which means they are
   *  flagged as belonging in a certain bucket.  Each new task is examined to
   *  determine the bucket in which it would belong.  If a previously published
   *  task is found to occupy the same bucket as a new task, those tasks are
   *  then compared.  If the tasks are identical, no blackboard action is taken.
   *  Otherwise, the published task is changed to take on the characteristics
   *  of the new task and the new task is discarded.  If no refill occupies the
   *  same bucket as a new refill task, that task is published.  Any unaccounted
   *  for published tasks are rescinded.
   *  @param newRefills The collection of newly generated Refills from the
   *    RefillGeneratorModule
   *  @param oldRefills The previously generated Refill Tasks
   *  @param inv The Inventory the Refills are refilling.
   **/
  public void compareRefills(ArrayList newRefills, ArrayList oldRefills, Inventory inv) {

    //Process all new Refill Tasks
    LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
	searchForPropertyGroup(LogisticsInventoryPG.class);
    if (logger.isDebugEnabled()) {
      logger.debug("DiffSupply handling "+
                   thePG.getResource().getTypeIdentificationPG().getTypeIdentification());
    }

    // Create bucket map from oldRefills for easy access during the compare
    HashMap publishedTaskMap = new HashMap();
    Task oldRefill=null;
    Iterator oldIter = oldRefills.iterator();
    while (oldIter.hasNext()) {
      oldRefill = (Task)oldIter.next();
      if (oldRefill != null) {
        int bucket = thePG.convertTimeToBucket(getTaskUtils().getEndTime(oldRefill), false);
        publishedTaskMap.put(new Integer(bucket), oldRefill);
      }
    }

    // Compare new refills to old (published) refills
    Task newRefill=null, publishedRefill=null, updatedTask=null;
    Iterator newIter = newRefills.iterator();
    while (newIter.hasNext()) {
      newRefill = (Task) newIter.next();
      int bucket = thePG.convertTimeToBucket(getTaskUtils().getEndTime(newRefill), false);
      publishedRefill=(Task)publishedTaskMap.get(new Integer(bucket));
      if (publishedRefill == null) {
        // No published refill for the bucket, just publish the new refill
        if (logger.isDebugEnabled()) {
          logger.debug("DiffSupply "+getTimeUtils().dateString(thePG.convertBucketToTime(bucket))+" - "+
                       "no refills on this day, add new task");
        }
        if (inventoryPlugin.publishRefillTask(newRefill, inv)) {
          thePG.addRefillRequisition(newRefill);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("publishRefillTask returned false - not adding Refill task to the BG" +
                         newRefill.getUID());
          }
        }
      } else {
        // found a published refill for the bucket
        oldRefills.remove(publishedRefill);
        updatedTask = getTaskUtils().changeTask(publishedRefill, newRefill);
        if (updatedTask == null) {// null indicates tasks are identical
          if (logger.isDebugEnabled()) {
            logger.debug("DiffSupply "+getTimeUtils().dateString(thePG.convertBucketToTime(bucket))+" - "+
                         "new task identical to published task.");
          }
          thePG.addRefillRequisition(publishedRefill);
        } else {// changeTask() returned the updated task
          if (logger.isDebugEnabled()) {
            logger.debug("DiffSupply "+getTimeUtils().dateString(thePG.convertBucketToTime(bucket))+" - "+
                         "changed published task to match new task");
          }
          inventoryPlugin.publishChange(updatedTask);
          thePG.addRefillRequisition(updatedTask);
        }
      }
    }

    //Rescind all old Refill Tasks that have not been accounted for
    oldIter = oldRefills.iterator();
    while (oldIter.hasNext()) {
      oldRefill = (Task) oldIter.next();
      // check for a null in the refills list!
      if (oldRefill != null) {
        // clean out the reference in the maintain inventory workflow
        if (logger.isDebugEnabled()) {
          logger.debug("DiffSupply Remove unwanted published task from previous plan "+
                       getTaskUtils().taskDesc(oldRefill));
        }
        //((NewWorkflow)oldRefill.getWorkflow()).removeTask(oldRefill);
        //inventoryPlugin.publishRemove(oldRefill);
        inventoryPlugin.removeSubTask(oldRefill);
      }
    }      
  }

  /** Compares the old and new Refill Projection tasks.
   *  A schedule is created from the previously published projections.  New tasks
   *  are compared to the schedule in order to identify overlapping tasks.  In 
   *  cases where overlapping tasks are found, the published task is changed to 
   *  convey the information of the new task.  If the tasks are identical no changes
   *  are made to the blackboard.  New tasks which have no overlap with existing
   *  tasks are published and published refills which have not been accounted for
   *  are rescinded.
   *  @param newRefillProjs The collection of newly generated Refill Projections
   *    from the RefillProjectionsGenerator Module
   *  @param oldRefillProjs The previously generated Refill Projection Tasks
   *  @param inv The Inventory the Refills Projections are refilling.
   **/
  public void compareRefillProjections(ArrayList newRefillProjs, 
                                       ArrayList oldRefillProjs,
                                       Inventory inv) {

    LogisticsInventoryPG thePG = (LogisticsInventoryPG)inv.
        searchForPropertyGroup(LogisticsInventoryPG.class);

    if (logger.isDebugEnabled()) {
      logger.debug("DiffProj handling "+
                   thePG.getResource().getTypeIdentificationPG().getTypeIdentification());
    }
    // Check for an empty schedule
    if ((newRefillProjs == null) || newRefillProjs.isEmpty()) {
      // Rescind all tasks as there is no longer any demand.
      if (logger.isDebugEnabled()) {
        logger.debug("DiffProj, New Task List empty: "+newRefillProjs);
      }
      Iterator list  = oldRefillProjs.iterator();
      while (list.hasNext()) {
        Task oldRefill = (Task)list.next();
        if (logger.isDebugEnabled()) {
          logger.debug("DiffProj \n"+getTaskUtils().taskDesc(oldRefill));
        }
        if (oldRefill != null) {
          // clean out the reference in the maintain inventory workflow
          //((NewWorkflow)oldRefill.getWorkflow()).removeTask(oldRefill);
          //inventoryPlugin.publishRemove(oldRefill);
          inventoryPlugin.removeSubTask(oldRefill);
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
        logger.error("DiffProj, Bad Schedule: "+newTaskSchedule);
        return;
      }
      // Get overlapping schedule elements from start to end of new task
      c = publishedSchedule.getScheduleElementsWithTime(start);
      if (!c.isEmpty()) {
        // change the task to look like new task
        ose = (ObjectScheduleElement)c.iterator().next();
        published_task = (Task)ose.getObject();
        Task saveTask = published_task;
        ((NewSchedule)publishedSchedule).removeScheduleElement(ose);

//         logger.debug(" Comparing plublished task  "+getTaskUtils().taskDesc(published_task)+
//                      " with \n"+getTaskUtils().taskDesc(new_task));
        // changeTask returns the changed published task if the 2 tasks are different and 
        // null if the tasks are identical.
        published_task = getTaskUtils().changeTask(published_task, new_task);
        if (published_task != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("DiffProj published task changed "+ getTaskUtils().taskDesc(published_task));
          }
          inventoryPlugin.publishChange(published_task);
          thePG.addRefillProjection(published_task);
        }else{ // published and new task are the same, so add the published task back into the BG
          thePG.addRefillProjection(saveTask);
          if (logger.isDebugEnabled()) {
            logger.debug("DiffProj published task identical to new task");
          }
        }
      }
      else {
        // no task exists that covers this timespan, publish it and
        // apply the Task to the LogisticsInventoryBG
        if (logger.isDebugEnabled()) {
          logger.debug("No task exists that covers this timespan, publish task "+
                       getTaskUtils().taskDesc(new_task));
        }
        if (inventoryPlugin.publishRefillTask(new_task, inv)) {
          thePG.addRefillProjection(new_task);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("publishRefillTask returned false - not adding Refill task to the BG" +
                         new_task.getUID());
          }
        }
      }
    }
    // Rescind any tasks that were not accounted for
    Enumeration e = publishedSchedule.getAllScheduleElements();
    while (e.hasMoreElements()) {
      Task task = (Task) ((ObjectScheduleElement) e.nextElement()).getObject();
      if (logger.isDebugEnabled()) {
        logger.debug("DiffProj Remove unwanted published task from previous plan "+
                     getTaskUtils().taskDesc(task));
      }
      //((NewWorkflow)task.getWorkflow()).removeTask(task);
      //inventoryPlugin.publishRemove(task);
      if (logger.isDebugEnabled()) {
        logger.debug("About to call pluginhelper.removeSubTask... Task is: " + task.getVerb() + " " +
                     task.getUID() + " Parent Task is: " + task.getParentTaskUID() + " " +
                     task.getWorkflow().getParentTask().getVerb() +" Parent PE is: " +
                     task.getWorkflow().getParentTask().getPlanElement());
      }
      inventoryPlugin.removeSubTask(task);
    }
  }
}
