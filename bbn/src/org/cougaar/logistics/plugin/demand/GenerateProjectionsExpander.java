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
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.util.PluginHelper;

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

    if (consumer instanceof AggregateAsset) {
      consumer = ((AggregateAsset) consumer).getAsset();
    }
    PropertyGroup pg = consumer.searchForPropertyGroup(dfPlugin.getSupplyClassPG());
    Collection items = getConsumed(pg);
    Collection subTasks = new ArrayList();
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
         //    String myOrgName = 
//               dfPlugin.getMyOrganization().getItemIdentificationPG().getItemIdentification();
//             if (myOrgName.indexOf("35-ARBN") >= 0) {
//               System.out.println("------------------ THE RATE is NULL ------------------------  " +
//                                  consumedItem.getTypeIdentificationPG().getNomenclature()+" - "+
//                                  consumedItem.getTypeIdentificationPG().getTypeIdentification());
//             }
            continue;
          }
          logger.info("checking Rate on consumed item " + rate.toString());
          subTasks.add(createProjectSupplyTask(gpTask, consumer, consumedItem, ose.getStartTime(),
                                               ose.getEndTime(), rate));
        }
      }
    }
     String myOrgName = 
	    dfPlugin.getMyOrganization().getItemIdentificationPG().getItemIdentification();
    if (myOrgName.indexOf("35-ARBN") >= 0) {
      System.out.println(" ----------- Size of Subtask Collection " + subTasks.size() +  " consumer "  +
                         consumer.getTypeIdentificationPG().getNomenclature()+ ", "+
                         consumer.getTypeIdentificationPG().getTypeIdentification());
    }
    if (!subTasks.isEmpty()) {
      createAndPublishExpansion(gpTask, subTasks);
    } else {
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


  private void createAndPublishExpansion(Task parent, Collection subtasks) {
    Iterator subtasksIT = subtasks.iterator();
    while (subtasksIT.hasNext()) {
      dfPlugin.publishAdd(subtasksIT.next());
    }
    Workflow wf = buildWorkflow(parent, subtasks);
    Expansion expansion = getPlanningFactory().createExpansion(parent.getPlan(), parent, wf, null);
    logger.info("GenerateProjectionsExpander publishing expansion " + dfPlugin.getClusterId());
    System.out.println("GenerateProjectionsExpander publishing expansion " + dfPlugin.getClusterId());
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
    }
    dfPlugin.publishChange(expansion);
  }


  private NewTask createProjectSupplyTask(Task parentTask, Asset consumer, Asset consumedItem, long start,
                                          long end, Rate rate) {
    logger.info("GenerateProjectionsExpander create ProjectSupply Task " + dfPlugin.getClusterId());
    System.out.println("GenerateProjectionsExpander create ProjectSupply Task ");
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

  public void updateAllocationResults(Collection planElements) {
    Iterator peIt = planElements.iterator();
    while (peIt.hasNext()) {
      PlanElement pe = (PlanElement) peIt.next();
      if (PluginHelper.updatePlanElement(pe)) {
        dfPlugin.publishChange(pe);
      }
    }
  }

}



