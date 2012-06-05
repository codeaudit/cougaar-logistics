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

import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.lib.vishnu.client.custom.CustomVishnuAggregatorPlugin;

import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.logistics.ldm.Constants;

import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationRoute;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.GLMPrepPhrase;

/**
 * This class is necessary because the ShipPacker needs a DataXMLizer that <br>
 * adds information specific to the ship packing problem.
 */
public class SeaVishnuPlugin extends GenericVishnuPlugin {
  protected int SUB_PROCESS_SIZE;

  public void localSetup () {
    super.localSetup ();

    try {
      SUB_PROCESS_SIZE = (getMyParams().hasParam ("SUB_PROCESS_SIZE")) ?
	getMyParams().getIntParam("SUB_PROCESS_SIZE") : 100;
    } catch (Exception bogus) { error ("got bogus, never happen exception " + bogus); }

    glmPrepHelper = new GLMPrepPhrase (logger);
  }

  Comparator tasksComparator = new TaskSorter();

  /**
   * Sort first by arrival time window width, then by UID.
   *
   * E.g. a task with ready at date of 9-10 and arrival at 10-10 will go
   * before a task with ready at of 8-10 and arrival of 10-10, since it's
   * more constrained.
   *
   * If don't sort by UID, throws away subsequently added tasks with same
   * arrival times are previous ones...
   */
  class TaskSorter implements Comparator {
    public int compare (Object obj1, Object obj2) {
      Task t1 = (Task) obj1;
      Task t2 = (Task) obj2;
      long best1  = prefHelper.getBestDate (t1).getTime();
      long ready1 = prefHelper.getReadyAt  (t1).getTime();
      long diff1  = best1 - ready1;
      long best2  = prefHelper.getBestDate (t2).getTime();
      long ready2 = prefHelper.getReadyAt  (t2).getTime();
      long diff2  = best2 - ready2;
      if (diff1 < diff2)
	return -1;
      else if (diff1 > diff2)
	return 1;
      
      return t1.getUID().compareTo (t2.getUID());
    }

    public boolean equals (Object obj1, Object obj2) {
      Task t1 = (Task) obj1;
      Task t2 = (Task) obj2;
      return (t1.equals (t2));
    }
  }

  /**
   * This avoid n^2 problem with prerequisite calculations within Vishnu!
   */
  public void processTasks (List tasks) {
    // make a sorted set of tasks, sorted by best arrival time
    SortedSet sorted = new TreeSet (tasksComparator);
    sorted.addAll (tasks);

    if (isInfoEnabled ()) {
      info ("processTasks - got " + tasks.size() + 
	    " now has " + sorted.size() + 
	    " sorted.");
    }

    List toProcess = new ArrayList(SUB_PROCESS_SIZE);
    for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
      toProcess.add (iter.next());
      if (toProcess.size () == SUB_PROCESS_SIZE) {
	if (isInfoEnabled ()) {
	  info ("processing task sub set of size " + toProcess.size());
	}

	super.processTasks (toProcess);
	toProcess.clear ();
      }
    }

    if (!toProcess.isEmpty ()) {
      if (isInfoEnabled ()) {
	info ("at end, processing task sub set of size " + toProcess.size());
      }

      super.processTasks (toProcess);
    }
  }

  /** 
   * override to use a different XMLizer <p>
   *
   * SeaDataXMLize adds fields for sea-specific info, like dealing with Ammunition
   */
  protected XMLizer createXMLizer (boolean direct) {
    GenericDataXMLize xmlizer = new SeaDataXMLize (direct, logger);
    setDataXMLizer(xmlizer);
    return xmlizer;
  }

  protected Task createMainTask (Task task, Asset asset, Date start, Date end, Date setupStart, Date wrapupEnd) {
    Task mainTask = super.createMainTask (task, asset, start, end, setupStart, wrapupEnd);
    
    // attach route
    TransportationRoute route = (TransportationRoute)
      glmPrepHelper.getIndirectObject(task, GLMTransConst.SEAROUTE);

    glmPrepHelper.addPrepToTask(mainTask, 
				glmPrepHelper.makePrepositionalPhrase(ldmf,
								      Constants.Preposition.VIA,
								      route));
    glmPrepHelper.removePrepNamed(mainTask, GLMTransConst.SEAROUTE); // not necessary now that we put it on the VIA prep

    return mainTask;
  }

  protected GLMPrepPhrase glmPrepHelper;
}
