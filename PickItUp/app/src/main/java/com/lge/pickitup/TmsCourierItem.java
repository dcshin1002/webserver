package com.lge.pickitup;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class TmsCourierItem {
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";

    public String id;
    public String name;
    public String latitude = "";
    public String longitude = "";

    public TmsCourierItem() {

    }

    public TmsCourierItem(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public TmsCourierItem(String id, String name, String latitude, String longitude) {
        this.id = id;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(KEY_ID, id);
        result.put(KEY_NAME, name);
        result.put(KEY_LATITUDE, latitude);
        result.put(KEY_LONGITUDE, longitude);
        return result;
    }
}
