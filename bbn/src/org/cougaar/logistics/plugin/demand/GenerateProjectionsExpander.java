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
import org.cougaar.glm.ldm.plan.ObjectScheduleElement;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.glm.plugins.TaskUtils;
import org.cougaar.logistics.ldm.asset.FuelConsumerBG;
import org.cougaar.logistics.ldm.asset.FuelConsumerPG;
import org.cougaar.logistics.ldm.asset.NewFuelConsumerPG;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
    // TODO: handle adds, changes, removes as necessary
    // TODO: create method to compare rates and extract tasks based on time, e.g., overlapping schedules

    // This is a mix of temp code to capture the logic. Add
    // real code when it is available.  --llg

    // asset.getBG

//     FuelConsumerBG bg = new FuelConsumerBGImpl("foo", new Service("bar"), "shebop");
    NewFuelConsumerPG pg = 
      (NewFuelConsumerPG)getPlanningFactory().createPropertyGroup(FuelConsumerPG.class);
//     pg.setMei(anAsset);
    pg.setService("bar");
    pg.setTheater("shebop");
    FuelConsumerBG bg = new FuelConsumerBG(pg);
    pg.setFuelBG(bg);
//     pg.initialize(this);
    Collection items = bg.getConsumed();
    Collection subTasks = new ArrayList();
    for (Iterator iterator = items.iterator(); iterator.hasNext();) {
      Asset consumedItem = (Asset) iterator.next();
      Enumeration scheduleElements = schedule.getAllScheduleElements();
      Rate rate;
      // for every item consumed, walk the schedule elements and get the rates
      while (scheduleElements.hasMoreElements()) {
        ObjectScheduleElement ose = (ObjectScheduleElement) scheduleElements.nextElement();
        rate = bg.getRate(consumedItem, (List) ose.getObject());
        subTasks.add(createProjectSupplyTask(gpTask, consumedItem, consumer, ose.getStartTime(),
                                             ose.getEndTime(), rate, 0.0));
      }
      publishAddExpansion(subTasks, gpTask, consumer);
    }
  }

  private void publishAddExpansion(Collection subTasks, Task gpTask, Asset consumer) {
    // build a work flow and publish the expansion
  }


  protected Task createProjectSupplyTask(Task gpTask, Asset consumedItem,
                                   Object consumer, long start, long end, Rate rate,
                                   double multiplier)
  {

    Vector prepPhrases = createPrepPhrases();
    // TODO:  find out what the multiplier is
    Vector prefs = createPreferences(start, end, rate, multiplier);
    PlanningFactory planFactory = getPlanningFactory();
    NewTask newtask = planFactory.newTask();

    newtask.setPrepositionalPhrases(gpTask.getPrepositionalPhrases());
    newtask.setPreferences(prefs.elements());
    newtask.setDirectObject(consumedItem);
    newtask.setVerb(new Verb(Constants.Verb.PROJECTSUPPLY));
    // TODO:  What is this, need to ask.
    newtask.setCommitmentDate(new Date(end));
    newtask.setParentTask(gpTask);
    newtask.setPlan(gpTask.getPlan());
    return newtask;

  }

  private Vector createPrepPhrases() {
    return null;
  }

  private Vector createPreferences(long start, long end, Rate rate, double mult) {
    // TODO: review this and make sure it is still applicable.
    ScoringFunction score;
    Vector prefs = new Vector();
    PlanningFactory planFactory = getPlanningFactory();
    score = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.START_TIME, start));
    prefs.addElement(planFactory.newPreference(AspectType.START_TIME, score));

    score = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(AspectType.END_TIME, end));
    prefs.addElement(planFactory.newPreference(AspectType.END_TIME, score));

    prefs.addElement(TaskUtils.createDemandRatePreference(planFactory, rate));
    prefs.addElement(TaskUtils.createDemandMultiplierPreference(planFactory, mult));
    return prefs;
  }

}



