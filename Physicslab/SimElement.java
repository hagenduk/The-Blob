/*
  File: SimElement.java

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

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/////////////////////////////////////////////////////////////////////////////
abstract class CElement implements Drawable {
  // class constants
  public static final int NO_DRAW = 0;
  public static final int MODE_RECT = 1;
  public static final int MODE_CIRCLE = 2;
  public static final int MODE_SPRING = 3;
  public static final int MODE_LINE = 4;
  public static final int MODE_CIRCLE_FILLED = 5;
  public static final int MODE_FILLED = 6;

  /* About bounds (m_X1, m_Y1, m_X2, m_Y2)
    It should always be true that m_X1 < m_X2 and m_Y1 < m_Y2
    Therefore m_X1, m_Y1 is either top-left or bottom-left depending on whether the
    CoordMap has INCREASE_UP or INCREASE_DOWN set.
    But this should not matter at all here... just use the ConvertMap routines
    to convert from simulation to screen coordinates.
    */
  public double m_X1, m_Y1, m_X2, m_Y2;  // bounds (simulation coords)

  /* ix & ivx are for collision checking */
  //public int ix = -1;   //index of position in array of vars
  //public int ivx = -1;  //index of velocity in array of vars
  public int    m_DrawMode; // drawing mode (rect, circle, ...)
  public double m_Mass;   // the mass in kilograms
  public Color m_Color;

  public CElement () {
    m_X1 = m_Y1 = m_X2 = m_Y2 = 0;
    m_DrawMode = 0;
    m_Mass = 0;
    m_Color = Color.black;
  }

  public CElement (double X1, double Y1, double X2, double Y2) {
    m_X1 = X1;
    m_Y1 = Y1;
    m_X2 = X2;
    m_Y2 = Y2;
    m_DrawMode = 0;
    m_Mass = 0;
    m_Color = Color.black;
  }


  /* Distance is meant to give the distance squared from the "center" of the object to the given x, y point */
  public double distanceSquared(double x, double y) {
    double dx = (m_X2 + m_X1)/2 - x;
    double dy = (m_Y2 + m_Y1)/2 - y;
    return dx*dx+dy*dy;
  }

  public void setPosition(double x, double y) {
    m_X1 = x;
    m_Y1 = y;
  }

  public void setBounds(DoubleRect r) {
    m_X1 = r.getXMin(); m_Y1 = r.getYMin();
    m_X2 = r.getXMax(); m_Y2 = r.getYMax();
  }

  public void setBounds(double x1, double y1, double x2, double y2) {
    m_X1 = x1; m_Y1 = y1; m_X2 = x2; m_Y2 = y2;
  }

  public void setX1(double p) { m_X1 = p; }

  public void setX2(double p) { m_X2 = p; }

  public void setY1(double p) { m_Y1 = p; }

  public void setY2(double p) { m_Y2 = p; }

  public abstract void draw (Graphics g, ConvertMap map) ;

  public double getCenterX() { return (m_X1 + m_X2)/2; }

  public double getCenterY() { return (m_Y1 + m_Y2)/2; }

  public double getX() { return m_X1; }

  public double getY() { return m_Y1; }

  public double getWidth() { return m_X2 - m_X1; }

  public double getHeight() { return m_Y2 - m_Y1; }

}

/////////////////////////////////////////////////////////////////////////////
class BarChart extends CElement {
  double te=0, pe=0, re=0;  // translational, potential, rotational energy
  double work=0;  // work done by damping, total energy
  private double workZero=0;  // where the zero point for work bar should be
  private Font graphFont = null;  // for numbers on the bar chart energy graph
  private double graphFactor = 10;  // determines how wide the bar chart is
  private double graphDelta = 2;  // spacing of the numbers in the bar chart
  private NumberFormat nf = NumberFormat.getNumberInstance();
  private final int LEFT_MARGIN = 10;
  private final int RIGHT_MARGIN = 0;
  private final int TOP_MARGIN = 10;
  private final int HEIGHT = 10;
  public String pes = "potential energy";
  public String tes = "translational energy";
  public String res = "rotational energy";
  public String wds = "work done by damping";
  private boolean needRescale = false;
  private Color potentialColor = Color.darkGray;
  private Color translationColor = Color.gray;
  private Color rotationColor = Color.lightGray;

