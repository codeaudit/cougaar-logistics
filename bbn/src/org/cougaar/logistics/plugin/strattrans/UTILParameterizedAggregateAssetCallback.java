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
