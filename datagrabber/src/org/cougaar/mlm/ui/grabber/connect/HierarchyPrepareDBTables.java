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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.*;

/**
 * Handles creating the Hierarchy DB tables.
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 2/01/01
 **/
public class HierarchyPrepareDBTables extends PrepareDBTables 
  implements HierarchyConstants{

  //Constants:
  ////////////

  //In HierarchyConstants.java

  //Variables:
  ////////////

  //Hints on which tables need to be created:
  private boolean makeRootTable=false;
  private boolean makeOrgTable=false;
  private boolean makeNamesTable=false;
  private boolean makeDescendTable=false;
  
  //Constructors:
  ///////////////

  public HierarchyPrepareDBTables(int id, int runID,
				  DBConfig dbConfig,
				  Connection c,
				  Logger l){
    super(id, runID, dbConfig, c,l);
  }

  //Members:
  //////////


  //Actions:

  protected boolean needPrepareDB(Connection c){
    makeRootTable=!isTablePresent(c,getRootTableName());
    makeOrgTable=!isTablePresent(c,getOrgTableName());
    makeNamesTable=!isTablePresent(c,getNamesTableName());
    makeDescendTable=!isTablePresent(c,getDescendTableName());

    return makeRootTable||makeOrgTable||
      makeNamesTable||makeDescendTable;
  }

  protected void prepareDB(Connection c){
    Statement s=null;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE,"Could not create/close Statement",e);
      return;
    }
    
    if(makeRootTable)
	prepareTable(s,getRootTableName(),
		     "CREATE TABLE "+getRootTableName()+" ( "+
		     COL_ORGID+" VARCHAR(255) NOT NULL, "+
		     COL_SOCIETY+" INTEGER NOT NULL UNIQUE)");
    if(makeOrgTable)
      prepareTable(s,getOrgTableName(),
		   "CREATE TABLE "+getOrgTableName()+" ( "+
		   COL_ORGID+" VARCHAR(255) NOT NULL, "+
		   COL_RELID+" VARCHAR(255) NOT NULL, "+
		   COL_REL+" INTEGER NOT NULL)");
    if(makeNamesTable)
      prepareTable(s,getNamesTableName(),
		   "CREATE TABLE "+getNamesTableName()+" ( "+
		   COL_ORGID+" VARCHAR(255) NOT NULL UNIQUE, "+
		   COL_PRETTY_NAME+" VARCHAR(255) NOT NULL)");
    if(makeDescendTable)
      prepareTable(s,getDescendTableName(),
		   "CREATE TABLE "+getDescendTableName()+" ( "+
		   COL_ORGID+" VARCHAR(255) NOT NULL, "+
		   COL_DESCEND+" VARCHAR(255) NOT NULL)");
    try{
      s.close();
    }catch(SQLException e){
      logMessage(Logger.ERROR,Logger.DB_WRITE,"Could not close Statement",e);
    }
  }

  //Static functions:
  ///////////////////

  public static void addToRootTable(Logger l, Statement s, String tableName,
				    String orgId, int society)
    throws SQLException{
    s.executeUpdate("INSERT INTO "+tableName+" ("+
		    COL_ORGID+","+
		    COL_SOCIETY+") VALUES ('"+
		    orgId+"',"+
		    Integer.toString(society)+")");
    l.logMessage(Logger.MINOR,Logger.DB_WRITE,"Added root ("+orgId+") to "+
	       tableName); 
  }

  public static void addToOrgTable(Logger l, Statement s, String tableName,
				   String orgID, String relID,
				   int relation) 
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    sb.append("INSERT INTO ");
    sb.append(tableName);
    sb.append('(');
    sb.append(COL_ORGID);
    sb.append(',');
    sb.append(COL_RELID);
    sb.append(',');
    sb.append(COL_REL);
    sb.append(") VALUES(");
    sb.append("'");
    sb.append(orgID);
    sb.append("','");
    sb.append(relID);
    sb.append("',");
    sb.append(relation);
    sb.append(")");
    s.executeUpdate(sb.toString());
  }

  public static void addToNamesTable(Logger l, Statement s, String tableName,
				     String orgID, String prettyName) 
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    sb.append("INSERT INTO ");
    sb.append(tableName);
    sb.append('(');
    sb.append(COL_ORGID);
    sb.append(',');
    sb.append(COL_PRETTY_NAME);
    sb.append(") VALUES(");
    sb.append("'");
    sb.append(orgID);
    sb.append("','");
    sb.append(prettyName);
    sb.append("')");
    s.executeUpdate(sb.toString());
  }

  public static void addToDescendTable(Logger l, Statement s, String tableName,
				       String parID, String id)
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    sb.append("INSERT INTO ");
    sb.append(tableName);
    sb.append('(');
    sb.append(COL_ORGID);
    sb.append(',');
    sb.append(COL_DESCEND);
    sb.append(") VALUES(");
    sb.append("'");
    sb.append(parID);
    sb.append("','");
    sb.append(id);
    sb.append("')");
    s.executeUpdate(sb.toString());
  }

  //InnerClasses:
  ///////////////
}
