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

import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleElementType;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.TimeSpan;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * <pre>
 * The default ProjectionsExpander for the DemandForecastPlugin.
 *
 * This class expands a generate projections task into ProjectSupply tasks for all the
 * resource components (Parts,fuel,ammo) of the supply class needed by the GP's MEI.
 *
 *
 **/

public class GenerateProjectionsExpander extends DemandForecastModule implements GenProjExpanderIfc {

  private String myOrgName = null;

  public GenerateProjectionsExpander(DemandForecastPlugin dfPlugin) {
    super(dfPlugin);
  }

  /**
   * Expand the passed in GenerateProjectins task into the requisite ProjectSupply
   * tasks - one for each resource need of this MEI/Asset determined by the
   * BG associated with the passed in supplyPGClass.
   **/
  public void expandGenerateProjections(Task gpTask, Schedule schedule, Asset consumer, TimeSpan timespan) {

    // TODO: document, add logger, handle null checks

    consumer = convertAggregateToAsset(consumer);
    PropertyGroup pg = consumer.searchForPropertyGroup(dfPlugin.getSupplyClassPG());

    dfPlugin.updateStartAndEndTimes();

    PlanElement pe = gpTask.getPlanElement();
    if ((pe != null) && !(pe instanceof Disposition)) {
      if (pe instanceof Expansion) {
        handleExpandedGpTask(gpTask, schedule, consumer, pg, timespan);
      }
    }
    else {
      if (pe != null) {
        dfPlugin.publishRemove(pe);
      }
      Collection subTasks = new ArrayList(0);
      if (schedule != null) {
      //We are trimming the time span when task scheduler is off down to currentTimeMillis start time
      //to only project from here on out.
      Schedule trimmedSchedule = ScheduleUtils.trimObjectSchedule(schedule,timespan);
      subTasks = buildTaskList(pg, getConsumed(pg), trimmedSchedule, gpTask, consumer);
      }
      if (!subTasks.isEmpty()) {
        createAndPublishExpansion(gpTask, subTasks);
      }
      else {
        if(logger.isInfoEnabled()) {
          logger.info("Creating a disposition for gpTask "+gpTask);
        }
        createDisposition(gpTask);
      }
    }
  }

  protected void handleExpandedGpTask(Task gpTask, Schedule schedule, Asset consumer, PropertyGroup pg, TimeSpan timespan) {
    Collection consumedItems = getConsumed(pg);


    ArrayList assetList = new ArrayList(1);
    for (Iterator iterator = consumedItems.iterator(); iterator.hasNext();) {
      Asset asset = (Asset) iterator.next();
      Collection publishedTasks =  dfPlugin.projectSupplySet(gpTask, asset);
      assetList.clear();
      assetList.add(asset);
      if(logger.isDebugEnabled()) {
        logger.debug("Handling consumed item " + dfPlugin.getAssetUtils().getAssetIdentifier(asset));
      }
      Collection newTasks = buildTaskList(pg, assetList, schedule, gpTask, consumer);
      if (publishedTasks.isEmpty() && newTasks.isEmpty()) {
        continue;
      }
      Schedule publishedTasksSched = getTaskUtils().newObjectSchedule(publishedTasks);
      Schedule newTasksSched = getTaskUtils().newObjectSchedule(newTasks);


      Collection diffedTasks = diffProjections(publishedTasksSched, newTasksSched, timespan);
      addToAndPublishExpansion(gpTask, diffedTasks);
    }
  }

  protected void createDisposition(Task gpTask) {
    // FIX ME LATER... Dispose of GPs that don't have any subtasks.
    // Later we won't create the GPS after we get the new db table that
    // defines which MEIs really are type x consumers.
    AspectValue avs[] = new AspectValue[1];
    avs[0] = AspectValue.newAspectValue(AspectType.START_TIME,
                                        getTaskUtils().getPreference(gpTask, AspectType.START_TIME));
    AllocationResult dispAR =
        getPlanningFactory().newAllocationResult(Constants.Confidence.OBSERVED, true, avs);
    Disposition disp = getPlanningFactory().createDisposition(gpTask.getPlan(), gpTask, dispAR);
    dfPlugin.publishAdd(disp);
  }

