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
import org.cougaar.logistics.ui.inventory.data.InventoryTask;
import org.cougaar.logistics.ui.inventory.data.InventoryAR;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;


import org.cougaar.util.TimeSpanSet;


/** 
 * <pre>
 * 
 * The RequisitionsChartDataModel is the ChartDataModel for the 
 * all non projection tasks.   A Schedule of actuals and their
 * corresponding allocation results are given as the 
 * values to compute into the x and y coordinates for the 
 * chart.   
 * 
 * 
 * @see InventoryBaseChartDataModel
 *
 **/

public class RequisitionsChartDataModel 
            extends InventoryBaseChartDataModel {

    protected String reqScheduleName;
    protected String reqARScheduleName;


    public static final String REQUISITION_SERIES_LABEL="Requisition";
    public static final String REQUISITION_ALLOCATION_SERIES_LABEL="Requisition Allocation";
    public static final String REQUISITION_LEGEND="Requisitions";

    public RequisitionsChartDataModel(String aReqScheduleName,
				     String aReqARScheduleName) {
	this(REQUISITION_LEGEND,aReqScheduleName,aReqARScheduleName);
    }

    public RequisitionsChartDataModel(String legendTitle,
				     String reqSchedule,
				     String reqARSchedule) {
	this(null,reqSchedule,reqARSchedule,legendTitle);
    }


    public RequisitionsChartDataModel(InventoryData data,
				     String reqSchedule,
				     String reqARSchedule,
				     String theLegendTitle) {
	inventory = data;
	legendTitle = theLegendTitle;
	reqScheduleName = reqSchedule;
	reqARScheduleName = reqARSchedule;
	nSeries = 2;
	scheduleNames = new String[2];
	scheduleNames[0] = reqScheduleName;
	scheduleNames[1] = reqARScheduleName;
	seriesLabels = new String[2];
	seriesLabels[0] = REQUISITION_SERIES_LABEL;
	seriesLabels[1] = REQUISITION_ALLOCATION_SERIES_LABEL;
	initValues();
    }



    public void setValues() {
	if(valuesSet) return;
	setRequisitionValues();
	valuesSet = true;
    }

    public void setRequisitionValues() {

	if(inventory == null) {
	    xvalues = new double[nSeries][0];
	    yvalues = new double[nSeries][0];
	    return;
	}

	InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
	    inventory.getSchedules().get(reqScheduleName);
	ArrayList requisitions = schedHeader.getSchedule();
	long baseTime = InventoryChartBaseCalendar.getBaseTime();
	schedHeader = (InventoryScheduleHeader) 
	    inventory.getSchedules().get(reqARScheduleName);
	ArrayList reqARs = schedHeader.getSchedule();

   
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

	for(int i=0; i < requisitions.size() ; i++) {
	    InventoryTask task = (InventoryTask) requisitions.get(i);
	    long endTime = task.getEndTime();
	    int endDay = (int) ((endTime - baseTime) / MILLIS_IN_DAY);
	    yvalues[0][endDay - minDay]+=task.getQty();
	}	    

	for(int i=0; i < reqARs.size() ; i++) {
	    InventoryAR ar = (InventoryAR) reqARs.get(i);
	    if(ar.isSuccess()) {
		long endTime = ar.getEndTime();
		int endDay = (int) ((endTime - baseTime) / MILLIS_IN_DAY);
		yvalues[1][endDay - minDay]+=ar.getQty();
	    }
	}	    
	    
	//MWD this is more expensive than it needs to be.
	//Cheaper to go through through all InventoryTasks
	//take the end time figure which day it goes into
	//and sum the value in the array bucket - same for
	//ARs - fix tommorow.  Probably don't even need 
	//schedules for this.
	
	/***
	 ** MWD Remove

	 TimeSpanSet reqSchedule = new TimeSpanSet(requisitions);
	 TimeSpanSet reqARSchedule = new TimeSpanSet(reqARs);

	for(int i=minDay; i<=maxDay ; i+=bucketDays) {
	    long startTime = ((i * MILLIS_IN_DAY) + baseTime);
	    long endTime = (startTime + (bucketDays * MILLIS_IN_DAY)) - 1;
	    Collection reqs = reqSchdule.encapsulatedSet(startTime,endTime);
	    Collection reqARs = reqARSchdule.encapsulatedSet(startTime,endTime);
	    double totalReqs=0;
	    double totalARs=0;
	    Iterator it = reqs.iterator();
	    while(it.hasNext()) {
		totalReqs+=(((InventoryTask) it.next()).getQty());
	    }
	    it = reqARs.iterator();
	    while(it.hasNext()) {
		InventoryAR ar = ((InventoryAR) it.next());
		if(ar.isSuccess()) {
		    totalARs+=ar.getQty();
		}
	    }
	    yvalues[0][i-minDay] = totalReqs;
	    yvalues[1][i-minDay] = totalARs;
	}

	***/
    }

    public void resetInventory(InventoryData newInventory){
	inventory = newInventory;
	valuesSet = false;
	initValues();
	fireChartDataEvent(ChartDataEvent.RELOAD,0,0);
    }
}

