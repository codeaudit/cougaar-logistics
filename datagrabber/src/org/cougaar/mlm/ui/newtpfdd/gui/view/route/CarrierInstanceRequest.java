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

import com.bbn.openmap.omGraphics.OMArrowHead;

import java.awt.Stroke;
import java.awt.BasicStroke;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Date;

/**
 * A request for an CarrierInstance to be displayed.
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2//01
 **/
public class CarrierInstanceRequest extends RouteViewRequest{

  //Constants:
  ////////////

  //Variables:
  ////////////
  String carrierNomen;
  String carrierid;

  String unit=null;

  boolean debug = "true".equals(System.getProperty("CarrierInstanceRequest.debug", "false"));
  
  //Constructors:
  ///////////////
  public CarrierInstanceRequest(String carrierNomen, String carrierid){
    this.carrierNomen=carrierNomen;
    this.carrierid=carrierid;
  }
  public CarrierInstanceRequest(String carrierNomen, 
				String carrierid, 
				String unit){
    this(carrierNomen,carrierid);
    this.unit=unit;
  }

  //Members:
  //////////
  public String getTitle(){return carrierNomen;}

  public RouteData getRouteData(DatabaseConfig dbConfig, int runID){
    try{
      Statement s=getStatement(dbConfig);
      RouteData rd=getCarrierInstanceRouteData(s,runID);
      if(s!=null)
	s.close();
      return rd;
    }catch(SQLException e){
      System.err.println(e);
    }
    return new RouteData("Carrier Instance: "+carrierNomen);
  }

