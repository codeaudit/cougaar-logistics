/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.client;

import java.util.Iterator;
import java.util.Vector;
import javax.swing.event.TableModelEvent;

import org.cougaar.lib.uiframework.ui.models.TransformableTableModel;

import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.query.AggregationResultSet;

public class TransResultSetTableModel extends TransformableTableModel {

  public TransResultSetTableModel() {}

  public void setResultSet(AggregationResultSet resultSet)
  {
    dataRows.clear();
    boolean firstAtom = true;
    for (Iterator i = resultSet.getAllAtoms(); i.hasNext();)
    {
      ResultSetDataAtom da = (ResultSetDataAtom)i.next();
      if (firstAtom)
      {
        // create header row
        Vector columnHeaderRow = new Vector();
        for (Iterator ids = da.getIdentifierNames(); ids.hasNext();)
        {
          columnHeaderRow.addElement((String)ids.next());
        }
        for (Iterator values = da.getValueNames(); values.hasNext();)
        {
          columnHeaderRow.addElement((String)values.next());
        }
        dataRows.addElement(columnHeaderRow);
        firstAtom = false;
      }

      // create data row
      Vector dataRow = new Vector();
      for (Iterator ids = da.getIdentifierNames(); ids.hasNext();)
      {
        dataRow.addElement(da.getIdentifier(ids.next()));
      }
      for (Iterator values = da.getValueNames(); values.hasNext();)
      {
        String data = (String)da.getValue(values.next());
        try {
          dataRow.addElement(Float.valueOf(data));
        } catch(Exception e) {
          dataRow.addElement(data);
        }
      }
      dataRows.addElement(dataRow);
    }

    fireTableChangedEvent(
      new TableModelEvent(this, TableModelEvent.HEADER_ROW));
  }
}