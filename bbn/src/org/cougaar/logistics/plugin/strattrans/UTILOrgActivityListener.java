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
        
        
                
                        
                
        
        
