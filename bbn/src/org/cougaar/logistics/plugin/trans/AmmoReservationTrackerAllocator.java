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
import java.util.HashMap;
import java.util.Map;
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
import org.cougaar.planning.ldm.measure.*;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.WorkflowImpl;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.util.TimeSpan;

import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.glm.packer.Geolocs;
import org.cougaar.glm.ldm.plan.AlpineAspectType;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.filter.UTILAllocatorPluginAdapter;
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

public class AmmoReservationTrackerAllocator extends UTILAllocatorPluginAdapter {
  public static long MILLIS_PER_DAY = 1000*60*60*24;
  public static String START = "Start";

  /**
   * Provide the callback that is paired with the buffering thread, which is a
   * listener.  The buffering thread is the listener to the callback
   *
   * @return an ExpandableTaskCallback with the buffering thread as its listener
   * @see org.cougaar.lib.callback.UTILWorkflowCallback
   */
  /*
  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) { 
    if (isInfoEnabled())
      info (getName () + " : Filtering for Transport Tasks...");

    myInputTaskCallback = new UTILWorkflowCallback (bufferingThread, logger) {
	protected UnaryPredicate getPredicate () {
	  return new UnaryPredicate() {
	      public boolean execute(Object o) {
		if ( o instanceof Task ) {
		  if (((Task)o).getVerb ().equals (Constants.Verb.TRANSPORT)) {
		    if (isDebugEnabled()) {
		      debug (getName () + " found task " + ((Task)o).getUID());
		    }

		    Task task = (Task) o;

		    if (task.getWorkflow() != null && 
			!taskInWorkflow(task, task.getWorkflow())) // it's already been publish removed
		      return false;
		    
		    if (task.getWorkflow () == null && isReservedTask(task)) {
		      if (isInfoEnabled()) {
			info (getName () + " skipping task " + ((Task)o).getUID() + " with null workflow.");
		      }
		      return false;
		    }

		    return true;
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
  */

  /** 
   * Implemented for UTILBufferingPlugin interface
   *
   * filter for tasks you find interesting. 
   * @param t Task to check for interest
   * @return boolean true if task is interesting
   * @see UTILBufferingPlugin
   */
  public boolean interestingTask(Task task) { 
    boolean hasTransport = task.getVerb().equals (Constants.Verb.TRANSPORT);

    if (isInfoEnabled() && hasTransport)
      info (getName () + " REALLY interested in task " + task.getUID());
    else if (isInfoEnabled () && !hasTransport)
      info (getName () + " not interested in task " + task.getUID());

    if (!hasTransport) return false;

    if (isDebugEnabled()) {
      debug (getName () + " found task " + task.getUID());
    }

    if (task.getWorkflow() != null && 
	!taskInWorkflow(task, task.getWorkflow())) // it's already been publish removed
      return false;
		    
    if (task.getWorkflow () == null && isReservedTask(task)) {
      if (isInfoEnabled()) {
	info (getName () + " skipping task " + task.getUID() + " with null workflow.");
      }
      return false;
    }

    return true;
  }

