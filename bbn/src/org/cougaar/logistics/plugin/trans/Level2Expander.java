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
  HashMap level6CustomerMap;

  public Level2Expander(Level2TranslatorPlugin level2Translator) {
    super(level2Translator);
    level2To6Map = new HashMap();
    level6CustomerMap = new HashMap();
  }


  protected double deriveTotalQty(long bucketStart, long bucketEnd, Collection projTasks) {
    Iterator tasksIt = projTasks.iterator();
    double totalQty = 0.0;
    while (tasksIt.hasNext()) {
      Task projTask = (Task) tasksIt.next();
      long taskStart = getTaskUtils().getStartTime(projTask);
      long taskEnd = getTaskUtils().getEndTime(projTask);
      long start = Math.max(taskStart, bucketStart);
      long end = Math.min(taskEnd, bucketEnd);
      //duration in seconds
      double duration = ((end - start) / 1000);
      Rate rate = getTaskUtils().getRate(projTask);
      double qty = (getBaseUnitPerSecond(rate) * duration);
      totalQty += qty;
    }
    return totalQty;
  }

  protected double getBaseUnitPerSecond(Rate rate) {
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


  protected void expandLevel2Task(Task parent,
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
    } else if (logger.isErrorEnabled()) {
      logger.error("Unknown Plan Element on task-" + parent);
    }


  }

  protected void republishExpansionWithTask(Task parent, NewTask childTask) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    removeAllFromWorkflow(wf);
    //TODO: MWD Remove debug statements:
    if ((translatorPlugin.getOrgName() != null) &&
        (translatorPlugin.getOrgName().trim().equals("1-35-ARBN"))) {
      System.out.println("translatorPlugin:Level2Expander:I'm publishing 1 Level2 " + translatorPlugin.getSupplyType() + "  task.");
    }
    childTask.setWorkflow(wf);
    translatorPlugin.publishAdd(childTask);
    wf.addTask(childTask);
    translatorPlugin.publishChange(expansion);
  }


  protected void removeAllFromWorkflow(NewWorkflow wf) {
    Enumeration subtasks = wf.getTasks();
    while (subtasks.hasMoreElements()) {
      Task childTask = (Task) subtasks.nextElement();
      wf.removeTask(childTask);
      translatorPlugin.publishRemove(childTask);
    }
  }

  protected void createAndPublishExpansion(Task parent, NewTask childTask) {
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
    childTask.setWorkflow(wf);
    wf.addTask(childTask);

    return wf;
  }


  protected NewTask createNewLevel2Task(Task parentTask,
                                        Rate newRate,
                                        long lastSupplyEndTime) {

    Vector newPrefs = new Vector();
    Enumeration oldPrefs = parentTask.getPreferences();

    while (oldPrefs.hasMoreElements()) {
      Preference pref = (Preference) oldPrefs.nextElement();
      if (pref.getAspectType() == AlpineAspectType.DEMANDRATE) {
        pref = getTaskUtils().createDemandRatePreference(getPlanningFactory(), newRate);
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

    //newTask.setCommitmentDate(parentTask.getCommitmentDate());

    return newTask;
  }


  protected PrepositionalPhrase newPrepositionalPhrase(String preposition,
                                                       Object io) {
    NewPrepositionalPhrase pp = getPlanningFactory().newPrepositionalPhrase();
    pp.setPreposition(preposition);
    pp.setIndirectObject(io);
    return pp;
  }


}


