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

import org.cougaar.logistics.plugin.inventory.TimeUtils;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;

/** 
 * <pre>
 * 
 * The InventoryBaseChartDataModel is the base abstract superclass of all
 * the data models that underly the charts in the gui.
 * Its the bridge between the data and the interface necessary
 * to use the jchart software.
 * 
 * 
 * @see InventoryLevelChartDataModel
 *
 **/

public abstract class InventoryBaseChartDataModel extends ChartDataSupport
  implements ChartDataManageable,
             ChartDataModel,
	     LabelledChartDataModel {

  protected final static long MILLIS_IN_DAY = TimeUtils.MSEC_PER_DAY;

  protected int nSeries;
  protected double xvalues[][];
  protected double yvalues[][];
  protected String[] seriesLabels;
  protected String[] scheduleNames;

  protected int minDay=-1;
  protected int maxDay=0;
  protected int bucketDays=1;

  protected int nValues=0;
    
  protected String legendTitle;

  protected InventoryData inventory;

  protected boolean valuesSet;

  protected Logger logger;

  public abstract void resetInventory(InventoryData inventory);

  public ChartDataManager getChartDataManager() { 
      return (ChartDataManager)this; 
  }
	
  public String getDataSourceName() {
      return legendTitle;
  }

  /**
   * Retrieves the specified x-value series
   * This returns the nominal getXSeries of the super class
   * @param index data series index
   * @return array of double values representing x-value data
   */
  public double[] getRealXSeries(int index) {
      return getXSeries(index);
  }

  /**
   * Retrieves the specified y-value series
   * The nth asset.
   * This returns the nominal getYSeries of the super class
   * This aids the ProjectionChartDataModel which 
   * has a real projected dimension which is added
   * to actual demand to give an artificial 
   * bar type chart.
   * @param index data series index
   * @return array of double values representing y-value data
   */
  public synchronized double[] getRealYSeries(int index) {
      return getYSeries(index);
  }

  /**
   * Retrieves the specified x-value series
   * Start and end times of the schedule for each asset
   * @param index data series index
   * @return array of double values representing x-value data
   */
  public double[] getXSeries(int index) {
    initValues();
    if (xvalues == null) {
	logger.error("InventoryChartDataModel ERROR getXSeries no xvalues?");
    }
    return xvalues[index];
  }

  /**
   * Retrieves the specified y-value series
   * The nth asset
   * @param index data series index
   * @return array of double values representing y-value data
   */
  public synchronized double[] getYSeries(int index) {
    initValues();
    if (yvalues == null) {
	logger.error("InventoryChartDataModel ERROR getYSeries no yvalues?");
    }    
    return yvalues[index];
  }
 
  /**
   * Retrieves the number of data series.
   */
  public int getNumSeries() {
    initValues();
    return nSeries;
  }

 
  public String[] getPointLabels() {
    return null;
  }

  public String[] getSeriesLabels() {
    return seriesLabels;
  }

  public void initValues() {
      if(!valuesSet) {
	  setValues();
      }
  }

    public abstract void setValues();
    
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
	    InventoryScheduleElement level = (InventoryScheduleElement) levels.get(i);
	    long startTime = level.getStartTime();
	    long endTime = level.getEndTime();
	    int startDay = (int)((startTime - baseTime) / MILLIS_IN_DAY);
	    int endDay = (int)((endTime - baseTime) / MILLIS_IN_DAY);
	    if(bucketDays == -1) {
		long bucketSize = endTime - startTime;
		bucketDays = (int) (bucketSize / MILLIS_IN_DAY);
	    }
	    if (minDay == -1) {
		minDay = startDay;
	    }
	    else if (startDay < minDay) {
		minDay = startDay;
	    }
	    maxDay = Math.max(endDay, maxDay);
	}
	nValues = (maxDay - minDay + 1) / bucketDays;
    }


}

