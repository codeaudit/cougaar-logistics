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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.planning.servlet.data.hierarchy.HierarchyDataFactory;
import org.cougaar.planning.servlet.data.hierarchy.HierarchyData;
import org.cougaar.planning.servlet.data.hierarchy.Organization;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.HierarchyPSPConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;

import java.sql.*;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.NumberFormat;

import org.cougaar.mlm.ui.grabber.workqueue.TimedWork;
import org.cougaar.mlm.ui.grabber.workqueue.TimedResult;

/**
 * Handles obtaining data from the PSP or Servlet
 *
 * Times out after a fixed duration, defaults to two minutes, but
 * can be set by xml tag.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/01/01
 **/
public class HierarchyConnection extends PSPConnection 
  implements HierarchyConstants, TimedWork {

  //Constants:
  ////////////

  //Variables:
  ////////////

  private HierarchyPSPConfig hConfig;
  protected long start = 0l;
  protected long duration = DEFAULT_TIMEOUT;
 
  //Constructors:
  ///////////////

  public HierarchyConnection(int id, int runID,
			     HierarchyPSPConfig hConfig, 
			     DBConfig dbConfig,
			     Connection c,
			     Logger l){
    super(id, runID, hConfig.getUrlConnection(), dbConfig, c,l);
    duration = hConfig.getUrlConnection().getTimeout();
    this.hConfig=hConfig;
  }

  //Members:
  //////////

  //Gets:

  protected String getFileName(){
    return urlConnectData.getFileName()+
      urlConnectData.getClusterName()+
      "_hierarchy.xml";
  }

  private String getRootTableName(){
    return getTableName(ORGROOTS_TABLE_NAME);
  }

  private String getOrgTableName(){
    return getTableName(ORG_TABLE_NAME);
  }

  private String getNamesTableName(){
    return getTableName(ORGNAMES_TABLE_NAME);
  }

  private String getDescendTableName(){
    return getTableName(ORGDESCEND_TABLE_NAME);
  }

  /**return the DeXMLableFactory specific to this URL connection**/
  protected DeXMLableFactory getDeXMLableFactory(){
    return new HierarchyDataFactory();
  }

  /** doesn't ask the hierarchy psp/servlet to recurse */
  protected String getQueryString(){
    return "";
  }

  //Actions:

  public Result perform(Logger l){
    start = System.currentTimeMillis ();
    logMessage(Logger.MINOR,Logger.RESULT,
	       "HierarchyConnection work " + getID () + " started.");
    return super.perform (l);
  }

  public Result getTimedOutResult () {
    HierarchyResult hr = new HierarchyResult(getID(),getRunID());
    hr.setTimedOut ();
    return hr;
  }

  private int orgRelToDBRel(int rel){
    switch(rel){
    case Organization.ADMIN_SUBORDINATE:
      return ADMIN_SUBORDINATE;
    case Organization.SUBORDINATE:
      return SUBORDINATE;
    default:
      logMessage(Logger.WARNING,Logger.GENERIC,"Unknown relation type: "+rel);
    }
    return -1;
  }

  private void updateOrgTable(Statement s, HierarchyData hd)
    throws SQLException{
    for(int i=0;i<hd.numOrgs();i++){
      Organization o=hd.getOrgDataAt(i);
      String orgID=o.getUID();
      for(int j=0;j<o.getNumRelations();j++){
	String relID=o.getRelationUIDAt(j);
	int rel=orgRelToDBRel(o.getRelationAt(j));
	if (!orgInOrgTable(s, relID, getOrgTableName()))
	  HierarchyPrepareDBTables.addToOrgTable(this,s,getOrgTableName(),
						 orgID,relID,rel);
      }
    }
    logMessage(Logger.MINOR,Logger.DB_WRITE,"Updated table("+
	       getOrgTableName()+")"); 
  }

  private void updateNamesTable(Statement s, HierarchyData hd)
    throws SQLException{
    for(int i=0;i<hd.numOrgs();i++){
      Organization o=hd.getOrgDataAt(i);
      String orgID=o.getUID();
      String prettyName=o.getPrettyName();
      if (!orgInNamesTable(s, orgID, getNamesTableName()))
	HierarchyPrepareDBTables.addToNamesTable(this,s,getNamesTableName(),
						 orgID,prettyName);
    }
    logMessage(Logger.MINOR,Logger.DB_WRITE,"Updated table("+
	       getNamesTableName()+")"); 
  }
  
  private boolean orgInOrgTable(Statement s, String orgID, String tableName) throws SQLException {
    ResultSet rs = s.executeQuery("Select * from " + tableName + " where related_id='" + orgID + "';");
    return rs.next();
  }

  private boolean orgInNamesTable(Statement s, String orgID, String tableName) throws SQLException {
    ResultSet rs = s.executeQuery("Select * from " + tableName + " where org_id='" + orgID + "';");
    return rs.next();
  }

  protected void updateDB(Connection c, DeXMLable obj){
    if (isOverdue ())
      return;

    setStatus("Starting");
    HierarchyData hd = (HierarchyData)obj;
    Statement s=null;
    
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    String table="Unknown";
    try{
      table=getOrgTableName();
      updateOrgTable(s,hd);
      table=getNamesTableName();
      updateNamesTable(s,hd);
      setStatus("Done");
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not update table("+table+")",e);
      return;
    }finally{
      if(s!=null)
	try{
	  s.close();
	}catch(Exception e){
	}
    }
  }

  /**Change the orgID to the name of the clusterID in the PSP URL -- currently
   * identical.
   * @return the clusterName corresponding to the orgID
   **/
  private String translateOrgIDToCluster(String orgID){
    return orgID;
  }
  
  protected RunResult prepResult(DeXMLable obj){
    setStatus("Starting");
    HierarchyResult hr = new HierarchyResult(getID(),getRunID());
    HierarchyData hd = (HierarchyData)obj;
    hr.setParent (hd.getRootOrgID ());

    for(int i=0;i<hd.numOrgs();i++){
      Organization o = hd.getOrgDataAt(i);
      for (int j = 0; j < o.getNumRelations (); j++) {
	hr.addClusterName(o.getRelationUIDAt (j));
      }
    }
    setStatus("Done");
    logMessage(Logger.MINOR,Logger.RESULT,"Produced Result");
    return hr;
  }

  public long getStart () { return start; }

  public void setDuration (long dur) { this.duration = dur; }
  public long getDuration () { return duration; }
  
  /** how long has the work taken, in wall-clock time */
  public long timeSpent() { return System.currentTimeMillis () - start; }

  /** returns true if spent more than the allotted time */
  public boolean isOverdue() { 
    if (start == 0l) // perform hasn't even run yet
      return false;

    boolean value = (timeSpent () > duration); 
    if (value) {
      logMessage(Logger.IMPORTANT,Logger.RESULT,"HierarchyConnection.isOverdue - work " + 
		 getID () + " timed out. Start was " + 
		 new Date (start) + " current time " + new Date(System.currentTimeMillis ()) +
		 "\nSo timeSpent " + timeSpent () + " > max dur " + duration);
    }
    else {
      NumberFormat doubleFormat=NumberFormat.getNumberInstance();
      doubleFormat.setMaximumIntegerDigits(20);
      doubleFormat.setMaximumFractionDigits(2);
      logMessage(Logger.MINOR,Logger.RESULT,"HierarchyConnection.isOverdue - work " + 
		 getID () + " - is not overdue, waited " +
		 doubleFormat.format(((double)timeSpent ()/(double)duration)*100.0d) + " % of max duration " + 
		 duration);
    }
    return value;
  }

  //Static functions:
  ///////////////////

  public static int getSociety(String s){
    for(int i=0;i<SOCIETIES.length;i++){
      if(s.equalsIgnoreCase(SOCIETIES[i])){
	return i;
      }
    }
    return SOC_UNKNOWN;
  }

  //InnerClasses:
  ///////////////

  public static class HierarchyResult implements RunResult, TimedResult{
    protected int id;
    protected int runID;

    protected Set clusters;
    protected boolean timedOut = false;
    protected String parent;

    public HierarchyResult(int id, int runID){
      this.id=id;
      this.runID=runID;
      this.clusters=new HashSet(31);
    }

    public void setTimedOut () { timedOut = true; }

    /** for timed result */
    public boolean didTimeOut  () { return timedOut; }

    /** root of the hierarchy */
    public void setParent (String parent) {
      this.parent = parent;
    }
    public String getParent () { return parent; }
 
    public void addClusterName(String clusterName){
      clusters.add(clusterName);
    }

    public int numClusterNames(){
      return clusters.size();
    }

    public Set getClusterNameSet(){
      return clusters;
    }

    public int getID(){
      return id;
    }

    public int getRunID(){
      return runID;
    }
  }
}
