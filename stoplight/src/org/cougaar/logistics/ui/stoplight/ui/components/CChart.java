/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.plaf.metal.MetalLookAndFeel;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.*;
import org.cougaar.logistics.ui.stoplight.ui.models.*;

/***********************************************************************************************************************
<b>Description</b>: This class is a Chart display component that provides X and Y dual-thumb range sliders to narrow
                    or broaden the view of the data on the chart.

***********************************************************************************************************************/
public class CChart extends javax.swing.JPanel implements ColorProducer, PropertyChangeListener, PlotColors
{
  protected PointViewGraph2D graph = new PointViewGraph2D();
  protected Axis    xaxis = null;
  protected Axis    yaxisLeft = null;
  protected Axis    yaxisRight = null;

  protected double[] xMinMax = {0.0, 0.0};
  protected double[] yMinMax = {0.0, 0.0};

  protected CMThumbSliderRangeControl xRC = null;
  protected CMThumbSliderRangeControl yRC = null;

  protected boolean[] xRangeScrollLock = new boolean[] {false};
  protected double[] xScrollSize = new double[] {0.0};

  protected boolean[] yRangeScrollLock = new boolean[] {false};
  protected double[] yScrollSize = new double[] {0.0};

  protected int lastColor = 0;
  protected Color[] plotColors = null;

  protected boolean autoYRange = false;

//  protected final static Color[] defaultColors = {Color.red, Color.blue, Color.green, Color.yellow, Color.orange, Color.cyan, Color.magenta, Color.pink};
//  protected final static Color defaultGridColor = Color.red;

  protected Color xDividerColor = Color.lightGray;
  protected final static Color defaultGridColor = Color.lightGray;
  protected final static Color[] defaultColors = {midnightBlue, darkGreen, darkYellow, rust, darkPurple, orange, red};

//  protected JLabel title = null;
  protected JPanel title = null;
  protected String name = null;
  protected double additionalSpace = 0.1;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a chart with the spedified labels and, as an option, a X time axis.

