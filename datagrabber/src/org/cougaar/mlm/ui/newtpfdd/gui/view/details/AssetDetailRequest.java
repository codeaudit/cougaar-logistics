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

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import java.util.List;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Gets data about asset details
 *
 * @since 4/27/01
 **/
public abstract class AssetDetailRequest{

  //Constants:
  ////////////
  public static boolean debug = false;

  //Variables:
  ////////////

  //Constructors:
  ///////////////
  public AssetDetailRequest(){}

  //Members:
  //////////

  protected String getTableName(String baseName, int runID){
    return Controller.getTableName(baseName,runID);
  }

  protected abstract String getSql(DatabaseConfig dbConfig, int runID);

  public List getAssetDetails(DatabaseConfig dbConfig, int runID){
    List ret=new ArrayList();
    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetDetailRequest.getAssetDetails called.");

    String sql = getSql(dbConfig,runID);
    try{
      Statement s = dbConfig.getConnection().createStatement();
      ResultSet rs= s.executeQuery(sql);
      while(rs.next()){
	ret.add(produceAssetDetails(rs));
      }
    }catch(SQLException e){
      System.err.println("SQL Exception getting Asset Details: "+e + "\nSql was:\n" + sql);
    }
    return ret;
  }

  protected AssetDetails produceAssetDetails(ResultSet rs)throws SQLException{
    AssetDetails ad=new AssetDetails();
    ad.setValueAt(new String(rs.getString(1)),AssetDetails.OWNER);
    ad.setValueAt(new String(rs.getString(2)),AssetDetails.TYPE);
    ad.setValueAt(new String(rs.getString(3)),AssetDetails.TYPENAME);
    ad.setValueAt(new String(rs.getString(4)),AssetDetails.ASSETNAME);
    ad.setValueAt(new Integer(rs.getInt(5)),AssetDetails.NUMBER);
    ad.setValueAt(new Integer(rs.getInt(6)),AssetDetails.ASSETCLASS);
    /*    ad.setValueAt(new Integer(rs.getInt(7)),AssetDetails.ASSETTYPE);*/
    ad.setValueAt(new Double(rs.getDouble(8)),AssetDetails.WEIGHT);
    ad.setValueAt(new Double(rs.getDouble(9)),AssetDetails.WIDTH);
    ad.setValueAt(new Double(rs.getDouble(10)),AssetDetails.HEIGHT);
    ad.setValueAt(new Double(rs.getDouble(11)),AssetDetails.DEPTH);
    ad.setValueAt(new Double(rs.getDouble(12)),AssetDetails.AREA);
    ad.setValueAt(new Double(rs.getDouble(13)),AssetDetails.VOLUME);

    String ccc =rs.getString(14);
    ad.setValueAt(ccc,AssetDetails.CCC);

    String transport;

    switch (ccc.charAt(1)) {
    case '0': // non-air
      transport = "Non-Air";
      break;
    case '1': // outsized
      transport = "Outsized";
      break;
    case '2': // oversized
      transport = "Oversized";
      break;
    case '3': // bulk
      transport = "Bulk";
      break;
    default:
      transport = "Unknown";
    }

    if (ccc.charAt(0) == 'R') {
      if (transport.charAt(0) == 'U') {
	transport = "Roadable";
    }  else if (ccc.charAt(0) == 'R') {
	transport += "_Roadable"; 
    }
    }

    ad.setValueAt(transport,AssetDetails.TRANSPORT);

    return ad;
  }

  //InnerClasses:
  ///////////////
}
