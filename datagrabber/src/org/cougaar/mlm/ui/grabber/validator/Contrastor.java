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

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all the tests and knows how to kick them off
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/27/01
 *
 * Note that this class must extend Validator because Test order
 * is dictated in Validator and test ordering is important for
 * comparison in a stateless world.
 **/
public class Contrastor extends Validator{

  //Constants:
  ////////////

  public static final int RESULT_NOT_RUN=0;
  public static final int RESULT_ERROR=1;
  public static final int RESULT_WARNING=2;
  public static final int RESULT_OK=3;
  public static final int RESULT_INFO=4;
  public static final int RESULT_DIFF=5;
  public static final int RESULT_SAME=6;
  
  public static final String[] RESULTS={"Not Run",
					"Error",
					"Warning",
					"Ok",
					"Info",
					"<font color=red><b>Different</b></font>",
					"Same"};

  public static final int SAME = 0;
  public static final int DIFFERENT = 1;
  

  //Variables:
  ////////////

  protected DBConfig dbConfig;

  //Constructors:
  ///////////////

  public Contrastor(DBConfig dbConfig){
    super(dbConfig);
    this.dbConfig = dbConfig;
  }

  //Public Members
  ////////////////

  public int getDiffResult(Logger logger, Statement s, 
			   int compone, int comptwo, int test){
    if (isTestCategory(test))
      return RESULT_OK;

    if(!tableAvailable(logger, s, compone, comptwo, test))
      return RESULT_NOT_RUN;
    try{
      return determineResult(s, test, compone, comptwo);
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Could not determine result from validation table",e);
    }
    return RESULT_NOT_RUN;
  }

  public String getDiffResultString(Logger l, Statement s,
				    int compone, int comptwo, int test){
    return RESULTS[getDiffResult(l, s, compone, comptwo, test)];
  }

