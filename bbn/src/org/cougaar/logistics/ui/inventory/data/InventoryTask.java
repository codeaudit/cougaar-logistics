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

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import java.util.Date;

/** 
 * <pre>
 * 
 * The InventoryTask is the concrete class that corresponds
 * to a supply or withdraw type task schedule element.
 * 
 *
 **/

public class InventoryTask extends InventoryTaskBase {

    public static final int QTY_INDEX=CSV_START_INDEX + 7;    

    protected double qty;

    public InventoryTask(String aParentUID,
			     String myUID,
			     String aVerb,
			     String aForOrg,
			     double aQty,
			     long aStartTime, 
			     long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,aStartTime,anEndTime);
	qty = aQty;
    }

    public double getQty() { return qty; }

    public String getHRHeader() {
	return "<parent UID,UID,Verb,For Org,Start Time,End Time,Qty";
    }

    public String toHRString() {
	return super.toHRString() + "," + getQty();
    }


    public static InventoryTask createFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	//double aQty = (new Double(subStrings[QTY_INDEX])).doubleValue();
        long qtyBits = Long.parseLong(subStrings[QTY_INDEX],16);
        double aQty = Double.longBitsToDouble(qtyBits);
							     
	long aStartTime = -0L;
	String startTimeStr = subStrings[START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	long anEndTime = (new Long(subStrings[END_TIME_INDEX])).longValue();
	
	InventoryTask newTask = new InventoryTask(subStrings[PARENT_UID_INDEX].trim(),
						  subStrings[UID_INDEX].trim(),
						  subStrings[VERB_INDEX].trim(),
						  subStrings[FOR_INDEX].trim(),
						  aQty,aStartTime,anEndTime);

	return newTask;

    }

    public String toString() {
	return super.toString() + ",qty=" + getQty();
    }

    public static void main(String[] args) {
	Date now = new Date();
	InventoryTask task = InventoryTask.createFromCSV(now.getTime() + ",parent UID,UID, SUPPLY,3-69-ARBN,," + (now.getTime() + 50000) + "," + 23 +"\n");
	Logger logger = Logging.getLoggerFactory().createLogger(InventoryLevel.class.getName());
	logger.shout("InventoryTask is " + task);
    }
}


