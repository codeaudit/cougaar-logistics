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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.awt.*;
import java.util.*;

import java.io.Serializable;

import java.awt.dnd.*;

import javax.swing.*;


import org.cougaar.logistics.ui.stoplight.ui.components.graph.*;
import org.cougaar.logistics.ui.stoplight.ui.inventory.*;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

public class DnDUIComponent extends ComponentFactory implements CougaarDesktopUI
{
  private DnDUI selector = null;
  private Vector persistedData = null;
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
    	
      selector = new DnDUI();
      selector.install(f);
    }
    catch(Exception e)
    {
    	
    }
  }
  
  public String getToolDisplayName()
	{
	  return("DnD UI");
	}
  
  public CougaarDesktopUI create()
	{
	  return(this);
	}
  
  public boolean supportsPlaf()
  {
    return(true);
  }
  
  // #DnD -------------------------------------------------------------------

  public boolean isPersistable()
  {
    return(false);
  }
  
  public Serializable getPersistedData()
  {
  	
  	
    return null;
  }

  public void setPersistedData(Serializable data)
   
  {
  	
  }
  
  public String getTitle()
  {
    return(null);
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(200, 400));
  }

  public boolean isResizable()
  {
    return(true);
  }

}
