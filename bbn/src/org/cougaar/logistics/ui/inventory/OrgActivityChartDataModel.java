/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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


  protected double offensiveQty = 0.0;
  protected double defensiveQty = 0.0;

  protected ArrayList orgActs;

  public static final int OFFENSIVE_SERIES_INDEX = 0;
  public static final int DEFENSIVE_SERIES_INDEX = 1;

  public static final String ORG_ACTIVITY_SERIES_LABEL = "Activity Level";

  public static final String OFFENSIVE_SERIES_LABEL = "Offensive";
  public static final String DEFENSIVE_SERIES_LABEL = "Defensive";

  public static final String ORG_ACTIVITY_LEGEND = "";

  public OrgActivityChartDataModel() {
    this(null, ORG_ACTIVITY_LEGEND);
  }

  public OrgActivityChartDataModel(String legendTitle) {
    this(null, legendTitle);
  }

  //Subclass needed a no arg construtor that did nothing but that 
  //was taken above.
  //The below constructor was made to prevent the subclass from calling
  //the no arg constructor automatically.
  //This allowed me to call this one explicitly from the subclass.
  public OrgActivityChartDataModel(String x,String y,String z ) {
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
    orgActs = new ArrayList();
    initValues();
  }


  public String getActivityFromLevel(double value) {
    if (value == defensiveQty) {
      return "Defensive";
    } else {
      return "Offensive";
    }
  }


  public double getLevelFromActivity(String value) {
    if (value.trim().toLowerCase().equals("defensive")) {
      return defensiveQty;
    } else {
      return offensiveQty;
    }
  }

  public void setValues() {
    if (valuesSet) return;
    setInventoryValues();
    valuesSet = true;
  }

  public boolean hasOrgActivities() {
    return !orgActs.isEmpty();
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

    orgActs.clear();

    double maxQty = 0;

    //Need to add target level which is a little more complicated
    //than you think.  We don't know how many values there are
    //in this third series. We have to add them if they are non null
    //to a vector, allocated the third series same length as the
    //vector and put them into there. mildly tricky business.
    for (int i = 0; i < levels.size(); i++) {
      InventoryLevel level = (InventoryLevel) levels.get(i);
      Double invQty = level.getInventoryLevel();
      if (invQty != null) {
        maxQty = Math.max(maxQty, (invQty.doubleValue() * unitFactor));
      }
      double reorderQty = level.getReorderLevel();
      maxQty = Math.max(maxQty, reorderQty);
      if (level.getTargetLevel() != null) {
        maxQty = Math.max(maxQty, level.getTargetLevel().doubleValue());
      }
      if (level.getActivityType() != null) {
        orgActs.add(level);
      }
    }


    offensiveQty = maxQty * 1.15;
    //offensiveQty = maxQty + 5;
    //defensiveQty = offensiveQty * .25;
    //offensiveQty = defensiveQty;
    defensiveQty = offensiveQty;

    for (int i = 0; i < orgActs.size(); i++) {
      InventoryLevel level = (InventoryLevel) orgActs.get(i);
      long startTime = level.getStartTime();
      long endTime = level.getEndTime();
      int startDay = (int) computeBucketFromTime(startTime);
      int endDay = (int) computeBucketFromTime(endTime);
      String actType = level.getActivityType();
      //xvalues[OFFENSIVE_SERIES_INDEX][i] = startDay;
      for (int j = startDay; j < endDay; j += bucketDays) {
        if (actType.trim().toLowerCase().equals("defensive")) {
          yvalues[DEFENSIVE_SERIES_INDEX][(j - minBucket) / bucketDays] = defensiveQty;
        } else {
          yvalues[OFFENSIVE_SERIES_INDEX][(j - minBucket) / bucketDays] = offensiveQty;
        }
      }
    }
  }
}

