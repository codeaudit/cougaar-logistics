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

import org.cougaar.logistics.ui.stoplight.transducer.dbsupport.*;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

import java.util.*;
import java.sql.*;

import java.io.*;

/**
 *  This variety of SqlTransducer uses the "native" database schema which is
 *  designed to be as general as possible in terms of the Structure classes
 *  and associated XML DTD.
 */
public class NativeTransducer extends SqlTransducer {
  /**
   *  Construct a new instance of NativeTransducer.  The driver class for
   *  database connectivity must be specified (by name).
   *
   *  @param driver the name of the DB driver class
   */
  public NativeTransducer (String driver) {
    super(driver);
  }

  /**
   *  Create an Assembly geared toward the native DB schema and the location
   *  provided.
   *
   *  @param id the location of the Structure to be constructed
   */
  protected Assembly getAssembly (String[] id) {
    return new NativeAssembly(id);
  }

  /**
   *  Create a Disassembly geared toward the native DB schema and the location
   *  and Structure specified.
   *
   *  @param id the location where the structure will be stored in the table
   *  @param s the Structure to be stored in the DB
   */
  protected Disassembly getDisassembly (String[] id, Structure s) {
    return new NativeDisassembly(id, s);
  }

  /**
   *  Clear the entries in the database corresponding to the provided id keys.
   *  In effect, this deletes the structure stored there, if any.
   *
   *  @param id the location of the Structure to be removed
   */
  protected void clearSpaceFor (String[] id) {
    Statement st = null;
    try {
      st = conn.createStatement();
      st.executeUpdate(createDelete(id, "text_element"));
      st.executeUpdate(createDelete(id, "list_element"));
      conn.commit();
    }
    catch (Exception bugger) {
      bugger.printStackTrace();
    }
    finally {
      closeStatement(st);
    }
  }

  //
  // Concrete implementation of the Assembly interface.  The "native" format
  // is used presumed for reading info from the database.
  //
  private class NativeAssembly implements Assembly {
    private Structure root = null;
    private String[] rootId = null;

    private Hashtable lists = new Hashtable();
    private Vector orphans = new Vector();
    private Hashtable values = new Hashtable();

    public NativeAssembly (String[] id) {
      rootId = id;
    }

    public Structure readFromDb () {
      try {
        // first get the nodes in the tree structure
        assembleStructure();

        if (orphans.size() > 0)
          System.out.println("SqlTransducer::Assembly::readFromDb:  found " +
            orphans.size() + " orphans");
        orphans.clear();

        // next get the text contents
        addTextValues();

        if (orphans.size() > 0)
          System.out.println("SqlTransducer::Assembly::readFromDb:  " +
            orphans.size() + " content strings orphaned");

        // set the database ID number before returning
        if (rootId != null && rootId.length > 0)
          root.setDatabaseId(Long.parseLong(rootId[0]));

        return root;
      }
      catch (Exception bugger) {
        bugger.printStackTrace();
      }
      return null;
    }

    private Element createElement (String type, String name) {
      if (type != null || type.length() > 0) {
        char c = type.charAt(0);
        if (c == 'S')
          return new Structure();
        if (c == 'L')
          return new ListElement();
        if (c == 'A')
          return new Attribute(name);
        if (c == 'V')
          return new ValElement();
        if (c == 'N')
          return new NullElement();
      }

      throw new IllegalArgumentException(
        "Unable to create element of type '" + type + "'");
    }

    private void assembleStructure () throws SQLException {
      Statement st = null;
      try {
        st = NativeTransducer.this.conn.createStatement();
        SqlQuery q = new SqlQuery();
        q.addTable("list_element");
        q.addSelection("id, type, parent, place, name");
        q.addOrdering("id");
        if (rootId != null && rootId.length > 0)
          q.addCondition("root_id = " + rootId[0]);
        ResultSet rs = st.executeQuery(q.toString());
        while (rs.next()) {
          long id = rs.getLong(1);
          String type = rs.getString(2);
          long parent = rs.getLong(3);
          int place = rs.getInt(4);
          String name = rs.getString(5);

          Element elt = createElement(type, name);

          // if this is the root node, keep a reference handy.  Otherwise,
          // check for an existing parent node (there should be one) and bond
          // to it.  If there isn't one, join the orphans.
          if (parent == -1) {
            root = elt.getAsStructure();
          }
          else {
            ListElement parentNode = (ListElement) lists.get(new Long(parent));
            Attribute asAtt = elt.getAsAttribute();
            if (parentNode == null)
              orphans.add(elt);
            else if (asAtt == null)
              parentNode.setChildAt(place, elt);
            else
              parentNode.addAttribute(asAtt);
          }

          // if this element is capable of containing information, keep a
          // tabulated reference so that text or subtags can be attatched later
          if (elt.getAsValue() != null)
            values.put(new Long(id), elt);
          else if (elt.getAsList() != null)
            lists.put(new Long(id), elt);
        }
      }
      finally {
        NativeTransducer.this.closeStatement(st);
      }
    }

