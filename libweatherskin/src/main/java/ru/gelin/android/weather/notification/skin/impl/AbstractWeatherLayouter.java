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

import static ru.gelin.android.weather.notification.skin.impl.PreferenceKeys.TEMP_UNIT;
import static ru.gelin.android.weather.notification.skin.impl.PreferenceKeys.TEMP_UNIT_DEFAULT;
import static ru.gelin.android.weather.notification.skin.impl.PreferenceKeys.WS_UNIT;
import static ru.gelin.android.weather.notification.skin.impl.PreferenceKeys.WS_UNIT_DEFAULT;
import static ru.gelin.android.weather.notification.skin.impl.ResourceIdFactory.STRING;

import java.util.Calendar;
import java.util.Date;

import ru.gelin.android.weather.Humidity;
import ru.gelin.android.weather.Temperature;
import ru.gelin.android.weather.TemperatureUnit;
import ru.gelin.android.weather.Weather;
import ru.gelin.android.weather.WeatherCondition;
import ru.gelin.android.weather.Wind;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.method.MovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * Utility to layout weather values.
 */
public abstract class AbstractWeatherLayouter {

    static final String URL_TEMPLATE = "<a href=\"%s\">%s</a>";

    /** Current context */
    protected Context context;
    /** ID factory */
    ResourceIdFactory ids;
    /** Condition text formatter */
    protected WeatherConditionFormat conditionFormat;
    /** Temperature formatter */
    TemperatureFormat tempFormat;
    /** Wind formatter */
    WindFormat windFormat;
    /** Humidity formatter */
    HumidityFormat humidityFormat;

    /**
     * Creates the utility for specified context.
     */
    protected AbstractWeatherLayouter(Context context) {
        this.context = context;
        this.ids = ResourceIdFactory.getInstance(context);
        this.conditionFormat = new WeatherConditionFormat(context);
        this.tempFormat = createTemperatureFormat();
        this.windFormat = new WindFormat(context);
        this.humidityFormat = new HumidityFormat(context);
    }

    /**
     * Retreives "id/<name>" resource ID.
     */
    protected int id(String name) {
        return this.ids.id(name);
    }

    /**
     * Retreives "string/<name>" resource ID.
     */
    protected int string(String name) {
        return this.ids.id(STRING, name);
    }

    /**
     * Lays out the weather values on a view.
     */
    public void bind(Weather weather) {
        if (weather.isEmpty()) {
            emptyViews();
        } else {
            bindViews(weather);
        }

    }

    void bindViews(Weather weather) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        bindUpdateTime(weather.getQueryTime());
        setText(id("location"),
                preferences.getString(PreferenceKeys.LOCATION, weather.getLocation().getText()));
        // setText(id("location"), weather.getLocation().getText());

        if (weather.getConditions().size() <= 0) {
            return;
        }
        WeatherCondition currentCondition = weather.getConditions().get(0);
        setText(id("condition"), this.conditionFormat.getText(currentCondition));
        setCondition(currentCondition, id("current_condition_img"));
        
        
        bindWindHumidity(currentCondition);

        TemperatureType tempType = TemperatureType.valueOf(preferences.getString(
                TEMP_UNIT, TEMP_UNIT_DEFAULT));

        Temperature tempC = currentCondition.getTemperature(TemperatureUnit.C);
        Temperature tempF = currentCondition.getTemperature(TemperatureUnit.F);
        TemperatureUnit mainUnit = tempType.getTemperatureUnit();
        Temperature mainTemp = currentCondition.getTemperature(mainUnit);

        setVisibility(id("temp"), View.VISIBLE);
        setText(id("current_temp"), preferences.getString("tempprefix", "") + tempFormat.format(mainTemp.getCurrent(), tempType));
        switch (tempType) { // TODO: remove multiple appearance of this switch
        case C:
        case F:
            setVisibility(id("current_temp_alt"), View.GONE);
            break;
        case CF:
            setText(id("current_temp_alt"), tempFormat.format(tempF.getCurrent(),
                    TemperatureType.F));
            setVisibility(id("current_temp_alt"), View.VISIBLE);
            break;
        case FC:
            setText(id("current_temp_alt"), tempFormat.format(tempC.getCurrent(),
                    TemperatureType.C));
            setVisibility(id("current_temp_alt"), View.VISIBLE);
            break;
        }
        setText(id("high_temp"), tempFormat.format(mainTemp.getHigh()));
        setText(id("low_temp"), tempFormat.format(mainTemp.getLow()));

