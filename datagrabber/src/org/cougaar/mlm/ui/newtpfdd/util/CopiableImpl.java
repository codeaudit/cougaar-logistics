/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/CopiableImpl.java,v 1.1 2002-05-14 20:41:08 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.beans.PropertyDescriptor;
import java.lang.IllegalAccessException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


public class CopiableImpl implements Copiable
{
    static String spaces = "";
    protected Object target;

    public CopiableImpl(Object target)
    {
	this.target = target;
	// Debug.out("CI:CI Inited target to a " + target.getClass().getName());
    }

    public void copyFrom(Object source) throws MismatchException
    {
	if ( !source.getClass().isAssignableFrom(target.getClass()) )
	    throw new MismatchException("Attempt to copy from mismatched classes: "
					+ source.getClass().getName() + "X-> "
					+ target.getClass().getName());
	Object[] value = new Object[1];
	PropertyDescriptor[] properties = BeanInfoProvider.getProperties(source.getClass());
	for ( int i = 0; i < properties.length; i++ ) {
	    try {
		value[0] = properties[i].getReadMethod().invoke(source, null);
		if ( properties[i].getWriteMethod() == null )
		    continue; // this is the Class field; immaterial
		properties[i].getWriteMethod().invoke(target, value);
	    }
	    catch ( IllegalAccessException e ) {
		OutputHandler.out(ExceptionTools.toString("CI:cF", e));
	    }
	    catch ( InvocationTargetException e ) {
		OutputHandler.out(ExceptionTools.toString("CI:cF", e));
	    }
	}
    }

    public Method getReader(String readerName)
    {
	return BeanInfoProvider.getReader(target.getClass(), readerName);
    }
    
    public Method getWriter(String writerName)
    {
	return BeanInfoProvider.getWriter(target.getClass(), writerName);
    }

    // NOT REENTRANT (at least as far as indentation is concerned)
    public String toString()
    {
	String saveSpaces = spaces;
	spaces += "  ";
	String myString = "";
	Object value = null;
	
	// Debug.out("Cop: tS " + target.getClass().getName());
	PropertyDescriptor[] properties = BeanInfoProvider.getProperties(target.getClass());
	for ( int i = 0; i < properties.length; i++ ) {
	    try {
		Method readMethod = properties[i].getReadMethod();
		if ( readMethod == null ) {
		    OutputHandler.out("CI:tS Warning: no read method for " + properties[i].getName());
		    continue;
		}
		value = readMethod.invoke(target, null);
	    }
	    catch ( IllegalAccessException e ) {
		OutputHandler.out(ExceptionTools.toString("CI:tS", e));
	    }
	    catch ( InvocationTargetException e ) {
		OutputHandler.out(ExceptionTools.toString("CI:tS", e));
	    }
	    if ( value == null )
		myString += spaces + properties[i].getName() + ": [null]\n";
	    else // if ( properties[i].getWriteMethod() != null )
		if ( properties[i].getPropertyType().isArray() ) {
		    int length = java.lang.reflect.Array.getLength(value);
		    if ( length == 0 )
			myString += spaces + properties[i].getName() + ": [none]\n";
		    else {
			myString += spaces + properties[i].getName() + ":\n";
			for ( int j = 0; j < length; j++ ) {
			    Object element = java.lang.reflect.Array.get(value, j);
			    if ( element instanceof Copiable )
				myString += element.toString();
			    else
				myString += spaces + "  " + element.toString() + "\n";
			}
		    }
		}
		else {
		    myString += spaces + properties[i].getName() + ": ";
		    if ( value instanceof Copiable )
			myString += "\n" + value;
		    else
			myString += value + "\n";
		}
	}
	spaces = saveSpaces;
	return myString;
    }

}
