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

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.NewMPTask;
import org.cougaar.planning.ldm.plan.Task;

/**
 * This class is used to wrap up an aggregation's state into an
 * object that is able to generate new MPTasks on demand.  These
 * MPTasks will be used as the "right-hand-side" of Aggregations
 * constructed by the GenericPlugin.<br>
 * A standard strategy for writing AggregationClosures is to provide
 * a constructor that sets a number of instance variables. Then the newTask method
 * will use these instance variables to appropriately construct the MPTask it
 * returns.<br>
 * The name of this class is a trifle unfortunate --- it would be
 * better if it and the GenericTemplate were renamed to MPTaskTemplate
 * and WorkflowTemplate, respectively, but this has not been done in the
 * interests of backward-compatibility.
 * @see GenericPlugin
 */
public abstract class AggregationClosure {
  protected GenericPlugin _gp = null;
  protected PlanningFactory _factory = null;

  /**
   * This method will be called by aggregation and packing scripts of
   * the scripting Plugin.  It is guaranteed to be called before the
   * newTask method is called, so that writers of newTask methods may
   * feel free to use the variables _gp and _factory, that point to the
   * GenericPlugin and its PlanningFactory, respectively.
   * @see #newTask
   */
  public void setGenericPlugin(GenericPlugin gp) {
    _gp = gp;
    _factory = _gp.getGPFactory();
  }

  /**
   * Developers of aggregation and packing rules should supply an
   * AggregationClosure subclass that provides an instantiation of this
   * method.  This method should return a NewMPTask, but need not publish
   * it (this will be done by the GenericPlugin) or set its Preferences
   * (this is the job of the PreferenceAggregator.
   * @see PreferenceAggregator
   */
  public abstract NewMPTask newTask();

  /**
   * getQuantity - return the amount this container can hold
   */
  public abstract double getQuantity();

  /**
   * return true if task is valid for this AggregationClosure.
   */
  public abstract boolean validTask(Task task);
}





