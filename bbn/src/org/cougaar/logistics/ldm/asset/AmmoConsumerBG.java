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
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.logistics.plugin.utils.*;
import org.cougaar.logistics.ldm.MEIPrototypeProvider;

import java.math.BigDecimal;

import java.util.*;

public class AmmoConsumerBG extends ConsumerBG {

  protected AmmoConsumerPG myPG;
  MEIPrototypeProvider parentPlugin;
  String supplyType = "Ammunition";
  HashMap consumptionRates = new HashMap();
  private transient LoggingService logger;

  public AmmoConsumerBG(AmmoConsumerPG pg) {
    myPG = pg;
  }

  public void initialize(MEIPrototypeProvider plugin) {
    parentPlugin = plugin;
    logger = parentPlugin.getLoggingService(this);
   }

  public List getPredicates() {
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
    if (myOrgName.indexOf("35-ARBN") >= 0) {
      System.out.println("getParamSched() Asset is "+
			 myPG.getMei().getTypeIdentificationPG().getTypeIdentification());
    }
    while (predList.hasNext()) {
      Iterator list = ((Collection)predList.next()).iterator();
      predicate = (UnaryPredicate)list.next();
      if (predicate instanceof ConsumerPredicate) {
	Schedule consumerSched = 
	  parentPlugin.getScheduleUtils().createConsumerSchedule((Collection)list.next());
	if (myOrgName.indexOf("35-ARBN") >= 0) {
	  System.out.println("getParamSched() ConsumerSched "+consumerSched);
	}
 	params.add(parentPlugin.getScheduleUtils().convertQuantitySchedule(consumerSched));
      } else if (predicate instanceof OrgActivityPred) {
	Schedule orgActSched = 
	  parentPlugin.getScheduleUtils().createOrgActivitySchedule((Collection)list.next());
 	params.add(orgActSched);
	if (myOrgName.indexOf("35-ARBN") >= 0) {
	  System.out.println("getParamSched() OrgActSched "+orgActSched);
	}
      } else {
 	logger.error("getParameterSchedule: unknown predicate");
      }
    }
    paramSchedule = parentPlugin.getScheduleUtils().getMergedSchedule(params);
    if (myOrgName.indexOf("35-ARBN") >= 0) {
      System.out.println("getParamSched() MERGED "+paramSchedule);
    }
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
      logger.error("getRate() orgAct null");
      if (myOrgName.indexOf("35-ARBN") >= 0) {
	System.out.println("getRate() orgAct null for "+
			   asset.getTypeIdentificationPG().getNomenclature());
      }
      return r;
    }
    HashMap map = (HashMap) consumptionRates.get(asset);
    if (map == null) {
      if (myOrgName.indexOf("35-ARBN") >= 0) {
	System.out.println("getRate()  no bulkpol consumption for "+
			   asset.getTypeIdentificationPG().getNomenclature());
      }
      return r;
    }
    if (myOrgName.indexOf("35-ARBN") >= 0) {
      System.out.println("getRate() orgAct name "+
			 orgAct.getOpTempo()+", "+orgAct);
    }
    Double d = (Double) map.get(orgAct.getOpTempo().toUpperCase());
    if (d == null) {
      logger.error("getRate() consumption rate null");
      if (myOrgName.indexOf("35-ARBN") >= 0) {
	System.out.println("getRate() consumption rate null for "+
			   asset.getTypeIdentificationPG().getNomenclature());
	System.out.println(" OrgACt "+orgAct);
      }
      return r;
    }
    r = CountRate.newEachesPerDay (d.doubleValue()*qty.doubleValue());
    if (myOrgName.indexOf("35-ARBN") >= 0) {
      System.out.println("getRate() Rate is  "+ r +", for "+
			 asset.getTypeIdentificationPG().getNomenclature());
    }
    return r;
  }

  public Collection getConsumed() {
    if (consumptionRates.isEmpty()) {
      Vector result = parentPlugin.lookupAssetConsumptionRate(myPG.getMei(), supplyType, 
							      myPG.getService(), myPG.getTheater());
      if (result == null) {
	logger.debug("getConsumed(): Database query returned EMPTY result set for "+
		     myPG.getMei()+", "+supplyType);
      } else {
	parseResults(result);
      }
    }
    return consumptionRates.keySet();
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

    for (int i=0; results.hasMoreElements(); i++) {
      row = (Object [])results.nextElement();
      mei_nsn = (String) row[0];
      logger.debug("Ammo: parsing results for MEI nsn: " + mei_nsn);
      typeid = "DODIC/"+(String) row[1];
      newAsset = parentPlugin.getPrototype(typeid);
      if (newAsset != null) {
	optempo = (String) row[2];
	dcr = ((BigDecimal) row[3]).doubleValue();
	map = (HashMap)consumptionRates.get(newAsset);
	if (map == null) {
	  map = new HashMap();
	  consumptionRates.put(newAsset, map);
	}
	map.put(optempo.toUpperCase(), new Double(dcr));
	logger.debug("parseResult() for "+newAsset+", MEI "+mei_nsn+
		     ", DCR "+dcr+", Optempo "+optempo);
	//DEBUG
	String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
	if (myOrgName.indexOf("35-ARBN") >= 0) {
	  System.out.println("parseResult() for "+newAsset+", MEI "+mei_nsn+
			     ", DCR "+dcr+", Optempo "+optempo);
	}
      } else {
	logger.error("parseResults() Unable to get prototype for "+typeid);
      }
    }
  } // parseResults

  public PGDelegate copy(PropertyGroup pg) {
    return null;
  }

}


