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

public class DGClass9Scheduler extends DemandGeneratorModule {

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


  public Collection filterProjectionsToMaxSpareParts(Collection projections) {
    //get the first projection and see if it contains a class 9 PG on it - if it do use the PG
    //to filter the top n NSNs
    return projections;
  }

  protected Collection getConsumedParts(PropertyGroup pg) {
    return new ArrayList();
  }

}

