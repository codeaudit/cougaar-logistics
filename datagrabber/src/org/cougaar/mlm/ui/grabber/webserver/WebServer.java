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
package org.cougaar.mlm.ui.grabber.webserver;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.config.WebServerConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.workqueue.WorkQueue;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.workqueue.ResultHandler;
import org.cougaar.mlm.ui.grabber.controller.*;

import java.io.*;
import java.net.*;
import java.util.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple web server to provide control interface to the grabber
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/8/01
 **/
public class WebServer extends Thread implements ResultHandler,
						 HttpConstants, Logger {

  //Constants:
  ////////////

  final static int BUF_SIZE = 2048;

  //Variables:
  ////////////

  protected WebServerConfig config;
  protected DBConfig dbConfig;
  protected Logger logger;
  protected Connection dbConnection;

  protected boolean halt;

  protected WorkQueue workQ;
  protected RequestHandlerFactory requestHandlerFactory;

  private int curID=1;

  /**The grabber's controller**/
  protected Controller controller;

  //Constructors:
  ///////////////

  public WebServer(Logger l, WebServerConfig config, 
		   DBConfig dbConfig, Controller c){
    this.logger=l;
    this.config=config;
    this.dbConfig=dbConfig;
    this.halt=false;
    this.workQ=new WorkQueue(this,this);
    this.controller=c;
    this.requestHandlerFactory = new RequestHandlerFactory(controller);
  }

  //Members:
  //////////

  //DB:

  protected Connection getNewDBConnection(DBConfig dbConfig){
    Connection c = null;
    try{
      c=DriverManager.getConnection
	(dbConfig.getConnectionURL(),
	 dbConfig.getUser(),
	 dbConfig.getPassword());
    }catch(SQLException e){
      logger.logMessage(Logger.FATAL,Logger.DB_CONNECT,
		 "Could not establish DB connection",e);
      return null;
    }
    logger.logMessage(Logger.IMPORTANT,Logger.DB_CONNECT,
		      "Connection established");
    return c;
  }

  protected void closeDBConnection(){
    try{
      dbConnection.close();
    }catch(SQLException e){
      logger.logMessage(Logger.WARNING,Logger.DB_CONNECT,
		 "Could not close connection",e);
    }
  }

  /**Return true on success**/
  protected boolean initDB(){
    dbConnection=getNewDBConnection(dbConfig);
    return dbConnection!=null;
  }

  //Gets:

  public Connection getDBConnection(){
    return dbConnection;
  }

  //Actions:

  public void halt(){
    halt=true;
    workQ.haltAllWork();
  }

  public void start(){
    initDB();
    super.start();
  }

  /**Don't call this -- call Thread.start() instead!**/
  public void run(){
    ServerSocket ss=null;
    try{
      ss = new ServerSocket(config.getPort());
    }catch(IOException e){
      logMessage(Logger.ERROR,Logger.WEB_IO,
		 "Could not bind to server port: "+config.getPort(),e);
      return;
    }
    logMessage(Logger.IMPORTANT,Logger.WEB_IO,
	       "Listening on port: "+config.getPort());
    while (!halt) {
      try{
	Socket s = ss.accept();
        /* we will only block in read for this many milliseconds
         * before we fail with java.io.InterruptedIOException,
         * at which point we will abandon the connection.
         */
        s.setSoTimeout(config.getTimeout());
        s.setTcpNoDelay(true);

	//logMessage(Logger.TRIVIAL,Logger.WEB_IO,"Accepted Connection");

	HttpRequest request = new HttpRequest(curID++,s);
	request.readHeader(this);
	//logMessage(Logger.TRIVIAL,Logger.WEB_IO,"Read header");
	RequestHandler rh = 
	  requestHandlerFactory.getRequestHandler(dbConfig,
						  dbConnection,
						  config, request);
	//logMessage(Logger.TRIVIAL,Logger.WEB_IO,"Got RequestHandler");
	workQ.enque(rh);
	//logMessage(Logger.TRIVIAL,Logger.WEB_IO,"Enqued handler");
      }catch(Exception e){
	logMessage(Logger.ERROR,Logger.WEB_IO,
		   "Could not hand off to request handler",e);
      }
    }
    //closeDBConnection();
  }
  
  //From ResultHandler interface:
  public void handleResult(Result r){
    //logMessage(Logger.TRIVIAL,Logger.WEB_IO,"Handled request: "+r.getID());
  }

  //From Logger interface:
  public void logMessage(int severity, int type, String message){
    logger.logMessage(severity,type,"WebServer: "+message);
  }

  public void logMessage(int severity, int type, String message, Exception e){
    logMessage(severity,type,message + ":"+e.toString());
  }

  //InnerClasses:
  ///////////////
}