/*
 *  Android Weather Notification.
 *  Copyright (C) 2010  Denis Nelubin aka Gelin
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *  http://gelin.ru
 *  mailto:den@gelin.ru
 */

package ru.gelin.android.weather.notification.skin.impl;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import ru.gelin.android.weather.Temperature;
import ru.gelin.android.weather.TemperatureUnit;
import ru.gelin.android.weather.Weather;
import ru.gelin.android.weather.WeatherCondition;
import ru.gelin.android.weather.notification.ParcelableWeather2;
import ru.gelin.android.weather.notification.WeatherStorage;
import ru.gelin.android.weather.notification.skin.Tag;

import static ru.gelin.android.weather.notification.skin.impl.PreferenceKeys.*;
import static ru.gelin.android.weather.notification.skin.impl.ResourceIdFactory.LAYOUT;
import static ru.gelin.android.weather.notification.skin.impl.ResourceIdFactory.STRING;

/**
 *  Weather notification receiver built into basic application.
 */
abstract public class BaseWeatherNotificationReceiver extends
        WeatherNotificationReceiver {

    /** Key to store the weather in the bundle */
    public static final String WEATHER_KEY = "weather";
    /** Suffix for layouts with last update time */
    public static final String LAYOUT_UPDATE_SUFFIX = "_update";
    
    /** Handler to receive the weather */
    static Handler handler;
    
    /** Temperature formatter */
    protected TemperatureFormat tempFormat = createTemperatureFormat();
    
    /**
     *  Registers the handler to receive the new weather.
     *  The handler is owned by activity which have initiated the update.
     *  The handler is used to update the weather displayed by the activity. 
     */
    public static synchronized void registerWeatherHandler(Handler handler) {
        BaseWeatherNotificationReceiver.handler = handler;
    }
    
    /**
     *  Unregisters the weather update handler.
     */
    public static synchronized void unregisterWeatherHandler() {
        BaseWeatherNotificationReceiver.handler = null;
    }
    
    @Override
    protected void cancel(Context context) {
        Log.d(Tag.TAG, "cancelling weather");
        getNotificationManager(context).cancel(getNotificationId());
    }

    @Override
    protected void notify(Context context, Weather weather) {
        Log.d(Tag.TAG, "displaying weather: " + weather);
        
        WeatherStorage storage = new WeatherStorage(context);
        storage.save(weather);
        
        ResourceIdFactory ids = ResourceIdFactory.getInstance(context);
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(context);
    
        TemperatureType unit = TemperatureType.valueOf(prefs.getString(
            TEMP_UNIT, TEMP_UNIT_DEFAULT));
        TemperatureUnit mainUnit = unit.getTemperatureUnit();
        NotificationStyle textStyle = NotificationStyle.valueOf(prefs.getString(
                NOTIFICATION_TEXT_STYLE, NOTIFICATION_TEXT_STYLE_DEFAULT));

        Notification notification = new Notification();
        
        notification.icon = getNotificationIconId();

        if (weather.isEmpty() || weather.getConditions().size() <= 0) {
            notification.tickerText = context.getString(ids.id(STRING, "unknown_weather"));
        } else {
            notification.tickerText = formatTicker(context, weather, unit);
            notification.iconLevel = getNotificationIconLevel(weather, mainUnit);
        }

        notification.when = weather.getTime().getTime();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        
        notification.contentView = new RemoteViews(context.getPackageName(), 
                getNotificationLayoutId(context, textStyle, unit));
        RemoteWeatherLayout layout = createRemoteWeatherLayout(
                context, notification.contentView, unit);
        layout.bind(weather);
        
        notification.contentIntent = getContentIntent(context);
        //notification.contentIntent = getMainActivityPendingIntent(context);
        
        getNotificationManager(context).notify(getNotificationId(), notification);
        
        notifyHandler(weather);
    }

    /**
     *  Returns the notification ID for the skin.
     *  Different skins withing the same application must return different results here.
     */
    protected int getNotificationId() {
        return this.getClass().getName().hashCode();
    }

    /**
     *  Returns the pending intent called on click on notification.
     *  This intent starts the weather info activity.
     */
    protected PendingIntent getContentIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(getWeatherInfoActivityComponentName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }
    
    protected String formatTicker(Context context, Weather weather, TemperatureType unit) {
        ResourceIdFactory ids = ResourceIdFactory.getInstance(context);
        WeatherCondition condition = weather.getConditions().get(0);
        Temperature tempC = condition.getTemperature(TemperatureUnit.C);
        Temperature tempF = condition.getTemperature(TemperatureUnit.F);
        return context.getString(ids.id(STRING, "notification_ticker"),
                weather.getLocation().getText(),
                tempFormat.format(tempC.getCurrent(), tempF.getCurrent(), unit));
    }
    
    protected void notifyHandler(Weather weather) {
        synchronized (BaseWeatherNotificationReceiver.class) {   //monitor of static methods
            if (handler == null) {
                return;
            }
            Message message = handler.obtainMessage();
            Bundle bundle = message.getData();
            bundle.putParcelable(WEATHER_KEY, new ParcelableWeather2(weather));
            message.sendToTarget();
        }
    }
    
    /**
     *  Returns the component name of the weather info activity
     */
    abstract protected ComponentName getWeatherInfoActivityComponentName();
    
    /**
     *  Returns the ID of the notification icon.
     */
    abstract protected int getNotificationIconId();
    
    /**
     *  Returns the notification icon level.
     */
    abstract protected int getNotificationIconLevel(Weather weather, ru.gelin.android.weather.TemperatureUnit unit);
    
    /**
     *  Creates the temperature formatter.
     */
    protected TemperatureFormat createTemperatureFormat() {
        return new TemperatureFormat();
    }
    
    /**
     *  Returns the notification layout id.
     */
    protected int getNotificationLayoutId(Context context, 
            NotificationStyle textStyle, TemperatureType unit) {
        ResourceIdFactory ids = ResourceIdFactory.getInstance(context);
        switch (unit) {
        case C:
        case F:
            return ids.id(LAYOUT, textStyle.getLayoutResName() + LAYOUT_UPDATE_SUFFIX);
        case CF:
        case FC:
            return ids.id(LAYOUT, textStyle.getLayoutResName());
        }
        return 0;   //unknown resource
    }
    
    /**
     *  Creates the remove view layout for the notification.
     */
    protected RemoteWeatherLayout createRemoteWeatherLayout(Context context, RemoteViews views,
            TemperatureType unit) {
        return new RemoteWeatherLayout(context, views, unit);
    }

}
