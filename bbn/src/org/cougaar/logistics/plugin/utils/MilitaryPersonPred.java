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
import org.cougaar.glm.ldm.asset.MilitaryPerson;
import org.cougaar.glm.ldm.asset.Person;
import org.cougaar.planning.ldm.asset.AggregateAsset;

/** 
 *  Military personnel, consumers for Subsistence 
 **/
public class MilitaryPersonPred implements UnaryPredicate {
  private static final String predString = "MilitaryPersonPred";
    
  public boolean execute (Object o) {
    if (o instanceof MilitaryPerson) {
      return true;
    }
    if (o instanceof AggregateAsset) {
      if (((AggregateAsset)o).getAsset() instanceof MilitaryPerson) {
	return true;
      }
    }
    return false;
  }

  public boolean equals(Object o) {
    if (o instanceof MilitaryPersonPred) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return predString.hashCode();
  }
}
