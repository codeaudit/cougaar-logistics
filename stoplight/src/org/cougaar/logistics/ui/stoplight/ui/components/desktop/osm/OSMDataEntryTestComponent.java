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
import java.awt.event.*;
import java.util.*;
import java.io.Serializable;

import javax.swing.*;
import javax.swing.table.*;

import org.cougaar.logistics.ui.stoplight.ui.components.desktop.*;
import org.cougaar.logistics.ui.stoplight.ui.components.desktop.dnd.*;

/***********************************************************************************************************************
<b>Description</b>: This class is a test Cougaar Desktop component for the Object Storage Manager (OSM) capabilities in
                    the Cougaar Desktop application.  This component tests the data entry/changing/deletion
                    capabilities of the OSM.

***********************************************************************************************************************/
public class OSMDataEntryTestComponent extends ComponentFactory implements CougaarDesktopUI, NotificationListener, DragSource
{
	public String getToolDisplayName()
	{
	  return("OSM Data Entry Test Component");
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

      longUpdateDialog = new LongUpdateDialog(f.getDesktop());
      doubleUpdateDialog = new DoubleUpdateDialog(f.getDesktop());
      stringUpdateDialog = new StringUpdateDialog(f.getDesktop());
      classUpdateDialog = new ClassUpdateDialog(f.getDesktop());
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
    return("OSM Data Entry Test Component");
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

  // ------------------- DragSource Interface ----------------------------  

  public Vector getSourceComponents()
  {
    Vector components = new Vector(1);
    components.add(objectTable);
    
    return(components);
  }

  public boolean dragFromSubComponents()
  {
    return(true);
  }

  public Object getData(Component componentAt, Point location)
  {
    int row = objectTable.rowAtPoint(location);
    int col = objectTable.columnAtPoint(location);
    
    if (row != -1 && col != -1)
    {
      return((ObjectID)objectTable.getValueAt(row, 0));
    }

    return(null);
  }

  public void dragDropEnd(boolean success)
  {
  }


  private JTable objectTable;
  private ObjectTableModel model;

  private JTextField longField = new JTextField();
  private JTextField doubleField = new JTextField();
  private JTextField stringField = new JTextField();
  private JTextField classField = new JTextField();

  private JTextField expireField = new JTextField();

  private Vector oidToTableRow = new Vector(0);

  private ObjectStorageManager osm;

  private LongUpdateDialog longUpdateDialog;
  private DoubleUpdateDialog doubleUpdateDialog;
  private StringUpdateDialog stringUpdateDialog;
  private ClassUpdateDialog classUpdateDialog;

  private void setUp(Container contentPane)
  {
    contentPane.setLayout(new BorderLayout());
    
    JPanel upperPanel = new JPanel();
    upperPanel.setLayout(new BorderLayout());
    contentPane.add(upperPanel, BorderLayout.NORTH);

    JPanel fieldPanel = new JPanel();
    fieldPanel.setLayout(new GridLayout(3, 4));
    upperPanel.add(fieldPanel, BorderLayout.CENTER);

    fieldPanel.add(new JLabel("Long Value:"));
    fieldPanel.add(longField);
    fieldPanel.add(new JLabel("Double Value:"));
    fieldPanel.add(doubleField);
    fieldPanel.add(new JLabel("String Value:"));
    fieldPanel.add(stringField);
    fieldPanel.add(new JLabel("Class Name:"));
    fieldPanel.add(classField);

    fieldPanel.add(new JLabel(""));
    fieldPanel.add(new JLabel("Expiration:"));
    fieldPanel.add(expireField);
    fieldPanel.add(new JLabel(""));

    JButton storeButton = new JButton("Store Object");
    storeButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataEntryTestComponent.this.storeObject();
        }
      });
    upperPanel.add(storeButton, BorderLayout.SOUTH);

    JPanel lowerPanel = new JPanel();
    lowerPanel.setLayout(new BorderLayout());
    contentPane.add(lowerPanel, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(2, 1));
    lowerPanel.add(buttonPanel, BorderLayout.NORTH);

    JButton updateButton = new JButton("Update Selected Objects");
    updateButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataEntryTestComponent.this.updateSelectedObjects();
        }
      });
    buttonPanel.add(updateButton);

    JButton deleteButton = new JButton("Delete Selected Objects");
    deleteButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          OSMDataEntryTestComponent.this.deleteSelectedObjects();
        }
      });
    buttonPanel.add(deleteButton);
    
    model = new ObjectTableModel();
    objectTable = new JTable(model);
    objectTable.setCellSelectionEnabled(false);
    objectTable.setColumnSelectionAllowed(false);
    objectTable.setRowSelectionAllowed(true);
    lowerPanel.add(new JScrollPane(objectTable), BorderLayout.CENTER);

    dndSupport.addDragSource(this);
    osm.addNotificationListener(Object.class, this);
  }

  public void storeObject()
  {
    boolean expireSet = false;
    long expireTime = 0;
    String expire = expireField.getText();
    expireField.setText("");
    if (expire.length() > 0)
    {
      try
      {
        expireTime = Long.parseLong(expire);
        expireSet = true;
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }

    String text;
    
    text = longField.getText();
    longField.setText("");
    if (text.length() > 0)
    {
      try
      {
        Long l = new Long(Long.parseLong(text));
        if (expireSet)
        {
          osm.storeCommit(l, expireTime);
        }
        else
        {
          osm.storeCommit(l);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }

    text = doubleField.getText();
    doubleField.setText("");
    if (text.length() > 0)
    {
      try
      {
        Double d = new Double(Double.parseDouble(text));
        if (expireSet)
        {
          osm.storeCommit(d, expireTime);
        }
        else
        {
          osm.storeCommit(d);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }

    text = stringField.getText();
    stringField.setText("");
    if (text.length() > 0)
    {
      try
      {
        if (expireSet)
        {
          osm.storeCommit(text, expireTime);
        }
        else
        {
          osm.storeCommit(text);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }

    text = classField.getText();
    classField.setText("");
    if (text.length() > 0)
    {
      try
      {
        Object o = Class.forName(text).newInstance();
        
        if (expireSet)
        {
          osm.storeCommit(o, expireTime);
        }
        else
        {
          osm.storeCommit(o);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }

  public void deleteSelectedObjects()
  {
    int[] rows = objectTable.getSelectedRows();
    
    ObjectID[] ids = new ObjectID[rows.length];
    for (int i=0; i<rows.length; i++)
    {
      ids[i] = (ObjectID)objectTable.getValueAt(rows[i], 0);
    }
    for (int i=0; i<ids.length; i++)
    {
      osm.deleteCommit(ids[i]);
    }

    objectTable.getSelectionModel().clearSelection();
  }

  public void updateSelectedObjects()
  {
    int[] rows = objectTable.getSelectedRows();
    
    ObjectID[] ids = new ObjectID[rows.length];
    for (int i=0; i<rows.length; i++)
    {
      ids[i] = (ObjectID)objectTable.getValueAt(rows[i], 0);
    }
    for (int i=0; i<ids.length; i++)
    {
      Object obj = objectTable.getValueAt(rows[i], 2);
      
      if (obj.getClass() == Long.class)
      {
        longUpdateDialog.show(obj.getClass() + ": " + ids[i]);
        if (longUpdateDialog.canceled == false)
        {
          boolean useExpire = false;
          long expire = 0;
          try
          {
            expire = Long.parseLong(longUpdateDialog.expirationField.getText());
            useExpire = true;
          }
          catch (Exception e)
          {
          }
          
          if (longUpdateDialog.markAsUpdatedCheck.isSelected())
          {
            if (useExpire)
            {
              osm.updateCommit(ids[i]);
              osm.updateCommit(ids[i], expire);
            }
            else
            {
              osm.updateCommit(ids[i]);
            }
          }
          else
          {
            try
            {
              long value = Long.parseLong(longUpdateDialog.valueField.getText());
              if (useExpire)
              {
                osm.updateCommit(ids[i], new Long(value), expire);
              }
              else
              {
                osm.updateCommit(ids[i], new Long(value));
              }
            }
            catch (Exception e)
            {
              if (useExpire)
              {
                osm.updateCommit(ids[i], expire);
              }
            }
          }
        }
      }
      else if (obj.getClass() == Double.class)
      {
        doubleUpdateDialog.show(obj.getClass() + ": " + ids[i]);
        if (doubleUpdateDialog.canceled == false)
        {
          boolean useExpire = false;
          long expire = 0;
          try
          {
            expire = Long.parseLong(doubleUpdateDialog.expirationField.getText());
            useExpire = true;
          }
          catch (Exception e)
          {
          }
          
          if (doubleUpdateDialog.markAsUpdatedCheck.isSelected())
          {
            if (useExpire)
            {
              osm.updateCommit(ids[i]);
              osm.updateCommit(ids[i], expire);
            }
            else
            {
              osm.updateCommit(ids[i]);
            }
          }
          else
          {
            try
            {
              double value = Double.parseDouble(doubleUpdateDialog.valueField.getText());
              if (useExpire)
              {
                osm.updateCommit(ids[i], new Double(value), expire);
              }
              else
              {
                osm.updateCommit(ids[i], new Double(value));
              }
            }
            catch (Exception e)
            {
              if (useExpire)
              {
                osm.updateCommit(ids[i], expire);
              }
            }
          }
        }
      }
      else if (obj.getClass() == String.class)
      {
        stringUpdateDialog.show(obj.getClass() + ": " + ids[i]);
        if (stringUpdateDialog.canceled == false)
        {
          boolean useExpire = false;
          long expire = 0;
          try
          {
            expire = Long.parseLong(stringUpdateDialog.expirationField.getText());
            useExpire = true;
          }
          catch (Exception e)
          {
          }
          
          if (stringUpdateDialog.markAsUpdatedCheck.isSelected())
          {
            if (useExpire)
            {
              osm.updateCommit(ids[i]);
              osm.updateCommit(ids[i], expire);
            }
            else
            {
              osm.updateCommit(ids[i]);
            }
          }
          else
          {
            String value = stringUpdateDialog.valueField.getText();
            if (value.length() > 0)
            {
              if (useExpire)
              {
                osm.updateCommit(ids[i], value, expire);
              }
              else
              {
                osm.updateCommit(ids[i], value);
              }
            }
            else if (useExpire)
            {
              osm.updateCommit(ids[i], expire);
            }
          }
        }
      }
      else
      {
        classUpdateDialog.show(obj.getClass() + ": " + ids[i]);
        if (classUpdateDialog.canceled == false)
        {
          boolean useExpire = false;
          long expire = 0;
          try
          {
            expire = Long.parseLong(classUpdateDialog.expirationField.getText());
            useExpire = true;
          }
          catch (Exception e)
          {
          }
          
          if (classUpdateDialog.markAsUpdatedCheck.isSelected())
          {
            if (useExpire)
            {
              osm.updateCommit(ids[i]);
              osm.updateCommit(ids[i], expire);
            }
            else
            {
              osm.updateCommit(ids[i]);
            }
          }
          else if (classUpdateDialog.updateWithNewCheck.isSelected())
          {
            try
            {
              Object value = obj.getClass().newInstance();
              if (useExpire)
              {
                osm.updateCommit(ids[i], value, expire);
              }
              else
              {
                osm.updateCommit(ids[i], value);
              }
            }
            catch (Exception e)
            {
            }
          }
          else if (useExpire)
          {
            osm.updateCommit(ids[i], expire);
          }
        }
      }
    }

    objectTable.getSelectionModel().clearSelection();
  }

  public void notify(NotificationEvent event)
  {
    if (event.isStore())
    {
      System.out.println("Store: " + event);
      model.addRow(new Object[] {event.getObjectID(), event.getSource().getClass(), event.getSource(), (event.isExpirationSet() ? (new Date(event.getExpiration())).toString() : "(None)")});
      oidToTableRow.add(event.getObjectID());
    }

    if (event.isUpdate())
    {
      System.out.println("Update: " + event);
      int row = oidToTableRow.indexOf(event.getObjectID());
//      model.addRow(new Object[] {event.getObjectID(), event.getSource().getClass(), event.getSource(), (event.isExpirationSet() ? (new Date(event.getExpiration())).toString() : "(None)")});
      model.setValueAt(event.getSource(), row, 2);
    }

    if (event.isObjectUpdate())
    {
      System.out.println("Object Update: " + event);
      int row = oidToTableRow.indexOf(event.getObjectID());
//      model.addRow(new Object[] {event.getObjectID(), event.getSource().getClass(), event.getSource(), (event.isExpirationSet() ? (new Date(event.getExpiration())).toString() : "(None)")});
      model.setValueAt(event.getSource(), row, 2);
    }

    if (event.isExpirationUpdate())
    {
      System.out.println("Expiration Update: " + event);
      int row = oidToTableRow.indexOf(event.getObjectID());
//      model.addRow(new Object[] {event.getObjectID(), event.getSource().getClass(), event.getSource(), (event.isExpirationSet() ? (new Date(event.getExpiration())).toString() : "(None)")});
      model.setValueAt((new Date(event.getExpiration())).toString(), row, 3);
    }

    if (event.isDelete())
    {
      System.out.println("Delete: " + event);
      int row = oidToTableRow.indexOf(event.getObjectID());
      oidToTableRow.remove(row);
      model.removeRow(row);
    }
  }
}

class ObjectTableModel extends DefaultTableModel
{
  public ObjectTableModel()
  {
    super(new Object[0][4], new Object[] {"Object ID", "Object Type", "Object", "Expiration"});
  }

  public boolean isCellEditable(int row, int column)
  {
    return(false);
  }
}

class LongUpdateDialog extends JDialog
{
  public JCheckBox markAsUpdatedCheck = new JCheckBox("Mark As Updated", false);
  public JTextField valueField = new JTextField();
  public JTextField expirationField = new JTextField();

  public boolean canceled = true;
  
  public LongUpdateDialog(Frame frame)
  {
    super(frame, true);
    
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridLayout(4,1));
    
    markAsUpdatedCheck.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          valueField.setEditable(!markAsUpdatedCheck.isSelected());
          valueField.setText("");
        }
      });
    contentPane.add(markAsUpdatedCheck);

    JPanel valuePanel = new JPanel();
    valuePanel.setLayout(new GridLayout(1,2));
    contentPane.add(valuePanel);
    valuePanel.add(new JLabel("New Long Object Value:"));
    valuePanel.add(valueField);
    
    JPanel expPanel = new JPanel();
    expPanel.setLayout(new GridLayout(1,2));
    contentPane.add(expPanel);
    expPanel.add(new JLabel("New Expiration:"));
    expPanel.add(expirationField);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(1,2));
    contentPane.add(buttonPanel);
    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          LongUpdateDialog.this.canceled = false;
          LongUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(updateButton);
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          LongUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(cancelButton);
    
    setSize(300, 150);
  }

  public void show(String title)
  {
    setTitle(title);
    
    markAsUpdatedCheck.setSelected(false);
    valueField.setText("");
    valueField.setEditable(true);
    expirationField.setText("");
    canceled = true;

    super.show();
  }
}

class DoubleUpdateDialog extends JDialog
{
  public JCheckBox markAsUpdatedCheck = new JCheckBox("Mark As Updated", false);
  public JTextField valueField = new JTextField();
  public JTextField expirationField = new JTextField();
  
  public boolean canceled = true;

  public DoubleUpdateDialog(Frame frame)
  {
    super(frame, true);
    
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridLayout(4,1));
    
    markAsUpdatedCheck.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          DoubleUpdateDialog.this.valueField.setEditable(!markAsUpdatedCheck.isSelected());
          DoubleUpdateDialog.this.valueField.setText("");
        }
      });
    contentPane.add(markAsUpdatedCheck);

    JPanel valuePanel = new JPanel();
    valuePanel.setLayout(new GridLayout(1,2));
    contentPane.add(valuePanel);
    valuePanel.add(new JLabel("New Double Object Value:"));
    valuePanel.add(valueField);
    
    JPanel expPanel = new JPanel();
    expPanel.setLayout(new GridLayout(1,2));
    contentPane.add(expPanel);
    expPanel.add(new JLabel("New Expiration:"));
    expPanel.add(expirationField);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(1,2));
    contentPane.add(buttonPanel);
    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          DoubleUpdateDialog.this.canceled = false;
          DoubleUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(updateButton);
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          DoubleUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(cancelButton);
    
    setSize(300, 150);
  }

  public void show(String title)
  {
    setTitle(title);

    markAsUpdatedCheck.setSelected(false);
    valueField.setText("");
    valueField.setEditable(true);
    expirationField.setText("");
    canceled = true;

    super.show();
  }
}

