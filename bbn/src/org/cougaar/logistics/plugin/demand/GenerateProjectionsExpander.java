/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.demand;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.PlanScheduleElementType;
import org.cougaar.glm.plugins.AssetUtils;
import org.cougaar.glm.plugins.ScheduleUtils;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.glm.plugins.TimeUtils;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.TimeSpan;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Date;

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
  public void expandGenerateProjections(Task gpTask, Schedule schedule, Asset consumer) {

    // TODO: document, add logger, handle null checks

    consumer = convertAggregateToAsset(consumer);
    PropertyGroup pg = consumer.searchForPropertyGroup(dfPlugin.getSupplyClassPG());

    if (gpTask.getPlanElement() != null) {
      System.out.println(" pg task plan element is of type " + gpTask.getPlanElement());
      if (gpTask.getPlanElement() instanceof Expansion) {
        handleExpandedGpTask(gpTask, schedule, consumer, pg);
      }
    }
    else {
      Collection subTasks = buildTaskList(pg, getConsumed(pg), schedule, gpTask, consumer);
      if (!subTasks.isEmpty()) {
        createAndPublishExpansion(gpTask, subTasks);
      }
      else {
        createDisposition(gpTask);
      }
    }
  }

  private void handleExpandedGpTask(Task gpTask, Schedule schedule, Asset consumer, PropertyGroup pg) {
    Collection consumedItems = getConsumed(pg);

    ArrayList assetList = new ArrayList(1);
    for (Iterator iterator = consumedItems.iterator(); iterator.hasNext();) {
      Asset asset = (Asset) iterator.next();
      Collection publishedTasks =  dfPlugin.projectSupplySet(gpTask, asset);
      if (publishedTasks.isEmpty()) {
        continue;
      }
      assetList.clear();
      assetList.add(asset);
      logger.debug("Handling consumed item " + dfPlugin.getAssetUtils().getAssetIdentifier(asset));
      Collection newTasks = buildTaskList(pg, assetList, schedule, gpTask, consumer);
      Schedule publishedTasksSched = newObjectSchedule(publishedTasks);
      Schedule newTasksSched = newObjectSchedule(newTasks);
      Collection diffedTasks = diffProjections(publishedTasksSched, newTasksSched);
      addToAndPublishExpansion(gpTask, diffedTasks);
    }
  }

  private void createDisposition(Task gpTask) {
    // FIX ME LATER... Dispose of GPs that don't have any subtasks.
    // Later we won't create the GPS after we get the new db table that
    // defines which MEIs really are type x consumers.
    AspectValue avs[] = new AspectValue[1];
    avs[0] = AspectValue.newAspectValue(AspectType.START_TIME,
                                        TaskUtils.getPreference(gpTask, AspectType.START_TIME));
    AllocationResult dispAR =
        getPlanningFactory().newAllocationResult(1.0, true, avs);
    Disposition disp = getPlanningFactory().createDisposition(gpTask.getPlan(), gpTask, dispAR);
    dfPlugin.publishAdd(disp);
  }

  private Collection buildTaskList(PropertyGroup pg, Collection items,
                                   Schedule schedule, Task gpTask,
                                   Asset consumer) {
    List subTasks = new ArrayList();
    Asset consumedItem;

    if (!items.isEmpty()) {
      for (Iterator iterator = items.iterator(); iterator.hasNext();) {
        consumedItem = (Asset) iterator.next();

        Enumeration scheduleElements = schedule.getAllScheduleElements();
        Rate rate;
        // for every item consumed, walk the schedule elements and get the rates
        while (scheduleElements.hasMoreElements()) {
          ObjectScheduleElement ose = (ObjectScheduleElement) scheduleElements.nextElement();
          rate = getRate(pg, consumedItem, (List) ose.getObject());
          // FIXME:  Should we report a warning or is this normal????
          // this is happening at both ends of the schedule where the MEI
          // is available but there is not matching org act for the time period.
          // (if the orgact is null then the bg returns a null rate)
          if (rate == null)  {
            continue;
          }
          logger.info("checking Rate on "+dfPlugin.getAssetUtils().getAssetIdentifier(consumedItem)+
		      " rate "+getDailyQuantity(rate));
          subTasks.add(createProjectSupplyTask(gpTask, consumer, consumedItem, ose.getStartTime(),
                                               ose.getEndTime(), rate));
        }
      }
    }
    return subTasks;
  }

  private Asset convertAggregateToAsset(Asset consumer) {
    if (consumer instanceof AggregateAsset) {
      consumer = ((AggregateAsset) consumer).getAsset();
    }
    return consumer;
  }

  /** Create a Time Preference for the Refill Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  @param aspectType The AspectType of the preference- should be start_time or end_time
   *  @return Preference The new Time Preference
   **/
  private Preference createTimePreference(long bestDay, int aspectType) {
    long early = dfPlugin.getLogOPlanStartTime();
    long late = dfPlugin.getTimeUtils().addNDays(bestDay, 1);
    long end = dfPlugin.getLogOPlanEndTime();
    double daysBetween = ((end - bestDay) / 86400000);
    //Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
    // define alpha .25
    double alpha = .25;

    Vector points = new Vector();
    AspectScorePoint earliest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, early), alpha);
    AspectScorePoint best = new AspectScorePoint(AspectValue.newAspectValue(aspectType, bestDay), 0.0);
    AspectScorePoint first_late = new AspectScorePoint(AspectValue.newAspectValue(aspectType, late), alpha);
    AspectScorePoint latest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, end), (alpha + late_score));

    points.addElement(earliest);
    points.addElement(best);
    points.addElement(first_late);
    points.addElement(latest);
    ScoringFunction timeSF = ScoringFunction.createPiecewiseLinearScoringFunction(points.elements());
    return getPlanningFactory().newPreference(aspectType, timeSF);

    // prefs.addElement(TaskUtils.createDemandRatePreference(planFactory, rate));
    //return prefs;
  }


  /** Create FOR, TO, MAINTAIN, and OFTYPE prepositional phrases
   *  for use by the subclasses.
   * @param consumer the consumer the task supports
   * @return Vector of PrepostionalPhrases
   **/
  protected Vector createPrepPhrases(Object consumer, Task parentTask, long end) {
    Vector prepPhrases = new Vector();

    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.OFTYPE, dfPlugin.getSupplyType()));
    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.FOR, getOrgName()));

    createGeolocPrepPhrases(parentTask, end, prepPhrases);

    if (consumer != null) {
      createMaintainingPrepPhrases(consumer, prepPhrases);
    }

    return prepPhrases;
  }

  private void createMaintainingPrepPhrases(Object consumer, Vector prepPhrases) {
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

  private void createGeolocPrepPhrases(Task parentTask, long end, Vector prepPhrases) {
    GeolocLocation geoloc = getGeolocLocation(parentTask, (end-1000));
    if (geoloc != null) {
      prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.TO, geoloc));
    } else { // Try to use HomeLocation
      try {
        geoloc = (GeolocLocation) dfPlugin.getMyOrganization().getMilitaryOrgPG().getHomeLocation();
        prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.TO, geoloc));
      } catch (NullPointerException npe) {
        logger.error("demandTaskPrepPhrases(), Unable to find Location for Transport");
      }
    }
  }

  protected GeolocLocation getGeolocLocation(Task parent_task, long time) {
    Enumeration geolocs = AssetUtils.getGeolocLocationAtTime(dfPlugin.getMyOrganization(), time);
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

  private void createAndPublishExpansion(Task parent, Collection subtasks) {
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      dfPlugin.publishAdd(subtasksIT.next());
    }
    Workflow wf = buildWorkflow(parent, subtasks);
    Expansion expansion = getPlanningFactory().createExpansion(parent.getPlan(), parent, wf, null);
    logger.info("GenerateProjectionsExpander publishing expansion " + dfPlugin.getClusterId());
     dfPlugin.publishAdd(expansion);
  }

  private void addToAndPublishExpansion(Task parent, Collection subtasks) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      Task task = (Task) subtasksIT.next();
      dfPlugin.publishAdd(task);
      wf.addTask(task);
      ((NewTask) task ).setWorkflow(wf);
    }
    dfPlugin.publishChange(expansion);
  }

  private NewTask createProjectSupplyTask(Task parentTask, Asset consumer, Asset consumedItem, long start,
                                          long end, Rate rate) {
    //logger.info("GenerateProjectionsExpander create ProjectSupply Task " + dfPlugin.getClusterId());
    NewTask newTask = getPlanningFactory().newTask();
    newTask.setParentTask(parentTask);
    newTask.setPlan(parentTask.getPlan());
    newTask.setDirectObject(consumedItem);
    newTask.setVerb(Verb.getVerb(Constants.Verb.PROJECTSUPPLY));
    newTask.setCommitmentDate(new Date(start));
    Vector prefs = new Vector();
    prefs.addElement(TaskUtils.createDemandRatePreference(getPlanningFactory(), rate));
    // start and end from schedule element
    prefs.addElement(createTimePreference(start, AspectType.START_TIME));
    prefs.addElement(createTimePreference(end, AspectType.END_TIME));

    newTask.setPreferences(prefs.elements());
    Vector childPhrases = createPrepPhrases(consumer, parentTask, end);
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

  private PrepositionalPhrase newPrepositionalPhrase(String preposition,
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

  public static Schedule newObjectSchedule(Collection tasks) {
    Vector os_elements = new Vector();
    ScheduleImpl s = new ScheduleImpl();
    s.setScheduleElementType(PlanScheduleElementType.OBJECT);
    s.setScheduleType(ScheduleType.OTHER);

    for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
      Task task = (Task)iterator.next();
      os_elements.add(new ObjectScheduleElement(TaskUtils.getStartTime(task),
                                                TaskUtils.getEndTime(task), task));
    }
    s.setScheduleElements(os_elements.elements());
    return s;
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
  protected Collection diffProjections(Schedule published_schedule, Schedule newtask_schedule) {
    // Check for an empty schedule
    if (newtask_schedule.isEmpty()) {
      // Rescind any tasks that were not accounted for
      logger.debug("publishChangeProjection(), New Task Schedule empty: "+newtask_schedule);
      Enumeration e = published_schedule.getAllScheduleElements();
      while (e.hasMoreElements()) {
        Task task = (Task) ((ObjectScheduleElement) e.nextElement()).getObject();
        logger.debug(printProjection("********** Removing task --> \n", task));
        publishRemoveFromExpansion(task);
      }
      return Collections.EMPTY_LIST;
    }

    List add_tasks = new ArrayList();
    // Remove from the published schedule of tasks  all tasks that occur BEFORE now but not overlapping now
    // These historical tasks should not be changed
    long now = dfPlugin.currentTimeMillis();
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
      Rate new_rate = TaskUtils.getRate(new_task);
      if (new_rate.equals(TaskUtils.getRate(published_task))) {
        // check end times not the same
        synchronized ( new_task ) {
          ((NewTask)published_task).setPreference(new_task.getPreference(AspectType.END_TIME));
        } // synch

        logger.debug( printProjection("extend old end", published_task));
        dfPlugin.publishChange(published_task);
      } else {
        // check to make sure start_time is not before now
        // long that is the maximum of now and the start_time
        long when = Math.max(now, TaskUtils.getStartTime(new_task));
        setEndTimePreference((NewTask) published_task, when);
        logger.debug(printProjection("truncate old end 1", published_task));
        dfPlugin.publishChange(published_task);
        setStartTimePreference((NewTask) new_task, when);
        logger.debug(printProjection("truncate new start 1", new_task));
        add_tasks.add(new_task);
      }
    } else if (new_task != null) {
      setStartTimePreference((NewTask) new_task, now);
      logger.debug(printProjection("truncate new start 2", new_task));
      add_tasks.add(new_task);
    } else if (published_task != null) {
      setEndTimePreference((NewTask) published_task, now);
      dfPlugin.publishChange(published_task);
      logger.debug(printProjection("truncate old end 2", published_task));
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
        logger.error("publishChangeProjection(), Bad Schedule: "+newtask_schedule);
        return Collections.EMPTY_LIST;
      }
      // Get overlapping schedule elements from start to end of new task
      c = published_schedule.getScheduleElementsWithTime(start);
      if (!c.isEmpty()) {
        // change the task to look like new task
        ose = (ObjectScheduleElement)c.iterator().next();
        published_task = (Task)ose.getObject();
        ((NewSchedule)published_schedule).removeScheduleElement(ose);

        logger.debug(" Comparing plublished task  "+dfPlugin.getTaskUtils().taskDesc(published_task)+
                     " with \n"+dfPlugin.getTaskUtils().taskDesc(new_task));
        published_task = changeTask(published_task, new_task);
        if (published_task != null) {
          logger.debug(printProjection("********** Replaced task with ---> \n", published_task));
          dfPlugin.publishChange(published_task);
        }
      }
      else {
        // no task exists that covers this timespan, publish it
        add_tasks.add(new_task);
      }
    }
    // Rescind any tasks that were not accounted for
    Enumeration e = published_schedule.getAllScheduleElements();
    while (e.hasMoreElements()) {
      Task task = (Task) ((ObjectScheduleElement) e.nextElement()).getObject();
      logger.debug(printProjection("********** Removing task --> \n", task));
      publishRemoveFromExpansion(task);
    }
    return add_tasks;
  }



  /** Create String defining task identity. Defaults to comparing preferences.
   * @param prev_task previously published task.
   * @param new_task already defined to have the same taskKey as task a.
   * @return null if the two tasks are the same,
   *         or returns task a modified for a publishChange.
   */
  protected Task changeTask(Task prev_task, Task new_task) {
    // Checks for changed preferences.
    if(prev_task==new_task) {
      return new_task;
    }
    if (!TaskUtils.comparePreferences(new_task, prev_task)) {
      synchronized ( new_task ) {
        Enumeration ntPrefs = new_task.getPreferences();
        ((NewTask)prev_task).setPreferences(ntPrefs);
      } // synch
      return prev_task;
    }
    return null;
  }

  protected void setStartTimePreference(NewTask task, long start) {
    task.setPreference(createTimePreference(start, AspectType.START_TIME));
  }

  protected void setEndTimePreference(NewTask task, long end) {
    task.setPreference(createTimePreference(end, AspectType.END_TIME));
  }

  private String printProjection(String msg, Task task) {
    return "diffProjections() "
        + task.getUID()
        + " " + msg + " "
        + TaskUtils.getDailyQuantity(task)
        + " "
        + TimeUtils.dateString(TaskUtils.getStartTime(task))
        + " to "
        + TimeUtils.dateString(TaskUtils.getEndTime(task));
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
      logger.error("cannot determine type of measure");
    }
    return result;
  }
}



