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

import java.math.BigDecimal;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.domain.LDMServesClient;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.glm.ldm.Constants;

import org.cougaar.glm.ldm.asset.Container;
import org.cougaar.glm.ldm.asset.ContainPG;
import org.cougaar.glm.ldm.asset.ForUnitPG;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.glm.ldm.asset.NewForUnitPG;
import org.cougaar.glm.ldm.asset.NewPhysicalPG;
import org.cougaar.glm.ldm.asset.PhysicalPG;
import org.cougaar.glm.ldm.asset.PropertyGroupFactory;
import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.logistics.plugin.trans.GLMTransConst;

import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.lib.callback.UTILAssetCallback;
import org.cougaar.lib.callback.UTILAssetListener;

import org.cougaar.lib.filter.UTILExpanderPluginAdapter;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;

import org.cougaar.planning.ldm.measure.*;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewTask;

/**
 * Breaks up incoming tasks into aggregates that are no bigger than the largest carrier asset.
 * Carrier could be truck, ship, or plane.
 */
public class SeaTransportExpanderPlugin extends TransportExpanderPlugin {

  protected static final double CAP_FUDGE = 0.1;

  /** 
   * <pre>
   * expands aggregate assets into truck-sized chunks, putting the 
   * subtasks into <code>subtasks</code> 
   *
   * Rounds the item wt to the same precision as vishnu uses so no disaggreements
   * about how much a truck can hold.
   * </pre>
   **/
  protected void expandAggregates (Task task, Vector subtasks) {
    AggregateAsset directObject = (AggregateAsset) task.getDirectObject();
    GLMAsset itemProto = (GLMAsset) directObject.getAsset();
    ForUnitPG unitPG = getForUnitPG(directObject);

    boolean isTruck     = isVehicle(itemProto);
    boolean isContainer = isContainer(itemProto);

    double itemContrib;

    if (isTruck) 
      itemContrib = getItemAreaContribution(itemProto);
    else if (isContainer) 
      itemContrib = 1;
    else
      itemContrib = getItemVolumeContribution(itemProto);

    double maxCap = 
      ((isTruck) ? maxAreaCapacity : 
       (isContainer ? maxContainerCapacity : maxVolumeCapacity)) - CAP_FUDGE;
    // double maxCap = (isPerson) ? maxPassengerCapacity : maxContainCapacity - CAP_FUDGE; // fudge necessary?
    String unit = (isTruck) ? "m^2" : (isContainer ? "container" : "m^3");
    // round item weight to same precision as Vishnu
    BigDecimal bigD = new BigDecimal (itemContrib);
    itemContrib = bigD.setScale (2,BigDecimal.ROUND_UP).doubleValue();

    int remainingQuantity = getAssetQuantity(directObject);
    double totalAggregateContrib = ((double)remainingQuantity)*itemContrib;
	  
    boolean mustExpand = (totalAggregateContrib > maxCap);
	  
    if (isDebugEnabled()) {
      debug (".expandAggregates - item contribution " + 
	    itemContrib + 
	    " quantity " + remainingQuantity + 
	    " total " + totalAggregateContrib +
	    " maxCapacity " + maxCap +
	    ((mustExpand) ? " must expand " : ""));
    }

    int numItemsOnTruck = (int) (maxCap/itemContrib);

    // sanity checking 
    if (numItemsOnTruck == 0) {
      warn (".expandAggregates - WARNING - max items on truck " 
	    + numItemsOnTruck +
	    " is ZERO.  Base asset of aggregate : " + itemProto + 
	    " is " + itemContrib + " " + unit + ", which is larger than" + 
	    " carrier asset's max capacity " + maxCap + " " + unit);
      numItemsOnTruck = 1;
    }

    if (remainingQuantity == 0)
      error (".expandAggregates - ERROR - aggregate asset quantity is zero for direct object " +
	     directObject + " of task " + task.getUID());

    double maxOnTruck = ((double)numItemsOnTruck)*itemContrib;

    if (isDebugEnabled())
      debug (".expandAggregates - max items on truck " 
	    + numItemsOnTruck +
	    " max num " + maxOnTruck);

	
    // keep making subtasks
    while (totalAggregateContrib > maxCap) {  
      Task newtask;
      Asset truckSizedSet = ldmf.createAggregate (itemProto, 
						  numItemsOnTruck);
      setItemPG (truckSizedSet, itemProto);
      subtasks.add (newtask = makeTask (task, truckSizedSet, unitPG));
      totalAggregateContrib -= maxOnTruck;
      remainingQuantity     -= numItemsOnTruck;

      if (isDebugEnabled())
	debug(".expandAggregates - cargo > truckload, " +
	     "Expanding with " + 
	     numItemsOnTruck + 
	     " items on carrier, task id " + 
	     newtask.getUID() + 
	     " remaining quantity " + 
	     remainingQuantity + 
	     ", contribution " + totalAggregateContrib);
    }

    // if there's any left over, make one last task
    if (remainingQuantity > 0) {
      Asset truckSizedSet = 
	(mustExpand) ? 
	ldmf.createAggregate (itemProto, remainingQuantity) : 
	task.getDirectObject();
      setItemPG (truckSizedSet, itemProto);

      if (isDebugEnabled() && mustExpand)
	debug(".expandAggregates - cargo was > truckload, " + 
	     remainingQuantity + 
	     " items on carrier, truckSizedSet had " +
	     ((AggregateAsset)truckSizedSet).getQuantity () 
	     + " items");
      subtasks.add (makeTask (task, truckSizedSet, unitPG));
    }
  }

