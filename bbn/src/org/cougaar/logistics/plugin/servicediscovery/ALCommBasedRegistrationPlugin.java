/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

import org.cougaar.servicediscovery.plugin.SDCommunityBasedRegistrationPlugin;
import org.cougaar.servicediscovery.description.StatusChangeMessage;
import org.cougaar.logistics.plugin.utils.ALStatusChangeMessage;
import java.util.Collection;
import java.util.Iterator;

public class ALCommBasedRegistrationPlugin extends SDCommunityBasedRegistrationPlugin {

  protected void handleStatusChange() {
    Collection adds = statusChangeSubscription.getAddedCollection();
    for (Iterator iterator = adds.iterator(); iterator.hasNext();) {
      final StatusChangeMessage statusChange = (StatusChangeMessage) iterator.next();

      synchronized (statusChange) {
	if(statusChange instanceof ALStatusChangeMessage) {
	  statusChange.setStatus(StatusChangeMessage.COMPLETED);
	  statusChange.setRegistryUpdated(true);
	  getBlackboardService().publishChange(statusChange);
	} else {
	  super.handleStatusChange();
	}
      } // end of synchronized
    } // end of for loop
  }
}
