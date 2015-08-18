package ru.gelin.android.weather.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *  Static methods to start main app services and activites.
 */
public class AppUtils {

    /** Main app package name */
    private static final String APP_PACKAGE_NAME = Tag.class.getPackage().getName();

    /** Intent action to start the service */
    public static String ACTION_START_UPDATE_SERVICE =
            APP_PACKAGE_NAME + ".ACTION_START_UPDATE_SERVICE";

    /** Intent action to start the main activity */
    public static String ACTION_START_MAIN_ACTIVITY =
            APP_PACKAGE_NAME + ".ACTION_START_MAIN_ACTIVITY";

    /** Verbose extra name for the service start intent. */
    public static String EXTRA_VERBOSE = "verbose";
    /** Force extra name for the service start intent. */
    public static String EXTRA_FORCE = "force";
    public static String EXTRA_WP = "weather_parcelable";

    /**
     *  Returns intent to start the main activity.
     */
    public static Intent getMainActivityIntent() {
        Intent startIntent = new Intent(ACTION_START_MAIN_ACTIVITY);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return startIntent;
    }

    /**
     *  Starts the main activity.
     */
    public static void startMainActivity(Context context) {
        context.startActivity(getMainActivityIntent());
    }

    /**
     *  Starts the update service.
     */
    public static void startUpdateService(Context context) {
        startUpdateService(context, false, false);
    }

    /**
     *  Starts the update service.
     *  If the verbose is true, the update errors will be displayed as toasts.
     */
    public static void startUpdateService(Context context, boolean verbose) {
        startUpdateService(context, verbose, false);
    }

    /**
     *  Starts the service.
     *  If the verbose is true, the update errors will be displayed as toasts.
     *  If the force is true, the update will start even when the weather is
     *  not expired.
     */
    public static void startUpdateService(Context context, boolean verbose, boolean force) {
        Intent startIntent = new Intent(ACTION_START_UPDATE_SERVICE);
        //startIntent.setClassName(UpdateService.class.getPackage().getName(), UpdateService.class.getName());
        startIntent.putExtra(EXTRA_VERBOSE, verbose);
        startIntent.putExtra(EXTRA_FORCE, force);
        context.startService(startIntent);
    }
    public static void startUpdateService(Context context, boolean verbose, boolean force, WeatherParcelable wp) {
        Intent startIntent = new Intent(ACTION_START_UPDATE_SERVICE);
        //startIntent.setClassName(UpdateService.class.getPackage().getName(), UpdateService.class.getName());
        startIntent.putExtra(EXTRA_VERBOSE, verbose);
        startIntent.putExtra(EXTRA_FORCE, force);
        startIntent.putExtra(EXTRA_WP, wp);
        context.startService(startIntent);
    }

    private AppUtils() {
        //avoid instantiation
    }
    
    public static class WeatherParcelable implements Parcelable {
        public String regionname;
        public String isshowweather;
        public String temperatureprefix;
        public String isshowtemperature;
        public String windprefix;
        public String isshowwind;
        public String airprefix;
        public String isshowair;
        public String ultraviolet;
        public String isshowultraviolet;
        public String movementindex;
        public String isshowmovementindex;
        public String coldindex;
        public String isshowcoldindex;
        public String humidity;
        public String serverType;
        public String regioncode;
        public String isshowhumidity;
        public String longitud;
        public String latitude;
        public String timezone;
        public String language;
        public String useproxy;
        public String proxyserver;
        public String proxyport;
        public String proxyuser;
        public String proxypsw;
        public String isshowpic;
        public String showstyle;

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(regionname);
        }

        public static final Parcelable.Creator<WeatherParcelable> CREATOR = new Parcelable.Creator<WeatherParcelable>() {
            public WeatherParcelable createFromParcel(Parcel in) {
                return new WeatherParcelable(in);
            }

            public WeatherParcelable[] newArray(int size) {
                return new WeatherParcelable[size];
            }
        };

        private WeatherParcelable(Parcel in) {
            regionname = in.readString();
        }

        public WeatherParcelable(String regionname, String isshowweather, String temperatureprefix, String isshowtemperature,
                String windprefix, String isshowwind, String airprefix, String isshowair, String ultraviolet, String isshowultraviolet,
                String movementindex, String isshowmovementindex, String coldindex, String isshowcoldindex, String humidity,
                String serverType, String regioncode, String isshowhumidity, String longitud, String latitude, String timezone,
                String language, String useproxy, String proxyserver, String proxyport, String proxyuser, String proxypsw,
                String isshowpic, String showstyle) {
            super();
            this.regionname = regionname;
            this.isshowweather = isshowweather;
            this.temperatureprefix = temperatureprefix;
            this.isshowtemperature = isshowtemperature;
            this.windprefix = windprefix;
            this.isshowwind = isshowwind;
            this.airprefix = airprefix;
            this.isshowair = isshowair;
            this.ultraviolet = ultraviolet;
            this.isshowultraviolet = isshowultraviolet;
            this.movementindex = movementindex;
            this.isshowmovementindex = isshowmovementindex;
            this.coldindex = coldindex;
            this.isshowcoldindex = isshowcoldindex;
            this.humidity = humidity;
            this.serverType = serverType;
            this.regioncode = regioncode;
            this.isshowhumidity = isshowhumidity;
            this.longitud = longitud;
            this.latitude = latitude;
            this.timezone = timezone;
            this.language = language;
            this.useproxy = useproxy;
            this.proxyserver = proxyserver;
            this.proxyport = proxyport;
            this.proxyuser = proxyuser;
            this.proxypsw = proxypsw;
            this.isshowpic = isshowpic;
            this.showstyle = showstyle;
        }

    }
}
