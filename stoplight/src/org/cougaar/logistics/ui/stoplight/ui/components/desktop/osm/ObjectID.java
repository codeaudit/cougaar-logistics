/* 
 * <copyright>
 *  
 *  Copyright 1997-2004 Clark Software Engineering (CSE)
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

package org.cougaar.logistics.ui.stoplight.ui.components.desktop.osm;

import java.net.InetAddress;
import java.rmi.server.UID;

/***********************************************************************************************************************
<b>Description</b>: This class combines the network IP address of the machine which it is generated on and a UID
                    instance which is intended to produce a unique ID.

***********************************************************************************************************************/
public class ObjectID implements java.lang.Cloneable, java.io.Serializable
{
  private UID uid;
  private InetAddress inetAddress;
  private int hashCode;
  
	/*********************************************************************************************************************
  <b>Description</b>: Constructs an ObjectID instance and generates a hash code for this instance.
	*********************************************************************************************************************/
  ObjectID()
  {
    try
    {
      uid = new UID();
      inetAddress = InetAddress.getLocalHost();
      hashCode = (inetAddress.toString() + uid.toString()).hashCode();
    }
    catch (Throwable t)
    {
      throw(new RuntimeException("Rethrown exception: " + t.toString()));
    }
  }

	/*********************************************************************************************************************
  <b>Description</b>: Overrides Object.equals() to compare two ObjectID instances to determine if they refer to the 
                      same UID and network IP address.

  <br>
  @param obj Instance of ObjectID to compare to the current instance
  @return True if the ObjectIDs are equivilant, false otherwise
	*********************************************************************************************************************/
  public boolean equals(Object obj)
  {
    ObjectID oid = (ObjectID)obj;
    
    return(this.uid.equals(oid.uid) && this.inetAddress.equals(oid.inetAddress));
  }

	/*********************************************************************************************************************
  <b>Description</b>: Overrides Object.hashCode() to provide a unique hash code for the ObjectID instance.

  <br><b>Notes</b>:<br>
	                  - The hash code generated is calculated by the toString concatenation of the UID instance and the
	                    network IP address and the hashCode() method is called on the resulting string.

  <br>
  @return Hash code of the ObjectID instance
	*********************************************************************************************************************/
  public int hashCode()
  {
    return(hashCode);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Provides text representation of the ObjectID instance in the form of a network IP address and
                      an UID.

  <br>
  @return Text representation of the ObjectID instance
	*********************************************************************************************************************/
  public String toString()
  {
    return(inetAddress + ": " + uid);
  }
}
