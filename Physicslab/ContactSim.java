/*
  File: ContactSim.java

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

/////////////////////////////////////////////////////////////////////////////////
/*  ContactSim
This class demonstrates rigid bodies with contact forces to allow
resting contact.  The contact forces prevent the bodies from interpenetrating
when they are in resting contact.  That is, they are not colliding, but have
edges and corners that are in continuous contact.

Overview of how it works:
The superclass takes care of collisions.  There should be no collisions
occuring when this contact code is active, because the collision code
detects collisions, backs up in time to just before the collision and
applies an appropriate collision impulse.

The algorithms here are based on the 97' Siggraph Notes by David Baraff,
and most importantly this paper:
David. Baraff. Fast contact force computation for nonpenetrating rigid bodies.
Computer Graphics Proceedings, Annual Conference Series: 23-34, 1994.  12 pages.
As of January 2004, there is a list of David Baraff's papers here
http://www-2.cs.cmu.edu/~baraff/papers/index.html

The idea is to calculate the exact amount of force needed to _just barely_
prevent the objects from penetrating.  This is calculated here in
the evaluate() method, which is called repeatedly by the differential
equation solver to advance the state of the world.

First we find all the contacts.  The criteria are:  the corner must be
close to the edge (distance tolerance), moving slowly (at least normal to
the edge), and accelerating towards the edge.

Now we have a set of contacts and the forces and velocities.  The forces
include things like gravity, thrust, rubber band, damping.  What we want
to find are the forces at the contact points.  We set up a matrix equation
 a = A f + b
where a = vector of accelerations,
      A = matrix describing how the i'th contact force affects
         the acceleration of the 'contact distance', ie. the separation
         between the bodies.
      f = vector of contact forces
      b = external forces (gravity, thrust, rubber band, damping)

Here is how to set up the A matrix:
For each contact distance d_i, find how the acceleration of that contact distance
d_i'' is related to the force at the j-th contact point, which is
f_j N_j, where f_j is a scalar and N_j is the vector normal.
This should be a scalar a_ij (which will change over time, but is
constant at this point in time, independent of the f_j's).

For example, here is how the matrix would look for Figure 26 of Baraff's
Siggraph course notes (the file is notesd2.pdf).  The figure shows
two bodies (B,C) resting on the ground, and a third body (A) resting on top
of the other two.  There are 5 points of contact between the bodies
and the ground.
a1    a11 a12 a13  0   0    f1   b1    (contacts on body B)
a2    a21 a22 a23  0   0    f2   b2    (contacts on body B)
a3 =  a31 a32 a33 a34  0  * f3 + b3    (contacts on bodies B, A)
a4     0   0  a43 a44 a45   f4   b4    (contacts on bodies A,C)
a5     0   0   0  a54 a55   f5   b5    (contacts on body C)
The 4th contact is given by the 4th row.  It is a contact between bodies A and C.
Body A has 2 contacts (f3, f4) and Body C has 2 contacts (f4, f5).  Therefore
the 4th contact is affected by movement of both bodies A and C and so
affected by those contact forces on those bodies, ie. f3, f4, f5.

Note that the f's are just the contact forces.  It is assumed that
extraneous forces have already been applied (they are captured in the b_i's).
Like gravity, spring, thrust, (probably damping too).  (Not friction, because
it changes how the reaction forces work).

We set up the b vector of external forces.  Then we set up
the A matrix of dependencies between contact forces and resulting accelerations
of different bodies.  Then we solve for the f vector of contact forces
subject to the following constraints:
  a >= 0, f>=0, a.f=0
The constraints in words are:
a >= 0:  we require all the relative normal accelerations (between bodies
at contact points) to be either zero or positive (ie. separating).
f >= 0:  We require contact forces to be zero or positive because they can
only push, not pull.
a.f = 0:  The third constraint is a math way of saying that
if there is a force, then acceleration is zero OR
if there is acceleration (separation), then there is no force.

The Baraff "Fast Contact Force..." paper goes into full detail about
how to solve this.  I've implemented that algorithm here.
The algorithm operates by setting all the forces to zero, then
adding in one force at a time, just enough to maintain the constraints,
and readjusting the other forces as necessary.

Suppose that we are working on adding in the 3rd force.  The trick is that
we only look at the constraints on the first 3 contact points, we ignore
the other contact points and other forces.  Once we've found the 3rd
force (and rebalanced forces 1 and 2) we then move on to consider the
4th force.

The last step is to apply the contact forces we've found to
the bodies, which gives the final set of accelerations that the
evaluate() method then returns to the differential equation solver.

*/
public class ContactSim extends Thruster5
{
  public static final double SMALL_NEGATIVE = -1E-10;
  public static final double SMALL_POSITIVE = 1E-10;
  private boolean debugContact = false;
  // starting configuration: 7=in corner, 2=one corner, 3=lie on floor
  // 1 = above floor; 4 = standing tall
  //protected int startConfig = 1;
  private static final String START_CONFIG = "start position";
  private Vector contactsFound = new Vector(10);

  public ContactSim(Container container) {
    super(container, false);
		// CLUDGE ALERT:  This is a bad way to set NUM_BODIES, GRAVITY, ELASTICITY
		// I need to figure out a better way to handle this later on.
    setParameter(Thruster5.NUM_BODIES, 4);
    setParameter(Thruster5.GRAVITY, 3.0);
    setParameter(Thruster5.ELASTICITY, 0.8);
  }

  protected Thruster5Object createBlock(double width, double height) {
    return new ContactObject(width, height);
  }

/*
  public void setupControls() {
    super.setupControls();
    //showControls(false);
    String[] configs = {"above floor","one corner on floor","lying on floor",
      "standing tall","left & floor","right & floor","in right corner"};
    addObserverControl(new MyChoice(this, "start position", startConfig, 1, configs));
    // this will be the last control, so figure out which parameter number it is
    //showControls(true);
  }

  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(START_CONFIG)) {
      startConfig = (int)value;
      //container.stop();  // stop animation
      m_Animating = false; // does this work?
      reset();
      m_Animating = true;
      //container.start();  // restart animation
      return true;
    }
    return super.trySetParameter(name, value);
  }

  // When overriding this method, be sure to call the super class
  //   method at the end of the procedure, to deal with other
  //   parameters and exceptions. 
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(START_CONFIG))
      return startConfig;
    return super.getParameter(name);
  }

  // When overriding this method, you need to call the super class
  //   to get its parameters, and add them on to the array. 
  public String[] getParameterNames() {
    // make an array containing superclass's parameters plus our parameters
    String[] superParams = super.getParameterNames();
    String[] myParams = new String[superParams.length + 1];
    for (int i=0; i<superParams.length; i++)
      myParams[i] = superParams[i];
    myParams[myParams.length-1] = START_CONFIG;
    return myParams;
  }
*/

  protected boolean showMomentum() { return false; }

