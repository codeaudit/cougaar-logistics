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

import java.awt.Dimension;
import java.io.Serializable;

/***********************************************************************************************************************
<b>Description</b>: This interface is implemented by all applications intended to be used within the Cougaar Desktop
                    environment.

***********************************************************************************************************************/
public interface CougaarDesktopUI extends org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI
{
//  public static final String COUGAAR_DESKTOP_UI_PROP = "CougaarDesktopUI";

  // Method call order upon creation:
  // getPreferredSize();
  // isResizable();
  // install(CDesktopFrame f);

  // Method call order upon serialization:
  // isPersistable();
  // getPersistedData();  (if persistable)
  // frameClosed();  ????????????????

  // Method call order upon rehydration:
  // isPersistable();
  // setPersistedData(Serializable data);  (if persistable)
  // install(CDesktopFrame f);
  // frameMinimized();  (If stored as such)
  // frameMaximized();  (If stored as such)

	/*********************************************************************************************************************
  <b>Description</b>: Called when a CougaarDesktopUI instance is to add its visual display components to the specified
                      desktop frame.

  <br>
  @param f Desktop frame to disply component within
	*********************************************************************************************************************/
  public void install(CDesktopFrame f);

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the component is persitable.

  <br>
  @return True if the component should be persisted, false otherwise
	*********************************************************************************************************************/
  public boolean isPersistable();

	/*********************************************************************************************************************
  <b>Description</b>: Returns persistable data of the component.

  <br>
  @return Persistable data of the component
	*********************************************************************************************************************/
  public Serializable getPersistedData();

	/*********************************************************************************************************************
  <b>Description</b>: Called when a CougaarDesktopUI instance un-persisted and required to re-initialize itself.

  <br>
  @param data Data to use during re-initialization
	*********************************************************************************************************************/
  public void setPersistedData(Serializable data);

	/*********************************************************************************************************************
  <b>Description</b>: Returns the initial frame window title to be displayed.

  <br>
  @return Title text to display
	*********************************************************************************************************************/
  public String getTitle();

	/*********************************************************************************************************************
  <b>Description</b>: Returns preferred initial window dimensions of this component.

  <br>
  @return Preferred initial window size
	*********************************************************************************************************************/
  public Dimension getPreferredSize();

	/*********************************************************************************************************************
  <b>Description</b>: Returns an indicator as to whether or not the component's window should be resizable.

  <br>
  @return True if the component's window should be resizable, false otherwise
	*********************************************************************************************************************/
  public boolean isResizable();

/*
  public void frameIconified();
  public void frameDeIconified();
  public void frameMaximized();
  public void frameClosed();
*/
}
