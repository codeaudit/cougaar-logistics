/*
 * <Copyright>
 * 		    BBNT SOLUTIONS LLC PROPRIETARY
 * Data contained in this document is proprietary to BBNT SOLUTIONS LLC
 * (BBN) or others from whom BBN has acquired such data and shall not be
 * copied, used or disclosed, in whole or in part, by Northrop Grumman
 * Space & Mission Systems Corp. (Northrop Grumman) and The Boeing
 * Company (Boeing) or any other non-US Government entity, for any
 * purpose other than Boeing's performance of its obligations to the
 * United States Government under Prime Contract No. DAAE07-03-9-F001
 * without the prior express written permission of BBN.
 * 
 * 			EXPORT CONTROL WARNING
 * This document contains technical data whose export is restricted by
 * the Arms Export Control Act (Title 22, U.S.C. Section 2751 et. seq.),
 * and the International Traffic in Arms Regulations (ITAR) or Executive
 * order 12470 of the United States of America. Violation of these export
 * laws is subject to severe criminal penalties.
 * 
 * 	    GOVERNMENT PURPOSE RIGHTS (US Government Only)
 * Contract No.:  DAAE07-03-9-F001 (Boeing Prime Contract)
 * Subcontract No.:  51300JAW3S (BBN subcontract under Northrop Grumman)
 * Contractor Name:  BBNT Solutions LLC under subcontract 
 *                   to Northrop Grumman Space & Mission Systems Corp.
 * Contractor Address: 10 Moulton Street, Cambridge MA  02138 USA
 * Expiration Date: None (Perpetual)
 * 
 * The Government is granted Government Purpose Rights to this Data or
 * Software.  The Government rights to use, modify, reproduce, release,
 * perform, display or disclose these technical data is subject to the
 * restriction as stated in Agreement DAAE07-03-9-F001 between the Boeing
 * Company and the Government.  No restrictions apply after the
 * expiration date shown above.  Any reproduction of the technical data
 * or portions thereof marked with this legend must also reproduce the
 * markings.
 * 
 * Copyright © BBNT Solutions LLC.  All Rights Reserved
 * </copyright>
 */
package org.cougaar.logistics.ui.inventory.data;

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleElement;
import org.cougaar.logistics.ui.inventory.data.InventoryScheduleHeader;
import org.cougaar.logistics.ui.inventory.InventoryChartBaseCalendar;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.planning.ldm.plan.Schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.text.ParseException;


public class BlankInventoryData extends InventoryData {

  public static final long HOURLY = TimeUtils.MSEC_PER_HOUR;

  public BlankInventoryData(String assetName, String orgName) {
    super(assetName, orgName, "", "", "", getFakeCTime(),"");
    initialize();
  }

  public void initialize() {
    InventoryScheduleHeader newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.COUNTED_PROJECTWITHDRAW_TASKS_TAG,
                                                                      LogisticsInventoryFormatter.PROJ_TASKS_TYPE,new ArrayList());
    addSchedule(newSchedule);
    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.COUNTED_PROJECTWITHDRAW_TASK_ARS_TAG,
            LogisticsInventoryFormatter.PROJ_ARS_TYPE,new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.PROJECTWITHDRAW_TASKS_TAG,
            LogisticsInventoryFormatter.PROJ_TASKS_TYPE,new ArrayList());
    addSchedule(newSchedule);
    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.PROJECTWITHDRAW_TASK_ARS_TAG,
            LogisticsInventoryFormatter.PROJ_ARS_TYPE,new ArrayList());
    addSchedule(newSchedule);
    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.WITHDRAW_TASKS_TAG,
            LogisticsInventoryFormatter.TASKS_TYPE, new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.WITHDRAW_TASK_ARS_TAG,
            LogisticsInventoryFormatter.ARS_TYPE, new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.PROJECTSUPPLY_TASKS_TAG,
            LogisticsInventoryFormatter.PROJ_TASKS_TYPE, new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.PROJECTSUPPLY_TASK_ARS_TAG,
            LogisticsInventoryFormatter.PROJ_ARS_TYPE, new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.SUPPLY_TASKS_TAG,
            LogisticsInventoryFormatter.TASKS_TYPE, new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.SUPPLY_TASK_ARS_TAG,
            LogisticsInventoryFormatter.ARS_TYPE,new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.RESUPPLY_PROJECTSUPPLY_TASKS_TAG,
            LogisticsInventoryFormatter.PROJ_TASKS_TYPE, new ArrayList());
    addSchedule(newSchedule);
    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.RESUPPLY_PROJECTSUPPLY_TASK_ARS_TAG,
            LogisticsInventoryFormatter.PROJ_ARS_TYPE, new ArrayList());
    addSchedule(newSchedule);
    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.RESUPPLY_SUPPLY_TASKS_TAG,
            LogisticsInventoryFormatter.TASKS_TYPE, new ArrayList());
    addSchedule(newSchedule);
    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.RESUPPLY_SUPPLY_TASK_ARS_TAG,
            LogisticsInventoryFormatter.ARS_TYPE, new ArrayList());
    addSchedule(newSchedule);

    newSchedule = new InventoryScheduleHeader(LogisticsInventoryFormatter.INVENTORY_LEVELS_TAG,
            LogisticsInventoryFormatter.LEVELS_TYPE, new ArrayList());
    addSchedule(newSchedule);

  }

  public long getBucketSize() {
    return HOURLY;
  }

  protected static long getFakeCTime() {
    return InventoryChartBaseCalendar.getBaseTime();
  }
}
