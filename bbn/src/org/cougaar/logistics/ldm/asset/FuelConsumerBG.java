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

package org.cougaar.logistics.ldm.asset;

import org.cougaar.planning.ldm.asset.PGDelegate;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.glm.ldm.plan.Service;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;


public abstract class FuelConsumerBG extends ConsumerBG {

  String typeId;
  Schedule consumerSchedule = null, 
    orgActivitySchedule = null,
    mergedSchedule = null;
  Service service;
  String theater;

  public FuelConsumerBG(String typeId, Service service, String theater) {
    this.typeId = typeId;
    this.service = service;
    this.theater = theater;
  }

  public List getPredicates() {
    ArrayList predList = new ArrayList();
    predList.add(new ConsumerPredicate(typeId));
    predList.add(new OrgActivityPred());
    return predList;
  }

  public Schedule getParameterSchedule(Collection col) {
    return null;
  }

  public Rate getRate(Asset asset, List params) {
    return null;
  }

  public Collection getConsumed() {
    return null;
  }

  public PGDelegate copy(PGDelegate del) {
    return null;
  }
}


