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

package org.cougaar.logistics.plugin.strattrans;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.DynamicUnaryPredicate;

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.lib.callback.UTILFilterCallbackAdapter;
import org.cougaar.lib.callback.UTILFilterCallbackListener;

import java.util.Enumeration;
import org.cougaar.util.log.Logger;

/**
 * Used to filter for new or changed generic aggregate assets or normal assets.
 *   Parameterized so that a single plugin may instantiate multiple instances
 *   of this class to handle multiple types of assets based upon the given
 *   String key passed in the constructor.  The "interestingAsset" method
 *   has an additional parameter, and the plugin must define this method to 
 *   handle each asset type accordingly based upon the key value.
 *
 *  Also may return a DynamicUnaryPredicate if so indicated by a boolean parameter
 *   passed into the constructor
 *
 */

public class UTILParameterizedAggregateAssetCallback extends UTILFilterCallbackAdapter {
  protected String myKey;
  protected boolean isDynamic;

  public UTILParameterizedAggregateAssetCallback (UTILFilterCallbackListener listener, Logger logger, String key, boolean dynamic) {
    super(listener, logger);

    myKey = key;
    isDynamic = dynamic;
    mySub = myListener.subscribeFromCallback(getPredicate (), getCollection ());
  }

  protected UnaryPredicate getPredicate () {
    if (isDynamic) {
      return new DynamicUnaryPredicate() {
        public boolean execute(Object o) {
          if (myKey == null)
            return false;  // needed to handle case where constructor super() calls this first before the key is set.

          while (o instanceof AggregateAsset)
            o = ((AggregateAsset)o).getAsset(); // handle case with nested aggregate assets
          return (o instanceof Asset && ((UTILParameterizedAssetListener) myListener).interestingParameterizedAsset((Asset) o, myKey));
        }
      };
    }
    else {
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          if (myKey == null)
            return false;  // needed to handle case where constructor super() calls this first before the key is set.

          while (o instanceof AggregateAsset)
            o = ((AggregateAsset)o).getAsset(); // handle case with nested aggregate assets
          return (o instanceof Asset && ((UTILParameterizedAssetListener) myListener).interestingParameterizedAsset((Asset) o, myKey));
        }
      };
    }
  }

  public void reactToChangedFilter () {
    Enumeration changedList = mySub.getChangedList();
    Enumeration addedList   = mySub.getAddedList();

    if (changedList.hasMoreElements ())
      ((UTILParameterizedAssetListener) myListener).handleChangedParameterizedAssets (changedList, myKey);

    if (addedList.hasMoreElements ())
      ((UTILParameterizedAssetListener) myListener).handleNewParameterizedAssets (addedList, myKey);
  }
}