  /** 
   * <pre>
   * expands aggregate assets into truck-sized chunks, putting the 
   * subtasks into <code>subtasks</code> 
   *
   * Rounds the item wt to the same precision as vishnu uses so no disaggreements
   * about how much a truck can hold.
   * </pre>
   **/
  protected void expandLowFiAsset (Task task, Vector subtasks) {
    if (isDebugEnabled())
      debug ("expanding low fi asset for task " + task.getUID());
    //    test();
    GLMAsset itemProto = (GLMAsset) task.getDirectObject();
    if (isDebugEnabled()) {
      debug (".expandLowFiAsset d.o. "+ itemProto.getUID () + " original dimensions :" + 
	     " m " + itemProto.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " a " + itemProto.getPhysicalPG().getFootprintArea ().getSquareMeters() + " m^2" + 
	     " v " + itemProto.getPhysicalPG().getVolume ().getCubicMeters() + " m^3");
    }

    ForUnitPG unitPG = getForUnitPG(itemProto);

    double totalContrib = getItemAreaContribution(itemProto);
    boolean originalWasTiny = (totalContrib <= 0.00001);
    double maxCap = maxAreaCapacity - CAP_FUDGE;
	
    // round item weight to same precision as Vishnu
    BigDecimal bigD = new BigDecimal (totalContrib);
    totalContrib = bigD.setScale (2,BigDecimal.ROUND_UP).doubleValue();

    boolean mustExpand = (totalContrib > maxCap);
    double itemContrib = (mustExpand) ? maxCap : totalContrib;

    if (originalWasTiny) {
      warn (".expandLowFiAsset - item contribution " + totalContrib + 
	    " for task " + task.getUID() + 
	    "'s direct object " + itemProto + 
	    " is < 0.00001, was " + totalContrib);
    }
      
    if (isDebugEnabled()) {
      debug (".expandLowFiAsset - item contribution " + itemContrib + 
	    " maxCapacity " + maxCap +
	    ((mustExpand) ? " must expand " : ""));
    }

    int numItemsOnTruck = 1;

    double maxOnTruck = maxCap; // area

    if (isDebugEnabled())
      debug (".expandLowFiAsset - max items on truck " 
	    + numItemsOnTruck +
	    " max num " + maxOnTruck);

    PhysicalPG originalPhysicalPG = itemProto.getPhysicalPG ();
	
    // keep making subtasks
    while (totalContrib > maxCap) {  
      Task newtask;
      Asset truckSizedAsset = 
	assetHelper.createInstance (ldmProtoCache, "LowFidelityPrototype", getUniqueID(itemProto));
      adjustDimensions ((GLMAsset)truckSizedAsset, originalPhysicalPG, maxOnTruck);

      subtasks.add (newtask = makeTask (task, truckSizedAsset, unitPG));
      totalContrib -= itemContrib;

      if (isDebugEnabled())
	debug(".expandLowFiAsset - cargo > truckload, " +
	     "Expanding with " + 
	     numItemsOnTruck + 
	     " items on carrier, task id " + 
	     newtask.getUID() + 
	      ", contribution " + maxOnTruck + " total now " + totalContrib);
    }

    // if there's any left over, make one last task
    if (totalContrib > 0.00001 || originalWasTiny) {
      Asset truckSizedAsset;
      if (mustExpand) {
	truckSizedAsset = 
	  assetHelper.createInstance (ldmProtoCache, "LowFidelityPrototype", getUniqueID(itemProto));
	adjustDimensions ((GLMAsset)truckSizedAsset, originalPhysicalPG, totalContrib);
      }
      else
	truckSizedAsset = task.getDirectObject();

      if (isDebugEnabled() && mustExpand)
	debug(".expandLowFiAsset - cargo was > carrier load, " + 
	      " truckSizedAsset is " + totalContrib + " square meters");
      subtasks.add (makeTask (task, truckSizedAsset, unitPG));
    }
    if (isDebugEnabled()) {
      debug (".expandLowFiAsset d.o. "+ itemProto.getUID () + " after dimensions :" + 
	     " m " + itemProto.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " a " + itemProto.getPhysicalPG().getFootprintArea ().getSquareMeters() + " m^2" + 
	     " v " + itemProto.getPhysicalPG().getVolume ().getCubicMeters() + " m^3");
    }
  }

