package org.cougaar.logistics.plugin.servicediscovery;

import org.cougaar.servicediscovery.plugin.SDRegistrationPlugin;
import org.cougaar.servicediscovery.description.StatusChangeMessage;
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

public class ALRegistrationPlugin extends SDRegistrationPlugin {

  protected void handleStatusChange(StatusChangeMessage statusChange) {
    if(statusChange instanceof ALStatusChangeMessage) {
      statusChange.setStatus(StatusChangeMessage.COMPLETED);
      statusChange.setRegistryUpdated(true);
      publishChange(statusChange);
    }
    else {
      super.handleStatusChange(statusChange);
    }
  }



}