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

package org.cougaar.logistics.ldm;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.glm.ldm.QueryLDMPlugin;
import org.cougaar.glm.ldm.asset.MilitaryPerson;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.plan.Service;
import org.cougaar.logistics.ldm.asset.NewSubsistenceConsumerPG;
import org.cougaar.logistics.ldm.asset.SubsistenceConsumerBG;
import org.cougaar.logistics.ldm.asset.SubsistenceConsumerPG;
import org.cougaar.logistics.plugin.inventory.AssetUtils;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.UtilsProvider;
import org.cougaar.logistics.plugin.utils.ScheduleUtils;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import java.util.ArrayList;
import java.util.List;

// Prototype Provider for Class I Subsistence Rations

public class ClassIConsumerPrototypeProvider extends QueryLDMPlugin implements UtilsProvider {
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

  private static UnaryPredicate orgsPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        return ((Organization) o).isSelf();
      }
      return false;
    }
  };

  // used for rehydration
  private static UnaryPredicate personPredicate() {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MilitaryPerson) {
          return true;
        }
        if (o instanceof AggregateAsset) {
          if (((AggregateAsset) o).getAsset() instanceof MilitaryPerson) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public TaskUtils getTaskUtils() {
    return taskUtils;
  }

  public TimeUtils getTimeUtils() {
    return timeUtils;
  }

  public AssetUtils getAssetUtils() {
    return assetUtils;
  }

  public ScheduleUtils getScheduleUtils() {
    return scheduleUtils;
  }

  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService) serviceBroker.getService(requestor,
                                                     LoggingService.class,
                                                     null);
  }

  public BlackboardQueryService getBlackboardQueryService(Object requestor) {
    return (BlackboardQueryService) serviceBroker.getService(requestor,
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
        if (logger.isErrorEnabled()) {
          logger.error("Unable to get query service");
        }
      }
    }
  }

  protected void setupSubscriptions() {
    super.setupSubscriptions();
    myOrganizations = (IncrementalSubscription) subscribe(orgsPredicate);
    consumerSubscription = (IncrementalSubscription) subscribe(personPredicate());
    if (didRehydrate()) {
      rehydrate();
    } // if
  } // setupSubscriptions

  public Organization getMyOrg() {
    return myOrg;
  }

  protected void rehydrate() {
    configure();
    if (logger.isDebugEnabled()) {
      logger.debug("Rehydrated - configured " + configured);
    }
    if (configured) {
      // rehook handlers
      Enumeration consumers = consumerSubscription.elements();
      Asset asset, proto;
      //boolean success;
      Vector good_prototypes = new Vector();

      while (consumers.hasMoreElements()) {
        asset = (Asset) consumers.nextElement();
        if (asset instanceof AggregateAsset) {
          proto = ((AggregateAsset) asset).getAsset();
        } else {
          proto = asset.getPrototype();
        }
        if (proto == null) {
          if (logger.isErrorEnabled()) {
            logger.error("no prototype for " + asset);
          }
        }
        if ((proto != null) && (!good_prototypes.contains(proto))) {
          TypeIdentificationPG tip = asset.getTypeIdentificationPG();
          if (tip != null) {
            String type_id = tip.getTypeIdentification();
            if (type_id != null) {
              getLDM().cachePrototype(type_id, proto);
              good_prototypes.add(proto);
              if (logger.isDebugEnabled()) {
                logger.debug("Rehydrated asset " + asset + " w/ proto " + proto);
              }
            } else {
              if (logger.isErrorEnabled()) {
                logger.error("cannot rehydrate " + proto + " no typeId");
              }
            }
          } else {
            if (logger.isErrorEnabled()) {
              logger.error("cannot rehydrate " + proto + " no typeIdPG");
            }
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

  public Collection generateRationList() {
    List list = new ArrayList();
    String query = (String) fileParameters_.get("Class1ConsumedList");
    if (query == null) { // if query not found, return null
      if (logger.isDebugEnabled()) {
        logger.debug("generaterationList(),  query is null");
      }
      return null;
    }
    Vector holdsQueryResult;
    try {
      holdsQueryResult = executeQuery(query);
      if (holdsQueryResult.isEmpty()) {
        return null;
      }
    } catch (Exception ee) {
      if (logger.isDebugEnabled()) {
        String str = " DB query failed. query= " + query + "\n ERROR " + ee.toString();
        logger.debug(" getSupplementalList()," + str);
      }
      return null;
    }
    String typeIDPrefix = "NSN/";
    PlanningFactory ldm = getLDM().getFactory();
    for (int i = 0; i < holdsQueryResult.size(); i++) {
      Object[] row = ((Object[]) holdsQueryResult.elementAt(i));
      Asset ration = ldm.getPrototype(typeIDPrefix + (String) row[0]);
      if (ration != null) {
        list.add(ration);
      } else {
        if (logger.isErrorEnabled()) {
          logger.error(" Asset prototype is null");
        }
      } // if
    } // for
    return list;
  }

  protected void addConsumerPGs(Collection consumers) {
    if (logger.isDebugEnabled()) {
      logger.debug(getAgentIdentifier() + ".addConsumerPGs with " + consumers.size() + " people");
    }
    Iterator people = consumers.iterator();
    Asset a, anAsset;

    // Loop over all eaters, adding the PG as necessary
    while (people.hasNext()) {
      a = (Asset) people.next();
      if (a instanceof AggregateAsset) {
        anAsset = ((AggregateAsset) a).getAsset();
      } else {
        anAsset = a;
      }
      if (anAsset instanceof MilitaryPerson) {
        if (logger.isDebugEnabled()) {
          logger.debug(getAgentIdentifier() + ".addConsumerPG for MilitaryPerson: " + anAsset);
        }

        NewSubsistenceConsumerPG foodpg = (NewSubsistenceConsumerPG) anAsset.searchForPropertyGroup(
            SubsistenceConsumerPG.class);

        // If have not added the PG yet, add it
        if (foodpg == null) {
          if (logger.isDebugEnabled()) {
            logger.debug(getAgentIdentifier() + ".addConsumerPG CREATING SubConsumerPG for " + anAsset);
          }
          foodpg =
              (NewSubsistenceConsumerPG) getLDM().getFactory().createPropertyGroup(SubsistenceConsumerPG.class);
          foodpg.setMei(a);
          foodpg.setService(service);
          foodpg.setTheater(THEATER);
          foodpg.setSubsistenceConsumerBG(new SubsistenceConsumerBG(foodpg));
          foodpg.initialize(this);
          anAsset.setPropertyGroup(foodpg);
          publishChange(a);
        } else if (didRehydrate()) {
          if (logger.isDebugEnabled()) {
            logger.debug(getAgentIdentifier() + ".addConsumerPG on rehydrate - reinitializing PG for " + anAsset);
          }
          // Otherwise, if it is there, and we rehydrated, then all the slots are filled in, but
          // we must re-initialize (so the BG has a parentPlugin reference, etc)
          foodpg.initialize(this);
        } // end of didRehydrate
      } // end of check for MilitaryPerson
    } // end of loop over eaters
  }

  // Associating a property group to the person asset
  public void fillProperties(Asset anAsset) {
  }
}



