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

import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// Empty status must have default meaning when queried - table not guaranteed to be completely full

// Remove this class from Contrastor - Free it
// Make static

// Table Structure === Key, R1, R2, Test, Status
// R1 < R2

public class ResultTable {
  
  static int counter = 0;
  static Object countMutex = new Object ();

  // Create a Set indicating whether a given test category has been run across
  // all runs.
  public static Set getTestStatus(HTMLizer h, Statement s, int test)
    throws SQLException{
    Set retval = new HashSet();

	confirmTableExistence (h,s);
	
    ResultSet rs=s.executeQuery(getTestStatusSql(test));
    while(rs.next()) {
      int run = rs.getInt(1);
      int result = rs.getInt(2);

      if (result != Test.RESULT_NOT_RUN)
	retval.add(new Integer(run));
    }
    
    rs.close();
    return retval;
  }

  protected static String getTestStatusSql(int test){
    StringBuffer sb=new StringBuffer();
    sb.append("SELECT ");
    sb.append("runone, test, status");
    sb.append(" FROM ");
    sb.append(DGPSPConstants.STATUS_TABLE);
    sb.append(" WHERE ");
    sb.append("runtwo=runone");
    sb.append(" AND ");
    sb.append("test=");
    sb.append(test);
    return sb.toString();
  }

  // Create a Set indicating whether a given diff category has been run across
  // all runs.
  public static Set getDiffStatus(HTMLizer h, Statement s, int test, int baseline)
    throws SQLException{
    Set retval = new HashSet();

	confirmTableExistence (h,s);

    ResultSet rs = s.executeQuery(getDiffStatusSql(test, baseline));
    while(rs.next()) {
      int runone = rs.getInt(1);
      int runtwo = rs.getInt(2);
      int result = rs.getInt(3);
      int otherrun = baseline;
      if (runone == baseline) {
	otherrun = runtwo;
      } else if (runtwo == baseline) {
	otherrun = runone;
    }

      if (result != Test.RESULT_NOT_RUN)
	retval.add(new Integer(otherrun));
    }
    
    rs.close();

    return retval;
  }

