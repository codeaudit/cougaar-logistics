/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
