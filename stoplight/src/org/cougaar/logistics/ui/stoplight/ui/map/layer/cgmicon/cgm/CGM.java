/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
package org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.cgm;

import java.io.*;
import java.util.*;
import java.awt.Color;

public class CGM implements Cloneable
{ Vector V;
  public boolean changeCGMFill = false;
        public void setChangeFill(boolean custom)
        {
          changeCGMFill = custom;
        }

	public void read (DataInputStream in)
		throws IOException
	{	V=new Vector();
		while (true)
		{	Command c=Command.read(in);
			if (c==null) break;
			V.addElement(c);
		}
	}

	public void paint (CGMDisplay d)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Command c=(Command)e.nextElement();
			if (!((c instanceof FillColor || c instanceof ColorCommand)&& changeCGMFill))
                        // FillColor.paint changes the fill color of d
                        {
                          c.paint(d);
                        }
                        else
                          {/*System.out.println("Command not painted: " + c.toString());*/}
		}
	}

	public void scale (CGMDisplay d)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Command c=(Command)e.nextElement();
			c.scale(d);
		}
	}

	public int[] extent ()
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Command c=(Command)e.nextElement();
			if (c instanceof VDCExtent)
				return ((VDCExtent)c).extent();
		}
		return null;
	}

	public static void main (String args[])
		throws IOException
	{	DataInputStream in=new DataInputStream(
			new FileInputStream(args[0]));
		CGM cgm=new CGM();
		cgm.read(in);
		in.close();
	}

  public Object clone()
  {
    CGM newOne = new CGM();
//System.out.println("in cgm.clone");
    newOne.V = new Vector ();
    for (int i=0;i<this.V.size();i++)
    {
      newOne.V.addElement(((Command)this.V.elementAt(i)).clone());
      //System.out.println("Command: " + (Command)newOne.V.elementAt(i));
    }
    return newOne;
  }

  public void showCGMCommands()
  {
    for (int i=0;i<V.size();i++)
      System.out.println("Command: " + (Command)V.elementAt(i));
  }

    public void changeColor(Color oldc, Color newc)
    {// actually changes the color in the cgm commands having this oldc, replacing
    // it with newc
    // find each color command whose color matches oldc, and substitute newc
    Command temp;
    Color currcolor;
    for (int i=0;i<V.size();i++)
    {
      temp = (Command)V.elementAt(i);
      if (temp instanceof ColorCommand)
      {// compare color to oldc
        currcolor = ((ColorCommand)temp).C;
        if (currcolor.equals(oldc))
        {
          ((ColorCommand)temp).C = new Color(newc.getRed(),newc.getGreen(),newc.getBlue());
        }
      }
    }

    }
}
