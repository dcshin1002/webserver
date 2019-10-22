package com.lge.pickitup;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class TmsCourierItem {
    public static final String KEY_ID = "id";
    public static final String KEY_SECTOR_ID = "sectorid";
    public static final String KEY_NAME = "name";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_START_PARCEL_ID = "startparcelid";
    public static final String KEY_END_PARCEL_ID = "endparcelid";

    public String id;
    public int sectorid;
    public String name;
    public String latitude = "";
    public String longitude = "";
    public int startparcelid = -1;
    public int endparcelid = -1;

    public TmsCourierItem() {

    }

    public TmsCourierItem(String id, String name) {
        this.id = id;
        this.sectorid = Integer.parseInt(id);
        this.name = name;
    }

    public TmsCourierItem(int id, String name) {
        this.id = String.valueOf(id);
        this.sectorid = id;
        this.name = name;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(KEY_ID, id);
        result.put(KEY_SECTOR_ID, sectorid);
        result.put(KEY_NAME, name);
        result.put(KEY_LATITUDE, latitude);
        result.put(KEY_LONGITUDE, longitude);
        result.put(KEY_START_PARCEL_ID, startparcelid);
        result.put(KEY_END_PARCEL_ID, endparcelid);
        return result;
    }
}
