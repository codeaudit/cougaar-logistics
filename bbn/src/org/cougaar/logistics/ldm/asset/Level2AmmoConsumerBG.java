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
import org.cougaar.logistics.ldm.MEIPrototypeProvider;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.glm.ldm.oplan.OrgActivity;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.math.BigDecimal;

public class Level2AmmoConsumerBG extends AmmoConsumerBG {

  public static HashMap cachedDBValues = new HashMap();
  String supplyType = "Ammunition";
  public final static String LEVEL2AMMUNITION = "Level2Ammunition";
  private transient LoggingService logger;
  private String orgName = null;

  public Level2AmmoConsumerBG(AmmoConsumerPG pg) {
    super(pg);
  }

  public void initialize(MEIPrototypeProvider plugin) {
    parentPlugin = plugin;
    logger = parentPlugin.getLoggingService(this);
  }

  public Rate getRate(Asset asset, List params) {
    if (orgName == null) {
      orgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
    }
    Rate r = null;
    // DEBUG
//     String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
    if (consumptionRates == null) {
      return r;
    }
    if (params == null) {
      logger.error("getRate() params null for " +
                   asset.getTypeIdentificationPG().getNomenclature());
//       if (myOrgName.indexOf("35-ARBN") >= 0) {
// 	System.out.println("getRate() params null for "+
// 			   asset.getTypeIdentificationPG().getNomenclature());
//       }
      return r;
    }
    Double qty = (Double) params.get(0);
    OrgActivity orgAct = (OrgActivity) params.get(1);
    if (orgAct == null) {
      logger.debug("getRate() orgAct null for " +
                   asset.getTypeIdentificationPG().getNomenclature());

      return r;
    }
    HashMap map = (HashMap) consumptionRates.get(asset);
    if (map == null) {
      logger.error("getRate()  no Ammo consumption for " +
                   asset.getTypeIdentificationPG().getNomenclature());

      return r;
    }

    Double d = (Double) map.get(orgAct.getOpTempo().toUpperCase());
    if (d == null) {
      logger.error("getRate() consumption rate null for " +
                   asset.getTypeIdentificationPG().getNomenclature());

      return r;
    }
    r = CountRate.newEachesPerDay(d.doubleValue() * qty.doubleValue());

    return r;
  }

  public Collection getConsumed() {
    if (orgName == null) {
      orgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
    }
    if (consumptionRates == null) {
      synchronized (cachedDBValues) {
        Asset asset = myPG.getMei();
        if (asset instanceof AggregateAsset) {
          asset = ((AggregateAsset) asset).getAsset();
        }
        String typeId = asset.getTypeIdentificationPG().getTypeIdentification();
        consumptionRates = (HashMap) cachedDBValues.get(typeId);
        if (consumptionRates == null) {
          Vector result = null;
          result = parentPlugin.lookupLevel2AssetConsumptionRate(orgName, asset, supplyType);
          if (result == null) {
            logger.debug("getConsumed(): Database query returned EMPTY result set for " +
                         myPG.getMei() + ", " + supplyType + " " + LEVEL2AMMUNITION);
          }
          else {
            consumptionRates = parseResults(result);
            cachedDBValues.put(typeId, consumptionRates);
          }
        }
      }
    }
    if (consumptionRates == null) {
      logger.debug("No consumption rates for " + myPG.getMei() + " at " +
                   parentPlugin.getMyOrg().getTypeIdentificationPG().getTypeIdentification());
      consumptionRates = new HashMap();
    }
    return consumptionRates.keySet();
  }

  protected HashMap parseResults(Vector result) {
    String optempo = null;
    double dcr = 0.0;
    Asset newAsset;
    HashMap map = null, ratesMap = new HashMap();
    if (result == null) {
      return null;
    }
    Enumeration results = result.elements();
    Object row[];
    while (results.hasMoreElements()) {
      row = (Object[]) results.nextElement();
      logger.debug("Ammo: parsing results for Level2MEI ");
      newAsset = parentPlugin.getPrototype(LEVEL2AMMUNITION);
      if (newAsset != null) {
        optempo = (String) row[0];
        dcr = ((BigDecimal) row[1]).doubleValue();
        map = (HashMap) ratesMap.get(newAsset);
        if (map == null) {
          map = new HashMap();
          ratesMap.put(newAsset, map);
        }
        map.put(optempo.toUpperCase(), new Double(dcr));
        logger.debug("parseResult() for " + newAsset + ", Level2MEI " +
                     ", DCR " + dcr + ", Optempo " + optempo);
      }
      else {
        logger.error("parseResults() Unable to get prototype for " + LEVEL2AMMUNITION);
      }
    }
    return ratesMap;
  }

  public PGDelegate copy(PropertyGroup pg) {
    return null;
  }
}


