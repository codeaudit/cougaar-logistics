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

package org.cougaar.logistics.ui.stoplight.transducer;

import org.cougaar.logistics.ui.stoplight.transducer.dbsupport.*;
import org.cougaar.logistics.ui.stoplight.transducer.elements.*;

import java.util.*;

/**
 *  <p>
 *  SqlTransducer is the base class for varying implementations capable of
 *  mapping the information contained in a Structure into a DB schema of some
 *  kind.  By supplying concrete implementations of the Assembly and
 *  Disassembly interfaces, subclasses can control the format and location of
 *  the Structures as represented in the database.
 *  </p><p>
 *  Most of the nitty-gritty of connecting to the database is handled by the
 *  superclass.  The API is, of course, available for the convenience of
 *  implementation in the subclasses.
 *  </p>
 */
public abstract class SqlTransducer extends DbConnect {
  /**
   *  Create a new SqlTransducer instance.  Since this is an abstract class,
   *  no public constructors are necessary.
   */
  protected SqlTransducer () {
  }

  /**
   *  Create a new SqlTransducer instance that uses the named DB driver.
   *  @param driver the name of the DB driver class
   */
  protected SqlTransducer (String driver) {
    super(driver);
  }

  /**
   *  Create an Assembly to build a Structure out of data found in the database.
   *  The values provided are used to select the required rows from the table.
   *  If no index keys are specified, it is presumed that the Structure in
   *  question is the sole occupant of the table.  Specific transducers must
   *  supply instances of their own specific Assembly implementation classes.
   *
   *  @param id the array of index values pointing to the Structure data
   */
  protected abstract Assembly getAssembly (String[] id);

  /**
   *  Create a Dissassembly capable of analyzing the contents of the given
   *  Structure and storing it in the database at the specified indices.  If no
   *  indices are given, then the Structure will replace everything currently
   *  in the target table.  Specific transducers must supply instances of their
   *  own specific Disassembly implementation classes.
   *
   *  @param id the array of index values
   *  @param s the Structure to be stored by the Disassembly
   */
  protected abstract Disassembly getDisassembly (String[] id, Structure s);

  /**
   *  Remove the Structure data (if any) for the given set of index keys.
   *  For different implementations of the transducer, the exact nature of this
   *  operation may vary.  If the ID key array is null or empty, then all rows
   *  of the target DB table will be removed.
   *
   *  @param id the array of index values.
   */
  protected abstract void clearSpaceFor (String[] id);

  /**
   *  Save a Structure in the database at the specified location, replacing
   *  previously stored information, if necessary.
   *
   *  @param id the location at which the data will be stored
   *  @param s the Structure to be stored
   */
  public void writeToDb (String[] id, Structure s) {
    clearSpaceFor(id);
    getDisassembly(id, s).writeToDb();
  }

  /**
   *  Read a Structure from the specified location in the database.
   *
   *  @param id the location at which the data (if any) is stored.
   *  @return the Structure
   */
  public Structure readFromDb (String[] id) {
    return getAssembly(id).readFromDb();
  }

  /**
   *  Delete an existing structure from the database.
   *
   *  @param id the location to be cleared of data
   */
  public void removeFromDb (String[] id) {
    clearSpaceFor(id);
  }
}