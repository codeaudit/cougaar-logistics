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

/**
 * Calculates total # people or cargo moved
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/26/01
 **/
public class TotalByClassTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////
  boolean people;
  String COL_TOTAL;

  protected boolean debug = 
	"true".equals (System.getProperty("org.cougaar.mlm.ui.grabber.validator.TotalByClassTest.debug", "false"));
  
  //Constructors:
  ///////////////

  public TotalByClassTest(DBConfig dbConfig, boolean people){
    super(dbConfig);
    this.people = people;
	COL_TOTAL = "total" + getSuffix();
	
  }

  //Members:
  //////////

  /**are we a warning or an error if we fail**/
  public int failureLevel(){
    return RESULT_INFO;
  }

  /**for gui**/
  public String getDescription(){
    return "Total " + getSuffix();
  }

  /**Base name**/
  protected String getRawTableName(){
    return "total" + getSuffix();
  }
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String aggnum = DGPSPConstants.COL_AGGREGATE;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;
    String personConstant = (new Integer(DGPSPConstants.ASSET_CLASS_PERSON)).toString();

    String sqlQuery =
      "select sum("+aggnum+")\n"+
      "from "+assetTable+", "+protoTable+"\n"+
      "where "+assetClass+(people?" = ":" <> ")+personConstant+"\n"+
      "and "+instProtoid+" = "+protoProtoid;

    if (debug)
	  System.out.println("\n"+sqlQuery);

    return sqlQuery;
  }
  
  /**Actually do the query and build the table**/
  protected void constructTable(Logger l, Statement s, int run)
    throws SQLException{
    createTable(s, run);
    insertResults(l,s,run);
  }

  private void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
					COL_TOTAL+" INTEGER NOT NULL"+
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
				   "TotalByClassTest.insertResults - Problem executing query : " + sql,
				   sqle);
	}

	try {
	  if (rs != null) {
		while(rs.next()){
		  insertRow (l, s, run, rs.getInt (1));
		}
	  }
	} catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"TotalByClassTest.insertResults - Problem walking results.",sqle);
	}
  }

	private void insertRow(Logger l, Statement s, int run, int totalByClass) throws SQLException {
	  String sql = null;
	  try {
		StringBuffer sb=new StringBuffer();
		sb.append("INSERT INTO ");
		sb.append(getTableName(run));
		sb.append(" (");
		sb.append(COL_TOTAL);
		sb.append(") VALUES(");
		sb.append(totalByClass);
		sb.append(")");
		sql = sb.toString();
		s.executeUpdate(sql);
	  } catch (SQLException sqle) {
		l.logMessage(Logger.ERROR,Logger.DB_WRITE,
					 "TotalByClassTest.insertRow - Problem inserting rows in table. Sql was " + sql,
					 sqle);
	  }
	}
  
  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Total moved by class "};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_INT};
    return types;
  }

  protected String getSuffix () { return ((people) ? "people" : "cargo"); }
}


