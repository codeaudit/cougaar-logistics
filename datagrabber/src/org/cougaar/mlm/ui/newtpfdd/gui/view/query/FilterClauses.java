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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;
import org.cougaar.mlm.ui.newtpfdd.gui.view.node.CategoryNode;

public class FilterClauses {
  List unitDBUIDs = new ArrayList ();

  List carrierTypes = new ArrayList ();
  List carrierInstances = new ArrayList ();

  List cargoTypes = new ArrayList ();
  List cargoInstances = new ArrayList ();

  List locations = new ArrayList ();
  List convoys = new ArrayList ();
  List assetTypes = new ArrayList ();

  boolean rollup = false;
  boolean byUnit = false;
  boolean sortByName = false;
  
  String other = "";

  public FilterClauses(){}

  public FilterClauses(FilterClauses fc){
    this.unitDBUIDs=new ArrayList(fc.unitDBUIDs);
    this.carrierTypes=new ArrayList(fc.carrierTypes);
    this.carrierInstances=new ArrayList(fc.carrierInstances);
    this.cargoTypes=new ArrayList(fc.cargoTypes);
    this.cargoInstances=new ArrayList(fc.cargoInstances);
    this.locations=new ArrayList(fc.locations);
    this.convoys=new ArrayList(fc.convoys);
    this.assetTypes=new ArrayList(fc.assetTypes);
    rollup=fc.rollup;
    byUnit=fc.byUnit;
    sortByName=fc.sortByName;
    other=fc.other;
  }
  
  public List getUnitDBUIDs () { return unitDBUIDs;  }
  public void addUnitDBUID (String unitDBUID)  {	unitDBUIDs.add (unitDBUID);  }
  public void setUnitDBUIDs (List l)  {	unitDBUIDs = l;  }

  public List getCarrierTypes () { return carrierTypes;  }
  public void addCarrierType (String type)  {	carrierTypes.add (type);  }
  public void setCarrierTypes (List l)  {	carrierTypes = l;  }
  public boolean hasCarrierTypes ()  {	return !carrierTypes.isEmpty();  }

  public List getCargoTypes () { return cargoTypes;  }
  public void addCargoType (String type)  {	cargoTypes.add (type);  }
  public void setCargoTypes (List l)  {	cargoTypes = l;  }
  public boolean hasCargoTypes ()  {	return !cargoTypes.isEmpty();  }

  public List getCarrierInstances () { return carrierInstances;  }
  public void addCarrierInstance (String instance)  {	carrierInstances.add (instance);  }
  public void setCarrierInstances (List l)  {	carrierInstances = l;  }
  public boolean hasCarrierInstances ()  {	return !carrierInstances.isEmpty();  }

  public List getCargoInstances () { return cargoInstances;  }
  public void addCargoInstance (String instance)  {	cargoInstances.add (instance);  }
  public void setCargoInstances (List l)  {	cargoInstances = l;  }
  public boolean hasCargoInstances ()  {	return !cargoInstances.isEmpty();  }


  public List getLocations () { return locations;  }
  public void addLocation (String type)  {	locations.add (type);  }
  public void setLocations (List l)  {	locations = l;  }
  public boolean hasLocations ()  {	return !locations.isEmpty();  }

  public List getConvoys () { return convoys;  }
  public void addConvoy (String type)  {	convoys.add (type);  }
  public void setConvoys (List l)  {	convoys = l;  }
  public boolean hasConvoys ()  {	return !convoys.isEmpty();  }

  public List getAssetTypes () { return assetTypes;  }
  public void addAssetType (String type)  {	assetTypes.add (type);  }
  public void setAssetTypes (List l)  {	assetTypes = l;  }
  public boolean hasAssetTypes ()  {	return !assetTypes.isEmpty();  }

  public String getOther () { return other;  }
  public void setOther (String other)  {	this.other = other; }

  public boolean getRollup () { return rollup;  }
  public void setRollup (boolean rollup)  {	this.rollup = rollup; }

  public boolean getByUnit () { return byUnit;  }
  public void setByUnit (boolean byUnit)  {	this.byUnit = byUnit; }

  public boolean getSortByName () { return sortByName;  }
  public void setSortByName (boolean sortByName)  {	this.sortByName = sortByName; }

  public String getUnitWhereSql (String unitCol) {
    return getWhereSql (unitCol, null, null, null, null);
  }
  
  public String getWhereSql (String unitCol, String carrierTypeCol, String carrierInstanceCol,
						String cargoTypeCol, String cargoInstanceCol) {
	StringBuffer sb = new StringBuffer ();
	sb.append (" where ");
	
	boolean justDidClause = false;
	
	justDidClause = appendClause (unitCol, unitDBUIDs, sb, justDidClause);
	justDidClause |= appendClause (carrierTypeCol, carrierTypes, sb, justDidClause);
	justDidClause |= appendClause (carrierInstanceCol, carrierInstances, sb, justDidClause);
	justDidClause |= appendClause (cargoTypeCol, cargoTypes, sb, justDidClause);
	justDidClause |= appendClause (cargoInstanceCol, cargoInstances, sb, justDidClause);

	if (other.length () > 0)  {
	  if(justDidClause)
	    sb.append ("\nand ");
	  sb.append (other);
	  justDidClause=true;
	}

	if(justDidClause) {
	  return sb.toString();
	} else {
	  return "";
    }
  }
  