class StringUpdateDialog extends JDialog
{
  public JCheckBox markAsUpdatedCheck = new JCheckBox("Mark As Updated", false);
  public JTextField valueField = new JTextField();
  public JTextField expirationField = new JTextField();
  
  public boolean canceled = true;

  public StringUpdateDialog(Frame frame)
  {
    super(frame, true);
    
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridLayout(4,1));
    
    markAsUpdatedCheck.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          StringUpdateDialog.this.valueField.setEditable(!markAsUpdatedCheck.isSelected());
          StringUpdateDialog.this.valueField.setText("");
        }
      });
    contentPane.add(markAsUpdatedCheck);

    JPanel valuePanel = new JPanel();
    valuePanel.setLayout(new GridLayout(1,2));
    contentPane.add(valuePanel);
    valuePanel.add(new JLabel("New String Object Value:"));
    valuePanel.add(valueField);
    
    JPanel expPanel = new JPanel();
    expPanel.setLayout(new GridLayout(1,2));
    contentPane.add(expPanel);
    expPanel.add(new JLabel("New Expiration:"));
    expPanel.add(expirationField);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(1,2));
    contentPane.add(buttonPanel);
    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          StringUpdateDialog.this.canceled = false;
          StringUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(updateButton);
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          StringUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(cancelButton);
    
    setSize(300, 150);
  }

  public void show(String title)
  {
    setTitle(title);

    markAsUpdatedCheck.setSelected(false);
    valueField.setText("");
    valueField.setEditable(true);
    expirationField.setText("");
    canceled = true;

    super.show();
  }
}

