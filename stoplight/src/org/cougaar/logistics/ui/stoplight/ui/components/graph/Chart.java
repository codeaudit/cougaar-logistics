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

package org.cougaar.logistics.ui.stoplight.ui.components.graph;

import java.util.Vector;

import java.awt.*;
import javax.swing.*;

import java.awt.event.*;

/***********************************************************************************************************************
<b>Description</b>: Chart display class that uses shared memory data represented by a double array of size 2 to create
                    a graph the the shared data values over time where the Y axis indicates the data and the X axis
                    indicates the time.  The graph can be displayed as a moving window which shows the graph data from
                    a point in history (specified by a range) to the current point in time, as well as a fixed range of
                    chart data and the whole graph of all the data collected from time zero.

<br><br><b>Notes</b>:<br>
                  - The chart class implements the Runnable interface and must be started with its own thread
                  - Uses the 3rd party graph package which was modified to provide "better" functionality

***********************************************************************************************************************/
public class Chart extends Graph2D implements Runnable
{
  /*********************************************************************************************************************
  <b>Description</b>: Define for Moving Window mode.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  public static final int MOVING_WINDOW = 1;

  /*********************************************************************************************************************
  <b>Description</b>: Define for Fixed Window mode.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  public static final int FIXED_WINDOW  = 2;

  /*********************************************************************************************************************
  <b>Description</b>: Define for Whole Graph mode.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  public static final int WHOLE_GRAPH   = 3;

  /*********************************************************************************************************************
  <b>Description</b>: Data set of all points plotted on the graph.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private DataSet dataSet = new DataSet();

  /*********************************************************************************************************************
  <b>Description</b>: Graph X axis control object.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private Axis xaxis = new Axis(Axis.BOTTOM);

  /*********************************************************************************************************************
  <b>Description</b>: Graph Y axis control object.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private Axis yaxis = new Axis(Axis.LEFT);

  /*********************************************************************************************************************
  <b>Description</b>: Shared memory data array.  This data array is checked periodically and its value is used as the
                      next data points for the graph

  <br><br><b>Notes</b>:<br>
                    - The
  *********************************************************************************************************************/
  private double[] dataArray = null;

  /*********************************************************************************************************************
  <b>Description</b>: Current image object for the display of the graph.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private Image image = null;

  /*********************************************************************************************************************
  <b>Description</b>: Current graphics object for the display of the graph.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private Graphics graphics = null;

  /*********************************************************************************************************************
  <b>Description</b>: Current number of data points read from the shared data memory array.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private int count = 0;

  /*********************************************************************************************************************
  <b>Description</b>: Number of milliseconds to delay between sampling of the shared data memory array.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private int delay = 500;

  /*********************************************************************************************************************
  <b>Description</b>: Time scale of the X axis tick marks vs number of data points.

  <br><br><b>Notes</b>:<br>
                    - Means number of sleep delays (counts) == 1.0 tick mark
  *********************************************************************************************************************/
  private double xScaleFactor = 2.0;

  /*********************************************************************************************************************
  <b>Description</b>: Minimum (lowest) X axis tick mark to display at any point in time.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double xaxisMinimum = 0.0;

  /*********************************************************************************************************************
  <b>Description</b>: Maximum (highest) X axis tick mark to display at the current point in time.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double xaxisMaximum = 50.0;

  /*********************************************************************************************************************
  <b>Description</b>: Minimum (lowest) Y axis tick mark to display at any point in time.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double yaxisMinimum = 0.0;

  /*********************************************************************************************************************
  <b>Description</b>: Maximum (highest) Y axis tick mark to display at the current point in time.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double yaxisMaximum = 50.0;

  /*********************************************************************************************************************
  <b>Description</b>: Largest X axis tick mark to display when the bigest X value is less than this value.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double xaxisMinimumSize = 50.0;

  /*********************************************************************************************************************
  <b>Description</b>: Largest Y axis tick mark to display when the bigest Y value is less than this value.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double yaxisMinimumSize = 50.0;

  /*********************************************************************************************************************
  <b>Description</b>: Current moving window range.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private double windowHistoryRange = 50.0;

  /*********************************************************************************************************************
  <b>Description</b>: Extra count to add to maximum X/Y axis tick mark displays.

  <br><br><b>Notes</b>:<br>
                    - Used to keep the graph line from ever touching the top of the graph display area
  *********************************************************************************************************************/
  private double buffer = 10.0;

  /*********************************************************************************************************************
  <b>Description</b>: Chart background color.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private Color dataBackgroundColor = Color.black;

  /*********************************************************************************************************************
  <b>Description</b>: Current display more of the chart.

  <br><br><b>Notes</b>:<br>
                    -
  *********************************************************************************************************************/
  private int mode = MOVING_WINDOW;

