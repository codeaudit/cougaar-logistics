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
package org.cougaar.mlm.ui.newtpfdd.gui.view.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Org;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.EquipmentNode;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;
import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;

import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.derived.PrepareDerivedTables;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

public class HierarchyQuery extends SqlQuery {
  int whichSociety = HierarchyConstants.SOC_DEMAND;
  
  boolean debug = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.debug", 
				       "false"));
  boolean showSqlTime = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.showSqlTime", 
				       "false"));

  DatabaseRun run;

  boolean doRollup = true;
  
  /** 
   * by default returns demand hierarchy, 
   * but setting setRoot to TRANSCOM will return TOPS hierarchy 
   */
  public HierarchyQuery (DatabaseRun run) {
	setLeavesOnly(false); // when should this be true?
	this.run = run;
  }

  public HierarchyQuery(DatabaseRun run, boolean doRollup){
    this(run);
    this.doRollup=doRollup;
  }
  
  /** expect to be either HierarchyConstants.SOC_DEMAND or HierarchyConstants.SOC_TOPS */
  public void setSociety (int societyID) { this.whichSociety = societyID;  }

  /** 
   * does a join between org descend table and org table to get only those orgs
   * that descend from root 
   *
   * e.g.
   * mysql> select org_1.org_id, org_1.related_id from org_1, orgdescend_1 where orgd
   * escend_1.org_id = 'HigherAuthority' and orgdescend_1.descendent_id = org_1.org_i
   * d;
   * +-----------------+------------+
   * | org_id          | related_id |
   * +-----------------+------------+
   * | HigherAuthority | XVIIICorps |
   * | HigherAuthority | VIICorps   |
   * | XVIIICorps      | 3BDE       |
   * | 3BDE            | 1BDE       |
   * | 1BDE            | 3-69-ARBN  |
   * | 1BDE            | 3-72-ARBN  |
   * | VIICorps        | 9-FRE      |
   * | 9-FRE           | 10--KJR    |
   * +-----------------+------------+
   *
   * Uses maps to keep track of which nodes have already been created.
   */
  public QueryResponse getResponse (Connection connection) {
	QueryResponse response = new QueryResponse ();
	Tree tree = new Tree ();
	UIDGenerator generator = tree.getGenerator ();
	Map nameToNode = new HashMap ();
	
	// first figure out which run to use
	//	int recentRun = getRecentRun (connection);
	int recentRun = run.getRunID ();

	// get name and pretty name of root
	String [] idAndName = getOrgRoot (connection, recentRun, whichSociety);
	// create root and make it tree's root
	Node rootNode = createNode (generator, idAndName[0], idAndName[1], nameToNode);
	tree.setRoot (rootNode);
	
	long time;

	time=System.currentTimeMillis();
	ResultSet rs = getResultSet(connection, formSql(idAndName[0], recentRun));
	buildTreeFromResult (rs, generator, tree, nameToNode);
	if(showSqlTime){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (false?"Fast ":"")+"Hierarchy query took: "+
			     (System.currentTimeMillis()-time));
	}
	
	if(doRollup){
	  time=System.currentTimeMillis();
	  doRollup (connection, tree, nameToNode, idAndName[0]/* root name e.g. HigherAuthority*/, recentRun);
	  if(showSqlTime){
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (run.hasRollupTable()?"Fast ":"")+"Rollup query took: "+
			       (System.currentTimeMillis()-time));
	  }
	}
	
	response.addTree(tree);
	
	return response;
  }

  protected void buildTreeFromResult (ResultSet rs, UIDGenerator generator, Tree tree, Map nameToNode) {
	try{
	  while(rs.next()){
		String cluster  = rs.getString (HierarchyConstants.COL_RELID);
		String superior = rs.getString (HierarchyConstants.COL_ORGID);
		String prettyName = rs.getString (HierarchyConstants.COL_PRETTY_NAME);
		
		if (debug) 
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - " +
							  cluster + "\tsuperior is " + superior + 
							  " pretty " + prettyName);

		Node clusterNode  = null;
		Node superiorNode = null;
		Node equipmentNode = null;
		if ((clusterNode = (Node) nameToNode.get (cluster)) == null) {
		  clusterNode = createNode (generator, cluster, prettyName, nameToNode);
		  if (debug) 
			TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - created child node " + clusterNode);
		  equipmentNode = createUnmappedNode (generator, cluster, "Equipment");
		}

		superiorNode = (Node) nameToNode.get (superior);
		if (superiorNode == null)
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - huh? no superior for " + superior);
		  
		tree.addNode (superiorNode.getUID(), clusterNode);
		if (equipmentNode != null)
		  tree.addNode (clusterNode.getUID(),  equipmentNode);
		superiorNode.setMode (Node.MODE_AGGREGATE);
		  
		if (debug) {
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - child " + clusterNode +
							  "'s parent is " + superiorNode);
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - superior " + superiorNode +
							  " has child " + clusterNode);
		}
		
	  }

	  if (debug)
		tree.show ();
	} catch (SQLException e) {
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "SqlQuery.getResponse - closing result set, got sql error : " + e);
		}
	  }
	}
  }
  
  protected void doRollup (Connection connection, Tree tree, Map nameToNode, String root, int recentRun) {
	ResultSet rs = null;
	try{
	  if(run.hasRollupTable()){
	    rs = getResultSet(connection, formFastRollupSql(root,recentRun));
	  }else{
	    rs = getResultSet(connection, formRollupSql(root, recentRun));
	  }
	  while(rs.next()){
		String cluster   = rs.getString (1);
		Date startTime = rs.getTimestamp(2);
		Date endTime   = rs.getTimestamp(3);

		Node unitNode = null;
		try {
		    unitNode = (Node) nameToNode.get (cluster);
		    unitNode.setActualStart (startTime);
		    unitNode.setActualEnd   (endTime);
		    if (debug) {
			TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.doRollup - unitNode " + unitNode + " start " +
					    unitNode.getActualStart() + " end " + unitNode.getActualEnd());
		    }
		} catch (NullPointerException npe) {
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.doRollup - ERROR, org table(s) are corrupt. unitNode for cluster " + cluster +
				      " is " + unitNode + 
				      " start " + startTime + " end " + endTime);
		}
	  }
	} catch (SQLException e) {
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.doRollup - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.getResponse - closing result set, got sql error : " + e);
		}
	  }
	}

	tree.doRollup (tree.getRoot ());
  }
	
  protected String formSql (String root, int recentRun) {
	String orgTable = HierarchyConstants.ORG_TABLE_NAME + "_" + recentRun;
	String orgDescendTable = HierarchyConstants.ORGDESCEND_TABLE_NAME + "_" + recentRun;
	String orgNames = HierarchyConstants.ORGNAMES_TABLE_NAME + "_" + recentRun;
	String orgTableOrg       = orgTable + "." + HierarchyConstants.COL_ORGID;
	String orgTableRel       = orgTable + "." + HierarchyConstants.COL_RELID;
	String orgNamesName      = orgNames + "." + HierarchyConstants.COL_PRETTY_NAME;
	String orgNamesOrg       = orgNames + "." + HierarchyConstants.COL_ORGID;
	String orgDescendOrg     = orgDescendTable + "." + HierarchyConstants.COL_ORGID;
	String orgDescendDescend = orgDescendTable + "." + HierarchyConstants.COL_DESCEND;
		
	String sqlQuery = 
	  "select " + orgTableOrg + ", " + orgTableRel + ", " + orgNamesName +
	  "\nfrom "  + orgTable + ", " + orgDescendTable + ", " + orgNames +
	  "\nwhere " + orgDescendOrg + " = '" + root + "' and " + 
	  orgDescendDescend + " = " + orgTableOrg + " and " + 
	  orgNamesOrg + " = " + orgTableRel;// + 
	//"\norder by " + orgTableOrg + ", " + orgTableRel;

	if (debug) 
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.formSql - " + sqlQuery);
	
	return sqlQuery;
  }

  protected String formFastRollupSql (String root, int recentRun) {
    String derivedTable = PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.ROLLUP,recentRun);

    // result columns
    String conveyedLegStart  = DGPSPConstants.COL_STARTTIME;
    String conveyedLegEnd    = DGPSPConstants.COL_ENDTIME;
    String orgTableRel       = HierarchyConstants.COL_RELID;

    String sqlQuery = 
      "select " + orgTableRel + ", " + conveyedLegStart + ", " + conveyedLegEnd + 
      " from "  + derivedTable;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.formFastRollupSql - " + sqlQuery);
	
    return sqlQuery;
  }

  protected String formRollupSql (String root, int recentRun) {
	String orgTable = HierarchyConstants.ORG_TABLE_NAME + "_" + recentRun;
	String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;

	// result columns
	String conveyedLegStart  = conveyedLegTable + "." + DGPSPConstants.COL_STARTTIME;
	String conveyedLegEnd    = conveyedLegTable + "." + DGPSPConstants.COL_ENDTIME;
	String orgTableRel       = orgTable + "." + HierarchyConstants.COL_RELID;

	String instanceOwner     = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String instanceAsset     = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
	String itineraryAsset    = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
	String itineraryLeg      = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
	String conveyedLegLeg    = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;

	String sqlQuery = 
	  "select " + orgTableRel + ", min(" + conveyedLegStart + "), max(" + conveyedLegEnd + ")" +
	  " from "  + orgTable + ", " + assetInstanceTable + ", " + assetItineraryTable + ", " + conveyedLegTable +
	  " where " + orgTableRel + " = " + instanceOwner + " and " + 
	  instanceAsset + " = " + itineraryAsset + " and " + 
	  itineraryLeg + " = " + conveyedLegLeg +
	  " group by " + orgTableRel;

	if (debug) 
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "HierarchyQuery.formRollupSql - " + sqlQuery);
	
	return sqlQuery;
  }
  
  protected Node createNode (UIDGenerator generator, String uid, String prettyName, Map nameToNode) {
	Node clusterNode = new Org (generator, uid);
	clusterNode.setDisplayName (prettyName);
	clusterNode.setMode (Node.MODE_UNKNOWN);
	nameToNode.put (uid, clusterNode);
	return clusterNode;
  }

  protected Node createUnmappedNode (UIDGenerator generator, String uid, String prettyName) {
	//	DBUIDNode clusterNode = new Org (generator, uid);
	DBUIDNode clusterNode = new EquipmentNode (generator, uid);
	clusterNode.setDisplayName (prettyName);
	clusterNode.setMode (Node.MODE_UNKNOWN);
	return clusterNode;
  }
  
  /**
   * some sql here, probably built using a string buffer. ... 
   * don't forget to handle dates and doubles in a DB safe way and to use ' quotes.  
   * See /tops/src/org/cougaar/domain/mlm/ui/grabber/config/DBConfig 
   * for examples of functions for doing oracle/my sql syntax)
   */	
  protected String getSqlQuery () {
	StringBuffer sb = new StringBuffer ();
	
	sb.append ("");
	return sb.toString ();
  }
  
  private boolean leavesOnly = false;
  public boolean getLeavesOnly() { return leavesOnly; };
  public void setLeavesOnly(boolean leavesOnly) { this.leavesOnly = leavesOnly; };
}
