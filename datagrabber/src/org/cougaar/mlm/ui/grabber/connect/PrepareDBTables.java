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

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;
import org.cougaar.mlm.ui.grabber.controller.FailureRunResult;

import java.sql.*;

/**
 * Base class for all Work that creates tables.
 *
 *
 * @since 2/01/01
 **/
public abstract class PrepareDBTables extends PSPWork{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public PrepareDBTables(int id, int runID, 
			 DBConfig dbConfig,
			 Connection dbConnection,
			 Logger logger){
    super(id,runID,dbConfig,dbConnection,logger);
  }

  //Members:
  //////////

  //Absract members:

  /**Determine if the database needs preparation**/
  protected abstract boolean needPrepareDB(Connection c);

  /**Prepare it, since it does**/
  protected abstract void prepareDB(Connection c);

  //Gets:

  //Sets:

  //Actions:

  /**Subclasses can use this to determine if a table is there**/
  protected boolean isTablePresent(Connection c, String tableName){
    setStatus("Determining presence of table: "+tableName);
    ResultSet rs=null;
    String tTypes[]={"TABLE"};
    boolean ret=true;
    try{
      DatabaseMetaData meta = c.getMetaData();
      try{
	rs = meta.getTables(null,null,tableName,tTypes);
	ret=rs.next(); //If there is no row, then we need to make the table;
      }finally{
	if(rs!=null)
	  rs.close();
      }
    }catch(SQLException e){
      haltForError(Logger.DB_CONNECT,"Could not determine table presence ("+
		   tableName+")");
      return false;
    }
    return ret;
  }

  /**Subclasses should use this when creating tables
   **/
  protected void prepareTable(Statement s, String tableName, String sql){
    setStatus("Creating table: "+tableName);
    try{
      s.executeUpdate(sql);
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE, 
		   "Could not create table("+tableName+")",e);
      return;
    }
    logMessage(Logger.MINOR,Logger.DB_STRUCTURE,"Created table("+
	       tableName+")"); 
  }

  /**Subclasses should use this when creating tables based on other tables
   **/
  protected void createTableSelect(Statement s, String tableName, String sql){
    setStatus("Creating table: "+tableName);
    try{
      dbConfig.createTableSelect(s,tableName,sql);
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE, 
		   "Could not create table("+tableName+")",e);
      return;
    }
    logMessage(Logger.MINOR,Logger.DB_STRUCTURE,"Created table("+
	       tableName+")"); 
  }

  /**
   * Subclasses should use this when inserting into tables based on 
   * a select from other tables
   **/
  protected void selectIntoTable(Statement s, String destTable, String destColumns, String selectSql){
    setStatus("Selecting into table: "+destTable);
    try{
      dbConfig.selectIntoTable(s,destTable,destColumns,selectSql);
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE, 
		   "Could not select into table("+destTable+")",e);
      return;
    }
    logMessage(Logger.MINOR,Logger.DB_STRUCTURE,"Selected into table("+
	       destTable+")"); 
  }

  protected void createIndex(Statement s,
			     String tableName,
			     String column){
    setStatus("Creating Index for: "+tableName+"."+column);
    long time =System.currentTimeMillis(); 
    try{
      s.executeUpdate("create index ix_"+
		      column+
		      " on "+
		      tableName+
		      " ("+
		      column+
		      ")");
      logMessage(Logger.NORMAL,Logger.DB_STRUCTURE,
		 "Index "+tableName+"."+column+" created in "+
		 (System.currentTimeMillis()-time) + " millis"); 
    }catch(SQLException e){
      logMessage(Logger.WARNING,Logger.DB_STRUCTURE,
		 "Could not create index "+tableName+"."+column,e);
    }
  }

  /**
   * Do the operation and return a hint for preparing the result<BR>
   *
   * Note that in this code we are explicitly testing for a halt condition
   * rather than using exception processing so that some other thread
   * could halt us.  We must leave state consistent after a halt is called
   * and return null as quickly as possible.
   **/
  public Object perform(){
    Connection c=getDBConnection();    
    setEpoch(CHECKING_DB);  
    if(needPrepareDB(c)){
      if(halt)return null;
      setEpoch(PREP_DB);
      prepareDB(c);
    }
    return null;
  }

  /**Prepare the result object based on the hint**/
  protected RunResult prepResult(Object hint){
    return new SuccessRunResult(getID(),getRunID());
  }

    protected String getRootTableName(){
      return getTableName(HierarchyConstants.ORGROOTS_TABLE_NAME);
    }

    protected String getOrgTableName(){
      return getTableName(HierarchyConstants.ORG_TABLE_NAME);
    }

    protected String getNamesTableName(){
      return getTableName(HierarchyConstants.ORGNAMES_TABLE_NAME);
    }

    protected String getDescendTableName(){
      return getTableName(HierarchyConstants.ORGDESCEND_TABLE_NAME);
    }

  //InnerClasses:
  ///////////////
}
