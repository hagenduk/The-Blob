/*
  File: Thruster5.java

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
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.*;

////////////////////////////////////////////////////////////////////
// ThrusterCanvas implements special mouse and key handling for the
// Thruster rigid body collision simulation.
// The mouse handling allows for "rubber band force" dragging,
// where we find the nearest object and then track the current mouse
// location.

class ThrusterCanvas extends SimCanvas {

  public ThrusterCanvas(MouseDragHandler mdh) {
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
    ((Thruster5)mdh).handleKeyEvent(keyCode, true);
  }

  public void keyReleased(KeyEvent e) {
    //System.out.println("keyReleased "+e);
    int keyCode = e.getKeyCode();
    ((Thruster5)mdh).handleKeyEvent(keyCode, false);
  }

  public void mouseEntered(MouseEvent evt) {
    //System.out.println("mouseEntered ThrusterCanvas");
    requestFocus();
  }

  protected synchronized void drawElements(Graphics g, ConvertMap map) {
    super.drawElements(g, map);
    ((Thruster5)mdh).drawRubberBand(g, map);
  }
}

//////////////////////////////////////////////////////////////////////////////////////
/* Thruster5 is a simulation of rigid body collisions.
How it works:
The collision handling algorithm is implemented in the super class CollidingSim.
There are 3 main methods called from CollidingSim:  evaluate, findAllCollsions,
and handleCollisions.

evaluate() --  called by the DiffEq solver to calculate forces.  The forces
here are gravity, damping, thrust, and rubber band.  Thrust is like jet thrusters
on a rocket ship.  The thrusters on the object can operate in various directions
from a certain point on the object (see Thruster5Object for details).  The
thrusters are activated by keyboard events like up arrow or down arrow.
The rubber band force is activated by pressing and holding the mouse button.
A rubber band (spring-like) force is temporarily created between the mouse
position and the nearest body.

findAllCollisions() -- returns a vector containing the current collisions.
Checks each corner of each body to see if it is penetrating a wall or another
object.  Creates a Collision object corresponding to each collision found
and returns them all in the vector.

handleCollisions() -- For each collision, calculates the appropriate impulse
that would reverse the collsion.  The physics behind this calculation is
explained on the MyPhysicsLab.com website.

variables:   x, x', y, y', th, th'
  bods[0]      0, 1,  2, 3,  4,  5
  bods[1]      6, 7,  8, 9, 10, 11
  etc...

*/
public class Thruster5 extends CollidingSim implements ActionListener, ObjectListener
{
  public static final double DISTANCE_TOL = 0.01;
  public static final double VELOCITY_TOL = 0.5;
  public static final int RIGHT_WALL=-1, BOTTOM_WALL=-2, LEFT_WALL=-3, TOP_WALL=-4;
  protected static final int MAX_BODIES = 6; // maximum number of bodies
  protected int dragObj = -1;
  protected double mouseX, mouseY;
  protected int numBods;  // number of blocks
  protected Thruster5Object[] bods;  // array of bodies
  protected Vector collisionsFound = new Vector(10);
  protected NumberFormat nf = NumberFormat.getNumberInstance();
  protected double gravity = 0, damping, elasticity=1.0, thrust=0.5;
  protected double m_Left, m_Right, m_Bottom, m_Top;
  protected CRect m_Walls;
  protected boolean debug = false;
  protected boolean doCollisions = true;  // used for debugging contact stuff
  protected boolean showCollisionDot = false;
  protected Vector rxnForces = new Vector(20);
  protected boolean showEnergy = false;
  protected BarChart energyBar;
  protected double zeroEnergyLevel = 0;
  private CText preText, postText;  // displays pre & post-collision momentum
  protected int stuckCounter = 0;
  private CText message = null;  // error message for when simulation is stuck
  protected boolean gameMode = false;
  private int winningHits = 10;
  private int greenHits = 0, blueHits = 0;  // number of times green or blue object hit walls
  private CText greenLabel, blueLabel;  // displays number of wall hits
  private CText message2 = null; // "game over" message
  protected JButton buttonReset;
  protected static final String NUM_BODIES = "number bodies",
      DAMPING = "damping", GRAVITY = "gravity", ELASTICITY = "elasticity",
      THRUST = "thrust",
      SHOW_ENERGY = "show energy";
  // important that the params list of strings remains private, so can't
  // be overridden
  protected String[] params = {NUM_BODIES, DAMPING, GRAVITY, ELASTICITY, THRUST,
      SHOW_ENERGY};


  public Thruster5(Container container, boolean gameMode) {
    super(container);
    this.gameMode = gameMode;
    double w = 5;
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -w, w, -w, w,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    //cvs.expandMap();  // NOTE: the sim boundaries will change later on!!!
    //cvs.setSimMinY(-2.0);

    DoubleRect box = cvs.getSimBounds();
    m_Left = box.getXMin();
    m_Right = box.getXMax();
    m_Bottom = box.getYMin();
    //zeroEnergyLevel = m_Bottom = -4.0;
    zeroEnergyLevel = m_Bottom;
    m_Top = box.getYMax();
    cvs.setObjectListener(this);

    damping = gameMode ? 0.2 : 0;
    numBods = gameMode ? 2: 3;

    reset(); // creates bodies
    //cvs.getCoordMap().zoom(-2.1, -0.9, -3.3, -2.7);
    //graphSetup();    // needs to come after vars is defined!
    //graph.setVars(0,2);

    // try to force the Collision class to pre-load (to avoid delay at first collision)
    Collision c = new Collision();

    // get keyboard focus onto the game at the start.
    if (gameMode) cvs.requestFocus();
  }

  public void setupControls() {
    super.setupControls();
    addControl(buttonReset = new JButton("reset"));
    buttonReset.addActionListener(this);
    if (!gameMode) {
      // create popup menu for number of bodies
      nf.setMinimumFractionDigits(0);
      String[] choices = new String[MAX_BODIES];
      for (int i=0; i<MAX_BODIES; i++)
        choices[i] = nf.format(i+1)+" object"+(i>0 ? "s" : "");
      // listener, name, value, minimum, choice strings
      addObserverControl(new MyChoice(this, NUM_BODIES, numBods, 1, choices));
    }
    // DoubleField params:  listener, name, value, fraction digits
    addObserverControl(new DoubleField(this, ELASTICITY, 2));
    addObserverControl(new DoubleField(this, GRAVITY, 2));
    addObserverControl(new DoubleField(this, DAMPING, 2));
    addObserverControl(new DoubleField(this, THRUST, 2));
    if (!gameMode)
      addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
    showControls(true);

  }

  public void setupGraph() {
    /* no graph here */
  }

  public String getVariableName(int i) {
    /*  variables:   x, x', y, y', th, th'
        bods[0]      0, 1,  2, 3,  4,  5
        bods[1]      6, 7,  8, 9, 10, 11
    */
    int j = i%6;  // % is mod, so j tells what derivative is wanted:
                  // 0=x, 1=x', 2=y, 3=y', 4=th, 5=th'
    int obj = i/6;  // which object: 0, 1
    switch (j) {
      case 0: return "x position "+obj;
      case 1: return "x velocity "+obj;
      case 2: return "y position "+obj;
      case 3: return "y velocity "+obj;
      case 4: return "angle "+obj;
      case 5: return "angular velocity "+obj;
      default: return "";
    }
  }

