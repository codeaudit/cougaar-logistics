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
/** Primary client interface for LowFidelityAssetUIDPG.
 * Carries unique identifier for a low fidelity asset, so it can be associated with other low-fi assets that are part of the same glob
 *  @see NewLowFidelityAssetUIDPG
 *  @see LowFidelityAssetUIDPGImpl
 **/

package org.cougaar.logistics.plugin.trans;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



public interface LowFidelityAssetUIDPG extends PropertyGroup {
  /** the UID of the original asset **/
  String getUID();

  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newLowFidelityAssetUIDPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "org.cougaar.logistics.plugin.trans.NewLowFidelityAssetUIDPG";
  /** the factory class **/
  Class factoryClass = org.cougaar.logistics.plugin.trans.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = org.cougaar.logistics.plugin.trans.LowFidelityAssetUIDPG.class;
  String assetSetter = "setLowFidelityAssetUIDPG";
  String assetGetter = "getLowFidelityAssetUIDPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  LowFidelityAssetUIDPG nullPG = new Null_LowFidelityAssetUIDPG();

/** Null_PG implementation for LowFidelityAssetUIDPG **/
final class Null_LowFidelityAssetUIDPG
  implements LowFidelityAssetUIDPG, Null_PG
{
  public String getUID() { throw new UndefinedValueException(); }
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
  public NewPropertyGroup unlock(Object key) { return null; }
  public PropertyGroup lock(Object key) { return null; }
  public PropertyGroup lock() { return null; }
  public PropertyGroup copy() { return null; }
  public Class getPrimaryClass(){return primaryClass;}
  public String getAssetGetMethod() {return assetGetter;}
  public String getAssetSetMethod() {return assetSetter;}
  public Class getIntrospectionClass() {
    return LowFidelityAssetUIDPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
}

/** Future PG implementation for LowFidelityAssetUIDPG **/
final class Future
  implements LowFidelityAssetUIDPG, Future_PG
{
  public String getUID() {
    waitForFinalize();
    return _real.getUID();
  }
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
  public NewPropertyGroup unlock(Object key) { return null; }
  public PropertyGroup lock(Object key) { return null; }
  public PropertyGroup lock() { return null; }
  public PropertyGroup copy() { return null; }
  public Class getPrimaryClass(){return primaryClass;}
  public String getAssetGetMethod() {return assetGetter;}
  public String getAssetSetMethod() {return assetSetter;}
  public Class getIntrospectionClass() {
    return LowFidelityAssetUIDPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }

  // Finalization support
  private LowFidelityAssetUIDPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof LowFidelityAssetUIDPG) {
      _real=(LowFidelityAssetUIDPG) real;
      notifyAll();
    } else {
      throw new IllegalArgumentException("Finalization with wrong class: "+real);
    }
  }
  private synchronized void waitForFinalize() {
    while (_real == null) {
      try {
        wait();
      } catch (InterruptedException _ie) {}
    }
  }
}
}
