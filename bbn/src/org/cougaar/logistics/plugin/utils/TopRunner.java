package org.cougaar.logistics.plugin.utils;

import java.text.SimpleDateFormat;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.io.StreamTokenizer;

public class TopRunner implements Runnable {
  int samples = 1;
  String machine;
  long period = 10000;
  long last = -1;
  SimpleDateFormat format = new SimpleDateFormat ("HH:mm:ss");
  Map timeToResult;
  boolean showIntermediate = false;

  public TopRunner (int samples, long period) {
    this.samples = samples;
    this.machine = machine;
  }

  public TopRunner (int samples, long period, String machine, Map timeToResult) {
    this.samples = samples;
    this.machine = machine;
    this.period  = period;
    this.timeToResult = timeToResult;
  }

  public void run () {
    while (samples-- > 0) {
      last = System.currentTimeMillis ();

      BufferedInputStream stream = null;
      try {
	String command ="ssh " + machine + " top -n 1 -b";
	// System.err.println ("doing command " + command);
	Process proc = Runtime.getRuntime().exec (command);
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
	String topTime = format.format (new Date(last));
	Map resultsAtTime = (Map) timeToResult.get (topTime); 

	if (resultsAtTime == null) {
	  timeToResult.put (topTime, resultsAtTime = new HashMap());
	  System.err.println ("timeToResult now " + timeToResult);
	}

	for (int i = 0; i < cpus; i++) {
	  String key = (machine+"-CPU_"+i);
	  String value = "" + percentage[i];
	  resultsAtTime.put (key, value);
	   System.err.println ("timeToResult put " + key + "->" + value);
	}

	if (showIntermediate)
	  System.out.println (format.format (new Date(last)) + "," + machine + "," + buf);
      }

      synchronized (this) {
	long timeTaken = System.currentTimeMillis () - last;
	if (timeTaken < period && samples > 0) {
	  try {
	    // System.err.println ("waiting " + (period-timeTaken));
	    wait (period - timeTaken); 
	  } 
	  catch (Exception e) {}
	}
      }
    }
  }

  protected void startThreads(Set machines) {
    Thread [] threads = new Thread [machines.size()];

    int i = 0;
    Map timeToResult = new HashMap();
    for (Iterator iter = machines.iterator(); iter.hasNext(); ) {
      String machine = (String) iter.next();
      // System.out.println ("machine " + machine);
      threads[i] = new Thread(new TopRunner (samples, period, machine, timeToResult));
      threads[i++].start();
    }

    i = 0;
    for (Iterator iter = machines.iterator(); iter.hasNext(); ) {
      try { 
	iter.next();
	threads[i++].join(); 
      } catch (Exception e) { System.err.println ("on join, got " +e); }
    }

    List keys = new ArrayList (timeToResult.keySet());
    Collections.sort (keys);

    System.out.print ("Time,");
    //    System.err.println ("keys were "+ keys);
    List sortedMachines = null;
    for (Iterator timeIter = keys.iterator(); timeIter.hasNext(); ) {
      Object topTime = timeIter.next();
      sortedMachines = new ArrayList (((Map) timeToResult.get(topTime)).keySet());
      Collections.sort (sortedMachines);
      
      for (Iterator machineCPUPairIter = sortedMachines.iterator(); machineCPUPairIter.hasNext(); ) {
	Object machineCPUPair = machineCPUPairIter.next();
	System.out.print (machineCPUPair + (machineCPUPairIter.hasNext() ? "," : "\n"));
      }
      break;
    }

    for (Iterator timeIter = keys.iterator(); timeIter.hasNext(); ) {
      Object topTime = timeIter.next();
      Map resultsAtTime = (Map) timeToResult.get(topTime);
      System.out.print (topTime +",");
      for (Iterator machineCPUPairIter = sortedMachines.iterator(); machineCPUPairIter.hasNext(); ) {
	Object machineCPUPair = machineCPUPairIter.next();
	Object value = resultsAtTime.get(machineCPUPair);
	// System.err.println ("value for "+ machineCPUPair + " was " + value);
	System.out.print ((value == null ? "-0" : value) + (machineCPUPairIter.hasNext() ? "," : "\n"));
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

    TopRunner runner = new TopRunner (samples, period);
    Set machines = new HashSet();
    for (int i = 2; i < args.length; i++)
      machines.add (args[i]);
    runner.startThreads(machines);
  }
}
