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
package org.cougaar.mlm.ui.newtpfdd.gui.view.details;

import java.util.List;
import java.util.ArrayList;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Gets data about assets on a given asset
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 4/27/01
 **/
public class AssetAssetDetailRequest extends AssetDetailRequest{

  //Constants:
  ////////////
  public static boolean debug = false;
  //Variables:
  ////////////
  private String assetID;

  //Constructors:
  ///////////////
  public AssetAssetDetailRequest(String assetID){
    this.assetID=assetID;
  }

  //Members:
  //////////

  protected String getSql(DatabaseConfig dbConfig, int runID){
    String aiTable=getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);
    String apTable=getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,runID);

    //For select:
    String aiOwner="ai."+DGPSPConstants.COL_OWNER;
    String apType="ap."+DGPSPConstants.COL_ALP_TYPEID;
    String apNomen="ap."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String aiName="ai."+DGPSPConstants.COL_NAME;
    String aiAgg="ai."+DGPSPConstants.COL_AGGREGATE;
    String apAClass="ap."+DGPSPConstants.COL_ASSET_CLASS;
    String apAType="ap."+DGPSPConstants.COL_ASSET_TYPE;
    String apWeight="ap."+DGPSPConstants.COL_WEIGHT;
    String apWidth="ap."+DGPSPConstants.COL_WIDTH;
    String apHeight="ap."+DGPSPConstants.COL_HEIGHT;
    String apDepth="ap."+DGPSPConstants.COL_DEPTH;

    //For Where:
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String aiProtoID="ai."+DGPSPConstants.COL_PROTOTYPEID;
    String apProtoID="ap."+DGPSPConstants.COL_PROTOTYPEID;

    String sql="select "+
      aiOwner+", "+
      apType+", "+
      apNomen+", "+
      aiName+", "+
      aiAgg+", "+
      apAClass+", "+
      apAType+", "+
      apWeight+", "+
      apWidth+", "+
      apHeight+", "+
      apDepth+
      " from "+
      aiTable+" ai, "+   
      apTable+" ap "+
      " where "+
      aiAssetID+"='"+assetID+"'\nand "+
      aiProtoID+"="+apProtoID + "\n" +
      "order by " + aiOwner +"," +apType+","+apNomen+","+aiName;

    if (debug) 
      System.out.println ("AssetAssetDetailRequest.getSql - sql is " + sql);

      return sql;
  }

  //InnerClasses:
  ///////////////
}



