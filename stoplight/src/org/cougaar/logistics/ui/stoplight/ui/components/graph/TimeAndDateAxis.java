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

import java.awt.*;
import java.applet.*;
import java.util.*;
import java.lang.*;

public class TimeAndDateAxis extends Axis
{
  private static final String[] monthList = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

  private static final long MS_IN_ONE_MONTH = 1000L*60L*60L*24L*30L;
  private static final long MS_IN_ONE_DAY = 1000L*60L*60L*24L;
  private static final long MS_IN_ONE_HOUR = 1000L*60L*60L;
  private static final long MS_IN_ONE_MINUTE = 1000L*60L;
  private static final long MS_IN_ONE_SECOND = 1000L;

	/*********************************************************************************************************************
  <b>Description</b>: Time scale of the axis.

  <br><br><b>Notes</b>:<br>
										- Default value is 1 millisecond per unit
	*********************************************************************************************************************/
  public long timeScale = 1;

	/*********************************************************************************************************************
  <b>Description</b>: Base time of the axis.

  <br><br><b>Notes</b>:<br>
										- Default value is 0
	*********************************************************************************************************************/
  public long baseTime = 0;

	/*********************************************************************************************************************
  <b>Description</b>: C-Date zero time value of the axis.

  <br><br><b>Notes</b>:<br>
										- Default value is 0
	*********************************************************************************************************************/
  public long cDate = 0;

	/*********************************************************************************************************************
  <b>Description</b>: Indicator of whether or not to use the C-Date display.

  <br><br><b>Notes</b>:<br>
										- Default value is false
	*********************************************************************************************************************/
  public boolean useCDate = false;

  private String[] majorTicLabel = null;

  private String titleText = null;

	/*********************************************************************************************************************
  <b>Description</b>: Default construtor.
	*********************************************************************************************************************/
  public TimeAndDateAxis()
  {
  }

 	/*********************************************************************************************************************
  <b>Description</b>: Sets the title of the axis.

  <br>
  @param s Title text
	*********************************************************************************************************************/
 public void setTitleText(String s)
  {
    titleText = s;
  }

 	/*********************************************************************************************************************
  <b>Description</b>: Gets the width, in pixels, of the axis.

  <br>
  @return Width of the axis in pixels
	*********************************************************************************************************************/
  public int getAxisWidth(Graphics g)
  {
    int i;
    width = 0;

//    if (minimum == maximum)    return 0;
//    if (dataset.size() == 0)   return 0;

    if (useCDate)
    {
      title.setText("C-Date: " + new Date(cDate));
    }
    else
    {
      title.setText(titleText);
    }

//    calculateGridLabels(g);

//    width = label.getRHeight(g) + label.getLeading(g);
    width = label.getHeight(g);

    if(!title.isNull())
    {
//      width += title.getRHeight(g);
      width += title.getHeight(g);
    }

/*    int maxWidth = 0;
    for(i=0; i<majorTicLabel.length; i++)
    {
      if (majorTicLabel[i] != null)
      {
        label.setText(" "+majorTicLabel[i]);
        maxWidth = Math.max(label.getRHeight(g),maxWidth);
      }
    }
    width += maxWidth;*/

    return(width);
  }

  // Show labels based on date (year/month/day)
  public void calculateGridLabels(Graphics g)
  {
    if (useCDate)
    {
      title.setText("C-Date: " + new Date(cDate));
      doCDateGridLabels(g);
    }
    else
    {
      title.setText(titleText);

      long timeDiff = (baseTime + (long)(maximum*timeScale)) - (baseTime + (long)(minimum*timeScale));

      if (timeDiff > MS_IN_ONE_MONTH)
      {
        doDayMonthGridLabels(g);
      }
      else if (timeDiff > MS_IN_ONE_DAY)
      {
        doDayGridLabels(g);
      }
      else if (timeDiff > MS_IN_ONE_HOUR)
      {
        doDateAndHourGridLabels(g);
      }
      else if (timeDiff > MS_IN_ONE_MINUTE)
      {
        doHourAndMinuteGridLabels(g);
      }
      else
      {
        doMinuteAndSecondGridLabels(g);
      }
    }
  }

