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

package org.cougaar.logistics.servlet;

import java.io.Serializable;

/**
 *  This object represents the state of inter-communications (as opposed to intra-communications)
 *  of an agent and when it changed.
 */

public class CommStatus implements Serializable {
  private boolean commUp = true;
  long commLossTime = -1;
  long commRestoreTime = -1;
  String connectedAgentName;

  /**
   * Contructor for the CommStatus object
   * @param connectedAgentName The name of the agent for which the connection has been lost or restored
   */
  public CommStatus(String connectedAgentName) {
    this.connectedAgentName = connectedAgentName;
  }

  public boolean isCommUp() {
    return commUp;
  }

  public void setCommLoss(long when) {
    this.commLossTime = when;
    this.commUp = false;
  }

  public void setCommRestore(long when) {
    this.commRestoreTime = when;
    this.commUp = true;
  }

  public long getCommRestoreTime() {
    return this.commRestoreTime;
  }

  public long getCommLossTime() {
    return this.commLossTime;
  }

  public String getConnectedAgentName() {
    return this.connectedAgentName;
  }
}
