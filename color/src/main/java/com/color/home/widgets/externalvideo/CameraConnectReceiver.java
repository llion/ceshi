package com.color.home.widgets.externalvideo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Administrator on 2016/8/25.
 */
public class CameraConnectReceiver extends BroadcastReceiver {

    public  static final String TAG ="CameraConnectReceiver";
    public  static final boolean DBG = true;
    private ItemExternalVideoView externalVideoView;
    private UsbDevice usbDevice;

    public CameraConnectReceiver(ItemExternalVideoView externalVideoView) {
        this.externalVideoView = externalVideoView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        int vendorId = usbDevice.getVendorId();
        int productId = usbDevice.getProductId();
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            Log.d(TAG,"[device." + device);
            Log.d(TAG,"[device.getClass= " + device.getInterface(0).getClass());
        }

//        for (int i = 0; i < 10; i++){
//            if (usbDevice.getInterface(i) != null){
//                Log.i(TAG, "getInterfaceClass : " +  usbDevice.getInterface(i).getInterfaceClass());//14,14,14,1,1,1
//            }else{
//                break;
//            }
//        }

        int deviceClass = usbDevice.getInterface(0).getInterfaceClass();
        if (DBG)
            Log.d(TAG, "action= " + action + ", externalVideoView= " + externalVideoView +", class= " +  usbDevice.getInterface(0).getInterfaceClass()
            + ", usbDevice.getDeviceClass()= " + usbDevice.getDeviceClass());

        if (DBG)
            Log.d(TAG, "usbDevice= " + usbDevice);

        if (DBG)
            Log.d(TAG, "getVendorId= " + vendorId + ", getProductId" + productId);


        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)){
            if (deviceClass == UsbConstants.USB_CLASS_VIDEO && externalVideoView !=null){

                externalVideoView.clear();
                externalVideoView.openCamera();
                externalVideoView.setPreviewDisplay(externalVideoView.getSurfaceHolder());
                externalVideoView.startPreview();

            }
        } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)){
            if (deviceClass == UsbConstants.USB_CLASS_VIDEO && externalVideoView !=null){
                externalVideoView.clear();
            }
        }
    }


}
