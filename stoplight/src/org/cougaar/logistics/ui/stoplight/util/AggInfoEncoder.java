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
