package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.logistics.plugin.utils.ALStatusChangeMessage;
import java.util.Collection;
import java.util.Iterator;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class ALRegistrationPlugin extends SimplePlugin {

  private IncrementalSubscription statusChangeSubscription;

  private UnaryPredicate statusChangePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof ALStatusChangeMessage);
    }
  };

  protected void setupSubscriptions() {
    statusChangeSubscription =
      (IncrementalSubscription) subscribe(statusChangePredicate);
  }

  protected void execute () {

    if (statusChangeSubscription.hasChanged()) {
      Collection adds = statusChangeSubscription.getAddedCollection();
      handleStatusChange(adds);
    }

  }

  private void handleStatusChange(Collection statusChangeMessages) {
    for (Iterator iterator = statusChangeMessages.iterator(); iterator.hasNext();) {
      ALStatusChangeMessage statusChange = (ALStatusChangeMessage) iterator.next();
      statusChange.setRegistryUpdated(true);
      System.out.println("ALRegistrationPlugin change registration here");
      publishChange(statusChange);
    }


  }



}