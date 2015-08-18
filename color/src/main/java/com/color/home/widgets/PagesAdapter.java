package com.color.home.widgets;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.color.home.ProgramParser.Page;
import com.color.home.ProgramParser.Program;
import com.color.home.R;

public class PagesAdapter extends BaseAdapter {
    private static final boolean DBG = false;
    private final static String TAG = "PagesAdapter";

    private Context mContext;

    private ArrayList<Page> mPages;
    private ContentResolver mContentResolver;
    private LayoutInflater mInflater;
    private final Program program;
    private AdaptedProgram mProgramView;

    public PagesAdapter(Context applicationContext, ArrayList<Page> pages, Program program, AdaptedProgram programView) {
        this.mContext = applicationContext;
        this.mPages = pages;
        this.program = program;
        mProgramView = programView;

        mInflater = LayoutInflater.from(applicationContext);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public Object getItem(int position) {
        return mPages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (DBG) {
            Log.i(TAG, "getView. position= " + position);
            // new Exception().printStackTrace();
        }

        PageView pageView = (PageView) mInflater.inflate(R.layout.layout_page, null);
        pageView.setProgram(program, mProgramView);
        Page page = (Page) getItem(position);
        pageView.setPageAndPos(page, position);

        if (DBG)
            Log.d(TAG, "getView. [2");
        
        return pageView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
