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
package org.cougaar.logistics.ui.stoplight.ui.util;

import java.io.FileInputStream;
import java.sql.*;
import java.util.*;
import javax.swing.tree.*;

import org.cougaar.logistics.ui.stoplight.transducer.MappedTransducer;
import org.cougaar.logistics.ui.stoplight.transducer.configs.SqlTableMap;
import org.cougaar.logistics.ui.stoplight.transducer.elements.Structure;

/**
 * This class is used to extract data from a database specified by system
 * properties.  It provides generic lookup methods for getting data out
 * of tables and wrapper methods for the use of the UI framework SQL
 * transducer.
 *
 * The following system properies must be set for proper configuration:<BR><BR>
 *
 * DBURL - JDBC url to use to access the database<BR>
 * DBDRIVER - JDBC driver classname to use to access the database<BR>
 * DBUSER - User account to use to access the database<BR>
 * DBPASSWORD - User password to use to access the database<BR>
 */
public class DBDatasource
{
    public static final String DBTYPE = System.getProperty("DBTYPE");

    /** JDBC url to use to access the database */
    public static final String DBURL = (DBTYPE.equalsIgnoreCase("access") ?
        "jdbc:odbc:":"jdbc:oracle:thin:@") + System.getProperty("DBURL");

    /** JDBC driver to use to access the database */
    public static final String DBDRIVER = (DBTYPE.equalsIgnoreCase("access") ?
        "sun.jdbc.odbc.JdbcOdbcDriver" : "oracle.jdbc.driver.OracleDriver");

    /** User account to use to access the database */
    public static final String DBUSER = System.getProperty("DBUSER");

    /** User password to use to access the database */
    public static final String DBPASSWORD = System.getProperty("DBPASSWORD");

    /**
     * Establish a new connection to the database using the configured system
     * property values.
     *
     * @return a new connection to the database.
     */
    public static Connection establishConnection() throws SQLException
    {
        Connection con = null;

        //
        // Load Database Driver
        //
        try
        {
            Class.forName(DBDRIVER);
        }
        catch(Exception e)
        {
            System.out.println("Failed to load driver");
        }

        // Connect to the database
        con = DriverManager.getConnection(DBURL, DBUSER, DBPASSWORD);

        return con;
    }

    /**
     * Get a existing connection to the database.  Create one if existing one
     * doesn't exist.
     *
     * @return a connection to the database.
     */
    public static Connection getConnection() throws SQLException
    {
        if (dbConnection == null)
        {
            dbConnection = establishConnection();
        }

        return dbConnection;
    }
    private static Connection dbConnection = null;

    /**
     * Recreates a structure based on data from the database.
     *
     * @param config configuration object for mapped transducer.
     * @return structure based on data from the database.
     */
    public static Structure restoreFromDb (SqlTableMap config)
    {
        // access can't deal with multiple concurent connections
        if (DBTYPE.equalsIgnoreCase("access") && dbConnection != null)
        {
            try { dbConnection.close(); } catch(SQLException e) {}
            dbConnection = null;
        }

        MappedTransducer mt = makeTransducer(config);
        mt.openConnection();
        Structure s = mt.readFromDb(null);
        mt.closeConnection();
        return s;
    }

