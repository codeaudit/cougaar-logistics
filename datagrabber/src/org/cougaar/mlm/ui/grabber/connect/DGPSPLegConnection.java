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
import org.cougaar.mlm.ui.psp.transit.data.legs.Leg;
import org.cougaar.mlm.ui.psp.transit.data.legs.LegsData;
import org.cougaar.mlm.ui.psp.transit.data.legs.LegsDataFactory;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DataGathererPSPConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

import java.sql.*;

import java.util.Iterator;

/**
 * Handles getting leg data from DataGatherer PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/19/01
 **/
public class DGPSPLegConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPLegConnection(int id, int runID,
			    DataGathererPSPConfig pspConfig, 
			    DBConfig dbConfig,
			    Connection c,
			    Logger l){
    super(id, runID, pspConfig, dbConfig, c,l);
  }

  //Members:
  //////////

  //Gets:

  /**return the DeXMLableFactory specific to this URL connection**/
  protected DeXMLableFactory getDeXMLableFactory(){
    return new LegsDataFactory();
  }

  //Actions:

  /** 
   * <pre>
   * Uses two prepared statements to write asset itinerary and leg information to 
   * database. 
   * The LegsData for the agent (= parameter obj) becomes a batch added to  
   * both prepared statements.  
   * The LegsData gets translated into assetitinerary rows and conveyedleg rows.
   * The commit is done in TopsRun, when the epochs change from "obtain legs" to 
   * "obtain instances".
   * </pre>
   * @param c connection to send sql statements to
   * @param obj the LegsData for the agent
   */
  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    LegsData data=(LegsData)obj;
    PreparedStatement legPS, itineraryPS;

    try{
      legPS = getLegPreparedStatement (c);
      itineraryPS = getItineraryPreparedStatement (c);
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getLegsIterator();
    setStatus("Creating batch updates for legs.");
    while(iter.hasNext()){
      num++;
      //setStatus("Updating leg "+num);
      Leg part=(Leg)iter.next();
      if(!part.isDetail){
	boolean ok=true;
	ok=updateAssetItinerary(itineraryPS,part);
	if(halt)return;
	ok&=updateConveyedLeg(legPS,part);
	if(halt)return;
	if(!ok)
	  unsuccessful++;
      }
    }
    try{
      itineraryPS.executeBatch();
      legPS.executeBatch();
    }catch(SQLException e){
      logMessage(Logger.WARNING,Logger.DB_WRITE,"SQL Error - " + e);
      e.printStackTrace ();
    }

    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" leg(s)");
    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " leg(s)");

    if(legPS!=null)
      try{ legPS.close(); }catch(Exception e){}
    if(itineraryPS!=null)
      try{ itineraryPS.close(); }catch(Exception e){}

    setStatus("Done");
  }

  protected PreparedStatement getLegPreparedStatement (Connection con) throws SQLException {
    StringBuffer sb=new StringBuffer();

    sb.append("INSERT INTO ");
    sb.append(getTableName(CONVEYED_LEG_TABLE));
    sb.append(" (");
    sb.append(COL_LEGID);sb.append(",");
    sb.append(COL_STARTTIME);sb.append(",");
    sb.append(COL_ENDTIME);sb.append(",");
    sb.append(COL_READYAT);sb.append(",");
    sb.append(COL_EARLIEST_END);sb.append(",");
    sb.append(COL_BEST_END);sb.append(",");
    sb.append(COL_LATEST_END);sb.append(",");
    sb.append(COL_STARTLOC);sb.append(",");
    sb.append(COL_ENDLOC);sb.append(",");
    sb.append(COL_LEGTYPE);sb.append(",");
    sb.append(COL_CONVEYANCEID); sb.append(",");
    sb.append(COL_ROUTEID); sb.append(",");
    sb.append(COL_MISSIONID);
    sb.append(") VALUES(");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(")");
    return con.prepareStatement(sb.toString());
  }

  protected PreparedStatement getItineraryPreparedStatement(Connection con) throws SQLException {
    StringBuffer sb=new StringBuffer();

    sb.append("INSERT INTO ");
    sb.append(getTableName(ASSET_ITINERARY_TABLE));
    sb.append(" (");
    sb.append(COL_ASSETID);sb.append(",");
    sb.append(COL_LEGID);
    sb.append(") VALUES(");
    sb.append("?");sb.append(",");
    sb.append("?");sb.append(")");

    return con.prepareStatement(sb.toString());
  }

  protected boolean updateAssetItinerary(PreparedStatement s, Leg l){
    boolean ret=false;
    try{
      Iterator iter=l.getCarriedAssetsIterator();
      while(iter.hasNext()){
	String assetUID=(String)iter.next();
	s.setString(1, assetUID);
	s.setString(2, l.UID);
	s.addBatch ();
      }
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could add batch for table("+
		   getTableName(ASSET_ITINERARY_TABLE)+")",e);
      return false;
    }
    return true;
  }

  protected boolean updateConveyedLeg(PreparedStatement s, Leg l){
    boolean ret=false;
    try{
      s.setString(1, l.UID);
      s.setString(2, dbConfig.dateToSQL(l.startTime));
      s.setString(3, dbConfig.dateToSQL(l.endTime));
      s.setString(4, dbConfig.dateToSQL(l.readyAtTime));
      s.setString(5, dbConfig.dateToSQL(l.earliestEndTime));
      s.setString(6, dbConfig.dateToSQL(l.bestEndTime));
      s.setString(7, dbConfig.dateToSQL(l.latestEndTime));
      s.setString(8, l.startLoc);
      s.setString(9, l.endLoc);
      s.setInt(10, pspToDBLegType(l.legType));
      s.setString(11, l.conveyanceUID);
      s.setString(12, l.routeUID);
      s.setString(13, l.missionUID);
      s.addBatch();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not add batch to table("+
		   getTableName(CONVEYED_LEG_TABLE)+")",e);
      return false;
    }
    return true;
  }

  protected RunResult prepResult(DeXMLable obj){
    setStatus("Starting");
    RunResult rr = new SuccessRunResult(getID(),getRunID());
    setStatus("Done");
    logMessage(Logger.MINOR,Logger.RESULT,"Produced Result");
    return rr;
  }

  //Converters:
  //===========

  public int pspToDBLegType(int type){
    switch(type){
    case Leg.LEG_TYPE_UNKNOWN:
      return LEG_TYPE_UNKNOWN;
    case Leg.LEG_TYPE_TRANSPORTING:
      return LEG_TYPE_TRANSPORTING;
    case Leg.LEG_TYPE_LOADING:
      return LEG_TYPE_LOADING;
    case Leg.LEG_TYPE_UNLOADING:
      return LEG_TYPE_UNLOADING;
    case Leg.LEG_TYPE_POSITIONING:
      return LEG_TYPE_POSITIONING;
    case Leg.LEG_TYPE_RETURNING:
      return LEG_TYPE_RETURNING;
    case Leg.LEG_TYPE_REFUELING:
      return LEG_TYPE_REFUELING;
    }
    logMessage(Logger.WARNING,Logger.DB_WRITE,"Unknown Leg Type: "+type);
    return LEG_TYPE_UNKNOWN;
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
