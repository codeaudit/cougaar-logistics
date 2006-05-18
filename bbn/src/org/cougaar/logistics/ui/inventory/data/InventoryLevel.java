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
import org.cougaar.logistics.plugin.inventory.TimeUtils;

/** 
 * <pre>
 * 
 * The InventoryLevel is a concrete class that reflects a
 * level csv in the xml.  It contains start and endtime
 * as well as reorder and inventory level for that given 
 * bucket.
 * 
 *
 **/



public class InventoryLevel extends InventoryScheduleElement {

    protected double reorderLevel;
    protected Double inventoryLevel;
    protected Double targetLevel;

    protected String activityType;
    protected String optempo;

    private static final int START_TIME_INDEX=CSV_START_INDEX + 1;
    private static final int END_TIME_INDEX=CSV_START_INDEX + 2;
    private static final int REORDER_LEVEL_INDEX=CSV_START_INDEX + 3;
    private static final int INVENTORY_LEVEL_INDEX=CSV_START_INDEX + 4;
    private static final int TARGET_LEVEL_INDEX=CSV_START_INDEX + 5;
    private static final int ACTIVITY_TYPE_INDEX=CSV_START_INDEX + 6;
    private static final int OPTEMPO_INDEX=CSV_START_INDEX + 7;

    public InventoryLevel(double aReorderLevel,
			  Double anInventoryLevel,
			  Double aTargetLevel,
        String anActivityType,
        String anOptempo,
			  long aStartTime,
			  long anEndTime) {
	super(aStartTime,anEndTime);
	reorderLevel = aReorderLevel;
	inventoryLevel = anInventoryLevel;
	targetLevel = aTargetLevel;
  activityType = anActivityType;
  optempo = anOptempo;
    }

    public double getReorderLevel() { return reorderLevel; }
    public Double getInventoryLevel() { return inventoryLevel; }
    public Double getTargetLevel() { return targetLevel;}
    public String getActivityType() { return activityType; }
    public String getOptempo() { return optempo; }

    public String getHRHeader() {
	return "<Start Time,End Time,Reorder Level,Inventory Level, Target Level, Activity Type, Optempo>";
    }

    public String toHRString() {
	return "" + new Date(startTime) + "," +  new Date(endTime) + ","
	    + getReorderLevel() + "," + getInventoryLevel() + "," +
	    getTargetLevel() + "," + getActivityType() + "," + getOptempo();
    }

    public String toString() {
	return super.toString() + ",reorderLevel=" + getReorderLevel() +
	    ",inventoryLevel=" + getInventoryLevel() + ",targetLevel=" + getTargetLevel() +
      ",activityType=" + getActivityType() + ",optempo=" + getOptempo();
    }


    public static InventoryLevel createFromCSV(String csvString) {
	String[] subStrings = csvString.split(SPLIT_REGEX);
	
	double aReorderLevel = (new Double(subStrings[REORDER_LEVEL_INDEX])).doubleValue();
	Double anInventoryLevel;
	Double aTargetLevel;
  String anActivityType;
  String anOptempo;

  if((subStrings.length == INVENTORY_LEVEL_INDEX) ||
	   (subStrings[INVENTORY_LEVEL_INDEX].trim().equals(""))) {
	    anInventoryLevel = null;
	}
	else {
	    anInventoryLevel = new Double(subStrings[INVENTORY_LEVEL_INDEX]);
	}

	if((subStrings.length <= TARGET_LEVEL_INDEX) ||
	   (subStrings[TARGET_LEVEL_INDEX].trim().equals(""))) {
	    aTargetLevel = null;
	}
	else {
	    aTargetLevel = new Double(subStrings[TARGET_LEVEL_INDEX]);
	}
	if((subStrings.length <= ACTIVITY_TYPE_INDEX) ||
	   (subStrings[ACTIVITY_TYPE_INDEX].trim().equals(""))) {
	    anActivityType = null;
	}
	else {
	    anActivityType = subStrings[ACTIVITY_TYPE_INDEX].intern();
	}
	if((subStrings.length <= OPTEMPO_INDEX) ||
	   (subStrings[OPTEMPO_INDEX].trim().equals(""))) {
	    anOptempo = null;
	}
	else {
	    anOptempo = subStrings[OPTEMPO_INDEX].intern();
	}
	long aStartTime = -0L;
	String startTimeStr = subStrings[START_TIME_INDEX].trim();
	if(!(startTimeStr.equals(""))) {
	    aStartTime = (new Long(startTimeStr)).longValue();
	}
	long anEndTime = (new Long(subStrings[END_TIME_INDEX])).longValue();
	
	InventoryLevel newLevel = new InventoryLevel(aReorderLevel,
						     anInventoryLevel,
						     aTargetLevel,
                 anActivityType,
                 anOptempo,
						     aStartTime,
						     anEndTime);

	return newLevel;
    }
    
    public static void main(String[] args) {
	Date now = new Date();
	long nowTime = now.getTime();
	InventoryLevel level = InventoryLevel.createFromCSV(nowTime + "," + nowTime + "," +
							    (nowTime + (3*TimeUtils.MSEC_PER_DAY)) + ",42.0,123.0,0,DEFENSIVE,HIGH");
	Logger logger = Logging.getLoggerFactory().createLogger(InventoryLevel.class.getName());
	logger.shout("InventoryLevel is " + level);
    }
    
}


