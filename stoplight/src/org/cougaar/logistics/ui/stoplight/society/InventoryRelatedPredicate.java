package org.cougaar.logistics.ui.stoplight.society;

import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.cougaar.util.UnaryPredicate;

/**
  * Used for collecting plan objects required for retrieving the inventory
  * info we need.
  */
public class InventoryRelatedPredicate implements UnaryPredicate
{
  Collection assetsOfInterest = null;

  public boolean execute(Object parm1)
  {
    return ExtractionHelper.checkInventoryRelated(assetsOfInterest, parm1);
  }

  public void setAssetsOfInterest(String assetString)
  {
    assetsOfInterest = new LinkedList();
    StringTokenizer assetTok = new StringTokenizer(assetString, ",");
    while (assetTok.hasMoreTokens())
    {
      assetsOfInterest.add(assetTok.nextToken());
    }
  }
}