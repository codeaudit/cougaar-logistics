/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

/* @generated Tue May 07 17:35:22 EDT 2002 from properties.def - DO NOT HAND EDIT */
/** Implementation of LowFidelityAssetUIDPG.
 *  @see LowFidelityAssetUIDPG
 *  @see NewLowFidelityAssetUIDPG
 **/

package org.cougaar.logistics.plugin.trans;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public final class LowFidelityAssetUIDPGImpl extends java.beans.SimpleBeanInfo
  implements NewLowFidelityAssetUIDPG, Cloneable
{
  public LowFidelityAssetUIDPGImpl() {
  }

  // Slots

  private String theUID;
  public String getUID(){ return theUID; }
  public void setUID(String UID) {
    if (UID!=null) UID=UID.intern();
    theUID=UID;
  }


  public LowFidelityAssetUIDPGImpl(LowFidelityAssetUIDPG original) {
    theUID = original.getUID();
  }

  public final boolean hasDataQuality() { return false; }

  private transient LowFidelityAssetUIDPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)
      _locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    return new LowFidelityAssetUIDPGImpl(LowFidelityAssetUIDPGImpl.this);
  }

  public PropertyGroup copy() {
    try {
      return (PropertyGroup) clone();
    } catch (CloneNotSupportedException cnse) { return null;}
  }

  public Class getPrimaryClass() {
    return primaryClass;
  }
  public String getAssetGetMethod() {
    return assetGetter;
  }
  public String getAssetSetMethod() {
    return assetSetter;
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    if (theUID!= null) theUID=theUID.intern();
  }

  private final static PropertyDescriptor properties[] = new PropertyDescriptor[1];
  static {
    try {
      properties[0]= new PropertyDescriptor("UID", LowFidelityAssetUIDPG.class, "getUID", null);
    } catch (Exception e) { System.err.println("Caught: "+e); e.printStackTrace(); }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements LowFidelityAssetUIDPG, Cloneable, LockedPG
  {
    private transient Object theKey = null;
    _Locked(Object key) { 
      if (this.theKey == null){  
        this.theKey = key; 
      } 
    }  

    /** public constructor for beaninfo - probably wont work**/
    public _Locked() {}

    public PropertyGroup lock() { return this; }
    public PropertyGroup lock(Object o) { return this; }

    public NewPropertyGroup unlock(Object key) throws IllegalAccessException {
       if( theKey.equals(key) )
         return LowFidelityAssetUIDPGImpl.this;
       else 
         throw new IllegalAccessException("unlock: mismatched internal and provided keys!");
    }

    public PropertyGroup copy() {
      try {
        return (PropertyGroup) clone();
      } catch (CloneNotSupportedException cnse) { return null;}
    }


    public Object clone() throws CloneNotSupportedException {
      return new LowFidelityAssetUIDPGImpl(LowFidelityAssetUIDPGImpl.this);
    }

    public String getUID() { return LowFidelityAssetUIDPGImpl.this.getUID(); }
  public final boolean hasDataQuality() { return false; }
    public Class getPrimaryClass() {
      return primaryClass;
    }
    public String getAssetGetMethod() {
      return assetGetter;
    }
    public String getAssetSetMethod() {
      return assetSetter;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
      return properties;
    }

    public Class getIntrospectionClass() {
      return LowFidelityAssetUIDPGImpl.class;
    }

  }

}
