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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cougaar.mlm.ui.newtpfdd.TPFDDConstants;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Node;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.Org;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.LegNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.DBUIDNode;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CargoInstance;
import org.cougaar.mlm.ui.grabber.logger.Logger;
import org.cougaar.mlm.ui.grabber.logger.TPFDDLoggerFactory;

/** Holds all nodes */

public class Tree {
  protected HashMap uidToNode = new HashMap();
  protected Node root;
  protected UIDGenerator generator = UIDGenerator.getGenerator ();
  protected long earliest = Long.MAX_VALUE;
  protected long latest   = Long.MIN_VALUE;
  boolean debug = false;
  
  public Node getRoot () { return root;  }
  public UIDGenerator getGenerator () { return generator;  }

  public Tree () {}
  
  public Tree (Node root) {
	setRoot (root);
  }
  
  public void setRoot (Node root) { 
	this.root = root;  
	uidToNode.put (new Long(root.getUID ()), root);
  }
  
  public synchronized Node getNode (long uid) {
	return (Node)uidToNode.get(new Long(uid));
  }

  public void graftOnChild (Node parent, Node child, Tree childTree) {
	if (parent != null)
	  addNode(parent, child);

	for (int i = 0; i < child.getChildCount (); i++) {
	  long uid = child.getChildUID (i);
	  Node childChild = childTree.getNode (uid);
	  uidToNode.put (new Long (uid), childChild);
	  graftOnChild (null, childChild, childTree);
	}
  }
  
  /** 
   * tpfdd assumes uid are unique, but 
   * database doesn't explicitly guarantee that it's keys are unique across categories.
   */
  public String getDBUID (long uid) {
	return generator.getDBUID (uid);
  }

  /** 
   * tpfdd assumes uid are unique, but 
   * database doesn't explicitly guarantee that it's keys are unique across categories.
   */
  public Node getNodeDBUID (int nodeType, String dbuid) {
	return getNode(generator.getUIDForDBUID (nodeType, dbuid));
  }

  public synchronized void addNode (Node parent, Node child) {
	addNode (parent.getUID(), child);
  }

  /**
   * Root nodes have special parent = ROOTNAME
   *
   * Accumulates earliest and latest times as nodes are added.
   */
  public synchronized void addNode (long parentUID, Node child) {
	child.setParentUID (parentUID);

	Node parent = getNode (parentUID);
	if (parent == null) {
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree.addNode -- ERROR, no parent for " + parentUID);
	} else {
	  parent.addChild (child.getUID());
    }
	
	uidToNode.put (new Long(child.getUID ()), child);
  }

  public synchronized void addSubTree (long parentUID, Tree subTree) {
	addNode (parentUID, subTree.getRoot());
	uidToNode.putAll (subTree.uidToNode);
  }

  public Set getTreesFromChildren (Node parentNode/*, Comparator comparator*/) {
	//	SortedSet forest = new TreeSet (comparator);
	Set forest = new HashSet ();
	
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree.getTreesFromChildren - parent " + parentNode);
	for (int i = 0; i < parentNode.getChildCount (); i++) {
	  Node child = getChild (parentNode, i);
	  if (debug)
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree.getTreesFromChildren - child " + child);
	  Tree childTree = new Tree(child);
	  childTree.reconstruct (child, this);
	  forest.add (childTree);
	}

	return forest;
  }

