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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.util.log.Logger;
import org.cougaar.core.logging.NullLoggingServiceImpl;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.NewTimeSpan;
import org.cougaar.util.TimeSpan;

import java.util.*;

import org.cougaar.logistics.ldm.Constants;
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
	    logger = NullLoggingServiceImpl.getLoggingService();
	}
	else {
	    logger = (Logger)utilProvider.getLoggingService(this);
	}
    }


    public AssetUtils(Logger aLogger) {
	utilProvider = null;
	logger = aLogger;
	if(logger == null) {
	    logger = NullLoggingServiceImpl.getLoggingService();
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
      RelationshipSchedule rel_sched = myOrg.getRelationshipSchedule();
      Collection c = rel_sched.getMatchingRelationships(role, time);
      Vector support_orgs = new Vector();
      Iterator i = c.iterator();
      Relationship r;
      while (i.hasNext()) {
        r = (Relationship)i.next();
        support_orgs.add(rel_sched.getOther(r));
      }
      return support_orgs.elements();
    }

    public Enumeration getSupportingOrgs(Organization myOrg, Role role, long start, long end) {
      TimeSpan timespan = new MutableTimeSpan();
      ((NewTimeSpan)timespan).setTimeSpan(start, end);
      RelationshipSchedule rel_sched = myOrg.getRelationshipSchedule();
      Collection c = rel_sched.getMatchingRelationships(role, timespan);
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



  public boolean isLevel2Asset(Asset asset) {
    Asset actualAsset=asset;
    if (asset instanceof AggregateAsset) {
      actualAsset = ((AggregateAsset) asset).getAsset();
    }
    return (getAssetIdentifier(actualAsset).startsWith("Level2"));
  }

  public long getQuantity(Asset asset) {
    long qty = 0;
    if (asset instanceof AggregateAsset) {
      AggregateAsset aa = (AggregateAsset)asset;
      qty = aa.getQuantity();
    } else {
      qty = 1;
    }
    return qty;
  }

}