  public void objectChanged(Object o) {
    if (o == cvs) {
      DoubleRect box = cvs.getSimBounds();
      m_Left = box.getXMin();
      m_Right = box.getXMax();
      zeroEnergyLevel = m_Bottom = box.getYMin();
      //m_Bottom = -4.0;
      m_Top = box.getYMax();
      m_Walls.setBounds(new DoubleRect(m_Left, m_Bottom, m_Right, m_Top));
    }
  }

  public void startDrag(Dragable e) {
    dragObj = -1;
    for (int i=0; i<bods.length; i++)
      if (e == bods[i])
        dragObj = i;
  }

  public void constrainedSet(Dragable e, double x, double y)  {
    mouseX = x;
    mouseY = y;
  }

  public void finishDrag(Dragable e) {
    // super should go back to odeSolver calculation for all vars
    super.finishDrag(e);
    dragObj = -1;
  }

  public void drawRubberBand(Graphics g, ConvertMap map) {
    // draw the rubberband to mouse position
    if (dragObj >= 0) {
      g.setColor(Color.black);
      g.drawLine(map.simToScreenX(mouseX), map.simToScreenY(mouseY),
        map.simToScreenX(bods[dragObj].tx), map.simToScreenY(bods[dragObj].ty));
    }
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(ELASTICITY))
      {elasticity = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {damping = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    else if (name.equalsIgnoreCase(THRUST)) {
      thrust = value; 
	    for (int i=0; i<numBods; i++) {
	      bods[i].tMagnitude = thrust;
			}
			return true;
		} else if (name.equalsIgnoreCase(NUM_BODIES)) {
      numBods = (int)value;
      // Shut down the animation while we change the number of bodies
      // -- otherwise the animation thread may be iterating over the list of
      // bodies and try to read past end of the list.
      // Ideally we should block here until the animation thread is stopped,
      // but that isn't happening now.
      //container.stop();  // stop animation
      m_Animating = false;  // does this help???
      reset();
      m_Animating = true;
      //container.start();  // restart animation
      return true;
    } else if (name.equalsIgnoreCase(SHOW_ENERGY)) {
      showEnergy = value != 0;
      boolean chartVisible = cvs.containsElement(energyBar);
      if (showEnergy && !chartVisible) {
        cvs.prependElement(energyBar);
        if (showMomentum()) {
          cvs.prependElement(preText);
          cvs.prependElement(postText);
        }
      } else if (!showEnergy && chartVisible) {
        cvs.removeElement(energyBar);
        if (showMomentum()) {
          cvs.removeElement(preText);
          cvs.removeElement(postText);
        }
      }
      // "AWT uses validate() to cause a container to lay out its subcomponents
      // again after the components it contains have been added to or modified."
      // Since we added or removed Labels, we need to lay out components.
      container.invalidate();
      container.validate();
      return true;
    }
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(ELASTICITY))
      return elasticity;
    else if (name.equalsIgnoreCase(DAMPING))
      return damping;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    else if (name.equalsIgnoreCase(THRUST))
      return thrust;
    else if (name.equalsIgnoreCase(NUM_BODIES))
      return numBods;
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
    if(e.getSource() == buttonReset) {
      reset();
    }
  }

  // A "Factory Method" pattern (see Design Patterns book).
  // Allows this class to specify what is actually instantiated by
  // some other class.
  protected SimCanvas makeSimCanvas() {
    return new ThrusterCanvas(this);
  }

  // This is an instance of "Factory Method" pattern (see the Design Patterns book)
  // so that a sub class can create objects of a different class.
  protected Thruster5Object createBlock(double width, double height) {
    return new Thruster5Object(width, height);
  }

  protected synchronized void reset() {
    cvs.removeAllElements();
    message2 = null;
    m_Walls = new CRect(new DoubleRect(m_Left, m_Bottom, m_Right, m_Top));
    cvs.addElement(m_Walls);
    if (!gameMode) {
      DoubleRect r = cvs.getSimBounds();
      energyBar = new BarChart(r);
      preText = new CText(r.getXMin()+0.05*r.getWidth(),
        r.getYMin()+0.75*r.getHeight(), "");
      preText.setFontSize(12);
      postText = new CText(r.getXMin()+0.05*r.getWidth(),
        r.getYMin()+0.70*r.getHeight(), "");
      postText.setFontSize(12);
      if (showEnergy) {
        cvs.addElement(energyBar);
        if (showMomentum()) {
          cvs.addElement(preText);
          cvs.addElement(postText);
        }
      }
    } else {
      DoubleRect r = cvs.getSimBounds();
      greenLabel = new CText(r.getXMin()+0.05*r.getWidth(),
        r.getYMin()+0.75*r.getHeight(), "");
      greenLabel.setFontSize(16);
      greenLabel.m_Color = Color.green;
      blueLabel = new CText(r.getXMin()+0.05*r.getWidth(),
        r.getYMin()+0.60*r.getHeight(), "");
      blueLabel.setFontSize(16);
      blueLabel.m_Color = Color.blue;
      cvs.addElement(greenLabel);
      cvs.addElement(blueLabel);
    }
    bods = new Thruster5Object[numBods];
    for (int i=0; i<numBods; i++) {
      bods[i] = createBlock(0.5, 3.0);
      bods[i].tMagnitude = thrust;
      cvs.addElement(bods[i]);
    }
    if (numBods>0) {
      if (gameMode)
        bods[0].moveTo(2,0,Math.PI/4);
      else
        bods[0].moveTo(-2,0,Math.PI/2);
      bods[0].color = Color.green;
    }
    if (numBods>1) {
      if (gameMode)
        bods[1].moveTo(-2,0,-Math.PI/4);
      else
        bods[1].moveTo(2,1,0);
      //bods[1].setWidth(1);
      //bods[1].setHeight(3);
      //bods[1].thrustX = bods[1].cmx;
      //bods[1].thrustY = 0.8*bods[1].getHeight();
      bods[1].color = Color.blue;
    }
    if (numBods>2) {
      bods[2].moveTo(1,0,0.1);
      bods[2].color = Color.red;
    }
    if (numBods>3) {
      bods[3].moveTo(-2.2, 1, 0.2+Math.PI/2);
      bods[3].color = Color.cyan;
    }
    if (numBods>4) {
      bods[4].moveTo(-2.4,-1, -0.2+Math.PI/2);
      bods[4].color = Color.magenta;
    }
    if (numBods>5) {
      bods[5].moveTo(-1.8,2, 0.3+Math.PI/2);
      bods[5].color = Color.orange;
    }
    /*  variables:   x, x', y, y', th, th'
        bods[0]      0, 1,  2, 3,  4,  5
        bods[1]      6, 7,  8, 9, 10, 11
    */
    vars = new double[6*numBods];
    calc = new boolean[vars.length];
    for (int i=0; i<calc.length; i++)
      calc[i] = true;
    for (int i=0; i<numBods; i++) {  // set initial position of each body
      vars[6*i] = bods[i].x;
      vars[6*i + 2] = bods[i].y;
      vars[6*i + 4] = bods[i].angle;
      /*
      // random velocities
      double speed = 1;
      vars[6*i + 1] = speed*(-0.5+Math.random());
      vars[6*i + 3] = speed*(-0.5+Math.random());
      vars[6*i + 5] = speed*(-0.5+Math.random());
      */
    }

    message = null;
    message2 = null;
    if (gameMode) {
      this.nf.setMinimumFractionDigits(0);
      greenLabel.setText("green "+nf.format(greenHits = 0));
      blueLabel.setText("blue "+nf.format(blueHits = 0));
    }
  }