  public BarChart(double X1, double Y1, double X2, double Y2) {
    super(X1, Y1, X2, Y2);
  }

  public BarChart(DoubleRect r) {
    super();
    setBounds(r);
  }

  public void draw (Graphics g, ConvertMap map) {
    int left = map.simToScreenX(m_X1);
    int width = map.simToScreenX(m_X2) - left;
    int y1 = map.simToScreenY(m_Y1);
    int y2 = map.simToScreenY(m_Y2);
    int top = Math.min(y1, y2);
    int height = Math.max(y1, y2) - top;
    int w = left + LEFT_MARGIN;
    int w2;
    int maxWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
    double total = Math.max((work<0) ? workZero : workZero+work, te + pe + re);
    if (total==0)
      return;
    rescale(total, (double)maxWidth);
    // draw a bar chart of the various energy types.
    g.setColor(potentialColor);
    g.fillRect(w, top + HEIGHT+TOP_MARGIN, w2 = (int)(0.5+pe*graphFactor), HEIGHT);
    g.setColor(rotationColor);
    w += w2;
    g.fillRect(w, top + HEIGHT+TOP_MARGIN, w2 = (int)(0.5+re*graphFactor), HEIGHT);
    g.setColor(translationColor);
    w += w2;
    g.fillRect(w, top + HEIGHT+TOP_MARGIN, w2 = (int)(0.5+te*graphFactor), HEIGHT);
    // draw bar for work done
    if (work!=0) {
      g.setColor(Color.black);
      w2 = left + LEFT_MARGIN + (int)(0.5+workZero*graphFactor);
      w = left + LEFT_MARGIN + (int)(0.5+(workZero+work)*graphFactor);
      if (work>0) {
        int d = w;
        w = w2;
        w2 = d;
      }
      g.drawRect(w, top + TOP_MARGIN, w2-w, HEIGHT);
    }
    setFont(g);
    // drawScale(Graphics g, int left, int top, double total)
    int y = drawScale(g, left + LEFT_MARGIN, top + HEIGHT+TOP_MARGIN, total);
    // draw legend:  boxes and text
    int x = left + LEFT_MARGIN;
    final boolean FILLED = true;
    final boolean OUTLINE = false;
    x = drawLegend(g, pes, potentialColor, FILLED, x, y);
    if (re > 0)
      x = drawLegend(g, res, rotationColor, FILLED, x, y);
    x = drawLegend(g, tes, translationColor, FILLED, x, y);
    if (work != 0)
      x = drawLegend(g, wds, Color.black, OUTLINE, x, y);
  }

  public void setWorkZero(double workZero) {
    this.workZero = workZero;
    needRescale = true;
  }

  private void rescale(double total, double maxWidth) {
    // if graph has gotten too big or too small, reset the scale.
    if (needRescale || total*graphFactor > maxWidth ||
        total*graphFactor < 0.2*maxWidth) {
      needRescale = false;
      if (total*graphFactor > maxWidth) // increasing
        graphFactor = 0.75*maxWidth/total;
      else  // decreasing
        graphFactor = 0.9*maxWidth/total;
      double power = Math.pow(10,Math.floor(Math.log(total)/Math.log(10)));
      double logTot = total/power;
      // logTot should be in the range from 1.0 to 9.999
      // choose a nice delta for the numbers on the chart
      if (logTot >= 8)
        graphDelta = 2;
      else if (logTot >= 5)
        graphDelta = 1;
      else if (logTot >= 3)
        graphDelta = 0.5;
      else if (logTot >= 2)
        graphDelta = 0.4;
      else
        graphDelta = 0.2;
      graphDelta *= power;
      //System.out.println("rescale "+total+" "+logTot+" "+power+" "+graphDelta);
    }
  }

