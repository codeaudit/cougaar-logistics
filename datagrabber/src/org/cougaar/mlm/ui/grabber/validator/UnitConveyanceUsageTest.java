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

/**
 * Looks for missing people
 *
 * @since 2/26/01
 **/
public class UnitConveyanceUsageTest extends Test{
  
  //Constants:
  ////////////
  static final String COL_NUMCONVEYANCES = "numberconveyances";

  //Variables:
  ////////////
  private boolean showSelfDeployed = false;

  //Constructors:
  ///////////////

  public UnitConveyanceUsageTest(DBConfig dbConfig, boolean showSelfDeployed){
    super(dbConfig);
    this.showSelfDeployed = showSelfDeployed;
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Conveyance Usage By Unit";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "UnitConveyanceUsage";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit","ConveyanceClass","ConveyanceType","MissionsDeployed"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_STRING,TYPE_STRING,TYPE_STRING};
    return types;
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_CONVEYANCE_TYPE+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_CONVEYANCEID+" VARCHAR(255) NOT NULL,"+
		    COL_NUMCONVEYANCES+" INTEGER NOT NULL"+
		    ")");
  }

  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String itinTable = Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,run);
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String instTable = Controller.getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,run);

    String owner = assetTable+"."+DGPSPConstants.COL_OWNER;
    String convInstid = instTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String convType = protoTable+"."+DGPSPConstants.COL_CONVEYANCE_TYPE;
    String legStarttime = legTable+"."+DGPSPConstants.COL_STARTTIME;
    String legEndtime = legTable+"."+DGPSPConstants.COL_ENDTIME;

    String convProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String assetAssetid = assetTable+"."+DGPSPConstants.COL_ASSETID;
    String itinAssetid = itinTable+"."+DGPSPConstants.COL_ASSETID;
    String itinLegid = itinTable+"."+DGPSPConstants.COL_LEGID;
    String legLegid = legTable+"."+DGPSPConstants.COL_LEGID;
    String legConvid = legTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String instConvid = instTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String instProtoid = instTable+"."+DGPSPConstants.COL_PROTOTYPEID;

    String sqlQuery =
      "select "+owner+", "+convType+", "+convProtoid+", "+convInstid+", "+legLegid+", "+legStarttime+", "+legEndtime+"\n"+
      "from "+assetTable+", "+itinTable+", "+legTable+", "+instTable+", "+protoTable+"\n"+
      "where "+assetAssetid+" = "+itinAssetid+"\n"+ 
      " and "+itinLegid+" = "+legLegid+"\n"+
      " and "+legConvid+" = "+instConvid+"\n"+
      " and "+instProtoid+" = "+convProtoid+"\n"+
      "group by "+legLegid+"\n"+
      "order by "+owner+", "+convType+", "+convProtoid+", "+convInstid+", "+legStarttime+"\n"+
      ";";

    System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }

  protected void insertResults (Logger l, Statement s, int run) {    
    ResultSet rs=null;
    String sql = null;
	
    try {
      sql = getQuery(run);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitConveyanceUsageTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }

    try {
      String prevOwner = "";
      int prevConvClass = 0;
      String prevConvType = "";
      String prevConvInst = "";
      String prevEndTime = "";
      int convTypeMissions = 0;
      boolean firstIteration = true;
      
      while(rs.next()){
	String owner = rs.getString(1);
	int convClass = Integer.parseInt(rs.getString(2));
	String convType = rs.getString(3);
	String convInst = rs.getString(4);
	String startTime = rs.getString(6);
	String endTime = rs.getString(7);
	 
	// Don't do anything for the first row
	if (!firstIteration) {

	  // Found a new mission if the start time of this leg is not equal
	  // to the end time of the previous leg.  
	  if (!startTime.equals(prevEndTime)) {
	    convTypeMissions++;
	  } 

	  // A rare case where the first leg of a new conveyance happens to 
	  // start exactly when the last leg of the previous conveyance ended.  
	  // Make sure we count the last mission of the previous conveyance. 
	  else if (!convInst.equals(prevConvInst)) {
	    convTypeMissions++;
	  }

	  // Insert an entry if we're starting to look at another conveyance type.
	  if (!convType.equals(prevConvType)) {
	    if ((prevConvClass != DGPSPConstants.CONV_TYPE_SELF_PROPELLABLE) ||
		showSelfDeployed) {
	      insertRow(l, s, run, 
			prevOwner, 
			DGPSPConstants.CONVEYANCE_TYPES[prevConvClass], 
			prevConvType, 
			convTypeMissions);
	    }
	    convTypeMissions = 0;
	  }
	}
	prevOwner = owner;
	prevConvClass = convClass;
	prevConvType = convType;
	prevConvInst = convInst;
	prevEndTime = endTime;
	firstIteration = false;
      }

    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitConveyanceUsageTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  protected void insertRow(Logger l, Statement s, int run,
			 String owner, String convclass, String convtype, int count) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
      sb.append(DGPSPConstants.COL_CONVEYANCE_TYPE);sb.append(",");
      sb.append(DGPSPConstants.COL_CONVEYANCEID);sb.append(",");
      sb.append(COL_NUMCONVEYANCES);
      sb.append(") VALUES('");
      sb.append(owner);sb.append("','");
      sb.append(convclass);sb.append("','");
      sb.append(convtype);sb.append("',");
      sb.append(count);
      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitConveyanceUsageTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  //InnerClasses:
  ///////////////
}





