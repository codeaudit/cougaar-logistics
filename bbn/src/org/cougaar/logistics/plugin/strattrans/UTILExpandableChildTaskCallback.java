/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.strattrans;

import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILGenericListener;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import org.cougaar.util.log.Logger;

/**
 * For use with (threaded?) expanders.
 * 
 * Filters for tasks without plan elements.
 *    This allows tasks which may be the child of another task
 *     (e.g., DetermineRequirement tasks that are child of GLS)
 *    (unlike UTILExpandableTaskCallback which also filters tasks with no workflow)
 */

public class UTILExpandableChildTaskCallback extends UTILExpandableTaskCallback {
  public UTILExpandableChildTaskCallback (UTILGenericListener listener, Logger logger) {
    super (listener, logger);
  }

  /** 
   * overridden to ignore workflow
   *
   * Just calls plugin interestingTask method.
   *
   * BUG FIX (deployment date perturbation bug) :
   *
   * Does NOT skip tasks that already have a plan element - this
   * will be true on rehydration.  Including the check means no tasks in the 
   * subscription and nothing to replan, should the org activity change.
   *
   * The plugin checks to see if the task already has a plan element and won't replan
   * unless something else has changed.
   *
   * @see org.cougaar.logistics.plugin.strattrans.StrategicTransportProjectorPlugin#handleTask
   * @see org.cougaar.logistics.plugin.strattrans.StrategicTransportProjectorPlugin#redoTasks
   * @see org.cougaar.logistics.plugin.strattrans.StrategicTransportProjectorPlugin#interestingTask
   */
  protected UnaryPredicate getPredicate () {
    return new UnaryPredicate() {
	public boolean execute(Object o) {
	  if ( o instanceof Task ) {
	    return (((UTILGenericListener) myListener).interestingTask ((Task) o)); 
	  }
	  return false;
	}
      };
  }
}        
