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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.mlm.ui.psp.transit.data.legs.Leg;
import org.cougaar.mlm.ui.psp.transit.data.prototypes.Prototype;
import org.cougaar.mlm.ui.psp.transit.data.population.ConveyancePrototype;

/**
 * Constants for the DataGathererPSP
 * @author Benjamin Lubin; last modified by: $Author: mthome $
 *
 * @since 2/18/01
 **/
public interface DGPSPConstants{

  //Constants:
  ////////////

  //Queries:
  //========

  public static final String QUERY_REGISTRATION="beginSession";
  public static final String QUERY_LEG="getLegs";
  public static final String QUERY_LOCATION="getLocations";
  public static final String QUERY_POPULATION="getPopulation";
  public static final String QUERY_INSTANCE="getInstances";
  public static final String QUERY_PROTOTYPE="getPrototypes";
  public static final String QUERY_ROUTES="getRoutes";
  public static final String QUERY_CONVOYS="getConvoys";
  public static final String QUERY_UNREGISTRATION="endSession";

  //Leg Query:
  //==========

  //Table base names:
  public static final String ASSET_ITINERARY_TABLE="assetitinerary";
  public static final String CONVEYED_LEG_TABLE="conveyedleg";
  
  //ASSET_ITINERARY_TABLE cols:
  public static final String COL_ASSETID = "assetid";
  public static final String COL_LEGID = "legid";

  //also needed for CONVEYED_LEG_TABLE cols:
  public static final String COL_STARTTIME = "starttime";
  public static final String COL_ENDTIME = "endtime";
  public static final String COL_READYAT = "readyattime";
  public static final String COL_EARLIEST_END = "earliestendtime";
  public static final String COL_BEST_END = "bestendtime";
  public static final String COL_LATEST_END = "latestendtime";
  public static final String COL_STARTLOC = "startlocid";
  public static final String COL_ENDLOC = "endlocid";
  public static final String COL_LEGTYPE = "legtype";
  public static final String COL_CONVEYANCEID = "convid";
  public static final String COL_ROUTEID="routeid";
  public static final String COL_MISSIONID="missionid";
  
  public static final int LEG_TYPE_UNKNOWN=Leg.LEG_TYPE_UNKNOWN;
  /**Guaranteed should not overlap for a given asset or conveyance**/
  public static final int LEG_TYPE_TRANSPORTING=Leg.LEG_TYPE_TRANSPORTING;
  /**Loading cargo (cargo does not show up on this leg)**/
  public static final int LEG_TYPE_LOADING=Leg.LEG_TYPE_LOADING;
  /**Unloading cargo (cargo does not show up on this leg)**/
  public static final int LEG_TYPE_UNLOADING=Leg.LEG_TYPE_UNLOADING;
  /**Positioning to pick up cargeo (no cargo on this leg)**/
  public static final int LEG_TYPE_POSITIONING=Leg.LEG_TYPE_POSITIONING;
  /**Returning from dropping off cargeo (no cargo on this leg)**/
  public static final int LEG_TYPE_RETURNING=Leg.LEG_TYPE_RETURNING;
  /**Refueling**/
  public static final int LEG_TYPE_REFUELING=Leg.LEG_TYPE_REFUELING;
  public static final int LEG_TYPE_LAST_TYPE=LEG_TYPE_REFUELING;

  public static final String[] LEG_TYPES={"Unknown",
					  "Transporting",
					  "Loading",
					  "Unloading",
					  "Positioning",
					  "Returning",
					  "Refueling"};

  //Instance Query:
  //===============

  //Table base names:
  public static final String ASSET_INSTANCE_TABLE="assetinstance";
  
  //also needed for ASSET_INSTANCE_TABLE:
  public static final String COL_AGGREGATE="aggnumber";
  public static final String COL_OWNER="ownerid";
  public static final String COL_PROTOTYPEID="prototypeid";
  public static final String COL_NAME="name";
  public static final String COL_ALP_ITEM_NOMEN="itemnomenclature";

  //Prototype Query:
  //================

  //Table base names:
  public static final String ASSET_PROTOTYPE_TABLE="assetprototype";
  public static final String CARGO_CAT_CODE_DIM_TABLE="cargocatcodedim";
  
  //also needed for ASSET_PROTOTYPE_TABLE:
  // Dimensions : 
  // Width  is in meters
  // Height is in meters
  // Depth  is in meters
  // Weight is in grams
  public static final String COL_PARENT_PROTOTYPEID="parentprototypeid";
  public static final String COL_ASSET_CLASS="assetclass";
  public static final String COL_ASSET_TYPE="assettype";
  public static final String COL_WEIGHT="weight"; // grams
  public static final String COL_WIDTH="width";   // meters
  public static final String COL_HEIGHT="height"; // meters
  public static final String COL_DEPTH="depth";   // meters
  public static final String COL_AREA="area";     // square meters
  public static final String COL_VOLUME="volume"; // cubic meters
  public static final String COL_CARGO_CAT_CODE="ccc"; // three letter code
  public static final String COL_ALP_TYPEID="alptypeid";
  public static final String COL_ALP_NOMENCLATURE="alpnomenclature";
  public static final String COL_IS_LOW_FIDELITY="isLowFidelity";

