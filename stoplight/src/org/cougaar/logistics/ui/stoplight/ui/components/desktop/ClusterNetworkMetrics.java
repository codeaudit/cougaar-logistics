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
package org.cougaar.logistics.ui.stoplight.ui.components.desktop;

import java.util.Vector;

public class ClusterNetworkMetrics
{

  public String clusterName;
  public String ipAddress;
  public double lat;
  public double lon;
  public int xCoord, yCoord;

  public Vector ipRcvPackets = new Vector();
  public Vector ipLostPackets = new Vector();
  public Vector ipSentPackets = new Vector();
  public Vector timeStamp = new Vector();

  public ClusterNetworkMetrics()
  {
  }

  public String toString()
  {
    return new String (        "cluster: " + clusterName + "\n" +
                               "latitude: " + lat + "\n" +
                               "longitude: " + lon + "\n" );
  }

  public boolean pointWithin (int x, int y)
  {
    boolean b = true;

    if (x < xCoord - 5 || x > xCoord + 5)
      b = false;

    if ( y < yCoord - 5 || y > yCoord + 5)
      b = false;

    return b;
  }



}