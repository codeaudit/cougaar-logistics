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

import java.io.*;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

/**
 *  This class provides the ability to convert an XML stream into a Structure.
 *  There are also methods for reversing the process, though most of the
 *  implementation resides in package transducer.elements (q.v.).
 */
public class XmlInterpreter {
  private String dtdPath = ".";

  /**
   *  Read an XML document from the provided InputStream and create a
   *  Structure which reflects its contents.
   *  @param in the InputStream from which to read the XML
   *  @return the Structure as defined in the XML stream
   */
  public Structure readXml (InputStream in) {
    return readXml(new BufferedReader(new InputStreamReader(in)));
  }

  /**
   *  Read an XML document from the provided BufferedReader and create a
   *  Structure which reflects its contents.
   *  @param bufr the BufferedReader from which to read the XML
   *  @return the Structure as defined in the XML stream
   */
  public Structure readXml (BufferedReader bufr) {
    try {
      DOMParser p = new DOMParser();
      p.parse(new InputSource(bufr));
      Document txt = p.getDocument();

      Node t = txt.getDocumentElement();
      if (t.getNodeName().equals("structure"))
        return visitStructure(t);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   *  Generate an XML representation of an existing Structure and channel it
   *  to a specified OutputStream.  A PrettyPrinter is used to format the
   *  resulting XML text.
   *  @param s the Structure to be written as XML output
   *  @param o the OutputStream to which the XML is sent
   */
  public void writeXml (Structure s, OutputStream o) {
    writeXml(s, new PrintWriter(o));
  }

  /**
   *  Generate an XML representation of an existing Structure and channel it
   *  to a specified PrintWriter.  A PrettyPrinter is used to format the
   *  resulting XML text.
   *  @param s the Structure to be written as XML output
   *  @param o the PrintWriter to which the XML is sent
   */
  public void writeXml (Structure s, PrintWriter o) {
    PrettyPrinter pp = new PrettyPrinter(o);
    pp.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    // pp.print("<!DOCTYPE structure SYSTEM \"structure.dtd\">");
    s.generateXml(pp);
    pp.flush();
  }

  private Structure visitStructure (Node n) {
    Structure ret = new Structure();

    ChildEnumerator ce = new ChildEnumerator(n);
    installAttributes(ce, ret);
    installListMembers(ce, ret);

    return ret;
  }

  private Attribute visitAttribute (Node n) {
    Attribute ret = new Attribute();

    // get the attribute's name
    NamedNodeMap nnm = n.getAttributes();
    ret.setName(getDomAttribute(n, "name"));

    // get the contents
    ChildEnumerator ce = new ChildEnumerator(n);
    installAttributes(ce, ret);
    installListMembers(ce, ret);

    return ret;
  }

  private ListElement visitList (Node n) {
    ListElement ret = new ListElement();

    ChildEnumerator ce = new ChildEnumerator(n);
    installAttributes(ce, ret);
    installListMembers(ce, ret);

    return ret;
  }

  private ValElement visitVal (Node n) {
    ValElement ret = new ValElement();

    ret.setValue(getTextContent(n));

    return ret;
  }

  private NullElement visitNull (Node n) {
    return new NullElement();
  }

  private static String getTextContent (Node n) {
    StringBuffer buf = new StringBuffer();
    NodeList nl = n.getChildNodes();
    int length = nl.getLength();
    for (int i = 0; i < length; i++) {
      Node t = nl.item(i);
      if (t instanceof CharacterData)
        buf.append(((CharacterData) t).getData());
    }
    return buf.toString();
  }

  private static String getDomAttribute (Node n, String attrib) {
    NamedNodeMap nnm = n.getAttributes();
    Node t = null;
    if (nnm != null && (t = nnm.getNamedItem(attrib)) != null)
      return t.getNodeValue();
    return null;
  }

  private void installAttributes (ChildEnumerator ce, ListElement le) {
    for (Node t = ce.current();
        t != null && t.getNodeName().equals("a"); t = ce.next())
    {
      le.addAttribute(visitAttribute(t));
    }
  }

  private void installListMembers (ChildEnumerator ce, ListElement le) {
    for (Node t = ce.current(); t != null; t = ce.next()) {
      String childName = t.getNodeName();
      if (childName.equals("structure"))
        le.addChild(visitStructure(t));
      else if (childName.equals("list"))
        le.addChild(visitList(t));
      else if (childName.equals("val"))
        le.addChild(visitVal(t));
      else if (childName.equals("null"))
        le.addChild(visitNull(t));
    }
  }

  public static void main (String[] argv) {
    if (argv.length < 1)
      System.out.println("Please specify an XML file");
    else {
      try {
        XmlInterpreter xint = new XmlInterpreter();

        FileInputStream fin = new FileInputStream(argv[0]);
        Structure s = xint.readXml(fin);
        fin.close();

        FileOutputStream fout = new FileOutputStream(argv[1]);
        xint.writeXml(s, fout);
        fout.close();
      }
      catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }
}
