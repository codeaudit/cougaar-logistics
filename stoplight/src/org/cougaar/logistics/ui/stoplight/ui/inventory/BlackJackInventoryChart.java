/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.cougaar.mlm.ui.data.UISimpleNamedSchedule;

import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.*;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;

public class BlackJackInventoryChart extends javax.swing.JPanel implements PropertyChangeListener
{
  public static final int SHOW_ALL_CHARTS = 0;
  public static final int SHOW_INVENTORY_CHART = 1;
  public static final int SHOW_SUPPLIER_CHART = 2;
  public static final int SHOW_CONSUMER_CHART = 3;

//  private CChart mainChart = null;
//  private CChart minorChart1 = null;
//  private CChart minorChart2 = null;

  public CChart mainChart = null;
  public CChart minorChart1 = null;
  public CChart minorChart2 = null;

  private JPanel chartPanel = new JPanel(new GridBagLayout());

//  private CSplitPane outerSplit = new CSplitPane(JSplitPane.VERTICAL_SPLIT);
//  private CSplitPane innerSplit = new CSplitPane(JSplitPane.VERTICAL_SPLIT);

  protected CMThumbSliderDateAndTimeRangeControl xRC = new CMThumbSliderDateAndTimeRangeControl(0.0f, 0.0f);
  private boolean xRangeScrollLock = false;
  private double xScrollSize = 0.0;
  private double[] xMinMax = {0.0, 0.0};

  private CChart[] chartList = null;

//  private JLabel title = null;

//  private boolean fullView = true;
  private int viewMode = 0;
  private boolean scrollMainChart = false;

  private Hashtable labelIconList = null;

  private JPanel inventoryChartLegend = new JPanel();
  private JPanel supplierChartLegend = new JPanel();
  private JPanel consumerChartLegend = new JPanel();

  public BlackJackInventoryChart(String chartTitle, String xLabel, String yLabel, boolean timeAxis)
  {
    labelIconList = InventoryScheduleNames.buildLabelIconList();
    buildLegends();

    mainChart = new CChart(InventoryScheduleNames.INVENTORY_STATUS, inventoryChartLegend, xLabel, yLabel, timeAxis);
    mainChart.setShowXRangeScroller(false);
    mainChart.setToolTipDelay(0);

    minorChart1 = new CChart(InventoryScheduleNames.SUPPLIER, supplierChartLegend, xLabel, yLabel, timeAxis);
    minorChart1.setShowXRangeScroller(false);
    minorChart1.setToolTipDelay(0);
    minorChart1.setXMinorTicMarks(0);

    minorChart2 = new CChart(InventoryScheduleNames.CONSUMER, consumerChartLegend, xLabel, yLabel, timeAxis);
    minorChart2.setShowXRangeScroller(false);
    minorChart2.setToolTipDelay(0);
    minorChart2.setXMinorTicMarks(0);

    chartList = new CChart[] {mainChart, minorChart1, minorChart2};

//    title = new JLabel(chartTitle, SwingConstants.CENTER);

    xRC.setDynamicLabelsVisible(false);
    xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, chartList));
    xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);

    setLayout(new BorderLayout());

//    innerSplit.setOneTouchExpandable(true);
//    innerSplit.setTopComponent(mainChart);
//    innerSplit.setBottomComponent(minorChart1);

//    outerSplit.setOneTouchExpandable(true);
//    outerSplit.setTopComponent(innerSplit);
//    outerSplit.setBottomComponent(minorChart2);

//    mainChart.setMinimumSize(new Dimension(0,8));
//    minorChart1.setMinimumSize(new Dimension(0,8));
//    minorChart2.setMinimumSize(new Dimension(0,8));

