/*
  File: String1.java

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
o  String
  Gershenfeld, Nature of Mathematical Modeling, gives a way
  of discretizing the PDE for a string.
  I'm imagining a string fixed at each end, and you can
  choose initial conditions somehow (eg. drag at a single point)
  and then run the simulation.
  Maybe have an oscillating driver, like a cello bow, that
  applies a force at a certain point repetitively.
  # find equations (ie. need tension & mass factors...)
  # new element that draws string corresponding to the vector.
    # create the u() vector (maybe 1000 long)
    # have element sample it at around 100 places for drawing
  # Test by evolving according to some simple formula.
    eg. u(x,t) = sin(2*pi*x/length)*sin(t)
  # 4 large vectors:
    old slope, old velocity, new slope, new velocity
    (note: can save time copying over by having a flag
    that says which is the more recent vector).
  # can just set initial conditions by code at first
    The velocity vector can be all zero.
    Fill in the slope vector by deciding on a function,
    and calc its first derivative.
  # solver applies eqns to calculate new from old.
  # integration of slope vector fills in the u vector.
    This can be done in doModifyObjects.

  # stability condition is: abs(v) delta(t) / delta(x) < 1
      Let's check what is the delta(t) that we are actually
    getting in this and other simulations.
    If its too big, then we need to go to non-real time, or
    find ways to speed things up!
    Note that we are very limited by delta(t)... can't choose
    delta(x) to be very small without also decreasing delta(t).
    We can artifically slow down time by having a scaling
    factor on time. Make this a variable that is controllable,
    and then we can see effect when its non-stable.
  # Show the stability as a (non-modifiable) number in control window.
    (or in message window as a quick cludge).

   # note:  had to fix boundary conditions (had wrong assumptions,
    they are not both zero, only the velocity is zero).
  # The problem with (nearly) square waves:
    Seems to get 2 different solutions that oscillate.
    This is an interesting artifact of the numerical method.
    Really there are two independent simulations going on,
    one involves even numbered slots (indices) in the vectors
    and the other involving odd numbered slots.
    Let r[t,x] be the r (slope) vector at time t, position x.
    and similarly for the s (speed) vector.
    Algorithm is then
        r[t+1,j] = (1/2)(r[t,j+1] + r[t,j-1]) + k(s[t,j+1] - s[t,j-1])
    and similarly for s, where k is a constant.
     Therefore, the even numbered slots depend only on the odd numbered
    slots in the last time period, and vice versa.
    Therefore, if the initial conditions are such that r[0,j] is
    very different from r[0,j+1], then you wind up with two
    different simulations for the odd and even slots.
    In the square wave case, I had all of r & s zero except for
    two points in r:  r[2*n/5] and r[3*n/5].
    Therefore, "half" of the simulation saw entirely zero vectors
    and the other half saw those two points.  The reason that
    the resulting string oscillated is that the integration
    algorithm adds even & odd spots at different strengths (2:1)
    and since the non-zero simulation switches between even and
    odd slots every iteration, this caused the oscillation.
    Is there perhaps a way to connect the two separate even/odd
    simulation and keep things together?
  # Try staggered leapfrog method
    This method didn't work either....
  # What works:
    Algorithm 12.4, "Wave Equation Finite-Difference" from
    Numerical Analysis, 6th Ed. by Burden & Faires.
    This definitely works where the others I tried (from Numerical Recipes)
    were failing.  Actually the Lax method sort of seemed to work, but
    had a lot of numerical dispersion (smoothing over time).
  # manual forcing
    curve element should take up less than full screen
        (maybe draw a boundary around it?)
    set a block at one end and allow it to be moved up & down
    set the endpoint of the string to be position of block.
    GOT IT!  It works!
  # select starting waveform from a menu
    Need to add a new menu that is different for each simulation.
    Is it a single menu which changes items, or different menus?
  # compare to ANALYTIC solution
    Plot both, with different colors.
  o Reset position of block when restarting (ie. option menu select).
  o Change delta(t) and delta(x) as parameters?
    Need to show stability somewhere....
  o specify amplitude of first 10 harmonics, and their phases?
   o Reset button
  o repetitive FORCING:
    add parameters for amplitude & frequency of forcing,
    show how this leads to "ringing" when frequency is same
    as natural frequency of the string.  Might need damping to get this.
  o PULSE
    Provide a single (half-cycle) of forcing, to create
    a smooth pulse.  Can we solve this analytically?
    This one seems to build up trailing artifacts over time... why?
  o Show FREQUENCY ANALYSIS of the string
    Possible to show frequency breakdown of the string?
    Maybe could see loss of high-frequency components over time?
    Is this what causes the build-up of wobbles in a wave
    over time (and loss of shape of a square wave).
  o Try using smaller time deltas (this will slow down the simulation)
    and see if the artifacts are reduced.
  o Ability to draw the starting waveform, or
  o Need to understand how the Burden & Faires method is derived.
    Work it out in Mathematica...  then can maybe apply similar
    results to adding forcing or damping or gravity, or a weight
    at certain points, etc.
  o add in gravity or damping or forcing, see Tung p. 29
    To do this, need to re-derive the recursion relationships from
    the new PDE.
  o Numerical considerations are mainly about how to represent
    the derivatives numerically in the recursion, and stability.
  o SPEED
    how to make the whole thing go faster?
    Need to increase tension (decrease density), but must keep
    stability below 1.
    Therefore, have to decrease delta(t).  But we are only
    getting about 20 frames per second.   Where is all the time
    going?
    Check in simpler simulations, what is maximum achievable?
    Try profiling.
    Use full-screen mode to get more time.
    (Here are settings I was playing with for faster string:
      STRING_POINTS = 21;  m_Tension = 30;   m_Density = 1;  gravity = 9;
      delta_t = 0.03;

*/

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;

