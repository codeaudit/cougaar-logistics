package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.servicediscovery.SDFactory;
import org.cougaar.servicediscovery.description.ServiceContract;
import org.cougaar.servicediscovery.transaction.ServiceContractRelay;
import org.cougaar.servicediscovery.description.ServiceRequest;
import org.cougaar.servicediscovery.description.StatusChangeMessage;
import org.cougaar.servicediscovery.plugin.SDProviderPlugin;

import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AspectType;

import org.cougaar.core.service.LoggingService;

import org.cougaar.planning.ldm.plan.Preference;

import org.cougaar.logistics.plugin.utils.ALStatusChangeMessage;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

/**
 * <p>Title: </p>
 * <p>Description:
 * This plugin replies to new service contract relays. Usually,
 * it will reply with a service contract which exactly matches the
 * service request. "Just say yes"
 * Responds to StatusChangeMessages by revoking the service contracts
 * with matching roles.
 * Responds to ALStatusChangeMessages.
 * Upon receiving this kind of message, the plugin publishes a change to
 * any affected ServiceContractRelays, with updated ServiceContract end dates.
 * Also, the plugin will now reply to any new service contract relay with
 * a service contract with start time == end time (a zero time contract)
 * as a way to "Just say no".
 * </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class ALProviderPlugin extends SDProviderPlugin {

  private LoggingService myLoggingService;
  private boolean sayYes;


  protected void setupSubscriptions() {
    sayYes = true;
    myLoggingService =
      (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    super.setupSubscriptions();
  }

  protected void handleAddedServiceContractRelay(ServiceContractRelay relay){
    if(sayYes) {
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug("ALProviderPlugin pass handleAddedServiceContractRelay to super");
      }
      super.handleAddedServiceContractRelay(relay);
    }
    //say no by making the end time preference  == the start time preferend
    else {
      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug("ALProviderPlugin handleAddedServiceContractRelay");
      }
      ServiceRequest serviceRequest = relay.getServiceRequest();

      ArrayList contractPreferences =
        new ArrayList(serviceRequest.getServicePreferences().size());
      Preference startPref = null;
      for (Iterator iterator = serviceRequest.getServicePreferences().iterator();
           iterator.hasNext();) {
        Preference requestPreference = (Preference) iterator.next();
        //remember the start time pref
        if(requestPreference.getAspectType() == Preference.START_TIME) {
            startPref = requestPreference;
        }
        //copy all prefs which are not the end time pref
        if(requestPreference.getAspectType() != Preference.END_TIME) {
          Preference contractPreference =
              getFactory().newPreference(requestPreference.getAspectType(),
              requestPreference.getScoringFunction(),
              requestPreference.getWeight());
          contractPreferences.add(contractPreference);
        }
      }
      //now put in an end time pref that is the same as the start time
      if(startPref != null) {
        Preference contractPreference =
            getFactory().newPreference(Preference.END_TIME,
            startPref.getScoringFunction(),
            startPref.getWeight());
        contractPreferences.add(contractPreference);
      }
    }
  }


  protected void changeServiceContractRelay(ServiceContractRelay contractRelay, StatusChangeMessage m) {
    if(m instanceof ALStatusChangeMessage) {
      ALStatusChangeMessage message = (ALStatusChangeMessage)m;
      contractRelay.setServiceContract(getAlteredServiceContract(contractRelay, message));
      sayYes = false;
      publishChange(contractRelay);

      if (myLoggingService.isDebugEnabled()) {
        myLoggingService.debug("ALProviderPlugin found publishChange contract relay"+
                               " provider " + contractRelay.getProviderName() +
                               " role "+contractRelay.getServiceContract().getServiceRole().getName());
      }
    }
    else {
      super.changeServiceContractRelay(contractRelay, m);
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

    ServiceContract newSc = mySDFactory.newServiceContract(getSelfOrg(),
        sc.getServiceRole(), newPreferences);

    return newSc;
  }

}
