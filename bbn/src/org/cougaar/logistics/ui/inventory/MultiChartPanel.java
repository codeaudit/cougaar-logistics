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
import java.util.Calendar;
import java.util.Iterator;

import java.awt.Event;
import java.awt.event.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Font;
import java.awt.BorderLayout;

import javax.swing.*;

import com.klg.jclass.chart.JCChartListener;
import com.klg.jclass.chart.JCChartEvent;
import com.klg.jclass.chart.JCChart;
import com.klg.jclass.chart.JCAxis;
import com.klg.jclass.chart.JCPickListener;
import com.klg.jclass.chart.JCPickEvent;
import com.klg.jclass.chart.JCDataIndex;
import com.klg.jclass.chart.JCChartUtil;
import com.klg.jclass.chart.ChartDataView;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.ui.inventory.data.InventoryData;

/**
 * <pre>
 *
 * The MultiChartPanel contains the three InventoryCharts
 * to be displayed: the levels, refill, and demand charts.
 * Its just a container that measures lays out the
 * charts appropriatly.
 *
 *
 * @see InventoryLevelChart
 * @see InventoryRefillChart
 * @see InventoryDemandChart
 *
 *
 **/

public class MultiChartPanel extends JPanel
        implements JCChartListener, JCPickListener,  ItemListener {

    private final static long MILLIS_IN_DAY = TimeUtils.MSEC_PER_DAY;

    public final static String INITIAL_POINT_LABEL = "Right click to get quantity at a point.";

    public static final String CDAY_MODE = "Cdays";
    public static final String CHOUR_MODE = "Chours";
    public static final String SHORTFALL_MODE = "Shortfall";

    protected InventoryLevelChart levelChart;
    protected InventoryRefillChart refillChart;
    protected InventoryDemandChart demandChart;

    protected MouseAdapter mouseAdapter = new MouseAdapter() {
           public void mousePressed(MouseEvent e) {
                  levelChartMousePick(e);
           }
    };

    protected JLabel pointLabel;

    private Logger logger;

    private JCheckBox cdaysModeCheck;
    private JCheckBox shortfallModeCheck;

    protected boolean displayCDay = false;
    private long baseCDayTime = InventoryChartBaseCalendar.getBaseTime();

    private long bucketSize = MILLIS_IN_DAY;

    private boolean displayShortfall = false;

    private boolean showInitialShortfall = true;

    public MultiChartPanel() {
        super();
        initializeMultiChart();
    }

    public MultiChartPanel(boolean showInitShortfall,boolean initialDisplayCDay) {
        super();
        showInitialShortfall = showInitShortfall;
	displayCDay = initialDisplayCDay;
        initializeMultiChart();
    }

    public void initializeMultiChart() {
        int gridx = 0;
        int gridy = 0;

        logger = Logging.getLogger(this);


        InventoryColorTable colorTable = new InventoryColorTable();

        Insets blankInsets = new Insets(0, 0, 0, 0);
        levelChart = new InventoryLevelChart(displayCDay);
        refillChart = new InventoryRefillChart(displayCDay);
        demandChart = new InventoryDemandChart(displayCDay);

        pointLabel = new JLabel(INITIAL_POINT_LABEL, JLabel.CENTER);
        pointLabel.setBackground(Color.magenta);

        cdaysModeCheck = new JCheckBox(CDAY_MODE, displayCDay);
        Font newFont = cdaysModeCheck.getFont();
        newFont = newFont.deriveFont(newFont.getStyle(), (float) 15);
        cdaysModeCheck.setFont(newFont);
        cdaysModeCheck.setActionCommand(CDAY_MODE);
        cdaysModeCheck.setToolTipText("Display xaxis dates as C-Days");
        cdaysModeCheck.addItemListener(this);

        shortfallModeCheck = new JCheckBox(SHORTFALL_MODE, displayShortfall);
        shortfallModeCheck.setFont(newFont);
        shortfallModeCheck.setActionCommand(SHORTFALL_MODE);
        shortfallModeCheck.setToolTipText("Display shortfall plots");
        shortfallModeCheck.setForeground(colorTable.get(ShortfallChartDataModel.SHORTFALL_SERIES_LABEL));
        shortfallModeCheck.setEnabled(false);
        shortfallModeCheck.addItemListener(this);

        addAllChartListeners();
        addAllPickListeners();

        // set header and legend to black
        // set beveled borders around plot area and chart
        //((JLabel)chart.getHeader()).setForeground(Color.black);
        //chart.setBorder(BorderFactory.createLoweredBevelBorder());
        //chart.getChartArea().setBorder(BorderFactory.createLoweredBevelBorder());

        // add chart to panel
        setLayout(new GridBagLayout());

        int height = 4;
        gridy += height;

        add(levelChart, new GridBagConstraints(gridx,
                                               GridBagConstraints.RELATIVE,
                                               1, height, 1.0, 1.0,
                                               GridBagConstraints.CENTER,
                                               GridBagConstraints.BOTH,
                                               blankInsets, 0, 0));

        height = 3;
        gridy += height;

        add(refillChart, new GridBagConstraints(gridx,
                                                GridBagConstraints.RELATIVE,
                                                1, height, 1.0, 0.7,
                                                GridBagConstraints.CENTER,
                                                GridBagConstraints.BOTH,
                                                blankInsets, 0, 0));

        height = 3;
        gridy += height;

        add(demandChart, new GridBagConstraints(gridx,
                                                GridBagConstraints.RELATIVE,
                                                1, height, 1.0, 0.7,
                                                GridBagConstraints.CENTER,
                                                GridBagConstraints.BOTH,
                                                blankInsets, 0, 0));

        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BorderLayout());
        checkBoxPanel.add(shortfallModeCheck, BorderLayout.WEST);
        checkBoxPanel.add(Box.createHorizontalStrut(4), BorderLayout.CENTER);
        checkBoxPanel.add(cdaysModeCheck, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(Box.createHorizontalStrut(20), BorderLayout.WEST);
        bottomPanel.add(pointLabel, BorderLayout.CENTER);
        bottomPanel.add(checkBoxPanel, BorderLayout.EAST);

        height = 1;
        gridy += height;

        add(bottomPanel, new GridBagConstraints(gridx,
                                                GridBagConstraints.RELATIVE,
                                                1, height, 1.0, 0.1,
                                                GridBagConstraints.CENTER,
                                                GridBagConstraints.HORIZONTAL,
                                                blankInsets, 0, 0));


    }

    public void setData(InventoryData data) {
        removeAllChartListeners();
        baseCDayTime = data.getStartCDay();
        bucketSize = data.getBucketSize();
        if (bucketSize < MILLIS_IN_DAY) {
            cdaysModeCheck.setText(CHOUR_MODE);
        } else {
            cdaysModeCheck.setText(CDAY_MODE);
        }

        levelChart.setData(data);
        refillChart.setData(data);
        demandChart.setData(data);
        JCAxis firstXAxis = levelChart.getFirstXAxis();
        firstXAxis.recalc();
        reallignChartsByXAxis(firstXAxis);
        pointLabel.setText(INITIAL_POINT_LABEL);
        if ((updateShortfallCheckBox()) &&
            (showInitialShortfall)) {
            setDisplayShortfall(true);
            shortfallModeCheck.setSelected(true);
        } else {
            shortfallModeCheck.setSelected(false);
        }
        addAllChartListeners();
    }

    public boolean updateShortfallCheckBox() {
        if (refillChart.isShortfall() ||
                demandChart.isShortfall()) {
            shortfallModeCheck.setEnabled(true);
            return true;
        } else {
            shortfallModeCheck.setEnabled(false);
            return false;
        }
    }

    public void setDisplayCDay(boolean doUseCDay) {
        if (displayCDay != doUseCDay) {
            displayCDay = doUseCDay;
            removeAllChartListeners();
            levelChart.setDisplayCDay(displayCDay);
            refillChart.setDisplayCDay(displayCDay);
            demandChart.setDisplayCDay(displayCDay);
            reallignChartsByXAxis(levelChart.getFirstXAxis());
            pointLabel.setText(INITIAL_POINT_LABEL);
            addAllChartListeners();
        }
    }

    public void setDisplayShortfall(boolean doDisplayShortfall) {
        if (displayShortfall != doDisplayShortfall) {
            displayShortfall = doDisplayShortfall;
            removeAllChartListeners();
            refillChart.setDisplayShortfall(displayShortfall);
            demandChart.setDisplayShortfall(displayShortfall);
            pointLabel.setText(INITIAL_POINT_LABEL);
            addAllChartListeners();
        }
    }

    public void removeAllChartListeners() {
        levelChart.removeChartListener(this);
        refillChart.removeChartListener(this);
        demandChart.removeChartListener(this);
    }

    public void addAllChartListeners() {
        levelChart.addChartListener(this);
        refillChart.addChartListener(this);
        demandChart.addChartListener(this);
    }

    public void removeAllPickListeners() {
        levelChart.getChart().removeMouseListener(mouseAdapter);
        refillChart.removePickListener(this);
        demandChart.removePickListener(this);
    }



    public void addAllPickListeners() {

        //levelChart.addPickListener(this);
        levelChart.getChart().addMouseListener(mouseAdapter);
        refillChart.addPickListener(this);
        demandChart.addPickListener(this);
    }


    //Make all three charts align along the xAxis
    //Hand in an xAxis as the reference point
    //typically this is xAxis being zoomed in upon
    //but also sometimes the level chart xAxis at the
    //beginning is the ruler.
    //This is should only be called after removing
    //All chart listeners.
    public void reallignChartsByXAxis(JCAxis xAxis) {
        double xStart = xAxis.getMin();
        double xEnd = xAxis.getMax();
        levelChart.setXZoom(xStart, xEnd);
        refillChart.setXZoom(xStart, xEnd);
        demandChart.setXZoom(xStart, xEnd);
    }

    public void changeChart(JCChartEvent jce) {
        JCChart source = (JCChart) jce.getSource();
        //String headerText =  ((JLabel) source.getHeader()).getText();
        JCAxis axis = jce.getModifiedAxis();
        if (!axis.isVertical()) {
            removeAllChartListeners();
            reallignChartsByXAxis(axis);
            pointLabel.setText(INITIAL_POINT_LABEL);
            addAllChartListeners();
        }
    }

    public void paintChart(JCChart chart) {
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() instanceof JCheckBox) {
            JCheckBox source = (JCheckBox) e.getSource();
            if (source.getActionCommand().equals(CDAY_MODE)) {
                setDisplayCDay(e.getStateChange() == e.SELECTED);
            } else if (source.getActionCommand().equals(SHORTFALL_MODE)) {
                setDisplayShortfall(e.getStateChange() == e.SELECTED);
            }
        }
    }

    public void levelChartMousePick(MouseEvent e) {
      if(SwingUtilities.isRightMouseButton(e)) {
          JCChart innerChart = (JCChart)e.getSource();
          ChartDataView invChartDataView = levelChart.getInvChartDataView();

          JCDataIndex dataIndex = innerChart.pick(e.getPoint(),invChartDataView);
          if(!(dataIndex.getPoint() < 0) && !(dataIndex.getSeriesIndex() < 0)){
            pick(innerChart,dataIndex);
          }
      }
    }

    public void pick(JCPickEvent e) {
        JCDataIndex dataIndex = e.getPickResult();
        // check for all the possible failures
        if (dataIndex == null) {
            logger.warn("WARNING: dataIndex is null");
            return;
        }
        pick((JCChart) e.getSource(), dataIndex);
    }

    public void pick(JCChart chart, JCDataIndex dataIndex) {

        ChartDataView chartDataView = dataIndex.getDataView();
        if (chartDataView == null) {
            logger.warn("WARNING: chartDataView is null");
            return;
        }
        int seriesIndex = dataIndex.getSeriesIndex();
        int pt = dataIndex.getPoint();
        if (pt < 0 || seriesIndex < 0) {
            logger.warn("WARNING: series or point index is null");
            return;
        }

        InventoryBaseChartDataModel dataModel = (InventoryBaseChartDataModel) chartDataView.getDataSource();

        if (dataModel == null) {
            logger.warn("WARNING: data model is null");
            return;
        }


        //The OrgActivityChart seems to be stealing picks
        //from the InventoryLevelChart data.  Turn about is fair play.
        if(dataModel instanceof OrgActivityChartDataModel){
            logger.warn("Still getting events from OrgActivityChartDataModel!");
        }


        // user has picked a valid point
        double[] x = dataModel.getXSeries(seriesIndex);
        double[] y = dataModel.getRealYSeries(seriesIndex);

        int cDay;
        int dayOfYear;
        int daysFromBaseToCDay = (int) ((baseCDayTime - InventoryChartBaseCalendar.getBaseTime())
                / TimeUtils.MSEC_PER_DAY);

        int hourOfDay = 0;
        boolean displayHourly = (bucketSize < MILLIS_IN_DAY);

        if (displayHourly) {
            if (displayCDay) {
                int cHour = ((int) x[pt]);
                cDay = (cHour / TimeUtils.HOUR_PER_DAY);
                //subtract 1, because in our scale between -1 and 0
                //is CDay -1 and the hour, not C0
                if(cHour < 1){
                    cDay--;
                }
                hourOfDay = cHour % TimeUtils.HOUR_PER_DAY;
                dayOfYear = cDay + daysFromBaseToCDay;
            } else {
                int hourOfYear = ((int) x[pt]);
                dayOfYear = hourOfYear / TimeUtils.HOUR_PER_DAY;
                hourOfDay = hourOfYear % TimeUtils.HOUR_PER_DAY;
                cDay = dayOfYear - daysFromBaseToCDay;
            }
        } else {
            if (displayCDay) {
                cDay = ((int) x[pt]);
                dayOfYear = cDay + daysFromBaseToCDay;
            } else {
                dayOfYear = ((int) x[pt]);
                cDay = dayOfYear - daysFromBaseToCDay;
            }
        }

        InventoryChartBaseCalendar tmpC = new InventoryChartBaseCalendar();
        tmpC.set(Calendar.YEAR, InventoryChartBaseCalendar.getBaseYear());
        // add one because day of year is Zero based.
        tmpC.set(Calendar.DAY_OF_YEAR, (dayOfYear + 1));
        tmpC.set(Calendar.HOUR_OF_DAY, hourOfDay);
        // add 1 to month as it numbers them from 0
        int month = tmpC.get(Calendar.MONTH) + 1;

        String label;
        String valueLabel = "Quantity: " + JCChartUtil.format(y[pt], 2);

        if (displayHourly) {
            label = "Date: " + month + "/" +
                    tmpC.get(Calendar.DAY_OF_MONTH) + "/" +
                    tmpC.get(Calendar.YEAR) + " " +
                    "(C" + cDay + ") " +
                    tmpC.get(Calendar.HOUR_OF_DAY) + ":00 " +
                    valueLabel;

        } else {
            label = "Date: " + month + "/" +
                    tmpC.get(Calendar.DAY_OF_MONTH) + "/" +
                    tmpC.get(Calendar.YEAR) + " " +
                    "(C" + cDay + ")" +
                    valueLabel;
        }


        //System.out.println("Pick Point is: " + label);
        pointLabel.setText(label);

    }

}
