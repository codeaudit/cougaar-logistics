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
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class CGMDisplay
{	Graphics G;

	protected double X=0,Y=0;
	protected double DX=1,DY=1;
	protected int W,H;
	protected CGM Cgm;
	Color FillColor=Color.black,EdgeColor=Color.black,
		LineColor=Color.black,TextColor=Color.black;
	boolean Filled=true,Edge=true;
	int TextSize=10;
	protected int Extent[]={-30000,-30000,30000,30000};

	public CGMDisplay (CGM cgm)
	{	int extent[]=cgm.extent();
		if (extent!=null) Extent=extent;
		Cgm=cgm;
	}

	public void paint (Graphics g)
	{	G=g;
		Cgm.paint(this);
	}

	public int x (int x)
	{	return W+(int)(X+x*DX);
	}

	public int y (int y)
	{	return H-(int)(Y+y*DY);
	}

	public Graphics graphics ()
	{	return G;
	}

	public void setFillColor (Color c) { FillColor=c; }
	public Color getFillColor () { return FillColor; }
	public void setFilled (boolean flag) { Filled=flag; }
	public boolean getFilled () { return Filled; }
	public void setEdgeColor (Color c) { EdgeColor=c; }
	public Color getEdgeColor () { return EdgeColor; }
	public void setEdge (boolean flag) { Edge=flag; }
	public boolean getEdge () { return Edge; }
	public void setLineColor (Color c) { LineColor=c; }
	public Color getLineColor () { return LineColor; }
	public void setTextColor (Color c) { TextColor=c; }
	public Color getTextColor () { return TextColor; }
	public void setTextSize (int h) { TextSize=h; }
	public int getTextSize () { return TextSize; }
	public double factorX () { return DX; }
	public double factorY () { return DY; }


	public void scale (int w, int h)
	{	if (Extent==null) return;
		double fx=(double)w/(Extent[2]-Extent[0]);
		if (fx*(Extent[3]-Extent[1])>h)
		{	fx=(double)h/(Extent[3]-Extent[1]);
		}
		fx*=1.0; //0.9;
		DX=fx; DY=fx;
		X=-Extent[0]*fx; Y=-Extent[1]*fx;
		W=(int)(w-fx*(Extent[2]-Extent[0]))/2;
		H=(int)(h-(h-fx*(Extent[3]-Extent[1]))/2);
		Cgm.scale(this);
	}

	public void frame (Graphics g)
	{	if (Extent==null) return;
		g.setColor(Color.black);
		g.drawRect(
			x(Extent[0])-1,y(Extent[3])-1,
			(int)Math.abs((Extent[2]-Extent[0])*DX)+1,
			(int)Math.abs((Extent[3]-Extent[1])*DY)+1);
	}

	public static void main (String args[])
		throws IOException
	{	DataInputStream in=new DataInputStream(
			new FileInputStream(args[0]));
		CGM cgm=new CGM();
		cgm.read(in);
		in.close();
		CGMDisplay d=new CGMDisplay(cgm);
		CGMPanel p=new CGMPanel(d);
		Frame f=new Frame ();
		f.addWindowListener(new WindowAdapter ()
			{	public void windowClosing (WindowEvent e)
				{	System.exit(0);
				}
			});
		f.setSize(600,450);
		f.setLayout(new BorderLayout());
		f.add("Center",p);
		f.setVisible(true);
	}
    public void changeColor(Color oldc, Color newc)
    {// actually changes the color in the cgm commands having this oldc, replacing
    // it with newc
      Cgm.changeColor(oldc,newc);
    }

}
