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

import java.awt.*;
import java.util.*;
import java.awt.datatransfer.*;

import java.io.Serializable;

import javax.swing.*;



import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;
import org.cougaar.logistics.ui.stoplight.ui.components.mthumbslider.*;
import org.cougaar.logistics.ui.stoplight.ui.inventory.*;
import org.cougaar.logistics.ui.stoplight.ui.util.CougaarUI;

/***********************************************************************************************************************
<b>Description</b>: This class is the implementation of the BlackJackUI chart Cougaar Desktop component.  It provides
                    the display of the BlackJackUI inventory chart application from within the Cougaar Desktop
                    application.

***********************************************************************************************************************/
public class BlackJackUIComponent extends ComponentFactory implements DateControllableSliderUI, DragSource, DropTarget
{
  private InventorySelector selector = null;

  private StartUpInfo startUpInfo = null;

  private CDesktopFrame containingFrame = null;

  private Vector components = new Vector(1);
  private Vector flavors = new Vector(1);

  public void install(CDesktopFrame f)
  {
    containingFrame = f;

    if (startUpInfo == null)
    {
      startUpInfo = new StartUpInfo();
    }

    selector = new InventorySelector(startUpInfo.host, startUpInfo.port, startUpInfo.file, startUpInfo.cluster, startUpInfo.asset, 0, 0);
    selector.install(f);

    setDropTargets(f.getContentPane());

    flavors.add(ObjectTransferable.getDataFlavor(Vector.class));

    DragAndDropSupport dndSupport = new DragAndDropSupport();

    dndSupport.addDragSource(this);
    dndSupport.addDropTarget(this);
  }

  private void setDropTargets(Component parent)
  {
  	if ((parent instanceof Container) && (!(parent instanceof COrderedLabeledMThumbSlider)))
  	{
  		Component[] componentList = ((Container)parent).getComponents();
  		for (int i=0; i<componentList.length; i++)
  		{
  			setDropTargets(componentList[i]);
  		}
  	}
  	
  	if (!(parent instanceof COrderedLabeledMThumbSlider))
  	{
      components.add(parent);
    }
  }

  public SliderProxy getDateControllableSlider()
  {
    if (selector != null)
    {
      return(new CDateRangeSliderProxy(selector.getRangeControl()));
    }
    
    return(null);
  }

  // ------------- DragSource Support -------------------

  public Vector getSourceComponents()
  {
    return(components);
  }

  public boolean dragFromSubComponents()
  {
    return(false);
  }

  public Object getData(Component componentAt, Point location)
  {
    return(selector);
  }

  public void dragDropEnd(boolean success)
  {
  }



  // ------------- DragSource Support -------------------

  public Vector getTargetComponents()
  {
    return(components);
  }

  public boolean dropToSubComponents()
  {
    return(true);
  }

  public boolean readyForDrop(Component componentAt, Point location, DataFlavor flavor)
  {
    return(true);
  }

  public void showAsDroppable(Component componentAt, Point location, DataFlavor flavor, boolean show, boolean droppable)
  {
    // Do nothing here
  }

  public void dropData(Component componentAt, Point location, DataFlavor flavor, Object droppedData)
  {
    Vector nameList = (Vector)droppedData;
    
    if (nameList.size() <1)
    {
      return;
    }

    StartUpInfo info = new StartUpInfo((StartUpInfo)getPersistedData());
    String factoryName = getClass().getName();

    // Set the first one in the current chart
    selector.setSelectedCluster((String)nameList.elementAt(0));

    for (int i=1, isize=nameList.size(); i<isize; i++)
    {
      // Keep the asset the same, but change the cluster
      info.cluster = (String)nameList.elementAt(i);
      containingFrame.createTool(factoryName, info);
    }
  }

  public Vector getSupportedDataFlavors(Component componentAt, Point location)
  {
    return(flavors);
  }






	public String getToolDisplayName()
	{
	  return("BlackJack UI");
	}

	public CougaarDesktopUI create()
	{
	  return(new BlackJackUIComponent());
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

  public boolean isPersistable()
  {
    return(true);
  }

  public Serializable getPersistedData()
  {
    startUpInfo.host = selector.getClusterHost();
    startUpInfo.port = selector.getClusterPort();
    startUpInfo.file = selector.getFileName();
    startUpInfo.cluster = selector.getSelectedCluster();
    startUpInfo.asset = selector.getSelectedAsset();

    return(startUpInfo);
  }

  public void setPersistedData(Serializable data)
  {
    startUpInfo = (StartUpInfo)data;
  }

  public String getTitle()
  {
    return("BlackJack UI");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(800, 600));
  }

  public boolean isResizable()
  {
    return(true);
  }
}

class StartUpInfo implements Serializable
{
  public String host = null;
  public String port = null;
  public String file = null;
  public String cluster = null;
  public String asset = null;

  public StartUpInfo()
  {
  }

  public StartUpInfo(String host, String port, String file, String cluster, String asset)
  {
    this.host = host;
    this.port = port;
    this.file = file;
    this.cluster = cluster;
    this.asset = asset;
  }

  public StartUpInfo(StartUpInfo info)
  {
    host = info.host;
    port = info.port;
    file = info.file;
    cluster = info.cluster;
    asset = info.asset;
  }
  
  public String toString()
  {
    return(host + ":" + port + ":" + file + ":" + cluster + ":" + asset);
  }
}
