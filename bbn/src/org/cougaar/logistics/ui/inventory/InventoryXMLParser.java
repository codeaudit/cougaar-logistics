/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.q
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

public class InventoryXMLParser
{
    private int ctr;
    private String[] lines;
    private InventoryData inventory;
    private String currentString;
    private Stack tagStack;
    private Logger logger;

    public InventoryXMLParser() {
	tagStack = new Stack();
	logger = Logging.getLogger(this);
    }

    public InventoryData parseString(String xmlInput){
	ctr=0;
	inventory=null;
	lines = xmlInput.split("\\n");
	if(logger.isDebugEnabled()) {
	    logger.debug("Number of Lines=" + lines.length);
	}
	parse();
	return inventory;
    }

    private void parse() {
	while(ctr < lines.length) {
	    currentString = lines[ctr];
	    if(currentString.startsWith("</")) {
		popTag();
	    }
	    else if(currentString.startsWith("<")) {
		tagStack.push(currentString);
		String name = getTagName(currentString);
		if(name.equals(LogisticsInventoryFormatter.INVENTORY_DUMP_TAG)) {
		    parseHeader();
		}
		else {
		    parseSchedule();
		}
	    }
	    else {
		logger.warn("Unparseable line: " + currentString);
		ctr++;
	    }
	}	
    }

    private String stripTag(String tag) {
	int start=0;
	int end=tag.length();
	if(tag.startsWith("</")) {
	    start=2;
	}
	else if(tag.startsWith("<")) {
	    start=1;
	}
	if(tag.endsWith(">")) {
	    end--;
	}
	return tag.substring(start,end);
    }

    private String getTagName(String tag) {
	tag = stripTag(tag);
	String words[] = tag.split("\\s");
	return words[0];
    }

    private void parseHeader() {
      String header = stripTag(currentString);
      String[] words = header.split("\\s");
      String org=null;
      String asset=null;
      String unit=null;
      long cDay=0L;
      int i=1;

      if(words[i].startsWith("org=")){
	org=words[i].substring("org=".length());
      }
      i++;
      if(words[i].startsWith("item=")){
	asset=words[i].substring("item=".length());
      }
      i++;
      if(words[i].startsWith("unit=")){
	unit=words[i].substring("unit=".length());
      }
      i++;
      while(!(words[i].startsWith("cDay"))) {
	  unit=unit + " " + words[i];
	  i++;
      }
      if(words[i].startsWith("cDay=")){
	String cDayString=words[i].substring("cDay=".length());
	cDay = (new Long(cDayString)).longValue();
      }      

      inventory = new InventoryData(org,asset,unit,cDay);
      if(logger.isDebugEnabled()) {
	  logger.debug("Parsed header w/org=|" + org +
		       "| item=|" + asset + 
		       "| unit=|" + unit +
		       "| cDay=|" + cDay + "|");
      }
      ctr++;
    }

    private String getScheduleType(String tag) {
	tag = stripTag(tag);
	String words[] = tag.split("type=");
	return words[1].trim();
    }

    private void parseSchedule() {
        String name = getTagName(currentString);
	logger.debug("Parsing Schedule " + name);
	String typeStr = getScheduleType(currentString);
	logger.debug(" of type " + typeStr);
	int type = InventoryScheduleHeader.getTypeInt(typeStr);
	int ctrIn = ctr;
	currentString = lines[++ctr];
	ArrayList elements = new ArrayList();
	while(!(currentString.startsWith("<"))) {
	    elements.add(parseString(currentString,type));
	    currentString = lines[++ctr];
	}
	
	InventoryScheduleHeader header = new InventoryScheduleHeader(name,
								     type,
								     elements);
	inventory.addSchedule(header);
	
	if(logger.isDebugEnabled()) {
	    logger.debug("Parsed Schedule " + name 
			 + " there were " + (ctr - ctrIn) +
			 " lines of type " + typeStr);
	}

    }
	
    private InventoryScheduleElement parseString(String elementString,
						 int    type){
      switch(type) {
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

	    
    private void popTag() {
	String lastTag = (String) tagStack.peek();
	if(getTagName(lastTag).equals(getTagName(currentString))) {
	    tagStack.pop();
	    if(logger.isDebugEnabled()) {
		logger.debug("Popping " + getTagName(currentString));
	    }
	}
	else {
	    throw new RuntimeException("ERROR:Last pushed tag doesn't match this termination tag");
	}
	ctr++;
    }



}

