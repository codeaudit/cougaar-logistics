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
import com.klg.jclass.chart.JCFillStyle;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.plugin.inventory.TimeUtils;


import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;

/**
 * <pre>
 *
 * The InventoryShortfallChart class is an abstract class that
 * captures common behavior between the
 * all charts that display shortfall.
 *
 * @see InventoryChart
 * @see InventoryDemandChart
 * @see InventoryRefillChart
 *
 *
 **/

public abstract class InventoryShortfallChart extends InventoryChart {

  protected RequisitionsChartDataModel reqDM;
  protected ProjectionsChartDataModel projDM;
  protected ChartDataView shortfallChartDataView;

  protected ShortfallChartDataModel shortfallDM;

  protected boolean displayShortfall = false;

  public InventoryShortfallChart(InventoryPreferenceData prefData) {
    super(prefData);
  }

  public void setData(InventoryData data) {
    inventory = data;
    if (data.getBucketSize() != currBucketSize) {
      resetAxes();
    }
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);
      InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();
      dataModel.resetInventory(data);
    }
    updateShortfall();
    myYAxisTitle = (conversionTable.getConversionForData(data, prefData)).getUnit();
    chart.reset();
    resetAxes();

  }

  public void updateShortfall() {
    if (isShortfall()) {
      shortfallChartDataView.setVisible(displayShortfall);
    } else {
      shortfallChartDataView.setVisible(false);
    }
  }

  public boolean isShortfall() {
    if (shortfallDM == null) {
      return false;
    }
    return shortfallDM.isShortfall();
  }

  public void setDisplayShortfall(boolean doDisplayShortfall) {
    if (doDisplayShortfall != displayShortfall) {
      displayShortfall = doDisplayShortfall;
      updateShortfall();
    }
  }


  protected void setShortfallChartColors(String colorScheme) {
    if (shortfallChartDataView != null) {
      for (int j = 0; j < shortfallChartDataView.getNumSeries(); j++) {
        ChartDataViewSeries series = shortfallChartDataView.getSeries(j);
        series.getStyle().setLineWidth(2);
        series.getStyle().setSymbolShape(JCSymbolStyle.NONE);
//                System.out.println("InventoryShortfallChart: setting shortfall colors for: " + series.getLabel());
        setSeriesColor(series, colorTable.get(colorScheme, series.getLabel()));
        int width = colorTable.getLineWidth(colorScheme, series.getLabel());
        series.getStyle().setLineWidth(width);
      }
    }
  }

  protected void setBarChartColors(String colorScheme) {
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);
      if (chartDataView.getChartType() == JCChart.BAR) {
        chartDataView.setOutlineColor(Color.black);
        for (int j = 0; j < chartDataView.getNumSeries(); j++) {
          ChartDataViewSeries series = chartDataView.getSeries(j);
//          System.out.println("InventoryShortfallChart setting bar chart colors for: " + series.getLabel());
          setSeriesColor(series, colorTable.get(colorScheme, series.getLabel()));
          int pattern = colorTable.getFillPattern(colorScheme, series.getLabel());
          series.getStyle().setFillPattern(pattern);
          if (pattern != JCFillStyle.SOLID)
            series.getStyle().setFillBackground(colorTable.getBackgroundColor(colorScheme, series.getLabel()));
          int symbolStyle = colorTable.getSymbolShape(colorScheme,series.getLabel());
          if(symbolStyle != JCSymbolStyle.NONE){
            series.getStyle().setSymbolShape(symbolStyle);
            series.getStyle().setSymbolColor(colorTable.getSymbolColor(colorScheme,series.getLabel()));
            series.getStyle().setSymbolSize(colorTable.getSymbolSize(colorScheme,series.getLabel()));
          }
        }
      }
    }
  }

  public void prefDataChanged(InventoryPreferenceData oldData,
                              InventoryPreferenceData newData) {
    super.prefDataChanged(oldData, newData);
    String colorScheme = newData.getColorScheme();
    if (!colorScheme.equals(oldData.getColorScheme())) {
      setShortfallChartColors(colorScheme);
      setBarChartColors(colorScheme);
    }
  }

}
