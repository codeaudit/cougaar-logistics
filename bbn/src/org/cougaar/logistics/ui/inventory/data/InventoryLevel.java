/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory.data;

import java.util.Date;
import org.cougaar.logistics.plugin.inventory.TimeUtils;

/** 
 * <pre>
 * 
 * The InventoryLevel is a concrete class that reflects a
 * level csv in the xml.  It contains start and endtime
 * as well as reorder and inventory level for that given 
 * bucket.
 * 
 *
 **/



public class InventoryLevel extends InventoryScheduleElement {

    protected double reorderLevel;
    protected double inventoryLevel;

    public static final int START_TIME_INDEX=CSV_START_INDEX + 1;
    public static final int END_TIME_INDEX=CSV_START_INDEX + 2;
    public static final int REORDER_LEVEL_INDEX=CSV_START_INDEX + 3;
    public static final int INVENTORY_LEVEL_INDEX=CSV_START_INDEX + 4;

    public InventoryLevel(double aReorderLevel,
			  double anInventoryLevel,
			  long aStartTime, 
			  long anEndTime) {
	super(aStartTime,anEndTime);
	reorderLevel = aReorderLevel;
	inventoryLevel = anInventoryLevel;
    }

    public double getReorderLevel() { return reorderLevel; }
    public double getInventoryLevel() { return inventoryLevel; }

    public String toString() {
	return super.toString() + ",reorderLevel=" + getReorderLevel() + 
	    ",inventoryLevel=" + getInventoryLevel();
    }


    public static InventoryLevel createFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	double aReorderLevel = (new Double(subStrings[REORDER_LEVEL_INDEX])).doubleValue();
	double anInventoryLevel = (new Double(subStrings[REORDER_LEVEL_INDEX])).doubleValue();
	long aStartTime = -0L;
	String startTimeStr = subStrings[START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	long anEndTime = (new Long(subStrings[END_TIME_INDEX])).longValue();
	
	InventoryLevel newLevel = new InventoryLevel(aReorderLevel,
						     anInventoryLevel,
						     aStartTime,
						     anEndTime);

	return newLevel;
    }
    
    public static void main(String[] args) {
	Date now = new Date();
	long nowTime = now.getTime();
	InventoryLevel level = InventoryLevel.createFromCSV(nowTime + "," + nowTime + "," +
							    (nowTime + (3*TimeUtils.MSEC_PER_DAY)) + ",42.0,123.0");
	System.out.println("InventoryLevel is " + level);
    }
    
}


