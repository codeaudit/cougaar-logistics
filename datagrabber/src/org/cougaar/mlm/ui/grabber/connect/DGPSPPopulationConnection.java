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
import org.cougaar.mlm.ui.psp.transit.data.population.ConveyanceInstance;
import org.cougaar.mlm.ui.psp.transit.data.population.ConveyancePrototype;
import org.cougaar.mlm.ui.psp.transit.data.population.PopulationData;
import org.cougaar.mlm.ui.psp.transit.data.population.PopulationDataFactory;

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
 * Handles getting population data from DataGatherer PSP
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/19/01
 **/
public class DGPSPPopulationConnection extends DGPSPConnection 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public DGPSPPopulationConnection(int id, int runID,
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
    return new PopulationDataFactory();
  }

  //Actions:

  protected boolean updateConveyanceInstance(Statement s, 
					     ConveyanceInstance ci){
    boolean ret=false;
    try{
      StringBuffer sb=new StringBuffer();
      boolean hasNomen=!(ci.itemNomen==null||ci.itemNomen.equals(""));
      sb.append("INSERT INTO ");
      sb.append(getTableName(CONV_INSTANCE_TABLE));
      sb.append(" (");
      sb.append(COL_CONVEYANCEID);sb.append(",");
      sb.append(COL_BUMPERNO);sb.append(",");
      sb.append(COL_BASELOC);sb.append(",");
      sb.append(COL_OWNER);sb.append(",");
      sb.append(COL_PROTOTYPEID);sb.append(",");
      sb.append(COL_SELFPROP);
      if(hasNomen){
	sb.append(",");
	sb.append(COL_ALP_ITEM_NOMEN);
      }
      sb.append(") VALUES('");
      sb.append(ci.UID);sb.append("','");
      sb.append(ci.bumperNo);sb.append("','");
      sb.append(ci.homeLocID);sb.append("','");
      sb.append(ci.ownerID);sb.append("','");
      sb.append(ci.prototypeUID);sb.append("',");
      sb.append((ci.selfPropelled?"1":"0"));
      if(hasNomen){
	sb.append(",'");
	sb.append(ci.itemNomen);
	sb.append("'");
      }
      sb.append(")");
      ret=(s.executeUpdate(sb.toString())==1);
    }catch(SQLException e){
      //If this is a duplicate insertion && we are self-prop its ok,
      //otherwise an error.  We assume that no TOPS clusters will
      //ever have duplicate assets
      if(dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())&&
	 ci.selfPropelled){
	if (logger.isTrivialEnabled()) {
	  logMessage(Logger.TRIVIAL,Logger.RESULT,
		     "Found duplicate self-propelled instance");
	}
	return true;
      }else{
	haltForError(Logger.DB_WRITE,"Could not update table("+
		     getTableName(CONV_INSTANCE_TABLE)+")"+
		     "["+e.getSQLState()+"]",e);
	return false;
      }
    }
    return ret;
  }

  protected boolean updateConveyancePrototype(Statement s, 
					     ConveyancePrototype cp){
    boolean ret=false;
    try{
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");sb.append(getTableName(CONV_PROTOTYPE_TABLE));
      sb.append(" (");
      sb.append(COL_PROTOTYPEID);sb.append(",");
      sb.append(COL_CONVEYANCE_TYPE);sb.append(",");
      sb.append(COL_VOL_CAP);sb.append(",");
      sb.append(COL_AREA_CAP);sb.append(",");
      sb.append(COL_WEIGHT_CAP);sb.append(",");
      sb.append(COL_AVE_SPEED);sb.append(",");
      sb.append(COL_ALP_TYPEID);sb.append(",");
      sb.append(COL_ALP_NOMENCLATURE);
      sb.append(") VALUES('");
      sb.append(cp.UID);sb.append("',");
      sb.append(pspToDBConvayanceTypeClass(cp.conveyanceType));sb.append(",");
      sb.append(dbConfig.getDBDouble(cp.volCap));sb.append(",");
      sb.append(dbConfig.getDBDouble(cp.areaCap));sb.append(",");
      sb.append(dbConfig.getDBDouble(cp.weightCap));sb.append(",");
      sb.append(dbConfig.getDBDouble(cp.aveSpeed));sb.append(",'");
      sb.append(cp.alpTypeID);sb.append("','");
      sb.append(cp.nomenclature.replace('\'','_'));
      sb.append("')");
      ret=(s.executeUpdate(sb.toString())==1);
    }catch(SQLException e){
      if(!dbConfig.getUniqueViolatedErrorCode().equals(e.getSQLState())){
	haltForError(Logger.DB_WRITE,"Could not update table("+
		     getTableName(CONV_PROTOTYPE_TABLE)+")"+
		     "["+e.getSQLState()+"]",e);
	return false;
      }else
	return true;
    }
    return ret;
  }

  protected void updateDB(Connection c, DeXMLable obj){
    setStatus("Starting");
    PopulationData data=(PopulationData)obj;
    Statement s;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_WRITE,"Could not create Statement",e);
      return;
    }
    setStatus("Writing Conveyance Instances");
    int num=0;
    int unsuccessful=0;
    Iterator iter=data.getInstancesIterator();
    while(iter.hasNext()){
      num++;
      setStatus("Updating instance "+num);
      ConveyanceInstance part=(ConveyanceInstance)iter.next();
      if(!updateConveyanceInstance(s,part))
	unsuccessful++;
      if(halt)return;
    }
    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" conveyance instance(s)");
    if(unsuccessful>0)
      logMessage(Logger.WARNING,Logger.DB_WRITE,
		 getClusterName()+" could not add "+unsuccessful+
		 " conveyance instance(s)");

    setStatus("Writing Conveyance Prototypes");
    num=0;
    unsuccessful=0;
    iter=data.getPrototypesIterator();
    while(iter.hasNext()){
      num++;
      setStatus("Updating prototype "+num);
      ConveyancePrototype part=(ConveyancePrototype)iter.next();
      updateConveyancePrototype(s,part);
      if(halt)return;
    }
    logMessage(Logger.TRIVIAL,Logger.DB_WRITE,
	       getClusterName()+" added "+num+" conveyance prototype(s)");
    if(unsuccessful>0)
	logMessage(Logger.WARNING,Logger.DB_WRITE,
		   getClusterName()+" could not add "+unsuccessful+
		   " conveyance prototypes(s)");
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

  public int pspToDBConvayanceTypeClass(int ct){
    switch(ct){
    default:
    case ConveyancePrototype.ASSET_TYPE_UNKNOWN:
      return CONV_TYPE_UNKNOWN;
    case ConveyancePrototype.ASSET_TYPE_TRUCK:
      return CONV_TYPE_TRUCK;
    case ConveyancePrototype.ASSET_TYPE_TRAIN:
      return CONV_TYPE_TRAIN;
    case ConveyancePrototype.ASSET_TYPE_PLANE:
      return CONV_TYPE_PLANE;
    case ConveyancePrototype.ASSET_TYPE_SHIP:
      return CONV_TYPE_SHIP;
    case ConveyancePrototype.ASSET_TYPE_DECK:
      return CONV_TYPE_DECK;
    case ConveyancePrototype.ASSET_TYPE_PERSON:
      return CONV_TYPE_PERSON;
    case ConveyancePrototype.ASSET_TYPE_FACILITY:
      return CONV_TYPE_FACILITY;
    case ConveyancePrototype.ASSET_TYPE_SELF_PROPELLABLE:
      return CONV_TYPE_SELF_PROPELLABLE;
    }
  }
 
  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
