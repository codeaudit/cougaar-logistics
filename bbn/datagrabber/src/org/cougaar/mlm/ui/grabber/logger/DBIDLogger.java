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
package org.cougaar.mlm.ui.grabber.logger;

import org.cougaar.mlm.ui.grabber.config.DBConfig;

import java.sql.*;
import java.util.Date;

/**
 * Logs to a database table.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/01/01
 **/
public class DBIDLogger implements IDLogger{

  //Constants:
  ///////////
  public static final String LOG_TABLE_NAME="logtable";

  public static final String COL_SEQ="sequence";
  public static final String COL_TIME="time";
  public static final String COL_SEVERITY="severity";
  public static final String COL_TYPE="type";
  public static final String COL_ID="id";
  public static final String COL_MESSAGE="message";
  public static final String COL_EXCEPTION="exception";
  

  //Variables:
  ////////////

  protected int verbosityLevel=NORMAL;
  protected DBConfig dbConfig;
  protected Statement statement;
  
  private StringBuffer sb=new StringBuffer();
  private int curSeq=0;

  //Constructors:
  ///////////////

  public DBIDLogger(DBConfig dbConfig)throws SQLException{
    verbosityLevel=NORMAL;
    this.dbConfig=dbConfig;
  }

  public DBIDLogger(DBConfig dbConfig, 
		    int verbosityLevel)throws SQLException{
    this.verbosityLevel=verbosityLevel;
    this.dbConfig=dbConfig;
  }

  //Members:
  //////////

  //Gets:


  protected int getLargestSeqNumber()throws SQLException{
    int ret=0;
    ResultSet rs=statement.executeQuery("SELECT MAX("+COL_SEQ+") FROM "+
					LOG_TABLE_NAME);
    if(rs.next()){
      ret=rs.getInt(1);
    }
    return ret;
  }

  //Sets:

  protected void setCurSeqFromDB()throws SQLException{
    curSeq=getLargestSeqNumber();
  }

  public void setVerbosityLevel(int level){
    verbosityLevel=level;
  }

  //Actions:

  protected int incSeqNumber(){
    return curSeq++;
  }

  public synchronized void start(Connection dbConnection)throws SQLException{
    statement=dbConnection.createStatement();
    prepareLogTable(dbConnection);
    createIndex(COL_SEQ);
    createIndex(COL_ID);
    setCurSeqFromDB();
  }

  public void stop(){
    try{
      statement.close();
    }catch(SQLException e){
    }
  }

  //Database functions:

  protected void prepareLogTable(Connection dbConnection)
    throws SQLException{
    DatabaseMetaData meta = dbConnection.getMetaData();
    String tTypes[]={"TABLE"}; 
    ResultSet rs;
    rs = meta.getTables(null,null,
			dbConfig.getDBTableName(LOG_TABLE_NAME),tTypes);
    if(!rs.next()){ //If there is no row, then we need to make table
      statement.executeUpdate("CREATE TABLE "+LOG_TABLE_NAME+" ( "+
			      COL_SEQ+" INTEGER NOT NULL,"+
			      COL_TIME+" "+
			      dbConfig.getDateTimeType()+
			      " NOT NULL, "+
			      COL_SEVERITY+" INTEGER NOT NULL,"+
			      COL_TYPE+" INTEGER NOT NULL,"+
			      COL_ID+" INTEGER, "+
			      COL_MESSAGE+" VARCHAR(255),"+
			      COL_EXCEPTION+" VARCHAR(255))");
    }
    rs.close();
  }

  protected void createIndex(String column){
    String tableName=dbConfig.getDBTableName(LOG_TABLE_NAME);
    try{
      statement.executeUpdate("create index ix_"+
			      column+
			      " on "+
			      tableName+
			      " ("+
			      column+
			      ")");
    }catch(SQLException e){
      //This will happen if we have already created the logtable...
      System.out.println("OK: Could not create index "+tableName+"."+column);
    }
  }

  //From Logger:

  public void logMessage(int severity, int type, String message){
    logMessage(NOID,severity,type,message,null);
  }

  public void logMessage(int severity, int type, String message, Exception e){
    logMessage(NOID,severity,type,message,e);
  }

  //From IDLogger:

  public void logMessage(int id, int severity, int type, String message){
    logMessage(id,severity,type,message,null);
  }
  public synchronized void logMessage(int id, int severity, int type, 
				      String message, 
				      Exception e){
    //Now break the message up into 255 byte pieces:
    //This could be a little simpler, but this should be the fastest for the
    //the expected case of short messages:
    if(message.length()<256)
      addToTable(id,severity,type,message,e);
    else{
      String rest=message;
      while(rest.length()>255){
	String chunk=rest.substring(0,255);
	rest=rest.substring(255);
	addToTable(id,severity,type,chunk,e);
      }
      if(rest.length()>0){
	addToTable(id,severity,type,rest,e);
      }
    }
  }


  private void addToTable(int id, int severity, int type, String message,
			  Exception e){
    if(severity>verbosityLevel)
      return;
    try{
      sb.setLength(0);
      sb.append("INSERT INTO ");
      sb.append(LOG_TABLE_NAME);
      sb.append(" (");
      //Do this one with + since a good compiler will make one string out of it
      sb.append(COL_SEQ+","+COL_TIME+","+COL_SEVERITY+","+COL_TYPE+","+
		COL_ID+","+COL_MESSAGE+","+COL_EXCEPTION+") VALUES(");
      sb.append(incSeqNumber());
      sb.append(",");
      sb.append(dbConfig.dateToSQL(new Date().getTime()));
      sb.append(",");
      sb.append(severity);
      sb.append(",");
      sb.append(type);
      sb.append(",");
      if(id!=NOID){
	sb.append(Integer.toString(id));
      }else
	sb.append("NULL");
      sb.append(",");
      if(message!=null){
	sb.append("'");
	sb.append(message.replace('\'','^').replace('\'','*'));
	sb.append("'");
      }else
	sb.append("NULL");
      sb.append(",");
      if(e!=null){
	sb.append("'");
	//Need to replace quotes since this is going in SQL:
	sb.append(e.toString().replace('\'','^').replace('\'','*'));
	sb.append("'");
      }else
	sb.append("NULL");
      sb.append(")");      
      statement.executeUpdate(sb.toString());
    }catch(SQLException sqle){
      System.err.println("Error updateing DB log: "+sb.toString());
    }
  }

  //Static Members:
  /////////////////
}
