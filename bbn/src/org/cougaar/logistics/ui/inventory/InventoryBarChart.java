/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
 
package org.cougaar.logistics.ui.inventory;

import java.util.Date;

import java.awt.Color;
import java.awt.Insets;

import com.klg.jclass.chart.JCChart;
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

/** 
 * <pre>
 * 
 * The InventoryBarChart class is an abstract class that
 * captures common behavior between the 
 * InventoryDemandChart and the InventoryRefillChart.
 * 
 * @see InventoryChart
 * @see InventoryDemandChart
 * @see InventoryRefillChart
 * 
 *
 **/

public abstract class InventoryBarChart extends InventoryChart {

    protected ChartDataView projChartDataView;
    protected ChartDataView reqChartDataView;
    protected ChartDataView shortfallChartDataView;

    protected RequisitionsChartDataModel reqDM;
    protected ProjectionsChartDataModel projDM;
    protected ShortfallChartDataModel shortfallDM;

    protected boolean displayShortfall=false;

    public void setData(InventoryData data) {
	inventory = data;
	java.util.List viewList = chart.getDataView();
	for (int i = 0; i < viewList.size(); i++) {
	    ChartDataView chartDataView = (ChartDataView)viewList.get(i);
	    InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel)chartDataView.getDataSource();
	    dataModel.resetInventory(data);
	}
	updateShortfall();
	myYAxisTitle = data.getUnit();
	chart.reset();
	resetAxes();
    }

    public void updateShortfall() {
	if(isShortfall()) {
	     shortfallChartDataView.setVisible(displayShortfall);
	}
	else {
	     shortfallChartDataView.setVisible(false);
	}	
    }

    public void setDisplayShortfall(boolean doDisplayShortfall) {
	if(doDisplayShortfall != displayShortfall) {
	    displayShortfall = doDisplayShortfall;
	    updateShortfall();
	}
    }


    protected void setShortfallChartColors() {
	if(shortfallChartDataView != null) {
	    for (int j = 0; j < shortfallChartDataView.getNumSeries(); j++) {
		ChartDataViewSeries series = shortfallChartDataView.getSeries(j);
		series.getStyle().setLineWidth(2);
		series.getStyle().setSymbolShape(JCSymbolStyle.NONE);
		setSeriesColor(series,colorTable.get(series.getLabel()));
	    }
	}
    }

    protected void setBarChartColors() {
	java.util.List viewList = chart.getDataView();
	for (int i = 0; i < viewList.size(); i++) {
	    // set line width
	    ChartDataView chartDataView = (ChartDataView)viewList.get(i);
	    //((JCBarChartFormat)chartDataView .getChartFormat()).setClusterWidth(100);
	    chartDataView.setOutlineColor(Color.black);
	    for (int j = 0; j < chartDataView.getNumSeries(); j++) {
		ChartDataViewSeries series = chartDataView.getSeries(j);
		setSeriesColor(series,colorTable.get(series.getLabel()));
		Color symbolColor = colorTable.get(series.getLabel() + "_SYMBOL");
		if(symbolColor != null) {
		    series.getStyle().setSymbolColor(symbolColor);
		}
	    }
	}
    }

    public boolean isShortfall() { 
	if(shortfallDM == null) {
	    return false;
	}
	else {
	    return shortfallDM.isShortfall();
	}
    }
 
}
