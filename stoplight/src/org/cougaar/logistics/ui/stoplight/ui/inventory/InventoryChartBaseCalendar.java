/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
// Decompiled by Decafe PRO - Java Decompiler
// Classes: 1   Methods: 4   Fields: 3

package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.util.*;

public class InventoryChartBaseCalendar extends GregorianCalendar
{

    private static final int baseYear = 2000;
//    private static final TimeZone baseTimeZone = TimeZone.getTimeZone("GMT");
    private static final TimeZone baseTimeZone = TimeZone.getDefault();
    private static final InventoryChartBaseCalendar baseCal = new InventoryChartBaseCalendar();

    public InventoryChartBaseCalendar()
    {
        super(2000, 0, 0);
        setTimeZone(baseTimeZone);
    }

    public static long getBaseTime()
    {
        return baseCal.getTime().getTime();
    }

    public static int getBaseYear()
    {
        return 2000;
    }

}
