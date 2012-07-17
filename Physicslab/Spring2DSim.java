/*
  File: Spring2DSim.java

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

// An immoveable top mass with a spring and moveable mass hanging below
// and swinging in 2D.  (The top mass can however be dragged by the user).
//

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Spring2DSim extends Simulation implements ActionListener
{
  private CSpring spring;
  private CMass bob, topMass;
  private JButton button_stop;
  private double gravity = 9.8;
  private double m_Damping = 0;
  private static final String MASS = "mass", DAMPING = "damping",
      LENGTH = "spring rest length", STIFFNESS = "spring stiffness",
      GRAVITY = "gravity";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS, DAMPING, STIFFNESS, LENGTH, GRAVITY};

  public Spring2DSim(Container container) {
    super(container, 4);
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -6, 6.0, -6, 6,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    double w = 1;
    // x1, y1, width, height, drawmode
    topMass = new CMass(-w/2, 3, w, w, CElement.MODE_RECT);
    cvs.addElement(topMass);

    bob = new CMass(-2.5, -2, w, w, CElement.MODE_CIRCLE);
    bob.m_Mass = 0.5;
    cvs.addElement(bob);

    // x1, y1, restLen, thickness
    spring = new CSpring (topMass.m_X1+w/2, topMass.m_Y1, 2.5, 0.6);
    spring.m_SpringConst = 6.0;
    cvs.addElement(spring);

    var_names = new String[] {
      "x position",
      "y position",
      "x velocity",
      "y velocity"
      };

    vars[0] = bob.m_X1 + bob.m_Width/2;
    vars[1] = bob.m_Y1 + bob.m_Height/2;
    vars[2] = 0;
    vars[3] = 0;
    modifyObjects();
  }

  public void setupControls() {
    super.setupControls();

    addControl(button_stop = new JButton("reset"));
    button_stop.addActionListener(this);
    // DoubleField params:  subject, name, fraction digits
    for (int i=0; i<params.length; i++)
      addObserverControl(new DoubleField(this, params[i], 2));
    showControls(true);
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null)
      graph.setVars(0,1);
  }

  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS))
      {bob.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {m_Damping = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS))
      {spring.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH))
      {spring.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS))
      return bob.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return m_Damping;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return spring.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH))
      return spring.m_RestLength;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    return super.getParameter(name);
  }

  public String[] getParameterNames() {
    return params;
  }

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) {
      vars[0] = topMass.m_X1 + topMass.m_Width/2;
      vars[1] = topMass.m_Y1 - spring.m_RestLength -
        bob.m_Mass*gravity/spring.m_SpringConst;
      vars[2] = 0;
      vars[3] = 0;
    }
  }

  public void modifyObjects() {
    bob.setX1(vars[0] - bob.m_Width/2);
    bob.setY1(vars[1] - bob.m_Height/2);
    spring.setX2(vars[0]);
    spring.setY2(vars[1]);
  }

  public void startDrag(Dragable e) {
    if (e == bob) {
      calc[0] = false;
      calc[1] = false;
      calc[2] = false;
      calc[3] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y) {
    if (e==topMass)
    {
      ((CElement)e).setX1(x);
      ((CElement)e).setY1(y);
      spring.setX1(x + topMass.m_Width/2);  // force spring to follow along
      spring.setY1(y);
    }
    else if (e==bob)
    {
      vars[0] = x + bob.m_Width/2;
      vars[1] = y + bob.m_Height/2;
      vars[2] = 0;
      vars[3] = 0;
      modifyObjects();
    }
    // objects other than mass are not allowed to be dragged
  }

  /* 2-D spring simulation with gravity
    spring is suspended from top mass
    origin = bottomleft corner
    th = angle formed with vertical, positive is counter clockwise

    L = displacement of spring from rest length
    R = rest length
    U = position of CENTER of bob
    S = position of spring's X1,Y1 point
    V = velocity of bob
    k = spring constant
    b = damping constant
    m = mass of mass
    w = width (radius) of bob

    Fx = -kL sin(th) -bVx = m Vx'
    Fy = -mg +kL cos(th) -bVy = m Vy'
    xx = Ux - Sx
    yy = -Uy + Sy
    len = Sqrt(xx^2+yy^2)
    L = len - R
    th = atan(xx/yy)
    cos(th) = yy / len
    sin(th) = xx / len

    so here are the four variables and their diffeq's
    vars[0] = Ux
    vars[1] = Uy
    vars[2] = Vx
    vars[3] = Vy
    Ux' = Vx
    Uy' = Vy
    Vx' = -(k/m)L sin(th) -(b/m)Vx
    Vy' = g + (k/m)L cos(th) -(b/m)Vy
  */
  // implementation of DiffEq protocol
  public void evaluate(double[] x, double[] change) {
    double xx, yy, len, m, r, b;
    m = bob.m_Mass;
    b = m_Damping;
    // find current length of the spring
    xx = x[0] - spring.m_X1;  //xx = Ux - Sx
    yy = -x[1] + spring.m_Y1;  //yy = -Uy + Sy
    len = Math.sqrt(xx*xx + yy*yy);  //len = Sqrt(xx^2+yy^2)

    change[0] = x[2]; // Ux' = Vx

    change[1] = x[3]; // Uy' = Vy

    //Vx' = -(k/m)L sin(th) = -(k/m)*(len - R)*(xx/len)
    r = -(spring.m_SpringConst/m)*(len - spring.m_RestLength) * xx / len;
    if (b != 0)
      r -= (b/m)*x[2];  // damping:  - (b/m) Vx
    change[2] = r;

    //Vy' = -g + (k/m)L cos(th) = -g + (k/m)*(len-R)*(yy/len)
    r = -gravity + (spring.m_SpringConst/m)*(len - spring.m_RestLength) * yy / len;
    if (b != 0)
      r -= (b/m)*x[3];  // damping:  - (b/m) Vy
    change[3] = r;
  }
}


