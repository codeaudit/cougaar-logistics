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
import java.util.*;
import javax.swing.*;

import java.awt.font.LineMetrics;

/***********************************************************************************************************************
<b>Description</b>: This class represents one slice of a pie chart.  It contains slice value, label and color.

***********************************************************************************************************************/
public class PieElement
{
  private static final Color[] defaultColors = new Color[] {Color.green, Color.pink, Color.cyan, Color.red, Color.yellow, Color.blue, Color.magenta, Color.orange};
  private static int colorCount = 0;

  String label;
  double value;
  Color color;
  
  double percentage;
  int angularDistance;

  private PieChart chart = null;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a pie slice with a label and a value.

  <br><b>Notes</b>:<br>
	                  - The color chosen for this slice will be one of the default colors:
	                      Color.green, Color.pink, Color.cyan, Color.red, Color.yellow, Color.blue, Color.magenta,
	                      Color.orange
	                    Once all of these colors have been cycled through, the cycle will start over
  <br>
  @param label Slice label
  @param value Slice value
	*********************************************************************************************************************/
  public PieElement(String label, double value)
  {
    this.label = label;
    this.value = value;
    this.color = defaultColors[colorCount++%defaultColors.length];
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Constructs a pie slice with a label, a value and a color.

  <br>
  @param label Slice label
  @param value Slice value
  @param color Slice color
	*********************************************************************************************************************/
  public PieElement(String label, double value, Color color)
  {
    this.label = label;
    this.value = value;
    this.color = color;
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Gets the current slice label.

  <br>
  @return Text of current label
	*********************************************************************************************************************/
  public String getLabel()
  {
    return(label);
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Sets the current slice label.

  <br>
  @param label New label text
	*********************************************************************************************************************/
  public void setLabel(String label)
  {
    this.label = label;
    if (chart != null)
    {
      chart.repaint();
    }
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Gets the current slice value.

  <br>
  @return Current value
	*********************************************************************************************************************/
  public double getValue()
  {
    return(value);
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Sets the current slice value.

  <br>
  @param value New value
	*********************************************************************************************************************/
  public void setValue(double value)
  {
    this.value = value;
    if (chart != null)
    {
      chart.recalculate();
      chart.repaint();
    }
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Gets the current slice color.

  <br>
  @return Current color
	*********************************************************************************************************************/
  public Color getColor()
  {
    return(color);
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Sets the current slice color.

  <br>
  @param color New color
	*********************************************************************************************************************/
  public void setColor(Color color)
  {
    this.color = color;
    if (chart != null)
    {
      chart.repaint();
    }
  }
  
	/*********************************************************************************************************************
  <b>Description</b>: Sets the current pie chart this slice is attached to.

  <br><b>Notes</b>:<br>
	                  - If the pie chart is set, it will automatically update when slice information (value, color, label)
	                    changes
	                  - To associated this pie element with another pie chart, setChart(null) must be called first to
	                    clear the current pie chart - this assures that only one pie chart will represent one pie slice

  <br>
  @param chart Pie chart slice is attached to
	*********************************************************************************************************************/
  void setChart(PieChart chart)
  {
    if (this.chart != null)
    {
      throw(new RuntimeException("PieElement already added to a pie chart!"));
    }

    this.chart = chart;
  }
  
  void calculateDimensions(double total)
  {
    percentage = value/total;
    angularDistance = (int)Math.round(percentage*360.0);
  }

  int drawSlice(Graphics g, int startingAngle, int x, int y, int radius, boolean outline)
  {
    g.setColor(color);
    g.fillArc(x-radius, y-radius, radius*2, radius*2, startingAngle, angularDistance);

    if (outline)
    {
      g.setColor(Color.black);
      g.drawArc(x-radius, y-radius, radius*2, radius*2, startingAngle, angularDistance);
      g.drawLine(x, y, x + (int)(radius * Math.cos(Math.toRadians(startingAngle))), y - (int)(radius * Math.sin(Math.toRadians(startingAngle))));
      g.drawLine(x, y, x + (int)(radius * Math.cos(Math.toRadians(startingAngle+angularDistance))), y - (int)(radius * Math.sin(Math.toRadians(startingAngle+angularDistance))));
    }

    return(angularDistance);
  }

  private String[] getLabel(boolean showLabel, boolean showValue, boolean showPercent)
  {
    Vector textList = new Vector(0);
    if ((angularDistance > 3) && (showLabel || showValue || showPercent))
    {
      if (showLabel)
      {
        textList.add(label);
      }

      if (showValue)
      {
        textList.add(Double.toString(value));
      }

      if (showPercent)
      {
        textList.add("(" + + Math.round(percentage * 100.0) + "%)");
      }
    }
    
    return((String[])textList.toArray(new String[textList.size()]));
  }

  private String getLargestString(String[] textList)
  {
    String text = "";
    
    for (int i=0; i<textList.length; i++)
    {
      if (textList[i].length() > text.length())
      {
        text = textList[i];
      }
    }
    
    return(text);
  }

  int getMaxRadius(Graphics g, int startingAngle, int radius, boolean showLabel, boolean showValue, boolean showPercent)
  {
    String[] textList = getLabel(showLabel, showValue, showPercent);
    String text = getLargestString(textList);
    
    int maxRadius = radius;
    if (text.length() > 0)
    {
    	maxRadius -= g.getFontMetrics().stringWidth(text);
    	maxRadius = (maxRadius <= 0) ? 0 : (int)Math.abs((maxRadius/Math.cos(Math.toRadians(startingAngle+(angularDistance/2)))));
    }

    int height = 0;
    for (int i=0; i<textList.length; i++)
    {
      height += getTextHeight(textList[i], g);
    }
    height = radius - height;

    maxRadius = (height < maxRadius) ? height : maxRadius;

    return((maxRadius < 0) ? 0 : maxRadius);
  }

  private int getTextHeight(String text, Graphics g)
  {
    LineMetrics lm = g.getFontMetrics().getLineMetrics(text, g);

    return((int)(lm.getAscent() + lm.getDescent()));
  }

  int drawLabel(Graphics g, int startingAngle, int x, int y, int radius, boolean showLabel, boolean showValue, boolean showPercent)
  {
    g.setColor(Color.black);

    String[] textList = getLabel(showLabel, showValue, showPercent);
    if (textList.length > 0)
    {
      String largestText = getLargestString(textList);
  
      int angle = startingAngle+(angularDistance/2);
    	int textX = x + (int)(radius * Math.cos(Math.toRadians(angle))) + 3;
    	int textY = y - (int)(radius * Math.sin(Math.toRadians(angle))) - 3;
  
    	if ((90 <= angle%360) && (angle%360 < 270))
    	{
    	  textX -= g.getFontMetrics().stringWidth(largestText);
    	}
  
    	if ((0 <= angle%360) && (angle%360 < 180))
    	{
        for (int i=textList.length-1; i>-1; i--)
        {
          String text = textList[i];
    
          if (text.length() > 0)
          {
        	  g.drawString(text, textX, textY);
          }
          
      	  textY -= getTextHeight(textList[i], g);
        }
    	}
    	else
    	{
        for (int i=0; i<textList.length; i++)
        {
          String text = textList[i];
    
          textY += getTextHeight(textList[i], g);

          if (text.length() > 0)
          {
        	  g.drawString(text, textX, textY);
          }
        }
    	}
    }

    return(angularDistance);
  }
}