  // Show labels based on date (month/day)
  public void doDayMonthGridLabels(Graphics g)
  {
    GregorianCalendar cal = null;
    GregorianCalendar nextCal = null;

    if (getInteger(maximum) != 0)
    {
      cal = new GregorianCalendar();
      nextCal = new GregorianCalendar();
      cal.setTime(new Date(baseTime + (long)(minimum*timeScale)));
      if (cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.MILLISECOND) != 0)
      {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Add one day
        cal.add(Calendar.DATE, 1);
      }

      long oneDayValue = MS_IN_ONE_DAY/timeScale;
      long numberOfDaysInRange = (long)((maximum-minimum)/oneDayValue);

      // Pick number of values to show and step size
      int pixelRange = getInteger(maximum) - getInteger(minimum);
      int maxCharWidth = label.charWidth(g, 'W');
      long numberOfLabels = (long)(pixelRange/(maxCharWidth*3));
      numberOfLabels = ((numberOfLabels > numberOfDaysInRange) || (numberOfLabels == 0)) ? numberOfDaysInRange : numberOfLabels;

      double modifier = numberOfDaysInRange/(double)numberOfLabels;

      modifier = (modifier == 0.0) ? 1.0 : RoundUp(modifier);
      label_step = oneDayValue*modifier;

      label_start = (cal.getTime().getTime() - baseTime)/timeScale;

      double val = label_start;
      label_count = 0;
      while (val < maximum ) { val += label_step; label_count++; }
    }
    else
    {
      label_count = 0;
      label_step = 0;
      label_start = 0;
    }

    label_string = new String[label_count];
    majorTicLabel = new String[label_count];
    label_value  = new double[label_count];

