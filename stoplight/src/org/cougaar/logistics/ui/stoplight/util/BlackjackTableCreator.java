/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.logistics.ui.stoplight.util;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Date;
import java.util.Random;

import org.cougaar.lib.uiframework.transducer.*;
import org.cougaar.lib.uiframework.transducer.configs.*;
import org.cougaar.lib.uiframework.transducer.elements.*;

public class BlackjackTableCreator
{
    private static String dbURL;
    private static String dbDriver;
    private static boolean accessdb = false;
    private static boolean randomdata = Boolean.getBoolean("RANDOMDATA");
    private static boolean createItemTable = Boolean.getBoolean("CREATEITEMS");
    private static boolean createMetricTable=Boolean.getBoolean("CREATEMETRICS");
    private static boolean createOrgTable = Boolean.getBoolean("CREATEORGS");
    private static boolean createDataTable = Boolean.getBoolean("CREATEDATA");
    private static boolean createItemUnitTable =
        Boolean.getBoolean("CREATEITEMUNITTABLE");
    private static int startTime = Integer.getInteger("STARTTIME").intValue();
    private static int endTime = Integer.getInteger("ENDTIME").intValue();

    public static void main(String[] args)
    {
        if (args.length != 4)
        {
            System.out.println("Usage: java mil.darpa.log.alpine.blackjack." +
                               "assessui.util.BlackjackTableCreator " +
                               "<access|oracle> <dburl> <user> <password>");
            return;
        }

        System.out.println("Database Type: " + args[0]);
        System.out.println(" Database URL: " + args[1]);
        System.out.println("  User Accout: " + args[2] + "\n");

        String databaseType = args[0];
        if (databaseType.equalsIgnoreCase("access"))
        {
            dbURL = "jdbc:odbc:";
            dbDriver = "sun.jdbc.odbc.JdbcOdbcDriver";
            accessdb = true;
        }
        else if (databaseType.equalsIgnoreCase("oracle"))
        {
            // dbURL = "jdbc:oracle:oci8:@";
            dbURL = "jdbc:oracle:thin:@";
            dbDriver = "oracle.jdbc.driver.OracleDriver";
            accessdb = false;
        }
        else
        {
            System.out.println("Don't recognize " + databaseType +
                               " database");
            return;
        }
        dbURL += args[1];
        String user = args[2];
        String password = args[3];

        Connection con = null;
        Statement stmt = null;
        try
        {
            con = establishConnection(dbURL, user, password);
            stmt = con.createStatement();
            createTables(stmt);
            stmt.close();                               //
            con.close();                                // can't have nested
            populateTransducerTables(user, password);   // connections in some
            con = establishConnection(dbURL, user, password);  // databases.
            con.setAutoCommit(false);                          //
            populateOtherTables(con);
            con.commit();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            }
            catch(SQLException e){}
        }
    }

    private static Connection
        establishConnection(String dbURL, String user, String password)
        throws Exception
    {
        Connection conOracle = null;

        //
        // Database
        //
         try
        {
            Class.forName(dbDriver);
        }
        catch(Exception e)
        {
            System.out.println("Failed to load driver");
        }

        // Connect to the database
        conOracle = DriverManager.getConnection(dbURL, user, password);

        return conOracle;
    }

    private static void createTables(Statement stmt) throws SQLException
    {
        if (createDataTable)
        {
            try
            {
                stmt.executeUpdate("DROP TABLE assessmentDemandData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP TABLE assessmentInventoryData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP TABLE assessmentTargetLevelData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP TABLE assessmentCriticalData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP TABLE assessmentDueOutData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP TABLE assessmentDueInData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP PROCEDURE new_assessmentDemandData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP PROCEDURE new_assessmentInventoryData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP PROCEDURE new_assessmentTargetLevelData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP PROCEDURE new_assessmentCriticalData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP PROCEDURE new_assessmentDueOutData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

            try
            {
                stmt.executeUpdate("DROP PROCEDURE new_assessmentDueInData");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}

/*
            stmt.executeUpdate("CREATE TABLE assessmentData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "metric          INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, metric, unitsOfTime))");
*/
            stmt.executeUpdate("CREATE TABLE assessmentDemandData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, unitsOfTime))");

            stmt.executeUpdate("CREATE TABLE assessmentInventoryData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, unitsOfTime))");

            stmt.executeUpdate("CREATE TABLE assessmentTargetLevelData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, unitsOfTime))");

            stmt.executeUpdate("CREATE TABLE assessmentCriticalData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, unitsOfTime))");

            stmt.executeUpdate("CREATE TABLE assessmentDueOutData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, unitsOfTime))");

            stmt.executeUpdate("CREATE TABLE assessmentDueInData " +
                                "(org             INTEGER     NOT NULL," +
                                 "item            INTEGER     NOT NULL," +
                                 "unitsOfTime     INTEGER     NOT NULL," +
                                 "assessmentValue FLOAT," +
                            "primary key (org, item, unitsOfTime))");

            // Create a stored procedure for adding new rows
/*
            stmt.executeUpdate("create procedure ADD_NEW_DATA_ROWS " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, metricid integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentdata where org = orgid and " +
                   "item = itemid and metric = metricid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentdata " +
                   "(org, item, unitsoftime, metric, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, metricid, value); " +
                   "end loop; " +
                   "end;");
*/
            stmt.executeUpdate("create procedure new_assessmentDemandData " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentDemandData where org = orgid and " +
                   "item = itemid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentDemandData " +
                   "(org, item, unitsoftime, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, value); " +
                   "end loop; " +
                   "end;");
            stmt.executeUpdate("create procedure new_assessmentInventoryData " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentInventoryData where org = orgid and " +
                   "item = itemid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentInventoryData " +
                   "(org, item, unitsoftime, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, value); " +
                   "end loop; " +
                   "end;");
            stmt.executeUpdate("create procedure new_assessmentTargetLevelData " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentTargetLevelData where org = orgid and " +
                   "item = itemid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentTargetLevelData " +
                   "(org, item, unitsoftime, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, value); " +
                   "end loop; " +
                   "end;");
            stmt.executeUpdate("create procedure new_assessmentCriticalData " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentCriticalData where org = orgid and " +
                   "item = itemid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentCriticalData " +
                   "(org, item, unitsoftime, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, value); " +
                   "end loop; " +
                   "end;");
            stmt.executeUpdate("create procedure new_assessmentDueOutData " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentDueOutData where org = orgid and " +
                   "item = itemid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentDueOutData " +
                   "(org, item, unitsoftime, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, value); " +
                   "end loop; " +
                   "end;");
            stmt.executeUpdate("create procedure new_assessmentDueInData " +
                   "(orgid integer, itemid integer, start_time integer, " +
                   "end_time integer, value float) " +
                   "as " +
                   "time_num integer; " +
                   "begin " +
                   "delete from assessmentDueInData where org = orgid and " +
                   "item = itemid and " +
                   "unitsoftime >= start_time and unitsoftime <= end_time; " +
                   "for time_num in start_time..end_time loop " +
                   "insert into assessmentDueInData " +
                   "(org, item, unitsoftime, assessmentvalue) " +
                   "values " +
                   "(orgid, itemid, time_num, value); " +
                   "end loop; " +
                   "end;");
        }

        if (createOrgTable)
        {
            try
            {
                stmt.executeUpdate("DROP TABLE assessmentOrgs");
            }
            catch(SQLException e) {} // it doesn't yet exist; good
            stmt.executeUpdate("CREATE TABLE assessmentOrgs " +
                                "(id     INTEGER      NOT NULL," +
                                 "parent INTEGER      NOT NULL," +
                                 "name   CHAR(40)     NOT NULL," +
                                 "primary key (id)            )");

            stmt.executeUpdate ("create index assessmentOrgs_name_index on assessmentOrgs (name)");
        }

        if (createItemTable)
        {
            try
            {
                stmt.executeUpdate("DROP TABLE itemWeights");
            }
            catch(SQLException e) {} // it doesn't yet exist; good
            stmt.executeUpdate("CREATE TABLE itemWeights" +
                                "(id     INTEGER      NOT NULL," +
                                 "item_id CHAR(20)," +
                                 "parent_id INTEGER," +
                                 "parent_id_text CHAR(20)," +
                                 "name   CHAR(70)," +
                                 "weight double," +
                                 "primary key (id)            )");

            stmt.executeUpdate ("create index itemWeights_item_id_index on itemWeights (item_id)");
        }

        if (createMetricTable)
        {
            try
            {
                stmt.executeUpdate("DROP TABLE assessmentMetrics");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}
            stmt.executeUpdate("CREATE TABLE assessmentMetrics " +
                                "(id     INTEGER      NOT NULL," +
                                 "name   CHAR(50)     NOT NULL," +
                                 "table_name   CHAR(50)," +
                                "primary key (id))");
        }

        if (createItemUnitTable)
        {
            try
            {
                stmt.executeUpdate("DROP TABLE assessmentItemUnits");
            }
            catch(SQLException e) {/* it doesn't yet exist; good */}
            stmt.executeUpdate("CREATE TABLE assessmentItemUnits " +
                                "(nsn        VARCHAR(32) NOT NULL," +
                                 "unit_issue CHAR(2)      NOT NULL," +
                                "primary key (nsn))");
        }
    }

    private static void populateTransducerTables(String user, String password)
        throws Exception
    {
        // Organization Tree
        SqlTableMap config = new SqlTableMap();
        config.setDbTable("assessmentOrgs");
        config.setIdKey("id");
        config.setParentKey("parent");
        config.addContentKey("UID", "name");
        //config.addContentKey("annotation", "note");
        config.setPrimaryKeys(new String[] {"keynum"});

        if (createOrgTable)
        {
            saveInDb(readFromFile("orgTree.xml"), null, config, user,password);
        }

        if (createItemTable)
        {
            // Item Tree
            config.setDbTable("itemWeights");
            saveInDb(readFromFile("itemTree.xml"), null, config,user,password);
        }
    }

    private static void populateOtherTables(Connection con) throws Exception
    {
        Statement stmt = con.createStatement();

        // metric table
        if (createMetricTable)
        {
            String[] metrics = {"Demand", "Inventory",
                                "Target Level", "Critical Level",
                                "DueOut", "DueIn",
                                "Inventory Over Target Level",
                                "Inventory Over Critical Level",
                                "Cumulative Resupply Over Cumulative Demand"};
            String[] table_names = {"assessmentDemandData",
                                    "assessmentInventoryData",
                                    "assessmentTargetLevelData",
                                    "assessmentCriticalData",
                                    "assessmentDueOutData",
                                    "assessmentDueInData",
                                    "",
                                    "",
                                    ""};

            for (int i=0; i < metrics.length; i++)
            {
                stmt.executeUpdate("INSERT INTO assessmentMetrics VALUES ("
             + (i+1) + ", '" + metrics[i] + "', '" + table_names[i] + "')");
            }

            con.commit();
        }

        // item unit table (based on tables from blackjack8)
        if (createItemUnitTable)
        {
            //dbURL = "jdbc:oracle:thin:@www.host.com:1521:alp";
            //dbDriver = "oracle.jdbc.driver.OracleDriver";

            Connection anothercon = null;
            Statement anotherstmt = null;
            ResultSet anotherrs = null;
            try
            {
                con.setAutoCommit(true);

                // class III
                //anothercon = establishConnection(dbURL, "icis",
                //    (dbURL.endsWith("www.host.com:1521:alp") ?
                //                    "password" : "icis"));
                anothercon =
                    establishConnection(dbURL, "blackjacka", "blackjacka");
                anotherstmt = anothercon.createStatement();
                //anotherrs = anotherstmt.executeQuery(
                //    "SELECT nsn, ui FROM header WHERE nsn like '91%'");
                anotherrs = anotherstmt.executeQuery(
                    "SELECT nsn, ui FROM fuels_unit_of_issue");
                while(anotherrs.next())
                {
                    try {
                        stmt.executeUpdate(
                            "INSERT INTO assessmentItemUnits VALUES ('NSN/" +
                            anotherrs.getString(1) + "', '" +
                            anotherrs.getString(2) + "')");
                    }
                    catch (Exception e){System.out.println(e.getMessage());}
                }
                anotherrs.close();
                anotherstmt.close();
                anothercon.close();
                con.commit();

                //*****************************
                //dbURL = "jdbc:oracle:thin:@alp-3.alp.isotic.org:1521:alp";
                //*****************************

                // class VIII
                anothercon = establishConnection(dbURL, "blackjack",
                    (dbURL.endsWith("www.host.com:1521:alp") ?
                                    "password" : "blackjack"));
                anotherstmt = anothercon.createStatement();
                anotherrs = anotherstmt.executeQuery(
                    "SELECT nsn, unit_issue FROM catalog_master");
                while(anotherrs.next())
                {
                    try {
                    stmt.executeUpdate(
                        "INSERT INTO assessmentItemUnits VALUES ('NSN/" +
                        anotherrs.getString(1) + "', '" +
                        anotherrs.getString(2)+"')");
                    }
                    catch (Exception e){System.out.println(e.getMessage());}
                }
                anotherrs.close();
                anotherstmt.close();
                anothercon.close();
                con.commit();

                // class I
                anothercon = establishConnection(dbURL, "blackjack",
                    (dbURL.endsWith("www.host.com:1521:alp") ?
                                    "password" : "blackjack"));
                anotherstmt = anothercon.createStatement();
                anotherrs = anotherstmt.executeQuery(
                    "SELECT nsn, ui FROM class1_item");
                while(anotherrs.next())
                {
                    try {
                        stmt.executeUpdate(
                            "INSERT INTO assessmentItemUnits VALUES ('NSN/" +
                            anotherrs.getString(1) + "', '" +
                            anotherrs.getString(2) + "')");
                    }
                    catch (Exception e){System.out.println(e.getMessage());}
                }
                anotherrs.close();
                anotherstmt.close();
                anothercon.close();
                con.commit();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (anotherrs != null) anotherrs.close();
                    if (anotherstmt != null) anotherstmt.close();
                    if (anothercon != null) anothercon.close();
                }
                catch(SQLException e){}
            }

            con.commit();
        }

        if (createDataTable)
        {
            // Value table
            Random rand = new Random();
            int orgSize = getNumberOfRows(stmt, "assessmentOrgs");
            int itemSize = getNumberOfRows(stmt, "itemWeights");
            int metricSize = getNumberOfRows(stmt, "assessmentMetrics");
            System.out.println("Time Range from "+startTime+" to "+endTime);
            System.out.println(
                "Number of rows to be inserted into assessmentData: " +
            (orgSize * (itemSize+1) * metricSize * (endTime - startTime + 1)));

            PreparedStatement prepStmt = null;
            prepStmt = con.prepareStatement(
                "INSERT INTO assessmentData VALUES (?, ?, ?, ?)");
            for (int org=0; org<orgSize; org++)
            {
                long start = (new Date()).getTime();
                for (int item=0; item<(itemSize+1); item++)
                    for (int time=startTime; time<(endTime+1); time++)
                        // access db does not support prepared statements
                        if (accessdb)
                        {
                            stmt.executeUpdate(
                                "INSERT INTO assessmentDemandData VALUES"
                                + " (" + org + ", " + item + ", " +
                                time + ", " +
                                (randomdata ?
                                String.valueOf(rand.nextFloat() * 2)
                                : "NULL") + ")");
                        }
                        else
                        {
                            prepStmt.setInt(1, org);
                            prepStmt.setInt(2, item);
                            prepStmt.setInt(3, time);
                            if (randomdata)
                            {
                                prepStmt.setFloat(4, rand.nextFloat() * 2);
                            }
                            else
                            {
                                prepStmt.setNull(4, Types.FLOAT);
                            }
                            prepStmt.executeUpdate();
                        }
                con.commit();
                long secondsPassed = ((new Date()).getTime() - start)/1000;
                long secondsToGo = secondsPassed * (orgSize - org);
                long hoursToGo = secondsToGo / 3600;
                secondsToGo -= (hoursToGo * 3600);
                long minutesToGo = secondsToGo / 60;
                secondsToGo -= (minutesToGo * 60);
                System.out.println("Completed filling data for org #" + org);
                System.out.println("Estimated time to go: " + hoursToGo +
                               " hours, " + minutesToGo + " minutes, and " +
                               secondsToGo + " seconds");
            }
            prepStmt.close();
        }

        stmt.close();
    }

    private static int getNumberOfRows(Statement stmt, String tableName)
        throws SQLException
    {
        ResultSet rs = stmt.executeQuery("SELECT COUNT (*) FROM " + tableName);
        rs.next();
        int rowCount = rs.getInt(1);
        rs.close();
        System.out.println(rowCount + " rows in " + tableName);
        return rowCount;
    }

    private static Structure readFromFile (String fileName) throws Exception
    {
        XmlInterpreter xint = new XmlInterpreter();
        FileInputStream fin = new FileInputStream(fileName);
        Structure s = xint.readXml(fin);
        fin.close();
        return s;
    }

    private static void saveInDb (Structure s, String[] keys,
                                  SqlTableMap config, String user,
                                  String password)
    {
        MappedTransducer mt = makeTransducer(config, user, password);
        mt.openConnection();
        mt.writeToDb(keys, s);
        mt.closeConnection();
    }

    private static MappedTransducer
        makeTransducer(SqlTableMap config, String user, String password)
    {
        MappedTransducer mt = new MappedTransducer(dbDriver, config);
        mt.setDbParams(dbURL, user, password);
        return mt;
    }
}
