package org.cougaar.logistics.plugin.trans.tools;

import org.cougaar.core.service.BlackboardService;

import org.cougaar.lib.callback.UTILFilterCallback;
import org.cougaar.lib.callback.UTILFilterCallbackListener;
import org.cougaar.lib.param.ParamMap;

public interface BlackboardPlugin extends UTILFilterCallbackListener {
  /** params for this plugin */
  ParamMap getMyParams ();
  void addFilter  (UTILFilterCallback callbackObj);
  BlackboardService getBlackboard ();
}
