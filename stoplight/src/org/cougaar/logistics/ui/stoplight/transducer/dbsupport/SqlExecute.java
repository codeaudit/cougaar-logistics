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

package org.cougaar.logistics.ui.stoplight.transducer.dbsupport;

import java.util.Date;
import java.text.*;

/**
 *  SqlExecute is a base class containing the commonalities of the SqlInsert
 *  and SqlUpdate classes.  It has methods allowing various types of values to
 *  be associated with field names and inserted into the respective statements.
 *  The implementation of the addLiteral method depends on the nature of the
 *  statement being formed, and thus is an abstract method.  Other insertion
 *  methods rely on its implementation to do their work correctly.
 */
public abstract class SqlExecute extends ConjClause {
  /**
   *  Used to represent the boolean TRUE value
   */
  protected static final String YES = "'Y'";

  /**
   *  Used to represent the boolean FALSE value
   */
  protected static final String NO = "'N'";

  /**
   *  A formatter that expresses Dates with accuracy to the nearest second.  A
   *  corresponding oracle date format is able to interpret these expressions.
   */
  protected static final SimpleDateFormat hiFiJavaDateFormat =
    new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

  /**
   *  An oracle date format that recognizes date expressions accurate to the
   *  nearest second.  A corresponding Java Date formatter produces date
   *  expressions in the appropriate format.
   */
  protected static final String hiFiOracleDateFormat = "'MM/DD/YYYY HH24:MI:SS'";

  /**
   *  A formatter that expresses Dates with accuracy to the nearest day.  A
   *  corresponding oracle date format is able to interpret these expressions.
   */
  protected static SimpleDateFormat loFiJavaDateFormat =
    new SimpleDateFormat( "dd'-'MMM'-'yyyy" );

  /**
   *  An oracle date format that interprets date expressions accurate to the
   *  nearest day.  A corresponding Java Date formatter produces date
   *  expressions in the appropriate format.
   */
  protected static final String loFiOracleDateFormat = "'DD-MON-YYYY'";

  /**
   *  Static method formatDate is a utility function that generates a formatted
   *  date expression from a Date.  It also handles the unpleasantries, such as
   *  nulls and synchronization on the formatter.
   *  @param d a Date to be formatted
   *  @param df the formatter that produces the desired date expressions
   */
  protected static String formatDate (Date d, DateFormat df) {
    if (d == null)
      return null;
    synchronized (df) {
      return df.format(d);
    }
  }

  /**
   *  Create a new executable SQL statement.  The caller must supply the basic
   *  elements of a ConjClause construction, namely a header, a conjunction,
   *  and a boolean indicating whether parentheses are to be included.
   *  @param head the header String
   *  @param conj the conjunction String
   *  @param parens true if and only if the expression should be in parentheses
   */
  protected SqlExecute (String head, String conj, boolean parens) {
    super(head, conj, parens);
  }

  /**
   *  Place a field name and an exact String value into this SQL statement
   *  @param field the name of the field
   *  @param val the String value
   */
  public abstract void addLiteral (String field, String val);

  /**
   *  Associate a numeric value (expressed as a String) with the given column.
   *  Note that if the database is expecting a number and the provided value is
   *  not a number, it may get upset.
   *  @param field the name of the column
   *  @param val a String representation of the numeric value
   */
  public void addNumber (String field, String val) {
    addLiteral(field, val);
  }

  /**
   *  Associate a numeric value (encased in a Number instance) with the given
   *  column.
   *  @param field the name of the column
   *  @param val the Number instance holding the numeric value
   */
  public void addNumber (String field, Number val) {
    addLiteral(field, val.toString());
  }

  /**
   *  Associate a numeric value (in this case a floating-point number) with the
   *  given column.
   *  @param field the name of the column
   *  @param val the numeric value expressed as a double (or float) type
   */
  public void addNumber (String field, double val) {
    addLiteral(field, String.valueOf(val));
  }

  /**
   *  Associate a numeric value (in this case an integer) with the given column.
   *  @param field the name of the column
   *  @param val the numeric value expressed as a long (or int, short, etc.)
   */
  public void addNumber (String field, long val) {
    addLiteral(field, String.valueOf(val));
  }

  /**
   *  Add a String value to the given column.  The appropriate quotation marks
   *  are automatically supplied.
   *  @param field the name of the column
   *  @param val the value being inserted
   */
  public void addString (String field, String val) {
    addLiteral(field, quote(val));
  }

  /**
   *  Associate a boolean value with the given column.  In this case, the
   *  boolean TRUE or FALSE is encoded as character "Y" or "N", respectively.
   *  @param field the name of the affected column
   *  @param val the boolean value being inserted
   */
  public void addYorN (String field, boolean val) {
    addLiteral(field, (val ? YES : NO));
  }

  /**
   *  Add a "high-fidelity" date value to the statement.  Dates so encoded are
   *  accurate to the nearest second.
   *  @param field the name of the column associated with the date
   *  @param d the Date value being inserted
   */
  public void addHiFiDate (String field, Date d) {
    StringBuffer buf = new StringBuffer("TO_DATE(");
    buf.append(quote(formatDate(d, hiFiJavaDateFormat)));
    buf.append(COMMA);
    buf.append(hiFiOracleDateFormat);
    buf.append(")");
    addLiteral(field, buf.toString());
  }

  /**
   *  Add a "low-fidelity" date value to the statement.  Dates so encoded are
   *  accurate to the nearest day.
   *  @param field the name of the column associates with the date
   *  @param d the Date value being inserted
   */
  public void addLoFiDate (String field, Date d) {
    StringBuffer buf = new StringBuffer("TO_DATE(");
    buf.append(quote(formatDate(d, loFiJavaDateFormat)));
    buf.append(COMMA);
    buf.append(loFiOracleDateFormat);
    buf.append(")");
    addLiteral(field, buf.toString());
  }

  /**
   *  Associate a "null" value with a field in this statement.
   *  @param field the name of the null field
   */
  public void addNull (String field) {
    addLiteral(field, NULL_STRING);
  }

  /**
   *  Associate "null" values with a series of fields as supplied by the caller
   *  @param field an array of names of fields slated to have "null" values
   */
  public void addNulls (String[] field) {
    if (field == null)
      return;

    for (int i = 0; i < field.length; i++)
      addNull(field[i]);
  }
}