  protected static String getDiffStatusSql(int test, int baseline){
    StringBuffer sb=new StringBuffer();
    sb.append("SELECT ");
    sb.append("runone, runtwo, test, status");
    sb.append(" FROM ");
    sb.append(DGPSPConstants.STATUS_TABLE);
    sb.append(" WHERE ");
    sb.append("runtwo<>runone");
    sb.append(" AND ");
    sb.append("(runone=");
    sb.append(baseline);
    sb.append(" OR ");
    sb.append("runtwo=");
    sb.append(baseline);
    sb.append(") AND ");
    sb.append("test=");
    sb.append(test);

    return sb.toString();
  }

  
  // Create a HashTable with status info in it for entire run or comparison
  public static HashMap fetchTable(Logger logger, Statement s, int compone, int comptwo) {
    confirmTableExistence(logger,s);
    int x = compone; int y = comptwo;
    if (comptwo < compone) { x = comptwo; y = compone; }
    
    HashMap retval = new HashMap();
    try {
      ResultSet rs = s.executeQuery("select * from "+
				    DGPSPConstants.STATUS_TABLE
				    +" where ("+
				    "runone = '"+x+"' and runtwo = '"+y+"')"+
				    " or (runone = '"+x+"' and runtwo = '"+x+"')"+
				    " or (runone = '"+y+"' and runtwo = '"+y+"')");
      while (rs.next()) {
	retval.put(generateKey(rs.getInt(2),rs.getInt(3),rs.getInt(4)),
		   Contrastor.RESULTS[rs.getInt(5)]);
      }
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_QUERY,
			"Problem querying status table",e);
    }
    return retval;
  }
  
  // Get an entry from Table after it has been created
  public static String readTable(HashMap table, int compone, int comptwo, int test) {
    int x = compone; int y = comptwo;
    if (comptwo < compone) { x = comptwo; y = compone; }
    
    return (String)table.get(generateKey(x,y,test));
  }
  
  // Remove entire run
  public static void remove(Logger logger, Statement s, int run ) {
    remove(logger,s,run,run);
  }
  
  // Remove an entire run or comparison
  public static void remove(Logger logger, Statement s, int compone, int comptwo) {
    confirmTableExistence(logger,s);
    int x = compone; int y = comptwo;
    if (comptwo < compone) { x = comptwo; y = compone; }
    
    boolean isComparison = (x!=0 && y!=0);
    try {
      s.executeQuery("delete from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+x+"' "+
		     (isComparison?"and":"or")+" runtwo = '"+y+"'");
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Problem removing run from status table",e);
    }
  }
  
  // Remove a single test from a run
  public static void removeTest(Logger logger, Statement s, int run, int test ) {
    removeTest(logger,s,run,run,test);
  }
  
  // Remove a single test (run or comparison)
  public static void removeTest(Logger logger, Statement s, int compone, int comptwo,int test) {
    confirmTableExistence(logger,s);
    int x = compone; int y = comptwo;
    if (comptwo < compone) { x = comptwo; y = compone; }
    
    try {
      s.executeQuery("delete from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+x+"' "+
		     "and runtwo = '"+y+"' "+
		     "and test = '"+test+"'");
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Problem removing run from status table",e);
    }
  }
  
  public static void removeTests(Logger logger, Statement s, int run) {
    removeTests(logger,s,run,run);
  }

  public static void removeTests(Logger logger, Statement s, int compone, int comptwo) {
    confirmTableExistence(logger,s);
    int x = compone; int y = comptwo;
    if (comptwo < compone) { x = comptwo; y = compone; }
    
    try {
      s.executeQuery("delete from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+x+"' "+
		     "and runtwo = '"+y+"'");
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Problem removing run from status table",e);
    }
  }


  public static void updateStatus(Logger logger, Statement s, Validator tester, int run, int test) {
    updateStatus(logger, s, tester, run, run, test);
  }
  
  // instruct a query to a table to fill in an entry (entry created or alterred)
  public static void updateStatus(Logger logger, Statement s, Validator tester, int compone, int comptwo, int test) { 
    synchronized (countMutex) { // protects against two threads doing update status at the same time 
      confirmTableExistence(logger,s);
      int x = compone; int y = comptwo;
      if (comptwo < compone) { x = comptwo; y = compone; }
    
      int status;
      if (compone == comptwo) {
	status = tester.getTestResult(logger,s,x,test);
      } else {
	status = ((Contrastor)tester).getDiffResult(logger,s,x,y,test);
      }
    
      // A whole comparison needs to be marked as different
      if (compone != comptwo && status == Contrastor.RESULT_DIFF) {
	try {
	  ResultSet rs = s.executeQuery("select * from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+x+"' and"+
					" runtwo = '"+y+"' and test = '0'");
	  if (!rs.next()) {
	    s.executeQuery("insert into "+DGPSPConstants.STATUS_TABLE+" values ('"+(counter++)+"','"+x+"','"+y+"','0','"+status+"')");
	  } else {
	    s.executeQuery("update "+DGPSPConstants.STATUS_TABLE+" set status = '"+status+"' where runone = '"+x+
			   "' and runtwo = '"+y+"' and test = '0'");
	  }
	} catch (SQLException e){
	  logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			    "Problem altering comparison state info in status table",e);
	}
      }
    
      try {
	ResultSet rs = s.executeQuery("select * from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+x+"' and"+
				      " runtwo = '"+y+"' and test = '"+test+"'");
	if (!rs.next()) {
	  s.executeQuery("insert into "+DGPSPConstants.STATUS_TABLE+" values ('"+(counter++)+"','"+x+"','"+y+"','"+test+"','"+status+"')");
	} else {
	  s.executeQuery("update "+DGPSPConstants.STATUS_TABLE+" set status = '"+status+"' where runone = '"+x+
			 "' and runtwo = '"+y+"' and test = '"+test+"'");
	}
      } catch (SQLException e){
	logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			  "Problem altering test info in status table",e);
      }
    }
  }
  
  public static int grabOverallDiffStatus(Logger logger, Statement s, int compone, int comptwo) {
    confirmTableExistence(logger, s);
    
    int x = compone; int y = comptwo;
    if (comptwo < compone) {
      x = comptwo; y = compone;
    }
    
    int result = 0;
    try {
      ResultSet rs = s.executeQuery("select status from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+x+
				    "' and runtwo = '"+y+"' and test = '0'");
      if (!rs.next()) {
	if (compone==0 || comptwo==0) {
        result = Contrastor.RESULT_NOT_RUN;
	} else {
        result = Contrastor.RESULT_SAME;
    }
      } else {
	result = rs.getInt(1);
      }
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_QUERY,
			"Problem getting overall status from status table",e);
    }
    return result;
  }
  
  // keyGeneraion for externally handled HashMap
  public static String generateKey(int compone, int comptwo, int test) {
    return compone+"-"+comptwo+"-"+test;
  }
  
  protected static void confirmTableExistence(Logger logger, Statement s) {
    try {
      DatabaseMetaData meta = s.getConnection().getMetaData();
      String tTypes[]={"TABLE"}; 
      ResultSet rs = meta.getTables(null,null,DGPSPConstants.STATUS_TABLE,tTypes);
      boolean statusTableAvailable=rs.next();
      rs.close();
      if (!statusTableAvailable) {
	s.executeQuery("create table "+DGPSPConstants.STATUS_TABLE+" (prikey INTEGER NOT NULL, runone INTEGER NOT NULL, runtwo INTEGER NOT NULL, "+
		       "test INTEGER NOT NULL, status INTEGER NOT NULL)");
      }
    } catch (SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Problem creating statusTable",e);
    }    
  }
  
}
