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
package org.cougaar.mlm.ui.grabber.webserver;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;
import org.cougaar.mlm.ui.grabber.logger.DBIDLogger;
import org.cougaar.mlm.ui.grabber.config.WebServerConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;

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
 * Handles requests for commands to the validator
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/8/01
 **/
public class ValidatorRequestHandler extends DynamicRequestHandler{

  //Constants:
  ////////////

  public static final int COM_UNKNOWN=0;
  public static final int COM_MAIN_MENU=1;
  public static final int COM_RUN_TEST=2;
  public static final int COM_DISPLAY_TEST=3;
  public static final int COM_CLEAR_TEST=4;

  private static String[]COMMANDS={"unknown",
				   "main",
				   "runtest",
				   "displaytest",
				   "cleartest"};

  //Variables:
  ////////////

  protected int run=-1;
  protected int test=-1;
  protected int main=0;

  protected Validator validator;

  //Constructors:
  ///////////////

  public ValidatorRequestHandler(DBConfig dbConfig, Connection connection,
				  WebServerConfig config, 
				  HttpRequest request){
    super(dbConfig, connection,config,request);
    validator=new Validator(dbConfig);
  }

  //Members:
  //////////

  protected String getURLRun(int command){
    return getURL(command,"?run="+run);
  }

  protected String getURLRun(int command, int run){
    return getURL(command,"?run="+run);
  }

  protected String getURLRunTest(int command){
    return getURL(command,"?run="+run+"?test="+test);
  }

  protected String getURLRunTest(int command, int run, int test){
    return getURL(command,"?run="+run+"?test="+test);
  }

  public int getModule(){
    return WebServerConfig.VALIDATOR;
  }

  /**Send the content for given command**/
  protected void sendCommandContent(HTMLizer h, 
				    Statement s, 
				    int command)
    throws SQLException,IOException{
    switch(command){
    case COM_MAIN_MENU:
      sendMainMenu(h,s);break;
    case COM_RUN_TEST:
      sendRunTest(h,s);break;
    case COM_DISPLAY_TEST:
      sendDisplayTest(h,s);break;
    case COM_CLEAR_TEST:
      sendClearTest(h,s);break;
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
    run=getQueryWithPrefix("run=");
    test=getQueryWithPrefix("test=");
    main=getQueryWithPrefix("main=");
  }

  /**should we allow this page to be cached**/
  protected boolean cacheable(){
    //allow caching of menu...
    //    if(command==COM_MAIN_MENU){
    //      return run<=0;
    //    }
    return false;
  }
  
  //Commands:

  protected void sendMainMenu(HTMLizer h, Statement s) 
    throws IOException, SQLException{

    if(run>0){
      sendRunMenu(h,s);
    }else{
      sendValidationMenu(h,s);
    }
  }

  protected String getRunListSql(){
    StringBuffer sb= Controller.getStdSelectQuery(dbConfig);
    if(run>0){
      sb.append(" WHERE ");
      sb.append(Controller.COL_RUNID);
      sb.append("=");
      sb.append(run);
    }
    sb.append(" ORDER BY ");
    sb.append(Controller.COL_RUNID);
    return sb.toString();
  }

  protected void sendValidationMenu(HTMLizer h, Statement s)
    throws IOException, SQLException{
    header(h,"Validation Menu");
    
    //if(run<1)
    //h.p("<B>Note</B>: This page may be cached for performance reasons.");

    h.sCenter();
    outputRunTable(h,s);
    h.eCenter();
    footer(h);
  }

  protected void outputRunTable(HTMLizer h, Statement s)
    throws IOException, SQLException{
    
    h.sTable();
    h.sRow();
    h.tHead("Run");
    h.tHead("Run started");
    h.tHead("Run Completed");
    h.tHead("Status");
    h.tHead("Action");
    h.eRow();
    
    Set testStatusCore = ResultTable.getTestStatus(h,s,Validator.CORE_TESTS);
    Set testStatusAll = ResultTable.getTestStatus(h,s,Validator.ALL_TESTS);

    ResultSet rs=s.executeQuery(getRunListSql());
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
      h.tData(getURL(WebServerConfig.CONTROLLER,
		     ControllerRequestHandler.COM_LISTLOG,
		     "?run="+r),
	      Run.CONDITIONS[condition]);

      StringBuffer sb=new StringBuffer();
      sb.append("[");
      if (testStatusCore.contains(new Integer(r))) {
	sb.append(h.aStr(getURLRunTest(COM_DISPLAY_TEST,r,Validator.CORE_TESTS),
			 "<B>Display Core Tests</B>"));
      } else {
	sb.append(h.aStr(getURLRunTest(COM_RUN_TEST,r,Validator.CORE_TESTS)+"?main=1",
			 "<B>Run Core Tests</B>"));
      }
      sb.append("][");
      
      if (testStatusAll.contains(new Integer(r))) {
	sb.append(h.aStr(getURLRunTest(COM_DISPLAY_TEST,r,Validator.ALL_TESTS),
			 "<B>Display All Tests</B>"));
      } else {
	sb.append(h.aStr(getURLRunTest(COM_RUN_TEST,r,Validator.ALL_TESTS)+"?main=1",
			 "<B>Run All Tests</B>"));
      }
      sb.append("]");
      h.tData(sb.toString());
      
      h.eRow();
    }
    rs.close();
    h.eTable();
  }