  public void handleKeyEvent(int keyCode, boolean pressed) {
    switch (keyCode) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_J: bods[0].active[1] = pressed; break;
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_L: bods[0].active[0] = pressed; break;
      case KeyEvent.VK_UP:
      case KeyEvent.VK_I: bods[0].active[3] = pressed; break;
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_K: bods[0].active[2] = pressed; break;
      case KeyEvent.VK_S: bods[1].active[1] = pressed; break;
      case KeyEvent.VK_F: bods[1].active[0] = pressed; break;
      case KeyEvent.VK_E: bods[1].active[3] = pressed; break;
      case KeyEvent.VK_D:
      case KeyEvent.VK_C: bods[1].active[2] = pressed; break;
      default:
        break;
    }
  }

  public void modifyObjects() {
    modifyObjects(vars);
  }

  public void modifyObjects(double[] x) {
    for (int i=0; i<numBods; i++)
      bods[i].moveTo(x[6*i+0], x[6*i+2], x[6*i+4]);
    if (energyBar != null) {
      energyBar.pe = energyBar.re = energyBar.te = 0;
      for (int i=0; i<numBods; i++) {
        if (bods[i].mass == Double.POSITIVE_INFINITY)  // skip infinite mass objects
          continue;
        energyBar.pe += (x[2+6*i]-zeroEnergyLevel-bods[i].getMinHeight())*bods[i].mass*gravity;
        energyBar.re += bods[i].rotationalEnergy(x[5+6*i]);
        energyBar.te += bods[i].translationalEnergy(x[1+6*i], x[3+6*i]);
      }
    }
  }

  protected double getEnergy() {
    double e = 0;
    for (int i=0; i<numBods; i++) {
      if (bods[i].mass == Double.POSITIVE_INFINITY)  // skip infinite mass objects
        continue;
      e += (vars[2+6*i]-zeroEnergyLevel-bods[i].getMinHeight())*bods[i].mass*gravity;
      e += bods[i].rotationalEnergy(vars[5+6*i]);
      e += bods[i].translationalEnergy(vars[1+6*i], vars[3+6*i]);
    }
    return e;
  }

  protected boolean showMomentum() { return true; }

  public void printEnergy(int n, String s) {
    // Display energy and momentum
    if (!showEnergy || !showMomentum()) {
      return;
    }
    double ke = 0, pe = 0;
    double[] m0 = new double[] {0, 0, 0};
    double[] m1 = new double[3];
    for (int i=0; i<numBods; i++) {
      if (bods[i].mass == Double.POSITIVE_INFINITY)  // skip infinite mass objects
        continue;
      // potential energy = m g h
      pe += (vars[2+6*i]-zeroEnergyLevel-bods[i].getMinHeight())*bods[i].mass*gravity;
      ke += bods[i].kineticEnergy(vars[1+6*i], vars[3+6*i], vars[5+6*i]);
      m1 = bods[i].momentum(vars[1+6*i],vars[3+6*i],vars[5+6*i]);
      for (int j=0; j<3; j++)
        m0[j] += m1[j];
    }
    if (showMomentum()) {
      nf.setMaximumFractionDigits(3);
      nf.setMinimumFractionDigits(3);
      CText label = (n == 0) ? preText : postText;
      label.m_text = (s+" momentum x: "+nf.format(m0[0])+
        "   y: "+nf.format(m0[1])+
        "   angular: "+nf.format(m0[2]));
    }
  }

  // Checks for collision of a particular corner of an object.
  // Returns the collision, or null if no collision found.
  protected void checkCollision(int obj, int corner) {
    Collision result = null;
    // get location of this corner
    double cornerX, cornerY;
    switch (corner) {
      case 1: cornerX = bods[obj].ax;  cornerY = bods[obj].ay; break;
      case 2: cornerX = bods[obj].bx;  cornerY = bods[obj].by; break;
      case 3: cornerX = bods[obj].cx;  cornerY = bods[obj].cy; break;
      case 4: cornerX = bods[obj].dx;  cornerY = bods[obj].dy; break;
      default: throw new IllegalArgumentException("bad corner "+corner);
    }
    double d;  // depth of penetration:  postive = more penetration
    // check for collision with each wall, take the one with maximum depth
    // when depth is positive, and there is no previous collision or depth is greater
    if ((d = cornerX - m_Right) > 0) {
      result = new Collision();
      result.depth = d;
      result.normalX = -1;  result.normalY = 0;
      result.normalObj = RIGHT_WALL;
    }
    if ((d = m_Left - cornerX) > 0 && (result == null || d > result.depth)) {
      result = new Collision();
      result.depth = d;
      result.normalX = 1;  result.normalY = 0;
      result.normalObj = LEFT_WALL;
    }
    if ((d = cornerY - m_Top) > 0 && (result == null || d > result.depth)) {
      result = new Collision();
      result.depth = d;
      result.normalX = 0;  result.normalY = -1;
      result.normalObj = TOP_WALL;
    }
    if ((d = m_Bottom - cornerY) > 0 && (result == null || d > result.depth)) {
      result = new Collision();
      result.depth = d;
      result.normalX = 0;  result.normalY = 1;
      result.normalObj = BOTTOM_WALL;
    }
    // check for collision with each object
    for (int i=0; i<numBods; i++)
      if (i != obj) {  // don't compare object with itself
        Collision c = bods[i].testCollision(cornerX, cornerY, obj, i);
        if (c != null) {
          if (result == null || c.depth > result.depth)
            result = c;
        }
      }
    // additional info for collision
    if (result != null) {
      result.colliding = true;
      result.impactX = cornerX;
      result.impactY = cornerY;
      result.object = obj;
      result.corner = corner;
      result.depth = -result.depth;  // because addCollision regards smaller as closer
      Collision.addCollision(collisionsFound, result);
    }
  }

  // Finds all current collisions, returns them in a vector.
  // Checks each corner on all bodies, to see if it is colliding.
  public Vector findAllCollisions() {
    collisionsFound.removeAllElements();
    if (doCollisions) {
      // NOTE: assumes that bodies have been moved to current positions
      for (int i=0; i<numBods; i++) {  //for each body
        for (int j=1; j<=4; j++) {  // for each corner
          // need radius check here
          // is corner colliding?
          checkCollision(i, j);
        }
      }
      if (debug && collisionsFound.size() > 0) {
        System.out.println("--------------------------------");
        if (collisionsFound.size() > 1) {
          System.out.println(collisionsFound.size()+" collisions detected time= "+simTime);
        }
        if (collisionsFound.size() > 0) {
          for (int i=0; i<collisionsFound.size(); i++) {
            DecimalFormat df = new DecimalFormat("0.0###");
            Collision c = (Collision)collisionsFound.elementAt(i);
            System.out.println("collision obj="+df.format(c.object)+
              " normalObj="+df.format(c.normalObj)+
              " impact x="+df.format(c.impactX)+" y="+df.format(c.impactY));
            System.out.println("normal x="+df.format(c.normalX)
              +" y="+df.format(c.normalY)+" depth= "+c.depth);
          }
        }
      }
    }
    return (collisionsFound.size() > 0) ? collisionsFound : null;
  }

  protected void findNormal(Collision c1, Collision c2) {
    // finds normal to the line joining the two collisions
    // and puts this normal into collision c1.
    // First, deal with case where c1.x == c2.x
    if (c1.impactX == c2.impactX) {
      // direction of normal should point away from normalObj, towards object
      c1.normalY = 0;
      c1.normalX = (vars[0+6*c1.object] < c1.impactX) ? -1 : 1;
    } else {
      // find equation of the line joining the two collisions.
      double slope = (c2.impactY - c1.impactY)/(c2.impactX - c1.impactX);
      double b = c1.impactY - slope*c1.impactX;
      // line from c1 to c2 is y = slope*x + b
      // On the line we have 0 = y - slope*x -b
      // For points above the line we have 0 < y - slope*x - b
      // So we can check where is the object, above or below the line?
      int obj = 6*c1.object;
      // this normal (nx, ny)=(-slope, 1) points up from the c1-c2 line
      double nx = -slope;
      double ny = 1;
      if (vars[2+obj] - slope*vars[0+obj] - b < 0) {
        // object is below line, so reverse normal direction
        nx = -nx;
        ny = -ny;
      }
      // normalize the normal
      double magnitude = Math.sqrt(nx*nx + ny*ny);
      c1.normalX = nx/magnitude;
      c1.normalY = ny/magnitude;
    }
    if (debug) System.out.println("findNormal "+c1.normalX+" "+c1.normalY);
  }

  protected void gameScore(Collision c) {
    if (gameMode && c.normalObj<0) {
      this.nf.setMinimumFractionDigits(0);
      if (c.object==0) {
        greenLabel.setText("green "+nf.format(++greenHits));
        if (greenHits >= winningHits && message2 == null)
          cvs.addElement(message2
            = new CText("Green hit wall "+winningHits+" times -- Blue wins!"));
      } else {
        blueLabel.setText("blue "+nf.format(++blueHits));
        if (blueHits >= winningHits && message2 == null)
          cvs.addElement(message2
            = new CText("Blue hit wall "+winningHits+" times -- Green wins!"));
      }
    }
  }

  // Calculates impact resulting from the given collision.
  // Modifies the passed-in velo[] array of changes to velocities for the bodies.
  // The layout of the velo[] array matches the vars[] array.
  protected void addImpact(Collision cd, double[] velo) {
    gameScore(cd);
    double nx = cd.normalX; // n = normal vector pointing towards body
    double ny = cd.normalY;
    // get the correct current location of corner, to get correct Rx, Ry
    // because (impactX, impactY) may be from an interpenetration state.
    //cd.impactX = bods[cd.object].getCornerX(cd.corner);
    //cd.impactY = bods[cd.object].getCornerY(cd.corner);
    /*
      cross product in the plane: unit vectors i,j,k
        (ax,ay,0) x (bx,by,0) = k(ax by - ay bx)
    */
    int objA,objB,offsetA,offsetB;
    double rax,ray,rbx,rby;
    double vax,vay,wa,vbx,vby,wb;
    double d,dx,dy,j;
    if (cd.normalObj < 0)  { // wall collision
      objB = cd.object;
      offsetB = 6*objB;
      // normal is pointing in towards body B, so it is correct here.
      rbx = cd.impactX - bods[objB].x; // r = vector from cm to point of impact, p
      rby = cd.impactY - bods[objB].y;
      double Ib = bods[objB].momentAboutCM();
      double mb = bods[objB].mass;
      vbx = vars[1+offsetB];
      vby = vars[3+offsetB];
      wb = vars[5+offsetB];
      double e = elasticity;

      /*
        vb = old linear velocity of cm = (vbx, vby)
        mb = mass
        n = normal vector pointing towards body (length 1 here)
        vb2 = new linear velocity of cm = (vbx2,vby2)
        wb = old angular velocity = vars[5]
        rb = vector from cm to point of impact
        Ib = moment of inertia of body about cm
        wb2 = new angular velocity
        velocity of collision point = vb + w x rb
             -(1 + elasticity) (v1 + w1 x r).n
        j = -------------------------
                n.n + (rp x n)^2
                ---   --------
                 M       I
      */
      // cross product r x n = (rx, ry, 0) x (nx, ny, 0) = (0, 0, rx*ny - ry*nx)
      j = rbx*ny - rby*nx;
      j = (j*j)/Ib + (1/mb);
      // make sure that normal velocity is negative, otherwise do nothing
      // cross product: w1 x r = (0,0,w) x (rx, ry, 0) = (-w*ry, w*rx, 0)
      double normalVelocity = (vbx-rby*wb)*nx + (vby+rbx*wb)*ny;
      if (normalVelocity >= 0) {
        if (debug)
          System.out.println("add Impact: positive relative velocity "+normalVelocity);
        return;
      }
      j = -(1 + e)*normalVelocity / j;
      // v2 = v1 + (j/M)n = new linear velocity
      velo[1+offsetB] += nx*j/mb;
      velo[3+offsetB] += ny*j/mb;
      // w2 = w1 + j(r x n)/I = new angular velocity
      velo[5+offsetB] += j*(rbx*ny - rby*nx)/Ib;
      addCollisionDot(cd.impactX, cd.impactY, j, Color.blue);
    } else { // object-object collision
      // The vertex of body A is colliding into an edge of body B.
      // The normal points out from body B, perpendicular to the edge.
      objA = cd.object;
      objB = cd.normalObj;
      offsetA = 6*objA;
      offsetB = 6*objB;
      rax = cd.impactX - bods[objA].x; // ra = vector from A's cm to point of impact, p
      ray = cd.impactY - bods[objA].y;
      rbx = cd.impactX - bods[objB].x; // rb = vector from B's cm to point of impact
      rby = cd.impactY - bods[objB].y;
      /*
      Ia = bods[objA].momentAboutCM();
      Ib = bods[objB].momentAboutCM();
      ma = bods[objA].mass;
      mb = bods[objB].mass;
      */
      double invIa, invIb, invma, invmb;
      invIa = bods[objA].invMomentAboutCM();
      invIb = bods[objB].invMomentAboutCM();
      invma = bods[objA].invMass();
      invmb = bods[objB].invMass();
      nx = -nx;  // reverse n so it points out from body A into body B
      ny = -ny;
      vax = vars[1+offsetA];
      vay = vars[3+offsetA];
      wa = vars[5+offsetA];
      vbx = vars[1+offsetB];
      vby = vars[3+offsetB];
      wb = vars[5+offsetB];
      /*
        ma = mass of body A
        n = normal vector pointing out from body A (length 1 here)
        j = impulse scalar
        jn = impulse vector
        va = old linear velocity of cm for body A
        va2 = new linear velocity of cm
        wa = old angular velocity for body A
        wa2 = new angular velocity
        ra = vector from body A cm to point of impact = (rax, ray)
        Ia = moment of inertia of body A about center of mass
        vab = relative velocity of contact points (vpa, vpb) on bodies
        vab = (vpa - vpb)
        vpa = va + wa x ra = velocity of contact point
        vab = va + wa x ra - vb - wb x rb

                      -(1 + elasticity) vab.n
        j = -------------------------------------
              1     1     (ra x n)^2    (rb x n)^2
            (--- + ---) + ---------  + ---------
              Ma   Mb        Ia           Ib
        Note that we use -j for body B.
      */
      // cross product r x n = (rx, ry, 0) x (nx, ny, 0) = (0, 0, rx*ny - ry*nx)
      d = rax*ny - ray*nx;
      j = d*d*invIa;
      d = -rby*nx + rbx*ny;
      j += d*d*invIb + invma + invmb;
      // vab.n = (va + wa x ra - vb - wb x rb) . n
      // cross product: w x r = (0,0,w) x (rx, ry, 0) = (-w*ry, w*rx, 0)
      dx = vax + wa*(-ray) - vbx - wb*(-rby);
      dy = vay + wa*(rax) - vby - wb*(rbx);
      j = -(1+elasticity)*(dx*nx + dy*ny)/j;
      // v2 = v1 + j n / m = new linear velocity
      velo[1+offsetA] += j*nx*invma;
      velo[3+offsetA] += j*ny*invma;
      velo[1+offsetB] += -j*nx*invmb;
      velo[3+offsetB] += -j*ny*invmb;
      // w2 = w1 + j(r x n)/I = new angular velocity
      velo[5+offsetA] += j*(-ray*nx + rax*ny)*invIa;
      velo[5+offsetB] += -j*(-rby*nx + rbx*ny)*invIb;
      if (debug) System.out.println("addImpact j= "+j+" normal= "+nx+" "+ny);
      addCollisionDot(cd.impactX, cd.impactY, j, Color.blue);
    }
  }

  // Searches for a pair of matching collisions, ie. one where the same two
  // objects are colliding (but presumably at different vertexes).
  // Returns the first match it finds in an array of two collisions.
  protected Collision[] findMatch(Vector collisions) {
    int n = collisions.size();
    for (int i=0; i<n; i++) {
      Collision c1 = (Collision)collisions.elementAt(i);
      if (c1.handled)
        continue;
      for (int j=i+1; j<n; j++) {
        Collision c2 = (Collision)collisions.elementAt(j);
        if (c2.handled)
          continue;
        if (c1.object==c2.object && c1.normalObj==c2.normalObj
            || c1.object==c2.normalObj && c1.normalObj==c2.object) {
          // found a match
          Collision[] result = {c1, c2};
          return result;
        }
      }
    }
    return null;
  }

