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
package org.cougaar.logistics.plugin.manager;

import org.cougaar.core.adaptivity.ConstrainingClause;
import org.cougaar.core.adaptivity.ConstraintOperator;
import org.cougaar.core.adaptivity.ConstraintPhrase;
import org.cougaar.core.adaptivity.InterAgentOperatingModePolicy;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.PolicyKernel;

/**
 * Relay used to notify logistics community of changes in load status
 **/
public class FallingBehindPolicy extends InterAgentOperatingModePolicy {
  public static String FALLING_BEHIND = "FallingBehind";

  public FallingBehindPolicy(double fallingBehindValue) {
    super(ConstrainingClause.TRUE_CLAUSE, new ConstraintPhrase[0]);
    setFallingBehindValue(fallingBehindValue);
  }

  /**
   * Gets the name of the Agent whose load status is reported.
   *
   * @return String Name of the agent
   */
  public void  setFallingBehindValue(double fallingBehindValue) {
    ConstraintPhrase[] omConstraints = new ConstraintPhrase[1];
    omConstraints[0] = new ConstraintPhrase(FALLING_BEHIND,
                                            ConstraintOperator.EQUAL, 
                                            new OMCRangeList(fallingBehindValue));
    
    PolicyKernel policyKernel = 
      new PolicyKernel(ConstrainingClause.TRUE_CLAUSE, omConstraints);

    setPolicyKernel(policyKernel);
  }

  /**
   * appliesToThisAgent - return true if FallingBehindPolicy applies to 
   * this Agent.
   * Overrides default dehaviour - FallingBehindPolicyies are applied at the
   * source as well at the target Agents. 
   */
  public boolean appliesToThisAgent() {
    if (getSource() == null) {
      /* Policy originated here - Am I also one of the targets?
       * (KLUDGE) - should probably use the community code to resolves the targets
       * and then look to see whether this agent is included.
       */
      return true;
    } else {
      return true;
    }
    
  }
}













