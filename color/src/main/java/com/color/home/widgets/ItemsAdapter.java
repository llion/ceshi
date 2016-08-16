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
import com.color.home.widgets.timer.ItemTimer;
import com.color.home.widgets.weather.ItemWeatherInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

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
               // int animationType = Integer.parseInt(item.ineffect.Type);
                int animationType = mRegionView.getmRealAnimationType();
                if (DBG)
                    Log.d(TAG, "animationType = " + animationType);
//                if (animationType == 1) {
//                    // type = mRand.nextInt((48 - 2) + 1) + 2;
//                    animationType = stypes[mRand.nextInt(stypes.length)];
//                    if (DBG) {
//                        Log.d(TAG, "animationType======" + animationType);
//                    }
//                }
                if (DBG)
                    Log.d(TAG,"convertView==" + convertView );
                if (animationType == 2 || animationType == 3 || animationType == 4 || animationType == 5 || animationType == 6
                        || animationType == 7 || animationType == 8 || animationType == 9 || animationType == 10
                        || animationType == 11 ||animationType == 12 ||animationType == 13 || animationType == 14
                        || animationType == 15 || animationType == 16 || animationType == 17 || animationType == 18
                        || animationType == 19 || animationType == 28 || animationType == 29 || animationType == 30
                        || animationType == 32 || animationType == 33 || animationType == 34 || animationType == 35
                        || animationType == 36 || animationType == 37 || animationType == 43 || animationType == 44
                        || animationType == 45 || animationType == 46 || animationType == 47 || animationType == 48) { //覆盖或百叶窗或马赛克或闭合或对开

                    SwitchableImageView siv;
                    if (convertView != null && convertView instanceof  SwitchableImageView) {
                        siv = (SwitchableImageView) convertView;
                        if (DBG)
                            Log.d(TAG,"convertView != null && convertView instanceof  SwitchableImageView");
                    } else {
                        siv = new SwitchableImageView(mContext);
                        siv.setRegion(mRegion);
                        if (DBG)
                            Log.d(TAG,"new SwitchableImageView(mContext)");
                    }
                    siv.setItem(mRegionView, item);

                    if (animationType == 2)//左覆盖
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_COVER_STYLE);
                    else if (animationType == 3)//右覆盖
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.RIGHT_COVER_STYLE);
                    else if (animationType == 4)//上覆盖
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.OVER_COVER_STYLE);
                    else if (animationType == 5)//下覆盖
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.UNDER_COVER_STYLE);
                    else if (animationType == 6)//左上覆盖--斜线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_TOP_DIAGONAL_STYLE);
                    else if (animationType == 7)//右上覆盖--斜线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.RIGHT_TOP_DIAGONAL_STYLE);
                    else if (animationType == 8)//左下覆盖--斜线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_BOTTOM_DIAGONAL_STYLE);
                    else if (animationType == 9)//右下覆盖--斜线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.RIGHT_BOTTOM_DIAGONAL_STYLE);
                    else if (animationType == 10)//左上角覆盖--直线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_TOP_LINE_STYLE);
                    else if (animationType == 11)//右上角覆盖--直线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.RIGHT_TOP_LINE_STYLE);
                    else if (animationType == 12)//左下角覆盖--直线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_BOTTOM_LINE_STYLE);
                    else if (animationType == 13)//右下角覆盖--直线
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.RIGHT_BOTTOM_LINE_STYLE);
                    else if (animationType == 14)//水平百叶
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.HORIZONTAL_STYLE);
                    else if (animationType == 15)//垂直百叶
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.VERTICAL_STYLE);
                    else if (animationType == 16) //左右对开
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_RIGHT_OPEN_STYLE);
                    else if (animationType == 17) //上下对开
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.UP_DOWN_OPEN_STYLE);
                    else if (animationType == 18) //左右闭合
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.LEFT_RIGHT_CLOSE_STYLE);
                    else if (animationType == 19) //上下闭合
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.UP_DOWN_CLOSE_STYLE);
                    else if (animationType == 28 || animationType == 29 || animationType == 30)
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.MOZAIC_STYLE);
                    else if (animationType == 32) //右旋360
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.ALL_RIGHT_ROTA_STYLE);
                    else if (animationType == 33) //左旋360
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.ALL_LEFT_ROTA_STYLE);
                    else if (animationType == 34) //右旋180
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.HALF_RIGHT_ROTA_STYLE);
                    else if (animationType == 35) //左旋180
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.HALF_LEFT_ROTA_STYLE);
                    else if (animationType == 36) //右旋90
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.QUARTER_RIGHT_ROTA_STYLE);
                    else if (animationType == 37) //左旋90
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.QUARTER_LEFT_ROTA_STYLE);
                    else if (animationType == 43) //中间向四周--矩形
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.CENTER_AROUND_RECT_STYLE);
                    else if (animationType == 44) //四周向中间--矩形
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.AROUND_CENTER_RECT_STYLE);
                    else if (animationType == 45) //中间向四周--菱形
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.DIAMOND_CENTER_OUTER_STYLE);
                    else if (animationType == 46) //四周向中间--菱形
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.DIAMOND_AROUND_CENTER_STYLE);
                    else if (animationType == 47) //中间向四周--十字
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.CENTER_AROUND_CROSS_STYLE);
                    else if (animationType == 48) //四周向中间--十字
                        siv.setSwitchingStyle(SwitchableImageView.SwitchingStyle.AROUND_CENTER_CROSS_STYLE);

                    return siv;
                }  else {
                   // if (convertView != null && convertView.getContext().toString())
//                    if (DBG)
                    ItemImageView iiv;
                    if (convertView != null
                            //&&
//                            convertView instanceof ItemImageView &&
//                            !(convertView instanceof  SwitchableImageView)
                            ) {
                        iiv = (ItemImageView) convertView;
                        if (DBG)
                            Log.d(TAG,"convertView != null && convertView instanceof ItemImageView && " +
                                    "!(convertView instanceof  SwitchableImageView)");
                    } else {
                        iiv = new ItemImageView(mContext);
                        iiv.setRegion(mRegion);
                        if (DBG)
                            Log.d(TAG,"new ItemImageView(mContext)");
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
                ItemWeatherInfo weather = new ItemWeatherInfo(mContext);
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
