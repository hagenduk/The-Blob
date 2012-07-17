/*
  File: Molecule3.java

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

/*
  A Molecule made of 2 to 6 masses with springs between.
  Some of the springs are in a "special group" which is colored differently
  and whose parameters can be set separately from the other springs.
  This allows non-symmetric molecules to be created.

    vars: 0   1   2   3   4   5   6   7   8   9  10  11  etc....
         U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y

	Modification History:
	Oct 10 2006:  Disabled the "gradient" feature by removing the checkbox for it.
	The gradient was an experiment to try to visualize the forces around a mass.
	The phenomenon of interest was that as you gradually change a spring stiffness or length, 
	eventually the assembly becomes unstable and flips to a new configuration.
	The gradient was an attempt to visualize how the change in stability 
	is occuring.
*/

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;

/////////////////////////////////////////////////////////////////////////////////
public class Molecule3 extends CollidingSim implements ActionListener, ObjectListener
{
  private int nm = 2; // number of masses
  private CMass[] m; // array of masses
  private CSpring[] s; // array of springs
  private CText tx;
  private CRect m_Walls;
  private CGradient gradient;
  private int gradientMass = 0;
  private boolean showGradient = false;
  private double m_Elasticity = 0.8;
  private double m_Damping = 1;
  private double m_Gravity = 0;
  private double m_Left, m_Right, m_Top, m_Bottom;
  private static final int TOP_WALL=1, BOTTOM_WALL=2, LEFT_WALL=3, RIGHT_WALL=4;
  private Vector collisions  = new Vector(10);
  private static final String MASS_SPECIAL = "red mass", MASS = "other mass",
      ELASTICITY="elasticity", GRAVITY="gravity", DAMPING = "damping",
      LENGTH = "spring rest length", STIFFNESS = "spring stiffness",
      LENGTH_SPECIAL="red spring length", STIFF_SPECIAL="red spring stiffness",
      GRADIENT="show gradient";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS_SPECIAL, MASS, ELASTICITY, GRAVITY, DAMPING,
      STIFFNESS, LENGTH, STIFF_SPECIAL, LENGTH_SPECIAL};
  //, GRADIENT};


  /*   These are the molecules that msm represents.
                  0---1
        0----1
        \   /
          2
                 0------1
                 |\   / |
                 |  \   |
                 |/    \|
                 3------2

            0----1   (internal connections not shown)
           /      \
          /        \
         4          2
          \        /
            \   /
              3

            0----1   (internal connections not shown)
           /      \
          /        \
         5          2
          \        /
           \      /
            4----3
  */
  // msm matrix: says how springs & masses are connected
  // each row is a spring.  with indices of masses connected to that spring
  private int[][] msm;
  private int[][] msm2 = {{0,1}};
  private int[][] msm3 = {{0,1},{1,2},{2,0}};
  private int[][] msm4 = {{0,1},{1,2},{2,3},{3,0},{1,3},{0,2}};
  private int[][] msm5 = {{0,1},{1,2},{2,3},{3,4},{4,0},{4,2},{4,1},
                          {0,3},{1,3},{0,2}};
  private int[][] msm6= {{0,1},{1,2},{2,3},{3,4},{4,5},{5,0},{0,2},{2,4},
                         {4,0},{1,3},{3,5},{5,1},{0,3},{1,4},{2,5}};
  private int[] sg;  // special group of springs, these are indices into msm[].
  private int[] sg2 = {};
  private int[] sg3 = {0};
  private int[] sg4 = {0,3,5};
  //private int[] sg5 = {8,9};
  private int[] sg5 = {0,4,7,9};
  private int[] sg6 = {12,13,14};
  private int[] nsg;  // non-special group of springs

  private JButton button_stop;

  public Molecule3(Container container, int nm) {
    super(container, nm*4);
    if (nm==3)
      gradientMass = 2;
    cvs.expandMap();
    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -6, 6, -6, 6,
          CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    gradient = new CGradient();
    if (showGradient)
      cvs.addElement(gradient);
    cvs.addElement(tx = new CText(3,3.0,"energy "));

    double w = 0.5;
    Color[] cm = {Color.red, Color.blue, Color.magenta, Color.orange,
        Color.gray, Color.green};

    DoubleRect box = cvs.getSimBounds();
    m_Walls = new CRect(box);
    m_Left = box.getXMin() + w/2;
    m_Right = box.getXMax() - w/2;
    m_Bottom = box.getYMin() + w/2;
    //m_Bottom += 2;
    m_Top = box.getYMax() - w/2;
    cvs.addElement(m_Walls);
    cvs.setObjectListener(this);

    // select the mass-spring-mass array
    switch (nm)  {
      case 2: msm = msm2; sg = sg2; break;
      case 3: msm = msm3; sg = sg3; break;
      case 4: msm = msm4; sg = sg4; break;
      case 5: msm = msm5; sg = sg5; break;
      case 6: msm = msm6; sg = sg6; break;
    }

    // create an array of the "non-special group" springs
    nsg = new int[msm.length - sg.length];  //non-special group
    int j = 0;
    int k = 0;
    if (sg.length > 0)
      for(int i=0; i<msm.length; i++)
        if ((j<sg.length) && (i == sg[j]))
          j++; // skip it
        else
          nsg[k++] = i;  // include it

    // create the CMass objects and put them into an array
    m = new CMass[nm];
    for (int i=0; i<m.length; i++)  {
      m[i] = new CMass(0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
      m[i].m_Mass = .5;
      m[i].m_Color = cm[i];
      m[i].m_Damping = 0.1;
      cvs.addElement(m[i]);
    }

    // create the CSpring objects and put them into an array
    s = new CSpring[msm.length];
    for (int i=0; i<s.length; i++)  {
      s[i] = new CSpring (0, 0, 3, 0.3); // x1, y1, restLen, thick
      s[i].m_SpringConst=6;
      s[i].m_Color = Color.green.darker();
      s[i].m_Color2 = Color.green;
      cvs.addElement(s[i]);
    }

    // highlight the special group of springs
    for (int i=0; i<sg.length; i++)  {
        s[sg[i]].m_Color = Color.red.darker();
        s[sg[i]].m_Color2 = Color.red;
      }

    stop();
    modifyObjects();
  }

  public void setupControls() {
    super.setupControls();
    addControl(button_stop = new JButton("reset"));
    button_stop.addActionListener(this);
    // DoubleField params:  subject, name, fraction digits
    addObserverControl(new DoubleField(this, MASS_SPECIAL, 2));
    addObserverControl(new DoubleField(this, MASS, 2));
    addObserverControl(new DoubleField(this, ELASTICITY, 2));
    addObserverControl(new DoubleField(this, GRAVITY, 2));
    //addObserverControl(new MySlider(this, GRAVITY, 0, 20, 100, 2));
    addObserverControl(new DoubleField(this, DAMPING, 2));
    addObserverControl(new DoubleField(this, STIFFNESS, 2));
    addObserverControl(new DoubleField(this, LENGTH, 2));
//    addObserverControl(new DoubleField(this, STIFF_SPECIAL, 2));
    addObserverControl(new MySlider(this, STIFF_SPECIAL,
        0, 12, 120, 1)); // min, max, increments, digits
//    addObserverControl(new DoubleField(this, LENGTH_SPECIAL 2));
    addObserverControl(new MySlider(this, LENGTH_SPECIAL,
        0, 12, 120, 1)); // min, max, increments, digits
    //addObserverControl(new MyCheckbox(this, GRADIENT));
    showControls(true);
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null)
      graph.setVars(0,1);
    showGraph(false);
  }

  public String getVariableName(int i) {
    /* vars: 0   1   2   3   4   5   6   7   8   9  10  11
            U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y
     */
    int j = i%4;  // % is mod, so j tells what derivative is wanted:
                  // 0=Ux, 1=Uy, 2=Vx, 3=Vy
    int obj = i/4;  // which object: 0, 1
    switch (j) {
      case 0: return "x position "+obj;
      case 1: return "y position "+obj;
      case 2: return "x velocity "+obj;
      case 3: return "y velocity "+obj;
      default: return "";
    }
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS_SPECIAL))
      {m[0].m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(MASS)) {
      for (int i=1; i<m.length; i++)
        m[i].m_Mass = value;
      return true;
    } else if (name.equalsIgnoreCase(ELASTICITY))
      {m_Elasticity = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {m_Gravity = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {m_Damping = value; return true;}
    else if (name.equalsIgnoreCase(STIFFNESS)) {
      for (int i=0; i<nsg.length; i++)
        s[nsg[i]].m_SpringConst = value;
      return true;
    } else if (name.equalsIgnoreCase(LENGTH)) {
      for (int i=0; i<nsg.length; i++)
        s[nsg[i]].m_RestLength = value;
      return true;
    } else if (name.equalsIgnoreCase(STIFF_SPECIAL)) {
      for (int i=0; i<sg.length; i++)
        s[sg[i]].m_SpringConst = value;
      return true;
    } else if (name.equalsIgnoreCase(LENGTH_SPECIAL)) {
      for (int i=0; i<sg.length; i++)
        s[sg[i]].m_RestLength = value;
      return true;
    } else if (name.equalsIgnoreCase(GRADIENT)) {
      boolean wantGradient = value != 0;
      if (wantGradient && !showGradient) {
        cvs.prependElement(gradient);
        showGradient = true;
      } else if (!wantGradient && showGradient) {
        cvs.removeElement(gradient);
        showGradient = false;
      }
      return true;
    }
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS_SPECIAL))
      return m[0].m_Mass;
    else if (name.equalsIgnoreCase(MASS))
      return m[1].m_Mass;
    else if (name.equalsIgnoreCase(ELASTICITY))
      return m_Elasticity;
    else if (name.equalsIgnoreCase(GRAVITY))
      return m_Gravity;
    else if (name.equalsIgnoreCase(DAMPING))
      return m_Damping;
    else if (name.equalsIgnoreCase(STIFFNESS))
      return s[nsg[0]].m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH))
      return s[nsg[0]].m_RestLength;
    else if (name.equalsIgnoreCase(STIFF_SPECIAL))
      return (sg.length>0) ? s[sg[0]].m_SpringConst : 0;
    else if (name.equalsIgnoreCase(LENGTH_SPECIAL))
      return (sg.length>0) ? s[sg[0]].m_RestLength : 0;
    else if (name.equalsIgnoreCase(GRADIENT))
      return showGradient ? 1 : 0;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  public void objectChanged(Object o) {
    if (o == cvs) {
      DoubleRect box = cvs.getSimBounds();
      m_Walls.setBounds(box);
      double w = m[0].m_Width;
      m_Left = box.getXMin() + w/2;
      m_Right = box.getXMax() - w/2;
      m_Bottom = box.getYMin() + w/2;
      //m_Bottom += 2;
      m_Top = box.getYMax() - w/2;
    }
  }

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) {
      stop();
    }
  }

  // sets position of masses so that there is no motion
  private void stop()
  {
    /* vars: 0   1   2   3   4   5   6   7   8   9  10  11
            U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y */
    for (int i=0; i<vars.length; i++)
      vars[i] = 0;
    // arrange all masses around a circle
    double r = 1; // radius
    for (int i=0; i<m.length; i++)
    {
      double rnd = 1+ 0.1*Math.random();
      vars[0 + i*4] = r*Math.cos(rnd*i*2*Math.PI/m.length);
      vars[1 + i*4] = r*Math.sin(rnd*i*2*Math.PI/m.length);
    }
    /*  rotating star for 4 masses
    double v = 3;  // velocity
    double l = 2;  // length of springs
    // ball 1 at 90 degrees, vel=(-v,0)
    vars[5] = l;
    vars[6] = -v;
    // ball 2 at -30 degrees
    vars[0 + 2*4] = l*Math.cos(Math.PI/6);
    vars[1 + 2*4] = -l*Math.sin(Math.PI/6);
    vars[2 + 2*4] = v*Math.cos(Math.PI/3);
    vars[3 + 2*4] = v*Math.sin(Math.PI/3);
    vars[0 + 3*4] = -l*Math.cos(Math.PI/6);
    vars[1 + 3*4] = -l*Math.sin(Math.PI/6);
    vars[2 + 3*4] = v*Math.cos(Math.PI/3);
    vars[3 + 3*4] = -v*Math.sin(Math.PI/3);
    */
  }

  // modifies visual objects according to current value of variables
  public void modifyObjects()  {
      /* vars: 0   1   2   3   4   5   6   7   8   9  10  11
              U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y */
    // assume all masses are same width & height
    double w = m[0].m_Width/2;
    for (int i=0;i<m.length;i++)  {
      m[i].setX1(vars[4*i] - w);
      m[i].setY1(vars[1 + 4*i] - w);
    }
    for (int i=0;i<msm.length;i++)  {
      CSpring spr = s[i];
      CMass m1 = m[msm[i][0]];
      spr.setX1(m1.m_X1 + w);
      spr.setY1(m1.m_Y1 + w);
      CMass m2 = m[msm[i][1]];
      spr.setX2(m2.m_X1 + w);
      spr.setY2(m2.m_Y1 + w);
    }
    tx.setNumber(getEnergy());
    if (showGradient)
      gatherMatrix();
  }

  private void gatherMatrix() {
    double[][] mat = gradient.getMatrix();
    CMass m1 = m[gradientMass];
    // don't draw the mass so we can see the gradient better.
    //m1.m_DrawMode = CElement.NO_DRAW;
    double saveX = m1.m_X1;
    double saveY = m1.m_Y1;
    double w = m1.m_Width/2;
    gradient.setCenterX(m1.m_X1 + w);
    gradient.setCenterY(m1.m_Y1 + w);
    int R = mat.length;
    if (R == 0)
      return;
    int C = mat[0].length;
    if (C == 0)
      return;
    double gradW = gradient.getWidth();
    double gradH = gradient.getHeight();  // gradient width & height
    double incX = gradW/C, incY = gradH/R;
    double left = m1.m_X1 + w - gradW/2;
    double bottom = m1.m_Y1 + w - gradH/2;
    double x,y;
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    for (int i=0; i<R; i++) {
      for (int j=0; j<C; j++) {
        x = left + incX*i;
        y = bottom + incY*j;
        for (int l=0;l<msm.length;l++)  {
          if (msm[l][0] == gradientMass) {
            s[l].setX1(x);
            s[l].setY1(y);
          }
          if (msm[l][1] == gradientMass) {
            s[l].setX2(x);
            s[l].setY2(y);
          }
        }
        m1.m_X1 = x - w;
        m1.m_Y1 = y - w;
        double e = getEnergy();
        mat[i][j] = e;
        if (e < min)
          min = e;
        if (e > max)
          max = e;
      }
    }
    // normalize the matrix
    double range = max - min;
    for (int i=0; i<R; i++) {
      for (int j=0; j<C; j++) {
        // take the cube root to emphasize the lower range of energy values
        mat[i][j] = Math.pow((mat[i][j] - min)/range, 0.33333333);
      }
    }

    x = saveX+w;
    y = saveY+w;
    for (int l=0;l<msm.length;l++)  {
      if (msm[l][0] == gradientMass) {
        s[l].setX1(x);
        s[l].setY1(y);
      }
      if (msm[l][1] == gradientMass) {
        s[l].setX2(x);
        s[l].setY2(y);
      }
    }
    m1.m_X1 = saveX;
    m1.m_Y1 = saveY;

  }

  private double getEnergy() {
    // We assume that modifyObjects() has been called so the objects are
    // in their current positions corresponding to the vars[] array.
    // kinetic energy is 1/2 m v^2
    double ke = 0;
    for (int i=0;i<m.length;i++)  {
      double vx = vars[2 + i*4];
      double vy = vars[3 + i*4];
      ke += 0.5 * m[i].m_Mass * (vx*vx + vy*vy);
    }
    double se = 0;
    // spring potential = 0.5*k*x^2 where k = spring const, x = spring stretch
    for (int i=0;i<s.length;i++)  {
      double dx = s[i].m_X1 - s[i].m_X2;
      double dy = s[i].m_Y1 - s[i].m_Y2;
      double len = Math.sqrt(dx*dx + dy*dy);
      double x = len - s[i].m_RestLength;
      se += 0.5 * s[i].m_SpringConst * x*x;
    }
    double ge = 0;
    // gravity potential = m g (y - floor)
    for (int i=0;i<m.length;i++)
      ge += m[i].m_Mass * m_Gravity * (m[i].m_Y1 + m[i].m_Width/2 - m_Bottom);
    return ke+se+ge;
  }

  // called when starting to drag an object, turns off calculations for
  // that object so that only the mouse controls it.
  public void startDrag(Dragable e)  {
    /* vars: 0   1   2   3   4   5   6   7   8   9  10  11
            U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y */
    for (int i=0; i<m.length; i++)
      if (e==m[i])
        for (int j=0; j<4; j++)
          calc[j + 4*i] = false;
  }

  // constrainedSet() is called while the element is being dragged by the mouse.
  // The mouse coordinates are passed in as x, y.  This routine then
  // will set the element to be at that position, but constrained in some way.
  // In this case, we disallow dragging outside of the simulation window.
  public void constrainedSet(Dragable e, double x, double y) {
    // use center of mass instead of topLeft
    double w = m[0].m_Width/2;
    x += w;
    y += w;

    // disallow drag outside of window
    if (x < m_Left)
      x = m_Left + 0.0001;
    if (x > m_Right)
      x = m_Right - 0.0001;
    if (y < m_Bottom)
      y = m_Bottom + 0.0001;
    if (y > m_Top)
      y = m_Top - 0.0001;

    for (int i=0; i<m.length; i++)
      if (e==m[i]) {
        vars[4*i] = x;
        vars[1 + 4*i] = y;
        vars[2 + 4*i] = 0;
        vars[3 + 4*i] = 0;
        // set the previous mass back to normal drawing
        /*
        m[gradientMass].m_DrawMode = CElement.MODE_CIRCLE_FILLED;
        gradientMass = i;
        */
      }
  }

  private void addCollision(int obj1, int obj2) {
    collisions.addElement(new int[] {obj1, obj2});
  }

  public Vector findAllCollisions() {
    collisions.removeAllElements();  // forget any previous value
    for (int j=0; j<m.length; j++) {
    /* vars: 0   1   2   3   4   5   6   7   8   9  10  11
            U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y */
      if (vars[4*j] < m_Left)
        addCollision(LEFT_WALL, j);
      if (vars[4*j] > m_Right)
        addCollision(RIGHT_WALL, j);
      if (vars[1+4*j] < m_Bottom)
        addCollision(BOTTOM_WALL, j);
      if (vars[1+4*j] > m_Top)
        addCollision(TOP_WALL, j);
    }
    return (collisions.size() > 0) ? collisions : null;
  }

  public void handleCollisions(Vector collisions) {
    for (int i=0; i < collisions.size(); i++) {
      int [] objs = (int[])collisions.elementAt(i);
      /* vars: 0   1   2   3   4   5   6   7   8   9  10  11
              U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y */
      int idx = 4*objs[1]; // mass index
      switch (objs[0]) {
        case LEFT_WALL:
        case RIGHT_WALL:
          vars[2+idx] = -m_Elasticity * vars[2+idx]; break;
        case TOP_WALL:
        case BOTTOM_WALL:
          vars[3+idx] = -m_Elasticity * vars[3+idx]; break;
      }
    }
  }

  /* 2-D spring simulation with gravity
    y increases UP

         m2     .
          \     .
           \ th .
            \   .
             \  .
              \ .
               m1

    m1, m2 = masses of particle 1 and 2
    th = angle formed with vertical, 0=up, positive is counter clockwise
    L = displacement of spring from rest length
    R = rest length
    U1, U2 = position of CENTER of mass of particle 1 or 2
    V1, V2 = velocity of particle
    F1, F2 = force on particle
    k = spring constant
    b1, b2 = damping constants for each particle

    F1x = k L sin(th) -b1 V1x = m1 V1x'
    F1y = -m1 g +k L cos(th) -b1 V1y = m1 V1y'
    F2x = -k L sin(th) -b2 V2x = m2 V2x'
    F2y = -m2 g -k L cos(th) -b2 V2y = m2 V2y'
    xx = U2x - U1x
    yy = U2y - U1y
    len = sqrt(xx^2+yy^2)
    L = len - R
    cos(th) = yy / len
    sin(th) = xx / len

    CONTACT FORCE
    We detect when a particle is in resting contact with floor or wall.
    Consider contact with the floor.  Suppose the particle is 'close' to
    the floor, then there are 3 cases:
      1. vertical velocity is 'large' and positive.  Then the particle is
      separating from the floor, so nothing needs to be done.
      2. vertical velocity is 'large' and negative.  A collision is imminent,
      so let the collision software handle this case.
      3. vertical velocity is 'small'.  Now the particle is likely in contact
      with the floor.  There are two cases:
        a.  Net force positive = particle is being pulled off floor.  In this
        case do nothing, there is no reaction force from the floor.
        b.  Net force negative = particle is being pulled downwards.
        Here, we set the net force to zero, because the force is resisted
        by the reaction force from the floor.

    How small is 'small' velocity?
      We are trying to avoid the case where there is a tiny upwards velocity
      and a large downwards force, which just results in zillions of collisions
      over the time step we are solving (typically about 0.03 seconds).
      We assume that the particle stops bouncing and comes into
      contact with the floor in this case.
      For a given force (assuming it stays approx constant over the time span
      of 0.03 seconds), there is an 'escape velocity' that would allow the particle
      to leave contact and be above the floor at the end of the time step.
        (If the particle is still below the floor at the end of the timestep,
        then the animation would not look any different, even though there is
        some small amount of physics still happening... so our threshold here
        depends on whether physical accuracy or visual appearance is more
        important.... we could have the collision software handle some
        intermediate collisions during the timestep if desired... so you
        could shorten the timestep to get higher accuracy)

      Let the time step = h, the force = F, mass = m, initial vertical velocity = v0.
      Then we have
        v' = F/m
        v = (F/m)t + v0
        y = (F/2m)t^2 + v0*t
      Requiring the particle to be below the floor at time h gives the condition
        0 > y = (F/2m)h^2 + v0*h
      Dividing by h gives
        0 > F*h/2m + v0
        -F*h/2m > v0
      This is our definition of a small velocity.  Note that it depends
      on the net force.  Because with a large downward force, it would take a big
      velocity to actually result in contact being lost at the end of the time period.
      Equivalently, if there is just a slight downward force (e.g. spring almost
      offsetting gravity), then just a little velocity is enough to result in
      contact being broken.

    vars: 0   1   2   3   4   5   6   7   8   9  10  11 ...
         U0x U0y V0x V0y U1x U1y V1x V1y U2x U2y V2x V2y ...
  */
  public void evaluate(double[] x, double[] change)
  {
    double DIST_TOL = 0.02;
    double timeStep = 0.03;  // assume timeStep is this length or longer

    // i = index of variable whose derivative we want to calc
    for (int i=0; i<vars.length; i++) {
      int j = i%4;  // % is mod, so j tells what derivative is wanted:
                    // 0=Ux, 1=Uy, 2=Vx, 3=Vy
      int obj = i/4;  // obj is the 'particle number', from 0 to 5
      if ((j==0)||(j==1))  // requested derivative for Ux or Uy
        change[i] = x[i+2]; // derivative of position U is velocity V
      else  {
        // requested derivative is Vx or Vy for particle number 'obj'
        double r = 0;  // result net force
        double mass = m[obj].m_Mass;  // mass of our object

        // for each spring, get force from spring,
        // look at LHS (left hand side) of msm matrix
        for (int k=0; k<msm.length; k++)
          if (msm[k][0] == obj) { // this spring is connected to our object
            int obj2 = msm[k][1];  // the object on other end of the spring
            double xx = x[4*obj2] - x[4*obj];  // x distance between objects
            double yy = x[1 + 4*obj2] - x[1 + 4*obj];  // y distance betw objects
            double len = Math.sqrt(xx*xx + yy*yy);  // total distance betw objects
            CSpring spr = s[k];
            double sc = spr.m_SpringConst;  // spring constant for this spring
            // see earlier comments for more on the force equations.
            // Fx = (sc/m)*(len - R)*xx/len or
            // Fy = (sc/m)*(len - R)*yy/len - g
            double f = (sc/mass)*(len - spr.m_RestLength)/len;
            r += (j==2) ? f*xx : -m_Gravity + f*yy;
          }

        // same deal, but look at RHS (right hand side) of the msm matrix
        for (int k=0; k<msm.length; k++)
          if (msm[k][1] == obj)  { // this spring is connected to our object
            int obj2 = msm[k][0];  // the object on other end of the spring
            double xx = x[4*obj2] - x[4*obj];  // x distance between objects
            double yy = x[1 + 4*obj2] - x[1 + 4*obj];  // y distance betw objects
            double len = Math.sqrt(xx*xx + yy*yy);  // total distance betw objects
            CSpring spr = s[k];
            double sc = spr.m_SpringConst;  // spring constant for this spring
            // see earlier comments for more on the force equations.
            // Fx = (sc/m)*(len - R)*xx/len or
            // Fy = (sc/m)*(len - R)*yy/len - g
            double f = (sc/mass)*(len - spr.m_RestLength)/len;
            r += (j==2) ? f*xx : -m_Gravity + f*yy;
          }
        if (m_Damping != 0)
          r -= (m_Damping/mass)*x[i];

        // Handle resting contact forces.
        // 0=Ux, 1=Uy, 2=Vx, 3=Vy
        if (j == 3) { // calculating Vy, so check for vertical contact force
          // floor contact if (downward force, near floor, and low velocity)
          if (r<0 && Math.abs(x[4*obj+1] - m_Bottom)<DIST_TOL && Math.abs(x[4*obj+3])<-r*timeStep/(2*mass)) {
            // also set velocity to zero, to avoid buildup of small velocity over time
            r = x[4*obj+3] = 0;
            x[4*obj+1] = m_Bottom;
          } else if (r>0 && Math.abs(x[4*obj+1]-m_Top)<DIST_TOL && Math.abs(x[4*obj+3])<r*timeStep/(2*mass)) {
            r = x[4*obj+3] = 0;
            x[4*obj+1] = m_Top;
          }
        } else if (j==2) { // calculating Vx, so check for horizontal contact force
          if (r<0 && Math.abs(x[4*obj]-m_Left)<DIST_TOL && Math.abs(x[4*obj+2])<-r*timeStep/(2*mass)) {
            r = x[4*obj+2] = 0;
            x[4*obj] = m_Left;
          } else if (r>0 && Math.abs(x[4*obj]-m_Right)<DIST_TOL && Math.abs(x[4*obj+2])<r*timeStep/(2*mass)) {
            r = x[4*obj+2] = 0;
            x[4*obj] = m_Right;
          }
        }
        change[i] = r;
      }
    }
  }

}