//    add(title, BorderLayout.NORTH);
//    add(mainChart, BorderLayout.CENTER);
    add(xRC, BorderLayout.SOUTH);
    showAllCharts();
  }

  private void buildLegends()
  {
    inventoryChartLegend.removeAll();
    supplierChartLegend.removeAll();
    consumerChartLegend.removeAll();

    Hashtable list = null;
    LabelIcon icon = null;

    inventoryChartLegend.setLayout(new GridLayout());
    inventoryChartLegend.add(new JLabel(InventoryScheduleNames.INVENTORY_STATUS + ": "));
    list = (Hashtable)labelIconList.get(InventoryScheduleNames.INVENTORY_STATUS);
//    for (int i=0, isize=list.size(); i<isize; i++)
    for (Enumeration e=list.elements(); e.hasMoreElements();)
    {
      icon = (LabelIcon)e.nextElement();
      if (icon.isVisible())
      {
        inventoryChartLegend.add(new JLabel(icon.getName(), icon, SwingConstants.LEADING));
      }
    }

    supplierChartLegend.setLayout(new GridLayout());
    supplierChartLegend.add(new JLabel(InventoryScheduleNames.SUPPLIER + ": "));
    list = (Hashtable)labelIconList.get(InventoryScheduleNames.SUPPLIER);
//    for (int i=0, isize=list.size(); i<isize; i++)
    for (Enumeration e=list.elements(); e.hasMoreElements();)
    {
      icon = (LabelIcon)e.nextElement();
      if (icon.isVisible())
      {
        supplierChartLegend.add(new JLabel(icon.getName(), icon, SwingConstants.LEADING));
      }
    }

    consumerChartLegend.setLayout(new GridLayout());
    consumerChartLegend.add(new JLabel(InventoryScheduleNames.CONSUMER + ": "));
    list = (Hashtable)labelIconList.get(InventoryScheduleNames.CONSUMER);
//    for (int i=0, isize=list.size(); i<isize; i++)
    for (Enumeration e=list.elements(); e.hasMoreElements();)
    {
      icon = (LabelIcon)e.nextElement();
      if (icon.isVisible())
      {
        consumerChartLegend.add(new JLabel(icon.getName(), icon, SwingConstants.LEADING));
      }
    }

    inventoryChartLegend.validate();
    supplierChartLegend.validate();
    consumerChartLegend.validate();
  }

  private class RangeChangeListener implements PropertyChangeListener
  {
    private CMThumbSliderRangeControl rC = null;
    private double[] minMax = null;
    private CChart[] chartList = null;

    public RangeChangeListener(CMThumbSliderRangeControl rC, double[] minMax, CChart[] chartList)
    {
      RangeChangeListener.this.rC = rC;
      RangeChangeListener.this.minMax = minMax;
      RangeChangeListener.this.chartList = chartList;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
      rangeChanged(rC, minMax, chartList, xScrollSize, xRangeScrollLock, RangeChangeListener.this);
    }
  }

  public void setDataTipLabel(JLabel label)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setDataTipLabel(label);
    }
  }

/*  public CSplitPane getInnerSplitPane()
  {
    return(innerSplit);
  }

  public CSplitPane getOuterSplitPane()
  {
    return(outerSplit);
  }*/

/*  public void setScrollMainChart(boolean value)
  {
    scrollMainChart = value;

    if (scrollMainChart)
    {
      mainChart.setXScrollerRange(xRC.getRange());
      mainChart.setShowXDividers(false);
    }
    else if (!fullView)
    {
      mainChart.setShowXDividers(true);
      mainChart.resetRange();
    }
  }*/

  public void setScrollMainChart(boolean value)
  {
    scrollMainChart = value;

    if (scrollMainChart)
    {
      mainChart.setXScrollerRange(xRC.getRange());
      mainChart.setShowXDividers(false);
    }
    else if (!(viewMode == SHOW_INVENTORY_CHART))
    {
      mainChart.resetRange();
      mainChart.setShowXDividers(true);
    }
  }

