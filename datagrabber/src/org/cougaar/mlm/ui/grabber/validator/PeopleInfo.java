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

import org.cougaar.mlm.ui.grabber.validator.Graphable;

/**
 * Looks for missing people
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/
public class PeopleInfo extends Test implements Graphable {
  
  //Constants:
  ////////////
  
  public final String COL_AIRPORT = "Airport";
  public final String COL_DAY = "Day";
  public final String COL_PEOPLE = "People";
  
  //Variables:
  ////////////
    
  public boolean debug = "true".equals (System.getProperty("PeopleInfo.debug", "false"));
  public boolean startian;

  //Constructors:
  ///////////////

  public PeopleInfo(DBConfig dbConfig, boolean startian) {
    super(dbConfig);
    this.startian = startian;
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "People per Day";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "PeopleInfo";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers = {"Airpot", "Day", "People"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types= {TYPE_STRING, TYPE_INT, TYPE_INT};
    return types;
  }

  /** methods for interface Graphable */
  public int getXAxisColumn () { return 1; }
  public int getYAxisColumn () { return getHeaders().length; }
  public int getZAxisColumn () { return -1; }
  public boolean hasThirdDimension () { return false; }

  /** 
   * if the third dimension is too wide, e.g. asset types, 
   * show one graph per X dimension entry 
   **/
  public boolean showMultipleGraphs () { return false; }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    COL_AIRPORT+ " VARCHAR(255) NOT NULL,"+
		    COL_DAY+" INTEGER NOT NULL,"+
		    COL_PEOPLE+" INTEGER NOT NULL"+
		    ")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;
    
    try {
      sql = getQuery(run);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "PeopleInfo.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      HashMap airportDayToPeople = new HashMap(); 
      while(rs.next()){
	String assetid = rs.getString(1);
	int assetnum = rs.getInt(2);
	int day = rs.getInt(3);
	String location = rs.getString(4);
	
	Integer people = (Integer)airportDayToPeople.get(formKey(location, day));
	if (people == null) people = new Integer(0);
	airportDayToPeople.put(formKey(location, day),new Integer(people.intValue() + assetnum));
      }    
      for(Iterator iter = airportDayToPeople.keySet().iterator(); iter.hasNext();) {
	Object key = iter.next();
	insertRow(l,s,run,
		  disectKeyForAirport(key), 
		  disectKeyForDay(key), 
		  ((Integer)airportDayToPeople.get(key)).intValue());
      }      
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "PeopleInfo.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String itinTable = Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,run);
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String locationTable = Controller.getTableName(DGPSPConstants.LOCATIONS_TABLE,run);
    String convInstTable = Controller.getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,run);
    String convProtoTable = Controller.getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,run);

    String aggnumber = assetTable+"."+DGPSPConstants.COL_AGGREGATE;
    String start = DGPSPConstants.COL_STARTTIME;
    String end = DGPSPConstants.COL_ENDTIME;
    String keydate = startian ? start : end;
    String assetclass = DGPSPConstants.COL_ASSET_CLASS;
    String location = DGPSPConstants.COL_PRETTYNAME;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String itinAssetid = itinTable+"."+DGPSPConstants.COL_ASSETID;
    String instAssetid = assetTable+"."+DGPSPConstants.COL_ASSETID;
    String itinLegid = itinTable+"."+DGPSPConstants.COL_LEGID;
    String legLegid = legTable+"."+DGPSPConstants.COL_LEGID;
    String legLocid = legTable+"."+DGPSPConstants.COL_ENDLOC;
    String locLocid = locationTable+"."+DGPSPConstants.COL_LOCID;
    String instConvid = convInstTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String legConvid = legTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String instConvProtoid = convInstTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoConvProtoid = convProtoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String conveyancetype = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String legtype = DGPSPConstants.COL_LEGTYPE;

    String sqlQuery =
      "select "+instAssetid+", "+aggnumber+", dayofyear("+keydate+"), "+location+"\n"+
      "from "+assetTable+", "+protoTable+", "+legTable+", "+locationTable+", "+itinTable+",\n"+
      convInstTable+", "+convProtoTable+"\n"+
      "where "+instProtoid+" = "+protoProtoid+"\n"+
      "and "+itinAssetid+" = "+instAssetid+"\n"+
      "and "+itinLegid+" = "+legLegid+"\n"+
      "and "+legLocid+" = "+locLocid+"\n"+
      "and "+assetclass+" = "+DGPSPConstants.ASSET_CLASS_PERSON+"\n"+
      "and "+instConvid+" = "+legConvid+"\n"+
      "and "+instConvProtoid+" = "+protoConvProtoid+"\n"+
      "and "+conveyancetype+" = "+DGPSPConstants.CONV_TYPE_PLANE+"\n"+
      "and "+legtype+" = "+DGPSPConstants.LEG_TYPE_TRANSPORTING+"\n"+
      "order by "+instAssetid+", "+end;

    if (debug)
      System.out.println ("PeopleInfo.getQuery - sql was:\n" + 
			  sqlQuery);
	
    return sqlQuery;
  }
  
  protected void insertRow(Logger l, Statement s, int run, String airport, int day, int people) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(COL_AIRPORT);sb.append(",");
      sb.append(COL_DAY + ",");
      sb.append(COL_PEOPLE);
      sb.append(") VALUES(");
      sb.append("'" + airport + "',");
      sb.append(day + ","); 
      sb.append(people); 
      sb.append(")");
	  
      sql = sb.toString();
      if (debug)
	System.out.println(sql);
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "PeopleInfo.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }


  private static final String divider = "^"; 
  private String formKey(String partA, int partB) {
    return partA + divider + partB;
  }
  private String disectKeyForAirport(Object obj) {
    String key = (String)obj;
    System.out.println(key+" : "+ key.substring(0,key.indexOf(divider)));
    return key.substring(0,key.indexOf(divider));
  }
  private int disectKeyForDay(Object obj) {
    String key = (String)obj;
    System.out.println(key+" : "+key.substring(key.indexOf(divider)+divider.length()));
    Integer retval = new Integer(key.substring(key.indexOf(divider)+divider.length()));
    return retval.intValue();
  }
  //InnerClasses:
  ///////////////

}





