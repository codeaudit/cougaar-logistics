package org.cougaar.logistics.ldm;

import org.cougaar.core.domain.Factory;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;

/**
 * Created by IntelliJ IDEA.
 * User: lgoldsto
 * Date: May 30, 2003
 * Time: 7:56:44 PM
 * To change this template use Options | File Templates.
 */
public class LogisticsFactory implements Factory{
  LDMServesPlugin myLDM;

  public LogisticsFactory(LDMServesPlugin ldm) {
    myLDM = ldm;
    PlanningFactory pf = ldm.getFactory();
    pf.addAssetFactory(new org.cougaar.logistics.ldm.asset.AssetFactory());
    pf.addPropertyGroupFactory(new org.cougaar.logistics.ldm.asset.PropertyGroupFactory());
  }
}
