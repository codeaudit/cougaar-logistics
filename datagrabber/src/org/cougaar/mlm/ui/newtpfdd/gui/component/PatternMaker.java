/* $Header: /opt/rep/cougaar/logistics/datagrabber/src/org/cougaar/mlm/ui/newtpfdd/gui/component/PatternMaker.java,v 1.3 2002-08-07 21:09:33 tom Exp $ */

/*
  Copyright (C) 1999-2000 Ascent Technology Inc. (Program).  All rights
  Reserved.
  
  This material has been developed pursuant to the BBN/RTI "ALPINE"
  Joint Venture contract number MDA972-97-C-0800, by Ascent Technology,
  Inc. 64 Sidney Street, Suite 380, Cambridge, MA 02139.

  @author Harry Tsai
*/

package org.cougaar.mlm.ui.newtpfdd.gui.component;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;


public class PatternMaker {

  static int imageWidth_ = 128;
  static int imageHeight_ = 16;

  static Toolkit tk_ = Toolkit.getDefaultToolkit();

  public    static final int Vertical = 0;
  public    static final int Horizontal = 1;
  public    static final int ThickVertical = 2;
  public    static final int ThickHorizontal = 3;
  public    static final int Specks = 4;
  public    static final int DiagonalPositive = 5;
  public    static final int DiagonalNegative = 6;
  public    static final int Alternator = 7;
  public    static final int DiagonalHatch = 8;
  public    static final int DiagonalBrokenNegative = 9;
  public    static final int DiagonalBrokenPositive= 10;
  public    static final int ArrowAlternator = 11;
  public    static final int ThickDiagonalPositive = 12;
  public    static final int ThickDiagonalNegative = 13;
  public    static final int ReallyThickDiagonalPositive = 14;
  public    static final int ReallyThickDiagonalNegative = 15;
  public    static final int ThickDiagonalBrokenPositive= 16;
  public    static final int ThickDiagonalBrokenNegative = 17;
  public    static final int BrokenVertical = 18;
  public    static final int StarsVertical = 19;
  public    static final int LooseSpecks = 20;
  public    static final int DenseSpecks = 21;

  public    static final int Patterns = DenseSpecks+1;

  public PatternMaker() {
  }

  public static int getHeight() { return imageHeight_; }
  public static int getWidth() { return imageWidth_; }

