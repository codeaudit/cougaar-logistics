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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.glm.ldm.Constants.Role;
import org.cougaar.glm.ldm.asset.Organization;

import org.cougaar.lib.uiframework.transducer.*;
import org.cougaar.lib.uiframework.transducer.configs.*;
import org.cougaar.lib.uiframework.transducer.elements.*;

import org.cougaar.lib.aggagent.client.AggregationClient;
import org.cougaar.lib.aggagent.session.XMLEncoder;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.logistics.ui.stoplight.society.ClassPredicate;
import org.cougaar.logistics.ui.stoplight.society.DataAtomUtilities;
import org.cougaar.logistics.ui.stoplight.society.ExtractionHelper;

public class OrgRelationshipFormatter implements XMLEncoder
{
  public void encode(Object o, Collection out)
  {
    Organization org = (Organization)o;
    if (org.isLocal())
    {
      Collection das = ExtractionHelper.getOrgRelationships(org);
      ResultSetDataAtom lastAtom = null;
      for (Iterator i = das.iterator(); i.hasNext();)
      {
        ResultSetDataAtom da = (ResultSetDataAtom)i.next();
        if (!isSuperiorRelationship(da))
        {
          lastAtom = da;
          i.remove();
        }
      }

      // check if organization has no superior
      if (das.isEmpty())
      {
        lastAtom.addIdentifier("type", Role.SUPERIOR.getName());
        lastAtom.addValue("other", "All Units");
        das.add(lastAtom);
      }

      out.addAll(das);
    }
  }

  public static boolean isSuperiorRelationship(ResultSetDataAtom da)
  {
    Object relType = da.getIdentifier("type");
    return (relType.equals(Role.SUPERIOR.getName()) ||
            relType.equals(Role.ADMINISTRATIVESUPERIOR.getName()));
  }

  public static Structure
    getOrgTreeFromAssessmentAgent(AggregationClient pspInterface, long timeout)
  {
    Collection validClusters = pspInterface.getClusterIds();
    AggregationQuery query = new AggregationQuery(QueryType.TRANSIENT);
    query.setTimeout(timeout);
    for (Iterator i = validClusters.iterator(); i.hasNext();)
    {
      query.addSourceCluster((String)i.next());
    }
    HashMap propertyMap = new HashMap();
    propertyMap.put("Class", Organization.class.getName());
    ScriptSpec unarySpec =
      new ScriptSpec(ScriptType.UNARY_PREDICATE,
                     ClassPredicate.class.getName(), propertyMap);
    query.setPredicateSpec(unarySpec);
    ScriptSpec formatSpec =
      new ScriptSpec(XmlFormat.XMLENCODER,
                     OrgRelationshipFormatter.class.getName(), null);
    query.setFormatSpec(formatSpec);

    AggregationResultSet rs =
      (AggregationResultSet)pspInterface.createQuery(query);

    return createOrgTree(rs);
  }

  private static Structure createOrgTree(AggregationResultSet rs)
  {
    // create vector of org descriptors
    Vector orgs = new Vector();
    for (Iterator i = rs.getAllAtoms(); i.hasNext();)
    {
      ResultSetDataAtom da = (ResultSetDataAtom)i.next();
      String otherOrg = da.getValue("other").toString();
      if (otherOrg.startsWith("UIC/"))
      {
        otherOrg = otherOrg.substring(4);
      }
      orgs.addElement(
        new OrgDesc(da.getIdentifier("cluster"), otherOrg));
    }

    // Create org structure
    Structure orgStruc = new Structure();
    orgStruc.addAttribute(new Attribute("hierarchy", "Organization"));
    ListElement root = new ListElement();
    orgStruc.addChild(root);
    root.addAttribute(new Attribute("UID", "All Units"));

    int oldOrgSize = 0;
    while (orgs.size() > 0)
    {
      if (orgs.size() != oldOrgSize) // check for progress
      {
        oldOrgSize = orgs.size();
        for (int i=0; i < orgs.size(); i++)
        {
          OrgDesc od = (OrgDesc)orgs.elementAt(i);
          boolean inserted = insertOrgIfPossible(od, root);
          if (inserted)
          {
            orgs.remove(od);
            i--;
          }
        }
      }
      else
      {
        System.out.println("################");
        System.out.println("Could not find referenced superior(s) for the " +
                           "following organization(s) (SUPERIOR_ID->ORG_ID):");
        for (int i=0; i < orgs.size(); i++)
        {
            OrgDesc od = (OrgDesc)orgs.elementAt(i);
            System.out.print("     ");
            System.out.println(od);
        }
        System.out.println("################");
        break;
      }
    }

    return orgStruc;
  }

  private static boolean insertOrgIfPossible(OrgDesc od, ListElement le)
  {
    Attribute a = le.getAttribute("UID");
    Enumeration vs = a.getChildren();
    String parentUID = ((Element)vs.nextElement()).getAsValue().getValue();

    if (parentUID.equals(od.parent))
    {
      ListElement newListElement = new ListElement();
      newListElement.addAttribute(new Attribute("UID", (String)od.name));
      le.addChild(newListElement);
      return true;
    }

    Enumeration children = le.getChildren();
    while(children.hasMoreElements())
    {
      ListElement child = (ListElement)children.nextElement();
      if (insertOrgIfPossible(od, child))
      {
        return true;
      }
    }

    return false;
  }

  private static class OrgDesc
  {
    public Object name;
    public Object parent;

    public OrgDesc(Object name, Object parent)
    {
      this.name = name;
      this.parent = parent;
    }

    public String toString()
    {
      return parent + "->" + name;
    }
  }
}