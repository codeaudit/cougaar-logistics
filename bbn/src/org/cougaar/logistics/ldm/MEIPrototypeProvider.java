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
import org.cougaar.core.adaptivity.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.ClassVIIMajorEndItem;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.glm.ldm.QueryLDMPlugin;

import java.math.BigDecimal;
import java.sql.*;
import java.text.StringCharacterIterator;
import java.util.*;

import org.cougaar.logistics.ldm.asset.*;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;

public class MEIPrototypeProvider extends QueryLDMPlugin implements UtilsProvider {


  private boolean configured;

  public static final String MEI_STRING = "ClassVIIMajorEndItem";
  public static final String THEATER = "SWA";
  private String service = null;
  // Subscription to policies
  private IncrementalSubscription meiSubscription;
	
  private IncrementalSubscription myOrganizations;
  protected Organization myOrg;

  private ServiceBroker serviceBroker;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils assetUtils;
  private ScheduleUtils scheduleUtils;

  private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
    public boolean execute (Object o) {
      if (o instanceof Organization) {
	return ((Organization)o).isSelf();
      } 
      return false;
    }
  };
	
  private static class ClassVIIPredicate implements UnaryPredicate {
    // Predicate defining MEIs, in theory all of the MEIs we created
    // prototypes for
    public boolean execute (Object o) {
      if (o instanceof ClassVIIMajorEndItem) {
	return true;
      }
		
      if (o instanceof AggregateAsset) {
	if (((AggregateAsset)o).getAsset() instanceof ClassVIIMajorEndItem) {
	  return true;
	}
      }
      return false;
    } 
  };

  public TaskUtils      getTaskUtils() {return taskUtils;}
  public TimeUtils      getTimeUtils() {return timeUtils;}
  public AssetUtils     getAssetUtils() {return assetUtils;}  
  public ScheduleUtils  getScheduleUtils() {return scheduleUtils;}

  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService)serviceBroker.getService(requestor,
						    LoggingService.class,
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
    }
  }

  protected void setupSubscriptions() { 
    super.setupSubscriptions();
    myOrganizations =
      (IncrementalSubscription) subscribe (orgsPredicate);
    meiSubscription =
      (IncrementalSubscription) subscribe (new ClassVIIPredicate());
    if (didRehydrate()) {
      rehydrate();
    }
  } // setupSubscriptions


  protected void rehydrate() {
    configure();
    if (configured) {
      // rehook handlers
      Enumeration meis = meiSubscription.elements();
      Asset asset, proto;
      Vector good_prototypes = new Vector();
      while (meis.hasMoreElements()) {
	asset = (Asset)meis.nextElement();
	if (asset instanceof AggregateAsset) {
	  proto = ((AggregateAsset)asset).getAsset();
	} else {
	  proto = asset.getPrototype();
	} // if
	if (proto == null) {
	  logger.error("no prototype for "+asset);
	} // if
	if ((proto != null) && (!good_prototypes.contains(proto))) {
	  TypeIdentificationPG tip = asset.getTypeIdentificationPG();
	  if (tip!= null) {
	    String type_id = tip.getTypeIdentification();
	    if (type_id != null) {
	      fillProperties(proto);
	      getLDM().cachePrototype(type_id, proto);
	      good_prototypes.add(proto);
	    } else {
	      logger.error("cannot rehydrate "+proto+" no typeId");
	    }
	  } else {
	    logger.error("cannot rehydrate "+proto+" no typeIdPG");
	  }
	}
      }
    }
  } 

  public void execute() {
    if (!configured) {
      configure();
    } 
  }

  protected void configure() {
    Enumeration new_orgs = myOrganizations.elements();
    if (new_orgs.hasMoreElements()) { 
      myOrg = (Organization) new_orgs.nextElement();
      Service srvc = myOrg.getOrganizationPG().getService();
      if (srvc != null) {
	service= srvc.toString();
      } // if
      configured = true;
    } // if
  } // configure

  public void setServiceBroker(ServiceBroker serviceBroker) {
    this.serviceBroker = serviceBroker;
  }

  public Organization getMyOrg() {
    return myOrg;
  }

  public boolean canHandle (String typeid, Class class_hint) {
    Boolean protoProvider = (Boolean) myParams_.get("PrototypeProvider");
    logger.debug("canHandle (typeid:"+typeid+")");
    if ((protoProvider == null) || (protoProvider.booleanValue())) {
      if ((class_hint == null) ||  class_hint.getName().equals(MEI_STRING)){
	String [] handlesList = {"NSN/", "MDS/", "TAMCN/", "MEI/", "DODIC/"};
	for (int i=0; i<handlesList.length; i++) {
	  if ( typeid.startsWith (handlesList[i]) ) {
	    return true;
	  } 
	} 
      }
    }
    logger.debug("canHandle(), Unable to provider Prototype."+
		 " ProtoProvider = "+protoProvider+", typeid= "+typeid);
    return false;
  } 


  public Asset makePrototype (String type_name, Class class_hint) {
    // Demand Rate Queries are based upon service
    if (service == null) {
      return null;
    } 

    if ((class_hint != null) &&  !class_hint.getName().equals(MEI_STRING)) {
      logger.error("make prototype How did we get this far?? "+class_hint);
      return null;
    }

    if (!configured) {
      logger.error("makePrototype("+type_name+","+class_hint+") PlugIn not configured yet");
      return null;
    }

    // create initial asset
    String nomen = getMEINomenclature(type_name, service);
    if (nomen == null) {
      return null;
    } // if
    logger.debug ("is dodic:" + (type_name.indexOf ("DODIC") > -1));
    return newAsset(type_name,MEI_STRING, nomen);
  } // makePrototype


  // getNormalizedName():
  // Normalizes the wide range of Airforce MDSs inorder to find the MDS in the
  // database.
  private String getNormalizedName (String name) {
	
    StringBuffer mission = new StringBuffer();
    StringBuffer design = new StringBuffer();
    StringBuffer series = new StringBuffer();
    logger.debug("getNormalizedName, Original MDS: "+name);
    StringCharacterIterator sci = new StringCharacterIterator(name);
    int state = 0;
    //0 = mission
    //1 = design
    //2 = series
	
    //Character Ch;
    char ch;
    while ((ch = sci.current()) != StringCharacterIterator.DONE) {
		
      if (!Character.isLetterOrDigit(ch)) {
	sci.next();
	continue;
      } // if
      ch = Character.toUpperCase(ch);
		
      if (state == 0) {
				//looking for mission
	if (Character.isLetter(ch)) {
	  mission.append(ch);
	  sci.next();
	} else {
	  state = 1;
	} // if
      } else if (state == 1) { //looking for design
	if (Character.isDigit(ch)) {
	  design.append(ch);
	  sci.next();
	} else {
	  state = 2;
	} // if

      } else { //looking for series
	series.append(ch);
	sci.next();
      }
    } // while
	
    //Converting design to Integer
	
    while (design.length() < 3) {
      design.insert(0, (int)0);
    } // while
    while (series.length() > 1) {
      series.deleteCharAt(series.length()-1);
    } // while
	
    return (mission.toString()+design.toString()+series.toString());
  } // getNormalizedName


  protected String getMEINomenclature (String type_id, String service) {
    String query = (String)fileParameters_.get("meiQuery");
    String consumer_id = type_id.substring(type_id.indexOf("/")+1);
    if (type_id.startsWith("MDS/") && service.equals(Service.AIRFORCE)) {
      consumer_id = getNormalizedName(consumer_id);
    } // if
    Vector result = null;
    String nomen = null; 
    if (query != null) {
      int i = query.indexOf(":nsn");
      String q1 = query.substring(0,i) + "'"+consumer_id+"'";
      String q2 = query.substring(i+4, query.indexOf(":service")) + "'"+service+"'";
      query = q1 + q2;
      try {
	result = executeQuery (query);
	if (result.isEmpty()) {
	  // this is fine - means the type_id is not an MEI
	  return null;
	} else {
	  Object row[] = (Object[])result.firstElement();
	  nomen = (String) row[0];
	}
      } catch (Exception ee) {
	logger.error("retrieveFromDB(), DB query failed. query= "+query+
		    "\n ERROR "+ee);
	return null;
      } 
    } 
    return nomen;
  } // getMEINomenclature


  public void fillProperties (Asset anAsset) {
    if (anAsset instanceof ClassVIIMajorEndItem) {
      logger.debug("fillProperties() CREATING FuelConsumerPG for "+anAsset);
      NewFuelConsumerPG fuelpg = 
	(NewFuelConsumerPG)getLDM().getFactory().createPropertyGroup(FuelConsumerPG.class);
      fuelpg.setMei(anAsset);
      fuelpg.setService(service);
      fuelpg.setTheater(THEATER);
      fuelpg.setFuelBG(new FuelConsumerBG(fuelpg));
      fuelpg.initialize(this);
      anAsset.setPropertyGroup(fuelpg);
      NewAmmoConsumerPG ammopg = 
	(NewAmmoConsumerPG)getLDM().getFactory().createPropertyGroup(AmmoConsumerPG.class);
      ammopg.setMei(anAsset);
      ammopg.setService(service);
      ammopg.setTheater(THEATER);
      ammopg.setAmmoBG(new AmmoConsumerBG(ammopg));
      ammopg.initialize(this);
      anAsset.setPropertyGroup(ammopg);
      NewPackagedPOLConsumerPG packagedpg = 
	(NewPackagedPOLConsumerPG)getLDM().getFactory().createPropertyGroup(PackagedPOLConsumerPG.class);
      packagedpg.setMei(anAsset);
      packagedpg.setService(service);
      packagedpg.setTheater(THEATER);
      packagedpg.setPackagedPOLBG(new PackagedPOLConsumerBG(packagedpg));
      packagedpg.initialize(this);
      anAsset.setPropertyGroup(packagedpg);
    } // if
  } // fillProperties

  public Asset getPrototype(String typeid) {
    return getLDM().getFactory().getPrototype(typeid);
  }

  // Queries the DB to retrieve the parts for an MEI and the consumption rates for each
  // part in order to create and AssetConsumptionRate object.
  // public AssetConsumptionRate lookupAssetConsumptionRate
  public Vector lookupAssetConsumptionRate(Asset asset, String asset_type, 
					   String service, String theater) {
    logger.debug ("lookupAssetConsumptionRate()");

    String query = createACRQuery (asset, asset_type, service, theater);
    if (query == null) {
      logger.error("lookupAssetConsumptionRate() Invalid ACR query for "+
		  asset_type+service);
      return null;
    } // if
    logger.debug("lookupAssetConsumptionRate() ACR query for "+
		asset_type+service+" = "+query);
    Vector result;
    try {
      result = executeQuery (query);
      logger.debug ("in lookupAssetConsumptionRate() query complete for asset "+
		   asset.getTypeIdentificationPG().getNomenclature()+
		   "\nquery= "+query);
      if (result.isEmpty()) {
	logger.debug ("no result for asset " +
		     asset.getTypeIdentificationPG().getNomenclature());
	return null;
      } // if
    } catch (Exception ee) {
      logger.error ( "in lookupAssetConsumptionRate(), DB query failed.Query= "+query);
      return null;
    } // try
//     return parseACRResults (result, asset_type);
    return result;
  } // lookupAssetConsumptionRate


  public String generateMEIQueryParameter
      (Asset asset, String asset_type, Service service) {
    String typeID = asset.getTypeIdentificationPG().getTypeIdentification();
    int indx = typeID.indexOf('/');
    String division = typeID.substring(0, indx);
    return asset_type+service.getName()+division;
  } // generateMEIQueryParameter
	

  public String createACRQuery (Asset asset, String asset_type, 
				String service,  String theater) {
    String typeID = asset.getTypeIdentificationPG().getTypeIdentification();
    int indx = typeID.indexOf ('/');
    String division = typeID.substring (0, indx);
    String query = (String) fileParameters_.get (asset_type+service+division);
    String consumer_id = typeID.substring (indx+1);
    logger.debug("createACRQuery(), typeID:" +typeID+", query:"+query+ 
		", consumer_Id:"+consumer_id);
    return substituteNSN (query, consumer_id);
  } // createACRQuery

  /** Replaces the ":nsn" in the query with the actual NSN. 
   * @param q query string
   * @param nsn actual NSN
   * @return new query
   **/
  public String substituteNSN (String q, String nsn) {
    String query=null;
    if (q != null) {
      int indx = q.indexOf(":nsn");
      if (indx != -1) {
	query = q.substring(0,indx) + "'"+nsn+"'";
	if (q.length() > indx+4) {
	  query +=q.substring(indx+4);
	} // if
      } // if
    } // if
    //System.JTEST.out.println ("The string AFTER the substitution was " + query);  		
    return query;
  } // substituteNSN

  //#002
  public String substituteSupplyType (String q, String supplyType) {
    String query=null;
    if (q != null) {
      int indx = q.indexOf(":type");
      if (indx != -1) {
	query = q.substring(0,indx) + "'"+supplyType+"'";
	if (q.length() > indx+4) {
	  query += q.substring(indx+4);
	} // if
      } // if
    } // if
    return query;
  } // substituteSupplyType


  /**
   *  Replaces the ":nsns" in the query for the actual list of NSNs.
   *  @param q query string
   *  @param list actual list of NSNs
   *  @return new query or null if unsuccessful
   **/
  public String substituteNSNList (String q, List list) {
    StringBuffer query=new StringBuffer(220);
    if ((q != null) && !list.isEmpty()) {
      int indx=q.indexOf(":nsns");
      if (indx != -1) {
	boolean comma = false;
	query.append(q.substring(0, indx));
	Iterator i = list.iterator();
	while (i.hasNext()) {
	  if (comma) {
	    query.append(',');
	  } // if
	  query.append("'"+(String)i.next()+"'");
	  comma=true;
	} // while
	if (q.length() > indx+5) {
	  query.append(q.substring(indx+5));
	} // if
	return query.toString();
      } // if
    } // if
    return null;
  } // substituteNSNList

} // MEIPrototypeProvider



