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

package org.cougaar.logistics.ldm;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.glm.ldm.asset.AssetConsumptionRatePG;
import org.cougaar.glm.ldm.asset.AssignedPG;
import org.cougaar.glm.ldm.asset.NewAssetConsumptionRatePG;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.AssetConsumptionRate;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.glm.ldm.QueryLDMPlugin;
import org.cougaar.glm.ldm.asset.ClassISubsistence;
import org.cougaar.glm.ldm.asset.MilitaryPerson;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.logistics.ldm.asset.SubsistenceConsumerBG;
import org.cougaar.logistics.ldm.asset.SubsistenceConsumerPG;
import org.cougaar.logistics.ldm.asset.NewSubsistenceConsumerPG;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;

// Prototype Provider for Class I Subsistence Rations
public class ClassIConsumerPrototypeProvider extends QueryLDMPlugin implements UtilsProvider{
  String service = null;
  public static final String THEATER = "SWA";
  boolean configured;
  private IncrementalSubscription consumerSubscription;
  private IncrementalSubscription myOrganizations;
  private ServiceBroker serviceBroker;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils assetUtils;
  private ScheduleUtils scheduleUtils;
  private Organization myOrg;
  private BlackboardQueryService queryService;

  private static UnaryPredicate orgsPredicate= new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
	return ((Organization)o).isSelf();
      }
      return false;
    }
  };

  // used for rehydration
  private static UnaryPredicate personPredicate() {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
	if (o instanceof MilitaryPerson ) {
	  return true;
	}
	if (o instanceof AggregateAsset) {
	  if (((AggregateAsset)o).getAsset() instanceof MilitaryPerson) {
	    return true;
	  }
	}
	return false;
      }
    };
  }

  public TaskUtils      getTaskUtils() {return taskUtils;}
  public TimeUtils      getTimeUtils() {return timeUtils;}
  public AssetUtils     getAssetUtils() {return assetUtils;}  
  public ScheduleUtils  getScheduleUtils() {return scheduleUtils;}

  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService)serviceBroker.getService(requestor,
						    LoggingService.class,
						    null);
  }

  public BlackboardQueryService getBlackboardQueryService(Object requestor) {
    return (BlackboardQueryService)serviceBroker.getService(requestor,
                                                            BlackboardQueryService.class,
                                                            null);
  }

  public void load() {
    super.load();
    serviceBroker = getBindingSite().getServiceBroker();
    if (serviceBroker != null) {
      logger = getLoggingService(this);
      timeUtils = new TimeUtils(this);
      assetUtils = new AssetUtils(this);
      taskUtils = new TaskUtils(this);
      scheduleUtils = new ScheduleUtils(this);
      queryService = getBlackboardQueryService(this);
      if (queryService == null) {
        logger.error("Unable to get query service");
      }
    }
  }

  protected void setupSubscriptions() {
    super.setupSubscriptions();
    myOrganizations = (IncrementalSubscription) subscribe( orgsPredicate);
    consumerSubscription = (IncrementalSubscription)subscribe(personPredicate());
    if (didRehydrate()) {
      rehydrate();
    } // if
  } // setupSubscriptions

  public Organization getMyOrg() {
    return myOrg;
  }

  protected void rehydrate() {
    configure();
    logger.debug ("Rehydrated - configured "+configured);
    if (configured) {
      // rehook handlers
      Enumeration consumers = consumerSubscription.elements();
      Asset asset, proto;
      //boolean success;
      Vector good_prototypes = new Vector();
      
      while (consumers.hasMoreElements()) {
	asset = (Asset)consumers.nextElement();
	if (asset instanceof AggregateAsset) {
	  proto = ((AggregateAsset)asset).getAsset();
	} else {
	  proto = asset.getPrototype();
	}
	if (proto == null) {
	  logger.error ("no prototype for "+asset);
	}
	if ((proto != null) && (!good_prototypes.contains(proto))) {
	  TypeIdentificationPG tip = asset.getTypeIdentificationPG();
	  if (tip!= null) {
	    String type_id = tip.getTypeIdentification();
	    if (type_id != null) {
	      getLDM().cachePrototype(type_id, proto);
	      good_prototypes.add(proto);
	      logger.debug ("Rehydrated asset "+asset+" w/ proto "+proto);
            } else {
	      logger.error ("cannot rehydrate "+proto+" no typeId");
	    }
          } else {
	    logger.error ("cannot rehydrate "+proto+" no typeIdPG");
	  }
	}
      } // end while loop
      addConsumerPGs(consumerSubscription);
    }
  }

  public void execute() {
    if (!configured) {
      configure();
    }

    if (!consumerSubscription.isEmpty()) {
//       System.out.println("Calling addConsumerPGs() for "+getAgentIdentifier());
      addConsumerPGs(consumerSubscription);
    }
  }
  
  protected void configure() {
    Iterator new_orgs = queryService.query(orgsPredicate).iterator();
    if (new_orgs.hasNext()) { 
      myOrg = (Organization) new_orgs.next();
      Service srvc = myOrg.getOrganizationPG().getService();
      if (srvc != null) {
	service = srvc.toString();
	configured = true;
      }
    }
  }
  // Don't want to do this.  I am not creating prototypes
  public boolean canHandle(String typeid, Class class_hint) {
    return false;
  }

  // I don't think this will ever be called since my can handle says no
  // let's return null just in case -- llg
  public Asset makePrototype(String type_name, Class class_hint) {
    return null;
  }

  public Asset getPrototype(String typeid) {
    return getLDM().getFactory().getPrototype(typeid);
  }

  public Vector generateRationList() {
    Vector list = new Vector();
    String query = (String)fileParameters_.get("Class1ConsumedList");
    if (query == null) { // if query not found, return null
      logger.debug ("generaterationList(),  query is null");
      return null;
    }
    Vector holdsQueryResult;
    try {
      holdsQueryResult = executeQuery(query);
      if (holdsQueryResult.isEmpty()){
	return null;
      }
    } catch (Exception ee) {
      String str =" DB query failed. query= "+ query+ "\n ERROR "+ee.toString();
      logger.debug (" getSupplementalList(),"+str);
      return null;
    }
    String  typeIDPrefix = "NSN/";
    PlanningFactory ldm = getLDM().getFactory();
    for(int i = 0; i < holdsQueryResult.size(); i++){
      Object[]  row = ( (Object[])holdsQueryResult.elementAt(i));
      Asset ration = ldm.getPrototype(typeIDPrefix + (String)row[0]);
      if (ration != null) {
	list.addElement(ration);
      } else {
	logger.error (" Asset prototype is null");
      } // if
    } // for
    return list;
  }

  protected void addConsumerPGs(Collection consumers) {
    Iterator people = consumers.iterator();
    Asset a, anAsset;
    while (people.hasNext()) {
      a = (Asset)people.next();
      if (a instanceof AggregateAsset) {
        anAsset = ((AggregateAsset)a).getAsset();
      } else {
        anAsset = a;
      }
      if (anAsset instanceof MilitaryPerson) {
        if (anAsset.searchForPropertyGroup(SubsistenceConsumerPG.class) == null) {
          NewSubsistenceConsumerPG foodpg = 
            (NewSubsistenceConsumerPG)getLDM().getFactory().createPropertyGroup(SubsistenceConsumerPG.class);
          foodpg.setMei(a);
          foodpg.setService(service);
          foodpg.setTheater(THEATER);
          foodpg.setSubsistenceConsumerBG(new SubsistenceConsumerBG(foodpg));
          foodpg.initialize(this);
          anAsset.setPropertyGroup(foodpg);
          publishChange(a);
        }
      }
    }
  }

  // Associating a property group to the person asset
  public void fillProperties(Asset anAsset) {
  }
  
}



