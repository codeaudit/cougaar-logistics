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
package org.cougaar.mlm.ui.grabber.validator;

import  org.cougaar.mlm.ui.grabber.logger.Logger;
import  org.cougaar.mlm.ui.grabber.config.DBConfig;
import  org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import  org.cougaar.mlm.ui.grabber.controller.Controller;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.Calendar;

/**
 * Looks for missing legs
 * @author Benjamin Lubin; last modified by: $Author: tom $
 *
 * @since 2/26/01
 **/
public class MissingLegTest extends Test implements DGPSPConstants{

  //Constants:
  ////////////

  public static final int BY_CARRIER=0;
  public static final int BY_ASSET  =1;

  public static final String COL_PRE_OR_POST="pre_or_post";
  public static final String COL_SEARCH="searchid";

  public static final int PRE=1;
  public static final int POST=2;

  //Variables:
  ////////////

  private int searchType;

  //Constructors:
  ///////////////

  public MissingLegTest(DBConfig dbConfig, int searchType){
    super(dbConfig);
    this.searchType=searchType;
    setPriorityLevel(Test.PRIORITY_CORE);
  }

  //Members:
  //////////

  private String getSearchTypeString(){
    switch(searchType){
    default:
    case BY_CARRIER:
      return "carrier";
    case BY_ASSET:
      return "asset";
    }
  }