  public static Image
  makeAlternator(int fgColor, int bgColor, int spacing) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];
 
    int index, y,x;
 
    index = 0;
    int band = 16;
    for (y=0; y<h; y++) {
      int xa = 0;
      for (x=0; x<w; x++) {
	if (x > xa*band) xa++;
	if (y % spacing == 0 && xa % 2 == 0)
	  pix[index++] = fgColor;
	else if ((y+spacing/2) % spacing == 0 && xa % 2 != 0)
	  pix[index++] = fgColor;
	else
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeArrowAlternator(int fgColor, int bgColor, int spacing) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];
 
    int index, y,x;
 
    index = 0;
    int band = 8;

    for (y=0; y<h; y++) {
      int xa = 0;
      for (x=0; x<w; x++) {

	if (x > xa*band) xa++;

	if (xa % 2 == 0) {
	  if ((y-1) % spacing == 0 && (x+1) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y+1) % spacing == 0 && (x+1) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y+2) % spacing == 0 && (x+2) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y-2) % spacing == 0 && (x+2) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if (y % spacing == 0) {
	    pix[index++] = fgColor;
	  }
	  else
	    pix[index++] = bgColor;
	}
	else {
	  if ((y-1+spacing/2) % spacing == 0 && (x+1) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y+1+spacing/2) % spacing == 0 && (x+1) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y-2+spacing/2) % spacing == 0 && (x+2) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y+2+spacing/2) % spacing == 0 && (x+2) == xa*band) {
	    pix[index++] = fgColor;
	  }
	  else if ((y+spacing/2) % spacing == 0) {
	    pix[index++] = fgColor;
	  }
	  else
	    pix[index++] = bgColor;

	} 
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeBrokenVertical(int fgColor, int bgColor, int spacing) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];
 
    int index, y,x;
    int band = 8;
 
    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if (x % spacing == 0) {
	  if (x/spacing % 2 == 0 && 
	      (y % band !=0) && (y+1 % band !=0)) {
	    pix[index++] =  fgColor; // Color.red.getRGB();
	  }
	  else if (x/spacing % 2 != 0 && 
		   (y+band/2) % band !=0 && (y+1+band/2) % band !=0) {
	    pix[index++] = fgColor; /// Color.blue.getRGB();
	  }
	  else
	    pix[index++] = bgColor;
	}
	else
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeDiagonal(int fgColor, int bgColor, int spacing, boolean slopeIsPositive,
	       int thickness) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];
 
    int index, y,x,j;
 
    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {

	int term;
	if (slopeIsPositive) term = x+y;
	else term = x-y;

	boolean foregroundPixel = false;

	for (j=0; j < thickness; j++) {
	  if ((term - j) % spacing == 0)
	    foregroundPixel = true;
	}

	if (foregroundPixel)
	  pix[index++] = fgColor;
	else if (pix[index] == 0)
	  pix[index++] = bgColor;
	else;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeDiagonalBroken(int fgColor, int bgColor, int spacing,boolean slopeIsPositive,
		     int thickness) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];
 
    int index, y,x,j,term;
    int band = 8;
 
    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {

	if (slopeIsPositive) term = x+y;
	else term = x-y;
 
	boolean foregroundPixel = false;
 
	for (j=0; j < thickness; j++) {
	  if ((term - j) % spacing == 0 && x % band !=0)
	    foregroundPixel = true;
	}
 
	if (foregroundPixel)
	  pix[index++] = fgColor;
	else if (pix[index] == 0)
	  pix[index++] = bgColor;
	else;

      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeDiagonalHatch(int fgColor, int bgColor, int spacing) {

    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x;

    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if ((x-y) % spacing == 0 || (x+y) % spacing == 0) 
	  pix[index++] = fgColor;
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeHorizontal(int fgColor, int bgColor, int spacing) {

    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x;

    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if (y % spacing == 0) 
	  pix[index++] = fgColor;
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makePattern(int pattern, Color colorFg, Color colorBg) {

    int spacing = 8; 
    int fgColor = colorFg.getRGB();
    int bgColor = colorBg.getRGB();
    Image image = null;

    switch (pattern) { 
    case Vertical:
      image = makeVertical(fgColor,bgColor,spacing); 
      break;

    case Horizontal :
      image = makeHorizontal(fgColor,bgColor,spacing); 
      break;

    case  ThickHorizontal :
      image = makeThickHorizontal(fgColor,bgColor,spacing); 
      break;

    case ThickVertical:
      image = makeThickVertical(fgColor,bgColor,spacing); 
      break;

    case StarsVertical:
      image = makeStarsVertical(fgColor,bgColor,Color.white.getRGB()); 
      break;

    case Specks:
      image = makeSpecks(fgColor,bgColor,spacing/2); 
      break;

    case DenseSpecks:
      image = makeSpecks(fgColor,bgColor,spacing/4); 
      break;

    case LooseSpecks:
      image = makeSpecks(fgColor,bgColor,spacing); 
      break;

    case DiagonalPositive:
      image = makeDiagonal(fgColor,bgColor,spacing,true,1); 
      break;

    case DiagonalNegative:
      image = makeDiagonal(fgColor,bgColor,spacing,false,1); 
      break;

    case ThickDiagonalNegative:
      image = makeDiagonal(fgColor,bgColor,spacing, false,2); 
      break;

    case ThickDiagonalPositive:
      image = makeDiagonal(fgColor,bgColor,spacing, true,2); 
      break;

    case ReallyThickDiagonalPositive:
      image = makeDiagonal(fgColor,bgColor,spacing,true,4); 
      break;

    case ReallyThickDiagonalNegative:
      image = makeDiagonal(fgColor,bgColor,spacing,false,4); 
      break;

    case Alternator:
      image = makeAlternator(fgColor,bgColor,spacing); 
      break;

    case DiagonalBrokenNegative:
      image = makeDiagonalBroken(fgColor,bgColor,4,false,2); 
      break;

    case DiagonalBrokenPositive:
      image = makeDiagonalBroken(fgColor,bgColor,4,true,2); 
      break;

    case ThickDiagonalBrokenNegative:
      image = makeDiagonalBroken(fgColor,bgColor,8,false,3); 
      break;

    case ThickDiagonalBrokenPositive:
      image = makeDiagonalBroken(fgColor,bgColor,8,true,3); 
      break;

    case ArrowAlternator:
      image = makeArrowAlternator(fgColor,bgColor,spacing); 
      break;

    case DiagonalHatch:
      image = makeDiagonalHatch(fgColor,bgColor,16); 
      break;

    case BrokenVertical:
      image = makeBrokenVertical(fgColor,bgColor,4); 
      break;
    }
	
    return image;
  }

  public static Image
  makeSpecks(int fgColor, int bgColor, int spacing) {

    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x;

    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if (y % spacing == 0 && x % spacing == 0) 
	  pix[index++] = fgColor;
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeStarsVertical(int fgColor, int bgColor, int starColor) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];
 
 
    int index, y,x,j;
    int spacing = 16;   //hardwired
 
    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
 
	int term1 = x+y;
	int term2 = x-y;
 
	boolean foregroundPixel = false;
 
	for (j=0; j < 2; j++) {    
	  if ((term1 - j) % spacing == 0)
	    foregroundPixel = true;
	  else if ((term2 - j) % spacing == 0)
	    foregroundPixel = true;
	  else;
	}

	boolean foregroundPixel2 = true;
	for (j =-3; j<4; j++) {
	  if ((y+j) % spacing == 0) foregroundPixel2= false;
	}

	boolean foregroundPixel3 = false;
	for (j =-1; j<1; j++) {
	  if ((x+spacing/2+j) % spacing == 0) foregroundPixel3 = true;
	}

	boolean foregroundPixel4 = true;
	for (j =-2; j<3; j++) {
	  if ((y+j) % spacing == 0) foregroundPixel4= false;
	}

	if (foregroundPixel && foregroundPixel2)
	  pix[index++] = starColor;
	else if (foregroundPixel3 && foregroundPixel4)
	  pix[index++] = starColor;
	else if (x % spacing == 0 || x % (spacing/2) == 0)
	  pix[index++] = fgColor;
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeStarsVerticalBad(int fgColor, int bgColor, int starColor) {
 
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x,j;
    int spacing = 16;    //hardwired
    int band = 4;
 
    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {

	boolean sf = true;
	boolean sf2 = false;

	for (j =-3; j<4; j++) {
	  if ((x+j) % spacing == 0) 
	    sf = false;
	} 

	for (j =0; j<1; j++) {
	  if (((x-y+j) % spacing == 0 || (x+y+j) % spacing == 0))
	    sf2 = true;
	} 

	if (x % spacing == 0) {
	  pix[index++] = fgColor;
	}
	else if (sf && sf2) {
	  pix[index++] = starColor;
	}

	else if (x % band == 0 && y % band == 0 && y % spacing !=0 && sf) {
	  pix[index++] = starColor;
	}

	else
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeThickHorizontal(int fgColor, int bgColor, int spacing) {

    // sorta broken, set for thickness == 2
    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x;

    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if (y % spacing == 0 || (y+1) % spacing == 0) 
	  pix[index++] = fgColor;
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeThickVertical(int fgColor, int bgColor, int spacing) {

    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x,j;
    int thickness = 2;

    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if (x % spacing == 0) {
	  x--;
	  for (j = 0; j<thickness; j++) {
	    pix[index++] = fgColor;
	    x++;
	  }
	}
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static Image
  makeVertical(int fgColor, int bgColor, int spacing) {

    int w = imageWidth_;
    int h = imageHeight_;
    int pix[] = new int[w*h];

    int index, y,x;

    index = 0;
    for (y=0; y<h; y++) {
      for (x=0; x<w; x++) {
	if (x % spacing == 0) 
	  pix[index++] = fgColor;
	else 
	  pix[index++] = bgColor;
      }
    }
    MemoryImageSource mi = new MemoryImageSource(w, h, pix, 0, w);
    Image image = tk_.createImage(mi);
    return image;
  }

  public static void
  paint(Image image, Rectangle r, Graphics g, ImageObserver c) {

    if (c == null) { return; }
    if (r == null) { return; }
    if (g == null) { return; }
    if (image == null) { return; }
 
    Rectangle cachedClip = g.getClipBounds();
    if (cachedClip == null) {
      //  System.err.println("null clip! returning");
      return;
      //  try { throw (new Exception()); }
      //  catch (Exception e) { e.printStackTrace(); }
    }
 
    boolean tiling;
 
    int width = r.width;
    int height = r.height;
    int imageWidth = PatternMaker.getWidth();
    int imageHeight = PatternMaker.getHeight();
 
    if (width > imageWidth || height > imageHeight)
      tiling = true;
    else
      tiling = false;
 
    g.setClip(r.x+1, r.y+1, r.width-1,r.height-1);
 
    if (tiling) {
      int wCount = Math.max(Math.round(width/imageWidth)+1, 1);
      int hCount = Math.max(Math.round(height/imageHeight)+1, 1);
 
      // tile in images here...
      for (int x = 0; x < wCount; x++) {
	for (int y = 0; y < hCount; y++) {
	  g.drawImage(image, r.x + x*imageWidth, r.y + y*imageHeight,
		      imageWidth,imageHeight,c);
	}
      }
      // System.err.println("tiled h:" + hCount+ " w:" + wCount);
    }
    else {
      g.drawImage(image,r.x,r.y,imageWidth,imageHeight,c);
    }
 
    // restore clip
    g.setClip(cachedClip.x, cachedClip.y, cachedClip.width,cachedClip.height);
  }
}
