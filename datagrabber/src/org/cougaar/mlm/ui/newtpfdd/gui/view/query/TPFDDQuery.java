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
package org.cougaar.mlm.ui.newtpfdd.gui.view.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;
import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.ByCargo;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoType;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoInstance;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.ByCarrier;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CarrierType;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CarrierInstance;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LegNode;

public class TPFDDQuery extends UnitQuery {
  boolean debug = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.debug", 
				       "false"));
  boolean showSqlTime = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.showSqlTime", 
				       "false"));
  
  public TPFDDQuery (DatabaseRun run, FilterClauses filterClauses) {
    super (run, filterClauses);
  }
  
  public QueryResponse getResponse (Connection connection) {
    QueryResponse response = new QueryResponse ();
	
    // first figure out which run to use
    int recentRun = run.getRunID ();

    Tree cargoTree = new Tree ();
    UIDGenerator generator = cargoTree.getGenerator ();
    String unitID = "";
    Iterator iter=filterClauses.getUnitDBUIDs().iterator();
    if(iter.hasNext())
      unitID=(String)iter.next();

    Node cargoRoot = new CargoType (generator, unitID); // could be anything...?
    cargoTree.setRoot (cargoRoot);

    Map instanceToNode = new HashMap ();

    long totalTime=System.currentTimeMillis();

    attachInstances (connection, filterClauses, recentRun, cargoTree, instanceToNode);
    attachLegs      (connection, filterClauses, recentRun, cargoTree, instanceToNode);
	
    if (showSqlTime)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.getResponse total millis:" + (System.currentTimeMillis()-totalTime));

    response.addTree (cargoTree);

    if (debug) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.getResponse - cargo tree :");
      cargoTree.show ();
    }
	
    return response;
  }

  /** overriden in FilterQuery ! 
   * 
   * @see org.cougaar.mlm.ui.newtpfdd.gui.view.query.FilterQuery#attachInstances
   */
  protected void attachInstances (Connection connection, FilterClauses filterClauses, int recentRun, Tree cargoTree,
				  Map instanceToNode) {
    ResultSet rs;
    long time;
    time=System.currentTimeMillis();

    if(run.hasCargoInstanceTable()){
      rs = getResultSet(connection, formFastCargoInstanceSql(filterClauses, recentRun));
      attachInstancesFromResult (rs, cargoTree.getGenerator(), cargoTree, instanceToNode);
    }else{
      rs = getResultSet(connection, formFirstCargoInstanceSql (filterClauses, recentRun));
      attachInstancesFromResult (rs, cargoTree.getGenerator(), cargoTree, instanceToNode);
      rs = getResultSet(connection, formSecondCargoInstanceSql (filterClauses, recentRun));
      attachInstancesFromResult (rs, cargoTree.getGenerator(), cargoTree, instanceToNode);
    }

    if(showSqlTime){
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (run.hasCargoInstanceTable()?"Fast ":"")+"TPFDDQuery.CargoInstance query took: "+
			 (System.currentTimeMillis()-time));
    }
  }
  
  protected void attachLegs (Connection connection, FilterClauses filterClauses, int recentRun, Tree cargoTree,
			     Map instanceToNode) {
    ResultSet rs;
    long time;
    time=System.currentTimeMillis();
    if(run.hasCargoLegTable()){
      rs = getResultSet(connection, formFastCargoLegSql (filterClauses, recentRun));
    }else{
      rs = getResultSet(connection, formCargoLegSql (filterClauses, recentRun));
    }
    attachLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);

    if(showSqlTime){
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (run.hasCargoLegTable()?"Fast ":"")+"TPFDDQuery.CargoLeg query took: "+
			 (System.currentTimeMillis()-time));
    }
  }
  
  protected void attachInstancesFromResult (ResultSet rs, UIDGenerator generator, Tree cargoTree, 
					    Map instanceToNode) {
    int rows = 0;
    try{
      while(rs.next()){
	rows++;
	String id        = rs.getString (1);
	String proto     = rs.getString (2);
	String name      = rs.getString (3);
	String aggnumber = rs.getString (4);
	String unitName  = rs.getString (5);
	String protoNomen = rs.getString (6);
	double weight    = rs.getDouble (7);
	double width     = rs.getDouble (8);
	double height    = rs.getDouble (9);
	double depth     = rs.getDouble (10);
	double area      = rs.getDouble (11);
	double volume    = rs.getDouble (12);
	if (instanceToNode.get (id) != null) // protect against sql bug (seems to be sql bug)
	  continue;

	Node instanceNode = createCargoInstance (generator, id, name, aggnumber, unitName, protoNomen,
						 weight, width, height, depth, area, volume);
	cargoTree.addNode (cargoTree.getRoot(), instanceNode);
	instanceToNode.put (id, instanceNode);
      }
      if (debug) 
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.attachInstancesFromResult - total rows for instances " + rows);
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.attachInstancesFromResult - SQLError : " + e);
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.attachInstancesFromResult - " +
			      "closing result set, got sql error : " + e); 
	}
      }
    }
  }
}
