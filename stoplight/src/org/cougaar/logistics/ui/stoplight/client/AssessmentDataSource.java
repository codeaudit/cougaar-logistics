package org.cougaar.logistics.ui.stoplight.client;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.lib.uiframework.transducer.XmlInterpreter;
import org.cougaar.lib.uiframework.transducer.elements.*;
import org.cougaar.lib.uiframework.ui.util.SelectableHashtable;
import org.cougaar.lib.aggagent.client.AggregationClient;

import org.cougaar.logistics.ui.stoplight.society.InventoryMetric;
import org.cougaar.logistics.ui.stoplight.util.TreeUtilities;

public class AssessmentDataSource
{
    private static boolean useAssessmentAgent = true;
    private static long cTime = 0l;
    private final static long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
    private static final String ITEM_FILENAME = "itemTree.xml";
    private static final String ORG_FILENAME = "orgTree.xml";
    private static final String NAMESERVER_PROPERTY_NAME = "cougaar.aggagent.NAMESERVER";
    private static final String CLUSTER_PROPERTY_NAME = "cougaar.aggagent.AGENTNAME";
    private static final String PSP_PROPERTY_NAME = "cougaar.aggagent.PSP";
    private static final String KEEP_ALIVE_PSP_PROPERTY_NAME = "cougaar.aggagent.KEEPALIVEPSP";
    private static final String DEFAULT_NAMESERVER_URL="http://localhost:8800";
    private static final String DEFAULT_CLUSTER_NAME = "Aggregator";
    private static final String DEFAULT_PSP = "assessment.psp";
    private static final String DEFAULT_KEEP_ALIVE_PSP = "assessmentkeepalive.psp";
    private static final String
      nameServerProp = System.getProperty(NAMESERVER_PROPERTY_NAME);
    private static String nameServerUrl =
      ((nameServerProp == null) || (nameServerProp.equals(""))) ?
        DEFAULT_NAMESERVER_URL : nameServerProp;
    private static final String
      clusterProp = System.getProperty(CLUSTER_PROPERTY_NAME);
    private static String aggClusterName =
      ((clusterProp == null) || (clusterProp.equals(""))) ?
        DEFAULT_CLUSTER_NAME : clusterProp;
    private static final String
      pspProp = System.getProperty(PSP_PROPERTY_NAME);
    private static String psp =
      ((pspProp == null) || (pspProp.equals(""))) ? DEFAULT_PSP : pspProp;
    private static final String
      keepAlivePspProp = System.getProperty(KEEP_ALIVE_PSP_PROPERTY_NAME);
    private static String keepAlivePsp =
      ((keepAlivePspProp == null) || (keepAlivePspProp.equals(""))) ?
        DEFAULT_KEEP_ALIVE_PSP : keepAlivePspProp;

    public static long timeout = 0;
       
    public static AggregationClient pspInterface = null;
    public static Collection validClusters = null;

    /** Minimum time found in time column of assessment data */
    public static int minTimeRange =
      useAssessmentAgent ? 0 : DBInterface.getTimeExt(false);

    /** Maximum time found in time column of assessment data */
    public static int maxTimeRange =
      useAssessmentAgent ? 300: DBInterface.getTimeExt(true);

    /** Item tree from database */
    public static Structure itemTreeStructure = null;
    public static DefaultMutableTreeNode itemTree = null;

    /** Organization tree from database (or assessmentAgent) */
    public static Structure orgTreeStructure = null;
    public static DefaultMutableTreeNode orgTree = null;

    static
    {
      String timeoutStr = System.getProperty("TIMEOUT");
      if (timeoutStr != null && timeoutStr.length() != 0) {
        try {
          timeout = Long.parseLong(timeoutStr);
        } catch (NumberFormatException nfe) {
          System.err.println("WARNING: Expecting number for TIMEOUT, but found: " + timeoutStr);
        }
      }

      createPSPInterface();
      createItemTrees();
      createOrgTrees();
    }

    /** Array of strings that represent blackjack metric types */
    // used specialized select for backwards compat. with old dbs
    public static final Vector rawMetrics = useAssessmentAgent ?
      convertToStringVector(InventoryMetric.getValidValues()) :
        DBInterface.executeVectorReturnQuery(
            "SELECT name FROM assessmentMetrics WHERE table_name IS NOT NULL");

    /** Hashtable to manage aggregation schemes for each type of metric */
    public static Object[] aggregationSchemeLabels = createAggLabels();
    public static Hashtable aggregationSchemes = createDefaultAggSchemes();

    private static void createPSPInterface()
    {
      try {
        if (useAssessmentAgent)
        {
          pspInterface = new AggregationClient(
                              nameServerUrl + "/$" + aggClusterName,
                              psp, keepAlivePsp);
          validClusters = pspInterface.getClusterIds();
          cTime = getCTime();
        }
        else
        {
          throw new Exception();
        }
      } catch (Exception e) {
        pspInterface = null;
        validClusters = null;
        cTime = 0l;
      }
    }

