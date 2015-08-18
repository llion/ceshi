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

package ru.gelin.android.weather.notification.skin.builtin;

import static ru.gelin.android.weather.notification.Tag.TAG;
import ru.gelin.android.weather.TemperatureUnit;
import ru.gelin.android.weather.Weather;
import ru.gelin.android.weather.notification.skin.impl.BaseWeatherNotificationReceiver;
import android.content.ComponentName;


/**
 *  Extends the basic notification receiver.
 *  Always displays the same notification icon.
 */
public class SkinWeatherNotificationReceiver extends BaseWeatherNotificationReceiver {

    @Override
    protected ComponentName getWeatherInfoActivityComponentName() {
        return new ComponentName(TAG, "XXXXXX");
    }

    @Override
    protected int getNotificationIconId() {
        return 0;
    }

    @Override
    protected int getNotificationIconLevel(Weather weather, TemperatureUnit unit) {
        return 0;
    }

}
