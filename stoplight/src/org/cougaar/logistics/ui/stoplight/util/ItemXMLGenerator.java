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

import java.io.FileWriter;
import java.io.PrintWriter;

import org.cougaar.lib.uiframework.transducer.*;
import org.cougaar.lib.uiframework.transducer.configs.*;
import org.cougaar.lib.uiframework.transducer.elements.*;

import org.cougaar.logistics.ui.stoplight.client.DBInterface;

public class ItemXMLGenerator
{
  public static void main(String[] args)
  {
    String saveTo = "itemTree.xml";

    if (args.length > 0)
    {
      saveTo = args[0];
    }

    try {
      Structure itemTreeStructure = DBInterface.createItemTree();
      saveToFile(itemTreeStructure, saveTo);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void saveToFile(Structure tree, String filename)
    throws Exception
  {
      // create tree xml string
      XmlInterpreter xint = new XmlInterpreter();
      FileWriter xmlWriter = new FileWriter(filename);
      xint.writeXml(tree, new PrintWriter(xmlWriter));
  }
}