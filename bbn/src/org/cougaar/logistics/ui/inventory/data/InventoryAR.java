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
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

/** 
 * <pre>
 * 
 * The InventoryAR is the concrete class that corresponds
 * to a supply or withdraw type Allocation Result.
 * 
 *@see InventoryProjAR
 *@see InventoryChildProjAR
 *
 **/

public class InventoryAR extends InventoryTaskBase {

    public static final int AR_TYPE_INDEX=CSV_START_INDEX + 5;    
    public static final int AR_SUCCESS_INDEX=CSV_START_INDEX + 6;    
    public static final int AR_START_TIME_INDEX=CSV_START_INDEX + 7;    
    public static final int AR_END_TIME_INDEX=CSV_START_INDEX + 8;    
    public static final int AR_QTY_INDEX=CSV_START_INDEX + 9;

    public final static String AR_SUCCESS_STR = LogisticsInventoryFormatter.AR_SUCCESS_STR;
    public final static String AR_FAILURE_STR = LogisticsInventoryFormatter.AR_FAILURE_STR;
    public final static String AR_ESTIMATED_STR = LogisticsInventoryFormatter.AR_ESTIMATED_STR;
    public final static String AR_REPORTED_STR = LogisticsInventoryFormatter.AR_REPORTED_STR;

    public final static int AR_SUCCESS = 1;
    public final static int AR_FAILURE = 0;

    public final static int AR_REPORTED = 1;
    public final static int AR_ESTIMATED = 0;

    protected double qty;
    protected boolean success;
    protected int resultType;

    public InventoryAR(String aParentUID,
		       String myUID,
		       String aVerb,
		       String aForOrg,
		       int aResultType,
		       boolean isSuccess,
		       double aQty,
		       long aStartTime, 
		       long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,aStartTime,anEndTime);
	qty = aQty;
	success = isSuccess;
	resultType = aResultType;
    }

    public double getQty() { return qty; }
    public boolean isSuccess() { return success; }
    public boolean isReported() { return resultType==AR_REPORTED; }
    public boolean isEstimated() { return resultType==AR_ESTIMATED; }
    public int getResultType() {return resultType;}
    public String getResultTypeStr() {
	if(resultType == AR_REPORTED) {
	    return AR_REPORTED_STR;
	}
	else
	    return AR_ESTIMATED_STR;
    }
    

    public static InventoryAR createFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	double aQty = (new Double(subStrings[AR_QTY_INDEX])).doubleValue();
	long aStartTime = -1;
	long anEndTime = -1;
	String startTimeStr = subStrings[AR_START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	String endTimeStr = subStrings[AR_END_TIME_INDEX].trim();
	if(!(endTimeStr.equals(""))) {
	    anEndTime= (new Long(endTimeStr)).longValue();
	    if(aStartTime == -1) {
		aStartTime = anEndTime-1;
	    }
	}
	else if(aStartTime == -1) {
	    throw new RuntimeException("Both Start time and end time have no value cannot process!!");
	}
	else {
	    anEndTime = aStartTime + 1;
	}

	int aResultType = AR_ESTIMATED;
	if(subStrings[AR_TYPE_INDEX].trim().equals(AR_REPORTED_STR)) {
	    aResultType = AR_REPORTED;
	}

	boolean isSuccess = (subStrings[AR_SUCCESS_INDEX].trim().equals(AR_SUCCESS_STR));
	
	InventoryAR newAR = new InventoryAR(subStrings[PARENT_UID_INDEX].trim(),
					    subStrings[UID_INDEX].trim(),
					    subStrings[VERB_INDEX].trim(),
					    subStrings[FOR_INDEX].trim(),
					    aResultType,isSuccess,
					    aQty,aStartTime,anEndTime);

	return newAR;

    }

    public String toString() {
	return super.toString() + ",qty=" + getQty() + 
	    ",resultType=" + getResultTypeStr() +
	    ",success=" + isSuccess();
    }

    public static void main(String[] args) {
	Date now = new Date();
	InventoryAR ar = InventoryAR.createFromCSV(now.getTime() + ",parent UID,UID, SUPPLY,3-69-ARBN,ESTIMATED,SUCCESS," + now.getTime() + "," + (now.getTime() + 1) + "," + 23 +"\n");
	System.out.println("InventoryAR is " + ar);
    }
}


