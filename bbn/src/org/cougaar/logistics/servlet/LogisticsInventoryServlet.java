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

package org.cougaar.logistics.servlet;

import javax.servlet.*;
import javax.servlet.http.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.Date;


import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.logistics.ldm.Constants.Verb;
import org.cougaar.glm.ldm.asset.SupplyClassPG;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Inventory;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.TimeSpanSet;

import java.io.*;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.glm.ldm.oplan.Oplan;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.LoggingService;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryPG;


public class LogisticsInventoryServlet
  extends HttpServlet
{

  private SimpleServletSupport support;
  private AlarmService         alarmService;
  private LoggingService       logger;
  private boolean              printOrgActs;


  public void setSimpleServletSupport(SimpleServletSupport support) {
    this.support = support;
  }

  public void setLoggingService(LoggingService loggingService) {
    this.logger = loggingService;
  }

  public void setAlarmService(AlarmService anAlarmService) {
    this.alarmService = anAlarmService;
  }

  public void setPrintOrgActs(boolean enableOrgActs) {
    this.printOrgActs = enableOrgActs;
  }

  public void doGet(
		    HttpServletRequest request,
		    HttpServletResponse response) throws IOException, ServletException
  {
    // create a new "InventoryGetter" context per request
    InventoryGetter ig = new InventoryGetter(support,alarmService,logger,printOrgActs);
    ig.execute(request, response);
  }

  public void doPut(
		    HttpServletRequest request,
		    HttpServletResponse response) throws IOException, ServletException
  {
    // create a new "InventoryGetter" context per request
    InventoryGetter ig = new InventoryGetter(support,alarmService,logger,printOrgActs);
    try {
	ig.execute(request, response);
    } catch (Exception e) {
	e.printStackTrace();
    }
  }

  /**
   * This inner class does all the work.
   * <p>
   * A new class is created per request, to keep all the
   * instance fields separate.  If there was only one
   * instance then multiple simultaneous requests would
   * corrupt the instance fields (e.g. the "out" stream).
   * <p>
   * This acts as a <b>context</b> per request.
   */
  private static class InventoryGetter {

    public String desiredAssetName = "";
    ServletOutputStream out;

    /* since "InventoryGetter" is a static inner class, here
     * we hold onto the support API.
     *
     * this makes it clear that InventoryGetter only uses
     * the "support" from the outer class.
     */
    SimpleServletSupport support;
    AlarmService         alarmService;
    LoggingService       logger;
    boolean              printOrgActs;


    final public static String ASSET = "ASSET";
    final public static String ASSET_AND_CLASSTYPE = ASSET + ":" + "CLASS_TYPE:";

    public InventoryGetter(SimpleServletSupport aSupport,
			   AlarmService         anAlarmService,
			   LoggingService       aLoggingService,
			   boolean              enableOrgActs) {
      this.support = aSupport;
      this.alarmService = anAlarmService;
      this.logger = aLoggingService;
      this.printOrgActs = enableOrgActs;
    }

    /*
      Called when a request is received from a client.
      Either gets the command ASSET to return the names of all the assets
      that contain a ScheduledContentPG or
      gets the name of the asset to plot from the client request.
    */
    public void execute(
			HttpServletRequest req,
			HttpServletResponse res) throws IOException
    {

	LogisticsInventoryPG logInvPG=null;
	this.out = res.getOutputStream();



      int len = req.getContentLength();
      if (len > 0) {
	  //logger.debug("READ from content-length["+len+"]");
	InputStream in = req.getInputStream();
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        desiredAssetName = bin.readLine();
        bin.close();
	desiredAssetName = desiredAssetName.trim();
	logger.info("POST DATA: " + desiredAssetName);
      } else {
	logger.warn(" No asset to plot");
	return;
      }

      // return list of asset names
      if (desiredAssetName.equals(ASSET)||
	  desiredAssetName.startsWith(ASSET_AND_CLASSTYPE)) {

	  //DemandObjectPredicate assetNamePredicate;
	AssetPredicate assetNamePredicate;

	if(desiredAssetName.startsWith(ASSET_AND_CLASSTYPE)) {
	  String desiredClassType = desiredAssetName.substring(ASSET_AND_CLASSTYPE.length());
	  //assetNamePredicate = new DemandObjectPredicate(desiredClassType);
	  assetNamePredicate = new AssetPredicate(desiredClassType,logger);
	}
	else {
	    //assetNamePredicate = new DemandObjectPredicate();
	  assetNamePredicate = new AssetPredicate(logger);
	}

	// Asset no demand type handling
	/***
	 **
	 */

	 Vector assetNames = new Vector();
	 Collection container = support.queryBlackboard(assetNamePredicate);
	 for (Iterator i = container.iterator(); i.hasNext(); ) {
	 Inventory inv = (Inventory)(i.next());
	 logInvPG=null;
	 logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
	 TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
	 String nomenclature = typeIdPG.getNomenclature();
// 	 String typeId = typeIdPG.getTypeIdentification();
	 String typeId = inv.getItemIdentificationPG().getItemIdentification();
         int idx = typeId.indexOf(':');
         typeId = typeId.substring(idx+1);
	 if (nomenclature != null) {
	     nomenclature = nomenclature + ":" + typeId;
	 }
	 else {
	     nomenclature = typeId;
	 }
	 if(logInvPG.getIsLevel2()) {
	     nomenclature = typeId;
	 }

	 assetNames.addElement(nomenclature);
	 }

	 /***
	  * Below is for Demand Object Predicate
	  * MWD fix and try this out -below
	  * MWD get rid of old commented out above replaced by below
	  * to get demand even where no inventories.
	  ***

	HashSet assetNamesSet = new HashSet();
	Collection container = support.queryBlackboard(assetNamePredicate);

	for (Iterator i = container.iterator(); i.hasNext(); ) {
	  Asset asset = ((Task)(i.next())).getDirectObject();
	  TypeIdentificationPG typeIdPG = asset.getTypeIdentificationPG();
	  String nomenclature = typeIdPG.getNomenclature();
	  String typeId = typeIdPG.getTypeIdentification();
	  if (nomenclature != null)
	    nomenclature = nomenclature + ":" + typeId;
	  else
	    nomenclature = typeId;
	  assetNamesSet.add(nomenclature);
	}

	Vector assetNames = new Vector(assetNamesSet);
	 ****/

	// send the results
	ObjectOutputStream p = new ObjectOutputStream(out);
	p.writeObject(assetNames);
        logger.info("Sent asset names");
	return;
      } // end returning list of asset names

      if (desiredAssetName.startsWith("UID:")) {
	String desiredAssetUID = desiredAssetName.substring(4);
	Collection collection = support.queryBlackboard(new AssetUIDPredicate(desiredAssetUID,logger));

	for (Iterator i = collection.iterator(); i.hasNext(); ) {
	 Inventory inv = (Inventory)(i.next());
	 logInvPG=null;
	 logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
	 TypeIdentificationPG typeIdPG = logInvPG.getResource().getTypeIdentificationPG();
	 String nomenclature = typeIdPG.getNomenclature();
	 String typeId = typeIdPG.getTypeIdentification();
	 if (nomenclature == null)
	     return;
	 desiredAssetName = nomenclature + ":" + typeId;
	}
      } // end getting asset name from UID

      Date startDay=getStartDate();

      // get roles and determine if this cluster is a provider (or consumer)
      //logger.debug("\n****** look for roles for agent \""+support.getEncodedAgentName()+"\"");
      /**** MWD do we still need to know if we are a provider or not?
       ** all the role predicate code below
      RolePredicate rolePred = new RolePredicate(support.getEncodedAgentName());
      Collection roleCollection = support.queryBlackboard(rolePred);

      boolean provider = false;
      if (!roleCollection.isEmpty()) {
	Organization asset = (Organization) roleCollection.iterator().next();

	Collection roles = asset.getOrganizationPG().getRoles();
        if (roles != null) {
	  Iterator i = roles.iterator();
	  while (i.hasNext()) {
	    Role role = (Role)i.next();
	    if (role.getName().endsWith("Provider")) {
	      provider = true;
	      break;
	    }
	  }
	}
      }

      *****/

      // get asset and tasks we need to create the inventory

      logger.debug("Getting Inventory w/InventoryPredicate for " + desiredAssetName);

      InventoryPredicate inventoryPredicate = new InventoryPredicate(desiredAssetName, support.getEncodedAgentName(),logger);
      Collection collection = support.queryBlackboard(inventoryPredicate);

      if (collection.isEmpty()) {
        logger.warn("\n\n ************* collection is empty; return no response!");
	return;
      }

      // create UIInventory data object from the log plan objects
      String xmlStr = getXMLFromLogPlan(collection,startDay);

      // set values in UISimpleInventory, a serializable object
      //UISimpleInventory simpleInventory =
      //getInventoryForClient(inventory, provider, startDay);

      // send the String object
      if ((xmlStr != null) &&
	  (!(xmlStr.trim().equals("")))){
	  //ObjectOutputStream p = new ObjectOutputStream(out);
	//logger.debug("\n\n\n\n sending back a non-null inventory:\n"+simpleInventory);
	  //p.writeObject(xmlStr);
	  BufferedWriter p = new BufferedWriter(new OutputStreamWriter(out,Charset.forName("ASCII")));
	  p.write(xmlStr);
	  p.flush();
    p.close();
	logger.info("Sent XML document");
      } else {
        logger.error("XML string is null or empty.  returning null response.");
      }
    }

    protected String getXMLFromLogPlan(Collection collection,Date startDay) {
	 Inventory inv=null;
	 int ctr=0;
	 for (Iterator i = collection.iterator(); i.hasNext(); ) {
	     Object o = (Object) (i.next());
	     if(o instanceof Inventory) {
		 inv = (Inventory)o;
		 ctr++;
	     }
	 }
	 if(ctr > 1) {
	     logger.error("More than one inventory at this cluster with asset match");
	 }
	 if(inv == null) {
	     logger.error("No Inventory Match.  Can't send any data");
	     return null;
	 }
	 else {
	     StringWriter strWriter = new StringWriter();
	     BufferedWriter buffWriter = new BufferedWriter(strWriter);
	     LogisticsInventoryFormatter formatter = null;
	     formatter = new LogisticsInventoryFormatter(buffWriter,logger,startDay);
	     formatter.logToXMLOutput(inv,getOrgActivities(),alarmService.currentTimeMillis());
	     try {
		 buffWriter.flush();
		 strWriter.flush();
		 buffWriter.close();
	     }
	     catch(IOException ioe) {
		 throw new RuntimeException(
				   "Unable to create Servlet support: ",ioe);
	     }
	     return strWriter.toString();
	 }

    }


    protected Date getStartDate() {
      Date startingCDay=null;

      // get oplan

      Collection oplanCollection = support.queryBlackboard(oplanPredicate());

      if (!(oplanCollection.isEmpty())) {
        Iterator iter = oplanCollection.iterator();
        Oplan plan = (Oplan) iter.next();
	startingCDay = plan.getCday();
      }
      return startingCDay;
    }

    protected TimeSpanSet getOrgActivities() {
      TimeSpanSet orgActivities=null;
      if(printOrgActs) {
     	  Collection orgActCollect = support.queryBlackboard(orgActivityPred());
    	  orgActivities = new TimeSpanSet(orgActCollect);
      }
      else {
	      orgActivities = new TimeSpanSet();
      }
      return orgActivities;
    }


    private static UnaryPredicate oplanPredicate() {
      return new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return (o instanceof Oplan);
	  }
      };
    }


    private static UnaryPredicate orgActivityPred() {
      return new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return (o instanceof OrgActivity);
	  }
	};

    }
  }
}


