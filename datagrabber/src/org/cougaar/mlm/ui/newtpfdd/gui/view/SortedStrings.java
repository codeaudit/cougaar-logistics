/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/

package org.cougaar.mlm.ui.newtpfdd.gui.view;


import java.util.Vector;
import java.util.Iterator;


public class SortedStrings extends Vector
{
    private boolean ascending = true;

    public SortedStrings()
    {
	super();
    }

    public SortedStrings(boolean isAscending)
    {
	ascending = isAscending;
    }

    public void addSorted(String s)
    {
	if ( ascending ) {
	    for ( int i = 0; i < size(); i++ )
		if ( s.compareTo((String)get(i)) <= 0 ) { 
		    add(i, s);
		    return;
		}
	    add(s);
	}
	else {
	    for ( int i = 0; i < size(); i++ )
		if ( s.compareTo((String)get(i)) >= 0 ) {
		    add(i, s);
		    return;
		}
	    add(s);
	}
    }

    public void mergeSortedUnique(Vector v)
    {
	for ( Iterator i = v.iterator(); i.hasNext(); ) {
	    Object o = i.next();
	    if ( !contains(o) && o.toString().indexOf('?') == -1 )
		addSorted(o.toString());
	}
    }

    public void mergeSortedUnique(Vector v, Vector comments)
    {
	for ( int i = 0; i < v.size(); i++ ) {
	    Object o = v.get(i), comment = comments.get(i);
	    String gonnaAdd = "[" + comment.toString() + "] " + o.toString();
	    if ( !contains(gonnaAdd) && o.toString().indexOf('?') == -1 )
		addSorted(gonnaAdd);
	}
    }

    public static String[] removeBracketedStuff(String[] source)
    {
	String[] dest = new String[source.length];
	for ( int i = 0; i < source.length; i++ )
	dest[i] = source[i].substring(source[i].indexOf("] ") + 2, source[i].length());
	return dest;
    }
}
