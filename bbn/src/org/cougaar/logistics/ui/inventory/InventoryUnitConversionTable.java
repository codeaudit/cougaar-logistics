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

import org.cougaar.logistics.ui.inventory.data.InventoryData;
import org.cougaar.logistics.ui.inventory.data.InventoryPreferenceData;
import org.cougaar.logistics.plugin.inventory.LogisticsInventoryFormatter;

import java.util.Hashtable;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class InventoryUnitConversionTable {

  Hashtable converterTable;

  public final static int ITEM_INDEX = 0;
  public final static int UNIT_FROM_INDEX = 1;
  public final static int UNIT_TO_INDEX = 2;
  public final static int FACTOR_INDEX = 3;

  public InventoryUnitConversionTable() {
    converterTable = new Hashtable();

    addEntry(LogisticsInventoryFormatter.WATER_ITEM_ID,
             InventoryPreferenceData.CASES_LABEL,
             InventoryPreferenceData.BOTTLES_LABEL,12);

    addEntry(LogisticsInventoryFormatter.WATER_ITEM_ID,
             InventoryPreferenceData.CASES_LABEL,
             InventoryPreferenceData.LITERS_LABEL,12);

  }

  /**

  private void add(String item, InventoryUnitConverter converter) {
    Hashtable unitTable = (Hashtable) converterTable.get(item);
    if(unitTable == null) {
      unitTable = new Hashtable();
      converterTable.put(item,unitTable);
    }
    unitTable.put(converter.getBaseUnit(), unitTable);
  }

  **/

  public void parseAndAddFile(String fileStr) {
    File file = new File(fileStr);
    if(file.exists()) {
      parse(file);
    }
  }

  public void parse(File file) {
          try {
        BufferedReader br = new BufferedReader(new FileReader(file));

        String nextLine = br.readLine();
        while (nextLine != null) {
          parseAndAddEntry(nextLine + "\n");
          nextLine = br.readLine();
        }
        br.close();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }

  }

  protected void parseAndAddEntry(String line) {
    String lines[] = line.split(",");
    String item = lines[ITEM_INDEX];
    String fromUnit = lines[UNIT_FROM_INDEX];
    String toUnit = lines[UNIT_TO_INDEX];
    double factor = Double.parseDouble(lines[FACTOR_INDEX]);

    addEntry(item,fromUnit,toUnit,factor);
  }


  private void addEntry(String item,
                        String fromUnit,
                        String toUnit,
                        double factor) {
    Hashtable unitTable = (Hashtable) converterTable.get(item);
    if(unitTable == null) {
      unitTable = new Hashtable();
      converterTable.put(item,unitTable);
    }
    InventoryUnitConverter converter = (InventoryUnitConverter) unitTable.get(fromUnit);
    if(converter == null) {
      converter = new InventoryUnitConverter(fromUnit);
      unitTable.put(fromUnit,converter);
    }
    converter.addFactor(toUnit,factor);
  }

  private InventoryUnitConverter getConverterForData(InventoryData data) {
      return getConverter(data.getItem(),data.getUnit());
  }

  private InventoryUnitConverter getConverter(String item, String unit) {
      Hashtable unitTable = (Hashtable) converterTable.get(item);
      InventoryUnitConverter converter = null;
      if(unitTable != null) {
        converter = (InventoryUnitConverter) unitTable.get(unit);
      }
      if(converter == null) {
       /** TODO: MWD This is a hack until we are assured the files that are
        * being examined are produced by the current LogisticsInventoryFormatter.
        *  and have the proper units (most notably water) in the xml contents.
         * In due time Jan 2004, take out the next if statement and only
         * return a newly built converter.
        **/
        if(unitTable != null && unitTable.size() == 1) {
          converter = (InventoryUnitConverter) unitTable.elements().nextElement();
        }
        else {
	        converter = new InventoryUnitConverter(unit);
        }
      }
      return converter;
  }

    public InventoryUnitConversion getConversionForData(InventoryData data, InventoryPreferenceData prefs) {
	InventoryUnitConverter converter = getConverterForData(data);
	return converter.getConversionForUnit(prefs.getPreferredUnit(data));
    }

}

