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

public class FontList extends Command
{	String S[];

	public FontList (int ec, int eid, int l, DataInputStream in)
		throws IOException
	{	super(ec,eid,l,in);
		int count=0,i=0;
		while (i<args.length)
		{	count++; i+=args[i]+1;
		}
		S=new String[count];
		count=0; i=0;
		while (i<args.length)
		{	char a[]=new char[args[i]];
			for (int j=0; j<args[i]; j++)
				a[j]=(char)args[i+j+1];
			S[count]=new String(a);
			count++; i+=args[i]+1;
		}
	}
	
	public String toString ()
	{	String s="Font List: ";
		for (int i=0; i<S.length-1; i++) s=s+S[i]+", ";
		s=s+S[S.length-1];
		return s;
	}
}
