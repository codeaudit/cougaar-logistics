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

package org.cougaar.logistics.ui.stoplight.society;

import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.glm.plugins.TimeUtils;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.GLMFactory;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.glm.ldm.plan.PlanScheduleType;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;
import org.cougaar.glm.ldm.plan.NewQuantityScheduleElement;
import org.cougaar.glm.ldm.plan.QuantityScheduleElementImpl;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScheduleUtilities;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryPG;


public class InventoryWrapper {

  Inventory inventory;
  // Used a lot, so just grab a reference to it once
  LogisticsInventoryPG logInvPG;
  private long switchOverDay = -1;
  Logger logger;

  public InventoryWrapper(Inventory inputInventory) {
    inventory = inputInventory;
    logInvPG = (LogisticsInventoryPG)inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
    Logger logger = Logging.getLogger(this);
  }

  public Inventory getInventory() {
    return inventory;
  }
  
  public Schedule getInventorySchedule() {
    if (logInvPG == null) {
      return null;
    }
    return scheduleToNonOverlapSchedule(logInvPG.getBufferedInvLevels());
  }
  
  public Schedule getReorderLevelSchedule() {
    if (logInvPG == null) {
      return null;
    }
    return scheduleToNonOverlapSchedule(logInvPG.getBufferedCritLevels());
  }
  
  public Schedule getGoalLevelSchedule() {
    if (logInvPG == null) {
      return null;
    }
    return scheduleToNonOverlapSchedule(logInvPG.getBufferedTargetLevels());
  }
  
  
  /*
  ***  All the Due In stuff
   */
   
