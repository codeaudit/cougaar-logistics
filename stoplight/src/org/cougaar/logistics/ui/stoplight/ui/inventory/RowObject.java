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
package org.cougaar.logistics.ui.stoplight.ui.inventory;
public class RowObject
{
  int numberOfRows;
  int numberOfColumns;
  Object[] columnValues;
  public RowObject(int rows, int cols, Object[] colValues)
  {
    numberOfRows = rows;
    numberOfColumns = cols;
    columnValues = colValues;
    //System.out.println("row object " + ((Double)columnValues[0]).doubleValue());
  }
  public int compareTo(RowObject b)
  {
    if(((Double)columnValues[0]).longValue() == ((Double)b.columnValues[0]).doubleValue())
      return 0;
    else if(((Double)columnValues[0]).doubleValue() < ((Double)b.columnValues[0]).doubleValue())
      return -1;
    else if(((Double)columnValues[0]).doubleValue() > ((Double)b.columnValues[0]).doubleValue())
      return 1;
    return 0;
  }
 public  void mergeUp(Object[] b)
 {
  for(int i = 1; i < numberOfColumns; i++) // start at column 2
  {
    /*
    Long colVal = ((Long)columnValues[i]).longValue();
    long appendData = ((Long)b[i]).longValue();
    colVal += appendData;
    columnValues[i] = new Long(colVal);
    */
    Class aCol = columnValues[i].getClass();
    Class bCol = b[i].getClass();
    if(aCol.getName().equals("java.lang.String"))
      if(bCol.getName().equals("java.lang.String"))
        continue;
      else
        columnValues[i] = b[i];
    else
      continue;
  }
 }
}
