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

import  org.cougaar.mlm.ui.grabber.config.DBConfig;
import  org.cougaar.mlm.ui.grabber.logger.Logger;
import  org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.Types;

/**
 * Abstract base class for data tests
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public abstract class Test{

  //Constants:
  ////////////

  public static final int RESULT_NOT_RUN=0;
  public static final int RESULT_ERROR=1;
  public static final int RESULT_WARNING=2;
  public static final int RESULT_OK=3;
  public static final int RESULT_INFO=4;

  public static final String[] RESULTS={"Not Run",
					"Error",
					"Warning",
					"Ok",
					"Info"};

  public static final int PRIORITY_CORE=0;
  public static final int PRIORITY_MISC=1;

  public static final String[] PRIORITIES={"Core",
					   "Miscellaneous"};

  public static final int TYPE_STRING=0;
  public static final int TYPE_INT=1;
  public static final int TYPE_DATETIME=2;
  public static final int TYPE_DOUBLE=3;
  public static final int TYPE_ENUM_1 = 11;
  public static final int TYPE_ENUM_2 = 12;
  public static final int TYPE_ENUM_3 = 13;
  public static final int TYPE_ENUM_4 = 14;
  public static final int TYPE_ENUM_5 = 15;

  public static final int TYPE_LEG = TYPE_STRING;
  public static final int TYPE_ASSET = TYPE_STRING;
  public static final int TYPE_ASSET_PROTO = TYPE_STRING;
  public static final int TYPE_CONV = TYPE_STRING;
  public static final int TYPE_CONV_PROTO = TYPE_STRING;
  public static final int TYPE_LOC = TYPE_STRING;
  public static final int TYPE_ORG = TYPE_STRING;
  public static final int TYPE_CONVOY = TYPE_STRING;
  public static final int TYPE_ROUTE = TYPE_STRING;
  public static final int TYPE_TONNAGE = 100; // doubles for tonnage test (no fractions)
  public static final int TYPE_TONNAGE_THREE_DIGITS = 101; // doubles for tonnage test three digits after decimal

  //Variables:
  ////////////

  protected DBConfig dbConfig;
  protected int priority = PRIORITY_MISC;

  //Constructors:
  ///////////////

  public Test(DBConfig dbConfig){
    this.dbConfig=dbConfig;
  }

  //Members:
  //////////

  //Absracts:
  //=========

  /**are we a warning or an error if we fail**/
  public abstract int failureLevel();

  /**for gui**/
  public abstract String getDescription();

  /**Base name**/
  protected abstract String getRawTableName();

  /**Actually do the query and build the table**/
  protected abstract void constructTable(Logger l, Statement s, int run)
    throws SQLException;

  /**Get header strings for the table**/
  public abstract String[] getHeaders();

  /**Get the types of the columns of the table**/
  public abstract int[] getTypes();

  //Likely to be over-ridden:
  //=========================

  /**Render enums**/
  protected String renderEnum(int whichEnum, int enum){
    return Integer.toString(enum);
  }

  /**
   * Get the result based on the table.
   * By default, any rows issue a failure at level given by failureLevel()
   **/
  protected int determineResult(Statement s, int run)
    throws SQLException{
    ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM "+getTableName(run));
    rs.next();
    if(rs.getInt(1)!=0){
      return failureLevel();
    }
    return RESULT_OK;
  }

  //Unlikely to be over-ridden:
  //===========================

  public void setPriorityLevel(int p) {
    priority = p;
  }

  public int getPriorityLevel() {
    return priority;
  }

  public String getTableName(int run){
    return dbConfig.getDBTableName(Controller.getTableName
				   ("test"+getRawTableName(),run));
  }

  public boolean tableAvailable(Logger logger, Statement s, int run){
    try{
      DatabaseMetaData meta = s.getConnection().getMetaData();
      String tTypes[]={"TABLE"}; 
      ResultSet rs;
      rs = meta.getTables(null,null,getTableName(run),tTypes);
      boolean ret=rs.next();
      rs.close();
      return ret;
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Could not determine if validation table available",e);
    }
    return false;
  }

  public void prepare(Logger logger, Statement s, int run){
	System.out.println ("Test.prepare --- entered.");
	
    boolean tableAvailable = tableAvailable(logger, s,run);
	System.out.println ("Test.prepare --- tableAvailable : " + tableAvailable);
    
    //    if(tableAvailable)
    //      dropTable (logger, s, run);
    
    if(!tableAvailable){
      try{
	System.out.println ("Test.prepare --- before Construct table.");
	logger.logMessage(Logger.NORMAL, Logger.DB_WRITE,
			  "Performing test - " + getDescription ());
	constructTable(logger,s,run);
	System.out.println ("Test.prepare --- after Construct table.");
      }catch(SQLException e){
	logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			  "Could not construct validation table.",
			  e);
      }
    }
  }

  protected void dropTable (Logger logger, Statement s, int run) {
    if(tableAvailable(logger,s,run)){
      try{
	logger.logMessage(Logger.MINOR, Logger.DB_WRITE,
			  "Dropping table " + getTableName(run));
	s.executeQuery("drop table "+getTableName(run));
      }catch(SQLException e){
	logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			  "Could not drop table "+getTableName(run),
			  e);
      }
    }
  }
  

  public int getResult(Logger logger, Statement s, int run){
    if(!tableAvailable(logger, s,run))
      return RESULT_NOT_RUN;
    try{
      return determineResult(s, run);
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Could not determine result from validation table",e);
    }
    return RESULT_NOT_RUN;
  }

  public void produceHTML(Statement s, int run, HTMLizer h){
    Logger logger=h;
    h.h2(getDescription());
    if(!tableAvailable(logger,s,run)){
      h.sFont("RED");
      h.p("Test not yet run");
      h.eFont();
      return;
    }
    try{
      h.sTable();
      //Print the headers:
      h.sRow();
      String[] headers=getHeaders();
      for(int i=0;i<headers.length;i++){
	h.tHead(headers[i]);
      }
      h.eRow();
      //Print the rest of the table:
      int types[]=getTypes();
      ResultSet rs=s.executeQuery("SELECT * FROM "+getTableName(run));
      int rows=0;
      while(rs.next()){
	rows++;
	h.sRow();
	for(int i=1;i<=types.length;i++){
	  switch(types[i-1]){
	  case TYPE_STRING:
	    h.tData(rs.getString(i));
	    break;
	  case TYPE_INT:
	    h.tData(rs.getInt(i));
	    break;
	  case TYPE_DOUBLE:
	    h.tData(rs.getDouble(i));
	    break;
	  case TYPE_TONNAGE:
	    h.tData(rs.getDouble(i), HTMLizer.NO_FRACTION_FORMAT);
	    break;
	  case TYPE_TONNAGE_THREE_DIGITS:
	    h.tData(rs.getDouble(i), HTMLizer.THREE_DIGIT_FORMAT);
	    break;
	  case TYPE_DATETIME:
	    h.tData(rs.getDate(i)+" "+rs.getTime(i));
	    break;
	  case TYPE_ENUM_1:
	  case TYPE_ENUM_2:
	  case TYPE_ENUM_3:
	  case TYPE_ENUM_4:
	  case TYPE_ENUM_5:
	    h.tData(renderEnum(types[i-1],rs.getInt(i)));
	    break;
	  default:
	    logger.logMessage(Logger.WARNING,Logger.DB_QUERY,
			      "Unknown type: "+types[i-1]);
	  }
	}
	h.eRow();
      }
      h.eTable();
      rs.close();
      h.p("Total rows: "+rows);
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Could not produce HTML from validation table",e);
    }
  }

  public boolean inKeySet(int x) {
    int[] types = getTypes();
    x--; // go from column index to array index
    if (x == (types.length-1)) return false;
    else return true;    
  }

  public static final int LESSTHAN = -1;
  public static final int GREATERTHAN = 1;
  public static final int EQUALTO = 0;
  public static final int CONFLICT = 2;

  public int linesEqual(Logger logger,
			ResultSet rs1, boolean hasLine1, 
			ResultSet rs2, boolean hasLine2,
			int columns) {
    if (!hasLine1 && !hasLine2) return EQUALTO;
    if (!hasLine1 && hasLine2) return GREATERTHAN;
    if (hasLine1 && !hasLine2) return LESSTHAN;
    
    int retval = EQUALTO;
    boolean keysEqual = true;
    for (int i = 1; i<=columns; i++) {
      int comparison = columnCompare(logger,rs1,rs2,i);
      if (inKeySet(i) && !(comparison == EQUALTO)) {
	keysEqual = false;
      }
      if (retval == EQUALTO && comparison != EQUALTO) {
	retval = comparison;
      }
    } 
    if (retval!=EQUALTO && keysEqual) retval = CONFLICT;
    return retval;
  }

  protected int columnCompare(Logger logger, ResultSet rs1, ResultSet rs2, 
			      int column) {
    int retval = EQUALTO;
    int x = 0;
    try {
      switch (rs1.getMetaData().getColumnType(column)) {
      case Types.CHAR:
      case Types.VARCHAR:
	x = ((String)rs1.getString(column)).compareTo((String)rs2.getString(column));
	if (x<0) retval=LESSTHAN; if (x>0) retval=GREATERTHAN; // normalize return value
	break;
      case Types.INTEGER:
	x = new Integer(rs1.getInt(column)).compareTo(new Integer(rs2.getInt(column)));
	if (x<0) retval=LESSTHAN; if (x>0) retval=GREATERTHAN; // normalize return value
	break;
      default:
	break;
      }
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Problems comparing columns",e);
    }
    return retval;
  }


  //InnerClasses:
  ///////////////
}
