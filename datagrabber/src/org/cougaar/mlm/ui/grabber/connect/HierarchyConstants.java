/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.grabber.connect;

import org.cougaar.planning.servlet.data.hierarchy.Organization;

/**
 * Constants for hierarchy connection and tables
 * @author Benjamin Lubin; last modified by: $Author: mthome $
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