  public boolean interestingAsset(Asset a) {
    if (!(a instanceof Organization)) return false;

    Organization org = (Organization) a;

    if (org.isSelf()) return false; // ignore self

    RelationshipSchedule schedule = org.getRelationshipSchedule();
    Collection orgCollection = 
      schedule.getMatchingRelationships(Constants.Role.STRATEGICTRANSPORTATIONPROVIDER,
					TimeSpan.MIN_VALUE,
					TimeSpan.MAX_VALUE);

    if (isWarnEnabled () && !(orgCollection.isEmpty ())) {
      warn ("found asset " + org);
    }
    else {
      warn ("ignoring " + org);
    }
      
    return (!(orgCollection.isEmpty ()));
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
  //  public boolean interestingExpandedTask (Task t) { return false; }

  /** 
   * Implemented for UTILBufferingPlugin interface
   *
   * @param tasks that have been buffered up to this point
   * @see UTILBufferingPlugin
   */
  public void processTasks (List tasks) {
    if (isInfoEnabled())
      info (getName () + 
	    ".processTasks - processing " + tasks.size() + " tasks.");
    Map reservedToActual = new HashMap ();
    for (int i = 0; i < tasks.size (); i++) 
      handleTask ((Task) tasks.get (i), reservedToActual);

    for (Iterator iter = reservedToActual.keySet().iterator(); iter.hasNext(); ) {
      Task reserved = (Task)iter.next ();
      Task actual   = (Task) reservedToActual.get(reserved);
      dealWithReservedTask (actual, reserved);
    }

    Asset TRANSCOM = (Asset) getAssets().next();

    for (int i = 0; i < tasks.size (); i++) {
      Task task = (Task) tasks.get (i);
      
      Date from = prefHelper.getReadyAt  (task);
      Date to   = prefHelper.getBestDate (task);

      AspectValue [] values = new AspectValue [2];
      values[0] = new AspectValue (AspectType.START_TIME, (double)from.getTime());
      values[1] = new AspectValue (AspectType.END_TIME,   (double)to.getTime());
      double confidence = allocHelper.MEDIUM_CONFIDENCE;

      if (task.getPlanElement () == null) {// could have been disposed
	PlanElement pe = allocHelper.makeAllocation(this,
						    ldmf, realityPlan, task, TRANSCOM, 
						    values,
						    confidence,
						    Constants.Role.TRANSPORTER);
	if (isWarnEnabled()) {
	  warn ("Allocating " + task.getUID () + " to " + TRANSCOM);
	}

	publishAdd (pe);
      }
    }
  }

  /**
   * find matching reservation transport task
   * see if date overlaps
   * if it does, publish remove it and replace it with one with altered date span and quantity
   *
   * OK - could be MUCH more efficient!
   */
  public void handleTask(Task task1, Map reservedToActual) {
    // find matching reservation transport task
    final Task task = task1;
    final Collection units = findForPreps (task);
    final boolean isReserved = isReservedTask (task);

    if (isReservedTask (task) && !ownWorkflow(task)) {
      if (isInfoEnabled()) info (".handleTask - skipping reserved task " + task.getUID () + 
				" that's not in it's own workflow.");
      return;
    }

    if (isInfoEnabled())
      info (getName () + ".handleTask - looking through blackboard for task to match " + 
	    ((isReserved) ? "reserved " : "normal ") + task.getUID());

    Collection matchingTaskCollection = blackboard.query (new UnaryPredicate () {
	public boolean execute (Object obj) {
	  if (!(obj instanceof Task)) return false;
	  Task examinedTask = (Task) obj;
	  if (task.getUID().equals(examinedTask.getUID())) {
	    if (isDebugEnabled())
	      debug ("skipping self " + examinedTask.getUID());
	    return false; // don't match yourself
	  }

	  // better be a transport task
	  if (!(examinedTask.getVerb ().equals (Constants.Verb.TRANSPORT))) {
	    if (isDebugEnabled())
	      debug ("skipping non-transport task " + examinedTask.getUID());
	    return false;
	  }

	  // is it a reservation task?
	  boolean examinedIsReserved = isReservedTask (examinedTask);
	  if ((!isReserved && !examinedIsReserved) || 
	      ( isReserved &&  examinedIsReserved)) {
	    if (isDebugEnabled())
	      debug ("skipping examined transport task because same type " + examinedTask.getUID() + " and " + task.getUID());
	    return false;
	  }

	  if (examinedIsReserved) {
	    // has it already been removed from workflow?
	    if (!taskInWorkflow (examinedTask, examinedTask.getWorkflow())) {
	      if (isDebugEnabled ())
		debug ("skipping reserved transport task " + examinedTask.getUID() + 
		       " since it's already been removed from it's workflow.");
	      return false;
	    }
	  }

	  // is it for the same org? 
	  Collection examinedUnits = findForPreps (examinedTask);
	  Collection copy = new ArrayList(examinedUnits);
	  examinedUnits.retainAll (units);
	  if (examinedUnits.isEmpty ()) {
	    if (isDebugEnabled())
	      debug ("skipping transport task where units don't match " + units + " vs examined " + copy);
	    return false;
	  }

	  // are they for the same type of supply?
	  if (!contentTypesOverlap (task, examinedTask)) return false;

	  // do the dates overlap
	  Task reserved, transport;
	  if (examinedIsReserved) {
	    reserved  = examinedTask;
	    transport = task;
	  }
	  else {
	    reserved  = task;
	    transport = examinedTask;
	  }
	  
	  return transportDateWithinReservedWindow (transport, reserved);
	}
      } 
							  );
    if (!isReserved) {
      if (matchingTaskCollection.size () > 1) {
	error (".handleTask - expecting only one matching task - was " + matchingTaskCollection.size() + " tasks : " + 
	       matchingTaskCollection);
      }
    }
    if (matchingTaskCollection.isEmpty () && isInfoEnabled ()) {
      info (".handleTask - could not find matching task for " + task.getUID());
      return;
    }
    if (isInfoEnabled ()) {
      info  (".handleTask - found " + matchingTaskCollection.size () + " matches.");
    }

    for (Iterator iter = matchingTaskCollection.iterator(); iter.hasNext();) {
      Task reservedTask, actual;

      if (isReserved) {
	reservedTask = task;
	actual       = (Task)iter.next();
      }
      else {
	reservedTask = (Task) iter.next ();
	actual       = task;
      }

      if (!ownWorkflow (reservedTask)) {
	error (".handleTask - huh? reserved task " + reservedTask.getUID () + 
	       " not a member of it's own workflow " + reservedTask.getWorkflow () + 
	       "\nuids " + uidsWorkflow(reservedTask));
      }

      updateMap (reservedToActual, actual, reservedTask);
    }
  }

  protected boolean contentTypesOverlap (Task task, Task examinedTask){
    Container taskDO = (Container)task.getDirectObject ();
    Container examinedDO = (Container)examinedTask.getDirectObject ();

    ContentsPG contents = taskDO.getContentsPG ();
    Collection typeIDs  = contents.getTypeIdentifications ();

    ContentsPG examinedContents = examinedDO.getContentsPG ();
    Collection examinedTypeIDs  = examinedContents.getTypeIdentifications ();
    Collection copy = new ArrayList (examinedTypeIDs);

    copy.retainAll (typeIDs);
    if (copy.isEmpty()) {
      if (isDebugEnabled())
	debug ("skipping transport task where type ids don't match. No overlap between examined container " + 
	       examinedTypeIDs + 
	       " and other container's list " + typeIDs);
      return false;
    }
    else {
      return true;
    }
  }

  protected boolean transportDateWithinReservedWindow (Task transport, Task reserved){
    Date reservedReady = (Date) prepHelper.getIndirectObject (reserved, START);
    Date reservedBest  = prefHelper.getBestDate  (reserved);

    Date best          = prefHelper.getBestDate  (transport);

    if (reservedReady.getTime () >= best.getTime()) {
      if (isDebugEnabled())
	debug ("skipping transport task where task best " + best + 
	       " before examined ready " + reservedReady);
      return false;
    }
	  
    boolean val = (best.getTime() <= reservedBest.getTime());
    
    if (isInfoEnabled () && val) 
      info ("transport " + transport.getUID() + " best "+ best+ " between reserved " + reserved.getUID()+ 
	    " ready " + reservedReady + " and " + reservedBest);
 
    return val;
  }

  protected void updateMap (Map reservedToActual, Task actual, Task reserved) {
    Task foundActual;
    if ((foundActual = (Task) reservedToActual.get (reserved)) == null) {
      if (isInfoEnabled ()) {
	info ("initally, actual " + actual.getUID () + " matches reserved " + reserved.getUID());
      }
      reservedToActual.put (reserved, actual);
    }
    else {
      if (isDebugEnabled ()) {
	debug ("actual " + actual.getUID () + " matches reserved " + reserved.getUID());
      }

      if (prefHelper.getBestDate(foundActual).getTime()<prefHelper.getBestDate(actual).getTime()) {
	if (isInfoEnabled ()) {
	  info ("replacing foundActual " + foundActual.getUID () + " with actual " + actual.getUID () + 
		" which matches reserved " + reserved.getUID());
	}
	reservedToActual.put (reserved, actual); // replace with later date
      }
    }
  }

  protected void dealWithReservedTask (Task task, Task reservedTask) {
    if (isReservedTask (task))
      error ("arg - task "  + task.getUID () + " is a reserved task.");

    if (!isReservedTask (reservedTask))
      error ("arg - task "  + reservedTask.getUID () + " is not a reserved task.");

    if (!ownWorkflow (reservedTask)) {
      error ("huh? reserved task " + reservedTask.getUID () + " not a member of it's own workflow " + reservedTask.getWorkflow () + 
	     "\nuids " + uidsWorkflow(reservedTask)); 
      error ("assuming it will be removed -- returning.");

      return;
    }

    Date best          = prefHelper.getBestDate (task);
    Date reservedBest  = prefHelper.getBestDate (reservedTask);
    long daysLeft      = (reservedBest.getTime()-best.getTime())/MILLIS_PER_DAY;
    Date reservedReady = (Date) prepHelper.getIndirectObject (reservedTask, START);
    long currentDays   = (reservedBest.getTime()-reservedReady.getTime())/MILLIS_PER_DAY;
    if (daysLeft < 0) error ("best dates broken");

    if (isInfoEnabled ()) {
      info (getName() + ".dealWithReservedTask - applying " + task.getUID () + " best " + best +
	    " to reserved " + reservedTask.getUID() + " reserved ready " + reservedReady + " to best " + reservedBest);
    }
    // if it does, publish remove it and replace it with one with altered date span, quantity, and START prep
    
    Container reservedDO = (Container)reservedTask.getDirectObject();
    ContentsPG contents = reservedDO.getContentsPG();
    Collection weights = contents.getWeights();
    Mass weight = (Mass) weights.iterator().next();
    weights.remove (weight);
    double factor = (double)daysLeft/(double)currentDays;
    weights.add (new Mass (weight.getKilograms()*factor, Mass.KILOGRAMS));
    //    int quantity = (int) (((double)reservedDO.getQuantity ())*((double)daysLeft/(double)currentDays));

    NewWorkflow tasksWorkflow = (NewWorkflow) reservedTask.getWorkflow ();

    if (tasksWorkflow == null) {
      error ("huh? reservedTask " + reservedTask.getUID () + " workflow is null?");
      return;
    }

    int numTasksBefore = numTasksInWorkflow (tasksWorkflow);

    if (factor/*quantity*/ > 0) {
      //  AggregateAsset deliveredAsset = (AggregateAsset) ldmf.createAggregate(reservedDO.getAsset(), quantity);
      Asset deliveredAsset = reservedDO;

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

      if (isInfoEnabled ())
	info ("Reserved task " + reservedTask.getUID () + 
	      " current days " + currentDays + 
	      " daysLeft " + daysLeft + 
	      " replacing asset weight " +weight.getKilograms() + //reservedDO.getQuantity () + 
	      " with " + weight.getKilograms()*factor);//quantity);

      if (isInfoEnabled ())
	info ("on task " + replacement.getUID() + " replacing start prep date " + reservedReady + 
	      " with " + best + " - also becomes early date for task.");

      prepHelper.replacePrepOnTask (replacement, 
				    prepHelper.makePrepositionalPhrase(ldmf,START,best));

      prefHelper.replacePreference (replacement, 
				    prefHelper.makeEndDatePreference(ldmf, 
								     best, 
								     reservedBest, 
								     prefHelper.getLateDate(reservedTask)));

      replacement.setWorkflow(tasksWorkflow);
      tasksWorkflow.addTask (replacement);
      publishAdd (replacement);
      if (isInfoEnabled ())
	info ("Publishing replacement " + replacement.getUID() + " in workflow "+ tasksWorkflow.getUID() + 
	      " start " + best + " best " + reservedBest + " dodic " + reservedDO.getContentsPG().getTypeIdentifications());

      if (best.getTime () != ((Date)prepHelper.getIndirectObject(replacement, START)).getTime())
	error ("replacement start " + prepHelper.getIndirectObject(replacement, START) + " != " + best);

      if (!taskInWorkflow(replacement, tasksWorkflow))
	error ("huh? after adding to workflow, replacement " + replacement.getUID () + " is not in workflow " + tasksWorkflow + "?"); 
    }
    else {
      if (isInfoEnabled ())
	info ("Removing reserved task " + reservedTask.getUID () + " since weight is zero. Days Left was " + daysLeft + 
	      ", current days was " + currentDays + " parent was " + reservedTask.getParentTaskUID());
    }

    tasksWorkflow.removeTask (reservedTask);
    publishRemove(reservedTask);

    if (taskInWorkflow(reservedTask, tasksWorkflow))
      error ("huh? after removing, reserved task " + reservedTask.getUID () + " is still a member of workflow " + tasksWorkflow); 

    int numTasksAfter = numTasksInWorkflow (tasksWorkflow);

    if (numTasksAfter == 0) {
      final Task reservedCopy = reservedTask;
      Collection parents = blackboard.query (new UnaryPredicate () { 
	  public boolean execute (Object obj) { 
	    if (obj instanceof Task) {
	      return (((Task) obj).getUID().equals (reservedCopy.getUID()));
	    } 
	    else return false;
	  }
	}
					     );

      Task parent = (Task) parents.iterator ().next();
      PlanElement exp = parent.getPlanElement();
      if (exp == null) {
	if (isWarnEnabled ()) {
	  warn ("found task " + parent.getUID () + " that had no plan element.");
	}
      } 
      else {
	publishRemove(exp);
	AllocationResult ar = new AllocationResult(1.0, true, exp.getEstimatedResult().getAspectValueResults());
	Disposition disposition =
	  ldmf.createDisposition(parent.getPlan(), parent, ar);
	publishAdd (disposition);
	if (isWarnEnabled ())
	  warn (" task " + parent.getUID () + " will get a disposition.");
      }
    }

    if (factor < 0.00000001 /*quantity == 0*/ && (numTasksAfter != numTasksBefore-1))
      error ("Reserved task " + reservedTask.getUID() + "'s workflow had " + numTasksBefore + " should have " + (numTasksBefore-1) + 
	     " but has " + numTasksAfter);
    else if (factor /*quantity*/ > 0 && (numTasksAfter != numTasksBefore))
      error ("Reserved task " + reservedTask.getUID() + "'s workflow had " + numTasksBefore + " != numTaskAfter, which is " + numTasksAfter);
  }

  protected boolean isReservedTask (Task task) {
    return (prepHelper.hasPrepNamed (task, START));
  }

  protected boolean ownWorkflow (Task task) {
    if (task.getWorkflow () == null) 
      return false;

    return taskInWorkflow (task, task.getWorkflow());
  }

  protected boolean taskInWorkflow (Task task, Workflow workflow) {
    String [] uidsInWorkflow = ((WorkflowImpl)workflow).getTaskIDs ();
    boolean found = false;
    for (int i = 0; i < uidsInWorkflow.length && !found; i++)
      if (uidsInWorkflow[i].equals(task.getUID().toString()))
	found = true;

    return found;
  }

  protected String uidsWorkflow (Task task) {
    return uids (((WorkflowImpl)task.getWorkflow ()).getTaskIDs());
  }

  protected String uids (String [] array) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < array.length; i++)
      buf.append (array[i] + ", ");
    return buf.toString();
  }

  protected int numTasksInWorkflow (Workflow workflow) {
    int num = 0;
    for (Enumeration enum = workflow.getTasks (); enum.hasMoreElements(); enum.nextElement()) { num++; }
    return num;
  }

  protected Collection findForPreps (final Task task) {
    List units = new ArrayList();
    if (task instanceof MPTask) {
      Collection parents = ((MPTask)task).getComposition().getParentTasks ();
      for (Iterator iter = parents.iterator(); iter.hasNext(); ) {
	Task parentTask = (Task) iter.next();
	if (prepHelper.hasPrepNamed (parentTask, Constants.Preposition.FOR))
	  units.add (prepHelper.getIndirectObject (parentTask, Constants.Preposition.FOR));
      }
    }
    else {
      if (prepHelper.hasPrepNamed (task, Constants.Preposition.FOR)) {
	units.add (prepHelper.getIndirectObject (task, Constants.Preposition.FOR));
      }
      else {
	if (isWarnEnabled())
	  warn ("no FOR prep on task " + task.getUID() + " using UID owner ");
	units.add (task.getUID().getOwner());
      }
    }

    if (isDebugEnabled())
      debug ("Units for " + task.getUID() + " were " + units);
    
    return units;
  }    
}
