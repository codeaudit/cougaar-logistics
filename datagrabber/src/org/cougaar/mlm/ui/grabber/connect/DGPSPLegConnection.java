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
package org.cougaar.mlm.ui.grabber.connect;

import java.sql.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.util.UID;

import org.cougaar.mlm.ui.grabber.config.DataGathererPSPConfig;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.config.URLConnectionData;

import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.SuccessRunResult;

import org.cougaar.mlm.ui.grabber.logger.Logger;

import org.cougaar.mlm.ui.grabber.workqueue.Result;

import org.cougaar.mlm.ui.psp.transit.data.legs.Leg;
import org.cougaar.mlm.ui.psp.transit.data.legs.LegsData;
import org.cougaar.mlm.ui.psp.transit.data.legs.LegsDataFactory;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;

/**
 * Handles getting leg data from DataGatherer PSP
 *
 * @since 2/19/01
 **/
public class DGPSPLegConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////
  boolean writePreferences = false;

  //Variables:
  ////////////

  protected Map assetUIDToString = new HashMap();
  protected Map legUIDToString   = new HashMap();
  protected Map conveyanceUIDToString = new HashMap();
  protected Map routeUIDToString = new HashMap();

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
      logMessage(Logger.MINOR,Logger.DB_WRITE,
		 getClusterName()+" created prepared statements.");
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

	if ((num+1) % 1000 == 0) {
	  try{
	    logMessage(Logger.MINOR,Logger.DB_WRITE,
		       getClusterName()+" executing a prepared batch of a thousand legs, " + num + " so far ");
	    itineraryPS.executeBatch();
	    legPS.executeBatch();
	  }catch(SQLException e){
	    logMessage(Logger.WARNING,Logger.DB_WRITE,"While executing batch, got SQL Error - " + e);
	    e.printStackTrace ();
	  }
	}
      }
    }

    try{
      logMessage(Logger.MINOR,Logger.DB_WRITE,
		 getClusterName()+" executing the last prepared batch, " + num + " total legs done.");
      itineraryPS.executeBatch();
      legPS.executeBatch();
    }catch(SQLException e){
      logMessage(Logger.WARNING,Logger.DB_WRITE,"SQL Error - " + e);
      e.printStackTrace ();
    }

    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " leg(s)");

    try { 
      legPS.close(); 
    } catch(Exception e){
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" got exception closing prepared leg statement " + legPS + 
		 " - exception was : " + e);
      e.printStackTrace ();
    }

    try { 
      itineraryPS.close(); 
    } catch(Exception e){ 
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" got exception closing prepared itinerary statement " + itineraryPS + 
		 " - exception was : " + e);
      e.printStackTrace ();
    }

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

    if (writePreferences) {
      sb.append(COL_READYAT);sb.append(",");
      sb.append(COL_EARLIEST_END);sb.append(",");
      sb.append(COL_BEST_END);sb.append(",");
      sb.append(COL_LATEST_END);sb.append(",");
    }

    sb.append(COL_STARTLOC);sb.append(",");
    sb.append(COL_ENDLOC);sb.append(",");
    sb.append(COL_LEGTYPE);sb.append(",");
    sb.append(COL_CONVEYANCEID); sb.append(",");
    sb.append(COL_ROUTEID);
    sb.append(") VALUES(");
    appendQueryParams(sb, 3);

    if (writePreferences) {
        appendQueryParams(sb, 4);
    }

    appendQueryParams(sb, 4);
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
    try{
      Iterator iter=l.getCarriedAssetsIterator();
      while(iter.hasNext()){
	UID assetUID=(UID)iter.next();
	s.setString(1, getUID(assetUID, assetUIDToString));
	s.setString(2, getUID(l.UID, legUIDToString));
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
    try{
      int col = 0;
      s.setString(++col, getUID(l.UID, legUIDToString));
      s.setString(++col, dbConfig.dateToSQL(l.startTime));
      s.setString(++col, dbConfig.dateToSQL(l.endTime));

      if (writePreferences) {
	s.setString(++col, dbConfig.dateToSQL(l.readyAtTime));
	s.setString(++col, dbConfig.dateToSQL(l.earliestEndTime));
	s.setString(++col, dbConfig.dateToSQL(l.bestEndTime));
	s.setString(++col, dbConfig.dateToSQL(l.latestEndTime));
      }

      s.setString(++col, l.startLoc);
      s.setString(++col, l.endLoc);
      s.setInt(++col, pspToDBLegType(l.legType));
      s.setString(++col, getUID (l.conveyanceUID, conveyanceUIDToString));
      s.setString(++col, (l.routeUID != null) ? getUID (l.routeUID, routeUIDToString) : "null");
      s.addBatch();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not add batch to table("+
		   getTableName(CONVEYED_LEG_TABLE)+")",e);
      return false;
    }
    return true;
  }

  protected String getUID (UID uid, Map uidToString) {
    String returnedUID = (String) uidToString.get (uid);
    if (returnedUID == null) {
      uidToString.put (uid, (returnedUID = uid.toString()));
    }
    return returnedUID;
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
