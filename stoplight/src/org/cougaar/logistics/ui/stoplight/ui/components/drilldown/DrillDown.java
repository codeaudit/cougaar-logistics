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

package org.cougaar.logistics.ui.stoplight.ui.components.drilldown;

import java.awt.Component;
import java.awt.event.MouseEvent;


/***********************************************************************************************************************
<b>Description</b>: Interface used to interact with a component that can be drilled down into.  UI Components, or
                    classes with UI components, implement this so the DrillDownStack can interact with the component or
                    other class types.

<br><br><b>Notes</b>:<br>
									- Any notes about the class go here

***********************************************************************************************************************/
public interface DrillDown
{
	/*********************************************************************************************************************
  <b>Description</b>: Called when the DrillDownStack receives a double click on a trigger area.

  <br><b>Notes</b>:<br>
	                  - Returning null will stop the drill down action

  <br>
  @param e Mouse event that triggered the action
  @return Next drill down to make active
	*********************************************************************************************************************/
  public DrillDown getNextDrillDown(MouseEvent e);

	/*********************************************************************************************************************
  <b>Description</b>: Called by the parent drill down to initialize its data.

  <br><b>Notes</b>:<br>
	                  - This is a convience method and is not required to be called

  <br>
  @param data Initialization data
	*********************************************************************************************************************/
  public void setData(Object data);

	/*********************************************************************************************************************
  <b>Description</b>: Called when the DrillDownStack gets a vaild drill down from the parent drill down.

  <br><b>Notes</b>:<br>
	                  - Any child drill down objects should be added to the DrillDownStack within this method
	                  - Returning null will stop the drill down action

  <br>
  @param drillDownStack DrillDownStack object to add child drill downs to
  @return UI component to display for the current drill down
	*********************************************************************************************************************/
  public Component activate(DrillDownStack drillDownStack);
}
