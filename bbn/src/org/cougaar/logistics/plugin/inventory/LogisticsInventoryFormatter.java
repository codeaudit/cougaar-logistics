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
import java.util.Enumeration;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;

import org.cougaar.glm.ldm.plan.AlpineAspectType;

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

    protected static String buildTaskPrefixString(Task aTask) {
	String taskStr = aTask.getParentTaskUID() + ",";
	taskStr = taskStr + aTask.getUID() + ",";
	taskStr = taskStr + aTask.getVerb() + ",";
	PrepositionalPhrase pp_for = aTask.getPrepositionalPhrase(Constants.Preposition.FOR);
	if (pp_for != null) {
	    Object org;
	    org = pp_for.getIndirectObject();
	    taskStr = taskStr + org + ",";
	}
	else {
	    taskStr = taskStr + ",";
	}
	return taskStr;
    }

    protected void logTasks(ArrayList tasks,long aCycleStamp) {
	cycleStamp = aCycleStamp;
	for(int i=0; i < tasks.size(); i++) {
	    Task aTask = (Task) tasks.get(i);
	    String taskStr = buildTaskPrefixString(aTask);
	    taskStr = taskStr + TimeUtils.dateString(taskUtils.getStartTime(aTask)) + ",";
	    taskStr = taskStr + TimeUtils.dateString(taskUtils.getEndTime(aTask)) + ",";
	    //This is qty for supply, daily rate for projection
	    taskStr = taskStr + taskUtils.getDailyQuantity(aTask);
	    write(taskStr);
	}
    }
    
    protected void logAllocationResults(ArrayList tasks, long aCycleStamp) {
	cycleStamp = aCycleStamp;
	for(int i=0; i < tasks.size(); i++) {
	    Task aTask = (Task) tasks.get(i);
	    PlanElement pe = aTask.getPlanElement();
	    String resultType="REPORTED";
	    AllocationResult ar = pe.getReportedResult();
	    if(ar == null) {
		resultType = "ESTIMATED";
		ar = pe.getEstimatedResult();
	    }
	    if(ar != null) {
		String taskStr = buildTaskPrefixString(aTask);
		taskStr = taskStr + resultType + ",";
		if(!ar.isPhased()) {
		    String outputStr = taskStr + TimeUtils.dateString(Math.round(TaskUtils.getStartTime(ar))) + ",";
		    outputStr = outputStr + TimeUtils.dateString(Math.round(TaskUtils.getEndTime(ar))) + ",";
		    outputStr = outputStr + TaskUtils.getQuantity(ar) + ",";
		    write(outputStr);
		}
		else {
		    int[] ats = ar.getAspectTypes();
		    int qtyInd = getIndexForType(ats,AspectType.QUANTITY);
		    int startInd = getIndexForType(ats,AspectType.START_TIME);
		    int endInd = getIndexForType(ats,AspectType.END_TIME);
		    Enumeration phasedResults = ar.getPhasedResults();
		    while(phasedResults.hasMoreElements()) {
			double[] results = (double[])phasedResults.nextElement();
			String outputStr = taskStr + TimeUtils.dateString(Math.round(results[startInd])) + ",";
			outputStr = outputStr + TimeUtils.dateString(Math.round(results[endInd])) + ",";
			outputStr = outputStr + results[qtyInd] + ",";
			write(outputStr);
		    }
		}
	    }
	}    
    }

    
    protected static int getIndexForType(int[] types, int type) {
	for (int ii = 0; ii < types.length; ii++) {
	    if (types[ii] == type) {
		return ii;
	    }
	}
	return -1;
    }

    
    protected void excelLogProjections(ArrayList tasks,long aCycleStamp) {
	writeNoCycle("CYCLE,PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,DAILY RATE");
	logTasks(tasks,aCycleStamp);
    } 
    
    
    protected void excelLogNonProjections(ArrayList tasks,long aCycleStamp) {
	writeNoCycle("CYCLE,PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,QTY");
	logTasks(tasks,aCycleStamp);
    } 
    
    protected ArrayList buildParentTaskArrayList(ArrayList tasks) {
	ArrayList parentList = new ArrayList(tasks.size());
	for(int i=0; i < tasks.size() ; i++ ) {
	    Task parentTask = ((Task)tasks.get(i)).getWorkflow().getParentTask();
	    if(parentTask != null) {
		parentList.add(parentTask);
	    }
	    else {
		logger.error("Problem deriving parent task from task");
	    }	
	}
	return parentList;
    }
    
    protected void logDemandToExcelOutput(ArrayList withdrawList,
					  ArrayList projWithdrawList,
					  long aCycleStamp) {
	cycleStamp = aCycleStamp;
	
	ArrayList supplyList = buildParentTaskArrayList(withdrawList);
	ArrayList projSupplyList = buildParentTaskArrayList(projWithdrawList);

	write("SUPPLY TASKS:START");
	excelLogNonProjections(supplyList,aCycleStamp);
	write("SUPPLY TASKS:END");
	write("WITHDRAW TASKS:START");
	excelLogNonProjections(withdrawList,aCycleStamp);
	write("WITHDRAW TASKS:END");
	write("PROJECTSUPPLY TASKS:START");
	excelLogProjections(projSupplyList,aCycleStamp);
	write("PROJECTSUPPLY TASKS:END");
	write("PROJECTWITHDRAW TASKS:START");
	excelLogNonProjections(projWithdrawList,aCycleStamp);
	write("PROJECTWITHDRAW TASKS:END");
    }

    protected void logResupplyToExcelOutput(ArrayList resupplyList,
					    ArrayList projResupplyList,
					    long aCycleStamp) {

    }

    public void write(String csvString) {
	writeNoCycle(cycleStamp + "," + csvString);
    }

    public void writeNoCycle(String aString) {
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


    
  
  
