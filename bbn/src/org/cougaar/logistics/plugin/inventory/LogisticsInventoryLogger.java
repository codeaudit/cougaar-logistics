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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.service.LoggingService;




public class LogisticsInventoryLogger {

    protected static String datestamp=null;
    protected static String baseDir=null;
    protected String fileId;
    protected int logCtr;

    protected LoggingService logger;
    protected FileWriter writer;
    protected TaskUtils taskUtils;

    public LogisticsInventoryLogger(Asset invAsset, InventoryPlugin invPlugin){
	logger = invPlugin.getLoggingService(this);
	initializeClass();
	taskUtils = invPlugin.getTaskUtils();
	//Initialize the file to COUGAAR_WORKSPACE\inventory\organizationid\datestamp\NSNinv.csv
	String orgId = invPlugin.getMyOrganization().getItemIdentificationPG().getItemIdentification();
	String pathId = baseDir + File.separator + "inventory" + File.separator + orgId + File.separator + datestamp;
	String fileId = pathId + File.separator + invAsset.getItemIdentificationPG().getItemIdentification() + "inv.csv";
	File csvFile = new File(fileId);
	try {
	    csvFile.mkdirs();
	    writer = new FileWriter(csvFile,false);
	}
	catch(Exception e) {
	    logger.error("Error creating csv file " + fileId, e);
	    writer=null;
	}	    
	logCtr = 0;
    }


    protected void finalize() throws Throwable {
	if(writer != null) {
	    writer.flush();
	    writer.close();
	}
	super.finalize();
    }

    private void initializeClass() {
	if(datestamp == null) {
	    GregorianCalendar now = new GregorianCalendar();
	    datestamp = "" + now.get(Calendar.YEAR);
	    datestamp = datestamp + now.get(Calendar.MONTH) + now.get(Calendar.DAY_OF_MONTH);
	    datestamp = datestamp + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE);
	    baseDir = System.getProperty("org.cougaar.workspace");
	    if((baseDir == null) ||
	       (baseDir.equals(""))) {
		logger.error("System property org.cougaar.workspace is null, please set cougaar workspace");
		baseDir = ".";
	    }
	}
								      
    }

    public void write(String csvString) {
	if(writer != null)
	    writeNoCtr(logCtr + "," + csvString);
    }

    public void writeNoCtr(String aString) {
	if(writer != null) {
	    try {
		writer.write(aString);
	    }
	    catch(IOException e) {		
		logger.error("Exception trying to write to csvWriter: " + fileId, e);
		writer=null;
	    }
	}
    }

    public void incrementCycleCtr() {
	logCtr++;
    }


}


    
  
  
