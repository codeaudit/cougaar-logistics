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
package org.cougaar.mlm.ui.grabber.webserver;

import org.cougaar.planning.servlet.data.completion.CompletionData;
import org.cougaar.planning.servlet.data.completion.FailedTask;
import org.cougaar.planning.servlet.data.completion.UnconfidentTask;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.StdLogger;
import org.cougaar.mlm.ui.grabber.config.WebServerConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.config.CompletionPSPConfig;
import org.cougaar.mlm.ui.grabber.connect.CompletionPSPConnection;
import org.cougaar.mlm.ui.grabber.workqueue.Result;

import org.cougaar.mlm.ui.grabber.validator.HTMLizer;

import java.io.*;
import java.net.*;
import java.util.*;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import java.text.NumberFormat;

/**
 * Handles a request to the completion assessor
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 3/4/01
 **/
public class CompletionAssessorRequestHandler extends DynamicRequestHandler{

  //Constants:
  ////////////

  private static final boolean VERBOSE=true;

  public static final int COM_HIT_PSP = 1;
  public static final int MAX_UNCONFIDENT_TO_SHOW = 100;
  public static final int MAX_FAILED_TO_SHOW = 100;

  private static String[]COMMANDS={"unknown",
				   "hitpsp"};

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public CompletionAssessorRequestHandler(DBConfig dbConfig, 
					  Connection connection,
					  WebServerConfig config, 
					  HttpRequest request){
    super(dbConfig, connection, config, request);
  }

  //Members:
  //////////

  //Likely overridden:

  /**module value for this class**/
  public int getModule(){
    return WebServerConfig.COMPLETIONASSESSOR;
  }

  /**Send the content for given command**/
  protected void sendCommandContent(HTMLizer h, 
				    Statement s, 
				    int command)
    throws SQLException,IOException{
    switch(command){
    case COM_HIT_PSP:
      sendNewHitPSP(h,s);break;
    }
  }

  /**@return the integer for the given command, or else COM_UNKNOWN**/
  protected int getCommand(String comStr){
    for(int i=0;i<COMMANDS.length;i++){
      if(comStr.endsWith(COMMANDS[i]))
	return i;
    }
    return COM_UNKNOWN;
  }

  /**The following static function is expected to be in all subclasses**/
  public static String getCommandName(int command){
    if(command>=0&&command<COMMANDS.length)
      return COMMANDS[command];
    return "unknown";
  }

  /**@return true iff all tasks were successful**/
  public boolean printHitPSP(HTMLizer h)throws IOException{
    boolean fullSuccess=false;
    try{
      h.setVerbosity(Logger.NORMAL);
      h.sCenter();
      Map results = getResults(h);
      if(!results.isEmpty ()){
	for (Iterator iter = results.keySet().iterator (); iter.hasNext (); ) {
	  String cluster = (String) iter.next ();
	  CompletionData cd=(CompletionData) results.get (cluster);
	  //Determine % complete:
	  double sumCompletion=getPercentComplete(cd);

	  int numTasks=cd.getNumberOfTasks();
	  double completion=(numTasks>0)?sumCompletion/numTasks:0;
	  String completionURL=
	    new CompletionPSPConfig (config.getCompletionPSPConfig(), cluster).
	      getUrlConnection().getURL();
	  h.p("<font size=+2>" + cluster + 
	      "</font><p><B>Percent of processing complete: "+getPercentStr(completion)+
	      "</B>" +
	      "&nbsp;&nbsp;" +
	      h.aStr(completionURL+"?format=html", "(Click here for more info.)"));

	  //Table of processing stats:
	  showTableHeader (h);

	  //Show the actual statistics
	  showStats (h, cd, numTasks);

	  //Unconfident tasks:
	  if(cd.getNumberOfUnconfidentTasks()>0) {
	    showUnconfidentHeader (h);
	    showUnconfidentTasks (h, cd);
	    h.eTable();
	  }
	  if (cd.getNumberOfUnconfidentTasks()>MAX_UNCONFIDENT_TO_SHOW){
	    h.p("<font size=+2>Only showing first " + MAX_UNCONFIDENT_TO_SHOW + " unconfident tasks. " + 
		"<br>See completion servlet for more info.</font>");
	  }

	  h.br();

	  //Failed tasks:
	  if(cd.getNumberOfFailedTasks()>0) {
	    showFailedHeader (h);
	    showFailedTasks (h, cd);
	    h.eTable();
	  }
	  if (cd.getNumberOfFailedTasks()>MAX_FAILED_TO_SHOW){
	    h.p("<font size=+2>Only showing first " + MAX_FAILED_TO_SHOW + " failed tasks. " + 
		"<br>See completion servlet for more info.</font>");
	  }
	}

	fullSuccess = showAdviceToUser (h, 
					getNumberOfTasks (results.values()), 
					getNumberOfUnplannedTasks (results.values()), 
					getNumberOfUnestimatedTasks (results.values()), 
					getNumberOfFailedTasks (results.values()), 
					getNumberOfFullySuccessfulTasks (results.values())); 
      }else{
	h.sFont("RED");
	h.p("<B>Could not speak to Completion PSP</B>");
	h.eFont();
      }
      h.eCenter();
    }catch(Exception e){
      h.print("Exception while trying to get completion data: "+e);
    }
    return fullSuccess;
  }

