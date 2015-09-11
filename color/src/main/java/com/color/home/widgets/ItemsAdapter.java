package com.color.home.widgets;

import android.content.ContentResolver;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.color.home.AppController;
import com.color.home.ProgramParser.FileSource;
import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.MultiPicInfo;
import com.color.home.ProgramParser.Region;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.R;
import com.color.home.Texts;
import com.color.home.widgets.clocks.ItemQuazAnalogClock;
import com.color.home.widgets.clocks.ItemSmoothAnalogClock;
import com.color.home.widgets.clocks.ItemTextClock;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.ItemMLScrollableText;
import com.color.home.widgets.multilines.ItemMultiLinesMultipic;
import com.color.home.widgets.multilines.ItemMultiLinesPagedText;
import com.color.home.widgets.singleline.ItemSingleLineText;
import com.color.home.widgets.singleline.PCItemSingleLineText;
import com.color.home.widgets.singleline.SLPCHTSurfaceView;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;
import com.color.home.widgets.singleline.localscroll.TextObject;
import com.color.home.widgets.singleline.localscroll.TextObjectHeadTail;
import com.color.home.widgets.singleline.pcscroll.SLPCSurfaceView;
import com.color.home.widgets.weather.ItemWeatherInfo;

import java.io.File;
import java.util.ArrayList;

public class ItemsAdapter extends BaseAdapter {
    private static final boolean DBG = true;
    private final static String TAG = "ItemsAdapter";

    private Context mContext;

    private ArrayList<Item> mItems;
    private ContentResolver mContentResolver;
    private LayoutInflater mInflater;
    private final Region mRegion;
    private RegionView mRegionView;

    public ItemsAdapter(Context applicationContext, ArrayList<Item> items, Region region, RegionView regionView) {
        this.mContext = applicationContext;
        this.mItems = items;
        this.mRegion = region;
        mRegionView = regionView;
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
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
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
        }

        Item item = (Item) getItem(position);

