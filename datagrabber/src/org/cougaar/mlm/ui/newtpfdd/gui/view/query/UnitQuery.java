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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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
import org.cougaar.mlm.ui.grabber.derived.PrepareDerivedTables;

public class UnitQuery extends SqlQuery {
    public static final int DEPTH_BY = 1;
    public static final int DEPTH_TYPE = 2;
    public static final int DEPTH_ITIN = 3;
    public static final int DEPTH_LEAF = 4;
    
  protected FilterClauses filterClauses;
  protected DatabaseRun run;
  protected boolean sortByName = false;
  
    private int treeDepth = DEPTH_LEAF;

  boolean detaildebug = false;
  
  boolean debug = 
      	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.UnitQuery.debug", 
    						   "false"));
  boolean showSqlTime = 
	"true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.UnitQuery.showSqlTime", 
									   "false"));

  public UnitQuery (DatabaseRun run, FilterClauses clauses) {
	filterClauses = clauses;
	sortByName = filterClauses.getSortByName();
	this.run = run;
  }
  
  public int getTreeDepth() { return treeDepth; };
  public void setTreeDepth(int treeDepth) { this.treeDepth = treeDepth; };

  public QueryResponse getResponse (Connection connection) {
	QueryResponse response = new QueryResponse ();
	
	// first figure out which run to use
	//	Date then = new Date();

	//	int recentRun = getRecentRun (connection);
	int recentRun = run.getRunID ();
	String unitID = (String) filterClauses.getUnitDBUIDs().iterator ().next();

	Tree carrierTree = new Tree ();
	UIDGenerator generator = carrierTree.getGenerator ();
	Node byCarrier = new ByCarrier (generator, unitID);
	carrierTree.setRoot (byCarrier);

	// cargo tree-------------------------------
	Tree cargoTree = buildByCargoTree (connection, filterClauses, unitID, recentRun);

	/*
	if (showSqlTime) {
	  Date now = new Date();
	  long diff = now.getTime()-then.getTime();
	  long secdiff = diff/1000l;
	  System.out.println ("UnitQuery.getResponse - built tree in " + diff+" msecs.");
	}
	*/

	response.addTree (cargoTree);
	response.addTree (carrierTree); // just the ByCarrier node, expanded in CarrierQuery

	if (debug) {
	  System.out.println ("cargo tree");
	  //	  cargoTree.show ();
	}
	
	return response;
  }

  /** build by cargo tree */
  protected Tree buildByCargoTree (Connection connection,
				   FilterClauses filterClauses, String unitID, int recentRun) {
	Tree cargoTree = new Tree ();
	UIDGenerator generator = cargoTree.getGenerator ();
	Node byCargo   = new ByCargo (generator, unitID);
	byCargo.setWasQueried (true);
	cargoTree.setRoot (byCargo);
	Map protoToCargoType = new HashMap ();
	Map protoToName = new HashMap ();
	ResultSet rs=null;

	long time;

	//CargoType:
	time=System.currentTimeMillis();
	if(run.hasCargoTypeTable()){
	  rs = getResultSet(connection, formFastCargoTypeSql(filterClauses,recentRun));
	}else{
	  rs = getResultSet(connection, formFirstCargoTypeSql (filterClauses, recentRun));
	}
	SortedMap nameToNode = buildCargoProtosFromResult (rs, generator, protoToCargoType, protoToName);

	if(!run.hasCargoTypeTable()){
	  rs = getResultSet(connection, formSecondCargoTypeSql (filterClauses, recentRun));
	  SortedMap nameToNode2 = buildCargoProtosFromResult (rs, generator, protoToCargoType, protoToName);  
	  nameToNode.putAll (nameToNode2);
	}

	buildCargoProtoTree (protoToCargoType, protoToName, cargoTree, generator);

	if(showSqlTime){
	  System.out.println((run.hasCargoTypeTable()?"Fast ":"")+"CargoType query took: "+
			     (System.currentTimeMillis()-time));
	}

	//CargoInstance:
	// attach instances
	Map instanceToNode = new HashMap ();
	
	time=System.currentTimeMillis();
	if(run.hasCargoInstanceTable()){

	  rs = getResultSet(connection, formFastCargoInstanceSql(filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formFirstCargoInstanceSql (filterClauses, recentRun));
	  attachInstancesFromResult (rs, generator, protoToCargoType, cargoTree, instanceToNode);
	  
	  rs = getResultSet(connection, formSecondCargoInstanceSql (filterClauses, recentRun));
	}
	attachInstancesFromResult (rs, generator, protoToCargoType, cargoTree, instanceToNode);
	if(showSqlTime){
	  System.out.println((run.hasCargoInstanceTable()?"Fast ":"")+"CargoInstance query took: "+
			     (System.currentTimeMillis()-time));
	}

	//CargoLeg:

	time=System.currentTimeMillis();
	if(run.hasCargoLegTable()){
	  rs = getResultSet(connection, formFastCargoLegSql(filterClauses, recentRun));
	}else{
	  rs = getResultSet(connection, formCargoLegSql(filterClauses, recentRun));
	}
	if(showSqlTime){
	  System.out.println((run.hasCargoLegTable()?"Fast ":"")+"CargoLeg query took: "+
			     (System.currentTimeMillis()-time));
	}
	time=System.currentTimeMillis();
	attachLegsFromResult (rs, generator, instanceToNode, cargoTree);
	if(showSqlTime){
	  System.out.println("Attaching cargo legs took: "+
			     (System.currentTimeMillis()-time));
	}
	
	return cargoTree;
  }

    protected class NodeComparator implements Comparator {
	private Map m;
	NodeComparator(Map protoToName) {
	    if (protoToName == null) 
		System.out.println("Got null hashmap in Comparator constructor.");
	    this.m = protoToName;
	}
	public int compare(Object o1, Object o2) {
	    String s1 = (String) m.get(o1);
	    String s2 = (String) m.get(o2);
	    return (s1.compareTo(s2));
	}

	public boolean equals(Object o1, Object o2) {
	  return (this.compare(o1, o2) == 0);
	}
    }

  protected void buildCargoProtoTree (Map protoToNode, Map protoToName, Tree tree, UIDGenerator generator) {

	List al = new ArrayList(protoToNode.keySet());
	Collections.sort(al, new NodeComparator(protoToName));

	for (Iterator iter = al.iterator (); iter.hasNext(); ) {
	  Node node = (Node) protoToNode.get(iter.next());
	  if (debug) {
	      System.out.println ("buildCargoProtoTree - adding " + node);
	      System.out.println("Called addNode in buildCargoProtoTree");
	  }

	  tree.addNode (tree.getRoot().getUID(), node);
	}

	//	if (debug)
	//  tree.show ();
  }
  
  
  protected SortedMap buildCargoProtosFromResult (ResultSet rs, UIDGenerator generator, Map protoToNode, Map protoToName) {
	SortedMap nameToNode = new TreeMap ();
	
	try{
	  int total = 0;
	  while(rs.next()){
		String type  = rs.getString (1);
		String nomen = rs.getString (2);
		String proto = rs.getString (3);
		boolean isLowFi = rs.getString (4).charAt(0)=='t';
		
		Node typeNode = createCargoType (generator, proto, type, nomen, isLowFi);

		nameToNode.put (nomen, typeNode);
		protoToNode.put (proto, typeNode);
		protoToName.put (proto, nomen);
		//		protoToNode.put (parentProto, typeNode);
		//		if (debug)
		//		  System.out.println ("UnitQuery.buildCargoProtosFromResult - mapping proto " + proto + "->" + 
		//							  typeNode);
		total++;
	  }
	  if (debug)
		System.out.println ("UnitQuery.buildCargoProtosFromResult - got " + total + 
							" cargo types");
	} catch (SQLException e) {
	  System.out.println ("UnitQuery.buildCargoProtosFromResult - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("UnitQuery.buildCargoProtosFromResult - " +
							  "closing result set, got sql error : " + e); 
		}
	  }
	}
	
	return nameToNode;
  }

  protected void attachInstancesFromResult (ResultSet rs, UIDGenerator generator, 
					    Map protoToNode, Tree cargoTree, Map instanceToNode) {
	try{
	  int total = 0;
	  while(rs.next()){
		String id        = rs.getString (1);
		String proto     = rs.getString (2);
		String name      = rs.getString (3);
		String aggnumber = rs.getString (4);
		String unitName  = rs.getString (5);
		String protoNomen = rs.getString (6);
		
		if (instanceToNode.get (id) != null) // protect against sql bug (seems to be sql bug)
		  continue;
		
		Node instanceNode = createCargoInstance (generator, id, name, aggnumber, unitName, protoNomen);
		Node protoNode = (Node)protoToNode.get(proto);

		if (protoNode == null)
		  System.out.println ("UnitQuery.attachInstancesFromResult - no proto node for : " + proto);
		else {
		  //		  if (debug)
		  //			System.out.println ("UnitQuery.attachInstancesFromResult - parent proto " + protoNode);
		  cargoTree.addNode (protoNode.getUID(), instanceNode);
		}
		instanceToNode.put (id, instanceNode);
		
		total++;
	  }
	  if (debug)
		System.out.println ("UnitQuery.attachInstancesFromResult - got " + total + 
							" cargo instances.");
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

  protected void attachLegsFromResult (ResultSet rs, UIDGenerator generator, 
				       Map instanceToNode, Tree cargoTree) {
    try{
      int total = 0;
      String prevReadyAt = null;
      Map assetToLeg = new HashMap ();

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
	String nomen     = rs.getString (14);
	String bumperno  = rs.getString (15);

	boolean isLowFi   = rs.getString (DGPSPConstants.COL_IS_LOW_FIDELITY).equals("true");

	if(debug){
	  System.out.println ("UnitQuery.attachLegsFromResult - isLowFi was " + isLowFi + "/" + 
			      rs.getString (DGPSPConstants.COL_IS_LOW_FIDELITY));
	}

	if (legtype != DGPSPConstants.LEG_TYPE_POSITIONING && legtype != DGPSPConstants.LEG_TYPE_RETURNING) {
	  if (readyAt.equals (start) && prevReadyAt != null)
	    readyAt = prevReadyAt;
	  LegNode legNode = createLeg (generator, id, start, end, readyAt, startLoc, startName, endLoc, endName, convtype, 
				    legtype, nomen, bumperno);
		  
	  Node instanceNode = (Node)instanceToNode.get(assetid);
	  if (instanceNode == null){
	    if(debug){
	      //Currently the Leg query ignores Carrier aspects of FilterClauses (so we get complete tpfdd lines
	      //even when filtering for specific critereon.  This means that when filtering for Carriers, there may NOT 
	      //be instances for some of the legs, hence this is an expected, if ugly condition.
	      System.out.println ("UnitQuery.attachLegsFromResult - no instance node for : " + assetid);
	    }
	  }else {
	    if (debug && false)
	      System.out.println ("UnitQuery.attachInstancesFromResult - parent instance " + instanceNode);

	    if (isLowFi) {
	      LegNode currentLegNode = findLegNode(assetToLeg, assetid, startLoc, endLoc);
	      if (currentLegNode == null) {
		registerLegNode (assetToLeg, assetid, startLoc, endLoc, legNode);
		cargoTree.addNode (instanceNode.getUID(), legNode);
	      }
	      else
		rollUpLegNode (legNode, currentLegNode);
	    }
	    else 
	      cargoTree.addNode (instanceNode.getUID(), legNode);
	  }
	}
	else {
	  prevReadyAt = readyAt;
	}
      }
      if (debug) 
	System.out.println ("UnitQuery.attachLegsFromResult - total rows for legs " + total);
	  
    } catch (SQLException e) {
      System.out.println ("UnitQuery.attachLegsFromResult - SQLError : " + e);
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  System.out.println ("UnitQuery.attachLegsFromResult - " + 
			      "closing result set, got sql error : " + e); 
	}
      }
    }
  }

  /** 
   * Assumes that there will never be multiple, different paths 
   * for different subobjects of a level 2 original asset 
   **/
  protected void registerLegNode (Map assetToLeg, String assetid, String startLoc, String endLoc, Node currentLegNode) {
    Map startToLegNode = (Map) assetToLeg.get (assetid);

    if (startToLegNode == null) {
      assetToLeg.put     (assetid,  (startToLegNode = new HashMap ()));
    }
    else {
      Node possibleLegNode = (Node) startToLegNode.get (startLoc);
      if (possibleLegNode != null)
	System.err.println ("UnitQuery.registerLegNode - error, found existing leg for asset " + assetid + 
			    " starting at " + startLoc);
    }

    if (debug)
      System.out.println ("UnitQuery.registerLegNode - register " + assetid + " at " + startLoc + " with leg " + currentLegNode);

    startToLegNode.put (startLoc, currentLegNode);

    if (debug)
      System.out.println ("UnitQuery.registerLegNode - maps now : assetToLeg - " + assetToLeg);
  }

  protected LegNode findLegNode (Map assetToLeg, String assetid, String startLoc, String endLoc) {
    Map startToLegNode = (Map) assetToLeg.get (assetid);
    if (startToLegNode == null) {
      if (debug)
	System.out.println ("UnitQuery.findLegNode - for " + assetid + " no entry found.");
      return null;
    }

    LegNode found = (LegNode) startToLegNode.get (startLoc);
    if (debug) {
      System.out.println ("UnitQuery.findLegNode - for " + assetid + " at " + startLoc + " got leg " + found);
      if (found == null)
	System.out.println ("UnitQuery.findLegNode - maps now : assetToLeg - " + assetToLeg);
    }
    
    return found;
  }

  protected void rollUpLegNode (LegNode newLeg, LegNode currentLegNode) {
    if (debug)
      System.out.println ("UnitQuery.rollUpLegNode - current " + currentLegNode + " new " + newLeg);

    if (newLeg.getActualStart().getTime() < currentLegNode.getActualStart().getTime())
      currentLegNode.setActualStart(newLeg.getActualStart());
    if (newLeg.getActualEnd().getTime ()  > currentLegNode.getActualEnd().getTime ())
      currentLegNode.setActualEnd  (newLeg.getActualEnd ());

    Date newReadyAt  = newLeg.getReadyAt();
    Date newEarlyEnd = newLeg.getEarlyEnd();
    Date newLateEnd  = newLeg.getLateEnd();

    if ((newReadyAt != null) && (newReadyAt.getTime() < currentLegNode.getReadyAt().getTime()))
      currentLegNode.setReadyAt(newReadyAt);
    if ((newEarlyEnd != null) && (newEarlyEnd.getTime() < currentLegNode.getEarlyEnd().getTime()))
      currentLegNode.setEarlyEnd(newEarlyEnd);
    if ((newLateEnd != null) && (newLateEnd.getTime() < currentLegNode.getLateEnd().getTime()))
      currentLegNode.setLateEnd(newLateEnd);

    if (newLeg.getCarrierType().indexOf(currentLegNode.getCarrierType()) == -1)
      currentLegNode.setCarrierType(currentLegNode.getCarrierType() + ", " +
				    newLeg.getCarrierType());

    currentLegNode.setCarrierName(currentLegNode.getCarrierName() + ", " +
				  newLeg.getCarrierName());
  }

  protected void attachConveyancesFromResult (ResultSet rs, UIDGenerator generator, 
					      Map protoToNode, Tree tree) {
	try{
	  int total = 0;
	  while(rs.next()){
		String id        = rs.getString (1);
		String proto     = rs.getString (2);
		String name      = rs.getString (3);
		
		Node instanceNode = createCarrierInstance (generator, id, name);
		Node protoNode = (Node)protoToNode.get(proto);
		if (protoNode == null)
		  System.out.println ("UnitQuery.attachConveyancesFromResult - no proto node for : " + proto);
		else {
		  //		  if (debug)
		  //			System.out.println ("UnitQuery.attachConveyancesFromResult - parent proto " + protoNode);
		  tree.addNode (protoNode.getUID(), instanceNode);
		}
		total++;
	  }
	  if (debug) 
		System.out.println ("UnitQuery.attachConveyancesFromResult - " + 
							total + " conveyances");
	  
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

  protected void buildTypeTreeFromResult (ResultSet rs, UIDGenerator generator, Tree tree, Map typeToNode) {
	try{
	  int total =0;
	  while(rs.next()){
		String type  = rs.getString (1);
		String nomen = rs.getString (2);
		String proto = rs.getString (3);
		
		Node typeNode = createCarrierType (generator, proto, type, nomen);
		
		tree.addNode (tree.getRoot().getUID(), typeNode);
		typeToNode.put (proto, typeNode);
		//		if (debug)
		//		  System.out.println ("UnitQuery.buildTypeTreeFromResult - " + proto + "->" + typeNode);
		total++;
	  }

	  //	  if (debug)
	  //		tree.show ();
	  if (debug) 
		System.out.println ("UnitQuery.buildTypeTreeFromResult - " + 
							total + " conveyance types.");
	} catch (SQLException e) {
	  System.out.println ("UnitQuery.buildTypeTreeFromResult - SQLError : " + e);
	}finally{
	  if(rs!=null) {
		try { rs.close(); } catch (SQLException e){
		  System.out.println ("UnitQuery.buildTypeTreeFromResult - " + 
							  "closing result set, got sql error : " + e); 
		}
	  }
	}
  }

  protected Node createCarrierType (UIDGenerator generator, String proto, String type, String nomen) {
	CarrierType carrierType = new CarrierType (generator, proto);
	carrierType.setDisplayName (nomen);
	carrierType.setCarrierName (nomen);
	carrierType.setCarrierType (type);
	//	carrierType.setCarrierTypeDBID (proto);
	return carrierType;
  }

  protected Node createCargoType (UIDGenerator generator, String proto, String type, String nomen, boolean isLowFi) {
	CargoType cargoType = new CargoType (generator, proto);
	cargoType.setDisplayName (nomen);
	cargoType.setCargoName (nomen);
	cargoType.setCargoType (type);
	cargoType.setLowFi (isLowFi);
	return cargoType;
  }

  protected Node createCargoInstance (UIDGenerator generator, String id, String name, String aggnumber,
				      String unitName, String protoNomen,
				      double weight, double width, double height, double depth, double area, double volume,
				      String alpType, String container) {
    CargoInstance cargo = 
      (CargoInstance) createCargoInstance (generator, id, name, aggnumber, unitName, protoNomen, weight, width, height, depth, area, volume);

    cargo.setALPType (alpType);
    cargo.setContainer (container);

    return cargo;
  }

  protected Node createCargoInstance (UIDGenerator generator, String id, String name, String aggnumber,
				      String unitName, String protoNomen,
				      double weight, double width, double height, double depth, double area, double volume) {
    CargoInstance cargo = 
      (CargoInstance) createCargoInstance (generator, id, name, aggnumber, unitName, protoNomen);

    cargo.setWeight (weight);
    cargo.setWidth  (width);
    cargo.setHeight (height);
    cargo.setDepth  (depth);
    cargo.setArea   (area);
    cargo.setVolume (volume);

    return cargo;
  }

  protected Node createCargoInstance (UIDGenerator generator, String id, String name, String aggnumber,
				      String unitName, String protoNomen) {
    long quantity = Long.parseLong (aggnumber);
	
    CargoInstance cargo = new CargoInstance (generator, id);
    String trimName = trim(name);
    if (trimName.length () == 0) {// for aggregates
      if (quantity > 1)
	trimName = "Group of " + aggnumber + " " + protoNomen;
      else
	trimName = protoNomen;
    }
	
    cargo.setDisplayName (trimName);
    cargo.setNomen       (protoNomen);
    cargo.setLongName    (protoNomen + " : " + trimName);
    cargo.setUnitName    (unitName);
    cargo.setQuantity    (quantity);
    return cargo;
  }

  protected Node createCarrierInstance (UIDGenerator generator, String id, String name) {
	CarrierInstance carrier = new CarrierInstance (generator, id);
	carrier.setDisplayName (name);
	return carrier;
  }

  protected LegNode createLeg (UIDGenerator generator, String id, String start, String end, String readyAt,
							String startLoc, String startName, String endLoc, String endName, int convType,
							int legType, String carrierNomen, String bumperNo) {
	LegNode leg = new LegNode (generator, id);
	leg.setDisplayName ("");
	leg.setActualStart (getDate(start));
	leg.setActualEnd   (getDate(end));
	leg.setReadyAt     (getDate(readyAt));
	leg.setFromCode (startLoc);
	leg.setFrom (startName);
	leg.setToCode (endLoc);
	leg.setTo (endName);
	leg.setModeFromConveyType (convType);
	leg.setLegType (legType);
	leg.setCarrierType (carrierNomen);
	leg.setCarrierName (bumperNo);

	if (detaildebug)
	  System.out.println ("UnitQuery.createLeg " + carrierNomen + " " + bumperNo + " leg is " + leg + 
						  " ready " + readyAt + " start " + start);
	
	return leg;
  }

  protected String formFastCargoTypeSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_TYPE,recentRun);
    
    String typeID = DGPSPConstants.COL_ALP_TYPEID;
    String nomen = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String proto = PrepareDerivedTables.COL_INST_PROTOTYPEID;
    String isLowFi = DGPSPConstants.COL_IS_LOW_FIDELITY;
    
    String instanceOwner = DGPSPConstants.COL_OWNER;
    
    String sqlQuery = 
      "select distinct " + typeID + ", " + nomen + ", " + proto + "," + isLowFi +
      "\nfrom " + derivedTable +
      filterClauses.getUnitWhereSql (instanceOwner) +
      "\norder by " + nomen;
    
    if (debug) 
      System.out.println ("UnitQuery.formFastCargoTypeSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formFirstCargoTypeSql (FilterClauses filterClauses, int recentRun) {
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;

	String self2TypeID  = "self2" + "." + DGPSPConstants.COL_ALP_TYPEID;
	String self2Nomen   = "self2" + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
	String self2Proto   = "self2" + "." + DGPSPConstants.COL_PROTOTYPEID;
	String self2ParentProto  = "self2" + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;

	String instanceOwner  = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String instanceProto  = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String self1Proto     = "self1" + "." + DGPSPConstants.COL_PROTOTYPEID;
	String self1ParentProto  = "self1" + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;

	String sqlQuery = 
	  "select distinct " + self2TypeID + ", " + self2Nomen + ", " + self2Proto + 
	  "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + " self1, " + assetProtoTable + " self2" + 
	  filterClauses.getUnitWhereSql (instanceOwner) +
	  "\nand " + instanceProto + " = " + self1Proto + 
	  "\nand " + self1ParentProto + " = " + self2Proto + 
	  "\norder by " + self2Nomen;

	if (debug) 
	  System.out.println ("UnitQuery.formFirstCargoTypeSql - \n" + sqlQuery);
	
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
		
	String sqlQuery = 
	  "select distinct " + self1TypeID + ", " + self1Nomen + ", " + self1Proto + 
	  "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + " self1" +
	  filterClauses.getUnitWhereSql (instanceOwner) +
	  "\nand " + instanceProto + " = " + self1Proto + 
	  "\nand " + self1ParentProto + " is null" +
	  "\norder by " + self1Nomen;

	if (debug) 
	  System.out.println ("UnitQuery.formSecondCargoTypeSql - \n" + sqlQuery);
	
	return sqlQuery;
  }


  protected String formFastCargoInstanceSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_INSTANCE,recentRun);

    String instanceID        = DGPSPConstants.COL_ASSETID;
    String instanceProto     = DGPSPConstants.COL_PROTOTYPEID;
    String instanceName      = DGPSPConstants.COL_NAME;
    String instanceAggNumber = DGPSPConstants.COL_AGGREGATE;
    String orgNamesName = HierarchyConstants.COL_PRETTY_NAME;
    String prototypeNomen = DGPSPConstants.COL_ALP_NOMENCLATURE;

    String weight = DGPSPConstants.COL_WEIGHT;
    String width  = DGPSPConstants.COL_WIDTH;
    String height = DGPSPConstants.COL_HEIGHT;
    String depth  = DGPSPConstants.COL_DEPTH;
    String area   = DGPSPConstants.COL_AREA;
    String volume = DGPSPConstants.COL_VOLUME;

    String instanceOwner     = DGPSPConstants.COL_OWNER;
    String cLegStart   = DGPSPConstants.COL_STARTTIME;
	
    String sqlQuery = 
      "select distinct " + instanceID + ", " + instanceProto + ", " + instanceName + ", " + instanceAggNumber + ", " +
      orgNamesName + ", " + prototypeNomen +", " + weight +", " + width +", " + height +", " + depth +", " + area +", " + volume +
      "\nfrom " + derivedTable +
      filterClauses.getUnitWhereSql (instanceOwner) +
      "\norder by " + orgNamesName + ", " + ((sortByName) ? instanceName + ", " : "") + prototypeNomen + ", " + cLegStart;
    
    if (debug) 
      System.out.println("UnitQuery.formFastCargoInstanceSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formFirstCargoInstanceSql (FilterClauses filterClauses, int recentRun) {
    String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
    String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;
    String cccDimTable = DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE + "_" + recentRun;

    String prototypeProto = assetProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String cccDimProto    = cccDimTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String prototypeParentProto = assetProtoTable + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;
    String prototypeNomen = assetProtoTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;

    String weight = DGPSPConstants.COL_WEIGHT;
    String width  = DGPSPConstants.COL_WIDTH;
    String height = DGPSPConstants.COL_HEIGHT;
    String depth  = DGPSPConstants.COL_DEPTH;
    String area   = DGPSPConstants.COL_AREA;
    String volume = DGPSPConstants.COL_VOLUME;

    String instanceID        = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceProto     = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String instanceName      = assetInstanceTable + "." + DGPSPConstants.COL_NAME;
    String instanceAggNumber = assetInstanceTable + "." + DGPSPConstants.COL_AGGREGATE;
    String instanceOwner     = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;

    String orgNames = HierarchyConstants.ORGNAMES_TABLE_NAME + "_" + recentRun;
    String orgNamesName = orgNames + "." + HierarchyConstants.COL_PRETTY_NAME;
    String orgNamesOrg  = orgNames + "." + HierarchyConstants.COL_ORGID;

    String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
    String itinLeg     = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
    String itinID      = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
    String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
    String cLegID      = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
    String cLegStart   = conveyedLegTable + "." + DGPSPConstants.COL_STARTTIME;
    String wherePrefix = 
      filterClauses.getUnitWhereSql (instanceOwner);
    if (wherePrefix.equals(""))
      wherePrefix = "\nwhere ";
    else
      wherePrefix += "\nand ";
	
    String sqlQuery = 
      "select distinct " + instanceID + ", " + instanceProto + ", " + instanceName + ", " + instanceAggNumber + ", " +
      orgNamesName + ", " + prototypeNomen +", " + weight +", " + width +", " + height +", " + depth +", " + area +", " + volume +
      "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + ", " + orgNames + ", " + assetItineraryTable + ", " + cccDimTable +", "+
      conveyedLegTable + 
      wherePrefix +
      instanceProto + " = " + prototypeProto +
      "\nand " + orgNamesOrg + " = " + instanceOwner +
      "\nand " + instanceID + " = " + itinID + 
      "\nand " + itinLeg + " = " + cLegID +
      "\nand " + cccDimProto + " = " + prototypeProto +
      "\nand " + prototypeParentProto + " is null " +
      "\norder by " + orgNamesName + ", " + ((sortByName) ? instanceName + ", " : "") + prototypeNomen + ", " + cLegStart;

    if (debug) 
      System.out.println ("UnitQuery.formFirstCargoInstanceSql - \n" + sqlQuery);
	
    return sqlQuery;
  }

  protected String formSecondCargoInstanceSql (FilterClauses filterClauses, int recentRun) {
    String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
    String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;
    String cccDimTable = DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE + "_" + recentRun;

    String prototypeProto = assetProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String cccDimProto    = cccDimTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String prototypeParentProto = assetProtoTable + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;
    String prototypeNomen = assetProtoTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;

    String weight = DGPSPConstants.COL_WEIGHT;
    String width  = DGPSPConstants.COL_WIDTH;
    String height = DGPSPConstants.COL_HEIGHT;
    String depth  = DGPSPConstants.COL_DEPTH;
    String area   = DGPSPConstants.COL_AREA;
    String volume = DGPSPConstants.COL_VOLUME;

    String instanceID        = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceProto     = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String instanceName      = assetInstanceTable + "." + DGPSPConstants.COL_NAME;
    String instanceAggNumber = assetInstanceTable + "." + DGPSPConstants.COL_AGGREGATE;
    String instanceOwner     = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	
    String orgNames = HierarchyConstants.ORGNAMES_TABLE_NAME + "_" + recentRun;
    String orgNamesName = orgNames + "." + HierarchyConstants.COL_PRETTY_NAME;
    String orgNamesOrg  = orgNames + "." + HierarchyConstants.COL_ORGID;

    String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
    String itinLeg     = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
    String itinID      = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
    String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
    String cLegID      = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
    String cLegStart   = conveyedLegTable + "." + DGPSPConstants.COL_STARTTIME;
    String wherePrefix = 
      filterClauses.getUnitWhereSql (instanceOwner);
    if (wherePrefix.equals(""))
      wherePrefix = "\nwhere ";
    else
      wherePrefix += "\nand ";
	
    String sqlQuery = 
      "select distinct " + instanceID + ", " + prototypeParentProto + ", " + instanceName + ", " + instanceAggNumber + ", " +
      orgNamesName + ", " + prototypeNomen +", " + weight +", " + width +", " + height +", " + depth +", " + area +", " + volume +
      "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + ", " + orgNames + ", " + assetItineraryTable + ", " +cccDimTable +", "+
      conveyedLegTable +
      wherePrefix +
      instanceProto + " = " + prototypeProto +
      "\nand " + orgNamesOrg + " = " + instanceOwner +
      "\nand " + instanceID + " = " + itinID + 
      "\nand " + itinLeg + " = " + cLegID +
      "\nand " + cccDimProto + " = " + prototypeProto +
      "\nand " + prototypeParentProto + " is not null " +
      "\norder by " + orgNamesName + ", " + ((sortByName) ? instanceName + ", " : "") + prototypeNomen + ", " + cLegStart;

    if (debug) 
      System.out.println ("UnitQuery.formSecondCargoInstanceSql - \n" + sqlQuery);
	
    return sqlQuery;
  }

  protected String formFastCargoLegSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_LEG,recentRun);
    
    String assetInstanceOwner = DGPSPConstants.COL_OWNER;
    String cLegID             = DGPSPConstants.COL_LEGID;
    String cLegStart          = DGPSPConstants.COL_STARTTIME;
    String cLegEnd            = DGPSPConstants.COL_ENDTIME;
    String cLegReadyAt        = DGPSPConstants.COL_READYAT;
    String l1geoloc           = PrepareDerivedTables.COL_START_GEOLOC;
    String l1name             = PrepareDerivedTables.COL_START_PRETTYNAME;
    String l2geoloc           = PrepareDerivedTables.COL_END_GEOLOC;
    String l2name             = PrepareDerivedTables.COL_END_PRETTYNAME;
    String cpConvType         = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String cLegType           = DGPSPConstants.COL_LEGTYPE;
    String assetInstanceID    = DGPSPConstants.COL_ASSETID;
    String ciConvID           = DGPSPConstants.COL_CONVEYANCEID;
    String instanceProto      = DGPSPConstants.COL_PROTOTYPEID;
    String cpNomen            = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String ciBumper           = DGPSPConstants.COL_BUMPERNO;
    String isLowFi            = DGPSPConstants.COL_IS_LOW_FIDELITY;

    String sqlQuery = 
      "select " + cLegID + ", " + cLegStart + ", " + cLegEnd + ", " + cLegReadyAt + ",\n" + 
      l1geoloc + ", " + l1name + ", " + l2geoloc + ", " + l2name + ",\n" + 
      cpConvType + ", " + cLegType + ", " + assetInstanceID + ",\n" + 
      ciConvID + ", " + instanceProto + ", " + cpNomen + ", " + ciBumper + ", " + isLowFi +
      "\nfrom " + derivedTable + " " +
      filterClauses.getUnitWhereSql (assetInstanceOwner) + 
      "\norder by " + assetInstanceID + ", " + cLegStart;
    
    if (debug) 
      System.out.println ("UnitQuery.formFastCargoLegSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formCargoLegSql (FilterClauses filterClauses, int recentRun) {
	String locTable = DGPSPConstants.LOCATIONS_TABLE + "_" + recentRun;
	String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
	String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;

	String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;
	String prototypeParentProto = assetProtoTable + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;

	String instanceProto     = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;

	String assetInstanceOwner = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
	String assetInstanceID    = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
	String itinID             = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
	String itinLeg            = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
	String cLegID      = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
	String cLegStartLoc= conveyedLegTable + "." + DGPSPConstants.COL_STARTLOC;
	String cLegEndLoc  = conveyedLegTable + "." + DGPSPConstants.COL_ENDLOC;
	String cLegStart   = conveyedLegTable + "." + DGPSPConstants.COL_STARTTIME;
	String cLegEnd     = conveyedLegTable + "." + DGPSPConstants.COL_ENDTIME;
	String cLegType    = conveyedLegTable + "." + DGPSPConstants.COL_LEGTYPE;
	String cLegConvID  = conveyedLegTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	String cLegReadyAt = conveyedLegTable + "." + DGPSPConstants.COL_READYAT;

	String l1id     = "l1" + "." + DGPSPConstants.COL_LOCID;
	String l1geoloc = "l1" + "." + DGPSPConstants.COL_GEOLOC;
	String l1name   = "l1" + "." + DGPSPConstants.COL_PRETTYNAME;

	String l2id     = "l2" + "." + DGPSPConstants.COL_LOCID;
	String l2geoloc = "l2" + "." + DGPSPConstants.COL_GEOLOC;
	String l2name   = "l2" + "." + DGPSPConstants.COL_PRETTYNAME;
	
	String ciPrototypeID = conveyanceInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String ciConvID      = conveyanceInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	String ciBumper      = conveyanceInstanceTable + "." + DGPSPConstants.COL_BUMPERNO;
	String cpPrototypeID = conveyancePrototypeTable + "." + DGPSPConstants.COL_PROTOTYPEID;
	String cpConvType    = conveyancePrototypeTable + "." + DGPSPConstants.COL_CONVEYANCE_TYPE;
	String cpNomen       = conveyancePrototypeTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
	String wherePrefix = 
	  filterClauses.getUnitWhereSql (assetInstanceOwner);
	if (wherePrefix.equals(""))
	  wherePrefix = "\nwhere ";
	else
	  wherePrefix += "\nand ";

	String sqlQuery = 
	  "select " + cLegID + ", " + cLegStart + ", " + cLegEnd + ", " + cLegReadyAt + ", " + l1geoloc + ", " + l1name +
	  ",\n" + l2geoloc + ", " + l2name + ", " + cpConvType + ", " + cLegType + ", " + assetInstanceID + 
	  ",\n" + ciConvID + ", " + instanceProto + ", " + cpNomen + ", " + ciBumper +
	  "\nfrom " + locTable + " l1, " + locTable + " l2, " + assetInstanceTable + ", " + assetItineraryTable + ",\n" +
	  conveyedLegTable + ", " + conveyanceInstanceTable + ", " + conveyancePrototypeTable + //", " + assetProtoTable +
	  wherePrefix + 
	  assetInstanceID + " = " + itinID + 
	  "\nand " + itinLeg + " = " + cLegID +
	  "\nand " + cLegStartLoc + " = " + l1id +
	  "\nand " + cLegEndLoc + " = " + l2id +
	  //  "\nand " + cLegType + " <> " + DGPSPConstants.LEG_TYPE_POSITIONING + 
	  //  "\nand " + cLegType + " <> " + DGPSPConstants.LEG_TYPE_RETURNING +
	  "\nand " + cLegConvID + " = " + ciConvID +
	  "\nand " + ciPrototypeID + " = " + cpPrototypeID +
	  //	  "\nand " + prototypeParentProto + " = " + instanceProto +
	  "\norder by " + assetInstanceID + ", " + cLegStart;

	if (debug) 
	  System.out.println ("UnitQuery.formCargoLegSql - \n" + sqlQuery);
	
	return sqlQuery;
  }
}
