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
import java.awt.image.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.event.*;

import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.COrderedLabeledMThumbSlider;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;

/***********************************************************************************************************************
<b>Description</b>: A version of the two thumbed slider range control that operates/displays based on time.  This
                    this control contains a right click properties menu

***********************************************************************************************************************/
public class CMThumbSliderDateAndTimeRangeControl extends CMThumbSliderRangeControl
{
  private static final String[] monthList = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

  private static final long MS_IN_ONE_MONTH = 1000L*60L*60L*24L*30L;
  private static final long MS_IN_ONE_DAY = 1000L*60L*60L*24L;
  private static final long MS_IN_ONE_HOUR = 1000L*60L*60L;
  private static final long MS_IN_ONE_MINUTE = 1000L*60L;

  private static int FIDELITY = 1000;
  private static final int MAJOR_TICK_SPACING = FIDELITY/10;

  // Default time scale of ms
  private long timeScale = 1;
  private long baseTime = 0;

  private long cDate = 0;
  private boolean useCDate = false;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a range control with min and max times of 0 milliseconds since 1970.
	*********************************************************************************************************************/
  public CMThumbSliderDateAndTimeRangeControl()
  {
    super(0, 0);
    
    slider.addMouseListener(popupMouseListener);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a range control with the specified min and max times.

  <br>
  @param minValue Minimum thumb value limit
  @param maxValue Maximum thumb value limit
	*********************************************************************************************************************/
  public CMThumbSliderDateAndTimeRangeControl(float minValue, float maxValue)
  {
    super(minValue, maxValue);

    slider.addMouseListener(popupMouseListener);

    addComponentListener(new ResizeListener());
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a range control with the specified min and max times.

  <br>
  @param minValue Minimum thumb value limit
  @param maxValue Maximum thumb value limit
	*********************************************************************************************************************/
  public CMThumbSliderDateAndTimeRangeControl(double minValue, double maxValue)
  {
    super(minValue, maxValue);

    slider.addMouseListener(popupMouseListener);

    addComponentListener(new ResizeListener());
  }

  private class ResizeListener extends ComponentAdapter
  {
    public void componentResized(ComponentEvent e)
    {
      setSliderRange(minDValue, maxDValue);
    }
  }

  private boolean showLeftThumbLabel = false;
  private boolean showRightThumbLabel = false;

  private MouseListener popupMouseListener = new PopupMouseListener();
  private class PopupMouseListener extends MouseAdapter implements ActionListener
  {
    private JPopupMenu popup = new JPopupMenu();
    public JCheckBoxMenuItem leftThumb = new JCheckBoxMenuItem("Show Left Thumb Dynamic Label", false);
    public JCheckBoxMenuItem rightThumb = new JCheckBoxMenuItem("Show Right Thumb Dynamic Label", false);

    public PopupMouseListener()
    {
      popup.add(leftThumb);
      popup.add(rightThumb);
      
      leftThumb.addActionListener(PopupMouseListener.this);
      rightThumb.addActionListener(PopupMouseListener.this);
    }
    
    public void actionPerformed(ActionEvent e)
    {
      showLeftThumbLabel = leftThumb.isSelected();
      showRightThumbLabel = rightThumb.isSelected();

      CMThumbSliderDateAndTimeRangeControl.this.adjustValueLabelHeight();
      CMThumbSliderDateAndTimeRangeControl.this.revalidate();
    }
    
    public void mouseReleased(MouseEvent e)
    {
      if (e.isPopupTrigger())
      {
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  };


	/*********************************************************************************************************************
  <b>Description</b>: Sets the time scale of the range values in milliseconds to units.  A value of 1000 means there are
                      1000 milliseconds in one unit or 1 second in one unit.

  <br>
  @param scale Time scale to use
	*********************************************************************************************************************/
  public void setTimeScale(long scale)
  {
    timeScale = scale;
    setSliderRange(minDValue, maxDValue);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets the current time scale of the slider.

  <br>
  @return Current time scale of the slider
	*********************************************************************************************************************/
  public long getTimeScale()
  {
    return(timeScale);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the base time.  This value will be added to the slider limit and range values.

  <br>
  @param time Base time to use
	*********************************************************************************************************************/
  public void setBaseTime(long time)
  {
    baseTime = time;
    setSliderRange(minDValue, maxDValue);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Gets the current base time of the slider.

  <br>
  @return Current base time of the slider
	*********************************************************************************************************************/
  public long getBaseTime()
  {
    return(baseTime);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Enables the use of C-Date time display or normal time display.

  <br>
  @param use True if C-Date time display is to be used, false otherwise
	*********************************************************************************************************************/
  public void setUseCDate(boolean use)
  {
    useCDate = use;
    setSliderRange(minDValue, maxDValue);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the C-Date display is enabled.

  <br>
  @return True if C-Dates are being displayed, false otherwise
	*********************************************************************************************************************/
  public boolean getUseCDate()
  {
    return(useCDate);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the time zero C-Date.

  <br>
  @param time Time zero C-Date to use
	*********************************************************************************************************************/
  public void setCDate(long time)
  {
    cDate = time;
    if (useCDate)
    {
      setSliderRange(minDValue, maxDValue);
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the current time zero C-Date.

  <br>
  @return Current C-Date
	*********************************************************************************************************************/
  public long getCDate()
  {
    return(cDate);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Sets the minimum and maximum limits of the thumb ranges.

  <br>
  @param minValue Minimum thumb value limit
  @param maxValue Maximum thumb value limit
	*********************************************************************************************************************/
  public void setSliderRange(double minValue, double maxValue)
  {
    // Try to maintain the same values if possible
/*    Vector currentValues = new Vector();
    for (int i = 0; i < numThumbs; i++)
    {
        currentValues.add(new Float(fromSlider(slider.getValueAt(i))));
    }*/
    RangeModel oldRange = getRange();

    this.minDValue = minValue;
    this.maxDValue = maxValue;
    this.minValue = (float)minValue;
    this.maxValue = (float)maxValue;
    unit = (float)((maxValue - minValue) / FIDELITY);
    unitD = (maxDValue - minDValue) / FIDELITY;

/*    if (Math.abs(maxDValue) > 10)
    {
        labelFormat = new DecimalFormat("####");
    }
    else
    {
        labelFormat = new DecimalFormat("##.##");
    }*/

    int sliderMin = toSlider(minDValue);
    int sliderMax = toSlider(maxDValue);
    for (int i = 0; i < numThumbs; i++)
    {
        BoundedRangeModel model = slider.getModelAt(i);
        model.setMaximum(sliderMax);
        model.setMinimum(sliderMin);
    }

    Hashtable valueLabels = new Hashtable(1);
    if (minDValue == maxDValue)
    {
      valueLabels.put(new Integer(0), new JLabel());
    }
    else if (useCDate)
    {
      setCDateLabels(valueLabels);
    }
    else
    {
      long timeDiff = (baseTime + (long)(maxDValue*timeScale)) - (baseTime + (long)(minDValue*timeScale));

      if (timeDiff > MS_IN_ONE_MONTH)
      {
        setMonthAndDayLabels(valueLabels);
      }
      else if (timeDiff > MS_IN_ONE_DAY)
      {
        setDayLabels(valueLabels);
      }
      else if (timeDiff > MS_IN_ONE_HOUR)
      {
        setDateAndHourLabels(valueLabels);
      }
      else if (timeDiff > MS_IN_ONE_MINUTE)
      {
        setHourAndMinuteLabels(valueLabels);
      }
      else
      {
        setMinuteAndSecondLabels(valueLabels);
      }
    }

    slider.setLabelTable(valueLabels);

    // Set sliders to old current values
    /*for (int i = 0; i < currentValues.size(); i++)
    {
      Number currentValue = (Number)currentValues.elementAt(i);
      slider.setValueAt(toSlider(currentValue.floatValue()), i);
    }*/

    setRange(oldRange);

    SwingUtilities.updateComponentTreeUI(this);
  }

  protected Component dynamicDateStrut = null;

  protected void adjustValueLabelHeight()
  {
    if (showLeftThumbLabel || showRightThumbLabel)
    {
      if (dynamicDateStrut != null)
      {
        dynamicDateStrut.removeMouseListener(popupMouseListener);
        remove(dynamicDateStrut);
      }

      // Create space for value labels in north quad. of component
      int fontHeight = getFontMetrics(MetalLookAndFeel.getSystemTextFont()).getHeight();
      dynamicDateStrut = Box.createVerticalStrut(fontHeight);
      dynamicDateStrut.addMouseListener(popupMouseListener);
      add(dynamicDateStrut, BorderLayout.NORTH);
    }
    else if (dynamicDateStrut != null)
    {
      dynamicDateStrut.removeMouseListener(popupMouseListener);
      remove(dynamicDateStrut);
    }
  }

  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (showLeftThumbLabel || showRightThumbLabel)
    {
      g.setFont(MetalLookAndFeel.getSystemTextFont());
      FontMetrics fm = getFontMetrics(g.getFont());

      SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss  MM/dd/yyyy");

      // Messy!!!

      int i = 0;
      
      while ((i<numThumbs) && slider.getThumbRendererAt(i) == invisibleIcon)
      {
        i++;
      }
      if (showLeftThumbLabel)
      {
        String label = dateFormat.format(new Date((baseTime + (long)(fromDSlider(slider.getValueAt(i))*timeScale))));
        int labelWidth = fm.stringWidth(label);
        int borderOffset = (getBorder() == null) ? 0 : getBorder().getBorderInsets(this).left;
        int thumbXLoc = slider.getThumbXLoc(i) + borderOffset;
        g.drawString(label, thumbXLoc - (labelWidth / 2), (int)slider.getLocation().getY()-5);
      }

      i++;
      while ((i<numThumbs) && slider.getThumbRendererAt(i) == invisibleIcon)
      {
        i++;
      }
      if (showRightThumbLabel)
      {
        String label = dateFormat.format(new Date((baseTime + (long)(fromDSlider(slider.getValueAt(i))*timeScale))));
        int labelWidth = fm.stringWidth(label);
        int borderOffset = (getBorder() == null) ? 0 : getBorder().getBorderInsets(this).left;
        int thumbXLoc = slider.getThumbXLoc(i) + borderOffset;
        g.drawString(label, thumbXLoc - (labelWidth / 2), (int)slider.getLocation().getY()-5);
      }

/*
      // paint dynamic value labels on component
      for (int i = 0; i < numThumbs; i++)
      {
        if (slider.getThumbRendererAt(i) != invisibleIcon)
        {
          String label = dateFormat.format(new Date((baseTime + (long)(fromSlider(slider.getValueAt(i))*timeScale))));
          int labelWidth = fm.stringWidth(label);
          int borderOffset = (getBorder() == null) ? 0 : getBorder().getBorderInsets(this).left;
          int thumbXLoc = slider.getThumbXLoc(i) + borderOffset;
          g.drawString(label, thumbXLoc - (labelWidth / 2), (int)slider.getLocation().getY()-5);
        }
      }*/
    }

    // Swing bug workaround
    if (updateUIAfterPaint)
    {
      SwingUtilities.updateComponentTreeUI(this);
      updateUIAfterPaint = false;
    }
  }

  private static BufferedImage gHelper = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

  private int calcFontPointSize(String worstCase)
  {
    int sliderPixelWidth = slider.getWidth();
    int fontPointSize = 12;
    
    Graphics2D g2D = gHelper.createGraphics();
    while (true)
    {
      FontMetrics fm = g2D.getFontMetrics(new Font("Times-Roman", Font.BOLD, fontPointSize));
      int width = fm.stringWidth(worstCase);
      if ((width * FIDELITY/MAJOR_TICK_SPACING) < (sliderPixelWidth))
      {
        break;
      }

      fontPointSize--;
    }
    
    return(fontPointSize);
  }

  private void setMonthAndDayLabels(Hashtable valueLabels)
  {
    int fontPointSize = calcFontPointSize("W 00");

    GregorianCalendar cal = new GregorianCalendar();
    GregorianCalendar nextCal = new GregorianCalendar();
    int year = Integer.MIN_VALUE;
    int month = -1;
    int day = -1;
    String label = null;
    for (int i=0; i<=FIDELITY; i+=MAJOR_TICK_SPACING)
    {
      cal.setTime(new Date(baseTime + (long)(fromDSlider(i)*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)(fromDSlider(i + MAJOR_TICK_SPACING)*timeScale)));

      label = "";

      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)))
      {
        year = cal.get(Calendar.YEAR);
        day = cal.get(Calendar.DAY_OF_MONTH);

        label = Integer.toString(day);
      }

      if (month != cal.get(Calendar.MONTH))
      {
        if (cal.get(Calendar.MONTH) == nextCal.get(Calendar.MONTH))
        {
          if ((cal.get(Calendar.DAY_OF_MONTH) == 15) || (nextCal.get(Calendar.DAY_OF_MONTH) > 15))
          {
            month = cal.get(Calendar.MONTH);
            label = label + " " + monthList[month];
          }
          else if (i == FIDELITY)
          {
            month = cal.get(Calendar.MONTH);
            label = label + " " + monthList[month];
          }
        }
        else
        {
          month = cal.get(Calendar.MONTH);
          label = label + " " + monthList[month];
        }
      }

      JLabel jLabel = new JLabel(label);
      jLabel.setFont(new Font("Times-Roman", Font.BOLD, fontPointSize));
      valueLabels.put(new Integer(i), jLabel);
    }
  }

  private void setCDateLabels(Hashtable valueLabels)
  {
    int fontPointSize = calcFontPointSize("0000");

    long lastDay = Long.MIN_VALUE;

    String label = null;
    for (int i=0; i<=FIDELITY; i+=MAJOR_TICK_SPACING)
    {
      label = "";
      long day = ((baseTime + (long)(fromDSlider(i)*timeScale))- cDate)/MS_IN_ONE_DAY;
      if (lastDay != day)
      {
        lastDay = day;
        label = Long.toString(day);
      }

      JLabel jLabel = new JLabel(label);
      jLabel.setFont(new Font("Times-Roman", Font.BOLD, fontPointSize));
      valueLabels.put(new Integer(i), jLabel);
    }
  }

  private void setDayLabels(Hashtable valueLabels)
  {
    int fontPointSize = calcFontPointSize("00");

    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    String label = null;
    GregorianCalendar cal = new GregorianCalendar();
    for (int i=0; i<=FIDELITY; i+=MAJOR_TICK_SPACING)
    {
      cal.setTime(new Date(baseTime + (long)(fromDSlider(i)*timeScale)));

      label = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        label = Integer.toString(day);
      }

      JLabel jLabel = new JLabel(label);
      jLabel.setFont(new Font("Times-Roman", Font.BOLD, fontPointSize));
      valueLabels.put(new Integer(i), jLabel);
    }
  }

  private void setDateAndHourLabels(Hashtable valueLabels)
  {
    int fontPointSize = calcFontPointSize("0 00pm");

    GregorianCalendar cal = new GregorianCalendar();
    GregorianCalendar nextCal = new GregorianCalendar();
    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    int hour = -1;
    String label = null;
    for (int i=0; i<=FIDELITY; i+=MAJOR_TICK_SPACING)
    {
      cal.setTime(new Date(baseTime + (long)(fromDSlider(i)*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)(fromDSlider(i + MAJOR_TICK_SPACING)*timeScale)));
//      label = Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + ":" + Integer.toString(cal.get(Calendar.MINUTE));
//      label = (cal.get(Calendar.HOUR) == 0 ? 12 : cal.get(Calendar.HOUR)) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm");


      label = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)) || (hour != cal.get(Calendar.HOUR_OF_DAY)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        label = (cal.get(Calendar.HOUR) == 0 ? 12 : cal.get(Calendar.HOUR)) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm");
      }

      if (day != cal.get(Calendar.DAY_OF_MONTH))
      {
        if (cal.get(Calendar.DAY_OF_MONTH) == nextCal.get(Calendar.DAY_OF_MONTH))
        {
          if ((cal.get(Calendar.HOUR_OF_DAY) == 12) || (nextCal.get(Calendar.HOUR_OF_DAY) > 12))
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            label = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH) + " " + label;
          }
          else if (i == FIDELITY)
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            label = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH) + " " + label;
          }
        }
        else
        {
          day = cal.get(Calendar.DAY_OF_MONTH);
          label = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH) + " " + label;
        }
      }

      JLabel jLabel = new JLabel(label);
      jLabel.setFont(new Font("Times-Roman", Font.BOLD, fontPointSize));
      valueLabels.put(new Integer(i), jLabel);
    }
  }

  private void setHourAndMinuteLabels(Hashtable valueLabels)
  {
    int fontPointSize = calcFontPointSize("00:00pm");
    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    int hour = -1;
    int minute = -1;
    String label = null;
    GregorianCalendar cal = new GregorianCalendar();
    for (int i=0; i<=FIDELITY; i+=MAJOR_TICK_SPACING)
    {
      cal.setTime(new Date(baseTime + (long)(fromDSlider(i)*timeScale)));

      label = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)) || (hour != cal.get(Calendar.HOUR_OF_DAY)) || (minute != cal.get(Calendar.MINUTE)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        label = cal.get(Calendar.HOUR) + ":" + (minute < 10 ? ("0" + minute) : "" + minute) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm"); 
      }

      JLabel jLabel = new JLabel(label);
      jLabel.setFont(new Font("Times-Roman", Font.BOLD, fontPointSize));
      valueLabels.put(new Integer(i), jLabel);
    }
  }

  private void setMinuteAndSecondLabels(Hashtable valueLabels)
  {
    int fontPointSize = calcFontPointSize("00:00");
    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    int hour = -1;
    int minute = -1;
    int second = -1;
    String label = null;
    GregorianCalendar cal = new GregorianCalendar();
    for (int i=0; i<=FIDELITY; i+=MAJOR_TICK_SPACING)
    {
      cal.setTime(new Date(baseTime + (long)(fromDSlider(i)*timeScale)));

      label = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)) || (hour != cal.get(Calendar.HOUR_OF_DAY)) || (minute != cal.get(Calendar.MINUTE)) || (second != cal.get(Calendar.SECOND)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        second = cal.get(Calendar.SECOND);
        label = minute + ":" + (second < 10 ? ("0" + second) : "" + second);
      }

      JLabel jLabel = new JLabel(label);
      jLabel.setFont(new Font("Times-Roman", Font.BOLD, fontPointSize));
      valueLabels.put(new Integer(i), jLabel);
    }
  }
}
