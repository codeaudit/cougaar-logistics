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
import org.cougaar.mlm.ui.grabber.config.WebServerConfig;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Handles requests for files
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/8/01
 **/
public class FileRequestHandler extends RequestHandler{

  //Constants:
  ////////////

  //Variables:
  ////////////

  protected File targetFile;

  /* mapping of file extensions to content-types */
  static java.util.Hashtable map = new java.util.Hashtable();

  //Static initializer:
  /////////////////////
  static {
    fillMap();
  }

  //Constructors:
  ///////////////

  public FileRequestHandler(WebServerConfig config, 
			    HttpRequest request){
    super(config,request);
  }

  //Members:
  //////////

  protected void modifyTarget(){

    String fname = target.replace('/', File.separatorChar);
    if (fname.startsWith(File.separator)) {
      fname = fname.substring(1);
    }
    target=config.getRoot()+File.separatorChar+fname;
    targetFile=new File(target);
    if (targetFile.isDirectory()) {
      File ind = new File(targetFile, "index.html");
      if (ind.exists()) {
	targetFile = ind;
	target=targetFile.getAbsolutePath();
      }
    }
  }

  protected String getContentType(){
    if(targetFile.isDirectory()){
      return "text/html";
    }
    int ind = target.lastIndexOf(".");
    if (ind > 0)
      return (String) map.get(target.substring(ind));
    return "unknown/unknown";
  }

  protected boolean targetExists(){
    return targetFile.exists();
  }

  protected String getLastModifiedDate(){
    return new Date().toString();
  }
  
  protected void sendContent(PrintStream ps) throws IOException{
    if (targetFile.isDirectory()) {
      listDirectory(targetFile, ps);
    } else {
      sendFile(targetFile,ps);
    }
  }

  protected static void sendFile(File targ, PrintStream ps)
    throws IOException {
    InputStream is = new FileInputStream(targ.getAbsolutePath());
    try {
      int n;
      byte[] buf = new byte[2048];
      while ((n = is.read(buf)) > 0) {
	ps.write(buf, 0, n);
      }
    } finally {
      is.close();
    }
  }

  protected static void listDirectory(File dir, PrintStream ps) 
    throws IOException {
    ps.println("<TITLE>Directory listing</TITLE><P>\n");
    ps.println("<A HREF=\"..\">Parent Directory</A><BR>\n");
    String[] list = dir.list();
    for (int i = 0; list != null && i < list.length; i++) {
      File f = new File(dir, list[i]);
      if (f.isDirectory()) {
	ps.println("<A HREF=\""+list[i]+"/\">"+list[i]+"/</A><BR>");
      } else {
	ps.println("<A HREF=\""+list[i]+"\">"+list[i]+"</A><BR>");
      }
    }
    ps.println("<P><HR><BR><I>" + (new Date()) + "</I>");
  }
  
  //Static members:
  /////////////////

  static void setSuffix(String k, String v) {
    map.put(k, v);
  }
  
  static void fillMap() {
    setSuffix("", "content/unknown");
    setSuffix(".uu", "application/octet-stream");
    setSuffix(".exe", "application/octet-stream");
    setSuffix(".ps", "application/postscript");
    setSuffix(".zip", "application/zip");
    setSuffix(".sh", "application/x-shar");
    setSuffix(".tar", "application/x-tar");
    setSuffix(".snd", "audio/basic");
    setSuffix(".au", "audio/basic");
    setSuffix(".wav", "audio/x-wav");
    setSuffix(".gif", "image/gif");
    setSuffix(".jpg", "image/jpeg");
    setSuffix(".jpeg", "image/jpeg");
    setSuffix(".htm", "text/html");
    setSuffix(".html", "text/html");
    setSuffix(".text", "text/plain");
    setSuffix(".c", "text/plain");
    setSuffix(".cc", "text/plain");
    setSuffix(".c++", "text/plain");
    setSuffix(".h", "text/plain");
    setSuffix(".pl", "text/plain");
    setSuffix(".txt", "text/plain");
    setSuffix(".java", "text/plain");
  }

  //InnerClasses:
  ///////////////
}
