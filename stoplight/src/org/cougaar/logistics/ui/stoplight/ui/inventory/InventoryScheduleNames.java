/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.awt.*;
import javax.swing.*;

import java.util.*;

import org.cougaar.mlm.ui.data.UISimpleNamedScheduleNames;
import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;

public class  InventoryScheduleNames implements PlotColors, UISimpleNamedScheduleNames
{
  public static final String BATCHED_HEADER = "Batched: ";


  public static final String ON_HAND = "On Hand";

  public static final String DUE_IN = "Approved Due In";
  public static final String PROJECTED_DUE_IN = "Projected Due In";
  public static final String REQUESTED_DUE_IN = "Resupply Requisitions";
  public static final String PROJECTED_REQUESTED_DUE_IN = "Projected Requisitions";

  public static final String DUE_OUT = "Approved Due Out";
  public static final String PROJECTED_DUE_OUT = "Projected Due Out";
  public static final String REQUESTED_DUE_OUT = "Requisitions";
  public static final String PROJECTED_REQUESTED_DUE_OUT = "Projections";


  // Calculated
  public static final String SHORTFALL_DUE_IN = "Shortfall (Due In)";
  public static final String SHORTFALL_DUE_OUT = "Shortfall (Due Out)";
  public static final String TARGET_LEVEL = "Target Level";
  public static final String CRITICAL_LEVEL = "Critical Level";


  // Not displayed
  public static final String UNCONFIRMED_DUE_IN = "Unconfirmed Due In";


  public static final String INVENTORY_STATUS = "Inventory Status";
  public static final String CONSUMER = "Consumer";
  public static final String SUPPLIER = "Supplier";
  
  //  new streams
  public final static String ALLOCATED = "Allocated";
  public final static String AVAILABLE = "Available";
	public final static String TOTAL_LABOR = "Labor";
	public final static String TOTAL_LABOR_8 = "Labor 8 Hours/Day";
	public final static String TOTAL_LABOR_10 = "Labor 10 Hours/Day";
	public final static String TOTAL_LABOR_12 = "Labor 12 Hours/Day";
	
	public final static String GOAL_LEVEL = "Goal Level";
	public final static String REORDER_LEVEL = "Reorder Level";
	public final static String AVERAGE_DEMAND_LEVEL = "Average Demand";
	//public final static String ON_HAND_MOCK_PERIOD = "On Hand w/Mock Period"; 
	public final static String ON_HAND_MOCK_PERIOD = "On Hand w/Mock Period";   
	public final static String PROJECTED_DUE_IN_MOCK_PERIOD = "Projected Due In w/Mock Period";
	public final static String PROJECTED_DUE_OUT_MOCK_PERIOD = "Requested Due Out w/Mock Period";
	public final static String REQUESTED_DUE_OUT_MOCK_PERIOD = "Requested Due Out w/Mock Period";
	public final static String PROJECTED_REQUESTED_DUE_OUT_MOCK_PERIOD = "Projected Requested Due Out w/Mock Period";
	public final static String PROJECTED_REQUESTED_DUE_IN_MOCK_PERIOD = "Projected Requested Due In w/Mock Period";
   

public final static String INACTIVE = "_INACTIVE"; // Suffix for inactive schedules
  


