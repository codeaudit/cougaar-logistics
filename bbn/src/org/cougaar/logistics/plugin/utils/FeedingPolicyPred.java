/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.logistics.plugin.utils;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.logistics.ldm.policy.FeedingPolicy;


public class FeedingPolicyPred implements UnaryPredicate {

  String feedingPolicyName;
  public FeedingPolicyPred(String service) {
   feedingPolicyName = service+"FeedingPolicy";
  }
  
  public boolean execute(Object o) {
    if (o instanceof FeedingPolicy) {
      FeedingPolicy pol = (FeedingPolicy) o;
      if (pol.getName().equals(feedingPolicyName)) {
	return true;
      } 
    } 
    return false;
  } 

  public String getFeedingPolicyName() {
    return feedingPolicyName;
  }

  public boolean equals(Object o) {
    if (o instanceof FeedingPolicyPred) {
      String tmp = ((FeedingPolicyPred)o).getFeedingPolicyName();
      if (tmp.equals(feedingPolicyName)){
	return true;
      }
    }
    return false;
  }

  public int hashCode() {
    return feedingPolicyName.hashCode();
  }

} 
