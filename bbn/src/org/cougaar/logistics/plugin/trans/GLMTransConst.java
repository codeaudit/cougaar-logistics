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
package org.cougaar.logistics.plugin.trans;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.plan.Role;

/**
 * Class to hold constants for the GLMTrans tree.
 * Values should probably only be included here if they are public static final
 *
 */
public class GLMTransConst {

  /** GENERIC **/

  /** GLOBAL MODE **/
  // MODE prep
  public  static final String MODE = "M";
  // MODE prep strings
  public static final String AIR = "AIR";
  public static final String SEA = "SEA";
  public static final String GROUND = "GROUND";
  public static final String NULL = "NO TRANSPORT";

  public static final String LOW_FIDELITY = "LOW_FIDELITY";
  public static final String PREREQ_TASK = "PREREQ_TASK";

  /** GLOBAL SEA **/
  // TASKEDTO prep
  public static final String TASKEDTO = "T";
  // TASKEDTO prep strings
  public static final String MISSION = "MISSION";
  public static final String THEATER = "THEATER";
  public static final String THEATERPORT = "THEATERPORT";
  public static final String PORT = "PORT"; // may go away?
  public static final String POE = "POE";
  public static final String CONUSPORT = "CONUSPORT";
  public static final String SHIP = "SHIP";


  public static final String VISHNU_DATA = "VISHNU_DATA";
  public static final String SEAROUTE_DISTANCE = "SEAROUTE_DISTANCE";
  public static final String READYAT = "READYAT";

  // Ship types
  public static final String AMMO_SHIP = "Ammo";
  public static final String FSS_RORO_COMBO_SHIP = "FSS|RORO|Combo";
  public static final String LMSR_RORO_COMBO_SHIP = "LMSR|RORO|Combo";
  public static final String MPS_RORO_COMBO_SHIP = "MPS|RORO|Combo";
  public static final String RORO_SHIP = "RORO";
  public static final String BREAKBULK_CONTAINER_SHIP = "BreakBulk|Container";
  public static final String BREAKBULK_SHIP = "BreakBulk";
  public static final String CONTAINER_SHIP = "Container";
  public static final String LASH_SHIP = "LASH";

  // Preposition for port time estimation
  public static final String PORT_DUR = "PORT_DUR";
  public static final long DEFAULT_PORT_DUR = (long)(60 * 60 * 24 * 2.5); // millis

  public static final String PACK_FAILURE = "Packing Failure";

  //* Preposition for POD offload time hack
  public static final String POD_OFFLOAD_DUR = "POD_OFFLOAD_DUR";

  // Preposition for prepo integrity support
  public static final String TOTAL_PREPO_TASKS = "TOTAL_PREPO_TASKS";

  // Load order
  public static final String MANIFEST = "MANIFEST";
  public static final String MANIFESTDATE = "MANIFESTDATE";

  // Prepositions for unit integrity support
  public static final String UNIT_DESCRIPTION = "unit";

  //* Prepositions for unit integrity support
  public static final String TOTAL_UNIT_WEIGHT = "w";
  public static final String TOTAL_UNIT_AREA = "a";
  public static final String TOTAL_UNIT_VOLUME = "v";
  public static final String TOTAL_UNIT_CONTAINERS = "c";

  // CONUSGround needs to be the only of whatever role we assign to it/it to
  public static final Role CONUSGROUND = Role.getRole("CONUSGroundTransportationProvider");
  public static final ClusterIdentifier GROUND_CLUSTER_ID = 
    new ClusterIdentifier("GlobalGround");
  public static final ClusterIdentifier THEATER_CLUSTER_ID = 
    new ClusterIdentifier("JMCC");

