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

package org.cougaar.logistics.ui.inventory;

import java.util.Stack;
import java.util.ArrayList;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;
import org.cougaar.logistics.ui.inventory.data.*;

/**
 * <pre>
 *
 * The InventoryXMLParser is the class that parses the input xml
 * string into data objects.
 *
 *
 *
 **/

public class InventoryXMLParser {
  protected int ctr;
  protected String[] lines;
  protected InventoryData inventory;
  protected String currentString;
  protected Stack tagStack;
  protected Logger logger;

  public InventoryXMLParser() {
    tagStack = new Stack();
    logger = Logging.getLogger(this);
  }

  public InventoryData parseString(String xmlInput) {
    return parseString(xmlInput, false);
  }

  public InventoryData parseHeader(String xmlInput) {
    return parseString(xmlInput, true);
  }

  protected InventoryData parseString(String xmlInput, boolean justHeader) {
    ctr = 0;
    inventory = null;
    if((xmlInput != null) && !(xmlInput.equals("null\n"))) {
	lines = xmlInput.split("\\n");
	if (logger.isDebugEnabled()) {
	    logger.debug("Number of Lines=" + lines.length);
	}
	parse(justHeader);
    }
    return inventory;
  }

  protected void parse(boolean justHeader) {
    while (ctr < lines.length) {
      currentString = lines[ctr];
      if (currentString.startsWith("</")) {
        popTag();
      } else if (currentString.startsWith("<")) {
        tagStack.push(currentString);
        String name = getTagName(currentString);
        if (name.equals(LogisticsInventoryFormatter.INVENTORY_DUMP_TAG)) {
          ctr++;
        } else if (name.equals(LogisticsInventoryFormatter.INVENTORY_HEADER_PERSON_READABLE_TAG)) {
          parseHumanReadable();
        } else if (name.equals(LogisticsInventoryFormatter.INVENTORY_HEADER_GUI_TAG)) {
          parseHeader();
          if (justHeader) {
            return;
          }
        } else {
          if (!justHeader) {
            parseSchedule();
          } else {
            ctr++;
          }
        }
      } else {
        logger.warn("Unparseable line: " + currentString);
        ctr++;
      }
    }
  }

  protected String stripTag(String tag) {
    int start = 0;
    int end = tag.length();
    if (tag.startsWith("</")) {
      start = 2;
    } else if (tag.startsWith("<")) {
      start = 1;
    }
    if (tag.endsWith(">")) {
      end--;
    }
    return tag.substring(start, end);
  }

  protected String getTagName(String tag) {
    tag = stripTag(tag);
    String words[] = tag.split("\\s");
    return words[0];
  }

  protected void parseHeader() {
    String header = stripTag(currentString);
    String[] words = header.split("\\s");
    String org = null;
    String asset = null;
    String unit = null;
    String nomenclature = null;
    String supplyType = null;
    long cDay = 0L;
    int i = 1;

    if (words[i].startsWith("org=")) {
      org = words[i].substring("org=".length());
    }
    i++;
    if (words[i].startsWith("item=")) {
      asset = words[i].substring("item=".length());
    }
    i++;
    if (words[i].startsWith("unit=")) {
      unit = words[i].substring("unit=".length());
    }
    i++;
    while (!(words[i].startsWith("nomenclature="))) {
      unit = unit + " " + words[i];
      i++;
    }
    if (words[i].startsWith("nomenclature=")) {
      nomenclature = words[i].substring("nomenclature=".length());
    }
    i++;
    while (!(words[i].startsWith("supplyType="))) {
      nomenclature = nomenclature + " " + words[i];
      i++;
    }
    if (words[i].startsWith("supplyType=")) {
      supplyType= words[i].substring("supplyType=".length());
    }
    i++;
    while (!(words[i].startsWith("cDay="))) {
      supplyType = supplyType + " " + words[i];
      i++;
    }
    if (words[i].startsWith("cDay=")) {
      String cDayString = words[i].substring("cDay=".length());
      cDay = (new Long(cDayString)).longValue();
    }

    inventory = new InventoryData(asset, org, unit, nomenclature, supplyType, cDay);
    if (logger.isDebugEnabled()) {
      logger.debug("Parsed header w/org=|" + org +
                   "| item=|" + asset +
                   "| unit=|" + unit +
                   "| nomenclature=|" + nomenclature +
                   "| supplyType=|" + supplyType +
                   "| cDay=|" + cDay + "|");
    }
    ctr++;
  }

  protected String getScheduleType(String tag) {
    tag = stripTag(tag);
    String words[] = tag.split("type=");
    return words[1].trim();
  }

  protected void parseSchedule() {
    String name = getTagName(currentString);
    logger.debug("Parsing Schedule " + name);
    String typeStr = getScheduleType(currentString);
    logger.debug(" of type " + typeStr);
    int type = InventoryScheduleHeader.getTypeInt(typeStr);
    int ctrIn = ctr;
    currentString = lines[++ctr];
    ArrayList elements = new ArrayList();
    while (!(currentString.startsWith("<"))) {
      elements.add(parseString(currentString, type));
      currentString = lines[++ctr];
    }

    InventoryScheduleHeader header = new InventoryScheduleHeader(name,
                                                                 type,
                                                                 elements);
    inventory.addSchedule(header);

    if (logger.isDebugEnabled()) {
      logger.debug("Parsed Schedule " + name
                   + " there were " + (ctr - ctrIn) +
                   " lines of type " + typeStr);
    }

  }

  protected void parseHumanReadable() {
//toss out this stuff
    String name = getTagName(currentString);
    logger.debug("Parsing: " + name);
    String humanReadableEndTag = "</" + LogisticsInventoryFormatter.INVENTORY_HEADER_PERSON_READABLE_TAG + ">";
    while (!(currentString.equals(humanReadableEndTag))) {
      currentString = lines[++ctr];
    }
  }


  protected InventoryScheduleElement parseString(String elementString,
                                               int type) {
    switch (type) {
      case InventoryScheduleHeader.TASKS_TYPE:
        return InventoryTask.createFromCSV(elementString);
      case InventoryScheduleHeader.PROJ_TASKS_TYPE:
        return InventoryProjTask.createFromCSV(elementString);
      case InventoryScheduleHeader.ARS_TYPE:
        return InventoryAR.createFromCSV(elementString);
      case InventoryScheduleHeader.PROJ_ARS_TYPE:
        return InventoryProjAR.createProjFromCSV(elementString);
      case InventoryScheduleHeader.LEVELS_TYPE:
        return InventoryLevel.createFromCSV(elementString);
      default:
        throw new RuntimeException("Unparseable CSV " + elementString +
                                   " Element Type " + type);
    }
  }


  protected void popTag() {
    String lastTag = (String) tagStack.peek();
    if (getTagName(lastTag).equals(getTagName(currentString))) {
      tagStack.pop();
      if (logger.isDebugEnabled()) {
        logger.debug("Popping " + getTagName(currentString));
      }
    } else {
      throw new RuntimeException("ERROR:Last pushed tag doesn't match this termination tag");
    }
    ctr++;
  }


}

