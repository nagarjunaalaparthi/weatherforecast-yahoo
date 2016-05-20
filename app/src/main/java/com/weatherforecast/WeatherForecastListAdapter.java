package com.weatherforecast;

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by nagarjuna on 30/3/16.
 */
public class WeatherForecastListAdapter extends BaseAdapter {

    private final Activity context;
    private ArrayList<Weather> weatherData = new ArrayList<>();

    public WeatherForecastListAdapter(Activity activity) {
        this.context = activity;
    }

    public void setWeatherData(ArrayList<Weather> weatherData){
        this.weatherData = weatherData;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if(weatherData!=null) {
            return weatherData.size();
        }else{
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        viewHolder holder = null;
        if(convertView == null) {
            convertView = context.getLayoutInflater().inflate(R.layout.list_item_for_forecast, parent, false);
            holder = new viewHolder();
            holder.date = (TextView) convertView.findViewById(R.id.forecast_day_textView);
            holder.condition = (TextView) convertView.findViewById(R.id.forecast_mode_textView);
            holder.max = (TextView) convertView.findViewById(R.id.max_temp_textview);
            holder.min = (TextView) convertView.findViewById(R.id.min_temp_textview);
            convertView.setTag(holder);
        }else{
            holder = (viewHolder) convertView.getTag();
            if(holder == null){
                holder = new viewHolder();
            }
        }
        Weather weather = weatherData.get(position);
        holder.date.setText(weather.getDay()+", "+weather.getDate());
        holder.condition.setText(weather.getCondition());
        int max = (int) ((Long.parseLong(weather.getMax()) - 32)/1.8);
        holder.max.setText(context.getString(R.string.degrees,max));
        int min = (int) ((Long.parseLong(weather.getMin()) - 32)/1.8);
        holder.min.setText(context.getString(R.string.degrees,min));
        return convertView;
    }

    public class viewHolder{
        TextView date;
        TextView condition;
        TextView max;
        TextView min;
    }
}
