/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.agent.ClusterIdentifier;

import java.lang.Class;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import org.cougaar.util.log.Logger;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

/** Provides convenience methods. */
public class TimeUtils {
    public static DateFormat dateTimeFormat_;
    /** number of msec per day */
    // 86400000 msec/day = 1000msec/sec * 60sec/min *60min/hr * 24 hr/day
    public static final long MSEC_PER_WEEK = 7 * 86400000L;
    public static final long MSEC_PER_DAY  =     86400000L;
    public static final long MSEC_PER_HOUR =      3600000L;
    public static final long MSEC_PER_MIN  =        60000L;
    public static final long SEC_PER_DAY   =        86400L;

    private transient Logger logger;

    public TimeUtils(InventoryPlugin aPlugin) {
	if(aPlugin == null) {
	    logger = NullLoggingServiceImpl.getNullLoggingServiceImpl();
	}
	else {
	    logger = (Logger)aPlugin.getLoggingService(this);
	}
    }

    public static long addNDays(long time, int n_days) {
	return addNDaysTime(time, n_days);
    }

    public static long addNDaysTime(long time, int n_days) {
	return (long)((int)(time/MSEC_PER_DAY)+n_days)*MSEC_PER_DAY;
    }

  public static long subtractNDays(long time, int n_days) {
    return (long)((int)(time/MSEC_PER_DAY)-n_days)*MSEC_PER_DAY;
  }

    public static String dateString () {
	return dateString(new Date());
    }

    public static String dateString (long time) {
	return dateString(new Date(time));
    }

    public static String dateString (Date date) {
//  	dateTimeFormat_ = DateFormat.getDateTimeInstance(DateFormat.SHORT,
//  							 DateFormat.SHORT);
	dateTimeFormat_= new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS z");
	String sdate = dateTimeFormat_.format(date);
	// mape '9/8/00 12:00 AM' to ' 9/8/00 12:00 AM'
	while(sdate.length()<17){
	    sdate = " "+sdate;
	}
	return sdate;
    }

    public static String msecDateString (long time) {
	dateTimeFormat_ = DateFormat.getDateTimeInstance(DateFormat.SHORT,
							 DateFormat.SHORT);
	String sdate = dateTimeFormat_.format(new Date(time));
	// mape '9/8/00 12:00 AM' to ' 9/8/00 12:00 AM'
	while(sdate.length()<17){
	    sdate = " "+sdate;
	}
	long msec = time - (time/1000)*1000;
	return sdate+":"+msec;
    }

    public static int getDaysBetween(long today, long tomorrow) {
	return (int)((tomorrow-today)/MSEC_PER_DAY);
    }

    
    public long pushToEndOfDay(long time) {
// 	logger.debug("pushToMidnight(), Before: "+TimeUtils.dateString(time));
	return pushToEndOfDay(Calendar.getInstance(), time);
    }    

    public long pushToEndOfDay(Calendar calendar, long time) {
// 	logger.debug("TimeUtils", "pushToMidnight(), Before: "+TimeUtils.dateString(time));
	calendar.setTime(new Date(time));
	calendar.set(Calendar.HOUR, 11);
	calendar.set(Calendar.MINUTE, 59);
	calendar.set(Calendar.AM_PM, Calendar.PM);
	calendar.set(Calendar.SECOND, 59);
 	calendar.set(Calendar.MILLISECOND, 999);
    	calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
//   	logger.debug("TimeUtils", "pushToEndOfDay(), After : "+TimeUtils.dateString(calendar.getTime().getTime()));
	return calendar.getTime().getTime();
    }
}

