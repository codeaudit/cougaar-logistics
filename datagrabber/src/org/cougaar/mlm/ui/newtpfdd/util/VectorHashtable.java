/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/Attic/VectorHashtable.java,v 1.1 2002-05-14 20:41:08 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.util.Vector;
import java.util.Hashtable;


/*
  A hashtable which supports multiple objects of the same key by
  stashing them all in the same externally visible bucket (vector).
  Warning: potentially inefficient with many duplicates; intended for
  the occasional (< 10/bucket) overlap.

  Note this is formally incomplete in semantics; we just override the
  methods currently of use to us.

  put and get are overridden to ensure type discipline; vectorPut and
  vectorGet are convenience methods only to avoid annoying casting.

  get: For lookups, we return a vector of ALL the elements in the
  bucket if there are one or more; null otherwise.

  put: We always return null, since no object can ever replace another
  -- semantics have changed here.
*/
public class VectorHashtable extends Hashtable
{
    public synchronized Object put(Object key, Object value)
    {
	Vector v;
	
	if ( (v = (Vector)(get(key))) == null ) {
	    v = new Vector();
	    v.add(value);
	    super.put(key, v);
	}
	else
	    if ( v.contains(value) )
		Debug.out("VHT:put Note ignoring duplicate: key: '" + key + "' value: '" + value + "'");
	    else
		v.add(value);
	return null;
    }

    public synchronized Vector vectorPut(Object key, Object value)
    {
	return (Vector)(put(key, value));
    }

    public synchronized Vector vectorGet(Object key)
    {
	return (Vector)(get(key));
    }

    // version that puts an empty vector there if there isn't one. useful for
    // having a common reference object even when it doesn't contain anything
    // yet -- esp. for multiple thread access to that vector.
    public synchronized Vector vectorGetWithCreate(Object key)
    {
	Vector v = (Vector)(get(key));
	if ( v == null ) {
	    v = new Vector();
	    super.put(key, v);
	}
	return v;
    }

  // Retrieve the Vector from the hashtable and remove it at the
  // same time so that no one else gets it

  public synchronized Vector findAndRemove(Object key) {
    Vector found = vectorGet(key);
    if (found != null)
      remove(key);
    return found;

  }


}

