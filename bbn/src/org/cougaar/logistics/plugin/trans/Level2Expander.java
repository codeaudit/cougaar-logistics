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

package org.cougaar.logistics.plugin.trans;


import java.util.*;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.util.Random;


/**
 * The Level2Expander expands a Level2Task into it's adjusted
 * (by Level6 tasks) equivilent
 *
 * @see Level2TranslatorPlugin
 * @see Level2TranslatorModule
 **/

public class Level2Expander extends Level2TranslatorModule {

  HashMap level2To6Map;
  HashMap endTimeMap;

  private static AllocationResultAggregator expansionARA = new ExpansionARA();

  public Level2Expander(Level2TranslatorPlugin level2Translator) {
    super(level2Translator);
    level2To6Map = new HashMap();
    endTimeMap = new HashMap();
  }

  public Collection translateLevel2Tasks(Collection level2Tasks,
                                         Collection level6Tasks,
                                         Collection supplyTasks) {
    ArrayList doneLevel2s = new ArrayList();
    mapLevel2ToLevel6(level2Tasks, level6Tasks);
    mapCustomerEndTimes(supplyTasks);
    Iterator subIt = level2To6Map.entrySet().iterator();
    while (subIt.hasNext()) {
      Map.Entry entry = (Map.Entry) subIt.next();
      Task level2Task = (Task) entry.getKey();
      Collection relLevel6Tasks = (Collection) entry.getValue();
      Task aDoneLevel2 = translateLevel2Task(level2Task, relLevel6Tasks);
      if (aDoneLevel2 != null) {
        doneLevel2s.add(aDoneLevel2);
      }
    }

    return doneLevel2s;
  }


  private Task translateLevel2Task(Task level2Task,
                                   Collection relevantL6Tasks) {
    Task doneLevel2Task = null;
    long endTime = getTaskUtils().getEndTime(level2Task);
    long startTime = getTaskUtils().getStartTime(level2Task);
    long lastActualSeen = startTime;
    Long custLastActual = (Long) endTimeMap.get(getTaskUtils().getCustomer(level2Task));
    if (custLastActual != null) {
      lastActualSeen = custLastActual.longValue();
    }
    boolean alreadyDisposed = false;

    double expL2BaseRate = 0;
    PlanElement pe = level2Task.getPlanElement();
    if (pe != null) {
      if (pe instanceof Expansion) {
        Expansion exp = (Expansion) pe;
        Enumeration subtasks = exp.getWorkflow().getTasks();
        while (subtasks.hasMoreElements()) {
          Task subtask = (Task) subtasks.nextElement();
          expL2BaseRate += getBaseUnitPerSecond(getTaskUtils().getRate(subtask));
        }
      } else if (pe instanceof Disposition) {
        alreadyDisposed = true;
      }
    }

    if (lastActualSeen >= endTime) {
      doneLevel2Task = level2Task;
      //If alreadyDisposed and still done we don't want to re-dispose. Just because its after the last actual seen
      if (alreadyDisposed) {
	  return null;
      }
    } else {
      long countedStartTime = Math.max(startTime, lastActualSeen);
      double totalL6BaseQty = deriveTotalQty(countedStartTime, endTime, relevantL6Tasks);
      Rate origL2Rate = getTaskUtils().getRate(level2Task);
      double origL2BaseQty = deriveTotalQty(countedStartTime, endTime, level2Task);
//
      
      double toleranceFactor = 0.10;  // for ammo within 200 lbs

      if ((origL2BaseQty - totalL6BaseQty) < toleranceFactor) {
        if((totalL6BaseQty - origL2BaseQty) > 2.0) {
          logger.warn("level2Task " + level2Task + " has a total qty of " + origL2BaseQty +
                      " which is exceeded by the level 6 tasks with a summed total qty of " + totalL6BaseQty);
        }
	//If already disposed we still want to redispose in case there has been a qty change
        doneLevel2Task = level2Task;
      } else {
        double durationMillis = (endTime - countedStartTime);
        double durationSecs = (durationMillis / 1000);
        double newL2BaseRate = ((origL2BaseQty - totalL6BaseQty) / durationSecs);
        if (newL2BaseRate != expL2BaseRate) {
          double origL2BaseRate = getBaseUnitPerSecond(origL2Rate);
          if (logger.isDebugEnabled()) {
            logger.debug("Level2Expander:Original Rate per day:" + (origL2BaseRate * TimeUtils.SEC_PER_DAY) +
                         ".  Changing expanded rate from " + (expL2BaseRate * TimeUtils.SEC_PER_DAY) +
                         " to " + (newL2BaseRate * TimeUtils.SEC_PER_DAY));
            logger.debug("     and L2 Start time is " + new Date(startTime) + " and end time is " + new Date(endTime) + ", but last actual seen is " + new Date(lastActualSeen));
            logger.debug("  and also origL2BaseQty:" + origL2BaseQty + " totalL6BaseQty:" + totalL6BaseQty);
          }

          Rate newL2Rate = newRateFromUnitPerSecond(origL2Rate, newL2BaseRate);
          expandLevel2Task(level2Task, newL2Rate, lastActualSeen);
        }
      }
    }

    return doneLevel2Task;
  }


