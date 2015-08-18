package com.color.home.widgets;

import android.widget.Adapter;

import com.color.home.ProgramParser.Program;

public interface AdaptedProgram {

    public abstract void setAdapter(Adapter adapter);

    /**
     * Returns the flip interval, in milliseconds.
     * 
     * @return the flip interval in milliseconds
     * 
     * @see #setFlipInterval(int)
     * 
     * @attr ref android.R.styleable#AdapterViewFlipper_flipInterval
     */
    public abstract int getFlipInterval();

    /**
     * How long to wait before flipping to the next view.
     * 
     * @param flipInterval
     *            flip interval in milliseconds
     * 
     * @see #getFlipInterval()
     * 
     * @attr ref android.R.styleable#AdapterViewFlipper_flipInterval
     */
    public abstract void setFlipInterval(int flipInterval);

    /**
     * {@inheritDoc}
     */
    public abstract void showNext();

    public abstract void setProgram(Program program);

    public abstract void onAllFinished(PageView pageView);

    public abstract Adapter getAdapter();

    public abstract int getDisplayedChild();

    public abstract void setDisplayedChild(int displayedChild);

}