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
import java.awt.image.*;

import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;

/***********************************************************************************************************************
<b>Description</b>: This class represents a histogram data set which is used along with the
                    org.cougaar.logistics.ui.stoplight.ui.components.HChart component to display a histogram.

***********************************************************************************************************************/
public class HistogramDataSet
{
  private static Graphics displayGraphics = (new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)).createGraphics();

  HChart chart;
  
  InternalBarDataSet dataset;

  double distanceBetweenBars = 0.0;

  private Color[] barColors;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a histogram data set with the specified labels and values.

  <br><b>Notes</b>:<br>
	                  - The value points (x,y) and labels need to have a 1 to 1 correlation

  <br>
  @param values Array of (x,y) points where values[n] is the x value and values[n+1] is the y value
  @param labels Labels of each data point where (values[n], values[n+1]) correspond to the label labels[n/2]
	*********************************************************************************************************************/
  public HistogramDataSet(double[] values, String[] labels)
  {
    this(values, labels, null, 0.0);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a histogram data set with the specified labels, values, distance between bars and an
                      array of bar colors.

  <br><b>Notes</b>:<br>
	                  - The bar color array may be less than the number of bars such that colors will be repeated if
	                    necessary.

  <br>
  @param values Array of (x,y) points where values[n] is the x value and values[n+1] is the y value
  @param labels Labels of each data point where (values[n], values[n+1]) correspond to the label labels[n/2]
  @param colors Bar color array of 1 or more colors, or null
  @param distanceBetweenBars Space distance drawn between bars with a minimum value of 0.0
	*********************************************************************************************************************/
  public HistogramDataSet(double[] values, String[] labels, Color[] colors, double distanceBetweenBars)
  {
    double[] valueArray = new double[values.length*2];
    barColors = colors;
    this.distanceBetweenBars = distanceBetweenBars;
    
    for (int i=0; i<valueArray.length; i+=2)
    {
      valueArray[i] = i/2 + distanceBetweenBars;
      valueArray[i+1] = values[i/2];
    }

    try
    {
      dataset = new InternalBarDataSet(valueArray, valueArray.length/2, labels);
    }
    catch (Exception ex)
    {
      throw(new RuntimeException("Re-Thrown exception: " + ex));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Changes the bar data value of the specified bar.

  <br><b>Notes</b>:<br>
	                  - The org.cougaar.logistics.ui.stoplight.ui.components.HCart.resetRangeLimits() method should be called
	                    when data set values are changed to keep the y-axis range limits correct

  <br>
  @param barNumber Bar number of bar to change
  @param newValue New value of the specified bar
	*********************************************************************************************************************/
  public void changeData(int barNumber, double newValue)
  {
    dataset.changeData(barNumber, newValue);
  }

  DataSet getDataSet(double barWidth)
  {
    dataset.barWidth = barWidth;

    return(dataset);
  }

  private class InternalBarDataSet extends BarDataSet
  {
    private String[] labels;
    
    private RTextLine textV;
    
    public InternalBarDataSet(double[] values, int length, String[] labels) throws Exception
    {
      super(values, length, true, 1.0);
      
      this.labels = labels;

      textV = new RTextLine();
      textV.setRotation(90);
    }

    private boolean recalcTemp = false;

    void changeData(int barNumber, double newValue)
    {
      super.change(barNumber, newValue);
      recalcTemp = true;
    }

    protected String getPointAsString(double x, double y)
    {
      int index = (int)(x/(1.0 + distanceBetweenBars));
      return("(" + labels[index] + "," + yaxis.getPointAsString(y) + ")");
    }

    protected void draw_lines(Graphics g, Rectangle w)
    {
      if (recalcTemp)
      {
        super.calculateTemp();
        recalcTemp = false;
      }

      if (barData.length == 0)
      {
        return;
      }

      Color oldC = g.getColor();

      Color[] colors;
      if (HistogramDataSet.this.barColors == null)
      {
        colors = new Color[] {oldC};
      }
      else
      {
        colors = HistogramDataSet.this.barColors;
      }

      Color labelColor = Color.black;
      boolean drawLabel = true;
      int offset = 0;
      int maxWidth = (int)(w.x + ((temp[6] + xValueOffset - xmin)/xrange)*w.width) - (int)(w.x + ((temp[0] + xValueOffset - xmin)/xrange)*w.width);
      for (int i=14; i>=7; i--)
      {
        Font font = new Font("Times-Roman", Font.PLAIN, i);
        FontMetrics fm = g.getFontMetrics(font);
        textV.setFont(font);
        if (textV.getRWidth(g) < maxWidth || i == 7)
        {
          offset = (maxWidth - textV.getRWidth(g))/2;

          if (i == 7)
          {
            textV.setFont(new Font("Times-Roman", Font.PLAIN, 14));
            drawLabel = false;
          }
          else
          {
            textV.setFont(font);
          }

          break;
        }
      }

      double[] data = new double[8];
      for (int i=0, colorIndex=0; i<temp.length; i+=8, colorIndex++)
      {
        data[0] = temp[i+0];
        data[1] = temp[i+1];
        data[2] = temp[i+2];
        data[3] = temp[i+3];
        data[4] = temp[i+4];
        data[5] = temp[i+5];
        data[6] = temp[i+6];
        data[7] = temp[i+7];

        g.setColor(colors[colorIndex%colors.length]);
        draw_lines(g, w, data, 8, 0.0);

        if (drawLabel && (data[4] >= xmin) && (data[4] <= xmax))
        {
          textV.setText(labels[i/8]);
          int x0 = (int)(w.x + ((data[4] + xValueOffset - xmin)/xrange)*w.width - offset);
          int y0 = (int)(w.y + (1.0 - (data[5] + yValueOffset + 0.0 - ymin)/yrange)*w.height);
          y0 = ((w.y + w.height) < y0) ? (w.y + w.height) : y0;
          g.setColor(labelColor);
          textV.draw(g2d, g, x0, y0);
        }
      }

      g.setColor(oldC);
    }

  	/*********************************************************************************************************************
    <b>Description</b>: Returns the maximum Y value within the specified X range.
  
    <br>
    @param xMin Minimum X value
    @param xMax Maximum X value
    @return Maximum Y value in the specified X range
  	*********************************************************************************************************************/
    public double getYmaxInRange(double xMin, double xMax)
    {
/*      if (barData.length == 0)
      {
        return(Double.NaN);
      }

      int maxWidth = (int)(w.x + ((temp[6] + xValueOffset - xmin)/xrange)*w.width) - (int)(w.x + ((temp[0] + xValueOffset - xmin)/xrange)*w.width);
      for (int i=14; i>=7; i--)
      {
        Font font = new Font("Times-Roman", Font.PLAIN, i);
        FontMetrics fm = HistogramDataSet.this.displayGraphics.getFontMetrics(font);
        if (fm.getHeight() < maxWidth || i == 7)
        {
          textV.setFont(font);
        }
      }*/

      // Go through each data point in the range, comparing them to find the largest Y value
      double yMax = Double.NaN;
      for (int i=0; i<barData.length; i+=stride)
      {
        if ((xMin <= barData[i]) && (barData[i] <= xMax))
        {
          textV.setText(labels[i/stride]);
          int height = textV.getRHeight(HistogramDataSet.this.displayGraphics);
          double pixelHeight = g2d.get1PixelHeight(height);
          double size = barData[i+1] + height*pixelHeight;
          yMax = ((yMax < size) || (Double.isNaN(yMax))) ? size : yMax;
        }
      }

      return(yMax);
    }

  	/*********************************************************************************************************************
    <b>Description</b>: Returns the maximum Y value in the data set according to extra space needed for bar labels.
  
    <br>
    @return Maximum Y value in the data set
  	*********************************************************************************************************************/
    public double getYmax()
    {
      return(getYmaxInRange(dxmin, dxmax));
    }

  	/*********************************************************************************************************************
    <b>Description</b>: Returns the maximum Y value in the data set.
  
    <br>
    @return Maximum Y value in the data set
  	*********************************************************************************************************************/
    public double getYDataValueMax()
    {
      return(super.getYmax());
    }
  }
}
