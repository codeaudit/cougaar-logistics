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
 * The InventoryLevelChartDataModel is a ChartDataModel for the
 * InventoryLevelChart.    It calculates the inventory
 * levels for puts them in x and y coordinates.
 *
 *
 * @see InventoryBaseChartDataModel
 * @see TargetReorderLevelChartDataModel
 *
 **/

public class InventoryLevelChartDataModel
    extends InventoryBaseChartDataModel {


  public static final String INVENTORY_LEVEL_SERIES_LABEL = "Inventory Level";

  public static final String INVENTORY_LEVEL_LEGEND = "Inventory Key Levels";

  public InventoryLevelChartDataModel() {
    this(null, INVENTORY_LEVEL_LEGEND);
  }

  public InventoryLevelChartDataModel(String legendTitle) {
    this(null, legendTitle);
  }


  public InventoryLevelChartDataModel(InventoryData data,
                                      String theLegendTitle) {
    inventory = data;
    legendTitle = theLegendTitle;
    nSeries = 1;
    seriesLabels = new String[nSeries];
    seriesLabels[0] = INVENTORY_LEVEL_SERIES_LABEL;
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
    for (int i = 0; i < nSeries ; i++) {
      xvalues[i] = new double[nValues];
      yvalues[i] = new double[nValues];
      for (int j = 0; j < nValues; j++) {
        xvalues[i][j] = minBucket + (j * bucketDays);
        yvalues[i][j] = 0;
      }
    }

    ArrayList targetLevels = new ArrayList();
    ArrayList orgActLevels = new ArrayList();

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
      Double invQty = level.getInventoryLevel();
      for (int j = startBucket; j <= endBucket; j += bucketDays) {
        if (invQty != null) {
          try {
          yvalues[0][(j - minBucket) / bucketDays] = (invQty.doubleValue() * unitFactor);
          }
          catch(java.lang.NullPointerException e) {
            System.out.println("Yikes NPE! " + invQty + " " + invQty.doubleValue() + " unit Factor: " + unitFactor);
            throw e;
          }
        }

      }
    }
  }


}

