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
import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.AggregateAsset;
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

  public static HashMap cachedDBValues = new HashMap();
  protected AmmoConsumerPG myPG;
  transient MEIPrototypeProvider parentPlugin;
  String supplyType = "Ammunition";
  HashMap consumptionRates = null;
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
    predList.add(new OrgActivityPred());
    return predList;
  }

  public Schedule getParameterSchedule(Collection col, TimeSpan span) {
    Schedule paramSchedule = null;
    Vector params = new Vector();
    Iterator predList = col.iterator();
    UnaryPredicate predicate;
    // DEBUG
//     String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
//     if (myOrgName.indexOf("35-ARBN") >= 0) {
//       System.out.println("getParamSched() Asset is "+
// 			 myPG.getMei().getTypeIdentificationPG().getTypeIdentification());
//     }
    ArrayList consumerlist = new ArrayList();
    consumerlist.add(myPG.getMei());
    Schedule consumerSched = parentPlugin.getScheduleUtils().createConsumerSchedule(consumerlist);
    consumerSched = parentPlugin.getScheduleUtils().convertQuantitySchedule(consumerSched);
    params.add(parentPlugin.getScheduleUtils().trimObjectSchedule(consumerSched, span));
    while (predList.hasNext()) {
      Iterator list = ((Collection)predList.next()).iterator();
      predicate = (UnaryPredicate)list.next();
      if (predicate instanceof OrgActivityPred) {
        Collection orgColl = (Collection)list.next();
        if ((orgColl == null) || (orgColl.isEmpty())) {
          return null;
        }
	Schedule orgActSched = 
	  parentPlugin.getScheduleUtils().createOrgActivitySchedule(orgColl);
   	params.add(parentPlugin.getScheduleUtils().trimObjectSchedule(orgActSched, span));
      } else {
        if (logger.isErrorEnabled()) {
          logger.error("getParameterSchedule: unknown predicate");
        }
      }
    }
    paramSchedule = parentPlugin.getScheduleUtils().getMergedSchedule(params);
    return paramSchedule;
  }

  public Rate getRate(Asset asset, List params) {
    Rate r = null;
    // DEBUG
//     String myOrgName = parentPlugin.getMyOrg().getItemIdentificationPG().getItemIdentification();
    if (consumptionRates == null) {
      return r;
    }
    if (params == null) {
      if (logger.isErrorEnabled()) {
        logger.error("getRate() params null for " +
                     asset.getTypeIdentificationPG().getNomenclature());
      }
//       if (myOrgName.indexOf("35-ARBN") >= 0) {
// 	System.out.println("getRate() params null for "+
// 			   asset.getTypeIdentificationPG().getNomenclature());
//       }
      return r;
    }
    Double qty = (Double)params.get(0);
    OrgActivity orgAct = (OrgActivity)params.get(1);
    if (orgAct == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("getRate() orgAct null for " +
                     asset.getTypeIdentificationPG().getNomenclature());
      }

      return r;
    }
    HashMap map = (HashMap) consumptionRates.get(asset);
    if (map == null) {
      if (logger.isErrorEnabled()) {
        logger.error("getRate()  no Ammo consumption for " +
                     asset.getTypeIdentificationPG().getNomenclature());
      }

      return r;
    }

    Double d = (Double) map.get(orgAct.getOpTempo().toUpperCase());
    if (d == null) {
      if (logger.isErrorEnabled()) {
        logger.error("getRate() consumption rate null for " +
                     asset.getTypeIdentificationPG().getNomenclature());
      }

      return r;
    }
    r = CountRate.newEachesPerDay (d.doubleValue()*qty.doubleValue());

    return r;
  }

  public Collection getConsumed() {
    if (consumptionRates == null) {
      synchronized (cachedDBValues) {
        Asset asset = myPG.getMei();
        if (asset instanceof AggregateAsset) {
          asset = ((AggregateAsset)asset).getAsset();
        }
        String typeId = asset.getTypeIdentificationPG().getTypeIdentification();

	String echelon = parentPlugin.getMyOrg().getMilitaryOrgPG().getEchelon();
	String key = typeId + "-" + echelon;

        consumptionRates = (HashMap)cachedDBValues.get(key);
        if (consumptionRates == null){
          Vector result = parentPlugin.lookupAssetConsumptionRate(asset, supplyType, 
                                                                  myPG.getService(), myPG.getTheater());
          if (result == null) {
            if (logger.isDebugEnabled()) {
              logger.debug("getConsumed(): Database query returned EMPTY result set for " +
                           myPG.getMei() + ", " + supplyType);
            }
          } else {
            consumptionRates = parseResults(result);
            cachedDBValues.put(key, consumptionRates);
          }
        }
      }
    }
    if (consumptionRates == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("No consumption rates for " + myPG.getMei() + " at " +
                     parentPlugin.getMyOrg().getItemIdentificationPG()
                     .getItemIdentification());
      }
      consumptionRates = new HashMap();
    }
    return consumptionRates.keySet();
  }

  public Collection getConsumed(int x) {
    return getConsumed();
  }

  public Collection getConsumed(int x, int y) {
    return getConsumed();
  }

  protected HashMap parseResults (Vector result) {
    String mei_nsn, typeid, optempo;
    double dcr;
    Asset newAsset;
    HashMap map = null, ratesMap = new HashMap();
    Enumeration results = result.elements();
    Object row[];

    for (int i=0; results.hasMoreElements(); i++) {
      row = (Object[]) results.nextElement();
      mei_nsn = (String) row[0];
      if (logger.isDebugEnabled()) {
        logger.debug("Ammo: parsing results for MEI nsn: " + mei_nsn);
      }
      typeid = "DODIC/" + (String) row[1];
      newAsset = parentPlugin.getPrototype(typeid);
      if (newAsset != null) {
        optempo = (String) row[2];
        dcr = ((BigDecimal) row[3]).doubleValue();
        map = (HashMap) ratesMap.get(newAsset);
        if (map == null) {
          map = new HashMap();
          ratesMap.put(newAsset, map);
        }
        map.put(optempo.toUpperCase(), new Double(dcr));
        if (logger.isDebugEnabled()) {
          logger.debug("parseResult() for " + newAsset + ", MEI " + mei_nsn +
                       ", DCR " + dcr + ", Optempo " + optempo);
        }
      } else {
        if (logger.isErrorEnabled()) {
          logger.error("parseResults() Unable to get prototype for " +
                       typeid);
        }
      }
    }
    return ratesMap;
  } // parseResults

  public PGDelegate copy(PropertyGroup pg) {
    return null;
  }

}


