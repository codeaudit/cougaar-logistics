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
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.io.PrintStream;
import java.io.IOException;

import java.sql.Connection;

/**
 * Returns the appropriate type of request handler for the given
 * request
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/8/01
 **/
public class RequestHandlerFactory implements HttpConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  protected Controller controller;

  //Constructors:
  ///////////////

  public RequestHandlerFactory(Controller controller){
    this.controller=controller;
  }

  //Members:
  //////////

  public RequestHandler getRequestHandler(DBConfig dbConfig,
					  Connection connection,
					  WebServerConfig config, 
					  HttpRequest request){
    //were not handling anything but HEAD & GET right now...
    int command = request.getCommand();
    if(command==HEAD)
      return new RequestHandler(config,request);
    else if(command==GET){
      String file = request.getTarget();
      
      //if it starts with "/commands" we know it is a special command:
      if(file.startsWith(config.getBaseDirectory
			 (WebServerConfig.CONTROLLER))){
	return new ControllerRequestHandler(dbConfig, connection,
					    config,request,controller);
      }else if(file.startsWith(config.getBaseDirectory
			       (WebServerConfig.VALIDATOR))){
	return new ValidatorRequestHandler(dbConfig,connection,
					   config,request);
      }else if(file.startsWith(config.getBaseDirectory
			       (WebServerConfig.COMPLETIONASSESSOR))){
	return new CompletionAssessorRequestHandler(dbConfig,connection,
						    config,request);
      }else if(file.startsWith(config.getBaseDirectory
			       (WebServerConfig.DERIVED_TABLES))){
	return new DerivedTablesRequestHandler(dbConfig,connection,
					       config,request);
      }else if(file.startsWith(config.getBaseDirectory
			       (WebServerConfig.COMPARATOR))){
	return new ComparisonRequestHandler(dbConfig,connection,
					   config,request);
      }else{
	return new FileRequestHandler(config,request);
      }
    }
    return new ErrorRequestHandler(config,request,HTTP_BAD_METHOD,
				   "unsupported method type");
  }

  //InnerClasses:
  ///////////////

  protected static class ErrorRequestHandler extends RequestHandler{
    protected int error;
    protected String desc;
    public ErrorRequestHandler(WebServerConfig config,
			       HttpRequest request,
			       int error,
			       String desc){
      super(config,request);
      this.error=error;
      this.desc=desc;
    }
    protected void printHeaders(PrintStream ps) throws IOException{
      ps.print("HTTP/1.0 " + error + " "+desc);
    }
  }
}
