/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory;

import java.util.Date;


import java.awt.Color;
import java.awt.Insets;


import com.klg.jclass.chart.JCChart;
import com.klg.jclass.chart.JCPickListener;
import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.util.swing.JCExitFrame;
import com.klg.jclass.chart.JCSymbolStyle;

import com.klg.jclass.chart.ChartDataView;
import com.klg.jclass.chart.ChartDataViewSeries;
import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.ChartDataEvent;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.plugin.inventory.TimeUtils;

import com.klg.jclass.util.legend.JCLegend;
import com.klg.jclass.util.legend.JCMultiColLegend;

import org.cougaar.logistics.ui.inventory.data.InventoryData;

/** 
 * <pre>
 * 
 * The InventoryDemandChart class is the chart class for
 * displaying demand information.   It plots in 
 * 2 views, 2 series apiece: demand requisitions, and
 * their corresponding allocation results and demand
 * projection and their allocation results.
 * 
 * @see InventoryChart
 * @see RequisitionChartDataModel
 * @see ProjectionChartDataModel
 * 
 *
 **/

public class InventoryDemandChart extends InventoryBarChart {


    public final static String DEMAND_TASKS =
	LogisticsInventoryFormatter.WITHDRAW_TASKS_TAG;
    public final static String DEMAND_ARS =
	LogisticsInventoryFormatter.WITHDRAW_TASK_ARS_TAG;
    public final static String DEMAND_PROJ_TASKS =
	LogisticsInventoryFormatter.COUNTED_PROJECTWITHDRAW_TASKS_TAG;
    public final static String DEMAND_PROJ_ARS =
    	LogisticsInventoryFormatter.COUNTED_PROJECTWITHDRAW_TASK_ARS_TAG;


    public InventoryDemandChart() {
	initialize("Demand");
    }

    public void initializeChart() {
	reqDM = 
	    new RequisitionsChartDataModel("",
					   DEMAND_TASKS,
					   DEMAND_ARS);

	projDM = 
	    new ProjectionsChartDataModel("Demand From Customers",
					  DEMAND_PROJ_TASKS,
					  DEMAND_PROJ_ARS,
					  DEMAND_TASKS,
					  reqDM,
					  false);

	shortfallDM = 
	    new ShortfallChartDataModel("",
					reqDM,
					projDM);
				       

	projChartDataView = addChartView(JCChart.BAR, projDM);
	reqChartDataView = addChartView(JCChart.BAR, reqDM);
	shortfallChartDataView = addChartView(JCChart.PLOT, shortfallDM);
	
	setBarChartColors();
	setShortfallChartColors();

	displayShortfall=false;
	updateShortfall();
    }	    
    
}