        // bindForecasts(weather, mainUnit);

    }

    abstract protected void setCondition(WeatherCondition currentCondition, int viewId) ;

    protected void bindUpdateTime(Date update) {
        if (update.getTime() == 0) {
            setText(id("update_time"), "");
        } else if (isDate(update)) {
            setText(id("update_time"), this.context.getString(
                    string("update_date_format"), update));
        } else {
            setText(id("update_time"), this.context.getString(
                    string("update_time_format"), update));
        }
    }

    protected void bindWindHumidity(WeatherCondition currentCondition) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this.context);

        // SharedPreferences preferences =
        // PreferenceManager.getDefaultSharedPreferences(context);
        // return preferences.getString("windprefix", this.context.getString(this.ids.id(ResourceIdFactory.STRING, resource)));
        // .putString("tempprefix", mItem.temperatureprefix)
        // .putString("windprefix", mItem.windprefix)
        // .putString("humidityprefix", mItem.humidity)

        WindUnit windUnit = WindUnit.valueOf(preferences.getString(
                WS_UNIT, WS_UNIT_DEFAULT));
        Wind wind = currentCondition.getWind(windUnit.getWindSpeedUnit());
        setText(id("wind"), preferences.getString("windprefix", "") + this.windFormat.format(wind));

        Humidity humidity = currentCondition.getHumidity();
        setText(id("humidity"), preferences.getString("humidityprefix", "") + this.humidityFormat.format(humidity));
    }

    protected void bindForecasts(Weather weather, ru.gelin.android.weather.TemperatureUnit unit) {
        setVisibility(id("forecasts"), View.VISIBLE);
        bindForecast(weather, unit, 1,
                id("forecast_1"), id("forecast_day_1"),
                id("forecast_condition_1"),
                id("forecast_high_temp_1"), id("forecast_low_temp_1"));
        bindForecast(weather, unit, 2,
                id("forecast_2"), id("forecast_day_2"),
                id("forecast_condition_2"),
                id("forecast_high_temp_2"), id("forecast_low_temp_2"));
        bindForecast(weather, unit, 3,
                id("forecast_3"), id("forecast_day_3"),
                id("forecast_condition_3"),
                id("forecast_high_temp_3"), id("forecast_low_temp_3"));
    }

    void bindForecast(Weather weather,
            ru.gelin.android.weather.TemperatureUnit unit, int i,
            int groupId, int dayId, int conditionId,
            int highTempId, int lowTempId) {
        if (weather.getConditions().size() > i) {
            setVisibility(groupId, View.VISIBLE);
            Date tomorrow = addDays(weather.getTime(), i);
            setText(dayId, context.getString(string("forecast_day_format"), tomorrow));
            WeatherCondition forecastCondition = weather.getConditions().get(i);
            setText(conditionId, this.conditionFormat.getText(forecastCondition));
            Temperature forecastTemp = forecastCondition.getTemperature(unit);
            setText(highTempId, tempFormat.format(forecastTemp.getHigh()));
            setText(lowTempId, tempFormat.format(forecastTemp.getLow()));
        } else {
            setVisibility(groupId, View.GONE);
        }
    }

    void emptyViews() {
        setText(id("update_time"), "");
        setText(id("location"), "");
        setText(id("condition"), context.getString(string("unknown_weather")));
        setText(id("humidity"), "");
        setText(id("wind"), "");
        setText(id("wind_humidity_text"), "");

        setVisibility(id("temp"), View.INVISIBLE);

        setVisibility(id("forecasts"), View.GONE);
        setVisibility(id("forecasts_text"), View.GONE);

    }

    protected abstract void setText(int viewId, CharSequence text);

    protected abstract void setVisibility(int viewId, int visibility);

    protected abstract void setMovementMethod(int viewId, MovementMethod method);

    Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTime();
    }

    /**
     * Returns true if the provided date has zero (0:00:00) time.
     */
    boolean isDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR) == 0 &&
                calendar.get(Calendar.MINUTE) == 0 &&
                calendar.get(Calendar.SECOND) == 0 &&
                calendar.get(Calendar.MILLISECOND) == 0;
    }

    /**
     * Creates the temperature formatter.
     */
    protected TemperatureFormat createTemperatureFormat() {
        return new TemperatureFormat();
    }

}
