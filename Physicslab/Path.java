/*
  File: Path.java

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
/////////////////////////////////////////////////////////////////////////////
// CPath class
/*
  Represents the track of the rollercoaster as a table of values
  which map the path length of the track to specific x-y locations.

  Uses a parametric version of the function f(t) so that we can
  have loops.  We build a table representing the function by
  varying t from tLo to tHi.  The table stores triplets (p,x,y)
  where p = path length, and x,y are position.
  After the table is created, the function (and tLo, tHi) is no
  longer used at all during the simulation.  The slope is found
  numerically from the table.  We also need to know the direction
  of the curve (ie. as p increases, does x increase or decrease?).
*/
package com.myphysicslab.simlab;

import java.applet.*;
import java.awt.*;

class CPoint {
  public double x;
  public double y;
  public double p;
  public double slope = 0;
  public double radius = 0;
  public boolean radius_flag = false;  /* whether to calculate radius */
  public int direction;
  public int ball  = 0;  /* ball number... see p_index comments */

  CPoint() {}

  CPoint(int num) {
    ball = num;
  }
}

class C2Points {
  public double x1;
  public double y1;
  public int ball = 0;  /* ball number... see p_index comments */
}

class PathName {
  private final String name;
  private PathName(String name) { this.name = name; }
  public String toString() { return name; }
  public static final PathName LOOP = new PathName("loop");
  public static final PathName CIRCLE = new PathName("circle");
  public static final PathName FLAT = new PathName("flat");
  public static final PathName LEMNISCATE = new PathName("infinity");
  public static final PathName OVAL = new PathName("oval");
  public static final PathName SPIRAL = new PathName("spiral");
  public static final PathName HUMP = new PathName("hump");
  public static final PathName CARDIOID = new PathName("cardioid");

  public static PathName[] getPathNames() {
    PathName[] p = new PathName[7];
    p[0] = PathName.HUMP;
    p[1] = PathName.LOOP;
    p[2] = PathName.CIRCLE;
    p[3] = PathName.LEMNISCATE;
    p[4] = PathName.OVAL;
    p[5] = PathName.SPIRAL;
    p[6] = PathName.CARDIOID;
    return p;
  }
}


abstract class CPath implements Drawable {
  protected static final int DRAW_POINTS = 500;
  protected static final int DATA_POINTS = 9000;
  private static final int BALLS = 4;
  private double[] xvals;
  private double[] yvals;
  private double[] pvals;
  protected boolean closed = false;  // closed loop?
  protected double plen = 0; // length of path
  public double tLo = 0;  /* range of t values */
  public double tHi = 1;
  private PathName pathName;
  /* p_index is index into the position table.
    Its a global so that we start looking near the same spot in table we were at last time.
    Each slot of the array is for a particular ball located on the track. */
  private int[] p_index;
  private int[] x_index;
  public double left=0, top=1, right=1, bottom=0;  // suggested bounds for path
  public boolean exact_slope = false;  // whether we have an analytic expression for slope

  public static CPath makePath(PathName pName) {
    CPath p = null;
    if (pName==PathName.HUMP) p = new CPath_Hump();
    else if (pName==PathName.LOOP) p = new CPath_Loop();
    else if (pName==PathName.CIRCLE) p = new CPath_Circle();
    else if (pName==PathName.FLAT) p = new CPath_Flat();
    else if (pName==PathName.LEMNISCATE) p = new CPath_Lemniscate();
    else if (pName==PathName.OVAL) p = new CPath_Oval();
    else if (pName==PathName.SPIRAL) p = new CPath_Spiral();
    else if (pName==PathName.CARDIOID) p = new CPath_Cardioid();
    else
      throw new IllegalArgumentException("no such path "+pName);
    p.pathName = pName;
    return p;
  }

  CPath() {
    initialize();
    xvals = new double[DATA_POINTS];
    yvals = new double[DATA_POINTS];
    pvals = new double[DATA_POINTS];
    p_index = new int[BALLS];
    x_index = new int[BALLS];
    for (int i=0; i<BALLS; i++)  {
      p_index[i] = -1;  // don't know location at start
      x_index[i] = -1;
    }
    make_table();
  }

  public String toString() {
    return "Path "+this.pathName;
  }

  public double path_lo() {
    return pvals[0];
  }

  public double path_hi() {
    return pvals[DATA_POINTS-1];
  }

  public double modp(double p) {
    /* returns p mod path_length for closed loops */
    if (closed && ((p < 0) || (p > plen)))  {
      //double savp = p;
      p = p - plen*Math.floor(p/plen);
    }
    return p;
  }

  protected abstract void initialize();
  protected abstract double x_func(double t);
  protected abstract double y_func(double x);

  protected double my_path_func(double x) {
    throw new RuntimeException();
  }

  protected double slope(double p) {
    throw new RuntimeException();
  }

  public boolean off_track(double x) {
    if (closed)
      return false;
    else
      return ((x < xvals[0]) || (x > xvals[DATA_POINTS-1]));
  }

