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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.workqueue.Result;
import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.controller.RunResult;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.*;

/**
 * Handles creating the DataGathererPSP Indexes etc.
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/18/01
 **/
public class DGPSPPostPass extends PrepareDBTables 
  implements DGPSPConstants{

  //Constants:
  ////////////

  //In HierarchyConstants.java

  //Variables:
  ////////////

  //Hints on which tables need to be created:

  //Constructors:
  ///////////////

  public DGPSPPostPass(int id, int runID,
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
    return true;
  }

  protected void prepareDB(Connection c){
    Statement s=null;
    try{
      s = c.createStatement();
    }catch(SQLException e){
      haltForError(Logger.DB_STRUCTURE,"Could not create/close Statement",e);
      return;
    }

    //COMMENTED LINES ARE UNIQUE, SO DB SHOULD ALREADY HAVE INDEXES IMPLICITLY

    createIndex(s,getTableName(ASSET_ITINERARY_TABLE),COL_ASSETID);
    createIndex(s,getTableName(ASSET_ITINERARY_TABLE),COL_LEGID);

    //    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_LEGID);
    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_STARTTIME);
    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_ENDTIME);
    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_STARTLOC);
    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_ENDLOC);
    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_CONVEYANCEID);
    createIndex(s,getTableName(CONVEYED_LEG_TABLE),COL_LEGTYPE);

    //    createIndex(s,getTableName(ASSET_INSTANCE_TABLE),COL_ASSETID);
    createIndex(s,getTableName(ASSET_INSTANCE_TABLE),COL_OWNER);
    createIndex(s,getTableName(ASSET_INSTANCE_TABLE),COL_PROTOTYPEID);

    //    createIndex(s,getTableName(ASSET_PROTOTYPE_TABLE),COL_PROTOTYPEID);
    createIndex(s,getTableName(ASSET_PROTOTYPE_TABLE),COL_PARENT_PROTOTYPEID);
    createIndex(s,getTableName(ASSET_PROTOTYPE_TABLE),COL_ASSET_CLASS);
    createIndex(s,getTableName(ASSET_PROTOTYPE_TABLE),COL_ASSET_TYPE);
    createIndex(s,getTableName(ASSET_PROTOTYPE_TABLE),COL_ALP_TYPEID);
    createIndex(s,getTableName(ASSET_PROTOTYPE_TABLE),COL_ALP_NOMENCLATURE);

    //    createIndex(s,getTableName(CONV_INSTANCE_TABLE),COL_CONVEYANCEID);
    createIndex(s,getTableName(CONV_INSTANCE_TABLE),COL_PROTOTYPEID);
    createIndex(s,getTableName(CONV_INSTANCE_TABLE),COL_SELFPROP);
    createIndex(s,getTableName(CONV_INSTANCE_TABLE),COL_OWNER);
    createIndex(s,getTableName(CONV_INSTANCE_TABLE),COL_BUMPERNO);

    //    createIndex(s,getTableName(CONV_PROTOTYPE_TABLE),COL_PROTOTYPEID);
    createIndex(s,getTableName(CONV_PROTOTYPE_TABLE),COL_CONVEYANCE_TYPE);
    createIndex(s,getTableName(CONV_PROTOTYPE_TABLE),COL_ALP_TYPEID);
    createIndex(s,getTableName(CONV_PROTOTYPE_TABLE),COL_ALP_NOMENCLATURE);

    //    createIndex(s,getTableName(LOCATIONS_TABLE),COL_LOCID);
    createIndex(s,getTableName(LOCATIONS_TABLE),COL_GEOLOC);

    createIndex(s,getTableName(ROUTE_ELEMENTS_TABLE),COL_ROUTEID);
    createIndex(s,getTableName(ROUTE_ELEMENTS_TABLE),COL_STARTLOC);
    createIndex(s,getTableName(ROUTE_ELEMENTS_TABLE),COL_ENDLOC);
    createIndex(s,getTableName(ROUTE_ELEMENTS_TABLE),COL_ROUTE_ELEM_NUM);

    createIndex(s,getTableName(CONVOYS_TABLE), COL_CONVOYID);
    createIndex(s,getTableName(CONVOYS_TABLE), COL_STARTTIME);
    createIndex(s,getTableName(CONVOYS_TABLE), COL_ENDTIME);

    createIndex(s,getTableName(CONVOY_MEMBER_TABLE), COL_CONVOYID);
    createIndex(s,getTableName(CONVOY_MEMBER_TABLE), COL_CONVEYANCEID);

    createIndex(s,getTableName(MANIFEST_TABLE),COL_ASSETID);

    try{
      if(s!=null)
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
