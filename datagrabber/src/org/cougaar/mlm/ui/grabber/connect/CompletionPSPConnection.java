/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
