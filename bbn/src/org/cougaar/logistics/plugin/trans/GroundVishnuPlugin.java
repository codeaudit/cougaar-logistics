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

import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;

import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.GLMAsset;
import org.cougaar.glm.ldm.asset.MovabilityPG;
import org.cougaar.glm.ldm.plan.GeolocLocation;  
import org.cougaar.glm.util.GLMMeasure;
import org.cougaar.glm.util.GLMPrepPhrase;

import org.cougaar.planning.ldm.measure.Distance;

public class GroundVishnuPlugin extends GenericVishnuPlugin {
  public void localSetup () {
    super.localSetup ();

    glmPrepHelper = new GLMPrepPhrase (logger);
    measureHelper = new GLMMeasure    (logger);
    
    try {
      if (getMyParams ().hasParam ("MAX_DISTANCE"))
	MAX_DISTANCE=getMyParams().getIntParam ("MAX_DISTANCE");
      if (getMyParams ().hasParam ("complainAboutMissingMovabilityPG"))
	complainAboutMissingMovabilityPG=getMyParams().getBooleanParam ("complainAboutMissingMovabilityPG");
    } catch (Exception e) { if (isWarnEnabled()) { warn ("got unexpected exception " + e); } }
  }

  /** 
   * Only tasks with transport verbs are given to Vishnu   <p>
   * If a task has no FROM prep, it is not handled.         <p>
   * If a task has a FROM prep that is in CONUS, it is not handled. <p>
   * If a task is an MPTask (and possibly an output of the plugin), it is not handled.
   */
  public boolean interestingTask(Task t) {
    boolean superVal = super.interestingTask (t);
    if (!superVal) return false;

    boolean hasTransportVerb = t.getVerb().equals (Constants.Verb.TRANSPORT);

    if (!hasTransportVerb)
      return false;
	  
    boolean isSelfPropelled = false;

    Asset directObject = t.getDirectObject ();
    if (!(directObject instanceof AssetGroup))
      isSelfPropelled = isSelfPropelled (t, directObject);

    boolean val = !isSelfPropelled;

    if (isDebugEnabled() && val) {
      debug (getName () + ".interestingTask - interested in " + t.getUID());
    }
    if (isDebugEnabled() && !val) {
      debug (getName () + ".interestingTask - IGNORING self-propelled " + t.getUID());
    }
	
    return val;
  }
  
  public boolean interestingAsset(Asset a) {
    if (a instanceof AggregateAsset) 
      return false;
    if (!(a instanceof GLMAsset))
      return false;
	
    return ((GLMAsset) a).hasContainPG ();
  }

  public boolean isTaskWellFormed(Task t) {
    GeolocLocation from = glmPrepHelper.getFromLocation (t);
    GeolocLocation to   = glmPrepHelper.getToLocation (t);
	
    Distance distance = measureHelper.distanceBetween (from, to);
	
    return (distance.getMiles () < MAX_DISTANCE);
  }

  protected void reportIllFormedTask (Task t) {
    super.reportIllFormedTask (t);

    if (!isTaskWellFormed (t)) {
      GeolocLocation from = glmPrepHelper.getFromLocation (t);
      GeolocLocation to   = glmPrepHelper.getToLocation (t);
      
      Distance distance = measureHelper.distanceBetween (from, to);

      error (getName () + ".reportIllFormedTask - task " + t.getUID() + 
	     " distance between FROM " + from + 
	     " and to " + to +
	     " is > " + MAX_DISTANCE + " miles = " + distance.getMiles());
    }
  }
  
  protected boolean isSelfPropelled (Task t, Asset directObject) {
    GLMAsset baseAsset = 
      (directObject instanceof AggregateAsset) ? 
      (GLMAsset) ((AggregateAsset)directObject).getAsset() : 
      (GLMAsset) directObject;
	
    MovabilityPG move_prop = baseAsset.getMovabilityPG();

    if (move_prop != null) {
      String cargocatcode = move_prop.getCargoCategoryCode();
      if ((cargocatcode != null) && (cargocatcode.charAt(0) == 'R')) {
	if (isDebugEnabled()) {
	  debug (getName() + ".isSelfPropelled - found self-propelled vehicle on task " + t.getUID());
	}
	return true;
      }
    }
    else if (complainAboutMissingMovabilityPG) {
      error (getName() + ".isSelfPropelled - asset " + baseAsset + 
	     " is missing its movability PG.");
    }

    return false;
  }

  protected boolean complainAboutMissingMovabilityPG = false;
  protected GLMPrepPhrase glmPrepHelper;
  protected GLMMeasure measureHelper;
  public int MAX_DISTANCE = 2000;
}
