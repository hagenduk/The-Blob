package prestudy_gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.Method;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;
public class gui implements ActionListener {
   public static void main(String[] args) {
      // create a new demo, and update it every 50 mSec
      new Timer(50, new gui()).start();
   }
   int phase = 0; // demo runs a number of consecutive phases
   int count = 0; // each of which takes a number of timesteps
   JFrame window1 = new JFrame("Java windows demo");
   JLabel text1 = new JLabel("<HTML><H1>This is a demo of some of the effects"
         + "<BR>that can be achieved with the new Java"
         + "<BR>transparent window methods</H1>"
         + "<BR>(requires latest version of Java)");
   JFrame window2 = new JFrame("Java windows demo");
   JLabel text2 = new JLabel("<HTML><center>Java<BR>rocks");
   int w, h, r, x, y; // parameters of iris circle
   gui() {
      // build and diplay the windows
      window1.add(text1);
      window1.pack();
      centerOnScreen(window1);
      window1.setVisible(true);
      window2.setUndecorated(true);
      setTransparent(window2);
      setAlpha(window2, 0.0f);
      text2.setFont(new Font("Arial", 1, 60));
      text2.setForeground(Color.red);
      window2.add(text2);
      window2.pack();
      centerOnScreen(window2);
      window2.setVisible(true);
      // parameters of the smallest circle that encloses window2
      // this is the starting pouint for the "iris out" effect
      w = window2.getWidth();
      h = window2.getHeight();
      r = (int) Math.sqrt(w * w + h * h) / 2; // radius
      x = w / 2 - r; // top left coordinates of circle
      y = h / 2 - r;
   }
   @Override
   public void actionPerformed(ActionEvent e) {
      // called by timer 20 times per sec
      // goes thru a number of phases, each a few seconds long
      switch (phase) {
      case 0: {
         // initial pause
         if (++count > 50) {
            phase = 1; // go to next phase
            count = 0;
         }
         break;
      }
      case 1: {
         // fade in
         if (++count < 100) {
            setAlpha(window2, 0.01f * count);
         } else {
            phase = 2; // go to next phase
            count = 0;
         }
         break;
      }
      case 2: {
         // move
         if (++count < 160) {
            if (count < 28 || count > 80) // pause for best effect
               window2.setLocation(window2.getX() + 1, window2.getY() + 1);
         } else {
            phase = 3; // go to next phase
            count = 0;
         }
         break;
      }
      case 3: {
         // iris out
         if (++count < r) {
            Shape shape = new Ellipse2D.Double(x + count, y + count, 2 * (r - count),
                  2 * (r - count));
            setShape(window2, shape);
         } else {
            phase = 99; // go to final (exit) phase
         }
         break;
      }
      case 99:
         System.exit(0);
      }
   }
   void centerOnScreen(JFrame window) { // convenience method
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      window.setLocation((screenSize.width - window.getWidth()) / 2,
            (screenSize.height - window.getHeight()) / 2);
   }
   // here's the magic:
   private boolean usingAwtUtilities = true;
   private Class<?> awtUtilitiesClass = null;
   private Method mSetWindowOpaque, mSetWindowOpacity, mSetWindowShape;
   void initReflection() {
      if (System.getProperty("java.version").startsWith("1.6")) {
         // Sun doc recommends accessing awtUtilities stuff via Reflection
         // (presumably to avoid access restriction rules)
         try {
            awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
            mSetWindowOpaque = awtUtilitiesClass.getMethod("setWindowOpaque",
                  Window.class, boolean.class);
            mSetWindowOpacity = awtUtilitiesClass.getMethod("setWindowOpacity",
                  Window.class, float.class);
            mSetWindowShape = awtUtilitiesClass.getMethod("setWindowShape", Window.class,
                  Shape.class);
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else { // no need to do anything for 1.7 and later
         usingAwtUtilities = false;
      }
   }
   void setTransparent(JFrame window) {
      // cover for temporary API expected to change for Java 7
      if (usingAwtUtilities && awtUtilitiesClass == null) initReflection();
      if (usingAwtUtilities) {
         try {
            mSetWindowOpaque.invoke(null, window, Boolean.valueOf(false));
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else {
         window.setBackground(new Color(0, 0, 0, 0));
         // Under 1.7 using an alpha <1 sets opaque to flase
      }
      window.getRootPane().setOpaque(false);
   }
   void setAlpha(JFrame window, float alpha) {
      // cover for temporary API expected to change for Java 7
      if (usingAwtUtilities && awtUtilitiesClass == null) initReflection();
      if (usingAwtUtilities) {
         try {
            mSetWindowOpacity.invoke(null, window, Float.valueOf(alpha));
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else {  
         // window.setOpacity(alpha);  // needs Java 1.7 to compile
      }
   }
   void setShape(JFrame window, Shape shape) {
      // cover for temporary API expected to change for Java 7
      if (usingAwtUtilities && awtUtilitiesClass == null) initReflection();
      if (usingAwtUtilities) {
         try {
            mSetWindowShape.invoke(null, window, shape);
         } catch (Exception e) {
            e.printStackTrace();
         }
      } else {
         // window.setShape(shape);   // needs Java 1.7 to compile
      }
   }
}