  // Deletes all objects and recreates 'numBods' new objects.
  // Moves the new objects to a nice default starting position.
  protected void reset() {
    cvs.removeAllElements();

    m_Walls = new CRect(new DoubleRect(m_Left, m_Bottom, m_Right, m_Top));
    cvs.addElement(m_Walls);
    energyBar = new BarChart(cvs.getSimBounds());
    if (showEnergy)
      cvs.addElement(energyBar);

    bods = new Thruster5Object[numBods];
    for (int i=0; i<numBods; i++) {
      bods[i] = createBlock(1, 3);
      bods[i].tMagnitude = thrust;
      //if (i==0) bods[i].setHeight(1);
      cvs.addElement(bods[i]);
    }
		/*
    if (numBods>0) {
      switch (startConfig) {
      case 1: // above floor, parallel to floor
        //bods[0].moveTo(-2,-2,3*Math.PI/8);
        bods[0].moveTo(1, 0, 0);
        break;
      case 2:  {// one corner on floor
        double angle = Math.PI/2-Math.PI/8;
        double[] coords = bods[0].setCornerAt(0, m_Bottom+0.001, angle);
        bods[0].moveTo(coords[0], coords[1], angle);
        break;
        }
      case 3: {// lying on floor
        double w = bods[0].getWidth();
        //bods[0].moveTo(0, m_Bottom+w/2+0.001, Math.PI/2);
        bods[0].moveTo(0, -3.5+0.001, Math.PI/2);
        break;
        }
      case 4:  {// standing tall
        double h = bods[0].getHeight();
        bods[0].moveTo(0, m_Bottom+h/2+0.001, Math.PI);
        break;
        }
      case 5: {// corner on floor & another corner on left wall
        double angle = Math.PI/8;
        double[] coords = bods[0].setCornerAt(0, m_Bottom+0.001, angle);
        bods[0].moveTo(coords[0], coords[1], angle);
        // find horizontal distance from CM to left corner
        double dist = bods[0].dx - m_Left;
        double newx = bods[0].x - dist;
        bods[0].moveTo(newx+0.001, bods[0].y, angle);
        break;
        }
      case 6: {// corner on floor & another corner on right wall
        double angle = -Math.PI/8;
        double[] coords = bods[0].setCornerAt(0, m_Bottom+0.001, angle);
        bods[0].moveTo(coords[0], coords[1], angle);
        // find horizontal distance from CM to left corner
        double dist = m_Right - bods[0].cx;
        double newx = bods[0].x + dist;
        bods[0].moveTo(newx-0.001, bods[0].y, angle);
        break;
        }
      case 7: { // right corner
        double x = m_Right - bods[0].getHeight()/2 - 0.00001;
        double y = m_Bottom + bods[0].getWidth()/2 + 0.00001;
        bods[0].moveTo(x, y, Math.PI/2);
        break;
        }
      }
    }
    if (numBods>1) {
      if (startConfig==2) {
        bods[1].setHeight(6);
        bods[1].setWidth(4);
      }
      bods[1].moveTo(1.4, m_Bottom+bods[1].getWidth()/2+DISTANCE_TOL+0.1, Math.PI/2);
      //bods[1].mass = Double.POSITIVE_INFINITY;
      bods[1].color = Color.lightGray;
      if (false && startConfig==1) {
        bods[0].moveTo(1+bods[1].getCornerX(3)-0.05,
            bods[0].getHeight()/2+bods[1].getCornerY(3)+0.03, 0);
        //bods[0].moveTo(bods[0].getHeight()/2+bods[1].getCornerX(3)-0.05,
        //  bods[0].getWidth()/2+bods[1].getCornerY(3)+0.03, Math.PI/2);
      }
      if (startConfig==2) {
        double angle = Math.PI/2-Math.PI/8;
        double[] coords = bods[0].setCornerAt(0, bods[1].getCornerY(3)+0.01, angle);
        bods[0].moveTo(coords[0], coords[1], angle);

      }
    }
		*/
    if (numBods>0) {
      bods[0].moveTo(-2,-0.6,Math.PI/3);
      bods[0].color = Color.green;
    }
    if (numBods>1) {
      bods[1].moveTo(2,1,Math.PI/6);
      bods[1].color = Color.blue;
    }
    if (numBods>2) {
      bods[2].moveTo(-0.5, 1.6, 0.1);
      bods[2].color = Color.lightGray;  // so we can see the red reaction forces
    }
    if (numBods>3) {
      bods[3].moveTo(-2.2, 2.5, 0.1);
      bods[3].color = Color.cyan;
    }
    if (numBods>4) {
      bods[4].moveTo(2.4,-1.5, -0.2+Math.PI/2);
      bods[4].color = Color.magenta;
    }
    if (numBods>5) {
      bods[5].moveTo(2,3.5, 0.3+Math.PI/2);
      bods[5].color = Color.orange;
    }
    vars = new double[6*numBods];
    calc = new boolean[vars.length];
    for (int i=0; i<calc.length; i++)
      calc[i] = true;
    /*  variables:   x, x', y, y', th, th'
        bods[0]      0, 1,  2, 3,  4,  5
        bods[1]      6, 7,  8, 9, 10, 11
    */
    for (int i=0; i<numBods; i++) {  // set initial position of each body
      vars[6*i] = bods[i].x;
      vars[6*i + 2] = bods[i].y;
      vars[6*i + 4] = bods[i].angle;
      // random velocities
      
      double speed = 1;
      vars[6*i + 1] = speed*(-0.5+Math.random());
      vars[6*i + 3] = speed*(-0.5+Math.random());
      vars[6*i + 5] = speed*(-0.5+Math.random());
      
    }
    //chart = new BarChart(cvs.getSimBounds());
    //cvs.addElement(chart);
    /*
    // make a line to show where the distance tolerance is
    CVector vTol = new CVector(-2, -4.0+DISTANCE_TOL, 4.0, 0.0);
    cvs.addElement(vTol);
    // Starting conditions that are near to the ground, for testing
    // cludges related to getting objects to settle down.
    if (startConfig == 4) {
      // standing tall, bouncing
      vars[0]=0; vars[1]=0; vars[2]=-2.4;
      vars[3]=0.005551; vars[4]=0.0018693; vars[5]=0.0111646;
      //cvs.getCoordMap().zoom(-.7, .7, -4.2, -3.8);
      //chart.setBounds(-.7,-4.2,.7,-3.8);
      zeroEnergyLevel = m_Bottom + bods[0].getHeight()/2.0;
    } else if (startConfig == 1) {
      vars[0]=0; vars[1]=0; vars[2]=-3.4;
      vars[3]=0; vars[4]=1.55; vars[5]=0.0412313;
      cvs.getCoordMap().zoom(-2.1, 2.1, -4.2, -3.8);
      chart.setBounds(-2.1,-4.2,2.1,-3.8);
      zeroEnergyLevel = m_Bottom + bods[0].getWidth()/2.0;
    }
    */
		
		// For debugging reaction force calculations:
		// this sets the two bodies in the corner with one of them 
		// thrusting the other into the corner..
		// You get reaction force calculations pretty quickly in this configuration.
    if (false && numBods==2) {
      vars[0]=0.4895456 ;
      vars[1]=-0.0830102 ;
      vars[2]=-4.4960853 ;
      vars[3]=0.0203515 ;
      vars[4]=1.5703743 ;
      vars[5]=-0.1380487 ;
      vars[6]=3.4958749 ;
      vars[7]=0.0819431 ;
      vars[8]=-4.4976776 ;
      vars[9]=-0.0694257 ;
      vars[10]=1.5692822 ;
      vars[11]=-0.129418;
      bods[0].active[3] = true;
      thrust = 1.0;
    }
		
  }

