package com.color.home.widgets;

import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/11/24.
 */
public class MultilinePageSplitter {

    private static final boolean DBG = false;
    private final static String TAG = "MultilinePageSplitter";

    private final List<CharSequence> pages = new ArrayList<CharSequence>();
    private SpannableStringBuilder currentPage;
    private TextView textView;
    private int maxLineNumPerPage;

    public MultilinePageSplitter(int maxLineNumPerPage, TextView tv) {
        this.maxLineNumPerPage = maxLineNumPerPage;
        this.textView = tv;

    }

    public void append(String text) {
        textView.setText(text);
        Layout layout = textView.getLayout();
        if (DBG)
            Log.d(TAG, "layout= " + layout);

        if (layout != null) {
            int lineCount = textView.getLineCount();
            if (DBG)
                Log.d(TAG, "lineCount= " + lineCount + ", maxLineNumPerPage= " + maxLineNumPerPage);

            int j, length, index = 0;
            for (int i = 0; i < lineCount; i += maxLineNumPerPage) {

                currentPage = new SpannableStringBuilder();
                j = 0;
                length = 0;
                while (j < maxLineNumPerPage && (i + j) < lineCount) {
                    if (DBG)
                        Log.d(TAG, "i= " + i + ", j= " + j
                                + ", line start= " + layout.getLineStart(i + j)
                                + ", line end= " + layout.getLineEnd(i + j));
                    length += layout.getLineEnd(i + j) - layout.getLineStart(i + j);
                    if (DBG)
                        Log.d(TAG, "length= " + length);

                    j++;

                }

                if (DBG)
                    Log.d(TAG, "index= " + index);
                currentPage.append(text.substring(index, index + length));
                pages.add(currentPage);

                if (DBG)
                    Log.d(TAG, "currentPage= " + currentPage);

                index += length;

            }
        }

    }

    public List<CharSequence> getPages() {
        List<CharSequence> copyPages = new ArrayList<CharSequence>(pages);
        return copyPages;
    }

}
