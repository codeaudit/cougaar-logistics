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