  //Asset classes:
  public static final int ASSET_CLASS_UNKNOWN = Prototype.ASSET_CLASS_UNKNOWN;
  public static final int ASSET_CLASS_1 = Prototype.ASSET_CLASS_1;
  public static final int ASSET_CLASS_2 = Prototype.ASSET_CLASS_2;
  public static final int ASSET_CLASS_3 = Prototype.ASSET_CLASS_3;
  public static final int ASSET_CLASS_4 = Prototype.ASSET_CLASS_4;
  public static final int ASSET_CLASS_5 = Prototype.ASSET_CLASS_5;
  public static final int ASSET_CLASS_6 = Prototype.ASSET_CLASS_6;
  public static final int ASSET_CLASS_7 = Prototype.ASSET_CLASS_7;
  public static final int ASSET_CLASS_8 = Prototype.ASSET_CLASS_8;
  public static final int ASSET_CLASS_9 = Prototype.ASSET_CLASS_9;
  public static final int ASSET_CLASS_10 = Prototype.ASSET_CLASS_10;
  public static final int ASSET_CLASS_CONTAINER = 
    Prototype.ASSET_CLASS_CONTAINER;
  public static final int ASSET_CLASS_PERSON = 
    Prototype.ASSET_CLASS_PERSON;

  /**All assets that are NOT MilVan, Container, Palet**/
  public static final int ASSET_TYPE_ASSET = 
    Prototype.ASSET_TYPE_ASSET;
  /**All assets that are MilVan, Container, Palet**/
  public static final int ASSET_TYPE_CONTAINER = 
    Prototype.ASSET_TYPE_CONTAINER;

  //Propulation Query:
  //================

  //Table base names:
  public static final String CONV_INSTANCE_TABLE="conveyanceinstance";
  public static final String CONV_PROTOTYPE_TABLE="conveyanceprototype";

  //Also needed for CONV_INSTANCE_TABLE:
  public static final String COL_BUMPERNO="bumperno";
  public static final String COL_BASELOC="baselocid";
  public static final String COL_SELFPROP="selfprop";//Int 0=false 1=true

  //Also needed for CONV_PROTOTYPE_TABLE:
  // Dimensions : 
  // Volume is in liters
  // Area   is in square meters
  // Weight is in grams
  // Speed  is in miles/hour
  public static final String COL_CONVEYANCE_TYPE="conveyancetype";
  public static final String COL_VOL_CAP="volumecapacity";    // liters
  public static final String COL_AREA_CAP="areacapacity";     // square meters
  public static final String COL_WEIGHT_CAP="weightcapacity"; // grams
  public static final String COL_AVE_SPEED="avespeed";        // miles/hour
  
  //types:
  public static final int CONV_TYPE_UNKNOWN=
    ConveyancePrototype.ASSET_TYPE_UNKNOWN;
  public static final int CONV_TYPE_TRUCK=
    ConveyancePrototype.ASSET_TYPE_TRUCK;
  public static final int CONV_TYPE_TRAIN=
    ConveyancePrototype.ASSET_TYPE_TRAIN;
  public static final int CONV_TYPE_PLANE=
    ConveyancePrototype.ASSET_TYPE_PLANE;
  public static final int CONV_TYPE_SHIP=
    ConveyancePrototype.ASSET_TYPE_SHIP;
  public static final int CONV_TYPE_DECK=
    ConveyancePrototype.ASSET_TYPE_DECK;
  public static final int CONV_TYPE_PERSON=
    ConveyancePrototype.ASSET_TYPE_PERSON;
  public static final int CONV_TYPE_FACILITY=
    ConveyancePrototype.ASSET_TYPE_FACILITY;
  public static final int CONV_TYPE_SELF_PROPELLABLE=
    ConveyancePrototype.ASSET_TYPE_SELF_PROPELLABLE;

  public static final String[] CONVEYANCE_TYPES={"Unknown",
						 "Truck",
						 "Train",
						 "Plane",
						 "Ship",
						 "Self-propelled",
						 "Deck",
						 "Person",
						 "Facility"};
  
  //Location Query:
  //===============

  //Table base names:
  public static final String LOCATIONS_TABLE="locations";
  
  //Also needed for LOCATIONS_TABLE:
  public static final String COL_LOCID="locid";
  public static final String COL_LAT="lat";
  public static final String COL_LON="lon";
  public static final String COL_GEOLOC="geoloc";
  public static final String COL_ICAO="icao";
  public static final String COL_PRETTYNAME="prettyname";

  //Route Query:
  //============

  //Table base names:
  public static final String ROUTE_ELEMENTS_TABLE="route_elements";
  
  //Also needed for ROUTES_TABLE:
  public static final String COL_ROUTE_ELEM_NUM="elem_num";

  //Convoy Query:
  //============

  //Table base names:
  public static final String CONVOYS_TABLE="convoys";
  public static final String CONVOY_MEMBER_TABLE="convoymember";
  
  //Also needed for CONVOY TABLES:
  public static final String COL_CONVOYID="convoyid";


  //Arrival Time Query:
  //============

  // Table base names:
  public static final String ARRIVAL_TIME_TABLE="preferredtimes";

  //Also needed for ARRIVAL TIME TABLES:
  public static final String COL_PREFERREDARRIVALTIME="arrivalpreference";

  //Status Table:
  //============

  public static final String STATUS_TABLE="statustable";

  public static final String MANIFEST_TABLE="manifest";
  public static final String COL_MANIFEST_ITEM_ID="manifestid";
  //Members:
  //////////
}

