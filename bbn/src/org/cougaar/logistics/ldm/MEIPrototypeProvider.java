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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.glm.ldm.QueryLDMPlugin;
import org.cougaar.glm.ldm.asset.ClassVIIMajorEndItem;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.NewMovabilityPG;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.logistics.ldm.asset.*;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.util.UnaryPredicate;

import java.text.StringCharacterIterator;
import java.util.Collection;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class MEIPrototypeProvider extends QueryLDMPlugin implements UtilsProvider {


  private boolean configured;

  public static final String MEI_STRING = "ClassVIIMajorEndItem";
  public static final String THEATER = "SWA";
  public static final String LEVEL2 = "Level2";

  private String service = null;
  private String echelon = null;
  private static final String LEVEL2TOGGLE = "-Level2";
  // By default, do level 2
  private boolean level2Off = false;
  // Subscription to policies
  private IncrementalSubscription meiSubscription;

  // MEI Consumption
  public static final int AMMO  = 0;
  public static final int FUEL  = 1;
  public static final int PKG_POL  = 2;
  public static final int SPARES  = 3;

//   private IncrementalSubscription myOrganizations;
  protected Organization myOrg;

  private ServiceBroker serviceBroker;
  private LoggingService logger;
  private TaskUtils taskUtils;
  private TimeUtils timeUtils;
  private AssetUtils assetUtils;
  private ScheduleUtils scheduleUtils;
  private BlackboardQueryService queryService;
  private PlanningFactory factory;
  private boolean publishedLevel2MeiAsset = false;

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

  private static UnaryPredicate level2MeiPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Level2MEIAsset);
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

  public BlackboardQueryService getBlackboardQueryService(Object requestor) {
    return (BlackboardQueryService)serviceBroker.getService(requestor,
                                                            BlackboardQueryService.class,
                                                            null);
  }

  private PlanningFactory getPlanningFactory() {
    PlanningFactory rootFactory = null;
    rootFactory = (PlanningFactory) getFactory("planning");
    return rootFactory;
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
      factory = getPlanningFactory();
      if (queryService == null) {
        if (logger.isErrorEnabled()) {
          logger.error("Unable to get query service");
        }
      }
    }
    Vector params = getParameters();
    if (params != null) {
      level2Off = params.contains(LEVEL2TOGGLE);
    }
  }

  protected void setupSubscriptions() {
    super.setupSubscriptions();
    meiSubscription =
        (IncrementalSubscription) subscribe (new ClassVIIPredicate());
    if (didRehydrate()) {
      if (logger.isInfoEnabled())
	logger.info("Did Rehydrate -- will do rehydrate() method");
      rehydrate();
    } else if (logger.isInfoEnabled())
      logger.info("Did not rehydrate.");
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
          if (logger.isErrorEnabled()) {
            logger.error("no prototype for "+asset);
          }
        } // if
        if ((proto != null) && (!good_prototypes.contains(proto))) {
          TypeIdentificationPG tip = asset.getTypeIdentificationPG();
          if (tip!= null) {
            String type_id = tip.getTypeIdentification();
            if (type_id != null) {
              getLDM().cachePrototype(type_id, proto);
              good_prototypes.add(proto);
            } else {
              if (logger.isErrorEnabled()) {
                logger.error("cannot rehydrate "+proto+" no typeId");
              }
            }
          } else {
            if (logger.isErrorEnabled()) {
              logger.error("cannot rehydrate "+proto+" no typeIdPG");
            }
          }
        }
      }
      addConsumerPGs(meiSubscription);
    } else if (logger.isInfoEnabled())
      logger.info("in rehydrate still not configured after call to configure - so not doing addConsumerPGs");
  }

  public void execute() {
//     System.out.println("Execute called for "+getAgentIdentifier());
    if (!configured) {
      configure();
    }
    if (!meiSubscription.isEmpty()) {
//       System.out.println("Calling addConsumerPGs() for "+getAgentIdentifier());
      addConsumerPGs(meiSubscription);
    } else if (logger.isInfoEnabled())
      logger.info("execute had empty meiSubscription");
  }

  protected void configure() {
//     System.out.println(" configuring MEIPrototypeProvider for "+getAgentIdentifier());
    Iterator new_orgs = queryService.query(orgsPredicate).iterator();
    if (new_orgs.hasNext()) {
      myOrg = (Organization) new_orgs.next();
      Service srvc = myOrg.getOrganizationPG().getService();
      if (srvc != null) {
        service= srvc.toString();
//         System.out.println("MEIPrototypeProvider configured for "+getAgentIdentifier());
        if (!level2Off) {
          if (!publishedLevel2MeiAsset && !queryLevel2Mei()) {
            publishLevel2Mei();
          }
        }
        configured = true;
      } else {
        if (logger.isErrorEnabled()) {
          logger.error("Organization has no Service :"+
                       myOrg.getItemIdentificationPG().getItemIdentification());
        }
      }
      echelon = myOrg.getMilitaryOrgPG().getEchelon();
      if (echelon == null) {
        if (logger.isErrorEnabled()) {
          logger.error("Organization has no echelon, will not produce Ammunition demand:"+
                       myOrg.getItemIdentificationPG().getItemIdentification());
        }
      }
    } else if (logger.isInfoEnabled())
      logger.info("No self orgs from query service?");
  }

  private void publishLevel2Mei() {
    boolean[] consumed = checkLevel2MeiConsumption();
    /*
    * Sorry for the confusion, the requirements have evolved, only create and publish
    * level 2 MEIs if there is consumption.
    */
    if (consumesAnything(consumed)) {
      Asset asset = factory.createPrototype(Level2MEIAsset.class,
                                            "Level2MEI");
      Asset a = factory.createInstance(asset);
      setupAvailableSchedule(a);
      if (logger.isInfoEnabled())
	logger.info("MEIPrototypeProvider publishing Level2MEI asset in agent: " + getAgentIdentifier());
      publishAdd(a);
    }
    publishedLevel2MeiAsset = true;
  }

  // In case we rehydrate, query the blackboard
  private boolean queryLevel2Mei() {
    Collection container = queryService.query(level2MeiPredicate);
    Object o = null;
    if (!container.isEmpty()) {
      o = container.iterator().next();
      if (o instanceof Level2MEIAsset) {
        // set this so that we don't keep querying
        publishedLevel2MeiAsset = true;
        return true;
      }
    }
    return false;
  }

  protected boolean[] checkLevel2MeiConsumption() {
    // by default, assume false
    boolean result[] = {false, false, false, false };
    // not implemented yet
    result[PKG_POL]= false;
    result[SPARES]= false;

    String query = (String) fileParameters_.get ("UnitConsumption");
    if (query == null) {
      if (logger.isErrorEnabled()) {
        logger.error("checkLevel2MEIConsumption() query is null ");
      }
      return result;
    }
    query = substituteOrgName(query, getAgentIdentifier().toString());
    Vector qresult = null;
    try {
      qresult = executeQuery (query);
      if (logger.isDebugEnabled())
	logger.debug("checkLevel2MEIConsumption() execute query: " +query);
      if (qresult.isEmpty()) {
        // TODO:  this should be a warn, but we are currently getting one and
        // I have asked Gary to look into it.  After that, we should switch back.
	if (logger.isDebugEnabled())
	  logger.debug ("No results returned for Level2MEIConsumption query:  " + query);
      } else {
        Object row[] = (Object[])qresult.firstElement();
        result[AMMO]=convertConsumptionElement(row[AMMO]);
        // not implemented yet
        //result[PKG_POL]=convertConsumptionElement(row[PKG_POL]);
        //result[SPARES]=convertConsumptionElement(row[SPARES]);
        result[FUEL]=convertConsumptionElement(row[FUEL]);
      }
    } catch (Exception ee) {
      if (logger.isErrorEnabled()) {
        logger.error ( "in checkLevel2MeiConsumption(), DB query failed.Query= "+query);
      }
    }
    return result;
  }

  protected  boolean[] checkMeiConsumption(Asset asset) {
    // The default is that the MEI consumes all 4 classes.
    // This prevents problems if the table hasn't been updated for
    // a new MEI.
    boolean result[] = {true, true, true, true };

    if (asset instanceof Level2MEIAsset) {
      return checkLevel2MeiConsumption();
    }

    String type_id = asset.getTypeIdentificationPG().getTypeIdentification();
    String query = (String) fileParameters_.get ("MeiConsumption");
    String consumer_id = type_id.substring(type_id.indexOf("/")+1);
    query = substituteNSN (query, consumer_id);

    Vector qresult;

    try {
      qresult = executeQuery (query);
      if (logger.isDebugEnabled())
	logger.debug ("in checkMeiConsumption() query complete for asset "+
                    asset.getTypeIdentificationPG().getNomenclature()+
                    "\nquery= "+query);
      if (qresult.isEmpty()) {
	if (logger.isDebugEnabled())
	  logger.debug ("no result for asset " +
                      asset.getTypeIdentificationPG().getNomenclature());
      } else {
        Object row[] = (Object[])qresult.firstElement();
        result[AMMO]=convertConsumptionElement(row[AMMO]);
        result[PKG_POL]=convertConsumptionElement(row[PKG_POL]);
        result[SPARES]=convertConsumptionElement(row[SPARES]);
        result[FUEL]=convertConsumptionElement(row[FUEL]);
      } // if else
    } catch (Exception ee) {
      if (logger.isErrorEnabled()) {
        logger.error ( "in checkMeiConsumption(), DB query failed.Query= "+query);
      }
    } // try

    return result;
  }

  private boolean convertConsumptionElement(Object ele) {
    boolean res = true;
    try {
      if (ele.toString().equals("0")) {
        res = false;
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error ( "in convertConsumptionElement(), convert failed; ele= "+ele);
      }
    }
    return res;
  }

  private boolean consumes(boolean[] consumed, int type) {
    boolean res = true;
    try {
      res = consumed[type];
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error ( "in consumes(), array access failed");
      }
    }
    return res;
  }

  private boolean consumesAnything(boolean[] consumed) {
    for (int i = 0; i < consumed.length; i++) {
      boolean b = consumed[i];
      if (b == true) {
        return true;
      }
    }
    return false;
  }

  // On rehydration, this should just re-initialize the PGs
  // Otherwise, it should create the various consumer PGs for the MEIs as necessary,
  // putting the appropriate BG on them as well
  // It only creates PGs for the MEIs that consume that supply class
  protected void addConsumerPGs(Collection meiConsumers) {
    if (logger.isInfoEnabled())
      logger.info(getAgentIdentifier() + " in addConsumerPGs with " + meiConsumers.size() + " meis");
    Iterator meis = meiConsumers.iterator();
    Asset a, anAsset;

    // Loop over all the MEIs, adding PGs as necessary
    while (meis.hasNext()) {
      // Reset this boolean for each Asset so decision to publishChange is accurate
      boolean addedPG = false; // Did we add any PGs to this Asset?

      a = (Asset)meis.next();
      if (a instanceof AggregateAsset) {
        anAsset = ((AggregateAsset)a).getAsset();
      } else {
        anAsset = a;
      }

      if (anAsset instanceof ClassVIIMajorEndItem) {
	if (logger.isInfoEnabled())
	  logger.info(getAgentIdentifier() + ".addConsumerPGs for Class7MEI: " + anAsset);

        if (anAsset instanceof Level2MEIAsset) {
          //This cargo category code means, "do not transport me."
          setNoTransportCargoCode(anAsset);
        }

        boolean[] consumed = checkMeiConsumption(anAsset);

	// For each class of supply, add the appropriate consumer PG as necessary
        if (consumes(consumed, FUEL)) {
	  NewFuelConsumerPG fuelpg = (NewFuelConsumerPG) anAsset.searchForPropertyGroup(FuelConsumerPG.class);
	  if ( fuelpg == null) {
	    if (logger.isInfoEnabled())
	      logger.info("addConsumerPGs() CREATING FuelConsumerPG for "+anAsset+" in "+
			  getAgentIdentifier());

	    fuelpg =
              (NewFuelConsumerPG)getLDM().getFactory().createPropertyGroup(FuelConsumerPG.class);
	    fuelpg.setMei(a);
	    fuelpg.setService(service);
	    fuelpg.setTheater(THEATER);
	    if (anAsset instanceof Level2MEIAsset) {
	      fuelpg.setFuelBG(new Level2FuelConsumerBG(fuelpg));
	    } else {
	      fuelpg.setFuelBG(new FuelConsumerBG(fuelpg));
	    }
	    fuelpg.initialize(this);
	    anAsset.setPropertyGroup(fuelpg);
	    addedPG = true;
	  } else {
	    if (logger.isInfoEnabled())
	      logger.info(getAgentIdentifier() + ".addConsPGs Re-BGing FuelConsumerPG for " + anAsset);
	    if (didRehydrate()) {
	      if (logger.isInfoEnabled())
		logger.info("          Due to rehydrate. Will re-initialize BG only.");
	      fuelpg.initialize(this);
	    } else {
	      if (logger.isDebugEnabled())
		logger.debug("          DidRehydrate is false. Blindly assume the PG is ok and avoid doing excess work.");

	      // Note that we could check the Mei, Service, Theater, and BG. But there's
	      // no situation where they should not be OK.
	    }
	  }
	} else {
	  if (logger.isInfoEnabled())
	    logger.info(getAgentIdentifier() + ".addConsPGs not putting FuelPG on asset that does not consume it: " + anAsset);
	}

        if (consumes(consumed, AMMO)) {
          NewAmmoConsumerPG ammopg =
	    (NewAmmoConsumerPG)anAsset.searchForPropertyGroup(AmmoConsumerPG.class);
	  if (ammopg == null) {
	    if (logger.isInfoEnabled())
	      logger.info("addConsumerPGs() CREATING AmmoConsumerPG for "+anAsset+" in "+
			  getAgentIdentifier());
	    ammopg =
              (NewAmmoConsumerPG)getLDM().getFactory().createPropertyGroup(AmmoConsumerPG.class);
	    ammopg.setMei(a);
	    ammopg.setService(service);
	    ammopg.setTheater(THEATER);
	    if (anAsset instanceof Level2MEIAsset) {
	      ammopg.setAmmoBG(new Level2AmmoConsumerBG(ammopg));
	    } else {
	      ammopg.setAmmoBG(new AmmoConsumerBG(ammopg));
	    }
	    ammopg.initialize(this);
	    anAsset.setPropertyGroup(ammopg);
	    addedPG = true;
	  } else {
	    if (logger.isInfoEnabled())
	      logger.info(getAgentIdentifier() + ".addConsPGs Re-BGing AmmoConsumerPG for " + anAsset);
	    if (didRehydrate()) {
	      if (logger.isInfoEnabled())
		logger.info("          Due to rehydrate. Will re-initialize BG only.");
	      ammopg.initialize(this);
	    } else {
	      if (logger.isDebugEnabled())
		logger.debug("          DidRehydrate is false. Blindly assume the PG is ok and avoid doing excess work.");

	      // Note that we could check the Mei, Service, Theater, and BG. But there's
	      // no situation where they should not be OK.
	    } // end of block to handle re-BG not rehydrate
	  } // end of block to handle existing PG
	} else {
	  if (logger.isInfoEnabled())
	    logger.info(getAgentIdentifier() + ".addConsPGs not putting AmmopG on asset that does not consume it: " + anAsset);
	}
	
        if (consumes(consumed, PKG_POL)) {
          NewPackagedPOLConsumerPG packagedpg = (NewPackagedPOLConsumerPG)anAsset.searchForPropertyGroup(PackagedPOLConsumerPG.class);
	  if (packagedpg == null) {
	    if (logger.isInfoEnabled())
	      logger.info("addConsumerPGs() CREATING PackagedPOLConsumerPG for "+anAsset+" in "+
			  getAgentIdentifier());
	    
	    packagedpg =
              (NewPackagedPOLConsumerPG)getLDM().getFactory().createPropertyGroup(PackagedPOLConsumerPG.class);
	    packagedpg.setMei(a);
	    packagedpg.setService(service);
	    packagedpg.setTheater(THEATER);
	    packagedpg.setPackagedPOLBG(new PackagedPOLConsumerBG(packagedpg));
	    packagedpg.initialize(this);
	    anAsset.setPropertyGroup(packagedpg);
	    addedPG = true;
	  } else {
	    // Already had this PG
	    if (logger.isInfoEnabled())
	      logger.info(getAgentIdentifier() + ".addConsPGs Re-BGing PackagedPOLConsumerPG for " + anAsset);
	    if (didRehydrate()) {
	      if (logger.isInfoEnabled())
		logger.info("          Due to rehydrate. Will re-initialize BG only.");
	      packagedpg.initialize(this);
	    } else {
	      if (logger.isDebugEnabled())
		logger.debug("          DidRehydrate is false. Blindly assume the PG is ok and avoid doing excess work.");

	      // Note that we could check the Mei, Service, Theater, and BG. But there's
	      // no situation where they should not be OK.
	    } // end of block to handle non-rehydrate existing PG
	      
	  } // end of block to handle existing PG
	} else {
	  if (logger.isInfoEnabled())
	    logger.info(getAgentIdentifier() + ".addConsPGs not putting PackagedPOLPG on asset that does not consume it: " + anAsset);
	}

        if (consumes(consumed, SPARES)) {
          NewRepairPartConsumerPG partpg =
	    (NewRepairPartConsumerPG)anAsset.searchForPropertyGroup(RepairPartConsumerPG.class);
	  if (partpg == null) {
	    if (logger.isInfoEnabled())
	      logger.info("addConsumerPGs() CREATING RepairPartConsumerPG for "+anAsset+" in "+
			  getAgentIdentifier());
	    
	    partpg =
              (NewRepairPartConsumerPG)getLDM().getFactory().createPropertyGroup(RepairPartConsumerPG.class);
	    partpg.setMei(a);
	    partpg.setService(service);
	    partpg.setTheater(THEATER);
	    partpg.setRepairPartBG(new RepairPartConsumerBG(partpg));
	    partpg.initialize(this);
	    anAsset.setPropertyGroup(partpg);
	    addedPG = true;
	  } else {
	    // Already had this PG
	    if (logger.isInfoEnabled())
	      logger.info(getAgentIdentifier() + ".addConsPGs Re-BGing RepairPartConsumerPG for " + anAsset);
	    if (didRehydrate()) {
	      if (logger.isInfoEnabled())
		logger.info("          Due to rehydrate. Will re-initialize BG only.");
	      partpg.initialize(this);
	    } else {
	      if (logger.isDebugEnabled())
		logger.debug("          DidRehydrate is false. Blindly assume the PG is ok and avoid doing excess work.");

	      // Note that we could check the Mei, Service, Theater, and BG. But there's
	      // no situation where they should not be OK.
	    } // end of block to handle non-rehydrate existing PG
	    
	  } // end of block to handle existing PG
	} else {
	  if (logger.isInfoEnabled())
	    logger.info(getAgentIdentifier() + ".addConsPGs not putting RepairPartPG on asset that does not consume it: " + anAsset);
	} // end of block to handle RepairPartPG at all
	
	// Only publishChange the Asset if we really changed it
        if (addedPG) {
          publishChange(a);
        } else if (logger.isInfoEnabled()) {
	  logger.info(getAgentIdentifier() + ".addConsumerPGs did not add any PGs to " + a);
	}
      } else {
	// Not a Class7MEI
	if (logger.isInfoEnabled())
	  logger.info(getAgentIdentifier() + ".addConsumerPGs had non Class7MEI: " + a);
      }
    } // while loop over MEIs
  } // addConsumerBGs method

  private void setNoTransportCargoCode(Asset anAsset) {
    MovabilityPG pg = (MovabilityPG) anAsset.searchForPropertyGroup(MovabilityPG.class);
    if (pg.getCargoCategoryCode() == null) {
      NewMovabilityPG newpg = (NewMovabilityPG) getLDM().getFactory().createPropertyGroup(MovabilityPG.class);
      newpg.setMoveable(false);
      newpg.setCargoCategoryCode("000");
      anAsset.setPropertyGroup(newpg);
    }
  }

  public void setServiceBroker(ServiceBroker serviceBroker) {
    this.serviceBroker = serviceBroker;
  }

  public Organization getMyOrg() {
    return myOrg;
  }

  public boolean canHandle (String typeid, Class class_hint) {
    Boolean protoProvider = (Boolean) myParams_.get("PrototypeProvider");
    if (logger.isDebugEnabled())
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
    if (logger.isDebugEnabled())
      logger.debug("canHandle(), Unable to provider Prototype."+
                 " ProtoProvider = "+protoProvider+", typeid= "+typeid);
    return false;
  }


  public Asset makePrototype (String type_name, Class class_hint) {
    // Demand Rate Queries are based upon service
    if (!configured) {
      configure();
      if (!configured) {
        if (logger.isErrorEnabled()) {
          logger.error("makePrototype() plugin missing myOrganization");
        }
        return null;
      }
    }
    if ((class_hint != null) &&  !class_hint.getName().equals(MEI_STRING)) {
      if (logger.isErrorEnabled()) {
        logger.error("make prototype How did we get this far?? "+class_hint);
      }
      return null;
    }

    if (!configured) {
      if (logger.isErrorEnabled()) {
        logger.error("makePrototype("+type_name+","+class_hint+") PlugIn not configured yet");
      }
      return null;
    }

    // create initial asset
    String nomen = getMEINomenclature(type_name, service);
    if (nomen == null) {
      return null;
    } // if
    if (logger.isDebugEnabled())
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
    if (logger.isDebugEnabled())
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
        if (logger.isErrorEnabled()) {
          logger.error("retrieveFromDB(), DB query failed. query= "+query+
                       "\n ERROR "+ee);
        }
        return null;
      }
    }
    return nomen;
  } // getMEINomenclature


  public void fillProperties (Asset anAsset) {
  }

  public Asset getPrototype(String typeid) {
    return getLDM().getFactory().getPrototype(typeid);
  }

  // Queries the DB to retrieve the parts for an MEI and the consumption rates for each
  // part in order to create and AssetConsumptionRate object.
  // public AssetConsumptionRate lookupAssetConsumptionRate
  public Vector lookupAssetConsumptionRate(Asset asset, String asset_type,
                                           String service, String theater) {
    if (logger.isDebugEnabled())
      logger.debug ("lookupAssetConsumptionRate()");

    String query = createACRQuery (asset, asset_type, service, theater);
    if (query == null) {
      if (logger.isErrorEnabled()) {
        logger.error("lookupAssetConsumptionRate() Invalid ACR query for "+
                     asset_type+service);
      }
      return null;
    } // if
    if (logger.isDebugEnabled())
      logger.debug("lookupAssetConsumptionRate() ACR query for "+
                 asset_type+service+" = "+query);
    Vector result;
    try {
      result = executeQuery (query);
      if (logger.isDebugEnabled())
	logger.debug ("in lookupAssetConsumptionRate() query complete for asset "+
                    asset.getTypeIdentificationPG().getNomenclature()+
                    "\nquery= "+query);
      if (result.isEmpty()) {
	if (logger.isDebugEnabled())
	  logger.debug ("no result for asset " +
                      asset.getTypeIdentificationPG().getNomenclature());
        return null;
      } // if
    } catch (Exception ee) {
      if (logger.isErrorEnabled()) {
        logger.error ( "in lookupAssetConsumptionRate(), DB query failed.Query= "+query);
      }
      return null;
    } // try
//     return parseACRResults (result, asset_type);
    return result;
  } // lookupAssetConsumptionRate

  public Vector lookupLevel2AssetConsumptionRate(String agent, Asset asset, String supply_type) {
    Vector result = null;
    String query = (String) fileParameters_.get (LEVEL2 + supply_type + "Rate");
    if (query == null) {
      if (logger.isErrorEnabled()) {
        logger.error("lookupLevel2AssetConsumptionRate() ACR query is null for "+
                     LEVEL2+supply_type);
      }
      return null;
    }
    query = substituteOrgName(query, agent);
    if (logger.isDebugEnabled())
      logger.debug("lookupLevel2AssetConsumptionRate() ACR query for "+ LEVEL2 + supply_type + " = "+query);
    try {
      result = executeQuery (query);
      if (logger.isDebugEnabled())
	logger.debug ("in lookupLevel2AssetConsumptionRate() query complete for asset "+
                    asset.getTypeIdentificationPG().getNomenclature()+
                    "\nquery= "+query);
      if (result.isEmpty()) {
	if (logger.isDebugEnabled())
	  logger.debug ("no result for asset " +
                      asset.getTypeIdentificationPG().getNomenclature());
        return null;
      }
    } catch (Exception ee) {
      if (logger.isErrorEnabled()) {
        logger.error ( "in lookupLevel2AssetConsumptionRate(), DB query failed.Query= "+query);
      }
      return null;
    }
    return result;
  }

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
    String division;
    try {
      division = typeID.substring (0, indx);
    } catch (Exception exc) {
      if (logger.isErrorEnabled()) {
        logger.error(" ########### "+typeID+" "+exc.getMessage());
      }
      exc.printStackTrace();
      return null;
    }
    String query = (String) fileParameters_.get (asset_type+service+division);
    String consumer_id = typeID.substring (indx+1);
    if (logger.isDebugEnabled())
      logger.debug("createACRQuery(), typeID:" +typeID+", query:"+query+
                 ", consumer_Id:"+consumer_id);
    if (asset_type.equals("Ammunition")) {
      String tmpQuery = substituteNSN(query, consumer_id);
      return substituteEchelon(tmpQuery);
    } else {
      return substituteNSN (query, consumer_id);
    }
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

  /** Replaces the ":echelon" in the ammunition query with the actual value.
   * @param q query string
   * @return new query
   **/
  public String substituteEchelon (String q) {
    String query=null;
    if ((q != null) && (echelon != null)) {
      int idx = q.indexOf(":echelon");
      if (idx != -1) {
        query = q.substring(0, idx) + "'"+echelon+"'";
        if (q.length() > idx+8) {
          query += q.substring(idx+8);
        }
      }
    }
    return query;
  }

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

  public String substituteOrgName (String q, String agent_name) {
    StringBuffer query = new StringBuffer();
    q = q.trim();
    agent_name = "'"+agent_name+"'";
    if (q != null) {
      int indx = q.indexOf(":org");
      if (indx != -1) {
        query.append(q.substring(0,indx) + agent_name);
        if (q.length() > indx) {
          query.append(q.substring(indx + 4));
        }
      }
    }
    return query.toString();
  }

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

  private void setupAvailableSchedule(Asset asset) {
    Calendar mycalendar = Calendar.getInstance();
    // set the start date of the available schedule to 01/01/1990
    mycalendar.set(1990, 0, 1, 0, 0, 0);
    Date start = mycalendar.getTime();
    // set the end date of the available schedule to 01/01/2010
    mycalendar.set(2010, 0, 1, 0, 0, 0);
    Date end = mycalendar.getTime();
    NewSchedule availsched = factory.newSimpleSchedule(start, end);
    // set the available schedule
    ((NewRoleSchedule)asset.getRoleSchedule()).setAvailableSchedule(availsched);
  }
} // MEIPrototypeProvider



