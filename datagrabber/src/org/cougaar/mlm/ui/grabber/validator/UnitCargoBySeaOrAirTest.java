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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cargo, broken down by asset class
 *
 * @since 2/26/01
 **/
public class UnitCargoBySeaOrAirTest extends UnitCargoByClassTest{
  
  //Constants:
  ////////////

  protected static String COL_SEA_OR_AIR = "sea_or_air";
  
  //Variables:
  ////////////

  protected boolean debug = 
	"true".equals (System.getProperty("org.cougaar.mlm.ui.grabber.validator.UnitCargoBySeaOrAirTest.debug", "false"));
  
  //Constructors:
  ///////////////

  public UnitCargoBySeaOrAirTest(DBConfig dbConfig, boolean groupByUnit){
    super(dbConfig, groupByUnit, false);
  }

  //Members:
  //////////

  /**for gui**/
  public String getDescription(){
    return "Amount moved by sea or air " + getSuffix ().replace('_',' ');
  }

  /**Base name**/
  protected String getRawTableName(){
    return "Unit"+getSuffix()+"BySeaOrAirAmount";
  }

  /**Get header strings for the table**/
  public String[] getHeaders(){
    String[] headers={"Unit","Class","Assets Moved", "Sea or Air"};
    return headers;
  }

  /**Get the types of the columns of the table**/
  public int[] getTypes(){
    int[] types={TYPE_STRING,TYPE_STRING,TYPE_INT,TYPE_STRING};
    return types;
  }

  /** methods for interface Graphable */
  public int getXAxisColumn () { return 1; }
  public int getYAxisColumn () { return 3; }
  public int getZAxisColumn () { return 4; }
  public boolean hasThirdDimension () { return true; }

  protected void createTable(Statement s, int run) throws SQLException{
    s.executeUpdate("CREATE TABLE "+getTableName(run)+" ( "+
					DGPSPConstants.COL_OWNER+" VARCHAR(255) NOT NULL,"+
					DGPSPConstants.COL_ASSET_CLASS+" VARCHAR(255) NOT NULL,"+
					COL_NUMASSETS+" INTEGER NOT NULL,"+
					COL_SEA_OR_AIR+" VARCHAR(255) NOT NULL"+
					")");
  }

