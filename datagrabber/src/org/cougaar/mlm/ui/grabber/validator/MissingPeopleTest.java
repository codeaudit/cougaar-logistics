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
 * Looks for missing people
 *
 * @since 2/26/01
 **/
public class MissingPeopleTest extends Test{

  //Constants:
  ////////////

  //Variables:
  ////////////

  //Constructors:
  ///////////////

  public MissingPeopleTest(DBConfig dbConfig){
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
    return "Units without people.";
  }

  /**Base name**/
  protected String getRawTableName(){
    return "missingpeople";
  }

  String COL_HASPEOPLE = "haspeople";
  
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String aPrototype = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String pPrototype = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String ownerid = DGPSPConstants.COL_OWNER;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;

    String sqlQuery =
      "select "+"distinct "+ownerid+", "+assetClass+"\n"+
      "from "+assetTable+", "+protoTable+"\n"+
      "where "+aPrototype+" = "+pPrototype+"\n"+
      "order by "+ownerid;

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
					DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
					COL_HASPEOPLE+" VARCHAR(255) NOT NULL"+
					")");
  }

  protected void insertResults (Logger l, Statement s, int run) {
	String currentOrg, lastOrg = null;
	boolean hasPeople = false;
    ResultSet rs=null;
	String sql = null;

	try {
	  sql = getQuery(run);
	  rs=s.executeQuery(sql);
	} catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
				   "MissingPeopleTest.insertResults - Problem executing query : " + sql,
				   sqle);
	}

	try {
	  while(rs.next()){
		currentOrg = rs.getString (1);
		int assetProto = rs.getInt(2);

		if (lastOrg == null)
		  lastOrg = currentOrg;

		if (!currentOrg.equals (lastOrg)) {
		  l.logMessage(Logger.MINOR,Logger.DB_WRITE,
					   "MissingPeopleTest.insertResults - " + lastOrg + ((hasPeople) ? " yes " : " NO "));
		  if (!hasPeople)
			insertRow (l, s, run, lastOrg, hasPeople);
		  hasPeople = false;
		}
		hasPeople = hasPeople || assetProto == DGPSPConstants.ASSET_CLASS_PERSON;
		lastOrg = currentOrg;
	  }
	} catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"MissingPeopleTest.insertResults - Problem walking results.",sqle);
	}
  }

	private void insertRow(Logger l, Statement s, int run, String ownerID, boolean hasPeople) throws SQLException {
	  String sql = null;
	  try {
		StringBuffer sb=new StringBuffer();
		sb.append("INSERT INTO ");
		sb.append(getTableName(run));
		sb.append(" (");
		sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
		sb.append(COL_HASPEOPLE);
		sb.append(") VALUES('");
		sb.append(ownerID);sb.append("',");
		sb.append(((hasPeople) ? "'Yes'" : "'No'"));
		sb.append(")");
		sql = sb.toString();
		s.executeUpdate(sql);
	  } catch (SQLException sqle) {
		l.logMessage(Logger.ERROR,Logger.DB_WRITE,
					 "MissingPeopleTest.insertRow - Problem inserting rows in table. Sql was " + sql,
					 sqle);
	  }
	}
  
  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Owner Unit ID","has people"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_STRING};
    return types;
  }

  //Likely to be over-ridden:
  //=========================

  /**Render enums**/
  /*
  protected String renderEnum(int whichEnum, int enum){
    switch(whichEnum){
    case TYPE_ENUM_1:
      return enum==DGPSPConstants.ASSET_CLASS_UNKNOWN?"Unknown":
	Integer.toString(enum);
    }
    return Integer.toString(enum);
  }
  */

  //InnerClasses:
  ///////////////
}


