/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.lang.reflect.Method;


public interface Copiable
{
    public void copyFrom(Object source) throws MismatchException;

    public Method getReader(String readerName);
    
    public Method getWriter(String writerName);

    // NOT REENTRANT (at least as far as indentation is concerned)
    public String toString();
}
