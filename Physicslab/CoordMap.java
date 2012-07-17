/*
  File: CoordMap.java

  Part of the www.MyPhysicsLab.com physics simulation applet.
  Copyright (c) 2001  Erik Neumann

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

  Contact Erik Neumann at erikn@MyPhysicsLab.com or
  610 N. 65th St. Seattle WA 98103

*/
// CoordMap class
/* Provides the coordinate mapping between screen and simulation world.
  Calculates the scaling and origin coordinates.

  QUICK GUIDE:
  new CoordMap(int y_dir, double x1, double x2, double y1, double y2,
    int align_x, int align_y)
  x1,x2,y1,y2 give the simulation coords of simulation area's left, right,
  top, bottom. This is different from the screen coords!  We assure that the
  rect x1,x2,y1,y2 fits in the screen area, but there is usually some excess
  area.

  y_dir is either CoordMap.INCREASE_UP or CoordMap.INCREASE_DOWN
  (specifies whether the y coordinate increases going up the screen or down).
  Note that always y1 < y2, so
    INCREASE_DOWN:  y1 = top,    y2 = bottom
    INCREASE_UP:    y1 = bottom, y2 = top

  There are ways to find the actual screen boundaries in simulation coords,
  but it is better to stick to using the simulation area -- because the screen
  area can change due to resizing of the window.  After a resize, we are
  guaranteed to have the entire simulation area showing in the window, plus
  some extra due to the screen having a different aspect ratio.

  If you want to use the entire screen area, try using the expand() method to
  grow the simulation area.  expand() adjusts the simulation limits (passed in
  as x1,x2,y1,y2, but stored as simMinX,simMaxX,simMinY,simMaxY). Then you can
  use the whole screen area, but if there is any later resizing of the window
  your simulation area won't change.

  align_x and align_y determine how the map is shifted when the screen area is
  too wide or too tall. That is, x1,x2,y1,y2 define a rectangle with a certain
  aspect ratio. The screen will usually have a different aspect ratio, so we
  have to fit the x1,x2,y1,y2 rectangle into the screen. The question is
  whether the x1,x2,y1,y2 rectangle is aligned to the left, right or middle.
  This is specified for horizontal and vertical separately. The extra area is
  still available to the app (but see comments above) its just a question of
  where the origin is placed.

  If setFillScreen(true) then the aspect ratio is not maintained, and the
  given x1,x2,y1,y2 will match the screen boundaries regardless of aspect
  ratio.

  TO DO:
  Add some exceptions for divide by zero errors in recalc().  Eg. width=0 is a
  problem.

*/
package com.myphysicslab.simlab;

import java.awt.*;

// DoubleRect is currently immutable... should it be?
final class DoubleRect  {
  private final double xMin, xMax, yMin, yMax;
  public DoubleRect(double xMin, double yMin, double xMax, double yMax) {
    if (xMin > xMax)
      throw new IllegalArgumentException("xMin="+xMin+" must be less than xMax="+xMax);
    if (yMin > yMax)
      throw new IllegalArgumentException("yMin="+yMin+" must be less than yMax="+yMax);
    this.xMin = xMin;
    this.xMax = xMax;
    this.yMin = yMin;
    this.yMax = yMax;
  }
  public String toString() {
    return "DoubleRect xMin="+xMin+" xMax="+xMax+" yMin="+yMin+" yMax="+yMax;
  }
  public double getXMin() { return xMin; }
  public double getXMax() { return xMax; }
  public double getYMin() { return yMin; }
  public double getYMax() { return yMax; }
  public double getWidth() { return xMax - xMin; }
  public double getHeight() { return yMax - yMin; }
}

/* ConvertMap provides a "safe" subset of operations from CoordMap.
   It is an 'immutable' version of the CoordMap, so it can safely
   be passed around without danger of the CoordMap being changed.
 */
class ConvertMap {
  private CoordMap map;

  public ConvertMap(CoordMap map) {
    this.map = map;
  }
  public int simToScreenScaleX(double x)  {
    /* does only scaling in x direction, no offsetting */
    return map.simToScreenScaleX(x);
  }

  public int simToScreenScaleY(double y)  {
    /* does only scaling in y direction, no offsetting */
    return map.simToScreenScaleY(y);
  }

  public int simToScreenX(double x)  {
    /* Returns the screen coords of x, given the various globals. */
    return map.simToScreenX(x);
  }

  public int simToScreenY(double y)  {
    /* Returns the screen coords of y, given the various globals. */
    return map.simToScreenY(y);
  }

  public double screenToSimX(int scr_x)  {
    /* scr_x is in screen coords, returns simulation coords */
    return map.screenToSimX(scr_x);
  }