  protected void sendRunMenu(HTMLizer h, Statement s)
    throws IOException, SQLException{
    header(h,"Validation Menu For Run "+run);
    h.sCenter();
    outputRunTable(h,s);
    h.br();
    h.br();
    
    h.sCenter();
    h.sTable();
    h.sRow();
    h.tHead("Test Description");
    h.tHead("Status");
    h.eRow();

    h.sRow();
    h.tData("");
    h.tData("["+h.aStr(getURLRunTest(COM_RUN_TEST,run,0),
		       "<B>Run all tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,run,0),
		   "<B>Display all tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,run,0),
		   "<B>Clear all tests</B>")+"]");
    h.eRow();
    HashMap hm = ResultTable.fetchTable(h,s,run,run);
    outputTests(h,s,Validator.CORE_TESTS,hm);
    outputTests(h,s,Validator.INFO_TESTS,hm);
    outputTests(h,s,Validator.WARNING_TESTS,hm);
    outputTests(h,s,Validator.ERROR_TESTS,hm);
    h.eTable();
    h.eCenter();
    footer(h);
  }
  
  protected void outputTests(HTMLizer h, Statement s, int testtype, HashMap hm)
    throws IOException, SQLException{
    h.sRow();
    h.tHead(validator.getTestTypeString(testtype),0);
    h.tData("["+h.aStr(getURLRunTest(COM_RUN_TEST,run,testtype),
		       "<B>Run these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_DISPLAY_TEST,run,testtype),
		   "<B>Display these tests</B>")+"]<BR>["+
	    h.aStr(getURLRunTest(COM_CLEAR_TEST,run,testtype),
		   "<B>Clear these tests</B>")+"]");
    h.eRow();

    List l = validator.getTestIndicesForTestType(testtype);
    for(int i=0;i<l.size();i++){
      int idx=((Integer)l.get(i)).intValue();

      h.sRow();
      h.tData(validator.getDescription(idx));
      String tag = (String)hm.get(ResultTable.generateKey(run,run,idx));
      if (tag == null) {
	h.tData(h.aStr(getURLRunTest(COM_RUN_TEST,run,idx),
		       "Run test"));
      } else {
	h.tData(h.aStr(getURLRunTest(COM_DISPLAY_TEST,run,idx),
		       tag));
      }
      h.eRow();
    }
  }

  protected void sendRunTest(HTMLizer h, Statement s) 
    throws IOException, SQLException{
    //Parse the query:
    if(run==-1||test<Validator.MIN_TEST_CATEGORY){
      sendInvalidQuery(h);
      return;
    }
    //Do the run
    int confirm=getQueryWithPrefix("confirm=");
    if(confirm==1){
      //Do the run
      header(h,"Run Test: "+validator.getTestTypeString(test)+" for run("+run+")");
      h.sCenter();
      h.dismissLink();

      validator.runTest(h,s,run,test);
      h.eCenter();
      emptyFooter(h);
    }else{
      header(h,"Run Test: "+validator.getTestTypeString(test)+" for run("+run+")");
      h.sCenter();


      h.p("<B>"+
	  ((main == 1) ? h.popupStr(getURL(COM_MAIN_MENU),
				    getURLRunTest(COM_RUN_TEST,run,test)+"?confirm=1",
				    "runWindow",
				    "Begin Run")
	   : h.popupStr(getURLRun(COM_MAIN_MENU),
			getURLRunTest(COM_RUN_TEST,run,test)+"?confirm=1",
			"runWindow",
			"Begin Run"))
	  +"</B>");
      h.eCenter();
      runMenuFooter(h,run);
    }
  }

  protected void sendDisplayTest(HTMLizer h, Statement s) 
    throws IOException, SQLException{
    //Parse the query:
    if(run==-1||test<Validator.MIN_TEST_CATEGORY){
      sendInvalidQuery(h);
      return;
    }
    //Do the run
    header(h,"Display Test: "+validator.getTestTypeString(test)+" for run("+run+")");
    h.sCenter();

    validator.displayTest(s,h,run,test);

    h.eCenter();
    runMenuFooter(h,run);
  }


  protected void sendClearTest(HTMLizer h, Statement s) 
    throws IOException, SQLException{
    //Parse the query:
    if(run==-1||test<Validator.MIN_TEST_CATEGORY){
      sendInvalidQuery(h);
      return;
    }
    int confirm=getQueryWithPrefix("confirm=");
    if(confirm==1){
      //Do the run
      header(h,"Clear Test: "+validator.getTestTypeString(test)+" for run("+run+")");
      h.sCenter();
      h.dismissLink();

      validator.clearTest(s,h,run,test);

      h.eCenter();
      emptyFooter(h);
    }else{
      header(h,"Confirm clear: "+validator.getTestTypeString(test)+" for run("+run+")");
      h.sCenter();
      h.p("<B>"+h.popupStr(getURLRun(COM_MAIN_MENU),
			   getURLRunTest(COM_CLEAR_TEST,run,test)+"?confirm=1",
			   "clearWindow",
			   "Confirm clear")
	  +"</B>");
      h.eCenter();
      runMenuFooter(h,run);
    }
  }

  //Helpers:
 
  protected void runMenuFooter(HTMLizer h, int run){
    if(run>0){
      h.print("<CENTER>\n"+
	       "<BR><BR><BR>\n"+
	       "<A HREF=\"main?run="+run+
	       "\">Validation menu for run ("+run+")</A>\n"+
	       "</CENTER>\n");
    }
    footer(h);
  }
  
  //Static members:
  /////////////////

  //InnerClasses:
  ///////////////
}



