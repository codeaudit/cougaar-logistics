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

public class RectangleElement extends Command
{	int X1,Y1,X2,Y2;
	int X,Y,W,H;

	public RectangleElement (int ec, int eid, int l, DataInputStream in)
		throws IOException
	{	super(ec,eid,l,in);
		X1=makeInt(0);
		Y1=makeInt(1);
		X2=makeInt(2);
		Y2=makeInt(3);
	}
	
	public String toString ()
	{	return "Rectangle ["+X1+","+Y1+"] ["+X2+","+Y2+"]";
	}

	public void scale (CGMDisplay d)
	{	X=d.x(X1); Y=d.y(Y1);
		W=d.x(X2)-X-1; H=d.y(Y2)-Y-1;
	}
	
	public void paint (CGMDisplay d)
	{	if (d.getFilled())
		{	d.graphics().setColor(d.getFillColor());
			d.graphics().fillRect(X,Y,W,H);
		}
		else
		{	d.graphics().setColor(d.getFillColor());
			if (!d.getEdge())
				d.graphics().drawRect(X,Y,W,H);
		}
		if (d.getEdge())
		{	d.graphics().setColor(d.getEdgeColor());
			d.graphics().drawRect(X,Y,W,H);
		}
	}
}
