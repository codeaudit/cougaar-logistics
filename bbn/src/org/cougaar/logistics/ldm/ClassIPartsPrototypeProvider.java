/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.logistics.ldm;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Area;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.measure.Mass;
import org.cougaar.planning.ldm.measure.Volume;
import org.cougaar.glm.ldm.asset.HumanitarianDailyRation;
import org.cougaar.glm.ldm.asset.ClassISubsistence;
import org.cougaar.glm.ldm.asset.NewMovabilityPG;
import org.cougaar.glm.ldm.asset.NewCostPG;
import org.cougaar.glm.ldm.asset.CostPGImpl;
import org.cougaar.glm.ldm.asset.NewPackagePG;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.asset.NewSupplyClassPG;
import org.cougaar.glm.ldm.QueryLDMPlugin;

import org.cougaar.logistics.ldm.asset.RationPG;
import org.cougaar.logistics.ldm.asset.NewRationPG;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

/**
 * Creates and rototype and their property groups for Subsistence
**/
public class ClassIPartsPrototypeProvider extends QueryLDMPlugin {

  private static Hashtable otherAssetNomenclatures;
  private Hashtable propertyGroupTable_ = new Hashtable(); 
  private static HashMap emptyHashMap = new HashMap(0); 
  private static Vector emptyVector = new Vector(0); 
  private LoggingService logger;

  //
  // NOTE that the static declarations here do decrease the queries overall within
  // a single JVM.  However, under certain circumstances, it may not be appropriate
  // to have all clusters sharing a single Map.  -- LLG 
  //
  private static Map supplementQueryResult = new HashMap(); 

  // Builds a hash table of nsn
  static {
    otherAssetNomenclatures= new Hashtable(1);
    otherAssetNomenclatures.put ("NSN/8970013750516", "HumanitarianDailyRation");
  } // static

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  /**
   * Method to determine if this class can handle an item typeid of type class_hint
   * @param typeid identifier for item (an NSN)
   * @param class_hint hint as to class of item being requested
   * @return boolean representing whether this prototype provider can handle
   *  this item
   **/
  public boolean canHandle(String typeid, Class class_hint) {
    logger.debug ("canHandle (typeid:"+typeid+")");

    Boolean protoProvider = (Boolean) myParams_.get ("PrototypeProvider");
    if ((protoProvider == null) || (protoProvider.booleanValue())) {
      if (class_hint == null) {
	if (typeid.startsWith("NSN/")) 
	  return true;
      } else {
	String class_name = class_hint.getName();
	if (typeid.startsWith("NSN/") && (class_name.equals ("ClassISubsistence")
					  || class_name.equals("HumanitarianDailyRation"))) {
	  return true;
	} // if
      } // if
    } // if
    logger.debug ("canHandle() could not handle <"+ class_hint + "> for "+ typeid);
    return false;
  } // canHandle


  /**
   * Makes a prototype for as Asset of an item of type class_hint
   * @param type_name identifier for item e.g a 
   * @param class_hint hint as to class of item being requested
   * @return Asset Prtotype that's created.  Note that Asset is 
   * actually a prototype here
   **/
  public Asset makePrototype (String type_name, Class class_hint) {
    String class_name = null;
    if (class_hint != null) {
      class_name = class_hint.getName();
      if (!(class_name.equals("ClassISubsistence") 
	    || class_name.equals("HumanitarianDailyRation"))) {
	logger.debug ("makePrototype() could not make prototype for<" + type_name);
	return null;
      } // if
    } //end (class_hint != null)
    if (class_name == null) {
      if (type_name.startsWith("NSN/89")) {
	if (otherAssetNomenclatures.containsKey(type_name)) {
	  class_name = (String)otherAssetNomenclatures.get(type_name);
	} else {
	  class_name = "ClassISubsistence";
	} // if
      } else {
	logger.debug ("makePrototype() could not make prototype for<" + type_name);
	return null;
      } // if
    } // if
    String nomenclature = getNomenclature (type_name, class_name);
    if (nomenclature == null) {
      //If the nomenclature can't be found, we can't handle this item after all.
      return null;
    } // if
    // Create the Asset prototype
    logger.debug ("makePrototype() making prototype for<" + type_name);
    return newAsset (type_name,class_name, nomenclature);
  } // makePrototype


