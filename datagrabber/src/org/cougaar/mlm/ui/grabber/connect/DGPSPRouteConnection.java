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

import org.cougaar.core.util.UID;

import org.cougaar.planning.servlet.data.xml.DeXMLable;
import org.cougaar.planning.servlet.data.xml.DeXMLableFactory;
import org.cougaar.mlm.ui.psp.transit.data.routes.Route;
import org.cougaar.mlm.ui.psp.transit.data.routes.RoutesData;
import org.cougaar.mlm.ui.psp.transit.data.routes.RoutesDataFactory;

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
 * Handles getting route data from DataGatherer PSP
 *
 * @since 4/18/01
 **/
public class DGPSPRouteConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPRouteConnection(int id, int runID,
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
    return new RoutesDataFactory();
  }

  //Actions:

  protected boolean updateRouteElements(Statement s, Route r){
    boolean ret=false;
    try{
      UID uid=r.getUID();
      Iterator iter=r.getSegmentLocIDIterator();

      //Go through and create segments w/ start end from linear list:
      int elemNum=0;
      String lastLoc=null;
      while(iter.hasNext()){
	String locUID=(String)iter.next();
	if(lastLoc==null){
	  lastLoc=locUID;
	}else{
	  StringBuffer sb=new StringBuffer();
	  sb.append("INSERT INTO ");
	  sb.append(getTableName(ROUTE_ELEMENTS_TABLE));
	  sb.append(" (");
	  sb.append(COL_ROUTEID);sb.append(",");
	  sb.append(COL_STARTLOC);sb.append(",");
	  sb.append(COL_ENDLOC);sb.append(",");
	  sb.append(COL_ROUTE_ELEM_NUM);
	  sb.append(") VALUES('");
	  sb.append(uid);sb.append("','");
	  sb.append(lastLoc);sb.append("','");
	  sb.append(locUID);sb.append("',");
	  sb.append(elemNum);
	  sb.append(")");
	  ret|=(s.executeUpdate(sb.toString())==1);
	  lastLoc=locUID;
	  elemNum++;
	}
      }
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not update table("+
		   getTableName(ROUTE_ELEMENTS_TABLE)+")",e);
      return false;
    }
    return ret;
  }

  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    RoutesData data=(RoutesData)obj;
    Statement s;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getRoutesIterator();
    while(iter.hasNext()){
      num++;
      setStatus("Updating route "+num);
      Route part=(Route)iter.next();
      boolean ok=true;
      ok=updateRouteElements(s,part);
      if(halt)return;
      if(!ok)
	unsuccessful++;
    }
    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" route(s)");
    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " route(s)");
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