    public static void setNameServerUrl(String nameServerUrl)
    {
      AssessmentDataSource.nameServerUrl = nameServerUrl;
      createPSPInterface();
      createOrgTrees();
    }

    public static String getNameServerUrl()
    {
      return nameServerUrl;
    }

    private static String createAbsoluteFilename(String filename)
    {
        String fullFilename = filename;
        String dataPath = System.getProperty("DATAPATH");
        if (dataPath != null)
        {
          fullFilename =
            dataPath + System.getProperty("file.separator") + filename;
        }
        return fullFilename;
    }

    public static void createItemTrees()
    {
      if (useAssessmentAgent)
      {
        itemTreeStructure =
            TreeUtilities.readFromFile(createAbsoluteFilename(ITEM_FILENAME));
      }
      else
      {
        /** Item tree from database */
        itemTreeStructure = DBInterface.createItemTree();
      }
      itemTree = TreeUtilities.createTree(itemTreeStructure);
      setShowProperty(itemTree, "ITEM_ID");

      /** strip out information not needed at clusters */
      TreeUtilities.stripTreeAttribute(itemTreeStructure, "ID");
      TreeUtilities.stripTreeAttribute(itemTreeStructure, "ITEM_ID");
      TreeUtilities.stripTreeAttribute(itemTreeStructure, "UNITS");
    }

    public static void createOrgTrees()
    {
      /** Organization tree from database (or assessmentAgent) */
      if (useAssessmentAgent)
      {
        if (pspInterface != null)
        {
          // try static file first
          String orgFilename = createAbsoluteFilename(ORG_FILENAME);
          File orgFile = new File(orgFilename);
          if (orgFile.exists())
          {
            orgTreeStructure = TreeUtilities.readFromFile(orgFilename);
          }
          else
          {
            // if org file does not exist, regenerate from society
            orgTreeStructure = OrgRelationshipFormatter.
                                  getOrgTreeFromAssessmentAgent(pspInterface, timeout);
          }
        }
        else
        {
          orgTreeStructure = new Structure();
          ListElement le = new ListElement();
          le.addAttribute(new Attribute("UID", "No Aggregator"));
          orgTreeStructure.addChild(le);
        }
      }
      else
      {
        orgTreeStructure =
          DBInterface.createTree(DBInterface.getTableName("org"));
      }
      orgTree = TreeUtilities.createTree(orgTreeStructure);
    }

    public static void saveOrgTree()
    {
      TreeUtilities.saveToFile(orgTreeStructure,
                               createAbsoluteFilename(ORG_FILENAME));
    }

    public static boolean isMetric(String s)
    {
      return rawMetrics.contains(s) || MetricInfo.isDerived(s);
    }

    public static Vector getAllMetrics()
    {
      Vector allMetrics = (Vector)rawMetrics.clone();
      Enumeration keys = MetricInfo.derivedMetrics.keys();
      while (keys.hasMoreElements())
      {
        allMetrics.add(keys.nextElement());
      }
      return allMetrics;
    }

    public static DefaultMutableTreeNode makeMetricTree()
    {
      Vector metrics = getAllMetrics();

      DefaultMutableTreeNode p;
      DefaultMutableTreeNode groupB;
      DefaultMutableTreeNode groupC;
      DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
      root.add(p = new DefaultMutableTreeNode(MetricInfo.GROUPA));
      root.add(groupB = new DefaultMutableTreeNode(MetricInfo.GROUPB));
      root.add(groupC = new DefaultMutableTreeNode(MetricInfo.GROUPC));

      for (int i = 0; i < metrics.size(); i++)
      {
        String metric = (String)metrics.elementAt(i);

        Hashtable mu = MetricInfo.metricUnits;
        if (mu.get(metric).equals(mu.get(MetricInfo.GROUPA)))
        {
          p.add(getDerivedMetricTree(metric, 0));
        }
        else if (mu.get(metric).equals(mu.get(MetricInfo.GROUPB)))
        {
          groupB.add(getDerivedMetricTree(metric, 0));
        }
        else if (mu.get(metric).equals(mu.get(MetricInfo.GROUPC)))
        {
          groupC.add(getDerivedMetricTree(metric, 0));
        }
      }

      return root;
    }

