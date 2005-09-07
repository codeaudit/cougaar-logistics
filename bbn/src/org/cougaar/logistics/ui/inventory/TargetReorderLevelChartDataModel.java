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
 * The TargetReorderLevelChartDataModel is a ChartDataModel for the
 * InventoryLevelChart.    It calculates the
 * target and reorder levels for puts them in x and y coordinates.
 *
 *
 * @see InventoryBaseChartDataModel
 * @see InventoryLevelChartDataModel
 *
 **/

public class TargetReorderLevelChartDataModel
    extends InventoryBaseChartDataModel {


  public static final int REORDER_LEVEL_SERIES_INDEX = 0;
  public static final int TARGET_LEVEL_SERIES_INDEX = 1;


  public static final String TARGET_LEVEL_SERIES_LABEL = "Target Level";
  public static final String REORDER_LEVEL_SERIES_LABEL = "Reorder Level";

  //public static final String SYMBOL = " Symbol";

  public static final String TARGET_REORDER_LEVEL_LEGEND = "Target And Reorder Levels";

  public TargetReorderLevelChartDataModel() {
    this(null, TARGET_REORDER_LEVEL_LEGEND);
  }

  public TargetReorderLevelChartDataModel(String legendTitle) {
    this(null, legendTitle);
  }


  public TargetReorderLevelChartDataModel(InventoryData data,
                                          String theLegendTitle) {
    inventory = data;
    legendTitle = theLegendTitle;
    nSeries = 2;
    seriesLabels = new String[nSeries];
    seriesLabels[0] = REORDER_LEVEL_SERIES_LABEL;
    seriesLabels[1] = TARGET_LEVEL_SERIES_LABEL;
    logger = Logging.getLogger(this);
    initValues();
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
    for (int i = 0; i < (nSeries - 1); i++) {
      xvalues[i] = new double[nValues];
      yvalues[i] = new double[nValues];
      for (int j = 0; j < nValues; j++) {
        xvalues[i][j] = minBucket + (j * bucketDays);
        yvalues[i][j] = 0;
      }
    }

    ArrayList targetLevels = new ArrayList();

    //Need to add target level which is a little more complicated
    //than you think.  We don't know how many values there are
    //in this third series. We have to add them if they are non null
    //to a vector, allocated the third series same length as the
    //vector and put them into there. mildly tricky business.
    for (int i = 0; i < levels.size(); i++) {
      InventoryLevel level = (InventoryLevel) levels.get(i);
      long startTime = level.getStartTime();
      long endTime = level.getEndTime();
      int startBucket = (int) computeBucketFromTime(startTime);
      int endBucket = (int) computeBucketFromTime(endTime);
      double reorderQty = level.getReorderLevel();
      for (int j = startBucket; j <= endBucket; j += bucketDays) {
        yvalues[0][(j - minBucket) / bucketDays] = (reorderQty * unitFactor);
      }
      if (level.getTargetLevel() != null) {
        targetLevels.add(level);
      }
    }

    xvalues[1] = new double[targetLevels.size()];
    yvalues[1] = new double[targetLevels.size()];

    for (int i = 0; i < targetLevels.size(); i++) {
      InventoryLevel level = (InventoryLevel) targetLevels.get(i);
      long startTime = level.getStartTime();
      int startDay = (int) computeBucketFromTime(startTime);
      double targetLevel = level.getTargetLevel().doubleValue();
      xvalues[1][i] = startDay;
      yvalues[1][i] = (targetLevel * unitFactor);
    }
  }
}

