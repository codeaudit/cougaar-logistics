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
                    the Cougaar Desktop application.  This component tests the notification capabilities of the OSM.

***********************************************************************************************************************/
public class OSMDataListenerTestComponent extends ComponentFactory implements CougaarDesktopUI, DropTarget
{
	public String getToolDisplayName()
	{
	  return("OSM Data Listener Test Component");
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
    return("OSM Data Listener Test Component");
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

  private JCheckBox storeCheck = new JCheckBox("Store", true);
  private JCheckBox updateCheck = new JCheckBox("Update", true);
  private JCheckBox objectUpdateCheck = new JCheckBox("Object Update", true);
  private JCheckBox expirationUpdateCheck = new JCheckBox("Expiration Update", true);
  private JCheckBox deleteCheck = new JCheckBox("Delete", true);

  private JTable listenerTable;
  private ListenerTableModel model;

  private JTextArea outputArea = new JTextArea();
  private Vector listenersToRow = new Vector();

  private void setUp(Container contentPane)
  {
    this.contentPane = contentPane;
    contentPane.setLayout(new BorderLayout());
    
    JPanel upperPanel = new JPanel();
    upperPanel.setLayout(new GridLayout(4, 1));
    contentPane.add(upperPanel, BorderLayout.NORTH);

    JPanel p1 = new JPanel();
    p1.setLayout(new GridLayout(1, 2));
    upperPanel.add(p1);
    JButton listenForEventsOnObjectIDButton = new JButton("Listen for events on Object ID:");
    listenForEventsOnObjectIDButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataListenerTestComponent.this.listenForEventsOnObjectID();
        }
      });
    p1.add(listenForEventsOnObjectIDButton);
    p1.add(objectIDLabel);

    JPanel p2 = new JPanel();
    p2.setLayout(new GridLayout(1, 2));
    upperPanel.add(p2);
    JButton listenForEventsOnObjectsOfTypeButton = new JButton("Listen for events on Objects of type:");
    listenForEventsOnObjectsOfTypeButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataListenerTestComponent.this.listenForEventsOnObjectsOfType();
        }
      });
    p2.add(listenForEventsOnObjectsOfTypeButton);
    p2.add(objectTypeField);

    JPanel innerUpperPanel = new JPanel();
    innerUpperPanel.setLayout(new GridLayout(1, 5));
    upperPanel.add(innerUpperPanel);
    innerUpperPanel.add(storeCheck);
    innerUpperPanel.add(updateCheck);
    innerUpperPanel.add(objectUpdateCheck);
    innerUpperPanel.add(expirationUpdateCheck);
    innerUpperPanel.add(deleteCheck);

    JButton removeSelectedListenersButton = new JButton("Remove Selected Listeners");
    removeSelectedListenersButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataListenerTestComponent.this.removeSelectedListeners();
        }
      });
    upperPanel.add(removeSelectedListenersButton);

    JPanel lowerPanel = new JPanel();
    lowerPanel.setLayout(new GridLayout(2, 1));
    contentPane.add(lowerPanel, BorderLayout.CENTER);

    model = new ListenerTableModel();
    listenerTable = new JTable(model);
    listenerTable.setCellSelectionEnabled(false);
    listenerTable.setColumnSelectionAllowed(false);
    listenerTable.setRowSelectionAllowed(true);
    lowerPanel.add(new JScrollPane(listenerTable));

    outputArea.setEditable(false);
    lowerPanel.add(new JScrollPane(outputArea));

    dndSupport.addDropTarget(this);
  }
  
  private void listenForEventsOnObjectID()
  {
    if (oid == null)
    {
      return;
    }

    long flags = 0x00L;
    flags = storeCheck.isSelected() ? (flags | NotificationEvent.STORE) : flags;
    flags = updateCheck.isSelected() ? (flags | NotificationEvent.UPDATE) : flags;
    flags = objectUpdateCheck.isSelected() ? (flags | NotificationEvent.OBJECT_UPDATE) : flags;
    flags = expirationUpdateCheck.isSelected() ? (flags | NotificationEvent.EXPIRATION_UPDATE) : flags;
    flags = deleteCheck.isSelected() ? (flags | NotificationEvent.DELETE) : flags;

    NotificationEventListener listener = new NotificationEventListener(outputArea, oid, flags);
    listener.add(osm);

    model.addRow(new Object[] {listener, new Boolean(storeCheck.isSelected()), new Boolean(updateCheck.isSelected()), new Boolean(objectUpdateCheck.isSelected()), new Boolean(expirationUpdateCheck.isSelected()), new Boolean(deleteCheck.isSelected())});
    listenersToRow.add(listener);

  }

  private void listenForEventsOnObjectsOfType()
  {
    String type = objectTypeField.getText();
    
    if (type.length() == 0)
    {
      return;
    }
    
    try
    {
      Class objectType = Class.forName(type);
      
      long flags = 0x00L;
      flags = storeCheck.isSelected() ? (flags | NotificationEvent.STORE) : flags;
      flags = updateCheck.isSelected() ? (flags | NotificationEvent.UPDATE) : flags;
      flags = objectUpdateCheck.isSelected() ? (flags | NotificationEvent.OBJECT_UPDATE) : flags;
      flags = expirationUpdateCheck.isSelected() ? (flags | NotificationEvent.EXPIRATION_UPDATE) : flags;
      flags = deleteCheck.isSelected() ? (flags | NotificationEvent.DELETE) : flags;

      NotificationEventListener listener = new NotificationEventListener(outputArea, objectType, flags);
      listener.add(osm);

      model.addRow(new Object[] {listener, new Boolean(storeCheck.isSelected()), new Boolean(updateCheck.isSelected()), new Boolean(objectUpdateCheck.isSelected()), new Boolean(expirationUpdateCheck.isSelected()), new Boolean(deleteCheck.isSelected())});
      listenersToRow.add(listener);
    }
    catch (ClassNotFoundException e)
    {
      outputArea.append("\n\nRequested class, " + type + ", is an invalid class type.");
    }
  }

  private void removeSelectedListeners()
  {
    int[] rows = listenerTable.getSelectedRows();
    
    NotificationEventListener[] listeners = new NotificationEventListener[rows.length];
    for (int i=0; i<rows.length; i++)
    {
      listeners[i] = (NotificationEventListener)listenerTable.getValueAt(rows[i], 0);
    }
    for (int i=0; i<listeners.length; i++)
    {
      int row = listenersToRow.indexOf(listeners[i]);
      listenersToRow.remove(row);
      model.removeRow(row);
      listeners[i].remove(osm);
    }

    listenerTable.getSelectionModel().clearSelection();
  }
}

