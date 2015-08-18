package ru.gelin.android.weather.openweathermap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ru.gelin.android.weather.Location;
import ru.gelin.android.weather.TestWeather;
import ru.gelin.android.weather.Weather;
import ru.gelin.android.weather.WeatherException;
import ru.gelin.android.weather.WeatherSource;
import ru.gelin.android.weather.source.HttpWeatherSource;
import android.content.Context;
import android.util.Log;

/**
 *  Weather source implementation which uses openweathermap.org
 */
public class OpenWeatherMapSource extends HttpWeatherSource implements WeatherSource {
    private final static String TAG = "OpenWeatherMapSource";
    private static final boolean DBG = false;

    /** Base API URL */
    static final String API_BASE_URL = "http://openweathermap.org/data/2.5";
    /** Current weather API URL */
    static final String API_WEATHER_URL = API_BASE_URL + "/weather?";
    /** Forecasts API URL */
    static final String API_FORECAST_URL = API_BASE_URL + "/forecast/daily?cnt=4&id=";
    /** API key */
    static final String API_KEY = "616a1aaacb2a1e3e3ca80c8e78455f76";

    Context context;

    public OpenWeatherMapSource(Context context) {
        this.context = context;
    }

    @Override
    public Weather query(Location location) throws WeatherException {
        if (location == null) {
            throw new WeatherException("null location");
        }
        if (location.getText().startsWith("-")) {
            return new TestWeather(Integer.parseInt(location.getText()));
        }
        if (location.getText().startsWith("+")) {
            return new TestWeather(Integer.parseInt(location.getText().substring(1)));
        }
        OpenWeatherMapWeather weather = new OpenWeatherMapWeather(this.context);
        weather.parseCurrentWeather(queryCurrentWeather(location));
        if (weather.isEmpty()) {
            return weather;
        }
//        weather.parseDailyForecast(queryDailyForecast(weather.getCityId()));
        return weather;
    }

    @Override
    public Weather query(Location location, Locale locale) throws WeatherException {
        return query(location);
        //TODO: what to do with locale?
    }

    @Override
    protected void prepareRequest(HttpGet request) {
        request.addHeader("X-API-Key", API_KEY);
    }

    JSONObject queryCurrentWeather(Location location) throws WeatherException {
        String url = API_WEATHER_URL + location.getQuery();
        JSONTokener parser = new JSONTokener(readJSON(url));
        try {
            return (JSONObject)parser.nextValue();
        } catch (JSONException e) {
            throw new WeatherException("can't parse weather", e);
        }
    }

    JSONObject queryDailyForecast(int cityId) throws WeatherException {
        String url = API_FORECAST_URL + String.valueOf(cityId);
        JSONTokener parser = new JSONTokener(readJSON(url));
        try {
            return (JSONObject)parser.nextValue();
        } catch (JSONException e) {
            throw new WeatherException("can't parse forecast", e);
        }
    }

    String readJSON(String url) throws WeatherException {
        StringBuilder result = new StringBuilder();
        InputStreamReader reader = getReaderForURL(url);
        char[] buf = new char[1024];
        try {
            int read = reader.read(buf);
            while (read >= 0) {
                result.append(buf, 0 , read);
                if (DBG)
                    Log.i(TAG, "readJSON. result = " + result + ", Thread=" + Thread.currentThread());
                read = reader.read(buf);
            }
        } catch (IOException e) {
            throw new WeatherException("can't read weather", e);
        }
        return result.toString();
    }

}
