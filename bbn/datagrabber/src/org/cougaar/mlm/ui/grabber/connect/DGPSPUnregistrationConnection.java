/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DataGathererPSPConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;

/**
 * Handles registering for data with DataGatherer PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/17/01
 **/
public class DGPSPUnregistrationConnection extends DGPSPConnection {

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPUnregistrationConnection(int id, int runID,
				       DataGathererPSPConfig pspConfig, 
				       DBConfig dbConfig,
				       Connection c,
				       Logger l){
    super(id, runID, pspConfig, dbConfig, c,l);
  }

  //Members:
  //////////

  //Gets:

  /**return the DeXMLableFactory specific to this URL connection...
   * This class we'll never get called**/
  protected DeXMLableFactory getDeXMLableFactory(){
    return null;
  }

  //Actions:

  /**
   * Do the operation and return a hint for preparing the result<BR>
   *
   * Note that in this code we are explicitly testing for a halt condition
   * rather than using exception processing so that some other thread
   * could halt us.  We must leave state consistent after a halt is called
   * and return null as quickly as possible.
   **/
  protected Object perform(){
    //We are going to back off and try again on thrown exceptions until
    //we run out of attempts:
    int backoff=0;
    boolean success=false;
    boolean giveup=false;
    while(!success && !giveup){
      try{
	setEpoch(CONNECTING);
	InputStream inStr=null;
	if(sourceIsFile()){
	  inStr=connectFile(getFileName());
	}
	else{
	  inStr=connectURL(getURL());
	}
	try{
	  if(inStr!=null)
	    inStr.close();
	}catch(IOException e){}
	success=true;
	if(backoff>0){
	  logMessage(Logger.WARNING,Logger.NET_IO,
		     "Successfully read from PSP on the "+
		     (backoff+1) + " try.");
	}
      }catch(PSPException e){
	if(backoff<BACKOFF_TIMES.length){
	  try{
	    String message=e.getMessage()+" -- Backing off for "+
	      BACKOFF_TIMES[backoff]+" millis.";
	    logMessage(Logger.WARNING, Logger.NET_IO,
		       message,
		       e.getNestedException());
	    setStatus(message);
	    Thread.sleep(BACKOFF_TIMES[backoff]);
	  }catch(InterruptedException ie){}
	  backoff++;
	}else{
	  haltForError(Logger.NET_IO, "Giving up after "+
		       (backoff+1)+" tries: "+e.getMessage(), 
		       e.getNestedException());
	  giveup=true;
	}
      }
    }
    return null;
  }
  
  /**In this class we'll never get called**/
  protected void updateDB(Connection c, DeXMLable obj){
  }

  protected RunResult prepResult(DeXMLable obj){
    setStatus("Starting");
    RunResult rr = new SuccessRunResult(getID(),getRunID());
    setStatus("Done");
    logMessage(Logger.MINOR,Logger.RESULT,"Produced Result");
    return rr;
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
