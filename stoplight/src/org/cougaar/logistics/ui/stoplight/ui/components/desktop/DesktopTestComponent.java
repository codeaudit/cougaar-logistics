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
import java.awt.BorderLayout;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

/***********************************************************************************************************************
<b>Description</b>: This class is a general test Cougaar Desktop component.  Is is a simple component created to show
                    what needs to be done to build a desktop component.

***********************************************************************************************************************/
public class DesktopTestComponent extends ComponentFactory implements CougaarDesktopUI
{
  private int count = 0;

  // Method call order upon creation:
  // getPreferredSize();
  // isResizable();
  // install(CDesktopFrame f);

  // Method call order upon serialization:
  // getPersistedData();
  // frameClosed();

  // Method call order upon rehydration:
  // setPersistedData(Serializable data);
  // install(CDesktopFrame f);
  // frameMinimized();  (If stored as such)
  // frameMaximized();  (If stored as such)

	public String getToolDisplayName()
	{
	  return("Desktop Test Component");
	}

	public CougaarDesktopUI create()
	{
	  return(this);
	}

  public boolean supportsPlaf()
  {
    return(true);
  }

  public void install(JFrame f)
  {
    throw(new RuntimeException("install(JFrame f) not supported"));
  }

  public void install(JInternalFrame f)
  {
    throw(new RuntimeException("install(JInternalFrame f) not supported"));
  }

  public void install(CDesktopFrame f)
  {
    try
    {
      JButton button = new JButton("Press Me!!!");
  		button.addActionListener(new ListenerAction(this, "buttonPressed", new Object[] {f}, ListenerAction.actionPerformed));
  
      f.getContentPane().setLayout(new BorderLayout());
      f.getContentPane().add(button, BorderLayout.CENTER);
    }
    catch (Throwable t)
    {
      t.printStackTrace();
    }
  }

  public boolean isPersistable()
  {
    return(false);
  }

  public Serializable getPersistedData()
  {
    return(null);
  }

  public void setPersistedData(Serializable data)
  {
  }

  public String getTitle()
  {
    return("Test Button " + count++);
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(250, 50));
  }

  public boolean isResizable()
  {
    return(true);
  }

  public void buttonPressed(CDesktopFrame frame)
  {
    frame.setTitle("Test Button " + count++);
  }
}
