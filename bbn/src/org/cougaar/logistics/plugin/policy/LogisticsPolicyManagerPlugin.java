/*
 * <copyright>
 *  Copyright 1997-2003 SRA International
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
 *
 */

package org.cougaar.logistics.plugin.policy;

import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.mlm.plugin.ldm.*;
import org.cougaar.planning.ldm.policy.Policy;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.core.util.UID;
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.glm.ldm.oplan.TimeSpan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.plan.RelationshipImpl;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.UIDService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.planning.service.LDMService;
import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.CommunityService;

/**
 * An instance of an LDMPlugin that reads a Cluster's startup policy
 * from an XML file.
 *
 * This Plugin is invoked with zero or more parameters. Parameters should be
 * listed in pairs, the policy to be read in and the role to which it should
 * be propagated. The files are found using the cluster's ConfigFinder.
 * This plugin also handles LogisticsPolicies which are propagated to it.
 *  Example from a sample cluster.ini file:
 * <PRE>
 * plugin=org.cougaar.logistics.plugin.policy.LogisticsPolicyManagerPlugin(POLICY=policy.ldm.xml,DESTINATION=Subordinate )
 * </PRE>
 *
 * @author   SRA
 *
 */
public class LogisticsPolicyManagerPlugin extends ComponentPlugin
{
  private boolean debugOn = false;

  private XMLPolicyCreator policyCreator;
    //private Properties globalParameters = new Properties();
  private String xmlfilename;
  protected Organization myOrganization_ = null;
  private IncrementalSubscription selfOrganizations_;
  private IncrementalSubscription allOrganizations_;
  private IncrementalSubscription allPolicies_;
  protected boolean policyRead = false;
  protected String myOrgName_;
  private PlanningFactory theLDMF = null;
  private UIDService uidService = null;
  private CommunityService comserv = null;
  private LoggingService logger;

/**
 * constructor
 */
  public LogisticsPolicyManagerPlugin() {
    super();
    //globalParameters.put( "XMLFile", xmlfilename );
  }

  IncrementalSubscription sub;
  IncrementalSubscription comSub;

  private static UnaryPredicate CommunityPolicyPredicate_ = new UnaryPredicate() {

    public boolean execute(Object o) {
      if (o instanceof LogisticsPolicy){
        LogisticsPolicy lp = (LogisticsPolicy) o;
        if (lp.getRole().equals("Subordinate") || (lp.getRole().equals("none"))){
          return false;
        } else return true;
      }
      return false;
    }
  };

  private static UnaryPredicate SubordinatePolicyPredicate_ = new UnaryPredicate() {

    public boolean execute(Object o) {
      if (o instanceof LogisticsPolicy){
        LogisticsPolicy lp = (LogisticsPolicy) o;
        if (lp.getRole().equals("Subordinate") || (lp.getRole().equals("none"))){
          return true;
        }
      }
      return false;
    }
  };


  /**
   * Predicate that looks for host Organization
   */

