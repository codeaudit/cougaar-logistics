/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.util;

import java.io.*;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class AggInfoDecoder {

  Document dom;
  NodeList element_list;
  int current_element = -1;
  String metric = null;
  String org = null;

  public void startXMLDecoding (String new_xml_string) {

//    System.out.println ("In startXMLDecoding");

    StringReader sr = new StringReader (new_xml_string);
    InputSource is = new InputSource (sr);
    DOMParser p = new DOMParser();

    try {
      p.parse (is);
      dom = p.getDocument();
    }
    catch (Exception e) {
      System.out.println ("Could not parse XML string");
    }

    NodeList org_list = dom.getElementsByTagName (AggInfoEncoder.getOrgXMLString());

    // Grab the first element
    Node current_node = org_list.item (0);

    if (current_node == null)
    {
      System.out.println ("It's null");
    }

    org = current_node.getFirstChild().getNodeValue();

    NodeList metric_list = dom.getElementsByTagName (AggInfoEncoder.getMetricXMLString());

    // Grab the first element
    current_node = metric_list.item (0);

    if (current_node == null)
    {
      System.out.println ("It's null");
    }

    metric = current_node.getFirstChild().getNodeValue();

    // Set up the list of <data-atom> nodes

    element_list = dom.getElementsByTagName (AggInfoEncoder.getDataAtomXMLString());
    current_element = 0;
  }

  public String getOrgFromXML () {
    return org;
  }

  public String getMetricFromXML () {
    return metric;
  }

  public boolean doneXMLDecoding () {
//    System.out.println ("In doneXMLDecoding");

    if (current_element < 0)
    {
      System.out.println ("Initialize Decoder with call to startXMLDecoding");
      return true;
    }

    return (current_element >= element_list.getLength());
  }

  public AggInfoStructure getNextDataAtom () {
//    System.out.println ("In getNextDataAtom");

    if (doneXMLDecoding())
      return null;

    Node current_node = element_list.item (current_element);

    if (current_node == null)
      return null;

    NodeList child_list = current_node.getChildNodes();
    String item = null;
    String time = null;
    String start_time = null;
    String end_time = null;
    String value = null;
    String rate = null;

    for (int index = 0; index < child_list.getLength(); index++)
    {
      Node this_node = child_list.item(index);
      if (this_node.getNodeName().compareTo(AggInfoStructure.getItemXMLString()) == 0) {
        item = this_node.getFirstChild().getNodeValue();
      }
      else if (this_node.getNodeName().compareTo(AggInfoStructure.getTimeXMLString()) == 0) {
        time = this_node.getFirstChild().getNodeValue();
      }
      else if (this_node.getNodeName().compareTo(AggInfoStructure.getStartTimeXMLString()) == 0) {
        start_time = this_node.getFirstChild().getNodeValue();
      }
      else if (this_node.getNodeName().compareTo(AggInfoStructure.getEndTimeXMLString()) == 0) {
        end_time = this_node.getFirstChild().getNodeValue();
      }
      else if (this_node.getNodeName().compareTo(AggInfoStructure.getValueXMLString()) == 0) {
        value = this_node.getFirstChild().getNodeValue();
      }
      else if (this_node.getNodeName().compareTo(AggInfoStructure.getRateXMLString()) == 0) {
        rate = this_node.getFirstChild().getNodeValue();
      }
    }

    AggInfoStructure ret;

    if (time != null)
      ret = new AggInfoStructure(item, time, value);
    else
      ret = new AggInfoStructure(item, start_time, end_time, rate);

    current_element++;

    return ret;
  }

  public static void main (String args[]) {
    AggInfoDecoder myDecoder = new AggInfoDecoder();
    AggInfoEncoder myEncoder = new AggInfoEncoder ();
    AggInfoStructure myStruct = new AggInfoStructure ("PEOPLE", "1", "65");
    AggInfoStructure myStruct2 = new AggInfoStructure ("COMPUTERS", "1", "1.5");
    AggInfoStructure myStruct3 = new AggInfoStructure ("LAMPS", "2", "5");
    AggInfoStructure myStruct4 = new AggInfoStructure ("LAMPS", "1", "5", "2");

    StringBuffer line = myEncoder.encodeStartOfXML("DEPT8H", "DEMAND");
    myEncoder.encodeDataAtom(line, myStruct);
    myEncoder.encodeDataAtom(line, myStruct2);
    myEncoder.encodeDataAtom(line, myStruct3);
    myEncoder.encodeDataAtom(line, myStruct4);
    myEncoder.encodeEndOfXML(line);

    System.out.print (line.toString());

    String metric = null;
    String org = null;

    myDecoder.startXMLDecoding (line.toString());

    metric = myDecoder.getMetricFromXML();
    org = myDecoder.getOrgFromXML();

//    myDecoder.startXMLDecoding ("<?XML bull?>\n<data-set>\n</data-set>");
//    myDecoder.startXMLDecoding ("/ata-set>");
//    String text = "<?xml version=1.0 encoding=UTF-8?><org>MyOrg</org><metric>DEMAND</metric><data-set><data-atom><item>MyItem </item><time>MyTime </time><value>MyValue </value></data-atom></data-set>";

//    myDecoder.startXMLDecoding (text);

    System.out.println ("");
    System.out.println ("Metric is " + metric);
    System.out.println ("Org is " + org);

    while (!myDecoder.doneXMLDecoding())
    {
      System.out.println ("Trying...");
      AggInfoStructure mystruct = myDecoder.getNextDataAtom();

      System.out.println ("Item is " + mystruct.getItem());

      if (mystruct.getTime() != null) {
        System.out.println ("Time is " + mystruct.getTime());
      }
      else {
        System.out.println ("Start Time is " + mystruct.getStartTime());
        System.out.println ("End Time is " + mystruct.getEndTime());
      }

      if (mystruct.getValue() != null) {
        System.out.println ("Value is " + mystruct.getValue());
      }
      else {
        System.out.println ("Rate is " + mystruct.getRate());
      }
    }
    System.out.println ("DONE!");
  }

}
