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

public class ShortfallChartDataModel
        extends InventoryBaseChartDataModel {

    protected RequisitionsChartDataModel reqModel;
    protected ProjectionsChartDataModel projModel;
    protected boolean shortfallExists;

    public static final String SHORTFALL_SERIES_LABEL = "Shortfall";
    public static final String SHORTFALL_LEGEND = "";

    public static final int REQUESTED_SERIES_INDEX = 0;
    public static final int ACTUAL_SERIES_INDEX = 1;


    public ShortfallChartDataModel() {
        inventory = null;
        logger = Logging.getLogger(this);
    }


    public ShortfallChartDataModel(RequisitionsChartDataModel reqDM,
                                   ProjectionsChartDataModel projDM) {
        this(SHORTFALL_LEGEND, reqDM, projDM);
    }


    public ShortfallChartDataModel(String theLegendTitle,
                                   RequisitionsChartDataModel reqDM,
                                   ProjectionsChartDataModel projDM) {
        inventory = null;
        legendTitle = theLegendTitle;
        reqModel = reqDM;
        projModel = projDM;
        nSeries = 1;
        seriesLabels = new String[nSeries];
        seriesLabels[0] = SHORTFALL_SERIES_LABEL;
        logger = Logging.getLogger(this);
        initValues();
    }

    public boolean isShortfall() {
        return shortfallExists;
    }


    public void setValues() {
        if (valuesSet) return;
        setShortfallValues();
        valuesSet = true;
    }

    public void setShortfallValues() {
        shortfallExists = false;
        if ((reqModel.getNumSeries() == 0) ||
                (projModel.getNumSeries() == 0)) {
            return;
        }
        nValues = reqModel.getXSeries(0).length;
        if (nValues != projModel.getXSeries(0).length) {
            return;
        }


        xvalues = new double[nSeries][];
        yvalues = new double[nSeries][nValues];

        xvalues[0] = reqModel.getXSeries(0);

        if (nValues == 0) {
            return;
        }

        yvalues[0][0] = ((reqModel.getYSeries(REQUESTED_SERIES_INDEX)[0] -
                reqModel.getYSeries(ACTUAL_SERIES_INDEX)[0]) +
                (projModel.getYSeries(REQUESTED_SERIES_INDEX)[0] -
                projModel.getYSeries(ACTUAL_SERIES_INDEX)[0]));
        int i;

        for (i = 1; i < nValues; i++) {
            yvalues[0][i] =
                    (((reqModel.getYSeries(REQUESTED_SERIES_INDEX)[i] -
                    reqModel.getYSeries(ACTUAL_SERIES_INDEX)[i]) +
                    (projModel.getRealYSeries(REQUESTED_SERIES_INDEX)[i] -
                    projModel.getRealYSeries(ACTUAL_SERIES_INDEX)[i])) +
                    yvalues[0][i - 1]);
            if (yvalues[0][i] > 0) {
                shortfallExists = true;
            }
            //get rid of negative shortfall
            else if (yvalues[0][i - 1] < 0) {
            //yvalues[0][i-1] = 0;
            }
        }

        if ((nValues > 1) &&
                (yvalues[0][nValues - 1] < 0)) {
            yvalues[0][nValues - 1] = 0;
        }

    }

    public void setDisplayCDay(boolean doUseCDay) {
        if (doUseCDay != useCDay) {
            useCDay = doUseCDay;
            reqModel.setDisplayCDay(useCDay);
            projModel.setDisplayCDay(useCDay);
            if (inventory != null) {
                resetInventory(inventory);
            }
        }
    }

}

