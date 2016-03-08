package com.color.home.messages;

import java.io.UnsupportedEncodingException;
import java.net.SocketException;

import android.content.Context;
import android.util.Log;

import com.color.network.udp.Message;
import com.color.network.udp.UDPMessenger;

public class MyMessenger extends UDPMessenger {
    private final static String TAG = "MyMessenger";
    private static final boolean DBG = false;

    public MyMessenger(Context context, int port) throws IllegalArgumentException {
        super(context, port);
    }

    @Override
    public void onIncomingMessage() {
        super.onIncomingMessage();
        Message im = getIncomingMessage();
        if (DBG)
            Log.d(TAG, "onIncomingMessage. [incomingMessage=" + im);

        if (im != null) {
            String message = im.getMessage();

            if (DBG)
                Log.d(TAG, "onIncomingMessage. xx[" + message + ", ip=" + im.getSrcIp() + ", im.getSrcPort()=" + im.getSrcPort());

            // Detection.
            if (message != null && message.startsWith("lookforc1s")) {
                try {
                    DetectionResponse dr = new DetectionResponse(im);
                    byte[] response = dr.genResponse();
                    // sendMessage(message, ip, port)
                    sendMessage(im.getSrcIp(), im.getSrcPort(), response);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UnsupportedEncodingException", e);
                } catch (SocketException e) {
                    // TODO Auto-generated catch block
                    Log.e(TAG, "SocketException", e);
                }
            }

        }
    }

}