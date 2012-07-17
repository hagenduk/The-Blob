package com.myphysicslab.simlab;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.URL;
import java.util.Enumeration;
import java.io.*;
import java.util.Iterator;
import javax.swing.*;

class SimWindowAdapter extends WindowAdapter {
  Applet applet = null;
  public SimWindowAdapter(Applet applet) {
    this.applet = applet;
  }

  public void windowIconified(WindowEvent e) {
    Utility.println("windowIconified()");
    applet.stop();
  }

  public void windowDeiconified(WindowEvent e) {
    Utility.println("windowDeiconified()");
    applet.start();
  }

  public void windowDeactivated(WindowEvent e) {
    Utility.println("windowDeactivated()");
    applet.stop();  // disable this line when using jdb
  }

  public void windowActivated(WindowEvent e) {
    Utility.println("windowActivated()");
    applet.start();
  }

  public void windowClosing(WindowEvent e) {
    applet.stop();
    applet.destroy();
    System.exit(0);
  }
}


/** SimFrame is only used when running as an application;  it provides
  a window to run in, and the methods needed to support running
  the applet as though it were in a browser.
  */
public class SimFrame extends JFrame implements AppletStub, AppletContext {

  public SimFrame(JApplet applet)  {
    setTitle("MyPhysicsLab Simulation");
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension d = tk.getScreenSize();
    setSize(800, 500);
    setLocation(300+d.width/10, 200+d.height/10);
    applet.setStub(this);
    addWindowListener(new SimWindowAdapter(applet));
    //add(applet);
    //applet.init();
    //applet.start();
   }

  // AppletStub methods
  public boolean isActive() { return true; }
  public URL getDocumentBase() { return null; }
  public URL getCodeBase() { return null; }

  /* The getParameter() method  is only used in stand-alone application mode.
    When running under a browser, these parameters are supplied
    by the browser, from html commands such as:
        <param name="graphYVar" value="3">
    WARNING:  Best to only use this for testing.  Otherwise it can
              be hard to debug if you forget you are doing something here!
  */
  public String getParameter(String name) {
    if (name.equalsIgnoreCase("simulation"))
			return "double spring";
      //return "pendulum 2";
    else if (name.equalsIgnoreCase("showControls"))
      return null;
    else if (name.equalsIgnoreCase("showGraph"))
      return null;
    else if (name.equalsIgnoreCase("graphYVar"))
      return null;
    else if (name.equalsIgnoreCase("graphXVar"))
      return null;
    else if (name.equalsIgnoreCase("graphMode"))
      return null;
    else if (name.equalsIgnoreCase("variable0"))
      return null;
    else if (name.equalsIgnoreCase("variable1"))
      return null;
    else if (name.equalsIgnoreCase("game"))
      return null;
    else if (name.equalsIgnoreCase("path"))
      return null;
    else if (name.equalsIgnoreCase("spring stiffness"))
      return null;
		/*
    else if (name.equalsIgnoreCase("left mass"))
      return "1.57";
   */
    else
      return null;
  }
  public AppletContext getAppletContext() { return this; }
  public void appletResize(int width, int height) {}

  // AppletContext methods
  public AudioClip getAudioClip(URL url) { return null; }
  public Image getImage(URL url) { return null; }
  public Applet getApplet(String name) { return null; }
  public Enumeration getApplets() { return null; }
  public void showDocument(URL url) {}
  public void showDocument(URL url, String target) {}
  public void showStatus(String status) {}
  // setStream, getStream, getStreamKeys are for Java 1.4 compatibility
  // comment out these methods to compile with Java 1.3
  public void setStream(String key, InputStream stream) throws IOException {}
  public InputStream getStream(String key) { return null; }
  public Iterator getStreamKeys() { return null; }

}
