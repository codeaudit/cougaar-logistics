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
package org.cougaar.mlm.ui.grabber.webserver;

import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * The data pertaining to a single HTTP request
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2//01
 **/
public class HttpRequest implements HttpConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////
  protected int id;
  protected Socket s;
  protected InputStream is;
  /** is there post data to read**/
  protected boolean postable=false;

  /**Request command**/
  protected int command=INIT;

  /**URL target name**/
  protected String target;
  
  /** The version of the conection**/
  protected String version;

  /** parameters of the header**/
  protected Map parameters;

  //Constructors:
  ///////////////

  public HttpRequest(int id, Socket s) throws IOException{
    this.id=id;
    this.s=s;
    is = new BufferedInputStream(s.getInputStream());
    parameters=new HashMap();
  }

  //Members:
  //////////
  
  //Init functions:

  private void setCommand(String s) throws Exception{
    if(s.equals("GET")) {
      command=GET;
    } else if(s.equals("HEAD")) {
      command=HEAD;
    } else {
      throw new Exception("Unsupported command: "+s);
    }
  }

  /**Don't use buffered reader, as we don't want to have unused chars in
   * the buffer
   **/
  public String readLine()throws IOException{
    StringBuffer sb=new StringBuffer();
    char c=(char)is.read();
    while(c!='\n'){
      if(c!='\r')
	sb.append(c);
      c=(char)is.read();
    }
    return(sb.toString());
  }

  public void readHeader(Logger l)throws IOException{
    String line;
    try{
      line=readLine();
      StringTokenizer strTok=new StringTokenizer(line);

      setCommand(strTok.nextToken());
      target=strTok.nextToken();
      version=strTok.nextToken();
      //now read values
      while(true){
	line=readLine();
	if(line==null) {
	  throw new Exception("Unexpected null line");
    } else if(line.equals("")) {
	  break;
	}else{	  
	  // Search for separating character
	  int slice = line.indexOf(':');
	  // Error if no separating character
	  if ( slice == -1 ) {
	    throw new Exception("Invalid HTTP header: " + line);
	  } else{
	    // Separate at the slice character into name, value
	    String name = line.substring(0,slice).trim();
	    String value = line.substring(slice + 1).trim();
	    parameters.put(name, value);
	  }
	}
      }
    }catch(Exception e){
      l.logMessage(Logger.ERROR,Logger.WEB_IO,
		   "Error while parsing HTTP header",e);
    }
  }


  //Request Functions:

  public int getID(){
    return id;
  }

  public int getCommand(){
    return command;
  }

  public String getTarget(){
    return target;
  }

  public String getVersion(){
    return version;
  }

  public String getParameter(String name){
    return (String)parameters.get(name);
  }

  public InputStream getPostStream(){
    if(postable)
      return is;
    return null;
  }
  
  public OutputStream getOutputStream()throws IOException{
    return s.getOutputStream();
  }

  public void close()throws IOException{
    is.close();
    s.close();
  }
  
  //InnerClasses:
  ///////////////
}
