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
import java.util.Map;
import java.util.HashMap;

import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Looks at all the transported assets and tests for deviation from preferred arrival time.  
 *
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
				 int current,
				 int total, 
				 Collection endTimesForUnit,
				 Map unitToNumAssets,
				 Map unitToMean,
				 Map unitToStdDev) throws SQLException {
    insertRow(l, s, run,
	      owner,
	      current,
	      (String) unitToMean.get(owner),
	      ((Integer)unitToStdDev.get(owner)).intValue());
  }
}
