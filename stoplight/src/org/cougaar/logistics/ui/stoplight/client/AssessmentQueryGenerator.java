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
package org.cougaar.logistics.ui.stoplight.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.cougaar.lib.aggagent.client.ResultSetMonitor;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.query.UpdateListener;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.logistics.ui.stoplight.society.InventoryRelatedPredicate;
import org.cougaar.logistics.ui.stoplight.society.OrganizationMelder;
import org.cougaar.logistics.ui.stoplight.society.MetricFormatter;
import org.cougaar.logistics.ui.stoplight.transducer.XmlInterpreter;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;
import org.cougaar.logistics.ui.stoplight.ui.models.RangeModel;
import org.cougaar.logistics.ui.stoplight.ui.models.VariableModel;
import org.cougaar.logistics.ui.stoplight.ui.util.VariableInterfaceManager;
import org.cougaar.logistics.ui.stoplight.util.TreeUtilities;

public class AssessmentQueryGenerator extends QueryGenerator
{
  public static boolean debug = true;
  private static boolean includeSubordinates = true;
  private static boolean disableOrgAxisAgg = false;

  private TransResultSetTableModel rsTableModel;

  // persistent query support
  private static ResultSetMonitor resultSetMonitor = null;
  private String queryId = null;
  private Thread shutdownHook = new Thread() {
      public void run()
      {
        AssessmentQueryGenerator.this.finalize();
      }
    };

  static
  {
    newPSPInterface();
  }