  private void setFont(Graphics g) {
    if (graphFont == null)
      graphFont = new Font("SansSerif", Font.PLAIN, 10);
    g.setFont(graphFont);
  }

  // draws the numeric scale for the bar chart.
  private int drawScale(Graphics g, int left, int top, double total) {
    FontMetrics graphFM = g.getFontMetrics();
    int graphAscent = graphFM.getAscent();
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(0);
    g.setColor(Color.black);
    double scale = 0;
    do {
      int x = left + (int)(scale*graphFactor);
      g.drawLine(x, top+HEIGHT/2, x, top+HEIGHT+2);
      String s = nf.format(scale);
      int textWidth = graphFM.stringWidth(s);
      g.drawString(s, x -textWidth/2, top+HEIGHT+graphAscent+3);
      scale += graphDelta;
    } while (scale < total);
    return top+HEIGHT+graphAscent+3+graphFM.getDescent();
  }

  private int drawLegend(Graphics g, String s, Color c, boolean filled, int x, int y) {
    final int BOX = 10;
    FontMetrics graphFM = g.getFontMetrics();
    g.setColor(c);
    if (filled)
      g.fillRect(x, y, BOX, BOX);
    else
      g.drawRect(x, y, BOX, BOX);
    x += BOX + 3;
    int textWidth = graphFM.stringWidth(s);
    g.setColor(Color.black);
    g.drawString(s, x, y+graphFM.getAscent());
    x += textWidth+5;
    return x;
  }

}

/////////////////////////////////////////////////////////////////////////////
class CRect extends CElement  {
  // this rectangle can be drawn between any two points (x1,y1) and (x2,y2)
  // The points need not be in any particular relation to each other.

  public CRect(double X1, double Y1, double X2, double Y2) {
    super(X1, Y1, X2, Y2);
  }

  public CRect(DoubleRect r) {
    super(r.getXMin(), r.getYMin(), r.getXMax(), r.getYMax());
  }

  public void draw (Graphics g, ConvertMap map)  {
    int x1, y1, x2, y2;
    x1 = map.simToScreenX(m_X1);
    y1 = map.simToScreenY(m_Y1);
    x2 = map.simToScreenX(m_X2);
    y2 = map.simToScreenY(m_Y2);
    // swap if necessary to ensure that, in screen coords, x1<x2 and y1<y2
    if (x2<x1) { int d=x2; x2=x1; x1=d; }
    if (y2<y1) { int d=y2; y2=y1; y1=d; }
    g.setColor(m_Color);
    if (m_DrawMode == MODE_FILLED)
      g.fillRect(x1, y1, x2-x1-1, y2-y1-1);
    else
      g.drawRect(x1, y1, x2-x1-1, y2-y1-1);
  }

}

/////////////////////////////////////////////////////////////////////////////
class CMass extends CElement implements Dragable {
  public double m_Height;
  public double m_Width;
  public double m_Damping = 0;  // slowing from friction, viscosity, etc.
  public double m_Elasticity = 1;  // bounciness, from 0 (blob) to 1 (superball)
  public boolean m_Dragable = false;  // whether it is mouse draggable

/*
  public CMass()
  {
    super(0, 0, 1, 1);
    m_Height = 1;
    m_Width = 1;
  }
*/

  public CMass (double X1, double Y1, double width, double height, int drawMode)  {
    super(X1, Y1, X1+width, Y1+height);
    m_Height = height;
    m_Width = width;
    m_Mass = 1;
    m_DrawMode = drawMode;
    m_Dragable = true;
    m_Color = Color.red;

  }

  public boolean isDragable() { return m_Dragable; }

  public void setPosition(double Xpos, double Ypos) {
    m_X1 = Xpos;
    m_X2 = Xpos + m_Width;
    m_Y1 = Ypos;
    m_Y2 = Ypos + m_Height;

  }

  public void setWidth(double w) { m_Width = w; }
  public void setHeight(double h) { m_Height = h; }
  public void setX1(double Xpos)  {
    m_X1 = Xpos;
    m_X2 = Xpos + m_Width;
  }

