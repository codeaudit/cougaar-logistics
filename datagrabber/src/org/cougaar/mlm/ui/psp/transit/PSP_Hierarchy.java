/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
 
package org.cougaar.mlm.ui.psp.transit;

import java.io.*;
import java.net.*;
import java.util.*;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.glm.ldm.Constants;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;

// explicitly name ALP's org and transit's org by using the full package

import org.cougaar.planning.servlet.data.hierarchy.HierarchyData;
import org.cougaar.planning.servlet.data.hierarchy.Organization;
import org.cougaar.planning.servlet.data.xml.XMLWriter;

import org.cougaar.lib.planserver.*;

import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * PSP to gather Organizational hierarchy information from an ALP society.
 *
 * Based upon org.cougaar.mlm.ui.psp.transportation.PSP_Hierarchy.
 */
public class PSP_Hierarchy 
  extends PSP_BaseAdapter
  implements PlanServiceProvider, UISubscriber
{

  public static final boolean DEBUG = false;

  public static boolean VERBOSE = false;

  static {
    VERBOSE = Boolean.getBoolean("org.cougaar.psp.hierarchy.verbose");
  }

    /** 
   * A zero-argument constructor is required for dynamically loaded PSPs,
   * required by Class.newInstance()
   **/
  public PSP_Hierarchy() {
    super();
  }

  private static final UnaryPredicate selfOrgP =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof org.cougaar.glm.ldm.asset.Organization) &&
           ((org.cougaar.glm.ldm.asset.Organization)o).isSelf());
      }
    };


  public void execute(PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception
  {
    // parse parameters
    MyPSPState myState = new MyPSPState(this, query_parameters, psc);
    myState.configure(query_parameters);

    if (myState.anyArgs) {
      if (VERBOSE) {
        System.out.println(
          "BEGIN hierarchy at "+myState.clusterID);
      }
      execute(out, myState);
      if (VERBOSE) {
        System.out.println(
          "FINISHED hierarchy at "+myState.clusterID);
      }
    } else {
      getUsage(out, myState);
    }
  }

  /** USAGE **/
  private void getUsage(PrintStream out, MyPSPState myState)
  {
    out.print(
      "<HTML><HEAD><TITLE>Hierarchy Usage</TITLE></HEAD><BODY>\n"+
      "<H2><CENTER>Hierarchy Usage</CENTER></H2><P>\n"+
      "<FORM METHOD=\"POST\" ACTION=\"");
    out.print(myState.cluster_psp_url);
    out.print(
      "?POST"+
      "\">\n"+
      "Show organization hierarchy for:<p>\n"+
      "&nbsp;&nbsp;<INPUT TYPE=\"radio\" NAME=\"recurse\" "+
        "VALUE=\"true\" CHECKED>"+
      "&nbsp;All related clusters<p>\n"+
      "&nbsp;&nbsp;<INPUT TYPE=\"radio\" NAME=\"recurse\" "+
        "VALUE=\"false\">"+
      "&nbsp;Just ");
    out.print(myState.clusterID);
    out.print(
      "<P>\n"+
      "</INPUT>\n"+
      "<INPUT TYPE=\"hidden\" NAME=\"html\" VALUE=\"true\">\n"+
      "<INPUT TYPE=\"submit\" NAME=\"Display PSP\">\n"+
      "</FORM></BODY></HTML>");
  }

  /**
   * get the self organization.
   */
  protected org.cougaar.glm.ldm.asset.Organization getSelfOrg(
      MyPSPState myState) {
    // get self org
    Collection col =
      myState.psc.getServerPluginSupport().queryForSubscriber(
          selfOrgP);
    if ((col != null) && 
        (col.size() == 1)) {
      Iterator iter = col.iterator();
      org.cougaar.glm.ldm.asset.Organization org = 
        (org.cougaar.glm.ldm.asset.Organization)iter.next();
      return org;
    } else {
      return null;
    }
  }

  /**
   * Fetch HierarchyData and write to output.
   */
  protected void execute(
      PrintStream out, MyPSPState myState) {
    // get self org
    org.cougaar.glm.ldm.asset.Organization selfOrg = 
      getSelfOrg(myState);
    if (selfOrg == null) {
      throw new RuntimeException("No self org?");
    }
    // get hierarchy data
    HierarchyData hd = getHierarchyData(myState, selfOrg);
    // write data
    try {
      if (myState.format == MyPSPState.FORMAT_DATA) {
        // serialize
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(hd);
        oos.flush();
      } else {
        // xml or html-wrapped xml
        XMLWriter w;
        if (myState.format == MyPSPState.FORMAT_HTML) {
          // wrapped xml
          out.println(
            "<HTML><HEAD><TITLE>Hierarcy at "+
            myState.clusterID+
            "</TITLE></HEAD><BODY>\n"+
            "<H2><CENTER>Hierarchy at "+
            myState.clusterID+
            "</CENTER></H2><p><pre>\n");
          w = 
            new XMLWriter(
              new OutputStreamWriter(
                new XMLtoHTMLOutputStream(out)),
              true);
        } else {
          // raw xml
          out.println("<?xml version='1.0'?>");
          w = 
            new XMLWriter(
              new OutputStreamWriter(out));
        }
        // write as xml
        hd.toXML(w);
        w.flush();
        if (myState.format == MyPSPState.FORMAT_HTML) {
          out.println(
            "\n</pre></BODY></HTML>\n");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected HierarchyData getHierarchyData(
      MyPSPState myState, 
      org.cougaar.glm.ldm.asset.Organization selfOrg) {

    // build list of orgs
    HierarchyData hd = new HierarchyData();

    // create a "self" org
    String selfOrgName = 
	//        selfOrg.getItemIdentificationPG().getNomenclature();
	selfOrg.getClusterPG().getClusterIdentifier().toString();
    org.cougaar.planning.servlet.data.hierarchy.Organization toOrg = 
      new org.cougaar.planning.servlet.data.hierarchy.Organization();
    toOrg.setUID(selfOrgName);
    // where is the pretty name kept?
    toOrg.setPrettyName(selfOrgName); 

    // set rootId as self
    hd.setRootOrgID(selfOrgName);

	MutableTimeSpan mts = new MutableTimeSpan ();
    RelationshipSchedule schedule = 
      selfOrg.getRelationshipSchedule();
	
	Collection subordinates = new HashSet ();
	
    Collection subordinates1 = 
      schedule.getMatchingRelationships(Constants.Role.SUBORDINATE,
										mts.getStartTime(), mts.getEndTime());
	subordinates.addAll (subordinates1);
	
	Collection subordinates2 = 
	  schedule.getMatchingRelationships(Constants.Role.ADMINISTRATIVESUBORDINATE,
										mts.getStartTime(), mts.getEndTime());

	subordinates.addAll (subordinates2);
	
    // add self org to hierarchy
    hd.addOrgData(toOrg);

    if (subordinates.isEmpty()) {
      // no subordinates
      return hd;
    }

    Set recurseSubOrgSet = 
      (myState.recurse ? (new HashSet()) : null);

    // Safe to iterate over subordinates because getSubordinates() returns
    // a new Collection.
    for (Iterator schedIter = subordinates.iterator(); 
        schedIter.hasNext();
        ) {
      Relationship relationship = (Relationship)schedIter.next();
      Asset subOrg = (Asset)schedule.getOther(relationship);
      String subOrgName = 
	  //        subOrg.getItemIdentificationPG().getNomenclature();
	subOrg.getClusterPG().getClusterIdentifier().toString();

      if (!(selfOrgName.equals(subOrgName))) {
        String role = relationship.getRoleA().getName();
        // client wants a numerical identifier for the role
        int roleId;
        if (role.equalsIgnoreCase("AdministrativeSubordinate")) {
          // admin_subord
          roleId = 
            org.cougaar.planning.servlet.data.hierarchy.Organization.ADMIN_SUBORDINATE;
        } else {
          // some other subord type
          //   ** add more String.equals cases here **
          roleId = 
            org.cougaar.planning.servlet.data.hierarchy.Organization.SUBORDINATE;
        }
        toOrg.addRelation(subOrgName, roleId);
        if (recurseSubOrgSet != null) {
          recurseSubOrgSet.add(subOrgName);
        }
      }
    }

    if (recurseSubOrgSet != null) {
      // add subordinate orgs to hierarchy
      for (Iterator iter = recurseSubOrgSet.iterator(); 
           iter.hasNext();
           ) {
        String subOrgName = (String)iter.next();
        // fetch the sub's data
        HierarchyData subHD = fetchForSubordinate(myState, subOrgName);
        // take Orgs from sub's hierarchy data
        int nSubHD = ((subHD != null) ? subHD.numOrgs() : 0);
        for (int i = 0; i < nSubHD; i++) {
          hd.addOrgData(subHD.getOrgDataAt(i));
        }
      }
    }

    // return list
    return hd;
  }

  protected HierarchyData fetchForSubordinate(
      MyPSPState myState, 
      String subOrgName) {
    try {
      // build URL for remote connection
      StringBuffer buf = new StringBuffer();
      buf.append(myState.base_url).append("$");
      buf.append(subOrgName).append(myState.psp_path);
      buf.append("?data?recurse");

      if (VERBOSE) {
        System.out.println(
          "In "+myState.clusterID+
          ", fetch hierarchy from "+subOrgName+
          ", URL: "+buf.toString());
      }

      // open connection
      URL myURL = new URL(buf.toString());
      URLConnection myConnection = myURL.openConnection();
      InputStream is = myConnection.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(is);

      // read single HierarchyData Object from subordinate
      HierarchyData hd = (HierarchyData)ois.readObject();

      if (VERBOSE) {
        System.out.println(
          "In "+myState.clusterID+
          ", got "+
          ((hd != null) ?
           ("hierarchy["+hd.numOrgs()+"]") :
           ("null"))+
          " from "+subOrgName);
      }

      return hd;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Convert XML to HTML-friendly output.
   *
   * Taken from PSP_PlanView.  For Internet Explorer this isn't such
   * a big deal...
   */
  protected static class XMLtoHTMLOutputStream 
      extends FilterOutputStream {
    protected static final byte[] LESS_THAN;
    protected static final byte[] GREATER_THAN;
    static {
      LESS_THAN = "<font color=green>&lt;".getBytes();
      GREATER_THAN = "&gt;</font>".getBytes();
    }
    public XMLtoHTMLOutputStream(OutputStream o) {
      super(o);
    }
    public void write(int b) throws IOException {
      if (b == '<') {
        out.write(LESS_THAN);
      } else if (b == '>') {
        out.write(GREATER_THAN);
      } else {
        out.write(b);
      }
    }
  }

  /** holds PSP state (i.e. url flags for MODE, etc) **/
  protected static class MyPSPState extends PSPState {

    /** my additional fields **/
    public boolean anyArgs;
    public int format;
    public boolean recurse;

    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    /** constructor **/
    public MyPSPState(
        UISubscriber xsubscriber,
        HttpInput query_parameters,
        PlanServiceContext xpsc) {
      super(xsubscriber, query_parameters, xpsc);
    }

    /** use a query parameter to set a field **/
    public void setParam(String name, String value) {
      //super.setParam(name, value);
      if (eq("recurse", name)) {
        anyArgs = true;
        recurse = 
          ((value == null) || 
           (eq("true", value)));
      } else if (eq("format", name)) {
        anyArgs = true;
        if (eq("data", value)) {
          format = FORMAT_DATA;
        } else if (eq("xml", value)) {
          format = FORMAT_XML;
        } else if (eq("html", value)) {
          format = FORMAT_HTML;
        }
      // stay backwards-compatable
      } else if (eq("data", name)) {
        anyArgs = true;
        format = FORMAT_DATA;
      } else if (eq("xml", name)) {
        anyArgs = true;
        format = FORMAT_XML;
      } else if (eq("html", name)) {
        anyArgs = true;
        format = FORMAT_HTML;
      }
    }

    // startsWithIgnoreCase
    private static final boolean eq(String a, String b) {
      return a.regionMatches(true, 0, b, 0, a.length());
    }
  }


  //
  // uninteresting and/or obsolete methods 
  //
  public PSP_Hierarchy(
      String pkg, String id) 
       throws RuntimePSPException {
    setResourceLocation(pkg, id);
  }
  public boolean test(
      HttpInput query_parameters, PlanServiceContext sc) {
    super.initializeTest();
    return false;
  }
  public void subscriptionChanged(Subscription subscription) {
  }
  public boolean returnsXML() {
    return true;
  }
  public boolean returnsHTML() {
    return false;
  }
  public String getDTD()  {
    return null;
  }

}

