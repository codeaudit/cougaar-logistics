/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.  </copyright> */

package org.cougaar.logistics.plugin.inventory;

import java.io.Writer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;

/** 
 * <pre>
 * 
 * The LogisticsInventoryFormatter is a specialized writer class that 
 * produces the csv output logged by the LogisticsInventoryLogger.
 * It maintains the contract with LogisticsInventoryBG on how to 
 * write its internal data structures into the proper csv data output
 * to a provided output stream writer.   It also provides methods that will
 * wrap that output with useful header and column info, or in xml
 * for a servlet.
 * 
 * @see LogisticsInventoryLogger
 * @see LogisticsInventoryBG
 *
 **/

public class LogisticsInventoryFormatter {


    protected LoggingService logger;
    protected TaskUtils taskUtils;
    protected long cycleStamp=-0L;
    protected Writer output;

    public LogisticsInventoryFormatter(Writer writeOutput, InventoryPlugin invPlugin){
	logger = invPlugin.getLoggingService(this);
	taskUtils = invPlugin.getTaskUtils();
	output = writeOutput;
    }

    public void logToExcelOutput(ArrayList dueOuts, long aCycleStamp){
	excelLogDueOuts(dueOuts,aCycleStamp);
    }
    
    protected void logDueOuts(ArrayList dueOuts,long aCycleStamp) {
	cycleStamp = aCycleStamp;
	for(int i=0; i < dueOuts.size(); i++) {
	    ArrayList bin = (ArrayList) dueOuts.get(i);
	    //	    write("Bin #" + i);
	    for(int j=0; j < bin.size(); j++) {
		Task aDueOut = (Task) bin.get(j);
		String dueOutStr = aDueOut.getUID() + ",";
		dueOutStr = dueOutStr + aDueOut.getVerb() + ",";
		PrepositionalPhrase pp_for = aDueOut.getPrepositionalPhrase(Constants.Preposition.FOR);
		if (pp_for != null) {
		    Object org;
		    org = pp_for.getIndirectObject();
		    dueOutStr = dueOutStr + org + ",";
		}
		Date startDate = new Date(taskUtils.getStartTime(aDueOut));  
		dueOutStr = dueOutStr + startDate.toString() + ",";
		Date endDate = new Date(taskUtils.getEndTime(aDueOut));  
		dueOutStr = dueOutStr + endDate.toString() + ",";
		if(taskUtils.isSupply(aDueOut)) {
		    dueOutStr = dueOutStr + taskUtils.getQuantity(aDueOut);
		}
		//We have to get the Rate if its a projection....MWD
		write(dueOutStr);
	    }
	}
    } 


    protected void excelLogDueOuts(ArrayList dueOuts,long aCycleStamp) {
	write("DUE_OUTS:START");
	writeNoCtr("CYCLE,UID,VERB,FOR(ORG),START TIME,END TIME,QTY");
	logDueOuts(dueOuts,aCycleStamp);
	write("DUE_OUTS:END");
    } 



    public void write(String csvString) {
	writeNoCtr(cycleStamp + "," + csvString);
    }

    public void writeNoCtr(String aString) {
	if(output != null) {
	    try {
		output.write(aString);
	    }
	    catch(IOException e) {		
		logger.error("Exception trying to write to Writer: " + output, e);
	    }
	}
    }
}


    
  
  
