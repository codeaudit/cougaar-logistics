/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
//import org.cougaar.glm.ldm.asset.LocationSchedulePG;
import org.cougaar.glm.ldm.asset.MilitaryOrgPG;
import org.cougaar.glm.ldm.plan.GeolocLocation;

import org.cougaar.glm.ldm.asset.Organization;
//import org.cougaar.glm.plugins.TaskUtils;

import org.cougaar.planning.ldm.trigger.*;
import org.cougaar.planning.plugin.legacy.PluginDelegate;

import org.cougaar.util.UnaryPredicate;




/***********************************************************************************************************************
<b>Description</b>: Allocate transport tasks to roles".

***********************************************************************************************************************/

public class DeskTopTestPlugin extends org.cougaar.planning.plugin.legacy.SimplePlugin
{
	
	
  
// ---------------------------------------------------------------------------------------------------------------------
// Public Member Methods
// ---------------------------------------------------------------------------------------------------------------------

	/*********************************************************************************************************************
  <b>Description</b>: Subscribe to "pack the books" tasks and any changes in the inventory.

	*********************************************************************************************************************/
  public void setupSubscriptions()
  {
    
  }


	  

	/*********************************************************************************************************************
  <b>Description</b>: Called by infrastructure whenever something we are interested in is changed or added.

	*********************************************************************************************************************/
  public void execute()
  {
  	

  }
 
  /*********************************************************************************************************************
  <b>Description</b>: Looks at the Plugin parameters for the debug value.
	*********************************************************************************************************************/
  private void parseParameters()
  {
  	// Look through the Plugin parameters for the packer time
  	//System.out.println("&&&& parsing");
    //Vector pVec = getParameters();
    //if (pVec.size() > 0)
    //{
    	
    	//System.out.println("setting debug to" + locationDebug);
    //}
  }
}
