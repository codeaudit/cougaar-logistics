/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.inventory;

import java.util.Hashtable;


public class InventoryUnitConverter  {
  Hashtable factorTable;
  private String baseUnit;

  public InventoryUnitConverter(String defaultUnit) {
    factorTable = new Hashtable();
    baseUnit = defaultUnit;
    addFactor(defaultUnit,(double)1.0);
  }

  public void addFactor(String convertingToUnit, double factor) {
      InventoryUnitConversion convert = new InventoryUnitConversion(baseUnit,convertingToUnit,factor);
      addFactor(convertingToUnit,convert);
  }

  public void addFactor(String convertingToUnit, InventoryUnitConversion convert) {
      factorTable.put(convertingToUnit,convert);
  }

  public String getBaseUnit() { return baseUnit; }

  protected InventoryUnitConversion getConversionForUnit(String unit) {
      InventoryUnitConversion factorObj = (InventoryUnitConversion) factorTable.get(unit);
      if(factorObj == null) {
	  factorObj = new InventoryUnitConversion(baseUnit,baseUnit,1);
      }
      return factorObj;
  }


}

