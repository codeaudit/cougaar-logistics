/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/BeanInfoProvider.java,v 1.3 2003-02-03 22:28:00 mthome Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/


package org.cougaar.mlm.ui.newtpfdd.util;


import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;



public class BeanInfoProvider
{
    static Hashtable ReaderMap = new Hashtable();
    static Hashtable WriterMap = new Hashtable();
    static Hashtable PropertiesMap = new Hashtable();
    
    static private void addClass(Class newClass)
    {
	BeanInfo beanInfo;
	String className = newClass.getName();
	try {
	    beanInfo = Introspector.getBeanInfo(newClass, Introspector.IGNORE_ALL_BEANINFO);
	} catch ( IntrospectionException e ) {
	    System.err.println(e);
	    return;
	}
	PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();
	Method readMethod, writeMethod;
	Vector propertyVector = new Vector();
	for ( int i = 0; i < properties.length; i++ ) {
	    PropertyDescriptor property = properties[i];
	    String key = className + '.' + property.getName();
	    if ( property.getName().indexOf('_') == property.getName().length() - 1 ) {
	      // Debug.out("BIP:aC Note: leaving out property " + property.getName() + " from class "
	      //	  + className);
	    }
	    else {
		propertyVector.add(property);
		readMethod = property.getReadMethod();
		if ( readMethod != null ) {
		    ReaderMap.put(key, readMethod);
		} else {
		    OutputHandler.out("BIP:aC Warning: no read method for " + key);
        }
	    }
	    writeMethod = property.getWriteMethod();
	    if ( writeMethod != null ) // for the class type itself
		WriterMap.put(key, writeMethod);
	}
	properties = new PropertyDescriptor[propertyVector.size()];
	for ( int j = 0; j < propertyVector.size(); j++ )
	    properties[j] = (PropertyDescriptor)(propertyVector.get(j));
	PropertiesMap.put(className, properties);
    }

    static public Method getReader(Class theClass, String theField)
    {
	Method reader = (Method)(ReaderMap.get(theClass.getName() + '.' + theField));
	if ( reader != null )
	    return reader;
	addClass(theClass);
	return (Method)(ReaderMap.get(theClass.getName() + '.' + theField));
    }

    static public Method getWriter(Class theClass, String theField)
    {
	Method writer = (Method)(WriterMap.get(theClass.getName() + '.' + theField));
	if ( writer != null )
	    return writer;
	addClass(theClass);
	return (Method)(WriterMap.get(theClass.getName() + '.' + theField));
    }

    static public PropertyDescriptor[] getProperties(Class theClass)
    {
	PropertyDescriptor[] properties = (PropertyDescriptor [])(PropertiesMap.get(theClass.getName()));
	if ( properties != null )
	    return properties;
	addClass(theClass);
	return (PropertyDescriptor[])(PropertiesMap.get(theClass.getName()));
    }
}