  // timeScale means # of sleep delays (counts) == 1.0 tick mark
  /*********************************************************************************************************************
  <b>Description</b>: Constructs a Chart object with the specified parameters.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param data Shared data memory array to use for graph data
  @param xaxisLabel Label to display for the X axis
  @param yaxisLabel Label to display for the Y axis
  @param sleepDelay Delay between sampling the shared data memory array
  @param timeScale  Number of sleep delays (counts) that are equal to 1.0 graph tick mark on the X (time) axis
  *********************************************************************************************************************/
  public Chart(double[] data, String xaxisLabel, String yaxisLabel, int sleepDelay, double timeScale)
  {
    try
    {
      // Set up the various members with default or specified values
      delay = sleepDelay;
      xScaleFactor = timeScale;
      dataArray = data;
      setDataBackground(dataBackgroundColor);

      dataSet.append(data, 1);
      dataSet.linecolor = new Color(0, 0, 255);
      attachDataSet(dataSet);

      xaxis.attachDataSet(dataSet);
      xaxis.setTitleText(xaxisLabel);
      xaxis.minimum = xaxisMinimum;
      xaxis.maximum = xaxisMaximum;
      attachAxis(xaxis);

      yaxis.attachDataSet(dataSet);
      yaxis.setTitleText(yaxisLabel);
      yaxis.minimum = yaxisMinimum;
      yaxis.maximum = yaxisMaximum;
      attachAxis(yaxis);

      // Add a listener that will initiate redrawing the chart when its containing window is resized
      addComponentListener(new ResizeAdapter());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  /*********************************************************************************************************************
  <b>Description</b>: Main thread method which samples the shared memory data array, graphs the data point and then
                      sleeps for the specified interval before starting again.

  <br><b>Notes</b>:<br>
                    -

  <br>
  *********************************************************************************************************************/
  public void run()
  {
    try
    {
      // Start infinite loop for drawing graph
      while (true)
      {
        synchronized(dataArray)
        {
          // Calculate the data point to graph based on the shared memory array for this chart
          dataArray[0] = (double)count/xScaleFactor;
          dataSet.append(dataArray, 1);
        }

        // Draw the graph
        resetAxisMinMaxAndDrawGraph();

        // Keep track of the count so the data points that are graphed match with the scale of the X axis
        count++;

        // Sleep until next iteration
        Thread.sleep(delay);
      }
    }
    catch (InterruptedException e)
    {
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /*********************************************************************************************************************
  <b>Description</b>: Gets the maximum Y data value within the specified X value range.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param xMin Minimum X data value
  @param xMax Maximum X data value
  @return The maximum Y data value within the specified range
  *********************************************************************************************************************/
  private double getYmaxInRange(int xMin, int xMax)
  {
    // If the requested max value is larger than the number of available points, use the last point instead
    if (xMax > (dataSet.dataPoints() -1))
    {
      xMax = dataSet.dataPoints() -1;
    }

    // Go through each data point in the range, comparing them to find the largest Y value
    double yMax = 0.0;
    for (int i=xMin; i<=xMax; i++)
    {
      yMax = yMax < dataSet.getPoint(i)[1] ? dataSet.getPoint(i)[1] : yMax;
    }

    return(yMax);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the buffer to add to maximum X/Y axis tick mark displays.

  <br><b>Notes</b>:<br>
                    - Used to keep the graph line from ever touching the top of the graph display area

  <br>
  @param value New buffer value
  *********************************************************************************************************************/
  public void setBuffer(double value)
  {
    // Set the buffer
    buffer = value;
    // Redraw the graph with the new buffer
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the history range for the moving window mode.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param range New range value
  *********************************************************************************************************************/
  public void setWindowHistoryRange(double range)
  {
    // Set the window history range
    windowHistoryRange = range;
    // Redraw the graph with the new range
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Gets the history range for the moving window mode.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @return History range value
  *********************************************************************************************************************/
  public double getWindowHistoryRange()
  {
    return(windowHistoryRange);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the Y axis minimum size.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param min New Y axis minimum size
  *********************************************************************************************************************/
  public void setYAxisMinimumSize(double min)
  {
    // Set the Y axis minimum
    yaxisMinimumSize = min;
    // Redraw the graph with the new minimum
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the X axis minimum size.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param min New X axis minimum size
  *********************************************************************************************************************/
  public void setXAxisMinimumSize(double min)
  {
    // Set the X axis minimum
    xaxisMinimumSize = min;
    // Redraw the graph with the new mimimum
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the minimum Y data point to display.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param min New minimum Y data point
  *********************************************************************************************************************/
  public void setYMin(double min)
  {
    // Set the Y minimum
    yaxisMinimum = min;
    // Redraw the graph with the new mimimum
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the maximum Y data point to display.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param max New maximum Y data point
  *********************************************************************************************************************/
  public void setYMax(double max)
  {
    // Set the Y maximum
    yaxisMaximum = max;
    // Redraw the graph with the new maximum
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the minimum X data point to display.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param min New minimum X data point
  *********************************************************************************************************************/
  public void setXMin(double min)
  {
    // Set the X minimum
    xaxisMinimum = min;
    // Redraw the graph with the new mimimum
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the maximum X data point to display.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param max New maximum X data point
  *********************************************************************************************************************/
  public void setXMax(double max)
  {
    // Set the X maximum
    xaxisMaximum = max;
    // Redraw the graph with the new maximum
    resetAxisMinMaxAndDrawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Gets the minimum X data point to display.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @return Minimum X data point
  *********************************************************************************************************************/
  public double getXMin()
  {
    return(xaxisMinimum);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Gets the maximum X data point to display.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @return maximum X data point
  *********************************************************************************************************************/
  public double getXMax()
  {
    return(xaxisMaximum);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the axis contols with the current values and draws the graph.

  <br><b>Notes</b>:<br>
                    -

  <br>
  *********************************************************************************************************************/
  private void resetAxisMinMaxAndDrawGraph()
  {
    // Dertermine the graph mode and adjust the visible data points
    switch (mode)
    {
      case FIXED_WINDOW:  // Don't Move any thing
      break;

      case MOVING_WINDOW:  // Move min and max X axis data points
        xaxisMaximum = (double)count/xScaleFactor > xaxisMaximum ? (double)count/xScaleFactor : xaxisMaximum;
        xaxisMaximum = xaxisMaximum > xaxisMinimumSize ? xaxisMaximum : xaxisMinimumSize;
        xaxisMinimum = 0.0 > (xaxisMaximum - windowHistoryRange) ? 0.0 : (xaxisMaximum - windowHistoryRange);
      break;

      case WHOLE_GRAPH:  // Move max X if needed to whatever the current count is
        xaxisMinimum = 0.0;
        xaxisMaximum = (double)count/xScaleFactor;
      break;
    }

    // Get the maximum Y in the range of X values calculated from the graph mode
    double maxyInRange = getYmaxInRange((int)(xaxisMinimum*xScaleFactor), (int)(xaxisMaximum*xScaleFactor));
    yaxisMaximum = maxyInRange >= yaxisMinimumSize ? maxyInRange + buffer : yaxisMinimumSize;

    // Set X diplay point values
    xaxis.minimum = xaxisMinimum;
    xaxis.maximum = xaxisMaximum;

    // Set Y diplay point values
    yaxis.minimum = yaxisMinimum;
    yaxis.maximum = yaxisMaximum;

    // Draw the graph
    drawGraph();
  }

  /*********************************************************************************************************************
  <b>Description</b>: Sets the current display mode of the graph (Moving Window, Fixed Window or Whole Graph).

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param newMode New display mode
  *********************************************************************************************************************/
  public void setMode(int newMode)
  {
    mode = newMode;
  }

  /*********************************************************************************************************************
  <b>Description</b>: Calculates the display size of the graph based on passed in values.

  <br><b>Notes</b>:<br>
                    -

  <br>
  @param width Graph width
  @param height Graph height
  *********************************************************************************************************************/
  private void sizeGraph(int width, int height)
  {
    // Set up the grphical display for the graph and base th image on the specified display size
    image = createImage(width, height);
    graphics = image.getGraphics();
    graphics.setColor(Chart.this.getBackground());
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(getGraphics().getColor());
    graphics.clipRect(0, 0, width, height);
  }

  /*********************************************************************************************************************
  <b>Description</b>: Draws the graph on the screen.

  <br><b>Notes</b>:<br>
                    -

  <br>
  *********************************************************************************************************************/
  private void drawGraph()
  {
    if (graphics != null)
    {
      // Set the graphics object
      update(graphics);
      // Draw the chart image
      try
      {
        getGraphics().drawImage(image, 0, 0, this);
      }
      catch(Exception e)
      {
      }
    }
  }

  /***********************************************************************************************************************
  <b>Description</b>: Adapter class to recalculate and redraw the graph when the graph's containing component is resized.

  <br><br><b>Notes</b>:<br>
                    -

  ***********************************************************************************************************************/
  private class ResizeAdapter extends ComponentAdapter
  {
    /*********************************************************************************************************************
    <b>Description</b>: Called when the component containing the chart object is resized.  This method will recalculate
                        the graph dimensions and redraw the graph.

    <br><b>Notes</b>:<br>
                      - Required by the ComponentAdapter super class

    <br>
    @param e Event object that triggered the method call
    *********************************************************************************************************************/
    public void componentResized(ComponentEvent e)
    {
      // Get the new size of the window
      int width = getSize().width;
      int height = getSize().height;

      // If the window is minimized, set the size to be 1 X 1 to prevent a error when drawing the chart
      if (width < 1) width = 1;
      if (height < 1) height = 1;

      // Size and draw the chart
      sizeGraph(width, height);
      drawGraph();
    }
  }
}
