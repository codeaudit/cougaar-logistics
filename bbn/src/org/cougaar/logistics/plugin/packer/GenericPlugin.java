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

/* This contains the GenericPlugin class definition. */

package org.cougaar.logistics.plugin.packer;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/** Main class for the generic plugin.
 *    real work is done by processAddTask, processChangeTask, processRemoveTasks
 *    these methods called at the end of execute().
 *
 * The Generic Plugin should have three subscriptions:
 * <ol>
 * <li> To all the PlanElements it has created.  Changes to these
 *     should be handled by updating their AllocationResults
 * <li>  To tasks based on the predicate defined by getTaskPredicate.
 * <li>  A subscription to all its assets, used to find itself.
 * </ol>
 */
abstract public class GenericPlugin extends SimplePlugin {
  public static final int DEFAULT_LOGGING_LEVEL = Logger.INFO;
  public static final String DEBUG_STRING = "debug";
  public static final String INFO_STRING = "info";
  public static final String WARN_STRING = "warn";
  public static final String ERROR_STRING = "error";
  public static final String FATAL_STRING = "fatal";

  private LoggingService myLoggingService;

  /**
   * This new Preposition is used to tag tasks that are created through
   * Packing and that should not trigger subscriptions.
   */
  public static String INTERNAL = "INTERNAL";

  // The subscriptions for this plugin...
  private IncrementalSubscription myPlanElements;

  private IncrementalSubscription myAllTasks;

