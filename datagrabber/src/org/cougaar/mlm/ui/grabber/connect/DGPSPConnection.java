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
  protected RunResult prepResult(DeXMLable obj){
    setStatus("Starting");
    RunResult rr = new SuccessRunResult(getID(),getRunID());
    setStatus("Done");
    logMessage(Logger.MINOR,Logger.RESULT,"Produced Result");
    return rr;
  }

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

    protected void appendQueryParams(StringBuffer sb, int count) {
        for (int i=0; i<count; i++) {
            sb.append("?,");
        }
    }
}
