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

package org.cougaar.logistics.ui.stoplight.transducer;

import org.cougaar.logistics.ui.stoplight.transducer.configs.*;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

import java.util.*;
import java.sql.*;

import java.io.*;

/**
 *  <p>
 *  A variety of transducer that uses in-line fields to populate nodes in a
 *  hierarchical structure.  There are certain requirements on the DB schema
 *  in order for this type of transducer to be applicable.  In particular,
 *  one column must be devoted to a type of unique identifier, which is assumed
 *  to be numerical.  Another column must be a reference to the first, where
 *  the entry corresponds to the id of the parent node.  Various other columns
 *  may be mapped to attributes of the node to which any given row corresponds.
 *  </p><p>
 *  The configuration info (along with some helpful methods) used to map tables
 *  and columns into the parts of a structure is stored in an instance of class
 *  SqlTableMap (q.v.).  Also stored there is the indexing scheme applied to
 *  whole Structures, which may reside in the same DB table.
 *  </p>
 */
public class MappedTransducer extends SqlTransducer {
  /**
   *  This is where the configuration of the MappedTransducer is stored.
   *  All operations of this transducer are governed by it, and it is presumed
   *  to be unchanging during the lifetime of the transducer.
   */
  protected SqlTableMap config = null;

  /**
   *  Construct a new instance of MappedTransducer.  The driver class for
   *  database connectivity must be specified (by name), as well as a
   *  configuration map for the DB schema.
   *
   *  @param driver the name of the DB driver class
   *  @param m the configuration mapping for the DB schema to be used
   */
  public MappedTransducer (String driver, SqlTableMap m) {
    super(driver);
    config = m;
  }

  /**
   *  Create an Assembly capable of constructing Structure trees from the DB
   *  as described in an SqlTableMap.  Each Assembly instance is created
   *  for a single lookup in the database, after which it is discarded.
   *  @param id the sequence of keys that identify the Structure to be retrieved
   *  @return an Assembly configured to retrieve the indicated Structure
   */
  protected Assembly getAssembly (String[] id) {
    return new MappedAssembly(id);
  }

  /**
   *  Create a Disassembly capable of walking the Structure's tree and saving
   *  its information in the database.  Each Dissassembly instance is created
   *  to store a single Structure, after which it is discarded.
   *  @param id the sequence of keys identifying where the Structure is stored
   *  @param s the Structure to be stored.
   */
  protected Disassembly getDisassembly (String[] id, Structure s) {
    return new MappedDisassembly(id, s);
  }

  /**
   *  Remove an existing entry in the database so that a new one may be
   *  inserted in its place.
   *  @param id the sequence of keys identifying the Structure to be deleted.
   */
  protected void clearSpaceFor (String[] id) {
    Statement st = null;
    try {
      st = conn.createStatement();
      st.executeUpdate(config.createDelete(id));
    }
    catch (Exception bugger) {
      bugger.printStackTrace();
    }
    finally {
      closeStatement(st);
    }
  }

  //
  // the concrete Assembly implementation used by this kind of transducer
  //
  private class MappedAssembly implements Assembly {
    private String[] rootId = null;
    private ListElement root = null;
    private Statement st = null;

    private Hashtable lists = new Hashtable();
    private Hashtable stand_ins = new Hashtable();
    private Hashtable values = new Hashtable();

    public MappedAssembly (String[] id) {
      rootId = id;
    }

    public Structure readFromDb () {
      try {
        st = MappedTransducer.this.conn.createStatement();

        // first get the nodes in the tree structure
        queryForStructure(config.createQuery(rootId));

        if (stand_ins.size() > 0)
          System.out.println("MappedTransducer::Assembly::readFromDb:  found " +
            stand_ins.size() + " stand-ins");
        stand_ins.clear();

        // Create the containing Structure for this tree and install its id
        // keys in its "dbId" attribute
        Structure s = new Structure();
        if (rootId != null && rootId.length > 0) {
          Attribute att = new Attribute("dbId");
          s.addAttribute(att);
          for (int i = 0; i < rootId.length; i++)
            att.addChild(new ValElement(rootId[i]));
        }

        if (root != null)
          s.addChild(root);

        return s;
      }
      catch (Exception bugger) {
        bugger.printStackTrace();
      }
      finally {
        MappedTransducer.this.closeStatement(st);
      }
      return null;
    }

    // Construct the tree.  The contents are presumed to be in the same table
    // as the child-parent links, so those will be extracted here as well.  All
    // nodes are presumed to be lists.  Attributes are constructed assuming
    // that they contain a simple String value.
    private void queryForStructure (String query) throws SQLException {
      ResultSet rs = st.executeQuery(query);
      String[] attributes = config.getAttributeNames();
      String[] fieldVals = new String[attributes.length];
      while (rs.next()) {
        int i = -1;
        Long id = new Long(rs.getLong(1));
        Long parent = new Long(rs.getLong(2));
        for (i = 0; i < fieldVals.length; i++)
          fieldVals[i] = rs.getString(i + 3);

        // check for an existing stand-in; if none exists, create a new node
        ListElement elt = (ListElement) stand_ins.remove(id);
        if (elt == null) {
          elt = new ListElement();
          // this element may have children; keep a tabulated reference so
          // that they can be attatched later
          lists.put(id, elt);
        }

        // Install the attributes into the possibly nascent ListElement.
        // By design, they must be in the canonical order
        for (i = 0; i < attributes.length; i++)
          elt.addAttribute(new Attribute(attributes[i], fieldVals[i]));

        // if this is the root node, keep a reference handy.  Otherwise,
        // check for an existing parent node (or create a stand-in) and bond
        // to it.
        if (parent.longValue() == -1) {
          root = elt;
        }
        else {
          ListElement parentNode = (ListElement) lists.get(parent);
          if (parentNode == null) {
            parentNode = new ListElement();
            stand_ins.put(parent, parentNode);
            lists.put(parent, parentNode);
          }
          parentNode.addChild(elt);
        }
      }
    }
  }

