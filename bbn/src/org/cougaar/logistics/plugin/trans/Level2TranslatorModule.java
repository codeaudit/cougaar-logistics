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

package org.cougaar.logistics.plugin.trans;

import org.cougaar.util.log.Logger;
import org.cougaar.logistics.plugin.inventory.TaskUtils;
import org.cougaar.logistics.plugin.inventory.TimeUtils;
import org.cougaar.logistics.plugin.inventory.AssetUtils;

import org.cougaar.planning.ldm.PlanningFactory;

/** 
 * <pre>
 * A utility superclass for all the sub-modules of the Level2TranslatorPlugin
 *
 * This superclass of all the components of the Level2TranslatorPlugin contains
 * utility functions attained from the back pointer to the Level2TranslatorPlugin.
 * Common utility functionality to all the components such as blackboard
 * activity go here.
 *
 *
 **/

public class Level2TranslatorModule {

    protected transient Logger logger;
    protected transient Level2TranslatorPlugin translatorPlugin;

    public Level2TranslatorModule(Level2TranslatorPlugin level2Translator) {
	this.translatorPlugin = level2Translator;
	logger = translatorPlugin.getLoggingService(this);
    }

    public TaskUtils    getTaskUtils() {return translatorPlugin.getTaskUtils();}
    public TimeUtils    getTimeUtils() {return translatorPlugin.getTimeUtils();}
    public AssetUtils   getAssetUtils(){return translatorPlugin.getAssetUtils();}

    public PlanningFactory getPlanningFactory() {
	return translatorPlugin.getPlanningFactory();
    }

}
    
  
  
