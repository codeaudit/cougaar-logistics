/*
 * <copyright>
 *  Copyright 1999-2003 Honeywell Inc.
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

package org.cougaar.logistics.plugin.packer;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AllocationResultDistributor;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.Sortings;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Packer - handles packing supply requests
 *
 */
public abstract class ALPacker extends GenericPlugin {
  private int ADD_TASKS = 0;
  private int REMOVE_TASKS = 0;
  private double ADD_TONS = 0;
  private double REMOVE_TONS = 0;

  /**
   * Packer - constructor
   */
  public ALPacker() {
    super();
  }

  /**
   * getSortFunction - returns comparator to be used in sorting the tasks to be
   * packed. Default implementation sorts on end time.
   *
   * @return Comparator
   */
  public Comparator getSortFunction() {
    return new SortByEndTime();
  }

  /**
   * getAllocationResultDistributor - returns the AllocationResultDistributor be
   * used in distributing allocation result for the transport task among the initial
   * supply tasks. Defaults to
   * ProportionalDistributor.DEFAULT_PROPORTIONAL_DISTRIBUTOR;
   *
   * @return AllocationResultDistributor
   */
  public AllocationResultDistributor getAllocationResultDistributor() {
    return ProportionalDistributor.DEFAULT_PROPORTIONAL_DISTRIBUTOR;
  }

  /**
   * getPreferenceAggregator - returns  PreferenceAggregator for setting the
   * start/end times on the transport tasks. Defaults to DefaultPreferenceAggregator.
   *
   * @return PreferenceAggregator
   */
  public PreferenceAggregator getPreferenceAggregator() {
    return new DefaultPreferenceAggregator(getAlarmService());
  }

  /**
   * getAggregationClosure - return AggregationClosure to be used for creating
   * transport tasks
   */
  public abstract AggregationClosure getAggregationClosure(ArrayList tasks);

  public int getTaskQuantityUnit() {
    return Sizer.TONS;
  }

  /**
   * processNewTasks - handle new ammo supply tasks
   * Called within GenericPlugin.execute.
   *
   * @param newTasks Enumeration of the new tasks
   */
  public void processNewTasks(Enumeration newTasks) {
    ArrayList tasks = new ArrayList();

    double tonsReceived = 0;

    while (newTasks.hasMoreElements()) {
      Task task = (Task) newTasks.nextElement();
      if (task.getPlanElement() != null) {
        getLoggingService().warn("Packer: Unable to pack - " + task.getUID() +
                                 " - task already has a PlanElement - " +
                                 task.getPlanElement() + ".\n" +
                                 "Is the UniversalAllocator also handling Supply tasks in this node?");
      } else {
        if (getLoggingService().isInfoEnabled()) {
          getLoggingService().info("Packer: Got a new task - " +
                                   task.getUID() +
                                   " from " + task.getSource());
        }
        ADD_TASKS++;

        double taskWeight =
            Sizer.getTaskMass(task, getTaskQuantityUnit()).getShortTons();
        ADD_TONS += taskWeight;
        tonsReceived += taskWeight;
        tasks.add(task);
      }
    }

    if (tasks.size() == 0) {
      return;
    }

    if (getLoggingService().isDebugEnabled()) {
      getLoggingService().debug("Packer - number of added SUPPLY tasks: " +
                                ADD_TASKS +
                                ", aggregated quantity from added SUPPLY tasks: " +
                                ADD_TONS + " tons.");
    }

    double tonsPacked = doPacking(tasks, getSortFunction(), getPreferenceAggregator(),
                                  getAllocationResultDistributor());

    if ((tonsPacked > tonsReceived + 0.1) || (tonsPacked < tonsReceived - 0.1)) {
      if (getLoggingService().isErrorEnabled()) {
        getLoggingService().warn("Packer - received " + tonsReceived + " tons but packed " + tonsPacked +
                                 " tons, (total received " + ADD_TONS + " vs total packed "
                                 + Filler.TRANSPORT_TONS +
                                 ") for tasks : ");
        Task t = null;
        for (Iterator iter = tasks.iterator(); iter.hasNext();) {
          t = (Task) iter.next();
          getLoggingService().warn("\t" + t.getUID());
          getLoggingService().warn(" Quanty : " + t.getPreferredValue(AspectType.QUANTITY));
        }
      }
    }
  }

