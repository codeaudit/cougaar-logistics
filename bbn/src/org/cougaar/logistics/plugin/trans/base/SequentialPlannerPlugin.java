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
package org.cougaar.logistics.plugin.trans.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UniqueObject;

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.SubTaskResult;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Schedule;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.glm.ldm.asset.Deck;
import org.cougaar.glm.ldm.asset.PhysicalAsset;
import org.cougaar.lib.callback.UTILAllocationListener;
import org.cougaar.lib.callback.UTILAllocationCallback;
import org.cougaar.lib.callback.UTILAssetListener;
import org.cougaar.lib.callback.UTILAssetCallback;
import org.cougaar.lib.callback.UTILExpansionListener;
import org.cougaar.lib.callback.UTILExpansionCallback;
import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;
import org.cougaar.lib.filter.UTILBufferingPluginAdapter;
import org.cougaar.lib.util.UTILPluginException;
import org.cougaar.lib.util.UTILAllocate;

import org.cougaar.logistics.plugin.trans.GLMTransConst;
import org.cougaar.logistics.plugin.trans.base.SequentialScheduleElement;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 ** Base class that orchestrates sequential backwards planning.
 **
 ** Handles the state transitions of each schedule element that makes up the schedule
 ** attached to each parent task.
 **/
public abstract class SequentialPlannerPlugin extends UTILBufferingPluginAdapter
  implements UTILAllocationListener, UTILAssetListener, UTILExpansionListener {
  protected List delayedTasks = new ArrayList ();
    
  Map childToParentUID = new HashMap();
  Map taskToSSE = new HashMap();       
  boolean tryToReplanOverlaps = false;
  protected long waitTime = 10000; // millis

  public void localSetup() {     
    super.localSetup();

    try {
      if (getMyParams().hasParam ("tryToReplanOverlaps")) {
	tryToReplanOverlaps = getMyParams().getBooleanParam("tryToReplanOverlaps");
      }
    } catch (Exception e) { warn ("got really unexpected exception " + e); }
  }

  // Plug in creates both Expansions and Allocations so it listens for both
  public void setupFilters() {
    super.setupFilters();
	
    if (isInfoEnabled())
      info (getName () + " : Filtering for generic Assets...");
    addFilter (myAssetCallback    = createAssetCallback    ());
	
    if (isInfoEnabled())
      info (getName () + " : Filtering for Expansions...");
    addFilter (myExpansionCallback    = createExpansionCallback    ());
	
    if (isInfoEnabled())
      info (getName () + " : Filtering for Allocations...");
    addFilter (myAllocCallback    = createAllocCallback    ());
  }

  protected UTILExpandableTaskCallback myInputTaskCallback;
  protected UTILFilterCallback getInputTaskCallback() { return myInputTaskCallback; }
    
  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) { 
    if (isInfoEnabled())
      info (getName () + " : Filtering for Naked Tasks...");
	
    myInputTaskCallback = new UTILExpandableTaskCallback (bufferingThread, logger);  
    return myInputTaskCallback;
  } 
    
  public boolean interestingTask(Task t) { return true; }
    
  protected void execute() {
    super.execute ();

    if (!delayedTasks.isEmpty ()) {
      processTasks (new ArrayList());
    }
  }

  /** 
   * If necessary subordinates have not reported yet, accumulates tasks into
   * a delayedTasks list, and asks to be kicked again in 10 seconds, by which
   * time hopefully the subordinates have reported.
   *
   * Solves the race condition between tasks showing up and subordinates showing up.
   * @param tasks to process
   */
  public void processTasks (List tasks) {
    if (!allNecessaryAssetsReported()) { // if need subordinates aren't there yet, way 10 seconds
      delayedTasks.addAll (tasks);

      if (logger.isInfoEnabled()) {
	logger.info (getName() + " - necessary subords have not reported, so waiting " + waitTime + 
		     " millis to process " + delayedTasks.size () + 
		     " tasks.");
      }

      examineBufferAgainIn (waitTime); // wait 10 seconds and check again
    }
    else { // ok, all subords are here, lets go!
      if (logger.isInfoEnabled()) {
	logger.info (getName() + 
		     " - all necessary subords have reported, so processing " + tasks.size() + 
		     " tasks.");
      }

      tasks.addAll (delayedTasks);
      delayedTasks.clear();

      tasks = getPrunedTaskList (tasks);

      for (int i = 0; i < tasks.size (); i++) {
	if (isDebugEnabled())
	  debug(getName () + ".processTasks calling handleTask - " + (Task) tasks.get (i));
		
	handleTask ((Task) tasks.get (i));
      }
    }
  }

  /** probably unnecessary */
  protected List getPrunedTaskList (List tasks) {
    java.util.List prunedTasks = new java.util.ArrayList(tasks.size());

    Collection removed = myInputTaskCallback.getSubscription().getRemovedCollection();

    for (Iterator iter = tasks.iterator(); iter.hasNext();){
      Task task = (Task) iter.next();
      if (removed.contains(task)) {
	if (isWarnEnabled()) {
	  warn ("ignoring task on removed list " + task.getUID());
	}
      }
      else
	prunedTasks.add (task);
    }
    return prunedTasks;
  }

  protected boolean allNecessaryAssetsReported () {
    return true;
  }

  /**
   * handleTask creates an empty schedule and attaches it to the parent task. It also creates 
   * an expansion which starts empty and initiates a "planning cycle".
   */
  public void handleTask(Task t) {
    if (isDebugEnabled())
      debug(getName () + ".handleTask, adding schedule prep to task " + t);

    prepHelper.addPrepToTask(t, prepHelper.makePrepositionalPhrase(ldmf,
								   GLMTransConst.SequentialSchedule,
								   createEmptyPlan(t)));
    
    publishChange (t); // we added a prep, should publish change it

    Workflow wf = makeEmptyWorkflow(t);
    Expansion exp = ldmf.createExpansion(t.getPlan(), t, wf, null);
    publishAdd(exp);
	
    turnCrank(t);
  }
    
  private Workflow makeEmptyWorkflow(Task t) {
    NewWorkflow wf = ldmf.newWorkflow();
    wf.setParentTask(t);
    return wf;
  }
    
  /**
   * turnCrank is a basic planning cycle. It looks through all the elements in the empty
   * schedule and if one is ready to be planned it plans it. It also places the parenttask in a
   * hashtable so it can be easily retrieved later when the subtask is succesfully allocated.
   *
   * Operates on the parent task of a backwards-planning expansion, e.g. the parent task
   * of a typical Conus->Air/Sea->Theater triplet workflow.
   *
   * @param task parent task of expansion
   */
  public void turnCrank(Task task) {
    if (isDebugEnabled()) debug(getName () + "---Turning Crank: S " + task.getUID());

    PrepositionalPhrase prep = prepHelper.getPrepNamed(task, GLMTransConst.SequentialSchedule);
    
    if (prep == null) {
      error(getName () + ".turnCrank - ERROR - no prep named " + GLMTransConst.SequentialSchedule +
	    " on task " + task);
      return;
    }

    Schedule sched = (Schedule) prep.getIndirectObject();
    Enumeration enum = sched.getAllScheduleElements();
    while (enum.hasMoreElements()) {
      SequentialScheduleElement spe = (SequentialScheduleElement)enum.nextElement();
      if (isDebugEnabled()) 
	debug(getName () + "------spe " + 
	     spe + " is planned: "+
	     spe.isPlanned() + " is ready " + 
	     spe.isReady ());
      if ((!spe.isPlanned()) && (spe.isReady())) {
	if (spe.getTask () != null) {
	  error ("huh? there already is a task " + spe.getTask().getUID () + " for " + spe);
	}

	Task subtask = spe.planMe(this);
	attachSubtask (subtask, spe);
	childToParentUID.put(subtask.getUID().toString(), task);
      }
    }
    if (isDebugEnabled()) debug(getName () + "---Turning Crank: E" + task.getUID());
  }
    
  /**
   * attachSubtask adds a created subtask to the parent task's expansion.
   * remembers subtask->schedule element mapping in a map.
   * sets the task pointer on the schedule element
   */
  protected void attachSubtask (Task subtask, SequentialScheduleElement spe) {
    enterHash(subtask.getUID().toString(), spe);

    // ScheduleElement now knows which task it has made
    spe.setTask (subtask);
	
    // Not needed down the line
    prepHelper.removePrepNamed(subtask, GLMTransConst.SequentialSchedule);

    // don't do this - drags info into downstream agents
    // add back pointer to spe
    /*
    prepHelper.addPrepToTask(subtask, prepHelper.makePrepositionalPhrase(ldmf,
										 GLMTransConst.SCHEDULE_ELEMENT,
										 spe));
    */
	
    Task parentTask = spe.getParentTask();
    Expansion exp = (Expansion)parentTask.getPlanElement();
    if (exp == null) {
      if (isInfoEnabled ()) {
	info ("attachSubtask - task " + parentTask.getUID() + 
	      "'s plan element is missing, so skipping trying to process subtask " + 
	      subtask.getUID() + " must be in process of rescinds?");
      }
      return;
    }
    NewWorkflow wf = (NewWorkflow)exp.getWorkflow();

    // So initial creation doesn't get added and changed in same pass
    boolean workflowWasEmpty = !wf.getTasks().hasMoreElements();

    wf.addTask(subtask);

    //    if (wf.getTasksIDs().length > 3) {
    //      error ("parent " + parentTask.getUID ()+ " has " + wf.getTasksIDs ().length + " subtasks?");
    //    }

    ((NewTask)subtask).setWorkflow(wf);
    ((NewTask)subtask).setParentTask(parentTask);
    publishAdd(subtask);
    if (!workflowWasEmpty) {
      publishChange(exp);
    }  // else Workflow is empty, so publishAdd already done
  }	
  
  /**
   * createEmptyPlan creates an empty schedule (there are no tasks 
   * yet in the workflow) made up of custom schedule elements. These 
   * schedule elements define a <code>planMe</code> method that adds a 
   * subtask to the parent task's workflow.  Generally for each schedule
   * element in the schedule returned here, there will be a subtask added
   * to the workflow.
   *
   * @see org.cougaar.logistics.plugin.trans.base.SequentialScheduleElement#planMe
   * @param parent referenced by sequential schedule elements in the returned schedule
   * @return Schedule of sequential schedule elements that represent the steps of the backwards planning
   */
  public abstract Schedule createEmptyPlan(Task parent);


  public void handleIllFormedTask (Task t) {
    reportIllFormedTask(t);
    publishAdd (expandHelper.makeFailedExpansion (null, ldmf, t));
  }

  /**********************/    
  /*** Asset Listener ***/
  /**********************/

  protected UTILAssetCallback myAssetCallback;
  protected UTILAssetCallback createAssetCallback () { return new UTILAssetCallback(this, logger);  } 
  protected UTILAssetCallback getAssetCallback    () { return myAssetCallback; }
    
  public boolean interestingAsset (Asset a) { return true; }
    
  public void handleNewAssets     (Enumeration e) {}
    
  public void handleChangedAssets (Enumeration e) {}
    
  protected final Iterator getAssets() {
    Collection assets = 
      getAssetCallback().getSubscription ().getCollection();
	
    if (assets.size() != 0) {
      return assets.iterator();
    }
    return null;
  }
    

  /**************************/
  /*** Expansion Listener ***/
  /**************************/
        
  protected UTILExpansionCallback myExpansionCallback;
  protected UTILExpansionCallback createExpansionCallback () { return new UTILExpansionCallback(this, logger);  } 
  protected UTILExpansionCallback getExpansionCallback    () { return myExpansionCallback; }
    
  public boolean interestingExpandedTask (Task t) { return interestingTask(t); }

  public void handleFailedExpansion(Expansion exp, List failedSubTaskResults) {
    if (isInfoEnabled()) info(getName () + "---handleFailedExpansion: S");
    reportChangedExpansion (exp);
	
    if (failedSubTaskResults.size () == 0)
      error(getName () + " - empty list of failed subtasks?"); 
	
    // Go through the list of failed subtasks
    Iterator failed_it = failedSubTaskResults.iterator();
    while (failed_it.hasNext()) {
      // Get the next failed task
      SubTaskResult str = (SubTaskResult)failed_it.next();
      Task failed_e_task = str.getTask();

      if (isInfoEnabled()) {
        info(getName() + ".handleFailedExpansion - Failed task : " + failed_e_task +
                "\n\twith pe " + failed_e_task.getPlanElement ());
        info("\nPref-Aspect comparison : ");
        expandHelper.showPlanElement (failed_e_task);
      }
    }
    if (isInfoEnabled()) info(getName () + "---handleFailedExpansion: E");
  }
    
  public void handleConstraintViolation(Expansion exp, List violatedConstraints) {
    throw new UTILPluginException (getName (), 
				   "handleConstraintViolation : expansion " + exp +
				   " has violated constraints that were ignored.");
  }
    
  public boolean wantToChangeExpansion(Expansion exp) { return false; }
    
  public void changeExpansion(Expansion exp) {}
    
  public void publishChangedExpansion(Expansion exp) {
    publishChange(exp);
  }

  /** 
   * Mainly just calls updateAllocationResult on <code>exp</code>
   * Also prints debug info when tasks fail.
   */
  public void reportChangedExpansion(Expansion exp) {
    if (isDebugEnabled()) {
      debug(getName () + "---reportChangedExpansion: S - reporting changed expansion to superior.");
    }
	
    Task task = exp.getTask();
    Schedule sched = null;

    if (!prepHelper.hasPrepNamed (task, GLMTransConst.SequentialSchedule)) {
      updateAllocationResult (exp); 
      return;
    }

    try {
      sched = (Schedule)prepHelper.getPrepNamed(task, GLMTransConst.SequentialSchedule).getIndirectObject();
    } catch (Exception e) {
      error (getName () + 
	     ".reportChangedExpansion - ERROR - no prep " + GLMTransConst.SequentialSchedule + " on " +
	     task);
      return;
    }
	
    Enumeration enum = sched.getAllScheduleElements();
    while (enum.hasMoreElements()) {
      SequentialScheduleElement spe = (SequentialScheduleElement)enum.nextElement();
      AllocationResult reportedResult = exp.getReportedResult();
      boolean isFailure = (reportedResult != null) ? !reportedResult.isSuccess () : false;
 
      if (!spe.isPlanned() && !isFailure) {
 	if (isDebugEnabled()) debug(getName () + "---reportChangedExpansion: E (not updating Alloc b/c interim change)"); 
	return;
      }
    }
	
    if (isDebugEnabled()) debug(getName () + "---reportChangedExpansion: E (updating Alloc)"); 
    updateAllocationResult (exp); 
  }
    
  public void handleSuccessfulExpansion(Expansion exp, List successfulSubtasks) { 
    if (isDebugEnabled()) {
      AllocationResult estAR = exp.getEstimatedResult();
      AllocationResult repAR = exp.getReportedResult();
      String est = "e null/";
      String rep = " r null";
      if (estAR != null)
	est = " e " + (estAR.isSuccess () ? "S" : "F") +  " - " +
	  (int) (estAR.getConfidenceRating ()*100.0) + "% /";
      if (repAR != null)
	rep = " r " + (repAR.isSuccess () ? "S" : "F") + " - " +
	  (int) (repAR.getConfidenceRating ()*100.0) + "%";
      
      debug (getName () + 
	     " : got successful expansion for task " + exp.getTask ().getUID() + 
	     est + rep);
    }
  }
    
  /***************************/
  /*** Allocation Listener ***/
  /***************************/
    
  protected UTILAllocationCallback myAllocCallback;
  protected UTILAllocationCallback createAllocCallback () { return new UTILAllocationCallback(this, logger);  } 
  protected UTILAllocationCallback getAllocCallback    () { return myAllocCallback; }
    
  public boolean interestingNotification(Task t) { 
    boolean interest = interestingTask (t);
    if (isDebugEnabled()) {
      if (interest) {
	debug(getName()+": noticing expansion I made of " + t.getUID() + " changed.");
      }
      else {
	debug(getName()+": ignoring expansion made by GLMTransTranscomExpander of " + t.getUID());
      }
    }
    return interest; 
  }
  public boolean needToRescind (Allocation alloc) { return false; }
  public boolean handleRescindedAlloc (Allocation alloc) { return false; }
    

  public void handleRemovedAlloc (Allocation alloc) {
    if (isDebugEnabled()) {
      String unit = "Undefined";//(prepHelper.hasPrepNamed(alloc.getTask (), Constants.Preposition.FOR)) ? 
      //("" + prepHelper.getPrepNamed(alloc.getTask (), Constants.Preposition.FOR)) : "nonUnit";
      debug (getName () + ".handleRemovedAlloc : alloc was removed for task " + 
	    alloc.getTask ().getUID () + " w/ d.o. " +
	    alloc.getTask ().getDirectObject () + " from " + unit);
    }
    
    if (isInfoEnabled()) {
      info ("got remove alloc for task " + alloc.getTask().getUID() + 
	    " verb " + alloc.getTask().getVerb());
    }

    String key = alloc.getTask ().getUID().toString();
    Object something = childToParentUID.remove(key);
    if (something == null) {
      if (isInfoEnabled()) {
	info (getName() + " - no task with uid " + alloc.getTask ().getUID() + " in child->parent map?");
      }
    }

    something = taskToSSE.remove(key);
    if (something == null) {
      if (isInfoEnabled()) {
	info (getName() + " - no task with uid " + alloc.getTask ().getUID() + " in task->schedule element map?");
      }
    }
  }
    
  public void publishRemovalOfAllocation(Allocation alloc) {
    if (isDebugEnabled())
      debug (getName () + " : removing allocation for task " +
	    alloc.getTask ().getUID ());
	
    try {
      publishRemove (alloc); 
    } catch (Exception e) {
      if (isDebugEnabled())
	debug (getName () + " : publishRemovalOfAllocation - got reset claim exception, ignoring...");
    }
  }
    
  /**
   * When an allocation is succesful you ignore it unless it is a meaningful allocation.
   * Meaningful is defined as having a non-null reported result and a highest confidence rating.
   * This would indicate that an allocation to an actual resource has been done. If there is
   * a successful allocation the schedule element corresponding to the task will be grabbed and
   * finishPlan will be run on it. Then another planning cycle will begin.
   *
   * @param alloc that has been changed (i.e. a downstream agent has sent back an allocation result)
   */
  public void handleSuccessfulAlloc(Allocation alloc) {
    if (isDebugEnabled()) {
      String assetInfo = (alloc.getAsset() instanceof PhysicalAsset) ? 
	" is physical asset " : " is not physical, is " + alloc.getAsset().getClass();
      debug(getName () + "---handleSuccessfulAlloc: task allocated to "+alloc.getAsset().getUID()+" "+ assetInfo);
    }
    AllocationResult AR = alloc.getReportedResult() == null ? alloc.getEstimatedResult() : alloc.getReportedResult(); 
    if (!AR.isSuccess ()) {
      if (isInfoEnabled())
	info(getName () + ".handleSuccessfulAlloc - WARNING : planning of leg for task " + alloc.getTask ().getUID () +
	     " failed, not continuing to next leg.");
      return;
    }
    double conf = 0;
    if (alloc.getReportedResult() != null)
      conf = alloc.getReportedResult().getConfidenceRating();
    
    if (isInfoEnabled ()) {
      info ("successful alloc " + alloc.getUID() + " task " + alloc.getTask().getUID () + 
	    " verb " + alloc.getTask().getVerb() +
	    " conf " + conf);
    }
		
    if ((alloc.getReportedResult() != null && alloc.getReportedResult().getConfidenceRating() >= UTILAllocate.HIGHEST_CONFIDENCE) ||
	alloc.getAsset() instanceof PhysicalAsset || alloc.getAsset() instanceof Deck) {
      // if (isDebugEnabled()) debug(getName () + "------non-null reported result with highest confidence");
      Task allocTask = alloc.getTask();
      String uid = allocTask.getUID().toString();
	    
      if (isDebugEnabled()) {
	debug(getName () + ".handleSuccessfulAlloc - considering finishing planning for allocation's task "+uid);
      }

      SequentialScheduleElement sse = null;
      Task parenttask = getParentTask(allocTask, uid);
      if (parenttask == null) {
	if (isInfoEnabled()) {
	  info(getName () + ".handleSuccessfulAlloc - no parent of task " + uid + 
	       " - must be during rescinds.  Skipping seq. planning."); 
	}
      }
      else {
	sse = getElement (allocTask, parenttask, uid);
      }

      if (sse == null) {
	if (parenttask != null) {
	  if (isInfoEnabled()) {	
	    info(getName () + ".handleSuccessfulAlloc - could not find seq. schedule element for task "+ uid);
	  }
	}
	else if (isInfoEnabled()) {
	  info(getName () + ".handleSuccessfulAlloc - no parent task of task " + uid + 
	       " must be during rescinds.  Skipping seq. planning."); 
	}
      } else if (!sse.isPlanned()) {
	if (isInfoEnabled()) {
	  info(getName () + ".handleSuccessfulAlloc - finishing planning of element " + sse + 
	       " b/c task completed : " + uid);
	}
	sse.finishPlan(alloc, this);

	//Task parenttask = getParentTask(t, uid);
	if (parenttask == null) {
	  if (isInfoEnabled()) {
	    info(getName () + ".handleSuccessfulAlloc - no parent task of task " + uid + 
		 " - must be during rescinds.  Skipping seq. planning."); 
	  }
	}
	else {
	  // this subtask is done, check to see if another subtask that depended on it can now be planned
	  turnCrank(parenttask); 
	}
      }
      else {

	long returnedStart = (long) AR.getValue(AspectType.START_TIME);

	if (isInfoEnabled ()) {
	  info ("planned alloc " + alloc.getUID() + " task " + uid + " compare sse start " +
		sse.getStartDate() + " vs AR start " + new Date (returnedStart));
	}

	// did the reported time get earlier?
	if (tryToReplanOverlaps) {
	  if (returnedStart < sse.getStartDate ().getTime ()) {
	    if (isInfoEnabled()) {
	      info(getName () + ".handleSuccessfulAlloc - resetting start and end times of the element " + sse + 
		   " b/c alloc results changed for task " + uid);
	    }
	    sse.finishPlan(alloc, this);

	    // find tasks that depended on this one and replan them
	    replanDependingTasks (parenttask, returnedStart);
	  }
	}
      }
    }

    if (isDebugEnabled()) {
      debug(getName () + "---handleSuccessfulAlloc: E of processing for " + alloc.getAsset().getUID()); 		
    }
  }

  /**
   * re-examine the workflow to see if the any tasks overlap the time
   * of the recently changed allocation, if they do, replan tasks
   * that depend on it.
   */
  protected void replanDependingTasks (Task parentTask, long beforeTime) {}

  /** 
   * Gets the parent task for the child task with the UID uid.
   * Uses a map to look up parents of child - if no key in the map,
   * does a blackboard query (CPU expensive!).
   *
   * deals with post-rehydration state 
   */
  public Task getParentTask (Task child, String uid) {
    Task parenttask = (Task) childToParentUID.get(uid);

    if (parenttask != null)
      return parenttask;

    // we rehydrated, so map is empty...

    UID parentUID = child.getParentTaskUID ();
	
    Collection stuff = 
      blackboard.query (new TaskCatcher (parentUID));

    if (stuff.isEmpty ())
      return null;
	
    parenttask = (Task) stuff.iterator().next ();
    childToParentUID.put(child.getUID().toString(), parenttask);

    return parenttask;
  }
 
  /** 
   * Finds the schedule element for the <code>child</code> task in the taskToSSE map.
   * If the map is empty (after rehydration) we look up the schedule element in the 
   * parent task's schedule.
   *
   * If we were to attach the schedule element to the child task (which would be 
   * the easier approach) we would drag the dependencies and all the tasks hanging
   * off them into the downstream agent, which sucks from a memory point-of-view.
   *
   * Deals with post-rehydration state by looking at parent task.
   * @param child task to look for matching SequentialScheduleElement
   * @param parent of child task (where we look for the schedule elements)
   * @param uid of child task
   * @return SequentialScheduleElement that has the planning info for <code>child</code>
   */
  protected SequentialScheduleElement getElement(Task child, Task parent, String uid) {
    if (isDebugEnabled()) {
      debug (getName ()+ ".getElement - getting schedule for " + uid + " parent " + parent.getUID());
    }

    SequentialScheduleElement sse = (SequentialScheduleElement)taskToSSE.get(uid);

    if (sse != null) {
      return sse;
    }
    else {
      if (isInfoEnabled()) {
	info (getName() + ".getElement - no schedule element for " + uid + 
	      " in hash with " + taskToSSE.size() + " elements.");
      }
    }

    // in case of rehydration, re-enter mapping into hash map

    PrepositionalPhrase prep = prepHelper.getPrepNamed(parent, GLMTransConst.SequentialSchedule);
    
    if (prep == null) {
      error(getName () + ".getElement - no prep named " + GLMTransConst.SequentialSchedule +
	    " on task " + parent);
      return null;
    }

    Schedule sched = (Schedule) prep.getIndirectObject();

    for (Enumeration enum = sched.getAllScheduleElements(); enum.hasMoreElements();) {
      SequentialScheduleElement spe = (SequentialScheduleElement)enum.nextElement();
      if (spe.getTask () == child) {
	if (isInfoEnabled()) {
	  info (getName () + ".getElement - found schedule for " + spe.getTask().getUID());
	}

	sse = spe;
      }
      else {
	if (isInfoEnabled() && (spe != null) && (spe.getTask() != null) && (child != null)) {
	  info (getName () + ".getElement - schedule task " + spe.getTask().getUID () + " != examined task " + child.getUID());
	}
      }
    }

    if (sse == null) {
      if (isInfoEnabled ()) {
	info (getName () + ".getElement - no schedule element for " + child.getUID());
      }
    }
    else 
      enterHash(child.getUID().toString(), sse);

    return sse;
  }

  /** 
   * for post-rehydration phase 
   * @see #getParentTask
   */
  class TaskCatcher implements UnaryPredicate {
    UID parentUID;
	
    public TaskCatcher (UID parentUID) {	  this.parentUID = parentUID;	}
	  
    public boolean execute (Object obj) {
      boolean isTask = (obj instanceof Task);
      if (!isTask) return false;
      return ((Task)obj).getUID().equals (parentUID);
    }
  }
  
  /*** Set of dumb functions to get around incredibly annoying Java Compiler bugs ***/
  // The following just allow protected plugin stuff to be called from the custom schedude
  // elements. It should probably be replaced with a delegate.
  public PlanningFactory publicGetFactory() {
    return ldmf;
  }
  public Plan publicGetRealityPlan() {
    return realityPlan;
  }
  public void publicPublishChange(Object o) {
    if (isDebugEnabled()) debug(getName () + " - publicPublishChange on : " + (UniqueObject) o );
    publishChange(o);
  }
  public void publicPublishAdd(Object o) {
    if (isDebugEnabled()) debug(getName () + " - publicPublishAdd of : " + (UniqueObject) o );
    publishAdd(o);
  }
  public MessageAddress publicGetMessageAddress() {
    return getAgentIdentifier();
  }
  public String publicGetMyClusterName() {
    return myClusterName;
  }

  /*** connect SSE to task so that you can do SSE processing when task returns ***/
  public void enterHash(String key, SequentialScheduleElement obj) {
    taskToSSE.put(key,obj);
  }

  public Map getChildToParentUID() { return childToParentUID; }
}
