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
import java.util.Enumeration;

import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

/** 
 * <pre>
 * 
 * The InventoryData holds all the Inventory Schedules
 * data and asset name and org associated with the inventory
 * data necessary to chart the graph.
 *
 **/

public class InventoryData {
    
    protected String    item;
    protected String    org;
    protected String    unit;
    protected String    nomenclature;
    protected long      startCDay;

    protected Hashtable schedules;

    public InventoryData(String itemName,String anOrg,String aUnit,String nomen,long aStartCDay) {
	item = itemName;
	org = anOrg;
	unit = aUnit;
	nomenclature = nomen;
	startCDay = aStartCDay;
	schedules = new Hashtable(20);
    }

    public void addSchedule(InventoryScheduleHeader schedule) {
	schedules.put(schedule.getName(),schedule);
    }

    public Hashtable getSchedules() { return schedules; }
    public String getUnit() { return unit; }
    public long getStartCDay() { return startCDay; }
    public String getOrg() { return org; }
    public String getItem() { return item; }
    public String getNomenclature() { return nomenclature; }

    public void writeHRString(Writer writer) throws java.io.IOException {
	writer.write("<" + LogisticsInventoryFormatter.INVENTORY_DUMP_TAG +
		     " org=" + org + " item=" + item + " unit=" + unit +
		     " cDay=" + new Date(startCDay) +">\n");

	for(int i=1; i <= 5; i++) {
	    Enumeration e = getSchedules().elements();
	    while(e.hasMoreElements()) {
		InventoryScheduleHeader schedule = 
		    (InventoryScheduleHeader) e.nextElement();
		if(schedule.getType() == i) {
		    schedule.writeHRString(writer);
		}
	    }
	}
	writer.write("</" + LogisticsInventoryFormatter.INVENTORY_DUMP_TAG 
		     + ">\n");
    }

}


