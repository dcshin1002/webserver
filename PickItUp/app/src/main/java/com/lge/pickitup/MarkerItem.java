package com.lge.pickitup;

import androidx.annotation.Nullable;
import java.util.Objects;

public class MarkerItem {
    private double latitude;
    private double longitude;

    public MarkerItem(String latitude, String longitude) {
        this.latitude = Double.valueOf(latitude);
        this.longitude = Double.valueOf(longitude);
    }

    @Override
    public int hashCode(){
        return Objects.hash(latitude, longitude);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        MarkerItem item = (MarkerItem) obj;
        if (Double.compare(item.latitude, this.latitude) == 0 && Double.compare(item.longitude, this.longitude) == 0) {
            return true;
        }
        return false;
    }

}
