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

    public static final String PROJECTION_SERIES_LABEL="Projection";
    public static final String PROJECTION_ALLOCATION_SERIES_LABEL="Projection Allocation";
    public static final String PROJECTION_LEGEND="Projections";

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
	    return;
	}

	InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
	    inventory.getSchedules().get(projScheduleName);
	ArrayList projections = schedHeader.getSchedule();
	long baseTime = InventoryChartBaseCalendar.getBaseTime();
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
	//initZeroYVal(nValues);

	for (int i = 0; i < nSeries; i++) {
	    for (int j = 0; j < nValues; j++) {
		xvalues[i][j] = minDay + (j * bucketDays);
		yvalues[i][j] = 0;
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
	    InventoryProjTask projTask = (InventoryProjTask) projARs.get(i);
	    dailyARs.addAll(projTask.explodeToDaily());
	}


	for(int i=0; i < dailys.size() ; i++) {
	    InventoryProjTask task = (InventoryProjTask) projections.get(i);
	    long endTime = task.getEndTime();
	    long startTime = task.getStartTime();
	    if(!resupply ||
	       (startTime > (maxReqEndTime + MILLIS_IN_DAY))) {
		int endDay = (int) ((endTime - baseTime) / MILLIS_IN_DAY);
		yvalues[0][(endDay - minDay)/bucketDays]+=task.getDailyRate();
	    }
	}	    

	for(int i=0; i < dailyARs.size() ; i++) {
	    InventoryProjAR ar = (InventoryProjAR) dailyARs.get(i);
	    if(ar.isSuccess()) {
		long endTime = ar.getEndTime();
		long startTime = ar.getStartTime();
		if(!resupply ||
		   (startTime > (maxReqEndTime + MILLIS_IN_DAY))) {
		    int endDay = (int) ((endTime - baseTime) / MILLIS_IN_DAY);
		    yvalues[1][(endDay - minDay)/bucketDays]+=ar.getDailyRate();
		}
	    }	    
	}
    }

    public void resetInventory(InventoryData newInventory){
	inventory = newInventory;
	valuesSet = false;
	initValues();
	fireChartDataEvent(ChartDataEvent.RELOAD,0,0);
    }

  /**
   * Retrieves the specified x-value series
   * This returns the nominal getXSeries of the super class
   * @param index data series index
   * @return array of double values representing x-value data
   */
  public double[] getRealXSeries(int index) {
      return super.getXSeries(index);
  }

  /**
   * Retrieves the specified y-value series
   * The nth asset.
   * This returns the nominal getYSeries of the super class
   * @param index data series index
   * @return array of double values representing y-value data
   */
  public synchronized double[] getRealYSeries(int index) {
      return super.getYSeries(index);
  }

  /**
   * Retrieves the specified x-value series
   * Start and end times of the schedule for each asset
   * For non resupply we sum the x series of the actuals plus these
   * projections to give the impression in the graph of 
   * stacking projections on top of the actuals.
   * (two views super imposed on each other the actuals on 
   * the projections)
   * @param index data series index
   * @return array of double values representing x-value data
   */
    /*** MWD Needs fixing should sum resultant array in seperate
     *** setProjectionValues() and keps as seperate array.
     *** don't want to sum 2 arrays every time - this may be tricky.
  public double[] getXSeries(int index) {
      return (getRealXSeries(index) + reqDataModel.getXSeries(index));
  }
    ***/

  /**
   * Retrieves the specified y-value series
   * The nth asset
   * For non resupply we sum the y series of the actuals plus these
   * projections to give the impression in the graph of 
   * stacking projections on top of the actuals.
   * (two views super imposed on each other the actuals on 
   * the projections)   * The nth asset
   * @param index data series index
   * @return array of double values representing y-value data
   */

    /***  MWD Needs fixing
  public synchronized double[] getYSeries(int index) {
      return (getRealYSeries(index) + reqDataModel.getYSeries(index));
  }

    ****/

}