  /**
   * processChangedTasks - handle changed supply tasks
   * Called within GenericPlugin.execute.
   * Rescind current PlanElement and reprocess tasks.
   *
   * @param changedTasks Enumeration of changed ammo supply tasks. Ignored.
   */
  public void processChangedTasks(Enumeration changedTasks) {
    while (changedTasks.hasMoreElements()) {
      Task task = (Task) changedTasks.nextElement();
      double taskWeight =
        Sizer.getTaskMass(task, getTaskQuantityUnit()).getShortTons();
      ADD_TONS -= taskWeight;
      ADD_TASKS--;

      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("Packer - handling changed task - " +
                                  task.getUID() +
                                  " from " + task.getSource());
      }

      PlanElement pe = task.getPlanElement();
      if (pe instanceof Expansion) {
        Enumeration tasks = ((Expansion)pe).getWorkflow().getTasks();
        while (tasks.hasMoreElements()) {
          Task t = (Task)tasks.nextElement();
          ((NewWorkflow)t.getWorkflow()).removeTask(t);
          publishRemove(t);
        }
      }
      publishRemove(pe);
    }
    processNewTasks(changedTasks);
  }

  /**
   * processRemovedTasks - handle removed supply tasks
   * Called within GenericPlugin.execute.
   * **** Tasks are currently ignored ****
   */
  public void processRemovedTasks(Enumeration removedTasks) {
    boolean anyRemoved = false;

    while (removedTasks.hasMoreElements()) {
      anyRemoved = true;
      Task task = (Task) removedTasks.nextElement();

      if (getLoggingService().isInfoEnabled()) {
        getLoggingService().info("Packer: Got a removed task - " +
                                 task.getUID() +
                                 " from " + task.getSource());
      }

      REMOVE_TASKS++;
      REMOVE_TONS += task.getPreferredValue(AspectType.QUANTITY);

      if (getLoggingService().isInfoEnabled()) {
        getLoggingService().info("Packer - number of removed SUPPLY tasks: " +
                                 REMOVE_TASKS +
                                 ", aggregated quantity from removed SUPPLY tasks: " +
                                 REMOVE_TONS + " tons.");
      }
    }

    if (anyRemoved) {
      Collection unplannedInternal = getBlackboardService().query(new UnaryPredicate() {
        public boolean execute(Object obj) {
          if (obj instanceof Task) {
            Task task = (Task) obj;
            return ((task.getPrepositionalPhrase(GenericPlugin.INTERNAL) != null) &&
                task.getPlanElement() == null);
          }
          return false;
        }
      }
      );

      handleUnplanned(unplannedInternal);
    }
  }

  protected void handleUnplanned(Collection unplanned) {
    if (getLoggingService().isInfoEnabled())
      getLoggingService().info("Packer: found " + unplanned.size() + " tasks -- replanning them!");

    for (Iterator iter = unplanned.iterator(); iter.hasNext();) {
      Task task = (Task) iter.next();
      ArrayList copy = new ArrayList();
      copy.add(task);
      AggregationClosure ac = getAggregationClosure(copy);

      Filler fil = new Filler(null, this, ac, getAllocationResultDistributor(),
                              getPreferenceAggregator());

      fil.handleUnplanned(task);
    }
  }

  /**
   * doPacking - packs specified set of supply tasks.
   * Assumes that it's called within an open/close transaction.
   * @param tasks ArrayList with the tasks which should be packed
   * @param sortfun BinaryPredicate to be used in sorting the tasks
   * @param prefagg PreferenceAggregator for setting the start/end times on the
   * transport tasks.
   * @param ard AllocationResultDistributor to be used in distributing allocation results
   * for the transport task amount the initial supply tasks.    *
   */
  protected double doPacking(ArrayList tasks,
                             Comparator sortfun,
                             PreferenceAggregator prefagg,
                             AllocationResultDistributor ard) {

    // Divide into 'pack together'  groups
    Collection packGroups = groupByAggregationClosure(tasks);

    double totalPacked = 0;

    for (Iterator iterator = packGroups.iterator(); iterator.hasNext();) {
      ArrayList packList = (ArrayList) iterator.next();
      // sort them, if appropriate
      if (sortfun != null) {
        packList = (ArrayList) Sortings.sort(packList, sortfun);
      }

      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("Packer: about to build the sizer in doPacking.");
      }

      AggregationClosure ac = getAggregationClosure(packList);

      // now we set the double wheel going...
      Sizer sz = new Sizer(packList, this, getTaskQuantityUnit());

      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("Packer: about to build the filler in doPacking.");
      }

      Filler fil = new Filler(sz, this, ac, ard, prefagg);

      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("Packer: about to run the wheelz in doPacking.");
      }

      totalPacked += fil.execute();
    }

    return totalPacked;
  }

  protected void setupSubscriptions() {
    super.setupSubscriptions();
    ProportionalDistributor.DEFAULT_PROPORTIONAL_DISTRIBUTOR.setLoggingService(getLoggingService());
  }

  protected void updateAllocationResult(IncrementalSubscription planElements) {
    // Make sure that quantity preferences get returned on the allocation
    // results. Transport thread may not have filled them in.
    Enumeration changedPEs = planElements.getChangedList();
    while (changedPEs.hasMoreElements()) {
      PlanElement pe = (PlanElement) changedPEs.nextElement();

      // Only update the plan element if this is a change to the reported
      // result.
      if (PluginHelper.checkChangeReports(planElements.getChangeReports(pe),
                                          PlanElement.ReportedResultChangeReport.class) &&
          PluginHelper.updatePlanElement(pe)) {
        boolean needToCorrectQuantity = false;

        AllocationResult estimatedAR = pe.getEstimatedResult();
        double prefValue =
            pe.getTask().getPreference(AspectType.QUANTITY).getScoringFunction().getBest().getAspectValue().getValue();

        AspectValue[] aspectValues = estimatedAR.getAspectValueResults();

        // Possibly need to add quantity to list of aspects if it's not there in the first place.
        // Couldn't see that this was in fact necessary so leaving it out for the moment
        // Gordon Vidaver 08/23/02

        boolean foundQuantity = false;
        for (int i = 0; i < aspectValues.length; i++) {
          if (aspectValues[i].getAspectType() == AspectType.QUANTITY) {
            if (aspectValues[i].getValue() != prefValue) {
              // set the quantity to be the preference quantity
              aspectValues[i] = aspectValues[i].dupAspectValue(prefValue);
              needToCorrectQuantity = true;
            }
            foundQuantity = true;
            break;
          }
        }

        if (!foundQuantity) {
          AspectValue[] copy = new AspectValue[aspectValues.length + 1];
          System.arraycopy(aspectValues, 0, copy, 0, aspectValues.length);
          copy[aspectValues.length] = AspectValue.newAspectValue(AspectType.QUANTITY, prefValue);
          aspectValues = copy;
        }

        if (needToCorrectQuantity) {
          if (getLoggingService().isDebugEnabled()) {
            getLoggingService().debug("Packer.updateAllocationResult - fixing quantity on estimated AR of pe " + pe.getUID());
          }

          AllocationResult correctedAR =
              new AllocationResult(estimatedAR.getConfidenceRating(),
                                   estimatedAR.isSuccess(),
                                   aspectValues);

          pe.setEstimatedResult(correctedAR);
        }

        publishChange(pe);
      }
    }
  }

  /**
   * SortByEndTime - sorts tasks by end date, earliest first
   */
  private class SortByEndTime implements Comparator {

    /*
     * compare - compares end date of the 2 tasks.
     * Compares its two arguments for order. Returns a negative integer, zero, or a
     * positive integer as the first argument is less than, equal
     * to, or greater than the second.
     */
    public int compare(Object first, Object second) {
      Task firstTask = null;
      Task secondTask = null;

      if (first instanceof Task) {
        firstTask = (Task) first;
      }

      if (second instanceof Task) {
        secondTask = (Task) second;
      }

      if ((firstTask == null) &&
          (secondTask == null)) {
        return 0;
      } else if (firstTask == null) {
        return -1;
      } else if (secondTask == null) {
        return 1;
      } else {
        return (firstTask.getPreferredValue(AspectType.END_TIME) >
            secondTask.getPreferredValue(AspectType.END_TIME)) ? 1 : -1;
      }
    }

    /**
     * Indicates whether some other object is "equal to" this Comparator.
     * This method must obey the general contract of Object.equals(Object).
     * Additionally, this method can return true only if the specified Object is
     * also a comparator and it imposes the same ordering as this comparator. Thus,
     * comp1.equals(comp2) implies that sgn(comp1.compare(o1,
     * o2))==sgn(comp2.compare(o1, o2)) for every object reference o1 and o2.
     */
    public boolean equals(Object o) {
      return (o.getClass() == SortByEndTime.class);
    }
  }

  protected abstract Collection groupByAggregationClosure(Collection tasks);
}









