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

import java.io.File;
import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.*;

import org.cougaar.logistics.ui.stoplight.ui.components.graph.Axis;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.DataSet;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.Graph2D;
import org.cougaar.logistics.ui.stoplight.ui.components.graph.Markers;
import org.cougaar.logistics.ui.stoplight.ui.models.DatabaseTableModel;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;
import org.cougaar.logistics.ui.stoplight.ui.models.TestFunctionTableModel;

/**
 * Line plot chart that can get it's data from a table model.  Each row of the
 * table model is graphed as a line.
 */
public class CLinePlotChart extends CChart
{
    private TableModel tm;
    private CChartLegend legend = null;
    private int plotCount = 0;

    /**
     * Default constructor.  Creates empty line chart
     */
    public CLinePlotChart()
    {
        super("TITLE", null, null, false);
        this.tm = new TestFunctionTableModel();
        init();
    }

    /**
     * Creates line chart that shows graph of data in rows of given table model
     *
     * @param tm table model to use for acquiring data
     */
    public CLinePlotChart(TableModel tm)
    {
        super("TITLE", null, null, false);
        this.tm = tm;

        init();
    }

    /**
     * Associate a legend with this chart
     *
     * @param legend to associate with this chart
     */
    public void setLegend(CChartLegend legend)
    {
        this.legend = legend;

        legend.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e)
                {
                    repaint();
                }
            });
    }

    /**
     * When look and feel or theme is changed, this method is called.  It sets
     * the graph color and font scheme based on metal L&F properties.
     */
    public void updateUI()
    {
        super.updateUI();

        regeneratePlots();
    }

    /**
     * Initializes the graph
     */
    private void init()
    {
        tm.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                regeneratePlots();
                            }
                        });
                }
            });

        updateUI();
    }

    /**
     * Plots the data in the data array as a single line on graph.
     *
     * @param data        Array containing the (x,y) data pairs.
     * @param np          The number of (x,y) data points. This means that the
     *                    minimum length of the data array is 2*np.
     * @param markerScale The scaling factor for the marker.
     * @param legendText  The text used to describe this line in the legend
     */
    public void plot(double[] data, int np, float markerScale,
                     String legendText)
    {
        // refuse to plot more than 8 lines
        if (plotCount >= 8) return;

        DataSet dataSet = null;
        try
        {
            dataSet = new DataSet(data,np);
            dataSet.dataGroup = "Legend";
            dataSet.dataName = legendText;
            attachDataSet(dataSet);
            if (legend != null)
                legend.addDataSet(dataSet);
            plotCount++;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Recreates plots based on table model
     */
    private void regeneratePlots()
    {
        if (tm == null) return;

        detachAllDataSets();
        if (legend != null)
            legend.removeAllDataSets();
        plotCount = 0;

        // find start of data (i.e. lose headers)
        int columnStart = 1;
        int rowStart = 0;
        search : {
            for (; rowStart < tm.getRowCount(); rowStart++)
            {
                for (; columnStart < tm.getColumnCount() ; columnStart++)
                {
                    Object testValue = tm.getValueAt(rowStart, columnStart);
                    if ((testValue instanceof Float) ||
                        (testValue instanceof Double) ||
                        (testValue.toString().
                            equals(DatabaseTableModel.NO_VALUE)))
                    {
                        break search;
                    }
                }
            }
        }

        int numberOfDataPoints = tm.getColumnCount() - columnStart;
        if (numberOfDataPoints <= 0) return;
        double[] data = new double[numberOfDataPoints * 2];

        if (tm != null)
        {
            for (int row = rowStart; row < tm.getRowCount(); row++)
            {
                for (int column = columnStart; column < tm.getColumnCount();
                     column++)
                {
                    Object value = tm.getValueAt(row, column);
                    double valueDouble = (value instanceof Number) ?
                        ((Number)value).doubleValue() : 0;

                    // don't attempt to plot infinite numbers
                    if (Double.isInfinite(valueDouble))
                      continue;

                    int pointLocation = (column - columnStart) * 2;
                    try
                    {
                        data[pointLocation] = Double.parseDouble(
                            tm.getColumnName(column).toString());
                    }
                    catch(NumberFormatException e)
                    {
                        data[pointLocation] = column;
                    }
                    data[pointLocation + 1] = valueDouble;
                }
                plot(data, numberOfDataPoints, 1,
                     tm.getValueAt(row, 0).toString());
            }
        }

        // Must set the total range of the slider
        resetTotalRange();
        resetRange();

        revalidate();
        repaint();
        legend.revalidate();
        legend.repaint();
    }

    /**
     * Main for unit testing.
     *
     * @param args ignored
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new CLinePlotChart(), BorderLayout.CENTER);
        frame.setSize(400, 100);
        frame.setVisible(true);
    }
}