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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.core.mts.MessageAddress;

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
    public static final long SEC_PER_HOUR  =         3600L;
    public static final int HOUR_PER_DAY  =            24;

    private transient Logger logger;

    public TimeUtils(UtilsProvider provider) {
	if(provider == null) {
	    logger = NullLoggingServiceImpl.getLoggingService();
	}
	else {
	    logger = (Logger)provider.getLoggingService(this);
	}
    }

  /** Add N days converts a time (long) into an int
   *  representing the number of days and then adds n_days
   *  therefore the resulting long will always be on the midnight boundary
   **/
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

