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

import java.util.Date;

import java.awt.*;

import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.util.swing.JCExitFrame;

import com.klg.jclass.chart.*;


import org.cougaar.logistics.plugin.inventory.TimeUtils;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

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

public class InventoryLevelChart extends InventoryShortfallChart {

  protected OrgActivityChartDataModel orgActDM;

  protected ChartDataView invChartDataView;
  protected ChartDataView targetReorderChartDataView;
  protected ChartDataView orgChartDataView;


  protected boolean displayOrgActivities = true;

  public InventoryLevelChart(boolean initialDisplayCDay, InventoryPreferenceData prefData) {
    super(prefData);
    initialize("Inventory", initialDisplayCDay);
  }

  public ChartDataView getInvChartDataView() {
    return invChartDataView;
  }

  public ChartDataView getOrgChartDataView() {
    return orgChartDataView;
  }

  public void initialize(String title, boolean initialDisplayCDay) {
    super.initialize(title, initialDisplayCDay);
    chart.getLegend().setPreferredSize(new Dimension(148, 160));
  }


  public void initializeChart() {


    reqDM =
        new RequisitionsChartDataModel("",
                                       InventoryDemandChart.DEMAND_TASKS,
                                       InventoryDemandChart.DEMAND_ARS);

    projDM =
        new ProjectionsChartDataModel("Demand from Customers",
                                      InventoryDemandChart.DEMAND_PROJ_TASKS,
                                      InventoryDemandChart.DEMAND_PROJ_ARS,
                                      InventoryDemandChart.DEMAND_TASKS,
                                      reqDM,
                                      false);


    InventoryLevelChartDataModel idm = new InventoryLevelChartDataModel();
    TargetReorderLevelChartDataModel trdm = new TargetReorderLevelChartDataModel("");


    orgActDM = new OrgActivityChartDataModel();


    shortfallDM =
        new InventoryShortfallChartDataModel("",
                                             idm,
                                             reqDM,
                                             projDM);


    //These are never viewed, but should but are added to get all
    // the axis et al information.
    ChartDataView reqView = addChartView(JCChart.BAR, reqDM);
    ChartDataView projView = addChartView(JCChart.BAR, projDM);
    reqView.setVisible(false);
    projView.setVisible(false);

    orgChartDataView = addChartView(JCChart.BAR, orgActDM);
    orgChartDataView.setVisible(false);

    invChartDataView = addChartView(JCChart.BAR, idm);
    shortfallChartDataView = addChartView(JCChart.BAR, shortfallDM);
    //belowZeroDataView = addChartView(JCChart.BAR, belowZeroDM);
    targetReorderChartDataView = addChartView(JCChart.PLOT, trdm);

    String colorScheme = prefData.getColorScheme();
    setShortfallChartColors(colorScheme);
    setInventoryBarChartColors(colorScheme, orgChartDataView);
    //setBelowZeroChartColors(colorScheme);

    setInventoryBarChartColors(colorScheme, invChartDataView);
    setLineChartDataViewColors(targetReorderChartDataView);

    displayShortfall = false;
    updateShortfall();
    updateOrgActivities();

  }

  protected boolean setYAxisMinToZero() {
    return !displayShortfall;
  }

  public void setDisplayShortfall(boolean doDisplayShortfall) {
    if (doDisplayShortfall != displayShortfall) {
      super.setDisplayShortfall(doDisplayShortfall);
      resetYMin();
      //chart.reset();
    }
  }

  public boolean hasOrgActivities() {
    return orgActDM.hasOrgActivities();
  }

  public void updateOrgActivities() {
    if (hasOrgActivities()) {
      orgChartDataView.setVisible(displayOrgActivities);
      orgChartDataView.setName(InventoryLevelChartDataModel.INVENTORY_LEVEL_LEGEND);
      invChartDataView.setName("");
    } else {
      orgChartDataView.setVisible(false);
      orgChartDataView.setName("");
      invChartDataView.setName(InventoryLevelChartDataModel.INVENTORY_LEVEL_LEGEND);
    }
  }


  public void setData(InventoryData data) {
    super.setData(data);
    updateOrgActivities();
  }

  protected void setInventoryBarChartColors(String colorScheme, ChartDataView theView) {
    JCBarChartFormat format = (JCBarChartFormat) theView.getChartFormat();
    format.setClusterWidth(100);
    format.setClusterOverlap(100);

    theView.setOutlineColor(colorTable.getOutlineColor(colorScheme, theView));

    //chartDataView.setOutlineColor(Color.black);
    for (int j = 0; j < theView.getNumSeries(); j++) {
      ChartDataViewSeries series = theView.getSeries(j);
//      System.out.println("InventoryLevelChart setting bar chart colors for: " + series.getLabel());
      setSeriesColor(series, colorTable.get(colorScheme, series.getLabel()));
      int symbolStyle = colorTable.getSymbolShape(colorScheme, series.getLabel());
      if (symbolStyle != JCSymbolStyle.NONE) {
        series.getStyle().setSymbolShape(symbolStyle);
        series.getStyle().setSymbolColor(colorTable.getSymbolColor(colorScheme, series.getLabel()));
        series.getStyle().setSymbolSize(colorTable.getSymbolSize(colorScheme, series.getLabel()));
      }
    }
  }

  protected void setShortfallChartColors(String colorScheme) {
    if (shortfallChartDataView != null) {
      setInventoryBarChartColors(colorScheme, shortfallChartDataView);
    }
  }


  public void prefDataChanged(InventoryPreferenceData oldData,
                              InventoryPreferenceData newData) {
    super.prefDataChanged(oldData, newData);
    String colorScheme = newData.getColorScheme();
    if (!colorScheme.equals(oldData.getColorScheme())) {
      setInventoryBarChartColors(colorScheme, orgChartDataView);
      setInventoryBarChartColors(colorScheme, invChartDataView);
      setLineChartDataViewColors(targetReorderChartDataView);
    }
  }


  protected void setLineChartDataViewColors(ChartDataView chartDataView) {
    // set line width and colors
    for (int j = 0; j < chartDataView.getNumSeries(); j++) {
      ChartDataViewSeries series = chartDataView.getSeries(j);
      series.getStyle().setLineWidth(2);
      series.getStyle().setSymbolShape(JCSymbolStyle.NONE);
      setSeriesColor(series, colorTable.get(colorScheme, series.getLabel()));
      int width = colorTable.getLineWidth(colorScheme, series.getLabel());
      series.getStyle().setLineWidth(width);
      int symbolStyle = colorTable.getSymbolShape(colorScheme, series.getLabel());
      if (symbolStyle != JCSymbolStyle.NONE) {
        series.getStyle().setSymbolShape(symbolStyle);
        series.getStyle().setSymbolColor(colorTable.getSymbolColor(colorScheme, series.getLabel()));
        series.getStyle().setSymbolSize(colorTable.getSymbolSize(colorScheme, series.getLabel()));
      }
    }

  }
}