  /**
   * gets the contribution each individual item makes to consuming
   * the capacity of the transport vehicle
   * Sub classes may want to override this
   * @param GLMAsset -- the asset of the direct object of the task
   * @return double of the contribution in TONS
   */
  public double getItemVolumeContribution( GLMAsset itemAsset)
  {
    double itemVolume = 0.01;
    try {
      itemVolume = itemAsset.getPackagePG().getPackVolume ().getCubicMeters();
    } catch (Exception e) {
      try {
	itemVolume = itemAsset.getPhysicalPG().getVolume ().getCubicMeters();
      } catch (Exception ee) {
	if (!itemAsset.hasPersonPG ())
	  warn (".getItemVolumeContribution - unable to determine " + 
		itemAsset.getUID () + "'s volume.");
      }
    }
	
    return itemVolume;
  }

  /**
   * gets the contribution each individual item makes to consuming
   * the capacity of the transport vehicle
   * Sub classes may want to override this
   * @param GLMAsset -- the asset of the direct object of the task
   * @return double of the contribution in TONS
   */
  public double getItemAreaContribution( GLMAsset itemAsset)
  {
    double itemArea = 0.01;
    try {
      itemArea = itemAsset.getPackagePG().getPackFootprintArea ().getSquareMeters();
    } catch (Exception e) {
      try {
	itemArea = itemAsset.getPhysicalPG().getFootprintArea ().getSquareMeters();
      } catch (Exception ee) {
	if (!itemAsset.hasPersonPG ())
	  warn (".getItemContribution - unable to determine " + 
		itemAsset.getUID () + "'s area.");
      }
    }
	
    return itemArea;
  }

  /** 
   * <pre>
   * Something is a vehicle if 
   * a) it has a ground vehicle PG OR
   * b) it has a movability PG with a cargo category code whose
   *    first character is either R or A
   *
   * </pre>
   * @return true if asset is a vehicle
   */
  protected boolean isVehicle (GLMAsset asset) {
    if (asset.hasGroundVehiclePG()) return true;

    if (!asset.hasMovabilityPG())
      return false;

    try {
      MovabilityPG move_prop = asset.getMovabilityPG();
      String cargocatcode = move_prop.getCargoCategoryCode();
      char first = cargocatcode.charAt(0);
      if (first == 'R' || first == 'A')
	return true;
    } catch (Exception e) {
      return false;
    } 

    return false;
  }

  protected boolean isContainer (GLMAsset asset) {
    return asset instanceof Container;
  }

