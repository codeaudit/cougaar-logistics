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





