/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

public class AggInfoEncoder {

  private int start_state = 0; 
  private int atom_state = 1;

  private int state = 0;

  private static final String start_xml_string = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
  private static final String start_begin_xml_string = "<xml_begin>";
  private static final String end_begin_xml_string = "</xml_begin>";
  private static final String data_set_xml_string = "set";
  private static final String begin_data_set_xml_string = "<set>";
  private static final String end_data_set_xml_string = "</set>";
  private static final String data_atom_xml_string = "atom";
  private static final String begin_data_atom_xml_string = "<atom>";
  private static final String end_data_atom_xml_string = "</atom>";
  private static final String org_xml_string = "org";
  private static final String start_org_xml_string = "<org>";
  private static final String end_org_xml_string = "</org>";
  private static final String metric_xml_string = "metric";
  private static final String start_metric_xml_string = "<metric>";
  private static final String end_metric_xml_string = "</metric>";

  public static String getStartXMLString () {
    return start_xml_string;
  }

  public static String getDataSetXMLString () {
    return data_set_xml_string;
  }

  public static String getStartDataSetXMLString () {
    return begin_data_set_xml_string;
  }

  public static String getEndDataSetXMLString () {
    return end_data_set_xml_string;
  }

  public static String getDataAtomXMLString () {
    return data_atom_xml_string;
  }

  public static String getStartDataAtomXMLString () {
    return begin_data_atom_xml_string;
  }

  public static String getEndDataAtomXMLString () {
    return end_data_atom_xml_string;
  }

  public static String getOrgXMLString () {
    return org_xml_string;
  }

  public static String getStartOrgXMLString () {
    return start_org_xml_string;
  }

  public static String getEndOrgXMLString () {
    return end_org_xml_string;
  }

  public static String getMetricXMLString () {
    return metric_xml_string;
  }

  public static String getStartMetricXMLString () {
    return start_metric_xml_string;
  }

  public static String getEndMetricXMLString () {
    return end_metric_xml_string;
  }

  public StringBuffer encodeStartOfXML (String org, String metric) {
    if (state != start_state) {
      System.out.println ("Must finish previous XML before starting another one");
      return null;
    }

    StringBuffer ret = new StringBuffer();

    ret.append (start_xml_string);

    ret.append (start_begin_xml_string);

    ret.append (start_org_xml_string);
    ret.append (org);
    ret.append (end_org_xml_string);

    ret.append (start_metric_xml_string);
    ret.append (metric);
    ret.append (end_metric_xml_string);

    ret.append (begin_data_set_xml_string);

    state = atom_state;

    return ret;
  }

  public void encodeDataAtom (StringBuffer ret, AggInfoStructure next_structure) {

     if (state != atom_state) {
       System.out.println ("Need to have written a start of XML message first");
     }

     ret.append (begin_data_atom_xml_string);

     ret.append (AggInfoStructure.getItemStartXMLString());
     ret.append (next_structure.getItem());
     ret.append (AggInfoStructure.getItemEndXMLString());

     if (next_structure.getTime() != null) {
       ret.append (AggInfoStructure.getTimeStartXMLString());
       ret.append (next_structure.getTime());
       ret.append (AggInfoStructure.getTimeEndXMLString());
     }
     else {
       ret.append (AggInfoStructure.getStartTimeStartXMLString());
       ret.append (next_structure.getStartTime());
       ret.append (AggInfoStructure.getStartTimeEndXMLString());
       ret.append (AggInfoStructure.getEndTimeStartXMLString());
       ret.append (next_structure.getEndTime());
       ret.append (AggInfoStructure.getEndTimeEndXMLString());
     }

     if (next_structure.getValue() != null) {
       ret.append (AggInfoStructure.getValueStartXMLString());
       ret.append (next_structure.getValue());
       ret.append (AggInfoStructure.getValueEndXMLString());
     }
     else {
       ret.append (AggInfoStructure.getRateStartXMLString());
       ret.append (next_structure.getRate());
       ret.append (AggInfoStructure.getRateEndXMLString());
     }

     ret.append (end_data_atom_xml_string);
  }

  public void encodeEndOfXML (StringBuffer ret) {

    if (state != atom_state) {
       System.out.println ("Need to have written a start of XML message first");
    }

    ret.append (end_data_set_xml_string);
    ret.append (end_begin_xml_string);

    state = start_state;
  }

  public static void main (String args[]) {
    AggInfoStructure myStruct = new AggInfoStructure ("PEOPLE", "1", "65");
    AggInfoStructure myStruct2 = new AggInfoStructure ("COMPUTERS", "1", "1.5");
    AggInfoStructure myStruct3 = new AggInfoStructure ("COMPUTERS", "1", "5", "5");
    AggInfoEncoder myEncoder = new AggInfoEncoder();

    StringBuffer buf = myEncoder.encodeStartOfXML("DEPT8H", "DEMAND");
    myEncoder.encodeDataAtom(buf, myStruct);
    myEncoder.encodeDataAtom(buf, myStruct2);
    myEncoder.encodeDataAtom(buf, myStruct3);
    myEncoder.encodeEndOfXML(buf);
    System.out.println(buf.toString());
  }

}
