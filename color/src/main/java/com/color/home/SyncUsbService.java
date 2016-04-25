package com.color.home;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.FileUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class SyncUsbService extends IntentService {
    private final static String TAG = "SyncUsbService";
    private static final boolean DBG = false;

    public SyncUsbService() {
        super(SyncUsbService.class.getName());
    }

    public boolean copyFileIfSizeDiff(File fileInUsb, File fileOrDirInSyncedUsb) {
        if (fileInUsb.length() != fileOrDirInSyncedUsb.length()) {
            if (DBG)
                Log.d(TAG, "copyFilesUnderFolder. [file diff, start copy. fileInUsb=" + fileInUsb
                        + ", destFile=" + fileOrDirInSyncedUsb);
            boolean result = FileUtils.copyFile(fileInUsb, fileOrDirInSyncedUsb);
            if (DBG)
                Log.d(TAG, "onHandleIntent. [copyFile result=" + result + ", src=" + fileInUsb + ", dest="
                        + fileOrDirInSyncedUsb);
            return result;
        } else {
            if (DBG)
                Log.d(TAG, "copyFilesUnderFolder. [file was OK. aFileInUsbFilesDir=" + fileInUsb
                        + ", destFile=" + fileOrDirInSyncedUsb);
            return true;
        }
    }

    public boolean copyFilesUnderFolder(File fileInUsb, File dirInSyncUsb) {
        // OK. copy files.
        File[] filesInUsbFilesDir = fileInUsb.listFiles();
        if (filesInUsbFilesDir != null) {
            for (File aFileInUsbFilesDir : filesInUsbFilesDir) {
                File destFile = new File(dirInSyncUsb, aFileInUsbFilesDir.getName());
                boolean copyFileIfSizeDiff = copyFileIfSizeDiff(aFileInUsbFilesDir, destFile);
                if (!copyFileIfSizeDiff) {
                    if (DBG)
                        Log.d(TAG, "copyFilesUnderFolder. [failed copy, abort.");
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (DBG)
            Log.d(TAG, "onHandleIntent. [");


        if (!ensureSyncUsbPath()) return;


        try {
            File[] inUsb = Constants.listUsbVsnAndFilesFolders();
            File[] inSyncedUsb = Constants.listSyncedUsbVsnAndFilesFolders();
            if (inUsb == null || inUsb.length == 0 || inSyncedUsb == null) {
                Log.e(TAG, "Error, inUsb=" + inUsb + ", inSyncedUsb=" + inSyncedUsb + ", inUsb.length=" + inUsb.length);
                return;
            }

            HashMap<String, File> inUsbMap = collectFileNameMap(inUsb);
            HashMap<String, File> inSyncedUsbMap = collectFileNameMap(inSyncedUsb);
            Set<String> keySetUsb = inUsbMap.keySet();
            Set<String> keySetSyncedUsb = inSyncedUsbMap.keySet();

            Set<String> dels = new HashSet<String>(keySetSyncedUsb);
            Set<String> copies = new TreeSet<String>(keySetUsb);
            Set<String> checkSame = new HashSet<String>(keySetSyncedUsb);

            // Copy only .files and .vsn not in the internal store USB folder.
            copies.removeAll(keySetSyncedUsb);
            // Del only .files and .vsn not in the usb.
            dels.removeAll(keySetUsb);
            // Files already exist in the internal storage synced USB.
            // keySetSyncedUsb intersect keySetUsb
            checkSame.retainAll(keySetUsb);

//            MessageDigest digester = MessageDigest.getInstance("MD5");
            for (String filename : checkSame) {
                if (filename.endsWith(".vsn")) {
                    File file = inUsbMap.get(filename);
                    File file2 = inSyncedUsbMap.get(filename);
                    String digest = getMd5ByFile(file);
                    String digest2 = getMd5ByFile(file2);
                    if (!digest.equals(digest2)) {
                        copies.add(filename.replace(".vsn", ".files"));
                        copies.add(filename);
                        if (DBG)
                            Log.d(TAG, "onHandleIntent. [synced usb file's md5 differs from USB.=" + filename);
                    } else {
                        if (DBG)
                            Log.d(TAG, "onHandleIntent. [synced usb file's md5 is identical to USB's.=" + filename);
                    }
                }
            }

            // if (copies.size() > 0 || dels.size() > 0) {
            // if (DBG)
            // Log.d(TAG, "onHandleIntent. [Sync usb start.");
            sendBroadcast(new Intent(Constants.ACTION_USB_SYNC_START));
            // }

            if (dels.size() > 0) {
                ArrayList<String> args = new ArrayList<String>(12);
                args.add("rm");
                args.add("-rf");

                for (String adel : dels) {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [adel = " + inSyncedUsbMap.get(adel).getAbsolutePath());
                    args.add(inSyncedUsbMap.get(adel).getAbsolutePath());
                }
                executeCommand(args.toArray(new String[0]));
            }

            int copyResult = -1;
            if (copies.size() > 0) {
                ArrayList<String> args = new ArrayList<String>(12);
                args.add("cp");
                args.add("-R");
                args.add("-v");
                for (String acopy : copies) {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [acopy = " + inUsbMap.get(acopy).getAbsolutePath());
                    args.add(inUsbMap.get(acopy).getAbsolutePath());
                }
                args.add(Constants.FOLDER_SYNCED_USB);

                if (DBG) {
                    for (String arg : args) {
                        Log.d(TAG, "onHandleIntent. [arg string=" + arg);
                    }
                }

                copyResult = executeCommand(args.toArray(new String[0]));

                if (copyResult != 0) {
                    failedSync();
                    return;
                }
            }

            // if (copies.size() > 0 || dels.size() > 0) {
            try {
                checkAndCopy();

            } catch (IllegalAccessException e) {
                failedSync();

                e.printStackTrace();
                return;
            }

            executeCommand("sync");
            if (DBG)
                Log.d(TAG, "onHandleIntent. [Sync usb ACTION_USB_SYNCED.");
            sendBroadcast(new Intent(Constants.ACTION_USB_SYNCED));
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean ensureSyncUsbPath() {
        File usbSyncDir = new File(Constants.FOLDER_SYNCED_USB);
        if (!usbSyncDir.isDirectory()) {
            if (!usbSyncDir.mkdirs()) {
                Log.w(TAG, "onHandleIntent. [Cannot make dir:" + usbSyncDir);


                return false;
            }
        }
        return true;
    }

    public String getMd5ByFile(File file) {

        String value = "";
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    public void failedSync() {
        Log.e(TAG, "failedSync. [COPY FAILED.");

        Intent intent2 = new Intent(Constants.ACTION_USB_SYNCED);
        intent2.putExtra(Constants.EXTRA_USB_SYNC_RESULT, false);
        sendBroadcast(intent2);
    }

    private void checkAndCopy() throws IllegalAccessException {
        File[] inUsb2 = Constants.listUsbVsnAndFilesFolders();

        if (inUsb2 != null)
            for (File fileInUsb : inUsb2) {
                File syncedUsb = new File(Constants.FOLDER_SYNCED_USB);
                File fileOrDirInSyncedUsb = new File(syncedUsb, fileInUsb.getName());
                if (DBG)
                    Log.d(TAG, "inUsb2. [file=" + fileInUsb);
                // .files
                if (fileInUsb.isDirectory()) {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [dir name=" + fileInUsb.getName());

                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [dirInSyncUsb=" + fileOrDirInSyncedUsb);
                    if (!fileOrDirInSyncedUsb.exists()) {
                        if (DBG)
                            Log.d(TAG, "onHandleIntent. [exist=" + fileOrDirInSyncedUsb);
                        boolean mkdirs = fileOrDirInSyncedUsb.mkdirs();
                        if (mkdirs) {
                            if (DBG)
                                Log.d(TAG, "onHandleIntent. [mk dir fileOrDirInSyncedUsb=" + fileOrDirInSyncedUsb
                                        + ", and start sync");
                            boolean copyFilesUnderFolder = copyFilesUnderFolder(fileInUsb, fileOrDirInSyncedUsb);
                            if (!copyFilesUnderFolder) {
                                throw new IllegalAccessError("Cannot copy");
                            }
                        }
                    } else {
                        boolean copyFilesUnderFolder = copyFilesUnderFolder(fileInUsb, fileOrDirInSyncedUsb);
                        if (!copyFilesUnderFolder) {
                            throw new IllegalAccessError("Cannot copy");
                        }
                        // OK. copy files.
                    }

                } else {
                    // is regular file.
                    boolean copyFileIfSizeDiff = copyFileIfSizeDiff(fileInUsb, fileOrDirInSyncedUsb);
                    if (!copyFileIfSizeDiff) {
                        throw new IllegalAccessError("Cannot copy file.");
                    }

                }
            }

        if (DBG) {
            File[] inSyncedUsb2 = Constants.listSyncedUsbVsnAndFilesFolders();
            if (inSyncedUsb2 != null)
                for (File file : inSyncedUsb2) {
                    Log.d(TAG, "inSyncedUsb2. [file=" + file
                            + ", size=" + file.length());
                }
        }
    }

    private HashMap<String, File> collectFileNameMap(File[] files) {
        HashMap<String, File> inUsbMap = new HashMap<String, File>(files.length);
        for (File file : files) {
            inUsbMap.put(file.getName(), file);
        }
        return inUsbMap;
    }

    private static int executeCommand(String... command) {
        if (DBG) {
            for (String commStr : command) {
                Log.d(TAG, "executeCommand. [command=" + commStr);
            }
        }

        BufferedReader br = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // if (DBG) {
            // br = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
            // String aline = br.readLine();
            // while (aline != null) {
            // if (DBG)
            // Log.d(TAG, "executeCommand. [command output=" + aline);
            // aline = br.readLine();
            // }
            // }

            return p.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return -1;

    }

    public static void startService() {
        if (DBG)
            Log.d(TAG, "startService. [");

        Context context = AppController.getInstance();
        context.startService(new Intent(context, SyncUsbService.class));
    }
}
