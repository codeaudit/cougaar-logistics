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

package org.cougaar.logistics.ldm.asset;

import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.glm.ldm.oplan.OpTempo;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.logistics.plugin.utils.*;
import org.cougaar.logistics.ldm.MEIPrototypeProvider;

import java.math.BigDecimal;

import java.util.*;

public class RepairPartConsumerBG extends ConsumerBG {

  protected RepairPartConsumerPG myPG;
  MEIPrototypeProvider parentPlugin;
  final static String supplyType = "Consumable";
  final static int MAX_PARTS = 20;
  final static double ZERO_X = 0.0;
  final static double RESERVE_X = 0.2;
  final static double LOW_X = 0.4;
  final static double MEDIUM_X = 0.6;
  final static double HIGH_X = 1.0;
  final static String LOW_OPTEMPO = OpTempo.LOW.toUpperCase();
  final static String MEDIUM_OPTEMPO = OpTempo.MEDIUM.toUpperCase();
  final static String HIGH_OPTEMPO = OpTempo.HIGH.toUpperCase();
  ArrayList consumptionRates = new ArrayList();
  ArrayList parts = new ArrayList();
  private transient LoggingService logger;

  public RepairPartConsumerBG(RepairPartConsumerPG pg) {
    myPG = pg;
  }

  public void initialize(MEIPrototypeProvider plugin) {
    parentPlugin = plugin;
    logger = parentPlugin.getLoggingService(this);
   }

  public List getPredicates() {
    // Add limit resources policy
    ArrayList predList = new ArrayList();
    String typeId = myPG.getMei().getTypeIdentificationPG().getTypeIdentification();
    predList.add(new ConsumerPredicate(typeId));
    predList.add(new OrgActivityPred());
    return predList;
  }

  public Schedule getParameterSchedule(Collection col, TimeSpan span) {
    Schedule paramSchedule = null;
    Vector params = new Vector();
    Iterator predList = col.iterator();
    UnaryPredicate predicate;
    // DEBUG
    String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
//     if (myOrgName.indexOf("35-ARBN") >= 0) {
//       System.out.println("getParamSched() Asset is "+
// 			 myPG.getMei().getTypeIdentificationPG().getTypeIdentification());
//     }
    while (predList.hasNext()) {
      Iterator list = ((Collection)predList.next()).iterator();
      predicate = (UnaryPredicate)list.next();
      if (predicate instanceof ConsumerPredicate) {
	Schedule consumerSched = 
	  parentPlugin.getScheduleUtils().createConsumerSchedule((Collection)list.next());
// 	if (myOrgName.indexOf("35-ARBN") >= 0) {
// 	  System.out.println("getParamSched() ConsumerSched "+consumerSched);
// 	}
 	params.add(parentPlugin.getScheduleUtils().convertQuantitySchedule(consumerSched));
      } else if (predicate instanceof OrgActivityPred) {
	Schedule orgActSched = 
	  parentPlugin.getScheduleUtils().createOrgActivitySchedule((Collection)list.next());
 	params.add(orgActSched);
// 	if (myOrgName.indexOf("35-ARBN") >= 0) {
// 	  System.out.println("getParamSched() OrgActSched "+orgActSched);
// 	}
      } else {
 	logger.error("getParameterSchedule: unknown predicate");
      }
    }
    paramSchedule = parentPlugin.getScheduleUtils().getMergedSchedule(params);
//     if (myOrgName.indexOf("35-ARBN") >= 0) {
//       System.out.println("getParamSched() MERGED "+paramSchedule);
//     }

    return paramSchedule;
  }

