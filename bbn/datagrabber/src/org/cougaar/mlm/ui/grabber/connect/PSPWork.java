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

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Work;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.FailureRunResult;

import java.sql.*;

/**
 * Base class for all Work tasks for handling PSPs.
 *
 * Subclasses should call setStatus() when appropriate.
 * Also sublcasses should call haltForError(...) and return from 
 * overridden function if they encounter an unrecoverable error
 *
 * If you perform a lengthy or blockable computation in an overriden function,
 * make sure you put in "<code>if(halt)return getFailureResult();</code>" 
 * every so often. We do it this way, instead of exception throwing to 
 * enable a different thread to stop us if need be (that is we check to
 * see if we should be stopped).
 *
 * This method might seem somewhat convoluted (i.e. isn't that what exceptions
 * are for?), but we want to enable another thread to call halt() on this 
 * one and have this thread clean up and stop, so we need the halt checks.
 *
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/01/01
 **/
public abstract class PSPWork implements Work, Logger{

  //Constants:
  ////////////
  
  public static final int PENDING=0;
  public static final int INITIALIZING=1;
  public static final int PERFORMING=2;
  public static final int PREP_RESULT=3;

  public static final int POST_QUERY=4;
  public static final int CONNECTING=5;
  public static final int STREAMING=6;
  public static final int CHECKING_DB=7;  
  public static final int PREP_DB=8;
  public static final int UPDATINGDB=9;

  public static final String[] EPOCHS={"Pending",
				       "Initializing",
				       "Performing",
				       "Preparing Result",
				       "Posting Query",
				       "Connecting",
				       "Streaming",
				       "Checking DB",
				       "Preparing DB",
				       "Updating DB"};

  //Variables:
  ////////////

  protected DBConfig dbConfig;
  
  /**if you perform a long calculation, periodically check to see if this
   * is set, and if so, clean up and return
   **/
  protected boolean halt=false;

  /**
   * This is set if an unrecoverable error has occured
   **/
  private boolean error=false;
  private int id;
  private int runID;
  private int epoch=PENDING;
  private String status=null;
  protected Logger logger=null;
  private Connection dbConnection;

  //Constructors:
  ///////////////

  public PSPWork(int id, int runID, 
		 DBConfig dbConfig,
		 Connection dbConnection,
		 Logger logger){
    this.id=id;
    this.runID=runID;
    this.dbConfig=dbConfig;
    this.dbConnection=dbConnection;
    this.logger=logger;
  }

  //Members:
  //////////

  //Abstract members:

  /**
   * Do the operation and return a hint for preparing the result<BR>
   *
   * Note that in this code we are explicitly testing for a halt condition
   * rather than using exception processing so that some other thread
   * could halt us.  We must leave state consistent after a halt is called
   * and return null as quickly as possible.
   **/
  protected abstract Object perform();
  
  /**Prepare the result object based on the hint**/
  protected abstract RunResult prepResult(Object hint);

  //Gets:

  public int getRunID(){
    return runID;
  }

  protected Connection getDBConnection(){
    return dbConnection;
  }

  public String getName(){
    String name = getClass().getName();
    int loc = name.lastIndexOf('.');
    if(loc==-1)
      return name + "["+Integer.toString(id)+"]";
    return name.substring(loc+1) + "["+Integer.toString(id)+"]";
  }

  protected Result getFailureResult(){
    return new FailureRunResult(id,runID,getStatus(),error);
  }

  /**provides a table name that includes the run number from the generic
   * name
   **/
  protected String getTableName(String baseTableName){
    return dbConfig.getDBTableName(Controller.
				   getTableName(baseTableName,runID));
  }

  //Sets:

  protected void setStatus(String s){
    status=s;
  }

  protected void setEpoch(int e){
    epoch=e;
  }

  //Actions:

  /**helper that halts and sets status at once**/
  public void haltForError(String status){
    setStatus(status);
    error=true;
    halt();
  }

  /**helper that halts, sets status and logs a message at once**/
  public void haltForError(int type, String status){
    logMessage(Logger.ERROR,type,status);
    haltForError(status);
  }

  /**helper that halts, sets status and logs a message at once**/
  public void haltForError(int type, String status, Exception e){
    logMessage(Logger.ERROR,type,status,e);
    haltForError(status);
  }

  //Actions from perform():
  
  protected void initialize(){
  }
  
  //From Logger Interface:

  public void logMessage(int severity, int type, String message){
    logger.logMessage(severity,type,getName()+": "+message);
  }

  public void logMessage(int severity, int type, String message, Exception e){
    logger.logMessage(severity,type,getName()+": "+message,e);
  }

  //From Work interface:
  
  public int getID(){
   return id;
  }

  public String getStatus(){
    return getName() + (halt?" |Halt| ":" | ") + 
      EPOCHS[epoch] + (status==null?"":(" | "+status));
  }

  /**Note that in this code we are explicitly testing for a halt condition
   * rather than using exception processing so that some other thread
   * could halt us.  We must leave state consistent after a halt is called
   **/
  public void halt(){
    halt=true;
  }

  /**Note that in this code we are explicitly testing for a halt condition
   * rather than using exception processing so that some other thread
   * could halt us.  We must leave state consistent after a halt is called
   **/
  public Result perform(Logger l){
    //Note we are ignoring the passed logger, and using the one we already
    //have...
    setEpoch(INITIALIZING);
    initialize();
    if(halt)return getFailureResult();
    
    setEpoch(PERFORMING);
    Object obj=perform();
    if(halt)return getFailureResult();
    
    setEpoch(PREP_RESULT);
    return prepResult(obj);
  }

  public boolean isWarningEnabled   () { return true; }
  public boolean isImportantEnabled () { return true; }
  public boolean isNormalEnabled    () { return true; }
  public boolean isMinorEnabled     () { return true; }
  public boolean isTrivialEnabled   () { return true; }

  //InnerClasses:
  ///////////////
}
