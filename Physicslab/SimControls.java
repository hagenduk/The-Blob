/*
  File: SimControls.java

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
import java.text.NumberFormat;
import javax.swing.*;

/////////////////////////////////////////////////////////////////////////////////
// A scrollbar where you can set the preferred width and height
class MyScrollbar extends JScrollBar {
  int w,h;
  public MyScrollbar(int w, int h, int orient, int value, int vis, int min, int max) {
    // new Scrollbar(orientation, value, visibleAmount, minimum, maximum)
    super(orient, value, vis, min, max);
    this.w = w;
    this.h = h;
  }

  public Dimension getPreferredSize() {
    return new Dimension(w,h);
  }
}


/////////////////////////////////////////////////////////////////////////////////
// A Label where the preferred width and height is determined by the text & font.
// Otherwise, the Label winds up being larger than necessary.
class MyLabel extends JLabel {
  String sample = null;  // sample text is used to figure max width of text

  public MyLabel(String text) {
    super(text);
  }

  public MyLabel(String text, int alignment) {
    super(text, alignment);
  }

  public MyLabel(String text, int alignment, String sample) {
    super(text, alignment);
    this.sample = sample;
  }

  public Dimension getPreferredSize() {
    Font myFont = new Font("SansSerif", Font.PLAIN, 12);
    this.setFont(myFont);
    FontMetrics myFM = this.getFontMetrics(myFont);
    int w,h;
    String txt;
    if (sample == null)
      txt = this.getText();
    else
      txt = sample;
    w = 5+myFM.stringWidth(txt); // use sample to figure text width
    h = myFM.getAscent() + myFM.getDescent();
    return new Dimension(w,h);
  }
}

/////////////////////////////////////////////////////////////////////////////////
// A checkbox that is also a parameter Observer.
class MyCheckbox extends JCheckBox implements ItemListener, Observer {
  private double value;
  private Subject subj;
  private String name;

  public MyCheckbox(Subject subj, String name) {
    super(name);
    this.subj = subj;
    this.name = name;
    this.value = subj.getParameter(name);
    setSelected(this.value != 0);
    addItemListener(this);
  }

  public String toString() {
    return "MyCheckbox \""+name+"\" value="+value;
  }

  public void update(Subject subj, String param, double value) {
    if (param.equalsIgnoreCase(name) && value != this.value) {
      this.value = value;
      setSelected(this.value != 0);
    }
  }

  public void itemStateChanged(ItemEvent event) {
    ItemSelectable isl = event.getItemSelectable();
    if (isl == this) {
      value = (null!=getSelectedObjects()) ? 1 : 0;
      subj.setParameter(name, value);
    }
  }
}

/////////////////////////////////////////////////////////////////////////////////
/* A slider consists of a label, a scrollbar, and a numeric display of the value.
 NOTE ON LIGHTWEIGHT CONTAINERS:
 Very important that this is lightweight (inherits from Container), not heavyweight
 (inherits from Panel or Canvas).  When running under the browser, if this is
 heavyweight, then there are LONG DELAYS when this is added back to the applet
 after having been previously removed (i.e. clicking the 'show controls' checkbox
 a couple of times).  If this is lightweight, then there is no delay.
 */
class MySlider extends JComponent implements AdjustmentListener, Observer {
  private double min, delta, value;
  private MyScrollbar scroll;
  private MyLabel nameLabel;
  private MyLabel myNumber;
  private NumberFormat nf;
  private Subject subj;
  private String name;

  public MySlider(Subject subj, String name,
      double min, double max, int increments, int digits) {
    this.subj = subj;
    this.name = name;
    this.min = min;
    this.value = subj.getParameter(name);
    delta = (max - min)/increments;
    nameLabel = new MyLabel(name, SwingConstants.CENTER);
    add(nameLabel);
    // new MyScrollbar(width, height, orientation, value, visibleAmount, minimum, maximum)
    scroll = new MyScrollbar(75, 15, Scrollbar.HORIZONTAL, (int)(0.5+(value-min)/delta),
      10, 0, increments+10);
    add(scroll);
    scroll.addAdjustmentListener(this);
    nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(digits);
    myNumber = new MyLabel(nf.format(value), SwingConstants.LEFT, "88.88");
    add(myNumber);
    setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
  }

  public String toString() {
    return "MySlider \""+name+"\" value="+nf.format(value);
  }

  public void update(Subject subj, String param, double value) {
    if (param.equalsIgnoreCase(name) && value != this.value) {
      this.value = value;
      myNumber.setText(nf.format(value));
      // note that the scroll can only reach certain discrete values,
      // so its positioning will only approximate this.value
      scroll.setValue((int)(0.5+(value-min)/delta));
    }
  }

  // MySlider is AdjustmentListener for the scrollbar, so this function is called when
  // the scrollbar is modified by the user.
  public void adjustmentValueChanged(AdjustmentEvent e) {
    if (e.getAdjustable() == scroll) {
      value = min + (double)scroll.getValue()*delta;
      myNumber.setText(nf.format(value));  // update the text as a side effect
      if (subj != null)
        subj.setParameter(name, value);
    }
  }

