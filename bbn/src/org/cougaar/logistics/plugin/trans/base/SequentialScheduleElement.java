/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.trans.base;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import java.util.Date;

import java.io.Serializable;

import org.cougaar.glm.util.GLMPrepPhrase;
import org.cougaar.util.log.Logger;

import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.SubTaskResult;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScheduleElementImpl;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.glm.ldm.asset.Deck;
import org.cougaar.glm.ldm.asset.PhysicalAsset;
import org.cougaar.lib.callback.UTILAllocationListener;
import org.cougaar.lib.callback.UTILAllocationCallback;
import org.cougaar.lib.callback.UTILAssetListener;
import org.cougaar.lib.callback.UTILAssetCallback;
import org.cougaar.lib.callback.UTILExpansionListener;
import org.cougaar.lib.callback.UTILExpansionCallback;
import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILGenericListener;
import org.cougaar.lib.filter.UTILBufferingPlugin;
import org.cougaar.lib.filter.UTILBufferingPluginAdapter;
import org.cougaar.lib.util.UTILPrepPhrase;
import org.cougaar.lib.util.UTILExpand;
import org.cougaar.lib.util.UTILPluginException;
import org.cougaar.lib.util.UTILAllocationResultAggregator;
import org.cougaar.lib.util.UTILAllocate;


import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.util.log.Logger;
    
/** 
 ** The following is a base class with important utilities to handle the custom schedule
 ** elements which make up the sequential plan.
 **/
public abstract class SequentialScheduleElement extends ScheduleElementImpl {
  protected boolean planned = false;
  protected Task parentTask;
  protected Vector dependencies = new Vector();
  protected Task task;
  protected GLMPrepPhrase glmPrepHelper;
  protected Logger logger;
  
  public SequentialScheduleElement(Task parent, GLMPrepPhrase glmPrepHelper, Logger logger) {
    parentTask = parent;
    this.logger = logger;
    this.glmPrepHelper = glmPrepHelper;
  } 
	
  public Task getParentTask () { return parentTask; }
  public Task getTask    () { return task; }
  public void setTask    (Task s) { task = s; }
	
  // The dependencies are managed as a Vector of other Schedule Elements
  public void setDependencies(Vector vect) { dependencies = vect; }
  public Vector getDependencies() { return dependencies; }
	
  public boolean isPlanned() { return planned; }
	
  public boolean isReady() {
    Vector dependencies = getDependencies();
    for (int i= 0; i < dependencies.size(); i++) {
      if (!((SequentialScheduleElement)dependencies.elementAt(i)).isPlanned()) {
	return false;
      }
    }
    return true;
  }
	
  /**
   * planMe does the essential planning on a schedule element. Presumbly it will create
   * a task and allocate it. If it does not this architecture will fail
   *
   * @param plugin to call publish calls on
   * @return Task that gets created
   */
  public abstract Task planMe(SequentialPlannerPlugin plugin); 
	
	
  // A default finishPlan which simply fills date information into the schedule. Note
  // that if this is overridden planned must still be set to true.
  public void finishPlan(Allocation alloc, SequentialPlannerPlugin plugin) {
    AllocationResult AR = alloc.getReportedResult() == null ? alloc.getEstimatedResult() : alloc.getReportedResult(); 
    Double d_start = new Double(AR.getValue(AspectType.START_TIME)); 
    Double d_end = new Double(AR.getValue(AspectType.END_TIME));
    setStartDate(new Date(d_start.longValue()));
		
    double ds = d_start.doubleValue();
    double de = d_end.doubleValue ();
		
    if (de > (ds-0.1) && de < (ds+0.1)) {
      de += 1000;
      d_end = new Double (de);
      Task task = alloc.getTask();
      Asset directObject = task.getDirectObject();
      if (!glmPrepHelper.getFromLocation (task).getGeolocCode ().equals (glmPrepHelper.getToLocation (task).getGeolocCode ()))
	logger.info ("SequentialScheduleElement.finishPlan - WARNING - start = end time for task " +
		     task.getUID() + " asset " + directObject.getUID() + 
		     " from " + glmPrepHelper.getFromLocation (task) +
		     " to "   + glmPrepHelper.getToLocation (task));
    }
		
    setEndDate(new Date(d_end.longValue()));
    planned = true;
  }
}
