/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import org.cougaar.planning.servlet.data.completion.CompletionData;
import org.cougaar.planning.servlet.data.completion.CompletionDataFactory;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.config.CompletionPSPConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.FailureRunResult;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

import java.sql.*;

/**
 * Connection for talking to CompletionPSP
 * 
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 3/04/01
 **/
public class CompletionPSPConnection extends PSPConnection{

  //Constants:
  ////////////
  
  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public CompletionPSPConnection(int id, int runID,
				 CompletionPSPConfig compConfig,
				 DBConfig dbConfig,
				 Connection dbConnection,
				 Logger logger){
    super(id,runID,compConfig.getUrlConnection(),
	  dbConfig, dbConnection, logger);
  }

  //Members:
  //////////

  /**update the database table based on the recieved object**/
  protected void updateDB(Connection c, DeXMLable obj){
  }

  /**Prepare the result object based on the recieved object**/
  protected RunResult prepResult(DeXMLable obj){
    return new CompletionRunResult(getID(),getRunID(),(CompletionData)obj);
  }

  /**return the DeXMLableFactory specific to this URL connection**/
  protected DeXMLableFactory getDeXMLableFactory(){
    return new CompletionDataFactory();
  }

  public String getURL(){
    String baseURL=urlConnectData.getURL();
    String separator = (usingPSPs()) ? "?" : "&";
    String queryString = 
      baseURL+(transferByXML()?"?format=xml":"?format=data")+separator+"showTables=true"+
      getQueryString();
    return queryString;
  }

  //InnerClasses:
  ///////////////

  public static class CompletionRunResult extends SuccessRunResult{
    private CompletionData completionData;

    public CompletionRunResult(int id, int runid, CompletionData cd){
      super(id,runid);
      this.completionData=cd;
    }
    public CompletionData getCompletionData(){
      return completionData;
    }
  }
}
