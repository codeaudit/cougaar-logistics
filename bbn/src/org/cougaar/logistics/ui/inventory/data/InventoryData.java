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

    protected long bucketSize;

    public InventoryData(String itemName, String anOrg, String aUnit, String nomen, long aStartCDay) {
        item = itemName;
        org = anOrg;
        unit = aUnit;
        nomenclature = nomen;
        startCDay = aStartCDay;
        schedules = new Hashtable(20);
        bucketSize = -1;
    }

    public void addSchedule(InventoryScheduleHeader schedule) {
        schedules.put(schedule.getName(), schedule);
    }

    public Hashtable getSchedules() {
        return schedules;
    }

    public String getUnit() {
        return unit;
    }

    public long getStartCDay() {
        return startCDay;
    }

    public String getOrg() {
        return org;
    }

    public String getItem() {
        return item;
    }

    public String getNomenclature() {
        return nomenclature;
    }

    public void writeHRString(Writer writer) throws java.io.IOException {
        writer.write("<" + LogisticsInventoryFormatter.INVENTORY_DUMP_TAG +
                     " org=" + org + " item=" + item + " unit=" + unit +
                     " cDay=" + new Date(startCDay) + ">\n");

        for (int i = 1; i <= 5; i++) {
            Enumeration e = getSchedules().elements();
            while (e.hasMoreElements()) {
                InventoryScheduleHeader schedule =
                        (InventoryScheduleHeader) e.nextElement();
                if (schedule.getType() == i) {
                    schedule.writeHRString(writer);
                }
            }
        }
        writer.write("</" + LogisticsInventoryFormatter.INVENTORY_DUMP_TAG
                     + ">\n");
    }

    public long getBucketSize() {
        if (bucketSize == -1) {
            InventoryScheduleHeader schedHeader = (InventoryScheduleHeader)
                    getSchedules().get(LogisticsInventoryFormatter.INVENTORY_LEVELS_TAG);
            ArrayList levels = schedHeader.getSchedule();
            for (int i = 0; i < levels.size(); i++) {
                InventoryScheduleElement level = (InventoryScheduleElement) levels.get(i);
                long startTime = level.getStartTime();
                long endTime = level.getEndTime();
                if(bucketSize == -1){
                    bucketSize = endTime-startTime;
                }
                else {
                    bucketSize = Math.min(bucketSize, (endTime - startTime));
                }
            }
        }
        return bucketSize;
    }
}


