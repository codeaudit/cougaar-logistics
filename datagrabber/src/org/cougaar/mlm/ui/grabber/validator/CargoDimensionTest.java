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
 * Shows dimension of cargo types
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/
public class CargoDimensionTest extends AbstractCargoTest implements Graphable {
  
  //Constants:
  ////////////

  public static final String COL_VOLUME = "volume";

  /** 
   * These constants are from :
   *	  http://www.wrsc.usace.army.mil/ndc/metric.htm
   */
  public static final double METRIC_TO_SHORT_TON = 1.1023;
  public static final double SHORT_TON_TO_METRIC = 0.90718;
  
  //Variables:
  ////////////
  public boolean debug = "true".equals (System.getProperty("CargoDimensionTest.debug", "false"));

  //Constructors:
  ///////////////

  public CargoDimensionTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  /**for gui**/
  public String getDescription(){
    return "Cargo Dimensions";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "CargoDimensions";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Cargo Type","Height (m)","Width (m)","Depth (m)","Area (m^2)","Volume (m^3)","Weight (Short Tons)"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_DOUBLE,TYPE_DOUBLE,TYPE_DOUBLE,TYPE_DOUBLE,TYPE_DOUBLE,TYPE_DOUBLE};
    return types;
  }

  public int getYAxisColumn () { return 6; }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_HEIGHT+" DOUBLE NOT NULL,"+
		    DGPSPConstants.COL_WIDTH+" DOUBLE NOT NULL,"+
		    DGPSPConstants.COL_DEPTH+" DOUBLE NOT NULL,"+
		    DGPSPConstants.COL_AREA+" DOUBLE NOT NULL,"+
		    DGPSPConstants.COL_VOLUME+" DOUBLE NOT NULL,"+
		    DGPSPConstants.COL_WEIGHT+" DOUBLE NOT NULL"+
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
		   "CargoDimensionTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      while(rs.next()){
	String nomenclature = rs.getString(1);
	double height = rs.getDouble (2);
	double width  = rs.getDouble (3);
	double depth  = rs.getDouble (4);
	double area   = rs.getDouble (5);
	double volume = rs.getDouble (6);
	// weight is in grams, we want short tons = 2000 pounds
	double weight = (rs.getDouble (7)/1000000.0)*METRIC_TO_SHORT_TON;
		
	if (l.isMinorEnabled())
	  l.logMessage(Logger.MINOR,Logger.DB_WRITE,"CargoDimensionTest - h " + height + 
		       " w " + width +
		       " d " + depth +
		       " a " + area +
		       " v " + volume +
		       " weight " + weight + " stons");

	insertRow(l,s,run,nomenclature,height,width,depth,area,volume,weight);
      }    
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "CargoDimensionTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  protected String getQuery (int run) {
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String cccDimTable = Controller.getTableName(DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE,run);
    String nomenclature = protoTable+"."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String height = DGPSPConstants.COL_HEIGHT;
    String width  = DGPSPConstants.COL_WIDTH;
    String depth  = DGPSPConstants.COL_DEPTH;
    String weight = DGPSPConstants.COL_WEIGHT;
    String area   = DGPSPConstants.COL_AREA;
    String volume = DGPSPConstants.COL_VOLUME;
    String protoProto  = protoTable +"."+DGPSPConstants.COL_PROTOTYPEID;
    String cccDimProto = cccDimTable+"."+DGPSPConstants.COL_PROTOTYPEID;

    String sqlQuery =
      "select "+nomenclature+"," + height + "," + width + "," + depth + "," + area + "," + volume + "," + weight + "\n"+
      "from "+protoTable+", " + cccDimTable +"\n"+
      "where " + protoProto + "=" + cccDimProto + "\n" +
      "order by "+nomenclature;

    if (debug)
      System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  protected void insertRow(Logger l, Statement s, int run,
			   String nomenclature, 
			   double height, double width, double depth, 
			   double area,
			   double volume, double weight) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_ALP_NOMENCLATURE + ",");
      sb.append(DGPSPConstants.COL_HEIGHT + ",");
      sb.append(DGPSPConstants.COL_WIDTH + ",");
      sb.append(DGPSPConstants.COL_DEPTH + ",");
      sb.append(DGPSPConstants.COL_AREA + ",");
      sb.append(DGPSPConstants.COL_VOLUME + ",");
      sb.append(DGPSPConstants.COL_WEIGHT);
      sb.append(") VALUES('");
      sb.append(nomenclature + "',");
      sb.append(dbConfig.getDBDouble(height) + ",");
      sb.append(dbConfig.getDBDouble(width) + ",");
      sb.append(dbConfig.getDBDouble(depth) + ",");
      sb.append(dbConfig.getDBDouble(area) + ",");
      sb.append(dbConfig.getDBDouble(volume) + ",");
      sb.append(dbConfig.getDBDouble(weight));
      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "CargoDimensionTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  /**
   * Get the result based on the table.
   * If all the rows are the info rows (start with the bold tag)
   * then it's not a warning.
   **/
  protected int determineResult(Statement s, int run) throws SQLException {
    String sql = "SELECT * FROM "+getTableName(run);
    ResultSet rs=s.executeQuery(sql);

    boolean hitRealRow = false;
    while(rs.next() && !hitRealRow){
      String nomenclature = rs.getString(1);
      hitRealRow = !nomenclature.startsWith ("<b>");
    }
    if (hitRealRow)
      return failureLevel();

    return RESULT_OK;
  }
}





