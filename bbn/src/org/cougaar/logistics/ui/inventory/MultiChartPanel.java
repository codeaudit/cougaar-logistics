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
import java.util.Calendar;

import java.awt.Event;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.klg.jclass.chart.JCChartListener;
import com.klg.jclass.chart.JCChartEvent;
import com.klg.jclass.chart.JCChart;
import com.klg.jclass.chart.JCAxis;
import com.klg.jclass.chart.JCPickListener;
import com.klg.jclass.chart.JCPickEvent;
import com.klg.jclass.chart.JCDataIndex;
import com.klg.jclass.chart.JCChartUtil;
import com.klg.jclass.chart.ChartDataView;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.ui.inventory.data.InventoryData;

/** 
 * <pre>
 * 
 * The MultiChartPanel contains the three InventoryCharts
 * to be displayed: the levels, refill, and demand charts.
 * Its just a container that measures lays out the 
 * charts appropriatly.
 *
 *
 * @see InventoryLevelChart
 * @see InventoryRefillChart
 * @see InventoryDemandChart
 * 
 *
 **/

public class MultiChartPanel extends JPanel 
    implements JCChartListener, JCPickListener{

    public final static String INITIAL_POINT_LABEL="Right click to get quantity at a point.";

    protected InventoryLevelChart levelChart;
    protected InventoryRefillChart refillChart;
    protected InventoryDemandChart demandChart;

    protected JLabel pointLabel;

    private Logger logger;

    public MultiChartPanel() {
	super();
	logger = Logging.getLogger(this);
	initializeMultiChart();
    }

    public void initializeMultiChart() {
	int gridx = 0;
	int gridy = 0;

	Insets blankInsets = new Insets(0, 0, 0, 0);
	levelChart = new InventoryLevelChart();
	refillChart = new InventoryRefillChart();
	demandChart = new InventoryDemandChart();

	pointLabel = new JLabel(INITIAL_POINT_LABEL);
	pointLabel.setBackground(Color.magenta);

	addAllChartListeners();
	addAllPickListeners();

	// set header and legend to black
	// set beveled borders around plot area and chart
	//((JLabel)chart.getHeader()).setForeground(Color.black);
	//chart.setBorder(BorderFactory.createLoweredBevelBorder());
	//chart.getChartArea().setBorder(BorderFactory.createLoweredBevelBorder());

	// add chart to panel
	setLayout(new GridBagLayout());

	int height = 4;
	gridy += height;

	add(levelChart, new GridBagConstraints(gridx, 
					       GridBagConstraints.RELATIVE, 
					       1, height, 1.0, 1.0,
					       GridBagConstraints.CENTER, 
					       GridBagConstraints.BOTH, 
					       blankInsets, 0, 0));

	height=3;
	gridy += height;

	add(refillChart, new GridBagConstraints(gridx, 
						GridBagConstraints.RELATIVE, 
						1, height, 1.0, 0.7,
						GridBagConstraints.CENTER, 
						GridBagConstraints.BOTH, 
						blankInsets, 0, 0));
	
	height=3;
	gridy += height;

	add(demandChart, new GridBagConstraints(gridx,
						GridBagConstraints.RELATIVE,  
						1,height, 1.0, 0.7,
						GridBagConstraints.CENTER, 
						GridBagConstraints.BOTH, 
						blankInsets, 0, 0));

	height = 1;
	gridy += height;

	add(pointLabel, new GridBagConstraints(gridx,
					       GridBagConstraints.RELATIVE,  
					       1, height, 1.0, 0.1,
					       GridBagConstraints.CENTER, 
					       GridBagConstraints.NONE, 
					       blankInsets, 0, 0));
    }
   
    public void setData(InventoryData data) {
	removeAllChartListeners();
	levelChart.setData(data);
	refillChart.setData(data);
	demandChart.setData(data);
	pointLabel.setText(INITIAL_POINT_LABEL);
	addAllChartListeners();
    }

    public void removeAllChartListeners() {
	levelChart.removeChartListener(this);
	refillChart.removeChartListener(this);
	demandChart.removeChartListener(this);
    }

    public void addAllChartListeners() {
	levelChart.addChartListener(this);
	refillChart.addChartListener(this);
	demandChart.addChartListener(this);
    }

    public void removeAllPickListeners() {
	levelChart.removePickListener(this);
	refillChart.removePickListener(this);
	demandChart.removePickListener(this);
    }

    public void addAllPickListeners() {
	levelChart.addPickListener(this);
	refillChart.addPickListener(this);
	demandChart.addPickListener(this);
    }

    public void changeChart(JCChartEvent jce) {
	JCChart source = (JCChart)jce.getSource();
	String headerText =  ((JLabel) source.getHeader()).getText();
	JCAxis axis = jce.getModifiedAxis();
	if(!axis.isVertical()) {
	    double xStart = axis.getMin();
	    double xEnd = axis.getMax();
	    if(logger.isDebugEnabled()) {
		logger.debug("X-Axis of " + headerText + " changed min: " + axis.getMin() + " Max: " + axis.getMax());
	    }
	    removeAllChartListeners();
	    levelChart.setXZoom(xStart,xEnd);
	    refillChart.setXZoom(xStart,xEnd);
	    demandChart.setXZoom(xStart,xEnd);
	    pointLabel.setText(INITIAL_POINT_LABEL);
	    addAllChartListeners();
	}
    }

    public void paintChart(JCChart chart) {
    }

    public void pick(JCPickEvent e) {
	JCDataIndex dataIndex = e.getPickResult();
	// check for all the possible failures
	if (dataIndex == null) {
	    logger.warn("WARNING: dataIndex is null");
	    return;
	}
	ChartDataView chartDataView = dataIndex.getDataView();
	if (chartDataView == null) {
	    logger.warn("WARNING: chartDataView is null");
	    return;
	}
	int seriesIndex = dataIndex.getSeriesIndex();
	int pt = dataIndex.getPoint();
	if (pt < 0 || seriesIndex < 0) {
     	    logger.warn("WARNING: series or point index is null");
	    return;
	}
	
	InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();

	if (dataModel == null) {
	    logger.warn("WARNING: data model is null");
	    return;
	}
	

	// user has picked a valid point
	double[] x = dataModel.getXSeries(seriesIndex);
	double[] y = dataModel.getRealYSeries(seriesIndex);


	boolean displayCDay=false;
	long baseCDayTime = InventoryChartBaseCalendar.getBaseTime();

	int cDay;
	int dayOfYear;
	int daysFromBaseToCDay = (int) ((baseCDayTime - InventoryChartBaseCalendar.getBaseTime())
					/ TimeUtils.MSEC_PER_DAY);


	if(displayCDay) {
	    cDay = ((int)x[pt]);
	    dayOfYear = cDay + daysFromBaseToCDay;
	} else {
	    dayOfYear = ((int)x[pt]);
	    cDay = dayOfYear - daysFromBaseToCDay;
	}
	InventoryChartBaseCalendar tmpC = new InventoryChartBaseCalendar();
	tmpC.set(Calendar.YEAR, InventoryChartBaseCalendar.getBaseYear());
	tmpC.set(Calendar.DAY_OF_YEAR, dayOfYear);
	// add 1 to month as it numbers them from 0
	int month = tmpC.get(Calendar.MONTH) + 1;
	
	String time="";

	String qty="";



	String label = "Date: " + month + "/" +
	    tmpC.get(Calendar.DAY_OF_MONTH) + "/" +
	    tmpC.get(Calendar.YEAR) + " " +
	    //    "(C" + cDay + ")" + time +
	    " Quantity: " + JCChartUtil.format(y[pt], 1);
	
	    
	//System.out.println("Pick Point is: " + label);
	pointLabel.setText(label);
    
    }

}
