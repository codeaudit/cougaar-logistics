/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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

import java.io.FileWriter;
import java.io.PrintWriter;


import org.cougaar.logistics.ui.stoplight.client.DBInterface;
import org.cougaar.logistics.ui.stoplight.transducer.*;
import org.cougaar.logistics.ui.stoplight.transducer.configs.*;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

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