  public static final String AIRCREW_READY = "AIRCREW_READY";
  public static final String AIRCREW_JOB = "AIRCREW_JOB";
  public static final String IMPLIED = "IMPLIED";               // hint to gui to potentially ignore this task or itinerary leg
  public static final String IMPLIED_LOAD = "IMPLIED_LOAD";     // hint to gui to potentially ignore this task or itinerary leg
  public static final String IMPLIED_UNLOAD = "IMPLIED_UNLOAD"; // hint to gui to potentially ignore this task or itinerary leg

  public static final String PILOT = "PILOT";
  public static final String COPILOT = "CO-PILOT";
  public static final String FLIGHT_ENGINEER = "FLIGHT ENGINEER";
  public static final String BOOM_OPERATOR = "BOOM OPERATOR";

  public static final Role AMMUNITION_SEA_PORT = Role.getRole("AmmunitionSeaPort");
  public static final Role AMMO_SEA_PORT = Role.getRole("AmmoSeaPort");
  public static final Role GENERIC_SEA_PORT = Role.getRole("GenericSeaPort");

  // CONUSGroundPredictor ---
  // The CODES are for decoding the returning AspectValues into the given
  // strings since there do not exist the appropriate AspectTypes for strings.
  
  public static final String RAIL = "RAIL";
  public static final int RAIL_CODE = 1;
  public static final String TRUCK = "TRUCK";
  public static final int TRUCK_CODE = 2;

  public static final String DUMMY = "Dummy";
  public static final String FORLOADING = "ForLoading";
  public static final String ONDECK = "OnDeck";
  public static final String INSEQUENCE = "InSequence";

  public static final String SequentialSchedule = "SequentialSchedule";
  public static final String SCHEDULE_ELEMENT = "ScheduleElement";
    public static final String FROMTABLE = "FromTable";
  
  public static final org.cougaar.planning.ldm.plan.Role THEATER_MCC_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("TheaterStrategicTransportationProvider");

  // Air Roles
  public static final org.cougaar.planning.ldm.plan.Role GLOBAL_AIR_ROLE = 
    org.cougaar.planning.ldm.plan.Role.getRole("AirTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role ORGANIC_AIR_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("OrganicAirTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role VIRTUAL_AIR_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("CommercialAirTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role C17_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("C17TransportProvider");
  public static final org.cougaar.planning.ldm.plan.Role C5_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("C5TransportProvider");
  public static final org.cougaar.planning.ldm.plan.Role C130_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("C130TransportProvider");
  public static final org.cougaar.planning.ldm.plan.Role MD11_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("CommercialTransportProvider");
  public static final org.cougaar.planning.ldm.plan.Role B747_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("CommercialTransportProvider");
  public static final org.cougaar.planning.ldm.plan.Role SQUADRON_ROLE = 
    org.cougaar.planning.ldm.plan.Role.getRole("AirCrewProvider");
  public static final org.cougaar.planning.ldm.plan.Role THEATER_AIR_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("TheaterAirTransportationProvider");

  // Sea Roles
  public static final org.cougaar.planning.ldm.plan.Role GLOBAL_SEA_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("SeaTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role SHIP_PACKER_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("ShipPackingTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role GLOBAL_SEA_AMMO_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("AmmoTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role GENERIC_PORT_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("GenericSeaPort");
  public static final org.cougaar.planning.ldm.plan.Role AMMO_PORT_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("AmmunitionSeaPort");
  public static final org.cougaar.planning.ldm.plan.Role THEATER_SEA_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("TheaterSeaTransportationProvider");

  // Ground Roles
  public static final org.cougaar.planning.ldm.plan.Role CONUS_GROUND_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("CONUSGroundTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role COMMERCIAL_GROUND_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("CommercialGroundTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role THEATER_GROUND_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("TheaterGroundTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role ITO_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("GroundTransportationProvider");
  public static final org.cougaar.planning.ldm.plan.Role TRANSCAP_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("TranscapProvider");
  public static final org.cougaar.planning.ldm.plan.Role THEATER_FORT_ROLE =
    org.cougaar.planning.ldm.plan.Role.getRole("TheaterTruckingProvider");
}

