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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Typically, when building an aggregator, one wants to be able to
 * specify how the Preferences on the MPTask of a Container will
 * depend on the Preferences of its parent Task(s).  This interface
 * encapsulates that behavior.
 */
public interface PreferenceAggregator {
  /**
   * @return The return value of this method should be an Enumeration of
   * Preferences, suitable to be the input value of the NewTask interface's
   * setPreferences method.
   * @see org.cougaar.planning.ldm.plan.NewTask#setPreferences
   */
  ArrayList aggregatePreferences(Iterator tasks, PlanningFactory rootFactory);
}




