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