  /**
   *  @return A schedule of all the SUCCESSFUL supply elements
   */
  public Schedule getDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
    Schedule diSched = scheduleToNonOverlapSchedule(getSuccessfulElements(logInvPG.getSupplyList()));
    if (logger.isDebugEnabled()) {
      if (diSched != null) {
        logger.debug("Due-In schedule");
        printSchedule(diSched, logger);
      } else {
        logger.debug("Due-In schedule is null!!!!");
      }
    }      
    //return scheduleToNonOverlapSchedule(getSuccessfulElements(logInvPG.getSupplyList()));
    return diSched;
  }
  
  /**
   *  @return A schedule of all the supply elements
   */
  public Schedule getRequestedDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
      
    Schedule reqDISched = scheduleToNonOverlapSchedule(getAllElements(logInvPG.getSupplyList()));
    if (logger.isDebugEnabled()) {
      if(reqDISched != null) {
        logger.debug("Requested Due-In schedule");
        printSchedule(reqDISched, logger);
      } else {
        logger.debug("Requested Due-In schedule is null!!!");
      }
    }      
    //return scheduleToNonOverlapSchedule(getAllElements(logInvPG.getSupplyList()));
    return reqDISched;
  }

  /**
   *  @return A schedule of all the supply elements whose outcome is unknown
   */
  public Schedule getUnconfirmedDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
      
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection list = logInvPG.getSupplyList();
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      if (ar == null) {
        long endTime = TaskUtils.getEndTime(task); 
        long startTime = getStartTime(task, endTime, false);
        double quantity = TaskUtils.getDailyQuantity(task);
        
        NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                         endTime,
                                                                         quantity);
        schedule.addScheduleElement(qse);
      }
    }
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  @return A schedule of all the projection supply elements that are AFTER the switch over day
   */
  public Schedule getProjectedRequestedDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
      
    calcSwitchOverDay();
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection list = logInvPG.getProjSupplyList();
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, false);
      double quantity = TaskUtils.getDailyQuantity(task);
      if (switchOverDay < endTime) {
        // If switch over time occurs in the middle of a projection, we need to grab the part after that
        if (switchOverDay > startTime)
          startTime = switchOverDay;
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    }
    if (logger.isDebugEnabled()) {
      if (schedule != null) {
        logger.debug("Projected Requested Due-In schedule");
        printSchedule(schedule, logger);
      } else {
        logger.debug("Projected Requested Due-In schedule is null!!!!");
      }
    }       
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  @return A schedule of all the SUCCESSFUL projection supply elements that are AFTER the switch over day
   */
  public Schedule getProjectedDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
      
    calcSwitchOverDay();
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection list = logInvPG.getProjSupplyList();
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      // only want the ones that were successful
      if (ar == null || !ar.isSuccess())
        continue;

      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, false);
      double quantity = TaskUtils.getDailyQuantity(task);
      if (switchOverDay < endTime) {
        // If switch over time occurs in the middle of a projection, we need to grab the part after that
        if (switchOverDay > startTime)
          startTime = switchOverDay;
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    } 
    if (logger.isDebugEnabled()) {
      if (schedule != null) {
        logger.debug("Projected Due-In schedule");
        printSchedule(schedule, logger);
      } else {
        logger.debug("Projected Due-In schedule is null");
      }
    }      
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  @return A schedule of all the projection supply elements that are BEFORE the switch over day
   */
  public Schedule getInactiveProjectedRequestedDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
      
    calcSwitchOverDay();
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection list = logInvPG.getProjSupplyList();
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, false);
      double quantity = TaskUtils.getDailyQuantity(task);
      if (switchOverDay > startTime) {
        // If switch over time occurs in the middle of a projection, we need to grab the part after that
        if (switchOverDay < endTime)
          endTime = switchOverDay;
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    } 
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  @return A schedule of all the SUCCESSFUL projection supply elements that are BEFORE the switch over day
   */
  public Schedule getInactiveProjectedDueInSchedule() {
    if (logInvPG == null) {
      return null;
    }
      
    calcSwitchOverDay();
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection list = logInvPG.getProjSupplyList();
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      // only want the ones that were successful
      if (ar == null || !ar.isSuccess())
        continue;

      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, false);
      double quantity = TaskUtils.getDailyQuantity(task);
      if (switchOverDay > startTime) {
        // If switch over time occurs in the middle of a projection, we need to grab the part after that
        if (switchOverDay < endTime)
          endTime = switchOverDay;
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    } 
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  Calculate the time  of the last supply task.  This time will mark when the projected
   *   tasks will go from inactive to active.
   */
  private void calcSwitchOverDay() {
    // only need to do this once
    if (switchOverDay > 0)
      return;
      
    Collection list = logInvPG.getSupplyList();
    Iterator i = list.iterator();
    while(i.hasNext()) { 
      Task task = (Task) i.next();
      if (task == null)
        continue;
      long endTime = TaskUtils.getEndTime(task); 
      switchOverDay = Math.max(endTime,switchOverDay); 
    }
  }


  /*
  ***  All the Due Out stuff
   */
   
  /**
   *  @return A schedule of all the SUCCESSFUL Demand elements
   */
  public Schedule getDueOutSchedule() {
    if (logInvPG == null)
      return null;
      
    return scheduleToNonOverlapSchedule(getSuccessfulElements(logInvPG.getWithdrawList()));
  }
  
  /**
   *  @return A schedule of all the Demand elements
   */
  public Schedule getRequestedDueOutSchedule() {
    if (logInvPG == null)
      return null;
      
    return scheduleToNonOverlapSchedule(getAllElements(logInvPG.getWithdrawList()));
  }

  /**
   *  @return A schedule of all the SUCCESSFUL projection Demand elements
   */
  public Schedule getProjectedDueOutSchedule() {
    if (logInvPG == null)
      return null;
      
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection activeDemand = getFlatDemandList(logInvPG.getActualDemandTasksList());
    Iterator i = activeDemand.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      if (ar != null && ar.isSuccess()) {
        long endTime = TaskUtils.getEndTime(task); 
        long startTime = getStartTime(task, endTime, true);
        double quantity = TaskUtils.getDailyQuantity(task);
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    }

    return scheduleToNonOverlapSchedule(schedule);
  }
    
  /**
   *  @return A schedule of all the projection Demand elements
   */
  public Schedule getProjectedRequestedDueOutSchedule() {
    if (logInvPG == null)
      return null;
      
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection activeDemand = getFlatDemandList(logInvPG.getActualDemandTasksList());
    Iterator i = activeDemand.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, true);
      double quantity = TaskUtils.getDailyQuantity(task);
      while (startTime < endTime) {
        NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                         startTime + TimeUtils.MSEC_PER_DAY,
                                                                         quantity);
        schedule.addScheduleElement(qse);
        startTime += TimeUtils.MSEC_PER_DAY;
      }
    }
    
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  @return A schedule of all the INACTIVE SUCCESSFUL projection Demand elements
   */
  public Schedule getInactiveProjectedDueOutSchedule() {
    if (logInvPG == null)
      return null;
      
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection activeDemand = getFlatDemandList(logInvPG.getActualDemandTasksList());
    Collection allDemand = logInvPG.getProjWithdrawList();
    Collection inactiveDemand = getInactiveDemand(allDemand, activeDemand);
    
    // First process the tasks that are completely inactive
    Iterator i = inactiveDemand.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      if (ar != null && ar.isSuccess()) {
        long endTime = TaskUtils.getEndTime(task); 
        long startTime = getStartTime(task, endTime, false);
        double quantity = TaskUtils.getDailyQuantity(task);
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    }

    // Now process the tasks that are possibly partly inactive
    i = activeDemand.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      if (ar != null && ar.isSuccess()) {
        // get the regular end time, but we'll only use it to calculate start time and a new endtime 
        long endTime = TaskUtils.getEndTime(task);
        // start time here will be the regular start time of the task 
        long startTime = getStartTime(task, endTime, false);
        // end time here will be when we switch from inactive to active - the start time for active demand tasks
        endTime = getStartTime(task, endTime, true);
        double quantity = TaskUtils.getDailyQuantity(task);
        while (startTime < endTime) {
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           startTime + TimeUtils.MSEC_PER_DAY,
                                                                           quantity);
          schedule.addScheduleElement(qse);
          startTime += TimeUtils.MSEC_PER_DAY;
        }
      }
    }
    
    return scheduleToNonOverlapSchedule(schedule);
  }

  /**
   *  @return A schedule of all the INACTIVE projection Demand elements
   */
  public Schedule getInactiveProjectedRequestedDueOutSchedule() {
    if (logInvPG == null)
      return null;
      
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Collection activeDemand = getFlatDemandList(logInvPG.getActualDemandTasksList());
    Collection allDemand = logInvPG.getProjWithdrawList();
    Collection inactiveDemand = getInactiveDemand(allDemand, activeDemand);
    
    // First process the tasks that are completely inactive
    Iterator i = inactiveDemand.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, false);
      double quantity = TaskUtils.getDailyQuantity(task);
      while (startTime < endTime) {
        NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                         startTime + TimeUtils.MSEC_PER_DAY,
                                                                         quantity);
        schedule.addScheduleElement(qse);
        startTime += TimeUtils.MSEC_PER_DAY;
      }
    }

    // Now process the tasks that are possibly partly inactive
    i = activeDemand.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      // get the regular end time, but we'll only use it to calculate start time and a new endtime 
      long endTime = TaskUtils.getEndTime(task);
      // start time here will be the regular start time of the task 
      long startTime = getStartTime(task, endTime, false);
      // end time here will be when we switch from inactive to active - the start time for active demand tasks
      endTime = getStartTime(task, endTime, true);
      double quantity = TaskUtils.getDailyQuantity(task);
      while (startTime < endTime) {
        NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                         startTime + TimeUtils.MSEC_PER_DAY,
                                                                         quantity);
        schedule.addScheduleElement(qse);
        startTime += TimeUtils.MSEC_PER_DAY;
      }
    }
    
    return scheduleToNonOverlapSchedule(schedule);
  }

  private Collection getFlatDemandList(ArrayList activeDemandList) {
  	ArrayList activeProjDemand = new ArrayList();
    for(int i=0; i<activeDemandList.size(); i++) {
      ArrayList bucketODemand = (ArrayList) activeDemandList.get(i);
      for(int j=0; j<bucketODemand.size(); j++) {
        Task aTask = (Task) bucketODemand.get(j);
        if(aTask.getVerb().equals(Constants.Verb.PROJECTWITHDRAW)) {
          if(!(activeProjDemand.contains(aTask))) {
            activeProjDemand.add(aTask);
          }
        }
      }
    }
    return activeProjDemand;
  }

  /**
   *   @return a list of all the tasks that are COMPLETELY inactive.  Pieces of the tasks in activeDemand may be
   *        inactive as well.
   */
  private Collection getInactiveDemand(Collection allDemand, Collection activeDemand) {
    if (allDemand == null || allDemand.size() == 0)
      return new Vector();
    if (activeDemand == null || activeDemand.size() == 0)
      return allDemand;
      
    Vector inactiveDemand = new Vector();
    Iterator i = allDemand.iterator();
    while (i.hasNext()) {
      Object obj = i.next();
      if (obj == null)
        continue;
      if (!activeDemand.contains(obj) && !inactiveDemand.contains(obj))
        inactiveDemand.add(obj);
    }
    return inactiveDemand;
  }
  
  /*
  ***  Utility methods used by due out and due in methods
   */
  private Schedule getSuccessfulElements(Collection list) {
    if (list == null)
      return null;
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      PlanElement pe = task.getPlanElement();
      if (pe == null)
        continue;
      AllocationResult ar = pe.getReportedResult();
      if (ar == null) {
        ar = pe.getEstimatedResult();
      }
      if (ar != null && ar.isSuccess()) {
        //Huh?? Use the AR results not the preferences!
        //long endTime = TaskUtils.getEndTime(task); 
        //long startTime = getStartTime(task, endTime, false);
        //double quantity = TaskUtils.getDailyQuantity(task);
        if (!ar.isPhased()) {
          long endTime = (long) TaskUtils.getEndTime(ar); 
          //since this is a supply task set the start time won't be set so just make
          //it right before the endtime so the schedule element spans some time.
          long startTime = endTime -1;
          double quantity = TaskUtils.getQuantity(ar);
        
          NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                           endTime,
                                                                           quantity);
          schedule.addScheduleElement(qse);
        } else {
          int[] ats = ar.getAspectTypes();
          int qtyInd = getIndexForType(ats, AspectType.QUANTITY);
          int startInd = getIndexForType(ats, AspectType.START_TIME);
          int endInd = getIndexForType(ats, AspectType.END_TIME);
          Enumeration phasedResults = ar.getPhasedResults();
          while (phasedResults.hasMoreElements()) {
            double[] results = (double[]) phasedResults.nextElement();
            double phasedQuantity = (double) results[qtyInd];
            long phasedEndTime = (long) results[endInd];
            long phasedStartTime;
            if (startInd == -1) {
              phasedStartTime = phasedEndTime -1;
            } else {
              phasedStartTime = (long) results[startInd];
            }
            NewQuantityScheduleElement pqse = new QuantityScheduleElementImpl(phasedStartTime,
                                                                              phasedEndTime,
                                                                              phasedQuantity);
            schedule.addScheduleElement(pqse);
          }
        }
      }
    }
    return schedule;
  }
  
  private Schedule getAllElements(Collection list) {
    if (list == null)
      return null;
      
    ScheduleImpl schedule = new ScheduleImpl();
    schedule.setScheduleElementType(QuantityScheduleElementImpl.class);
    Iterator i = list.iterator();
    while (i.hasNext()) {
      Task task = (Task) i.next();
      if (task == null)
        continue;
      long endTime = TaskUtils.getEndTime(task); 
      long startTime = getStartTime(task, endTime, false);
      double quantity = TaskUtils.getDailyQuantity(task);
        
      NewQuantityScheduleElement qse = new QuantityScheduleElementImpl(startTime,
                                                                       endTime,
                                                                       quantity);
      schedule.addScheduleElement(qse);
    }
    return schedule;
  }

  private long getStartTime(Task task, long endTime, boolean isActiveDemandProj) {
    long startTime;
    // This is just a way to check if this preference is set
    if(TaskUtils.getPreferenceBest(task, AspectType.START_TIME) == null) {
      startTime = endTime - 1;
    } else { 
      startTime = TaskUtils.getStartTime(task);
      if (isActiveDemandProj)
        startTime = logInvPG.getEffectiveProjectionStart(task,startTime); 
    }
    return startTime;
  }

  /*
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
  ***  NEED TO DO
   */
  public void computeSimulatedProjectionSchedules() {
  }

  /**
   * Take a schedule of QuantityScheduleElements and
   * make it into an COUGAAR schedule
   *
  private Schedule collectionToSchedule(Collection mySchedule) {
    if(mySchedule == null) return new ScheduleImpl();
    Vector scheduleElements = new Vector();
    for (Iterator it = mySchedule.iterator(); it.hasNext();) {
      QuantityScheduleElement s = (QuantityScheduleElement)it.next();
      NewQuantityScheduleElement qse = GLMFactory.newQuantityScheduleElement();
      qse.setStartTime(s.getStartTime());
      qse.setEndTime(s.getEndTime());
      qse.setQuantity(s.getQuantity());
      scheduleElements.addElement(qse);
    }
    return GLMFactory.newQuantitySchedule(scheduleElements.elements(),
                                       PlanScheduleType.TOTAL_INVENTORY);
  }
*/  
  private Schedule scheduleToNonOverlapSchedule(Schedule schedule) {
    if (schedule == null) {
      return null;
    }
    //    Logger logger = Logging.getLogger(this);
    //blowing out on logger... why?
    if (logger == null) {
      logger = Logging.getLogger(this);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Original schedule");
      printSchedule(schedule, logger);
    }

    if (isOverlappingSchedule(schedule)) {
      Schedule nonoverlapping = ScheduleUtilities.computeNonOverlappingSchedule(schedule);
      if (logger.isDebugEnabled()) {
        logger.debug("Is Overlapping::Computing non-overlapping schedule");
        printSchedule(nonoverlapping, logger);
      }

      return nonoverlapping;
    }
	
    return schedule;
  }

  private boolean isOverlappingSchedule(Schedule aSchedule) {
    if(aSchedule != null) {
      Enumeration en = aSchedule.getAllScheduleElements();
      if(!en.hasMoreElements())
        return false; 
      long last_time = aSchedule.getStartTime()-1;
      while (en.hasMoreElements()) {
        ScheduleElement element = (ScheduleElement)en.nextElement();
        if (element.getStartTime() <= last_time)
          return true;
        last_time = element.getEndTime();
      }
    }
    return false;
  }

  private void printSchedule(Schedule s, Logger logger) {
    if (s == null)
      return;
    Enumeration e = s.getAllScheduleElements();
    while (e.hasMoreElements()) {
      QuantityScheduleElement se = (QuantityScheduleElement)e.nextElement();
      if (logger.isDebugEnabled()) {
        logger.debug("Start date: " + shortDate(se.getStartTime()) +
                     " end date: " + shortDate(se.getEndTime()) +
                     " quantity: " + se.getQuantity());
      }
    }
  }

  private void printSchedule(Vector s, Logger logger) {
    if (s == null || s.isEmpty()) {
      if (logger.isDebugEnabled())
        logger.debug("printSchedule() Empty Schedule");
      return;
    }
    Enumeration e = s.elements();
    while (e.hasMoreElements()) {
      printQuantityScheduleElement((QuantityScheduleElement)e.nextElement(), logger);
    }
  }

  private void printQuantityScheduleElement(QuantityScheduleElement qse, Logger logger) {
    if (logger.isDebugEnabled())
      logger.debug("Start date: " + shortDate(qse.getStartTime()) +
                   " end date: " + shortDate(qse.getEndTime()) +
                   " quantity: " + qse.getQuantity());
  }

  private String shortDate(long time) {
    String sdate = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(time));
    // map '9/8/00 12:00 AM' to ' 9/8/00 12:00 AM'
    while(sdate.length()<17){
      sdate = " "+sdate;
    }
    return sdate;
  }

 private int getIndexForType(int[] types, int type) {
    for (int ii = 0; ii < types.length; ii++) {
      if (types[ii] == type) {
        return ii;
      }
    }
    return -1;
  }
  
}