  public void setY1(double Ypos)  {
    m_Y1 = Ypos;
    m_Y2 = Ypos + m_Height;
  }

  public void setCenterX(double Xpos)  {
    double w = m_Width/2;
    m_X1 = Xpos - w;
    m_X2 = Xpos + w;
  }

  public void setCenterY(double Ypos)  {
    double w = m_Width/2;
    m_Y1 = Ypos - w;
    m_Y2 = Ypos + w;
  }

  public void draw (Graphics g, ConvertMap map)  {
    int x1, y1, x2, y2;
    x1 = map.simToScreenX(m_X1);
    y1 = map.simToScreenY(m_Y1);
    x2 = map.simToScreenX(m_X2);
    y2 = map.simToScreenY(m_Y2);
    if (y2 < y1) { // adjust for INCREASE_UP mode
      int d = y2;
      y2 = y1;
      y1 = d;
    }

    g.setColor(m_Color);
    switch (m_DrawMode)
    {
    case CElement.NO_DRAW:
      break;
    case CElement.MODE_RECT:
      g.drawRect(x1, y1, x2-x1, y2-y1);
      break;
    case CElement.MODE_CIRCLE:
      g.drawOval(x1, y1, x2-x1, y2-y1);
      break;
    case CElement.MODE_CIRCLE_FILLED:
      g.fillOval(x1, y1, x2-x1, y2-y1);
      break;
    //default:
    //  throw "unknown draw mode";
    }

  }
}

/////////////////////////////////////////////////////////////////////////////
class CVector extends CElement {
  public double magnitudeX = 0;
  public double magnitudeY = 0;

  public CVector(double x, double y, double magX, double magY) {
    super();
    m_X1 = x;
    m_Y1 = y;
    magnitudeX = magX;
    magnitudeY = magY;
    m_Color = Color.red;
  }


  public CVector() {
    super();
    m_Color = Color.red;
  }

  public void draw(Graphics g, ConvertMap map) {
      int xscr = map.simToScreenX(m_X1);
      int yscr = map.simToScreenY(m_Y1);
      int xscr2 = map.simToScreenX(m_X1 + magnitudeX);
      int yscr2 = map.simToScreenY(m_Y1+magnitudeY);
      g.setColor(m_Color);
      g.drawLine(xscr, yscr, xscr2, yscr2);
  }
}

/////////////////////////////////////////////////////////////////////////////
class CSpring extends CElement {
  public double m_RestLength = 1.0; // the unstretched (slack) length in meters
  public double m_Thickness = 0.5;  // the thickness (width) of coil in meters
  public double m_SpringConst = 1.0;  // the spring constant in Newtons/meter
  public Color m_Color2 = null;  // expanded color

  public CSpring (double X1, double Y1, double restLen, double thick)  {
    // X1, and Y1 are the attachment points of the left part of spring
    // default is horizontal spring at rest
    super(X1, Y1, X1+restLen, Y1);
    m_Thickness = thick;
    m_Mass = 0;
    m_SpringConst = 1;
    m_RestLength = restLen;
    m_DrawMode = CElement.MODE_SPRING;  // default
    m_Color = Color.red;  // compressed
    m_Color2 = Color.green;
  }

  public double getStretch() {
    double x = m_X2 - m_X1;
    double y = m_Y2 - m_Y1;
    return Math.sqrt(x*x + y*y) - m_RestLength;
  }

  public double getEnergy() {
    // spring potential energy = 0.5*stiffness*(stretch^2)
    double stretch = getStretch();
    return 0.5*m_SpringConst*stretch*stretch;
  }