class ClassUpdateDialog extends JDialog
{
  public JRadioButton markAsUpdatedCheck = new JRadioButton("Mark As Updated", true);
  public JRadioButton updateWithNewCheck = new JRadioButton("Update With New Instance", false);
  public JRadioButton updateOnlyExpireCheck = new JRadioButton("Update Only Expire", false);
  public JTextField expirationField = new JTextField();
  
  public boolean canceled = true;

  public ClassUpdateDialog(Frame frame)
  {
    super(frame, true);
    
    ButtonGroup bg = new ButtonGroup();
    bg.add(markAsUpdatedCheck);
    bg.add(updateWithNewCheck);
    bg.add(updateOnlyExpireCheck);
    
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridLayout(5,1));
    
    contentPane.add(markAsUpdatedCheck);
    contentPane.add(updateWithNewCheck);
    contentPane.add(updateOnlyExpireCheck);

    JPanel expPanel = new JPanel();
    expPanel.setLayout(new GridLayout(1,2));
    contentPane.add(expPanel);
    expPanel.add(new JLabel("New Expiration:"));
    expPanel.add(expirationField);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(1,2));
    contentPane.add(buttonPanel);
    JButton updateButton = new JButton("Update");
    updateButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          ClassUpdateDialog.this.canceled = false;
          ClassUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(updateButton);
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          ClassUpdateDialog.this.hide();
        }
      });
    buttonPanel.add(cancelButton);
    
    setSize(300, 175);
  }

  public void show(String title)
  {
    setTitle(title);

    markAsUpdatedCheck.setSelected(true);
    updateWithNewCheck.setSelected(false);
    updateOnlyExpireCheck.setSelected(false);
    expirationField.setText("");
    canceled = true;

    super.show();
  }
}
