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
package org.cougaar.mlm.ui.newtpfdd.gui.view.route;

import org.cougaar.mlm.ui.newtpfdd.gui.view.DatabaseConfig;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Date;

/**
 * A request for an AssetInstance to be displayed.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 4/20/01
 **/
public class AssetInstanceRequest extends RouteViewRequest{

  //Constants:
  ////////////

  //Variables:
  ////////////
  String assetNomen;
  String assetid;

  boolean debug = "true".equals(System.getProperty("AssetInstanceRequest.debug", "false"));
  
  //Constructors:
  ///////////////
  public AssetInstanceRequest(String assetNomen, String assetid){
    this.assetNomen=assetNomen;
    this.assetid=assetid;
  }

  //Members:
  //////////
  public String getTitle(){return assetNomen;}

  public RouteData getRouteData(DatabaseConfig dbConfig, int runID){
    try{
      Statement s=getStatement(dbConfig);
      RouteData rd=getAssetInstanceRouteData(s,runID);
      if(s!=null)
	s.close();
      return rd;
    }catch(SQLException e){
      System.err.println(e);
    }
    return new RouteData("Asset Instance: "+assetNomen);
  }

  protected RouteData getAssetInstanceRouteData(Statement s, int runID)
    throws SQLException{
    RouteData routeData = new RouteData("Asset Instance: "+assetNomen);
    ResultSet rs;

    //Add for the legs w/o routes
    rs=s.executeQuery(getLegNoRouteSQL(runID));
    addRouteData(routeData,rs);

    //Add for the legs w routes:
    rs=s.executeQuery(getLegWithRouteSQL(runID));
    addRouteData(routeData,rs);

    return routeData;
  }

  protected void addRouteData(RouteData rd, ResultSet rs)
    throws SQLException{
    while(rs.next()){
      int mode=modeFromConvType(rs.getInt(1));
      String sLocID=rs.getString(2);
      String sGeoLoc=rs.getString(3);
      String sPrettyName=rs.getString(4);
      float sLat=rs.getFloat(5);
      float sLon=rs.getFloat(6);
      String eLocID=rs.getString(7);
      String eGeoLoc=rs.getString(8);
      String ePrettyName=rs.getString(9);
      float eLat=rs.getFloat(10);
      float eLon=rs.getFloat(11);
      Date start=rs.getTimestamp(12);
      Date end=rs.getTimestamp(13);
      String cargoType =rs.getString(14);
      String cargoName =rs.getString(15);
      String carrierType =rs.getString(16);
      String carrierName =rs.getString(17);
      
      RouteLoc s=new RouteLoc(sLocID,sGeoLoc,sPrettyName,sLat,sLon);
      RouteLoc e=new RouteLoc(eLocID,eGeoLoc,ePrettyName,eLat,eLon);
      AssetInstanceSegment seg=new AssetInstanceSegment(mode,
							RouteSegment.TYPE_TRANSPORTING,
							s,e);
      seg.setStart(start);
      seg.setEnd  (end);
      seg.setCargoType(cargoType);
      seg.setCargoName(cargoName);
      seg.setCarrierType(carrierType);
      seg.setCarrierName(carrierName);
	  
      rd.addSegment(s,e,seg);
    }
  }

  protected int modeFromConvType(int convtype){
    switch(convtype){
    case DGPSPConstants.CONV_TYPE_TRUCK:
    case DGPSPConstants.CONV_TYPE_TRAIN:
    case DGPSPConstants.CONV_TYPE_SELF_PROPELLABLE:
      return RouteSegment.MODE_GROUND;
    case DGPSPConstants.CONV_TYPE_SHIP:
      return RouteSegment.MODE_SEA;
    case DGPSPConstants.CONV_TYPE_PLANE:
      return RouteSegment.MODE_AIR;
    default:
      return RouteSegment.MODE_UNKNOWN;
    }
  }

