/*
  File: Roller3.java

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

/////////////////////////////////////////////////////////////////////////////
// CRoller3 class
//
// Rollercoaster -- 2 balls along a curved track, with spring connecting them.
//
/*
  4 variables:
  x[0] = p1  -- position of ball 1
  x[1] = v1  -- velocity of ball 1
  x[2] = p2  -- position of ball 2
  x[3] = v2  -- velocity of ball 2

*/


public class Roller3 extends Simulation implements ObjectListener {
  private CMass m_Mass1;
  private CMass m_Mass2;
  private CSpring m_Spring;
  private CBitmap m_TrackBM;  // track bitmap
  private double gravity = 9.8;
  private CPath m_Path;
  private CPoint m_Point1 = new CPoint(0);
  private CPoint m_Point2 = new CPoint(1);
  private int m_Path_Num = 0;
  protected CText m_Text = null;
  protected boolean showEnergy = false;
  protected static final String RED_MASS = "red mass",
      BLUE_MASS = "blue mass",
      DAMPING = "damping",
      GRAVITY = "gravity",
      PATH = "path",
      SHOW_ENERGY = "show energy",
      STIFF = "spring stiffness",
      LENGTH = "spring restlength";

  public Roller3(Container app, int the_path)  {
    super(app, 4);  // 4 variables
    // CoordMap inputs are direction, x1, x2, y1, y2, align_x, align_y
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, 0, 1,
        0, 1, CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

    var_names = new String[] {
      "position red",
      "velocity red",
      "position blue",
      "velocity blue"
      };

    m_Text = new CText(0, 0, "energy ");
    if (showEnergy)
      cvs.addElement(m_Text);

    double w = 0.3;
    // red ball is 1
    m_Mass1 = new CMass(1, 1, w, w, CElement.MODE_CIRCLE_FILLED); // x1, y1, width, height, mode
    m_Mass1.m_Mass = 0.5;
    double damp = 0.001;
    m_Mass1.m_Damping = damp;
    cvs.addElement(m_Mass1);

    m_Spring = new CSpring (0, 0, 1, 0.5); // x1, y1, restLen, thickness
    m_Spring.m_SpringConst=5;
    cvs.addElement(m_Spring);

    // blue ball is 2
    m_Mass2 = new CMass(0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
    m_Mass2.m_Mass = 0.5;
    m_Mass2.m_Damping = damp;
    m_Mass2.m_Color = Color.blue;
    cvs.addElement(m_Mass2);

    set_path(the_path);
    modifyObjects();
    cvs.setObjectListener(this);
  }

  public void setupControls() {
    super.setupControls();
    // create popup menu for paths
    // listener, name, value, minimum, choice strings
    addObserverControl(new MyChoice(this, PATH, m_Path_Num, 0, PathName.getPathNames()));
    // DoubleField params:  subject, name, fraction digits
    addObserverControl(new DoubleField(this, RED_MASS, 2));
    addObserverControl(new DoubleField(this, BLUE_MASS, 2));
    addObserverControl(new DoubleField(this, DAMPING, 3));
    addObserverControl(new DoubleField(this, GRAVITY, 2));
    addObserverControl(new DoubleField(this, LENGTH, 2));
    addObserverControl(new DoubleField(this, STIFF, 2));
    addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
    showControls(true);
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null)
      graph.setVars(0,2);
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(RED_MASS))
      {m_Mass1.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(BLUE_MASS))
      {m_Mass2.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {m_Mass1.m_Damping = m_Mass2.m_Damping = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH))
      {m_Spring.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(STIFF))
      {m_Spring.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(PATH))
      {set_path((int)value);
      modifyObjects();
      return true;}
    else if (name.equalsIgnoreCase(SHOW_ENERGY)) {
      boolean wantEnergy = value!=0;
      if (wantEnergy && !showEnergy)
        cvs.addElement(m_Text);
      else if (!wantEnergy && showEnergy)
        cvs.removeElement(m_Text);
      showEnergy = wantEnergy;
      return true;
    }
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(RED_MASS))
      return m_Mass1.m_Mass;
    else if (name.equalsIgnoreCase(BLUE_MASS))
      return m_Mass2.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return m_Mass1.m_Damping;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    else if (name.equalsIgnoreCase(LENGTH))
      return m_Spring.m_RestLength;
    else if (name.equalsIgnoreCase(STIFF))
      return m_Spring.m_SpringConst;
    else if (name.equalsIgnoreCase(PATH))
      return m_Path_Num;
    else if (name.equalsIgnoreCase(SHOW_ENERGY))
      return showEnergy ? 1.0 : 0.0;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    String[] params = {RED_MASS, BLUE_MASS, DAMPING, GRAVITY, PATH,
      SHOW_ENERGY, STIFF, LENGTH};
    return params;
  }

  protected void resetTrackBitmap() {
    if (m_TrackBM != null)
      cvs.removeElement(m_TrackBM);
    m_TrackBM = new CBitmap(container, m_Path);
    // We want to be able to draw into the bitmap using same simulation
    // coordinates and same ConvertMap as we use for drawing into
    // the SimCanvas.  Therefore, we need to offset the topleft corner
    // of the drawing coordinates (= the Graphics origin for the bitmap).
    Rectangle r = cvs.getConvertMap().getScreenRect();
    m_TrackBM.setGraphicsTopLeft(r.x, r.y);
    cvs.prependElement(m_TrackBM);
  }

  protected void set_path(int the_path)  {
    PathName[] pNames = PathName.getPathNames();
    if (the_path >= 0 && the_path < pNames.length) {
      m_Path_Num = the_path;
      m_Path = CPath.makePath(pNames[the_path]);
      cvs.getCoordMap().setRange(m_Path.left, m_Path.right, m_Path.bottom, m_Path.top);
      resetTrackBitmap();
      if (graph != null)
        graph.reset();
      DoubleRect r = cvs.getCoordMap().getSimBounds();
      m_Text.setX1(r.getXMin() + r.getWidth()*0.1);
      m_Text.setY1(r.getYMax() - r.getHeight()*0.1);
      /* find closest starting point to a certain x-y position on screen */
      vars[0] = m_Path.map_x_y_to_p(r.getXMin() + r.getWidth()*0.1,
            r.getYMax() - r.getHeight()*0.1);
      vars[1] = 0;
      vars[2] = m_Path.map_x_y_to_p(r.getXMin() + r.getWidth()*0.2,
            r.getYMax() - r.getHeight()*0.3);
      vars[3] = 0;
    } else
      throw new IllegalArgumentException("no such path number "+the_path);
  }

  // when SimCanvas is resized, this will be called to let us know.
  public void objectChanged(Object o) {
    if (cvs == o)
      resetTrackBitmap();
  }

  public void modifyObjects()  {
    vars[0] = m_Path.modp(vars[0]);
    m_Point1.p = vars[0];
    m_Path.map_p_to_slope(m_Point1);
    m_Mass1.setCenterX(m_Point1.x);
    m_Mass1.setCenterY(m_Point1.y);
    m_Spring.setX2(m_Point1.x);
    m_Spring.setY2(m_Point1.y);
    vars[2] = m_Path.modp(vars[2]);
    m_Point2.p = vars[2];
    m_Path.map_p_to_slope(m_Point2);
    m_Mass2.setCenterX(m_Point2.x);
    m_Mass2.setCenterY(m_Point2.y);
    m_Spring.setX1(m_Point2.x);
    m_Spring.setY1(m_Point2.y);
    m_Text.setNumber(getEnergy());
  }

  protected double getEnergy()  {
    // WARNING:  assumes that current x-y position of m_Mass1 & m_Spring is correct!
    // kinetic energy is 1/2 m v^2
    double e = 0.5*m_Mass1.m_Mass*vars[1]*vars[1];
    e += 0.5*m_Mass2.m_Mass*vars[3]*vars[3];
    // gravity potential = m g y
    e += m_Mass1.m_Mass*gravity*m_Mass1.getCenterY();
    e += m_Mass2.m_Mass*gravity*m_Mass2.getCenterY();
    // WARNING:  this assumes that current position of spring is correct!
    e += m_Spring.getEnergy();
    return e;
  }

  public void constrainedSet(Dragable e, double x, double y)  {
    if (e==m_Mass1) {
       // x,y correspond to the new m_X1, m_Y1 of the object
       // We want to work with the center of the object,
       // so adjust to xx,yy as follows.
      double w = m_Mass1.m_Width/2;
      vars[0] = m_Path.map_x_y_to_p(x+w, y+w);
      vars[1] = 0;  // velocity
      modifyObjects();  // because we've turned off animation during dragging
    } else if (e==m_Mass2) {
       // x,y correspond to the new m_X1, m_Y1 of the object
       // We want to work with the center of the object,
       // so adjust to xx,yy as follows.
      double w = m_Mass2.m_Width/2;
      vars[2] = m_Path.map_x_y_to_p(x+w, y+w);
      vars[3] = 0;  // velocity
      modifyObjects();  // because we've turned off animation during dragging
    }
  }

  public void startDrag(Dragable e)  {
    if (e==m_Mass1) {
      calc[0] = false;
      calc[1] = false;
    } else if (e==m_Mass2) {
      calc[2] = false;
      calc[3] = false;
    }
  }

  public void evaluate(double[] x, double[] change) {
    // FIRST BALL.
    change[0] = x[1];  // p1' = v1
    // calculate the slope at the given arc-length position on the curve
    // x[0] is p = path length position.
    m_Point1.p = x[0];
    m_Path.map_p_to_slope(m_Point1);
    double k = m_Point1.slope;
    // see Mathematica file "roller.nb" for derivation of the following
    // let k = slope of curve. Then sin(theta) = k/sqrt(1+k^2)
    // Component due to gravity is v' = - g sin(theta) = - g k/sqrt(1+k^2)
    double sinTheta = Double.isInfinite(k) ? 1 : k/Math.sqrt(1+k*k);
    change[1] = -gravity*m_Point1.direction*sinTheta;
    // add friction damping:  - (b/m)*x[1]
    change[1] -= m_Mass1.m_Damping*x[1]/m_Mass1.m_Mass;
    // Let sx, sy be the x & y components of the spring length.
    // NOTE important to use particle position here, not the
    // data stored in the m_Spring, because the position of particle
    // is changed during the RK solver procedure
    m_Point2.p = x[2];
    m_Path.map_p_to_slope(m_Point2);   // find position of the other ball
    double sx = m_Point2.x - m_Point1.x;
    double sy = m_Point2.y - m_Point1.y;
    double slen = Math.sqrt(sx*sx + sy*sy);
    double cosTheta;
    if (Double.isInfinite(k))
      cosTheta = m_Point1.direction*sy/slen;
    else
      cosTheta = m_Point1.direction*(sx + k*sy)/(slen * Math.sqrt(1+k*k));
    if (cosTheta > 1 || cosTheta < -1)
      System.out.println("cosTheta out of range in evaluate");
    // stretch amount of spring is
    double stretch = slen - m_Spring.m_RestLength;
    // Then component due to spring is */
    change[1] += cosTheta*stretch*m_Spring.m_SpringConst/m_Mass1.m_Mass;

    // SECOND BALL.
    change[2] = x[3];  // p2' = v2
    // calculate the slope at the given arc-length position on the curve
    k = m_Point2.slope;
    // see Mathematica file "roller.nb" for derivation of the following
    // let k = slope of curve. Then sin(theta) = k/sqrt(1+k^2)
    // Component due to gravity is v' = - g sin(theta) = - g k/sqrt(1+k^2)
    sinTheta = Double.isInfinite(k) ? 1 : k/Math.sqrt(1+k*k);
    change[3] = -gravity*m_Point2.direction*sinTheta;
    // add friction damping:  - (b/m)*x[3]
    change[3] -= m_Mass2.m_Damping*x[3]/m_Mass2.m_Mass;
    // reverse sign for spring, since we are looking at it from other ball
    sx = -sx;
    sy = -sy;
    if (Double.isInfinite(k))
      cosTheta = m_Point2.direction*sy/slen;
    else
      cosTheta = m_Point2.direction*(sx + k*sy)/(slen * Math.sqrt(1+k*k));
    if (cosTheta > 1 || cosTheta < -1)
      System.out.println("cosTheta out of range in evaluate");
    // Then component due to spring is
    change[3] += cosTheta*stretch*m_Spring.m_SpringConst/m_Mass2.m_Mass;
  }
}
