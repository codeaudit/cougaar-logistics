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

package org.cougaar.logistics.plugin.strattrans;

import org.cougaar.lib.callback.UTILExpandableTaskCallback;
import org.cougaar.lib.callback.UTILGenericListener;

import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import org.cougaar.util.log.Logger;

/**
 * For use with (threaded?) expanders.
 * 
 * Filters for tasks without plan elements.
 *    This allows tasks which may be the child of another task
 *     (e.g., DetermineRequirement tasks that are child of GLS)
 *    (unlike UTILExpandableTaskCallback which also filters tasks with no workflow)
 */

public class UTILExpandableChildTaskCallback extends UTILExpandableTaskCallback {
  public UTILExpandableChildTaskCallback (UTILGenericListener listener, Logger logger) {
    super (listener, logger);
  }

  /** overridden to ignore workflow
   */
  protected UnaryPredicate getPredicate () {
    return new UnaryPredicate() {
	public boolean execute(Object o) {
	  if ( o instanceof Task ) {
	    if (logger.isDebugEnabled()) {
	      logger.debug("T:"+o);
	      logger.debug("PE:"+((Task)o).getPlanElement());
	    }
	    return ( (((Task)o).getPlanElement() == null ) &&
		     ((UTILGenericListener) myListener).interestingTask ((Task) o)); 
	  }
	  return false;
	}
      };
  }
}        
