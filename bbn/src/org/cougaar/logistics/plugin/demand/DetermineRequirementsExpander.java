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

package org.cougaar.logistics.plugin.demand;


import java.util.Collection;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.HashSet;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.plan.AlpineAspectType;


/**
 * <pre>
 * The default RequirementsExpander for the DemandForecastPlugin.
 *
 * This class expands a determine requirements task into generateProjections tasks for each
 * MEI that has the passed in supply class PG on it.
 *
 *
 **/

public class DetermineRequirementsExpander extends DemandForecastModule implements DetReqExpanderIfc {


  public DetermineRequirementsExpander(DemandForecastPlugin dfPlugin) {
    super(dfPlugin);
  }

  /**
   * Expand DetermineRequirements tasks into GenerateProjections tasks.
   **/
  public void expandDetermineRequirements(Task detReqTask, Collection assets) {
    if ((assets == null) || (assets.isEmpty())) {
      disposeOfTask(detReqTask);
      return;
    }
    ArrayList gpTasks = new ArrayList();
    Iterator assetIT = assets.iterator();
    while (assetIT.hasNext()) {
      Asset consumer = (Asset) assetIT.next();
      NewTask gpTask = createGPTask(detReqTask, consumer);
      gpTasks.add(gpTask);
    }
    if (gpTasks.isEmpty()) {
      logger.warn("Cannot expand - no subtasks for determine requirements task "
                  + getTaskUtils().taskDesc(detReqTask));
    } else {
      PlanElement pe = detReqTask.getPlanElement();
      if ((pe != null) && (pe instanceof Disposition)) {
        dfPlugin.publishRemove(pe);
        pe = null;
      }
      // First time through build a fresh expansion
      if (pe == null) {
        createAndPublishExpansion(detReqTask, gpTasks);
      }
      // There are new assets to add to the expansion
      else if (pe instanceof Expansion) {
        addToAndPublishExpansion(detReqTask, gpTasks);
      } else {
        logger.error("Unhandled plan element type on DetermineRequirementsTask :" + pe.getClass().getName());
      }
    }

  }

  public void removeSubtasksFromDetermineRequirements(Task detReqTask, Collection removedAssets) {
    Expansion expansion = (Expansion) detReqTask.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    HashSet remTaskHash = new HashSet(removedAssets);
    Enumeration subtasks = wf.getTasks();
    while (subtasks.hasMoreElements()) {
      Task task = (Task) subtasks.nextElement();
      Asset consumer = task.getDirectObject();
      if (remTaskHash.contains(consumer)) {
        wf.removeTask(task);
        dfPlugin.publishRemove(task);
      }
    }
    dfPlugin.publishChange(expansion);
  }

  protected void createAndPublishExpansion(Task parent, Collection subtasks) {
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      dfPlugin.publishAdd(subtasksIT.next());
    }
    Workflow wf = buildWorkflow(parent, subtasks);
    Expansion expansion = getPlanningFactory().createExpansion(parent.getPlan(), parent, wf, null);
    dfPlugin.publishAdd(expansion);
  }

  protected void addToAndPublishExpansion(Task parent, Collection subtasks) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      Task task = (Task) subtasksIT.next();
      dfPlugin.publishAdd(task);
      wf.addTask(task);
      ((NewTask) task).setWorkflow(wf);
    }
    dfPlugin.publishChange(expansion);
  }


  protected NewTask createGPTask(Task parentTask, Asset consumer) {
    Vector prefs = new Vector();

    PrepositionalPhrase prepPhrase = newPrepositionalPhrase(Constants.Preposition.OFTYPE, dfPlugin.getSupplyType());

    prefs.addElement(createStartTimePref(parentTask));

    NewTask newTask = getPlanningFactory().newTask();

    newTask.setParentTask(parentTask);
    newTask.setPlan(parentTask.getPlan());

    //MWD Remove
    //newTask.setPrepositionalPhrases(parentTask.getPrepositionalPhrases());
    //newTask = addPrepositionalPhrase(newTask, prepPhrase);

    newTask.setPrepositionalPhrases(prepPhrase);

    newTask.setDirectObject(consumer);
    newTask.setVerb(Verb.getVerb(Constants.Verb.GENERATEPROJECTIONS));


    newTask.setPreferences(prefs.elements());

    return newTask;
  }

  /**
   *  Build a workflow from a vector of tasks.
   * @param parent parent task of workflow
   * @param subtasks workflow tasks
   * @return Workflow
   **/
  public Workflow buildWorkflow(Task parent, Collection subtasks) {
    NewWorkflow wf = getPlanningFactory().newWorkflow();
    wf.setParentTask(parent);
    wf.setIsPropagatingToSubtasks(true);
    NewTask t;
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      t = (NewTask) subtasksIT.next();
      t.setWorkflow(wf);
      wf.addTask(t);
    }
    return wf;
  }


  protected void disposeOfTask(Task task) {
    AspectValue avs[] = new AspectValue[1];
    avs[0] = AspectValue.newAspectValue(AlpineAspectType.DEMANDRATE, CountRate.newEachesPerDay(0.0));
    AllocationResult dispAR =
        getPlanningFactory().newAllocationResult(1.0, true, avs);
    Disposition disposition =
        getPlanningFactory().createDisposition(task.getPlan(), task, dispAR);
    dfPlugin.publishAdd(disposition);
  }

  protected long getStartTimePref(Task task) {
    synchronized (task) {
      Enumeration taskPrefs = task.getPreferences();
      while (taskPrefs.hasMoreElements()) {
        Preference aPref = (Preference) taskPrefs.nextElement();
        if (aPref.getAspectType() == AspectType.START_TIME) {
          ScoringFunction sf = aPref.getScoringFunction();
          return (long) sf.getBest().getValue();
        }
      }
    }
    return 0L;
  }

  protected Preference createStartTimePref(Task parentTask) {
    long startTime = getStartTimePref(parentTask);
    AspectValue av = AspectValue.newAspectValue(AspectType.START_TIME, startTime);
    ScoringFunction score = ScoringFunction.createNearOrAbove(av, 0);
    return getPlanningFactory().newPreference(AspectType.START_TIME, score);
  }

  public PrepositionalPhrase newPrepositionalPhrase(String preposition,
                                                    Object io) {
    NewPrepositionalPhrase pp = getPlanningFactory().newPrepositionalPhrase();
    pp.setPreposition(preposition);
    pp.setIndirectObject(io);
    return pp;
  }

  public void updateAllocationResults(Collection planElements) {
    Iterator peIt = planElements.iterator();
    while (peIt.hasNext()) {
      PlanElement pe = (PlanElement) peIt.next();
      if (PluginHelper.updatePlanElement(pe)) {
        dfPlugin.publishChange(pe);
      }
    }
  }


  /**
   *
   * MWD remove
   *
   public static NewTask addPrepositionalPhrase(NewTask task, PrepositionalPhrase pp) {
   Enumeration enum = task.getPrepositionalPhrases();
   Vector phrases = new Vector();
   while (enum.hasMoreElements()) {
   phrases.addElement(enum.nextElement());
   }
   phrases.addElement(pp);
   task.setPrepositionalPhrases(phrases.elements());
   return task;
   }

   **/
}



