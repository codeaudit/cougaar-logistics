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
 * The InventoryProjAR is the concrete class that corresponds
 * to a projectsupply or projectwithdraw type Allocation Result.
 * 
 *@see InventoryChildProjAR
 *
 **/

public class InventoryProjAR extends InventoryAR {


    public InventoryProjAR(String aParentUID,
		       String myUID,
		       String aVerb,
		       String aForOrg,
		       int aResultType,
		       boolean isSuccess,
		       double aQty,
		       long aStartTime, 
		       long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,
	      aResultType,isSuccess,aQty,aStartTime,anEndTime);
    }

    public double getDailyRate() {
	long duration = getEndTime() - getStartTime();
	if(duration < TimeUtils.MSEC_PER_DAY)
	    return getQty();
	else
	    return getQty()/(duration/TimeUtils.MSEC_PER_DAY);
    }

    public String toString() {
	return super.toString() + ",dailyRate=" + getDailyRate();
    }

    public static InventoryProjAR createProjFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	double aQty = (new Double(subStrings[AR_QTY_INDEX])).doubleValue();
	long aStartTime = -0L;
	String startTimeStr = subStrings[AR_START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	long anEndTime = (new Long(subStrings[AR_END_TIME_INDEX])).longValue();

	int aResultType = AR_ESTIMATED;
	if(subStrings[AR_TYPE_INDEX].trim().equals(AR_REPORTED_STR)) {
	    aResultType = AR_REPORTED;
	}

	boolean isSuccess = (subStrings[AR_SUCCESS_INDEX].trim().equals(AR_SUCCESS_STR));
	
	InventoryProjAR newAR = new InventoryProjAR(subStrings[PARENT_UID_INDEX].trim(),
						    subStrings[UID_INDEX].trim(),
						    subStrings[VERB_INDEX].trim(),
						    subStrings[FOR_INDEX].trim(),
						    aResultType,isSuccess,
						    aQty,aStartTime,anEndTime);
	
	return newAR;

    }

    public static void main(String[] args) {
	Date now = new Date();
	InventoryProjAR ar = InventoryProjAR.createProjFromCSV(now.getTime() + ",parent UID,UID, SUPPLY,3-69-ARBN,ESTIMATED,SUCCESS," + now.getTime() + "," + (now.getTime() + (TimeUtils.MSEC_PER_DAY*3)) + "," + 69 +"\n");
	System.out.println("InventoryProjAR is " + ar);
	System.out.println("Children are");
	InventoryChildProjAR[] children = InventoryChildProjAR.expandProjAR(ar,
									    (TimeUtils.MSEC_PER_DAY*2));
	for(int i=0; i < children.length; i++){
	    System.out.println(children[i]);
	}
    }
}

