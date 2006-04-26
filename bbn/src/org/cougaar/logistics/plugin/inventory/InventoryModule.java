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

package org.cougaar.logistics.plugin.inventory;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.core.component.Component;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.util.log.Logger;

/** 
 * <pre>
 * A utility superclass for all the sub-modules of the Inventory Plugin
 *
 * This superclass of all the components of the InventoryPlugin contains
 * utility functions attained from the back pointer to the InventoryPlugin.
 * Common utility functionality to all the components such as blackboard
 * activity go here.
 *
 *
 **/

public class InventoryModule {

    protected transient Logger logger;
  protected transient InventoryManager inventoryPlugin;

    public InventoryModule(InventoryManager imPlugin) {
      this.inventoryPlugin = imPlugin;
	logger = (Logger)inventoryPlugin.getLoggingService(this);
    }

    public TaskUtils    getTaskUtils() {return inventoryPlugin.getTaskUtils();}
    public TimeUtils    getTimeUtils() {return inventoryPlugin.getTimeUtils();}
    public AssetUtils   getAssetUtils(){return inventoryPlugin.getAssetUtils();}


}
    
  
  
