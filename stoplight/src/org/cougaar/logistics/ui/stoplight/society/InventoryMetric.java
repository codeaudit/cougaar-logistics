/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.society;

import java.util.Collection;
import java.util.LinkedList;

import org.cougaar.lib.aggagent.util.Enum;

public class InventoryMetric extends Enum {
  private static final Collection validValues = new LinkedList();

  /*
  *** These strings used in the metric name constructors MUST match the method names used in InventoryWrapper.  The format is
  ***  get<Metric Name w/o spaces>Schedule(). Alternatively, a mapping can be created in the static initialization section of
  ***  ExtractionHelper
   */
  public static final InventoryMetric INVENTORY =                            new InventoryMetric("Inventory");
  public static final InventoryMetric REORDER_LEVEL =                        new InventoryMetric("Reorder Level");
  public static final InventoryMetric GOAL_LEVEL =                           new InventoryMetric("Goal Level");
  public static final InventoryMetric DUE_IN =                               new InventoryMetric("Due In");
  public static final InventoryMetric UNCONFIRMED_DUE_IN =                   new InventoryMetric("Unconfirmed Due In");
  public static final InventoryMetric PROJECTED_DUE_IN =                     new InventoryMetric("Projected Due In");
  public static final InventoryMetric INACTIVE_PROJECTED_DUE_IN =            new InventoryMetric("Inactive Projected Due In");
  public static final InventoryMetric REQUESTED_DUE_IN =                     new InventoryMetric("Requested Due In");
  public static final InventoryMetric PROJECTED_REQUESTED_DUE_IN =           new InventoryMetric("Projected Requested Due In");
  public static final InventoryMetric INACTIVE_PROJECTED_REQUESTED_DUE_IN =  new InventoryMetric("Inactive Projected Requested Due In");
  public static final InventoryMetric DUE_OUT =                              new InventoryMetric("Due Out");
  public static final InventoryMetric PROJECTED_DUE_OUT =                    new InventoryMetric("Projected Due Out");
  public static final InventoryMetric INACTIVE_PROJECTED_DUE_OUT =           new InventoryMetric("Inactive Projected Due Out");
  public static final InventoryMetric REQUESTED_DUE_OUT =                    new InventoryMetric("Requested Due Out");
  public static final InventoryMetric PROJECTED_REQUESTED_DUE_OUT =          new InventoryMetric("Projected Requested Due Out");
  public static final InventoryMetric INACTIVE_PROJECTED_REQUESTED_DUE_OUT = new InventoryMetric("Inactive Projected Requested Due Out");

  public static Collection getValidValues() {
    return new LinkedList(validValues);
  }

  protected String getStringObject(String enumName)
  {
    Enum enum = (Enum) findEnum(validValues, enumName);
    return enum == null ? null : enum.toString();
  }

  public static InventoryMetric fromString (String enumName) {
    return (InventoryMetric)findEnum(validValues, enumName);
  }

  private InventoryMetric (String name) {
    super(name);
    validValues.add(this);
  }

  public static boolean usesItemUnits(String metric)
  {
    return usesItemUnits(fromString(metric));
  }

  public static boolean usesItemUnits(InventoryMetric metric)
  {
    return (metric == InventoryMetric.INVENTORY) ||
           (metric == InventoryMetric.REORDER_LEVEL) ||
           (metric == InventoryMetric.GOAL_LEVEL);
  }
}
