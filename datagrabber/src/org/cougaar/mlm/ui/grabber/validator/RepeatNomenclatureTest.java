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

import java.util.*;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Looks for prototypes with repeated ALP nomenclature
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class RepeatNomenclatureTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public RepeatNomenclatureTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_WARNING;
  }

  /**for gui**/
  public String getDescription(){
    return "Prototypes with repeated names.";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "RepeatedName";
  }
  
  private String getQuery (int run) {
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String abbrev1 = "p1";
    String abbrev2 = "p2";
    String pPrototype1 = abbrev1+"."+DGPSPConstants.COL_PROTOTYPEID;
    String pNomenclature1 = abbrev1+"."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String pPrototype2 = abbrev2+"."+DGPSPConstants.COL_PROTOTYPEID;
    String pNomenclature2 = abbrev2+"."+DGPSPConstants.COL_ALP_NOMENCLATURE;

    String sqlQuery =
      "select "+pPrototype1+", "+pNomenclature1+"\n"+
      "from "+protoTable+" "+abbrev1+"\n"+
      "order by "+pNomenclature1;

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
					DGPSPConstants.COL_PROTOTYPEID+" VARCHAR(255) NOT NULL,"+
					DGPSPConstants.COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL"+
					")");
  }

    /** keeps track of duplicates in a map, 
     * then if any nomen key maps to a list with more than one prototype, these get inserted as rows
     */
  protected void insertResults (Logger l, Statement s, int run) {
    String prototype, name = null;
    ResultSet rs=null;
    String sql = null;
    
    try {
      sql = getQuery(run);

      if (l.isMinorEnabled()) {
	  l.logMessage(Logger.MINOR,Logger.DB_WRITE,".insertResults - sql was " + sql);
      }
	  
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "RepeatNomenclatureTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    
    try {
	Map nameToProto = new HashMap ();

      while(rs.next()){
	prototype = rs.getString(1);
	name = rs.getString(2);

	List protoList = null;
	if ((protoList = (List) nameToProto.get (name)) == null) {
	    nameToProto.put (name, (protoList=new ArrayList()));
	}

	protoList.add (prototype);
      }	

      List keys = new ArrayList(nameToProto.keySet());
      Collections.sort (keys);

      for (Iterator iter = keys.iterator(); iter.hasNext(); ){
	  String foundName = (String) iter.next();
	  List protos = (List) nameToProto.get(foundName);
	  if (protos.size () > 1) {
	      for (Iterator iter2 = protos.iterator(); iter2.hasNext(); ) 
		  insertRow (l, s, run, (String) iter2.next(), foundName);
	  }
      }

    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "RepeatNomenclatureTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  private void insertRow(Logger l, Statement s, int run, String prototype, String name) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_PROTOTYPEID);sb.append(",");
      sb.append(DGPSPConstants.COL_ALP_NOMENCLATURE);
      sb.append(") VALUES('");
      sb.append(prototype);sb.append("','");
      sb.append(name);
      sb.append("')");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "RepeatNomenclatureTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }
  
  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Prototype","Nomenclature"};
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


