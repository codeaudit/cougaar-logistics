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