  public void evaluate(double[] x, double[] change) {
		// let super-class figure out the effects of thruster, rubber-band, damping, gravity.
    super.evaluate(x, change);

		// Move objects to the resulting current location & orientation,
		// so that we can figure out the points of resting contact.
    modifyObjects(x);
    // There should be no collisions (interpenetrations) at this time,
		// because those should have been handled by the collision handling mechanism.
		// See handleCollisions and findAllCollisions.
		// Just to be sure, we check here that there are no interpenetrating collisions.
    // findAllCollisions does not look at vars[], only at object positions.
    Vector collisions = findAllCollisions();    // could skip this step?
    if (collisions != null)
      throw new CollisionException(collisions);

    findAllContacts(x);
    if (contactsFound.size() > 0) {
      /*
      if (stuckCounter > 1 && stuckCounter < 6) { // we may be stuck
        for (int i=0; i<numBods; i++)
          System.out.println("object "+i+" "+bods[i]);
        if (debugContact) System.out.println("found "+contactsFound.size()+" contacts");
        for (int i=0; i<contactsFound.size(); i++)
          System.out.println("("+i+") "+(Collision)contactsFound.elementAt(i));
      }
      */
	    // There should be no collisions (interpenetrations) at this time,
			// because those should have been handled by the collision handling mechanism.
			// See handleCollisions and findAllCollisions.
			for (int i=0; i<contactsFound.size(); i++) {
        Collision c = (Collision)contactsFound.elementAt(i);
        if (c.colliding)
					throw new IllegalStateException("unexpected collision at time="+getTime());
      }
				
      double[] b = calculate_b_vector(contactsFound, change, x);
      //printArray("b vector ",b);
      double[][] A = calculate_a_matrix(contactsFound, change, x);
      //printMatrix("A matrix",A);
      try {
        double[] f = compute_forces(A, b);
        //printArray("forces ",f);
        for (int i=0; i<contactsFound.size(); i++) {
          Collision c = (Collision)contactsFound.elementAt(i);
          applyContactForce(c, f[i], change);
        }
      } catch (IllegalStateException e) {
				//Here are some useful debugging statements showing the state of the world:
        //printArray("vars", vars);
        //for (int i=0; i<contactsFound.size(); i++)
        //  System.out.println("("+i+") "+(Collision)contactsFound.elementAt(i));
				//printArray("x ", x);
        //printArray("b vector ",b);
        //printMatrix("A matrix ",A);
        System.out.println("caught "+e);
      }
    }
  }

  private void printMatrix(String s, double[][] m) {
    System.out.println(s);
    for (int i=0; i<m.length; i++)
      printArray(("["+i+"]"), m[i]);
  }

  private void printArray(String s, double[] r) {
    nf.setMaximumFractionDigits(7);
    for (int i=0; i<r.length; i++)
      s += " ["+i+"]="+nf.format(r[i]);
    System.out.println(s);
  }

  private void addWallContact(int obj, int normalObj,
          double impactX, double impactY, int corner) {
    Collision c = new Collision();
    c.impactX = impactX;
    c.impactY = impactY;
    c.object = obj;
    c.normalObj = normalObj;
    c.corner = corner;
    c.colliding = false;
    switch (normalObj) {
      case BOTTOM_WALL:  c.normalX=0; c.normalY=1; break;
      case TOP_WALL: c.normalX=0; c.normalY=-1; break;
      case LEFT_WALL: c.normalX=1; c.normalY=0; break;
      case RIGHT_WALL: c.normalX=-1; c.normalY=0; break;
    }
    c.Rx = impactX - bods[obj].x;
    c.Ry = impactY - bods[obj].y;
    c.R2x = c.R2y = 0;  // not used for walls.
    contactsFound.addElement(c);
  }

  private void checkForContact(int obj, int corner, double[] x) {
    double cornerX, cornerY;
    switch (corner) {
      case 1: cornerX = bods[obj].ax;  cornerY = bods[obj].ay; break;
      case 2: cornerX = bods[obj].bx;  cornerY = bods[obj].by; break;
      case 3: cornerX = bods[obj].cx;  cornerY = bods[obj].cy; break;
      case 4: cornerX = bods[obj].dx;  cornerY = bods[obj].dy; break;
      default: cornerX = 0; cornerY = 0;
    }
    /* velocity of corner
      let V = velocity of center of mass;  R = distance vector CM to corner
      let w = angular velocity
      w x R = (0, 0, w) x (Rx, Ry, 0) = (-w Ry, w Rx, 0)
      velocity of corner = V + w x R = (Vx - w Ry, Vy + w Rx, 0)
    */
    // Check for contact with fixed walls.
    // is corner close & moving slow?
    if (Math.abs(cornerX - m_Left) < DISTANCE_TOL)
      // Vx - w Ry
      if (Math.abs(x[6*obj+1]-x[6*obj+5]*(cornerY-x[6*obj+2])) < VELOCITY_TOL)
        addWallContact(obj, LEFT_WALL, cornerX, cornerY, corner);

    if (Math.abs(cornerX - m_Right) < DISTANCE_TOL)
      // Vx - w Ry
      if (Math.abs(x[6*obj+1]-x[6*obj+5]*(cornerY-x[6*obj+2])) < VELOCITY_TOL)
        addWallContact(obj, RIGHT_WALL, cornerX, cornerY, corner);

      // Vy + w Rx
    if (Math.abs(cornerY - m_Bottom) < DISTANCE_TOL)
    //if (m_Bottom <= cornerY && cornerY <= m_Bottom + DISTANCE_TOL)
      if (Math.abs(x[6*obj+3] + x[6*obj+5]*(cornerX-x[6*obj])) < VELOCITY_TOL)
        addWallContact(obj, BOTTOM_WALL, cornerX, cornerY, corner);
    if (Math.abs(cornerY - m_Top) < DISTANCE_TOL)
      // Vy + w Rx
      if (Math.abs(x[6*obj+3] + x[6*obj+5]*(cornerX-x[6*obj])) < VELOCITY_TOL)
        addWallContact(obj, TOP_WALL, cornerX, cornerY, corner);
    // Check for contact with each object.
    for (int i=0; i<numBods; i++)
      if (i != obj)  // don't compare object with itself
        ((ContactObject)bods[i]).testContacts(contactsFound, cornerX, cornerY, corner, obj, i, x);
  }

