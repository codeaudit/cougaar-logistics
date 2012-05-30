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
 
package org.cougaar.logistics.servlet;

import java.lang.reflect.Method;
import java.lang.ClassNotFoundException;
import java.util.List;

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

    public final static String PRINT_ORG_ACTIVITIES = "PRINT_ORG_ACTIVITIES";
    

    protected boolean printOrgActs=false;

 /**
   * Save our Servlet's configurable path, for example
   * "/test".
   * <p>
   * This is only set during initialization and is constant
   * for the lifetime of the Servlet.
   */
  public void setParameter(Object o) {

    super.setParameter(o);
    List l = (List)o;

    if(l.size() == 3) {
	Object o3 = l.get(2);
	if(!(o3 instanceof String)) {
	    throw new IllegalArgumentException("Expecting third optional argument as a string, not ("+o3+")");
	}
	String[] keyAndValue = ((String) o3).split("=");
	if((keyAndValue.length == 2) &&
	   (keyAndValue[0].trim().equals(PRINT_ORG_ACTIVITIES)) &&
	   (!(keyAndValue[1].trim().equals("")))) {
	    printOrgActs = keyAndValue[1].trim().toLowerCase().equals("true");
	}
	else {
	    throw new IllegalArgumentException("Optional thirg argument should be " + PRINT_ORG_ACTIVITIES + "=<true or false>");
	}

    }
  }

    protected Servlet createServlet() {

	LogisticsInventoryServlet invServe = new LogisticsInventoryServlet();
	
	AlarmService alarmService = (AlarmService)
	    getService(invServe, AlarmService.class, null);
	
	LoggingService logService = (LoggingService)
	    getService(invServe, LoggingService.class, null);
	

	
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


	// set the alarm service
	try {
	    invServe.setPrintOrgActs(printOrgActs);
	} catch (Exception e) {
	    throw new RuntimeException(
				       "Unable to set print org activities boolean: "+
				       e.getMessage());
	}


	
	
	return invServe;

    }

}

