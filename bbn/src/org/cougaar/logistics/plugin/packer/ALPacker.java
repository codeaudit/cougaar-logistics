/*
 * <copyright>
 *  
 *  Copyright 1999-2004 Honeywell Inc
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

package org.cougaar.logistics.plugin.packer;

import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AllocationResultDistributor;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.Sortings;
import org.cougaar.util.UnaryPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Packer - handles packing supply requests
 *
 */
public abstract class ALPacker extends GenericPlugin {
  private int ADD_TASKS = 0;
  private int REMOVE_TASKS = 0;
  private double ADD_TONS = 0;
  private double REMOVE_TONS = 0;
  Map receiverToType = new HashMap();

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
        ADD_TASKS++;

        double taskWeight =
            Sizer.getTaskMass(task, getTaskQuantityUnit()).getShortTons();
        ADD_TONS += taskWeight;
        tonsReceived += taskWeight;
        tasks.add(task);

        if (getLoggingService().isInfoEnabled()) {
          getLoggingService().info("Packer: Got a task - " +
                                   task.getUID() +
                                   " from " + task.getSource() + 
				   " with " + taskWeight + 
				   " tons of ammo.");
        }
      }
    }

    if (tasks.size() == 0) {
      return;
    }

    if (getLoggingService().isInfoEnabled()) {
      getLoggingService().info("Packer - number of added SUPPLY tasks: " +
                                ADD_TASKS +
                                ", aggregated quantity from added SUPPLY tasks: " +
                                ADD_TONS + " tons, " + 
			       " this cycle got " + tasks.size () + 
			       " tasks, " + tonsReceived + 
			       " tons.");
    }

    double tonsPacked = doPacking(tasks, getSortFunction(), getPreferenceAggregator(),
                                  getAllocationResultDistributor());

    if ((tonsPacked > tonsReceived + 0.1) || (tonsPacked < tonsReceived - 0.1)) {
      if (getLoggingService().isWarnEnabled()) {
        getLoggingService().warn("Packer - received " + tonsReceived + " tons but packed " + tonsPacked +
                                 " tons, (total received " + ADD_TONS + " vs total packed "
                                 + Filler.TRANSPORT_TONS +
                                 ") for tasks : ");
        Task t = null;
        for (Iterator iter = tasks.iterator(); iter.hasNext();) {
          t = (Task) iter.next();
          getLoggingService().warn("\t" + t.getUID());
          getLoggingService().warn(" Quantity : " + t.getPreferredValue(AspectType.QUANTITY));
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
    Vector changedTaskList = new Vector();
    while (changedTasks.hasMoreElements()) {
      Task task = (Task) changedTasks.nextElement();
      changedTaskList.add(task);
      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("Packer - handling changed task - " +
                                  task.getUID() +
                                  " from " + task.getSource());
      }

      PlanElement pe = task.getPlanElement();
      if (pe != null) {
        AllocationResult ar;
        if (pe.getReportedResult() != null) {
          ar = pe.getReportedResult();
        } else {
          ar = pe.getEstimatedResult();
        }
        // make sure that we got atleast a valid reported OR estimated allocation result
        if (ar != null) {
          double taskWeight = PluginHelper.getARAspectValue(ar, AspectType.QUANTITY);
          ADD_TONS -= taskWeight;
          ADD_TASKS--;
        }
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
    }
    processNewTasks(changedTaskList.elements());
  }

  /**
   * processRemovedTasks - handle removed supply tasks
   * Called within GenericPlugin.execute.
   * **** Tasks are currently ignored ****
   */
  public void processRemovedTasks(Enumeration removedTasks) {
    boolean anyRemoved = false;
    Set toSubtract = new HashSet();
    while (removedTasks.hasMoreElements()) {
      anyRemoved = true;
      Task task = (Task) removedTasks.nextElement();

      if (getLoggingService().isInfoEnabled()) {
        getLoggingService().info("Packer: Got a removed task - " +
                                 task +
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

      /*
      Expansion exp = (Expansion) task.getPlanElement();
      if (exp == null) {
	if (getLoggingService().isInfoEnabled()) {
	  getLoggingService().info("Packer - no plan element for remove task " + task.getUID()+ " so subtracting it.");
	}
	toSubtract.add (task);
      }
      else {
	Enumeration subtaskEnum = exp.getWorkflow().getTasks();
	for (;subtaskEnum.hasMoreElements();) {
	  toSubtract.add(subtaskEnum.nextElement());
	}
      }
      */
    }

    /*
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

      // any tasks we're going to put back into milvans, don't subtract from our totals
      toSubtract.removeAll(unplannedInternal);
      handleUnplanned(unplannedInternal);

    }
    */

    Set unplannedInternal = new HashSet();
    for (Iterator iter = allInternalTasks.getCollection().iterator(); iter.hasNext(); ) {
	Task internalTask = (Task) iter.next ();
	if (internalTask.getPlanElement() == null) {
	    unplannedInternal.add (internalTask);
	}
    }

    toSubtract = new HashSet(allInternalTasks.getRemovedCollection());

    unplannedInternal.removeAll (toSubtract);
    handleUnplanned(unplannedInternal);
    toSubtract.addAll (unplannedInternal);

    if (getLoggingService().isInfoEnabled()) {
      getLoggingService().info("Packer - subtracting " + toSubtract.size () + " tasks.");
    }

    for (Iterator iter = toSubtract.iterator(); iter.hasNext(); ) {
      subtractTaskFromReceiver ((Task)iter.next());
    }
  }

  protected void handleUnplanned(Collection unplanned) {
    if (getLoggingService().isInfoEnabled())
      getLoggingService().info("Packer: found " + unplanned.size() + " tasks -- replanning them!");

    for (Iterator iter = unplanned.iterator(); iter.hasNext();) {
      Task task = (Task) iter.next();
      if (getLoggingService().isInfoEnabled()) {
	  getLoggingService().info("Packer: replanning " + task.getUID());
      }

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

  protected IncrementalSubscription allInternalTasks;

  protected void setupSubscriptions() {
    super.setupSubscriptions();
    ProportionalDistributor.DEFAULT_PROPORTIONAL_DISTRIBUTOR.setLoggingService(getLoggingService());
    /*
    unplannedInternalTasks = (IncrementalSubscription) subscribe(new UnaryPredicate() {
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

    */

    allInternalTasks = (IncrementalSubscription) subscribe(new UnaryPredicate() {
        public boolean execute(Object obj) {
	    if (obj instanceof Task) {
		Task task = (Task) obj;
		return (task.getPrepositionalPhrase(GenericPlugin.INTERNAL) != null);
	    }
	    return false;
        }
    }
							   );
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

  protected void addToReceiver (String receiverID, String type, double newQuantity) {
    Map typeToQuantity = (Map) receiverToType.get (receiverID);
    if (typeToQuantity == null) {
      receiverToType.put (receiverID, typeToQuantity=new HashMap());
    }

    Double currentQ = (Double) typeToQuantity.get (type);
    double current = 0;

    if (currentQ != null) {
      current = currentQ.doubleValue();
    }
    
    typeToQuantity.put (type, new Double (current + newQuantity));
  }

  private static final String UNKNOWN = "UNKNOWN";
  protected void subtractTaskFromReceiver (Task task) {
    TypeIdentificationPG typeIdentificationPG =
      task.getDirectObject().getTypeIdentificationPG();
    String typeID;
    if (typeIdentificationPG != null) {
      typeID = typeIdentificationPG.getTypeIdentification();
      if ((typeID == null) || (typeID.equals(""))) {
	typeID = UNKNOWN;
      }

    } else {
      typeID = UNKNOWN;
    }
    Object receiver =
      task.getPrepositionalPhrase(Constants.Preposition.FOR);

    if (receiver != null)
      receiver = ((PrepositionalPhrase) receiver).getIndirectObject();

    String receiverID;

    // Add field with recipient
    if (receiver == null) {
      receiverID = UNKNOWN;
      if (getLoggingService().isErrorEnabled()) {
        getLoggingService().error("Filler.addContentsInfo - Task " + task.getUID() + " had no FOR prep.");
      }
    } else if (receiver instanceof String) {
      receiverID = (String) receiver;
    } else if (!(receiver instanceof Asset)) {
      receiverID = UNKNOWN;
    } else {
      ItemIdentificationPG itemIdentificationPG =
	((Asset) receiver).getItemIdentificationPG();
      if ((itemIdentificationPG == null) ||
	  (itemIdentificationPG.getItemIdentification() == null) ||
	  (itemIdentificationPG.getItemIdentification().equals(""))) {
	receiverID = UNKNOWN;
      } else {
	receiverID = itemIdentificationPG.getItemIdentification();
      }
    }

    double quantity = task.getPreferredValue(AspectType.QUANTITY);
    if (getLoggingService().isInfoEnabled()) {
      getLoggingService().info("Subtracting - " + task.getUID () + " for "+
                               receiverID + " - " + typeID + " - " + quantity);
    }
    subtractFromReceiver (receiverID, typeID, quantity);
  }

  protected void subtractFromReceiver (String receiverID, String type, double newQuantity) {
    Map typeToQuantity = (Map) receiverToType.get (receiverID);
    if (typeToQuantity == null) {
      receiverToType.put (receiverID, typeToQuantity=new HashMap());
    }

    Double currentQ = (Double) typeToQuantity.get (type);
    double current = 0;

    if (currentQ != null) {
      current = currentQ.doubleValue();
    }
    
    typeToQuantity.put (type, new Double (current - newQuantity));
  }

  protected void reportQuantities () {
    for (Iterator iter = receiverToType.keySet().iterator (); iter.hasNext(); ) {
      Object receiver = iter.next();
      Map typeToQuantity = (Map) receiverToType.get(receiver);
      if (typeToQuantity == null) {
        if (getLoggingService().isErrorEnabled()) {
          getLoggingService().error("ALPacker: no type->quantity map for " +
                                    receiver);
        }
      }
      Set types = new TreeSet(typeToQuantity.keySet()); // sorted
      for (Iterator iter2 = types.iterator (); iter2.hasNext(); ) {
	Object type = iter2.next();
        if (getLoggingService().isInfoEnabled()) {
          getLoggingService().info("\t" + receiver + " : " + type + " - " +
                                   typeToQuantity.get(type));
        }
      }
    }
  }

  protected abstract Collection groupByAggregationClosure(Collection tasks);
}









