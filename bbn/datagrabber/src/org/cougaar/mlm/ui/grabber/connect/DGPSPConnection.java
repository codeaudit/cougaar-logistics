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
import org.cougaar.mlm.ui.psp.transit.data.registration.Registration;
import org.cougaar.mlm.ui.psp.transit.data.registration.RegistrationFactory;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DataGathererPSPConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

import java.sql.*;

/**
 * Base class for connections to the DataGrathererPSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/18/01
 **/
public abstract class DGPSPConnection extends PSPConnection {

  //Constants:
  ////////////

  //Variables:
  ////////////

  protected DataGathererPSPConfig pspConfig;

  //Constructors:
  ///////////////

  public DGPSPConnection(int id, int runID,
			 DataGathererPSPConfig pspConfig, 
			 DBConfig dbConfig,
			 Connection c,
			 Logger l){
    super(id, runID, pspConfig.getUrlConnection(), dbConfig, c,l);
    this.pspConfig=pspConfig;
  }

  //Members:
  //////////

  //Gets:

  protected String getQueryString(){
    return "?"+pspConfig.getQuery()+getSessionID();
  }

  protected String getSessionID(){
    String ses=pspConfig.getSessionID();
    if(ses==null||ses.equals(""))
      return "";
    return "?sessionId="+ses;
  }

  protected String getFileName(){
    return urlConnectData.getFileName()+
      getClusterName()+"_"+
      pspConfig.getQuery()+".xml";
  }

  //Actions:
  
  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
