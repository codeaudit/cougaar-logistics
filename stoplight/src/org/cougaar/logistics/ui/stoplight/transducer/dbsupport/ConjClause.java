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

import java.util.*;

/**
 *  This class keeps track of a collection of inputs and is capable of
 *  converting them to a structured output String.  The main intent, as the
 *  name implies, is to create a compound clause by joining clauses together
 *  with a repeated conjunction.  In fact, the conjunction can be any String
 *  and must supply its own spacing from adjacent arguments.  Arguments that
 *  evaluate to the empty String are ignored, and if no non-empty Strings are
 *  found, the entire clause evaluates to the empty String.
 *    Parentheses and/or a header String may be supplied, if desired, but are
 *  nevertheless omitted in case all the arguments are blank.
 */
public class ConjClause {
  protected static final String NULL_STRING = "null";
  protected static final String QUOTE = "'";

  protected static final String SPACE = " ";
  protected static final String COMMA = ", ";

  private Vector conditions = new Vector();
  private String header = null;
  private String conjunction = null;
  private boolean parens;

  /**
   *  Create a new ConjClause instance.  A header and a conjunction are
   *  supplied as well as a flag indicating whether the whole expression should
   *  be parenthesized.
   *  @param head the header String
   *  @param conj the conjunction inserted between adjacent entries
   *  @param p a flag which is true if and only if this clause should be in
   *    parentheses
   */
  public ConjClause (String head, String conj, boolean p) {
    header = head;
    conjunction = conj;
    if (conjunction == null)
      conjunction = " ";
    parens = p;
  }

  /**
   *  Specify whether or not to use parentheses in the output
   *  @param p the new paranetheses flag
   */
  public void setParentheses (boolean p) {
    parens = p;
  }

  /**
   *  Specify the header to use in the output.  A space is automatically
   *  inserted between the header and the body of the clause.
   *  @param h the new header String
   */
  public void setHeader (String h) {
    header = h;
  }

  /**
   *  Add a new element to the clause
   *  @param c the new element
   */
  public void add (Object c) {
    conditions.add(c);
  }

  /**
   *  Remove all of the elements of this clause (presumably to generate a new
   *  clause).
   */
  public void clear () {
    conditions.clear();
  }

  /**
   *  Check to see if this clause is empty
   *  @return true if and only if this clause has no non-trivial elements
   */
  public boolean isEmpty () {
    for (Enumeration e = conditions.elements(); e.hasMoreElements(); )
      if (e.nextElement().toString().length() > 0)
        return false;
    return true;
  }

  /**
   *  Produce a String output from the elements present in this clause
   *  @return the product String
   */
  public String toString () {
    if (isEmpty())
      return "";

    StringBuffer buf = new StringBuffer();
    if (parens)
      buf.append("(");
    if (header != null) {
      buf.append(header);
      buf.append(" ");
    }

    Enumeration e = conditions.elements();
    String s = null;
    do {
      s = e.nextElement().toString();
    } while (s.length() == 0);

    buf.append(s);
    while (e.hasMoreElements()) {
      s = e.nextElement().toString();
      if (s.length() > 0) {
        buf.append(conjunction);
        buf.append(s);
      }
    }

    if (parens)
      buf.append(")");

    return buf.toString();
  }

  /**
   *  This utility function places a String in quotation marks, automatically
   *  escaping in embedded quotation marks.
   *  @param s the initial String value
   *  @return a quoted version of s
   */
  public static String quote (String s) {
    if (s == null)
      return NULL_STRING;

    StringTokenizer tok = new StringTokenizer(s, QUOTE, true);
    StringBuffer buf = new StringBuffer(QUOTE);
    while (tok.hasMoreTokens()) {
      String t = tok.nextToken();
      if (QUOTE.equals(t)) {
        buf.append(QUOTE);
        buf.append(QUOTE);
      }
      else {
        buf.append(t);
      }
    }
    buf.append(QUOTE);

    return buf.toString();
  }
}