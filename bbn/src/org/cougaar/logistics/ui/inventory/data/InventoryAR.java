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

import java.util.Date;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.ldm.Constants;

/** 
 * <pre>
 * 
 * The InventoryAR is the concrete class that corresponds
 * to a supply or withdraw type Allocation Result.
 * 
 *@see InventoryProjAR
 *@see InventoryChildProjAR
 *
 **/

public class InventoryAR extends InventoryTaskBase {

    public static final int AR_TYPE_INDEX=CSV_START_INDEX + 5;    
    public static final int AR_SUCCESS_INDEX=CSV_START_INDEX + 6;    
    public static final int AR_START_TIME_INDEX=CSV_START_INDEX + 7;    
    public static final int AR_END_TIME_INDEX=CSV_START_INDEX + 8;    
    public static final int AR_QTY_INDEX=CSV_START_INDEX + 9;

    public final static String AR_SUCCESS_STR = LogisticsInventoryFormatter.AR_SUCCESS_STR;
    public final static String AR_FAILURE_STR = LogisticsInventoryFormatter.AR_FAILURE_STR;
    public final static String AR_ESTIMATED_STR = LogisticsInventoryFormatter.AR_ESTIMATED_STR;
    public final static String AR_REPORTED_STR = LogisticsInventoryFormatter.AR_REPORTED_STR;

    public final static int AR_SUCCESS = 1;
    public final static int AR_FAILURE = 0;

    public final static int AR_REPORTED = 1;
    public final static int AR_ESTIMATED = 0;

    protected double qty;
    protected boolean success;
    protected int resultType;
    
    private static Logger logger=Logging.getLoggerFactory().createLogger(InventoryAR.class.getName());

    public InventoryAR(String aParentUID,
		       String myUID,
		       String aVerb,
		       String aForOrg,
		       int aResultType,
		       boolean isSuccess,
		       double aQty,
		       long aStartTime, 
		       long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,aStartTime,anEndTime);
	qty = aQty;
	success = isSuccess;
	resultType = aResultType;
    }

    public double getQty() { return qty; }
    public boolean isSuccess() { return success; }
    public boolean isReported() { return resultType==AR_REPORTED; }
    public boolean isEstimated() { return resultType==AR_ESTIMATED; }
    public int getResultType() {return resultType;}
    public String getResultTypeStr() {
	if(resultType == AR_REPORTED) {
	    return AR_REPORTED_STR;
	}
	else
	    return AR_ESTIMATED_STR;
    }

    public String getSuccessStr() {
	if(isSuccess()) {
	    return AR_SUCCESS_STR;
	}
	else
	    return AR_FAILURE_STR;
    }
    
    public String getHRHeader() {
	return "<parent UID,UID,Verb,For Org,Result Type,Success?,Start Time,End Time,Qty>";
    }

    public String toHRString() {
	return getParentUID() + "," + getUID() + "," +
	    getVerb() + "," + getDestination() + "," +
	    getResultTypeStr() + "," + getSuccessStr() + "," +
	    new Date(startTime) + "," +  new Date(endTime) + ","
	    + getQty();
    }

    public static InventoryAR createFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	//double aQty = (new Double(subStrings[AR_QTY_INDEX])).doubleValue();

        long qtyBits = Long.parseLong(subStrings[AR_QTY_INDEX],16);
        double aQty = Double.longBitsToDouble(qtyBits);

	long aStartTime = -1;
	long anEndTime = -1;
	String startTimeStr = subStrings[AR_START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	String endTimeStr = subStrings[AR_END_TIME_INDEX].trim();
	if(!(endTimeStr.equals(""))) {
	    anEndTime= (new Long(endTimeStr)).longValue();
	    if((aStartTime == 0) && 
	       ((subStrings[VERB_INDEX].equals(Constants.Verb.WITHDRAW)) ||  
		(subStrings[VERB_INDEX].equals(Constants.Verb.SUPPLY)))) {
		logger.warn("A 0 start time in the withdraw task UID:" +
			    subStrings[UID_INDEX].trim() + ".  Setting the start time to " +
			    "a moment before end time.   This is harmless, " +
			    "but why is there 0 start time in the society?");
		aStartTime = anEndTime-1;
	    }
	    if(aStartTime == -1) {
		aStartTime = anEndTime-1;
	    }
	}
	else if(aStartTime == -1) {
	    throw new RuntimeException("Both Start time and end time have no value cannot process!!");
	}
	else {
	    anEndTime = aStartTime + 1;
	}

	int aResultType = AR_ESTIMATED;
	if(subStrings[AR_TYPE_INDEX].trim().equals(AR_REPORTED_STR)) {
	    aResultType = AR_REPORTED;
	}

	boolean isSuccess = (subStrings[AR_SUCCESS_INDEX].trim().equals(AR_SUCCESS_STR));
	
	InventoryAR newAR = new InventoryAR(subStrings[PARENT_UID_INDEX].trim(),
					    subStrings[UID_INDEX].trim(),
					    subStrings[VERB_INDEX].trim(),
					    subStrings[FOR_INDEX].trim(),
					    aResultType,isSuccess,
					    aQty,aStartTime,anEndTime);

	return newAR;

    }

    public String toString() {
	return super.toString() + ",qty=" + getQty() + 
	    ",resultType=" + getResultTypeStr() +
	    ",success=" + isSuccess();
    }

    public static void main(String[] args) {
	Date now = new Date();
	InventoryAR ar = InventoryAR.createFromCSV(now.getTime() + ",parent UID,UID, SUPPLY,3-69-ARBN,ESTIMATED,SUCCESS," + now.getTime() + "," + (now.getTime() + 1) + "," + 23 +"\n");
	logger.shout("InventoryAR is " + ar);
    }
}


