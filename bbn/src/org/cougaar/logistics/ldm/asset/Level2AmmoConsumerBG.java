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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

public class Level2AmmoConsumerBG extends AmmoConsumerBG {
  String supplyType = "Ammunition";
  public final static String LEVEL2AMMUNITION = "Level2Ammunition";
  private transient LoggingService logger = LoggingService.NULL;
  private String orgName = null;

  public Level2AmmoConsumerBG(AmmoConsumerPG pg) {
    super(pg);
  }

  public void initialize(MEIPrototypeProvider plugin) {
    super.initialize(plugin);
    logger = parentPlugin.getLoggingService(this);
  }

  public Collection getConsumed() {
    if (orgName == null) {
      if (parentPlugin != null)
	orgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
      else {
	if (logger != null)
	  logger.error("getConsumed - parentPlugin is null!");
	else
	  System.err.println("Lvl2AmmoConsBG.getConsumed null parent / logger");
      }
    }
    if (consumptionRates == null) {
      synchronized (cachedDBValues) {
        Asset asset = myPG.getMei();
        if (asset instanceof AggregateAsset) {
          asset = ((AggregateAsset) asset).getAsset();
        }
        String typeId = asset.getTypeIdentificationPG().getTypeIdentification();
        consumptionRates = (HashMap) cachedDBValues.get(orgName + typeId);
        if (consumptionRates == null) {
          Vector result = null;
	  if (parentPlugin != null)
	    result = parentPlugin.lookupLevel2AssetConsumptionRate(orgName, asset, supplyType);
	  else {
	    if (logger != null)
	      logger.error("getConsumed - parentPlugin is null for " + asset);
	    else
	      System.err.println("Lvl2AmmoConsBG.getConsumed - null parent / logger for " + asset);
	  }
          if (result == null) {
	    if (logger.isDebugEnabled())
	      logger.debug("getConsumed(): Database query returned EMPTY result set for " +
                         myPG.getMei() + ", " + supplyType + " " + LEVEL2AMMUNITION);
          }
          else {
            consumptionRates = parseResults(result);
            cachedDBValues.put(orgName + typeId, consumptionRates);
          }
        }
      }
    }
    if (consumptionRates == null) {
      if (logger.isDebugEnabled())
	logger.debug("No consumption rates for "+myPG.getMei()+ ((parentPlugin != null) ? (" at " +
                   parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification()) : ""));
      consumptionRates = new HashMap();
    }
    return consumptionRates.keySet();
  }

  protected HashMap parseResults(Vector result) {
    String optempo = null;
    double dcr = 0.0;
    Asset newAsset;
    HashMap map = null, ratesMap = new HashMap();

    Enumeration results = result.elements();
    Object row[];
    while (results.hasMoreElements()) {
      row = (Object[]) results.nextElement();
      if (logger.isDebugEnabled())
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
	if (logger.isDebugEnabled())
	  logger.debug("parseResult() for " + newAsset + ", Level2MEI " +
                     ", DCR " + dcr + ", Optempo " + optempo);
      }
      else {
        logger.error("parseResults() Unable to get prototype for " + LEVEL2AMMUNITION);
      }
    }
    return ratesMap;
  }
}