class ShapeName {
  private final String name;
  private ShapeName(String name) { this.name = name; }
  public String toString() { return name; }
  public static final ShapeName FLAT = new ShapeName("flat");
  public static final ShapeName TRIANGLE = new ShapeName("triangle");
  public static final ShapeName QUARTER_TRI = new ShapeName("quarter tri");
  public static final ShapeName SQUARE_PULSE = new ShapeName("square pulse");
  public static final ShapeName SINE_PULSE = new ShapeName("sine pulse");
  public static final ShapeName HALF_SINE_PULSE = new ShapeName("half sine pulse");
  public static final ShapeName FANCY_SINE = new ShapeName("fancy sine");

  public static ShapeName[] getShapeNames() {
    ShapeName[] p = new ShapeName[7];
    p[0] = ShapeName.FLAT;
    p[1] = ShapeName.TRIANGLE;
    p[2] = ShapeName.QUARTER_TRI;
    p[3] = ShapeName.SQUARE_PULSE;
    p[4] = ShapeName.SINE_PULSE;
    p[5] = ShapeName.HALF_SINE_PULSE;
    p[6] = ShapeName.FANCY_SINE;
    return p;
  }
}

/////////////////////////////////////////////////////////////////////////////////
public class String1 extends Simulation implements ObjectListener {
  public static final int FLAT = 0;
  public static final int TRIANGLE = 1;
  public static final int QUARTER_TRI = 2;
  public static final int SQUARE_PULSE = 3;
  public static final int SINE_PULSE = 4;
  public static final int HALF_SINE_PULSE = 5;
  public static final int FANCY_SINE = 6;

  private CCurve m_Curve;
  private CCurve m_Curve2;  // analytic version
  private CMass m_Block;
  private double m_Length;   // length of string;
  private double m_Tension;  // tension of string
  private double m_Density;  // density of string per unit length
  private double m_Density2;  // density on right half of string
  private double m_damping;
  private double gravity;
  private int m_shape;  // starting wave shape

  private double[] w1;  // data array
  private double[] w2;  // data array
  private double[] w3;  // data array
  private double[] w4;  // analytic data array
  private int w_idx;  // tells which array (1 or 2 or 3) is most recent
  private double delta_x;  // delta x
  private double delta_t;  // delta time (fixed)
  private double total_t;  // cumulative time;
  private double bead_y; // position of bead
  private double bead_v; // velocity of bead
  private double bead_mass;

  private static final int AVG_LEN = 10;
  private double[] times;  // for checking on delta(t) = avg time between updates
  private double[] stab;   // for averaging stability value
  private int time_idx;   // index into times[] array
  private double last_time;  // last time we printed delta(t)
  private double now_time;  // current real time
  private static final String SHAPE = "shape", TENSION = "tension",
      GRAVITY = "gravity", DENSITY = "density";
  //protected MyChoice shapeControl;
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {TENSION, GRAVITY, DENSITY};
  private static final int STRING_POINTS = 501;

