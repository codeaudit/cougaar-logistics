/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA)
 *  and the Defense Logistics Agency (DLA).
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
 */

public class ALProviderPlugin extends SDProviderPlugin {

  protected void setupSubscriptions() {
    super.setupSubscriptions();
  }
}