  public double off_track_adjust(double x) {
    if (x < xvals[0])
      x = xvals[0] + 0.1;
    if (x > xvals[DATA_POINTS-1])
      x = xvals[DATA_POINTS-1] - 0.1;
    return x;
  }

  private int binSearch(double arr[], double x, int guess)
  {
    /* given an array arr[0..n-1], and given a value x, binSearch() returns a value i
    such that x is between arr[i] and arr[i+1].
    arr[0..n-1] must be monotonic, either increasing or decreasing.
    i=-1 or i=n is returned to indicate that x is out of range.
    guess is taken as the initial guess for i.
    */
    int i, min, max;
    int n = arr.length;
    if (n<2)
      throw new IllegalArgumentException("array must have more than one element");
    boolean dir = arr[0] < arr[n-1];  // sort direction of array
    if (guess < 0)
      i = 0;
    else if (guess > 0)
      i = n-1;
    else
      i = guess;
    if (dir) {
      min = 0;
      max = n-1;
    } else {
      min = n-1;
      max = 0;
    }
    // deal with x being outside array first
    if (dir) {
      if (x < arr[0])
        return -1;
      if (x > arr[n-1])
        return n;
    } else {
      if (x < arr[n-1])
        return n;
      if (x > arr[0])
        return -1;
    }
    while (Math.abs(max - min) > 1) {
      if (x > arr[i]) {
        if (dir)
          min = i;
        else
          max = i;
      } else {
        if (dir)
          max = i;
        else
          min = i;
      }
      if (dir)
        i = min + (max - min)/2;
      else
        i = max + (min - max)/2;
    }
    return i;
  }

  private void make_table_old()
  {
    /* Create table of position (= path length) and x values. */
    /* x ranges from start to finish. */
    /* The function my_path_func is the integrand in the path integral:
      sqrt(1 + (dy/dx)^2)
      This is integrated in small pieces over the range of the function.
       to build up a table of path length vs. x value. */
    double delta, halfdelta, t, p;
    double p1, p2, p3;
    delta = (tHi-tLo)/(double)DATA_POINTS;
    halfdelta = delta/2;
    t = tLo;
    p = 0;  /* path length is always zero at start */
    for (int i=0;i<DATA_POINTS;i++) {
      xvals[i] = x_func(t);  /* write previous values */
      pvals[i] = p;
      yvals[i] = y_func(t);
      /* analytic solution for y=x^2 -- useful for testing: */
      //avals[i] = (x/2)*sqrtf(1+4*x*x)+(double)0.25*logf(2*x+sqrtf(4*x*x+1));
      /* simpson's quadrature integration rule is 1/3, 4/3, 1/3 */
      /* note: we can reduce this to two function evaluations easily */
      p1 = my_path_func(t)/3;
      p2 = 4*my_path_func(t + halfdelta)/3;
      p3 = my_path_func(t + delta)/3;
      p += halfdelta*(p1+p2+p3);
      t += delta;
    }
    plen = pvals[DATA_POINTS-1];
  }

  private void make_table()
  {
    /* Create table of x,y, and p (= path length). */
    double t, p, delta;
    double dx, dy;
    boolean warn = true;
    delta = (tHi-tLo)/(double)(DATA_POINTS-1);
    t = tLo;
    p = 0;  /* path length is always zero at start */
    pvals[0] = 0;
    xvals[0] = x_func(t);
    yvals[0] = y_func(t);
    int i = 1;
    do {
      t += delta;
      xvals[i] = x_func(t);  /* write previous values */
      yvals[i] = y_func(t);
      dx = xvals[i] - xvals[i-1];
      if (warn && (dx == 0))
      {
        System.out.println("track has a vertical section");
        warn = false;
      }
      dy = yvals[i] - yvals[i-1];
      // use distance between points for path length... crude but effective
      // (alternatively, could try to construct a curve through points for a better path length)
      p += Math.sqrt(dx*dx + dy*dy);
      pvals[i] = p;
    }
    while (++i < DATA_POINTS);
    plen = pvals[DATA_POINTS-1];
  }


  private double interp4(double xx[], double yy[], double x, int i)
  {
    /* returns the y-value corresponding to the x-value in the 4 point (3rd order)
      polynomial interpolant formed from the 4 values in xx and yy arrays
      at xx[i], xx[i+1], xx[i+2], xx[i+3] and similarly for yy.
      NOTE:  if we get the same indices into the table, we could cache the
      constants and reuse them... would need to pass index to table?
    */
    double c1,c2,c3,c4,y;
    /* See Intro to Scientific Computing by Van Loan, p. 77 */
    /* calculate the constants on the polynomial */
    if (i<0)
      i = 0;
    if (i>DATA_POINTS-4)
      i = DATA_POINTS-4;
    c1 = yy[i+0];
    c2 = (yy[i+1]-c1)/(xx[i+1]-xx[i+0]);
    c3 = (yy[i+2]- (c1 + c2*(xx[i+2]-xx[i+0]))) / ((xx[i+2]-xx[i+0])*(xx[i+2]-xx[i+1]));
    c4 = yy[i+3] - (c1 + c2*(xx[i+3]-xx[i+0]) + c3*(xx[i+3]-xx[i+0])*(xx[i+3]-xx[i+1]));
    c4 = c4 / ((xx[i+3]-xx[i+0])*(xx[i+3]-xx[i+1])*(xx[i+3]-xx[i+2]));
    /* Use Horner's rule for nested multiplication to evaluate the polynomial at x.
      see Van Loan, p. 80 */
    y = ((c4*(x-xx[i+2]) + c3)*(x-xx[i+1]) + c2)*(x-xx[i+0]) + c1;
    return y;
  }

