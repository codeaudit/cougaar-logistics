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


