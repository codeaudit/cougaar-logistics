/* 
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;

// Constants here are in place of references here
//import org.cougaar.mlm.ui.grabber.connect.DGPSPConstants;

// which were using numbers from
//import org.cougaar.mlm.ui.psp.transit.data.population.ConveyancePrototype;

/**
 * Constants used by <code>RouteJdbcConnector</code> and 
 * <code>PspIconLayerModel</code> and <code>PspIconLayer</code>, duplicating constants from 
 * the <code>datagrabber</code> module (not open-source). The original 
 * usage was to refer to <code>DGPSPConstants.CONV_TYPE_SHIP</code> and 
 * <code>.CONV_TYPE_PLANE</code>. These in turn really are 
 * <code>ConveyancePrototype.ASSET_TYPE_SHIP</code>, etc. If the 
 * constants are changed in the <code>datagrabber</code> module, 
 * they will also be changed here.
 * @see RouteJdbcConnector
 * @see PspIconLayerModel
 * @see PspIconLayer
 **/
public interface AssetTypeConstants {
    //types:
  /**none of the below**/
  int ASSET_TYPE_UNKNOWN = 0;
  /**specific moving conveyances (trucks, etc)**/
  int ASSET_TYPE_TRUCK = 1;
  int ASSET_TYPE_TRAIN = 2;
  int ASSET_TYPE_PLANE = 3;
  int ASSET_TYPE_SHIP = 4;
  /**catch-all for other moving conveyances (tanks, etc)**/
  int ASSET_TYPE_SELF_PROPELLABLE = 5;
  /**non-moving "conveyances" that one can assign to**/
  int ASSET_TYPE_DECK = 6;
  int ASSET_TYPE_PERSON = 7;
  int ASSET_TYPE_FACILITY = 8;
}
