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

import com.klg.jclass.chart.JCChart;
import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.util.swing.JCExitFrame;
import com.klg.jclass.chart.JCFillStyle;
import com.klg.jclass.chart.JCAxis;
import com.klg.jclass.chart.JCAxisTitle;
import com.klg.jclass.chart.JCPickListener;
import com.klg.jclass.chart.EventTrigger;
import com.klg.jclass.chart.JCChartListener;

import com.klg.jclass.util.legend.JCLegend;
import com.klg.jclass.util.legend.JCMultiColLegend;

import com.klg.jclass.chart.ChartDataView;
import com.klg.jclass.chart.ChartDataViewSeries;
import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.ChartDataEvent;
import com.klg.jclass.chart.ChartText;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;
import org.cougaar.logistics.plugin.inventory.TimeUtils;

/**
 * <pre>
 * <p/>
 * The InventoryChart class is the base class for all Inventory
 * charts displayed in the GUI.   It contains all the shared
 * behavior of all charts in the GUI.
 */

public abstract class InventoryChart extends JPanel {

  private final static long MILLIS_IN_DAY = TimeUtils.MSEC_PER_DAY;

  protected static InventoryUnitConversionTable conversionTable = null;

  protected JCChart chart = null;
  protected InventoryData inventory = null;

  protected boolean displayCDay = false;
  protected String myYAxisTitle;
  protected String myTitle;

  protected int viewIndex;

  protected Logger logger;

  protected long currBucketSize = MILLIS_IN_DAY;

  protected InventoryPreferenceData prefData;
  protected InventoryColorTable colorTable;
  protected String colorScheme;

  public InventoryChart() {

  }

  public InventoryChart(InventoryPreferenceData prefData) {
    this.prefData = prefData;
    colorTable = prefData.getColorTable();
    colorScheme = prefData.getColorScheme();
  }

  public abstract void initializeChart();

  public void initialize(String title, boolean initialDisplayCDay) {
    int gridx = 0;
    int gridy = 0;

    viewIndex = 0;

    logger = Logging.getLogger(this);

    displayCDay = initialDisplayCDay;

    Insets blankInsets = new Insets(0, 0, 0, 0);

    myTitle = title;
    chart = new JCChart();
    initializeChart();
    customizeAxes("");

    if (myTitle != null) {
      chart.setHeader(new JLabel(myTitle));
    }

// allow user to zoom in on chart
    chart.setTrigger(0, new EventTrigger(0, EventTrigger.ZOOM));

    // allow interactive customization using left shift click
    chart.setAllowUserChanges(true);
    chart.setTrigger(1, new EventTrigger(Event.SHIFT_MASK,
            EventTrigger.CUSTOMIZE));

    // allow user to display labels using right mouse click
    chart.setTrigger(1, new EventTrigger(Event.META_MASK, EventTrigger.PICK));

    // set header and legend to black
    // set beveled borders around plot area and chart
    ((JLabel) chart.getHeader()).setForeground(Color.black);
    chart.setBorder(BorderFactory.createLoweredBevelBorder());
    chart.getChartArea().setBorder(BorderFactory.createLoweredBevelBorder());

    // add chart to panel
    setLayout(new GridBagLayout());
    chart.getHeader().setVisible(false);
    add(chart, new GridBagConstraints(gridx, gridy++, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            blankInsets, 0, 0));


    JCMultiColLegend legend = new JCMultiColLegend();
    legend.setNumColumns(1);
    chart.setLegend(legend);

    // set legend invisible, because we create our own??
    chart.getLegend().setVisible(true);
    chart.getLegend().setPreferredSize(new Dimension(148, 110)); //95

  }

  public JCChart getChart() {
    return chart;
  }

  public void addChartListener(JCChartListener listener) {
    chart.addChartListener(listener);
  }

  public void removeChartListener(JCChartListener listener) {
    chart.removeChartListener(listener);
  }

  public void addPickListener(JCPickListener listener) {
    chart.addPickListener(listener);
  }

  public void removePickListener(JCPickListener listener) {
    chart.removePickListener(listener);
  }