        // Video
        if ("3".equals(item.type)) {
            ItemVideoView vv = new ItemVideoView(mContext);
            vv.setRegion(mRegion);
            vv.setItem(mRegionView, item);

            // If I'm the only one, always loop.
            vv.setLoop(getCount() == 1);
            return vv;
        } else if ("7".equals(item.type)) { // Image

            String filepath = item.filesource.filepath;
            int index = filepath.indexOf("clock") + 5;
            String clockType = null;
            if (index < 5) {
                // return default.
                clockType = "8";
            } else {
                clockType = filepath.substring(index);
            }
            if (DBG)
                Log.i(TAG, "getView. clocktype=" + clockType);

            ItemData itemData = null;
            if ("8".equals(clockType)) {
                return genItemQuazAnalogClock(item);
            } else {
                itemData = new ItemSmoothAnalogClock(mContext);
                itemData.setRegion(mRegion);
                itemData.setItem(mRegionView, item);
                return (View) itemData;
            }

        } else if ("9".equals(item.type)) { // NormalClock
            if ("0".equals(item.isAnalog)) {
                ItemTextClock itc = new ItemTextClock(mContext);
                itc.setRegion(mRegion);
                itc.setItem(mRegionView, item);
                return itc;
            } else {
                // Currently, use the Item Quaz.
                return genItemQuazAnalogClock(item);
            }
        } else if ("2".equals(item.type)) { // Image
            ItemImageView iiv = new ItemImageView(mContext);
            iiv.setRegion(mRegion);
            iiv.setItem(mRegionView, item);
            return iiv;
        } else if ("4".equals(item.type)) {// Single line text.
            if ("1".equals(item.isscroll)) {
                final ScrollPicInfo scrollpicinfo = item.scrollpicinfo;
                
                if (DBG)
                    Log.d(TAG, "getView. [isscroll single line=" + item.scrollpicinfo
                            + ", file exist=" + new File(ItemsAdapter.getAbsFilePathByFileSource(scrollpicinfo.filePath)).exists());
//                LogFont logfont = item.logfont;
//                if (logfont != null) {
//                    if (logfont.lfHeight != null) {
//                        mTextobj.setTextSize(Integer.parseInt(logfont.lfHeight));
//                    }
//
//                    if ("1".equals(logfont.lfUnderline)) {
//                        mTextobj.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
//                    }
//
//                    int style = Typeface.NORMAL;
//                    if ("1".equals(logfont.lfItalic)) {
//                        style = Typeface.ITALIC;
//                    }
//                    if ("700".equals(logfont.lfWeight)) {
//                        style |= Typeface.BOLD;
//                    }
//
//                    mTextobj.setTypeface(logfont.lfFaceName, style);
//                }
                if (Texts.shouldUseBitmapFromPC(item)) { // we intercept "song".
                    // File must also exist.
                    if (DBG)
                        Log.d(TAG, "getView. [scroll single line.");
                    if ("1".equals(item.isheadconnecttail)) {
                        if (DBG)
                            Log.d(TAG, "getView. [isheadconnecttail single line.");
                        SLPCHTSurfaceView view = new SLPCHTSurfaceView(mContext);
                        view.setItem(mRegionView, item);
                        
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        
                        return view;
                    } else {
                        // ItemSingleLineText singleLineText = (ItemSingleLineText)
                        // mInflater.inflate(R.layout.layout_singleline_movingleft_text,
                        // null);
                        SLPCSurfaceView view = new SLPCSurfaceView(mContext);
                        // String filePath = getAbsFilePath(item);
                        /*
                         * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                         */
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        return view;
                    }
                } else { // Generate my text, do not use pc img.
                    if ("1".equals(item.isheadconnecttail)) {
//                        SLHTSurfaceView view = new SLHTSurfaceView(mContext);
                        SLTextSurfaceView view = new SLTextSurfaceView(mContext, new TextObjectHeadTail(mContext));
                        view.setItem(mRegionView, item);
                        
                        if (DBG)
                            Log.d(TAG, "getView. [isheadconnecttail view=" + view);
                        return view;
                    } else {
                        // ItemSingleLineText singleLineText = (ItemSingleLineText)
                        // mInflater.inflate(R.layout.layout_singleline_movingleft_text,
                        // null);
                        
                        SLTextSurfaceView view = new SLTextSurfaceView(mContext, new TextObject(mContext));
                        // String filePath = getAbsFilePath(item);
                        /*
                         * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                         */
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        return view;
                    }
                }

            } else {
                final MultiPicInfo multipicinfo = item.multipicinfo;
                // if we have multipic and it's correctly packed into relative path.
                if (multipicinfo != null && !"0".equals(multipicinfo.picCount) && multipicinfo.filePath != null
                        && "1".equals(multipicinfo.filePath.isrelative)) {
                    PCItemSingleLineText view = new PCItemSingleLineText(mContext);
                    view.setItem(mRegionView, mRegion, item);
                    if (DBG)
                        Log.d(TAG, "PC single line paged. getView. [view=" + view);
                    return view;
                } else {
                    ItemSingleLineText view = (ItemSingleLineText) mInflater.inflate(R.layout.layout_singleline_text, null);
                    // String filePath = getAbsFilePath(item);
                    /*
                     * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                     */
                    view.setItem(mRegionView, item);
                    if (DBG)
                        Log.d(TAG, "getView. [view=" + view);
                    return view;
                }
            }

        } else if ("5".equals(item.type)) {// Multi lines text.

            if ("1".equals(item.isscroll)) {
                if (DBG)
                    Log.d(TAG, "getView. [isscroll multi lines=" + item.scrollpicinfo);
                final ScrollPicInfo scrollpicinfo = item.scrollpicinfo;
                if (scrollpicinfo != null && !"0".equals(scrollpicinfo.picCount) && "1".equals(scrollpicinfo.filePath.isrelative)) {
                    if (DBG)
                        Log.d(TAG, "getView. [scrollmultipic.");
                    ItemMLScrollMultipic2View view = new ItemMLScrollMultipic2View(mContext);
                    // String filePath = getAbsFilePath(item);
                    /*
                     * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                     */
                    view.setItem(mRegionView, item);
                    if (DBG)
                        Log.d(TAG, "getView. [view=" + view);
                    
                    return view;
                } else { // legacy scroll view.
                    if (DBG)
                        Log.d(TAG, "getView. [legacy scroll view");
                    ItemMLScrollableText view = (ItemMLScrollableText) mInflater.inflate(
                            R.layout.layout_multilines_scrollable_text, null);
                    // String filePath = getAbsFilePath(item);
                    /*
                     * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                     */
                    if (DBG)
                        Log.d(TAG, "getView. [view=" + view);
                    view.setItem(mRegionView, item);
                    return view;
                }
            } else {
                final MultiPicInfo multipicinfo = item.multipicinfo;
                // if we have multipic and it's correctly packed into relative path.
                if (multipicinfo != null && !"0".equals(multipicinfo.picCount) && multipicinfo.filePath != null && "1".equals(multipicinfo.filePath.isrelative)) {
                    // It's scrolling txt.
                    if ("1".equals(item.isscroll)) {
                        ItemMLScrollMultipic2View view = new ItemMLScrollMultipic2View(mContext);
                        // String filePath = getAbsFilePath(item);
                        /*
                         * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                         */
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        return view;
                    } else { // Multi lines text?
                        ItemMultiLinesMultipic view = new ItemMultiLinesMultipic(mContext);
                        // String filePath = getAbsFilePath(item);
                        /*
                         * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                         */
                        view.setItem(mRegionView, mRegion, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        return view;
                    }
                } else {
                    if ("1".equals(item.isscroll)) {
                        ItemMLScrollableText view = (ItemMLScrollableText) mInflater.inflate(
                                R.layout.layout_multilines_scrollable_text, null);
                        // String filePath = getAbsFilePath(item);
                        /*
                         * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                         */
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        
                        return view;
                    } else {
                        ItemMultiLinesPagedText view = (ItemMultiLinesPagedText) mInflater.inflate(
                                R.layout.layout_multilines_paged_text, null);
                        view.setItem(mRegionView, mRegion, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        return view;
                    }
                }
            }
        } else if ("11".equals(item.type) || "12".equals(item.type) || "13".equals(item.type)) {

            if ("1".equals(item.isscroll)) {
                if (DBG)
                    Log.d(TAG, "getView. [isscroll multi lines=" + item.scrollpicinfo + ", item type=" + item.type);
                final ScrollPicInfo scrollpicinfo = item.scrollpicinfo;
                if (scrollpicinfo != null && !"0".equals(scrollpicinfo.picCount) && scrollpicinfo.filePath != null && "1".equals(scrollpicinfo.filePath.isrelative)) {
                    if (DBG)
                        Log.d(TAG, "getView. [scrollmultipic.");
                    ItemMLScrollMultipic2View view = new ItemMLScrollMultipic2View(mContext);
                    // String filePath = getAbsFilePath(item);
                    /*
                     * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                     */
                    view.setItem(mRegionView, item);
                    if (DBG)
                        Log.d(TAG, "getView. [view=" + view);
                    
                    return view;
                } else { // legacy scroll view.
                    if (DBG)
                        Log.d(TAG, "getView. [Scroll view, but scrollpicinfo=" + scrollpicinfo);
                    return unknowView(item);
                }
            } else { // doc, excel, etc. not scrolling?
                final MultiPicInfo multipicinfo = item.multipicinfo;
                if (multipicinfo != null && !"0".equals(multipicinfo.picCount)) {
                    ItemMultiLinesMultipic view = new ItemMultiLinesMultipic(mContext);
                    // String filePath = getAbsFilePath(item);
                    /*
                     * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                     */
                    view.setItem(mRegionView, mRegion, item);
                    if (DBG)
                        Log.d(TAG, "getView. [view=" + view);
                    return view;
                } else {
                    return unknowView(item);
                }
            }
        } else if ("14".equals(item.type)) {// web
            ItemWeatherInfo weather = (ItemWeatherInfo) mInflater.inflate(R.layout.layout_weather_info, null);
            // String filePath = getAbsFilePath(item);
            /*
             * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
             */
            weather.setRegion(mRegion);
            weather.setItem(mRegionView, item);
            return weather;
        } else if ("27".equals(item.type)) {// web
            ItemWebView web = (ItemWebView) mInflater.inflate(R.layout.layout_webview, null);
            // String filePath = getAbsFilePath(item);
            /*
             * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
             */
            web.setItem(mRegionView, item);
            return web;
        } else {
            return unknowView(item);
        }

    }

    public View unknowView(Item item) {
        TextView tv = new TextView(mContext);
        tv.setText("UC. Type(" + item.type + ")");
        tv.setTextColor(0xFF00FF00);
        return tv;
    }

    public View genItemQuazAnalogClock(Item item) {
        ItemData itemData;
        itemData = new ItemQuazAnalogClock(mContext);
        itemData.setRegion(mRegion);
        itemData.setItem(mRegionView, item);
        return (View) itemData;
    }

    public static String getAbsFilePath(Item item) {
        FileSource filesource = item.filesource;
        String absFilePath = getAbsFilePathByFileSource(filesource);
        return absFilePath;
    }

    public static String getAbsFilePathByFileSource(FileSource filesource) {
        String filepath = filesource.filepath;

        String absFilePath = AppController.getPlayingRootPath() + filepath;
        if (DBG)
            Log.i(TAG, "getAbsFilePathByFileSource.filepath=" + filepath + ", absFilePath=" + absFilePath);
        return absFilePath;
    }

    @Override
    public int getItemViewType(int position) {
        Item item = (Item) getItem(position);
        if (DBG)
            Log.i(TAG, "getItemViewType. type=" + item.type);
        return Integer.parseInt(item.type);
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
