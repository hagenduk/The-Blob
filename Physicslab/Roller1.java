/*
  File: Roller1.java

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

/////////////////////////////////////////////////////////////////////////////////
// Although Roller1 doesn't deal with collisions, Roller1 extends CollidingSim
// so that a subclass can handle collisions.
public class Roller1 extends CollidingSim implements ObjectListener {
  protected CMass m_Mass1;
  protected CBitmap m_TrackBM = null;  // track bitmap
  protected double gravity = 9.8;
  protected CPath m_Path = null;
  protected CPoint m_Point = new CPoint();
  protected CText m_Text = null;
  protected int m_Path_Num = 0;
  protected boolean showEnergy = false;
  protected static final String MASS = "mass", DAMPING = "damping",
      GRAVITY = "gravity", PATH = "path",
      SHOW_ENERGY = "show energy";
  protected MyChoice pathControl;

  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS, DAMPING, GRAVITY, PATH, SHOW_ENERGY};

  public Roller1(Container app, int the_path)
  {
    super(app, 2);
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, 0, 1,
        0, 1, CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    var_names = new String[] {
      "position",
      "velocity"
      };
    createElements();
    set_path(the_path);
    modifyObjects();
    cvs.setObjectListener(this);
  }

  public void setupControls() {
    super.setupControls();
    // create popup menu for paths
    // listener, name, value, minimum, choice strings
    addObserverControl(pathControl =
        new MyChoice(this, PATH, m_Path_Num, 0, PathName.getPathNames()));
    // DoubleField params:  subject, name, fraction digits
    addObserverControl(new DoubleField(this, MASS, 2));
    addObserverControl(new DoubleField(this, DAMPING, 2));
    addObserverControl(new DoubleField(this, GRAVITY, 2));
    addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
    showControls(true);
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null)  graph.setVars(0,1);
  }

  protected void createElements() {
    m_Text = new CText(0, 0, "energy ");
    if (showEnergy)
      cvs.addElement(m_Text);
    // CMass parameters:  x1, y1, width, height, mode
    m_Mass1 = new CMass(1, 1, 0.3, 0.3, CElement.MODE_CIRCLE_FILLED);
    m_Mass1.m_Mass = 0.5;
    m_Mass1.m_Damping = 0;
    cvs.addElement(m_Mass1);
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS))
      {m_Mass1.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {m_Mass1.m_Damping = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
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
    if (name.equalsIgnoreCase(MASS))
      return m_Mass1.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return m_Mass1.m_Damping;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    else if (name.equalsIgnoreCase(PATH))
      return m_Path_Num;
    else if (name.equalsIgnoreCase(SHOW_ENERGY))
      return showEnergy ? 1.0 : 0.0;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
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
      DoubleRect r = cvs.getConvertMap().getSimBounds();
      m_Text.setX1(r.getXMin() + r.getWidth()*0.1);
      m_Text.setY1(r.getYMax() - r.getHeight()*0.1);
      //m_Mass1.setCenterX(m_Path.tlo+0.2);
      //m_Mass1.setCenterY(m_Path.y_func(m_Path.tlo+0.2));
      /* find closest starting point to a certain x-y position on screen */
      vars[0] = m_Path.map_x_y_to_p(r.getXMin() + r.getWidth()*0.1,
            r.getYMax() - r.getHeight()*0.1);
      vars[1] = 0;
    } else
      throw new IllegalArgumentException("no such path number "+the_path);
  }

  // when SimCanvas is resized, this will be called to let us know.
  public void objectChanged(Object o) {
    if (cvs == o)
      resetTrackBitmap();
  }

  public void modifyObjects() {
    vars[0] = m_Path.modp(vars[0]);
    m_Point.p = vars[0];
    m_Path.map_p_to_slope(m_Point);
    m_Mass1.setCenterX(m_Point.x);
    m_Mass1.setCenterY(m_Point.y);
    m_Text.setNumber(getEnergy());
  }

  protected double getEnergy()  {
    // WARNING:  assumes that current x-y position of m_Mass1 & m_Spring is correct!
    // kinetic energy is 1/2 m v^2
    double e = 0.5*m_Mass1.m_Mass*vars[1]*vars[1];
    // gravity potential = m g y
    e += m_Mass1.m_Mass*gravity*m_Mass1.getCenterY();
    return e;
  }

  public void startDrag(Dragable e)  {
    if (e == m_Mass1) {
      calc[0] = false;
      calc[1] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y) {
    if (e==m_Mass1)  {
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
    // let k = slope of curve. Then sin(theta) = k/sqrt(1+k^2)
    // v' = - g sin(theta) = - g k/sqrt(1+k^2)
    double sinTheta = Double.isInfinite(k) ? 1 : k/Math.sqrt(1+k*k);
    change[1] = -gravity*m_Point.direction*sinTheta;
    // add friction damping:  - b*x[1]
    change[1] -= m_Mass1.m_Damping*x[1]/m_Mass1.m_Mass;
  }

  public Vector findAllCollisions() {
    return null;
  }

  public void handleCollisions(Vector collisions) {
  }

}