  public String1(Container app)  {
    super(app);
    CoordMap map = new CoordMap(CoordMap.INCREASE_UP, 0, 15,
        -.5, .5, CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE);
    setCoordMap(map);
    map.setFillScreen(true);
    map.setRange(0, 15, -0.25, 0.25);
    DoubleRect simRect = map.getSimBounds();

    var_names = new String[] {};  //empty string

    m_shape = FLAT;

    // initialize stuff for figuring average time delta
    times = new double[AVG_LEN];  // automatically initialized to zero
    stab = new double[AVG_LEN];
    last_time = -100;
    now_time = 0;
    time_idx = 0;

    bead_y = bead_v = 0;
    bead_mass = 1;
    // determine window size in simulation coords,
    // to make the curve fill the window
    m_Length = 0.9*simRect.getWidth();
    m_Tension = 2;
    m_Density = 1;
    m_Density2 = 4;
    m_damping = 2.0;
    gravity = 1;
    delta_x = m_Length/(STRING_POINTS-1);  // delta x
    System.out.println("string length= "+m_Length + "  delta_x= "+delta_x);
    //  delta_t = 0.055;  // delta time is fixed!!!

    delta_t = 0.005;  // was 0.03
    System.out.println("delta_t = "+delta_t+" must be < "+(delta_x*Math.sqrt(m_Density2/m_Tension)));
    total_t = 0;
    initializeShape();

    double bw = 0.7; // width of block
    double bh = 0.05; // height of block
    double h = simRect.getHeight()/2;

    //X1, Y1, width, height
    m_Curve = new CCurve(simRect.getXMin()+bw+0.2, -h, m_Length, 2*h, STRING_POINTS);
    cvs.addElement(m_Curve);

    // second curve is to show analytic version
    m_Curve2 = new CCurve(simRect.getXMin()+2*bw, -h, m_Length, 2*h, STRING_POINTS);
    m_Curve2.m_Data = w4;
    m_Curve2.m_Color = Color.red;

    // This mass is for user to drag up & down for manual forcing
    m_Block = new CMass(m_Curve.m_X1 - bw, -bh/2, bw, bh, CElement.MODE_RECT);
    cvs.addElement(m_Block);

    modifyObjects();

    params = new String[] {"gravity","tension","density"};

    modifyObjects();
    cvs.setObjectListener(this);
  }

  public void setupControls() {
    super.setupControls();
    // create popup menu for paths
    // listener, name, value, minimum, choice strings
    /*addObserverControl(shapeControl =
        new MyChoice(this, SHAPE, shape, 0, PathName.getPathNames()));
      */
    // DoubleField params:  subject, name, fraction digits
    //addObserverControl(new DoubleField(this, SHAPE, 0));
    addObserverControl(new DoubleField(this, GRAVITY, 2));
    addObserverControl(new DoubleField(this, TENSION, 2));
    addObserverControl(new DoubleField(this, DENSITY, 2));
    showControls(true);
  }

  public void setupGraph() {
    /* override to do nothing because no graph here. */
  }

  /* set initial conditions for string.
     input is width of the string */
  private void initializeShape()  {
    w_idx = 2;
    w1 = new double[STRING_POINTS];
    w2 = new double[STRING_POINTS];
    w3 = new double[STRING_POINTS];
    w4 = new double[STRING_POINTS];

    // In terms of Burden Faires, p. 702, I think we have:
    // k = delta_t = time step size
    // h = delta_x = spatial grid size
    // alpha = sqrt(m_Tension/m_Density) = wave speed
    double r = (delta_t*delta_t*m_Tension/m_Density)/(delta_x*delta_x);
    w1[0] = w1[STRING_POINTS-1] = 0;
    w2[0] = w2[STRING_POINTS-1] = 0;
    int i;
    for (i=1;i<STRING_POINTS-1;i++) {
      w1[i] = init(i*delta_x);
      // Following assumes initial velocity is zero.
      // Note that we could use second derivative of f for more accuracy.
      w2[i] = (1 - r)*init(i*delta_x) +
          (r/2)*(init((i+1)*delta_x) + init((i-1)*delta_x));
      // add in the initial velocity term
      w2[i] += delta_t*Math.sqrt(m_Tension/m_Density)*velocity(i*delta_x);
      // set up the constant
    }
    total_t = 0;
  }

  /*  How to find the correct velocity for a traveling wave:
  The d'Alembert equation for a left-moving traveling wave is f(x + ct), where f()
  is a general single-variable waveform, think of it as f(x) moving to
  the left as t increases.  The velocity (partial derivative with respect
  to time) is then c f'(x + ct) which at time t=0 is  c f'(x).
  So take the first derivative of the waveform, and multiply by c
  where c is the wave speed = sqrt(tension/density).
  Right-moving wave is f(x - ct) with derivative -c f'(x)
  */
  private double velocity(double x) {
    double w;
    switch (m_shape)    {
      default:
      case FLAT:
        return 0;
      case QUARTER_TRI:
        x = x - m_Length/8;
        w = m_Length/8;
        if ((x < -w) || (x > w))
          return 0;
        else
          return -(0.1/w)*((x < 0) ? 1 : -1);
      case SINE_PULSE:
        x = x - m_Length/8;
        w = m_Length/8;
        if ((x<-w) || (x > w))
          return 0;
        else
          return -0.05*(Math.PI/w)*Math.cos(Math.PI*x/w);
    }
  }

