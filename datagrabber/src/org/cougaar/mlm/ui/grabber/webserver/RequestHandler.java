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
package org.cougaar.mlm.ui.grabber.webserver;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.config.WebServerConfig;
import org.cougaar.mlm.ui.grabber.workqueue.Work;
import org.cougaar.mlm.ui.grabber.workqueue.Result;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Base class for handlers of individual connections to the web
 * server.  By default just returns an empty document
 *
 * @since 2/8/01
 **/
public class RequestHandler implements Logger, HttpConstants, Work {

  //Constants:
  ////////////

  //Variables:
  ////////////
  
  protected boolean halt=false;
  protected Logger logger;
  protected WebServerConfig config;
  protected HttpRequest request;

  protected String target;

  //Constructors:
  ///////////////

  RequestHandler(WebServerConfig config, 
		 HttpRequest request) {
    this.config=config;
    this.request = request;
    this.target=request.getTarget();
  }
  
  //Members:
  //////////

  protected void modifyTarget(){
  }

  protected String getContentType(){
    return "unknown/unknown";
  }

  protected boolean targetExists(){
    return true;
  }

  /**should we allow this page to be cached**/
  protected boolean cacheable(){
    return true;
  }

  protected int getContentLength(){
    return -1;
  }

  protected String getLastModifiedDate(){
    return new Date().toString();
  }
  
  protected void sendContent(PrintStream ps)throws IOException{
  }

  protected void processRequest() throws IOException{
    modifyTarget();
    PrintStream ps = new PrintStream(request.getOutputStream());
    printHeaders(ps);
    if(targetExists()){
      try{
	sendContent(ps);
      }catch(Exception e){
	logMessage(Logger.ERROR,Logger.GENERIC,
		   "Exception while sending content",e);
	e.printStackTrace();
      }
    }else{
      send404(ps);
    }
  }

  protected void printHeaders(PrintStream ps) throws IOException {
    //    logMessage(Logger.TRIVIAL,Logger.WEB_IO,"Printing Headers");
    boolean ret = false;
    if (!targetExists()) {
      ps.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");
      ps.write(EOL);
      ret = false;
    }else {
      ps.print("HTTP/1.1 " + HTTP_OK+" OK");
      ps.write(EOL);
      ret = true;
    }
    ps.print("Server: Data Gatherer");
    ps.write(EOL);
    ps.print("Date: " + (new Date()));
    ps.write(EOL);
    if (ret) {
      String ct = getContentType();
      if (ct == null) {
	ct = "unknown/unknown";
      }
      ps.print("Content-type: " + ct);
      ps.write(EOL);
      
      int contLength=getContentLength();
      if (contLength>-1){
	ps.print("Content-length: "+contLength);
	ps.write(EOL);
      }
      ps.print("Last Modified: " + getLastModifiedDate());
      ps.write(EOL);
    }
    if(!cacheable()){
      ps.print("Cache-Control: no-cache");
      ps.write(EOL);
    }
    ps.write(EOL);
  }

  private void send404(PrintStream ps) throws IOException {
    ps.write(EOL);
    ps.write(EOL);
    ps.println("Not Found\n\n"+
	       "The requested resource was not found.\n");
  }


  public String getName(){
    String name = getClass().getName();
    int loc = name.lastIndexOf('.');
    if(loc==-1)
      return name;
    return name.substring(loc+1);
  }
  
  //Work interface:
  
  public int getID(){
    return request.getID();
  }

  public String getStatus(){
    return "";
  }

  public void halt(){
    halt=true;
  }

  public Result perform(Logger l){
    logger=l;
    try{
      processRequest();
      logMessage(Logger.NORMAL, Logger.WEB_IO,"Handled request: "+target);
    }catch(IOException e){
      logMessage(Logger.ERROR,Logger.WEB_IO,
		 "Exception while processing request",e);
    }finally{
      try{
	request.close();
      }catch(Exception e){
	logMessage(Logger.ERROR,Logger.WEB_IO,
		   "Unable to close request(socket)",e);
      }
    }
    return new RequestHandlerResult(getID());
  }

  //Logger interface:
  public void logMessage(int severity, int type, String message){
    if(logger==null){
      System.err.println(getName()+": No logger!");
      System.err.println(getName()+": "+message);
      return;
    }
    logger.logMessage(severity, type, getName()+":"+message);
  }

  public void logMessage(int severity, int type, String message, Exception e){
    logMessage(severity, type, message + ": "+e.toString());
  }

  //InnerClasses:
  ///////////////
  public static class RequestHandlerResult implements Result{
    protected int id;
    public RequestHandlerResult(int id){
      this.id=id;
    }
    
    public int getID(){
      return id;
    }
  }

  public boolean isWarningEnabled   () { return true; }
  public boolean isImportantEnabled () { return true; }
  public boolean isNormalEnabled    () { return true; }
  public boolean isMinorEnabled     () { return true; }
  public boolean isTrivialEnabled   () { return true; }
}