  /** test has a ONE based index, 0 is reserved for 'all tests'**/
  public void runTest(HTMLizer h, Statement s1, Statement s2, Statement s3,
		      int compone, int comptwo, int test){
    // keep track of whether this category has been run
    if (isTestCategory(test))
      ResultTable.updateStatus(h,s1,this,compone,comptwo,test);

    List l = getTestIndicesForTestType(test);

    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();
      try {
	runSingleTest(h,s1,s2,s3,compone,comptwo,idx);
	ResultTable.updateStatus(h,s1,this,compone,comptwo,idx);
      } catch (Exception e) {
	h.logMessage(Logger.WARNING, Logger.GENERIC,
		     "Problem running comparison for "+getTest(idx).getDescription());
      }
    }
    h.p("<BR><B>Finished Comparison</B>");
  }
    
  /** test has a ONE based index, 0 is reserved for 'all tests'**/
  public void displayTest(Statement s, HTMLizer h, int compone, int comptwo, int test){
    List l = getTestIndicesForTestType(test);

    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();
      displaySingleTest(h,s,compone,comptwo,idx);
    }
  }
  
  /** test has a ONE based index, 0 is reserved for 'all tests'**/
  public void clearTest(Statement s, HTMLizer h, int compone, int comptwo, int test){
    // keep track of whether this category has been run
    if (isTestCategory(test))
      ResultTable.removeTest(h,s,compone,comptwo,test);

    List l = getTestIndicesForTestType(test);

    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();
      clearSingleTest(h,s,compone,comptwo,idx);
    }
    h.p("<BR><B>Cleared</B>");
  }
  

  //Members
  /////////

  public boolean tableAvailable(Logger logger, Statement s, int compone, int comptwo, int test){
    try{
      DatabaseMetaData meta = s.getConnection().getMetaData();
      String tTypes[]={"TABLE"}; 
      ResultSet rs;
      rs = meta.getTables(null,null,getDiffName(test, compone, comptwo),tTypes);
      boolean ret=rs.next();
      rs.close();
      return ret;
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Could not determine if status table available",e);
    }
    return false;
  }
  
  protected String getDiffName(int test, int compone, int comptwo){
    Test t = getTest(test);
    int one;
    int two;
    if(compone > comptwo) {
      two = compone;
      one = comptwo;
    } else {
      one = compone;
      two = comptwo;
    }
    return dbConfig.getDBTableName(Controller.getTableName
				   ("df"+t.getRawTableName()+"_"+one,two));
  }

  protected int determineResult(Statement s, int test, int compone, int comptwo)
    throws SQLException{
    ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM "+getDiffName(test, compone, comptwo));
    rs.next();
    if(rs.getInt(1)!=0){
      return RESULT_DIFF;
    }
    return RESULT_SAME;
  }

  /////////////////////////////////////////////////////////////////////
  
  protected void runSingleTest(HTMLizer h, Statement s1, Statement s2, Statement s3,
			       int compone, int comptwo, int test) {
    if (!tableAvailable(h,s1,compone,comptwo,test)) {
      try {
	getTest(test).prepare(h,s1,compone);
	ResultTable.updateStatus(h,s1,this,compone,test);
	getTest(test).prepare(h,s2,comptwo);
	ResultTable.updateStatus(h,s2,this,comptwo,test);
	ResultSet rs1 = grabTestResults(h,s1,compone,test);
	ResultSet rs2 = grabTestResults(h,s2,comptwo,test);
	createTable(h,s3,compone,comptwo,test);
	int count = 1;
	boolean hasLine1 = rs1.next(); // point at first line
	boolean hasLine2 = rs2.next(); // point at first line
	while (hasLine1 || hasLine2) {
	  int equality = getTest(test).linesEqual(h,rs1,hasLine1,rs2,hasLine2,rs1.getMetaData().getColumnCount());
	  if (equality == Test.EQUALTO) {
	    hasLine1 = rs1.next();
	    hasLine2 = rs2.next();
	  } else if (equality == Test.LESSTHAN) {
	    insertShortRow(h,s3,rs1,1,getDiffName(test,compone,comptwo),count++);
	    hasLine1 = rs1.next();
	  } else if (equality == Test.GREATERTHAN) {
	    insertShortRow(h,s3,rs2,2,getDiffName(test,compone,comptwo),count++);
	    hasLine2 = rs2.next();
	  } else if (equality == Test.CONFLICT) {
	    insertConflictRow(h,s3,rs1,rs2,getDiffName(test,compone,comptwo),count++);
	    hasLine1 = rs1.next();
	    hasLine2 = rs2.next();
	  }
	}
	updateDifferenceTable(h,s1,compone,comptwo,test);
      } catch (SQLException e){
	h.logMessage(Logger.ERROR,Logger.DB_WRITE,
		     "Problem running test "+test,e);
      }
    }
  }

  protected void updateDifferenceTable(Logger logger, Statement s, int compone, int comptwo, int test) {
    try {
      boolean oldDifferent = grabOverallDiffStatus(logger, s,compone,comptwo);
      
      // New table different?
      ResultSet rs = s.executeQuery("select count(*) from "+getDiffName(test,compone,comptwo));
      rs.next();
      boolean newDifferent = false;
      if (rs.getInt(1) > 0) {
	newDifferent = true;
      }
      
      // Grab old value from table
      int x = compone; int y = comptwo;
      if (comptwo < compone) {
	x = comptwo; y = compone;
      }
      
      // Make decision
      if (newDifferent && !oldDifferent) {
	// System.out.println("Changing Entry");
	s.executeQuery("update " + DGPSPConstants.STATUS_TABLE + " set status='"+DIFFERENT+"' where runone = '"+x+"' and runtwo = '"+y+"'");
      }
    } catch (SQLException e) {
      logger.logMessage(Logger.ERROR,Logger.GENERIC,
		   "Problem handling difference table "+getDiffName(test,compone,comptwo),e);
    }
  }

  public boolean grabOverallDiffStatus(Logger logger, Statement s, int compone, int comptwo) {
    boolean oldDifferent = false;
    try {


      int x = compone; int y = comptwo;
      if (comptwo < compone) {
	x = comptwo; y = compone;
      }
      int result;
      ResultSet rs = s.executeQuery("select status from " + DGPSPConstants.STATUS_TABLE + " where runone = '"+x+"' and runtwo = '"+y+"'");
      if (!rs.next()) {
	// System.out.println("Inserting Entry");
	s.executeQuery("insert into " + DGPSPConstants.STATUS_TABLE + " values('"+x+"','"+y+"','"+SAME+"')");
	oldDifferent = false;
      } else {
	result = rs.getInt(1);
	oldDifferent = (result==SAME?false:true);
      }
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.GENERIC,
		   "Problem querying status table",e);
    }
    return oldDifferent;
  }

  protected void createTable(Logger logger, Statement s, int compone, int comptwo, int test) {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("create table "+getDiffName(test,compone,comptwo)+" (");
      sb.append("uniqueid INTEGER NOT NULL");
      ResultSet rs=s.executeQuery("select * from "+getTest(test).getTableName(compone));
      ResultSetMetaData rsmd=rs.getMetaData();
      for(int i=1;i<=rsmd.getColumnCount();i++) {
	sb.append(",");
	sb.append(rsmd.getColumnName(i)+"_"+compone+" ");
	sb.append(fixType(rsmd.getColumnTypeName(i)));
      }
      for(int i=1;i<=rsmd.getColumnCount();i++) {
	sb.append(",");
	sb.append(rsmd.getColumnName(i)+"_"+comptwo+" ");
	sb.append(fixType(rsmd.getColumnTypeName(i)));
      } 
      sb.append(")");
      // System.out.println(sb.toString());
      s.executeQuery(sb.toString());
//       dbConfig.createTableSelect(s,getDiffName(test,compone,comptwo),
// 				 "select * from "+getTest(test).getTableName(compone));
//       s.executeQuery("delete from "+getDiffName(test,compone,comptwo));
//       s.executeQuery("alter table "+getDiffName(test,compone,comptwo)+" add Origin INTEGER");
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "Problem creating table "+getDiffName(test,compone,comptwo),e);
    }
  }

  protected String fixType(String type) {
    if (type.equals("VARCHAR")) {
        return "VARCHAR(255)";
    } else if (type.equals("LONG")) {
        return "INTEGER";
    } else if (type.equals("CHAR")) {
        return "VARCHAR(30)";
    } else  {
        return type;
    }
  }

  protected void insertShortRow(Logger logger, Statement s, ResultSet rs, 
				int position, String tablename, int count) {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("insert into "+tablename+" values (");
      sb.append("'"+count+"',");
      if (position==1) {
	for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
	  String thisColumn = rs.getString(i);
	  if (thisColumn == null) {
          sb.append("NULL,");
	  } else {
          sb.append("'"+thisColumn+"',");
      }
	}
	for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
	  sb.append("NULL,");
	}
      } else if (position==2) {
	for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
	  sb.append("NULL,");
	}
	for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
	  String thisColumn = rs.getString(i);
	  if (thisColumn == null) {
          sb.append("NULL,");
	  } else {
          sb.append("'"+thisColumn+"',");
      }
	}
      } else {
	logger.logMessage(Logger.ERROR,Logger.GENERIC,
			  "Incorrect position field in row insertion");
      }
      sb.deleteCharAt(sb.length()-1); // remove trailing ,
      sb.append(")");
      // System.out.println(sb.toString());
      s.executeQuery(sb.toString());
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
 			"Problem inserting row",e);
    }
  }

  protected void insertConflictRow(Logger logger, Statement s, ResultSet rs1, ResultSet rs2,
				   String tableName, int count) {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("insert into "+tableName+" values (");
      sb.append("'"+-count+"',");
      for(int i=1;i<=rs1.getMetaData().getColumnCount();i++) {
	String thisColumn = rs1.getString(i);
	if (thisColumn == null) {
        sb.append("NULL,");
	} else {
        sb.append("'"+thisColumn+"',");
    }
      }
      for(int i=1;i<=rs2.getMetaData().getColumnCount();i++) {
	String thisColumn = rs2.getString(i);
	if (thisColumn == null) {
        sb.append("NULL,");
	} else {
        sb.append("'"+thisColumn+"',");
    }
      }
      sb.deleteCharAt(sb.length()-1); // remove trailing ,
      sb.append(")");
      s.executeQuery(sb.toString());
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
 			"Problem inserting conflicted row",e);
    }
  }

  protected ResultSet grabTestResults(Logger l, Statement s, int run, int test) {
    String sql = null;
    StringBuffer sb = new StringBuffer();
    ResultSet rs = null;
    try {
      sql="select * from "+getTest(test).getTableName(run);
      rs=s.executeQuery(sql);
      int columns = rs.getMetaData().getColumnCount();
      sb.append(sql);
      sb.append("\norder by ");
      for (int i=1; i<=columns;i++) {
	sb.append(i+",");
      }
      sb.deleteCharAt(sb.length()-1);

      if (l.isMinorEnabled()) {
	  l.logMessage(Logger.MINOR,Logger.DB_WRITE,"Contrastor.grabTestResults - sql was " +
		       sb.toString());
      }
	  
      //System.out.println("Check: "+sb.toString());
      rs=s.executeQuery(sb.toString());
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "Contrastor.grabTestResults - Problem executing query : " + sql,
		   sqle);
    } 
    return rs;
  }

  /////////////////////////////////////////////////////////////////////

  // Nabbed from test with special cases to handle extra column
  protected void displaySingleTest(HTMLizer h, Statement s, int compone, int comptwo, int testnum) {
    Logger logger=h;
    Test test=getTest(testnum);
    h.h2(test.getDescription());
    if(!tableAvailable(logger,s,compone,comptwo,testnum)){
      h.sFont("RED");
      h.p("Test not yet run");
      h.eFont();
      return;
    }
    try{
      h.sTable();
      //Print the headers:
      h.sRow();
      String[] headers=test.getHeaders();
      for(int i=0;i<(headers.length*2);i++){
	if(i<headers.length) {
	  h.tHead(headers[i]+" "+compone);
	} else {
	  h.tHead(headers[i-headers.length]+" "+comptwo);
    }
      }
      h.eRow();
      //Print the rest of the table:
      int types[]=test.getTypes();
      ResultSet rs=s.executeQuery("SELECT * FROM "+getDiffName(testnum, compone, comptwo));
      int rows=0;
      while(rs.next()){
	rows++;
	h.sRow();
	int conflictFlag = rs.getInt(1);
	if (conflictFlag < 0) h.sFont("RED");
	for(int i=0;i<(types.length*2);i++){
	  int typeindex = i;
	  if (typeindex >= types.length) typeindex = typeindex - types.length;
	  int tableindex = i+2;
	  String output_s;
	  switch(types[typeindex]){
	  case Test.TYPE_STRING:
	    output_s = rs.getString(tableindex);
	    if (output_s==null) output_s = " ";
	    if (conflictFlag < 0) output_s = "<FONT COLOR=RED>"+output_s+"</FONT>";
	    h.tData(output_s);
	    break;
	  case Test.TYPE_INT:
	    int output_i = rs.getInt(tableindex);
	    if (output_i==0) {
            output_s=" ";
	    } else {
            output_s=new Integer(output_i).toString();
        }
	    if (conflictFlag < 0) output_s = "<FONT COLOR=RED>"+output_s+"</FONT>";
	    h.tData(output_s);
	    break;
	  case Test.TYPE_DOUBLE:
	    h.tData(rs.getDouble(tableindex));
	    break;
	  case Test.TYPE_TONNAGE:
	    h.tData(rs.getDouble(tableindex), HTMLizer.NO_FRACTION_FORMAT);
	    break;
	  case Test.TYPE_DATETIME:
	    h.tData(rs.getDate(tableindex)+" "+rs.getTime(tableindex));
	    break;
	  case Test.TYPE_ENUM_1:
	  case Test.TYPE_ENUM_2:
	  case Test.TYPE_ENUM_3:
	  case Test.TYPE_ENUM_4:
	  case Test.TYPE_ENUM_5:
	    h.tData(test.renderEnum(types[typeindex],rs.getInt(tableindex)));
	    break;
	  default:
	    logger.logMessage(Logger.WARNING,Logger.DB_QUERY,
			      "Unknown type: "+types[typeindex]);
	  }
	}
	if (conflictFlag < 0) h.eFont();
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

  /////////////////////////////////////////////////////////////////////

  protected void clearSingleTest(Logger logger, Statement s, int compone, int comptwo, int test) {
    if(tableAvailable(logger,s,compone,comptwo,test)){
      try{
	logger.logMessage(Logger.MINOR, Logger.DB_WRITE,
			  "Dropping table " + getDiffName(test,compone,comptwo));
	s.executeQuery("drop table "+getDiffName(test,compone,comptwo));
	ResultTable.removeTest(logger,s,compone,comptwo,test);
      }catch(SQLException e){
	logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			  "Could not drop table "+getDiffName(test,compone,comptwo),
			  e);
      }
    }
  }

  //InnerClasses:
  ///////////////


}
