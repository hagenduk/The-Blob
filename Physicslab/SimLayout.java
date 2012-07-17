/*
  File: SimLayout.java

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
import javax.swing.*;
///////////////////////////////////////////////////////////////////////////
/*  Gives the canvas (and optionally also the graph) the bulk of the
space in the window, with the controls at the bottom taking just enough
room for their current size:
   | ----------------|
   | GRAPH  |  CANVAS|
   | ----------------|
   | controls        |
   | ----------------|
The canvas must be the first component (index 0) in the list of
components in the Container.
The graph is optionally the second component.
The controls come after that.

We determine the number of 'panels', ie. canvas or graph, by
looking at what components are instances of SimPanel, which is
an empty interface used solely to mark these items for this layout
manager.
*/
public class SimLayout implements LayoutManager {
  private final int SPACER = 5;
  private boolean debug = false;

  public SimLayout() {}
  public void addLayoutComponent(String name, Component c) {
  }
  public void removeLayoutComponent(Component c) {
  }

  public Dimension preferredLayoutSize(Container target) {
    return new Dimension(500, 500);
  }

  public Dimension minimumLayoutSize(Container target) {
    return new Dimension(100,100);
  }

  private int countNumPanels(Container target) {
    int numPanels = 0;
    int n = target.getComponentCount();
    for (int i = 0; i < n; i++) {
      if (target.getComponent(i) instanceof SimPanel)
        numPanels++;
    }
    return numPanels;
  }

  //NOTE: getX() and getHeight() are Java 1.2 features, so don't use them!!
  // leave the x location as is for now, only change the vertical location.
  private int grovelControls(Container target, int startComponent,
    boolean doMove, int startVertical, int[] lineHeights) {
    int n = target.getComponentCount();
    int canvasWidth = target.getSize().width;
    int verticalPosition = startVertical;
    int horizontalPosition = SPACER;
    int currentLineHeight = 0;
    int lineNum = 0;
    int itemOnLine = 0;
    boolean forceNewLine = false;
    for (int i = startComponent; i < n; i++) {
      Component m = target.getComponent(i);
      if (m.isVisible()) {
        int w,h;  // width & height for current component
        Dimension d = m.getPreferredSize();
        h = d.height;
        // We assume that wide labels take up a whole line of the display.
        //boolean wideLabel = m instanceof MyLabel || m instanceof SimLine;
        boolean wideLabel = false;
        if (wideLabel) {
          // wide label gets entire width of canvas
          w = canvasWidth;
          // wide label must be first and only item on a line
          if (itemOnLine!=0) forceNewLine = true;
        } else {
          w = d.width;
        }
        // check if this component can't fit on current line.
        if (forceNewLine || (!wideLabel && horizontalPosition + w > canvasWidth)) {

          // Can't fit on the current line, so move to next line.
          lineHeights[lineNum++] = currentLineHeight;
          // Move to next line
          horizontalPosition = SPACER;
          verticalPosition += currentLineHeight + SPACER;
          currentLineHeight = 0;
          itemOnLine = 0;
        } // end new line
        // set location of component
        if (doMove) {
          m.setSize(w, h);
          // vertically center within the item's line.
          int x = horizontalPosition;
          int y = verticalPosition + (lineHeights[lineNum] - m.getSize().height)/2;
          m.setLocation(x,y);
          if (debug) {
            System.out.println("component "+m);
            System.out.println("verticalPosition = "+verticalPosition+
                " lineHeight="+lineHeights[lineNum]+" lineNum="+lineNum);
            System.out.println("setlocation "+x+" "+y+"  setsize "+w+" "+h);
          }
        }
        if (h > currentLineHeight)  currentLineHeight = h;
        // advance position where the next component will be placed
        horizontalPosition += w + SPACER;
        forceNewLine = wideLabel;  // force a new line after a wideLabel
        itemOnLine++;
      }
    }
    // record last line statistics
    lineHeights[lineNum] = currentLineHeight;
    return verticalPosition + currentLineHeight;
  }

  public void layoutContainer(Container target) {
    // Canvas is assumed to be first component (index 0).
    // Graph (if present) is assumed to be second component (index 1).
    // First pass: position the controls in upper part of window.
    // Lay them out left to right, and skip to next line when they
    // don't fit.
    int canvasWidth = target.getSize().width;
    int numPanels = countNumPanels(target);
    int[] lineHeights = new int[100];
    // startComponent is: Which component is at start of current line.
    int lastY = grovelControls(target, numPanels,
            false /* doMove*/, 0/*startVertical*/, lineHeights);
    // Now we know how much vertical space the controls need.
    // Set the canvas & graph to take up the rest of the space.
    int canvasHeight = target.getSize().height - (lastY+SPACER);
    Component cnvs = target.getComponent(0);  // this should be the canvas
    if (numPanels == 1) {
      cnvs.setLocation(0, 0);
      cnvs.setSize(canvasWidth, canvasHeight);
    } else if (numPanels >= 2) {
      cnvs.setLocation(canvasWidth/2, 0);
      cnvs.setSize(canvasWidth/2, canvasHeight);
      Component graph = target.getComponent(1);  // should be the graph
      graph.setLocation(0,0);
      graph.setSize(canvasWidth/2, canvasHeight);
    }
    // Second pass: move all the controls to below the canvas.
    grovelControls(target, numPanels, true, canvasHeight+SPACER, lineHeights);
		// The following seems to help Firefox and Camino on the Mac, where
		// without this the controls do not appear.  Added 10/10/06.
		//target.repaint();  
		// 10/12/06:  moved this to Simulation.run()
  }
}