  public void map_x(CPoint pt)
  {
    /* Table lookup & interpolate to map x -> y & p. */
    /* first find where x falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    x_index[pt.ball] = binSearch(xvals, pt.x, x_index[pt.ball]);
    int k = x_index[pt.ball];
    /* create and evaluate interpolant using 4 surrounding points */
    pt.y = interp4(xvals, yvals, pt.x, k-1);
    pt.p = interp4(xvals, pvals, pt.x, k-1);
  }

  public double map_x_to_y(double x, int ball)
  {
    /* Table lookup & interpolate to map x -> y. */
    /* first find where x falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    x_index[ball] = binSearch(xvals, x, x_index[ball]);
    /* create and evaluate interpolant using 4 surrounding points */
    return interp4(xvals, yvals, x, x_index[ball]-1);
  }

  public double map_x_to_p(double x, int ball)
  {
    /* Table lookup & interpolate to map p -> x. */
    /* first find where x falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    x_index[ball] = binSearch(xvals, x, x_index[ball]);
    /* take 4 datapoints starting at k & centered around j as best as possible */
    /* create and evaluate interpolant using 4 surrounding points */
    return interp4(xvals, pvals, x, x_index[ball]-1);
  }

  public double map_p_to_x(double p, int ball)
  {
    /* Table lookup & interpolate to map p -> x. */
    /* first find where p falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    p = modp(p);
    p_index[ball] = binSearch(pvals, p, p_index[ball]);
    /* take 4 datapoints starting at k & centered around j as best as possible */
    /* create and evaluate interpolant using 4 surrounding points */
    return interp4(pvals, xvals, p, p_index[ball]-1);
  }

  public double map_p_to_y(double p, int ball)
  {
    /* Table lookup & interpolate to map p -> x. */
    /* first find where p falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    p = modp(p);
    p_index[ball] = binSearch(pvals, p, p_index[ball]);
    /* take 4 datapoints starting at k & centered around j as best as possible */
    /* create and evaluate interpolant using 4 surrounding points */
    return interp4(pvals, yvals, p, p_index[ball]-1);
  }

  public double map_x_y_to_p(double x, double y)
  {
    /* Find the closest point on the curve to the given x,y position */
    /* For now, just do a straight search... improve later if necessary. */
    double best_len = Double.MAX_VALUE;
    double p = -999999999;
    double len, xd, yd;
    /* for each point in the table, check the distance */
    for (int i=0;i<DATA_POINTS;i++) {
      xd = x - xvals[i];
      yd = y - yvals[i];
      len = xd*xd + yd*yd;
      if (len < best_len) {
        best_len = len;
        p = pvals[i];
      }
    }
    return p;
  }

  public void closest_to_x_y(CPoint pt, double x, double y)
  {
    /* Find the closest point on the curve to the given x,y position */
    /* For now, just do a straight search... improve later if necessary. */
    double best_len = Double.MAX_VALUE;
    double len, xd, yd;
    /* for each point in the table, check the distance */
    for (int i=0;i<DATA_POINTS;i++) {
      xd = x - xvals[i];
      yd = y - yvals[i];
      len = xd*xd + yd*yd;
      if (len < best_len) {
        best_len = len;
        pt.x = xvals[i];
        pt.y = yvals[i];
        pt.p = pvals[i];
      }
    }

  }

  public void closest_slope(double x, double y, double p_guess, CPoint pt)
  {
    /* Find slope at the closest point on the curve to the given x,y position. */
    /* Start at position p_guess and only search while distance decreases */
    double len, xd, yd, dx, dy;
    p_guess = modp(p_guess);
    int i = binSearch(pvals, p_guess, p_index[pt.ball]);
    if (i<0) // off left hand edge
      i = 1;
    else if (i>DATA_POINTS-1)
      i = DATA_POINTS-2;
    else
    {
      xd = x - xvals[i];
      yd = y - yvals[i];
      double best_len = xd*xd + yd*yd;
      /* try to search up */
      while (i<DATA_POINTS-2)
      {
        xd = x - xvals[i+1];
        yd = y - yvals[i+1];
        len = xd*xd + yd*yd;
        if (len > best_len)
          break;
        i++;
      }
      /* search down */
      while (i>1)
      {
        xd = x - xvals[i-1];
        yd = y - yvals[i-1];
        len = xd*xd + yd*yd;
        if (len > best_len)
          break;
        i--;
      }
    }
    /* figure out slope */
    dx = xvals[i+1] - xvals[i-1];
    dy = yvals[i+1] - yvals[i-1];
    //System.out.println("intersection distance = "+best_len);
    pt.slope = dy/dx;
    if (dx == 0)
      System.out.println("**** infinite slope ****");
    pt.p = pvals[i];  /* very rough... better approximation possible by interpolation */
  }