/*  public void setShowFullView(boolean value)
  {
    fullView = value;

    if (fullView)
    {
      mainChart.setXScrollerRange(xRC.getRange());
      mainChart.setShowXDividers(false);

      innerSplit.remove(mainChart);
      remove(outerSplit);
      add(mainChart, BorderLayout.CENTER);
      validate();
    }
    else
    {
      if (scrollMainChart)
      {
        mainChart.setShowXDividers(false);
      }
      else
      {
        mainChart.setShowXDividers(true);
        mainChart.resetRange();
      }

      remove(mainChart);
      innerSplit.setTopComponent(mainChart);
      add(outerSplit, BorderLayout.CENTER);
      validate();
    }
  }*/

  public void showAllCharts()
  {
    viewMode = SHOW_ALL_CHARTS;

    if (scrollMainChart)
    {
      mainChart.setShowXDividers(false);
    }
    else
    {
      mainChart.setShowXDividers(true);
      mainChart.resetRange();
    }

    remove(mainChart);
    remove(minorChart1);
    remove(minorChart2);

    GridBagConstraints constraints = new GridBagConstraints();

    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    constraints.gridheight = 2;
    constraints.weightx = 2.0;
    constraints.weighty = 2.0;
    constraints.fill = GridBagConstraints.BOTH;
    chartPanel.add(mainChart, constraints);

    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    constraints.fill = GridBagConstraints.BOTH;
    chartPanel.add(minorChart1, constraints);

    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    constraints.fill = GridBagConstraints.BOTH;
    chartPanel.add(minorChart2, constraints);

    add(chartPanel, BorderLayout.CENTER);

    validate();
  }

  public void showFullInventoryChart()
  {
    viewMode = SHOW_INVENTORY_CHART;

    chartPanel.removeAll();
    remove(chartPanel);
    remove(mainChart);
    remove(minorChart1);
    remove(minorChart2);

    mainChart.setXScrollerRange(xRC.getRange());
    mainChart.setShowXDividers(false);

    add(mainChart, BorderLayout.CENTER);

    validate();
  }

  public void showFullSupplierChart()
  {
    viewMode = SHOW_SUPPLIER_CHART;

    chartPanel.removeAll();
    remove(chartPanel);
    remove(mainChart);
    remove(minorChart1);
    remove(minorChart2);

    if (scrollMainChart)
    {
      mainChart.setShowXDividers(false);
    }
    else
    {
      mainChart.setShowXDividers(true);
      mainChart.resetRange();
    }

    add(minorChart1, BorderLayout.CENTER);

    validate();
  }

  public void showFullConsumerChart()
  {
    viewMode = SHOW_CONSUMER_CHART;

    chartPanel.removeAll();
    remove(chartPanel);
    remove(mainChart);
    remove(minorChart1);
    remove(minorChart2);

    if (scrollMainChart)
    {
      mainChart.setShowXDividers(false);
    }
    else
    {
      mainChart.setShowXDividers(true);
      mainChart.resetRange();
    }

    add(minorChart2, BorderLayout.CENTER);

    validate();
  }

  public void updateUI()
  {
    super.updateUI();

    if (chartList != null)
    {
      for (int i=0; i<chartList.length; i++)
      {
        chartList[i].updateUI();
      }

      inventoryChartLegend.updateUI();
      supplierChartLegend.updateUI();
      consumerChartLegend.updateUI();

      buildLegends();
    }
  }

  public void attachDataSet(DataSet dataSet, int chartNumber)
  {
    chartList[chartNumber].attachDataSet(dataSet);
    InventoryScheduleNames.addToLabelList(labelIconList, dataSet, chartList[chartNumber].getName());

    buildLegends();
  }

  public Vector getDataSets()
  {
    Vector list = new Vector(0);
    for (int i=0; i<chartList.length; i++)
    {
      list.add(chartList[i].getDataSets());
    }

    return(list);
  }

  public void setAutoYRange(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setAutoYRange(value);
    }
  }

  public void setShowTitle(boolean show)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowTitle(show);
    }
  }

  public void setVisible(DataSet dataSet, boolean visible)
  {
    dataSet.visible = visible;
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].resetYRangeScroller();
      chartList[i].recalculateAutoYRange();
    }
    repaint();
  }

  public void setShowXRangeScroller(boolean value)
  {
    if (value)
    {
      add(xRC, BorderLayout.SOUTH);
      validate();
    }
    else
    {
      remove(xRC);
      validate();
    }
  }

  public void setShowXRangeTickLabels(boolean value)
  {
    xRC.setDrawTickLabels(value);
  }

  public void setXRangeScrollLock(boolean value)
  {
    xRangeScrollLock = value;

    if (xRangeScrollLock)
    {
      xScrollSize = xRC.getRange().getFMax() - xRC.getRange().getFMin();
    }
  }

  public void setUseCDate(boolean value)
  {
    xRC.setUseCDate(value);

    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setUseCDate(value);
    }
  }

  public void setYAxisLabel(String label)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setYAxisLabel(label);
    }
  }

  public void setXScrollerRange(RangeModel range)
  {
    if (viewMode == SHOW_INVENTORY_CHART)
    {
      mainChart.setXScrollerRange(range);
    }

    minorChart1.setXScrollerRange(range);
    minorChart2.setXScrollerRange(range);
    xRC.setRange(range);
  }

  public void resetTotalRange()
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].resetTotalRange();
      chartList[i].resetYRangeScroller();
      chartList[i].recalculateAutoYRange();
    }

    RangeModel range = mainChart.getTotalXRange();
    minorChart1.resetTotalXRange(range.getFMin(), range.getFMax());
    minorChart2.resetTotalXRange(range.getFMin(), range.getFMax());
    xRC.setSliderRange(range.getFMin(), range.getFMax());
  }

  public void setInitialRange(long timeRange)
  {
    xRC.setRange(new RangeModel((float)xRC.getMinValue(), (float)(xRC.getMinValue() + (timeRange/xRC.getTimeScale()))));
  }  

  public void resetRange()
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].resetRange();
    }

    RangeModel range = mainChart.getXScrollerRange();

    minorChart1.setXScrollerRange(range);
    minorChart2.setXScrollerRange(range);
    xRC.setRange(range);
  }

