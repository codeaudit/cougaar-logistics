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

import java.util.ArrayList;

import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.LabelledChartDataModel;
import com.klg.jclass.chart.ChartDataSupport;
import com.klg.jclass.chart.ChartDataEvent;
import com.klg.jclass.chart.ChartDataManageable;
import com.klg.jclass.chart.ChartDataManager;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryLevel;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;


/** 
 * <pre>
 * 
 * The InventoryLevelChartDataModel is the ChartDataModel for the 
 * InventoryLevelChart.    It calculates the inventory, reorder, 
 * and target levels for puts them in x and y coordinates.
 * 
 * 
 * @see InventoryBaseChartDataModel
 *
 **/

public class InventoryLevelChartDataModel 
            extends InventoryBaseChartDataModel {



    public static final String INVENTORY_LEVEL_SERIES_LABEL="Inventory Level";
    public static final String REORDER_LEVEL_SERIES_LABEL="Reorder Level";
    public static final String TARGET_LEVEL_SERIES_LABEL="Target Level";
    public static final String INVENTORY_LEVEL_LEGEND="Inventory Key Levels";

    public InventoryLevelChartDataModel() {
	this(null,INVENTORY_LEVEL_LEGEND);
    }

    public InventoryLevelChartDataModel(String legendTitle) {
	this(null,legendTitle);
    }


    public InventoryLevelChartDataModel(InventoryData data,
					String theLegendTitle) {
	inventory = data;
	legendTitle = theLegendTitle;
	nSeries = 3;
	seriesLabels = new String[nSeries];
	seriesLabels[0] = INVENTORY_LEVEL_SERIES_LABEL;
	seriesLabels[1] = REORDER_LEVEL_SERIES_LABEL;
	seriesLabels[2] = TARGET_LEVEL_SERIES_LABEL;
	logger = Logging.getLogger(this);
	initValues();
    }



    public void setValues() {
	if(valuesSet) return;
	setInventoryValues();
	valuesSet = true;
    }

    public void setInventoryValues() {

	if(inventory == null) {
	    xvalues = new double[nSeries][0];
	    yvalues = new double[nSeries][0];
	    return;
	}

	InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
	    inventory.getSchedules().get(LogisticsInventoryFormatter.INVENTORY_LEVELS_TAG);
	ArrayList levels = schedHeader.getSchedule();

	computeCriticalNValues();

	xvalues = new double[nSeries][];
	yvalues = new double[nSeries][];
	//initZeroYVal(nValues);
	for (int i = 0; i < (nSeries-1); i++) {
	    xvalues[i] = new double[nValues];
	    yvalues[i] = new double[nValues];	    
	    for (int j = 0; j < nValues; j++) {
		xvalues[i][j] = minDay + (j * bucketDays);
		yvalues[i][j] = 0;
	    }
	}

	ArrayList targetLevels = new ArrayList();

	//Need to add target level which is a little more complicated
	//than you think.  We don't know how many values there are
	//in this third series. We have to add them if they are non null
	//to a vector, allocated the third series same length as the 
	//vector and put them into there. mildly tricky business.
	for (int i=0; i < levels.size(); i++) {
	    InventoryLevel level = (InventoryLevel) levels.get(i);
	    long startTime = level.getStartTime();
	    long endTime = level.getEndTime();
	    int startDay = (int)((startTime - baseTime) / MILLIS_IN_DAY);
	    int endDay = (int)((endTime - baseTime) / MILLIS_IN_DAY);
	    double invQty = level.getInventoryLevel();
	    double reorderQty = level.getReorderLevel();
	    for(int j = startDay; j<= endDay; j+=bucketDays) {
		yvalues[0][(j-minDay)/bucketDays] = invQty;
		yvalues[1][(j-minDay)/bucketDays] = reorderQty;
	    }
	    if(level.getTargetLevel() != null) {
		targetLevels.add(level);
	    }
	}

	xvalues[2] = new double[targetLevels.size()];
	yvalues[2] = new double[targetLevels.size()];

	for(int i=0; i<targetLevels.size(); i++) {
	    InventoryLevel level = (InventoryLevel) targetLevels.get(i);
	    long startTime = level.getStartTime();
	    int startDay = (int)((startTime - baseTime) / MILLIS_IN_DAY);
	    double targetLevel = level.getTargetLevel().doubleValue();
	    xvalues[2][i] = startDay;
	    yvalues[2][i] = targetLevel;
	}
    }



}

