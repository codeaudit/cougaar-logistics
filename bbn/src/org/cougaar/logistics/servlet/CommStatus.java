/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
