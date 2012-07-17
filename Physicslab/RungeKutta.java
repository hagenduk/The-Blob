package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;

/////////////////////////////////////////////////////////////////////////////////
public class RungeKutta implements DiffEqSolver {
  DiffEq ode;
  double[] inp,k1,k2,k3,k4;

  public RungeKutta(DiffEq ode) {
    this.ode = ode;
  }

  // Runge-Kutta method for solving ordinary differential equations
  // Calculates the values of the variables at time t+h
  // t = last time value
  // h = time increment
  // vars = array of variables
  // N = number of variables in x array
  public void step(double stepSize) {
    double[] vars = ode.getVars();
    int N = vars.length;
    if ((inp == null) || (inp.length != N)) {
      inp = new double[N];
      k1 = new double[N];
      k2 = new double[N];
      k3 = new double[N];
      k4 = new double[N];
    }
    int i;
    ode.evaluate(vars, k1);  // evaluate at time t
    for (i=0; i<N; i++)
      inp[i] = vars[i]+k1[i]*stepSize/2; // set up input to diffeqs
    ode.evaluate(inp, k2);   // evaluate at time t+stepSize/2
    for (i=0; i<N; i++)
      inp[i] = vars[i]+k2[i]*stepSize/2; // set up input to diffeqs
    ode.evaluate(inp, k3);   // evaluate at time t+stepSize/2
    for (i=0; i<N; i++)
      inp[i] = vars[i]+k3[i]*stepSize; // set up input to diffeqs
    ode.evaluate(inp, k4);  // evaluate at time t+stepSize
    // determine which vars should be modified (calculated)
    boolean[] calc = ode.getCalc();
    // modify the variables
    for (i=0; i<N; i++)
      if (calc[i])
        vars[i] = vars[i]+(k1[i]+2*k2[i]+2*k3[i]+k4[i])*stepSize/6;

  }
}
