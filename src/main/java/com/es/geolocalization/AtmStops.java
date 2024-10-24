package com.es.geolocalization;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * in questo file vengono caricati i dati delle fermate e metrofermate da file JSON,
 * vengono fornite funzionalità per trovare le fermate più vicine,
 * viene gestita la logica per filtrare e ordinare le fermate in base alla distanza.
 * viene utilizzato un servizio di geocodifica per convertire indirizzi in coordinate.
 */

@Slf4j
public class AtmStops {
    static final long MAX_DISTANCE = 200;  // in meters
    static final int MAX_RESULTS = 10;
    private final List<AtmStop> availableStops;
    private final GeocodingService geocodingService;

    public AtmStops(GeocodingService geo) {
        this.geocodingService = geo;
        availableStops = loadStops();
    }

    public List<AtmStop> stopsNearAddress(String address){
        GeoPoint position = geocodingService.geocodeAddress(address);
        log.debug("Address '{}' geolocated at {}", address, position);
        return nearGeoPoint(position);
    }

    public List<AtmStop> nearGeoPoint(GeoPoint point) {
        return availableStops.stream()
                .filter(stop -> GeocodingService.haversineDistance(stop.getPosition(), point) <= MAX_DISTANCE)
                .sorted(Comparator.comparingDouble(s -> GeocodingService.haversineDistance(s.getPosition(), point)))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
    }

    List<AtmStop> loadStops() {
        List<AtmStop> stops = new ArrayList<>();
        stops.addAll(loadStopsFromJson("ds534_tpl_fermate.json"));
        stops.addAll(loadStopsFromJson("ds535_tpl_metrofermate.json"));
        log.info("Loaded {} ATM stops", stops.size());
        return stops;
    }

    public List<AtmStop> loadStopsFromJson(String filename) {
        List<AtmStop> stops = new ArrayList<>();
        Gson gson = new Gson();

        try (Reader reader = new InputStreamReader(IOHelper.loadClasspathResource(filename))) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            JsonArray fields = json.getAsJsonArray("fields");
            JsonArray records = json.getAsJsonArray("records");

            Map<String, Integer> fieldIndexMap = createFieldIndexMap(fields);

            for (int i = 0; i < records.size(); i++) {
                JsonArray record = records.get(i).getAsJsonArray();

                String code = record.get(fieldIndexMap.get("id_amat")).getAsString();
                String description = getDescription(record, fieldIndexMap);
                String lines = record.get(fieldIndexMap.get("linee")).getAsString();
                double lng = record.get(fieldIndexMap.get("LONG_X_4326")).getAsDouble();
                double lat = record.get(fieldIndexMap.get("LAT_Y_4326")).getAsDouble();

                AtmStop stop = new AtmStop();
                stop.setCode(code);
                stop.setDescription(description);
                stop.setAvailableLines(List.of(lines.split(",")));
                stop.setPosition(new GeoPoint(lat, lng));

                stops.add(stop);
            }
        } catch (IOException e) {
            log.error("Error loading stops from {}", filename, e);
        }

        return stops;
    }

    private String getDescription(JsonArray record, Map<String, Integer> fieldIndexMap) {
        if (fieldIndexMap.containsKey("nome")) {
            return record.get(fieldIndexMap.get("nome")).getAsString();
        } else if (fieldIndexMap.containsKey("ubicazione")) {
            return record.get(fieldIndexMap.get("ubicazione")).getAsString();
        } else {
            log.warn("No description field found in the JSON");
            return "Unknown";
        }
    }

    private Map<String, Integer> createFieldIndexMap(JsonArray fields) {
        Map<String, Integer> fieldIndexMap = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            JsonObject field = fields.get(i).getAsJsonObject();
            fieldIndexMap.put(field.get("id").getAsString(), i);
        }
        return fieldIndexMap;
    }
}
