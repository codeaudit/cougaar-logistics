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
import org.cougaar.glm.plugins.AssetUtils;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.logistics.plugin.inventory.MaintainedItem;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.plan.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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


  public GenerateProjectionsExpander(DemandForecastPlugin dfPlugin) {
    super(dfPlugin);
  }

  /**
   * Expand the passed in GenerateProjectins task into the requisite ProjectSupply
   * tasks - one for each resource need of this MEI/Asset determined by the
   * BG associated with the passed in supplyPGClass.
   **/
  public void expandGenerateProjections(Task gpTask, Schedule schedule, Asset consumer) {

    // TODO: Add diff based planning code
    // TODO: document, add logger, handle null checks
    // TODO: create method to compare rates and extract tasks based on time, e.g., overlapping schedules


    PropertyGroup pg = consumer.searchForPropertyGroup(dfPlugin.getSupplyClassPG());

    Collection items = getConsumed(pg);
    Collection subTasks = new ArrayList();
    for (Iterator iterator = items.iterator(); iterator.hasNext();) {
      Asset consumedItem = (Asset) iterator.next();
      Enumeration scheduleElements = schedule.getAllScheduleElements();
      Rate rate;
      // for every item consumed, walk the schedule elements and get the rates
      while (scheduleElements.hasMoreElements()) {
        ObjectScheduleElement ose = (ObjectScheduleElement) scheduleElements.nextElement();
        rate = getRate(pg, consumedItem, (List) ose.getObject());
        subTasks.add(createProjectSupplyTask(gpTask, consumer, consumedItem, ose.getStartTime(),
                                             ose.getEndTime(), rate));
      }
      createAndPublishExpansion(gpTask, subTasks);
    }
  }


  /** Create a Time Preference for the Refill Task
   *  Use a Piecewise Linear Scoring Function.
   *  For details see the IM SDD.
   *  @param bestDay The time you want this preference to represent
   *  FIXME !!!! start The earliest time this preference can have
   *  @param aspectType The AspectType of the preference- should be start_time or end_time
   *  @return Preference The new Time Preference
   **/
  private Preference createTimePreference(long bestDay, int aspectType) {
    //TODO - really need last day in theatre from an OrgActivity -
    long end = dfPlugin.getOplan().getEndDay().getTime();
    //double daysBetween = ((end - bestDay)  / thePG.getBucketMillis()) - 1;
    // TODO:  fix this what should it be?????
    double daysBetween = ((end - bestDay) / 86400000);
    //Use .0033 as a slope for now
    double late_score = .0033 * daysBetween;
    // define alpha .25
    double alpha = .25;
    Vector points = new Vector();
    // long early = TimeUtils.subtractNDays(bestDay, 1);
    TimeUtils t = dfPlugin.getTimeUtils();
    long early = t.subtractNDays(bestDay, 1);
    AspectScorePoint earliest = new AspectScorePoint(AspectValue.newAspectValue(aspectType, early), alpha);
    AspectScorePoint best = new AspectScorePoint(AspectValue.newAspectValue(aspectType, bestDay), 0.0);
//     AspectScorePoint first_late = new AspectScorePoint(getTimeUtils().addNDays(bestDay, 1),
//                                                        alpha, aspectType);
    long late = dfPlugin.getTimeUtils().addNDays(bestDay, 1);
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
   * FIXME time - used to find the OPlan and the geoloc for the TO preposition
   * @return Vector of PrepostionalPhrases
   **/
  protected Vector createPrepPhrases(Object consumer, Task parentTask) {

    Vector prepPhrases = new Vector();

    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.OFTYPE, dfPlugin.getSupplyType()));
    prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.FOR, dfPlugin.getMyOrganization()));

    GeolocLocation geoloc = getGeolocLocation(parentTask, dfPlugin.getCurrentTimeMillis());
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

    if (consumer != null) {
      MaintainedItem itemID;
      if (consumer instanceof Asset) {
        TypeIdentificationPG tip = ((Asset) consumer).getTypeIdentificationPG();
        ItemIdentificationPG iip = ((Asset) consumer).getItemIdentificationPG();
        if (iip != null) {
          itemID = MaintainedItem.findOrMakeMaintainedItem("Asset", tip.getTypeIdentification(),
                                                           iip.getItemIdentification(), tip.getNomenclature(),
                                                           dfPlugin);
        } else {
          itemID = MaintainedItem.findOrMakeMaintainedItem("Asset", tip.getTypeIdentification(),
                                                           null, tip.getNomenclature(), dfPlugin);
        }
      } else {
        itemID = MaintainedItem.findOrMakeMaintainedItem("Other", consumer.toString(), null, null, dfPlugin);
      }
      prepPhrases.addElement(newPrepositionalPhrase(Constants.Preposition.MAINTAINING, itemID));
    }

    return prepPhrases;
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


  protected void createAndPublishExpansion(Task parent, Collection subtasks) {
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      dfPlugin.publishAdd(subtasksIT.next());
    }
    Workflow wf = buildWorkflow(parent, subtasks);
    Expansion expansion = getPlanningFactory().createExpansion(parent.getPlan(), parent, wf, null);
    dfPlugin.publishAdd(expansion);
  }

  protected void addToAndPublishExpansion(Task parent, Collection subtasks) {
    Expansion expansion = (Expansion) parent.getPlanElement();
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      Task task = (Task) subtasksIT.next();
      dfPlugin.publishAdd(task);
      wf.addTask(task);
    }
    dfPlugin.publishChange(expansion);
  }

  protected void addNewTasksToExpansion(Task parentTask, Collection subtasks) {

  }

  private NewTask createProjectSupplyTask(Task parentTask, Asset consumer, Asset consumedItem, long start, long end, Rate rate) {
    NewTask newTask = getPlanningFactory().newTask();
    newTask.setParentTask(parentTask);
    newTask.setPlan(parentTask.getPlan());
    newTask.setDirectObject(consumedItem);
    newTask.setVerb(Verb.getVerb(Constants.Verb.PROJECTSUPPLY));

    Vector prefs = new Vector();
    prefs.addElement(TaskUtils.createDemandRatePreference(getPlanningFactory(), rate));
    // start and end from schedule element
    prefs.addElement(createTimePreference(start, AspectType.START_TIME));
    prefs.addElement(createTimePreference(end, AspectType.END_TIME));
    newTask.setPreferences(prefs.elements());

    Enumeration parentPhrases = parentTask.getPrepositionalPhrases();
    Vector childPhrases = createPrepPhrases(consumer, parentTask);
    if (parentPhrases.hasMoreElements()) {
      newTask.setPrepositionalPhrases(addPrepositionalPhrase(parentPhrases, childPhrases).elements());
    } else {
      newTask.setPrepositionalPhrases(childPhrases.elements());
    }

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

  private Vector addPrepositionalPhrase(Enumeration enum, Collection childPhrases) {
    Vector phrases = new Vector();
    while (enum.hasMoreElements()) {
      phrases.addElement(enum.nextElement());
    }
    phrases.addAll(childPhrases);
    return phrases;
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
}