  private void findAllContacts(double[] x) {
    contactsFound.removeAllElements();
    // move bodies to current positions
    for (int i=0; i<numBods; i++)  //for each body
      bods[i].moveTo(x[6*i+0], x[6*i+2], x[6*i+4]);
    // for each body, for each corner, is it close to floor?
    for (int i=0; i<numBods; i++)   //for each body
      for (int j=1; j<=4; j++)  // for each corner
        checkForContact(i, j, x);
  }

	/**  Calculates the A matrix which specifies how contact points react to contact forces.
		
		For a particular contact point i, let the bodies be numbered 1 and 2, with body 2 specifying
		the normal vector pointing out from body 2 into body 1.
		Let p1 be the point on body 1 that is in contact with the point p2 on body 2.
		Let di be the distance between p1 and p2.  Because the bodies are in resting contact,
		it should be the case that di = 0 (within numerical tolerance).  
		Resting contact also implies that the velocity of separation should be di' = 0
		(otherwise, the bodies are moving apart).
		However, the acceleration di'' is likely to be non-zero.  If di'' > 0, then the bodies
		are about to separate, and the reaction force should be zero.
		If di'' < 0, then the bodies are accelerating into each other, and a reaction force is
		needed to prevent them from interpenetrating.
		We therefore need to find reaction forces that will just barely ensure that di'' = 0.
		
		This method calculates the A matrix in the equation  a = A f + b   (see earlier notes).
		The i-j-th entry in the A matrix, Aij, specifies how the j-th contact force, fj, affects
		the acceleration of the i-th contact distance, ai = di''.
		Our goal is to find this expression for di''  (Equation D-1 of [Baraff-1]) 
		  (D-1)  di'' = Ai1 f1 + Ai2 f2 + ... + Ain fn + bi
		where bi is from the b vector which specifies external forces (gravity, thrust, etc.).
		
		A contact force fj only affects di'' if that contact force operates on body 1 or body 2.
		If that is not the case, we have the Aij = 0.  Assume now that fj affects body 1 or body 2.
		We can find Aij from the geometry of the situation as follows.
		
		Let Nj be the vector normal at the j-th contact point.  Then the contact force is fj Nj
		(note that fj is a scalar).  From the geometry we can calculate the effect of the force
		on p1'' and p2''.  (Keep in mind that the reaction force is equal and opposite for the
		two bodies.)   The effect on di'' is given by equation D-2 of [Baraff-1]:
      (D-2)   di'' = Ni.(p1'' - p2'') + 2 Ni'.(p1' - p2')
		The 2 Ni'.(p1' - p2') "is a velocity dependent term (i.e. you can immediately calculate 
		it without knowing the forces involved), and is part of bi".  See method calculate_b_vector.
		So the fj dependent part of (D-2) is just
		  Ni.(p1'' - p2'').
		
		Here is a quick derivation of equation (D-3) based on section C.3 of [Baraff-1].
		Let p(t) be the world space coordinates of a point on a rigid body.
		Let R(t) be the vector from center of mass to p(t)
		Let X(t) be the center of mass
		Let V(t) be velocity of center of mass
		Let w(t) be the angular velocity (about the CM) of the rigid body
		Then p(t) = X(t) + R(t)
		Taking derivatives:
			p'(t) = X'(t) + R'(t) = V(t) + w(t) x R(t)
		Here we use the knowledge that R(t) is only changing by rotation at a rate
		of w(t).  An elementary calculus result then gives the result that R'(t) = w(t) x R(t).
		(See derivation of Ni' in calculate_b_vector which shows the calculus steps).
		Taking another derivative:
			p''(t) = V'(t) + w'(t) x R(t) + w(t) x R'(t)
						= V'(t) + w'(t) x R(t) + w(t) x (w(t) x R(t))
		(Baraff's derivation in section C.3 is slightly more elaborate, and I don't
		entirely understand why its necessary).
		
		Two cases to consider here: whether the force is acting on body 1 or body 2.
		Suppose fj is acting on body 1.  Then fj will contribute to p1''.
		(Otherwise if fj is acting on body 2, then fj will contribute to p2''.)
		NEED: Derivation of following equation D-3.
		Equation D-3 of [Baraff-1] gives an expression for p1''
		  (D-3)  p1'' = v1' + w1' x R1 + w1 x (w1 x R1)
		where v1 = linear velocity of center of mass of body 1,
		w1 = angular velocity of body 1
		R1 = vector from center of mass to point of contact p1
		The term w1 x (w1 x R1) is velocity dependent, so it goes into the b vector.
		The fj dependent part of (D-3) is therefore
		  p1'' = v1' + w1' x R1
		
		Because v1' is the linear acceleration of body 1, we have by Newton's First Law
		  v1' = (total force on body 1)/ m1
		where m1 = mass of body 1.  Therefore the the force fj Nj contributes
		  (D-4)  fj Nj / m1
		to v1'.
		
		Next we find the term w1' x R1 in (D-3).  Equation C-10 of [Baraff-1] gives w1' as
		  (C-10)  w1' = I1^-1 (t1 + L1 x w1)
		where I1 = the rotational inertia (about what axis? CM?  is it a vector?)
		t1 = torque,  L1 = angular momentum.
		I think that I1 is a scalar quantity, so I don't know why he uses I1^-1 instead of dividing by I1.
		Additionally, the L1 vector is also perpendicular to the plane in 2D, as is w1,
		so L1 x w1 = 0.  (In 3D, this could be non-zero, but it would go into the b vector since
		it is velocity dependent).  So we are left with simply:
		  w1' = t1 / I1
		To find t1, suppose that the j-th contact occurs at the point pj, and the vector Rj goes
		from center of mass of object 1 to pj.  Then the force fj Nj contributes a torque of
		  Rj x fj Nj
		So (C-10) becomes
		  w1' = (Rj x fj Nj) / I1
		and we can write the fj dependent part of (D-3) as
		  p1'' = fj Nj / m1 + (Rj x fj Nj) x R1 / I1
		       = fj (Nj / m1 + (Rj x Nj) x R1 / I1
		and the fj dependent part of (D-2) is
		  di'' = fj Ni.(Nj / m1 + (Rj x Nj) x R1 / I1)
		Therefore from (D-1) we can see that the fj dependent part, which is Aij, is:
		  Aij = Ni.(Nj / m1 + (Rj x Nj) x R1 / I1)
		
		Next, we expand express the vector cross product (Rj x Nj) x R1, to help with writing
		the computer code.  All these vectors lie in the plane initially.
		  Rj x Nj = {0, 0, Rjx Njy - Rjy Njx}
		  (Rj x Nj) x R1 = {0, 0, Rjx Njy - Rjy Njx} x {R1x, R1y, 0}
		  = (Rjx Njy - Rjy Njx) {-R1y, R1x, 0}
		Then we can expand the dot product:
		  Aij = Nix (Njx / m1 + (Rjx Njy - Rjy Njx)(-R1y) / I1) +
		        Niy (Njy / m1 + (Rjx Njy - Rjy Njx) R1x / I1)
		Keep in mind that this is only the effect of fj on p1''.  The complete picture
		is given in equation (D-2) above, so there can be a contribution from p2'' to Aij as well.
		
		The above assumed that Nj pointed out of body 2 into body 1, that body 2 is the "normal object".  
		Actually this can vary for each contact. In the j-th contact, it could be that body 1 is the
		"normal object" and therefore Nj points out of body 1 into some other body. In that case we 
		need to use -fj Nj as the force in the above analysis.
		
		The above assumed that fj was affecting body 1.  If fj is affecting body 2, then
		the analysis is identical except the effect is on p2''.  Note that p2 has a negative
		sign in equation D-2, so we need to multiply the resulting Aij by -1.
		And of course you use m2 instead of m1, I2 instead of I1, R2 instead of R1.
		
		So there are four cases, which we list with the overall factor needed for each case.
		(The factor is determined by the sign on fj and the sign of p1 or p2 in equation D-2).
		fj affects body 1
		  body 1 is primary object in contact j -->  fj affects p1  --> +1
		  body 1 is normal object in contact j -->  -fj affects p1 --> -1
		fj affects body 2
		  body 2 is primary object in contact j -->  fj affects p2  --> -1
		  body 2 is normal object in contact j -->  -fj affects p2 --> +1
		("primary" object just means it is the non-normal object in the contact, 
		it is the object whose corner is colliding. See the fields of Collision object).
		
		Note that in the case where fj affects both bodies 1 and 2 (which happens for 
		the contact force at the contact point) then Aij is the sum of these two.
    
		References:  [Baraff-1] = David Baraff, Physically Based Modeling, Siggraph '97 Course Notes.
	*/
  private double[][] calculate_a_matrix(Vector contacts, double[] change, double[] x) {
    int nc = contacts.size();
    double[][] a = new double[nc][nc];
    for (int i=0; i<nc; i++) {
      Collision ci = (Collision)contacts.elementAt(i);
      double m1,I1,m2,I2;
      m1 = bods[ci.object].mass;
      I1 = bods[ci.object].momentAboutCM();
      m2 = (ci.normalObj>=0) ? bods[ci.normalObj].mass : Double.POSITIVE_INFINITY;
      I2 = (ci.normalObj>=0) ? bods[ci.normalObj].momentAboutCM() : Double.POSITIVE_INFINITY;
      // (D-1) di'' = ai1 f1 + ai2 f2 + ... + ain fn + bi = sum(aij fj) + bi
      for (int j=0; j<nc; j++) {
        a[i][j] = 0;
        // Find contribution of j-th contact force to the accel of gap at i-th contact
        // See equation above for Aij.
        // Note that in Collision object, R is distance vector from CM to point of impact
        // for the "primary" object in collision, while R2 is for the normal object.
        Collision cj = (Collision)contacts.elementAt(j);
        if (ci.object == cj.object) {
          // body 1 is primary object in j-th contact
          // fj affects p1 in eqn (D-2), so use m1, I1
          a[i][j] += ci.normalX*(cj.normalX/m1 + (-ci.Ry*cj.Rx*cj.normalY + ci.Ry*cj.Ry*cj.normalX)/I1);
          a[i][j] += ci.normalY*(cj.normalY/m1 + (-ci.Rx*cj.Ry*cj.normalX + ci.Rx*cj.Rx*cj.normalY)/I1);
        }
        if (ci.object == cj.normalObj) {
          // body 1 is normal object in j-th contact
          // -fj affects p1, and use cj.R2 for calculating torque
          a[i][j] -= ci.normalX*(cj.normalX/m1 + (-ci.Ry*cj.R2x*cj.normalY + ci.Ry*cj.R2y*cj.normalX)/I1);
          a[i][j] -= ci.normalY*(cj.normalY/m1 + (-ci.Rx*cj.R2y*cj.normalX + ci.Rx*cj.R2x*cj.normalY)/I1);
        }
        if (ci.normalObj>=0 && ci.normalObj == cj.object) {
          // body 2 is primary object in j-th contact
          // fj affects p2; use m2, I2, ci.R2, and cj.R
          a[i][j] -= ci.normalX*(cj.normalX/m2 + (-ci.R2y*cj.Rx*cj.normalY + ci.R2y*cj.Ry*cj.normalX)/I2);
          a[i][j] -= ci.normalY*(cj.normalY/m2 + (-ci.R2x*cj.Ry*cj.normalX + ci.R2x*cj.Rx*cj.normalY)/I2);
        }
        if (ci.normalObj>=0 && ci.normalObj == cj.normalObj) {
          // body 2 is normal object in j-th contact
          // -fj affects p2 (double negative);  use m2, I2, ci.R2 and cj.R2
          a[i][j] += ci.normalX*(cj.normalX/m2 + (-ci.R2y*cj.R2x*cj.normalY + ci.R2y*cj.R2y*cj.normalX)/I2);
          a[i][j] += ci.normalY*(cj.normalY/m2 + (-ci.R2x*cj.R2y*cj.normalX + ci.R2x*cj.R2x*cj.normalY)/I2);
        }
      }
    }
    return a;
  }