  public String getAssetTables(int recentRun) {
    StringBuffer sb = new StringBuffer();
    if (hasConvoys()) {
      sb.append(", ");
      sb.append(DGPSPConstants.CONVOY_MEMBER_TABLE + "_" + recentRun);
      sb.append("\n");
    }
    return sb.toString();
  }

    public String getAssetWhereSql(int recentRun) {
	StringBuffer sb = new StringBuffer ();
	sb.append (" where ");
	
	String conveyanceInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String conveyancePrototypeTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
	String convoyTable = DGPSPConstants.CONVOY_MEMBER_TABLE + "_" + recentRun;
	String ciPrototypeID = conveyanceInstanceTable  + "." + DGPSPConstants.COL_PROTOTYPEID;
	String ciConvID      = conveyanceInstanceTable  + "." + DGPSPConstants.COL_CONVEYANCEID;
	String ciLoc         = conveyanceInstanceTable  + "." + DGPSPConstants.COL_OWNER;
	String ciSelfProp    = conveyanceInstanceTable  + "." + DGPSPConstants.COL_SELFPROP;
	String cpConvType    = conveyancePrototypeTable + "." + DGPSPConstants.COL_CONVEYANCE_TYPE;
	String cvConvID      = convoyTable              + "." + DGPSPConstants.COL_CONVOYID;


	boolean justDidClause = false;
	justDidClause = appendClause (ciPrototypeID, carrierTypes, sb, justDidClause);
	justDidClause = appendClause (ciConvID, carrierInstances, sb, justDidClause);
	justDidClause = appendClause (ciLoc, locations, sb, justDidClause);
	justDidClause = appendClause (cvConvID, convoys, sb, justDidClause);
	justDidClause = specialAppendClause (cpConvType, ciSelfProp, assetTypes, sb, justDidClause);

	if (hasConvoys()) {
	  sb.append("\nand ");
	  sb.append(DGPSPConstants.CONVOY_MEMBER_TABLE + "_" + recentRun+"."+DGPSPConstants.COL_CONVEYANCEID+
		    " = "+
		    DGPSPConstants.CONV_INSTANCE_TABLE + "_" +recentRun+"."+DGPSPConstants.COL_CONVEYANCEID);
	}

	if (other.length () > 0)  {
	  sb.append ("\nand ");
	  sb.append (other);
	}

	return sb.toString();
    }

  protected String getTables (int recentRun, boolean includeItin, boolean includeLeg) {
	String convProtoTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
	String convInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String itinTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
	String cLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;

	String tablesForCarrierInstance = (includeItin ? itinTable + ", " : "") +  
	  (includeLeg ? cLegTable + ", " : "") + convInstanceTable;
	String tablesForCarrierProto    = ", " + convProtoTable;

	return (hasCarrierInstances() ? ", " + tablesForCarrierInstance :
			(hasCarrierTypes   () ? ", " + tablesForCarrierInstance + tablesForCarrierProto : ""));
  }
  
  protected String getTables (int recentRun) {
	String convInstanceTable = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String itinTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
	String cLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;

	String tablesForCarrierInstance = itinTable + ", " + cLegTable + ", " + convInstanceTable;
	return tablesForCarrierInstance;
  }
  
  protected String getJoins (int recentRun) {
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String instanceID        = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;

	String itinTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
	String itinID  = itinTable + "." + DGPSPConstants.COL_ASSETID;
	String itinLeg = itinTable + "." + DGPSPConstants.COL_LEGID;

	String cLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
	String cLegID      = cLegTable + "." + DGPSPConstants.COL_LEGID;
	String cLegConvID  = cLegTable + "." + DGPSPConstants.COL_CONVEYANCEID;

	String convInstanceTable    = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String convInstanceID       = convInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;
	String convInstanceProtoID  = convInstanceTable + "." + DGPSPConstants.COL_PROTOTYPEID;

	String convProtoTable = DGPSPConstants.CONV_PROTOTYPE_TABLE + "_" + recentRun;
	String convProtoProtoID = convProtoTable + "." + DGPSPConstants.COL_PROTOTYPEID;

	String joinsToCarrierInstance = "\nand " + instanceID + " = " + itinID +
	  "\nand " + itinLeg + " = " + cLegID +
	  "\nand " + cLegConvID + " = " + convInstanceID;
	String joinsToCarrierProto = "\nand " + convInstanceProtoID + " = " + convProtoProtoID;

	return (hasCarrierInstances() ? joinsToCarrierInstance :
			(hasCarrierTypes   () ? joinsToCarrierInstance + joinsToCarrierProto : ""));
  }