    protected void showTableHeader (HTMLizer h) {
      h.sTable();
      h.sRow();
      h.tHead("Time");
      h.tHead("# Total Tasks");
      h.tHead("# Un-planned");
      h.tHead("# Un-estimated");
      h.tHead("# Success & &LT 100% Conf.");
      h.tHead("# Success & 100% Conf.");
      h.tHead("# Failed");
      h.eRow();
    }

    protected void showStats (HTMLizer h, CompletionData cd, int numTasks) {
      h.sRow();
      h.tData(new Date(cd.getTimeMillis()));
      h.tData(numTasks);
      h.tData(cd.getNumberOfUnplannedTasks()+
	      getPercentStr(cd.getNumberOfUnplannedTasks(),numTasks));
      h.tData(cd.getNumberOfUnestimatedTasks()+
	      getPercentStr(cd.getNumberOfUnestimatedTasks(),numTasks));
      h.tData(cd.getNumberOfUnconfidentTasks()+
	      getPercentStr(cd.getNumberOfUnconfidentTasks(),numTasks));
      h.tData(cd.getNumberOfFullySuccessfulTasks()+
	      getPercentStr(cd.getNumberOfFullySuccessfulTasks(),numTasks));
      h.tData(cd.getNumberOfFailedTasks()+
	      getPercentStr(cd.getNumberOfFailedTasks(),numTasks));
      h.eRow();
      h.eTable();
    }

    protected void showUnconfidentHeader (HTMLizer h) {
      h.h3("Unconfident Tasks:");
      h.sTable();
      h.sRow();
      h.tHead("Plan Element Type");
      h.tHead("UID");
      h.tHead("Parent UID");
      h.tHead("Confidence");
      h.eRow();
    }

    protected void showUnconfidentTasks (HTMLizer h, CompletionData cd) {
      int num = (cd.getNumberOfUnconfidentTasks() > MAX_UNCONFIDENT_TO_SHOW) ?
	MAX_UNCONFIDENT_TO_SHOW : cd.getNumberOfUnconfidentTasks();

      for(int i=0;i<num;i++){
	UnconfidentTask ut=cd.getUnconfidentTaskAt(i);
	h.sRow();
	h.tData(ut.getPlanElement());
	h.tData(h.aStr(addHost(ut.getUID_URL()),
		       ut.getUID()));
	h.tData(h.aStr(addHost(ut.getParentUID_URL()),
		       ut.getParentUID()));
	h.tData(getPercentStr(ut.getConfidence()));
	h.eRow();
      }
    }

    protected void showFailedHeader (HTMLizer h) {
      h.h3("Failed Tasks:");
      h.sTable();
      h.sRow();
      h.tHead("Plan Element Type");
      h.tHead("UID");
      h.tHead("Parent UID");
      h.eRow();
    }

