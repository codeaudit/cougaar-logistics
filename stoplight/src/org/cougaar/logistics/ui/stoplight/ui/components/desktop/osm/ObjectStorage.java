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

/***********************************************************************************************************************
<b>Description</b>: This class is used exclusively by the Object Storage manager and its supporting classes to store
                    each object and its associated information, such as expriation time.

<br><b>Notes</b>:<br>
                  - This class also generates the ObjectID instance associated with the object

***********************************************************************************************************************/
public class ObjectStorage
{
  // Make these package visible for faster access

  ObjectID oid = new ObjectID();
  int hashCode = oid.hashCode();

  // Forget some of the initializers since they are supposed to take extra time
  Object object;
  Object previousObject;
  long expiration;
  boolean expirationSet;

  long actionType = NotificationEvent.STORE;

	/*********************************************************************************************************************
  <b>Description</b>: Constructor for building an ObjectStorage instance representing an object without an expiration
                      time.

  <br>
  @param object Object to be represented
	*********************************************************************************************************************/
  ObjectStorage(Object object)
  {
    this.object = object;

    this.expirationSet = false;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Constructor for building an ObjectStorage instance representing an object with an expiration
                      time.

  <br>
  @param object Object to be represented
  @param expiration Object's expiration time in the form of milliseconds since 1970
	*********************************************************************************************************************/
  ObjectStorage(Object object, long expiration)
  {
    this.object = object;

    this.expiration = expiration;
    this.expirationSet = true;
  }

	/*********************************************************************************************************************
  <b>Description</b>: Returns the hash code of the object ID associated with the object.

  <br><b>Notes</b>:<br>
	                  - The hashcode of the object ID should be unique, so this object can be used in a hashtable

  <br>
  @return Hashcode of the object ID associated with the object
	*********************************************************************************************************************/
  public int hashCode()
  {
    return(hashCode);
  }

	/*********************************************************************************************************************
  <b>Description</b>: Commits the pending transactions on this object and generates an event for the transactions.

  <br>
  @return Event describing the commited transactions, or null if there were no pending transactions
	*********************************************************************************************************************/
  NotificationEvent commit()
  {
    NotificationEvent event = null;

    if (actionType != 0x00L)
    {
      event = new NotificationEvent(oid, object, previousObject, actionType, expiration, expirationSet);

      actionType = 0x00L;
      previousObject = null;
    }

    return(event);
  }
}
