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
package org.cougaar.mlm.ui.grabber.webserver;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;
import org.cougaar.mlm.ui.grabber.logger.DBIDLogger;
import org.cougaar.mlm.ui.grabber.config.WebServerConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;

import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.controller.Run;
import org.cougaar.mlm.ui.grabber.validator.*;

import java.io.*;
import java.net.*;
import java.util.*;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import java.text.SimpleDateFormat;

/**
 * Handles requests for commands to the contrastor
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/8/01
 **/
public class ComparisonRequestHandler extends DynamicRequestHandler{

  //Constants:
  ////////////

  public static final int COM_UNKNOWN=0;
  public static final int COM_MAIN_MENU=1;
  public static final int COM_RUN_TEST=2;
  public static final int COM_DISPLAY_TEST=3;
  public static final int COM_CLEAR_TEST=4;

  public static final int COM_RUN_DIFF=5;
  public static final int COM_DISPLAY_DIFF=6;
  public static final int COM_CLEAR_DIFF=7;

  private static String[]COMMANDS={"unknown",
				   "main",
				   "runtest",
				   "displaytest",
				   "cleartest",
				   "rundiff",
				   "displaydiff",
				   "cleardiff"};

  //Variables
  ///////////

  protected int run=-1;
  protected int compone=-1;  
  protected int comptwo=-1;  
  protected int test=-1;
  protected int baseline=1;
  protected int main=0;

  protected Contrastor contrastor; // functions as a Validator as well for some purposes

  //Constructors:
  ///////////////

  public ComparisonRequestHandler(DBConfig dbConfig, Connection connection,
				  WebServerConfig config, 
				  HttpRequest request){
    super(dbConfig, connection,config,request);
    contrastor=new Contrastor(dbConfig);
  }

  //Members
  /////////

  // Overriding this so I can create TWO statements so I can
  // do diffs easily instead of storing ResultSets
  protected void sendContent(PrintStream ps) throws IOException{
    Statement s1=getStatement(ps);
    Statement s2=getStatement(ps);
    Statement s3=getStatement(ps);
    if(s1==null || s2==null)
      return;
    try{
      HTMLizer h=new HTMLizer(ps);
      sendCommandContent(h,s1,s2,s3,command);
    }catch(SQLException e){
      ps.print("Exception while processing command '"+target+"':"+e);
    }
    closeStatement(ps,s1);
    closeStatement(ps,s2);
    closeStatement(ps,s3);
  }

  protected String getURLRun(int command){
    return getURL(command,"?run="+run)+"?baseline="+baseline;
  }

  protected String getURLRun(int command, int run){
    return config.getURL(WebServerConfig.VALIDATOR,command)+"?run="+run+"?baseline="+baseline;
  }

  protected String getURLRunTest(int command, int run, int test){
    return config.getURL(WebServerConfig.VALIDATOR,command)+"?run="+run+"?test="+test+"?baseline="+baseline;
  }

  protected String getURLRunDiff(int command){
    return getURL(command,"?compone="+compone+"&comptwo="+comptwo+"&test="+test)+"?baseline="+baseline;
  }

  protected String getURLRunDiff(int command, int compone, int comptwo, int test){
    return getURL(command,"?compone="+compone+"&comptwo="+comptwo+"&test="+test)+"?baseline="+baseline;
  }

  public int getModule(){
    return WebServerConfig.COMPARATOR;
  }

  protected void sendCommandContent(HTMLizer h, Statement s, int command) {
    h.logMessage(Logger.ERROR,Logger.DB_WRITE,"This function shouldn't be called!!!");
  }

  /**Send the content for given command**/
  protected void sendCommandContent(HTMLizer h, 
				    Statement s1, 
				    Statement s2, 
				    Statement s3, 
				    int command)
    throws SQLException,IOException{
    switch(command){
    case COM_MAIN_MENU:
      sendMainMenu(h,s1,s2);break;
    case COM_RUN_DIFF:
      sendRunTest(h,s1,s2,s3);break;
    case COM_DISPLAY_DIFF:
      sendDisplayTest(h,s1);break;
    case COM_CLEAR_DIFF:
      sendClearTest(h,s1);break;
    }
  }

  protected int getCommand(String comStr){
    for(int i=0;i<COMMANDS.length;i++){
      if(comStr.endsWith(COMMANDS[i]))
	return i;
    }
    return COM_UNKNOWN;
  }

