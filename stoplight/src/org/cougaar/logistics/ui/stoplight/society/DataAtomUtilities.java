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
package org.cougaar.logistics.ui.stoplight.society;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.cougaar.lib.aggagent.query.ResultSetDataAtom;

public class DataAtomUtilities
{
  public static Collection
    extractAtomsWithIncludedId(Collection atoms, Object idName,
                               Collection idValues)
  {
    Collection extractedAtoms = new LinkedList();
    for (Iterator i = atoms.iterator(); i.hasNext();)
    {
      ResultSetDataAtom da = (ResultSetDataAtom)i.next();
      if (idValues.contains(da.getIdentifier(idName)))
      {
        extractedAtoms.add(da);
        i.remove();
      }
    }
    return extractedAtoms;
  }

  public static Collection
    extractAtomsWithId(Collection atoms, Object idName, Object idValue)
  {
    Collection extractedAtoms = new LinkedList();
    for (Iterator i = atoms.iterator(); i.hasNext();)
    {
      ResultSetDataAtom da = (ResultSetDataAtom)i.next();
      if (idValue.equals(da.getIdentifier(idName)))
      {
        extractedAtoms.add(da);
        i.remove();
      }
    }
    return extractedAtoms;
  }

  public static Collection
    expandAllAtoms(Collection sourceAtoms, Expander expander)
  {
    Collection newAtoms = new LinkedList();

    for (Iterator i = sourceAtoms.iterator(); i.hasNext();)
    {
      ResultSetDataAtom atom = (ResultSetDataAtom)i.next();
      newAtoms.addAll(expander.expand(atom));
    }

    return newAtoms;
  }

  /**
    * Used to model how to expand one atom into many atoms
    */
  public static interface Expander
  {
    /**
      * expands one atom into many atoms
      *
      * @param atom the atom to be expanded
      * @return a collection of atoms to replace this atom
      */
    public Collection expand(ResultSetDataAtom atom);
  }

  /**
   * Add the same identifier to all data atoms in passed in Collection
   */
  public static Collection
    addIdentifier(Collection dataAtoms, Object key, Object value)
  {
    for (Iterator i = dataAtoms.iterator(); i.hasNext();)
    {
      ((ResultSetDataAtom)i.next()).addIdentifier(key, value);
    }

    return dataAtoms;
  }
}