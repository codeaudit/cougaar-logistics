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