  public static String getCommandName(int command){
    if(command>=0&&command<COMMANDS.length)
      return COMMANDS[command];
    return "unknown";
  }

  protected void modifyTarget(){
    super.modifyTarget();
    compone=getQueryWithPrefix("compone=");
    comptwo=getQueryWithPrefix("comptwo=");
    run=getQueryWithPrefix("run=");
    test=getQueryWithPrefix("test=");
    baseline=getQueryWithPrefix("baseline=");
    main=getQueryWithPrefix("main=");
  }

  protected String getStringQueryWithPrefix(String prefix){
    StringTokenizer strTok=new StringTokenizer(getQuery,"?&");
    try{
      while(strTok.hasMoreTokens()){
	String tok=strTok.nextToken();
	if(tok.startsWith(prefix)){
	  return tok.substring(prefix.length());
	}
      }
    }catch(Exception e){
      return " ";
    }
    return " ";
  }

  protected boolean cacheable(){
    return false;
  }
  
  //Commands:

  protected void sendMainMenu(HTMLizer h, Statement s1, Statement s2) 
    throws IOException, SQLException{

    //    System.out.println ("ComparisonRequestHandler.sendMainMenu called.");

    if(compone>0 && comptwo>0 && compone!=comptwo){
      sendRunMenu(h,s1);
    }else{
      sendValidationMenu(h,s1,s2);
    }
  }

  protected String getRunListSql(){
    StringBuffer sb=new StringBuffer();
    //    System.out.println ("ComparisonRequestHandler.getRunListSql called.");

    sb.append("SELECT ");
    sb.append(Controller.COL_RUNID);
    sb.append(", ");
    sb.append(Controller.COL_STARTTIME);
    sb.append(", ");
    sb.append(Controller.COL_ENDTIME);
    sb.append(", ");
    sb.append(Controller.COL_CONDITION);
    sb.append(" FROM ");
    sb.append(dbConfig.getDBTableName(Controller.RUN_TABLE_NAME));
    if(compone>0 && comptwo>0 && compone!=comptwo){
      sb.append(" WHERE ");
      sb.append(Controller.COL_RUNID);
      sb.append(" IN (");
      sb.append("'"+compone+"','"+comptwo+"'");
      sb.append(")");
    }
    sb.append(" ORDER BY ");
    sb.append(Controller.COL_RUNID);
    return sb.toString();
  }

  protected void sendValidationMenu(HTMLizer h, Statement s1, Statement s2)
    throws IOException, SQLException{
    //    System.out.println ("ComparisonRequestHandler.sendValidationMenu called.");
    header(h,"Comparator Menu");
    
    //if(run<1)
    //h.p("<B>Note</B>: This page may be cached for performance reasons.");

    h.sCenter();
    comparisonForm(h,s1);
    outputRunTable(h,s1,s2);
    h.eCenter();
    footer(h);
  }

  protected void comparisonForm(HTMLizer h, Statement s) 
    throws IOException, SQLException {
    ResultSet rs=s.executeQuery(getRunListSql());
    List runs = new ArrayList();

    while(rs.next()) {
      int r=rs.getInt(1);
      runs.add(new Integer(r));
    }

    h.h3("Compare Runs: ");
    h.print("<form>");
    h.print("<select name=\"compone\">");
    for(Iterator iter = runs.iterator(); iter.hasNext();) {
      Integer i = (Integer)iter.next();
      h.print("<option value=\""+i+"\">"+i+"</option>");
    }
    h.print("</select>");
    h.print("<select name=\"comptwo\">");
    for(Iterator iter = runs.iterator(); iter.hasNext();) {
      Integer i = (Integer)iter.next();
      h.print("<option value=\""+i+"\">"+i+"</option>");
    }
    h.print("</select>");
    h.print("<input type=\"submit\" value=\"Compare\">");
    h.print("</form>");

    
  }