  protected String getJoinsToCarrierInstance (int recentRun) {
	String assetInstanceTable = DGPSPConstants.ASSET_INSTANCE_TABLE + "_" + recentRun;
	String instanceID        = assetInstanceTable + "." + DGPSPConstants.COL_ASSETID;

	String itinTable = DGPSPConstants.ASSET_ITINERARY_TABLE + "_" + recentRun;
	String itinID  = itinTable + "." + DGPSPConstants.COL_ASSETID;
	String itinLeg = itinTable + "." + DGPSPConstants.COL_LEGID;

	String cLegTable = DGPSPConstants.CONVEYED_LEG_TABLE + "_" + recentRun;
	String cLegID      = cLegTable + "." + DGPSPConstants.COL_LEGID;
	String cLegConvID  = cLegTable + "." + DGPSPConstants.COL_CONVEYANCEID;

	String convInstanceTable    = DGPSPConstants.CONV_INSTANCE_TABLE + "_" + recentRun;
	String convInstanceID       = convInstanceTable + "." + DGPSPConstants.COL_CONVEYANCEID;

	String joinsToCarrierInstance = "\nand " + instanceID + " = " + itinID +
	  "\nand " + itinLeg + " = " + cLegID +
	  "\nand " + cLegConvID + " = " + convInstanceID;

	return joinsToCarrierInstance;
  }

  protected boolean appendClause (String col, List constraints, StringBuffer sb, boolean justDidClause) {
	if (constraints.isEmpty() || (col==null))
	  return false;
	
	if (justDidClause) sb.append("\nand ");

	sb.append("(");
	
	for (Iterator iter = constraints.iterator (); iter.hasNext ();) {
	  String constraint = (String) iter.next ();
	  sb.append (col + " = '" + constraint + "'");
	  if (iter.hasNext ())
		sb.append (" or ");
	}

	sb.append (")");
	return true;
  }

  protected boolean specialAppendClause (String col, String col2, List constraints, StringBuffer sb, boolean justDidClause) {
	if (constraints.isEmpty())
	  return false;

	boolean selfPropelled = false;
	boolean ground = false;

	if (justDidClause) sb.append("\nand ");

	sb.append("(");
	
	StringBuffer range = new StringBuffer();
	range.append("(");

	for (Iterator iter = constraints.iterator (); iter.hasNext ();) {
	    String constraint = (String) iter.next ();
	    if (constraint.equals(CategoryNode.GROUND) || constraint.equals(CategoryNode.SELF)) {
		range.append(DGPSPConstants.CONV_TYPE_TRUCK+","+
			     DGPSPConstants.CONV_TYPE_TRAIN+","+
			     DGPSPConstants.CONV_TYPE_SELF_PROPELLABLE+","+
			     DGPSPConstants.CONV_TYPE_FACILITY);
		ground = true;
	    } else if (constraint.equals(CategoryNode.AIR)) {
		range.append(DGPSPConstants.CONV_TYPE_PLANE+","+
			     DGPSPConstants.CONV_TYPE_PERSON);
	    } else if (constraint.equals(CategoryNode.SEA)) {
		range.append(DGPSPConstants.CONV_TYPE_SHIP+","+
			     DGPSPConstants.CONV_TYPE_DECK);
	    }
	    if (constraint.equals(CategoryNode.SELF)) selfPropelled = true;
	    if (iter.hasNext()) range.append(",");
	}
	range.append(")");
	
	sb.append (col + " in " + range);

	sb.append (")");
	
	if (ground) {
	  sb.append("\nand (");
	  sb.append(col2);
	  sb.append(" = ");
	  if (selfPropelled)
	    sb.append("1");
	  else
	    sb.append("0");
	  sb.append(")");
	}

	return true;
  }

  public String toString () {
	StringBuffer sb = new StringBuffer ();
	sb.append ("Units : ");
	
	sb.append (constraintList (unitDBUIDs));
	if (!carrierTypes.isEmpty ())  sb.append (" | Carrier Types : ");

	sb.append (constraintList (carrierTypes));
	if (!carrierInstances.isEmpty ())  sb.append (" | Instances : ");

	sb.append (constraintList (carrierInstances));
	if (!cargoTypes.isEmpty ())  sb.append (" | Cargo Types : ");

	sb.append (constraintList (cargoTypes));
	if (!cargoInstances.isEmpty ())  sb.append (" | Cargo : ");

	sb.append (constraintList (cargoInstances));

	if (other.length () > 0)  {
	  sb.append (" | Other : ");
	  sb.append (other);
	}

	return sb.toString();
  }

  protected String constraintList (List constraints) 
  {
	String constraint = "";
	for (Iterator iter = constraints.iterator (); iter.hasNext ();) {
	  constraint = constraint + (String) iter.next () + (iter.hasNext() ? ", " : "");
	}
	return constraint;
  }
  
}
