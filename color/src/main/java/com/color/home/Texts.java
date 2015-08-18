package com.color.home;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.text.TextUtils;
import android.util.Log;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.ScrollPicInfo;
import com.color.home.widgets.ItemsAdapter;

public class Texts {
    private final static String TAG = "Texts";
    private static final boolean DBG = false;
    public static boolean shouldUseBitmapFromPC(Item item) {

        if ("1".equals(item.isscroll)) {
            final ScrollPicInfo scrollpicinfo = item.scrollpicinfo;
            if (scrollpicinfo != null && !"0".equals(scrollpicinfo.picCount) && "1".equals(scrollpicinfo.filePath.isrelative)
                    && new File(ItemsAdapter.getAbsFilePathByFileSource(scrollpicinfo.filePath)).exists()
                    && item.logfont != null && !"宋体".equals(item.logfont.lfFaceName)) {
                if (DBG)
                    Log.d(TAG, "usingBitmapFromPC. [Should usingBitmapFromPC. typeface=" + item.logfont.lfFaceName);
                return true;
            }
        }
        
        return false;
    }

    public Texts(Item item) {
        if (shouldUseBitmapFromPC(item)) {
            return;
        }
        
        boolean shouldFromFile = false;
        // 4 // Single line text.
        // 5 Multi
        // if ("4".equals(item.type) || "5".equals(item.type)) {
        if ("4".equals(item.type) && "1".equals(item.isfromfile)) {
            shouldFromFile = true;
        } else if ("5".equals(item.type)) {
            shouldFromFile = true;
        }

        if (shouldFromFile) {
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
