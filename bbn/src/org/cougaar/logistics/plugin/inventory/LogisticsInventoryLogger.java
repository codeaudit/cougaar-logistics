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
 * </copyright>*/

package org.cougaar.logistics.plugin.inventory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
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
			      boolean doAppend,
			      InventoryPlugin invPlugin){
	LogisticsInventoryLogger newLogger=null;
	LoggingService classLogger = invPlugin.getLoggingService(LogisticsInventoryLogger.class);
	initializeClass(classLogger);
	//Initialize the file to COUGAAR_WORKSPACE\inventory\organizationid\datestamp\NSNinv.csv
	String orgId = anOrg.getItemIdentificationPG().getItemIdentification();
	orgId = orgId.replaceAll("UIC/","");
	String pathId = baseDir + File.separator + "inventory" + File.separator + orgId + File.separator + datestamp;
	String item = invAsset.getTypeIdentificationPG().getTypeIdentification();
	item = item.replaceAll("/","-");
	String fileId = pathId + File.separator + item + "inv.csv";
	File csvFile = new File(fileId);
	File pathDirs = new File(pathId);
	try {
	    pathDirs.mkdirs();
	    csvFile.createNewFile();
	    newLogger = new LogisticsInventoryLogger(csvFile,doAppend,invPlugin);
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

    private static String prependSingle(int digit) {
	if(digit < 10) {
	    return "0" + digit;
	}
	else {
	    return "" + digit;
	}
    }

    private static void initializeClass(LoggingService classLogger) {
	if(datestamp == null) {
	    //TimeZone est = TimeZone.getTimeZone("EST");
	    TimeZone gmt = TimeZone.getDefault();
	    GregorianCalendar now = new GregorianCalendar(gmt);
	    now.setTime(new Date());
	    datestamp = "" + now.get(Calendar.YEAR);
	    datestamp += prependSingle(now.get(Calendar.MONTH) + 1);
	    datestamp += prependSingle(now.get(Calendar.DAY_OF_MONTH));
	    datestamp += prependSingle(now.get(Calendar.HOUR_OF_DAY));
	    datestamp += prependSingle(now.get(Calendar.MINUTE));
	    baseDir = System.getProperty("org.cougaar.workspace");
	    if((baseDir == null) ||
	       (baseDir.equals(""))) {
		classLogger.error("System property org.cougaar.workspace is null, please set cougaar workspace");
		baseDir = ".";
	    }
	}
    }

}


    
  
  
