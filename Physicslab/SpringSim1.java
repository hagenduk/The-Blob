/*
  File: SpringSim1.java
  Simulation of a simple spring and mass system.

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

/////////////////////////////////////////////////////////////////////////////////
public class SpringSim1 extends Simulation
{
  private CSpring spring;
  private CMass block;
  private double m_Damping = 0.1;
  private BarChart chart;
  private static final String MASS = "mass", DAMPING = "damping",
      LENGTH = "spring rest length", STIFFNESS = "spring stiffness";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS, DAMPING, STIFFNESS, LENGTH};

  public SpringSim1(Container container) {
    super(container, 3);
    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -0.5, 6.0, -3, 3,
        CoordMap.ALIGN_LEFT, CoordMap.ALIGN_MIDDLE));
    double w = 0.3;
    // x1, y1, width, height, drawmode
    block = new CMass(0.5, -w, 2*w, 2*w, CElement.MODE_RECT);
    block.m_Mass = 0.5;
    cvs.addElement(block);

    spring = new CSpring (0, 0, 2.5, 0.5); // x1, y1, restLen, thickness
    spring.m_SpringConst=3;
    spring.setX2(block.m_X1);
    cvs.addElement(spring);

    chart = new BarChart(cvs.getSimBounds());
    cvs.addElement(chart);

    var_names = new String[] {
      "position",
      "velocity",
      "work from damping",
      "acceleration",
      "kinetic energy",
      "spring energy",
      "total energy"
      };

    vars[0] = block.m_X1;
    vars[1] = 0;
    vars[2] = 0;
    initWork();
  }

  public void setupControls() {
    super.setupControls();
    // DoubleField params:  subject, name, fraction digits
    for (int i=0; i<params.length; i++)
      addObserverControl(new DoubleField(this, params[i], 2));
    showControls(true);
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS))
      {block.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {m_Damping = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS))
      {spring.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH))
      {spring.m_RestLength = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS))
      return block.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return m_Damping;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return spring.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH))
      return spring.m_RestLength;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  private void initWork() {
    calcEnergy();
    chart.setWorkZero(chart.te + chart.pe);
  }

  private void calcEnergy() {
    chart.te = 0.5*block.m_Mass*vars[1]*vars[1];
    double x1 = vars[0] - spring.m_X1 - spring.m_RestLength;
    chart.pe = 0.5*spring.m_SpringConst*x1*x1;
    chart.work = vars[2];
  }

  public void modifyObjects() {
    block.setX1(vars[0]);
    spring.setX2(block.m_X1);
    calcEnergy();
    //double tot = chart.te + chart.pe;
    //System.out.println("total energy="+(tot)+" work="+chart.work+" energy-work="+(tot-chart.work));
  }

  public int numVariables() {
    return var_names.length;
  }

  public double getVariable(int i) {
    if (i<=2)
      return vars[i];
    if (i==3) {   // acceleration
      double[] rate = new double[vars.length];   // this creates lots of heap garbage!
      evaluate(vars, rate);
      return rate[1];
    }
    // kinetic energy = 0.5*m*v^2
    double ke = 0.5*block.m_Mass*vars[1]*vars[1];
    if (i==4)
      return ke;
    // potential = 0.5*k*x^2 where k = spring const, x = spring stretch
    double x = vars[0] - spring.m_X1 - spring.m_RestLength;
    double pe = 0.5*spring.m_SpringConst*x*x;
    if (i==5)
      return pe;
    // total energy
    if (i==6)
      return ke+pe;
    return 0;
  }

  public void startDrag(Dragable e) {
    if (e == block) {
      calc[0] = false;
      calc[1] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y) {
    if (e==block) {
      // don't allow vertical dragging, so only set horizontal component
      vars[0] = x;
      vars[1] = 0;
      vars[2] = 0;
      modifyObjects();
      initWork();
    }
    // objects other than mass are not allowed to be dragged
  }

  /*
    vars[0] = position (x) with origin as above
    vars[1] = velocity (v=x')
    R = rest length
    len = current length of spring = x - origin.x
    L = how much spring is stretched from rest length
    L = len - R = x - origin.x - R
    k = spring constant
    b = damping constant
    F = m a  (force = mass * acceleration) leads to:
    F = -kL -bv = -k(x - origin.x - R) -bv = m v'
    so diffeq's are:
    x' = v
    v' = -(k/m)(x - origin.x - R) -(b/m)v
  */
  public void evaluate(double[] x, double[] change) {
    // assume that one end of the spring1 is fixed at 0, the other end is at position x1.
    // spring2 stretches from x1 to x2
    // variables are:  x1, v1
    // parameters are: k=spring stiffness, R=spring restlength, L=damping, m1,m2=mass
    // x' = v
    change[0] = x[1];
    // v' = -(k/m)(x - R) - (b/m) v
    double r = -spring.m_SpringConst*(x[0] - spring.m_X1 - spring.m_RestLength)
        - m_Damping*x[1];
    change[1] = r/block.m_Mass;
    // here we are intergrating work done by damping.
    // dW = F dR  (work = force times distance)
    // therefore dW/dt = F dR/dt = F v
    // Since the damping force is F = -b v we have dW/dt = -b v^2.
    change[2] = -m_Damping*x[1]*x[1];
  }
}