  /**
   * Convenience method to read the query statement from a file
   * and execute the query.
   * @param query_name String refers to the specific query statement
   * @param a String for first query variable
   * @param b String for second query varaible
   * @return Vector containing the query result
   **/
  protected Vector doQuery (String query_name, String a, String b) {
    String query = 
      substituteList((String)fileParameters_.get(query_name), a, b);
    if (query == null) {
      logger.error ("doQuery(), query string from file is null"); 
      return null;
    } // if
    Vector holdsQueryResult;
    try {
      holdsQueryResult = executeQuery(query);
    } catch (Exception ee) {
      logger.error ("doQuery(), DB query failed. query= "+ query+ "\n"+ee.toString());
      return null;
    } // try
    return holdsQueryResult;
  } // doQuery

  /**
   * Gets a list of supplement NSNs for a specific meal_type and specific alternate_name
   * @param meal_type type of meals: BREAKFAST, LUNCH/DINNER
   * @param nomenclature name of the food item e.g MEAL READY TO EAT
   * @return HashMap of NSNs and their rates 
   **/
  protected HashMap getSupplementalList (String meal_type, String alternate_name) {
    QueryHashKey key = new QueryHashKey(meal_type, alternate_name);
    HashMap result;
    synchronized (supplementQueryResult) {
      result = (HashMap) supplementQueryResult.get(key);
      if (result == null) {
	Vector holdsQueryResult = doQuery("ClassISupplementList", meal_type, alternate_name);
	if (holdsQueryResult.isEmpty()) {
	  supplementQueryResult.put(key, emptyHashMap);
	  return emptyHashMap;
	} else {
	  // parse results
	  String  typeIDPrefix = "NSN/";
	  result = new HashMap(5);
	  for(int i = 0; i < holdsQueryResult.size(); i++) {
	    Object[]  row = ( (Object[])holdsQueryResult.elementAt(i));
	    result.put(typeIDPrefix+(String)row[0], (BigDecimal)row[1]);
	  } // for
	  supplementQueryResult.put(key, result);
	} // if
      } // if
      return result;
    } // synch
  } // getSupplementalList


  /**
   * Method to retrieve the nomenclature for the item in question
   * @param type_id identifier for item e.g a nsn 
   * @param type class type of item being requested
   * @return Nomenclature
   */
  protected String getNomenclature (String type_id, String type) {
    String item_id = type_id.substring(type_id.indexOf("/")+1);  // The NSN
    String query = substituteNSN((String)fileParameters_.get("ClassIData"), item_id, ":nsns");
    if (query == null) {
      logger.error("doQuery(), query string from file is null"); 
      return null;
    } 
    Vector result;
    try {
      result = executeQuery(query);
    } catch (Exception ee) {
      logger.error("doQuery(), DB query failed. query= "+ query+ "\n"+ee.toString());
      return null;
    }      
    if (result.isEmpty()) {
      return null;
    } // if
    // parse results
    Object row[] = (Object[])result.firstElement();
    String nomen = (String)row[0];
    if (nomen == null) {
      return null;
    } // if
    String meal_type = (String)row[1];
    int rotation_day = intValue(row[3]);

    // Optimize this later.
    if (meal_type == null ) {
      meal_type = "";
    } // if
    nomen = nomen+" "+meal_type+" ";
    if (rotation_day > 0) {
      nomen += rotation_day;
    } // if
    Vector pgs = null;
    pgs = parseSubsistence(row, type_id, type);
    if (pgs != null) {
      propertyGroupTable_.put(type_id, pgs);
    } // if
    logger.debug ("getNomenclature() returning " + nomen);
    return nomen;
  } // getNomenclature


  /**
   * Return int value else -1 if it is null
   * @param x Object whose int value will be returned
   * @return int value of the object
   **/
  private int intValue (Object x) {
    return (x == null) ? -1 : (((BigDecimal) x).intValue());
  } // intValue

