/* **********************************************************************
 *
 *  Clark Software Engineering, Ltd.
 *  5100 Springfield St. Ste 308
 *  Dayton, OH 45431-1263
 *  (937) 256-7848
 *
 *  Copyright (C) 2001
 *  This software is subject to copyright protection under the laws of
 *  the United States and other countries.
 *
 */

package org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon;

import java.util.Hashtable;
import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.cougaar.util.ConfigFinder;

public class OMCGMIcons
{

  public String loadFile = "cgmload.txt";

  private Hashtable iconsByName = new Hashtable(37);

  public OMCGMIcons() throws IOException
  {
    try
    {
      loadUp (loadFile);
    }
    catch (FileNotFoundException fnfe)
    {
      System.err.println ("default load file, cgmload.txt, not found, no icons loaded");
    }

  }

  public OMCGMIcons (String loadMe) throws FileNotFoundException, IOException
  {
    loadUp (loadMe);
  }

  private void loadUp (String loadMe) throws FileNotFoundException, IOException
  {
    System.out.println ("loading CGM icons from file: " + loadMe);

    ConfigFinder cf = ConfigFinder.getInstance();
    BufferedReader br = new BufferedReader ( new InputStreamReader ( new FileInputStream (cf.locateFile(loadMe))));

    String line = br.readLine();

    OMCGM omcgmVal;
    while (line != null)
    {

//      System.out.println ("loading CGM icons line is: " + line);

      // tab separated file, like from Excel
      StringTokenizer stok = new StringTokenizer (line, "\t");

      String idkey = stok.nextToken();

      String fileName = stok.nextToken();

      try
      {
        String nextTok = (String) stok.nextElement();

        if (nextTok.equals ("V"))
        {
          // this is a VISIO generated CGM file
//          System.out.println (idkey + " is a visio generated CGM file.");

          try
          {
            // after this "V" (for Visio) will we find the echelon indication, if any
            String unitSize = stok.nextToken();

            //omcgmVal = new OMCGMbyVisio (fileName,unitSize);
            omcgmVal = new OMCGM (fileName,unitSize);
            omcgmVal = new OMCGMbyVisio(omcgmVal);
            //omcgmVal.omcgmdisp.setChangeFill(true);
          }
          catch (java.util.NoSuchElementException nsee)
          {
            omcgmVal = new OMCGM(fileName);
            omcgmVal = new OMCGMbyVisio (omcgmVal);

            iconsByName.put(idkey, omcgmVal);

            line = br.readLine();

            continue;
          }

        }

        String unitSize = nextTok;

        // if we catch an exception above this next line won't be called
        omcgmVal = new OMCGM ( fileName, unitSize );
            //omcgmVal.omcgmdisp.setChangeFill(true);
      }

      catch ( java.util.NoSuchElementException nsee)
      {
        try
        {
          omcgmVal = new OMCGM (fileName);
            //omcgmVal.omcgmdisp.setChangeFill(true);
        }
        catch (FileNotFoundException fnf)
        {
          System.err.println ("CGM file not found: " + fileName);
          line = br.readLine();
          continue;
        }

      }

      catch (FileNotFoundException fnf)
      {
        System.err.println ("CGM file not found: " + fileName);
        line = br.readLine();
        continue;
      }

      //omcgmVal.omcgmdisp.setFillColor(Color.red);
      iconsByName.put(idkey, omcgmVal);

      line = br.readLine();
    }

  }

  public OMCGM get (String idkey)
  {
    OMCGM orig = (OMCGM) iconsByName.get(idkey);
    OMCGM ret;
    if (orig instanceof OMCGMbyVisio)
      ret = ( (OMCGMbyVisio) orig).makeAnother();
    else
      ret = orig.makeAnother();

    return ret;
  }

}