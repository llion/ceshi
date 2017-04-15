package com.color.home.widgets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

import com.color.home.AppController;
import com.color.home.ProgramParser;
import com.color.home.ProgramParser.BgAudio;
import com.color.home.ProgramParser.Page;
import com.color.home.ProgramParser.Program;
import com.color.home.ProgramParser.Region;
import com.color.home.ProgramParser.Item;
import com.color.home.R;
import com.color.home.widgets.ItemImageView.FilePathAndDim;

public class PageView extends AbsoluteLayout {
    private static final boolean DBG = false;
    // never public, so that another class won't be messed up.
    private final static String TAG = "PageView";

    private Page mPage;
    private ImageView mBgImage;
    private Bitmap mBitmap;
    private Program mProgram;
    private MediaPlayer mMediaPlayer;

    public PageView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public PageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public void setPageAndPos(Page page, int position) {
        this.mPage = page;

        setupPageCommon();
        setupRegions();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isSyncProgram(mProgram)){
            if (DBG)
                Log.d(TAG, "onAttachedToWindow. this is sync program, send broadcast of syncProgramStart.");
            mContext.sendStickyBroadcast(new Intent("com.clt.intent.syncProgramStart"));
        }

        if (mPage.bgaudios != null && mPage.bgaudios.size() > 0) {
            mMediaPlayer = new MediaPlayer();
            if (DBG)
                Log.i(TAG, "onAttachedToWindow. play nex audio., Thread=" + Thread.currentThread());
            play(nextAudio());
        } else {
            if (DBG)
                Log.i(TAG, "play. no audio, Thread=" + Thread.currentThread());
        }
    }

    protected void play(BgAudio nextAudio) {
        if (DBG)
            Log.i(TAG, "play. nextAudio=" + nextAudio + ", Thread=" + Thread.currentThread());

        if (nextAudio == null || nextAudio.filesource == null || !nextAudio.filesource.isValidFileSource()) {
            Log.e(TAG, "play. nextAudio, bad bgaudio file source=" + nextAudio + ", Thread=" + Thread.currentThread());
            return;
        }
        // if (mPage.bgaudios.filesource != null &&
        // !TextUtils.isEmpty(mPage.bgaudio.filesource.filepath)) {
        MediaPlayer mediaPlayer = mMediaPlayer;
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(ItemsAdapter.getAbsFilePathByFileSource(nextAudio.filesource));
            mediaPlayer.prepare();
            mediaPlayer.setLooping(mPage.bgaudios.size() == 1);
            float leftVolume;
            float rightVolume = leftVolume = Float.parseFloat(nextAudio.volume);
            mediaPlayer.setVolume(leftVolume, rightVolume);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (DBG)
                        Log.i(TAG, "onCompletion. mp=" + mp + ", Thread=" + Thread.currentThread());
                    play(nextAudio());

                }
            });
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private int mCurrentAudioIndex = 0;
    private Map<RegionView, Boolean> mPlayedMap;
    private AdaptedProgram mProgramView;

    private BgAudio nextAudio() {
        if (DBG)
            Log.i(TAG, "nextAudio. mCurrentAudioIndex=" + mCurrentAudioIndex + ", Thread=" + Thread.currentThread());
        int index = mCurrentAudioIndex;

        mCurrentAudioIndex++;
        if (mCurrentAudioIndex >= mPage.bgaudios.size()) {
            mCurrentAudioIndex = 0;
        }

        return mPage.bgaudios.get(index);
    }

    private void setupRegions() {
        if (DBG)
            Log.i(TAG, "setupRegions. this= " + this);

        List<Region> regions = mPage.regions;
        if (regions == null || regions.size() == 0) {
            if (DBG)
                Log.i(TAG, "setupRegions. mPage.regions = EMPTY.");
            return;
        }

        mPlayedMap = new HashMap<RegionView, Boolean>(regions.size());

        LayoutInflater li = LayoutInflater.from(getContext());
        for (Region region : regions) {
            if (region != null) {
                RegionView regionView;
                if (isSinglelineScrollRegion(region))
                    regionView = (SinglelineScrollRegionView) li.inflate(R.layout.layout_singleline_scroll_region, null);
                else
                    regionView = (RegionView) li.inflate(R.layout.layout_region, null);

                mPlayedMap.put(regionView, false);
                /*
                 * String type; if ("3".equals(region.type)) { regionView = new ImageRegionView(getContext()); } else { if (DBG) Log.i(TAG,
                 * "setupRegions. Unknonw region item type = " + type); }
                 */

                if (regionView != null) {
                    if (isSyncProgram(mProgram) && isSyncRegion(region))
                        regionView.setSync(true);

                    regionView.setRegion(this, region);
                    addView(regionView);
                }
            }
        }
    }

    private void setupPageCommon() {
        if (DBG)
            Log.i(TAG, "setPageCommon. ");

        mBgImage = (ImageView) findViewById(R.id.pageBg);

        String imageFilePath = mPage.bgfile == null ? null : mPage.bgfile.filepath;

        if (TextUtils.isEmpty(imageFilePath)) {
            if (DBG)
                Log.i(TAG, "setupPageCommon. no image, remove imageview., Thread=" + Thread.currentThread());
            removeView(mBgImage);
            mBgImage = null;
        } else {
            String imgPath = AppController.getPlayingRootPath() + "/" + imageFilePath;
            if (DBG)
                Log.i(TAG, "setupPageCommon. has image, imgPath=" + imgPath + ", Thread=" + Thread.currentThread());
            new HelpDataParseWorker().execute(new FilePathAndDim(imgPath, Integer.parseInt(mProgram.info.width), Integer
                    .parseInt(mProgram.info.height)));
        }

        if (mPage.bgColor != null) {
            if (DBG)
                Log.i(TAG, "setupPageCommon. set color=" + mPage.bgColor + ",, Thread=" + Thread.currentThread());
            setBackgroundColor(mPage.bgColor);
        }
    }

    private class HelpDataParseWorker extends AsyncTask<FilePathAndDim, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(FilePathAndDim... params) {
            final FilePathAndDim info = params[0];
            if (DBG)
                Log.i(TAG, "doInBackground. loading image=" + info.file);

            // Bitmap decodeFile = decodeImagePurgeOnly(file);

            Bitmap decodeFile = ItemImageView.getArtworkQuick(info.file, info.width, info.height);

            return decodeFile;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (DBG)
                Log.i(TAG, "onPostExecute. [result=" + result);

            mBitmap = result;
            mBgImage.setImageBitmap(result);
        }
    }

    public void setProgram(Program program, AdaptedProgram programView) {
        this.mProgram = program;
        // TODO Auto-generated method stub
        mProgramView = programView;

    }

    @Override
    protected void onDetachedFromWindow() {
        // TODO Auto-generated method stub
        super.onDetachedFromWindow();
        if (DBG)
            Log.i(TAG, "onDetachedFromWindow. this= " + this);

        if (isSyncProgram(mProgram)) {
            if (DBG)
                Log.d(TAG, "onDetachedFromWindow. this is sync program, send broadcast of syncProgramStop.");
            mContext.sendBroadcast(new Intent("com.clt.intent.syncProgramStop"));
        }

        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void onAllPlayed(RegionView regionView) {
        if (DBG)
            Log.i(TAG, "onAllPlayed. regionView=" + regionView);

        if (!mPlayedMap.get(regionView)) {
            mPlayedMap.put(regionView, true);
        }

        boolean isAllFinished = true;
        for (AdaptedRegion rv : mPlayedMap.keySet()) {
            if (!mPlayedMap.get(rv)) {
                isAllFinished = false;
                break;
            }
        }
        if (DBG)
            Log.i(TAG, "onAllPlayed. regionView=" + regionView + ", isAllFinished=" + isAllFinished);
        if (isAllFinished) {
            if (DBG) {
                Set<RegionView> set = mPlayedMap.keySet();
                for (RegionView regionView1 : set) {
                    Log.i(TAG, "regionview= " + regionView1);
                }
            }
            // Flipping or not depends on the program view.
            mProgramView.onAllFinished(this);
        }

        // Not flipping.
//        return false;
    }

    public static boolean isSinglelineScrollRegion(Region region) {

        if ("singleline_scroll".equals(region.name) && region.items.size() >= 2) {
            if (DBG)
                Log.d(TAG, "isSinglelineScrollRegion. region name is singleline_scroll.");

            int picNum = 0, textNum = 0;
            for (Item item : region.items) {
                if ("2".equals(item.type)) {
                    picNum++;
                } else if ("5".equals(item.type)) {
                    if ("1".equals(item.isscroll))
                        textNum++;
                } else {
                    if (DBG)
                        Log.d(TAG, "isSinglelineScrollRegion. there is other type, type= " + item.type);
                    return false;
                }

            }

            if (DBG)
                Log.d(TAG, "picNum= " + picNum + ", textNum= " + textNum);
            if (picNum > 0 && textNum > 0) {
                return true;
            }

        }

        return false;
    }

    public boolean isSyncProgram(Program program) {

        if (program != null && program.pages != null && program.pages.size() == 1){

            Page page = program.pages.get(0);
            if (page.regions != null && page.regions.size() > 0){
                for (ProgramParser.Region region : page.regions){
                    if (isSyncRegion(region)) {
                        if (DBG)
                            Log.d(TAG, "this is sync program.");
                        return true;
                    }
                }
            }

        }

        if (DBG)
            Log.d(TAG, "this is not sync program.");
        return false;
    }

    public boolean isSyncRegion(ProgramParser.Region region){

        if ("sync_program".equals(region.name)) {
            for (int i = 0; i < region.items.size(); i++) {
                if (DBG)
                    Log.d(TAG, "i= " + i + ", item type= " + region.items.get(i).type);

                if ("2".equals(region.items.get(i).type)) {
                    if (DBG)
                        Log.d(TAG, "this region is sync");
                    return true;
                }
            }
        }

        if (DBG)
            Log.d(TAG, "this region is not sync");

        return false;
    }
}