/** Get asset which represents this cluster.
   */

class RolePredicate implements UnaryPredicate {
  String myCluster;

  public RolePredicate(String myCluster) {
    this.myCluster = myCluster;
  }

  public boolean execute(Object o) {
    if (o instanceof Organization) {
      Organization asset = (Organization)o;
      String s = asset.getItemIdentificationPG().getNomenclature();
      if (s != null)
	if (s.equals(myCluster))
	  return true;
    }
    return false;
  }

}

/** Subscribes to Logistics type inventories where the BG contains
    Buffered lists of information for each cycle.
  */

class InventoryPredicate implements UnaryPredicate {
  String desiredAssetName; // nomenclature:type id
  MessageAddress myClusterId;
  LoggingService logger;

  public InventoryPredicate(String desiredAssetName,
			    String myCluster,
			    LoggingService aLogger) {
    this.desiredAssetName = desiredAssetName;
    myClusterId = MessageAddress.getMessageAddress(myCluster);
    logger = aLogger;
  }

  private boolean assetMatch(Asset asset,Asset resource, boolean level2) {
    ItemIdentificationPG itemIdPG = asset.getItemIdentificationPG();
    TypeIdentificationPG typeIdPG = resource.getTypeIdentificationPG();
    if (itemIdPG == null) {
      logger.warn("No typeIdentificationPG for asset");
      return false;
    }
    String nomenclature = typeIdPG.getNomenclature();
    String itemId = itemIdPG.getItemIdentification();
    int idx = itemId.indexOf(':');
    itemId = itemId.substring(idx+1);
    if(level2) {
	nomenclature = itemId;
    }
    else if (nomenclature != null) {
	nomenclature = nomenclature + ":" + itemId;
    }
    else {
	nomenclature = itemId;
    }
    return nomenclature.equals(desiredAssetName);
  }