  public JCAxis getFirstXAxis() {
    java.util.List viewList = chart.getDataView();
    if (viewList.size() > 0) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(0);
      return chartDataView.getXAxis();
    }
    return null;
  }

  protected void resetYMin() {
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);
      JCAxis yaxis = chartDataView.getYAxis();
      if (setYAxisMinToZero()) {
        yaxis.setMin(0);
        yaxis.setMinIsDefault(false);
      } else {
        yaxis.setMinIsDefault(true);
      }
    }
  }

  public void resetAxes() {

    long baseCDayTime = 0L;
    if (inventory != null) {
      baseCDayTime = inventory.getStartCDay();
      currBucketSize = inventory.getBucketSize();
    }
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);

      // use time axis for x axis
      JCAxis xaxis = chartDataView.getXAxis();

      if (currBucketSize < MILLIS_IN_DAY) {
        if (displayCDay) {
          xaxis.setAnnotationMethod(JCAxis.VALUE);
          xaxis.setPrecision(0);
          xaxis.setTickSpacing(7);
          xaxis.setGridSpacing(7);
          xaxis.setGridVisible(true);
          xaxis.setAnnotationRotation(JCAxis.ROTATE_NONE);

          /***
           xaxis.setAnnotationMethod(JCAxis.TIME_LABELS);
           xaxis.setTimeBase(new Date(0));
           xaxis.setTimeUnit(JCAxis.HOURS);
           xaxis.setTickSpacing(24);
           xaxis.setGridSpacing(24);
           xaxis.setTimeFormat("'CD H");
           xaxis.setGridVisible(true);
           xaxis.setAnnotationRotation(JCAxis.ROTATE_270);
           **/
        } else {
          xaxis.setAnnotationMethod(JCAxis.TIME_LABELS);
          xaxis.setTimeBase(new Date(InventoryChartBaseCalendar.getBaseTime()));

          xaxis.setTimeUnit(JCAxis.HOURS);
          xaxis.setTickSpacing(24);
          xaxis.setGridSpacing(24);
          xaxis.setTimeFormat("M/d H");

          /*
           *if(logger.isDebugEnabled()) {
           *logger.debug("Base Date is: " + new Date(InventoryChartBaseCalendar.getBaseTime()));
           *}
           */
          xaxis.setGridVisible(true);
          xaxis.setAnnotationRotation(JCAxis.ROTATE_270);
        }

      } else {
        if (displayCDay) {
          xaxis.setAnnotationMethod(JCAxis.VALUE);
          xaxis.setPrecision(0);

          //xaxis.setAnnotationMethod(JCAxis.TIME_LABELS);
          //xaxis.setTimeUnit(JCAxis.DAYS);
          //xaxis.setTimeBase(new Date(InventoryChartBaseCalendar.getBaseTime() - baseCDayTime ));
          //xaxis.setTimeFormat("D");

          xaxis.setTickSpacing(7);
          xaxis.setGridSpacing(7);
          //		xaxis.setTickSpacingIsDefault(true);
          //              xaxis.setGridSpacingIsDefault(true);
          //xaxis.setMin(CDAY_MIN);
          xaxis.setGridVisible(true);
          xaxis.setAnnotationRotation(JCAxis.ROTATE_NONE);
        } else {
          xaxis.setAnnotationMethod(JCAxis.TIME_LABELS);
          xaxis.setTimeBase(new Date(InventoryChartBaseCalendar.getBaseTime()));

          xaxis.setTimeUnit(JCAxis.DAYS);
          xaxis.setTickSpacing(7);
          xaxis.setGridSpacing(7);
          xaxis.setTimeFormat("M/d");

          /*
           *if(logger.isDebugEnabled()) {
           *logger.debug("Base Date is: " + new Date(InventoryChartBaseCalendar.getBaseTime()));
           *}
           */
          xaxis.setGridVisible(true);
          xaxis.setAnnotationRotation(JCAxis.ROTATE_270);
        }
      }

      // y axis titles
      JCAxis yaxis = chartDataView.getYAxis();
      yaxis.setGridVisible(true);
      JCAxisTitle yAxisTitle = new JCAxisTitle(myYAxisTitle);
      yAxisTitle.setRotation(ChartText.DEG_270);
      yAxisTitle.setPlacement(JCLegend.WEST);
      yaxis.setTitle(yAxisTitle);

      if (setYAxisMinToZero()) {
        yaxis.setMin(0);
        yaxis.setMinIsDefault(false);
      } else {
        yaxis.setMinIsDefault(true);
      }
      //       yaxis.setEditable(false); // don't allow zoomin on this axis
      //(Math.round(getYMin())); // set zero as the minimum value on the axis
    }
  }

  /**
   * paintNowBar creates a horizontal line on any chart given a now time.    This was needed by multiple charts
   * in LAT.   Rather than copy it to several places the base chart was the best place where it belonged - here.
   *  It is a NO-OP here as it is not called in albbn and getNowTime() always is less than 0.
   * @param g - A Graphics Context.
   */

  public void paintNowBar(Graphics g) {

    if (inventory == null || (inventory.getNowTime() < 0)) {
      return;
    }
    long nowTime  = inventory.getNowTime() - (30 * 60 * 1000);
    Date now = new Date(nowTime);
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);

      InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();

      int bucketTime = dataModel.computeBucketFromTime(nowTime);


      // use time axis for x axis
      JCAxis xaxis = chartDataView.getXAxis();

      Rectangle boundingRect = xaxis.getDrawingArea();
      int boundsX = new Double(boundingRect.getX()).intValue();
      int boundsY = new Double(boundingRect.getY()).intValue();
      int width = new Double(boundingRect.getWidth()).intValue();
      int height = new Double(boundingRect.getHeight()).intValue();


      double origin = xaxis.getOrigin();


      double factor = 0.8d;
      int spanFactor = (new Double((xaxis.getMax() - xaxis.getMin()) * factor)).intValue();

      double value = 0.0d;
      int nowPixel = 0;

      if (displayCDay) {
        value = bucketTime;
        nowPixel = xaxis.toPixel(value) + spanFactor;
      } else {
        value = xaxis.dateToValue(now);
        nowPixel = xaxis.toPixel(value) + xaxis.toPixel(origin);
      }

      /**
      *logger.warn("\nOrigin is " + origin + "\n and value is " + value + " \nand bucketTime is " + bucketTime +
      *        " \nand finally now Pixel is " + nowPixel);
      */

      Color origColor = g.getColor();
      g.setColor(Color.BLUE);

      //g.drawRect(boundsX, boundsY, width, height);

      boundingRect = this.getBounds();
      boundsX = new Double(boundingRect.getX()).intValue();
      boundsY = new Double(boundingRect.getY()).intValue();
      width = new Double(boundingRect.getWidth()).intValue();
      height = new Double(boundingRect.getHeight()).intValue();


      nowPixel = nowPixel - 1;
      g.drawLine(nowPixel, boundsY, nowPixel, boundsY + height);
      nowPixel++;
      g.drawLine(nowPixel, boundsY, nowPixel, boundsY + height);
      nowPixel++;
      g.drawLine(nowPixel, boundsY, nowPixel, boundsY + height);

      g.setColor(origColor);
    }
  }

  protected void customizeAxes(String yAxisTitleText) {
    myYAxisTitle = yAxisTitleText;
    resetAxes();
  }

  protected boolean setYAxisMinToZero() {
    return true;
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
    myYAxisTitle = (conversionTable.getConversionForData(data, prefData)).getUnit();
    chart.reset();
    resetAxes();
  }

  public void setDisplayCDay(boolean doUseCDay) {
    displayCDay = doUseCDay;
    java.util.List viewList = chart.getDataView();
    for (int i = viewList.size() - 1; i >= 0; i--) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);
      InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();
      dataModel.setDisplayCDay(displayCDay);
    }
    chart.reset();
    resetAxes();
  }


  protected ChartDataView addChartView(int chartType,
                                       InventoryBaseChartDataModel dm) {

    ChartDataView chartDataView = new ChartDataView();
    chartDataView.setChartType(chartType);
    dm.setInitialDisplayCDay(displayCDay);
    chartDataView.setDataSource(dm);
    chart.setDataView(viewIndex, chartDataView);
    viewIndex++;


    return chartDataView;
  }

  protected int getChartViewIndex(ChartDataView chartDataView) {
    for (int i = 0; i < viewIndex; i++) {
      if (chartDataView == chart.getDataView(i)) {
        return i;
      }
    }
    return -1;
  }

  protected void removeChartView(ChartDataView chartDataView) {
    int chartIndex = getChartViewIndex(chartDataView);
    if (chartIndex != -1) {
      chart.removeDataView(chartIndex);
    }
    viewIndex--;
  }

  protected void setSeriesColor(ChartDataViewSeries series, Color color) {
    series.getStyle().setLineColor(color);
    series.getStyle().setFillColor(color);
    series.getStyle().setSymbolColor(color);
  }


  public void setXZoom(double start, double end) {
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);
      // use time axis for x axis
      JCAxis xaxis = chartDataView.getXAxis();
      chart.zoom(start, end, xaxis, true);
    }
  }

  public void prefDataChanged(InventoryPreferenceData origData,
                              InventoryPreferenceData newData) {
    java.util.List viewList = chart.getDataView();
    for (int i = 0; i < viewList.size(); i++) {
      ChartDataView chartDataView = (ChartDataView) viewList.get(i);
      InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();
      dataModel.prefDataChanged(origData, newData);
    }
    prefData = newData;
    colorTable = prefData.getColorTable();
    colorScheme = prefData.getColorScheme();
  }

  public static void setConversionTable(InventoryUnitConversionTable convertTable) {
    conversionTable = convertTable;
  }

  public static InventoryUnitConversionTable getConversionTable() {
    return conversionTable;
  }
}