    private static
      DefaultMutableTreeNode getDerivedMetricTree(String metric, int count)
    {
      Hashtable ht = new SelectableHashtable("UID");
      ht.put("UID", metric);
      ht.put("ID", String.valueOf(count++));
      DefaultMutableTreeNode metricNode = new DefaultMutableTreeNode(ht);

      if (MetricInfo.isDerived(metric))
      {
        Vector metricFormula = (Vector)MetricInfo.derivedMetrics.get(metric);

        for (int i = 0; i < metricFormula.size(); i++)
        {
          String item = (String)metricFormula.elementAt(i);
          if (isMetric(item))
          {
            metricNode.add(getDerivedMetricTree(item, count * 10));
          }
        }
      }
      return metricNode;
    }

    private static Object[] createAggLabels()
    {
        Vector aggLabels = getAllMetrics();
        aggLabels.add(MetricInfo.GROUPA);
        aggLabels.add(MetricInfo.GROUPB);
        aggLabels.add(MetricInfo.GROUPC);

        return aggLabels.toArray();
    }

    /**
     * update aggregation schemes and labels to include latest derived metrics
     */
    public static void updateAggSchemes()
    {
        aggregationSchemeLabels = createAggLabels();

        for (int i = 0; i < aggregationSchemeLabels.length; i++)
        {
            String metric = (String)aggregationSchemeLabels[i];
            if (aggregationSchemes.get(metric) == null)
            {
                String units = (String)MetricInfo.metricUnits.get(metric);
                aggregationSchemes.put(metric, createDefaultAggScheme(units));
            }
        }
    }

    public static Hashtable createDefaultAggSchemes()
    {
        Hashtable aggSchemes = new Hashtable();
        for (int i = 0; i < aggregationSchemeLabels.length; i++)
        {
            String metric = (String)aggregationSchemeLabels[i];
            String units = (String)MetricInfo.metricUnits.get(metric);
            aggSchemes.put(metric, createDefaultAggScheme(units));
        }

        return aggSchemes;
    }

    private static AggregationScheme createDefaultAggScheme(String units)
    {
        int defaultTimeAggregation =
            units.equals(MetricInfo.ITEM_DAY_UNITS) ?
                AggregationScheme.SUM : AggregationScheme.AVG;

        int defaultOrgAggregation =
          (useAssessmentAgent && units.equals(MetricInfo.UNITLESS)) ?
              AggregationScheme.AVG : AggregationScheme.SUM;

        return new AggregationScheme(defaultOrgAggregation,
                                     defaultTimeAggregation,
                                     AggregationScheme.NONE);
    }

    /**
     * Modify show property on every node of a tree.
     *
     * @param tn root of tree to modify.
     * @param prop new show property for all nodes.
     */
    public static void setShowProperty(DefaultMutableTreeNode tn, String prop)
    {
      SelectableHashtable ht = (SelectableHashtable)tn.getUserObject();
      ht.setSelectedProperty(prop);

      for (int i = 0; i < tn.getChildCount(); i++)
      {
        DefaultMutableTreeNode ctn = (DefaultMutableTreeNode)tn.getChildAt(i);
        setShowProperty(ctn, prop);
      }
    }

    public static String getShowProperty(DefaultMutableTreeNode tn)
    {
      SelectableHashtable ht = (SelectableHashtable)tn.getUserObject();
      return ht.getSelectedProperty();
    }

    private static Vector convertToStringVector(Collection sourceCollection)
    {
      Vector stringVector = new Vector();
      for (Iterator i = sourceCollection.iterator(); i.hasNext();)
        stringVector.addElement(i.next().toString());
      return stringVector;
    }

    private static long getCTime()
    {
      long c_time_msec = 0L;
      String cdate_property =
        pspInterface.getSystemProperty("org.cougaar.core.agent.startTime");
      String timezone_property =
        pspInterface.getSystemProperty("user.timezone");
      if ((cdate_property == null) || (timezone_property == null))
        return c_time_msec;
      TimeZone tz = TimeZone.getTimeZone(timezone_property);
      GregorianCalendar gc = new GregorianCalendar (tz);

      StringTokenizer st = new StringTokenizer (cdate_property, "/");
      String c_time_month_string = st.nextToken();
      String c_time_day_string = st.nextToken();
      String c_time_year_string = st.nextToken();

      // Month is offset from zero, others are not
      // Last three are hour, minute, second
      gc.set (Integer.parseInt (c_time_year_string),
              Integer.parseInt (c_time_month_string) - 1,
              Integer.parseInt (c_time_day_string),
              0, 0, 0);

      c_time_msec = gc.getTime().getTime();

      // This was needed to ensure that the milliseconds were set to 0
      c_time_msec = c_time_msec / 1000;
      c_time_msec *= 1000;

      return c_time_msec;
    }

    public static int convertMSecToCDate (long current_time_msec) {
      return (int) ((current_time_msec - cTime) / MILLIS_IN_DAY);
    }

    public static long convertCDateToMSec (int current_time_c) {
      return (cTime + (current_time_c * MILLIS_IN_DAY));
    }
}