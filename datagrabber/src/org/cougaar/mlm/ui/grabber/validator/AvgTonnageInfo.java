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
package org.cougaar.mlm.ui.grabber.validator;

import  org.cougaar.mlm.ui.grabber.logger.Logger;
import  org.cougaar.mlm.ui.grabber.config.DBConfig;
import  org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import  org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**
 * Looks for missing people
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/
public class AvgTonnageInfo extends AbstractTonnageInfo {
  
  boolean debug = false;

  public AvgTonnageInfo(DBConfig dbConfig, boolean showUnit, boolean showClass,
				  boolean showType, boolean showLocation){
    super(dbConfig);
      init(showUnit, showClass, showType, showLocation);
  }

  /**for gui**/
  public String getDescription(){
      return super.getDescription("Average Tonnage Per Day Information by ");
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] possibleHeaders = new String[4];
    int count = 0;
    if (showUnit) possibleHeaders[count++] = "Unit";
    if (showClass) possibleHeaders[count++] = "Class";
    if (showType) possibleHeaders[count++] = "Type";
    if (showLocation) possibleHeaders[count++] = "Location";

    String[] headers= new String[count+1];
    for (int i = 0; i < count; i++) {
      headers[i] = possibleHeaders[i];
    }
    headers[count++] = "AverageTonnage";
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int count = 0;
    if (showUnit) count++;
    if (showClass) count++;
    if (showType) count++;
    if (showLocation) count++;
    count++;

    int[] types= new int[count];
    for (int i = 0; i < count; i++) {
      types[i] = TYPE_STRING;
    }
    types[count-1] = TYPE_TONNAGE_THREE_DIGITS;
    return types;
  }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    (showUnit?(COL_UNIT+" VARCHAR(255) NOT NULL,"):"")+
		    (showClass?(COL_CLASS+" VARCHAR(255) NOT NULL,"):"")+
		    (showType?(COL_TYPE+" VARCHAR(255) NOT NULL,"):"")+
		    (showLocation?(COL_LOCATION+" VARCHAR(255) NOT NULL,"):"")+
		    COL_TONNAGE+" VARCHAR(255) NOT NULL"+
		    ")");
  }

  private static double DAY_IN_MILLIS = 1000.0d * 60.0d * 60.0d * 24.0d;

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;
    
    try {
      sql = getQuery(run);
      if (debug)
	System.out.println ("SQL was\n" + sql);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "AvgTonnageInfo.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      while(rs.next()){
	String unit = showUnit ? rs.getString(DGPSPConstants.COL_OWNER) : null;
	int assetclass = showClass ? rs.getInt(DGPSPConstants.COL_ASSET_CLASS) : -1;
	String assettype = showType ? rs.getString(DGPSPConstants.COL_ALP_NOMENCLATURE) : null;
	String location = showLocation ? rs.getString(DGPSPConstants.COL_PRETTYNAME) : null;
	double weight = rs.getDouble("sum("+DGPSPConstants.COL_WEIGHT+")");
	Date start = rs.getTimestamp("min("+DGPSPConstants.COL_STARTTIME+")");
	Date end = rs.getTimestamp("max("+DGPSPConstants.COL_ENDTIME+")");

	// System.out.println("weight = "+weight+" / "+start+" to "+end);
	// System.out.println("Divide by "+((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));

	weight = weight / (((double)end.getTime() - (double)start.getTime()) / DAY_IN_MILLIS);
	// System.out.println("FINAL AVG WEIGHT = "+weight);

	if (assetclass != DGPSPConstants.ASSET_CLASS_PERSON)
	  insertRow(l,s,run,unit,getRomanClass(assetclass),assettype,location,weight);
      }    
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "AvgTonnageInfo.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String itinTable = Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,run);
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String locTable = Controller.getTableName(DGPSPConstants.LOCATIONS_TABLE,run);

    String weight = DGPSPConstants.COL_WEIGHT;
    String unit = DGPSPConstants.COL_OWNER;
    String assetclass = DGPSPConstants.COL_ASSET_CLASS;
    String assettype = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String location = DGPSPConstants.COL_PRETTYNAME;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String instAssetid = assetTable+"."+DGPSPConstants.COL_ASSETID;
    String itinAssetid = itinTable+"."+DGPSPConstants.COL_ASSETID;
    String itinLegid = itinTable+"."+DGPSPConstants.COL_LEGID;
    String legLegid = legTable+"."+DGPSPConstants.COL_LEGID;
    String legLoc = legTable+"."+DGPSPConstants.COL_ENDLOC;
    String locLoc = locTable+"."+DGPSPConstants.COL_LOCID;
    String start = DGPSPConstants.COL_STARTTIME;
    String end = DGPSPConstants.COL_ENDTIME;


    String sqlQuery =
      "select "+
      (showUnit?(unit+", "):"")+
      (showClass?(assetclass+", "):"")+
      (showType?(assettype+", "):"")+
      (showLocation?(location+", "):"")+
      "sum("+weight+"),\n"+
      "min("+start+"), max("+end+")\n"+
      "from "+assetTable+", "+protoTable+", "+itinTable+", "+legTable+
      (showLocation?(", "+locTable):"")+"\n"+
      "where "+instProtoid+" = "+protoProtoid+"\n"+
      "and "+instAssetid+" = "+itinAssetid+"\n"+
      "and "+itinLegid+" = "+legLegid+"\n"+
      (showLocation?("and "+legLoc+" = "+locLoc+"\n"):"")+
      ((showUnit || showClass || showType || showLocation) ? 
       (
	"group by "+
	(showUnit?(unit+((showClass || showType || showLocation)?", ":"")):"")+
	(showClass?(assetclass+((showType || showLocation)?", ":"")):"")+
	(showType?(assettype+((showLocation)?", ":"")):"")+
	(showLocation?location:"")+"\n"+
	"order by "+
	(showUnit?(unit+((showClass || showType || showLocation)?", ":"")):"")+
	(showClass?(assetclass+((showType || showLocation)?", ":"")):"")+
	(showType?(assettype+((showLocation)?", ":"")):"")+
	(showLocation?location:"")+"\n"
	):"");
    return sqlQuery;
  }
  
  protected void insertRow(Logger l, Statement s, int run, String unit, String assetclass, 
			   String assettype, String location, double weight) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      if (showUnit) {sb.append(COL_UNIT);sb.append(",");}
      if (showClass) {sb.append(COL_CLASS);sb.append(",");}
      if (showType) {sb.append(COL_TYPE);sb.append(",");}
      if (showLocation) {sb.append(COL_LOCATION);sb.append(",");}
      sb.append(COL_TONNAGE);
      sb.append(") VALUES('");
      if (showUnit) {sb.append(unit);sb.append("','");}
      if (showClass) {sb.append(assetclass);sb.append("','");}
      if (showType) {sb.append(assettype);sb.append("','");}
      if (showLocation) {sb.append(location);sb.append("','");}
      sb.append(dbConfig.getDBDouble((weight/1000000.0d)*CargoDimensionTest.METRIC_TO_SHORT_TON)); 
      sb.append("')");
      sql = sb.toString();
      // System.out.println(sql);
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "AvgTonnageInfo.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  //InnerClasses:
  ///////////////
}





