package org.cougaar.logistics.ui.stoplight.society;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.util.XmlUtils;

import org.cougaar.lib.uiframework.transducer.XmlInterpreter;
import org.cougaar.lib.uiframework.transducer.elements.Structure;

import org.cougaar.logistics.ui.stoplight.client.AggregationScheme;
import org.cougaar.logistics.ui.stoplight.util.TreeUtilities;

public class OrganizationMelder extends ScheduleMelder
{
  private DefaultMutableTreeNode orgTree = null;

  public OrganizationMelder()
  {
    super("Org");
  }

  public void setAggregationScheme(String aggregationSchemeXML)
  {
    try {
      AggregationScheme aggregationScheme =
        new AggregationScheme(XmlUtils.parse(aggregationSchemeXML));
      setAggregationMethod(aggregationScheme.orgAggregation);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setOrgTree(String orgTreeXML)
  {
    try
    {
      XmlInterpreter xint = new XmlInterpreter();
      BufferedReader orgReader =
        new BufferedReader(new StringReader(orgTreeXML));
      orgTree = TreeUtilities.createTree(xint.readXml(orgReader));
      completeAggregation = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void aggregatePartiallyCorrelated(boolean timeIncluded,
                                              Collection expandedAtoms,
                                              List output)
  {
    // incoming atoms are correlated across all dimensions except for
    // org and time
    if (orgTree == null)
    {
      // enter secondary melder (aggregate over all orgs)
      List orgAggregation = new LinkedList();
      aggregate(timeIncluded, expandedAtoms, orgAggregation);
      Object orgString = ((ResultSetDataAtom)
        expandedAtoms.iterator().next()).getIdentifier("cluster");
      DataAtomUtilities.addIdentifier(orgAggregation, "Org", orgString);
      output.addAll(orgAggregation);
    }
    else
    {
      for (Enumeration e = orgTree.children(); e.hasMoreElements();)
      {
        DefaultMutableTreeNode orgNode=(DefaultMutableTreeNode)e.nextElement();
        Vector members = TreeUtilities.getSubordinateList(orgNode, false);
        members = convertToStringVector(members);
        // find atoms for this organization and aggregate
        Collection orgAtoms =
          DataAtomUtilities.extractAtomsWithIncludedId(expandedAtoms,"cluster",
                                                       members);
        DataAtomUtilities.addIdentifier(orgAtoms, "Org", orgNode.toString());
        aggregate(timeIncluded, orgAtoms, output);
      }

      // add the head quarters as well (left over in expandedAtoms collection)
      DataAtomUtilities.addIdentifier(expandedAtoms, "Org",orgTree.toString());
      aggregate(timeIncluded, expandedAtoms, output);
    }
  }
}