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

import org.cougaar.mlm.ui.grabber.config.WebServerConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;

import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.validator.*;

import java.io.*;
import java.net.*;
import java.util.*;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import java.text.SimpleDateFormat;

/**
 * Abstract base class for Handlers for requests for commands 
 * to dynamic pages
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 3/3/01
 **/
public abstract class DynamicRequestHandler extends RequestHandler{

  //Constants:
  ////////////

  public static final int COM_UNKNOWN = 0;

  //Variables:
  ////////////

  protected DBConfig dbConfig;
  protected Connection dbConnection;
  protected int command;
  protected String getQuery;

  //Constructors:
  ///////////////

  public DynamicRequestHandler(DBConfig dbConfig, Connection connection,
			       WebServerConfig config, 
			       HttpRequest request){
    super(config,request);
    this.dbConfig=dbConfig;
    this.dbConnection=connection;
  }

  //Members:
  //////////

  //Likely overridden:

  /**module value for this class**/
  public abstract int getModule();

  /**Send the content for given command**/
  protected abstract void sendCommandContent(HTMLizer h, 
					     Statement s, 
					     int command)
    throws SQLException,IOException;

  /**@return the integer for the given command, or else COM_UNKNOWN**/
  protected abstract int getCommand(String comStr);

  /**The following static function is expected to be in all subclasses**/
  public static String getCommandName(int command){
    return "unknown";
  }
  
  //May not need to be overrriden:

  protected String getURL(int command){
    return getURL(getModule(),command);
  }

  protected String getURL(int command, String args){
    return getURL(getModule(),command,args);
  }

  protected String getURL(int module, int command){
    return getURL(module,command,"");
  }

  protected String getURL(int module, int command,String args){
    return config.getURL(module,command)+args;
  }

  protected void modifyTarget(){
    int split=target.indexOf('?');
    String comStr=target;
    getQuery="";
    if(split!=-1){
      comStr=target.substring(0,split);
      getQuery=target.substring(split+1);
    }
    command=getCommand(comStr);
  }

  protected String getContentType(){
    return "text/html";
  }

  protected boolean targetExists(){
    return command!=COM_UNKNOWN;
  }

  /**should we allow this page to be cached**/
  protected boolean cacheable(){
    return false;
  }

  protected String getLastModifiedDate(){
    return new Date().toString();
  }
  
  protected void sendContent(PrintStream ps) throws IOException{
    Statement s=getStatement(ps);
    if(s==null)
      return;
    try{
      HTMLizer h=new HTMLizer(ps);
      sendCommandContent(h,s,command);
    }catch(SQLException e){
      ps.print("Exception while processing command '"+target+"':"+e);
    }
    closeStatement(ps,s);
  }

  //Helpers:

  protected int getQueryWithPrefix(String prefix){
    StringTokenizer strTok=new StringTokenizer(getQuery,"?&");
    try{
      while(strTok.hasMoreTokens()){
	String tok=strTok.nextToken();
	if(tok.startsWith(prefix)){
	  return Integer.parseInt(tok.substring(prefix.length()));
	}
      }
    }catch(Exception e){
      return -1;
    }
    return -1;
  }

  protected String getStringQueryWithPrefix(String prefix){
    StringTokenizer strTok=new StringTokenizer(getQuery,"?&");
    try{
      while(strTok.hasMoreTokens()){
	String tok=strTok.nextToken();
	if(tok.startsWith(prefix)){
	  return tok.substring(prefix.length());
	}
      }
    }catch(Exception e){
      return "";
    }
    return "";
  }

  protected void sendInvalidQuery(HTMLizer h) throws IOException{
    header(h, "Invalid Query:"+getQuery);
    footer(h);
  }

  protected Statement getStatement(PrintStream ps)throws IOException{
    try{
      return dbConnection.createStatement();
    }catch(SQLException e){
      ps.print("Exception creating statement:"+e);
    }
    return null;
  }

  protected void closeStatement(PrintStream ps, Statement s) 
    throws IOException{
    try{
      s.close();
    }catch(SQLException e){
      ps.print("Exception closing statement:"+e);
    }
  }
  
  protected void header(HTMLizer h, String title){
    h.print("<HTML>\n<HEAD>\n"+
	    "<TITLE>"+title+"</TITLE>\n"+
	    "</HEAD>\n"+
	    "<BODY alink=\"BLUE\" vlink=\"BLUE\">\n"+
	    "<P align=right>\n"+
	    "DB Host: "+dbConfig.getHostName()+"&nbsp&nbsp&nbsp&nbsp DB Name: "+dbConfig.getDatabaseName()+"\n"+
	    "</P>\n"+
	    "<CENTER>\n"+
	    "<H1>"+config.getApplicationName()+"</H1>\n"+
	    "<H2>"+title+"</H2>\n"+
	    "</CENTER>\n");
  }

