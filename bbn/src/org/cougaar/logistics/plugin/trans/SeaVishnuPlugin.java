/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.lib.vishnu.client.custom.CustomVishnuAggregatorPlugin;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationRoute;
import org.cougaar.glm.util.GLMPrepPhrase;
import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.glm.ldm.Constants;

import java.util.*;

import org.cougaar.logistics.plugin.trans.tools.RouteFinder;

/**
 * This class is necessary because the ShipPacker needs a DataXMLizer that <br>
 * adds information specific to the ship packing problem.
 */
public class SeaVishnuPlugin extends CustomVishnuAggregatorPlugin {
  RouteFinder routeFinder;

  public void localSetup () {
    super.localSetup ();

    createRouteFinder (); // allows subclass
    glmPrepHelper = new GLMPrepPhrase (logger);
  }

  protected void createRouteFinder () {
    routeFinder = new RouteFinder (logger);
    routeFinder.setFactory (ldmf);
  }

  /**
   * Implemented for UTILAssetListener
   * <p>
   * OVERRIDE to see which assets you think are interesting.
   * <p>
   * For instance, if you are scheduling trucks/ships/planes, 
   * you'd want to check like this : 
   * <code>
   * return (GLMAsset).hasContainPG ();
   * </code>
   * @param a asset to check 
   * @return boolean true if asset is interesting
   */
  public boolean interestingAsset(Asset a) {
    if (a instanceof GLMAsset) {
      return ((GLMAsset) a).hasContainPG();
    }
    return false;
  }

  /** 
   * Looks only at TRANSPORT tasks, must call super to not 
   * accidentally get one of the tasks from the final expansion 
   **/
  public boolean interestingTask (Task task) {
    boolean interesting = super.interestingTask (task);
    return interesting && task.getVerb().equals (Constants.Verb.TRANSPORT);
  }

  /** 
   * override to use a different XMLizer <p>
   *
   * SeaDataXMLize adds fields for sea-specific info, like dealing with Ammunition
   */
  protected XMLizer createXMLizer (boolean direct) {
    return new SeaDataXMLize (direct, logger);
  }

  protected Task createMainTask (Task task, Asset asset, Date start, Date end, Date setupStart, Date wrapupEnd) {
    Task mainTask = super.createMainTask (task, asset, start, end, setupStart, wrapupEnd);
    
    // attach route
    TransportationRoute route = routeFinder.getRoute (glmPrepHelper.getFromLocation(mainTask),
						      glmPrepHelper.getToLocation  (mainTask), 
						      false /* don't include destination */);

    glmPrepHelper.addPrepToTask(mainTask, 
				glmPrepHelper.makePrepositionalPhrase(ldmf,
								      Constants.Preposition.VIA,
								      route));
    return mainTask;
  }

  protected GLMPrepPhrase glmPrepHelper;
}
