/*
  File: Graph.java

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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.*;

/**  Graph class -- draws a graph.

We keep an off screen buffer to draw the graph into.

There is a variable "needRedraw" which we can set whenever
the entire graph needs to be redrawn, (for example because
the scale has changed, or axes changed, or window resized.)
If 'needRedraw' is set, we clear and redraw the entire graph
into the off screen buffer.

If 'needRedraw' is not set, then we just paint the most recent
points into the graph.  (This is an incremental draw, instead
of a full redraw).

To paint the graph, we first make sure the off screen buffer
is up-to-date;  then we copy from the off screen buffer to
the screen.  Additionally, we draw the current position of
the most recent point directly to the screen (NOT into the 
offscreen buffer).

There is a large circular list of points for the graph.  This is
filled in until full, then we keep a pointer showing the latest
position in the list.  This list allows us to redraw the graph
in full after a 'needRedraw' request.

How the circular memory list works:
  We write new entries into the array memX & memY until it fills.
  Then we wrap around and start writing to the beginning again.
  memIndex always points to the next location to write to.
  If memSize < memLen, then we have entries at 0,1,2,...,memIndex-1
  If memSize = memLen, then the order of entries is:
    memIndex, memIndex+1, ..., memLen-1, 0, 1, 2, ..., memIndex-1

Reference for more info on Painting:
"Painting in AWT and Swing" by Amy Fowler
http://java.sun.com/products/jfc/tsc/articles/painting/index.html

MODIFICATION HISTORY:
12 Oct 2006:  Fixed bug in drawPoints related to the "clear graph" button.
   Synchronized the methods related to the memory list.
*/
public class Graph extends JComponent implements MouseListener, ItemListener,
  ActionListener,  SimPanel {
  public static final int DOTS = 0;
  public static final int LINES = 1;
  private Image offScreen = null;   // the off screen buffer for the graph image. 
  private Graphable sim;
  private CoordMap map;
  //private Rectangle dirtyRect = null; // where update is needed (or null if no update needed)
  private int xVar = 0;  // index of x variable in simulation's vars[]
  private int yVar = 1;  // index of y variable in simulation's vars[]
  private int zVar = 2;  // index of (optional) z variable in simulation's vars[]
  private int drawMode = LINES;
  private int dotSize = 1;
  private NumberFormat nf;
  private Font numFont = null;
  private FontMetrics numFM = null;
  private boolean autoScale;
  private boolean rangeSet;
  private boolean rangeDirty;
  private double rangeXHi, rangeXLo, rangeYHi, rangeYLo;
  private double rangeTime = 0;  // zero means 'uninitialized'
  private boolean needRedraw = true;
  private static final int memLen = 3000;
  private double[] memX = new double[memLen];  // memory of x coords
  private double[] memY = new double[memLen];  // memory of y coords
  private double[] memZ = new double[memLen];  // memory of (optional) z coords
  private int memIndex = 0;  // index for next entry in memory list
  private int memSize = 0;  // number of items in memory list
  private int memDraw = 0;  // index for last entry drawn
  private JComboBox yGraphChoice;
  private JPanel yPanel;
  private JComboBox xGraphChoice;
  private JPanel xPanel;
  private JComboBox dotChoice;
  private JButton clearButton;
  private Container container;
  private double startTime;
  // find red and blue hues, their difference, etc.
  static private float[] red = Color.RGBtoHSB(1, 0, 0, null);
  static private float[] blue = Color.RGBtoHSB(0, 0, 1, null);
  static private float redHue = red[0];
  static private float blueHue = blue[0];
  static private float diffHue = (redHue < blueHue) ? blueHue - redHue : redHue - blueHue;
  static private float lowHue = (redHue < blueHue) ? redHue : blueHue;
  static private boolean zMode = false;  // turns on color display of z mode.
  
  
  public Graph(Graphable sim, Container applet) {
    startTime = (double)System.currentTimeMillis()/1000;
    this.sim = sim;
    this.container = applet;
    // CoordMap inputs are direction, x1, x2, y1, y2, align_x, align_y
    int sz = 10;
    map = new CoordMap(CoordMap.INCREASE_UP, -sz, sz, -sz, sz,
      CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE);
    nf = NumberFormat.getNumberInstance();
    map.setFillScreen(true);
    setAutoScale(true);
    addMouseListener(this);
    if (!isOpaque()) {
      Utility.println("setting Graph to be opaque!");
      setOpaque(true);
    }
  }

  // getPreferredSize() is defined here just to override the default
  // which returns 1 by 1 size... in the unlikely case it is every used by
  // the layout manager.
  public Dimension getPreferredSize() {
    return new Dimension(300, 300);
  }

  // debugging function that prints a string and the time
  private void dbg(String s) {
    double now = getTime();
    System.out.println(s+" "+now);
  }

  public void createButtons(Container container, int index) {
    clearButton = new JButton("clear graph");
    clearButton.addActionListener(this);

    int n = sim.numVariables();
    yPanel = new JPanel();
    yPanel.setLayout(new BorderLayout(1, 1));
    yPanel.add(new MyLabel("Y:"), BorderLayout.WEST);
    yGraphChoice = new JComboBox();
    for(int i=0; i<n; i++)
      yGraphChoice.addItem(sim.getVariableName(i));
    yGraphChoice.addItemListener(this);
    yGraphChoice.setSelectedIndex(yVar);
    yPanel.add(yGraphChoice, BorderLayout.EAST);

    xPanel = new JPanel();
    xPanel.setLayout(new BorderLayout(1, 1));
    xPanel.add(new MyLabel("X:"), BorderLayout.WEST);
    xGraphChoice = new JComboBox();
    for(int i=0; i<n; i++)
      xGraphChoice.addItem(sim.getVariableName(i));
    xGraphChoice.addItemListener(this);
    xGraphChoice.setSelectedIndex(xVar);
    xPanel.add(xGraphChoice, BorderLayout.EAST);

    /*
    dotPanel = new JPanel();
    dotPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
    dotPanel.add(new MyLabel("graph type"));
    */
    dotChoice = new JComboBox();
    dotChoice.addItem("dots");
    dotChoice.addItem("lines");
    dotChoice.setSelectedIndex(drawMode);
    dotChoice.addItemListener(this);

    showControls(container, index);
  }

  public void enableControls(boolean b) {
    yGraphChoice.setEnabled(b);
    xGraphChoice.setEnabled(b);
    dotChoice.setEnabled(b);
    clearButton.setEnabled(b);
  }

  public void hideControls(Container container) {
    container.remove(yPanel);
    container.remove(xPanel);
    container.remove(dotChoice);
    container.remove(clearButton);
  }

  public void showControls(Container container, int index) {
    container.add(clearButton, index);
    container.add(yPanel, ++index);
    container.add(xPanel, ++index);
    container.add(dotChoice, ++index);
  }

  public void itemStateChanged(ItemEvent event) {
    if (yGraphChoice!=null)
      yVar = yGraphChoice.getSelectedIndex();
    if (xGraphChoice!=null)
      xVar = xGraphChoice.getSelectedIndex();
    if (dotChoice!=null)    
      drawMode = dotChoice.getSelectedIndex();
    reset();
  }

  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == clearButton)
      reset();
  }

  public void setXVar(int xVar) {
    if ((xVar >= 0) && (xVar < sim.numVariables())) {
      this.xVar = xVar;
      xGraphChoice.setSelectedIndex(xVar);
      reset();
    }
  }

  public void setYVar(int yVar) {
    if ((yVar >= 0) && (yVar < sim.numVariables())) {
      this.yVar = yVar;
      yGraphChoice.setSelectedIndex(yVar);
      reset();
    }
  }

  public void setZVar(int zVar) {
    if ((zVar >= 0) && (zVar < sim.numVariables())) {
      this.zVar = zVar;
      this.zMode = true;  // turn on special z color mode
			this.zMode = false;  // turned entirely off for now...
      reset();
    }
  }
  public void setVars(int xVar, int yVar) {
    setXVar(xVar);
    setYVar(yVar);
  }

  public void setDrawMode(int drawMode) {
    this.drawMode = drawMode;
    dotChoice.setSelectedIndex(drawMode);
    reset();
  }

  public int getDrawMode() {
    return this.drawMode;
  }

  public synchronized void reset(){
    rangeSet = false;
    rangeDirty = false;
    rangeTime = 0;  // zero means 'uninitialized'
    memIndex = memSize = memDraw = 0;  // clear out the memory
    needRedraw = true;
    Utility.println("**** reset is calling repaint()");
    repaint();
  }


  public void setSize(int width, int height) {
    Utility.println("*****  Graph.setSize("+width+", "+height+")");
    super.setSize(width, height);
    freeOffScreen();
    map.setScreen(0, 0, width, height);
    needRedraw = true;
  }

  /** converts a z value to a color, by the following
  rough correspondence:
  ang velocity: -2.4 to +2.4  -->  blue to red
  ang accel: -1.7 to +1.7  --> blue to red
  */
  static private Color zToColor(double z) {
    // use ang accel values for now...
    float zFraction = (((float)z - (float)(-1.7)) / (float)3.4 );
    return Color.getHSBColor(zFraction * diffHue + lowHue, 1, 1);
    //return Color.getHSBColor(zFraction * diffHue + lowHue, (float)1.0 - (float)0.5*zFraction, 1);
    //return Color.getHSBColor(zFraction * diffHue + lowHue, 1, (float)1.0 - (float)0.8*zFraction);
    //return Color.getHSBColor(redHue, zFraction, (float)0.80);
  }
  
	public void freeOffScreen() {
		offScreen = null;
	}
	
  // remember data values in a big list
  public synchronized void memorize() {
    memX[memIndex] = sim.getVariable(xVar);
    memY[memIndex] = sim.getVariable(yVar);
    if (zMode)
      memZ[memIndex] = sim.getVariable(zVar);
    if (autoScale)
      rangeCheck(memX[memIndex], memY[memIndex]);
    memIndex++;
    if (memSize < memLen)
      memSize++;
    if (memIndex >= memLen)  // wrap around at end
      memIndex = 0;
  }
	
	/** Draws the points starting from the given "from" index;  returns
	 * the index of last point drawn.
	*/
  private int drawPoints(Graphics g, int from) {
    int pointer = from;
		if (memSize > 0)
	    while (true) {
	      // pointer = last point drawn TO, so (possibly) draw from there to next point
	      int i1 = pointer;
	      int i2 = (pointer+1) % memLen;
	      // memIndex = next memory buffer to write to
	      if (i2 != memIndex) {
	        g.setColor(Color.black);
	        int x,y,w,h;
	        if (zMode)
	          g.setColor(Graph.zToColor(memZ[i1]));
	        if (drawMode == DOTS) {
	          x = map.simToScreenX(memX[i1]);
	          y = map.simToScreenY(memY[i1]);
	          w = dotSize;
	          h = dotSize;
	          g.fillRect(x, y, w, h);
	        } else {
	          int x1=map.simToScreenX(memX[i1]);
	          int y1=map.simToScreenY(memY[i1]);
	          int x2=map.simToScreenX(memX[i2]);
	          int y2=map.simToScreenY(memY[i2]);
	          x = (x1 < x2) ? x1 : x2;
	          y = (y1 < y2) ? y1 : y2;
	          // add 1 to width & height because graphics pen hangs down and right
	          // from the coordinates it is drawing at.
	          w = 1 + ((x1 < x2) ? x2-x1 : x1-x2);
	          h = 1 + ((y1 < y2) ? y2-y1 : y1-y2);
	          g.drawLine(x1, y1, x2, y2);
	        }
	        pointer = i2;
	      } else
	        break;  // exit loop when we reach the 'next to write to' point
	    }
    return pointer;
  }
	
	/** updates the off screen buffer to have the entire image of the graph. */
	private void updateOSB() {
		if (offScreen==null)
    	offScreen = createImage(getSize().width, getSize().height);
		assert(offScreen != null);
		Graphics osb = offScreen.getGraphics();  // off screen buffer
		assert(osb != null);
    if (needRedraw) {
      Rectangle b = new Rectangle(0, 0, getWidth(), getHeight());
      //osb.setClip(b.x, b.y, b.width, b.height);
      osb.setColor(Color.white);
      osb.fillRect(b.x, b.y, b.width, b.height);
      osb.setColor(Color.lightGray);
      osb.drawRect(b.x, b.y, b.width-1, b.height-1);
      drawAxes(osb);
      // redraw entire memory list by drawing from oldest point in list
      int start = (memSize<memLen) ? 0 : memIndex;
      memDraw = drawPoints(osb, start); 
      needRedraw = false;
    } else {
			// draw points up to the current point
      memDraw = drawPoints(osb, memDraw);
    }
		osb.dispose();
	}
	
  protected synchronized void paintComponent(Graphics g) {
		updateOSB();
		Rectangle clip = g.getClipBounds();
		//boolean fullClip = clip.x == 0 && clip.y == 0 && clip.height == getHeight() 
		//   && clip.width == getWidth();
		// blit the offScreen buffer onto the screen.
    g.drawImage(offScreen, clip.x, clip.y, clip.width, clip.height, null);

    // Draw XOR rectangle, in its new position, on top of the blitted graph.
    int xorX = map.simToScreenX(memX[memDraw])-1;
    int xorY = map.simToScreenY(memY[memDraw])-1;
		Color saveColor = g.getColor();
    g.setColor(Color.red);
    g.fillRect(xorX, xorY, 4, 4);
		g.setColor(saveColor);
  }
    

  public void setAutoScale(boolean auto) {
    autoScale = auto;
    rangeSet = false;
    rangeDirty = false;
    rangeTime = 0;  // zero means 'uninitialized'
  }

  // BUG NOTE:  see other notes in this file about how different threads think
  // the current time is different by as much as 25 seconds.
  private double getTime() {
    double time = (double)System.currentTimeMillis()/1000;
    //Thread t = Thread.currentThread();
    //System.out.println("Thread="+t.getName()+"  getTime="+(time-startTime));
    //return time-startTime;
    return time;
  }

  // for auto-scaling, see if we need to expand the range of the graph
  private void rangeCheck(double nowX, double nowY) {
    if (!rangeSet) {
      rangeXHi = nowX;
      rangeXLo = nowX;
      rangeYHi = nowY;
      rangeYLo = nowY;
      rangeSet = true;
    } else {
      double xspan = rangeXHi - rangeXLo;
      double yspan = rangeYHi - rangeYLo;
      double extra = 0.1;
      if (nowX <= rangeXLo) {
        rangeXLo = nowX - extra*xspan;
        rangeDirty = true;
      }
      if (nowX >= rangeXHi) {
        rangeXHi = nowX + extra*xspan;
        rangeDirty = true;
      }
      if (nowY <= rangeYLo) {
        rangeYLo = nowY - extra*yspan;
        rangeDirty = true;
      }
      if (nowY >= rangeYHi) {
        rangeYHi = nowY + extra*yspan;
        rangeDirty = true;
      }
    }
    double now = getTime();
    // JAVA BUG NOTE:  different threads get
    // different values for System.currentTimeMillis()!!!!
    // We set rangeTime to zero to indicate that rangeTime needs to be initialized.
    // We initialize here because then we are guaranteed to be running in
    // the same thread as where we get the 'now' time.
    // Otherwise, different threads think the time is as much as 25 seconds different!
    if (rangeTime == 0.0)
      rangeTime = now;
    if (rangeDirty && now > rangeTime + 2) {
      rangeTime = now;
      map.setRange(rangeXLo, rangeXHi, rangeYLo, rangeYHi);
      // change of scale means we need to redraw the entire graph
      needRedraw = true;
      repaint();
      rangeDirty = false;
    }
  }

  private void setAxesFont(Graphics g)
  {
    if (numFont == null) {
      numFont = new Font("SansSerif", Font.PLAIN, 12);
      numFM = g.getFontMetrics(numFont);
    }
    g.setFont(numFont);
  }

  private void drawAxes(Graphics g) {
    setAxesFont(g);
    // figure where to draw axes
    int x0, y0;  // screen coords of axes
    double sim_x1 = map.getMinX();
    double sim_x2 = map.getMaxX();
    double sim_y1 = map.getMinY();
    double sim_y2 = map.getMaxY();
    x0 = map.simToScreenX(sim_x1 + 0.05*(sim_x2 - sim_x1));
    // leave room to draw the numbers below the horizontal axis
    y0 = map.simToScreenY(sim_y1) - (10+numFM.getAscent()+numFM.getDescent());
    // draw horizontal axis
    g.setColor(Color.darkGray);
    g.drawLine(map.simToScreenX(sim_x1), y0, map.simToScreenX(sim_x2), y0);
    // draw vertical axis
    g.drawLine(x0, map.simToScreenY(sim_y1),  x0, map.simToScreenY(sim_y2));
    drawHorizTicks(y0, g);
    drawVertTicks(x0, g);
  }


  private void drawHorizTicks(int y0, Graphics g) {
    int y1 = y0 - 4;  // bottom edge of tick mark
    int y2 = y1 + 8;  // top edge of tick mark
    double sim_x1 = map.getMinX();
    double sim_x2 = map.getMaxX();
    double graphDelta = getNiceIncrement(sim_x2 - sim_x1);
    double x_sim = getNiceStart(sim_x1, graphDelta);
    while (x_sim < sim_x2)
    {
      int x_screen = map.simToScreenX(x_sim);
      g.setColor(Color.black);
      g.drawLine(x_screen,y1,x_screen,y2); // draw a tick mark

      // draw a number
      g.setColor(Color.gray);
      String s = nf.format(x_sim);
      int textWidth = numFM.stringWidth(s);
      g.drawString(s, x_screen - textWidth/2, y2+ numFM.getAscent());

      x_sim += graphDelta;  // next tick mark
    }

    // draw name of the horizontal axis
    String hname = sim.getVariableName(xVar);
    int w = numFM.stringWidth(hname);
    g.drawString(hname, map.simToScreenX(sim_x2) - w - 5,   y0 - 8);
  }

  private void drawVertTicks(int x0, Graphics g) {
    int x1 = x0 - 4;  // left edge of tick mark
    int x2 = x1 + 8;  // right edge of tick mark
    double sim_y1 = map.getMinY();
    double sim_y2 = map.getMaxY();
    double graphDelta = getNiceIncrement(sim_y2 - sim_y1);
    double y_sim = getNiceStart(sim_y1, graphDelta);
    while (y_sim < sim_y2)
    {
      int y_screen = map.simToScreenY(y_sim);
      g.setColor(Color.black);
      g.drawLine(x1,y_screen,x2,y_screen);  // draw a tick mark

      // draw a number
      g.setColor(Color.gray);
      String s = nf.format(y_sim);
      int textWidth = numFM.stringWidth(s);
      g.drawString(s, x2+5, y_screen+(numFM.getAscent()/2));

      y_sim += graphDelta;  // next tick mark
    }

    // draw name of the vertical axis
    String vname = sim.getVariableName(yVar);
    int w = numFM.stringWidth(vname);
    g.drawString(vname, x0 + 6, map.simToScreenY(sim_y2) + 13);
  }

  private double getNiceIncrement(double range) {
    // choose a nice increment for the numbers on the chart
    // Given the range, find a nice increment that will give around 5 to 7
    // nice round numbers within that range.
    // First, scale the range to within 1 to 10.
    double power = Math.pow(10,Math.floor(Math.log(range)/Math.log(10)));
    double logTot = range/power;
    // logTot should be in the range from 1.0 to 9.999
    double incr;
    if (logTot >= 8)
      incr = 2;
    else if (logTot >= 5)
      incr = 1;
    else if (logTot >= 3)
      incr = 0.5;
    else if (logTot >= 2)
      incr = 0.4;
    else
      incr = 0.2;
    incr *= power;  // scale back to original range
    // setup for nice formatting of numbers in this range
    double dlog = Math.log(incr)/Math.log(10);
    int digits = (dlog < 0) ? (int)Math.ceil(-dlog) : 0;
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(0);
    return incr;
  }

  private double getNiceStart(double start, double incr) {
    // gives the first nice increment just greater than the starting number
    return Math.ceil(start/incr)*incr;
  }

  public void mousePressed(MouseEvent evt) {
  }

  public void mouseReleased(MouseEvent evt) {
  }

  public void mouseClicked(MouseEvent evt) {
    //reset();
  }

  public void mouseEntered(MouseEvent evt) {
  }

  public void mouseExited(MouseEvent evt) {
  }

}
