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
 
package org.cougaar.logistics.ui.servicediscovery;

import java.util.Stack;
import java.util.ArrayList;

import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;


/** 
 * <pre>
 * 
 * The RelationshipXMLParser is the class that parses the input xml
 * string into data objects.
 * 
 * 
 *
 **/

public class RelationshipXMLParser
{
    private int ctr;
    private String[] lines;
    private RelationshipScheduleData relSchedule;
    private String currentString;
    private Stack tagStack;
    private Logger logger;

    public RelationshipXMLParser() {
	tagStack = new Stack();
	logger = Logging.getLogger(this);
    }

    public RelationshipScheduleData parseString(String xmlInput){
	return parseString(xmlInput,false);
    }
    public RelationshipScheduleData parseHeader(String xmlInput){
	return parseString(xmlInput,true);
    }

    private RelationshipScheduleData parseString(String xmlInput,boolean justHeader){
	ctr=0;
	relSchedule=null;
	lines = xmlInput.split("\\n");
	if(logger.isDebugEnabled()) {
	    logger.debug("Number of Lines=" + lines.length);
	}
	parse(justHeader);
	return relSchedule;
    }

    private void parse(boolean justHeader) {
	while(ctr < lines.length) {
	    currentString = lines[ctr];
	    if(currentString.startsWith("</")) {
		popTag();
	    }
	    else if(currentString.startsWith("<")) {
		tagStack.push(currentString);
		String name = getTagName(currentString);
		if(name.equals(RelationshipScheduleData.RELATIONSHIP_SCHEDULE_TAG)) {
		    ctr++;
		}
		else if(name.equals(RelationshipScheduleData.RELATIONSHIP_SCHEDULE_HEADER_TAG)) {
		    parseHeader();
		    if(justHeader) {
			return;
		    }
		}
		else if(name.equals(RelationshipScheduleData.RELATIONSHIPS_TAG)) {
		    if(!justHeader) {
			parseRelationshipSchedule();
		    }
		    else {
			ctr++;
		    }
		}
		else {
		    parseInstanceSchedule();
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
      String sourceAgent=null;
      long cDay=0L;
      int i=1;

      if(words[i].startsWith("sourceAgent=")){
	sourceAgent=words[i].substring("sourceAgent=".length());
      }
      i++;
      if(words[i].startsWith("startDate=")){
	String startCDayStr=words[i].substring("startDate=".length());
	cDay = Long.parseLong(startCDayStr);
      } 
      i++;
      relSchedule = new RelationshipScheduleData(cDay, sourceAgent);
      if(logger.isDebugEnabled()) {
	  logger.debug("Parsed header w/sourceAgent=|" + sourceAgent +
		       "| cDay=|" + cDay + "|");
      }
      ctr++;
    }

 

    private void parseRelationshipSchedule() {
        String name = getTagName(currentString);
	logger.debug("Parsing Schedule " + name);
	int ctrIn = ctr;
	currentString = lines[++ctr];
	while(!(currentString.startsWith("<"))) {
	    String[] relationships = currentString.split(",");
	    relSchedule.addRelationship(relationships);
	    currentString = lines[++ctr];
	}
	if(logger.isDebugEnabled()) {
	    logger.debug("Parsed Schedule " + name 
			 + " there were " + (ctr - ctrIn) +
			 " lines ");
	}
    }

    private void parseInstanceSchedule() {
        String name = getTagName(currentString);
	logger.debug("Parsing Schedule " + name);
	int ctrIn = ctr;
	currentString = lines[++ctr];
	while(!(currentString.startsWith("<"))) {
	    currentString = lines[++ctr];
	}
	if(logger.isDebugEnabled()) {
	    logger.debug("Parsed Schedule " + name 
			 + " there were " + (ctr - ctrIn) +
			 " discarded lines ");
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