  protected void reconstruct (Node parent, Tree originalTree) {
	List children = new ArrayList ();
	
	for (int i = 0; i < parent.getChildCount (); i++) {
	  Node child = originalTree.getChild (parent, i);
	  children.add (child);
	}
	parent.clearChildren();
	
	for (Iterator iter = children.iterator(); iter.hasNext();) {
	  Node child = (Node) iter.next();

	  if (parent == child) {
		TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree.reconstruct - circular graph? parent " + parent + " = " +
							child + "???");
		continue;
	  }
	  addNode (parent, child);
	  reconstruct (child, originalTree);
	}
  }
  
  public synchronized int getChildCount (Node node) {
	return node.getChildCount();
  }

  public synchronized Node getChild (Node node, int index) {
	return getNode(node.getChildUID(index));
  }

  public synchronized int getIndexOfChild(Node node, Node child) {
	return node.indexOf(child.getUID());
  }

  public synchronized Node getChildWithDBUID (Node node, String dbuid) {
	for (int i = 0; i < node.getChildCount (); i++) {
	  Node child = getChild (node, i);
	  String childDBUID = getDBUID (child.getUID());
	  if (childDBUID.equals (dbuid))
		return child;
	}
	return null;
  }

  public synchronized long getMinTaskStart() {
	if (root == null)
	  return 0;
	if (root.getActualStart () == null)
	  return 0;
	return root.getActualStart().getTime();
  }

  public synchronized long getMaxTaskEnd() {
	if (root == null)
	  return 0;
	if (root.getActualEnd () == null)
	  return 0;
	return root.getActualEnd().getTime();
  }

  public void show () {
	//	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, header + this + " children : " + children);
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree is :");
	showNode (getRoot (), "");
  }

  public void showNode (Node node, String indent) {
	TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, indent + "" + node + " (" + node.getChildCount()+ ")");
	for (int i = 0; i < node.getChildCount(); i++)
	  showNode (getNode(node.getChildUID(i)), indent + " ");
  }

  public synchronized Object [] doRollup (Node node) {
	Date nodeStart = node.getActualStart ();
	Date nodeEnd   = node.getActualEnd   ();
	String from = node.getFrom ();
	String to = node.getTo ();
	int mode = node.getMode ();
	Set fromSet = new HashSet ();
	if (from != null && from.length()>0)
	  fromSet.add(from);
	Set toSet   = new HashSet ();
	if (to != null && to.length()>0)
	  toSet.add(to);
	
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree.doRollup - before " + node + " s " + 
						       nodeStart + " e " + nodeEnd + " mode " + mode);
	
	for (int i = 0; i < node.getChildCount (); i++) {
	  Node childNode = getNode (node.getChildUID (i));
	  Object [] rollup = doRollup (childNode);
	  Date childStart = (Date) rollup[0];
	  Date childEnd   = (Date) rollup[1];
	  Set childFromSet = (Set) rollup[2];
	  Set childToSet = (Set) rollup[3];
	  int childMode = ((Integer) rollup[4]).intValue();
	  
	  if (childStart != null && 
		  (nodeStart == null || childStart.before (nodeStart))) {
		nodeStart = childStart;
		fromSet = childFromSet;
	  }
	  if (childEnd != null && 
		  (nodeEnd == null || childEnd.after (nodeEnd))) {
		nodeEnd = childEnd;
		toSet = childToSet;
	  }
	  //	  if (!(node instanceof LegNode) && !(node instanceof CargoInstance)) {
	  if (node instanceof Org) {
		fromSet.addAll (childFromSet);
		toSet.addAll   (childToSet);
	  }

	  mode = getMode (mode, childMode);
	}
	if (debug)
	  TPFDDLoggerFactory.createLogger().logMessage(Logger.NORMAL, Logger.GENERIC, "Tree.doRollup - after " + 
						       node + " s " + nodeStart + " e " + nodeEnd + " mode " + mode);

	node.setActualStart (nodeStart);
	node.setActualEnd   (nodeEnd);

	node.setFrom (trim(fromSet.toString()));
	node.setTo   (trim(toSet.toString()));
	node.setMode (mode);
	
	return new Object [] { nodeStart, nodeEnd, fromSet, toSet, new Integer(mode)};
  }

  protected int getMode (int myMode, int childMode) {
	if (myMode == Node.MODE_AGGREGATE)
	  return myMode;
	if ((myMode == Node.MODE_SEA && childMode == Node.MODE_AIR) ||
		(myMode == Node.MODE_AIR && childMode == Node.MODE_SEA)) {
	  return Node.MODE_SEA;
	}
	if (myMode == Node.MODE_SEA || myMode == Node.MODE_AIR)
	  return myMode;
	if (childMode != Node.MODE_UNKNOWN)
	  return childMode;
	return myMode;
  }

  protected String trim (String toTrim) {
	return toTrim.substring (1, toTrim.length() -1);
  }
  
}

  