  private double init(double x)  {
    double w;
    switch (m_shape)    {
      default:
      case FLAT:
        return 0;
      case TRIANGLE:
        return 0.2*((x < m_Length/2) ? x/m_Length : 1 - x/m_Length);
      case QUARTER_TRI:
        x = x - m_Length/8;
        w = m_Length/8;
        if ((x < -w) || (x > w))
          return 0;
        else
          return 0.1*((x < 0) ? x/w + 1 : -x/w + 1);
      case SQUARE_PULSE:
        x = x - m_Length/2;
        w = m_Length/8;
        if ((x < -w) || (x > w))
          return 0;
        else
          return 0.1;
      case SINE_PULSE:
        x = x - m_Length/8;
        w = m_Length/8;
        if ((x<-w) || (x > w))
          return 0;
        else
          return 0.05*Math.sin(Math.PI*x/w);
      case HALF_SINE_PULSE:
        w = m_Length/3;
        if (x>w)
          return 0;
        else
          return Math.sin(Math.PI*x/w);
      case FANCY_SINE:
        return 0.1*(Math.sin(2*Math.PI*x/m_Length) + Math.sin(4*Math.PI*x/m_Length) +
          Math.sin(6*Math.PI*x/m_Length))/3;
    }
  }

  public void modifyObjects()  {
    // set the curve to point at the current vector
    double[] w;
    switch (w_idx) {
      default:
      case 1: w = w1; break;
      case 2: w = w2; break;
      case 3: w = w3; break;
    }
    m_Curve.m_Data = w;
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(TENSION))
      {m_Tension = value; return true;}
    else if (name.equalsIgnoreCase(DENSITY))
      {m_Density = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(TENSION))
      return m_Tension;
    else if (name.equalsIgnoreCase(DENSITY))
      return m_Density;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  // when SimCanvas is resized, this will be called to let us know.
  public void objectChanged(Object o) {
    if (cvs == o) {
      // could change size of the curve object here???
    }
  }

  /* This simulation should not inherit from DiffEq!!!
    It is not an ODE simulation...   So we define this bogus evaluate function.
    */
  public void evaluate(double[] x, double[] change) { }

