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
 * Gets data about assets on a given leg
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 4/27/01
 **/
public class LegAssetDetailRequest extends AssetAssetDetailRequest{

  //Constants:
  ////////////

  //Variables:
  ////////////
  private String legID;

  //Constructors:
  ///////////////
  public LegAssetDetailRequest(String legID){
    super ("");
    this.legID=legID;
  }

  //Members:
  //////////

  protected String getFrom (int runID) {
    String itinTable=getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,runID);

    return super.getFrom (runID) + ", "+itinTable+ " itin";
  } 

  protected String getWhere (int runID) {
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String aiProtoID="ai."+DGPSPConstants.COL_PROTOTYPEID;
    String apProtoID="ap."+DGPSPConstants.COL_PROTOTYPEID;
    String itinLegID="itin."+DGPSPConstants.COL_LEGID;
    String itinAssetID="itin."+DGPSPConstants.COL_ASSETID;
    String cccDimTable=getTableName(DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE,runID);
    String cccDimProtoID=cccDimTable +"."+DGPSPConstants.COL_PROTOTYPEID;

    return 
      itinLegID     +"='"+legID     + "'\n and " + 
      itinAssetID   +"=" +aiAssetID +  "\n and " + 
      aiProtoID     +"=" +apProtoID +  "\n and " +
      cccDimProtoID +"=" +apProtoID;
  }

  //InnerClasses:
  ///////////////
}



