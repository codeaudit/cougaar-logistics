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
package org.cougaar.mlm.ui.grabber.validator;

import  org.cougaar.mlm.ui.grabber.logger.Logger;
import  org.cougaar.mlm.ui.grabber.config.DBConfig;
import  org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import  org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * Cargo, broken down by asset class
 *
 * @since 2/26/01
 **/
public class UnitMilvansTest extends UnitCargoAmountTest{
  
  //Constants:
  ////////////

  //Variables:
  ////////////

  boolean groupByUnit;
  protected boolean debug = 
	"true".equals (System.getProperty("org.cougaar.mlm.ui.grabber.validator.UnitMilvansTest.debug", "false"));

  //Constructors:
  ///////////////

  public UnitMilvansTest(DBConfig dbConfig, boolean g){
    super(dbConfig, false);
	groupByUnit = g;
  }

  //Members:
  //////////

  /**for gui**/
  public String getDescription(){
    return "Number of milvans " + getSuffix ();
  }

  /**Base name**/
  protected String getRawTableName(){
    return "Unit"+getSuffix()+"MilvansAmount";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit","Milvans Moved"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_INT};
    return types;
  }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
		    COL_NUMASSETS+" INTEGER NOT NULL"+
		    ")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;
    
    try {
      sql = getQuery(run);
      if (l.isMinorEnabled()) {
	l.logMessage(Logger.MINOR,Logger.DB_WRITE,"doing query " + sql);
      }
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoAmountTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      if (!rs.next()) {
	if (l.isMinorEnabled()) {
	  l.logMessage(Logger.MINOR,Logger.DB_WRITE,"no rows returned from sql " + sql);
	}
	insertRow(l,s,run,"All Units",0);
      }
      else {
	if (l.isMinorEnabled()) {
	  l.logMessage(Logger.MINOR,Logger.DB_WRITE,"some rows returned from sql " + sql);
	}
	do {
	  String owner;
	  int count;
		  
	  if (groupByUnit) {
	    owner = rs.getString(1);
	    count = rs.getInt(2);
	  }
	  else {
	    owner = "All Units";
	    count = rs.getInt(1);
	  }
		
	  if (l.isTrivialEnabled()) {
	    l.logMessage(Logger.TRIVIAL,Logger.DB_WRITE,"inserting row, owner " + owner + " count " + count);
	  }
	  insertRow(l,s,run,owner,count);
	} while(rs.next());
      }
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitMilvansTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String owner = DGPSPConstants.COL_OWNER;
    String aggnum = DGPSPConstants.COL_AGGREGATE;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoNomen = protoTable+"."+DGPSPConstants.COL_ALP_NOMENCLATURE;

    String sqlQuery =
      "select "+ (groupByUnit ? (owner+", ") : "") + "sum("+aggnum+")\n"+
      "from "+assetTable+", "+protoTable+"\n"+
      "where "+instProtoid+" = "+protoProtoid+"\n"+
      "and "+protoNomen+" like '" + getMatchString() + "'"+
      (groupByUnit ? ("group by " + owner) : "");

    if (debug)
	  System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  protected String getMatchString () { return "MILVAN%"; }
  
  protected String getSuffix () { return ((groupByUnit) ? "by_unit" : ""); }

  //InnerClasses:
  ///////////////
}