  /**
   * Return double value else -1 if it is null
   * @param x Object whose double value will be returned
   * @return double value of the object
   **/
  private double doubleValue (Object x) {
    return (x == null) ? -1.0 : (((BigDecimal) x).doubleValue());
  } // doubleValue

  /**
   * Return long value else -1 if it is null
   * @param x Object whose long value will be returned
   * @return long value of the object
   **/
  private long longValue(Object x) {
    return (x == null) ? -1 : ((long)((BigDecimal) x).longValue());
  } // longValue


  // Method to parse the results of the query
  private Vector parseSubsistence (Object row[], String typeID, String type) {
    String nomenclature = (String)row[0];
    String meal_type = (String)row[1];
    String ui = (String)row[2];
    int rotation_day = intValue(row[3]);
    double weight= doubleValue(row[4]);
    String alternate_name = (String)row[5];
    long count_per_ui = longValue(row[6]);
    String unit_of_pack = (String)row[7];
    double vol_cubic_feet =doubleValue(row[8]);
    double cost = doubleValue(row[9]);
    if (ui == null) {
      logger.debug ("Unit of Issue was null, seting to EA !!!!!!!!!!!!!");
      ui = "EA";
    } // if
    Vector pgs = createPackagePGs(nomenclature, alternate_name, ui, 
				  weight, count_per_ui,vol_cubic_feet);
    PropertyGroup physicalPG = createPhysicalPGs(weight, vol_cubic_feet);
    PropertyGroup movabilityPG = createMovabilityPGs();
    PropertyGroup rationPG = createRationPG(nomenclature, alternate_name, meal_type, 
					    ui, rotation_day, unit_of_pack, typeID, type);
    if (rationPG != null) {
      pgs.add(rationPG);
    } // if
    if (physicalPG != null) {
      pgs.add(physicalPG);
    } // if
    if (movabilityPG != null) {
      pgs.add(movabilityPG);
    } // if
    NewSupplyClassPG supply_pg = 
      (NewSupplyClassPG) org.cougaar.glm.ldm.asset.PropertyGroupFactory.newSupplyClassPG();
    supply_pg.setSupplyClass ("ClassISubsistence");
    supply_pg.setSupplyType ("Subsistence");
    pgs.add(supply_pg);
    NewCostPG pg = new CostPGImpl();
    pg.setBreakOutCost(cost);
    pgs.add(pg); 
    return pgs;
  } // parseSubsistence


  /**Creates a MovabilityPG for a given assset*/
  private PropertyGroup createMovabilityPGs() {
    NewMovabilityPG pg2 = PropertyGroupFactory.newMovabilityPG();
    pg2.setCargoCategoryCode ("J3A");
    pg2.setMoveable(true);
    return pg2;
  } // createMovabilityPGs


  /**Creates a RationPG for a given assset*/
  private PropertyGroup createRationPG (String nomenclature, String alternate_name, 
					String meal_type, String ui, int rotationDay,
					String unitOfPack, String typeID, String type) {

    NewRationPG pg = (NewRationPG)getLDM().getFactory().createPropertyGroup(RationPG.class);
    pg.setMealType(meal_type); //null for MRES
    pg.setRationType(alternate_name);
    pg.setUnitOfPack(unitOfPack);
    if (meal_type != null) {
      pg.setMandatorySupplement(getSupplementalList(meal_type, alternate_name));
    } else {
      pg.setMandatorySupplement(emptyHashMap);
    } // if
    return pg;
  } // createRationPG

  /**Creates a PackagePG for a given assset*/
  private Vector createPackagePGs(String nomenclature, String alternate_name, 
				  String unitOfIssue, double weight, long countPerUI, double volCubicFeet){
    Vector pgs = new Vector(1);
    if (unitOfIssue == null) {
      return null;
    } // if
    NewPackagePG pg2 = PropertyGroupFactory.newPackagePG();
    pg2.setCountPerPack(countPerUI);
    pg2.setPackVolume(new Volume(volCubicFeet,Volume.CUBIC_FEET));
    pg2.setUnitOfIssue(unitOfIssue);
    pg2.setPackMass(new Mass(weight, Mass.POUNDS));
    pg2.setPackFootprintArea(new Area((1000.0), Area.SQUARE_INCHES ));//BOGUS
    pg2.setPackHeight(new Distance(1000.0, Distance.INCHES) );//BOGUS
    pgs.add(pg2);
    return pgs;
  } // createPagckagePGs


