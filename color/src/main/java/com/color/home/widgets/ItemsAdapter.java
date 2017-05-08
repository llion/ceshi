package com.color.home.widgets;

import android.content.ContentResolver;
import android.content.Context;
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.net.Uri;
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
import com.color.home.widgets.clt_json.ItemMLScrollCltJsonView;
import com.color.home.widgets.clt_json.ItemSLScrollCltJsonView;
import com.color.home.widgets.clt_json.ItemSLPagedCltJsonView;
import com.color.home.widgets.clt_json.SLScrollCltJsonHeadTailObject;
import com.color.home.widgets.clt_json.SLScrollCltJsonObject;
import com.color.home.widgets.externalvideo.ItemExternalVideoView;
import com.color.home.widgets.multilines.ItemMLScrollMultipic2View;
import com.color.home.widgets.multilines.ItemMultiLinesMultipic;
import com.color.home.widgets.multilines.ItemMultiLinesPagedText;
import com.color.home.widgets.clt_json.ItemMLPagedCltJsonView;
import com.color.home.widgets.singleline.ItemSingleLineText;
import com.color.home.widgets.singleline.PCItemSingleLineText;
import com.color.home.widgets.singleline.SLPCHTSurfaceView;
import com.color.home.widgets.singleline.localscroll.SLTextSurfaceView;
import com.color.home.widgets.singleline.localscroll.TextObject;
import com.color.home.widgets.singleline.localscroll.TextObjectHeadTail;
import com.color.home.widgets.singleline.pcscroll.SLPCSurfaceView;

import com.color.home.widgets.singleline_scroll.ScrollRSSSurfaceview;

import com.color.home.widgets.sync_playing.ItemSyncImageView;
import com.color.home.widgets.sync_playing.ItemTextureVideoView;

import com.color.home.widgets.timer.ItemTimer;
import com.color.home.widgets.weather.ItemWeatherMLPagesView;
import com.color.home.widgets.weather.ItemWeatherMLScrollView;
import com.color.home.widgets.weather.ItemWeatherSLPagesView;
import com.color.home.widgets.weather.ItemWeatherSLScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import okhttp3.HttpUrl;

