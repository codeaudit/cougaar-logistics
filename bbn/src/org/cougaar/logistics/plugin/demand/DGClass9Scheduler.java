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

import java.util.Collection;
import java.util.ArrayList;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.asset.PropertyGroup;


/**
 * The DFClass9Scheduler class aids in determining what percentage of
 * the maxSpareParts will be generated demand for based upon
 * the passed in policy.
 *
 * @see DemandGeneratorPlugin
 * @see DemandGeneratorModule
 **/

public class DGClass9Scheduler extends DemandGeneratorModule
{

    int maxPartsPolicy;
    int maxPartsLimit;

    public DGClass9Scheduler(DemandGeneratorPlugin demandGeneratorPlugin) {
	super(demandGeneratorPlugin);
    }

    public void newMaxPartsPolicy(int policy) {
	maxPartsPolicy = policy;
    }

    public void limitMaxNumberPartsTo(int maxParts) {
	maxPartsLimit = maxParts;
    }


    public Collection filterProjectionsToMaxSpareParts(PropertyGroup pg, Collection projections) {
	return projections;
    }

    protected Collection getConsumedParts(PropertyGroup pg) {
      return new ArrayList();
    }

}

