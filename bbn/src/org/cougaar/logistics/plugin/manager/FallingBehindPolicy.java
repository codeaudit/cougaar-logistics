/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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