  public void map_p_to_slope(CPoint pt)
  {
    /* INPUT:  pt.p and pt.ball number and pt.radius_flag */
    /* CALCULATES:  pt.slope, pt.direction, pt.x, pt.y, pt.radius */
    /* Table lookup & interpolate to map p -> slope. */
    /* Also returns "direction" of curve:
             left to right = +1, right to left = -1 */
    /* WARNING: result may be infinite slope! */
    int k;
    double dy, dx;
    /* first find where p falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    /* p will then be between pvals[p_index-1] and pvals[p_index]
       unless p_index = 0 or DATA_POINTS, in which case it is out of range. */
    pt.p = modp(pt.p);
    p_index[pt.ball] = binSearch(pvals, pt.p, p_index[pt.ball]);
    k = p_index[pt.ball];
    /* adjust index if at either end */
    if (k<0)
      k = 1;
    if (k >= DATA_POINTS-1)
      k = DATA_POINTS-2;
    /* find the corresponding x & y */
    /* take 4 datapoints starting at k-1 & centered around p_index */
    pt.x = interp4(pvals, xvals, pt.p, k-1);
    pt.y = interp4(pvals, yvals, pt.p, k-1);
    if (xvals[k+1] == xvals[k])  // vertical line is special case
    {
      // WARNING: not sure about this calculation of pt.direction... might depend on the
      // particulars of the track.
      pt.direction = (yvals[k+1] > yvals[k]) ? 1 : -1;
      if (exact_slope)
        pt.slope = slope(pt.p);
      else
        pt.slope = Double.POSITIVE_INFINITY;
      pt.radius = Double.POSITIVE_INFINITY;
    }
    else
    {
      /* figure out direction of curve:  left to right = +1, right to left = -1 */
      pt.direction = (xvals[k+1] > xvals[k]) ? 1 : -1;

        /* EXPERIMENT:  get slope from a polynomial fitted to nearest 4 points */
        /* NOTE:  this doesn't seem to smooth out the energy very much at all... */
        /* derivation of quadratic interpolant using Newton polynomials
           Let our three datapoints be (x1,y1), (x2,y2), (x3,y3), (x4,y4)
           Our polynomial will be
           p(x) = a1 + a2(x-x1) + a3(x-x1)(x-x2) + a4(x-x1)(x-x2)(x-x3)
           The first derivative is
           p'(x) = a2 + a3(2x-x1-x2) + a4((x-x2)(x-x3) + (x-x1)(2x-x2-x3))
           The coefficients are given by solving the system:
           a1 = y1
           a1 + a2(x2-x1) = y2
           a1 + a2(x3-x1) + a3(x3-x1)(x3-x2) = y3
           a1 + a2(x4-x1) + a3(x4-x1)(x4-x2) + a4(x4-x1)(x4-x2)(x4-x3) = y4
           Solving this system gives:
           a1 = y1
           a2 = (y2-y1)/(x2-x1)
           a3 = (y3 - y1 - a2(x3-x1))/((x3-x2)(x3-x1))
           a4 = (y4 - y1 - a2(x4-x1) - a3(x4-x1)(x4-x2))/((x4-x1)(x4-x2)(x4-x3))
        */
        /*
        int i = k;
        double a2,a3,a4;
        double x1,x2,x3,x4,y1,y2,y3,y4;
        double s;
        x1 = xvals[i-1]; x2 = xvals[i]; x3 = xvals[i+1]; x4 = xvals[i+2];
        y1 = yvals[i-1]; y2 = yvals[i]; y3 = yvals[i+1]; y4 = yvals[i+2];
        a2 = (y2-y1)/(x2-x1);
        a3 = (y3 - y1 - a2*(x3-x1))/((x3-x2)*(x3-x1));
        a4 = (y4 - y1 - a2*(x4-x1) - a3*(x4-x1)*(x4-x2))/((x4-x1)*(x4-x2)*(x4-x3));
        // plug in the desired x value into derivative to get the slope
        s = a2 + a3*(2*pt.x-x1-x2);
        s += a4*((pt.x-x2)*(pt.x-x3) + (pt.x-x1)*(2*pt.x-x2-x3));
        dx = xvals[k+1] - xvals[k];
        dy = yvals[k+1] - yvals[k];
        pt.slope = s;
        */
      if (exact_slope)
      {
        pt.slope = slope(pt.p);
        /*
          dx = xvals[k+1] - xvals[k];
          dy = yvals[k+1] - yvals[k];
        */
      }
      else
      {
        /* take 2 surrounding points and figure average slope from these */
        dx = xvals[k+1] - xvals[k];
        dy = yvals[k+1] - yvals[k];
        pt.slope = dy/dx;
      }

      if (pt.radius_flag)
      {
        /* assume straight-line (infinite radius) at end-points of track */
        // ??? or calculate the radius at the end-points???
        if ((k < 2) || (k > DATA_POINTS-4))
          pt.radius = Double.POSITIVE_INFINITY;
        else
        {
          /*  The radius of curvature of the track is given by reciprocal
            of kappa = |d phi / d s|  where
            phi = slope angle of curve = taninverse(dy/dx)
            s = arc length.
            Therefore, we get the slope at two points near p, and figure
            derivative of change in taninverse of slope.  */
          // Here is schematic of the values
          //      k-3   k-2   k-1    k    k+1   k+2   k+3    k+4  <- table indices
          //                            p                        <- p is here in table
          //            <---- p1 ---->
          //                               <---- p2 ---->
          // Let slopes at p1 & p2 be b1 & b2.
          // Then radius will be inverse of:  atan(b2) - atan(b1)/(p2-p1)

          dx = xvals[k] - xvals[k-2];
          dy = yvals[k] - yvals[k-2];
          double b1 = dy/dx;
          double p1 = pvals[k-1];  // ??? or should it be (pvals[k] + pvals[k-2])/2  ???

          dx = xvals[k+3] - xvals[k+1];
          dy = yvals[k+3] - yvals[k+1];
          double b2 = dy/dx;
          double p2 = pvals[k+2];
          pt.radius = (p2-p1)/(Math.atan(b2)-Math.atan(b1));
          //if (pt.radius < -0.5)
          //  pt.radius = pt.radius;
        }
      }
    }
  }