  // don't use the Runge Kutta diff eq solver
  // instead calculate the variables directly from the time.
  protected void advance(double time_step)  {
    total_t += delta_t;

    CalcAnalyticData(total_t);

    //if (t>-99999)
    //  return;

    // figure out which vector to use for latest data
    double[] w_new;
    double[] w;
    double[] w_old;

    switch (w_idx) {
      default:
      case 1:  // w1 is most recent data, then 3, 2 is oldest
        w = w1; w_old = w3; w_new = w2; w_idx = 2; break;
      case 2:  // w2 is most recent data, then 1, 3 is oldest
        w = w2; w_old = w1; w_new = w3; w_idx = 3; break;
      case 3:  // w3 is most recent data, then 2, 1 is oldest
        w = w3; w_old = w2; w_new = w1; w_idx = 1; break;
    }

    final int MODE = 0;  // 0 normal, 1 different density, 2 bead, 3 damping

    int N = STRING_POINTS-1;
    w_new[0] = 0;
    w_new[N] = 0;

    // use vertical position of block to set left point
    w_new[0] = m_Block.getCenterY();
    double r = (delta_t*delta_t*m_Tension/m_Density)/(delta_x*delta_x);

    if (m_shape == FLAT) {
      double c = Math.sqrt(m_Tension/m_Density);
      if (total_t < Math.PI*2/c)
        w_new[0] = 0.05*Math.sin(c*total_t);
      else
        w_new[0] = w[1];
      w_new[N] = w[N-1];

      // cancel the wave at the other end with opposite impulse
      // (and failed attempt to make a non-reflective sink)
      if (false) {
        double t = total_t - m_Length/c;
        double calc_sin = 0.05*Math.sin(c*t);
        double pred = r*(w[N-2] -2*w[N-1] +w[N]) +2*w[N] -w_old[N];
        //double pred = r*(11*w[N-4] -56*w[N-3] +114*w[N-2] -104*w[N-1] +35*w[N])/12 +2*w[N] -w_old[N];
        if (false && t > -2 && t < 5+2*Math.PI/c) {
          System.out.println("time="+t+"   calc_sin="+calc_sin+"   pred="+pred);
        }
        if (true && t>0 && t<2*Math.PI/c)
          w_new[N] = calc_sin;
        //w_new[N] = pred;
      }
    }

    int midpt = STRING_POINTS/2-1;
    // ******  here is the heart of the PDE solver  ***********
    for (int i=1; i<=N-1; i++) {
      if (MODE==2 && i==midpt) {
        double d = w[i-1] -2*w[i] + w[i+1];
        d = d*m_Tension*delta_t*delta_t/(bead_mass*delta_x);
        w_new[i] = d + 2*w[i] - w_old[i];
        continue;
      }
      if (MODE==1 && i>=midpt)
        r = (delta_t*delta_t*m_Tension/m_Density2)/(delta_x*delta_x);
      w_new[i] = 2*(1-r)*w[i] + r*(w[i+1] + w[i-1]) - w_old[i];
      if (MODE==3 && i>=midpt) {
        // add damping
        double d = -m_damping*(w[i] - w_old[i])*delta_t/m_Density;
        // increase damping gradually over string to avoid reflection
        w_new[i] += (i-midpt)*d/midpt;
      }
    }
    //int N=STRING_POINTS-1;
    //w_new[N] = w_new[N-1];

    // move the bead to its new position  (simple Euler diff eq solver... can we do better?)
    /*
    bead_y += delta_t*bead_v;
    double slopeL = (w_new[midpt] - w_new[midpt-1])/delta_x;
    double slopeR = (w_new[midpt+1] - w_new[midpt])/delta_x;
    bead_v += (delta_t*m_Tension/bead_mass)*(slopeR - slopeL);
    w_new[midpt] = bead_y;
    */

    // print average delta(t) and stability every few seconds
    if (++time_idx >= AVG_LEN)
      time_idx = 0;
    times[time_idx] = time_step;
    now_time += time_step;
    stab[time_idx] = Math.sqrt(r);
    if (now_time - last_time > 1)  {
      last_time = now_time;
      double d=0;
      double e=0;
      for (int i=0; i<AVG_LEN; i++)  {
        d += times[i];
        e += stab[i];
      }
      System.out.println("avg delta(t) = "+d/AVG_LEN+" stability ="+e/AVG_LEN);
      double left = amplitude(0, w_new, delta_x);
      double right = amplitude(1, w_new, delta_x);
      System.out.println("amplitude left = "+left+"   right = "+right);
      left = energy(0, w, w_new, delta_x, delta_t);
      right = energy(1, w, w_new, delta_x, delta_t);
      System.out.println("energy left = "+left+"   right = "+right+ "  total= "
      +(left+right));
      System.out.println("");
    }
    modifyObjects();
  }

  // figures the amplitude of left or right side of string
  private double amplitude(int side, double[] w, double delta_x) {
    int start = (side == 0) ? 0 : STRING_POINTS/2;
    int end = (side == 0) ? STRING_POINTS/2 : STRING_POINTS;
    double a = 0;
    for (int i=start; i<end; i++) {
      double m = w[i];
      if (m<0) m = -m;
      if (m>a) a = m;
    }
    return a;
  }

  private double amplitude2(int side, double[] w, double delta_x) {
    int start = (side == 0) ? 0 : STRING_POINTS/2;
    int end = (side == 0) ? STRING_POINTS/2 : STRING_POINTS;
    double s = 0;
    // integrate square of amplitude over length of string
    for (int i=start; i<end; i++) {
      s += w[i]*w[i]*delta_x;  // integrate square of wave
    }
    return Math.sqrt(s);
  }

  private double energy(int side, double[] w, double[] w_new, double delta_x,
    double delta_t) {
    int start = (side == 0) ? 1 : STRING_POINTS/2;
    int end = (side == 0) ? 1+STRING_POINTS/2 : STRING_POINTS-1;
    double k = 0;
    double v = 0;
    // integrate potential and kinetic energy over length of string
    for (int i=start; i<end; i++) {
      double diff = (w_new[i-1] - w_new[i+1]) / (2*delta_x);
      v += diff*diff*delta_x;  // potential energy integral
      diff = (w_new[i] - w[i])/ delta_t;
      k += diff*diff*delta_x;  // kinetic energy integral
    }
    double density = (side == 0) ? m_Density : m_Density2;
    return 0.5*(m_Tension*v + density*k);
  }

  private void CalcAnalyticData(double t)
  {
  }

}