  protected void outputRunTable(HTMLizer h, Statement s)
    throws IOException, SQLException{

    Map runToOwners = new HashMap ();
    Map runToAssets = new HashMap ();

    getSizes(s, getRunListSql(), runToOwners, runToAssets);
    ResultSet rs=s.executeQuery(getRunListSql());
    
    h.sTable();
    h.sRow();
    h.tHead("Run");
    h.tHead("Run started");
    h.tHead("Run Completed");
    h.tHead("Units");
    h.tHead("Assets");
    h.tHead("Status");
    h.tHead("Action");
    h.eRow();
    
    while(rs.next()){
      int r=rs.getInt(1);
      Date sDate=rs.getTimestamp(2);
      Date eDate=rs.getTimestamp(3);
      int condition=rs.getInt(4);

      h.sRow();
      h.tData(r);
      h.tData(sDate);
      h.tData(eDate);

      Object numOwners = runToOwners.get(new Integer(r));
      Object numAssets = runToAssets.get(new Integer(r));
      h.tData((numOwners == null) ? "N/A" : numOwners.toString());
      h.tData((numAssets == null) ? "N/A" : numAssets.toString());

      h.tData(getURL(WebServerConfig.CONTROLLER,
		     ControllerRequestHandler.COM_LISTLOG,
		     "?run="+r),
	      Run.CONDITIONS[condition]);
      h.tData(h.aStr(getURLRun(COM_MAIN_MENU,r),
		     "Validation"));

      h.eRow();
    }
    rs.close();
    h.eTable();
  }

