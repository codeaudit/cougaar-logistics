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

import org.cougaar.mlm.ui.grabber.validator.Graphable;

/**
 * Looks for missing people
 *
 * @since 2/26/01
 **/
public class MissionInfo extends Test implements Graphable {
  
  //Constants:
  ////////////
  
  public final String COL_AIRPORT = "Airport";
  public final String COL_DAY = "Day";
  public final String COL_MISSION = "Missions";
  
  //Variables:
  ////////////

  boolean byAirport;
  public boolean debug = "true".equals (System.getProperty("MissionInfo.debug", "false"));

  //Constructors:
  ///////////////

  public MissionInfo(DBConfig dbConfig, boolean byAirport) {
    super(dbConfig);
    this.byAirport = byAirport;
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Missions per Day" + (byAirport?" by Airport":"");
  }

  /**Base name**/
  protected String getRawTableName(){
    return "MissionInfo"+(byAirport?"byAirport":"");
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers = new String[byAirport ? 3 : 2];
    int count = 0;
    if (byAirport) headers[count++]="Airport";
    headers[count++]="Day";
    headers[count++]="Missions";
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int count = 0;
    int[] types= new int[byAirport ? 3 : 2];
    if (byAirport) types[count++]=TYPE_STRING;
    types[count++]=TYPE_INT;
    types[count++]=TYPE_INT;
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
		    (byAirport?(COL_AIRPORT+ " VARCHAR(255) NOT NULL,"):"")+
		    COL_DAY+" INTEGER NOT NULL,"+
		    COL_MISSION+" INTEGER NOT NULL"+
		    ")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;
    
    try {
      sql = getQuery(run);
      l.logMessage(Logger.MINOR,Logger.DB_WRITE,"SQL is " + sql);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "MissionInfo.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      HashMap airportDayToMissions = new HashMap(); 
      while(rs.next()){
	String missionid = rs.getString(1);
	int day = rs.getInt(2);
	String location = rs.getString(3);
	
	Integer missions = (Integer)airportDayToMissions.get(formKey(location, day));
	if (missions == null) missions = new Integer(0);
	airportDayToMissions.put(formKey(location, day),new Integer(missions.intValue() + 1));
      }    
      for(Iterator iter = airportDayToMissions.keySet().iterator(); iter.hasNext();) {
	Object key = iter.next();
	insertRow(l,s,run,
		  disectKeyForAirport(key), 
		  disectKeyForDay(key), 
		  ((Integer)airportDayToMissions.get(key)).intValue());
      }      
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "MissionInfo.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String locationTable = Controller.getTableName(DGPSPConstants.LOCATIONS_TABLE,run);
    String convInstTable = Controller.getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,run);
    String convProtoTable = Controller.getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,run);

    String missionid = DGPSPConstants.COL_MISSIONID;
    String end = DGPSPConstants.COL_ENDTIME;
    String location = DGPSPConstants.COL_PRETTYNAME;
    String legEndlocid = legTable+"."+DGPSPConstants.COL_ENDLOC;
    String locLocid = locationTable+"."+DGPSPConstants.COL_LOCID;
    String instConvid = convInstTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String legConvid = legTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String instProtoid = convInstTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = convProtoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String conveyancetype = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String legtype = DGPSPConstants.COL_LEGTYPE;

    String sqlQuery =
      "select "+missionid+", dayofyear("+end+"), "+location+"\n"+
      "from "+legTable+", "+locationTable+", "+convInstTable+", "+convProtoTable+"\n"+
      "where "+legEndlocid+" = "+locLocid+"\n"+
      "and "+instConvid+" = "+legConvid+"\n"+
      "and "+instProtoid+" = "+protoProtoid+"\n"+
      "and "+conveyancetype+" = "+DGPSPConstants.CONV_TYPE_PLANE+"\n"+
      "and "+legtype+" = "+DGPSPConstants.LEG_TYPE_TRANSPORTING+"\n"+
      "order by "+missionid;

    if (debug)
      System.out.println ("MissionInfo.getQuery - sql was:\n" + 
			  sqlQuery);
	
    return sqlQuery;
  }
  
  protected void insertRow(Logger l, Statement s, int run, String airport, int day, int missions) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      if (byAirport) {sb.append(COL_AIRPORT);sb.append(",");}
      sb.append(COL_DAY + ",");
      sb.append(COL_MISSION);
      sb.append(") VALUES(");
      if (byAirport)  {sb.append("'" + airport + "',");}
      sb.append(day + ","); 
      sb.append(missions); 
      sb.append(")");
	  
      sql = sb.toString();
      if (debug)
	System.out.println(sql);
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "MissionInfo.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }


  private static final String divider = "^"; 
  private String formKey(String partA, int partB) {
    if (byAirport) {
        return "default" + divider + partB;
    } else {
        return partA + divider + partB;
    }
  }
  private String disectKeyForAirport(Object obj) {
    String key = (String)obj;
    return key.substring(0,key.indexOf(divider));
  }
  private int disectKeyForDay(Object obj) {
    String key = (String)obj;
    Integer retval = new Integer(key.substring(key.indexOf(divider) + 1));
    return retval.intValue();
  }
  //InnerClasses:
  ///////////////

}





