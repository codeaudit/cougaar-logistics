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

public class EllipticalArcClosedElement extends Command
{	int X,Y,X1,Y1,X2,Y2,SX1,SY1,SX2,SY2;
	boolean Closed;

	public EllipticalArcClosedElement (int ec, int eid, int l, DataInputStream in)
		throws IOException
	{	super(ec,eid,l,in);
		X=makeInt(0);
		Y=makeInt(1);
		X1=makeInt(2);
		Y1=makeInt(3);
		X2=makeInt(4);
		Y2=makeInt(5);
		SX1=makeInt(6);
		SY1=makeInt(7);
		SX2=makeInt(8);
		SY2=makeInt(9);
		Closed=(makeInt(10)==1);
	}
	
	public String toString ()
	{	return "Ellipse ["+X+","+Y+"] ["+X1+","+Y1+"] ["+X2+","+Y2+"] ["+
			SX1+","+SY1+"] ["+SX2+","+SY2+"] "+
			(Closed?"Closed":"Pie");
	}
}
