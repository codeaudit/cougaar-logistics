/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

import java.util.Date;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.measure.Distance;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.GLMAsset;

import org.cougaar.lib.util.UTILPreference;
import org.cougaar.lib.util.UTILPrepPhrase;

import org.cougaar.logistics.plugin.trans.GLMTransConst;
import org.cougaar.util.log.Logger;

/**
 * Create XML document in the Vishnu Data format, directly from ALP objects.
 * <p>
 * Create and return xml for log plan objects.
 * <p>
 */

public class SeaDataXMLize extends GenericDataXMLize {
  public static final String TRUE  = "true";
  public static final String FALSE = "false";

  public SeaDataXMLize (boolean direct, Logger logger) {
    super (direct, logger);
    prefHelper = new UTILPreference (logger);
    prepHelper = new UTILPrepPhrase (logger);
  }

  /** 
   * Create XML for asset, subclass to add fields
   * 
   * @param object node representing asset
   * @param taskOrAsset asset being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processAsset (Object object, Object taskOrAsset) {
    if (!super.processAsset(object, taskOrAsset))
      return false;

    GLMAsset asset = (GLMAsset) taskOrAsset;
	
    dataHelper.createField(object, "Asset", "containerCapacity", "" + getContainerCapacity   (asset));
    dataHelper.createField(object, "Asset", "isAmmoShip", isAmmoShip (asset) ? TRUE : FALSE);

    return true;
  }

  /** 
   * Create XML for task, subclass to add fields
   * 
   * @param object node representing task
   * @param taskOrAsset task being translated
   * @return true if should add object to list of new objects
   */
  protected boolean processTask (Object object, Object taskOrAsset) {
    super.processTask (object, taskOrAsset);
    Task task = (Task) taskOrAsset;
    Asset directObject = task.getDirectObject();
    GLMAsset baseAsset;

    if (directObject instanceof AggregateAsset) {
      baseAsset = (GLMAsset) ((AggregateAsset)directObject).getAsset ();
    } 
    else {
      baseAsset = (GLMAsset)directObject;
    }
	
    boolean isContainer = isContainer (baseAsset);
    dataHelper.createField(object, "Transport", "isContainer", isContainer (baseAsset) ? TRUE : FALSE);
    // dataHelper.createField(object, "Transport", "isAmmo", (isContainer ? (isAmmo (baseAsset) ? TRUE : FALSE) : FALSE));

    Date earliestArrival = new Date(prefHelper.getEarlyDate(task).getTime());
    dataHelper.createDateField(object, "earliestArrival", earliestArrival);
    Date latestArrival = new Date(prefHelper.getLateDate(task).getTime());
    dataHelper.createDateField(object, "latestArrival", latestArrival);

    return true;
  }

  /** don't send the isPerson field */
  protected void addTaskPersonField (Object object, GLMAsset baseAsset) {}
  /** don't send the passengerCapacity field */
  protected void addPassengerCapacity (Object object, GLMAsset asset) {}

  protected boolean isContainer (GLMAsset asset) {
    LowFidelityAssetPG currentLowFiAssetPG = (LowFidelityAssetPG)
      asset.resolvePG (LowFidelityAssetPG.class);
    
    if (currentLowFiAssetPG != null) {
      return currentLowFiAssetPG.getCCCDim().getIsContainer();
    }
    else {
      return asset instanceof Container;
    }
  }

  /** 
   * An asset is an ammo container if it has a contents pg, since
   * only the Ammo Packer put a contents pg on a container.
   *
   * NOTE : should call isContainer first!
   */
  protected boolean isAmmo (GLMAsset asset) {
    return asset.hasContentsPG ();
  }

  protected double getContainerCapacity (GLMAsset asset) {
    return (asset.hasContainPG()) ?
      asset.getContainPG().getMaximumContainers() : 0.0d;
  }

  protected boolean isAmmoShip (GLMAsset asset) {
    String typeid = asset.getTypeIdentificationPG().getNomenclature ();
    return typeid.equals ("Ammo");
  }

  protected UTILPreference prefHelper;
  protected UTILPrepPhrase prepHelper;
}
