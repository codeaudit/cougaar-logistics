/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

import org.cougaar.planning.ldm.PlanningFactory;
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
import org.cougaar.core.mts.MessageAddress;

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
  
  public SequentialScheduleElement(Task parent) {
    parentTask = parent;
  } 
	
  public Task getParentTask () { return parentTask; }
  public Task getTask    () { return task; }
  public void setTask    (Task s) { task = s; }
	
  /** The dependencies are managed as a Vector of other Schedule Elements */
  public void setDependencies(Vector vect) { dependencies = vect; }
  public Vector getDependencies() { return dependencies; }
	
  public boolean isPlanned() { return planned; }

  public void unplan () { 
    planned = false; 
  }
	
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
	
	
  /**
   * A default finishPlan which simply fills date information into the schedule. Note
   * that if this is overridden planned must still be set to true.
   */
  public void finishPlan(Allocation alloc, SequentialPlannerPlugin plugin) {
    AllocationResult AR = alloc.getReportedResult() == null ? alloc.getEstimatedResult() : alloc.getReportedResult(); 
    Double d_start = new Double(AR.getValue(AspectType.START_TIME)); 
    Double d_end = new Double(AR.getValue(AspectType.END_TIME));
    setStartDate(new Date(d_start.longValue()));
		
    double ds = d_start.doubleValue();
    double de = d_end.doubleValue ();
		
    if (de > (ds-0.1) && de < (ds+0.1)) { // hack so schedule element doesn't complain about a zero length
      de += 1000;                         // but this may be an error
      d_end = new Double (de);

      reportZeroDuration (alloc, plugin);
    }
		
    setEndDate(new Date(d_end.longValue()));
    planned = true;
  }

  /** default does nothing */
  protected void reportZeroDuration (Allocation alloc, SequentialPlannerPlugin plugin) {}
}
