package org.cougaar.logistics.ui.stoplight.society;

import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.SubscriptionAccess;
import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.cougaar.lib.aggagent.util.XmlUtils;

import org.cougaar.lib.uiframework.transducer.MappedTransducer;
import org.cougaar.lib.uiframework.transducer.XmlInterpreter;
import org.cougaar.lib.uiframework.transducer.elements.Structure;
import org.cougaar.lib.uiframework.transducer.configs.SqlTableMap;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.DBConnectionPool;
import org.cougaar.util.Parameters;

import org.cougaar.logistics.ui.stoplight.client.AggregationScheme;
import org.cougaar.logistics.ui.stoplight.util.TreeUtilities;

public class MetricFormatter implements IncrementFormat
{
  private static DefaultMutableTreeNode fullItemTree = null;
  private Vector metricNames = null;
  private Vector metricFormulas = null;
  private Integer startTime = null;
  private Integer endTime = null;
  private boolean aggregateTime = false;
  private boolean itemFixed = false;
  private DefaultMutableTreeNode itemTree = null;
  private AggregationScheme aggregationScheme = null;

  public void setMetricFormulas(String metricFormulaString)
  {
    metricFormulas = new Vector();
    StringTokenizer formulaTok = new StringTokenizer(metricFormulaString, "|");
    while (formulaTok.hasMoreElements())
    {
      Vector metricFormula = new Vector();
      StringTokenizer elementTok =
        new StringTokenizer(formulaTok.nextToken(), ",");
      while (elementTok.hasMoreTokens())
      {
        metricFormula.addElement(elementTok.nextToken());
      }
      metricFormulas.add(metricFormula);
    }
  }

  public void setMetricNames(String metricNamesString)
  {
    metricNames = new Vector();
    StringTokenizer nameTok = new StringTokenizer(metricNamesString, ",");
    while (nameTok.hasMoreElements())
    {
      metricNames.addElement(nameTok.nextToken());
    }
  }

  public void setStartTime(String startTimeString)
  {
    startTime = new Integer(startTimeString);
  }

  public void setEndTime(String endTimeString)
  {
    endTime = new Integer(endTimeString);
  }

  public void setAggregateTime(String aggregateTime)
  {
    this.aggregateTime = Boolean.valueOf(aggregateTime).booleanValue();
  }

  public void setItemFixed(String itemFixed)
  {
    this.itemFixed = Boolean.valueOf(itemFixed).booleanValue();
  }

  public void setItemTree(String selectedItem)
  {
    synchronized (this.getClass())
    {
      // attempt to load full item weights tree
      if (fullItemTree == null)
      {
        if ((fullItemTree = loadFullItemTree()) == null)
        {
          // item aggregation not possible without tree
          System.out.println("Failed to load item tree, " +
                             "item aggregation disabled");
          return;
        }
      }
    }

    itemTree = TreeUtilities.findNode(fullItemTree, selectedItem);
  }

  public void setAggregationScheme(String aggregationSchemeXML)
  {
    try {
      aggregationScheme =
        new AggregationScheme(XmlUtils.parse(aggregationSchemeXML));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void encode(UpdateDelta out, SubscriptionAccess sacc)
  {
    out.setReplacement(true);

    Collection inventories =
      ExtractionHelper.getInventoriesFromLogPlan(sacc.getMembership());
    boolean multiMetric = metricFormulas.size() > 1;
    for (int i = 0; i < metricFormulas.size(); i++)
    {
      Vector metricFormula = (Vector)metricFormulas.elementAt(i);

      try {
        Collection derivedSchedules =
          ExtractionHelper.calculateDerivedSchedules(
            metricFormula, inventories, startTime, endTime, aggregateTime ?
            aggregationScheme.getSQLString(aggregationScheme.timeAggregation) :
            null);

        // aggregate over items if needed
        if ((itemTree != null) && (!derivedSchedules.isEmpty()))
        {
          List itemAggregated = new LinkedList();
          ItemMelder itemMelder = new ItemMelder();
          itemMelder.setItemFixed(itemFixed);
          itemMelder.setItemTree(itemTree);
          itemMelder.setAggregationMethod(aggregationScheme.itemAggregation);
          itemMelder.meld(null, null, (List)derivedSchedules, itemAggregated);
          derivedSchedules = itemAggregated;
        }

        if (multiMetric)
        {
          DataAtomUtilities.addIdentifier(derivedSchedules, "Metric",
                                          (String)metricNames.elementAt(i));
        }
        out.getReplacementList().addAll(derivedSchedules);
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  private static DefaultMutableTreeNode loadFullItemTree()
  {
    Structure fullItemTreeStruct = null;

    // try to load item weights tree from database
    Connection conn = null;
    try {
      String dbType = "oracle"; // need to find correct way to determine this
      String driver = Parameters.findParameter("driver." + dbType);
      if (driver != null)
      {
        DBConnectionPool.registerDriver(driver);
      }
      String database = "blackjack.database";
      String url = Parameters.findParameter(database);
      String user = Parameters.findParameter(database + ".user");
      String password = Parameters.findParameter(database + ".password");
      conn = DBConnectionPool.getConnection(url, user, password);
      fullItemTreeStruct = restoreFromDb(conn, createItemTreeConfig());
    } catch (Exception e) {
      System.out.println("Failed to load item tree from database.");
      e.printStackTrace();
    }
    finally {
      if (conn != null)
      try {
        conn.close();
      } catch (Exception e) {e.printStackTrace();}
    }

    if (fullItemTreeStruct == null)
    {
      // try to load item weights tree from file
      System.out.println("Attempting to load item tree from file.");
      File itemFile = ConfigFinder.getInstance().locateFile("itemTree.xml");
      if (itemFile != null)
      {
        fullItemTreeStruct = TreeUtilities.readFromFile(itemFile.getPath());
      }
    }

    return (fullItemTreeStruct == null) ? null :
            TreeUtilities.createTree(fullItemTreeStruct);
  }

  /**
    * Gets tree representation of data in item weights table. Table must follow
    * a given schema.
    */
  private static SqlTableMap createItemTreeConfig()
  {
    SqlTableMap config = new SqlTableMap();
    config.setDbTable("itemWeights");
    config.setIdKey("id");
    config.setParentKey("parent_id");
    config.addContentKey("UID", "item_id");
    //config.addContentKey("ID",     "id");
    config.addContentKey("WEIGHT", "weight");
    config.setPrimaryKeys(new String[] {"keynum"});

    return config;
  }

  /**
    * Recreates a structure based on data from the database.
    *
    * @param config configuration object for mapped transducer.
    * @return structure based on data from the database.
    */
  private static Structure restoreFromDb (Connection conn, SqlTableMap config)
  {
    MappedTransducer mt = new MappedTransducer(null, config);
    mt.setConnection(conn);
    Structure s = mt.readFromDb(null);
    return s;
  }
}