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
import java.util.ArrayList;
import java.util.Date;

import org.cougaar.core.service.LoggingService;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.asset.Asset;


/** 
 * <pre>
 * 
 * The LogisticsInventoryLogger is a specialized logger seperate from
 * the infrastructure logging facility that logs to files the special
 * csv files for Inventory testing.
 *
 * There is a one to one correspondance between these and the inventories
 * managed by an inventory plugin.   Most of the logging is invoked by
 * the LogisticsInventoryBG.  The LogisticsInventoryWriter nows how to
 * write to one of these streams.
 *
 * 
 * @see LogisticsInventoryWriter
 * @see LogisticsInventoryBG
 *
 **/

public class LogisticsInventoryLogger extends FileWriter{

    protected static String datestamp=null;
    protected static String baseDir=null;
    protected String fileId;

    protected LoggingService logger;

    public LogisticsInventoryLogger(File aFile, 
				    boolean append, 
				    InventoryPlugin invPlugin) throws IOException {
	super(aFile,append);
	logger = invPlugin.getLoggingService(this);
    }

    public static LogisticsInventoryLogger 
	createInventoryLogger(Asset invAsset, 
			      Organization anOrg, 
			      InventoryPlugin invPlugin){
	LogisticsInventoryLogger newLogger=null;
	LoggingService classLogger = invPlugin.getLoggingService(LogisticsInventoryLogger.class);
	initializeClass(classLogger);
	//Initialize the file to COUGAAR_WORKSPACE\inventory\organizationid\datestamp\NSNinv.csv
	String orgId = anOrg.getItemIdentificationPG().getItemIdentification();
	String pathId = baseDir + File.separator + "inventory" + File.separator + orgId + File.separator + datestamp;
	String fileId = pathId + File.separator + invAsset.getItemIdentificationPG().getItemIdentification() + "inv.csv";
	File csvFile = new File(fileId);
	try {
	    csvFile.mkdirs();
	    newLogger = new LogisticsInventoryLogger(csvFile,false,invPlugin);
	}
	catch(Exception e) {
	    classLogger.error("Error creating csv file " + fileId, e);
	}	    

	return newLogger;

    }


    protected void finalize() throws Throwable {
	flush();
	close();
	super.finalize();
    }

    private static void initializeClass(LoggingService classLogger) {
	if(datestamp == null) {
	    GregorianCalendar now = new GregorianCalendar();
	    datestamp = "" + now.get(Calendar.YEAR);
	    datestamp = datestamp + now.get(Calendar.MONTH) + now.get(Calendar.DAY_OF_MONTH);
	    datestamp = datestamp + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE);
	    baseDir = System.getProperty("org.cougaar.workspace");
	    if((baseDir == null) ||
	       (baseDir.equals(""))) {
		classLogger.error("System property org.cougaar.workspace is null, please set cougaar workspace");
		baseDir = ".";
	    }
	}
    }

}


    
  
  