/*
  BUG:  3 BODY COLLISION BUG  If you change the collision time tolerance TOL in
  CollidingSim from 0.0001 to 0.03, you will see the energy level increase while
  running Thruster5 (try 5 bodies with gravity=1.0). The reason is there are
  simultaneous collisions on the same object.  For example object 2 is colliding both
  with object 1 and a wall. This is a case I don't handle correctly now, I treat the
  collisions like separate collisions, which is wrong.  I have a feeling it would not
  be terribly difficult to figure out the correct solution at least for this case of
  3 objects in a collision.  I think getting more than that in a bunch would be
  unlikely, unless they were put artifically in contact... and that is getting into
  the much more complex contact stuff.
*/
  // Finds multiple contact collisions, ie. where two objects are colliding
  // with contact at more than one vertex.
  // Adds the effect of these collisions into an array of changes to velocity,
  // and removes these collisions from the input collisions vector.
  // The layout of the velo[] array matches the vars[] array.
  protected void specialImpact(Vector collisions, double[] velo) {
    Collision[] match;
    while ((match = findMatch(collisions)) != null) {
      Collision c1 = match[0];
      Collision c2 = match[1];
      // ...deal with this multiple collision...
      // find normal... it should point out from normalObj
      findNormal(c1, c2);
      // find the midpoint between the two impact points, pretend it happened there.
      c1.impactX = (c1.impactX + c2.impactX)/2;
      c1.impactY = (c1.impactY + c2.impactY)/2;
      addImpact(c1, velo);

      // We could deal with other combinations here, but need to figure out how.
      // * Side collision between two objects
      // * Object hitting two walls in a corner
      // * Object rotates so corner A hits another object, corner C hits wall
      if (true || debug)  System.out.println("special impact "+c1.object+" "+c1.normalObj);
      // As a debugging aid, we don't delete collisions that have been handled,
      // instead we mark them as 'handled'.  This way we can see what collisions
      // have taken place, which helps with debugging.
      c2.handled = c1.handled = true;
    }
  }

  // Handles the effects of the input set of collisions, by
  // modifying the velocities of the objects.
  public void handleCollisions(Vector collisions) {
    //double startEnergy = getEnergy();
    printEnergy(0, "pre-collision ");
    double[] velo = new double[vars.length];  // NOTE: ??? avoid allocation by keeping this around?
    for (int i=0; i<vars.length; i++)
      velo[i] = 0;
    if (debug) System.out.println("handleCollisions "+collisions.size()+
      " collisions time="+simTime);
    specialImpact(collisions, velo);
    if (debug) System.out.println("handleCollisions "+collisions.size()+
      " collisions after specialImpact");

    for (int i=0; i<collisions.size(); i++) {
      Collision c = (Collision)collisions.elementAt(i);
      if (!c.handled) {
        addImpact((Collision)collisions.elementAt(i), velo);
        c.handled = true;
      }
    }
    for (int i=0; i<vars.length; i++) {
      if (debug && velo[i] != 0)
        System.out.println("var "+i+" modified by "+velo[i]);
      vars[i] += velo[i];
    }
    /*
    // for debugging unexpected changes in energy
    double e = getEnergy();
    double r = e/startEnergy;
    if (r<0.95 || r>1.05) {
      System.out.println("energy changed from "+startEnergy+" to "+e);
      for (int i=0; i<collisions.size(); i++)
        System.out.println(collisions.elementAt(i));
    }
    */
    printEnergy(1, "post-collision");
  }

  /*
    Let th = angle of the body
    variables:   x, x', y, y', th, th'
    bods[0]      0, 1,  2, 3,  4,  5
    bods[1]      6, 7,  8, 9, 10, 11
    Let R be the vector from CM (center of mass) to T (thrust point)
    components of R are Rx, Ry
    Let N be normalized R, so that N = R / |R|
    Let F be the thrust vector, with components Fx, Fy
    (and for mouse dragging, we add a spring force also).
    The force on the center of mass is (F.N)N... no its just F!
    CM moves according to (F.N)N = M A... no just F = M A
    So we have the two equations:
      Ax = Nx (F.N)/M
      Ay = Ny (F.N)/M
      ... no just Ax = Fx/M and Ay = Fx/M
    The moment of inertia about the CM is I = M (width^2 + height^2)/12
    The torque at T about the CM is given by R x F = Rx Fy - Ry Fx
    The angular dynamics are given by R x F = th'' I
    So we have the equation
      th'' = (Rx Fy - Ry Fx)/I
    The method calcVectors calculates F,N,R given x,y,th
  */
  // executes the i-th diffeq
  // i = which diffeq,  t=time,  x= array of variables
  public void evaluate(double[] x, double[] change) {
    while (!rxnForces.isEmpty()) {
      Drawable d = (Drawable)rxnForces.lastElement();
      cvs.removeElement(d);
      rxnForces.removeElement(d);
    }
    for (int i=0; i<vars.length; i++) {
      int j = i%6;  // % is mod, so j tells what derivative is wanted:
                    // 0=x, 1=x', 2=y, 3=y', 4=th, 5=th'
      int obj = i/6;  // which object: 0, 1
      int offset = 6*obj;
      int k;
      double result = 0;
      double invMass = bods[obj].invMass();
      double invMoment = bods[obj].invMomentAboutCM();
      final double springConst = 1;
      switch (j) {
        case 0: change[i] = x[1+offset]; break;
        case 1:
          result = - damping*x[1+offset]*invMass;
          //result -= .3;
          for (k=0; k<4; k++) {  // for each of the 4 thrusters
            if (bods[obj].active[k]) {
              double[] v = bods[obj].calcVectors(x[0+offset], x[2+offset], x[4+offset], k);
              // v[0] = Rx, v[1] = Ry, v[2] = Nx, v[3] = Ny, v[4] = Fx, v[5] = Fy
              result += v[4]*invMass;  // Ax = Fx/M
            }
          }
          if (obj == dragObj) { // add rubber band force
            double[] v = bods[obj].calcVectors(x[0+offset], x[2+offset], x[4+offset], 0);
            // x component of rubber band force
            double Fx = springConst*(mouseX - (x[0+offset] + v[0]));
            result += Fx*invMass;  // Ax = Fx/M
          }
          change[i] = result; break;
        case 2:  change[i] = x[3+offset]; break;
        case 3:
          result = - damping*x[3+offset]/bods[obj].mass;
          if (invMass != 0)  // assume gravity doesn't affect infinite mass objects
            result -= gravity;
          for (k=0; k<4; k++) {  // for each thruster
            if (bods[obj].active[k]) {
              double[] v = bods[obj].calcVectors(x[0+offset], x[2+offset], x[4+offset], k);
              // v[0] = Rx, v[1] = Ry, v[2] = Nx, v[3] = Ny, v[4] = Fx, v[5] = Fy
              result += v[5]*invMass;  // Ay = Fy/M
            }
          }
          if (obj == dragObj) { // add rubber band force
            double[] v = bods[obj].calcVectors(x[0+offset], x[2+offset], x[4+offset], 0);
            // y component of rubber band force
            double Fy = springConst*(mouseY - (x[2+offset] + v[1]));
            result += Fy*invMass;  // Ay = Fy/M
          }
          change[i] = result; break;
        case 4:  change[i] = x[5+offset]; break;
        case 5:
          result = - damping*x[5+offset];
          for (k=0; k<4; k++) {  // for each thruster
            if (bods[obj].active[k]) {
              double[] v = bods[obj].calcVectors(x[0+offset], x[2+offset], x[4+offset], k);
              // v[0] = Rx, v[1] = Ry, v[2] = Nx, v[3] = Ny, v[4] = Fx, v[5] = Fy
              //  th'' = (Rx Fy - Ry Fx)/I
              result += (v[0]*v[5] - v[1]*v[4])*invMoment;
            }
          }
          if (obj == dragObj) { // add rubber band force
            double[] v = bods[obj].calcVectors(x[0+offset], x[2+offset], x[4+offset], 0);
            // x & y components of rubber band force
            double Fx = springConst*(mouseX - (x[0+offset] + v[0]));
            double Fy = springConst*(mouseY - (x[2+offset] + v[1]));
            //  th'' = (Rx Fy - Ry Fx)/I
            result += (v[0]*Fy - v[1]*Fx)*invMoment;
          }
          change[i] = result; break;
      }
    }
  }

  public void advance(double timeStep) {
    super.advance(timeStep);
    // check if the simulation is 'stuck' ie. time is not advancing
    // if so, display a message.
    if (lastTimeStep == 0)
      ++stuckCounter;
    if (lastTimeStep > 0) {
      stuckCounter = 0;
      if (message != null) {
        cvs.removeElement(message);
        message = null;
      }
    } else if (stuckCounter >= 4) {
      System.out.println("we are stuck at time "+simTime);
      if (message == null) {
        message = new CText("Simulation is stuck!  Click reset to continue.");
        cvs.addElement(message);
      }
    }
  }

  protected void addCollisionDot(double x, double y, double magnitude, Color c) {
    if (showCollisionDot) {
      final double w = Math.max(0.02, Math.abs(magnitude));
      CMass m = new CMass(x-w/2, y-w/2, w, w, CElement.MODE_CIRCLE_FILLED);
      m.m_Color = c;
      cvs.addElement(m);
      rxnForces.addElement(m);
    }
  }
} // END class Thruster5

