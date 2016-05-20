package com.weatherforecast;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by nagarjuna on 13/4/16.
 */
public class ForecastFragment extends Fragment {


    public static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;

    private static final String SELECTED_KEY = "selected_position";
    private ArrayAdapter<String> mForecastAdapter;
    private WeatherForecastListAdapter weatherForecastAdapter;
    private ProgressDialog dialogue;
    private View headerView;


    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.content_main, container, false);

        mListView = (ListView) rootView.findViewById(R.id.listview);
        // We'll call our MainActivity
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        weatherForecastAdapter = new WeatherForecastListAdapter(getActivity());
        mListView.setAdapter(weatherForecastAdapter);


        updateWeather();
        return rootView;
    }

    public void showProgressDialogue(){
        dialogue = ProgressDialog.show(getActivity(),"","fetching");
    }

    public void dismissProgressDialogue(){
        if(dialogue!=null && dialogue.isShowing()){
            dialogue.dismiss();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    private void updateWeather() {
        if(isNetworkAvailable(getActivity())) {
            showProgressDialogue();
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("Hyderabad,in");
        }else{
            Toast.makeText(getActivity(),"no network connectivity, Please connect to a network and refresh again.",Toast.LENGTH_LONG).show();
        }
    }
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connection = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = null;
        if (connection != null) {
            nInfo = connection.getActiveNetworkInfo();
        }
        if (nInfo == null || !nInfo.isConnectedOrConnecting()) {
            return false;
        }

        if (nInfo == null || !nInfo.isConnected()) {
            return false;
        }
        if (nInfo != null && ((nInfo.getType() == ConnectivityManager.TYPE_MOBILE) || (nInfo.getType() == ConnectivityManager.TYPE_WIFI))) {
            if (nInfo.getState() != NetworkInfo.State.CONNECTED || nInfo.getState() == NetworkInfo.State.CONNECTING) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, ArrayList<Weather>> {


        @Override
        protected ArrayList<Weather> doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String query = "select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\""+params[0]+"\")";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "https://query.yahooapis.com/v1/public/yql?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "format";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM,query)
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .build();

                URL url = new URL(builtUri.toString());


                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            }
            try {
                return getWeatherDataFromJson(forecastJsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Weather> weathers) {
            super.onPostExecute(weathers);
            dismissProgressDialogue();
            if(weathers!=null) {
                setDataToList(weathers);
            }else{
                Toast.makeText(getActivity(),"something went wrong, please refresh again.",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setDataToList(ArrayList<Weather> weathers) {
        mListView.removeHeaderView(headerView);
        headerView = getActivity().getLayoutInflater().inflate(R.layout.forecast_list_header,null);
        TextView date = (TextView) headerView.findViewById(R.id.date);
        TextView city = (TextView) headerView.findViewById(R.id.city);
        TextView country = (TextView) headerView.findViewById(R.id.country);
        TextView temp = (TextView) headerView.findViewById(R.id.temperature);
        TextView condition = (TextView) headerView.findViewById(R.id.condition);
        TextView wind = (TextView) headerView.findViewById(R.id.wind);
        TextView humidity = (TextView) headerView.findViewById(R.id.humidity);

        Weather weather = weathers.get(0);
        date.setText(weather.getDay()+", "+weather.getDate());
        city.setText(weather.getCity());
        country.setText(weather.getRegion()+", "+weather.getCountry());
        condition.setText(weather.getCondition());
        int temperature = (int) ((Long.parseLong(weather.getCurrentTemp()) - 32)/1.8);
        temp.setText(getString(R.string.degrees,temperature));
        wind.setText(getString(R.string.wind,weather.getWindSpeed()));
        humidity.setText(getString(R.string.humidity,weather.getHumidity()));
        mListView.addHeaderView(headerView);
        weathers.remove(weather);
        weatherForecastAdapter.setWeatherData(weathers);
    }


    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private ArrayList<Weather> getWeatherDataFromJson(String forecastJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        ArrayList<Weather> weatherArrayList = new ArrayList<>();
        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        if(forecastJson.has("query")){
            JSONObject queryObject = forecastJson.getJSONObject("query");
            if (queryObject.has("results")){
                JSONObject resultObject = queryObject.getJSONObject("results");
                if(resultObject.has("channel")){
                    JSONObject channelObject = resultObject.getJSONObject("channel");
                    if(channelObject.has("item")){
                        JSONObject itemsObject = channelObject.getJSONObject("item");
                        if(itemsObject.has("forecast")){
                            JSONArray forecastArray = itemsObject.getJSONArray("forecast");
                            if(forecastArray!=null && forecastArray.length() > 0){
                                for(int i=0;i<forecastArray.length();i++){
                                    JSONObject forecastObject = forecastArray.getJSONObject(i);
                                    Weather weather = new Weather();
                                    weather.setDate(forecastObject.optString("date"));
                                    weather.setDay(forecastObject.optString("day"));
                                    weather.setMax(forecastObject.optString("high"));
                                    weather.setMin(forecastObject.optString("low"));
                                    weather.setCondition(forecastObject.optString("text"));
                                    if(i == 0){
                                        if(itemsObject.has("condition")){
                                            JSONObject conditionObject = itemsObject.getJSONObject("condition");
                                            weather.setCurrentTemp(conditionObject.optString("temp"));
                                            weather.setCondition(conditionObject.optString("text"));
                                        }
                                        if(channelObject.has("astronomy")){
                                            JSONObject astronomyObject = channelObject.getJSONObject("astronomy");
                                            weather.setSunrise(astronomyObject.optString("sunrise"));
                                            weather.setSunset(astronomyObject.optString("sunset"));
                                        }
                                        if(channelObject.has("atmosphere")){
                                            JSONObject atmosphereObject = channelObject.getJSONObject("atmosphere");
                                            weather.setHumidity(atmosphereObject.optString("humidity"));
                                        }
                                        if(channelObject.has("wind")){
                                            JSONObject windObject = channelObject.getJSONObject("wind");
                                            weather.setWindSpeed(windObject.optString("speed"));
                                        }
                                        if(channelObject.has("location")){
                                            JSONObject locationObject = channelObject.getJSONObject("location");
                                            weather.setCity(locationObject.optString("city"));
                                            weather.setRegion(locationObject.optString("region"));
                                            weather.setCountry(locationObject.optString("country"));
                                        }
                                    }
                                    weatherArrayList.add(weather);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Yahoo Weather API returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.


        return weatherArrayList;

    }
}
