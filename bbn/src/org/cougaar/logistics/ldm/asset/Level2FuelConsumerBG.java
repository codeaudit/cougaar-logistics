 /*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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

public class Level2FuelConsumerBG extends FuelConsumerBG {
  public final static String LEVEL2BULKPOL = "Level2BulkPOL";
  private transient LoggingService logger = LoggingService.NULL;
  private String orgName = null;

  public Level2FuelConsumerBG(FuelConsumerPG pg) {
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
	  System.err.println("Lvl2FuelConsBG.getConsumed null parent / logger");
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
	      logger.error("getConsumed - null parentPlugin for " + asset);
	    else
	      System.err.println("Lvl2FuelConsBG.getConsumed null parent / logger for " + asset);
	  }
          if (result == null) {
	    if (logger.isDebugEnabled())
	      logger.debug("getConsumed(): Database query returned EMPTY result set for " +
                         myPG.getMei() + ", " + supplyType + " " + LEVEL2BULKPOL);
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
    Asset newAsset = null;
    HashMap map = null, ratesMap = new HashMap();

    Enumeration results = result.elements();
    Object row[];
    while (results.hasMoreElements()) {
      row = (Object[]) results.nextElement();
      if (logger.isDebugEnabled())
	logger.debug("BulkPOL: parsing results for Level2MEI ");
      newAsset = parentPlugin.getPrototype(LEVEL2BULKPOL);
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
        logger.error("parseResults() Unable to get prototype for " + LEVEL2BULKPOL);
      }
    }
    if (ratesMap.isEmpty()) {
      System.out.println(" rates map empty for " + newAsset.getTypeIdentificationPG().getTypeIdentification());
    }
    return ratesMap;
  }
}
