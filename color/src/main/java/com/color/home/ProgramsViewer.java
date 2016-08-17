package com.color.home;

import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.Adapter;

import com.color.home.ProgramParser.Page;
import com.color.home.ProgramParser.Program;
import com.color.home.R.layout;
import com.color.home.widgets.PagesAdapter;
import com.color.home.widgets.ProgramView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProgramsViewer{
    private final static String TAG = "ProgramsViewer";
    private static final boolean DBG = false;

    ProgramView mProgramView;
    public List<Program> mPrograms;
    private MainActivity mMainActivity;
    private final LayoutInflater mLayoutInflater;
    private final ViewGroup mContentVG;

    public ProgramsViewer(MainActivity mainActivity) {
        mMainActivity = mainActivity;

        mLayoutInflater = mMainActivity.getLayoutInflater();
        mContentVG = mMainActivity.mContentVG;
    }

    public ViewGroup removeProgramView() {
        if (DBG)
            Log.e(TAG, "removeProgramView. mProgramView=" + mProgramView, new Exception());

        ViewGroup vg = null;
        if (mProgramView != null && mProgramView.getParent() != null) {
            vg = (ViewGroup) (mProgramView.getParent());
            vg.removeView(mProgramView);
            if (DBG)
                Log.i(TAG, "inflatePrograms. mProgramView already exist, remove from parent."
                        + "\n, vg=" + vg
                        + "\n, mContentVG=" + mContentVG);

            setProgramView(null);
        }

        mContentVG.setBackgroundResource(R.drawable.background_empty_content);
        return vg;
    }

    class HelpDataParseWorker extends AsyncTask<File, Void, List<Program>> {

        private File mVsnFile;

        @Override
        protected List<Program> doInBackground(File... params) {
            mVsnFile = params[0];
            ProgramParser pp = new ProgramParser(mVsnFile);
            InputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(mVsnFile));
                // Be aware of that he VsnSync also parse the vsn file, but it's deprecated.
                // - hmh 2016-02-25
                List<Program> parsed = pp.parse(in);

                if (DBG)
                    if (parsed != null)
                        for (ResourceCollectable prog : parsed) {
                            Log.i(TAG, "onCreate. [program=" + prog);
                        }
                return parsed;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Program> result) {
            if (result == null) {
                Log.e(TAG, "onCreate. [Program is null ignore.");
                return;
            }

            setPrograms(result);

            mMainActivity.onProgramStarted(mVsnFile);
            inflatePrograms();
        }
    }

    public void parsePrograms(File vsnFile) {
        // setPrograms(null);
        new HelpDataParseWorker().execute(vsnFile);
    }

    private void inflatePrograms() {
        if (DBG)
            Log.i(TAG, "onCreate. [inflatePrograms MainActivity =");

        LayoutInflater inflater = mLayoutInflater;


        ViewGroup vg = removeProgramView();
        setProgramView((ProgramView) inflater.inflate(layout.layout_program, null));
        if (vg != null) {
            vg.addView(mProgramView);
            // Clear bg.
            vg.setBackgroundResource(0);
        } else {
            mContentVG.addView(mProgramView);
            // Clear bg.
            mContentVG.setBackgroundResource(0);
        }

        for (Program program : getPrograms()) {
            ArrayList<Page> pages = program.pages;

            if (pages != null && pages.size() > 0) {
                mProgramView.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                        if (DBG)
                            Log.i(TAG, "onChildViewRemoved. child=" + child);
                        if (DBG) {
                            Log.i(TAG, "onChildViewRemoved. ");
                            // new
                            // Exception().printStackTrace();
                        }
                    }

                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        if (DBG)
                            Log.i(TAG, "onChildViewAdded. child=" + child);
                    }
                });

                Adapter adapter = new PagesAdapter(mMainActivity.getApplicationContext(), pages, program, mProgramView);
                mProgramView.setProgram(program);
                mProgramView.setAdapter(adapter);
            } else {
                Log.i(TAG, "inflatePrograms. Program has invalid pages.");
            }
        }
    }

    public List<Program> getPrograms() {
        return mPrograms;
    }

    public void setPrograms(List<Program> programs) {
        mPrograms = programs;
    }

    public void setProgramView(ProgramView programView) {
        mProgramView = programView;
    }

    void showNext() {
        if (getPrograms() != null && mProgramView != null)
            mProgramView.showNext();
    }

}