  private int getSearchColType(){
    switch(searchType){
    default:
    case BY_CARRIER:
      return TYPE_CONV;
    case BY_ASSET:
      return TYPE_ASSET;
    }
  }

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_ERROR;
  }

  /**for gui**/
  public String getDescription(){
    return "Missing legs by "+getSearchTypeString()+" (Long to run)";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "missleg"+getSearchTypeString();
  }

  private void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    COL_LEGID+" VARCHAR(255) NOT NULL,"+
		    COL_LEGTYPE+" INTEGER NOT NULL,"+
		    COL_STARTTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		    COL_ENDTIME+" "+dbConfig.getDateTimeType()+" NOT NULL,"+
		    COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		    COL_ENDLOC+" VARCHAR(255) NOT NULL,"+
		    COL_SEARCH+" VARCHAR(255) NOT NULL,"+
		    COL_OWNER+" VARCHAR(255) NOT NULL,"+
		    COL_NAME+" VARCHAR(255) NOT NULL,"+
		    COL_PRE_OR_POST+" INTEGER NOT NULL"+
		    ")");
  }

    private void addCarrierFields(StringBuffer sb) {
        sb.append(COL_LEGID);sb.append(",");
        sb.append(COL_LEGTYPE);sb.append(",");
        sb.append(COL_STARTTIME);sb.append(",");
        sb.append(COL_ENDTIME);sb.append(",");
        sb.append(COL_STARTLOC);sb.append(",");
        sb.append(COL_ENDLOC);sb.append(",");
    }

  private String getQuery(Logger l, int run){
    StringBuffer sb=new StringBuffer();
    switch(searchType){
    case BY_CARRIER:
      sb.append("select ");
      addCarrierFields(sb);
      sb.append("cl.");
      sb.append(COL_CONVEYANCEID);
      sb.append(",cl.");
      sb.append(COL_OWNER);
      sb.append(",cl.");
      sb.append(COL_BUMPERNO);
      sb.append("\nfrom ");
      sb.append(Controller.getTableName(CONVEYED_LEG_TABLE,run));
      sb.append(" cl");
      sb.append(",");
      sb.append(Controller.getTableName(CONV_INSTANCE_TABLE,run));
      sb.append(" ci");
      sb.append("\nwhere ");
      sb.append("cl.");
      sb.append(COL_CONVEYANCEID);
      sb.append("=");
      sb.append("ci.");
      sb.append(COL_CONVEYANCEID);
      sb.append("\nand ");
      sb.append("ci.");
      sb.append(COL_SELFPROP);
      sb.append("=");
      sb.append("0");
      sb.append("\norder by ");
      sb.append(COL_CONVEYANCEID);
      sb.append(",");
      sb.append(COL_STARTTIME);
      break;
    case BY_ASSET:
      sb.append("select c.");
      sb.append(COL_LEGID);
      sb.append(",c.");
      sb.append(COL_LEGTYPE);
      sb.append(",c.");
      sb.append(COL_STARTTIME);
      sb.append(",c.");
      sb.append(COL_ENDTIME);
      sb.append(",c.");
      sb.append(COL_STARTLOC);
      sb.append(",\nc.");
      sb.append(COL_ENDLOC);
      sb.append(",i.");
      sb.append(COL_ASSETID);
      sb.append(",a.");
      sb.append(COL_OWNER);
      sb.append(",a.");
      sb.append(COL_NAME);
      sb.append(",p.");
      sb.append(COL_IS_LOW_FIDELITY);
      sb.append("\nfrom ");
      sb.append(Controller.getTableName(ASSET_PROTOTYPE_TABLE,run));
      sb.append(" p, ");
      sb.append(Controller.getTableName(ASSET_INSTANCE_TABLE,run));
      sb.append(" a, ");
      sb.append(Controller.getTableName(CONVEYED_LEG_TABLE,run));
      sb.append(" c, ");
      sb.append(Controller.getTableName(ASSET_ITINERARY_TABLE,run));
      sb.append(" i where c.");
      sb.append(COL_LEGID);
      sb.append("=i.");
      sb.append(COL_LEGID);
      sb.append("\nand ");
      sb.append(COL_LEGTYPE);
      sb.append(" <> ");
      sb.append(LEG_TYPE_POSITIONING);
      sb.append("\nand ");
      sb.append(COL_LEGTYPE);
      sb.append(" <> ");
      sb.append(LEG_TYPE_RETURNING);
      sb.append("\nand i.");
      sb.append(COL_ASSETID);
      sb.append("=a.");
      sb.append(COL_ASSETID);
      sb.append("\nand a.");
      sb.append(COL_PROTOTYPEID);
      sb.append("=p.");
      sb.append(COL_PROTOTYPEID);
      sb.append("\norder by i.");
      sb.append(COL_ASSETID);
      sb.append(",c.");
      sb.append(COL_STARTTIME);
      break;
    }

    l.logMessage(Logger.MINOR,Logger.DB_QUERY,"SQL was : \n" + sb.toString());

    return sb.toString();
  }

  private void insertLegs(Logger l, Statement s, int run)
    throws SQLException{
    String legID="";
    int legType=-1;
    long startTime=-1;
    long endTime=-1;
    String startLoc="";
    String endLoc="";
    String search="";
    
    int rows=0;

    long curStartTime=0;
    long curEndTime=0;

    Calendar dCal=Calendar.getInstance();
    Calendar tCal=Calendar.getInstance();

    ResultSet rs=s.executeQuery(getQuery(l, run));
    //Now actually do the walk.
    while(rs.next()){
      if (rs.getString(10).charAt(0) == 't') // ignore low fidelity assets
	continue;
      //First we need to get the times -- is there a better way?
      dCal.setTime(rs.getDate(3));
      tCal.setTime(rs.getTime(3));
      dCal.add(Calendar.HOUR_OF_DAY,tCal.get(Calendar.HOUR_OF_DAY));
      dCal.add(Calendar.MINUTE,tCal.get(Calendar.MINUTE));
      dCal.add(Calendar.SECOND,tCal.get(Calendar.SECOND));
      dCal.add(Calendar.MILLISECOND,tCal.get(Calendar.MILLISECOND));
      curStartTime=dCal.getTime().getTime();
      dCal.setTime(rs.getDate(4));
      tCal.setTime(rs.getTime(4));
      dCal.add(Calendar.HOUR_OF_DAY,tCal.get(Calendar.HOUR_OF_DAY));
      dCal.add(Calendar.MINUTE,tCal.get(Calendar.MINUTE));
      dCal.add(Calendar.SECOND,tCal.get(Calendar.SECOND));
      dCal.add(Calendar.MILLISECOND,tCal.get(Calendar.MILLISECOND));
      curEndTime=dCal.getTime().getTime();

      //If the search matches the last guy, then do our comparison:
      if(search.equals(rs.getString(7))){ // compare asset ids
	if(!endLoc.equals(rs.getString(5))){ // end geoloc of last != start geoloc of this
	  insertRow(s,run,
		    legID,legType,startTime,endTime,startLoc,endLoc,search,
		    rs.getString(8),
		    rs.getString(9),
		    PRE);
	  insertRow(s,run,
		    rs.getString(1),
		    rs.getInt(2),
		    curStartTime,
		    curEndTime,
		    rs.getString(5),
		    rs.getString(6),
		    rs.getString(7),
		    rs.getString(8),
		    rs.getString(9),
		    POST);
	}
      }
      //Setup for next row:
      legID=rs.getString(1);
      legType=rs.getInt(2);
      startTime=curStartTime;
      endTime=curEndTime;
      startLoc=rs.getString(5);
      endLoc=rs.getString(6);
      search=rs.getString(7);
      rows++;
      if(rows%500==0){
	l.logMessage(Logger.NORMAL,Logger.DB_QUERY,"Processed "+rows+" rows");
      }
    }
  }

  private void insertRow(Statement s, int run,
			 String legID,
			 int legType,
			 long startTime,
			 long endTime,
			 String startLoc,
			 String endLoc,
			 String search,
						 String owner,
						 String name,
			 int pre_post)throws SQLException{
    StringBuffer sb=new StringBuffer();
    sb.append("INSERT INTO ");
    sb.append(getTableName(run));
    sb.append(" (");
    addCarrierFields(sb);
    sb.append(COL_SEARCH);sb.append(",");
    sb.append(COL_OWNER);sb.append(",");
    sb.append(COL_NAME);sb.append(",");
    sb.append(COL_PRE_OR_POST);
    sb.append(") VALUES('");
    sb.append(legID);sb.append("',");
    sb.append(legType);sb.append(",");
    sb.append(dbConfig.dateToSQL(startTime));sb.append(",");
    sb.append(dbConfig.dateToSQL(endTime));sb.append(",'");
    sb.append(startLoc);sb.append("','");
    sb.append(endLoc);sb.append("','");
    sb.append(search);sb.append("','");
    sb.append(owner);sb.append("','");
    sb.append(name);sb.append("',");
    sb.append(pre_post);
    sb.append(")");
    s.executeUpdate(sb.toString());
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertLegs(l,s,run);
  }
    
  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Leg ID",
		      "Leg Type",
		      "Start Time",
		      "end Time",
		      "Start Loc ID",
		      "End Loc ID",
		      getSearchTypeString()+" ID",
		      "Owner",
		      "Name",
		      "Pre/Post Gap"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_LEG,
		 TYPE_ENUM_1,
		 TYPE_DATETIME,
		 TYPE_DATETIME,
		 TYPE_LOC,
		 TYPE_LOC,
		 getSearchColType(),
				 TYPE_STRING,
				 TYPE_STRING,
		 TYPE_INT};
    return types;
  }

  /**Render enums**/
  protected String renderEnum(int whichEnum, int enum){
    switch(whichEnum){
    case TYPE_ENUM_1:
      try{
	return DGPSPConstants.LEG_TYPES[enum];
      }catch(Exception e){
	return Integer.toString(enum);
      }
    }
    return Integer.toString(enum);
  }

  //InnerClasses:
  ///////////////
}
