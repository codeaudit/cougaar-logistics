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

import java.awt.Event;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.util.swing.JCExitFrame;

import com.klg.jclass.chart.*;


import org.cougaar.logistics.plugin.inventory.TimeUtils;

import org.cougaar.logistics.ui.inventory.data.InventoryData;

/**
 * <pre>
 *
 * The InventoryChart class is the base class for all Inventory
 * charts displayed in the GUI.   It contains all the shared
 * behavior of all charts in the GUI.
 *
 *
 *
 **/

public class InventoryLevelChart extends InventoryChart {

  protected ChartDataView invChartDataView;
  protected ChartDataView orgChartDataView;

  public InventoryLevelChart() {
    initialize("Inventory");
  }

  public ChartDataView getInvChartDataView() { return invChartDataView; }
  public ChartDataView getOrgChartDataView() { return orgChartDataView; }



  public void initializeChart() {

    InventoryLevelChartDataModel idm = new InventoryLevelChartDataModel();
    invChartDataView = addChartView(JCChart.PLOT, idm);

    OrgActivityChartDataModel oadm = new OrgActivityChartDataModel();
    orgChartDataView = addChartView(JCChart.BAR, oadm);

    JCBarChartFormat format = (JCBarChartFormat)orgChartDataView.getChartFormat();
    format.setClusterWidth(100);
    format.setClusterOverlap(100);

    orgChartDataView.setOutlineColor(Color.gray);

    //chartDataView.setOutlineColor(Color.black);
    for (int j = 0; j < orgChartDataView.getNumSeries(); j++) {
      ChartDataViewSeries series = orgChartDataView.getSeries(j);
      setSeriesColor(series, colorTable.get(series.getLabel()));
      Color symbolColor = colorTable.get(series.getLabel() + "_SYMBOL");
      if (symbolColor != null) {
        series.getStyle().setSymbolColor(symbolColor);
      }

    }



// set line width
    for (int j = 0; j < invChartDataView.getNumSeries(); j++) {
      ChartDataViewSeries series = invChartDataView.getSeries(j);
      series.getStyle().setLineWidth(2);
      series.getStyle().setSymbolShape(JCSymbolStyle.NONE);
      setSeriesColor(series, colorTable.get(series.getLabel()));
      if (series.getLabel().equals(idm.TARGET_LEVEL_SERIES_LABEL)) {
        setSeriesColor(series, Color.yellow);
        series.getStyle().setSymbolShape(JCSymbolStyle.VERT_LINE);
        series.getStyle().setSymbolColor(colorTable.get(series.getLabel() + "_SYMBOL"));
      }

    }

  }

}
