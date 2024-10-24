package com.es.geolocalization;

import lombok.Value;

@Value
public class GeoPoint {
    double lat;
    double lng;

    @Override
    public String toString() {
        return "(" + lat + ", " + lng + ")";
    }
}