/*
  File: SimCanvas.java

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
import java.applet.*;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.*;

  /**
For Swing components, paint() is always invoked as a result of both system-triggered and
app-triggered paint requests;  update() is never invoked on Swing components.
*/
public class SimCanvas extends JComponent implements KeyListener, MouseListener,
  MouseMotionListener, SimPanel {
  private Image offScreen = null;
  private Graphics offScreenGraphics = null;
  protected MouseDragHandler mdh = null;
  private boolean needExpand = false;
  protected Dragable dragObj = null;
  private int dragOffsetX = 0, dragOffsetY = 0;
  // Synchronize any method that uses drawables Vector!   (as of Nov 2006).
  private Vector drawables = new Vector(10);  // contains drawable elements (eg. mass, spring...)
  protected CoordMap map = new CoordMap();
  private ObjectListener objListen = null;

  public SimCanvas() {
    this(null);
  }

  public SimCanvas(MouseDragHandler mdh) {
    this.mdh = mdh;
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    if (!isOpaque()) {
      Utility.println("setting SimCanvas to be opaque!");
      setOpaque(true);
    }
  }
  
  public String toString() {
    return getClass().getName()+" with offScreen= "+offScreen.toString();
  }
  
  public void setMouseDragHandler(MouseDragHandler mdh) {
    this.mdh = mdh;
  }

  public void freeOffscreen() {
    offScreen = null;
    // Important to dispose of any Graphics that we cause to be created.
    // See Java documentation about the method Graphicsl.dispose().
    if (offScreenGraphics != null) {
      offScreenGraphics.dispose();
      offScreenGraphics = null;
    }
  }

  // getPreferredSize() is defined here just to override the default
  // which returns 1 by 1 size... in the unlikely case it is every used by
  // the layout manager.
  public Dimension getPreferredSize() {
    return new Dimension(300, 300);
  }

  public void setObjectListener(ObjectListener objListen) {
    this.objListen = objListen;
  }

  // Prepend puts the element at the front of the list
  public synchronized void prependElement(Drawable e) {
    drawables.insertElementAt(e, 0);
  }

  public synchronized void addElement(Drawable e) {
    drawables.addElement(e);
  }

  public synchronized void removeElement(Drawable e) {
    drawables.removeElement(e);
  }

  public synchronized void removeAllElements() {
    drawables.removeAllElements();
  }

  public synchronized boolean containsElement(Drawable e) {
    return drawables.contains(e);
  }

  /* override update to *not* erase the background before painting */
  //public void update(Graphics g) {
  //  paint(g);
  //}

  /* CoordMap related methods */
  public synchronized void setCoordMap(CoordMap map) {
    this.map = map;
    // WARNING: don't use getWidth() or getHeight(), they are Java 1.2 features.
    Dimension d = this.getSize();
    map.setScreen(0, 0, d.width, d.height);
    freeOffscreen();
    // is this really needed?  Seems like the listeners only care about the window
    // size changing, not necessarily that the CoordMap changed.
    if (objListen != null)
      objListen.objectChanged(this);
    //System.out.println("SimCanvas.setCoordMap "+map);
  }

  public CoordMap getCoordMap() {
    return map;
  }

  public DoubleRect getSimBounds() {
    return map.getSimBounds();
  }

  public ConvertMap getConvertMap() {
    return map.getConvertMap();
  }

  /* setSize needs to be synchronized with paint to ensure it doesn't change
    the offscreen buffer during the paint operation */
  public synchronized void setSize(int width, int height) {
    //System.out.println("SimCanvas.setSize("+width+", "+height+")");
    super.setSize(width, height);
    map.setScreen(0, 0, width, height);
    freeOffscreen();
    // Expand the simulation to take up the entire screen, but only first time through here.
    // Why?  Because when the applet starts, it has zero size, only after the applet
    // and simulation have been created does the window come into being, so we can't
    // expand the map until that later time.
    // Cludgey, but no other good place for this.
    if (needExpand) {
      if (map.expand())
        needExpand = false;
    }
    if (objListen != null)
      objListen.objectChanged(this);
  }

  /* expandMap ensures that the map gets expanded (to fill available
    window space) */
  public void expandMap() {
    // usually we just set the needExpand flag and wait for expansion to happen
    // during the next setSize().
    needExpand = true;
    // But just in case its useful, expand the map now.
    //if (map!= null)
    //  map.expand();
  }

/*
  public void setLocation(int x, int y) {
    super.setLocation(x, y);
    Dimension sz = getSize();
    //mdh.map.setScreen(x, y, sz.width, sz.height);
    mdh.map.setScreen(0, 0, sz.width, sz.height);
    freeOffscreen();
  }
*/
/*
  public void print(Graphics g) {
    Dimension size = getSize();
    g.setClip(0,0,size.width, size.height);
    g.setColor(Color.white);
    g.fillRect(0,0,size.width, size.height);
    drawElements(g, map.getConvertMap());
  }
*/

  // Synchronized methods are guaranteed to run to completion before
  // any other synchronized method on the same object.
  /* synchronized to prevent anyone changing the offscreen buffer */
  /* July 2006, no longer synchronized because under swing, everything should
     run under a single thread, the UI-swing thread.
		Nov 2006:  I'm getting an exception starting here which seems thread related.
		  It is a NoSuchElementException on nextElement() in drawElements()...
		  So perhaps I DO need synchronization?
		  I'm trying to add synchronization to every method that looks at drawables.
		  I think that because I have a separate thread for running the simulation,
		  that I DO have threads other than the UI-Swing thread.
  */
  public void paintComponent (Graphics g) {
    // createImage doesn't work during "init()"... probably because
    // applet is zero width & height during init.
    if (false && offScreen == null) {  // offscreen not being used now!!!
      offScreen = createImage(getSize().width, getSize().height);
      if (offScreen==null)
        throw new IllegalStateException("SimCanvas.paintComponent() is unable to create offscreen image!");
    }

    if (offScreen != null) {
      if (offScreenGraphics == null)
        offScreenGraphics = offScreen.getGraphics();
      Dimension size = getSize();
      offScreenGraphics.setClip(0,0,size.width, size.height);
      // clear offScreen to white
      offScreenGraphics.setColor(Color.white);
      offScreenGraphics.fillRect(0,0,size.width, size.height);
      drawElements(offScreenGraphics, map.getConvertMap());
      g.drawImage(offScreen, 0, 0, null);
      g.drawRect(10, 10, 100, 100);
    } else {
      // clearing and drawing everything! NOT efficient...
      Dimension size = getSize();
      g.setClip(0,0,size.width, size.height);
      // clear offScreen to white
      g.setColor(Color.white);
      g.fillRect(0,0,size.width, size.height);
      drawElements(g, map.getConvertMap());
    }
  }

  protected synchronized void drawElements(Graphics g, ConvertMap map) {
    for (Enumeration e = drawables.elements(); e.hasMoreElements(); )
      ((Drawable)e.nextElement()).draw(g, map);
  }


  private Frame getFrame() {
    Component c = this;
    while ((c = c.getParent()) != null) {
      if (c instanceof Frame)
        return (Frame)c;
    }
    return null;
  }

  protected synchronized Dragable findNearestDragable(double x, double y) {
    // Returns the draggable object that is NEAREST the specified point
    double distance = Double.POSITIVE_INFINITY;
    Dragable nearest = null;
    for (Enumeration e = drawables.elements(); e.hasMoreElements(); ) {
      Object o = e.nextElement();
      if (o instanceof Dragable) {
        Dragable d = (Dragable)o;
        if (d.isDragable()) {
          double dist = d.distanceSquared(x,y);
          if (dist < distance) {
            distance = dist;
            nearest = d;
          }
        }
      }
    }
    return nearest;
  }

  public void mousePressed(MouseEvent evt) {
    int scr_x = evt.getX();  // screen coords
    int scr_y = evt.getY();
    // which object did mouse click on?
    double sim_x = map.screenToSimX(scr_x);  // simulation coords
    double sim_y = map.screenToSimY(scr_y);
    dragObj = findNearestDragable(sim_x, sim_y);
    if (dragObj != null) {
      dragOffsetX = scr_x - map.simToScreenX(dragObj.getX());
      dragOffsetY = scr_y - map.simToScreenY(dragObj.getY());
      if (mdh!=null)
        mdh.startDrag((Dragable)dragObj);
    }
    //System.out.println("mouse pressed at "+scr_x+" "+scr_y);
  }

  public void mouseDragged(MouseEvent evt) {
    if (dragObj != null) {
      double sim_x = map.screenToSimX(evt.getX() - dragOffsetX);
      double sim_y = map.screenToSimY(evt.getY() - dragOffsetY);
      // sim_x, sim_y should correspond to the new m_X1, m_Y1 for obj
      // let the simulation modify the object
      if (mdh!=null)
        mdh.constrainedSet(dragObj, sim_x, sim_y);
    }
    //System.out.println("mouse dragged to "+x+" "+y);
  }

  public void mouseReleased(MouseEvent evt) {
    if (dragObj != null && mdh!=null)
        mdh.finishDrag(dragObj);
    dragObj = null;
  }

  public void mouseClicked(MouseEvent evt) {}

  public void mouseEntered(MouseEvent evt) {}

  public void mouseExited(MouseEvent evt) {}

  public void mouseMoved(MouseEvent evt) {}

  // keyPressed is where we can capture control keys like backspace & enter
  public void keyPressed(KeyEvent e) {
    //System.out.println("keyPressed "+e);
    int keyCode = e.getKeyCode();
  }

  public void keyReleased(KeyEvent e) {
    //System.out.println("keyReleased "+e);
    int keyCode = e.getKeyCode();
  }

  // keyTyped indicates a key has been pressed & released... only for keys that have a unicode
  // character representation (so no control keys like enter, backspace, cursor)
  public void keyTyped(KeyEvent e) {
    //System.out.println("keyTyped "+e);
    char c = e.getKeyChar();
  }

  /* 
    (Sept 2006: commented out because with swing, we don't care about older versions anymore...)
      NOTE: we need the Java 1.1 version also!!!  Otherwise, this component
     will not be focusable under Java 1.1
     So ignore any warning that this method has been deprecated and leave it here. 

  public boolean isFocusTraversable()   { // Java 1.1 version
    return true;
  }
*/

  public boolean isFocusable()  { // Java 1.4 version
    return true;
  }

}

