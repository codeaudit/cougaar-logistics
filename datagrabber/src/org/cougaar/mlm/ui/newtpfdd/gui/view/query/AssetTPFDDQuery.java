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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;

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
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CategoryNode;

public class AssetTPFDDQuery extends SqlQuery {
  boolean debug = 
  	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.debug", 
  									   "false"));
  boolean showSqlTime = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.TPFDDQuery.showSqlTime", 
									   "false"));
  protected FilterClauses filterClauses;
  protected DatabaseRun run;

  public AssetTPFDDQuery (DatabaseRun run, FilterClauses filterClauses) {
    this.filterClauses = filterClauses;
    this.run = run;
  }
  
    public QueryResponse getResponse (Connection connection) {
	// Set up basic variables
	QueryResponse response = new QueryResponse ();
	Tree tree = new Tree();
	UIDGenerator generator = tree.getGenerator ();
	int recentRun = run.getRunID ();
	
	// timing stuff
	Date then = new Date();
	
	// Set up Root
	Node rootNode = new CategoryNode(generator, "Root");
	tree.setRoot (rootNode);
	Map instanceToNode = new HashMap ();
	
	// Instance + Legs + Final Tree Setup
	attachInstances (connection, filterClauses, recentRun, generator, tree, instanceToNode);
	attachLegs      (connection, filterClauses, recentRun, generator, tree, instanceToNode);
	removeLeglessInstances(tree);
	response.addTree (tree);
	
	// end timing stuff
	if (showSqlTime) {
	    Date now = new Date();
	    long diff = now.getTime()-then.getTime();
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.getResponse - built tree in " + diff+" msecs.");
	}
	
	if (debug) {
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetTPFDDQuery.getResponse - cargo tree :");
	    tree.show ();
	}
	
	return response;
    }
    
    protected void attachInstances (Connection connection, FilterClauses filterClauses, int recentRun, 
				    UIDGenerator generator, Tree tree, Map instanceToNode) {
	ResultSet rs = getResultSet(connection, formInstanceSql (filterClauses, recentRun));

	int rows = 0;
	try{
	    while(rs.next()){
		rows++;
		String id        = rs.getString (1);
		String name      = rs.getString (2);
		String base      = rs.getString (3);

		if (instanceToNode.get (id) != null) // protect against sql bug (seems to be sql bug)
		    continue;

		Node instanceNode = createCarrierInstance (generator, id, name, base);
		tree.addNode (tree.getRoot(), instanceNode);
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
    
    private String formInstanceSql(FilterClauses filterClauses, int recentRun) {
      String conveyancePrototype = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" +recentRun;
      String conveyanceInstance = DGPSPConstants.CONV_INSTANCE_TABLE + "_" +recentRun;
      String convoyTable = DGPSPConstants.CONVOY_MEMBER_TABLE + "_" + recentRun;
      String conveyanceLeg = DGPSPConstants.CONVEYED_LEG_TABLE + "_" +recentRun;

	String sqlQuery =
	  "select " + conveyanceInstance+"."+DGPSPConstants.COL_CONVEYANCEID + ", " + DGPSPConstants.COL_BUMPERNO + 
	  ", " + DGPSPConstants.COL_OWNER + "\n" +
	  "from " + conveyancePrototype + ", " + conveyanceLeg + ", " +
	  conveyanceInstance + " left join " + convoyTable +" using ("+DGPSPConstants.COL_CONVEYANCEID +")\n" +
	  filterClauses.getAssetWhereSql(recentRun) + "\n" + 
	  "and " +  conveyancePrototype+"."+DGPSPConstants.COL_PROTOTYPEID+" = "+
	  conveyanceInstance+"."+DGPSPConstants.COL_PROTOTYPEID + "\n" +
	  "and " + conveyanceLeg+"."+DGPSPConstants.COL_CONVEYANCEID + "= " +
	  conveyanceInstance+"."+DGPSPConstants.COL_CONVEYANCEID + "\n" +
// 	  "and " + convoyTable+"."+DGPSPConstants.COL_CONVEYANCEID + " = " + 
// 	  conveyanceInstance+"."+DGPSPConstants.COL_CONVEYANCEID + "\n" +
	  "order by " + DGPSPConstants.COL_OWNER + ", " + DGPSPConstants.COL_BUMPERNO;

	if (debug) 
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetTPFDDQuery.formInstanceSql - sql: "+sqlQuery);
	return sqlQuery;
    }
  

  
  protected void attachLegs (Connection connection, FilterClauses filterClauses, int recentRun, 
			     UIDGenerator generator, Tree tree, Map instanceToNode) {
    ResultSet rs = getResultSet(connection, formLegSql (filterClauses, recentRun));
    try{
      while(rs.next()){
	String id        = rs.getString (1);
	String start     = rs.getString (2);
	String end       = rs.getString (3);
	int    legtype   = rs.getInt (4);
	String convid    = rs.getString (5);
	int    convtype  = rs.getInt (6);
	String nomen     = rs.getString (7);
	String bumperno  = rs.getString (8);
	String startLoc  = rs.getString (9);
	String startName = rs.getString (10);
	String endLoc    = rs.getString (11);
	String endName   = rs.getString (12);

	Node legNode = createAssetLeg(generator, id, start, end, convtype, 
				      startLoc, startName, endLoc, endName,
				      legtype, nomen, bumperno);
	Node instanceNode = (Node)instanceToNode.get(convid);
	if (instanceNode == null)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitQuery.attachLegsFromResult - no instance node for : " + convid);
	else {
	  tree.addNode (instanceNode.getUID(), legNode);
	}
      }
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitQuery.attachLegsFromResult - SQLError : " + e);
      e.printStackTrace();
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitQuery.attachLegsFromResult - " +
			      "closing result set, got sql error : " + e); 
	}
      }
    }
  }

  private String formLegSql(FilterClauses filterClauses, int recentRun) {
    String sep = ", ";
    String lineend = "\n";
    String isEqualTo = " = ";

    String conveyanceLeg = DGPSPConstants.CONVEYED_LEG_TABLE + "_" +recentRun;
    String conveyancePrototype = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" +recentRun;
    String conveyanceInstance = DGPSPConstants.CONV_INSTANCE_TABLE + "_" +recentRun;

    String clLegID        = conveyanceLeg       + "." + DGPSPConstants.COL_LEGID;
    String clStart        = conveyanceLeg       + "." + DGPSPConstants.COL_STARTTIME;
    String clEnd          = conveyanceLeg       + "." + DGPSPConstants.COL_ENDTIME;
    String clLegType      = conveyanceLeg       + "." + DGPSPConstants.COL_LEGTYPE;
    String clConvID       = conveyanceLeg       + "." + DGPSPConstants.COL_CONVEYANCEID;
    String clLegStartLoc  = conveyanceLeg       + "." + DGPSPConstants.COL_STARTLOC;
    String clLegEndLoc    = conveyanceLeg       + "." + DGPSPConstants.COL_ENDLOC;
    String cpConvType     = conveyancePrototype + "." + DGPSPConstants.COL_CONVEYANCE_TYPE;
    String cpNomenclature = conveyancePrototype + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
    String cpPrototypeID  = conveyancePrototype + "." + DGPSPConstants.COL_PROTOTYPEID;
    String ciBumperno     = conveyanceInstance  + "." + DGPSPConstants.COL_BUMPERNO;
    String ciConvID       = conveyanceInstance  + "." + DGPSPConstants.COL_CONVEYANCEID;
    String ciPrototypeID  = conveyanceInstance  + "." + DGPSPConstants.COL_PROTOTYPEID;

    String locTable = DGPSPConstants.LOCATIONS_TABLE + "_" + recentRun;
    String l1id     = "l1" + "." + DGPSPConstants.COL_LOCID;
    String l1geoloc = "l1" + "." + DGPSPConstants.COL_GEOLOC;
    String l1name   = "l1" + "." + DGPSPConstants.COL_PRETTYNAME;
    String l2id     = "l2" + "." + DGPSPConstants.COL_LOCID;
    String l2geoloc = "l2" + "." + DGPSPConstants.COL_GEOLOC;
    String l2name   = "l2" + "." + DGPSPConstants.COL_PRETTYNAME;


    String sqlQuery =
      "select " + clLegID + sep + clStart + sep + clEnd + sep + clLegType + sep + clConvID + sep
      + cpConvType + sep + cpNomenclature + sep + ciBumperno + sep
      + l1geoloc + sep + l1name + sep + l2geoloc + sep + l2name + lineend + 
      "from " + conveyanceLeg + sep + conveyancePrototype + sep + conveyanceInstance + sep
      + locTable + " l1"+ sep  + locTable + " l2" + lineend +
      filterClauses.getAssetTables(recentRun) +
      filterClauses.getAssetWhereSql(recentRun) + lineend +
      "and " + clConvID + isEqualTo + ciConvID + lineend +
      "and " + ciPrototypeID + isEqualTo + cpPrototypeID + lineend +
      "and " + clLegStartLoc + isEqualTo + l1id+ lineend +
      "and " + clLegEndLoc + isEqualTo + l2id + lineend +
      "order by " + clConvID + sep + clStart;
      if (debug) 
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetTPFDDQuery.formInstanceSql - sql: "+sqlQuery);
      return sqlQuery;
  }
  

  protected Node createCarrierInstance (UIDGenerator generator, String id, String name, String base) {
    CarrierInstance carrier = new CarrierInstance (generator, id);
    carrier.setDisplayName (name);
    carrier.setUnitName (base);
    return carrier;
  }
  protected Node createAssetLeg(UIDGenerator generator, String id, String start, String end, int convType, 
				String startLoc, String startName, String endLoc, String endName,
				 int legType, String carrierNomen, String bumperNo) {
	LegNode leg = new LegNode (generator, id);
	leg.setDisplayName ("");
	leg.setActualStart (getDate(start));
	leg.setActualEnd   (getDate(end));
	leg.setReadyAt     (getDate(start));
	leg.setFromCode (startLoc);
	leg.setFrom (startName);
	leg.setToCode (endLoc);
	leg.setTo (endName);
	leg.setModeFromConveyType (convType);
	leg.setLegType (legType);
	leg.setCarrierType (carrierNomen);
	leg.setCarrierName (bumperNo);

	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "UnitQuery.createLeg " + carrierNomen + " " + bumperNo + " leg is " + leg +
			      " start " + start);	
	return leg;
  }

  private void removeLeglessInstances(Tree tree) {
    Node root = tree.getRoot();
    List instances = root.getChildren();
    for (Iterator iter = instances.iterator(); iter.hasNext();) {
      Node node = tree.getNode(((Long)iter.next()).longValue());
      if (node.getChildCount() == 0) {
	iter.remove();
      }
    } 
  }
}