    protected void showFailedTasks (HTMLizer h, CompletionData cd) {
      int num = (cd.getNumberOfFailedTasks() > MAX_FAILED_TO_SHOW) ?
	MAX_FAILED_TO_SHOW : cd.getNumberOfFailedTasks();
      for(int i=0;i<num;i++){
	FailedTask ft=cd.getFailedTaskAt(i);
	h.sRow();
	h.tData(ft.getPlanElement());
	h.tData(h.aStr(addHost(ft.getUID_URL()),
		       ft.getUID()));
	h.tData(h.aStr(addHost(ft.getParentUID_URL()),
		       ft.getParentUID()));

	h.eRow();
      }
    }

    protected boolean showAdviceToUser (HTMLizer h, int numTasks,
					int numUnplanned, int numUnestimated,
					int numFailed, int numSuccess) {
      boolean fullSuccess = false;
      //Description:
      String status="";
      StringBuffer description=new StringBuffer("");
      if(numTasks==0){
	status="Yet to begin";
	description.append("No tasks have been recieved yet.");
      }else if(numUnplanned>0 || numUnestimated>0){
	status="New tasks recieved";
	description.append("New tasks have been recieved "+
			   "that don't yet have plan elements.");
      }else if(numTasks > numFailed + numSuccess){
	status="Appears busy";
	description.append("All tasks have plan elements, but not all "+
			   "allocation results have reached 100% "+
			   "confidence yet.");
      }else if(numFailed > 0){
	status="<FONT color=red>Appears complete; Has failures</FONT>";
	description.append("All tasks have allocation results that have "+
			   "100% confidence, but some are failures.\n"+
			   "<B><br>Note: Although completion is apparent, "+
			   "other parts of the society may still "+
			   "send new tasks that will need to be processed."+
			   "</B>");
      }else{
	status="<FONT color=green>Appears complete and successful</FONT>";
	description.append("All tasks have allocation results that have "+
			   "100% confidence and indicate success.\n"+
			   "<B><br>Note: Although completion is apparent, "+
			   "other parts of the society may still "+
			   "send new tasks that will need to be processed."+
			   "</B>");
	fullSuccess=true;
      }

      h.h3("Description: "+status);
      h.p(description.toString());
      
      return fullSuccess;
    }

  protected Map getResults (HTMLizer h) {
    Map results = new HashMap ();

    for (Iterator iter=config.getCompletionPSPConfig().getClusterIterator (); 
	 iter.hasNext ();) {
      String cluster = (String) iter.next ();
      if (VERBOSE)
	System.out.println ("CompletionAssessorRequestHandler.getResults - " + 
			    "getting results for " + cluster);
      CompletionPSPConfig completionConfig = config.getCompletionPSPConfig();
      CompletionPSPConnection connection=new
	CompletionPSPConnection(-1, -1,
				new CompletionPSPConfig (completionConfig, cluster),
				dbConfig,
				dbConnection,
				h);
      String completionURL=config.getCompletionPSPConfig().getUrlConnection().getURL();
      Result r=connection.perform(h);
      if(r instanceof CompletionPSPConnection.CompletionRunResult){
	CompletionData cd=((CompletionPSPConnection.CompletionRunResult)r).
	  getCompletionData();
	results.put (cluster, cd);
      }
    }

    return results;
  }

