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

import java.util.*;

import org.cougaar.lib.vishnu.client.XMLizer;
import org.cougaar.lib.vishnu.client.custom.CustomVishnuAggregatorPlugin;

import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.TransportationRoute;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.GLMPrepPhrase;

/**
 * This class is necessary because the ShipPacker needs a DataXMLizer that <br>
 * adds information specific to the ship packing problem.
 */
public class SeaVishnuPlugin extends GenericVishnuPlugin {
  public void localSetup () {
    super.localSetup ();

    glmPrepHelper = new GLMPrepPhrase (logger);
  }

  /** 
   * override to use a different XMLizer <p>
   *
   * SeaDataXMLize adds fields for sea-specific info, like dealing with Ammunition
   */
  protected XMLizer createXMLizer (boolean direct) {
    GenericDataXMLize xmlizer = new SeaDataXMLize (direct, logger);
    setDataXMLizer(xmlizer);
    return xmlizer;
  }

  protected Task createMainTask (Task task, Asset asset, Date start, Date end, Date setupStart, Date wrapupEnd) {
    Task mainTask = super.createMainTask (task, asset, start, end, setupStart, wrapupEnd);
    
    // attach route
    TransportationRoute route = (TransportationRoute)
      glmPrepHelper.getIndirectObject(task, GLMTransConst.SEAROUTE);

    glmPrepHelper.addPrepToTask(mainTask, 
				glmPrepHelper.makePrepositionalPhrase(ldmf,
								      Constants.Preposition.VIA,
								      route));
    return mainTask;
  }

  protected GLMPrepPhrase glmPrepHelper;
}
