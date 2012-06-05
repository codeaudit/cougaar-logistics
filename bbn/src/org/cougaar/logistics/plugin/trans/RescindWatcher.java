/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.trans;

import java.util.Enumeration;

import org.cougaar.lib.callback.UTILFilterCallbackAdapter;
import org.cougaar.lib.callback.UTILFilterCallbackListener;
import org.cougaar.lib.filter.UTILPluginAdapter;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.log.Logger;

public class RescindWatcher extends UTILPluginAdapter implements UTILFilterCallbackListener {
  public void setupFilters () {
    super.setupFilters ();
    addFilter (new AllTasksFilter (this, logger));
    warn(getName () + " - adding filter for all tasks.");
  }

  class AllTasksFilter extends UTILFilterCallbackAdapter {
    public AllTasksFilter (UTILFilterCallbackListener listener, Logger logger) {
      super (listener, logger);
    }

    protected UnaryPredicate getPredicate () {
      return new UnaryPredicate() {
	  public boolean execute(Object o) {
	    return ( o instanceof Task );
	  }
	};
    }

    public void reactToChangedFilter () {
      if (getSubscription().getRemovedCollection().size () > 0) {
	warn (getName () + " - got " + getSubscription().getRemovedCollection().size () + " removed tasks.");
      }

      if (getSubscription().getRemovedList().hasMoreElements ()) {
	Enumeration removedtasks = getSubscription().getRemovedList();
	while (removedtasks.hasMoreElements()) {
	  Task task = (Task) removedtasks.nextElement();
	  warn(getName () + " - task " + task.toString () + " was removed.");
	}
      }
    }
  }
}
