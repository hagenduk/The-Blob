/* Dealing with thread sync problems:
Thruster5 changes the number of objects animating under control of
a menu item... this can result in the animation thread (this one here)
trying to iterate over a list of objects where it expects to have 5 objects
but it only has 3 objects for example.  The solution I chose is to
call stop() and start() in the method that changes the number of objects.
Ideally these methods should wait until the animation thread ends... but
that's not happening now.
I don't recommend using the requestSuspend() method unless you learn
more about avoiding thread deadlocks & such.
*/
package com.myphysicslab.simlab;
import java.awt.*;

public class SimThread extends Thread {
  private Runnable sim;
  private long delay;
  private boolean suspendRequested = false;

  SimThread(Runnable sim, long delay) {
    super("SimThread");
    this.sim = sim;
    this.delay = delay;
  }

  public void run() {
    try {
      while (!interrupted()) { // loop until interrupted
        checkSuspended();
        sim.run();
        sleep(delay);  // milliseconds
      }
    }
    catch(InterruptedException e) {
      Utility.println("SimThread thread interrupted.");
    }
  }

  public void requestSuspend() {
    suspendRequested = true;
  }

  private synchronized void checkSuspended() throws InterruptedException  {
    while (suspendRequested)
      wait();
  }

  public synchronized void requestResume() {
    suspendRequested = false;
    notifyAll();
  }
}

