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

public class PolylineElement extends Command
{	int X[],Y[];
	int X0[],Y0[];

	public PolylineElement (int ec, int eid, int l, DataInputStream in)
		throws IOException
	{	super(ec,eid,l,in);
		int n=args.length/4;
		X=new int[n]; Y=new int[n];
		for (int i=0; i<n; i++)
		{	X[i]=makeInt(2*i);
			Y[i]=makeInt(2*i+1);
		}
	}
	
	public String toString ()
	{	String s="Polyline";
		for (int i=0; i<X.length; i++)
			s=s+" ["+X[i]+","+Y[i]+"]";
		return s;
	}

	public void scale (CGMDisplay d)
	{	X0=new int[X.length]; Y0=new int[X.length];
		for (int i=0; i<X.length; i++)
		{	X0[i]=d.x(X[i]);
			Y0[i]=d.y(Y[i]);
		}
	}
	
	public void paint (CGMDisplay d)
	{	d.graphics().setColor(d.getLineColor());
		d.graphics().drawPolyline(X0,Y0,X0.length);
	}
}
