/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.logistics.servlet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.Servlet;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.plugin.PluginBindingSite;

import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.SchedulerService;

import org.cougaar.core.servlet.BlackboardServletComponent;
import org.cougaar.core.servlet.SimpleServletSupport;

import org.cougaar.lib.xml.parser.ParamParser;
import org.cougaar.lib.param.Param;

import org.cougaar.util.ConfigFinder;

public class ConditionComponent extends BlackboardServletComponent {

  public static final String COMPONENT_PARAMETER = "conditionName";

  public final void setConditionService(ConditionService s) {
    conditionService = s;
  }
  /**
   * Get the condition service
   */
  protected ConditionService getConditionService() {
    return conditionService;
  }

  private Collection parameters;

  /**
   * Called just after construction (via introspection) by the 
   * loader if a non-null parameter Object was specified by
   * the ComponentDescription.
   * <p>
   * Save our Servlet's configurable path, for example
   * "/test".
   * <p>
   * This is only set during initialization and is constant
   * for the lifetime of the Servlet.
   *
   * @param o list of parameters to plugin
   */
  public void setParameter(Object o) {
    // expecting a List of [String, String]
    if (!(o instanceof List)) {
      throw new IllegalArgumentException(
        "Expecting a List parameter, not : "+
        ((o != null) ? o.getClass().getName() : "null"));
    }
    List l = (List)o;
    if (l.size() < 2) {
      throw new IllegalArgumentException(
          "Expecting a List with at least two elements,"+
          " \"classname\" and \"path\", not "+l.size());
    }
    Object o1 = l.get(0);
    Object o2 = l.get(1);
    if ((!(o1 instanceof String)) ||
        (!(o2 instanceof String))) {
      throw new IllegalArgumentException(
          "Expecting two Strings, not ("+o1+", "+o2+")");
    }

    // save the servlet classname and path
    this.classname = (String) o1;
    this.path = (String) o2;

    if (o != null) {
      parameters = (Collection) o;
    } else {
      parameters = new Vector(0);
    }
  }
  
  /** 
   * Get any component parameters passed by the component instantiator.
   */
  public Collection getParameters() {        
    return parameters;
  }

  /**
   * so a subclass can create a different servlet support just by overriding this method
   * perhaps the core should work like this?
   */
  protected SimpleServletSupport makeServletSupport () {
    if (log.isInfoEnabled())
      log.info ("Creating ConditionSupport, log is " + log);

    setConditionName (getParameters());

    // create a new support instance
    return new ConditionSupport (
        path,
        agentId,
        blackboardQuery,
        ns,
	log,
        getBlackboardService(),
	getConfigFinder(),
	getDomainService().getFactory(),
	getLDMService().getLDM(),
	getSchedulerService(),
	getConditionService(),
	serviceBroker,
	conditionName);
  }

  public final void setDomainService(DomainService s) {
    domainService = s;
  }
  protected final DomainService getDomainService() {
    return domainService;
  }

  /** 
   * Expecting conditionName=ferris as parameter to component. <br>
   * If none, defaults to DoubleCondition. <p>
   *
   * Potentially there could be a list of conditions, but then the gui would have to deal with it, etc...
   **/
  protected void setConditionName (Collection envParams) {
    if (log.isDebugEnabled()) {
      log.debug ("Creating param table for agent " + agentId);
    }

    for (Iterator i = envParams.iterator(); i.hasNext();) {
      String runtimeParam = (String)i.next();
      Param p = paramParser.getParam(runtimeParam);
      if(p != null){
	String name = p.getName();
	if (log.isDebugEnabled())
	  log.debug(agentId +
		    " - ConditionComponent.createParamTable() - got param name " + name
		    + " with value " + p);
	if (name.equals (COMPONENT_PARAMETER)) {
	  try {
	    conditionName = p.getStringValue();
	  } catch (Exception e) {
	    log.error ("Got Exception when reading value of parameter " + name);
	  }
	}
      }
    }
  }

  protected DomainService domainService = null;
  protected ConditionService conditionService;
  protected ParamParser paramParser = new ParamParser();
  protected String conditionName = "DoubleCondition";
}
