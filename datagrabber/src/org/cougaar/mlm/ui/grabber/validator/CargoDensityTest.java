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
public class CargoDensityTest extends CargoDimensionTest {
  
  //Constants:
  ////////////

  public static final String COL_DENSITY = "density";
  public static final double DENSITY_OF_LEAD = 11340.0d; // kg/m3  
  
  //Variables:
  ////////////
  public boolean debug = "true".equals (System.getProperty("CargoDensityTest.debug", "false"));

  //Constructors:
  ///////////////

  public CargoDensityTest(DBConfig dbConfig){
    super(dbConfig);
  }

  //Members:
  //////////

  public int failureLevel(){
    return RESULT_WARNING;
  }

  /**for gui**/
  public String getDescription(){
    return "Cargo Density Test (cargo denser than lead)";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "CargoDensity";
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
	// weight is in grams, we want short tons = 2000 pounds
	double area = rs.getDouble (5);
	double volume   = rs.getDouble (6);
	double weight = rs.getDouble (7);
	double kilograms  = weight/1000.0;
	double metricTons = kilograms/1000.0;
	double shortTons = (metricTons)*METRIC_TO_SHORT_TON;

	if (kilograms/volume > DENSITY_OF_LEAD)
	  insertRow(l,s,run,nomenclature,height,width,depth,area,volume,shortTons);
      }    
      insertRow(l,s,run,"<b></b>",0,0,0,0,0,0);
      insertRow(l,s,run,"<b>Compared with : </b>",0,0,0,0,0,0);
      insertRow(l,s,run,"<b>LEAD</b>",0,0,0,0,1,(DENSITY_OF_LEAD/1000)*METRIC_TO_SHORT_TON);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "CargoProfileTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  /** 
   * Compare a column from two different result sets 
   * @return EQUALTO, GREATERTHAN, or LESSTHAN
   */
  protected int columnCompare(Logger logger, ResultSet rs1, ResultSet rs2, 
			      int column) {

    if (logger.isMinorEnabled())
      logger.logMessage(Logger.MINOR,Logger.DB_WRITE, "comparing column " + column);
      
    try {
      if (column == 1) {
	String columnOneString = rs1.getString(1);
	if (logger.isMinorEnabled())
	  logger.logMessage(Logger.MINOR,Logger.DB_WRITE, "Column 1 string is " + columnOneString);
	if (columnOneString.startsWith ("<b>")) 
	  return EQUALTO;
      }
    } catch (Exception sqle) {
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Problem comparing columns.",sqle);
    }

    return super.columnCompare (logger, rs1, rs2, column);
  }

  //InnerClasses:
  ///////////////
}
