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

  protected void insertUnitRow (Logger l, Statement s, int run,
				String owner, 
				int number, 
				String mean, int stddev) throws SQLException {
    insertRow(l, s, run, 
	      owner,
	      number,
	      mean,
	      stddev);
  }

  protected void insertFinalRow (Logger l, Statement s, int run,
				 String owner, 
				 int number, 
				 Collection endTimesForUnit,
				 Map unitToNumAssets,
				 Map unitToMean,
				 Map unitToStdDev) throws SQLException {
    insertRow(l, s, run,
	      owner,
	      endTimesForUnit.size(),
	      (String) unitToMean.get(owner),
	      ((Integer)unitToStdDev.get(owner)).intValue());
  }
}
