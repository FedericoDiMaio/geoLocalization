package com.es.geolocalization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spark.utils.IOUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Questa classe gestisce le richieste a un'API di geocodifica esterna,
 * controlla se le coordinate risultanti sono all'interno dei limiti di Milano,
 * utilizza la formula di Haversine per calcolare le distanze e
 * gestisce le eccezioni relative alla geocodifica e ai limiti della città.
 */
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {

    private final String url;
    private final String apiKey;
    private static final double EARTH_RADIUS = 6371000;
    private static final double MILAN_NORTH = 45.535;
    private static final double MILAN_SOUTH = 45.390;
    private static final double MILAN_EAST = 9.280;
    private static final double MILAN_WEST = 9.070;

    public GeoPoint geocodeAddress(String address) throws GeoLocalizationApplication.GeocodingException, GeoLocalizationApplication.AddressOutOfMilanException {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String requestUrl = String.format("%s?q=%s&apiKey=%s", url, encodedAddress, apiKey);
        //log.debug("Calling API with URL: {}", requestUrl);

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new GeoLocalizationApplication.GeocodingException("HTTP error code: " + connection.getResponseCode());
            }

            String jsonResponse = IOUtils.toString(connection.getInputStream());
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!jsonObject.has("items") || jsonObject.getAsJsonArray("items").isEmpty()) {
                throw new GeoLocalizationApplication.GeocodingException("No results found for the given address");
            }

            JsonObject location = jsonObject.getAsJsonArray("items")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("position");

            double lat = location.get("lat").getAsDouble();
            double lng = location.get("lng").getAsDouble();

            if (!isWithinMilan(lat, lng)) {
                throw new GeoLocalizationApplication.AddressOutOfMilanException("The requested address is not within Milan city limits");
            }

            return new GeoPoint(lat, lng);

        } catch (IOException e) {
            log.error("Error geocoding address: {}", address, e);
            throw new GeoLocalizationApplication.GeocodingException("Failed to geocode address: " + e.getMessage());
        }
    }

    public static long haversineDistance(GeoPoint point1, GeoPoint point2) {
        double lat1 = Math.toRadians(point1.getLat());
        double lat2 = Math.toRadians(point2.getLat());
        double lon1 = Math.toRadians(point1.getLng());
        double lon2 = Math.toRadians(point2.getLng());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        long distance = Math.round(EARTH_RADIUS * c);
        //log.debug("Distance between {} and {} is {} meters", point1, point2, distance);

        return distance;
    }

    private boolean isWithinMilan(double lat, double lng) {
        return lat >= MILAN_SOUTH && lat <= MILAN_NORTH && lng >= MILAN_WEST && lng <= MILAN_EAST;
    }
}