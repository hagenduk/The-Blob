package com.myphysicslab.simlab;

public interface MouseDragHandler {
  public void startDrag(Dragable e);
  public void finishDrag(Dragable e);
  public void constrainedSet(Dragable e, double x, double y);
}

