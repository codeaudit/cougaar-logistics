/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.ui.stoplight.transducer.configs;

import org.cougaar.logistics.ui.stoplight.transducer.dbsupport.*;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

import java.util.*;

/**
 *  <p>
 *  An instance of this class contains the information used to configure a
 *  MappedTransducer (q.v.) so that it can interpret the fields in a database
 *  table.  Within reason, various schema may be used to store Structures in a
 *  database.
 *  </p><p>
 *  The current implementation presumes that nodes do not store structured
 *  attribute values.  In effect, a tree or matrix of nodes, each containing
 *  certain name-value pairs is presumed.  The "value" components in these
 *  pairs are stored as fields in the table containing the tree structure(s).
 *  </p>
 */
public class SqlTableMap {
  // The database table in which structures are stored
  private String dbTable = null;

  // primary keys not associated with the child-parent links in the structure
  // these are used to index particular Structures stored in a table.
  private String[] primaryKeys = null;

  // primary id field referenced by the child-parent links
  private String idKey = null;

  // field in which the parent link is stored
  private String parentKey = null;

  // join two or more tables together according to these conditions, if any
  private String joinConditions = null;

  // in case of a join, specify which one is the major table
  private String primaryTableName = null;

  // Names of the fields that comprise the content of a node and the attributes
  // they represent.
  private Vector attNames = new Vector();
  private Vector colNames = new Vector();

  /**
   *  Specify the name of the DB table where Structures are or will be stored.
   *  The structure of the table must be consistent with the rest of the
   *  configuration values in order for the transducer to work properly.
   *  @param s the name of the table in the database.
   */
  public void setDbTable (String s) {
    dbTable = s;
  }

  /**
   *  Specify the name of the DB table that contains parent-child relationships
   *  and primary IDs.  For example, when joining tables "fred" and "john",
   *  where "fred" contains the parent-child relationships, call:
   *  <pre>
   *    setDbTable("fred, john");
   *    setPrimaryTableName("fred");
   *    setJoinConditions("fred.id = john.id");
   *  </pre>
   *  Setting this parameter is unnecessary when only one table is used or when
   *  there are no column name conflicts among the tables being joined.
   *
   *  @param s the query-local name of the primary table
   */
  public void setPrimaryTableName (String s) {
    primaryTableName = s;
  }

  /**
   *  Name the columns used to index separate Structures in the DB table.  The
   *  order of the keys as provided here is the same as the order in which
   *  their specified values will be expected when queries are performed.  If
   *  no keys are given (either an empty array or null), then it is assumed
   *  that the table will be taken up by a single Structure.
   *  @param keys the names of the index fields in canonical order.
   */
  public void setPrimaryKeys (String[] keys) {
    primaryKeys = keys;
  }

  /**
   *  Set the name of the field used in the DB to identify rows in the table.
   *  This field is the same one referenced by the "parent" field (see
   *  setParentKey) to forge parent-child relationships among the nodes.
   *  @param s the name of the field in which the rows unique ID is stored
   */
  public void setIdKey (String s) {
    idKey = s;
  }

  /**
   *  Set the name of the field indicating which row in the table contains the
   *  parent of the current row.  It does this by referencing the parent's
   *  unique ID field (see setIdKey).
   *  @param s the name of the parent pointer field
   */
  public void setParentKey (String s) {
    parentKey = s;
  }

  /**
   *  <p>
   *  Set the conditions that join multiple tables together.  If this variable
   *  is not null, then these conditions are added to the "where" clause of
   *  the query produced by the createQuery method (q.v.).
   *  </p><p>
   *  <b>Note:</b>  Altering contents of the database using the createDelete
   *  and createInsert methods is not encouraged in cases where two or more
   *  tables are joined.  All kinds of bad things can happen.
   *  </p>
   *  @param s the new joining conditions for the DB tables.
   */
  public void setJoinConditions (String s) {
    joinConditions = s;
  }

  /**
   *  Add to this configuration a pair of Strings to define a new node
   *  attribute in the Structure format.  One of these corresponds to the
   *  column in the table from which the information is to be retrieved, while
   *  the other is the name by which the attribute is known in the Structure.
   *  The same one-to-one mapping is used for storing in the database and
   *  reading from the database.
   *  @param att the name of the attribute as it appears in the Structure
   *  @param col the name of the column in which values are stored in the DB
   */
  public void addContentKey (String att, String col) {
    attNames.add(att);
    colNames.add(col);
  }