  /**
   * Predicate that finds all Tasks, <em>except</em> those explicitly
   * tagged as INTERNAL.  (These are created by the Sizer.)
   * @see Sizer
   */
  private UnaryPredicate myAllTaskPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        if (((Task) o).getPrepositionalPhrase(INTERNAL) == null) {
          return true;
        } else {
          return false;
        }
      }
      return false;
    }
  };

  public LoggingService getLoggingService() {
    return myLoggingService;
  }

  /**
   *
   *
   */
  public void createExpansion(Iterator tasksForWorkflow, Task parentTask) {
    // this needs to publish the Expansion, all the subtasks and
    // the workflow
    if (parentTask.getPlan() == null) {
      throw (new RuntimeException("Unable to find plan for expansion - parentTask is " + parentTask.getUID()));
    }

    NewWorkflow workflow = getFactory().newWorkflow();
    workflow.setIsPropagatingToSubtasks(true); // was true, but may lead to dangling unprocessed internal Supply tasks
    workflow.setParentTask(parentTask);

    workflow.setAllocationResultAggregator(AllocationResultAggregator.DEFAULT);

    while (tasksForWorkflow.hasNext()) {
      NewTask subTask = (NewTask) tasksForWorkflow.next();
      subTask.setWorkflow(workflow);
      workflow.addTask(subTask);

      publishAdd(subTask);
    }

    publishAdd(workflow);

    Expansion expan = getFactory().createExpansion(parentTask.getPlan(),
                                                   parentTask, workflow,
                                                   null);
    publishAdd(expan);

    return;
  }

  /** Creates <em>and publishes</em> aggregations for all of the tasks
   * in the input argument.  For robustness, catches all errors we could
   * foresee, prints error messages and returns a boolean indicating
   * success or failure.
   */
  public boolean createAggregation(Iterator tasksToAgg, MPTask childTask, Plan plan,
                                   AllocationResultDistributor distributor) {
    // Save into a vector because we want to use the enumeration in multiple
    // places
    ArrayList parentTasks = new ArrayList();
    while (tasksToAgg.hasNext()) {
      parentTasks.add(tasksToAgg.next());
    }

    if (getLoggingService().isDebugEnabled()) {
      getLoggingService().debug("GenericPlugin: Top of create Aggregation.");
    }

    // Create Composition
    NewComposition comp = getFactory().newComposition();

    comp.setCombinedTask(childTask);
    comp.setDistributor(distributor);

    if (getLoggingService().isDebugEnabled()) {
      getLoggingService().debug("GenericPlugin: Created comp.");
    }

    Iterator taskIterator = parentTasks.iterator();
    while (taskIterator.hasNext()) {
      Task parentTask = (Task) taskIterator.next();

      if (plan == null) {
        plan = parentTask.getPlan();

        if (plan == null) {
          plan = getFactory().getRealityPlan();

          if (plan == null) {
            getLoggingService().error("GenericPlugin: Unable to find plan for aggregation.");
            return false;
          }
        }
      }

      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("GenericPlugin: Top of while loop.");
      }

      Aggregation aggregation =
          getFactory().createAggregation(plan, parentTask, comp, null);

      if (getLoggingService().isInfoEnabled()) {
        getLoggingService().info("GenericPlugin: adding aggregation to " + parentTask.getUID());
      }

      if (getLoggingService().isDebugEnabled()) {
        getLoggingService().debug("GenericPlugin: built the aggregation.");
      }

      publishAdd(aggregation);

      /* BOZO - Work around for persistence bug - need to publish the
       * aggregation before adding it to the composition.  Should not be
       * required since this all occurs inside of a single transaction.
       */
      comp.addAggregation(aggregation);

    }

    if (getLoggingService().isDebugEnabled()) {
      getLoggingService().debug("GenericPlugin: After the while loop.");
    }

    publishAdd(comp);

    NewMPTask mpt = (NewMPTask) childTask;

    mpt.setPlan(plan);
    mpt.setComposition(comp);
    //BOZO !!!
    mpt.setParentTasks(new Vector(parentTasks).elements());

    publishAdd(childTask);

    return true;
  }

  /** Called from cycle when something has changed or the plugin was triggered.
   */
  protected void execute() {
    // now we handle updates to our plan elements
    if (myPlanElements != null) {
      updateAllocationResult(myPlanElements);
    } else if (getLoggingService().isDebugEnabled()) {
      getLoggingService().debug("GenericPlugin: _myPlanElements subscription is missing!");
    }

    processNewTasks(myAllTasks.getAddedList());

    processChangedTasks(myAllTasks.getChangedList());

    processRemovedTasks(myAllTasks.getRemovedList());
    
    if (myLoggingService.isInfoEnabled()) {
      myLoggingService.info("Reporting on ammo quantity per receiver per type.");
      reportQuantities ();
    }
  }

  protected void reportQuantities () {}

  /**
   * processNewTasks - called during execute to handle new tasks.
   * plugins should implement as appropriate.
   *
   * @param newTasks Enumeration of new tasks
   */
  public abstract void processNewTasks(Enumeration newTasks);

  /**
   * processChangedTasks - called during execute to handle changed tasks.
   * plugins should implement as appropriate.
   *
   * @param changedTasks Enumeration of changed tasks
   */
  public abstract void processChangedTasks(Enumeration changedTasks);

  /**
   * processRemovedTasks - called during execute to handle removed tasks.
   * plugins should implement as appropriate.
   *
   * @param removedTasks Enumeration of removed tasks
   */
  public abstract void processRemovedTasks(Enumeration removedTasks);

  /**
   * getTaskPredicate - returns task predicate for task subscription
   * Default implementation subscribes to all non-internal tasks. Derived classes
   * should probably implement a more specific version.
   *
   * @return UnaryPredicate - task predicate to be used.
   */
  public UnaryPredicate getTaskPredicate() {
    return myAllTaskPred;
  }

  /**
   * getPlanElementPredicate - returns predicate which screens for plan
   * elements which will need to have allocation results set. In this case,
   * plan elements associated with Ammunition Supply tasks
   *
   *
   * @return UnaryPredicate screens for plan elements which the packer is
   * reponsible
   */
  public abstract UnaryPredicate getPlanElementPredicate();

  /**
   * setupSubscriptions - set up subscriptions to assets, tasks, and planElements.
   * Uses predicates returned by getTaskPredicate() & getPlanElementPredicate
   */
  protected void setupSubscriptions() {
    myLoggingService =
        (LoggingService) getDelegate().getServiceBroker().getService(this, LoggingService.class, null);

    if (myLoggingService.isDebugEnabled()) {
      myLoggingService.debug("GenericPlugin: setting up subscriptions for Packer.");

      myLoggingService.debug("GenericPlugin.setupSubscriptions: didRehydrate() " +
                             didRehydrate() +
                             " _allTasks " + myAllTasks);
    }

    myAllTasks = (IncrementalSubscription) subscribe(getTaskPredicate());

    myPlanElements = (IncrementalSubscription) subscribe(getPlanElementPredicate());

  }

  /**
   * Creates an generic plugin
   */
  public GenericPlugin() {
  }

  /** Function that tests if the first argument is of a given class
   */
  public boolean testInstance(Object arg, String ty) {
    boolean test = true;

    try {
      test = (Class.forName(ty)).isInstance(arg);
    } catch (ClassNotFoundException e) {
      getLoggingService().error(e.getMessage(), e);
      test = false;
    } 

    return test;
  }

  /**
   * Provides access to a protected method.
   */
  public PlanningFactory getGPFactory() {
    return getFactory();
  }

  /**
   * Provides access to a protected method.
   */
  public MessageAddress getGPMessageAddress() {
    return getMessageAddress();
  }

  protected Collection tasksForPred(UnaryPredicate up) {
    return query(up);
  }

  protected void updateAllocationResult(IncrementalSubscription planElements) {
    PluginHelper.updateAllocationResult(planElements);
  }

  protected int getLogLevel(String logging) {
    logging = logging.toLowerCase();

    if (logging.equals(DEBUG_STRING)) {
      return (Logger.DEBUG);
    } else if (logging.equals(INFO_STRING)) {
      return (Logger.INFO);
    } else if (logging.equals(WARN_STRING)) {
      return (Logger.WARN);
    } else if (logging.equals(ERROR_STRING)) {
      return (Logger.ERROR);
    } else if (logging.equals(FATAL_STRING)) {
      return (Logger.FATAL);
    } else {
      return (DEFAULT_LOGGING_LEVEL);
    }
  }
}