  /** Get Inventories at this cluster such that
      LogisticsInventoryPG().getResource().getTypeIdentificationPG().getNomenclature
      equals desiredAssetName.
      Also matches if asset uid is equal to desiredAssetName -- i.e.
      the client can pass in a UID instead of the asset name.
  */

  public boolean execute(Object o) {
    if (o instanceof Inventory) {
      // looking for Inventory Assets
      Inventory inv = (Inventory)o;
      LogisticsInventoryPG logInvPG=null;
      logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
      if (logInvPG == null)
	return false;
      Asset a1 = logInvPG.getResource();
      if (a1 == null) {
	logger.warn("no asset in Inventory in InventoryPredicate");
	return false;
      }
      return assetMatch(inv,a1,logInvPG.getIsLevel2());
    }
    return false;
  }
}

class AssetPredicate implements UnaryPredicate {

  private String supplyType;
  private LoggingService logger;

  public AssetPredicate(LoggingService aLogger) {
    super();
    supplyType = null;
    logger = aLogger;
  }

  public AssetPredicate(String theSupplyType, LoggingService aLogger) {
    super();
    supplyType = theSupplyType;
    logger = aLogger;
  }

  public boolean execute(Object o) {
    if (!(o instanceof Inventory))
      return false;
    Inventory inv = (Inventory)o;
    LogisticsInventoryPG logInvPG=null;
    logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
    if (logInvPG == null)
	return false;
    Asset a1 = logInvPG.getResource();
    if (a1 == null) {
	if(logger != null)
	    logger.warn("no asset in Inventory in InventoryPredicate");
      return false;
    }
    TypeIdentificationPG typeIdPG = a1.getTypeIdentificationPG();
    if (typeIdPG == null) {
	if(logger != null)
	    logger.warn(" No typeIdentificationPG for asset");
      return false;
    }
    //If we care about supply type make sure direct object matches supply type
    if (supplyType != null) {
      SupplyClassPG pg = (SupplyClassPG)a1.searchForPropertyGroup(SupplyClassPG.class);
      if ((pg == null) ||
	  (!(supplyType.equals(pg.getSupplyType())))){
	return false;
      }
      /***
	  if (pg == null) {
	  logger.warn(" Null Supply type");
	  return false;
	  }
	  else if (!(supplyType.equals(pg.getSupplyType()))){
	  if(logger != null)
	  logger.warn(" The Supply type is: " + pg.getSupplyType());
	  return false;
	  }
	  logger.debug("NO WARNING: SUCCESS got Asset of right type");
      ***/
    }
    return true;
  }
}

