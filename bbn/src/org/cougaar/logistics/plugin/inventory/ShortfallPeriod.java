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


    public ShortfallPeriod() {
    }

    public ShortfallPeriod(long aStartTime,
                           long anEndTime) {
        this(aStartTime, anEndTime, 0.0d, 0.0d);
    }

    public ShortfallPeriod(long aStartTime,
                           long anEndTime,
                           double demand,
                           double filled) {
        startTime = aStartTime;
        endTime = anEndTime;
        totalShortfallDemand = demand;
        totalFilled = filled;

    }

    /**
     * The start time for the shortfall period
     *
     * @return long milliseconds since epoch time represnting date/time of the start of thr shortfall period
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * The end time for the shortfall period
     *
     * @return long milliseconds since epoch time representing date/time of the end of the shortfall period
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * The number gleaned from the InventoryBG of the total amount of demand during the time period of this ShortfallPeriod
     *
     * @return double The total amount of demand being asked of this inventory during this time period
     */
    public double getTotalDemand() {
        return totalShortfallDemand;
    }

    /**
     * The total amount allocated as the filled amount of the asked for demand during this period.  Should be less than
     * the amount of demand, but could be equal due to a late delivery in an allocation result attached to one of the demand
     * tasks within the shortfall period.  Or if quiescence has not taken place yet and there is not allocation results on the
     * demand.
     *
     * @return double The total amount of demand filled at this inventory over the course of the shortfall period.
     */
    public double getTotalFilled() {
        return totalFilled;
    }

    /**
     * Get total demand rounded to integer or to the hundreths place
     *
     * @param roundToInt true if you want to round to an integer
     * @return total demand rounded to the appropriate precision.
     */
    public double getRoundedTotalDemand(boolean roundToInt) {
        return roundAppropriately(totalShortfallDemand, roundToInt);
    }

    /**
     * Get total demand rounded to an integer or to the hundreths place depending on the passed boolean
     *
     * @param roundToInt true if you want the filled demand rounded to an integer false if only to the hundreths place
     * @return the double represent total filled demand.
     */
    public double getRoundedTotalFilled(boolean roundToInt) {
        return roundAppropriately(totalFilled, roundToInt);
    }

    /**
     * Given the Milliseconds per bucket compute the total number of inventory buckets spanned by this ShortfallPeriod.
     * A Bucket is the granularity at which we calculate the inventory numbers.  If the bucket is an hour long then we
     * add things up that fall within the same hour.   If a day long demand coming in just after midnight and demand coming
     * in before midnight of the next day is counted in the same bucket.
     *
     * @param msecPerBucket The number of milliseconds per inventory bucket
     * @return int The number of buckets spanned.
     */
    public int getNumBuckets(long msecPerBucket) {
        return ((int) (((endTime + 1) - startTime) / msecPerBucket));
    }

    /**
     * The shortfall qty is the total demand over this period minus the total filled by this period.  Most times greater than zero
     *
     * @param roundToInt booean true if round to an integer otherwise round to hundreths
     * @return double representing the integer or double rounded to the hundreths place
     */
    public double getShortfallQty(boolean roundToInt) {
        return roundAppropriately(totalShortfallDemand - totalFilled, roundToInt);
    }

    /**
     * The percent shortfall which is totalDemand - totalFilled/totalDemand * 100  (Ie the less that is filled the greater
     * the qty percent shortfall.
     *
     * @return  the double representing total percent shortfall for this period
     */
    public double getPercentShortfall() {
        if(getTotalDemand() > 0.0d) {
          return roundToInt((getShortfallQty(false) / getTotalDemand()) * 100);
        }
        return 0.0d;
    }

    /**
     * Setters for the total demand
     *
     * @param demand The total demand for this shortfall period
     */
    public void setTotalDemand(double demand) {
        totalShortfallDemand = demand;
    }

    /**
     * Setters for the total filled
     *
     * @param filled The total amount of demand filled over this shortfall period.
     */
    public void setTotalFilled(double filled) {
        totalFilled = filled;
    }

    /**
     * The string representation of all the fields in this object
     *
     * @return String - The string representation of all the fields in this object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("");
        sb.append("\nStartTime=" + TimeUtils.dateString(getStartTime()));
        sb.append(" - EndTime=" + TimeUtils.dateString(getEndTime()));
        sb.append("\nShortfallQty=" + getShortfallQty(false));
        sb.append(",PercentShortfall=" + getPercentShortfall());
        return sb.toString();
    }

    /**
     * The method to compare two shortfall period.  If all the slot values are the same then they are equal periods.
     *
     * @param sp other shortfall period
     * @return boolean true if all the slots are equivilent
     */
    public boolean equals(ShortfallPeriod sp) {
        return ((getStartTime() == sp.getStartTime()) &&
                (getEndTime() == sp.getEndTime()) &&
                (getTotalDemand() == sp.getTotalDemand()) &&
                (getTotalFilled() == sp.getTotalFilled()));
    }

    /**
     * Helper rounding function
     *
     * @param aNum       Number to round
     * @param roundToInt true if round to integer false yields something rounded to the hundreths place
     * @return double integer or double value rounded to hundreths
     */
    public static double roundAppropriately(double aNum, boolean roundToInt) {
        if (roundToInt) {
            return roundToInt(aNum);
        } else {
            return roundToHundreths(aNum);
        }

    }

    /**
     * Round to the hundreths place the passed in number
     *
     * @param aNum - Number to be rounded to hundreths place.
     * @return The passed in number rounded to the hundreths place.
     */
    public static double roundToHundreths(double aNum) {
        BigDecimal roundedQty = ((new BigDecimal((double) aNum)).setScale(2, BigDecimal.ROUND_HALF_EVEN));
        return roundedQty.doubleValue();
    }

    /**
     * Round to an integer the passed in number
     *
     * @param aNum - Number to be rounded
     * @return double an integer value.
     */
    public static double roundToInt(double aNum) {
        BigDecimal roundedQty = ((new BigDecimal((double) aNum)).setScale(0, BigDecimal.ROUND_HALF_EVEN));
        return roundedQty.doubleValue();
    }

    /**
     * Indicates that this is a persistable object
     *
     * @return true
     */
    public boolean isPersistable() {
        return true;
    }
}
