/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.plugin.packer; 

import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preposition;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Preposition;
 
import org.cougaar.logistics.ldm.Constants;
import org.cougaar.glm.ldm.asset.Ammunition;
import org.cougaar.glm.packer.GenericPlugin;

/**
  * This UnaryPredicate is used to test whether Tasks should be
  * packed together, in the Packer's packing rule.  It picks out
  * all supply tasks that request Ammunition.
  */

public class ALAmmoPackerPredicate  {

  public static UnaryPredicate getInputTaskPredicate() {
    return new UnaryPredicate() {
      
      public boolean execute(Object o) {
        if ((o instanceof Task) &&
            (((Task)o).getVerb().equals(Constants.Verb.SUPPLY)) &&
            (((Task)o).getPrepositionalPhrase(GenericPlugin.INTERNAL) == null) &&
            (((Task)o).getDirectObject() instanceof Ammunition) &&
            (((Task)o).getPrepositionalPhrase("LOW_FIDELITY") == null)) {// BOZO - change this to a const reference later...
          return true;
        } else {
	  /*if ((o instanceof Task) &&
            (((Task)o).getVerb().equals(Constants.Verb.SUPPLY))) {
            if (((Task)o).getPrepositionalPhrase(GenericPlugin.INTERNAL) != null)
            System.out.println ("AmmoPackerPredicate - Ignoring supply task " + ((Task)o).getUID() + 
            " b/c has internal prep.");
	    else if (!(((Task)o).getDirectObject() instanceof Ammunition))
            System.out.println ("AmmoPackerPredicate - Ignoring supply task " + ((Task)o).getUID() + 
            " b/c d.o. not Ammunition.");
	    else if (!(((Task)o).getPrepositionalPhrase("LOW_FIDELITY") != null))
            System.out.println ("AmmoPackerPredicate - Ignoring supply task " + ((Task)o).getUID() + 
            " b/c has LOW_FIDELITY prep.");
	    else
            System.out.println ("AmmoPackerPredicate - Ignoring supply task " + ((Task)o).getUID() + 
            " b/c - ?");
            
            } */
          return false;
        }
      }
    };
  }

  public static UnaryPredicate getPlanElementPredicate() {
    return new UnaryPredicate() {
      private UnaryPredicate myInputTaskPredicate = getInputTaskPredicate();

      public boolean execute(Object o) {
        if (o instanceof PlanElement) {
          Task task = ((PlanElement)o).getTask();
          if (myInputTaskPredicate.execute(task) ||
              ((task.getPrepositionalPhrase(GenericPlugin.INTERNAL) !=null) &&
               (task.getDirectObject() instanceof Ammunition))) {
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
    };
  }
}





