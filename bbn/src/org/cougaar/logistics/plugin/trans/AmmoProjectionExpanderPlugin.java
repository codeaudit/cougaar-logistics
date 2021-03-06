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

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
import org.cougaar.glm.ldm.asset.*;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILFilterCallbackAdapter;
import org.cougaar.lib.callback.UTILGenericListener;
import org.cougaar.lib.util.UTILAllocate;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.measure.Area;
import org.cougaar.planning.ldm.measure.Mass;
import org.cougaar.planning.ldm.measure.Volume;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.UnaryPredicate;

import java.util.*;

/**
 * Takes PROJECT_SUPPLY tasks 
 * and converts them into TRANSPORT tasks with a RESERVATION prep. <p>
 *
 */
public class AmmoProjectionExpanderPlugin extends AmmoLowFidelityExpanderPlugin {
  public long CHUNK_DAYS = 30;
  protected boolean FIND_FOR_UNIT_PREP_ON_TASK = false;
  public static long MILLIS_PER_DAY = 1000*60*60*24;
  public static double SECS_PER_DAY = 60.0d*60.0d*24.0d;
  public static final String AMMO_CATEGORY_CODE = "MBB";
  public static final String MILVAN_NSN = "NSN/8115001682275";
  public static final double PACKING_LIMIT = 13.9; /* short tons */
  public static final long TASK_TRANSMISSION_DELAY = 1000*60*60;
  private static Asset MILVAN_PROTOTYPE = null;
  public static String START = "Start";
  protected Map childToParent = new HashMap ();
  protected Map uidToTask   = new HashMap ();
  protected Map typeToTasks   = new HashMap ();
  protected UTILFilterCallback myTaskCallback;
  protected List defaultUnit;
  public TaskUtils taskUtils;

  /**
   * rely upon load-time introspection to set these services - 
   * don't worry about revokation.
   */
  public void setLoggingService(LoggingService bs) {
    super.setLoggingService(bs);
    taskUtils = new TaskUtils(logger);
  }


  public void localSetup () {
    super.localSetup ();

    try {
      if (getMyParams ().hasParam ("CHUNK_DAYS"))
        CHUNK_DAYS=getMyParams().getLongParam ("CHUNK_DAYS");
      if (getMyParams ().hasParam ("FIND_FOR_UNIT_PREP_ON_TASK"))
        FIND_FOR_UNIT_PREP_ON_TASK=getMyParams().getBooleanParam ("FIND_FOR_UNIT_PREP_ON_TASK");
    } catch (Exception e) { if (isWarnEnabled()) { warn ("got unexpected exception " + e); } }
  }