  public void find_intersect(C2Points pts, double ax, double ay, double qx, double qy)
  {
    /* finds the intersection point I of track & line between points A, Q
       and the reflected (bounced) point R */
    // if A is not the leftmost point, then swap them
    boolean flip = false;
    if (qx < ax) {
      double h;
      h = ax;
      ax = qx;
      qx = h;
      h = ay;
      ay = qy;
      qy = h;
      flip = true;
    }
    /* find where ax falls in the table */
    /* use the technique from numerical recipes which starts near last value found */
    x_index[pts.ball] = binSearch(xvals, ax, x_index[pts.ball]);
    int k = x_index[pts.ball];
    /* if out of range, we assume that a straight line continues the track
      from the endpoints, at same slope as last two points.
      To accomplish this, just set the x_index to first or last point.*/
    if (k < 0)
      k = 0;
    if (k >= DATA_POINTS)
      k = DATA_POINTS-1;

    // Here is schematic of the values (assume that A is to left of Q)
    //          ax
    //     k        k+1      k+2      k+3...          <- table indices
    //                                     qx
    // So we know that the point at pvals[k] is to the left of
    // the line joining A and Q.
    // Therefore we search to the right until we cross the AQ line.
    // The AQ line divides the plane into positive & negative halves.
    // Find out whether pvals[k] is in positive or negative half.
    // Slope of line AQ is:
    double slope2, x, y;
    if (ax == qx)
    {
      // Here the ball is dropping straight down.
      // So intersect the equation of the track with the straight down line.
      // Here is slope of the track.
      slope2 = (yvals[k+1] - yvals[k])/(xvals[k+1] - xvals[k]);
      // plug in ax for x into the equation for the track.
      x = ax;
      y = yvals[k] + slope2*(x - xvals[k]);
    }
    else
    {
      double slope1 = (qy - ay)/(qx - ax);
      // Find point on track corresponding to ax
      y = interp4(xvals, yvals, ax, k-1);

      // Is the track above or below the line at pvals[k]?
      if (y == ay)  // we are exactly at the intersection (this is unlikely)
      {
        x = ax;
        slope2 = (yvals[k] - yvals[k-1])/(xvals[k] - xvals[k-1]);
        System.out.println("exact intersection");
      }
      else
      {
        double traj_y = ay;  // the y-coord of the trajectory of the object
        boolean below = (y < traj_y);
        boolean below2;
        // Advance to the right until the above test changes.
        int i = k;
        do {
          i++;
          if (i > DATA_POINTS-1)  // we fell off the right-hand edge
          {
            i = DATA_POINTS-1;
            break;
          }
          // if the last point was to right of Q, and still no intersection, then trouble
          if (xvals[i-1] > qx)
          {
            if (i==1)  // we are beyond the left-hand edge
            {
              i = 1;
              break;
            }
            System.out.println("intersection trouble");
            return; // for debugging, so we can get back to caller
          }
          traj_y = ay + slope1*(xvals[i] - ax);
          below2 = (yvals[i] < traj_y);
        } while (below2 == below);

        // Now pvals[i-1] and pvals[i] are on opposite sides of the line.
        // Find slope of line connecting pvals[i-1] and pvals[i]
        slope2 = (yvals[i] - yvals[i-1])/(xvals[i] - xvals[i-1]);

        // Find the intersection by solving simultaneous equations.
        // Here are the two equations:
        // y = ay + slope*(x - ax)
        // y = yvals[i] + slope2*(x - xvals[i])
        // Solving for x gives:
        x = (-slope1*ax + slope2*xvals[i] + ay - yvals[i])/(slope2 - slope1);

        // Plug this in to either equation to get y.
        y = ay + slope1*(x - ax);
        /*
        double dist = Math.sqrt(((ax-x)*(ax-x)+(ay-y)*(ay-y)));
        if (dist < .00001)
        {
          System.out.println("intersect moved tiny distance = "+dist);
        }
        */
      }

    }
    // Here is the intersection point
    pts.x1 = x;  // pass back intersection point in structure
    pts.y1 = y;

  }

