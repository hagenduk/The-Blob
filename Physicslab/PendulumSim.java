/*
  File: PendulumSim.java

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

public class PendulumSim extends Simulation
  {
  private CMass m_Mass;
  private CSpring m_Spring;
  private CArc m_Drive;
  private double driveAmplitude = 1.15;  /* 0 for normal pendulum; 1.15 for chaos */
  private double m_DriveFrequency = 2.0/3.0;  /* 2/3 for chaos */
  private double m_Gravity = 1.0;  /* 6 for normal pendulum;  1.0 for chaos */

  public String toString() {
    return "Pendulum simulation";
  }

  private static final String MASS = "mass",
      DAMPING = "damping",
      LENGTH = "length",
      AMPLITUDE = "drive amplitude",
      FREQUENCY = "drive frequency",
      GRAVITY = "gravity",
      RADIUS = "radius";
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {MASS, DAMPING, LENGTH, AMPLITUDE, FREQUENCY, GRAVITY,
    RADIUS};

  public PendulumSim(Container container) {
    super(container, 3);
    var_names = new String[] {
      "angle",
      "angular velocity",
      "time",
      "angular accel"
      };

    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -1.5, 1.5, -1.5, 1.5,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

    // the CArc will track the drive frequency
    // the radius of the arc = 0.5 * the drive amplitude A
    // amplitude 0.5<A<1.5 is one of the settings for chaos.
    // A=1.15 is the preferred chaos value, so use r = 0.5*1.15
    // CArc params: (X1, Y1,  r,  angle0, angle)
    m_Drive = new CArc(0, 0, (0.5*driveAmplitude), -90, 0);
    cvs.addElement(m_Drive);

    double len = 1;
    // x1, y1, restLen, thickness, drawing mode
    m_Spring = new CSpring (0, 0, len, 0.4);
    m_Spring.m_DrawMode = CElement.MODE_LINE;
    cvs.addElement(m_Spring);

    double w = 0.3;
    // assume angle is zero at start (pendulum hanging straight down)
    m_Mass = new CMass( -w/2 + Math.sin(0)*len,
               -w/2 + Math.cos(0)*len, w, w, CElement.MODE_CIRCLE);
    m_Spring.setX2(m_Mass.m_X2 + w/2);
    m_Spring.setY2(m_Mass.m_Y2 + w/2);
    m_Mass.m_Mass = 1;
    m_Mass.m_Damping = 0.5;  // 0.5 for chaos;  normally 0
    cvs.addElement(m_Mass);

    vars[0] = Math.PI/4;  // angle
    vars[1] = 0;  // velocity
    vars[2] = 0;  // time
    modifyObjects();
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null) {
      graph.setDrawMode(Graph.DOTS);
			// this zVar feature is turned off as of 10/10/06
      graph.setZVar(3);  // this also causes graph to draw in color
    }
  }

  public void setupControls() {
    super.setupControls();
    // DoubleField params:  subject, name, fraction digits
    addObserverControl(new DoubleField(this, MASS, 3));
    addObserverControl(new DoubleField(this, DAMPING, 3));
    addObserverControl(new DoubleField(this, LENGTH, 3));
    addObserverControl(new DoubleField(this, AMPLITUDE, 3));
    addObserverControl(new DoubleField(this, FREQUENCY, 7));
    addObserverControl(new DoubleField(this, GRAVITY, 3));
    showControls(true);
  }

  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS))
      {m_Mass.m_Mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {m_Mass.m_Damping = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH))
      {m_Spring.m_RestLength = value; return true;}
    else if (name.equalsIgnoreCase(AMPLITUDE))
      {driveAmplitude = value;
      m_Drive.m_Radius = 0.5*value; return true;}
    else if (name.equalsIgnoreCase(FREQUENCY))
      {m_DriveFrequency = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {m_Gravity = value; return true;}
    else if (name.equalsIgnoreCase(RADIUS)) {
      m_Mass.setHeight(2*value);
      m_Mass.setWidth(2*value);
      return true;}
    return super.trySetParameter(name, value);
  }

  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS))
      return m_Mass.m_Mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return m_Mass.m_Damping;
    else if (name.equalsIgnoreCase(LENGTH))
      return m_Spring.m_RestLength;
    else if (name.equalsIgnoreCase(AMPLITUDE))
      return driveAmplitude;
    else if (name.equalsIgnoreCase(FREQUENCY))
      return m_DriveFrequency;
    else if (name.equalsIgnoreCase(GRAVITY))
      return m_Gravity;
    else if (name.equalsIgnoreCase(RADIUS))
      return m_Mass.getWidth()*2;
    return super.getParameter(name);
  }

  public String[] getParameterNames() {
    return params;
  }

  public void modifyObjects()  {
    // cludge: limit the pendulum angle to +/- Pi
    // how much error are we introducing here???
    if (vars[0] > Math.PI)
      vars[0] = vars[0] - 2*Math.PI*Math.floor(vars[0]/Math.PI);
    else if (vars[0] < -Math.PI)
      vars[0] = vars[0] - 2*Math.PI*Math.ceil(vars[0]/Math.PI);

    // set the position of the pendulum according to the angle
    double len = m_Spring.m_RestLength;
    double w = m_Mass.m_Width/2;
    m_Mass.setX1(len*Math.sin(vars[0]) - w);
    m_Mass.setY1(len*Math.cos(vars[0]) - w);
    m_Spring.setX2(m_Mass.m_X1 + w);
    m_Spring.setY2(m_Mass.m_Y1 + w);

    // show the driving torque as a line circling about origin
    double t = m_DriveFrequency*vars[2];   // vars[2] = time
    // angle is the angle from the startAngle, so from -90 to 90 degrees
    t = 180*t/Math.PI;  // convert to degrees, starting at 0
    t = t - 360 *Math.floor(t/360);  // mod 360, range is 0 to 360
    // here we generate a ramp that works as follows:
    // we want to represent cos(k t)
    // 0   90   180   270   360
    // 90   0   -90     0    90
    if ((t>0) && (t<=180))  // 0 to 180 is reversed and offset
      t = 90 - t;
    else
      t = t - 270;
    m_Drive.m_Angle = t;
  }

  public int numVariables() {
    return var_names.length;
  }

  public double getVariable(int i) {
    if (i<=2)
      return vars[i];
    else {   // acceleration
      double[] rate = new double[vars.length]; // this creates lots of heap garbage!
      evaluate(vars, rate);
      return rate[1];
    }
  }

  public void startDrag(Dragable e) {
    if (e==m_Mass) {
      calc[0] = false;
      calc[1] = false;
    }
  }

  public void constrainedSet(Dragable e, double x, double y) {
    if (e==m_Mass) {
      // only allow movement along circular arc
      // calculate angle theta given current mass position & width & origin setting, etc.
      double w = m_Mass.m_Width/2;
      double th = Math.atan2(x+w, y+w);
      vars[0] = th;
      vars[1] = 0;
    }
  }

  /*
    mass is suspended from ceiling on a stick
    origin = connection point of stick to ceiling, with y increasing downwards
    th = angle formed with vertical, positive is counter clockwise
    U = position of CENTER of mass
    v = velocity of angle = d(th)/dt
    m = mass of mass
    g = gravity constant
    L = length of rope
    b = friction constant
    A = amplitude of driving force
    k = related to frequency of driving force

    Note:  we use 2*radius of arc as driving force

    Regard th as the only degree of freedom of the system (one-dimensional).
    there is no acceleration in the direction of the rope
    the only acceleration is perpendicular to the rope,

    Use the rotational analog of Newton's second law:
       Sum(torques) = I a
    where I = rotational inertia, and a = angular acceleration.

    Rotational inertia = mL^2
    Torque due to gravity is -Lmg sin(th)
    Torque due to friction is -b v
    Torque due to driving force is A cos(w) where A is constant amplitude
    and w = k t is a linear function of time.

    Then we have
      -Lmg sin(th) -b v +A cos(w) = mL^2 a

    If our variables are as above, we get the equations
      th' = v
      v' = -(g/L) sin(th) -(b/mL^2) v + (A/mL^2) cos(k t)

    Compare to equation 3.1 in Chaotic Dynamics by Baker/Gollub.
    (I've translated that equation to equivalent variables here...)
      v' = - sin(th) - v/q + A cos(k t)
    The range of chaos is:  q=2, 0.5<A<1.5, k=2/3.
    So if we have m=L=g=1, then we want g=1, 0.5<A<1.5, k=2/3, b=0.5.

    The position of the pendulum is given by
      Ux = L sin(th)
      Uy = L cos(th)
    and the variables and diffeq's are
      vars[0] = th
      vars[1] = v
      vars[2] = time
  */
  // variables are:  th, th', time
  public void evaluate(double[] x, double[] change ){
    // th' = v
    change[0] = x[1];
    // v' = -(g/L) sin(th) -(b/mL^2) v + (A/mL^2) sin(k t)
    double l = m_Spring.m_RestLength;
    double dd = -(m_Gravity/l)*Math.sin(x[0]);
    double mlsq = m_Mass.m_Mass * l * l;
    dd += -(m_Mass.m_Damping/mlsq) * x[1];
    dd += (driveAmplitude/mlsq) * Math.cos(m_DriveFrequency * x[2]);
    change[1] = dd;
    change[2] = 1;  // time
  }

}