  /**
   * Uses a special callback that maintains two maps :
   *
   * a uidToTask map so we can do parent task lookup efficiently AND
   * a typeToTasks map so we can look up a list of tasks by what ammo type they are
   *
   * @see #getMatchingTasksByType
   */
  public void setupFilters () {
    super.setupFilters ();

    addFilter (myTaskCallback = new UTILFilterCallbackAdapter (this, logger) {
	protected UnaryPredicate getPredicate () {
	  return new UnaryPredicate() {
	      public boolean execute(Object o) {
		return ( o instanceof Task );
	      }
	    };
	}
	public void reactToChangedFilter () {
	  Enumeration addedtasks = getSubscription().getAddedList();
	  while (addedtasks.hasMoreElements()) {
	    Task task = (Task) addedtasks.nextElement();

	    if (!(task instanceof Task))
	      error ("huh? " + task + " is not an MPTask??");

	    if (isInfoEnabled ()) {
	      info ("uidToTask - mapping task " + task.getUID());
	    }

	    uidToTask.put (task.getUID (), task);

	    // keep a ammo type to tasks map so we can later compare
	    // an actual task with a reserved or vice versa by just examining 
	    // tasks with the same ammo type
	    // see #getMatchingTasksByType

	    if (task.getVerb ().equals (Constants.Verb.TRANSPORT)) {
	      Collection types = getTypesInContainer (task);

	      for (Iterator iter = types.iterator(); iter.hasNext(); ) {
		String type = (String) iter.next();
		List tasksForType = (List) typeToTasks.get(type);
		if (tasksForType == null) {
		  typeToTasks.put (type, tasksForType = new ArrayList());
		  if (isInfoEnabled()) {
		    info ("adding type " + type + " to map.");
		  }
		}
		tasksForType.add (task);
	      }
	    }
	  }

	  if (getSubscription().getRemovedList().hasMoreElements ()) {
	    Set uniqueTasks = new HashSet ();
	    Enumeration removedtasks = getSubscription().getRemovedList();
	    while (removedtasks.hasMoreElements()) {
	      Task task = (Task) removedtasks.nextElement();

	      if (uniqueTasks.contains (task)) { // skip duplicate entries in removed list, if any
		if (isWarnEnabled ()) { warn ("duplicate entries in removed list, skipping " + task.getUID()); }
		continue;
	      }
	      else {
		uniqueTasks.add (task);
	      }

	      if (uidToTask.remove (task.getUID()) == null) {
		if (isWarnEnabled ()) { warn ("no task in uidToTask map with uid " + task.getUID ());	}
	      }
	      else {
		if (isInfoEnabled ()) { info ("uidToTask - removing task uid " + task.getUID() + " from map."); }
	      }

	      // keep a ammo type to tasks map so we can later compare
	      // an actual task with a reserved or vice versa by just examining 
	      // tasks with the same ammo type
	      // see #getMatchingTasksByType
	      if (task.getVerb ().equals (Constants.Verb.TRANSPORT)) {
		Collection types = getTypesInContainer (task);

		for (Iterator iter = types.iterator(); iter.hasNext(); ) {
		  String type = (String) iter.next();
		  List tasksForType = (List) typeToTasks.get(type);
		  if (tasksForType != null) { // might have been removed already
		    if (!tasksForType.remove (task)) {
		      if (isWarnEnabled()) { warn ("no task " + task.getUID() + " in typeToTasks."); }
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
	       );
  }

  protected UTILFilterCallback createThreadCallback (UTILGenericListener bufferingThread) {
    if (isInfoEnabled())
      info (getName () + " : Filtering for Expandable Tasks...");

    myInputTaskCallback = new UTILExpandableTaskCallback (bufferingThread, logger) {
      protected UnaryPredicate getPredicate () {
        return new UnaryPredicate() {
          protected TaskUtils myTaskUtils = new TaskUtils(logger);

          public boolean execute(Object o) {
            if ( o instanceof Task ) {
              Task task = (Task) o;
              boolean hasTransport = task.getVerb().equals (Constants.Verb.TRANSPORT);
              boolean isReserved   = (task.getPrepositionalPhrase (START) != null);
              if (hasTransport && isReserved) return true;

              if(// (task.getPlanElement() == null ) &&  ** Now dealing with changed tasks **
                  (task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) &&
                  (myTaskUtils.isLevel2(task))) {
                boolean isReadyForTransport=(myTaskUtils.isReadyForTransport(task));
                //TODO: MWD delete this debug line
                if((isReadyForTransport) &&
                    logger.isDebugEnabled()){
                  logger.debug("AmmoProjectionExpanderPlugin:isLevel2 ReadyForTransport=" + isReadyForTransport);
                }
                return isReadyForTransport;
              }

              return (// (task.getWorkflow() == null )  &&   ** Now dealing with changed tasks **
                      // (task.getPlanElement() == null ) &&
                  ((UTILGenericListener) myListener).interestingTask (task));
            }
            return false;
          }
        };
      }
    };

    return myInputTaskCallback;
  }

  /**
   * State that we are interested in all transport tasks
   * @param task the task to test.
   * @return true if the tasks verb is SUPPLY, false otherwise
   */
  public boolean interestingTask(Task task){
    boolean hasSupply = task.getVerb().equals (Constants.Verb.PROJECTSUPPLY);
    boolean hasTransport = task.getVerb().equals (Constants.Verb.TRANSPORT);

    if (isDebugEnabled() && hasSupply)
      debug (".interestingTask - processing PROJECT_SUPPLY task " + task.getUID ());
    if (isDebugEnabled() && hasTransport)
      debug (".interestingTask - processing TRANSPORT task " + task.getUID ());

    return (hasSupply || hasTransport);
  }

  int total = 0;

  /**
   * Implemented for UTILExpanderPlugin interface
   *
   * Break up project supply tasks into reserved transport tasks for 
   * discrete periods of time.  Takes the (demand rate X period) of the project
   * supply task and breaks this up total ammo up across several subtasks, one
   * per month.
   *
   * Called from handleTask, which is called from HandleProjectSupplyTask
   * @see #handleProjectSupplyTask
   * @return transport subtasks for the parent project supply
   */
  public Vector getSubtasks(Task parentTask) {
    if (!(parentTask.getVerb ().equals (Constants.Verb.PROJECTSUPPLY)))
      error ("Expecting a project supply task, got " + parentTask);

    Vector childTasks = new Vector ();

    // create the d.o.

    Asset supplyAsset = parentTask.getDirectObject();

    Preference pref = prefHelper.getPrefWithAspectType (parentTask, AlpineAspectType.DEMANDRATE);
    double ratePerSec = prefHelper.getPreferenceBestValue (pref); // base number is in tons per second!
    double ratePerDay = ratePerSec*SECS_PER_DAY;

    Date readyAt = prefHelper.getReadyAt   (parentTask);
    Date early   = getEarlyDate (parentTask);
    Date best    = prefHelper.getBestDate  (parentTask);
    Date late    = getLateDate  (parentTask);

    if (early.getTime () < readyAt.getTime ())
      early = readyAt;
    if (best.getTime () < readyAt.getTime ())
      best = readyAt;

    long window = best.getTime () - readyAt.getTime();
    long originalWindowInDays = window/MILLIS_PER_DAY;
    long numSubtasks = window/(CHUNK_DAYS*MILLIS_PER_DAY);
    if (window - (numSubtasks * CHUNK_DAYS*MILLIS_PER_DAY) != 0)
      numSubtasks++;

    if (numSubtasks < 1) {
      error (getName () + ".getSubtasks - task " + parentTask.getUID () +
             " will create no subtasks?  Window was " + originalWindowInDays + " days");
    }

    if (isInfoEnabled ()) {
      info (getName () + ".getSubtasks - task " + parentTask.getUID () +
            " from " + readyAt +
            " to " + best +
            " will produce " + numSubtasks + " subtasks.");
    }

    String unit;
    if (FIND_FOR_UNIT_PREP_ON_TASK) {
      unit = (prepHelper.hasPrepNamed (parentTask, Constants.Preposition.FOR)) ?
        (String) prepHelper.getIndirectObject (parentTask, Constants.Preposition.FOR) : "null";
    } 
    else {
      unit = getClusterName ();
    }

    // create one subtask for every chunk set of days, with an asset that is the total
    // delivered over the period = days*ratePerDay
    long daysSoFar = 0;
    double totalQuantity = 0;
    double targetQuantity = ((double) (window/1000l))*ratePerSec;
    Date lastBestDate = readyAt;

    if (isInfoEnabled ()) {
      info (getName () + ".getSubtasks - task " + parentTask.getUID () + " target quantity " + targetQuantity +
            " windowInSec " + window/1000l + " rate/sec " + ratePerSec);
    }

    for (int i = 0; i < (int) numSubtasks; i++) {
      boolean onLastTask = (window/MILLIS_PER_DAY) < CHUNK_DAYS;
      long daysToChunk = (onLastTask) ? window/MILLIS_PER_DAY : CHUNK_DAYS;

      if (isInfoEnabled () && onLastTask)
        info ("on last task - days " + daysToChunk + " since " + window/MILLIS_PER_DAY + " < " + CHUNK_DAYS);

      daysSoFar += daysToChunk;
      window    -= daysToChunk*MILLIS_PER_DAY;
      double quantity = ((double)daysToChunk) * ratePerDay;
      if (onLastTask && ((totalQuantity + quantity) != targetQuantity)) {
        if (isInfoEnabled ())
          info (" task " + parentTask.getUID () +
                " adjusting quantity from " +quantity +
                " to " + (targetQuantity - totalQuantity) +
                " total is " + totalQuantity);
        quantity = targetQuantity - totalQuantity;
      } else if (isInfoEnabled ()) {
        info (".getSubtasks - task " + parentTask.getUID () + " quantity is " + quantity +
              " chunk days " + daysToChunk+ " rate " + ratePerDay);
      }

      if (quantity < 0.00001) {
        if (isInfoEnabled()) {
          info (".getSubtasks - task " + parentTask.getUID () +
                " gets a quantity of zero, ratePerDay was " + ratePerDay +
                " chunk days " +daysToChunk);
        }
      }

      //      double massInKGs = ((GLMAsset)supplyAsset).getPhysicalPG().getMass().getKilograms()*quantity;
      double massInSTons = quantity;
      totalQuantity += quantity;

      //  CHANGE to JUST Grab the d.o.?

      // set item id pg to show it's a reservation, and not a normal task's asset

      ItemIdentificationPG itemIDPG = (ItemIdentificationPG) supplyAsset.getItemIdentificationPG();
      String itemID = itemIDPG.getItemIdentification();
      String itemNomen = itemIDPG.getNomenclature ();

      if (itemID == null) {
        TypeIdentificationPG typeID = supplyAsset.getTypeIdentificationPG ();
        itemID = typeID.getTypeIdentification();
      }

      if (itemNomen == null) {
        TypeIdentificationPG typeID = supplyAsset.getTypeIdentificationPG ();
        itemNomen = typeID.getNomenclature();
      }

      Asset directObject =
          getMilvanDirectObject (itemNomen, itemID, unit, massInSTons);

      long bestTime = getBestTime (early, best, daysSoFar);

      Task subTask = createSubtask (parentTask, directObject,
				    early, best, late,
				    bestTime,
				    daysSoFar,
				    lastBestDate,
				    itemNomen, 
				    massInSTons);

      lastBestDate = new Date (bestTime);
      childTasks.addElement (subTask);
    }

    // post condition
    if (totalQuantity > targetQuantity + 0.001 ||
	totalQuantity < targetQuantity - 0.001) {
      if (isWarnEnabled ()) {
        warn (getName () + " total quantity " + totalQuantity +
              " != original total " + targetQuantity +
              " = window " + originalWindowInDays +
              " * ratePerDay " + ratePerDay);
      }
    }

    if (isInfoEnabled())
      info (getName () + " returning " + childTasks.size() + " subtasks for " + parentTask.getUID());

    return childTasks;
  }

  /** 
   * make sure best time is before or equal to best time on task 
   */
  protected long getBestTime (Date early, Date best, long daysSoFar) {
    long bestTime = early.getTime() + ((long)daysSoFar)*MILLIS_PER_DAY;
    if (bestTime > best.getTime()) {
      if (isInfoEnabled()) {
	info (getName () +
	      ".getSubtasks - had to correct bestTime, was " + new Date (bestTime) +
	      " now " + best);
      }

      bestTime = best.getTime();
    }

    return bestTime;
  }

  /**
   * Create the transport reservation subtask of the parent.
   */
  protected Task createSubtask (Task parentTask, Asset directObject, 
				Date early, Date best, Date late, 
				long bestTime,
				long daysSoFar,
				Date lastBestDate,
				String itemNomen,
				double massInSTons) {
    Task subTask = makeTask (parentTask, directObject);//deliveredAsset);
    Date startDate = new Date(getAlarmService().currentTimeMillis () +
			      TASK_TRANSMISSION_DELAY);
    if (isInfoEnabled ())
      info (getName () + ".getSubtasks - making task " + subTask.getUID() +
	    " with best arrival " + new Date(bestTime) + " and start " + startDate);
    
    // prevent earliest arrival being before start date
    if (early.getTime() < startDate.getTime()) {
      early = startDate;
    }    
    
    prefHelper.replacePreference((NewTask)subTask,
				 prefHelper.makeEndDatePreference (ldmf,
								   early,
								   new Date (bestTime),
								   late));

    prefHelper.replacePreference((NewTask)subTask,
				 prefHelper.makeStartDatePreference (ldmf, startDate));

    prefHelper.removePrefWithAspectType (subTask, AlpineAspectType.DEMANDRATE); // we've included it in the d.o.

    prepHelper.removePrepNamed (subTask, Constants.Preposition.MAINTAINING);
    prepHelper.removePrepNamed (subTask, Constants.Preposition.REFILL);
    prepHelper.addPrepToTask (subTask, prepHelper.makePrepositionalPhrase (ldmf, "Start", lastBestDate));

    if (!FIND_FOR_UNIT_PREP_ON_TASK) {
      prepHelper.replacePrepOnTask (subTask, 
				    prepHelper.makePrepositionalPhrase (ldmf, Constants.Preposition.FOR, 
									getClusterName()));
    }

    if (isInfoEnabled()) {
      info (getName () + " publishing reservation " + subTask.getUID() +
	    " for " + itemNomen +
	    " from " + lastBestDate + " to " + new Date(bestTime) + " weight " +
	    massInSTons + " short tons.");
    }

    return subTask;
  }

  private static final double MAX_IN_MILVAN = 13.9;

  protected Asset getMilvanDirectObject (String itemNomen, String itemID,
                                         String unit, double massInSTons) {
    int numMilvans = (int) Math.ceil(massInSTons/MAX_IN_MILVAN);
    if (numMilvans == 0) {
      numMilvans++;
      if (isWarnEnabled())
        warn ("Got mass that was zero : " + massInSTons);
      massInSTons += 0.1;
    }
    double tonsLeft = massInSTons;

    if (numMilvans == 1) {
      GLMAsset milvan = makeMilvan ();
      addContentsInfo (milvan, itemNomen, itemID, unit, massInSTons);
      return milvan;
    }
    else {
      Vector milvans = new Vector();
      for (int i = 0; i < numMilvans; i++) {
        GLMAsset milvan = makeMilvan ();
        double amount = Math.min (tonsLeft, MAX_IN_MILVAN);
        addContentsInfo (milvan, itemNomen, itemID, unit, amount);
        tonsLeft -= amount;
        milvans.add (milvan);
      }
      if ((tonsLeft > 0.0000001) || (tonsLeft < -0.0000001)) {
        error ("Tons left is not zero = " + tonsLeft);
      }
      return assetHelper.makeAssetGroup(getLDMService().getLDM().getFactory(),
                                        milvans);
    }
  }

  public Date getEarlyDate(Task t) {
    Preference endDatePref = prefHelper.getPrefWithAspectType(t, AspectType.END_TIME);
    AspectScoreRange range = endDatePref.getScoringFunction().getDefinedRange();
    return new Date (((AspectScorePoint) range.getRangeStartPoint ()).getAspectValue ().longValue ());
  }
  public Date getLateDate(Task t) {
    Preference endDatePref = prefHelper.getPrefWithAspectType(t, AspectType.END_TIME);
    AspectScoreRange range = endDatePref.getScoringFunction().getDefinedRange();
    return new Date (((AspectScorePoint) range.getRangeEndPoint ()).getAspectValue ().longValue ());
  }

  protected Enumeration getValidEndDateRanges (Preference endDatePref) {
    Calendar cal = java.util.Calendar.getInstance();
    cal.set(2200, 0, 0, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date endOfRange = (Date) cal.getTime();

    Enumeration validRanges =
        endDatePref.getScoringFunction().getValidRanges (TimeAspectValue.create (AspectType.END_TIME,
                                                                                 0l),
                                                         TimeAspectValue.create (AspectType.END_TIME,
                                                                                 endOfRange));
    return validRanges;
  }

  /** create aggregate asset aggregating the direct object's prototype **/
  protected AggregateAsset createDeliveredAsset (Task originalTask, Asset originalAsset, int quantity) {
    Asset prototype = originalAsset.getPrototype ();

    if (prototype == null) {
      prototype = originalAsset;
      GLMAsset glmProto = (GLMAsset)prototype;
      if (!glmProto.hasPhysicalPG()) {
        warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + " - " +
              prototype + " doesn't have a physical PG - " + glmProto.getPhysicalPG());
        if (!(prototype instanceof PhysicalAsset))
          error ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " +
                 prototype + " is not a physical asset?.");
        else if (isInfoEnabled()){
          info ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() +
                " is a physical asset.");
        }
      }
      else {
        if (glmProto.getPhysicalPG().getFootprintArea() == null) {
          ((NewPhysicalPG)glmProto.getPhysicalPG()).setFootprintArea (new Area (Area.SQUARE_FEET, 1));
          if (isWarnEnabled()) {
            warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() +
                  " doesn't have an area slot on its physical pg.");
          }
        }
        if (glmProto.getPhysicalPG().getVolume() == null) {
          ((NewPhysicalPG)glmProto.getPhysicalPG()).setVolume (new Volume (Volume.CUBIC_FEET, 1));
          if (isWarnEnabled()) {
            warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() +
                  " doesn't have a volume slot on its physical pg.");
          }
        }
      }
      if (!glmProto.hasPackagePG()) {
        warn ("createDeliveredAsset - task " + originalTask.getUID () + "'s d.o. " + prototype.getUID() + " - " +
              prototype + " doesn't have a package PG.");
      }
    }

    AggregateAsset deliveredAsset = (AggregateAsset) ldmf.createAggregate(prototype, quantity);

    return deliveredAsset;
  }

  /**
   * <pre>
   * Report to superior that the expansion has changed. 
   * 
   * An override is needed here for two reasons :
   *
   * 1) to add the DEMANDRATE preference value into the allocation result, 
   *    since downstream plugins won't set this aspect.
   * 2) echo the start time preference in the allocation result (why?)
   *
   * No allocation results flow upward unless reported confidence reaches 100%.
   *
   * </pre>
   * @param cpe Expansion that has changed.
   * @see org.cougaar.lib.filter.UTILPluginAdapter#updateAllocationResult
   */
  public void reportChangedExpansion(Expansion cpe) { 
    if (isDebugEnabled ())
      debug (getName () + " : Received changed pe " +
             cpe.getUID () + " for task " +
             cpe.getTask ().getUID());
    AllocationResult reportedresult = cpe.getReportedResult();
    if (reportedresult != null) {
      // compare entire allocationresults.
      AllocationResult estimatedresult = cpe.getEstimatedResult();
      double confidence = reportedresult.getConfidenceRating ();
      boolean nullEstimated  = (estimatedresult == null);
      // if we are not ignoring low confidence reported values
      boolean highConfidence = (!skipLowConfidence || confidence > HIGH_CONFIDENCE);

      if ( nullEstimated  ||
          (highConfidence &&
          (! isEqual(estimatedresult, reportedresult)))) {
        if (isDebugEnabled ())
          debug (getName () + " : Swapping Alloc Results for task " +
                 cpe.getTask ().getUID ());
        if (isWarnEnabled() && !reportedresult.isSuccess ())
          warn (getName () + " : " +
                cpe.getTask ().getUID () + " failed to allocate.");

        cpe.setEstimatedResult(reportedresult);

        //        double prefValue = getScaledRate (cpe);
        Task task = cpe.getTask();
        AspectValue prefRate =
            task.getPreference(AlpineAspectType.DEMANDRATE).getScoringFunction().getBest().getAspectValue();

        AspectValue[] aspectValues = cpe.getEstimatedResult().getAspectValueResults();

        AspectValue [] copy = new AspectValue [aspectValues.length+1];
        System.arraycopy (aspectValues, 0, copy, 0, aspectValues.length);
        copy[aspectValues.length] = prefRate;

        // fix start time to echo start time preference
        for (int i = 0; i < copy.length; i++) {
          AspectValue value = copy[i];
          if (value.getAspectType () == AspectType.START_TIME) {
            Date preferredStart = prefHelper.getReadyAt (task);
            copy[i] = AspectValue.newAspectValue (AspectType.START_TIME, preferredStart);
            break;
          }
        }

        AllocationResult correctedAR =
            new AllocationResult(reportedresult.getConfidenceRating(),
                                 reportedresult.isSuccess(),
                                 copy);

        cpe.setEstimatedResult(correctedAR);

        if (isInfoEnabled())
          info (getName () + " : publish changing task " + cpe.getTask ().getUID ());

        blackboard.publishChange(cpe);
      }
    }
    else if (!cpe.getTask().getSource().equals(getAgentIdentifier())) {
      error ("ERROR! " + getName() +
             " : "     + cpe.getTask().getUID() +
             " has a null reported allocation.");
    }
  }

  /**
   * <pre>
   * scale the rate to make it the rate over the time of the performance of the 
   * task, so that when the inventory does days*rate, they come up with the original
   * requested quantity.
   *
   * The problem is that we're translating from one meaning of start and end date to another.
   * The start->end date window on a project supply task means "I need X widgets per day, each
   * day, over this period."  The transportation start->end window means "The move started on this
   * day and ended on this other day."  So they mean different things.
   * </pre>
   */
  protected double getScaledRate (PlanElement planElement) {
    Task task = planElement.getTask();
    double prefRate =
        task.getPreference(AlpineAspectType.DEMANDRATE).getScoringFunction().getBest().getAspectValue().getValue();

    Date preferredStart = prefHelper.getReadyAt (task);
    Date preferredEnd   = prefHelper.getBestDate(task);

    Date reportedStart  = prefHelper.getReportedReadyAt (planElement);
    Date reportedEnd    = prefHelper.getReportedEndDate (planElement);

    long preferredWindow = preferredEnd.getTime () - preferredStart.getTime();
    long reportedWindow  = reportedEnd.getTime  () - reportedStart.getTime();

    double ratio = (double)preferredWindow/(double)reportedWindow;

    return prefRate*ratio;
  }

  /** 
   * checks to see if the AllocationResult is equal to this one.
   *
   * We have to use this copy of the AllocationResult.isEqual because we want to
   * to skip equals comparison of DEMANDRATE and START_TIME aspects. (why?)
   *
   * Mainly we need our own version of AspectValue.nearlyEquals
   *
   * @param thisAR
   * @param that
   * @return boolean
   */
  public boolean isEqual(AllocationResult thisAR, AllocationResult that) {
    if (thisAR == that) return true; // quick success
    if (that == null) return false; // quick fail
    if (!(thisAR.isSuccess() == that.isSuccess() &&
        thisAR.isPhased() == that.isPhased() &&
        thisAR.getConfidenceRating() == that.getConfidenceRating())) {
      if (isInfoEnabled())
        info ("AspectValues - success/phased/confidence this AR " + thisAR + " != " + that);
      return false;
    }

    //check the real stuff now!
    //check the aspect types
    //check the summary results
    synchronized (thisAR.getAspectValueResults()) {
      if (!nearlyEquals(thisAR.getAspectValueResults(), that.getAspectValueResults())) {
        if (isDebugEnabled())
          debug ("AspectValues - this AR " + thisAR + " != " + that);
        return false;
      }
      // check the phased results
      if (thisAR.isPhased()) {
        Iterator i1 = that.getPhasedAspectValueResults().iterator();
        Iterator i2 = thisAR.getPhasedAspectValueResults().iterator();
        while (i1.hasNext()) {
          if (!i2.hasNext()) return false;
          if (!nearlyEquals((AspectValue[]) i1.next(), (AspectValue[]) i2.next())) {
            if (isDebugEnabled())
              debug ("phased AspectValues - this AR " + thisAR + " != " + that);
            return false;
          }
        }
        if (i2.hasNext()) return false;
      }
    }

    // check the aux queries

    // AUX QUERIES are not used in this plugin - honestly, where are they used?

    /*
    String[] taux = that.auxqueries;
    if (auxqueries != taux) {
    if (!Arrays.equals(taux, auxqueries)) return false;
    }
    */

    // must be equals...
    return true;
  }

  /** 
   * checks to see that two aspect value arrays are equal
   *
   * skips comparison of DEMANDRATE and START_TIME aspects.
   *
   * We have to skip start time, since it doesn't mean the same in the inventory world
   * as in the transportation world.
   *
   * @param avs1 first set
   * @param avs2 second set
   * @return true if the same
   */
  public boolean nearlyEquals(AspectValue[] avs1, AspectValue[] avs2) {
    int len = avs1.length;
    // if (len != avs2.length) return false; // Can't be equal if different length
    outer:
    for (int i = 0; i < len; i++) {
      AspectValue av1 = avs1[i];
      int type1 = av1.getAspectType();
      if (type1 == AlpineAspectType.DEMANDRATE)
        continue; // ignore DEMAND RATE!
      if (type1 == AspectType.START_TIME)
        continue; // ignore Start time, since it doesn't mean the same in the inventory world
      inner:
      for (int j = 0; j < len; j++) {
        int k = (i + j) % len;
        AspectValue av2 = avs2[k];
        int type2 = av2.getAspectType();
        if (type1 == type2) {
          if (av1.nearlyEquals(av2)) continue outer;
          break inner;
        }
      }
      return false;             // Found no match
    }
    return true;                // Found a match for every aspect
  }

  /**
   * An ancillary method that creates an asset that represents a MILVAN 
   * (military container) carrying ammunition
   */
  protected GLMAsset makeMilvan() {

    if (MILVAN_PROTOTYPE == null) {
      MILVAN_PROTOTYPE = getLDMService().getLDM().getPrototype(MILVAN_NSN);

      if (MILVAN_PROTOTYPE == null) {
        error("AmmoTransport: Error! Unable to get prototype for" +
              " milvan NSN -" + MILVAN_NSN);
        return null;
      }
    }

    String itemID = makeMilvanID();
    Container milvan =
        (Container)getLDMService().getLDM().getFactory().createInstance(MILVAN_PROTOTYPE, itemID);

    // AMMO Cargo Code
    NewMovabilityPG movabilityPG =
        PropertyGroupFactory.newMovabilityPG(milvan.getMovabilityPG());
    movabilityPG.setCargoCategoryCode(AMMO_CATEGORY_CODE);
    milvan.setMovabilityPG(movabilityPG);

    // Milvan Contents
    NewContentsPG contentsPG =
        PropertyGroupFactory.newContentsPG();
    milvan.setContentsPG(contentsPG);

    // Unique Item Identification
    NewItemIdentificationPG itemIdentificationPG =
        PropertyGroupFactory.newItemIdentificationPG ();

    //    String itemID = makeMilvanID();
    itemIdentificationPG.setItemIdentification(itemID); // redundant?
    itemIdentificationPG.setNomenclature("Milvan");
    itemIdentificationPG.setAlternateItemIdentification(itemID);
    milvan.setItemIdentificationPG(itemIdentificationPG);

    return milvan;
  }

  protected String makeMilvanID() {
    return new String("Reserved_" + getCounter());
  }

  private static int COUNTER = 0;

  private static synchronized long getCounter() {
    return COUNTER++;
  }

  protected void addContentsInfo(GLMAsset container, String nomen, String typeID, String unit, double massInSTons) {
    List typeIDs = new ArrayList();
    List nomenclatures = new ArrayList();
    List weights = new ArrayList();
    List receivers = new ArrayList();

    typeIDs.add(typeID);
    nomenclatures.add(nomen);

    Mass mass = Mass.newMass(massInSTons, Mass.SHORT_TONS);
    weights.add(mass);

    receivers.add(unit);

    // Contents
    NewContentsPG contentsPG =
        PropertyGroupFactory.newContentsPG();
    contentsPG.setNomenclatures(nomenclatures);
    contentsPG.setTypeIdentifications(typeIDs);
    contentsPG.setWeights(weights);
    contentsPG.setReceivers(receivers);
    container.setContentsPG(contentsPG);
  }

  /**
   * Implemented for UTILBufferingPlugin interface
   *
   * @param tasks that have been buffered up to this point
   * @see org.cougaar.lib.filter.UTILBufferingPlugin#processTasks
   */
  public void processTasks (List tasks) {
    if (isInfoEnabled()) {
      info (getName () +
            ".processTasks - processing " + tasks.size() + " tasks.");
    }

    Map reservedToActual = new HashMap ();
    for (int i = 0; i < tasks.size (); i++) {
      Task task = (Task) tasks.get (i);

      if (task.getVerb ().equals (Constants.Verb.TRANSPORT))
        handleTransportTask (task, reservedToActual);
      else
        handleProjectSupplyTask (task);
    }

    for (Iterator iter = reservedToActual.keySet().iterator(); iter.hasNext(); ) {
      Task reserved = (Task)iter.next ();
      Task actual   = (Task) reservedToActual.get(reserved);
      Task reservedParent = getParentTask (reserved);
      if (reservedParent == null) {
	if (isInfoEnabled()) {
          info ("skipping reserved task " + reserved.getUID () + " with no parent on blackboard.");
	}
      }
      else if (reserved.getWorkflow() == null) {
	if (isInfoEnabled()) {
          info ("skipping reserved task " + reserved.getUID () + " with no workflow.");
	}
      } else {
	synchronized (reserved.getWorkflow ()) {
	  if (ownWorkflow (reserved)) {
	    dealWithReservedTask (actual, reserved, reservedParent);
	  }
	  else if (isInfoEnabled()) {
	    info ("reserved task " + reserved.getUID () +
		  " not a member of it's own workflow " + reserved.getWorkflow () +
		  "\nworkflow task uids : " + uidsWorkflow(reserved) + " - assuming it will be removed.");
	  }
	}
      }
    }
  }

  /**
   * Handle a task with verb Supply
   */
  public void handleProjectSupplyTask(Task t) {
    wantConfidence = true;
    if (t.getPlanElement() != null) {
      publishRemove (t.getPlanElement());
    }
    handleTask(t); // expands t with subtasks from getSubtasks
    Preference pref = prefHelper.getPrefWithAspectType (t, AlpineAspectType.DEMANDRATE);
    AspectValue ratePerSec = pref.getScoringFunction().getBest().getAspectValue();
    if (isInfoEnabled ()) 
      info (getName () + ".handleTask - task " + t.getUID() + " had p.e. " + t.getPlanElement().getUID());
    if (t.getPlanElement () instanceof Expansion) {
      addToEstimatedAR (t.getPlanElement (), ratePerSec);
      if (isInfoEnabled()) {
        Workflow tasksWorkflow = t.getWorkflow();
        Workflow peWorkflow = ((Expansion)t.getPlanElement()).getWorkflow();
        info (getName () + ".handleTask " + t.getUID() + " in " + ((tasksWorkflow != null) ? tasksWorkflow.getUID().toString() : "null wf?")+
              " p.e. " +t.getPlanElement ().getUID() + " p.e. wf. " + ((peWorkflow != null) ? peWorkflow.getUID().toString() : " null p.e. wf?"));
      }
    } 
    else if (isWarnEnabled ()) 
      warn (getName () + ".handleTask - task " + t.getUID() + " had no p.e.???");
  }

  protected void addToEstimatedAR (PlanElement exp, AspectValue rate) {
    AllocationResult estAR = exp.getEstimatedResult ();
    AspectValue [] aspectValues = estAR.getAspectValueResults();
    AspectValue [] copy = new AspectValue [aspectValues.length+1];
    System.arraycopy (aspectValues, 0, copy, 0, aspectValues.length);
    copy[aspectValues.length] = rate;

    AllocationResult replacement =
        ldmf.newAllocationResult(UTILAllocate.MEDIUM_CONFIDENCE, true, copy);
    exp.setEstimatedResult (replacement);
  }

  /** 
   * Queries the uid->task map for the parent task of the child... 
   * must do this since child task has a uid reference to the parent task not
   * an actual reference.
   */
  protected Task getParentTask (final Task child) {
    Task parent = (Task) uidToTask.get (child.getParentTaskUID ());

    if (parent == null) {
      logger.warn ("huh? no task with " + child.getParentTaskUID () + " in uid->task map.");
    }

    return parent;
  }

  /**
   * <pre>
   *
   * find matching reservation transport task
   * see if date overlaps
   * if it does, publish remove it and replace it with one with altered date span and quantity
   *
   * OK - could be MUCH more efficient - blackboard queries are extremely slow!
   *
   * </pre>
   */
  public void handleTransportTask(Task task1, Map reservedToActual) {
    // find matching reservation transport task
    final Task task = task1;
    final Collection units = findForPreps (task);
    final boolean isReserved = isReservedTask (task);

    if (isReservedTask (task) && !ownWorkflow(task)) {
      if (isInfoEnabled()) info (".handleTransportTask - skipping reserved task " + task.getUID () +
                                 " that's not in it's own workflow.");
      return;
    }

    if (isInfoEnabled())
      info (getName () + ".handleTransportTask - looking through blackboard for task to match " +
            ((isReserved) ? "reserved " : "normal ") + task.getUID());

    Collection matchingTaskCollection = getMatchingTasksByType (task, units, isReserved);

    // there can be more than one matching reserved task, e.g. when the actual contains multiple different DODICS:
    //  types [DODIC/C787, DODIC/C380, ] vs [DODIC/C787] (Task 1)
    //  types [DODIC/C787, DODIC/C380, ] vs [DODIC/C380] (Task 2)

    if (matchingTaskCollection.isEmpty () && isInfoEnabled ()) {
      info (".handleTransportTask - could not find matching task for " + task.getUID());
      return;
    }
    if (isInfoEnabled ()) {
      info  (".handleTransportTask - found " + matchingTaskCollection.size () + " matches for task " + task.getUID());
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
        if (isInfoEnabled()) {
          info (".handleTransportTask - huh? reserved task " + reservedTask.getUID () +
                " not a member of it's own workflow " + reservedTask.getWorkflow () +
                "\nuids " + uidsWorkflow(reservedTask));
        }
      }
      else {
        if (isInfoEnabled()) {
          info (".handleTransportTask - updating map with reserved task " + reservedTask.getUID () +
                " and actual " + actual.getUID());
        }
      }

      updateMap (reservedToActual, actual, reservedTask);
    }
  }

  /**
   * Find those tasks of the same ammo type and try to find tasks that overlap in
   * time with the given task.
   *
   * @param task to look for matching tasks for
   * @param units units the task is for
   * @param isReserved is the task a reserved (project supply child) task
   * @return List of tasks that are for the same type of ammo, same unit, and for an overlapping period of time
   */
  protected List getMatchingTasksByType (Task task, Collection units, boolean isReserved) {
    List matches = new ArrayList ();

    Collection types = getTypesInContainer (task);

    // reduce set of possible matching tasks to those for the same types of ammo
    List possibleMatches = new ArrayList();
    for (Iterator iter = types.iterator(); iter.hasNext(); ) {
      String type = (String) iter.next();

      List tasksForType = (List)typeToTasks.get(type);
      if (tasksForType == null) {
	if (isWarnEnabled()) {
	  warn ("Could not find tasks for type <" + type + "> for task " + task.getUID() + 
		" with d.o. " + task.getDirectObject() + " and types <" + types + ">");
	}
      }
      else {
	possibleMatches.addAll (tasksForType);
      }
    }

    if (isInfoEnabled()) {
      info ("examining " + possibleMatches.size()+ " possible matches for task " + task.getUID());
    }

    for (Iterator iter = possibleMatches.iterator (); iter.hasNext(); ) {
      Task examinedTask = (Task) iter.next();
      if (task.getUID().equals(examinedTask.getUID())) {
	if (isInfoEnabled()) {
	  info ("skipping self " + examinedTask.getUID());
	}

	continue; // don't match yourself
      }

      // better be a transport task
      /*
      if (!(examinedTask.getVerb ().equals (Constants.Verb.TRANSPORT))) {
	if (isDebugEnabled())
	  debug ("skipping non-transport task " + examinedTask.getUID());
	continue;
      }
      */

      // is it a reservation task?
      boolean examinedIsReserved = isReservedTask (examinedTask);
      if ((!isReserved && !examinedIsReserved) ||
	  ( isReserved &&  examinedIsReserved)) {
	if (isInfoEnabled()) {
	  info ("skipping examined transport task because same type " + examinedTask.getUID() + " and " + task.getUID());
	}
	continue;
      }

      if (examinedIsReserved) {
	// has it already been removed from workflow?
	if (!taskInWorkflow (examinedTask, examinedTask.getWorkflow())) {
	  if (isInfoEnabled ())
	    info ("skipping reserved transport task " + examinedTask.getUID() +
		  " since it's already been removed from it's workflow.");
	  continue;
	}
      }
      else if (!(task instanceof MPTask)) {
	if (isInfoEnabled ())
	  info ("saw transport task "  + task.getUID() + " vs examined " + examinedTask.getUID());
      }

      // is it for the same org?
      Collection examinedUnits = findForPreps (examinedTask);
      Collection copy = new ArrayList(examinedUnits);
      examinedUnits.retainAll (units);
      if (examinedUnits.isEmpty ()) {
	if (isInfoEnabled())
	  info ("skipping transport task where units don't match " + units + " vs examined " + copy);
	continue;
      }

      // THIS IS DONE WHEN WE GET POSSIBLE MATCHES
      // are they for the same type of supply?
      //      if (!contentTypesOverlap (task, examinedTask)) 
      //	continue;

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

      if (transportDateWithinReservedWindow (transport, reserved)) {
	matches.add (examinedTask);
      }
    }

    return matches;
  }

  /*
  protected boolean contentTypesOverlap (Task task, Task examinedTask){
    Collection typeIDs  = getTypesInContainer (task);

    Collection examinedTypeIDs  = getTypesInContainer (examinedTask);
    Collection copy = new ArrayList (examinedTypeIDs);

    copy.retainAll (typeIDs);
    if (copy.isEmpty()) {
      if (isDebugEnabled())
        debug ("skipping transport task where type ids don't match. " + 
	       "No overlap between examined container " +
               examinedTypeIDs +
               " and other container's list " + typeIDs);
      return false;
    }
    else {
      return true;
    }
  }
  */

  /** 
   * @return all the ammo types inside the task's milvan 
   */
  protected Collection getTypesInContainer (Task task) {
    Container taskDO;
    if (task.getDirectObject() instanceof AssetGroup) {
      taskDO =
	(Container) ((AssetGroup)
		     task.getDirectObject()).getAssets().iterator().next();
    }
    else {
      taskDO = (Container)task.getDirectObject ();
    }
    ContentsPG contents = taskDO.getContentsPG ();
    Collection typeIDs  = contents.getTypeIdentifications ();
    return typeIDs;
  }

  /** 
   * @return a report on all the ammo types inside both tasks' milvan 
   */
  protected String reportContentTypes (Task task, Task examinedTask){
    if (task.getDirectObject () instanceof Container) {
      Container taskDO = (Container)task.getDirectObject ();
      Container examinedDO;
      if (examinedTask.getDirectObject() instanceof AssetGroup) {
	examinedDO=
          (Container) ((AssetGroup)
		       examinedTask.getDirectObject()).getAssets().iterator().next();
      }
      else {
	examinedDO = (Container)examinedTask.getDirectObject ();
      }
      
      ContentsPG contents = taskDO.getContentsPG ();
      Collection typeIDs  = contents.getTypeIdentifications ();
      
      ContentsPG examinedContents = examinedDO.getContentsPG ();
      Collection examinedTypeIDs  = examinedContents.getTypeIdentifications ();
      
      StringBuffer buf =new StringBuffer();

      buf.append("[");
      for (Iterator iter = typeIDs.iterator(); iter.hasNext();)
	buf.append(iter.next() + ", ");
      buf.append("]");
      buf.append(" vs ");
      buf.append("[");
      for (Iterator iter = examinedTypeIDs.iterator(); iter.hasNext();)
	buf.append(iter.next());
      buf.append("]");

      return buf.toString();
    }
    else 
      return "<not a container>";
  }

  /** 
   * Must check best dates of Supply task parents. 
   * Fix for bug #12467 and #12468.
   *
   * The problem is that the packer can aggregate supply tasks for two different ammo types
   * into one milvan, and the EARLIER of the two dates becomes the END_TIME best for the
   * MP Transport task.  It was this time I was using to determine overlap of the Transport
   * Reservation that's derived from the ProjectSupply.
   *
   * E.g. the packer can make:
   *
   * Parent #1 : C380 arrive at 10/18
   * Parent #2 : A986 arrive at 10/15
   * -> MPTask with milvan arrive at 10/15 - don't want to make parent #2 be late
   *
   * So when we try to find the overlap with a Reservation:
   * C380 from 9/25->10/20
   * if we use the MPTask's arrival time, the overlap results in a Reservation 
   * from 10/15->10/20 not from 10/18->10/20.
   *
   * Since the packer may do different packings from run-to-run, the quantities of Reserved
   * ammo varies from run to run (and potentially the # of milvans).
   * 
   * @param transport - actual transport task
   * @param reserved projected transport task
   * @return true if transport task overlaps in time with the reserved task 
   */
  protected boolean transportDateWithinReservedWindow (Task transport, Task reserved){
    Date reservedReady = (Date) prepHelper.getIndirectObject (reserved, START);

    Date latestDateOfParents = getLatestParentEndDate (transport, reserved);

    if (reservedReady.getTime () >= latestDateOfParents.getTime()) {
      if (isDebugEnabled()) {
        debug ("skipping actual transport task where task latest supply parent time " +latestDateOfParents +
               " before examined ready " + reservedReady);
      }

      return false;
    }

    if (isInfoEnabled ()) {
      info ("transport " + transport.getUID() + " latest supply parent time "+ latestDateOfParents+ 
	    " after reserved " + reserved.getUID()+
            " ready " + reservedReady);
    }

    return true;
  }

  /**
   *  Must check best dates of Supply task parents. 
   *
   *  Remember to round to end of day!  A task arriving at midnight + 1 sec
   *  of a day counts for the whole day against the projection.
   */
  protected Date getLatestParentEndDate (Task transport, Task reserved) {
    Collection types = getTypesInContainer (reserved);
    if (types.isEmpty()) {
      if (isWarnEnabled ()) {
	warn (getName () + " - huh? no types in container for " + reserved);
      }
    }

    // should be only one ammo type in container
    String reservedAmmoType  = (String) types.iterator().next();
    Date latest = getLatestParentEndDate (transport, reservedAmmoType);

    // round to nearest next day boundary
    long round = ((latest.getTime() + MILLIS_PER_DAY)/MILLIS_PER_DAY)*MILLIS_PER_DAY;

    Date roundDate = new Date(round);

    if (roundDate.getTime () == latest.getTime()) {
      if (isWarnEnabled ()) {
	warn ("huh? Didn't move date : date was " + latest + " now " + roundDate);
      }
    }
    else if (isInfoEnabled ()) {
      info ("date was " + latest + " now " + roundDate);
    }

    return roundDate;
  }

  /**
   *  Must check best dates of Supply task parents. 
   */
  protected Date getLatestParentEndDate (Task transport, String reservedAmmoType) {
    Date best                = prefHelper.getBestDate  (transport);
    long latestDateOfParents = best.getTime();

    if (transport instanceof MPTask) {
      MPTask multiParent = (MPTask) transport;
      for (Enumeration en = multiParent.getParentTasks(); en.hasMoreElements(); ) {
	Task supplyParent = (Task) en.nextElement();
	
	// check to see parent is of same ammo type
	TypeIdentificationPG typePG = supplyParent.getDirectObject ().getTypeIdentificationPG();

	if (typePG == null) { // never happen
	  if (isWarnEnabled()) {
	    warn ("huh? for task " + supplyParent + " the direct object is missing its type PG?");
	  }
	}

	String typeName = typePG.getTypeIdentification ();
	if (typeName.equals (reservedAmmoType)) {
	  // check to see if date is later
	  long parentBest = prefHelper.getBestDate(supplyParent).getTime();
	  if (parentBest > latestDateOfParents) {
	    latestDateOfParents = parentBest;
	  }
	}
      }

      return new Date(latestDateOfParents);
    }
    else {
      // never used
      if (isWarnEnabled()) {
	warn ("transport task " +transport+ " is not an MPTask?");
      }

      return prefHelper.getBestDate (transport);
    }
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

  /**
   * <pre>
   * Called from processTasks. 
   *
   * Compares transport reservation and overlapping actual transport task.
   *
   * Creates a replacement for existing reserved task, if there is any time
   * span left where it doesn't overlap the actual task.  If the actual completely 
   * overlaps the reservation, creates a successful disposition for the task.
   *
   * If there is partial overlap, adjusts the earliest arrival date on the reserved task
   * to be equal to the best date of the actual and updates the contents pg of the direct
   * object to indicate a smaller weight.
   *
   * As a convenience, adds the START prep to the reserved task, indicating the start
   * of the period of the reservation.  This is used in transportDateWithinReservedWindow to
   * determine if an actual falls in the span of a reservation and to indicate whether
   * a transport task is indeed a reservation.
   *
   * </pre>
   * @param task actual transport task
   * @param reservedTask reserved transport task to be replaced
   */
  protected void dealWithReservedTask (Task task, Task reservedTask, Task reservedParent) {
    // preconditions
    if (isReservedTask (task))
      error ("arg - task "  + task.getUID () + " is a reserved task.");

    if (!isReservedTask (reservedTask))
      error ("arg - task "  + reservedTask.getUID () + " is not a reserved task.");

    NewWorkflow tasksWorkflow = (NewWorkflow) reservedTask.getWorkflow ();

    if (tasksWorkflow == null) {
      error ("huh? reservedTask " + reservedTask.getUID () + " workflow is null?");
      return;
    }

    int numTasksBefore = numTasksInWorkflow (tasksWorkflow);

    // real code starts here ---

    // should be only one ammo type in container
    Date best          = getLatestParentEndDate (task, reservedTask);
    Date reservedBest  = prefHelper.getBestDate (reservedTask);
    long daysLeft      = (reservedBest.getTime()-best.getTime())/MILLIS_PER_DAY;
    Date reservedReady = (Date) prepHelper.getIndirectObject (reservedTask, START);
    long currentDays   = (reservedBest.getTime()-reservedReady.getTime())/MILLIS_PER_DAY;

    if (isInfoEnabled ()) {
      info (getName() + ".dealWithReservedTask - applying actual\n" + task.getUID () + " best " + best +
            "\nto reserved\n" + reservedTask.getUID() + " reserved ready " + reservedReady + " to best " + reservedBest);
      info ("\t" + reportContentTypes (task, reservedTask));
    }

    double factor = (double)daysLeft/(double)currentDays;
    if (factor > 0) { // if the actual doesn't completely cover the period of the projection
      Asset deliveredAsset =
          getTrimmedDirectObject (reservedTask.getDirectObject(), factor);

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
      replacement.setContext(reservedTask.getContext());

      if (isInfoEnabled ())
        info ("Reserved task " + reservedTask.getUID () +
              " current days " + currentDays +
              " daysLeft " + daysLeft +
              " replacing asset weights.");

      if (isInfoEnabled ())
        info ("on task " + replacement.getUID() + " replacing start prep date " + reservedReady +
              " with " + best + " - also becomes early date for task.");

      prepHelper.replacePrepOnTask (replacement,
                                    prepHelper.makePrepositionalPhrase(ldmf,START,best));

      if (!FIND_FOR_UNIT_PREP_ON_TASK) {
	prepHelper.replacePrepOnTask (replacement, 
				      prepHelper.makePrepositionalPhrase (ldmf, Constants.Preposition.FOR, 
									  getClusterName()));
      }

      prefHelper.replacePreference (replacement,
                                    prefHelper.makeEndDatePreference(ldmf,
                                                                     best,
                                                                     reservedBest,
                                                                     prefHelper.getLateDate(reservedTask)));

      replacement.setWorkflow(tasksWorkflow);
      tasksWorkflow.addTask (replacement);
      publishAdd (replacement);
      if (isInfoEnabled ()) {
        info ("Publishing replacement " + replacement.getUID() + " in workflow "+ tasksWorkflow.getUID() +
              " start " + best + " best " + reservedBest);
      }

      if (best.getTime () != ((Date)prepHelper.getIndirectObject(replacement, START)).getTime()) {
        error ("replacement start " + prepHelper.getIndirectObject(replacement, START) + " != " + best);
      }

      if (!taskInWorkflow(replacement, tasksWorkflow)) {
        error ("huh? after adding to workflow, replacement " + replacement.getUID () + " is not in workflow " + tasksWorkflow + "?");
      }
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
      if (reservedParent != null) { // I guess if the task is being removed, the parent could be missing from the blackboard
        PlanElement exp = reservedParent.getPlanElement();
        if (exp == null) {
          if (isWarnEnabled ()) {
            warn ("found task " + reservedParent.getUID () + " verb " + reservedParent.getVerb() + " that had no plan element.");
          }
        }
        else {
          publishRemove(exp);
          if (isInfoEnabled ()) {
            info ("removing expansion of task " + exp.getTask().getUID());
          }
          AllocationResult ar = makeSuccessfulDisposition(reservedParent);
          Disposition disposition =
              ldmf.createDisposition(reservedParent.getPlan(), reservedParent, ar);
          publishAdd (disposition);
          if (isInfoEnabled ())
            info (" task " + reservedParent.getUID () + " verb " + reservedParent.getVerb() + " - will get a DISPOSITION, since workflow now empty.");
        }
      }
    }

    if (factor < 0.00000001 && (numTasksAfter != numTasksBefore-1))
      error ("Reserved task " + reservedTask.getUID() + "'s workflow had " + numTasksBefore + " should have " + (numTasksBefore-1) +
             " but has " + numTasksAfter);
    else if (factor > 0 && (numTasksAfter != numTasksBefore))
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
    if (workflow == null)
      return false;
    String [] uidsInWorkflow = ((WorkflowImpl)workflow).getTaskIDs ();
    boolean found = false;
    for (int i = 0; i < uidsInWorkflow.length && !found; i++)
      if (uidsInWorkflow[i].equals(task.getUID().toString()))
        found = true;

    return found;
  }

  protected String uidsWorkflow (Task task) {
    if (task.getWorkflow () == null) {
      return "<null workflow for " + task.getUID() + ">";
    }

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
    for (Enumeration en = workflow.getTasks (); en.hasMoreElements(); en.nextElement()) { num++; }
    return num;
  }

  /**
   * Makes allocation result with aspect values that echo the task preferences
   *
   * @param task to dispose
   * @return successful allocation result with aspect values taken from task prefs
   */
  protected AllocationResult makeSuccessfulDisposition(Task task) {
    Enumeration prefEnum;
    synchronized(task) { prefEnum = task.getPreferences(); } // bug #2125
    List aspectValues = new ArrayList ();
    while (prefEnum.hasMoreElements()) {
      Preference pref = (Preference)prefEnum.nextElement();
      ScoringFunction sfunc = pref.getScoringFunction();
      aspectValues.add (sfunc.getBest().getAspectValue());
    }

    AspectValue [] aspectValueArray =
        (AspectValue []) aspectValues.toArray(new AspectValue [aspectValues.size()]);

    AllocationResult successfulAR = ldmf.newAllocationResult(1.0,
							     true,
							     aspectValueArray);
    return successfulAR;
  }

  /**
   * Look on task for what unit the ammo is for.  Generally this will be 191-ORDN.
   *
   * If FIND_FOR_UNIT_PREP_ON_TASK is false, just use agent identifier (OSC).
   */
  protected Collection findForPreps (final Task task) {
    List units;
    if (FIND_FOR_UNIT_PREP_ON_TASK) {
      units = new ArrayList();
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
    }
    else {
      if (defaultUnit == null) {
	defaultUnit = new ArrayList();
	defaultUnit.add (getClusterName()); // i.e. OSC
      }
      units = defaultUnit;
    }
    return units;
  }

  /**
   * Assumes the direct object is either an asset group or a container (milvan).
   * If it's a container, updates the contentsPG to reflect a new weight that is the
   * old multiplied by factor (0.0 < factor < 1.0).
   *
   * Called from dealWithReservedTask.
   *
   * @param directObject old container or asset group
   * @param factor to reduce container weight by
   * @return new or old milvan with updated contentsPG
   * @see #dealWithReservedTask
   */
  protected Asset getTrimmedDirectObject (Asset directObject, double factor) {
    if (directObject instanceof AssetGroup) {
      double total = 0.0;
      Container last = null;

      for (Iterator iter =
          ((AssetGroup) directObject).getAssets().iterator();
           iter.hasNext();) {
        last = (Container) iter.next();
        total += getContainerTons (last);
      }

      if (last == null) {
        error ("Nothing in the asset group of milvans?");
      }

      ContentsPG contents = last.getContentsPG ();

      String nomen =
          (String) contents.getNomenclatures().iterator().next();
      String type  =
          (String) contents.getTypeIdentifications().iterator().next();
      String unit =
          (String) contents.getReceivers().iterator().next();

      return getMilvanDirectObject (nomen, type, unit, total*factor);
    }
    else {
      Container reserved = (Container)directObject;
      ContentsPG contents = reserved.getContentsPG();
      Collection weights = contents.getWeights();
      Mass weight = (Mass) weights.iterator().next();
      weights.remove (weight);
      weights.add (new Mass (weight.getKilograms()*factor, Mass.KILOGRAMS));
      return reserved;
    }
  }

  /** assumes only one type of ammo in container -- true for reservations */
  protected double getContainerTons (Container container) {
    ContentsPG contents = container.getContentsPG();
    Collection weights  = contents.getWeights();
    Mass weight = (Mass) weights.iterator().next();
    return weight.getShortTons();
  }
}