    /**
     * Executes a query and returns the first column of the result set in a
     * Vector.
     *
     * @param query query to execute
     * @return vector containing string representations of the first column of
     *         the result set.
     */
    public static Vector executeVectorReturnQuery(String query)
    {
        Connection con = null;

        try
        {
            con = getConnection();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        // (reusing a single connection; don't close it)

        return executeVectorReturnQuery(con, query);
    }

    /**
     * Executes a query and returns the first column of the result set in a
     * Vector.
     *
     * @param con database connection to use
     * @param query query to execute
     * @return vector containing string representations of the first column of
     *         the result set.
     */
    private static Vector
        executeVectorReturnQuery(Connection con, String query)
    {
        Statement stmt = null;
        ResultSet rs = null;
        Vector result = new Vector();

        try
        {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
            while (rs.next())
            {
                String rsString = rs.getString(1);
                if (rsString != null)
                {
                    result.add(rsString.trim());
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (Exception e){/* I tried */}
        }

        return result;
    }

    /**
     * Searches through a table for each of a list of search values.  Returns
     * a list of values from the result columns that are found.  One result
     * value is returned for each search value.
     *
     * @param table        name of table to search through
     * @param searchColumn name of column to use as search column
     * @param resultColumn name of column to use as result column
     * @param searchValues values to search for in search column
     * @return vector of result strings
     */
    public static Vector
        lookupValues(String table, String searchColumn, String resultColumn,
                     Enumeration searchValues)
    {
        Vector values = new Vector();
        Connection con = null;

        try
        {
            con = getConnection();
            while(searchValues.hasMoreElements())
            {
                values.add(lookupValue(con, table, searchColumn, resultColumn,
                                       searchValues.nextElement().toString()));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return values;
    }

    /**
     * Returns a vector of values from a single column of a table
     *
     * @param table        name of table to search through
     * @param resultColumn name of column to get values from
     */
    public static Vector
        lookupValues(String table, String resultColumn)
    {
        return  lookupValues(table, null, resultColumn, (String)null);
    }

    /**
     * Searches through a table for a single search value in a specified
     * search column.  Returns a list of values from the result column of the
     * rows that are found.
     *
     * @param table        name of table to search through
     * @param searchColumn name of column to use as search column
     * @param resultColumn name of column to use as result column
     * @param searchValue  value to search for in search column
     * @return vector of result strings
     */
    public static Vector
        lookupValues(String table, String searchColumn, String resultColumn,
                     String searchValue)
    {
        Vector values = null;
        Connection con = null;

        try
        {
            con = getConnection();
            values = lookupValues(con, table, searchColumn,
                                  resultColumn, searchValue);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return values;
    }

    /**
     * Searches through a table for a single search value in a specified
     * search column.  Returns a single values from the result column of the
     * first row that is found.
     *
     * @param table        name of table to search through
     * @param searchColumn name of column to use as search column
     * @param resultColumn name of column to use as result column
     * @param searchValue  value to search for in search column
     * @return result data string
     */
    public static String lookupValue(String table, String searchColumn,
                                     String resultColumn, String searchValue)
    {
        return (String)lookupValues(table, searchColumn,
                                    resultColumn, searchValue).firstElement();
    }

    /**
     * Searches through a table for a single search value in a specified
     * search column.  Returns a single values from the result column of the
     * first row that is found.
     *
     * @param con          connection to database
     * @param table        name of table to search through
     * @param searchColumn name of column to use as search column
     * @param resultColumn name of column to use as result column
     * @param searchValue  value to search for in search column
     * @return result data string
     */
    private static String lookupValue(Connection con, String table,
                                      String searchColumn, String resultColumn,
                                      String searchValue)
    {
        return (String)lookupValues(con, table, searchColumn,
                                    resultColumn, searchValue).firstElement();
    }

    private static Vector
        lookupValues(Connection con, String table, String searchColumn,
                    String resultColumn, String searchValue)
    {
        StringBuffer query = new StringBuffer("SELECT ");
        query.append(resultColumn + " FROM " + table);
        if ((searchColumn != null) && (searchValue != null))
        {
            if (!searchColumn.equalsIgnoreCase("id"))
            {
                searchValue = "'" + searchValue + "'";
            }

            query.append(" WHERE (" + searchColumn + "=" + searchValue + ")");
        }

        return executeVectorReturnQuery(con, query.toString());
    }

    /*
    private static void trimUIDs(DefaultMutableTreeNode dmtn)
    {
        dmtn.setUserObject(dmtn.getUserObject().toString().trim());
        for (int i=0; i<dmtn.getChildCount(); i++)
        {
            trimUIDs((DefaultMutableTreeNode)dmtn.getChildAt(i));
        }
    }
    */

    private static MappedTransducer makeTransducer (SqlTableMap config)
    {
        MappedTransducer mt = new MappedTransducer(DBDRIVER, config);
        mt.setDbParams(DBURL, DBUSER, DBPASSWORD);
        return mt;
    }
}
