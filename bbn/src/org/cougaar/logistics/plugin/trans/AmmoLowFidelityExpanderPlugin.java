/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

import java.util.*;

import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.util.GLMPrepPhrase;
import org.cougaar.lib.filter.UTILExpanderPluginAdapter;
import org.cougaar.logistics.plugin.trans.GLMTransConst;
import org.cougaar.logistics.plugin.packer.Geolocs;
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

  public void processTasks (java.util.List tasks) {
      super.processTasks (getPrunedTaskList(tasks));
  }

    protected List getPrunedTaskList (List tasks) {
	java.util.List prunedTasks = new java.util.ArrayList(tasks.size());

	Collection removed = myInputTaskCallback.getSubscription().getRemovedCollection();

	for (Iterator iter = tasks.iterator(); iter.hasNext();){
	    Task task = (Task) iter.next();
	    if (removed.contains(task)) {
		if (isInfoEnabled()) {
		    info ("ignoring task on removed list " + task.getUID());
		}
	    }
	    else
		prunedTasks.add (task);
	}
	return prunedTasks;
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
					     getAgentIdentifier());
    ((NewTask)newtask).setContext(parentTask.getContext());
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
