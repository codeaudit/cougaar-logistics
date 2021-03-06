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

package org.cougaar.logistics.plugin.trans;


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
import org.cougaar.planning.plugin.util.AllocationResultHelper;
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

public class Level2Disposer extends Level2TranslatorModule {


  public Level2Disposer(Level2TranslatorPlugin translatePlugin) {
    super(translatePlugin);
  }

  /**
   * Remove the expansion which is the transport version of the Level2 task
   * and then successfully dispose of each level 2 Task in the collection (should be 1)/
   **/
  public void disposeAndRemoveExpansion(Collection taskCollect) {
    Iterator it = taskCollect.iterator();
    while (it.hasNext()) {
      disposeAndRemoveExpansion((Task) it.next());
    }
  }

  /**
   * Remove the expansion which is the transport version of the Level2 task
   * and then successfully dispose of this one.
   **/
  public void disposeAndRemoveExpansion(Task level2Task) {
    boolean doDispose=true;
    PlanElement pe = level2Task.getPlanElement();
    if ((pe != null) && (pe instanceof Expansion)) {
      removeExpansion(level2Task);
    }
    else if ((pe != null) && (pe instanceof Disposition)) {
      doDispose = removeDisposition(level2Task);
    }
    if(doDispose) {
	disposeOfTask(level2Task);
    }
  }

  public void removeExpansion(Task level2Task) {
    Expansion expansion = (Expansion) level2Task.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    Enumeration subtasks = wf.getTasks();
    while (subtasks.hasMoreElements()) {
      Task task = (Task) subtasks.nextElement();
      wf.removeTask(task);
      translatorPlugin.publishRemove(task);
    }
    //Do you need to remove the workflow?
    //translatorPlugin.publishRemove(wf);
    translatorPlugin.publishRemove(expansion);
  }

  public boolean removeDisposition(Task level2Task) {
    Disposition disposition = (Disposition) level2Task.getPlanElement();
    AllocationResult ar = disposition.getEstimatedResult();
    if(getTaskUtils().getDailyQuantity(level2Task) == getTaskUtils().getQuantity(level2Task,ar)) {
      if(logger.isInfoEnabled()) {
	logger.info("Quantity on task is same as on disposition - not re-disposing (Bug 13424)");
      }
      return false;
    }
    else {
      if(logger.isInfoEnabled()) {
	logger.info("Re-disposing: Quantity on task is " + getTaskUtils().getDailyQuantity(level2Task) + " while the estimate result says " + getTaskUtils().getQuantity(level2Task,ar) + " - (Bug 13424)");
      }
      translatorPlugin.publishRemove(disposition);
      return true;
    }
  }


  protected void disposeOfTask(Task task) {
    AllocationResultHelper helper = new AllocationResultHelper(task, null);
    AllocationResult dispAR = helper.getAllocationResult(Constants.Confidence.OBSERVED, true);
    Disposition disposition =
        getPlanningFactory().createDisposition(task.getPlan(), task, dispAR);
    translatorPlugin.publishAdd(disposition);
  }

  /**
   *
   * TODO: MWD remove
   *

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



