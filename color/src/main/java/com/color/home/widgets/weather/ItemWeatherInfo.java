package com.color.home.widgets.weather;

import static ru.gelin.android.weather.notification.skin.impl.BaseWeatherNotificationReceiver.WEATHER_KEY;
import ru.gelin.android.weather.Weather;
import ru.gelin.android.weather.notification.AppUtils;
import ru.gelin.android.weather.notification.WeatherStorage;
import ru.gelin.android.weather.notification.app.PreferenceKeysApp;
import ru.gelin.android.weather.notification.skin.impl.BaseWeatherNotificationReceiver;
import ru.gelin.android.weather.notification.skin.impl.WeatherLayout;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.color.home.ProgramParser.Item;
import com.color.home.ProgramParser.Region;
import com.color.home.widgets.ItemData;
import com.color.home.widgets.RegionView;

public class ItemWeatherInfo extends LinearLayout implements ItemData {
    private final static String TAG = "ItemWeatherInfo";
    private static final boolean DBG = false;

    private Item mItem;
    private Region mRegion;

    public ItemWeatherInfo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ItemWeatherInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemWeatherInfo(Context context) {
        super(context);
    }

    @Override
    public void setRegion(Region region) {
        mRegion = region;
    }

    @Override
    public void setItem(RegionView regionView, Item item) {
        mItem = item;

        // createWeatherLayout(getContext(), this);
    }

    /**
     * Creates the weather layout to render activity.
     */
    protected WeatherLayout createWeatherLayout(Context context, View view) {
        return new WeatherLayout(context, view);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        BaseWeatherNotificationReceiver.registerWeatherHandler(weatherHandler);
        Context context = getContext();
        WeatherStorage storage = new WeatherStorage(context);
        WeatherLayout layout = createWeatherLayout(context, this);
        Weather weather = storage.load();
        layout.bind(weather);

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        preferences.edit().putString(ru.gelin.android.weather.notification.skin.impl.PreferenceKeys.LOCATION, mItem.regionname)
                .remove(PreferenceKeysApp.LOCATION_TYPE)
                .putString("lang", mItem.language)
                .putString("tempprefix", mItem.temperatureprefix)
                .putString("windprefix", mItem.windprefix)
                .putString("humidityprefix", mItem.humidity)
                .apply();

        // AppUtils.startUpdateService(getContext(), true, true, new AppUtils.WeatherParcelable(mItem.regionname, mItem.isshowweather,
        // mItem.temperatureprefix, mItem.isshowtemperature, mItem.windprefix, mItem.isshowwind, mItem.airprefix,
        // mItem.isshowair, mItem.ultraviolet,
        // mItem.isshowultraviolet, mItem.movementindex, mItem.isshowmovementindex, mItem.coldindex, mItem.isshowcoldindex,
        // mItem.humidity,
        // mItem.serverType, mItem.regioncode,
        // mItem.isshowhumidity, mItem.longitud, mItem.latitude, mItem.timezone, mItem.language, mItem.useproxy, mItem.proxyserver,
        // mItem.proxyport, mItem.proxyuser, mItem.proxypsw, mItem.isshowpic,
        // mItem.showstyle));
        // Location location = weather.getLocation();
        // setTitle(location == null ? "" : location.getText());

        AppUtils.startUpdateService(getContext(), true, true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BaseWeatherNotificationReceiver.unregisterWeatherHandler();
    }

    final Handler weatherHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // stopProgress();
            Weather weather = (Weather) msg.getData().getParcelable(WEATHER_KEY);
            if (DBG)
                Log.i(TAG, "handleMessage. weather=" + weather);
            if (weather == null) {
                return;
            }
            WeatherLayout layout = createWeatherLayout(
                    getContext(), ItemWeatherInfo.this);
            layout.bind(weather);
        };
    };

    // Intent intent = getIntent();
    // Parcelable par = intent.getParcelableExtra("xx");
    //
    // MyParcelable mp = (MyParcelable) par;

}
