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


/**
 * <pre>
 *
 * The ShortfallChartDataModel is the ChartDataModel for the
 * calculation of shortfall lines from chartdatamodels that
 * contain requested and allocation results displayed in their
 * y coordinates.
 *
 * @see InventoryBaseChartDataModel
 * @see RequisitionsChartDataModel
 * @see ProjectionsChartDataModel
 *
 **/

public class InventoryShortfallChartDataModel
    extends ShortfallChartDataModel {

  protected InventoryLevelChartDataModel invModel;

  public final static int INVENTORY_SHORTFALL_SERIES_INDEX = 0;

  private double minShortfall;


  public InventoryShortfallChartDataModel(InventoryLevelChartDataModel invDM,
                                          RequisitionsChartDataModel reqDM,
                                          ProjectionsChartDataModel projDM) {
    this(SHORTFALL_LEGEND, invDM, reqDM, projDM);
  }


  public InventoryShortfallChartDataModel(String theLegendTitle,
                                          InventoryLevelChartDataModel invDM,
                                          RequisitionsChartDataModel reqDM,
                                          ProjectionsChartDataModel projDM) {
    inventory = null;
    legendTitle = theLegendTitle;
    invModel = invDM;
    reqModel = reqDM;
    projModel = projDM;
    nSeries = 1;
    seriesLabels = new String[nSeries];
    seriesLabels[INVENTORY_SHORTFALL_SERIES_INDEX] = SHORTFALL_SERIES_LABEL;
    logger = Logging.getLogger(this);
    initValues();
  }


  public void setShortfallValues() {
    shortfallExists = false;
    if ((reqModel.getNumSeries() == 0) ||
        (projModel.getNumSeries() == 0) ||
        (invModel.getNumSeries() == 0)) {
      return;
    }
    nValues = reqModel.getXSeries(0).length;
    if (nValues != projModel.getXSeries(0).length) {
      logger.debug("Whoa! Different number of values between reqModel " + nValues +
                                 " and projModel! " + projModel.getXSeries(0).length);
    }


    xvalues = new double[nSeries][];
    yvalues = new double[nSeries][nValues];

    xvalues[INVENTORY_SHORTFALL_SERIES_INDEX] = reqModel.getXSeries(0);

    if (nValues == 0) {
      return;
    }

    minShortfall = 0;

    double tolerance = 0.01;

    if (invModel.getYSeries(0)[0] - tolerance <= 0) {
      yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][0] = ((reqModel.getYSeries(ACTUAL_SERIES_INDEX)[0] -
          reqModel.getYSeries(REQUESTED_SERIES_INDEX)[0]) +
          (projModel.getRealYSeries(ACTUAL_SERIES_INDEX)[0] -
          projModel.getRealYSeries(REQUESTED_SERIES_INDEX)[0]));

    } else {
      yvalues[0][0] = 0;
    }
    int i;

    for (i = 1; i < nValues; i++) {
      if (invModel.getYSeries(0)[i] - tolerance <= 0) {
        yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i] = ((reqModel.getYSeries(ACTUAL_SERIES_INDEX)[i] -
            reqModel.getYSeries(REQUESTED_SERIES_INDEX)[i]) +
            (projModel.getRealYSeries(ACTUAL_SERIES_INDEX)[i] -
            projModel.getRealYSeries(REQUESTED_SERIES_INDEX)[i])
            + yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i - 1]);


      } else {
        yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i] = 0;
      }
      if (yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i] < 0) {
        shortfallExists = true;
        logger.debug("Shortfall value point(" + xvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i] + "," + yvalues[0][i] + ")");
	minShortfall = Math.min(minShortfall,yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i]);
      }
      //get rid of negative shortfall
      else if (yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][i - 1] > 0) {
        //yvalues[0][i-1] = 0;
      }
    }

    if ((nValues > 1) &&
        (yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][nValues - 1] > 0)) {
      yvalues[INVENTORY_SHORTFALL_SERIES_INDEX][nValues - 1] = 0;
    }

  }

  public void setDisplayCDay(boolean doUseCDay) {
        if (doUseCDay != useCDay) {
            invModel.setDisplayCDay(doUseCDay);
            super.setDisplayCDay(doUseCDay);
        }
    }

    public double getMinShortfall() { return minShortfall; }

}

