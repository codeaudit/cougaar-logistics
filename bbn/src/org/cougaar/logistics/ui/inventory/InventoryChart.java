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
import com.klg.jclass.chart.JCFillStyle;
import com.klg.jclass.chart.JCAxis;
import com.klg.jclass.chart.JCAxisTitle;
import com.klg.jclass.chart.JCDataIndex;
import com.klg.jclass.chart.JCPickEvent;
import com.klg.jclass.chart.EventTrigger;

import com.klg.jclass.util.legend.JCLegend;

import com.klg.jclass.chart.ChartDataView;
import com.klg.jclass.chart.ChartDataViewSeries;
import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.ChartDataEvent;
import com.klg.jclass.chart.ChartText;


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

public abstract class InventoryChart extends JPanel
  implements JCPickListener {

    protected JCChart chart=null;
    protected InventoryData inventory=null;

    protected boolean displayCDay=false;
    protected String myYAxisTitle;
    protected String myTitle;
    
    protected int viewIndex=0;


    public abstract void initializeChart();

    public void initialize(String title) {
	int gridx = 0;
	int gridy = 0;
	Insets blankInsets = new Insets(0, 0, 0, 0);

	myTitle = title;
	chart = new JCChart();
	initializeChart();
	customizeAxes("");

// allow user to zoom in on chart
	chart.setTrigger(0, new EventTrigger(0, EventTrigger.ZOOM));

	// allow interactive customization using left shift click
	chart.setAllowUserChanges(true);
	chart.setTrigger(1, new EventTrigger(Event.SHIFT_MASK,
					     EventTrigger.CUSTOMIZE));

	// allow user to display labels using right mouse click
	chart.addPickListener(this);
	chart.setTrigger(1, new EventTrigger(Event.META_MASK, EventTrigger.PICK));

	// set header and legend to black
	// set beveled borders around plot area and chart
	((JLabel)chart.getHeader()).setForeground(Color.black);
	chart.setBorder(BorderFactory.createLoweredBevelBorder());
	chart.getChartArea().setBorder(BorderFactory.createLoweredBevelBorder());

	// add chart to panel
	setLayout(new GridBagLayout());
	chart.getHeader().setVisible(true);
	// set legend invisible, because we create our own
	chart.getLegend().setVisible(true);
	add(chart, new GridBagConstraints(gridx, gridy++, 1, 1, 1.0, 1.0,
					  GridBagConstraints.CENTER, 
					  GridBagConstraints.BOTH, 
					  blankInsets, 0, 0));


	// provide for point labels displayed beneath chart
         // to leave space in layout
	/***
	pointLabel = new JLabel("Right click to get quantity at a point.");

	pointLabel.setBackground(Color.magenta);
	add(pointLabel, new GridBagConstraints(gridx, gridy, 1, 1, 1.0, 0.0,
					       GridBagConstraints.CENTER, 
					       GridBagConstraints.NONE, 
					       blankInsets, 0, 0));

	***/
    }

    public void pick(JCPickEvent e) {
	JCDataIndex dataIndex = e.getPickResult();
	// check for all the possible failures
	if (dataIndex == null) {
	    System.out.println("WARNING: dataIndex is null");
	    return;
	}
	ChartDataView chartDataView = dataIndex.getDataView();
	if (chartDataView == null) {
	    System.out.println("WARNING: chartDataView is null");
	    return;
	}
	int seriesIndex = dataIndex.getSeriesIndex();
	int pt = dataIndex.getPoint();
	if (pt < 0 || seriesIndex < 0) {
	    
	    System.out.println("WARNING: series or point index is null");
	    return;
	}
	

	// temporary until chart data view to data model correspondence is working
	//ChartDataModel dataModel = (ChartDataModel)dataViews.get(chartDataView);

	ChartDataModel dataModel = chartDataView.getDataSource();

	if (dataModel == null) {
	    System.out.println("WARNING: data model is null");
	    return;
	}
	
	// user has picked a valid point
	double[] x = dataModel.getXSeries(seriesIndex);
	double[] y = dataModel.getYSeries(seriesIndex);
	
	//MWD just gets info doesn't do anything yet.  Copied
	//from glm version.
    }

    private void resetAxes() {


	java.util.List viewList = chart.getDataView();
	for (int i = 0; i < viewList.size(); i++) {
	    ChartDataView chartDataView = (ChartDataView)viewList.get(i);

	    // use time axis for x axis
	    JCAxis xaxis = chartDataView.getXAxis();
	    
	    if(displayCDay) {
		xaxis.setAnnotationMethod(JCAxis.VALUE);
		xaxis.setPrecision(0);

		//xaxis.setAnnotationMethod(JCAxis.TIME_LABELS);
		//xaxis.setTimeUnit(JCAxis.DAYS);
		//xaxis.setTimeBase(new Date(InventoryChartBaseCalendar.getBaseTime() - baseCDayTime ));
		//xaxis.setTimeFormat("D");

		xaxis.setTickSpacing(7);
		xaxis.setGridSpacing(7);
		//		xaxis.setTickSpacingIsDefault(true);
		//              xaxis.setGridSpacingIsDefault(true);
		//xaxis.setMin(CDAY_MIN);
		xaxis.setGridVisible(true);
		xaxis.setAnnotationRotation(JCAxis.ROTATE_NONE);
	    }
	    else {
		xaxis.setAnnotationMethod(JCAxis.TIME_LABELS);
		xaxis.setTimeBase(new Date(InventoryChartBaseCalendar.getBaseTime()));

		xaxis.setTimeUnit(JCAxis.DAYS);
		xaxis.setTickSpacing(7);
		xaxis.setGridSpacing(7);
		xaxis.setTimeFormat("M/d");

		//System.out.println("Base Date is: " + new Date(InventoryChartBaseCalendar.getBaseTime()));
		xaxis.setGridVisible(true);
		xaxis.setAnnotationRotation(JCAxis.ROTATE_270);
	    }


      
	    // y axis titles
	    JCAxis yaxis = chartDataView.getYAxis();
	    yaxis.setGridVisible(true);
	    JCAxisTitle yAxisTitle = new JCAxisTitle(myYAxisTitle);
	    yAxisTitle.setRotation(ChartText.DEG_270);
	    yAxisTitle.setPlacement(JCLegend.WEST);
	    yaxis.setTitle(yAxisTitle);
	    //       yaxis.setEditable(false); // don't allow zoomin on this axis
	    yaxis.setMin(0); // set zero as the minimum value on the axis
	}
    }  

    protected void customizeAxes(String yAxisTitleText) {
	myYAxisTitle = yAxisTitleText;
	resetAxes();
    }
    
    public void setData(InventoryData data) {
	inventory = data;
	java.util.List viewList = chart.getDataView();
	for (int i = 0; i < viewList.size(); i++) {
	    ChartDataView chartDataView = (ChartDataView)viewList.get(i);
	    InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel)chartDataView.getDataSource();
	    dataModel.resetInventory(data);
	}
	chart.reset();
	resetAxes();
    }

    protected ChartDataView addView(int chartType, 
				  InventoryBaseChartDataModel dm) {

	ChartDataView chartDataView = new ChartDataView();
	chartDataView.setChartType(chartType);
	chartDataView.setDataSource(dm);
	chart.setDataView(viewIndex, chartDataView);
	viewIndex++;


	return chartDataView;
    }

    protected void setSeriesColor(ChartDataViewSeries series, Color color) {
	series.getStyle().setLineColor(color);
	series.getStyle().setFillColor(color);
	series.getStyle().setSymbolColor(color);
    }

}