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
import org.cougaar.mlm.ui.psp.transit.data.prototypes.Prototype;
import org.cougaar.mlm.ui.psp.transit.data.prototypes.PrototypesData;
import org.cougaar.mlm.ui.psp.transit.data.prototypes.PrototypesDataFactory;

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
 * Handles getting prototype data from DataGatherer PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/19/01
 **/
public class DGPSPPrototypeConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPPrototypeConnection(int id, int runID,
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
    return new PrototypesDataFactory();
  }

  //Actions:

  protected boolean updateAssetPrototype(Statement s, Prototype p){
    boolean ret=false;
	StringBuffer sb=new StringBuffer();
    try{
      sb.append("INSERT INTO ");
      sb.append(getTableName(ASSET_PROTOTYPE_TABLE));
      sb.append(" (");
      sb.append(COL_PROTOTYPEID);sb.append(",");
      sb.append(COL_PARENT_PROTOTYPEID);sb.append(",");
      sb.append(COL_ASSET_CLASS);sb.append(",");
      sb.append(COL_ASSET_TYPE);sb.append(",");
      sb.append(COL_WEIGHT);sb.append(",");
      sb.append(COL_WIDTH);sb.append(",");
      sb.append(COL_HEIGHT);sb.append(",");
      sb.append(COL_DEPTH);sb.append(",");
      sb.append(COL_ALP_TYPEID);sb.append(",");
      sb.append(COL_ALP_NOMENCLATURE);
      sb.append(") VALUES('");
      sb.append(p.UID);sb.append("',");
      sb.append(((p.parentUID==null||p.parentUID.equals(""))?
		 "NULL,":
		 ("'"+p.parentUID+"',")));//Could be NULL
      sb.append(Integer.toString(pspToDBAssetClass(p.assetClass)));
      sb.append(",");
      sb.append(Integer.toString(pspToDBAssetType(p.assetType)));
      sb.append(",");
      sb.append(dbConfig.getDBDouble(p.weight));sb.append(",");
      sb.append(dbConfig.getDBDouble(p.width));sb.append(",");
      sb.append(dbConfig.getDBDouble(p.height));sb.append(",");
      sb.append(dbConfig.getDBDouble(p.depth));sb.append(",'");
      sb.append(p.alpTypeID);sb.append("','");
      sb.append(p.nomenclature.replace('\'','_'));sb.append("')");
      ret=(s.executeUpdate(sb.toString())==1);
    }catch(SQLException e){
	  String message1 = "Could not update table("+getTableName(ASSET_PROTOTYPE_TABLE)+")"+ 
		"["+e.getSQLState()+"]<br>sql was : " + sb;
      if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){
		haltForError(Logger.DB_WRITE, message1, e);
	
	return false;
      }else
	return true;
    }catch(Exception e){
      String message1 = "Could not update table("+getTableName(ASSET_PROTOTYPE_TABLE)+")"+ 
	"<br>sql was : " + sb + "<br>Exception was " + e;
      System.err.println ("Exception was " + e);
      e.printStackTrace ();
      logMessage(Logger.WARNING,Logger.DB_WRITE,message1);
      return false;
    }
    return ret;
  }

  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    PrototypesData data=(PrototypesData)obj;
    Statement s;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getPrototypesIterator();
    while(iter.hasNext()){
      num++;
      setStatus("Updating prototype "+num);
      Prototype part=(Prototype)iter.next();
      if(!updateAssetPrototype(s,part))
	unsuccessful++;
      if(halt)return;
    }
    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" prototype(s)");
    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " prototype(s)");
    setStatus("Done");
    
    if(s!=null){
      try{
	s.close();
      }catch(Exception e){
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
 
  public int pspToDBAssetClass(int ac){
    switch(ac){
    case Prototype.ASSET_CLASS_UNKNOWN:
      return ASSET_CLASS_UNKNOWN;
    case Prototype.ASSET_CLASS_1:
      return ASSET_CLASS_1;
    case Prototype.ASSET_CLASS_2:
      return ASSET_CLASS_2;
    case Prototype.ASSET_CLASS_3:
      return ASSET_CLASS_3;
    case Prototype.ASSET_CLASS_4:
      return ASSET_CLASS_4;
    case Prototype.ASSET_CLASS_5:
      return ASSET_CLASS_5;
    case Prototype.ASSET_CLASS_6:
      return ASSET_CLASS_6;
    case Prototype.ASSET_CLASS_7:
      return ASSET_CLASS_7;
    case Prototype.ASSET_CLASS_8:
      return ASSET_CLASS_8;
    case Prototype.ASSET_CLASS_9:
      return ASSET_CLASS_9;
    case Prototype.ASSET_CLASS_10:
      return ASSET_CLASS_10;
    case Prototype.ASSET_CLASS_CONTAINER:
      return ASSET_CLASS_CONTAINER;
    case Prototype.ASSET_CLASS_PERSON:
      return ASSET_CLASS_PERSON;
    }
    logMessage(Logger.WARNING,Logger.DB_WRITE,"Unknown Asset Class: "+ac);
    return ASSET_CLASS_UNKNOWN;
  }

  public int pspToDBAssetType(int at){
    switch(at){
    case Prototype.ASSET_TYPE_ASSET:
      return ASSET_TYPE_ASSET;
    case Prototype.ASSET_TYPE_CONTAINER:
      return ASSET_TYPE_CONTAINER;
    }
    logMessage(Logger.WARNING,Logger.DB_WRITE,"Unknown Asset Type: "+at);
    return ASSET_TYPE_ASSET;
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
