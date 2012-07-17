/* DoubleSpringSim
*/
package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// Wall, 3 Springs & 2 Masses connected as W-S1-M1-S2-M2-S3-W2

/* The number of springs can be either 2 or 3...
If 3 springs are selected, then extra controls will appear for that
spring and the right wall position.
*/
public class DoubleSpringSim extends Simulation implements ActionListener {
  private CSpring m_Spring1, m_Spring2, m_Spring3;
  private CMass m_Mass1, m_Mass2;
  //private CWall m_Wall;
  private JButton button_stop;
  private static final String MASS_L = "left mass",
      LENGTH_1 = "spring 1 length",
      STIFF_1 = "spring 1 stiffness",
      MASS_R = "right mass",
      STIFF_3 = "spring 3 stiffness",
      LENGTH_3 = "spring 3 length",
      STIFF_2 = "spring 2 stiffness",
      LENGTH_2 = "spring 2 length",
      WALL_R = "right wall position";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS_L, MASS_R, STIFF_1, LENGTH_1,
      STIFF_2, LENGTH_2};
  private String[] params3 = {MASS_L, MASS_R, STIFF_1, LENGTH_1,
      STIFF_2, LENGTH_2, STIFF_3, LENGTH_3, WALL_R};

  public DoubleSpringSim(Container container, int numSprings) {
    super(container, 4);
    // if 3 springs requested, then show additional controls
    if (numSprings==3)
      params = params3;
    var_names = new String[] {
      "position left",
      "position right",
      "velocity left",
      "velocity right",
      "acceleration left",
      "acceleration right",
      "total energy"
      };
    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -0.5, 9.5, -1.5, 1.5,
        CoordMap.ALIGN_LEFT, CoordMap.ALIGN_MIDDLE));
    m_Spring1 = new CSpring (0, 0, 2, 0.5); // x1, y1, restLen, thickness
    m_Spring1.m_SpringConst = 6.0;
    cvs.addElement(m_Spring1);

    double w = 0.75;
    /* x1, y1, width, height, drawmode */
    m_Mass1 = new CMass(m_Spring1.m_X2+0.5, -w/2, w, w, CElement.MODE_RECT);
    m_Mass1.m_Mass = 1.0;
    cvs.addElement(m_Mass1);

    m_Spring2 = new CSpring (m_Mass1.m_X2, 0, 2.0, 0.5); // x1, y1, restLen, height
    m_Spring2.m_SpringConst = 6.0;
    cvs.addElement(m_Spring2);

    // x1, y1, height, width
    m_Mass2 = new CMass(m_Spring2.m_X2+1.0, -w/2, w, w, CElement.MODE_RECT);
    m_Mass2.m_Mass = 1.0;
    cvs.addElement(m_Mass2);

    m_Spring3 = new CSpring (m_Mass2.m_X2, 0, 2, 0.5); // x1, y1, restLen, height
    m_Spring3.m_SpringConst = (numSprings==3) ? 6 : 0;
    m_Spring3.m_X2 += 1.0;
    cvs.addElement(m_Spring3);

    vars[0] = m_Mass1.m_X1;
    vars[1] = m_Mass2.m_X1;
    vars[2] = 0;
    vars[3] = 0;
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
    showGraph(false);
    if (graph!=null)
      graph.setVars(0,1);
  }

  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS_L))
      {m_Mass1.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(STIFF_1))
      {m_Spring1.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH_1))
      {m_Spring1.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(MASS_R))
      {m_Mass2.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(STIFF_2))
      {m_Spring2.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH_2))
      {m_Spring2.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(STIFF_3))
      {m_Spring3.m_SpringConst = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH_3))
      {m_Spring3.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(WALL_R))
      {m_Spring3.setX2(value); return true;}
    return super.trySetParameter(name, value);
  }

  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS_L))
      return m_Mass1.m_Mass;
    else if (name.equalsIgnoreCase(STIFF_1))
      return m_Spring1.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH_1))
      return m_Spring1.m_RestLength;
    else if (name.equalsIgnoreCase(MASS_R))
      return m_Mass2.m_Mass;
    else if (name.equalsIgnoreCase(STIFF_2))
      return m_Spring2.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH_2))
      return m_Spring2.m_RestLength;
    else if (name.equalsIgnoreCase(STIFF_3))
      return m_Spring3.m_SpringConst;
    else if (name.equalsIgnoreCase(LENGTH_3))
      return m_Spring3.m_RestLength;
    else if (name.equalsIgnoreCase(WALL_R))
      return m_Spring3.m_X2;
    return super.getParameter(name);
  }

  public String[] getParameterNames() {
    return params;
  }

  /* stop calculation... find the state where system is stable and motionless
    need forces on each block to be zero
      0 = -k1 L1 + k2 L2
      0 = -k2 L2 + k3 L3
    Really its an equation for finding u1, u2 = position of the two blocks.
      0 = -k1 (u1 - R1) + k2 (u2 - (u1+w1) - R2)
      0 = -k2 (u2 - (u1+w1) - R2) + k3 (Wall2 - (u2 + w2) - R3)
    OK, 2 equations in 2 unknowns... solving the first for u2:
      0 = -(k1/k2) (u1 - R1) + u2 - (u1+w1) - R2
      u2 = (k1/k2)(u1-R1) + u1 + w1 + R2
    plug that into the second and solve for u1:
      0 = -k2 (u2 - (u1+w1) - R2) + k3 (Wall2 - (u2 + w2) - R3)
      0 = -k1(u1-R1) + k3(Wall2 - ((k1/k2)(u1-R1) + u1 + w1 + R2) - w2 - R3)
      0 = u1 (-k1 -k3 k1/k2 - k3) + k1 R1 + k3 (Wall2 + k1 R1/k2 - w1 -R2 -w2 -R3)
      u1 = {k1 R1 + k3 (Wall2 + k1 R1/k2 - w1 -R2 -w2 -R3)}/(k1 + k3 (1+k1/k2))

    If k2=0, then simply set springs 1 and 3 to rest lengths.
  */
  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) {
      double k1,k2,k3,u1,u2,w1,w2,R1,R2,R3,Wall2;
      k1=m_Spring1.m_SpringConst;  k2=m_Spring2.m_SpringConst;  k3=m_Spring3.m_SpringConst;
      w1=m_Mass1.m_Width;   w2=m_Mass2.m_Width;
      R1=m_Spring1.m_RestLength;  R2=m_Spring2.m_RestLength;  R3=m_Spring3.m_RestLength;
      Wall2=m_Spring3.m_X2;
      if (k2==0) {
        vars[0] = R1;
        vars[1] = Wall2-R3-w2;
      } else {
        u1=k1*R1 + k3*(Wall2 + k1*R1/k2 - w1 -R2 -w2 -R3);
        u1= u1/(k1 +k3*(1+k1/k2));
        vars[0] = u1;
        vars[1] = (k1/k2)*(u1-R1) + u1 + w1 + R2;
      }
      vars[2] = 0;  // velocity 1
      vars[3] = 0;  // velocity 2
    }
  }

  public void modifyObjects()
  {
    m_Mass1.setX1(vars[0]);
    m_Spring1.setX2(m_Mass1.m_X1);
    m_Mass2.setX1(vars[1]);
    m_Spring2.setX1(m_Mass1.m_X2);
    m_Spring2.setX2(m_Mass2.m_X1);
    m_Spring3.setX1(m_Mass2.m_X2);
  }

  public void startDrag(Dragable e) {
    if (e==m_Mass1) {
      calc[0] = false;
      calc[2] = false;
    } else if (e==m_Mass2) {
      calc[1] = false;
      calc[3] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y)
  {
    if (e==m_Mass1) {
      vars[0] = x;
      vars[2] = 0;  // velocity
    } else if (e==m_Mass2) {
      vars[1] = x;
      vars[3] = 0;  // velocity
    }
    modifyObjects();
    // objects other than mass are not allowed to be dragged
  }

  public int numVariables() {
    return var_names.length;
  }

  public double getVariable(int i) {
    if (i<=3)
      return vars[i];
    if (i<=5) {   // accelerations
      double[] rate = new double[vars.length];   // this creates lots of heap garbage!
      evaluate(vars, rate);
      return rate[i-2];
    }
    // find kinetic energy
    double k = 0.5*(m_Mass1.m_Mass*vars[2]*vars[2] +
        m_Mass2.m_Mass*vars[3]*vars[3]);
    // find extension of each spring
    double x1 = vars[0] - m_Spring1.m_RestLength;
    double x2 = vars[1] - vars[0] - m_Mass1.m_Width - m_Spring2.m_RestLength;
    double x3 = m_Spring3.m_X2 - (vars[1]+m_Mass2.m_Width) - m_Spring3.m_RestLength;
    // find potential spring energy
    double p = 0.5*(m_Spring1.m_SpringConst*x1*x1 +
          m_Spring2.m_SpringConst*x2*x2 + m_Spring3.m_SpringConst*x3*x3);
    return k + p; // total energy
  }

  // assume that one end of the spring1 is fixed at 0, the other end is at position x1.
  // spring2 stretches from x1 to x2
  // variables are:  u1, u2, v1, v2
  /*
    origin = connection of spring1 to wall
    vars[0] = u1 = x1 position of mass1 with origin as above
    vars[1] = u2 = x1 position of mass2 with origin as above
    vars[2] = v1 = velocity of mass1
    vars[3] = v2 = velocity of mass2

    Abbreviations for diff eqs
    R = rest length of spring
    w = width of mass
    k = spring constant
    m = mass
    u = mass.x1 position
    L = how much spring is stretched
    F = force
    len = current length of spring
    Wall2 = position of 2nd wall  (= spring3.m_X2 which is fixed)

    L1 = u1 - R1
    len2 = u2 - (u1 + w1)
    L2 = len2 - R2 = u2 - (u1 + w1) - R2
    L3 = len3 - R3 = Wall2 - (u2 + w2) - R3
    F1 = -k1 L1 + k2 L2 = m1 v1'
    F2 = -k2 L2 + k3 L3 = m2 v2'

    so the diff eq's are
    u1' = v1
    u2' = v2
    v1' = -(k1/m1) (u1 - R1) + (k2/m1) (u2 - (u1 + w1) - R2)
    v2' = -(k2/m2) (u2 - (u1 + w1) - R2) + (k3/m2) (wall2 - (u2+w2) - R3)

  */
  public void evaluate(double[] x, double[] change ){
    // u1' = v1
    change[0] = x[2];
    // u2' = v2
    change[1] = x[3];
    // v1' = -(k1/m1) (u1 - R1) + (k2/m1) (u2 - (u1 + w1) - R2)
    double r1 = -(m_Spring1.m_SpringConst / m_Mass1.m_Mass)*
      (x[0] - m_Spring1.m_RestLength);
    double r2 = (m_Spring2.m_SpringConst / m_Mass1.m_Mass)*
      (x[1] - x[0] - m_Mass1.m_Width - m_Spring2.m_RestLength);
    change[2] = r1+r2;
    // v2' = -(k2/m2) (u2 - (u1 + w1) - R2) + (k3/m2) (wall2 - (u2+w2) - R3)
    change[3] = -(m_Spring2.m_SpringConst / m_Mass2.m_Mass)*
      (x[1] - x[0] - m_Mass1.m_Width - m_Spring2.m_RestLength);
    change[3] += (m_Spring3.m_SpringConst / m_Mass2.m_Mass) *
      (m_Spring3.m_X2 - (x[1] + m_Mass2.m_Width) - m_Spring3.m_RestLength);
    }

}


