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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.derived.PrepareDerivedTables;

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

public class CarrierQuery extends UnitQuery {
    public static final int DEPTH_BY = 1;
    public static final int DEPTH_TYPE = 2;
    public static final int DEPTH_ITIN = 3;
    public static final int DEPTH_LEAF = 4;
    

  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.CarrierQuery.debug", 
									   "false"));
  boolean showSqlTime = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.CarrierQuery.showSqlTime", 
									   "false"));

  public CarrierQuery (DatabaseRun run, FilterClauses filterClauses) {
	super (run, filterClauses);
  }

  public QueryResponse getResponse (Connection connection) {
	//	SortedQueryResponse response = new SortedQueryResponse (); // trees returned are sorted
	//	response.setForestWithComparator (getComparator ());
	QueryResponse response = new QueryResponse ();
	
	// first figure out which run to use
	//	int recentRun = getRecentRun (connection);
	int recentRun = run.getRunID ();

	Tree carrierTree = new Tree ();
	UIDGenerator generator = carrierTree.getGenerator ();

	String unitID = (String) filterClauses.getUnitDBUIDs().iterator ().next();
	Node byCarrier = new ByCarrier (generator, unitID);
	byCarrier.setWasQueried (true);
	
	carrierTree.setRoot (byCarrier);

	long time;
	long totalTime;
	totalTime=System.currentTimeMillis();

	// carrier tree-------------------------------
	time=System.currentTimeMillis();
	Map protoToCarrierType = new HashMap ();
	ResultSet rs;
	if(run.hasCarrierTypeTable()){
	  rs = getResultSet(connection, formFastCarrierTypeSql(filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formCarrierTypeSql (filterClauses, recentRun));
	}
	buildTypeTreeFromResult  (rs, generator, carrierTree, protoToCarrierType);
	if(showSqlTime){
	  System.out.println((run.hasCarrierTypeTable()?"Fast ":"")+"CarrierType query took: "+
			     (System.currentTimeMillis()-time));
	}

	// attach converyance instances
	time=System.currentTimeMillis();
	if(run.hasCarrierInstanceTable()){
	  rs = getResultSet(connection, formFastCarrierInstanceSql(filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formCarrierInstanceSql (filterClauses, recentRun));
	}
	Map carrierInstanceToNode = attachConveyances (rs, generator, protoToCarrierType, carrierTree);
	if(showSqlTime){
	  System.out.println((run.hasCarrierInstanceTable()?"Fast ":"")+"CarrierInstance query took: "+
			     (System.currentTimeMillis()-time));
	}

	//	filterClauses.setCarrierInstances (new ArrayList (carrierInstanceToNode.keySet()));
	
	// attach cargo types
	Map protoToCargoType = new HashMap ();
	Map assetNodeToCarrierInstance = new HashMap ();
	//	Map nomenToCargoTypeNode = 
	//	  createCargoProtoNodes (connection, filterClauses, recentRun, generator, cargoTypeToCarrierInstance);
	//	Map protoToCargoType = new HashMap ();

	time=System.currentTimeMillis();
	if(run.hasCargoTypeTable()){
	  rs = getResultSet(connection, formFastCargoTypeSql(filterClauses, recentRun));
	  SortedMap nameToNode  = buildCargoProtosFromResult (rs, generator, protoToCargoType, assetNodeToCarrierInstance);
	  buildCargoProtoTree(assetNodeToCarrierInstance, carrierInstanceToNode, carrierTree);
	}else{
	  rs = getResultSet(connection, formFirstCargoTypeSql (filterClauses, recentRun));
	  SortedMap nameToNode  = 
	    buildCargoProtosFromResult (rs, generator, protoToCargoType, assetNodeToCarrierInstance);
	  
	  rs = getResultSet(connection, formSecondCargoTypeSql (filterClauses, recentRun));
	  SortedMap nameToNode2 = 
	    buildCargoProtosFromResult (rs, generator, protoToCargoType, assetNodeToCarrierInstance);
	  
	  nameToNode.putAll (nameToNode2);
	  buildCargoProtoTree(assetNodeToCarrierInstance, carrierInstanceToNode, carrierTree);
	}
	if(showSqlTime){
	  System.out.println((run.hasCargoTypeTable()?"Fast ":"")+"CargoType (Carrier) query took: "+
			     (System.currentTimeMillis()-time));
	}

	// attach cargo instances
	Map instanceToNode = new HashMap ();
	time=System.currentTimeMillis();
	rs = getResultSet(connection, formCargoInstanceSql (filterClauses, recentRun, false));
	attachInstancesFromResult (rs, generator, carrierInstanceToNode, carrierTree, instanceToNode);

	rs = getResultSet(connection, formCargoInstanceSql (filterClauses, recentRun, true));
	attachInstancesFromResult (rs, generator, carrierInstanceToNode, carrierTree, instanceToNode);
	if(showSqlTime){
	  System.out.println((false?"Fast ":"")+"CargoInstance (Carrier) query took: "+
			     (System.currentTimeMillis()-time));
	}

	// attach cargo legs
	time=System.currentTimeMillis();
	if(run.hasCargoLegTable()){
	  rs = getResultSet(connection, formFastCargoLegSql(filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formCargoLegSql(filterClauses, recentRun));
	}
	if(showSqlTime){
	  System.out.println((run.hasCargoLegTable()?"Fast ":"")+"CargoLeg (Carrier) query took: "+
			     (System.currentTimeMillis()-time));
	}
	time=System.currentTimeMillis();
	attachLegs (rs, generator, carrierInstanceToNode, instanceToNode, carrierTree);
	if(showSqlTime){
	  System.out.println("Attaching CargoLeg query results took: "+
			     (System.currentTimeMillis()-time));
	}
	
	if (debug)
	  System.out.println ("CarrierQuery.getResponse - by carrier has " + byCarrier.getChildCount() +
						  " children.");
	/*
	if (debug) {
	  System.out.println ("CarrierQuery.getResponse - carrier tree : ");
	  carrierTree.show ();
	}
	*/

	//	response.addForest (carrierTree.getTreesFromChildren(byCarrier, getComparator()));
	java.util.Set childrenOfCarrier = carrierTree.getTreesFromChildren(byCarrier);
	if (debug) {
	  System.out.println ("CarrierQuery.getResponse - by carrier has " + childrenOfCarrier.size () + 
						  " children.");
	}
	
	response.addForest (childrenOfCarrier);

	if(showSqlTime){
	  System.out.println((false?"Fast ":"")+"Total Carrier query took: "+
			     (System.currentTimeMillis()-totalTime));
	}

	return response;
  }

  protected Comparator getComparator () {
	return new Comparator () {
		public int compare (Object o1, Object o2) 
		{
		  Tree t1 = (Tree) o1;
		  Tree t2 = (Tree) o2;
		  if (debug)
			System.out.println ("CarrierQuery.getResponse - comparing tree 1 " + t1.getRoot () + " with " + 
								t2.getRoot ());

		  return (int) (t1.getRoot ().getUID() - t2.getRoot ().getUID());
		}
	  };
  }
	  
  /**
   *
   */
  protected SortedMap buildCargoProtosFromResult (ResultSet rs, UIDGenerator generator, Map protoToNode,
												  Map nodeToCarrier) {
	SortedMap nameToNode = new TreeMap ();
	
	try{
	  int i = 0;
	  while(rs.next()){
		String type  = rs.getString (1);
		String nomen = rs.getString (2);
		String proto = rs.getString (3);
		String carrierInstance = rs.getString (4);
		//		String parentProto = rs.getString (4);
		boolean isLowFi = rs.getString (5).charAt(0)=='t';
		
		Node typeNode = createCargoType (generator, proto, type, nomen, isLowFi);
		nameToNode.put    (nomen, typeNode);
		protoToNode.put   (proto, typeNode);
		nodeToCarrier.put (typeNode, carrierInstance);
		//		protoToNode.put (parentProto, typeNode);
		if (debug)
		  System.out.println ("CarrierQuery.buildCargoProtosFromResult - mapping type node, type " + 
							  type + " proto " + proto +
							  "-> carrier " + carrierInstance);
	  }
	  if (debug)
		System.out.println ("CarrierQuery.buildCargoProtosFromResult - got " + i + " cargo types.");
	} catch (SQLException e) {
	  System.out.println ("CarrierQuery.buildCargoProtosFromResult - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("CarrierQuery.buildCargoProtosFromResult - " +
							  "closing result set, got sql error : " + e); 
		}
	  }
	}
	return nameToNode;
  }

  /**
   * assetNode     -> carrier instance DB UID
   * carrier DBUID -> carrier instance
   */
  protected void buildCargoProtoTree (Map assetNodeToCarrierInstance, Map carrierToNode, Tree tree) {
	for (Iterator iter = assetNodeToCarrierInstance.keySet().iterator (); iter.hasNext(); ) {
	  Node node = (Node) iter.next();
	  String carrierInstanceDBUID = (String) assetNodeToCarrierInstance.get (node);
	  Node carrierInstance = (Node) carrierToNode.get (carrierInstanceDBUID);
	  
	  if (carrierInstance == null) {
		System.out.println ("CarrierQuery.buildCargoProtoTree - ERROR - no carrier instance for : " + carrierInstanceDBUID);
	  } else {
		tree.addNode (carrierInstance, node);
      }
	}

	//	if (debug)
	//	  tree.show ();
  }

  protected Map attachConveyances (ResultSet rs, UIDGenerator generator, Map protoToNode, Tree tree) {
	Map convInstanceToNode = new HashMap ();
	
	try{
	  while(rs.next()){
		String id        = rs.getString (1);
		String proto     = rs.getString (2);
		String name      = rs.getString (3);
		
		Node instanceNode = createCarrierInstance (generator, id, name);
		Node protoNode = (Node)protoToNode.get(proto);
		if (protoNode == null)
		  System.out.println ("CarrierQuery.attachConveyancesFromResult - no proto node for : " + proto);
		else {
		  //		  if (debug)
		  //			System.out.println ("CarrierQuery.attachConveyancesFromResult - parent proto " + protoNode);
		  tree.addNode (protoNode.getUID(), instanceNode);
		}
		convInstanceToNode.put (id, instanceNode);
	  }
	} catch (SQLException e) {
	  System.out.println ("CarrierQuery.attachInstancesFromResult - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("CarrierQuery.attachInstancesFromResult - " + 
							  "closing result set, got sql error : " + e); 
		}
	  }
	}
	return convInstanceToNode;
  }

  protected void buildTypeTreeFromResult (ResultSet rs, UIDGenerator generator, Tree tree, Map typeToNode) {
	try{
	  int rows = 0;
	  while(rs.next()){
		String type  = rs.getString (1);
		String nomen = rs.getString (2);
		String proto = rs.getString (3);
		int selfProp = rs.getInt (4);
		
		Node typeNode = createCarrierType (generator, proto, type, nomen, selfProp);
		
		tree.addNode (tree.getRoot().getUID(), typeNode);
		typeToNode.put (proto, typeNode);
		rows++;
	  }
	  if (debug)
		System.out.println ("CarrierQuery.buildTypeTreeFromResult - rows " + rows);

	  //	  if (debug)
	  //		tree.show ();
	} catch (SQLException e) {
	  System.out.println ("CarrierQuery.getResponse - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("CarrierQuery.getResponse - closing result set, got sql error : " + e); 
		}
	  }
	}
  }

  protected void attachInstancesFromResult (ResultSet rs, UIDGenerator generator, 
											Map carrierIDToInstanceNode, Tree cargoTree, Map instanceToNode) {
	try{
	  while(rs.next()){
		String id        = rs.getString (1);
		String proto     = rs.getString (2);
		String name      = rs.getString (3);
		String aggnumber = rs.getString (4);
		String unitName  = rs.getString (5);
		String carrierInstanceID = rs.getString (6);
		String protoNomen = rs.getString (7);
		
		Node instanceNode = createCargoInstance (generator, id, name, aggnumber, unitName, protoNomen);

		Node carrierInstanceNode = (Node)carrierIDToInstanceNode.get(carrierInstanceID);
		
		if (carrierInstanceNode == null)
		  System.out.println ("CarrierQuery.attachInstancesFromResult - no carrier instance node for : " + 
							  carrierInstanceID);
		else {
		  //		  if (debug)
		  //			System.out.println ("UnitQuery.attachInstancesFromResult - parent proto " + protoNode);
		  Node cargoTypeParent = cargoTree.getChildWithDBUID (carrierInstanceNode, proto);
		  if (cargoTypeParent == null)
			System.out.println ("CarrierQuery.attachInstancesFromResult - no cargo type node for : " + 
								proto);
		  else {
			cargoTree.addNode (cargoTypeParent, instanceNode);
			//carrierIDToCargoID.put (id, carrierInstanceID);
		  }
		}
		instanceToNode.put (id, instanceNode);
	  }
	} catch (SQLException e) {
	  System.out.println ("UnitQuery.attachInstancesFromResult - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("UnitQuery.attachInstancesFromResult - " + 
							  "closing result set, got sql error : " + e); 
		}
	  }
	}
  }

  protected void attachLegs (ResultSet rs, UIDGenerator generator, Map carrierIDToInstanceNode, 
							 Map instanceToNode, Tree cargoTree) {
	try{
	  int total = 0;
	  while(rs.next()){
		total++;
		
		String id        = rs.getString (1);
		String start     = rs.getString (2);
		String end       = rs.getString (3);
		String readyAt   = rs.getString (4);
		String startLoc  = rs.getString (5);
		String startName = rs.getString (6);
		String endLoc    = rs.getString (7);
		String endName   = rs.getString (8);
		int    convtype  = rs.getInt (9);
		int    legtype   = rs.getInt (10);
		String assetid   = rs.getString (11);
		String carrierInstID = rs.getString (12);
		String instanceProto = rs.getString (13);
		
		Node legNode = createLeg (generator, id, start, end, readyAt, startLoc, startName, endLoc, endName, convtype, 
								  legtype, null, null);
		Node carrierInstanceNode = (Node)carrierIDToInstanceNode.get(carrierInstID);

		if (debug)
		  System.out.println ("CarrierQuery.attachLegsFromResult - instance asset id " + assetid +
							  " carrier inst " + carrierInstID + " instance proto " + instanceProto);

		if (carrierInstanceNode == null)
		  System.out.println ("CarrierQuery.attachLegsFromResult - no instance node for : " + carrierInstID);
		else {
		  Node cargoType = cargoTree.getChildWithDBUID (carrierInstanceNode, instanceProto);
		  if (cargoType == null) {
			System.out.println ("CarrierQuery.attachLegsFromResult - no cargo type (1) node for : " + 
								instanceProto);
		  } else {
			Node cargoInstance = cargoTree.getChildWithDBUID (cargoType, assetid);
			if (cargoInstance == null)
			  System.out.println ("CarrierQuery.attachLegsFromResult - no cargo instance for : " + 
								  cargoInstance);
			else {
			  cargoTree.addNode (cargoInstance, legNode);
			}
		  }
		}
	  }
	  if (debug) 
		System.out.println ("CarrierQuery.attachInstancesFromResult - total rows for legs " + total);
	  
	} catch (SQLException e) {
	  System.out.println ("CarrierQuery.attachInstancesFromResult - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("CarrierQuery.attachInstancesFromResult - " + 
							  "closing result set, got sql error : " + e); 
		}
	  }
	}
  }

  protected Node createCarrierType (UIDGenerator generator, String proto, String type, String nomen, int selfProp) {
	CarrierType carrierType = new CarrierType (generator, proto);
	carrierType.setDisplayName (nomen);
	carrierType.setCarrierName (nomen);
	carrierType.setCarrierType (type);
	carrierType.setSelfPropelled ((selfProp == 1));
	
	//	carrierType.setCarrierTypeDBID (proto);
	return carrierType;
  }

  protected Node createCarrierInstance (UIDGenerator generator, String id, String name) {
	CarrierInstance carrier = new CarrierInstance (generator, id);
	carrier.setDisplayName (name);
	return carrier;
  }

  protected String formFastCarrierTypeSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable=
      PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARRIER_TYPE,recentRun);

    String instanceOwner  = DGPSPConstants.COL_OWNER;
    String conveyanceInstanceSelfProp = DGPSPConstants.COL_SELFPROP;
    String conveyanceProtoProto = DGPSPConstants.COL_PROTOTYPEID;
    String conveyanceProtoType = DGPSPConstants.COL_ALP_TYPEID;
    String conveyanceProtoNomen = DGPSPConstants.COL_ALP_NOMENCLATURE;

    String sqlQuery = 
      "select distinct " + conveyanceProtoType + ", " + conveyanceProtoNomen + ", " + conveyanceProtoProto + ", " + 
      conveyanceInstanceSelfProp +
      " from " + derivedTable +
      filterClauses.getUnitWhereSql (instanceOwner) +
      " order by " + conveyanceInstanceSelfProp + ", " + conveyanceProtoNomen;

    if (debug) 
      System.out.println ("CarrierQuery.formFastCarrierTypeSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formCarrierTypeSql (FilterClauses filterClauses, int recentRun) {
	String assetInstanceTable  = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
	String conveyedLegTable    = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
	String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;

	String instanceOwner  = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String instanceID     = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
	String itineraryID    = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
	String itineraryLeg   = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
	String conveyedLegLeg = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
	String conveyedLegID  = conveyedLegTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	String conveyanceInstanceID = conveyanceInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	String conveyanceInstanceProto = conveyanceInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String conveyanceInstanceSelfProp = conveyanceInstanceTable + "." + DGPSPConstants.COL_SELFPROP;
	String conveyanceProtoProto = conveyancePrototypeTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String conveyanceProtoType = conveyancePrototypeTable + "." + DGPSPConstants.COL_ALP_TYPEID;
	String conveyanceProtoNomen = conveyancePrototypeTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;

	String sqlQuery = 
	  "select distinct " + conveyanceProtoType + ", " + conveyanceProtoNomen + ", " + conveyanceProtoProto + ", " + 
	   conveyanceInstanceSelfProp +
	  " from "  + assetInstanceTable + ", " + assetItineraryTable + ", " + conveyedLegTable + ", " + 
	  conveyanceInstanceTable + ", " + conveyancePrototypeTable +
	  //" where " + instanceOwner + " = '" + unitUID + "' and " + 
	  filterClauses.getUnitWhereSql (instanceOwner) + " and " +
	  instanceID + " = " + itineraryID + " and " + 
	  itineraryLeg + " = " + conveyedLegLeg + " and " + 
	  conveyedLegID + " = " + conveyanceInstanceID + " and " + 
	  conveyanceInstanceProto + " = " + conveyanceProtoProto + 
	  " order by " + conveyanceInstanceSelfProp + "," + conveyanceProtoNomen;

	if (debug) 
	  System.out.println ("CarrierQuery.formCarrierTypeSql - \n" + sqlQuery);
	
	return sqlQuery;
  }

  protected String formFastCarrierInstanceSql(FilterClauses filterClausesString, int recentRun){
    String derivedTable=
      PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARRIER_INSTANCE,recentRun);

    String instanceID        = DGPSPConstants.COL_CONVEYANCEID;
    String instanceName      = DGPSPConstants.COL_BUMPERNO;
    String instanceProto     = DGPSPConstants.COL_PROTOTYPEID;
    
    String assetInstanceOwner = DGPSPConstants.COL_OWNER;
    
    String sqlQuery = 
      "select distinct " + instanceID + ", " + instanceProto + ", " + instanceName +
      " from " + derivedTable +
      filterClauses.getUnitWhereSql (assetInstanceOwner) +
      " order by " + instanceName;
    
    if (debug) 
      System.out.println ("CarrierQuery.formFastCarrierInstanceSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formCarrierInstanceSql (FilterClauses filterClausesString, int recentRun) {
	String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;

	String instanceID        = conveyanceInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	String instanceName      = conveyanceInstanceTable + "." + DGPSPConstants.COL_BUMPERNO;
	String instanceProto     = conveyanceInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;

	String assetInstanceOwner     = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String assetInstanceID   = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
	String itinID             = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
	String itinLeg            = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
	String conveyedLegLeg     = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
	String conveyedLegConvID  = conveyedLegTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	
	String sqlQuery = 
	  "select distinct " + instanceID + ", " + instanceProto + ", " + instanceName +
	  " from "  + conveyanceInstanceTable + ", " + assetInstanceTable + ", " + assetItineraryTable + ", " +
	    conveyedLegTable +
	  filterClauses.getUnitWhereSql (assetInstanceOwner) +
	  " and " + assetInstanceID + " = " + itinID + 
	  " and " + itinLeg + " = " + conveyedLegLeg +
	  " and " + conveyedLegConvID + " = " + instanceID +
	  " order by " + instanceName;

	if (debug) 
	  System.out.println ("CarrierQuery.formCarrierInstanceSql - \n" + sqlQuery);
	
	return sqlQuery;
  }

  protected String formFastCargoTypeSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = 
      PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_TYPE,recentRun);

    String self2TypeID  = DGPSPConstants.COL_ALP_TYPEID;
    String self2Nomen   = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String self2Proto   = PrepareDerivedTables.COL_INST_PROTOTYPEID;
    String isLowFi = DGPSPConstants.COL_IS_LOW_FIDELITY;
    
    String instanceOwner  = DGPSPConstants.COL_OWNER;
    String convInstanceID = DGPSPConstants.COL_CONVEYANCEID;
    
    String sqlQuery = 
      "select distinct " + self2TypeID + ", " + self2Nomen + ", " + self2Proto + ", " + 
      convInstanceID + "," + isLowFi +
      "\nfrom "  + derivedTable +
      filterClauses.getUnitWhereSql (instanceOwner) + 
      "\norder by " + self2Nomen;

    if (debug) 
      System.out.println ("CarrierQuery.formFastCargoTypeSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formFirstCargoTypeSql (FilterClauses filterClauses, int recentRun) {
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;

	String self2TypeID  = "self2" + "." + DGPSPConstants.COL_ALP_TYPEID;
	String self2Nomen   = "self2" + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
	String self2Proto   = "self2" + "." + DGPSPConstants.COL_PROTOTYPEID;

	String instanceOwner  = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String instanceProto  = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String self1Proto     = "self1" + "." + DGPSPConstants.COL_PROTOTYPEID;
	String self1ParentProto  = "self1" + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;

	String convInstanceTable    = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String convInstanceID       = convInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
		
	String sqlQuery = 
	  "select distinct " + self2TypeID + ", " + self2Nomen + ", " + self2Proto + ", " + 
	  convInstanceID +
	  "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + " self1, " + assetProtoTable + " self2" + 
	  ",\n" + filterClauses.getTables (recentRun) +
	  filterClauses.getUnitWhereSql (instanceOwner) + //, null, convInstanceID, null, null) +
	  filterClauses.getJoinsToCarrierInstance (recentRun) +
	  "\nand " + instanceProto + " = " + self1Proto + 
	  "\nand " + self1ParentProto + " = " + self2Proto + 
	  "\norder by " + self2Nomen;

	if (debug) 
	  System.out.println ("CarrierQuery.formFirstCargoTypeSql - \n" + sqlQuery);
	
	return sqlQuery;
  }

  protected String formSecondCargoTypeSql (FilterClauses filterClauses, int recentRun) {
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;

	String self1TypeID  = "self1" + "." + DGPSPConstants.COL_ALP_TYPEID;
	String self1Nomen   = "self1" + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
	String self1Proto   = "self1" + "." + DGPSPConstants.COL_PROTOTYPEID;
	String self1ParentProto  = "self1" + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;

	String instanceOwner  = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String instanceProto  = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
		
	String convInstanceTable    = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String convInstanceID       = convInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
		
	String sqlQuery = 
	  "select distinct " + self1TypeID + ", " + self1Nomen + ", " + self1Proto + ", " +
	  convInstanceID +
	  "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + " self1" +
	  ",\n" + 
	  filterClauses.getTables (recentRun) +
	  filterClauses.getUnitWhereSql (instanceOwner) + //, null, convInstanceID, null, null) +
	  filterClauses.getJoinsToCarrierInstance (recentRun) +
	  "\nand " + instanceProto + " = " + self1Proto + 
	  "\nand " + self1ParentProto + " is null" +
	  "\norder by " + self1Nomen;

	if (debug) 
	  System.out.println ("CarrierQuery.formSecondCargoTypeSql - \n" + sqlQuery);
	
	return sqlQuery;
  }

  protected String formCargoInstanceSql (FilterClauses filterClauses, int recentRun, boolean protoParentNull) {
	String convInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String convInstanceID   = convInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;

	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;

	String prototypeProto = assetProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String prototypeParentProto = assetProtoTable + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;
	String prototypeNomen = assetProtoTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;

	String instanceID        = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
	String instanceProto     = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String instanceName      = assetInstanceTable + "." + DGPSPConstants.COL_NAME;
	String instanceAggNumber = assetInstanceTable + "." + DGPSPConstants.COL_AGGREGATE;
	String instanceOwner     = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;

	String orgNames = HierarchyConstants.ORGNAMES_TABLE_NAME + "_" + recentRun;
	String orgNamesName = orgNames + "." + HierarchyConstants.COL_PRETTY_NAME;
	String orgNamesOrg  = orgNames + "." + HierarchyConstants.COL_ORGID;
	String protoColToUse = (protoParentNull) ? instanceProto : prototypeParentProto;
	String protoTest = (protoParentNull) ? " is null " : " is not null" ;

	String sqlQuery = 
	  "select distinct " + instanceID + ", " + protoColToUse + ", " + instanceName + "," + instanceAggNumber + "," +
   	    orgNamesName + ", " + convInstanceID + ", " + prototypeNomen +
	  "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + ", " + orgNames + ", " +
	  filterClauses.getTables   (recentRun) + "\n" +
	  filterClauses.getUnitWhereSql (instanceOwner) + //, null, convInstanceID, null, null) +
	  filterClauses.getJoinsToCarrierInstance (recentRun) +
	  "\nand " + instanceProto + " = " + prototypeProto +
	  "\nand " + orgNamesOrg + " = " + instanceOwner +
	  "\nand " + prototypeParentProto + protoTest +
	  "\norder by " + orgNamesName + ", " + instanceName;

	if (debug) 
	  System.out.println ("CarrierQuery.formCargoInstanceSql -\n" + sqlQuery);
	
	return sqlQuery;
  }
}