/////////////////////////////////////////////////////////////////////////////////
// Collision represents a collision or resting contact between a corner
// of one object and an edge of another object (possibly a wall).
// Walls are indicated by negative object index numbers.
// Objects are indicated by the index of their body in the bods[] vector.
class Collision {
  public double depth; // depth of collision (positive = penetration)
  public double normalX; // normal (pointing outward from normalObj)
  public double normalY;
  public double impactX; // point of impact
  public double impactY;
	// NOTE (Dec 2006):  R and R2 seem to actually be vector from point of impact to CM,
	// not from CM to point of impact!
  public double Rx;  // distance vector from CM (center of mass) of object to point of impact
  public double Ry;
  public double R2x; // distance vector from CM of normalObj to point of impact
  public double R2y;
  public double rxnForceX;
  public double rxnForceY;
  public int object; // "primary" object whose corner is colliding (index in bods[] vector)
  public int normalObj; // object corresponding to the normal (negative = wall)
  public int corner;  // which corner, 1=a, 2=b, 3=c, 4=d  see Thruster5Object
  public boolean colliding = true; // true if colliding, false if resting contact
  public boolean handled = false;  // true once its impact has been handled

  public Collision() {
  }
  public String toString() {
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(7);
    return (colliding ? "Collision" : "Contact")+ " object="+object
      +" normalObj="+normalObj+" corner="+corner
      +" depth="+nf.format(depth)
      +" impact=("+nf.format(impactX)+","+nf.format(impactY)
      +") normal=("+nf.format(normalX)+","+nf.format(normalY)
      +")";
  }