  protected double getPercentComplete (Collection results) {
    double sumCompletion=0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();

      //Determine % complete:
      for(int i=0;i<cd.getNumberOfUnconfidentTasks();i++){
	UnconfidentTask ut=cd.getUnconfidentTaskAt(i);
	sumCompletion+=ut.getConfidence();
      }
      sumCompletion+=cd.getNumberOfFullySuccessfulTasks();
      sumCompletion+=cd.getNumberOfFailedTasks();
    }
    return sumCompletion;
  }

  protected double getPercentComplete (CompletionData cd) {
    double sumCompletion=0;

    //Determine % complete:
    for(int i=0;i<cd.getNumberOfUnconfidentTasks();i++){
      UnconfidentTask ut=cd.getUnconfidentTaskAt(i);
      sumCompletion+=ut.getConfidence();
    }
    sumCompletion+=cd.getNumberOfFullySuccessfulTasks();
    sumCompletion+=cd.getNumberOfFailedTasks();

    return sumCompletion;
  }

  protected int getNumberOfUnplannedTasks (Collection results) {
    int num = 0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      num += cd.getNumberOfUnplannedTasks();
    }
    return num;
  }
  protected int getNumberOfUnestimatedTasks (Collection results) {
    int num = 0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      num += cd.getNumberOfUnestimatedTasks();
    }
    return num;
  }
  protected int getNumberOfUnconfidentTasks (Collection results) {
    int num = 0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      num += cd.getNumberOfUnconfidentTasks();
    }
    return num;
  }
  protected int getNumberOfFullySuccessfulTasks (Collection results) {
    int num = 0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      num += cd.getNumberOfFullySuccessfulTasks();
    }
    return num;
  }
  protected int getNumberOfFailedTasks (Collection results) {
    int num = 0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      num += cd.getNumberOfFailedTasks();
    }
    return num;
  }

  protected int getNumberOfTasks(Collection results) {
    int num=0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      num += cd.getNumberOfTasks ();
    }
    return num;
  }

  protected long getTimeMillis(Collection results) {
    long latest = 0;
    for (Iterator iter=results.iterator(); iter.hasNext ();) {
      CompletionData cd=(CompletionData) iter.next();
      if (cd.getTimeMillis () > latest)
	latest = cd.getTimeMillis ();
    }
    return latest;
  }

  public void sendNewHitPSP(HTMLizer h, Statement s)throws IOException{
    header(h,"Completion Assessor Data",
	   config.getViewRefresh(),
	   getURL(COM_HIT_PSP));
    boolean fullSuccess=false;
    if(config.getCompletionPSPConfig()==null){
      h.p("<Font color=red>Completion configuration data not available"+
	  "</font>");
    }else{
      fullSuccess=printHitPSP(h);
    }
    h.sCenter();
    h.h3("Actions:");
    if(fullSuccess){
      h.p("Conditions (as described above) indicate that a "+
	  "<FONT color=green>"+
	  "run should be possible at this time"+
	  "</FONT>.");
      
    }else{
      h.p("Conditions (as described above) indicate that it is "+
	  "<FONT color=red>"+
	  "<B>not</B> advisable to start a run at this time"+
	  "</FONT>.  "+
	  "However, you may do so anyway.");
    }
    h.p(h.aStr(getURL(WebServerConfig.CONTROLLER,
		      ControllerRequestHandler.COM_NEWRUN),
	       "Start a new run"));

    h.eCenter();
    footer(h);
  }

  protected String getPercentStr(int numerator, int denom){
    StringBuffer sb = new StringBuffer();
    sb.append(" <FONT COLOR=GRAY><SMALL>");
    sb.append(" (");
    sb.append(getPercentStr((double)numerator/denom));
    sb.append(")</SMALL></FONT>");
    return sb.toString();
  }

  protected String getPercentStr(double val){
    NumberFormat nf=NumberFormat.getPercentInstance();
    nf.setMaximumFractionDigits(2);
    return nf.format(val);
  }

  protected String addHost(String url){
    String newHost=config.getCompletionPSPConfig().
      getUrlConnection().getHost();
    int newPort=config.getCompletionPSPConfig().
      getUrlConnection().getPort();
    return "http://"+newHost+":"+newPort+url;
    /*
    int afterProtocol=url.indexOf("//");
    if(afterProtocol==-1)
      return url;
    afterProtocol+=2;
    int afterHostPort=url.indexOf("/",afterProtocol);
    if(afterHostPort==-1)
      return url;
    return url.substring(0,afterProtocol)+
      newHost+":"+newPort+
      url.substring(afterHostPort);
    */
  }

  //Static members:
  /////////////////

  //InnerClasses:
  ///////////////
}
