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

import org.cougaar.mlm.ui.grabber.validator.Graphable;

/**
 * Looks for missing people
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class UnitCargoAmountTest extends Test implements Graphable {
  
  //Constants:
  ////////////
  static final String COL_NUMASSETS = "numberassets";

  //Variables:
  ////////////

    boolean people;

  //Constructors:
  ///////////////

  public UnitCargoAmountTest(DBConfig dbConfig, boolean people){
    super(dbConfig);
    this.people = people;
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return (people?"Number":"Amount") + " of "+(people?"people":"cargo")+" per Unit";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "Unit"+(people?"People":"Cargo")+"Amount";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit","AssetsMoved"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_INT};
    return types;
  }

  /** methods for interface Graphable */
  public int getXAxisColumn () { return 1; }
  public int getYAxisColumn () { return 2; }
  public int getZAxisColumn () { return -1; }
  public boolean hasThirdDimension () { return false; }

  /** 
   * if the third dimension is too wide, e.g. asset types, 
   * show one graph per X dimension entry 
   **/
  public boolean showMultipleGraphs () { return false; }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    createTable(s, run);
    insertResults(l,s,run);
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
	String owner = rs.getString(1);
	int count = rs.getInt(2);
	insertRow(l,s,run,owner,count);
      }    
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoAmountTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String owner = DGPSPConstants.COL_OWNER;
    String aggnum = DGPSPConstants.COL_AGGREGATE;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;
    String personConstant = (new Integer(DGPSPConstants.ASSET_CLASS_PERSON)).toString();

    String sqlQuery =
      "select "+owner+", sum("+aggnum+")\n"+
      "from "+assetTable+", "+protoTable+"\n"+
      "where "+assetClass+(people?" = ":" <> ")+personConstant+"\n"+
      "and "+instProtoid+" = "+protoProtoid+"\n"+
      "group by "+owner;

    System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  protected void insertRow(Logger l, Statement s, int run,
			 String owner, int count) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
      sb.append(COL_NUMASSETS);
      sb.append(") VALUES('");
      sb.append(owner);sb.append("',");
      sb.append(count);
      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoAmountTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  //InnerClasses:
  ///////////////
}





