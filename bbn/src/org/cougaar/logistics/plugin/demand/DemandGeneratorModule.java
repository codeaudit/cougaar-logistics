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

import org.cougaar.util.log.Logger;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.AssetUtils;

import org.cougaar.planning.ldm.PlanningFactory;

/** 
 * <pre>
 * A utility superclass for all the sub-modules of the DemandGeneratorPlugin
 *
 * This superclass of all the components of the DemandGeneratorPlugin contains
 * utility functions attained from the back pointer to the DemandGeneratorPlugin.
 * Common utility functionality to all the components such as blackboard
 * activity go here.
 *
 *
 **/

public class DemandGeneratorModule {

    protected transient Logger logger;
    protected transient DemandGeneratorPlugin dgPlugin;

    public DemandGeneratorModule(DemandGeneratorPlugin demandGeneratorPlugin) {
	this.dgPlugin = demandGeneratorPlugin;
	logger = dgPlugin.getLoggingService(this);
    }

    public TaskUtils    getTaskUtils() {return dgPlugin.getTaskUtils();}
    public TimeUtils    getTimeUtils() {return dgPlugin.getTimeUtils();}
    public AssetUtils   getAssetUtils(){return dgPlugin.getAssetUtils();}

    public PlanningFactory getPlanningFactory() {
	return dgPlugin.getPlanningFactory();
    }

}
    
  
  