/*  public void setTitle(String tileString)
  {
    title.setText(tileString);
  }*/

  public void setBaseTime(long time)
  {
    xRC.setBaseTime(time);
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setBaseTime(time);
    }
  }

  public void setTimeScale(long scale)
  {
    xRC.setTimeScale(scale);
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setTimeScale(scale);
    }
  }

  public void setCDate(long date)
  {
    xRC.setCDate(date);
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setCDate(date);
    }
  }

// -----------------------------------------------------------------------------------------------

  public void propertyChange(PropertyChangeEvent e)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].repaint();
    }
  }

  public void detachAllDataSets()
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].detachAllDataSets();
    }

//    mainChart.detachAllDataSets();

    labelIconList = InventoryScheduleNames.buildLabelIconList();
    buildLegends();
  }

  public void setYScrollerRange(RangeModel range)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setYScrollerRange(range);
    }
  }

  public void setXAxisSigDigitDisplay(int numDigits)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setXAxisSigDigitDisplay(numDigits);
    }
  }

  public void setYAxisSigDigitDisplay(int numDigits)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setYAxisSigDigitDisplay(numDigits);
    }
  }

  public void setShowGrid(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowGrid(value);
    }
  }

  public void setGridOnTop(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setGridOnTop(value);
    }
  }

  public void setShowDataTips(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowDataTips(value);
    }
  }

  public void setShowLeftYAxis(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowLeftYAxis(value);
    }
  }

  public void setShowRightYAxis(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowRightYAxis(value);
    }
  }

  public void setShowYRangeScroller(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowYRangeScroller(value);
    }
  }

  public void setShowYRangeTickLabels(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setShowYRangeTickLabels(value);
    }
  }

  public void setYRangeScrollLock(boolean value)
  {
    for (int i=0; i<chartList.length; i++)
    {
      chartList[i].setYRangeScrollLock(value);
    }
  }

// -----------------------------------------------------------------------------------------------

  private void rangeChanged(CMThumbSliderRangeControl rC, double[] minMax, CChart[] chartList, double scrollSize, boolean scrollLock, PropertyChangeListener listener)
  {
    double currentMin = rC.getRange().getFMin();
    double currentMax = rC.getRange().getFMax();

    if ((currentMin == currentMax) || ((minMax[0] == currentMin) && (minMax[1] == currentMax)))
    {
      return;
    }

    if (scrollLock)
    {
      rC.removePropertyChangeListener("range", listener);
      if (minMax[0] != currentMin)
      {
        rC.setRange(new RangeModel((float)currentMin, (float)(currentMin + scrollSize)));
      }
      else if (minMax[1] != currentMax)
      {
        rC.setRange(new RangeModel((float)(currentMax - scrollSize), (float)currentMax));
      }
      else
      {
        rC.setRange(new RangeModel((float)minMax[0], (float)minMax[1]));
      }
      rC.addPropertyChangeListener("range", listener);
    }

    minMax[0] = rC.getRange().getFMin();
    minMax[1] = rC.getRange().getFMax();

    // Must set min and max
    if ((viewMode == SHOW_INVENTORY_CHART) || (scrollMainChart))
    {
      mainChart.setXScrollerRange(new RangeModel((float)minMax[0], (float)minMax[1]));
    }
    else
    {
      mainChart.resetRange();
    }

    mainChart.setXDividers(minMax[0], minMax[1]);

    minorChart1.setXScrollerRange(new RangeModel((float)minMax[0], (float)minMax[1]));
    minorChart2.setXScrollerRange(new RangeModel((float)minMax[0], (float)minMax[1]));
  }
}
