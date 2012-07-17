/*
  File: DangleStick.java

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

//    A stick dangling (hanging) from a spring.

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/////////////////////////////////////////////////////////////////////////////////
public class DangleStick extends Simulation implements ActionListener
{
  private CMass m_Mass1, m_Mass2;
  private CSpring m_Spring, m_Stick;
  private double gravity = 9.8;
  private JButton button_stop;
  private static final String MASS1 = "upper mass",
      MASS2 = "lower mass",
      GRAVITY = "gravity", STICK_LENGTH = "stick length",
      SPRING_LENGTH = "spring rest length", STIFFNESS = "spring stiffness";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS1, MASS2, STIFFNESS, SPRING_LENGTH,
      STICK_LENGTH, GRAVITY};


  public DangleStick(Container container) {
    super(container, 6);
    var_names = new String[] {
      "spring angle",
      "spring angle vel",
      "spring length",
      "spring length vel",
      "stick angle",
      "stick angle vel"
    };
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -2, 2, -4, 2,
          CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    double w = 0.3;
    // x1, y1, width, height, drawmode
    m_Mass1 = new CMass(0.4, -1.2, w, w, CElement.MODE_CIRCLE);
    m_Mass1.m_Mass = 0.5;
    cvs.addElement(m_Mass1);

    // x1, y1, restLen, thickness, drawing mode
    m_Stick = new CSpring (0.4, -1.2, 1, 0.4);
    m_Stick.m_DrawMode = CElement.MODE_LINE;
    cvs.addElement(m_Stick);

    m_Mass2 = new CMass( 0.4, -2.2, w, w, CElement.MODE_CIRCLE);
    //m_Stick.setX2(m_Mass2.m_X1 + w/2);
    //m_Stick.setY2(m_Mass2.m_Y1 - w/2);
    m_Mass2.m_Mass = 0.5;
    m_Mass2.m_Damping = 0;
    cvs.addElement(m_Mass2);

    // x1, y1, restLen, thickness, drawing mode
    m_Spring = new CSpring (0, 0, 1, 0.4);
    m_Spring.m_SpringConst=20;
    //m_Spring.setX2(m_Mass1.m_X1 + w/2);
    //m_Spring.setY2(m_Mass1.m_Y1 - w/2);
    cvs.addElement(m_Spring);

    vars[0] = (Math.PI*30)/180;
    vars[1] = 0;
    vars[2] = 2.0;
    vars[3] = 0;
    vars[4] = (-Math.PI*30)/180;
    vars[5] = 0;
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
    showGraph(false);
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS1))
      {m_Mass1.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(MASS2))
      {m_Mass2.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS))
      {m_Spring.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(SPRING_LENGTH))
      {m_Spring.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(STICK_LENGTH))
      {m_Stick.m_RestLength = value; return true;}
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
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return m_Spring.m_SpringConst;
    else if (name.equalsIgnoreCase(SPRING_LENGTH))
      return m_Spring.m_RestLength;
    else if (name.equalsIgnoreCase(STICK_LENGTH))
      return m_Stick.m_RestLength;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  public void actionPerformed (ActionEvent e) {
    if (e.getSource() == button_stop)
      stop();
  }

  private void stop()  {
    vars[0] = vars[1] = vars[3] = vars[4] = vars[5] = 0;
    double r = gravity*(m_Mass1.m_Mass + m_Mass2.m_Mass)/m_Spring.m_SpringConst;
    vars[2] = m_Spring.m_RestLength + r;
  }

  public void modifyObjects() {
    //  variables are:  0,     1,   2,3,  4,  5:
    //                 theta,theta',r,r',phi,phi'
    double w = m_Mass1.m_Width/2;
    m_Mass1.setX1(vars[2]*Math.sin(vars[0]) - w);
    m_Mass1.setY1(-vars[2]*Math.cos(vars[0]) - w);
    double L = m_Stick.m_RestLength;
    m_Mass2.setX1(m_Mass1.m_X1 + L*Math.sin(vars[4]));
    m_Mass2.setY1(m_Mass1.m_Y1 - L*Math.cos(vars[4]));
    m_Spring.setX2(m_Mass1.m_X1+w);
    m_Spring.setY2(m_Mass1.m_Y1+w);
    m_Stick.setX1(m_Mass1.m_X1+w);
    m_Stick.setY1(m_Mass1.m_Y1+w);
    m_Stick.setX2(m_Mass2.m_X1+w);
    m_Stick.setY2(m_Mass2.m_Y1+w);
  }

  public void startDrag(Dragable e)  {
    // can't do "live dragging" because everything is too connected!
    for (int i=0; i<vars.length; i++)
      calc[i] = false;
  }

  public void constrainedSet(Dragable e, double x, double y)  {
    double w = m_Mass1.m_Width/2;
    if (e==m_Mass1) {
      //the variables are:  0,     1,   2,3,  4,  5:
      //                   theta,theta',r,r',phi,phi'      */
       // x,y correspond to the new m_X1, m_Y1 of the object
       // We want to work with the center of the object,
       // so adjust to xx,yy as follows.
      double xx = x + w; // coords of center
      double yy = y + w;
      double th = Math.atan2(xx, -yy);
      vars[0] = th;
       vars[2] = Math.sqrt(xx*xx + yy*yy);  // r
       vars[1] = 0; // theta'
       vars[3] = 0; // r'
       vars[5] = 0; // phi'
    } else if (e==m_Mass2) {
      // get center of mass1
      double x1 = vars[2]*Math.sin(vars[0]);
      double y1 = -vars[2]*Math.cos(vars[0]);
      // get center of mass2  (x,y correspond to m_X1,m_Y1 = topleft)
      double x2 = x + w; // coords of center
      double y2 = y + w;
      double th = Math.atan2(x2-x1, -(y2-y1));
      vars[4] = th;
      vars[1] = 0; // theta'
        vars[3] = 0; // r'
      vars[5] = 0; // phi'
    }
  }

  /*
    A stick (actually a massless bar with a mass on each end)
    dangles from a spring.

      b = spring rest length
      L = stick length
      m1 = mass at spring end
      m2 = mass at free end
      g = gravity
      k = spring constant
      r = length of spring
      theta = angle of spring with vertical (down = 0)
      phi = angle of stick with vertical (down = 0)

    diff eqs:
    theta'' = (-4 m1(m1+m2)r' theta'
        + 2 m1 m2 L phi'^2 sin(phi-theta)
        - 2g m1(m1+m2)sin(theta)
        + k m2 (b-r)sin(2(theta-phi)))
        /(2 m1(m1+m2)r)
    r'' = (2 b k m1
       + b k m2
       - 2 k m1 r
       - k m2 r
       + 2 g m1(m1+m2) cos(theta)
        - k m2 (b-r) cos(2(theta-phi))
       + 2 L m1 m2 cos(phi-theta)phi'^2
       + 2 m1^2 r theta'^2
       + 2 m1 m2 r theta'^2)
       / (2 m1 (m1+m2))
    phi'' = k(b-r)sin(phi-theta)/(L m1)


     the variables are:  0,1,2,3,4,5:  theta,theta',r,r',phi,phi'
    vars[0] = theta
    vars[1] = theta'
    vars[2] = r
    vars[3] = r'
    vars[4] = phi
    vars[5] = phi'

  */
  public void evaluate(double[] x, double[] change)  {
    double m2 = m_Mass2.m_Mass;
    double m1 = m_Mass1.m_Mass;
    double L = m_Stick.m_RestLength;
    double k = m_Spring.m_SpringConst;
    double b = m_Spring.m_RestLength;
    change[0] = x[1];
    change[2] = x[3];
    change[4] = x[5];
    /*  theta'' = (-4 m1(m1+m2)r' theta'
        + 2 m1 m2 L phi'^2 sin(phi-theta)
        - 2g m1(m1+m2)sin(theta)
        + k m2 (b-r)sin(2(theta-phi))
        /(2 m1(m1+m2)r)
     the variables are:  0,     1,   2,3,  4,  5:
                        theta,theta',r,r',phi,phi'
    */
    double sum = -4*m1*(m1+m2)*x[3]*x[1];
    sum += 2*m1*m2*L*x[5]*x[5]*Math.sin(x[4]-x[0]);
    sum -= 2*gravity*m1*(m1+m2)*Math.sin(x[0]);
    sum += k*m2*(b-x[2])*Math.sin(2*(x[0]-x[4]));
    sum = sum / (2*m1*(m1+m2)*x[2]);
    change[1] = sum;

    /*  r'' = (2 b k m1
         + b k m2
         - 2 k m1 r
         - k m2 r
          - k m2 (b-r) cos(2(theta-phi))
         + 2 L m1 m2 cos(phi-theta)phi'^2 )
         / (2 m1 (m1+m2))
         + r theta'^2
         + g cos(theta);
       the variables are:  0,     1,   2,3,  4,  5:
                          theta,theta',r,r',phi,phi'
    */
    sum = 2*b*k*m1 + b*k*m2 - 2*k*m1*x[2] - k*m2*x[2];
    sum -= k*m2*(b-x[2])*Math.cos(2*(x[0]-x[4]));
    sum += 2*L*m1*m2*Math.cos(x[4]-x[0])*x[5]*x[5];
    sum = sum/(2*m1*(m1+m2));
    sum += x[2]*x[1]*x[1];
    sum += gravity*Math.cos(x[0]);
    change[3] = sum;

    //    phi'' = k(b-r)sin(phi-theta)/(L m1)
    change[5] = k*(b-x[2])*Math.sin(x[4]-x[0])/(L*m1);
  }
}