  private void mapCustomerEndTimes(Collection supplyTasks) {
    endTimeMap.clear();
    Iterator supplyTaskIt = supplyTasks.iterator();
    while (supplyTaskIt.hasNext()) {
      Task supplyTask = (Task) supplyTaskIt.next();
      long endTime = getTaskUtils().getEndTime(supplyTask);
      Object org = getTaskUtils().getCustomer(supplyTask);
      if (org != null) {
        Long lastActualSeen = (Long) endTimeMap.get(org);
        if ((lastActualSeen == null) || (endTime > lastActualSeen.longValue())) {
          endTimeMap.put(org, new Long(endTime));
        }
      }
    }
  }

  private void mapLevel2ToLevel6(Collection level2s, Collection level6s) {
    //alreadyMapped is only here to print out the error below.   This turns out
    //to be a nominal case where level 6s overlap more than one level2.  Commented
    //out the debug code for right now.  Can uncomment if any need to re-examine.
    //HashSet alreadyMapped = new HashSet();
    level2To6Map.clear();
    Iterator level2It = level2s.iterator();
    while (level2It.hasNext()) {
      Task level2Task = (Task) level2It.next();
      long l2StartTime = getTaskUtils().getStartTime(level2Task);
      long l2EndTime = getTaskUtils().getEndTime(level2Task);
      Object l2Cust = getTaskUtils().getCustomer(level2Task);
      Iterator level6It = level6s.iterator();
      ArrayList mappedL6s = new ArrayList();
      while (level6It.hasNext()) {
        Task level6Task = (Task) level6It.next();
        long l6StartTime = getTaskUtils().getStartTime(level6Task);
        long l6EndTime = getTaskUtils().getEndTime(level6Task);
        Object l6Cust = getTaskUtils().getCustomer(level2Task);
        if ((l6StartTime < l2EndTime) &&
            (l6EndTime > l2StartTime)) {
          if (l2Cust.equals(l6Cust)) {
            mappedL6s.add(level6Task);
            /**
            if ((alreadyMapped.contains(level6Task)) &&
                logger.isWarnEnabled()) {
              //Apparently lots overlap commented out alreadyMapped and all debug related code.
              logger.warn("The following task has already been mapped: " + level6Task.getUID() + " startTime: " +
                          new Date(l6StartTime) + " endTime: " +
                          new Date(l6EndTime) + ".  And the new overlapping L2 Task startTime " + new Date(l2StartTime) +
                          " and endTime is:" + new Date(l2EndTime));
            } else {
              alreadyMapped.add(level6Task);
            }
            **/
          } else {
            logger.error("Unexpected Customer of level2Task " + l2Cust + " differs from level6 cust:" + l6Cust);
          }
        }
      }
      level2To6Map.put(level2Task, mappedL6s);
    }
  }

  protected double deriveTotalQty(long bucketStart, long bucketEnd, Collection projTasks) {
    Iterator tasksIt = projTasks.iterator();
    double totalQty = 0.0;
    while (tasksIt.hasNext()) {
      Task projTask = (Task) tasksIt.next();
      double qty = deriveTotalQty(bucketStart, bucketEnd, projTask);
      totalQty += qty;
    }
    return totalQty;
  }


  protected double deriveTotalQty(long bucketStart, long bucketEnd, Task projTask) {

    long taskStart = getTaskUtils().getStartTime(projTask);
    long taskEnd = getTaskUtils().getEndTime(projTask);
    long start = Math.max(taskStart, bucketStart);
    long end = Math.min(taskEnd, bucketEnd);
    //duration in seconds
    double duration = ((end - start) / 1000);
    Rate rate = getTaskUtils().getRate(projTask);
    double qty = (getBaseUnitPerSecond(rate) * duration);
    return qty;
  }

