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
 
package org.cougaar.logistics.ui.inventory.data;

import java.util.Date;
import java.util.Vector;
import org.cougaar.logistics.plugin.inventory.TimeUtils;    

/** 
 * <pre>
 * 
 * The InventoryChildProjAR is the concrete class that corresponds
 * to an expansion of an InventoryProjAR into bucket size buckets.
 * 
 *
 **/

public class InventoryChildProjAR extends InventoryProjAR {


    public InventoryChildProjAR(String aParentUID,
		       String myUID,
		       String aVerb,
		       String aForOrg,
		       int aResultType,
		       boolean isSuccess,
		       double aQty,
		       long aStartTime, 
		       long anEndTime) {
	super(aParentUID,myUID,aVerb,aForOrg,
	      aResultType,isSuccess,aQty,aStartTime,anEndTime);
    }


    public static InventoryChildProjAR[] expandProjAR(InventoryProjAR ar,
						      long bucketDuration) {
	Vector childARs = new Vector();
	long startTime = ar.getStartTime();
	long endTime = ar.getEndTime();
	int ctr=1;

       
	double bucketDays = bucketDuration / TimeUtils.MSEC_PER_DAY;
	double phaseDays = (endTime - startTime) / TimeUtils.MSEC_PER_DAY;

	double qtyFactor = phaseDays/bucketDays;

	if(qtyFactor < 1) qtyFactor = 1;

	while (startTime < endTime) {
	    InventoryChildProjAR childAR = 
		new InventoryChildProjAR(ar.getUID(),
					 "CHILD" + ctr,
					 ar.getVerb(),
					 ar.getDestination(),
					 ar.getResultType(),
					 ar.isSuccess(),
					 ar.getDailyRate()*qtyFactor,
					 startTime,
					 startTime+1);
	    childARs.add(childAR);
	    ctr++;
	    startTime += bucketDuration;
	}

	InventoryChildProjAR[] childARArray = new InventoryChildProjAR[childARs.size()];
	for(int i=0; i <childARs.size(); i++ ) {
	    childARArray[i] = (InventoryChildProjAR) childARs.elementAt(i);
	}

	return childARArray;
    }

}