  public void draw(Graphics g, ConvertMap map)
  {
    /* Draw track based on data in tables.
      Assumes that the p-values (ie. distance along track) are increasing in the table.
      This version can handle a loop track. */
    int scrx, scry, oldx, oldy;
    double p_first, p_final;
    double p, p_prev, x, y;
    double delta;
    int p_index;

    p_first = pvals[0];
    p_final = pvals[DATA_POINTS-1];  /* the last point */
    if (p_final <= p_first)
      System.out.println("draw_track reports track data is out of order");
    /* translate adjusts the origin to match the screen.
       This is important when the layout moves the simulation area (the 'screen') to the right
       of the graph.  */
  //g.translate(-map.screen_left, -map.screen_top);  // comment out Jan 26 2004
    /* delta determines how finely the track is drawn */
    /* NOTE:  ideally we would plot more points where the track is more */
    /* curvy, ie. where second derivative is bigger */
    delta = (p_final - p_first)/DRAW_POINTS;
    p_index = 0;
    p_prev = pvals[p_index];
    /* find initial x,y values and move pen to that position */
    x = xvals[p_index];
    y = yvals[p_index];
    scrx = map.simToScreenX(x);
    scry = map.simToScreenY(y);
    // clear background to white
    g.setPaintMode();
    g.setColor(Color.white);
    Rectangle r = map.getScreenRect();
    g.fillRect(r.x, r.y, r.width, r.height);
    g.setColor(Color.black);

    do {
      do {    /* find the next p-value that is bigger by "delta" */
        p_index++;
        if (p_index > DATA_POINTS-1) {  /* we went off end of the list */
          p = p_final;   /* so choose the last point */
          p_index = DATA_POINTS-1;
          break;
        }
        else
          p = pvals[p_index];
      } while (p - p_prev < delta);
      p_prev = p;
      /* get the corresponding x,y values and draw a line to them */
      oldx = scrx;
      oldy = scry;
      x = xvals[p_index];
      y = yvals[p_index];
      scrx = map.simToScreenX(x);
      scry = map.simToScreenY(y);
      g.drawLine(oldx, oldy, scrx, scry);
    } while (p < p_final);  /* until we reach  the last point */
    /* code to draw crosshairs at the origin
      int orx, ory;
      orx = map.simToScreenX(0);
      ory = map.simToScreenY(0);
      g.drawLine(-20+orx, ory, 20+orx, ory);
      g.drawLine(orx, -20+ory, orx, 20+ory);
    */
  }
}

/////////////////////////////////////////////////////////////////////////////////////
class CPath_Hump extends CPath {
  public CPath_Hump() {
    super();
  }

  protected void initialize() {
    tLo = -4.5;
    tHi = 4.5;
    left = tLo; //3
    right = tHi;
    top = 6;
    bottom = 0.5;
  }

  protected double x_func(double t)  {
    /* returns x(t), ie. x as a function of t */
    return t;
  }

  protected double y_func(double x)  {
    /* the function defining the path */
    return 3 + x*x*(-7 + x*x)/6;
  }

  protected double my_path_func(double x) {
    /* this is the integrand in the path integral: sqrt(1 + (dy/dx)^2) */
    double d = x*(-14+4*x*x)/6;  // derivative of f(x)
    return Math.sqrt(1+d*d);
  }

}


/////////////////////////////////////////////////////////////////////////////////////
// CPath_Loop
/* loop curve */
/* formed from part of a parabola, then part of a circle, then another parabola */
/* for details see Mathematica file roller.nb */

class CPath_Loop extends CPath
{
  private static final double theta1 = 3.46334;
  private static final double theta2 = -0.321751;
  private static final double radius = 0.527046;
  private static final double ycenter = 2.41667;
  private static final double xcenter = 0;
  private static final double yoffset = 1;

  public CPath_Loop()  {
    super();
  }

  protected void initialize() {
    tLo = -4;
    tHi = 8.5;
    left = -3;
    right = 3;
    top = 6;
    bottom = 0.5;
  }

  protected double x_func(double t)  {
    if (t<0.5)
      return t;
    else if (t < 0.5 + theta1 - theta2)
      return radius * Math.cos(t - 0.5 + theta2) + xcenter;
    else
      return t - theta1 + theta2 - 1;
  }

  protected double y_func(double t)  {
    if (t<0.5)
      return (t+1)*(t+1) + yoffset;
    else if (t < 0.5 + theta1 - theta2)
      return radius * Math.sin(t - 0.5 + theta2) + ycenter + yoffset;
    else {
      double dd = t - theta1 + theta2 - 2;
      return dd*dd + yoffset;
    }
  }

