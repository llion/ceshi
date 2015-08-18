package com.color.home.widgets;

public interface OnPlayFinishObserverable {
    public abstract void setListener(OnPlayFinishedListener listener);
    public abstract void removeListener(OnPlayFinishedListener listener);
}