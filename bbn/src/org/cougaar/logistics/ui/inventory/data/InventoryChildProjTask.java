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

import java.util.Vector;
import org.cougaar.logistics.plugin.inventory.TimeUtils;

/** 
 * <pre>
 * 
 * The InventoryChildProjTask is the concrete class that corresponds
 * to an expanded InventoryProjTask into bucket size qty's
 * 
 *
 **/

public class InventoryChildProjTask extends InventoryProjTask {

    protected double qty;

    public InventoryChildProjTask(String aParentUID,
				  String myUID,
				  String aVerb,
				  String aForOrg,
				  double aRate,
				  double aQty,
				  long aStartTime, 
				  long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,aRate,aStartTime,anEndTime);
	qty = aQty;
    }

    public double getQty() { return qty; }

    public String toString() {
	return super.toString() + ",qty=" + getQty();
    }


    public static InventoryChildProjTask[] expandProjTask(InventoryProjTask task,
							  long bucketDuration) {
	Vector childTasks = new Vector();
	long startTime = task.getStartTime();
	long endTime = task.getEndTime();
	int ctr=1;

	double numDays = bucketDuration / TimeUtils.MSEC_PER_DAY;

	while (startTime < endTime) {
	    InventoryChildProjTask childTask = 
		new InventoryChildProjTask(task.getUID(),
					   "CHILD" + ctr,
					   task.getVerb(),
					   task.getDestination(),
					   task.getDailyRate(),
					   task.getDailyRate()*numDays,
					   startTime,
					   startTime+1);
	    childTasks.add(childTask);
	    ctr++;
	    startTime += bucketDuration;
	}

	InventoryChildProjTask[] childTaskArray = new InventoryChildProjTask[childTasks.size()];
	for(int i=0; i <childTasks.size(); i++ ) {
	    childTaskArray[i] = (InventoryChildProjTask) childTasks.elementAt(i);
	}

	return childTaskArray;
    }

}


