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

/**
 * Calculates total # people or cargo moved
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/
public class TotalPeopleTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////
  boolean people;
  String COL_TOTAL;

  protected boolean debug = 
	"true".equals (System.getProperty("org.cougaar.mlm.ui.grabber.validator.TotalPeopleTest.debug", "false"));
  
  //Constructors:
  ///////////////

  public TotalPeopleTest(DBConfig dbConfig, boolean people){
    super(dbConfig);
    this.people = people;
	COL_TOTAL = "total" + getSuffix();
	
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Total " + getSuffix();
  }

  /**Base name**/
  protected String getRawTableName(){
    return "total" + getSuffix();
  }
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String aggnum = DGPSPConstants.COL_AGGREGATE;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;
    String personConstant = (new Integer(DGPSPConstants.ASSET_CLASS_PERSON)).toString();

    String sqlQuery =
      "select sum("+aggnum+")\n"+
      "from "+assetTable+", "+protoTable+"\n"+
      "where "+assetClass+(people?" = ":" <> ")+personConstant+"\n"+
      "and "+instProtoid+" = "+protoProtoid;

    if (debug)
	  System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  private void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
					COL_TOTAL+" INTEGER NOT NULL"+
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
				   "TotalPeopleTest.insertResults - Problem executing query : " + sql,
				   sqle);
	}

	try {
	  if (rs != null) {
		while(rs.next()){
		  insertRow (l, s, run, rs.getInt (1));
		}
	  }
	} catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"TotalPeopleTest.insertResults - Problem walking results.",sqle);
	}
  }

	private void insertRow(Logger l, Statement s, int run, int totalPeople) throws SQLException {
	  String sql = null;
	  try {
		StringBuffer sb=new StringBuffer();
		sb.append("INSERT INTO ");
		sb.append(getTableName(run));
		sb.append(" (");
		sb.append(COL_TOTAL);
		sb.append(") VALUES(");
		sb.append(totalPeople);
		sb.append(")");
		sql = sb.toString();
		s.executeUpdate(sql);
	  } catch (SQLException sqle) {
		l.logMessage(Logger.ERROR,Logger.DB_WRITE,
					 "TotalPeopleTest.insertRow - Problem inserting rows in table. Sql was " + sql,
					 sqle);
	  }
	}
  
  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Total " + getSuffix () + " moved"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_INT};
    return types;
  }

  protected String getSuffix () { return ((people) ? "people" : "cargo"); }
}


