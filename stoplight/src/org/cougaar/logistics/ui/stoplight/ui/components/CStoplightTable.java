/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.components;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.text.DecimalFormat;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.cougaar.logistics.ui.stoplight.ui.models.DatabaseTableModel;
import org.cougaar.logistics.ui.stoplight.ui.models.StoplightThresholdModel;

/**
 * Stoplight table bean.  Data cells of table are color coded based on the
 * value in the cell and the threshold values set in the bean.  Either the
 * MThumbSliderThresholdControl or the SliderThresholdControl can be used to
 * dynamically set the color thresholds of this bean.
 */
public class CStoplightTable extends CRowHeaderTable
{
    private StoplightThresholdModel thresholds = new StoplightThresholdModel();
    private boolean showColor = true;
    private boolean showValue = true;
    private StoplightCellRenderer stoplightCellRenderer;

    /**
     * Default constructor.  Creates a stoplight chart with a table model
     * that is a 10 by 10 matrix of random data.
     */
    public CStoplightTable()
    {
        super();

        // without a given table model, just provide random data
        // This is just for bean testing.
        // In the future, no model will be default
        Float[][] data = new Float[10][10];
        String[] columnNames = new String[10];
        Random rand = new Random();
        for (int row = 0; row < data.length; row++)
        {
            columnNames[row] = String.valueOf(row);
            for (int column=0; column < data[row].length; column++)
            {
                data[row][column] = new Float(rand.nextFloat() * 2);
            }
        }
        setModel(new DefaultTableModel(data, columnNames));

        init();
    }

    /**
     * Creates a stoplight chart that gets it's data from the given table
     * model
     *
     * @param tableModel the model to use as data source
     */
    public CStoplightTable(TableModel tableModel)
    {
        super(tableModel);
        init();
    }

    /**
     * Initilize stoplight chart
     */
    private void init()
    {
        stoplightCellRenderer = new StoplightCellRenderer();
        setRowSelectionAllowed(false);
    }

    /**
     * Returns the cell renderer to use for the given table cell.
     *
     * @param row cell row index
     * @param column cell column index
     */
    public TableCellRenderer getCellRenderer(int row, int column)
    {
        if (row >= rowStart)
        {
            return stoplightCellRenderer;
        }

        return super.getCellRenderer(row, column);
    }

    /**
     * Set the selected thresholds.
     *
     * @param thresholds new thresholds
     */
    public void setThresholds(StoplightThresholdModel thresholds)
    {
        this.thresholds = thresholds;
        threadedTableUpdate();
    }

    /**
     * Get the selected thresholds
     *
     * @return currently selected thresholds
     */
    public StoplightThresholdModel getThresholds()
    {
        return thresholds;
    }

    /**
     * Set whether color coded is activated.
     *
     * @param showColor boolean telling whether color coding is activated.
     */
    public void setShowColor(boolean showColor)
    {
        this.showColor = showColor;
    }

    /**
     * Set whether data values are displayed.
     *
     * @param showValue boolean telling whether data values are displayed.
     */
    public void setShowValue(boolean showValue)
    {
        this.showValue = showValue;
    }

