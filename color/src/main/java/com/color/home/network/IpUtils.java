package com.color.home.network;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

public class IpUtils {
    // never public, so that another class won't be messed up.
    private final static String TAG = "IpUtils";
    private static final boolean DBG = false;

    /**
     * Convert byte array to hex string
     * 
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for (int idx = 0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10)
                sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     * 
     * @param str
     * @return array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     * 
     * @param filename
     * @return
     * @throws java.io.IOException
     */
    public static String loadFileAsString(String filename) throws java.io.IOException {
        final int BUFLEN = 1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8 = false;
            int read, count = 0;
            while ((read = is.read(bytes)) != -1) {
                if (count == 0 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                    isUTF8 = true;
                    baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count += read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try {
                is.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Returns MAC address of the given interface name.
     * 
     * @param interfaceName
     *            eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName))
                        continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null)
                    return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0)
                    buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
        /*
         * try { // this is so Linux hack return
         * loadFileAsString("/sys/class/net/" +interfaceName +
         * "/address").toUpperCase().trim(); } catch (IOException ex) { return
         * null; }
         */
    }

    /**
     * Get IP address from first non-localhost interface
     * 
     * @param ipv4
     *            true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static ArrayList<String> getIPAddressAndMask(boolean useIPv4) {
        ArrayList<String> ipandmasks = new ArrayList<String>();

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (DBG) {
                    List<InterfaceAddress> interfaceAddresses = intf.getInterfaceAddresses();
                    Log.i(TAG,
                            "getIPAddress. intf=" + intf + ", getInterfaceAddresses=" + interfaceAddresses + ", intf name="
                                    + intf.getName());
                    // w|0.1
                    for (InterfaceAddress ia : interfaceAddresses) {
                        if (DBG) {
                            InetAddress broadcast = ia.getBroadcast();

                            short networkPrefixLength = ia.getNetworkPrefixLength();
                            int numberips = (1 << (32 - networkPrefixLength));
                            Log.i(TAG, "getIPAddressAndMask. InterfaceAddress prefix=" + networkPrefixLength + ", ip="
                                    + ia.getAddress().getHostAddress() + ", subnet=" + ia.getBroadcast() + ", hosts=" + numberips);

                            if (broadcast != null) {
                                byte[] address = broadcast.getAddress();

                                InetAddress i4astart = InetAddress.getByAddress(new byte[] { address[0], address[1], address[2],
                                        (byte) (address[3] - numberips + 2) });

                                if (DBG)
                                    Log.i(TAG, "getIPAddressAndMask. i4astart=" + i4astart.getHostAddress());
                            }
                        }
                    }
                }

                // List<InetAddress> addrs = Collections.list(intf
                // .getInetAddresses());
                // for (InetAddress addr : addrs) {
                // if (!addr.isLoopbackAddress()) {
                // String sAddr = addr.getHostAddress().toUpperCase();
                // boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                // if (useIPv4) {
                // if (isIPv4) {
                // ipandmasks.add(sAddr);
                // if (DBG)
                // Log.i(TAG, "getIPAddressAndMask. sAddr=" + sAddr);
                // }
                // }
                // }
                // }
            }
        } catch (Exception ex) {
            if (DBG)
                Log.i(TAG, "getIPAddress. ex=" + ex);
        } // for now eat exceptions
        return ipandmasks;
    }

}