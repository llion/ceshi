package com.color.home.widgets;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

public class PageSplitter {

    private static final boolean DBG = false;
    private final static String TAG = "PageSplitter";

    private final int pageWidth;
    private final int pageHeight;
    private final float lineSpacingMultiplier;
    private final int lineSpacingExtra;
    private final List<CharSequence> pages = new ArrayList<CharSequence>();
    private SpannableStringBuilder currentLine = new SpannableStringBuilder();
    private SpannableStringBuilder currentPage = new SpannableStringBuilder();
    private int currentLineHeight;
    private int pageContentHeight;
    private int currentLineWidth;
    private int textLineHeight;
    private BackgroundColorSpan backgroundColorSpan;
    private int textSize;

    public PageSplitter(int pageWidth, int pageHeight, float lineSpacingMultiplier, int lineSpacingExtra, BackgroundColorSpan backgroundColorSpan, int textSize) {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.lineSpacingMultiplier = lineSpacingMultiplier;
        this.lineSpacingExtra = lineSpacingExtra;
        this.backgroundColorSpan = backgroundColorSpan;
        this.textSize = textSize;
    }

    public void append(String text, TextPaint textPaint) {
        textLineHeight = (int) Math.ceil(textPaint.getFontMetrics(null) * lineSpacingMultiplier + lineSpacingExtra);
        if (DBG)
            Log.d(TAG, "append. textLineHeight= " + textLineHeight + ", textPaint.getFontMetrics(null)= "+ textPaint.getFontMetrics(null));

        String[] paragraphs = text.split("\n", -1);
        int i;
        for (i = 0; i < paragraphs.length - 1; i++) {
            if (DBG)
                Log.d(TAG, "append. i= " + i);
            appendText(paragraphs[i], textPaint);
            appendNewLine();
        }
        appendText(paragraphs[i], textPaint);
    }

    private void appendText(String text, TextPaint textPaint) {
        if (DBG)
            Log.d(TAG, "appendText. text= " + text);
        String[] words = text.split(" ", -1);

        int i;
        for (i = 0; i < words.length - 1; i++) {
            if (DBG)
                Log.d(TAG, "appendText. i= " + i);
            appendWord(words[i] + " ", textPaint);
        }
        appendWord(words[i], textPaint);
    }

    private void appendNewLine() {
        if (DBG)
            Log.d(TAG, "appendNewLine." );
        currentLine.append("\n");
        checkForPageEnd();
        appendLineToPage(textLineHeight);
    }

    private void checkForPageEnd() {
        if (DBG)
            Log.d(TAG, "checkForPageEnd. currentPage= " + currentPage
                    + ", pageContentHeight + currentLineHeight= " + (pageContentHeight + currentLineHeight)
             + ", pageHeight= " + pageHeight);
        if (pageContentHeight + currentLineHeight > pageHeight) {
            pages.add(currentPage);
            currentPage = new SpannableStringBuilder();
            pageContentHeight = 0;
        }
    }

    private void appendWord(String appendedText, TextPaint textPaint) {

        if (DBG)
            Log.d(TAG, "appendWord. appendedText= " + appendedText);
        int textWidth = (int) Math.ceil(textPaint.measureText(appendedText));
        if (DBG)
            Log.d(TAG, "appendWord. textWidth= " + textWidth + ", currentLineWidth + textWidth= " + (currentLineWidth + textWidth)
             + ", pageWidth= " + pageWidth);

        if (currentLineWidth + textWidth >= pageWidth) {
            checkForPageEnd();
            appendLineToPage(textLineHeight);
        }
        appendTextToLine(appendedText, textPaint, textWidth);
    }

    private void appendLineToPage(int textLineHeight) {
        if (DBG)
        Log.d(TAG, "appendLineToPage. currentLine= " + currentLine + ", currentPage= " + currentPage);

        currentPage.append(currentLine);
        pageContentHeight += currentLineHeight;

        currentLine = new SpannableStringBuilder();
        currentLineHeight = textLineHeight;
        currentLineWidth = 0;
    }

    private void appendTextToLine(String appendedText, TextPaint textPaint, int textWidth) {
        if (DBG)
            Log.d(TAG, "appendTextToLine. currentLine= " + currentLine);
        currentLineHeight = Math.max(currentLineHeight, textLineHeight);
        currentLine.append(renderToSpannable(appendedText, textPaint));
        currentLineWidth += textWidth;
    }

    public List<CharSequence> getPages() {
        List<CharSequence> copyPages = new ArrayList<CharSequence>(pages);
        SpannableStringBuilder lastPage = new SpannableStringBuilder(currentPage);
        if (pageContentHeight + currentLineHeight > pageHeight) {
            copyPages.add(lastPage);
            lastPage = new SpannableStringBuilder();
        }
        lastPage.append(currentLine);
        copyPages.add(lastPage);
        return copyPages;
    }

    private SpannableString renderToSpannable(String text, TextPaint textPaint) {
        if (DBG)
            Log.d(TAG, "renderToSpannable. text= " + text);

        SpannableString spannable = new SpannableString(text);

        if (textPaint.isFakeBoldText()) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
        }

        if (DBG)
            Log.d(TAG, "renderToSpannable. spannable.length= " + spannable.length() + ", text.length= " + text.length());
        spannable.setSpan(backgroundColorSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannable;
    }
}