  protected double my_path_func(double t)  {
    /* this is the integrand in the path integral: sqrt((dx/dt)^2 + (dy/dt)^2) */
    double dx,dy;
    if (t<0.5) {
      dx =1;
      dy = 2*(t+1);
    } else if (t < 0.5 + theta1 - theta2) {
      dx = -radius * Math.sin(t - 0.5 + theta2);
      dy = radius * Math.cos(t - 0.5 + theta2);
    } else {
      dx = 1;
      dy = 2*(t - theta1 + theta2 - 2);
    }
    return Math.sqrt(dx*dx + dy*dy);
  }

}

/////////////////////////////////////////////////////////////////////////////////////
/* This path looks like an oval racetrack.  The straight sections are vertical,
   so it is a good test for handling infinite slope situations.
 */

class CPath_Oval extends CPath
{
  private static final double s = 2;
  private static final double t0 = Math.PI/2; // top of upper arc
  private static final double t1 = Math.PI;  // left end of upper arc
  private static final double t2 = t1 + s;  // bottom of left vertical line
  private static final double t3 = t2 + Math.PI; // right end of lower arc
  private static final double t4 = t3 + s; // top of right vertical line
  private static final double t5 = t4 + Math.PI/2;  // top of upper arc

  public CPath_Oval()  {
    super();
  }

  protected void initialize() {
    tLo = t0;
    tHi = t5;
    closed = true;
    double b = 1.5;
    left = -b;
    right = b;
    top = b+s;
    bottom = -b;
  }

  protected double x_func(double t)  {
    if (t<t1)
      return Math.cos(t);
    else if (t<t2)
      return -1;
    else if (t< t3)
      return Math.cos(Math.PI + t-t2);
    else if (t< t4)
      return 1;
    else if (t<t5)
      return Math.cos(t-t4);
    else
      return 0;
  }

  protected double y_func(double t)  {
    if (t<t1)
      return s+Math.sin(t);
    else if (t<t2)
      return s - (t-t1);
    else if (t< t3)
      return Math.sin(Math.PI + t-t2);
    else if (t< t4)
      return t-t3;
    else if (t<t5)
      return s + Math.sin(t-t4);
    else
      return 0;
  }
}

/////////////////////////////////////////////////////////////////////////////////////
/* closed loop curve */

class CPath_Circle extends CPath {
  final double radius = 1.5;
  final double edgeBuffer = 0.5;

  public CPath_Circle()  {
    super();
  }

  protected void initialize() {
    tLo = -3*Math.PI/2;
    tHi = Math.PI/2;
    closed = true;
    double b = radius+edgeBuffer;
    left = -b;
    right = b;
    top = b;
    bottom = -b;
    exact_slope = false;
  }

  protected double x_func(double t)  {
    return radius*Math.cos(t);
  }

  protected double y_func(double t)  {
    return radius*Math.sin(t);
  }

  /*
  protected double slope(double p)  {
    // NOTE: also need to fix the direction calculation to be exact!!!
    return -1/Math.tan(p - 3*Math.PI/2);
  }
  */
}

/////////////////////////////////////////////////////////////////////////////////////
/* Lemniscate curve... a "figure eight"
  Equation in polar coords is:
     2       2
    r  =  2 a  cos(2t)

    r = (+/-) a Sqrt(2 cos(2t))

  where a=constant, t=angle from -Pi/4 to Pi/4, and r=radius

  To get both lobes with the direction of travel increasing across the origin, define
    T = -t + Pi/2
  Then
    r = a Sqrt(2 cos(2t))   for -Pi/4 < t < Pi/4
    r = -a Sqrt(2 cos(2T))   for Pi/4 < t < 3 Pi/4

  To get into Cartesian coords, we use
    x = r cos(t)
    y = r sin(t)
 */


class CPath_Lemniscate extends CPath {
  private static final double a = 1.5;

  public CPath_Lemniscate()  {
    super();
  }

  protected void initialize() {
    tLo = -Math.PI/4;
    tHi = 3*Math.PI/4;
    closed = true;
    //plen = 2*Math.PI;
    final double bx = 3;
    final double by = 1.5;
    left = -bx;
    right = bx;
    top = by;
    bottom = -by;
  }

  protected double x_func(double t)  {
    if (t<=Math.PI/4)
      return a*Math.sqrt(2*Math.cos(2*t))*Math.cos(t);
    else if (t<=3*Math.PI/4) {
      double T = -t + Math.PI/2;
      return -a*Math.sqrt(2*Math.cos(2*T))*Math.cos(T);
    }
    else
      return 0;
  }

  protected double y_func(double t)  {
    if (t<=Math.PI/4)
      return a*Math.sqrt(2*Math.cos(2*t))*Math.sin(t);
    else if (t<=3*Math.PI/4) {
      double T = -t + Math.PI/2;
      return -a*Math.sqrt(2*Math.cos(2*T))*Math.sin(T);
    }
    else
      return 0;
  }

