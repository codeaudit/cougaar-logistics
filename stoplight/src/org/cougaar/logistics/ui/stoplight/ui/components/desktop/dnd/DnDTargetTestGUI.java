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

package org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd;

import java.awt.*;
import java.awt.datatransfer.*;

import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import java.util.Vector;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CDesktopFrame;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI;

/***********************************************************************************************************************
<b>Description</b>: This class is a test Cougaar Desktop component for drag and drop capabilities in the Cougaar
                    Desktop application.  This is the target component which a text string can be dropped to from the
                    DnDSourceTestGUI desktop component.

***********************************************************************************************************************/
public class DnDTargetTestGUI extends org.cougaar.logistics.ui.stoplight.ui.components.desktop.ComponentFactory implements org.cougaar.logistics.ui.stoplight.ui.components.desktop.CougaarDesktopUI, DropTarget
{
  private JTextArea textArea = new JTextArea();
  private Color background = textArea.getBackground();

  // Drag & Drop supporting class
  private DragAndDropSupport dndSupport = new DragAndDropSupport();



  // ------------------- DragSource Interface ----------------------------  

  public Vector getTargetComponents()
  {
    Vector components = new Vector(1);
    components.add(textArea);
    
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
		if(show)
		{
		  if (droppable)
		  {
			  textArea.setBackground(Color.green);
		  }
		  else
		  {
			  textArea.setBackground(Color.red);
			}
		}
		else
		{
			textArea.setBackground(background);
		}
  }

  public void dropData(Component componentAt, Point location, DataFlavor flavor, Object data)
  {
  	textArea.setText(data.toString());
  }

  public Vector getSupportedDataFlavors(Component componentAt, Point location)
  {
    Vector flavors = new Vector(1);
    flavors.add(ObjectTransferable.getDataFlavor(String.class));
    
    return(flavors);
  }




  public void install(CDesktopFrame f)
  {
	  JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(textArea, BorderLayout.CENTER);

    f.getContentPane().add(panel);


    // Add the drop target
    dndSupport.addDropTarget(this);
  }







	public String getToolDisplayName()
	{
	  return("DnD Target Test UI");
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
    return("DnD Target Test UI");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(200, 200));
  }

  public boolean isResizable()
  {
    return(true);
  }
}