  protected static double getBaseUnitPerSecond(Rate rate) {
    if (rate instanceof CostRate) {
      return ((CostRate) rate).getDollarsPerSecond();
    } else if (rate instanceof CountRate) {
      return ((CountRate) rate).getEachesPerSecond();
    } else if (rate instanceof FlowRate) {
      return ((FlowRate) rate).getGallonsPerSecond();
    } else if (rate instanceof MassTransferRate) {
      return ((MassTransferRate) rate).getShortTonsPerSecond();
    } else if (rate instanceof TimeRate) {
      return ((TimeRate) rate).getHoursPerSecond();
    } // if
    return 0.0;
  }

  protected Rate newRateFromUnitPerSecond(Rate rate, double unitsPerSecond) {
    if (rate instanceof CostRate) {
      return (CostRate.newDollarsPerSecond(unitsPerSecond));
    } else if (rate instanceof CountRate) {
      return (CountRate.newEachesPerSecond(unitsPerSecond));
    } else if (rate instanceof FlowRate) {
      return (FlowRate.newGallonsPerSecond(unitsPerSecond));
    } else if (rate instanceof MassTransferRate) {
      return (MassTransferRate.newShortTonsPerSecond(unitsPerSecond));
    } else if (rate instanceof TimeRate) {
      return (TimeRate.newHoursPerSecond(unitsPerSecond));
    } // if

    if (logger.isErrorEnabled()) {
      logger.error("Unknown rate type");
    }
    return (CountRate.newEachesPerSecond(unitsPerSecond));
  }


  private void expandLevel2Task(Task parent,
                                Rate newRate,
                                long lastSupplyTaskTime) {


    NewTask childTask = createNewLevel2Task(parent,
                                            newRate,
                                            lastSupplyTaskTime);
    PlanElement pe = parent.getPlanElement();
    if (pe == null) {
      createAndPublishExpansion(parent, childTask);
    } else if (pe instanceof Expansion) {
      republishExpansionWithTask(parent, childTask);
    } else if (pe instanceof Disposition) {
      logger.warn("Level2Expander:expandLevel2Task: Expanding an already disposed task");
      removeDisposition(parent);
      createAndPublishExpansion(parent, childTask);
    } else if (logger.isErrorEnabled()) {
      logger.error("Unknown Plan Element on task-" + parent);
    }


  }

  private void removeDisposition(Task level2Task) {
    Disposition disposition = (Disposition) level2Task.getPlanElement();
    translatorPlugin.publishRemove(disposition);
  }

