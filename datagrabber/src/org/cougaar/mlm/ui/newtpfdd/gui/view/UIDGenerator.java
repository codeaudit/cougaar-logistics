/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.mlm.ui.newtpfdd.gui.view;

import java.util.HashMap;
import java.util.Map;

public class UIDGenerator {
  public static final int CARRIER_PROTOTYPE = 0;
  public static final int ASSET_PROTOTYPE   = 1;
  public static final int CARRIER_INSTANCE  = 2;
  public static final int ASSET_INSTANCE    = 3;
  public static final int LEG               = 4;
  public static final int ORGANIZATION      = 5;
  public static final int EQUIPMENT         = 6;
  public static final int LOCATION          = 7;
  public static final int CONVOY            = 8;
  public static final int CATEGORY          = 9;
  public static final int MAX_TYPE          = CATEGORY;

  private static UIDGenerator oneInstance = null;
  
  long counter = 0;

  Map UIDToDBID = new HashMap ();

  Map [] maps;
  
  private UIDGenerator () {
	maps = new Map [MAX_TYPE+1];
	
	for (int i = 0; i <= MAX_TYPE; i++)
	  maps[i] = new HashMap ();
  }
  
  
  public static UIDGenerator getGenerator () {
	if (oneInstance == null)
	  oneInstance = new UIDGenerator ();
	return oneInstance;
  }
  
  private long makeUID () {	return counter++; }
  
  public synchronized String getDBUID (long uid) {
	return (String) UIDToDBID.get(new Long (uid));
  }
  
  public synchronized long getUIDForDBUID (int whichType, String DBUID) {
	return ((Long) maps[whichType].get (DBUID)).longValue ();
  }

  public synchronized long makeUID (int whichType, String DBUID) {
	long newid = makeUID ();
	Long longID = new Long (newid);

	maps[whichType].put (DBUID, longID);
	UIDToDBID.put       (longID, DBUID);

	return newid;
  }

  public synchronized long makeUnmappedUID () {
	return makeUID ();
  }
}
