/*
  File: PendulumCart.java

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

//    A cart moves without friction on a horizontal track.
//    Suspended from the cart is a pendulum.


package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/////////////////////////////////////////////////////////////////////////////////
public class PendulumCart extends Simulation implements ActionListener
{
  private CMass cart, pendulum;
  private CSpring rod, spring;
  private BarChart chart;
  private boolean showEnergy = true;
  private double gravity = 9.8;
  private double damping_cart = 0.1; // cart damping
  private double damping_pendulum = 0.1; // pendulum damping
  private JButton button_stop;
  private static final String MASS_CART = "cart mass",
      MASS_PENDULUM = "pendulum mass",
      DAMPING_CART = "cart damping",
      DAMPING_PENDULUM = "pendulum damping",
      LENGTH_PENDULUM = "pendulum length",
      STIFFNESS = "spring stiffness",
      GRAVITY = "gravity",
      SHOW_ENERGY = "show energy";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS_CART, MASS_PENDULUM, LENGTH_PENDULUM,
    STIFFNESS, GRAVITY, DAMPING_CART, DAMPING_PENDULUM, SHOW_ENERGY};

  public PendulumCart(Container container) {
    super(container, 5);
    var_names = new String[] {
      "cart position",
      "pendulum angle",
      "cart velocity",
      "angle velocity",
      "work done by damping"
    };
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -3, 3, -2, 2,
          CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    //cvs.getCoordMap().zoom(-1, 1, -1, 1);
    double len = 1;
    // rod = pendulum stick
    // x1, y1, restLen, thickness, drawing mode
    rod = new CSpring (0, 0, len, 0.4);  // x1, y1, restLen, thickness
    rod.m_DrawMode = CElement.MODE_LINE;
    rod.m_SpringConst=6;
    cvs.addElement(rod);

    double w = 0.3;
    // mass1 = cart
    // x1, y1, width, height, drawmode
    cart = new CMass(-w/2, -w/2, w, w, CElement.MODE_RECT);
    cart.m_Mass = 1;
    cvs.addElement(cart);

    // mass2 = pendulum bob
    pendulum = new CMass( -w/2 - Math.sin(0)*len,
               w/2 - Math.cos(0)*len, w, w, CElement.MODE_CIRCLE);
    rod.setX2(pendulum.m_X2 + w/2);
    rod.setY2(pendulum.m_Y2 + w/2);
    pendulum.m_Mass = 1;
    pendulum.m_Damping = 0;
    cvs.addElement(pendulum);

    // spring = spring attached to cart
    // x1, y1, restLen, thickness, drawing mode
    spring = new CSpring (3, 0, 3, 0.4);
    spring.m_SpringConst = 6;
    cvs.addElement(spring);
    chart = new BarChart(cvs.getSimBounds());
    chart.tes = "kinetic energy";
    if (showEnergy)
      cvs.addElement(chart);

    stop();
    vars[1] = Math.PI/8;
    modifyObjects();
    initWork();
  }

  public void setupControls() {
    super.setupControls();
    addControl(button_stop = new JButton("reset"));
    button_stop.addActionListener(this);
    // DoubleField params:  subject, name, fraction digits
    for (int i=0; i<params.length-1; i++)
      addObserverControl(new DoubleField(this, params[i], 2));
    addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
    showControls(true);
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null) {
      graph.setVars(0,1);
      showGraph(true);
    }
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS_CART))
      {cart.m_Mass = value; initWork(); return true;}
    else if (name.equalsIgnoreCase(MASS_PENDULUM))
      {pendulum.m_Mass = value; initWork(); return true;}
    else if (name.equalsIgnoreCase(DAMPING_CART))
      {damping_cart = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING_PENDULUM))
      {damping_pendulum = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS))
      {spring.m_SpringConst = value; initWork(); return true;}
    else if (name.equalsIgnoreCase(LENGTH_PENDULUM))
      {rod.m_RestLength = value; initWork(); return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; initWork(); return true;}
    else if (name.equalsIgnoreCase(SHOW_ENERGY)) {
      showEnergy = value != 0;
      boolean chartVisible = cvs.containsElement(chart);
      if (showEnergy && !chartVisible)
        cvs.addElement(chart);
      else if (!showEnergy && chartVisible)
        cvs.removeElement(chart);
      return true;
    }
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS_CART))
      return cart.m_Mass;
    else if (name.equalsIgnoreCase(MASS_PENDULUM))
      return pendulum.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING_CART))
      return damping_cart;
    else if (name.equalsIgnoreCase(DAMPING_PENDULUM))
      return damping_pendulum;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return spring.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH_PENDULUM))
      return rod.m_RestLength;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    else if (name.equalsIgnoreCase(SHOW_ENERGY))
      return showEnergy ? 1 : 0;
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
    for (int i=0; i<vars.length; i++)
      vars[i] = 0;
  }

  public void setVariable(int i, double value) {
    super.setVariable(i, value);
    initWork();
  }

  private void initWork() {
    vars[4] = 0;
    calcEnergy();
    chart.setWorkZero(chart.te + chart.pe);
  }

  private void calcEnergy() {
    chart.te = 0.5*cart.m_Mass*vars[2]*vars[2];
    double csh = Math.cos(vars[1]);
    double snh = Math.sin(vars[1]);
    double d1 = vars[2] + rod.m_RestLength*vars[3]*csh;
    double d2 = rod.m_RestLength*vars[3]*snh;
    chart.te += 0.5*pendulum.m_Mass*(d1*d1 + d2*d2);
    chart.pe = 0.5*spring.m_SpringConst*vars[0]*vars[0];
    chart.pe += pendulum.m_Mass*gravity*rod.m_RestLength*(1-csh);
    chart.work = vars[4];
  }

  public void modifyObjects()  {
    // set the position of the pendulum according to the angle
    //   the variables are:  0,1,2,3:  x,h(theta),v=x',w=h'
    double w = cart.m_Width/2;
    cart.setX1(vars[0] - w);
    double L = rod.m_RestLength;
    pendulum.setX1(cart.m_X1 + L*Math.sin(vars[1]));
    pendulum.setY1(cart.m_Y1 - L*Math.cos(vars[1]));
    // pendulum rod, Y1 is fixed at zero
    rod.setX1(cart.m_X1+w);
    rod.setX2(pendulum.m_X1+w);
    rod.setY2(pendulum.m_Y1+w);
    // the actual spring
    spring.setX2(cart.m_X1+w);
    calcEnergy();
  }

  public void startDrag(Dragable e)  {
    // can't do "live dragging" because everything is too connected!
    for (int i=0; i<vars.length; i++)
      calc[i] = false;
  }

  public void constrainedSet(Dragable e, double x, double y)  {
    //   the variables are:  0,1,2,3:  x,h(theta),v=x',w=h'
    double w = cart.m_Width/2;
    if (e==cart) {  // dragging the cart
      vars[0] = x + w;
      vars[2] = 0;
      initWork();
    } else if (e==pendulum) {  // dragging the pendulum mass
      // get center of mass1
      double x1 = vars[0];
      double y1 = -w;
      // get center of mass2  (x,y correspond to m_X1,m_Y1 = topleft)
      double x2 = x + w; // coords of center
      double y2 = y;
      double th = Math.atan2(x2-x1, -(y2-y1));
      vars[1] = th;
      vars[3] = 0;
      initWork();
    }
  }

  /*
    A cart moves without friction on a horizontal track.
    Suspended from the cart is a pendulum.

     x = position of cart (vertical position is zero)
    when x=0, the spring is relaxed.
    v = x'
    h = angle (vertical is zero, counterclockwise is positive)
    w = h'
    x2 = horiz position of pendulum
    y2 = vertical position of pendulum (y2 increases downwards)
    M = mass of cart
    m = mass of pendulum
    T = tension in rod
    L = length of rod
    d = cart damping
    b = pendulum damping

     diff eqs:  messy because I had to get rid of 2nd derivs on RHS!!!
    x' = v
    h' = w
    v' = (m w^2 L sin(h) + m g sin(h) cos(h) - k x)/(M + m sin^2(h))
    w' = [-m w^2 L sin(h) cos(h) + k x cos(h) - (M+m)g sin(h)]/(L(M + m sin^2(h))

    with damping these are different!
                                    2
{{xpp -> (-2 (k x + d xp) + 2 m R hp  Sin[h] + ,

       g m Sin[2 h]) / (m + 2 M - m Cos[2 h])

   hpp -> (b m hp + 2 b M hp -

        2 m R (k x + d xp) Cos[h] - b m hp Cos[2 h] +

             2
        2 g m  R Sin[h] + 2 g m M R Sin[h] +          }}

         2  2   2
        m  R  hp  Sin[2 h]) /

          2
      (m R  (-m - 2 M + m Cos[2 h]))

     the variables are:  0,1,2,3:  x,h(theta),v,w
    vars[0] = x  (position of cart)
    vars[1] = h  (theta, angle of pendulum)
    vars[2] = v = x'  (velocity of cart)
    vars[3] = w = h'  (angular velocity of pendulum)
  */

  public void evaluate(double[] x, double[] change)
  {
    double m = pendulum.m_Mass; // pendulum mass
    double M = cart.m_Mass; // cart mass
    double L = rod.m_RestLength;  // length of pendulum rod
    double k = spring.m_SpringConst;
    double sh = Math.sin(x[1]);  // sin(h)
    double csh = Math.cos(x[1]); // cos(h)
    double cs2h = csh*csh - sh*sh; // cos(2h)

    change[0] = x[2];  //x' = v
    change[1] = x[3];  //h' = w
    /*  without damping:
    //v' = (m w^2 L sin(h) + m g sin(h) cos(h) - k x)/(M + m sin^2(h))
    double numer = m*x[3]*x[3]*L*sh + m*gravity*sh*csh - k*x[0];
    double denom = M + m*sh*sh;
    change[2] = numer/denom;

    //w' = [-m w^2 L sin(h) cos(h) + k x cos(h) - (M+m)g sin(h)]/[L(M + m sin^2(h))]
    numer = -m*x[3]*x[3]*L*sh*csh + k*x[0]*csh - (M+m)*gravity*sh;
    denom = L*(M + m*sh*sh);
    change[3] = numer/denom;
    */
    // with damping:
    //v' = (m w^2 L sin(h) + m g sin(h) cos(h) - k x - d v + b w cos(h)/L)/(M + m sin^2(h))
    double numer = m*x[3]*x[3]*L*sh + m*gravity*sh*csh - k*x[0]
          - damping_cart*x[2] + damping_pendulum*x[3]*csh/L;
    change[2] = numer/(M + m*sh*sh);
    //w' = [-m w^2 L sin(h) cos(h) + k x cos(h) - (M+m)g sin(h) + d v cos(h)
    //      -(m+M)b w / (m*L)]/[L(M + m sin^2(h))]
    numer = -m*x[3]*x[3]*L*sh*csh + k*x[0]*csh - (M+m)*gravity*sh + damping_cart*x[2]*csh;
    numer += -(m+M)*damping_pendulum*x[3]/(m*L);
    change[3] = numer/(L*(M + m*sh*sh));
    change[4] = -damping_cart*x[2]*x[2] -damping_pendulum*x[3]*x[3];
  }
}
