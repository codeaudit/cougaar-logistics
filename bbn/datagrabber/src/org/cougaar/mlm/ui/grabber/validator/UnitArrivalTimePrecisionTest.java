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
public class UnitArrivalTimePrecisionTest extends ArrivalTimePrecisionTest{
  

  //Constructors:
  ///////////////

  public UnitArrivalTimePrecisionTest(DBConfig dbConfig){
    super(dbConfig);
  }


  /**for gui**/
  public String getDescription(){
    return "Arrival time precision by Unit";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "unitarrivalprecision";
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

	    insertRow(l, s, run, 
		      prevOwner,
		      endTimesForUnit.size(),
		      mean,
		      stddev);
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

      insertRow(l, s, run, 
		prevOwner,
		endTimesForUnit.size(),
		mean,
		stddev);

    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ArrivalTimePrecisionTest.insertResults - Problem walking results.",sqle);
    }
  }
}
