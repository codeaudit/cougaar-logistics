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
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
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
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoAmountTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      while(rs.next()){
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
		
	insertRow(l,s,run,owner,count);
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
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;
    String personConstant = (new Integer(DGPSPConstants.ASSET_CLASS_PERSON)).toString();

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





