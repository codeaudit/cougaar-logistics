/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;

import java.math.BigDecimal;

/** 
 * ShortfallSummary is an object that summarizizes which inventories of 
 * a particular inventory have shortfall.
 */

public class ShortfallPeriod implements java.io.Serializable, Publishable {

  
  private long startTime;
  private long endTime;
  private double totalShortfallDemand;
  private double totalFilled;

  /**
   * Constructor 
   */


  public ShortfallPeriod () {
  }
  
  public ShortfallPeriod (long aStartTime,
			  long anEndTime) {
      this(aStartTime,anEndTime,0.0d,0.0d);
  }

  public ShortfallPeriod (long aStartTime,
			  long anEndTime,
			  double demand,
			  double filled) {
      startTime = aStartTime;
      endTime = anEndTime;
      totalShortfallDemand = demand;
      totalFilled = filled;

  }


  public long getStartTime() { return startTime; }
  public long getEndTime() { return endTime; }
  public double getTotalDemand() { return totalShortfallDemand; }
  public double getTotalFilled() { return totalFilled; }

  public double getRoundedTotalDemand(boolean roundToInt) { return roundAppropriately(totalShortfallDemand,roundToInt); }
  public double getRoundedTotalFilled(boolean roundToInt) { return roundAppropriately(totalFilled,roundToInt); }

  public int getNumBuckets(long msecPerBucket) { return ((int) (((endTime + 1) - startTime)/msecPerBucket)); }
  public double getShortfallQty(boolean roundToInt) { return roundAppropriately(totalShortfallDemand-totalFilled,roundToInt); }
  public double getPercentShortfall() { return roundToInt((getShortfallQty(false)/getTotalDemand()) * 100); }

  public void setTotalDemand(double demand) 
    { totalShortfallDemand = demand; }

  public void setTotalFilled(double filled) 
    { totalFilled = filled; }

  public String toString() {
    StringBuffer sb = new StringBuffer("");
    sb.append("\nStartTime=" + TimeUtils.dateString(getStartTime()));
    sb.append(" - EndTime=" + TimeUtils.dateString(getEndTime()));
    sb.append("\nShortfallQty=" + getShortfallQty(false));
    sb.append(",PercentShortfall=" + getPercentShortfall());
    return sb.toString();
  }

  public boolean equals(ShortfallPeriod sp) {
      return ((getStartTime() == sp.getStartTime()) &&
              (getEndTime() == sp.getEndTime()) &&
	      (getTotalDemand() == sp.getTotalDemand()) &&
	      (getTotalFilled() == sp.getTotalFilled()));
  }

  public static double roundAppropriately(double aNum, boolean roundToInt) {
      if(roundToInt) {
	  return roundToInt(aNum);
      }
      else {
	  return roundToHundreths(aNum);
      }

  }


  public static double roundToHundreths(double aNum) {
      BigDecimal roundedQty = ((new BigDecimal((double)aNum)).setScale(2,BigDecimal.ROUND_HALF_EVEN));
      return roundedQty.doubleValue();
  }

  public static double roundToInt(double aNum) {
      BigDecimal roundedQty = ((new BigDecimal((double)aNum)).setScale(0,BigDecimal.ROUND_HALF_EVEN));
      return roundedQty.doubleValue();
  }

  public boolean isPersistable() {
    return true;
  }
}