class DemandObjectPredicate implements UnaryPredicate {

  private String supplyType;
  private LoggingService logger;

  public DemandObjectPredicate(LoggingService aLogger) {
    super();
    supplyType = null;
    logger = aLogger;
  }

  public DemandObjectPredicate(String theSupplyType,
			       LoggingService aLogger) {
    super();
    supplyType = theSupplyType;
    logger = aLogger;
  }

  public boolean execute(Object o) {
    if (!(o instanceof Task))
      return false;
    Task task = (Task)o;
    if(!((task.getVerb().equals(Constants.Verb.PROJECTSUPPLY)) ||
	 (task.getVerb().equals(Constants.Verb.SUPPLY))))
      return false;
    Asset asset = task.getDirectObject();
    if (asset == null)
      return false;
    TypeIdentificationPG typeIdPG = asset.getTypeIdentificationPG();
    if (typeIdPG == null) {
	if(logger != null)
	    logger.warn(" No typeIdentificationPG for asset");
      return false;
    }
    //If we care about supply type make sure direct object matches supply type
    if (supplyType != null) {
      SupplyClassPG pg = (SupplyClassPG)asset.searchForPropertyGroup(SupplyClassPG.class);
      if ((pg == null) ||
	  (!(supplyType.equals(pg.getSupplyType())))){
	return false;
      }
    }
    return true;
  }
}

class AssetUIDPredicate implements UnaryPredicate {
  String desiredAssetUID;
    private LoggingService logger;

  public AssetUIDPredicate(String desiredAssetUID,LoggingService aLogger) {
    this.desiredAssetUID = desiredAssetUID;
    logger = aLogger;
  }

  public boolean execute(Object o) {
    if (!(o instanceof Inventory))
      return false;
    Inventory inv = (Inventory)o;
    if (inv.getUID() == null)
      return false;
    if (!inv.getUID().toString().equals(desiredAssetUID))
      return false;
      LogisticsInventoryPG logInvPG=null;
      logInvPG = (LogisticsInventoryPG)inv.searchForPropertyGroup(LogisticsInventoryPG.class);
    if (logInvPG == null)
      return false;
    Asset a1 = logInvPG.getResource();
    if (a1 == null) {
      logger.warn("no asset in Inventory in InventoryPredicate");
      return false;
    }
    TypeIdentificationPG typeIdPG = a1.getTypeIdentificationPG();
    if (typeIdPG == null) {
      logger.warn(" No typeIdentificationPG for asset");
      return false;
    }
    return true;
  }

}












