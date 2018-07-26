package com.example.guptarak.tomtom_map_sample.utils;

import android.content.Context;
import android.location.Location;
import android.os.Vibrator;

import com.tomtom.online.sdk.common.location.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchUtil {
    private static List<Location> alreadyVoiced = new ArrayList<Location>();


    public static  boolean isWithinDistance(Location location, List<Location> bumpers){
        List<Location> withinDistance = new ArrayList();
        for(Location coordinate : bumpers){
            if(coordinate.distanceTo(location) <=10 && !alreadyBeeped(coordinate)){
                alreadyVoiced.add(coordinate);
                return true;
            }
        }

        return false;

    }

    public static  boolean alreadyBeeped(Location loc){

        return alreadyVoiced.contains(loc);

    }
    public static  Location convertLatLngToLocation(LatLng latLng){
        Location location1 = new Location("");
        location1.setLatitude(latLng.getLatitude());
        location1.setLongitude(latLng.getLongitude());
        return location1;

    }
    public static LatLng convertLocationToLatLng(Location loc){
        return  new LatLng(loc.getLatitude(),loc.getLongitude());
    }

    public static JSONObject getJsonObject(LatLng first, LatLng next){
        JSONObject jsonObject1 = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            jsonObject1.put("lat",first.getLatitude());
            jsonObject1.put("lon",first.getLongitude());

            jsonObject2.put("lat",next.getLatitude());
            jsonObject2.put("lon",next.getLongitude());
            jsonArray.put(jsonObject1);
            jsonArray.put(jsonObject2);

            jsonObject.put("location",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

   }
