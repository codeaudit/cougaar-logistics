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

import java.util.Vector;
import java.util.Hashtable;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.util.TimeSpan;

/** 
 * <pre>
 * 
 * The InventoryScheduleElement is the abstract class that all data
 * elements (tasks,ARs, and levels) inherit from  that are
 * coming over the wire from the servlet in csv form. 
 * 
 * @see InventoryTaskBase
 * @see InventoryTask
 * @see InventoryProjTask
 * @see InventoryChildProjTask
 * @see InventoryAR
 * @see InventoryProjAR
 * @see InventoryChildProjAR
 * @see InventoryLevel
 *
 **/



public abstract class InventoryScheduleElement implements TimeSpan {

    public static String CSV_DELIMITER = ",";
    public static String SPLIT_REGEX = CSV_DELIMITER;

    public static final int CSV_START_INDEX = 0;
    public static final int CYCLE_INDEX = CSV_START_INDEX;
    
    long startTime;
    long endTime;

    protected Logger logger;

    public InventoryScheduleElement(long aStartTime, long anEndTime) {
	startTime = aStartTime;
	endTime = anEndTime;
	logger = Logging.getLogger(this);
    }

    public long getStartTime() {
	if(startTime == -0L) {
	    return endTime - 1;
	}
	else return startTime;
    }
    public long getEndTime() {return endTime;}

    public String toString() {
	return super.toString() + " - startTime=" + getStartTime() + ",endTime=" + getEndTime();
    }


    public String getHRHeader() { return ""; }
    public String toHRString() { return ""; }
}


