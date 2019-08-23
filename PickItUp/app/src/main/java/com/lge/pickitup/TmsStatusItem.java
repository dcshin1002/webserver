package com.lge.pickitup;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class TmsStatusItem {
    public static final String KEY_STATUS = "route_job";

    public String route_job;

    public TmsStatusItem() {
        // Default constructor required for calls to DataSnapshot.getValue(TmsStatusItem.class)
    }

    public TmsStatusItem(String status) {
        this.route_job = status;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(KEY_STATUS, route_job);
        return result;
    }
}