  private static UnaryPredicate myOrgsPredicate_= new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        Organization org = (Organization) o;
        boolean self = org.isSelf();
        return self;
      }
      return false;
    }
  };

  /**
   * Predicate to look for other Organizations
   */

  private static UnaryPredicate allOrgsPredicate_= new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Organization) {
        Organization org = (Organization) o;
        if (org.isSelf()) {
          return false;
        }else{
          return true;
        }
      }
      return false;
    }
  };

  private static UnaryPredicate policiesPredicate_ = new UnaryPredicate() {
    public boolean execute(Object o) {
      boolean isInstance = false;
      if (o instanceof Policy) {
        isInstance =  true;
      }
      return isInstance;
    }

  };

  protected void setupSubscriptions() {
    DomainService dService = null;
    if (theLDMF == null) {
      dService = (DomainService) getBindingSite().getServiceBroker().getService(this, org.cougaar.core.service.DomainService.class,
                                                                                new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {
          theLDMF = null;
        }
      });
    }
    if (uidService == null) {
      uidService = (UIDService) getBindingSite().getServiceBroker().getService(this, org.cougaar.core.service.UIDService.class, null);
    }
    //use the service
    theLDMF = (PlanningFactory) dService.getFactory("planning");
    selfOrganizations_ = (IncrementalSubscription)getBlackboardService().subscribe( myOrgsPredicate_);
    allOrganizations_ = (IncrementalSubscription)getBlackboardService().subscribe( allOrgsPredicate_);
    allPolicies_ = (IncrementalSubscription)getBlackboardService().subscribe( policiesPredicate_);
    getBlackboardService().getSubscriber().setShouldBePersisted(false);

    sub = (IncrementalSubscription)getBlackboardService().subscribe(SubordinatePolicyPredicate_);

    comSub = (IncrementalSubscription)getBlackboardService().subscribe(CommunityPolicyPredicate_);

    if (comserv == null) {
      comserv = (CommunityService) getBindingSite().getServiceBroker().getService(this, org.cougaar.core.service.community.CommunityService.class, new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {
          theLDMF = null;
        }
      });
    }

    if (getBlackboardService().didRehydrate()) {
      if (!selfOrganizations_.isEmpty()) {
        myOrganization_ = (Organization) selfOrganizations_.iterator().next();
        myOrgName_ = myOrganization_.getItemIdentificationPG().getItemIdentification();
      } else {
        logger.error(" Self organization subscription was empty on rehydration ");
      }
    }
  }

  /**
   * method to go through parameters of filenames and roles and create policies from xml files using XMLPolicyCreator
   */

  private void readInPolicies() {
    if (logger.isDebugEnabled()) {
      logger.debug("in readInPolicies");
    }
    try {

      Collection pv = getParameters();
      if ( pv != null ) {
        if (logger.isDebugEnabled()) {
          logger.debug("about to enter for loop vector size: " + pv.size());
        }
        // iterate through the list of XML Policy files to parse
        Vector policyVector = new Vector();
        Vector communityVector = new Vector();
        for (Iterator pi = pv.iterator(); pi.hasNext();) {
          xmlfilename = (String) pi.next();
          if (logger.isDebugEnabled()) {
            logger.debug("startsWith: " + xmlfilename.startsWith("POLICY="));
          }
          if (xmlfilename.startsWith("POLICY=")) {
            xmlfilename = xmlfilename.replaceFirst("POLICY=", "");
          }
          policyCreator = new XMLPolicyCreator(xmlfilename,
                                               getConfigFinder(),
                                               theLDMF);
          Policy policies[] = policyCreator.getPolicies();
          String role = (String) pi.next();
          if (role.startsWith("DESTINATION=")) {
            role = role.replaceFirst("DESTINATION=", "");
          }
          if (logger.isDebugEnabled()) {
            logger.debug("*******policies.length " + policies.length);
          }

          for (int i=0; i<policies.length; i++) {
            if (myOrganization_ == null && logger.isDebugEnabled()) {
              logger.debug("Org is null");
            }
            if (logger.isDebugEnabled()) {
              logger.debug("policy UID: " + policies[i].getUID());
            }
            boolean continu = true;
            for (Iterator t = allPolicies_.iterator(); t.hasNext();) {
              Policy compP = (Policy) t.next();
              if (compP.getName().equals(policies[i].getName())) {
                continu = false;
              }
            }
            if (continu == true) {
              LogisticsPolicy lp = new LogisticsPolicy(policies[i], null, Collections.EMPTY_SET, myOrgName_, role);
              if (role.equals("Subordinate") || role.equals("none")) {
                policyVector.add(lp);
              } else {
                communityVector.add(lp);
              }
            }

	  }
	}
        //	distributeCheckOrgs(policyVector.elements(), allOrganizations_.elements());
	distributeCheckPolicies(policyVector.elements());
	distributeCommunityPolicies(communityVector.elements());
      }
    } catch ( SubscriberException se ) {
      se.printStackTrace();
    }
    policyRead = true;
  }

  /**
   * responds to changes in subscriptions
   */
  public void execute() {

    if (selfOrganizations_.hasChanged()){
      if (myOrganization_ == null) {
        Enumeration new_orgs = selfOrganizations_.elements();
        if (logger.isDebugEnabled()) {
          logger.debug("selfOrganizations has changed");
        }
        if (new_orgs.hasMoreElements()) {
          myOrganization_ = (Organization) new_orgs.nextElement();
          myOrgName_ = myOrganization_.getItemIdentificationPG().getItemIdentification();
        }
        readInPolicies();
      }
    }

    if (sub.hasChanged()) {
      if (myOrganization_ != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("sub has changed and myOrg is defined");
        }
        distributeCheckPolicies(sub.getAddedList());
        distributeCheckPolicies(sub.getChangedList());
      }
    }

    if (allOrganizations_.hasChanged()) {
      if (myOrganization_ != null){
        distributeCheckOrgs(sub.elements(), allOrganizations_.getAddedList());
        distributeCheckOrgs(sub.elements(), allOrganizations_.getChangedList());

      }
    }

    if (comSub.hasChanged()) {
      communityPolicyPublisher(comSub.getAddedList());
    }
  }

  private void communityPolicyPublisher(Enumeration comPols) {
    while (comPols.hasMoreElements()) {
      LogisticsPolicy lp = (LogisticsPolicy) comPols.nextElement();
      Policy p = (Policy) lp.getPolicy();
      if (isConflict(lp) == false) {
        getBlackboardService().publishAdd(p);
      }
    }

  }

  private void distributeCommunityPolicies(Enumeration policies) {
    Collection communities = comserv.listAllCommunities();
    while (policies.hasMoreElements()){
      LogisticsPolicy lp = (LogisticsPolicy) policies.nextElement();
      for (Iterator i = communities.iterator(); i.hasNext();) {
        String comName = (String) i.next();
        lp.addTarget(AttributeBasedAddress.getAttributeBasedAddress(comName, "Role", lp.getRole()));
      }
      if (lp.getUID() == null) {
        lp.setUID(uidService.nextUID());
      }
      lp.setSource(myOrganization_.getMessageAddress());
      getBlackboardService().publishAdd(lp);
    }
  }

  /**
   * method to check changed or added policies for whether they a) need to be published and b) need to be propagated. calls distributeCheckOrgs if more than zero policies qualify to continue
   * @param policies Enumeration of LogisticsPolicies to be checked
   */
  private void distributeCheckPolicies(Enumeration policies) {
    boolean debugging = false;
    if (logger.isDebugEnabled()) {
      debugging = true;
    }
    if (debugging) logger.debug("in checkPolicies");
    Vector disPolicies = new Vector();
    if (debugging) logger.debug("in Check");
    while(policies.hasMoreElements()) {
      if (debugging) logger.debug("Check creating LP");
      LogisticsPolicy lp = (LogisticsPolicy) policies.nextElement();
      if (debugging) logger.debug("lp.getSource: " + lp.getSource() + " myOrg: " + myOrganization_.getMessageAddress());
      if (lp.getSource() != null) {
        if (debugging) logger.debug("so equals? " + lp.getSource().equals(myOrganization_.getMessageAddress()));
      } else {
        if (debugging) logger.debug("lp.getSource is null");
      }
      if (debugging) logger.debug("lp.targets " + lp.getTargets());
      if (lp.getSource() == null || !lp.getSource().equals(myOrganization_.getMessageAddress()) || (lp.getSource().equals(myOrganization_.getMessageAddress()) && (lp.getTargets() == null || lp.getTargets().isEmpty()))){
        if (debugging) logger.debug("role: " + lp.getRole() + " condition? " + (!lp.getRole().equals("none")));
        Policy p = lp.getPolicy();
        if (p.getUID() == null) {
          p.setUID(uidService.nextUID());
        }
        if (!lp.getRole().equals("none")){
          if (debugging) logger.debug("adding lp to disPolicies");
          disPolicies.add(lp);
        }
        if (isConflict(lp) == false) {
          getBlackboardService().publishAdd(p);
          if (debugging) logger.debug("published Policy " + p.getUID() + " at " + myOrgName_ );
        }
      }
    }
    if (debugging) logger.debug("validPolicies.size: " + disPolicies.size());
    if (disPolicies.size() != 0){
      if (debugging) logger.debug("sending to distribute from Check");
      distributeCheckOrgs(disPolicies.elements(), allOrganizations_.elements());

    }
  }

  /**
   * method to check an Enumeration of Organizations as targets for each policy
   * @param policies Enumeration of LogisticsPolicies to determine new targets for
   * @param targets Enumeration of Organizations to be checked as targets
   */

  private void distributeCheckOrgs(Enumeration policies, Enumeration targets){
    if (logger.isDebugEnabled()) {
      logger.debug("in checkOrgs");
    }
    Vector allTargets = new Vector();
    while (targets.hasMoreElements()){
      allTargets.add(targets.nextElement());
    }
    Vector goodTargets = new Vector();
    while ( policies.hasMoreElements() ) {
      LogisticsPolicy lp = (LogisticsPolicy) policies.nextElement();
      MutableTimeSpan mts = new MutableTimeSpan ();
      Enumeration t = allTargets.elements();
      while (t.hasMoreElements()){
        Organization org = (Organization) t.nextElement();
        Collection sups = org.getSuperiors(mts.getStartTime(), mts.getEndTime()); //assume subordinates are what we are matching for now
        if (logger.isDebugEnabled()) {
          logger.debug("Test: " + org.getMessageAddress() + " sups: " + sups.toString());
        }
        for(Iterator i = sups.iterator(); i.hasNext();){
          RelationshipImpl ri = (RelationshipImpl) i.next();
          Organization oa = (Organization) ri.getA();
          Organization ob = (Organization) ri.getB();

          if (oa.equals(myOrganization_) || ob.equals(myOrganization_)) {
            goodTargets.add(org);
          }
        }
      }
      distribute(lp, goodTargets.elements());
    }
  }

  /**
   * method to distribute one LogisticsPolicy to an Enumeration of targets
   * @param lp LogisticsPolicy to be distributed, by PublishAdd or PublishChange
   * @param targets Enumeration of Organizations from which MessageAddresses will be pulled to populated the LogisticsPolicy's targets
   */

  private void distribute(LogisticsPolicy lp, Enumeration targets) {
    Set mas = new HashSet();
    if (logger.isDebugEnabled()) {
      logger.debug("in distribute. target.hasMore: " + targets.hasMoreElements());
    }
    if (targets != null && targets.hasMoreElements()) {
      while (targets.hasMoreElements()) {
        Organization org = (Organization) targets.nextElement();
        MessageAddress ma = (MessageAddress) org.getMessageAddress();
        if (!lp.getTargets().contains(ma)){
          if (logger.isDebugEnabled()) {
            logger.debug("adding ma " + ma.toString());
          }
          mas.add(ma);
        }
      }
    } else {
      mas = Collections.EMPTY_SET;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("lp.getSource: " + lp.getSource());
      if (lp.getSource() != null) logger.debug("1source equal myOrg? " + lp.getSource().equals(myOrganization_.getMessageAddress()));
      logger.debug("lp.second if statement: " + (lp.getSource() == null || !lp.getSource().equals(myOrganization_.getMessageAddress())));
    }
    if (!(lp.getSource() == null && (lp.getTargets() == null || lp.getTargets().isEmpty()) && lp.getAuthority() == myOrgName_)) {
      if (lp.getUID() == null) {
        UID uid = uidService.nextUID();
        lp.setUID(uid);
      }
      if (!((mas == null) || mas.isEmpty() || mas.equals(lp.getTargets()))){
        lp.setTargets(mas);
        lp.setSource(myOrganization_.getMessageAddress());
        getBlackboardService().publishChange(lp);
        if (logger.isDebugEnabled()) {
          logger.debug("publishChange: " + lp.getUID().toString() + " at " + myOrgName_ +
                       " targets: " + lp.getTargets().toString());
        }
      }
    } else {
      LogisticsPolicy newLP = new LogisticsPolicy(lp.getPolicy(),myOrganization_.getMessageAddress(), mas, lp.getAuthority(), lp.getRole());
      UID uid = uidService.nextUID();
      newLP.setUID(uid);
      if (isConflict(lp) == false) {
        getBlackboardService().publishAdd(newLP);
        if (logger.isDebugEnabled()) {
          logger.debug("publishAdd: " + newLP.getUID().toString() + " at " + myOrgName_ +
                       " targets: " + newLP.getTargets().toString());
        }
      }
    }
  }


  private boolean isConflict(LogisticsPolicy lp) {
    Policy p = lp.getPolicy();
    Enumeration pols = allPolicies_.elements();
    boolean match = false;
    Policy matchPol = null;
    //see if the type of policy in question has been published already
    while(pols.hasMoreElements()){
      Policy pol = (Policy) pols.nextElement();
      if (pol.getName().equals(p.getName()) && pol != p){
        match = true;
        matchPol = pol;
      }
    }
    if (match == true){
      boolean eq = false;
      LogisticsPolicy matchLP = null;
      //if so, check to see if the policy has a corresponding LogisticsPolicy
      //if not, the policy was fed in at the agent itself and that agent becomes the authority
      Enumeration lps = sub.elements();
      while (lps.hasMoreElements()){
        LogisticsPolicy curLP = (LogisticsPolicy) lps.nextElement();
        if (curLP.getPolicy().equals(matchPol)){
          eq = true;
          matchLP = curLP;
        }
      }
      if (eq == false){
        Enumeration comlps = comSub.elements();
        while (comlps.hasMoreElements()){
          LogisticsPolicy curLP = (LogisticsPolicy) comlps.nextElement();
          if (curLP.getPolicy().equals(matchPol)){
            eq = true;
            matchLP = curLP;
          }
        }
      }
      if (matchLP != null && matchLP.getPolicy().equals(lp.getPolicy())) eq = false;
      //get instance of Organization that is source of each Policy
      if (eq == true){
        //conflict -- if true, remove matchLP
        if (matchLP.getAuthority().equals(lp.getAuthority())){
          getBlackboardService().publishRemove(matchLP);
          getBlackboardService().publishRemove(matchPol);
          return false;
        } else if ((matchLP.getSource() != null && lp.getSource() != null) && matchLP.getSource().equals(lp.getSource())){
          getBlackboardService().publishRemove(matchLP);
          getBlackboardService().publishRemove(matchPol);
          return false;
        } else {
          Enumeration orgs = allOrganizations_.elements();
          Organization newOrg = null;
          Organization oldOrg = null;
          while (orgs.hasMoreElements()){
            Organization o = (Organization) orgs.nextElement();
            if (lp.getSource() == null) {
              newOrg = myOrganization_;
            }else if (lp.getSource().toString().equals(o.getItemIdentificationPG().getItemIdentification())){
              newOrg = o;
            }
            if (matchLP.getSource().toString().equals(o.getItemIdentificationPG().getItemIdentification())){
              oldOrg = o;
            }

          }
          if (oldOrg == null && !(matchLP.getSource().toString().equals(myOrgName_) && newOrg == null)){
            if (matchLP != null && (!matchLP.getRole().equals("Subordinate") || (matchLP.getRole().equals("Subordinate") && lp.getRole().equals("Subordinate")))) {
              //					if (matchLP != null && !matchLP.getRole().equals("Subordinate")){
              getBlackboardService().publishRemove(matchLP);
            }
            if (matchPol != null) {
              getBlackboardService().publishRemove(matchPol);
            }
            return false;
          } else if (newOrg != null) {
            MutableTimeSpan mts = new MutableTimeSpan ();
            Collection sups = myOrganization_.getSuperiors(mts.getStartTime(), mts.getEndTime());
            /***********/
            boolean containsOldOrg = false;
            for(Iterator i = sups.iterator(); i.hasNext();){
              RelationshipImpl ri = (RelationshipImpl) i.next();
              Organization oa = (Organization) ri.getA();
              Organization ob = (Organization) ri.getB();
              if (oa.equals(newOrg) || ob.equals(newOrg)) {
                getBlackboardService().publishRemove(matchLP);
                getBlackboardService().publishRemove(matchPol);
                return false;
              } else if (oa.equals(oldOrg) || ob.equals(oldOrg)) {
                containsOldOrg = true;
              }
            }
            if (containsOldOrg == false) {
              getBlackboardService().publishRemove(matchLP);
              getBlackboardService().publishRemove(matchPol);
              return false;
            }
          }
        }
        //if there is no LP match for previous Policy, that means Policy was injected locally
        //so check new LP to see if the source is the Organization's superior
      } else {
        Enumeration orgs = allOrganizations_.elements();
        Organization newOrg = null;
        while (orgs.hasMoreElements()){
          Organization o = (Organization) orgs.nextElement();
          if (lp.getSource() != null && lp.getSource().toString().equals(o.getItemIdentificationPG().getItemIdentification())){
            newOrg = o;
          }
        }
        if (newOrg != null) {
          MutableTimeSpan mts = new MutableTimeSpan ();
          Collection sups = myOrganization_.getSuperiors(mts.getStartTime(), mts.getEndTime());
          for(Iterator i = sups.iterator(); i.hasNext();){
            RelationshipImpl ri = (RelationshipImpl) i.next();
            Organization oa = (Organization) ri.getA();
            Organization ob = (Organization) ri.getB();
            if (oa.equals(newOrg) || ob.equals(newOrg)) {
              getBlackboardService().publishRemove(matchPol);
              return false;
            }
          }
        }
      }
    }else {
      return false;
    }
    //check this
    if (lp != null && lp.getSource() != null && !lp.getSource().toString().equals(myOrgName_)) {

      //needs to be a check here
      getBlackboardService().publishRemove(lp);
    }
    return true;

  }

  /**
   *  Calls ComponentPlugin's load(), creates the Logging Service.
   **/
  public void load() {
    super.load();
    logger = getLoggingService(this);
  } // load

  public LoggingService getLoggingService(Object requestor) {
    return (LoggingService)
      getServiceBroker().getService(requestor,
				    LoggingService.class,
				    null);
  }

}
