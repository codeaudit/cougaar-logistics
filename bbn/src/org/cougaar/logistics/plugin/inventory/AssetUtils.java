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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.util.log.Logger;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;

import java.util.*;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.glm.ldm.plan.GeolocLocation;
import org.cougaar.glm.debug.GLMDebug;

import org.cougaar.logistics.plugin.inventory.InventoryPlugin;

/** Provides convenience methods. */
public class AssetUtils {


    private transient Logger logger;
    private transient UtilsProvider utilProvider;

    public AssetUtils(UtilsProvider provider) {
	utilProvider = provider;
	if(utilProvider == null) {
	    logger = NullLoggingServiceImpl.getNullLoggingServiceImpl();
	}
	else {
	    logger = (Logger)utilProvider.getLoggingService(this);
	}
    }


    public AssetUtils(Logger aLogger) {
	utilProvider = null;
	logger = aLogger;
	if(logger == null) {
	    logger = NullLoggingServiceImpl.getNullLoggingServiceImpl();
	}
    }
 
    public String assetDesc(Asset asset){
	String nsn = getAssetIdentifier(asset);
	return nsn+"("+getPartNomenclature(asset)+")";
    }

    public static String getPartNomenclature(Asset part){
	String nomen="Unknown part name";
	TypeIdentificationPG tip = part.getTypeIdentificationPG();
	if (tip!= null) {
	    nomen = tip.getNomenclature();
	}
	return nomen;
    }

    public String getAssetIdentifier(Asset asset) {
	if (asset == null) {
	    return null;
	} else {
	    TypeIdentificationPG tip = asset.getTypeIdentificationPG();
	    if (tip!= null) {
		return tip.getTypeIdentification();
	    }else {
		logger.error("asset: "+asset+" has null getTypeIdentificationPG()");
		return null;
	    }
	}
    }

    public Enumeration getSupportingOrgs(Organization myOrg, Role role, long time) {
        return getSupportingOrgs(myOrg, role, time, time);
    }

    public Enumeration getSupportingOrgs(Organization myOrg, Role role, long start, long end) {
	RelationshipSchedule rel_sched = myOrg.getRelationshipSchedule();
	Collection c = rel_sched.getMatchingRelationships(role, start, end);
	Vector support_orgs = new Vector();
	Iterator i = c.iterator();
	Relationship r;
	while (i.hasNext()) {
	    r = (Relationship)i.next();
	    support_orgs.add(rel_sched.getOther(r));
	}
	return support_orgs.elements();
    }

   public Enumeration getGeolocLocationAtTime(Organization org, long time) {
     LocationSchedulePG lspg = org.getLocationSchedulePG();
     Vector geolocs = new Vector();
     try {
       Schedule ls = lspg.getSchedule();
       Iterator i  = ls.getScheduleElementsWithTime(time).iterator();
       while (i.hasNext()) {
	 LocationScheduleElement lse = (LocationScheduleElement)i.next(); 
	 geolocs.add((GeolocLocation)lse.getLocation());
       }
     } catch (NullPointerException npe) {
     // Not all organizations have LocationSchedulePG's
       logger.info("AssetUtils AssetGeolocLocationAtTime(), LocationSchedulePG NOT found on "+org);
     }
     return geolocs.elements();
   }


}

