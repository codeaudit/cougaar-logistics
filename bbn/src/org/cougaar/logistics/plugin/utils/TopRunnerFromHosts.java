package org.cougaar.logistics.plugin.utils;

import java.text.SimpleDateFormat;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.io.StreamTokenizer;

/**
 * Parses hosts file - only will ask hosts that have an acme service
 *
 * Generates table like :
 *
 * Time,    sv023-CPU_0,sv023-CPU_1,sv023-CPU_2
 * 18:27:09,        0.0,        1.0,        0.0
 * 18:27:14,         -0,         -0,         -0
 * 
 * (-0 indicates no top data at that time at that host)
 * Numbers represent percentage CPU usage.
 *
 * Arguments : number of samples, seconds between samples, and host file to get hosts from
 *
 * Typical Usage : 
 *  java -classpath albbn.jar org.cougaar.logistics.plugin.utils.TopRunnerFromHosts 2 5 $CIP/operator/s1-hosts.xml > results.csv
 *
 * (Here it asks for 2 samples, 5 seconds apart, reading from the s1-hosts.xml file.)
 */ 
public class TopRunnerFromHosts extends TopRunner {
  public TopRunnerFromHosts (int samples, long period, String machine, Map timeToResult) {
    super (samples, period, machine, timeToResult);
  }

  protected Set getHosts (File hostsFile) {
    Set hostSet = new HashSet();

    try {
      Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(hostsFile)));
      StreamTokenizer st = new StreamTokenizer(r);
      String host = "";
      boolean gotName = false;
      boolean gotService = false;
      boolean hostIsNext = true;
      while (st.nextToken () != StreamTokenizer.TT_EOF) {
	String token = st.sval;
	if (token != null) {
	  //System.err.println ("token " + token);
	  if (token.startsWith ("name")) {
	    gotName = true;
	    gotService = false;
	    hostIsNext = true;
	    // System.err.println ("name " + gotName + " service " + gotService);
	  }
	  else if (gotName && hostIsNext) {
	    host = token;
	    hostIsNext = false;
	    // System.err.println ("host is " + host);
	  }
	  if (token.startsWith ("service")) {
	    gotService = true;
	    // System.err.println ("name " + gotName + " service " + gotService);
	  }
	  if (gotService && token.equals("acme")) {
	    hostSet.add (host);
	    gotName = false;
	    gotService = false;
	    // System.err.println ("name " + gotName + " service " + gotService);
	  }
	}
      }
      System.err.println ("machines " + hostSet);
    } catch (Exception e) {
      System.err.println ("exception " + e);
      e.printStackTrace ();
    }

    return hostSet;
  }

  public static void main (String [] args) {
    if (args.length < 3) {
      System.err.println ("Usage : TopRunnerFromHosts num_samples period (sec) hosts.xml");
      return;
    }

    int samples = 1;
    long period = 10000;
    try { samples = Integer.parseInt(args[0]); } catch (Exception e) {}
    try { period  = ((long)Integer.parseInt  (args[1]))*1000l; } catch (Exception e) {}

    File hosts = new File (args[2]);
    if (!hosts.exists()) {
      System.err.println ("can't find file " + hosts);
    } else {
      TopRunnerFromHosts runner = new TopRunnerFromHosts (samples, period, "", null);
      // parse the xml file and give the result to startThreads
      runner.startThreads(runner.getHosts(hosts));
    }
  }
}
