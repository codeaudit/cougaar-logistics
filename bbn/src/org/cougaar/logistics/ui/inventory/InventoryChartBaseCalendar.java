/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
 
package org.cougaar.logistics.ui.inventory;

import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <pre>
 *
 * The InventoryChartBaseCalendar supports getting the base time
 * from which the axis and other information in the chart
 * is oriented to.
 *
 * @see InventoryUIFrame
 *
 **/

public class InventoryChartBaseCalendar extends GregorianCalendar {

    final static private int baseYear = 2005;
    final static private TimeZone baseTimeZone = TimeZone.getTimeZone("GMT");
    final static private InventoryChartBaseCalendar baseCal = new InventoryChartBaseCalendar();

    public InventoryChartBaseCalendar() {
        super(baseYear, 0, 1, 0, 0);
        setTimeZone(baseTimeZone);
        // Weird thing if you reverse the constructor and setter like below doing exact same thing supposedly
        // you get a different base time!!! and it effects the calendar.
        //super(TimeZone.getTimeZone("GMT"));
        //set(baseYear,0,0,0,0,0);
        //System.out.println("InventoryChartBaseCalendar::My date: " + this.getTime());
    }

    public static long getBaseTime() {
        return baseCal.getTime().getTime();
    }

    public static int getBaseYear() {
        return baseYear;
    }

}




