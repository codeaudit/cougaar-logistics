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

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import java.sql.*;

import org.cougaar.logistics.ui.stoplight.ui.util.DBDatasource;

/**
 * The database table model is used to represent the result set of a SQL
 * query as a table model that can be used to populate either a graph or
 * table view of the data.
 *
 * Additional methods are included to manipulate the modeled result set after
 * the query (e.g. transpose, setXY, aggregate).
 */
public class DatabaseTableModel extends TransformableTableModel
{
    /** Default constructor; model will contain no data until DBQuery is set */
    public DatabaseTableModel() {}

    /**
     * Create new model of result set of specified SQL query
     *
     * @param     sqlQuery  the SQL query to make of database.
     */
    public DatabaseTableModel(String sqlQuery)
    {
        setDBQuery(sqlQuery);
    }

    /**
     * Set the database query to which this model should model result set.
     * This result set will replace any existing data in model.
     *
     * @param sqlQuery the SQL query to send to database
     */
    public void setDBQuery(String sqlQuery)
    {
        setDBQuery(sqlQuery, 1, 1);
    }

    /**
     * Set the database query to which this model should model result set.
     * This result set will replace any existing data in model.
     *
     * @param sqlQuery the SQL query to send to database
     * @param rowHeaders the number of leading columns that should be considered
     *                   row headers in the result set.
     * @param columnHeaders the number of leading rows that should be
     *                      considered column headers in the result set.
     */
    public synchronized void
        setDBQuery(String sqlQuery, int rowHeaders, int columnHeaders)
    {
        dataRows = new Vector();

        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            //con = DBDatasource.establishConnection();
            con = DBDatasource.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(sqlQuery);

            // get/use meta data from table
            Vector columnRow = new Vector();
            dataRows.add(columnRow);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numColumns = rsmd.getColumnCount();
            for (int i = 1; i <= numColumns; i++)
            {
                columnRow.add(rsmd.getColumnName(i));
            }

            // get the data
            addDataFromResultSet(rs, rowHeaders, columnHeaders, numColumns);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                //if (con != null) con.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        fireTableChangedEvent(
            new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * Set a database query to add the result set of which to this model.
     * Only append query whose result sets have the same format as the
     * existing result set.
     *
     * @param sqlQuery the SQL query to send to database
     * @param rowHeaders the number of leading columns that should be considered
     *                   row headers in the result set.
     * @param columnHeaders the number of leading rows that should be
     *                      considered column headers in the result set.
     */
    public synchronized
        void appendDBQuery(String sqlQuery, int rowHeaders, int columnHeaders)
    {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            //con = DBDatasource.establishConnection();
            con = DBDatasource.getConnection();
            stmt = con.createStatement();
            rs = stmt.executeQuery(sqlQuery);

            // get the data
            addDataFromResultSet(rs, rowHeaders, columnHeaders,
                                 rs.getMetaData().getColumnCount());
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                //if (con != null) con.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        fireTableChangedEvent(
            new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    private void
        addDataFromResultSet(ResultSet rs, int rowHeaders, int columnHeaders,
                             int numColumns) throws SQLException
    {
        int rowCount = 1;
        while(rs.next())
        {
            rowCount++;
            Vector dataRow = new Vector();
            for (int i = 1; i <= numColumns; i++)
            {
                if ((i <= rowHeaders) || (rowCount <= columnHeaders))
                {
                    dataRow.add(rs.getString(i));
                }
                else
                {
                    String assessmentValue;
                    if ((assessmentValue = rs.getString(i)) == null)
                    {
                        dataRow.add(NO_VALUE);
                    }
                    else
                    {
                        dataRow.add(Float.valueOf(assessmentValue));
                    }
                }
            }
            dataRows.add(dataRow);
        }
    }

}