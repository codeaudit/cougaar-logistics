package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.SDDomain;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;

import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AspectType;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.blackboard.IncrementalSubscription;
//import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.ldm.plan.Preference;

import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.planning.plugin.legacy.SimplePlugin;

import org.cougaar.logistics.plugin.utils.ALStatusChangeMessage;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

/**
 * <p>Title: </p>
 * <p>Description:
 * This plugin is used by AL providers to respond to ALStatusChangeMessages.
 * Upon receiving this kind of message, the plugin publishes a change to
 * any affected ServiceContractRelays, with updated ServiceContract end dates.
 * </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class ALProviderPlugin extends SimplePlugin {

  private IncrementalSubscription myStatusChangeSubscription;
  private IncrementalSubscription myServiceContractRelaySubscription;
  private IncrementalSubscription mySelfOrgSubscription;

  private String myAgentName;

  private SDFactory mySDFactory;

  private UnaryPredicate mySelfOrgPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        Organization org = (Organization) o;
        if (org.isLocal()) {
          return true;
        }
      }
      return false;
    }
  };

  private UnaryPredicate myServiceContractRelayPred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof ServiceContractRelay) {
        ServiceContractRelay relay = (ServiceContractRelay) o;
        return (relay.getProviderName().equals(myAgentName));
      } else {
        return false;
      }
    }
  };

  private UnaryPredicate statusChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ALStatusChangeMessage);
    }
  };

  protected void setupSubscriptions() {
    myStatusChangeSubscription =
      (IncrementalSubscription) subscribe(statusChangePredicate);
    myServiceContractRelaySubscription = (IncrementalSubscription) subscribe(myServiceContractRelayPred);
    mySelfOrgSubscription = (IncrementalSubscription) subscribe(mySelfOrgPred);

    mySDFactory = (SDFactory) getFactory(SDDomain.SD_NAME);

    myAgentName = getBindingSite().getAgentIdentifier().toString();
  }

  protected void execute () {
    if(myStatusChangeSubscription.hasChanged()) {
      Iterator it = myStatusChangeSubscription.getChangedCollection().iterator();
      while(it.hasNext()) {
        ALStatusChangeMessage m = (ALStatusChangeMessage)it.next();

        System.out.println("ALProviderPlugin found ALStatusChangeMessage");

        //only proceed if the registry has already been updated to reflect service disruption
        if(m.registryUpdated()) {
          Iterator contracts = myServiceContractRelaySubscription.getCollection().iterator();
          while(contracts.hasNext()) {
            ServiceContractRelay contractRelay = (ServiceContractRelay)contracts.next();
            //find the service contract relay with matching role to the service disrupted
            if(contractRelay.getServiceRequest().getServiceRole().toString().equals(m.getRole())) {
              //alter the service contract
              contractRelay.setServiceContract(getAlteredServiceContract(contractRelay, m));
              publishChange(contractRelay);

              System.out.println("ALProviderPlugin found publishChange contract relay");
              System.out.println("provider " + contractRelay.getProviderName());
              System.out.println("role "+contractRelay.getServiceContract().getServiceRole().getName());
            }
          }
        }
      }
    }
  }

  private ScoringFunction changeDate(ScoringFunction oldScoringFunction, Date newEndDate) {
    AspectValue endTAV = TimeAspectValue.create(AspectType.END_TIME, newEndDate.getTime());
    ScoringFunction endScoreFunc =
      ScoringFunction.createStrictlyAtValue(endTAV);
    return endScoreFunc;
  }

  private ServiceContract getAlteredServiceContract(ServiceContractRelay relay,
      ALStatusChangeMessage m) {

    ServiceContract sc = relay.getServiceContract();
    Iterator oldPreferences = sc.getServicePreferences().iterator();
    ArrayList newPreferences = new ArrayList();
    while(oldPreferences.hasNext()) {
      Preference oldPref = (Preference) oldPreferences.next();
      Preference newPref;
      if(oldPref.getAspectType() == Preference.END_TIME) {
        newPref = getFactory().newPreference(oldPref.getAspectType(),
            changeDate(oldPref.getScoringFunction(), m.getEndDate()),
            oldPref.getWeight());
        newPreferences.add(newPref);
      }
      else {
        newPref = getFactory().newPreference(oldPref.getAspectType(),
            oldPref.getScoringFunction(),
            oldPref.getWeight());
        newPreferences.add(newPref);
      }
    }

    System.out.println("ALProviderPlugin old end time preference " +
                       new Date((long)SDFactory.getPreference(sc.getServicePreferences(),
                       Preference.END_TIME)));
    System.out.println("ALProviderPlugin old new time preference " +
                       new Date((long)SDFactory.getPreference(newPreferences,
                       Preference.END_TIME)));

    ServiceContract newSc = mySDFactory.newServiceContract(getSelfOrg(),
        sc.getServiceRole(), newPreferences);

    return newSc;
  }

  protected Organization getSelfOrg() {
    for (Iterator iterator = mySelfOrgSubscription.iterator();
         iterator.hasNext();) {
      return (Organization) iterator.next();
    }

    return null;
  }
}