  /**  Calculates the b vector which specifies how external forces (like gravity, thrust, etc)
    affect acceleration of contact points.
		
		Background:
		In calculate_a_matrix, we found for di'' the parts that were dependent on the contact forces fi.
		Here we find the part of di'' that is independent of the fi's, such as gravity, thrust,
		rubber band force, and damping.  For the purposes of calculating the contact forces
		we regard these forces as "constant" (at this moment in time) in the matrix equation
		a = A f + b.  These "constant" or "external" forces are put into the b vector, so that
		we can then solve the matrix equation a = A f + b, subject to the constraints that
		f >= 0, a >= 0, and f.a = 0.  (Those constraints say that reaction forces can only push
		things apart, that objects cannot interpenetrate, and that if objects are separating then
		there is no reaction force).
		
		Derivation:
		We start with the expression for  di'' = the acceleration of the distance between
		the contact points p1, p2.  This was derived above as:
		  (D-2)   di'' = Ni.(p1'' - p2'') + 2 Ni'.(p1' - p2')
		
		The term Ni.(p1'' - p2'') involves accelerations and therefore forces, so we need
		to include the effect of any forces other than the reaction forces on the accelerations
		of the contact points p1 and p2.  We will examine each of those forces (such as gravity)
		and determine through the geometry of the situation how the force affects the acceleration
		of each contact point.
		
		The term 2 Ni'.(p1' - p2') is dependent only on current velocity, not acceleration,
		and therefore is independent of any forces being applied, and therefore belongs
		in the b vector.
		
		Derivation of Ni'
		  Ni = {Nix, Niy, 0}
		The vector Ni is rotating at a rate w.  If we ignore any acceleration, we could
		write the vector Ni as a function of time like this:
		  Ni = |Ni| {cos wt, sin wt, 0}
		Where |Ni| is the magnitude of the vector Ni, w = angular velocity, and t = time.
		For some particular value of time t, this will be equal to {Nix, Niy, 0}.
		Now elementary calculus gives us the derivative:
		  Ni' = |Ni| {-w sin wt, w cos wt, 0}
		And we can recognize that this is equivalent to:
		  Ni' = w {-Niy, Nix, 0}
		Because we picked the value of t such that Nix = |Ni| cos wt, and Niy = |Ni| sin wt.
		Note that this can also be expressed as a cross product of two vectors:
		  Ni' = W x Ni
		where we treat angular velocity as a vector W with components {0, 0, w}.
		
		Derivation of p1'
		This is the velocity of a particular point, p1, on the object.  The object is
		translating and rotating.  The translation is given by the velocity of the 
		center of mass, V = {Vx, Vy, 0}, and the rotation is given by the angular velocity w.
		Let R be the vector from the center of mass to the point.  
		Let X be the vector from the origin to the center of mass of the object.  
		Let V be the velocity of the center of mass, so that V = X'.
		Let W be the angular velocity of the object, in 2D we have W = {0, 0, w}.
		Then the point p1 is given by
		  p1 = X + R
		Taking derivatives, we get
		  p1' = X' + R' = V + W x R
		where we used the vector cross product method of finding R' (see above derivation
		of Ni').  This then expands to
		  p1' = {Vx, Vy, 0} + w {-Ry, Rx, 0}
		  p1' = {Vx - w Ry, Vy + w Rx, 0}
		
		Expansion of 2 Ni'.(p1' - p2')
		To bring together the entire expression 2 Ni'.(p1' - p2') we need to recognize
		that each body has its own angular velocity w.  The w used in calculating Ni'
		is that of the "normal" object, which is always body 2 in our scheme, or w2.
		Here is the completely expansion using subscripts 1 and 2 to refer to body 1 or 2
		of the contact.
		  2 Ni'.(p1' - p2')
		  = 2 w2 {-Niy, Nix, 0} . ({V1x - w1 R1y, V1y + w1 R1x, 0} - {V2x - w2 R2y, V2y + w2 R2x, 0})
		  = 2 w2 {-Niy (V1x - w1 R1y - V2x + w2 R2y),  Nix (V1y + w1 R1x - V2y - w2 R2x),  0}
		
		External forces in Ni.(p1'' - p2'')
		Next we examine the "external" forces (all forces other than the contact forces
		we are trying to solve for) and determine their contribution to the acceleration
		of the separation of the contact points.  As explained above, these show up in
		the term Ni.(p1'' - p2'').  
		Equation D-3 of [Baraff-1] gives an expression for p1''
		  (D-3)  p1'' = v1' + w1' x R1 + w1 x (w1 x R1)
		(this magic needs to be elucidated)
		where v1' = acceleration of center of mass
		w1 = angular velocity
		w1' = angular acceleration (= torque? I think not!)
		R1 = vector from CM to contact point p1
		The corresponding expression for p2'' is
		  p2'' = v2' + w2' x R2 + w2 x (w2 x R2)
		
		It turns out that the external forces have already been calculated for us by
		the earlier processes (see evaluate method of Thruster5 class).  In the change
		variable we get passed in the current values for x'', y'', th''.
		These accelerations are the result of all the external forces such as gravity,
		thrust, rubber bands, damping.  They operate on each object regardless of the
		contact forces that are applied.  So we only have to plug these in to
		the above equations.
			Ni.(p1'' - p2'') =
				Ni.( (v1' + w1' x R1 + w1 x (w1 x R1)) - (v2' + w2' x R2 + w2 x (w2 x R2)))
		We can expand the above as follows.  (Keep in mind that we regard w' and w
		as vectors perpendicular to the plane for purposes of vector cross products.)
			w' x R = (-w' Ry, w' Rx, 0)
			w x (w x R) = (-w^2 Rx, -w^2 Ry, 0)
			V' + w' x R + w x (w x R) = (Vx -w^2 Rx - w' Ry, Vy - w^2 Ry + w' Rx, 0)
		
		Conclusion
		We now have the expansions of the two terms in equation (D-2).
		This gives for each contact i, the contribution to the acceleration 
		which we add to b[i].  Keep in mind that for each contact, the Collision
		object figures out for us:  which is body 1 (the "primary" object whose corner is
		in contact), which is body 2 (the "normal" object whose edge determines the normal
		vector), and the normal vector Ni.
  */
  private double[] calculate_b_vector(Vector contacts, double[] change, double[] x) {
    double[] b = new double[contacts.size()];
    for (int i=0; i<contacts.size(); i++) {
      b[i] = 0;
      Collision c = (Collision)contacts.elementAt(i);
      double w = x[c.object*6+5];
      // Only if normalObj is spinning... walls cannot spin.
      if (c.normalObj>=0) {
        /*  The velocity of a corner is given by
           p' = V + w x R = (Vx, Vy, 0) + w (-Ry, Rx, 0)
           = (Vx - w Ry, Vy + w Rx, 0)
          (16)  2 N'.(p1' - p2') = 2 w2(-Ny, Nx, 0).(p1' - p2')
        */
        double w2 = x[c.normalObj*6+5];
        double v1x = x[c.object*6+1] - w*c.Ry;
        double v1y = x[c.object*6+3] + w*c.Rx;
        double v2x = x[c.normalObj*6+1] - w2*c.R2y;
        double v2y = x[c.normalObj*6+3] + w2*c.R2x;
        b[i] += 2*w2*(-c.normalY*(v1x - v2x)+c.normalX*(v1y-v2y));
      }
      /*
        variables:   x, x', y, y', th, th'
        bods[0]      0, 1,  2, 3,  4,  5
        bods[1]      6, 7,  8, 9, 10, 11
        Next look for force independent parts of the first term in (D-2).
        They involve the external (what I call "initial") linear acceleration
        and torque, Ai and ai.  Plus the w x (w x R) term.
           Ai + ai x R + w x (w x R)
        So take this for each point to get
        (17) N.( (Ai1 + ai1 x R1 + w1 x (w1 x R1)) - (Ai2 + ai2 x R2 + w2 x (w2 x R2)))
        And b is then the sum of (16) and (17)
        a x R = (-a Ry, a Rx, 0)
        w x (w x R) = (-w^2 Rx, -w^2 Ry, 0)
        A + w x (w x R) + a x R = (Ax -w^2 Rx - a Ry, Ay - w^2 Ry + a Rx, 0)
      */
      b[i] += c.normalX*(change[c.object*6+1] - w*w*c.Rx - change[c.object*6+5]*c.Ry);
      b[i] += c.normalY*(change[c.object*6+3] - w*w*c.Ry + change[c.object*6+5]*c.Rx);
      if (c.normalObj >= 0) {
        double w2 = x[c.normalObj*6+5];
        b[i] -= c.normalX*(change[c.normalObj*6+1] - w2*w2*c.R2x - change[c.normalObj*6+5]*c.R2y);
        b[i] -= c.normalY*(change[c.normalObj*6+3] - w2*w2*c.R2y + change[c.normalObj*6+5]*c.R2x);
      }
    }
    return b;
  }