  /**Creates a PhysicalPG for a given assset*/
  private PropertyGroup createPhysicalPGs (double weight,  double volCubicFeet){
    NewPhysicalPG pg2 = PropertyGroupFactory.newPhysicalPG();
    pg2.setFootprintArea(new Area((1000.0), Area.SQUARE_INCHES ));//BOGUS
    pg2.setHeight (new Distance(1000.0, Distance.INCHES) );//BOGUS
    pg2.setVolume (new Volume(volCubicFeet,Volume.CUBIC_FEET));
    pg2.setMass(new Mass(weight, Mass.POUNDS));
    pg2.setLength (new Distance(100.0, Distance.INCHES) );//BOGUS
    pg2.setWidth (new Distance(500.0, Distance.INCHES) );//BOGUS;
    return pg2;
  } // createPhysicalPGs


  // CDW - looks like it's looking for consumed
  public void fillProperties (Asset anAsset) {
    logger.debug ("fillProperties for " + anAsset);
    Vector pgs = null;
    if ((anAsset instanceof ClassISubsistence) 
	|| (anAsset instanceof HumanitarianDailyRation)) {
      String typeID = anAsset.getTypeIdentificationPG().getTypeIdentification();
      pgs = (Vector) propertyGroupTable_.get(typeID);
    } // if
    if ((pgs != null) && !pgs.isEmpty()) {
      Enumeration pgs_enum = pgs.elements();
      while (pgs_enum.hasMoreElements()) {
	NewPropertyGroup pg = (NewPropertyGroup)pgs_enum.nextElement();
	anAsset.setPropertyGroup(pg);
	logger.debug ("setting PGs for " + anAsset);
      } // while
    } // if
  } // fillProperties


  /**
   * Replaces the ":nsns" in the query with the actual NSN.
   * @param q query string
   * @param nsn actual NSN
   * @param nsnStr  String in the database
   * @return new query
   **/
  public String substituteNSN (String q, String nsn, String nsnStr) {
    String query=null;
    if (q != null) {
      int indx = q.indexOf(nsnStr);
      if (indx != -1) {
	query = q.substring(0,indx) + "'"+nsn+"'";
	if (q.length() > indx+5) {
	  query +=q.substring(indx+5);
	} // if
      } // if
    } // if
    return query;
  } // subsituteNSN


  /**
   * Replaces the ":nsn" in the query with the actual NSN.
   * @param q query string
   * @param nsn actual NSN
   * @param nomenclature 
   * @return new query
   **/
  public String substituteList (String q, String meal_type, String nomenclature) {    
    String query=null;
    String query1 = null;
    if (q != null) {
      query = substituteNSN(q,meal_type, ":meal");
      query1 = substituteNSN(query,nomenclature, ":nomn");
    } // if
    return query1;
  } // substituteList

  /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

  /**
   * Class to avoid concantenating Strings as keys.
   **/
  private class QueryHashKey {
    private String a;
    private String b;

    public QueryHashKey (String n, String s) {
      a = n;
      b = s;
    } // constructor

    public int hashCode() {
      int val = 0;

      if (a != null) 
	val += a.hashCode();
      if (b != null) 
	val += b.hashCode();
      return val;
    } // hashCode

    public boolean equals(Object o) {
      if (o instanceof QueryHashKey) {
	QueryHashKey thekey = (QueryHashKey) o;
	if (this.a != thekey.a) {
	  if (this.a == null) {
	    return false;
	  } // if
	  if ( ! this.a.equals(thekey.a)) {
	    return false;
	  } // if
	} // if
	if (this.b != thekey.b) {
	  if (this.b == null) {
	    return false;
	  } // if
	  if ( ! this.b.equals(thekey.b)) {
	    return false;
	  } // if
	} // if
	return true;
      } // if
      return false;
    } // equals
  } // QueryHashKey

} // ClassIPartsPrototypeProvider
