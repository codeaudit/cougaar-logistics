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