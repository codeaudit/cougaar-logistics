/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/Attic/PathString.java,v 1.1 2002-05-14 20:41:08 gvidaver Exp $ */

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


public class PathString
{
    public static String firstname(String source)
    {
	return firstname(source, '/');
    }

    public static String firstname(String source, int ch)
    {
	int divider = source.indexOf(ch);
	if ( divider > -1 )
	    return source.substring(0, divider);
	else
	    return source;
    }

    public static String restname(String source)
    {
	return restname(source, '/');
    }

    public static String restname(String source, int ch)
    {
	int divider = source.indexOf(ch);
	if ( divider > -1 )
	    return source.substring(divider + 1);
	else
	    return source;
    }

    public static String basename(String source)
    {
	return basename(source, '/');
    }

    public static String basename(String source, int ch)
    {
	int divider = source.lastIndexOf(ch);
	if ( divider > -1 )
	    return source.substring(divider + 1);
	else
	    return source;
    }

    public static String dirname(String source)
    {
	return dirname(source, '/');
    }

    public static String dirname(String source, int ch)
    {
	int divider = source.lastIndexOf(ch);
	if ( divider > -1 )
	    return source.substring(0, divider);
	else
	    return source;
    }

    public static Vector split(String source, int ch)
    {
	Vector result = new Vector();
	String copy = new String(source);
	while ( true ) {
	    int cutIndex = copy.indexOf(ch);
	    if ( cutIndex == -1 )
		break;
	    result.add(copy.substring(0, cutIndex));
	    copy = copy.substring(cutIndex + 1, copy.length());
	}
	result.add(copy);
	return result;
    }
}
