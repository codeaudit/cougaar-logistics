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

import java.awt.Point;
import java.awt.Dimension;

import java.io.Serializable;

/***********************************************************************************************************************
<b>Description</b>: This class stores current information about each desktop frame window within the desktop.  It is
                    used by the DesktopInfo class to store/retrieve information about the current application.

***********************************************************************************************************************/
public class FrameInfo implements java.io.Serializable
{
  protected transient CougaarDesktopUI component = null;
  
  protected Serializable componentData = null;
	protected String componentFactory = null;

	protected Point frameLocation = null;
	protected Dimension componentSize = null;
  protected boolean iconified = false;
  protected boolean selected = false;

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a frame information instance with the specified values.

  <br>
  @param factory Factory name of the CougaarDesktopUI component
  @param location Location of the component's window within the desktop scroll pane
  @param icon Indicator as to whether or not the component's window is iconified
  @param select Indicator as to whether or not the component's window is selected
	*********************************************************************************************************************/
	public FrameInfo(String factory, Point location, boolean icon, boolean select)
	{
		componentFactory = factory;
		component = ComponentFactoryRegistry.getFactory(componentFactory).create();

		frameLocation = location;
		componentSize = component.getPreferredSize();
		iconified = icon;
		selected = select;
	}

	/*********************************************************************************************************************
  <b>Description</b>: Constructs a frame information instance with the specified values.

  <br>
  @param factory Factory name of the CougaarDesktopUI component
  @param data Component application data
  @param location Location of the component's window within the desktop scroll pane
  @param icon Indicator as to whether or not the component's window is iconified
  @param select Indicator as to whether or not the component's window is selected
	*********************************************************************************************************************/
	public FrameInfo(String factory, Serializable data, Point location, boolean icon, boolean select)
	{
	  this(factory, location, icon, select);

		componentData = data;
	}

	/*********************************************************************************************************************
  <b>Description</b>: Returns the CougaarDesktopUI component specified by this information object.

  <br><b>Notes</b>:<br>
	                  - Unless the component has already been created, the component factory is used to create the
	                    component

  <br>
  @return CougaarDesktopUI component specified by this information object
	*********************************************************************************************************************/
  public CougaarDesktopUI getComponent()
  {
    if (component == null)
    {
      component = ComponentFactoryRegistry.getFactory(componentFactory).create();
    }

    return(component);
  }
}
