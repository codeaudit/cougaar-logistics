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

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.controller.Controller;

import java.util.List;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Gets data about asset details
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
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
      System.out.println ("AssetDetailRequest.getAssetDetails called.");

    try{
      Statement s = dbConfig.getConnection().createStatement();
      ResultSet rs= s.executeQuery(getSql(dbConfig,runID));
      while(rs.next()){
	ret.add(produceAssetDetails(rs));
      }
    }catch(SQLException e){
      System.err.println("SQL Exception getting Asset Details: "+e);
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
    return ad;
  }

  //InnerClasses:
  ///////////////
}