  public static void addCollision(Vector collisions, Collision c2) {
    // Add this collision only if it there is not already a closer
    // collision at this corner.
    // This helps prevent having two collisions (or contacts) at a corner,
    // since really only one of the two corners should be in contact.
    boolean shouldAdd = true;
    for (int i=0; i<collisions.size(); i++) {
      Collision c = (Collision)collisions.elementAt(i);
      // ensure same bodies are involved
      if (c.object == c2.object && c.normalObj == c2.normalObj ||
          c.object == c2.normalObj && c.normalObj == c2.object) {
        // find distance between the collisions
        double dx = c.impactX - c2.impactX;
        double dy = c.impactY - c2.impactY;
        if (Math.sqrt(dx*dx + dy*dy) <= Thruster5.DISTANCE_TOL) {
          // now take the closer of the two collisions
          // (only add the new collision c2 if it is closer)
          if (c2.depth < c.depth) {
            collisions.removeElement(c);
          } else
            shouldAdd = false;
        }
      }
    }
    if (shouldAdd)
      collisions.addElement(c2);
  }
}

/////////////////////////////////////////////////////////////////////////////////
/*  Thruster5Object represents the rigid body.
It is a drawable element in the simulation.  It also handles the geometry
calculations for intersections and collisions, as well as energy and
momentum calculations.

object coords are as follows.  angle = zero as shown.  tAngle = -pi/4

     d-----c(width, height)
     |     |
     |  t  |  coords of e = x + sin(angle)*cmy, y - cos(angle)*cmy
     |     |  coords of a = ex - cos(angle)*cmx, ey - sin(angle)*cmx
     |     |  coords of b = ax + cos(angle)*width, ay + sin(angle)*width
     |  cm |  coords of c = bx - sin(angle)*height, by + cos(angle)*height
     |     |  coords of d = ax - sin(angle)*height, ay + cos(angle)*height
     |     |
     |     |
     |     |
     |     |
     a--e--b
(0,0)

t is the thrust point.  
cm is the center of mass.

*/
class Thruster5Object implements Dragable {
  protected static final int BOTTOM=0, RIGHT=1, TOP=2, LEFT=3;
  protected static final int X=0, VX=1, Y=2, VY=3, W=4, VW=5;
  public double x=0, y=0;  // position of the center of mass in the world
  public double angle = 0;  // rotation of object around center of mass
  protected double width = 0.5;  // width of object
  protected double height = 3.0; // height of object
  public double cmx, cmy; // position of  center of mass in object coords
  public double thrustX, thrustY;  // position of thrust point in object coords
  public double[] tAngle;  // angle of the thrust in object coords
  public boolean[] active;  // which thrusters are firing
  public double tMagnitude = 0.5;  // thrust magnitude
  public double mass = 1;
  public double ax,ay,bx,by,cx,cy,dx,dy,tx,ty;  // positions of corners
  public Color color = Color.black;