  <br>
  @param chartTitle Title of the chart
  @param xLabel X axis label
  @param yLabel Y axis label
  @param timeAxis True if the X axis of the chart should refer to date and time, false otherwise
	*********************************************************************************************************************/
  public CChart(String chartTitle, String xLabel, String yLabel, boolean timeAxis)
  {
    this(chartTitle, new JPanel(new BorderLayout()), xLabel, yLabel, timeAxis);
    title.add(new JLabel(chartTitle, SwingConstants.CENTER), BorderLayout.CENTER);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a chart with the spedified labels, a panel that represents the chart title, and, as an
                      option, a X time axis.

  <br>
  @param chartName Title of the chart
  @param chartTitle Panel to display as the chart title
  @param xLabel X axis label
  @param yLabel Y axis label
  @param timeAxis True if the X axis of the chart should refer to date and time, false otherwise
	*********************************************************************************************************************/
  public CChart(String chartName, JPanel chartTitle, String xLabel, String yLabel, boolean timeAxis)
  {
    title = chartTitle;
    name = chartName;

    setUpXRangeScroller(timeAxis);
    setUpYRangeScroller();


    xaxis.setTitleText(xLabel);
    xaxis.sigDigitDisplay = 1;
    xaxis.exponentDisplayThreshold = 7;
    xaxis.setManualRange(true);

    yaxisLeft.setTitleText(yLabel);
    yaxisLeft.sigDigitDisplay = 1;
    yaxisLeft.exponentDisplayThreshold = 7;
    yaxisLeft.setManualRange(true);

    yaxisRight.setTitleText(yLabel);
    yaxisRight.sigDigitDisplay = 1;
    yaxisRight.exponentDisplayThreshold = 7;
    yaxisRight.setManualRange(true);



    // Set up the graph and x/y axis
    graph.gridOnTop = false;
    graph.attachAxis(xaxis);
    graph.attachAxis(yaxisLeft);
    graph.attachAxis(yaxisRight);
//    graph.borderTop          = 10;
//    graph.borderBottom       = 10;
//    graph.borderLeft         = 10;
//    graph.borderRight        = 10;



    setLayout(new BorderLayout());
    add(title, BorderLayout.NORTH);
    add(graph, BorderLayout.CENTER);
    add(yRC, BorderLayout.WEST);
    add(xRC, BorderLayout.SOUTH);

    // Make sure that the UI L&F/Themes are set up
    doUIUpdate();
  }
  
  /*********************************************************************************************************************
  <b>Description</b>: Constructs a chart with the spedified labels, a panel that represents the chart title, and, as an
                      option, a X time axis.

  <br>
  @param chartName Title of the chart
  @param chartTitle Panel to display as the chart title
  @param xLabel X axis label
  @param yLabel Y axis label
  @param timeAxis True if the X axis of the chart should refer to date and time, false otherwise
	*********************************************************************************************************************/
  public CChart(String chartName, JPanel chartTitle, String xLabel, String yLabel, boolean timeAxis, double space)
  {
  	
  	additionalSpace = space;
    title = chartTitle;
    name = chartName;

    setUpXRangeScroller(timeAxis);
    setUpYRangeScroller();


    xaxis.setTitleText(xLabel);
    xaxis.sigDigitDisplay = 1;
    xaxis.exponentDisplayThreshold = 7;
    xaxis.setManualRange(true);

    yaxisLeft.setTitleText(yLabel);
    yaxisLeft.sigDigitDisplay = 1;
    yaxisLeft.exponentDisplayThreshold = 7;
    yaxisLeft.setManualRange(true);

    yaxisRight.setTitleText(yLabel);
    yaxisRight.sigDigitDisplay = 1;
    yaxisRight.exponentDisplayThreshold = 7;
    yaxisRight.setManualRange(true);



    // Set up the graph and x/y axis
    graph.gridOnTop = false;
    graph.attachAxis(xaxis);
    graph.attachAxis(yaxisLeft);
    graph.attachAxis(yaxisRight);
//    graph.borderTop          = 10;
//    graph.borderBottom       = 10;
//    graph.borderLeft         = 10;
//    graph.borderRight        = 10;



    setLayout(new BorderLayout());
    add(title, BorderLayout.NORTH);
    add(graph, BorderLayout.CENTER);
    add(yRC, BorderLayout.WEST);
    add(xRC, BorderLayout.SOUTH);

    // Make sure that the UI L&F/Themes are set up
    doUIUpdate();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the Y axis label.

  <br>
  @param label New Y axis label
  *********************************************************************************************************************/
  public void setYAxisLabel(String label)
  {
    yaxisLeft.setTitleText(label);
    yaxisRight.setTitleText(label);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the X axis label.

  <br>
  @param label New X axis label
  *********************************************************************************************************************/
  public void setXAxisLabel(String label)
  {
    xaxis.setTitleText(label);
  }

  protected void setUpXRangeScroller(boolean timeAxis)
  {
    if (timeAxis)
    {
      xaxis = new TimeAndDateAxis();
      xRC = new CMThumbSliderDateAndTimeRangeControl(xMinMax[0], xMinMax[1]);
      xRC.setRange(new RangeModel(xMinMax[0], xMinMax[1]));
    }
    else
    {
      xaxis = new Axis(Axis.BOTTOM, additionalSpace);
      xRC = new CMThumbSliderRangeControl(xMinMax[0], xMinMax[1]);
      xRC.setRange(new RangeModel(xMinMax[0], xMinMax[1]));
    }
    xRC.setDynamicLabelsVisible(false);
    xRC.addPropertyChangeListener("range", new RangeChangeListener(xRC, xMinMax, new Axis[] {xaxis}, xScrollSize, xRangeScrollLock));

    xRC.getSlider().setOrientation(CMThumbSlider.HORIZONTAL);
  }

  protected void setUpYRangeScroller()
  {
    yaxisLeft = new Axis(Axis.LEFT, additionalSpace);
    yaxisRight = new Axis(Axis.RIGHT, additionalSpace);

    yRC = new CMThumbSliderRangeControl(yMinMax[0], yMinMax[1]);
    yRC.setRange(new RangeModel(yMinMax[0], yMinMax[1]));

    yRC.setDynamicLabelsVisible(false);
    yRC.addPropertyChangeListener("range", new RangeChangeListener(yRC, yMinMax, new Axis[] {yaxisLeft, yaxisRight}, yScrollSize, yRangeScrollLock));

    yRC.getSlider().setOrientation(CMThumbSlider.VERTICAL);
  }

  protected class RangeChangeListener implements PropertyChangeListener
  {
    protected CMThumbSliderRangeControl rC = null;
    protected double[] minMax = null;
    protected Axis[] axisList = null;
    protected double[] scrollSize = null;
    protected boolean[] scrollLock = null;

    public RangeChangeListener(CMThumbSliderRangeControl rC, double[] minMax, Axis[] axisList, double[] scrollSize, boolean[] scrollLock)
    {
      RangeChangeListener.this.rC = rC;
      RangeChangeListener.this.minMax = minMax;
      RangeChangeListener.this.axisList = axisList;
      RangeChangeListener.this.scrollSize = scrollSize;
      RangeChangeListener.this.scrollLock = scrollLock;
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
      rangeChanged(rC, minMax, axisList, scrollSize[0], scrollLock[0], RangeChangeListener.this);

      // A functionality hack
      if (rC == xRC)
      {
        recalculateAutoYRange();
      }
    }
  }

  // 
	/*********************************************************************************************************************
  <b>Description</b>: This method recalculates the Y axis range based on the currently visible data sets.  It is used
                      internally when auto Y range is enabled.

  <br><b>Notes</b>:<br>
	                  - Must be called (if auto Y range is enabled) when datasets are set visible/not visible (currently
	                    no automatic hook available)
	*********************************************************************************************************************/
  public void recalculateAutoYRange()
  {
    if (autoYRange)
    {
      double yMax = graph.getYmaxInRange(xMinMax[0], xMinMax[1]);
      if (Double.isNaN(yMax))
      {
        yRC.setRange(new RangeModel(0.0, 1.0));
      }
      else
      {
        //yRC.setRange(new RangeModel(0.0, yMax + yMax*0.10));
        yRC.setRange(new RangeModel(0.0, yMax + yMax * additionalSpace));
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the title of the chart as text.

  <br>
  @return Chart title
	*********************************************************************************************************************/
  public String getName()
  {
    return(name);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/disables automatic Y axis ranging.  When enabled, the Y axis will automatcally reset
                      its thumb sliders to be within the limits of the maximum Y value of all data sets that are
                      visible.

  <br><b>Notes</b>:<br>
	                  - When this feature is turned on and a data set is made not visible, the
	                    CChart.recalculateAutoYRange() method must be called

  <br>
  @param value True if auto Y axis ranging should be enabled, false otherwise
	*********************************************************************************************************************/
  public void setAutoYRange(boolean value)
  {
    autoYRange = value;
    recalculateAutoYRange();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the label object to display data tips within.  By default, the data tip will display next
                      to the mouse pointer.  Setting the data tip label will cause the data tip to be displayed on
                      the specified label.

  <br><b>Notes</b>:<br>
	                  - The label is not added to the chart display, and, therefore, must be added to a layout manually

  <br>
  @param label Label object to display the data tip within
	*********************************************************************************************************************/
  public void setDataTipLabel(JLabel label)
  {
    graph.setDataTipLabel(label);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the delay, in milliseconds, of how long the data tip should delay after no mouse movement to
                      display a data tip.

  <br>
  @param delay Delay time in milliseconds
	*********************************************************************************************************************/
  public void setToolTipDelay(int delay)
  {
    graph.setToolTipDelay(delay);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the location of X axis dividers.  Dividers are vertical lines that segment a chart into 3
                      sections.  Typically, they are used to mark the current displayed range of a sub chart on a
                      larger over-all chart.

  <br>
  @param x1 X location of first divider
  @param x2 X location of second divider
	*********************************************************************************************************************/
  public void setXDividers(double x1, double x2)
  {
    if (xaxis.dividers == null)
    {
      xaxis.dividers = new Object[2][2];
    }

    xaxis.dividers[0][0] = new Double(x1);
    xaxis.dividers[0][1] = xDividerColor;

    xaxis.dividers[1][0] = new Double(x2);
    xaxis.dividers[1][1] = xDividerColor;

    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of the chart title.

  <br>
  @param show True if the title is to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowTitle(boolean show)
  {
    remove(title);

    if (show)
    {
      add(title, BorderLayout.NORTH);
    }

    validate();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of X axis dividers.

  <br>
  @param value True if the dividers are to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowXDividers(boolean value)
  {
    xaxis.showDividers = value;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the number of X axis minior tic marks per major tic mark.

  <br>
  @param count Number of minior tic marks per major tic mark
	*********************************************************************************************************************/
  public void setXMinorTicMarks(int count)
  {
    xaxis.minor_tic_count = count;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the color of the X axis dividers.

  <br>
  @param color Color of X axis dividers
	*********************************************************************************************************************/
  public void setXDividerColor(Color color)
  {
    xDividerColor = color;
    if (xaxis.dividers != null)
    {
      xaxis.dividers[0][1] = xDividerColor;

      xaxis.dividers[1][1] = xDividerColor;

      graph.repaint();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the suggested maximum squared distance from a data point to trigger a data tip.  If a data
                      point is further away from the mouse pointer than this squared distance, the data tip will not
                      be displayed for that point.

  <br>
  @param value Maximum squared distance in pixels
	*********************************************************************************************************************/
  public void setSuggestedMaxPointDist2(double value)
  {
    graph.suggestedMaxPointDist2 = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the data set that has a point closest to the specifed (X,Y) pixel cooridinates.

  <br>
  @param x X pixel coordinate
  @param y Y pixel coordinate
  @return Data set closest to the specified location or null if there isn't one
	*********************************************************************************************************************/
  public DataSet getClosestDataSet(int x, int y)
  {
    return((DataSet)graph.getClosestPoint(x, y)[1]);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of the chart grid.

  <br>
  @param value True if the grid is to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowGrid(boolean value)
  {
    graph.drawgrid = value;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of the chart grid on top of the data set plots.

  <br>
  @param value True if the grid is to be displayed on top, false otherwise
	*********************************************************************************************************************/
  public void setGridOnTop(boolean value)
  {
    graph.gridOnTop = value;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of chart data tips.

  <br>
  @param value True if data tips are to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowDataTips(boolean value)
  {
    graph.setShowDataTips(value);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of chart's left Y axis.

  <br>
  @param value True if left Y axis is to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowLeftYAxis(boolean value)
  {
    yaxisLeft.visible = value;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of chart's right Y axis.

  <br>
  @param value True if right Y axis is to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowRightYAxis(boolean value)
  {
    yaxisRight.visible = value;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of chart's X range dual-thumb slider.

  <br>
  @param value True if the X range slider is to be displayed, false otherwise
	*********************************************************************************************************************/
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

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of X range slider's tic labels.

  <br>
  @param value True if the X range slider's tic labels are to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowXRangeTickLabels(boolean value)
  {
    xRC.setDrawTickLabels(value);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of X axis tic marks and labels.

  <br>
  @param value True if the X axis tic marks and labels are to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowXAxisTickLabels(boolean value)
  {
    xaxis.drawTicMarks = value;
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the X range slider's scroll lock.  When enabled, moving one of the thumb sliders
                      of the X range slider results in the movement of the other thumb slider an equal distance in the
                      same direction keeping the current range distance of the slider the same.

  <br>
  @param value True if the X range slider's scroll lock is to be enabled, false otherwise
	*********************************************************************************************************************/
  public void setXRangeScrollLock(boolean value)
  {
    xRangeScrollLock[0] = value;

    if (xRangeScrollLock[0])
    {
      xScrollSize[0] = xRC.getRange().getDMax() - xRC.getRange().getDMin();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of chart's Y range dual-thumb slider.

  <br>
  @param value True if the Y range slider is to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowYRangeScroller(boolean value)
  {
    if (value)
    {
      add(yRC, BorderLayout.WEST);
      validate();
    }
    else
    {
      remove(yRC);
      validate();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of Y range slider's tic labels.

  <br>
  @param value True if the Y range slider's tic labels are to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowYRangeTickLabels(boolean value)
  {
    yRC.setDrawTickLabels(value);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of Y axis tic marks and labels.

  <br>
  @param value True if the Y axis tic marks and labels are to be displayed, false otherwise
	*********************************************************************************************************************/
  public void setShowYAxisTickLabels(boolean value)
  {
    yaxisLeft.drawTicMarks = value;
    yaxisRight.drawTicMarks = value;
    
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the Y range slider's scroll lock.  When enabled, moving one of the thumb sliders
                      of the Y range slider results in the movement of the other thumb slider an equal distance in the
                      same direction keeping the current range distance of the slider the same.

  <br>
  @param value True if the Y range slider's scroll lock is to be enabled, false otherwise
	*********************************************************************************************************************/
  public void setYRangeScrollLock(boolean value)
  {
    yRangeScrollLock[0] = value;

    if (yRangeScrollLock[0])
    {
      yScrollSize[0] = yRC.getRange().getFMax() - yRC.getRange().getFMin();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables/Disables the display of C-Date values.

  <br>
  @param value True if the X axis should use C-Date values, false otherwise
	*********************************************************************************************************************/
  public void setUseCDate(boolean value)
  {
    if (xaxis instanceof TimeAndDateAxis)
    {
      ((TimeAndDateAxis)xaxis).useCDate = value;
      ((CMThumbSliderDateAndTimeRangeControl)xRC).setUseCDate(value);

      graph.repaint();
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Attaches a data set to the chart.

  <br><b>Notes</b>:<br>
	                  - The CChart.resetTotalRange() method should be called after adding all data sets to the chart to
	                    set the minimum and maximum limits of the X & Y range sliders according to the minimum and
	                    maximum values of the data sets.

  <br>
  @param dataSet Data set to attach to the chart
	*********************************************************************************************************************/
  public void attachDataSet(DataSet dataSet)
  {
    if (dataSet instanceof StackableBarDataSet)
    {
      // Attach the data set to the graph and axes
      ((StackableBarDataSet)dataSet).reAttachBarBataSets(graph, xaxis, yaxisLeft, yaxisRight, this);
      ((StackableBarDataSet)dataSet).resetDataSetColors();
    }
    else
    {
      if (dataSet.automaticallySetColor)
      {
        dataSet.linecolor = getColor(dataSet.linecolor, dataSet.colorNumber);
      }

      // Attach the data set to the graph and axes
      graph.attachDataSet(dataSet);
      xaxis.attachDataSet(dataSet);
      yaxisLeft.attachDataSet(dataSet);
      yaxisRight.attachDataSet(dataSet);

    }

    recalculateAutoYRange();
    resetYRangeScroller();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes all data sets from the chart.
	*********************************************************************************************************************/
  public void detachAllDataSets()
  {
    lastColor = 0;

    graph.detachDataSets();
    xaxis.detachAll();
    yaxisLeft.detachAll();
    yaxisRight.detachAll();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the number of digits on the X axis major tic mark labels to display before resorting to an
                      exponent display.

  <br>
  @param numDigits Maximum number of digits
	*********************************************************************************************************************/
  public void setXAxisExponentDisplayThreshold(int numDigits)
  {
    xaxis.exponentDisplayThreshold = numDigits;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the number of digits on the Y axis major tic mark labels to display before resorting to an
                      exponent display.

  <br>
  @param numDigits Maximum number of digits
	*********************************************************************************************************************/
  public void setYAxisExponentDisplayThreshold(int numDigits)
  {
    yaxisLeft.exponentDisplayThreshold = numDigits;
    yaxisRight.exponentDisplayThreshold = numDigits;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the number of significant digits after the decimal on the X axis major tic mark labels to
                      display before truncating.

  <br>
  @param numDigits Maximum number of digits
	*********************************************************************************************************************/
  public void setXAxisSigDigitDisplay(int numDigits)
  {
    xaxis.sigDigitDisplay = numDigits;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the number of significant digits after the decimal on the Y axis major tic mark labels to
                      display before truncating.

  <br>
  @param numDigits Maximum number of digits
	*********************************************************************************************************************/
  public void setYAxisSigDigitDisplay(int numDigits)
  {
    yaxisLeft.sigDigitDisplay = numDigits;
    yaxisRight.sigDigitDisplay = numDigits;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns all of the data sets currently contained in the chart.

  <br>
  @return Array of all data sets contained in the chart
	*********************************************************************************************************************/
  public DataSet[] getDataSets()
  {
    return(graph.getDataSetList());
  }

  public void propertyChange(PropertyChangeEvent e)
  {
    graph.repaint();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current range limits of the X axis range slider.

  <br>
  @return Range limits of X axis range slider
	*********************************************************************************************************************/
  public RangeModel getXScrollerRangeLimit()
  {
    return(new RangeModel(xRC.getMinDValue(), xRC.getMaxDValue()));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current range limits of the Y axis range slider.

  <br>
  @return Range limits of Y axis range slider
	*********************************************************************************************************************/
  public RangeModel getYScrollerRangeLimit()
  {
    return(new RangeModel(yRC.getMinDValue(), yRC.getMaxDValue()));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current range of the X axis range slider.

  <br>
  @return Range of X axis range slider
	*********************************************************************************************************************/
  public RangeModel getXScrollerRange()
  {
    return(xRC.getRange());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current range of the Y axis range slider.

  <br>
  @return Range of Y axis range slider
	*********************************************************************************************************************/
  public RangeModel getYScrollerRange()
  {
    return(yRC.getRange());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the range of the X axis range slider.

  <br>
  @param range New range of X axis range slider
	*********************************************************************************************************************/
  public void setXScrollerRange(RangeModel range)
  {
    xRC.setRange(range);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the range of the Y axis range slider.

  <br>
  @param range New range of Y axis range slider
	*********************************************************************************************************************/
  public void setYScrollerRange(RangeModel range)
  {
    yRC.setRange(range);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current minimum and maximum range limits of the X axis range slider.

  <br>
  @return Minimum and maximum range limits of X axis range slider
	*********************************************************************************************************************/
  public RangeModel getTotalXRange()
  {
    return(new RangeModel(graph.getXmin(), graph.getXmax()));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current minimum and maximum range limits of the Y axis range slider.

  <br>
  @return Minimum and maximum range limits of Y axis range slider
	*********************************************************************************************************************/
  public RangeModel getTotalYRange()
  {
    return(new RangeModel(graph.getYmin(), graph.getYmax()));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Resets minimum and maximum range limits of the X and Y axis range sliders based on the minimum
                      and maximum values of all data sets contained in the chart.
	*********************************************************************************************************************/
  public void resetTotalRange()
  {
    xRC.setSliderRange(graph.getXmin(), graph.getXmax());
//    yRC.setSliderRange((float)graph.getYmin(), (float)graph.getYmax());
    //yRC.setSliderRange(0.0, graph.getYmax() + graph.getYmax()*0.10);
    yRC.setSliderRange(0.0, graph.getYmax() + graph.getYmax()*additionalSpace);
//    resetYRangeScroller();
  }

	/*********************************************************************************************************************
  <b>Description</b>: Resets minimum and maximum range limits of the Y axis range slider based on the minimum and
                      maximum Y values of all data sets contained in the chart.
	*********************************************************************************************************************/
  public void resetYRangeScroller()
  {
    double yMax = graph.getYmaxInRange(graph.getXmin(), graph.getXmax());
    if (Double.isNaN(yMax))
    {
      yRC.setSliderRange(0.0, 1.0);
      yRC.setRange(new RangeModel(0.0, 1.0));
    }
    else
    {
      //yRC.setSliderRange(0.0, yMax + yMax*0.10);
      yRC.setSliderRange(0.0, yMax + yMax * additionalSpace);
      //yRC.setRange(new RangeModel(0.0, yMax + yMax*0.10));
      yRC.setRange(new RangeModel(0.0, yMax + yMax * additionalSpace));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Resets the minimum and maximum range value limits of the X axis range slider to the specified
                      values.

  <br>
  @param min New minimum range limit of X axis range slider
  @param max New maximum range limit of X axis range slider
	*********************************************************************************************************************/
  public void resetTotalXRange(double min, double max)
  {
    xRC.setSliderRange(min, max);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Resets the minimum and maximum range value limits of the Y axis range slider to the specified
                      values.

  <br>
  @param min New minimum range limit of Y axis range slider
  @param max New maximum range limit of Y axis range slider
	*********************************************************************************************************************/
  public void resetTotalYRange(double min, double max)
  {
    yRC.setSliderRange(min, max);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Resets the current range of the X and Y axis range sliders to the current minumum and maximum
                      range limit values.
	*********************************************************************************************************************/
  public void resetRange()
  {
//    xRC.setRange(new RangeModel((int)graph.getXmin(), (int)graph.getXmax()));
    xRC.setRange(new RangeModel(xRC.getMinDValue(), xRC.getMaxDValue()));
//    yRC.setRange(new RangeModel((int)graph.getYmin(), (int)graph.getYmax()));
//    yRC.setRange(new RangeModel(0, (int)graph.getYmax()));
    yRC.setRange(new RangeModel(yRC.getMinDValue(), yRC.getMaxDValue()));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Creates a stackable bar data with the specified bar width.

  <br><b>Notes</b>:<br>
	                  - The data set is automatically attached to the chart

  <br>
  @param width Width of the bars of the data set
  @return New stackable bar data set
	*********************************************************************************************************************/
  public StackableBarDataSet createStackableBarDataSet(double width)
  {
    StackableBarDataSet dataSet = new StackableBarDataSet(graph, xaxis, yaxisLeft, yaxisRight, width, this);

    return(dataSet);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the title of the chart.

  <br>
  @param tileString New title
	*********************************************************************************************************************/
  public void setTitle(String tileString)
  {
    title.removeAll();
    title.add(new JLabel(tileString, SwingConstants.CENTER), BorderLayout.CENTER);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the base time the chart uses for the data set data.

  <br><b>Notes</b>:<br>
	                  - The chart needs to be created with a X time axis for this method to have any effect.

  <br>
  @param time Base time
	*********************************************************************************************************************/
  public void setBaseTime(long time)
  {
    if (xaxis instanceof TimeAndDateAxis)
    {
      ((TimeAndDateAxis)xaxis).baseTime = time;
      ((CMThumbSliderDateAndTimeRangeControl)xRC).setBaseTime(time);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the time scale the chart uses for the data set data.

  <br><b>Notes</b>:<br>
	                  - The chart needs to be created with a X time axis for this method to have any effect.

  <br>
  @param scale Time scale
	*********************************************************************************************************************/
  public void setTimeScale(long scale)
  {
    if (xaxis instanceof TimeAndDateAxis)
    {
      ((TimeAndDateAxis)xaxis).timeScale = scale;
      ((CMThumbSliderDateAndTimeRangeControl)xRC).setTimeScale(scale);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the C-Date zero time the chart uses for the data set data.

  <br><b>Notes</b>:<br>
	                  - The chart needs to be created with a X time axis and have C-Date display enabled for this method
	                    to have any effect.

  <br>
  @param date Zero time C-Date value
	*********************************************************************************************************************/
  public void setCDate(long date)
  {
    if (xaxis instanceof TimeAndDateAxis)
    {
      ((TimeAndDateAxis)xaxis).cDate = date;
      ((CMThumbSliderDateAndTimeRangeControl)xRC).setCDate(date);
      graph.repaint();
    }
  }

  public void updateUI()
  {
    super.updateUI();

    doUIUpdate();
  }

  public void doUIUpdate()
  {
    if (graph == null)
    {
      return;
    }

    if (UIManager.get("Graph.dataSeries1") == null)
    {
        plotColors = defaultColors;
    }
    else
    {
      Vector colorList = new Vector();
      int i = 1;
      Object nextResource = null;
      while ((nextResource = UIManager.get("Graph.dataSeries" + i++)) != null)
      {
          colorList.add(nextResource);
      }

      plotColors = (Color[])colorList.toArray(new Color[colorList.size()]);
    }

    Color backgroundColor = (Color)MetalLookAndFeel.getControl();
    Color foregroundColor = (Color)MetalLookAndFeel.getControlTextColor();
    Font controlTextFont = MetalLookAndFeel.getControlTextFont();
    Font subControlTextFont = MetalLookAndFeel.getSubTextFont();

    graph.setGraphBackground(backgroundColor);
    graph.setDataBackground(backgroundColor);
    graph.framecolor = foregroundColor;
    if (UIManager.get("Graph.grid") != null)
    {
      graph.gridcolor = (Color)UIManager.get("Graph.grid");
      setXDividerColor(graph.gridcolor);
    }
    else
    {
      graph.gridcolor = defaultGridColor;
      setXDividerColor(defaultGridColor);
    }

    xaxis.setLabelFont(subControlTextFont);
    xaxis.setTitleFont(controlTextFont);
    xaxis.setTitleColor(foregroundColor);
    xaxis.axiscolor = foregroundColor;

    yaxisLeft.setLabelFont(subControlTextFont);
    yaxisLeft.setTitleFont(controlTextFont);
    yaxisLeft.setTitleColor(foregroundColor);
    yaxisLeft.axiscolor = foregroundColor;

    yaxisRight.setLabelFont(subControlTextFont);
    yaxisRight.setTitleFont(controlTextFont);
    yaxisRight.setTitleColor(foregroundColor);
    yaxisRight.axiscolor = foregroundColor;

    resetDataSetColors();

    // Update the sliders in case they are not visible (added to the layout)
    xRC.getSlider().updateUI();
    yRC.getSlider().updateUI();
  }

  protected void resetDataSetColors()
  {
    lastColor = 0;

    if (graph != null)
    {
      DataSet[] dataSetList = graph.getDataSetList();
      for (int i=0; i<dataSetList.length; i++)
      {
        if (dataSetList[i] instanceof StackableBarDataSet)
        {
          ((StackableBarDataSet)dataSetList[i]).resetDataSetColors();
        }
        else
        {
          if (dataSetList[i].automaticallySetColor)
          {
            dataSetList[i].linecolor = getColor(dataSetList[i].linecolor, dataSetList[i].colorNumber);
          }
        }
      }
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Implemented method of color producer to provide a color for data sets.  Called when a stackable
                      bar data set is defining colors for stacked data sets.

  <br><b>Notes</b>:<br>
	                  - Any notes about the method goes here

  <br>
  @param currentColor Current color of the data set
  @param colorNumber Current color number of the data set
  @return Color to be used
	*********************************************************************************************************************/
  public Color getColor(Color currentColor, int colorNumber)
  {
    if ((plotColors != null) && (colorNumber > -1) && (colorNumber < plotColors.length))
    {
      currentColor = plotColors[colorNumber];
    }
    else if (plotColors != null)
    {
      currentColor = plotColors[lastColor%plotColors.length];
      lastColor = (lastColor%plotColors.length == 0) ? 1 : lastColor +1;
    }

    return(currentColor);
  }

  protected void rangeChanged(CMThumbSliderRangeControl rC, double[] minMax, Axis[] axisList, double scrollSize, boolean scrollLock, PropertyChangeListener listener)
  {
    double currentMin = rC.getRange().getDMin();
    double currentMax = rC.getRange().getDMax();

    if ((currentMin == currentMax) || ((minMax[0] == currentMin) && (minMax[1] == currentMax)))
    {
      return;
    }

    if (scrollLock)
    {
      rC.removePropertyChangeListener("range", listener);
      if (minMax[0] != currentMin)
      {
        rC.setRange(new RangeModel(currentMin, currentMin + scrollSize));
      }
      else if (minMax[1] != currentMax)
      {
        rC.setRange(new RangeModel(currentMax - scrollSize, currentMax));
      }
      else
      {
        rC.setRange(new RangeModel(minMax[0], minMax[1]));
      }
      rC.addPropertyChangeListener("range", listener);
    }

    minMax[0] = rC.getRange().getDMin();
    minMax[1] = rC.getRange().getDMax();

    // Must set min and max
    for (int i=0; i<axisList.length; i++)
    {
      axisList[i].minimum = minMax[0];
      axisList[i].maximum = minMax[1];
    }

    graph.repaint();
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: CChart test example.  Use "java org.cougaar.logistics.ui.stoplight.ui.components.CChart" to run.
	*********************************************************************************************************************/
  public static void main(String[] args)
  {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.getContentPane().setLayout(new BorderLayout());
    CChart chart = new CChart("chartTitle", "xLabel", "yLabel", false);
    frame.getContentPane().add(chart, BorderLayout.CENTER);
    
    double[] data = new double[10*2];
    data[0] = 0.0;
    data[1] = 1.3;
    for (int i=2; i<data.length; i+=2)
    {
      data[i] = (double)i;
      data[i+1] = (double)i/(double)data.length/2.0;
    }

    try
    {
      DataSet dataSet = new PolygonFillableDataSet(data, data.length/2, false);
      chart.attachDataSet(dataSet);
      chart.resetTotalRange();
      chart.resetRange();

      chart.setXAxisSigDigitDisplay(2);
      chart.setYAxisSigDigitDisplay(2);
      chart.setXAxisExponentDisplayThreshold(0);
      chart.setYAxisExponentDisplayThreshold(0);
    }
    catch (Throwable t)
    {
      t.printStackTrace();
    }
    
    frame.setSize(400, 400);
    frame.show();
  }
}
