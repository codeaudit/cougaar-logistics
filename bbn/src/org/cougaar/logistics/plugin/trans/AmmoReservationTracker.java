/*
 * <copyright>
 *  Copyright 2001-2 BBNT Solutions, LLC
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

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Vector;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.WorkflowImpl;

import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.packer.Geolocs;
import org.cougaar.glm.ldm.plan.AlpineAspectType;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;
import org.cougaar.lib.callback.*;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.lib.util.UTILPreference;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.planning.ldm.plan.Preposition;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.glm.ldm.asset.ContentsPG;

public class AmmoReservationTracker extends UTILExpanderPluginAdapter {
  public static long MILLIS_PER_DAY = 1000*60*60*24;

  /**
   * Provide the callback that is paired with the buffering thread, which is a
   * listener.  The buffering thread is the listener to the callback
   *
   * @return an ExpandableTaskCallback with the buffering thread as its listener
   * @see org.cougaar.lib.callback.UTILWorkflowCallback
   */
  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) { 
    if (isInfoEnabled())
      info (getName () + " : Filtering for Transport Tasks...");

    myInputTaskCallback = new UTILExpandableTaskCallback (bufferingThread, logger) {
	protected UnaryPredicate getPredicate () {
	  return new UnaryPredicate() {
	      public boolean execute(Object o) {
		if ( o instanceof MPTask ) {
		  if (((Task)o).getVerb ().equals (Constants.Verb.TRANSPORT)) {
		    if (isDebugEnabled()) {
		      debug (getName () + " found task " + ((Task)o).getUID());
		    }
		    boolean hasPrep = prepHelper.hasPrepNamed ((Task)o, "Start");

		    if (isDebugEnabled() && hasPrep) {
		      debug (getName () + " found transport with prep " + ((Task)o).getUID());
		    }
		    else if (isDebugEnabled() && !hasPrep) {
		      debug (getName () + " found transport without prep " + ((Task)o).getUID());
		    }

		    // not if it's a reservation task
		    return !hasPrep;
		  }
		  else 
		    return false;
		}
		else 
		  return false;
	      }
	    };
	}  
      };  

    return myInputTaskCallback;
  } 

  /** 
   * Implemented for UTILBufferingPlugin interface
   *
   * filter for tasks you find interesting. 
   * @param t Task to check for interest
   * @return boolean true if task is interesting
   * @see UTILBufferingPlugin
   */
  public boolean interestingTask(Task t) { 
    boolean hasTransport = t.getVerb().equals (Constants.Verb.TRANSPORT);

    if (isWarnEnabled() && hasTransport)
      warn (getName () + " REALLY interested in task " + t.getUID());
    else if (isWarnEnabled () && !hasTransport)
      warn (getName () + " not interested in task " + t.getUID());

    return hasTransport;
  }

  /**
   * Implemented for UTILExpansionListener
   *
   * Gives plugin a way to filter out which expanded tasks it's
   * interested in.
   *
   * @param t Task that has been expanded (getTask of Expansion)
   * @return true if task is interesting to this plugin
   */
  public boolean interestingExpandedTask (Task t) { return false; }

  /**
   * find matching reservation transport task
   * see if date overlaps
   * if it does, publish remove it and replace it with one with altered date span and quantity
   */
  public void handleTask(Task task1) {
    // find matching reservation transport task
    final Task task = task1;
    if (isWarnEnabled())
      warn (getName () + ".handleTask - looking through blackboard for task to match " + task.getUID());

    final Collection units = findForPreps ((MPTask)task);

    Collection matchingTaskCollection = blackboard.query (new UnaryPredicate () {
	public boolean execute (Object obj) {
	  if (!(obj instanceof Task)) return false;
	  Task examinedTask = (Task) obj;
	  if (task == examinedTask) return false; // don't match yourself

	  // better be a transport task
	  if (!(examinedTask.getVerb ().equals (Constants.Verb.TRANSPORT))) {
	    if (isDebugEnabled())
	      debug ("skipping non-transport task " + examinedTask.getUID());
	    return false;
	  }

	  // is it a reservation task?
	  if (!prepHelper.hasPrepNamed (examinedTask, "Start")) {
	    if (isDebugEnabled())
	      debug ("skipping transport task with no Start prep " + examinedTask.getUID());
	    return false;
	  }

	  // is it for the same org? redundant, but...
	  if (prepHelper.hasPrepNamed (examinedTask, Constants.Preposition.FOR)) {
	    String reservedUnit = (String) prepHelper.getIndirectObject (examinedTask, Constants.Preposition.FOR);
	    if (!units.contains (reservedUnit)) {
	      if (isDebugEnabled())
		debug ("skipping transport task where units don't match " + units + " vs " + reservedUnit);
	      return false;
	    }
	  }

	  // are they for the same type of supply?
	  Container taskDO = (Container)task.getDirectObject ();
	  Asset examinedDO = ((AggregateAsset)examinedTask.getDirectObject()).getAsset();

	  ContentsPG contents = taskDO.getContentsPG ();
	  Collection typeIDs = contents.getTypeIdentifications ();
	  // String taskDOId =taskDO.getTypeIdentificationPG().getTypeIdentification();
	  String examinedDOId = examinedDO.getTypeIdentificationPG().getTypeIdentification();
	  if (!typeIDs.contains(examinedDOId)) {
	    if (isDebugEnabled())
	      debug ("skipping transport task where type ids don't match. No " + examinedDOId + " in container's list " + typeIDs);
	    return false;
	  }

	  // do the dates overlap?
	  Date readyAt = prefHelper.getReadyAt (task);
	  Date examinedReady = (Date) prepHelper.getIndirectObject (examinedTask, "Start");
	  Date best         = prefHelper.getBestDate  (task);

	  if (examinedReady.getTime () > best.getTime()) {
	    if (isDebugEnabled())
	      debug ("skipping transport task where task best " + best + " before examined ready " + examinedReady);
	    return false;
	  }
	  
	  Date examinedBest = prefHelper.getBestDate  (examinedTask);
	  
	  return (best.getTime() <= examinedBest.getTime());
	}
      } 
							  );

    if (matchingTaskCollection.size () > 1)
      error (".handleTask - expecting only one matching task - was " + matchingTaskCollection.size() + " tasks : " + 
	     matchingTaskCollection);

    if (matchingTaskCollection.isEmpty () && isWarnEnabled ()) {
      warn (".handleTask - could not find matching task for " + task.getUID());
      return;
    }

    Task reservedTask = (Task) matchingTaskCollection.iterator().next ();
    
    dealWithReservedTask (task, reservedTask);
  }

  protected void dealWithReservedTask (Task task, Task reservedTask) {
    Date reservedReady = (Date) prepHelper.getIndirectObject (reservedTask, "Start");
    Date readyAt = prefHelper.getReadyAt (task);
    Date best    = prefHelper.getBestDate (task);
    Date reservedBest    = prefHelper.getBestDate (reservedTask);
    long daysLeft    = (reservedBest.getTime()-best.getTime())/MILLIS_PER_DAY;
    long currentDays = (reservedBest.getTime()-reservedReady.getTime())/MILLIS_PER_DAY;
    if (daysLeft < 0) error ("best dates broken");
    
    //    if (examinedReady.getTime () > readyAt.getTime())
    //      error (".handleTask  - huh? thought examined start " + examinedReady + " was before transport start " + readyAt);

    // if it does, publish remove it and replace it with one with altered date span, quantity, and START prep
    
    AggregateAsset reservedDO = (AggregateAsset)reservedTask.getDirectObject();
    int quantity = (int) (((double)reservedDO.getQuantity ())*((double)daysLeft/(double)currentDays));

    NewWorkflow tasksWorkflow = (NewWorkflow) reservedTask.getWorkflow ();

    if (tasksWorkflow == null) {
      error ("huh? reservedTask " + reservedTask.getUID () + " workflow is null?");
      return;
    }

    if (!taskInWorkflow(reservedTask, tasksWorkflow))
      error ("huh? reserved task " + reservedTask.getUID () + " not a member of it's own workflow " + tasksWorkflow + 
	     "\nuids " + ((WorkflowImpl)tasksWorkflow).getTaskIDs()); 

    int numTasksBefore = numTasksInWorkflow (tasksWorkflow);

    if (quantity > 0) {
      AggregateAsset deliveredAsset = (AggregateAsset) ldmf.createAggregate(reservedDO.getAsset(), quantity);

      NewTask replacement = 
	(NewTask) expandHelper.makeSubTask (ldmf,
					    reservedTask.getPlan(),
					    reservedTask.getParentTaskUID(),
					    reservedTask.getVerb(),
					    reservedTask.getPrepositionalPhrases(),
					    deliveredAsset,
					    reservedTask.getPreferences(),
					    reservedTask.getPriority(),
					    reservedTask.getSource());

      if (isWarnEnabled ())
	warn ("Reserved task " + reservedTask.getUID () + 
	      " current days " + currentDays + 
	      " daysLeft " + daysLeft + 
	      " replacing asset quantity " +reservedDO.getQuantity () + 
	      " with " + quantity);

      if (isWarnEnabled ())
	warn ("on task " + replacement.getUID() + " replacing start prep date " + reservedReady + 
	      " with " + best + " - also becomes early date for task.");

      prepHelper.replacePrepOnTask (replacement, 
				    prepHelper.makePrepositionalPhrase(ldmf,"START",best));

      prefHelper.replacePreference (replacement, 
				    prefHelper.makeEndDatePreference(ldmf, 
								     best, 
								     reservedBest, 
								     prefHelper.getLateDate(reservedTask)));

      replacement.setWorkflow(tasksWorkflow);
      tasksWorkflow.addTask (replacement);
      publishAdd (replacement);
      if (isWarnEnabled ())
	warn ("Publishing replacement " + replacement.getUID() + " in workflow "+ tasksWorkflow.getUID());
    }
    else {
      if (isWarnEnabled ())
	warn ("Removing reserved task " + reservedTask.getUID () + " since quantity is zero.");
    }

    tasksWorkflow.removeTask (reservedTask);
    publishRemove(reservedTask);

    if (taskInWorkflow(reservedTask, tasksWorkflow))
      error ("huh? after removing, reserved task " + reservedTask.getUID () + " is still a member of workflow " + tasksWorkflow); 

    int numTasksAfter = numTasksInWorkflow (tasksWorkflow);

    if (quantity == 0 && (numTasksAfter != numTasksBefore-1))
      error ("Reserved task " + reservedTask.getUID() + "'s workflow had " + numTasksBefore + " should have " + (numTasksBefore-1) + 
	     " but has " + numTasksAfter);
    else if (quantity > 0 && (numTasksAfter != numTasksBefore))
      error ("Reserved task " + reservedTask.getUID() + "'s workflow had " + numTasksBefore + " != numTaskAfter, which is " + numTasksAfter);
  }

  protected boolean taskInWorkflow (Task task, Workflow workflow) {
    String [] uidsInWorkflow = ((WorkflowImpl)workflow).getTaskIDs ();
    boolean found = false;
    for (int i = 0; i < uidsInWorkflow.length && !found; i++)
      if (uidsInWorkflow[i].equals(task.getUID().toString()))
	found = true;

    return found;
  }

  protected int numTasksInWorkflow (Workflow workflow) {
    int num = 0;
    for (Enumeration enum = workflow.getTasks (); enum.hasMoreElements(); enum.nextElement()) { num++; }
    return num;
  }

  protected Collection findForPreps (final MPTask task) {
    List units = new ArrayList();
    Collection parents = task.getComposition().getParentTasks ();
    for (Iterator iter = parents.iterator(); iter.hasNext(); ) {
      Task parentTask = (Task) iter.next();
      if (prepHelper.hasPrepNamed (parentTask, Constants.Preposition.FOR))
	units.add (prepHelper.getIndirectObject (parentTask, Constants.Preposition.FOR));
    }
    return units;
  }    
}