  public AssessmentQueryGenerator()
  {
    super(new TransResultSetTableModel());
    rsTableModel = (TransResultSetTableModel)getTableModel();

    // Make sure that finalizer is called upon program exit.
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public static void newPSPInterface()
  {
    if (AssessmentDataSource.pspInterface != null)
    {
      resultSetMonitor =
        AssessmentDataSource.pspInterface.createResultSetMonitor();
    }
  }

  public void setIncludeSubordinates(boolean includeSubordinates)
  {
    this.includeSubordinates = includeSubordinates;
  }

  public void setDisableOrgAxisAgg(boolean disableOrgAxisAgg)
  {
    this.disableOrgAxisAgg = disableOrgAxisAgg;
  }

  public void finalize()
  {
    // Free resources on the aggregation agent before leaving.
    resultSetMonitor.cancel();
    cancelQueryMonitor();

    if (!shutdownHook.isAlive())
    {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
  }

  public void cancelQueryMonitor()
  {
    if (queryId != null)
    {
      AssessmentDataSource.pspInterface.cancelQuery(queryId);
      queryId = null;
    }
  }

  /**
    * Generate an assessment agent query based on the selected variable values
    * referenced by the given variable interface manager.  Set the result set
    * of this query in the query generator's assessment table model.  Further
    * manipulate the result set as required through the database table model
    * transformation methods.
    *
    * @param vim the variable interface manager from which the variable
    *            value selections will be gathered.
    * @param postQueryRunnable runnable to run after query is complete
    */
  public void generateQuery(VariableInterfaceManager vim,
                            Runnable postQueryRunnable)
  {
    generateQuery(vim, postQueryRunnable, 0, false);
  }

  /**
    * Generate an assessment agent query based on the selected variable values
    * referenced by the given variable interface manager.  Set the result set
    * of this query in the query generator's assessment table model.  Further
    * manipulate the result set as required through the database table model
    * transformation methods.
    *
    * @param vim the variable interface manager from which the variable
    *            value selections will be gathered.
    * @param postQueryRunnable runnable to run after query is complete
    * @param timeout period to wait for non-monitored queries
    * @param monitor use a persistent query and monitor results if true
    *
    * @return String containing any error messages generated during query
    */
  public String generateQuery(final VariableInterfaceManager vim,
                            final Runnable postQueryRunnable,
                            long  timeout, boolean monitor)
  {
    System.out.println("GENERATING ASSESSMENT QUERY");

    if (AssessmentDataSource.pspInterface == null)
    {
      System.out.println(
        "No valid aggregation agent found; check name server url");

      // display empty result set in table
      displayResultSetLater(new AggregationResultSet(), vim, false,
                            postQueryRunnable);
      return null;
    }

    // determine if this is a multiple metric query
    VariableModel metricDesc = vim.getDescriptor("Metric");
    final boolean multiMetric =
      ((metricDesc.getValue() instanceof DefaultMutableTreeNode) &&
       !((DefaultMutableTreeNode)metricDesc.getValue()).isLeaf());

    // get aggregation scheme based on metric selected
    aggregationScheme = (AggregationScheme)
      AssessmentDataSource.aggregationSchemes.
        get(metricDesc.getValue().toString());

    // create assessment query
    AggregationQuery query =
      new AggregationQuery(monitor?QueryType.PERSISTENT:QueryType.TRANSIENT);

    query.setTimeout(timeout);
    
    // configure query based on user preferences
    if (monitor)
    {
      if (UpdateEditor.usePushMethod)
      {
        query.setUpdateMethod(UpdateMethod.PUSH);
      }
      else
      {
        query.setUpdateMethod(UpdateMethod.PULL);
        query.setPullRate(UpdateEditor.pullRate);
      }
    }

    // generate unary predicate
    VariableModel itemDesc = vim.getDescriptor("Item");
    ScriptSpec unaryPredicateSpec = generateUnaryPredicate(itemDesc);
    query.setPredicateSpec(unaryPredicateSpec);

    // generate formatter
    VariableModel timeDesc = vim.getDescriptor("Time");
    ScriptSpec formatter =
      generateFormatter(multiMetric, metricDesc.getValue(), aggregationScheme,
                        (RangeModel)timeDesc.getValue(),
                        timeDesc.getState() == VariableModel.FIXED, itemDesc);
    query.setFormatSpec(formatter);

    // generate org aggregator (if needed)
    VariableModel orgDesc = vim.getDescriptor("Org");
    TreeNode selectedOrgNode = (TreeNode)orgDesc.getValue();
    boolean orgAggDisabled =
      disableOrgAxisAgg && (orgDesc.getState() != VariableModel.FIXED);
    if (!selectedOrgNode.isLeaf() && includeSubordinates && !orgAggDisabled)
    {
      ScriptSpec orgAggregator =
        generateOrgAggregator(multiMetric, aggregationScheme, orgDesc);
      query.setAggSpec(orgAggregator);
    }

    // set source cluster list based on org variable model
    Vector orgs = null;
    if (includeSubordinates)
    {
        orgs = TreeUtilities.getSubordinateList(selectedOrgNode, false);
    }
    else
    {
        orgs = new Vector();
        orgs.add(selectedOrgNode);
    }
    for (int i = 0; i < orgs.size(); i++)
    {
      String selectedOrg = orgs.elementAt(i).toString();
      if (AssessmentDataSource.validClusters.contains(selectedOrg))
      {
        query.addSourceCluster(selectedOrg);
      }
    }

    if (debug)
      System.out.println("Generated Query:\n" + query.toXml());

    // cancel old query (if it exists) and stop monitoring previous result set.
    // (monitoring will automatically stop when query is removed)
    cancelQueryMonitor();

    String errorStr = null;
    // don't send a query without any source clusters
    if (!query.getSourceClusters().hasMoreElements())
    {
      // display empty result set in table
      displayResultSetLater(new AggregationResultSet(), vim, multiMetric,
                            postQueryRunnable);
    }
    else if (!monitor)
    {
      // send query to assessment agent
      AggregationResultSet resultSet = (AggregationResultSet)
        AssessmentDataSource.pspInterface.createQuery(query);

      // display result set in table
      displayResultSetLater(resultSet, vim, multiMetric, postQueryRunnable);
      
      if (resultSet.exceptionThrown()) {
        errorStr = "";
        Map exceptions = resultSet.getExceptionMap(); 
        Iterator iter = exceptions.values().iterator();
        int i = 0;
        while (iter.hasNext() && i++ < 20)
          errorStr = errorStr + iter.next().toString() + "\n";
        if (iter.hasNext())
          errorStr = errorStr + "\n[Only first 20 errors shown out of " + exceptions.size() + " total]\n";
      }
    }
    else
    {
      queryId = (String)AssessmentDataSource.pspInterface.createQuery(query);
      AggregationResultSet liveResultSet =
        resultSetMonitor.monitorResultSet(queryId);

      // display result set in table
      displayResultSetLater(liveResultSet, vim, multiMetric,
                            postQueryRunnable);

      // update display if resultSetChanges
      liveResultSet.addUpdateListener(new UpdateListener() {
          public void objectAdded(Object sourceObject)
          {
            objectChanged(sourceObject);
          }
          public void objectChanged(Object sourceObject)
          {
            AggregationResultSet updatedResultSet =
              (AggregationResultSet)sourceObject;
            System.out.println("Result Set Changed");
            displayResultSetLater(updatedResultSet, vim, multiMetric,
                                  postQueryRunnable);
          }
          public void objectRemoved(Object sourceObject) {}
        });
    }

    System.out.println("ASSESSMENT QUERY COMPLETE");
    
    return errorStr;
  }

  private void displayResultSetLater(final AggregationResultSet resultSet,
                                     final VariableInterfaceManager vim,
                                     final boolean multiMetric,
                                     final Runnable postQueryRunnable)
  {
    // these updates must be made in the swing event thread
    SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          displayResultSet(resultSet, vim, multiMetric);
          postQueryRunnable.run();
        }
      });
  }

  private void displayResultSet(AggregationResultSet resultSet,
                                VariableInterfaceManager vim,
                                boolean multiMetric)
  {
    VariableModel metricDesc = vim.getDescriptor("Metric");
    VariableModel itemDesc = vim.getDescriptor("Item");

    if (resultSet.exceptionThrown())
    {
      if (debug)
      {
        System.out.println("Exception(s) Reported By Assessment Agent:\n");
        System.out.println(resultSet.getExceptionSummary());
      }
    }

    // set result set in table model
    rsTableModel.setResultSet(resultSet);
    if (rsTableModel.getRowCount() == -1)
      return; // don't try to transform data that does not exist

    // expand all schedule elements into individual day values
    final int startColumn = rsTableModel.getColumnIndex("start time");
    final int endColumn = rsTableModel.getColumnIndex("end time");
    if ((startColumn != -1) && (endColumn != -1))
    {
      TransResultSetTableModel.Expander expander =
        new TransResultSetTableModel.Expander() {
          public Vector expand(Vector row)
          {
            Vector newRows = new Vector();

            try {
              int startTime =
                Integer.parseInt((String)row.elementAt(startColumn));
              int endTime = Integer.parseInt((String)row.elementAt(endColumn));
              if (startTime == endTime) endTime++;
              for (int day = startTime; day < endTime; day++)
              {
                Vector newRow = new Vector(row);
                newRow.setElementAt(new Integer(day), startColumn);
                newRow.setElementAt(new Integer(day), endColumn);
                newRows.addElement(newRow);
              }
            } catch (Exception e) {
              newRows.add(row);
            }

            return newRows;
          }
        };
      rsTableModel.expandAllRows(expander);
      rsTableModel.removeColumn(endColumn);
    }

    // convert from atom ids to headers
    int newStartColumn = rsTableModel.getColumnIndex("start time");
    if (newStartColumn != -1)
      rsTableModel.setColumnName(newStartColumn, "Time");
    if (rsTableModel.getColumnIndex("Org") == -1)
     rsTableModel.setColumnName(rsTableModel.getColumnIndex("cluster"), "Org");
    rsTableModel.setColumnName(rsTableModel.getColumnIndex("item"), "Item");

    VariableModel xAxis =
      (VariableModel)vim.getDescriptors(VariableModel.X_AXIS).nextElement();
    VariableModel yAxis =
      (VariableModel)vim.getDescriptors(VariableModel.Y_AXIS).nextElement();

    // transform based on needed X and Y variables
    if (debug) System.out.println("Transforming based on needed X/Y");
    String xColumnName = xAxis.getName();
    String yColumnName = yAxis.getName();
    String metric = metricDesc.getValue().toString();
    if (yAxis.getName().equals("Metric") &&
        (rsTableModel.getColumnIndex("Metric") == -1))
    {
      if (multiMetric)
      {
        addMetricColumn(((DefaultMutableTreeNode)
                        metricDesc.getValue()).getFirstChild().toString());
      }
      else
      {
        addMetricColumn(metric);
      }
    }
    int[] yColumns =
      (vim.getDescriptor("Item").getState() == VariableModel.FIXED) ?
        new int[]{rsTableModel.getColumnIndex(yColumnName),
                  rsTableModel.getColumnIndex("Item")} :
        new int[]{rsTableModel.getColumnIndex(yColumnName)};
    rsTableModel.setXY(rsTableModel.getColumnIndex(xColumnName), yColumns,
                       rsTableModel.getColumnIndex("rate"));

    // convert column and row header ids to names
    boolean itemsAggregated =
      aggregationScheme.itemAggregation != AggregationScheme.NONE;
    if (debug) System.out.println("Converting column headers to names");
    boolean aggregated = !(xAxis.getName().equals("Item") && !itemsAggregated);
    convertColumnHeaderIDsToNames(xAxis, rsTableModel, yColumns.length,
                                  "UID", aggregated);
    if (debug) System.out.println("Converting row headers to names");
    aggregated = !(yAxis.getName().equals("Item") && !itemsAggregated);
    convertRowHeaderIDsToNames(yAxis, rsTableModel, 0, "UID", aggregated);
    if (yColumns.length > 1)
    {
      convertRowHeaderIDsToNames(itemDesc, rsTableModel, 1, "UID",
                                 itemsAggregated);
    }

    // sort everything in a reasonable way (items get sorted differently)
    sortRowsAndColumns(itemDesc);

    // remove item column if it is there and not needed
    if ((itemDesc.getState() == VariableModel.FIXED) &&
        (aggregationScheme.itemAggregation != AggregationScheme.NONE))
    {
      rsTableModel.removeColumn(1);
    }

    // derive unit column if needed
    deriveUnitColumn(yColumnName, metric);

    rsTableModel.fireTableChangedEvent(
      new TableModelEvent(rsTableModel, TableModelEvent.HEADER_ROW));

  }

  private static ScriptSpec generateUnaryPredicate(VariableModel itemDesc)
  {
    // create list of items needed for query and set it as a property of
    // unary predicate
    HashMap propertyMap = new HashMap();
    String selectedItemUID =
      ((Hashtable)((DefaultMutableTreeNode)itemDesc.getValue()).
        getUserObject()).get("UID").toString();
    if (!selectedItemUID.equals("ALL_ITEMS"))
    {
      Enumeration itemList = TreeUtilities.
        getSubordinateList((TreeNode)itemDesc.getValue(),true).elements();
      Vector neededIDsVector = new Vector();
      while (itemList.hasMoreElements())
      {
        DefaultMutableTreeNode n =
          (DefaultMutableTreeNode)itemList.nextElement();
        String id = ((Hashtable)n.getUserObject()).get("UID").toString();
        neededIDsVector.add(id);
      }
      String itemListString =
        createDelimitedList(neededIDsVector.elements(), "", "", ",");
      propertyMap.put("AssetsOfInterest", itemListString);
    }
    ScriptSpec unaryPredicateSpec =
      new ScriptSpec(ScriptType.UNARY_PREDICATE,
                     InventoryRelatedPredicate.class.getName(),
                     propertyMap);

    return unaryPredicateSpec;
  }

  private static ScriptSpec
    generateFormatter(boolean multiMetric, Object metricValue,
                      AggregationScheme aggregationScheme,
                      RangeModel timeRange, boolean aggregateTime,
                      VariableModel itemDesc)
  {
    String metric = metricValue.toString();
    Vector metricNames = new Vector();
    Vector metricFormulas = new Vector();

    // handle multiple metrics if needed
    if (multiMetric)
    {
      DefaultMutableTreeNode metricNode = (DefaultMutableTreeNode)metricValue;
      Enumeration metrics = metricNode.children();
      while (metrics.hasMoreElements())
      {
        String childMetric = ((DefaultMutableTreeNode)
          metrics.nextElement()).getUserObject().toString();

        metricNames.addElement(childMetric);
        metricFormulas.addElement(generateMetricFormula(childMetric));
      }
    }
    else
    {
      metricNames.addElement(metric);
      metricFormulas.addElement(generateMetricFormula(metric));
    }

    HashMap propertyMap = new HashMap();
    String metricNamesString =
      createDelimitedList(metricNames.elements(), "", "", ",");
    propertyMap.put("MetricNames", metricNamesString);
    Vector metricFormulaStrings = new Vector();
    for (int i = 0; i < metricFormulas.size(); i++)
    {
      Vector metricFormula = (Vector)metricFormulas.elementAt(i);
      metricFormulaStrings.addElement(
        createDelimitedList(metricFormula.elements(), "", "", ","));
    }
    String metricFormulaStringsString =
      createDelimitedList(metricFormulaStrings.elements(), "", "", "|");
    propertyMap.put("MetricFormulas", metricFormulaStringsString);
    propertyMap.put("StartTime", String.valueOf(timeRange.getMin()));
    propertyMap.put("EndTime", String.valueOf(timeRange.getMax()));
    propertyMap.put("AggregateTime", String.valueOf(aggregateTime));
    propertyMap.put("AggregationScheme", aggregationScheme.toXML());
    propertyMap.put("ItemFixed",
                    String.valueOf(itemDesc.getState()==VariableModel.FIXED));

    // if not aggregating over items, tree is not needed
    DefaultMutableTreeNode itemNode =
      (DefaultMutableTreeNode)itemDesc.getValue();
    if ((aggregationScheme.itemAggregation != AggregationScheme.NONE) &&
        !itemNode.isLeaf())
    {
      // create a tree that contains only the items of interest
      String itemUID =(String)((Hashtable)itemNode.getUserObject()).get("UID");
      //String itemSubTree =
      //  generateSubtreeXML(AssessmentDataSource.itemTreeStructure, itemUID);
      propertyMap.put("ItemTree", itemUID);
    }

    ScriptSpec formatterSpec =
      new ScriptSpec(XmlFormat.INCREMENT, MetricFormatter.class.getName(),
                     propertyMap);

    return formatterSpec;
  }

  private static ScriptSpec
    generateOrgAggregator(boolean multiMetrics,
                          AggregationScheme aggregationScheme,
                          VariableModel orgDesc)
  {
    String idsToCorrelate = "item";
    if (multiMetrics) idsToCorrelate += ",Metric";

    HashMap propertyMap = new HashMap();

    // if org variable is fixed, tree is not needed
    // (aggregating over all organizations in query)
    if (orgDesc.getState() != VariableModel.FIXED)
    {
      // create a tree that contains only the orgs of interest
      String orgSubTree =
        generateSubtreeXML(AssessmentDataSource.orgTreeStructure,
                           orgDesc.getValue().toString());
      propertyMap.put("OrgTree", orgSubTree);
    }

    propertyMap.put("AggregationScheme", aggregationScheme.toXML());
    ScriptSpec orgAggregator =
      new ScriptSpec(AggType.MELDER, OrganizationMelder.class.getName(),
                     idsToCorrelate, propertyMap);

    return orgAggregator;
  }

  private static Vector generateMetricFormula(String metric)
  {
    Vector derivedMetricFormula = null;

    if (MetricInfo.isDerived(metric))
    {
      derivedMetricFormula =MetricInfo.getExpandedDerivedMetricFormula(metric);
    }
    else
    {
      derivedMetricFormula = new Vector();
      derivedMetricFormula.add(metric);
    }

    return derivedMetricFormula;
  }

  private void addMetricColumn(String metric)
  {
    rsTableModel.insertColumn(0);
    rsTableModel.setColumnName(0, "Metric");
    for (int i = 0; i < rsTableModel.getRowCount(); i++)
    {
      rsTableModel.setValueAt(metric, i, 0);
    }
  }

  private static Structure generateSubtree(Structure tree, String uid)
  {
      // create a subtree with given uid as root
      ListElement le = TreeUtilities.findListElement(uid, tree);
      Structure subtree = new Structure();
      subtree.addChild(le);
      return subtree;
  }

  private static String generateTreeXML(Structure tree)
  {
      // create tree xml string
      XmlInterpreter xint = new XmlInterpreter();
      StringWriter xmlWriter = new StringWriter();
      xint.writeXml(tree, new PrintWriter(xmlWriter));

      return xmlWriter.toString();
  }

  private static String generateSubtreeXML(Structure tree, String uid)
  {
      // create a subtree with given uid as root
      Structure subtree = generateSubtree(tree, uid);

      // create tree xml string
      return generateTreeXML(subtree);
  }
}