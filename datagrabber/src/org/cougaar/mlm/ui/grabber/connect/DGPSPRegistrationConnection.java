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
 * Handles registering for data with DataGatherer PSP
 *
 * @since 2/17/01
 **/
public class DGPSPRegistrationConnection extends DGPSPConnection {

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPRegistrationConnection(int id, int runID,
				     DataGathererPSPConfig pspConfig, 
				     DBConfig dbConfig,
				     Connection c,
				     Logger l){
    super(id, runID, pspConfig, dbConfig, c,l);
  }

  //Members:
  //////////

  //Gets:

  /** get query string -- append the flag for getting transit legs */
  protected String getQueryString() {
    String query = super.getQueryString () + (pspConfig.includeTransitLegs () ? "&transitLegs" : "");
    return query;
  }

  /**return the DeXMLableFactory specific to this URL connection**/
  protected DeXMLableFactory getDeXMLableFactory(){
    return new RegistrationFactory();
  }

  //Actions:
  
  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("DBAccess not required");
  }

  protected RunResult prepResult(DeXMLable obj){
    setStatus("Starting");
    Registration r=(Registration)obj;
    RunResult rr = new RegistrationRunResult(getID(),getRunID(),
					     r.getSessionID());
    setStatus("Done");
    logMessage(Logger.MINOR,Logger.RESULT,"Produced Result");
    return rr;
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////

  public class RegistrationRunResult extends SuccessRunResult{
    private String sessionID;
    public RegistrationRunResult(int id, int runID, String sessionID){
      super(id,runID);
      this.sessionID=sessionID;
    }
    public String getSessionID(){
      return sessionID;
    }
  }
}
