/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
 * </copyright>
 */
 
package org.cougaar.logistics.ui.inventory.data;

import java.util.Date;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;




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

}


