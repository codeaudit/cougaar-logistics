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

import org.cougaar.logistics.ui.stoplight.ui.map.layer.cgmicon.cgm.*;

public class OpenMapCGMDisplay extends CGMDisplay
{

  private CGM myCGM = null;


  public OpenMapCGMDisplay (CGM cgm)
  {

    super(cgm);

    // remember so when we clone we can re-use teh CGM drawing, it should be
    // the same no matter the location
    myCGM = cgm;
  }

  public void setChangeFill(boolean custom)
  {
    myCGM.setChangeFill(custom);
  }

  public void setOrigin (int orgX, int orgY)
  {
    W=orgX;
    H=orgY;
  }

  public void showCGMCommands()
  {
    myCGM.showCGMCommands();
  }


	public void scale (int w, int h)
	{	if (Extent==null) return;
		double fx=(double)w/(Extent[2]-Extent[0]);
		if (fx*(Extent[3]-Extent[1])>h)
		{	fx=(double)h/(Extent[3]-Extent[1]);
		}
		fx*=1.0;
		DX=fx; DY=fx;
		X=-Extent[0]*fx; Y=-Extent[1]*fx;
		Cgm.scale(this);
	}

  public Object makeAnother()
  {
    CGM newCGM = (CGM) this.myCGM.clone();
    return (new OpenMapCGMDisplay(newCGM));

  }


}