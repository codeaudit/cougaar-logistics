/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;

import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Org;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.AssetPrototypeNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CategoryNode;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;
import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;

import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

public class AssetCategoryQuery extends SqlQuery {
    
  boolean debug =  "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.HierarchyQuery.debug", 
						      "false"));
  String excludedPrototypes = "('NullTypeID')";


    DatabaseRun run;
    
    Node landNode;
    Node seaNode;
    Node airNode;
    Node propelledNode;
    Node miscNode;
    
    public AssetCategoryQuery(DatabaseRun run) {
	this.run = run;
    }
    
    public QueryResponse getResponse (Connection connection) {
	// Set up basic variables
	QueryResponse response = new QueryResponse ();
	Tree tree = new Tree ();
	UIDGenerator generator = tree.getGenerator ();
	Map nameToNode = new HashMap ();
	int recentRun = run.getRunID ();

	// Build initial parts of tree
	buildInitialTree(generator, tree, nameToNode);

	// Get ResultSet as list of prototypes
	ResultSet rs = getResultSet(connection, formSql(recentRun));

	// fill in next level of table
	buildTreeFromResult (rs, generator, tree, nameToNode);

	// Get Usage Numbers
 	rs = getResultSet(connection, formUsageSql(recentRun));
 	updateTreeWithUsage(rs, nameToNode);
	
	// finish and return
	response.addTree(tree);	
	return response;
    }
    
    private void buildInitialTree(UIDGenerator generator, Tree tree, Map nameToNode) {
	
	// create Root
	Node rootNode = createCategoryNode(generator, "All Assets", nameToNode);
	rootNode.setMode(Node.MODE_AGGREGATE);
	tree.setRoot(rootNode);

	// create Three Types: Land, Air, Sea
        airNode = createCategoryNode(generator, CategoryNode.AIR, nameToNode); 
	airNode.setMode(Node.MODE_AIR);

	seaNode = createCategoryNode(generator, CategoryNode.SEA, nameToNode); 
	seaNode.setMode(Node.MODE_SEA);
	
	landNode = createCategoryNode(generator, CategoryNode.GROUND, nameToNode); 
	landNode.setMode(Node.MODE_GROUND);
	
	propelledNode = createCategoryNode(generator, CategoryNode.SELF, nameToNode); 
	propelledNode.setMode(Node.MODE_SELF);
	
	miscNode = createCategoryNode(generator, CategoryNode.MISC, nameToNode); 
	miscNode.setMode(Node.MODE_UNKNOWN);
	
	tree.addNode(rootNode.getUID(),airNode);
	tree.addNode(rootNode.getUID(),seaNode);
	tree.addNode(rootNode.getUID(),landNode);
	tree.addNode(rootNode.getUID(),propelledNode);
	tree.addNode(rootNode.getUID(),miscNode);

    }

    private String formSql (int recentRun) {
      String protoTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
      String instTable  = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
      String protoID = DGPSPConstants.COL_PROTOTYPEID;
      String convType = DGPSPConstants.COL_CONVEYANCE_TYPE;
      String selfprop = DGPSPConstants.COL_SELFPROP;
	
      String sqlQuery = 
	"select " + protoTable+"."+protoID + ", " + protoTable+"."+convType + ", "+ selfprop + "\n" +
	"from " + protoTable + ", " + instTable + "\n" +
	"where " + protoTable+"."+protoID + " = " + instTable+"."+protoID + "\n" +
	"and " + protoTable+"."+protoID + " not in " + excludedPrototypes + "\n" +
	"order by " + protoTable+"."+protoID;
      
      if (debug) 
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.formSql - " + sqlQuery);
      
      return sqlQuery;
    }

  private String formUsageSql(int recentRun) {
    String protoTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
    String instTable  = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String legTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
    String protoID = DGPSPConstants.COL_PROTOTYPEID;
    String convID = DGPSPConstants.COL_CONVEYANCEID;

    String sqlQuery =
      "select " + protoTable+"."+protoID + ", "+ instTable+"."+convID + "\n" +
      "from " + protoTable + ", " + instTable + ", " + legTable + "\n" +
      "where " + protoTable+"."+protoID + " = " + instTable+"."+protoID + "\n" +
      "and " + instTable+"."+convID + " = " + legTable+"."+convID + "\n" +
      "and " + protoTable+"."+protoID + " not in " + excludedPrototypes + "\n" +
      "order by " + protoTable+"."+protoID + ", " + instTable+"."+convID;
    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.formSql - " + sqlQuery);
    
    return sqlQuery;
  }

    private void buildTreeFromResult (ResultSet rs, UIDGenerator generator, Tree tree, Map nameToNode) {
      try{
	String oldName = null;
	int count = 0;
	while(rs.next()){
	  String prototypeName = rs.getString(DGPSPConstants.COL_PROTOTYPEID);
	  int conveyanceType = rs.getInt(DGPSPConstants.COL_CONVEYANCE_TYPE);
	  boolean selfProp = (rs.getInt(DGPSPConstants.COL_SELFPROP) == 1) ? true : false;

	  if (oldName != null && prototypeName.equals(oldName)) {
	    count++;
	  } else {
	    if (oldName != null) {
	      AssetPrototypeNode oldNode = (AssetPrototypeNode)nameToNode.get(oldName);
	      oldNode.setTotalAssets(count);
	    }
	    oldName = prototypeName;
	    count = 1;
	    
	    if (debug) 
	      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.getResponse - " +
				  prototypeName + "\tof type " +conveyanceType);
	    
	    Node prototypeNode = (Node)nameToNode.get(prototypeName);
	    if (prototypeNode == null) {
	      prototypeNode = createPrototypeNode(generator, prototypeName, nameToNode, conveyanceType);
	    }
	    
	    Node categoryNode;
	    switch (conveyanceType) {
	    case DGPSPConstants.CONV_TYPE_TRUCK:
	    case DGPSPConstants.CONV_TYPE_TRAIN:
	    case DGPSPConstants.CONV_TYPE_SELF_PROPELLABLE:
	    case DGPSPConstants.CONV_TYPE_FACILITY:
	      categoryNode = landNode;
	      if (selfProp) categoryNode = propelledNode;
	      break;
	    case DGPSPConstants.CONV_TYPE_PLANE:
	    case DGPSPConstants.CONV_TYPE_PERSON:
	      categoryNode = airNode;
	      break;
	    case DGPSPConstants.CONV_TYPE_SHIP:
	    case DGPSPConstants.CONV_TYPE_DECK:
	      categoryNode = seaNode;
	      break;
	    default:
	      if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.buildTreeFromResult - " +
				   prototypeName + " has bad conveyance type " + DGPSPConstants.CONVEYANCE_TYPES[conveyanceType]);
	      categoryNode = miscNode;
	      break;
	    }
	    
	    tree.addNode(categoryNode.getUID(), prototypeNode);
	    
	    if (debug) {
	      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.buildTreeFromResult - child " + prototypeNode +
				  "'s parent is " + categoryNode);
	    }
	  }
	}
	if (oldName != null) {
	  AssetPrototypeNode oldNode = (AssetPrototypeNode)nameToNode.get(oldName);
	  oldNode.setTotalAssets(count);
	}
	tree.doRollup(tree.getRoot());
	if (debug)
	  tree.show ();
      } catch (SQLException e) {
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.getResponse - SQLError : " + e);
      }finally{
	if(rs!=null) {
	  try { rs.close(); } catch (SQLException e){
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "SqlQuery.getResponse - closing result set, got sql error : " + e);
	  }
	}
      }
    }

  private void updateTreeWithUsage(ResultSet rs, Map nameToNode) {
    try { 
      String oldProto = null;
      String oldConvey = null;
      boolean protoChange = false;
      boolean conveyChange = false;
      int count = 0;
      while (rs.next()) {
	String prototypeName = rs.getString(DGPSPConstants.COL_PROTOTYPEID);
	String conveyanceName = rs.getString(DGPSPConstants.COL_CONVEYANCEID);

	if (oldProto == null || !prototypeName.equals(oldProto)) {
	  protoChange = true; 
	} else { 
        protoChange = false;
    }
	if (oldConvey == null || !conveyanceName.equals(oldConvey)) { 
	  conveyChange = true;
	} else {
        conveyChange = false;
    }
	
	if (protoChange) {
	  if (oldProto != null) {
	    AssetPrototypeNode oldNode = (AssetPrototypeNode)nameToNode.get(oldProto);
	    oldNode.setUsedAssets(count);
	  }
	  oldProto = prototypeName;
	  oldConvey = conveyanceName;
	  count = 1;
	  // Something is weird with the counting...
	} else if (conveyChange) {
	  count++;
	  oldConvey = conveyanceName;
	} 
      }
      if (oldProto != null) {
	AssetPrototypeNode oldNode = (AssetPrototypeNode)nameToNode.get(oldProto);
	oldNode.setUsedAssets(count);
      }
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetCategoryQuery.getResponse - SQLError : " + e);
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "SqlQuery.getResponse - closing result set, got sql error : " + e);
	}
      }
    } 
  }

  
    private Node createCategoryNode (UIDGenerator generator, String uid, Map nameToNode) {
	Node catNode = new CategoryNode(generator, uid);
	catNode.setDisplayName (uid);
	catNode.setMode (Node.MODE_UNKNOWN);
	nameToNode.put (uid, catNode);
	return catNode;
    }

    private Node createPrototypeNode (UIDGenerator generator, String uid, Map nameToNode, int conveyanceType) {
	AssetPrototypeNode protoNode = new AssetPrototypeNode(generator, uid);
	protoNode.setDisplayName (uid);
	protoNode.setAssetPrototypeName(uid);
	//	protoNode.setMode (Node.MODE_UNKNOWN);
	protoNode.setModeFromConveyType (conveyanceType);
	nameToNode.put (uid, protoNode);
	return protoNode;
    }
}
