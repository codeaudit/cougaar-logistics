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
package org.cougaar.logistics.ui.stoplight.ui.map.layer;

import java.util.*;
import com.bbn.openmap.Layer;
import com.bbn.openmap.event.*;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.proj.Projection;

import java.awt.event.*;
import java.awt.*;
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.MapBean;
import com.bbn.openmap.proj.Projection;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.logistics.ui.stoplight.ui.map.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

class IconDialog extends JDialog implements TableModelListener, ListSelectionListener, ActionListener
{
  protected static IconDialog iconDialog = null;
  protected static PspIconLayer ownerIconLayer = null;

  public static IconDialog getDialog(PspIconLayer owner)
  {
    if ((ownerIconLayer != owner) || (iconDialog == null))
    {
      ownerIconLayer = owner;
      Container parent = ownerIconLayer.getParent();
      do
      {
        if ((parent == null) || (parent instanceof Frame))
        {
          break;
        }
      }
      while ((parent = parent.getParent()) != null);

      iconDialog = new IconDialog((Frame)parent);
    }
    
    return(iconDialog);
  }

  protected ListSelectionModel selectionModel = null;
  protected DefaultTableModel tableModel = null;
  protected JTable tableList = null;

  protected Vector namedLocationTimeList = null;
  protected Hashtable namedLocationTimeHash = null;

  private AssetBarGraphic abg = new AssetBarGraphic(null, null, "");

  private IconDialog(Frame frame)
  {
    super(frame);

    abg.setVisible(false);

    tableModel = new IconTableModel(); 
    tableList = new CustomEditorJTable(tableModel); 
    tableList.setColumnSelectionAllowed(false);
    tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    selectionModel = tableList.getSelectionModel();


    // Col 0
    tableModel.addColumn("Unit");
    // Col 1
    tableModel.addColumn("Visible");
    // Col 2
    tableModel.addColumn("Show Subordinates");
    // Col 3
    tableModel.addColumn("Asset Inventory");

    GraphicRenderer renderer = new GraphicRenderer();
    tableList.setDefaultRenderer(NamedLocationTime.class, renderer);
    tableList.setDefaultRenderer(AssetBarGraphic.class, renderer);

    selectionModel.addListSelectionListener(this);
    tableModel.addTableModelListener(this);


    setModal(true);
    setSize(500, 500);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(new JScrollPane(tableList), BorderLayout.CENTER);
    
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(2,2));

    JButton button = new JButton("Show All Units");
    button.setActionCommand("0");
    button.addActionListener(this);
    panel.add(button);

    button = new JButton("Hide All Units");
    button.setActionCommand("1");
    button.addActionListener(this);
    panel.add(button);

    button = new JButton("Show All Assets");
    button.setActionCommand("2");
    button.addActionListener(this);
    panel.add(button);

    button = new JButton("Hide All Assets");
    button.setActionCommand("3");
    button.addActionListener(this);
    panel.add(button);

    getContentPane().add(panel, BorderLayout.SOUTH);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (namedLocationTimeList == null)
    {
      return;
    }

    Boolean allVisibility = new Boolean(false);
    NamedLocationTime nltm = null;
    switch (Integer.parseInt(e.getActionCommand()))
    {
      case 0:
        allVisibility = new Boolean(true);
      case 1:
        for (int i=0, isize=namedLocationTimeList.size(); i<isize; i++)
        {
          updateVisibility(i, allVisibility);
    
          tableModel.removeTableModelListener(this);
          tableModel.setValueAt(new Boolean(false), i, 2);
          tableModel.addTableModelListener(this);
        }
      break;

      case 2:
        allVisibility = new Boolean(true);
      case 3:
        for (int i=0, isize=namedLocationTimeList.size(); i<isize; i++)
        {

          AssetBarGraphic assetG = updateAssetVisibility(i, allVisibility.booleanValue());
          
          tableModel.removeTableModelListener(this);
          tableModel.setValueAt(assetG, i, 3);
          tableModel.addTableModelListener(this);
        }
      break;
    }