  public void draw (Graphics g, ConvertMap map)  {
    if (m_Color2 == null)
      m_Color2 = m_Color.brighter();
    int cycles = 3;
    double x1=m_X1;
    double x2=m_X2;
    double y1=m_Y1;
    double y2=m_Y2;
    double cos_theta, sin_theta;
    // find angle of rotation
    // note:  if x2==x1 then slope is infinite, which is a valid double
    {
      double theta=Math.atan((y2-y1)/(x2-x1));
      if (x2<x1)  // need to add Pi to theta
        theta+=Math.acos(-1);
      cos_theta=Math.cos(theta);
      sin_theta=Math.sin(theta);
    }
    double len=Math.sqrt((y2-y1)*(y2-y1)+(x2-x1)*(x2-x1));
    if (len == 0)
      return;  // draw nothing if length is zero
    if ((m_DrawMode == CElement.MODE_SPRING) && (m_SpringConst == 0))
      return;  // draw nothing if spring constant is zero
    double h = m_Thickness;
    double w = len/16;
    double x, y;
    int xscr0, yscr0;

    // set up points based on a horizontal orientation of length len,
    // and then rotate about the point x1, y1
    // regard the origin as being at x1,y1

    // move the pen to the starting point
    // first point is at (0,0)
    xscr0 = map.simToScreenX(x1);
    yscr0 = map.simToScreenY(y1);

    if (m_DrawMode == CElement.MODE_LINE)    {
      // single horizontal line
      x2=len;
      y2=0;
      x = x1+cos_theta*x2-sin_theta*y2;
      y = y1+sin_theta*x2+cos_theta*y2;
      int xscr = map.simToScreenX(x);
      int yscr = map.simToScreenY(y);

      g.setColor(m_Color);
      g.drawLine(xscr0, yscr0, xscr, yscr);
    }
    else if (m_DrawMode == CElement.MODE_SPRING)    {
      /* make a polygon object */
      int nPoints = 5+2*cycles;
      int i=0;
      // NOTE: better to allocate these arrays permanently for the object, avoid GC
      int[] xPoints = new int[nPoints];
      int[] yPoints = new int[nPoints];
      xPoints[i] = xscr0;
      yPoints[i++] = yscr0;

      /* draw pen red when compressed, green when stretched */
      if (len < m_RestLength)
        g.setColor(m_Color);  // compressed
      else
        g.setColor(m_Color2);

      // second point is at (w,0), rotated by theta
      x2=w;
      y2=0;
      x = x1+cos_theta*x2-sin_theta*y2;
      y = y1+sin_theta*x2+cos_theta*y2;
      xPoints[i] = map.simToScreenX(x);
      yPoints[i++] = map.simToScreenY(y);

      // first ramp up
      x2=2*w;
      y2=-h/2;
      x = x1+cos_theta*x2-sin_theta*y2;
      y = y1+sin_theta*x2+cos_theta*y2;
      xPoints[i] = map.simToScreenX(x);
      yPoints[i++] = map.simToScreenY(y);

      // 3 up and down cycles
      int j;
      for (j=1; j<=cycles; j++)      {
        x2=4*j*w;
        y2=h/2;
        x = x1+cos_theta*x2-sin_theta*y2;
        y = y1+sin_theta*x2+cos_theta*y2;
        xPoints[i] = map.simToScreenX(x);
        yPoints[i++] = map.simToScreenY(y);

        x2=(4*j+2)*w;
        y2=-h/2;
        x = x1+cos_theta*x2-sin_theta*y2;
        y = y1+sin_theta*x2+cos_theta*y2;
        xPoints[i] = map.simToScreenX(x);
        yPoints[i++] = map.simToScreenY(y);
      }

      // last ramp down
      x2=(3+cycles*4)*w;
      y2=0;
      x = x1+cos_theta*x2-sin_theta*y2;
      y = y1+sin_theta*x2+cos_theta*y2;
      xPoints[i] = map.simToScreenX(x);
      yPoints[i++] = map.simToScreenY(y);

      // final horizontal line
      x2=len;
      y2=0;
      x = x1+cos_theta*x2-sin_theta*y2;
      y = y1+sin_theta*x2+cos_theta*y2;
      xPoints[i] = map.simToScreenX(x);
      yPoints[i++] = map.simToScreenY(y);

      g.drawPolyline(xPoints, yPoints, nPoints);
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
class CArc extends CElement {
  // X1, Y1 are the center of the arc
  // X2, Y2 will be calculated if necessary?
  public double m_Radius;
  public double m_Angle;      // degrees
  public double m_StartAngle;  // where the curve starts from; 0 = "east"
  public double m_HeadLength = 0.2;  // length of arrowhead

  public CArc (double X1, double Y1, double r, double angle0, double angle)  {
    super(X1, Y1, 0, 0);
    m_Radius = r;
    m_StartAngle = angle0;
    m_Angle = angle;
  }

  public void draw (Graphics g, ConvertMap map)  {
    int x1, y1, x2, y2, r;
    x1 = map.simToScreenX(m_X1);
    y1 = map.simToScreenY(m_Y1);
    r = map.simToScreenScaleX(m_Radius); // assumes x & y are scaled same!

    g.setColor(Color.black);
    /* parameters to drawArc are:
      x, y (top-left corner), width, height (bounding rect)
      startAngle (degrees, 0 = 3 o'clock),
      arcAngle (degrees relative to startAngle) */
    int ang = (int)(m_Angle+0.5);
    // bug:  seems that drawArc sometimes draws a circle for
    //  angle = +1 or -1 degrees!!!
    // BUG:  this bug is still happening for small radius in driven
    // pendulum (eg. radius 0.2).  Seems to be related to small radius and
    // small angle at same time.  To really check out this bug
    // would need to set up a test program where I can interactively
    // give static inputs to g.drawArc, and try to determine where the
    // bug is happening exactly.
    if (Math.abs(ang)>1)
       g.drawArc(x1-r, y1-r, 2*r, 2*r, (int)m_StartAngle, ang);

    if ((ang != 0) && (r > 0))  {
      // arrowhead
      // find tip of arrowhead
      double x,y;
      double a0, a1, a;  // startangle & angle in radians
      a0 = Math.PI*(double)m_StartAngle/(double)180;
      a1 = Math.PI*(double)m_Angle/(double)180;
      a = -(a0 + a1);
      x = m_X1 + m_Radius*Math.cos(a);
      y = m_Y1 + m_Radius*Math.sin(a);

      double h = Math.min(m_HeadLength, 0.5*m_Radius);
      if (a1 > 0)
        h = -h;

      // find endpoint of first arrowhead, and draw it
      double xp, yp;
      xp = x + h*Math.cos(Math.PI/2 - a - Math.PI/6);
      yp = y - h*Math.sin(Math.PI/2 - a - Math.PI/6);
      x1 = map.simToScreenX(x);
      y1 = map.simToScreenY(y);
      x2 = map.simToScreenX(xp);
      y2 = map.simToScreenY(yp);
      g.drawLine(x1, y1, x2, y2);

      // find endpoint of 2nd arrowhead, and draw it
      xp = x + h*Math.cos(Math.PI/2 - a + Math.PI/6);
      yp = y - h*Math.sin(Math.PI/2 - a + Math.PI/6);
      x2 = map.simToScreenX(xp);
      y2 = map.simToScreenY(yp);
      g.drawLine(x1, y1, x2, y2);
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
class CWall extends CElement {
  public double m_Angle;   // orientation angle in degrees... 0=vertical facing right

  public CWall (double X1, double Y1, double X2, double Y2, double angle)  {
    super(X1, Y1, X2, Y2);
    m_Angle = angle;
    m_Mass = Math.exp(30);  // essentially infinite
  }

  public void draw (Graphics g, ConvertMap map)  {
    int i;
    int x1, x2, y1, y2;
    x1 = map.simToScreenX(m_X1);
    y1 = map.simToScreenY(m_Y1);
    x2 = map.simToScreenX(m_X2);
    y2 = map.simToScreenY(m_Y2);
    g.setColor(Color.black);

    if (m_Angle==0)  {
      g.drawLine(x2, y1, x2, y2);

      // 5 diagonal hatch marks
      int hatchHeight = (y2 - y1)/5;
      for (i=1; i<=5; i++)
        g.drawLine(x1, y1+(i*hatchHeight), x2, y1+((i-1)*hatchHeight));
    }
    else if (m_Angle==-90)  {
      g.drawLine(x1, y1, x1, y2);

      // 5 diagonal hatch marks
      int hatchHeight = (y2 - y1)/5;
      for (i=1; i<=5; i++)
        g.drawLine(x1, y1+((i-1)*hatchHeight), x2, y1+(i*hatchHeight));
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
class CText extends CElement  {
  public String m_text;
  private double m_num = 0;
  private int fontSize = 14;
  private boolean show_num = false;
  private Font myFont = null;
  private FontMetrics myFM = null;
  private int ascent = 20;
  private int descent = 10;
  private int leading = 5;
  private NumberFormat nf = null;
  public int line_height = 10; // WARNING: only accurate after draw()
  public boolean centered = false;

  public CText (double X1, double Y1, String t)  {
    super(X1, Y1, X1, Y1);
    m_text = t;
    m_Color = Color.gray;
  }

  public CText (String t) {
    this(0, 0, t);
    centered = true;
  }

  public void setCentered(boolean centered) {
    this.centered = centered;
  }

  public void setNumber(double n)  {
    show_num = true;
    m_num = n;
  }

  public void setText(String t) {
    m_text = t;
  }

  public void setFontSize(int size) {
    this.fontSize = size;
  }

  private void setFont(Graphics g)  {
    if (myFont != null)
      return;
    myFont = new Font("Serif", Font.PLAIN, fontSize);
    myFM = g.getFontMetrics(myFont);
    ascent = myFM.getAscent();
    descent = myFM.getDescent();
    leading = myFM.getLeading();
    nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(5);
    if (line_height != ascent+descent+leading)  {
      line_height = ascent+descent+leading;
    }
  }

  public void draw (Graphics g, ConvertMap map)  {
    int x1, y1;
    setFont(g);
    g.setFont(myFont);
    g.setColor(m_Color);
    String tx = (show_num) ? (m_text + nf.format(m_num)) : m_text;
    if (centered) {
      DoubleRect r = map.getSimBounds();
      y1 = map.simToScreenY((r.getYMax() + r.getYMin())/2);
      int w = myFM.stringWidth(tx);
      x1 = map.simToScreenX((r.getXMax()+r.getXMin())/2) - w/2;
    } else {
      x1 = map.simToScreenX(m_X1);
      y1 = map.simToScreenY(m_Y1);
    }
    g.drawString(tx, x1, y1);
  }
}

/////////////////////////////////////////////////////////////////////////////
class CGradient extends CElement  {
  private double[][] matrix;
  private double w = 3.0;
  private double h = w;

  public CGradient() {
    super(0, 0, 1, 1);
    matrix = new double[15][15];
  }

  public void setCenterX(double x) { m_X1 = x - w/2; m_X2 = m_X1 + w; }
  public void setCenterY(double y) { m_Y1 = y - h/2; m_Y2 = m_Y1 + h; }
  public double getWidth() { return w;}
  public double getHeight() { return h;}
  public double[][] getMatrix() { return matrix; }

  public void draw (Graphics g, ConvertMap map)  {
    int R = matrix.length;
    if (R==0) return;
    int C = matrix[0].length;
    if (C==0) return;
    double incX = w/R;
    double incY = h/C;
    for (int i=0; i<R; i++) {
      for (int j=0; j<C; j++) {
        // assume matrix is normalized to values between 0 and 1
        // 0 = black, 1 = white
        double x = m_X1 + incX*i - incX/2;
        double y = m_Y1 + incY*j - incY/2;
        int x1, y1, x2, y2;
        x1 = map.simToScreenX(x);
        y1 = map.simToScreenY(y);
        x2 = map.simToScreenX(x + incX);
        y2 = map.simToScreenY(y + incY);
        // swap if necessary to ensure that, in screen coords, x1<x2 and y1<y2
        if (x2<x1) { int d=x2; x2=x1; x1=d; }
        if (y2<y1) { int d=y2; y2=y1; y1=d; }
        float v = (float)matrix[i][j];
        g.setColor(new Color(v,v,v));  // may be expensive to create all these colors?
        g.fillRect(x1, y1, x2-x1, y2-y1);
      }
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
// CBitmap class

class CBitmap extends CElement
{
  private Image myImage;
  private Drawable path;
  private Container applet;
  private int offsetX = 0, offsetY = 0;

  /* creates a bitmap of same size as the screen */
  public CBitmap  (Container applet, Drawable path)
  {
    /* note: can't create the image here... because applet is zero sized at start ? */
    this.applet = applet;
    this.path = path;
  }

  public void draw (Graphics g, ConvertMap map)
  {
    Rectangle r = map.getScreenRect();
    /* compare size of image to that of the screen... if different reinitialize */
    if (myImage != null)  {
      if (myImage.getWidth(null) != r.width || myImage.getHeight(null) != r.height)  {
        System.out.println("CBitmap.draw:  changed size, flushing");
        myImage.flush();  // drop any resources
        myImage = null;
      }
    }

    if (myImage == null && r.width>0 && r.height>0)   {
      // NOTE:  for now, bitmaps are always size of the simulation area.
      // In future we could make them any desired size, though the 'screen sized'
      // would be a special mode, because we don't know the size until
      // the draw() happens (since applets are zero sized when created).
      myImage = applet.createImage(r.width, r.height);
      //System.out.println("new bitmap size "+r.width+" "+r.height);
      if (myImage == null)
        System.out.println("bitmap not created");
      else {
        /* draw the track into the bitmap */
        Graphics myGraphics = myImage.getGraphics();
        myGraphics.translate(offsetX, offsetY);
        path.draw(myGraphics, map);
        // Important to dispose of any Graphics that we cause to be created
        // See Java documentation about the method Graphicsl.dispose().
        myGraphics.dispose();
      }

    }
    g.drawImage(myImage, r.x, r.y, null);
  }

  // We want to be able to draw into the bitmap using same simulation
  // coordinates and same ConvertMap as we use for drawing into
  // the SimCanvas.  Therefore, we need to offset the topleft corner
  // of the drawing coordinates (= the Graphics origin for the bitmap).
  // This method records the desired offset.
  public void setGraphicsTopLeft(int x, int y) {
    offsetX = -x;
    offsetY = -y;
  }
}

/////////////////////////////////////////////////////////////////////////////
// CCurve class

class CCurve extends CElement
{
  //private static final int DRAW_POINTS = 201;
  public double[] m_Data = null;   // curve is defined by this data vector
  public double m_Height;
  public double m_Width;
  private int draw_points;


  public CCurve (double X1, double Y1, double width, double height, int draw_points)  {
    super(X1, Y1, X1+width, Y1+height);
    m_Height = height;
    m_Width = width;
    this.draw_points = draw_points;
  }

  public void draw (Graphics g, ConvertMap map)  {
    // Draw the curve defined by the data vector.
    // Strategy:
    // There are m_N points in the data vector
    // We want to sample the data vector at a lesser number (= DRAW_POINTS)
    // of points.  To do so, we use floating point calculations to pick
    // the x-values and then determine the nearest index to those points
    // in the data vector.
    // WARNING:  the following assumes DRAW_POINTS <= m_N  ????
    double x,y,x0,y0;
    int i,j;
    if (m_Data == null)
      return;
    x0 = 0;
    y0 = m_Data[0];
    int scrx = map.simToScreenX(m_X1+x0);
    int scry = map.simToScreenY(y0);
    int oldx, oldy;
    g.setColor(m_Color);
    for(i=1;i<draw_points;i++) {
      if (i==draw_points/2)
        g.setColor(Color.red);
      x = (m_Width*i)/(draw_points-1); // the next x-value to draw
      // calculate nearest index to this point
      j = (int)Math.floor(m_Data.length*x/m_Width);
      if (j >= m_Data.length)
        j = m_Data.length-1;
      //System.out.println("draw point "+j);
      y = m_Data[j];
      oldx = scrx;
      oldy = scry;
      scrx = map.simToScreenX(m_X1+x);
      scry = map.simToScreenY(y);
      g.drawLine(oldx, oldy, scrx, scry);
      x0 = x;
      y0 = y;
    }
  }
}