  protected void header(HTMLizer h, String title, 
			int refresh, String url){
    h.print("<HTML>\n<HEAD>\n"+
	    "<TITLE>"+title+"</TITLE>\n"+
	    "<META HTTP-EQUIV=\"Refresh\" CONTENT=\""+refresh+
	    ";URL="+url+"\">\n"+
	    "</HEAD>\n"+
	    "<BODY>\n"+
	    "<P align=right>\n"+
	    "DB Host: "+dbConfig.getHostName()+"&nbsp&nbsp&nbsp&nbsp&nbsp DB Name: "+dbConfig.getDatabaseName()+"\n"+
	    "</P>\n"+
	    "<CENTER>\n"+
	    "<H1>"+config.getApplicationName()+"</H1>\n"+
	    "<H2>"+title+"</H2>\n"+
	    "</CENTER>\n");
  }

  protected void footerSpacer(HTMLizer h){
    h.print("<BR><BR><BR>\n<HR>\n");
  }

  protected void footerMenu(HTMLizer h){
    h.print("<font size=+2>[");
    h.print(h.aStr("/","Home"));
    h.print("]</font><br>\n[");
    h.print(h.aStr(getURL(WebServerConfig.CONTROLLER,
			  ControllerRequestHandler.COM_LISTRUNS),
		   "List all runs"));
    h.print("]\n[");
    h.print(h.aStr(getURL(WebServerConfig.COMPLETIONASSESSOR,
			  CompletionAssessorRequestHandler.COM_HIT_PSP),
		   "Completion Assessor"));
    h.print("]\n[");
    h.print(h.aStr(getURL(WebServerConfig.CONTROLLER,
			  ControllerRequestHandler.COM_LISTWORK),
		   "List all active work"));
    h.print("]\n[");
    h.print(h.aStr(getURL(WebServerConfig.CONTROLLER,
			  ControllerRequestHandler.COM_LISTLOG),
		   "List complete log"));
    h.print("]\n[");
    h.print(h.aStr(getURL(WebServerConfig.VALIDATOR,
			  ValidatorRequestHandler.COM_MAIN_MENU),
		   "Validation for all runs"));
    h.print("]\n[");
    h.print(h.aStr(getURL(WebServerConfig.COMPARATOR,
			  ValidatorRequestHandler.COM_MAIN_MENU),
		   "Comparison for all runs"));
    h.print("]\n[");
    h.print(h.aStr(getURL(WebServerConfig.DERIVED_TABLES,
			  DerivedTablesRequestHandler.COM_MAIN_MENU),
		   "Derived table generation"));
    h.print("]\n");
  }

  protected void footer(HTMLizer h){
    h.print("<CENTER>\n");
    footerSpacer(h);
    footerMenu(h);
    h.print("</CENTER>\n"+
	    "</BODY>\n"+
	    "</HTML>\n");
  }

  protected void emptyFooter(HTMLizer h){
    h.print("</BODY>\n"+
	     "</HTML>\n");
  }

  protected void getSizes(Statement s, String sql, Map runToOwners, Map runToAssets) throws SQLException {
    ResultSet rs=s.executeQuery(sql);

    Set runIDs = new HashSet ();

    while(rs.next()){
      int runID=rs.getInt(1);
      runIDs.add (new Integer(runID));
    }

    String sqlBase = "select count(distinct " + 
	DGPSPConstants.COL_OWNER + "), count(*)" + 
      "\nfrom ";

    for(Iterator iter = runIDs.iterator(); iter.hasNext(); ) {
      Integer runID = (Integer) iter.next(); 
      
      String perRunSQL = sqlBase + 
	Controller.getTableName (DGPSPConstants.ASSET_INSTANCE_TABLE, runID.intValue());

      try {
	ResultSet perRunResultSet=s.executeQuery(perRunSQL);
	while(perRunResultSet.next()){
	  int owners=perRunResultSet.getInt(1);
	  int assets=perRunResultSet.getInt(2);
	  runToOwners.put (runID, new Integer(owners));
	  runToAssets.put (runID, new Integer(assets));
	}
      } catch (Exception e) {
	//System.out.println ("got exception on query\n" + perRunSQL + "\nexception was:\n" + e);
      }
    }
  }

  //Static members:
  /////////////////

  //InnerClasses:
  ///////////////
}
