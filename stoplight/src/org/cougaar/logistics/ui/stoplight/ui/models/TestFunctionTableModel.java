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
package org.cougaar.logistics.ui.stoplight.ui.models;

import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelListener;

//import java.sql.*;

/**
  * Test class.  A table model that models two mathmatical functions
  * (sin and cosine).  Used to test table model viewers.
  */
public class TestFunctionTableModel extends DefaultTableModel
{
    private static int rowCount = 2;
    private double[][] data = new double[rowCount][30];

    public TestFunctionTableModel()
    {
        createPlotData();
    }

    private void createPlotData()
    {
        int row = 0;
        for (int column=0; column < data[row].length; column++)
        {
            data[row][column] = Math.sin(column * (360/30));
        }

        row = 1;
        for (int column=0; column < data[row].length; column++)
        {
            data[row][column] = Math.cos(column * (360/30));
        }
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public int getColumnCount()
    {
        return data[0].length + 1;
    }

    public String getColumnName(int columnIndex)
    {
        return String.valueOf(columnIndex);
    }

    public Class getColumnClass(int columnIndex)
    {
        if (columnIndex == 0)
        {
            return String.class;
        }
        else
        {
            return Double.class;
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (columnIndex == 0)
        {
            return "Row: " + rowIndex;
        }

        return new Double(data[rowIndex][columnIndex - 1]);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        //TODO: Implement this javax.swing.table.TableModel method
    }
}