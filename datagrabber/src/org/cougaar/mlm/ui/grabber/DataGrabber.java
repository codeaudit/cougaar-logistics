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
package org.cougaar.mlm.ui.grabber;

import org.cougaar.mlm.ui.grabber.logger.*;
import org.cougaar.mlm.ui.grabber.controller.*;
import org.cougaar.mlm.ui.grabber.webserver.*;
import org.cougaar.mlm.ui.grabber.config.DataGrabberConfig;
import org.cougaar.mlm.ui.grabber.config.DataGrabberConfigFactory;

import org.cougaar.planning.servlet.data.xml.DeXMLizer;
import org.cougaar.planning.servlet.data.xml.UnexpectedXMLException;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;

import java.util.Date;

import org.xml.sax.SAXException;

/**
 * Main class for the data grabber
 * @author Benjamin Lubin; last modified by: $Author: ahelsing $
 *
 * @since 2/01/01
 **/
public class DataGrabber{

  //Constants:
  ////////////

  //Variables:
  ////////////

  public IDLogger logger;

  protected Controller controller;
  protected WebServer webServer;

  protected DataGrabberConfig dgConfig;

  //Constructors:
  ///////////////

  public DataGrabber(DataGrabberConfig dgConfig){
    this.dgConfig=dgConfig;
    logger = new StdLogger(dgConfig.getVerbosity());
  }

  //Members:
  //////////

  protected String getDBDriverName(){
    return dgConfig.getDBConfig().getDriverClassName();
  }

  protected void loadDBDriver(){
    try{
      Class.forName(getDBDriverName()).newInstance();
    }catch(Exception e){
      logger.logMessage(logger.FATAL,logger.DB_CONNECT,
			"Database driver could not be loaded ("+
			getDBDriverName()+")",e);      
    }
    logger.logMessage(logger.NORMAL,logger.DB_CONNECT,
		      "Database driver loaded: "+getDBDriverName());    
  }

  // Invoke the NAI security code, if available
  private void doSecureUserAuthInit() {
    String securityUIClass = System.getProperty("org.cougaar.ui.userAuthClass");
    
    if (securityUIClass == null) {
      securityUIClass = "org.cougaar.core.security.userauth.UserAuthenticatorImpl";
    }
    
    Class cls = null;
    try {
      cls = Class.forName(securityUIClass);
    } catch (ClassNotFoundException e) {
      logger.logMessage(logger.NORMAL, logger.WEB_IO, "Not using secure User Authentication: " + securityUIClass);
    } catch (ExceptionInInitializerError e) {
      logger.logMessage(logger.IMPORTANT, logger.WEB_IO, "Unable to use secure User Authentication: " + securityUIClass + ". " + e);
    } catch (LinkageError e) {
      logger.logMessage(logger.NORMAL, logger.WEB_IO, "Not using secure User Authentication: " + securityUIClass);
    }
    
    if (cls != null) {
      try {
	cls.newInstance();
      } catch (Exception e) {
	logger.logMessage(logger.IMPORTANT, logger.WEB_IO, "Error using secure User Authentication (" + securityUIClass + "): " + e);
      }
    }
  }

  public void start(){
    logger.logMessage(logger.IMPORTANT,logger.STATE_CHANGE,
		      "DataGrabber started on "+new Date());
    logger.logMessage(logger.NORMAL,logger.DB_CONNECT,
		      "Using "+dgConfig.getDBConfig().getSyntaxString()+
		      " database SQL format");

    loadDBDriver();

    // Support HTTPS with client-cert authentication
    doSecureUserAuthInit();

    controller = new Controller(logger, dgConfig);
    controller.start();
    webServer = new WebServer(logger, 
			      dgConfig.getWebServerConfig(), 
			      dgConfig.getDBConfig(),
			      controller);
    webServer.start();
  }
  
  public static void main(String[] args){
    String configFile="GrabberConfig.xml"; 
    DataGrabberConfig dgc; 
    if(args.length>0)
      configFile=args[0];
    try{
      dgc = loadConfigFromFile(configFile);
    }catch(Exception e){
      System.out.println("Could not load config data from("+
			 configFile+"):"+e.getMessage());
      return;
    }

    DataGrabber dg= new DataGrabber(dgc);
    dg.start();
  }

  public static DataGrabberConfig loadConfigFromFile(String fileName)
    throws IOException, UnexpectedXMLException, SAXException{
    File f = new File(fileName);
    FileInputStream fIn= new FileInputStream(f);
    BufferedInputStream inStream = new BufferedInputStream(fIn);
    DeXMLizer dXML = new DeXMLizer(new DataGrabberConfigFactory());
    return (DataGrabberConfig)dXML.parseObject(inStream);
  }

  //InnerClasses:
  ///////////////
}