  private void republishExpansionWithTask(Task parent, NewTask childTask) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    removeAllFromWorkflow(wf);
    if (logger.isDebugEnabled()) {
      logger.debug("translatorPlug  in:Level2Expander:I'm re-publishing 1 Level2 " +
                   translatorPlugin.getSupplyType() + "  task.");
    }
    childTask.setWorkflow(wf);
    translatorPlugin.publishAdd(childTask);
    wf.addTask(childTask);
    translatorPlugin.publishChange(expansion);
  }


  private void removeAllFromWorkflow(NewWorkflow wf) {
    Enumeration subtasks = wf.getTasks();
    while (subtasks.hasMoreElements()) {
      Task childTask = (Task) subtasks.nextElement();
      wf.removeTask(childTask);
      translatorPlugin.publishRemove(childTask);
    }
  }

   /**
    * Define an ARA that can deal with the expansion of a
    * Level2 ProjectSupply task to a Ready For Transport Level 2 ProjectSupply Task. 
    * Mostly, we just clone the result of the single chile Ready for Transport ProjectSupply
    * task.
    **/
    private static class ExpansionARA implements AllocationResultAggregator {
        public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
            if (tst.size() != 1)
                throw new IllegalArgumentException("expansionARA: multiple subtasks");
            AllocationResult ar = (AllocationResult) tst.getAllocationResult(0);
            if (ar == null) return null;
            if (ar.isEqual(currentar)) return currentar;
            return (AllocationResult) ar.clone();
        }
    }


  private void createAndPublishExpansion(Task parent, NewTask childTask) {
    Workflow wf = buildWorkflow(parent, childTask);
    translatorPlugin.publishAdd(childTask);
    Expansion expansion = getPlanningFactory().createExpansion(parent.getPlan(), parent, wf, null);
    translatorPlugin.publishAdd(expansion);
  }


  /**
   *  Build a workflow from a vector of tasks.
   * @param parent parent task of workflow
   * @param childTask new child task to be added to workflow tasks
   **/
  public Workflow buildWorkflow(Task parent, NewTask childTask) {
    NewWorkflow wf = getPlanningFactory().newWorkflow();
    wf.setParentTask(parent);
    wf.setIsPropagatingToSubtasks(true);
    //wf.setAllocationResultAggregator(expansionARA);
    childTask.setWorkflow(wf);
    wf.addTask(childTask);

    return wf;
  }


  private NewTask createNewLevel2Task(Task parentTask,
                                      Rate newRate,
                                      long lastSupplyEndTime) {

    Vector newPrefs = new Vector();
    Enumeration oldPrefs = parentTask.getPreferences();

    long startTime = getTaskUtils().getStartTime(parentTask);

    while (oldPrefs.hasMoreElements()) {
      Preference pref = (Preference) oldPrefs.nextElement();
      if (pref.getAspectType() == AlpineAspectType.DEMANDRATE) {
        pref = getTaskUtils().createDemandRatePreference(getPlanningFactory(), newRate);
      }
      if ((lastSupplyEndTime > startTime) &&
          (pref.getAspectType() == AspectType.START_TIME)) {
        pref = createNewTimePreference(lastSupplyEndTime, pref);
      }
      newPrefs.add(pref);
    }

    Vector newPreps = new Vector();
    Enumeration oldPreps = parentTask.getPrepositionalPhrases();
    while (oldPreps.hasMoreElements()) {
      newPreps.add(oldPreps.nextElement());
    }
    newPreps.add(newPrepositionalPhrase(Constants.Preposition.READYFORTRANSPORT, translatorPlugin.getOrgName()));

    NewTask newTask = getPlanningFactory().newTask();

    newTask.setParentTask(parentTask);
    newTask.setPlan(parentTask.getPlan());

    newTask.setDirectObject(parentTask.getDirectObject());
    newTask.setVerb(parentTask.getVerb());

    newTask.setPreferences(newPrefs.elements());
    newTask.setPrepositionalPhrases(newPreps.elements());

    newTask.setContext(parentTask.getContext());

    //newTask.setCommitmentDate(parentTask.getCommitmentDate());

    return newTask;
  }


  /** Create a Time Preference for the Refill Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param origTimePref A simarly generated time preference that was taken from the parent (we want a duplicate with a different best time)
   *  @return Preference The new Time Preference
   **/
  private Preference createNewTimePreference(long bestDay, Preference origTimePref) {
    int aspectType = origTimePref.getAspectType();
    ScoringFunction.PiecewiseLinearScoringFunction origTimeSF = (ScoringFunction.PiecewiseLinearScoringFunction) origTimePref.getScoringFunction();
    AspectScoreRange origRange = origTimeSF.getDefinedRange();
    long early = (long) origRange.getRangeStartPoint().getValue();
    long late = getTimeUtils().addNDays(bestDay, 1);
    long end = (long) origRange.getRangeEndPoint().getValue();
    double daysBetween = ((end - bestDay) / 86400000);
//Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
// define alpha .25
    double alpha = .25;

    Vector points = new Vector();
    AspectScorePoint earliest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, early), alpha);
    AspectScorePoint best = new AspectScorePoint(AspectValue.newAspectValue(aspectType, bestDay), 0.0);
    AspectScorePoint first_late = new AspectScorePoint(AspectValue.newAspectValue(aspectType, late), alpha);
    AspectScorePoint latest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, end), (alpha + late_score));

    points.addElement(earliest);
    points.addElement(best);
    points.addElement(first_late);
    points.addElement(latest);
    ScoringFunction timeSF = ScoringFunction.createPiecewiseLinearScoringFunction(points.elements());
    return getPlanningFactory().newPreference(aspectType, timeSF);

// prefs.addElement(TaskUtils.createDemandRatePreference(planFactory, rate));
//return prefs;
  }


  protected PrepositionalPhrase newPrepositionalPhrase(String preposition,
                                                       Object io) {
    NewPrepositionalPhrase pp = getPlanningFactory().newPrepositionalPhrase();
    pp.setPreposition(preposition);
    pp.setIndirectObject(io);
    return pp;
  }


}


