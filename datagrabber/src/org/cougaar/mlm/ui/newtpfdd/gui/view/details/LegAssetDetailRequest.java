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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Gets data about assets on a given leg
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



