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
package org.cougaar.logistics.ui.stoplight.society;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cougaar.logistics.ui.stoplight.client.AggregationScheme;

public class ItemMelder extends ScheduleMelder
{
  private DefaultMutableTreeNode itemTree = null;

  public ItemMelder()
  {
    super("item");
  }

  public void setItemTree(DefaultMutableTreeNode itemTree)
  {
    this.itemTree = itemTree;
  }

  public void setItemFixed(boolean itemFixed)
  {
    completeAggregation = itemFixed;
  }

  protected void
    aggregatePartiallyCorrelated(boolean timeIncluded,
                                 Collection expandedAtoms, List output)
  {
    // incoming atoms are correlated across all dimensions except for
    // item and time
    if (completeAggregation)
    {
      // aggregate over all items in tree
      aggregateItems(expandedAtoms, itemTree, timeIncluded, output,
                     getItemString(itemTree));
    }
    else
    {
      // aggregate over each child branch
      for (Enumeration e = itemTree.children(); e.hasMoreElements();)
      {
        DefaultMutableTreeNode itemNode=(DefaultMutableTreeNode)e.nextElement();
        aggregateItems(expandedAtoms, itemNode, timeIncluded, output,
                       getItemString(itemNode));
      }
    }
  }

  private void aggregateItems(Collection expandedAtoms,
                              DefaultMutableTreeNode itemNode,
                              boolean timeIncluded, List output,
                              String aggregatedItemId)
  {
    Collection aggregatedItems =
      aggregateItems(expandedAtoms, itemNode, timeIncluded);
    DataAtomUtilities.addIdentifier(aggregatedItems, "item", aggregatedItemId);
    output.addAll(aggregatedItems);
  }

  private Collection aggregateItems(Collection expandedAtoms,
                                    DefaultMutableTreeNode itemNode,
                                    boolean timeIncluded)
  {
    List aggregatedItems = new LinkedList();
    String itemString = getItemString(itemNode);
    if (itemNode.isLeaf())
    {
      Collection itemAtoms =
        DataAtomUtilities.extractAtomsWithId(expandedAtoms, "item",itemString);
      return itemAtoms;
    }
    else
    {
      Collection atomsToAggregate = new LinkedList();
      for (Enumeration children = itemNode.children();
           children.hasMoreElements();)
      {
        DefaultMutableTreeNode child =
          (DefaultMutableTreeNode)children.nextElement();
        Collection childGroupAtomsToAggregate =
          aggregateItems(expandedAtoms, child, timeIncluded);
        if (aggregationMethod == AggregationScheme.WAVG)
        {
          DataAtomUtilities.addIdentifier(childGroupAtomsToAggregate,
                                          "weight", getItemWeight(child));
        }
        atomsToAggregate.addAll(childGroupAtomsToAggregate);
      }
      aggregate(timeIncluded, atomsToAggregate, aggregatedItems);
    }

    return aggregatedItems;
  }

  private static String getItemString(DefaultMutableTreeNode node)
  {
    return (String)((Hashtable)node.getUserObject()).get("UID");
  }

  private static Double getItemWeight(DefaultMutableTreeNode node)
  {
    Object weightObject = ((Hashtable)node.getUserObject()).get("WEIGHT");
    if (weightObject == null)
    {
      System.out.println("WARNING: weight not set for " + getItemString(node) +
                         " using 0");
      return new Double(0);
    }
    return new Double((String)weightObject);
  }
}