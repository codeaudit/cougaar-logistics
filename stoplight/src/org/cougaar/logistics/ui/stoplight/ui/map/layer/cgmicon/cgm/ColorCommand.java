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

import java.awt.*;
import java.io.*;

public class ColorCommand extends Command
{	int R,G,B;
	Color C;
	Color Colors[]=
	{	Color.black,Color.white,Color.green,Color.yellow,Color.blue,
		Color.magenta,Color.cyan,Color.red,
		Color.black.brighter(),Color.white.darker(),
		Color.green.darker(),Color.yellow.darker(),Color.blue.darker(),
		Color.magenta.darker(),Color.cyan.darker(),Color.red.darker(),
	};

	public ColorCommand (int ec, int eid, int l, DataInputStream in)
		throws IOException
	{	super(ec,eid,l,in);
		if (args.length>=3)
		{	R=args[0];
			G=args[1];
			B=args[2];
			C=new Color(R,G,B);
		}
		else if (args.length>0 &&
			args[0]>=1 && args[0]<=Colors.length)
		{	C=Colors[args[0]-1];
		}
		else
		{	C=new Color(128,128,128);
		}
	}

	public String toString ()
	{	return "Fill Color Input "+R+","+G+","+B;
	}

	public void paint (CGMDisplay d)
	{	d.setFillColor(C);
	}
}
