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