  /*
  public Insets getInsets() {
    return new Insets(1,1,1,1);
  }*/
  /*  // keep this around for understanding how panel layout works!
  14. Extensions of Swing components which have UI delegates (including JPanel), should
typically invoke super.paintComponent() within their own paintComponent() implementation.
Since the UI delegate will take responsibility for clearing the background on opaque
components, this will take care of #5.

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Rectangle r = getBounds();
    Rectangle c = g.getClipBounds();
    System.out.println("Panel.paint "+r.x+" "+ r.y+" "+r.width+" "+r.height);
    if (c != null)
      System.out.println(" clip bounds "+c.x+" "+ c.y+" "+c.width+" "+c.height);
    Insets i = getInsets();
    System.out.println("  insets "+i.top+" "+i.bottom+" "+i.right+" "+i.left);
    FlowLayout lm = (FlowLayout)getLayout();
    System.out.println("  layout hgap vgap "+lm.getHgap()+" "+lm.getVgap());
    g.setColor(Color.yellow);
    // NOTE: drawing takes place in local (panel) coordinates!
    g.fillRect(0, 0, r.width-1, r.height-1);
  }
  */
}

///////////////////////////////////////////////////////////////////////////
// SimLine is used as a graphic component to visually divide up groups of
// buttons or other controls.  The actual width is set by the layout manager.
class SimLine extends JComponent {
  public void paint (Graphics g) {
    Dimension size = getSize();
    g.setColor(Color.gray);
    g.fillRect(0, 0, size.width, size.height);
  }

  public Dimension getPreferredSize() {
    return new Dimension(1000, 1);
  }
}

///////////////////////////////////////////////////////////////////////////
/* DoubleField is a labeled editable text field for a double number.
   Each instance creates its own listener (NumberValidator), which responds
   to events like changing the keyboard focus, or hitting the enter key.
   The listener then updates the double value (if valid, otherwise it
   restores the old value).
*/
class DoubleField extends JComponent implements Observer {
  private double value;
  private JTextField field;
  private MyLabel nameLabel;
  private NumberFormat nf;
  private NumberValidator validator;
  private Subject subj;
  private String name;

  public DoubleField(Subject subj, String name, double value,
          int digits, int columns) {
    setLayout(new BorderLayout(1, 1));
    this.subj = subj;
    this.value = value;
    this.name = name;
    nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(digits);
    nf.setMinimumFractionDigits(digits);
    nameLabel = new MyLabel(name, SwingConstants.CENTER);
    add(nameLabel, BorderLayout.WEST);
    field = new JTextField(nf.format(value),columns);
    add(field, BorderLayout.EAST);
    validator = new NumberValidator(this);
    field.addActionListener(validator);
    field.addFocusListener(validator);
  }

  public DoubleField(Subject subj, String name, int digits) {
    this(subj, name, subj.getParameter(name), digits, 4);
  }

  public DoubleField(Subject subj, String name, double value, int digits) {
    this(subj, name, value, digits, 4);
  }

  public String toString() {
    return "DoubleField \""+name+"\" value="+nf.format(value);
  }

  public void update(Subject subj, String param, double value) {
    if (param.equalsIgnoreCase(this.name) && this.value != value) {
      this.value = value;
      field.setText(nf.format(value));
    }
  }

  private void revert() {
    field.setText(nf.format(value));
  }

  // this should only be called from our NumberValidator inner class
  private void modifyValue(double value) {
    this.value = value;
    field.setText(nf.format(value));
    if (subj != null) {
      subj.setParameter(name, value);
    }
  }

  protected class NumberValidator extends FocusAdapter implements ActionListener {
    DoubleField dblField;

    protected NumberValidator(DoubleField dblField) {
      super();
      this.dblField = dblField;
    }

    public void actionPerformed(ActionEvent event) {
      validate((JTextField)event.getSource());
    }
    public void focusGained(FocusEvent event) {
      JTextField tf = (JTextField)event.getSource();
      tf.selectAll();
    }
    public void focusLost(FocusEvent event) {
      JTextField tf = (JTextField)event.getSource();
      validate(tf);
      tf.select(0, 0);
    }
    private void validate(JTextField field) {
      try {
        // WARNING: do not use Double.parseDouble which is Java 1.2
        double value = (new Double(field.getText())).doubleValue();
        if (value != dblField.value)
          dblField.modifyValue(value);
      }
      catch(NumberFormatException e) {
        dblField.revert();
      }
    }
  }
}


//////////////////////////////////////////////////////////////////////////////////////
// MyChoice is a pop-up menu (Choice) which is also an Observer, so it can
// synchronize its value with a parameter in its Subject.

class MyChoice extends JComboBox implements ItemListener, Observer {
  private double value;
  private double min;
  private String name;
  private Subject subj;

  public MyChoice(Subject subj, String name, double value, double min, Object[] choices) {
    this.subj = subj;
    this.name = name;
    this.value = value;
    this.min = min;
    int index = (int)(value - min);
    if (index < 0 || index >= choices.length)
      throw new IllegalArgumentException("Value="+value+" but must be in range "+min+
            " to "+(min+choices.length-1));
    for (int i=0; i<choices.length; i++)
      addItem(choices[i].toString());
    setSelectedIndex(index);
    addItemListener(this);
  }
  
  public void itemStateChanged(ItemEvent e) {
    value = min + (double)getSelectedIndex();
    if (subj != null)
      subj.setParameter(name, value);
  }

  public void update(Subject subj, String param, double value) {
    if (param.equalsIgnoreCase(name) && value != this.value) {
      int index = (int)(Math.floor(value) - min);
      if (index < 0 || index >= this.getItemCount())
        throw new IllegalArgumentException("Value="+value+" but must be in range "+min+
              " to "+(min+getItemCount()-1));
      this.value = Math.floor(value);
      setSelectedIndex((int)(this.value - this.min));
    }
  }
}

