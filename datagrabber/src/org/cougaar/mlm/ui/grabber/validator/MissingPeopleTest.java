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

/**
 * Looks for missing people
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 2/26/01
 **/
public class MissingPeopleTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public MissingPeopleTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_ERROR;
  }

  /**for gui**/
  public String getDescription(){
    return "Units without people.";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "missingpeople";
  }

  String COL_HASPEOPLE = "haspeople";
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String aPrototype = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String pPrototype = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String ownerid = DGPSPConstants.COL_OWNER;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;

    String sqlQuery =
      "select "+"distinct "+ownerid+", "+assetClass+"\n"+
      "from "+assetTable+", "+protoTable+"\n"+
      "where "+aPrototype+" = "+pPrototype+"\n"+
      "order by "+ownerid;

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
					DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
					COL_HASPEOPLE+" VARCHAR(255) NOT NULL"+
					")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
	String currentOrg, lastOrg = null;
	boolean hasPeople = false;
    ResultSet rs=null;
	String sql = null;

	try {
	  sql = getQuery(run);
	  rs=s.executeQuery(sql);
	} catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
				   "MissingPeopleTest.insertResults - Problem executing query : " + sql,
				   sqle);
	}

	try {
	  while(rs.next()){
		currentOrg = rs.getString (1);
		int assetProto = rs.getInt(2);

		if (lastOrg == null)
		  lastOrg = currentOrg;

		if (!currentOrg.equals (lastOrg)) {
		  l.logMessage(Logger.MINOR,Logger.DB_WRITE,
					   "MissingPeopleTest.insertResults - " + lastOrg + ((hasPeople) ? " yes " : " NO "));
		  if (!hasPeople)
			insertRow (l, s, run, lastOrg, hasPeople);
		  hasPeople = false;
		}
		hasPeople = hasPeople || assetProto == DGPSPConstants.ASSET_CLASS_PERSON;
		lastOrg = currentOrg;
	  }
	} catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"MissingPeopleTest.insertResults - Problem walking results.",sqle);
	}
  }

	private void insertRow(Logger l, Statement s, int run, String ownerID, boolean hasPeople) throws SQLException {
	  String sql = null;
	  try {
		StringBuffer sb=new StringBuffer();
		sb.append("INSERT INTO ");
		sb.append(getTableName(run));
		sb.append(" (");
		sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
		sb.append(COL_HASPEOPLE);
		sb.append(") VALUES('");
		sb.append(ownerID);sb.append("',");
		sb.append(((hasPeople) ? "'Yes'" : "'No'"));
		sb.append(")");
		sql = sb.toString();
		s.executeUpdate(sql);
	  } catch (SQLException sqle) {
		l.logMessage(Logger.ERROR,Logger.DB_WRITE,
					 "MissingPeopleTest.insertRow - Problem inserting rows in table. Sql was " + sql,
					 sqle);
	  }
	}
  
  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Owner Unit ID","has people"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_STRING};
    return types;
  }

  //Likely to be over-ridden:
  //=========================

  /**Render enums**/
  /*
  protected String renderEnum(int whichEnum, int enum){
    switch(whichEnum){
    case TYPE_ENUM_1:
      return enum==DGPSPConstants.ASSET_CLASS_UNKNOWN?"Unknown":
	Integer.toString(enum);
    }
    return Integer.toString(enum);
  }
  */

  //InnerClasses:
  ///////////////
}


