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

package org.cougaar.logistics.plugin.demand;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.TimeSpan;

import java.util.Collection;


/**
 * The GenProjExpanderIfc dictates the functionality needed by the
 * {@link DemandForecastPlugin}.  The Expander should extend the
 * DemandForecastModule for convenience and call super(DemandForcastPlugin).
 * The constructor of the Expander will take a single argument, a
 * reference to the plugin.
 **/

public interface GenProjExpanderIfc {

  /** 
   * Expand the passed in GenerateProjectins task into the requisite ProjectSupply
   * tasks - one for each resource need of this MEI/Asset determined by the 
   * BG associated with the passed in supplyPGClass.
   **/
  void expandGenerateProjections(Task gpTask, Schedule schedule, Asset asset, TimeSpan timespan);

  /** 
   * Reflect new received results in the estimated results slot so that
   * AllocationResult notifications flow back to the root.
   * @param planElements - Collection of PlanElements to check for updates.
   **/
  void updateAllocationResults(Collection planElements);
}

