package org.cougaar.mlm.ui.grabber.config;

import java.io.*;
import java.net.*;
import java.util.*;
import org.cougaar.util.ConfigFinder;

/**
 * Largely stolen from Parameters -- why not just use it?
 *
 * Statics are bad, and I don't need all the other stuff in that class
 * And I have the fantasy of not being dependent on the core jar.
 */
public class CougaarRCParams {

  protected static boolean warnedBefore = false;

  public CougaarRCParams () {
    findFile ();
  }

  public void findFile () {
    // initialize parameter map from various places

    String home = System.getProperty("user.home");
    boolean found = false;

    try {
      File f = new File(home+File.separator+".cougaarrc");
      if (! f.exists()) {
        // System.err.println("Warning: no \""+f+"\"");
      } else {
        parseParameterStream(f.toString(), new FileInputStream(f));
        found=true;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      InputStream in = ConfigFinder.getInstance().open("cougaar.rc");
      if (in != null) {
        parseParameterStream("cougaar.rc", in);
        found=true;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (!found && !warnedBefore) {
      System.out.println("Note: Could not find cougaar.rc style parameters, no .cougaarrc or ConfigPath/cougaar.rc");
      System.out.println("If you are trying to use the cougaar.rc file and not script parameters, check your cougaar install path.");
      warnedBefore = true;
    }
	if (debug)
      System.out.println("CougaarRCParams - params now " + parameterMap);

  }

  protected void parseParameterStream(String sname, InputStream in) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    int l = 0;
    String line;
    while ((line = br.readLine()) != null) {
      l++;
      line = line.trim();
      if (line.startsWith("#") ||
          line.startsWith(";") ||
          line.length() == 0)
        continue;
      try {
        int i = line.indexOf("=");
        String param = line.substring(0, i).trim();
        String value = line.substring(i+1).trim();
        // don't overwrite values - first wins forever
        if (parameterMap.get(param) == null)
          parameterMap.put(param, value);
      } catch (RuntimeException re) {
        System.err.println("Badly formed line in \""+sname+"\" ("+l+"):\n"+line);
      }
    }
    br.close();
  }

  public String getParam (String name) {
	return (String) parameterMap.get (name);
  }

  private boolean debug = "true".equals(System.getProperty("org.cougaar.mlm.ui.grabber.config.CougaarRCParams","false"));
  protected Map parameterMap = new HashMap(89);
}