    ownerIconLayer.repaint();
  }

  private static final Comparator ntlComparator = new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return(((NamedLocationTime)o1).getUnit().getLabel().compareTo(((NamedLocationTime)o2).getUnit().getLabel()));
      }
    };

  public void setTableData(Vector namedLocationTimeList)
  {
    Collections.sort(namedLocationTimeList, ntlComparator);
    
    namedLocationTimeHash = new Hashtable(namedLocationTimeList.size()+1);
    this.namedLocationTimeList = namedLocationTimeList;
    tableModel.setNumRows(0);
    NamedLocationTime nltm = null;
    for (int i=0, isize=namedLocationTimeList.size(); i<isize; i++)
    {
      nltm = (NamedLocationTime)namedLocationTimeList.elementAt(i);
//      tableModel.addRow(new Object[] {nltm, new Boolean(nltm.getUnit().getGraphic().isVisible()), new Boolean(false), new Boolean(((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic.isVisible())});
      AssetBarGraphic assetG = null;
      if (((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic != null)
      {
        assetG = ((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic;
      }
      else
      {
        assetG = abg;
      }

      tableModel.addRow(new Object[] {nltm, new Boolean(nltm.getUnit().getGraphic().isVisible()), new Boolean(false), assetG});
      
      namedLocationTimeHash.put(nltm.getUnit().getLabel(), nltm);
    }

//    selectionModel.setSelectionInterval(0, 0);
  }

  public void valueChanged(ListSelectionEvent e)
  {
    if ((!e.getValueIsAdjusting()) && (!selectionModel.isSelectionEmpty()))
    {
      int selectedRow = selectionModel.getMinSelectionIndex();
      NamedLocationTime nltm = (NamedLocationTime)tableModel.getValueAt(selectedRow, 0);
      ownerIconLayer.graphics.moveIndexedToTop(ownerIconLayer.graphics.indexOf(nltm.getUnit().getGraphic()));
//      ownerIconLayer.myState.curTimeMarkers.moveIndexedToTop(ownerIconLayer.myState.curTimeMarkers.indexOf(nltm.getUnit().getGraphic()));
      ownerIconLayer.myState.moveIconToTop(nltm.getUnit());
      ownerIconLayer.repaint();
    }
  }

  public void tableChanged(TableModelEvent e)
  {
    if (e.getType() == TableModelEvent.UPDATE)
    {
      int index = e.getFirstRow();
      NamedLocationTime nltm = null;

      switch(e.getColumn())
      {
        case 1:  // Visible
          updateVisibility(index, (Boolean)tableModel.getValueAt(index, 1));
        break;

        case 2:  // Show Subordinates
          nltm = (NamedLocationTime)tableModel.getValueAt(index, 0);
          Vector subordinates = ownerIconLayer.getRelationships(nltm.getUnit().getLabel() );
//          System.out.println("subordinates: " + subordinates);
          for (int i=0, isize=subordinates.size(); i<isize; i++)
          {
            if (((String)subordinates.elementAt(i)).toUpperCase().indexOf("RED-CROSS-HQ") == -1)
            {
              updateVisibility(namedLocationTimeList.indexOf(namedLocationTimeHash.get(subordinates.elementAt(i))), (Boolean)tableModel.getValueAt(index, 2));
            }
          }
        break;

        case 3:  // Show/Type Asset
          String value = (String)tableModel.getValueAt(index, 3);
          boolean visible = true;
          if (value.length() == 0)
          {
            visible = false;
          }

          AssetBarGraphic assetG = updateAssetVisibility(index, visible);
          assetG.setCurrentAsset(value);
          
          tableModel.removeTableModelListener(this);
          tableModel.setValueAt(assetG, index, 3);
          tableModel.addTableModelListener(this);
        break;
      }

      ownerIconLayer.repaint();
    }
  }

  private AssetBarGraphic updateAssetVisibility(int index, boolean visible)
  {
    NamedLocationTime nltm = (NamedLocationTime)tableModel.getValueAt(index, 0);
    AssetBarGraphic assetBarGraphic = ((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic;
    if (assetBarGraphic != null)
    {
      ((VecIcon)nltm.getUnit().getGraphic()).assetBarGraphic.setVisible(visible);
    }
    else
    {
      assetBarGraphic = abg;
    }
    
    return(assetBarGraphic);
  }

  private void updateVisibility(int index, Boolean visible)
  {
    if (index < 0)
    {
      return;
    }

    tableModel.removeTableModelListener(this);

    NamedLocationTime nltm = (NamedLocationTime)tableModel.getValueAt(index, 0);

    nltm.getUnit().getGraphic().setVisible(visible.booleanValue());
    ownerIconLayer.myState.setVisibleForAllNamedLocations(nltm.getUnit().getLabel(), visible.booleanValue());
    tableModel.setValueAt(visible, index, 1);
    nltm.getUnit().getGraphic().setNeedToRegenerate(true);

    tableModel.addTableModelListener(this);
  }

  class CustomEditorJTable extends JTable
  {
    private JComboBox comboBox = new JComboBox();
    
    public CustomEditorJTable(TableModel model)
    {
      super(model);
    }

    public TableCellEditor getCellEditor(int row, int column)
    {
      if (getValueAt(row, column) instanceof AssetBarGraphic)
      {
        AssetBarGraphic assetG = (AssetBarGraphic)getValueAt(row, column);
        comboBox.removeAllItems();
        comboBox.addItem("");
        comboBox.setSelectedIndex(0);

        for (Enumeration e=assetG.getAssetNameList(); e.hasMoreElements();)
        {
          comboBox.addItem((String)e.nextElement());
        }

        if (assetG.isVisible())
        {
          String assetName = assetG.getCurrentAsset();
          for (int i=1, isize=comboBox.getItemCount(); i<isize; i++)
          {
            if (comboBox.getItemAt(i).equals(assetName))
            {
              comboBox.setSelectedIndex(i);
              break;
            }
          }
        }

        return(new DefaultCellEditor(comboBox));
      }

      return(super.getCellEditor(row, column));
    }
  }

  class IconTableModel extends DefaultTableModel
  {
    public Class getColumnClass(int c)
    {
      return(getValueAt(0, c).getClass());
    }

    public boolean isCellEditable(int row, int col)
    {
      Object value = getValueAt(row, col);
      if (value instanceof NamedLocationTime)
      {
        return(false);
      }

      return(true);
    }
  }

  class GraphicRenderer implements TableCellRenderer
  {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      String text = null;
      if (value instanceof NamedLocationTime)
      {
        NamedLocationTime nltm = (NamedLocationTime)value;
        text = nltm.getUnit().getLabel();
      }
      else if (value instanceof AssetBarGraphic)
      {
        AssetBarGraphic assetG = ((AssetBarGraphic)value);
        text = (assetG.isVisible()) ? assetG.getCurrentAsset() : "";
        text = (assetG.getDataSets().size() > 0) ? text : "No Assets";
      }

      JLabel label = new JLabel(text);
      label.setFont(table.getFont());
      label.setOpaque(true);

      if (isSelected)
      {
        label.setBackground(table.getSelectionBackground());
        label.setForeground(table.getSelectionForeground());
      }
      else
      {
        label.setBackground(table.getBackground());
        label.setForeground(table.getForeground());
      }

      return(label);
    }
  }
}
