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
//  public static final InventoryMetric AVERAGE_DEMAND =                       new InventoryMetric("Average Demand");
  public static final InventoryMetric DUE_IN =                               new InventoryMetric("Due In");
  public static final InventoryMetric UNCONFIRMED_DUE_IN =                   new InventoryMetric("Unconfirmed Due In");
  public static final InventoryMetric PROJECTED_DUE_IN =                     new InventoryMetric("Projected Due In");
  public static final InventoryMetric MOCK_DUE_IN =                          new InventoryMetric("Projected Mock Due In");
//  public static final InventoryMetric INACTIVE_DUE_IN =                      new InventoryMetric("Inactive Due In");
//  public static final InventoryMetric INACTIVE_UNCONFIRMED_DUE_IN =          new InventoryMetric("Inactive Unconfirmed Due In");
  public static final InventoryMetric INACTIVE_PROJECTED_DUE_IN =            new InventoryMetric("Inactive Projected Due In");
  public static final InventoryMetric REQUESTED_DUE_IN =                     new InventoryMetric("Requested Due In");
  public static final InventoryMetric REQUESTED_PROJECTED_MOCK_DUE_IN =      new InventoryMetric("Projected Requested Mock Due In");
  public static final InventoryMetric PROJECTED_REQUESTED_DUE_IN =           new InventoryMetric("Projected Requested Due In");
//  public static final InventoryMetric INACTIVE_REQUESTED_DUE_IN =            new InventoryMetric("Inactive Requested Due In");
  public static final InventoryMetric INACTIVE_PROJECTED_REQUESTED_DUE_IN =  new InventoryMetric("Inactive Projected Requested Due In");
  public static final InventoryMetric DUE_OUT =                              new InventoryMetric("Due Out");
//  public static final InventoryMetric INACTIVE_DUE_OUT =                     new InventoryMetric("Inactive Due Out");
  public static final InventoryMetric PROJECTED_DUE_OUT =                    new InventoryMetric("Projected Due Out");
  public static final InventoryMetric PROJECTED_MOCK_DUE_OUT =               new InventoryMetric("Projected Mock Due Out");
  public static final InventoryMetric INACTIVE_PROJECTED_DUE_OUT =           new InventoryMetric("Inactive Projected Due Out");
  public static final InventoryMetric REQUESTED_DUE_OUT =                    new InventoryMetric("Requested Due Out");
//  public static final InventoryMetric INACTIVE_REQUESTED_DUE_OUT =           new InventoryMetric("Inactive Requested Due Out");
  public static final InventoryMetric PROJECTED_REQUESTED_DUE_OUT =          new InventoryMetric("Projected Requested Due Out");
  public static final InventoryMetric PROJECTED_REQUESTED_MOCK_DUE_OUT =     new InventoryMetric("Projected Requested Mock Due Out");
  public static final InventoryMetric INACTIVE_PROJECTED_REQUESTED_DUE_OUT = new InventoryMetric("Inactive Projected Requested Due Out");
//  public static final InventoryMetric DETAILED_INVENTORY =                   new InventoryMetric("Detailed Inventory");
  public static final InventoryMetric MOCK_INVENTORY =                       new InventoryMetric("Mock Inventory");
//  public static final InventoryMetric LABOR_SCHEDULE =                       new InventoryMetric("Labor");
//  public static final InventoryMetric DUE_OUT_LABOR =                        new InventoryMetric("Due Out Labor");

  public static Collection getValidValues() {
    return new LinkedList(validValues);
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
//           (metric == InventoryMetric.DETAILED_INVENTORY) ||
           (metric == InventoryMetric.MOCK_INVENTORY) ||
           (metric == InventoryMetric.REORDER_LEVEL) ||
           (metric == InventoryMetric.GOAL_LEVEL);
  }
}
