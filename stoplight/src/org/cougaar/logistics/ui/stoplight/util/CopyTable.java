package org.cougaar.logistics.ui.stoplight.util;

import java.io.StringReader;
import java.sql.*;

public class CopyTable
{
  public static void main(String[] args)
  {
    if (args.length < 9)
    {
      System.out.println(
        "Usage: org.cougaar.logistics.ui.stoplight.util.CopyTable " +
        "<table name> <source db driver> <source db url> " +
        "<source db username> <source db password> <target db driver> " +
        "<target db url> <target db username> <target db password>");
    }

    copyTable(args[0],
              new DatabaseDescriptor(args[1], args[2], args[3], args[4]),
              new DatabaseDescriptor(args[5], args[6], args[7], args[8]));
  }

  private static void copyTable(String tableName, DatabaseDescriptor sourceDb,
                                DatabaseDescriptor targetDb)
  {
    Connection sourceCon = null;
    Statement sourceStmt = null;
    ResultSet sourceRs = null;
    Connection targetCon = null;
    Statement targetStmt = null;

    try {
      sourceCon = establishConnection(sourceDb.driver, sourceDb.url,
                                      sourceDb.user, sourceDb.password);
      sourceStmt = sourceCon.createStatement();
      sourceRs = sourceStmt.executeQuery("SELECT * FROM " + tableName);

      targetCon = establishConnection(targetDb.driver, targetDb.url,
                                      targetDb.user, targetDb.password);
      targetStmt = targetCon.createStatement();

      try
      {
        targetStmt.executeUpdate("DROP TABLE " + tableName);
      }
      catch(SQLException e) {/* it doesn't yet exist; good */}

      // create the new table
      StringBuffer createTableSQL = new StringBuffer();
      createTableSQL.append("CREATE TABLE " + tableName + "\n(");
      ResultSetMetaData rsmd = sourceRs.getMetaData();
      int columnCount = rsmd.getColumnCount();
      for (int column = 1; column <= columnCount; column++)
      {
        createTableSQL.append(rsmd.getColumnLabel(column));
        createTableSQL.append(" ");
        createTableSQL.append(getColumnType(rsmd, column));
        createTableSQL.append(" ");
        createTableSQL.append((rsmd.isNullable(column) == rsmd.columnNullable)?
                              "NULL" : "NOT NULL");
        if (column + 1 <= columnCount) createTableSQL.append(",\n");
      }
      createTableSQL.append(")\n");
      System.out.println(createTableSQL);
      int rtn = targetStmt.executeUpdate(createTableSQL.toString());

      // fill the new table
      while(sourceRs.next())
      {
        StringBuffer insertIntoSQL = new StringBuffer();
        insertIntoSQL.append("INSERT INTO " + tableName + " VALUES (");
        for (int column = 1; column <= columnCount; column++)
        {
          insertIntoSQL.append("'");
          insertIntoSQL.append(fixQuotes(sourceRs.getString(column)));
          insertIntoSQL.append("'");
          if (column + 1 <= columnCount) insertIntoSQL.append(", ");
        }
        insertIntoSQL.append(")\n");
        //System.out.println(insertIntoSQL);
        targetStmt.executeUpdate(insertIntoSQL.toString());
      }

   } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (sourceRs != null)
          sourceRs.close();
        if (sourceStmt != null)
          sourceStmt.close();
        if (sourceCon != null)
          sourceCon.close();
        if (targetStmt != null)
          targetStmt.close();
        if (targetCon != null)
          targetCon.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static String getColumnType(ResultSetMetaData rsmd, int column)
    throws SQLException
  {
    String columnTypeString = null;
    int columnType = rsmd.getColumnType(column);

    switch (columnType)
    {
      case Types.VARCHAR:
        columnTypeString = "VARCHAR";
        break;
      default:
        columnTypeString = rsmd.getColumnTypeName(column);
    }

    return columnTypeString;
  }

  private static String fixQuotes(String s)
  {
    int index = s.indexOf('\'');
    if (index == -1)
      return s;

    String fixedString =
      s.substring(0, index) + "''" + fixQuotes(s.substring(index + 1));
    return fixedString;
  }

  private static Connection
    establishConnection(
      String dbDriver, String dbURL, String user, String password)
      throws Exception
  {
    Connection con = null;

    try
    {
      Class.forName(dbDriver);
    }
    catch(Exception e)
    {
      System.out.println("Failed to load driver: " + dbDriver);
    }

    // Connect to the database
    con = DriverManager.getConnection(dbURL, user, password);

    return con;
  }

  private static class DatabaseDescriptor
  {
    public String driver = null;
    public String url = null;
    public String user = null;
    public String password = null;
    public DatabaseDescriptor(String driver, String url, String user,
                              String password)
    {
      this.driver = driver;
      this.url = url;
      this.user = user;
      this.password = password;
    }
  }
}