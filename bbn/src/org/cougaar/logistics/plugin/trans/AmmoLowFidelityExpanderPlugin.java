/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

import java.util.Vector;

import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.packer.Geolocs;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;

/**
 * Takes low fidelity SUPPLY tasks marked with the LOW_FIDELITY prep
 * and converts them into TRANSPORT tasks.
 *
 * The only real work that goes on is adding a FROM prep of Blue Grass, KY,
 * which is prototypical ammo depot.
 */
public class AmmoLowFidelityExpanderPlugin extends UTILExpanderPluginAdapter {

  public void localSetup() {     
    super.localSetup();
    glmPrepHelper = new GLMPrepPhrase (logger);
  }

  /**
   * State that we are interested in all transport tasks
   * @param task the task to test.
   * @return true if the tasks verb is SUPPLY, false otherwise
   */
  public boolean interestingTask(Task task){
    boolean hasSupply = task.getVerb().equals (Constants.Verb.SUPPLY);
    if (!hasSupply)
      return false;

    if (!prepHelper.hasPrepNamed (task, GLMTransConst.LOW_FIDELITY)) {
      if (isDebugEnabled())
	debug (".interestingTask - ignoring SUPPLY task " + task.getUID () + " that doesn't have a LOW_FIDELITY prep.");
      return false;
    }

    if (isDebugEnabled())
      debug (".interestingTask - processing SUPPLY task " + task.getUID () + " that has a LOW_FIDELITY prep.");

    return true;
  }

  /**
   * Implemented for UTILExpanderPlugin interface
   *
   * Break up tasks into constituent parts.
   */
  public Vector getSubtasks(Task parentTask) {
    Vector childTasks = new Vector ();

    Asset lowFiAsset = parentTask.getDirectObject();
    Task subTask = makeTask (parentTask, lowFiAsset);
    childTasks.addElement (subTask);

    return childTasks;
  }

  /** 
   * Makes subtask of parent task, with given direct object.
   *
   * removes OFTYPE prep, since it's not needed by TRANSCOM
   * Adds FROM prep.
   **/
  Task makeTask (Task parentTask, Asset directObject) {
    Task newtask = expandHelper.makeSubTask (ldmf,
					     parentTask,
					     directObject,
					     ((PluginBindingSite)getBindingSite()).getAgentIdentifier());
    glmPrepHelper.removePrepNamed(newtask, Constants.Preposition.OFTYPE);
    // mark the new task as an aggregate low fi task
    glmPrepHelper.addPrepToTask (newtask, 
				 prepHelper.makePrepositionalPhrase(ldmf,
								    Constants.Preposition.FROM,
								    Geolocs.blueGrass()));
    ((NewTask)newtask).setVerb (Constants.Verb.Transport);

    return newtask;
  }

  protected GLMPrepPhrase glmPrepHelper;
}
