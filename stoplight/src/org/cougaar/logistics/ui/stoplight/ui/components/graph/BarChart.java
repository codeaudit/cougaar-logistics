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

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Component;
import java.awt.Dimension;

public class BarChart extends java.awt.Canvas
{
  public static final int    VERTICAL = 0;
  public static final int    HORIZONTAL = 1;

  public static final int    SOLID = 0;
  public static final int    STRIPED = 1;

  public int orientation = VERTICAL;

  public Font titleFont;
  public FontMetrics titleFontMetrics;

  public int columns = 0;
  public Color colors[] = null;
  public String labels[] = null;
  public int styles[] = null;

  public int maxLabelHeight = 0;
  public int barWidth = 0;

  public int barSpacing = 10;

  private Dimension displaySize = null;
  private int width = 0;
  private int height = 0;

  private Object dataSemaphore = new Object();
  private double scale = 10.0;
  public int values[] = null;

  public BarChart(int orient, int size, double scale, Font font)
  {
    orientation = orient;
    switch (orientation)
    {
      case VERTICAL:
      default:
        height = size;
      break;

      case HORIZONTAL:
        width = size;
      break;
    }

    this.scale = scale;

    titleFont = font;
    titleFontMetrics = getFontMetrics(titleFont);

    maxLabelHeight = titleFontMetrics.getMaxDescent() + titleFontMetrics.getMaxAscent();

    barWidth = maxLabelHeight + 20;
  }

  public void setScale(double newScale)
  {
    synchronized (dataSemaphore)
    {
      scale = newScale;
    }
  }

  public void setValue(int index, int value)
  {
    synchronized (dataSemaphore)
    {
      values[index] = value;
    }
  }

  public void setValues(int newValues[])
  {
    synchronized (dataSemaphore)
    {
      values = newValues;
    }
  }

  public int getMaxValue()
  {
    int max = 0;
    for (int i=0; i<values.length; i++)
    {
      if (values[i] > max)
      {
        max = values[i];
      }
    }

    return(max);
  }

  public Dimension preferredSize()
  {
    return(minimumSize());
  }

  public Dimension minimumSize()
  {
    switch (orientation)
    {
      case VERTICAL:
      default:
        displaySize = new Dimension((columns * (barWidth + (2 * barSpacing))) - barWidth, height);
      break;

      case HORIZONTAL:
        displaySize = new Dimension(width, (columns * (barWidth + (2 * barSpacing))) - barWidth);
      break;
    }

    return(displaySize);
  }

  public void paint(Graphics g)
  {
    synchronized (dataSemaphore)
    {
      switch (orientation)
      {
        case VERTICAL:
        default:
          for (int i=0; i<columns; i++)
          {
            // Calculate the scaled value
            int value = (int)(values[i] / scale);
            if (value > getSize().height - maxLabelHeight -5)
            {
              value = getSize().height - maxLabelHeight -5;
            }

            paintVertical(g, value, i);
          }
        break;

        case HORIZONTAL:
          for (int i=0; i<columns; i++)
          {
            // Calculate the scaled value
            int value = (int)(values[i] / scale);
            if (value > getSize().height - maxLabelHeight -5)
            {
              value = getSize().height - maxLabelHeight -5;
            }

            paintHorizontal(g, value, i);
          }
        break;
      }
    }
  }

  private void paintVertical(Graphics g, int value, int i)
  {
    // Set the next X coordinate
    int cx = (barWidth + barSpacing) * i + barSpacing;

    // Set the next Y coordinate
    int cy = getSize().height - value - 1;

    // Draw the shadow bar
    g.setColor(Color.gray);
    g.fillRect(cx + 5, cy - 3, barWidth, value);

    // Draw the bar with the specified color
    g.setColor(colors[i]);
    switch (styles[i])
    {
      case SOLID:
      default:
        g.fillRect(cx, cy, barWidth, value);
      break;

      case STRIPED:
        int steps = value/2;
        int ys;

        for (int j=0; j<steps; j++)
        {
          ys = cy + (2 * j);
          g.drawLine(cx, ys, cx + barWidth, ys);
        }
      break;
    }

    // Draw bar value
    g.setColor(Color.black);
    g.drawString("" + values[i], cx, cy - titleFontMetrics.getDescent());

    // Draw the bar label
    g.setColor(Color.black);
    RTextLine textV = new RTextLine();
    textV.setText(labels[i]);
    textV.setFont(titleFont);
    textV.setRotation(90);
    textV.draw((Component)this, g, cx + maxLabelHeight +10, cy + value);
  }

  private void paintHorizontal(Graphics g, int value, int i)
  {
    // Set the Y coordinate
    int cy = ((barWidth + barSpacing) * i) + barSpacing;

    // Set the X coordinate
    int cx = 0;

    // Draw the shadow bar
    g.setColor(Color.gray);
    g.fillRect(cx + 3, cy + 5, value, barWidth);

    // Draw the bar with the specified color
    g.setColor(colors[i]);
    switch (styles[i])
    {
      case SOLID:
      default:
        g.fillRect(cx, cy, value, barWidth);
      break;

      case STRIPED:
        int steps = value/2;
        int ys;

        for (int j=0; j<steps; j++)
        {
          ys = cx + (2 * j);
          g.drawLine(ys, cy, ys, cy + barWidth);
        }
      break;
    }

    // Draw bar value
    g.setColor(Color.black);
    g.drawString("" + values[i], cx + value + 3, cy + titleFontMetrics.getAscent());

    // Draw the bar label
    g.setColor(Color.black);
    RTextLine textH = new RTextLine();
    textH.setText(labels[i]);
    textH.setFont(titleFont);
    textH.draw((Component)this, g, cx, cy + 15);
  }
}
