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

import java.util.Date;

import java.awt.Color;
import java.awt.Insets;

import com.klg.jclass.chart.JCChart;
import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.util.swing.JCExitFrame;
import com.klg.jclass.chart.JCSymbolStyle;

import com.klg.jclass.chart.ChartDataView;
import com.klg.jclass.chart.ChartDataViewSeries;
import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.ChartDataEvent;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.plugin.inventory.TimeUtils;


import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

/**
 * <pre>
 *
 * The InventoryBarChart class is an abstract class that
 * captures common attributes between the
 * InventoryDemandChart and the InventoryRefillChart.
 *
 * @see InventoryChart
 * @see InventoryShortfallChart
 * @see InventoryDemandChart
 * @see InventoryRefillChart
 *
 *
 **/

public abstract class InventoryBarChart extends InventoryShortfallChart {

    public InventoryBarChart(InventoryPreferenceData prefData) {
      super(prefData);
    }

    protected ChartDataView projChartDataView;
    protected ChartDataView reqChartDataView;

}
