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
package org.cougaar.mlm.ui.newtpfdd.gui.view.details;

import java.util.List;
import java.util.ArrayList;
import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;
import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Gets data about assets on a given asset
 *
 * @since 4/27/01
 **/
public class AssetAssetDetailRequest extends AssetDetailRequest{

  //Constants:
  ////////////
  public static boolean debug = Boolean.getBoolean("org.cougaar.mlm.ui.newtpfdd.gui.view.details.AssetAssetDetailRequest.debug");
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
    if (assetID.indexOf ("-item-") != -1)
      return getManifestSql(dbConfig, runID);

    //For select:
    String aiOwner="ai."+DGPSPConstants.COL_OWNER;
    String apType="ap."+DGPSPConstants.COL_ALP_TYPEID;
    String apNomen="ap."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String aiName="ai."+DGPSPConstants.COL_NAME;
    String aiAgg="ai."+DGPSPConstants.COL_AGGREGATE;
    String apAClass="ap."+DGPSPConstants.COL_ASSET_CLASS;
    String apAType="ap."+DGPSPConstants.COL_ASSET_TYPE;
    String cccDimWeight=DGPSPConstants.COL_WEIGHT;
    String cccDimWidth=DGPSPConstants.COL_WIDTH;
    String cccDimHeight=DGPSPConstants.COL_HEIGHT;
    String cccDimDepth=DGPSPConstants.COL_DEPTH;
    String cccDimArea=DGPSPConstants.COL_AREA;
    String cccDimVolume=DGPSPConstants.COL_VOLUME;

    String cccDimCCC=DGPSPConstants.COL_CARGO_CAT_CODE;

    String sql="select "+
      aiOwner+", "+
      apType+", "+
      apNomen+", "+
      aiName+", "+
      aiAgg+", "+
      apAClass+", "+
      apAType+", "+
      cccDimWeight+", "+
      cccDimWidth+", "+
      cccDimHeight+", "+
      cccDimDepth+", "+
      cccDimArea+", "+
      cccDimVolume+", "+
      cccDimCCC+
      "\nfrom " +getFrom  (runID) + "\n" +
      "where "+getWhere (runID) + "\n" +
      "order by " + aiOwner +"," +apType+","+apNomen+","+aiName;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetAssetDetailRequest.getSql - sql is\n" + sql);

      return sql;
  }

  protected String getManifestSql(DatabaseConfig dbConfig, int runID){
    //For select:
    String aiOwner="ai."+DGPSPConstants.COL_OWNER;
    String apType="ap."+DGPSPConstants.COL_ALP_TYPEID;
    String apNomen="ap."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String aiName="ai."+DGPSPConstants.COL_NAME;
    String aiAgg="ai."+DGPSPConstants.COL_AGGREGATE;
    String apAClass="ap."+DGPSPConstants.COL_ASSET_CLASS;
    String apAType="ap."+DGPSPConstants.COL_ASSET_TYPE;
    String cccDimWeight="man." + DGPSPConstants.COL_WEIGHT;
    String cccDimWidth=DGPSPConstants.COL_WIDTH;
    String cccDimHeight=DGPSPConstants.COL_HEIGHT;
    String cccDimDepth=DGPSPConstants.COL_DEPTH;
    String cccDimArea=DGPSPConstants.COL_AREA;
    String cccDimVolume=DGPSPConstants.COL_VOLUME;

    String cccDimCCC=DGPSPConstants.COL_CARGO_CAT_CODE;

    String sql="select "+
      aiOwner+", "+
      apType+", "+
      apNomen+", "+
      aiName+", "+
      aiAgg+", "+
      apAClass+", "+
      apAType+", "+
      cccDimWeight+", "+
      cccDimWidth+", "+
      cccDimHeight+", "+
      cccDimDepth+", "+
      cccDimArea+", "+
      cccDimVolume+", "+
      cccDimCCC+
      "\nfrom " +getManifestFrom  (runID) + "\n" +
      "where "+getManifestWhere (runID) + "\n" +
      "order by " + aiOwner +"," +apType+","+apNomen+","+aiName;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetAssetDetailRequest.getSql - sql is\n" + sql);

      return sql;
  }

  protected String getFrom (int runID) {
    String aiTable  =getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);
    String apTable  =getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,runID);
    String cccDimTable=getTableName(DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE,runID);

    return aiTable+" ai, "+   
      apTable+" ap, " + cccDimTable;
  }

  protected String getManifestFrom (int runID) {
    String aiTable  =getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);
    String apTable  =getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,runID);
    String cccDimTable=getTableName(DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE,runID);
    String manifestTable = getTableName(DGPSPConstants.MANIFEST_TABLE,runID);

    return aiTable+" ai, "+   
      apTable+" ap, " + cccDimTable + ", " + manifestTable + " man ";
  }

  protected String getWhere (int runID) {
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String aiProtoID="ai."+DGPSPConstants.COL_PROTOTYPEID;
    String apProtoID="ap."+DGPSPConstants.COL_PROTOTYPEID;
    String cccDimTable=getTableName(DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE,runID);
    String cccDimProtoID=cccDimTable +"."+DGPSPConstants.COL_PROTOTYPEID;

    return aiAssetID+"='"+assetID+"'\n and "+
      aiProtoID     +"="+apProtoID + "\n and " +
      cccDimProtoID +"="+apProtoID;
  }

  protected String getManifestWhere (int runID) {
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String aiProtoID="ai."+DGPSPConstants.COL_PROTOTYPEID;
    String apProtoID="ap."+DGPSPConstants.COL_PROTOTYPEID;
    String cccDimTable=getTableName(DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE,runID);
    String cccDimProtoID=cccDimTable +"."+DGPSPConstants.COL_PROTOTYPEID;
    String manifestTable = getTableName(DGPSPConstants.MANIFEST_TABLE,runID);

    String manifestInstanceID = "man." + DGPSPConstants.COL_MANIFEST_ITEM_ID;
    String manifestName      = "man." + DGPSPConstants.COL_NAME;
    String manifestAssetID   = "man." + DGPSPConstants.COL_ASSETID;

    return 
      manifestInstanceID +"='"+assetID + "'\n and " +
      manifestAssetID +"="+aiAssetID + "\n and " +
      aiProtoID     +"="+apProtoID + "\n and " +
      cccDimProtoID +"="+apProtoID;
  }

  //InnerClasses:
  ///////////////
}



