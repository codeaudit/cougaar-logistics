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
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.logistics.plugin.inventory.TimeUtils;




/** 
 * <pre>
 * 
 * The InventoryTaskBase is the abstract class that task based
 * types of schedule data elements inherit from.  It encapsulates
 * all the common bits of data to that class.
 * 
 * @see InventoryTask
 * @see InventoryProjTask
 * @see InventoryChildProjTask
 * @see InventoryAR
 * @see InventoryProjAR
 * @see InventoryChildProjAR
 *
 **/

public abstract class InventoryTaskBase extends InventoryScheduleElement {

    public static final int PARENT_UID_INDEX=CSV_START_INDEX + 1;
    public static final int UID_INDEX=CSV_START_INDEX + 2;
    public static final int VERB_INDEX=CSV_START_INDEX + 3;
    public static final int FOR_INDEX=CSV_START_INDEX + 4;
    public static final int START_TIME_INDEX=CSV_START_INDEX + 5;
    public static final int END_TIME_INDEX=CSV_START_INDEX + 6;

    String parentUID;
    String UID;
    String verb;
    String forOrg;

    public InventoryTaskBase(String aParentUID,
			     String myUID,
			     String aVerb,
			     String aForOrg,
			     long aStartTime, 
			     long anEndTime) {
	super(aStartTime, anEndTime);
	parentUID = aParentUID;
	UID = myUID;
	verb = aVerb;
	forOrg = aForOrg;
    }

    public String getUID() { return UID; }
    public String getParentUID() { return parentUID; }
    public String getVerb() { return verb; }
    public String getDestination() { return forOrg; }

    public String toHRString() {
	return getParentUID() + "," + getUID() + "," +
	    getVerb() + "," + getDestination() + "," +
	    new Date(startTime) + "," +  new Date(endTime);
    } 

    public String toString() {
	return super.toString() + ",ParentUID=" + getParentUID() +
	    ",UID=" + getUID() +
	    ",Verb=" + getVerb() +
	    ",For(ORG)=" + getDestination();
    }

  /**
   * Get the time in milliseconds that would be just before midnight of the next day
   *  or 1 millisecond shy of the next bucket start date.
   *
   * @return - the time in milliseconds that represents last millisecond of the bucket
   */
  protected long getEndOfPeriod(long startOfPeriod, long msecUnits, int numUnits) {
    int bucket = (int) (startOfPeriod / (msecUnits * numUnits));
    long endOfBucket = (((bucket + 1) * msecUnits * numUnits) - 1);
    return endOfBucket;
  }

}