  protected void outputRunTable(HTMLizer h, Statement s1, Statement s2)
    throws IOException, SQLException{

    Set testStatusCore = ResultTable.getDiffStatus(h,s1,Validator.CORE_TESTS, baseline);
    Set testStatusAll =  ResultTable.getDiffStatus(h,s1,Validator.ALL_TESTS, baseline);

    Map runToOwners = new HashMap ();
    Map runToAssets = new HashMap ();

    getSizes(s1, getRunListSql(), runToOwners, runToAssets);

    ResultSet rs=s1.executeQuery(getRunListSql());
    List runs = new ArrayList();
    while(rs.next()) {
      int r=rs.getInt(1);
      runs.add(new Integer(r));
    }
    rs=s1.executeQuery(getRunListSql());

    h.sTable();
    h.sRow();
    h.tHead("Run");
    h.tHead("Run started");
    h.tHead("Run Completed");
    h.tHead("Units");
    h.tHead("Assets");
    h.tHead("Status");
    StringBuffer str = new StringBuffer();
    str.append("Baseline:<form><select name=\"baseline\">");
    for(Iterator iter = runs.iterator(); iter.hasNext();) {
      Integer i = (Integer)iter.next();
      str.append("<option ");
      if (baseline == -1) {
	baseline = i.intValue();
      }
      if (i.intValue() == baseline) {
	str.append("selected");
      }
      str.append(" value=\""+i+"\">"+i+"</option>");
    }
    str.append("</select><input type=\"submit\" value=\"Set\"></form>");
    h.tHead(str.toString());
    h.tHead("Action");
    h.eRow();
      
      while(rs.next()){
	int r=rs.getInt(1);
	Date sDate=rs.getTimestamp(2);
	Date eDate=rs.getTimestamp(3);
	int condition=rs.getInt(4);
	
	h.sRow();
	h.tData(h.aStr(getURLRun(COM_MAIN_MENU,r),
		       r+""));
	h.tData(sDate);
	h.tData(eDate);

	Object numOwners = runToOwners.get(new Integer(r));
	Object numAssets = runToAssets.get(new Integer(r));
	h.tData((numOwners == null) ? "N/A" : numOwners.toString());
	h.tData((numAssets == null) ? "N/A" : numAssets.toString());

	h.tData(getURL(WebServerConfig.CONTROLLER,
		   ControllerRequestHandler.COM_LISTLOG,
		       "?run="+r),
		Run.CONDITIONS[condition]);
    
	if (r == baseline)
	  h.tData("Baseline");
	else {
	  int x,y;
	  if (r < baseline) { x = r; y = baseline; }
	  else { y = r; x = baseline; }
	  
	  ResultSet diffrs = s2.executeQuery("select status from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+
					     x+"' and runtwo = '"+y+"' and test = '0'");
	  StringBuffer sb = new StringBuffer();
	  if (diffrs.next() && diffrs.getInt("status") == Contrastor.RESULT_DIFF) {
	    sb.append("<FONT COLOR=\"RED\">Different</FONT>");
	  } else {
	    sb.append("Nothing Detected");
	  }
	  
	  diffrs = s2.executeQuery("select count(*) from "+DGPSPConstants.STATUS_TABLE+" where runone = '"+
				   x+"' and runtwo = '"+y+"' and test != '0'");
	  diffrs.next();
	  sb.append(" "+diffrs.getInt(1)+"/"+contrastor.getNumTests()+" Run");
	  
	  h.tData(h.aStr(getURLRunDiff(COM_MAIN_MENU,x,y,0),"["+sb.toString()+"]"));

	  StringBuffer strb=new StringBuffer();
	  strb.append("[");
	  if (testStatusCore.contains(new Integer(r)))
	    strb.append(h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,x,y,Validator.CORE_TESTS),
			       "<B>Display Core Diffs</B>"));
	  else
	    strb.append(h.aStr(getURLRunDiff(COM_RUN_DIFF,x,y,Validator.CORE_TESTS)+"&main=1",
			       "<B>Run Core Diffs</B>"));
	  strb.append("][");
	  if (testStatusAll.contains(new Integer(r)))
	    strb.append(h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,x,y,Validator.ALL_TESTS),
			       "<B>Display All Diffs</B>"));
	  else
	    strb.append(h.aStr(getURLRunDiff(COM_RUN_DIFF,x,y,Validator.ALL_TESTS)+"&main=1",
			       "<B>Run All Diffs</B>"));
	  strb.append("]");
	  h.tData(strb.toString());

	}
	h.eRow();
      }
      rs.close();
      h.eTable();
  }

  protected void sendRunMenu(HTMLizer h, Statement s)
    throws IOException, SQLException{

    //    System.out.println ("ComparisonRequestHandler.sendRunMenu called.--->");

    header(h,"Comparator Menu For Runs "+compone+" and "+comptwo);
    h.sCenter();
    outputRunTable(h,s);
    h.br();
    h.br();
    
    int different = ResultTable.grabOverallDiffStatus(h,s,compone,comptwo);
    if (different == Contrastor.RESULT_DIFF) {
      h.h2("<FONT COLOR=\"RED\">Difference Detected</FONT>");
    } else {
      h.h2("<FONT COLOR=\"GREEN\">Difference Undetected </FONT>");
    }

    h.sCenter();
    h.sTable();
    h.sRow();
    h.tHead("Test Description");
    h.tHead("Run "+compone);
    h.tHead("Run "+comptwo);
    h.tHead("Diff");
    h.eRow();

    h.sRow();
    h.tData("");
    h.tData("["+
	    h.aStr(getURLRunTest(COM_RUN_TEST,compone,0),
		   "<B>Run all tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,compone,0),
		   "<B>Display all tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,compone,0),
		   "<B>Clear all tests</B>")+"]");
    h.tData("["+
	    h.aStr(getURLRunTest(COM_RUN_TEST,comptwo,0),
		   "<B>Run all tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,comptwo,0),
		   "<B>Display all tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,comptwo,0),
		   "<B>Clear all tests</B>")+"]");
    h.tData("["+
	    h.aStr(getURLRunDiff(COM_RUN_DIFF,compone,comptwo,0),
		   "<B>Run all diffs</B>")+"]<BR>["+
	    h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,compone,comptwo,0),
		   "<B>Display all diffs</B>")+"]<BR>["+
	    h.aStr(getURLRunDiff(COM_CLEAR_DIFF,compone,comptwo,0),
		   "<B>Clear all diffs</B>")+"]");
    h.eRow();

    HashMap hm = ResultTable.fetchTable(h,s,compone,comptwo);
    outputTests(h,s,Validator.CORE_TESTS,hm);
    outputTests(h,s,Validator.INFO_TESTS,hm);
    outputTests(h,s,Validator.WARNING_TESTS,hm);
    outputTests(h,s,Validator.ERROR_TESTS,hm);

    h.eTable();
    h.eCenter();
    footer(h);
  }
  
  protected void outputTests(HTMLizer h, Statement s, int testtype)
    throws IOException, SQLException{
    //    System.out.println ("ComparisonRequestHandler.outputTests called.--->");
    h.sRow();
    h.tHead(contrastor.getTestTypeString(testtype));
    h.tData("["+
	    h.aStr(getURLRunTest(COM_RUN_TEST,compone,testtype),
		   "<B>Run these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,compone,testtype),
		   "<B>Display these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,compone,testtype),
		   "<B>Clear these tests</B>")+"]");
    h.tData("["+
	    h.aStr(getURLRunTest(COM_RUN_TEST,comptwo,testtype),
		   "<B>Run these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,comptwo,testtype),
		   "<B>Display these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,comptwo,testtype),
		   "<B>Clear these tests</B>")+"]");
    h.tData("["+
	    h.aStr(getURLRunDiff(COM_RUN_DIFF,compone,comptwo,testtype),
		   "<B>Run these diffs</B>")+"]<BR>["+
	    h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,compone,comptwo,testtype),
		   "<B>Display these diffs</B>")+"]<BR>["+
	    h.aStr(getURLRunDiff(COM_CLEAR_DIFF,compone,comptwo,testtype),
		   "<B>Clear these diffs</B>")+"]");
    h.eRow();

    List l=contrastor.getTestIndicesForTestType(testtype);
    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();

      h.sRow();
      h.tData(contrastor.getDescription(idx));
      // First Run
      int res=contrastor.getTestResult(h,s,compone,idx);
      if(res==Test.RESULT_NOT_RUN){
	h.tData(h.aStr(getURLRunTest(COM_RUN_TEST,compone,idx),
		       "Run test"));
      }else{
	String resStr=contrastor.getTestResultString(h,s,compone,idx);
	h.tData(h.aStr(getURLRunTest(COM_DISPLAY_TEST,compone,idx),
		       resStr));
      }
      // Second Run
      res=contrastor.getTestResult(h,s,comptwo,idx);
      if(res==Test.RESULT_NOT_RUN){
	h.tData(h.aStr(getURLRunTest(COM_RUN_TEST,comptwo,idx),
		       "Run test"));
      }else{
	String resStr=contrastor.getTestResultString(h,s,comptwo,idx);
	h.tData(h.aStr(getURLRunTest(COM_DISPLAY_TEST,comptwo,idx),
		       resStr));
      }
      // Difference
      res=contrastor.getDiffResult(h,s,compone,comptwo,idx);
      if(res==Test.RESULT_NOT_RUN){
	h.tData(h.aStr(getURLRunDiff(COM_RUN_DIFF,compone,comptwo,idx),
		       "Run diff"));
      }else{
	String resStr=contrastor.getDiffResultString(h,s,compone,comptwo,idx);
	h.tData(h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,compone,comptwo,idx),
		       resStr));
      }
      h.eRow();
    }
  }

  protected void outputTests(HTMLizer h, Statement s, int testtype, HashMap hm)
    throws IOException, SQLException{
    //    System.out.println ("ComparisonRequestHandler.outputTests called.--->");
    h.sRow();
    h.tHead(contrastor.getTestTypeString(testtype),0);
    h.tData("["+
	    h.aStr(getURLRunTest(COM_RUN_TEST,compone,testtype),
		   "<B>Run these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,compone,testtype),
		   "<B>Display these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,compone,testtype),
		   "<B>Clear these tests</B>")+"]");
    h.tData("["+
	    h.aStr(getURLRunTest(COM_RUN_TEST,comptwo,testtype),
		   "<B>Run these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,comptwo,testtype),
		   "<B>Display these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,comptwo,testtype),
		   "<B>Clear these tests</B>")+"]");
    h.tData("["+
	    h.aStr(getURLRunDiff(COM_RUN_DIFF,compone,comptwo,testtype),
		   "<B>Run these diffs</B>")+"]<BR>["+
	    h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,compone,comptwo,testtype),
		   "<B>Display these diffs</B>")+"]<BR>["+
	    h.aStr(getURLRunDiff(COM_CLEAR_DIFF,compone,comptwo,testtype),
		   "<B>Clear these diffs</B>")+"]");
    h.eRow();

    List l=contrastor.getTestIndicesForTestType(testtype);
    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();

      h.sRow();
      h.tData(contrastor.getDescription(idx));
      // First Run
      String tag = (String)hm.get(ResultTable.generateKey(compone,compone,idx));
      if (tag == null) {
	h.tData(h.aStr(getURLRunTest(COM_RUN_TEST,compone,idx),
		       "Run test"));
      } else {
	h.tData(h.aStr(getURLRunTest(COM_DISPLAY_TEST,compone,idx),
		       tag));
      }
      // Second Run
      tag = (String)hm.get(ResultTable.generateKey(comptwo,comptwo,idx));
      if (tag == null) {
	h.tData(h.aStr(getURLRunTest(COM_RUN_TEST,comptwo,idx),
		       "Run test"));
      } else {
	h.tData(h.aStr(getURLRunTest(COM_DISPLAY_TEST,comptwo,idx),
		       tag));
      }
      // Difference
      tag = (String)hm.get(ResultTable.generateKey(compone,comptwo,idx));
      if (tag == null) {
	h.tData(h.aStr(getURLRunDiff(COM_RUN_DIFF,compone,comptwo,idx),
		       "Run diff"));
      } else {
	h.tData(h.aStr(getURLRunDiff(COM_DISPLAY_DIFF,compone,comptwo,idx),
		       tag));
      }
      h.eRow();
    }
  }


  protected void sendRunTest(HTMLizer h, Statement s1, Statement s2, Statement s3) 
    throws IOException, SQLException{
    //    System.out.println ("ComparisonRequestHandler.sendRunTest called.--->");
    //Parse the query:
    if(compone==-1||comptwo==-1||test<Validator.MIN_TEST_CATEGORY){
      sendInvalidQuery(h);
      return;
    }

    int confirm=getQueryWithPrefix("confirm=");
    if(confirm==1){
      //Do the run
      header(h,"Run Comparison: "+contrastor.getTestTypeString(test)+
	     " for runs("+compone+","+comptwo+")");

      h.sCenter();
      h.dismissLink();
      contrastor.runTest(h,s1,s2,s3,compone,comptwo,test);
      h.eCenter();
      emptyFooter(h);
    }else{
      header(h,"Confirm Comparison: "+contrastor.getTestTypeString(test)+
	     " for runs("+compone+","+comptwo+")");
      h.sCenter();
      h.p("<B>"+
	  ((main == 1) ? h.popupStr(getURL(COM_MAIN_MENU)+"?baseline="+baseline,
				    getURLRunDiff(COM_RUN_DIFF,compone,comptwo,test)+"?confirm=1",
				    "diffWindow",
				    "Begin Comparison")
	   : h.popupStr(getURLRunDiff(COM_MAIN_MENU,compone,comptwo,0)+"?baseline="+baseline,
			getURLRunDiff(COM_RUN_DIFF,compone,comptwo,test)+"?confirm=1",
			"diffWindow",
			"Begin comparison"))
	  +"</B>");

      h.eCenter();
      runMenuFooter(h,compone,comptwo);
    }
  }

  protected void sendDisplayTest(HTMLizer h, Statement s) 
    throws IOException, SQLException{
    //    System.out.println ("ComparisonRequestHandler.sendDisplayTest called.--->");
    //Parse the query:
    if(compone==-1||comptwo==-1||test<Validator.MIN_TEST_CATEGORY){
      sendInvalidQuery(h);
      return;
    }
    //Do the run
    header(h,"Display Comparison: "+contrastor.getTestTypeString(test)+
	   " for runs("+compone+","+comptwo+")");

    h.sCenter();
    contrastor.displayTest(s,h,compone,comptwo,test);
    h.eCenter();
    runMenuFooter(h,compone,comptwo);
  }


  protected void sendClearTest(HTMLizer h, Statement s) 
    throws IOException, SQLException{
    //    System.out.println ("ComparisonRequestHandler.sendClearTest called.--->");
    //Parse the query:
    if(compone==-1||comptwo==-1||test<Validator.MIN_TEST_CATEGORY){
      sendInvalidQuery(h);
      return;
    }
    int confirm=getQueryWithPrefix("confirm=");
    if(confirm==1){
      //Do the run
      header(h,"Clear Comparison: "+contrastor.getTestTypeString(test)+
	     " for runs("+compone+","+comptwo+")");

      h.sCenter();
      h.dismissLink();
      contrastor.clearTest(s,h,compone,comptwo,test);
      h.eCenter();
      emptyFooter(h);
    }else{
      header(h,"Confirm Clear: "+contrastor.getTestTypeString(test)+
	     " for runs("+compone+","+comptwo+")");
      h.sCenter();
      h.p("<B>"+h.popupStr(getURLRunDiff(COM_MAIN_MENU,compone,comptwo,test)+"?baseline="+baseline,
			   getURLRunDiff(COM_CLEAR_DIFF,compone,comptwo,test)+"?confirm=1",
			   "clearWindow",
			   "Confirm clear")
	  +"</B>");
      h.eCenter();
      runMenuFooter(h,compone,comptwo);
    }
  }

  //Helpers:

  protected void runMenuFooter(HTMLizer h, int compone, int comptwo){
    if(compone>0 && comptwo>0){
      h.print("<CENTER>\n"+
	       "<BR><BR><BR>\n"+
	       "<A HREF=\"main?compone="+compone+"&comptwo="+comptwo+
	       "\">Comparison menu for runs ("+compone+","+comptwo+")</A>\n"+
	       "</CENTER>\n");
    }
    footer(h);
  }
  
  //Static members:
  /////////////////

  //InnerClasses:
  ///////////////
}


