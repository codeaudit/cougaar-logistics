/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

/* @generated Tue May 07 17:35:22 EDT 2002 from properties.def - DO NOT HAND EDIT */
/** Abstract Asset Skeleton implementation
 * Implements default property getters, and additional property
 * lists.
 * Intended to be extended by org.cougaar.planning.ldm.asset.Asset
 **/

package org.cougaar.logistics.plugin.trans;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;


import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public abstract class AssetSkeleton extends org.cougaar.planning.ldm.asset.AssetSkeletonBase {

  protected AssetSkeleton() {}

  protected AssetSkeleton(AssetSkeleton prototype) {
    super(prototype);
  }

  /**                 Default PG accessors               **/

  /** Search additional properties for a LowFidelityAssetUIDPG instance.
   * @return instance of LowFidelityAssetUIDPG or null.
   **/
  public LowFidelityAssetUIDPG getLowFidelityAssetUIDPG()
  {
    LowFidelityAssetUIDPG _tmp = (LowFidelityAssetUIDPG) resolvePG(LowFidelityAssetUIDPG.class);
    return (_tmp==LowFidelityAssetUIDPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a LowFidelityAssetUIDPG
   **/
  public boolean hasLowFidelityAssetUIDPG() {
    return (getLowFidelityAssetUIDPG() != null);
  }

  /** Set the LowFidelityAssetUIDPG property.
   * The default implementation will create a new LowFidelityAssetUIDPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setLowFidelityAssetUIDPG(PropertyGroup aLowFidelityAssetUIDPG) {
    if (aLowFidelityAssetUIDPG == null) {
      removeOtherPropertyGroup(LowFidelityAssetUIDPG.class);
    } else {
      addOtherPropertyGroup(aLowFidelityAssetUIDPG);
    }
  }

}