class ListenerTableModel extends DefaultTableModel
{
  public ListenerTableModel()
  {
    super(new Object[0][4], new Object[] {"Listener", "S", "U", "OU", "EU", "D"});
  }

  public boolean isCellEditable(int row, int column)
  {
    return(false);
  }
}

class NotificationEventListener implements NotificationListener
{
  private long flags;
  private Class objectType;
  private ObjectID oid;
  private JTextArea outputArea;
  
  private String toString;
  
  public NotificationEventListener(JTextArea outputArea, Class objectType, long flags)
  {
    this.outputArea = outputArea;
    this.objectType = objectType;
    this.flags = flags;
    
    toString = "Class: " + objectType.getName();
  }
  
  public NotificationEventListener(JTextArea outputArea, ObjectID oid, long flags)
  {
    this.outputArea = outputArea;
    this.oid = oid;
    this.flags = flags;

    toString = "Object ID: " + oid.toString();
  }
  
  public void add(ObjectStorageManager osm)
  {
    if (oid != null)
    {
      osm.addNotificationListener(oid, this, flags);
    }
    else
    {
      osm.addNotificationListener(objectType, this, flags);
    }
  }
  
  public void remove(ObjectStorageManager osm)
  {
    if (oid != null)
    {
      osm.removeNotificationListener(oid, this);
    }
    else
    {
      osm.removeNotificationListener(objectType, this);
    }
  }
  
  public void notify(NotificationEvent event)
  {
    if ((event.getNotificationType() & flags) == 0x00L)
    {
      throw(new RuntimeException("Notified when not registered for event: Registered: " + flags + " Event: " + event));
    }
    
    if (event.isStore())
    {
      outputArea.append("\n" + this + ": Store Event: " + event);
    }

    if (event.isUpdate())
    {
      outputArea.append("\n" + this + ": Udate Event: " + event);
    }

    if (event.isObjectUpdate())
    {
      outputArea.append("\n" + this + ": Object Update Event: " + event);
    }

    if (event.isExpirationUpdate())
    {
      outputArea.append("\n" + this + ": Expiration Update Event: " + event);
    }

    if (event.isDelete())
    {
      outputArea.append("\n" + this + ": Delete Event: " + event);
    }
  }

  public String toString()
  {
    return(toString);
  }
}