  //
  // The concrete Disassembly impelemntation used by this kind of transducer
  //
  private class MappedDisassembly implements Disassembly {
    private Structure root = null;
    private String[] rootId = null;
    private Statement st = null;

    private long nodeIdCounter = 0;

    public MappedDisassembly (String[] id, Structure s) {
      rootId = id;
      root = s;
    }

    public void writeToDb () {
      try {
        st = MappedTransducer.this.conn.createStatement();
        writeNode(root.getContentList(), -1);
      }
      catch (Exception bugger) {
        bugger.printStackTrace();
      }
      finally {
        MappedTransducer.this.closeStatement(st);
      }
    }

    private long getNewElementId () {
      return nodeIdCounter++;
    }

    private void writeNode (ListElement le, long parent) throws SQLException {
      long id = getNewElementId();
      String q = config.createInsert(rootId, id, parent, le);
      st.executeUpdate(q);

      for (Enumeration ch = le.getChildren(); ch.hasMoreElements(); ) {
        ListElement child = (ListElement) ch.nextElement();
        writeNode(child, id);
      }
    }
  }

  // - - - - - - - Test harness - - - - - - - - - - - - - - - - - - - - - - - -
  public static void main (String[] argv) {
    try {
      if (argv.length == 0) {
        System.out.println("You dumb ass.");
      }
      else if (argv[0].equalsIgnoreCase("to")) {
        if (argv.length == 2)
          toFile(argv[1]);
        else if (argv.length > 2)
          indexToFile(argv[2], argv[1], false);
      }
      else if (argv[0].equalsIgnoreCase("from") && argv.length > 0) {
        if (argv.length == 2)
          fromFile(argv[1]);
        else if (argv.length > 2)
          indexFromFile(argv[2], argv[1]);
      }
      else if (argv[0].equalsIgnoreCase("kill")) {
        if (argv.length == 1)
          killStruct();
        else
          killIndex(argv[1]);
      }
      else if (argv[0].equalsIgnoreCase("extra")) {
        if (argv.length == 3)
          indexToFile(argv[2], argv[1], true);
        else
          System.out.println("You dumb ass.");
      }
    }
    catch (Exception problem) {
      problem.printStackTrace();
    }
  }

  private static void indexToFile (String id, String fileName, boolean extra)
      throws Exception
  {
    writeToFile(restoreFromDb(new String[] {id}, extra), fileName);
  }

  private static void indexFromFile (String id, String fileName)
      throws Exception
  {
    saveInDb(readFromFile(fileName), new String[] {id});
  }

  private static void killIndex (String id) throws Exception {
    MappedTransducer mt = makeTransducer(false);
    mt.openConnection();

    String[] keys = null;
    if (id != null)
      keys = new String[] {id};

    mt.removeFromDb(keys);
    mt.closeConnection();
  }

  private static void toFile (String fileName) throws Exception {
    writeToFile(restoreFromDb(null, false), fileName);
  }

  private static void fromFile (String fileName) throws Exception {
    saveInDb(readFromFile(fileName), null);
  }

  private static void killStruct () throws Exception {
    killIndex(null);
  }

  // - - - - - - - Utility functions for test harness - - - - - - - - - - - - -

  private static Structure readFromFile (String fileName) throws Exception {
    XmlInterpreter xint = new XmlInterpreter();
    FileInputStream fin = new FileInputStream(fileName);
    Structure s = xint.readXml(fin);
    fin.close();
    return s;
  }

  private static void writeToFile (Structure s, String fileName)
      throws Exception
  {
    XmlInterpreter xint = new XmlInterpreter();
    FileOutputStream fout = new FileOutputStream(fileName);
    xint.writeXml(s, fout);
    fout.close();
  }

  private static Structure restoreFromDb (String[] keys, boolean extra) {
    MappedTransducer mt = makeTransducer(extra);
    mt.openConnection();
    Structure s = mt.readFromDb(keys);
    mt.closeConnection();
    return s;
  }

  private static void saveInDb (Structure s, String[] keys) {
    MappedTransducer mt = makeTransducer(false);
    mt.openConnection();
    mt.writeToDb(keys, s);
    mt.closeConnection();
  }

  private static MappedTransducer makeTransducer (boolean extra) {
    MappedTransducer mt = new MappedTransducer(
      "oracle.jdbc.driver.OracleDriver", makeConfig(extra));
    mt.setDbParams("jdbc:oracle:thin:@delta.alpine.bbn.com:1521:fgi",
      "gdonovan", "gdonovan");
    return mt;
  }

  private static SqlTableMap makeConfig (boolean extra) {
    SqlTableMap ret = new SqlTableMap();
    StringBuffer table = new StringBuffer("stuff_n_note");

    if (extra) {
      table.append(", extra");
    }

    ret.setDbTable(table.toString());
    ret.setIdKey("id");
    ret.setParentKey("parent");
    ret.addContentKey("content", "stuff");
    ret.addContentKey("annotation", "note");
    if (extra) {
      ret.addContentKey("extra_field", "extra");
      ret.setPrimaryTableName("stuff_n_note");
      ret.setJoinConditions("stuff_n_note.id = extra.id");
    }
    ret.setPrimaryKeys(new String[] {"keynum"});
    return ret;
  }
}