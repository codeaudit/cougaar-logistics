/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import java.util.*;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.AspectRate;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResultAggregator;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;

import org.cougaar.glm.ldm.plan.AlpineAspectType;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Inventory;

import org.cougaar.planning.ldm.plan.ScoringFunction;

import org.cougaar.core.plugin.util.AllocationResultHelper;
import org.cougaar.core.plugin.util.PluginHelper;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

public class SupplyExpander extends InventoryModule {

    protected static final long MSEC_PER_MIN =  60 * 1000;
    protected static final long MSEC_PER_HOUR = MSEC_PER_MIN *60;
    public static final long  DEFAULT_ORDER_AND_SHIPTIME = 24 * MSEC_PER_HOUR; // second day

    public static final Verb                 WITHDRAWVERB = new Verb(Constants.Verb.WITHDRAW);
    public static final Verb                 PROJECTWITHDRAWVERB = Constants.Verb.ProjectWithdraw;
    public static final Verb                 TRANSPORTVERB = new Verb(Constants.Verb.TRANSPORT);

    private Organization myOrg;
    protected boolean addTransport; // Add load tasks when expanding supply tasks
    private long ost;

    private ClusterIdentifier clusterId;


    public SupplyExpander(InventoryPlugin imPlugin) {
	super(imPlugin);
	ost = DEFAULT_ORDER_AND_SHIPTIME;  //In the future plugin should supply from suppliers predictor the OST - MWD
	addTransport = false;
    }

  public void initialize(Organization org) {
    myOrg = org;
    clusterId = myOrg.getClusterPG().getClusterIdentifier();
  }

    public boolean expandAndDistributeProjections(Collection tasks) {
	boolean newProjections = false;
	LogisticsInventoryPG logInvPG;
	Task aTask, wdrawTask;
	Iterator tasksIter = tasks.iterator();
	while (tasksIter.hasNext()) {
	    aTask = (Task)tasksIter.next();
	    wdrawTask = expandDemandTask(aTask, createProjectWithdrawTask(aTask));
	    logInvPG = getLogisticsInventoryPG(wdrawTask);
	    if (logInvPG != null) {
	      logInvPG.addWithdrawProjection(wdrawTask);
	      // if we have atleast one new projection - set this to true.
	      newProjections = true;
	    }
	}
	return newProjections;
    }

  public void expandAndDistributeRequisitions(Collection tasks) {
	LogisticsInventoryPG logInvPG;
	Task aTask, wdrawTask;
	Iterator tasksIter = tasks.iterator();
	while (tasksIter.hasNext()) {
	    aTask = (Task)tasksIter.next();
	    wdrawTask = expandDemandTask(aTask, createWithdrawTask(aTask));
	    logInvPG = getLogisticsInventoryPG(wdrawTask);
	    if (logInvPG != null) {
	      logInvPG.addWithdrawRequisition(wdrawTask);
	    }
	}
    }


  public LogisticsInventoryPG getLogisticsInventoryPG(Task wdrawTask) {
	LogisticsInventoryPG logInvPG = null;
	Asset asset = (Asset)wdrawTask.getDirectObject();
	Inventory inventory = inventoryPlugin.findOrMakeInventory(asset);
	logInvPG = (LogisticsInventoryPG)
	  inventory.searchForPropertyGroup(LogisticsInventoryPG.class);
	return logInvPG;
  }

    public void updateExpandedTasks(Collection changedTasks) {
	Task aTask;
	Iterator tasksIter = changedTasks.iterator();
	while (tasksIter.hasNext()) {
	    aTask = (Task)tasksIter.next();
	}
    }


  private Task expandDemandTask(Task parentTask, Task withdrawTask) {
    Vector expand_tasks = new Vector();
    expand_tasks.addElement(withdrawTask);
    NewTask transportTask = null;
    if (addTransport) {
      transportTask = createTransportTask(parentTask, withdrawTask);
      expand_tasks.addElement(transportTask);
    }
    Expansion expansion = PluginHelper.wireExpansion(parentTask, expand_tasks, inventoryPlugin.getRootFactory());
    inventoryPlugin.publishAddExpansion(expansion);
    NewWorkflow wf = (NewWorkflow) expansion.getWorkflow();
    wf.setAllocationResultAggregator(new AllocationResultAggregator.DefaultARA());
    return withdrawTask;
  }

