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

import java.util.Vector;
import java.util.Hashtable;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.util.TimeSpan;

/** 
 * <pre>
 * 
 * The InventoryScheduleElement is the abstract class that all data
 * elements (tasks,ARs, and levels) inherit from  that are
 * coming over the wire from the servlet in csv form. 
 * 
 * @see InventoryTaskBase
 * @see InventoryTask
 * @see InventoryProjTask
 * @see InventoryChildProjTask
 * @see InventoryAR
 * @see InventoryProjAR
 * @see InventoryChildProjAR
 * @see InventoryLevel
 *
 **/



public abstract class InventoryScheduleElement implements TimeSpan {

    public static String CSV_DELIMITER = ",";
    public static String SPLIT_REGEX = CSV_DELIMITER;

    public static final int CSV_START_INDEX = 0;
    public static final int CYCLE_INDEX = CSV_START_INDEX;
    
    long startTime;
    long endTime;

    protected Logger logger;

    public InventoryScheduleElement(long aStartTime, long anEndTime) {
	startTime = aStartTime;
	endTime = anEndTime;
	logger = Logging.getLogger(this);
    }

    public long getStartTime() {
	if(startTime == -0L) {
	    return endTime - 1;
	}
	else return startTime;
    }
    public long getEndTime() {return endTime;}

    public String toString() {
	return super.toString() + " - startTime=" + getStartTime() + ",endTime=" + getEndTime();
    }


    public String getHRHeader() { return ""; }
    public String toHRString() { return ""; }
}


