/*  */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

*/


package org.cougaar.mlm.ui.newtpfdd.gui.view;


import java.util.Vector;

import javax.swing.SwingUtilities;

import org.cougaar.mlm.ui.newtpfdd.util.ExceptionTools;
import org.cougaar.mlm.ui.newtpfdd.util.OutputHandler;

/**
   Textually extended (need to fully override constructor to pass in parent
   parameter; can't truly extend).  Not elegant but certain that this will
   work w/o race conditions.
 */
public abstract class TPFDDSwingWorker
{
    private Thread thread;

    /** 
     * Compute the value to be returned by the <code>get</code> method. 
     */
    public abstract void construct();

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * after the <code>construct</code> method has returned.
     */
    public abstract void finished();
    
    /**
     * A new method that interrupts the worker thread.  Call this method
     * to force the worker to abort what it's doing.
     */
    public void interrupt()
    {
	Thread t = thread;
	if (t != null) {
	    t.interrupt();
	}
	thread = null;
    }

  protected long start;
  protected long end;
  public long getDuration () { return end-start; }

  /**
     * Start a thread that will call the <code>construct</code> method
     * and then exit.
     */
  public TPFDDSwingWorker () {
    start = System.currentTimeMillis ();
    final Runnable doFinished = new Runnable() {
	public void run() { 
	  finished(); 
	  end = System.currentTimeMillis ();
	}
      };

    Runnable doConstruct = new Runnable() { 
	public void run() {
	  synchronized(TPFDDSwingWorker.this) {
	    construct();
	    thread = null;
	  }
	  SwingUtilities.invokeLater(doFinished);
	}
      };

    thread = new Thread(doConstruct);

  }

    public void startWorker()
    {
	try {
	    thread.start();
	}
	catch ( Exception e ) {
	    OutputHandler.out(ExceptionTools.toString("IP:PT:run", e));
	}
	catch ( Error e ) {
	    OutputHandler.out(ExceptionTools.toString("IP:PT:run", e));
	}
    }

}
