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

import java.util.Date;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Looks at all the transported assets and tests for deviation from preferred arrival time.  
 *
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2/26/01
 **/
public class ArrivalTimePrecisionTest extends Test{
  
  //Constants:
  ////////////
  static final String COL_NUMBER = "number";
  static final String COL_MEAN = "mean";
  static final String COL_STDDEV = "stddev";
  static final SimpleDateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
  static final int MILLIS_PER_HOUR = 60 * 60 * 1000;

  //Variables:
  ////////////


  //Constructors:
  ///////////////

  public ArrivalTimePrecisionTest(DBConfig dbConfig){
    super(dbConfig);
    setPriorityLevel(Test.PRIORITY_CORE);
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Arrival time precision";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "arrivalprecision";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit", "Number of Assets", "Arrival Time Mean", "Arrival Time Standard Deviation (hrs)"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_INT,TYPE_DATETIME,TYPE_INT};
    return types;
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    StringBuffer sb=new StringBuffer();
    createTable(s, run);
    insertResults(l,s,run);
  }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
		    COL_NUMBER+" INTEGER NOT NULL,"+
		    COL_MEAN+" DATETIME NOT NULL,"+
		    COL_STDDEV+" INTEGER NOT NULL"+
		    ")");
  }

  protected String getQuery (int run) {
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
      "select "+assetOwner+", "+assetAssetid+", "+legEndtime+"\n"+
      "from "+assetTable+", "+itinTable+", "+legTable+"\n"+
      "where "+assetAssetid+" = "+itinAssetid+"\n"+ 
      " and "+itinLegid+" = "+legLegid+"\n"+
      "order by "+assetOwner+", "+assetAssetid+", "+legEndtime+"\n"+
      ";";

    // System.out.println("\n ArrivalTimePrecisionTest.getQuery() - "+sqlQuery);

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
		   "ArrivalTimePrecisionTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }

    try {
      String prevOwner = "";
      String prevAssetid = "";
      String prevEndTime = "";
      boolean firstIteration = true;
      Collection endTimesForUnit = new LinkedList();

      // aggregate quantities over all units
      int totalNumAssets = 0;
      Map unitToNumAssets = new HashMap();
      Map unitToMean = new HashMap();
      Map unitToStdDev = new HashMap();
      
      while(rs.next()){
	String owner = rs.getString(1);
	String assetid = rs.getString(2);
	String endTime = rs.getString(3);
	 
	// Don't do anything for the first row
	if (!firstIteration) {

	  // Beginning legs for a new asset.  Hence the previous leg processed was the
	  // last leg of the previous asset.
	  if (!assetid.equals(prevAssetid))
	      endTimesForUnit.add(prevEndTime);

	  // Insert an entry if we're starting to look at another unit.  
	  if (!owner.equals(prevOwner)) {
	    
//       System.out.println("ArrivalTimePrecisionTest.insertRow() - unit = " + prevOwner + 
//  			 " total = " + assetsForUnitCount + 
//  			 " diff = " + assetsNonBestArrivalCount + 
//  			 " endTimesForUnit=" + endTimesForUnit);

	    String mean = computeArrivalMean(endTimesForUnit);
	    int stddev = computeArrivalStdDev(mean, endTimesForUnit);

	    totalNumAssets += endTimesForUnit.size();
	    unitToNumAssets.put(prevOwner, new Integer(endTimesForUnit.size()));
	    unitToMean.put(prevOwner, mean);
	    unitToStdDev.put(prevOwner, new Integer(stddev));

//  	    insertRow(l, s, run, 
//  		      prevOwner,
//  		      endTimesForUnit.size(),
//  		      mean,
//  		      stddev);
	    endTimesForUnit.clear();
	  }
	}
	prevOwner = owner;
	prevAssetid = assetid;
	prevEndTime = endTime;
	firstIteration = false;
      }

      // Enter last value
      endTimesForUnit.add(prevEndTime);
      String mean = computeArrivalMean(endTimesForUnit);
      int stddev = computeArrivalStdDev(mean, endTimesForUnit);

      totalNumAssets += endTimesForUnit.size();
      unitToNumAssets.put(prevOwner, new Integer(endTimesForUnit.size()));
      unitToMean.put(prevOwner, mean);
      unitToStdDev.put(prevOwner, new Integer(stddev));

//        insertRow(l, s, run, 
//  		prevOwner,
//  		endTimesForUnit.size(),
//  		mean,
//  		stddev);

      // Enter the aggregate values
      insertRow(l, s, run,
		"ALL UNITS",
		totalNumAssets,
		computeAggregateMean(unitToNumAssets, unitToMean),
		computeAggregateStdDev(unitToNumAssets, unitToStdDev));

    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimePrecisionTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  // Computes the average arrival date.
  protected String computeArrivalMean(Collection endTimesForUnit) {
    String retval = null;

    try {
      long sum = 0;
      for (Iterator iter = endTimesForUnit.iterator(); iter.hasNext();)
	sum += format.parse((String) iter.next()).getTime();

      retval = format.format(new Date((long)(sum / endTimesForUnit.size())));

	  
    } catch (ParseException e) {
      System.out.println ("ArrivalTimePrecisionTest.computeArrivalMean() - could not parse date format.");
    }

    return retval;
  }

  // Computes the standard deviation of arrival dates in hours
  protected int computeArrivalStdDev(String mean, Collection endTimesForUnit) {
    long squaresum = 0;

    if (endTimesForUnit.size() < 2)
      return 0;

    try {
      long meanL = format.parse(mean).getTime() / MILLIS_PER_HOUR;
      
      for (Iterator iter = endTimesForUnit.iterator(); iter.hasNext();) {
	long actualL = format.parse((String) iter.next()).getTime() / MILLIS_PER_HOUR;
	squaresum += (meanL - actualL) * (meanL - actualL);
      }
    } catch (ParseException e) {
      System.out.println ("ArrivalTimePrecisionTest.computeSumSquareArrivalTimeDiff() - could not parse date format.");
    }

    return ((int)Math.sqrt(squaresum /(endTimesForUnit.size() - 1)));
  }

  // Computes the aggregate arrival mean over all units
  protected String computeAggregateMean(Map unitToNumAssets, Map unitToMean) {
    String retval = null;
    long sum = 0;
    long totalAssets = 0;

    try {
      for (Iterator iter = unitToMean.keySet().iterator(); iter.hasNext();) {
	String unit = (String) iter.next();
	int unitNumAssets = ((Integer) unitToNumAssets.get(unit)).intValue();
	String unitArrivalMean = (String) unitToMean.get(unit);
	long meanL = format.parse(unitArrivalMean).getTime();
	sum += unitNumAssets * meanL;
	totalAssets += unitNumAssets;
      }

      retval = format.format(new Date((long)(sum / totalAssets)));
    } catch (ParseException e) {
      System.out.println ("ArrivalTimePrecisionTest.computeAggregateMean() - could not parse date format.");
    }

    return retval;
  }

  // Computes the aggregate standard deviation over all units
  protected int computeAggregateStdDev(Map unitToNumAssets, Map unitToStdDev) {
    long squaresum = 0;
    long totalAssets = 0;

    for (Iterator iter = unitToStdDev.keySet().iterator(); iter.hasNext();) {
      String unit = (String) iter.next();
      int unitNumAssets = ((Integer) unitToNumAssets.get(unit)).intValue();
      int unitArrivalStdDev = ((Integer) unitToStdDev.get(unit)).intValue();
      
      squaresum += (unitArrivalStdDev) * (unitArrivalStdDev) * (unitNumAssets - 1);
      totalAssets += unitNumAssets;
    }

	// protect against Divide by Zero error
	if ((totalAssets - 1) == 0)
	  return ((int)Math.sqrt(squaresum));
	
    return ((int)Math.sqrt(squaresum /(totalAssets - 1)));
  }

  protected void insertRow(Logger l, Statement s, int run,
			   String owner, 
			   int number,
			   String mean,
			   int stddev) throws SQLException {
    String sql = null;
    
//      System.out.println("ArrivalTimePrecisionTest.insertRow() - inserting owner = " + owner + 
//  		       " total = " + count + 
//  		       " diffcount = " + diffcount + 
//  		       " avgDiff = " + avgArrivalTimeDiff);

    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
      sb.append(COL_NUMBER);sb.append(",");
      sb.append(COL_MEAN);sb.append(",");
      sb.append(COL_STDDEV);
      sb.append(") VALUES('");
      sb.append(owner);sb.append("','");
      sb.append(number);sb.append("','");
      sb.append(mean);sb.append("','");
      sb.append(stddev);
      sb.append("')");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimePrecisionTest.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);
    }
  }

  // Uses a t-Test at the 95% confidence interval to determine whether the
  // sample run differs significantly from the benchmark run.  
  public int linesEqual(Logger logger,
			ResultSet rs1, boolean hasLine1, 
			ResultSet rs2, boolean hasLine2,
			int columns) {
    if (!hasLine1 && !hasLine2) return EQUALTO;
    if (!hasLine1 && hasLine2) return GREATERTHAN;
    if (hasLine1 && !hasLine2) return LESSTHAN;

    int retval = columnCompare(logger, rs1, rs2, 1);
    if (retval != EQUALTO) return retval;

    try {
    // z-value for 95% confidence interval
    final double zValue = 1.65;

    // Get mean times in hours
    long benchmarkMean = format.parse(rs1.getString(3)).getTime() / MILLIS_PER_HOUR;
    long sampleMean = format.parse(rs2.getString(3)).getTime() / MILLIS_PER_HOUR;

    // Get the sample deviation
    int benchmarkDeviation = rs1.getInt(4);
    int sampleSize = rs2.getInt(2);
    double samplingDeviation = benchmarkDeviation / Math.sqrt(sampleSize);

    if ((sampleMean > (benchmarkMean + zValue * samplingDeviation)) ||
	(sampleMean < (benchmarkMean - zValue * samplingDeviation)))
      retval = CONFLICT;
    else
      retval = EQUALTO;
    } catch (SQLException sqle) {
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimePrecisionTest.linesEqual() - Problem walking results.",sqle);
    } catch (ParseException pe) {
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimePrecisionTest.linesEqual() - Could not parse date format.",pe);
    }

    return retval;
  }

  //InnerClasses:
  ///////////////
}





