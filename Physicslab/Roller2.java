/*
  File: Roller2.java

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

// CRoller2 class
//
// Rollercoaster -- ball along a curved track, with spring.
//
/*
  See Roller1 for description, or Mathematica file roller.nb.

  Note on an interesting bug that came up here (and has been fixed):
    SYMPTOM: simulation goes haywire without damping (oscillations increase).
    REASON is that the diff eq includes spring length in its calculation
    which depends on position of the mass.  which is one of the variables
    in the RK calculation.  Because the spring length is NOT adjusted
    in the RK calculation (which spans time) it is constant for the RK
    calculation, hence wrong!
    SOLUTION:  rewrite the spring part of the diffeq to use position
      of mass  (and then calculate spring length based on that).
    MORAL:  be careful in the diff eq about hidden dependency on variables.

  */
public class Roller2 extends Roller1 {
  protected CMass m_TopMass;
  protected CSpring m_Spring;
  protected static final String STIFF = "spring stiffness",
                LENGTH = "spring rest length";

  public Roller2(Container app, int the_path)  {
    super(app, the_path);
  }

  public void setupControls() {
    super.setupControls();
    addObserverControl(new DoubleField(this, LENGTH, 2));
    addObserverControl(new DoubleField(this, STIFF, 2));
  }

  protected void createElements() {
    super.createElements();
    m_Spring = new CSpring (1, 1, 1, 0.5); // x1, y1, restLen, thickness
    m_Spring.m_SpringConst=5;
    cvs.addElement(m_Spring);

    m_TopMass = new CMass(0, 0, 0.5, 0.5, CElement.MODE_RECT);
    cvs.addElement(m_TopMass);
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(LENGTH))
      {m_Spring.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(STIFF))
      {m_Spring.m_SpringConst = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(LENGTH))
      return m_Spring.m_RestLength;
    else if (name.equalsIgnoreCase(STIFF))
      return m_Spring.m_SpringConst;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    String[] params = {MASS, DAMPING, GRAVITY, PATH, SHOW_ENERGY, STIFF, LENGTH};
    return params;
  }

  protected void set_path(int the_path)  {
    super.set_path(the_path);
    // Find starting position for TopMass
    double xx, yy;
    if (m_Path.closed) {
      xx = m_Path.left + 0.05*(m_Path.right - m_Path.left);
      yy = m_Path.bottom + 0.1*(m_Path.top - m_Path.bottom);
    } else {
      xx = m_Path.left + 0.3*(m_Path.right - m_Path.left);
      yy = m_Path.bottom + 0.5*(m_Path.top - m_Path.bottom);
    }
    m_Spring.setX1(xx);
    m_Spring.setY1(yy);
    m_TopMass.setCenterX(xx);
    m_TopMass.setCenterY(yy);
  }

  public void modifyObjects() {
    vars[0] = m_Path.modp(vars[0]);
    m_Point.p = vars[0];
    m_Path.map_p_to_slope(m_Point);
    m_Mass1.setCenterX(m_Point.x);
    m_Mass1.setCenterY(m_Point.y);
    m_Spring.setX2(m_Point.x);
    m_Spring.setY2(m_Point.y);
    // NOTE: because the energy calculation depends on the spring being updated,
    // we can't call the superClass version of modifyObjects.
    m_Text.setNumber(getEnergy());
  }

  protected double getEnergy()  {
    // WARNING:  assumes that current x-y position of m_Mass1 & m_Spring is correct!
    double e = super.getEnergy();
    // spring potential energy = 0.5*stiffness*(stretch^2)
    e += m_Spring.getEnergy();
    return e;
  }

  public void constrainedSet(Dragable e, double x, double y) {
    if (e==m_TopMass)  {
      // use center of mass instead of topLeft
      double w = m_TopMass.m_Width/2;
      x += w;
      y += w;

      DoubleRect r = cvs.getSimBounds();
      double L = r.getXMin() + w;
      double R = r.getXMax() - w;
      double B = r.getYMin() + w;
      double T = r.getYMax() - w;

      // disallow drag outside of window
      if (x < L)  x = L + 0.0001;
      if (x > R)  x = R - 0.0001;
      if (y < B)  y = B + 0.0001;
      if (y > T)  y = T - 0.0001;

      m_TopMass.setCenterX(x);
      m_TopMass.setCenterY(y);
      // force the spring to follow along
      m_Spring.setX1(x);
      m_Spring.setY1(y);
    } else if (e==m_Mass1)  {
       // x,y correspond to the new m_X1, m_Y1 of the object
       // We want to work with the center of the object,
       // so adjust to xx,yy as follows.
      double w = m_Mass1.m_Width/2;
      vars[0] = m_Path.map_x_y_to_p(x + w, y + w);
      vars[1] = 0;  // velocity
      modifyObjects();
    }
  }

  public void evaluate(double[] x, double[] change) {
    change[0] = x[1];  // p' = v
    // calculate the slope at the given arc-length position on the curve
    // x[0] is p = path length position.  xval is the corresponding x value.
    m_Point.p = x[0];
    m_Path.map_p_to_slope(m_Point);
    double k = m_Point.slope;
    // see Mathematica file "roller.nb" for derivation of the following
    // let k = slope of curve. Then sin(theta) = k/sqrt(1+k^2)
    // Component due to gravity is v' = - g sin(theta) = - g k/sqrt(1+k^2)
    double sinTheta = Double.isInfinite(k) ? 1 : k/Math.sqrt(1+k*k);
    change[1] = -gravity*m_Point.direction*sinTheta;
    // add friction damping:  - (b/m)*x[1]
    change[1] -= m_Mass1.m_Damping*x[1]/m_Mass1.m_Mass;

    // Let sx, sy be the x & y components of the spring length. */
    // NOTE important to use particle position here, not the */
    // data stored in the m_Spring, because the position of particle */
    // is changed during the RK solver procedure */
    // The X1,Y1 endpoint of the spring is fixed during this calculation
    // (X1,Y1 is connected to the TopMass).
    double sx = m_Spring.m_X1 - m_Point.x;
    double sy = m_Spring.m_Y1 - m_Point.y;
    double slen = Math.sqrt(sx*sx + sy*sy);
    /* cos theta is then */
    double cosTheta;
    if (Double.isInfinite(k))
      cosTheta = m_Point.direction*sy/slen;
    else
      cosTheta = m_Point.direction*(sx + k*sy)/(slen * Math.sqrt(1+k*k));
    if (cosTheta > 1 || cosTheta < -1)
      System.out.println("cosTheta out of range in diffeq1");
    /* stretch amount of spring is */
    double stretch = slen - m_Spring.m_RestLength;
    /* Then component due to spring is */
    change[1] += m_Spring.m_SpringConst*cosTheta*stretch/m_Mass1.m_Mass;
  }
}
