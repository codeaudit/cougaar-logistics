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

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.mlm.ui.psp.transit.data.locations.Location;
import org.cougaar.mlm.ui.psp.transit.data.locations.LocationsData;
import org.cougaar.mlm.ui.psp.transit.data.locations.LocationsDataFactory;

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
 * Handles getting location data from DataGatherer PSP
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/19/01
 **/
public class DGPSPLocationConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPLocationConnection(int id, int runID,
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
    return new LocationsDataFactory();
  }

  //Actions:

  protected boolean updateLocation(Statement s, Location l){
    boolean ret=false;
    try{
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");sb.append(getTableName(LOCATIONS_TABLE));
      sb.append(" (");
      sb.append(COL_LOCID);sb.append(",");
      sb.append(COL_LAT);sb.append(",");
      sb.append(COL_LON);sb.append(",");
      sb.append(COL_GEOLOC);sb.append(",");
      sb.append(COL_ICAO);sb.append(",");
      sb.append(COL_PRETTYNAME);
      sb.append(") VALUES('");
      sb.append(l.UID);sb.append("',");
      sb.append(l.lat);sb.append(",");
      sb.append(l.lon);sb.append(",");
      sb.append(((l.geoLoc==null||l.geoLoc.equals(""))?
		 "NULL":("'"+l.geoLoc+"'")));
      sb.append(",");
      sb.append(((l.icao==null||l.icao.equals(""))?
		 "NULL":("'"+l.icao+"'")));
      sb.append(",'");
      sb.append(l.prettyName);sb.append("')");
      ret=(s.executeUpdate(sb.toString())==1);
    }catch(SQLException e){
      if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){
	haltForError(Logger.DB_WRITE,"Could not update table("+
		     getTableName(LOCATIONS_TABLE)+")"+
		     "["+e.getSQLState()+"]",e);
	return false;
      }else
	return true;
    }
    return ret;
  }

  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    LocationsData data=(LocationsData)obj;
    Statement s;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getLocationsIterator();
    while(iter.hasNext()){
      num++;
      setStatus("Updating location "+num);
      Location part=(Location)iter.next();
      if(!updateLocation(s,part))
	unsuccessful++;
      if(halt)return;
    }
    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" location(s)");
    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " location(s)");
    setStatus("Done");
    if(s!=null){
      try{
	s.close();
      }catch(Exception e){e.printStackTrace();
      }
    }
  }


  //Converters:
  //===========

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