	/*
		Regarding the mechanics of passing around arrays, here is something from
		"Core Java Vol I Fundamentals" by Horstmann & Cornell (page 89):
		"All arguments to methods in Java are passed by value and not by reference.  It is,
		therefore, impossible to change variables by means of method calls.
		On the other hand... since arrays and objects in Java are actually references (pointers),
		methods can modify the contents of arrays or the state of an object.  They just can't
		modify the actual parameters."
	*/
  private double[] compute_forces(double[][] A, double[] b) {
    int n = b.length;
    double[] f = new double[n];  // force at each contact point
    double[] a = new double[n]; // acceleration at each point.  a > 0 means separation.
    boolean[] C = new boolean[n];  // C = Contact, points that remain in contact with non-zero contact force.
    boolean[] NC = new boolean[n];  // NC = Non-Contact, points that are separating, so contact force is zero.
    for (int i=0; i<n; i++) {
      f[i] = 0;
      a[i] = b[i];
      C[i] = NC[i] = false;
    }
		// The algorithm actually calls for "while there exists d such that a[d] < 0" here.
		// But it seems to me that drive_to_zero should always succeed in setting a[d] >= 0
		// and it will preserve that condition for all the other contacts already
		// treated.  Therefore, this "for" loop should be equivalent.
    for (int d=0; d<n; d++) 
			drive_to_zero(d, f, a, C, NC, A);
    return f;
  }
	
