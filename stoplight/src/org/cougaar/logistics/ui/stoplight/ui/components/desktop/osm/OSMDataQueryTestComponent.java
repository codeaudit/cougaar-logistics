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

package org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.io.Serializable;

import javax.swing.*;
import javax.swing.table.*;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.*;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;

/***********************************************************************************************************************
<b>Description</b>: This class is a test Cougaar Desktop component for the Object Storage Manager (OSM) capabilities in
                    the Cougaar Desktop application.  This component tests the query capabilities of the OSM.

***********************************************************************************************************************/
public class OSMDataQueryTestComponent extends ComponentFactory implements CougaarDesktopUI, DropTarget
{
	public String getToolDisplayName()
	{
	  return("OSM Data Query Test Component");
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
      osm = f.getDesktop().getOSM();
      setUp(f.getContentPane());
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
    return("OSM Data Query Test Component");
  }

  public Dimension getPreferredSize()
  {
    return(new Dimension(250, 200));
  }

  public boolean isResizable()
  {
    return(true);
  }

  // Drag & Drop supporting class
  private DragAndDropSupport dndSupport = new DragAndDropSupport();

  // ------------------- DropTarget Interface ----------------------------  

  public Vector getTargetComponents()
  {
    Vector components = new Vector(1);
    components.add(contentPane);
    
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
/*		if(show)
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
		}*/
  }

  public Vector getSupportedDataFlavors(Component componentAt, Point location)
  {
    Vector flavors = new Vector(1);
    flavors.add(ObjectTransferable.getDataFlavor(ObjectID.class));
    
    return(flavors);
  }


  public void dropData(Component componentAt, Point location, DataFlavor flavor, Object data)
  {
    objectIDLabel.setText(data.toString());
    oid = (ObjectID)data;
  }

  private Container contentPane;
  private ObjectID oid = null;
  private ObjectStorageManager osm;
  private JLabel objectIDLabel = new JLabel();
  private JTextField objectTypeField = new JTextField();

  private JTextArea outputArea = new JTextArea();

  private void setUp(Container contentPane)
  {
    this.contentPane = contentPane;
    contentPane.setLayout(new BorderLayout());
    
    JPanel upperPanel = new JPanel();
    upperPanel.setLayout(new GridLayout(2, 2));
    contentPane.add(upperPanel, BorderLayout.NORTH);

    JButton findObjectWithObjectIDButton = new JButton("Find Object With Object ID: ");
    findObjectWithObjectIDButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataQueryTestComponent.this.findObjectWithObjectID();
        }
      });
    upperPanel.add(findObjectWithObjectIDButton);
    upperPanel.add(objectIDLabel);

    JButton findObjectsOfTypeButton = new JButton("Find Objects Of Type:");
    findObjectsOfTypeButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataQueryTestComponent.this.findObjectsOfType();
        }
      });
    upperPanel.add(findObjectsOfTypeButton);
    upperPanel.add(objectTypeField);

    outputArea.setEditable(false);
    contentPane.add(new JScrollPane(outputArea), BorderLayout.CENTER);

    dndSupport.addDropTarget(this);
  }
  
  private void findObjectWithObjectID()
  {
    if (oid != null)
    {
      Object object = osm.find(oid);
      outputArea.append("\n\nQuery \"Find object with object ID " + oid + "\" resulted in 1 object(s) found. ------------------");
      outputArea.append("\n\nClass Type: " + object.getClass());
      outputArea.append("\nObject ID: " + oid);
      outputArea.append("\nExpires: " + (osm.isExpirationSet(oid) ? (new Date(osm.getExpiration(oid))).toString() : "(No Expiration)"));
      outputArea.append("\nValue: " + object);
    }
  }

  private void findObjectsOfType()
  {
    String type = objectTypeField.getText();
    
    if (type.length() == 0)
    {
      return;
    }
    
    try
    {
      Class objectType = Class.forName(type);
      
      Object[] objects = osm.find(objectType);
      outputArea.append("\n\nQuery \"Find objects of class " + objectType.getName() + "\" resulted in " + objects.length + " object(s) found. ------------------");
      for (int i=0; i<objects.length; i++)
      {
        outputArea.append("\n\nClass Type: " + objects[i].getClass());
        outputArea.append("\nObject ID: " + osm.getObjectID(objects[i]));
        outputArea.append("\nExpires: " + (osm.isExpirationSet(objects[i]) ? (new Date(osm.getExpiration(objects[i]))).toString() : "(No Expiration)"));
        outputArea.append("\nValue: " + objects[i]);
      }
    }
    catch (ClassNotFoundException e)
    {
      outputArea.append("\n\nRequested class, " + type + ", is an invalid class type.");
    }
  }
}
