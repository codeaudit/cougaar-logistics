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
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Collections;

import org.cougaar.mlm.ui.grabber.connect.HierarchyConstants;
import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.grabber.derived.PrepareDerivedTables;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

import org.cougaar.mlm.ui.newtpfdd.gui.view.UIDGenerator;
import org.cougaar.mlm.ui.newtpfdd.gui.view.Tree;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LegNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.AggregateCargoType;

public class FilterQuery extends TPFDDQuery {
  boolean debug = 
      "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.debug", 
  				       "false"));
  boolean showSqlTime = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.showSqlTime", 
				       "false"));
  boolean matchPathEnds = 
    "true".equals (System.getProperty ("org.cougaar.mlm.ui.newtpfdd.gui.view.FilterQuery.matchPathEnds", 
				       "true"));

  boolean aggregateByUnit = true;
  
  public FilterQuery (DatabaseRun run, FilterClauses filterClauses) {
    super (run, filterClauses);
    aggregateByUnit = filterClauses.getByUnit();
  }
  
  /** nameToNode is just so that we can have a sorted list of types 
   *  Since we have to merge the results of both cargo type queries into one sorted list 
   **/
  protected void attachInstances (Connection connection, FilterClauses filterClauses, int recentRun, Tree cargoTree,
				  Map instanceToNode) {

    UIDGenerator generator = cargoTree.getGenerator();
    long time;
    time=System.currentTimeMillis();
    Map protoToName = new HashMap();

    if (filterClauses.getRollup ()) {
      if(run.hasCargoTypeTable()){
      	ResultSet rs = getResultSet(connection, formFastCargoTypeSql(filterClauses, recentRun));
	SortedMap nameToNode = buildRollupCargoProtos (rs, generator, instanceToNode, protoToName);
	// buildCargoProtoTree (instanceToNode, protoToName, cargoTree, generator);
	rs = getResultSet(connection, formManifestTypeSql(filterClauses, recentRun));
	SortedMap nameToNode2 = buildRollupCargoProtos (rs, generator, instanceToNode, protoToName);
	// what if collision in protoToX maps?  Does it matter?
	nameToNode.putAll(nameToNode2);
      }else{
	ResultSet rs = getResultSet(connection, formFirstCargoTypeSql (filterClauses, recentRun));
	SortedMap nameToNode = buildRollupCargoProtos (rs, generator, instanceToNode, protoToName);
	
	rs = getResultSet(connection, formSecondCargoTypeSql (filterClauses, recentRun));
	SortedMap nameToNode2 = buildRollupCargoProtos (rs, generator, instanceToNode, protoToName);
	nameToNode.putAll (nameToNode2);
	// buildCargoProtoTree (instanceToNode, protoToName, cargoTree, generator);
      }
      if(showSqlTime){
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (run.hasCargoTypeTable()?"Fast ":"")+"FilterCargoType query took: "+
			   (System.currentTimeMillis()-time));
      }
    }
    else {
      ResultSet rs = getResultSet(connection, formCargoInstanceSql (filterClauses, recentRun, false));
      attachInstancesFromResult (rs, cargoTree.getGenerator(), cargoTree, instanceToNode);

      rs = getResultSet(connection, formCargoInstanceSql (filterClauses, recentRun, true));
      attachInstancesFromResult (rs, cargoTree.getGenerator(), cargoTree, instanceToNode);

      rs = getResultSet(connection, formManifestInstanceSql (filterClauses, recentRun, true));
      attachManifestInstancesFromResult (rs, cargoTree.getGenerator(), cargoTree, instanceToNode);

      if(showSqlTime){
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (false?"Fast ":"")+"FilterCargoInstance query took: "+
			   (System.currentTimeMillis()-time));
      }
    }
  }

  protected void attachLegs (Connection connection, FilterClauses filterClauses, int recentRun, Tree cargoTree,
			     Map instanceToNode) {
    FilterClauses myFC=new FilterClauses(filterClauses);
//     myFC.setCarrierTypes(new ArrayList());
//     myFC.setCarrierInstances(new ArrayList());

    long time;
    time=System.currentTimeMillis();

    if(run.hasCargoLegTable()){
      ResultSet rs = getResultSet(connection, formFastCargoLegSql(myFC, recentRun));
      if (myFC.getRollup ()) {
	attachRollupLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
      } else {
	attachLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
    }

      rs = getResultSet(connection, formManifestLegSql(myFC, recentRun));
      if (myFC.getRollup ()) {
	attachRollupLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
      } else {
	attachLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
    }
      
    }else{
      ResultSet rs = getResultSet(connection, formCargoLegSql (myFC, recentRun, false));
      if (myFC.getRollup ()) {
	attachRollupLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
      } else {
	attachLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
    }

      rs = getResultSet(connection, formCargoLegSql (myFC, recentRun, true));
      if (myFC.getRollup ()) {
	attachRollupLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
      } else {
	attachLegsFromResult (rs, cargoTree.getGenerator(), instanceToNode, cargoTree);
    }
    }
    if(showSqlTime){
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, (run.hasCargoLegTable()?"Fast ":"")+"FilterCargoLeg query took: "+
			 (System.currentTimeMillis()-time));
    }
  }

  /** 
   * protoToNode - creates aggregate cargo nodes based on SQL results.  Used to map a unique key to the aggType node.  
   * This allows the leg nodes to be attached to the aggType node with the protoToNode key.
   */
  protected SortedMap buildRollupCargoProtos (ResultSet rs, UIDGenerator generator, Map protoToNode, Map protoToName) {
    SortedMap nameToNode = new TreeMap ();

    try{
      while(rs.next()){
	String type  = rs.getString (1);
	String nomen = rs.getString (2);
	String proto = rs.getString (3);
	int aggNumber = rs.getInt (4);
	String owner = rs.getString (5);
	double weight    = rs.getDouble (7);
	double width     = rs.getDouble (8);
	double height    = rs.getDouble (9);
	double depth     = rs.getDouble (10);
	double area      = rs.getDouble (11);
	double volume    = rs.getDouble (12);

	String nameKey = nomen;
	if (aggregateByUnit)
	  nameKey = owner + nomen;

	String key = proto;		
	if (aggregateByUnit)
	  key = owner + proto; // the key is unit+proto

	AggregateCargoType aggTypeNode = (AggregateCargoType)protoToNode.get(key);
	if(aggTypeNode == null){
	  aggTypeNode = createCargoType (generator, proto, type, nomen, aggNumber, aggNumber, owner,
					 weight, width, height, depth, area, volume);
	  nameToNode.put  (nameKey, aggTypeNode);
	  protoToNode.put (key, aggTypeNode);
	  protoToName.put (key, nameKey);
	  
	  if (debug)
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.buildRollupCargoProtos - mapping key " + key + " to " + aggTypeNode);
	}else{
	  aggTypeNode.incrementTotalAggNumber(aggNumber);
	}
      }
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.buildRollupCargoProtos - SQLError : " + e);
      e.printStackTrace();
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.buildRollupCargoProtos - " +
			      "closing result set, got sql error : " + e); 
	}
      }
    }
    return nameToNode;
  }

  protected void attachRollupLegsFromResult (ResultSet rs, UIDGenerator generator, 
					     Map prototypeToNode, Tree cargoTree) {
    Map protoToPaths = new HashMap();

    try{
      int total = 0;
      boolean firstResult = true;
      String prevReadyAt = null;
      String prevAssetID = null;
      int prevAggNumber = 0;
      List prevPathsForProto = null;
      Path currentPath = null;
	  
      // For all legs
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
	String instanceProto  = rs.getString (13);
	String nomen     = rs.getString (14);
	String bumperno  = rs.getString (15);
	String owner     = rs.getString (16);
	int    aggNumber = rs.getInt (17);
	String protoKey  = (aggregateByUnit) ? (owner + instanceProto) : instanceProto;

	// Deal with implied legs POSITIONING and RETURNING
	if (legtype == DGPSPConstants.LEG_TYPE_POSITIONING || legtype == DGPSPConstants.LEG_TYPE_RETURNING) {
	  prevReadyAt = readyAt;
	  continue;
	} 
	// There may be no positioning or returning legs, which is the only way prevReadyAt gets set
	// What is the purpose of prevReadyAt, anyway?  I wish I could remember... (Gordon 08-17-01)
	else if (!firstResult && readyAt.equals (start) && (prevReadyAt != null)) {
	    if (debug) {
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.attachRollupLegsFromResult - setting ready At = prevReadyAt = " + prevReadyAt +
				    " first result " + firstResult + " and ready at " + readyAt + " = " +  start);
	    }
	  readyAt = prevReadyAt;
	}

	// if there is no path list for prototype, create it
	List pathsForProto = (List) protoToPaths.get (protoKey);
	if (pathsForProto == null) {
	    pathsForProto = new ArrayList ();
	    protoToPaths.put (protoKey, pathsForProto);
	}
	
	// This result is the start of a new path.  
	// Process the old path and create a new path.  
	if (!assetid.equals (prevAssetID)) {
	  if (!firstResult) {
	    // Set the aggregation number
	    currentPath.setAggNumber(prevAggNumber);

	    // if we could not combine this path with a known one, then it's a new one to add
	    if (!couldCombine (prevPathsForProto, currentPath)) 
	      prevPathsForProto.add (currentPath);
	  }

	  // begin a new path
	  currentPath = new Path ();
	}

	// Create the leg node and add it to the current path
	LegNode legNode = 
	  (LegNode) createLeg (generator, id, start, end, readyAt, startLoc, startName, endLoc, endName, 
			       convtype, legtype, nomen, bumperno);
	currentPath.addLeg (legNode);

	// Update prev variables for next iteration
	prevAssetID = assetid;
	prevAggNumber = aggNumber;
	prevPathsForProto = pathsForProto;
	firstResult = false;

      } // end while

      // If no legs found (i.e. empty result set), don't do anything
      if (total == 0)
	return;
	
      // Final fence-posting code: process the last path
      currentPath.setAggNumber(prevAggNumber);
      if (!couldCombine (prevPathsForProto, currentPath)) 
	prevPathsForProto.add (currentPath);
  
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.attachRollupLegsFromResult - SQLError : " + e);
      e.printStackTrace();
    } finally {
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.attachRollupLegsFromResult - " +
			      "closing result set, got sql error : " + e); 
	}
      }
    }

    //Map protoToSinglePath = combinePaths (protoToPaths);

    // I don't think this does anything . . . 
    // Map protoToFewPath = combinePathsAsNecessary (protoToPaths);
    Map protoToFewPath = protoToPaths;

    // Create an array list to sort the prototypes by name
    Map protoToName = new HashMap();
    for (Iterator iter = prototypeToNode.keySet().iterator(); iter.hasNext();) {
      String proto = (String) iter.next();
      AggregateCargoType prototypeNode = (AggregateCargoType)prototypeToNode.get(proto);
      String name = prototypeNode.getCargoName();
      protoToName.put(proto, name);
    }

    List al = new ArrayList(prototypeToNode.keySet());

    addLegsToTree (al, protoToName, prototypeToNode, protoToFewPath, generator, cargoTree);
  }

  protected void addLegsToTree (List prototypes, Map protoToName, Map prototypeToNode, Map protoToFewPath,
				UIDGenerator generator, Tree cargoTree) {
    Collections.sort(prototypes, new NodeComparator(protoToName)); 

    // Walk the sorted list of prototypes
    for (Iterator iter = prototypes.iterator(); iter.hasNext();) {
      String proto = (String) iter.next();
      AggregateCargoType prototypeNode = (AggregateCargoType)prototypeToNode.get(proto);
      if (prototypeNode == null){
	if(debug){
	  //Currently the Leg query ignores Carrier aspects of FilterClauses (so we get complete tpfdd lines
	  //even when filtering for specific critereon.  This means that when filtering for Carriers, there may NOT 
	  //be instances for some of the legs, hence this is an expected, if ugly condition.	  
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.attachRollupLegsFromResult - no prototype node for : " + proto);
	}
      }else {
	List paths = (List) protoToFewPath.get(proto);
	if (paths == null) {
	  if (debug) {
	    System.err.println ("FilterQuery.attachRollupLegsFromResult - NOTE - could not find path list for proto : " + proto);
	    System.err.println ("Map keys were " + protoToFewPath.keySet ());
	  }
	}
	else {
	  if (debug) {
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.attachRollupLegsFromResult - adding path to : " + proto);
	    TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.attachRollupLegsFromResult - pathlist: " + paths.size());
	  }

	  // For each path, create a clone of the aggTypeNode with that path
	  for (Iterator pathIter = paths.iterator(); pathIter.hasNext();) {
	    Path path = (Path) pathIter.next();
	    AggregateCargoType branchingNode = createCargoType(generator,
							       proto,
							       prototypeNode.getCargoType(),
							       prototypeNode.getName(),
							       path.getAggNumber(), // this is key
							       prototypeNode.getTotalAggNumber(),
							       prototypeNode.getUnitName(),
							       prototypeNode.getWeight(),
							       prototypeNode.getWidth(),
							       prototypeNode.getHeight(),
							       prototypeNode.getDepth(),
							       prototypeNode.getArea(),
							       prototypeNode.getVolume());
	    cargoTree.addNode(cargoTree.getRoot().getUID(), branchingNode);
	
	    if (debug) 
	      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Adding legs to tree");
	    path.addLegsToNode (branchingNode, cargoTree);  
	  }
	}
      }
    }
  }

  protected boolean couldCombine (List paths, Path newPath) {
    for (Iterator iter = paths.iterator(); iter.hasNext();) {
      Path path = (Path) iter.next();
      if (path.startsWith (newPath)) {
	if (path.canCombine (newPath)) {
	  path.combineWithPath (newPath);
	  return true;
	}
      }
      else if (debug) 
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.couldCombine - path " + path +
			    " does not start with " + newPath);
    }

    return false;
  }
  

  // THIS FUNCTION IS FAIRLY INEFFICIENT Because I don't fully
  // understand the nature of the data. It may be largely
  // unnecessary (At least the n^2 part)
  // NOTE THIS IS DESTRUCTIVE TO ARGUMENT
  protected Map combinePathsAsNecessary (Map protoToPaths) {
    Map newProtoToPath = new HashMap ();
    
    for (Iterator iter = protoToPaths.keySet ().iterator(); iter.hasNext();) {
      String proto = (String) iter.next();

      List pathsForProto = (List) protoToPaths.get(proto);
      List newPaths = new LinkedList();

      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "combinePaths - prototype " + proto +
			    " has " + pathsForProto.size () + " paths");

      if (pathsForProto.isEmpty ())
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.combinePaths - ERROR - no paths for " + proto);
	  
      Path thisPath = null;
      for (int j = 0; j < pathsForProto.size(); j++) {
	thisPath = (Path) pathsForProto.get(j);
	if (thisPath != null) {
	  for (int i = j+1; i < pathsForProto.size(); i++) {
	    Path other = (Path) pathsForProto.get (i);
	    if (other != null) {
	      if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "combinePaths - combining " + thisPath +
				    " with " + other);
	      if (thisPath.startsWith (other)) {
		if (thisPath.canCombine (other)) {
		  thisPath.combineWithPath (other);
		  pathsForProto.set(i,null);
		}
	      }
	    }
	  }
	  newPaths.add(thisPath);
	}
      }
      newProtoToPath.put (proto, newPaths);

      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "combinePaths - prototype " +
			    proto + " path is " + thisPath);
    }

    return newProtoToPath;
  }

  protected Map combinePaths (Map protoToPaths) {
    Map newProtoToPath = new HashMap ();
	
    for (Iterator iter = protoToPaths.keySet ().iterator(); iter.hasNext();) {
      String proto = (String) iter.next();
	  
      List pathsForProto = (List) protoToPaths.get(proto);

      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "combinePaths - prototype " + proto +
			    " has " + pathsForProto.size () + " paths");

      if (pathsForProto.isEmpty ())
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.combinePaths - ERROR - no paths for " + proto);
	  
      Path firstPath = (Path) pathsForProto.get(0);

      for (int i = 1; i < pathsForProto.size(); i++) {
	Path other = (Path) pathsForProto.get (i);
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "combinePaths - combining " + firstPath +
			      " with " + other);

	firstPath.combineWithPath (other);
      }
      newProtoToPath.put (proto, firstPath);
      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "combinePaths - prototype " +
			    proto + " path is " + firstPath);
    }

    return newProtoToPath;
  }
  
  protected void addLegToPath (Map protoToPaths, LegNode leg, String prototype) {
    List pathsForProto = (List) protoToPaths.get(prototype);
    if (pathsForProto == null) {
      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "addLegToPath - creating new paths for proto " + prototype);
      pathsForProto = new ArrayList ();
      protoToPaths.put (prototype, pathsForProto);
    }
    else {
      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "addLegToPath - got paths for proto " + prototype + " - " + pathsForProto);
    }
	
    // look to add leg to an existing path
    // yes, if two paths use the same leg, then it's luck which path gets the new leg
    boolean match = false;
    for (int i = 0; i < pathsForProto.size (); i++) {
      Path path = (Path) pathsForProto.get (i);
      if (path.hasLeg (leg) || path.couldAppend(leg)) {
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Found existing path to add leg " + leg + " to.");
		
	path.addLeg (leg);
	match=true;
	break;
      }
    }
    if (!match) {
      Path newPath = new Path ();
      newPath.addLeg (leg);
      pathsForProto.add (newPath);

      if (debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Creating new path with leg " + leg + "");
    }
  }
  
  int numPaths = 0;
  
  class Path {
    List legs = new ArrayList ();          // Ordered list of legs
    Map fromToPairToLeg = new HashMap ();  // Maps from/to code to legNode
    int myNumPaths;                        // Path index
    int aggNumber;                         // Number of assets that share this path

    public Path () {
      myNumPaths = numPaths++;
      aggNumber = 1;
    }

    public int getAggNumber() { return aggNumber; }
    public void setAggNumber(int a) { aggNumber = a; }

    void addLegsToNode (Node prototypeNode, Tree cargoTree) {
      for (int i = 0; i < legs.size (); i++)
	cargoTree.addNode (prototypeNode.getUID(), (LegNode) legs.get(i));
    }
	
    /** ordering guaranteed by order by clause of sql statement */
    void addLeg (LegNode leg) {  
      LegNode currentLeg = 
	(LegNode) fromToPairToLeg.get (leg.getFromCode()+leg.getToCode());
      if (currentLeg == null) {
	//		if (debug)
	//		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "addLeg - appending " + leg +
	//							  " to " + legs + " at " + legs.size ());
	legs.add (legs.size(), leg); // append
	fromToPairToLeg.put (leg.getFromCode()+leg.getToCode(), leg);
	//		if (debug)
	//		  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "addLeg - fromToPairToLeg now " + fromToPairToLeg);
		  
      } else {
	mergeLeg (currentLeg, leg);
      }
    }
	
    void mergeLeg (LegNode currentLeg, LegNode leg) {
      if (leg.getActualStart().getTime() < currentLeg.getActualStart ().getTime())
	currentLeg.setActualStart (leg.getActualStart ());
      if (leg.getActualEnd().getTime()   > currentLeg.getActualEnd ().getTime())
	currentLeg.setActualEnd (leg.getActualEnd ());
      if (currentLeg.getCarrierType().indexOf (leg.getCarrierType()) == -1)
	currentLeg.setCarrierType (currentLeg.getCarrierType() + ", " + 
				   leg.getCarrierType());
      String carrierName = trim(leg.getCarrierName());
	  
      if (currentLeg.getCarrierName().indexOf (carrierName) == -1)
	currentLeg.setCarrierName (currentLeg.getCarrierName() + ", " + carrierName);
    }

    boolean hasLeg (LegNode leg) {
      String key = leg.getFromCode()+leg.getToCode();
      boolean retval = (fromToPairToLeg.get (key) != null);
      if (!retval && debug)
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "hasLeg - fromToPairToLeg " + fromToPairToLeg +
			    " does not include " + key);
			  

      return retval;
    }

    boolean startsWith (Path otherPath) {
      if (legs.isEmpty ())
	return false;

      LegNode leg = (LegNode) otherPath.legs.get(0);

      LegNode firstLeg = (LegNode) legs.get (0);
	  
      return (firstLeg.getFromCode ().equals (leg.getFromCode ()) &&
	      firstLeg.getToCode   ().equals (leg.getToCode ()));
    }
	  
    boolean couldAppend (LegNode leg) {
      if (legs.isEmpty ())
	return false;

      LegNode lastLeg = (LegNode) legs.get (legs.size()-1);
	  
      return (lastLeg.getToCode ().equals (leg.getFromCode ()));
    }
	
    /** 
     * one path can combine with another if they follow the same geographic path
     * (the same from-to sequence)
     **/
    boolean canCombine (Path other) {
      if (legs.size () != other.legs.size ())
	return false;
	  
      for (int i = 0; i < legs.size (); i++) {
	LegNode thisLeg  = (LegNode)legs.get(i);
	LegNode otherLeg = (LegNode)other.legs.get(i);

	// compare the other from and tos
	if (thisLeg.getFromCode().indexOf (otherLeg.getFromCode()) == -1)
	  return false;
			
	if (thisLeg.getToCode().indexOf (otherLeg.getToCode()) == -1)
	  return false;
      }

      return true;
    }

    void combineWithPath (Path other) {
      //	  if (!matchPathEnds) {
      if (legs.size () != other.legs.size ()) {
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Unequal number of legs in paths - " + legs + " vs " + other.legs);
	return;
      }
      //	  }
	  
      /*
	int lengthDiff = 0;
	int thisStart   = 0;
	int otherStart  = 0;
	  
	if (matchPathEnds) {
	lengthDiff  = legs.size ()-other.legs.size();
	thisStart   = (lengthDiff > 0) ? 0 : lengthDiff;
	otherStart  = (lengthDiff > 0) ? lengthDiff : 0;
	max         = (lengthDiff > 0) ? legs.size() : other.legs.size();
	}
      */

      //	  for (int i = Math.abs(lengthDiff); i < max; i++) {
      //		LegNode thisLeg  = (LegNode)legs.get(i-thisStart);
      //		LegNode otherLeg = (LegNode)other.legs.get(i-otherStart);
      for (int i = 0; i < legs.size(); i++) {
	LegNode thisLeg  = (LegNode)legs.get(i);
	LegNode otherLeg = (LegNode)other.legs.get(i);
	mergeLeg (thisLeg, otherLeg);

	// append the other from and tos if we don't have them already
	if (thisLeg.getFromCode().indexOf (otherLeg.getFromCode()) == -1) {
	  thisLeg.setFromCode (thisLeg.getFromCode () + ", " + otherLeg.getFromCode ());
	  thisLeg.setFrom     (thisLeg.getFrom ()     + ", " + otherLeg.getFrom ());
	}
			
	if (thisLeg.getToCode().indexOf (otherLeg.getToCode()) == -1) {
	  thisLeg.setToCode   (thisLeg.getToCode () + ", " + otherLeg.getToCode ());
	  thisLeg.setTo       (thisLeg.getTo ()     + ", " + otherLeg.getTo ());
	}
      }

      // increment the path agg number
      aggNumber += other.getAggNumber();
    }
	  
	
    public String toString () {
      StringBuffer sb = new StringBuffer ();

      sb.append ("#" + myNumPaths + " ");
      sb.append ("[");
      for (int i = 0; i < legs.size (); i++) {
	LegNode leg = (LegNode)legs.get(i);
	sb.append ("(" + leg.getFromCode() + "-" + leg.getToCode () + ")");
      }
      sb.append ("]");

      return sb.toString ();
    }
  } // End inner class Path
  

  protected AggregateCargoType createCargoType (UIDGenerator generator, 
						String proto, 
						String type, 
						String nomen, 
						int aggNumber, 
						int totalAggNumber,
						String unit,
						double weight, double width, double height, double depth, double area, double volume) {
    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.createCargoType() - proto=" + proto +
			 " type=" + type + 
			 " nomen=" + nomen + 
			 " aggNumber=" + aggNumber + 
			 " unit=" + unit + " weight " + weight + " width " + width + " height " + height + " depth " + depth);

    AggregateCargoType cargoType = new AggregateCargoType (generator, proto);
    cargoType.setDisplayName (nomen);
    cargoType.setCargoName (nomen);
    cargoType.setAggNumber (aggNumber);
    cargoType.setTotalAggNumber (totalAggNumber);
    cargoType.setCargoType (type);

    cargoType.setWeight (weight);
    cargoType.setWidth  (width);
    cargoType.setHeight (height);
    cargoType.setDepth  (depth);
    cargoType.setArea   (area);
    cargoType.setVolume (volume);

    String longName = nomen; // shows on mouseover in tpfdd view
	
    if (aggregateByUnit) {
      cargoType.setUnitName (unit);
      longName = unit + " : " + longName;
    }
    cargoType.setLongName (longName);
	  
    if (debug)
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.createCargoType() - created aggregate cargo " + cargoType);

    return cargoType;
  }
  
  protected String formCargoInstanceSql (FilterClauses filterClauses, int recentRun, boolean protoParentNull) {
    String convInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String convInstanceID   = convInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;

    String convProtoTable   = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
    String convProtoProtoID = convProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;

    String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
    String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;
    String cccDimTable = DGPSPConstants.CARGO_CAT_CODE_DIM_TABLE + "_" + recentRun;
    String prototypeProto = assetProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String cccDimProto = cccDimTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String prototypeParentProto = assetProtoTable + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;
    String prototypeNomen = assetProtoTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
    String cccDimWeight = DGPSPConstants.COL_WEIGHT;
    String cccDimWidth  = DGPSPConstants.COL_WIDTH;
    String cccDimHeight = DGPSPConstants.COL_HEIGHT;
    String cccDimDepth  = DGPSPConstants.COL_DEPTH;
    String cccDimArea   = DGPSPConstants.COL_AREA;
    String cccDimVolume = DGPSPConstants.COL_VOLUME;

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

    String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
    String itinLeg     = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
    String itinID      = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;
    String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
    String cLegID      = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
    String cLegStart   = conveyedLegTable + "." + DGPSPConstants.COL_STARTTIME;

    String sqlQuery = 
      "select distinct " + instanceID + ", " + protoColToUse + ", " + instanceName + "," + instanceAggNumber + "," +
      orgNamesName + ", " + prototypeNomen +",\n" + 
      cccDimWeight +", " + cccDimWidth +", " + cccDimHeight +", " + cccDimDepth +", " + cccDimArea +", " + cccDimVolume +
      "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + ", " + orgNames + ", " + assetItineraryTable + ", " +
      conveyedLegTable + ", " + cccDimTable +
      filterClauses.getTables   (recentRun, false, false) + "\n" +
      filterClauses.getWhereSql (instanceOwner, convProtoProtoID, convInstanceID, protoColToUse, instanceID) +
      filterClauses.getJoins    (recentRun) +
      "\nand " + instanceProto + " = " + prototypeProto +
      "\nand " + orgNamesOrg + " = " + instanceOwner +
      "\nand " + prototypeParentProto + protoTest +
      "\nand " + instanceID + " = " + itinID + 
      "\nand " + itinLeg + " = " + cLegID +
      "\nand " + prototypeProto + " = " + cccDimProto +
      "\norder by " + orgNamesName + ", " + ((sortByName) ? instanceName + ", " : "") + prototypeNomen + ", " + cLegStart;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formCargoInstanceSql - sort by name " + sortByName + "\n" + sqlQuery);
	
    return sqlQuery;
  }

  protected String formFastCargoLegSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_LEG,recentRun);

    String assetInstanceOwner = DGPSPConstants.COL_OWNER;
    String assetInstanceID    = DGPSPConstants.COL_ASSETID;
    String instanceProto      = PrepareDerivedTables.COL_INST_PROTOTYPEID;
    String cLegID      = DGPSPConstants.COL_LEGID;
    String cLegStart   = DGPSPConstants.COL_STARTTIME;
    String cLegEnd     = DGPSPConstants.COL_ENDTIME;
    String cLegType    = DGPSPConstants.COL_LEGTYPE;
    String cLegReadyAt = DGPSPConstants.COL_READYAT;

    String l1geoloc = PrepareDerivedTables.COL_START_GEOLOC;
    String l1name   = PrepareDerivedTables.COL_START_PRETTYNAME;

    String l2geoloc = PrepareDerivedTables.COL_END_GEOLOC;
    String l2name   = PrepareDerivedTables.COL_END_PRETTYNAME;

    String ciConvID      = DGPSPConstants.COL_CONVEYANCEID;
    String ciBumper      = DGPSPConstants.COL_BUMPERNO;
    String cpPrototypeID = PrepareDerivedTables.COL_CONV_PROTOTYPEID;
    String cpConvType    = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String cpNomen       = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String aggNumber     = DGPSPConstants.COL_AGGREGATE;
    String isLowFi       = DGPSPConstants.COL_IS_LOW_FIDELITY;

    String sqlQuery = 
      "select " + "d2."+cLegID + ", " + "d2."+cLegStart + ", " + "d2."+cLegEnd + ", " + "d2."+cLegReadyAt + ",\n" + 
      "d2."+l1geoloc + ", " + "d2."+l1name + ",\n" + 
      "d2."+l2geoloc + ", " + "d2."+l2name + ",\n" + 
      "d2."+cpConvType + ", " + "d2."+cLegType + ", " + "d2."+assetInstanceID + ",\n"+
      "d2."+ciConvID + ", " + "d2."+instanceProto + ", " + "d2."+cpNomen + ",\n" + 
      "d2."+ciBumper + ", " + "d2."+assetInstanceOwner + ", " + "d2."+aggNumber + ", " + "d2."+isLowFi+
      "\nfrom " + derivedTable + " d2" + 
      filterClauses.getWhereSql ("d2."+assetInstanceOwner, "d2."+cpPrototypeID, "d2."+ciConvID, 
				 "d2."+instanceProto, "d2."+assetInstanceID) +
      "\norder by " + assetInstanceID + ", " + cLegStart;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formFastCargoLegSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formCargoLegSql (FilterClauses filterClauses, int recentRun, boolean protoParentNull) {
    String locTable = DGPSPConstants.LOCATIONS_TABLE + "_" + recentRun;
    String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
    String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
    String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
    String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;

    String assetInstanceOwner = assetInstanceTable + "." + DGPSPConstants.COL_OWNER;
    String assetInstanceID    = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceProto      = assetInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;
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

    String assetProtoTable = DGPSPConstants.ASSET_PROTOTYPE_TABLE + "_" + recentRun;
    String prototypeProto = assetProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    String prototypeParentProto = assetProtoTable + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;
    String instanceAggNumber  = assetInstanceTable + "." + DGPSPConstants.COL_AGGREGATE;

    String protoColToUse = (protoParentNull) ? instanceProto : prototypeParentProto;
    String protoTest =     (protoParentNull) ? " is null " : " is not null" ;
	
    String sqlQuery = 
      "select " + cLegID + ", " + cLegStart + ", " + cLegEnd + ", " + cLegReadyAt + ", " + 
      l1geoloc + ", " + l1name + ",\n" + 
      l2geoloc + ", " + l2name + ", " + 
      cpConvType + ", " + cLegType + ", " + assetInstanceID +
      ",\n" + ciConvID + ", " + instanceProto + ", " + cpNomen + ", " + 
      ciBumper + ", " + assetInstanceOwner + ", " + instanceAggNumber +
      "\nfrom " + locTable + " l1, " + locTable + " l2, " + assetInstanceTable + ", " + assetItineraryTable + ",\n" +
      conveyedLegTable + ", " + conveyanceInstanceTable + ", " + conveyancePrototypeTable + ", " + assetProtoTable +
      filterClauses.getWhereSql (assetInstanceOwner, cpPrototypeID, ciConvID, protoColToUse, assetInstanceID) +
      filterClauses.getJoins (recentRun) +
      "\nand " + assetInstanceID + " = " + itinID + 
      "\nand " + itinLeg + " = " + cLegID +
      "\nand " + cLegStartLoc + " = " + l1id +
      "\nand " + cLegEndLoc + " = " + l2id +
      "\nand " + cLegConvID + " = " + ciConvID +
      "\nand " + ciPrototypeID + " = " + cpPrototypeID +
      "\nand " + instanceProto + " = " + prototypeProto +
      "\nand " + prototypeParentProto + protoTest +
      "\norder by " + assetInstanceID + ", " + cLegStart;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formCargoLegSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formFastCargoTypeSql(FilterClauses filterClauses, int recentRun) {
    String derivedTable = 
      PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_TYPE,recentRun);
    
    String typeID  = DGPSPConstants.COL_ALP_TYPEID;
    String nomen   = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String proto   = PrepareDerivedTables.COL_INST_PROTOTYPEID;
    String prototypeWeight = DGPSPConstants.COL_WEIGHT;
    String prototypeWidth  = DGPSPConstants.COL_WIDTH;
    String prototypeHeight = DGPSPConstants.COL_HEIGHT;
    String prototypeDepth  = DGPSPConstants.COL_DEPTH;
    
    String instanceOwner  = DGPSPConstants.COL_OWNER;
    String instanceID     = DGPSPConstants.COL_ASSETID;
    String instanceAggNumber = DGPSPConstants.COL_AGGREGATE;
    
    String ciConvID      = DGPSPConstants.COL_CONVEYANCEID;
    String cpPrototypeID = PrepareDerivedTables.COL_CONV_PROTOTYPEID;

    String sqlQuery = 
      "select distinct " + typeID + ", " + nomen + ", " + proto + ", " + 
      instanceAggNumber + ", " + instanceOwner + ", " + instanceID + //This is needed for distinct!
      ", " + prototypeWeight +", " + prototypeWidth +", " + prototypeHeight +", " + prototypeDepth +
      "\nfrom " + derivedTable +
      filterClauses.getWhereSql (instanceOwner, cpPrototypeID, ciConvID, proto, instanceID) +
      //      "\ngroup by " + typeID + ((aggregateByUnit) ? ", " + instanceOwner : "") +
      "\norder by " + instanceOwner + "," + nomen;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formFastCargoTypeSql - \n" + sqlQuery);
	
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
    String instanceID     = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceAggNumber  = assetInstanceTable + "." + DGPSPConstants.COL_AGGREGATE;
    String self1Proto     = "self1" + "." + DGPSPConstants.COL_PROTOTYPEID;
    String self1ParentProto  = "self1" + "." + DGPSPConstants.COL_PARENT_PROTOTYPEID;
    
    String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
    String ciConvID      = conveyanceInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
    String cpPrototypeID = conveyancePrototypeTable + "." + DGPSPConstants.COL_PROTOTYPEID;

    String sqlQuery = 
      "select distinct " + self2TypeID + ", " + self2Nomen + ", " + self2Proto + ", " +
      instanceAggNumber + ", " + instanceOwner + ", " + instanceID + //This is needed for distinct!
      "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + " self1, " + assetProtoTable + " self2" +//," + 
      filterClauses.getTables   (recentRun, true, true) +
      filterClauses.getWhereSql (instanceOwner, cpPrototypeID, ciConvID, self2Proto, instanceID) +
      filterClauses.getJoins    (recentRun) +
      "\nand " + instanceProto + " = " + self1Proto + 
      "\nand " + self1ParentProto + " = " + self2Proto + 
      //      "\ngroup by " + self2TypeID + ((aggregateByUnit) ? ", " + instanceOwner : "") +
      "\norder by " + instanceOwner + "," + self2Nomen;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formFirstCargoTypeSql - \n" + sqlQuery);
	
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
    String instanceID     = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceAggNumber  = assetInstanceTable + "." + DGPSPConstants.COL_AGGREGATE;

    String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
    String ciConvID      = conveyanceInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
    String cpPrototypeID = conveyancePrototypeTable + "." + DGPSPConstants.COL_PROTOTYPEID;
		
    String sqlQuery = 
      "select distinct " + self1TypeID + ", " + self1Nomen + ", " + self1Proto + ", " + 
      instanceAggNumber + ", " + instanceOwner + ", " + instanceID + //This is needed for distinct!
      "\nfrom "  + assetInstanceTable + ", " + assetProtoTable + " self1" + //," + 
      filterClauses.getTables   (recentRun, true, true) +
      filterClauses.getWhereSql (instanceOwner, cpPrototypeID, ciConvID, instanceProto, instanceID) +
      filterClauses.getJoins    (recentRun) +
      "\nand " + instanceProto + " = " + self1Proto + 
      "\nand " + self1ParentProto + " is null" +
      //      "\ngroup by " + self1TypeID + ((aggregateByUnit) ? ", " + instanceOwner : "") +
      "\norder by " + instanceOwner + "," + self1Nomen;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formSecondCargoTypeSql - \n" + sqlQuery);
	
    return sqlQuery;
  }

  protected String formManifestTypeSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = 
      PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_TYPE,recentRun);
    String manifestTable = DGPSPConstants.MANIFEST_TABLE + "_" + recentRun;
    String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
    String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;

    String ciConvID      = conveyanceInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
    String cpPrototypeID = conveyancePrototypeTable + "." + DGPSPConstants.COL_PROTOTYPEID;
    
    String typeID = DGPSPConstants.COL_ALP_TYPEID;
    String nomen  = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String instanceOwner = DGPSPConstants.COL_OWNER;
    String instanceID    = DGPSPConstants.COL_MANIFEST_ITEM_ID;
    String proto = "m." + DGPSPConstants.COL_ALP_TYPEID;
    String manifestAssetID = "m." + DGPSPConstants.COL_ASSETID;
    String derivedAssetID   = "d." + DGPSPConstants.COL_ASSETID;

    String sqlQuery = 
      "select distinct m." + typeID + ", m." + nomen + ", m." + typeID + 
      ",\"1\", d."+ instanceOwner + ", m." + instanceID + ", \nm." + 
      DGPSPConstants.COL_WEIGHT + ",\"1\""+",\"1\""+",\"1\""+
      "\nfrom " + manifestTable + " m, " + derivedTable + " d \n" +
      filterClauses.getWhereSql (instanceOwner, cpPrototypeID, ciConvID, proto, instanceID) +
      "\nand " + manifestAssetID + " = " + derivedAssetID +
      "\norder by d." + instanceOwner + ", m." + nomen;
    
    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formManifestTypeSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

  protected String formManifestInstanceSql (FilterClauses filterClauses, int recentRun, boolean something) {
    String derivedTable = 
      PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_TYPE,recentRun);
    String manifestTable = DGPSPConstants.MANIFEST_TABLE + "_" + recentRun;
    String instanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
    String orgNames = HierarchyConstants.ORGNAMES_TABLE_NAME + "_" + recentRun;

    String manifestInstanceID = manifestTable + "." + DGPSPConstants.COL_MANIFEST_ITEM_ID;
    String manifestName      = manifestTable + "." + DGPSPConstants.COL_NAME;
    String manifestAssetID   = manifestTable + "." + DGPSPConstants.COL_ASSETID;
    String typeID = manifestTable + "." + DGPSPConstants.COL_ALP_TYPEID;
    String nomen  = manifestTable + "." + DGPSPConstants.COL_ALP_NOMENCLATURE;
    String weight = manifestTable + "." + DGPSPConstants.COL_WEIGHT;

    String instanceOwner    = derivedTable + "." + DGPSPConstants.COL_OWNER;
    String derivedAssetID   = derivedTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceAssetID  = instanceTable + "." + DGPSPConstants.COL_ASSETID;
    String instanceName     = instanceTable + "." + DGPSPConstants.COL_NAME;

    String orgNamesName = orgNames + "." + HierarchyConstants.COL_PRETTY_NAME;
    String orgNamesOrg  = orgNames + "." + HierarchyConstants.COL_ORGID;

    String ciConvID      = derivedTable + "." + DGPSPConstants.COL_CONVEYANCEID;
    String cpPrototypeID = PrepareDerivedTables.COL_CONV_PROTOTYPEID;

    String assetItineraryTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
    String itinLeg     = assetItineraryTable + "." + DGPSPConstants.COL_LEGID;
    String itinID      = assetItineraryTable + "." + DGPSPConstants.COL_ASSETID;

    String conveyedLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
    String cLegID      = conveyedLegTable + "." + DGPSPConstants.COL_LEGID;
    String cLegStart   = conveyedLegTable + "." + DGPSPConstants.COL_STARTTIME;

    String sqlQuery = 
      "select distinct " + manifestInstanceID + ", " + typeID + ", " + manifestName + ", \"1\",\n" +
      orgNamesName + ", " + nomen + ", " + weight +", " + "\"1\"" +", " + "\"1\"" +", " + "\"1\", " + instanceName+ ", " +cLegStart+
      "\nfrom "  + derivedTable + ", " + manifestTable + ", " + orgNames + ", " + instanceTable + ", " + 
      assetItineraryTable+ ", " + conveyedLegTable + "\n"+
      filterClauses.getWhereSql (instanceOwner, cpPrototypeID, ciConvID, typeID, manifestInstanceID) +
      "\nand " + orgNamesOrg + " = " + instanceOwner +
      "\nand " + manifestAssetID + " = " + derivedAssetID +
      "\nand " + derivedAssetID + " = " + instanceAssetID +
      "\nand " + instanceAssetID + " = " + itinID + 
      "\nand " + itinLeg + " = " + cLegID +
      "\norder by " + orgNamesName + ((sortByName) ? ", " + manifestName : "") + ", " + cLegStart +
      ((!sortByName) ? ", " + manifestName : "");

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formManifestInstanceSql - \n" + sqlQuery);
	
    return sqlQuery;
  }

  protected void attachManifestInstancesFromResult (ResultSet rs, UIDGenerator generator, Tree cargoTree, 
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
	String container = rs.getString (11);

	if (instanceToNode.get (id) != null) // protect against sql bug (seems to be sql bug)
	  continue;

	Node instanceNode = createCargoInstance (generator, id, name, aggnumber, unitName, protoNomen,
						 weight, width, height, depth, width*depth, width*depth*height, proto, container);		
	cargoTree.addNode (cargoTree.getRoot(), instanceNode);
	instanceToNode.put (id, instanceNode);
      }
      if (debug) 
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.attachManifestInstancesFromResult - total rows for instances " + rows);
    } catch (SQLException e) {
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.attachManifestInstancesFromResult - SQLError : " + e);
      e.printStackTrace();
    }finally{
      if(rs!=null) {
	try { rs.close(); } catch (SQLException e){
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "TPFDDQuery.attachManifestInstancesFromResult - " +
			      "closing result set, got sql error : " + e); 
	}
      }
    }
  }

  protected String formManifestLegSql (FilterClauses filterClauses, int recentRun) {
    String derivedTable = PrepareDerivedTables.getDerivedTableName(PrepareDerivedTables.CARGO_LEG,recentRun);
    String manifestTable = DGPSPConstants.MANIFEST_TABLE + "_" + recentRun;

    String assetInstanceOwner = DGPSPConstants.COL_OWNER;
    String assetInstanceID    = DGPSPConstants.COL_ASSETID;
    String cLegID      = DGPSPConstants.COL_LEGID;
    String cLegStart   = DGPSPConstants.COL_STARTTIME;
    String cLegEnd     = DGPSPConstants.COL_ENDTIME;
    String cLegType    = DGPSPConstants.COL_LEGTYPE;
    String cLegReadyAt = DGPSPConstants.COL_READYAT;

    String l1geoloc = PrepareDerivedTables.COL_START_GEOLOC;
    String l1name   = PrepareDerivedTables.COL_START_PRETTYNAME;

    String l2geoloc = PrepareDerivedTables.COL_END_GEOLOC;
    String l2name   = PrepareDerivedTables.COL_END_PRETTYNAME;

    String ciConvID      = DGPSPConstants.COL_CONVEYANCEID;
    String ciBumper      = DGPSPConstants.COL_BUMPERNO;
    String cpPrototypeID = PrepareDerivedTables.COL_CONV_PROTOTYPEID;
    String cpConvType    = DGPSPConstants.COL_CONVEYANCE_TYPE;
    String cpNomen       = DGPSPConstants.COL_ALP_NOMENCLATURE;
    String aggNumber     = DGPSPConstants.COL_AGGREGATE;

    String manifestProto = manifestTable + "." + DGPSPConstants.COL_ALP_TYPEID;
    String manifestInstanceID = manifestTable + "." + DGPSPConstants.COL_MANIFEST_ITEM_ID;
    String manifestAssetID = manifestTable + "." + DGPSPConstants.COL_ASSETID;
    String isLowFi       = DGPSPConstants.COL_IS_LOW_FIDELITY;
    
    String sqlQuery = 
      "select " + "d2."+cLegID + ", " + "d2."+cLegStart + ", " + "d2."+cLegEnd + ", " + "d2."+cLegReadyAt + ", " + 
      "d2."+l1geoloc + ", " + "d2."+l1name + ",\n" + 
      "d2."+l2geoloc + ", " + "d2."+l2name + ", " + 
      "d2."+cpConvType + ", " + "d2."+cLegType + ", " + manifestInstanceID + ",\n"+
      "d2."+ciConvID + ", " + manifestProto + ", " + "d2."+cpNomen + ", " + 
      "d2."+ciBumper + ", " + "d2."+assetInstanceOwner + ", " + "d2."+aggNumber +", d2."+isLowFi+
      "\nfrom " + derivedTable + " d2, " + manifestTable + "\n" +
      filterClauses.getWhereSql ("d2."+assetInstanceOwner, "d2."+cpPrototypeID, "d2."+ciConvID, 
				 manifestProto, manifestInstanceID) +
      "\nand " + "d2."+assetInstanceID + " = " + manifestAssetID + 
      "\norder by " + "d2." + assetInstanceID + ", " + cLegStart;

    if (debug) 
      TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "FilterQuery.formManifestLegSql - \n" + sqlQuery);
    
    return sqlQuery;
  }

}
