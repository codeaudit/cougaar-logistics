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
package org.cougaar.logistics.ui.stoplight.ui.inventory;

import java.util.*;
import java.io.*;

public class NSNItemUnits implements Runnable
{
  private Hashtable itemTable = new Hashtable(1);

  private String file = null;

  public NSNItemUnits(String file)
  {
    this.file = file;

    Thread thread = new Thread(this);
    thread.setPriority(((thread.getPriority()-2) < Thread.MIN_PRIORITY) ? Thread.MIN_PRIORITY : (thread.getPriority()-2));
    thread.start();
  }

  public void run()
  {
    long time = System.currentTimeMillis();
    try
    {
      BufferedReader br = new BufferedReader ( new InputStreamReader (new FileInputStream (file)));
      
      String line = null;
      StringTokenizer stok = null;
      while ((line = br.readLine()) != null)
      {
        stok = new StringTokenizer (line, ",");
        itemTable.put(stok.nextToken(), stok.nextToken());
      }
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
    
    System.out.println(System.currentTimeMillis() - time + "ms");
  }

  public String getUnit(String nsn)
  {
    String unit = (String)itemTable.get(nsn);

    if (unit == null)
    {
      throw(new RuntimeException("No unit type found for " + nsn));
    }

    return(unit);
  }

  public static void main(String[] args)
  {
    System.out.println ("testing..");

    NSNItemUnits nsnUnits = new NSNItemUnits("ItemUnits.txt");

    System.out.println ("NSN/8970014728983 unit is: " + nsnUnits.getUnit("NSN/8970014728983") );
    System.out.println ("NSN/9150000825636 unit is: " + nsnUnits.getUnit("NSN/9150000825636") );
    System.out.println ("NSN/9150001416770 unit is: " + nsnUnits.getUnit("NSN/9150001416770") );
    System.out.println ("NSN/6515014728648 unit is: " + nsnUnits.getUnit("NSN/6515014728648") );
  }
}
