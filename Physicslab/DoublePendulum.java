/*
  File: DoublePendulum.java

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


public class DoublePendulum extends Simulation implements ActionListener
{
  private CMass m_Mass1, m_Mass2;
  private CSpring m_Stick1, m_Stick2;
  private double gravity = 9.8;
  private JButton button_stop;
  private static final String MASS1 = "mass1",
      MASS2 = "mass2", LENGTH1 = "stick1 length", LENGTH2 = "stick2 length",
      GRAVITY = "gravity";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS1, MASS2, LENGTH1, LENGTH2, GRAVITY};

  public DoublePendulum(Container container) {
    super(container, 4);
    var_names = new String[] {
      "angle1",
      "angle1 velocity",
      "angle2",
      "angle2 velocity"
    };
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -2, 2, -2.2, 1.5,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

    // x1, y1, restLen, thickness
    m_Stick1 = new CSpring (0, 0, 1, 0.4);
    m_Stick1.m_DrawMode = CElement.MODE_LINE;
    cvs.addElement(m_Stick1);

    // x1, y1, restLen, thickness
    m_Stick2 = new CSpring (0, 0, 1, 0.4);
    m_Stick2.m_DrawMode = CElement.MODE_LINE;
    cvs.addElement(m_Stick2);

    double w = 0.2;
    // x1, y1, width, height, drawmode
    m_Mass1 = new CMass(0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
    m_Mass1.m_Mass = 0.5;
    m_Mass1.m_Color = Color.blue;
    cvs.addElement(m_Mass1);


    m_Mass2 = new CMass( 0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
    m_Mass2.m_Mass = 0.5;
    m_Mass2.m_Damping = 0;
    m_Mass2.m_Color = Color.blue;
    cvs.addElement(m_Mass2);

    vars[0]=Math.PI/8;
    modifyObjects();
  }

  public void setupControls() {
    super.setupControls();
    // DoubleField params:  subject, name, fraction digits
    for (int i=0; i<params.length; i++)
      addObserverControl(new DoubleField(this, params[i], 2));
    showControls(true);
    addControl(button_stop = new JButton("reset"));
    button_stop.addActionListener(this);
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null)
      graph.setVars(0,2);
  }

  protected void setValue(int param, double value) {
    switch (param) {
      case 0: m_Mass1.m_Mass = value; break;
      case 1: m_Mass2.m_Mass = value; break;
      case 2: m_Stick1.m_RestLength = value; break;
      case 3: m_Stick2.m_RestLength = value; break;
      case 4: gravity = value; break;
    }
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS1))
      {m_Mass1.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(MASS2))
      {m_Mass2.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH1))
      {m_Stick1.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH2))
      {m_Stick2.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
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
    else if (name.equalsIgnoreCase(LENGTH1))
      return m_Stick1.m_RestLength;
    else if (name.equalsIgnoreCase(LENGTH2))
      return m_Stick2.m_RestLength;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) { vars[0] = vars[1] = vars[2] = vars[3] = 0; }
  }

  public void modifyObjects()  {
    // the variables are:  0,1,2,3:  theta1,theta1',theta2,theta2'
    // limit the pendulum angle to +/- Pi
    if (vars[0] > Math.PI)
      vars[0] = vars[0] - 2*Math.PI*Math.floor(vars[0]/Math.PI);
    else if (vars[0] < -Math.PI)
      vars[0] = vars[0] - 2*Math.PI*Math.ceil(vars[0]/Math.PI);

    if (vars[2] > Math.PI)
      vars[2] = vars[2] - 2*Math.PI*Math.floor(vars[2]/Math.PI);
    else if (vars[2] < -Math.PI)
      vars[2] = vars[2] - 2*Math.PI*Math.ceil(vars[2]/Math.PI);

    double w = m_Mass1.m_Width/2;
    double L1 = m_Stick1.m_RestLength;
    double L2 = m_Stick2.m_RestLength;
    double th1 = vars[0];
    double th2 = vars[2];
    double x1 = L1*Math.sin(th1);
    double y1 = -L1*Math.cos(th1);
    double x2 = x1 + L2*Math.sin(th2);
    double y2 = y1 - L2*Math.cos(th2);
    m_Stick1.setBounds(0,0,x1,y1);
    m_Mass1.setPosition(x1-w, y1-w);
    m_Stick2.setBounds(x1,y1,x2,y2);
    m_Mass2.setPosition(x2-w, y2-w);
  }

  public void startDrag(Dragable e) {
    // can't do "live dragging" because everything is too connected!
    if ((e == m_Mass1) || (e == m_Mass2)) {
      calc[0] = calc[1] = calc[2] = calc[3] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y)
  {
    double w = m_Mass1.m_Width/2;
    if (e==m_Mass1)
    {
      //the variables are:  0,1,2,3:  theta1,theta1',theta2,theta2'
      // x,y correspond to the new m_X1, m_Y1 of the object
      // We want to work with the center of the object,
      // so adjust to xx,yy as follows.
      double xx = x + w; // coords of center
      double yy = y + w;
      double th1 = Math.atan2(xx, -yy);
      vars[0] = th1;
      vars[1] = 0;  // theta1'
      vars[3] = 0; // theta2'
      modifyObjects();
    }
    else if (e==m_Mass2)
    {
      double L1 = m_Stick1.m_RestLength;
      double L2 = m_Stick2.m_RestLength;
      // get center of mass1
      double x1 = L1*Math.sin(vars[0]);
      double y1 = -L1*Math.cos(vars[0]);
      // get center of mass2  (x,y correspond to m_X1,m_Y1 = topleft)
      double x2 = x + w; // coords of center
      double y2 = y + w;
      double th2 = Math.atan2(x2-x1, -(y2-y1));
      vars[1] = 0;  // theta1'
      vars[2] = th2;
      vars[3] = 0;
      modifyObjects();
    }
  }

  /*  Equations for double pendulum

      L1,L2 = stick lengths
      m1,m2 = masses
      g = gravity
      theta1,theta2 = angles of sticks with vertical (down = 0)
      th1,th2 = theta1,theta2 abbreviations

    diff eqs:
        -g (2 m1 + m2) Sin[th1] 
        - g m2 Sin[th1 - 2 th2]
        - 2 m2 dth2^2 L2 Sin[th1 - th2]
        - m2 dth1^2 L1 Sin[2(th1 - th2)]
    ddth1 = -----------------------------------------------------------------
             L1 (2 m1 + m2 - m2 Cos[2(th1-th2)])


        2 Sin[th1-th2](
          (m1+m2) dth1^2 L1 
          + g (m1+m2) Cos[th1] 
          + m2 dth2^2 L2 Cos[th1-th2]
        )
    ddth2= ---------------------------------------------------------------------------------
                             L2 (2 m1 + m2 - m2 Cos[2(th1-th2)])


     the variables are:  0,1,2,3:  theta1,theta1',theta2,theta2'
    vars[0] = theta1
    vars[1] = theta1'
    vars[2] = theta2
    vars[3] = theta2'

  */
  public void evaluate(double[] x, double[] change) {
    double th1 = x[0];
    double dth1 = x[1];
    double th2 = x[2];
    double dth2 = x[3];
    double m2 = m_Mass2.m_Mass;
    double m1 = m_Mass1.m_Mass;
    double L1 = m_Stick1.m_RestLength;
    double L2 = m_Stick2.m_RestLength;
    double g = gravity;

    change[0] = dth1;

    double num = -g*(2*m1+m2)*Math.sin(th1);
    num = num - g*m2*Math.sin(th1-2*th2);
    num = num - 2*m2*dth2*dth2*L2*Math.sin(th1-th2);
    num = num - m2*dth1*dth1*L1*Math.sin(2*(th1-th2));
    num = num/(L1*(2*m1+m2-m2*Math.cos(2*(th1-th2))));
    change[1] = num;

    change[2] = dth2;

    num = (m1+m2)*dth1*dth1*L1;
    num = num + g*(m1+m2)*Math.cos(th1);
    num = num + m2*dth2*dth2*L2*Math.cos(th1-th2);
    num = num*2*Math.sin(th1-th2);
    num = num/(L2*(2*m1+m2-m2*Math.cos(2*(th1-th2))));
    change[3] = num;
  }

}
