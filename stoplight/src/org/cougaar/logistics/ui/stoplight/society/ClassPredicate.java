package org.cougaar.logistics.ui.stoplight.society;

import org.cougaar.util.UnaryPredicate;

/**
 * Simple unary predicate that looks for objects of a certain class.
 * Uses bean interface for specifying class so it could be used in an
 * assessment query.
 */
public class ClassPredicate implements UnaryPredicate
{
  private Class wantedClass = null;

  public void setClass(String className)
  {
    try {
      wantedClass = Class.forName(className);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean execute(Object parm1)
  {
    return wantedClass.isInstance(parm1);
  }
}