package com.color.home;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.color.home.android.providers.downloads.CLStorageManager;
import com.color.home.network.IpUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class Constants {
    private final static String TAG = "Constants";


    public static final String FONT_SONG = "宋体";
    public static final String FONT_KAI = "楷体";
    public static final String FONT_FANGSONG = "仿宋";
    public static final String FONT_HEI = "黑体";
    public static final String FONT_LISHU = "隶书";
    public static final String FONT_PATH = "/system/fonts/";

    private static final boolean DBG = false;
    public static final boolean HTTP_SERVER_SUPPORT = false;
    public static final String KEY_ETAG = "etag";
    public static final String KEY_CHECKINGVSN_PATH = "checking_vsnpath";

    public static final String TEST_MOBILE_DEVICE_ID = "KOT49H";

    public static final String MNT_SDCARD = "/mnt/sdcard";

    public static final String FOLDER_NET = "/mnt/sdcard/Android/data/com.color.home/files/Download";
    public static final String FOLDER_USB_0 = "/mnt/usb_storage/USB_DISK0/udisk0";
    public static final String FOLDER_SYNCED_USB = "/mnt/sdcard/Android/data/com.color.home/files/Usb";
    public static final String FOLDER_FTP = "/mnt/sdcard/Android/data/com.color.home/files/Ftp";
    public static final String FOLDER_FTP_CONFIG = FOLDER_FTP + "/config";
    
    public final static String FTP_PROGRAM_PATH = FOLDER_FTP + "/program";

    public static final int TYPE_INVALID = -1;
    public static final int TYPE_USB = 0;
    public static final int TYPE_SYNCED_USB = 1;
    public static final int TYPE_NET = 2;
    public static final int TYPE_FTP = 3;

    public static String getRemoteFileUri(String relativeFilepath) {
        return AppController.getInstance().getConnectivity().getProgramFolderUri() + relativeFilepath;
    }

    public static String getAbsPath(String fileName) {
        return AppController.getInstance().getDownloadDataDir() +
                "/" + fileName;
    }

    // public static String getDowningvsnAbsPath(Context context, String vsnName) {
    // return CLStorageManager.getDownloadDataDirectory(context) +
    // "/" + vsnName + ".downing";
    // // return context.getFilesDir().getAbsolutePath() + "/new.vsn.downing";
    // }

    public static String[] convertToAbsPaths(Context context, Set<String> collectedFiles) {
        final String[] fileArray = collectedFiles.toArray(new String[0]);
        String downloadDataDir = CLStorageManager.getDownloadDataDirectory(context).toString();
        int count = fileArray.length;
        for (int i = 0; i < count; i++) {
            fileArray[i] = downloadDataDir + fileArray[i];
        }
        return fileArray;
    }

    // file abs path as extra.
    public static final String ACTION_COLOR_CONFIG = "com.color.intent.action.CONFIG";

    public static final String ACTION_COLOR_HOME_STARTED = "com.color.intent.action.APP_STARTED";
    public static final String ACTION_FORCE_PLAY_NET_PROGRAMJSON = "com.color.intent.action.FORCE_PLAY_NET_PROGRAMJSON";
    public static final String ACTION_ALL_DOWNLOAD_FINISHED = "com.color.intent.action.ALL_DOWNLOAD_FINISHED";
    public final static String ACTION_REFRESH = "com.color.intent.action.REFRESH";
    public static final String ACTION_USB_SYNCED = "com.color.intent.ACTION_USB_SYNCED";
    public static final String ACTION_USB_SYNC_START = "com.color.intent.ACTION_USB_SYNC_START";
    public static final String PROGRAMS_JSON = "programs.json";
    public static final String ACTION_PROGRAM_STARTED = "com.color.intent.action.ACTION_PROGRAM_STARTED";
    public static final String ACTION_CURRENT_PROG_INDEX = "com.clt.intent.action.CURRENT_PROG_INDEX";
    public static final String VSN_EXT = ".vsn";

    public static File[] listUsbVsnFiles() {
        return listVsns(FOLDER_USB_0);
    }

    public static File[] listUsbVsnAndFilesFolders() {
        return listVsnAndFilesFolders(FOLDER_USB_0);
    }

    public static File[] listVsns(String folder) {
        File dir = new File(folder);
        if (DBG)
            Log.d(MainActivity.TAG, "listVsn. [dir.isDirectory()=" + dir.isDirectory());
        // If the dir is not a dir, null.
        File[] files = listVsns(dir);
        return files;
    }

    public static File[] listVsns(File dir) {
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(VSN_EXT);
            }
        });

        if (files != null)
            Arrays.sort(files);

        // null or sorted files.
        return files;
    }

    public static File[] listVsnAndFilesFolders(String folder) {
        File dir = new File(folder);
        if (DBG)
            Log.d(MainActivity.TAG, "listVsnAndFilesFolders. [dir.isDirectory()=" + dir.isDirectory());
        // If the dir is not a dir, null.
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(VSN_EXT) || name.toLowerCase().endsWith(".files");
            }
        });
        
