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

import java.util.Date;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;

import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Looks at all the transported assets and tests for deviation from preferred arrival time.  
 *
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/
public class ArrivalTimeTest extends Test{
  
  //Constants:
  ////////////
  static final String COL_NUMASSETS = "numassets";
  static final String COL_NUMDIFF = "numdiff";
  static final String COL_ARRIVALTIMEDIFF = "avgdiff";
  static final int MILLIS_PER_HOUR = 60 * 60 * 1000;

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public ArrivalTimeTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Average deviation from desired arrival time.";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "ArrivalTime";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit", "Number of Assets", "Number Differing From Best Arrival", "Average Deviation (hrs)"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_INT,TYPE_INT,TYPE_INT};
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
		    COL_NUMASSETS+" INTEGER NOT NULL,"+
		    COL_NUMDIFF+" INTEGER NOT NULL,"+
		    COL_ARRIVALTIMEDIFF+" INTEGER NOT NULL"+
		    ")");
  }

  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String itinTable = Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,run);
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String arrivalTimeTable = Controller.getTableName(DGPSPConstants.ARRIVAL_TIME_TABLE,run);

    String assetOwner = assetTable+"."+DGPSPConstants.COL_OWNER;
    String prefOwner = arrivalTimeTable+"."+DGPSPConstants.COL_OWNER;
    String assetAssetid = assetTable+"."+DGPSPConstants.COL_ASSETID;
    String itinAssetid = itinTable+"."+DGPSPConstants.COL_ASSETID;
    String itinLegid = itinTable+"."+DGPSPConstants.COL_LEGID;
    String legLegid = legTable+"."+DGPSPConstants.COL_LEGID;
    String legEndtime = legTable+"."+DGPSPConstants.COL_ENDTIME;
    String preferredArrivaltime = arrivalTimeTable+"."+DGPSPConstants.COL_PREFERREDARRIVALTIME;

    String sqlQuery =
      // "select "+owner+", "+assetAssetid+", "+legEndtime+"\n"+
      "select "+assetOwner+", "+assetAssetid+", "+legEndtime+","+preferredArrivaltime+"\n"+
      // "from "+assetTable+", "+itinTable+", "+legTable+"\n"+
      "from "+assetTable+", "+itinTable+", "+legTable+", "+arrivalTimeTable+"\n"+
      "where "+assetAssetid+" = "+itinAssetid+"\n"+ 
      " and "+itinLegid+" = "+legLegid+"\n"+
      " and "+assetOwner+" = "+prefOwner+"\n"+
      "order by "+assetOwner+", "+assetAssetid+", "+legEndtime+"\n"+
      ";";

    // System.out.println("\n ArrivalTimeTest.getQuery() - "+sqlQuery);

    return sqlQuery;
  }

  protected void insertResults (Logger l, Statement s, int run) {    
    ResultSet rs=null;
    String sql = null;
	
    try {
      sql = getQuery(run);
      l.logMessage(Logger.MINOR,Logger.DB_WRITE,"SQL Query was " + sql);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimeTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }

    try {
      String prevOwner = "";
      String prevAssetid = "";
      String prevEndTime = "";
      String prevPreferredTime = "";
      boolean firstIteration = true;
      int assetsForUnitCount = 0;
      int assetsNonBestArrivalCount = 0;
      Collection endTimesForUnit = new LinkedList();
      
      while(rs.next()){
	String owner = rs.getString(1);
	String assetid = rs.getString(2);
	String endTime = rs.getString(3);
	String preferredTime = rs.getString(4);
	 
	// Don't do anything for the first row
	if (!firstIteration) {

	  // Beginning legs for a new asset.  Hence the previous leg processed was the
	  // last leg of the previous asset.
	  if (!assetid.equals(prevAssetid)) {
	    // Increment the number of assets for this unit
	    assetsForUnitCount++;

	    // If arrival time is not best, keep track of it
	    if (!prevEndTime.equals(prevPreferredTime)) {
	      assetsNonBestArrivalCount++;
	      endTimesForUnit.add(prevEndTime);
	    }
	  } 

	  // Insert an entry if we're starting to look at another unit.  
	  if (!owner.equals(prevOwner)) {
	    
//       System.out.println("ArrivalTimeTest.insertRow() - unit = " + prevOwner + 
//  			 " total = " + assetsForUnitCount + 
//  			 " diff = " + assetsNonBestArrivalCount + 
//  			 " endTimesForUnit=" + endTimesForUnit);

	    insertRow(l, s, run, 
		      prevOwner,
		      assetsForUnitCount, 
		      assetsNonBestArrivalCount,
		      computeArrivalTimeDiff(endTimesForUnit, prevPreferredTime, assetsForUnitCount));
	    assetsForUnitCount = 0;
	    assetsNonBestArrivalCount = 0;
	    endTimesForUnit.clear();
	    }
	}
	prevOwner = owner;
	prevAssetid = assetid;
	prevEndTime = endTime;
	prevPreferredTime = preferredTime;
	firstIteration = false;
      }

      // Enter last value
      assetsForUnitCount++;
      if (!prevEndTime.equals(prevPreferredTime)) {
	assetsNonBestArrivalCount++;
	endTimesForUnit.add(prevEndTime);
      }

      insertRow(l, s, run, 
		prevOwner,
		assetsForUnitCount, 
		assetsNonBestArrivalCount,
		computeArrivalTimeDiff(endTimesForUnit, prevPreferredTime, assetsForUnitCount));

    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimeTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  // Computes the average of the set of end times versus the preferred end time.
  protected int computeArrivalTimeDiff(Collection endTimesForUnit, String preferredTimeString, int total) {
    long sum = 0;
    try {
      SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
      long preferredTime = format.parse(preferredTimeString).getTime() / MILLIS_PER_HOUR;
      
      for (Iterator iter = endTimesForUnit.iterator(); iter.hasNext();) {
	long actualEndTime = format.parse((String) iter.next()).getTime() / MILLIS_PER_HOUR;
	sum += preferredTime - actualEndTime;
      }
    } catch (ParseException e) {
      System.out.println ("ArrivalTimeTest.computeArrivalTimeDiff() - could not parse date format.");
    }

    return ((int)(sum / total));
  }

  // Computes the "standard deviation" of the set of end times versus the preferred
  // end time.
  protected int computeSumSquareArrivalTimeDiff(Collection endTimesForUnit, String preferredTimeString, int total) {

    double squaresum = 0;
    try {
      SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
      long preferredTime = format.parse(preferredTimeString).getTime() / MILLIS_PER_HOUR;

      for (Iterator iter = endTimesForUnit.iterator(); iter.hasNext();) {
	long actualEndTime = format.parse((String) iter.next()).getTime() / MILLIS_PER_HOUR;
	squaresum += (preferredTime - actualEndTime) * (preferredTime - actualEndTime);
      }
    } catch (ParseException e) {
      System.out.println ("ArrivalTimeTest.computeSumSquareArrivalTimeDiff() - could not parse date format.");
    }

    return ((int)Math.sqrt(squaresum / total));
  }

  protected void insertRow(Logger l, Statement s, int run,
			   String owner, 
			   int count, 
			   int diffcount, 
			   int avgArrivalTimeDiff) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
      sb.append(COL_NUMASSETS);sb.append(",");
      sb.append(COL_NUMDIFF);sb.append(",");
      sb.append(COL_ARRIVALTIMEDIFF);
      sb.append(") VALUES('");
      sb.append(owner);sb.append("','");
      sb.append(count);sb.append("','");
      sb.append(diffcount);sb.append("','");
      sb.append(avgArrivalTimeDiff);
      sb.append("')");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimeTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  //InnerClasses:
  ///////////////
}





