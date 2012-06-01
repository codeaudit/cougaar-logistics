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

import java.util.Enumeration;

import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.lib.callback.UTILFilterCallbackListener;

/**
 * Parameterized Asset listener -- can be used with ParameterizedAggregateAssetCallback.
 */

public interface UTILParameterizedAssetListener extends UTILFilterCallbackListener {

  /** 
   * Defines assets you find interesting, based upon given key.
   * @param a An Asset to check for interest
   * @param key String to identify key of asset of interest
   * @return boolean true if asset is interesting
   */
  boolean interestingParameterizedAsset (Asset a, String key);

  /**
   * Place to handle updated assets, based upon given key.
   * @param e An Enumeration of new assets found in the container
   */
  void handleNewParameterizedAssets     (Enumeration e, String key);

  /**
   * Place to handle changed assets, based upon given key.
   * @param e An Enumeration of changed assets found in the container
   */
  void handleChangedParameterizedAssets (Enumeration e, String key);
}
        
        
                
                        
                
        
        