  public Thruster5Object() {
    initialize();
  }

  public Thruster5Object(double width, double height) {
    initialize();
    setWidth(width);
    setHeight(height);
    thrustX = width/2;  thrustY = 0.8*height;
  }

  private void initialize() {
    thrustX = width/2;  thrustY = 0.8*height;
    tAngle = new double[4];
    tAngle[0] = Math.PI/2; // left
    tAngle[1] = -Math.PI/2; // right
    tAngle[2] = 0; // up
    tAngle[3] = Math.PI; // down
    active = new boolean[4];
    active[0] = active[1] = active[2] = active[3] = false;
    moveTo(x, y, angle);
  }

  public String toString() {
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(7);
    return "Thruster5Object x="+nf.format(x)+" y="+nf.format(y)+" angle="+nf.format(angle)+
    " width="+nf.format(width)+" height="+nf.format(height)+" mass="+nf.format(mass)+"\n"+
    " cornerA=("+nf.format(ax)+","+nf.format(ay)+
    ") cornerB=("+nf.format(bx)+","+nf.format(by)+")\n"+
    " cornerC=("+nf.format(cx)+","+nf.format(cy)+
    ") cornerD=("+nf.format(dx)+","+nf.format(dy)+")";
  }

  public boolean isDragable() {
    return (mass != Double.POSITIVE_INFINITY) ? true : false;
  }

  public double distanceSquared(double x, double y) {
    double dx = this.x - x;
    double dy = this.y - y;
    return dx*dx+dy*dy;
  }
  public double getX() { return this.x; }

  public double getY() { return this.y; }

  public void setPosition(double x, double y) {
    moveTo(x, y, this.angle);
  }

  public double getCornerX(int corner) {
    switch (corner) {
      case 1: return ax;
      case 2: return bx;
      case 3: return cx;
      case 4: return dx;
      default: return Double.POSITIVE_INFINITY;
    }
  }

  public double getCornerY(int corner) {
    switch (corner) {
      case 1: return ay;
      case 2: return by;
      case 3: return cy;
      case 4: return dy;
      default: return Double.POSITIVE_INFINITY;
    }
  }

  public double getWidth() { return this.width; }

  public double getHeight() { return this.height; }

  public double getMinHeight() {  // for potential energy calculation
    return (width<height) ? width/2 : height/2;
  }

  public void setWidth(double width) {
    this.width = width;
    cmx = width/2;
  }

  public void setHeight(double height) {
    this.height = height;
    cmy = height/2;
  }

  public double invMass() {
    return (mass == Double.POSITIVE_INFINITY) ? 0 : 1/mass;
  }

  public double invMomentAboutCM() {
    return (mass == Double.POSITIVE_INFINITY) ? 0 : 12/(mass*(width*width + height*height));
  }

  // returns moment of inertia about center of mass
  public double momentAboutCM() {
    return mass*(width*width + height*height)/12;
  }

  // given velocities (linear and angular) return kinetic energy
  public double kineticEnergy(double vx, double vy, double w) {
    double e = translationalEnergy(vx, vy);
    e += rotationalEnergy(w);
    return e;
  }

  public double rotationalEnergy(double w) {
    return 0.5*momentAboutCM()*w*w;
  }

  public double translationalEnergy(double vx, double vy) {
    double e = 0.5*mass*(vx*vx + vy*vy);
    return e;
  }

  // given velocities (linear and angular) return momentum
  public double[] momentum(double vx, double vy, double w) {
    double result[] = new double[3];
    result[0] = mass*vx;
    result[1] = mass*vy;
    /*
    angular momentum about a fixed point in space is defined as
    Icm w k + r x m vcm
    (k is unit z vector, r is vector from fixed point to cm)
    cross product in the plane: (ax,ay,0) x (bx,by,0) = k(ax by - ay bx)
    Icm w + m(rx vy - ry vx)
    take the fixed point to be the origin (0,0), so (rx,ry) is cm
    */
    result[2] = momentAboutCM()*w + mass*(x*vy - y*vx);
    return result;
  }

