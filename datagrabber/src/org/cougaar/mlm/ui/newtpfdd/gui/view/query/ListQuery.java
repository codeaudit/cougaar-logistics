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

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

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

import org.cougaar.mlm.ui.newtpfdd.gui.view.query.SortedQueryResponse;

public class ListQuery extends CarrierQuery {
    public static final int DEPTH_BY = 1;
    public static final int DEPTH_TYPE = 2;
    public static final int DEPTH_ITIN = 3;
    public static final int DEPTH_LEAF = 4;
    
    private String unitID = null;

  boolean debug = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.ListQuery.debug", 
									   "false"));
  boolean showSqlTime = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.ListQuery.showSqlTime", 
									   "false"));


  public ListQuery (DatabaseRun run, FilterClauses filterClauses) {
	super (run, filterClauses);
  }

  public QueryResponse getResponse (Connection connection) {
	ListQueryResponse response = new ListQueryResponse ();
	
	// first figure out which run to use
	//	int recentRun = getRecentRun (connection);
	int recentRun = run.getRunID ();

	String unitID = (String) filterClauses.getUnitDBUIDs().iterator ().next();

	Tree carrierTree = new Tree ();
	UIDGenerator generator = carrierTree.getGenerator ();
	Node byCarrier = new ByCarrier (generator, unitID);
	carrierTree.setRoot (byCarrier);

	long time;
	long totalTime=System.currentTimeMillis();

	//Carrier Type:

	Map protoToCarrierType = new HashMap ();
	time=System.currentTimeMillis();
	ResultSet rs;
	if(run.hasCarrierTypeTable()){
	  rs = getResultSet(connection, formFastCarrierTypeSql (filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formCarrierTypeSql(filterClauses, recentRun));
	}
	buildTypeTreeFromResult (rs, generator, carrierTree, protoToCarrierType);
	response.setCarrierTypeTree (carrierTree);
	if(showSqlTime){
	  System.out.println((run.hasCarrierTypeTable()?"Fast ":"")+"CarrierType query took: "+
			     (System.currentTimeMillis()-time));
	}

	// Carrier Instance:

	Tree carrierInstance = new Tree ();
	generator = carrierInstance.getGenerator ();
	byCarrier = new ByCarrier (generator, unitID);
	carrierInstance.setRoot (byCarrier);

	time=System.currentTimeMillis();
	if(run.hasCarrierInstanceTable()){
	  rs = getResultSet(connection, formFastCarrierInstanceSql(filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formCarrierInstanceSql (filterClauses, recentRun));
	}
	getConveyancesFromResult (rs, generator, carrierInstance);
	response.setCarrierInstanceTree (carrierInstance);
	if(showSqlTime){
	  System.out.println((run.hasCarrierInstanceTable()?"Fast ":"")+"CarrierInstance query took: "+
			     (System.currentTimeMillis()-time));
	}

	// cargo trees ----------------------------

	Tree cargoTypes = new Tree ();
	generator = cargoTypes.getGenerator ();
	Node cargoTypeRoot = new CargoType (generator, unitID); // could be anything...?
	cargoTypes.setRoot (cargoTypeRoot);

	Map protoToCargoType = new HashMap ();
	Map protoToName = new HashMap ();
	UnitQuery unitQuery = new UnitQuery (run, filterClauses);

	//cargo type:
	
	time=System.currentTimeMillis();
	if(run.hasCargoTypeTable()){
	  rs = getResultSet(connection, unitQuery.formFastCargoTypeSql(filterClauses, recentRun));
	  SortedMap nameToNode = unitQuery.buildCargoProtosFromResult(rs, generator, protoToCargoType, protoToName);
	  rs = getResultSet(connection, formManifestTypeSql(filterClauses, recentRun));
	  // what if collision in protoToX maps?  Does it matter?
	  SortedMap nameToNode2 = unitQuery.buildCargoProtosFromResult(rs, generator, protoToCargoType, protoToName);
	  nameToNode.putAll(nameToNode2);
	  unitQuery.buildCargoProtoTree (protoToCargoType, protoToName, cargoTypes, generator);
	}else{
	  rs = getResultSet(connection, unitQuery.formFirstCargoTypeSql (filterClauses, recentRun));
	  SortedMap nameToNode = unitQuery.buildCargoProtosFromResult (rs, generator, protoToCargoType, protoToName);
	  
	  rs = getResultSet(connection, unitQuery.formSecondCargoTypeSql (filterClauses, recentRun));
	  SortedMap nameToNode2 = unitQuery.buildCargoProtosFromResult (rs, generator, protoToCargoType, protoToName);
	  nameToNode.putAll (nameToNode2);
	  unitQuery.buildCargoProtoTree (protoToCargoType, protoToName, cargoTypes, generator);
	}
	response.setCargoTypeTree (cargoTypes);
	if(showSqlTime){
	  System.out.println((run.hasCargoTypeTable()?"Fast ":"")+"CargoType query took: "+
			     (System.currentTimeMillis()-time));
	}

	//cargo instances:

	Tree cargoInstances = new Tree ();
	generator = cargoInstances.getGenerator ();
	cargoTypeRoot = new CargoType (generator, unitID); // could be anything...?
	cargoInstances.setRoot (cargoTypeRoot);

	time=System.currentTimeMillis();
	rs = getResultSet(connection, unitQuery.formFirstCargoInstanceSql (filterClauses, recentRun));
	getInstancesFromResult (rs, generator, cargoInstances);

	rs = getResultSet(connection, unitQuery.formSecondCargoInstanceSql (filterClauses, recentRun));
	getInstancesFromResult (rs, generator, cargoInstances);

	rs = getResultSet(connection, formManifestInstanceSql (filterClauses, recentRun));
	getInstancesFromResult (rs, generator, cargoInstances);
	response.setCargoInstanceTree (cargoInstances);
	if(showSqlTime){
	  System.out.println((false?"Fast ":"")+"CargoInstance query took: "+
			     (System.currentTimeMillis()-time));
	}

	if(showSqlTime){
	  System.out.println("Total ListQuery took: "+
			     (System.currentTimeMillis()-totalTime));
	}

	if (debug) {
	  System.out.println ("ListQuery.getResponse - cargo tree :");
	  //	  cargoTree.show ();
	}
	
	return response;
  }
  
  protected void showTime (Date then, String label) {
	Date now = new Date();
	long diff = now.getTime()-then.getTime();
	System.out.println ("ListQuery.getResponse - " + label + " in " + diff+" msecs.");
  }

  protected void getConveyancesFromResult (ResultSet rs, UIDGenerator generator, Tree tree) {
	try{
	  while(rs.next()){
		String id        = rs.getString (1);
		String proto     = rs.getString (2);
		String name      = rs.getString (3);
		
		Node instanceNode = createCarrierInstance (generator, id, name);
		tree.addNode (tree.getRoot().getUID(), instanceNode);
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
		//		if (debug)
		//		  System.out.println ("ListQuery.buildTypeTreeFromResult - " + proto + "->" + typeNode);
		rows++;
	  }
	  if (debug)
		System.out.println ("ListQuery.buildTypeTreeFromResult - rows " + rows);

	  //	  if (debug)
	  //		tree.show ();
	} catch (SQLException e) {
	  System.out.println ("ListQuery.getResponse - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("ListQuery.getResponse - closing result set, got sql error : " + e); 
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

  protected void getInstancesFromResult (ResultSet rs, UIDGenerator generator, Tree cargoTree) {
    Set seenIDs = new HashSet(89);
	try{
	  while(rs.next()){
		String id        = rs.getString (1);
		String proto     = rs.getString (2);
		String name      = rs.getString (3);
		String aggnumber = rs.getString (4);
		String unitName  = rs.getString (5);
		
		if(seenIDs.contains(id)) // protect against sql bug (seems to be sql bug)
		  continue;

		Node instanceNode = createCargoInstance (generator, id, name, aggnumber, unitName);

		seenIDs.add(id);

		cargoTree.addNode (cargoTree.getRoot().getUID(), instanceNode);
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

  protected Node createCargoInstance (UIDGenerator generator, String id, String name, String aggnumber,
									  String unitName) {
	long quantity = Long.parseLong (aggnumber);
	
	CargoInstance cargo = new CargoInstance (generator, id);
	cargo.setDisplayName (name);
	cargo.setUnitName    (unitName);
	cargo.setQuantity    (quantity);
	return cargo;
  }

  protected String formManifestTypeSql (FilterClauses filterClauses, int recentRun) {
    String manifestTable = DGPSPConstants.MANIFEST_TABLE + "_" + recentRun;
    String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
    
    String typeID = DGPSPConstants.COL_ALP_TYPEID;
    String nomen  = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String instanceOwner = DGPSPConstants.COL_OWNER;
    String instanceID    = DGPSPConstants.COL_ASSETID;
    
    String sqlQuery = 
      "select distinct m." + typeID + ",m." + nomen + ",m." + typeID + 
      "\nfrom " + manifestTable + " m, " + assetInstanceTable + " a \n" +
      filterClauses.getUnitWhereSql (instanceOwner) +
      "\nand a." + instanceID + "=m." +instanceID +
      "\norder by " + typeID;
    
    if (debug) 
      System.out.println ("ListQuery.formManifestTypeSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formManifestInstanceSql (FilterClauses filterClauses, int recentRun) {
    String manifestTable = DGPSPConstants.MANIFEST_TABLE + "_" + recentRun;
    String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;

    String typeID = manifestTable + "." + DGPSPConstants.COL_ALP_TYPEID;
    String nomen  = manifestTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
    String weight = manifestTable + "." + DGPSPConstants.COL_WEIGHT;

    String manifestInstanceID = manifestTable + "." + DGPSPConstants.COL_MANIFEST_ITEM_ID;
    String manifestName      = manifestTable + "." + DGPSPConstants.COL_NAME;
    String instanceOwner     = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
    String manifestAssetID = manifestTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceAssetID = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;

    String orgNames = HierarchyConstants.ORGNAMES_TABLE_NAME + "_" + recentRun;
    String orgNamesName = orgNames + "." + HierarchyConstants.COL_PRETTY_NAME;
    String orgNamesOrg  = orgNames + "." + HierarchyConstants.COL_ORGID;

    String sqlQuery = 
      "select " + manifestInstanceID + ", " + typeID + ", " + manifestName + ", \"1\",\n" +
      orgNamesName + ", " + nomen + ", " + weight +", " + "\"1\"" +", " + "\"1\"" +", " + "\"1\"" +
      "\nfrom "  + assetInstanceTable + ", " + manifestTable + ", " + orgNames + "\n"+
      filterClauses.getUnitWhereSql (instanceOwner) +
      "\nand " + orgNamesOrg + " = " + instanceOwner +
      "\nand " + instanceAssetID + "=" +manifestAssetID +
      "\norder by " + orgNamesName + ((sortByName) ? ", " + nomen + ", " : "");

    if (debug) 
      System.out.println ("ListQuery.formManifestInstanceSql - \n" + sqlQuery);
	
    return sqlQuery;
  }
}
