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
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedList;

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
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LocationNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.MissionNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.ConvoyNode;

public class AssetInstanceQuery extends SqlQuery {

  boolean debugALot = false;
  boolean debug =
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.AssetInstanceQuery.debug",
				       "false"));
  boolean showSqlTime =
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.AssetInstanceQuery.showSqlTime",
				       "false"));

  protected FilterClauses filterClauses;
  protected DatabaseRun run;

  public AssetInstanceQuery (DatabaseRun run, FilterClauses clauses) {
    filterClauses = clauses;
    this.run = run;
  }

  public QueryResponse getResponse (Connection connection) {
    // Set up basic variables
    QueryResponse response = new QueryResponse ();
    Tree tree = new Tree();
    tree.setRoot (new Node ()); // root could be anything
    UIDGenerator generator = tree.getGenerator ();
    int recentRun = run.getRunID ();

    // timing stuff
    Date then = new Date();

    // Get ResultSet
    ResultSet rs = getResultSet(connection, formCarrierSql(recentRun));

    Map carrierToConvoy    = new HashMap ();
    Map convoyToStartEnd   = new HashMap ();
    Map convoyToCarrierMap = new HashMap ();
    Map uidToCarrierNode   = new HashMap ();

    // Build tree (Response has stuff added in this function)
    buildPrototypeTreesFromResult(rs, generator, carrierToConvoy, convoyToStartEnd, convoyToCarrierMap, uidToCarrierNode,
				  tree);

    rs = getResultSet(connection, formConvoyLegSql(recentRun));

    // Build tree (Response has stuff added in this function)
    attachLegsFromResult(rs, generator, carrierToConvoy, convoyToStartEnd, convoyToCarrierMap, uidToCarrierNode, tree);

    // end timing stuff
    if (showSqlTime) {
      Date now = new Date();
      long diff = now.getTime()-then.getTime();
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.getResponse - built tree in " + diff+" msecs.");
    }

    response.addTree (tree);

    if (debug)
      tree.show();

    return response;
  }

  private void buildPrototypeTreesFromResult(ResultSet rs, UIDGenerator generator,
					     Map carrierToConvoy, Map convoyToStartEnd, Map convoyToCarrierMap,
					     Map uidToCarrierNode, Tree tree) {
    List baseNames = new LinkedList();
    List convoys = new LinkedList();
    List instances = new LinkedList();
    Node locationNode = null;
    Node convoyNode = null;
    Hashtable convoyIDToNode = new Hashtable();

    String legID = null;

    String convID = null;
    int convType = 0;
    Date start = null;
    Date end = null;

    Node tempInstance = null;

    if (debug) TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.buildPrototypeTreesFromResult - ");

    // Note: The workings of the following are very dependent on the sort order created by the SQL query
    try {
      while(rs.next()) {
	String baseName = rs.getString(DGPSPConstants.COL_OWNER);
	convID = rs.getString(DGPSPConstants.COL_CONVEYANCEID);
	String bumperno = rs.getString(DGPSPConstants.COL_BUMPERNO);
	convType = rs.getInt(DGPSPConstants.COL_CONVEYANCE_TYPE);
	start = rs.getTimestamp(DGPSPConstants.COL_STARTTIME);
	end   = rs.getTimestamp(DGPSPConstants.COL_ENDTIME);
	String convoyid = rs.getString(DGPSPConstants.COL_CONVOYID);
	String convoyName = rs.getString(DGPSPConstants.COL_PRETTYNAME);

	if (debugALot) TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "convID: "+convID+"  legID: "+legID+"  start: "+start+"  end: "+end);

	// new baseName - create Node in tree / grab LocationNode
	// remember that response is ordered by baselocid so you will only change baselocid when
	// a new one appears
	if (!baseNames.contains(baseName)) {
	  locationNode = createLocationNode(generator,baseName,convType);
	  baseNames.add(baseName);
	  tree.addNode (tree.getRoot().getUID(), locationNode);

	  if (debug) {
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.buildPrototypeTreesFromResult - starting subtree with root " +
				locationNode);
	  }
	}

	Node goodParent = null;

	// CONVOY STUFF
	if (convoyid ==null) {
	  goodParent = locationNode;
	} else if (!convoys.contains(convoyid)) {
	  convoyNode = createConvoyNode(generator, convoyid, convoyName, convType);
	  convoys.add(convoyid);
	  convoyIDToNode.put(convoyid,convoyNode);
	  //		  newTree.addNode(locationNode.getUID(),convoyNode);
	  tree.addNode(locationNode.getUID(),convoyNode);
	  instances = new LinkedList(); // done so that second instances in a new convoy will appear
	  tempInstance = null;
	  goodParent = convoyNode;

	  Date [] startEnd = (Date []) convoyToStartEnd.get(convoyid);
	  convoyToStartEnd.put (convoyid, new Date [] {start, end});

	  if (debug)
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery - mapping convoy " + convoyid +
				" to start, end " + startEnd);

	} else {
	  convoyNode = (Node) convoyIDToNode.get(convoyid);
	  goodParent = convoyNode;
	}

	// new instance - create Node in tree / grab instance
	if (!instances.contains(convID)) {
	  Node instanceNode = createCarrierNode(generator, convID, convType, bumperno);
	  instances.add(convID);
	  uidToCarrierNode.put(convID,instanceNode);

	  tree.addNode(goodParent.getUID(),instanceNode);

	  List convoysForCarrier = (List) carrierToConvoy.get(convID);
	  if (convoysForCarrier == null)
	    carrierToConvoy.put (convID, convoysForCarrier=new ArrayList());
	  convoysForCarrier.add (convoyid);

	  if (debug)
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery - mapping convoy " + convoyid +
				" to carrier " + convoysForCarrier);

	  Map uidToNode = (Map) convoyToCarrierMap.get(convoyid);
	  if (uidToNode == null)
	    convoyToCarrierMap.put (convoyid, uidToNode=new HashMap(19));
	  uidToNode.put (convID, instanceNode);

	  if (debug)
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery - mapping carrier " + convID +
				" to node " + instanceNode);
	}
      }
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.getResponse - SQLError : " + e);
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "SqlQuery.getResponse - closing result set, got sql error : " + e);
	}
      }
    }

  }

  private void attachLegsFromResult(ResultSet rs, UIDGenerator generator,
				    Map carrierToConvoy, Map convoyToStartEnd, Map convoyToCarrierMap,
				    Map uidToCarrierNode, Tree carrierTree) {
    try {
      Date lastEnd = null;
      Node legNode = null;
      Node missionNode = null;
      String lastConvoyID = null;

      while(rs.next()) {
	String carrierConvID = rs.getString(1);
	String legID = rs.getString(2);
	Date legStart = rs.getTimestamp(3);
	Date legEnd   = rs.getTimestamp(4);
	int  legType  = rs.getInt (5);

	if (lastEnd == null)
	  lastEnd = legEnd;

	List convoys = (List) carrierToConvoy.get (carrierConvID);
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.attachLegs for carrier " + carrierConvID + " convoys " +
			     convoys);
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.attachLegs leg " + legID + " start " + legStart + " end " + legEnd);

	for (int i = 0; i < convoys.size(); i++) {
	  String convoyID = (String) convoys.get(i);
	  if (convoyID == null) {
	    Node instanceNode = (Node) uidToCarrierNode.get (carrierConvID);

	    if (lastEnd.getTime() != legStart.getTime()) {
	      if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.attachLegs creating mission node b/c lastEnd " + lastEnd +
				   " != legStart " + legStart);
	      missionNode = createMissionNode(generator,legID,legStart,legEnd, instanceNode.getMode());
	      carrierTree.addNode(instanceNode.getUID(), missionNode);
	    }
	    legNode = createLegNode(generator,legID,legStart,legEnd, instanceNode.getMode(), legType);
	    carrierTree.addNode(missionNode.getUID(), legNode);
	    lastEnd = legEnd;
	  }
	  else {
	    Date [] startEnd = (Date []) convoyToStartEnd.get(convoyID);
	    if (legStart.getTime () >= startEnd[0].getTime () &&
		legEnd.getTime ()   <= startEnd[1].getTime ()) {
	      if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.attachLegs found time match for convoy " + convoyID);

	      Map uidToNode = (Map) convoyToCarrierMap.get (convoyID);
	      Node instanceNode = (Node) uidToNode.get (carrierConvID);

	      if (lastEnd.getTime() != legStart.getTime() || !convoyID.equals(lastConvoyID)) {
		missionNode = createMissionNode(generator,legID,legStart,legEnd, instanceNode.getMode());
		carrierTree.addNode(instanceNode.getUID(), missionNode);
	      }

	      lastConvoyID = convoyID;

	      legNode = createLegNode(generator,legID,legStart,legEnd, instanceNode.getMode(), legType);
	      carrierTree.addNode(missionNode.getUID(), legNode);
	      lastEnd = legEnd;
	    }
	  }
	}
      }
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.getResponse - SQLError : " + e);
    } catch (Exception f) {
      System.err.println ("AssetInstanceQuery.getResponse - Exception : " + f);
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "SqlQuery.getResponse - closing result set, got sql error : " + e);
	}
      }
    }
  }

  private String formCarrierSql(int recentRun) {
    String instanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String legTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" +recentRun;
    String protoTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
    String convoyNameTable = DGPSPConstants.CONVOYS_TABLE + "_" + recentRun;
    String convoyTable = DGPSPConstants.CONVOY_MEMBER_TABLE + "_" + recentRun;
    String queriedPrototype = (String)filterClauses.getCarrierTypes().get(0);

    String baseloc = DGPSPConstants.COL_OWNER;
    String convid = DGPSPConstants.COL_CONVEYANCEID;
    String prototypeid = DGPSPConstants.COL_PROTOTYPEID;
    String convType = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String bumperno = DGPSPConstants.COL_BUMPERNO;
    String starttime = convoyNameTable+"."+DGPSPConstants.COL_STARTTIME;
    String endtime   = convoyNameTable+"."+DGPSPConstants.COL_ENDTIME;
    String convoyid = DGPSPConstants.COL_CONVOYID;
    String prettyname = DGPSPConstants.COL_PRETTYNAME;

    String sqlQuery =
      "select " + baseloc + ", " + instanceTable+"."+convid + ", "
      + convType + ", " + bumperno + ", "
      + starttime + ", " + endtime + ", " + convoyTable+"."+convoyid +", "
      + prettyname +"\n" +
      "from " + legTable + ", " + protoTable + ", " + instanceTable +
      "\nleft join "+convoyTable+" using ("+convid+") left join "
      + convoyNameTable +" using ("+convoyid+")\n"+
      "where " + instanceTable + "." + convid + " = " + legTable + "." + convid + " and \n"
      + protoTable + "." + prototypeid + " =  '" + queriedPrototype + "'" + " and \n"
      + instanceTable + "." + prototypeid + " =  '" + queriedPrototype + "'" + // " and \n" +
      "order by " + baseloc + ", " + convoyid + ", "+ convid + ", " + starttime;

    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.formSql - \n" + sqlQuery);

    return sqlQuery;
  }

  private String formConvoyLegSql(int recentRun) {
    String carrierInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String legTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" +recentRun;
    String queriedPrototype = (String)filterClauses.getCarrierTypes().get(0);

    String carrierConvID = carrierInstanceTable+"."+DGPSPConstants.COL_CONVEYANCEID;
    String legConvID     = legTable+     "."+DGPSPConstants.COL_CONVEYANCEID;
    String carrierInstancePrototypeID = carrierInstanceTable+"."+DGPSPConstants.COL_PROTOTYPEID;
    String legid         = DGPSPConstants.COL_LEGID;
    String legStartTime = legTable+"."+DGPSPConstants.COL_STARTTIME;
    String legEndTime   = legTable+"."+DGPSPConstants.COL_ENDTIME;
    String cLegType     = legTable+"."+DGPSPConstants.COL_LEGTYPE;

    String sqlQuery =
      "select " + carrierConvID + ", " + legid + ", " + legStartTime    + ", " + legEndTime + ", " + cLegType +
      "\nfrom "   + legTable + ", " + carrierInstanceTable +
      "\nwhere " +
      carrierInstancePrototypeID + " = '" + queriedPrototype + "'" + " and \n" +
      carrierConvID + " = " + legConvID +
      "\norder by " + carrierConvID + ", " + legStartTime;

    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "AssetInstanceQuery.formSql - \n" + sqlQuery);

    return sqlQuery;
  }

  private Node createLocationNode(UIDGenerator generator, String name, int convType) {
    LocationNode locNode = new LocationNode(generator, name);
    locNode.setDisplayName(name);
    locNode.setMode(Node.MODE_UNKNOWN);
    locNode.setModeFromConveyType(convType);
    locNode.setLocationName(name);
    return locNode;
  }
  private Node createConvoyNode(UIDGenerator generator, String convoyID, String convoyName, int convType) {
    if ((convType == DGPSPConstants.CONV_TYPE_TRAIN) && convoyName.startsWith ("Convoy"))
      convoyName = "Train" + convoyName.substring(convoyName.indexOf (' '));

    ConvoyNode convoyNode = new ConvoyNode(generator, convoyID);
    convoyNode.setDisplayName(convoyName);
    convoyNode.setConvoyID(convoyID);
    convoyNode.setMode(Node.MODE_UNKNOWN);
    convoyNode.setModeFromConveyType(convType);
    return convoyNode;
  }

  private Node createCarrierNode(UIDGenerator generator, String name, int convType, String bumperno) {
    CarrierInstance carrierNode = new CarrierInstance(generator, name);
    carrierNode.setDisplayName(bumperno);
    carrierNode.setMode(Node.MODE_UNKNOWN);
    carrierNode.setModeFromConveyType(convType);
    carrierNode.setCarrierName(name);
    return carrierNode;
  }

  private Node createLegNode(UIDGenerator generator, String id, Date start, Date end, int mode, int legType) {
    LegNode leg = new LegNode (generator, id);
    leg.setDisplayName ("");
    leg.setActualStart (start);
    leg.setActualEnd   (end);
    leg.setMode(mode);
    leg.setLegType(legType);
    return leg;
  }

  private Node createMissionNode(UIDGenerator generator, String id, Date start, Date end, int mode) {
    MissionNode leg = new MissionNode (generator, id);
    leg.setDisplayName ("");
    leg.setActualStart (start);
    leg.setActualEnd   (end);
    leg.setMode(mode);
    return leg;
  }
}
