/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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