  /**
   * Use this when inserting into tables based on 
   * a select from other tables
   **/
  protected void selectIntoTable(Logger l, Statement s, String destTable, String destColumns, String selectSql){
    try{
	  dbConfig.createTableSelect(s,destTable,selectSql);
    }catch(SQLException e){
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
				   "UnitCargoBySeaOrAirTest.insertResults - Problem executing query : " + selectSql + "\n" +
				   "Could not select into table("+destTable+")\n", e);
      return;
    }
  }

  protected void insertResults (Logger l, Statement s, int run) {
    ResultSet rs=null;
    String sql = null;
	String createTempSql = null;
    
	  if (tempTableAvailable(l,s,run)) {
		l.logMessage(Logger.MINOR, Logger.DB_WRITE,
						  "Someone else is doing this test, aborting.");
		return; // error - someone else is running this test right now!
	  }
	  
	  l.logMessage(Logger.MINOR, Logger.DB_WRITE,
				   "Creating temp table " + getTempTableName(run));
	  createTempSql = getCreateTempSql (run);
	  
	  selectIntoTable (l,s,getTempTableName(run),
					   DGPSPConstants.COL_ASSETID + "," + DGPSPConstants.COL_CONVEYANCE_TYPE,
					   createTempSql);

	  l.logMessage(Logger.MINOR, Logger.DB_WRITE,
				   "Sql for temp table:\n" + createTempSql);
	
	try{
      sql = getQuery(run);

	  l.logMessage(Logger.MINOR, Logger.DB_WRITE, "Sql for result:\n" + sql);

      rs=s.executeQuery(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoBySeaOrAirTest.insertResults - Problem executing query : " + sql,
		   sqle);
    }
    try {
      while(rs.next()){
		String owner = rs.getString(1);
		if (!groupByUnit)
		  owner = "All Units";
	
		int assetclass = rs.getInt(2);
		int count = rs.getInt(3);
		int conveyancetype = rs.getInt(4);
		insertRow(l,s,run,owner,getRomanClass(assetclass),count,getSeaOrAir(conveyancetype));
      }    
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoBySeaOrAirTest.insertResults - Problem walking results.",sqle);
    }
  }
  
  private String getCreateTempSql (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String itinTable = Controller.getTableName(DGPSPConstants.ASSET_ITINERARY_TABLE,run);
    String legTable = Controller.getTableName(DGPSPConstants.CONVEYED_LEG_TABLE,run);
    String convInstTable = Controller.getTableName(DGPSPConstants.CONV_INSTANCE_TABLE,run);
    String convProtoTable = Controller.getTableName(DGPSPConstants.CONV_PROTOTYPE_TABLE,run);
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String instID = assetTable+"."+DGPSPConstants.COL_ASSETID;
    String itinID = itinTable+"."+DGPSPConstants.COL_ASSETID;
    String itinLegID = itinTable+"."+DGPSPConstants.COL_LEGID;
    String legLegID = legTable+"."+DGPSPConstants.COL_LEGID;
    String legConvID = legTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String convConvID = convInstTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String convInstProtoID = convInstTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String convProtoProtoID = convProtoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String conveyanceType = convProtoTable+"."+DGPSPConstants.COL_CONVEYANCE_TYPE;

    String sqlQuery =
      "select distinct "+instID+"," + conveyanceType + "\n"+
      "from "+assetTable+", "+protoTable+","+itinTable+", " + legTable + ", " + convInstTable+ ", " + convProtoTable+"\n"+
      "where "+instProtoid+" = "+protoProtoid+"\n"+
	  "and " +instID +" = "+itinID +"\n"+
	  "and " +itinLegID +" = "+legLegID +"\n"+
	  "and " +legConvID +" = "+convConvID +"\n"+
	  "and " +convInstProtoID +" = "+convProtoProtoID +"\n"+
	  "and (" + conveyanceType + " = " + DGPSPConstants.CONV_TYPE_PLANE + " or " +
	  "" + conveyanceType + " = " + DGPSPConstants.CONV_TYPE_SHIP + ")\n";
	
    return sqlQuery;
  }
  private String getQuery (int run) {
    String assetTable = Controller.getTableName(DGPSPConstants.ASSET_INSTANCE_TABLE,run);
    String protoTable = Controller.getTableName(DGPSPConstants.ASSET_PROTOTYPE_TABLE,run);
    String owner = assetTable + "."+ DGPSPConstants.COL_OWNER;
    String aggnum = DGPSPConstants.COL_AGGREGATE;
    String instProtoid = assetTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String instID = assetTable+"."+DGPSPConstants.COL_ASSETID;
    String protoProtoid = protoTable+"."+DGPSPConstants.COL_PROTOTYPEID;
	//    String conveyanceType = convProtoTable+"."+DGPSPConstants.COL_CONVEYANCE_TYPE;
    String conveyanceType = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String assetClass = DGPSPConstants.COL_ASSET_CLASS;

	String tempTable = getTempTableName (run);
	String tempID    = tempTable+"." +DGPSPConstants.COL_ASSETID;
	
    String sqlQuery =
      "select "+owner+", " + assetClass + ", " + "sum("+aggnum+")"+ "," + conveyanceType + "\n"+
      "from "+assetTable+", "+tempTable +"," + protoTable+"\n"+
	  "where " +
	  instID +" = "+tempID +"\n"+
	  "and "+instProtoid+" = "+protoProtoid+"\n"+
      "group by "+(groupByUnit ? (owner + ", ") : "") +assetClass + "," + conveyanceType;

    return sqlQuery;
  }
  
  private void insertRow(Logger l, Statement s, int run,
			 String owner, String assetClass, int count, String seaOrAir) throws SQLException {
    String sql = null;
    try {
      StringBuffer sb=new StringBuffer();
      sb.append("INSERT INTO ");
      sb.append(getTableName(run));
      sb.append(" (");
      sb.append(DGPSPConstants.COL_OWNER);sb.append(",");
	  sb.append(DGPSPConstants.COL_ASSET_CLASS);sb.append(",");
      sb.append(COL_NUMASSETS);sb.append(",");
      sb.append(COL_SEA_OR_AIR);
      sb.append(") VALUES('");
      sb.append(owner);sb.append("',");
	  sb.append("'");sb.append(assetClass);sb.append("',");
      sb.append(count);sb.append(",'");
      sb.append(seaOrAir);sb.append("'");
      sb.append(")");
      sql = sb.toString();
      s.executeUpdate(sql);
    } catch (SQLException sqle) {
      l.logMessage(Logger.ERROR,Logger.DB_WRITE,
		   "UnitCargoBySeaOrAirTest.insertRow - Problem inserting rows in table. Sql was\n" + sql,
		   sqle);
    }
  }

  protected String getSuffix () { return ((groupByUnit) ? "by_unit" : ""); }

  protected String getSeaOrAir (int i) 
  {
	switch (i) {
	case DGPSPConstants.CONV_TYPE_PLANE:
	  return "air";
	case DGPSPConstants.CONV_TYPE_SHIP:
	  return "sea";
	default:
	  return "unknown";
	}
  }
    
  public String getTempTableName(int run){
    return dbConfig.getDBTableName(Controller.getTableName
								   ("temp_test"+getRawTableName(),run));
  }

  public boolean tempTableAvailable(Logger logger, Statement s, int run){
    try{
      DatabaseMetaData meta = s.getConnection().getMetaData();
      String tTypes[]={"TABLE"}; 
      ResultSet rs;
      rs = meta.getTables(null,null,getTempTableName(run),tTypes);
      boolean ret=rs.next();
      rs.close();
      return ret;
    }catch(SQLException e){
      logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
			"Could not determine if validation table available",e);
    }
    return false;
  }

  protected void dropTable (Logger logger, Statement s, int run) {
	super.dropTable(logger,s,run);
	
    if(tempTableAvailable(logger,s,run)){
      try{
		logger.logMessage(Logger.MINOR, Logger.DB_WRITE,
						  "Dropping table " + getTempTableName(run));
		s.executeQuery("drop table "+getTempTableName(run));
      }catch(SQLException e){
		logger.logMessage(Logger.ERROR,Logger.DB_WRITE,
						  "Could not drop table "+getTableName(run),
						  e);
      }
    }
  }
  //InnerClasses:
  ///////////////
}





