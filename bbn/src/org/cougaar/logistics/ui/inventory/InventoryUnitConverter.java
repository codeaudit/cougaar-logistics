/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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