    private void addTextValues () throws SQLException {
      Statement st = null;
      try {
        st = NativeTransducer.this.conn.createStatement();
        SqlQuery q = new SqlQuery();
        q.addTable("text_element");
        q.addSelection("parent, value");
        if (rootId != null && rootId.length > 0)
          q.addCondition("root_id = " + rootId[0]);
        ResultSet rs = st.executeQuery(q.toString());
        while (rs.next()) {
          long parent = rs.getLong(1);
          String val = rs.getString(2);

          // attatch this text String to its parent, if found; otherwise join the
          // orphans' roster
          ValElement parentNode = (ValElement) values.get(new Long(parent));
          if (parentNode != null)
            parentNode.setValue(val);
          else
            orphans.add(val);
        }
      }
      finally {
        NativeTransducer.this.closeStatement(st);
      }
    }
  }

  //
  // Concrete Disassembly implementation for the "native" format.
  //
  private class NativeDisassembly implements Disassembly {
    private Structure root = null;
    private String[] rootId = null;
    private Statement st = null;
    private long elementIdCounter = 0;

    public NativeDisassembly (String[] id, Structure s) {
      rootId = id;
      root = s;
    }

    public void writeToDb () {
      try {
        st = NativeTransducer.this.conn.createStatement();
        writeNode(root, -1, -1);
      }
      catch (Exception bugger) {
        bugger.printStackTrace();
      }
      finally {
        NativeTransducer.this.closeStatement(st);
      }
    }

    private long getNewElementId () throws SQLException {
      return elementIdCounter++;
    }

    private void writeNode (Element elt, long parent, int place)
        throws SQLException
    {
      long id = getNewElementId();
      SqlInsert ins = new SqlInsert("list_element");
      if (rootId != null && rootId.length > 0)
        ins.addNumber("root_id", rootId[0]);
      ins.addNumber("id", id);
      ins.addString("type", elt.getSymbol());
      ins.addNumber("parent", parent);
      ins.addNumber("place", place);
      st.executeUpdate(ins.toString());

      writeSubTags(elt.getAsList(), id);
      writeTextContent(elt.getAsValue(), id);
    }

    private void writeSubTags (ListElement le, long id) throws SQLException {
      if (le == null)
        return;
      for (Enumeration atts = le.getAttributes(); atts.hasMoreElements(); ) {
        Attribute a = (Attribute) atts.nextElement();
        writeAttribute(a, id);
      }
      int i = 0;
      for (Enumeration ch = le.getChildren(); ch.hasMoreElements(); i++) {
        Element child = (Element) ch.nextElement();
        writeNode(child, id, i);
      }
    }

    private void writeTextContent (ValElement v, long id) throws SQLException {
      if (v == null)
        return;
      SqlInsert ins = new SqlInsert("text_element");
      if (rootId != null && rootId.length > 0)
        ins.addNumber("root_id", rootId[0]);
      ins.addNumber("parent", id);
      ins.addString("value", v.getValue());

      st.executeUpdate(ins.toString());
    }

    private void writeAttribute (Attribute a, long parent) throws SQLException {
      long id = getNewElementId();
      SqlInsert ins = new SqlInsert("list_element");
      if (rootId != null && rootId.length > 0)
        ins.addNumber("root_id", rootId[0]);
      ins.addNumber("id", id);
      ins.addString("type", a.getSymbol());
      ins.addNumber("parent", parent);
      ins.addNumber("place", -1);
      ins.addString("name", a.getName());
      st.executeUpdate(ins.toString());

      writeSubTags(a, id);
    }
  }

  private String createDelete (String[] id, String table) {
    StringBuffer buf = new StringBuffer("delete from ");
    buf.append(table);
    if (id != null && id.length > 0) {
      buf.append(" where root_id = ");
      buf.append(id[0]);
    }
    return buf.toString();
  }

  // - - - - - - - Testing Harness - - - - - - - - - - - - - - - - - - - - - - -

  public static void main (String[] argv) {
    if (argv.length < 2) {
      System.out.println(
        "Please specify an XML file for input and another for output");
      return;
    }
    try {
      NativeTransducer sqlt = new NativeTransducer("oracle.jdbc.driver.OracleDriver");
      sqlt.setDbParams("jdbc:oracle:thin:@delta.alpine.bbn.com:1521:fgi",
        "gdonovan", "gdonovan");
      sqlt.openConnection();

      XmlInterpreter xint = new XmlInterpreter();
      FileInputStream fin = new FileInputStream(argv[0]);
      Structure s = xint.readXml(fin);
      fin.close();

      s.setDatabaseId(1);
      String[] index = new String[] {"1"};
      sqlt.writeToDb(index, s);

      Structure s2 = sqlt.readFromDb(index);

      FileOutputStream fout = new FileOutputStream(argv[1]);
      xint.writeXml(s2, fout);
      fout.close();

      sqlt.closeConnection();
    }
    catch (Exception problem) {
      problem.printStackTrace();
    }
  }
}