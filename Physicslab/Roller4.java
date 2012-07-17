/*
  File: Roller4.java

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
package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;

/////////////////////////////////////////////////////////////////////////////
// CRoller4 class
//
// Rollercoaster -- ball that can jump off track
//
/*
  See website www.MyPhysicsLab.com for additional explanations
  of the math involved here.

  This version has the ball jump off the track when appropriate.
  The acceleration for uniform circular motion is a = v^2/r

  Suppose the ball is going over a hill.  The minimum radial
  acceleration to keep the ball on the track is v^2/r, where r =
  radius of curvature of the track (at the point where the ball
  currently is).  The actual acceleration normal to the track is given
  by the component of gravity (and other forces, eg. spring) that are
  normal to the track.  If the actual acceleration is less than the
  minimum acceleration then the ball leaves the track.

  The radius of curvature of the track is given by reciprocal
  of kappa = |d phi / d s|  where
  phi = slope angle of curve = taninverse(dy/dx)
  s = arc length.
  Another way to get it is:
    kappa = |d^2 y / d x^2|
            ---------------
           (1 + (dy/dx)^2)^(3/2)

  When the ball leaves the track, we have a different diff eq that
  takes over, which will be free flight if no spring or similar to the
  single 2D spring simulation. We also have a different set of
  variables depending on whether we are on or off the track.

  On the track we have 2 variables plus the 'track/free mode' flag:
    vars[0] = arc length = p
    vars[1] = velocity on track
    vars[2] unused
    vars[3] unused
    vars[4] = track or free mode (0 or 1)
  The diff eqs are the same as in Roller2.

  Off the track let U = position, V = velocity
    vars[0] = Ux
    vars[1] = Uy
    vars[2] = Vx
    vars[3] = Vy
    vars[4] = track or free mode (0 or 1)
  The diff eqns are the same as in Spring2DSim.

  Collisions:  it is simple to detect collisions, just see if the ball
  is below the track.  (We require that the track has no loops, so
  each x has a single point of the track above or below it). To handle
  the collision, we reflect the velocity in the tangent of the curve.

  TO DO:
  o To have a track with loops and vertical lines we need to have a
    way to determine what is inside or outside, or which direction the
    track inhibits movement. Need to deal with track sections that are
    vertical lines (infinite slope) & straight lines (infinite
    radius).

	MODIFICATION HISTORY:
	Oct 10 2006:  changed modifyObjects() to ensure that ball is not below track when
	switching from track to free flight.
  */

public class Roller4 extends Roller2
{
  protected static final double TRACK = 0;
  protected static final double FREE = 1;
  protected static final int MODE = 4;
  private Vector collisions = new Vector(10);
  private double stickiness = 0.1;  // stickiness of track, determines when ball jumps on
  protected static final String ELASTICITY = "elasticity",
                STICKINESS = "stickiness";

  public Roller4(Container app) {
    super(app, 0);  // can only deal with non-looped path for now.
  }

  public void setupControls() {
    super.setupControls();
    removeObserverControl(pathControl);  // don't allow path to be changed.
    addObserverControl(new DoubleField(this, ELASTICITY, 3));
    addObserverControl(new DoubleField(this, STICKINESS, 3));
  }

  public void setupGraph() {
    // Because the variables change between track & free flight,
    // the graph would get confused... so disallow graphing entirely.
  }

