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


/** 
 * <pre>
 * 
 * The ShortfallChartDataModel is the ChartDataModel for the 
 * calculation of shortfall lines from chartdatamodels that 
 * contain requested and allocation results displayed in their
 * y coordinates.
 * 
 * @see InventoryBaseChartDataModel
 * @see RequistionsChartDataModel
 * @see ProjectionsChartDataModel
 *
 **/

public class ShortfallChartDataModel 
            extends InventoryBaseChartDataModel {

    protected RequisitionsChartDataModel reqModel;
    protected ProjectionsChartDataModel  projModel;
    protected boolean shortfallExists;

    public static final String SHORTFALL_SERIES_LABEL="Shortfall";
    public static final String SHORTFALL_LEGEND="";

    public static final int REQUESTED_SERIES_INDEX = 0;
    public static final int ACTUAL_SERIES_INDEX = 1;


    public ShortfallChartDataModel(RequisitionsChartDataModel reqDM,
				   ProjectionsChartDataModel projDM) {
	this(SHORTFALL_LEGEND,reqDM,projDM);
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

    public boolean isShortfall() { return shortfallExists; }


    public void setValues() {
	if(valuesSet) return;
	setShortfallValues();
	valuesSet = true;
    }

    public void setShortfallValues() {
	shortfallExists = false;
	if((reqModel.getNumSeries() == 0) ||
	   (projModel.getNumSeries() == 0)) {
	    return;
	}
	nValues = reqModel.getXSeries(0).length;
	if(nValues != projModel.getXSeries(0).length) {
	    return;
	}
	

	xvalues = new double[nSeries][];
	yvalues = new double[nSeries][nValues];

	xvalues[0] = reqModel.getXSeries(0);

	if(nValues == 0) { return; }

	yvalues[0][0] = ((reqModel.getYSeries(REQUESTED_SERIES_INDEX)[0] -
			  reqModel.getYSeries(ACTUAL_SERIES_INDEX)[0]) +
			 (projModel.getYSeries(REQUESTED_SERIES_INDEX)[0] -
			  projModel.getYSeries(ACTUAL_SERIES_INDEX)[0]));

	for(int i=1; i < nValues ; i++) {
	    yvalues[0][i] = 
	    (((reqModel.getYSeries(REQUESTED_SERIES_INDEX)[i] -
	       reqModel.getYSeries(ACTUAL_SERIES_INDEX)[i]) +
	      (projModel.getRealYSeries(REQUESTED_SERIES_INDEX)[i] -
	       projModel.getRealYSeries(ACTUAL_SERIES_INDEX)[i])) +
	     yvalues[0][i-1]);
	    if(yvalues[0][i] > 0) {
		shortfallExists = true;
	    }
	    else if(yvalues[0][i] < 0) {
		yvalues[0][i] = 0;
	    }
	}
    }

    public void setDisplayCDay(boolean doUseCDay) {
	if(doUseCDay != useCDay) {
	    useCDay = doUseCDay;
	    reqModel.setDisplayCDay(useCDay);
	    projModel.setDisplayCDay(useCDay);
	    if(inventory != null) { 
		resetInventory(inventory);
	    }
	}
    }
}