  public double screenToSimY(int scr_y) {
    /* scr_y is in screen coords, returns simulation coords */
    return map.screenToSimY(scr_y);
  }

  public DoubleRect getSimBounds() {
    return map.getSimBounds();
  }

  public Rectangle getScreenRect() {
    return map.getScreenRect();
  }
}

public class CoordMap
{
  // class constants for origin location
  public static final int ALIGN_MIDDLE = 0;
  public static final int ALIGN_LEFT = 1;
  public static final int ALIGN_RIGHT = 2;
  public static final int ALIGN_UPPER = 3;
  public static final int ALIGN_LOWER = 4;
  public static final int INCREASE_UP = -1;
  public static final int INCREASE_DOWN = 1;
  private int y_direction = INCREASE_DOWN;
  private int origin_x = 0;  /* CALCULATED origin in screen coords */
  private int origin_y = 0;  /* CALCULATED origin in screen coords */
  private int screen_left = 0;  /* in screen coords (pixels) */
  private int screen_top = 0;  /* in screen coords (pixels) */
  private int screen_width = 0;  /* in screen coords (pixels) */
  private int screen_height = 0;  /* in screen coords (pixels) */
  private double pixel_per_unit_x = 100;  /* CALCULATED */
  private double pixel_per_unit_y = 100;  /* CALCULATED */
  private boolean fill_screen = false;
  private boolean zoomMode = false;
  private boolean originFixed = false; /* for setting origin directly */
  private boolean scaleFixed = false; /* for setting scale directly */
  private double zx1, zx2, zy1, zy2; // zoom simulation coords
  private double simMinX = -10;  /* in simulation coords, left boundary of simulation area */
  private double simMaxX = 10;  /* in simulation coords, right boundary of simulation area */
  private double simMinY = -10;  /* simulation area top (INCREASE_DOWN) or bottom (INCREASE_UP) */
  private double simMaxY = 10;  /* simulation area bottom (INCREASE_UP) or top (INCREASE_DOWN) */
  private int align_x = ALIGN_LEFT;
  private int align_y = ALIGN_UPPER;
  private ConvertMap convertMap = new ConvertMap(this);

  public CoordMap() {
    this(INCREASE_UP, -10.0, 10.0, -10.0, 10.0, ALIGN_MIDDLE, ALIGN_MIDDLE);
  }

  public CoordMap(int y_dir, double x1, double x2, double y1, double y2,
                  int align_x, int align_y) {
    y_direction = y_dir;
    this.align_x = align_x;
    this.align_y = align_y;
    setRange(x1, x2, y1, y2);
  }

  /* this constructor is for hard-coding the origin and scaling factors */
  public CoordMap(int y_dir, double x1, double x2, double y1, double y2,
                  int originX, int originY, double scaleX, double scaleY) {
    y_direction = y_dir;
    originFixed = true;
    origin_x = originX;
    origin_y = originY;
    scaleFixed = true;
    pixel_per_unit_x = scaleX;
    pixel_per_unit_y = scaleY;
    setRange(x1, x2, y1, y2);
  }

  public ConvertMap getConvertMap() {
    return convertMap;
  }

  public String toString() {
    String s = "CoordMap with [";
    s+= scaleFixed ? " scaleFixed, " : "";
    s+= originFixed ? " originFixed, " : "";
    s+= fill_screen ? " fill_screen, " : "";
    s+= ", y_direction="+(y_direction==INCREASE_UP ? "UP" : "DOWN");
    s+= ", SCREEN (left="+screen_left;
    s+=", top="+screen_top;
    s+=", width="+screen_width;
    s+=", height="+screen_height+")";
    s+=", SIM (x1="+simMinX+",x2="+simMaxX+",y1="+simMinY+",y2="+simMaxY+")";
    s+=", scale(x="+pixel_per_unit_x+", y="+pixel_per_unit_y+")";
    s+=", origin(x="+origin_x+", y="+origin_y+")";
    s+="]";
    return s;
  }

  /*
  private void debug(String s)  {
    System.out.println("------- map vars: "+s+" -------");
    System.out.println("sim x1="+simMinX+",x2="+simMaxX+",y1="+simMinY+",y2="+simMaxY);
    System.out.println("screen left="+screen_left+",top="+screen_top+",width="+screen_width+",height="+screen_height);
    System.out.println("origin x="+ origin_x+",y="+origin_y);
    System.out.println("pixel_per_unit_x="+pixel_per_unit_x);
    System.out.println("align x="+align_x+", y="+align_y+")");
  }
  */

