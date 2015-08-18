package com.color.home.messages;

import java.io.UnsupportedEncodingException;

import android.util.Log;

public class ResponseMessage {
    // “hostname=ImagePRO-II\0ip-address=172.16.2.222\0mac-address=00:04:A5:20:10:D1\0type=ImagePRO-II\0”
    private final static String TAG = "ResponseMessage";
    private static final boolean DBG = false;;;

    private final static String HOSTNAME = "hostname";
    private final static String IP_ADDRESS = "ip-address";
    private final static String MAC_ADDRESS = "mac-address";
    private final static String TYPE = "type";

    private String mHostName;
    private String mIp;
    private String mMac;
    private String mType;

    public void setHostName(String hostName) {
        mHostName = hostName;
    }

    public void setIp(String ip) {
        mIp = ip;
    }

    public void setMac(String mac) {
        mMac = mac;
    }

    public void setType(String type) {
        mType = type;
    }

    public byte[] toBytes() throws UnsupportedEncodingException {
        byte[] host = genAVP(HOSTNAME, mHostName);
        byte[] ip = genAVP(IP_ADDRESS, mIp);
        byte[] mac = genAVP(MAC_ADDRESS, mMac);
        byte[] type = genAVP(TYPE, mType);
        
        int pos = 0;
        // We have 4 '\0's.
        byte[] result = new byte[host.length + ip.length + mac.length + type.length + 4];
        
        pos += copy(host, pos, result);
        pos += copy(ip, pos, result);
        pos += copy(mac, pos, result);
        copy(type, pos, result);
        
        return result;
    }

    private int copy(byte[] src, int pos, byte[] result) {
        System.arraycopy(src, 0, result, pos, src.length);
        result[pos + src.length] = '\0';
        return src.length + 1;
    }

    private byte[] genAVP(String attr, String value) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(attr).append("=").append(value);
        if (DBG)
            Log.d(TAG, "genAVP. [attr=" + attr + ", value=" + value);
        return sb.toString().getBytes("UTF-8");
    }

}