public class ItemsAdapter extends BaseAdapter {
    private static final boolean DBG = false;
    private final static String TAG = "ItemsAdapter";
    Random mRand = new Random();

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
            new Exception("test").printStackTrace();
        }

        Item item = (Item) getItem(position);
        if (DBG)
            Log.d(TAG, "adapter:.........   item.type=" + item.type);
        try {
            // Video
            if ("3".equals(item.type)) {

                if (mRegionView.isSync()) {
                    ItemTextureVideoView syncView = new ItemTextureVideoView(mContext);
                    syncView.setRegion(mRegion);
                    syncView.setItem(mRegionView, item);
                    syncView.setLoop(getCount() == 1);

                    return syncView;

                } else {
                    ItemVideoView vv = new ItemVideoView(mContext);
                    vv.setRegion(mRegion);
                    vv.setItem(mRegionView, item);

                    // If I'm the only one, always loop.
                    vv.setLoop(getCount() == 1);
                    return vv;
                }
            } else if ("7".equals(item.type)) { // clock

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

            } else if ("8".equals(item.type)) { // external video
                ItemExternalVideoView ievv = new ItemExternalVideoView(mContext);
                ievv.setRegion(mRegion);
                ievv.setItem(mRegionView, item);
                return ievv;

            } else if ("9".equals(item.type)) { // NormalClock
                if ("0".equals(item.isAnalog)) {
                    ItemTextClock itc = new ItemTextClock(mContext);
                    itc.setRegion(mRegion);
                    itc.setItem(mRegionView, item);
                    return itc;
                } else {
                    if (DBG)
                        Log.i(TAG, "item = " + item + ", item.anologClock = " + item.anologClock);
                    // Currently, use the Item Quaz.
                    return genItemQuazAnalogClock(item);
                }
            } else if ("15".equals(item.type)) { // Timer
                ItemTimer itemTimer = new ItemTimer(mContext);
                itemTimer.setRegion(mRegion);
                itemTimer.setItem(mRegionView, item);
                return itemTimer;

            } else if ("2".equals(item.type)) { // Image

                if (mRegionView.isSync()){
                    if (DBG)
                        Log.d(TAG, "convertView==" + convertView);

                    ItemSyncImageView iiv;
                    if (convertView != null) {
                        iiv = (ItemSyncImageView) convertView;
                        if (DBG)
                            Log.d(TAG, "convertView != null && convertView instanceof ItemImageView && " +
                                    "!(convertView instanceof  SwitchableImageView)");
                    } else {
                        iiv = new ItemSyncImageView(mContext);
                        iiv.setRegion(mRegion);
                        if (DBG)
                            Log.d(TAG, "new ItemSyncImageView(mContext)");
                    }
                    iiv.setItem(mRegionView, item);
                    return iiv;

                } else {
                    if (DBG)
                        Log.d(TAG, "convertView==" + convertView);

                    // if (convertView != null && convertView.getContext().toString())
//                    if (DBG)
                    ItemImageView iiv;
                    if (convertView != null) {
                        iiv = (ItemImageView) convertView;
                        if (DBG)
                            Log.d(TAG, "convertView != null && convertView instanceof ItemImageView && " +
                                    "!(convertView instanceof  SwitchableImageView)");
                    } else {
                        iiv = new ItemImageView(mContext);
                        iiv.setRegion(mRegion);
                        if (DBG)
                            Log.d(TAG, "new ItemImageView(mContext)");
                    }
                    iiv.setItem(mRegionView, item);
                    return iiv;
                }

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

                        //clt_json
                        if (Texts.isValidCltJsonText(Texts.getText(item))){

                            if ("1".equals(item.isheadconnecttail)) {
                                ItemSLScrollCltJsonView view = new ItemSLScrollCltJsonView(mContext, new SLScrollCltJsonHeadTailObject(mContext, item));
                                view.setItem(mRegionView, item);
                                if (DBG)
                                    Log.d(TAG, "getView. [view=" + view);

                                return view;

                            } else {
                                ItemSLScrollCltJsonView view = new ItemSLScrollCltJsonView(mContext, new SLScrollCltJsonObject(mContext, item));
                                view.setItem(mRegionView, item);
                                if (DBG)
                                    Log.d(TAG, "getView. [view=" + view);

                                return view;
                            }

                        }

                        if ("1".equals(item.isheadconnecttail)) {
//                        SLHTSurfaceView view = new SLHTSurfaceView(mContext);
                            SLTextSurfaceView view = new SLTextSurfaceView(mContext, new TextObjectHeadTail(mContext, item));
                            view.setItem(mRegionView, item);

                            if (DBG)
                                Log.d(TAG, "getView. [isheadconnecttail view=" + view);
                            return view;
                        } else {
                            // ItemSingleLineText singleLineText = (ItemSingleLineText)
                            // mInflater.inflate(R.layout.layout_singleline_movingleft_text,
                            // null);

                            SLTextSurfaceView view = new SLTextSurfaceView(mContext, new TextObject(mContext, item));
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

                    if (Texts.isValidCltJsonText(Texts.getText(item))) {//clt_json
                        ItemSLPagedCltJsonView view = (ItemSLPagedCltJsonView) mInflater.inflate(R.layout.layout_singleline_clt_json_text, null);
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);

                        return view;
                    }

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

                if ("1".equals(item.isscroll)) {// multiline scroll
                    if (DBG)
                        Log.d(TAG, "getView. [isscroll multi lines=" + item.scrollpicinfo);
//                    final ScrollPicInfo scrollpicinfo = item.scrollpicinfo;
//                    if (scrollpicinfo != null && !"0".equals(scrollpicinfo.picCount) && "1".equals(scrollpicinfo.filePath.isrelative)) {

                        if (DBG)
                            Log.d(TAG, "getView. [scrollmultipic.");

                    if (("0".equals(item.sourceType) || item.scrollpicinfo == null
                            || "0".equals(item.scrollpicinfo.picCount) || item.scrollpicinfo.filePath == null
                            || "0".equals(item.scrollpicinfo.filePath.isrelative)
                            || TextUtils.isEmpty(item.scrollpicinfo.filePath.filepath))
                            && Texts.isValidCltJsonText(Texts.getText(item))){//clt_json

                        ItemMLScrollCltJsonView view = new ItemMLScrollCltJsonView(mContext);
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);

                        return view;

                    } else {

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
    //                    } else { // legacy scroll view.   one page, scroll
    //                        if (DBG)
    //                            Log.d(TAG, "getView. [legacy scroll view");
    //                        ItemMLScrollableText view = (ItemMLScrollableText) mInflater.inflate(
    //                                R.layout.layout_multilines_scrollable_text, null);
    //                        // String filePath = getAbsFilePath(item);
    //                    /*
    //                     * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
    //                     */
    //                        if (DBG)
    //                            Log.d(TAG, "getView. [view=" + view);
    //                        view.setItem(mRegionView, item);
    //                        return view;
                    }
                } else {  // not scroll
                    final MultiPicInfo multipicinfo = item.multipicinfo;
                    if (DBG)
                        Log.d(TAG, "multipicinfo= " + multipicinfo);

                        if (("0".equals(item.sourceType) || multipicinfo == null
                                || "0".equals(multipicinfo.picCount) || multipicinfo.filePath == null
                                || "0".equals(multipicinfo.filePath.isrelative))
                                && Texts.isValidCltJsonText(Texts.getText(item))) {

                            ItemMLPagedCltJsonView view = (ItemMLPagedCltJsonView) mInflater.inflate(
                                    R.layout.layout_multilines_paged_clt_json_text, null);
                            view.setItem(mRegionView, item);
                            if (DBG)
                                Log.d(TAG, "getView. [view=" + view);
                            return view;
                        }

                    // if we have multipic and it's correctly packed into relative path.
                    if (multipicinfo != null && !"0".equals(multipicinfo.picCount) && multipicinfo.filePath != null && "1".equals(multipicinfo.filePath.isrelative)) {
                        // Multi lines text?  multi page, not scroll
                        ItemMultiLinesMultipic view = new ItemMultiLinesMultipic(mContext);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
//                         String filePath = getAbsFilePath(item);
                        /*
                         * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
                         */
                        view.setItem(mRegionView, mRegion, item);
//                        int animationType = mRegionView.getmRealAnimationType();
//                        if (DBG)
//                            Log.d(TAG, "animationType = " + animationType);

                        if (DBG)
                            Log.d(TAG, "convertView==" + convertView);
//
                        return view;
//
                    } else {
                        ItemMultiLinesPagedText view = (ItemMultiLinesPagedText) mInflater.inflate(
                                R.layout.layout_multilines_paged_text, null);
                        view.setItem(mRegionView, item);
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        return view;
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
                        if (DBG)
                            Log.d(TAG, "getView. [view=" + view);
                        view.setItem(mRegionView, mRegion, item);
                        if (DBG)
                            Log.d(TAG, "convertView==" + convertView);
                        return view;
                    } else {
                        return unknowView(item);
                    }
                }
            } else if ("14".equals(item.type)) {// weather
                if ("0".equals(item.showstyle)){//一般风格
                    if ("1".equals(item.isMultiLine)){//多行
                        if ("0".equals(item.moveType)) {//翻页
                            ItemWeatherMLPagesView itemWeatherMLPagesView = new ItemWeatherMLPagesView(mContext);
                            itemWeatherMLPagesView.setItem(mRegionView, item);
                            return itemWeatherMLPagesView;

                        } else {//上移
                            ItemWeatherMLScrollView itemWeatherMLScrollView = new ItemWeatherMLScrollView(mContext);
                            itemWeatherMLScrollView.setItem(mRegionView, item);
                            return itemWeatherMLScrollView;

                        }

                    } else {//单行
                        if ("0".equals(item.moveType)) {//翻页

                            ItemWeatherSLPagesView itemWeatherSLPagesView = new ItemWeatherSLPagesView(mContext);
                            itemWeatherSLPagesView.setItem(mRegionView, item);
                            return itemWeatherSLPagesView;

                        } else {//左移

                            ItemWeatherSLScrollView itemWeatherSLScrollView = new ItemWeatherSLScrollView(mContext, item);
                            itemWeatherSLScrollView.setItem(mRegionView, item);
                            return itemWeatherSLScrollView;

                        }
                    }

                } else
                    return unknowView(item);

            } else if ("27".equals(item.type)) {// web

                if (DBG)
                    Log.d(TAG, "type = 27, url= " + item.url);
                if (item.url == null)
                    return unknowView(item);

                String url = item.url;
                if (!item.url.contains("://")) {//has no protocol header
                    if (DBG)
                        Log.d(TAG, "type == 27 has no protocol header.");
                    url = "http://" + item.url;
                }

                Uri parse = Uri.parse(url);
                String scheme = parse.getScheme();
                if (scheme.equals("rtsp")
                        || scheme.equals("udp")
                        || scheme.equals("rtp")
                        || scheme.equals("rtmp")
                        || (parse.getQueryParameter("type") != null && parse.getQueryParameter("type").equals("clt_streaming"))
                        || url.contains(".m3u8")) {

                    ItemStreamView isv = new ItemStreamView(mContext);
                    item.url = url;
                    isv.setItem(mRegionView, item);
                    return isv;
                }

                HttpUrl httpUrl = HttpUrl.parse(url);
                if (DBG) {
                    Log.d(TAG, "url= " + url);
                    if (httpUrl != null)
                        Log.d(TAG, "httpUrl.encodedPath()= " + httpUrl.encodedPath());
                }

                if (httpUrl != null
                        && (!TextUtils.isEmpty(httpUrl.encodedPath())
                        && (httpUrl.encodedPath().endsWith(".png") || httpUrl.encodedPath().endsWith(".jpg")
                        || httpUrl.encodedPath().endsWith(".jpeg") || httpUrl.encodedPath().endsWith(".gif")
                        || httpUrl.encodedPath().endsWith(".bmp")))
                        && !TextUtils.isEmpty(httpUrl.queryParameter("updateInterval"))) {

                    ItemImageView itemImageView = new ItemImageView(mContext);
                    itemImageView.setRegion(mRegion);
                    itemImageView.setItem(mRegionView, item);

                    return itemImageView;

                } else if (httpUrl != null
                        && (!TextUtils.isEmpty(httpUrl.queryParameter("type")) && httpUrl.queryParameter("type").toLowerCase().equals("rss"))) {
                    ScrollRSSSurfaceview scrollRSSSurfaceview = new ScrollRSSSurfaceview(mContext, mRegion, mRegionView);
                    scrollRSSSurfaceview.setRssItems(item, httpUrl);

                    return scrollRSSSurfaceview;
                } else {
                    ItemWebView web = (ItemWebView) mInflater.inflate(R.layout.layout_webview, null);
                    // String filePath = getAbsFilePath(item);
            /*
             * if (DBG) Log.i(TAG, "getView. [TextView file path=" + filePath);
             */
                    web.setItem(mContext, mRegionView, item);
                    return web;
                }
            } else {
                return unknowView(item);
            }
        } catch (Exception e) {
            // We catch all the exceptions.
            e.printStackTrace();
            return unknowView(item);
        }
    }

    public View unknowView(Item item) {
        TextView tv = new TextView(mContext);
        tv.setText("UC. Type(" + item.type + ")");
        tv.setTextColor(0xFF00FF00);
        return tv;
    }

    private View genItemQuazAnalogClock(Item item) {
        ItemQuazAnalogClock itemView = new ItemQuazAnalogClock(mContext, item);
        itemView.setRegion(mRegion);
        itemView.setItem(mRegionView, item);
        return itemView;
    }

    public static String getAbsFilePathByFileSource(FileSource filesource) {
        String filepath = filesource.filepath;

        String absFilePath = AppController.getPlayingRootPath() + filepath;
        if (DBG)
            Log.i(TAG, "getAbsFilePathByFileSource.filepath=" + filepath + ", absFilePath=" + absFilePath);
        return absFilePath;
    }


    public static String getZippedAbsFilePathByFileSource(FileSource filesource) {
        String filepath = filesource.filepath;

        String absFilePath = AppController.getPlayingRootPath() + filepath + ".zip";
        if (DBG)
            Log.i(TAG, "getZippedAbsFilePathByFileSource.filepath=" + filepath + ", absFilePath=" + absFilePath);
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
