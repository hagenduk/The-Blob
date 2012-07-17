/*
  File: Double2DSpring.java

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
// An immoveable but draggable mass with a 2 springs and 2 masses hanging below
// and swinging in 2D.

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/////////////////////////////////////////////////////////////////////////////////
public class Double2DSpring extends Simulation implements ActionListener
{
  private CMass mass1, mass2, topMass;
  private CSpring spring1, spring2;
  private double gravity = 9.8, damping=0.0;
  private JButton button_stop;
  private static final String MASS1 = "mass1",
          MASS2="mass2",
          LENGTH1="spring1 length",
          LENGTH2="spring2 length",
          STIFF1="spring1 stiffness",
          STIFF2="spring2 stiffness",
          DAMPING="damping",
          GRAVITY="gravity";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS1, MASS2, LENGTH1, LENGTH2, STIFF1, STIFF2,
        DAMPING, GRAVITY};

  public Double2DSpring(Container container) {
    super(container, 8);
    var_names = new String[] {
    "x1 position",
    "y1 position",
    "x2 position",
    "y2 position",
    "x1 velocity",
    "y1 velocity",
    "x2 velocity",
    "y2 velocity"
    };
    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -6, 6, -6, 6,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    double xx = 0, yy = -2, w = 0.5;
    topMass = new CMass(xx-w/2, yy-w, w, w, CElement.MODE_RECT);
    cvs.addElement(topMass);

    spring1 = new CSpring (xx, yy, 1.0, 0.3); // x1, y1, restLen, thick
    spring1.setX2(xx);
    spring1.m_SpringConst=6;
    cvs.addElement(spring1);

    mass1 = new CMass(xx-w/2, 0, w, w, CElement.MODE_CIRCLE);
    mass1.m_Mass = .5;
    mass2 = new CMass(xx-w/2, 0, w, w, CElement.MODE_CIRCLE);
    mass2.m_Mass = .5;
    // set the position of the mass to be where spring stretch balances weight
    double yy2 = yy + spring1.m_RestLength +
      (mass1.m_Mass+mass2.m_Mass)*gravity/spring1.m_SpringConst;
    mass1.setY1(yy2-w/2);
    spring1.setY2(yy2);
    mass1.m_Damping = 0;
    cvs.addElement(mass1);

    spring2 = new CSpring (xx, yy2, 1.0, 0.3); // x1, y1, restLen, thick
    spring2.setX2(xx);
    spring2.m_SpringConst=6;
    cvs.addElement(spring2);

    // set the position of the mass to be where spring stretch balances weight
    double yy3 = yy2 + spring2.m_RestLength +
      mass2.m_Mass*gravity/spring2.m_SpringConst;
    mass2.setY1(yy3-w/2);
    spring2.setY2(yy3);
    mass2.m_Damping = 0;
    cvs.addElement(mass2);

    stopMotion();  // get to quiet state
    vars[0] += 0.5; // perturb slightly to get some motion
    vars[1] += 0.5;
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

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS1))
      {mass1.m_Mass = value; return true;}
    if (name.equalsIgnoreCase(MASS2))
      {mass2.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH1))
      {spring1.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH2))
      {spring2.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(STIFF1))
      {spring1.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(STIFF2))
      {spring2.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {damping = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS1))
      return mass1.m_Mass;
    if (name.equalsIgnoreCase(MASS2))
      return mass2.m_Mass;
    else if (name.equalsIgnoreCase(LENGTH1))
      return spring1.m_RestLength;
    else if (name.equalsIgnoreCase(LENGTH2))
      return spring2.m_RestLength;
    else if (name.equalsIgnoreCase(STIFF1))
      return spring1.m_SpringConst;
    else if (name.equalsIgnoreCase(STIFF2))
      return spring2.m_SpringConst;
    else if (name.equalsIgnoreCase(DAMPING))
      return damping;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  private void stopMotion() {
    double m1 = mass1.m_Mass;
    double m2 = mass2.m_Mass;
    double k1 = spring1.m_SpringConst;
    double k2 = spring2.m_SpringConst;
    double r1 = spring1.m_RestLength;
    double r2 = spring2.m_RestLength;
    double T = topMass.m_Y2;

    //x1 & x2 position
    vars[0] = vars[2] = topMass.m_X1 + topMass.m_Width/2;

    // derive these by writing the force equations to yield zero accel
    // when everything is lined up vertically.
    //y1 position
    vars[1] = gravity*(m1+m2)/k1 + r1 + T;
    //y2 position
    vars[3] = gravity*(m2/k2 + (m1+m2)/k1) + r1 + r2 + T;
    //velocities are all zero
    vars[4] = vars[5] = vars[6] = vars[7] = 0;
  }

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) {
      stopMotion();
    }
  }

  public void modifyObjects() {
    // assume all masses are same width & height
    double w = mass1.m_Width/2;
    mass1.setPosition(vars[0] - w, vars[1]-w);
    mass2.setPosition(vars[2] - w, vars[3]-w);
    spring1.setX2(mass1.m_X1 + w);
    spring1.setY2(mass1.m_Y1 + w);
    spring2.setBounds(mass1.m_X1+w, mass1.m_Y1+w, mass2.m_X1+w, mass2.m_Y1+w);
  }

  public void startDrag(Dragable e) {
    if (e==mass1) {
      calc[0] = calc[1] = calc[4] = calc[5] = false;
    } else if (e==mass2) {
      calc[2] = calc[3] = calc[6] = calc[7] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y) {
    // objects other than mass are not allowed to be dragged
    // assume all masses are same width & height
    double w = mass1.m_Width/2;
    if (e==topMass) {
      e.setPosition(x, y);
      // force spring to follow along
      spring1.setPosition(x + topMass.m_Width/2, y + topMass.m_Height);
    } else if (e==mass1) {
      vars[0] = x + w;
      vars[1] = y + w;
      vars[4] = vars[5] = 0;
      modifyObjects();
    } else if (e==mass2) {
      vars[2] = x + w;
      vars[3] = y + w;
      vars[6] = vars[7] = 0;
      modifyObjects();
    }
  }

  /* 2-D spring simulation with gravity
    spring is suspended from top mass
    origin = topleft corner
    th = angle formed with vertical, positive is counter clockwise
    L = displacement of spring from rest length
    R = rest length
    U = position of CENTER of mass
    S1 = position of spring1 X1,Y1 point
    V = velocity of mass
    k = spring constant
    b = damping constant
    m = mass of mass
    w = width (radius) of mass

    F1x = -k1 L1 sin(th1) +k2 L2 sin(th2) -b1 V1x = m1 V1x'
    F1y = m1 g -k1 L1 cos(th1) +k2 L2 cos(th2) -b1 V1y = m1 V1y'
    F2x = -k2 L2 sin(th2) -b2 V2x = m2 V2x'
    F2y = m2 g -k2 L2 cos(th2) -b2 V2y = m2 V2y'
    xx = U1x - S1x
    yy = U1y - S1y
    len1 = Sqrt(xx^2+yy^2)
    L1 = len1 - R1
    th1 = atan(xx/yy)
    cos(th1) = yy / len1
    sin(th1) = xx / len1
    xx2 = U2x - U1x
    yy2 = U2y - U1y
    len2 = sqrt(xx2^2+yy2^2)
    L2 = len2 - R2
    cos(th2) = yy2 / len2
    sin(th2) = xx2 / len2

    vars[i]:  U1x, U1y, U2x, U2y, V1x, V1y, V2x, V2y
    i:         0    1    2    3    4    5    6    7
  */
  public void evaluate(double[] x, double[] change) {
    double xx = x[0] - spring1.m_X1; //xx = U1x - S1x
    double yy = x[1] - spring1.m_Y1; //yy = U1y - S1y
    double len1 = Math.sqrt(xx*xx + yy*yy);
    double m1 = mass1.m_Mass;
    double xx2 = x[2] - x[0]; //xx2 = U2x - U1x
    double yy2 = x[3] - x[1]; //yy2 = U2y - U1y
    double len2 = Math.sqrt(xx2*xx2 + yy2*yy2);
    double m2 = mass2.m_Mass;

    change[0] = x[4]; //U1x' = V1x
    change[1] = x[5]; //U1y' = V1y
    change[2] = x[6]; //U2x' = V2x
    change[3] = x[7]; //U2y' = V2y

    // F1x = -k1 L1 sin(th1) +k2 L2 sin(th2) -damping V1x = m1 V1x'
    // -(k1/m1)L1 sin(th1) = -(k1/m1)*(len1 - R1)*(xx/len1)
    double r = -(spring1.m_SpringConst/m1)*
      (len1 - spring1.m_RestLength) * xx / len1;
    //+(k2/m1) L2 sin(th2) = +(k2/m1)*(len2 - R2)*(xx2/len2)
    r += (spring2.m_SpringConst/m1)*
      (len2 - spring2.m_RestLength) * xx2 / len2;
    // damping:  - (damping/m1) V1x
    if (damping!=0) { r -= (damping/m1)*x[4] ;}
    change[4] = r;

    //F1y = m1 g -k1 L1 cos(th1) +k2 L2 cos(th2) -damping V1y = m1 V1y'
    // g -(k1/m1)L1 cos(th1) = g - (k1/m1)*(len1 - R1)*(yy/len1)
    r = gravity -(spring1.m_SpringConst/m1)*
      (len1 - spring1.m_RestLength) * yy / len1;
    //+(k2/m1) L2 cos(th2) = +(k2/m1)*(len2-R2)*(yy2/len2)
    r += (spring2.m_SpringConst/m1)*
      (len2 - spring2.m_RestLength) * yy2 / len2;
    // damping:  - (damping/m1) V1y
    if (damping!=0) { r -= (damping/m1)*x[5] ;}
    change[5] = r;

    // F2x = -k2 L2 sin(th2) -damping V2x = m2 V2x'
    //-(k2/m2) L2 sin(th2) = -(k2/m2)*(len2-R2)*(xx2/len2)
    r = -(spring2.m_SpringConst/m2)*
      (len2 - spring2.m_RestLength) * xx2 / len2;
    // damping:  - (b2/m2) V2x
    if (damping!=0) { r -= (damping/m2)*x[6] ;}
    change[6] = r;

    // F2y = m2 g -k2 L2 cos(th2) -damping V2y = m2 V2y'
    //g -(k2/m2) L2 cos(th2) = g - (k2/m2)*(len2-R2)*(yy2/len2)
    r = gravity -(spring2.m_SpringConst/m2)*
      (len2 - spring2.m_RestLength) * yy2 / len2;
    // damping:  - (damping/m2) V2y
    if (damping!=0) { r -= (damping/m2)*x[7] ;}
    change[7] = r;
  }
}
