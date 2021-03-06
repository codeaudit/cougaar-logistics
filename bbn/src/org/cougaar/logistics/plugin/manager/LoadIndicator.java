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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.util.log.Logging;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * Relay used to notify logistics community of changes in load status
 **/
public class LoadIndicator extends RelayAdapter {
  public static String NORMAL_LOAD = "Normal";
  public static String MODERATE_LOAD = "Moderate";
  public static String SEVERE_LOAD = "Severe";


  private String myAgentName = null;
  private String myReportingSensorClassName = null;
  private String myLoadStatus = null;
  private transient String myToString = null;

  public static boolean validLoadStatus(String loadStatus) {
    return (loadStatus.equals(NORMAL_LOAD) ||
            loadStatus.equals(MODERATE_LOAD) ||
            loadStatus.equals(SEVERE_LOAD));
  }

  public LoadIndicator(Object reportingSensor, String agentName, UID uid, 
                       String loadStatus) {
    super();
    setReportingSensorClassName(reportingSensor.getClass());
    setAgentName(agentName);
    setUID(uid);
    setLoadStatus(loadStatus);
  }

  /**
   * Gets the name of the Agent whose load status is reported.
   *
   * @return String Name of the agent
   */
  public String getAgentName() {
    return myAgentName;
  }

  /**
   * Sets the name of the Agent whose load status is reported.
   *
   * @param agentName String name of the agent
   */
  public void setAgentName(String agentName) {
    if ((myAgentName != null) &&
        (!myAgentName.equals("")) &&
        (!myAgentName.equals(agentName))) {
      Logging.defaultLogger().warn("Attempt to reset agent name ignored.");
    } else {
      myAgentName = agentName;
      myToString = null;
    }
  }

  /**
   * Gets the class name of the sensor which reports the load status
   *
   * @return String Class name of the reporting sensor
   */
  public String getReportingSensorClassName() {
    return myReportingSensorClassName;
  }

  /**
   * Sets the class name of the sensor which reports the load status
   *
   * @param reportingSensorClassName String class name of the reporting sensor
   */
  public void setReportingSensorClassName(String reportingSensorClassName) {
    if ((myReportingSensorClassName != null) &&
        (!myReportingSensorClassName.equals("")) &&
        (!myReportingSensorClassName.equals(reportingSensorClassName))) {
      Logging.defaultLogger().warn("Attempt to reset reporting sensor class ignored.");
    } else {
      myReportingSensorClassName = reportingSensorClassName;
      myToString = null;
    }
  }


  /**
   * Sets the class name of the sensor which reports the load status
   *
   * @param reportingSensorClass Class of the reporting sensor
   */
  public void setReportingSensorClassName(Class reportingSensorClass) {
    setReportingSensorClassName(reportingSensorClass.toString());
  }


  /**
   * Gets the reported load status of the agent
   *
   * @return String Load status of the agent
   */
  public String getLoadStatus() {
    return myLoadStatus;
  }

  /**
   * Sets the reported load status for the agent. Should be one of the
   * defined statics.
   *
   * @param loadStatus Reported load status for the agent.
   */
  public void setLoadStatus(String loadStatus) {
    if (!validLoadStatus(loadStatus)) {
      Logging.defaultLogger().warn("Attempt to set load status to an unrecognised value - " + loadStatus);
    } else {
      myLoadStatus = loadStatus;
      myToString = null;
    }
  }

  protected boolean contentChanged(RelayAdapter newLoadIndicator) {
    LoadIndicator loadIndicator = (LoadIndicator) newLoadIndicator;

    // Only the load status should actually change   
    if (!getLoadStatus().equals(loadIndicator.getLoadStatus())) {
      setLoadStatus(loadIndicator.getLoadStatus());
      return true;
    } else {
      return (super.contentChanged(newLoadIndicator));
    }
  }

  public String toString() {
    if (myToString == null) {
      myToString = getClass() + ": agent=<" + getAgentName() + ">, sensor=<" +
        getReportingSensorClassName() + ">, load status=<" + getLoadStatus() + 
        ">, UID=<" + getUID() + ">";
    }

    return myToString;
  }
  
}










