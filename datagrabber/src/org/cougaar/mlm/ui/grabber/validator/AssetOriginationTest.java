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
public class AssetOriginationTest extends Test{
  
  //Constants:
  ////////////
  
  static final String COL_ASSETNUMBER = "numberassets";
  
  //Variables:
  ////////////
    
    boolean people;
    
  
  //Constructors:
  ///////////////

  public AssetOriginationTest(DBConfig dbConfig, boolean people){
    super(dbConfig);
    this.people = people;
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_WARNING;
  }

  /**for gui**/
  public String getDescription(){
    return "Units with mixed origin ("+(people?"":"non-")+"people)";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "AssetOrigination_"+(people?"":"non")+"people_";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Owner Unit ID","Origin Geoloc","Origin Name","Number of Assets",};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_STRING,TYPE_STRING,TYPE_INT};
    return types;
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  private void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_PRETTYNAME+" VARCHAR(255) NOT NULL,"+
		    COL_ASSETNUMBER+" INTEGER NOT NULL"+
		    ")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    String lastAsset = null;
    ResultSet rs=null;
    String sql = null;
    Map unitToMap = new HashMap();
    Map locIDToName = new HashMap ();
	
    try {
      sql = getQuery(run);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "AssetOriginationTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    
    try {
      while(rs.next()){
	String thisAsset = rs.getString(DGPSPConstants.COL_ASSETID);
	String unit = rs.getString(DGPSPConstants.COL_OWNER);
	String startloc = rs.getString(DGPSPConstants.COL_STARTLOC);
	String prettyname = rs.getString(DGPSPConstants.COL_PRETTYNAME);
	
	if (lastAsset == null)
	  lastAsset = thisAsset;
	
	if (!thisAsset.equals (lastAsset)) {
	  Map startlocToCount = (Map)unitToMap.get(unit);
	  if (startlocToCount == null) {
	    startlocToCount = new HashMap();
	    unitToMap.put(unit, startlocToCount);
	  }

	  Integer count = (Integer)startlocToCount.get(startloc);
	  if (count == null) {
	    count = new Integer(0);
	    startlocToCount.put(startloc,count);
	    locIDToName.put(startloc,prettyname);
	  }
	  int newcount = count.intValue() + 1;
	  startlocToCount.put(startloc,new Integer(newcount));
	  
	  lastAsset = thisAsset;
	}
      }
      
      for (Iterator one = unitToMap.keySet().iterator(); one.hasNext();) {
	String unitKey = (String)one.next();
	Map startlocToCount = (Map)unitToMap.get(unitKey);
	if (startlocToCount.size() > 1) {
	  for (Iterator two = startlocToCount.keySet().iterator(); two.hasNext();) {
	    String startloc = (String)two.next();
	    int count = ((Integer)startlocToCount.get(startloc)).intValue();
	    insertRow(l,s,run,unitKey,startloc,(String)locIDToName.get(startloc),count);
	  }
	}	
      }    
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "AssetOriginationTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String instanceTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String prototypeTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String itineraryTable = Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,run);
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String locTable = Controller.getTableName(DGPSPConstants.LOCATIONS_TABLE,run);
    
    String instAssetid = instanceTable+"."+DGPSPConstants.COL_ASSETID;
    String itinAssetid = itineraryTable+"."+DGPSPConstants.COL_ASSETID;
    String itinConvid = itineraryTable+"."+DGPSPConstants.COL_LEGID;
    String legConvid = legTable+"."+DGPSPConstants.COL_LEGID;
    String ownerid = DGPSPConstants.COL_OWNER;
    String startloc = legTable + "." + DGPSPConstants.COL_STARTLOC;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;
    String instProtoid = instanceTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = prototypeTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String personConstant = (new Integer(DGPSPConstants.ASSET_CLASS_PERSON)).toString();
    String starttime = DGPSPConstants.COL_STARTTIME;

    String locid = locTable + "." + DGPSPConstants.COL_LOCID;
    String prettyname = locTable + "." + DGPSPConstants.COL_PRETTYNAME;

    String sqlQuery =
      "select "+instAssetid+", "+ownerid+", "+startloc+", "+prettyname+"\n"+
      "from "+instanceTable+", "+itineraryTable+", "+legTable+", "+prototypeTable+", "+locTable+"\n"+
      "where "+instAssetid+" = "+itinAssetid+"\n"+
      "and "+itinConvid+" = "+legConvid+"\n"+
      "and "+instProtoid+" = "+protoProtoid+"\n"+
      "and "+assetClass+(people?" = ":" <> ")+personConstant+"\n"+
      "and "+startloc + " = " +locid+"\n"+
      "order by "+instAssetid+", "+starttime;

    System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  private void insertRow(Logger l, Statement s, int run, 
			 String ownerID, String startlocation, String prettyname, int count) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
      sb.append(DGPSPConstants.COL_STARTLOC);sb.append(",");
      sb.append(DGPSPConstants.COL_PRETTYNAME);sb.append(",");
      sb.append(COL_ASSETNUMBER);
      sb.append(") VALUES('");
      sb.append(ownerID);sb.append("','");
      sb.append(startlocation);sb.append("','");
      sb.append(prettyname);sb.append("',");
      sb.append(count);
      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "AssetOriginationTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  //InnerClasses:
  ///////////////
}


