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