  protected double my_path_func(double t)  {
    /* this is the integrand in the path integral: sqrt((dx/dt)^2 + (dy/dt)^2)
       Using Mathematica, this is
              2
      sqrt(2 a  Sec(2 t))

      When we go over to the negative branch we can use the same formula because
      of symmetry.
    */
    //if (t>Math.PI/4)
    //  t -= Math.PI/2;
    //return a*Math.sqrt(2/Math.cos(2*t));
    throw new RuntimeException();
    //return 0;  // currently not using this!
  }

}
/////////////////////////////////////////////////////////////////////////////////////
/*
  Cardioid:
  r = a (1 - cos theta)

  x = a cos t (1 + cos t)
  y = a sin t (1 + cos t)

  or interchange x-y to rotate by 90 degrees.
*/
class CPath_Cardioid extends CPath {
  private static final double a = 1.5;

  public CPath_Cardioid()  {
    super();
  }

  protected void initialize() {
    tLo = 0;
    tHi = 2*Math.PI;
    closed = true;
    //plen = 2*Math.PI;
    final double bx = 2.8;
    final double by = 3;
    left = -bx;
    right = bx;
    top = 1.0;
    bottom = -3.5;
  }

  protected double x_func(double t)  {
    double c = Math.cos(t);
    return a*Math.sin(t)*(1+c);
  }

  protected double y_func(double t)  {
    double c = Math.cos(t);
    return -a*c*(1+c);
  }

}


/////////////////////////////////////////////////////////////////////////////////////

class CPath_Flat extends CPath {

  public CPath_Flat() {
    super();
  }

  protected void initialize() {
    tLo = -8;
    tHi = 8;
    left = -3;
    right = 3;
    top = 6;
    bottom = 0.5;
  }

  protected double x_func(double t)  {
    return t;
  }

  protected double y_func(double t) {
    return 1;
  }

  protected double my_path_func(double t)  {
    /* this is the integrand in the path integral: sqrt((dx/dt)^2 + (dy/dt)^2) */
    return 1;
  }

}

/////////////////////////////////////////////////////////////////////////////////////
/* Spiral path.
  See the Mathematica file Rollercurves.nb for construction.

 */

class CPath_Spiral extends CPath {
  private static final double arc1x = -2.50287; // center of upper arc
  private static final double arc1y = 5.67378;
  private static final double rad = 1; // radius of the arcs
  private static final double slo = 4.91318; // t value at inside of spiral
  private static final double slox = 0.122489;  // inside point of spiral
  private static final double sloy = -0.601809;
  private static final double shi = 25.9566; // t value at outside of spiral
  private static final double shix = 2.20424;  // outside point of spiral
  private static final double shiy = 2.38089;
  private static final double arc2y = sloy + rad; // center of lower arc
  private static final double arc1rx = arc1x + Math.cos(Math.PI/4); // right point of upper arc
  private static final double t1 = Math.PI/2;  // end of upper arc
  private static final double t2 = t1 + arc1y - arc2y; // end of left vertical line
  private static final double t3 = t2 + Math.PI/2;  // end of lower arc
  private static final double t4 = t3 + slox - arc1x;  // end of horiz line, start of spiral
  private static final double t5 = t4 + shi - slo;  // end of spiral
  private static final double t6 = t5 + Math.sqrt(2)*(shix-arc1rx); // end of diagonal line
  private static final double t7 = t6 + Math.PI/4;  // top of upper arc

  public CPath_Spiral() {
    super();
  }

  protected void initialize() {
    tLo = 0;
    tHi = t7;
    closed = true;
    left = -4;
    right =4;
    top = 7;
    bottom = -4;
  }

  protected double x_func(double t)  {
    if (t < t1)  // upper arc
      return Math.cos(t + Math.PI/2) + arc1x;
    else if (t < t2)  // left vertical line
      return arc1x - rad;
    else if (t < t3)  // lower arc
      return Math.cos(t-t2+Math.PI) + arc1x;
    else if (t < t4)  // end of horiz line
      return arc1x + (t-t3);
    else if (t < t5)  // end of spiral
      return ((t-t4+slo)/8)*Math.cos(t-t4+slo);
    else if (t < t6)  // end of diagonal line
      return shix - (t-t5)/Math.sqrt(2);
    else if (t < t7)
      return arc1x + Math.cos(Math.PI/4 + t-t6);
    else
      return 0;
  }

  protected double y_func(double t)  {
    if (t < t1)  // upper arc
      return Math.sin(t + Math.PI/2) + arc1y;
    else if (t < t2)  // left vertical line
      return arc1y - (t-t1);
    else if (t < t3)  // lower arc
      return Math.sin(t-t2+Math.PI) + arc2y;
    else if (t < t4)  // end of horiz line
      return sloy;
    else if (t < t5)  // end of spiral
      return ((t-t4+slo)/8)*Math.sin(t-t4+slo);
    else if (t < t6)  // end of diagonal line
      return shiy + (t-t5)/Math.sqrt(2);
    else if (t < t7)
      return arc1y + Math.sin(Math.PI/4 + t-t6);
    else
      return 0;
  }
}

