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
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.config.DerivedTablesConfig;
import org.cougaar.mlm.ui.grabber.derived.PrepareDerivedTables;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.controller.Run;

import org.cougaar.mlm.ui.grabber.validator.HTMLizer;

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
 * Handles requests concerning preparion of derived tables
 *
 * @since 4/13/01
 **/
public class DerivedTablesRequestHandler extends DynamicRequestHandler{

  //Constants:
  ////////////

  public static final int COM_MAIN_MENU = 1;
  public static final int COM_LIST_TABLES = 2;
  public static final int COM_CREATE_TABLE = 3;
  public static final int COM_DEL_TABLE = 4;

  private static String[]COMMANDS={"unknown",
				   "main",
				   "listtables",
				   "createtable",
				   "deletetable"};

  //Variables:
  ////////////

  protected int run=-1;
  protected String table="";

  //Constructors:
  ///////////////

  public DerivedTablesRequestHandler(DBConfig dbConfig, Connection connection,
				     WebServerConfig config, 
				     HttpRequest request){
    super(dbConfig,connection,config,request);
  }

  //Members:
  //////////

  protected String getURLRun(int command){
    return getURL(command,"?run="+run);
  }

  protected String getURLRun(int command, int run){
    return getURL(command,"?run="+run);
  }

  protected String getURLRunTable(int command){
    return getURL(command,"?run="+run+"?table="+table);
  }

  protected String getURLRunTable(int command, int run, String table){
    return getURL(command,"?run="+run+"?table="+table);
  }

  //Likely overridden:

  public int getModule(){
    return WebServerConfig.DERIVED_TABLES;
  }

  /**Send the content for given command**/
  protected void sendCommandContent(HTMLizer h, 
				    Statement s, 
				    int command)
    throws SQLException,IOException{
    switch(command){
    case COM_MAIN_MENU:
      sendMainMenu(h,s);break;
    case COM_LIST_TABLES:
      sendListTables(h,s);break;
    case COM_CREATE_TABLE:
      sendCreateTable(h,s);break;
    case COM_DEL_TABLE:
      sendDeleteTable(h,s);break;
    }
  }

  protected int getCommand(String comStr){
    for(int i=0;i<COMMANDS.length;i++){
      if(comStr.endsWith(COMMANDS[i]))
	return i;
    }
    return COM_UNKNOWN;
  }

  public static String getCommandName(int command){
    if(command>=0&&command<COMMANDS.length)
      return COMMANDS[command];
    return "unknown";
  }

  protected void modifyTarget(){
    super.modifyTarget();
    run=getQueryWithPrefix("run=");
    table=getStringQueryWithPrefix("table=");
  }


  protected void sendMainMenu(HTMLizer h, Statement s)
    throws IOException, SQLException{
    header(h,"Current Runs");
    ResultSet rs=s.executeQuery("SELECT "+Controller.COL_RUNID+","+
				Controller.COL_STARTTIME+","+
				Controller.COL_ENDTIME+","+
				Controller.COL_CONDITION+" FROM "+
				dbConfig.getDBTableName(Controller.RUN_TABLE_NAME)+
				" ORDER BY "+Controller.COL_RUNID);
    h.sCenter();
    h.sTable();
    h.sRow();
    h.tHead("ID");
    h.tHead("Start Time");
    h.tHead("End Time");
    h.tHead("Status");
    h.tHead("Action");
    h.eRow();
    while(rs.next()){
      int id=rs.getInt(1);
      String startTime=rs.getDate(2)+" "+rs.getTime(2);
      String endTime=rs.getDate(3)+" "+rs.getTime(3);
      String status=Run.CONDITIONS[rs.getInt(4)];
      h.sRow();
      h.tData(id);
      h.tData(startTime);
      h.tData(endTime);
      h.tData(status);
      h.tData(h.aStr(getURLRun(COM_LIST_TABLES,id), "List Derived Tables"));
      h.eRow();
    }
    h.eTable();
    h.eCenter();
    footer(h);
  }

  protected void sendListTables(HTMLizer h, Statement s)
    throws IOException, SQLException{
    DerivedTablesConfig dtConfig=new DerivedTablesConfig();
    PrepareDerivedTables pdTables=new PrepareDerivedTables(-1,run,dbConfig,dbConnection,dtConfig,h);
    header(h,"Derived tables for run "+run);
    h.sCenter();
    h.sTable();
    h.sRow();
    h.tHead("Derived Table");
    h.tHead("Description");
    h.tHead("Current Status");
    h.tHead("Action");
    h.eRow();
    for(int i=0;i<PrepareDerivedTables.DERIVED_TABLES.length;i++){
      String tableName=PrepareDerivedTables.DERIVED_TABLES[i];
      boolean created=pdTables.isDerivedTablePresent(tableName);
      h.sRow();
      h.tData(tableName);
      h.tData(PrepareDerivedTables.DERIVED_TABLE_DESCS[i]);
      h.tData(created?"Present":"Not yet created");
      h.tData(created?h.aStr(getURLRunTable(COM_DEL_TABLE,run,tableName),
			     "Delete Table"):
	      h.popupStr(getURLRun(COM_LIST_TABLES),
			 getURLRunTable(COM_CREATE_TABLE,run,tableName),
			 "createTable",
			 "Create Table"));
      h.eRow();
    }
    h.eTable();
    h.eCenter();
    footer(h);
  }

  protected void sendCreateTable(HTMLizer h, Statement s)
    throws IOException, SQLException{
    DerivedTablesConfig dtConfig=new DerivedTablesConfig();
    dtConfig.setDoAll();
    PrepareDerivedTables pdTables=new PrepareDerivedTables(-1,run,dbConfig,dbConnection,dtConfig,h);
    header(h,"Creating table '"+table+"' for run "+run);
    pdTables.createTable(s,table);
    h.sCenter();
    h.dismissLink();
    h.eCenter();
    emptyFooter(h);
  }

  protected void sendDeleteTable(HTMLizer h, Statement s)
    throws IOException, SQLException{
    int confirm=getQueryWithPrefix("confirm=");
    if(confirm==1){
      DerivedTablesConfig dtConfig=new DerivedTablesConfig();
      PrepareDerivedTables pdTables=new PrepareDerivedTables(-1,run,dbConfig,dbConnection,dtConfig,h);
      header(h,"Deleting table '"+table+"' for run "+run);
      pdTables.dropTable(s,table);
      h.sCenter();
      h.dismissLink();
      h.eCenter();
      emptyFooter(h);
    }else{
      header(h,"Confirm deletion of '"+table+"' for run "+run);
      h.sCenter();
      h.p(h.popupStr(getURLRun(COM_LIST_TABLES),
		     getURLRunTable(COM_DEL_TABLE)+"?confirm=1",
		     "deletetable",
		     "Confirm deletion of '"+table+"' for run "+run));
      h.eCenter();
      footer(h);
    }
  }

  //Static members:
  /////////////////

  //InnerClasses:
  ///////////////
}
