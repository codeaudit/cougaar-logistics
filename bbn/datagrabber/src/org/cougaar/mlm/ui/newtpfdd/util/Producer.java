/* $Header: /opt/rep/cougaar/logistics/bbn/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/util/Attic/Producer.java,v 1.1 2002-05-14 20:41:08 gvidaver Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Daniel Bromberg
*/

/**
 * Basic model for any entity that wants to produce items.  Current
 * examples include LogPlanProducers which grab data from clusters, and
 * the PlanElementProvider which produces useful objects for the UI by
 * processing a complex set of task-related structures into a single object.
 * Member functions should be self-explanatory.
 */

package org.cougaar.mlm.ui.newtpfdd.util;


public interface Producer
{
    void addNotify(Object element);

    void changeNotify(Object element);

    void deleteNotify(Object element);

    void addNotify(Object[] elements);

    void changeNotify(Object[] elements);

    void deleteNotify(Object[] elements);
    
    void addConsumer(Consumer consumer);

    void deleteConsumer(Consumer consumer);

    void firingComplete();
}
