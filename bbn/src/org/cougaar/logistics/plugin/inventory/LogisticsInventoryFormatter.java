/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the Cougaar Open Source License as
 *  published by DARPA on the Cougaar Open Source Website
 *  (www.cougaar.org).
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
import java.util.Collection;
import java.util.Iterator;

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
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.glm.ldm.plan.QuantityScheduleElement;

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

    protected void logTasks(ArrayList tasks,
			    long aCycleStamp,
			    boolean expandTimestamp) {
	cycleStamp = aCycleStamp;
	for(int i=0; i < tasks.size(); i++) {
	    Task aTask = (Task) tasks.get(i);
	    logTask(aTask,expandTimestamp);
	}
    }

    protected void logTask(Task aTask,boolean expandTimestamp) {
	String taskStr = buildTaskPrefixString(aTask);
	taskStr = taskStr + getDateString(taskUtils.getStartTime(aTask),expandTimestamp) + ",";
	taskStr = taskStr + getDateString(taskUtils.getEndTime(aTask),expandTimestamp) + ",";
	//This is qty for supply, daily rate for projection
	taskStr = taskStr + taskUtils.getDailyQuantity(aTask);
	writeln(taskStr);
    } 
    
    protected void logAllocationResults(ArrayList tasks, long aCycleStamp, boolean expandTimestamp) {
	cycleStamp = aCycleStamp;
	for(int i=0; i < tasks.size(); i++) {
	    Task aTask = (Task) tasks.get(i);
	    logAllocationResult(aTask, expandTimestamp);
	}
    }

    protected void logAllocationResult(Task aTask, boolean expandTimestamp) {
	PlanElement pe = aTask.getPlanElement();
	if(pe == null) { return; }
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
		String outputStr = taskStr + getDateString(TaskUtils.getStartTime(ar),expandTimestamp) + ",";
		outputStr = outputStr + getDateString(TaskUtils.getEndTime(ar),expandTimestamp) + ",";
		outputStr = outputStr + TaskUtils.getQuantity(ar);
		writeln(outputStr);
	    }
	    else {
		int[] ats = ar.getAspectTypes();
		int qtyInd = getIndexForType(ats,AspectType.QUANTITY);
		int startInd = getIndexForType(ats,AspectType.START_TIME);
		int endInd = getIndexForType(ats,AspectType.END_TIME);
		Enumeration phasedResults = ar.getPhasedResults();
		while(phasedResults.hasMoreElements()) {
		    double[] results = (double[])phasedResults.nextElement();
		    String outputStr = taskStr;
		    if(startInd == -1) {
			outputStr += ",,";
		    } 
		    else {
			outputStr += getDateString(results[startInd],expandTimestamp) + ",";
		    }
		    outputStr += getDateString(results[endInd],expandTimestamp) + ",";
		    outputStr += results[qtyInd];
		    writeln(outputStr);
		}
	    }
	}
    }    


    protected void logLevels(Schedule reorderLevels,
			     Schedule inventoryLevels, 
			     long aCycleStamp, 
			     boolean expandTimestamp) {
	cycleStamp = aCycleStamp;
	Enumeration e = reorderLevels.getAllScheduleElements();
	while(e.hasMoreElements()) {
	    QuantityScheduleElement qse=(QuantityScheduleElement)e.nextElement();
	    Collection invLevelsInRange = 
		inventoryLevels.getEncapsulatedScheduleElements(qse.getStartTime(),qse.getEndTime());
	    logLevels(qse,invLevelsInRange,expandTimestamp);
	}
    }
    
    protected void logLevels(QuantityScheduleElement reorderLevel,
			     Collection invLevelsInRange,
			     boolean expandTimestamp) {

	String outputStr = getDateString(reorderLevel.getStartTime(),expandTimestamp) + ",";
	outputStr += getDateString(reorderLevel.getEndTime(),expandTimestamp) + ",";
	outputStr += reorderLevel.getQuantity() + ",";
	if(invLevelsInRange.isEmpty()) {
	    if(logger.isWarnEnabled()) {
		logger.warn("logLevel:no inventory level in range " + outputStr);
	    }
	    writeln(outputStr);
	}
	else {
	    Iterator it = invLevelsInRange.iterator();
	    boolean moreThanOne = false;
	    boolean alreadyLogged = false;
	    while(it.hasNext()) {
		QuantityScheduleElement invLevel=(QuantityScheduleElement) it.next();
		outputStr += invLevel.getQuantity();
		if(moreThanOne && !alreadyLogged) {
		    if(logger.isWarnEnabled()) {
			logger.warn("logLevel:More than one inventory level in range " + outputStr);			
		    }
		    alreadyLogged=true;
		}
		writeln(outputStr);
		moreThanOne=true;
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

    protected static String getDateString(double datetime, boolean expandTimestamp){
	return getDateString(Math.round(datetime),expandTimestamp);
    }

    protected static String getDateString(long datetime, boolean expandTimestamp) {
	if(expandTimestamp) {
	    return TimeUtils.dateString(datetime);
	}
	else {
	    return Long.toString(datetime);
	}
    }

    protected void excelLogLevels(Schedule reorderLevels,
				  Schedule inventoryLevels,
				  long aCycleStamp) {
	writeNoCycleLn("CYCLE,START TIME,END TIME,REORDER LEVEL,INVENTORY LEVEL");
	logLevels(reorderLevels,inventoryLevels,aCycleStamp,true);
    } 


    protected void excelLogProjections(ArrayList tasks,long aCycleStamp) {
	writeNoCycleLn("CYCLE,PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,DAILY RATE");
	logTasks(tasks,aCycleStamp,true);
    } 
    
    
    protected void excelLogNonProjections(ArrayList tasks,long aCycleStamp) {
	writeNoCycleLn("CYCLE,PARENT UID,UID,VERB,FOR(ORG),START TIME,END TIME,QTY");
	logTasks(tasks,aCycleStamp,true);
    } 
    

    protected void excelLogARs(ArrayList tasks,long aCycleStamp) {
	writeNoCycleLn("CYCLE,PARENT UID,TASK UID,TASK VERB,TASK FOR(ORG),AR TYPE,AR START TIME,AR END TIME,AR QTY");
	logAllocationResults(tasks,aCycleStamp,true);
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
    

    public void logToExcelOutput(ArrayList withdrawList,
				 ArrayList projWithdrawList,
				 ArrayList resupplyList,
				 ArrayList projResupplyList,
				 Schedule  reorderLevels,
				 Schedule  inventoryLevels,
				 long aCycleStamp) {
	logDemandToExcelOutput(withdrawList,projWithdrawList,aCycleStamp);
	logResupplyToExcelOutput(resupplyList,projResupplyList,aCycleStamp);
	logLevelsToExcelOutput(reorderLevels,inventoryLevels,aCycleStamp);

	try {
	    output.flush();
	}
	catch(IOException e) {
	    logger.error("Exception when trying to flush excel stream." + e.toString());
	}
    }

    protected void logDemandToExcelOutput(ArrayList withdrawList,
					  ArrayList projWithdrawList,
					  long aCycleStamp) {
	cycleStamp = aCycleStamp;
	
	ArrayList supplyList = buildParentTaskArrayList(withdrawList);
	ArrayList projSupplyList = buildParentTaskArrayList(projWithdrawList);

	writeNoCycleLn("SUPPLY TASKS:START");
	excelLogNonProjections(supplyList,aCycleStamp);
	writeNoCycleLn("SUPPLY TASKS:END");
	writeNoCycleLn("WITHDRAW TASKS:START");
	excelLogNonProjections(withdrawList,aCycleStamp);
	writeNoCycleLn("WITHDRAW TASKS:END");
	writeNoCycleLn("PROJECTSUPPLY TASKS:START");
	excelLogProjections(projSupplyList,aCycleStamp);
	writeNoCycleLn("PROJECTSUPPLY TASKS:END");
	writeNoCycleLn("PROJECTWITHDRAW TASKS:START");
	excelLogNonProjections(projWithdrawList,aCycleStamp);
	writeNoCycleLn("PROJECTWITHDRAW TASKS:END");

	writeNoCycleLn("SUPPLY TASK ALLOCATION RESULTS :START");
	excelLogARs(supplyList,aCycleStamp);
	writeNoCycleLn("SUPPLY TASK ALLOCATION RESULTS :END");
	writeNoCycleLn("WITHDRAW TASK ALLOCATION RESULTS :START");
	excelLogARs(withdrawList,aCycleStamp);
	writeNoCycleLn("WITHDRAW TASK ALLOCATION RESULTS :END");
	writeNoCycleLn("PROJECTSUPPLY TASK ALLOCATION RESULTS :START");
	excelLogARs(projSupplyList,aCycleStamp);
	writeNoCycleLn("PROJECTSUPPLY TASK ALLOCATION RESULTS :END");
	writeNoCycleLn("PROJECTWITHDRAW TASK ALLOCATION RESULTS :START");
	excelLogARs(projWithdrawList,aCycleStamp);
	writeNoCycleLn("PROJECTWITHDRAW TASK ALLOCATION RESULTS :END");

    }

    protected void logResupplyToExcelOutput(ArrayList resupplyList,
					    ArrayList projResupplyList,
					    long aCycleStamp) {
	cycleStamp = aCycleStamp;

	writeNoCycleLn("RESUPPLY SUPPLY TASKS: START");
	excelLogNonProjections(resupplyList,aCycleStamp);
	writeNoCycleLn("RESUPPLY SUPPLY TASKS: END");
	writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASKS: START");
	excelLogProjections(projResupplyList,aCycleStamp);
	writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASKS: END");

	writeNoCycleLn("RESUPPLY SUPPLY TASK ALLOCATION RESULTS: START");
	excelLogARs(resupplyList,aCycleStamp);
	writeNoCycleLn("RESUPPLY SUPPLY TASK ALLOCATION RESULTS: END");
	writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASK ALLOCATION RESULTS: START");
	excelLogARs(projResupplyList,aCycleStamp);
	writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASK ALLOCATION RESULTS: END");

    }

    protected void logLevelsToExcelOutput(Schedule reorderLevels,
					  Schedule inventoryLevels,
					  long aCycleStamp) {

	writeNoCycleLn("INVENTORY LEVELS: START");
	excelLogLevels(reorderLevels,inventoryLevels,aCycleStamp);
	writeNoCycleLn("INVENTORY LEVELS: END");
    }

    public void logToXMLOutput(Asset invAsset,
			       Organization anOrg,
			       ArrayList withdrawList,
			       ArrayList projWithdrawList,
			       ArrayList resupplyList,
			       ArrayList projResupplyList,
			       Schedule  reorderLevels,
			       Schedule  inventoryLevels,
			       long aCycleStamp) {
	cycleStamp = aCycleStamp;
	String orgId = anOrg.getItemIdentificationPG().getItemIdentification();
	String assetName = invAsset.getItemIdentificationPG().getItemIdentification();
	writeln("<INVENTORY_DUMP org=" + orgId + " item=" + assetName + ">");
	logDemandToXMLOutput(withdrawList,projWithdrawList,aCycleStamp);
	logResupplyToXMLOutput(resupplyList,projResupplyList,aCycleStamp);
	logLevelsToXMLOutput(reorderLevels,inventoryLevels,aCycleStamp);
	writeln("</INVENTORY_DUMP>");
	try {
	    output.flush();
	}
	catch(IOException e) {
	    logger.error("Exception when trying to flush xml stream." + e.toString());
	}
    }

    protected void logDemandToXMLOutput(ArrayList withdrawList,
					ArrayList projWithdrawList,
					long aCycleStamp) {

	ArrayList supplyList = buildParentTaskArrayList(withdrawList);
	ArrayList projSupplyList = buildParentTaskArrayList(projWithdrawList);

	writeNoCycleLn("SUPPLY TASKS type=TASKS");
	logTasks(supplyList,aCycleStamp,false);
	writeNoCycleLn("/SUPPLY TASKS");
	writeNoCycleLn("WITHDRAW TASKS type=TASKS");
	logTasks(withdrawList,aCycleStamp,false);
	writeNoCycleLn("/WITHDRAW TASKS");
	writeNoCycleLn("PROJECTSUPPLY TASKS type=PROJTASKS");
	logTasks(projSupplyList,aCycleStamp,false);
	writeNoCycleLn("/PROJECTSUPPLY TASKS");
	writeNoCycleLn("PROJECTWITHDRAW TASKS type=PROJTASKS");
	logTasks(projWithdrawList,aCycleStamp,false);
	writeNoCycleLn("/PROJECTWITHDRAW TASKS");

	writeNoCycleLn("SUPPLY TASK ALLOCATION RESULTS type=ALLOCATION_RESULTS");
	logAllocationResults(supplyList,aCycleStamp,false);
	writeNoCycleLn("/SUPPLY TASK ALLOCATION RESULTS");
	writeNoCycleLn("WITHDRAW TASK ALLOCATION RESULTS type=ALLOCATION_RESULTS");
	logAllocationResults(withdrawList,aCycleStamp,false);
	writeNoCycleLn("/WITHDRAW TASK ALLOCATION RESULTS");
	writeNoCycleLn("PROJECTSUPPLY TASK ALLOCATION RESULTS type=ALLOCATION_RESULTS");
	logAllocationResults(projSupplyList,aCycleStamp,false);
	writeNoCycleLn("/PROJECTSUPPLY TASK ALLOCATION RESULTS");
	writeNoCycleLn("PROJECTWITHDRAW TASK ALLOCATION RESULTS type=ALLOCATION_RESULTS");
	logAllocationResults(projWithdrawList,aCycleStamp,false);
	writeNoCycleLn("/PROJECTWITHDRAW TASK ALLOCATION RESULTS");

    }

    protected void logResupplyToXMLOutput(ArrayList resupplyList,
					  ArrayList projResupplyList,
					  long aCycleStamp) {
	cycleStamp = aCycleStamp;

	writeNoCycleLn("RESUPPLY SUPPLY TASKS type=TASKS");
	logTasks(resupplyList,aCycleStamp,false);
	writeNoCycleLn("/RESUPPLY SUPPLY TASKS");
	writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASKS type=PROJTASKS");
	logTasks(projResupplyList,aCycleStamp,false);
	writeNoCycleLn("/RESUPPLY PROJECTSUPPLY TASKS");

	writeNoCycleLn("RESUPPLY SUPPLY TASK ALLOCATION RESULTS type=ALLOCATION_RESULTS");
	excelLogARs(resupplyList,aCycleStamp);
	writeNoCycleLn("/RESUPPLY SUPPLY TASK ALLOCATION RESULTS");
	writeNoCycleLn("RESUPPLY PROJECTSUPPLY TASK ALLOCATION RESULTS type=ALLOCATION_RESULTS");
	excelLogARs(projResupplyList,aCycleStamp);
	writeNoCycleLn("/RESUPPLY PROJECTSUPPLY TASK ALLOCATION RESULTS");

    }

    protected void logLevelsToXMLOutput(Schedule reorderLevels,
					Schedule inventoryLevels,
					long aCycleStamp) {
	writeNoCycleLn("INVENTORY LEVELS type=LEVELS");
	logLevels(reorderLevels,inventoryLevels,aCycleStamp,false);
	writeNoCycleLn("INVENTORY LEVELS: END");
    }

    public void writeln(String csvString) {
	writeNoCycleLn(cycleStamp + "," + csvString);
    }

    public void writeNoCycleLn(String aString) {
	if(output != null) {
	    try {
		output.write(aString + "\n");
	    }
	    catch(IOException e) {		
		logger.error("Exception trying to write to Writer: " + output, e);
	    }
	}
    }
}


    
  
  