  public static String getName(String scheduleName)
  {
  	
    if (scheduleName.equals(UISimpleNamedScheduleNames.DUE_IN))
    {
      return(DUE_IN);
    }
    else if (scheduleName.equals(UISimpleNamedScheduleNames.PROJECTED_DUE_OUT_MOCK_PERIOD))
    {
      return(PROJECTED_DUE_OUT);
    }
    //else if (scheduleName.equals(UISimpleNamedScheduleNames.PROJECTED_DUE_IN))
    //{
    //  return(PROJECTED_DUE_IN);
    //}
    else if (scheduleName.equals(UISimpleNamedScheduleNames.REQUESTED_DUE_IN))
    {
      return(REQUESTED_DUE_IN);
    }
    //else if (scheduleName.equals(UISimpleNamedScheduleNames.PROJECTED_REQUESTED_DUE_IN))
    //{
    //  return(PROJECTED_REQUESTED_DUE_IN);
    //}
    //else if (scheduleName.equals(UISimpleNamedScheduleNames.ON_HAND))
    //{
    //  return(ON_HAND);
    //}
    else if (scheduleName.equals(UISimpleNamedScheduleNames.DUE_OUT))
    {
      return(DUE_OUT);
    }
    //else if (scheduleName.equals(UISimpleNamedScheduleNames.PROJECTED_DUE_OUT))
    //{
      //return(PROJECTED_DUE_OUT);
    //}
    else if (scheduleName.equals(UISimpleNamedScheduleNames.REQUESTED_DUE_OUT))
    {
      return(REQUESTED_DUE_OUT);
    }
    //else if (scheduleName.equals(UISimpleNamedScheduleNames.PROJECTED_REQUESTED_DUE_OUT))
    //{
    //  return(PROJECTED_REQUESTED_DUE_OUT);
    //}
    else if (scheduleName.equals(SHORTFALL_DUE_IN))
    {
      return(SHORTFALL_DUE_IN);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_OUT))
    {
      return(SHORTFALL_DUE_OUT);
    }
    else if (scheduleName.equals(TARGET_LEVEL))
    {
      return(TARGET_LEVEL);
    }
    else if (scheduleName.equals(CRITICAL_LEVEL))
    {
      return(CRITICAL_LEVEL);
    }
    else if (scheduleName.equals(ALLOCATED))
    {
      return(ALLOCATED);
    }
    else if (scheduleName.equals(AVAILABLE))
    {
      return(AVAILABLE);
    }
    else if (scheduleName.equals(TOTAL_LABOR))
    {
      return(TOTAL_LABOR);
    }
    else if (scheduleName.equals(TOTAL_LABOR_8))
    {
      return(TOTAL_LABOR_8);
    }
    else if (scheduleName.equals(TOTAL_LABOR_10))
    {
      return(TOTAL_LABOR_10);
    }
    else if (scheduleName.equals(TOTAL_LABOR_12))
    {
      return(TOTAL_LABOR_12);
    }
    else if (scheduleName.equals(GOAL_LEVEL))
    {
      return(GOAL_LEVEL);
    }
    else if (scheduleName.equals(REORDER_LEVEL))
    {
      return(REORDER_LEVEL);
    }
    else if (scheduleName.equals(AVERAGE_DEMAND_LEVEL))
    {
      return(AVERAGE_DEMAND_LEVEL);
    }
    else if (scheduleName.equals(ON_HAND_MOCK_PERIOD))
    {
      return(ON_HAND);
    }
    else if (scheduleName.equals(PROJECTED_DUE_IN_MOCK_PERIOD))
    {
      return(PROJECTED_DUE_IN);
    }
    //else if (scheduleName.equals(REQUESTED_DUE_OUT_MOCK_PERIOD))
    //{
    //  return(REQUESTED_DUE_OUT);
    //}
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_OUT_MOCK_PERIOD))
    {
      return(PROJECTED_REQUESTED_DUE_OUT);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_IN_MOCK_PERIOD))
    {
      return(PROJECTED_REQUESTED_DUE_IN);
    }
    else if (scheduleName.equals(INACTIVE))
    {
      return(INACTIVE);
    }
    else if(scheduleName.equals(UNCONFIRMED_DUE_IN))
      return UNCONFIRMED_DUE_IN;

    return(null);
  }

  public static String getGroup(String scheduleName)
  {
    if (scheduleName.equals(ON_HAND))
    {
      return(INVENTORY_STATUS);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_IN))
    {
      return(INVENTORY_STATUS);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_OUT))
    {
      return(INVENTORY_STATUS);
    }
    else if (scheduleName.equals(TARGET_LEVEL))
    {
      return(INVENTORY_STATUS);
    }
    else if (scheduleName.equals(CRITICAL_LEVEL))
    {
      return(INVENTORY_STATUS);
    }
    else if (scheduleName.equals(DUE_IN))
    {
      return(SUPPLIER);
    }
    else if (scheduleName.equals(PROJECTED_DUE_IN))
    {
      return(SUPPLIER);
    }
    else if (scheduleName.equals(REQUESTED_DUE_IN))
    {
      return(SUPPLIER);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_IN))
    {
      return(SUPPLIER);
    }
    else if (scheduleName.equals(DUE_OUT))
    {
      return(CONSUMER);
    }
    else if (scheduleName.equals(PROJECTED_DUE_OUT))
    {
      return(CONSUMER);
    }
    else if (scheduleName.equals(REQUESTED_DUE_OUT))
    {
      return(CONSUMER);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_OUT))
    {
      return(CONSUMER);
    }
    else if (scheduleName.startsWith(BATCHED_HEADER))
    {
      return(getGroup(scheduleName.substring(BATCHED_HEADER.length())));
    }

    return(null);
  }

  public static DataSet buildDataSet(String scheduleName, String group, double[] data) throws Exception
  {
    PolygonFillableDataSet dataSet = null;

    if (scheduleName.equals(DUE_IN))
    {
      dataSet = new BarDataSet(data, data.length/2, true, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
    }
    else if (scheduleName.equals(PROJECTED_DUE_IN))
    {
      dataSet = new BarDataSet(data, data.length/2, false, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
      ((PolygonFillableDataSet)dataSet).polygonFill = true;
      ((PolygonFillableDataSet)dataSet).useFillPattern = true;
    }
    else if (scheduleName.equals(REQUESTED_DUE_IN))
    {
      dataSet = new BarDataSet(data, data.length/2, true, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_IN))
    {
      dataSet = new BarDataSet(data, data.length/2, false, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
      ((PolygonFillableDataSet)dataSet).polygonFill = true;
      ((PolygonFillableDataSet)dataSet).useFillPattern = true;
    }
    else if (scheduleName.equals(ON_HAND))
    {
      dataSet = new StepDataSet(data, data.length/2, true);
      ((StepDataSet)dataSet).endPointLead = 60L*60L*24L;
    }
    else if (scheduleName.equals(DUE_OUT))
    {
      dataSet = new BarDataSet(data, data.length/2, true, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
    }
    else if (scheduleName.equals(PROJECTED_DUE_OUT))
    {
      dataSet = new BarDataSet(data, data.length/2, false, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
      ((PolygonFillableDataSet)dataSet).polygonFill = true;
      ((PolygonFillableDataSet)dataSet).useFillPattern = true;
    }
    else if (scheduleName.equals(REQUESTED_DUE_OUT))
    {
      dataSet = new BarDataSet(data, data.length/2, true, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_OUT))
    {
      dataSet = new BarDataSet(data, data.length/2, false, InventoryTableModel.barWidth);
      if(!InventoryChartUI.tableView)
        dataSet.visible = false;
      else
        dataSet.visible = true;
      ((PolygonFillableDataSet)dataSet).polygonFill = true;
      ((PolygonFillableDataSet)dataSet).useFillPattern = true;
    }
    else if (scheduleName.equals(SHORTFALL_DUE_IN))
    {
      dataSet = new PolygonFillableDataSet(data, data.length/2, false);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_OUT))
    {
      dataSet = new PolygonFillableDataSet(data, data.length/2, false);
    }
    else if (scheduleName.equals(TARGET_LEVEL))
    {
      dataSet = new PolygonFillableDataSet(data, data.length/2, false);
    }
    else if (scheduleName.equals(CRITICAL_LEVEL))
    {
      dataSet = new PolygonFillableDataSet(data, data.length/2, false);
    }
    else if (scheduleName.startsWith(BATCHED_HEADER))
    {
      String batchedName = scheduleName.substring(BATCHED_HEADER.length());
      dataSet = new BarDataSet(data, data.length/2, true, InventoryTableModel.barWidth);

      // With 2 data set positions within a day, we want to center both data sets within the day marker range
      double offset = ((1000.0*60.0*60.0*24.0)/InventoryTableModel.timeScale - InventoryTableModel.barWidth*2)/3;

      if (batchedName.equals(InventoryScheduleNames.DUE_IN))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = false;
        dataSet.xValueOffset = offset + InventoryTableModel.barWidth/2.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.PROJECTED_DUE_IN))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = true;
        dataSet.xValueOffset = offset + InventoryTableModel.barWidth/2.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.REQUESTED_DUE_IN))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = false;
        dataSet.xValueOffset = offset*2.0 + InventoryTableModel.barWidth/2.0*3.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.PROJECTED_REQUESTED_DUE_IN))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = true;
        dataSet.xValueOffset = offset*2.0 + InventoryTableModel.barWidth/2.0*3.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.DUE_OUT))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = false;
        dataSet.xValueOffset = offset + InventoryTableModel.barWidth/2.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.PROJECTED_DUE_OUT))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = true;
        dataSet.xValueOffset = offset + InventoryTableModel.barWidth/2.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.REQUESTED_DUE_OUT))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = false;
        dataSet.xValueOffset = offset*2.0 + InventoryTableModel.barWidth/2.0*3.0;
      }
      else if (batchedName.equals(InventoryScheduleNames.PROJECTED_REQUESTED_DUE_OUT))
      {
        dataSet.polygonFill = true;
        dataSet.useFillPattern = true;
        dataSet.xValueOffset = offset*2.0 + InventoryTableModel.barWidth/2.0*3.0;
      }
    }

    if (dataSet != null)
    {
      dataSet.dataName = scheduleName;
      dataSet.dataGroup = group;
      dataSet.linecolor = getColor(scheduleName);
      dataSet.colorNumber = getColorNumber(scheduleName);
      dataSet.automaticallySetColor = (dataSet.linecolor == null);
    }

    return(dataSet);
  }

  private static Color getColor(String scheduleName)
  {
    if (scheduleName.equals(DUE_IN))
    {
//      return(midnightBlue);
      return(null);
    }
    else if (scheduleName.equals(PROJECTED_DUE_IN))
    {
//      return(midnightBlue);
      return(null);
    }
    else if (scheduleName.equals(REQUESTED_DUE_IN))
    {
//      return(darkGreen);
      return(null);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_IN))
    {
//      return(darkGreen);
      return(null);
    }
    else if (scheduleName.equals(ON_HAND))
    {
//      return(darkYellow);
      return(null);
    }
    else if (scheduleName.equals(DUE_OUT))
    {
//      return(rust);
      return(null);
    }
    else if (scheduleName.equals(PROJECTED_DUE_OUT))
    {
//      return(rust);
      return(null);
    }
    else if (scheduleName.equals(REQUESTED_DUE_OUT))
    {
//      return(darkPurple);
      return(null);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_OUT))
    {
//      return(darkPurple);
      return(null);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_IN))
    {
      return(orange);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_OUT))
    {
      return(red);
    }
    else if (scheduleName.equals(TARGET_LEVEL))
    {
      return(Color.green);
    }
    else if (scheduleName.equals(CRITICAL_LEVEL))
    {
      return(Color.blue);
    }
    else if (scheduleName.startsWith(BATCHED_HEADER))
    {
      return(getColor(scheduleName.substring(BATCHED_HEADER.length())));
    }

    return(null);
  }

  private static int getColorNumber(String scheduleName)
  {
    if (scheduleName.equals(DUE_IN))
    {
      return(0);
    }
    else if (scheduleName.equals(PROJECTED_DUE_IN))
    {
      return(0);
    }
    else if (scheduleName.equals(REQUESTED_DUE_IN))
    {
      return(1);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_IN))
    {
      return(1);
    }
    else if (scheduleName.equals(ON_HAND))
    {
      return(2);
    }
    else if (scheduleName.equals(DUE_OUT))
    {
      return(3);
    }
    else if (scheduleName.equals(PROJECTED_DUE_OUT))
    {
      return(3);
    }
    else if (scheduleName.equals(REQUESTED_DUE_OUT))
    {
      return(4);
    }
    else if (scheduleName.equals(PROJECTED_REQUESTED_DUE_OUT))
    {
      return(4);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_IN))
    {
      return(-1);
    }
    else if (scheduleName.equals(SHORTFALL_DUE_OUT))
    {
      return(-1);
    }
    else if (scheduleName.equals(TARGET_LEVEL))
    {
      return(-1);
    }
    else if (scheduleName.equals(CRITICAL_LEVEL))
    {
      return(-1);
    }
    else if (scheduleName.startsWith(BATCHED_HEADER))
    {
      return(getColorNumber(scheduleName.substring(BATCHED_HEADER.length())));
    }

    return(-1);
  }

  private static boolean alwaysVisible(String scheduleName, String chartGroup)
  {
    boolean always = false;

    if (scheduleName.equals(ON_HAND))
    {
      always = true;
    }
    else if (scheduleName.equals(SHORTFALL_DUE_IN))
    {
      always = true;
    }
    else if (scheduleName.equals(SHORTFALL_DUE_OUT))
    {
      always = true;
    }
    else if (scheduleName.equals(TARGET_LEVEL))
    {
      always = true;
    }
    else if (scheduleName.equals(CRITICAL_LEVEL))
    {
      always = true;
    }
    else if (chartGroup.equals(SUPPLIER) || chartGroup.equals(CONSUMER))
    {
      always = true;
    }
    else if (scheduleName.startsWith(BATCHED_HEADER))
    {
      return(alwaysVisible(scheduleName.substring(BATCHED_HEADER.length()), chartGroup));
    }

    return(always);
  }

  public static Hashtable buildLabelIconList()
  {
    Hashtable list = new Hashtable(1);
    double[] fakeData = new double[] {0.0, 0.0};
    try
    {
      // INVENTORY_STATUS
      addToLabelList(list, buildDataSet(ON_HAND, getGroup(ON_HAND), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(SHORTFALL_DUE_IN, getGroup(SHORTFALL_DUE_IN), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(SHORTFALL_DUE_OUT, getGroup(SHORTFALL_DUE_OUT), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(TARGET_LEVEL, getGroup(TARGET_LEVEL), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(CRITICAL_LEVEL, getGroup(CRITICAL_LEVEL), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(DUE_IN, getGroup(DUE_IN), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(PROJECTED_DUE_IN, getGroup(PROJECTED_DUE_IN), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(REQUESTED_DUE_IN, getGroup(REQUESTED_DUE_IN), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(PROJECTED_REQUESTED_DUE_IN, getGroup(PROJECTED_REQUESTED_DUE_IN), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(DUE_OUT, getGroup(DUE_OUT), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(PROJECTED_DUE_OUT, getGroup(PROJECTED_DUE_OUT), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(REQUESTED_DUE_OUT, getGroup(REQUESTED_DUE_OUT), fakeData), INVENTORY_STATUS);
      addToLabelList(list, buildDataSet(PROJECTED_REQUESTED_DUE_OUT, getGroup(PROJECTED_REQUESTED_DUE_OUT), fakeData), INVENTORY_STATUS);

      // SUPPLIER
      addToLabelList(list, buildDataSet(DUE_IN, getGroup(DUE_IN), fakeData), SUPPLIER);
      addToLabelList(list, buildDataSet(PROJECTED_DUE_IN, getGroup(PROJECTED_DUE_IN), fakeData), SUPPLIER);
      addToLabelList(list, buildDataSet(REQUESTED_DUE_IN, getGroup(REQUESTED_DUE_IN), fakeData), SUPPLIER);
      addToLabelList(list, buildDataSet(PROJECTED_REQUESTED_DUE_IN, getGroup(PROJECTED_REQUESTED_DUE_IN), fakeData), SUPPLIER);

      // CONSUMER
      addToLabelList(list, buildDataSet(DUE_OUT, getGroup(DUE_OUT), fakeData), CONSUMER);
      addToLabelList(list, buildDataSet(PROJECTED_DUE_OUT, getGroup(PROJECTED_DUE_OUT), fakeData), CONSUMER);
      addToLabelList(list, buildDataSet(REQUESTED_DUE_OUT, getGroup(REQUESTED_DUE_OUT), fakeData), CONSUMER);
      addToLabelList(list, buildDataSet(PROJECTED_REQUESTED_DUE_OUT, getGroup(PROJECTED_REQUESTED_DUE_OUT), fakeData), CONSUMER);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    return(list);
  }

  public static void addToLabelList(Hashtable list, DataSet dataSet, String chartGroup)
  {
    Hashtable labelList = (Hashtable)list.get(chartGroup);
    if (labelList == null)
    {
      labelList = new Hashtable(1);
      list.put(chartGroup, labelList);
    }

    labelList.put(dataSet.dataName, new LabelIcon(dataSet, alwaysVisible(dataSet.dataName, chartGroup)));
  }
}