  /**
   *  Get an array containing the names of the attributes as they should appear
   *  in the Structure instances.  The order of the names in the array is the
   *  same as the order of the corresponding column names returned by method
   *  getFieldNames (q.v.).
   */
  public String[] getAttributeNames () {
    String[] names = new String[attNames.size()];
    Enumeration e = attNames.elements();
    for (int i = 0; e.hasMoreElements(); i++)
      names[i] = e.nextElement().toString();
    return names;
  }

  /**
   *  Get an array containing the names of the columns in the DB that hold
   *  values for the attributes.  The order of the names in the array is the
   *  same as the order of the corresponding attribute names returned by
   *  method getAttributeNames (q.v.).
   */
  public String[] getFieldNames () {
    String[] names = new String[colNames.size()];
    Enumeration e = colNames.elements();
    for (int i = 0; e.hasMoreElements(); i++)
      names[i] = e.nextElement().toString();
    return names;
  }

  /**
   *  Write an SQL query that will remove an entire structure from the database.
   *  @param id the major indices that identify the targetted structure
   *  @return an SQL query in String form
   */
  public String createDelete (String[] id) {
    if (joinConditions != null)
      throw new Error("Can't delete from joined tables");

    SqlDelete del = new SqlDelete();
    del.setTable(dbTable);
    if (id != null && primaryKeys != null)
      for (int i = 0; i < id.length && i < primaryKeys.length; i++)
        del.addCondition(primaryKeys[i] + " = " + id[i]);

    return del.toString();
  }

  /**
   *  Write the SQL query that requests all of the rows and columns associated
   *  with a particular structure.  Columns are selected in the order in which
   *  they were added to the configuration table (after the "id" and "parent
   *  reference" columns), and that is the same order in which they will be
   *  accessed by the MappedTransducer code.  Any specializations that deviate
   *  from this contract are likely to run into trouble.
   *  @param id the major indices that identify the targetted structure
   *  @return an SQL query in String form
   */
  public String createQuery (String[] id) {
    // create an SqlQuery ...
    SqlQuery q = new SqlQuery();

    // include the table designation, if necessary, for id and parent fields
    StringBuffer fullId = new StringBuffer();
    StringBuffer fullParent = new StringBuffer();
    if (primaryTableName != null) {
      fullId.append(primaryTableName);
      fullParent.append(primaryTableName);
      fullId.append(".");
      fullParent.append(".");
    }
    fullId.append(idKey);
    fullParent.append(parentKey);

    // include the join conditions for the tables, if necessary
    if (joinConditions != null)
      q.addCondition(joinConditions);

    q.addTable(dbTable);
    q.addSelection(fullId);
    q.addSelection(fullParent);
    for (Enumeration e = colNames.elements(); e.hasMoreElements(); )
      q.addSelection(e.nextElement());

    if (id != null && primaryKeys != null)
      for (int i = 0; i < id.length && i < primaryKeys.length; i++)
        q.addCondition(primaryKeys[i] + " = " + id[i]);

    return q.toString();
  }

  /**
   *  Write an SQL query that will insert a single structure node into the
   *  database.  Each row must be added separately, alas.
   *  @param rootId the major indices identifying the targetted structure
   *  @param id the numerical value used to identify this particular node
   *  @param parent the numerical value identifying the parent of this node
   *  @param le the node, which must be a ListElement for a MappedTransducer
   */
  public String createInsert (
      String[] rootId, long id, long parent, ListElement le)
  {
    if (joinConditions != null)
      throw new Error("Can't insert into joined tables");

    int i = -1;
    SqlInsert ins = new SqlInsert(dbTable);
    ins.addNumber(idKey, id);
    ins.addNumber(parentKey, parent);
    if (rootId != null && primaryKeys != null)
      for (i = 0; i < rootId.length && i < primaryKeys.length; i++)
        ins.addString(primaryKeys[i], rootId[i]);

    Enumeration atts = attNames.elements();
    Enumeration cols = colNames.elements();
    for (i = 0; atts.hasMoreElements(); i++)
      ins.addString((String) cols.nextElement(),
        getNamedAttribute(le, (String) atts.nextElement()));

    return ins.toString();
  }

  private String getNamedAttribute (ListElement le, String name) {
    String valString = null;
    Enumeration e = le.getAttribute(name).getChildren();
    if (e.hasMoreElements()) {
      ValElement val = ((Element) e.nextElement()).getAsValue();
      if (val != null)
        valString = val.getValue();
    }
    return valString;
  }
}