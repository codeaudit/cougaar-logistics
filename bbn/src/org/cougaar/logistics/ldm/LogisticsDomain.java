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

import java.util.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;

import org.cougaar.core.domain.*;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.domain.DomainAdapter;

import org.cougaar.logistics.ldm.asset.PropertyGroupFactory;
import org.cougaar.logistics.ldm.asset.AssetFactory;

import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.service.LDMService;


/**
 * COUGAAR Domain package definition.
 **/

public class LogisticsDomain extends DomainAdapter {

  public static final String LOGISTICS_NAME = "logistics";

  private MessageAddress self;
  private AgentIdentificationService agentIdService;
  private DomainService domainService;
  private LDMService ldmService;

  public String getDomainName() {
    return LOGISTICS_NAME;
  }

  public LogisticsDomain() {
    super();
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais == null) {
      // Revocation
    } else {
      this.self = ais.getMessageAddress();
    }
  }

  public void setDomainService(DomainService domainService) {
    this.domainService = domainService;
  }

  public void setLDMService(LDMService ldmService) {
    this.ldmService = ldmService;
  }

  public void initialize() {
    super.initialize();
    Constants.Role.init();    // Insure that our Role constants are initted
  }

  public void unload() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (ldmService != null) {
      sb.releaseService(
          this, LDMService.class, ldmService);
      ldmService = null;
    }
    if (domainService != null) {
      sb.releaseService(
          this, DomainService.class, domainService);
      domainService = null;
    }
    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    super.unload();
  }

  public Collection getAliases() {
    ArrayList l = new ArrayList(3);
    l.add("logistics");
    l.add("albbn");
    return l;
  }

  protected void loadFactory() {
    LDMServesPlugin ldm = ldmService.getLDM();
    PlanningFactory ldmf = (PlanningFactory) ldm.getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException("Missing \"planning\" factory!");
    }

    ldmf.addPropertyGroupFactory(new PropertyGroupFactory());
    ldmf.addAssetFactory(new AssetFactory());
  }

  protected void loadXPlan() {
    // no logistics-specific plan
  }

  protected void loadLPs() {
    RootPlan rootplan = (RootPlan) getXPlanForDomain("root");
    if (rootplan == null) {
      throw new RuntimeException("Missing \"root\" plan!");
    }

    PlanningFactory ldmf = (PlanningFactory)
      domainService.getFactory("planning");
    if (ldmf == null) {
      throw new RuntimeException("Missing \"planning\" factory!");
    }

    /**
     * We have no new logistics LPs for the time being.
     *
    GLMFactory glmFactory = (GLMFactory)
      domainService.getFactory("glm");

    addLogicProvider(new ReceiveTransferableLP(rootplan, ldmf));
    addLogicProvider(new TransferableLP(rootplan, self, ldmf));
    addLogicProvider(new DetailRequestLP(rootplan, self, glmFactory));
    addLogicProvider(new OPlanWatcherLP(rootplan, ldmf));
    */

  }

}