    protected NewTask createWithdrawTask(Task parentTask) {
	
	NewTask subtask = createVanillaWithdrawTask(parentTask);

	//This method does the Supply specific stuff

        long anticipation = 0L;
        if (addTransport) anticipation += ost;

	// Add preferences for QUANTITY
	subtask.setVerb(WITHDRAWVERB);
	double quantity = getTaskUtils().getPreference(parentTask, AspectType.QUANTITY);
	Preference p_qty = createQuantityPreference(AspectType.QUANTITY, quantity);
	subtask.addPreference(p_qty);
	addEndTimePref(subtask, getTaskUtils().getEndTime(parentTask) - anticipation);

	return subtask;
     }

    protected NewTask createProjectWithdrawTask(Task parentTask) {
	
	NewTask subtask = createVanillaWithdrawTask(parentTask);

	//This method does the ProjectSupply specific stuff

        long anticipation = 0L;
        if (addTransport) anticipation += ost;

	subtask.setVerb(PROJECTWITHDRAWVERB);
	Preference pref = parentTask.getPreference(AlpineAspectType.DEMANDRATE);
	if (pref.getScoringFunction().getBest().getAspectValue() instanceof AspectRate) {
	} else {
	  logger.error("SupplyExpander DEMANDRATE preference not AspectRate:" + pref);
	}
	subtask.addPreference(pref);
	//design issue:
	//MWD do we build in ost anticipation to end time pref 
	//like above if there is a
	//PROJECTTRANSPORT in theatre transportation.
	
	return subtask;
     }

    /** creates a Withdraw task from a Supply task **/
    protected NewTask createVanillaWithdrawTask(Task parentTask) {

	// Create new task
	Asset prototype = parentTask.getDirectObject();
	NewTask subtask = inventoryPlugin.getRootFactory().newTask();
	// attach withdraw task to parent and fill it in
	subtask.setDirectObject( prototype);
	subtask.setParentTask( parentTask );
	subtask.setPlan( parentTask.getPlan() );
	subtask.setPrepositionalPhrases( parentTask.getPrepositionalPhrases() );
	subtask.setPriority(parentTask.getPriority());
	subtask.setSource( clusterId );

	// Copy all preferences that aren't used for repetitive tasks
	Vector prefs = new Vector();
	int aspect_type;
	Preference pref;
	Enumeration preferences = parentTask.getPreferences();
	while (preferences.hasMoreElements()) {
	    pref = (Preference)preferences.nextElement();
	    aspect_type = pref.getAspectType();
	    // Quanity added to withdraw by task specific method.
	    // Inerval and DemandRate are not added to withdraw task.
	    if ((aspect_type != AspectType.QUANTITY) && 
		(aspect_type != AspectType.INTERVAL) &&
		(aspect_type != AlpineAspectType.DEMANDRATE)) {
		prefs.addElement(pref);
	    }
	}
	subtask.setPreferences(prefs.elements());
	return subtask;
    }

  /** creates a Transport or ProjectTransport task from a Supply and Withdraw 
   ** or ProjectSupply and ProjectWithdraw task.
   ** Must fill in.
   **/
  protected NewTask createTransportTask(Task parentTask, Task wdraw_task) {
    return null;
  }


    /** Create a preference with the scoring NearOrAbove function at 'value' for the
	given aspect type */
    public Preference createQuantityPreference(int aspect, double value) {
	AspectValue av = new AspectValue(aspect,value);
 	ScoringFunction score = ScoringFunction.createNearOrAbove(av, 0);
	return inventoryPlugin.getRootFactory().newPreference(aspect, score);
    }
    
    public Preference createDateBeforePreference(int aspect, long value) {
	AspectValue av = new AspectValue(aspect,value);
 	ScoringFunction score = ScoringFunction.createNearOrBelow(av, 0);
	return inventoryPlugin.getRootFactory().newPreference(aspect, score);
    }

    public Preference createDateAfterPreference(int aspect, long value) {
	AspectValue av = new AspectValue(aspect,value);
 	ScoringFunction score = ScoringFunction.createNearOrAbove(av, 0);
	return inventoryPlugin.getRootFactory().newPreference(aspect, score);
    }

   /** Creates a start and end preference and attaches them to a task **/
    protected void addEndTimePref(NewTask task, long end) {
	Preference p_end = createDateBeforePreference(AspectType.END_TIME, end);
	task.addPreference(p_end);
    }

    protected void addStartTimePref(NewTask task, long start) {
 	Preference p_start = createDateAfterPreference(AspectType.START_TIME, start);
 	task.addPreference(p_start);
    }

}
    
  
  