//        if (files != null) {
//            Arrays.sort(files, new Comparator<File>() {
//
//                @Override
//                public int compare(File lhs, File rhs) {
//                    return priority(lhs) - priority(rhs);
//                }
//
//                // 0 for higher priority, ".files".
//                // 1 for lower priority, ".vsn".
//                public int priority(File file) {
//                    return file.getName().toLowerCase().endsWith(VSN_EXT) ? 1 : 0;
//                }
//            });
//        }
        
        
        return files;
    }

    public static File[] listSyncedUsbSubFiles(){
        ArrayList<File> files = listAllSubFilesUnderFolder(FOLDER_SYNCED_USB);
        return files.toArray(new File[files.size()]);
    }

    public static File[] listUsbSubFiles(){
        ArrayList<File> files = listAllSubFilesUnderFolder(FOLDER_USB_0);
        return files.toArray(new File[files.size()]);
    }

    public static ArrayList<File> listAllSubFilesUnderFolder(String folder){
        if(DBG)
            Log.d(TAG, "listAllSubFilesUnderFolder of : " + folder);
        File dir = new File(folder);
        ArrayList<File> files = new ArrayList<File>();
        for(File f : dir.listFiles()){
            if(!f.isDirectory())
                files.add(f);
            else
                files.addAll(listAllSubFilesUnderFolder(f.getAbsolutePath()));
        }
        return files;
    }

    public static File[] listSyncedUsbVsnAndFilesFolders() {
        return listVsnAndFilesFolders(FOLDER_SYNCED_USB);
    }

    public static File getDefaultUsbVsnFile() {
        File[] files = listUsbVsnFiles();
        if (files != null && files.length > 0) {
            Arrays.sort(files);

            if (DBG)
                for (File file : files)
                    Log.d(MainActivity.TAG, "getDefaultUsbVsnFile. [file=" + file);

            return files[0];
        }

        return null;
    }

    public static void listIpAddress() {
        IpUtils.getIPAddressAndMask(true);
    }

    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_USB_SYNC_RESULT = "usb_sync_result";
    public static final String EXTRA_REASON_FOR_SYNC_FAILURE = "reason_for_usb_sync_failure";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String EXTRA_PATH = "path";
    public static final String ACTION_PLAY_PROGRAM = "com.clt.broadcast.playProgram";
    public static final String ACTION_PROGRAM_SEEK = "com.clt.broadcast.programSeek";

    public static int absPathToSourceTypeID(String playingFolder) {
        if (TextUtils.isEmpty(playingFolder))
            return TYPE_INVALID;
    
        if (playingFolder.startsWith(FOLDER_USB_0)) {
            return TYPE_USB;
        } else if (playingFolder.startsWith(FOLDER_NET)) {
            return TYPE_NET;
        } else if (playingFolder.startsWith(FTP_PROGRAM_PATH)) {
            return TYPE_FTP;
        } else {
            return TYPE_SYNCED_USB;
        }
    }

    public static final String STOP = "STOP";
    public static final String USB_SYNCED = "usb-synced";
    public static final String LAN = "lan";
    public static final String INTERNET = "internet";
    public static final String USB = "usb";

    public static String sourceTypeIDToSourceType(int playingType) {
        if (playingType == TYPE_USB) {
            return USB;
        } else if (playingType == TYPE_NET) {
            return INTERNET;
        } else if (playingType == TYPE_FTP) {
            return LAN;
        } else if (playingType == TYPE_SYNCED_USB) {
            return USB_SYNCED;
        }
    
        return STOP;
    
    }

    public static long availableSizeOfSdcard(){
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();

        return availableBlocks * blockSize;
    }

    public static String absFolderToSourceType(String playingFolder) {
        return sourceTypeIDToSourceType(absPathToSourceTypeID(playingFolder));
    }

    // final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    //
    // public static String bytesToHex(byte[] bytes) {
    // char[] hexChars = new char[bytes.length * 2];
    // for (int j = 0; j < bytes.length; j++) {
    // int v = bytes[j] & 0xFF;
    // hexChars[j * 2] = hexArray[v >>> 4];
    // hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    // }
    // return new String(hexChars);
    // }

}

// adb logcat MainActivity:V SyncService:V SyncedPrograms:V SyncServiceReceiver:V AppController:V Strategy:V AndroidRuntime:E *:S