  protected void createElements() {
    super.createElements();
    // switch to 5 variables, including one for the sim_mode
    double p = vars[0];  // save starting point calculated by super class
    vars = new double[5];
    vars[0] = p;
    vars[2] = vars[3] = 0;
    vars[MODE] = TRACK;
    calc = new boolean[5];
    for (int i=0; i<calc.length; i++)
      calc[i] = true;
    calc[MODE] = false;  // never let diff eq change the sim_mode

    m_Point.radius_flag = true;  /* we want radius calculated */
    double x = -4;
    double y = 4;
    m_TopMass.setCenterX(x);
    m_TopMass.setCenterY(y);
    m_Spring.setX1(x);
    m_Spring.setY1(y);
    m_Spring.m_SpringConst = 0;
    m_Mass1.m_Color = Color.blue;  // because we are in track mode
    m_Mass1.m_Elasticity = 0.8;
    gravity = 10;
    cvs.expandMap();  // use full available screen area?
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(ELASTICITY))
      {m_Mass1.m_Elasticity = value; return true;}
    else if (name.equalsIgnoreCase(STICKINESS)) {
      // stickiness = 0 leads to insanity, so prevent it here
      if (value < 0.001)
        value = 0.001;
      if (value > 1)
        value = 1;
      stickiness = value; return true;
    }
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(ELASTICITY))
      return m_Mass1.m_Elasticity;
    else if (name.equalsIgnoreCase(STICKINESS))
      return stickiness;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    String[] params = {MASS, DAMPING, GRAVITY, SHOW_ENERGY, STIFF, LENGTH,
    ELASTICITY, STICKINESS};
    return params;
  }

  protected double getEnergy()  {
    // WARNING:  assumes that current x-y position of m_Mass1 & m_Spring is correct!
    // kinetic energy is 1/2 m v^2
    double v = (vars[MODE] == TRACK) ? vars[1]*vars[1] : vars[2]*vars[2]+vars[3]*vars[3];
    double e = 0.5*m_Mass1.m_Mass*v;
    // gravity potential = m g y
    e += m_Mass1.m_Mass*gravity*m_Mass1.getCenterY();
    // spring potential energy = 0.5*stiffness*(stretch^2)
    e += m_Spring.getEnergy();
    return e;
  }

  public void startDrag(Dragable e)  {
    if (e==m_Mass1)
      for (int i=0; i<calc.length; i++)
        calc[i] = false;
  }

  public void finishDrag(Dragable e) {
    super.finishDrag(e);
    calc[MODE] = false;  // don't allow diff eq to change the track/free mode.
  }

  // synchronized because changing sim_mode has to happen completely, otherwise
  // you wind up with code in another thread looking for 4 vars when there are only 2!
  public synchronized void constrainedSet(Dragable e, double x, double y) {
    if (e==m_TopMass)
      super.constrainedSet(e, x, y);
    else if (e==m_Mass1)  {
       // x,y correspond to the new m_X1, m_Y1 of the object
       // We want to work with the center of the object,
       // so adjust to xx,yy as follows.
      double w = m_Mass1.m_Width/2;
      x += w;
      y += w;
      // are we within the x-range of the track?
      if (m_Path.off_track(x)) {
        // find nearest point on track
        vars[MODE] = TRACK;
        m_Path.closest_to_x_y(m_Point, x, y);
        vars[0] = m_Point.p;
        vars[1] = 0;
        m_Mass1.m_Color = Color.blue;
      } else {  // we are within x-range
        // if below the track, then find closest point on the track
        if (y < m_Path.map_x_to_y(x, 0)) {
          vars[MODE] = TRACK;
          m_Path.closest_to_x_y(m_Point, x, y);
          vars[0] = m_Point.p;
          vars[1] = 0;
          m_Mass1.m_Color = Color.blue;
        } else {
          // above track, so FREE mode.
          vars[MODE] = FREE;
          vars[0] = x;
          vars[1] = y;
          vars[2] = 0;
          vars[3] = 0;
          m_Mass1.m_Color = Color.red;
        }
      }
    }
  }

  public void modifyObjects() {
    // switch mode from track to free-flying if appropriate.
    if (vars[MODE] == TRACK) {
      m_Point.p = vars[0];
      m_Path.map_p_to_slope(m_Point);
      double x = m_Point.x;
      double y = m_Point.y;

      m_Mass1.setCenterX(x);
      m_Mass1.setCenterY(y);
      m_Spring.setX2(x);
      m_Spring.setY2(y);

      // Compare the circular acceleration a = v^2/r to the actual
      // acceleration from gravity and spring that is normal to track.
      // If not enough to hold ball on track, then switch to free flight.
      double r = m_Point.radius;
      //NOTE: should check for infinite radius, but for now assume not possible
      // the accel from gravity, normal to track, is g*sin theta
      // where theta = angle between tangent vector & gravity vector
      // (see Mathematica file for derivation)
      int direction = m_Point.direction;
      double k = m_Point.slope;
      double slopeDenom = Math.sqrt(1+k*k);
      //NOTE: should check for infinite slope, but for now assume not possible
      double g = gravity / slopeDenom;
      // for positive curvature, gravity decreases radial accel
      if (r>0)
        g = -g;
      double ar = g;  // ar = radial acceleration

      // Need to figure out sign based on whether spring endpoint
      // is above or below the tangent line.
      // Tangent line is defined by: y = k*x + b.
      double b = y - k*x;
      int below = (m_Spring.m_Y1 < k*m_Spring.m_X1 + b) ? 1 : -1;
      // Add in the normal component of spring force
      // it's similar to tangent calculation in diff eq, except its sin(theta).
      // Let sx, sy be the x & y components of the spring length.
      double sx = m_Spring.m_X1 - x;
      double sy = m_Spring.m_Y1 - y;
      double slen = Math.sqrt(sx*sx + sy*sy); // spring length
      // we'll get sin theta from cos theta
      double costh = direction*(sx + k*sy)/slen;
      costh = costh / slopeDenom;
      double sinth = Math.sqrt(1 - costh*costh);
      if (sinth > 1 || sinth < 0)
        System.out.println("problem in roller4:doModifyObjects");
      // stretch amount of spring is
      double stretch = slen - m_Spring.m_RestLength;
      // Component due to spring is
      double as = (sinth*stretch*m_Spring.m_SpringConst)/m_Mass1.m_Mass;
      // (assume spring is stretched)
      // if negative curve, being below tangent increases ar
      if (r<0)
        ar = ar + below*as;
      else  // if positive curve, being below tangent decreases ar
        ar = ar - below*as;

      double v = vars[1];  // velocity
      double av = v*v/r;
      if (av<0)
        av = -av;  // use absolute value
      // to switch to free flight:
      // for negative curvature, must have ar < av
      // for positive curvature, must have ar > av
      if (r<0 && ar < av || r>0 && ar > av) {    /* switch to free flight */
        vars[MODE] = FREE;
        vars[0] = x;
        vars[1] = y;
	      m_Point.x = vars[0];
	      m_Path.map_x(m_Point);
	      // ball must not be below the track
	      if (vars[1] < m_Point.y) {
					//System.out.println("ball is below track by "+(m_Point.y - vars[1]));
					vars[1] = m_Point.y;
				}
        m_Mass1.m_Color = Color.red;
        // the magnitude of current velocity is v, direction
        //  is given by the slope
        // if you make a right triangle with horiz = 1, vert = k,
        //  then diagonal is sqrt(1+k^2).  Then divide each side
        //  by sqrt(1+k^2) and you get horiz = 1/sqrt(1+k^2) and
        //  vert = k/sqrt(1+k^2)
        vars[2] = v/slopeDenom;  // x-component of velocity
        vars[3] = v*k/slopeDenom;  // y-component of velocity
      }
    } else if (vars[MODE] == FREE) {
      m_Mass1.setCenterX(vars[0]);
      m_Mass1.setCenterY(vars[1]);
      m_Spring.setX2(vars[0]);
      m_Spring.setY2(vars[1]);
    }
    m_Text.setNumber(getEnergy());
  }

  /* Off the track let U = position of mass, V = velocity vector
     S = position of spring's other endpoint
    vars[0] = Ux
    vars[1] = Uy
    vars[2] = Vx
    vars[3] = Vy
    vars[4] = sim_mode (0=TRACK, 1=FREE)
    Ux' = Vx
    Uy' = Vy
    Vx' = 0
    Vy' = -g
  */
  public void evaluate(double[] x, double[] change) {
    if (x[MODE]==TRACK) {
      super.evaluate(x, change);
      // x[2] & x[3] don't actually exist now
      // x[MODE] is the sim_mode
      change[2] = change[3] = change[MODE] = 0;
    } else {  // free flight mode... forces are from spring & gravity
      change[0] = x[2];  // Ux' = Vx
      change[1] = x[3];  // Uy' = Vy

      //xx = Ux - Sx
      //yy = Uy - Sy
      //len = Sqrt(xx^2+yy^2)
      double xx = x[0] - m_Spring.m_X1;
      double yy = x[1] - m_Spring.m_Y1;
      double len = Math.sqrt(xx*xx + yy*yy);
      double m = m_Mass1.m_Mass;
      //L = len - R
      //sin(th) = xx / len
      //Vx' = -(k/m)L sin(th)
      change[2] = -(m_Spring.m_SpringConst/m)*(len - m_Spring.m_RestLength) * xx / len;
      // damping:  - (b/m) Vx
      change[2] -= (m_Mass1.m_Damping/m)*x[2];

      //L = len - R
      //cos(th) = yy / len
      //Vy' = -g - (k/m)L cos(th)
      change[3] = -gravity - (m_Spring.m_SpringConst/m)*(len - m_Spring.m_RestLength) * yy / len;
      // damping:  - (b/m) Vy
      change[3] -= (m_Mass1.m_Damping/m)*x[3];
      change[MODE] = 0;
    }
  }

  // returns a vector with list of collisions, or null if no collisions.
  public Vector findAllCollisions() {
    collisions.removeAllElements();  // forget any previous value
    if (vars[MODE] == FREE) {
      // Assume that the track does NOT LOOP for now....
      // Then if the ball is below the track there has been a collision.
      m_Point.x = vars[0];
      m_Path.map_x(m_Point);
      // ball is below the track implies a collision occurred
      if (vars[1] < m_Point.y)
        collisions.addElement(new Double(m_Point.p));
    }
    return (collisions.size() > 0) ? collisions : null;
  }

  // Adjust simulation for the given collisions.
  // For example, reverse velocities of objects colliding against a wall.
  public void handleCollisions(Vector collisions) {
    if (collisions.size()==0 || vars[MODE] == TRACK)
      return;

    // Find slope at closest point on track.
    // Use the point of the collision as a starting guess.
    Double pt = (Double)collisions.firstElement();
    m_Path.closest_slope(vars[0], vars[1], pt.doubleValue(), m_Point);
    double k = m_Point.slope;

    /* End Of Track Cludge:
       Beyond ends of track we just want something that doesn't crash!
       Otherwise have to have a whole separate model for the track
       extension, including calculating distance along the track...
       its just too much mess.
    */
    if (m_Path.off_track(vars[0])) {
      vars[0] = m_Path.off_track_adjust(vars[0]);  // put ball back in-bounds
      vars[2] = 0;  // set velocity to zero
      vars[3] = 0;
    } else {
      /* modify the velocities according to track geometry */
      /* From Vector Algebra:
        Let B = (1,k) be vector of line with slope k
        Let A = (vx, vy) be vector of velocity
                (A·B)
        Let C = ----- B  = component of A in B direction
                (B·B)

        Let N = A - C = component of A normal to B

        Then the reflection of A across the line B = C - N

        But we multiply the normal by the elasticity e, so
        result = C - e*N
      */
      double cx, cy, d;
      double vx = vars[2];
      double vy = vars[3];

      /* Find C = velocity component in track direction */
      d = (vx + k*vy)/(1 + k*k);
      cx = d;
      cy = d*k;
      /* Find N = A - C = velocity normal to track */
      double nx, ny;  // normal velocity
      nx = vx - cx;
      ny = vy - cy;
      double rx, ry;  // result velocity
      /* Result = C - e*N */
      rx = cx - m_Mass1.m_Elasticity*nx;
      ry = cy - m_Mass1.m_Elasticity*ny;
      vars[2] = rx;
      vars[3] = ry;

      double nv = m_Mass1.m_Elasticity*Math.sqrt(nx*nx + ny*ny);
      double rv = Math.sqrt(rx*rx + ry*ry);
      // BUG note: if bouncing straight up and down on a flat surface, then nv = rv
      //   and nv/rv = 1 no matter how small nv becomes... so maybe add an absolute value test too?
      // Switch to Track mode when velocity is small.
      if (nv/rv < stickiness) { // normal velocity is small compared to total velocity
        vars[MODE] = TRACK;
        vars[0] = m_Point.p;
        vars[1] = Math.sqrt(cx*cx + cy*cy) * (cx > 0 ? 1 : -1);
        m_Mass1.m_Color = Color.blue;
      }
    }
  }

}
