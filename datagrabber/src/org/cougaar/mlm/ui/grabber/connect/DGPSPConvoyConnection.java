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
import org.cougaar.mlm.ui.psp.transit.data.convoys.Convoy;
import org.cougaar.mlm.ui.psp.transit.data.convoys.ConvoysData;
import org.cougaar.mlm.ui.psp.transit.data.convoys.ConvoysDataFactory;

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
 * Handles getting convoy data from DataGatherer PSP
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 4/18/01
 **/
public class DGPSPConvoyConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPConvoyConnection(int id, int runID,
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
    return new ConvoysDataFactory();
  }

  //Actions:

  protected boolean updateConvoys(Statement s, Convoy c){
    boolean ret=false;
    try{
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(CONVOYS_TABLE));
      sb.append(" (");
      sb.append(COL_CONVOYID);sb.append(",");
      sb.append(COL_STARTTIME);sb.append(",");
      sb.append(COL_ENDTIME);sb.append(",");
      sb.append(COL_PRETTYNAME);sb.append(") VALUES('");
      sb.append(c.getUID());sb.append("',");
      sb.append(dbConfig.dateToSQL(c.getStartTime()));sb.append(",");
      sb.append(dbConfig.dateToSQL(c.getEndTime()));sb.append(",'");
      sb.append(c.getPrettyName());
      sb.append("')");
      ret=(s.executeUpdate(sb.toString())==1);
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not update table("+
		   getTableName(CONVOYS_TABLE)+")",e);
      return false;
    }
    return ret;
  }

  protected boolean updateConvoyMember(Statement s, Convoy c){
    boolean ret=false;
    try{
      String uid=c.getUID();
      Iterator iter=c.getConveyanceIDIterator();
      while(iter.hasNext()){
	String conveyanceUID=(String)iter.next();
	StringBuffer sb=new StringBuffer();
	sb.append("INSERT INTO ");
	sb.append(getTableName(CONVOY_MEMBER_TABLE));
	sb.append(" (");
	sb.append(COL_CONVOYID);sb.append(",");
	sb.append(COL_CONVEYANCEID);sb.append(") VALUES('");
	sb.append(uid);sb.append("','");
	sb.append(conveyanceUID);
	sb.append("')");
	ret|=(s.executeUpdate(sb.toString())==1);
      }
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not update table("+
		   getTableName(CONVOY_MEMBER_TABLE)+")",e);
      return false;
    }
    return ret;
  }

  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    ConvoysData data=(ConvoysData)obj;
    Statement s;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getConvoysIterator();
    while(iter.hasNext()){
      num++;
      setStatus("Updating convoy "+num);
      Convoy part=(Convoy)iter.next();
      boolean ok=true;
      ok=updateConvoys(s,part);
      if(halt)return;
      ok&=updateConvoyMember(s,part);
      if(halt)return;
      if(!ok)
	unsuccessful++;
    }
    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" convoy(s)");
    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " convoy(s)");
    setStatus("Done");
    if(s!=null){
      try{
	s.close();
      }catch(Exception e){e.printStackTrace();
      }
    }
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

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