  /*
  input:  must have following set:
    screen_width, screen_height, screen_left, screen_top,
    simMinX, simMinY, simMaxX, simMaxY
    y_direction, fill_screen

    additionally, if (fill_screen==false) must have:
    align_x, align_y
  */
  private void recalc() {
    double sim_width = simMaxX - simMinX;
    double sim_height = simMaxY - simMinY;
    if (zoomMode) {
      if (!scaleFixed) {
        pixel_per_unit_x = (double)screen_width/(zx2 - zx1);
        pixel_per_unit_y = (double)screen_height/(zy2 - zy1);
      }
      if (!originFixed) {
        origin_x = screen_left - (int)(zx1 * pixel_per_unit_x +0.4999);
        if (y_direction == INCREASE_DOWN)
          origin_y = screen_top-(int)(zy1*pixel_per_unit_y +0.4999);
        else
          origin_y = screen_top+screen_height+(int)(zy1*pixel_per_unit_y +0.4999);
      }
    } else if (fill_screen) {
      /* fill the screen according to chosen sim values
         calculate resulting pixel_per_unit for x & y
         calculate resulting screen coord origin location
         offsets should be zero
      */
      if (!scaleFixed) {
        pixel_per_unit_x = (double)screen_width/sim_width;
        pixel_per_unit_y = (double)screen_height/sim_height;
      }
      if (!originFixed) {
        origin_x = screen_left - (int)(simMinX * pixel_per_unit_x +0.4999);
        if (y_direction == INCREASE_DOWN)
          origin_y = screen_top-(int)(simMinY*pixel_per_unit_y +0.4999);
        else
          origin_y = screen_top+screen_height+(int)(simMinY*pixel_per_unit_y +0.4999);
      }
    } else {
      if (sim_width <= 0 || sim_height <= 0) {
        System.out.println("WARNING: Coordmap cannot recalc, found zero sim width "
          +sim_width+" or height "+sim_height);
        return;
      }

      if (screen_width <= 0 || screen_height <= 0)
        return;
        //throw new IllegalArgumentException("Coordmap cannot recalc, found zero screen width "
        //  +screen_width+" or height "+screen_height);

      int ideal_height = (int)((double)screen_width*sim_height/sim_width);
      int ideal_width;
      int offset_x, offset_y;
      /* how to figure out location of origin:
        Imagine the rectangle (within screen) that holds the simulation, so it has width =
        sim_width and height = sim_height.  Now impose a temporary coord system over this with
        upperleft being 0,0, and lowerright being 1,1. The origin location is specified in
        these temporary coords. We shift the entire rectangle according to requested
        alignment.  And then finally we can determine the screen coords of the origin.
      */
      if (screen_height < ideal_height)  {// height is limiting factor
        if (!scaleFixed)
          pixel_per_unit_y = pixel_per_unit_x = (double)screen_height/sim_height;
        offset_y = 0;
        ideal_width = (int)(sim_width*pixel_per_unit_x);
        switch (align_x) {
          case ALIGN_LEFT:  offset_x = 0; break;
          case ALIGN_RIGHT: offset_x = screen_width - ideal_width; break;
          case ALIGN_MIDDLE: offset_x = (screen_width - ideal_width)/2; break;
          default: offset_x = 0; break;
        }
      } else  {// width is limiting factor
        pixel_per_unit_y = pixel_per_unit_x = (double)screen_width/sim_width;
        offset_x = 0;
        ideal_height = (int)(sim_height*pixel_per_unit_y);
        switch (align_y) {
          case ALIGN_UPPER:  offset_y = 0; break;
          case ALIGN_MIDDLE: offset_y = (screen_height - ideal_height)/2; break;
          case ALIGN_LOWER:  offset_y = screen_height - ideal_height; break;
          default: offset_y = 0; break;
        }
      }
      if (!originFixed) {
        origin_x = screen_left + offset_x - (int)(simMinX*pixel_per_unit_x);
        if (y_direction == INCREASE_DOWN)
          origin_y = screen_top + offset_y - (int)(simMinY*pixel_per_unit_y);
        else
          origin_y = screen_top + screen_height - offset_y + (int)(simMinY*pixel_per_unit_y);
      }
    }
  }

  public void setAlignment(int align_x, int align_y) {
    this.align_x = align_x;
    this.align_y = align_y;
    originFixed = false;
    recalc();
  }

  public void setOrigin(int x, int y) {
    origin_x = x;
    origin_y = y;
    originFixed = true;
  }

  public void setScale(double x, double y) {
    pixel_per_unit_x = x;
    pixel_per_unit_y = y;
    scaleFixed = true;
  }

  public void zoom(double x1, double x2, double y1, double y2) {
    if (y2 <= y1)
      throw new IllegalArgumentException("CoordMap:zoom() y1="+y1+" must be less than y2="+y2);
    if (x2 <= x1)
      throw new IllegalArgumentException("CoordMap:zoom() x1="+x1+" must be less than x2="+x2);
    zoomMode = true;
    originFixed = scaleFixed = false;
    zx1 = x1; zx2 = x2; zy1 = y1; zy2 = y2;
    recalc();
  }

