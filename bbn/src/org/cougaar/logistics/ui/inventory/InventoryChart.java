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
import java.awt.Dimension;

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
import org.cougaar.logistics.plugin.inventory.TimeUtils;

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

public abstract class InventoryChart extends JPanel {

    private final static long MILLIS_IN_DAY = TimeUtils.MSEC_PER_DAY;


    protected JCChart chart = null;
    protected InventoryData inventory = null;

    protected boolean displayCDay = false;
    protected String myYAxisTitle;
    protected String myTitle;

    protected int viewIndex;

    protected InventoryColorTable colorTable;

    protected Logger logger;

    protected long currBucketSize=MILLIS_IN_DAY;


    public abstract void initializeChart();

    public void initialize(String title) {
        int gridx = 0;
        int gridy = 0;

        viewIndex = 0;

        logger = Logging.getLogger(this);

        colorTable = new InventoryColorTable();

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

    public JCChart getChart() { return chart; }

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

    protected void resetAxes() {

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
            //       yaxis.setEditable(false); // don't allow zoomin on this axis
            yaxis.setMin(0); // set zero as the minimum value on the axis
        }
    }

    protected void customizeAxes(String yAxisTitleText) {
        myYAxisTitle = yAxisTitleText;
        resetAxes();
    }

    public void setData(InventoryData data) {
        inventory = data;
        if(data.getBucketSize() != currBucketSize) {
           resetAxes();
        }
        java.util.List viewList = chart.getDataView();
        for (int i = 0; i < viewList.size(); i++) {
            ChartDataView chartDataView = (ChartDataView) viewList.get(i);
            InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();
            dataModel.resetInventory(data);
        }
        myYAxisTitle = data.getUnit();
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
}
