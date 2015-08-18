package com.color.home.netplay;

public interface ConfigAPI {

    public final static String ATTR_LOCALE = "locale";
    public final static String ATTR_TEXT_CHARSET = "charset";
    // is.wifi.p2p=false
    public static final String ATTR_WIFI_ENABLED = "wifi.enabled";
    public static final String ATTR_WIFI_SSID = "wifi.ssid";
    public static final String ATTR_WIFI_PASS = "wifi.pass";
    public static final String ATTR_WIFI_TYPE = "wifi.type";
    public static final String ATTR_WIFI_ISHIDDEN = "wifi.ishidden";
    public static final String ATTR_AP_SSID = "wifi.ap.ssid";
    public static final String ATTR_AP_PASS = "wifi.ap.pass";
    public static final String ATTR_AP_CHANNEL = "wifi.ap.channel";
    public static final String ATTR_IS_WIFI_P2P = "is.wifi.p2p";
    // server.ip=10.193.250.193:8080
    public final static String ATTR_SERVER_IP = "server.ip";
    // lan.mode=static
    public final static String ATTR_LAN_MODE = "lan.mode";
    public final static String ATTR_LAN_ENABLED = "lan.enabled";
    // lan.static.ip=192.168.99.100
    public final static String ATTR_LAN_STATIC_IP = "lan.static.ip";
    // lan.static.netmask=255.255.255.0
    public final static String ATTR_LAN_STATIC_NETMASK = "lan.static.netmask";
    public final static String ATTR_LAN_STATIC_GATEWAY = "lan.static.gateway";
    public final static String ATTR_LAN_STATIC_DNS1 = "lan.static.dns1";
    public final static String ATTR_LAN_STATIC_DNS2 = "lan.static.dns2";
    public static final String ATTR_TERMINAL_NAME = "terminal.name";
    public static final String ATTR_TEXT_ANTIALIAS = "text.antialias";

}