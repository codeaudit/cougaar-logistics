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
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

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

    public InventoryDemandChart(boolean initialDisplayCDay, InventoryPreferenceData prefData) {
      super(prefData);
	    initialize("Demand",initialDisplayCDay);
    }

    public void initializeChart() {
	reqDM = 
	    new RequisitionsChartDataModel("",
					   DEMAND_TASKS,
					   DEMAND_ARS);

	projDM = 
	    new ProjectionsChartDataModel("Demand from Customers",
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
	
	setBarChartColors(prefData.getColorScheme());
	setShortfallChartColors(prefData.getColorScheme());

	displayShortfall=false;
	updateShortfall();
    }	    
    
}
