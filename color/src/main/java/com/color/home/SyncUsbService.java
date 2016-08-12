package com.color.home;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.FileUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.color.widgets.floating.FloatingLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static final boolean DBG = true;

    public SyncUsbService() {
        super(SyncUsbService.class.getName());
    }

    public boolean copyFileIfSizeDiff(File fileInUsb, File fileOrDirInSyncedUsb) {
        if (fileInUsb.length() != fileOrDirInSyncedUsb.length()
                || fileInUsb.lastModified() != fileOrDirInSyncedUsb.lastModified()) {
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

        sendBroadcast(new Intent(Constants.ACTION_USB_SYNC_START));
        try {

            Set<String> sameFilesFolder = intersectUsbAndSynced();


            // if (copies.size() > 0 || dels.size() > 0) {
            try {
//                checkAndCopy();
                if (sameFilesFolder != null && sameFilesFolder.size() > 0)
                    intersectTheSameFilesFolders(sameFilesFolder);

            } catch (IllegalAccessException e) {
                failedSync(e.getMessage());

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

    private Set<String> intersectUsbAndSynced() {

        File[] inUsb = Constants.listUsbVsnAndFilesFolders();
        File[] inSyncedUsb = Constants.listSyncedUsbVsnAndFilesFolders();
        if (inUsb == null || inUsb.length == 0 || inSyncedUsb == null) {
            Log.e(TAG, "Error, inUsb=" + inUsb + ", inSyncedUsb=" + inSyncedUsb + ", inUsb.length=" + inUsb.length);
            return null;
        }

        HashMap<String, File> inUsbMap = collectFileNameMap(inUsb);
        HashMap<String, File> inSyncedUsbMap = collectFileNameMap(inSyncedUsb);
        Set<String> keySetUsb = inUsbMap.keySet();
        Set<String> keySetSyncedUsb = inSyncedUsbMap.keySet();

        Set<String> dels = new HashSet<String>(keySetSyncedUsb);
        Set<String> copies = new TreeSet<String>(keySetUsb);
        Set<String> checkSame = new HashSet<String>(keySetSyncedUsb);

        Set<String> filesToOverwrite = new HashSet<String>();
        Set<String> sameFilesFolder = new HashSet<String>();

        // Copy only .files and .vsn not in the internal store USB folder.
        copies.removeAll(keySetSyncedUsb);
        // Del only .files and .vsn not in the usb.
        dels.removeAll(keySetUsb);
        // Files already exist in the internal storage synced USB.
        // keySetSyncedUsb intersect keySetUsb.
        checkSame.retainAll(keySetUsb);

//            MessageDigest digester = MessageDigest.getInstance("MD5");

        for (String filename : checkSame) {
            if (filename.endsWith(".vsn")) {
                File file = inUsbMap.get(filename);
                File file2 = inSyncedUsbMap.get(filename);
                String digest = getMd5ByFile(file);
                String digest2 = getMd5ByFile(file2);
                if (!digest.equals(digest2)) {
//                        copies.add(filename.replace(".vsn", ".files"));
//                        copies.add(filename);
                    filesToOverwrite.add(filename);
                    sameFilesFolder.add(filename.replace(".vsn", ".files"));
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [synced usb file's md5 differs from USB.=" + filename);
                } else {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [synced usb file's md5 is identical to USB's.=" + filename);
                }
            }
        }


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

        int copyResult;
        if (copies.size() > 0) {

            for (String acopy : copies) {
                String absolutePath = inUsbMap.get(acopy).getAbsolutePath();
                if (DBG)
                    Log.d(TAG, "onHandleIntent. [acopy = " + absolutePath);
                File additionalFile = new File(absolutePath);
                long availableSize = Constants.availableSizeOfSdcard();
                long sizeNeeded = additionalFile.length();
                if(availableSize > sizeNeeded){
                    copyResult = copyFileOrDirToSynced(absolutePath);
                    if (copyResult != 0) {
                        failedSync("Failed to copy " + absolutePath + ".");
                        break;
                    }
                }else{
                    Log.e(TAG, "No enough space in sdcard.", new IOException("CopyingException"));
                    failedSync("Failed to copy " + absolutePath + ", No enough space in sdcard.");
                    break;
                }
            }

        }
        if(filesToOverwrite.size() > 0){
            for (String overwrite : filesToOverwrite) {
                String usbAbsolutePath = inUsbMap.get(overwrite).getAbsolutePath();
                String syncedAbsolutePath = inSyncedUsbMap.get(overwrite).getAbsolutePath();
                if (DBG)
                    Log.d(TAG, "onHandleIntent. [acopy = " + usbAbsolutePath );
                File additionalFile = new File(usbAbsolutePath);
                File existingFile = new File(syncedAbsolutePath);
                long availableSize = Constants.availableSizeOfSdcard();
                long sizeNeeded = additionalFile.length() - existingFile.length();
                if(availableSize > sizeNeeded){
                    copyResult = copyFileOrDirToSynced(usbAbsolutePath);
                    if (copyResult != 0) {
                        failedSync("Failed to copy " + usbAbsolutePath + ".");
                        break;
                    }
                }else{
                    Log.e(TAG, "No enough space in sdcard.", new IOException("CopyingException"));
                    failedSync("Failed to copy " + usbAbsolutePath + ", No enough space in sdcard.");
                    break;
                }
            }
        }

        return sameFilesFolder;
    }

    private int copyFileOrDirToSynced(String absolutePath) {
        ArrayList<String> args = new ArrayList<String>(12);
        args.add("cp");
        args.add("-R");
        args.add("-v");
        args.add(absolutePath);
        args.add(absolutePath.replace(Constants.FOLDER_USB_0, Constants.FOLDER_SYNCED_USB));
        return executeCommand(args.toArray(new String[0]));
    }

    private void intersectTheSameFilesFolders(Set<String> sameFilesFolderNames) throws IllegalAccessException {
        if (DBG)
            Log.d(TAG, "sameFilesFolderNames : " + sameFilesFolderNames + " , size : " + sameFilesFolderNames.size());
        if (sameFilesFolderNames.size() <= 0)
            return;
        File[] syncedFilesUnderOneFolder;
        File[] usbFilesUnderOneFolder;
        HashMap<String, File> inUsbMap;
        HashMap<String, File> inSyncedUsbMap;

        Set<String> keySetUsb;
        Set<String> keySetSyncedUsb;

        Set<String> filesToOverwrite;

        Set<String> dels;
        Set<String> copies;
        Set<String> checkSame;

        for (String filesFolderName : sameFilesFolderNames) {
            if (DBG)
                Log.d(TAG, " one of the same .files , name: " + filesFolderName);
            File oneFilesFolderInUsb = new File(Constants.FOLDER_USB_0, filesFolderName);
            File oneFilesFolderInSyncedUsb = new File(Constants.FOLDER_SYNCED_USB, filesFolderName);
            usbFilesUnderOneFolder = oneFilesFolderInUsb.listFiles();
            syncedFilesUnderOneFolder = oneFilesFolderInSyncedUsb.listFiles();
            inUsbMap = collectFileNameMap(usbFilesUnderOneFolder);
            inSyncedUsbMap = collectFileNameMap(syncedFilesUnderOneFolder);

            keySetUsb = inUsbMap.keySet();
            keySetSyncedUsb = inSyncedUsbMap.keySet();

            dels = new HashSet<String>(keySetSyncedUsb);
            copies = new TreeSet<String>(keySetUsb);
            checkSame = new HashSet<String>(keySetSyncedUsb);

            filesToOverwrite = new HashSet<String>();

            copies.removeAll(keySetSyncedUsb);
            dels.removeAll(keySetUsb);
            checkSame.retainAll(keySetUsb);

            for (String filename : checkSame) {
                File file = inUsbMap.get(filename);
                File file2 = inSyncedUsbMap.get(filename);
                if (file.length() != file2.length() ||
                        file.lastModified() != file2.lastModified()) {
                    filesToOverwrite.add(filename);
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [synced usb file's differs from USB.=" + filename +
                                " in " + filesFolderName);
                } else {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [synced usb file's is identical to USB's.=" + filename
                                + " in " + filesFolderName);
                }
            }

            if (dels.size() > 0) {
                ArrayList<String> args = new ArrayList<String>(12);
                args.add("rm");
                args.add("-f");

                for (String adel : dels) {
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [adel = " + inSyncedUsbMap.get(adel).getAbsolutePath());
                    args.add(inSyncedUsbMap.get(adel).getAbsolutePath());
                }
                executeCommand(args.toArray(new String[0]));
            }

            int copyResult;
            if (copies.size() > 0) {

                for (String acopy : copies) {
                    String absolutePath = inUsbMap.get(acopy).getAbsolutePath();
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [acopy = " + absolutePath);
                    File additionalFile = new File(absolutePath);
                    long availableSize = Constants.availableSizeOfSdcard();
                    long sizeNeeded = additionalFile.length();
                    if(availableSize > sizeNeeded){
                        copyResult = copyFileOrDirToSynced(absolutePath);
                        if (copyResult != 0) {
                            failedSync("Failed to copy " + absolutePath + ".");
                            break;
                        }
                    }else{
                        Log.e(TAG, "No enough space in sdcard.", new IOException("CopyingException"));
                        //TODO show alert in floating window
                        failedSync("Failed to copy " + absolutePath + ", No enough space in sdcard.");
                        break;
                    }
                }
            }
            if(filesToOverwrite.size() > 0){
                for (String overwrite : filesToOverwrite) {
                    String usbAbsolutePath = inUsbMap.get(overwrite).getAbsolutePath();
                    String syncedAbsolutePath = inSyncedUsbMap.get(overwrite).getAbsolutePath();
                    if (DBG)
                        Log.d(TAG, "onHandleIntent. [acopy = " + usbAbsolutePath );
                    File additionalFile = new File(usbAbsolutePath);
                    File existingFile = new File(syncedAbsolutePath);
                    long availableSize = Constants.availableSizeOfSdcard();
                    long sizeNeeded = additionalFile.length() - existingFile.length();
                    if(availableSize > sizeNeeded){
                        copyResult = copyFileOrDirToSynced(usbAbsolutePath);
                        if (copyResult != 0) {
                            failedSync("Failed to copy " + usbAbsolutePath + ".");
                            break;
                        }
                    }else{
                        Log.e(TAG, "No enough space in sdcard.", new IOException("CopyingException"));
                        //TODO show alert in floating window
                        failedSync("Failed to copy " + usbAbsolutePath + ", No enough space in sdcard.");
                        break;
                    }
                }
            }
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

    public static String getMd5ByFile(File file) {

        StringBuilder sb = new StringBuilder(40);
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            byte[] bs = md5.digest();
            for (byte x : bs) {
                if ((x & 0xff) >> 4 == 0) {
                    sb.append("0").append(Integer.toHexString(x & 0xff));
                } else {
                    sb.append(Integer.toHexString(x & 0xff));
                }
            }
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
        return sb.toString();
    }

    public void failedSync(String reason) {
        Log.e(TAG, "failedSync. [COPY FAILED.");

        Intent intent2 = new Intent(Constants.ACTION_USB_SYNCED);
        intent2.putExtra(Constants.EXTRA_USB_SYNC_RESULT, false);
        intent2.putExtra(Constants.EXTRA_REASON_FOR_SYNC_FAILURE, reason);
        sendBroadcast(intent2);
    }

    private void checkAndCopy() throws IllegalAccessException {
        File[] inUsb2 = Constants.listUsbVsnAndFilesFolders();
        File[] inSyncedUsb2 = Constants.listSyncedUsbVsnAndFilesFolders();

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
//            File[] inSyncedUsb2 = Constants.listSyncedUsbVsnAndFilesFolders();
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

            if (DBG) {
                br = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
                String aline = br.readLine();
                while (aline != null) {
                    if (DBG)
                        Log.d(TAG, "executeCommand. [command output=" + aline);
                    aline = br.readLine();
                }
            }

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
