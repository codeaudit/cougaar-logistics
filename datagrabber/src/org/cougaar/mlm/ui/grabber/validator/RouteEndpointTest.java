/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * Looks for missing people
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/

public class RouteEndpointTest extends Test{
  
  //Constants:
  ////////////
  
  //Variables:
  ////////////
    
  //Constructors:
  ///////////////

  public RouteEndpointTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_ERROR;
  }

  /**for gui**/
  public String getDescription(){
    return "Legs with route that don't match start and end locations.";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "RouteEndpoint";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"LegID","LegStart","LegEnd","RouteID","RouteStart","RouteEnd","Element"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_STRING,TYPE_STRING,
		 TYPE_STRING,TYPE_STRING,TYPE_STRING, TYPE_INT};
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
		    DGPSPConstants.COL_LEGID+" VARCHAR(255) NOT NULL,"+
		    "leg_"+DGPSPConstants.COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		    "leg_"+DGPSPConstants.COL_ENDLOC+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_ROUTEID+" VARCHAR(255) NOT NULL,"+
		    "route_"+DGPSPConstants.COL_STARTLOC+" VARCHAR(255) NOT NULL,"+
		    "route_"+DGPSPConstants.COL_ENDLOC+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_ROUTE_ELEM_NUM+" INTEGER NOT NULL"+
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
		   "RouteEndpointTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    
    try {
      boolean nextIsEndline = true;

      while(rs.next()){
	String leg = rs.getString(1);
	String legStart = rs.getString(2);
	String legEnd = rs.getString(3);
	String route = rs.getString(4);
	String routeStart = rs.getString(5);
	String routeEnd = rs.getString(6);
	int elem = rs.getInt(7);

	if (nextIsEndline && !legEnd.equals(routeEnd)) {
	  insertRow(l,s,run,leg,legStart,legEnd,route,routeStart,routeEnd,elem);
	}
	nextIsEndline = false;
	if (elem == 0) {
	  if (!legStart.equals(routeStart)) {
	    insertRow(l,s,run,leg,legStart,legEnd,route,routeStart,routeEnd,elem);
	  }
	  nextIsEndline = true;
	}	
      }    
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "RouteEndpointTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getQuery (int run) {
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String routeTable = Controller.getTableName(DGPSPConstants.ROUTE_ELEMENTS_TABLE,run);

    String legid = legTable+"."+DGPSPConstants.COL_LEGID;    
    String legStart = legTable+"."+DGPSPConstants.COL_STARTLOC;
    String legEnd = legTable+"."+DGPSPConstants.COL_ENDLOC;
    String routeStart = routeTable+"."+DGPSPConstants.COL_STARTLOC;
    String routeEnd = routeTable+"."+DGPSPConstants.COL_ENDLOC;
    String elem = DGPSPConstants.COL_ROUTE_ELEM_NUM;
    String legRouteid = legTable+"."+DGPSPConstants.COL_ROUTEID;
    String routeRouteid = routeTable+"."+DGPSPConstants.COL_ROUTEID;

    String sqlQuery =
      "select "+legid+", "+legStart+", "+legEnd+",\n"+
      routeRouteid+", "+routeStart+", "+routeEnd+", "+elem+"\n"+
      "from "+legTable+", "+routeTable+"\n"+
      "where "+legRouteid+" = "+routeRouteid+"\n"+
      "order by "+legid+", "+ legRouteid+", "+elem+" desc";

    System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  private void insertRow(Logger l, Statement s, int run,
			 String leg, String legStart, String legEnd,
			 String route, String routeStart, String routeEnd, int elem) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_LEGID);sb.append(",");
      sb.append("leg_"+DGPSPConstants.COL_STARTLOC);sb.append(",");
      sb.append("leg_"+DGPSPConstants.COL_ENDLOC);sb.append(",");
      sb.append(DGPSPConstants.COL_ROUTEID);sb.append(",");
      sb.append("route_"+DGPSPConstants.COL_STARTLOC);sb.append(",");
      sb.append("route_"+DGPSPConstants.COL_ENDLOC);sb.append(",");
      sb.append(DGPSPConstants.COL_ROUTE_ELEM_NUM);
      sb.append(") VALUES('");
      sb.append(leg);sb.append("','");
      sb.append(legStart);sb.append("','");
      sb.append(legEnd);sb.append("','");
      sb.append(route);sb.append("','");
      sb.append(routeStart);sb.append("','");
      sb.append(routeEnd);sb.append("',");
      sb.append(elem);
      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "RouteEndpointTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  //InnerClasses:
  ///////////////
}


