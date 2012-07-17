package com.myphysicslab.simlab;

public interface DiffEq {
  /* returns the array of state variables associated with this diff eq
   */
  public double[] getVars();

  /* defines the equations of the diff eq.
    input is the current variables in array 'x'.
    output is change rates for each diffeq in array 'change'.
   */
  public void evaluate(double[] x, double[] change);

  /* returns array of booleans corresponding to the state variables.
     If true, then the variable is calculated by the ode solver.
     If false, then the variable is not modified by the ode solver. */
  public boolean[] getCalc();
}
