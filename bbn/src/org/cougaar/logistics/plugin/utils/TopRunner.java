package org.cougaar.logistics.plugin.utils;

import java.text.SimpleDateFormat;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.io.StreamTokenizer;

public class TopRunner implements Runnable {
  int samples = 1;
  String machine;
  long period = 10000;
  long last = -1;
  SimpleDateFormat format = new SimpleDateFormat ("HH:mm:ss");

  public TopRunner (int samples, long period, String machine) {
    this.samples = samples;
    this.machine = machine;
    this.period  = period;
  }

  public void run () {
    while (samples-- > 0) {
      last = System.currentTimeMillis ();

      BufferedInputStream stream = null;
      try {
	Process proc = Runtime.getRuntime().exec ("ssh " + machine + " top -n 1 -b");
	stream = new BufferedInputStream (proc.getInputStream ());
      } catch (Exception e) {
	System.err.println ("exception " + e);
      }

      byte [] bytes = new byte [1024];
      int length = 0;

      String [] percentage = new String [10];
      int cpus = 0;

      try {
	Reader r = new BufferedReader(new InputStreamReader(stream));
	StreamTokenizer st = new StreamTokenizer(r);
	while (st.nextToken () != StreamTokenizer.TT_EOF) {
	  // System.out.println ("token " + st.sval + " num token " +st.nval);
	  if (st.sval != null) {
	    if (st.sval.startsWith ("states")) {
	      st.nextToken ();
	      st.nextToken ();
	      //System.out.println ("after states token " + st.sval + " num " +st.nval);
	      percentage [cpus++] = "" + st.nval;
	    }
	    else if (st.sval.startsWith ("Mem")) {
	      break;
	    }
	  }
	}

      } catch (Exception e) {
	System.err.println ("got read " + e);
      }

      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < cpus; i++) {
	buf.append (percentage[i]);
	if (i < cpus-1)
	  buf.append (",");
      }

      synchronized(System.out) {
	System.out.println (format.format (new Date(last)) + "," + machine + "," + buf);
      }

      synchronized (this) {
	long timeTaken = System.currentTimeMillis () - last;
	if (timeTaken < period) {
	  try {
	    wait (period - timeTaken); 
	  } 
	  catch (Exception e) {}
	}
      }
    }
  }

  public static void main (String [] args) {
    if (args.length < 3) {
      System.err.println ("Usage : TopRunner num_samples period (sec) machine1 machine2 ...");
      return;
    }
    int samples = 1;
    long period = 10000;
    try { samples = Integer.parseInt(args[0]); } catch (Exception e) {}
    try { period  = ((long)Integer.parseInt  (args[1]))*1000l; } catch (Exception e) {}

    Thread [] threads = new Thread [args.length-2];

    for (int i = 2; i < args.length; i++) {
      String machine = args[i];
      // System.out.println ("machine " + machine);
      threads[i-2] = new Thread(new TopRunner (samples, period, machine));
      threads[i-2].start();
    }

    for (int i = 0; i < args.length-2; i++) {
      try { threads[i].join(); } catch (Exception e) { System.err.println ("on join, got " +e); }
    }
  }
}
