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
    
  
  
