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
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
 
package org.cougaar.logistics.servlet;

import java.lang.reflect.Method;
import java.lang.ClassNotFoundException;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.core.servlet.SimpleServletComponent;

import javax.servlet.Servlet;

/** 
 * <pre>
 * A special servlet component for the LogisticsInventoryServlet.
 *
 * Can't use SimpleServletComponent because we need additional services.
 * Specifically we need the LoggingService and the AlarmService for 
 * current time stamp.
 *
 **/


public class LogisticsInventoryServletComponent extends SimpleServletComponent {

    protected Servlet createServlet() {

	LogisticsInventoryServlet invServe = new LogisticsInventoryServlet();
	
	AlarmService alarmService = (AlarmService)
	    serviceBroker.getService(invServe,
				     AlarmService.class,
				     null);
	
	LoggingService logService = (LoggingService)
	    serviceBroker.getService(invServe,
				     LoggingService.class,
				     null);
	

	
	// create the support
	SimpleServletSupport support;
	try {
	    support = createSimpleServletSupport(invServe);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to create Servlet support: "+
				       e.getMessage());
	}
	
	// set the support
	try {
	    invServe.setSimpleServletSupport(support);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set Servlet support: "+
				       e.getMessage());
	}

	// set the logging service
	try {
	    invServe.setLoggingService(logService);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set LoggingService: "+
				       e.getMessage());
	}

	// set the alarm service
	try {
	    invServe.setAlarmService(alarmService);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set alarm service: "+
				       e.getMessage());
	}
	
	return invServe;

    }

}

