//package com.color.home.provider;
//
//import android.net.Uri;
//import android.provider.BaseColumns;
//
//public final class ColorContract implements BaseColumns {
//
//    /**
//     * The authority we use to get to our sample provider.
//     */
//    public static final String AUTHORITY = "com.color.home";
//    // This class cannot be instantiated
//    private ColorContract() {
//    }
//
//    /**
//     * The table name offered by this provider
//     */
//    public static final String TABLE_NAME = "network";
//    public static final String PROGRAM_TABLE_NAME = "program";
//
//    /**
//     * The content:// style URL for this table
//     */
//    public static final Uri NETWORK_CONTENT_URI = Uri.parse("content://" + ColorContract.AUTHORITY + "/network");
//    public static final Uri NETWORK_WIFI_CONTENT_URI = Uri.parse("content://" + ColorContract.AUTHORITY + "/network/wifi");
//    public static final Uri NETWORK_AP_CONTENT_URI = Uri.parse("content://" + ColorContract.AUTHORITY + "/network/ap");
//    public static final Uri NETWORK_LAN_CONTENT_URI = Uri.parse("content://" + ColorContract.AUTHORITY + "/network/lan");
//
//    public static final Uri PROGRAM_CONTENT_URI = Uri.parse("content://" + ColorContract.AUTHORITY + "/" + PROGRAM_TABLE_NAME);
//
//    /**
//     * The content URI base for a single row of data. Callers must append a numeric row id to this Uri to retrieve a row
//     */
//    public static final Uri CONTENT_ID_URI_BASE = Uri.parse("content://" + ColorContract.AUTHORITY + "/network/");
//    public static final Uri PROGRAM_CONTENT_ID_URI_BASE = Uri.parse("content://" + ColorContract.AUTHORITY + "/" + PROGRAM_TABLE_NAME);
//
//    /**
//     * The MIME type of {@link #NETWORK_CONTENT_URI}.
//     */
//    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.colorlight.network";
//
//    /**
//     * The MIME type of a {@link #NETWORK_CONTENT_URI} sub-directory of a single row.
//     */
//    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.colorlight.network";
//
//    public static final String CONTENT_PROGRAM_ITEM_TYPE = "vnd.android.cursor.item/vnd.colorlight.program";
//    /**
//     * The default sort order for this table
//     */
//    public static final String DEFAULT_SORT_ORDER = "_id ASC";
//
//    /**
//     * Column name for the single column holding our data.
//     * <P>
//     * Type: INTEGER
//     * </P>
//     */
//    public static final String COLUMN_TYPE = "type";
//
//    public static final String COLUMN_ENABLED = "enabled";
//
//    public static final String COLUMN_ISSTATIC = "isstatic";
//    public static final String COLUMN_GW = "gateway";
//    public static final String COLUMN_DNS1 = "dns1";
//    public static final String COLUMN_DNS2 = "dns2";
//    public static final String COLUMN_IP = "ip";
//    public static final String COLUMN_MASK = "mask";
//    public static final String COLUMN_MAC = "mac";
//    public static final String COLUMN_BROADCAST = "broadcast";
//
//    public static final String COLUMN_SSID = "ssid";
//    public static final String COLUMN_PASS = "pass";
//
//    // now for channel.
//    public static final String COLUMN_RES1 = "res1";
//    public static final String COLUMN_RES2 = "res2";
//    public static final String COLUMN_RES3 = "res3";
//    public static final String COLUMN_RES4 = "res4";
//
//
//    public static final String COLUMN_PROGRAM_SOURCE = "program_source";
//    public static final String COLUMN_PROGRAM_PATH = "program_path";
//    public static final String COLUMN_PROGRAM_FILENAME = "program_filename";
//
//}
