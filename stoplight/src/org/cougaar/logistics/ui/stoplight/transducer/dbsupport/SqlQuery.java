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

/**
 *  An SqlQuery is a special case of a ConjClause with exactly four elements,
 *  each of which is itself a ConjClause.  Additional methods provide direct
 *  access to each of the four subclauses.  The subclauses are based on the
 *  features normally found in an SQL statement, namely, a list of selected
 *  fields, a list of tables, an AND-conjoined condition clause, and a list of
 *  sorting keys.
 */
public class SqlQuery extends ConjClause {
  private static final String SELECT = "SELECT";
  private static final String FROM = "from";
  private static final String WHERE = "where";
  private static final String ORDER_BY = "order by";

  private ConjClause selects = null;
  private ConjClause tables = null;
  private ConjClause conditions = null;
  private ConjClause orderings = null;

  /**
   *  Create a new SqlQuery instance.  Initially, the SqlQuery is empty.
   */
  public SqlQuery () {
    super(null, SPACE, false);
    selects = new ConjClause(SELECT, COMMA, false);
    tables = new ConjClause(FROM, COMMA, false);
    conditions = new ConjClause(WHERE, " and ", false);
    orderings = new ConjClause(ORDER_BY, COMMA, false);

    add(selects);
    add(tables);
    add(conditions);
    add(orderings);
  }

  /**
   *  Add a selected field to this query
   *  @param s an object whose String representation is the field name
   */
  public void addSelection (Object s) {
    selects.add(s);
  }

  /**
   *  Add a table to this SqlQuery
   *  @param s an object whose String representation is the table name
   */
  public void addTable (Object s) {
    tables.add(s);
  }

  /**
   *  Add a condition to this SqlQuery.  This may, for instance, be another
   *  ConjClause.
   *  @param s an object whose toString method gives an SQL logical expression
   */
  public void addCondition (Object s) {
    conditions.add(s);
  }

  /**
   *  Add a sorting key to this query.
   *  @param s an object whose String representation is a sorting key
   */
  public void addOrdering (Object s) {
    orderings.add(s);
  }

  /**
   *  Clear this SqlQuery in preparation for making a new query
   */
  public void clear () {
    selects.clear();
    tables.clear();
    conditions.clear();
    orderings.clear();
  }
}