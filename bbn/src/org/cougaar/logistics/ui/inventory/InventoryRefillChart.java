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

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

/** 
 * <pre>
 * 
 * The InventoryRefillChart class is the chart class for
 * displaying refill information.   It plots in 
 * 2 views, 2 series apiece: resupply requisitions, and
 * their corresponding allocation results and resupply
 * projection and their allocation results.
 * 
 * @see InventoryChart
 * @see RequisitionsChartDataModel
 * @see ProjectionsChartDataModel
 * 
 *
 **/

public class InventoryRefillChart extends InventoryBarChart{

    ChartDataView projChartDataView;
    ChartDataView reqChartDataView;

    public final static String RESUPPLY_TASKS =
	LogisticsInventoryFormatter.RESUPPLY_SUPPLY_TASKS_TAG;
    public final static String RESUPPLY_ARS =
	LogisticsInventoryFormatter.RESUPPLY_SUPPLY_TASK_ARS_TAG;
    public final static String RESUPPLY_PROJ_TASKS =
	LogisticsInventoryFormatter.RESUPPLY_PROJECTSUPPLY_TASKS_TAG;
    public final static String RESUPPLY_PROJ_ARS =
    	LogisticsInventoryFormatter.RESUPPLY_PROJECTSUPPLY_TASK_ARS_TAG;



    public InventoryRefillChart(boolean initialDisplayCDay, InventoryPreferenceData prefData) {
      super(prefData);
	    initialize("Refill",initialDisplayCDay);
    }



    public void initializeChart() {
	reqDM = 
	    new RequisitionsChartDataModel("",
					   RESUPPLY_TASKS,
					   RESUPPLY_ARS);

	projDM = 
	    new ProjectionsChartDataModel("Refill from Suppliers ",
					  RESUPPLY_PROJ_TASKS,
					  RESUPPLY_PROJ_ARS,
					  RESUPPLY_TASKS,
					  reqDM,
					  true);
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
