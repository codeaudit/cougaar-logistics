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
   * @param a String to identify key of asset of interest
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
        
        
                
                        
                
        
        
