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
	}catch(IOException e){e.printStackTrace();}
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
	  }catch(InterruptedException ie){ie.printStackTrace();}
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

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