	/**
	The function drive-to-zero increases f[d] until a[d] is zero. The 
	direction of change for the force, f, is computed by fdirection. 
	The function maxstep determines the maximum step size s, and the 
	constraint j responsible for limiting s. If j is in C or NC, j is moved 
	from one to the other; otherwise, j = d, meaning a[d] has been driven 
	to zero, and drive-to-zero returns
	*/
	private void drive_to_zero(int d, double[] f, double[] a, boolean[] C, boolean[] NC,
		double[][] A) {
    int n = f.length;
    double[] delta_a = new double[n];
    double[] delta_f = new double[n];
    int loopCtr = 0; // to detect infinite loops
    if (a[d] >= 0)  // if separating, put this into NC set, and we are done.  CORRECT???
      NC[d] = true;
    while (a[d] < 0) {
      if (++loopCtr > 200) {
        throw new IllegalStateException("drive_to_zero() loopCtr="+loopCtr
         +" d="+d+" a[d]="+a[d]);
      }
      fdirection(delta_f, d, A, C);
      // matrix multiply:  delta_a = A delta_f
      for (int i=0; i<n; i++) {
        delta_a[i] = 0;
        for (int j=0; j<n; j++)
          delta_a[i] += A[i][j]*delta_f[j];
				// cludge to fix floating point math problem: 
				// we set extremely small negative numbers to be exactly zero.
				if (delta_a[i] < 0 && SMALL_NEGATIVE < delta_a[i])
					delta_a[i] = 0;
        // double-check that delta_a[i] = 0 for members of C
        if (C[i] && Math.abs(delta_a[i])>SMALL_POSITIVE)
          throw new IllegalStateException(
					"fdirection failed, should be zero:  delta_a["+i+"]="+delta_a[i]);
      }
			double[] stepSize = new double[1];
      int j = maxStep(f, a, delta_f, delta_a, d, C, NC, stepSize);  // also writes into stepSize class variable
      for (int i=0; i<n; i++) {
        f[i] += stepSize[0]*delta_f[i];
        a[i] += stepSize[0]*delta_a[i];
				// cludge to fix floating point math problem:
				// we set extremely small negative numbers to be exactly zero.
				if (f[i] < 0 && SMALL_NEGATIVE < f[i])
					f[i] = 0;
				if (a[i] < 0 && SMALL_NEGATIVE < a[i])
					a[i] = 0;
				if ((NC[i] || C[i]) && a[i] < 0)
					throw new IllegalStateException(
						"acceleration cannot be negative,  a["+i+"]="+a[i]);
        if (f[i] < 0)
          throw new IllegalStateException(
					"reaction force cannot be negative,  f["+i+"]="+f[i]);
      }
			// If j is in C or NC, j is moved from one to the other
      if (C[j]) {
        C[j] = false;
        NC[j] = true;
      } else if (NC[j]) {
        NC[j] = false;
        C[j] = true;
      } else {
        // j must be d, implying a[d] = 0
        C[j] = true;
        break;
      }
    }
	}

  /** fdirection computes delta_f resulting from a change of 1 in delta_f[d].
    We have a unit increase in the d-th force, so delta_f[d] = 1.
    The forces in NC remain zero, so delta_f[i] = 0, for i an element of NC.
    For i an element of C, we adjust those forces to maintain a[i] = 0.
    Essentially, we balance out the increase of the d-th force by adjusting
    all the C forces (this involves a matrix equation solve).
  */
  private void fdirection(double[] delta_f, int d, double[][] A, boolean[] C) {
    int n = C.length;
    for (int i=0; i<n; i++)
      delta_f[i] = 0;
    delta_f[d] = 1;
    int c = 0;  // number of elements in set C
    for (int i=0; i<n; i++)
      if (C[i]) c++;
    if (c>0) {
			// Acc is an augmented matrix: the last column is for vector v1
      double[][] Acc = new double[c][c+1];  
      int p = 0;
      for (int i=0; i<n; i++)
        if (C[i]) {
          int q = 0;
          for (int j=0; j<n; j++)
            if (C[j]) {
              // Acc is the submatrix of A obtained by deleting the j-th row and 
							// column of A for all j not in C
              Acc[p][q] = A[i][j];
              q++;
            }
          // The last column of Acc is where we put the vector v1 of the algorithm.
					// This is where the matrixSolve algorithm expects to find it.
					// v1 is the d-th column of A, but has only elements in C.
          Acc[p][c] = -A[i][d];  
          p++;
        }
      double[] x = new double[c];
			// note that we put v1 into the last column of Acc earlier
      Utility.matrixSolve(Acc, x);  // solves Acc x = v1
      // transfer x into delta_f
      p = 0;
      for (int i=0; i<n; i++)
        if (C[i])
          delta_f[i] = x[p++];
    }
  }

  /**  maxStep finds the biggest step of force increase we can take,
		while maintaining the constraint that acceleration >= 0 and force >=0
		and a dot f = 0 for all contact points treated so far.  
    It returns the stepSize and the index j of the force that limited the step.
  */
  private int maxStep(double[] f, double[] a, double[] delta_f, double[] delta_a, int d,
    boolean[] C, boolean[] NC, double[] stepSize) {
    double s = Double.POSITIVE_INFINITY;
    int j = -1;
    int n = f.length;
    //  d is the contact whose (currently negative) acceleration we are trying to drive to zero.
    //  d is neither in C nor NC.
    //  Figure the stepsize that would drive the acceleration to zero at contact d.
    if (delta_a[d] > 0) {
      j = d;
      s = -a[d]/delta_a[d];
    }
    //  If i element of C, we can reduce the force there, but only to zero.
    //  Then i will move over to NC.
    for (int i=0; i<n; i++)
      if (C[i] && delta_f[i] < 0) {
        double sPrime = -f[i]/delta_f[i];
        if (sPrime<s) {
          s = sPrime;
          j = i;
        }
      }
    //  If i element of NC, we can decrease the acceleration there, but only to zero.
    //   Then i will move over to C.
    for (int i=0; i<n; i++)
      if (NC[i] && delta_a[i] < 0) {
        double sPrime = -a[i]/delta_a[i];
        if (sPrime < s) {
          s = sPrime;
          j = i;
        }
      }
		if (s<0) 
			throw new IllegalStateException("maxStep negative.  d="+d+" s="+s);
    stepSize[0] = s;
    return j;
  }

