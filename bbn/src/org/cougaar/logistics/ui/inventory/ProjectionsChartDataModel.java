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
import java.util.Date;

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
import org.cougaar.logistics.ui.inventory.data.InventoryProjTask;
import org.cougaar.logistics.ui.inventory.data.InventoryTask;
import org.cougaar.logistics.ui.inventory.data.InventoryProjAR;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;


import org.cougaar.util.TimeSpanSet;


/** 
 * <pre>
 * 
 * The ProjectionsChartDataModel is the ChartDataModel for the 
 * all projection tasks.   A Schedule of  projections and their
 * corresponding allocation results are given as the 
 * values to compute into the x and y coordinates for the 
 * chart.   If the chart is type of resupply then we calculate
 * the last corresponding actual date of resupply, and only use
 * projections and allocation results found after that date.
 * Otherwise it is counted demand schedules where we have to 
 * sum the actual sum in the same bucket.   Either way this
 * chartDataModel depends on either the actual schedule, or 
 * the actual schedules chart data model.
 * 
 * 
 * @see InventoryBaseChartDataModel
 *
 **/

public class ProjectionsChartDataModel 
            extends InventoryBaseChartDataModel {

    protected String projScheduleName;
    protected String projARScheduleName;
    protected String reqScheduleName;
    protected RequisitionsChartDataModel reqDataModel;
    protected boolean resupply;

    protected double projYValues[][];

    public static final String PROJECTION_SERIES_LABEL="Projection";
    public static final String PROJECTION_ALLOCATION_SERIES_LABEL="Projection Allocation";
    public static final String PROJECTION_LEGEND="";

    public ProjectionsChartDataModel(String aProjScheduleName,
				     String aProjARScheduleName,
				     String aReqScheduleName,
				     RequisitionsChartDataModel aReqDataModel,
				     boolean isResupply) {
	this(PROJECTION_LEGEND,aProjScheduleName,aProjARScheduleName,aReqScheduleName,aReqDataModel,isResupply);
    }

    public ProjectionsChartDataModel(String legendTitle,
				     String projSchedule,
				     String projARSchedule,
				     String aReqScheduleName,
				     RequisitionsChartDataModel aReqDataModel,
				     boolean isResupply) {
	this(null,projSchedule,projARSchedule,legendTitle,aReqScheduleName,aReqDataModel,isResupply);
    }


    public ProjectionsChartDataModel(InventoryData data,
				     String projSchedule,
				     String projARSchedule,
				     String theLegendTitle,
				     String aReqScheduleName,
				     RequisitionsChartDataModel aReqDataModel,
				     boolean isResupply) {
	inventory = data;
	legendTitle = theLegendTitle;
	projScheduleName = projSchedule;
	projARScheduleName = projARSchedule;
	reqScheduleName = aReqScheduleName;
	reqDataModel = aReqDataModel;
	resupply = isResupply;	
	
	nSeries = 2;
	scheduleNames = new String[2];
	scheduleNames[0] = projScheduleName;
	scheduleNames[1] = projARScheduleName;
	seriesLabels = new String[2];
	seriesLabels[0] = PROJECTION_SERIES_LABEL;
	seriesLabels[1] = PROJECTION_ALLOCATION_SERIES_LABEL;
	logger = Logging.getLogger(this);
	initValues();
    }



    public void setValues() {
	if(valuesSet) return;
	setProjectionValues();
	valuesSet = true;
    }

    public void setProjectionValues() {

	if(inventory == null) {
	    xvalues = new double[nSeries][0];
	    yvalues = new double[nSeries][0];
	    projYValues = new double[nSeries][0];
	    return;
	}

	InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
	    inventory.getSchedules().get(projScheduleName);
	ArrayList projections = schedHeader.getSchedule();
	schedHeader = (InventoryScheduleHeader) 
	    inventory.getSchedules().get(projARScheduleName);
	ArrayList projARs = schedHeader.getSchedule();
	schedHeader = (InventoryScheduleHeader)
	    inventory.getSchedules().get(reqScheduleName);
	ArrayList requisitions = schedHeader.getSchedule();
	long maxReqEndTime=0;

	if(resupply) {
	    for(int i=0; i < requisitions.size(); i++) {
		InventoryTask task = (InventoryTask) requisitions.get(i);
		long endTime = task.getEndTime();
		maxReqEndTime = Math.max(endTime,maxReqEndTime);
	    }
	}

   
	computeCriticalNValues();

	xvalues = new double[nSeries][nValues];
	yvalues = new double[nSeries][nValues];
	projYValues = new double[nSeries][nValues];
	//initZeroYVal(nValues);

	for (int i = 0; i < nSeries; i++) {
	    for (int j = 0; j < nValues; j++) {
		xvalues[i][j] = minDay + (j * bucketDays);
		yvalues[i][j] = 0;
		projYValues[i][j] = 0;
	    }
	}

	//Explode into dailys
	ArrayList dailys = new ArrayList();
	ArrayList dailyARs = new ArrayList();
	for(int i=0; i < projections.size(); i++) {
	    InventoryProjTask projTask = (InventoryProjTask) projections.get(i);
	    dailys.addAll(projTask.explodeToDaily());
	}
	for(int i=0; i < projARs.size(); i++) {
	    InventoryProjAR projAR = (InventoryProjAR) projARs.get(i);
	    dailyARs.addAll(projAR.explodeToDaily());
	}


	for(int i=0; i < dailys.size() ; i++) {
	    InventoryProjTask task = (InventoryProjTask) dailys.get(i);
	    long endTime = task.getEndTime();
	    long startTime = task.getStartTime();
	    if(!resupply ||
	       (startTime >= (maxReqEndTime + MILLIS_IN_DAY))) {
		int endDay = (int) ((endTime - baseTime) / MILLIS_IN_DAY);
		int graphDay = (endDay - minDay)/bucketDays;
		if(graphDay < 0) 
		    logger.error("Array out of bounds alright.  The Graphday is negative");
		if(graphDay < nValues)
		    projYValues[0][graphDay]+=task.getDailyRate();
		else {
		    if(logger.isInfoEnabled()) {
			logger.info("ProjectionsChartDataModel:Index Out of bounds on the tasks - falling off the end. Length " + nValues + " and graph day is: " + graphDay);
		    }
		}
	    }
	}	    

	for(int i=0; i < dailyARs.size() ; i++) {
	    InventoryProjAR ar = (InventoryProjAR) dailyARs.get(i);
	    if(ar.isSuccess()) {
		long endTime = ar.getEndTime();
		long startTime = ar.getStartTime();
		if(!resupply ||
		   (startTime >= (maxReqEndTime + MILLIS_IN_DAY))) {
		    int endDay = (int) ((endTime - baseTime) / MILLIS_IN_DAY);
		    int graphDay = (endDay - minDay)/bucketDays;
		    if(graphDay < 0) 
			logger.error("Array out of bounds alright.  The Graphday is negative");
		    if(graphDay < nValues)
			projYValues[1][graphDay]+=ar.getDailyRate();
		    else {
			if(logger.isInfoEnabled()) {
			    logger.info("ProjectionsChartDataModel:Index Out of bounds on the ARs - falling off the end. Length " + nValues + " and graph day is: " + graphDay);
			}
		    }
		}	    
	    }
	}

	reqDataModel.resetInventory(inventory);
	for (int i = 0; i < nSeries; i++) {
	    for (int j = 0; j < nValues; j++) {
		if(xvalues[i][j] == reqDataModel.getXSeries(i)[j]) {
		    yvalues[i][j] = projYValues[i][j] + 
			reqDataModel.getYSeries(i)[j] ;
		}
		else {
		    throw new RuntimeException("ProjectionsChartDataModel:Accccch!  Mis-matching dates between two views...");
		}
	    }
	}
	
    }



  /**
   * Retrieves the specified y-value series
   * The nth asset.
   * This returns the nominal getYSeries of the super class
   * @param index data series index
   * @return array of double values representing y-value data
   */
  public synchronized double[] getRealYSeries(int index) {
      initValues();
      if (projYValues == null) {
	  logger.error("InventoryChartDataModel ERROR getRealYSeries has no projYValues?");
      }    
      return projYValues[index];

  }


}

