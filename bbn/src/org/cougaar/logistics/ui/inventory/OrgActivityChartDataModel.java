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
 * The OrgActivityChartDataModel is the 2nd ChartDataModel for the
 * InventoryLevelChart.    It shows the defensive and offensive,
 * org activities as background to the inventory levels.
 *
 *
 * @see InventoryBaseChartDataModel
 *
 **/

public class OrgActivityChartDataModel
        extends InventoryBaseChartDataModel {


    private double offensiveQty=0.0;
    private double defensiveQty=0.0;

    public static final int OFFENSIVE_SERIES_INDEX = 0;
    public static final int DEFENSIVE_SERIES_INDEX = 1;

    public static final String OFFENSIVE_SERIES_LABEL = "Offensive";
    public static final String DEFENSIVE_SERIES_LABEL = "Defensive";

    public static final String ORG_ACTIVITY_LEGEND = "";

    public OrgActivityChartDataModel() {
        this(null, ORG_ACTIVITY_LEGEND);
    }

    public OrgActivityChartDataModel(String legendTitle) {
        this(null, legendTitle);
    }

    public OrgActivityChartDataModel(InventoryData data,
                                     String theLegendTitle) {
        inventory = data;
        legendTitle = theLegendTitle;
        nSeries = 2;
        seriesLabels = new String[nSeries];
        seriesLabels[OFFENSIVE_SERIES_INDEX] = OFFENSIVE_SERIES_LABEL;
        seriesLabels[DEFENSIVE_SERIES_INDEX] = DEFENSIVE_SERIES_LABEL;
        logger = Logging.getLogger(this);
        initValues();
    }


    public String getActivityFromLevel(double value){
      if(value == defensiveQty){
        return "Defensive";
      }
      else {
        return "Offensive";
      }
    }


    public double getLevelFromActivity(String value){
      if(value.trim().toLowerCase().equals("defensive")){
        return defensiveQty;
      }
      else {
        return offensiveQty;
      }
    }

    public void setValues() {
        if (valuesSet) return;
        setInventoryValues();
        valuesSet = true;
    }

    public void setInventoryValues() {

        if (inventory == null) {
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
        for (int i = 0; i < (nSeries); i++) {
            xvalues[i] = new double[nValues];
            yvalues[i] = new double[nValues];
            for (int j = 0; j < nValues; j++) {
                xvalues[i][j] = minBucket + (j * bucketDays);
                yvalues[i][j] = 0;
            }
        }

        ArrayList orgActs = new ArrayList();

        double maxQty = 0;

        //Need to add target level which is a little more complicated
        //than you think.  We don't know how many values there are
        //in this third series. We have to add them if they are non null
        //to a vector, allocated the third series same length as the
        //vector and put them into there. mildly tricky business.
        for (int i = 0; i < levels.size(); i++) {
            InventoryLevel level = (InventoryLevel) levels.get(i);
            double invQty = level.getInventoryLevel();
            maxQty = Math.max(maxQty,invQty);
            double reorderQty = level.getReorderLevel();
            maxQty = Math.max(maxQty,reorderQty);
            if (level.getTargetLevel() != null) {
                maxQty = Math.max(maxQty,level.getTargetLevel().doubleValue());
            }
            if (level.getActivityType() != null){
                orgActs.add(level);
            }
        }

        offensiveQty = maxQty + 5;
        defensiveQty = offensiveQty * .25;
        offensiveQty = defensiveQty;

        for (int i = 0; i < orgActs.size(); i++) {
            InventoryLevel level = (InventoryLevel) orgActs.get(i);
            long startTime = level.getStartTime();
            long endTime = level.getEndTime();
            int startDay = (int) computeBucketFromTime(startTime);
            int endDay = (int) computeBucketFromTime(endTime);
            String actType = level.getActivityType();
            //xvalues[OFFENSIVE_SERIES_INDEX][i] = startDay;
            for (int j = startDay; j < endDay; j += bucketDays) {
                if(actType.trim().toLowerCase().equals("defensive")){
                  yvalues[DEFENSIVE_SERIES_INDEX][(j-minBucket) / bucketDays] = defensiveQty;
                }
                else {
                  yvalues[OFFENSIVE_SERIES_INDEX][(j-minBucket) / bucketDays] = offensiveQty;
                }
            }
        }
    }
}

