/*
  File: MoveableDoublePendulum.java

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


////////////////////////////////////////////////////////////////////
// MoveableDoublePendulumCanvas implements special mouse and key handling for the
// MoveableDoublePendulum simulation.
// The mouse handling allows for "rubber band force" dragging,
// where we find the nearest object and then track the current mouse
// location.

class MoveableDoublePendulumCanvas extends SimCanvas {

  public MoveableDoublePendulumCanvas(MouseDragHandler mdh) {
    super(mdh);
  }

  public void mousePressed(MouseEvent evt) {
    int scr_x = evt.getX();  // screen coords
    int scr_y = evt.getY();
    // which object did mouse click on?
    double sim_x = map.screenToSimX(scr_x);  // simulation coords
    double sim_y = map.screenToSimY(scr_y);
    dragObj = findNearestDragable(sim_x, sim_y);
    if (dragObj != null) {
      if (mdh!=null) {
        mdh.startDrag(dragObj);
        mdh.constrainedSet(dragObj, sim_x, sim_y);
      }
    }
  }

  public void mouseDragged(MouseEvent evt) {
    if (dragObj != null) {
      double sim_x = map.screenToSimX(evt.getX());
      double sim_y = map.screenToSimY(evt.getY());
      // let the simulation modify the object
      if (mdh!=null) mdh.constrainedSet(dragObj, sim_x, sim_y);
    }
  }

  // keyPressed is where we can capture control keys like backspace & enter
  public void keyPressed(KeyEvent e) {
    //System.out.println("keyPressed "+e);
    int keyCode = e.getKeyCode();
   // ((Thruster5)mdh).handleKeyEvent(keyCode, true);
  }

  public void keyReleased(KeyEvent e) {
    //System.out.println("keyReleased "+e);
    int keyCode = e.getKeyCode();
    //((Thruster5)mdh).handleKeyEvent(keyCode, false);
  }

  public void mouseEntered(MouseEvent evt) {
    //System.out.println("mouseEntered ThrusterCanvas");
    requestFocus();
  }

  protected void drawElements(Graphics g, ConvertMap map) {
    super.drawElements(g, map);
    ((MoveableDoublePendulum)mdh).drawRubberBand(g, map);
  }
}


public class MoveableDoublePendulum extends Simulation implements ActionListener
{
  private CMass m_Mass1, m_Mass2, topMass;
  private CSpring m_Stick1, m_Stick2;
  private double gravity = 9.8;
  private JButton button_stop;
  private boolean mouseDown = false;
  private double mouseX = 0;
  private double mouseY = 0;
  private double damping1 = 0.5;
  private double damping2 = 0.5;
  private double stiffness = 3;
  private double anchorDamping = 0.8;
  private static final String MASS1 = "mass1",
      MASS2 = "mass2", LENGTH1 = "stick1 length", LENGTH2 = "stick2 length",
      GRAVITY = "gravity", DAMPING1 = "damping1", DAMPING2 = "damping2",
      STIFFNESS = "mouse spring stiffness", ANCHOR_DAMPING = "anchor damping";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS1, MASS2, LENGTH1, LENGTH2, GRAVITY, 
    DAMPING1, DAMPING2, STIFFNESS, ANCHOR_DAMPING};

  public MoveableDoublePendulum(Container container) {
    super(container, 9);
    var_names = new String[] {
      "angle1",
      "angle1 velocity",
      "angle2",
      "angle2 velocity",
      "time",
      "anchorX",
      "anchorX velocity",
      "anchorY",
      "anchorY velocity"
    };
    // x range was -2 to 2
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -4, 4, -2.2, 1.5,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

    double xx = 0, yy = 0, w = 0.5;
    topMass = new CMass(xx-w/2, yy-w/2, w, w, CElement.MODE_RECT);
    topMass.m_Color = Color.red;
    cvs.addElement(topMass);

    // x1, y1, restLen, thickness
    m_Stick1 = new CSpring (0, 0, 1, 0.4);
    m_Stick1.m_DrawMode = CElement.MODE_LINE;
    cvs.addElement(m_Stick1);

    // x1, y1, restLen, thickness
    m_Stick2 = new CSpring (0, 0, 1, 0.4);
    m_Stick2.m_DrawMode = CElement.MODE_LINE;
    cvs.addElement(m_Stick2);

    w = 0.2;
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

    //vars[0]=Math.PI/8;
    vars[0] = vars[1] = vars[2] = vars[3] = 0;
    vars[4] = 0; // time
    vars[5] = 0;  // x0 = anchor X
    vars[6] = 0;  // x0' = anchor X velocity
    vars[7] = 0;  // y0 = anchor Y
    vars[8] = 0;  // y0' = anchor Y velocity
    modifyObjects();
  }

  // A "Factory Method" pattern (see Design Patterns book).
  // Allows this class to specify what is actually instantiated by
  // some other class.
  protected SimCanvas makeSimCanvas() {
    return new MoveableDoublePendulumCanvas(this);
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

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) { 
      vars[0] = vars[1] = vars[2] = vars[3] = 0; 
      vars[4] = 0; // time
      vars[5] = 0;  // x0 = anchor X
      vars[6] = 0;  // x0' = anchor X velocity
      vars[7] = 0;  // y0 = anchor Y
      vars[8] = 0;  // y0' = anchor Y velocity
    }
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null) {
      graph.setVars(0,2);
      graph.setDrawMode(Graph.DOTS);
    }
  }

  protected void setValue(int param, double value) {
    switch (param) {
      case 0: m_Mass1.m_Mass = value; break;
      case 1: m_Mass2.m_Mass = value; break;
      case 2: m_Stick1.m_RestLength = value; break;
      case 3: m_Stick2.m_RestLength = value; break;
      case 4: gravity = value; break;
      case 5: damping1 = value; break;
      case 6: damping2 = value; break;
      case 7: stiffness = value; break;
      case 8: anchorDamping = value; break;
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
    else if (name.equalsIgnoreCase(DAMPING1))
      {damping1 = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING2))
      {damping2 = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS))
      {stiffness = value; return true;}
    else if (name.equalsIgnoreCase(ANCHOR_DAMPING))
      {anchorDamping = value; return true;}
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
    else if (name.equalsIgnoreCase(DAMPING1))
      return damping1;
    else if (name.equalsIgnoreCase(DAMPING2))
      return damping2;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return stiffness;
    else if (name.equalsIgnoreCase(ANCHOR_DAMPING))
      return anchorDamping;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }
  
  double period = 5;
  
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
    
    //topMass.setPosition(getAnchorX(vars[4]), 0);
    // x1, y1 is bottom left, because we have y increasing upwards.
    topMass.setPosition(vars[5] - topMass.m_Width/2, vars[7] - topMass.m_Height/2);
    double x0 = topMass.m_X1 + topMass.m_Width/2;  // center of anchor cube
    double y0 = topMass.m_Y1 + topMass.m_Height/2; // center of anchor cube
    //rememberAnchorPosition(x0, y0, getTime());
    double w = m_Mass1.m_Width/2;
    double L1 = m_Stick1.m_RestLength;
    double L2 = m_Stick2.m_RestLength;
    double th1 = vars[0];
    double th2 = vars[2];
    double x1 = x0 + L1*Math.sin(th1);
    double y1 = y0 - L1*Math.cos(th1);
    double x2 = x1 + L2*Math.sin(th2);
    double y2 = y1 - L2*Math.cos(th2);
    m_Stick1.setBounds(x0,y0,x1,y1);
    m_Mass1.setPosition(x1-w, y1-w);
    m_Stick2.setBounds(x1,y1,x2,y2);
    m_Mass2.setPosition(x2-w, y2-w);
  }

  public void startDrag(Dragable e) {
    if (e==topMass)
      this.mouseDown = true;
    // can't do "live dragging" because everything is too connected!
    if ((e == m_Mass1) || (e == m_Mass2)) {
      calc[0] = calc[1] = calc[2] = calc[3] = false;
    }
  }

  public void finishDrag(Dragable e) {
    super.finishDrag(e);
    if (e==topMass)
      this.mouseDown = false;
  }
  
  
  public void constrainedSet(Dragable e, double x, double y)
  {
    //double x0 = topMass.m_X1 + topMass.m_Width/2;  // center of anchor cube
    double x0 = vars[5];
    double y0 = vars[7];  // center of anchor cube
    double w = m_Mass1.m_Width/2;
        
    if (e==topMass) {
      this.mouseX = x;
      this.mouseY = y;
      //System.out.println("mouse at "+this.mouseX+"  "+this.mouseY);
      //e.setPosition(x, y);
      //modifyObjects();
    } else if (e==m_Mass1) {
      //the variables are:  0,1,2,3:  theta1,theta1',theta2,theta2'
      // x,y correspond to the new m_X1, m_Y1 of the object
      // We want to work with the center of the object,
      // so adjust to xx,yy as follows.
      double xx = (x-x0) + w; // coords of center
      double yy = (y-y0) + w;
      double th1 = Math.atan2(xx, -yy);
      vars[0] = th1;
      vars[1] = 0;  // theta1'
      vars[3] = 0; // theta2'
      modifyObjects();
    } else if (e==m_Mass2) {
      double L1 = m_Stick1.m_RestLength;
      double L2 = m_Stick2.m_RestLength;
      // get center of mass1
      double x1 = x0 + L1*Math.sin(vars[0]);
      double y1 = y0 - L1*Math.cos(vars[0]);
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
      
   the variables are:  0,1,2,3:  theta1,theta1',theta2,theta2'
  vars[0] = theta1  = th1
  vars[1] = theta1' = dth1
  vars[2] = theta2  = th2
  vars[3] = theta2' = dth2
  vars[4] = time;
  vars[5] = x0 = anchor X
  vars[6] = x0' = anchor X velocity
  vars[7] = y0 = anchor Y
  vars[8] = y0' = anchor Y velocity

      x0'', y0'' = acceleration of upper pivot point on (moveable) anchor
            (how to find out x0'' and y0''?)
      L1,L2 = stick lengths
      m1,m2 = masses
      g = gravity
      theta1,theta2 = angles of sticks with vertical (down = 0)
      th1,th2 = theta1,theta2 abbreviations

      Diff eqs:  see the Mathematica file:  double pendulum 4.nb
      
  ============= Mathematica to Java or Latex =======================
  When you have a complicated result in Mathematica and you want to
  program it into Java or C++ or Latex, here are the steps:
  1. Use //CForm to output in a C-friendly text format
    or //TeXForm for Latex (not sure how well TeXForm works in Latex).
     You can also try //OutputForm, or even try to invent your
     own output format (see Mathematica book sections 2.8.1, 2.8.17)
  2. Select the cell and do "copy as text"
  3. Paste into a text editor and do modifications like changing
    Power(a,2) to a*a.
    NOTE: i have a new automatic way to do this... 
    Unprotect[Power];
    Format[x_^2, CForm] := Format[StringForm["``*``",CForm[x],CForm[x]],OutputForm]
      (This is from the Power Programming book I think).

  4. Copy into Java.  Don't try to break a long expression into
    pieces by storing intermediate results in dummy variables.
    Instead, just let Java do all that for you... give the
    compiler the whole long calculation on multiple lines and
    it will do the right thing!
                              
  */
  public void evaluate(double[] x, double[] change) {
    change[4] = 1; // time
    change[5] = x[6]; // x0 ' = vx0
    change[6] = -this.anchorDamping*x[6] + (this.mouseDown ? this.stiffness*(this.mouseX-x[5]) : 0);  
    change[7] = x[8]; // y0 ' = vy0
    change[8] = -this.anchorDamping*x[8] + (this.mouseDown ? this.stiffness*(this.mouseY-x[7]) : 0);
    double ddx0 = change[6];  // x0''  (x0 prime prime)   WAS: x[6]   ????
    double ddy0 = change[8];  // y0''  (y0 prime prime)
    double th1 = x[0];
    double dth1 = x[1];
    double th2 = x[2];
    double dth2 = x[3];
    double m2 = m_Mass2.m_Mass;
    double m1 = m_Mass1.m_Mass;
    double L1 = m_Stick1.m_RestLength;
    double L2 = m_Stick2.m_RestLength;
    double g = gravity;
    double b = damping1;
    double b2 = damping2;

    change[0] = dth1;
    
    change[1] = -((2*b*dth1 + 
              ddx0*L1*(2*m1 + m2)*Math.cos(th1) - 
              ddx0*L1*m2*Math.cos(th1 - 2*th2) + 
              2*ddy0*L1*m1*Math.sin(th1) + 2*g*L1*m1*Math.sin(th1) + 
              ddy0*L1*m2*Math.sin(th1) + g*L1*m2*Math.sin(th1) + 
              ddy0*L1*m2*Math.sin(th1 - 2*th2) + 
              g*L1*m2*Math.sin(th1 - 2*th2) + 
              2*dth2*dth2*L1*L2*m2*Math.sin(th1 - th2) + 
              dth1*dth1*L1*L1*m2*Math.sin(2*(th1 - th2)))/
            (L1*L1*(2*m1 + m2 - m2*Math.cos(2*(th1 - th2)))));
    
    change[2] = dth2;

    change[3] = -((2*b*dth1*L2*m2*Math.cos(th1 - th2) - 
            b2*(dth1 - dth2)*L1*m2*Math.cos(2*(th1 - th2)) + 
            L1*(2*b2*dth1*m1 - 2*b2*dth2*m1 + b2*dth1*m2 - 
               b2*dth2*m2 + 
               ddx0*L2*m2*(m1 + m2)*Math.cos(2*th1 - th2) - 
               ddx0*L2*m2*(m1 + m2)*Math.cos(th2) + 
               2*dth1*dth1*L1*L2*m1*m2*Math.sin(th1 - th2) + 
               2*dth1*dth1*L1*L2*m2*m2*Math.sin(th1 - th2) + 
               dth2*dth2*L2*L2*m2*m2*Math.sin(2*(th1 - th2)) + 
               ddy0*L2*m1*m2*Math.sin(2*th1 - th2) + 
               g*L2*m1*m2*Math.sin(2*th1 - th2) + 
               ddy0*L2*m2*m2*Math.sin(2*th1 - th2) + 
               g*L2*m2*m2*Math.sin(2*th1 - th2) - 
               ddy0*L2*m1*m2*Math.sin(th2) - 
               g*L2*m1*m2*Math.sin(th2) - 
               ddy0*L2*m2*m2*Math.sin(th2) - g*L2*m2*m2*Math.sin(th2))
            )/
          (L1*L2*L2*m2*(-2*m1 - m2 + m2*Math.cos(2*(th1 - th2))))
          );
  
  }

  public void drawRubberBand(Graphics g, ConvertMap map) {
    // draw the rubberband to mouse position
    if (this.mouseDown) {
      g.setColor(Color.red);
      g.drawLine(map.simToScreenX(this.mouseX), map.simToScreenY(this.mouseY),
        map.simToScreenX(topMass.getCenterX()), map.simToScreenY(topMass.getCenterY()));
    }
  }

}
