package com.myphysicslab.simlab;

public interface Dragable extends Drawable {
  public boolean isDragable();
  public double distanceSquared(double x, double y);
  public double getX();
  public double getY();
  public void setPosition(double x, double y);

}
