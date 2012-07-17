/*
  File: CollideSpring.java

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

// One spring connected to one mass, with another free moving mass, in 1D

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;


public class CollideSpring extends CollidingSim implements ActionListener
{
  private double damping = 0;
  private CSpring m_Spring;
  private CMass m_Mass1, m_Mass2;
  private CWall m_Wall, m_Wall2;
  private static final int ID_MASS1=1, ID_MASS2=2, ID_LEFT_WALL=3, ID_RIGHT_WALL=4;
  private JButton button_stop;
  private Vector collisions = new Vector(10);
  private static final String MASS1 = "mass1", DAMPING = "damping",
      LENGTH = "spring rest length", STIFFNESS = "spring stiffness",
      MASS2 = "mass2";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS1, DAMPING, STIFFNESS, LENGTH, MASS2};

  public CollideSpring(Container container) {
    super(container, 4);
    var_names = new String[] {
      "position 1",
      "velocity 1",
      "position 2",
      "velocity 2"
    };

    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -0.5, 7.5, -2, 2,
        CoordMap.ALIGN_LEFT, CoordMap.ALIGN_MIDDLE));

    double xx = 0.0;
    double yy = 0.0;
    m_Wall = new CWall (xx-0.3, yy-2, xx, yy+2, 0);  //x1, y1, x2, y2
    cvs.addElement(m_Wall);

    m_Spring = new CSpring (xx, yy, 2.5, 0.4); // x1, y1, restLen, thickness
    m_Spring.m_SpringConst=6;
    cvs.addElement(m_Spring);

    double w = 0.3;
    m_Mass1 = new CMass(xx+m_Spring.m_RestLength-2.0, yy-w, 2*w, 2*w, CElement.MODE_RECT);
    m_Spring.setX2(m_Mass1.m_X1);
    m_Mass1.m_Mass = 0.5;
    damping = 0;
    cvs.addElement(m_Mass1);

    m_Mass2 = new CMass(m_Mass1.m_X2+1.0, yy-w, 2*w, 2*w, CElement.MODE_RECT);
    m_Mass2.m_Mass = 1.5;
    cvs.addElement(m_Mass2);

    m_Wall2 = new CWall(7, yy-2, 7.4, yy+2, -90);
    cvs.addElement(m_Wall2);

    vars[0] = m_Mass1.m_X1;
    vars[1] = 0;  // velocity is zero at start
    vars[2] = m_Mass2.m_X1;
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
      graph.setVars(0,2);
    showGraph(false);
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS1))
      {m_Mass1.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(MASS2))
      {m_Mass2.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {damping = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS))
      {m_Spring.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH))
      {m_Spring.m_RestLength = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS1))
      return m_Mass1.m_Mass;
    else if (name.equalsIgnoreCase(MASS2))
      return m_Mass2.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return damping;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return m_Spring.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH))
      return m_Spring.m_RestLength;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) {
      vars[0] = 1;
      vars[1] = 0;
      vars[2] = 3;
      vars[3] = 0;
    }
  }

  public void modifyObjects()
  {
    m_Mass1.setX1(vars[0]);
    m_Spring.setX2(m_Mass1.m_X1);
    m_Mass2.setX1(vars[2]);
  }

  public void startDrag(Dragable e) {
    calc[0] = calc[1] = calc[2] = calc[3] = false;
  }

  public void constrainedSet(Dragable e, double x, double y)
  {
    if (e==m_Mass1)
    {
      if (x < m_Wall.m_X2)  // don't allow drag past wall
        x = m_Wall.m_X2;
      // don't drag past wall2
      if (x + m_Mass1.m_Width + m_Mass2.m_Width > m_Wall2.m_X1)
        x = m_Wall2.m_X1 - m_Mass2.m_Width - m_Mass1.m_Width;
      if (x+m_Mass1.m_Width > m_Mass2.m_X1)  // move other block
        m_Mass2.setX1(x+m_Mass1.m_Width);
      m_Mass1.setX1(x);  // only horizontal dragging allowed
      m_Spring.setX2(x);  // force spring to follow along
      vars[0] = m_Mass1.m_X1;
      vars[1] = 0;  // velocity is zero at start
      vars[2] = m_Mass2.m_X1;
      vars[3] = 0;
    }
    else if (e==m_Mass2)
    {
      if (x+m_Mass2.m_Width > m_Wall2.m_X1) // don't allow drag past wall2
        x = m_Wall2.m_X1 - m_Mass2.m_Width;
      if (x - m_Mass1.m_Width < m_Wall.m_X2)  // don't allow drag past wall1
        x = m_Wall.m_X2 + m_Mass1.m_Width;
      if (x < m_Mass1.m_X2) {
        // move other block, but keep a smidgen of space between to avoid collision
        m_Mass1.setX1(x - m_Mass1.m_Width - 0.001);
        m_Spring.setX2(m_Mass1.m_X1);  // force spring to follow along
      }
      m_Mass2.setX1(x);  // only horizontal dragging allowed
      vars[0] = m_Mass1.m_X1;
      vars[1] = 0;  // velocity is zero at start
      vars[2] = m_Mass2.m_X1;
      vars[3] = 0;
    }
    // objects other than mass 1 and mass 2 are not allowed to be dragged
  }

  private void addCollision(int obj1, int obj2) {
    collisions.addElement(new int[] {obj1, obj2});
  }

  public Vector findAllCollisions() {
    // Assumes only 3 possible collisions.
    collisions.removeAllElements();  // forget any previous value
    if (m_Mass1.m_X1 < m_Wall.m_X2)
      addCollision(ID_LEFT_WALL, ID_MASS1);
    if (m_Mass1.m_X2 > m_Mass2.m_X1)
      addCollision(ID_MASS1, ID_MASS2);
    if (m_Mass2.m_X2 > m_Wall2.m_X1)
      addCollision(ID_RIGHT_WALL, ID_MASS2);
    return (collisions.size() > 0) ? collisions : null;
  }

  // variables are:  position 1, velocity 1, position 2, velocity 2
  public void handleCollisions(Vector collisions) {
    for (Enumeration e = collisions.elements(); e.hasMoreElements();) {
      int[] objs = (int[])e.nextElement();
      if (objs[0] == ID_LEFT_WALL) {
        // mass1 collided with left wall, so just reverse the velocity
        vars[1] = - vars[1];
      } else if (objs[0] == ID_RIGHT_WALL) {
        // mass2 collided with right wall, so just reverse the velocity
        vars[3] = - vars[3];
      } else if ((objs[0] == ID_MASS1) && (objs[1] == ID_MASS2)) {
        // mass1 and mass2 collided.
        // Find velocity of center of mass.
        double vcm = (m_Mass1.m_Mass*vars[1] + m_Mass2.m_Mass*vars[3])
          /(m_Mass1.m_Mass+m_Mass2.m_Mass);
        // adjust the velocities of particles
        // To find new velocity, find the velocity in the center of mass frame
        // and reflect it.  This works out to -v + 2*vcm.
        // Here's the derivation:
        //    Velocity in cm frame is v-vcm.
        //    In cm frame, total momentum is zero, after collision momentum is
        //    preserved, so we just reverse signs of each velocity in cm frame.
        //    Reflection of velocity is -(v-vcm) = vcm-v
        //    Add back vcm to get to laboratory frame:  = vcm + (vcm-v) = 2*vcm - v
        vars[1] = -vars[1] + 2*vcm;
        vars[3] = -vars[3] + 2*vcm;
      }
    }
  }

  /*
    xxx origin = connection of spring to wall
    origin = topleft
    vars[0] = position (x) with origin as above
    vars[1] = velocity (v=x')
    vars[2] = x1 position of mass 2
    vars[3] = velocity of mass 2
    R = rest length
    S1 = left end of spring
    S2 = right end of spring (same as x?)
    len = current length of spring = x - S1.x
    L = how much spring is stretched from rest length
    L = len - R = x - S1.x - R
    k = spring constant
    b = damping constant
    F = -kL -bv = -k(x - S1.x - R) -bv = m v'
    so diffeq's are:
    x' = v
    v' = -(k/m)(x - S1.x - R) -(b/m)v
  */
  public void evaluate(double[] x, double[] change) {
    change[0] = x[1]; // x' = v
    // v' = -(k/m)(x - S1.x - R) - (damping/m) v
    double r = -m_Spring.m_SpringConst*(x[0] - m_Spring.m_X1
      - m_Spring.m_RestLength) - damping*x[1];
    change[1] = r/m_Mass1.m_Mass;
    change[2] = x[3]; // x' = v
    change[3] = 0;  // v' = 0 because constant velocity
  }

}
