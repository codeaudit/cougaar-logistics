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
 * Shows dimension of cargo types
 *
 * @since 2/26/01
 **/
public class CargoSizeTest extends CargoDimensionTest {
  
  //Constants:
  ////////////

  public static final String COL_DENSITY = "density";
  public static final double MILVAN_VOLUME = 36.095; // m3  
  public static final double MILVAN_HEIGHT = 2.4384; // m
  public static final double MILVAN_WIDTH  = 2.4384; // m  
  public static final double MILVAN_DEPTH  = 6.0706; // m  
  public static final double MILVAN_AREA   = MILVAN_WIDTH*MILVAN_DEPTH; // m^2  
  public static final double MILVAN_WEIGHT = 22.513; // short tons
  
  public static final double C5_VOLUME = 1042.3; // m3  
  public static final double C5_HEIGHT = 4.11; // m  
  public static final double C5_WIDTH  = 5.79;  // m  
  public static final double C5_DEPTH  = 43.8;  // m  
  public static final double C5_AREA   = C5_DEPTH*C5_WIDTH;  // m^2  
  public static final double C5_WEIGHT = 122472.0;  // kg  

  public static final boolean BIGGER_THAN_MILVAN = false;
  public static final boolean BIGGER_THAN_C5     = true;

  //Variables:
  ////////////
  public boolean debug = "true".equals (System.getProperty("CargoSizeTest.debug", "false"));
  protected boolean biggerThanAC5;
  
  //Constructors:
  ///////////////

  public CargoSizeTest(DBConfig dbConfig, boolean biggerThanAC5){
    super(dbConfig);
    this.biggerThanAC5 = biggerThanAC5;
  }

  //Members:
  //////////

  public int failureLevel(){
    //    return (biggerThanAC5) ? RESULT_WARNING : RESULT_INFO;
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Cargo Size Test (cargo bigger than a " + (biggerThanAC5 ? "C5's capacity" : "milvan") + ")";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "CargoSize" + (biggerThanAC5 ? "_C5" : "_Milvan");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;
    
    try {
      sql = getQuery(run);
      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "CargoProfileTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      while(rs.next()){
	String nomenclature = rs.getString(1);
	double height = rs.getDouble (2);
	double width = rs.getDouble (3);
	double depth = rs.getDouble (4);
	double area = rs.getDouble (5);
	double volume = rs.getDouble (6);
	double weight = rs.getDouble (7);
	double kilograms  = weight/1000.0;
	double metricTons = kilograms/1000.0;
	double shortTons = (metricTons)*METRIC_TO_SHORT_TON;

	if (l.isMinorEnabled())
	  l.logMessage(Logger.MINOR,Logger.DB_WRITE,"CargoSizeTest - h " + height + 
		       " w " + width +
		       " d " + depth +
		       " a " + area +
		       " v " + volume +
		       " weight " + shortTons + " stons");

	if (biggerThanAC5) {
	  if (height > C5_HEIGHT ||
	      width  > C5_WIDTH  ||
	      depth  > C5_DEPTH  ||
	      area   > C5_AREA  ||
	      volume > C5_VOLUME  ||
	      kilograms > C5_WEIGHT)
	    insertRow(l,s,run,nomenclature,height,width,depth,area,volume,shortTons);
	}
	else {
	  if (height > MILVAN_HEIGHT ||
	      width  > MILVAN_WIDTH  ||
	      depth  > MILVAN_DEPTH  ||
	      area   > MILVAN_AREA  ||
	      volume > MILVAN_VOLUME  ||
	      shortTons > MILVAN_WEIGHT)
	    insertRow(l,s,run,nomenclature,height,width,depth,area,volume,shortTons);
	}
      }    

      if (biggerThanAC5) {
	insertRow(l,s,run,"<b>",0,0,0,0,0,0);
	insertRow(l,s,run,"<b>Compared with : </b>",0,0,0,0,0,0);
	insertRow(l,s,run,"<b>C5</b>",C5_HEIGHT,C5_WIDTH,C5_DEPTH,C5_AREA,C5_VOLUME,(C5_WEIGHT/1000)*METRIC_TO_SHORT_TON);
      }
      else {
	insertRow(l,s,run,"<b>",0,0,0,0,0,0);
	insertRow(l,s,run,"<b>Compared with : </b>",0,0,0,0,0,0);
	insertRow(l,s,run,"<b>MILVAN</b>",MILVAN_HEIGHT,MILVAN_WIDTH,MILVAN_DEPTH,MILVAN_AREA,MILVAN_VOLUME,MILVAN_WEIGHT);
      }
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "CargoProfileTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  //InnerClasses:
  ///////////////
}
