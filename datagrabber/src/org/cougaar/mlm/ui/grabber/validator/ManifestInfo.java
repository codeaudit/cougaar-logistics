/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
/*
 * User: tom
 * Date: Aug 22, 2002
 * Time: 2:41:10 PM
 */
package org.cougaar.mlm.ui.grabber.validator;

import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Dump the contents of the manifest table.
 */
public class ManifestInfo extends Test {
  public final String COL_TOTALWEIGHT = "Weight";

  //Variables:
  ////////////

  public boolean debug = "true".equals (System.getProperty("ManifestInfo.debug", "false"));

  public ManifestInfo(DBConfig dbConfig) {
    super(dbConfig);
  }

  public String getDescription(){
    return "Milvan manifest tonnage grouped by type";
  }

  protected String getRawTableName(){
    return "ManifestInfo";
  }

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  protected void createTable(Statement s, int run) throws SQLException {
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_ALP_TYPEID+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL,"+
		    COL_TOTALWEIGHT+" DOUBLE NOT NULL" + 
		    ")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;

    try {
      sql = getQuery(run);
      rs  = s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ManifestInfo.insertResults - Problem executing query : " + sql,
		   sqle);
    }

    try {
      if (rs != null) {
	double total = 0;
	while(rs.next()){
	  double tons = rs.getDouble (3);
	  insertRow (l, s, run, rs.getString (1), rs.getString (2), tons);
	  total += tons;
	}
	insertRow (l, s, run, "All Dodics", "All Kinds - total tons", total);
      }
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ManifestInfo.insertResults - Problem walking results.",sqle);
    }
  }

  private void insertRow(Logger l, Statement s, int run, String type, String nomen, double weight) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_ALP_TYPEID);
      sb.append(",");
      sb.append(DGPSPConstants.COL_ALP_NOMENCLATURE);
      sb.append(",");
      sb.append(DGPSPConstants.COL_WEIGHT);
      sb.append(")\nVALUES(");
      sb.append("'");
      sb.append(type);
      sb.append("'");
      sb.append(",");
      sb.append("'");
      sb.append(nomen);
      sb.append("'");
      sb.append(",");

      // weight is in grams, we want short tons = 2000 pounds
      sb.append(dbConfig.getDBDouble((weight/1000000.0d)*CargoDimensionTest.METRIC_TO_SHORT_TON)); 

      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ManifestInfo.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);

    }
  }

  /** Get header strings for the table **/
  public String[] getHeaders(){
    String[] headers={"Ammo type", "nomenclature", "weight (short tons)"};
    return headers;
  }

  /** Get the types of the columns of the table **/
  public int[] getTypes(){
    int[] types={TYPE_STRING, TYPE_STRING, TYPE_TONNAGE};
    return types;
  }
  
  protected String getQuery (int run) {
    String manifestTable = Controller.getTableName(DGPSPConstants.MANIFEST_TABLE,run);

    String sqlQuery =
      "select "+
      DGPSPConstants.COL_ALP_TYPEID + ", " + 
      DGPSPConstants.COL_ALP_NOMENCLATURE + ", " + 
      "sum("+DGPSPConstants.COL_WEIGHT+")\n"+
      "from "+manifestTable + "\n" +
      "group by " + DGPSPConstants.COL_ALP_TYPEID;
	
    if (debug)
      System.out.println ("ManifestInfo.getQuery - sql was:\n" + sqlQuery);
	
    return sqlQuery;
  }
}