  protected Collection buildTaskList(PropertyGroup pg, Collection items,
                                   Schedule schedule, Task gpTask,
                                   Asset consumer) {
    if (schedule == null || items.isEmpty()) {
      return Collections.EMPTY_LIST;
    }

    List subTasks = new ArrayList(items.size());
    for (Iterator iterator = items.iterator(); iterator.hasNext();) {
      Asset consumedItem = (Asset) iterator.next();

      Schedule rate_schedule =
        buildRateSchedule(pg, consumedItem, schedule, consumer);

      subTasks.add(
          createProjectSupplyTask(
            gpTask, consumer, consumedItem,
            rate_schedule));
    }
    return subTasks;
  }

  protected Schedule buildRateSchedule(
      PropertyGroup pg, Asset consumedItem,
      Schedule schedule, Asset consumer) {

    List rate_schedule_elements = new ArrayList();

    Enumeration scheduleElements = schedule.getAllScheduleElements();
    while (scheduleElements.hasMoreElements()) {
      ObjectScheduleElement ose = (ObjectScheduleElement) scheduleElements.nextElement();
      Rate rate = getRate(pg, consumedItem, getRateParams(ose));
      // A null rate can happen at both ends of the schedule where the MEI
      // is available but there is no matching org act for the time period.
      // (if the orgact is null then the bg returns a null rate)
      if (rate == null)  {
        continue;
      }
      if (getTaskUtils().getDailyQuantity(rate) <= 0.0) {
        if (logger.isWarnEnabled()) {
          logger.warn(getAssetUtils().getAssetIdentifier(consumedItem)+" on "+
              getAssetUtils().getAssetIdentifier(consumer)+" has a zero rate for period "+
              getTimeUtils().dateString(ose.getStartTime())+" - "+
              getTimeUtils().dateString(ose.getEndTime()));
        }
        continue;
      }
      if (logger.isInfoEnabled()) {
        logger.info("checking Rate on "+dfPlugin.getAssetUtils().getAssetIdentifier(consumedItem)+
            " rate "+getDailyQuantity(rate));
      }
      ObjectScheduleElement rse = 
        new ObjectScheduleElement(ose.getStartTime(), ose.getEndTime(), rate);
      rate_schedule_elements.add(rse);
    }

    ScheduleImpl rate_schedule = new ScheduleImpl();
    rate_schedule.setScheduleElementType(PlanScheduleElementType.OBJECT);
    rate_schedule.setScheduleType(ScheduleType.OTHER);
    rate_schedule.setScheduleElements(rate_schedule_elements);

    return rate_schedule;
  }
  
  protected List getRateParams(ObjectScheduleElement ose) {
    return (List) ose.getObject();
  }

  protected Asset convertAggregateToAsset(Asset consumer) {
    if (consumer instanceof AggregateAsset) {
      consumer = ((AggregateAsset) consumer).getAsset();
    }
    return consumer;
  }

