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

import java.awt.Event;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.klg.jclass.chart.JCChart;
import com.klg.jclass.chart.JCPickListener;
import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.util.swing.JCExitFrame;
import com.klg.jclass.chart.JCSymbolStyle;

import com.klg.jclass.chart.ChartDataView;
import com.klg.jclass.chart.ChartDataViewSeries;
import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.ChartDataEvent;


import org.cougaar.logistics.plugin.inventory.TimeUtils;

import org.cougaar.logistics.ui.inventory.data.InventoryData;

/** 
 * <pre>
 * 
 * The InventoryChart class is the base class for all Inventory
 * charts displayed in the GUI.   It contains all the shared
 * behavior of all charts in the GUI.
 * 
 * 
 *
 **/

public class InventoryLevelChart extends InventoryChart{

    public InventoryLevelChart() {
	initialize("Inventory");
    }

    public void initializeChart() {
	InventoryLevelChartDataModel dm = new InventoryLevelChartDataModel();
	ChartDataView chartDataView = addView(JCChart.PLOT, dm);

	// set line width
	for (int j = 0; j < chartDataView.getNumSeries(); j++) {
	    ChartDataViewSeries series = chartDataView.getSeries(j);
	    series.getStyle().setLineWidth(2);
	    series.getStyle().setSymbolShape(JCSymbolStyle.NONE);
	    setSeriesColor(series,colorTable.get(series.getLabel()));
	    if(series.getLabel().equals(dm.TARGET_LEVEL_SERIES_LABEL)){
		setSeriesColor(series,Color.yellow);
		series.getStyle().setSymbolShape(JCSymbolStyle.VERT_LINE);
		series.getStyle().setSymbolColor(colorTable.get(series.getLabel() + "_SYMBOL"));
	    }
	    
	}
		    
    }
 
}
