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

/***********************************************************************************************************************
<b>Description</b>: This class is a pie chart component capable of drawing a pie chart with labels, values and
                    percentages.  A right click menu, which can be disabled, is provided for dynamic control for the
                    display properties of the chart.

***********************************************************************************************************************/
public class PieChart extends JPanel
{
  private static final int BORDER = 10;
  
  private Vector pieElements = new Vector(0);

  private boolean antiAliasing = true;
  private boolean outlineSlices = true;
  private boolean shadow = true;

  private boolean drawLabels = true;

  private boolean showLabels = true;
  private boolean showValues = true;
  private boolean showPercents = true;

  private PieChartSizer sizer = null;

  private int startingOffset = 45;

  private Font font = null;
  
	/*********************************************************************************************************************
  <b>Description</b>: Default constructor builds an empty pie chart with a right click properties menu.
	*********************************************************************************************************************/
  public PieChart()
  {
    addMouseListener(new PopupMouseListener());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a pie chart with slices corresponding to the specified lables and values and a right
                      click properties menu.

  <br>
  @param labels Array of labels where the array index corresponds to the specified values array index
  @param values Array of values where the array index corresponds to the specified lables array index
	*********************************************************************************************************************/
  public PieChart(String[] labels, double[] values)
  {
    this();
    
    for (int i=0; i<labels.length; i++)
    {
      addPieElement(labels[i], values[i]);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a pie chart with slices corresponding to the specified lables, values and colors and a
                      right click properties menu.

  <br>
  @param labels Array of labels where the array index corresponds to the specified values/colors array index
  @param values Array of values where the array index corresponds to the specified lables/colors array index
  @param colors Array of colors where the array index corresponds to the specified lables/values array index
	*********************************************************************************************************************/
  public PieChart(String[] labels, double[] values, Color[] colors)
  {
    this();
    
    for (int i=0; i<labels.length; i++)
    {
      addPieElement(labels[i], values[i], colors[i]);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a pie chart with slices corresponding to the specified lables and values.

  <br>
  @param labels Array of labels where the array index corresponds to the specified values array index
  @param values Array of values where the array index corresponds to the specified lables array index
  @param enablePopup True if the right click properties menu should be enabled, false otherwise
	*********************************************************************************************************************/
  public PieChart(String[] labels, double[] values, boolean enablePopup)
  {
    if (enablePopup)
    {
      addMouseListener(new PopupMouseListener());
    }
    
    for (int i=0; i<labels.length; i++)
    {
      addPieElement(labels[i], values[i]);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a pie chart with slices corresponding to the specified lables, values and colors.

  <br>
  @param labels Array of labels where the array index corresponds to the specified values/colors array index
  @param values Array of values where the array index corresponds to the specified lables/colors array index
  @param colors Array of colors where the array index corresponds to the specified lables/values array index
  @param enablePopup True if the right click properties menu should be enabled, false otherwise
	*********************************************************************************************************************/
  public PieChart(String[] labels, double[] values, Color[] colors, boolean enablePopup)
  {
    if (enablePopup)
    {
      addMouseListener(new PopupMouseListener());
    }
    
    for (int i=0; i<labels.length; i++)
    {
      addPieElement(labels[i], values[i], colors[i]);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the font of the slice labels.

  <br>
  @param f Lebel font
	*********************************************************************************************************************/
  public void setFont(Font f)
  {
    font = f;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the pie chart sizer object for this chart.

  <br><b>Notes</b>:<br>
	                  - Setting this to a non-null object disables automatic sizing
	                  - Setting this to null enables automatic sizing

  <br>
  @param sizer PieChartSizer object
	*********************************************************************************************************************/
  public void setPieChartSizer(PieChartSizer sizer)
  {
    this.sizer = sizer;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns anti-aliasing graphics on/off.

  <br>
  @param value True if anti-aliasing is to be used, false otherwise
	*********************************************************************************************************************/
  public void setAntiAliasing(boolean value)
  {
    antiAliasing = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns slice outline on/off.

  <br>
  @param value True if slice outlining is to be used, false otherwise
	*********************************************************************************************************************/
  public void setOutlineSlices(boolean value)
  {
    outlineSlices = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns pie shadow on/off.

  <br>
  @param value True if pie shadowing is to be used, false otherwise
	*********************************************************************************************************************/
  public void setShadow(boolean value)
  {
    shadow = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns slice labels on/off.

  <br>
  @param value True if slice labels are to be drawn, false otherwise
	*********************************************************************************************************************/
  public void setDrawLabels(boolean value)
  {
    drawLabels = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns slice labels on/off.

  <br>
  @param value True if slice labels are to be drawn, false otherwise
	*********************************************************************************************************************/
  public void setShowLabels(boolean value)
  {
    showLabels = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns slice values on/off.

  <br>
  @param value True if slice values are to be drawn, false otherwise
	*********************************************************************************************************************/
  public void setShowValues(boolean value)
  {
    showValues = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Turns slice percents on/off.

  <br>
  @param value True if slice percents are to be drawn, false otherwise
	*********************************************************************************************************************/
  public void setShowPercents(boolean value)
  {
    showPercents = value;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a pie slice to the pie chart with the specified label and value.

  <br><b>Notes</b>:<br>
	                  - The pie chart will automatically repaint

  <br>
  @param label Slice label
  @param value Slice value
  @return Pie slice that was added to the chart
	*********************************************************************************************************************/
  public PieElement addPieElement(String label, double value)
  {
    PieElement element = new PieElement(label, value);
    pieElements.add(element);
    element.setChart(this);

    recalculate();
    
    repaint();
    
    return(element);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a pie slice to the pie chart with the specified label, value and color.

  <br><b>Notes</b>:<br>
	                  - The pie chart will automatically repaint

  <br>
  @param label Slice label
  @param value Slice value
  @param color Slice color
  @return Pie slice that was added to the chart
	*********************************************************************************************************************/
  public PieElement addPieElement(String label, double value, Color color)
  {
    PieElement element = new PieElement(label, value, color);
    pieElements.add(element);
    element.setChart(this);

    recalculate();
    
    repaint();
    
    return(element);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Adds a pie slice to the pie chart.

  <br><b>Notes</b>:<br>
	                  - The pie chart will automatically repaint

  <br>
  @param element Pie slice to add to the chart
  @return Pie slice that was added to the chart
	*********************************************************************************************************************/
  public PieElement addPieElement(PieElement element)
  {
    pieElements.add(element);
    element.setChart(this);

    recalculate();

    repaint();
    
    return(element);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets all of the current pie slices in the chart.

  <br>
  @return All pie slices the chart has
	*********************************************************************************************************************/
  public PieElement[] getAllPieElements()
  {
    return((PieElement[])pieElements.toArray(new PieElement[pieElements.size()]));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Removes a pie slice from the pie chart.

  <br><b>Notes</b>:<br>
	                  - The pie chart will automatically repaint

  <br>
  @param element Pie slice to remove from the chart
  @return Pie slice that was removed from the chart
	*********************************************************************************************************************/
  public PieElement removePieElement(PieElement element)
  {
    if (pieElements.remove(element))
    {
      element.setChart(null);

      recalculate();

      repaint();

      return(element);
    }

    return(null);
  }

  void recalculate()
  {
    double total = 0.0;
    for (int i=0, isize=pieElements.size(); i<isize; i++)
    {
      total += ((PieElement)pieElements.get(i)).value;
    }

    for (int i=0, isize=pieElements.size(); i<isize; i++)
    {
      ((PieElement)pieElements.get(i)).calculateDimensions(total);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the maximum radius the pie chart can draw within give the graphics context.  The method
                      take into account the current display properties of the chart, such as displaying labels, values
                      and percentages.

  <br><b>Notes</b>:<br>
	                  - This method is useful for classes that implement the PieChartSizer interface to determine maximum
	                    radius of all charts the PieChartSizer class controls

  <br>
  @param g Graphics context the chart would be drawn in
	*********************************************************************************************************************/
  public int getMaxRadius(Graphics g)
  {
    if (font != null)
    {
      g.setFont(font);
    }

    Dimension size = this.getSize();
    int radius = ((size.width < size.height) ? size.width : size.height)/2;

    if (drawLabels)
    {
      int maxRadius = radius;
      int startingAngle = startingOffset;
      for (int i=0, isize=pieElements.size(); i<isize; i++)
      {
        PieElement element = (PieElement)pieElements.get(i);
        int radi = element.getMaxRadius(g, startingAngle, radius, showLabels, showValues, showPercents);
        maxRadius = (maxRadius > radi) ? radi : maxRadius;
        startingAngle += element.angularDistance;
      }

      return((maxRadius < radius-BORDER) ? maxRadius : radius-BORDER);
    }
    else
    {
      return(radius -= BORDER);
    }
  }

  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);

    if (font != null)
    {
      g.setFont(font);
    }

    if (antiAliasing && (g instanceof Graphics2D))
    {
      // Turn anti-aliasing on for non-text rendering
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    Dimension size = this.getSize();
    int radius = ((size.width < size.height) ? size.width : size.height)/2;
    int x = size.width/2;
    int y = size.height/2;

    
    if (sizer != null)
    {
      radius = sizer.getRadius(g);
    }
    else if (drawLabels)
    {
      radius = getMaxRadius(g);
    }
    else
    {
      radius -= BORDER;
    }

    if (shadow)
    {
      g.setColor(Color.gray);
      g.fillOval(x-radius+10, y-radius+10, radius*2, radius*2);
    }
    
    int startingAngle = startingOffset;
    for (int i=0, isize=pieElements.size(); i<isize; i++)
    {
      startingAngle += ((PieElement)pieElements.get(i)).drawSlice(g, startingAngle, x, y, radius, outlineSlices);
    }
    
    if (drawLabels)
    {
      startingAngle = startingOffset;
      for (int i=0, isize=pieElements.size(); i<isize; i++)
      {
        startingAngle += ((PieElement)pieElements.get(i)).drawLabel(g, startingAngle, x, y, radius, showLabels, showValues, showPercents);
      }
    }
  }

  private class PopupMouseListener extends MouseAdapter implements ActionListener
  {
    private JPopupMenu popup = new JPopupMenu();
    public JCheckBoxMenuItem antiAliasingCheck = new JCheckBoxMenuItem("Anti-Aliasing", PieChart.this.antiAliasing);
    public JCheckBoxMenuItem outlineSlicesCheck = new JCheckBoxMenuItem("Outline Slices", PieChart.this.outlineSlices);
    public JCheckBoxMenuItem shadowCheck = new JCheckBoxMenuItem("Show Shadow", PieChart.this.shadow);
    public JCheckBoxMenuItem drawLabelsCheck = new JCheckBoxMenuItem("Draw Labels", PieChart.this.drawLabels);
    public JCheckBoxMenuItem showLabelsCheck = new JCheckBoxMenuItem("Show Labels", PieChart.this.showLabels);
    public JCheckBoxMenuItem showValuesCheck = new JCheckBoxMenuItem("Show Values", PieChart.this.showValues);
    public JCheckBoxMenuItem showPercentsCheck = new JCheckBoxMenuItem("Show Percents", PieChart.this.showPercents);

    public PopupMouseListener()
    {
      popup.add(antiAliasingCheck);
      popup.add(outlineSlicesCheck);
      popup.add(shadowCheck);
      popup.addSeparator();
      popup.add(drawLabelsCheck);
      popup.add(showLabelsCheck);
      popup.add(showValuesCheck);
      popup.add(showPercentsCheck);
      
      antiAliasingCheck.addActionListener(PopupMouseListener.this);
      outlineSlicesCheck.addActionListener(PopupMouseListener.this);
      shadowCheck.addActionListener(PopupMouseListener.this);
      drawLabelsCheck.addActionListener(PopupMouseListener.this);
      showLabelsCheck.addActionListener(PopupMouseListener.this);
      showValuesCheck.addActionListener(PopupMouseListener.this);
      showPercentsCheck.addActionListener(PopupMouseListener.this);
    }
    
    public void actionPerformed(ActionEvent e)
    {
      PieChart.this.antiAliasing = antiAliasingCheck.isSelected();
      PieChart.this.outlineSlices = outlineSlicesCheck.isSelected();
      PieChart.this.shadow = shadowCheck.isSelected();
      PieChart.this.drawLabels = drawLabelsCheck.isSelected();
      PieChart.this.showLabels = showLabelsCheck.isSelected();
      PieChart.this.showValues = showValuesCheck.isSelected();
      PieChart.this.showPercents = showPercentsCheck.isSelected();

      
      PieChart.this.repaint();
    }
    
    public void mouseReleased(MouseEvent e)
    {
      if (e.isPopupTrigger())
      {
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }
}
