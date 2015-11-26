package com.color.home;

import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.widgets.ItemsAdapter;
import com.google.common.base.CharMatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Texts {
    private final static String TAG = "Texts";
    private static final boolean DBG = false;

    /**
     * @param item
     * @return true on use bitmap, otherwise, use native font.
     */
    public static boolean shouldUseBitmapFromPC(Item item) {
        // TODO: HMH check the native fonts rendering issue for: singleline-headtail-margin.
        // currently, it has some extra vertical lines when scrolling.
        if ("1".equals(item.isscroll)) {
            final ScrollPicInfo scrollpicinfo = item.scrollpicinfo;
            if (scrollpicinfo != null && !"0".equals(scrollpicinfo.picCount) && "1".equals(scrollpicinfo.filePath.isrelative)
                    && (new File(ItemsAdapter.getAbsFilePathByFileSource(scrollpicinfo.filePath)).exists() || new File(ItemsAdapter.getZippedAbsFilePathByFileSource(scrollpicinfo.filePath)).exists())
                    && item.logfont != null
                    && !isFontLocallyAvailable(item)
            ) {
                // Currently, the SONG, FangSong, Kai, LiShu, Hei, are handled with native fonts.
                if (DBG)
                    Log.d(TAG, "usingBitmapFromPC. [Should usingBitmapFromPC. typeface=" + item.logfont.lfFaceName);

                return true;
            }
        }
        
        return false;
    }

    private static boolean isFontLocallyAvailable(Item item) {
        return Constants.FONT_SONG.equals(item.logfont.lfFaceName)
                || Constants.FONT_FANGSONG.equals(item.logfont.lfFaceName)
                || Constants.FONT_HEI.equals(item.logfont.lfFaceName)
                || Constants.FONT_KAI.equals(item.logfont.lfFaceName)
                || Constants.FONT_LISHU.equals(item.logfont.lfFaceName)
//                || CharMatcher.ASCII.matchesAllOf(item.logfont.lfFaceName)
                ;

                // Check the none chinese later for the LOCAL rendering issue. There are issues
                // with the english sentence.

                // If the font name is All in ASCII, it's supposed, we have all ascii fonts natively.
                // In this case, the SONG, (KAI, LISHU, etc.) are handled by native, as well as the
                // logfont.lfFaceName is asc.
                // So, the native will not handle "幼圆", "adobe 宋体", etc., as the font names are not in pure
                // ascii.
    }

    public Texts(Item item) {
        if (shouldUseBitmapFromPC(item)) {
            return;
        }
        
        boolean shouldReadFromFile = false;
        // 4 // Single line text.
        // 5 Multi
        // if ("4".equals(item.type) || "5".equals(item.type)) {
        if ("4".equals(item.type) && "1".equals(item.isfromfile)) {
            shouldReadFromFile = true;
        } else if ("5".equals(item.type)) {
            shouldReadFromFile = true;
        }

        if (shouldReadFromFile) {
            mText = "";
            // From RTF or txt.
            if (item.filesource != null) {
                String filepath = item.filesource.filepath;

                if (!TextUtils.isEmpty(filepath) && filepath.endsWith(".txt")) {
                    // We have a file.
                    String absFilePath = AppController.getPlayingRootPath() + "/" + filepath;
                    mText = getStringFromFile(absFilePath);
                }
            }

        } else {
            mText = item.text;
        }

    }

    private String getStringFromFile(String absFilePath) {
        StringBuffer fileContent = new StringBuffer("");

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(absFilePath), AppController.sCharset));
            // reader = new BufferedReader(new InputStreamReader(new
            // FileInputStream(absFilePath), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line);
                fileContent.append("\n");
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }

        return fileContent.toString();
    }

    public String mText;
}
