/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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

import org.cougaar.mlm.ui.grabber.config.DBConfig;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.controller.Controller;
import org.cougaar.mlm.ui.grabber.logger.Logger;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Dump the contents of the manifest table.
 *
 *
 * @since 02/24/04
 **/
public class ManifestInfo extends Test {
  public final String COL_RESERVED = "Reserved";

  //Variables:
  ////////////

  public boolean debug = "true".equals (System.getProperty("ManifestInfo.debug", "false"));

  public ManifestInfo(DBConfig dbConfig) {
    super(dbConfig);
  }

  public String getDescription(){
    return "Milvan manifest tonnage grouped by type";
  }

  protected String getRawTableName(){
    return "ManifestInfo";
  }

  public int failureLevel(){
    return RESULT_INFO;
  }

  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  protected void createTable(Statement s, int run) throws SQLException {
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
		    DGPSPConstants.COL_ALP_TYPEID+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_ALP_NOMENCLATURE+" VARCHAR(255) NOT NULL,"+
		    COL_RESERVED+" VARCHAR(255) NOT NULL,"+
		    DGPSPConstants.COL_WEIGHT+" DOUBLE NOT NULL" + 
		    ")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;

    try {
      sql = getQuery(run);
      rs  = s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ManifestInfo.insertResults - Problem executing query : " + sql,
		   sqle);
    }

    try {
      if (rs != null) {
	double actual = 0;
	double reserved = 0;
	while(rs.next()){
	  int wasReserved = rs.getInt (3);
	  double tons = rs.getDouble (4);
	  boolean isReserved = (wasReserved==1);
	  insertRow (l, s, run, rs.getString (1), rs.getString (2), 
		     isReserved ? "reserved" : "actual", 
		     tons);

	  if (isReserved)
	    reserved += tons;
	  else
	    actual += tons;
	}
	insertRow (l, s, run, "All Dodics", "All Kinds - total tons", "reserved", reserved);
	insertRow (l, s, run, "All Dodics", "All Kinds - total tons", "actual", actual);
	insertRow (l, s, run, "All Dodics", "All Kinds - total tons", "both", reserved+actual);
      }
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ManifestInfo.insertResults - Problem walking results.",sqle);
    }
  }

  private void insertRow(Logger l, Statement s, int run, 
			 String type, String nomen, 
			 String reservedLabel, double weight) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_ALP_TYPEID);
      sb.append(",");
      sb.append(DGPSPConstants.COL_ALP_NOMENCLATURE);
      sb.append(",");
      sb.append(COL_RESERVED);
      sb.append(",");
      sb.append(DGPSPConstants.COL_WEIGHT);
      sb.append(")\nVALUES(");

      sb.append("'");
      sb.append(type);
      sb.append("'");

      sb.append(",");

      sb.append("'");
      sb.append(nomen);
      sb.append("'");

      sb.append(",");

      sb.append("'");
      sb.append(reservedLabel);
      sb.append("'");

      sb.append(",");

      // weight is in grams, we want short tons = 2000 pounds
      sb.append(dbConfig.getDBDouble((weight/1000000.0d)*CargoDimensionTest.METRIC_TO_SHORT_TON)); 

      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "ManifestInfo.insertRow - Problem inserting rows in table. Sql was " + sql,
		   sqle);

    }
  }

  /** Get header strings for the table **/
  public String[] getHeaders(){
    String[] headers={"Ammo type", "nomenclature", "Reserved(Projection)", "weight (short tons)"};
    return headers;
  }

  /** Get the types of the columns of the table **/
  public int[] getTypes(){
    int[] types={TYPE_STRING, TYPE_STRING, TYPE_STRING, TYPE_TONNAGE_THREE_DIGITS};
    return types;
  }

  /**
   * Returns four columns : type, nomen, 1 if "Reserved", and sum of weight
   * grouped by type and reserved or not.
   * @return sql query
   */
  protected String getQuery (int run) {
    String manifestTable = Controller.getTableName(DGPSPConstants.MANIFEST_TABLE,run);

    String sqlQuery =
      "select "+
      DGPSPConstants.COL_ALP_TYPEID + ", " + 
      DGPSPConstants.COL_ALP_NOMENCLATURE + ", " + 
      "instr(name, 'Reserved')<>0 as reserved, \n"+ 
      "sum("+DGPSPConstants.COL_WEIGHT+")\n"+
      "from "+manifestTable + "\n" +
      "group by " + DGPSPConstants.COL_ALP_TYPEID + "," +
      "reserved";
	
    if (debug)
      System.out.println ("ManifestInfo.getQuery - sql was:\n" + sqlQuery);
	
    return sqlQuery;
  }
}