    /**
     * Associates a control to this table that is used to select features of
     * this table.
     *
     * @param vfc control to associate with table
     */
    public void
        setViewFeatureSelectionControl(final CViewFeatureSelectionControl vfc)
    {
        final int defaultCellWidth = getMinCellWidth();

        vfc.addPropertyChangeListener("mode",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    String newValue = e.getNewValue().toString();
                    if (newValue.equals(CViewFeatureSelectionControl.COLOR))
                    {
                        setShowColor(true);
                        setShowValue(false);
                    }
                    else if (newValue.
                        equals(CViewFeatureSelectionControl.VALUE))
                    {
                        setShowColor(false);
                        setShowValue(true);
                    }
                    else if (newValue.
                        equals(CViewFeatureSelectionControl.BOTH))
                    {
                        setShowColor(true);
                        setShowValue(true);
                    }

                    threadedTableUpdate();
                }
            });

        vfc.addPropertyChangeListener("fitHorizontally",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    if (((Boolean)e.getNewValue()).booleanValue())
                    {
                        setMinCellWidth(0);
                    }
                    else
                    {
                        setMinCellWidth(defaultCellWidth);
                    }
                }
            });

        vfc.addPropertyChangeListener("fitVertically",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    setFitRowHeight(((Boolean)e.getNewValue()).booleanValue());
                }
            });
    }

    /**
     * I'm not sure that this helps (need to find a way to reduce number of
     * tableChanged events to a minimum)
     */
    private void threadedTableUpdate()
    {
        (new Thread() {
                public void run()
                {
                    tableChanged(new TableModelEvent(getModel()));
                }
            }).start();
    }

    /**
     * This renderer is used to color code the data cells.
     */
    private static final Color naGrey = new Color(230, 230, 230);
    private static final DecimalFormat valueFormat =
        new DecimalFormat("####.##");
    private class StoplightCellRenderer extends DefaultTableCellRenderer
    {
        private JTable table;
        private Object value;
        private int row;
        private int column;

        public StoplightCellRenderer()
        {
            super();
            setHorizontalAlignment(JLabel.CENTER);
        }

        public String getToolTipText()
        {
            // Create tooltip for each data cell to describe the row, column,
            // and value of that cell.
            // When jdk1.3 is used, I might make this a multiline tooltip
            // using html
            StringBuffer toolTipText = new StringBuffer("(");
            toolTipText.append(table.getColumnName(column));
            toolTipText.append(", ");
            toolTipText.append(table.getModel().getValueAt(row, 0));
            toolTipText.append(")");

            String valueString = value.toString();
            if (!valueString.equals(DatabaseTableModel.NO_VALUE))
            {
                toolTipText.append(": ");
                toolTipText.append(valueString);
            }

            return toolTipText.toString();
        }

        public Component
            getTableCellRendererComponent(JTable table, Object value,
                                      boolean isSelected, boolean hasFocus,
                                      int row, int column)
        {
            this.table = table;
            this.value = value;
            this.row = row;
            this.column = column;

            colorRenderer(value);
            StoplightCellRenderer.this.setFont(table.getFont());
            if ((showValue) && (value instanceof Number))
            {
                setText(valueFormat.format(value));
            }
            else
            {
                setText(null);
            }

            return this;
        }

        private void colorRenderer(Object value)
        {
            if (showColor)
            {
                // enforce black fonts
                // (otherwise L&F themes could make unreadable)
                if (StoplightCellRenderer.this.getForeground() != Color.black)
                {
                    StoplightCellRenderer.this.setForeground(Color.black);
                }

                if (value instanceof Number)
                {
                    Comparable compValue = (Comparable)value;
                    Float greenMin = new Float(thresholds.getGreenMin());
                    Float greenMax = new Float(thresholds.getGreenMax());
                    Float yellowMin = new Float(thresholds.getYellowMin());
                    Float yellowMax = new Float(thresholds.getYellowMax());

                    if ((compValue.compareTo(greenMin) >= 0) &&
                        (compValue.compareTo(greenMax) <= 0))
                    {
                        if (StoplightCellRenderer.this.getBackground() != Color.green)
                        {
                            StoplightCellRenderer.this.setBackground(Color.green);
                        }
                    }
                    else if ((compValue.compareTo(yellowMin) >= 0) &&
                             (compValue.compareTo(yellowMax) <= 0))
                    {
                        if (StoplightCellRenderer.this.getBackground() != Color.yellow)
                        {
                            StoplightCellRenderer.this.setBackground(Color.yellow);
                        }
                    }
                    else
                    {
                        if (StoplightCellRenderer.this.getBackground() != Color.red)
                        {
                            StoplightCellRenderer.this.setBackground(Color.red);
                        }
                    }
                }
                else
                {
                    if (StoplightCellRenderer.this.getBackground() != naGrey)
                    {
                        StoplightCellRenderer.this.setBackground(naGrey);
                    }
                }
            }
            else
            {
                // Use default colors from theme / L&F
                StoplightCellRenderer.this.setForeground(null);
                StoplightCellRenderer.this.setBackground(null);
            }
        }
    }
}