  public void setZoom(boolean state) {
    zoomMode = state;
    recalc();
  }

  //  If setFillScreen(true) then the aspect ratio is not maintained, and the given
  //  sim boundaries will match the screen boundaries regardless of aspect ratio.
  public void setFillScreen(boolean f)  {
    fill_screen = f;
    scaleFixed = false;
    recalc();
  }

  /* Expand the simulation boundaries to take up the entire screen.
     Why is this useful?  When starting up, we ask for a certain simulation boundary
     area, for example a square boundary of (-5,-5,5,5).  The recalc() procedure then figures out how
     to fit that simulation area into the current screen, which usually is rectangular with
     the width not equal to the height.  Suppose that the width is greater than the height.
     Then expand() changes the simulation area to match the maximum available
     screen area.  In our example, it might change to (-6.3, -5, 6.3, 5) where the horizontal
     boundaries got extended.
  */
  public boolean expand() {
    if (screen_width > 0 && screen_height > 0) {
      simMinX = screenToSimX(screen_left);
      simMaxX = screenToSimX(screen_left + screen_width);
      simMinY = screenToSimY(screen_top);
      simMaxY = screenToSimY(screen_top + screen_height);
      if (simMinY > simMaxY) { // swap yMin and yMax if necessary
        double d = simMinY;
        simMinY = simMaxY;
        simMaxY = d;
      }
      scaleFixed = false;
      recalc();
      return true;
    } else {
      //System.out.println("cannot expand CoordMap because screen size is zero "+screen_width+" "+screen_height);
      return false;
    }
  }

  public void setRange(double xlo, double xhi, double ylo, double yhi)  {
    //System.out.println("map.setRange("+xlo+","+xhi+","+ylo+","+yhi+")");
    simMinX = xlo;
    simMaxX = xhi;
    simMinY = ylo;
    simMaxY = yhi;
    recalc();
  }

  public void setScreen(int left, int top, int width, int height)  {
    //System.out.println("map.setScreen "+left+" "+top+" "+width+" "+height);
    if ((width>0) && (height>0)) {
      screen_top = top;
      screen_left = left;
      screen_width = width;
      screen_height = height;
      recalc();
    }
  }

  public int simToScreenScaleX(double x)  {
    /* does only scaling in x direction, no offsetting */
    return (int)(x*pixel_per_unit_x+0.5);
  }

  public int simToScreenScaleY(double y)  {
    /* does only scaling in y direction, no offsetting */
    return (int)(y*pixel_per_unit_y+0.5);
  }

  public int simToScreenX(double x)  {
    /* Returns the screen coords of x, given the various globals. */
    return origin_x + (int)(x*pixel_per_unit_x+0.5);
  }

  public int simToScreenY(double y)  {
    /* Returns the screen coords of y, given the various globals. */
    return origin_y + y_direction*(int)(y*pixel_per_unit_y+0.5);
  }

  public double screenToSimX(int scr_x)  {
    /* scr_x is in screen coords, returns simulation coords */
    return (double)(scr_x - origin_x)/pixel_per_unit_x;
  }

  public double screenToSimY(int scr_y) {
    /* scr_y is in screen coords, returns simulation coords */
    return y_direction*(double)(scr_y - origin_y)/pixel_per_unit_y;
  }

  public DoubleRect getSimBounds() {
    return new DoubleRect(simMinX, simMinY, simMaxX, simMaxY);
  }

  // Returns the translation of the simulation boundary in screen coords.
  // NOTE: not really the full screen coords!
  public Rectangle getScreenRect() {
    double top = (y_direction == INCREASE_UP) ? simMaxY : simMinY;
    // Rectangle(int left, top, width, height);
    return new Rectangle(simToScreenX(simMinX), simToScreenY(top),
      simToScreenScaleX(simMaxX - simMinX),
      simToScreenScaleY(simMaxY - simMinY));
  }

  public double getMinX() {
    return simMinX;
  }

  public double getMaxX() {
    return simMaxX;
  }

  public double getMinY() {
    return simMinY;
  }

  public double getMaxY() {
    return simMaxY;
  }

  public boolean intersectRect(Rectangle r) {
    int x1, y1, x2, y2;
    x1 = r.x;
    y1 = r.y;
    x2 = x1 + r.width;
    y2 = y1 + r.height;
    int sx1 = screen_left;
    int sy1 = screen_top;
    int sx2 = screen_left + screen_width;
    int sy2 = screen_top + screen_height;
    if (sx1 >= x2)
      return false;
    if (x1 >= sx2)
      return false;
    if (sy1 >= y2)
      return false;
    if (y1 >= sy2)
      return false;
    return true;
  }
}

