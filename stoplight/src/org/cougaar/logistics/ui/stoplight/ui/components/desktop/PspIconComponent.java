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
import java.awt.Component;

import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;


import org.cougaar.logistics.ui.stoplight.ui.components.*;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

/***********************************************************************************************************************
<b>Description</b>: This class is the implementation of the PSP Icon Cougaar Desktop component.  It provides
                    the display of the PSP Icon map application from within the Cougaar Desktop application.

***********************************************************************************************************************/
public class PspIconComponent extends ComponentFactory implements DateControllableSliderUI
{
  static
  {
    try
    {
      Class.forName("org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap");
    }
    catch (Throwable t)
    {
      throw(new RuntimeException(t.toString()));
    }
  }

	public String getToolDisplayName()
	{
	  return("Psp Icon OpenMap");
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
      CougaarUI ui = (CougaarUI)Class.forName("org.cougaar.logistics.ui.stoplight.ui.map.app.ScenarioMap").newInstance();
      ui.install(f);
    }
    catch (Throwable t)
    {
      t.printStackTrace();
      f.getContentPane().setLayout(new BorderLayout());
      f.getContentPane().add(new JLabel(t.toString()), BorderLayout.CENTER);
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
    return("Psp Icon OpenMap");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(800, 600));
  }

  public boolean isResizable()
  {
    return(true);
  }

  public SliderProxy getDateControllableSlider()
  {
    return(new CDateSliderProxy(RangeSliderPanel.rangeSlider));
  }
}