  protected RouteData getCarrierInstanceRouteData(Statement s, int runID)
    throws SQLException{
    RouteData routeData = new RouteData("Carrier Instance: "+carrierNomen);
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
      String carrierType =rs.getString(14);
      String carrierName =rs.getString(15);
      int legType = rs.getInt(16);
      String legID = rs.getString(17);

      CarrierInstanceSegment cis=(CarrierInstanceSegment)
	rd.getSegment(new CarrierInstanceSegment.CarrierInstanceSegmentKey(sLocID,eLocID));
      
      if(cis==null){
	RouteLoc s=new RouteLoc(sLocID,sGeoLoc,sPrettyName,sLat,sLon);
	RouteLoc e=new RouteLoc(eLocID,eGeoLoc,ePrettyName,eLat,eLon);
	CarrierInstanceSegment seg=new CarrierInstanceSegment(mode,getSegType(legType),
							      legID,
							      s,start,
							      e,end);
	seg.setCarrierType(carrierType);
	seg.setCarrierName(carrierName);
	rd.addSegment(s,e,seg);
      }else{
	cis.addLegInfo(legID,getSegType(legType),sLocID,start,eLocID,end);
      }
    }
  }

  protected int getSegType(int legType){
    switch(legType){
    case DGPSPConstants.LEG_TYPE_TRANSPORTING:
      return RouteSegment.TYPE_TRANSPORTING;
    case DGPSPConstants.LEG_TYPE_POSITIONING:
      return RouteSegment.TYPE_POSITIONING;
    case DGPSPConstants.LEG_TYPE_RETURNING:
      return RouteSegment.TYPE_RETURNING;
    default:
      return RouteSegment.TYPE_UNKNOWN;
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
    String clTable=getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,runID);
    String sTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String eTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String ciTable=getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,runID);
    String cpTable=getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,runID);

    String itinTable=getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,runID);
    String aiTable=getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);

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
    String carrierNomen="cp."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String carrierName="ci."+DGPSPConstants.COL_BUMPERNO;
    String clLegType="cl."+DGPSPConstants.COL_LEGTYPE;
    String clLegID="cl."+DGPSPConstants.COL_LEGID;
    
    //Also for where:
    String clSLocID="cl."+DGPSPConstants.COL_STARTLOC;
    String clELocID="cl."+DGPSPConstants.COL_ENDLOC;
    String clConvID="cl."+DGPSPConstants.COL_CONVEYANCEID;
    String ciConvID="ci."+DGPSPConstants.COL_CONVEYANCEID;
    String ciProtoID="ci."+DGPSPConstants.COL_PROTOTYPEID;
    String cpProtoID="cp."+DGPSPConstants.COL_PROTOTYPEID;
    String clRouteID="cl."+DGPSPConstants.COL_ROUTEID;

    String itinLegID="itin."+DGPSPConstants.COL_LEGID;
    String itinAssetID="itin."+DGPSPConstants.COL_ASSETID;
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String aiOwner="ai."+DGPSPConstants.COL_OWNER;

    String sql=
      "select distinct "+
      convType+", "+
      sLocID+", "+sGeo+", "+sPretty+", "+sLat+", "+sLon+", "+
      eLocID+", "+eGeo+", "+ePretty+", "+eLat+", "+eLon+", "+
      clStart+", "+clEnd+", "+
      carrierNomen+", "+carrierName+", "+
      clLegType+", "+
      clLegID+      
      "\nfrom "+
      clTable+" cl"+", "+
      sTable+" s"+", "+
      eTable+" e"+", "+
      ciTable+" ci"+", "+
      cpTable+" cp";
    if(unit!=null){
      sql+=", "+itinTable+" itin"+", "+aiTable+" ai";
    }
    sql+="\nwhere "+
      clSLocID+"="+sLocID+"\nand "+
      clELocID+"="+eLocID+"\nand "+
      clConvID+"="+ciConvID+"\nand "+
      ciProtoID+"="+cpProtoID+"\nand "+
      "("+clLegType+"="+DGPSPConstants.LEG_TYPE_TRANSPORTING+" or "+
      clLegType+"="+DGPSPConstants.LEG_TYPE_POSITIONING+" or "+
      clLegType+"="+DGPSPConstants.LEG_TYPE_RETURNING+
      ")"+"\nand "+
      clRouteID+"='null'"+"\nand "+
      ciConvID+"='"+carrierid+"'";

    if(unit!=null){
      sql+="\nand "+
	clLegID+"="+itinLegID+"\nand "+
	itinAssetID+"="+aiAssetID+"\nand "+
	aiOwner+"='"+unit+"'";
    }

    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "CarrierInstanceRequest - sql\n" + sql);
	
    return sql;
  }

  protected String getLegWithRouteSQL(int runID){
    String clTable=getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,runID);
    String sTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String eTable=getTableName(DGPSPConstants.LOCATIONS_TABLE,runID);
    String ciTable=getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,runID);
    String cpTable=getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,runID);
    String reTable=getTableName(DGPSPConstants.ROUTE_ELEMENTS_TABLE,runID);

    String itinTable=getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,runID);
    String aiTable=getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,runID);

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
    String carrierNomen="cp."+DGPSPConstants.COL_ALP_NOMENCLATURE;
    String carrierName="ci."+DGPSPConstants.COL_BUMPERNO;
    String clLegType="cl."+DGPSPConstants.COL_LEGTYPE;
    String clLegID="cl."+DGPSPConstants.COL_LEGID;
    
    //Also for where:
    String clConvID="cl."+DGPSPConstants.COL_CONVEYANCEID;
    String ciConvID="ci."+DGPSPConstants.COL_CONVEYANCEID;
    String ciProtoID="ci."+DGPSPConstants.COL_PROTOTYPEID;
    String cpProtoID="cp."+DGPSPConstants.COL_PROTOTYPEID;
    String clRouteID="cl."+DGPSPConstants.COL_ROUTEID;
    String reRouteID="re."+DGPSPConstants.COL_ROUTEID;
    String reSLocID="re."+DGPSPConstants.COL_STARTLOC;
    String reELocID="re."+DGPSPConstants.COL_ENDLOC;

    String itinLegID="itin."+DGPSPConstants.COL_LEGID;
    String itinAssetID="itin."+DGPSPConstants.COL_ASSETID;
    String aiAssetID="ai."+DGPSPConstants.COL_ASSETID;
    String aiOwner="ai."+DGPSPConstants.COL_OWNER;

    String sql=
      "select distinct "+
      convType+", "+
      sLocID+", "+sGeo+", "+sPretty+", "+sLat+", "+sLon+", "+
      eLocID+", "+eGeo+", "+ePretty+", "+eLat+", "+eLon+", "+
      clStart+", "+clEnd+", "+
      carrierNomen+", "+carrierName+", "+
      clLegType+", "+
      clLegID+      
      "\nfrom "+
      clTable+" cl"+", "+
      sTable+" s"+", "+
      eTable+" e"+", "+
      ciTable+" ci"+", "+
      cpTable+" cp"+", "+
      reTable+" re";
    if(unit!=null){
      sql+=", "+itinTable+" itin"+", "+aiTable+" ai";
    }
    sql+="\nwhere "+
      clConvID+"="+ciConvID+"\nand "+
      ciProtoID+"="+cpProtoID+"\nand "+
      "("+clLegType+"="+DGPSPConstants.LEG_TYPE_TRANSPORTING+" or "+
      clLegType+"="+DGPSPConstants.LEG_TYPE_POSITIONING+" or "+
      clLegType+"="+DGPSPConstants.LEG_TYPE_RETURNING+
      ")"+"\nand "+
      clRouteID+"="+reRouteID+"\nand "+
      reSLocID+"="+sLocID+"\nand "+
      reELocID+"="+eLocID+"\nand "+
      ciConvID+"='"+carrierid+"'";

    if(unit!=null){
      sql+="\nand "+
	clLegID+"="+itinLegID+"\nand "+
	itinAssetID+"="+aiAssetID+"\nand "+
	aiOwner+"='"+unit+"'";
    }

    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "CarrierInstanceRequest.getLegWithRouteSQL - sql\n" + sql);

    return sql;
  }

  //InnerClasses:
  ///////////////
}




