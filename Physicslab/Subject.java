package com.myphysicslab.simlab;

public interface Subject {
  public void attach(Observer o);
  public void detach(Observer o);
  public double getParameter(String param);
  public void setParameter(String param, double value);
}
