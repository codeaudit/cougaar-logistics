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

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.*;

/**
 * Handles creating the DataGathererPSP DB tables.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/18/01
 **/
public class DGPSPPrepareDBTables extends PrepareDBTables 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //In HierarchyConstants.java

  //Variables:
  ////////////

  //Hints on which tables need to be created:
  private boolean makeAssetItinierary=false;
  private boolean makeConveyedLeg=false;
  private boolean makeAssetInstance=false;
  private boolean makeAssetPrototype=false;
  private boolean makeCargoCatCodeDim=false;
  private boolean makeConveyanceInstance=false;
  private boolean makeConveyancePrototype=false;
  private boolean makeLocations=false;
  private boolean makeConvoys=false;
  private boolean makeConvoyMember=false;
  private boolean makeRouteElements=false;
  private boolean makeManifest=false;

  public static boolean usePreferences = true; // until we can rip them out

  //Constructors:
  ///////////////

  public DGPSPPrepareDBTables(int id, int runID,
				  DBConfig dbConfig,
				  Connection c,
				  Logger l){
    super(id, runID, dbConfig, c,l);
  }

  //Members:
  //////////

  //Gets:

  //Actions:

  protected boolean needPrepareDB(Connection c){
    makeAssetItinierary=!isTablePresent(c,getTableName(ASSET_ITINERARY_TABLE));
    makeConveyedLeg=!isTablePresent(c,getTableName(CONVEYED_LEG_TABLE));
    makeAssetInstance=!isTablePresent(c,getTableName(ASSET_INSTANCE_TABLE));
    makeAssetPrototype=!isTablePresent(c,getTableName(ASSET_PROTOTYPE_TABLE));
    makeCargoCatCodeDim=!isTablePresent(c,getTableName(CARGO_CAT_CODE_DIM_TABLE));
    makeConveyanceInstance=!isTablePresent(c,getTableName
					   (CONV_INSTANCE_TABLE));
    makeConveyancePrototype=!isTablePresent(c,getTableName
					    (CONV_PROTOTYPE_TABLE));
    makeLocations=!isTablePresent(c,getTableName(LOCATIONS_TABLE));
    makeConvoys=!isTablePresent(c,getTableName(CONVOYS_TABLE));
    makeConvoyMember=!isTablePresent(c,getTableName(CONVOY_MEMBER_TABLE));
    makeRouteElements=!isTablePresent(c,getTableName(ROUTE_ELEMENTS_TABLE));
    makeManifest=!isTablePresent(c,getTableName(MANIFEST_TABLE));
    
    return 
      makeAssetItinierary||
      makeConveyedLeg||
      makeAssetInstance||
      makeAssetPrototype||
      makeCargoCatCodeDim||
      makeConveyanceInstance||
      makeConveyancePrototype||
      makeLocations||
      makeConvoys||
      makeConvoyMember||
      makeRouteElements||
      makeManifest;
  }

  protected void prepareDB(Connection c){
    Statement s=null;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE,"Could not create/close Statement",e);
      return;
    }

    if(makeAssetItinierary)
      prepareTable(s,getTableName(ASSET_ITINERARY_TABLE),
		   "CREATE TABLE "+getTableName(ASSET_ITINERARY_TABLE)+" ( "+
		   COL_ASSETID+" VARCHAR(255) NOT NULL,"+
		   COL_LEGID+" VARCHAR(255) NOT NULL"+
		   ")");
    if(makeConveyedLeg) {
      if (usePreferences) {
	prepareTable(s,getTableName(CONVEYED_LEG_TABLE),
		     "CREATE TABLE "+getTableName(CONVEYED_LEG_TABLE)+" ( "+
		     COL_LEGID+" VARCHAR(255) UNIQUE NOT NULL,"+
		     COL_STARTTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_ENDTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_READYAT+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_EARLIEST_END+" "+dbConfig.getDateTimeType()+
		     " NOT NULL,"+
		     COL_BEST_END+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_LATEST_END+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		     COL_ENDLOC+" VARCHAR(255) NOT NULL,"+
		     COL_LEGTYPE+" INTEGER NOT NULL,"+
		     COL_CONVEYANCEID+ " VARCHAR(255) NOT NULL,"+
		     COL_ROUTEID+" VARCHAR(255),"+
		     COL_MISSIONID+" VARCHAR(255)"+
		     ")");
      }
      else {
	prepareTable(s,getTableName(CONVEYED_LEG_TABLE),
		     "CREATE TABLE "+getTableName(CONVEYED_LEG_TABLE)+" ( "+
		     COL_LEGID+" VARCHAR(255) UNIQUE NOT NULL,"+
		     COL_STARTTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_ENDTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		     COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		     COL_ENDLOC+" VARCHAR(255) NOT NULL,"+
		     COL_LEGTYPE+" INTEGER NOT NULL,"+
		     COL_CONVEYANCEID+ " VARCHAR(255) NOT NULL,"+
		     COL_ROUTEID+" VARCHAR(255),"+
		     COL_MISSIONID+" VARCHAR(255)"+
		     ")");
      }
    }
    if(makeAssetInstance)
      prepareTable(s,getTableName(ASSET_INSTANCE_TABLE),
		   "CREATE TABLE "+getTableName(ASSET_INSTANCE_TABLE)+" ( "+
		   COL_ASSETID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_AGGREGATE+" INTEGER NOT NULL,"+
		   COL_OWNER+" VARCHAR(255) NOT NULL,"+
		   COL_PROTOTYPEID+" VARCHAR(255) NOT NULL,"+
		   COL_NAME+" VARCHAR(255) NOT NULL,"+
		   COL_ALP_ITEM_NOMEN+" VARCHAR(255)"+
		   ")");
    if(makeAssetPrototype)
      prepareTable(s,getTableName(ASSET_PROTOTYPE_TABLE),
		   "CREATE TABLE "+getTableName(ASSET_PROTOTYPE_TABLE)+" ( "+
		   COL_PROTOTYPEID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_PARENT_PROTOTYPEID+" VARCHAR(255) NULL,"+//NULL
		   COL_ASSET_CLASS+" INTEGER NOT NULL,"+
		   COL_ASSET_TYPE+" INTEGER NOT NULL,"+
		   COL_ALP_TYPEID+" VARCHAR(255) NOT NULL,"+
		   COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL,"+
		   COL_IS_LOW_FIDELITY+" VARCHAR(5) NOT NULL"+
		   ")");
    if(makeCargoCatCodeDim)
      prepareTable(s,getTableName(CARGO_CAT_CODE_DIM_TABLE),
		   "CREATE TABLE "+getTableName(CARGO_CAT_CODE_DIM_TABLE)+" ( "+
		   COL_PROTOTYPEID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_WEIGHT+" DOUBLE NOT NULL,"+
		   COL_WIDTH+" DOUBLE NOT NULL,"+
		   COL_HEIGHT+" DOUBLE NOT NULL,"+
		   COL_DEPTH+" DOUBLE NOT NULL,"+
		   COL_AREA+" DOUBLE NOT NULL,"+
		   COL_VOLUME+" DOUBLE NOT NULL,"+
		   COL_CARGO_CAT_CODE+" VARCHAR(3) NOT NULL"+
		   ")");
    if(makeConveyanceInstance)
      prepareTable(s,getTableName(CONV_INSTANCE_TABLE),
		   "CREATE TABLE "+getTableName(CONV_INSTANCE_TABLE)+" ( "+
		   COL_CONVEYANCEID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_BUMPERNO+" VARCHAR(255) NOT NULL,"+
		   COL_BASELOC+" VARCHAR(255) NOT NULL,"+
		   COL_OWNER+" VARCHAR(255) NOT NULL,"+
		   COL_PROTOTYPEID+" VARCHAR(255) NOT NULL,"+
		   COL_SELFPROP+" INTEGER NOT NULL,"+
		   COL_ALP_ITEM_NOMEN+" VARCHAR(255)"+
		   ")");
    if(makeConveyancePrototype)
      prepareTable(s,getTableName(CONV_PROTOTYPE_TABLE),
		   "CREATE TABLE "+getTableName(CONV_PROTOTYPE_TABLE)+" ( "+
		   COL_PROTOTYPEID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_CONVEYANCE_TYPE+" INTEGER NOT NULL,"+
		   COL_VOL_CAP+" DOUBLE NOT NULL,"+
		   COL_AREA_CAP+" DOUBLE NOT NULL,"+
		   COL_WEIGHT_CAP+" DOUBLE NOT NULL,"+
		   COL_AVE_SPEED+" DOUBLE NOT NULL,"+
		   COL_ALP_TYPEID+" VARCHAR(255) NOT NULL,"+
		   COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL"+
		   ")");
    if(makeLocations)
      prepareTable(s,getTableName(LOCATIONS_TABLE),
		   "CREATE TABLE "+getTableName(LOCATIONS_TABLE)+" ( "+
		   COL_LOCID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_LAT+" DOUBLE NOT NULL,"+
		   COL_LON+" DOUBLE NOT NULL,"+
		   COL_GEOLOC+" CHAR(4) NULL,"+// NULL
		   COL_ICAO+" VARCHAR(255) NULL,"+//NULL
		   COL_PRETTYNAME+" VARCHAR(255) NOT NULL"+
		   ")");
    if(makeConvoys){
      prepareTable(s,getTableName(CONVOYS_TABLE),
		   "CREATE TABLE "+getTableName(CONVOYS_TABLE)+" ( "+
		   COL_CONVOYID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_STARTTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		   COL_ENDTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		   COL_PRETTYNAME+" VARCHAR(255)"+
		   ")");
    }
    if(makeConvoyMember){
      prepareTable(s,getTableName(CONVOY_MEMBER_TABLE),
		   "CREATE TABLE "+getTableName(CONVOY_MEMBER_TABLE)+" ( "+
		   COL_CONVOYID+" VARCHAR(255) NOT NULL,"+
		   COL_CONVEYANCEID+" VARCHAR(255) NOT NULL"+
		   ")");      
    }
    if(makeRouteElements){
      prepareTable(s,getTableName(ROUTE_ELEMENTS_TABLE),
		   "CREATE TABLE "+getTableName(ROUTE_ELEMENTS_TABLE)+" ( "+
		   COL_ROUTEID+" VARCHAR(255) NOT NULL,"+
		   COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		   COL_ENDLOC+" VARCHAR(255) NOT NULL,"+
		   COL_ROUTE_ELEM_NUM+" INTEGER NOT NULL"+
		   ")");      
    }
    if(makeManifest){
      prepareTable(s,getTableName(MANIFEST_TABLE),
		   "CREATE TABLE "+getTableName(MANIFEST_TABLE)+" ( "+
		   COL_MANIFEST_ITEM_ID+" VARCHAR(255) UNIQUE NOT NULL,"+
		   COL_ASSETID+" VARCHAR(255),"+
		   COL_NAME+" VARCHAR(255) NOT NULL,"+
		   COL_ALP_TYPEID+" VARCHAR(255) NOT NULL,"+
		   COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL,"+
		   COL_WEIGHT+" DOUBLE NOT NULL"+
		   ")");      
    }

    try{
      s.close();
    }catch(SQLException e){
      logMessage(Logger.ERROR,Logger.DB_WRITE,"Could not close Statement",e);
    }
  }

  //Static functions:
  ///////////////////

  //InnerClasses:
  ///////////////
}