  private void applyContactForce(Collision c, double f, double[] change) {
    if (f==0)
      return;
    //CRect v = new CRect(c.impactX, c.impactY, c.impactX+0.05+c.normalX*f, c.impactY+0.05+c.normalY*f);
    //v.m_DrawMode = CElement.MODE_FILLED;
    CVector v = new CVector(c.impactX, c.impactY, c.normalX*f, c.normalY*f);
    v.m_Color = Color.red;
    cvs.addElement(v);
    rxnForces.addElement(v);
    //if (debugContact) System.out.println("contact force "+f+"at x="+c.impactX+" y="+c.impactY);
    int obj = c.object;
    double invMass = bods[obj].invMass();
    double invMoment = bods[obj].invMomentAboutCM();
    // try adding friction force
    /*
    if (c.normalObj==BOTTOM_WALL) {
      c.rxnForceY += -x[6*obj+1]*c.rxnForceY*0.3;
    }*/
    /*   apply reaction force to change[] array
    (3)  Ax = Axi + F1/m
    (4)  Ay = Ayi + F2/m
    (5)  a = ai + (R1 x F1)/I + (R2 x F2)/I
         a = ai + (-Ry1 F1x + Rx2 F2y)/I          */
    change[6*obj+1] += c.normalX*f*invMass;
    change[6*obj+3] += c.normalY*f*invMass;
    change[6*obj+5] += (-c.Ry*c.normalX*f + c.Rx*c.normalY*f)*invMoment;

    // apply negative force to the normalObj too.
    obj = c.normalObj;
    if (obj>=0) {
      invMass = bods[obj].invMass();
      invMoment = bods[obj].invMomentAboutCM();
      change[6*obj+1] -= c.normalX*f*invMass;
      change[6*obj+3] -= c.normalY*f*invMass;
      change[6*obj+5] -= (-c.R2y*c.normalX*f + c.R2x*c.normalY*f)*invMoment;
    }
  }

}

/////////////////////////////////////////////////////////////////////////////////

class ContactObject extends Thruster5Object {

  public ContactObject(double width, double height) {
    super(width, height);
  }

  private boolean testVelocity(Vector contacts, Collision c, int edge, double[] x) {
    //  normal relative velocity = n.vab = n.(va + wa x ra - vb - wb x rb)
    // cross product: w x r = (0,0,w) x (rx, ry, 0) = (-w*ry, w*rx, 0)
    // find normal for this edge
    getNormalForEdge(c, edge);
    int obj = 6*c.object;
    int nobj = 6*c.normalObj;
    boolean debugthis = (false && c.normalObj==0 && c.corner==3);
    double nrv = c.normalX * (x[VX+obj] + x[VW+obj]*(-c.Ry) - x[VX+nobj] - x[VW+nobj]*(-c.R2y));
    nrv += c.normalY * (x[VY+obj] + x[VW+obj]*c.Rx - x[VY+nobj] - x[VW+nobj]*c.R2x);
    if (Math.abs(nrv) < Thruster5.VELOCITY_TOL) {
      Collision.addCollision(contacts, c);
      if (debugthis) {
        System.out.println("found contact on edge "+edge+" nrv="+nrv);
        System.out.println("  contact info: "+c);
      }
      return true;
    } else {
      if (debugthis)
        System.out.println("velocity too big, nrv="+nrv);
      return false;
    }
  }

  public void testContacts(Vector contacts, double cornerX, double cornerY, int corner,
        int objIndex, int selfIndex, double[] x) {
    // Given the point (cornerX, cornerY) which is a corner of the other object,
    // determine if that corner is in contact with this object.
    // If so, add a Collision to the contacts list.
    // First do a rough distance check.
    double dx = cornerX - this.x;
    double dy = cornerY - this.y;
    double d = dx*dx + dy*dy;
    dx = this.width/2 + Thruster5.DISTANCE_TOL;
    dy = this.height/2 + Thruster5.DISTANCE_TOL;
    if (d > dx*dx + dy*dy)
      return;  // corner is too far away to be in contact
    //System.out.println("distance squared="+d+" cornerX="+cornerX+" cornerY="+cornerY);
    //System.out.println("this.x="+this.x+" this.y="+this.y+" object radius="+(dx*dx+dy*dy));
    // Move corner to body coordinate system.
    double gx = cornerX, gy = cornerY;
    gx -= this.x;   // set center of mass as origin, and unrotate
    gy -= this.y;
    double px = gx*Math.cos(-this.angle) - gy*Math.sin(-this.angle);
    double py = gx*Math.sin(-this.angle) + gy*Math.cos(-this.angle);
    px += this.cmx; // translate to body coordinates
    py += this.cmy;
    // accept the first contact we find
    int obj = 6*objIndex;
    int nobj = 6*selfIndex;
    Collision c = new Collision();
    c.colliding = false;  // resting contact
    c.Rx = cornerX - x[X+obj];
    c.Ry = cornerY - x[Y+obj];
    c.R2x = cornerX - x[X+nobj];
    c.R2y = cornerY - x[Y+nobj];
    c.impactX = cornerX;
    c.impactY = cornerY;
    c.normalObj = selfIndex;
    c.object = objIndex;
    c.corner = corner;
    if (false && selfIndex==0 && corner==3) {
      System.out.println("px= "+px+" py="+py+" cornerX="+cornerX+" cornerY="+cornerY);
    }
    double dtol = Thruster5.DISTANCE_TOL;
    if (0 <= py && py <= this.height) {
      // colliding contact
      if (0 <= px && px <= this.width) {
        c.colliding = true;
        contacts.addElement(c);
        return;
      }
      // left edge
      if (-dtol <= px && px <= 0) {
        c.depth = -px;
        if (testVelocity(contacts, c, LEFT, x))
          return;
      }
      // right edge
      if (width <= px && px <= width+dtol) {
        c.depth = px - width;
        if (testVelocity(contacts, c, RIGHT, x))
          return;
      }
    }
    if (0 <= px && px <= this.width) {
      // colliding contact
      if (0 <= py && py <= this.height) {
        c.colliding = true;
        contacts.addElement(c);
        return;
      }
      // bottom edge
      if (-dtol <= py && py <= 0) {
        c.depth = -py;
        if (testVelocity(contacts, c, BOTTOM, x))
          return;
      }
      // top edge
      if (height <= py && py <= height+dtol) {
        c.depth = py - height;
        if (testVelocity(contacts, c, TOP, x))
          return;
      }
    }
  }

}
