/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.planning.servlet.data.hierarchy.Organization;

/**
 * Constants for hierarchy connection and tables
 * @author Benjamin Lubin; last modified by: $Author: gvidaver $
 *
 * @since 2//01
 **/
public interface HierarchyConstants{

  //Constants:
  ////////////

  //Table base names:
  public static final String ORG_TABLE_NAME="org";
  public static final String ORGNAMES_TABLE_NAME="orgnames";
  public static final String ORGDESCEND_TABLE_NAME="orgdescend";
  public static final String ORGROOTS_TABLE_NAME="orgroots";

  //OrgTable columns:
  public static final String COL_ORGID = "org_id";
  public static final String COL_RELID = "related_id";
  public static final String COL_REL = "relation_type";

  public static final int ADMIN_SUBORDINATE=Organization.ADMIN_SUBORDINATE;
  public static final int SUBORDINATE=Organization.SUBORDINATE;

  //Also needed for NamesTable
  public static final String COL_PRETTY_NAME = "pretty_name";

  //Also needed for Descendents table:
  public static final String COL_DESCEND = "descendent_id";

  //Also needed for Root table:
  public static final String COL_SOCIETY = "society";
  public static final int SOC_UNKNOWN = 0;
  public static final int SOC_TOPS = 1;
  public static final int SOC_DEMAND = 2;
  public static final String[] SOCIETIES={"Unknown","Tops","Demand"};

  public static final long DEFAULT_TIMEOUT = 120000; //millis - two minutes
}