  /** Create FOR, TO, MAINTAIN, OFTYPE, and DEMANDRATE prepositional phrases
   *  for use by the subclasses.
   * @param consumer the consumer the task supports
   * @return Vector of PrepostionalPhrases
   **/
  protected Vector createPrepPhrases(Object consumer, Task parentTask, long end, Schedule rate_schedule) {
    Vector prepPhrases = new Vector();

    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.OFTYPE, dfPlugin.getSupplyType()));
    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.FOR, getOrgName()));

    if (rate_schedule != null) {
      prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.DEMANDRATE, rate_schedule));
    }

    createGeolocPrepPhrases(parentTask, end, prepPhrases);

    if (consumer != null) {
      createMaintainingPrepPhrases(consumer, prepPhrases);
    }

    return prepPhrases;
  }

  protected void createMaintainingPrepPhrases(Object consumer, Vector prepPhrases) {
    MaintainedItem itemID;
    if (consumer instanceof Asset) {
      TypeIdentificationPG tip = ((Asset) consumer).getTypeIdentificationPG();
      ItemIdentificationPG iip = ((Asset) consumer).getItemIdentificationPG();
      if (iip != null) {
        itemID = MaintainedItem.findOrMakeMaintainedItem("Asset", tip.getTypeIdentification(),
                                                         iip.getItemIdentification(), tip.getNomenclature(),
                                                         dfPlugin);
      }
      else {
        itemID = MaintainedItem.findOrMakeMaintainedItem("Asset", tip.getTypeIdentification(),
                                                         null, tip.getNomenclature(), dfPlugin);
      }
    }
    else {
      itemID = MaintainedItem.findOrMakeMaintainedItem("Other", consumer.toString(), null, null, dfPlugin);
    }
    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.MAINTAINING, itemID));
  }

  protected void createGeolocPrepPhrases(Task parentTask, long end, Vector prepPhrases) {
    GeolocLocation geoloc = getGeolocLocation(parentTask, (end-1000));
    if (geoloc != null) {
      prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.TO, geoloc));
    } else { // Try to use HomeLocation
      try {
        geoloc = (GeolocLocation) dfPlugin.getMyOrganization().getMilitaryOrgPG().getHomeLocation();
        prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.TO, geoloc));
      } catch (NullPointerException npe) {
	if(logger.isErrorEnabled()) {
          logger.error("demandTaskPrepPhrases(), Unable to find Location for Transport");
	}
      }
    }
  }

  protected GeolocLocation getGeolocLocation(Task parent_task, long time) {
    Enumeration geolocs = getAssetUtils().getGeolocLocationAtTime(dfPlugin.getMyOrganization(), time);
    if (geolocs.hasMoreElements()) {
      GeolocLocation geoloc = (GeolocLocation) geolocs.nextElement();
//    GLMDebug.DEBUG("GenerateSupplyDemandExpander", clusterId_, "At "+TimeUtils.dateString(time)+ " the geoloc is "+geoloc);
      return geoloc;
    }
    return null;
  }

 /** Utility accessor to get the Org Name from my organization and keep it around **/
  private String getOrgName() {
    if (myOrgName == null) {
      myOrgName =dfPlugin.getMyOrganization().getItemIdentificationPG().getItemIdentification();
    }
    return myOrgName;
  }

  protected void createAndPublishExpansion(Task parent, Collection subtasks) {
    Workflow wf = buildWorkflow(parent, subtasks);
    Expansion expansion = getPlanningFactory().createExpansion(parent.getPlan(), parent, wf, null);
    if (logger.isInfoEnabled()) {
      logger.info("GenerateProjectionsExpander publishing expansion " + dfPlugin.getClusterId());
    }
    dfPlugin.publishAdd(expansion);
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      dfPlugin.publishAdd(subtasksIT.next());
    }
  }

  protected void addToAndPublishExpansion(Task parent, Collection subtasks) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      Task task = (Task) subtasksIT.next();
      wf.addTask(task);
      ((NewTask) task ).setWorkflow(wf);
      dfPlugin.publishAdd(task);
    }
    dfPlugin.publishChange(expansion);
  }

  /** @deprecated */
  protected NewTask createProjectSupplyTask(
      Task parentTask, Asset consumer, Asset consumedItem,
      long start, long end, Rate rate) {
    ObjectScheduleElement rse = new ObjectScheduleElement(start, end, rate);
    ScheduleImpl rate_schedule = new ScheduleImpl();
    rate_schedule.setScheduleElementType(PlanScheduleElementType.OBJECT);
    rate_schedule.setScheduleType(ScheduleType.OTHER);
    rate_schedule.setScheduleElements(Collections.singleton(rse));
    return createProjectSupplyTask(parentTask, consumer, consumedItem, rate_schedule);
  }

  protected NewTask createProjectSupplyTask(
      Task parentTask, Asset consumer, Asset consumedItem,
      Schedule rate_schedule) {
    //logger.info("GenerateProjectionsExpander create ProjectSupply Task " + dfPlugin.getClusterId());
    NewTask newTask = getPlanningFactory().newTask();
    newTask.setParentTask(parentTask);
    newTask.setPlan(parentTask.getPlan());
    newTask.setDirectObject(consumedItem);
    newTask.setVerb(Verb.get(Constants.Verb.PROJECTSUPPLY));
    // start and end from schedule element
    long start = rate_schedule.getStartTime();
    long end = rate_schedule.getEndTime();
    //newTask.setCommitmentDate(new Date(start));
    Vector prefs = new Vector(2);
    prefs.addElement(getTaskUtils().createTimePreference(start,
          dfPlugin.getLogOPlanStartTime(), dfPlugin.getLogOPlanEndTime(),
          AspectType.START_TIME, dfPlugin.getClusterId(), getPlanningFactory(), null));
    prefs.addElement(getTaskUtils().createTimePreference(end,
          dfPlugin.getLogOPlanStartTime(), dfPlugin.getLogOPlanEndTime(),
          AspectType.END_TIME, dfPlugin.getClusterId(), getPlanningFactory(), null));

    newTask.setPreferences(prefs.elements());
    Vector childPhrases = createPrepPhrases(consumer, parentTask, end, rate_schedule);
    newTask.setPrepositionalPhrases(childPhrases.elements());

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

  protected PrepositionalPhrase newPrepositionalPhrase(String preposition,
                                                     Object io) {
    NewPrepositionalPhrase pp = getPlanningFactory().newPrepositionalPhrase();
    pp.setPreposition(preposition);
    pp.setIndirectObject(io);
    return pp;
  }

  public Collection getConsumed(PropertyGroup pg) {
    Collection preds = null;
    Class parameters[] = {};
    Object arguments[] = {};
    Method m = null;
    try {
      m = dfPlugin.getSupplyClassPG().getMethod("getConsumed", parameters);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    try {
      preds = (Collection) m.invoke(pg, arguments);
      return preds;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return new ArrayList();
  }


  public Rate getRate(PropertyGroup pg,
                      Asset consumedItem,
                      List params) {
    Rate rate = null;
    Class parameters[] = {Asset.class, List.class};
    Object arguments[] = {consumedItem, params};
    Method m = null;
    try {
      m = dfPlugin.getSupplyClassPG().getMethod("getRate", parameters);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }
    try {
      rate = (Rate) m.invoke(pg, arguments);
      return rate;
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return null;
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
   * Reconcile an intended schedule of projections with the
   * currently published schedule of projections so as to reuse as
   * many of the existing projection tasks as possible.
   *
   * Generally as elements from the published schedule are used they
   * are removed from the schedule. Tasks remaining in the schedule
   * are rescinded.
   *
   * There are three regions of interest: before now, around now and
   * after now. These are each handled separately. In the region
   * before now, already published tasks are unconditionally
   * retained and new tasks are unconditionally ignored.
   *
   * In the region around now, tasks may start before now and end
   * after. If both a published task and a new task spanning now
   * exist, then there are two cases: If the demand rates are the
   * same, then the published task is changed to look like the new
   * task (by changing its end time preference). The start time of
   * the published task is unchanged. Think of the existing task
   * ending now and the new task starting now and then splicing the
   * two together into one task. If the rates are different, then
   * the existing task must end when the new task starts. The
   * current code accomplishes this by setting the end time
   * preference of the existing task to the start time of the new.
   * This is not exactly correct since we shouldn't change the past.
   * The times of the tasks should be no less than now.
   *
   * In the region after now, we try to match up the tasks. When a
   * match is possible, the existing task is changed if necessary
   * (and republished) otherwise it is rescinded and the new task
   * added.
   **/
  protected Collection diffProjections(Schedule published_schedule, Schedule newtask_schedule, TimeSpan timespan) {
    // Only diff new projections against published tasks during this TimeSpan
    published_schedule = ScheduleUtils.trimObjectSchedule(published_schedule, timespan);


    // Check for an empty schedule
    if (newtask_schedule.isEmpty()) {
      // Rescind any tasks that were not accounted for
      if(logger.isDebugEnabled()) {
        logger.debug("publishChangeProjection(), New Task Schedule empty: "+newtask_schedule);
      }
      Enumeration e = published_schedule.getAllScheduleElements();
      while (e.hasMoreElements()) {
        Task task = (Task) ((ObjectScheduleElement) e.nextElement()).getObject();
        if(logger.isDebugEnabled()) {
          logger.debug(printProjection("********** Removing task --> \n", task));
        }
        publishRemoveFromExpansion(task);
      }
      return Collections.EMPTY_LIST;
    }

    List add_tasks = new ArrayList();
    // Remove from the published schedule of tasks  all tasks that occur BEFORE now but not overlapping now
    // These historical tasks should not be changed
    //TODO: MWD warning pug getStartOfPeriod here in diffProjections - not sure if should be done.
    long now = dfPlugin.getStartOfPeriod(dfPlugin.currentTimeMillis());

    ObjectScheduleElement ose;
    Iterator historical_tasks =
        published_schedule.getEncapsulatedScheduleElements(TimeSpan.MIN_VALUE, now).iterator();
    while (historical_tasks.hasNext()) {
      ose = (ObjectScheduleElement)historical_tasks.next();
      ((NewSchedule)published_schedule).removeScheduleElement(ose);
    }

    // Examine the new task and published task that straddle NOW
    Task published_task = null;
    Task new_task = null;
    //Remove historical tasks that don't touch now first.
    Iterator new_historical_tasks =
      newtask_schedule.getEncapsulatedScheduleElements(TimeSpan.MIN_VALUE, now).iterator();
    while (new_historical_tasks.hasNext()) {
      ose = (ObjectScheduleElement)new_historical_tasks.next();
      ((NewSchedule)newtask_schedule).removeScheduleElement(ose);
      if (logger.isDebugEnabled()) {
        logger.debug("Found NEW Historical task... current society time is:" +
                     new Date(now) + " start time of ose is: " + new Date(ose.getStartTime()) +
                     " end time of ose is: " + new Date(ose.getEndTime()));
      }
    }
    Collection c = newtask_schedule.getScheduleElementsWithTime(now);
    if (!c.isEmpty()) {
      ose = (ObjectScheduleElement)c.iterator().next();
      new_task = (Task)ose.getObject();
      ((NewSchedule)newtask_schedule).removeScheduleElement(ose);
    }
    c =  published_schedule.getScheduleElementsWithTime(now);
    if (!c.isEmpty()) {
      ose = (ObjectScheduleElement)c.iterator().next();
      published_task = (Task)ose.getObject();
      ((NewSchedule)published_schedule).removeScheduleElement(ose);
    }
    if (published_task != null && new_task != null) {
      // Depending upon whether the rate is equal set the end time of the published task to the start or
      // end time of the new task
      //
      // FIXME compare rate schedules, publish change w/ change report instead of
      // replacing the entire task.  we may need the report to say the old rate_sched, or
      // maybe the UAInvPi can examine its buckets and figure this out.  the important point
      // is that we don't want to redo out our entire plan on the slightest orgAct change.
      logger.warn(
          "TODO: compare rate schedules on old task "+published_task.getUID()+
          " and new task "+new_task.getUID());
      Rate new_rate = getTaskUtils().getRate(new_task);
      if (new_rate.equals(getTaskUtils().getRate(published_task))) {
        // check end times not the same
        synchronized ( new_task ) {
          ((NewTask)published_task).setPreference(new_task.getPreference(AspectType.END_TIME));
          if (getTaskUtils().getStartTime(published_task) == getTaskUtils().getEndTime(published_task)) {
            if (logger.isWarnEnabled()) {
              logger.warn("diffProjections is setting a PUBLISHED task where the start time equals the end time " +
                  new Date(getTaskUtils().getStartTime(published_task)) + " the current society time is ->  "  +
                  new Date(dfPlugin.getCurrentTimeMillis()) +
                  "\nPublished task -> " + published_task + "\n New task -> " + new_task);
            } // synch
          }
        }

        if(logger.isDebugEnabled()) {
          logger.debug( printProjection("extend old end", published_task));
        }
        dfPlugin.publishChange(published_task);
      } else {
        // check to make sure start_time is not before now
        // long that is the maximum of now and the start_time
        long when = Math.max(now, getTaskUtils().getStartTime(new_task));
        setEndTimePreference((NewTask) published_task, when);
        if (getTaskUtils().getStartTime(published_task) == getTaskUtils().getEndTime(published_task)) {
          if (logger.isWarnEnabled()) {
            logger.warn("diffProjections is setting a PUBLISHED task where the start time equals the end time " +
                new Date(getTaskUtils().getStartTime(published_task)) +
                "\nPublished task -> " + published_task + " the current society time is ->  "  +
                new Date(dfPlugin.getCurrentTimeMillis()) + "\n New task -> " + new_task);
          }
        }
        if(logger.isDebugEnabled()) {
          logger.debug(printProjection("truncate old end 1", published_task));
        }
        dfPlugin.publishChange(published_task);
        setStartTimePreference((NewTask) new_task, when);
        if (getTaskUtils().getStartTime(new_task) == getTaskUtils().getEndTime(new_task)) {
          if (logger.isWarnEnabled()) {
            logger.warn("diffProjections is setting a NEW task where the start time equals the end time " +
                new Date(getTaskUtils().getStartTime(new_task)) +
                "\nPublished task -> " + published_task + " the current society time is ->  "  +
                new Date(dfPlugin.getCurrentTimeMillis()) + "\n New task -> " + new_task);
          }
        }
        if(logger.isDebugEnabled()) {
          logger.debug(printProjection("truncate new start 1", new_task));
        }
        add_tasks.add(new_task);
      }
    } else if (new_task != null) {
      setStartTimePreference((NewTask) new_task, now);
      if (getTaskUtils().getStartTime(new_task) == getTaskUtils().getEndTime(new_task)) {
        if (logger.isWarnEnabled()) {
          logger.warn("diffProjections is setting a NEW task where the start time equals the end time " +
              new Date(getTaskUtils().getStartTime(new_task)) +
              "\nPublished task -> " + published_task + " the current society time is ->  "  +
              new Date(dfPlugin.getCurrentTimeMillis()) + "\n New task -> " + new_task);
        }
      }
      if(logger.isDebugEnabled()) {
        logger.debug(printProjection("truncate new start 2", new_task));
      }
      add_tasks.add(new_task);
    } else if (published_task != null) {
      setEndTimePreference((NewTask) published_task, now);
      if (getTaskUtils().getStartTime(published_task) == getTaskUtils().getEndTime(published_task)) {
        if (logger.isWarnEnabled()) {
          logger.warn("diffProjections is setting a PUBLISHED task where the start time equals the end time " +
              new Date(getTaskUtils().getStartTime(published_task)) +
              "\nPublished task -> " + published_task + " the current society time is ->  "  +
              new Date(dfPlugin.getCurrentTimeMillis()) + "\n New task -> " + new_task);
        }
      }
      dfPlugin.publishChange(published_task);
      if(logger.isDebugEnabled()) {
        logger.debug(printProjection("truncate old end 2", published_task));
      }
    }

    // Compare new tasks to previously scheduled tasks, if a published task is found that
    // spans the new task's start time then adjust the published task (if needed) and publish
    // the change.  If no task is found than add new_task to list of tasks to be published.
    // When start time of schedule is equal to TimeSpan.MIN_VALUE, schedule is empty
    long start;
    while (!newtask_schedule.isEmpty()) {
      start = newtask_schedule.getStartTime();
      ose = (ObjectScheduleElement)ScheduleUtils.getElementWithTime(newtask_schedule, start);
      if (ose != null) {
        new_task = (Task)ose.getObject();
        ((NewSchedule)newtask_schedule).removeScheduleElement(ose);
      }
      else {
        if(logger.isErrorEnabled()) {
          logger.error("publishChangeProjection(), Bad Schedule: "+newtask_schedule);
        }
        return Collections.EMPTY_LIST;
      }
      // Get overlapping schedule elements from start to end of new task
      c = published_schedule.getScheduleElementsWithTime(start);
      if (!c.isEmpty()) {
        // change the task to look like new task
        ose = (ObjectScheduleElement)c.iterator().next();
        published_task = (Task)ose.getObject();
        ((NewSchedule)published_schedule).removeScheduleElement(ose);

        if(logger.isDebugEnabled()) {
          logger.debug(" Comparing plublished task  "+dfPlugin.getTaskUtils().taskDesc(published_task)+
              " with \n"+dfPlugin.getTaskUtils().taskDesc(new_task));
        }
        published_task = getTaskUtils().changeTask(published_task, new_task);
        if (published_task != null) {
          if (getTaskUtils().getStartTime(published_task) == getTaskUtils().getEndTime(published_task)) {
            if (logger.isWarnEnabled()) {
              logger.warn("diffProjections is setting a PUBLISHED task where the start time equals the end time " +
                  new Date(getTaskUtils().getStartTime(published_task)) +
                  "\nPublished task -> " + published_task + "\n New task -> " + new_task);
            }
          }
          if(logger.isDebugEnabled()) {
            logger.debug(printProjection("********** Replaced task with ---> \n", published_task));
          }
          dfPlugin.publishChange(published_task);
        }
      }
      else {
        // no task exists that covers this timespan, publish it
        add_tasks.add(new_task);
        if (getTaskUtils().getStartTime(new_task) == getTaskUtils().getEndTime(new_task)) {
          if (logger.isWarnEnabled()) {
            logger.warn("diffProjections is setting a NEW task where the start time equals the end time " +
                new Date(getTaskUtils().getStartTime(new_task)) +
                "\nPublished task -> " + published_task + " the current society time is ->  "  +
                new Date(dfPlugin.getCurrentTimeMillis()) + "\n New task -> " + new_task);
          }
        }
      }
    }
    // Rescind any tasks that were not accounted for
    Enumeration e = published_schedule.getAllScheduleElements();
    while (e.hasMoreElements()) {
      Task task = (Task) ((ObjectScheduleElement) e.nextElement()).getObject();
      if(logger.isDebugEnabled()) {
        logger.debug(printProjection("********** Removing task --> \n", task));
      }
      publishRemoveFromExpansion(task);
    }
    return add_tasks;
  }

  protected void setStartTimePreference(NewTask task, long start) {
    task.setPreference(getTaskUtils().createTimePreference(start, dfPlugin.getLogOPlanStartTime(), dfPlugin.getLogOPlanEndTime(), AspectType.START_TIME, dfPlugin.getClusterId(), getPlanningFactory(), null));
  }

  protected void setEndTimePreference(NewTask task, long end) {
    task.setPreference(getTaskUtils().createTimePreference(end, dfPlugin.getLogOPlanStartTime(), dfPlugin.getLogOPlanEndTime(), AspectType.END_TIME, dfPlugin.getClusterId(), getPlanningFactory(), null));
  }

  protected String printProjection(String msg, Task task) {
    return "diffProjections() "
        + task.getUID()
        + " " + msg + " "
        + getTaskUtils().getDailyQuantity(task)
        + " "
        + getTimeUtils().dateString(getTaskUtils().getStartTime(task))
        + " to "
        + getTimeUtils().dateString(getTaskUtils().getEndTime(task));
  }

  public void publishRemoveFromExpansion(Task subtask) {
    NewWorkflow wf = (NewWorkflow) subtask.getWorkflow();
    if (wf != null) {
      wf.removeTask(subtask);
    }
    dfPlugin.publishRemove(subtask);
  }

  public double getDailyQuantity(Rate r) {
    Duration d = Duration.newDays(1.0);
    Scalar measure = (Scalar)r.computeNumerator(d);
    double result = Double.NaN;
    if (measure instanceof Volume) {
      result = ((Volume)measure).getGallons();
    } else if (measure instanceof Count) {
      result = ((Count)measure).getEaches();
    } else if (measure instanceof Mass) {
      result = ((Mass)measure).getShortTons();
    } else {
      if(logger.isErrorEnabled()) {
        logger.error("cannot determine type of measure");
      }
    }
    return result;
  }
}



