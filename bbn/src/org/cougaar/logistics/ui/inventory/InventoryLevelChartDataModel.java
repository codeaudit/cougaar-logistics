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

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryLevel;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;

/** 
 * <pre>
 * 
 * The InventoryMenuEvent has all the menu commands and allows
 * mechanism to broadcast the given menu event to other parts
 * of the inventory UI.
 * 
 * 
 * @see InventoryUIFrame
 *
 **/

public class InventoryLevelChartDataModel 
            extends InventoryBaseChartDataModel {

    protected int minDay=-1;
    protected int maxDay=0;
    protected int bucketDays=1;

    protected int nValues=0;

    public static final String INVENTORY_LEVEL_SERIES_LABEL="Inventory Level";
    public static final String REORDER_LEVEL_SERIES_LABEL="Reorder Level";
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
	nSeries = 2;
	seriesLabels = new String[2];
	seriesLabels[0] = INVENTORY_LEVEL_SERIES_LABEL;
	seriesLabels[1] = REORDER_LEVEL_SERIES_LABEL;
	initValues();
    }

    public void computeCriticalNValues() {

	minDay = -1;
	maxDay = 0;
	bucketDays = 1;
	nValues=0;

	if(inventory==null) return;

	InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
	    inventory.getSchedules().get(LogisticsInventoryFormatter.INVENTORY_LEVELS_TAG);
	ArrayList levels = schedHeader.getSchedule();
	long baseTime = InventoryChartBaseCalendar.getBaseTime();

	bucketDays = -1;

	for (int i=0; i < levels.size(); i++) {
	    //MWD Stopped here - you've got a ways to go!!
	    InventoryScheduleElement level = (InventoryScheduleElement) levels.get(i);
	    long startTime = level.getStartTime();
	    long endTime = level.getEndTime();
	    int startDay = (int)((startTime - baseTime) / MILLIS_IN_DAY);
	    int endDay = (int)((endTime - baseTime) / MILLIS_IN_DAY);
	    if(bucketDays == -1) {
		long bucketSize = endTime - startTime;
		bucketDays = (int) (bucketSize / MILLIS_IN_DAY);
	    }
	    if (minDay == -1)
		minDay = startDay;
	    else if (startDay < minDay)
		minDay = startDay;
	    maxDay = Math.max(endDay, maxDay);
	}
	nValues = (maxDay - minDay + 1) / bucketDays;
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
	long baseTime = InventoryChartBaseCalendar.getBaseTime();

	computeCriticalNValues();

	xvalues = new double[nSeries][nValues];
	yvalues = new double[nSeries][nValues];
	//initZeroYVal(nValues);
	for (int i = 0; i < nSeries; i++) {
	    for (int j = 0; j < nValues; j++) {
		xvalues[i][j] = minDay + (j * bucketDays);
		yvalues[i][j] = 0;
	    }
	}

	for (int i=0; i < levels.size(); i++) {
	    InventoryLevel level = (InventoryLevel) levels.get(i);
	    long startTime = level.getStartTime();
	    long endTime = level.getEndTime();
	    int startDay = (int)((startTime - baseTime) / MILLIS_IN_DAY);
	    int endDay = (int)((endTime - baseTime) / MILLIS_IN_DAY);
	    double invQty = level.getInventoryLevel();
	    double reorderQty = level.getReorderLevel();
	    for(int j = startDay; j<= endDay; j+=bucketDays) {
		yvalues[0][j-minDay] = invQty;
		yvalues[1][j-minDay] = reorderQty;
	    }
	}
    }

    public void resetInventory(InventoryData newInventory){
	inventory = newInventory;
	valuesSet = false;
	initValues();
	fireChartDataEvent(ChartDataEvent.RELOAD,0,0);
    }

}

