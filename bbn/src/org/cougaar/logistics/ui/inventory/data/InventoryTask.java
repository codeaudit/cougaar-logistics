/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
 
package org.cougaar.logistics.ui.inventory.data;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import java.util.Date;

/** 
 * <pre>
 * 
 * The InventoryTask is the concrete class that corresponds
 * to a supply or withdraw type task schedule element.
 * 
 *
 **/

public class InventoryTask extends InventoryTaskBase {

    public static final int QTY_INDEX=CSV_START_INDEX + 7;    

    protected double qty;

    public InventoryTask(String aParentUID,
			     String myUID,
			     String aVerb,
			     String aForOrg,
			     double aQty,
			     long aStartTime, 
			     long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,aStartTime,anEndTime);
	qty = aQty;
    }

    public double getQty() { return qty; }

    public String getHRHeader() {
	return "<parent UID,UID,Verb,For Org,Start Time,End Time,Qty";
    }

    public String toHRString() {
	return super.toHRString() + "," + getQty();
    }


    public static InventoryTask createFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	//double aQty = (new Double(subStrings[QTY_INDEX])).doubleValue();
        long qtyBits = Long.parseLong(subStrings[QTY_INDEX],16);
        double aQty = Double.longBitsToDouble(qtyBits);
							     
	long aStartTime = -0L;
	String startTimeStr = subStrings[START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	long anEndTime = (new Long(subStrings[END_TIME_INDEX])).longValue();
	
	InventoryTask newTask = new InventoryTask(subStrings[PARENT_UID_INDEX].trim(),
						  subStrings[UID_INDEX].trim(),
						  subStrings[VERB_INDEX].trim(),
						  subStrings[FOR_INDEX].trim(),
						  aQty,aStartTime,anEndTime);

	return newTask;

    }

    public String toString() {
	return super.toString() + ",qty=" + getQty();
    }

    public static void main(String[] args) {
	Date now = new Date();
	InventoryTask task = InventoryTask.createFromCSV(now.getTime() + ",parent UID,UID, SUPPLY,3-69-ARBN,," + (now.getTime() + 50000) + "," + 23 +"\n");
	Logger logger = Logging.getLoggerFactory().createLogger(InventoryLevel.class.getName());
	logger.shout("InventoryTask is " + task);
    }
}


