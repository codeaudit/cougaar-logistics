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

import org.cougaar.glm.ldm.oplan.OrgActivity;

import org.cougaar.lib.callback.UTILFilterCallbackListener;

/**
 * OrgActivity listener -- can be used with org-activity callback.
 */

public interface UTILOrgActivityListener extends UTILFilterCallbackListener {

  /** 
   * Defines org activities you find interesting. 
   * @param a OrgActivity to check for interest
   * @return boolean true if activities is interesting
   */
  boolean interestingOrgActivity (OrgActivity a);

  /**
   * Place to handle updated activities.
   * @param e Enumeration of new activities found in the container
   */
  void handleNewOrgActivities (Enumeration e);

  /**
   * Place to handle changed activities.
   * @param e Enumeration of changed activities found in the container
   */
  void handleChangedOrgActivities (Enumeration e);
}
        
        
                
                        
                
        
        
