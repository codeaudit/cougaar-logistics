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

package org.cougaar.logistics.plugin.demand;


import java.util.Collection;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.HashSet;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.logistics.ldm.Constants;
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
      if (getAssetUtils().getQuantity(consumer) > 0) {
        NewTask gpTask = createGPTask(detReqTask, consumer);
        gpTasks.add(gpTask);
      } else {
        if (logger.isWarnEnabled()) {
          Asset asset;
          if (consumer instanceof AggregateAsset) {
            asset = ((AggregateAsset)consumer).getAsset();
          } else {
            // Not expecting this case, only AggregateAssets can have a zero quantity
            asset = consumer.getPrototype();
          }
          logger.warn("Ignoring Asset: "+getAssetUtils().getAssetIdentifier(asset)+
                      " at "+dfPlugin.getMyOrganization()+" - Asset has a quantity of zero. Bug #3467");
        }
      }
    }
    if (gpTasks.isEmpty()) {
      if(logger.isWarnEnabled()) {
        logger.warn("Cannot expand - no subtasks for determine requirements task "
                    + getTaskUtils().taskDesc(detReqTask));
      }
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
        if(logger.isErrorEnabled()) {
          logger.error("Unhandled plan element type on DetermineRequirementsTask :" + pe.getClass().getName());
        }
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
    if (logger.isDebugEnabled()) {
      logger.debug("Agent: " + dfPlugin.getClusterId().toString() + "DetReqExp type[" +
                   dfPlugin.getSupplyType() + "] " + " Expanding DetReq: " + parent.getUID() +
                   " Expansion is: " + expansion.getUID());
    }
  }

  protected void addToAndPublishExpansion(Task parent, Collection subtasks) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      Task task = (Task) subtasksIT.next();
      wf.addTask(task);
      ((NewTask) task).setWorkflow(wf);
      dfPlugin.publishAdd(task);
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
    newTask.setVerb(Verb.get(Constants.Verb.GENERATEPROJECTIONS));


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
        getPlanningFactory().newAllocationResult(Constants.Confidence.OBSERVED, true, avs);
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
   Enumeration enm = task.getPrepositionalPhrases();
   Vector phrases = new Vector();
   while (enm.hasMoreElements()) {
   phrases.addElement(enm.nextElement());
   }
   phrases.addElement(pp);
   task.setPrepositionalPhrases(phrases.elements());
   return task;
   }

   **/
}