  protected void adjustDimensions (GLMAsset asset, PhysicalPG originalPhysicalPG, double squareMeters) {
    if (isDebugEnabled())
      debug (".adjustDimensions called on "+ asset.getUID ());

    double aggregateSquareMeters = originalPhysicalPG.getFootprintArea().getSquareMeters();

    double ratio = squareMeters/aggregateSquareMeters;

    double length = originalPhysicalPG.getLength().getMeters();
    double width  = originalPhysicalPG.getWidth().getMeters();
    double height = originalPhysicalPG.getHeight().getMeters();
    double area   = originalPhysicalPG.getFootprintArea().getSquareMeters();
    double volume = originalPhysicalPG.getVolume().getCubicMeters();
    double mass   = originalPhysicalPG.getMass().getKilograms();

    NewPhysicalPG newPhysicalPG = PropertyGroupFactory.newPhysicalPG ();
    newPhysicalPG.setLength(new Distance (length*ratio, Distance.METERS));
    newPhysicalPG.setWidth (new Distance (width*ratio,  Distance.METERS));
    newPhysicalPG.setHeight(new Distance (height*ratio, Distance.METERS));
    newPhysicalPG.setMass  (new Mass     (mass*ratio,   Mass.KILOGRAMS ));

    newPhysicalPG.setFootprintArea(new Area (area*ratio,   Area.SQUARE_METERS));
    newPhysicalPG.setVolume(new Volume   (volume*ratio,   Volume.CUBIC_METERS));

    if (isDebugEnabled()) {
      debug (".adjustDimensions chunk "+ asset.getUID () + " original dimensions :" + 
	     " m " + asset.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " a " + asset.getPhysicalPG().getFootprintArea ().getSquareMeters() + " m^2, " +
	     " v " + asset.getPhysicalPG().getVolume ().getCubicFeet() + " m^3");
    }

    asset.setPhysicalPG (newPhysicalPG);

    if (isDebugEnabled()) {
      debug (".adjustDimensions chunk "+ asset.getUID () + " after setting dimensions :" + 
	     " m " + asset.getPhysicalPG().getMass   ().getTons() + " tons, " +
	     " a " + asset.getPhysicalPG().getFootprintArea ().getSquareMeters() + " m^2, " +
	     " v " + asset.getPhysicalPG().getVolume ().getCubicMeters() + " m^3");
    }
  }

  //  protected double getRatio (PhysicalPG originalPhysicalPG, double originalAggregate) {
  //    double aggregateTons  = originalPhysicalPG.getMass().getTons();
  //
  //    double ratio = tons/aggregateTons;
  //  }

  /** 
   * calculates max weight, vol, pax for glmasset.
   *
   * if doesn't have pg or field on pg, slot gets Double.MAX_VALUE.
   */
  public double [] getAssetMaxContain (GLMAsset glmasset) {
    double max [] = new double [3];

    max[0] = Double.MAX_VALUE;
    max[1] = Double.MAX_VALUE;
    max[2] = Double.MAX_VALUE;

    if (glmasset.hasContainPG()) {
      try {
	max[0] = glmasset.getContainPG().getMaximumFootprintArea().getSquareMeters();
      } catch (Exception e) {
	if (isDebugEnabled()) logger.debug ("getAssetMaxContain, getting area, got exception for asset " + glmasset, e);
      }
	  
      try {
	max[1] = glmasset.getContainPG().getMaximumVolume().getCubicMeters();
      } catch (Exception e) {
	if (isDebugEnabled()) logger.debug ("getAssetMaxContain, getting volume, got exception for asset " + glmasset, e);
      }

      try {
	max[2] = (double)glmasset.getContainPG().getMaximumContainers();
      } catch (Exception e) {
	if (isDebugEnabled()) logger.debug ("getAssetMaxContain, getting containers, got exception for asset " + glmasset, e);
      }
	  
      if (isDebugEnabled())
	debug (".getAssetMaxContain : max area - " + max[0] + " max vol " + max[1] + " max containers " + max[2]);
    }
    else {
      warn ("getAssetMaxContain " + glmasset + " doesn't have a contain pg?");
    }
    return max;
  }

  /** calculates the max contain -- smallest of all seen */
  protected void calculateCommonMaxContain (double [] maxcontain) {
    if (maxcontain[0] < maxAreaCapacity && maxcontain[0] > 0)
      maxAreaCapacity   = maxcontain[0];

    if (maxcontain[1] < maxVolumeCapacity && maxcontain[1] > 0)
      maxVolumeCapacity = maxcontain[1];

    if (maxcontain[1] < maxContainerCapacity && maxcontain[2] > 0)
      maxContainerCapacity = maxcontain[2];
  }

  protected double maxAreaCapacity      = Double.MAX_VALUE;
  protected double maxVolumeCapacity    = Double.MAX_VALUE;
  protected double maxContainerCapacity = Double.MAX_VALUE;
}