  protected String getLegNoRouteSQL(int runID){
    String protoTable=getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,runID);
    String instanceTable=getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);
    String aiTable=getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,runID);
    String clTable=getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,runID);
    String sTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String eTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String ciTable=getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,runID);
    String cpTable=getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,runID);

    //For return:
    String convType="cp."+DGPSPConstants.COL_CONVEYANCE_TYPE;

    String sGeo="s."+DGPSPConstants.COL_GEOLOC;
    String sPretty="s."+DGPSPConstants.COL_PRETTYNAME;
    String sLat="s."+DGPSPConstants.COL_LAT;
    String sLon="s."+DGPSPConstants.COL_LON;

    String sLocID="s."+DGPSPConstants.COL_LOCID;
    String eGeo="e."+DGPSPConstants.COL_GEOLOC;
    String ePretty="e."+DGPSPConstants.COL_PRETTYNAME;
    String eLat="e."+DGPSPConstants.COL_LAT;
    String eLon="e."+DGPSPConstants.COL_LON;

    String eLocID="e."+DGPSPConstants.COL_LOCID;
    String clStart="cl."+DGPSPConstants.COL_STARTTIME;
    String clEnd="cl."+DGPSPConstants.COL_ENDTIME;
    String instanceNomen="pr."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String instanceName="inst."+DGPSPConstants.COL_NAME;
    String carrierNomen="cp."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String carrierName="ci."+DGPSPConstants.COL_BUMPERNO;
    
    //Also for where:
    String aiLegID="ai."+DGPSPConstants.COL_LEGID;
    String clLegID="cl."+DGPSPConstants.COL_LEGID;
    String clSLocID="cl."+DGPSPConstants.COL_STARTLOC;
    String clELocID="cl."+DGPSPConstants.COL_ENDLOC;
    String clConvID="cl."+DGPSPConstants.COL_CONVEYANCEID;
    String ciConvID="ci."+DGPSPConstants.COL_CONVEYANCEID;
    String ciProtoID="ci."+DGPSPConstants.COL_PROTOTYPEID;
    String cpProtoID="cp."+DGPSPConstants.COL_PROTOTYPEID;
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String clLegType="cl."+DGPSPConstants.COL_LEGTYPE;
    String clRouteID="cl."+DGPSPConstants.COL_ROUTEID;
    String instanceID="inst."+DGPSPConstants.COL_ASSETID;
    String instanceProtoID="inst."+DGPSPConstants.COL_PROTOTYPEID;
    String protoID="pr."+DGPSPConstants.COL_PROTOTYPEID;

    String sql=
      "select "+
      convType+", "+
      sLocID+", "+sGeo+", "+sPretty+", "+sLat+", "+sLon+", "+
      eLocID+", "+eGeo+", "+ePretty+", "+eLat+", "+eLon+", "+
      clStart+", "+clEnd+", "+
      instanceNomen+", "+instanceName+", "+
      carrierNomen+", "+carrierName+
      "\nfrom "+
      aiTable+" ai"+", "+
      clTable+" cl"+", "+
      sTable+" s"+", "+
      eTable+" e"+", "+
      ciTable+" ci"+", "+
      cpTable+" cp"+", "+
      protoTable+" pr"+", "+
      instanceTable+" inst"+
      "\nwhere "+
      aiLegID+"="+clLegID+"\nand "+
      clSLocID+"="+sLocID+"\nand "+
      clELocID+"="+eLocID+"\nand "+
      clConvID+"="+ciConvID+"\nand "+
      ciProtoID+"="+cpProtoID+"\nand "+
      "("+clLegType+"="+DGPSPConstants.LEG_TYPE_TRANSPORTING+//" or "+
      //      clLegType+"="+DGPSPConstants.LEG_TYPE_POSITIONING+" or "+
      //      clLegType+"="+DGPSPConstants.LEG_TYPE_RETURNING+
      ")"+"\nand "+
      aiAssetID+"='"+assetid+"'\nand "+
      aiAssetID+"="+instanceID+"\nand "+
      instanceProtoID+"="+protoID+"\nand "+
      clRouteID+"='null'";

    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceRequest - sql\n" + sql);
	
    return sql;
  }

  protected String getLegWithRouteSQL(int runID){
    String protoTable=getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,runID);
    String instanceTable=getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);
    String aiTable=getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,runID);
    String clTable=getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,runID);
    String sTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String eTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String ciTable=getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,runID);
    String cpTable=getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,runID);
    String reTable=getTableName(DGPSPConstants.ROUTE_ELEMENTS_TABLE,runID);

    //For return:
    String convType="cp."+DGPSPConstants.COL_CONVEYANCE_TYPE;

    String sLocID="s."+DGPSPConstants.COL_LOCID;
    String sGeo="s."+DGPSPConstants.COL_GEOLOC;
    String sPretty="s."+DGPSPConstants.COL_PRETTYNAME;
    String sLat="s."+DGPSPConstants.COL_LAT;
    String sLon="s."+DGPSPConstants.COL_LON;

    String eLocID="e."+DGPSPConstants.COL_LOCID;
    String eGeo="e."+DGPSPConstants.COL_GEOLOC;
    String ePretty="e."+DGPSPConstants.COL_PRETTYNAME;
    String eLat="e."+DGPSPConstants.COL_LAT;
    String eLon="e."+DGPSPConstants.COL_LON;

    String clStart="cl."+DGPSPConstants.COL_STARTTIME;
    String clEnd="cl."+DGPSPConstants.COL_ENDTIME;
    String instanceNomen="pr."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String instanceName="inst."+DGPSPConstants.COL_NAME;
    String carrierNomen="cp."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String carrierName="ci."+DGPSPConstants.COL_BUMPERNO;
    
    //Also for where:
    String aiLegID="ai."+DGPSPConstants.COL_LEGID;
    String clLegID="cl."+DGPSPConstants.COL_LEGID;
    String clConvID="cl."+DGPSPConstants.COL_CONVEYANCEID;
    String ciConvID="ci."+DGPSPConstants.COL_CONVEYANCEID;
    String ciProtoID="ci."+DGPSPConstants.COL_PROTOTYPEID;
    String cpProtoID="cp."+DGPSPConstants.COL_PROTOTYPEID;
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String clLegType="cl."+DGPSPConstants.COL_LEGTYPE;
    String clRouteID="cl."+DGPSPConstants.COL_ROUTEID;
    String reRouteID="re."+DGPSPConstants.COL_ROUTEID;
    String reSLocID="re."+DGPSPConstants.COL_STARTLOC;
    String reELocID="re."+DGPSPConstants.COL_ENDLOC;
    String instanceID="inst."+DGPSPConstants.COL_ASSETID;
    String instanceProtoID="inst."+DGPSPConstants.COL_PROTOTYPEID;
    String protoID="pr."+DGPSPConstants.COL_PROTOTYPEID;

    String sql=
      "select "+
      convType+", "+
      sLocID+", "+sGeo+", "+sPretty+", "+sLat+", "+sLon+", "+
      eLocID+", "+eGeo+", "+ePretty+", "+eLat+", "+eLon+", "+
      clStart+", "+clEnd+", "+instanceNomen+", "+instanceName+", "+carrierNomen+", "+carrierName+
      "\nfrom "+
      aiTable+" ai"+", "+
      clTable+" cl"+", "+
      sTable+" s"+", "+
      eTable+" e"+", "+
      ciTable+" ci"+", "+
      cpTable+" cp"+", "+
      reTable+" re"+", "+
      protoTable+" pr"+", "+
      instanceTable+" inst"+
      "\nwhere "+
      aiLegID+"="+clLegID+"\nand "+
      clConvID+"="+ciConvID+"\nand "+
      ciProtoID+"="+cpProtoID+"\nand "+
      "("+clLegType+"="+DGPSPConstants.LEG_TYPE_TRANSPORTING+//" or "+
      //      clLegType+"="+DGPSPConstants.LEG_TYPE_POSITIONING+" or "+
      //      clLegType+"="+DGPSPConstants.LEG_TYPE_RETURNING+
      ")"+"\nand "+
      aiAssetID+"='"+assetid+"'\nand "+
      aiAssetID+"="+instanceID+"\nand "+
      instanceProtoID+"="+protoID+"\nand "+
      clRouteID+"="+reRouteID+"\nand "+
      reSLocID+"="+sLocID+"\nand "+
      reELocID+"="+eLocID;

    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceRequest.getLegWithRouteSQL - sql\n" + sql);

    return sql;
  }

  //InnerClasses:
  ///////////////
}