  // Given an angle, suppose we want a corner to be at a certain location.
  // This returns the position needed for center of mass to make it so.
  // Returns array of (CMx, CMy).
  public double[] setCornerAt(double H, double V, double angle) {
    double[] r = new double[2];
    double cosa = Math.cos(angle);
    double sina = Math.sin(angle);
    if (angle>=0 && angle <= Math.PI) {
      // We are working with corner A.
      // coords of e = x + sin(angle)*cmy, y - cos(angle)*cmy
      // coords of a = ex - cos(angle)*cmx, ey - sin(angle)*cmx = (H, V)
      // solve for x,y
      // H = x + sina*cmy - cosa*cmx
      // V = y - cosa*cmy - sina*cmx
      r[0] = H - sina*cmy + cosa*cmx;
      r[1] = V + cosa*cmy + sina*cmx;
    } else if (angle<0 && angle>= -Math.PI) {
      // Working with corner B.
      // coords of e = x + sin(angle)*cmy, y - cos(angle)*cmy
      // coords of a = ex - cos(angle)*cmx, ey - sin(angle)*cmx
      //  coords of b = ax + cos(angle)*width, ay + sin(angle)*width = (H, V)
      // solve for x,y
      // H = x + sina*cmy - cosa*cmx + cosa*width
      // V = y - cosa*cmy - sina*cmx + sina*width
      r[0] = H - sina*cmy + cosa*cmx - cosa*width;
      r[1] = V + cosa*cmy + sina*cmx - sina*width;
    } else
      throw new IllegalArgumentException("setCorner can only handle angles from -PI/2 to PI/2 "
      +" but angle="+angle);
    return r;
  }

  // returns the vector from CM to thrust point, based on passed in vars
  public double[] calcVectors(double x, double y, double angle, int thruster) {
    double sinAngle = Math.sin(angle);
    double cosAngle = Math.cos(angle);
    double ex = x + sinAngle*cmy;
    double ey = y - cosAngle*cmy;
    double ax = ex - cosAngle*cmx;
    double ay = ey - sinAngle*cmx;
    double tx = ax + cosAngle*thrustX - sinAngle*thrustY;
    double ty = ay + sinAngle*thrustX + cosAngle*thrustY;
    double tx2 = tx - Math.sin(angle + tAngle[thruster])*tMagnitude;
    double ty2 = ty + Math.cos(angle + tAngle[thruster])*tMagnitude;
    double rx = tx - x;
    double ry = ty - y;
    double rlen = Math.sqrt(rx*rx + ry*ry);
    double result[] = new double[6];
    result[0] = rx;  // vector from CM to thrust point
    result[1] = ry;
    result[2] = rx/rlen;  // normalized version
    result[3] = ry/rlen;
    result[4] = tx2 - tx;  // thrust vector
    result[5] = ty2 - ty;
    return result;
  }

  public void moveTo(double x, double y, double angle) {
    this.x = x;
    this.y = y;
    this.angle = angle;
    // find position of corners labeled a,b,c,d,e,t in diagram above
    double sinAngle = Math.sin(this.angle);
    double cosAngle = Math.cos(this.angle);
    double ex = this.x + sinAngle*cmy;
    double ey = this.y - cosAngle*cmy;
    this.ax = ex - cosAngle*cmx;
    this.ay = ey - sinAngle*cmx;
    this.bx = this.ax + cosAngle*width;
    this.by = this.ay + sinAngle*width;
    this.cx = this.bx - sinAngle*height;
    this.cy = this.by + cosAngle*height;
    this.dx = this.ax - sinAngle*height;
    this.dy = this.ay + cosAngle*height;
    this.tx = this.ax + cosAngle*thrustX - sinAngle*thrustY;
    this.ty = this.ay + sinAngle*thrustX + cosAngle*thrustY;
  }

  public void draw(Graphics g, ConvertMap map) {
    // make a polygon object
    int[] xPoints = new int[5];
    int[] yPoints = new int[5];
    int i = 0;
    xPoints[i] = map.simToScreenX(ax);
    yPoints[i++] = map.simToScreenY(ay);
    xPoints[i] = map.simToScreenX(bx);
    yPoints[i++] = map.simToScreenY(by);
    xPoints[i] = map.simToScreenX(cx);
    yPoints[i++] = map.simToScreenY(cy);
    xPoints[i] = map.simToScreenX(dx);
    yPoints[i++] = map.simToScreenY(dy);
    xPoints[i] = map.simToScreenX(ax);
    yPoints[i++] = map.simToScreenY(ay);
    g.setColor(this.color);
    g.fillPolygon(xPoints, yPoints, 5);
    if (mass != Double.POSITIVE_INFINITY) {
      // draw a dot at thruster point
      double sz = 0.15*((width < height) ? width : height);
      int w = map.simToScreenScaleX(2*sz);
      int sx = map.simToScreenX(tx-sz);
      int sy = map.simToScreenY(ty+sz);
      g.setColor(Color.gray);
      g.fillOval(sx, sy, w, w);
      // draw a line in *REVERSE* direction of thrust force
      // (reverse, because that's how we think about thrust on a rocket)
      double tx2, ty2;
      int k;
      for (k=0; k<4; k++) {  // for each thruster
        if (active[k]) {
          double len = Math.log(1+tMagnitude)/0.693219;
          tx2 = tx + Math.sin(angle + tAngle[k])*len;
          ty2 = ty - Math.cos(angle + tAngle[k])*len;
          g.setColor(Color.red);
          g.drawLine(map.simToScreenX(tx), map.simToScreenY(ty),
              map.simToScreenX(tx2), map.simToScreenY(ty2));
        }
      }
    }
  }

  public Collision testCollision(double gx, double gy, int objIndex, int selfIndex) {
    // given the point (gx, gy) which is a corner of the other object,
    // determine how far inside this object it is.
    // Calculates the distance to the closest edge (negative if outside the object),
    // and the normal to that edge (pointing out).
    // Start by moving to body coordinate system
    gx -= this.x;  // set center of mass as origin, and unrotate
    gy -= this.y;
    double px = gx*Math.cos(-this.angle) - gy*Math.sin(-this.angle);
    double py = gx*Math.sin(-this.angle) + gy*Math.cos(-this.angle);
    px += this.cmx; // translate to body coordinates
    py += this.cmy;
    // find nearest edge: 0=bottom, 1=right, 2=top, 3=left
    int edge = 0;
    double dist = py;  // bottom edge
    double d;
    d = this.width - px;  // right edge
    if (d < dist) {
      dist = d; edge = 1;
    }
    d = this.height - py; // top edge
    if (d < dist) {
      dist = d; edge = 2;
    }
    d = px;  // left edge
    if (d < dist) {
      dist = d; edge = 3;
    }
    if (dist > 0) {
      Collision result = new Collision();
      result.colliding = true;
      result.depth = dist; // depth of collision
      result.impactX = gx; // point of impact
      result.impactY = gy;
      result.normalObj = selfIndex; // object corresponding to the normal
      result.object = objIndex; // object whose corner is colliding
      // figure out normal to that edge, and rotate it back to world coords
      // (don't need to translate it)
      getNormalForEdge(result, edge);
      return result;
    } else
      return null;
  }

  protected void getNormalForEdge(Collision c, int edge) {
    // figure out normal to that edge, and rotate it back to world coords
    // (don't need to translate it)
    double px, py;
    switch (edge) {
      case BOTTOM: px=0;py=-1;break; //bottom
      case RIGHT: px=1;py=0;break; //right
      case TOP: px=0;py=1;break; //top
      case LEFT: px=-1;py=0;break; //left
      default:
        throw new IllegalStateException("Can't find normal, no edge specified for "+c);
    }
    // normal (outward pointing)
    c.normalX = px*Math.cos(this.angle) - py*Math.sin(this.angle);
    c.normalY = px*Math.sin(this.angle) + py*Math.cos(this.angle);
  }
}
