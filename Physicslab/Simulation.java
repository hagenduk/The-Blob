/*
  File: Simulation.java

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

/* Current assumption is:  All Simulations have controls.  But not all
   Simulations have a graph.

	MODIFICATION HISTORY:
	Oct 10 2006:  hiding graph now also hides its controls.
 */

package com.myphysicslab.simlab;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;

public abstract class Simulation implements Runnable, Graphable, MouseDragHandler,
      DiffEq, Subject {
  private double lastTime = -9999;
  protected boolean m_Animating = true;
  protected String[] var_names;
  protected SimCanvas cvs;
  protected Graph graph;  // graph is optional... might be null
  private JCheckBox showGraphCheckbox; // optional... may be null
  private JCheckBox showControlsCheckbox;
  private Listener listener = new Listener();
  protected double[] vars;  //  variables for Diff Eqn... positions and velocities
  // calc vector = whether variables are calculated by ode solver
  protected boolean[] calc;
  protected DiffEqSolver odeSolver;
  protected Container container;
  private Vector controls = new Vector(10);  // list of controls added by this simulation
  private Vector observers = new Vector(10);
  private SimLine separatorLine = null;  // for visually separating the graph and other controls
  protected double startTime = 0;
  protected double simTime = 0;
	private boolean paintControlCludge = true;  // cludge to deal with controls not painting.

  public Simulation(Container applet, int numVars) {
    this(applet);
    vars = new double[numVars];
    calc = new boolean[numVars];
    for (int i=0; i<calc.length; i++)
      calc[i] = true;
  }

  public Simulation(Container applet) {
    this.container = applet;  // really its the getContentPane() of the JApplet
    container.add(cvs = makeSimCanvas(), 0);
    odeSolver = makeDiffEqSolver();
  }

  // Factory Method, so that particular subclasses can specify other classes.
  protected SimCanvas makeSimCanvas() {
    return new SimCanvas(this);
  }

  // Factory Method, so that particular subclasses can specify other classes.
  protected LayoutManager makeLayoutManager() {
    return new SimLayout();
  }

  // Factory Method, so that particular subclasses can specify other classes.
  protected DiffEqSolver makeDiffEqSolver() {
    return new RungeKutta(this);
  }

  // Factory Method, so that particular subclasses can specify other classes.
  protected Graph makeGraph() {
    return new Graph(this, container);
  }

  public void setupControls() {
    // default state for parameter controls is not visible
    // (but most simulations make them visible in constructor).
    container.add(showControlsCheckbox = new JCheckBox("show controls"));
    showControlsCheckbox.setSelected(false);
    showControlsCheckbox.addItemListener(listener);
  }

  // This class handles the 'show graph' and 'show controls' checkboxes
  protected class Listener implements ItemListener {

    public void itemStateChanged(ItemEvent event) {
      ItemSelectable isl = event.getItemSelectable();
      if (showGraphCheckbox != null && isl == showGraphCheckbox) {
        showGraph(null!=showGraphCheckbox.getSelectedObjects());
        container.invalidate();  // cause the container to lay out
        container.validate();
        container.repaint();
      }
      else if (showControlsCheckbox != null && isl == showControlsCheckbox) {
        // adds controls to the end of the list of components in the window
        showControls(null!=showControlsCheckbox.getSelectedObjects());
        container.invalidate();
        container.validate();
        container.repaint();
      }
    }
  }

  protected int getComponentIndex(Component c) {
    Component[] cArray = container.getComponents();
    for(int i=0; i<cArray.length; i++) {
      if (cArray[i] == c)
        return i;
    }
    return -1;
  }

  private boolean containerHas(Component c) {
    int n = container.getComponentCount();
    for (int i=0; i<n; i++)
      if (c == container.getComponent(i))
        return true;
    return false;
  }

  public CoordMap getCoordMap() {
    return cvs.getCoordMap();
  }

  public void setCoordMap(CoordMap map) {
    cvs.setCoordMap(map);
  }

  public Graph getGraph() {
    return graph;
  }

  protected void addControl(Component c) {
    if (showControlsCheckbox != null && 
        null!=showControlsCheckbox.getSelectedObjects()) {
      container.add(c);
    }
    controls.addElement(c);
  }

  protected void removeControl(Component c) {
    container.remove(c);
    controls.removeElement(c);
  }

  protected void addObserverControl(Observer obs) {
    addControl((Component)obs);
    attach(obs);
  }

  protected void removeObserverControl(Observer obs) {
    removeControl((Component)obs);
    detach(obs);
  }

  /* implementation of Subject interface */
  public void attach(Observer o) {
    observers.addElement(o);
  }

  public void detach(Observer o) {
    observers.removeElement(o);
  }

  public void setParameter(String name, double value) {
    if (trySetParameter(name, value)) {
      // notify all Observers that the parameter has changed
      for (Enumeration e = observers.elements(); e.hasMoreElements(); )
        ((Observer)e.nextElement()).update(this, name, value);
    } else
      throw new IllegalArgumentException("no such parameter "+name);
  }

  public double getParameter(String name) {
    throw new IllegalArgumentException("no such parameter "+name);
  }
  /* end of Subject interface */

  // trySetParameter() returns true if it was able to set the given parameter
  protected boolean trySetParameter(String name, double value) {
    return false;
  }

  public String[] getParameterNames() {
    // default is to return an empty array of strings, because no parameters.
    return new String[0];
  }

  protected void graphSetup() {
        throw new IllegalStateException("graphSetup is deprecated");
  }

  public void setupGraph() {
    // default state for graph is to be visible
    graph = makeGraph();
    if (graph!=null) {
      container.add(graph, 1);
      // add the 'show graph' checkbox after the 'show controls' checkbox
      int index = getComponentIndex(showControlsCheckbox);
      if (index < 0)
        throw new IllegalStateException("cannot setup graph because controls not yet created");
      container.add(showGraphCheckbox = new JCheckBox("show graph"), ++index);
      showGraphCheckbox.setSelected(true);  // was: setState(true);
      showGraphCheckbox.addItemListener(listener);
      separatorLine = new SimLine();
      container.add(separatorLine, ++index);
      // add graph buttons after the graph checkbox, before separatorLine
      graph.createButtons(container, index);
      graph.setVars(0,1);
      container.invalidate();
      //container.validate();
    }
  }

  public void showGraph(boolean wantGraph) {
    //System.out.println("Simulation.showGraph("+wantGraph+") with graph="+graph);
    if (!wantGraph && graph==null)
      return;  // if don't want graph, and graph not created, do nothing
    if (graph==null || showGraphCheckbox==null)
      throw new IllegalStateException("cannot show graph because graph not created");
    boolean hasGraph = containerHas(graph);
    if (!hasGraph && wantGraph) { // show graph
      container.add(graph, 1); // add at specified position in list
      graph.setVisible(true);
      container.invalidate();
      container.validate();
			graph.showControls(container, getComponentIndex(separatorLine));
      graph.enableControls(true);
    } else if (hasGraph && !wantGraph) {
      // hide graph and its controls.
			graph.hideControls(container);
      container.remove(graph);
      container.invalidate();
      container.validate();
      graph.enableControls(false);
    }
    showGraphCheckbox.setSelected(wantGraph);
  }

  protected void shutDown() {
    showControls(false);
    if (graph != null) {
      graph.hideControls(container);
      container.remove(graph);
      graph.freeOffScreen();
    }
    container.remove(cvs);
    cvs.freeOffscreen();
    if (separatorLine != null)
      container.remove(separatorLine);
    if (showGraphCheckbox != null)
      container.remove(showGraphCheckbox);
    if (showControlsCheckbox != null)
      container.remove(showControlsCheckbox);
    controls.removeAllElements();
    observers.removeAllElements();
  }

  public synchronized void showControls(boolean wantControls) {
    for (Enumeration e = controls.elements(); e.hasMoreElements();) {
      if (wantControls) {
        Component m = (Component)e.nextElement();
        container.add(m);
      } else
        container.remove((Component)e.nextElement());
    }
    if (showControlsCheckbox != null)
      showControlsCheckbox.setSelected(wantControls);
  }

  public void setVariable(int i, double value) {
    if (i>=0 && i<vars.length) {
      vars[i] = value;
      modifyObjects();
    }
  }

  /* begin Graphable interface */
  // NOTE:  Some simulations provide 'fake' computed variables
  // (such as acceleration, kinetic energy, total energy,...)
  // Such simulations should override some of these methods.

  public int numVariables() {
    return (vars!=null) ? vars.length : 0;
  }

  public String getVariableName(int i) {
    return (var_names!=null) ? var_names[i] : "";
  }

  public double getVariable(int i) {
    return (i < vars.length) ? vars[i] : 0;
  }
  /* end Graphable interface */

  protected void advance(double time) {
    odeSolver.step(time);
    modifyObjects();
  }

	
  public void run() {
    if (m_Animating) {
      if (graph != null) {
        graph.memorize();  // always remember data, even if graph not visible
				// we could optimize here by repainting only the part of the graph
				// that has changed (the one little dot or line segment that needs to be drawn)
				// but for now we redraw the entire graph for each dot... computers are fast!
        graph.repaint();
      }
      advance(getTimeStep());
      cvs.repaint();  // causes entire canvas to repaint?
    }
		// cludge alert!  This is to try to fix the following bug:
		// Bug:  sometimes at startup, the controls area remains blank (white).
		// (seen in stand-alone application mode, and in Firefox browser on Mac).
		// I tried adding a repaint() to the end of SimLayout.layoutContainer(),
		// which helped but the bug still occurs.
		// Therefore, try doing a repaint here a second or two after startup.
		if (paintControlCludge && getTime() > 2.0) {
			Utility.println("repaint controls cludge at time="+getTime());
			container.repaint();
			paintControlCludge = false;
		}
  }

  public abstract void modifyObjects();

  public double getTime() {
    double now = (double)System.currentTimeMillis()/1000;
    if (startTime == 0) // initialize startTime... used for debugging
      startTime = now;
    return now - startTime;
  }

  public double getTimeStep() {
    /*
    double now = (double)System.currentTimeMillis()/10000;
    // figure out how much time has passed since last simulation step
    if (startTime == 0) // initialize startTime... used for debugging
      startTime = now;
    */
    double now = getTime();
    double h;
    if (lastTime < 0)  // initialization needed
      h = 0.05;
    else {
      h = now - lastTime;
      // Deal with long delays here...
      // Limiting step size causes time slippage & animation will stutter.
      // It will look like the animation "paused" during the delay, but its
      // better than having the animation do a huge discontinuous jump.
      final double limit = 0.01;
      if (h > 0.1) {
        Utility.println("time step of "+h+" reduced to "+limit);
        h = limit;
      }
    }
    lastTime = now;
    return h;
  }

  /* start of MouseDragHandler interface */
  /* set the given element to the given x & y position, subject to
    whatever constraints that the simulation wants to impose.
  // constrainedSet() is called while the element is being dragged by the mouse.
  // The mouse coordinates are passed in as x, y.  This routine then
  // will set the element to be at that position, but constrained in some way.
  // The x & y are passed in simulation coordinates, and offset to correspond
  // to the "position" of the object being dragged.
   */
  public void constrainedSet(Dragable e, double x, double y)  {
  }

  public void startDrag(Dragable e) {
  }

  public void finishDrag(Dragable e) {
    // go back to odeSolver calculation for all vars
    for (int i=0; i<calc.length; i++)
      calc[i] = true;
  }

  /* end of MouseDragHandler interface */

  /* implementation of DiffEq protocol */

  public double[] getVars() {
    return vars;
  }

  public boolean[] getCalc() {
    return calc;
  }
  /* end of DiffEq protocol */

  public String toString() {
    String s = getClass().getName() + " with "+ 
      (vars!=null ? vars.length+" variables " : "no variables");
    return s;
  }
}


