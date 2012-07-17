package com.myphysicslab.simlab;
import java.util.*;
import java.io.*;
import java.awt.*;

/**
* Provides utility methods.
*
*/
public class Utility {
  // perhaps it would be nice to set the debugSetting from ant somehow...
  // then we could vary the flag based on ant commands automatically.
  //
  public static final int DEBUG_NONE = 0;
  public static final int DEBUG_MIN = 1;
  public static final int DEBUG_MEDIUM = 2;
  public static final int DEBUG_MAX = 4;
  public static int debugSetting = DEBUG_NONE;
  //public static int debugSetting = DEBUG_MIN+DEBUG_MEDIUM+DEBUG_MAX;
  static public java.util.Random random = new Random(System.currentTimeMillis());

  /**
  * tells whether the requested flag is on... note that
  */
  static public boolean debug(int flag) {
    return 0 != (flag & debugSetting);
  }
  
  /**
  * Prints the string to System.out but only when debugging.
  */
  static public void println(String s) {
    if (debugSetting != DEBUG_NONE)
      System.out.println(s);
  }

  // Sets r to be the intersection of the two rectangles
  // and returns true if the result is non empty.
  // Best of all, it allocates no new objects!
  private boolean rectIntersect(Rectangle r, Rectangle b) {
    int left = (r.x > b.x) ? r.x : b.x;
    int top = (r.y > b.y) ? r.y : b.y;
    int right = (r.x+r.width < b.x+b.width) ? r.x+r.width : b.x+b.width;
    int bottom = (r.y+r.height < b.y+b.height) ? r.y+r.height : b.y+b.height;
    r.x = left;
    r.y = top;
    r.width = right-left;
    r.height = bottom-top;
    return (r.width>0 && r.height>0);
  }

  /** Solves A x = b, using Gaussian Elimination with Backward Substitution,
   where A is an n by n+1 augmented matrix with last column being b.
	 Based on algorithm 6.1 in Numerical Analysis, 6th edition by Burden & Faires,
	 page 358.
  */
  static public void matrixSolve(double[][] A, double[] x) {
    int n = x.length;
    int[] nrow = new int[n];
    for (int i=0; i<n; i++)
      nrow[i] = i;
    for (int i=0; i<n-1; i++) {  // elimination process
      // let p be the smallest integer with i <= p <= n and
      // A[nrow[p], i]  = max(i<=j<=n)  A[nrow[j], i] 
      int p = i;
      double max = Math.abs(A[nrow[p]][i]);
      for (int j=i+1; j<n; j++)
        if (Math.abs(A[nrow[j]][i]) > max) {
          max = Math.abs(A[nrow[j]][i]);
          p = j;
        }
      // If A[nrow[p]][i] == 0, then no solution
      if (A[nrow[p]][i] == 0)
        throw new IllegalStateException("matrixSolve: no unique solution exists");
      if (nrow[i] != nrow[p]) {  // simulated row interchange
        int ncopy = nrow[i];
        nrow[i] = nrow[p];
        nrow[p] = ncopy;
      }
      for (int j=i+1; j<n; j++) {
        double m = A[nrow[j]][i] / A[nrow[i]][i];
        // do a row operation
        for (int k=0; k<n+1; k++)
          A[nrow[j]][k] -= m*A[nrow[i]][k];
      }
    }
    if (A[nrow[n-1]][n-1] == 0)
      throw new IllegalStateException("matrixSolve: no unique solution exists");
    // start backward substitution
    x[n-1] = A[nrow[n-1]][n]/A[nrow[n-1]][n-1];
    for (int i=n-2; i>=0; i--) {
      double sum = 0;
      for (int j=i+1; j<n; j++)
        sum += A[nrow[i]][j]*x[j];
      x[i] = (A[nrow[i]][n] - sum)/A[nrow[i]][i];
    }
  }



}