    int month = -1;
    double val = 0.0;
    for (int i=0; i<label_count; i++)
    {
      val = label_start + i*label_step;
      cal.setTime(new Date(baseTime + (long)(val*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)((label_start + (i+1)*label_step)*timeScale)));

      if (month != cal.get(Calendar.MONTH))
      {
        if (cal.get(Calendar.MONTH) == nextCal.get(Calendar.MONTH))
        {
          if ((cal.get(Calendar.DAY_OF_MONTH) == 15) || (nextCal.get(Calendar.DAY_OF_MONTH) > 15))
          {
            month = cal.get(Calendar.MONTH);
//            majorTicLabel[i] = monthList[month] + " " + cal.get(Calendar.YEAR);
            majorTicLabel[i] = monthList[month];
          }
          else if (i == label_count-1)
          {
            month = cal.get(Calendar.MONTH);
//            majorTicLabel[i] = monthList[month] + " " + cal.get(Calendar.YEAR);
            majorTicLabel[i] = monthList[month];
          }
        }
        else
        {
          month = cal.get(Calendar.MONTH);
//          majorTicLabel[i] = monthList[month] + " " + cal.get(Calendar.YEAR);
          majorTicLabel[i] = monthList[month];
        }
      }

      label_string[i] = "" + cal.get(Calendar.DAY_OF_MONTH);

      label_value[i] = val;
    }
  }

  // Show labels based on day
  public void doDayGridLabels(Graphics g)
  {
    GregorianCalendar cal = null;
    GregorianCalendar nextCal = null;

    if (getInteger(maximum) != 0)
    {
      cal = new GregorianCalendar();
      nextCal = new GregorianCalendar();
      cal.setTime(new Date(baseTime + (long)(minimum*timeScale)));
      if (cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.SECOND) != 0 || cal.get(Calendar.MILLISECOND) != 0)
      {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Add one day
        cal.add(Calendar.DATE, 1);
      }

      long oneDayValue = MS_IN_ONE_DAY/timeScale;
      long numberOfDaysInRange = (long)((maximum-minimum)/oneDayValue);

      // Pick number of values to show and step size
      int pixelRange = getInteger(maximum) - getInteger(minimum);
      int maxCharWidth = label.charWidth(g, '0');
      long numberOfLabels = (long)(pixelRange/(maxCharWidth*3));
      numberOfLabels = ((numberOfLabels > numberOfDaysInRange) || (numberOfLabels == 0)) ? numberOfDaysInRange : numberOfLabels;

      double modifier = numberOfDaysInRange/(double)numberOfLabels;

      modifier = (modifier == 0.0) ? 1.0 : RoundUp(modifier);
      label_step = oneDayValue*modifier;

      label_start = (cal.getTime().getTime() - baseTime)/timeScale;

      double val = label_start;
      label_count = 0;
      while (val < maximum ) { val += label_step; label_count++; }
    }
    else
    {
      label_count = 0;
      label_step = 0;
      label_start = 0;
    }

    label_string = new String[label_count];
    majorTicLabel = new String[label_count];
    label_value  = new double[label_count];

    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    double val = 0.0;
    for (int i=0; i<label_count; i++)
    {
      val = label_start + i*label_step;
      cal.setTime(new Date(baseTime + (long)(val*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)((label_start + (i+1)*label_step)*timeScale)));

      label_string[i] = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)))
      {
        year = cal.get(Calendar.YEAR);
        day = cal.get(Calendar.DAY_OF_MONTH);
        label_string[i] = Integer.toString(day);

        if (month != cal.get(Calendar.MONTH))
        {
          if (cal.get(Calendar.MONTH) == nextCal.get(Calendar.MONTH))
          {
            if ((cal.get(Calendar.DAY_OF_MONTH) == 15) || (nextCal.get(Calendar.DAY_OF_MONTH) > 15))
            {
              month = cal.get(Calendar.MONTH);
              label_string[i] = month + "/" + label_string[i];
            }
            else if (i == label_count-1)
            {
              month = cal.get(Calendar.MONTH);
              label_string[i] = month + "/" + label_string[i];
            }
          }
          else
          {
            month = cal.get(Calendar.MONTH);
            label_string[i] = month + "/" + label_string[i];
          }
        }
      }

      label_value[i] = val;
    }
  }

  // Show labels based on date and hour
  public void doDateAndHourGridLabels(Graphics g)
  {
    GregorianCalendar cal = null;
    GregorianCalendar nextCal = null;

    if (getInteger(maximum) != 0)
    {
      cal = new GregorianCalendar();
      nextCal = new GregorianCalendar();
      cal.setTime(new Date(baseTime + (long)(minimum*timeScale)));
      if (cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.SECOND) != 0 || cal.get(Calendar.MILLISECOND) != 0)
      {
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Add one hour
        cal.add(Calendar.HOUR, 1);
      }

      long oneHourValue = MS_IN_ONE_HOUR/timeScale;
      long numberOfHoursInRange = (long)((maximum-minimum)/oneHourValue);

      // Pick number of values to show and step size
      int pixelRange = getInteger(maximum) - getInteger(minimum);
      int maxCharWidth = label.charWidth(g, 'W');
      long numberOfLabels = (long)(pixelRange/(maxCharWidth*6));
      numberOfLabels = ((numberOfLabels > numberOfHoursInRange) || (numberOfLabels == 0)) ? numberOfHoursInRange : numberOfLabels;

      double modifier = numberOfHoursInRange/(double)numberOfLabels;

      modifier = (modifier == 0.0) ? 1.0 : RoundUp(modifier);
      label_step = oneHourValue*modifier;

      label_start = (cal.getTime().getTime() - baseTime)/timeScale;

      double val = label_start;
      label_count = 0;
      while (val < maximum ) { val += label_step; label_count++; }
    }
    else
    {
      label_count = 0;
      label_step = 0;
      label_start = 0;
    }

    label_string = new String[label_count];
    majorTicLabel = new String[label_count];
    label_value  = new double[label_count];

    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    int hour = -1;
    double val = 0.0;
    for (int i=0; i<label_count; i++)
    {
      val = label_start + i*label_step;
      cal.setTime(new Date(baseTime + (long)(val*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)((label_start + (i+1)*label_step)*timeScale)));

      label_string[i] = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)) || (hour != cal.get(Calendar.HOUR_OF_DAY)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        label_string[i] = (cal.get(Calendar.HOUR) == 0 ? 12 : cal.get(Calendar.HOUR)) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm");
      }

      if (day != cal.get(Calendar.DAY_OF_MONTH))
      {
        if (cal.get(Calendar.DAY_OF_MONTH) == nextCal.get(Calendar.DAY_OF_MONTH))
        {
          if ((cal.get(Calendar.HOUR_OF_DAY) == 12) || (nextCal.get(Calendar.HOUR_OF_DAY) > 12))
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            majorTicLabel[i] = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
          }
          else if (i == label_count-1)
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            majorTicLabel[i] = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
          }
        }
        else
        {
          day = cal.get(Calendar.DAY_OF_MONTH);
          majorTicLabel[i] = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
        }
      }

      label_value[i] = val;
    }
  }

  // Show labels based on hour and minute
  public void doHourAndMinuteGridLabels(Graphics g)
  {
    GregorianCalendar cal = null;
    GregorianCalendar nextCal = null;

    if (getInteger(maximum) != 0)
    {
      cal = new GregorianCalendar();
      nextCal = new GregorianCalendar();
      cal.setTime(new Date(baseTime + (long)(minimum*timeScale)));
      if (cal.get(Calendar.SECOND) != 0 || cal.get(Calendar.MILLISECOND) != 0)
      {
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Add one minute
        cal.add(Calendar.MINUTE, 1);
      }

      long oneMinuteValue = MS_IN_ONE_MINUTE/timeScale;
      long numberOfMinutesInRange = (long)((maximum-minimum)/oneMinuteValue);

      // Pick number of values to show and step size
      int pixelRange = getInteger(maximum) - getInteger(minimum);
      int maxCharWidth = label.charWidth(g, 'W');
      long numberOfLabels = (long)(pixelRange/(maxCharWidth*6));
      numberOfLabels = ((numberOfLabels > numberOfMinutesInRange) || (numberOfLabels == 0)) ? numberOfMinutesInRange : numberOfLabels;

      double modifier = numberOfMinutesInRange/(double)numberOfLabels;

      modifier = (modifier == 0.0) ? 1.0 : RoundUp(modifier);
      label_step = oneMinuteValue*modifier;

      label_start = (cal.getTime().getTime() - baseTime)/timeScale;

      double val = label_start;
      label_count = 0;
      while (val < maximum ) { val += label_step; label_count++; }
    }
    else
    {
      label_count = 0;
      label_step = 0;
      label_start = 0;
    }

    label_string = new String[label_count];
    majorTicLabel = new String[label_count];
    label_value  = new double[label_count];

    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    int hour = -1;
    int minute = -1;
    double val = 0.0;
    for (int i=0; i<label_count; i++)
    {
      val = label_start + i*label_step;
      cal.setTime(new Date(baseTime + (long)(val*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)((label_start + (i+1)*label_step)*timeScale)));

      label_string[i] = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)) || (hour != cal.get(Calendar.HOUR_OF_DAY)) || (minute != cal.get(Calendar.MINUTE)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        label_string[i] = cal.get(Calendar.HOUR) + ":" + (minute < 10 ? ("0" + minute) : "" + minute) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm"); 
      }

      if (day != cal.get(Calendar.DAY_OF_MONTH))
      {
        if (cal.get(Calendar.DAY_OF_MONTH) == nextCal.get(Calendar.DAY_OF_MONTH))
        {
          if ((cal.get(Calendar.HOUR_OF_DAY) == 12) || (nextCal.get(Calendar.HOUR_OF_DAY) > 12))
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            majorTicLabel[i] = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
          }
          else if (i == label_count-1)
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            majorTicLabel[i] = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
          }
        }
        else
        {
          day = cal.get(Calendar.DAY_OF_MONTH);
          majorTicLabel[i] = (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
        }
      }

      label_value[i] = val;
    }
  }

  // Show labels based on minute and second
  public void doMinuteAndSecondGridLabels(Graphics g)
  {
    GregorianCalendar cal = null;
    GregorianCalendar nextCal = null;

    if (getInteger(maximum) != 0)
    {
      cal = new GregorianCalendar();
      nextCal = new GregorianCalendar();
      cal.setTime(new Date(baseTime + (long)(minimum*timeScale)));
      if (cal.get(Calendar.MILLISECOND) != 0)
      {
        cal.set(Calendar.MILLISECOND, 0);

        // Add one second
        cal.add(Calendar.SECOND, 1);
      }

      long oneSecondValue = MS_IN_ONE_SECOND/timeScale;
      long numberOfSecondsInRange = (long)((maximum-minimum)/oneSecondValue);

      // Pick number of values to show and step size
      int pixelRange = getInteger(maximum) - getInteger(minimum);
      int maxCharWidth = label.charWidth(g, 'W');
      long numberOfLabels = (long)(pixelRange/(maxCharWidth*6));
      numberOfLabels = ((numberOfLabels > numberOfSecondsInRange) || (numberOfLabels == 0)) ? numberOfSecondsInRange : numberOfLabels;

      double modifier = numberOfSecondsInRange/(double)numberOfLabels;

      modifier = (modifier == 0.0) ? 1.0 : RoundUp(modifier);
      label_step = oneSecondValue*modifier;

      label_start = (cal.getTime().getTime() - baseTime)/timeScale;

      double val = label_start;
      label_count = 0;
      while (val < maximum ) { val += label_step; label_count++; }
    }
    else
    {
      label_count = 0;
      label_step = 0;
      label_start = 0;
    }

    label_string = new String[label_count];
    majorTicLabel = new String[label_count];
    label_value  = new double[label_count];

    int year = Integer.MAX_VALUE;
    int month = -1;
    int day = -1;
    int hour = -1;
    int minute = -1;
    int second = -1;
    double val = 0.0;
    for (int i=0; i<label_count; i++)
    {
      val = label_start + i*label_step;
      cal.setTime(new Date(baseTime + (long)(val*timeScale)));
      nextCal.setTime(new Date(baseTime + (long)((label_start + (i+1)*label_step)*timeScale)));

      label_string[i] = "";
      if ((year != cal.get(Calendar.YEAR)) || (month != cal.get(Calendar.MONTH)) || (day != cal.get(Calendar.DAY_OF_MONTH)) || (hour != cal.get(Calendar.HOUR_OF_DAY)) || (minute != cal.get(Calendar.MINUTE)) || (second != cal.get(Calendar.SECOND)))
      {
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        second = cal.get(Calendar.SECOND);
        label_string[i] = minute + ":" + (second < 10 ? ("0" + second) : "" + second);
      }

      if (day != cal.get(Calendar.DAY_OF_MONTH))
      {
        if (cal.get(Calendar.DAY_OF_MONTH) == nextCal.get(Calendar.DAY_OF_MONTH))
        {
          if ((cal.get(Calendar.HOUR_OF_DAY) == 12) || (nextCal.get(Calendar.HOUR_OF_DAY) > 12))
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
//            majorTicLabel[i] = cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm") + " " + (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH);
            majorTicLabel[i] = + (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm");
          }
          else if (i == label_count-1)
          {
            day = cal.get(Calendar.DAY_OF_MONTH);
            majorTicLabel[i] = + (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm");
          }
        }
        else
        {
          day = cal.get(Calendar.DAY_OF_MONTH);
          majorTicLabel[i] = + (cal.get(Calendar.MONTH) +1) +"/" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR) + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm");
        }
      }

      label_value[i] = val;
    }
  }

  // Show labels based on date (month/day)
  public void doCDateGridLabels(Graphics g)
  {
    GregorianCalendar cal = null;
    GregorianCalendar nextCal = null;

    if ((getInteger(maximum) != 0) || (getInteger(maximum) != 0))
    {
      cal = new GregorianCalendar();
      nextCal = new GregorianCalendar();
      cal.setTime(new Date(baseTime + (long)(minimum*timeScale)));
      if (cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.MILLISECOND) != 0)
      {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Add one day
        cal.add(Calendar.DATE, 1);
      }

      long oneDayValue = MS_IN_ONE_DAY/timeScale;
      long numberOfDaysInRange = (long)((maximum-minimum)/oneDayValue);

      // Pick number of values to show and step size
      int pixelRange = getInteger(maximum) - getInteger(minimum);
      int maxCharWidth = label.charWidth(g, 'W');
      long numberOfLabels = (long)(pixelRange/(maxCharWidth*3));
      numberOfLabels = ((numberOfLabels > numberOfDaysInRange) || (numberOfLabels == 0)) ? numberOfDaysInRange : numberOfLabels;

      double modifier = numberOfDaysInRange/(double)numberOfLabels;

      modifier = (modifier == 0.0) ? 1.0 : RoundUp(modifier);
      label_step = oneDayValue*modifier;

      label_start = (cal.getTime().getTime() - baseTime)/timeScale;

      double val = label_start;
      label_count = 1;
      while (val < maximum ) { val += label_step; label_count++; }
    }
    else
    {
      label_count = 0;
      label_step = 0;
      label_start = 0;
    }

    label_string = new String[label_count];
    majorTicLabel = new String[label_count];
    label_value  = new double[label_count];

    Date date = new Date();
    int month = -1;
    double val = 0.0;
    long time = 0;
    date.setTime(cDate);
    cal.setTime(date);
    long currentCDate = truncateTimeToDays(cal);
    for (int i=0; i<label_count; i++)
    {
      val = label_start + i*label_step;
      date.setTime(baseTime + (long)val*timeScale);
      cal.setTime(date);
      time = truncateTimeToDays(cal);

      date.setTime(time);
      label_string[i] = "" + (long)((time - currentCDate)/MS_IN_ONE_DAY);
      label_value[i] = val;
    }
  }

  private long truncateTimeToDays(GregorianCalendar cal)
  {
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.HOUR, 0);

    return(cal.getTime().getTime());
  }

     protected void drawHAxis(Graphics g)
     {
          Graphics lg;
          int i;
          int j;
          int x0,y0,x1,y1;
          int direction;
          int offset;
          double minor_step;

          Color c;

//          double vmin = minimum*1.001;
//          double vmax = maximum*1.001;
          double vmin = minimum;
          double vmax = maximum;

          double scale  = (amax.x - amin.x)/(maximum - minimum);
          double val;
          double minor;

//          System.out.println("Drawing Horizontal Axis!");


          if( axiscolor != null) g.setColor(axiscolor);

          g.drawLine(amin.x,amin.y,amax.x,amax.y);

          if(position == TOP )     direction =  1;
          else                     direction = -1;

          minor_step = label_step/(minor_tic_count+1);
          val = label_start;
          for(i=0; i<label_count; i++)
          {
              if( val >= vmin && val <= vmax )
              {
                 y0 = amin.y;
                 x0 = amin.x + (int)( ( val - minimum ) * scale);
                 if( Math.abs(label_value[i]) <= 0.0001 && drawzero )
                 {
                      c = g.getColor();
                      if(zerocolor != null) g.setColor(zerocolor);
                      g.drawLine(x0,y0,x0,y0+data_window.height*direction);
                      g.setColor(c);

                 }
                 else if( drawgrid )
                 {
                      c = g.getColor();
                      if(gridcolor != null) g.setColor(gridcolor);
                      g.drawLine(x0,y0,x0,y0+data_window.height*direction);
                      g.setColor(c);
                 }
                 x1 = x0;
                 y1 = y0 + major_tic_size*direction;
                 g.drawLine(x0,y0,x1,y1);
              }

              minor = val + minor_step;
              for(j=0; j<minor_tic_count; j++)
              {
                 if( minor >= vmin && minor <= vmax )
                 {
                    y0 = amin.y;
                    x0 = amin.x + (int)( ( minor - minimum ) * scale);
                    if( drawgrid )
                    {
                      c = g.getColor();
                      if(gridcolor != null) g.setColor(gridcolor);
                      g.drawLine(x0,y0,x0,y0+data_window.height*direction);
                      g.setColor(c);
                    }
                    x1 = x0;
                    y1 = y0 + minor_tic_size*direction;
                    g.drawLine(x0,y0,x1,y1);
                 }
                minor += minor_step;
              }

              val += label_step;
          }


          if (dividers != null)
          {
            for (i=0; i<dividers.length; i++)
            {
              val = ((Double)dividers[i][0]).doubleValue();
              if( val >= vmin && val <= vmax )
              {
                y0 = amin.y;
                x0 = amin.x + (int)( ( val - minimum ) * scale);
                c = g.getColor();
                if(dividers[i][1] != null) g.setColor((Color)dividers[i][1]);
                if (g instanceof Graphics2D)
                {
                  Graphics2D graphics2D = (Graphics2D)g;
                  Stroke stroke = graphics2D.getStroke();
                  BasicStroke newStroke = new BasicStroke(dividerThickness);
                  graphics2D.setStroke(newStroke);
                  g.drawLine(x0,y0,x0,y0+data_window.height*direction);
                  graphics2D.setStroke(stroke);
                }
                else
                {
                  g.drawLine(x0,y0,x0,y0+data_window.height*direction);
                }
                g.setColor(c);
              }
            }
          }


          if(position == TOP ) {
             offset = - label.getLeading(g) - label.getDescent(g);
          } else {
             offset = + label.getLeading(g) + label.getAscent(g);
          }


          val = label_start;
          for(i=0; i<label_count; i++)
          {
              if( val >= vmin && val <= vmax )
              {
                 y0 = amin.y + offset;
                 x0 = amin.x + (int)(( val - minimum ) * scale);
                 label.setText(label_string[i]);
                 label.draw(g,x0,y0,TextLine.CENTER);

                  if (majorTicLabel[i] != null)
                  {
                   if(position == TOP)
                   {
                      y0 = amin.y + offset*2;
                   }
                   else
                   {
                      y0 = amax.y + offset*2;
                   }
                   x0 = amin.x + (int)((val - minimum)*scale);
                   label.setText(majorTicLabel[i]);
                   label.draw(g,x0,y0,TextLine.CENTER);
                  }
              }
              val += label_step;

          }

          if(!title.isNull())
          {
             if(position == TOP)
             {
                y0 = amin.y - (label.getLeading(g) - label.getDescent(g))*2 - title.getLeading(g) - title.getDescent(g);
             }
             else
             {
                y0 = amax.y + (label.getLeading(g) + label.getAscent(g))*2 + title.getLeading(g) + title.getAscent(g);
             }
              x0 = amin.x + ( amax.x - amin.x)/2;
              title.draw(g,x0,y0,TextLine.CENTER);
          }
     }

 	/*********************************************************************************************************************
  <b>Description</b>: Converts the given value into a string based on C-Date or normal time and scale values.

  <br>
  @param val Value of point
  @return String date representation of point value
	*********************************************************************************************************************/
  public String getPointAsString(double val)
  {
    String string = null;
    Date date = new Date(baseTime + (long)val*timeScale);
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(date);

    if (useCDate)
    {
      long time = truncateTimeToDays(cal);
      date.setTime(cDate);
      cal.setTime(date);
      long currentCDate = truncateTimeToDays(cal);

      string = "" + (long)((time - currentCDate)/MS_IN_ONE_DAY);
    }
    else
    {
      long timeDiff = (baseTime + (long)(maximum*timeScale)) - (baseTime + (long)(minimum*timeScale));
      int year = cal.get(Calendar.YEAR);
      int month = cal.get(Calendar.MONTH);
      int day = cal.get(Calendar.DAY_OF_MONTH);
      int hour = cal.get(Calendar.HOUR);
      int minute = cal.get(Calendar.MINUTE);
      int second = cal.get(Calendar.SECOND);
      int ampm = cal.get(Calendar.AM_PM);

      if (timeDiff > MS_IN_ONE_MONTH)
      {
        string = monthList[month] + " " + day + ", " + year;
      }
      else if (timeDiff > MS_IN_ONE_DAY)
      {
        string = (month +1) + "/" + day + "/" + year;
      }
      else if (timeDiff > MS_IN_ONE_HOUR)
      {
        string = (month +1) + "/" + day + "/" + year + " " + (hour == 0 ? 12 : hour) + (ampm == 0 ? "am" : "pm");
      }
      else if (timeDiff > MS_IN_ONE_MINUTE)
      {
        string = (month +1) + "/" + day + "/" + year + " " + hour + ":" + (minute < 10 ? ("0" + minute) : "" + minute) + (ampm == 0 ? "am" : "pm");
      }
      else
      {
        string = (month +1) + "/" + day + "/" + year + " " + hour + ":" + (minute < 10 ? ("0" + minute) : "" + minute) + ":" + (second < 10 ? ("0" + second) : "" + second) + (ampm == 0 ? "am" : "pm");
      }
    }

    return(string);
  }
}
