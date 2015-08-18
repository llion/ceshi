package com.color.home.messages;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.List;

import android.os.Build;
import android.util.Log;

import com.color.home.AppController;
import com.color.home.netplay.ConfigAPI;
import com.color.network.udp.Message;
import com.color.network.udp.UDPMessenger;
import com.google.common.base.Splitter;

public class DetectionResponse {
    private final static String TAG = "DetectionResponse";
    private static final boolean DBG = false;;

    private Message mMessage;

    public DetectionResponse(Message incomingMessage) throws UnsupportedEncodingException {
        mMessage = incomingMessage;
        // hostname=ImagePRO-II\0ip-address=172.16.2.222\0mac-address=00:04:A5:20:10:D1\0type=ImagePRO-II\0
        // ::
        // type=C1S.
        // hostname = isSetHostname() ? hostname : deviceid (Build.SERIAL)
    }

    public byte[] genResponse() throws UnsupportedEncodingException, SocketException {
        ResponseMessage responseMessage = new ResponseMessage();

        responseMessage.setHostName(AppController.getInstance().getSettings().getString(ConfigAPI.ATTR_TERMINAL_NAME, Build.SERIAL));
        responseMessage.setType("C1S");

        InetAddress srcIpInetAddr = mMessage.getSrcIp();
        
        NetInfo ni = getNetwork(srcIpInetAddr);
        responseMessage.setMac(ni.getMac());
        responseMessage.setIp(ni.getIp());
        return responseMessage.toBytes();
    }

    public static class NetInfo {
        public byte[] mac;
        public InetAddress interfaceIpAddress;

        public String getMac() {
            if (mac == null || mac.length != 6) {
                return "00:00:00:00:00:00";
            } else {
                Iterable<String> split = Splitter.fixedLength(2).split(UDPMessenger.bytesToHex(mac));
                // 18 contains an extra ":".
                StringBuilder sb = new StringBuilder(18);
                for (String abytehex : split) {
                    sb.append(abytehex);
                    sb.append(":");
                }
                // Exclude the last ":".
                return sb.substring(0, sb.length() - 1);
            }
        }
        
        public String getIp() {
            if (interfaceIpAddress == null) {
                return "127.0.0.1";
            } else {
                return interfaceIpAddress.getHostAddress();
            }
        }
    }

    public static NetInfo getNetwork(InetAddress findAddressTowards) throws SocketException {
        NetInfo ni = new NetInfo();

        for (Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();)
        {
            NetworkInterface netinterface = list.nextElement();
            Log.e("network_interfaces", "display name " + netinterface.getDisplayName());
            Log.d(TAG, " netinterface=" + netinterface);

            List<InterfaceAddress> interfaceAddresses = netinterface.getInterfaceAddresses();
            if (interfaceAddresses != null && !interfaceAddresses.isEmpty()) {
                for (InterfaceAddress ifa : interfaceAddresses) {
                    InetAddress inetaddr = ifa.getAddress();
                    if ((inetaddr instanceof Inet4Address)) {
                        // Register: FF 00 00 00
                        // Bigendian: mem = FF 00 00 00. <mem low --> high>
                        // byteToHex dumps : mem low -> high.
                        int prefixNet = 0;
                        short N = ifa.getNetworkPrefixLength();
                        for (int i = 0; i < N; i++) {
                            // Register.
                            prefixNet |= (1 << (31 - i)); // Small
                            // FF 00
                        }

                        // Default to bigendian.
                        // Big-endian systems store the most significant byte of a word in the smallest address and
                        // the least significant byte is stored in the largest address

                        // 01-01 00:25:35.890: D/UDPMessenger(1925): HEX=C0A82B01
                        // Registor: 01 2B A8 C0. Mem: C0 A8 2B 01. /192.168.43.1
                        byte[] byteInterfaceAddr = inetaddr.getAddress();

                        int subnetInt = prefixNet & ByteBuffer.wrap(byteInterfaceAddr).getInt();
                        int subnetTowardsInt = prefixNet & ByteBuffer.wrap(findAddressTowards.getAddress()).getInt();

                        if (DBG) {
                            if (netinterface.getHardwareAddress() != null)
                                Log.d(TAG, " mac=" + UDPMessenger.bytesToHex(netinterface.getHardwareAddress()));

                            Log.d(TAG, "HEX=" + UDPMessenger.bytesToHex(byteInterfaceAddr));
                            Log.d(TAG, "Integer.toHexString=" + Integer.toHexString(prefixNet));
                            Log.d(TAG, "prefixNet=" + UDPMessenger.bytesToHex(ByteBuffer.allocate(4).putInt(prefixNet).array()));
                            
                            Log.d(TAG, "subnet=" + UDPMessenger.bytesToHex(ByteBuffer.allocate(4).putInt(subnetInt).array()));
                            Log.d(TAG,
                                    "subnetTowardsInt subnet=" + UDPMessenger.bytesToHex(ByteBuffer.allocate(4).putInt(subnetTowardsInt).array()));
                            Log.d(TAG, "is ipv4=" + (inetaddr instanceof Inet4Address) + ", leng=" + byteInterfaceAddr.length
                                    + ", identical subnet="
                                    + (subnetInt == subnetTowardsInt));
                            Log.d(TAG, "dumpNetworkinterfaces. [interfaceAddress=" + ifa + ", getPrefixLength=" + N
                                    + ", getAddr=" + inetaddr);
                        }

                        if (subnetInt == subnetTowardsInt) {
                            ni.mac = netinterface.getHardwareAddress();
                            ni.interfaceIpAddress = inetaddr;
                            return ni;
                        }

                    }
                }
            }

        }

        return ni;
    }
}