  public Rate getRate(Asset asset, List params) {
    Rate r = null;
    // DEBUG
    String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
    if (params == null) {
      logger.error("getRate() params null");
      if (myOrgName.indexOf("35-ARBN") >= 0) {
	System.out.println("getRate() params null for "+
			   asset.getTypeIdentificationPG().getNomenclature());
      }
      return r;
    }
    Double qty = (Double)params.get(0);
    OrgActivity orgAct = (OrgActivity)params.get(1);
    if (orgAct == null) {
      logger.debug("getRate() orgAct null for "+
		   asset.getTypeIdentificationPG().getNomenclature());
//       if (myOrgName.indexOf("35-ARBN") >= 0) {
// 	System.out.println("getRate() orgAct null for "+
// 			   asset.getTypeIdentificationPG().getNomenclature());
//       }
      return r;
    }
    int idx = parts.indexOf(asset);
    Double d = (Double)consumptionRates.get(idx);
    if (d == null) {
      if (myOrgName.indexOf("35-ARBN") >= 0) {
	System.out.println("getRate()  no repair part consumption for "+
			   asset.getTypeIdentificationPG().getNomenclature());
      }
      return r;
    }

    double  dailyRate = d.doubleValue();
    if (orgAct.getOpTempo().toUpperCase().equals(HIGH_OPTEMPO)) {
      dailyRate = dailyRate * HIGH_X;
    } else if (orgAct.getOpTempo().toUpperCase().equals(MEDIUM_OPTEMPO)) {
      dailyRate = dailyRate * MEDIUM_X;
    } else if (orgAct.getOpTempo().toUpperCase().equals(LOW_OPTEMPO)) {
      dailyRate = dailyRate * LOW_X;
    } else {
      dailyRate = dailyRate * ZERO_X;
    }

    r = CountRate.newEachesPerDay (dailyRate*qty.doubleValue());
//     if (myOrgName.indexOf("35-ARBN") >= 0) {
//       System.out.println("getRate() Rate is  "+ r +", for "+
// 			 asset.getTypeIdentificationPG().getNomenclature()+
// 			 " at "+orgAct.getOpTempo());
//     }
    return r;
  }

  public Collection getConsumed() {
    if (parts.isEmpty()) {
      Vector result = parentPlugin.lookupAssetConsumptionRate(myPG.getMei(), supplyType, 
							      myPG.getService(), myPG.getTheater());
      if (result == null) {
	logger.debug("getConsumed(): Database query returned EMPTY result set for "+
		     myPG.getMei()+", "+supplyType);
      } else {
	parseResults(result);
      }
    }
    return parts;
  }

  public Collection getConsumed(int x) {
    return getConsumed();
  }

  public Collection getConsumed(int x, int y) {
    return getConsumed();
  }

  protected void parseResults (Vector result) {
    String mei_nsn, typeid, optempo;
    double dcr;
    Asset newAsset;
    HashMap map = null;
    Enumeration results = result.elements();
    Object row[];

    for (int i=0; results.hasMoreElements() && (i < MAX_PARTS); i++) {
      row = (Object [])results.nextElement();
      mei_nsn = (String) row[0];
      logger.debug("RepairParts: parsing results for MEI nsn: " + mei_nsn);
      typeid = "NSN/"+(String) row[1];
      newAsset = parentPlugin.getPrototype(typeid);
      if (newAsset != null) {
	optempo = (String) row[2]; // Query only retrieves HIGH optempo, ignore
	dcr = ((BigDecimal) row[3]).doubleValue();
	parts.add(newAsset);
	consumptionRates.add(new Double(dcr));
	logger.debug("parseResult() for part "+i+" "+newAsset+", MEI "+mei_nsn+
		     ", DCR "+dcr+", Optempo "+optempo);
	//DEBUG
	String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
// 	if (myOrgName.indexOf("35-ARBN") >= 0) {
// 	  System.out.println("parseResult() for "+newAsset+", MEI "+mei_nsn+
// 			     ", DCR "+dcr+", Optempo "+optempo);
// 	}
      } else {
	logger.debug("parseResult() could not getPrototype for "+typeid+", "+supplyType);
      }
    }
  } // parseResults

  public PGDelegate copy(PropertyGroup pg) {
    return null;
  }

}

