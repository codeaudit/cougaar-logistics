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

import java.io.Writer;
import java.io.IOException;

import java.util.ArrayList;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;


/** 
 * <pre>
 * 
 * The InventoryScheduleHeader holds the schedule of all the
 * elements in a given schedule
 *
 **/



public class InventoryScheduleHeader{
    
    public final static int TASKS_TYPE=1;
    public final static int PROJ_TASKS_TYPE=2;
    public final static int ARS_TYPE=3;
    public final static int PROJ_ARS_TYPE=4;
    public final static int LEVELS_TYPE=5;
    
    protected String    name;
    protected ArrayList schedule;
    protected int       type;

    public InventoryScheduleHeader(String aName,int aType, ArrayList aSchedule) {
	name = aName;
	schedule = aSchedule;
	type = aType;
    }

    public InventoryScheduleHeader(String aName,String aType, ArrayList aSchedule) {
	this(aName,getTypeInt(aType),aSchedule);
    }

    public String getName() { return name; }
    public ArrayList getSchedule() {return schedule; }
    public int getType() { return type; }

    public static int getTypeInt(String aType) {
	if(aType.equals(LogisticsInventoryFormatter.TASKS_TYPE))
	    return InventoryScheduleHeader.TASKS_TYPE;
	if(aType.equals(LogisticsInventoryFormatter.PROJ_TASKS_TYPE))
	    return InventoryScheduleHeader.PROJ_TASKS_TYPE;
	if(aType.equals(LogisticsInventoryFormatter.ARS_TYPE))
	    return InventoryScheduleHeader.ARS_TYPE;
	if(aType.equals(LogisticsInventoryFormatter.PROJ_ARS_TYPE))
	    return InventoryScheduleHeader.PROJ_ARS_TYPE;
	if(aType.equals(LogisticsInventoryFormatter.LEVELS_TYPE))
	    return InventoryScheduleHeader.LEVELS_TYPE;
	return -1;
    }

    public static String getTypeString(int aType) {
	switch(aType) {
	case InventoryScheduleHeader.TASKS_TYPE:
	    return LogisticsInventoryFormatter.TASKS_TYPE;
	case InventoryScheduleHeader.PROJ_TASKS_TYPE:
	    return LogisticsInventoryFormatter.PROJ_TASKS_TYPE;
	case InventoryScheduleHeader.ARS_TYPE:
	    return LogisticsInventoryFormatter.ARS_TYPE;
	case InventoryScheduleHeader.PROJ_ARS_TYPE:
	    return LogisticsInventoryFormatter.PROJ_ARS_TYPE;
	case InventoryScheduleHeader.LEVELS_TYPE:
	    return LogisticsInventoryFormatter.LEVELS_TYPE;
	default:
	    return "Unknown Type";
	}
    }

    public void writeHRString(Writer writer) throws IOException {
	writer.write("<" + getName() + "  type=" + 
		     getTypeString(getType()) + ">\n");
	for(int i=0; i < schedule.size(); i++) {
	    InventoryScheduleElement e = 
		(InventoryScheduleElement)schedule.get(i);
	    if(i==0) {
		writer.write(e.getHRHeader() + "\n");
	    }
	    writer.write(e.toHRString() + "\n");
	}
	writer.write("</" + getName() + ">\n");
    }
}


