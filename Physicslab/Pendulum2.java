/*
  File: Pendulum2.java

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

public class Pendulum2 extends Simulation
  {
  private CMass m_Mass, m_Mass2;
	private double radius = 0.4; // radius of disk at end of pendulum
	private double mass = 1.0;
	private double damping = 0; // 0.5 for chaos
  private CSpring m_Spring, m_Spring2;
  private double m_Gravity = 1.0;  /* 6 for normal pendulum;  1.0 for chaos */
  protected Vector rxnForces = new Vector(20);
	private double offsetX = 0.5;
	private double offsetY = 0;

  public String toString() {
    return "Pendulum2 simulation";
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

  public Pendulum2(Container container) {
    super(container, 8);
    var_names = new String[] {
      "x position",
      "x velocity",
      "y position",
      "y velocity",
      "angle",
      "angular velocity",
			"angle2",
			"angle2 velocity"
      };

    setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -1.5, 1.5, -1.5, 1.5,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

    double len = 1.0;

		// pendulum 2 (uses equation of motion)
		// pendulum 1 is equivalent to pendulum 2 with length
		// len = (Icm / m + R^2) / R
		// Icm = m r^2 /2 where r = radius
		double len2 = (radius*radius/2.0 + len*len)/len;
		System.out.println("len="+len+" len2="+len2);
		m_Spring2 = new CSpring (0, 0, len2, 0.4);
    m_Spring2.m_DrawMode = CElement.MODE_LINE;
		m_Spring2.m_Color = Color.blue;
    cvs.addElement(m_Spring2);
    m_Mass2 = new CMass(0, 0, radius*2, radius*2, CElement.MODE_CIRCLE);
    cvs.addElement(m_Mass2);

    // x1, y1, restLen, thickness, drawing mode
    m_Spring = new CSpring (0, 0, len, 0.4);
    m_Spring.m_DrawMode = CElement.MODE_LINE;
		m_Spring.m_Color = Color.green;
    cvs.addElement(m_Spring);

    //double w = radius*2;
    // assume angle is zero at start (pendulum hanging straight down)
    m_Mass = new CMass(0, 0, radius*2, radius*2, CElement.MODE_CIRCLE);
    //m_Spring.setX2(m_Mass.m_X2 + w/2);
    //m_Spring.setY2(m_Mass.m_Y2 + w/2);
    //damping = 0.1;  // 0.5 for chaos;  normally 0
    cvs.addElement(m_Mass);

		// start with pendulum not moving, but hanging straight down.
    vars[4] = 3*Math.PI/4;  // angle
		vars[0] = len*Math.sin(vars[4]);
		vars[2] = -len*Math.cos(vars[4]);
    vars[1] = vars[3] = vars[5] = 0;  // velocity
		vars[6] = vars[4];  // angle2
		vars[7] = 0;  // angle2 velocity
    modifyObjects();
  }

  public void setupGraph() {
    super.setupGraph();
    if (graph!=null) {
      graph.setDrawMode(Graph.DOTS);
			graph.setXVar(4);
			graph.setYVar(5);
			// this zVar feature is turned off as of 10/10/06
      //graph.setZVar(3);  // this also causes graph to draw in color
    }
  }

  public void setupControls() {
    super.setupControls();
    // DoubleField params:  subject, name, fraction digits
    addObserverControl(new DoubleField(this, MASS, 3));
    addObserverControl(new DoubleField(this, DAMPING, 3));
    addObserverControl(new DoubleField(this, LENGTH, 3));
    addObserverControl(new DoubleField(this, GRAVITY, 3));
    showControls(true);
  }

  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(MASS))
      {mass = value; return true;}
    else if (name.equalsIgnoreCase(DAMPING))
      {damping = value; return true;}
    else if (name.equalsIgnoreCase(LENGTH)) {
			// need to move to new position as well... THIS IS NOT THREAD SAFE!!!
			// need to have this be done in the "run" thread between evaluations....
			m_Spring.m_RestLength = value; 
			vars[0] = value*Math.sin(vars[4]);
			vars[2] = -value*Math.cos(vars[4]);
			return true;
		} else if (name.equalsIgnoreCase(GRAVITY))
      {m_Gravity = value; return true;}
    return super.trySetParameter(name, value);
  }

  public double getParameter(String name) {
    if (name.equalsIgnoreCase(MASS))
      return mass;
    else if (name.equalsIgnoreCase(DAMPING))
      return damping;
    else if (name.equalsIgnoreCase(LENGTH))
      return m_Spring.m_RestLength;
    else if (name.equalsIgnoreCase(GRAVITY))
      return m_Gravity;
    return super.getParameter(name);
  }

  public String[] getParameterNames() {
    return params;
  }

  public void modifyObjects()  {
    // cludge: limit the pendulum angle to +/- Pi
    // how much error are we introducing here???
		
    if (vars[4] > Math.PI)
      vars[4] = vars[4] - 2*Math.PI*Math.floor(vars[4]/Math.PI);
    else if (vars[4] < -Math.PI)
      vars[4] = vars[4] - 2*Math.PI*Math.ceil(vars[4]/Math.PI);
		
		
    // set the position of the pendulum according to the angle
    double len = m_Spring.m_RestLength;
    //double w = m_Mass.m_Width/2;
    //m_Mass.setX1(vars[0] - w);
    //m_Mass.setY1(vars[2] - w);
		// center of spring = x, y
		// pivot point
    m_Spring.setX1(vars[0] - len*Math.sin(vars[4]));
    m_Spring.setY1(vars[2] + len*Math.cos(vars[4]));
		// free end
    m_Spring.setX2(vars[0]);
    m_Spring.setY2(vars[2]);
		m_Mass.setCenterX(m_Spring.m_X2);
		m_Mass.setCenterY(m_Spring.m_Y2);
		
    double len2 = m_Spring2.m_RestLength;
    m_Spring2.setX1(0 + offsetX);
    m_Spring2.setY1(0 + offsetY);
    m_Spring2.setX2(len2*Math.sin(vars[6]) + offsetX);
    m_Spring2.setY2(-len2*Math.cos(vars[6]) + offsetY);
		m_Mass2.setCenterX(m_Spring2.m_X2);
		m_Mass2.setCenterY(m_Spring2.m_Y2);
		
  }

  public int numVariables() {
    return var_names.length;
  }


  /*
		This version is for the pivot of the pendulum sliding on a horizontal surface.
		The contact force is normal to the horizontal surface (nominally pointing downwards).
		
  */
  public void evaluate2(double[] x, double[] change ){
		double m = mass;
    change[0] = x[1]; // x' = vx
		change[1] = 0;  // vx' = 0
		change[2] = x[3];  // y' = vy
		change[3] = - m_Gravity;  	// vy' = -m g / m
		change[4] = x[5]; // th' = w
		change[5] = 0;  // w' = 0
		
		// figure out and apply contact force...
    double len = m_Spring.m_RestLength;
		double nx = 0;
		double ny = 1;
		double rx = -len*Math.sin(x[4]);
		double ry = len*Math.cos(x[4]);
		double w = x[5];
		double vx = x[1];
		double vy = x[3];
		double A[][] = new double[1][2];
		double B[] = new double[1];
		double f[] = new double[1];
		//System.out.println("x[2]-ry="+(x[2]-ry));
		// try regarding the point on the stick as p1, and the point on wall as p2...
		// eqn (10) is NOT used in this case because the normal does not change.
		// eqn (10) gives 2 (W x n) . (v + W x r)
		// = 2* w (-ny, nx, 0) . (vx -w ry, vy + w rx, 0)
		// = 2* w (-ny(vx - w ry) + nx(vy + w rx))
		//		double b = 2*w*(-ny*(vx - w*ry) + nx*(vy + w*rx));
		//
		// eqn (11) is used:
		// W' = (0, 0, w'), so W' x r = (-w' ry, w' rx, 0)
		// W x (W x r) = W x (-w ry, w rx, 0) = w^2 (-rx, -ry, 0)
		// eqn (11) gives n . (v' + W' x r + w x (w x r))
		// = n . (vx' -w' ry - w^2 rx, vy' + w' rx - w^2 ry, 0)
		// = nx*(vx' -w' ry - w^2 rx) +ny*(vy' + w' rx - w^2 ry)
		double b = 0;
		b += nx*(change[1] -change[5]*ry - w*w*rx) + ny*(change[3] + change[5]*rx - w*w*ry);
		B[0] = b;
		// I = m (width^2 + height^2)/ 12
		double I = m*(len*len)/12;
		// eqn (9)  a = n . (n/ m + (r x n) x r /I)
		// (r x n) x r = {0, 0, rx ny - ry nx} x r = {-ry(rx ny - ry nx), rx(rx ny - ry nx), 0}
		// a = n . {nx/m -ry(rx ny - ry nx)/I, ny/m + rx(rx ny - ry nx)/I, 0}
		double a = nx*(nx/m -ry*(rx*ny - ry*nx)/I) + ny*(ny/m + rx*(rx*ny - ry*nx)/I);
		A[0][0] = a;
		A[0][1] = -B[0];
		Utility.matrixSolve(A, f);
		// d'' = 0 = f a + B
		// f = -B/a
		//double f = -B/a;
		// now apply the force f n to the pivot end.
		// x and y change according to f nx and f ny
		// acceleration = force/mass
		change[1] += f[0]*nx/m;
		change[3] += f[0]*ny/m;
		// w' = (r x f n) /I  = {0, 0, rx f ny - ry f nx} /I
		// not sure why, but needs a sign change here!
		change[5] += (rx*f[0]*ny - ry*f[0]*nx)/I;
		
		// find n.p1'', or p1y''
		double py = change[3] + change[5]*rx - w*w*ry;
		System.out.println("py''= "+py);
  }

	/*
		We regard there being two contact points at the pivot.
		Contact 0 is with a horizontal surface, contact 1 is with a vertical surface.
		n1 points downwards. n2 points rightward.
		
	*/
  public void evaluate(double[] x, double[] change ){
    while (!rxnForces.isEmpty()) {
      Drawable d = (Drawable)rxnForces.lastElement();
      cvs.removeElement(d);
      rxnForces.removeElement(d);
    }

		double m = mass;
    change[0] = x[1]; // x' = vx
		change[1] = -damping*x[1];  // vx' = -b vx
		change[2] = x[3];  // y' = vy
		change[3] = - m_Gravity - damping*x[3];  	// vy' = -g - b vy
		change[4] = x[5]; // th' = w
		change[5] = 0;  // w' = 0

    change[6] = x[7];  // angle2' = angle2 velocity
    // v' = -(g/L) sin(th) -(b/mL^2) v + (A/mL^2) sin(k t)
    double l2 = m_Spring2.m_RestLength;
    double dd = -(m_Gravity/l2)*Math.sin(x[6]);
    double mlsq = m * l2 * l2;
    dd += -(damping/mlsq) * x[7];
    change[7] = dd;
		
		// figure out and apply contact force...
    double len = m_Spring.m_RestLength;
		// parallel axis theorem: I = Icm + m R^2
		// rotational inertia of disk radius r about center = m r^2 /2
		double I = m*(radius*radius/2.0);
		double n0x = 0;
		double n0y = -1;
		double n1x = -1;
		double n1y = 0;
		double rx = -len*Math.sin(x[4]);
		double ry = len*Math.cos(x[4]);
		double vx = x[1];
		double vy = x[3];
		double w = x[5];
		// A matrix:  Aij = effect of fj on acceleration at contact i
		// last column of Aij is where -B goes
		double A[][] = new double[2][3];
		double B[] = new double[2];
		double f[] = new double[2];
		double nx, ny, nix, niy, b;
		nx = n0x;
		ny = n0y;
		// try regarding the point on the stick as p1, and the point on wall as p2...
		// eqn (10) gives 2 (w x ni) . (v + w x r)
		// = 2* w (-niy, nix, 0) . (vx -w ry, vy + w rx, 0)
		// = 2* w (-niy(vx - w ry) + nix(vy + w rx))
		//b = 2*w*(-ny*(vx - w*ry) + nx*(vy + w*rx));
		// W' = (0, 0, w'), so W' x r = (-w' ry, w' rx, 0)
		// w x (w x r) = w x (-w ry, w rx, 0) = w^2 (-rx, -ry, 0)
		// eqn (11) gives n . (v' + W' x r + w x (w x r))
		// = n . (vx' -w' ry - w^2 rx, vy' + w' rx - w^2 ry, 0)
		// = nx*(vx' -w' ry - w^2 rx) +ny*(vy' + w' rx - w^2 ry)
		b = nx*(change[1] -change[5]*ry - w*w*rx) + ny*(change[3] + change[5]*rx - w*w*ry);
		B[0] = b;
		
		// same formulas, but now for contact 1
		nx = n1x;
		ny = n1y;
		//b = 2*w*(-ny*(vx - w*ry) + nx*(vy + w*rx));
		b = nx*(change[1] -change[5]*ry - w*w*rx) + ny*(change[3] + change[5]*rx - w*w*ry);
		B[1] = b;
		
		// notation:  here nj = {nx, ny, 0}  and ni = {nix, nyx, 0}
		// I = m (width^2 + height^2)/ 12
		// eqn (9)  a = ni . (nj/ m + (r x nj) x r /I)
		// (r x n) x r = {0, 0, rx ny - ry nx} x r = {-ry(rx ny - ry nx), rx(rx ny - ry nx), 0}
		// a = ni . {nx/m -ry(rx ny - ry nx)/I, ny/m + rx(rx ny - ry nx)/I, 0}
		nx = n0x; ny = n0y;  nix = n0x; niy = n0y;
		A[0][0] = nix*(nx/m -ry*(rx*ny - ry*nx)/I) + niy*(ny/m + rx*(rx*ny - ry*nx)/I);
		nx = n1x; ny = n1y;  nix = n0x; niy = n0y;
		A[0][1] = nix*(nx/m -ry*(rx*ny - ry*nx)/I) + niy*(ny/m + rx*(rx*ny - ry*nx)/I);
		nx = n0x; ny = n0y;  nix = n1x; niy = n1y;
		A[1][0] = nix*(nx/m -ry*(rx*ny - ry*nx)/I) + niy*(ny/m + rx*(rx*ny - ry*nx)/I);
		nx = n1x; ny = n1y;  nix = n1x; niy = n1y;
		A[1][1] = nix*(nx/m -ry*(rx*ny - ry*nx)/I) + niy*(ny/m + rx*(rx*ny - ry*nx)/I);

		// d'' = 0 = A f + B
		// A f = -B
		// put -B in last column of A, which is where matrixSolve expects it to be.
		A[0][2] = -B[0];
		A[1][2] = -B[1];
		//System.out.println("x[0]+rx="+(x[0]+rx)+" x[2]+ry="+(x[2]+ry)+" x[4]="+x[4]);
		//System.out.println("B[0]="+B[0]+" B[1]="+B[1]);
		//System.out.println("A[0][0]="+A[0][0]+" A[0][1]="+A[0][1]+" A[1][0]="+A[1][0]+" A[1][1]="+A[1][1]);
		Utility.matrixSolve(A, f);
		//System.out.println("f[0]="+f[0]+" f[1]="+f[1]);
		// now apply the force f n to the pivot end.
		nx = n0x; ny = n0y;
		// x and y change according to f nx and f ny
		// acceleration = force/mass
		double Fx, Fy;
		Fx = f[0]*nx;
		Fy = f[0]*ny;
		showForce(0, 0, f[0]*nx, f[0]*ny);
		change[1] += f[0]*nx/m;
		change[3] += f[0]*ny/m;
		// w' = (r x f n) /I  = {0, 0, rx f ny - ry f nx} /I
		// not sure why, but needs a sign change here!
		change[5] += (rx*f[0]*ny - ry*f[0]*nx)/I;

		nx = n1x; ny = n1y;
		Fx += f[1]*nx;
		Fy += f[1]*ny;
		showForce(0, 0, f[1]*nx, f[1]*ny);
		change[1] += f[1]*nx/m;
		change[3] += f[1]*ny/m;
		change[5] += (rx*f[1]*ny - ry*f[1]*nx)/I;
		// find n.p1'', or p1y''
		double px = change[1] - change[5]*ry - w*w*rx;
		double py = change[3] + change[5]*rx - w*w*ry;
		//System.out.println("px''="+px+" py''= "+py);
		//System.out.println("");
		
		showForce(0, 0, Fx, Fy); 
  }

	private void showForce(double x, double y, double fx, double fy) {
    //CVector v = new CVector(c.impactX, c.impactY, c.normalX*f, c.normalY*f);
		CVector v = new CVector(x, y, fx, fy);
    cvs.addElement(v);
    rxnForces.addElement(v);
	}
}
